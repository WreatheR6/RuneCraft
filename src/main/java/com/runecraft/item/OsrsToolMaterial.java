package com.runecraft.item;

import com.runecraft.osrs.data.MetalTier;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

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
    public int getLevel() {
        return switch (tier) {
            case BRONZE -> 1;
            case IRON, STEEL, BLACK -> 2;
            case MITHRIL, ADAMANT -> 3;
            case RUNE, DRAGON -> 4;
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
