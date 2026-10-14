package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef.Kind;
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
        /** Success low value for an axe multiplier: the wiki's per-axe value where known, else the bronze value scaled. */
        public int low(double axeMultiplier) {
            int[] e = exact(axeMultiplier);
            return e != null ? e[0] : (int) Math.floor(low * axeMultiplier);
        }

        /** Success high value for an axe multiplier (see {@link #low(double)}). */
        public int high(double axeMultiplier) {
            int[] e = exact(axeMultiplier);
            return e != null ? e[1] : (int) Math.floor(high * axeMultiplier);
        }

        private int[] exact(double axeMultiplier) {
            int[][] t = AXE_VALUES.get(blockId);
            int col = com.runecraft.osrs.data.Tree.axeColumn(axeMultiplier);
            return t == null || col < 0 || col >= t.length ? null : t[col];
        }
    }

    /**
     * Per-axe low/high values (columns as {@link com.runecraft.osrs.data.Tree#AXE_MULTIPLIERS}: bronze ... rune, dragon,
     * crystal) from the OSRS Wiki's skilling success charts. The hollow tree has no crystal-axe line.
     */
    static final java.util.Map<String, int[][]> AXE_VALUES = java.util.Map.of(
            "achey_tree", new int[][]{{64, 200}, {96, 300}, {128, 400}, {144, 450}, {160, 500}, {192, 600}, {224, 700}, {240, 750},
                    {250, 800}},
            "arctic_pine_tree", new int[][]{{8, 25}, {12, 37}, {16, 50}, {18, 56}, {20, 62}, {24, 75}, {28, 87}, {30, 93}, {32, 100}},
            "sulliuscep", new int[][]{{11, 55}, {17, 85}, {23, 115}, {26, 131}, {29, 147}, {35, 177}, {41, 208}, {45, 228}, {48, 240}},
            "hollow_tree", new int[][]{{18, 26}, {28, 40}, {36, 54}, {42, 57}, {46, 68}, {59, 81}, {64, 94}, {67, 101}},
            // Camphor black axe: the wiki chart says 17/71 (the adamant high); 52 lies between steel and mithril (approx).
            "camphor_tree", new int[][]{{8, 23}, {11, 36}, {15, 47}, {17, 52}, {19, 59}, {23, 71}, {27, 83}, {32, 89}, {34, 93}},
            "ironwood_tree", new int[][]{{6, 21}, {10, 38}, {13, 42}, {15, 46}, {17, 53}, {21, 63}, {24, 75}, {29, 80}, {30, 83}},
            "rosewood_tree", new int[][]{{6, 18}, {9, 28}, {12, 37}, {13, 40}, {15, 47}, {18, 56}, {21, 66}, {25, 70}, {27, 73}});

    public static final List<ModTree> TREES = List.of(
            new ModTree("achey_tree", "Achey tree", 1, 25.0, "achey_tree_logs", 64, 200, 1.0, 60, 1, 40.0, 0x8A5A3A),
            new ModTree("arctic_pine_tree", "Arctic pine", 54, 40.0, "arctic_pine_logs", 8, 25, 0.125, 34, 42, 125.0, 0x4A5A3A),
            new ModTree("sulliuscep", "Sulliuscep", 65, 127.0, "sulliuscep_cap", 11, 55, 0.25, 50, 0, 0, 0xB08A5A),
            // Hollow tree (Morytania): bark at 45 Woodcutting for 82.5 XP.
            new ModTree("hollow_tree", "Hollow tree", 45, 82.5, "bark", 18, 26, 0.125, 45, 0, 0, 0x5A4A3A),
            // Sailing-era lumber trees (OSRS Wiki): camphor 66 Woodcutting / 143.5 XP (the Great Conch), ironwood 80 / 175
            // (Sunbleak Island), rosewood 92 / 212.5 (Drumstick Isle). Depletion and respawn approx (Forestry-style trees).
            new ModTree("camphor_tree", "Camphor tree", 66, 143.5, "camphor_logs", 8, 23, 0.125, 60, 0, 0, 0x8A6A4A),
            new ModTree("ironwood_tree", "Ironwood tree", 80, 175.0, "ironwood_logs", 6, 21, 0.1, 100, 0, 0, 0x5A4A40),
            new ModTree("rosewood_tree", "Rosewood tree", 92, 212.5, "rosewood_logs", 6, 18, 0.1, 100, 0, 0, 0x9A4A4A));

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

        /** Catch chance with a tool multiplier scaling low/high (harpoons; see {@link com.runecraft.osrs.data.Fish#chance}). */
        public double chance(int fishing, double toolMultiplier) {
            return com.runecraft.osrs.data.Fish.catchChance(fishing, low, high, toolMultiplier);
        }
    }

    /** A fishing spot added by this module. {@code bait} may be null. */
    public record Spot(String blockId, String name, String tool, String bait, List<Catch> catches, List<Loot> junk, int junkChance) {
    }

    public static final List<Spot> SPOTS = List.of(
            new Spot("big_net_fishing_spot", "Fishing spot (big net)", "big_fishing_net", null, List.of(
                    Catch.of("raw_bass", 46, 100.0, 3, 40), Catch.of("raw_cod", 23, 45.0, 4, 55), Catch.of("raw_mackerel", 16, 20.0, 5, 65)),
                    List.of(Loot.of("seaweed", 1, 2, 60), Loot.of("oyster", 15), Loot.of("casket", 5), Loot.of("leather_boots", 10),
                            Loot.of("leather_gloves", 10)), 10),
            new Spot("barbarian_fishing_spot", "Fishing spot (barbarian)", "barbarian_rod", "fishing_bait", List.of(
                    new Catch("leaping_sturgeon", 70, 80.0, 8, 64, 7.0, 7.0, 45, 45),
                    new Catch("leaping_salmon", 58, 70.0, 16, 96, 6.0, 6.0, 30, 30),
                    new Catch("leaping_trout", 48, 50.0, 32, 192, 5.0, 5.0, 15, 15)), List.of(), 0),
            new Spot("karambwanji_fishing_spot", "Fishing spot (karambwanji)", "small_fishing_net", null, List.of(
                    Catch.of("raw_karambwanji", 5, 5.0, 100, 250)), List.of(), 0),
            new Spot("karambwan_fishing_spot", "Fishing spot (karambwan)", "karambwan_vessel", "raw_karambwanji", List.of(
                    Catch.of("raw_karambwan", 65, 50.0, 5, 160)), List.of(), 0),
            new Spot("eel_fishing_spot", "Fishing spot (cave eels)", "fishing_rod", "fishing_bait", List.of(
                    Catch.of("raw_cave_eel", 38, 80.0, 10, 80), Catch.of("raw_slimy_eel", 28, 65.0, 10, 80)), List.of(), 0),
            new Spot("lava_eel_fishing_spot", "Fishing spot (lava eels)", "oily_fishing_rod", "fishing_bait", List.of(
                    Catch.of("raw_lava_eel", 53, 60.0, 16, 96)), List.of(), 0),
            new Spot("sacred_eel_fishing_spot", "Fishing spot (sacred eels)", "fishing_rod", "fishing_bait", List.of(
                    Catch.of("sacred_eel", 87, 105.0, 0, 60)), List.of(), 0),
            new Spot("infernal_eel_fishing_spot", "Fishing spot (infernal eels)", "oily_fishing_rod", "fishing_bait", List.of(
                    Catch.of("infernal_eel", 80, 95.0, 30, 93)), List.of(), 0),
            new Spot("deep_sea_fishing_spot", "Fishing spot (deep sea)", "harpoon", null, List.of(
                    Catch.of("raw_manta_ray", 81, 46.0, 4, 30), Catch.of("raw_sea_turtle", 79, 38.0, 6, 40),
                    Catch.of("raw_shark", 76, 110.0, 3, 40)), List.of(Loot.of("casket", 1), Loot.of("seaweed", 4)), 4),
            // Minnows (Kylie Minnow's platform, Fishing Guild): small net, 82 Fishing, 26.1 XP per catch of 10-14 minnows
            // (see minnowsPerCatch). The wiki has no success chart: low/high approx.
            new Spot("minnow_fishing_spot", "Fishing spot (minnow)", "small_fishing_net", null, List.of(
                    Catch.of("minnow", 82, 26.1, 150, 256)), List.of(), 0));

    public static final String MINNOW = "minnow";
    public static final String MINNOW_SPOT = "minnow_fishing_spot";
    public static final String KYLIE_MINNOW = "kylie_minnow";
    /** Kylie Minnow trades one raw shark for this many minnows. */
    public static final int MINNOWS_PER_SHARK = 40;

    /**
     * Minnows in one catch at a Fishing level (OSRS Wiki: 10 at 82, 11 at 85, 12 at 90, 13 at 95, 14 at 99; the levels
     * in between are approx).
     */
    public static int minnowsPerCatch(int fishing) {
        if (fishing >= 99) {
            return 14;
        }
        return fishing >= 95 ? 13 : fishing >= 90 ? 12 : fishing >= 85 ? 11 : 10;
    }

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
        simple(s, "bark", "Bark", "bark", 0x5A4A3A, 1).examine("Bark from a hollow tree.");
        simple(s, "split_log", "Split log", "log", 0x4A5A3A, 15);
        // Neitiznot shield (the Fremennik round shield; OSRS Wiki stats): made from arctic pine logs on Neitiznot.
        s.add("neitiznot_shield", "Neitiznot shield", Kind.SHIELD, "round_shield", 0x6A5A3A).value(500).stack(1).weight(3.628)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.SHIELD,
                        new com.runecraft.osrs.EquipmentStats(0, 0, 0, -10, -10, 17, 17, 31, -4, 33, 0, 0, 0, 0), 25))
                .examine("A sturdy Fremennik round shield.");
        tool(s, "big_fishing_net", "Big fishing net", "net", 0xB0A080, 20);
        tool(s, "barbarian_rod", "Barbarian rod", "rod", 0x7A5A3A, 5);
        tool(s, "karambwan_vessel", "Karambwan vessel", "pot", 0xA08050, 5);
        tool(s, "oily_fishing_rod", "Oily fishing rod", "rod", 0x4A4A2A, 15);
        simple(s, "raw_karambwanji", "Raw karambwanji", "fish_raw", 0x8AAA5A, 2);
        simple(s, MINNOW, "Minnow", "fish_raw", 0x9AAAB0, 1).stack(Integer.MAX_VALUE)
                .examine("A tiny fish. Kylie Minnow swaps forty of them for a raw shark.");
        simple(s, "leaping_trout", "Leaping trout", "fish_raw", 0xC09070, 10);
        simple(s, "leaping_salmon", "Leaping salmon", "fish_raw", 0xE07050, 20);
        simple(s, "leaping_sturgeon", "Leaping sturgeon", "fish_raw", 0x9A8A7A, 40);
        simple(s, "sacred_eel", "Sacred eel", "fish_raw", 0x3A8A5A, 100);
        simple(s, "infernal_eel", "Infernal eel", "fish_raw", 0xE04010, 100);
        simple(s, "casket", "Casket", "casket", 0x8A6A3A, 50);
        simple(s, "oyster", "Oyster", "secondary", 0x9A9A8A, 5);
        simple(s, "empty_oyster", "Empty oyster", "secondary", 0x9A9A8A, 1);
        simple(s, "oyster_pearl", "Oyster pearl", "gem", 0xF0F0F0, 112);
        // Infernal and crystal tools (bonuses: com.runecraft.osrs.data.SpecialTool). They count as dragon tier.
        s.add("infernal_axe", "Infernal axe", Kind.AXE, "axe", 0xE04A10).value(56000).stack(1).tier("dragon")
                .equipment(weapon(stats(-2, 38, 32, 0, 0, 0, 1, 0, 0, 0, 42, 0, 0, 0), 5, AttackType.SLASH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 60))
                .examine("A very powerful axe; it burns some of the logs it cuts.");
        s.add("crystal_axe", "Crystal axe", Kind.AXE, "axe", 0x9AD8F0).value(56000).stack(1).tier("dragon")
                .equipment(weapon(stats(-2, 38, 32, 0, 0, 0, 1, 0, 0, 0, 42, 0, 0, 0), 5, AttackType.SLASH,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70))
                .examine("A crystal axe, lighter and sharper than any metal.");
        s.add("infernal_pickaxe", "Infernal pickaxe", Kind.PICKAXE, "pickaxe", 0xE04A10).value(70000).stack(1).tier("dragon")
                .equipment(weapon(stats(38, -2, 32, 0, 0, 0, 0, 0, 0, 0, 42, 0, 0, 0), 5, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 60))
                .examine("A very powerful pickaxe; it smelts some of the ore it mines.");
        s.add("crystal_pickaxe", "Crystal pickaxe", Kind.PICKAXE, "pickaxe", 0x9AD8F0).value(70000).stack(1).tier("dragon")
                .equipment(weapon(stats(38, -2, 32, 0, 0, 0, 0, 0, 0, 0, 42, 0, 0, 0), 5, AttackType.STAB,
                        EquipmentDef.WeaponClass.MELEE, Skill.ATTACK, 70))
                .examine("A crystal pickaxe that mines faster than any metal.");
        s.add("infernal_harpoon", "Infernal harpoon", Kind.TOOL, "harpoon", 0xE04A10).value(100000).stack(1).tier("dragon")
                .examine("A very powerful harpoon; it cooks some of the fish it catches.");
        s.add("crystal_harpoon", "Crystal harpoon", Kind.TOOL, "harpoon", 0x9AD8F0).value(100000).stack(1).tier("dragon")
                .examine("A crystal harpoon that catches fish faster than any other.");
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        out.accept(r("fletch_split_log", MenuType.FLETCHING, "Split log (arctic pine)", Skill.FLETCHING, 1, 0)
                .in("arctic_pine_logs", 1).out("split_log", 1).tool("knife").ticks(2).build());
        // Neitiznot shield (OSRS Wiki): 2 arctic pine logs, a rope and a bronze nail with a hammer at the Neitiznot woodcutting
        // stump: 56 Woodcutting, 34 Crafting XP. RuneCraft has no stump, so it is in the fletching menu, and the recipe can only
        // name one skill, so the 56 Woodcutting is checked by the diary task, not here. // approx
        out.accept(r("make_neitiznot_shield", MenuType.FLETCHING, "Neitiznot shield", Skill.CRAFTING, 1, 34)
                .in("arctic_pine_logs", 2).in("rope", 1).in("bronze_nails", 1).out("neitiznot_shield", 1).tool("hammer").ticks(3).build());
        out.accept(plain("shop_general_big_fishing_net", MenuType.SHOP_GENERAL, "Big fishing net - 20 gp").in("coins", 20)
                .out("big_fishing_net", 1).ticks(0).build());
        out.accept(plain("shop_general_karambwan_vessel", MenuType.SHOP_GENERAL, "Karambwan vessel - 5 gp").in("coins", 5)
                .out("karambwan_vessel", 1).ticks(0).build());
        out.accept(plain("shop_general_barbarian_rod", MenuType.SHOP_GENERAL, "Barbarian rod - 5 gp").in("coins", 5)
                .out("barbarian_rod", 1).ticks(0).build());
        // Infernal tools: a smouldering stone fused onto the dragon tool (OSRS: 85 Firemaking/Smithing/Cooking, 350 XP).
        out.accept(r("make_infernal_axe", MenuType.SMITHING, "Infernal axe", Skill.FIREMAKING, 85, 350.0)
                .in("dragon_axe", 1).in("smouldering_stone", 1).out("infernal_axe", 1).ticks(3).build());
        out.accept(r("make_infernal_pickaxe", MenuType.SMITHING, "Infernal pickaxe", Skill.SMITHING, 85, 350.0)
                .in("dragon_pickaxe", 1).in("smouldering_stone", 1).out("infernal_pickaxe", 1).ticks(3).build());
        out.accept(r("make_infernal_harpoon", MenuType.SMITHING, "Infernal harpoon", Skill.COOKING, 85, 350.0)
                .in("dragon_harpoon", 1).in("smouldering_stone", 1).out("infernal_harpoon", 1).ticks(3).build());
        // Crystal tools: a crystal tool seed and 120 crystal shards sung into shape (OSRS: 76 Smithing and Crafting).
        for (String tool : new String[]{"crystal_axe", "crystal_pickaxe", "crystal_harpoon"}) {
            String dragon = tool.equals("crystal_axe") ? "dragon_axe" : tool.equals("crystal_pickaxe") ? "dragon_pickaxe" : "dragon_harpoon";
            out.accept(r("make_" + tool, MenuType.SMITHING, title(tool), Skill.SMITHING, 76, 0)
                    .in(dragon, 1).in("crystal_tool_seed", 1).in("crystal_shard", 120).out(tool, 1).ticks(3).build());
        }
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
                // Camphor trees west of the Summer Shore on the Great Conch (approx tile). The ironwood trees of Sunbleak Island
                // and the rosewood trees of Drumstick Isle stand on their own tiles (ResourceSites).
                PlaceDef.of("great_conch_camphor_trees", "Camphor trees", 3160, 2425, PlaceDef.Style.CAMP,
                        List.of("camphor_tree", "camphor_tree", "camphor_tree")),
                // Otto's Grotto also has the anvil where barbarian spears and hastae are smithed.
                new PlaceDef("otto_grotto", "Otto's Grotto", 2500, 3497, PlaceDef.Style.CAMP, List.of(com.runecraft.osrs.Station.ANVIL),
                        List.of("barbarian_fishing_spot", "barbarian_fishing_spot")),
                // Hollow trees (bark): east of Canifis in the Haunted Woods and south of Port Phasmatys (wiki object tiles).
                PlaceDef.of("haunted_woods_hollow_trees", "Hollow trees", 3553, 3500, PlaceDef.Style.CAMP,
                        List.of("hollow_tree", "hollow_tree", "hollow_tree")),
                PlaceDef.of("phasmatys_hollow_trees", "Hollow trees", 3620, 3430, PlaceDef.Style.CAMP,
                        List.of("hollow_tree", "hollow_tree", "hollow_tree")),
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
                PlaceDef.of("fishing_platform_deep_sea", "Deep sea fishing", 2770, 3290, PlaceDef.Style.NONE, List.of()),
                // Kylie Minnow on her platform at the Fishing Guild (cache tile 2614, 3446).
                PlaceDef.of("kylie_minnow_platform", "Kylie Minnow's platform", 2614, 3446, PlaceDef.Style.NONE, List.of()));
    }

    @Override
    public List<com.runecraft.osrs.content.NpcDef> npcs() {
        return List.of(com.runecraft.osrs.content.NpcDef.of(KYLIE_MINNOW, "Kylie Minnow", "kylie_minnow_platform", 0, 0,
                0xE0B890, 0x3A6A8A, 0x4A5A3A, 0x3A6A8A));
    }
}
