package com.runecraft.entity;

import com.runecraft.combat.CombatHandler;
import net.minecraft.entity.LivingEntity;

/** Small bridge so bosses can perform multi-target attacks. */
final class CombatHandlerAccess {
    private CombatHandlerAccess() {
    }

    static void clearCooldown(LivingEntity mob) {
        CombatHandler.clearCooldown(mob);
    }
}
