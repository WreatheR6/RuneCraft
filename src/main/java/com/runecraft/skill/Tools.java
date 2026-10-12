package com.runecraft.skill;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * Finds the best usable axe/pickaxe/harpoon anywhere in the inventory, like OSRS. Crystal and
 * infernal tools count as dragon tier with their own level requirement and bonuses
 * ({@link SpecialTool}).
 */
public final class Tools {
    private Tools() {
    }

    /** A usable tool: its metal tier and the item it is. */
    public record Tool(ToolTier tier, String itemId) {
        public double woodcuttingMultiplier() {
            return SpecialTool.woodcuttingMultiplier(tier, itemId);
        }

        /** Game ticks until the next mining roll ({@code roll} uniform in [0, 1)). */
        public int miningTicks(double roll) {
            return SpecialTool.miningTicks(tier, itemId, roll);
        }
    }

    public static ToolTier bestAxe(PlayerEntity player, SkillSet skills) {
        Tool t = bestAxeTool(player, skills);
        return t == null ? null : t.tier();
    }

    public static ToolTier bestPickaxe(PlayerEntity player, SkillSet skills) {
        Tool t = bestPickaxeTool(player, skills);
        return t == null ? null : t.tier();
    }

    public static Tool bestAxeTool(PlayerEntity player, SkillSet skills) {
        return best(player, skills, ItemDef.Kind.AXE, Skill.WOODCUTTING);
    }

    public static Tool bestPickaxeTool(PlayerEntity player, SkillSet skills) {
        return best(player, skills, ItemDef.Kind.PICKAXE, Skill.MINING);
    }

    /** The best harpoon the player can use (crystal > infernal/dragon > harpoon), or null. */
    public static String bestHarpoon(PlayerEntity player, SkillSet skills) {
        String best = null;
        double bestMult = 0;
        PlayerInventory inv = player.getInventory();
        int level = skills.currentLevel(Skill.FISHING);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            ItemDef def = ModItems.def(stack);
            if (def == null || !SpecialTool.isHarpoon(def.id())) {
                continue;
            }
            SpecialTool special = SpecialTool.of(def.id());
            if (special != null && level < special.level()) {
                continue;
            }
            double mult = SpecialTool.harpoonMultiplier(def.id()) + (special == null ? 0 : special.rank() * 0.001);
            if (mult > bestMult) {
                bestMult = mult;
                best = def.id();
            }
        }
        return best;
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

    private static Tool best(PlayerEntity player, SkillSet skills, ItemDef.Kind kind, Skill skill) {
        Tool best = null;
        int bestScore = -1;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            ToolTier t = tierOf(stack, kind);
            if (t == null) {
                continue;
            }
            String id = ModItems.def(stack).id();
            if (skills.currentLevel(skill) < SpecialTool.levelFor(t, id)) {
                continue;
            }
            SpecialTool special = SpecialTool.of(id);
            int score = t.ordinal() * 10 + (special == null ? 0 : special.rank());
            if (score > bestScore) {
                bestScore = score;
                best = new Tool(t, id);
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
