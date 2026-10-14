package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Feldip Hills and the west: the ogre city of Gu'Tanoth, Castle Wars (the lobby and the arena with
 * the Saradomin and Zamorak castles, fairy ring BKP), Corsair Cove (the town, its docks and the
 * Corsair Cove Dungeon carved beneath its entrance), the Feldip Hunter area (fairy ring AKS),
 * Zul-Andra (fairy ring DLR), Port Tyras, Braindeath Island and Isafdar's dense forest.
 *
 * <p>Left alone: the Ogre Shaman's arena east of Gu'Tanoth, the Smoke Devil Dungeon and its boss's
 * arena south of Castle Wars, Yanille, Jiggig, Ithoi's hut and arena, the Myths' Guild, Zulrah's arena
 * and the sacred eel shore, King Tyras' camp and Lletya.
 */
final class IslandsWest {
    private IslandsWest() {
    }

    static List<StructureDef> all() {
        return List.of(guTanoth(), castleWars(), corsairCove(), corsairCoveDocks(), feldipHunterArea(), zulAndra(), portTyras(),
                braindeathIsland(), isafdar());
    }

    private static final String OGRE_WALL = "minecraft:mossy_cobblestone";

    /** Gu'Tanoth: the walled ogre city with its huts, market and the south gate. */
    static StructureDef guTanoth() {
        Builder b = StructureDef.builder("isl_gu_tanoth", "Gu'Tanoth").replaces(IslandsPlaces.GU_TANOTH);
        b.tFlatten(2490, 3016, 2523, 3053, "minecraft:coarse_dirt");
        b.tFlatten(2524, 3012, 2545, 3028, "minecraft:coarse_dirt");
        wall(b, 2491, 3017, 2504, 3017, 5, OGRE_WALL);
        wall(b, 2509, 3017, 2522, 3017, 5, OGRE_WALL);
        wall(b, 2491, 3052, 2522, 3052, 5, OGRE_WALL);
        wall(b, 2491, 3017, 2491, 3052, 5, OGRE_WALL);
        wall(b, 2522, 3017, 2522, 3052, 5, OGRE_WALL);
        b.tFill(2505, 3017, 2508, 3017, 4, 5, OGRE_WALL);
        b.tFill(2505, 3012, 2508, 3036, -1, -1, PATH);
        // Ogre huts of mud brick and thatch.
        int[][] huts = {{2496, 3046, 3}, {2506, 3047, 3}, {2516, 3046, 3}, {2496, 3024, 3}, {2516, 3024, 3}, {2497, 3036, 2}};
        for (int[] h : huts) {
            hut(b, h[0], h[1], h[2] * 2, MUD_BRICKS, "minecraft:hay_block", PACKED_MUD, "south");
        }
        // The market (ogre trader and merchant) and the rock-cake oven.
        b.tFloor(2512, 3032, 2520, 3044, PACKED_MUD);
        b.tSet(2514, 3034, 0, rc("general_store"));
        b.tSet(2514, 3042, 0, rc("cooking_range"));
        for (int[] p : new int[][]{{2513, 3033}, {2515, 3033}, {2513, 3035}, {2515, 3035}}) {
            b.tFill(p[0], p[1], p[0], p[1], 0, 2, "minecraft:spruce_fence");
        }
        b.tFill(2513, 3033, 2515, 3035, 3, 3, "minecraft:brown_wool");
        // Ogre totems and the battlefield bones.
        for (int[] t : new int[][]{{2502, 3038}, {2510, 3030}}) {
            b.tFill(t[0], t[1], t[0], t[1], 0, 3, "minecraft:stripped_dark_oak_log");
            b.tSet(t[0], t[1], 4, "minecraft:skeleton_skull");
        }
        // South of the gate: the rocky approach.
        for (int[] r : new int[][]{{2528, 3016}, {2536, 3022}, {2542, 3014}, {2531, 3025}}) {
            rocks(b, r[0], r[1], "minecraft:stone");
        }
        return b.build();
    }

    /** A Castle Wars castle: a keep with corner towers, battlements, banners and the team's standard. */
    private static void castle(Builder b, int tx1, int ty1, String banner, String stand, String door) {
        int tx2 = tx1 + 8;
        int ty2 = ty1 + 8;
        building(b, tx1, ty1, tx2, ty2, 3, STONE, ANDESITE, "minecraft:stone_brick_slab", door, false);
        b.tTower(tx1, ty1, 3, 15, STONE, ANDESITE);
        b.tTower(tx2, ty1, 3, 15, STONE, ANDESITE);
        b.tTower(tx1, ty2, 3, 15, STONE, ANDESITE);
        b.tTower(tx2, ty2, 3, 15, STONE, ANDESITE);
        b.tFill(tx1 + 3, ty1 + 3, tx1 + 5, ty1 + 5, 12, 12, banner);
        b.tSet(tx1 + 4, ty1 + 4, 13, rc(stand));
        for (int t = tx1 + 1; t < tx2; t += 2) {
            b.tSet(t, ty1, 8, banner);
            b.tSet(t, ty2, 8, banner);
        }
    }

    /** Castle Wars: the lobby (entrance, rewards, bank) and the arena with its two castles. */
    static StructureDef castleWars() {
        Builder b = StructureDef.builder("isl_castle_wars", "Castle Wars").replaces(IslandsPlaces.CASTLE_WARS);
        b.tFlatten(2434, 3076, 2450, 3098, GRASS);
        b.tFlatten(2366, 3074, 2433, 3136, GRASS);
        b.tFlatten(2381, 3031, 2389, 3039, GRASS);
        // The lobby as on the OSRS map (mejrs layers_osrs): two chamfered octagons, 2435-2446 x 3081-3089 and
        // 3090-3098, with notched sides; the door east at 3089-3090 onto the bridge over the river and a
        // door north at 2444. An open oculus over 2440,3090 keeps ring-of-dueling arrivals indoors.
        int g = b.ground();
        int lx = StructureDef.bx(2435);
        int lz = StructureDef.bz(3081) + 1;
        for (int u = 0; u < 24; u++) {
            for (int w = 0; w < 36; w++) {
                if (!lobby(u, w)) {
                    continue;
                }
                int x = lx + u;
                int z = lz - w;
                boolean edge = !lobby(u - 1, w) || !lobby(u + 1, w) || !lobby(u, w - 1) || !lobby(u, w + 1);
                b.fill(x, g - 3, z, x, g - 1, z, "minecraft:polished_andesite");
                if (edge) {
                    b.fill(x, g, z, x, g + 5, z, STONE);
                    b.set(x, g + 7, z, "minecraft:stone_brick_wall");
                } else if ((u + w) % 9 == 0) {
                    b.set(x, g + 5, z, "minecraft:lantern[hanging=true]");
                }
                b.set(x, g + 6, z, "minecraft:stone_bricks");
            }
        }
        for (int w = 16; w <= 19; w++) {
            b.clear(lx + 18, g, lz - w, lx + 23, g + 2, lz - w);
        }
        b.clear(lx + 18, g, lz - 35, lx + 19, g + 2, lz - 35);
        b.clear(lx + 10, g + 6, lz - 19, lx + 11, g + 7, lz - 18);
        b.tFill(2446, 3089, 2455, 3090, -1, -1, "minecraft:spruce_planks");
        b.tFill(2447, 3089, 2455, 3090, -4, -2, "minecraft:spruce_log");
        b.tSet(2442, 3092, 0, rc("castle_wars_entrance"));
        b.tSet(2443, 3086, 0, rc("castle_wars_rewards"));
        b.tSet(2444, 3082, 0, rc("bank_booth"));
        b.tSet(2439, 3092, 0, "minecraft:blue_stained_glass");
        b.tSet(2439, 3092, 1, "minecraft:blue_stained_glass");
        b.tSet(2445, 3092, 0, "minecraft:red_stained_glass");
        b.tSet(2445, 3092, 1, "minecraft:red_stained_glass");
        // The arena: Saradomin's castle in the south-east, Zamorak's in the north-west.
        castle(b, 2424, 3074, "minecraft:blue_wool", "cw_saradomin_stand", "west");
        castle(b, 2367, 3127, "minecraft:red_wool", "cw_zamorak_stand", "east");
        // The rocky centre, barricade trees and the stepping stones over the river.
        b.tFill(2396, 3100, 2404, 3112, 0, 3, "minecraft:stone");
        b.tFill(2398, 3102, 2402, 3110, 4, 5, "minecraft:stone");
        b.tFill(2366, 3104, 2393, 3106, -2, -1, "minecraft:water");
        b.tFill(2407, 3104, 2433, 3106, -2, -1, "minecraft:water");
        for (int x = 2370; x <= 2392; x += 3) {
            b.tSet(x, 3105, -1, "minecraft:stone");
        }
        for (int x = 2409; x <= 2431; x += 3) {
            b.tSet(x, 3105, -1, "minecraft:stone");
        }
        String leaves = "minecraft:oak_leaves[persistent=true]";
        for (int[] t : new int[][]{{2380, 3090}, {2390, 3080}, {2410, 3122}, {2420, 3130}, {2372, 3115}, {2428, 3095},
                {2385, 3125}, {2415, 3085}}) {
            tree(b, t[0], t[1], "minecraft:oak_log", leaves, 6);
        }
        // Fairy ring BKP south of the arena.
        fairyRing(b, IslandsStructures.CASTLE_WARS_RING);
        return b.build();
    }

    /** Castle Wars lobby footprint in blocks: u east from 2435, w north from 3081 (24 x 36). */
    private static boolean lobby(int u, int w) {
        if (u < 0 || u > 23 || w < 0 || w > 35) {
            return false;
        }
        int v = w % 18;
        if (Math.min(u, 23 - u) + Math.min(v, 17 - v) < 4) {
            return false; // chamfered corners
        }
        return !((u < 4 || u > 21) && v >= 6 && v <= 11); // notched sides
    }

    /** Corsair Cove: houses, the tavern and the Corsair Cove Dungeon carved beneath its entrance. */
    static StructureDef corsairCove() {
        Builder b = StructureDef.builder("isl_corsair_cove", "Corsair Cove").replaces(IslandsPlaces.CORSAIR_COVE);
        b.tFlatten(2550, 2848, 2592, 2878, SAND);
        b.tFlatten(2517, 2855, 2529, 2869, SAND);
        String wall = "minecraft:stripped_oak_log";
        building(b, 2551, 2850, 2559, 2856, 1, wall, OAK, "minecraft:oak", "east", true);
        building(b, 2582, 2850, 2590, 2856, 1, wall, OAK, "minecraft:oak", "west", true);
        building(b, 2551, 2869, 2559, 2876, 1, wall, OAK, "minecraft:oak", "east", true);
        building(b, 2581, 2869, 2590, 2876, 1, wall, OAK, "minecraft:oak", "west", true);
        b.tSet(2584, 2874, 0, rc("cooking_range"));
        b.tSet(2588, 2874, 0, "minecraft:barrel");
        b.tFloor(2560, 2861, 2581, 2863, PATH);
        for (int[] p : new int[][]{{2552, 2860}, {2590, 2861}, {2560, 2847}, {2580, 2847}, {2570, 2876}, {2552, 2866}}) {
            palm(b, p[0], p[1], 6);
        }
        int[] e = IslandsStructures.CORSAIR_DUNGEON_ENTRANCE;
        cave(b, e[0], e[1], IslandsStructures.CORSAIR_DUNGEON_HALF, IslandsStructures.CORSAIR_DUNGEON_HALF, "minecraft:stone",
                "minecraft:gravel");
        return b.build();
    }

    /** The docks of Corsair Cove (the skilling module's port with its task board) and the ship. */
    static StructureDef corsairCoveDocks() {
        Builder b = StructureDef.builder("isl_corsair_cove_docks", "Corsair Cove docks").replaces(IslandsPlaces.CORSAIR_COVE_DOCKS);
        b.tFlatten(2566, 2858, 2574, 2866, SAND);
        b.tFloor(2566, 2858, 2574, 2866, "minecraft:spruce_planks");
        b.tSet(2570, 2865, 0, rc("corsair_cove_task_board"));
        b.tSet(2567, 2859, 0, "minecraft:barrel");
        b.tSet(2573, 2859, 0, "minecraft:barrel");
        b.tFill(2569, 2832, 2571, 2857, -1, -1, "minecraft:spruce_planks");
        b.tFill(2568, 2832, 2568, 2846, 0, 0, "minecraft:spruce_fence");
        b.tFill(2572, 2832, 2572, 2846, 0, 0, "minecraft:spruce_fence");
        ship(b, 2573, 2838, 12, "minecraft:dark_oak_planks", "minecraft:white_wool");
        lamp(b, 2566, 2866, "minecraft:spruce_fence");
        lamp(b, 2574, 2866, "minecraft:spruce_fence");
        return b.build();
    }

    /** The Feldip Hunter area: kebbit deadfalls, larupia and graahk pits, the expert's hut, fairy ring AKS. */
    static StructureDef feldipHunterArea() {
        Builder b = StructureDef.builder("isl_feldip_hunter_area", "Feldip Hunter area").replaces(IslandsPlaces.FELDIP_HUNTER_AREA);
        b.tFlatten(2536, 2894, 2576, 2930, GRASS);
        b.tFlatten(2566, 2951, 2576, 2961, GRASS);
        hut(b, 2540, 2914, 4, "minecraft:stripped_spruce_log", "minecraft:hay_block", SPRUCE, "east");
        b.tSet(2560, 2920, 0, rc("barb_tailed_kebbit_deadfall"));
        b.tSet(2566, 2924, 0, rc("barb_tailed_kebbit_deadfall"));
        b.tSet(2548, 2900, 0, rc("spined_larupia_pitfall"));
        b.tSet(2568, 2900, 0, rc("horned_graahk_pitfall"));
        b.tFloor(2546, 2898, 2550, 2902, "minecraft:coarse_dirt");
        b.tFloor(2566, 2898, 2570, 2902, "minecraft:coarse_dirt");
        String leaves = "minecraft:jungle_leaves[persistent=true]";
        for (int[] t : new int[][]{{2538, 2896}, {2556, 2896}, {2574, 2908}, {2538, 2928}, {2552, 2926}, {2572, 2928},
                {2558, 2910}}) {
            tree(b, t[0], t[1], JUNGLE_LOG, leaves, 7);
        }
        b.tFill(2552, 2915, 2556, 2917, 0, 0, "minecraft:fern");
        fairyRing(b, IslandsStructures.FELDIP_RING);
        return b.build();
    }

    /** Zul-Andra: the snakeling priests' village, totems, the sacrificial boat jetty and fairy ring DLR. */
    static StructureDef zulAndra() {
        Builder b = StructureDef.builder("isl_zul_andra", "Zul-Andra").replaces(IslandsPlaces.ZUL_ANDRA);
        b.tFlatten(2184, 3030, 2203, 3045, MUD);
        b.tFlatten(2184, 3063, 2203, 3082, MUD);
        b.tFlatten(2204, 3067, 2216, 3082, MUD);
        b.tFlatten(2208, 3094, 2218, 3104, "minecraft:mycelium");
        String hutWall = "minecraft:mud_bricks";
        String thatch = "minecraft:moss_block";
        for (int[] h : new int[][]{{2188, 3034}, {2198, 3034}, {2188, 3042}, {2190, 3068}, {2199, 3074}, {2190, 3078},
                {2210, 3076}}) {
            hut(b, h[0], h[1], 4, hutWall, thatch, PACKED_MUD, "east");
        }
        // The shrine of the priestess and the serpent totems.
        b.tFill(2196, 3040, 2202, 3044, -1, -1, "minecraft:mossy_stone_bricks");
        b.tFill(2199, 3044, 2199, 3044, 0, 2, "minecraft:mossy_stone_bricks");
        b.tSet(2199, 3044, 3, "minecraft:green_glazed_terracotta");
        for (int[] t : new int[][]{{2195, 3065}, {2203, 3070}, {2214, 3069}, {2186, 3031}}) {
            b.tFill(t[0], t[1], t[0], t[1], 0, 3, "minecraft:stripped_jungle_log");
            b.tSet(t[0], t[1], 4, "minecraft:green_glazed_terracotta");
        }
        // The jetty of the sacrificial boat, out into the swamp.
        pier(b, 2217, 3070, 2224, 3072, "minecraft:mangrove_planks");
        b.tFill(2225, 3070, 2228, 3072, -1, -1, "minecraft:dark_oak_planks");
        b.tFill(2184, 3063, 2203, 3064, -1, -1, "minecraft:water");
        b.tSet(2190, 3064, 0, "minecraft:lily_pad");
        b.tSet(2198, 3063, 0, "minecraft:lily_pad");
        fairyRing(b, IslandsStructures.ZUL_ANDRA_RING);
        return b.build();
    }

    /** Port Tyras: the harbour with its jetty and ship, the warehouse, guardhouse and store. */
    static StructureDef portTyras() {
        Builder b = StructureDef.builder("isl_port_tyras", "Port Tyras").replaces(IslandsPlaces.PORT_TYRAS);
        b.tFlatten(2138, 3108, 2161, 3134, GRASS);
        b.tFloor(2138, 3117, 2161, 3119, PATH);
        pier(b, 2126, 3117, 2137, 3119, SPRUCE);
        ship(b, 2124, 3123, 10, "minecraft:spruce_planks", "minecraft:red_wool");
        building(b, 2142, 3122, 2152, 3130, 1, "minecraft:spruce_planks", SPRUCE, "minecraft:dark_oak", "south", true);
        b.tSet(2146, 3128, 0, "minecraft:barrel");
        b.tSet(2148, 3128, 0, "minecraft:barrel");
        b.tSet(2150, 3126, 0, rc("general_store"));
        building(b, 2142, 3109, 2150, 3115, 1, STONE, SPRUCE, "minecraft:deepslate_tile", "north", true);
        b.tSet(2148, 3112, 0, rc("cooking_range"));
        b.tTower(2156, 3112, 3, 10, STONE, SPRUCE);
        b.tFill(2156, 3112, 2156, 3112, 10, 13, "minecraft:spruce_fence");
        b.tSet(2156, 3112, 14, "minecraft:red_wool");
        b.tFill(2160, 3108, 2160, 3134, 0, 1, "minecraft:spruce_fence");
        b.tClear(2160, 3117, 2160, 3119, 0, 1);
        return b.build();
    }

    /** Braindeath Island: the rum distillery, zombie pirates' shacks and the landing. */
    static StructureDef braindeathIsland() {
        Builder b = StructureDef.builder("isl_braindeath_island", "Braindeath Island").replaces(IslandsPlaces.BRAINDEATH_ISLAND);
        b.tFlatten(2124, 3141, 2152, 3164, SAND);
        building(b, 2132, 3152, 2146, 3162, 2, "minecraft:dark_oak_planks", SPRUCE, "minecraft:dark_oak", "south", true);
        for (int x = 2134; x <= 2144; x += 3) {
            b.tSet(x, 3160, 0, "minecraft:barrel");
            b.tSet(x, 3160, 1, "minecraft:barrel");
        }
        b.tSet(2136, 3155, 0, "minecraft:cauldron");
        b.tSet(2142, 3155, 0, "minecraft:brewing_stand");
        b.tSet(2139, 3157, 0, "minecraft:copper_block");
        b.tSet(2139, 3157, 1, "minecraft:lightning_rod");
        hut(b, 2128, 3146, 4, "minecraft:stripped_dark_oak_log", "minecraft:black_wool", OAK, "east");
        hut(b, 2148, 3146, 4, "minecraft:stripped_dark_oak_log", "minecraft:black_wool", OAK, "west");
        pier(b, 2136, 3134, 2139, 3140, "minecraft:dark_oak_planks");
        b.tFill(2124, 3164, 2124, 3164, 0, 5, "minecraft:dark_oak_fence");
        b.tSet(2124, 3164, 6, "minecraft:black_wool");
        for (int[] p : new int[][]{{2126, 3160}, {2150, 3160}, {2138, 3144}}) {
            palm(b, p[0], p[1], 5);
        }
        return b.build();
    }

    /** Isafdar's dense forest: leaf-trap pits, an elven lookout platform and crystal waymarkers. */
    static StructureDef isafdar() {
        Builder b = StructureDef.builder("isl_isafdar_forest", "Isafdar dense forest").replaces(IslandsPlaces.ISAFDAR);
        b.tFlatten(2226, 3160, 2258, 3190, PODZOL);
        String leaves = "minecraft:dark_oak_leaves[persistent=true]";
        for (int x = 2228; x <= 2256; x += 5) {
            for (int y = 3162; y <= 3188; y += 5) {
                if (Math.abs(x - 2242) <= 3 && Math.abs(y - 3174) <= 3) {
                    continue;
                }
                int jx = (x * 7 + y * 3) % 3 - 1;
                int jy = (x * 3 + y * 5) % 3 - 1;
                tree(b, x + jx, y + jy, "minecraft:dark_oak_log", leaves, 8 + (x + y) % 3);
            }
        }
        // Leaf traps: pits hidden under leaves.
        for (int[] p : new int[][]{{2236, 3170}, {2248, 3180}, {2232, 3184}}) {
            b.tFill(p[0], p[1], p[0], p[1], -3, -1, "minecraft:air");
            b.tSetTile(p[0], p[1], -1, "minecraft:azalea_leaves[persistent=true]");
        }
        // The elven lookout: a platform in the trees with a ladder.
        b.tFill(2241, 3173, 2243, 3175, 6, 6, "minecraft:spruce_planks");
        b.tFill(2241, 3173, 2241, 3173, 0, 5, "minecraft:spruce_log");
        b.tFill(2243, 3174, 2243, 3174, 0, 5, "minecraft:spruce_log");
        for (int y = 0; y <= 6; y++) {
            b.tSet(2244, 3174, y, "minecraft:ladder[facing=east]");
        }
        b.tSet(2242, 3174, 7, "minecraft:lantern");
        for (int[] c : new int[][]{{2230, 3160}, {2256, 3190}}) {
            b.tSet(c[0], c[1], 0, "minecraft:amethyst_block");
            b.tSet(c[0], c[1], 1, "minecraft:amethyst_cluster");
        }
        return b.build();
    }
}
