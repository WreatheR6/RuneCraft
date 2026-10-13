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

    private KruksDungeon() {
    }

    private static int x(int caveX) {
        return StructureDef.bx(caveX + DX);
    }

    private static int z(int caveY) {
        return StructureDef.bz(caveY + DY);
    }

    /** A room over the cache tile rectangle (inclusive) on floor f. */
    private static void room(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int h) {
        c.room(x(tx1), z(ty2), x(tx2) + 1, z(ty1) + 1, f, h);
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
        c.carve();

        // The way down: a ladder through the vine hole.
        c.ladderUp(x(holeX), z(holeY), LOWER, UPPER);
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
        return c.build();
    }
}
