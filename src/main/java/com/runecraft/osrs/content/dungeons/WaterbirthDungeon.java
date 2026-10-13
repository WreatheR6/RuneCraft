package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The Waterbirth Island Dungeon, below the cave on Waterbirth Island, down to the ladder of the Dagannoth Kings.
 *
 * <p>Sublevel 1 copies the cache's tiles (mejrs/data_osrs NPCList_OSRS.json, 2440-2552, 10118-10170): the entrance hall
 * of level 90 dagannoth with their eggs (dagannoth spawn) and a few rock crabs in its north-west, the three paths
 * beyond it - giant rock crabs to the north, wallasalki in the middle, level 88 (ranged) dagannoth to the south - and
 * the small central room with the ladder down. The deeper sublevels copy the cache's region at 1790-1975, 4353-4412,
 * whose four planes are carved as four floors: plane 1 (sublevel 2, with the dagannoth fledgelings that Bardur
 * fights), plane 2 (sublevel 3), plane 3 (sublevel 4) and plane 0 (sublevel 5, the great hall with the Kings' ladder
 * and the rock lobsters' corridor), each room holding exactly the cache's monsters, and the rock lobster rooms
 * (sublevels 5 to 2) that once led to the Lighthouse. The Kings' ladder takes the player to the Dagannoth Kings'
 * arena (bosses module).
 *
 * <p>Approximations: both cache regions are shifted so that they lie under and beside the island (sublevel 1 by
 * ({@link #DX1}, {@link #DY1}), the sublevels by ({@link #DX2}, {@link #DY2})); the passages between the cache's rooms
 * and the ladders between the floors follow the wiki's walkthrough but not its exact map; the hut stands on the island's
 * west shore, about 12 tiles from the real cave mouth (the islands module builds the cave itself); the circular doors
 * (two players or a pet rock) and the door-supports in front of the central ladder (a rune thrownaxe) are open
 * passages; the dagannoth eggs are spawn camps beside the eggs; Bardur, the Askeladden exit and the Lighthouse exit are
 * not built.
 */
final class WaterbirthDungeon {
    /** OSRS tile of the entrance hall where the cave's stairs arrive, and the shift that puts it under the hut. */
    static final int ARRIVAL_X = 2442;
    static final int ARRIVAL_Y = 10146;
    static final int DX1 = 65;
    static final int DY1 = -6406;
    /** Shift of the sublevels: sublevel 1's ladder (2545, 10143) stands above plane 1's tile (1900, 4360). */
    static final int DX2 = 710;
    static final int DY2 = -623;
    /** Floor heights: sublevel 1, then planes 1, 2, 3 and 0 (16 blocks apart so camps wake one floor at a time). */
    static final int L1 = 40;
    static final int P1 = 24;
    static final int P2 = 8;
    static final int P3 = -8;
    static final int P0 = -24;

    private WaterbirthDungeon() {
    }

    /** Sublevel 1's tile to block. */
    private static int x1(int t) {
        return StructureDef.bx(t + DX1);
    }

    private static int z1(int t) {
        return StructureDef.bz(t + DY1);
    }

    /** The sublevels' tile to block. */
    private static int xs(int t) {
        return StructureDef.bx(t + DX2);
    }

    private static int zs(int t) {
        return StructureDef.bz(t + DY2);
    }

    private static boolean deep(int f) {
        return f != L1;
    }

    private static int bx(int t, int f) {
        return deep(f) ? xs(t) : x1(t);
    }

    private static int bz(int t, int f) {
        return deep(f) ? zs(t) : z1(t);
    }

    /** A room over the tile rectangle (inclusive) on floor f. */
    private static void room(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int h) {
        c.room(bx(tx1, f), bz(ty2, f), bx(tx2, f) + 1, bz(ty1, f) + 1, f, h);
    }

    /** A 3-wide passage between two tiles on floor f (along x first, then along y). */
    private static void hall(Cave c, int ax, int ay, int bxT, int byT, int f) {
        c.hall(bx(ax, f), bz(ay, f), bx(bxT, f), bz(byT, f), f, 3, 5);
    }

    /** A camp over the tile rectangle (inclusive), one block in from the walls. */
    private static void camp(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int cap, int respawn, String creature) {
        c.camp(bx(tx1, f) + 1, bz(ty2, f) + 1, bx(tx2, f), bz(ty1, f), f, cap, respawn, creature);
    }

    /** A ladder at a sublevel tile between floors fLow and fHigh. */
    private static void ladder(Cave c, int tx, int ty, int fLow, int fHigh) {
        c.ladderUp(xs(tx), zs(ty), fLow, fHigh);
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_waterbirth_dungeon", "Waterbirth Island Dungeon", mc("tuff"), mc("mud"), mc("sea_lantern"));
        // ---- sublevel 1
        room(c, 2440, 10120, 2492, 10170, L1, 8);                  // the entrance hall
        room(c, 2495, 10118, 2552, 10134, L1, 6);                  // south path: ranged dagannoth
        room(c, 2495, 10137, 2539, 10150, L1, 6);                  // middle path: wallasalki
        room(c, 2495, 10153, 2551, 10168, L1, 6);                  // north path: giant rock crabs
        room(c, 2541, 10137, 2549, 10149, L1, 6);                  // the central room
        hall(c, 2492, 10126, 2495, 10126, L1);                     // the three (circular) doors
        hall(c, 2492, 10145, 2495, 10145, L1);
        hall(c, 2492, 10160, 2495, 10160, L1);
        hall(c, 2539, 10143, 2541, 10143, L1);                     // and the paths' ends at the central room
        hall(c, 2545, 10134, 2545, 10137, L1);
        hall(c, 2545, 10149, 2545, 10153, L1);
        // ---- plane 1 (sublevel 2)
        room(c, 1896, 4356, 1904, 4364, P1, 5);                    // landing below the central ladder
        hall(c, 1896, 4360, 1866, 4360, P1);
        room(c, 1829, 4356, 1866, 4367, P1, 6);                    // dagannoth of both kinds
        hall(c, 1829, 4362, 1803, 4369, P1);
        room(c, 1795, 4369, 1810, 4395, P1, 6);                    // wallasalki
        hall(c, 1848, 4367, 1848, 4385, P1);
        room(c, 1844, 4385, 1854, 4397, P1, 6);                    // Bardur's fight with the fledgelings
        hall(c, 1854, 4395, 1858, 4395, P1);
        room(c, 1858, 4393, 1892, 4412, P1, 6);                    // melee dagannoth
        room(c, 1936, 4372, 1951, 4380, P1, 5);                    // rock lobsters by the old Lighthouse ladder
        // ---- plane 2 (sublevel 3)
        room(c, 1793, 4380, 1827, 4410, P2, 6);                    // dagannoth and a wallasalki
        hall(c, 1810, 4380, 1810, 4372, P2);
        room(c, 1800, 4359, 1820, 4372, P2, 6);                    // giant rock crabs
        hall(c, 1827, 4384, 1837, 4384, P2);
        room(c, 1837, 4367, 1866, 4385, P2, 6);                    // dagannoth
        room(c, 1926, 4380, 1960, 4397, P2, 6);                    // rock lobsters
        // ---- plane 3 (sublevel 4)
        room(c, 1823, 4373, 1841, 4382, P3, 6);                    // melee dagannoth
        hall(c, 1832, 4382, 1832, 4393, P3);
        room(c, 1826, 4393, 1852, 4410, P3, 6);                    // giant rock crabs
        hall(c, 1826, 4404, 1807, 4404, P3);
        room(c, 1796, 4400, 1807, 4411, P3, 6);                    // wallasalki
        hall(c, 1852, 4400, 1884, 4400, P3);
        room(c, 1881, 4397, 1887, 4403, P3, 5);                    // the ladder down to sublevel 5
        room(c, 1955, 4387, 1961, 4393, P3, 5);
        hall(c, 1958, 4390, 1966, 4396, P3);
        room(c, 1965, 4394, 1975, 4407, P3, 5);                    // rock lobsters
        // ---- plane 0 (sublevel 5)
        room(c, 1876, 4356, 1916, 4380, P0, 8);                    // the great hall with the Kings' ladder
        room(c, 1880, 4381, 1910, 4412, P0, 6);                    // the rock lobsters' corridor
        hall(c, 1916, 4365, 1940, 4365, P0);
        room(c, 1940, 4353, 1960, 4371, P0, 6);                    // rock lobsters (east)
        hall(c, 1958, 4371, 1958, 4390, P0);
        room(c, 1955, 4387, 1961, 4393, P0, 5);
        c.carve();

        // Ladders: down from sublevel 1, then through the sublevels; the rock lobster rooms climb back up.
        ladder(c, 1900, 4360, P1, L1);
        ladder(c, 1800, 4385, P2, P1);
        ladder(c, 1839, 4377, P3, P2);
        ladder(c, 1884, 4400, P0, P3);
        ladder(c, 1958, 4390, P0, P3);
        ladder(c, 1960, 4390, P3, P2);
        ladder(c, 1944, 4380, P2, P1);
        // The Kings' ladder in the great hall.
        c.set(xs(1896), P0 + 1, zs(4370), rc(DungeonBlocks.KINGS_LADDER));

        // Sublevel 1: tide pools and dagannoth eggs in the hall, rocks, and the door-supports beside the central ladder.
        c.pool(x1(2450), z1(10140), x1(2455), z1(10136), L1, mc("water"));
        c.pool(x1(2470), z1(10166), x1(2476), z1(10162), L1, mc("water"));
        c.pool(x1(2480), z1(10134), x1(2484), z1(10130), L1, mc("water"));
        for (int[] p : new int[][]{{2444, 10137}, {2452, 10133}, {2454, 10129}, {2459, 10127}, {2465, 10125}, {2475, 10126},
                {2485, 10123}, {2447, 10164}, {2459, 10169}, {2477, 10158}, {2458, 10147}, {2466, 10141}, {2489, 10144},
                {2492, 10155}}) {
            c.set(x1(p[0]), L1 + 1, z1(p[1]), mc("turtle_egg[eggs=4]"));
        }
        for (int[] p : new int[][]{{2460, 10150}, {2478, 10136}, {2470, 10146}}) {
            c.pillar(x1(p[0]), z1(p[1]), L1, 8, mc("dripstone_block"));
        }
        for (int[] p : new int[][]{{2543, 10146}, {2547, 10140}, {2547, 10146}}) {
            c.set(x1(p[0]), L1 + 1, z1(p[1]), mc("stripped_spruce_log[axis=y]"));
            c.set(x1(p[0]), L1 + 2, z1(p[1]), mc("stripped_spruce_log[axis=y]"));
        }
        c.set(x1(2520), L1 + 1, z1(10144), mc("bone_block[axis=x]"));
        c.set(x1(2521), L1 + 1, z1(10144), mc("skeleton_skull[rotation=4]"));
        // The sublevels: puddles, kelp and the bones of past adventurers.
        c.pool(xs(1806), zs(4392), xs(1808), zs(4388), P1, mc("water"));
        c.pool(xs(1902), zs(4376), xs(1906), zs(4372), P0, mc("water"));
        c.pool(xs(1886), zs(4362), xs(1889), zs(4358), P0, mc("water"));
        c.set(xs(1850), P1 + 1, zs(4392), mc("skeleton_skull[rotation=8]"));
        c.set(xs(1890), P0 + 1, zs(4405), mc("bone_block[axis=y]"));

        c.entrance(x1(ARRIVAL_X), z1(ARRIVAL_Y), L1, mc("stone"), mc("gravel"), 'e');

        // Monsters: the cache's counts in every room (respawn: dagannoth 30 ticks, wallasalki 25, giant rock crabs 50,
        // eggs 5 minutes; rock lobsters approx 30).
        camp(c, 2440, 10120, 2492, 10170, L1, 54, 30, "dagannoth_lvl90");
        camp(c, 2440, 10120, 2492, 10170, L1, 33, 500, "dagannoth_spawn");
        camp(c, 2442, 10153, 2472, 10167, L1, 18, 25, "rock_crab");
        camp(c, 2495, 10118, 2552, 10134, L1, 39, 30, "dagannoth_lvl88");
        camp(c, 2495, 10137, 2539, 10150, L1, 29, 25, "wallasalki");
        camp(c, 2495, 10153, 2551, 10168, L1, 27, 50, "giant_rock_crab");
        // plane 1
        camp(c, 1829, 4356, 1866, 4367, P1, 4, 30, "dagannoth_lvl90");
        camp(c, 1829, 4356, 1866, 4367, P1, 5, 30, "dagannoth_lvl88");
        camp(c, 1795, 4369, 1810, 4395, P1, 9, 25, "wallasalki");
        camp(c, 1844, 4385, 1854, 4397, P1, 5, 30, "dagannoth_fledgeling");
        camp(c, 1858, 4393, 1892, 4412, P1, 5, 30, "dagannoth_lvl90");
        camp(c, 1936, 4372, 1951, 4380, P1, 2, 30, "rock_lobster");
        // plane 2
        camp(c, 1793, 4380, 1827, 4410, P2, 5, 30, "dagannoth_lvl90");
        camp(c, 1793, 4380, 1827, 4410, P2, 5, 30, "dagannoth_lvl88");
        camp(c, 1793, 4380, 1827, 4410, P2, 1, 25, "wallasalki");
        camp(c, 1800, 4359, 1820, 4372, P2, 5, 50, "giant_rock_crab");
        camp(c, 1837, 4367, 1866, 4385, P2, 4, 30, "dagannoth_lvl90");
        camp(c, 1837, 4367, 1866, 4385, P2, 5, 30, "dagannoth_lvl88");
        camp(c, 1926, 4380, 1960, 4397, P2, 7, 30, "rock_lobster");
        // plane 3
        camp(c, 1823, 4373, 1841, 4382, P3, 3, 30, "dagannoth_lvl90");
        camp(c, 1826, 4393, 1852, 4410, P3, 6, 50, "giant_rock_crab");
        camp(c, 1796, 4400, 1807, 4411, P3, 3, 25, "wallasalki");
        camp(c, 1965, 4394, 1975, 4407, P3, 3, 30, "rock_lobster");
        // plane 0
        camp(c, 1876, 4356, 1916, 4380, P0, 6, 30, "dagannoth_lvl90");
        camp(c, 1876, 4356, 1916, 4380, P0, 4, 30, "dagannoth_lvl88");
        camp(c, 1876, 4356, 1916, 4380, P0, 3, 50, "giant_rock_crab");
        camp(c, 1880, 4356, 1910, 4412, P0, 5, 25, "wallasalki");
        camp(c, 1880, 4381, 1910, 4412, P0, 10, 30, "rock_lobster");
        camp(c, 1940, 4353, 1960, 4371, P0, 2, 30, "rock_lobster");
        return c.build();
    }
}
