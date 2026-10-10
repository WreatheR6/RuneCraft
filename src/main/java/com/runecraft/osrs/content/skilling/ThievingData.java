package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Thieving: more pickpocket targets (master farmer and his seed table, rogue, knight of Ardougne,
 * watchman, menaphite thug, paladin, gnome, hero, vyre, elf, TzHaar-Hur), thieving chests and extra
 * market stalls. Levels, XP, stun damage and respawn times are OSRS values; success rates use the
 * standard low/high interpolation (approximate).
 */
public final class ThievingData extends SkillingPart {

    /**
     * A pickpocket target (a module NPC). {@code always} is given on every success; one line of
     * {@code table} is rolled on top of it (an empty table gives only {@code always}).
     */
    public record Target(String npcId, String name, String place, int dx, int dz, int level, double xp, int low, int high,
                         int stunDamage, List<Loot> always, List<Loot> table, int[] colors) {
    }

    public static final List<Loot> SEED_TABLE = List.of(
            Loot.of("potato_seed", 1, 4, 300), Loot.of("onion_seed", 1, 3, 200), Loot.of("cabbage_seed", 1, 3, 150),
            Loot.of("tomato_seed", 1, 2, 100), Loot.of("sweetcorn_seed", 1, 2, 70), Loot.of("strawberry_seed", 1, 1, 40),
            Loot.of("watermelon_seed", 1, 1, 20), Loot.of("snape_grass_seed", 1, 1, 10), Loot.of("barley_seed", 1, 4, 120),
            Loot.of("hammerstone_seed", 1, 3, 100), Loot.of("asgarnian_seed", 1, 2, 80), Loot.of("jute_seed", 1, 3, 70),
            Loot.of("yanillian_seed", 1, 2, 50), Loot.of("krandorian_seed", 1, 2, 30), Loot.of("wildblood_seed", 1, 1, 20),
            Loot.of("redberry_seed", 60), Loot.of("cadavaberry_seed", 40), Loot.of("dwellberry_seed", 30),
            Loot.of("jangerberry_seed", 15), Loot.of("whiteberry_seed", 8), Loot.of("poison_ivy_seed", 5),
            Loot.of("guam_seed", 80), Loot.of("marrentill_seed", 60), Loot.of("tarromin_seed", 45), Loot.of("harralander_seed", 30),
            Loot.of("ranarr_seed", 9), Loot.of("toadflax_seed", 13), Loot.of("irit_seed", 9), Loot.of("avantoe_seed", 7),
            Loot.of("kwuarm_seed", 6), Loot.of("snapdragon_seed", 2), Loot.of("cadantine_seed", 2), Loot.of("lantadyme_seed", 2),
            Loot.of("dwarf_weed_seed", 1), Loot.of("torstol_seed", 1), Loot.of("mushroom_spore", 10), Loot.of("belladonna_seed", 4),
            Loot.of("cactus_seed", 4), Loot.of("potato_cactus_seed", 2));

    public static final List<Target> TARGETS = List.of(
            new Target("rogue_pickpocket", "Rogue", "EDGEVILLE", 5, 4, 32, 36.5, 74, 240, 2,
                    List.of(), List.of(Loot.of("coins", 25, 120, 60), Loot.of("air_rune", 8, 8, 15), Loot.of("jug_of_wine", 10),
                    Loot.of("iron_dagger", 5), Loot.of("lockpick", 10)), new int[]{0xC8A080, 0x3A2A2A, 0x2A2A2A, 0x1A1A1A}),
            new Target("master_farmer", "Master Farmer", "DRAYNOR_VILLAGE", 4, -4, 38, 43.0, 90, 240, 3,
                    List.of(), SEED_TABLE, new int[]{0xC8A080, 0x4A7A2A, 0x6A4A2A, 0xC0A060}),
            new Target("knight_of_ardougne", "Knight of Ardougne", "ARDOUGNE", -5, 3, 55, 84.3, 50, 240, 3,
                    List.of(Loot.of("coins", 50, 50, 1)), List.of(), new int[]{0xC8A080, 0xA0A0A8, 0x6A6A70, 0x9A9AA0}),
            new Target("ardougne_paladin", "Paladin", "ARDOUGNE", 5, -3, 70, 151.8, 50, 230, 3,
                    List.of(Loot.of("coins", 80, 80, 1)), List.of(Loot.of("chaos_rune", 2, 2, 1), Loot.of("coins", 1, 1, 7)),
                    new int[]{0xC8A080, 0xE0E0E8, 0xC0C0C8, 0xE0C040}),
            new Target("watchman", "Watchman", "YANILLE", 4, 4, 65, 137.5, 15, 160, 3,
                    List.of(Loot.of("coins", 60, 60, 1)), List.of(Loot.of("bread", 1, 1, 1), Loot.of("coins", 1, 1, 3)),
                    new int[]{0xC8A080, 0x3A5A8A, 0x2A3A5A, 0x2A2A2A}),
            new Target("menaphite_thug", "Menaphite Thug", "POLLNIVNEACH", 3, 4, 65, 137.5, 60, 240, 5,
                    List.of(Loot.of("coins", 60, 60, 1)), List.of(), new int[]{0xA07850, 0x8A2A2A, 0x5A3A2A, 0x2A2A2A}),
            new Target("gnome_pickpocket", "Gnome", "GNOME_STRONGHOLD", 4, 5, 75, 198.5, 8, 120, 1,
                    List.of(), List.of(Loot.of("coins", 300, 300, 40), Loot.of("earth_rune", 1, 1, 15), Loot.of("gold_ore", 10),
                    Loot.of("fire_orb", 5), Loot.of("uncut_sapphire", 10), Loot.of("mithril_ore", 10)),
                    new int[]{0xC8A080, 0x3A8A3A, 0x6A4A2A, 0x9A2A2A}),
            new Target("ardougne_hero", "Hero", "ARDOUGNE", 0, 6, 80, 273.3, 40, 220, 4,
                    List.of(), List.of(Loot.of("coins", 200, 300, 70), Loot.of("death_rune", 2, 2, 8), Loot.of("blood_rune", 1, 1, 6),
                    Loot.of("fire_orb", 4), Loot.of("diamond", 2), Loot.of("gold_ore", 4), Loot.of("jug_of_wine", 6)),
                    new int[]{0xC8A080, 0x7A2A2A, 0x3A3A3A, 0xE0C040}),
            new Target("vyre_pickpocket", "Vyre", "CANIFIS", -4, 5, 82, 306.9, 35, 210, 5,
                    List.of(), List.of(Loot.of("coins", 250, 250, 60), Loot.of("death_rune", 4, 4, 15), Loot.of("diamond", 4),
                    Loot.of("uncut_ruby", 6), Loot.of("vial_of_blood", 10)), new int[]{0xB0B0B8, 0x4A1A2A, 0x2A1A1A, 0x1A1A1A}),
            new Target("elf_pickpocket", "Elf", "LLETYA", 4, -4, 85, 353.3, 34, 200, 5,
                    List.of(), List.of(Loot.of("coins", 280, 350, 60), Loot.of("death_rune", 2, 2, 10), Loot.of("nature_rune", 3, 3, 10),
                    Loot.of("diamond", 3), Loot.of("gold_ore", 5), Loot.of("fire_orb", 4), Loot.of("crystal_dust", 3, 6, 4)),
                    new int[]{0xE0D0B0, 0x3A8A5A, 0x2A5A3A, 0xE0E0A0}),
            new Target("tzhaar_hur", "TzHaar-Hur", "MOR_UL_REK", 5, 5, 90, 103.4, 40, 200, 4,
                    List.of(), List.of(Loot.of("tokkul", 3, 14, 70), Loot.of("uncut_sapphire", 6), Loot.of("uncut_emerald", 4),
                    Loot.of("uncut_ruby", 2), Loot.of("uncut_diamond", 1)), new int[]{0x8A3A1A, 0x5A2A1A, 0x4A2A1A, 0xE06010}));

    public static Target target(String npcId) {
        for (Target t : TARGETS) {
            if (t.npcId().equals(npcId)) {
                return t;
            }
        }
        return null;
    }

    /** A thieving chest or stall block: all of {@code always} plus one {@code table} roll; respawn in game ticks. */
    public record Container(String blockId, String name, boolean chest, int level, double xp, int respawnTicks, List<Loot> always,
                            List<Loot> table, String look, int color) {
    }

    public static final List<Container> CONTAINERS = List.of(
            new Container("ten_coin_chest", "Chest (10 coins)", true, 13, 7.8, 7, List.of(Loot.of("coins", 10, 10, 1)), List.of(),
                    "chest", 0x8A6A3A),
            new Container("nature_rune_chest", "Chest (nature runes)", true, 28, 25.0, 13,
                    List.of(Loot.of("coins", 3, 3, 1), Loot.of("nature_rune", 1, 1, 1)), List.of(), "chest", 0x8A6A3A),
            new Container("fifty_coin_chest", "Chest (50 coins)", true, 43, 125.0, 83, List.of(Loot.of("coins", 50, 50, 1)), List.of(),
                    "chest", 0x8A6A3A),
            new Container("arrowtip_chest", "Chest (steel arrowtips)", true, 47, 150.0, 130,
                    List.of(Loot.of("coins", 20, 20, 1), Loot.of("steel_arrowtips", 5, 5, 1)), List.of(), "chest", 0x8A6A3A),
            new Container("blood_rune_chest", "Chest (blood runes)", true, 59, 250.0, 225,
                    List.of(Loot.of("coins", 500, 500, 1), Loot.of("blood_rune", 2, 2, 1)), List.of(), "chest", 0x6A2A2A),
            new Container("ardougne_castle_chest", "Chest (Ardougne castle)", true, 72, 500.0, 666,
                    List.of(Loot.of("coins", 1000, 1000, 1), Loot.of("raw_shark", 1, 1, 1), Loot.of("adamantite_ore", 1, 1, 1),
                            Loot.of("uncut_sapphire", 1, 1, 1)), List.of(), "chest", 0xA08040),
            new Container("rogues_castle_chest", "Chest (Rogues' Castle)", true, 84, 701.7, 15, List.of(),
                    List.of(Loot.of("coins", 100, 500, 50), Loot.of("raw_shark", 1, 3, 15), Loot.of("death_rune", 10, 30, 10),
                            Loot.of("uncut_diamond", 6), Loot.of("uncut_ruby", 8), Loot.of("dragonstone", 1), Loot.of("rune_dagger", 2),
                            Loot.of("adamantite_ore", 1, 4, 8)), "chest", 0x5A4A3A),
            new Container("vegetable_stall", "Vegetable stall", false, 2, 10.0, 3, List.of(),
                    List.of(Loot.of("potato", 1), Loot.of("onion", 1), Loot.of("cabbage", 1), Loot.of("tomato", 1), Loot.of("garlic", 1)),
                    "stall", 0x6A9A3A),
            new Container("tea_stall", "Tea stall", false, 5, 16.0, 12, List.of(), List.of(Loot.of("cup_of_tea", 1)), "stall", 0xA06030),
            new Container("wine_stall", "Wine stall", false, 22, 27.0, 27, List.of(),
                    List.of(Loot.of("jug_of_wine", 2), Loot.of("grapes", 3), Loot.of("jug_of_water", 3), Loot.of("jug_of_bad_wine", 2),
                            Loot.of("jug", 2)), "stall", 0x8A1A3A),
            new Container("seed_stall", "Seed stall", false, 27, 10.0, 18, List.of(), SEED_TABLE, "stall", 0x4A7A2A),
            new Container("fish_stall", "Fish stall", false, 42, 42.0, 27, List.of(),
                    List.of(Loot.of("raw_salmon", 4), Loot.of("raw_tuna", 3), Loot.of("raw_lobster", 2)), "stall", 0x5A80A0),
            new Container("crossbow_stall", "Crossbow stall", false, 49, 52.0, 15, List.of(),
                    List.of(Loot.of("bronze_bolts", 3, 3, 1), Loot.of("bronze_limbs", 1), Loot.of("wooden_stock", 1)), "stall", 0x8A6A3A),
            new Container("magic_stall", "Magic stall", false, 65, 100.0, 133, List.of(),
                    List.of(Loot.of("air_rune", 1), Loot.of("water_rune", 1), Loot.of("earth_rune", 1), Loot.of("fire_rune", 1),
                            Loot.of("law_rune", 1)), "stall", 0x5A3A8A),
            new Container("scimitar_stall", "Scimitar stall", false, 65, 100.0, 133, List.of(),
                    List.of(Loot.of("iron_scimitar", 4), Loot.of("steel_scimitar", 1)), "stall", 0x8A8A9A),
            new Container("ore_stall", "Ore stall", false, 82, 180.0, 100, List.of(),
                    List.of(Loot.of("iron_ore", 30), Loot.of("silver_ore", 15), Loot.of("coal", 30), Loot.of("gold_ore", 12),
                            Loot.of("mithril_ore", 8), Loot.of("adamantite_ore", 4), Loot.of("runite_ore", 1)), "stall", 0x6A6A6A));

    public static Container container(String blockId) {
        for (Container c : CONTAINERS) {
            if (c.blockId().equals(blockId)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        tool(s, "lockpick", "Lockpick", "key", 0x8A8A9A, 20);
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        for (Target t : TARGETS) {
            int[] c = t.colors();
            list.add(NpcDef.of(t.npcId(), t.name(), t.place(), t.dx(), t.dz(), c[0], c[1], c[2], c[3]));
        }
        return list;
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Container c : CONTAINERS) {
            list.add(BlockDef.scripted(c.blockId(), c.name(), c.color(), c.look()));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("ardougne_chests", "Ardougne chests", 2671, 3299, PlaceDef.Style.TOWN,
                        List.of("ten_coin_chest", "nature_rune_chest", "fifty_coin_chest")),
                PlaceDef.of("ardougne_castle", "Ardougne Castle", 2588, 3291, PlaceDef.Style.TOWN, List.of("ardougne_castle_chest")),
                PlaceDef.of("rellekka_arrowtip_chest", "Rellekka chest", 2648, 3660, PlaceDef.Style.CAMP, List.of("arrowtip_chest", "fish_stall")),
                PlaceDef.of("morytania_blood_chest", "Sanguinesti chest", 3585, 3360, PlaceDef.Style.RUINS, List.of("blood_rune_chest")),
                PlaceDef.of("rogues_castle", "Rogues' Castle", 3286, 3930, PlaceDef.Style.RUINS,
                        List.of("rogues_castle_chest", "rogues_castle_chest")),
                PlaceDef.of("draynor_market", "Draynor market", 3080, 3250, PlaceDef.Style.TOWN,
                        List.of("seed_stall", "wine_stall", "vegetable_stall")),
                PlaceDef.of("varrock_tea_stall", "Varrock tea stall", 3270, 3410, PlaceDef.Style.TOWN, List.of("tea_stall")),
                PlaceDef.of("keldagrim_stalls", "Keldagrim market", 2765, 3712, PlaceDef.Style.CAVE,
                        List.of("crossbow_stall")),
                PlaceDef.of("ape_atoll_stalls", "Ape Atoll market", 2760, 2785, PlaceDef.Style.CAMP,
                        List.of("magic_stall", "scimitar_stall")),
                PlaceDef.of("lovakengj_ore_stall", "Lovakengj ore stall", 1490, 3770, PlaceDef.Style.TOWN, List.of("ore_stall")));
    }
}
