package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * blood splats, death balls...). Everything runs on the server thread, ticked once per game tick.
 */
public final class RaidEffects {
    /** A marked area that explodes after a delay. */
    /**
     * A marked area that explodes after a delay. {@code protectedShare}: the share of the roll a matching protection
     * prayer lets through (0 blocks it, the default unless piercing; Kephri's fireballs let a third through).
     */
    private record Strike(ServerLevel world, Vec3 pos, double radius, int maxHit, AttackType style, boolean piercing,
                          long due, LivingEntity source, ParticleOptions warn, double protectedShare) {
    }

    private record Task(long due, Runnable run) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();
    /** Game ticks the raid clock runs ahead of the server's (the self-test fast-forwards raids, see {@link #advance}). */
    private static long skew;
    /** Delayed tasks that threw (logged and skipped); the self-test fails on any. */
    private static int failures;
    private static String lastFailure;

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
     * Marks {@code pos}; after {@code delay} game ticks every player within {@code radius} takes up to
     * {@code maxHit} (protection prayers block it unless piercing).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, boolean piercing) {
        STRIKES.add(new Strike(world, pos, radius, maxHit, style, piercing, now() + Math.max(1, delay), source,
                warn == null ? ParticleTypes.FLAME : warn, piercing ? 1.0 : 0.0));
    }

    /**
     * {@link #strike} whose damage a matching protection prayer only reduces: {@code protectedShare} of the roll still
     * lands (at least Quiet Prayers' share in ToA).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, double protectedShare) {
        STRIKES.add(new Strike(world, pos, radius, maxHit, style, false, now() + Math.max(1, delay), source,
                warn == null ? ParticleTypes.FLAME : warn, Math.max(0, Math.min(1, protectedShare))));
    }

    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn) {
        strike(world, pos, radius, maxHit, delay, source, warn, null, true);
    }

    public static void clear(ServerLevel world, net.minecraft.world.phys.AABB box) {
        STRIKES.removeIf(s -> s.world() == world && box.contains(s.pos()));
    }

    public static void gameTick() {
        long now = now();
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
            if (now >= s.due()) {
                due.add(s);
                it.remove();
            } else {
                int n = (int) Math.max(4, s.radius() * 8);
                s.world().sendParticles(s.warn(), s.pos().x, s.pos().y + 0.15, s.pos().z, n, s.radius() * 0.5, 0.02,
                        s.radius() * 0.5, 0.0);
            }
        }
        for (Strike s : due) {
            s.world().sendParticles(ParticleTypes.EXPLOSION, s.pos().x, s.pos().y + 0.5, s.pos().z, 1, 0, 0, 0, 0);
            s.world().playSound(null, s.pos().x, s.pos().y, s.pos().z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    0.4f, 1.5f);
            double r2 = s.radius() * s.radius();
            for (ServerPlayer p : s.world().getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative()
                    && horizontalDistSq(pl.position(), s.pos()) <= r2 && Math.abs(pl.getY() - s.pos().y) < 3)) {
                hit(s.source(), p, p.getRandom().nextInt(s.maxHit() + 1), s.style(), s.piercing() ? 1.0 : s.protectedShare());
            }
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
        int dealt = CombatHandler.applyDamage(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
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
        int dealt = CombatHandler.applyDamage(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
    }

    public static boolean protectedFrom(ServerPlayer player, AttackType style) {
        var data = Players.data(player);
        return switch (style) {
            case MAGIC -> data.isActive(Prayer.PROTECT_FROM_MAGIC);
            case RANGED -> data.isActive(Prayer.PROTECT_FROM_MISSILES);
            default -> data.isActive(Prayer.PROTECT_FROM_MELEE);
        };
    }
}
