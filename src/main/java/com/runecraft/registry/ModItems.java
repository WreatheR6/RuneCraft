package com.runecraft.registry;

import com.runecraft.RuneCraft;
import com.runecraft.item.OsrsArmorMaterial;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.item.OsrsItem;
import com.runecraft.item.OsrsItemLike;
import com.runecraft.item.OsrsToolMaterial;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.data.MetalTier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registers one Minecraft item per {@link ItemCatalog} entry. */
public final class ModItems {
    private static final Map<String, Item> BY_ID = new LinkedHashMap<>();
    private static final Map<String, OsrsArmorMaterial> ARMOR_MATERIALS = new HashMap<>();
    private static final Map<MetalTier, OsrsToolMaterial> TOOL_MATERIALS = new EnumMap<>(MetalTier.class);

    private ModItems() {
    }

    public static void register() {
        for (MetalTier t : MetalTier.values()) {
            TOOL_MATERIALS.put(t, new OsrsToolMaterial(t));
        }
        for (ItemDef def : ItemCatalog.all()) {
            Item item = Registry.register(Registries.ITEM, RuneCraft.id(def.id()), create(def));
            BY_ID.put(def.id(), item);
        }
    }

    private static Item create(ItemDef def) {
        Item.Settings settings = new Item.Settings().maxCount(def.maxStack());
        if (def.tier() != null && (def.tier().equals("dragon") || def.tier().equals("rune") || def.tier().equals("magic"))) {
            settings = settings.rarity(def.tier().equals("dragon") ? Rarity.RARE : Rarity.UNCOMMON);
        }
        MetalTier metal = metal(def.tier());
        return switch (def.kind()) {
            case AXE -> new OsrsEquipmentItems.Axe(def, TOOL_MATERIALS.get(metal), new Item.Settings());
            case PICKAXE -> new OsrsEquipmentItems.Pickaxe(def, TOOL_MATERIALS.get(metal), new Item.Settings());
            case MELEE_WEAPON -> {
                EquipmentDef eq = def.equipment();
                int damage = Math.max(1, eq.stats().meleeStrength() / 12);
                // OSRS attack speed in game ticks (0.6s) -> vanilla attacks per second.
                float speed = -4.0f + 1.0f / (eq.attackSpeed() * 0.6f);
                yield new OsrsEquipmentItems.Weapon(def, TOOL_MATERIALS.get(metal), damage, speed, new Item.Settings());
            }
            case ARMOUR -> new OsrsEquipmentItems.Armour(def, armorMaterial(def.tier()), armorType(def.equipment().slot()),
                    new Item.Settings());
            case BOW -> new OsrsEquipmentItems.Bow(def, new Item.Settings().maxDamage(384));
            case ARROW -> new OsrsEquipmentItems.Arrow(def, new Item.Settings());
            case STAFF -> new OsrsEquipmentItems.Staff(def, new Item.Settings().maxCount(1));
            case POTION -> new OsrsItem(def, new Item.Settings().maxDamage(4));
            default -> new OsrsItem(def, settings);
        };
    }

    private static MetalTier metal(String tier) {
        if (tier == null) {
            return MetalTier.BRONZE;
        }
        for (MetalTier t : MetalTier.values()) {
            if (t.id().equals(tier)) {
                return t;
            }
        }
        return MetalTier.BRONZE;
    }

    private static OsrsArmorMaterial armorMaterial(String tier) {
        return ARMOR_MATERIALS.computeIfAbsent(tier, t -> new OsrsArmorMaterial(t,
                switch (t) {
                    case "leather" -> 1;
                    case "barrows", "bandos" -> 8;
                    default -> metal(t).ordinal() + 1;
                }));
    }

    private static ArmorItem.Type armorType(EquipmentDef.Slot slot) {
        return switch (slot) {
            case HEAD -> ArmorItem.Type.HELMET;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
            default -> ArmorItem.Type.CHESTPLATE;
        };
    }

    public static Item get(String id) {
        return BY_ID.get(id);
    }

    /** The catalog entry for an item stack, or null for non-RuneCraft items. */
    public static ItemDef def(ItemStack stack) {
        if (stack.getItem() instanceof OsrsItemLike like) {
            return like.def();
        }
        return null;
    }

    public static ItemDef def(Item item) {
        if (item instanceof OsrsItemLike like) {
            return like.def();
        }
        Identifier id = Registries.ITEM.getId(item);
        return id.getNamespace().equals(RuneCraft.MOD_ID) ? ItemCatalog.get(id.getPath()) : null;
    }

    public static List<Item> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    public static int count() {
        return BY_ID.size();
    }
}
