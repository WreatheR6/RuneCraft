package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.ItemCatalog;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Herbiboar numbers from the OSRS Wiki (Module:Herbiboar). */
class HerbiboarTest {
    @Test
    void hunterExperience() {
        assertEquals(1950.0, Herbiboar.harvestXp(80), 1e-9);
        assertEquals(2370.0, Herbiboar.harvestXp(94), 1e-9);
        assertEquals(2385.0, Herbiboar.harvestXp(95), 1e-9);
        assertEquals(2461.0, Herbiboar.harvestXp(99), 1e-9);
        assertEquals(1950.0 + 128.0, Herbiboar.averageXp(80), 1e-9);
        assertEquals(50.0, Herbiboar.SEARCH_XP);
        assertEquals(80, Herbiboar.HUNTER_LEVEL);
        assertEquals(31, Herbiboar.HERBLORE_LEVEL);
        assertEquals(6500, Herbiboar.PET_CHANCE);
        assertEquals(Herbiboar.PET_CHANCE, Pets.rate("pet_herbi", "herbiboar"));
    }

    @Test
    void herbs() {
        for (int level : new int[]{31, 60, 99}) {
            double sum = 0;
            for (double c : Herbiboar.herbChances(level)) {
                assertTrue(c >= 0);
                sum += c;
            }
            assertEquals(1.0, sum, 1e-9);
        }
        // Ranarr is tried first: (1 + floor(-10 * 0 / 98 + 20 * 98 / 98 + 0.5)) / 256 at Herblore 99.
        assertEquals(21 / 256.0, Herbiboar.herbChances(99)[0], 1e-9);
        assertTrue(Herbiboar.herbChances(99)[0] > Herbiboar.herbChances(31)[0]);
        Random r = new Random(3);
        assertEquals(2, Herbiboar.harvest(70, false, r::nextDouble).size());
        assertEquals(3, Herbiboar.harvest(70, true, r::nextDouble).size());
        for (Herbiboar.HerbLine h : Herbiboar.HERB_ORDER) {
            assertTrue(ItemCatalog.exists(h.item()), h.item());
        }
        for (Object[] f : Herbiboar.FOSSILS) {
            assertTrue(ItemCatalog.exists((String) f[0]), (String) f[0]);
        }
        double searches = 0;
        for (int i = 0; i < 100_000; i++) {
            searches += Herbiboar.searches(r.nextDouble());
        }
        assertEquals(Herbiboar.AVERAGE_SEARCHES, searches / 100_000, 0.01);
    }
}
