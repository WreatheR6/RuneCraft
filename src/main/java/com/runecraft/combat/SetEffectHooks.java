package com.runecraft.combat;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.data.SetEffects;
import com.runecraft.util.Inv;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armour set effects from {@link SetEffects}: Dharok's, Guthan's and Verac's Barrows sets, Inquisitor's armour
 * (crush, via PlayerHitCalc). Dharok's and crystal armour are applied by {@link com.runecraft.osrs.PlayerHitCalc}.
 */
public final class SetEffectHooks {
    /** Players whose next roll is a Verac's "Defiler" hit (ignores defence, +1 damage). */
    private static final Map<UUID, Boolean> VERAC_PROC = new ConcurrentHashMap<>();
    /** The Minecraft tick of each player's latest Verac's hit, for targets whose prayers it pierces (demonic gorillas). */
    private static final Map<UUID, Long> VERAC_HIT = new ConcurrentHashMap<>();

    private SetEffectHooks() {
    }

    /**
     * True (once) when the hit being applied now is a Verac's "Defiler" hit, which also passes through protection prayers:
     * demonic gorillas can't block it (OSRS Wiki), although they block special attacks.
     */
    public static boolean takeVeracHit(ServerPlayer p) {
        Long tick = p == null ? null : VERAC_HIT.remove(p.getUUID());
        return tick != null && tick == com.runecraft.action.ActionManager.mcTick();
    }

    private static String worn(ServerPlayer p, EquipmentSlot slot) {
        return Inv.idOf(p.getItemBySlot(slot));
    }

    private static SetEffects.BarrowsSet barrows(ServerPlayer p) {
        return SetEffects.barrows(worn(p, EquipmentSlot.HEAD), worn(p, EquipmentSlot.CHEST), worn(p, EquipmentSlot.LEGS),
                Inv.idOf(p.getMainHandItem()));
    }

    private static boolean damned(ServerPlayer p) {
        return "amulet_of_the_damned".equals(com.runecraft.content.systems.SysState.of(p).wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.NECK));
    }

    public static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                // Inquisitor's armour's crush bonus is part of com.runecraft.osrs.PlayerHitCalc (HitCalcBridge).
                double m = 1.0;
                if (type.isMelee() && barrows(player) == SetEffects.VERAC
                        && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    VERAC_PROC.put(player.getUUID(), Boolean.TRUE);
                    m *= 1000.0; // the hit ignores the target's defence
                }
                return m;
            }

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                if (VERAC_PROC.remove(player.getUUID()) != null) {
                    VERAC_HIT.put(player.getUUID(), com.runecraft.action.ActionManager.mcTick());
                    return damage + 1;
                }
                // Ahrim's set with the amulet of the damned: 25% of spells deal 30% more damage (osrs-dps-calc).
                if (type == AttackType.MAGIC && damage > 0 && barrows(player) == SetEffects.AHRIM && damned(player)
                        && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    return SetEffects.ahrimDamned(damage);
                }
                return damage;
            }

            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                // Karil's set with the amulet of the damned: 25% of shots hit a second time for half the damage.
                if (dealt > 1 && type == AttackType.RANGED && barrows(player) == SetEffects.KARIL && damned(player)
                        && target.isAlive() && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    // The second hitsplat goes through the after-roll hooks and the target's own rules, and gives XP.
                    int second = CombatHooks.afterRoll(player, target, AttackType.RANGED, SetEffects.karilDamnedSecondHit(dealt));
                    int done = com.runecraft.content.systems.SysCombat.hit(player, target, AttackType.RANGED, second);
                    CombatHandler.awardXp(player, com.runecraft.player.Players.data(player).style().forRanged(), done, false);
                }
                if (dealt > 0 && type.isMelee() && barrows(player) == SetEffects.GUTHAN
                        && player.getRandom().nextDouble() < SetEffects.BARROWS_PROC_CHANCE) {
                    player.heal(dealt); // "Infestation": heals the damage dealt, up to full health
                }
            }
        });
    }
}
