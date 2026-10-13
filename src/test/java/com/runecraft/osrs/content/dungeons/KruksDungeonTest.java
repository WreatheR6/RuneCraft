package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.world.Elevation;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kruk's Dungeon under Ape Atoll against the OSRS cache spawns and the wiki. */
class KruksDungeonTest {
    private static Dungeons.Dungeon kruk() {
        return Dungeons.all().stream().filter(d -> d.id().equals("dng_kruks_dungeon")).findFirst().orElseThrow();
    }

    @Test
    void entranceIsTheHoleWestOfMarim() {
        Dungeons.Entrance e = kruk().entrances().get(0);
        assertTrue(e.surface());
        int x = GielinorMap.toOsrsX((e.x1() + e.x2()) / 2);
        int y = GielinorMap.toOsrsY((e.z1() + e.z2()) / 2);
        assertTrue(Math.abs(x - 2715) <= 1 && Math.abs(y - 2789) <= 1, x + "," + y);
    }

    /** Each camp keeps the cache's count of each monkey, and the dungeon has the cache's lower-floor halls. */
    @Test
    void campsHoldTheCacheSpawnCounts() {
        Map<String, Integer> caps = new HashMap<>();
        for (Dungeons.Camp c : kruk().camps()) {
            assertEquals(1, c.creatures().size(), c.toString());
            caps.merge(c.creatures().get(0), c.cap(), Integer::sum);
            assertEquals(KruksDungeon.RESPAWN, c.respawnTicks());
            assertTrue(c.feetY() == KruksDungeon.LOWER + 1 || c.feetY() == KruksDungeon.UPPER + 1, c.toString());
        }
        // Cache (mejrs NPCList_OSRS.json): halls 11+12+6+5 monkeys and 7+4+4+6 archers, the gallery 6 and 4.
        assertEquals(40, caps.get("maniacal_monkey"));
        assertEquals(25, caps.get("maniacal_monkey_archer"));
    }

    @Test
    void monkeysHaveTheirWikiStats() {
        MonsterDef m = ContentRegistry.monster("maniacal_monkey");
        assertNotNull(m);
        assertEquals(140, m.combatLevel);
        assertEquals(65, m.hitpoints);
        assertTrue(m.aggressive && m.underground && m.spawns.isEmpty());
        MonsterDef a = ContentRegistry.monster("maniacal_monkey_archer");
        assertNotNull(a);
        assertEquals(132, a.combatLevel);
        assertEquals(200, a.ranged);
    }

    /** The whole dungeon lies under Ape Atoll's land, so the sea floor around the island is left alone. */
    @Test
    void dungeonLiesUnderTheIsland() {
        for (StructureDef.Op op : kruk().structure().ops()) {
            for (int[] p : new int[][]{{op.x1(), op.z1()}, {op.x2(), op.z2()}, {op.x1(), op.z2()}, {op.x2(), op.z1()}}) {
                assertTrue(Elevation.coastDistance(p[0], p[1]) > 2, "under the sea at " + GielinorMap.toOsrsX(p[0]) + ","
                        + GielinorMap.toOsrsY(p[1]) + ": " + op);
            }
        }
    }
}
