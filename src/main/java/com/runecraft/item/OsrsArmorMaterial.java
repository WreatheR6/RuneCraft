package com.runecraft.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Vanilla armour values for RuneCraft armour. Inside Gielinor OSRS defence bonuses are used
 * instead (OSRS hits bypass vanilla armour). Texture: assets/minecraft/textures/models/armor/runecraft_&lt;tier&gt;_layer_N.png
 */
public final class OsrsArmorMaterial {
    private final String tier;
    private final int strength;

    public OsrsArmorMaterial(String tier, int strength) {
        this.tier = tier;
        this.strength = strength;
    }

    public String tier() {
        return tier;
    }

    public int getDurabilityForType(ArmorItem.Type type) {
        return type.getDurability(10 + strength * 6);
    }

    public int getDefenseForType(ArmorItem.Type type) {
        int base = Math.max(1, strength);
        return switch (type) {
            case HELMET -> Math.max(1, base / 2);
            case CHESTPLATE, BODY -> Math.min(9, base + 2);
            case LEGGINGS -> Math.min(7, base + 1);
            case BOOTS -> Math.max(1, base / 2);
        };
    }

    public String getName() {
        return "runecraft_" + tier;
    }

    public float getToughness() {
        return strength >= 6 ? 2.0f : 0.0f;
    }

    /** The vanilla armour material record for this tier, to register in the armour material registry. */
    public ArmorMaterial toMaterial() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, getDefenseForType(type));
        }
        return new ArmorMaterial(defense, 10,
                "leather".equals(tier) ? SoundEvents.ARMOR_EQUIP_LEATHER : SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace(getName()))),
                getToughness(), 0.0f);
    }
}
