package com.runecraft.combat;

import com.runecraft.content.systems.SysCombat;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatBonuses;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Helpers for equipment set effects (salve amulets vs undead, Dharok's set, obsidian armour). The accuracy and
 * max-hit maths of every gear effect lives in {@link com.runecraft.osrs.PlayerHitCalc} (snapshot-tested against the
 * OSRS Wiki DPS calculator) and is applied by {@link HitCalcBridge}; this modifier no longer multiplies anything.
 */
public final class GearEffects implements CombatHooks.PlayerAttackModifier {
    private static final double EPS = 1e-9;

    static String worn(ServerPlayerEntity p, EquipmentSlot slot) {
        return Inv.idOf(p.getEquippedStack(slot));
    }

    static String neck(ServerPlayerEntity p) {
        return SysState.of(p).wornId(Jewellery.Slot.NECK);
    }

    /** Salve multiplier for this attack (1.0 when it does not apply). */
    public static double salve(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (target == null || target instanceof ServerPlayerEntity) {
            return 1.0;
        }
        double m = CombatBonuses.salve(neck(p), type);
        return m > 1.0 && SysCombat.isUndead(target) ? m : 1.0;
    }

    /** True when a salve amulet boosts this attack (the black mask / slayer helmet bonus is then skipped). */
    public static boolean salveActive(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return salve(p, target, type) > 1.0;
    }

    static boolean dharoks(ServerPlayerEntity p) {
        return worn(p, EquipmentSlot.HEAD).equals("dharoks_helm") && worn(p, EquipmentSlot.CHEST).equals("dharoks_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("dharoks_platelegs") && Inv.idOf(p.getMainHandStack()).equals("dharoks_greataxe");
    }

    static boolean obsidianArmour(ServerPlayerEntity p) {
        return worn(p, EquipmentSlot.HEAD).equals("obsidian_helmet") && worn(p, EquipmentSlot.CHEST).equals("obsidian_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("obsidian_platelegs");
    }

    /** Magic level (or magic attack, whichever is higher) the twisted bow scales with. */
    static int targetMagic(LivingEntity target) {
        if (target instanceof ServerPlayerEntity other) {
            return Players.data(other).skills().currentLevel(Skill.MAGIC);
        }
        return MonsterStats.of(target).magic();
    }

    static boolean crystal(ServerPlayerEntity p) {
        return CombatBonuses.isCrystalBow(Inv.idOf(p.getMainHandStack()));
    }

    // Accuracy and max-hit effects (salve, Dharok's, berserker necklace, crystal armour, twisted bow, prayer magic
    // damage) are computed by com.runecraft.osrs.PlayerHitCalc through HitCalcBridge, in the DPS calculator's order.
}
