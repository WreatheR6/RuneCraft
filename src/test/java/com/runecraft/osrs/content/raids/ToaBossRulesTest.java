package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Tombs of Amascut's boss rules (OSRS wiki boss pages). */
class ToaBossRulesTest {
    @Test
    void akkha() {
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) {
            List<Integer> order = ToaBossRules.memoryBlastOrder(ToaBossRules.memoryBlastRounds(4), r);
            assertEquals(6, order.size());
            for (int k = 1; k < order.size(); k++) {
                int step = Math.floorMod(order.get(k) - order.get(k - 1), 4);
                assertTrue(step == 1 || step == 3, "each lit quadrant is next to the previous one: " + order);
            }
        }
        assertEquals(4, ToaBossRules.memoryBlastRounds(0));
        assertEquals(3, ToaBossRules.akkhaSwapHits(1));
        assertEquals(5, ToaBossRules.akkhaSwapHits(3));
        assertEquals(25, ToaBossRules.unstableOrbDamage(5.0, 4), "unstable orbs are capped at 25");
        assertEquals(12, ToaBossRules.unstableOrbDamage(1.0, 0));
        assertEquals(List.of("fire", "shadow", "lightning", "ice"), List.of(ToaBossRules.QUADRANTS));
    }

    @Test
    void babaAndZebak() {
        assertEquals(25, ToaBossRules.babaBoulderHp(0));
        assertEquals(27, ToaBossRules.babaBoulderHp(2));
        assertEquals(31, ToaBossRules.babaBoulderHp(4));
        assertEquals(10, ToaBossRules.debrisHp(1));
        assertEquals(30, ToaBossRules.debrisHp(3));
        assertEquals(10, ToaBossRules.BOULDER_SETS);
        assertEquals(5, ToaBossRules.BOULDERS_PER_SET);
        assertTrue(ToaBossRules.babaRockfallEvery(4) < ToaBossRules.babaRockfallEvery(0));
        assertEquals(7, ToaBossRules.zebakAttackSpeed(false, 0));
        assertEquals(4, ToaBossRules.zebakAttackSpeed(true, 0));
        assertEquals(5, ToaBossRules.zebakAttackSpeed(false, 4));
        assertEquals(38, ToaBossRules.ZEBAK_MELEE_MAX);
        assertEquals(3, ToaBossRules.ZEBAK_ROCK_HP / ToaBossRules.ZEBAK_ROAR_ON_ROCK, "a rock shelters from one whole roar");
    }

    @Test
    void wardens() {
        assertEquals(7, ToaBossRules.wardenSlamEvery(Set.of()));
        assertEquals(6, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED)));
        assertEquals(4, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2)));
        assertEquals(3, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2, Invocation.INSANITY)));
        assertEquals(1, ToaBossRules.siphonHp(1));
        assertEquals(1, ToaBossRules.siphonHp(3));
        assertEquals(2, ToaBossRules.siphonHp(5));
        assertEquals(3, ToaBossRules.siphonHp(7));
        assertEquals(4, ToaBossRules.siphonHp(8));
        assertTrue(ToaBossRules.siphonTicks(1, true) < ToaBossRules.siphonTicks(1, false));
        assertEquals(List.of(80, 60, 40, 20), List.of(ToaBossRules.SIPHON_THRESHOLDS[0], ToaBossRules.SIPHON_THRESHOLDS[1],
                ToaBossRules.SIPHON_THRESHOLDS[2], ToaBossRules.SIPHON_THRESHOLDS[3]));
        assertEquals(5, ToaBossRules.WARDEN_ENRAGE_AT);
        assertEquals(20, ToaBossRules.WARDEN_ENRAGE_HEAL);
        assertNotNull(Invocation.byName("Insanity"));
        assertNotEquals(ToaBossRules.wardenSlamEvery(Set.of()), ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.INSANITY)));
    }
}
