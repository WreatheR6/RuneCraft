package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.osrs.world.Elevation;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The underground lairs (Yama's Domain, the Royal Titans' cave, the Maggot King's lair, Mokhaiotl's delve). */
class BossLairsTest {
    private static StructureDef structure(String id) {
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** The last op of a structure touching a block (what stands there once it is built), or null. */
    private static StructureDef.Op last(StructureDef s, int x, int y, int z) {
        StructureDef.Op found = null;
        for (StructureDef.Op op : s.ops()) {
            if (op.type() == StructureDef.Op.Type.FLATTEN) {
                continue;
            }
            if (x >= op.x1() && x <= op.x2() && y >= op.y1() && y <= op.y2() && z >= op.z1() && z <= op.z2()) {
                if (op.type() != StructureDef.Op.Type.WALLS || x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2()) {
                    found = op;
                }
            }
        }
        return found;
    }

    private static boolean air(StructureDef s, int x, int y, int z) {
        StructureDef.Op op = last(s, x, y, z);
        return op != null && op.type() == StructureDef.Op.Type.CLEAR;
    }

    @Test
    void lairBossesFightUnderground() {
        assertEquals(List.of("yama", "branda_the_fire_queen", "maggot_king", "doom_of_mokhaiotl"),
                BossLairs.ALL.stream().map(BossLairs.Lair::bossId).toList());
        for (BossLairs.Lair l : BossLairs.ALL) {
            BossDef b = ContentRegistry.boss(l.bossId());
            assertNotNull(b.lair, l.bossId());
            ArenaLayout.Arena a = ArenaLayout.of(l.bossId());
            assertTrue(a.underground(), l.bossId());
            assertEquals(l.x(), a.x());
            assertEquals(l.z(), a.z());
            assertEquals(l.floorY(), a.floorY());
            assertEquals(b.arenaRadius, a.radius());
        }
        // the surface arenas are unchanged in kind
        assertFalse(ArenaLayout.of("scurrius").underground());
        // Yama's Domain at its cache tiles (1503, 10080 in the dungeon plane); the Titans below the Asgarnian Ice Dungeon's floor
        assertEquals(1503, BossLairs.YAMA.tileX());
        assertEquals(10080 - 6400, BossLairs.YAMA.tileY());
        assertTrue(BossLairs.ROYAL_TITANS.floorY() + BossLairs.HEADROOM + 1 < 26, "below the ice dungeon (floor 26)");
    }

    @Test
    void eachLairHasItsArenaStoneLightAndExit() {
        for (BossLairs.Lair l : BossLairs.ALL) {
            BossDef b = ContentRegistry.boss(l.bossId());
            StructureDef s = structure(BossLairs.structureId(l));
            assertNotNull(s, l.bossId());
            int r = b.arenaRadius;
            int f = l.floorY();
            int[] stone = l.stoneBlock(r);
            assertEquals("runecraft:" + b.summonBlockId(), last(s, stone[0], stone[1], stone[2]).block(), l.bossId());
            assertTrue(Math.hypot(stone[0] - l.x(), stone[2] - l.z()) < r, "stone inside the arena");
            int[] exit = l.exitBlock(r);
            assertEquals("runecraft:" + BossLairs.EXIT, last(s, exit[0], exit[1], exit[2]).block());
            int[] in = l.arrival(r);
            assertTrue(air(s, in[0], in[1], in[2]) && air(s, in[0], in[1] + 1, in[2]), l.bossId() + " arrival spot");
            assertSame(l, BossLairs.at(in[0], in[1], in[2], id -> ContentRegistry.boss(id).arenaRadius));
            // a walk from the arrival spot through the southern opening to the centre, on floor all the way
            for (int z = in[2]; z >= l.z(); z--) {
                assertTrue(air(s, l.x(), f + 1, z) && air(s, l.x(), f + 2, z), l.bossId() + " path blocked at z " + z);
                StructureDef.Op floor = last(s, l.x(), f, z);
                assertTrue(floor != null && floor.type() == StructureDef.Op.Type.FILL, l.bossId() + " no floor at z " + z);
            }
            // the ring wall stands two high between the openings
            int d = (int) Math.round((r + 0.5) / Math.sqrt(2));
            assertEquals(BossLairs.arenaBlocks(b.arenaStyle)[1], last(s, l.x() + d, f + 1, l.z() + d).block());
            assertTrue(air(s, l.x() + d, f + 3, l.z() + d));
            assertTrue(s.ops().stream().anyMatch(op -> "minecraft:shroomlight".equals(op.block())), "lit");
            // sealed: a shell around the whole cave
            int h = l.half(r);
            assertEquals(l.cave(), last(s, l.x() - h - 1, f + 2, l.z()).block());
            assertEquals(l.cave(), last(s, l.x(), f + BossLairs.HEADROOM + 1, l.z() + 1).block());
        }
    }

    @Test
    void lairsDontCutIntoOtherStructures() {
        List<String> clashes = new ArrayList<>();
        for (BossLairs.Lair l : BossLairs.ALL) {
            StructureDef mine = structure(BossLairs.structureId(l));
            for (StructureDef other : ContentRegistry.structures()) {
                if (other == mine) {
                    continue;
                }
                for (StructureDef.Op a : mine.ops()) {
                    boolean aFlat = a.type() == StructureDef.Op.Type.FLATTEN;
                    for (StructureDef.Op o : other.ops()) {
                        boolean oFlat = o.type() == StructureDef.Op.Type.FLATTEN;
                        if (aFlat && oFlat) {
                            continue; // levelled ground beside levelled ground blends
                        }
                        int m = aFlat ? 0 : o.margin();
                        int ay1 = aFlat ? a.y1() - 12 : a.y1();
                        int oy1 = oFlat ? o.y1() - 12 : o.y1();
                        if (a.x2() >= o.x1() - m && a.x1() <= o.x2() + m && a.z2() >= o.z1() - m && a.z1() <= o.z2() + m
                                && a.y2() >= oy1 && ay1 <= o.y2()) {
                            clashes.add(mine.id() + " " + a + " x " + other.id() + " " + o);
                        }
                    }
                }
            }
        }
        assertTrue(clashes.isEmpty(), clashes.size() + " clashes, first " + (clashes.isEmpty() ? "" : clashes.get(0)));
    }

    @Test
    void waysInAndOut() {
        for (BossLairs.Lair l : BossLairs.ALL) {
            StructureDef s = structure(BossLairs.structureId(l));
            int[] to = l.exitTo();
            if (l.hasHut()) {
                int[] e = l.entranceBlock();
                assertEquals("runecraft:" + BossLairs.ENTRANCE, last(s, e[0], e[1], e[2]).block());
                assertSame(l, BossLairs.byEntrance(e[0], e[1], e[2]));
                assertFalse(Elevation.isWater(e[0], e[2]), l.bossId() + " hut in water");
                // the exit leads out in front of the hut, on its levelled yard, clear of the hut's walls
                assertEquals(BossLairs.SURFACE, to[1]);
                assertTrue(s.ops().stream().anyMatch(op -> op.type() == StructureDef.Op.Type.FLATTEN && to[0] >= op.x1()
                        && to[0] <= op.x2() && to[2] >= op.z1() && to[2] <= op.z2() && op.y1() == BossLairs.SURFACE), l.bossId());
                assertEquals(null, last(s, to[0], to[1], to[2]));
                // the hut's doorway is open
                assertTrue(air(s, e[0], e[1], e[2] + 2) && air(s, e[0], e[1] + 1, e[2] + 2), l.bossId() + " doorway");
            } else {
                // Mokhaiotl: back beside the entrance in the Ruins of Mokhaiotl, the Tonali Cavern's carved lobby
                StructureDef tonali = structure("dng_tonali_cavern");
                assertNotNull(tonali);
                assertTrue(air(tonali, to[0], to[1], to[2]) && air(tonali, to[0], to[1] + 1, to[2]), "exit into the ruins");
                assertTrue(tonali.ops().stream().anyMatch(op -> op.block() != null && op.block().endsWith("mokhaiotl_entrance")
                        && Math.abs(op.x1() - to[0]) <= 2 && Math.abs(op.z1() - to[2]) <= 3), "beside the ruins' entrance");
            }
        }
    }
}
