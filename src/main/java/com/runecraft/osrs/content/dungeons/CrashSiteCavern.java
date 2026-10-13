package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * The Crash Site Cavern (Monkey Madness II) beneath the wreck of Glough's airship, north-west of the Grand Tree. The
 * surface hut stands just north of the opening in the Gnome Stronghold's fence (OSRS tile 2436, 3520); below it the
 * cavern follows the OSRS map of the real dungeon (cavern tiles 2068-2160, 5638-5685, re-anchored so that the
 * entrance chamber lies under the hut). Every demonic and tortured gorilla camp sits where the OSRS cache spawns them
 * (mejrs/data_osrs NPCList_OSRS.json): 28 demonic gorillas and 8 tortured gorillas, respawning after 100 ticks.
 * Entering needs Monkey Madness II (systems module's access gate on the hut).
 */
final class CrashSiteCavern {
    /** OSRS cavern tile of the entrance chamber and the surface tile of the hut above it. */
    static final int CAVE_X = 2140;
    static final int CAVE_Y = 5645;
    static final int HUT_X = 2438;
    static final int HUT_Y = 3528;
    private static final int FLOOR = 20;
    private static final int HEIGHT = 8;
    private static final int RESPAWN = 100;

    private CrashSiteCavern() {
    }

    /** Block x of an OSRS cavern tile. */
    private static int x(int caveX) {
        return StructureDef.bx(caveX - CAVE_X + HUT_X);
    }

    /** Block z of an OSRS cavern tile. */
    private static int z(int caveY) {
        return StructureDef.bz(caveY - CAVE_Y + HUT_Y);
    }

    /** A chamber over the cavern tile rectangle (inclusive). */
    private static void room(Cave c, int tx1, int ty1, int tx2, int ty2) {
        c.room(x(tx1), z(ty2), x(tx2) + 1, z(ty1) + 1, FLOOR, HEIGHT);
    }

    /** A camp over the cavern tile rectangle (inclusive), one block in from the walls. */
    private static void camp(Cave c, int tx1, int ty1, int tx2, int ty2, int cap, String creature) {
        c.camp(x(tx1) + 1, z(ty2) + 1, x(tx2), z(ty1), FLOOR, cap, RESPAWN, creature);
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_crash_site_cavern", "Crash Site Cavern", mc("deepslate"), mc("coarse_dirt"), mc("shroomlight"));
        room(c, 2128, 5639, 2156, 5652);   // entrance chamber: tortured gorillas
        room(c, 2116, 5655, 2160, 5664);   // the eastern gallery
        room(c, 2118, 5667, 2160, 5684);   // the north-eastern (multi-way) hall
        room(c, 2090, 5638, 2114, 5682);   // the great central cavern
        room(c, 2068, 5640, 2086, 5685);   // the western (single-way) caves
        room(c, 2139, 5652, 2142, 5655);   // passages between them
        room(c, 2134, 5664, 2137, 5667);
        room(c, 2114, 5659, 2116, 5662);
        room(c, 2086, 5649, 2090, 5652);
        room(c, 2086, 5673, 2090, 5676);
        c.carve();
        // The wreckage of Glough's airship came down through the roof: timbers, scrap metal and skeletal remains.
        for (int[] p : new int[][]{{2100, 5665}, {2104, 5648}, {2076, 5660}, {2145, 5675}}) {
            c.pillar(x(p[0]), z(p[1]), FLOOR, HEIGHT, mc("stripped_dark_oak_log"));
        }
        c.set(x(2137), FLOOR + 1, z(5641), mc("skeleton_skull[rotation=6]"));
        c.set(x(2138), FLOOR + 1, z(5641), mc("bone_block[axis=x]"));
        c.set(x(2144), FLOOR + 1, z(5642), mc("bone_block[axis=z]"));
        c.set(x(2108), FLOOR + 1, z(5670), mc("chain[axis=y]"));
        c.fill(x(2096), FLOOR + 1, z(5664), x(2098), FLOOR + 1, z(5664), mc("iron_bars"));
        c.pool(x(2078), z(5668), x(2081), z(5666), FLOOR, mc("water"));
        c.entrance(x(CAVE_X), z(CAVE_Y), FLOOR, mc("stripped_spruce_log"), mc("coarse_dirt"), 's');
        camp(c, 2130, 5642, 2152, 5651, 5, "tortured_gorilla");
        camp(c, 2117, 5656, 2159, 5663, 3, "demonic_gorilla");
        camp(c, 2130, 5658, 2144, 5663, 3, "tortured_gorilla");
        camp(c, 2120, 5668, 2159, 5683, 6, "demonic_gorilla");
        camp(c, 2091, 5639, 2113, 5681, 12, "demonic_gorilla");
        camp(c, 2069, 5641, 2085, 5684, 7, "demonic_gorilla");
        return c.build();
    }
}
