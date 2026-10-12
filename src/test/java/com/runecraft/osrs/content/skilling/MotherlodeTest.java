package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.extras.SkillingPets;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Motherlode Mine numbers from the OSRS Wiki (Motherlode Mine, Pay-dirt, Sack). */
class MotherlodeTest {
    @Test
    void levelNinetyNineMatchesTheWiki() {
        Map<String, Double> d = Motherlode.distribution(99);
        // The wiki row sums to 99.33%; coal takes the remainder here.
        assertEquals(0.246, d.get("coal"), 0.008);
        assertEquals(0.0313, d.get("golden_nugget"), 0.0005);
        assertEquals(0.2422, d.get("gold_ore"), 0.002);
        assertEquals(0.2693, d.get("mithril_ore"), 0.002);
        assertEquals(0.1818, d.get("adamantite_ore"), 0.002);
        assertEquals(0.0227, d.get("runite_ore"), 0.001);
    }

    @Test
    void oresUnlockAtTheirLevels() {
        for (int level = 30; level <= 99; level++) {
            Map<String, Double> d = Motherlode.distribution(level);
            double sum = 0;
            for (double v : d.values()) {
                assertTrue(v >= 0);
                sum += v;
            }
            assertEquals(1.0, sum, 1e-9, "level " + level);
        }
        assertEquals(0.0, Motherlode.distribution(39).get("gold_ore"), 1e-12);
        assertTrue(Motherlode.distribution(40).get("gold_ore") > 0);
        assertEquals(0.0, Motherlode.distribution(54).get("mithril_ore"), 1e-12);
        assertTrue(Motherlode.distribution(55).get("mithril_ore") > 0);
        assertEquals(0.0, Motherlode.distribution(69).get("adamantite_ore"), 1e-12);
        assertEquals(0.0, Motherlode.distribution(84).get("runite_ore"), 1e-12);
        assertTrue(Motherlode.distribution(85).get("runite_ore") > 0);
        assertTrue(Motherlode.distribution(99).get("runite_ore") > Motherlode.distribution(85).get("runite_ore"));
        assertEquals(1, MiningData.payDirtTable(30).size());
        assertEquals(5, MiningData.payDirtTable(99).size());
    }

    @Test
    void collectionExperience() {
        assertEquals(60.0, Motherlode.PAY_DIRT_XP);
        assertEquals(0.0, Motherlode.collectXp("coal"));
        assertEquals(15.0, Motherlode.collectXp("gold_ore"));
        assertEquals(30.0, Motherlode.collectXp("mithril_ore"));
        assertEquals(45.0, Motherlode.collectXp("adamantite_ore"));
        assertEquals(75.0, Motherlode.collectXp("runite_ore"));
        assertEquals(0.0, Motherlode.collectXp("golden_nugget"));
        // Wiki: average bonus XP per pay-dirt is 16.84 at 85 and 21.9 at 99.
        assertEquals(16.84, Motherlode.averageCollectXp(85), 0.5);
        assertEquals(21.9, Motherlode.averageCollectXp(99), 0.5);
        MiningData.MineRock vein = MiningData.rock("pay_dirt_vein");
        assertNotNull(vein);
        assertEquals(30, vein.level());
        assertEquals(60.0, vein.yields().get(0).xp());
    }

    @Test
    void sackAndUpperLevel() {
        assertEquals(108, Motherlode.sackCapacity(false));
        assertEquals(189, Motherlode.sackCapacity(true));
        assertEquals(108, MiningData.SACK_CAPACITY);
        assertEquals(200, Motherlode.SACK_UPGRADE_NUGGETS);
        assertEquals(8, Motherlode.deposit(28, 100, false));
        assertEquals(28, Motherlode.deposit(28, 100, true));
        assertEquals(0, Motherlode.deposit(28, 108, false));
        assertFalse(Motherlode.canUseUpperLevel(71, true));
        assertTrue(Motherlode.canUseUpperLevel(72, true));
        assertFalse(Motherlode.canUseUpperLevel(99, false));
        assertEquals(100, Motherlode.UPPER_LEVEL_NUGGETS);
    }

    @Test
    void washingMatchesTheDistribution() {
        Random r = new Random(4);
        Map<String, Integer> seen = new HashMap<>();
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            seen.merge(Motherlode.wash(99, r::nextDouble), 1, Integer::sum);
        }
        Map<String, Double> d = Motherlode.distribution(99);
        for (Map.Entry<String, Double> e : d.entrySet()) {
            assertTrue(ItemCatalog.exists(e.getKey()), e.getKey());
            assertEquals(e.getValue(), seen.getOrDefault(e.getKey(), 0) / (double) n, 0.005, e.getKey());
        }
        SkillingPets.Roll pet = SkillingPets.forItem("pay_dirt");
        assertNotNull(pet);
        assertEquals(247_200, pet.base());
    }
}
