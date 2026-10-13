package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Great Kourend's, the Kebos Lowlands' and Varlamore's coasts, lakes and rivers against the OSRS world map (mejrs/layers_osrs
 * map squares; every land tile below is at least 4 tiles from the map's water and every water tile at least 4 tiles from
 * its land), plus the transport tiles of shortest-path and the places built there.
 */
class ZeahGeographyTest {
    private static double h(int x, int y) {
        return Elevation.height(GielinorMap.toBlockX(x), GielinorMap.toBlockZ(y));
    }

    private static boolean zeah(int x, int y) {
        return x >= 1050 && x < 2000 && y >= 2810 && y < 4160;
    }

    @Test
    void townsAndLandmarksStandOnLand() {
        Object[][] land = {
                {1639, 3681, Region.KOUREND, "Kourend Castle"}, {1762, 3598, Region.KOUREND, "Hosidius"},
                {1517, 3592, Region.KOUREND, "Shayzien"}, {1700, 3800, Region.KOUREND, "Arceuus"},
                {1505, 3801, Region.KOUREND, "Lovakengj"}, {1803, 3752, Region.KOUREND, "Port Piscarilius"},
                {1249, 3737, Region.KOUREND, "the Farming Guild"}, {1311, 3807, Region.KOUREND, "Mount Karuulm"},
                {1510, 3430, Region.KOUREND, "Land's End"}, {1340, 3700, Region.KOUREND, "the Battlefront's shore"},
                {1368, 3633, Region.KOUREND, "Molch Island in Lake Molch"},
                {1680, 3130, Region.VARLAMORE, "Civitas illa Fortis"}, {1800, 3105, Region.VARLAMORE, "the Fortis Colosseum"},
                {1391, 2935, Region.VARLAMORE, "Aldarin"}, {1387, 2862, Region.VARLAMORE, "Mistrock"},
                {1344, 3022, Region.VARLAMORE, "Kastori"}, {1226, 3091, Region.VARLAMORE, "Tal Teklan"},
                {1548, 2995, Region.VARLAMORE, "the Sunset Coast"}, {1585, 3053, Region.VARLAMORE, "the Hunter Guild"},
                {1411, 3361, Region.VARLAMORE, "Auburnvale"}, {1510, 3222, Region.VARLAMORE, "Quetzacalli Gorge"},
                {1888, 3298, Region.VARLAMORE, "Port Roberts' island"}, {1285, 3256, Region.VARLAMORE, "Tempestus' island"},
                {1767, 3420, Region.KOUREND, "Crabclaw Isle"},
        };
        for (Object[] p : land) {
            int x = (int) p[0];
            int y = (int) p[1];
            assertTrue(h(x, y) > Elevation.SEA_LEVEL, p[3] + " at " + x + "," + y + " is under water: " + h(x, y));
            assertEquals(p[2], GielinorMap.regionAt(x, y), (String) p[3]);
        }
    }

    @Test
    void seaLakesAndStraitsAreWater() {
        Object[][] water = {
                {1395, 3640, "Lake Molch"}, {1275, 3392, "Custodia's lake"}, {1600, 3555, "the lake in the Kourend Woodland"},
                {1410, 3430, "the strait between Kourend and Varlamore"}, {1650, 3420, "the bay south of the Kourend Woodland"},
                {1345, 3275, "Tempest Bay east of Tempestus"}, {1215, 3255, "Tempest Bay west of Tempestus"},
                {1840, 3420, "the channel east of Crabclaw Isle"}, {1480, 2990, "the strait north of Aldarin's island"},
                {1700, 2870, "the sea south of Varlamore"}, {1880, 3700, "the sea off Port Piscarilius"},
                {1300, 3950, "the frozen sea north of Mount Karuulm"}, {1060, 3300, "the sea west of the Custodia Mountains"},
                {1150, 3520, "the sea west of Mount Quidamortem"},
        };
        for (Object[] p : water) {
            int x = (int) p[0];
            int y = (int) p[1];
            assertTrue(h(x, y) < Elevation.SEA_LEVEL, p[2] + " at " + x + "," + y + " is dry: " + h(x, y));
        }
        // Molch Island is a real island: open water on every side of it
        for (int[] d : new int[][]{{1368, 3615}, {1368, 3652}, {1350, 3633}, {1388, 3633}}) {
            assertTrue(h(d[0], d[1]) < Elevation.SEA_LEVEL, "Lake Molch at " + d[0] + "," + d[1] + ": " + h(d[0], d[1]));
        }
    }

    @Test
    void riversFlowWhereTheMapDrawsThem() {
        int[][] rivers = {
                {1703, 3563}, // the River Hos west of Hosidius
                {1683, 3587}, // ... and its bend north
        };
        for (int[] r : rivers) {
            assertTrue(Elevation.isWater(GielinorMap.toBlockX(r[0]), GielinorMap.toBlockZ(r[1])), "river at " + r[0] + "," + r[1]);
        }
    }

    @Test
    void varlamoreHasItsMountains() {
        double ralos = 0;
        double proudspire = 0;
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dy = -8; dy <= 8; dy += 2) {
                ralos = Math.max(ralos, h(1450 + dx, 3130 + dy));
                proudspire = Math.max(proudspire, h(1570 + dx, 3238 + dy));
            }
        }
        assertTrue(ralos > 130, "Ralos' Rise " + ralos);
        assertTrue(proudspire > 120, "the Proudspire " + proudspire);
        assertTrue(h(1411, 3361) < 80, "Auburnvale lies in a valley: " + h(1411, 3361));
    }

    /** Every place with a plaza, arena and monster camp in Zeah and Varlamore is on dry ground (or a hand-built water feature). */
    @Test
    void everythingBuiltInZeahIsOnLand() {
        List<String> wet = new ArrayList<>();
        for (PlaceDef p : ContentRegistry.places()) {
            if (zeah(p.osrsX(), p.osrsY()) && p.style() != PlaceDef.Style.NONE
                    && Elevation.height(p.blockX(), p.blockZ()) <= Elevation.SEA_LEVEL && !Elevation.isWaterFeature(p.blockX(), p.blockZ())) {
                wet.add(p.id());
            }
        }
        for (Map.Entry<String, ArenaLayout.Arena> e : ArenaLayout.all().entrySet()) {
            ArenaLayout.Arena a = e.getValue();
            if (zeah(GielinorMap.toOsrsX(a.x()), GielinorMap.toOsrsY(a.z())) && Elevation.height(a.x(), a.z()) <= Elevation.SEA_LEVEL) {
                wet.add("arena " + e.getKey());
            }
        }
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            if (c.floorY() == SpawnPoint.SURFACE && zeah(c.x(), c.y()) && h(c.x(), c.y()) <= Elevation.SEA_LEVEL) {
                wet.add("camp " + c.creature() + " @" + c.x() + "," + c.y());
            }
        }
        for (StructureDef d : ContentRegistry.structures()) {
            int cx = (d.minX() + d.maxX()) / 2;
            int cz = (d.minZ() + d.maxZ()) / 2;
            if (zeah(GielinorMap.toOsrsX(cx), GielinorMap.toOsrsY(cz)) && Elevation.height(cx, cz) <= Elevation.SEA_LEVEL
                    && !Elevation.isWaterFeature(cx, cz)) {
                wet.add("structure " + d.id());
            }
        }
        assertTrue(wet.isEmpty(), "under water: " + wet);
    }
}
