package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.NpcStoreys;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slayer masters and chests stand on their OSRS Wiki map tiles: Konar quo Maten and the brimstone chest on Mount
 * Karuulm (1309, 3785), Vannaka in the Edgeville Dungeon (3147, 9914), Larran's big chest on the ship west of the
 * Pirates' Hideout (3018, 3955).
 */
class SlayerSpotsTest {
    private static int[] block(String npcId) {
        NpcDef npc = ContentRegistry.npc(npcId);
        assertNotNull(npc, npcId);
        PlaceDef place = ContentRegistry.place(npc.place());
        assertNotNull(place, npc.place());
        return new int[]{place.blockX() + npc.dx(), place.blockZ() + npc.dz()};
    }

    @Test
    void konarOnTheSummitWithTheBrimstoneChest() {
        int[] konar = block("konar");
        assertEquals(StructureDef.bx(1309), konar[0]);
        assertEquals(StructureDef.bz(3785), konar[1]);
        PlaceDef camp = ContentRegistry.place("mount_karuulm");
        assertTrue(camp.blocks().contains("brimstone_chest"));
        // the chest's camp is within a few tiles of her (it fills a slot of the camp around the chunk centre)
        assertTrue(Math.abs(camp.blockX() - konar[0]) <= 8 && Math.abs(camp.blockZ() - konar[1]) <= 8);
    }

    @Test
    void vannakaInTheEdgevilleDungeon() {
        int[] v = block("vannaka");
        assertEquals(StructureDef.bx(SlayerContent.VANNAKA_TILE[0]), v[0]);
        assertEquals(StructureDef.bz(SlayerContent.VANNAKA_TILE[1] - 6400), v[1]);
        assertNotNull(NpcStoreys.undergroundFeetY("vannaka"), "he stands underground");
    }

    @Test
    void larransChestOnTheShipNotInTheHideout() {
        PlaceDef ship = ContentRegistry.place(SlayerContent.LARRAN_SHIP);
        assertNotNull(ship);
        assertEquals(3018, ship.osrsX());
        assertEquals(3955, ship.osrsY());
        assertTrue(ContentRegistry.place("pirates_hideout").blocks().isEmpty());
    }
}
