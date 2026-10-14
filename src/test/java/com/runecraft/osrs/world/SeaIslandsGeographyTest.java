package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Sailing-era islands traced from the OSRS map squares, against cache/wiki tiles and the open sea around them. */
class SeaIslandsGeographyTest {
    private static double h(int x, int y) {
        return Elevation.height(GielinorMap.toBlockX(x), GielinorMap.toBlockZ(y));
    }

    private static boolean inside(double[] xy, double x, double y) {
        boolean in = false;
        int n = xy.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = xy[2 * i], yi = xy[2 * i + 1], xj = xy[2 * j], yj = xy[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    @Test
    void everyTracedIslandRisesAboveTheSea() {
        for (GielinorGeography.Polygon p : SeaIslands.ALL) {
            double[] xy = p.xy();
            double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
            for (int i = 0; i < xy.length; i += 2) {
                minX = Math.min(minX, xy[i]);
                maxX = Math.max(maxX, xy[i]);
                minY = Math.min(minY, xy[i + 1]);
                maxY = Math.max(maxY, xy[i + 1]);
            }
            int tiles = 0;
            int land = 0;
            for (int y = (int) minY; y <= maxY; y++) {
                for (int x = (int) minX; x <= maxX; x++) {
                    if (inside(xy, x + 0.5, y + 0.5)) {
                        tiles++;
                        if (h(x, y) > Elevation.SEA_LEVEL) {
                            land++;
                        }
                    }
                }
            }
            assertTrue(tiles > 0 && land >= 0.6 * tiles, p.name() + ": " + land + " of " + tiles + " tiles are land");
        }
    }

    @Test
    void cacheTilesAreOnLand() {
        int[][] land = {
                {2200, 2328}, {2231, 2334}, {2223, 2347}, {2203, 2316}, {2211, 2320},  // Sunbleak Island ironwood trees
                {2135, 3548}, {2154, 3543}, {2150, 3555}, {2149, 3558}, {2137, 3544},  // Drumstick Isle rosewood trees
                {2975, 2255}, {2974, 2252}, {2978, 2251},                              // the Onyx Crest's lead rocks
                {2980, 2284}, {2982, 2288},                                            // Onyx Crest pirates
                {1160, 2778}, {1165, 2782}, {1161, 2786},                              // Laguna Aurorae pirates
                {2100, 3682}, {2092, 3685}, {2095, 3679},                              // Buccaneers' Haven pirates
                {3050, 2966}, {3040, 2980},                                            // the Pandemonium's pub and island
                {2963, 2264},                                                          // the fearless bird's nest (charting)
                {2067, 2606},                                                          // Wintumber Island
                {2334, 2270},                                                          // Rainbow's End
        };
        for (int[] p : land) {
            assertTrue(h(p[0], p[1]) > Elevation.SEA_LEVEL, "land at " + p[0] + "," + p[1] + ": " + h(p[0], p[1]));
        }
        int[][] sea = {
                {2209, 2290},  // south of Sunbleak Island
                {2260, 2330},  // east of Sunbleak Island (tangled kelp)
                {2147, 3580},  // north of Drumstick Isle
                {2180, 3545},  // between Drumstick Isle and Tirannwn
                {2080, 3640},  // south of Buccaneers' Haven
                {2975, 2230},  // south of the Onyx Crest
                {1200, 2700},  // south of Laguna Aurorae
                {3030, 2940},  // south-west of the Pandemonium
                {3085, 2985},  // east of the Pandemonium
        };
        for (int[] p : sea) {
            assertTrue(h(p[0], p[1]) < Elevation.SEA_LEVEL, "sea at " + p[0] + "," + p[1] + ": " + h(p[0], p[1]));
        }
    }
}
