package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.world.MisthalinKit.Box;
import com.runecraft.osrs.content.world.MisthalinKit.Roof;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.MisthalinKit.*;

/**
 * Draynor Village (timber houses, the bank, the market, the Wise Old Man, Aggie, Ned, Morgan, the
 * jail and the willow riverbank), the allotments north-west of the village, Draynor Manor with
 * Professor Oddenstein's tower, and the Wizards' Tower on its island.
 */
final class MisthalinDraynor {
    private MisthalinDraynor() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(village());
        out.add(manor());
        out.add(wizardsTower());
        out.add(market());
        out.add(belladonna());
    }

    /** A Draynor timber-framed cottage: white plaster between dark oak posts and beams, dark roof. */
    static MisthalinKit.House timber(MisthalinKit k, Box b, int storeys) {
        return k.house(b).storeys(storeys).wall(WHITE_PLASTER).corner(DARK_LOG).beam(STRIPPED_DARK)
                .base(COBBLE).floor(DARK_PLANKS).upperFloor(SPRUCE_PLANKS).roof(Roof.DARK_OAK).gable(DARK_PLANKS);
    }

    /** Vertical dark oak studs every few blocks on the long walls of a timber house. */
    static void studs(MisthalinKit k, Box b, int top) {
        for (int x = b.x1() + 3; x < b.x2() - 1; x += 4) {
            k.fill(x, 0, b.z1(), x, top, b.z1(), STRIPPED_DARK);
            k.fill(x, 0, b.z2(), x, top, b.z2(), STRIPPED_DARK);
        }
    }

    // ------------------------------------------------------------------ village

    static StructureDef village() {
        MisthalinKit k = new MisthalinKit("draynor_village", "Draynor Village", "DRAYNOR_VILLAGE");
        k.tFlatten(3070, 3226, 3132, 3276);
        k.meadow(t(3070, 3226, 3132, 3276), 16);
        // roads: the main east-west road, north to the manor and south to the river
        k.tPath(3070, 3248, 3132, 3248, 2);
        k.tPath(3104, 3276, 3104, 3249, 2);
        k.tPath(3092, 3239, 3092, 3233, 2);
        k.tPath(3118, 3247, 3118, 3238, 2);
        // the river / sea shore to the south with Draynor's willows and fishing spots
        k.water(t(3072, 3226, 3110, 3230), 3);
        k.floor(t(3072, 3231, 3110, 3231), "minecraft:sand");
        k.scatter(t(3072, 3231, 3110, 3231), -1, GRAVEL, 4);
        int[][] willows = {{3076, 3234}, {3081, 3233}, {3085, 3236}, {3089, 3233}, {3096, 3235}, {3100, 3233}, {3105, 3235}};
        for (int[] w : willows) {
            k.willow(X(w[0]), Z(w[1]));
        }
        k.set(X(3086), -1, Z(3228), "runecraft:net_bait_fishing_spot");
        k.set(X(3087), -1, Z(3229), "runecraft:net_bait_fishing_spot");
        // a little jetty
        k.fill(X(3094), 0, Z(3231), X(3095) + 1, 0, Z(3228), "minecraft:spruce_planks");
        k.fill(X(3094), -3, Z(3228), X(3094), -1, Z(3228), SPRUCE_LOG).fill(X(3095) + 1, -3, Z(3228), X(3095) + 1, -1, Z(3228), SPRUCE_LOG);

        bank(k);
        wiseOldMan(k);
        aggie(k);
        ned(k);
        morgan(k);
        jail(k);
        // more village cottages
        int[][] cottages = {{3108, 3252, 3113, 3257, 's'}, {3110, 3237, 3116, 3242, 'n'}, {3074, 3238, 3080, 3243, 'n'},
                {3122, 3252, 3127, 3257, 's'}, {3074, 3257, 3079, 3262, 'e'}};
        for (int[] c : cottages) {
            Box b = t(c[0], c[1], c[2], c[3]);
            char side = (char) c[4];
            MisthalinKit.House h = timber(k, b, 1);
            h.doorAt(side, side == 'n' || side == 's' ? b.cx() : b.cz(), 2, 3).build();
            studs(k, b, 3);
            Box in = b.inset(1);
            k.bed(in.x1(), 0, in.z1(), 's', "brown");
            k.set(in.x2(), 0, in.z1(), "minecraft:chest[facing=west]").set(in.x2(), 0, in.z1() + 1, BARREL);
            k.table(in.cx(), 0, in.cz() + 1).chair(in.cx() - 1, 0, in.cz() + 1, "east");
            k.set(in.x1(), 0, in.z2(), "minecraft:furnace[facing=east]").set(in.x1() + 1, 0, in.z2(), CRAFTING);
            k.hang(in.cx(), 2, in.cz());
        }
        // pigsty and well by the crossroads
        Box sty = t(3112, 3260, 3118, 3265);
        k.floor(sty.inset(1), MUD);
        k.fenceRing(sty, "minecraft:spruce_fence");
        k.gate(sty.x1(), sty.cz(), "minecraft:spruce_fence_gate", "east");
        k.set(sty.x2() - 1, 0, sty.z1() + 1, "minecraft:composter");
        k.well(X(3101), Z(3251));
        // trees
        for (int[] tr : new int[][]{{3072, 3268}, {3084, 3270}, {3112, 3272}, {3126, 3266}, {3128, 3232}, {3118, 3230},
                {3072, 3250}, {3108, 3244}}) {
            k.tOak(tr[0], tr[1]);
        }
        allotments(k);
        return k.build();
    }

    private static void bank(MisthalinKit k) {
        // Draynor Village bank, tiles 3088-3097 x 3240-3246, door to the north onto the main road
        Box b = t(3088, 3240, 3097, 3246);
        k.house(b).storeys(1).height(5).wall(STONE_BRICKS).corner(CHISELED).base(COBBLE).floor(POLISHED_ANDESITE)
                .roof(Roof.SLATE).doorAt('n', X(3092), 4, 4).build();
        Box in = b.inset(1);
        // booth counter across the room; bankers' side to the south
        int z = Z(3243);
        k.boothsX(in.x1() + 1, in.x2() - 1, 0, z);
        k.fill(in.x1(), 0, z, in.x1(), 1, z, POLISHED_ANDESITE).fill(in.x2(), 0, z, in.x2(), 1, z, POLISHED_ANDESITE);
        k.carpet(new Box(in.x1(), in.z1(), in.x2(), z - 1), 0, "red");
        k.carpet(new Box(in.x1(), z + 1, in.x2(), in.z2()), 0, "gray");
        for (int x = in.x1(); x <= in.x2(); x += 3) {
            k.set(x, 0, in.z2(), "minecraft:chest[facing=north]");
            k.set(x + 1, 0, in.z2(), BARREL);
        }
        k.hang(in.cx(), 3, in.z1() + 2).hang(in.x1() + 3, 3, in.z1() + 2).hang(in.x2() - 3, 3, in.z1() + 2);
        k.hang(in.cx(), 3, in.z2() - 1);
        k.set(X(3092) - 1, 2, b.z1() - 1, "minecraft:wall_torch[facing=north]").set(X(3093) + 2, 2, b.z1() - 1, "minecraft:wall_torch[facing=north]");
        k.floor(new Box(X(3091), b.z1() - 3, X(3094) + 1, b.z1() - 1), POLISHED_ANDESITE);
    }

    private static void wiseOldMan(MisthalinKit k) {
        // the Wise Old Man's house, two storeys of books and curios
        Box b = t(3088, 3250, 3094, 3255);
        timber(k, b, 2).doorAt('s', X(3091), 2, 3).build();
        studs(k, b, 7);
        Box in = b.inset(1);
        k.shelvesX(in.x1(), in.x2(), 0, in.z1(), "south");
        k.set(in.x1(), 0, in.z2(), "minecraft:lectern[facing=east]");
        k.table(in.x2() - 1, 0, in.cz()).darkChair(in.x2() - 2, 0, in.cz(), "east");
        k.carpet(new Box(in.x1() + 1, in.z1() + 2, in.x2() - 1, in.z2() - 1), 0, "blue");
        k.ladder(in.x1(), in.z1() + 2, "east", 0, 4);
        k.hang(in.cx(), 2, in.cz());
        // upstairs: bed, telescope and a display of oddities
        k.bed(in.x2(), 4, in.z1(), 's', "purple");
        k.shelvesX(in.x1() + 2, in.x2() - 2, 4, in.z1(), "south");
        k.set(in.cx(), 4, in.z2(), "minecraft:lightning_rod");
        k.set(in.x2(), 4, in.z2(), "minecraft:enchanting_table");
        k.set(in.x1() + 1, 4, in.z2(), "minecraft:brewing_stand");
        k.hang(in.cx(), 6, in.cz());
    }

    private static void aggie(MisthalinKit k) {
        // Aggie the witch's house, cauldron bubbling
        Box b = t(3083, 3257, 3088, 3262);
        timber(k, b, 1).doorAt('s', X(3086), 2, 3).build();
        Box in = b.inset(1);
        k.set(in.cx(), 0, in.cz(), "minecraft:water_cauldron[level=3]");
        k.set(in.cx(), -1, in.cz(), "minecraft:magma_block");
        k.shelvesZ(in.x1(), 0, in.z1(), in.z2(), "east");
        k.set(in.x2(), 0, in.z1(), "minecraft:brewing_stand").set(in.x2(), 0, in.z1() + 1, BARREL);
        k.bed(in.x2(), 0, in.z2() - 1, 'n', "black");
        k.hang(in.cx() + 1, 2, in.cz());
        k.pot(in.x2(), 1, in.z1() + 1, "red_mushroom");
    }

    private static void ned(MisthalinKit k) {
        // Ned's house (the old sailor who makes rope)
        Box b = t(3097, 3256, 3102, 3261);
        timber(k, b, 1).doorAt('s', X(3099), 2, 3).build();
        Box in = b.inset(1);
        k.bed(in.x1(), 0, in.z1(), 's', "blue");
        k.set(in.x2(), 0, in.z1(), "minecraft:chest[facing=west]");
        k.fill(in.x2(), 0, in.z2() - 2, in.x2(), 0, in.z2(), "minecraft:white_wool");
        k.set(in.x2() - 1, 0, in.z2(), SPINNING);
        k.table(in.cx(), 0, in.cz()).chair(in.cx(), 0, in.cz() + 1, "north");
        k.hang(in.cx(), 2, in.cz());
    }

    private static void morgan(MisthalinKit k) {
        // Morgan's house (Vampyre Slayer): garlic upstairs
        Box b = t(3096, 3266, 3102, 3271);
        timber(k, b, 2).doorAt('w', Z(3268), 2, 3).build();
        studs(k, b, 7);
        Box in = b.inset(1);
        k.kitchen(in, 0);
        k.ladder(in.x2(), in.z2(), "west", 0, 4);
        k.bed(in.x1(), 4, in.z1(), 's', "red");
        k.set(in.x2() - 1, 4, in.z1(), "minecraft:chest[facing=south]");
        k.fill(in.x1() + 2, 4, in.z2(), in.x1() + 3, 4, in.z2(), "minecraft:white_wool");
        k.hang(in.cx(), 6, in.cz());
    }

    private static void jail(MisthalinKit k) {
        // Draynor jail (Lady Keli's prison for Prince Ali), stone with barred cells
        Box b = t(3121, 3240, 3130, 3247);
        k.house(b).storeys(1).height(5).wall(COBBLE).corner(STONE_BRICKS).base(COBBLE).floor(STONE)
                .roof(Roof.SLATE).window(BARS).doorAt('w', Z(3244), 2, 3).build();
        Box in = b.inset(1);
        // two cells along the east side behind iron bars
        int cx = in.x2() - 5;
        k.connect(cx, 0, in.z1(), cx, 3, in.z2(), BARS);
        k.connect(cx, 0, in.cz(), in.x2(), 3, in.cz(), BARS);
        k.bed(in.x2(), 0, in.z1(), 's', "gray");
        k.bed(in.x2(), 0, in.z2() - 1, 's', "gray");
        k.set(in.x2() - 3, 0, in.z1(), "minecraft:cauldron");
        // guard room
        k.table(in.x1() + 2, 0, in.cz()).chair(in.x1() + 1, 0, in.cz(), "east");
        k.set(in.x1(), 0, in.z1(), BARREL).set(in.x1() + 1, 0, in.z1(), "minecraft:chest[facing=south]");
        k.hang(in.x1() + 3, 3, in.cz()).hang(in.x2() - 2, 3, in.z1() + 2).hang(in.x2() - 2, 3, in.z2() - 2);
        k.fenceRing(t(3119, 3238, 3132, 3249), "minecraft:spruce_fence");
        k.clear(X(3119), 0, Z(3244), X(3119), 0, Z(3244) + 1);
    }

    private static void allotments(MisthalinKit k) {
        // Draynor allotments north-west of the village (herb patch, two allotments, compost bin)
        k.tFlatten(3050, 3300, 3068, 3316);
        Box plot = t(3052, 3302, 3066, 3314);
        k.fenceRing(plot, "minecraft:oak_fence");
        k.clear(plot.x2(), 0, Z(3308), plot.x2(), 0, Z(3308) + 1);
        k.floor(t(3053, 3310, 3065, 3313), "minecraft:farmland[moisture=7]");
        k.floor(t(3053, 3303, 3065, 3306), "minecraft:farmland[moisture=7]");
        k.fill(X(3053), -2, Z(3308), X(3060) + 1, -2, Z(3307) + 1, DIRT);
        k.fill(X(3053), -1, Z(3308), X(3060) + 1, -1, Z(3307) + 1, WATER);
        k.set(X(3059), 0, Z(3311), "runecraft:allotment_patch");
        k.set(X(3059), 0, Z(3304), "runecraft:allotment_patch");
        k.floor(t(3062, 3307, 3065, 3309), COARSE_DIRT);
        k.set(X(3063), 0, Z(3308), "runecraft:herb_patch");
        k.set(X(3061), 0, Z(3308), "runecraft:compost_bin");
        k.tPath(3067, 3308, 3067, 3300, 1);
    }

    // ------------------------------------------------------------------ market

    static StructureDef market() {
        // Draynor market square west of the bank: Martin's seed stall, the wine and vegetable stalls
        MisthalinKit k = new MisthalinKit("draynor_market", "Draynor market", "draynor_market");
        Box sq = t(3074, 3246, 3086, 3256);
        k.street(sq);
        k.stall(X(3077), Z(3253), "runecraft:seed_stall", "green");
        k.stall(X(3083), Z(3253), "runecraft:wine_stall", "purple");
        k.stall(X(3080), Z(3249), "runecraft:vegetable_stall", "orange");
        k.stall(X(3076), Z(3249), "runecraft:silk_stall", "red");
        k.lampPost(X(3074), Z(3246)).lampPost(X(3086) + 1, Z(3246)).lampPost(X(3074), Z(3256)).lampPost(X(3086) + 1, Z(3256));
        k.crates(X(3085), Z(3255));
        k.crates(X(3074) + 1, Z(3255));
        return k.build();
    }

    // ------------------------------------------------------------------ manor

    static StructureDef manor() {
        MisthalinKit k = new MisthalinKit("draynor_manor", "Draynor Manor", "DRAYNOR_MANOR");
        k.tFlatten(3078, 3332, 3132, 3382);
        Box grounds = t(3082, 3336, 3128, 3379);
        k.floor(grounds, GRASS);
        k.scatter(grounds, -1, "minecraft:podzol", 5);
        k.scatter(grounds, -1, COARSE_DIRT, 7);
        k.scatter(grounds, 0, "minecraft:dead_bush", 30);
        k.scatter(grounds, 0, "minecraft:grass", 12);
        // gravel drive from the gate to the door
        k.floor(t(3107, 3336, 3110, 3352), GRAVEL);
        k.floor(t(3098, 3346, 3119, 3348), GRAVEL);
        // black iron fence around the grounds, gate to the south
        String fence = "minecraft:nether_brick_fence";
        k.fenceRing(grounds, fence);
        k.connect(grounds.x1(), 1, grounds.z1(), grounds.x2(), 1, grounds.z1(), fence);
        k.connect(grounds.x1(), 1, grounds.z2(), grounds.x2(), 1, grounds.z2(), fence);
        k.connect(grounds.x1(), 1, grounds.z1(), grounds.x1(), 1, grounds.z2(), fence);
        k.connect(grounds.x2(), 1, grounds.z1(), grounds.x2(), 1, grounds.z2(), fence);
        int gx = X(3108);
        k.clear(gx, 0, grounds.z2(), gx + 3, 1, grounds.z2());
        k.gate(gx + 1, grounds.z2(), "minecraft:dark_oak_fence_gate", "south");
        k.gate(gx + 2, grounds.z2(), "minecraft:dark_oak_fence_gate", "south");
        k.fill(gx - 1, 0, grounds.z2(), gx - 1, 3, grounds.z2(), "minecraft:polished_blackstone_bricks");
        k.fill(gx + 4, 0, grounds.z2(), gx + 4, 3, grounds.z2(), "minecraft:polished_blackstone_bricks");
        k.set(gx - 1, 4, grounds.z2(), "minecraft:soul_lantern").set(gx + 4, 4, grounds.z2(), "minecraft:soul_lantern");
        k.tPath(3108, 3335, 3108, 3332, 2);

        String wall = "minecraft:deepslate_bricks";
        String trim = "minecraft:polished_deepslate";
        String win = "minecraft:gray_stained_glass_pane";
        // main block and the two front wings
        Box main = t(3097, 3360, 3120, 3372);
        Box west = t(3097, 3353, 3104, 3359);
        Box east = t(3113, 3353, 3120, 3359);
        k.house(main).storeys(2).height(5).wall(wall).corner(trim).beam(trim).base("minecraft:cobbled_deepslate")
                .floor(DARK_PLANKS).roof(Roof.BLACKSTONE).gable(wall).window(win).doorAt('s', X(3108), 4, 4).build();
        for (Box w : new Box[]{west, east}) {
            k.house(w).storeys(2).height(5).wall(wall).corner(trim).beam(trim).base("minecraft:cobbled_deepslate")
                    .floor(DARK_PLANKS).roof(Roof.BLACKSTONE).gable(wall).window(win).build();
        }
        // wings open into the main block
        k.clear(west.x1() + 3, 0, main.z2(), west.x2() - 3, 2, west.z1());
        k.clear(east.x1() + 3, 0, main.z2(), east.x2() - 3, 2, east.z1());
        k.clear(west.x1() + 3, 5, main.z2(), west.x2() - 3, 7, west.z1());
        k.clear(east.x1() + 3, 5, main.z2(), east.x2() - 3, 7, east.z1());
        k.fill(west.x1() + 3, -1, main.z2(), west.x2() - 3, -1, west.z1(), DARK_PLANKS);
        k.fill(east.x1() + 3, -1, main.z2(), east.x2() - 3, -1, east.z1(), DARK_PLANKS);
        k.fill(west.x1() + 3, 4, main.z2(), west.x2() - 3, 4, west.z1(), DARK_PLANKS);
        k.fill(east.x1() + 3, 4, main.z2(), east.x2() - 3, 4, east.z1(), DARK_PLANKS);
        // front porch between the wings
        Box porch = t(3106, 3355, 3111, 3359);
        k.floor(porch, "minecraft:polished_blackstone_bricks");
        k.fill(porch.x1(), 0, porch.z2(), porch.x1(), 4, porch.z2(), trim).fill(porch.x2(), 0, porch.z2(), porch.x2(), 4, porch.z2(), trim);
        k.fill(porch.x1(), 5, porch.z1(), porch.x2(), 5, porch.z2(), "minecraft:polished_blackstone_brick_slab");
        k.set(X(3108) - 1, 2, main.z2() + 1, "minecraft:soul_wall_torch[facing=south]");
        k.set(X(3109) + 2, 2, main.z2() + 1, "minecraft:soul_wall_torch[facing=south]");
        // Ernest's tower (Professor Oddenstein's laboratory) at the north-east corner
        int tx = main.x2();
        int tz = main.z1();
        k.tower(tx, tz, 5, 25, 5, wall, DARK_PLANKS, Roof.BLACKSTONE, true);

        Box in = main.inset(1);
        // entrance hall with the grand staircase
        int hx = X(3108);
        k.carpet(new Box(hx, in.z2() - 10, hx + 3, in.z2()), 0, "red");
        k.stairs2(hx + 1, 0, in.z1() + 6, 'n', 5, "minecraft:dark_oak_stairs", 'e');
        k.connect(hx, 5, in.z1() + 2, hx, 5, in.z1() + 5, "minecraft:dark_oak_fence");
        k.connect(hx + 3, 5, in.z1() + 2, hx + 3, 5, in.z1() + 5, "minecraft:dark_oak_fence");
        k.fill(hx - 2, 0, in.z1(), hx - 2, 3, in.z2(), wall);
        k.fill(hx + 5, 0, in.z1(), hx + 5, 3, in.z2(), wall);
        k.clear(hx - 2, 0, in.z2() - 4, hx - 2, 2, in.z2() - 3);
        k.clear(hx + 5, 0, in.z2() - 4, hx + 5, 2, in.z2() - 3);
        k.hang(hx + 2, 3, in.z2() - 3).hang(hx + 2, 3, in.z1() + 2);
        // library to the west (the secret bookcase)
        k.shelvesX(in.x1(), hx - 3, 0, in.z1(), "south");
        k.shelvesZ(in.x1(), 0, in.z1() + 1, in.z2(), "east");
        k.fill(in.x1() + 4, 0, in.z1() + 4, in.x1() + 12, 1, in.z1() + 4, BOOKSHELF);
        k.fill(in.x1() + 4, 0, in.z1() + 8, in.x1() + 12, 1, in.z1() + 8, BOOKSHELF);
        k.set(in.x1() + 14, 0, in.z2() - 2, "minecraft:lectern[facing=west]");
        k.table(in.x1() + 8, 0, in.z2() - 2).chair(in.x1() + 7, 0, in.z2() - 2, "east");
        k.set(in.x1() + 8, 0, in.z1() + 6, "minecraft:cobweb").set(in.x1() + 2, 3, in.z1() + 2, "minecraft:cobweb");
        k.hang(in.x1() + 8, 3, in.z1() + 6).hang(in.x1() + 16, 3, in.z1() + 6);
        // dining room and kitchen to the east
        k.diningX(hx + 9, hx + 17, 0, in.cz() + 3);
        k.set(hx + 8, 0, in.cz() + 3, "minecraft:dark_oak_stairs[facing=east]");
        k.set(in.x2(), 0, in.z2(), RANGE).set(in.x2() - 1, 0, in.z2(), BARREL).set(in.x2() - 2, 0, in.z2(), "minecraft:cauldron");
        k.fill(in.x2(), 0, in.z2() - 4, in.x2(), 0, in.z2() - 1, "minecraft:dark_oak_planks");
        k.hang(hx + 13, 3, in.cz() + 3);
        k.set(in.x2() - 3, 0, in.z1() + 2, "minecraft:cobweb");
        // first floor: bedrooms and corridor to the laboratory tower
        k.carpet(new Box(in.x1(), in.cz() - 1, in.x2(), in.cz() + 1), 5, "red");
        for (int x = in.x1() + 2; x < in.x2() - 6; x += 8) {
            if (x + 5 >= hx - 2 && x <= hx + 5) {
                continue;
            }
            k.fill(x + 5, 5, in.z1(), x + 5, 8, in.cz() - 2, wall);
            k.fill(x + 5, 5, in.cz() + 2, x + 5, 8, in.z2(), wall);
            k.bed(x, 5, in.z1(), 's', "black");
            k.set(x + 2, 5, in.z1(), "minecraft:chest[facing=south]");
            k.hang(x + 2, 8, in.z1() + 3);
            k.set(x, 5, in.z2(), BOOKSHELF).set(x + 1, 5, in.z2(), "minecraft:flower_pot");
            k.hang(x + 2, 8, in.z2() - 3);
        }
        k.set(in.x1() + 10, 5, in.cz(), "minecraft:cobweb");
        // the basement (Ernest the Chicken's lever puzzle)
        Box cellar = new Box(in.x1() + 2, in.z1() + 2, in.x1() + 18, in.z1() + 12);
        k.fill(cellar.grow(1), -8, -2, "minecraft:cobbled_deepslate");
        k.clear(cellar, -7, -3);
        k.fill(cellar.x1() + 5, -7, cellar.z1(), cellar.x1() + 5, -3, cellar.z2(), "minecraft:cobbled_deepslate");
        k.fill(cellar.x1() + 11, -7, cellar.z1(), cellar.x1() + 11, -3, cellar.z2(), "minecraft:cobbled_deepslate");
        k.clear(cellar.x1() + 5, -7, cellar.cz(), cellar.x1() + 5, -6, cellar.cz());
        k.clear(cellar.x1() + 11, -7, cellar.cz() + 2, cellar.x1() + 11, -6, cellar.cz() + 2);
        for (int x = cellar.x1() + 1; x <= cellar.x2(); x += 3) {
            k.set(x, -5, cellar.z1() - 0, "minecraft:lever[face=wall,facing=south]");
        }
        k.ladder(cellar.x2(), cellar.z2(), "west", -7, -1);
        k.set(cellar.x2(), -1, cellar.z2(), "minecraft:dark_oak_trapdoor[half=top,facing=west]");
        k.set(cellar.x1() + 2, -3, cellar.cz(), "minecraft:soul_lantern[hanging=true]");
        k.set(cellar.x1() + 8, -3, cellar.cz(), "minecraft:soul_lantern[hanging=true]");
        k.set(cellar.x1() + 14, -3, cellar.cz(), "minecraft:soul_lantern[hanging=true]");
        k.set(cellar.x1() + 15, -7, cellar.z1(), "minecraft:chest[facing=south]");
        // laboratory at the top of the tower
        k.set(tx, 15, tz + 2, "minecraft:brewing_stand").set(tx + 2, 15, tz, "minecraft:cauldron");
        k.set(tx - 2, 15, tz, "minecraft:crafting_table").set(tx, 15, tz - 2, "minecraft:enchanting_table");
        k.set(tx - 2, 20, tz + 1, "minecraft:lightning_rod");
        // wings: conservatory (west) and storerooms (east)
        Box wi = west.inset(1);
        k.scatter(wi, 0, "minecraft:potted_dead_bush", 6);
        k.set(wi.cx(), 0, wi.cz(), "minecraft:composter");
        k.hang(wi.cx(), 3, wi.cz()).hang(wi.cx(), 8, wi.cz());
        Box ei = east.inset(1);
        k.crates(ei.x1(), ei.z1());
        k.crates(ei.x2() - 1, ei.z2());
        k.set(ei.cx(), 0, ei.cz(), "minecraft:cobweb");
        k.hang(ei.cx(), 3, ei.cz()).hang(ei.cx(), 8, ei.cz());
        k.ladder(wi.x1(), wi.z2(), "east", 0, 4);
        k.ladder(ei.x2(), ei.z2(), "west", 0, 4);

        // the fountain in the south-west garden
        int fx = X(3090);
        int fz = Z(3343);
        k.discClear(fx, fz, 4, 0, 1);
        k.ring(fx, fz, 4, 1, 0, 0, "minecraft:stone_bricks");
        k.disc(fx, fz, 3, -2, -1, WATER);
        k.fill(fx, -1, fz, fx, 1, fz, "minecraft:mossy_stone_bricks");
        k.set(fx, 2, fz, "minecraft:mossy_stone_brick_wall");
        // compost heap, graves and dead trees
        k.fill(X(3122), 0, Z(3340), X(3123) + 1, 0, Z(3339) + 1, "minecraft:composter");
        for (int x = X(3114); x <= X(3124); x += 3) {
            k.grave(x, Z(3376));
        }
        for (int[] d : new int[][]{{3086, 3375}, {3093, 3369}, {3124, 3366}, {3084, 3350}, {3126, 3344}, {3100, 3340}, {3118, 3341}}) {
            k.deadTree(X(d[0]), Z(d[1]));
        }
        k.tree(X(3088), Z(3365), 6, DARK_LOG, DARK_LEAVES);
        k.tree(X(3125), Z(3356), 6, DARK_LOG, DARK_LEAVES);
        return k.build();
    }

    static StructureDef belladonna() {
        MisthalinKit k = new MisthalinKit("draynor_belladonna", "Draynor Manor belladonna patch", "draynor_belladonna");
        int x = X(3087);
        int z = Z(3355);
        k.fill(x - 2, -1, z - 2, x + 2, -1, z + 2, "minecraft:podzol");
        k.set(x, 0, z, "runecraft:belladonna_patch");
        k.connect(x - 3, 0, z - 3, x + 3, 0, z - 3, "minecraft:nether_brick_fence");
        k.connect(x - 3, 0, z + 3, x + 3, 0, z + 3, "minecraft:nether_brick_fence");
        k.connect(x - 3, 0, z - 3, x - 3, 0, z + 3, "minecraft:nether_brick_fence");
        k.clear(x + 3, 0, z - 1, x + 3, 0, z + 1);
        return k.build();
    }

    // ------------------------------------------------------------------ wizards' tower

    static StructureDef wizardsTower() {
        MisthalinKit k = new MisthalinKit("wizards_tower", "Wizards' Tower", "WIZARDS_TOWER");
        k.tFlatten(3086, 3138, 3132, 3190);
        int cx = X(3109);
        int cz = Z(3162);
        // island and the surrounding water, bridge to the north shore
        k.ring(cx, cz, 32, 11, -4, -4, DIRT);
        k.ring(cx, cz, 32, 11, -3, -1, WATER);
        k.ring(cx, cz, 21, 2, -1, -1, "minecraft:sand");
        k.disc(cx, cz, 19, -1, -1, GRASS);
        k.meadow(new Box(cx - 13, cz - 13, cx + 13, cz + 13), 6);
        k.bridgeZ(cx - 2, cx + 2, cz - 35, cz - 18, 0, "minecraft:spruce_planks", "minecraft:spruce_fence");
        k.fill(cx - 2, -4, cz - 30, cx - 2, -1, cz - 30, SPRUCE_LOG).fill(cx + 2, -4, cz - 30, cx + 2, -1, cz - 30, SPRUCE_LOG);
        k.fill(cx - 2, -4, cz - 24, cx - 2, -1, cz - 24, SPRUCE_LOG).fill(cx + 2, -4, cz - 24, cx + 2, -1, cz - 24, SPRUCE_LOG);
        k.set(cx - 2, 2, cz - 35, LANTERN).set(cx + 2, 2, cz - 35, LANTERN).set(cx - 2, 2, cz - 18, LANTERN).set(cx + 2, 2, cz - 18, LANTERN);
        k.dirtPath(new Box(cx - 1, cz - 52, cx + 1, cz - 36));
        k.dirtPath(new Box(cx - 1, cz - 17, cx + 1, cz - 14));
        // the tower: 4 storeys of 6 blocks, slate cone, basement
        int r = 12;
        k.tower(cx, cz, r, 24, 6, STONE_BRICKS, SPRUCE_PLANKS, Roof.SLATE, false);
        k.ring(cx, cz, r + 1, 1, -1, 0, POLISHED_ANDESITE);
        for (int y = 5; y < 24; y += 6) {
            k.ring(cx, cz, r + 1, 1, y, y, "minecraft:polished_andesite");
        }
        k.disc(cx, cz, r - 1, -1, -1, POLISHED_ANDESITE);
        k.disc(cx, cz, 3, -1, -1, "minecraft:lapis_block");
        k.clear(cx - 1, 0, cz - r - 1, cx + 1, 3, cz - r);
        k.fill(cx - 1, -1, cz - r - 1, cx + 1, -1, cz - r, POLISHED_ANDESITE);
        k.fill(cx - 2, 0, cz - r - 1, cx - 2, 4, cz - r - 1, CHISELED).fill(cx + 2, 0, cz - r - 1, cx + 2, 4, cz - r - 1, CHISELED);
        k.fill(cx - 1, 4, cz - r - 1, cx + 1, 4, cz - r - 1, "minecraft:stone_brick_slab[type=top]");
        // extra windows around the drum
        for (int y = 2; y < 24; y += 6) {
            for (int pass = 0; pass < 2; pass++) {
                k.set(cx - 8, y, cz - 9, GLASS).set(cx + 8, y, cz - 9, GLASS).set(cx - 8, y, cz + 9, GLASS).set(cx + 8, y, cz + 9, GLASS);
            }
        }
        // spiral of ladders: alternate sides each storey
        k.ladder(cx + r - 1, cz + 3, "west", 0, 5);
        k.ladder(cx - r + 1, cz + 3, "east", 6, 11);
        k.ladder(cx + r - 1, cz + 3, "west", 12, 17);
        // ground floor: the hall of wizards with the rune shop
        k.carpet(new Box(cx - 1, cz - r + 1, cx + 1, cz - 4), 0, "blue");
        k.set(cx, 0, cz + 6, RUNE_SHOP);
        k.fill(cx - 3, 0, cz + 5, cx - 1, 0, cz + 5, "minecraft:spruce_planks").fill(cx + 1, 0, cz + 5, cx + 3, 0, cz + 5, "minecraft:spruce_planks");
        k.shelvesX(cx - 4, cx + 4, 0, cz + 10, "north");
        k.table(cx - 6, 0, cz).table(cx + 6, 0, cz - 3).chair(cx - 7, 0, cz, "east").chair(cx + 7, 0, cz - 3, "west");
        k.set(cx - 6, 0, cz - 6, "minecraft:enchanting_table");
        for (int i = 0; i < 4; i++) {
            k.set(cx + (i < 2 ? -9 : 9), 0, cz + (i % 2 == 0 ? -3 : 3), "minecraft:end_rod");
        }
        // first floor: library (Wizard Traiborn's study)
        k.shelvesX(cx - 6, cx + 6, 6, cz - 10, "south");
        k.shelvesX(cx - 6, cx + 6, 6, cz + 10, "north");
        k.shelvesZ(cx - 10, 6, cz - 5, cz + 5, "east");
        k.diningX(cx - 3, cx + 3, 6, cz);
        k.set(cx + 6, 6, cz + 4, "minecraft:lectern[facing=west]");
        k.carpet(new Box(cx - 5, cz - 6, cx + 5, cz + 6), 6, "purple");
        k.clear(cx - 5, 6, cz, cx + 5, 6, cz);
        k.diningX(cx - 3, cx + 3, 6, cz);
        // second floor: bedrooms and an alchemy bench
        k.bed(cx - 8, 12, cz - 4, 's', "blue").bed(cx - 8, 12, cz + 2, 's', "blue").bed(cx + 6, 12, cz + 6, 'e', "purple");
        k.set(cx + 4, 12, cz - 6, "minecraft:brewing_stand").set(cx + 5, 12, cz - 6, "minecraft:cauldron");
        k.set(cx + 6, 12, cz - 6, "minecraft:crafting_table");
        k.carpet(new Box(cx - 4, cz - 4, cx + 4, cz + 4), 12, "light_blue");
        // top floor: observatory beneath the cone
        k.set(cx, 18, cz, "minecraft:enchanting_table");
        k.set(cx - 2, 18, cz, BOOKSHELF).set(cx + 2, 18, cz, BOOKSHELF).set(cx, 18, cz - 2, BOOKSHELF).set(cx, 18, cz + 2, BOOKSHELF);
        k.set(cx + 6, 18, cz + 6, "minecraft:lightning_rod");
        // basement: Sedridor's chamber with the portal to the Rune Essence mine
        k.disc(cx, cz, r - 1, -8, -2, STONE_BRICKS);
        k.discClear(cx, cz, r - 3, -7, -3);
        k.ladder(cx, cz + r - 3, "north", -7, -1);
        k.set(cx, -1, cz + r - 3, "minecraft:spruce_trapdoor[half=top,facing=north]");
        k.disc(cx, cz, 2, -8, -8, "minecraft:crying_obsidian");
        k.set(cx, -7, cz, "minecraft:amethyst_cluster");
        k.set(cx - 4, -3, cz, LANTERN_HANG).set(cx + 4, -3, cz, LANTERN_HANG).set(cx, -3, cz - 4, LANTERN_HANG);
        k.shelvesX(cx - 3, cx + 3, -7, cz - r + 4, "south");
        return k.build();
    }
}
