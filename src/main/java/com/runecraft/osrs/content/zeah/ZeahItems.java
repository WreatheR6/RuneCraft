package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Kourend and Varlamore items. Items that already exist elsewhere are reused, not redefined:
 * Xeric's talisman and Rada's blessing (systems), the dark totem pieces, ancient shards, sunfire
 * fanatic armour and sunfire splinters (bosses), Kharedst's memoirs (quests), the butterfly net,
 * box trap and tecu salamander (skilling).
 *
 * <p>Equipment bonuses for the Moons of Peril sets, Shayzien armour (5), the guild hunter outfit
 * and the antler bolts are approximations of the wiki values.
 */
public final class ZeahItems {
    private ZeahItems() {
    }

    public static final List<String> BLOOD_MOON = List.of("blood_moon_helm", "blood_moon_chestplate", "blood_moon_tassets",
            "dual_macuahuitl");
    public static final List<String> BLUE_MOON = List.of("blue_moon_helm", "blue_moon_chestplate", "blue_moon_tassets",
            "blue_moon_spear");
    public static final List<String> ECLIPSE_MOON = List.of("eclipse_moon_helm", "eclipse_moon_chestplate", "eclipse_moon_tassets",
            "eclipse_atlatl");
    public static final List<String> SHAYZIEN_ARMOUR = List.of("shayzien_helm_5", "shayzien_platebody_5", "shayzien_greaves_5",
            "shayzien_boots_5");
    public static final List<String> GUILD_HUNTER_OUTFIT = List.of("guild_hunter_headwear", "guild_hunter_top", "guild_hunter_legs",
            "guild_hunter_boots");

    static EquipmentStats st(int stabA, int slashA, int crushA, int magicA, int rangedA, int stabD, int slashD, int crushD,
                             int magicD, int rangedD, int str, int rangedStr, int magicDmg, int prayer) {
        return new EquipmentStats(stabA, slashA, crushA, magicA, rangedA, stabD, slashD, crushD, magicD, rangedD, str, rangedStr,
                magicDmg, prayer);
    }

    private static Map<Skill, Integer> reqs(Object... kv) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        for (int i = 0; i < kv.length; i += 2) {
            map.put((Skill) kv[i], (Integer) kv[i + 1]);
        }
        return map;
    }

    private static ItemDef wear(ItemSink items, String id, String name, String shape, int color, int value, Slot slot,
                                EquipmentStats s, String tier, Object... req) {
        return items.add(id, name, Kind.ARMOUR, shape, color).value(value).stack(1).tier(tier)
                .equipment(new EquipmentDef(slot, s, reqs(req), 0, AttackType.CRUSH, WeaponClass.NONE));
    }

    private static ItemDef weapon(ItemSink items, String id, String name, Kind kind, String shape, int color, int value,
                                  EquipmentStats s, int speed, AttackType type, WeaponClass wc, Object... req) {
        return items.add(id, name, kind, shape, color).value(value).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, s, reqs(req), speed, type, wc));
    }

    private static ItemDef simple(ItemSink items, String id, String name, String shape, int color, int value, int stack,
                                  String examine) {
        return items.add(id, name, Kind.SIMPLE, shape, color).value(value).stack(stack).examine(examine);
    }

    public static void add(ItemSink items) {
        // ---- Kourend ----
        simple(items, "lizardman_fang", "Lizardman fang", "secondary", 0xE8E0C8, 10, 64000,
                "Dropped by lizardmen. Charges Xeric's talisman.");
        simple(items, "xerics_talisman_inert", "Xeric's talisman (inert)", "talisman", 0x5A8A80, 100, 1,
                "An inert talisman. Lizardman fangs would bring it back to life.");
        simple(items, "book_of_the_dead", "Book of the dead", "book", 0x3A1A4A, 1, 1,
                "A tome of Arceuus necromancy. Its pages allow teleports to the Kourend graveyards.");
        items.add("lovakite_ore", "Lovakite ore", Kind.SIMPLE, "ore", 0x4A6A7A).value(60).stack(64)
                .examine("Lovakite ore, mined beneath Lovakengj.");
        items.add("lovakite_bar", "Lovakite bar", Kind.SIMPLE, "bar", 0x5A7A8A).value(120).stack(64)
                .examine("A bar of lovakite, used to make Shayzien armour.");
        simple(items, "dynamite", "Dynamite", "stake", 0xC03A2A, 40, 64000, "Highly explosive. Light it next to hard rock.");
        int shay = 0x8A2A2A;
        wear(items, "shayzien_helm_5", "Shayzien helm (5)", "full_helm", shay, 2000, Slot.HEAD,
                st(0, 0, 0, -3, -1, 14, 15, 13, -1, 14, 0, 0, 0, 1), "adamant", Skill.DEFENCE, 20);
        wear(items, "shayzien_platebody_5", "Shayzien platebody (5)", "platebody", shay, 6000, Slot.CHEST,
                st(0, 0, 0, -15, -5, 42, 44, 38, -6, 42, 0, 0, 0, 1), "adamant", Skill.DEFENCE, 20);
        wear(items, "shayzien_greaves_5", "Shayzien greaves (5)", "platelegs", shay, 4000, Slot.LEGS,
                st(0, 0, 0, -10, -3, 28, 27, 25, -4, 27, 0, 0, 0, 1), "adamant", Skill.DEFENCE, 20);
        wear(items, "shayzien_boots_5", "Shayzien boots (5)", "boots", shay, 1000, Slot.FEET,
                st(0, 0, 0, -3, -1, 7, 8, 9, 0, 8, 0, 0, 0, 1), "adamant", Skill.DEFENCE, 20);

        // ---- Varlamore Hunter ----
        int hunter = 0x5A6A3A;
        wear(items, "guild_hunter_headwear", "Guild hunter headwear", "cowl", hunter, 1, Slot.HEAD,
                st(0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0), "leather");
        wear(items, "guild_hunter_top", "Guild hunter top", "leather_body", hunter, 1, Slot.CHEST,
                st(0, 0, 0, 0, 4, 6, 6, 6, 4, 6, 0, 0, 0, 0), "leather");
        wear(items, "guild_hunter_legs", "Guild hunter legs", "chaps", hunter, 1, Slot.LEGS,
                st(0, 0, 0, 0, 3, 4, 4, 4, 3, 4, 0, 0, 0, 0), "leather");
        wear(items, "guild_hunter_boots", "Guild hunter boots", "boots", hunter, 1, Slot.FEET,
                st(0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0), "leather");
        simple(items, "quetzal_whistle_basic", "Basic quetzal whistle", "talisman", 0x3AA05A, 1, 1,
                "Summons a quetzal to carry you to the Hunter Guild.");
        simple(items, "quetzal_whistle_enhanced", "Enhanced quetzal whistle", "talisman", 0x3AB07A, 1, 1,
                "Summons a quetzal to carry you to the Hunter Guild.");
        simple(items, "quetzal_whistle_perfected", "Perfected quetzal whistle", "talisman", 0x4AC09A, 1, 1,
                "Summons a quetzal to carry you to the Hunter Guild.");
        simple(items, "hunters_loot_sack", "Hunters' loot sack", "package", 0x8A6A3A, 1, 64,
                "A reward from the Hunter Guild for completing a rumour.");
        simple(items, "butterfly_jar", "Butterfly jar", "jar", 0xD0E0E8, 1, 64, "An empty jar for catching moths and butterflies.");
        simple(items, "sunlight_moth", "Sunlight moth", "jar", 0xE0C040, 50, 64, "A sunlight moth in a jar.");
        simple(items, "moonlight_moth", "Moonlight moth", "jar", 0x8090D0, 70, 64, "A moonlight moth in a jar.");
        simple(items, "sunlight_antler", "Sunlight antler", "secondary", 0xE0C080, 100, 64, "The antler of a sunlight antelope.");
        simple(items, "moonlight_antler", "Moonlight antler", "secondary", 0xB0B8E0, 150, 64, "The antler of a moonlight antelope.");
        simple(items, "sunlight_antelope_fur", "Sunlight antelope fur", "fur", 0xD0A050, 120, 64, "Golden fur.");
        simple(items, "moonlight_antelope_fur", "Moonlight antelope fur", "fur", 0x6A7AB0, 160, 64, "Silvery fur.");
        Object[][] meats = {{"sunlight_antelope", "Sunlight antelope", 12}, {"moonlight_antelope", "Moonlight antelope", 14},
                {"embertailed_jerboa", "Embertailed jerboa", 8}};
        for (Object[] m : meats) {
            items.add("raw_" + m[0], "Raw " + ((String) m[1]).toLowerCase(), Kind.SIMPLE, "meat_raw", 0xC07060).value(20).stack(64);
            items.add("cooked_" + m[0], "Cooked " + ((String) m[1]).toLowerCase(), Kind.FOOD, "meat_cooked", 0x8A5030).value(40)
                    .stack(64).heal((Integer) m[2]);
        }
        items.add("sunlight_antler_bolts", "Sunlight antler bolts", Kind.ARROW, "arrow", 0xE0C080).value(60).stack(64000)
                .rangedStrength(55).tier("dragon").examine("Bolts for the hunters' sunlight crossbow.");
        items.add("moonlight_antler_bolts", "Moonlight antler bolts", Kind.ARROW, "arrow", 0xB0B8E0).value(90).stack(64000)
                .rangedStrength(65).tier("dragon").examine("Bolts for the hunters' sunlight crossbow.");
        weapon(items, "hunters_sunlight_crossbow", "Hunters' sunlight crossbow", Kind.BOW, "crossbow", 0xC0A050, 10000,
                st(0, 0, 0, 0, 72, 0, 0, 0, 0, 0, 0, 0, 0, 0), 5, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 50)
                .examine("A crossbow made for hunting; fires antler bolts.");

        // ---- Moons of Peril (Lunar Chest) ----
        int blood = 0x8A1A1A;
        wear(items, "blood_moon_helm", "Blood moon helm", "full_helm", blood, 30000, Slot.HEAD,
                st(0, 0, 0, -5, -5, 30, 32, 28, -1, 30, 4, 0, 0, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "blood_moon_chestplate", "Blood moon chestplate", "platebody", blood, 60000, Slot.CHEST,
                st(0, 0, 0, -15, -10, 70, 72, 62, -5, 70, 8, 0, 0, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "blood_moon_tassets", "Blood moon tassets", "platelegs", blood, 45000, Slot.LEGS,
                st(0, 0, 0, -10, -7, 50, 52, 46, -3, 50, 6, 0, 0, 0), "dragon", Skill.DEFENCE, 75);
        weapon(items, "dual_macuahuitl", "Dual macuahuitl", Kind.MELEE_WEAPON, "club", 0x6A3A2A, 80000,
                st(0, 0, 85, 0, 0, 0, 0, 0, 0, 0, 85, 0, 0, 0), 4, AttackType.CRUSH, WeaponClass.MELEE, Skill.ATTACK, 75)
                .examine("A pair of obsidian-edged clubs. Each attack strikes twice.");
        int blue = 0x3A5AA0;
        wear(items, "blue_moon_helm", "Blue moon helm", "full_helm", blue, 30000, Slot.HEAD,
                st(0, 0, 0, 6, -2, 20, 22, 18, 8, 20, 2, 0, 1, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "blue_moon_chestplate", "Blue moon chestplate", "platebody", blue, 60000, Slot.CHEST,
                st(0, 0, 0, 15, -5, 45, 46, 40, 20, 44, 4, 0, 2, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "blue_moon_tassets", "Blue moon tassets", "platelegs", blue, 45000, Slot.LEGS,
                st(0, 0, 0, 10, -3, 32, 33, 30, 14, 32, 3, 0, 1, 0), "dragon", Skill.DEFENCE, 75);
        weapon(items, "blue_moon_spear", "Blue moon spear", Kind.MELEE_WEAPON, "spear", 0x6A8AC0, 80000,
                st(70, 50, 0, 20, 0, 0, 0, 0, 0, 0, 72, 0, 0, 0), 5, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 75)
                .examine("A frost-touched spear that can also channel water magic.");
        int eclipse = 0xC08A2A;
        wear(items, "eclipse_moon_helm", "Eclipse moon helm", "full_helm", eclipse, 30000, Slot.HEAD,
                st(0, 0, 0, -5, 8, 20, 20, 20, 10, 20, 0, 2, 0, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "eclipse_moon_chestplate", "Eclipse moon chestplate", "platebody", eclipse, 60000, Slot.CHEST,
                st(0, 0, 0, -10, 18, 45, 45, 45, 25, 45, 0, 4, 0, 0), "dragon", Skill.DEFENCE, 75);
        wear(items, "eclipse_moon_tassets", "Eclipse moon tassets", "platelegs", eclipse, 45000, Slot.LEGS,
                st(0, 0, 0, -7, 12, 32, 32, 32, 18, 32, 0, 3, 0, 0), "dragon", Skill.DEFENCE, 75);
        weapon(items, "eclipse_atlatl", "Eclipse atlatl", Kind.BOW, "shortbow", 0xD0A040, 80000,
                st(0, 0, 0, 0, 70, 0, 0, 0, 0, 0, 0, 0, 0, 0), 3, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 75)
                .examine("Hurls atlatl darts. In OSRS its damage scales with Strength.");
        items.add("atlatl_dart", "Atlatl dart", Kind.ARROW, "arrow", 0xA08050).value(30).stack(64000).rangedStrength(40).tier("dragon")
                .examine("Darts for the eclipse atlatl.");
    }

    public static void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("charge_xerics_talisman", MenuType.SMITHING, "Xeric's talisman")
                .in("xerics_talisman_inert", 1).in("lizardman_fang", 100).out("xerics_talisman", 1).build());
        recipes.accept(Recipe.builder("smelt_lovakite_bar", MenuType.SMELTING, "Lovakite bar").skill(Skill.SMITHING, 45, 20.0)
                .in("lovakite_ore", 1).in("coal", 2).out("lovakite_bar", 1).ticks(4).build());
        // Cooking levels and XP: jerboa / sunlight / moonlight antelope (approx.).
        Object[][] cook = {{"embertailed_jerboa", 41, 70.0}, {"sunlight_antelope", 68, 175.0}, {"moonlight_antelope", 92, 220.0}};
        for (Object[] c : cook) {
            int lvl = (Integer) c[1];
            recipes.accept(Recipe.builder("cook_cooked_" + c[0], MenuType.COOKING, "Cooked " + ((String) c[0]).replace('_', ' '))
                    .skill(Skill.COOKING, lvl, (Double) c[2]).in("raw_" + c[0], 1).out("cooked_" + c[0], 1)
                    .burn(Math.min(99, lvl + 25), "burnt_meat").ticks(4).build());
        }
        recipes.accept(Recipe.builder("fletch_sunlight_antler_bolts", MenuType.FLETCHING, "Sunlight antler bolts")
                .skill(Skill.FLETCHING, 87, 12.0).in("sunlight_antler", 1).out("sunlight_antler_bolts", 10).tool("chisel").ticks(3)
                .build());
        // Hunters' sunlight crossbow (approx: OSRS fits a sunlight antler to a hunters' crossbow).
        recipes.accept(Recipe.builder("fletch_hunters_sunlight_crossbow", MenuType.FLETCHING, "Hunters' sunlight crossbow")
                .skill(Skill.FLETCHING, 62, 100.0).in("sunlight_antler", 2).in("yew_logs", 1).in("bow_string", 1)
                .out("hunters_sunlight_crossbow", 1).tool("knife").ticks(3).build());
        recipes.accept(Recipe.builder("fletch_moonlight_antler_bolts", MenuType.FLETCHING, "Moonlight antler bolts")
                .skill(Skill.FLETCHING, 92, 14.0).in("moonlight_antler", 1).out("moonlight_antler_bolts", 10).tool("chisel").ticks(3)
                .build());
    }

    /** A piece the Shayzien quartermaster sells and its price (approx: OSRS ties the armour to the combat ring). */
    public record QuartermasterOffer(String item, int price) {
    }

    public static final List<QuartermasterOffer> QUARTERMASTER = List.of(new QuartermasterOffer("shayzien_helm_5", 4000),
            new QuartermasterOffer("shayzien_platebody_5", 12000), new QuartermasterOffer("shayzien_greaves_5", 8000),
            new QuartermasterOffer("shayzien_boots_5", 2000));

    public static List<CollectionLogData.Page> collectionLog() {
        return List.of(
                new CollectionLogData.Page("Other", "Catacombs of Kourend", List.of("ancient_shard", "dark_totem_base",
                        "dark_totem_middle", "dark_totem_top")),
                new CollectionLogData.Page("Other", "Lizardmen", List.of("xerics_talisman_inert", "lizardman_fang")),
                new CollectionLogData.Page("Other", "Shayzien Armour", SHAYZIEN_ARMOUR),
                new CollectionLogData.Page("Minigames", "Hunter Guild", List.of("guild_hunter_headwear", "guild_hunter_top",
                        "guild_hunter_legs", "guild_hunter_boots", "quetzal_whistle_basic", "quetzal_whistle_enhanced",
                        "quetzal_whistle_perfected", "hunters_sunlight_crossbow")));
    }
}
