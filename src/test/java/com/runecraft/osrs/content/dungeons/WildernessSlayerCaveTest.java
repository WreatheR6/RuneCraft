package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.MultiCombat;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Wilderness Slayer Cave (wiki): a multicombat dungeon under level 17-32 Wilderness. */
class WildernessSlayerCaveTest {
    private static int bx(int cacheX) {
        return StructureDef.bx(cacheX + CacheDungeons.WSC_DX);
    }

    private static int bz(int cacheY) {
        return StructureDef.bz(cacheY + CacheDungeons.WSC_DY);
    }

    @Test
    void cavernsAreMulticombat() {
        int floor = 20;
        // greater demons, hellhounds, ankou, black dragons
        int[][] rooms = {{3428, 10150}, {3442, 10087}, {3360, 10076}, {3361, 10157}};
        for (int[] r : rooms) {
            assertTrue(MultiCombat.isMultiAtBlock(bx(r[0]), floor + 1, bz(r[1])), r[0] + "," + r[1]);
            int level = GielinorMap.wildernessLevel(bx(r[0]), bz(r[1]));
            assertTrue(level >= 17 && level <= 32, r[0] + "," + r[1] + " under level " + level);
        }
        // the multicombat boxes are the caverns' own air (not the rock above them)
        assertTrue(MultiCombat.inUndergroundMulti(bx(3360), floor + 1, bz(10076)), "ankou cavern");
        assertFalse(MultiCombat.inUndergroundMulti(bx(3360), floor + 30, bz(10076)), "above the ankou cavern");
    }
}
