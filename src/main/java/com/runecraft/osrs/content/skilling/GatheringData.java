package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Woodcutting, Firemaking and Fishing extras: arctic pines, achey trees and sulliuscep mushrooms;
 * barbarian (leaping fish), big net, karambwanji/karambwan, cave/slimy eel, lava eel, sacred and
 * infernal eel and deep sea (sea turtle/manta ray) fishing spots; caskets and oysters.
 */
public final class GatheringData extends SkillingPart {

    /** A tree added by this module. Logs light with a tinderbox at {@code fmLevel} for {@code fmXp}. */
    public record ModTree(String blockId, String name, int level, double xp, String log, int low, int high, double depleteChance,
                          int respawnTicks, int fmLevel, double fmXp, int color) {
    }

    public static final List<ModTree> TREES = List.of(
            new ModTree("achey_tree", "Achey tree", 1, 25.0, "achey_tree_logs", 64, 200, 1.0, 60, 1, 40.0, 0x8A5A3A),
            new ModTree("arctic_pine_tree", "Arctic pine", 54, 40.0, "arctic_pine_logs", 6, 30, 0.125, 34, 42, 125.0, 0x4A5A3A),
            new ModTree("sulliuscep", "Sulliuscep", 65, 127.0, "sulliuscep_cap", 4, 20, 0.25, 50, 0, 0, 0xB08A5A));

    public static ModTree tree(String blockId) {
        for (ModTree t : TREES) {
            if (t.blockId().equals(blockId)) {
                return t;
            }
        }
        return null;
    }

    public static ModTree treeByLog(String log) {
        for (ModTree t : TREES) {
            if (t.log().equals(log) && t.fmLevel() > 0) {
                return t;
            }
        }
        return null;
    }

    /**
     * One possible catch: item, fishing level, XP, success low/high (out of 256), and extra Agility
     * and Strength XP plus their level requirements (barbarian fishing).
     */
    public record Catch(String item, int level, double xp, int low, int high, double agilityXp, double strengthXp, int agilityReq,
                        int strengthReq) {
        static Catch of(String item, int level, double xp, int low, int high) {
            return new Catch(item, level, xp, low, high, 0, 0, 1, 1);
        }
    }

    /** A fishing spot added by this module. {@code bait} may be null. */
    public record Spot(String blockId, String name, String tool, String bait, List<Catch> catches, List<Loot> junk, int junkChance) {
    }

    public static final List<Spot> SPOTS = List.of(
            new Spot("big_net_fishing_spot", "Fishing spot (big net)", "big_fishing_net", null, List.of(
                    Catch.of("raw_bass", 46, 100.0, 4, 40), Catch.of("raw_cod", 23, 45.0, 8, 64), Catch.of("raw_mackerel", 16, 20.0, 16, 96)),
                    List.of(Loot.of("seaweed", 1, 2, 60), Loot.of("oyster", 15), Loot.of("casket", 5), Loot.of("leather_boots", 10),
                            Loot.of("leather_gloves", 10)), 10),
            new Spot("barbarian_fishing_spot", "Fishing spot (barbarian)", "barbarian_rod", "fishing_bait", List.of(
                    new Catch("leaping_sturgeon", 70, 80.0, 4, 50, 7.0, 7.0, 45, 45),
                    new Catch("leaping_salmon", 58, 70.0, 8, 70, 6.0, 6.0, 30, 30),
                    new Catch("leaping_trout", 48, 50.0, 32, 190, 5.0, 5.0, 15, 15)), List.of(), 0),
            new Spot("karambwanji_fishing_spot", "Fishing spot (karambwanji)", "small_fishing_net", null, List.of(
                    Catch.of("raw_karambwanji", 5, 5.0, 100, 250)), List.of(), 0),
            new Spot("karambwan_fishing_spot", "Fishing spot (karambwan)", "karambwan_vessel", "raw_karambwanji", List.of(
                    Catch.of("raw_karambwan", 65, 50.0, 6, 80)), List.of(), 0),
            new Spot("eel_fishing_spot", "Fishing spot (cave eels)", "fishing_rod", "fishing_bait", List.of(
                    Catch.of("raw_cave_eel", 38, 80.0, 10, 80), Catch.of("raw_slimy_eel", 28, 65.0, 10, 80)), List.of(), 0),
            new Spot("lava_eel_fishing_spot", "Fishing spot (lava eels)", "oily_fishing_rod", "fishing_bait", List.of(
                    Catch.of("raw_lava_eel", 53, 60.0, 16, 96)), List.of(), 0),
            new Spot("sacred_eel_fishing_spot", "Fishing spot (sacred eels)", "fishing_rod", "fishing_bait", List.of(
                    Catch.of("sacred_eel", 87, 105.0, 10, 80)), List.of(), 0),
            new Spot("infernal_eel_fishing_spot", "Fishing spot (infernal eels)", "oily_fishing_rod", "fishing_bait", List.of(
                    Catch.of("infernal_eel", 80, 95.0, 20, 100)), List.of(), 0),
            new Spot("deep_sea_fishing_spot", "Fishing spot (deep sea)", "harpoon", null, List.of(
                    Catch.of("raw_manta_ray", 81, 46.0, 4, 30), Catch.of("raw_sea_turtle", 79, 38.0, 6, 40),
                    Catch.of("raw_shark", 76, 110.0, 3, 40)), List.of(Loot.of("casket", 1), Loot.of("seaweed", 4)), 4));

    public static Spot spot(String blockId) {
        for (Spot s : SPOTS) {
            if (s.blockId().equals(blockId)) {
                return s;
            }
        }
        return null;
    }

    public static final List<Loot> CASKET = List.of(
            Loot.of("coins", 20, 640, 60), Loot.of("uncut_sapphire", 10), Loot.of("uncut_emerald", 6), Loot.of("uncut_ruby", 3),
            Loot.of("uncut_diamond", 1), Loot.of("cosmic_rune", 4, 12, 6), Loot.of("law_rune", 2, 8, 4), Loot.of("clue_scroll_easy", 4),
            Loot.of("raw_swordfish", 3), Loot.of("silver_bar", 3));

    public static final List<Loot> OYSTER = List.of(Loot.of("empty_oyster", 9), Loot.of("oyster_pearl", 1));

    /** Infernal eels are crushed with a hammer (OSRS-like weights). */
    public static final List<Loot> INFERNAL_EEL = List.of(Loot.of("tokkul", 14, 20, 75), Loot.of("lava_scale_shard", 1, 5, 20),
            Loot.of("onyx_bolt_tips", 1, 1, 1));

    @Override
    public void items(ItemSink s) {
        simple(s, "achey_tree_logs", "Achey tree logs", "log", 0x8A5A3A, 4);
        simple(s, "arctic_pine_logs", "Arctic pine logs", "log", 0x4A5A3A, 30);
        simple(s, "sulliuscep_cap", "Sulliuscep cap", "produce", 0xB08A5A, 50);
        simple(s, "split_log", "Split log", "log", 0x4A5A3A, 15);
        tool(s, "big_fishing_net", "Big fishing net", "net", 0xB0A080, 20);
        tool(s, "barbarian_rod", "Barbarian rod", "rod", 0x7A5A3A, 5);
        tool(s, "karambwan_vessel", "Karambwan vessel", "pot", 0xA08050, 5);
        tool(s, "oily_fishing_rod", "Oily fishing rod", "rod", 0x4A4A2A, 15);
        simple(s, "raw_karambwanji", "Raw karambwanji", "fish_raw", 0x8AAA5A, 2);
        simple(s, "leaping_trout", "Leaping trout", "fish_raw", 0xC09070, 10);
        simple(s, "leaping_salmon", "Leaping salmon", "fish_raw", 0xE07050, 20);
        simple(s, "leaping_sturgeon", "Leaping sturgeon", "fish_raw", 0x9A8A7A, 40);
        simple(s, "sacred_eel", "Sacred eel", "fish_raw", 0x3A8A5A, 100);
        simple(s, "infernal_eel", "Infernal eel", "fish_raw", 0xE04010, 100);
        simple(s, "casket", "Casket", "casket", 0x8A6A3A, 50);
        simple(s, "oyster", "Oyster", "secondary", 0x9A9A8A, 5);
        simple(s, "empty_oyster", "Empty oyster", "secondary", 0x9A9A8A, 1);
        simple(s, "oyster_pearl", "Oyster pearl", "gem", 0xF0F0F0, 112);
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        out.accept(r("fletch_split_log", MenuType.FLETCHING, "Split log (arctic pine)", Skill.FLETCHING, 1, 0)
                .in("arctic_pine_logs", 1).out("split_log", 1).tool("knife").ticks(2).build());
        out.accept(plain("shop_general_big_fishing_net", MenuType.SHOP_GENERAL, "Big fishing net - 20 gp").in("coins", 20)
                .out("big_fishing_net", 1).ticks(0).build());
        out.accept(plain("shop_general_karambwan_vessel", MenuType.SHOP_GENERAL, "Karambwan vessel - 5 gp").in("coins", 5)
                .out("karambwan_vessel", 1).ticks(0).build());
        out.accept(plain("shop_general_barbarian_rod", MenuType.SHOP_GENERAL, "Barbarian rod - 5 gp").in("coins", 5)
                .out("barbarian_rod", 1).ticks(0).build());
        out.accept(plain("shop_general_oily_fishing_rod", MenuType.SHOP_GENERAL, "Oily fishing rod - 15 gp").in("coins", 15)
                .out("oily_fishing_rod", 1).ticks(0).build());
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (ModTree t : TREES) {
            list.add(BlockDef.scripted(t.blockId(), t.name(), t.color(), t.blockId().equals("sulliuscep") ? "mushroom" : "tree"));
        }
        for (Spot s : SPOTS) {
            list.add(BlockDef.scripted(s.blockId(), s.name(), 0x3A6AB0, "fishing_spot"));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("castle_wars_achey", "Achey trees", 2504, 3060, PlaceDef.Style.CAMP, List.of("achey_tree", "achey_tree")),
                PlaceDef.of("neitiznot_arctic_pines", "Neitiznot arctic pines", 2330, 3820, PlaceDef.Style.CAMP,
                        List.of("arctic_pine_tree", "arctic_pine_tree", "arctic_pine_tree")),
                PlaceDef.of("fossil_island_sulliuscep", "Sulliuscep grove", 3678, 3733, PlaceDef.Style.CAMP,
                        List.of("sulliuscep", "sulliuscep")),
                PlaceDef.of("otto_grotto", "Otto's Grotto", 2500, 3497, PlaceDef.Style.CAMP,
                        List.of("barbarian_fishing_spot", "barbarian_fishing_spot")),
                PlaceDef.of("fishing_guild_big_net", "Fishing Guild pier", 2604, 3420, PlaceDef.Style.TOWN,
                        List.of("big_net_fishing_spot", "big_net_fishing_spot")),
                PlaceDef.of("karamja_river_karambwanji", "Karambwanji spot", 2791, 3019, PlaceDef.Style.CAMP,
                        List.of("karambwanji_fishing_spot")),
                PlaceDef.of("karambwan_spot", "Karambwan fishing spot", 2899, 3119, PlaceDef.Style.CAMP, List.of("karambwan_fishing_spot")),
                PlaceDef.of("lumbridge_swamp_caves", "Lumbridge Swamp Caves", 3243, 3160, PlaceDef.Style.CAVE,
                        List.of("eel_fishing_spot", "eel_fishing_spot")),
                PlaceDef.of("taverley_dungeon_lava_eels", "Lava eel fishing", 2895, 3480, PlaceDef.Style.CAVE, List.of("lava_eel_fishing_spot")),
                PlaceDef.of("zul_andra_shore", "Zul-Andra", 2192, 3058, PlaceDef.Style.CAMP, List.of("sacred_eel_fishing_spot", "sacred_eel_fishing_spot")),
                PlaceDef.of("mor_ul_rek_eels", "Infernal eel fishing", 2858, 3190, PlaceDef.Style.CAVE,
                        List.of("infernal_eel_fishing_spot", "infernal_eel_fishing_spot")),
                PlaceDef.of("fishing_platform_deep_sea", "Deep sea fishing", 2770, 3290, PlaceDef.Style.NONE, List.of()));
    }
}
