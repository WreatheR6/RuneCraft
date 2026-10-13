package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fossil Island's coast against the OSRS map and cache tiles. */
class FossilIslandGeographyTest {
    private static double h(int x, int y) {
        return Elevation.height(GielinorMap.toBlockX(x), GielinorMap.toBlockZ(y));
    }

    @Test
    void cacheTilesAreOnTheIsland() {
        int[][] land = {
                {3683, 3706},  // the Deranged archaeologist (south end of the Tar Swamp)
                {3676, 3755},  // Sticky swamp Magic Mushtree
                {3757, 3756},  // Verdant Valley Magic Mushtree
                {3676, 3871},  // Mushroom Meadow Magic Mushtree
                {3764, 3879},  // House on the Hill Magic Mushtree
                {3724, 3807},  // Museum Camp rowboat
                {3811, 3813},  // Volcanic Mine
                {3680, 3854},  // the Wyvern Cave trapdoor
                {3734, 3893},  // the north coast rowboat
                {3818, 3809},  // Petrified Pete
        };
        for (int[] p : land) {
            assertTrue(h(p[0], p[1]) > Elevation.SEA_LEVEL, "land at " + p[0] + "," + p[1] + ": " + h(p[0], p[1]));
            assertTrue(GielinorMap.regionAt(p[0], p[1]) == Region.FOSSIL_ISLAND, p[0] + "," + p[1]);
        }
        int[][] sea = {
                {3718, 3765},  // the channel between the Tar Swamp and the Verdant Valley
                {3738, 3870},  // the bay west of the House on the Hill
                {3700, 3690},  // south of the Tar Swamp
                {3790, 3715},  // off the south coast
                {3860, 3800},  // east of the volcano
        };
        for (int[] p : sea) {
            assertTrue(h(p[0], p[1]) < Elevation.SEA_LEVEL, "sea at " + p[0] + "," + p[1] + ": " + h(p[0], p[1]));
        }
    }

    /** Places snap to chunk centres (8 tiles apart), so the arena centre can be up to about 5 tiles off. */
    @Test
    void derangedArchaeologistFightsOnHisCacheTile() {
        ArenaLayout.Arena a = ArenaLayout.of("deranged_archaeologist");
        assertNotNull(a);
        double dx = GielinorMap.toOsrsX(a.x()) - 3683;
        double dy = GielinorMap.toOsrsY(a.z()) - 3706;
        assertTrue(Math.hypot(dx, dy) <= 5, "arena centre " + GielinorMap.toOsrsX(a.x()) + "," + GielinorMap.toOsrsY(a.z()));
    }
}
