package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The OSRS terrain model: coasts, rivers, mountains, and room for every structure and dungeon. */
class ElevationTest {

    private static int bx(double osrsX) {
        return GielinorMap.toBlockX((int) Math.floor(osrsX));
    }

    private static int bz(double osrsY) {
        return GielinorMap.toBlockZ((int) Math.floor(osrsY));
    }

    private static double h(double osrsX, double osrsY) {
        return Elevation.height(bx(osrsX), bz(osrsY));
    }

    /** Highest point within {@code r} tiles. */
    private static double peak(double x, double y, int r) {
        double best = -1;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dy = -r; dy <= r; dy += 2) {
                best = Math.max(best, h(x + dx, y + dy));
            }
        }
        return best;
    }

    @Test
    void openSeaIsBelowSeaLevel() {
        double[][] sea = {
                {2050, 3250}, // west of Tirannwn
                {2000, 3500}, // between Zeah and the mainland
                {2880, 2840}, // south of Karamja
                {2550, 2790}, // south of the Feldip Hills
                {3850, 3400}, // east of Morytania
                {3620, 2900}, // east of the Kharidian Desert
                {3150, 4030}, // north of the Wilderness
                {2700, 3800}, // north of the Fremennik Province
                {3055, 3150}, // the sea between Port Sarim and the Wizards' Tower
                {2886, 3340}, // between Entrana and Asgarnia
        };
        for (double[] p : sea) {
            double v = h(p[0], p[1]);
            assertTrue(v < Elevation.SEA_LEVEL - 2, "sea at " + p[0] + "," + p[1] + " is " + v);
        }
        assertTrue(Elevation.isOcean(bx(2050), bz(3250)));
        assertTrue(Elevation.isOcean(bx(3850), bz(3400)));
    }

    @Test
    void landmassesAndIslandsRiseAboveTheSea() {
        double[][] land = {
                {3222, 3218}, {3213, 3428}, {2965, 3380}, {2757, 3477}, {2662, 3305}, {3496, 3488}, // mainland towns
                {2850, 3060}, // Karamja
                {2836, 3262}, // Crandor
                {2836, 3345}, // Entrana
                {3104, 3102}, // Tutorial Island
                {3764, 3869}, // Fossil Island
                {2755, 2780}, // Ape Atoll
                {2532, 3869}, // Miscellania
                {2110, 3905}, // Lunar Isle
                {2336, 3804}, // Neitiznot
                {1639, 3681}, // Kourend Castle (Zeah)
                {1739, 3126}, // Civitas illa Fortis (Varlamore)
        };
        for (double[] p : land) {
            double v = h(p[0], p[1]);
            assertTrue(v > Elevation.SEA_LEVEL, "land at " + p[0] + "," + p[1] + " is " + v);
        }
        // Karamja and Crandor are separate islands, Crandor and Entrana too
        assertTrue(h(2836, 3214) < Elevation.SEA_LEVEL, "channel between Karamja and Crandor");
        assertTrue(h(2912, 3193) < Elevation.SEA_LEVEL, "strait between Karamja and Rimmington: " + h(2912, 3193));
    }

    /** The Sailing quests' places stand on proper islands, not just on their plaza's islet. */
    @Test
    void sailingIslandsHoldTheirQuestSites() {
        List<String> ids = List.of("quest_the_pandemonium", "quest_tortugan_islet", "quest_great_conch", "quest_gryphon_lair",
                "quest_little_pearl", "quest_sacred_grove", "quest_red_rock", "quest_last_light", "quest_zenith",
                "quest_wyrmscraig", "quest_wyrmscraig_landing", "quest_wyrmscraig_cave", "quest_wyrmscraig_cathedral");
        for (String id : ids) {
            PlaceDef p = ContentRegistry.place(id);
            assertTrue(p != null, id);
            double inland = Elevation.landDistance(p.osrsX(), p.osrsY());
            assertTrue(inland >= 3, id + " is only " + inland + " tiles from the coast");
        }
        // The big islands are islands, with open sea around them.
        for (double[] land : new double[][]{{3186, 2443}, {3150, 2440}, {3225, 2460}, {2566, 2250}, {2540, 2290}, {2855, 2327}}) {
            assertTrue(Elevation.landDistance(land[0], land[1]) > 8, "inland " + land[0] + "," + land[1]);
        }
        for (double[] sea : new double[][]{{3186, 2560}, {3110, 2440}, {2566, 2350}, {2700, 2400}, {3000, 2967}}) {
            assertTrue(h(sea[0], sea[1]) < Elevation.SEA_LEVEL - 2, "sea at " + sea[0] + "," + sea[1]);
        }
    }

    @Test
    void riversCutThroughTheLand() {
        // River Lum east of Lumbridge Castle (inside the town's hand-built channel), north of the town
        // between the chicken farm and the cow field, and south towards the swamp.
        assertTrue(Elevation.isWater(bx(3240), bz(3240)), "River Lum east of the castle: " + h(3240, 3240));
        assertTrue(Elevation.isWater(bx(3243), bz(3280)), "River Lum north of Lumbridge: " + h(3243, 3280));
        assertTrue(Elevation.isWater(bx(3256), bz(3188)), "River Lum south of Lumbridge: " + h(3256, 3188));
        // banks are dry
        assertTrue(h(3232, 3280) > Elevation.SEA_LEVEL);
        // River Salve on the Morytanian border, the Elid between Pollnivneach and Nardah
        assertTrue(Elevation.isWater(bx(3442), bz(3460)), "River Salve: " + h(3442, 3460));
        assertTrue(Elevation.isWater(bx(3390), bz(2950)), "River Elid: " + h(3390, 2950));
        // Barbarian Village's river
        assertTrue(Elevation.isWater(bx(3104), bz(3425)), "Barbarian Village river: " + h(3104, 3425));
    }

    @Test
    void mountainsAreHigh() {
        assertTrue(peak(3009, 3478, 6) > 95, "Ice Mountain " + peak(3009, 3478, 6));
        assertTrue(peak(2822, 3492, 6) > 105, "White Wolf Mountain " + peak(2822, 3492, 6));
        assertTrue(peak(2912, 3730, 10) > 140, "Troll Country " + peak(2912, 3730, 10));
        assertTrue(peak(2760, 3690, 10) > 125, "Fremennik mountains " + peak(2760, 3690, 10));
        assertTrue(peak(2305, 3395, 10) > 150, "Arandar mountains " + peak(2305, 3395, 10));
        assertTrue(peak(1245, 3565, 10) > 125, "Mount Quidamortem " + peak(1245, 3565, 10));
        // lowland farmland stays low and gentle
        assertTrue(h(3180, 3240) < 80, "Lumbridge countryside " + h(3180, 3240));
        // snow on the high peaks
        assertTrue(Elevation.isSnowyPeak(bx(2912), bz(3730)) || Elevation.isSnowyPeak(bx(2905), bz(3810)));
        assertTrue(!Elevation.isSnowyPeak(bx(3222), bz(3218)));
    }

    @Test
    void structuresSitOnLevelledGround() {
        List<String> bad = new ArrayList<>();
        for (TerrainAnchors.Anchor a : TerrainAnchors.get().anchors()) {
            if (a.kind() != TerrainAnchors.Kind.CORE || a.source().contains(":")) {
                continue;
            }
            int m = (int) TerrainAnchors.STRUCTURE_RADIUS;
            for (int x = a.x1() - m; x <= a.x2() + m; x += 4) {
                for (int z = a.z1() - m; z <= a.z2() + m; z += 4) {
                    if (Elevation.isWaterFeature(x, z)) {
                        continue;
                    }
                    double v = Elevation.height(x, z);
                    double d = a.distance(x, z);
                    if (d > a.radius()) {
                        continue;
                    }
                    double tol = d == 0 ? 1.5 : TerrainAnchors.BAND + 0.75;
                    if (Math.abs(v - a.target()) > tol && !nearOtherLevel(a, x, z)) {
                        bad.add(a.source() + " @" + x + "," + z + " d=" + Math.round(d) + " h=" + Math.round(v) + " want " + a.target());
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " points off: " + bad.subList(0, Math.min(20, bad.size())));
    }

    /** Next to another levelled area at a different height (e.g. a mountain-top fort) the bands overlap. */
    private static boolean nearOtherLevel(TerrainAnchors.Anchor self, int x, int z) {
        for (TerrainAnchors.Anchor o : TerrainAnchors.get().anchors()) {
            if (o != self && o.kind() == TerrainAnchors.Kind.CORE && Math.abs(o.target() - self.target()) > 1
                    && o.distance(x, z) <= o.radius() + TerrainAnchors.BAND) {
                return true;
            }
        }
        return false;
    }

    @Test
    void placesAndLandmarksHaveFlatPlazas() {
        List<String> bad = new ArrayList<>();
        int near64 = 0;
        int total = 0;
        for (PlaceDef p : ContentRegistry.places()) {
            if (!TerrainAnchors.hasPlaza(p)) {
                continue;
            }
            total++;
            double c = Elevation.height(p.blockX(), p.blockZ());
            near64 += Math.abs(c - Elevation.GROUND) <= 6 ? 1 : 0;
            for (int[] o : new int[][]{{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
                double v = Elevation.height(p.blockX() + o[0], p.blockZ() + o[1]);
                if (Math.abs(v - c) > 2 && !Elevation.isWaterFeature(p.blockX() + o[0], p.blockZ() + o[1])) {
                    bad.add(p.id() + " " + Math.round(c) + "/" + Math.round(v));
                    break;
                }
            }
            assertTrue(c >= Elevation.SEA_LEVEL || Elevation.isWaterFeature(p.blockX(), p.blockZ()), p.id() + " is under water: " + c);
        }
        for (Landmark l : Landmark.values()) {
            if (TerrainAnchors.MOUNTAIN_PLACES.contains(l.name())) {
                continue;
            }
            total++;
            double c = Elevation.height(l.blockX(), l.blockZ());
            near64 += Math.abs(c - Elevation.GROUND) <= 6 ? 1 : 0;
            assertTrue(c > Elevation.SEA_LEVEL, l + " is under water: " + c);
        }
        assertTrue(bad.size() <= total / 50, "plazas on slopes: " + bad);
        // most of the world's places are in the lowlands at town level
        assertTrue(near64 >= total * 0.8, near64 + " of " + total + " places near y 64");
    }

    @Test
    void dungeonsStayUnderSolidRock() {
        List<String> bad = new ArrayList<>();
        for (StructureDef def : ContentRegistry.structures()) {
            for (StructureDef.Op op : def.ops()) {
                if (op.y2() >= TerrainAnchors.UNDERGROUND_BELOW || op.type() == StructureDef.Op.Type.FLATTEN) {
                    continue;
                }
                int need = Math.max(op.y2() + TerrainAnchors.ROCK_COVER, 55);
                int[][] pts = {{op.x1(), op.z1()}, {op.x2(), op.z2()}, {op.x1(), op.z2()}, {op.x2(), op.z1()},
                        {(op.x1() + op.x2()) / 2, (op.z1() + op.z2()) / 2}};
                for (int[] p : pts) {
                    int top = Elevation.surfaceY(p[0], p[1]);
                    if (top < need) {
                        bad.add(def.id() + " @" + p[0] + "," + p[1] + " top=" + top + " need " + need);
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " uncovered: " + bad.subList(0, Math.min(20, bad.size())));
    }

    @Test
    void modelIsDeterministicAndCheap() {
        double a = Elevation.rawHeight(123, -456);
        Elevation.clearCache();
        double b = Elevation.rawHeight(123, -456);
        assertEquals(a, b, 1e-9);
        // the cached grid agrees with the full model at grid points
        assertEquals(Elevation.rawHeight(128, -448), Elevation.height(128, -448), 1e-3);
        long t = System.nanoTime();
        double sum = 0;
        for (int x = 2000; x < 2512; x++) {
            for (int z = 0; z < 512; z++) {
                sum += Elevation.height(x, z);
            }
        }
        double secs = (System.nanoTime() - t) / 1e9;
        assertTrue(sum > 0 && secs < 20, "512x512 columns took " + secs + "s");
    }
}
