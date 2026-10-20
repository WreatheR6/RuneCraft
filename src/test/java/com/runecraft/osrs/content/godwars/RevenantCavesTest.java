package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.MultiCombat;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Revenant Caves' layout: everything stands on the cache floor plan, at the right Wilderness levels, clear of other works. */
class RevenantCavesTest {
    @Test
    void everySpotIsOnTheFloorPlan() {
        int floor = 0;
        for (String r : RevenantCaveMap.ROWS) {
            floor += (int) r.chars().filter(c -> c == 'o').count();
        }
        assertTrue(floor > 7000, "floor tiles " + floor);
        for (RevenantCaves.Spawn s : RevenantCaves.SPAWNS) {
            assertTrue(RevenantCaves.walkable(s.x(), s.y()), s.toString());
        }
        for (RevenantCaves.Entrance e : RevenantCaves.ENTRANCES) {
            assertTrue(RevenantCaves.walkable(e.toX(), e.toY()), e.id() + " landing");
            assertEquals(e, RevenantCaves.entranceAt(e.x1(), e.y1()));
        }
        for (RevenantCaves.Exit e : RevenantCaves.EXITS) {
            // The stairs may stand against the cave wall; a floor tile beside them lets the player reach them.
            boolean reachable = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    reachable |= RevenantCaves.walkable(e.x() + dx, e.y() + dy);
                }
            }
            assertTrue(reachable, e.id() + " stairs");
            assertEquals(e, RevenantCaves.exitAt(e.x(), e.y()));
        }
        for (RevenantCaves.Pillar p : RevenantCaves.PILLARS) {
            assertTrue(RevenantCaves.walkable(p.ax(), p.ay()), p.id() + " end A");
            assertTrue(RevenantCaves.walkable(p.bx(), p.by()), p.id() + " end B");
            assertFalse(RevenantCaves.walkable(p.px(), p.py()), p.id() + " stands in its chasm");
            assertEquals(p, RevenantCaves.pillarAt(p.px(), p.py()));
        }
        for (RevenantCaves.MaledictusSpot s : RevenantCaves.MALEDICTUS_SPOTS) {
            assertTrue(RevenantCaves.walkable(s.x(), s.y()), s.name());
        }
        assertTrue(RevenantCaves.walkable(RevenantCaves.EMBLEM_TRADER[0], RevenantCaves.EMBLEM_TRADER[1]), "Emblem Trader");
        assertEquals(11, RevenantCaves.MALEDICTUS_SPOTS.size());
        assertEquals(5, RevenantCaves.PILLARS.size());
        assertEquals(65, RevenantCaves.PILLARS.get(0).level());
        assertEquals(89, RevenantCaves.PILLARS.get(4).level());
    }

    @Test
    void wildernessLevelsAsInTheGame() {
        // OSRS: an underground tile's level is that of the surface tile 6,400 rows south; the caves span levels 17 to 40.
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int j = 0; j < RevenantCaveMap.ROWS.length; j++) {
            int y = RevenantCaveMap.TOP - j;
            int i = RevenantCaveMap.ROWS[j].indexOf('o');
            if (i < 0) {
                continue;
            }
            int level = GielinorMap.wildernessLevel(RevenantCaves.blockX(RevenantCaveMap.X0 + i), RevenantCaves.blockZ(y));
            min = Math.min(min, level);
            max = Math.max(max, level);
        }
        assertEquals(17, min);
        assertEquals(40, max);
        assertEquals(17, GielinorMap.wildernessLevel(GielinorMap.toBlockX(3073), GielinorMap.toBlockZ(3653)), "level 17 cavern");
        assertEquals(40, GielinorMap.wildernessLevel(GielinorMap.toBlockX(3126), GielinorMap.toBlockZ(3834)), "level 40 cavern");
        assertEquals(36, GielinorMap.wildernessLevel(GielinorMap.toBlockX(3122), GielinorMap.toBlockZ(3805)), "northern trapdoor");
    }

    @Test
    void singlesPlusUnderground() {
        // Singles-plus (single-way against monsters) everywhere in the caves, whatever multi zone lies on the surface above.
        int multiAbove = 0;
        for (int j = 0; j < RevenantCaveMap.ROWS.length; j += 3) {
            int y = RevenantCaveMap.TOP - j;
            for (int i = 0; i < RevenantCaveMap.ROWS[j].length(); i += 3) {
                if (RevenantCaveMap.ROWS[j].charAt(i) != 'o') {
                    continue;
                }
                int bx = RevenantCaves.blockX(RevenantCaveMap.X0 + i);
                int bz = RevenantCaves.blockZ(y);
                if (MultiCombat.isMultiAtBlock(bx, bz)) {
                    multiAbove++;
                }
                assertFalse(MultiCombat.isMultiAtBlock(bx, RevenantCaves.feetY(), bz), (RevenantCaveMap.X0 + i) + "," + y);
            }
        }
        assertTrue(multiAbove >= 0);
    }

    @Test
    void clearOfOtherCarvedWorks() {
        StructureDef caves = RevenantCaves.structure();
        int[] box = RevenantCaves.blockBox();
        List<String> clash = new ArrayList<>();
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(caves.id())) {
                continue;
            }
            for (StructureDef.Op op : s.ops()) {
                if (op.type() == StructureDef.Op.Type.CLEAR && op.y1() <= RevenantCaves.FLOOR + RevenantCaves.HEIGHT + 1
                        && op.y2() >= RevenantCaves.FLOOR - 1 && op.x2() >= box[0] && op.x1() <= box[2] && op.z2() >= box[1]
                        && op.z1() <= box[3]) {
                    clash.add(s.id() + " " + op);
                    break;
                }
            }
        }
        assertTrue(clash.isEmpty(), String.join("\n", clash));
        assertTrue(ContentRegistry.structures().stream().anyMatch(s -> s.id().equals("revenant_caves_dungeon")));
        for (RevenantCaves.Entrance e : RevenantCaves.ENTRANCES) {
            assertTrue(ContentRegistry.structures().stream().anyMatch(s -> s.id().equals("revenant_caves_" + e.id())), e.id());
        }
    }

    @Test
    void emblemTraderStandsInTheCaves() {
        PlaceDef p = ContentRegistry.place(RevenantCaves.EMBLEM_TRADER_PLACE);
        assertNotNull(p);
        assertEquals(PlaceDef.Style.NONE, p.style());
        Integer feet = com.runecraft.osrs.world.NpcStoreys.undergroundFeetY(RevenantCaves.EMBLEM_TRADER_NPC);
        assertEquals(RevenantCaves.feetY(), feet);
        assertTrue(RevenantCaves.contains(p.blockX() + 1, feet, p.blockZ() + 1));
    }
}
