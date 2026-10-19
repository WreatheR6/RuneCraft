package com.runecraft.platform;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric's living entity events. ALLOW_DAMAGE is fired from {@code LivingIncomingDamageEvent} (false cancels the
 * damage). ALLOW_DEATH is fired once per death: from {@code LivingDamageEvent.Post} when the damage leaves the entity
 * dead, before the totem check and {@code die()}, or from {@code LivingDeathEvent} for a {@code die()} that damage did
 * not lead to. False cancels the death and no totem is used; the listener should give the entity its health back, or it
 * stays dying at 0 health. AFTER_DEATH is fired from {@code LivingDeathEvent} at the lowest priority when no one
 * cancelled it.
 */
public final class ServerLivingEntityEvents {
    private ServerLivingEntityEvents() {
    }

    @FunctionalInterface
    public interface AllowDamage {
        boolean allowDamage(LivingEntity entity, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface AllowDeath {
        boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount);
    }

    @FunctionalInterface
    public interface AfterDeath {
        void afterDeath(LivingEntity entity, DamageSource damageSource);
    }

    public static final Event<AllowDamage> ALLOW_DAMAGE = new Event<>(listeners -> (entity, source, amount) -> {
        for (AllowDamage l : listeners) {
            if (!l.allowDamage(entity, source, amount)) {
                return false;
            }
        }
        return true;
    });

    public static final Event<AllowDeath> ALLOW_DEATH = new Event<>(listeners -> (entity, source, amount) -> {
        for (AllowDeath l : listeners) {
            if (!l.allowDeath(entity, source, amount)) {
                return false;
            }
        }
        return true;
    });

    public static final Event<AfterDeath> AFTER_DEATH = new Event<>(listeners -> (entity, source) -> {
        for (AfterDeath l : listeners) {
            l.afterDeath(entity, source);
        }
    });
}
