package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Kruk's Dungeon (Monkey Madness II) beneath Ape Atoll. Its entrance is the hole under the jungle grass west of Marim
 * (OSRS tile 2715, 2789). The real dungeon is a 370 x 240-tile labyrinth on two floors, far larger than the island
 * above it, so only its core is carved here (approx): the upper floor's passages from the entrance with one monkey
 * gallery, the "vine hole" down to the dark lower floor, the four lower-floor halls where players train on the
 * maniacal monkeys, and Glough's ruined laboratory, now the maniacal monkey Hunter area.
 *
 * <p>Every lower-floor hall copies one cluster of the cache spawns (mejrs/data_osrs NPCList_OSRS.json, plane 0) tile for
 * tile, shifted by ({@link #DX}, {@link #DY}) so that the cluster lies under the atoll; each camp keeps exactly the
 * cache's number of maniacal monkeys and maniacal monkey archers. The upper gallery copies a plane-1 cluster the same
 * way, and the laboratory holds the Hunter area's banana boulders (cache tiles 2902-2920, 9112-9137, shifted east).
 * Entering needs Monkey Madness II (systems module's access gate on the hut).
 *
 * <p>The western hordes of the upper floor (cache plane 1, 2311-2452, 9135-9275: 204 maniacal monkeys and 54 archers)
 * would lie under open sea with the main shift, so they are carved deeper ({@link #HORDE}) with their own shift
 * ({@link #DXH}, {@link #DYH}) that centres them under the atoll, keeping their relative layout; a ladder from the lower
 * north-west hall leads down to them (approx: in OSRS they are reached through the upper floor's western passages).
 * Each {@link #HORDES} hall is a box around one cache cluster with exactly its monsters.
 */
final class KruksDungeon {
    /** OSRS surface tile of the entrance. */
    static final int ENTRANCE_X = 2715;
    static final int ENTRANCE_Y = 2789;
    /** Shift from the cache's dungeon tiles to the atoll tiles above the carved halls. */
    static final int DX = 80;
    static final int DY = -6399;
    /** Floor heights: the upper floor and the dark lower floor. */
    static final int UPPER = 36;
    static final int LOWER = 22;
    /** The western hordes' floor (below Ape Atoll's undead dungeons at 6) and their shift to the atoll's tiles. */
    static final int HORDE = -10;
    static final int DXH = 369;
    static final int DYH = -6455;
    /** Cache tile of the lower north-west hall where the ladder down to the hordes stands. */
    static final int HORDE_LADDER_X = 2641;
    static final int HORDE_LADDER_Y = 9200;
    /** Maniacal monkeys come back 25 ticks after dying (wiki: respawn 25 ticks), approx for the archers. */
    static final int RESPAWN = 25;

    /**
     * A monkey cluster of the cache: its tile box (plane 0 = lower floor, 1 = upper) and the number of maniacal
     * monkeys and archers spawned in it.
     */
    record Cluster(String name, int plane, int x1, int y1, int x2, int y2, int monkeys, int archers) {
    }

    static final Cluster[] CLUSTERS = {
            new Cluster("lower south hall", 0, 2629, 9103, 2644, 9118, 11, 7),
            new Cluster("lower central hall", 0, 2657, 9147, 2667, 9157, 12, 4),
            new Cluster("lower north-west hall", 0, 2637, 9192, 2646, 9200, 6, 4),
            new Cluster("lower north-east hall", 0, 2665, 9197, 2672, 9206, 5, 6),
            new Cluster("upper gallery", 1, 2626, 9135, 2646, 9139, 6, 4),
    };

    /** The western hordes' halls (cache plane 1; every monster of the region is counted in exactly one hall). */
    static final Cluster[] HORDES = {
            new Cluster("south-west horde", 1, 2311, 9135, 2349, 9150, 29, 0),
            new Cluster("south-west horde (east)", 1, 2327, 9151, 2365, 9184, 33, 0),
            new Cluster("south-west horde (passage)", 1, 2353, 9148, 2377, 9163, 7, 0),
            new Cluster("south-west horde (corner)", 1, 2366, 9164, 2387, 9184, 5, 0),
            new Cluster("southern horde", 1, 2390, 9157, 2431, 9180, 31, 0),
            new Cluster("south-east horde", 1, 2427, 9157, 2452, 9189, 41, 0),
            new Cluster("north-west horde", 1, 2337, 9225, 2373, 9268, 41, 0),
            new Cluster("archers' gallery (south)", 1, 2376, 9190, 2420, 9231, 0, 26),
            new Cluster("archers' gallery (north)", 1, 2376, 9232, 2420, 9262, 0, 28),
            new Cluster("northern horde", 1, 2376, 9263, 2420, 9275, 17, 0),
    };

    /**
     * The remaining monkeys of the cache's labyrinth (every maniacal monkey west of the Hunter area that no hall above
     * holds): the plane-0 groups west of 2600 and in the far south and north, and the plane-1 groups east of 2500
     * (2505-2681, 9037-9275). Each box is one cluster of the cache spawns (spawns within 10 tiles of each other) with
     * exactly its monsters. Their floors are deeper still ({@link #OUTER0}, {@link #OUTER1}), each with its own shift
     * that centres it under the atoll; passages chain each floor's boxes and ladders join the floors to the hordes'
     * (approx: in OSRS they are the passages between the halls above).
     */
    static final Cluster[] OUTER = {
            new Cluster("far south-east passage", 0, 2665, 9038, 2674, 9047, 6, 0),
            new Cluster("south-west passage", 0, 2521, 9041, 2531, 9045, 3, 0),
            new Cluster("southern crossing", 0, 2576, 9051, 2587, 9062, 6, 0),
            new Cluster("western dead end", 0, 2503, 9069, 2511, 9077, 4, 0),
            new Cluster("far north passage", 0, 2628, 9251, 2635, 9262, 6, 0),
            new Cluster("upper south passage", 1, 2560, 9037, 2565, 9041, 2, 0),
            new Cluster("upper south-east maze", 1, 2625, 9039, 2660, 9095, 18, 0),
            new Cluster("upper southern hall", 1, 2595, 9040, 2614, 9054, 6, 0),
            new Cluster("upper eastern maze", 1, 2651, 9043, 2681, 9139, 26, 0),
            new Cluster("upper south-west hall", 1, 2505, 9044, 2512, 9057, 4, 0),
            new Cluster("upper south-west passage", 1, 2533, 9056, 2551, 9070, 5, 0),
            new Cluster("upper nook", 1, 2615, 9064, 2617, 9066, 1, 0),
            new Cluster("upper side passage", 1, 2626, 9068, 2637, 9072, 2, 0),
            new Cluster("upper western nook", 1, 2519, 9069, 2521, 9071, 1, 0),
            new Cluster("upper central passage", 1, 2581, 9069, 2614, 9082, 8, 0),
            new Cluster("upper vine passage", 1, 2663, 9168, 2667, 9174, 2, 0),
            new Cluster("upper northern nook", 1, 2651, 9199, 2658, 9204, 2, 0),
            new Cluster("upper north-west nook", 1, 2616, 9201, 2621, 9208, 3, 0),
            new Cluster("upper north-west passage", 1, 2617, 9217, 2632, 9244, 8, 0),
            new Cluster("upper far north-west nook", 1, 2554, 9258, 2560, 9266, 3, 0),
            new Cluster("upper far north passage", 1, 2605, 9269, 2622, 9275, 7, 0),
    };
    /** The floors of the remaining plane-0 and plane-1 groups, and their shifts (cluster box centres under the atoll). */
    static final int OUTER0 = -24;
    static final int OUTER1 = -38;
    static final int DX0 = 164;
    static final int DY0 = -6400;
    static final int DX1 = 159;
    static final int DY1 = -6406;

    private KruksDungeon() {
    }

    private static int x(int caveX) {
        return StructureDef.bx(caveX + DX);
    }

    private static int z(int caveY) {
        return StructureDef.bz(caveY + DY);
    }

    /** The hordes' cache tile to block. */
    private static int xh(int caveX) {
        return StructureDef.bx(caveX + DXH);
    }

    private static int zh(int caveY) {
        return StructureDef.bz(caveY + DYH);
    }

    /** A horde hall over the cache tile rectangle (inclusive). */
    private static void hordeRoom(Cave c, Cluster k) {
        c.room(xh(k.x1()), zh(k.y2()), xh(k.x2()) + 1, zh(k.y1()) + 1, HORDE, 7);
    }

    /** A 3-wide passage between two horde tiles. */
    private static void hordeHall(Cave c, int ax, int ay, int bx, int by) {
        c.hall(xh(ax), zh(ay), xh(bx), zh(by), HORDE, 3, 5);
    }

    /** A room over the cache tile rectangle (inclusive) on floor f. */
    private static void room(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int h) {
        c.room(x(tx1), z(ty2), x(tx2) + 1, z(ty1) + 1, f, h);
    }

    /** Block x and z of a remaining group's cache tile. */
    private static int xo(Cluster k, int caveX) {
        return StructureDef.bx(caveX + (k.plane() == 0 ? DX0 : DX1));
    }

    private static int zo(Cluster k, int caveY) {
        return StructureDef.bz(caveY + (k.plane() == 0 ? DY0 : DY1));
    }

    static int outerFloor(Cluster k) {
        return k.plane() == 0 ? OUTER0 : OUTER1;
    }

    /** Block centre {x, z} of a remaining group's box. */
    private static int[] outerCentre(Cluster k) {
        return new int[]{(xo(k, k.x1()) + xo(k, k.x2()) + 1) / 2, (zo(k, k.y1()) + zo(k, k.y2()) + 1) / 2};
    }

    /** Block centre {x, z} of a horde hall. */
    private static int[] hordeCentre(Cluster k) {
        return new int[]{(xh(k.x1()) + xh(k.x2()) + 1) / 2, (zh(k.y1()) + zh(k.y2()) + 1) / 2};
    }

    /** The remaining groups from west to east (the order their passages chain them). */
    private static java.util.List<Cluster> outerByX() {
        java.util.List<Cluster> l = new java.util.ArrayList<>(java.util.List.of(OUTER));
        l.sort(java.util.Comparator.comparingInt(k -> k.x1() + k.x2()));
        return l;
    }

    /** A 3-wide passage between two cache tiles on floor f (along x first, then along z). */
    private static void hall(Cave c, int ax, int ay, int bx, int by, int f) {
        c.hall(x(ax), z(ay), x(bx), z(by), f, 3, 5);
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_kruks_dungeon", "Kruk's Dungeon", mc("mossy_cobblestone"), mc("rooted_dirt"), mc("verdant_froglight"));
        // Cache tiles of the entrance landing and of the vine hole between the floors.
        int ex = ENTRANCE_X - DX;
        int ey = ENTRANCE_Y - DY;
        int holeX = 2651;
        int holeY = 9168;
        // ---- upper floor: the landing, the winding passages and the gallery
        room(c, ex - 3, ey - 3, ex + 3, ey + 3, UPPER, 5);
        hall(c, ex, ey, ex, holeY, UPPER);
        hall(c, ex, holeY, holeX, holeY, UPPER);
        room(c, holeX - 2, holeY - 2, holeX + 2, holeY + 2, UPPER, 5);
        hall(c, ex, holeY, ex, 9139, UPPER);
        room(c, 2624, 9133, 2648, 9141, UPPER, 6);
        // ---- lower floor: the halls of the cache clusters, joined through the vine hole's cavern
        room(c, holeX - 3, holeY - 3, holeX + 3, holeY + 3, LOWER, 6);
        room(c, 2627, 9101, 2646, 9120, LOWER, 7);
        room(c, 2655, 9145, 2669, 9159, LOWER, 7);
        room(c, 2635, 9190, 2648, 9202, LOWER, 7);
        room(c, 2663, 9195, 2674, 9206, LOWER, 7);
        hall(c, holeX, holeY, 2636, 9120, LOWER);
        hall(c, holeX, holeY, 2662, 9159, LOWER);
        hall(c, holeX, holeY, 2641, 9190, LOWER);
        hall(c, 2648, 9196, 2663, 9196, LOWER);
        // Glough's laboratory (the Hunter area), east of the central hall.
        int lx1 = 2681;
        int ly1 = 9163;
        int lx2 = 2700;
        int ly2 = 9189;
        hall(c, 2669, 9152, 2690, 9163, LOWER);
        room(c, lx1, ly1, lx2, ly2, LOWER, 7);
        // ---- the western hordes, deeper down, joined where their clusters meet
        for (Cluster k : HORDES) {
            hordeRoom(c, k);
        }
        hordeHall(c, 2387, 9170, 2390, 9170);
        hordeHall(c, 2400, 9180, 2400, 9190);
        hordeHall(c, 2373, 9245, 2376, 9245);
        // ---- the remaining groups, deeper down: each floor's boxes chained west to east by passages
        int[] prev0 = null;
        int[] prev1 = null;
        for (Cluster k : outerByX()) {
            c.room(xo(k, k.x1()), zo(k, k.y2()), xo(k, k.x2()) + 1, zo(k, k.y1()) + 1, outerFloor(k), 6);
            int[] m = outerCentre(k);
            int[] prev = k.plane() == 0 ? prev0 : prev1;
            if (prev != null) {
                c.hall(prev[0], prev[1], m[0], m[1], outerFloor(k), 3, 5);
            }
            if (k.plane() == 0) {
                prev0 = m;
            } else {
                prev1 = m;
            }
        }
        // Ladder shafts: from the south-east horde down to the plane-0 groups' first box, and from that floor down to
        // the plane-1 groups' south-east maze; a passage on the upper floor of each pair reaches the ladder.
        int[] l0 = outerCentre(OUTER[0]);
        int[] h0 = hordeCentre(HORDES[5]);
        c.hall(h0[0], h0[1], l0[0], l0[1], HORDE, 3, 5);
        int[] l1 = outerCentre(OUTER[6]);
        c.hall(l0[0], l0[1], l1[0], l1[1], OUTER0, 3, 5);
        c.carve();
        c.ladderUp(l0[0], l0[1], OUTER0, HORDE);
        c.ladderUp(l1[0], l1[1], OUTER1, OUTER0);

        // The way down: a ladder through the vine hole.
        c.ladderUp(x(holeX), z(holeY), LOWER, UPPER);
        // The ladder from the lower north-west hall down to the hordes; jungle roots and bones in their halls.
        c.ladderUp(x(HORDE_LADDER_X), z(HORDE_LADDER_Y), HORDE, LOWER);
        for (int[] p : new int[][]{{2330, 9142}, {2346, 9168}, {2410, 9168}, {2440, 9173}, {2355, 9240}, {2398, 9210}, {2398, 9247}}) {
            c.pillar(xh(p[0]), zh(p[1]), HORDE, 7, mc("jungle_log[axis=y]"));
        }
        c.set(xh(2345), HORDE + 1, zh(9255), mc("bone_block[axis=x]"));
        // Jungle roots and old bones in the halls; the laboratory's ruined benches.
        for (int[] p : new int[][]{{2636, 9110}, {2662, 9152}, {2641, 9196}, {2668, 9201}}) {
            c.pillar(x(p[0]), z(p[1]), LOWER, 7, mc("jungle_log[axis=y]"));
        }
        c.set(x(2630), LOWER + 1, z(9104), mc("bone_block[axis=y]"));
        c.set(x(2666), LOWER + 1, z(9146), mc("skeleton_skull[rotation=4]"));
        c.fill(x(lx1) + 1, LOWER + 1, z(ly2) + 1, x(lx1) + 4, LOWER + 1, z(ly2) + 1, mc("iron_bars"));
        c.set(x(lx2), LOWER + 1, z(ly1), mc("brewing_stand"));
        // The maniacal monkey boulders (skilling module's Hunter trap, needs Monkey Madness II), spread over the
        // laboratory like the cache's 29 Hunter monkeys.
        for (int[] p : new int[][]{{2684, 9167}, {2690, 9172}, {2696, 9168}, {2686, 9180}, {2694, 9184}, {2698, 9177}}) {
            c.set(x(p[0]), LOWER + 1, z(p[1]), rc("maniacal_monkey_boulder"));
        }

        c.entrance(x(ex), z(ey), UPPER, mc("jungle_log"), mc("rooted_dirt"), 'e');
        for (Cluster k : CLUSTERS) {
            int f = k.plane() == 0 ? LOWER : UPPER;
            c.camp(x(k.x1()), z(k.y2()), x(k.x2()) + 1, z(k.y1()) + 1, f, k.monkeys(), RESPAWN, "maniacal_monkey");
            c.camp(x(k.x1()), z(k.y2()), x(k.x2()) + 1, z(k.y1()) + 1, f, k.archers(), RESPAWN, "maniacal_monkey_archer");
        }
        for (Cluster k : OUTER) {
            c.camp(xo(k, k.x1()), zo(k, k.y2()), xo(k, k.x2()) + 1, zo(k, k.y1()) + 1, outerFloor(k), k.monkeys(), RESPAWN,
                    "maniacal_monkey");
        }
        for (Cluster k : HORDES) {
            if (k.monkeys() > 0) {
                c.camp(xh(k.x1()), zh(k.y2()), xh(k.x2()) + 1, zh(k.y1()) + 1, HORDE, k.monkeys(), RESPAWN, "maniacal_monkey");
            }
            if (k.archers() > 0) {
                c.camp(xh(k.x1()), zh(k.y2()), xh(k.x2()) + 1, zh(k.y1()) + 1, HORDE, k.archers(), RESPAWN, "maniacal_monkey_archer");
            }
        }
        return c.build();
    }
}
