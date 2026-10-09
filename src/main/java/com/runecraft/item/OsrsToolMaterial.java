package com.runecraft.item;

import com.runecraft.osrs.data.MetalTier;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

/** Vanilla tool stats for RuneCraft metal tiers, so tools also work outside Gielinor. */
public final class OsrsToolMaterial implements ToolMaterial {
    private final MetalTier tier;

    public OsrsToolMaterial(MetalTier tier) {
        this.tier = tier;
    }

    @Override
    public int getDurability() {
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
    public float getMiningSpeedMultiplier() {
        return 3.0f + tier.ordinal() * 1.1f;
    }

    @Override
    public float getAttackDamage() {
        return tier.ordinal() * 0.75f;
    }

    @Override
    public int getMiningLevel() {
        return switch (tier) {
            case BRONZE -> 1;
            case IRON, STEEL, BLACK -> 2;
            case MITHRIL, ADAMANT -> 3;
            case RUNE, DRAGON -> 4;
        };
    }

    @Override
    public int getEnchantability() {
        return 10 + tier.ordinal();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
