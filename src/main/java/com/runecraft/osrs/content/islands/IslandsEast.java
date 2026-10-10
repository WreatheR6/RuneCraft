package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Mos Le'Harmless (the pirate town and its dock; its caves are the dungeons module's), Harmony Island's
 * monastery, Dragontooth Island, Tutorial Island, Mudskipper Point with fairy ring AIQ, and Zanaris
 * with the fairy ring centre. The Trouble Brewing brewery, Barrelchest's arena on Harmony Island and
 * the Wizards' Tower, Tanglefoot's and the Temple of the Eye's areas north of Tutorial Island are left
 * to their own modules.
 */
final class IslandsEast {
    private IslandsEast() {
    }

    static List<StructureDef> all() {
        return List.of(mosLeHarmless(), harmonyIsland(), dragontooth(), tutorialIsland(), mudskipperPoint(), zanaris());
    }

    private static final String SHACK = "minecraft:dark_oak_planks";

    static StructureDef mosLeHarmless() {
        Builder b = StructureDef.builder("isl_mos_le_harmless", "Mos Le'Harmless").replaces(IslandsPlaces.MOS_LE_HARMLESS);
        b.tFlatten(3660, 2950, 3702, 2990, SAND);
        b.tFloor(3662, 2964, 3700, 2967, PATH);
        b.tFloor(3671, 2950, 3673, 2963, PATH);
        // Shacks: Mike's general store, Charley's hats, Patchy's and the tavern.
        b.tBuilding(3684, 2970, 3692, 2976, 1, SHACK, OAK, "minecraft:dark_oak", "south", true);
        b.tSet(3688, 2974, 0, rc("general_store"));
        b.tBuilding(3672, 2970, 3680, 2976, 1, SHACK, OAK, "minecraft:dark_oak", "south", true);
        b.tBuilding(3684, 2955, 3692, 2961, 1, SHACK, OAK, "minecraft:dark_oak", "north", true);
        b.tBuilding(3662, 2970, 3669, 2980, 1, SHACK, OAK, "minecraft:dark_oak", "east", true);
        b.tSet(3664, 2978, 0, rc("cooking_range"));
        b.tSet(3666, 2978, 0, "minecraft:barrel");
        b.tSet(3667, 2978, 0, "minecraft:barrel");
        hut(b, 3696, 2981, 4, "minecraft:stripped_dark_oak_log", "minecraft:black_wool", OAK, "west");
        hut(b, 3676, 2984, 4, "minecraft:stripped_dark_oak_log", "minecraft:black_wool", OAK, "south");
        // The dock (charters and Pirate Pete's boat).
        pier(b, 3670, 2932, 3674, 2949, SPRUCE);
        ship(b, 3676, 2938, 10, SHACK, "minecraft:black_wool");
        b.tFill(3661, 2989, 3661, 2989, 0, 5, "minecraft:dark_oak_fence");
        b.tSet(3661, 2989, 6, "minecraft:black_wool");
        for (int[] p : new int[][]{{3662, 2952}, {3700, 2952}, {3700, 2988}, {3686, 2988}, {3664, 2960}, {3696, 2964}}) {
            palm(b, p[0], p[1], 6);
        }
        lamp(b, 3682, 2963, "minecraft:dark_oak_fence");
        return b.build();
    }

    /** Harmony Island: the monastery with its altar, the herb patch and the jetty. */
    static StructureDef harmonyIsland() {
        Builder b = StructureDef.builder("isl_harmony_island", "Harmony Island").replaces(IslandsPlaces.HARMONY_ISLAND);
        b.tFlatten(3780, 2842, 3795, 2864, GRASS);
        b.tFlatten(3784, 2865, 3815, 2880, GRASS);
        b.tBuilding(3790, 2868, 3806, 2877, 2, MOSSY_STONE, "minecraft:spruce_planks", "minecraft:deepslate_tile", "south", true);
        b.tSet(3798, 2876, 0, rc("prayer_altar"));
        b.tFill(3797, 2875, 3799, 2875, -1, -1, "minecraft:red_carpet");
        b.tTower(3808, 2873, 3, 14, MOSSY_STONE, SPRUCE);
        b.tSet(3787, 2850, 0, rc("herb_patch"));
        b.tFill(3786, 2849, 3788, 2851, -1, -1, "minecraft:farmland");
        b.tFloor(3790, 2852, 3795, 2853, PATH);
        b.tFloor(3797, 2865, 3798, 2867, PATH);
        pier(b, 3772, 2852, 3779, 2854, SPRUCE);
        for (int[] t : new int[][]{{3782, 2862}, {3786, 2878}, {3813, 2867}, {3783, 2844}}) {
            tree(b, t[0], t[1], "minecraft:oak_log", "minecraft:oak_leaves[persistent=true]", 6);
        }
        return b.build();
    }

    /** Dragontooth Island: the dragon-skull rocks, bones and the wreck the Ghost captain sails past. */
    static StructureDef dragontooth() {
        Builder b = StructureDef.builder("isl_dragontooth_island", "Dragontooth Island").replaces(IslandsPlaces.DRAGONTOOTH_ISLAND);
        b.tFlatten(3792, 3544, 3824, 3570, SAND);
        // The "teeth": tall white rocks.
        for (int[] t : new int[][]{{3798, 3562, 8}, {3802, 3566, 10}, {3806, 3567, 9}, {3810, 3566, 10}, {3814, 3562, 8}}) {
            b.tFill(t[0], t[1], t[0], t[1], 0, t[2], "minecraft:calcite");
            b.tSet(t[0], t[1], t[2] + 1, "minecraft:diorite");
        }
        b.tFill(3800, 3556, 3812, 3560, 0, 0, "minecraft:bone_block");
        b.tClear(3802, 3557, 3810, 3559, 0, 0);
        b.tSet(3806, 3558, 0, "minecraft:dragon_head");
        // The wreck on the south shore.
        b.tFill(3794, 3546, 3804, 3548, -1, 0, "minecraft:dark_oak_planks");
        b.tClear(3796, 3547, 3802, 3547, 0, 0);
        b.tFill(3800, 3547, 3800, 3547, 1, 4, "minecraft:dark_oak_fence");
        b.tSet(3816, 3550, 0, "minecraft:soul_lantern");
        b.tSet(3819, 3556, 0, "minecraft:skeleton_skull");
        pier(b, 3812, 3536, 3815, 3543, "minecraft:dark_oak_planks");
        return b.build();
    }

    /**
     * Tutorial Island: the Gielinor Guide's house, the Survival Expert's woods and pond, the Master Chef's
     * kitchen, the Quest Guide's house, the mining area with furnace and anvil, the rat pit, the bank,
     * Brother Brace's chapel and the Magic Instructor's hut.
     */
    static StructureDef tutorialIsland() {
        Builder b = StructureDef.builder("isl_tutorial_island", "Tutorial Island").replaces(IslandsPlaces.TUTORIAL_ISLAND);
        b.tFlatten(3068, 3080, 3146, 3129, GRASS);
        // Paths.
        b.tFloor(3070, 3100, 3144, 3101, PATH);
        b.tFloor(3093, 3080, 3094, 3128, PATH);
        b.tFloor(3116, 3100, 3117, 3127, PATH);
        // Gielinor Guide's house.
        b.tBuilding(3090, 3104, 3098, 3111, 1, OAK, OAK, "minecraft:oak", "south", true);
        // Survival Expert: trees and the fishing pond.
        String leaves = "minecraft:oak_leaves[persistent=true]";
        for (int[] t : new int[][]{{3100, 3096}, {3108, 3098}, {3102, 3090}, {3110, 3090}}) {
            tree(b, t[0], t[1], rc("tree_tree_log"), leaves, 5);
        }
        b.tFill(3104, 3092, 3107, 3094, -2, -1, "minecraft:water");
        b.tSet(3105, 3093, -1, rc("net_bait_fishing_spot"));
        b.tSet(3102, 3094, 0, "minecraft:campfire");
        // Master Chef's kitchen.
        b.tBuilding(3072, 3082, 3080, 3088, 1, "minecraft:bricks", OAK, "minecraft:spruce", "north", true);
        b.tSet(3076, 3084, 0, rc("cooking_range"));
        b.tSet(3074, 3086, 0, "minecraft:barrel");
        // Quest Guide's house.
        b.tBuilding(3082, 3118, 3090, 3125, 1, STONE, OAK, "minecraft:spruce", "south", true);
        b.tSet(3084, 3123, 0, "minecraft:bookshelf");
        b.tSet(3088, 3123, 0, "minecraft:lectern");
        // Mining area: copper and tin rocks, the furnace and anvil.
        b.tFloor(3072, 3106, 3084, 3115, "minecraft:gravel");
        for (int x = 3073; x <= 3083; x += 2) {
            b.tSet(x, 3114, 0, rc(x % 4 == 1 ? "copper_rock" : "tin_rock"));
        }
        b.tSet(3076, 3108, 0, rc("furnace"));
        b.tSet(3080, 3108, 0, rc("anvil"));
        // The rat pit for the Combat Instructor.
        b.tFill(3104, 3116, 3114, 3126, -3, -1, "minecraft:air");
        b.tFill(3104, 3116, 3114, 3126, -4, -4, "minecraft:coarse_dirt");
        b.tWalls(3103, 3115, 3115, 3127, -4, 0, "minecraft:oak_fence");
        b.tClear(3108, 3115, 3110, 3115, -4, 0);
        b.tFill(3108, 3113, 3110, 3115, -4, -1, "minecraft:oak_stairs[facing=north]");
        // The bank and Brother Brace's chapel.
        b.tBuilding(3120, 3118, 3128, 3125, 1, STONE, "minecraft:polished_andesite", "minecraft:deepslate_tile", "west", true);
        b.tSet(3124, 3124, 0, rc("bank_booth"));
        b.tSet(3126, 3124, 0, rc("bank_booth"));
        b.tBuilding(3120, 3100, 3128, 3108, 1, STONE, "minecraft:polished_andesite", "minecraft:deepslate_tile", "west", true);
        b.tSet(3127, 3104, 0, rc("prayer_altar"));
        // The Magic Instructor's hut and the chicken pen.
        hut(b, 3140, 3086, 4, "minecraft:stripped_birch_log", "minecraft:blue_wool", "minecraft:birch_planks", "north");
        pen(b, 3130, 3082, 3136, 3088, "minecraft:oak_fence");
        pier(b, 3094, 3072, 3097, 3079, SPRUCE);
        lamp(b, 3092, 3102, "minecraft:oak_fence");
        lamp(b, 3118, 3102, "minecraft:oak_fence");
        return b.build();
    }

    static StructureDef mudskipperPoint() {
        Builder b = StructureDef.builder("isl_mudskipper_point", "Mudskipper Point").replaces(IslandsPlaces.MUDSKIPPER_POINT);
        b.tFlatten(2984, 3100, 3006, 3126, "minecraft:coarse_dirt");
        b.tFloor(2986, 3102, 3004, 3110, MUD);
        b.tFill(2990, 3104, 2993, 3106, -1, -1, "minecraft:water");
        b.tFill(2999, 3103, 3001, 3105, -1, -1, "minecraft:water");
        fairyRing(b, IslandsStructures.MUDSKIPPER_RING);
        // The wrecked rowing boat and the reeds.
        b.tFill(2987, 3118, 2991, 3119, -1, 0, OAK);
        b.tClear(2988, 3118, 2990, 3119, 0, 0);
        for (int[] r : new int[][]{{2995, 3104}, {2988, 3112}, {3003, 3116}, {2997, 3122}}) {
            b.tSet(r[0], r[1], 0, "minecraft:tall_grass[half=lower]");
        }
        rocks(b, 3004, 3122, "minecraft:mossy_cobblestone");
        rocks(b, 2985, 3124, "minecraft:mossy_cobblestone");
        return b.build();
    }

    /** Zanaris: the fairy ring centre, the bank and market, Jukat's sword shop and the fungus fields. */
    static StructureDef zanaris() {
        Builder b = StructureDef.builder("isl_zanaris", "Zanaris").replaces(IslandsPlaces.ZANARIS);
        b.tFlatten(2385, 4405, 2440, 4460, "minecraft:moss_block");
        // The fairy ring centre: the hub ring in a circle of mushrooms and flowers.
        int[] ring = IslandsStructures.ZANARIS_RING;
        b.tFloor(ring[0] - 3, ring[1] - 3, ring[0] + 3, ring[1] + 3, "minecraft:mycelium");
        b.tSet(ring[0], ring[1], 0, rc(IslandsBlocks.FAIRY_RING));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                if (Math.abs(dx) == 3 || Math.abs(dy) == 3) {
                    b.tSet(ring[0] + dx, ring[1] + dy, 0, (dx + dy) % 2 == 0 ? "minecraft:red_mushroom" : "minecraft:allium");
                }
            }
        }
        // Bank and general store; Jukat's and Irksol's shops.
        String wall = "minecraft:stripped_birch_log";
        b.tBuilding(2396, 4436, 2405, 4444, 1, wall, "minecraft:birch_planks", "minecraft:warped", "south", true);
        b.tSet(2399, 4442, 0, rc("bank_booth"));
        b.tSet(2402, 4442, 0, rc("bank_booth"));
        b.tBuilding(2414, 4436, 2423, 4444, 1, wall, "minecraft:birch_planks", "minecraft:warped", "south", true);
        b.tSet(2418, 4442, 0, rc("general_store"));
        b.tBuilding(2416, 4419, 2423, 4425, 1, wall, "minecraft:birch_planks", "minecraft:crimson", "west", true);
        b.tSet(2421, 4422, 0, rc("weapon_shop"));
        b.tBuilding(2397, 4419, 2404, 4425, 1, wall, "minecraft:birch_planks", "minecraft:crimson", "east", true);
        // Glowing trees and lanterns.
        String leaves = "minecraft:flowering_azalea_leaves[persistent=true]";
        for (int[] t : new int[][]{{2388, 4458}, {2438, 4458}, {2388, 4430}, {2438, 4430}, {2410, 4456}, {2390, 4446},
                {2434, 4446}}) {
            tree(b, t[0], t[1], "minecraft:birch_log", leaves, 6);
        }
        for (int[] l : new int[][]{{2406, 4432}, {2414, 4432}, {2406, 4424}, {2414, 4424}}) {
            lamp(b, l[0], l[1], "minecraft:birch_fence");
        }
        b.tFloor(2408, 4405, 2411, 4434, PATH);
        // The fungus fields in the south where the zygomites grow.
        b.tFloor(2392, 4406, 2430, 4414, "minecraft:mycelium");
        for (int[] m : new int[][]{{2394, 4408, 5}, {2402, 4411, 6}, {2420, 4409, 5}, {2428, 4412, 6}}) {
            mushroom(b, m[0], m[1], m[2], 2, "minecraft:red_mushroom_block");
        }
        return b.build();
    }
}
