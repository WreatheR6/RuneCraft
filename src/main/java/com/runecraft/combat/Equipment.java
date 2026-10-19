package com.runecraft.combat;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.data.EquipmentBonuses;
import com.runecraft.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Totals a player's OSRS equipment bonuses (armour slots, main hand weapon, off-hand shield unless two-handed). */
public final class Equipment {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Equipment() {
    }

    public static EquipmentStats total(Player player, SkillSet skills) {
        EquipmentStats total = EquipmentStats.NONE;
        for (EquipmentSlot slot : ARMOUR) {
            total = total.plus(stats(player.getItemBySlot(slot), skills, null));
        }
        total = total.plus(stats(player.getMainHandItem(), skills, EquipmentDef.Slot.WEAPON));
        if (!twoHanded(player)) {
            total = total.plus(stats(player.getOffhandItem(), skills, EquipmentDef.Slot.SHIELD));
        }
        return total;
    }

    /** True when the main-hand weapon is two-handed: like OSRS, an off-hand shield then gives no bonuses. */
    public static boolean twoHanded(Player player) {
        ItemDef def = ModItems.def(player.getMainHandItem());
        return def != null && EquipmentBonuses.twoHanded(def.id());
    }

    private static EquipmentStats stats(ItemStack stack, SkillSet skills, EquipmentDef.Slot onlySlot) {
        ItemDef def = ModItems.def(stack);
        if (def == null || def.equipment() == null) {
            return EquipmentStats.NONE;
        }
        EquipmentDef eq = def.equipment();
        if (com.runecraft.item.ItemCharges.depleted(stack)) {
            return EquipmentStats.NONE; // broken Barrows, inactive crystal, uncharged trident/helm
        }
        if (onlySlot != null && eq.slot() != onlySlot) {
            return EquipmentStats.NONE;
        }
        if (skills != null && eq.unmetRequirement(skills) != null) {
            return EquipmentStats.NONE;
        }
        EquipmentStats s = eq.stats();
        if (def.kind() == ItemDef.Kind.ARROW) {
            return EquipmentStats.NONE;
        }
        return s;
    }

    /** The wielded weapon's definition (or null when unarmed / wielding something else). */
    public static EquipmentDef weapon(Player player, SkillSet skills) {
        ItemDef def = ModItems.def(player.getMainHandItem());
        if (def == null || def.equipment() == null || def.equipment().slot() != EquipmentDef.Slot.WEAPON) {
            return null;
        }
        return skills == null || def.equipment().unmetRequirement(skills) == null ? def.equipment() : null;
    }

    public static int weaponSpeed(Player player, SkillSet skills) {
        EquipmentDef w = weapon(player, skills);
        return w == null ? 4 : w.attackSpeed();
    }

    public static AttackType meleeType(Player player, SkillSet skills) {
        EquipmentDef w = weapon(player, skills);
        return w == null || !w.attackType().isMelee() ? AttackType.CRUSH : w.attackType();
    }
}
