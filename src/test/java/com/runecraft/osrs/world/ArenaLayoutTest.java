package com.runecraft.osrs.world;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLayoutTest {
    @Test
    void arenasStayClearOfStructuresAndEachOther() {
        Map<String, ArenaLayout.Arena> all = ArenaLayout.all();
        List<int[]> ops = new ArrayList<>();
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                if (op.y2() >= 56 || op.type() == StructureDef.Op.Type.FLATTEN) {
                    int m = op.margin();
                    ops.add(new int[]{op.x1() - m, op.z1() - m, op.x2() + m, op.z2() + m});
                }
            }
        }
        List<ArenaLayout.Arena> placed = new ArrayList<>(all.values());
        int stuck = 0;
        for (int i = 0; i < placed.size(); i++) {
            ArenaLayout.Arena a = placed.get(i);
            int h = a.radius() + 3;
            boolean hit = false;
            for (int[] b : ops) {
                if (b[2] >= a.x() - h && b[0] <= a.x() + h && b[3] >= a.z() - h && b[1] <= a.z() + h) {
                    hit = true;
                    break;
                }
            }
            for (int j = 0; j < i && !hit; j++) {
                ArenaLayout.Arena o = placed.get(j);
                int oh = o.radius() + 3;
                hit = Math.abs(a.x() - o.x()) <= h + oh && Math.abs(a.z() - o.z()) <= h + oh;
            }
            if (hit) {
                stuck++;
            }
        }
        assertTrue(stuck == 0, stuck + " arenas still overlap structures or other arenas");
    }

    @Test
    void everyPlacedBossHasAnArena() {
        for (BossDef b : ContentRegistry.bosses()) {
            if (!b.raidOnly && b.place != null && ArenaLayout.all().containsKey(b.id)) {
                assertNotNull(ArenaLayout.of(b.id));
            }
        }
        assertFalse(ArenaLayout.all().isEmpty());
    }
}
