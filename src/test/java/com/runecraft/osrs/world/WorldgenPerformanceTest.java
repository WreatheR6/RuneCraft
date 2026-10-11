package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the world-generation hot paths that were optimised (structure chunk index, compact built-share
 * set, the terrain tile cache and pre-sorted anchor lists): each must give exactly what the
 * straightforward version gives, and stay fast. Time limits are generous (several times the measured
 * cost) so slow CI machines don't flake.
 */
class WorldgenPerformanceTest {

    // ------------------------------------------------------------------ StructureIndex

    /** The original StructurePlacer indexing: every chunk of each bounding box, keep it if any op touches it. */
    private static Map<Long, List<StructureDef>> referenceByChunk(List<StructureDef> defs) {
        Map<Long, List<StructureDef>> by = new HashMap<>();
        for (StructureDef def : defs) {
            for (int cx = def.minX() >> 4; cx <= def.maxX() >> 4; cx++) {
                for (int cz = def.minZ() >> 4; cz <= def.maxZ() >> 4; cz++) {
                    final int fx = cx;
                    final int fz = cz;
                    if (def.ops().stream().anyMatch(op -> op.intersectsChunk(fx, fz))) {
                        by.computeIfAbsent(StructureIndex.key(cx, cz), k -> new ArrayList<>()).add(def);
                    }
                }
            }
        }
        return by;
    }

    @Test
    void structureIndexMatchesTheBoundingBoxScanForEveryChunk() {
        List<StructureDef> defs = ContentRegistry.structures();
        StructureIndex index = assertTimeoutPreemptively(Duration.ofSeconds(20), () -> StructureIndex.build(defs));
        Map<Long, List<StructureDef>> ref = referenceByChunk(defs);
        assertEquals(ref.size(), index.chunkCount(), "indexed chunk count");
        for (Map.Entry<Long, List<StructureDef>> e : ref.entrySet()) {
            int cx = StructureIndex.keyX(e.getKey());
            int cz = StructureIndex.keyZ(e.getKey());
            List<StructureIndex.Share> shares = index.at(cx, cz);
            assertNotNull(shares, "chunk " + cx + "," + cz);
            assertEquals(e.getValue().size(), shares.size(), "structures in chunk " + cx + "," + cz);
            for (int i = 0; i < shares.size(); i++) {
                StructureDef def = e.getValue().get(i);
                assertSame(def, shares.get(i).def(), "registry order in chunk " + cx + "," + cz);
                List<StructureDef.Op> expected = new ArrayList<>();
                for (StructureDef.Op op : def.ops()) {
                    if (op.intersectsChunk(cx, cz)) {
                        expected.add(op);
                    }
                }
                assertEquals(expected, shares.get(i).ops(), def.id() + " ops in chunk " + cx + "," + cz);
            }
        }
    }

    @Test
    void structureIndexKeysMatchMinecraftChunkPosPacking() {
        int[][] chunks = {{0, 0}, {1, 2}, {-1, 0}, {0, -1}, {-1234, 5678}, {Integer.MAX_VALUE >> 4, Integer.MIN_VALUE >> 4}};
        for (int[] c : chunks) {
            long mc = (long) c[0] & 4294967295L | ((long) c[1] & 4294967295L) << 32; // ChunkPos.toLong
            assertEquals(mc, StructureIndex.key(c[0], c[1]));
            assertEquals(c[0], StructureIndex.keyX(mc));
            assertEquals(c[1], StructureIndex.keyZ(mc));
        }
        StructureDef small = StructureDef.builder("t", "T").fill(15, 60, 15, 16, 60, 16, "minecraft:stone")
                .set(100, 60, 100, "minecraft:dirt").build();
        StructureIndex index = StructureIndex.build(List.of(small));
        assertEquals(5, index.chunkCount()); // the 2x2 fill straddles 4 chunks, the single block one more
        assertArrayEquals(new int[]{0}, index.at(0, 0).get(0).opIndices());
        assertArrayEquals(new int[]{1}, index.at(6, 6).get(0).opIndices());
        assertNull(index.at(2, 2));
        StructureDef flat = StructureDef.builder("f", "F").flatten(0, 0, 3, 3, "minecraft:grass_block").build();
        // FLATTEN reaches BLEND blocks further: into the chunks west and north too
        assertTrue(StructureIndex.build(List.of(flat)).contains(-1, -1));
    }

    // ------------------------------------------------------------------ BuiltChunks

    @Test
    void builtChunksStoresSharesAndReadsLegacyKeys() {
        BuiltChunks b = new BuiltChunks();
        assertFalse(b.isBuilt("lumbridge_castle", 3, -7));
        assertTrue(b.markBuilt("lumbridge_castle", 3, -7));
        assertFalse(b.markBuilt("lumbridge_castle", 3, -7));
        assertTrue(b.isBuilt("lumbridge_castle", 3, -7));
        assertFalse(b.isBuilt("lumbridge_castle", -7, 3));
        assertFalse(b.isBuilt("varrock", 3, -7));

        BuiltChunks legacy = new BuiltChunks();
        assertTrue(legacy.parseLegacyKey(BuiltChunks.legacyKey("al_kharid", -120, 45)));
        assertTrue(legacy.parseLegacyKey("s:mod:thing:-1,-2"));
        assertFalse(legacy.parseLegacyKey("LUMBRIDGE"));
        assertFalse(legacy.parseLegacyKey("place:gnome_stronghold"));
        assertFalse(legacy.parseLegacyKey("s:broken"));
        assertFalse(legacy.parseLegacyKey("s:x:1,y"));
        assertTrue(legacy.isBuilt("al_kharid", -120, 45));
        assertTrue(legacy.isBuilt("mod:thing", -1, -2));
        assertEquals(2, legacy.size());
    }

    @Test
    void builtChunksRoundTripsManySharesIncludingExtremeKeys() {
        BuiltChunks b = new BuiltChunks();
        Random r = new Random(5);
        List<int[]> added = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            int[] c = {r.nextInt(4000) - 2000, r.nextInt(4000) - 2000};
            String id = "s" + (i % 37);
            b.markBuilt(id, c[0], c[1]);
            added.add(new int[]{i % 37, c[0], c[1]});
        }
        b.markBuilt("edge", 0, Integer.MIN_VALUE); // key == Long.MIN_VALUE, the set's sentinel
        BuiltChunks copy = new BuiltChunks();
        for (String id : b.structures()) {
            copy.addAll(id, b.chunkKeys(id));
        }
        assertEquals(b.size(), copy.size());
        for (int[] a : added) {
            assertTrue(copy.isBuilt("s" + a[0], a[1], a[2]));
        }
        assertTrue(copy.isBuilt("edge", 0, Integer.MIN_VALUE));
        assertFalse(copy.isBuilt("edge", 0, 0));
    }

    // ------------------------------------------------------------------ Elevation

    /**
     * Reference model: the natural landscape plus the anchor, water and floor passes exactly as
     * originally written (testing each anchor's kind and source inline, over all anchors), so the
     * pre-sorted lists of the optimised model can't silently drop or reorder an anchor group.
     */
    private static double referenceHeight(double bx, double bz) {
        List<TerrainAnchors.Anchor> anchors = new ArrayList<>();
        for (TerrainAnchors.Anchor a : TerrainAnchors.get().anchors()) {
            if (a.distance(bx, bz) < Elevation.ANCHOR_REACH) {
                anchors.add(a);
            }
        }
        double ox = Elevation.osrsX(bx);
        double oy = Elevation.osrsY(bz);
        double h = Elevation.natural(ox, oy)[0];
        h = referencePass(h, bx, bz, anchors, true);
        h = referencePass(h, bx, bz, anchors, false);
        boolean inCore = false;
        for (TerrainAnchors.Anchor a : anchors) {
            if (a.kind() == TerrainAnchors.Kind.CORE && !a.source().contains(":") && a.distance(bx, bz) == 0) {
                inCore = true;
                break;
            }
        }
        if (!inCore) {
            h = Elevation.carveNatural(h, ox, oy);
        }
        for (TerrainAnchors.WaterBox w : TerrainAnchors.get().water()) {
            double dx = Math.max(0, Math.max(w.x1() - bx, bx - w.x2()));
            double dz = Math.max(0, Math.max(w.z1() - bz, bz - w.z2()));
            double d = Math.max(dx, dz);
            if (d <= 0) {
                h = Math.min(h, w.bedTop());
            } else if (d < 3) {
                h = Math.min(h, w.bedTop() + (h - w.bedTop()) * Elevation.smooth(d / 3));
            }
        }
        for (TerrainAnchors.Anchor a : anchors) {
            if (a.kind() != TerrainAnchors.Kind.FLOOR) {
                continue;
            }
            double need = a.target() - Math.max(0, a.distance(bx, bz) - 2);
            if (need > h) {
                h = need;
            }
        }
        return h;
    }

    private static double referencePass(double h, double bx, double bz, List<TerrainAnchors.Anchor> anchors, boolean places) {
        double upper = Double.MAX_VALUE;
        double lower = -Double.MAX_VALUE;
        double coreW = 0;
        double coreT = Elevation.GROUND;
        for (TerrainAnchors.Anchor a : anchors) {
            if (a.kind() == TerrainAnchors.Kind.FLOOR || a.source().contains(":") != places) {
                continue;
            }
            double d = a.distance(bx, bz);
            if (a.kind() == TerrainAnchors.Kind.CORE) {
                double w = 1 - Elevation.smooth(d / Elevation.CORE_BLEND);
                if (w > coreW) {
                    coreW = w;
                    coreT = a.target();
                }
            }
            double slack = TerrainAnchors.BAND + a.slope() * Math.max(0, d - a.radius());
            upper = Math.min(upper, a.target() + slack);
            lower = Math.max(lower, a.target() - slack);
        }
        if (upper < Double.MAX_VALUE) {
            h = Elevation.smin(h, upper, 4);
        }
        if (lower > -Double.MAX_VALUE) {
            h = Elevation.smax(h, lower, 4);
        }
        return h + (coreT - h) * coreW;
    }

    @Test
    void rawHeightMatchesTheReferenceModelAroundAnchors() {
        List<TerrainAnchors.Anchor> anchors = TerrainAnchors.get().anchors();
        Random r = new Random(42);
        int checked = 0;
        for (int i = 0; i < anchors.size(); i += Math.max(1, anchors.size() / 400)) {
            TerrainAnchors.Anchor a = anchors.get(i);
            for (int k = 0; k < 3; k++) {
                // inside, at the blend edge, and out on the clamped slope
                double bx = a.x1() + r.nextInt(a.x2() - a.x1() + 1) + (k == 0 ? 0 : (k == 1 ? 8 : 30) * (r.nextBoolean() ? 1 : -1));
                double bz = a.z1() + r.nextInt(a.z2() - a.z1() + 1) + (k == 0 ? 0 : (k == 1 ? -6 : 25));
                assertEquals(referenceHeight(bx, bz), Elevation.rawHeight(bx, bz), 1e-9, "anchor " + a.source() + " at " + bx + "," + bz);
                checked++;
            }
        }
        assertTrue(checked > 300);
    }

    @Test
    void cachedGridAgreesWithTheModelAtGridNodes() {
        // At the 4-block grid nodes the interpolated lookup is the model value itself (stored as float).
        Random r = new Random(7);
        int lx = GielinorMap.toBlockX(2400);
        int hx = GielinorMap.toBlockX(3800);
        int lz = GielinorMap.toBlockZ(3900);
        int hz = GielinorMap.toBlockZ(2700);
        for (int i = 0; i < 1500; i++) {
            int bx = (Math.min(lx, hx) + r.nextInt(Math.abs(hx - lx))) & ~3;
            int bz = (Math.min(lz, hz) + r.nextInt(Math.abs(hz - lz))) & ~3;
            assertEquals(Elevation.rawHeight(bx, bz), Elevation.height(bx, bz), 1e-3, "grid node " + bx + "," + bz);
        }
    }

    @Test
    void tileCacheIsConsistentAcrossClearsAndThreads() throws Exception {
        int x0 = GielinorMap.toBlockX(3222) - 300;
        int z0 = GielinorMap.toBlockZ(3218) - 300;
        double[] first = new double[600 * 60];
        for (int i = 0; i < first.length; i++) {
            first[i] = Elevation.height(x0 + i % 600, z0 + (i / 600) * 10);
        }
        Elevation.clearCache();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<double[]>> runs = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                final int offset = t * 977;
                runs.add(pool.submit(() -> {
                    double[] out = new double[first.length];
                    for (int n = 0; n < first.length; n++) {
                        int i = (n + offset) % first.length;
                        out[i] = Elevation.height(x0 + i % 600, z0 + (i / 600) * 10);
                    }
                    return out;
                }));
            }
            for (Future<double[]> f : runs) {
                assertArrayEquals(first, f.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void warmHeightLookupsAreCheap() {
        int x0 = GielinorMap.toBlockX(3222) - 512;
        int z0 = GielinorMap.toBlockZ(3218) - 512;
        double warm = 0;
        for (int x = 0; x < 1024; x++) {
            for (int z = 0; z < 1024; z++) {
                warm += Elevation.height(x0 + x, z0 + z);
            }
        }
        final double expected = warm;
        // ~13 ms per million lookups when measured; allow two orders of magnitude for slow machines.
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            for (int rep = 0; rep < 5; rep++) {
                double sum = 0;
                for (int x = 0; x < 1024; x++) {
                    for (int z = 0; z < 1024; z++) {
                        sum += Elevation.height(x0 + x, z0 + z);
                    }
                }
                assertEquals(expected, sum, 0);
            }
        });
    }
}
