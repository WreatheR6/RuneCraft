package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Yama's per-player rewards (OSRS Wiki "Yama" Drops and Contribution). */
class YamaLootTest {
    @Test
    void tablesAddUpAndUseRealItems() {
        int total = YamaLoot.SUPPLY_WEIGHT;
        for (YamaLoot.Line l : YamaLoot.MAIN) {
            total += l.weight();
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        assertEquals(YamaLoot.MAIN_TABLE_WEIGHT, total);
        for (YamaLoot.Line l : YamaLoot.JUNK) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        for (String s : new String[]{"pineapple_pizza", "wild_pie", "prayer_potion(3)", "super_restore_mix", "super_combat_potion(1)",
                "zamorak_mix", "soulflame_horn", "dossier", "forgotten_lockbox", "oathplate_shards"}) {
            assertTrue(ItemCatalog.exists(Drop.baseOf(s)), s);
        }
    }

    @Test
    void underFifteenPercentRollsJunkOnly() {
        assertTrue(YamaLoot.junk(YamaLoot.contribution(374, 2500)));
        assertFalse(YamaLoot.junk(YamaLoot.contribution(375, 2500)));
        Random r = new Random(3);
        for (int i = 0; i < 500; i++) {
            List<YamaLoot.Loot> loot = YamaLoot.roll(0.1, null, r);
            assertEquals(1, loot.size());
            assertTrue(YamaLoot.JUNK.stream().anyMatch(l -> l.item().equals(loot.get(0).item())), loot.toString());
        }
    }

    @Test
    void quantitiesScaleWithContribution() {
        // "a player who deals 1,000 damage to Yama, or 40% of his health, is eligible to receive 16,000 fire runes"
        assertEquals(16000, YamaLoot.scaled(40000, YamaLoot.contribution(1000, 2500)));
        assertEquals(6, YamaLoot.scaled(12, 0.5));
        assertEquals(1, YamaLoot.scaled(6, 0.01));
    }

    @Test
    void solosRollTheWikiRates() {
        Random r = new Random(11);
        int n = 300_000;
        int uniques = 0;
        int horns = 0;
        int dossiers = 0;
        int lockboxes = 0;
        int shards = 0;
        int fire = 0;
        for (int i = 0; i < n; i++) {
            for (YamaLoot.Loot l : YamaLoot.roll(1.0, null, r)) {
                switch (l.item()) {
                    case "soulflame_horn" -> {
                        uniques++;
                        horns++;
                    }
                    case "oathplate_helm", "oathplate_chest", "oathplate_legs" -> uniques++;
                    case "dossier" -> dossiers++;
                    case "forgotten_lockbox" -> lockboxes++;
                    case "oathplate_shards" -> {
                        shards++;
                        assertEquals(12, l.count());
                    }
                    case "fire_rune" -> {
                        fire++;
                        assertEquals(40000, l.count());
                    }
                    default -> {
                    }
                }
            }
        }
        // effective solo rates on the wiki: unique 1/120 (horn 2/600), dossier 1/12.1, lockbox 1/33, shards 1/17.07
        assertEquals(n / 120.0, uniques, n / 120.0 * 0.12);
        assertEquals(n / 300.0, horns, n / 300.0 * 0.15);
        assertEquals(n / 12.1, dossiers, n / 12.1 * 0.05);
        assertEquals(n / 33.0, lockboxes, n / 33.0 * 0.08);
        assertEquals(n / 17.07, shards, n / 17.07 * 0.06);
        assertEquals(n / 95.11, fire, n / 95.11 * 0.08); // 1/78 of the standard table, reached ~82% of the time
    }

    @Test
    void contractsSkipTheStandardTableAndHalfContributionHalvesUniques() {
        Random r = new Random(5);
        int n = 200_000;
        int standard = 0;
        int uniques = 0;
        for (int i = 0; i < n; i++) {
            for (YamaLoot.Loot l : YamaLoot.roll(1.0, YamaContract.CATALYST_ACQUISITION, r)) {
                if (YamaLoot.MAIN.stream().anyMatch(m -> m.item().equals(l.item())) || l.item().equals("pineapple_pizza")
                        || l.item().equals("wild_pie")) {
                    standard++;
                }
            }
            for (YamaLoot.Loot l : YamaLoot.roll(0.5, null, r)) {
                if (YamaLoot.OATHPLATE.contains(l.item()) || l.item().equals("soulflame_horn")) {
                    uniques++;
                }
            }
        }
        assertEquals(0, standard);
        assertEquals(n / 240.0, uniques, n / 240.0 * 0.15);
    }

    @Test
    void contractThresholdsAndDuos() {
        assertArrayEquals(new double[]{75, 50}, YamaLoot.thresholds(YamaContract.BLOODIED_BLOWS));
        assertEquals(66.67, YamaLoot.thresholds(null)[0], 0.01);
        assertEquals(33.33, YamaLoot.thresholds(null)[1], 0.01);
        assertTrue(YamaLoot.duoAllowed(null));
        assertTrue(YamaLoot.duoAllowed(YamaContract.SHARD_ACQUISITION));
        assertFalse(YamaLoot.duoAllowed(YamaContract.OATHPLATE_ACQUISITION));
        assertFalse(YamaLoot.duoAllowed(YamaContract.HARMONY_ACQUISITION));
        assertFalse(YamaLoot.duoAllowed(YamaContract.DIVINE_SEVERANCE));
        assertEquals(3000, YamaContract.BLOODIED_BLOWS.hitpoints);
        assertEquals(2875, YamaContract.GLYPHIC_ATTENUATION.hitpoints);
    }
}
