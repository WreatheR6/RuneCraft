package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Op;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DungeonsContentTest {
    private static final DungeonsContent MODULE = (DungeonsContent) Modules.ALL.stream()
            .filter(m -> m instanceof DungeonsContent).findFirst().orElseThrow();

    /** Structures of every other module. */
    private static List<StructureDef> others() {
        Set<String> mine = new HashSet<>();
        for (StructureDef s : MODULE.structures()) {
            mine.add(s.id());
        }
        List<StructureDef> out = new ArrayList<>();
        for (StructureDef s : ContentRegistry.structures()) {
            if (!mine.contains(s.id())) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean intersects3d(Op a, Op b) {
        int ma = a.margin();
        int mb = b.margin();
        return a.x1() - ma <= b.x2() + mb && a.x2() + ma >= b.x1() - mb && a.z1() - ma <= b.z2() + mb && a.z2() + ma >= b.z1() - mb
                && a.y1() <= b.y2() && a.y2() >= b.y1();
    }

    private static boolean intersectsXz(Dungeons.Entrance e, Op b) {
        int mb = b.margin();
        return e.x1() <= b.x2() + mb && e.x2() >= b.x1() - mb && e.z1() <= b.z2() + mb && e.z2() >= b.z1() - mb;
    }

    private static Dungeons.Entrance entranceOf(Dungeons.Dungeon d, Op op) {
        for (Dungeons.Entrance e : d.entrances()) {
            if (e.contains(op)) {
                return e;
            }
        }
        return null;
    }

    private static boolean creatureExists(String id) {
        return id.startsWith("minecraft:") || ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null;
    }

    @Test
    void moduleIsRegisteredAndIdsAreUnique() {
        assertEquals("dungeons", MODULE.id());
        assertTrue(Modules.ALL.get(Modules.ALL.size() - 1) instanceof DungeonsContent);
        Set<String> structureIds = new HashSet<>();
        for (StructureDef s : ContentRegistry.structures()) {
            assertTrue(structureIds.add(s.id()), "duplicate structure id " + s.id());
        }
        for (StructureDef s : MODULE.structures()) {
            assertTrue(s.id().startsWith("dng_"), s.id());
            assertTrue(s.replacesLandmark() == null, s.id() + " must not replace a place");
        }
        Set<String> blockIds = new HashSet<>();
        for (ContentModule m : Modules.ALL) {
            for (BlockDef b : m.blocks()) {
                assertTrue(blockIds.add(b.id()), "duplicate block id " + b.id());
            }
        }
        for (BlockDef b : MODULE.blocks()) {
            assertTrue(b.id().startsWith("dng_"), b.id());
        }
        assertEquals(27, Dungeons.all().size());
    }

    @Test
    void opsStayBelowTheSurfaceExceptEntrances() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            assertTrue(!d.entrances().isEmpty(), d.id() + " has no entrance");
            for (Op op : d.structure().ops()) {
                assertTrue(op.y1() >= -48, d.id() + " digs too deep: " + op);
                if (op.y2() > Dungeons.DEEP_TOP) {
                    assertNotNull(entranceOf(d, op), d.id() + " touches y > " + Dungeons.DEEP_TOP + " outside its entrances: " + op);
                }
            }
        }
    }

    @Test
    void surfaceEntrancesAreOnOpenGround() {
        List<StructureDef> others = others();
        // Plazas, boss arenas and landmark settlements built at runtime.
        List<int[]> keepOut = new ArrayList<>(); // x, z, radius
        Map<String, Integer> arenas = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            PlaceDef p = ContentRegistry.place(b.place);
            Landmark l = Landmark.byName(b.place);
            int cx = p != null ? p.blockX() : l != null ? l.blockX() : Integer.MIN_VALUE;
            int cz = p != null ? p.blockZ() : l != null ? l.blockZ() : 0;
            if (cx == Integer.MIN_VALUE) {
                continue;
            }
            int i = arenas.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            int ax = cx + (i % 2 == 0 ? 1 : -1) * (14 + r);
            int az = cz + (i / 2) * (2 * r + 6);
            keepOut.add(new int[]{ax, az, r + 3});
        }
        Set<String> replaced = new HashSet<>();
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.replacesLandmark() != null) {
                replaced.add(s.replacesLandmark());
            }
        }
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.style() != PlaceDef.Style.NONE && !replaced.contains(p.id())) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2);
                keepOut.add(new int[]{p.blockX(), p.blockZ(), r + 3});
            } else {
                keepOut.add(new int[]{p.blockX(), p.blockZ(), 6});
            }
        }
        for (Landmark l : Landmark.values()) {
            if (!replaced.contains(l.name())) {
                keepOut.add(new int[]{l.blockX(), l.blockZ(), l.isAltar() ? 12 : 24});
            }
        }
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Dungeons.Entrance e : d.entrances()) {
                if (!e.surface()) {
                    continue;
                }
                for (StructureDef s : others) {
                    if (e.x2() < s.minX() || e.x1() > s.maxX() || e.z2() < s.minZ() || e.z1() > s.maxZ()) {
                        continue;
                    }
                    for (Op op : s.ops()) {
                        if (op.y2() >= 56 && intersectsXz(e, op)) {
                            fail(d.id() + " entrance " + e + " hits " + s.id() + " " + op);
                        }
                    }
                }
                for (int[] k : keepOut) {
                    int dx = Math.max(0, Math.max(e.x1() - k[0], k[0] - e.x2()));
                    int dz = Math.max(0, Math.max(e.z1() - k[1], k[1] - e.z2()));
                    assertTrue(dx * dx + dz * dz > k[2] * k[2],
                            d.id() + " entrance " + e + " is within " + k[2] + " of a plaza/arena at " + k[0] + "," + k[1]);
                }
            }
        }
    }

    @Test
    void linksOnlyCutTheFloorOfOtherEntrancePits() {
        List<StructureDef> others = others();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Op op : d.structure().ops()) {
                Dungeons.Entrance e = entranceOf(d, op);
                if (e == null || e.surface()) {
                    continue;
                }
                assertTrue(e.yTop() < 60, d.id() + " link reaches the surface " + e);
                for (StructureDef s : others) {
                    for (Op o : s.ops()) {
                        if (o.type() != Op.Type.FLATTEN && o.y2() >= StructureDef.GROUND && intersects3d(op, o)) {
                            fail(d.id() + " link op " + op + " cuts " + s.id() + " building " + o);
                        }
                    }
                }
            }
        }
    }

    @Test
    void dungeonsDoNotCutOtherStructures() {
        List<StructureDef> others = others();
        for (Dungeons.Dungeon d : Dungeons.all()) {
            StructureDef mine = d.structure();
            for (StructureDef s : others) {
                if (mine.maxX() < s.minX() || mine.minX() > s.maxX() || mine.maxZ() < s.minZ() || mine.minZ() > s.maxZ()) {
                    continue;
                }
                for (Op op : mine.ops()) {
                    Dungeons.Entrance e = entranceOf(d, op);
                    if (e != null) {
                        continue;
                    }
                    for (Op o : s.ops()) {
                        if (o.type() != Op.Type.FLATTEN && intersects3d(op, o)) {
                            fail(d.id() + " op " + op + " cuts " + s.id() + " " + o);
                        }
                    }
                }
            }
        }
    }

    @Test
    void dungeonsDoNotOverlapEachOther() {
        List<Dungeons.Dungeon> all = Dungeons.all();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                StructureDef a = all.get(i).structure();
                StructureDef b = all.get(j).structure();
                if (a.maxX() < b.minX() || a.minX() > b.maxX() || a.maxZ() < b.minZ() || a.minZ() > b.maxZ()) {
                    continue;
                }
                for (Op x : a.ops()) {
                    for (Op y : b.ops()) {
                        if (intersects3d(x, y)) {
                            fail(a.id() + " and " + b.id() + " overlap: " + x + " / " + y);
                        }
                    }
                }
            }
        }
    }

    @Test
    void campsAreInsideTheirDungeonsAndReferenceRealMonsters() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            assertTrue(!d.camps().isEmpty() || d.id().equals("dng_nothing"), d.id() + " has no monsters");
            for (Dungeons.Camp c : d.camps()) {
                assertTrue(c.feetY() <= Dungeons.DEEP_TOP, d.id() + " camp on the surface " + c);
                assertTrue(c.cap() > 0 && c.respawnTicks() > 0, c.toString());
                for (String id : c.creatures()) {
                    assertTrue(creatureExists(id), d.id() + " camp creature " + id);
                }
                // Every corner of the camp box and its centre are carved out at the camp's height.
                int[][] pts = {{c.x1(), c.z1()}, {c.x2(), c.z2()}, {c.x1(), c.z2()}, {c.x2(), c.z1()}, {c.centreX(), c.centreZ()}};
                for (int[] p : pts) {
                    boolean carved = false;
                    for (Op op : d.structure().ops()) {
                        if (op.type() == Op.Type.CLEAR && p[0] >= op.x1() && p[0] <= op.x2() && p[1] >= op.z1() && p[1] <= op.z2()
                                && c.feetY() >= op.y1() && c.feetY() + 1 <= op.y2()) {
                            carved = true;
                            break;
                        }
                    }
                    assertTrue(carved, d.id() + " camp point " + p[0] + "," + p[1] + " is not carved: " + c);
                }
            }
        }
        for (MonsterDef m : MODULE.monsters()) {
            assertTrue(m.underground, m.id + " should only appear in dungeons");
            assertTrue(m.spawns.isEmpty(), m.id + " should not spawn naturally");
        }
    }

    @Test
    void entranceLaddersReachTheDungeonFloor() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            for (Dungeons.Entrance e : d.entrances()) {
                Op ladder = null;
                for (Op op : d.structure().ops()) {
                    if (op.block() != null && op.block().startsWith("minecraft:ladder") && e.contains(op)) {
                        ladder = op;
                    }
                }
                assertNotNull(ladder, d.id() + " entrance without a ladder " + e);
                // The ladder's bottom block is inside the carved dungeon.
                boolean landed = false;
                for (Op op : d.structure().ops()) {
                    if (op.type() == Op.Type.CLEAR && op.y2() <= Dungeons.DEEP_TOP && ladder.x1() >= op.x1() && ladder.x1() <= op.x2()
                            && ladder.z1() >= op.z1() && ladder.z1() <= op.z2() && ladder.y1() >= op.y1() && ladder.y1() <= op.y2()) {
                        landed = true;
                    }
                }
                assertTrue(landed, d.id() + " ladder does not land in the dungeon: " + ladder);
            }
        }
    }

    @Test
    void abyssRiftsLeadToAltars() {
        List<Dungeons.Rift> rifts = Dungeons.rifts();
        assertEquals(13, rifts.size());
        for (Dungeons.Rift r : rifts) {
            Landmark l = Landmark.byName(r.altar());
            assertNotNull(l, r.altar());
            assertTrue(l.isAltar(), r.altar());
        }
    }

    @Test
    void scriptedBlocksUsedInStructuresExist() {
        Set<String> ids = new HashSet<>();
        for (BlockDef b : MODULE.blocks()) {
            ids.add(b.id());
        }
        for (StructureDef s : MODULE.structures()) {
            for (Op op : s.ops()) {
                String b = op.block();
                if (b.startsWith("runecraft:dng_")) {
                    assertTrue(ids.contains(b.substring("runecraft:".length())), s.id() + " uses unknown " + b);
                }
            }
        }
    }
}
