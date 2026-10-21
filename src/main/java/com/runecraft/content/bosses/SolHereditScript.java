package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.action.ActionManager;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.minigames.SolHeredit;
import com.runecraft.osrs.content.minigames.SolHeredit.Attack;
import com.runecraft.osrs.content.minigames.SolHeredit.Grapple;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.platform.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sol Heredit ({@link SolHeredit}): spear and shield AoEs dodged by position (the safe tiles are drawn from the wind-up; the
 * dust covers the pattern's tiles, prayer doesn't help), the triple parry (Protect from Melee on as each called-out hit
 * lands), the grapple (a gear slot to defend within {@link SolHeredit#GRAPPLE_WINDOW_TICKS} ticks: the hotbar keys 1-5 in the
 * Colosseum ({@code ColosseumSession.grappleKeys}) or the chat links running {@code /solheredit parry <slot>}; the last tick
 * is a perfect parry and the next attack within 5 ticks is a max hit), phase transitions with molten sand and light-beam
 * crystals (their lasers are strikes: the outline drawn at once, the hit test horizontal), and the enrage at 10%.
 */
public final class SolHereditScript extends ScriptedBoss {
    /** Players with a grapple to defend, and the script that called it. */
    private static final Map<UUID, SolHereditScript> GRAPPLED = new HashMap<>();
    /** Perfect parries: player to the last game tick their next attack is a max hit. */
    private static final Map<UUID, Long> PERFECT = new HashMap<>();

    private final Random random = new Random();
    private int phase = 1;
    private Attack previous;
    private boolean afterPhase;
    private int wait = 3;
    private int rooted;
    // Pending regular attack.
    private Attack pending;
    private long pendingLands;
    private BlockPos pendingCentre;
    private int[] pendingFacing;
    // Triple parry: the tick of the start, hits landed so far.
    private long tripleStart = -1;
    private long tripleNext;
    private int tripleHit;
    // Grapple.
    private Grapple grapple;
    private long grappleStart;
    private UUID grappled;
    private boolean defended;
    /** The grapple was answered (right or wrong slot): later answers are refused, so a wrong slot can't be taken back. */
    private boolean answered;
    /** The triple-attack hit whose cue (spear glint, "1/3") was shown, -1 for none yet. */
    private int tripleCued = -1;
    // Protect from Melee tracking (per target).
    private long protectSince = -1;
    /** The real Minecraft tick the pending regular attack's tiles were last drawn on. */
    private long aoeDrawnMc;
    // Light beams (their lasers are strikes).
    private long nextLaser;
    private int beamIndex;
    private long nextSand;
    private boolean opened;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                Commands.literal("solheredit").then(Commands.literal("parry")
                        .then(Commands.argument("slot", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                parry(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "slot"));
                            }
                            return 1;
                        })))));
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public boolean alwaysMaxHit(ServerPlayer player, LivingEntity target, AttackType type) {
                Long until = PERFECT.get(player.getUUID());
                return until != null && com.runecraft.action.ActionManager.gameTick() <= until && isSol(target);
            }

            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                if (isSol(target)) {
                    PERFECT.remove(player.getUUID());
                }
            }
        });
    }

    private static boolean isSol(LivingEntity e) {
        return e instanceof DataBossEntity b && b.def() != null && b.def().id.equals("sol_heredit");
    }

    /** {@code /solheredit parry <slot>}: defends the called gear slot; returns whether it blocked the grapple. */
    public static boolean parry(ServerPlayer p, String slot) {
        SolHereditScript s = GRAPPLED.get(p.getUUID());
        if (s == null || s.grapple == null) {
            Msg.game(p, "There is nothing to defend against.");
            return false;
        }
        if (s.answered) {
            Msg.game(p, "You have already answered Sol Heredit's grapple.");
            return s.defended;
        }
        s.answered = true;
        Grapple g = Grapple.bySlot(slot);
        if (g != s.grapple) {
            Msg.red(p, "You defend the wrong slot!");
            s.grappleTicksLeftWrong();
            return false;
        }
        int tick = (int) (s.now - s.grappleStart);
        s.defended = true;
        if (SolHeredit.perfectParry(tick)) {
            PERFECT.put(p.getUUID(), s.now + SolHeredit.PERFECT_PARRY_TICKS);
            Msg.game(p, "You perfectly parry Sol Heredit's grapple!");
        } else {
            Msg.game(p, "You successfully defend from Sol Heredit's grapple!");
        }
        return true;
    }

    /** The slot an unanswered grapple on {@code p} calls, or null (the Colosseum answers it from the hotbar keys). */
    public static Grapple calledFor(ServerPlayer p) {
        SolHereditScript s = GRAPPLED.get(p.getUUID());
        return s == null || s.answered || s.grapple == null ? null : s.grapple;
    }

    /** A wrong slot leaves the grapple undefended: it lands on the next game tick (and {@link #answered} refuses a retry). */
    private void grappleTicksLeftWrong() {
        defended = false;
        grappleStart = now - SolHeredit.GRAPPLE_WINDOW_TICKS;
    }

    /** The current phase (1-6), the last attack and whether a grapple is waiting (self-test). */
    public int phase() {
        return phase;
    }

    public Attack previous() {
        return previous;
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return false; // every attack is scripted below
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        cleanUp(boss);
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        cleanUp(boss);
    }

    private void cleanUp(DataBossEntity boss) {
        GRAPPLED.values().removeIf(s -> s == this);
        Mechanics.clearHazards(BossFx.world(boss), boss.getBoundingBox().inflate(24, 6, 24));
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (!boss.isAlive()) {
            cleanUp(boss);
            return;
        }
        if (!opened && target != null) {
            opened = true;
            BossFx.say(boss, "Sol Heredit: " + SolHeredit.OPENING_LINE);
        }
        if (rooted > 0) {
            rooted--;
            boss.getNavigation().stop();
        }
        if (target != null) {
            boolean on = BossFx.protects(target, Prayer.PROTECT_FROM_MELEE);
            protectSince = on ? (protectSince < 0 ? now : protectSince) : -1;
        }
        int nowPhase = SolHeredit.phase(percent(boss));
        if (nowPhase > phase) {
            for (int p = phase + 1; p <= nowPhase; p++) {
                enterPhase(boss, target, p);
            }
            phase = nowPhase;
        }
        tickPending(boss);
        tickTriple(boss, target);
        tickGrapple(boss);
        tickLasers(boss, target);
        if (phase >= 6 && target != null && now >= nextSand) {
            nextSand = now + SolHeredit.ENRAGE_SAND_TICKS;
            sand(boss, target, 1, false);
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (target == null || pending != null || tripleStart >= 0 || grapple != null) {
            return;
        }
        Attack a = SolHeredit.next(previous, phase, afterPhase, random);
        BlockPos c = boss.blockPosition();
        BlockPos t = target.blockPosition();
        if (!a.special() && Math.max(Math.abs(t.getX() - c.getX()), Math.abs(t.getZ() - c.getZ())) > SolHeredit.MELEE_REACH) {
            return; // he must be next to the player to start a regular attack
        }
        afterPhase = false;
        previous = a;
        // wait counts down on the ticks in between (the ScriptedBoss convention), so the next attack starts attackDelay ticks on.
        wait = Math.max(1, SolHeredit.attackDelay(a, phase)) - 1;
        switch (a) {
            case TRIPLE -> {
                tripleStart = now;
                tripleHit = 0;
                tripleCued = -1;
                tripleNext = now + SolHeredit.tripleGap(0, phase);
                BossFx.warn(boss, "Sol Heredit grips his spear tightly...");
            }
            case GRAPPLE -> startGrapple(boss, target);
            default -> {
                pending = a;
                pendingLands = now + SolHeredit.AOE_LAND_TICKS;
                pendingCentre = c;
                pendingFacing = SolHeredit.facing(t.getX() - c.getX(), t.getZ() - c.getZ());
                rooted = SolHeredit.AOE_ROOT_TICKS - 1; // this tick plus the next AOE_ROOT_TICKS - 1
                boss.getNavigation().stop();
                BossFx.warn(boss, a.spear() ? "Sol Heredit draws back his spear!" : "Sol Heredit raises his shield!");
                drawAoe(BossFx.world(boss), true);
            }
        }
    }

    private void enterPhase(DataBossEntity boss, ServerPlayer target, int p) {
        // The BossDef's phases (same thresholds) announce SolHeredit.PHASE_LINES.
        afterPhase = true;
        if (target != null) {
            sand(boss, target, p >= 6 ? SolHeredit.ENRAGE_INITIAL_SAND : SolHeredit.SAND_PER_PHASE, true);
        }
        nextLaser = Math.min(nextLaser == 0 ? Long.MAX_VALUE : nextLaser, now + 6);
    }

    /** Beams of light around the player (one on the player's tile when {@code onPlayer}) leaving molten sand 2 ticks later. */
    private void sand(DataBossEntity boss, ServerPlayer target, int count, boolean onPlayer) {
        ServerLevel world = BossFx.world(boss);
        BlockPos t = target.blockPosition();
        for (int i = 0; i < count; i++) {
            int dx = i == 0 && onPlayer ? 0 : random.nextInt(2 * SolHeredit.SAND_RADIUS + 1) - SolHeredit.SAND_RADIUS;
            int dz = i == 0 && onPlayer ? 0 : random.nextInt(2 * SolHeredit.SAND_RADIUS + 1) - SolHeredit.SAND_RADIUS;
            Vec3 at = BossFx.ground(world, t.getX() + dx + 0.5, t.getZ() + dz + 0.5, target.getY());
            BossFx.column(world, at, ParticleTypes.END_ROD, 4);
            // Exact: the beam only leaves the molten sand (a hazard, which arms on its own delay); it deals no damage itself.
            strikeExact(boss, at, SolHeredit.SAND_DELAY_TICKS, 0, 0.5, ParticleTypes.END_ROD, (hit, pos) ->
                    Mechanics.addHazard(world, pos, SolHeredit.SAND_MAX_DAMAGE, 6000, boss));
        }
    }

    @Override
    public void tick(DataBossEntity boss) {
        super.tick(boss);
        if (pending != null && boss.isAlive() && ArenaTiming.redrawDue(aoeDrawnMc, ActionManager.mcTick())) {
            drawAoe(BossFx.world(boss), false);
        }
    }

    /** Safe-tile marker: bright green, well apart from the danger orange. */
    private static final DustParticleOptions SAFE_TILE = new DustParticleOptions(new Vector3f(0.45f, 1.0f, 0.45f), 1.5f);
    private static final DustParticleOptions DANGER_TILE = new DustParticleOptions(new Vector3f(1.0f, 0.63f, 0.13f), 1.0f);

    /**
     * Draws the pending regular attack: every safe tile next to the pattern (the shields' safe ring, the spears' outline) in
     * green, redrawn every few Minecraft ticks until the dust lands, and once at the wind-up a sparse orange fill of the tiles
     * it hits. // playability: OSRS shows only the animation, and the safe tiles are learned
     */
    private void drawAoe(ServerLevel world, boolean withDanger) {
        aoeDrawnMc = ActionManager.mcTick();
        Attack a = pending;
        if (a == null || pendingCentre == null) {
            return;
        }
        int fx = pendingFacing[0];
        int fz = pendingFacing[1];
        double y = pendingCentre.getY() + 0.15;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean hit = SolHeredit.hits(a, dx, dz, fx, fz);
                double x = pendingCentre.getX() + dx + 0.5;
                double z = pendingCentre.getZ() + dz + 0.5;
                if (!hit && bordersHit(a, dx, dz, fx, fz)) {
                    world.sendParticles(SAFE_TILE, x, y, z, 2, 0.22, 0.0, 0.22, 0.0);
                } else if (hit && withDanger && (dx + dz) % 2 == 0) {
                    world.sendParticles(DANGER_TILE, x, y, z, 1, 0.2, 0.0, 0.2, 0.0);
                }
            }
        }
    }

    private static boolean bordersHit(Attack a, int dx, int dz, int fx, int fz) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if ((ox != 0 || oz != 0) && SolHeredit.hits(a, dx + ox, dz + oz, fx, fz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void tickPending(DataBossEntity boss) {
        if (pending == null || now < pendingLands) {
            return;
        }
        Attack a = pending;
        pending = null;
        ServerLevel world = BossFx.world(boss);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (SolHeredit.hits(a, dx, dz, pendingFacing[0], pendingFacing[1]) && (dx + dz) % 2 == 0) {
                    world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pendingCentre.getX() + dx + 0.5, pendingCentre.getY() + 0.2,
                            pendingCentre.getZ() + dz + 0.5, 1, 0.3, 0.05, 0.3, 0.0);
                }
            }
        }
        for (ServerPlayer p : BossFx.players(boss, 16)) {
            // within AOE_EDGE_LENIENCY of a safe tile counts as safe // playability: OSRS tests the one tile stood on
            if (SolHeredit.hitsLenient(a, p.getX() - pendingCentre.getX(), p.getZ() - pendingCentre.getZ(), pendingFacing[0],
                    pendingFacing[1])) {
                CombatHooks.nameAttack(a.spear() ? "spear" : "shield_bash", null);
                CombatHandler.applyDamage(boss, p, random.nextInt(SolHeredit.AOE_MAX_HIT + 1));
            }
        }
    }

    private void tickTriple(DataBossEntity boss, ServerPlayer target) {
        if (tripleStart < 0) {
            return;
        }
        if (tripleCued < tripleHit && now >= tripleNext - SolHeredit.TRIPLE_CUE_TICKS) {
            tripleCued = tripleHit;
            cueTriple(boss, tripleHit);
        }
        if (now < tripleNext) {
            return;
        }
        if (target != null) {
            // Protect from Melee on as the hit lands blocks it (SolHeredit.parried). // playability: OSRS wants it switched on
            // the tick before each hit and knocks off a prayer left on early: a one-tick flick, three times
            boolean on = BossFx.protects(target, Prayer.PROTECT_FROM_MELEE);
            CombatHooks.nameAttack("triple_attack", null);
            CombatHandler.applyDamage(boss, target, SolHeredit.parried(on, protectSince, now) ? 0
                    : SolHeredit.tripleDamage(tripleHit, phase));
        }
        tripleHit++;
        if (tripleHit >= 3) {
            tripleStart = -1;
            tripleCued = -1;
        } else {
            tripleNext = now + SolHeredit.tripleGap(tripleHit, phase);
        }
    }

    /**
     * A triple-attack hit comes in {@link SolHeredit#TRIPLE_CUE_TICKS} game ticks (24 Minecraft ticks): his spear glints (a
     * column of light and a chime, higher each hit) and "Spear 1/3" is called out. // playability: OSRS gives the animation
     */
    private void cueTriple(DataBossEntity boss, int hit) {
        ServerLevel world = BossFx.world(boss);
        Vec3 tip = boss.position().add(0, boss.getBbHeight() + 0.3, 0);
        BossFx.column(world, tip, ParticleTypes.END_ROD, 4);
        world.sendParticles(ParticleTypes.FLASH, tip.x, tip.y + 1.0, tip.z, 1, 0, 0, 0, 0);
        BossFx.sound(boss, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f + 0.3f * hit);
        for (ServerPlayer p : BossFx.players(boss, 24)) {
            Msg.telegraph(p, "Spear " + (hit + 1) + "/3 - Protect from Melee!", ChatFormatting.RED);
        }
    }

    /** Self-test: Sol Heredit starts his triple parry now (its hits land 3, 6 and 9 or 10 ticks on). */
    public void selfTestTriple() {
        previous = Attack.TRIPLE;
        tripleStart = now;
        tripleHit = 0;
        tripleCued = -1;
        tripleNext = now + SolHeredit.tripleGap(0, phase);
        wait = Math.max(wait, SolHeredit.attackDelay(Attack.TRIPLE, phase));
    }

    /** Self-test: whether the triple parry is under way. */
    public boolean selfTestTripleRunning() {
        return tripleStart >= 0;
    }

    /** Self-test: the last triple-attack thrust (0-2) called out, -1 before the first. */
    public int selfTestTripleCued() {
        return tripleCued;
    }

    private void startGrapple(DataBossEntity boss, ServerPlayer target) {
        startGrapple(boss, target, Grapple.values()[random.nextInt(Grapple.values().length)]);
    }

    /** Self-test: Sol Heredit calls a grapple on {@code slot} now. */
    public void selfTestGrapple(DataBossEntity boss, ServerPlayer target, Grapple slot) {
        startGrapple(boss, target, slot);
    }

    /** Whether the player's next attack on Sol Heredit is a perfect parry's max hit (self-test). */
    public static boolean perfectReady(ServerPlayer p) {
        Long until = PERFECT.get(p.getUUID());
        return until != null && com.runecraft.action.ActionManager.gameTick() <= until;
    }

    private void startGrapple(DataBossEntity boss, ServerPlayer target, Grapple slot) {
        grapple = slot;
        grappleStart = now;
        grappled = target.getUUID();
        defended = false;
        answered = false;
        GRAPPLED.put(target.getUUID(), this);
        BossFx.say(boss, "Sol Heredit: " + grapple.shout);
        MutableComponent line = Component.literal("Defend: ").withStyle(ChatFormatting.GRAY);
        for (Grapple g : Grapple.values()) {
            String label = SolHeredit.hotbarKey(g) + " " + g.slot.substring(0, 1).toUpperCase() + g.slot.substring(1);
            line.append(Component.literal("[" + label + "]").withStyle(st -> st.withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/solheredit parry " + g.slot))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Defend your " + g.slot)))))
                    .append(Component.literal(" "));
        }
        target.displayClientMessage(line, false);
    }

    private void tickGrapple(DataBossEntity boss) {
        if (grapple == null || now - grappleStart < SolHeredit.GRAPPLE_WINDOW_TICKS) {
            return;
        }
        ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(grappled);
        if (p == null) {
            for (ServerPlayer near : BossFx.players(boss, 24)) {
                if (near.getUUID().equals(grappled)) {
                    p = near;
                }
            }
        }
        if (p != null && !defended && boss.isAlive()) {
            CombatHooks.nameAttack("grapple", null);
            CombatHandler.applyDamage(boss, p, random.nextInt(SolHeredit.GRAPPLE_MAX_HIT + 1));
            unequip(p, grapple);
        }
        GRAPPLED.remove(grappled);
        grapple = null;
    }

    /**
     * An undefended grapple tears the called slot's item off into the inventory (wiki: "puts it into the player's inventory
     * if not blocked in time"). Body, legs and feet are the armour slots; the back and hands slots are worn as accessories
     * here and stay on. With no free inventory slot the item stays worn: it is never put on the arena floor, which is
     * cleared when the wave ends. // approx (the wiki doesn't say what a full inventory does)
     */
    static boolean unequip(ServerPlayer p, Grapple g) {
        net.minecraft.world.entity.EquipmentSlot slot = SolHeredit.armourSlot(g) == null ? null
                : net.minecraft.world.entity.EquipmentSlot.valueOf(SolHeredit.armourSlot(g));
        if (slot == null) {
            return false;
        }
        net.minecraft.world.item.ItemStack worn = p.getItemBySlot(slot);
        if (worn.isEmpty() || p.getInventory().getFreeSlot() < 0) {
            return false;
        }
        net.minecraft.world.item.ItemStack copy = worn.copy();
        if (!p.getInventory().add(copy)) {
            return false; // nothing was moved (add fills a free slot whole, but never risk the item)
        }
        p.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        Msg.red(p, "Sol Heredit tears your " + worn.getHoverName().getString() + " off!");
        return true;
    }

    /**
     * Light-beam crystals: each shot targets the player's tile and lands a few ticks later, as a strike (round 42): its outline
     * ({@link SolHeredit#LASER_RADIUS} round the tile's centre) drawn at once and redrawn, a bell as it lands, and the hit test
     * horizontal within that outline. // playability: OSRS hits the one tile; round 41 kept it tile-exact under a thin ring
     */
    private void tickLasers(DataBossEntity boss, ServerPlayer target) {
        ServerLevel world = BossFx.world(boss);
        int beams = SolHeredit.beams(phase);
        if (target == null || beams == 0 || now < nextLaser) {
            return;
        }
        boolean enraged = phase >= 6;
        nextLaser = now + Math.max(4, (enraged ? SolHeredit.ENRAGE_LASER_INTERVAL_TICKS : SolHeredit.LASER_INTERVAL_TICKS) / beams);
        BlockPos t = target.blockPosition();
        // The crystal circles the arena's edge; it shines a harmless beam at the player's tile, then fires.
        double angle = (beamIndex++ % 8) * Math.PI / 4;
        Vec3 crystal = new Vec3(t.getX() + 0.5 + Math.cos(angle) * 8, target.getY() + 1, t.getZ() + 0.5 + Math.sin(angle) * 8);
        world.sendParticles(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, 6, 0.2, 0.2, 0.2, 0.0);
        Vec3 at = new Vec3(t.getX() + 0.5, Math.floor(target.getY()), t.getZ() + 0.5);
        // the beam of light from the crystal to the tile, then the strike on it
        Vec3 d = at.subtract(crystal);
        for (int i = 0; i <= 10; i++) {
            Vec3 p = crystal.add(d.scale(i / 10.0));
            world.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        // the enraged 2-tick warning gets the strike floor (ScriptedBoss.strike) // playability: OSRS 3 ticks to step off, 2
        // when enraged
        namedStrike("laser", boss, at, enraged ? SolHeredit.ENRAGE_LASER_WARNING_TICKS : SolHeredit.LASER_WARNING_TICKS,
                SolHeredit.LASER_MAX_HIT, SolHeredit.LASER_RADIUS, ParticleTypes.END_ROD);
        Msg.telegraph(target, "A beam of light locks onto you!", ChatFormatting.GOLD);
    }
}
