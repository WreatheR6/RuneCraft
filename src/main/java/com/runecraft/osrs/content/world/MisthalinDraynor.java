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
        // two counters running north-south as in the OSRS cache: the west one at x 3091 (booths 3241-3243 and
        // 3245, the bankers behind it to the west) and the east one at x 3096 (booths 3241, 3242, 3245); a
        // hatch in each at 3244 / 3243-3244; the customers' floor between them
        int wx = X(3091) + 1;
        int ex = X(3096);
        for (int ty = 3240; ty <= 3245; ty++) {
            boolean wb = ty == 3241 || ty == 3242 || ty == 3243 || ty == 3245;
            boolean eb = ty == 3241 || ty == 3242 || ty == 3245;
            if (ty != 3244) {
                k.set(wx, 0, Z(ty), wb ? BANK : POLISHED_ANDESITE).set(wx, 0, Z(ty) + 1, POLISHED_ANDESITE);
            }
            if (ty != 3244 && ty != 3243) {
                k.set(ex, 0, Z(ty), eb ? BANK : POLISHED_ANDESITE).set(ex, 0, Z(ty) + 1, POLISHED_ANDESITE);
            }
        }
        k.carpet(new Box(wx + 1, in.z1() + 1, ex - 1, in.z2()), 0, "red");
        k.carpet(new Box(in.x1(), in.z1() + 1, wx - 1, in.z2()), 0, "gray");
        for (int z = in.z1() + 2; z <= in.z2(); z += 3) {
            k.set(in.x1(), 0, z, "minecraft:chest[facing=east]");
            k.set(in.x2(), 0, z, BARREL);
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
        // Draynor market square west of the bank, the stalls on their cache tiles: the seed stall (3079, 3253),
        // the market stall (3083, 3251; the wine stall here), a second seed stall (3075, 3249; the vegetable
        // stall here, approx) and the toy stall (3079, 3246; the silk stall here, approx)
        MisthalinKit k = new MisthalinKit("draynor_market", "Draynor market", "draynor_market");
        Box sq = t(3074, 3246, 3087, 3256);
        k.street(sq);
        k.stall(X(3079) + 1, Z(3253) + 1, "runecraft:seed_stall", "green");
        k.stall(X(3083) + 1, Z(3251) + 1, "runecraft:wine_stall", "purple");
        k.stall(X(3075) + 1, Z(3249) + 1, "runecraft:vegetable_stall", "orange");
        k.stall(X(3079) + 1, Z(3246) + 1, "runecraft:silk_stall", "red");
        k.lampPost(X(3074), Z(3246)).lampPost(X(3086) + 1, Z(3246)).lampPost(X(3074), Z(3256)).lampPost(X(3086) + 1, Z(3256));
        k.crates(X(3085), Z(3255));
        k.crates(X(3074) + 1, Z(3255));
        return k.build();
    }

    // ------------------------------------------------------------------ manor

    /**
     * Draynor Manor, traced from the OSRS map (mejrs layers_osrs, planes 0-2 and the cellar) and the
     * shortest-path transports. Ground floor: the west wing with the basement ladder (3092, 3361) behind the
     * library's secret bookcase (3097, 3357-3358), the library, the north-west (poison) room, the long north
     * hall, the small rooms north of the staircase hall (Ernest's rubber tube room at 3109-3112, 3364-3367),
     * the entrance room behind the large doors (3108-3109, 3353), the chequered ballroom with its stairs down
     * (3115, 3356) and the east wing with its north door (3123, 3360). The first floor has its own rooms (OSRS plane 1;
     * the east wing is single-storey); the top floor is the narrow block 3104-3112, 3356-3370 (Professor Oddenstein's floor). The
     * lever-puzzle cellar keeps its OSRS room grid, shifted so its west arm lies under the ladder (it really is
     * 25 tiles further east, reached by a teleporting ladder). Walls stand on tile edges, one block thick.
     */
    static StructureDef manor() {
        MisthalinKit k = new MisthalinKit("draynor_manor", "Draynor Manor", "DRAYNOR_MANOR");
        k.tFlatten(3078, 3326, 3132, 3386);
        Box grounds = t(3084, 3330, 3130, 3381);
        k.floor(grounds, GRASS);
        k.scatter(grounds, -1, "minecraft:podzol", 5);
        k.scatter(grounds, -1, COARSE_DIRT, 7);
        k.scatter(grounds, 0, "minecraft:dead_bush", 30);
        k.scatter(grounds, 0, "minecraft:short_grass", 12);
        // the winding path from the gate to the large doors (OSRS map)
        k.floor(t(3108, 3331, 3110, 3337), GRAVEL);
        k.floor(t(3107, 3337, 3109, 3343), GRAVEL);
        k.floor(t(3108, 3343, 3110, 3348), GRAVEL);
        k.floor(t(3106, 3348, 3113, 3353), GRAVEL);
        // black iron fence around the grounds with the gate (3108-3109) to the south
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
        k.tPath(3108, 3329, 3108, 3326, 2);

        String wall = "minecraft:deepslate_bricks";
        String trim = "minecraft:polished_deepslate";
        String win = "minecraft:gray_stained_glass_pane";
        // ---- the hull: one box per OSRS room block; shared edges make single walls
        Box wWing = hull(3091, 3354, 3096, 3362);
        Box library = hull(3097, 3354, 3104, 3363);
        Box nwRoom = hull(3097, 3366, 3101, 3373);
        Box strip = hull(3097, 3364, 3101, 3365);
        Box northHall = hull(3102, 3368, 3119, 3373);
        Box corridor = hull(3102, 3364, 3104, 3367);
        Box stairHall = hull(3105, 3357, 3112, 3363);
        Box smallRoom = hull(3105, 3364, 3107, 3367);
        Box tubeRoom = hull(3108, 3364, 3112, 3367);
        Box entrance = hull(3105, 3354, 3112, 3356);
        Box ballroom = hull(3113, 3354, 3119, 3367);
        Box eWing = hull(3120, 3354, 3126, 3360);
        Box[] rooms = {wWing, library, nwRoom, strip, northHall, corridor, stairHall, smallRoom, tubeRoom, entrance, ballroom, eWing};
        for (Box r : rooms) {
            k.fill(r, -3, -1, "minecraft:cobbled_deepslate");
            k.fill(r, 0, 4, wall);
        }
        for (Box r : rooms) {
            k.walls(r, 4, 4, trim);
        }
        for (Box r : rooms) {
            k.clear(r.inset(1), 0, 3);
            k.fill(r.inset(1), -1, -1, DARK_PLANKS).fill(r.inset(1), 4, 4, DARK_PLANKS);
        }
        // the first floor has its own rooms (OSRS plane 1): the west block over the wing, library and entrance,
        // the north-west bedroom, the landing round the stairwell, the north-east room, the long north gallery
        // and the two rooms over the entrance and ballroom; the east wing is single-storey
        Box uWest = hull(3091, 3354, 3106, 3362);
        Box uNw = hull(3097, 3363, 3103, 3368);
        Box uLanding = hull(3104, 3363, 3113, 3368);
        Box uNe = hull(3114, 3362, 3119, 3368);
        Box uGallery = hull(3097, 3369, 3119, 3373);
        Box uSouth = hull(3107, 3354, 3110, 3361);
        Box uSe = hull(3111, 3354, 3119, 3361);
        Box[] upper = {uWest, uNw, uLanding, uNe, uGallery, uSouth, uSe};
        for (Box r : upper) {
            k.fill(r, 5, 9, wall);
        }
        for (Box r : upper) {
            k.clear(r.inset(1), 5, 8);
            k.fill(r.inset(1), 4, 4, DARK_PLANKS);
        }
        // the first-floor west block's partitions (OSRS plane 1): the alcoves north and south along the west wall
        // (walls at 3360 and 3357 across 3092-3094), the stub off the north wall at 3095, and the wall at x 3103
        // that closes off the two small east rooms (open to the middle room at 3357; the south room's door at 3105)
        k.fill(X(3091), 5, Z(3359), X(3094) + 1, 8, Z(3359), wall);
        k.fill(X(3095), 5, Z(3362), X(3095), 8, Z(3361), wall);
        k.fill(X(3091), 5, Z(3356), X(3094) + 1, 8, Z(3356), wall);
        k.fill(X(3103), 5, Z(3361), X(3103), 8, Z(3358) + 1, wall);
        k.fill(X(3103), 5, Z(3356), X(3103), 8, Z(3354) + 1, wall);
        k.fill(X(3104), 5, Z(3356), X(3106) + 1, 8, Z(3356), wall);
        k.clear(X(3105), 5, Z(3356), X(3105) + 1, 7, Z(3356));
        // corner quoins on the outer corners
        for (int[] c : new int[][]{{wWing.x1(), wWing.z1()}, {wWing.x1(), wWing.z2()}, {nwRoom.x1(), nwRoom.z1()}, {northHall.x2(), northHall.z1()},
                {eWing.x2(), eWing.z1()}, {eWing.x2(), eWing.z2()}, {wWing.x2(), wWing.z1()}, {ballroom.x2(), ballroom.z1()}}) {
            k.fill(c[0], 0, c[1], c[0], c[0] > X(3120) ? 4 : 9, c[1], trim);
        }
        // the ballroom's chequered floor
        Box bIn = ballroom.inset(1);
        for (int x = bIn.x1(); x <= bIn.x2(); x += 2) {
            for (int z = bIn.z1(); z <= bIn.z2(); z += 2) {
                boolean dark = ((x - bIn.x1()) / 2 + (z - bIn.z1()) / 2) % 2 == 0;
                k.fill(x, -1, z, Math.min(x + 1, bIn.x2()), -1, Math.min(z + 1, bIn.z2()), dark ? "minecraft:polished_blackstone" : "minecraft:calcite");
            }
        }

        // ---- doors and openings (OSRS plane 0; the same openings upstairs)
        int sz = Z(3353);
        k.clear(X(3108), 0, sz, X(3109) + 1, 2, sz);
        k.set(X(3108), 0, sz, "minecraft:dark_oak_door[facing=south,half=lower,hinge=left]")
                .set(X(3108) + 1, 0, sz, "minecraft:dark_oak_door[facing=south,half=lower,hinge=right]")
                .set(X(3109), 0, sz, "minecraft:dark_oak_door[facing=south,half=lower,hinge=left]")
                .set(X(3109) + 1, 0, sz, "minecraft:dark_oak_door[facing=south,half=lower,hinge=right]");
        k.fill(X(3108) - 1, 0, sz + 1, X(3108) - 1, 3, sz + 1, trim).fill(X(3109) + 2, 0, sz + 1, X(3109) + 2, 3, sz + 1, trim);
        k.fill(X(3108) - 1, 4, sz + 1, X(3109) + 2, 4, sz + 1, "minecraft:polished_blackstone_brick_slab[type=bottom]");
        k.set(X(3108) - 1, 2, sz + 2, "minecraft:soul_wall_torch[facing=south]").set(X(3109) + 2, 2, sz + 2, "minecraft:soul_wall_torch[facing=south]");
        doorZ(k, Z(3360), 3123, 0, "north");                      // east wing, north door
        // first-floor doors (OSRS plane 1)
        doorX(k, uSouth.x1(), 3358, 5, "east");
        doorX(k, uSe.x1(), 3357, 5, "east");
        doorZ(k, uSe.z1(), 3115, 5, "north");
        doorX(k, uNe.x1(), 3367, 5, "east");
        doorX(k, uLanding.x1(), 3366, 5, "east");
        doorZ(k, uNw.z1(), 3100, 5, "south");
        doorZ(k, uLanding.z1(), 3109, 5, "south");
        doorZ(k, uWest.z1(), 3101, 5, "south");
        doorZ(k, uWest.z1(), 3105, 5, "south");
        // the grand staircase arrives on the landing (OSRS 3108, 3366 on plane 1) through the stairwell
        k.clear(X(3108), 5, Z(3362), X(3109) + 1, 8, Z(3361));
        for (int y : new int[]{0}) {
            doorZ(k, Z(3363), 3103, y, "south");                  // library -> corridor
            doorZ(k, Z(3365), 3099, y, "south");                  // strip -> north-west room
            doorX(k, X(3102), 3364, y, "east");                   // strip -> corridor
            doorX(k, X(3102), 3370, y, "east");                   // north-west room -> north hall
            doorZ(k, Z(3367), 3103, y, "south");                  // corridor -> north hall
            doorZ(k, Z(3367), 3106, y, "south");                  // small room -> north hall
            doorX(k, X(3108), 3366, y, "east");                   // small room -> tube room
            doorX(k, X(3105), 3360, y, "east");                   // library -> staircase hall
            doorZ(k, Z(3356), 3109, y, "south");                  // entrance -> staircase hall
            doorX(k, X(3120), 3355, y, "east");                   // ballroom -> east wing
            k.clear(X(3113), y, Z(3361), X(3113), y + 2, Z(3360) + 1);   // staircase hall -> ballroom arch
            k.clear(X(3115), y, Z(3367), X(3116) + 1, y + 2, Z(3367));   // ballroom -> north hall
        }
        // the secret bookcase (3097, 3357-3358) and the lever behind it (3096, 3357)
        k.fill(X(3097), 0, Z(3358), X(3097), 2, Z(3357) + 1, BOOKSHELF);
        k.clear(X(3097), 0, Z(3358) + 1, X(3097), 1, Z(3357));
        k.set(X(3097) - 1, 1, Z(3357) + 1, "minecraft:lever[face=wall,facing=west]");
        // windows on the outer faces, both storeys
        for (int y : new int[]{1, 6}) {
            for (int x = X(3092); x <= X(3126); x += 6) {
                if (x < X(3107) || x > X(3110) + 1) {
                    k.replace(new Box(x, sz, x + 1, sz), y, y + 1, wall, win);
                }
            }
            for (int x = X(3098); x <= X(3118); x += 6) {
                k.replace(new Box(x, nwRoom.z1(), x + 1, nwRoom.z1()), y, y + 1, wall, win);
            }
            for (int z = Z(3361); z <= Z(3355); z += 6) {
                k.replace(new Box(wWing.x1(), z, wWing.x1(), z + 1), y, y + 1, wall, win);
                k.replace(new Box(eWing.x2(), z, eWing.x2(), z + 1), y, y + 1, wall, win);
            }
            for (int z = Z(3372); z <= Z(3362); z += 5) {
                k.replace(new Box(northHall.x2(), z, northHall.x2(), z + 1), y, y + 1, wall, win);
            }
        }

        // ---- roofs: the wings, the south and north ranges, then the top floor block above them
        k.roof(hull(3091, 3354, 3119, 3362), 10, Roof.BLACKSTONE, wall);
        k.roof(hull(3097, 3363, 3119, 3373), 10, Roof.BLACKSTONE, wall);
        k.roof(eWing, 5, Roof.BLACKSTONE, wall);
        Box top = hull(3104, 3356, 3112, 3370);
        k.clear(top.grow(1), 10, 30);
        k.fill(top, 9, 14, wall).walls(top, 9, 9, trim);
        k.clear(top.inset(1), 10, 13).fill(top.inset(1), 9, 9, DARK_PLANKS);
        for (int z = Z(3369); z <= Z(3357); z += 5) {
            k.replace(new Box(top.x1(), z, top.x1(), z + 1), 11, 12, wall, win).replace(new Box(top.x2(), z, top.x2(), z + 1), 11, 12, wall, win);
        }
        k.roof(top, 15, Roof.BLACKSTONE, wall);
        // the top floor's partition (OSRS plane 2: x 3108 wall, the landing at 3105-3107)
        k.fill(X(3108), 10, top.z1() + 1, X(3108), 13, Z(3362), wall);
        k.clear(X(3108), 10, Z(3366), X(3108), 12, Z(3366) + 1);
        k.fill(top.x1() + 1, 10, Z(3361), X(3112) + 1, 13, Z(3361), wall);
        k.clear(X(3110), 10, Z(3361), X(3110) + 1, 12, Z(3361));

        // ---- stairs: the grand staircase (3108-3109, 3361) up to the first floor, then (3106, 3362) to the top
        k.stairs2(X(3108) + 1, 0, Z(3360) + 1, 'n', 5, "minecraft:dark_oak_stairs", 'e');
        k.fill(X(3108), 0, Z(3360) + 1, X(3108), 0, Z(3362), "minecraft:dark_oak_fence");
        k.stairs2(X(3106), 5, Z(3360) + 1, 'n', 5, "minecraft:dark_oak_stairs", 'e');
        k.carpet(new Box(X(3108), Z(3359), X(3109) + 1, Z(3357) + 1), 0, "red");
        // the ballroom stairs down (3115, 3356), railed, into Count Draynor's crypt (OSRS: his basement at
        // 3077, 9771, a separate area; here a sealed vault under the ballroom and east wing, walled off from
        // the lever cellar)
        Box well = t(3114, 3357, 3117, 3359);
        k.stairs2(well.x2(), -8, well.z1(), 'w', 8, "minecraft:dark_oak_stairs", 's');
        k.connect(well.x1(), 0, well.z1() - 1, well.x2() + 1, 0, well.z1() - 1, "minecraft:dark_oak_fence");
        k.connect(well.x1(), 0, well.z2() + 1, well.x2() + 1, 0, well.z2() + 1, "minecraft:dark_oak_fence");
        k.connect(well.x2() + 1, 0, well.z1() - 1, well.x2() + 1, 0, well.z2() + 1, "minecraft:dark_oak_fence");

        // ---- furnishing, ground floor
        Box li = library.inset(1);
        k.shelvesX(li.x1(), li.x2(), 0, li.z1(), "south");
        k.fill(li.x1() + 3, 0, Z(3359), li.x2() - 3, 1, Z(3359), BOOKSHELF);
        k.fill(li.x1() + 3, 0, Z(3356), li.x2() - 3, 1, Z(3356), BOOKSHELF);
        k.set(li.x2() - 1, 0, li.z2(), "minecraft:lectern[facing=north]");
        k.hang(X(3101), 2, Z(3360));
        Box wi = wWing.inset(1);
        k.set(wi.x1() + 1, -1, Z(3361), "minecraft:dark_oak_trapdoor[half=top,facing=east]");
        k.set(wi.x2(), 0, wi.z1(), "minecraft:cobweb").set(wi.x1(), 0, wi.z2(), "minecraft:chest[facing=north]");
        k.hang(wi.cx(), 2, wi.cz());
        Box nw = nwRoom.inset(1);
        k.set(nw.x1(), 0, nw.z1(), "minecraft:cauldron").set(nw.x1() + 1, 0, nw.z1(), "minecraft:brewing_stand");
        k.table(nw.x2() - 1, 0, nw.z1() + 2).set(nw.x1(), 0, nw.z2(), "minecraft:cobweb");
        Box nh = northHall.inset(1);
        k.diningX(X(3109), X(3115), 0, nh.cz());
        k.set(nh.x2(), 0, nh.z1(), RANGE).set(nh.x2() - 1, 0, nh.z1(), BARREL).set(nh.x2() - 2, 0, nh.z1(), "minecraft:cauldron");
        k.carpet(new Box(nh.x1(), nh.cz() + 2, nh.x1() + 6, nh.cz() + 3), 0, "red");
        k.hang(X(3105), 2, nh.cz()).hang(X(3112), 2, nh.cz());
        Box tr = tubeRoom.inset(1);
        k.set(tr.x2(), 0, tr.z1(), "minecraft:chest[facing=south]").set(tr.x2() - 1, 0, tr.z1(), BARREL);
        k.set(smallRoom.inset(1).x1(), 0, smallRoom.inset(1).z1(), "minecraft:cobweb");
        k.hang(X(3109), 2, Z(3359)).hang(X(3116), 2, Z(3362)).hang(X(3116), 2, Z(3355));
        Box ew = eWing.inset(1);
        k.set(ew.x1(), 0, ew.z2(), BARREL).set(ew.x1() + 1, 0, ew.z2(), "minecraft:chest[facing=north]").set(ew.x2(), 0, ew.cz(), "minecraft:cobweb");
        k.hang(ew.cx(), 2, ew.cz());
        // ---- first floor: bedrooms in the west, the long gallery north, the landing
        for (Box r : new Box[]{wWing, library, nwRoom}) {
            Box in = r.inset(1);
            k.bed(in.x1(), 5, in.z1(), 's', "black");
            k.set(in.x1() + 2, 5, in.z1(), "minecraft:chest[facing=south]");
            k.hang(in.cx(), 7, in.cz());
        }
        k.carpet(new Box(nh.x1(), nh.cz() - 1, nh.x2(), nh.cz() + 1), 5, "red");
        k.shelvesX(nh.x1() + 2, nh.x2() - 2, 5, nh.z1(), "south");
        k.set(nh.cx(), 5, nh.z2(), "minecraft:cobweb");
        k.diningZ(bIn.cx(), 5, Z(3360), Z(3355));
        k.bed(uNe.inset(1).x2() - 1, 5, uNe.inset(1).z1(), 's', "black");
        // ---- top floor: Professor Oddenstein's laboratory, the whistles' room
        Box ti = top.inset(1);
        k.set(X(3111), 10, Z(3368), "minecraft:brewing_stand").set(X(3110), 10, Z(3368), "minecraft:cauldron");
        k.set(X(3112), 10, Z(3366), "minecraft:crafting_table").set(X(3112), 10, Z(3364), "minecraft:enchanting_table");
        k.set(X(3109) + 1, 10, Z(3369), "minecraft:lightning_rod");
        k.set(ti.x1(), 10, Z(3359), "minecraft:chest[facing=east]").set(ti.x1(), 10, Z(3358), BOOKSHELF);
        k.hang(X(3110), 12, Z(3366)).hang(X(3108), 12, Z(3358));

        // ---- the lever-puzzle cellar: floor -9, rooms -8..-4, a thick ceiling (OSRS 3090-3118, 9744-9767, +1/+6406)
        Box[] cellar = {cellar(3096, 3357, 3112, 3367), cellar(3090, 3352, 3095, 3357), cellar(3096, 3350, 3112, 3356),
                cellar(3100, 3344, 3113, 3349)};
        for (Box r : cellar) {
            k.fill(r.grow(1), -10, -2, "minecraft:cobbled_deepslate");
        }
        for (Box r : cellar) {
            k.clear(r, -8, -4);
            k.fill(r, -9, -9, "minecraft:deepslate_tiles");
        }
        String cw = "minecraft:deepslate_bricks";
        // the room grid: walls at x 3100 and 3105, y 3363 and 3357 (shown coordinates), with doors
        int[] ys = {cz(3363), cz(3357)};
        k.fill(cx(3100), -8, cz(3367), cx(3100), -4, cz(3344), cw);
        k.fill(cx(3105), -8, cz(3367), cx(3105), -4, cz(3344), cw);
        k.fill(cx(3096), -8, ys[0], cx(3112), -4, ys[0], cw);
        k.fill(cx(3096), -8, ys[1], cx(3113), -4, ys[1], cw);
        k.fill(cx(3100), -8, cz(3350), cx(3113), -4, cz(3350), cw);
        k.fill(cx(3096), -8, cz(3356), cx(3096), -4, cz(3352) + 1, cw);
        for (int[] dz : new int[][]{{3100, 3365}, {3105, 3360}, {3100, 3354}, {3105, 3352}, {3105, 3347}, {3096, 3354}}) {
            k.clear(cx(dz[0]), -8, cz(dz[1]), cx(dz[0]), -6, cz(dz[1]) + 1);
            k.set(cx(dz[0]), -8, cz(dz[1]), "minecraft:dark_oak_door[facing=east,half=lower,hinge=left]");
            k.set(cx(dz[0]), -8, cz(dz[1]) + 1, "minecraft:dark_oak_door[facing=east,half=lower,hinge=right]");
        }
        for (int[] dx : new int[][]{{3098, 3363}, {3102, 3363}, {3108, 3363}, {3098, 3357}, {3108, 3357}, {3102, 3350}, {3110, 3350}}) {
            k.clear(cx(dx[0]), -8, cz(dx[1]), cx(dx[0]) + 1, -6, cz(dx[1]));
            k.set(cx(dx[0]), -8, cz(dx[1]), "minecraft:dark_oak_door[facing=south,half=lower,hinge=left]");
            k.set(cx(dx[0]) + 1, -8, cz(dx[1]), "minecraft:dark_oak_door[facing=south,half=lower,hinge=right]");
        }
        // levers A-F on the walls, the ladder (west arm, under 3092, 3361) and the OSRS ladder spot (3117, 9753)
        for (int[] lv : new int[][]{{3098, 3366}, {3103, 3366}, {3108, 3366}, {3092, 3356}, {3110, 3353}, {3103, 3345}}) {
            k.set(cx(lv[0]), -6, cz(lv[1]), "minecraft:lever[face=wall,facing=south]");
        }
        int lx = X(3092);
        int lz = Z(3361);
        k.fill(lx - 1, -8, lz, lx - 1, -2, lz + 1, "minecraft:cobbled_deepslate");
        k.ladder(lx, lz, "east", -8, -2).ladder(lx, lz + 1, "east", -8, -2);
        k.set(lx, -1, lz + 1, "minecraft:dark_oak_trapdoor[half=top,facing=east]");
        k.set(cx(3112) + 1, -8, cz(3353), "minecraft:chest[facing=west]");
        // Count Draynor's crypt at the foot of the ballroom stairs: his coffin on a dais, candles
        Box crypt = t(3115, 3354, 3126, 3360);
        k.fill(crypt.grow(1), -10, -2, "minecraft:cobbled_deepslate");
        k.clear(crypt, -8, -4).fill(crypt, -9, -9, "minecraft:polished_blackstone_bricks");
        k.stairs2(well.x2(), -8, well.z1(), 'w', 8, "minecraft:dark_oak_stairs", 's');
        int cfx = X(3123);
        int cfz = Z(3357);
        k.fill(cfx - 1, -9, cfz - 1, cfx + 2, -9, cfz + 2, "minecraft:polished_blackstone");
        k.fill(cfx, -8, cfz, cfx + 1, -8, cfz, "minecraft:dark_oak_slab[type=bottom]");
        k.set(cfx - 1, -8, cfz - 1, "minecraft:candle[candles=3,lit=true]").set(cfx + 2, -8, cfz + 1, "minecraft:candle[candles=3,lit=true]");
        k.set(X(3120), -4, Z(3357), "minecraft:soul_lantern[hanging=true]").set(X(3125), -4, Z(3355), "minecraft:soul_lantern[hanging=true]");
        k.set(crypt.x2(), -8, crypt.z1(), "minecraft:cobweb").set(crypt.x2(), -8, crypt.z2(), "minecraft:cobweb");
        for (int[] l : new int[][]{{3093, 3354}, {3098, 3360}, {3103, 3360}, {3108, 3360}, {3108, 3353}, {3102, 3353}, {3107, 3346}}) {
            k.set(cx(l[0]), -4, cz(l[1]), "minecraft:soul_lantern[hanging=true]");
        }

        // ---- the grounds: the fountain (3088, 3334), the compost heap (3085, 3361), graves and dead trees
        int fx = X(3088) + 1;
        int fz = Z(3334) + 1;
        k.discClear(fx, fz, 5, 0, 1);
        k.ring(fx, fz, 5, 1, 0, 0, "minecraft:stone_bricks");
        k.disc(fx, fz, 4, -2, -1, WATER);
        k.fill(fx, -1, fz, fx, 1, fz, "minecraft:mossy_stone_bricks");
        k.set(fx, 2, fz, "minecraft:mossy_stone_brick_wall");
        k.fill(X(3085), 0, Z(3361), X(3085) + 1, 0, Z(3361) + 1, "minecraft:composter");
        for (int x = X(3114); x <= X(3124); x += 3) {
            k.grave(x, Z(3378));
        }
        for (int[] d : new int[][]{{3087, 3377}, {3093, 3376}, {3124, 3366}, {3088, 3346}, {3127, 3344}, {3100, 3340}, {3118, 3341}}) {
            k.deadTree(X(d[0]), Z(d[1]));
        }
        k.tree(X(3089), Z(3368), 6, DARK_LOG, DARK_LEAVES);
        k.tree(X(3128), Z(3356), 6, DARK_LOG, DARK_LEAVES);
        return k.build();
    }

    /** Block box of a tile rectangle whose east and south walls sit on the neighbouring tiles' edge blocks. */
    private static Box hull(int tx1, int ty1, int tx2, int ty2) {
        Box b = t(tx1, ty1, tx2, ty2);
        return new Box(b.x1(), b.z1(), b.x2() + 1, b.z2() + 1);
    }

    /** Draynor Manor cellar: block X of a tile in the OSRS cellar's (shown) coordinates. */
    private static int cx(int shownX) {
        return X(shownX + 1);
    }

    /** Draynor Manor cellar: north block Z of a tile in the OSRS cellar's (shown, y - 6400) coordinates. */
    private static int cz(int shownY) {
        return Z(shownY + 6);
    }

    private static Box cellar(int sx1, int sy1, int sx2, int sy2) {
        return t(sx1 + 1, sy1 + 6, sx2 + 1, sy2 + 6);
    }

    /** A double door in a wall running east-west (block row z) at tile x. */
    private static void doorZ(MisthalinKit k, int z, int tx, int y, String facing) {
        k.clear(X(tx), y, z, X(tx) + 1, y + 2, z);
        k.set(X(tx), y, z, "minecraft:dark_oak_door[facing=" + facing + ",half=lower,hinge=left]");
        k.set(X(tx) + 1, y, z, "minecraft:dark_oak_door[facing=" + facing + ",half=lower,hinge=right]");
    }

    /** A double door in a wall running north-south (block column x) at tile y. */
    private static void doorX(MisthalinKit k, int x, int ty, int y, String facing) {
        k.clear(x, y, Z(ty), x, y + 2, Z(ty) + 1);
        k.set(x, y, Z(ty), "minecraft:dark_oak_door[facing=" + facing + ",half=lower,hinge=left]");
        k.set(x, y, Z(ty) + 1, "minecraft:dark_oak_door[facing=" + facing + ",half=lower,hinge=right]");
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

    /**
     * The Wizards' Tower, traced from the OSRS map (mejrs layers_osrs, planes 0-2 and the basement) and the
     * shortest-path transports: an octagonal tower 13 tiles across (3103-3115, 3154-3166) with a north-west
     * annex holding the staircase (3103-3105, 3159-3161) and the basement ladder (3104, 3162), on an island joined to
     * Draynor's south shore by a long bridge (3112-3115). Three storeys: the ground floor with the north
     * entrance hall, Wizard Traiborn's floor (two halves split at x 3110) and the top floor with Wizard
     * Mizgog and the caged lesser demon (3113, 3162). The basement (Sedridor's study, the Rune Mysteries and
     * Restless Ghost basement) keeps its OSRS layout 6400 tiles south of the instance, under the tower and
     * the island's north half; a short passage joins the ladder foot to its west hall.
     */
    static StructureDef wizardsTower() {
        MisthalinKit k = new MisthalinKit("wizards_tower", "Wizards' Tower", "WIZARDS_TOWER");
        k.tFlatten(3086, 3138, 3132, 3209);
        Box all = t(3086, 3138, 3132, 3209);
        // open water around the island, the shore at the north edge
        k.fill(all, -4, -4, DIRT);
        Box shore = t(3086, 3205, 3132, 3209);
        k.fill(shore, -3, -2, DIRT).fill(shore, -1, -1, GRASS);
        k.fill(t(3086, 3204, 3132, 3204), -2, -2, DIRT).fill(t(3086, 3204, 3132, 3204), -1, -1, "minecraft:sand");
        int[] island = {3094, 3160, 3094, 3165, 3096, 3171, 3099, 3174, 3103, 3177, 3109, 3178, 3112, 3172, 3116, 3173,
                3120, 3173, 3123, 3171, 3124, 3166, 3125, 3161, 3125, 3157, 3122, 3152, 3119, 3150, 3115, 3147, 3110, 3144,
                3104, 3145, 3100, 3149, 3099, 3153, 3096, 3156};
        polygon(k, island, 0, t(3086, 3138, 3132, 3203), -3, -1, WATER, true);
        polygon(k, island, 0, all, -3, -2, DIRT, false);
        polygon(k, island, 0, all, -1, -1, "minecraft:sand", false);
        polygon(k, island, 1.5, all, -1, -1, GRASS, false);
        // paved forecourt north of the door, with the well (3117, 3166)
        k.floor(t(3107, 3167, 3116, 3171), "minecraft:polished_andesite");
        k.floor(t(3109, 3167, 3110, 3170), STONE_BRICKS);
        int wx = X(3117);
        int wz = Z(3166);
        k.fill(wx, 0, wz, wx + 1, 0, wz + 1, "minecraft:stone_brick_wall");
        k.fill(wx, -1, wz, wx + 1, -1, wz + 1, WATER).clear(wx, 0, wz, wx + 1, 0, wz + 1);
        k.fill(wx - 1, 0, wz - 1, wx + 2, 0, wz - 1, "minecraft:stone_brick_wall").fill(wx - 1, 0, wz + 2, wx + 2, 0, wz + 2, "minecraft:stone_brick_wall");
        k.fill(wx - 1, 0, wz, wx - 1, 0, wz + 1, "minecraft:stone_brick_wall").fill(wx + 2, 0, wz, wx + 2, 0, wz + 1, "minecraft:stone_brick_wall");
        // the bridge to the north shore: grey stone with low parapets
        Box bridge = t(3112, 3171, 3115, 3205);
        k.fill(bridge, -1, -1, STONE_BRICKS).clear(bridge, 0, 3);
        k.fill(bridge.x1(), 0, bridge.z1(), bridge.x1(), 0, bridge.z2() - 2, "minecraft:stone_brick_wall");
        k.fill(bridge.x2(), 0, bridge.z1(), bridge.x2(), 0, bridge.z2() - 2, "minecraft:stone_brick_wall");
        for (int z = bridge.z1() + 6; z < bridge.z2() - 4; z += 10) {
            k.fill(bridge.x1(), -4, z, bridge.x1() + 1, -2, z + 1, STONE_BRICKS).fill(bridge.x2() - 1, -4, z, bridge.x2(), -2, z + 1, STONE_BRICKS);
            k.set(bridge.x1(), 1, z, LANTERN).set(bridge.x2(), 1, z, LANTERN);
        }
        k.dirtPath(t(3113, 3206, 3114, 3209));
        // rocks either side of the bridge foot, trees and bushes on the island
        for (int[] r : new int[][]{{3110, 3203}, {3108, 3204}, {3117, 3203}, {3119, 3202}}) {
            k.fill(X(r[0]), 0, Z(r[1]), X(r[0]) + 1, 0, Z(r[1]) + 1, "minecraft:cobblestone");
        }
        k.tOak(3105, 3171).tOak(3104, 3150);
        for (int[] bsh : new int[][]{{3119, 3161}, {3119, 3157}, {3116, 3151}, {3113, 3150}}) {
            k.bush(X(bsh[0]), Z(bsh[1]), OAK_LEAVES);
        }

        // ---- the tower: octagon body plus the north-west annex, walls 0..16, storeys of 6
        Box body = t(3103, 3154, 3115, 3166);
        Box annex = t(3102, 3157, 3107, 3165);
        int cb = 7;
        int ca = 2;
        octagon(k, body, cb, -3, 17, STONE_BRICKS);
        octagon(k, annex, ca, -3, 17, STONE_BRICKS);
        for (int y : new int[]{-1, 5, 11, 17}) {
            octagon(k, body.grow(1), cb + 1, y, y, POLISHED_ANDESITE);
            octagon(k, annex.grow(1), ca + 1, y, y, POLISHED_ANDESITE);
        }
        for (int s = 0; s < 3; s++) {
            octagon(k, body.inset(1), cb, s * 6, s * 6 + 4, null);
            octagon(k, annex.inset(1), ca, s * 6, s * 6 + 4, null);
            String fl = s == 0 ? POLISHED_ANDESITE : SPRUCE_PLANKS;
            octagon(k, body.inset(1), cb, s * 6 - 1, s * 6 - 1, fl);
            octagon(k, annex.inset(1), ca, s * 6 - 1, s * 6 - 1, fl);
        }
        k.disc(X(3109), Z(3160) + 1, 2, -1, -1, "minecraft:lapis_block");
        // windows on every straight face of every storey
        for (int s = 0; s < 3; s++) {
            int y = s * 6 + 2;
            for (int x : new int[]{X(3111), X(3113)}) {
                k.fill(x, y, body.z1(), x, y + 1, body.z1(), GLASS).fill(x, y, body.z2(), x, y + 1, body.z2(), GLASS);
            }
            k.fill(X(3107), y, body.z2(), X(3107), y + 1, body.z2(), GLASS);
            for (int z : new int[]{Z(3158), Z(3161), Z(3163)}) {
                k.fill(body.x2(), y, z, body.x2(), y + 1, z, GLASS);
            }
            k.fill(annex.x1(), y, Z(3162), annex.x1(), y + 1, Z(3162), GLASS);
            k.fill(body.x1(), y, Z(3155) - 1, body.x1() + 1, y + 1, Z(3155) - 1, GLASS);
        }
        // north door (3109, 3166) under a stone hood
        int dx = X(3109);
        k.clear(dx, 0, body.z1(), dx + 1, 2, body.z1());
        k.set(dx, 0, body.z1(), "minecraft:spruce_door[facing=north,half=lower,hinge=left]");
        k.set(dx + 1, 0, body.z1(), "minecraft:spruce_door[facing=north,half=lower,hinge=right]");
        k.fill(dx - 1, 0, body.z1() - 1, dx - 1, 3, body.z1() - 1, CHISELED).fill(dx + 2, 0, body.z1() - 1, dx + 2, 3, body.z1() - 1, CHISELED);
        k.fill(dx - 1, 4, body.z1() - 1, dx + 2, 4, body.z1() - 1, "minecraft:stone_brick_slab[type=bottom]");
        k.set(dx - 1, 2, body.z1() - 2, "minecraft:wall_torch[facing=north]").set(dx + 2, 2, body.z1() - 2, "minecraft:wall_torch[facing=north]");
        // slate roof: stepped octagonal cones over the body and the annex
        for (int i = 0; ; i++) {
            Box r = body.grow(1).inset(2 * i);
            if (r.x2() - r.x1() < 2) {
                k.fill(r.cx(), 18 + i, r.cz(), r.cx() + 1, 18 + i, r.cz() + 1, "minecraft:deepslate_tiles");
                k.set(r.cx(), 19 + i, r.cz(), "minecraft:lightning_rod");
                break;
            }
            octagon(k, r, Math.max(0, cb + 1 - 2 * i), 18 + i, 18 + i, "minecraft:deepslate_tiles");
        }
        for (int i = 0; i < 3; i++) {
            Box r = annex.grow(1).inset(2 * i);
            octagon(k, r, Math.max(0, ca + 1 - 2 * i), 18 + i, 18 + i, "minecraft:deepslate_tiles");
        }

        // ---- ground floor: the entrance hall between two diagonal walls, the stair annex to the west
        int cx = X(3109);
        int cz = Z(3160);
        for (int i = 0; i <= 6; i++) {
            k.fill(cx - i - 1, 0, cz - i, cx - i, 4, cz - i, STONE_BRICKS);   // to the north-west
            k.fill(cx + i + 1, 0, cz - i, cx + i + 2, 4, cz - i, STONE_BRICKS); // to the north-east
        }
        k.clear(cx - 4, 0, cz - 4, cx - 3, 2, cz - 3);   // door (3106, 3162)
        k.clear(cx + 4, 0, cz - 3, cx + 5, 2, cz - 2);   // door (3111, 3162)
        k.carpet(new Box(dx, body.z1() + 1, dx + 1, cz - 1), 0, "blue");
        k.set(dx - 2, 3, body.z1() + 2, LANTERN_HANG).set(dx + 3, 3, body.z1() + 2, LANTERN_HANG);
        // the stair annex: short walls at y 3158 and x 3107 (OSRS plane 0)
        k.fill(X(3104), 0, Z(3158), X(3106) + 1, 4, Z(3158), STONE_BRICKS);
        k.fill(X(3107), 0, Z(3160), X(3107), 4, Z(3158), STONE_BRICKS);
        k.clear(X(3105), 0, Z(3158), X(3105) + 1, 2, Z(3158));
        // the main hall to the south: the rune shop counter, bookshelves and the wizards' tables
        k.set(X(3109), 0, Z(3156), RUNE_SHOP);
        k.fill(X(3108), 0, Z(3156), X(3108), 0, Z(3156), "minecraft:spruce_planks").fill(X(3110), 0, Z(3156), X(3110), 0, Z(3156), "minecraft:spruce_planks");
        k.fill(X(3107), 0, body.z2() - 1, X(3111) + 1, 1, body.z2() - 1, BOOKSHELF);
        k.table(X(3113), 0, Z(3158)).chair(X(3113) + 1, 0, Z(3158), "west").table(X(3105), 0, Z(3156)).chair(X(3105), 0, Z(3156) - 1, "south");
        k.set(X(3114), 0, Z(3160), "minecraft:enchanting_table").set(X(3112), 0, Z(3155), "minecraft:lectern[facing=north]");
        k.set(X(3109), 3, Z(3157), LANTERN_HANG).set(X(3113), 3, Z(3160), LANTERN_HANG);
        // the staircase (OSRS 3103-3105 x 3159-3161: up from 3105,3159 to 3104,3161 on each floor) as two flights,
        // north up the annex's west side to the first floor, then south beside it to the top floor; the
        // basement ladder (3104, 3162)
        for (int i = 0; i < 6; i++) {
            int z1 = Z(3159) + 1 - i;
            k.clear(X(3103), i, z1, X(3103) + 1, i + 3, z1);
            if (i > 0) {
                k.fill(X(3103), 0, z1, X(3103) + 1, i - 1, z1, STONE_BRICKS);
            }
            k.fill(X(3103), i, z1, X(3103) + 1, i, z1, "minecraft:stone_brick_stairs[facing=north,half=bottom]");
            int z2 = Z(3161) + i;
            k.clear(X(3104), 6 + i, z2, X(3104) + 1, 9 + i, z2);
            if (i > 0) {
                k.fill(X(3104), 6, z2, X(3104) + 1, 5 + i, z2, STONE_BRICKS);
            }
            k.fill(X(3104), 6 + i, z2, X(3104) + 1, 6 + i, z2, "minecraft:stone_brick_stairs[facing=south,half=bottom]");
        }
        int lx = X(3104);
        int lz = Z(3162) + 1;
        k.set(lx, -1, lz, "minecraft:spruce_trapdoor[half=top,facing=north]");

        // ---- first floor (feet 6): split at x 3110; Wizard Traiborn's north-east study, store south-east, library west
        int y1 = 6;
        k.fill(X(3110), y1, body.z1(), X(3110), y1 + 4, body.z2(), STONE_BRICKS);
        k.clear(X(3110), y1, Z(3162), X(3110), y1 + 2, Z(3162) + 1);
        k.clear(X(3110), y1, Z(3159), X(3110), y1 + 2, Z(3159) + 1);
        k.fill(X(3110), y1, Z(3160) + 1, body.x2(), y1 + 4, Z(3160) + 1, STONE_BRICKS);
        k.fill(X(3104), y1, Z(3157), X(3109) + 1, y1 + 4, Z(3157), STONE_BRICKS);
        k.clear(X(3108), y1, Z(3157), X(3108) + 1, y1 + 2, Z(3157));
        k.shelvesX(X(3111), X(3114), y1, body.z1() + 1, "south");
        k.fill(body.x2() - 1, y1, Z(3164), body.x2() - 1, y1 + 1, Z(3162), BOOKSHELF);
        k.table(X(3113), y1, Z(3162)).chair(X(3112), y1, Z(3162), "east");
        k.set(X(3114), y1, Z(3164), "minecraft:chest[facing=west]");
        k.set(X(3112), y1, Z(3157), BARREL).set(X(3112) + 1, y1, Z(3157), BARREL);
        k.shelvesX(X(3104), X(3108), y1, body.z1() + 1, "south");
        k.diningZ(X(3106), y1, Z(3162), Z(3160));
        k.set(X(3109), y1 + 3, Z(3162), LANTERN_HANG).set(X(3113), y1 + 3, Z(3163), LANTERN_HANG).set(X(3113), y1 + 3, Z(3157), LANTERN_HANG);
        k.carpet(new Box(X(3105), Z(3156), X(3109) + 1, Z(3155) + 1), y1, "purple");

        // ---- top floor (feet 12): Wizard Mizgog's room to the west, the caged lesser demon north-east
        int y2 = 12;
        k.fill(X(3108), y2, body.z1(), X(3108), y2 + 4, Z(3161) + 1, STONE_BRICKS);
        k.clear(X(3108), y2, Z(3163), X(3108), y2 + 2, Z(3163) + 1);
        k.fill(X(3108), y2, Z(3160), body.x2(), y2 + 4, Z(3160), STONE_BRICKS);
        k.clear(X(3110), y2, Z(3160), X(3110) + 1, y2 + 2, Z(3160));
        Box cage = t(3112, 3161, 3114, 3164);
        k.walls(cage, y2, y2 + 3, BARS).fill(cage, y2 + 4, y2 + 4, "minecraft:iron_block");
        k.set(X(3113) + 1, y2 + 3, Z(3165), LANTERN_HANG);
        k.set(X(3104), y2, Z(3164), "minecraft:brewing_stand").set(X(3105), y2, Z(3164), "minecraft:cauldron");
        k.bed(X(3105), y2, Z(3161), 'e', "purple");
        k.shelvesX(X(3104), X(3107), y2, body.z2() - 1, "north");
        k.table(X(3112), y2, Z(3157)).chair(X(3111), y2, Z(3157), "east");
        k.set(X(3114), y2, Z(3156), "minecraft:enchanting_table");
        k.set(X(3106), y2 + 3, Z(3160), LANTERN_HANG).set(X(3111), y2 + 3, Z(3157), LANTERN_HANG);

        // ---- the basement (OSRS 9553-9577, drawn 6400 tiles north): floor -8, rooms -7..-3
        Box west = t(3097, 3165, 3107, 3174);
        Box alcove = t(3103, 3175, 3110, 3177);
        Box hall = t(3108, 3165, 3110, 3177);
        Box south = t(3106, 3155, 3110, 3164);
        Box eastN = t(3111, 3164, 3121, 3169);
        Box eastS = t(3111, 3155, 3119, 3163);
        Box passage = t(3104, 3162, 3104, 3164);
        for (Box r : new Box[]{west, alcove, hall, south, eastN, eastS, passage}) {
            k.fill(r.grow(1), -8, -2, "minecraft:cobbled_deepslate");
        }
        for (Box r : new Box[]{west, alcove, hall, south, eastN, eastS, passage}) {
            k.clear(r, -7, -3);
            k.fill(r, -8, -8, "minecraft:deepslate_tiles");
        }
        String bw = "minecraft:deepslate_bricks";
        // west hall | hall wall at x 3108 with its door (3108, 3170); the pillar block (3100, 3166)
        k.fill(X(3108), -7, Z(3174), X(3108), -3, Z(3165) + 1, bw).clear(X(3108), -7, Z(3170), X(3108), -5, Z(3170) + 1);
        k.fill(t(3100, 3165, 3101, 3167), -7, -3, bw);
        // the alcove's wall, open at (3103, 3175) and into the hall
        k.fill(X(3104), -7, Z(3175) + 1, X(3107) + 1, -3, Z(3175) + 1, bw);
        // south room: chamfered south-west corner
        for (int i = 0; i < 4; i++) {
            k.fill(X(3106) + i, -7, Z(3155) + 1 - (3 - i), X(3106) + i, -3, Z(3155) + 1, bw);
        }
        // east rooms: wall at x 3111 with the door (3111, 3159), the dividing wall at y 3164 open at x 3111-3112
        k.fill(X(3111), -7, Z(3169), X(3111), -3, Z(3155) + 1, bw).clear(X(3111), -7, Z(3159), X(3111), -5, Z(3159) + 1);
        k.clear(X(3111), -7, Z(3166), X(3111), -5, Z(3166) + 1);
        k.fill(X(3113), -7, Z(3164) + 1, eastN.x2(), -3, Z(3164) + 1, bw);
        k.fill(eastS.x2() + 1, -7, Z(3163), eastN.x2(), -3, Z(3155) + 1, bw);
        // a lower ceiling over the east rooms (y -3)
        k.fill(eastN, -3, -3, "minecraft:cobbled_deepslate").fill(eastS, -3, -3, "minecraft:cobbled_deepslate");
        // ladder foot under the trapdoor; the OSRS ladder (3104, 3176) in the alcove
        k.ladder(lx, lz, "north", -7, -2);
        k.ladder(X(3104), alcove.z1() + 1, "south", -7, -4);
        // Sedridor's study (west hall): bookshelves, his desk, the orb
        k.shelvesX(X(3097), X(3099), -7, west.z1(), "south");
        k.fill(west.x1(), -7, Z(3173), west.x1(), -6, Z(3166), BOOKSHELF);
        k.table(X(3104), -7, Z(3171)).chair(X(3104), -7, Z(3170) + 1, "north");
        k.set(X(3105), -7, Z(3171), "minecraft:lectern[facing=south]");
        k.set(X(3109), -7, Z(3160), "minecraft:amethyst_cluster");
        k.disc(X(3109), Z(3160), 1, -8, -8, "minecraft:crying_obsidian");
        k.set(X(3116), -7, Z(3157), BARREL).set(X(3117), -7, Z(3157), BARREL);
        k.set(X(3118), -7, Z(3168), "minecraft:chest[facing=south]");
        k.diningX(X(3114), X(3118), -7, Z(3166));
        for (int[] l : new int[][]{{3100, 3170}, {3105, 3168}, {3109, 3174}, {3108, 3158}, {3115, 3167}}) {
            k.set(X(l[0]), -3, Z(l[1]), LANTERN_HANG);
        }
        k.set(X(3116), -4, Z(3160), LANTERN_HANG);
        return k.build();
    }

    /** Fills (or clears, when block is null) an octagon: the box with its corners cut c blocks diagonally. */
    private static void octagon(MisthalinKit k, Box b, int c, int y1, int y2, String block) {
        for (int z = b.z1(); z <= b.z2(); z++) {
            int edge = Math.min(z - b.z1(), b.z2() - z);
            int cut = Math.max(0, c - edge);
            if (b.x1() + cut > b.x2() - cut) {
                continue;
            }
            if (block == null) {
                k.clear(b.x1() + cut, y1, z, b.x2() - cut, y2, z);
            } else {
                k.fill(b.x1() + cut, y1, z, b.x2() - cut, y2, z, block);
            }
        }
    }

    /**
     * Fills the blocks inside a polygon of OSRS tile centres ({x, y} pairs), shrunk by {@code inset} tiles
     * toward its centroid, clipped to {@code clip} (or the blocks outside it, row by row, when {@code outside}).
     */
    private static void polygon(MisthalinKit k, int[] tiles, double inset, Box clip, int y1, int y2, String block, boolean outside) {
        int n = tiles.length / 2;
        double mx = 0;
        double mz = 0;
        double[] px = new double[n];
        double[] pz = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = X(tiles[2 * i]) + 1.0;
            pz[i] = Z(tiles[2 * i + 1]) + 1.0;
            mx += px[i] / n;
            mz += pz[i] / n;
        }
        for (int i = 0; i < n; i++) {
            double dxp = px[i] - mx;
            double dzp = pz[i] - mz;
            double len = Math.sqrt(dxp * dxp + dzp * dzp);
            double f = len <= 0 ? 0 : Math.max(0, len - 2 * inset) / len;
            px[i] = mx + dxp * f;
            pz[i] = mz + dzp * f;
        }
        for (int z = clip.z1(); z <= clip.z2(); z++) {
            double zc = z + 0.5;
            List<Double> xs = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                if ((pz[i] <= zc && zc < pz[j]) || (pz[j] <= zc && zc < pz[i])) {
                    xs.add(px[i] + (zc - pz[i]) * (px[j] - px[i]) / (pz[j] - pz[i]));
                }
            }
            java.util.Collections.sort(xs);
            int from = clip.x1();
            for (int i = 0; i + 1 < xs.size(); i += 2) {
                int a = Math.max(clip.x1(), (int) Math.ceil(xs.get(i) - 0.5));
                int c = Math.min(clip.x2(), (int) Math.floor(xs.get(i + 1) - 0.5));
                if (outside) {
                    if (from <= a - 1) {
                        k.fill(from, y1, z, a - 1, y2, z, block);
                    }
                    from = Math.max(from, c + 1);
                } else if (a <= c) {
                    k.fill(a, y1, z, c, y2, z, block);
                }
            }
            if (outside && from <= clip.x2()) {
                k.fill(from, y1, z, clip.x2(), y2, z, block);
            }
        }
    }
}
