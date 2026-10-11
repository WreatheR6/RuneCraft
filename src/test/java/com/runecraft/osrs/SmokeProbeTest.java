package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The station positions the dedicated-server smoke test checks (tools/smoke_server.sh). */
class SmokeProbeTest {
    @Test
    void everyCheckedTownHasVerifiableStations() {
        for (String town : List.of("LUMBRIDGE", "VARROCK", "FALADOR")) {
            List<SmokeProbe.Probe> probes = SmokeProbe.probes(town, 2);
            assertFalse(probes.isEmpty(), "no verifiable station block in " + town);
            for (SmokeProbe.Probe p : probes) {
                assertEquals(p.block(), SmokeProbe.finalBlock(p.x(), p.y(), p.z()), p.line());
                assertFalse(SmokeProbe.nearOtherBuilders(p.x(), p.z()), p.line());
                assertTrue(SmokeProbe.STATION_BLOCKS.contains(p.block()), p.line());
            }
        }
    }

    @Test
    void blockIds() {
        assertEquals("minecraft:oak_stairs", SmokeProbe.base("oak_stairs[facing=north]"));
        assertEquals("runecraft:bank_booth", SmokeProbe.base("runecraft:bank_booth"));
        assertTrue(SmokeProbe.multipart("minecraft:oak_door[half=upper]"));
        assertFalse(SmokeProbe.multipart("minecraft:oak_trapdoor[half=top]"));
        assertFalse(SmokeProbe.multipart("minecraft:stone_brick_stairs[half=top]"));
        // Far out at sea nothing is built: the block there is natural terrain (unknown).
        assertNull(SmokeProbe.finalBlock(100_000, 64, 100_000));
    }
}
