package com.runecraft.combat;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.data.SetEffects;
import com.runecraft.util.Inv;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armour set effects from {@link SetEffects}: Dharok's, Guthan's and Verac's Barrows sets, Inquisitor's armour
 * (crush) and crystal armour with crystal bows.
 */
public final class SetEffectHooks {
    /** Players whose next roll is a Verac's "Defiler" hit (ignores defence, +1 damage). */
    private static final Map<UUID, Boolean> VERAC_PROC = new ConcurrentHashMap<>();

    private SetEffectHooks() {
    }

    private static String worn(ServerPlayerEntity p, EquipmentSlot slot) {
        return Inv.idOf(p.getEquippedStack(slot));
    }

    private static SetEffects.BarrowsSet barrows(ServerPlayerEntity p) {
        return SetEffects.barrows(worn(p, EquipmentSlot.HEAD), worn(p, EquipmentSlot.CHEST), worn(p, EquipmentSlot.LEGS),
                Inv.idOf(p.getMainHandStack()));
    }

    public static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                double m = 1.0;
                if (type == AttackType.CRUSH) {
                    m *= SetEffects.inquisitorCrushBonus(worn(player, EquipmentSlot.HEAD), worn(player, EquipmentSlot.CHEST),
                            worn(player, EquipmentSlot.LEGS));
                }
                if (type == AttackType.RANGED && SetEffects.crystalBow(Inv.idOf(player.getMainHandStack()))) {
                    m *= SetEffects.crystalAccuracy(worn(player, EquipmentSlot.HEAD), worn(player, EquipmentSlot.CHEST),
                            worn(player, EquipmentSlot.LEGS));
                }
                if (type.isMelee() && barrows(player) == SetEffects.VERAC
                        && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    VERAC_PROC.put(player.getUuid(), Boolean.TRUE);
                    m *= 1000.0; // the hit ignores the target's defence
                }
                return m;
            }

            @Override
            public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                double m = 1.0;
                if (type == AttackType.CRUSH) {
                    m *= SetEffects.inquisitorCrushBonus(worn(player, EquipmentSlot.HEAD), worn(player, EquipmentSlot.CHEST),
                            worn(player, EquipmentSlot.LEGS));
                }
                if (type == AttackType.RANGED && SetEffects.crystalBow(Inv.idOf(player.getMainHandStack()))) {
                    m *= SetEffects.crystalDamage(worn(player, EquipmentSlot.HEAD), worn(player, EquipmentSlot.CHEST),
                            worn(player, EquipmentSlot.LEGS));
                }
                if (type.isMelee() && barrows(player) == SetEffects.DHAROK) {
                    m *= SetEffects.dharokMultiplier(Math.round(player.getHealth()), Math.round(player.getMaxHealth()));
                }
                return m;
            }

            @Override
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                if (VERAC_PROC.remove(player.getUuid()) != null) {
                    return damage + 1;
                }
                return damage;
            }

            @Override
            public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
                if (dealt > 0 && type.isMelee() && barrows(player) == SetEffects.GUTHAN
                        && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    player.heal(dealt); // "Infestation": heals the damage dealt, up to full health
                }
            }
        });
    }
}
