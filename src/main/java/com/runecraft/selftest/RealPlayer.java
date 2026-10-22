package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.FloorMarkers;
import com.runecraft.boss.HazardKind;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.FreezeRules;
import com.runecraft.combat.PrayerService;
import com.runecraft.network.HudNet;
import com.runecraft.osrs.CastBar;
import com.runecraft.osrs.GridPath;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.osrs.RealReaction;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The self-test's player played like a real one (round 44 playability, docs/PLAYABILITY.md 'Self-test and walk migration'):
 * the shared helper of the boss, raid and minigame walks, so CI proves the real-time windows of rounds 41-43 are fair for a
 * human, not only correct on paper.
 * <ul>
 *   <li><b>Real movement</b> ({@link #goTo}, {@link #walkTo}): the player walks at {@link RealReaction#RUN_BLOCKS_PER_SECOND}
 *   blocks a second through the world's collision (round pillars and debris by a {@link GridPath} route, up single-block steps,
 *   never while frozen), one Minecraft tick at a time, instead of being teleported to a safe spot.</li>
 *   <li><b>Prayers on cues</b> ({@link #autoPray}, {@link #react}): protection prayers switch through the prayer key's own
 *   path ({@link PrayerService#set}) {@link RealReaction#REACTION_MC} Minecraft ticks after the cue a player sees: the cast bar
 *   row ({@link HudNet#castListener}), a projectile, wind-up or telegraph appearing ({@link Mechanics#pendingListener}). Never
 *   by setting the active prayers.</li>
 *   <li><b>Walk-out budget</b>: every round ground strike or hazard on its way ({@link HudNet#groundListener}) that the player
 *   stood in when it appeared and that the walk then left is judged against {@link RealReaction#fits}; one that can't be
 *   escaped in time by a player reacting at once is a failure naming the strike ({@link #budgetFailures}).</li>
 *   <li><b>No-heal mode</b> ({@link #noHeal}): the player is mortal, carries a fixed food and prayer potion budget, eats and
 *   drinks through the quick-eat and quick-drink keys' path (the shared food and potion delays apply), runs its own game
 *   ticks (prayer drain, poison and venom, regeneration) and dodges ground strikes by itself ({@link #autoDodge}); a hit that
 *   would kill it is recorded ({@link #death}) instead of killing a real player mid-run.</li>
 * </ul>
 * The driver of a walk calls {@link #tick} once every Minecraft tick, after the world's own tick (the clock, the boss, the
 * pending hits). The helper chains itself onto the recorders it listens to and puts them back on {@link #close}.
 */
public final class RealPlayer implements AutoCloseable {
    /** Food eaten below this share of the hitpoints, prayer potions drunk below these points (no-heal mode). */
    private static final double EAT_BELOW = 0.6;
    private static final int DRINK_BELOW = 25;
    /** How far a dodge looks for a safe spot, and how far a walk may route round obstacles (blocks). */
    private static final double DODGE_REACH = 14.0;
    private static final int ROUTE_RADIUS = 48;
    private static final int ROUTE_NODES = 12_000;
    /** Cues kept this long after they landed (MC), for the budget's judgement. */
    private static final int CUE_MEMORY_MC = 60;
    /** A walk that moved the player less than this from where a strike found them didn't dodge it. */
    private static final double MOVED = 0.5;
    /** How far (blocks) a dodge looks for a safe-coloured floor tile. */
    private static final int SAFE_TILE_REACH = 8;
    /** Fresh routes a blocked walk tries before the player stops where it is. */
    private static final int MAX_REPLANS = 3;

    /** One thing the player saw coming. */
    private static final class Cue {
        final String name;
        final long cueMc;
        final long landMc;
        /** The protection it asks for (0 magic, 1 missiles, 2 melee), -1 for none. */
        final int protection;
        final boolean dodge;
        /** A round strike's place and size (null: no shape known). */
        final Vec3 centre;
        final double radius;
        final Vec3 playerAt;
        /** Blocks to the nearest safe spot when it appeared (0: not in it). */
        double need;
        boolean judged;
        /** A dodge cue with no shape (an area shown by safe floor tiles): the player has looked for a safe tile. */
        boolean sought;

        Cue(String name, long cueMc, long landMc, int protection, boolean dodge, Vec3 centre, double radius, Vec3 playerAt) {
            this.name = name;
            this.cueMc = cueMc;
            this.landMc = landMc;
            this.protection = protection;
            this.dodge = dodge;
            this.centre = centre;
            this.radius = radius;
            this.playerAt = playerAt;
        }
    }

    private static final Map<UUID, RealPlayer> ACTIVE = new ConcurrentHashMap<>();
    private static boolean hooked;

    private final ServerPlayer p;
    private final ServerLevel world;
    private final RealPlayer previous;
    private final Mechanics.PendingListener pendingBefore;
    private final Consumer<HudNet.SentCast> castBefore;
    private final Consumer<HudNet.GroundCast> groundBefore;
    private final List<Cue> cues = new ArrayList<>();
    /** The (source, landing) keys of the round ground casts seen on Minecraft tick {@link #shapedMc}. */
    private final java.util.Set<Long> shapedThisTick = new java.util.HashSet<>();
    private long shapedMc = Long.MIN_VALUE;
    private boolean closed;

    private boolean autoPray;
    private boolean autoDodge;
    /** A protection prayer the player keeps up (switched on again after a disable), or null. */
    private Prayer keep;
    private int restores;
    /** Prayer switches a walk asked for after a cue it read itself: {at MC, prayer ordinal, on 1/0}. */
    private final List<long[]> reactions = new ArrayList<>();

    private List<Vec3> route;
    /** Spots the routes keep clear of (a dark seed, a pool), each with its radius: {x, z, r}. */
    private final List<double[]> avoid = new ArrayList<>();
    private int routeIndex;
    private Vec3 goal;
    private boolean dodging;
    private int stuckMc;
    /** Fresh routes a blocked walk has tried since its goal was set. */
    private int replans;
    /** Whether the last route reached its spot's own column (false: it ends at the nearest column it can reach). */
    private boolean planReached = true;
    /** Walks that ended short of their spot (blocked, or the spot off the floor), and the last such spot. */
    private int blockedWalks;
    private Vec3 lastBlocked;
    private double vy;
    private long lastTickMc = Long.MIN_VALUE;
    private long frozenMc;
    private double walked;

    private boolean mortal;
    private boolean wasInvulnerable;
    private long lastGameTick = Long.MIN_VALUE;
    private int foodGiven;
    private int potionsGiven;
    private int ate;
    private int drank;
    private boolean outOfFood;
    private boolean outOfPotions;
    /** The Minecraft tick a danger-coloured floor mark was first seen under the player (-1: none under it now). */
    private long markSeenMc = -1;
    private String death;

    private int switches;
    private int dodges;
    private double longestDodge;
    private final List<String> budget = new ArrayList<>();
    private final List<String> caughtWalking = new ArrayList<>();
    private final Map<String, Integer> taken = new LinkedHashMap<>();

    private RealPlayer(ServerPlayer p) {
        this.p = p;
        this.world = p.serverLevel();
        this.previous = ACTIVE.put(p.getUUID(), this);
        hook();
        pendingBefore = Mechanics.pendingListener;
        castBefore = HudNet.castListener;
        groundBefore = HudNet.groundListener;
        Mechanics.PendingListener before = pendingBefore;
        Mechanics.pendingListener = new Mechanics.PendingListener() {
            @Override
            public void created(Mechanics.Threat t) {
                before.created(t);
                onThreat(t);
            }

            @Override
            public void cue(Mechanics.Threat t, long mc) {
                before.cue(t, mc);
            }

            @Override
            public void resolved(Mechanics.Threat t, long mc, boolean landed) {
                before.resolved(t, mc, landed);
            }
        };
        HudNet.castListener = castBefore.andThen(this::onCast);
        HudNet.groundListener = groundBefore.andThen(this::onGround);
    }

    /** Starts playing {@code p} as a real player: listening to its cues, walking and praying when asked. Close it when done. */
    public static RealPlayer start(ServerPlayer p) {
        return new RealPlayer(p);
    }

    /** The helper playing {@code p} now, or null. */
    public static RealPlayer of(ServerPlayer p) {
        return p == null ? null : ACTIVE.get(p.getUUID());
    }

    public ServerPlayer player() {
        return p;
    }

    /** Switch protection prayers by themselves, {@link RealReaction#REACTION_MC} after each cue, against the next hit to land. */
    public RealPlayer autoPray(boolean on) {
        autoPray = on;
        return this;
    }

    /** Step out of every round ground strike, hazard and danger-marked floor block by itself, after the reaction time. */
    public RealPlayer autoDodge(boolean on) {
        autoDodge = on;
        return this;
    }

    /**
     * No-heal mode: the player becomes mortal (not invulnerable, never topped up by the walk), carries {@code food} sharks and
     * {@code prayerPotions} four-dose prayer potions, eats below {@value #EAT_BELOW} of its hitpoints and drinks below
     * {@value #DRINK_BELOW} Prayer points through the quick-eat and quick-drink keys' path, and runs its own game ticks.
     */
    public RealPlayer noHeal(int food, int prayerPotions) {
        mortal = true;
        wasInvulnerable = p.isInvulnerable();
        p.setInvulnerable(false);
        foodGiven = food;
        potionsGiven = prayerPotions;
        if (food > 0) {
            Inv.give(p, "shark", food);
        }
        for (int i = 0; i < prayerPotions; i++) {
            Inv.give(p, "prayer_potion", 1);
        }
        lastGameTick = ActionManager.gameTick();
        return this;
    }

    public boolean mortal() {
        return mortal;
    }

    /** Melee armour (head, body, legs, feet) for {@link #wear}: no quest locks, 75 Defence at most. */
    public static final List<String> MELEE_ARMOUR = List.of("justiciar_faceguard", "justiciar_chestguard", "justiciar_legguards",
            "primordial_boots");
    /** Ranged armour for {@link #wear}, worn where players range or switch styles. */
    public static final List<String> RANGED_ARMOUR = List.of("armadyl_helmet", "armadyl_chestplate", "armadyl_chainskirt",
            "pegasian_boots");

    /** What the player wore before {@link #wear}, put back on {@link #close}. */
    private final Map<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack> wornBefore =
            new java.util.EnumMap<>(net.minecraft.world.entity.EquipmentSlot.class);

    /**
     * Wears armour for the fight, given as item ids for the head, body, legs and feet (in that order): a real player comes
     * dressed. {@link #close} puts back what was worn before.
     */
    public RealPlayer wear(List<String> armour) {
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < Math.min(slots.length, armour.size()); i++) {
            net.minecraft.world.item.ItemStack piece = Inv.stack(armour.get(i), 1);
            if (piece.isEmpty()) {
                continue;
            }
            wornBefore.putIfAbsent(slots[i], p.getItemBySlot(slots[i]).copy());
            p.setItemSlot(slots[i], piece);
        }
        return this;
    }

    // ------------------------------------------------------------------ cues

    /**
     * A projectile or a wind-up on its way to the player: seen as it appears (the style-coloured trail or swing). A telegraph is
     * read from its cast-bar row instead, which also says when it is an area to step out of rather than a hit to pray against.
     */
    private void onThreat(Mechanics.Threat t) {
        if (closed || t.kind() == Mechanics.Kind.TELEGRAPH || Mechanics.targetOf(t.id()) != p) {
            return;
        }
        add(new Cue(t.kind().name().toLowerCase(Locale.ROOT), t.createdMc(), t.dueMc(), RealReaction.protectionOf(t.style()), false,
                null, 0, p.position()));
    }

    private void onCast(HudNet.SentCast c) {
        if (closed || c.player() != p) {
            return;
        }
        boolean dodge = (c.flags() & CastBar.FLAG_DODGE) != 0;
        if (dodge && shapedThisTick.contains(groundKey(c.source(), c.landInMc()))) {
            return; // the row of a round strike or hazard just seen with its place and size (onGround dodges that one)
        }
        boolean prayable = !dodge && (c.flags() & CastBar.FLAG_UNPRAYABLE) == 0;
        long now = ActionManager.mcTick();
        add(new Cue(c.label() == null || c.label().isEmpty() ? "cast" : c.label(), now, now + c.landInMc(),
                prayable ? RealReaction.protectionOf(c.style()) : -1, dodge, null, 0, p.position()));
    }

    /** A ground cast's (source, landing) key: its cast-bar row, sent right after it, carries the same. */
    private static long groundKey(int sourceId, int landInMc) {
        return ((long) sourceId << 32) ^ (landInMc & 0xFFFFFFFFL);
    }

    private void onGround(HudNet.GroundCast g) {
        if (closed || g.world() != world) {
            return;
        }
        long nowMc = ActionManager.mcTick();
        if (nowMc != shapedMc) {
            shapedThisTick.clear();
            shapedMc = nowMc;
        }
        shapedThisTick.add(groundKey(g.source() == null ? -1 : g.source().getId(), g.landInMc()));
        double dx = p.getX() - g.centre().x;
        double dz = p.getZ() - g.centre().z;
        if (!CastBar.nearStrike(dx, p.getY() - g.centre().y, dz, g.radius())) {
            return;
        }
        long now = ActionManager.mcTick();
        String name = g.label() != null ? g.label() : g.source() != null ? g.source().getName().getString() : "strike";
        Cue cue = new Cue(name, now, now + g.landInMc(), -1, true, g.centre(), g.radius(), p.position());
        add(cue);
        if (RealReaction.exitDistance(dx, dz, g.radius(), RealReaction.BUDGET_CLEARANCE) > 0) {
            // what a player reacting at once has to walk: to the nearest spot clear of everything landing with it (its edge)
            double[] spot = safeSpot(p.position(), cue.landMc - 6, cue.landMc + 6, false);
            cue.need = spot == null ? Double.POSITIVE_INFINITY : Math.max(spot[2], 1e-3);
        }
    }

    private void add(Cue cue) {
        cues.add(cue);
    }

    /** The protection prayer a walk read the need for itself switches on {@link RealReaction#REACTION_MC} from now. */
    public void react(Prayer prayer) {
        if (prayer == null) {
            return;
        }
        long at = ActionManager.mcTick() + RealReaction.REACTION_MC;
        for (long[] r : reactions) {
            if (r[1] == prayer.ordinal() && r[2] == 1) {
                return; // already on its way
            }
        }
        reactions.add(new long[]{at, prayer.ordinal(), 1});
    }

    /** {@link #react} switching a prayer off. */
    public void reactOff(Prayer prayer) {
        if (prayer != null) {
            reactions.add(new long[]{ActionManager.mcTick() + RealReaction.REACTION_MC, prayer.ordinal(), 0});
        }
    }

    /** Whether a prayer switch the walk asked for is still waiting for its reaction time. */
    public boolean reacting() {
        return !reactions.isEmpty();
    }

    /**
     * Keeps a protection prayer up (null: stop): whenever something switches it off (a boss's prayer disable, running dry), the
     * player switches it on again {@link RealReaction#REACTION_MC} after seeing it go, through the prayer key.
     */
    public void keep(Prayer prayer) {
        keep = prayer;
        if (prayer != null && !Players.data(p).isActive(prayer)) {
            set(prayer, true);
        }
    }

    /** Times a kept prayer had to be switched on again after something switched it off. */
    public int restores() {
        return restores;
    }

    private void keepUp(long now) {
        if (keep == null || Players.data(p).isActive(keep)) {
            return;
        }
        for (long[] r : reactions) {
            if (r[1] == keep.ordinal() && r[2] == 1) {
                return; // already switching it back
            }
        }
        reactions.add(new long[]{now + RealReaction.REACTION_MC, keep.ordinal(), 1});
        restores++;
    }

    /**
     * Judges a dodge the walk makes on a cue of its own (an area with no round strike behind it: a gaze, a screech, a blast with
     * a safe spot): walking {@code distance} blocks must fit {@code leadMc}, the time from the cue to the landing.
     */
    public void dodgeBudget(String name, double distance, long leadMc) {
        dodges++;
        longestDodge = Math.max(longestDodge, distance);
        String why = RealReaction.budgetFailure(name, distance, leadMc);
        if (why != null && budget.size() < 8) {
            budget.add(why);
        }
    }

    // ------------------------------------------------------------------ every Minecraft tick

    /** One Minecraft tick of the player: due prayer switches, eating and drinking, dodging, one step of its walk. */
    public void tick() {
        long now = ActionManager.mcTick();
        if (closed || now == lastTickMc) {
            return;
        }
        lastTickMc = now;
        if (mortal) {
            gameTicks();
            if (!p.isAlive() && death == null) {
                death = "died";
            }
        }
        keepUp(now);
        applyReactions(now);
        if (autoPray) {
            prayAgainstNext(now);
        }
        if (mortal) {
            consume();
        }
        if (autoDodge) {
            dodge(now);
        }
        step();
        judge(now);
    }

    /** The player's own game ticks while mortal (prayer drain, poison and venom, regeneration), as the server runs them. */
    private void gameTicks() {
        long gt = ActionManager.gameTick();
        if (lastGameTick == Long.MIN_VALUE) {
            lastGameTick = gt;
        }
        while (lastGameTick < gt) {
            lastGameTick++;
            PrayerService.gameTick(p, lastGameTick);
            com.runecraft.player.PlayerEvents.gameTick(p, lastGameTick);
        }
    }

    private void applyReactions(long now) {
        for (Iterator<long[]> it = reactions.iterator(); it.hasNext(); ) {
            long[] r = it.next();
            if (r[0] <= now) {
                it.remove();
                set(Prayer.values()[(int) r[1]], r[2] == 1);
            }
        }
    }

    private void prayAgainstNext(long now) {
        List<Cue> live = new ArrayList<>();
        for (Cue c : cues) {
            if (c.protection >= 0 && c.landMc >= now) {
                live.add(c);
            }
        }
        int[] prot = new int[live.size()];
        long[] cue = new long[live.size()];
        long[] land = new long[live.size()];
        for (int i = 0; i < live.size(); i++) {
            prot[i] = live.get(i).protection;
            cue[i] = live.get(i).cueMc;
            land[i] = live.get(i).landMc;
        }
        int want = RealReaction.wantedProtection(prot, cue, land, now);
        if (want >= 0) {
            set(PrayerGrace.PROTECTIONS[want], true);
        }
    }

    /**
     * Switches a prayer through the prayer key's path; without points, a mortal player drinks first (a walk tops them up). Under a
     * curse that shuffles the protection keys (the Nightmare's), the player presses the key that now gives the prayer it wants,
     * as the HUD's curse map tells a real player.
     */
    private void set(Prayer prayer, boolean on) {
        PlayerData data = Players.data(p);
        if (data.isActive(prayer) == on) {
            return;
        }
        if (on && data.prayerPoints() <= 0) {
            if (mortal) {
                drink();
            } else {
                data.setPrayerPoints(99); // a healed walk tests mechanics, not prayer upkeep
            }
        }
        PrayerService.set(p, on ? keyFor(prayer) : prayer, on);
        if (data.isActive(prayer) == on) {
            switches++;
        }
    }

    /** The protection key that switches {@code wanted} on now (the key itself without a curse). */
    private Prayer keyFor(Prayer wanted) {
        if (PrayerGrace.index(wanted) < 0 || PrayerService.REDIRECTS.isEmpty()) {
            return wanted;
        }
        for (Prayer key : PrayerGrace.PROTECTIONS) {
            Prayer target = key;
            for (java.util.function.BiFunction<ServerPlayer, Prayer, Prayer> r : PrayerService.REDIRECTS) {
                Prayer to = r.apply(p, target);
                target = to == null ? target : to;
            }
            if (target == wanted) {
                return key;
            }
        }
        return wanted;
    }

    /**
     * A cue the walk read itself (a soul glowing, a volley named on screen): a hit asking for {@code protection} (0 magic, 1
     * missiles, 2 melee; -1 none) lands on {@code landMc}. Auto-pray weighs it with the rest, a reaction time from now.
     */
    public void cue(String name, int protection, long landMc) {
        add(new Cue(name, ActionManager.mcTick(), landMc, protection, false, null, 0, p.position()));
    }

    private void consume() {
        if (!p.isAlive()) {
            return;
        }
        if (p.getHealth() < p.getMaxHealth() * EAT_BELOW && !outOfFood
                && !com.runecraft.skill.ConsumeCooldowns.blocked(p, com.runecraft.osrs.ConsumeRules.Group.FOOD)) {
            if (com.runecraft.skill.QuickConsume.use(p, false)) { // the quick-eat key: the best food carried
                ate++;
            } else if ("You have no food to eat.".equals(com.runecraft.skill.QuickConsume.lastRefusal)) {
                outOfFood = true;
            }
        }
        if (Players.data(p).prayerPoints() < DRINK_BELOW) {
            drink();
        }
    }

    /** The quick-drink key, while potions last. */
    private void drink() {
        if (!outOfPotions && !com.runecraft.skill.ConsumeCooldowns.blocked(p, com.runecraft.osrs.ConsumeRules.Group.POTION)) {
            if (com.runecraft.skill.QuickConsume.use(p, true)) {
                drank++;
            } else if ("You have no potions to drink.".equals(com.runecraft.skill.QuickConsume.lastRefusal)) {
                outOfPotions = true;
            }
        }
    }

    // ------------------------------------------------------------------ dodging

    /** The round strikes on their way (and landing within the window) as discs. */
    private List<RealReaction.Disc> discs(long fromMc, long toMc) {
        List<RealReaction.Disc> out = new ArrayList<>();
        for (Cue c : cues) {
            if (c.centre != null && c.landMc >= fromMc && c.landMc <= toMc) {
                out.add(new RealReaction.Disc(c.centre.x, c.centre.z, c.radius, c.landMc, c.name));
            }
        }
        return out;
    }

    /**
     * The nearest standable spot clear of every round strike landing between {@code from} and {@code to}: {x, z, distance}.
     * With {@code avoidDanger} it also keeps off danger marks and hazards (where a dodge ends); without, it measures the walk
     * out of the strikes themselves (the budget: their own floor marks, drawn at block size, would make the way out look longer).
     */
    private double[] safeSpot(Vec3 at, long from, long to, boolean avoidDanger) {
        List<RealReaction.Disc> d = discs(from, to);
        int feet = feetY(at.y);
        double clearance = avoidDanger ? RealReaction.EDGE_CLEARANCE : RealReaction.BUDGET_CLEARANCE;
        return RealReaction.safeSpot(at.x, at.z, d, to, DODGE_REACH, clearance, (x, z) -> standable(x, z, feet, avoidDanger));
    }

    private void dodge(long now) {
        if (FreezeRules.frozen(p)) {
            return;
        }
        seekSafeTiles(now);
        long soonest = Long.MAX_VALUE;
        for (Cue c : cues) {
            if (c.centre != null && c.landMc >= now && now - c.cueMc >= RealReaction.REACTION_MC
                    && RealReaction.exitDistance(p.getX() - c.centre.x, p.getZ() - c.centre.z, c.radius) > 0) {
                soonest = Math.min(soonest, c.landMc);
            }
        }
        // a danger-coloured floor mark (or a hazard) under the player: seen as it appears, stepped off a reaction time later
        boolean underDanger = dangerUnder(p.getX(), p.getZ(), feetY(p.getY())) || Mechanics.hazardAt(world, p.position());
        if (!underDanger) {
            markSeenMc = -1;
        } else if (markSeenMc < 0) {
            markSeenMc = now;
        }
        boolean onMark = underDanger && now - markSeenMc >= RealReaction.REACTION_MC;
        if (soonest == Long.MAX_VALUE && !onMark) {
            return;
        }
        if (dodging && goal != null && safeHere(goal, now)) {
            return; // already walking to a safe spot
        }
        long until = soonest == Long.MAX_VALUE ? now + 24 : soonest + 24;
        double[] spot = safeSpot(p.position(), now, until, true);
        if (spot == null || spot[2] < 1e-3) {
            return;
        }
        Vec3 to = new Vec3(spot[0], p.getY(), spot[1]);
        goTo(to);
        dodging = true;
        dodges++;
        longestDodge = Math.max(longestDodge, spot[2]);
    }

    /**
     * A dodge cast with no round shape behind it (a boss's area attack drawn as safe tiles, Sol Heredit's spear and shield): a
     * reaction time after its cue the player walks to the nearest safe-coloured floor tile, when not on one already, and the
     * walk is judged against the cue's lead.
     */
    private void seekSafeTiles(long now) {
        for (Cue c : cues) {
            if (!c.dodge || c.centre != null || c.sought || c.landMc < now || now - c.cueMc < RealReaction.REACTION_MC) {
                continue;
            }
            c.sought = true;
            int feet = feetY(p.getY());
            if (safeUnder(p.getX(), p.getZ(), feet)) {
                continue;
            }
            Vec3 best = null;
            double bestD = Double.MAX_VALUE;
            int px = (int) Math.floor(p.getX());
            int pz = (int) Math.floor(p.getZ());
            for (int dx = -SAFE_TILE_REACH; dx <= SAFE_TILE_REACH; dx++) {
                for (int dz = -SAFE_TILE_REACH; dz <= SAFE_TILE_REACH; dz++) {
                    double x = px + dx + 0.5;
                    double z = pz + dz + 0.5;
                    double d = Math.hypot(x - p.getX(), z - p.getZ());
                    if (d < bestD && safeUnder(x, z, feet) && standable(x, z, feet, true)) {
                        best = new Vec3(x, p.getY(), z);
                        bestD = d;
                    }
                }
            }
            if (best == null) {
                continue;
            }
            goTo(best);
            dodging = true;
            dodges++;
            longestDodge = Math.max(longestDodge, bestD);
            String why = RealReaction.budgetFailure(c.name, bestD, c.landMc - c.cueMc);
            if (why != null && budget.size() < 8) {
                budget.add(why);
            }
        }
    }

    /** Whether the floor block under (x, z) is marked safe (cyan). */
    private boolean safeUnder(double x, double z, int feetY) {
        BlockPos floor = new BlockPos((int) Math.floor(x), feetY - 1, (int) Math.floor(z));
        return FloorMarkers.marked(world, floor) && kindOf(world.getBlockState(floor)) == HazardKind.SAFE;
    }

    /**
     * The nearest spot the player can stand on at least {@code radius} blocks from every one of {@code threats} (chasing
     * currents, tornadoes) and off any danger mark, within {@code reach}; null when there is none.
     */
    public Vec3 clearSpot(List<Vec3> threats, double radius, double reach) {
        List<RealReaction.Disc> d = new ArrayList<>();
        for (Vec3 t : threats) {
            d.add(new RealReaction.Disc(t.x, t.z, radius, 0, "threat"));
        }
        int feet = feetY(p.getY());
        double[] s = RealReaction.safeSpot(p.getX(), p.getZ(), d, Long.MAX_VALUE, reach, (x, z) -> standable(x, z, feet, true));
        return s == null ? null : new Vec3(s[0], p.getY(), s[1]);
    }

    private boolean safeHere(Vec3 at, long now) {
        for (Cue c : cues) {
            if (c.centre != null && c.landMc >= now && RealReaction.exitDistance(at.x - c.centre.x, at.z - c.centre.z, c.radius) > 0) {
                return false;
            }
        }
        return !dangerUnder(at.x, at.z, feetY(at.y));
    }

    /** Judges the strikes that landed: did the walk leave one it stood in, and could a human have? */
    private void judge(long now) {
        for (Iterator<Cue> it = cues.iterator(); it.hasNext(); ) {
            Cue c = it.next();
            if (!c.judged && c.landMc <= now) {
                c.judged = true;
                if (c.centre != null && c.need > 0) {
                    double movedFrom = Math.hypot(p.getX() - c.playerAt.x, p.getZ() - c.playerAt.z);
                    boolean stillIn = RealReaction.exitDistance(p.getX() - c.centre.x, p.getZ() - c.centre.z, c.radius,
                            RealReaction.BUDGET_CLEARANCE) > 0;
                    if (movedFrom >= MOVED || autoDodge) {
                        String why = RealReaction.budgetFailure(c.name, c.need, c.landMc - c.cueMc);
                        if (why != null && budget.size() < 8) {
                            budget.add(why);
                        }
                        if (stillIn && caughtWalking.size() < 8) {
                            caughtWalking.add(String.format(Locale.ROOT, "%s landed on the player %.1f blocks into a %.1f-block walk out",
                                    c.name, movedFrom, c.need));
                        }
                    }
                }
            }
            if (c.landMc < now - CUE_MEMORY_MC) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ walking

    /** Starts walking to {@code at} (a route round obstacles); {@link #tick} moves the player each Minecraft tick. */
    public void goTo(Vec3 at) {
        dodging = false;
        goal = at;
        route = plan(at);
        routeIndex = 0;
        stuckMc = 0;
        replans = 0;
        if (!planReached && Math.hypot(at.x - p.getX(), at.z - p.getZ()) > 0.5) {
            blockedWalks++;
            lastBlocked = at;
        }
    }

    /** Routes keep at least {@code radius} (plus half the player's width) from these spots until {@link #clearAvoid}. */
    public void avoid(List<Vec3> spots, double radius) {
        for (Vec3 v : spots) {
            avoid.add(new double[]{v.x, v.z, radius + 0.3});
        }
    }

    public void clearAvoid() {
        avoid.clear();
    }

    private boolean avoided(double x, double z) {
        for (double[] a : avoid) {
            double dx = x - a[0];
            double dz = z - a[1];
            if (dx * dx + dz * dz < a[2] * a[2]) {
                return true;
            }
        }
        return false;
    }

    /** Stops walking where the player is. */
    public void stop() {
        route = null;
        goal = null;
        dodging = false;
    }

    /** Whether the player is on its way somewhere. */
    public boolean moving() {
        return route != null;
    }

    /**
     * Walks to {@code at}, running {@code mcTick} (the world's Minecraft tick: the clock, the boss, the pending hits) every
     * Minecraft tick until the player is there; returns the Minecraft ticks it took, or -1 when it didn't get there in
     * {@code maxMc} (blocked, or frozen all along).
     */
    public int walkTo(Vec3 at, int maxMc, Runnable mcTick) {
        if (Math.hypot(at.x - p.getX(), at.z - p.getZ()) < 0.05 && Math.abs(at.y - p.getY()) < 0.6) {
            stop();
            return 0; // already there: no time passes
        }
        goTo(at);
        for (int i = 0; i < maxMc; i++) {
            if (route == null) {
                return i;
            }
            mcTick.run();
            tick(); // a driver that doesn't tick the player itself (no-op if it did)
        }
        return route == null ? maxMc : -1;
    }

    /** MC ticks a walk of {@code blocks} takes at the run pace (rounded up). */
    public static int walkMc(double blocks) {
        return RealReaction.walkMc(blocks);
    }

    /** Where the player stops when its walk is blocked short of the goal (or the goal when it got there). */
    public Vec3 goal() {
        return goal;
    }

    private List<Vec3> plan(Vec3 at) {
        int sx = (int) Math.floor(p.getX());
        int sz = (int) Math.floor(p.getZ());
        int sy = feetY(p.getY());
        int gx = (int) Math.floor(at.x);
        int gz = (int) Math.floor(at.z);
        List<Vec3> out = new ArrayList<>();
        // a dodging player (no-heal mode) also routes round hazards and danger marks, except right where it starts (to step off)
        GridPath.Floor floor = (x, z, near) -> {
            boolean goalCell = x == gx && z == gz;
            if (!goalCell && !avoid.isEmpty() && avoided(x + 0.5, z + 0.5)) {
                return GridPath.NONE;
            }
            if (!goalCell && autoDodge && (Math.abs(x - sx) > 1 || Math.abs(z - sz) > 1)
                    && (dangerUnder(x + 0.5, z + 0.5, near) || Mechanics.hazardAt(world, new Vec3(x + 0.5, near, z + 0.5)))) {
                return GridPath.NONE;
            }
            return feet(x, z, near);
        };
        // the nearest column it can reach when the spot itself can't be (off the floor, behind the platform's barrier wall): a
        // player walks to the edge, never off it
        List<int[]> cells = GridPath.findNearest(sx, sz, sy, gx, gz, ROUTE_RADIUS, ROUTE_NODES, floor);
        int[] end = cells.isEmpty() ? new int[]{sx, sy, sz} : cells.get(cells.size() - 1);
        boolean reached = end[0] == gx && end[2] == gz;
        if (!cells.isEmpty()) {
            cells = GridPath.smooth(new int[]{sx, sy, sz}, cells, floor);
            for (int i = 0; i < cells.size() - 1; i++) {
                int[] c = cells.get(i);
                out.add(new Vec3(c[0] + 0.5, c[1], c[2] + 0.5));
            }
        }
        if (reached) {
            out.add(at); // the last point is the spot itself
        } else {
            // as close to the spot as the last reachable column lets it stand (its body inside the column)
            double x = Math.max(end[0] + 0.3, Math.min(end[0] + 0.7, at.x));
            double z = Math.max(end[2] + 0.3, Math.min(end[2] + 0.7, at.z));
            out.add(new Vec3(x, end[1], z));
        }
        planReached = reached;
        return out;
    }

    /** One Minecraft tick of walking along the route (nothing while frozen). */
    private void step() {
        if (route == null) {
            gravity();
            return;
        }
        if (FreezeRules.frozen(p)) {
            frozenMc++;
            return;
        }
        double budgetLeft = RealReaction.RUN_BLOCKS_PER_MC;
        for (int guard = 0; guard < 6 && budgetLeft > 1e-4 && route != null; guard++) {
            Vec3 wp = route.get(routeIndex);
            double dx = wp.x - p.getX();
            double dz = wp.z - p.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.02) {
                if (++routeIndex >= route.size()) {
                    route = null;
                    dodging = false;
                }
                continue;
            }
            double s = Math.min(budgetLeft, len);
            double mx = dx / len * s;
            double mz = dz / len * s;
            double bx = p.getX();
            double bz = p.getZ();
            face(dx, dz);
            p.move(MoverType.SELF, new Vec3(mx, p.onGround() ? -0.08 : vy, mz));
            fall();
            double moved = Math.hypot(p.getX() - bx, p.getZ() - bz);
            if (moved < s * 0.3 && stepUp(mx, mz)) {
                moved = Math.hypot(p.getX() - bx, p.getZ() - bz);
            }
            if (moved < s * 0.3) {
                if (++stuckMc > 6) {
                    // blocked for a third of a second: a fresh route from here, up to MAX_REPLANS times; then the player stops
                    // where it is (the walk's own checks then say what it missed)
                    stuckMc = 0;
                    if (goal != null && replans++ < MAX_REPLANS) {
                        route = plan(goal);
                        routeIndex = 0;
                    } else {
                        route = null;
                        dodging = false;
                        blockedWalks++;
                        lastBlocked = goal;
                    }
                }
                break;
            }
            stuckMc = 0;
            walked += moved;
            budgetLeft -= moved;
        }
        p.fallDistance = 0;
    }

    /** A full-block step: up a block, then on (as a jump does); back down when that didn't help. */
    private boolean stepUp(double mx, double mz) {
        if (!p.onGround() && vy > -0.2) {
            return false;
        }
        double y0 = p.getY();
        p.move(MoverType.SELF, new Vec3(0, 1.05, 0));
        if (p.getY() - y0 < 1.0) {
            p.move(MoverType.SELF, new Vec3(0, y0 - p.getY(), 0));
            return false;
        }
        double bx = p.getX();
        double bz = p.getZ();
        p.move(MoverType.SELF, new Vec3(mx, 0, mz));
        if (Math.hypot(p.getX() - bx, p.getZ() - bz) < 1e-3) {
            p.move(MoverType.SELF, new Vec3(0, y0 - p.getY(), 0));
            return false;
        }
        p.move(MoverType.SELF, new Vec3(0, -1.05, 0));
        vy = -0.08;
        return true;
    }

    /** Falls while standing on nothing (a walk off a step or a ledge). */
    private void gravity() {
        if (!p.onGround() || vy < -0.08) {
            p.move(MoverType.SELF, new Vec3(0, vy, 0));
            fall();
        }
        p.fallDistance = 0;
    }

    private void fall() {
        if (p.onGround()) {
            vy = -0.08;
        } else {
            vy = Math.max(-3.0, (vy - 0.08) * 0.98);
        }
    }

    private void face(double dx, double dz) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
    }

    /** Turns the player towards a point (where it looks: a boss, a clone). */
    public void faceTowards(Vec3 at) {
        face(at.x - p.getX(), at.z - p.getZ());
    }

    // ------------------------------------------------------------------ the world's columns

    private static int feetY(double y) {
        return (int) Math.floor(y + 0.01);
    }

    /** The feet height a player can stand at in column (x, z) near {@code nearY}: the same level, a step up or one down. */
    private int feet(int x, int z, int nearY) {
        for (int dy : new int[]{0, 1, -1}) {
            if (standsAt(x, nearY + dy, z)) {
                return nearY + dy;
            }
        }
        return GridPath.NONE;
    }

    private boolean standsAt(int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        if (!world.isLoaded(feet)) {
            return false;
        }
        BlockPos under = feet.below();
        BlockState floor = world.getBlockState(under);
        if (floor.getCollisionShape(world, under).isEmpty() || floor.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
            return false;
        }
        return world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getBlockState(feet.above()).getCollisionShape(world, feet.above()).isEmpty();
    }

    /** Whether a player may end a dodge at (x, z): ground under it, room for its body, no danger mark or hazard under it. */
    private boolean standable(double x, double z, int feetY, boolean avoidDanger) {
        int y = feet((int) Math.floor(x), (int) Math.floor(z), feetY);
        if (y == GridPath.NONE || Math.abs(y - feetY) > 1) {
            return false;
        }
        if (!avoidDanger) {
            return true;
        }
        return !dangerUnder(x, z, y) && !Mechanics.hazardAt(world, new Vec3(x, y, z));
    }

    /** Whether a danger-coloured floor mark (a strike, fire, acid, poison...; not a safe or glyph tile) is under the player's feet. */
    private boolean dangerUnder(double x, double z, int feetY) {
        double half = 0.3; // half the player's width
        for (double ox : new double[]{-half, half}) {
            for (double oz : new double[]{-half, half}) {
                BlockPos floor = new BlockPos((int) Math.floor(x + ox), feetY - 1, (int) Math.floor(z + oz));
                if (FloorMarkers.marked(world, floor)) {
                    HazardKind kind = kindOf(world.getBlockState(floor));
                    if (kind != null && kind != HazardKind.SAFE && kind != HazardKind.GLYPH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static HazardKind kindOf(BlockState state) {
        for (HazardKind k : HazardKind.values()) {
            if (state.is(k.block)) {
                return k;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ what happened

    /** Strikes the walk left although a player reacting at once couldn't have (each names the strike), empty when fair. */
    public List<String> budgetFailures() {
        return List.copyOf(budget);
    }

    /** Strikes that landed on the player while it walked out of them (names and distances). */
    public List<String> caughtWalking() {
        return List.copyOf(caughtWalking);
    }

    /** What would have killed the mortal player (attack, hit, hitpoints left), or null. */
    public String death() {
        return death;
    }

    public int ate() {
        return ate;
    }

    public int drank() {
        return drank;
    }

    /** Damage taken by attack name while mortal. */
    public Map<String, Integer> taken() {
        return taken;
    }

    /** Throws when a strike couldn't be escaped in time or the mortal player would have died. */
    public void assertFair(String what) {
        if (death != null) {
            throw new AssertionError(what + ": a correctly reacting player died: " + death + " (" + summary() + ")");
        }
        if (!budget.isEmpty()) {
            throw new AssertionError(what + ": " + budget.size() + " telegraphed strike(s) can't be walked out of in time by a "
                    + "player reacting at once: " + budget);
        }
    }

    /** "7 prayer switches, 3 dodges (longest 3.4 blocks), 41 blocks walked; ate 2/10, drank 1/4". */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(switches).append(" prayer switches, ").append(dodges).append(" dodges");
        if (dodges > 0) {
            sb.append(String.format(Locale.ROOT, " (longest %.1f blocks)", longestDodge));
        }
        sb.append(String.format(Locale.ROOT, ", %.0f blocks walked", walked));
        if (frozenMc > 0) {
            sb.append(", frozen ").append(frozenMc).append(" MC");
        }
        if (blockedWalks > 0 && lastBlocked != null) {
            sb.append(String.format(Locale.ROOT, ", %d walk(s) stopped short (last to %.1f, %.1f, %.1f)", blockedWalks,
                    lastBlocked.x, lastBlocked.y, lastBlocked.z));
        }
        if (mortal) {
            sb.append(", ate ").append(ate).append('/').append(foodGiven).append(" sharks, drank ").append(drank)
                    .append(" doses (").append(potionsGiven).append(" potions), ")
                    .append(Math.round(p.getHealth())).append(" hp left");
            if (!taken.isEmpty()) {
                sb.append(", took ").append(taken);
            }
        }
        if (!caughtWalking.isEmpty()) {
            sb.append(", caught walking: ").append(caughtWalking);
        }
        return sb.toString();
    }

    /** Puts the recorders back, makes the player as (in)vulnerable as before and stops playing it. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Mechanics.pendingListener = pendingBefore;
        HudNet.castListener = castBefore;
        HudNet.groundListener = groundBefore;
        if (previous != null) {
            ACTIVE.put(p.getUUID(), previous);
        } else {
            ACTIVE.remove(p.getUUID(), this);
        }
        if (mortal) {
            p.setInvulnerable(wasInvulnerable);
        }
        wornBefore.forEach(p::setItemSlot);
        wornBefore.clear();
    }

    // ------------------------------------------------------------------ death guard (no-heal mode)

    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            RealPlayer r = ACTIVE.get(player.getUUID());
            if (r == null || !r.mortal || r.closed || damage <= 0) {
                return;
            }
            String name = attack != null ? attack : style != null ? style.name().toLowerCase(Locale.ROOT) : "typeless";
            r.taken.merge(name, damage, Integer::sum);
            if (damage >= player.getHealth() && r.death == null && !player.isInvulnerable()) {
                r.death = String.format(Locale.ROOT, "%s hit %d on %.0f hitpoints (from %s)", name, damage, player.getHealth(),
                        attacker == null ? "nothing" : attacker.getName().getString());
                // a real player must not die mid-run: the hit is ignored, the check fails on it
                player.setInvulnerable(true);
            }
        });
        com.runecraft.platform.ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer sp) {
                RealPlayer r = ACTIVE.get(sp.getUUID());
                if (r != null && r.mortal && !r.closed) {
                    if (r.death == null) {
                        r.death = "killed by " + source.getMsgId() + " (" + Math.round(amount) + ")";
                    }
                    sp.setHealth(1f);
                    sp.setInvulnerable(true);
                    return false;
                }
            }
            return true;
        });
    }

    /** For tests: how many cues the player has seen and still remembers. */
    int cuesRemembered() {
        return cues.size();
    }

    static Map<String, Integer> copy(Map<String, Integer> m) {
        return new HashMap<>(m);
    }
}
