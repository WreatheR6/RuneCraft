package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Settlements of the Sailing-era islands traced in {@code SeaIslands}, laid out from the OSRS map squares: the
 * Pandemonium (the pub on the water, Henderson's fishing shop, the deposit box and the docks), the pirates' castle on
 * the east end of Buccaneers' Haven with the outbuildings to its west, the pirate houses on Laguna Aurorae and the
 * pirate camp on the Onyx Crest. Sunbleak Island and Drumstick Isle have no buildings (their trees and the tortoises,
 * rabbits and foxes are the only inhabitants); their ironwood and rosewood trees are skilling resource sites.
 */
final class IslandsSailing {
    private IslandsSailing() {
    }

    /** The quests module's Pandemonium place (its plaza is replaced by the pub and the docks). */
    static final String PANDEMONIUM_PLACE = "quest_the_pandemonium";

    static List<StructureDef> all() {
        return List.of(pandemonium(), buccaneersHaven(), lagunaAurorae(), onyxCrest());
    }

    private static final String SHACK = "minecraft:dark_oak_planks";

    /**
     * The Pandemonium: the pub (map 3038-3052, 2964-2986) stands on piles at the south-east corner of the island, the
     * fishing shop north of it, and the jetties run east to the charter berth and north past the shipwright. The
     * square between them (3049-3056) stays open: the charter dock (3050, 2972) and the Sailing port lie there.
     */
    static StructureDef pandemonium() {
        Builder b = StructureDef.builder("isl_the_pandemonium", "The Pandemonium").replaces(PANDEMONIUM_PLACE);
        b.tFlatten(3036, 2962, 3058, 2990, SAND);
        b.tFloor(3049, 2964, 3056, 2988, SPRUCE);
        // the pub: a dark wooden hall with the bar along its west wall ('Squawking' Steve Beanie behind it)
        building(b, 3038, 2965, 3047, 2977, 1, SHACK, SPRUCE, "minecraft:dark_oak", "east", true);
        b.tFill(3039, 2967, 3039, 2975, 0, 0, "minecraft:dark_oak_slab[type=top]");
        b.tSet(3039, 2971, 1, "minecraft:brewing_stand");
        b.tSet(3040, 2966, 0, "minecraft:barrel[facing=up]");
        b.tSet(3040, 2976, 0, "minecraft:barrel[facing=up]");
        // Henderson's Catch of the Day, just north of the pub
        building(b, 3039, 2980, 3046, 2986, 1, SHACK, SPRUCE, "minecraft:spruce", "east", true);
        b.tSet(3040, 2983, 0, "minecraft:barrel[facing=up]");
        b.tSet(3040, 2984, 0, "minecraft:barrel[facing=up]");
        // the bank deposit box by the jetty (OSRS: a crab bank chest and a deposit box)
        b.tSet(3052, 2969, 0, rc("bank_deposit_box"));
        // jetties: east to the charter berth, north past the shipwright and the salvaging station
        pier(b, 3057, 2969, 3066, 2973, SPRUCE);
        pier(b, 3059, 2974, 3062, 2995, SPRUCE);
        ship(b, 3063, 2978, 8, SHACK, "minecraft:white_wool");
        b.tSet(3055, 2986, 0, "minecraft:anvil[facing=north]");
        b.tSet(3056, 2986, 0, "minecraft:barrel[facing=up]");
        for (int[] p : new int[][]{{3049, 2964}, {3056, 2964}, {3049, 2988}, {3056, 2988}}) {
            lamp(b, p[0], p[1], "minecraft:dark_oak_fence");
        }
        for (int[] p : new int[][]{{3030, 2995}, {3024, 2985}, {3035, 3000}}) {
            palm(b, p[0], p[1], 6);
        }
        return b.build();
    }

    /**
     * Buccaneers' Haven: the pirates' castle on the east end (map 2090-2103, 3679-3690) with corner towers around a
     * courtyard, the tower on the hill to the west (a pirate keeps watch on its top floor, 2073, 3688) and the
     * outbuildings by the south shore.
     */
    static StructureDef buccaneersHaven() {
        Builder b = StructureDef.builder("isl_buccaneers_haven", "Buccaneers' Haven").replaces(IslandsPlaces.BUCCANEERS_HAVEN);
        b.tFlatten(2088, 3677, 2105, 3691, COARSE);
        b.tFloor(2091, 3680, 2102, 3689, COBBLE);
        wall(b, 2090, 3679, 2103, 3679, 6, STONE);
        wall(b, 2090, 3690, 2103, 3690, 6, STONE);
        wall(b, 2090, 3679, 2090, 3690, 6, STONE);
        wall(b, 2103, 3679, 2103, 3690, 6, STONE);
        // the gate in the west wall, facing the rest of the island
        b.tClear(2090, 3684, 2090, 3685, 0, 3);
        for (int[] t : new int[][]{{2090, 3679}, {2103, 3679}, {2090, 3690}, {2103, 3690}}) {
            b.tTower(t[0], t[1], 2, 9, STONE, SPRUCE);
        }
        // the hall along the courtyard's east side, with the pirates' plunder
        building(b, 2098, 3681, 2102, 3688, 2, STONE, SPRUCE, "minecraft:dark_oak", "west", false);
        b.tSet(2095, 3688, 0, "minecraft:chest[facing=south]");
        b.tSet(2093, 3681, 0, "minecraft:barrel[facing=up]");
        b.tSet(2094, 3681, 0, "minecraft:barrel[facing=up]");
        lamp(b, 2092, 3683, "minecraft:dark_oak_fence");
        // the lookout tower on the hill and the outbuildings
        b.tFlatten(2070, 3683, 2079, 3692, COARSE);
        b.tTower(2073, 3688, 3, 13, STONE, SPRUCE);
        b.tFlatten(2056, 3680, 2063, 3688, COARSE);
        building(b, 2057, 3681, 2062, 3687, 1, SHACK, SPRUCE, "minecraft:dark_oak", "east", true);
        b.tFlatten(2076, 3662, 2097, 3669, SAND);
        building(b, 2077, 3664, 2082, 3667, 1, SHACK, SPRUCE, "minecraft:dark_oak", "north", true);
        building(b, 2090, 3664, 2096, 3668, 1, SHACK, SPRUCE, "minecraft:dark_oak", "north", true);
        return b.build();
    }

    /** Laguna Aurorae: the pirates' houses in the north-west of the island (map 1154-1172, 2774-2789). */
    static StructureDef lagunaAurorae() {
        Builder b = StructureDef.builder("isl_laguna_aurorae", "Laguna Aurorae").replaces(IslandsPlaces.LAGUNA_AURORAE);
        b.tFlatten(1152, 2772, 1174, 2791, GRASS);
        b.tFloor(1158, 2780, 1170, 2781, PATH);
        // the western house, its door to the north (the small key is dug up 4 tiles east of it)
        building(b, 1162, 2774, 1169, 2779, 1, JUNGLE, OAK, "minecraft:jungle", "north", true);
        hut(b, 1163, 2787, 2, BAMBOO, "minecraft:green_wool", OAK, "south");
        hut(b, 1156, 2783, 2, BAMBOO, "minecraft:green_wool", OAK, "east");
        hut(b, 1170, 2785, 2, BAMBOO, "minecraft:green_wool", OAK, "west");
        lamp(b, 1166, 2782, "minecraft:jungle_fence");
        for (int[] p : new int[][]{{1153, 2775}, {1173, 2790}, {1174, 2774}}) {
            palm(b, p[0], p[1], 6);
        }
        return b.build();
    }

    /** The Onyx Crest: the pirates' tents north-east of the volcano (map 2976-2989, 2280-2293). */
    static StructureDef onyxCrest() {
        Builder b = StructureDef.builder("isl_the_onyx_crest", "The Onyx Crest").replaces(IslandsPlaces.ONYX_CREST);
        b.tFlatten(2975, 2279, 2991, 2294, COARSE);
        tent(b, 2977, 2289, 2981, 2292, "minecraft:red_wool");
        tent(b, 2984, 2287, 2988, 2290, "minecraft:black_wool");
        tent(b, 2977, 2281, 2981, 2284, "minecraft:red_wool");
        b.tSet(2983, 2284, -1, "minecraft:campfire");
        b.tSet(2986, 2283, 0, "minecraft:barrel[facing=up]");
        b.tSet(2987, 2283, 0, "minecraft:chest[facing=west]");
        return b.build();
    }
}
