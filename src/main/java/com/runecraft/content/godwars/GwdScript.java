package com.runecraft.content.godwars;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.boss.StrikeMarkers;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.GwdDungeon;
import com.runecraft.osrs.content.godwars.WildernessBosses;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/**
 * Shared machinery for this module's boss scripts:
 * <ul>
 *   <li>an attack "rotation": {@link #pick} decides the next attack once, and it is kept until it lands
 *       (the engine asks for a style every tick);</li>
 *   <li>multi-target attacks ({@link #isMultiTarget}) are repeated on every other player in the room;</li>
 *   <li>delayed ground strikes (shadow traps, explosive books...) that can be dodged by moving, with the real-time floor and
 *       markers of every arena ({@link ArenaTiming});</li>
 *   <li>pet top-up so the combined pet chance matches the real OSRS rate.</li>
 * </ul>
 */
public abstract class GwdScript implements BossScript {
    /**
     * A marked spot that explodes after a delay, hitting players horizontally within {@code radius} blocks of it. It can hurt
     * when it has a max hit or a landing callback ({@code damaging}); the draw bookkeeping feeds its redraws and the recorder.
     */
    private static final class Strike {
        final ServerLevel world;
        final Vec3 pos;
        final long due;
        final int maxHit;
        final double radius;
        final ParticleOptions warning;
        final ParticleOptions blast;
        final BiConsumer<ServerPlayer, Integer> onHit;
        final String name;
        final boolean damaging;
        /** When it was made, on this script's clock (game ticks x 12 plus the Minecraft ticks into the game tick). */
        final long createdMc;
        /** The real Minecraft tick it was last drawn on (redraws every {@link ArenaTiming#REDRAW_MC}). */
        long lastDrawMc;
        int draws;

        Strike(ServerLevel world, Vec3 pos, long due, int maxHit, double radius, ParticleOptions warning, ParticleOptions blast,
               BiConsumer<ServerPlayer, Integer> onHit, String name, long createdMc) {
            this.world = world;
            this.pos = pos;
            this.due = due;
            this.maxHit = maxHit;
            this.radius = radius;
            this.warning = warning;
            this.blast = blast;
            this.onHit = onHit;
            this.name = name;
            this.damaging = maxHit > 0 || onHit != null;
            this.createdMc = createdMc;
        }
    }

    /** Self-test hook: every strike of these bosses as it lands, with its timing ({@link ArenaTiming.StrikeTrace}). */
    public static Consumer<ArenaTiming.StrikeTrace> strikeListener = t -> {
    };

    protected AttackDef current;
    protected long chosenAt = -1000;
    private boolean spreading;
    private final List<Strike> strikes = new ArrayList<>();
    /** The game tick this script last ran ({@link #gameTick}) and the real Minecraft tick it ran on: the strikes' clock. */
    private long strikeTick;
    private long strikeTickMc = Long.MIN_VALUE / 4;

    /** Chooses the next attack (called only when the previous one has landed). Null = engine default. */
    protected AttackDef pick(DataBossEntity boss, ServerPlayer target, double distance) {
        return null;
    }

    /** Whether this attack also hits every other player near the boss. */
    protected boolean isMultiTarget(AttackDef attack) {
        return false;
    }

    /** Hook called when an attack lands on a player; returns the damage to deal. */
    protected int landed(DataBossEntity boss, ServerPlayer player, int damage, AttackDef attack) {
        return damage;
    }

    /** Distance (centre to centre) within which this boss can melee. */
    protected static double meleeReach(DataBossEntity boss) {
        return boss.getBbWidth() / 2 + 1.6;
    }

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        long now = ActionManager.gameTick();
        BossDef def = boss.def();
        int speed = def == null ? 4 : def.attackSpeed;
        boolean stale = now - chosenAt > speed * 2L + 2;
        boolean outOfReach = current != null && current.melee() && distance > meleeReach(boss) + 2.5;
        if (current == null || stale || outOfReach) {
            current = pick(boss, target, distance);
            chosenAt = now;
        }
        return current;
    }

    /**
     * The pick is used up when the attack starts, so the next attack is picked afresh even when this one's hit never lands (a
     * projectile that fizzles, a swing the target stepped away from: hits are delivered in real time since round 41).
     */
    @Override
    public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
        current = null;
        return attack;
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        return onHitPlayer(boss, player, damage, type, null);
    }

    /** {@code attack}: the attack that landed (the boss may have picked its next one since); null for a built-in hit. */
    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
        AttackDef used = attack != null ? attack : current;
        if (spreading) {
            return landed(boss, player, damage, used);
        }
        int result = landed(boss, player, damage, used);
        if (used != null && isMultiTarget(used)) {
            splash(boss, used, player);
        }
        if (attack == null) {
            current = null;
        }
        return result;
    }

    /** Repeats an attack on every other player in the room within 20 blocks (each hit is rolled separately). */
    protected void splash(DataBossEntity boss, AttackDef attack, ServerPlayer except) {
        spreading = true;
        try {
            for (ServerPlayer p : roomPlayers(boss, 20)) {
                if (p != except) {
                    Mechanics.perform(boss, p, attack, 1.0);
                }
            }
        } finally {
            spreading = false;
        }
    }

    /** Repeats an attack on every other player within {@code radius} blocks of {@code centre} (an area spell's splash). */
    protected void splashAround(DataBossEntity boss, AttackDef attack, ServerPlayer centre, double radius) {
        if (spreading) {
            return;
        }
        spreading = true;
        try {
            for (ServerPlayer p : roomPlayers(boss, 40)) {
                if (p != centre && p.distanceToSqr(centre) <= radius * radius) {
                    Mechanics.perform(boss, p, attack, 1.0);
                }
            }
        } finally {
            spreading = false;
        }
    }

    /**
     * Lands the strikes that are due (their own game tick of the boss) and redraws the rest. Subclasses call it first from
     * their own game tick.
     */
    @Override
    public void gameTick(DataBossEntity boss, long gameTick) {
        strikeTick = gameTick;
        strikeTickMc = ActionManager.mcTick();
        Iterator<Strike> it = strikes.iterator();
        List<Strike> due = new ArrayList<>();
        while (it.hasNext()) {
            Strike s = it.next();
            if (gameTick >= s.due) {
                due.add(s);
                it.remove();
            }
        }
        redrawStrikes(); // for drivers that only run game ticks
        for (Strike s : due) {
            if (s.blast != null) {
                s.world.sendParticles(s.blast, s.pos.x, s.pos.y + 0.5, s.pos.z, 12, s.radius / 2, 0.4, s.radius / 2, 0.02);
            }
            strikeListener.accept(new ArenaTiming.StrikeTrace(s.createdMc, s.createdMc, gameTick * ArenaTiming.MC_PER_GAME_TICK,
                    s.draws, s.radius, s.maxHit, s.damaging, false));
            if (!boss.isAlive()) {
                continue;
            }
            GwdDungeon.Area in = room(boss);
            // What is drawn is what hits: horizontally inside the outline, within its height tolerance. // playability: OSRS
            // tests the strike's tiles; this was a 3D distance, which missed a player on a step inside the marked area
            for (ServerPlayer p : s.world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator()
                    && ArenaTiming.inStrike(pl.getX() - s.pos.x, pl.getY() - s.pos.y, pl.getZ() - s.pos.z, s.radius)
                    && inRoom(pl, in))) {
                int dmg = strikeDamage(boss, s.name, p, boss.getRandom().nextInt(s.maxHit + 1));
                if (s.name != null) {
                    com.runecraft.combat.CombatHooks.nameAttack(s.name, null); // "no damage from ..." Combat Achievements
                }
                int dealt = CombatHandler.applyDamage(boss, p, dmg);
                if (s.onHit != null) {
                    s.onHit.accept(p, dealt);
                }
            }
        }
    }

    /**
     * A landing strike's damage on a player, from its roll (0 to its max hit): what a script's prayer rule leaves of it (Nex's
     * Containment through Protect from Missiles). {@code name}: the strike's attack name, or null.
     */
    protected int strikeDamage(DataBossEntity boss, String name, ServerPlayer player, int rolled) {
        return rolled;
    }

    /** Redraws the pending strike markers between game ticks (DataBossEntity runs this every Minecraft tick). */
    @Override
    public void tick(DataBossEntity boss) {
        redrawStrikes();
    }

    /**
     * Marks a spot that explodes after {@code delay} game ticks; returns the game tick it lands on (cosmetics that go with it,
     * such as Vet'ion's lightning bolts, land then too).
     */
    protected long strike(DataBossEntity boss, Vec3 pos, int delay, int maxHit, double radius, ParticleOptions warning,
                          ParticleOptions blast, BiConsumer<ServerPlayer, Integer> onHit) {
        return strike(null, boss, pos, delay, maxHit, radius, warning, blast, onHit);
    }

    /**
     * {@link #strike} whose damage carries an attack name (e.g. "green explosion", for the "no damage from" tasks). A strike
     * that can hurt lands no sooner than {@link ArenaTiming#STRIKE_FLOOR_TICKS} game ticks (1.8 s) after it is made, measured
     * from the Minecraft tick it was made on, and its marker is drawn at once at its true radius, then redrawn every
     * {@link ArenaTiming#REDRAW_MC} Minecraft ticks, brightening with a rising bell in its last moments.
     * // playability: OSRS lands these from 2 ticks after marking them (Nex's 'Contain this!': 2), and the marker here was a
     * sparse spray first drawn a game tick late
     */
    protected long strike(String name, DataBossEntity boss, Vec3 pos, int delay, int maxHit, double radius, ParticleOptions warning,
                          ParticleOptions blast, BiConsumer<ServerPlayer, Integer> onHit) {
        int into = mcIntoTick();
        // a script whose game ticks paused (a boss walking home) counts from the server's game tick, not a stale one
        long base = ActionManager.mcTick() - strikeTickMc > ArenaTiming.MC_PER_GAME_TICK
                ? Math.max(strikeTick, ActionManager.gameTick()) : strikeTick;
        long due = ArenaTiming.strikeDue(base, delay, maxHit > 0 || onHit != null, into);
        Strike s = new Strike((ServerLevel) boss.level(), pos, due, maxHit, radius, warning, blast, onHit, name,
                base * ArenaTiming.MC_PER_GAME_TICK + into);
        strikes.add(s);
        drawStrike(s, false); // drawn right here, as it is made
        return due;
    }

    /** Whether any strike is still pending. */
    protected boolean strikesPending() {
        return !strikes.isEmpty();
    }

    /** Minecraft ticks since this script's game tick began (0 for strikes made in it). */
    private int mcIntoTick() {
        return (int) Math.max(0, Math.min(ArenaTiming.MC_PER_GAME_TICK - 1, ActionManager.mcTick() - strikeTickMc));
    }

    private void drawStrike(Strike s, boolean landing) {
        s.lastDrawMc = ActionManager.mcTick();
        s.draws++;
        StrikeMarkers.ring(s.world, s.pos, s.radius, s.warning, landing, ArenaTiming.ringPoints(s.radius, strikes.size()));
    }

    /**
     * Redraws pending markers every {@link ArenaTiming#REDRAW_MC} Minecraft ticks; in a strike's last
     * {@link ArenaTiming#FINAL_CUE_MC} its outline brightens and a bell ticks, rising as it nears (one bell per boss).
     * // playability: OSRS draws the marker once
     */
    private void redrawStrikes() {
        if (strikes.isEmpty()) {
            return;
        }
        long mc = ActionManager.mcTick();
        long clock = strikeTick * ArenaTiming.MC_PER_GAME_TICK + mcIntoTick();
        Strike bell = null;
        for (Strike s : strikes) {
            if (!ArenaTiming.redrawDue(s.lastDrawMc, mc)) {
                continue;
            }
            boolean landing = ArenaTiming.finalCue(s.due * ArenaTiming.MC_PER_GAME_TICK, clock);
            drawStrike(s, landing);
            if (landing && s.damaging && (bell == null || s.due < bell.due)) {
                bell = s;
            }
        }
        if (bell != null) {
            StrikeMarkers.bell(bell.world, bell.pos, bell.due * ArenaTiming.MC_PER_GAME_TICK - clock);
        }
    }

    protected static List<ServerPlayer> players(DataBossEntity boss, double radius) {
        return ((ServerLevel) boss.level()).getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(boss) <= radius * radius);
    }

    /**
     * The players a room-wide attack reaches: those within {@code radius} blocks who stand in the boss's room. In the God
     * Wars Dungeon that is its chamber only (OSRS: nobody outside the walls, by the door or in Nex's refuge, is hit);
     * outside the dungeon everyone in range.
     */
    protected List<ServerPlayer> roomPlayers(DataBossEntity boss, double radius) {
        List<ServerPlayer> near = new ArrayList<>(players(boss, radius));
        GwdDungeon.Area in = room(boss);
        if (in != null) {
            near.removeIf(p -> !inRoom(p, in));
        }
        return near;
    }

    /** The dungeon area the boss last stood in (a doorway tile has none), null once it is outside the dungeon. */
    private GwdDungeon.Area room;

    /** The God Wars Dungeon room (chamber) the boss fights in, or null when it is outside the dungeon. */
    protected GwdDungeon.Area room(DataBossEntity boss) {
        int x = boss.getBlockX();
        int y = boss.getBlockY();
        int z = boss.getBlockZ();
        GwdDungeon.Area here = GwdDungeon.areaAt(x, y, z);
        if (here != null) {
            room = here;
        } else if (!GwdDungeon.contains(x, y, z)) {
            room = null;
        }
        return room;
    }

    private static boolean inRoom(ServerPlayer p, GwdDungeon.Area room) {
        return GwdDungeon.inRoom(room, p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    /** Overhead-style shout shown to nearby players. */
    protected static void shout(DataBossEntity boss, String text) {
        BossDef def = boss.def();
        String name = def == null ? "Boss" : def.name;
        for (ServerPlayer p : players(boss, 40)) {
            p.displayClientMessage(Component.literal(name + ": " + text).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    /**
     * True when the boss stands in the God Wars Dungeon: its bodyguards are then the dungeon's own (they respawn on their
     * own tiles, {@code GwdSpawner}), not summoned with it.
     */
    protected static boolean inDungeon(DataBossEntity boss) {
        return GwdDungeon.areaAt(boss.getBlockX(), boss.getBlockY(), boss.getBlockZ()) != null;
    }

    /**
     * A bodyguard next to the boss: one of this kind still alive in the room from an earlier fight stays (OSRS bodyguards
     * outlive their general), else a new one; either way it joins the boss's lair.
     */
    protected static Mob spawnGuard(DataBossEntity boss, String id, double dx, double dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type != null) {
            for (var e : ((ServerLevel) boss.level()).getEntities(type, boss.getBoundingBox().inflate(48), e -> e.isAlive())) {
                if (e instanceof Mob mob) {
                    boss.claimMinion(mob);
                    return mob;
                }
            }
        }
        return spawnMinion(boss, id, dx, dz);
    }

    /** Spawns a module creature next to the boss and sets it on the boss's target. */
    protected static Mob spawnMinion(DataBossEntity boss, String id, double dx, double dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            return null;
        }
        ServerLevel world = (ServerLevel) boss.level();
        var e = type.create(world);
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(boss.getX() + dx, boss.getY(), boss.getZ() + dz, boss.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (boss.getTarget() != null) {
            mob.setTarget(boss.getTarget());
        }
        boss.claimMinion(mob); // a reset of the boss clears it
        world.addFreshEntity(mob);
        return mob;
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        topUpPet(boss, killer);
    }

    /** Extra pet roll so the overall chance equals the boss's real OSRS pet rate. */
    protected static void topUpPet(DataBossEntity boss, ServerPlayer killer) {
        BossDef def = boss.def();
        if (def == null || def.pet == null || killer == null) {
            return;
        }
        double chance = GodWarsFactions.extraPetChance(WildernessBosses.realPetRate(def.id));
        if (chance > 0 && boss.getRandom().nextDouble() < chance) {
            givePet(killer, def.pet);
        }
    }

    static void givePet(ServerPlayer player, String pet) {
        Inv.give(player, pet, 1);
        CollectionLog.obtain(player, pet);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
    }
}
