package com.runecraft.platform;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric's living entity events. ALLOW_DAMAGE is fired from {@code LivingIncomingDamageEvent} (false cancels the
 * damage), ALLOW_DEATH from {@code LivingDeathEvent} at the highest priority (false cancels the death) and AFTER_DEATH
 * from {@code LivingDeathEvent} at the lowest priority when no one cancelled it.
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
