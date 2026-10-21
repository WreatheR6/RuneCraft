package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Telegraphed ground strikes and delayed tasks used by raid mechanics (falling crystals, lightning,
 * blood splats, death balls...). Everything runs on the server thread: strikes and tasks land once per game tick, while
 * markers and cues are redrawn every few Minecraft ticks ({@link #redraw}).
 */
public final class RaidEffects {
    /**
     * A marked area that explodes after a delay. {@code protectedShare}: the share of the roll a matching protection
     * prayer lets through (0 blocks it, the default unless piercing; Kephri's fireballs let a third through). The rest is
     * what the arena-cue recorder sees: when it was created and first drawn (raid-clock Minecraft ticks), how often it was
     * drawn, and whether it was created exact (logic that keeps its tick).
     */
    private static final class Strike {
        final ServerLevel world;
        final Vec3 pos;
        final double radius;
        final int maxHit;
        final AttackType style;
        final boolean piercing;
        final long due;
        final LivingEntity source;
        final ParticleOptions warn;
        final double protectedShare;
        final boolean exact;
        final long createdMc;
        long firstDrawnMc = -1;
        int draws;

        Strike(ServerLevel world, Vec3 pos, double radius, int maxHit, AttackType style, boolean piercing, long due,
               LivingEntity source, ParticleOptions warn, double protectedShare, boolean exact, long createdMc) {
            this.world = world;
            this.pos = pos;
            this.radius = radius;
            this.maxHit = maxHit;
            this.style = style;
            this.piercing = piercing;
            this.due = due;
            this.source = source;
            this.warn = warn;
            this.protectedShare = protectedShare;
            this.exact = exact;
            this.createdMc = createdMc;
        }
    }

    private record Task(long due, Runnable run) {
    }

    /** A marker redrawn every few Minecraft ticks until the raid clock reaches {@code until} or it says to stop. */
    private record Cue(long until, BooleanSupplier draw) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Cue> CUES = new ArrayList<>();
    /** Game ticks the raid clock runs ahead of the server's (the self-test fast-forwards raids, see {@link #advance}). */
    private static long skew;
    /** Delayed tasks that threw (logged and skipped); the self-test fails on any. */
    private static int failures;
    private static String lastFailure;
    /** The Minecraft tick the last raid game tick ran on (a strike made between game ticks still gets its full lead). */
    private static long boundaryMc;

    /** Self-test hook: every strike as it lands, with its timing ({@link ArenaTiming.StrikeTrace}, raid-clock Minecraft ticks). */
    public static Consumer<ArenaTiming.StrikeTrace> strikeListener = t -> {
    };

    private RaidEffects() {
    }

    /**
     * The raid clock in game ticks: the server's game tick, plus whatever the self-test fast-forwarded. Every raid timer
     * (delayed tasks, strikes, room deadlines, the raid timer) reads it, so a raid can be driven tick by tick in one go.
     */
    public static long now() {
        return ActionManager.gameTick() + skew;
    }

    /** Self-test: moves the raid clock forward by {@code ticks} game ticks (it never goes back). */
    static void advance(int ticks) {
        skew += Math.max(0, ticks);
    }

    /** Delayed tasks that threw so far (self-test). */
    static int failures() {
        return failures;
    }

    /** The last failed delayed task's exception, or null (self-test). */
    static String lastFailure() {
        return lastFailure;
    }

    /** Runs {@code run} after {@code gameTicks} game ticks. */
    public static void later(int gameTicks, Runnable run) {
        TASKS.add(new Task(now() + Math.max(1, gameTicks), run));
    }

    /**
     * Minecraft ticks into the current raid game tick (0 on the game tick itself, and in the self-test's driven raids). On a
     * boundary Minecraft tick the server's game tick ({@link #now}) has already moved on before RaidManager runs this raid
     * game tick, so the count restarts there rather than reading 11 Minecraft ticks into the new game tick.
     */
    private static int mcIntoTick() {
        return ArenaTiming.mcIntoTick(ActionManager.mcTick(), boundaryMc);
    }

    /** The raid clock in Minecraft ticks: game ticks x 12, plus the Minecraft ticks into the current one. */
    public static long clockMc() {
        return now() * ArenaTiming.MC_PER_GAME_TICK + mcIntoTick();
    }

    /**
     * Marks {@code pos}; after {@code delay} game ticks every player within {@code radius} takes up to
     * {@code maxHit} (protection prayers block it unless piercing).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, boolean piercing) {
        add(world, pos, radius, maxHit, delay, source, warn, style, piercing, piercing ? 1.0 : 0.0, false);
    }

    /**
     * {@link #strike} whose damage a matching protection prayer only reduces: {@code protectedShare} of the roll still
     * lands (at least Quiet Prayers' share in ToA).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, double protectedShare) {
        add(world, pos, radius, maxHit, delay, source, warn, style, false, Math.max(0, Math.min(1, protectedShare)), false);
    }

    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn) {
        strike(world, pos, radius, maxHit, delay, source, warn, null, true);
    }

    /**
     * A strike that keeps its exact OSRS delay (at least a tick) even when it deals damage: for chained logic whose timing a
     * room depends on. Ordinary strikes are floored ({@link ArenaTiming#strikeDue}).
     */
    public static void strikeExact(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                                   ParticleOptions warn, AttackType style, boolean piercing) {
        add(world, pos, radius, maxHit, delay, source, warn, style, piercing, piercing ? 1.0 : 0.0, true);
    }

    /**
     * Every strike: a damaging one lands no sooner than {@link ArenaTiming#STRIKE_FLOOR_TICKS} game ticks (1.8 s) after it
     * is made, and its marker is drawn at once at its true radius, then redrawn every few Minecraft ticks ({@link #redraw}).
     * // playability: OSRS lands raid strikes from a tick after marking them, and the marker here was first drawn a game
     * tick late
     */
    private static void add(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                            ParticleOptions warn, AttackType style, boolean piercing, double protectedShare, boolean exact) {
        long now = now();
        int into = mcIntoTick();
        int d = Math.max(1, delay);
        long due = exact ? now + d : ArenaTiming.strikeDue(now, d, maxHit > 0, into);
        Strike s = new Strike(world, pos, radius, maxHit, style, piercing, due, source, warn == null ? ParticleTypes.FLAME : warn,
                protectedShare, exact, now * ArenaTiming.MC_PER_GAME_TICK + into);
        STRIKES.add(s);
        draw(s, false);
    }

    /** Draws a pending strike's marker: the outline at its true radius, its warn particle as a sparse fill. */
    private static void draw(Strike s, boolean landing) {
        if (s.firstDrawnMc < 0) {
            s.firstDrawnMc = clockMc();
        }
        s.draws++;
        // the same outline as every other arena's strikes: typeless/AoE orange, red-orange in its last moments
        com.runecraft.boss.StrikeMarkers.ring(s.world, s.pos, s.radius, s.warn, landing, ArenaTiming.ringPoints(s.radius, STRIKES.size()));
    }

    /**
     * Redraws every pending strike marker and cue. RaidManager calls it every {@link ArenaTiming#REDRAW_MC} Minecraft ticks,
     * between the game ticks that land them. In a strike's last {@link ArenaTiming#FINAL_CUE_MC} Minecraft ticks its outline
     * brightens and a bell ticks, rising as it nears. // playability: OSRS draws the marker once
     */
    public static void redraw() {
        long mc = clockMc();
        long now = now();
        CUES.removeIf(c -> now >= c.until());
        for (Cue c : new ArrayList<>(CUES)) {
            boolean keep;
            try {
                keep = c.draw().getAsBoolean();
            } catch (RuntimeException e) {
                keep = false;
                com.runecraft.RuneCraft.LOGGER.error("Raid cue failed", e);
            }
            if (!keep) {
                CUES.remove(c);
            }
        }
        Strike bell = null;
        for (Strike s : STRIKES) {
            boolean landing = ArenaTiming.finalCue(s.due * ArenaTiming.MC_PER_GAME_TICK, mc);
            draw(s, landing);
            if (landing && s.maxHit > 0 && (bell == null || s.due < bell.due)) {
                bell = s;
            }
        }
        if (bell != null) {
            com.runecraft.boss.StrikeMarkers.bell(bell.world, bell.pos, bell.due * ArenaTiming.MC_PER_GAME_TICK - mc);
        }
    }

    /**
     * A marker (gap columns, safe edges, a projectile's trail) drawn now, then every {@link ArenaTiming#REDRAW_MC} Minecraft
     * ticks for {@code gameTicks} game ticks, or until {@code draw} returns false.
     */
    public static void cue(int gameTicks, BooleanSupplier draw) {
        if (draw.getAsBoolean()) {
            CUES.add(new Cue(now() + Math.max(1, gameTicks), draw));
        }
    }

    public static void clear(ServerLevel world, net.minecraft.world.phys.AABB box) {
        STRIKES.removeIf(s -> s.world == world && box.contains(s.pos));
    }

    public static void gameTick() {
        long now = now();
        boundaryMc = ActionManager.mcTick();
        CUES.removeIf(c -> now >= c.until());
        List<Task> dueTasks = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (now >= t.due()) {
                dueTasks.add(t);
                it.remove();
            }
        }
        for (Task t : dueTasks) {
            try {
                t.run().run();
            } catch (RuntimeException e) {
                failures++;
                lastFailure = e.toString();
                com.runecraft.RuneCraft.LOGGER.error("Raid task failed", e);
            }
        }
        List<Strike> due = new ArrayList<>();
        for (Iterator<Strike> it = STRIKES.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if (now >= s.due) {
                due.add(s);
                it.remove();
            }
        }
        for (Strike s : due) {
            s.world.sendParticles(ParticleTypes.EXPLOSION, s.pos.x, s.pos.y + 0.5, s.pos.z, 1, 0, 0, 0, 0);
            s.world.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    0.4f, 1.5f);
            // What is drawn is what hits: horizontally inside the outline, within its height tolerance. // playability
            for (ServerPlayer p : s.world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative()
                    && ArenaTiming.inStrike(pl.getX() - s.pos.x, pl.getY() - s.pos.y, pl.getZ() - s.pos.z, s.radius))) {
                hit(s.source, p, p.getRandom().nextInt(s.maxHit + 1), s.style, s.piercing ? 1.0 : s.protectedShare);
            }
            strikeListener.accept(new ArenaTiming.StrikeTrace(s.createdMc, s.firstDrawnMc, now * ArenaTiming.MC_PER_GAME_TICK,
                    s.draws, s.radius, s.maxHit, s.maxHit > 0, s.exact));
        }
    }

    public static double horizontalDistSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Deals damage from a raid mechanic. A matching protection prayer blocks it unless piercing.
     * Returns the damage dealt.
     */
    public static int hit(LivingEntity source, ServerPlayer player, int damage, AttackType style, boolean piercing) {
        return hit(source, player, damage, style, piercing ? 1.0 : 0.0);
    }

    /**
     * Deals damage from a raid mechanic; a matching protection prayer lets {@code protectedShare} of it through (0: blocks
     * it, 1: ignores the prayer). Returns the damage dealt.
     */
    public static int hit(LivingEntity source, ServerPlayer player, int damage, AttackType style, double protectedShare) {
        RaidInstance raid = RaidManager.of(player);
        if (raid != null) {
            if (raid.isDowned(player)) {
                return 0;
            }
            raid.noteAttack();
        }
        if (protectedShare < 1.0 && style != null && protectedFrom(player, style)) {
            // ToA Quiet Prayers (OSRS): protection prayers are 10% less effective.
            double leak = raid != null && raid.type == com.runecraft.osrs.content.raids.RaidType.TOA
                    ? com.runecraft.osrs.content.raids.Invocation.protectionLeak(raid.invocations()) : 0;
            damage = (int) Math.round(damage * Math.max(protectedShare, leak));
        }
        if (raid != null) {
            damage = (int) Math.round(damage * raid.damageMultiplier(source));
        }
        LivingEntity attacker = source != null && source.isAlive() ? source : null;
        int dealt = applyScaled(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
    }

    /** True while a raid mechanic applies damage it has already scaled (see {@link RaidManager}'s minion scaling). */
    private static boolean applying;

    static boolean applyingScaled() {
        return applying;
    }

    private static int applyScaled(LivingEntity attacker, ServerPlayer player, int damage) {
        boolean was = applying;
        applying = true;
        try {
            return CombatHandler.applyDamage(attacker, player, damage);
        } finally {
            applying = was;
        }
    }

    /**
     * Like {@link #hit} but without the raid's damage multiplier (for damage that is already scaled or capped, such as
     * Akkha's unstable orbs).
     */
    public static int hitRaw(LivingEntity source, ServerPlayer player, int damage) {
        RaidInstance raid = RaidManager.of(player);
        if (raid != null) {
            if (raid.isDowned(player)) {
                return 0;
            }
            raid.noteAttack();
        }
        LivingEntity attacker = source != null && source.isAlive() ? source : null;
        int dealt = applyScaled(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
    }

    /**
     * Whether the player prays against {@code style}, read through the one protection chokepoint
     * ({@link CombatHandler#isProtected}, which the prayer scope gives its switch grace), so raid checks agree with every
     * other hit.
     */
    public static boolean protectedFrom(ServerPlayer player, AttackType style) {
        return style != null && CombatHandler.isProtected(Players.data(player), style);
    }
}
