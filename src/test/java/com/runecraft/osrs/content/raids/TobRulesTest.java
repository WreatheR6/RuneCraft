package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.ItemCatalog;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Theatre of Blood room rules (round 33) against the OSRS wiki. */
class TobRulesTest {
    @Test
    void supplyChestStockAndPrices() {
        assertEquals(7, TobRules.SUPPLIES.size());
        assertEquals(1, TobRules.supply("stamina_potion").cost());
        assertEquals(1, TobRules.supply("stamina_potion").perChest());
        assertEquals(2, TobRules.supply("prayer_potion").cost());
        assertEquals(3, TobRules.supply("saradomin_brew").cost());
        assertEquals(3, TobRules.supply("super_restore").cost());
        assertEquals(1, TobRules.supply("shark").cost());
        assertEquals(2, TobRules.supply("sea_turtle").cost());
        assertEquals(2, TobRules.supply("manta_ray").cost());
        assertNull(TobRules.supply("dragon_claws"));
        for (TobRules.Supply s : TobRules.SUPPLIES) {
            assertTrue(ItemCatalog.exists(s.item()), "the supply chest sells unknown " + s.item());
        }
    }

    @Test
    void chestPointsFollowPerformance() {
        Random r = new Random(5);
        for (int i = 0; i < 500; i++) {
            int above = TobRules.chestPoints(TobRules.Performance.ABOVE_AVERAGE, r);
            int avg = TobRules.chestPoints(TobRules.Performance.AVERAGE, r);
            int below = TobRules.chestPoints(TobRules.Performance.BELOW_AVERAGE, r);
            assertTrue(above >= 10 && above <= 13, "above average " + above);
            assertTrue(avg >= 8 && avg <= 11, "average " + avg);
            assertTrue(below >= 6 && below <= 9, "below average " + below);
        }
        assertEquals(TobRules.Performance.ABOVE_AVERAGE, TobRules.performance(0, true));
        assertEquals(TobRules.Performance.AVERAGE, TobRules.performance(0, false));
        assertEquals(TobRules.Performance.BELOW_AVERAGE, TobRules.performance(1, true));
        assertTrue(TobRules.onionOnly(2));
        assertFalse(TobRules.onionOnly(1));
    }

    @Test
    void maidenNumbers() {
        assertEquals(36, TobRules.maidenBlackstormMax(0));
        assertEquals(40, TobRules.maidenBlackstormMax(1));
        assertEquals(71, TobRules.maidenBlackstormMax(10));
        assertEquals(10, TobRules.maidenSplat(0));
        assertEquals(16, TobRules.maidenSplat(3));
        assertEquals(8, TobRules.maidenDrain(39));
        assertEquals(0.1, TobRules.bloodSpawnChance(false));
        assertEquals(0.2, TobRules.bloodSpawnChance(true));
    }

    @Test
    void bloatRunsBetweenFortyAndSixtyPercent() {
        assertFalse(TobRules.bloatRuns(0.9, 0));
        assertTrue(TobRules.bloatRuns(0.5, 0));
        assertTrue(TobRules.bloatRuns(0.4, 0));
        assertFalse(TobRules.bloatRuns(0.3, 0));
        assertTrue(TobRules.bloatRuns(0.3, 1));
        assertEquals(38, RaidScaling.TOB_BLOAT_FIRST_WALK);
        assertEquals(30, TobRules.BLOAT_STOMP_TICK);
    }

    @Test
    void sotetsegAndXarpus() {
        assertEquals(15, TobRules.mazeWrongTile(0));
        assertEquals(21, TobRules.mazeWrongTile(99));
        assertEquals(12, TobRules.xarpusExhumeds(1));
        assertEquals(12, TobRules.xarpusExhumeds(3));
        assertEquals(15, TobRules.xarpusExhumeds(4));
        assertEquals(15, TobRules.xarpusExhumeds(5));
        assertEquals(8, TobRules.xarpusPoison(8, 0));
        assertTrue(TobRules.xarpusPoison(8, 10) > 8);
        assertEquals(75, TobRules.xarpusRetaliation(75, 0));
        Random r = new Random(9);
        Set<Integer> seen = new HashSet<>();
        int q = 0;
        for (int i = 0; i < 400; i++) {
            int next = TobRules.xarpusNextQuadrant(q, r);
            assertNotEquals(q, next, "Xarpus stared at the same quadrant twice");
            assertTrue(next >= 0 && next < 4);
            seen.add(next);
            q = next;
        }
        assertEquals(4, seen.size());
    }

    @Test
    void verzikNumbers() {
        assertEquals(10, TobRules.verzikP1Cap(50, true, false));
        assertEquals(3, TobRules.verzikP1Cap(50, false, false));
        assertEquals(2, TobRules.verzikP1Cap(2, false, false));
        assertEquals(80, TobRules.verzikP1Cap(80, false, true));
        assertEquals(74, TobRules.greenBall(99));
        assertEquals(137, TobRules.VERZIK_P1_BLAST_MAX);
        assertEquals(4, TobRules.P3_ROTATION.size());
        assertEquals(TobRules.P3Special.NYLOCAS, TobRules.P3_ROTATION.get(0));
        assertTrue(TobRules.VERZIK_ENRAGE_AT < TobRules.VERZIK_P25_AT);
    }
}
