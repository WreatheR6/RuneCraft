package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 28: boss tables patched from their real OSRS Wiki pages (not the parsed dump rows, which for Obor and Bryophyta are
 * the hill and moss giant tables).
 */
class Round28BossTablesTest {
    private static Drop line(List<Drop> drops, String item) {
        return drops.stream().filter(d -> d.item().equals(item)).findFirst().orElse(null);
    }

    private static List<Drop> boss(String id) {
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.id.equals(id)) {
                return b.drops;
            }
        }
        throw new AssertionError("no boss " + id);
    }

    @Test
    void oborAndBryophytaUseTheirChestTables() {
        List<Drop> obor = Boss.OBOR.drops();
        assertEquals(118, line(obor, "hill_giant_club").rarity());
        assertEquals(16, line(obor, "giant_key").rarity());
        assertNotNull(line(obor, "rune_2h_sword"));
        assertEquals(50, line(obor, "law_rune").min());
        assertFalse(obor.stream().anyMatch(d -> d.item().equals("iron_full_helm")), "hill giant table leaked into Obor");
        List<Drop> bryo = Boss.BRYOPHYTA.drops();
        assertEquals(118, line(bryo, "bryophytas_essence").rarity());
        assertEquals(16, line(bryo, "mossy_key").rarity());
        assertNotNull(line(bryo, "rune_sq_shield"));
        assertFalse(bryo.stream().anyMatch(d -> d.item().equals("black_sq_shield")), "moss giant table leaked into Bryophyta");
    }

    @Test
    void zalcanoContributionTable() {
        List<Drop> z = boss("zalcano");
        assertEquals(8000, line(z, "uncut_onyx").rarity(), "uncut onyx is a 1/8000 tertiary, not 1/23");
        assertTrue(z.stream().noneMatch(d -> d.item().startsWith("clue_scroll")), "Zalcano drops no clue scrolls");
        assertEquals(750, line(z, "zalcano_shard").rarity());
        assertEquals(36, line(z, "soul_rune").rarity());
        assertEquals(784, line(z, "pure_essence").min());
    }

    @Test
    void lunarChestUniquesAreOneIn224PerPiece() {
        for (String moon : new String[]{"blue_moon", "eclipse_moon", "blood_moon"}) {
            List<Drop> d = boss(moon);
            long uniques = d.stream().filter(x -> x.rarity() == 224).count();
            assertEquals(4, uniques, moon + " has four 1/224 pieces");
            assertEquals(6, line(d, "atlatl_dart").rarity(), moon);
            assertNotNull(line(d, "blessed_bone_shards"), moon);
            assertTrue(d.stream().noneMatch(x -> x.item().equals("coins") || x.item().startsWith("clue_scroll")), moon);
        }
    }
}
