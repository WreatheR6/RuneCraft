package com.runecraft.item;

import com.runecraft.osrs.data.MetalTier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Vanilla tool stats for RuneCraft metal tiers, so tools also work outside Gielinor. */
public final class OsrsToolMaterial implements Tier {
    private final MetalTier tier;

    public OsrsToolMaterial(MetalTier tier) {
        this.tier = tier;
    }

    @Override
    public int getUses() {
        return switch (tier) {
            case BRONZE -> 150;
            case IRON -> 250;
            case STEEL -> 400;
            case BLACK -> 500;
            case MITHRIL -> 700;
            case ADAMANT -> 1000;
            case RUNE -> 1600;
            case DRAGON -> 2400;
        };
    }

    @Override
    public float getSpeed() {
        return 3.0f + tier.ordinal() * 1.1f;
    }

    @Override
    public float getAttackDamageBonus() {
        return tier.ordinal() * 0.75f;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return switch (tier) {
            case BRONZE -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case IRON, STEEL, BLACK -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case MITHRIL, ADAMANT -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            case RUNE, DRAGON -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + tier.ordinal();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
