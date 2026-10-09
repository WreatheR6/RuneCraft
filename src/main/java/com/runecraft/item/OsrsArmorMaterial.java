package com.runecraft.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Vanilla armour values for RuneCraft armour. Inside Gielinor OSRS defence bonuses are used
 * instead (OSRS hits bypass vanilla armour). Texture: assets/minecraft/textures/models/armor/runecraft_&lt;tier&gt;_layer_N.png
 */
public final class OsrsArmorMaterial implements ArmorMaterial {
    private final String tier;
    private final int strength;

    public OsrsArmorMaterial(String tier, int strength) {
        this.tier = tier;
        this.strength = strength;
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return (100 + strength * 60) * switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        } / 10;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        int base = Math.max(1, strength);
        return switch (type) {
            case HELMET -> Math.max(1, base / 2);
            case CHESTPLATE -> Math.min(9, base + 2);
            case LEGGINGS -> Math.min(7, base + 1);
            case BOOTS -> Math.max(1, base / 2);
        };
    }

    @Override
    public int getEnchantability() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return "leather".equals(tier) ? SoundEvents.ITEM_ARMOR_EQUIP_LEATHER : SoundEvents.ITEM_ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return "runecraft_" + tier;
    }

    @Override
    public float getToughness() {
        return strength >= 6 ? 2.0f : 0.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}
