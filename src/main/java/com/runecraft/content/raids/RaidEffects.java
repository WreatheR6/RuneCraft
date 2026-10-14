package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.player.Players;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Telegraphed ground strikes and delayed tasks used by raid mechanics (falling crystals, lightning,
 * blood splats, death balls...). Everything runs on the server thread, ticked once per game tick.
 */
public final class RaidEffects {
    /** A marked area that explodes after a delay. */
    private record Strike(ServerWorld world, Vec3d pos, double radius, int maxHit, AttackType style, boolean piercing,
                          long due, LivingEntity source, ParticleEffect warn, Predicate<ServerPlayerEntity> filter) {
    }

    private record Task(long due, Runnable run) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();

    private RaidEffects() {
    }

    /** Runs {@code run} after {@code gameTicks} game ticks. */
    public static void later(int gameTicks, Runnable run) {
        TASKS.add(new Task(ActionManager.gameTick() + Math.max(1, gameTicks), run));
    }

    /**
     * Marks {@code pos}; after {@code delay} game ticks every player within {@code radius} takes up to
     * {@code maxHit} (protection prayers block it unless piercing).
     */
    public static void strike(ServerWorld world, Vec3d pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleEffect warn, AttackType style, boolean piercing) {
        STRIKES.add(new Strike(world, pos, radius, maxHit, style, piercing, ActionManager.gameTick() + Math.max(1, delay), source,
                warn == null ? ParticleTypes.FLAME : warn, p -> true));
    }

    public static void strike(ServerWorld world, Vec3d pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleEffect warn) {
        strike(world, pos, radius, maxHit, delay, source, warn, null, true);
    }

    public static void clear(ServerWorld world, net.minecraft.util.math.Box box) {
        STRIKES.removeIf(s -> s.world() == world && box.contains(s.pos()));
    }

    public static void gameTick() {
        long now = ActionManager.gameTick();
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
                s.world().spawnParticles(s.warn(), s.pos().x, s.pos().y + 0.15, s.pos().z, n, s.radius() * 0.5, 0.02,
                        s.radius() * 0.5, 0.0);
            }
        }
        for (Strike s : due) {
            s.world().spawnParticles(ParticleTypes.EXPLOSION, s.pos().x, s.pos().y + 0.5, s.pos().z, 1, 0, 0, 0, 0);
            s.world().playSound(null, s.pos().x, s.pos().y, s.pos().z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE,
                    0.4f, 1.5f);
            double r2 = s.radius() * s.radius();
            for (ServerPlayerEntity p : s.world().getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative()
                    && horizontalDistSq(pl.getPos(), s.pos()) <= r2 && Math.abs(pl.getY() - s.pos().y) < 3)) {
                hit(s.source(), p, p.getRandom().nextInt(s.maxHit() + 1), s.style(), s.piercing());
            }
        }
    }

    public static double horizontalDistSq(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Deals damage from a raid mechanic. A matching protection prayer blocks it unless piercing.
     * Returns the damage dealt.
     */
    public static int hit(LivingEntity source, ServerPlayerEntity player, int damage, AttackType style, boolean piercing) {
        RaidInstance raid = RaidManager.of(player);
        if (!piercing && style != null && protectedFrom(player, style)) {
            // ToA Quiet Prayers (OSRS): protection prayers are 10% less effective.
            double leak = raid != null && raid.type == com.runecraft.osrs.content.raids.RaidType.TOA
                    ? com.runecraft.osrs.content.raids.Invocation.protectionLeak(raid.invocations()) : 0;
            damage = (int) Math.round(damage * leak);
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

    public static boolean protectedFrom(ServerPlayerEntity player, AttackType style) {
        var data = Players.data(player);
        return switch (style) {
            case MAGIC -> data.isActive(Prayer.PROTECT_FROM_MAGIC);
            case RANGED -> data.isActive(Prayer.PROTECT_FROM_MISSILES);
            default -> data.isActive(Prayer.PROTECT_FROM_MELEE);
        };
    }
}
