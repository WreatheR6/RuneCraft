package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * The northern seas: Miscellania (King Vargas' castle with the kingdom's royal ledger, the village,
 * the maples, coal rocks, flax and fishing jetty, fairy ring CIP and the ferry dock) and Etceteria
 * (Queen Sigrid's castle); Lunar Isle's Moonclan village, Baba Yaga's house, the dock and the mine;
 * Neitiznot (the Burgher's longhall, the yak pen) and Jatizso (walled town, King Gjuki's tower) with
 * the rope bridge between them; Waterbirth Island; Pirates' Cove; Ungael; and Lithkren's ruins.
 *
 * <p>Left alone: the Miscellania dungeon and the giant sea snake's arena, Etceteria's patches, the
 * Lunar Isle altar plaza (systems), the dream world arenas and the Astral Altar, Neitiznot's arctic
 * pines, the ice troll cave and its king's arena, Dagannoth Rex's arena and Vorkath's arenas.
 */
final class IslandsNorth {
    private IslandsNorth() {
    }

    static List<StructureDef> all() {
        return List.of(miscellania(), etceteria(), lunarIsle(), lunarMine(), neitiznot(), jatizso(), waterbirth(), piratesCove(),
                ungael(), lithkren());
    }

    private static final String MAPLE = rc("maple_tree_log");
    private static final String OAK_LEAVES = "minecraft:oak_leaves[persistent=true]";
    private static final String SPRUCE_LEAVES = "minecraft:spruce_leaves[persistent=true]";

    // ------------------------------------------------------------------ Miscellania & Etceteria

    static StructureDef miscellania() {
        Builder b = StructureDef.builder("isl_miscellania", "Miscellania").replaces(IslandsPlaces.MISCELLANIA);
        b.tFlatten(2483, 3855, 2506, 3878, GRASS);
        b.tFlatten(2511, 3858, 2548, 3890, GRASS);
        // King Vargas' castle: curtain walls with corner towers and the keep.
        String w = STONE;
        wall(b, 2485, 3857, 2493, 3857, 6, w);
        wall(b, 2496, 3857, 2504, 3857, 6, w);
        wall(b, 2485, 3876, 2504, 3876, 6, w);
        wall(b, 2485, 3857, 2485, 3876, 6, w);
        wall(b, 2504, 3857, 2504, 3876, 6, w);
        b.tFill(2494, 3857, 2495, 3857, 4, 5, w);
        b.tTower(2485, 3857, 3, 9, w, ANDESITE);
        b.tTower(2504, 3857, 3, 9, w, ANDESITE);
        b.tTower(2485, 3876, 3, 9, w, ANDESITE);
        b.tTower(2504, 3876, 3, 9, w, ANDESITE);
        b.tFloor(2494, 3855, 2495, 3863, COBBLE);
        b.tBuilding(2489, 3864, 2500, 3873, 3, w, "minecraft:spruce_planks", "minecraft:deepslate_tile", "south", true);
        // Throne room, the royal ledger and the workforce board (kingdom management).
        b.tFill(2493, 3870, 2496, 3872, -1, -1, "minecraft:red_wool");
        b.tSet(2494, 3872, 0, "minecraft:gold_block");
        b.tSet(2495, 3872, 0, "minecraft:gold_block");
        b.tSet(2491, 3866, 0, rc(IslandsBlocks.KINGDOM_LEDGER));
        b.tSet(2498, 3866, 0, rc(IslandsBlocks.KINGDOM_WORKFORCE));
        b.tFill(2493, 3872, 2496, 3872, 2, 2, "minecraft:blue_wool");
        // The village: longhall, houses, the maples, coal rocks, the flax and herb fields.
        longhall(b, 2513, 3863, 2523, 3870, "south");
        b.tBuilding(2527, 3863, 2533, 3868, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "south", true);
        b.tBuilding(2537, 3865, 2543, 3870, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "west", true);
        b.tFloor(2511, 3861, 2548, 3861, PATH);
        field(b, 2514, 3875, 2524, 3879, "minecraft:cornflower");
        field(b, 2526, 3875, 2530, 3879, "minecraft:fern");
        b.tFill(2514, 3874, 2530, 3874, 0, 0, "minecraft:oak_fence");
        for (int[] t : new int[][]{{2536, 3878}, {2540, 3882}, {2544, 3877}, {2546, 3886}, {2538, 3887}, {2533, 3884}}) {
            tree(b, t[0], t[1], MAPLE, "minecraft:acacia_leaves[persistent=true]", 6);
        }
        for (int x = 2544; x <= 2547; x++) {
            b.tSet(x, 3864, 0, rc("coal_rock"));
        }
        b.tFill(2543, 3863, 2548, 3863, 0, 1, "minecraft:stone");
        // Fishing jetty and the ferry dock (boats from Rellekka).
        pier(b, 2549, 3872, 2556, 3874, SPRUCE);
        pier(b, 2574, 3845, 2584, 3848, SPRUCE);
        b.tSet(2584, 3846, 0, "minecraft:spruce_fence");
        b.tSet(2584, 3846, 1, "minecraft:lantern");
        fairyRing(b, IslandsStructures.MISCELLANIA_RING);
        lamp(b, 2512, 3860, "minecraft:spruce_fence");
        lamp(b, 2534, 3860, "minecraft:spruce_fence");
        return b.build();
    }

    static StructureDef etceteria() {
        Builder b = StructureDef.builder("isl_etceteria", "Etceteria").replaces(IslandsPlaces.ETCETERIA);
        b.tFlatten(2604, 3866, 2632, 3894, GRASS);
        String w = STONE;
        wall(b, 2610, 3870, 2617, 3870, 6, w);
        wall(b, 2620, 3870, 2628, 3870, 6, w);
        wall(b, 2610, 3890, 2628, 3890, 6, w);
        wall(b, 2610, 3870, 2610, 3890, 6, w);
        wall(b, 2628, 3870, 2628, 3890, 6, w);
        b.tFill(2618, 3870, 2619, 3870, 4, 5, w);
        b.tTower(2610, 3870, 3, 9, w, ANDESITE);
        b.tTower(2628, 3870, 3, 9, w, ANDESITE);
        b.tTower(2610, 3890, 3, 9, w, ANDESITE);
        b.tTower(2628, 3890, 3, 9, w, ANDESITE);
        b.tFloor(2618, 3866, 2619, 3876, COBBLE);
        // Queen Sigrid's keep with the castle kitchen.
        b.tBuilding(2613, 3877, 2625, 3887, 2, w, "minecraft:spruce_planks", "minecraft:deepslate_tile", "south", true);
        b.tFill(2617, 3884, 2620, 3886, -1, -1, "minecraft:purple_wool");
        b.tSet(2618, 3886, 0, "minecraft:gold_block");
        b.tSet(2624, 3879, 0, rc("cooking_range"));
        // The village general store.
        b.tBuilding(2604, 3879, 2608, 3885, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "east", true);
        b.tSet(2605, 3882, 0, rc("general_store"));
        for (int[] t : new int[][]{{2606, 3869}, {2631, 3868}, {2631, 3893}, {2606, 3892}}) {
            tree(b, t[0], t[1], "minecraft:oak_log", OAK_LEAVES, 6);
        }
        return b.build();
    }

    // ------------------------------------------------------------------ Lunar Isle

    /** The Moonclan village east and north of the dream world arenas, Baba Yaga's house and the dock. */
    static StructureDef lunarIsle() {
        Builder b = StructureDef.builder("isl_lunar_isle", "Moonclan village").replaces(IslandsPlaces.LUNAR_ISLE);
        b.tFlatten(2106, 3894, 2134, 3928, GRASS);
        b.tFlatten(2072, 3930, 2104, 3942, GRASS);
        String wall = "minecraft:calcite";
        String roof = "minecraft:purple_terracotta";
        b.tBuilding(2108, 3920, 2114, 3925, 1, wall, "minecraft:smooth_stone", roof, "south", false);
        b.tBuilding(2118, 3918, 2125, 3925, 1, wall, "minecraft:smooth_stone", roof, "south", false);
        b.tBuilding(2110, 3898, 2116, 3905, 1, wall, "minecraft:smooth_stone", roof, "north", false);
        b.tBuilding(2120, 3900, 2127, 3907, 1, wall, "minecraft:smooth_stone", roof, "west", false);
        b.tSet(2112, 3901, 0, rc("general_store"));
        b.tSet(2124, 3904, 0, rc("cooking_range"));
        b.tSet(2121, 3922, 0, "minecraft:enchanting_table");
        b.tSet(2122, 3920, 0, "minecraft:bookshelf");
        b.tFloor(2112, 3908, 2117, 3916, "minecraft:smooth_stone");
        b.tSet(2114, 3912, 0, "minecraft:amethyst_block");
        b.tSet(2114, 3912, 1, "minecraft:amethyst_cluster");
        // Dock where Lokar's and Pirate Pete's ship lands.
        b.tFloor(2128, 3898, 2134, 3900, PATH);
        pier(b, 2135, 3898, 2142, 3900, SPRUCE);
        ship(b, 2136, 3904, 10, "minecraft:dark_oak_planks", "minecraft:white_wool");
        // North: Baba Yaga's chicken-legged house and a Moonclan house.
        int x1 = StructureDef.bx(2090);
        int x2 = StructureDef.bx(2096) + 1;
        int z1 = StructureDef.bz(3938);
        int z2 = StructureDef.bz(3933) + 1;
        int g = b.ground();
        for (int[] c : new int[][]{{x1 + 1, z1 + 1}, {x2 - 1, z1 + 1}, {x1 + 1, z2 - 1}, {x2 - 1, z2 - 1}}) {
            b.fill(c[0], g, c[1], c[0], g + 2, c[1], "minecraft:stripped_birch_log");
            b.set(c[0], g, c[1], "minecraft:yellow_terracotta");
        }
        b.fill(x1, g + 3, z1, x2, g + 3, z2, SPRUCE);
        b.walls(x1, g + 4, z1, x2, g + 6, z2, "minecraft:spruce_log");
        b.clear(x1 + 1, g + 4, z1 + 1, x2 - 1, g + 6, z2 - 1);
        b.roof(x1, z1, x2, z2, g + 7, "minecraft:dark_oak");
        b.clear((x1 + x2) / 2, g + 4, z2, (x1 + x2) / 2, g + 5, z2);
        b.fill((x1 + x2) / 2, g, z2, (x1 + x2) / 2, g + 2, z2, "minecraft:stripped_birch_log");
        b.fill((x1 + x2) / 2, g, z2 + 1, (x1 + x2) / 2, g + 3, z2 + 1, "minecraft:ladder[facing=south]");
        b.set(x1 + 2, g + 4, z1 + 2, "minecraft:cauldron");
        b.tBuilding(2076, 3933, 2082, 3939, 1, wall, "minecraft:smooth_stone", roof, "south", false);
        for (int[] t : new int[][]{{2074, 3941}, {2085, 3941}, {2100, 3934}, {2130, 3926}, {2132, 3896}, {2107, 3896}}) {
            tree(b, t[0], t[1], "minecraft:spruce_log", SPRUCE_LEAVES, 7);
        }
        return b.build();
    }

    /** The Lunar Isle mine entrance with its essence and coal rocks. */
    static StructureDef lunarMine() {
        Builder b = StructureDef.builder("isl_lunar_isle_mine", "Lunar Isle mine").replaces(IslandsPlaces.LUNAR_ISLE_MINE);
        b.tFlatten(2136, 3936, 2154, 3950, "minecraft:stone");
        b.tFill(2140, 3944, 2150, 3949, 0, 4, "minecraft:stone");
        b.tFill(2142, 3945, 2148, 3948, 5, 6, "minecraft:stone");
        b.tClear(2144, 3943, 2146, 3947, 0, 2);
        b.tFill(2144, 3943, 2144, 3943, 0, 2, "minecraft:spruce_log");
        b.tFill(2146, 3943, 2146, 3943, 0, 2, "minecraft:spruce_log");
        b.tFill(2144, 3943, 2146, 3943, 3, 3, SPRUCE);
        b.tSet(2145, 3947, 0, rc("pure_essence_rock"));
        b.tSet(2144, 3946, 0, rc("coal_rock"));
        b.tSet(2146, 3946, 0, rc("coal_rock"));
        b.tSet(2145, 3943, 2, "minecraft:lantern[hanging=true]");
        rocks(b, 2138, 3938, "minecraft:stone");
        rocks(b, 2152, 3939, "minecraft:andesite");
        b.tFill(2144, 3937, 2146, 3942, -1, -1, "minecraft:gravel");
        return b.build();
    }

    // ------------------------------------------------------------------ the Fremennik Isles

    static StructureDef neitiznot() {
        Builder b = StructureDef.builder("isl_neitiznot", "Neitiznot").replaces(IslandsPlaces.NEITIZNOT);
        b.tFlatten(2310, 3780, 2366, 3812, GRASS);
        b.tFlatten(2342, 3814, 2366, 3832, GRASS);
        // The Burgher's longhall with the bank.
        longhall(b, 2329, 3801, 2343, 3811, "south");
        b.tSet(2335, 3809, 0, rc("bank_booth"));
        b.tSet(2337, 3809, 0, rc("bank_booth"));
        b.tFill(2333, 3805, 2339, 3805, -1, -1, "minecraft:red_carpet");
        b.tSet(2341, 3806, 0, "minecraft:campfire");
        // Houses, Thakkrad's tannery and the well.
        longhall(b, 2313, 3786, 2321, 3793, "east");
        longhall(b, 2350, 3786, 2358, 3793, "west");
        longhall(b, 2352, 3800, 2360, 3807, "west");
        b.tBuilding(2344, 3789, 2348, 3795, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "north", true);
        b.tSet(2346, 3791, 0, rc("tanning_rack"));
        b.tFill(2336, 3790, 2337, 3791, -1, -1, "minecraft:water");
        b.tFill(2335, 3789, 2338, 3792, 0, 0, "minecraft:cobblestone_wall");
        b.tClear(2336, 3790, 2337, 3791, 0, 0);
        b.tFloor(2310, 3797, 2366, 3798, PATH);
        b.tFloor(2336, 3780, 2337, 3796, PATH);
        // The yak pen north-east of the longhall.
        pen(b, 2345, 3816, 2364, 3830, "minecraft:spruce_fence");
        b.tSet(2350, 3820, 0, "minecraft:hay_block");
        b.tSet(2358, 3826, 0, "minecraft:hay_block");
        pier(b, 2330, 3772, 2333, 3779, SPRUCE);
        for (int[] t : new int[][]{{2312, 3781}, {2364, 3781}, {2312, 3810}, {2325, 3782}}) {
            tree(b, t[0], t[1], "minecraft:spruce_log", SPRUCE_LEAVES, 7);
        }
        return b.build();
    }

    static StructureDef jatizso() {
        Builder b = StructureDef.builder("isl_jatizso", "Jatizso").replaces(IslandsPlaces.JATIZSO);
        b.tFlatten(2394, 3782, 2436, 3828, COBBLE);
        b.tFloor(2394, 3782, 2436, 3828, "minecraft:gravel");
        String w = STONE;
        wall(b, 2396, 3784, 2434, 3784, 6, w);
        wall(b, 2396, 3826, 2412, 3826, 6, w);
        wall(b, 2418, 3826, 2434, 3826, 6, w);
        wall(b, 2396, 3784, 2396, 3802, 6, w);
        wall(b, 2396, 3807, 2396, 3826, 6, w);
        wall(b, 2434, 3784, 2434, 3826, 6, w);
        b.tFill(2396, 3803, 2396, 3806, 4, 5, w);
        b.tFill(2413, 3826, 2417, 3826, 4, 5, w);
        b.tTower(2396, 3784, 3, 9, w, ANDESITE);
        b.tTower(2434, 3784, 3, 9, w, ANDESITE);
        b.tTower(2396, 3826, 3, 9, w, ANDESITE);
        b.tTower(2434, 3826, 3, 9, w, ANDESITE);
        // King Gjuki Sorvott IV's tower and his longhall with the bank.
        b.tTower(2422, 3817, 5, 16, w, SPRUCE);
        b.tClear(2422, 3812, 2422, 3812, 0, 2);
        longhall(b, 2408, 3793, 2426, 3803, "south");
        b.tSet(2415, 3801, 0, rc("bank_booth"));
        b.tSet(2417, 3801, 0, rc("bank_booth"));
        b.tFill(2416, 3794, 2416, 3800, -1, -1, "minecraft:red_carpet");
        b.tSet(2422, 3797, 0, rc("cooking_range"));
        // The taxed shops: ore (Hring Hring), fish (Flosi), weapons (Raum) and armour (Skuli).
        b.tBuilding(2400, 3787, 2406, 3792, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "north", true);
        b.tBuilding(2427, 3787, 2432, 3792, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "north", true);
        b.tBuilding(2400, 3809, 2406, 3814, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "south", true);
        b.tBuilding(2427, 3807, 2432, 3812, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "south", true);
        b.tSet(2403, 3789, 0, rc("anvil"));
        b.tSet(2404, 3811, 0, rc("weapon_shop"));
        b.tFloor(2397, 3804, 2433, 3805, COBBLE);
        b.tFloor(2414, 3806, 2416, 3828, COBBLE);
        // The rope bridge west to Neitiznot.
        b.tFill(2367, 3804, 2395, 3805, -1, -1, SPRUCE);
        b.tFill(2367, 3803, 2395, 3803, 0, 0, "minecraft:spruce_fence");
        b.tFill(2367, 3806, 2395, 3806, 0, 0, "minecraft:spruce_fence");
        for (int x = 2370; x <= 2392; x += 4) {
            b.tSet(x, 3803, -8, "minecraft:spruce_log");
            b.tFill(x, 3803, x, 3803, -7, -2, "minecraft:spruce_log");
            b.tSet(x, 3806, -8, "minecraft:spruce_log");
            b.tFill(x, 3806, x, 3806, -7, -2, "minecraft:spruce_log");
        }
        pier(b, 2415, 3774, 2418, 3781, SPRUCE);
        return b.build();
    }

    // ------------------------------------------------------------------ Waterbirth Island, Pirates' Cove

    static StructureDef waterbirth() {
        Builder b = StructureDef.builder("isl_waterbirth_island", "Waterbirth Island").replaces(IslandsPlaces.WATERBIRTH);
        b.tFlatten(2514, 3726, 2546, 3765, "minecraft:coarse_dirt");
        // Jarvald's landing at the north.
        pier(b, 2542, 3766, 2545, 3774, SPRUCE);
        b.tFloor(2540, 3755, 2545, 3765, "minecraft:gravel");
        // The cave mouth leading down to the Waterbirth Island Dungeon.
        b.tFill(2518, 3738, 2524, 3743, 0, 5, "minecraft:stone");
        b.tFill(2519, 3739, 2523, 3742, 6, 7, "minecraft:stone");
        b.tClear(2520, 3737, 2522, 3740, 0, 2);
        b.tSet(2520, 3737, 2, "minecraft:cobweb");
        // Rocks, dead trees and dagannoth bones on the shore.
        for (int[] r : new int[][]{{2516, 3730}, {2530, 3728}, {2544, 3732}, {2536, 3748}, {2526, 3760}, {2516, 3756}, {2544, 3745}}) {
            rocks(b, r[0], r[1], "minecraft:andesite");
        }
        for (int[] t : new int[][]{{2528, 3734}, {2538, 3740}, {2520, 3750}, {2532, 3756}}) {
            b.tFill(t[0], t[1], t[0], t[1], 0, 3, "minecraft:dark_oak_log");
            b.tSet(t[0], t[1], 4, "minecraft:dark_oak_fence");
        }
        b.tSet(2534, 3744, 0, "minecraft:bone_block");
        b.tSet(2535, 3744, 0, "minecraft:bone_block[axis=x]");
        b.tSet(2527, 3746, 0, "minecraft:skeleton_skull");
        return b.build();
    }

    static StructureDef piratesCove() {
        Builder b = StructureDef.builder("isl_pirates_cove", "Pirates' Cove").replaces(IslandsPlaces.PIRATES_COVE);
        b.tFlatten(2196, 3794, 2228, 3822, SAND);
        b.tFloor(2198, 3804, 2226, 3806, PATH);
        // The pirate bank and the tavern.
        b.tBuilding(2214, 3808, 2222, 3814, 1, "minecraft:dark_oak_planks", OAK, "minecraft:dark_oak", "south", true);
        b.tSet(2216, 3812, 0, rc("bank_booth"));
        b.tSet(2218, 3812, 0, rc("bank_booth"));
        b.tBuilding(2200, 3808, 2208, 3816, 1, "minecraft:dark_oak_planks", OAK, "minecraft:dark_oak", "south", true);
        b.tSet(2202, 3814, 0, rc("cooking_range"));
        b.tSet(2206, 3814, 0, "minecraft:barrel");
        b.tSet(2205, 3814, 0, "minecraft:barrel");
        hut(b, 2203, 3798, 4, "minecraft:stripped_oak_log", "minecraft:black_wool", OAK, "north");
        hut(b, 2222, 3798, 4, "minecraft:stripped_oak_log", "minecraft:black_wool", OAK, "north");
        // Lokar Searunner's ship at the jetty.
        pier(b, 2208, 3785, 2211, 3793, SPRUCE);
        ship(b, 2213, 3788, 12, "minecraft:dark_oak_planks", "minecraft:black_wool");
        b.tFill(2196, 3822, 2196, 3822, 0, 5, "minecraft:dark_oak_fence");
        b.tSet(2196, 3822, 6, "minecraft:black_wool");
        for (int[] p : new int[][]{{2198, 3820}, {2226, 3820}, {2212, 3820}}) {
            palm(b, p[0], p[1], 5);
        }
        return b.build();
    }

    // ------------------------------------------------------------------ Ungael and Lithkren

    /** Ungael: Torfinn's landing and the frozen ruins west of Vorkath's ice (his arenas are left alone). */
    static StructureDef ungael() {
        Builder b = StructureDef.builder("isl_ungael", "Ungael").replaces(IslandsPlaces.UNGAEL);
        b.tFlatten(2250, 4030, 2265, 4062, SNOW);
        b.tFlatten(2266, 4055, 2275, 4066, SNOW);
        pier(b, 2241, 4044, 2249, 4046, SPRUCE);
        ship(b, 2232, 4049, 7, "minecraft:spruce_planks", "minecraft:red_wool");
        // Frozen dragonkin ruins.
        ruins(b, 2254, 4050, 2262, 4058, 5, "minecraft:deepslate_tiles", "minecraft:packed_ice");
        ruins(b, 2266, 4058, 2273, 4064, 4, "minecraft:deepslate_tiles", "minecraft:packed_ice");
        for (int[] s : new int[][]{{2252, 4034}, {2260, 4040}, {2256, 4044}, {2263, 4033}, {2276, 4036}}) {
            b.tFill(s[0], s[1], s[0], s[1], 0, 2 + (s[0] % 3), "minecraft:packed_ice");
        }
        b.tSet(2258, 4054, 0, "minecraft:bone_block");
        b.tSet(2259, 4054, 0, "minecraft:dragon_head");
        return b.build();
    }

    /**
     * Lithkren: the ruined dragonkin research facility and Dallas Jones' dig tent, west and north of the
     * Lithkren Vault's entrance hut (the vault is the dungeons module's and is left alone).
     */
    static StructureDef lithkren() {
        Builder b = StructureDef.builder("isl_lithkren", "Lithkren").replaces(IslandsPlaces.LITHKREN);
        b.tFlatten(3528, 3988, 3545, 4014, "minecraft:coarse_dirt");
        b.tFlatten(3546, 4004, 3566, 4014, "minecraft:coarse_dirt");
        ruins(b, 3530, 4000, 3540, 4010, 6, "minecraft:deepslate_tiles", "minecraft:cobbled_deepslate");
        ruins(b, 3550, 4006, 3562, 4013, 5, "minecraft:deepslate_tiles", "minecraft:cobbled_deepslate");
        tent(b, 3533, 3990, 3538, 3994, "minecraft:brown_wool");
        b.tSet(3543, 3992, 0, "minecraft:dragon_head");
        b.tSet(3542, 3996, 0, "minecraft:bone_block");
        b.tSet(3564, 4005, 0, "minecraft:dragon_head");
        pier(b, 3534, 3980, 3537, 3987, SPRUCE);
        return b.build();
    }
}
