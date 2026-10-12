package com.runecraft.osrs;

import com.runecraft.osrs.data.ArmourType;
import com.runecraft.osrs.data.ArrowTier;
import com.runecraft.osrs.data.Bar;
import com.runecraft.osrs.data.BowWood;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.MetalTier;
import com.runecraft.osrs.data.Potion;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.WeaponType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All Make-X recipes and shop stock. */
public final class Recipes {
    private static final Map<String, Recipe> BY_ID = new LinkedHashMap<>();
    private static final Map<MenuType, List<Recipe>> BY_MENU = new EnumMap<>(MenuType.class);

    static {
        build();
    }

    private Recipes() {
    }

    public static List<Recipe> menu(MenuType type) {
        return BY_MENU.getOrDefault(type, List.of());
    }

    public static Recipe get(String id) {
        return BY_ID.get(id);
    }

    public static List<Recipe> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private static void add(Recipe r) {
        if (BY_ID.put(r.id(), r) != null) {
            throw new IllegalStateException("Duplicate recipe " + r.id());
        }
        BY_MENU.computeIfAbsent(r.menu(), k -> new ArrayList<>()).add(r);
    }

    private static String name(String itemId) {
        ItemDef def = ItemCatalog.get(itemId);
        if (def != null) {
            return def.name();
        }
        String path = itemId.substring(itemId.indexOf(':') + 1).replace('_', ' ');
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static void build() {
        buildBuiltIn();
        for (com.runecraft.osrs.content.ContentModule module : com.runecraft.osrs.content.Modules.ALL) {
            module.recipes(Recipes::add);
        }
    }

    private static void buildBuiltIn() {
        // --- Smelting (furnace) ---
        for (Bar bar : Bar.values()) {
            Recipe.Builder b = Recipe.builder("smelt_" + bar.itemId(), MenuType.SMELTING, bar.displayName())
                    .skill(Skill.SMITHING, bar.level(), bar.xp()).out(bar.itemId(), 1).ticks(4).success(bar.successChance());
            bar.ores().forEach(b::in);
            add(b.build());
        }
        jewellery("gold_ring", 5, 15.0, "ring_mould", null);
        jewellery("gold_necklace", 6, 20.0, "necklace_mould", null);
        jewellery("gold_amulet", 8, 30.0, "amulet_mould", null);
        jewellery("sapphire_ring", 20, 40.0, "ring_mould", "sapphire");
        jewellery("emerald_ring", 27, 55.0, "ring_mould", "emerald");
        jewellery("ruby_ring", 34, 70.0, "ring_mould", "ruby");
        jewellery("diamond_ring", 43, 85.0, "ring_mould", "diamond");

        // --- Smithing (anvil, hammer required) ---
        for (MetalTier tier : MetalTier.values()) {
            if (!tier.smithable()) {
                continue;
            }
            for (WeaponType w : WeaponType.values()) {
                smith(w.itemId(tier), 1, tier, w.bars(), w.smithingLevel(tier));
            }
            for (ArmourType a : ArmourType.values()) {
                smith(a.itemId(tier), 1, tier, a.bars(), a.smithingLevel(tier));
            }
            for (ToolTier t : ToolTier.values()) {
                if (t.metal() == tier) {
                    smith(t.axeId(), 1, tier, 1, tier.smithingLevel(1));
                    smith(t.pickaxeId(), 1, tier, 2, tier.smithingLevel(5));
                }
            }
            for (ArrowTier a : ArrowTier.values()) {
                if (a.metal() == tier) {
                    smith(a.tipsId(), 15, tier, 1, a.smithingLevel());
                }
            }
        }

        // --- Cooking (fire or range) ---
        for (Fish f : Fish.values()) {
            add(Recipe.builder("cook_" + f.cookedItem(), MenuType.COOKING, f.cookedName())
                    .skill(Skill.COOKING, f.cookLevel(), f.cookXp()).in(f.rawItem(), 1).out(f.cookedItem(), 1)
                    .burn(f.stopBurn(), "burnt_fish").gauntlets(f.gauntletStopBurn()).ticks(4).build());
        }
        meat("roast_bird_meat", "raw_bird_meat", 11, 62.5, 44);
        meat("minecraft:cooked_beef", "minecraft:beef", 1, 30.0, 31);
        meat("minecraft:cooked_chicken", "minecraft:chicken", 1, 30.0, 31);
        meat("minecraft:cooked_porkchop", "minecraft:porkchop", 1, 30.0, 31);
        meat("minecraft:cooked_mutton", "minecraft:mutton", 1, 30.0, 31);
        meat("minecraft:cooked_rabbit", "minecraft:rabbit", 1, 30.0, 31);
        meat("minecraft:cooked_cod", "minecraft:cod", 1, 30.0, 34);
        meat("minecraft:cooked_salmon", "minecraft:salmon", 1, 30.0, 34);
        meat("minecraft:baked_potato", "potato", 7, 15.0, 40);

        // --- Fletching (knife) ---
        for (BowWood w : BowWood.values()) {
            String log = w.tree().logItem();
            if (w == BowWood.NORMAL) {
                add(Recipe.builder("fletch_arrow_shaft", MenuType.FLETCHING, "15 arrow shafts")
                        .skill(Skill.FLETCHING, 1, 5.0).in(log, 1).out("arrow_shaft", 15).tool("knife").build());
            }
            for (boolean longbow : new boolean[]{false, true}) {
                String unstrung = w.bowId(longbow, false);
                add(Recipe.builder("fletch_" + unstrung, MenuType.FLETCHING, w.bowName(longbow, false))
                        .skill(Skill.FLETCHING, w.level(longbow), w.xp(longbow)).in(log, 1).out(unstrung, 1)
                        .tool("knife").build());
            }
        }
        for (BowWood w : BowWood.values()) {
            for (boolean longbow : new boolean[]{false, true}) {
                add(Recipe.builder("string_" + w.bowId(longbow, true), MenuType.FLETCHING, w.bowName(longbow, true))
                        .skill(Skill.FLETCHING, w.level(longbow), w.xp(longbow)).in(w.bowId(longbow, false), 1)
                        .in("bow_string", 1).out(w.bowId(longbow, true), 1).ticks(2).build());
            }
        }
        add(Recipe.builder("fletch_headless_arrow", MenuType.FLETCHING, "15 headless arrows")
                .skill(Skill.FLETCHING, 1, 15.0).in("arrow_shaft", 15).in("minecraft:feather", 15)
                .out("headless_arrow", 15).ticks(2).build());
        for (ArrowTier a : ArrowTier.values()) {
            add(Recipe.builder("fletch_" + a.arrowId(), MenuType.FLETCHING, "15 " + name(a.arrowId()).toLowerCase() + "s")
                    .skill(Skill.FLETCHING, a.fletchingLevel(), a.xpEach() * 15).in("headless_arrow", 15)
                    .in(a.tipsId(), 15).out(a.arrowId(), 15).ticks(2).build());
        }

        // --- Crafting ---
        for (Gem g : Gem.values()) {
            add(Recipe.builder("cut_" + g.cutItem(), MenuType.GEM_CUTTING, g.displayName())
                    .skill(Skill.CRAFTING, g.level(), g.xp()).in(g.uncutItem(), 1).out(g.cutItem(), 1)
                    .tool("chisel").ticks(2).build());
        }
        leather("leather_boots", 7, 16.3, 1);
        leather("leather_cowl", 9, 18.5, 1);
        leather("leather_body", 14, 25.0, 1);
        leather("leather_chaps", 18, 27.0, 1);
        add(Recipe.builder("spin_bow_string", MenuType.SPINNING, "Bow string")
                .skill(Skill.CRAFTING, 10, 15.0).in("flax", 1).out("bow_string", 1).ticks(3).build());
        add(Recipe.builder("spin_ball_of_wool", MenuType.SPINNING, "Ball of wool")
                .skill(Skill.CRAFTING, 1, 2.5).in("minecraft:white_wool", 1).out("ball_of_wool", 1).ticks(3).build());
        add(Recipe.builder("spin_string", MenuType.SPINNING, "String (from wool)")
                .skill(Skill.CRAFTING, 1, 1.0).in("ball_of_wool", 1).out("minecraft:string", 2).ticks(3).build());
        add(Recipe.builder("tan_leather", MenuType.TANNING, "Leather")
                .in("cowhide", 1).in("coins", 1).out("leather", 1).ticks(0).build());

        // --- Herblore ---
        for (Herb h : Herb.values()) {
            add(Recipe.builder("unf_" + h.unfinishedPotion(), MenuType.HERBLORE, h.shortName() + " potion (unf)")
                    .skill(Skill.HERBLORE, h.unfLevel(), 0).in(h.cleanItem(), 1).in("vial_of_water", 1)
                    .out(h.unfinishedPotion(), 1).ticks(1).build());
        }
        for (Potion p : Potion.values()) {
            add(Recipe.builder("mix_" + p.itemId(), MenuType.HERBLORE, p.displayName())
                    .skill(Skill.HERBLORE, p.level(), p.xp()).in(p.herb().unfinishedPotion(), 1).in(p.secondary(), 1)
                    .out(p.itemId(), 1).ticks(2).build());
        }

        // --- Construction (workbench: sawmill + furniture) ---
        plank("plank", "logs", 100);
        plank("oak_plank", "oak_logs", 250);
        plank("teak_plank", "teak_logs", 500);
        plank("mahogany_plank", "mahogany_logs", 1500);
        furniture("Crude wooden chair", 1, 58.0, "plank", 2);
        furniture("Wooden bookcase", 4, 115.0, "plank", 4);
        furniture("Wooden chair", 8, 87.0, "plank", 3);
        furniture("Oak chair", 19, 120.0, "oak_plank", 2);
        furniture("Oak bookcase", 29, 180.0, "oak_plank", 3);
        furniture("Oak larder", 33, 480.0, "oak_plank", 8);
        furniture("Teak armchair", 35, 180.0, "teak_plank", 2);
        furniture("Teak table", 38, 360.0, "teak_plank", 4);
        furniture("Mahogany armchair", 50, 280.0, "mahogany_plank", 2);
        furniture("Mahogany bookcase", 40, 420.0, "mahogany_plank", 3);
        furniture("Mahogany table", 52, 840.0, "mahogany_plank", 6);

        // --- Shops ---
        shop(MenuType.SHOP_GENERAL, "tinderbox", "knife", "chisel", "hammer", "needle", "small_fishing_net",
                "fishing_rod", "fly_fishing_rod", "lobster_pot", "harpoon", "fishing_bait", "minecraft:feather",
                "vial_of_water", "seed_dibber", "bird_snare", "box_trap", "ring_mould", "necklace_mould",
                "amulet_mould", "enchanted_gem", "bronze_axe", "bronze_pickaxe", "potato_seed", "guam_seed",
                "minecraft:bucket", "cake", "pot_of_flour", "beer", "garlic", "vial");
        shop(MenuType.SHOP_RUNES, "air_rune", "water_rune", "earth_rune", "fire_rune", "mind_rune", "body_rune",
                "chaos_rune", "death_rune", "law_rune", "nature_rune", "staff", "staff_of_air", "staff_of_water",
                "staff_of_earth", "staff_of_fire");
        List<String> weapons = new ArrayList<>();
        for (MetalTier tier : new MetalTier[]{MetalTier.BRONZE, MetalTier.IRON, MetalTier.STEEL, MetalTier.BLACK,
                MetalTier.MITHRIL, MetalTier.ADAMANT, MetalTier.RUNE}) {
            weapons.add(WeaponType.DAGGER.itemId(tier));
            weapons.add(WeaponType.SCIMITAR.itemId(tier));
            weapons.add(WeaponType.LONGSWORD.itemId(tier));
        }
        weapons.add("dragon_dagger");
        weapons.add("dragon_scimitar");
        weapons.add("dragon_longsword");
        shop(MenuType.SHOP_WEAPONS, weapons.toArray(new String[0]));
        List<String> tools = new ArrayList<>();
        for (ToolTier t : ToolTier.values()) {
            if (t != ToolTier.DRAGON && t != ToolTier.BLACK) {
                tools.add(t.axeId());
                tools.add(t.pickaxeId());
            }
        }
        shop(MenuType.SHOP_TOOLS, tools.toArray(new String[0]));
        List<String> archery = new ArrayList<>();
        for (BowWood w : BowWood.values()) {
            if (w != BowWood.MAGIC) {
                archery.add(w.bowId(false, true));
                archery.add(w.bowId(true, true));
            }
        }
        for (ArrowTier a : ArrowTier.values()) {
            if (a != ArrowTier.RUNE) {
                archery.add(a.arrowId());
            }
        }
        archery.add("bow_string");
        archery.add("leather_cowl");
        archery.add("leather_body");
        archery.add("leather_chaps");
        archery.add("leather_boots");
        shop(MenuType.SHOP_ARCHERY, archery.toArray(new String[0]));
    }

    private static void smith(String itemId, int amount, MetalTier tier, int bars, int level) {
        add(Recipe.builder("smith_" + itemId, MenuType.SMITHING, (amount > 1 ? amount + " " : "") + name(itemId))
                .skill(Skill.SMITHING, level, tier.xpPerBar() * bars).in(tier.bar(), bars).out(itemId, amount)
                .tool("hammer").ticks(4).build());
    }

    private static void jewellery(String itemId, int level, double xp, String mould, String gem) {
        Recipe.Builder b = Recipe.builder("craft_" + itemId, MenuType.SMELTING, name(itemId))
                .skill(Skill.CRAFTING, level, xp).in("gold_bar", 1).out(itemId, 1).tool(mould).ticks(3);
        if (gem != null) {
            b.in(gem, 1);
        }
        add(b.build());
    }

    private static void meat(String cooked, String raw, int level, double xp, int stopBurn) {
        add(Recipe.builder("cook_" + cooked.replace("minecraft:", "mc_"), MenuType.COOKING, name(cooked))
                .skill(Skill.COOKING, level, xp).in(raw, 1).out(cooked, 1).burn(stopBurn, "burnt_meat").ticks(4).build());
    }

    private static void leather(String itemId, int level, double xp, int leather) {
        add(Recipe.builder("craft_" + itemId, MenuType.LEATHERWORKING, name(itemId))
                .skill(Skill.CRAFTING, level, xp).in("leather", leather).in("minecraft:string", 1).out(itemId, 1)
                .tool("needle").ticks(3).build());
    }

    private static void plank(String plank, String log, int fee) {
        add(Recipe.builder("saw_" + plank, MenuType.CONSTRUCTION, name(plank) + " (" + fee + " coins)")
                .in(log, 1).in("coins", fee).out(plank, 1).ticks(0).build());
    }

    private static void furniture(String name, int level, double xp, String plank, int planks) {
        String id = "build_" + name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        add(Recipe.builder(id, MenuType.CONSTRUCTION, name).skill(Skill.CONSTRUCTION, level, xp)
                .in(plank, planks).tool("hammer").ticks(3).build());
    }

    private static void shop(MenuType type, String... items) {
        for (String item : items) {
            ItemDef def = ItemCatalog.get(item);
            int value = def != null ? Math.max(1, def.value()) : vanillaPrice(item);
            // OSRS rates at normal stock: general stores sell at 130% of value, specialist shops at 100%.
            int price = type == MenuType.SHOP_GENERAL ? Math.max(1, value * 13 / 10) : value;
            add(Recipe.builder(type.name().toLowerCase(java.util.Locale.ROOT) + "_" + item.replace("minecraft:", "mc_"), type,
                    name(item) + " - " + String.format(java.util.Locale.ROOT, "%,d", price) + " gp")
                    .in("coins", price).out(item, 1).ticks(0).build());
        }
    }

    private static int vanillaPrice(String item) {
        return switch (item) {
            case "minecraft:feather" -> 2;
            case "minecraft:bucket" -> 2;
            default -> 10;
        };
    }
}
