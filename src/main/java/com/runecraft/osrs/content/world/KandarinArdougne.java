package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

/** East and West Ardougne with the castle, zoo, monastery, Clock Tower, Tower of Life, Witchaven and the Fishing Platform. */
final class KandarinArdougne {
    private KandarinArdougne() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(eastArdougne());
        out.add(ardougneCastle());
        out.add(ardougneChests());
        out.add(ardougneZoo());
        out.add(westArdougne());
        out.add(clockTower());
        out.add(towerOfLife());
        out.add(monastery());
        out.add(witchaven());
        out.add(fishingPlatform());
    }

    private static final String ROAD = "minecraft:andesite";

    // ------------------------------------------------------------------ East Ardougne

    static StructureDef eastArdougne() {
        KandarinKit k = new KandarinKit("east_ardougne", "East Ardougne").replaces("ARDOUGNE");
        k.flatten(2556, 3254, 2694, 3342);
        k.road(2594, 3305, 2650, 3306, ROAD, COBBLE);
        k.road(2674, 3305, 2694, 3306, ROAD, COBBLE);
        k.road(2661, 3256, 2662, 3292, ROAD, COBBLE);
        k.road(2661, 3312, 2662, 3342, ROAD, COBBLE);
        k.road(2597, 3297, 2600, 3298, ROAD, COBBLE);
        k.road(2599, 3297, 2600, 3306, ROAD, COBBLE);
        k.road(2617, 3307, 2618, 3329, ROAD, COBBLE);
        k.road(2653, 3288, 2654, 3292, ROAD, null);
        k.road(2610, 3283, 2611, 3304, ROAD, null);
        k.road(2580, 3307, 2598, 3308, ROAD, null);
        k.road(2573, 3307, 2574, 3317, ROAD, null);
        k.road(2570, 3276, 2571, 3281, ROAD, null);

        // --- Market square with stalls and fountain ---
        k.road(2651, 3293, 2673, 3311, SB, ANDESITE);
        int[][] stalls = {{2654, 3309}, {2670, 3309}, {2662, 3310}, {2653, 3301}, {2654, 3295}, {2670, 3295}, {2662, 3294}};
        String[] ids = {"cake", "cake", "silk", "silver", "spice", "gem", "fur"};
        String[] wool = {"brown", "brown", "purple", "light_gray", "orange", "lime", "gray"};
        for (int i = 0; i < stalls.length; i++) {
            k.stall(KandarinKit.X(stalls[i][0]) + 1, KandarinKit.Z(stalls[i][1]) + 1, "runecraft:" + ids[i] + "_stall", wool[i], "spruce");
        }
        int fx = KandarinKit.X(2659) + 1;
        int fz = KandarinKit.Z(3302) + 1;
        k.disc(fx, fz, -2, -1, 3, SB);
        k.ring(fx, fz, 0, 0, 3, "minecraft:stone_brick_wall");
        k.disc(fx, fz, -1, -1, 2, WATER);
        k.fill(fx, -1, fz, fx, 2, fz, "minecraft:chiseled_stone_bricks");
        k.set(fx, 3, fz, "minecraft:lantern");
        for (int[] c : new int[][]{{2651, 3293}, {2673, 3293}, {2651, 3311}, {2673, 3311}}) {
            k.lamp(KandarinKit.X(c[0]), KandarinKit.Z(c[1]), "minecraft:dark_oak_fence");
        }
        marketDetails(k, fx, fz);

        // --- Rooftop course houses around the market (flat roofs) ---
        Pal flat = ARDY.roof("flat:minecraft:smooth_stone");
        Bld r0 = k.house(2676, 3290, 2682, 3296, 2, flat, 'w', FURN | FLAT);
        Bld r1 = k.house(2676, 3308, 2682, 3314, 2, flat, 'w', FURN | FLAT);
        Bld r2 = k.house(2665, 3314, 2671, 3320, 2, flat, 's', FURN | FLAT);
        Bld r3 = k.house(2653, 3314, 2659, 3320, 2, flat, 's', FURN | FLAT);
        Bld r4 = k.house(2643, 3313, 2649, 3319, 2, flat, 'e', FURN | FLAT);
        Bld r5 = k.house(2643, 3296, 2649, 3302, 2, flat, 'e', FURN | FLAT);
        ardougneCourse(k, r0, r1, r2, r3, r4, r5);

        // --- Banks ---
        Pal bankPal = ARDY.roof("minecraft:deepslate_tile");
        Bld sb = k.house(2649, 3280, 2658, 3287, 1, bankPal, 'n', NO_LADDER | WIDE_DOOR);
        int sbz = sb.z2() - 3;
        k.fill(sb.x1() + 1, 0, sbz, sb.x2() - 1, 0, sbz, "minecraft:dark_oak_slab[type=top]");
        for (int x = sb.x1() + 2; x <= sb.x2() - 2; x += 3) {
            k.set(x, 0, sbz, BANK);
        }
        k.fill(sb.doorX(), 0, sb.z1() + 1, sb.doorX() + 1, 0, sbz - 1, "minecraft:red_carpet");
        k.set(sb.cx(), 2, sb.cz(), "minecraft:lantern[hanging=true]");
        Bld nb = k.house(2612, 3330, 2621, 3336, 1, bankPal, 's', NO_LADDER | WIDE_DOOR);
        int nbz = nb.z1() + 3;
        k.fill(nb.x1() + 1, 0, nbz, nb.x2() - 1, 0, nbz, "minecraft:dark_oak_slab[type=top]");
        for (int x = nb.x1() + 2; x <= nb.x2() - 2; x += 3) {
            k.set(x, 0, nbz, BANK);
        }
        k.fill(nb.doorX(), 0, nbz + 1, nb.doorX() + 1, 0, nb.z2() - 1, "minecraft:red_carpet");
        k.set(nb.cx(), 2, nb.cz(), "minecraft:lantern[hanging=true]");

        // --- Church ---
        Pal churchPal = ARDY.floor(ANDESITE).roof("minecraft:deepslate_tile").post("minecraft:chiseled_stone_bricks");
        Bld ch = k.house(2604, 3309, 2614, 3320, 1, churchPal, 's', NO_LADDER | NO_CEILING);
        KandarinSeers.churchInterior(k, ch);
        k.belfry(ch.x1(), ch.z2(), 14, SB, "minecraft:deepslate_tile");

        // --- Shops ---
        Bld gen = k.house(2614, 3290, 2620, 3296, 1, ARDY, 'n', NO_LADDER);
        k.counter(gen, GENERAL, "dark_oak");
        Bld zen = k.house(2632, 3290, 2637, 3296, 1, ARDY_RED, 'n', NO_LADDER);
        k.counter(zen, WEAPONS, "oak");
        k.set(zen.x1() + 1, 0, zen.z1() + 1, ANVIL);
        Bld aem = k.house(2632, 3309, 2638, 3314, 1, ARDY, 's', NO_LADDER);
        k.counter(aem, TOOLS, "dark_oak");

        // --- Handelmort Mansion ---
        k.tWalls(2622, 3317, 2642, 3332, 0, 1, "minecraft:iron_bars");
        k.tFill(2622, 3317, 2642, 3317, -1, 0, SB);
        k.tClear(2631, 3317, 2632, 3317, 0, 1);
        Bld hm = k.house(2624, 3320, 2640, 3330, 2, ARDY.roof("minecraft:deepslate_tile"), 's', FURN | WIDE_DOOR);
        k.longTable(hm.x1() + 4, hm.x2() - 6, 0, hm.cz() + 1, "dark_oak");
        k.oak(KandarinKit.X(2623) + 1, KandarinKit.Z(3318), 5);
        k.oak(KandarinKit.X(2641), KandarinKit.Z(3318), 5);

        // --- Flying Horse Inn ---
        Bld inn = k.house(2568, 3318, 2579, 3327, 2, ARDY.roof("minecraft:spruce"), 's', FURN | WIDE_DOOR);
        k.fill(inn.x1() + 1, 0, inn.z1() + 3, inn.x2() - 5, 0, inn.z1() + 3, "minecraft:spruce_slab[type=top]");
        k.fill(inn.x1() + 1, 0, inn.z1() + 1, inn.x2() - 5, 1, inn.z1() + 1, "minecraft:barrel[facing=south]");
        k.set(inn.x1() + 2, 0, inn.z1() + 2, RANGE);
        k.flag(inn.x2() + 1, 0, inn.z2() + 1, "white", "brown");

        // --- Carnillean house ---
        Bld car = k.house(2563, 3262, 2577, 3275, 2, ARDY.roof("minecraft:deepslate_tile"), 'n', FURN | WIDE_DOOR);
        k.fill(car.x1() + 2, 0, car.z1() + 2, car.x1() + 2, 1, car.z2() - 2, "minecraft:bookshelf");
        k.tFill(2562, 3259, 2578, 3259, 0, 1, "minecraft:oak_leaves[persistent=true]");

        // --- Herb patch garden ---
        k.fence(2643, 3332, 2649, 3337, "minecraft:oak_fence");
        k.tClear(2646, 3332, 2646, 3332, 0, 0);
        k.floor(2644, 3333, 2648, 3336, "minecraft:farmland[moisture=7]");
        k.tFill(2645, 3334, 2645, 3334, 0, 0, "runecraft:herb_patch");
        k.tFill(2647, 3335, 2647, 3335, 0, 0, "minecraft:wheat[age=7]");
        k.at(2644, 3333, -1, WATER);

        // --- Town houses ---
        int[][] houses = {
                {2600, 3290, 2606, 3296, 'n'}, {2622, 3290, 2628, 3296, 'n'}, {2640, 3283, 2646, 3289, 'n'},
                {2664, 3283, 2670, 3289, 'n'}, {2676, 3280, 2682, 3286, 'n'}, {2685, 3292, 2691, 3298, 'w'},
                {2685, 3310, 2691, 3316, 'w'}, {2652, 3323, 2658, 3329, 's'}, {2664, 3324, 2670, 3330, 's'},
                {2676, 3320, 2682, 3326, 's'}, {2644, 3322, 2649, 3328, 's'}, {2600, 3322, 2606, 3328, 's'},
                {2624, 3335, 2630, 3340, 's'}, {2584, 3320, 2590, 3326, 's'}, {2586, 3332, 2592, 3338, 's'},
                {2600, 3333, 2607, 3339, 's'}, {2664, 3262, 2670, 3268, 'n'}, {2676, 3262, 2682, 3268, 'n'},
                {2685, 3275, 2691, 3281, 'w'}, {2638, 3262, 2644, 3268, 'n'}, {2650, 3262, 2656, 3268, 'n'},
                {2676, 3332, 2682, 3338, 's'}, {2652, 3333, 2658, 3339, 's'}, {2636, 3298, 2641, 3302, 'n'}};
        for (int i = 0; i < houses.length; i++) {
            int[] h = houses[i];
            Pal p = (i % 3 == 0) ? ARDY_RED : (i % 3 == 1 ? ARDY : ARDY.roof("minecraft:deepslate_tile"));
            k.house(h[0], h[1], h[2], h[3], 1 + (i % 4 == 3 ? 0 : 1), p, (char) h[4], FURN);
        }
        // Trees and lamps along the main road
        for (int x = 2604; x <= 2646; x += 7) {
            k.lamp(KandarinKit.X(x), KandarinKit.Z(3304), "minecraft:dark_oak_fence");
        }
        for (int[] t : new int[][]{{2630, 3302}, {2604, 3300}, {2686, 3302}, {2690, 3286}, {2632, 3279}, {2660, 3276}, {2680, 3272}}) {
            k.oak(KandarinKit.X(t[0]), KandarinKit.Z(t[1]), 6);
        }
        return k.build();
    }

    /**
     * The Ardougne market's furnishings: a second fountain tier, benches round the basin, flower
     * tubs, crate stacks behind the stalls, a notice board and a paved edge kerb.
     */
    private static void marketDetails(KandarinKit k, int fx, int fz) {
        int g = k.g;
        // an upper basin round the fountain column
        k.d.air(fx - 1, g + 2, fz - 1, fx + 1, g + 2, fz + 1, "minecraft:stone_brick_slab[type=top]");
        // benches facing the fountain on all four sides
        k.d.bench(fx - 1, g, fz - 6, 'e', 3, 's', "dark_oak");
        k.d.bench(fx - 1, g, fz + 6, 'e', 3, 'n', "dark_oak");
        k.d.bench(fx - 6, g, fz - 1, 's', 3, 'e', "dark_oak");
        k.d.bench(fx + 6, g, fz - 1, 's', 3, 'w', "dark_oak");
        // flower tubs at the diagonals
        int i = 0;
        for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
            k.d.planter(fx + p[0], g, fz + p[1], i++);
        }
        // the square's kerb, crates behind the stalls and a notice board
        int x1 = KandarinKit.X(2651);
        int x2 = KandarinKit.X(2673) + 1;
        int z1 = KandarinKit.Z(3311);
        int z2 = KandarinKit.Z(3293) + 1;
        k.d.replace(x1, g - 1, z1, x2, g - 1, z1, SB, "minecraft:polished_andesite");
        k.d.replace(x1, g - 1, z2, x2, g - 1, z2, SB, "minecraft:polished_andesite");
        k.d.replace(x1, g - 1, z1, x1, g - 1, z2, SB, "minecraft:polished_andesite");
        k.d.replace(x2, g - 1, z1, x2, g - 1, z2, SB, "minecraft:polished_andesite");
        k.d.crates(KandarinKit.X(2657), g, KandarinKit.Z(3311));
        k.d.crates(KandarinKit.X(2666), g, KandarinKit.Z(3294));
        k.d.crates(KandarinKit.X(2651) + 1, g, KandarinKit.Z(3305));
        k.d.signpost(KandarinKit.X(2662), g, KandarinKit.Z(3299), "dark_oak");
    }

    /** Ardougne rooftop course (7 obstacles) over the houses around the market. */
    private static void ardougneCourse(KandarinKit k, Bld r0, Bld r1, Bld r2, Bld r3, Bld r4, Bld r5) {
        int c = 9;
        int top = 8;
        // 0: climb the west wall of the first house
        int lz = r0.z1() + 2;
        for (int dz = 0; dz < 5; dz++) {
            if (r0.z1() + 2 + dz == r0.doorZ()) {
                continue;
            }
            if (k.ladder(r0.x1() - 1, r0.z1() + 2 + dz, 0, top, "west")) {
                lz = r0.z1() + 2 + dz;
                break;
            }
        }
        k.set(r0.x1(), top + 1, lz, AIR);
        k.set(r0.x1() + 1, top, lz, obstacle(c, 0));
        // 1: tightrope north over the east road
        int rx = KandarinKit.X(2679);
        k.set(rx, top + 1, r0.z1(), AIR);
        k.set(rx, top + 1, r1.z2(), AIR);
        k.fill(rx, top, r1.z2() + 1, rx, top, r0.z1() - 1, "minecraft:spruce_slab[type=top]");
        k.set(rx, top, r1.z2() - 1, obstacle(c, 1));
        // 2: plank west to the north row
        bridgeX(k, r2.x2(), r1.x1(), KandarinKit.Z(3314), top);
        k.set(r2.x2() - 1, top, KandarinKit.Z(3314), obstacle(c, 2));
        // 3: jump west
        bridgeX(k, r3.x2(), r2.x1(), KandarinKit.Z(3317), top);
        k.set(r3.x2() - 1, top, KandarinKit.Z(3317), obstacle(c, 3));
        // 4: jump west again
        bridgeX(k, r4.x2(), r3.x1(), KandarinKit.Z(3316), top);
        k.set(r4.x2() - 1, top, KandarinKit.Z(3316), obstacle(c, 4));
        // 5: tightrope south over the west road
        int sx = KandarinKit.X(2646);
        k.set(sx, top + 1, r4.z2(), AIR);
        k.set(sx, top + 1, r5.z1(), AIR);
        k.fill(sx, top, r4.z2() + 1, sx, top, r5.z1() - 1, "minecraft:spruce_slab[type=top]");
        k.set(sx, top, r5.z1() + 1, obstacle(c, 5));
        // 6: jump down onto the hay
        k.set(sx, top + 1, r5.z2(), AIR);
        int gz = r5.z2() + 4;
        k.fill(sx - 1, -1, gz - 1, sx + 1, -1, gz + 1, HAY);
        k.set(sx, -1, gz, obstacle(c, 6));
    }

    /** Plank bridge at roof height between a west roof edge (wx) and an east roof edge (ex), with a one-block gap. */
    static void bridgeX(KandarinKit k, int wx, int ex, int z, int top) {
        k.set(wx, top + 1, z, AIR);
        k.set(ex, top + 1, z, AIR);
        k.fill(wx + 1, top, z, ex - 1, top, z, "minecraft:spruce_planks");
        k.set((wx + ex) / 2, top, z, AIR);
    }

    // ------------------------------------------------------------------ Ardougne Castle

    static StructureDef ardougneCastle() {
        KandarinKit k = new KandarinKit("ardougne_castle", "Ardougne Castle").replaces("ardougne_castle");
        String w = SB;
        k.water(2564, 3282, 2596, 3314, 2);
        k.tFill(2566, 3284, 2594, 3312, -4, -2, "minecraft:dirt");
        k.floor(2566, 3284, 2594, 3312, GRASS);
        k.cwallRect(2566, 3284, 2594, 3312, 10, w);
        KandarinSeers.weather(k, KandarinKit.X(2566), KandarinKit.Z(3312), KandarinKit.X(2594) + 1, KandarinKit.Z(3284) + 1, 0, 9);
        k.gate(2594, 3297, 2594, 3298, 5, COBBLE);
        k.pier(2595, 3297, 2596, 3298, "spruce", false);
        k.fill(KandarinKit.X(2594), 4, KandarinKit.Z(3298), KandarinKit.X(2594) + 1, 4, KandarinKit.Z(3297) + 1, "minecraft:iron_bars");
        k.tower(2594, 3295, 3, 13, w, "minecraft:spruce_planks", null, 'w');
        k.tower(2594, 3300, 3, 13, w, "minecraft:spruce_planks", null, 'w');
        int[][] corners = {{2566, 3284, 1, -1}, {2594, 3284, -1, -1}, {2566, 3312, 1, 1}, {2594, 3312, -1, 1}};
        for (int[] c : corners) {
            k.tower(c[0], c[1], 5, 16, w, "minecraft:spruce_planks", null, ' ');
            k.towerDiagDoor(KandarinKit.X(c[0]) + 1, KandarinKit.Z(c[1]) + 1, 5, c[2], c[3]);
            k.flag(KandarinKit.X(c[0]) + 1, 16, KandarinKit.Z(c[1]) + 1, "white", "black");
        }
        k.road(2581, 3297, 2593, 3298, COBBLE, GRAVEL);
        // Keep
        Pal keepPal = new Pal(w, ANDESITE, "minecraft:dark_oak_planks", "flat:minecraft:stone_bricks", "dark_oak", PANE);
        Bld keep = k.house(2569, 3290, 2580, 3306, 3, keepPal, 'e', FLAT | NO_LADDER | WIDE_DOOR);
        int ty = keep.cz();
        k.fill(keep.x1() + 1, 0, ty - 2, keep.x1() + 3, 0, ty + 2, "minecraft:polished_andesite");
        k.set(keep.x1() + 2, 1, ty, "minecraft:dark_oak_stairs[facing=west]");
        k.set(keep.x1() + 1, 2, ty, "minecraft:gold_block");
        k.fill(keep.x1() + 4, 0, ty, keep.x2() - 1, 0, ty + 1, "minecraft:purple_carpet");
        for (int z = keep.z1() + 3; z <= keep.z2() - 3; z += 5) {
            k.fill(keep.x1() + 1, 1, z, keep.x1() + 1, 2, z, "minecraft:purple_wool");
            k.fill(keep.x2() - 1, 1, z, keep.x2() - 1, 2, z, "minecraft:purple_wool");
            k.set(keep.cx(), 2, z, "minecraft:lantern[hanging=true]");
        }
        k.stairs(keep.x2() - 2, 0, keep.z1() + 2, 's', 4, "minecraft:stone_brick");
        k.stairs(keep.x2() - 2, 4, keep.z1() + 7, 's', 4, "minecraft:stone_brick");
        k.stairs(keep.x2() - 2, 8, keep.z1() + 12, 's', 5, "minecraft:stone_brick");
        // Treasury with the castle chest
        k.set(keep.x1() + 1, 4, keep.z1() + 1, "runecraft:ardougne_castle_chest");
        k.fill(keep.x1() + 2, 4, keep.z1() + 1, keep.x1() + 4, 4, keep.z1() + 1, "minecraft:chest[facing=south]");
        k.walls(keep.x1(), 4, keep.z1(), keep.x1() + 6, 6, keep.z1() + 5, w);
        k.clear(keep.x1() + 6, 4, keep.z1() + 3, keep.x1() + 6, 5, keep.z1() + 3);
        for (int z = keep.z1() + 7; z <= keep.z2() - 3; z += 3) {
            k.bed(keep.x1() + 1, 4, z, "purple");
        }
        k.longTable(keep.x1() + 4, keep.x2() - 4, 8, keep.cz(), "dark_oak");
        k.set(keep.cx(), 10, keep.cz() - 3, "minecraft:lantern[hanging=true]");
        k.tower(2569, 3306, 4, 20, w, "minecraft:spruce_planks", "minecraft:deepslate_tiles", 'e');
        k.tower(2569, 3290, 4, 20, w, "minecraft:spruce_planks", "minecraft:deepslate_tiles", 'e');
        // Barracks and stables
        Bld bar = k.house(2583, 3303, 2591, 3309, 1, STONE.roof("minecraft:spruce"), 's', FURN | NO_LADDER);
        for (int x = bar.x1() + 1; x <= bar.x2() - 1; x += 3) {
            k.bed(x, 0, bar.z1() + 1, "purple");
        }
        Bld stab = k.house(2583, 3286, 2591, 3290, 1, STONE.roof("minecraft:spruce"), 'n', NO_LADDER | NO_WINDOWS);
        for (int x = stab.x1() + 2; x <= stab.x2() - 1; x += 3) {
            k.fill(x, 0, stab.z1() + 1, x, 0, stab.z2() - 2, "minecraft:spruce_fence");
            k.set(x + 1, 0, stab.z1() + 1, HAY);
        }
        // Archery butts
        for (int i = 0; i < 3; i++) {
            k.set(KandarinKit.X(2592), 0, KandarinKit.Z(3294) + i * 3, HAY);
            k.set(KandarinKit.X(2592), 1, KandarinKit.Z(3294) + i * 3, "minecraft:target");
        }
        k.well(KandarinKit.X(2583), KandarinKit.Z(3299));
        return k.build();
    }

    /** The 10-coin, nature rune and 50-coin chests on the upper floor of the first rooftop-course house. */
    static StructureDef ardougneChests() {
        KandarinKit k = new KandarinKit("ardougne_chest_house", "Ardougne chest house").replaces("ardougne_chests");
        int x1 = KandarinKit.X(2676);
        int x2 = KandarinKit.X(2682) + 1;
        int z1 = KandarinKit.Z(3296);
        int z2 = KandarinKit.Z(3290) + 1;
        int cz = (z1 + z2) / 2;
        k.set(x1 + 1, 4, cz + 1, "runecraft:ten_coin_chest");
        k.set(x2 - 1, 4, cz, "runecraft:nature_rune_chest");
        k.set((x1 + x2) / 2 + 1, 4, z2 - 1, "runecraft:fifty_coin_chest");
        return k.build();
    }

    // ------------------------------------------------------------------ Ardougne Zoo

    static StructureDef ardougneZoo() {
        KandarinKit k = new KandarinKit("ardougne_zoo", "Ardougne Zoo").replaces("ardougne_zoo");
        String fence = "minecraft:oak_fence";
        k.tWalls(2596, 3258, 2632, 3282, 0, 1, fence);
        k.tClear(2610, 3282, 2611, 3282, 0, 1);
        k.road(2610, 3259, 2611, 3281, GRAVEL, PATH);
        k.road(2597, 3270, 2631, 3271, GRAVEL, PATH);
        k.tFill(2609, 3282, 2609, 3282, 0, 3, "minecraft:stripped_oak_log");
        k.tFill(2612, 3282, 2612, 3282, 0, 3, "minecraft:stripped_oak_log");
        k.tFill(2609, 3282, 2612, 3282, 4, 4, "minecraft:oak_slab");
        // Penguins
        k.floor(2598, 3273, 2607, 3280, "minecraft:snow_block");
        k.water(2600, 3275, 2604, 3278, 2);
        k.tFill(2598, 3273, 2599, 3274, 0, 0, "minecraft:packed_ice");
        k.tWalls(2598, 3273, 2607, 3280, 0, 1, "minecraft:spruce_fence");
        // Monkeys
        k.floor(2614, 3273, 2630, 3280, "minecraft:moss_block");
        k.tWalls(2614, 3273, 2630, 3280, 0, 2, "minecraft:iron_bars");
        for (int x = 2616; x <= 2628; x += 5) {
            k.bigTree(KandarinKit.X(x), KandarinKit.Z(3277), 7, 2, "minecraft:jungle_log", "minecraft:jungle_leaves");
        }
        k.tFill(2620, 3275, 2621, 3275, 0, 3, "minecraft:bamboo");
        // Bears
        k.floor(2598, 3260, 2607, 3268, "minecraft:podzol");
        k.tWalls(2598, 3260, 2607, 3268, 0, 1, "minecraft:spruce_fence");
        k.spruce(KandarinKit.X(2600), KandarinKit.Z(3266), 8);
        k.blob(KandarinKit.X(2605), 0, KandarinKit.Z(3262), 3, 2, 3, "minecraft:mossy_cobblestone", false);
        k.clear(KandarinKit.X(2605) - 1, 0, KandarinKit.Z(3262), KandarinKit.X(2605), 1, KandarinKit.Z(3262) + 3);
        // Camels
        k.floor(2614, 3260, 2621, 3268, SAND);
        k.tWalls(2614, 3260, 2621, 3268, 0, 1, "minecraft:birch_fence");
        k.at(2616, 3262, 0, "minecraft:cactus");
        k.at(2619, 3265, 0, "minecraft:dead_bush");
        k.at(2617, 3266, 0, "minecraft:cactus");
        // Birds and unicorns
        k.tWalls(2623, 3260, 2630, 3268, 0, 1, "minecraft:dark_oak_fence");
        int bx = KandarinKit.X(2627);
        int bz = KandarinKit.Z(3264);
        k.ring(bx, bz, 0, 4, 3, "minecraft:iron_bars");
        k.disc(bx, bz, 5, 5, 3, "minecraft:iron_bars");
        k.fill(bx, 0, bz, bx, 3, bz, "minecraft:oak_fence");
        k.meadow(KandarinKit.X(2623) + 1, KandarinKit.Z(3268) + 1, KandarinKit.X(2630), KandarinKit.Z(3260), 4);
        for (int x = 2600; x <= 2628; x += 7) {
            k.lamp(KandarinKit.X(x), KandarinKit.Z(3272) + 1, "minecraft:dark_oak_fence");
        }
        k.set(KandarinKit.X(2608), 0, KandarinKit.Z(3272), "minecraft:oak_stairs[facing=north]");
        k.set(KandarinKit.X(2613), 0, KandarinKit.Z(3272), "minecraft:oak_stairs[facing=north]");
        return k.build();
    }

    // ------------------------------------------------------------------ West Ardougne

    static StructureDef westArdougne() {
        KandarinKit k = new KandarinKit("west_ardougne", "West Ardougne").replaces("west_ardougne");
        k.flatten(2462, 3264, 2555, 3342, "minecraft:coarse_dirt");
        k.cwallRect(2463, 3265, 2556, 3341, 9, SB);
        KandarinSeers.weather(k, KandarinKit.X(2463), KandarinKit.Z(3341), KandarinKit.X(2556) + 1, KandarinKit.Z(3265) + 1, 0, 8);
        k.gate(2556, 3302, 2556, 3303, 4, COBBLE);
        k.road(2464, 3302, 2555, 3303, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2464, 3281, 2555, 3282, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2464, 3323, 2555, 3324, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2508, 3266, 2509, 3340, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2530, 3266, 2531, 3340, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2486, 3266, 2487, 3340, COBBLE, "minecraft:mossy_cobblestone");
        // Central square
        k.road(2524, 3297, 2537, 3308, "minecraft:stone_bricks", "minecraft:cracked_stone_bricks");
        k.well(KandarinKit.X(2527), KandarinKit.Z(3306));
        // Mourner headquarters
        Pal hq = PLAGUE.roof("minecraft:deepslate_tile");
        Bld m = k.house(2545, 3326, 2552, 3335, 2, hq, 'w', FURN);
        k.set(m.x2() - 1, 0, m.z1() + 1, "minecraft:brewing_stand");
        k.set(m.x2() - 1, 0, m.z1() + 2, "minecraft:cauldron");
        // Plague church
        Pal ch = PLAGUE.roof("minecraft:stone_brick").floor(ANDESITE);
        Bld church = k.house(2514, 3285, 2524, 3296, 1, ch, 'n', NO_LADDER | NO_CEILING);
        k.set(church.cx(), 0, church.z2() - 1, ALTAR);
        for (int z = church.z1() + 3; z <= church.z2() - 3; z += 2) {
            k.fill(church.x1() + 1, 0, z, church.doorX() - 1, 0, z, "minecraft:spruce_stairs[facing=north]");
            k.fill(church.doorX() + 1, 0, z, church.x2() - 1, 0, z, "minecraft:spruce_stairs[facing=north]");
        }
        // House blocks between the streets
        int[][] xr = {{2465, 2485}, {2488, 2507}, {2510, 2529}, {2532, 2554}};
        int[][] yr = {{3267, 3280}, {3283, 3301}, {3304, 3322}, {3325, 3339}};
        int n = 0;
        for (int[] xs : xr) {
            for (int[] ys : yr) {
                for (int x = xs[0]; x + 5 <= xs[1]; x += 7) {
                    for (int y = ys[0]; y + 5 <= ys[1]; y += 7) {
                        n++;
                        if (overlaps(x, y, x + 5, y + 5, 2513, 3284, 2525, 3297) || overlaps(x, y, x + 5, y + 5, 2523, 3296, 2538, 3309)
                                || overlaps(x, y, x + 5, y + 5, 2544, 3325, 2553, 3336)) {
                            continue;
                        }
                        int h = KandarinKit.hash(x, y);
                        if (h % 7 == 0) {
                            // ruined / empty plot
                            k.tFill(x + 1, y + 1, x + 4, y + 1, 0, h % 2, "minecraft:mossy_cobblestone");
                            k.at(x + 2, y + 3, 0, "minecraft:cobweb");
                            continue;
                        }
                        Pal p = switch (h % 3) {
                            case 0 -> PLAGUE;
                            case 1 -> PLAGUE.roof("minecraft:cobblestone");
                            default -> PLAGUE.roof("flat:minecraft:cobblestone").wall("minecraft:stone_bricks");
                        };
                        char door = (y + 5 == ys[1] || y + 12 > ys[1]) ? 'n' : 's';
                        if (ys[0] == 3267) {
                            door = 'n';
                        }
                        Bld b = k.house(x, y, x + 5, y + 5, 1 + (h % 5 == 0 ? 1 : 0), p, door, FURN);
                        if (h % 4 == 1) {
                            k.set(b.x1() + 1, 0, b.z1() + 1, "minecraft:cobweb");
                        }
                        if (h % 6 == 2) {
                            k.set(b.doorX(), 2, b.doorZ() + (door == 'n' ? -1 : 1), "minecraft:red_wool");
                        }
                    }
                }
            }
        }
        // Dead trees and debris
        for (int[] t : new int[][]{{2470, 3300}, {2500, 3330}, {2545, 3310}, {2490, 3270}}) {
            k.fill(KandarinKit.X(t[0]), 0, KandarinKit.Z(t[1]), KandarinKit.X(t[0]), 4, KandarinKit.Z(t[1]), "minecraft:dark_oak_log");
            k.set(KandarinKit.X(t[0]) + 1, 3, KandarinKit.Z(t[1]), "minecraft:dark_oak_log[axis=x]");
        }
        return k.build();
    }

    private static boolean overlaps(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        return ax1 <= bx2 && ax2 >= bx1 && ay1 <= by2 && ay2 >= by1;
    }

    // ------------------------------------------------------------------ Clock Tower, Tower of Life, Monastery

    static StructureDef clockTower() {
        KandarinKit k = new KandarinKit("clock_tower", "Clock Tower").replaces("clock_tower");
        k.flatten(2554, 3232, 2582, 3251);
        Pal p = new Pal(SB, "minecraft:chiseled_stone_bricks", "minecraft:spruce_planks", "flat:minecraft:stone_bricks", "spruce", PANE);
        Bld t = k.house(2561, 3239, 2567, 3246, 4, p, 'e', FLAT | FURN);
        k.hipRoof(t.x1(), t.z1(), t.x2(), t.z2(), t.top() + 1, "minecraft:deepslate_tile");
        // clock faces on each side near the top
        int y = t.top() - 3;
        int cx = t.cx();
        int cz = t.cz();
        k.fill(cx - 1, y - 1, t.z1(), cx + 1, y + 1, t.z1(), "minecraft:white_concrete");
        k.fill(cx - 1, y - 1, t.z2(), cx + 1, y + 1, t.z2(), "minecraft:white_concrete");
        k.fill(t.x1(), y - 1, cz - 1, t.x1(), y + 1, cz + 1, "minecraft:white_concrete");
        k.fill(t.x2(), y - 1, cz - 1, t.x2(), y + 1, cz + 1, "minecraft:white_concrete");
        k.set(cx, y, t.z1(), "minecraft:black_concrete");
        k.set(cx, y + 1, t.z1(), "minecraft:black_concrete");
        k.set(cx, y, t.z2(), "minecraft:black_concrete");
        k.set(cx + 1, y, t.z2(), "minecraft:black_concrete");
        k.set(t.x1(), y, cz, "minecraft:black_concrete");
        k.set(t.x2(), y, cz, "minecraft:black_concrete");
        // coloured cogs on pedestals
        String[] cogs = {"red", "blue", "black", "white"};
        for (int i = 0; i < 4; i++) {
            k.set(t.x1() + 2 + (i % 2) * 6, i * H, t.z1() + 2 + (i / 2) * 8, "minecraft:" + cogs[i] + "_concrete");
        }
        k.set(t.x1() + 1, 0, t.z2() - 1, "minecraft:oak_trapdoor[half=top]");
        // garden
        k.fence(2557, 3235, 2571, 3249, "minecraft:spruce_fence");
        k.tClear(2571, 3242, 2571, 3243, 0, 0);
        k.road(2568, 3242, 2581, 3243, GRAVEL, null);
        k.water(2558, 3247, 2560, 3248, 1);
        k.oak(KandarinKit.X(2569), KandarinKit.Z(3237), 5);
        return k.build();
    }

    static StructureDef towerOfLife() {
        KandarinKit k = new KandarinKit("tower_of_life", "Tower of Life").replaces("tower_of_life");
        k.flatten(2636, 3205, 2660, 3230);
        int cx = KandarinKit.X(2646) + 1;
        int cz = KandarinKit.Z(3216) + 1;
        k.disc(cx, cz, -1, -1, 10, GRAVEL);
        k.towerB(cx, cz, 7, 26, SB, "minecraft:spruce_planks", null, 'e', 6);
        k.ring(cx, cz, 4, 4, 8, "minecraft:stone_brick_slab");
        k.ring(cx, cz, 10, 10, 8, "minecraft:stone_brick_slab");
        k.ring(cx, cz, 16, 16, 8, "minecraft:stone_brick_slab");
        // machinery
        k.fill(cx - 4, 0, cz - 1, cx - 4, 22, cz - 1, "minecraft:iron_block");
        k.fill(cx - 3, 0, cz + 3, cx - 3, 22, cz + 3, "minecraft:copper_block");
        k.set(cx, 0, cz, "minecraft:blast_furnace");
        k.set(cx + 2, 0, cz - 3, "minecraft:anvil");
        k.set(cx - 1, 6, cz - 2, "minecraft:observer");
        k.set(cx + 1, 12, cz + 1, "minecraft:piston");
        k.set(cx, 18, cz, "minecraft:lightning_rod");
        k.set(cx + 2, 6, cz + 2, "minecraft:crafting_table");
        k.fill(cx - 1, 25, cz - 1, cx + 1, 25, cz + 1, "minecraft:glass");
        k.set(cx, 26, cz, "minecraft:lightning_rod");
        return k.build();
    }

    static StructureDef monastery() {
        KandarinKit k = new KandarinKit("ardougne_monastery", "Ardougne Monastery").replaces("ardougne_monastery");
        k.flatten(2588, 3196, 2614, 3230);
        k.cwallRect(2590, 3198, 2612, 3228, 4, "minecraft:mossy_cobblestone");
        k.gate(2600, 3228, 2601, 3228, 3, GRAVEL);
        k.road(2600, 3214, 2601, 3227, GRAVEL, null);
        k.road(2600, 3214, 2611, 3215, GRAVEL, null);
        Pal mp = STONE.roof("minecraft:deepslate_tile").floor("minecraft:spruce_planks");
        Bld chapel = k.house(2593, 3203, 2604, 3212, 2, mp, 'n', 0);
        k.set(chapel.cx(), 4, chapel.z2() - 1, ALTAR);
        k.set(chapel.cx() - 1, 4, chapel.z2() - 1, "minecraft:candle[candles=3,lit=true]");
        k.set(chapel.cx() + 1, 4, chapel.z2() - 1, "minecraft:candle[candles=3,lit=true]");
        for (int z = chapel.z1() + 2; z <= chapel.z2() - 3; z += 2) {
            k.fill(chapel.x1() + 2, 4, z, chapel.x2() - 3, 4, z, "minecraft:spruce_stairs[facing=north]");
        }
        k.fill(chapel.x1() + 1, 0, chapel.z1() + 1, chapel.x1() + 1, 1, chapel.z2() - 1, "minecraft:bookshelf");
        k.longTable(chapel.x1() + 3, chapel.x2() - 3, 0, chapel.cz(), "spruce");
        Bld dorm = k.house(2593, 3216, 2598, 3225, 1, mp, 'e', NO_LADDER);
        for (int z = dorm.z1() + 1; z <= dorm.z2() - 2; z += 3) {
            k.bed(dorm.x1() + 1, 0, z, "brown");
        }
        k.belfry(chapel.x2() - 4, chapel.z1() + 4, 13, "minecraft:mossy_stone_bricks", "minecraft:deepslate_tile");
        k.farm(2606, 3200, 2611, 3210, "minecraft:wheat[age=7]");
        k.well(KandarinKit.X(2604), KandarinKit.Z(3220));
        k.oak(KandarinKit.X(2608), KandarinKit.Z(3225), 6);
        k.oak(KandarinKit.X(2610), KandarinKit.Z(3218), 5);
        return k.build();
    }

    // ------------------------------------------------------------------ Witchaven & Fishing Platform

    static StructureDef witchaven() {
        KandarinKit k = new KandarinKit("witchaven", "Witchaven").replaces("witchaven");
        k.flatten(2706, 3268, 2742, 3302);
        k.floor(2732, 3268, 2735, 3302, SAND);
        k.water(2736, 3268, 2742, 3302, 3);
        k.road(2708, 3285, 2735, 3287, PATH, GRAVEL);
        Pal wp = STONE.roof("thatch").wall("minecraft:cobblestone").post("minecraft:stripped_spruce_log");
        k.house(2710, 3290, 2715, 3295, 1, wp, 's', FURN);
        k.house(2717, 3292, 2722, 3297, 2, wp, 's', FURN);
        Bld gs = k.house(2725, 3291, 2730, 3296, 1, wp, 's', NO_LADDER);
        k.counter(gs, GENERAL, "spruce");
        k.house(2710, 3276, 2715, 3281, 1, wp, 'n', FURN);
        k.house(2725, 3276, 2730, 3281, 2, wp, 'n', FURN);
        Bld chap = k.house(2717, 3270, 2722, 3279, 1, STONE.floor(ANDESITE), 'n', NO_LADDER | NO_CEILING);
        k.set(chap.cx(), 0, chap.z2() - 1, ALTAR);
        for (int z = chap.z1() + 3; z <= chap.z2() - 3; z += 2) {
            k.fill(chap.x1() + 1, 0, z, chap.x2() - 1, 0, z, "minecraft:spruce_stairs[facing=north]");
        }
        k.clear(chap.cx(), 0, chap.z1() + 3, chap.cx(), 0, chap.z2() - 3);
        k.well(KandarinKit.X(2716), KandarinKit.Z(3287));
        k.pier(2736, 3286, 2748, 3287, "spruce", true);
        k.longboat(KandarinKit.X(2744), KandarinKit.Z(3290), 13, true, "brown");
        for (int y = 3270; y <= 3300; y += 6) {
            k.set(KandarinKit.X(2733), 0, KandarinKit.Z(y), "minecraft:barrel[facing=up]");
        }
        return k.build();
    }

    static StructureDef fishingPlatform() {
        KandarinKit k = new KandarinKit("fishing_platform", "Fishing Platform").replaces("fishing_platform");
        int x1 = KandarinKit.X(2776);
        int x2 = KandarinKit.X(2794) + 1;
        int z1 = KandarinKit.Z(3294);
        int z2 = KandarinKit.Z(3278) + 1;
        k.waterB(x1 - 6, z1 - 6, x2 + 6, z2 + 6, 6);
        k.clear(x1 - 6, 3, z1 - 6, x2 + 6, 20, z2 + 6);
        for (int x = x1; x <= x2; x += 6) {
            for (int z = z1; z <= z2; z += 6) {
                k.fill(x, -9, z, x, 2, z, "minecraft:stripped_dark_oak_log");
            }
        }
        k.fill(x1, 3, z1, x2, 3, z2, "minecraft:spruce_planks");
        KandarinKit d = k.raised(4);
        d.walls(x1, 0, z1, x2, 0, z2, "minecraft:spruce_fence");
        d.clear(x1, 0, (z1 + z2) / 2, x1, 0, (z1 + z2) / 2 + 1);
        k.fill(x1, -9, (z1 + z2) / 2, x1, 3, (z1 + z2) / 2, "minecraft:stripped_dark_oak_log");
        k.ladder(x1 - 1, (z1 + z2) / 2, -2, 3, "west");
        Bld a = d.house(2777, 3289, 2782, 3293, 1, FREM.roof("minecraft:spruce"), 'e', FURN | NO_LADDER);
        Bld b = d.house(2788, 3289, 2793, 3293, 2, FREM.roof("minecraft:spruce"), 's', FURN);
        d.set(a.x1() + 1, 0, a.z2() - 1, RANGE);
        // crane and nets
        int cx = x2 - 3;
        int cz = z2 - 3;
        d.fill(cx, 0, cz, cx, 8, cz, "minecraft:spruce_log");
        d.fill(cx - 5, 8, cz, cx, 8, cz, "minecraft:spruce_log[axis=x]");
        d.fill(cx - 5, 4, cz, cx - 5, 7, cz, "minecraft:chain");
        d.set(cx - 5, 3, cz, "minecraft:barrel[facing=up]");
        for (int x = x1 + 2; x <= x2 - 8; x += 4) {
            d.set(x, 0, z2 - 1, "minecraft:barrel[facing=up]");
            d.set(x + 1, 0, z2 - 1, "minecraft:cobweb");
        }
        d.lamp(x1 + 1, z1 + 1, "minecraft:spruce_fence");
        d.lamp(x2 - 1, z1 + 1, "minecraft:spruce_fence");
        return k.build();
    }
}
