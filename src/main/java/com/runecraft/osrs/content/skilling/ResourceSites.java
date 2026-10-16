package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Region;

import java.util.List;

/**
 * Gathering resources at their OSRS map positions that no hand-built structure places: fishing spots,
 * trees and rocks. The Minecraft side ({@code ResourceSitePlacer}) puts each one on the real surface the
 * first time its chunk loads (rocks and trees on open ground, fishing spots on the nearest shoreline
 * water), after the chunk's structures, and skips a tile whose resource a structure already placed nearby.
 *
 * <p>Fishing spot tiles come from a world scan of OSRS fishing spot NPCs (mejrs/data_osrs
 * NPCList_OSRS.json); fishing spots wander, so these are where they were seen. Tree and rock tiles are
 * from the OSRS map; ones only known to within a few tiles are marked approx. Sites already built by
 * structures (Draynor willows and net spots, Edgeville yews, Barbarian Village and Lumbridge rod spots,
 * Seers' yews, the Fishing Guild big net spots, Rimmington, Crafting Guild, Varrock east and Mining Guild
 * mines, the Wilderness Resource Area, the Woodcutting Guild's redwoods) are not repeated here.
 */
public final class ResourceSites {
    private ResourceSites() {
    }

    public enum Kind { TREE, ROCK, FISHING }

    /** A cluster of one resource: block id (without namespace), the kingdom it is in, OSRS tiles {x, y}. */
    public record Site(String id, String name, Kind kind, String block, Region region, List<int[]> tiles) {
    }

    private static Site fish(String id, String name, String block, Region region, int... xy) {
        return new Site(id, name, Kind.FISHING, block, region, tiles(xy));
    }

    private static Site tree(String id, String name, String tree, Region region, int... xy) {
        return new Site(id, name, Kind.TREE, tree + "_tree_log", region, tiles(xy));
    }

    /** Trees whose block is one of {@link GatheringData#TREES} (the block id itself, not a {@code *_tree_log}). */
    private static Site modTree(String id, String name, String block, Region region, int... xy) {
        return new Site(id, name, Kind.TREE, block, region, tiles(xy));
    }

    private static Site rock(String id, String name, String rock, Region region, int... xy) {
        return new Site(id, name, Kind.ROCK, rock + "_rock", region, tiles(xy));
    }

    private static List<int[]> tiles(int... xy) {
        if (xy.length == 0 || xy.length % 2 != 0) {
            throw new IllegalArgumentException("tiles need x,y pairs");
        }
        int[][] out = new int[xy.length / 2][];
        for (int i = 0; i < out.length; i++) {
            out[i] = new int[]{xy[2 * i], xy[2 * i + 1]};
        }
        return List.of(out);
    }

    private static final String NET_BAIT = "net_bait_fishing_spot";
    private static final String LURE_BAIT = "lure_bait_fishing_spot";
    private static final String CAGE_HARPOON = "cage_harpoon_fishing_spot";
    private static final String NET_HARPOON = "net_harpoon_fishing_spot";
    private static final String SHARK = "shark_fishing_spot";
    private static final String BIG_NET = "big_net_fishing_spot";

    public static final List<Site> SITES = List.of(
            // ---------------------------------------------------------------- fishing
            // The Lava Maze's lava eel spot (cache tile 3070, 3839), placed on the lava channel next to it.
            fish("lava_maze_lava_eels", "Lava Maze (lava eels)", "lava_eel_fishing_spot", Region.WILDERNESS, 3070, 3839),
            fish("catherby_net_bait", "Catherby shore (net/bait)", NET_BAIT, Region.KANDARIN, 2836, 3431, 2838, 3431),
            fish("catherby_cage_harpoon", "Catherby shore (cage/harpoon)", CAGE_HARPOON, Region.KANDARIN, 2844, 3429, 2853, 3423),
            fish("catherby_shark", "Catherby shore (big net/harpoon)", SHARK, Region.KANDARIN, 2846, 3429, 2855, 3423, 2859, 3426),
            fish("catherby_big_net", "Catherby shore (big net)", BIG_NET, Region.KANDARIN, 2850, 3426),
            fish("fishing_guild_cage_harpoon", "Fishing Guild (cage/harpoon)", CAGE_HARPOON, Region.KANDARIN,
                    2599, 3419, 2603, 3419, 2604, 3417, 2602, 3426),
            fish("fishing_guild_shark", "Fishing Guild (big net/harpoon)", SHARK, Region.KANDARIN, 2602, 3412, 2605, 3420, 2601, 3422),
            // Kylie Minnow's platform: the guild's small-net spots are the minnow spots (cache tiles of NPCs 7730-7733).
            fish("fishing_guild_small_net", "Fishing Guild (minnows)", GatheringData.MINNOW_SPOT, Region.KANDARIN,
                    2609, 3443, 2612, 3444, 2617, 3444, 2620, 3443),
            // Lake Molch's aerial fishing spots around Molch Island (cache tiles of NPC 8523).
            fish("lake_molch_aerial", "Lake Molch (aerial fishing)", AerialFishing.SPOT, Region.KOUREND,
                    1355, 3621, 1355, 3627, 1357, 3630, 1355, 3634, 1355, 3638, 1359, 3619, 1362, 3624, 1381, 3631),
            fish("musa_point_net_bait", "Musa Point (net/bait)", NET_BAIT, Region.KARAMJA, 2924, 3181, 2926, 3176),
            fish("musa_point_cage_harpoon", "Musa Point (cage/harpoon)", CAGE_HARPOON, Region.KARAMJA, 2923, 3180, 2926, 3179),
            fish("shilo_village_river", "Shilo Village river (lure/bait)", LURE_BAIT, Region.KARAMJA, 2855, 2974, 2855, 2977, 2856, 2971),
            fish("piscatoris_monkfish", "Piscatoris fishing colony (net/harpoon)", NET_HARPOON, Region.KANDARIN,
                    2310, 3697, 2326, 3700, 2343, 3702, 2349, 3702),
            fish("al_kharid_net_bait", "Al Kharid shore (net/bait)", NET_BAIT, Region.KHARIDIAN_DESERT, 3276, 3140, 3272, 3145),
            fish("lumbridge_swamp_net_bait", "Lumbridge Swamp shore (net/bait)", NET_BAIT, Region.LUMBRIDGE_SWAMP, 3240, 3146, 3245, 3152),
            fish("port_sarim_net_bait", "Port Sarim shore (net/bait)", NET_BAIT, Region.ASGARNIA, 2986, 3176, 2996, 3158),
            fish("rellekka_net_bait", "Rellekka pier (net/bait)", NET_BAIT, Region.FREMENNIK_PROVINCE, 2633, 3690, 2633, 3693),
            fish("rellekka_cage_harpoon", "Rellekka pier (cage/harpoon)", CAGE_HARPOON, Region.FREMENNIK_PROVINCE, 2639, 3694, 2642, 3699),
            fish("rellekka_shark", "Rellekka pier (big net/harpoon)", SHARK, Region.FREMENNIK_PROVINCE, 2645, 3708, 2648, 3711),
            fish("burgh_de_rott_shark", "Burgh de Rott coast (big net/harpoon)", SHARK, Region.MORYTANIA, 3472, 3196, 3476, 3191),
            fish("wilderness_dark_crabs", "Wilderness Resource Area dark crabs", "dark_crab_fishing_spot", Region.WILDERNESS,
                    3181, 3926, 3185, 3926, 3350, 3817),
            fish("piscarilius_anglerfish", "Port Piscarilius anglerfish", "angler_fishing_spot", Region.KOUREND,
                    1826, 3770, 1833, 3769),
            // ---------------------------------------------------------------- trees
            tree("varrock_palace_yews", "Varrock Palace yews", "yew", Region.MISTHALIN, 3205, 3503, 3210, 3503, 3220, 3502),   // approx
            tree("falador_yews", "South of Falador yews", "yew", Region.ASGARNIA, 3040, 3320, 3044, 3315, 3049, 3321, 3052, 3316), // approx
            tree("catherby_yews", "Catherby beach yews", "yew", Region.KANDARIN, 2756, 3431, 2760, 3428, 2766, 3428),   // approx
            tree("catherby_willows", "Catherby willows", "willow", Region.KANDARIN, 2782, 3430, 2786, 3427),   // approx
            tree("seers_maples", "Seers' Village maples", "maple", Region.KANDARIN, 2722, 3501, 2727, 3501, 2731, 3500, 2733, 3502),
            tree("sorcerers_tower_magics", "Sorcerer's Tower magic trees", "magic", Region.KANDARIN, 2701, 3397, 2704, 3397, 2705, 3399),
            tree("gnome_stronghold_magic", "Tree Gnome Stronghold magic tree", "magic", Region.KANDARIN, 2434, 3413),   // approx
            tree("tree_gnome_village_magic", "Tree Gnome Village magic tree", "magic", Region.KANDARIN, 2509, 3166),   // approx
            tree("hardwood_grove_teaks", "Tai Bwo Wannai hardwood grove", "teak", Region.KARAMJA, 2820, 3078, 2823, 3083),   // approx
            tree("hardwood_grove_mahogany", "Tai Bwo Wannai hardwood grove", "mahogany", Region.KARAMJA, 2818, 3084, 2826, 3080), // approx
            // ---------------------------------------------------------------- rocks
            rock("varrock_west_tin", "Varrock west mine", "tin", Region.MISTHALIN, 3177, 3367, 3182, 3367),
            rock("varrock_west_clay", "Varrock west mine", "clay", Region.MISTHALIN, 3179, 3371, 3180, 3372),
            rock("varrock_west_iron", "Varrock west mine", "iron", Region.MISTHALIN, 3175, 3366, 3175, 3368),
            rock("varrock_west_silver", "Varrock west mine", "silver", Region.MISTHALIN, 3176, 3370, 3177, 3369),
            rock("lumbridge_swamp_east_copper", "Lumbridge Swamp east mine", "copper", Region.LUMBRIDGE_SWAMP, 3226, 3146, 3228, 3146, 3229, 3145),
            rock("lumbridge_swamp_east_tin", "Lumbridge Swamp east mine", "tin", Region.LUMBRIDGE_SWAMP, 3223, 3147, 3225, 3148, 3226, 3149),
            rock("lumbridge_swamp_west_coal", "Lumbridge Swamp west mine", "coal", Region.LUMBRIDGE_SWAMP, 3144, 3150, 3145, 3151, 3146, 3149),
            rock("lumbridge_swamp_west_mithril", "Lumbridge Swamp west mine", "mithril", Region.LUMBRIDGE_SWAMP, 3146, 3146, 3147, 3147),
            rock("lumbridge_swamp_west_adamantite", "Lumbridge Swamp west mine", "adamantite", Region.LUMBRIDGE_SWAMP, 3147, 3148),
            rock("al_kharid_iron", "Al Kharid mine", "iron", Region.KHARIDIAN_DESERT, 3295, 3310, 3296, 3311, 3301, 3307),
            rock("al_kharid_coal", "Al Kharid mine", "coal", Region.KHARIDIAN_DESERT, 3300, 3299, 3301, 3301),
            rock("al_kharid_silver", "Al Kharid mine", "silver", Region.KHARIDIAN_DESERT, 3295, 3301, 3297, 3312),
            rock("al_kharid_gold", "Al Kharid mine", "gold", Region.KHARIDIAN_DESERT, 3302, 3286, 3303, 3287),
            rock("al_kharid_mithril", "Al Kharid mine", "mithril", Region.KHARIDIAN_DESERT, 3303, 3304, 3304, 3305),
            rock("al_kharid_adamantite", "Al Kharid mine", "adamantite", Region.KHARIDIAN_DESERT, 3299, 3318, 3300, 3317),
            rock("al_kharid_copper_tin", "Al Kharid mine", "copper", Region.KHARIDIAN_DESERT, 3297, 3315, 3298, 3314),
            rock("barbarian_village_coal", "Barbarian Village mine", "coal", Region.MISTHALIN, 3081, 3421, 3082, 3422),
            rock("barbarian_village_tin", "Barbarian Village mine", "tin", Region.MISTHALIN, 3079, 3418, 3080, 3419),
            rock("seers_coal_trucks", "Coal trucks mine", "coal", Region.KANDARIN, 2581, 3481, 2584, 3483, 2586, 3478, 2589, 3481), // approx
            rock("lava_maze_runite", "Lava Maze runite rocks", "runite", Region.WILDERNESS, 3059, 3884, 3060, 3885),   // approx
            // Lead rocks on Deepfin Point and the Onyx Crest (wiki object tiles; the Pandemonium Cave's rocks are not built).
            rock("deepfin_point_lead", "Deepfin Point lead rocks", "lead", Region.UNCHARTED_ISLES,
                    1935, 2795, 1933, 2794, 1936, 2794, 1934, 2792, 1932, 2791, 1930, 2789, 1939, 2789),
            rock("onyx_crest_lead", "Onyx Crest lead rocks", "lead", Region.UNCHARTED_ISLES,
                    2975, 2255, 2975, 2253, 2977, 2253, 2974, 2252, 2975, 2251, 2978, 2251),
            // Nickel rocks on Deepfin Point, Ynysdail and Grimstone (wiki object tiles; the Deepfin Mine's are underground and not
            // built). The rubium rocks are in the Charred Dungeon (dungeons module, CharredDungeon).
            rock("deepfin_point_nickel", "Deepfin Point nickel rocks", "nickel", Region.UNCHARTED_ISLES,
                    1930, 2794, 1931, 2795, 1932, 2794, 1931, 2788, 1933, 2787, 1936, 2791, 1937, 2793, 1939, 2792, 1940, 2791),
            rock("ynysdail_nickel", "Ynysdail nickel rocks", "nickel", Region.UNCHARTED_ISLES, 2224, 3478, 2226, 3475, 2228, 3476),
            rock("grimstone_nickel", "Grimstone nickel rocks", "nickel", Region.UNCHARTED_ISLES,
                    2911, 4084, 2915, 4085, 2912, 4082, 2916, 4084, 2911, 4064, 2912, 4062, 2908, 4061, 2910, 4060),
            // The 14 ironwood trees of Sunbleak Island and the 10 rosewood trees of Drumstick Isle (wiki object tiles).
            modTree("sunbleak_island_ironwood", "Sunbleak Island ironwood trees", "ironwood_tree", Region.UNCHARTED_ISLES,
                    2200, 2328, 2231, 2334, 2228, 2340, 2227, 2330, 2223, 2347, 2223, 2323, 2221, 2344, 2221, 2319, 2211, 2320,
                    2210, 2317, 2207, 2327, 2207, 2313, 2204, 2325, 2203, 2316),
            modTree("drumstick_isle_rosewood", "Drumstick Isle rosewood trees", "rosewood_tree", Region.UNCHARTED_ISLES,
                    2135, 3548, 2154, 3543, 2153, 3540, 2153, 3537, 2150, 3555, 2150, 3538, 2149, 3558, 2148, 3556, 2137, 3547,
                    2137, 3544),
            // The Lovakengj sulphur mine's volcanic sulphur deposits (wiki object tiles).
            rock("lovakengj_sulphur", "Lovakengj sulphur mine", "volcanic_sulphur", Region.KOUREND,
                    1423, 3872, 1445, 3863, 1439, 3869, 1432, 3866, 1446, 3873));
}
