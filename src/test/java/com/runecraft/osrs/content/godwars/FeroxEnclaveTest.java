package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Ferox Enclave (wiki): a safe zone in level 13-16 Wilderness, its barriers and its pools of refreshment. */
class FeroxEnclaveTest {
    @Test
    void safeZoneInsideTheWallsOnly() {
        assertTrue(FeroxEnclave.contains(3139, 3632));
        assertTrue(FeroxEnclave.contains(3128, 3638), "the chapel's pool (wiki pin)");
        assertFalse(FeroxEnclave.contains(3139, 3612), "south of the walls");
        assertFalse(FeroxEnclave.contains(3160, 3620), "the level 13 obelisk east of it");
        // still the Wilderness's levels 13-16 around and inside it (the safe zone is a rule on top)
        assertEquals(13, GielinorMap.wildernessLevel(GielinorMap.toBlockX(3139), GielinorMap.toBlockZ(3617)));
        assertEquals(16, GielinorMap.wildernessLevel(GielinorMap.toBlockX(3139), GielinorMap.toBlockZ(3647)));
        assertTrue(GielinorMap.inFeroxEnclave(GielinorMap.toBlockX(3139), GielinorMap.toBlockZ(3632)));
        assertFalse(FeroxEnclave.pvpAllowed(true, false));
        assertFalse(FeroxEnclave.pvpAllowed(false, true));
        assertTrue(FeroxEnclave.pvpAllowed(false, false));
        assertFalse(FeroxEnclave.barrierLetsIn(true), "a tele-blocked player can't pass the barriers");
        assertTrue(FeroxEnclave.barrierLetsIn(false));
    }

    @Test
    void poolsRestoreEverythingButSpecialAttack() {
        FeroxEnclave.PoolEffect fx = FeroxEnclave.POOL;
        assertTrue(fx.hitpoints() && fx.prayer() && fx.runEnergy() && fx.resetStats() && fx.curesPoison() && fx.curesVenom());
        assertTrue(fx.disablesPrayers(), "drinking turns active prayers off");
        assertFalse(fx.specialAttack(), "the enclave's pools don't restore special attack energy");
        assertEquals("You feel reinvigorated after drinking from the pool.", FeroxEnclave.POOL_MESSAGE);
        BlockDef pool = null;
        for (BlockDef b : ContentRegistry.blocks()) {
            if (b.id().equals(FeroxEnclave.BLOCK_POOL)) {
                pool = b;
            }
        }
        assertNotNull(pool, "the pool block is registered");
        // The enclave's chapel holds the pools (and no house pool that would restore special attack).
        StructureDef ferox = null;
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals("ferox_enclave")) {
                ferox = s;
            }
        }
        assertNotNull(ferox);
        int pools = 0;
        for (StructureDef.Op op : ferox.ops()) {
            if (("runecraft:" + FeroxEnclave.BLOCK_POOL).equals(op.block())) {
                pools++;
                int x = GielinorMap.toOsrsX(op.x1());
                int y = GielinorMap.toOsrsY(op.z1());
                assertTrue(FeroxEnclave.contains(x, y), "pool at " + x + "," + y + " is inside the enclave");
            }
            assertFalse("runecraft:poh_ornate_rejuvenation_pool".equals(op.block()), "a house pool in the enclave");
        }
        assertEquals(2, pools, "the chapel's two pools of refreshment");
    }
}
