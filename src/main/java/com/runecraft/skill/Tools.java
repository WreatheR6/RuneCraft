package com.runecraft.skill;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/** Finds the best usable axe/pickaxe anywhere in the inventory, like OSRS. */
public final class Tools {
    private Tools() {
    }

    public static ToolTier bestAxe(PlayerEntity player, SkillSet skills) {
        return best(player, skills, ItemDef.Kind.AXE, Skill.WOODCUTTING);
    }

    public static ToolTier bestPickaxe(PlayerEntity player, SkillSet skills) {
        return best(player, skills, ItemDef.Kind.PICKAXE, Skill.MINING);
    }

    /** Lowest-tier tool of the kind the player owns but can't use (for the error message). */
    public static ToolTier anyOwned(PlayerEntity player, ItemDef.Kind kind) {
        ToolTier found = null;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ToolTier t = tierOf(inv.getStack(i), kind);
            if (t != null && (found == null || t.ordinal() < found.ordinal())) {
                found = t;
            }
        }
        return found;
    }

    private static ToolTier best(PlayerEntity player, SkillSet skills, ItemDef.Kind kind, Skill skill) {
        ToolTier best = null;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ToolTier t = tierOf(inv.getStack(i), kind);
            if (t != null && skills.currentLevel(skill) >= t.level() && (best == null || t.ordinal() > best.ordinal())) {
                best = t;
            }
        }
        return best;
    }

    private static ToolTier tierOf(ItemStack stack, ItemDef.Kind kind) {
        ItemDef def = ModItems.def(stack);
        if (def == null || def.kind() != kind) {
            return null;
        }
        for (ToolTier t : ToolTier.values()) {
            if (t.metal().id().equals(def.tier())) {
                return t;
            }
        }
        return null;
    }
}
