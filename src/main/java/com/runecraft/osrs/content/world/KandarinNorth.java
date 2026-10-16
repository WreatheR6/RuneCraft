package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

/** The north: Barbarian Outpost, the Lighthouse, Rellekka, Piscatoris, Trollheim and the Troll Stronghold. */
final class KandarinNorth {
    private KandarinNorth() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(barbarianOutpost());
        out.add(lighthouse());
        out.add(rellekka());
        out.add(rellekkaLonghall());
        out.add(rellekkaMarket());
        out.add(rellekkaDocks());
        out.add(piscatoris());
        out.add(trollheim());
        out.add(trollStronghold());
    }

    private static final String LOG = "minecraft:spruce_log";

    /** A palisade of logs with sharpened tops along a tile line. */
    static void palisade(KandarinKit k, int tx1, int ty1, int tx2, int ty2) {
        int ax = KandarinKit.x1(tx1, tx2);
        int az = KandarinKit.z1(ty1, ty2);
        int cx = KandarinKit.x2(tx1, tx2);
        int cz = KandarinKit.z2(ty1, ty2);
        if (cx - ax > cz - az) {
            k.fill(ax, -1, az, cx, 3, az, LOG);
            k.fill(ax, 4, az, cx, 4, az, "minecraft:spruce_fence");
        } else {
            k.fill(ax, -1, az, ax, 3, cz, LOG);
            k.fill(ax, 4, az, ax, 4, cz, "minecraft:spruce_fence");
        }
    }

    // ------------------------------------------------------------------ Barbarian Outpost

    static StructureDef barbarianOutpost() {
        KandarinKit k = new KandarinKit("barbarian_outpost", "Barbarian Outpost").replaces("mg_barbarian_outpost");
        k.flatten(2512, 3540, 2560, 3586);
        palisade(k, 2514, 3584, 2558, 3584);
        palisade(k, 2514, 3542, 2558, 3542);
        palisade(k, 2514, 3542, 2514, 3584);
        palisade(k, 2558, 3542, 2558, 3584);
        k.tClear(2558, 3563, 2558, 3564, 0, 4);
        k.tClear(2535, 3542, 2536, 3542, 0, 4);
        k.road(2530, 3543, 2536, 3571, PATH, "minecraft:coarse_dirt");
        k.road(2537, 3563, 2557, 3564, PATH, "minecraft:coarse_dirt");
        // Assault hall
        Pal hall = FREM_DARK.roof("thatch");
        Bld h = k.house(2523, 3572, 2539, 3582, 2, hall, 's', WIDE_DOOR);
        k.clear(h.x1() + 1, 3, h.z1() + 1, h.x2() - 1, 3, h.z2() - 1);
        k.longTable(h.x1() + 3, h.x2() - 3, 0, h.z1() + 4, "spruce");
        k.set(h.cx(), 0, h.z1() + 1, "runecraft:barbarian_assault_rewards");
        k.fill(h.cx() - 1, 0, h.cz() + 2, h.cx() + 1, 0, h.cz() + 2, "minecraft:campfire[lit=true]");
        for (int x = h.x1() + 2; x <= h.x2() - 2; x += 4) {
            k.fill(x, 1, h.z1() + 1, x + 1, 2, h.z1() + 1, "minecraft:red_wool");
        }
        String[] roles = {"attacker", "defender", "collector", "healer"};
        int ez = KandarinKit.Z(3570);
        for (int i = 0; i < 4; i++) {
            k.set(KandarinKit.X(2524 + i * 4 + (i >= 2 ? 4 : 0)), 0, ez, "runecraft:barbarian_assault_" + roles[i] + "_entrance");
        }
        k.set(KandarinKit.X(2530), 0, KandarinKit.Z(3571), "runecraft:barbarian_assault_entrance");
        // Second longhouse, fire pit and tents
        k.house(2542, 3570, 2554, 3578, 1, FREM_DARK.roof("thatch"), 'w', FURN);
        int fx = KandarinKit.X(2521);
        int fz = KandarinKit.Z(3560);
        k.ring(fx, fz, -1, -1, 3, COBBLE);
        k.set(fx, 0, fz, "minecraft:campfire[lit=true]");
        k.fill(fx - 2, 0, fz, fx - 2, 1, fz, "minecraft:spruce_fence");
        k.fill(fx + 2, 0, fz, fx + 2, 1, fz, "minecraft:spruce_fence");
        k.fill(fx - 2, 2, fz, fx + 2, 2, fz, "minecraft:stripped_spruce_log[axis=x]");
        k.tent(KandarinKit.X(2516), KandarinKit.Z(3550), 7, "brown");
        k.tent(KandarinKit.X(2516), KandarinKit.Z(3570), 7, "white");
        // Barbarian agility course (south-east)
        int ax = KandarinKit.X(2540);
        int az = KandarinKit.Z(3558);
        k.waterB(ax, az, ax + 10, az + 6, 3);
        k.fill(ax + 5, 0, az - 1, ax + 5, 7, az - 1, "minecraft:spruce_log");
        k.fill(ax + 5, 7, az - 1, ax + 5, 7, az + 7, "minecraft:spruce_log[axis=z]");
        k.fill(ax + 5, 2, az + 3, ax + 5, 6, az + 3, "minecraft:chain");
        k.fill(ax + 12, -1, az, ax + 12, -1, az + 14, "minecraft:oak_log[axis=z]");
        k.waterB(ax + 11, az + 1, ax + 11, az + 13, 2);
        k.waterB(ax + 13, az + 1, ax + 13, az + 13, 2);
        k.fill(ax + 2, 0, az + 12, ax + 8, 3, az + 12, "minecraft:scaffolding[distance=0]");
        k.fill(ax, 0, az + 16, ax + 8, 2, az + 16, "minecraft:stone_bricks");
        k.fill(ax, 3, az + 16, ax + 8, 3, az + 16, "minecraft:stone_brick_slab");
        for (int i = 0; i < 3; i++) {
            k.fill(ax + 14 + i * 3, 0, az + 18, ax + 14 + i * 3, 0, az + 22, "minecraft:cobblestone_wall");
        }
        k.fill(KandarinKit.X(2546), 0, KandarinKit.Z(3546), KandarinKit.X(2548), 0, KandarinKit.Z(3546), "minecraft:target");
        k.spruce(KandarinKit.X(2552), KandarinKit.Z(3580), 8);
        k.spruce(KandarinKit.X(2518), KandarinKit.Z(3580), 7);
        return k.build();
    }

    // ------------------------------------------------------------------ Lighthouse

    static StructureDef lighthouse() {
        KandarinKit k = new KandarinKit("lighthouse", "Lighthouse").replaces("lighthouse");
        k.flatten(2490, 3622, 2526, 3652);
        k.water(2490, 3622, 2495, 3652, 3);
        k.water(2496, 3646, 2526, 3652, 3);
        k.floor(2496, 3622, 2526, 3645, "minecraft:stone");
        k.floor(2509, 3622, 2526, 3645, SAND);
        for (int[] r : new int[][]{{2497, 3644, 3}, {2507, 3645, 2}, {2497, 3626, 2}, {2512, 3622, 2}}) {
            k.blob(KandarinKit.X(r[0]), 0, KandarinKit.Z(r[1]), r[2] + 1, r[2], r[2] + 1, "minecraft:stone", true);
        }
        int cx = KandarinKit.X(2503) + 1;
        int cz = KandarinKit.Z(3633) + 1;
        int h = 36;
        k.disc(cx, cz, -1, 0, 8, "minecraft:stone_bricks");
        k.towerB(cx, cz, 6, h, SB, "minecraft:spruce_planks", null, 'e', 6);
        for (int y = 4; y < h; y += 8) {
            k.ring(cx, cz, y, y + 1, 6, "minecraft:white_concrete");
        }
        // keep the windows in the white bands
        for (int y = 1; y < h - 1; y += 6) {
            k.set(cx + 6, y, cz, PANE);
            k.set(cx - 6, y, cz, PANE);
            k.set(cx, y, cz - 6, PANE);
        }
        k.clear(cx + 6, 0, cz, cx + 6, 1, cz);
        // gallery and lamp room
        k.ring(cx, cz, h - 1, h - 1, 7, "minecraft:stone_brick_slab[type=top]");
        k.ring(cx, cz, h - 1, h - 1, 8, "minecraft:stone_brick_slab[type=top]");
        k.ring(cx, cz, h, h, 8, "minecraft:iron_bars");
        k.ring(cx, cz, h, h + 3, 4, "minecraft:glass");
        k.fill(cx, h, cz, cx, h + 1, cz, "minecraft:sea_lantern");
        k.set(cx, h + 2, cz, "minecraft:glowstone");
        k.disc(cx, cz, h + 4, h + 4, 5, "minecraft:polished_deepslate");
        k.cone(cx, cz, h + 5, 4, 1, "minecraft:deepslate_tiles", "minecraft:lightning_rod");
        k.clear(cx + 4, h, cz, cx + 4, h + 1, cz);
        // Larrissa's cottage and the path
        k.house(2495, 3624, 2500, 3629, 1, FREM, 'n', FURN);
        k.road(2507, 3626, 2508, 3633, PATH, GRAVEL);
        return k.build();
    }

    // ------------------------------------------------------------------ Rellekka

    static StructureDef rellekka() {
        KandarinKit k = new KandarinKit("rellekka", "Rellekka").replaces("RELLEKKA");
        k.flatten(2606, 3640, 2694, 3712);
        k.floor(2606, 3692, 2652, 3695, SAND);
        k.water(2606, 3696, 2652, 3712, 4);
        palisade(k, 2610, 3642, 2690, 3642);
        palisade(k, 2610, 3642, 2610, 3691);
        palisade(k, 2690, 3642, 2690, 3694);
        palisade(k, 2653, 3694, 2690, 3694);
        k.tClear(2658, 3642, 2659, 3642, 0, 4);
        for (int tx : new int[]{2657, 2660}) {
            k.tFill(tx, 3642, tx, 3642, -1, 6, "minecraft:stripped_spruce_log");
            k.at(tx, 3642, 7, "minecraft:carved_pumpkin[facing=south]");
        }
        k.road(2658, 3643, 2659, 3669, PATH, GRAVEL);
        k.road(2612, 3660, 2688, 3661, PATH, GRAVEL);
        k.road(2633, 3662, 2634, 3692, PATH, GRAVEL);
        k.road(2640, 3656, 2654, 3668, "minecraft:coarse_dirt", GRAVEL);

        // the market square (2636-2649 x 3670-3682) is open ground; its stalls are rellekkaMarket()
        // smithy and range house
        Bld sm = k.house(2664, 3645, 2670, 3651, 1, FREM_DARK, 'w', NO_LADDER);
        k.set(sm.x2() - 1, 0, sm.z1() + 1, FURNACE);
        k.set(sm.cx(), 0, sm.cz(), ANVIL);
        k.set(sm.x2() - 1, 0, sm.z2() - 1, "minecraft:water_cauldron[level=3]");
        k.set(sm.x2() - 1, 0, sm.z1() + 2, "minecraft:grindstone[face=floor,facing=west]");
        Bld rh = k.house(2640, 3645, 2648, 3651, 1, FREM.roof("minecraft:spruce"), 'n', FURN);
        k.set(rh.cx(), 0, rh.z2() - 1, RANGE);
        // longhouses
        int[][] hs = {{2614, 3646, 2624, 3652, 'n'}, {2626, 3645, 2636, 3651, 'n'}, {2614, 3664, 2624, 3670, 's'},
                {2614, 3676, 2624, 3682, 's'}, {2626, 3676, 2631, 3684, 'e'}, {2627, 3663, 2631, 3669, 'e'}, {2678, 3684, 2688, 3690, 'w'}, {2614, 3685, 2624, 3690, 's'}};
        for (int i = 0; i < hs.length; i++) {
            int[] h = hs[i];
            k.house(h[0], h[1], h[2], h[3], 1, i % 2 == 0 ? FREM.roof("minecraft:spruce") : FREM_DARK, (char) h[4], FURN);
        }
        // rooftop course houses (flat sod roofs)
        Pal flat = FREM.roof("flat:minecraft:spruce_planks");
        Bld r0 = k.house(2672, 3664, 2678, 3670, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld r1 = k.house(2672, 3674, 2678, 3680, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld r2 = k.house(2682, 3674, 2688, 3680, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld r3 = k.house(2682, 3662, 2688, 3668, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld r4 = k.house(2682, 3650, 2688, 3656, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld r5 = k.house(2672, 3650, 2678, 3656, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        rellekkaCourse(k, r0, r1, r2, r3, r4, r5);
        // trees inside the walls
        for (int[] t : new int[][]{{2612, 3656}, {2637, 3656}, {2650, 3646}, {2686, 3646}, {2668, 3658}, {2612, 3674}}) {
            k.spruce(KandarinKit.X(t[0]), KandarinKit.Z(t[1]), 9);
        }
        rellekkaDetails(k, hs);
        return k.build();
    }

    /**
     * Rellekka's yard life: painted shields on the longhouses, firewood stacks, fish-drying racks,
     * braziers along the main street, crates and barrels, and carved posts at the gate.
     */
    private static void rellekkaDetails(KandarinKit k, int[][] houses) {
        int g = k.g;
        String[] shields = {"red", "yellow", "blue", "white"};
        int i = 0;
        for (int[] h : houses) {
            int x1 = KandarinKit.x1(h[0], h[2]);
            int x2 = KandarinKit.x2(h[0], h[2]);
            int z1 = KandarinKit.z1(h[1], h[3]);
            int z2 = KandarinKit.z2(h[1], h[3]);
            char door = (char) h[4];
            // shields hung along the long walls, away from the windows
            if (door == 'n' || door == 's') {
                int z = door == 'n' ? z1 - 1 : z2 + 1;
                for (int x = x1 + 4; x <= x2 - 3; x += 4) {
                    if (Math.abs(x - (x1 + x2) / 2) > 1) {
                        k.d.banner(x, g + 2, z, door, shields[i++ % shields.length]);
                    }
                }
                // firewood stacked against the back wall
                int bz = door == 'n' ? z2 + 1 : z1 - 1;
                k.d.air(x1 + 1, g, bz, x1 + 3, g + 1, bz, "minecraft:spruce_log[axis=x]");
            } else {
                int x = door == 'w' ? x1 - 1 : x2 + 1;
                for (int z = z1 + 4; z <= z2 - 3; z += 4) {
                    if (Math.abs(z - (z1 + z2) / 2) > 1) {
                        k.d.banner(x, g + 2, z, door, shields[i++ % shields.length]);
                    }
                }
                int bx = door == 'w' ? x2 + 1 : x1 - 1;
                k.d.air(bx, g, z1 + 1, bx, g + 1, z1 + 3, "minecraft:spruce_log[axis=z]");
            }
        }
        // fish-drying racks on the beach
        for (int x = KandarinKit.X(2612); x <= KandarinKit.X(2628); x += 8) {
            int z = KandarinKit.Z(3693);
            k.d.air(x, g, z, x, g + 1, z, "minecraft:spruce_fence");
            k.d.air(x + 3, g, z, x + 3, g + 1, z, "minecraft:spruce_fence");
            k.d.air(x, g + 2, z, x + 3, g + 2, z, "minecraft:spruce_fence");
            k.d.air(x + 1, g + 1, z, x + 2, g + 1, z, "minecraft:dried_kelp_block");
        }
        // braziers along the main east-west street
        for (int tx = 2616; tx <= 2686; tx += 14) {
            int x = KandarinKit.X(tx);
            int z = KandarinKit.Z(3662);
            k.d.air(x, g, z, x, g, z, "minecraft:cobblestone_wall");
            k.d.air(x, g + 1, z, x, g + 1, z, "minecraft:campfire[lit=true]");
        }
        // crates and barrels by the smithy and the market, sign post at the gate
        k.d.crates(KandarinKit.X(2662), g, KandarinKit.Z(3646));
        k.d.crates(KandarinKit.X(2650), g, KandarinKit.Z(3668));
        k.d.signpost(KandarinKit.X(2661), g, KandarinKit.Z(3645), "spruce");
        for (int tx : new int[]{2656, 2661}) {
            k.d.air(KandarinKit.X(tx), g, KandarinKit.Z(3643), KandarinKit.X(tx), g + 2, KandarinKit.Z(3643), "minecraft:stripped_dark_oak_log");
            k.d.air(KandarinKit.X(tx), g + 3, KandarinKit.Z(3643), KandarinKit.X(tx), g + 3, KandarinKit.Z(3643), "minecraft:skeleton_skull[rotation=0]");
        }
    }

    /** Rellekka rooftop course (7 obstacles) over the six flat-roofed houses on the east side. */
    private static void rellekkaCourse(KandarinKit k, Bld r0, Bld r1, Bld r2, Bld r3, Bld r4, Bld r5) {
        int c = 8;
        int top = 4;
        int lx = r0.x1() + 2;
        for (int dx = 0; dx < 4; dx++) {
            if (k.ladder(r0.x1() + 2 + dx, r0.z2() + 1, 0, top, "south")) {
                lx = r0.x1() + 2 + dx;
                break;
            }
        }
        k.set(lx, top + 1, r0.z2(), AIR);
        k.set(lx, top, r0.z2() - 1, obstacle(c, 0));
        // 1: rope north to r1
        int x1 = KandarinKit.X(2675);
        ropeZ(k, x1, r1.z2(), r0.z1(), top);
        k.set(x1, top, r1.z2() - 1, obstacle(c, 1));
        // 2: plank east to r2
        KandarinArdougne.bridgeX(k, r1.x2(), r2.x1(), KandarinKit.Z(3677), top);
        k.set(r2.x1() + 1, top, KandarinKit.Z(3677), obstacle(c, 2));
        // 3: rope south to r3
        int x3 = KandarinKit.X(2685);
        ropeZ(k, x3, r2.z2(), r3.z1(), top);
        k.set(x3, top, r3.z1() + 1, obstacle(c, 3));
        // 4: rope south to r4
        ropeZ(k, x3, r3.z2(), r4.z1(), top);
        k.set(x3, top, r4.z1() + 1, obstacle(c, 4));
        // 5: plank west to r5
        KandarinArdougne.bridgeX(k, r5.x2(), r4.x1(), KandarinKit.Z(3653), top);
        k.set(r5.x2() - 1, top, KandarinKit.Z(3653), obstacle(c, 5));
        // 6: jump down onto the hay
        int gx = r5.x1() - 4;
        int gz = KandarinKit.Z(3653);
        k.set(r5.x1(), top + 1, gz, AIR);
        k.fill(gx - 1, -1, gz - 1, gx + 1, -1, gz + 1, HAY);
        k.set(gx, -1, gz, obstacle(c, 6));
    }

    /** Tightrope at roof height between a north roof edge zn and a south roof edge zs. */
    private static void ropeZ(KandarinKit k, int x, int zn, int zs, int top) {
        k.set(x, top + 1, zn, AIR);
        k.set(x, top + 1, zs, AIR);
        k.fill(x, top, zn + 1, x, top, zs - 1, "minecraft:spruce_slab[type=top]");
    }

    static StructureDef rellekkaLonghall() {
        KandarinKit k = new KandarinKit("rellekka_longhall", "Rellekka Longhall").replaces("rellekka_longhall_basement");
        Pal p = FREM_DARK.roof("minecraft:dark_oak");
        // OSRS map (mejrs layers_osrs): a long narrow hall 2655-2662 x 3665-3685 with its door south at
        // 2658-2659, the chieftain's rooms partitioned off at the north end (3682-3685) and a small annex
        // east of them (2663-2666 x 3682-3685) with its own door east at 3683
        Bld h = k.house(2655, 3665, 2662, 3685, 2, p, 's', WIDE_DOOR | NO_LADDER);
        k.clear(h.x1() + 1, 3, h.z1() + 1, h.x2() - 1, 3, h.z2() - 1);
        int pz = KandarinKit.Z(3682) + 1;
        k.fill(h.x1() + 1, 0, pz, h.x2() - 1, 6, pz, "minecraft:spruce_planks");
        k.clear(h.x1() + 2, 0, pz, h.x1() + 3, 1, pz);
        k.house(2663, 3682, 2666, 3685, 1, FREM_DARK.roof("minecraft:dark_oak"), 'e', NO_LADDER);
        k.clear(h.x2(), 0, KandarinKit.Z(3683), KandarinKit.X(2663), 1, KandarinKit.Z(3683));
        k.bed(h.x2() - 2, 0, h.z1() + 2, "red");
        k.set(h.x2() - 1, 0, h.z1() + 1, "minecraft:chest[facing=south]");
        int tz = pz + 1;
        // pillars, shields, tables, fire and the chieftain's throne
        for (int z = h.z1() + 4; z <= h.z2() - 4; z += 6) {
            k.fill(h.x1() + 4, 0, z, h.x1() + 4, 6, z, "minecraft:stripped_spruce_log");
            k.fill(h.x2() - 4, 0, z, h.x2() - 4, 6, z, "minecraft:stripped_spruce_log");
        }
        for (int z = h.z1() + 3; z <= h.z2() - 3; z += 4) {
            k.set(h.x1() + 1, 2, z, "minecraft:red_wall_banner[facing=east]");
            k.set(h.x2() - 1, 2, z, "minecraft:yellow_wall_banner[facing=west]");
        }
        k.longTable(h.x1() + 6, h.x1() + 9, 0, h.cz() + 6, "spruce");
        k.fill(h.x1() + 2, 0, h.z1() + 6, h.x1() + 2, 0, h.z2() - 4, "minecraft:spruce_slab[type=top]");
        k.fill(h.x2() - 2, 0, h.z1() + 6, h.x2() - 2, 0, h.z2() - 4, "minecraft:spruce_slab[type=top]");
        k.fill(h.cx() - 1, -1, h.cz() - 1, h.cx() + 1, -1, h.cz() + 1, COBBLE);
        k.set(h.cx(), 0, h.cz(), "minecraft:campfire[lit=true]");
        k.fill(h.cx() - 2, 0, tz, h.cx() + 2, 0, tz + 1, "minecraft:spruce_planks");
        k.set(h.cx(), 1, tz, "minecraft:dark_oak_stairs[facing=north]");
        k.set(h.cx() - 1, 1, tz, "minecraft:white_wool");
        k.set(h.cx() + 1, 1, tz, "minecraft:white_wool");
        k.set(h.cx(), 6, h.cz() + 4, "minecraft:lantern[hanging=true]");
        k.set(h.cx(), 6, h.cz() - 4, "minecraft:lantern[hanging=true]");
        // dragon-headed gable posts
        k.set(h.x1() - 1, 8, h.z2() + 1, "minecraft:carved_pumpkin[facing=south]");
        k.set(h.x2() + 1, 8, h.z2() + 1, "minecraft:carved_pumpkin[facing=south]");
        // painted shields along the outside, braziers by the door, firewood and a carved post
        String[] shields = {"red", "yellow", "blue", "white"};
        int i = 0;
        for (int z = h.z1() + 4; z <= h.z2() - 3; z += 4) {
            k.d.banner(h.x1() - 1, k.g + 2, z, 'w', shields[i++ % 4]);
            k.d.banner(h.x2() + 1, k.g + 2, z, 'e', shields[i++ % 4]);
            k.d.banner(h.x1() - 1, k.g + 6, z, 'w', shields[(i + 1) % 4]);
            k.d.banner(h.x2() + 1, k.g + 6, z, 'e', shields[(i + 2) % 4]);
        }
        for (int x : new int[]{h.cx() - 3, h.cx() + 4}) {
            k.d.air(x, k.g, h.z2() + 2, x, k.g, h.z2() + 2, "minecraft:cobblestone_wall");
            k.d.air(x, k.g + 1, h.z2() + 2, x, k.g + 1, h.z2() + 2, "minecraft:campfire[lit=true]");
        }
        k.d.air(h.x1() + 1, k.g, h.z1() - 1, h.x1() + 4, k.g + 1, h.z1() - 1, "minecraft:spruce_log[axis=x]");
        k.d.crates(h.x2() - 3, k.g, h.z1() - 2);
        return k.build();
    }

    /**
     * Rellekka's market square as in the OSRS cache: two rows of stalls facing each other across the
     * square, the west row at x 2638 (fish 3672, fur 3676, fish 3680, facing east) and the east row at
     * x 2647 (market stall 3672, fish 3676, fur 3680, facing west), with the barrels and crates beside them.
     * The thieving chest (steel arrowtips) stands on its OSRS tile south-east of the market (2650, 3659).
     */
    static StructureDef rellekkaMarket() {
        KandarinKit k = new KandarinKit("rellekka_market", "Rellekka market").replaces("rellekka_arrowtip_chest");
        k.road(2636, 3670, 2649, 3682, "minecraft:coarse_dirt", GRAVEL);
        Object[][] stalls = {
                {2638, 3672, "runecraft:fish_stall", "blue", 'e'}, {2638, 3676, "runecraft:fur_stall", "brown", 'e'},
                {2638, 3680, "runecraft:fish_stall", "cyan", 'e'}, {2647, 3672, "minecraft:barrel[facing=up]", "green", 'w'},
                {2647, 3676, "runecraft:fish_stall", "blue", 'w'}, {2647, 3680, "runecraft:fur_stall", "brown", 'w'}};
        for (Object[] s : stalls) {
            marketStall(k, (Integer) s[0], (Integer) s[1], (String) s[2], (String) s[3], (Character) s[4]);
        }
        for (int[] b : new int[][]{{2637, 3673}, {2638, 3674}, {2649, 3677}, {2648, 3678}, {2637, 3680}, {2637, 3681}, {2640, 3682}}) {
            k.set(KandarinKit.X(b[0]), 0, KandarinKit.Z(b[1]), "minecraft:barrel[facing=up]");
        }
        for (int[] c : new int[][]{{2638, 3665}, {2637, 3666}, {2638, 3666}}) {
            k.set(KandarinKit.X(c[0]), 0, KandarinKit.Z(c[1]), "minecraft:spruce_planks");
        }
        k.set(KandarinKit.X(2638), 1, KandarinKit.Z(3666), "minecraft:spruce_planks");
        k.set(KandarinKit.X(2650), 0, KandarinKit.Z(3659), "runecraft:arrowtip_chest");
        k.set(KandarinKit.X(2650) + 1, 0, KandarinKit.Z(3659), "minecraft:barrel[facing=up]");
        return k.build();
    }

    /**
     * A Fremennik market stall on its OSRS tile: the station block on the tile's front block (towards the
     * aisle), a counter running north-south either side, fence posts and a striped wool canopy.
     */
    private static void marketStall(KandarinKit k, int tx, int ty, String station, String wool, char front) {
        int back = front == 'e' ? KandarinKit.X(tx) - 1 : KandarinKit.X(tx) + 2;
        int x = front == 'e' ? KandarinKit.X(tx) + 1 : KandarinKit.X(tx);
        int z = KandarinKit.Z(ty);
        k.set(x, 0, z, station);
        k.set(x, 0, z - 1, "minecraft:spruce_slab[type=top]").set(x, 0, z + 1, "minecraft:spruce_slab[type=top]");
        k.set(x, 0, z + 2, "minecraft:spruce_slab[type=top]");
        for (int[] p : new int[][]{{x, z - 2}, {x, z + 3}, {back, z - 2}, {back, z + 3}}) {
            k.fill(p[0], 0, p[1], p[0], 2, p[1], "minecraft:spruce_fence");
        }
        int lo = Math.min(x, back);
        int hi = Math.max(x, back);
        for (int zz = z - 2; zz <= z + 3; zz++) {
            k.fill(lo, 3, zz, hi, 3, zz, "minecraft:" + ((zz & 1) == 0 ? wool : "white") + "_wool");
        }
        k.set(back, 0, z, "minecraft:barrel[facing=up]");
    }

    static StructureDef rellekkaDocks() {
        KandarinKit k = new KandarinKit("rellekka_docks", "Rellekka docks").replaces("rellekka_docks");
        k.pier(2631, 3692, 2633, 3710, "spruce", true);
        k.pier(2614, 3702, 2650, 3703, "spruce", true);
        k.tClear(2631, 3702, 2633, 3703, 0, 0);
        k.tClear(2631, 3692, 2633, 3692, 0, 0);
        k.set(KandarinKit.X(2634), 0, KandarinKit.Z(3690), "runecraft:rellekka_task_board");
        k.longboat(KandarinKit.X(2622), KandarinKit.Z(3707), 19, true, "red");
        k.longboat(KandarinKit.X(2643), KandarinKit.Z(3707), 17, true, "blue");
        k.longboat(KandarinKit.X(2620), KandarinKit.Z(3698), 15, true, "yellow");
        for (int x = 2616; x <= 2648; x += 6) {
            k.set(KandarinKit.X(x), 0, KandarinKit.Z(3694), "minecraft:barrel[facing=up]");
        }
        return k.build();
    }

    // ------------------------------------------------------------------ Piscatoris

    static StructureDef piscatoris() {
        KandarinKit k = new KandarinKit("piscatoris", "Piscatoris Fishing Colony").replaces("piscatoris");
        k.flatten(2326, 3630, 2370, 3668);
        k.floor(2326, 3657, 2370, 3659, SAND);
        k.water(2326, 3660, 2370, 3668, 3);
        k.road(2328, 3646, 2350, 3647, PATH, GRAVEL);
        k.road(2341, 3648, 2342, 3656, PATH, null);
        Pal p = FREM.roof("minecraft:spruce");
        k.house(2330, 3639, 2336, 3644, 1, p, 'e', FURN);
        k.house(2330, 3649, 2336, 3654, 2, p, 'e', FURN);
        Bld bank = k.house(2344, 3650, 2350, 3655, 1, p, 's', NO_LADDER);
        int bz = bank.z1() + 2;
        k.fill(bank.x1() + 1, 0, bz, bank.x2() - 1, 0, bz, "minecraft:spruce_slab[type=top]");
        k.set(bank.x1() + 3, 0, bz, BANK);
        k.set(bank.x2() - 3, 0, bz, BANK);
        Bld shop = k.house(2340, 3636, 2346, 3641, 1, p, 'n', NO_LADDER);
        k.counter(shop, TOOLS, "spruce");
        k.pier(2340, 3657, 2342, 3667, "spruce", true);
        k.pier(2334, 3665, 2348, 3666, "spruce", true);
        k.tClear(2340, 3665, 2342, 3666, 0, 0);
        k.longboat(KandarinKit.X(2333), KandarinKit.Z(3662), 11, true, "white");
        k.house(2328, 3632, 2333, 3636, 1, p, 'n', FURN);
        return k.build();
    }

    // ------------------------------------------------------------------ Trollheim

    static StructureDef trollheim() {
        // Trollheim is a mountain top: the summit is levelled high up in the Troll Country range.
        KandarinKit k = new KandarinKit("trollheim", "Trollheim", 122).replaces("TROLLHEIM");
        k.flatten(2876, 3664, 2904, 3692, "minecraft:snow_block");
        int cx = KandarinKit.X(2890);
        int cz = KandarinKit.Z(3678);
        // crags around the summit
        int[][] rocks = {{-22, -18, 6, 9}, {20, -20, 7, 11}, {-24, 10, 6, 8}, {22, 16, 5, 7}, {0, -26, 8, 10}, {-6, 25, 6, 6}, {26, -2, 4, 6}};
        for (int[] r : rocks) {
            k.blob(cx + r[0], r[3] / 2 - 1, cz + r[1], r[2], r[3] / 2 + 1, r[2], "minecraft:stone", false);
            k.blob(cx + r[0], r[3] / 2 + 1, cz + r[1], r[2] - 1, r[3] / 2, r[2] - 1, "minecraft:snow_block", true);
        }
        // Eadgar's cave mouth
        int ex = KandarinKit.X(2897);
        int ez = KandarinKit.Z(3686);
        k.blob(ex, 1, ez, 5, 4, 4, "minecraft:andesite", false);
        k.clear(ex - 1, 0, ez - 4, ex + 1, 2, ez);
        k.fill(ex - 1, -1, ez - 4, ex + 1, -1, ez, "minecraft:gravel");
        // summit cairn and standing stones with the shrine altar
        k.ring(cx, cz, -1, -1, 5, "minecraft:cobblestone");
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            int x = cx + (int) Math.round(9 * Math.cos(ang));
            int z = cz + (int) Math.round(9 * Math.sin(ang));
            k.fill(x, 0, z, x, 2 + (a % 2), z, "minecraft:mossy_cobblestone");
            k.set(x, 3 + (a % 2), z, "minecraft:snow[layers=2]");
        }
        int sx = cx - 6;
        int sz = cz - 9;
        k.walls(sx - 2, 0, sz - 2, sx + 2, 2, sz + 2, "minecraft:cobblestone");
        k.clear(sx, 0, sz + 2, sx, 1, sz + 2);
        k.hipRoof(sx - 2, sz - 2, sx + 2, sz + 2, 3, "minecraft:cobblestone");
        k.set(sx, 0, sz - 1, ALTAR);
        k.set(sx - 1, 0, sz - 1, "minecraft:candle[candles=2,lit=true]");
        k.set(cx + 7, 0, cz + 6, "minecraft:snow[layers=3]");
        return k.build();
    }

    // ------------------------------------------------------------------ Troll Stronghold

    /**
     * The trolls' fortress. Its boss arena is generated 24 blocks east of the place centre, so the
     * east half of the courtyard is left open as the troll arena; the keep and the prison stand west.
     */
    static StructureDef trollStronghold() {
        // The stronghold stands on the crags above Death Plateau.
        KandarinKit k = new KandarinKit("troll_stronghold", "Troll Stronghold", 108).replaces("troll_stronghold");
        String w = "minecraft:cobbled_deepslate";
        k.flatten(2820, 3666, 2866, 3708, "minecraft:coarse_dirt");
        k.cwallRect(2824, 3670, 2862, 3704, 11, w);
        KandarinSeers.weather(k, KandarinKit.X(2824), KandarinKit.Z(3704), KandarinKit.X(2862) + 1, KandarinKit.Z(3670) + 1, 0, 10);
        k.replace(KandarinKit.X(2824), 0, KandarinKit.Z(3704), KandarinKit.X(2862) + 1, 10, KandarinKit.Z(3670) + 1, MOSSY_SB, "minecraft:mossy_cobblestone");
        k.replace(KandarinKit.X(2824), 0, KandarinKit.Z(3704), KandarinKit.X(2862) + 1, 10, KandarinKit.Z(3670) + 1, CRACKED_SB, "minecraft:cracked_deepslate_bricks");
        k.gate(2841, 3670, 2843, 3670, 7, GRAVEL);
        for (int[] c : new int[][]{{2824, 3670}, {2862, 3670}, {2824, 3704}, {2862, 3704}}) {
            k.tower(c[0], c[1], 5, 17, w, "minecraft:dark_oak_planks", null, ' ');
        }
        k.tower(2839, 3670, 3, 15, w, "minecraft:dark_oak_planks", null, 'n');
        k.tower(2845, 3670, 3, 15, w, "minecraft:dark_oak_planks", null, 'n');
        k.road(2841, 3671, 2843, 3689, GRAVEL, "minecraft:bone_block");
        // keep
        Pal kp = TROLL.roof("flat:minecraft:polished_blackstone");
        Bld keep = k.house(2826, 3690, 2840, 3702, 3, kp, 'e', FLAT | FURN | WIDE_DOOR);
        k.fill(keep.x1() + 1, 0, keep.cz() - 1, keep.x1() + 3, 0, keep.cz() + 1, "minecraft:bone_block");
        k.set(keep.x1() + 2, 1, keep.cz(), "minecraft:skeleton_skull");
        k.tower(2826, 3702, 4, 22, w, "minecraft:dark_oak_planks", "minecraft:deepslate_tiles", 'e');
        // prison
        Bld pr = k.house(2826, 3673, 2834, 3684, 1, TROLL, 'e', NO_LADDER);
        for (int z = pr.z1() + 3; z <= pr.z2() - 1; z += 4) {
            k.fill(pr.x1() + 1, 0, z, pr.x2() - 4, 2, z, "minecraft:iron_bars");
        }
        k.fill(pr.x2() - 4, 0, pr.z1() + 1, pr.x2() - 4, 2, pr.z2() - 1, "minecraft:iron_bars");
        // troll camp debris
        for (int i = 0; i < 6; i++) {
            int x = KandarinKit.X(2836) + (KandarinKit.hash(i, 3) % 8);
            int z = KandarinKit.Z(3688) + (KandarinKit.hash(i, 5) % 6);
            k.set(x, 0, z, i % 2 == 0 ? "minecraft:bone_block" : "minecraft:skeleton_skull");
        }
        k.set(KandarinKit.X(2837), 0, KandarinKit.Z(3676), "minecraft:campfire[lit=true]");
        // The stronghold's door (2840, 3690): a ladder shaft by the keep down to y 55, where the dungeons module's
        // TrollStrongholdInterior carries it on into the stronghold's halls.
        int lx = KandarinKit.X(2838);
        int lz = KandarinKit.Z(3688);
        int sz = (lz & 15) != 0 ? lz - 1 : lz + 1;
        String face = (lz & 15) != 0 ? "south" : "north";
        k.fill(lx, 55 - 108, sz, lx, 2, sz, w);
        k.fill(lx, 56 - 108, lz, lx, 2, lz, "minecraft:ladder[facing=" + face + "]");
        // Troll Stronghold herb patch (disease-free in OSRS; there it is on the keep's roof, approx. placed by the keep here)
        k.set(KandarinKit.X(2828), 0, KandarinKit.Z(3687), "runecraft:herb_patch");
        k.flag(KandarinKit.X(2824) + 1, 17, KandarinKit.Z(3704) + 1, "black", "gray");
        return k.build();
    }
}
