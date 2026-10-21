package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Leviathan (OSRS Wiki "The Leviathan/Strategies"). */
class LeviathanRulesTest {
    /** 3-tick M/R, 2-tick M/R, 2-tick all three, 1-tick M/R, 1-tick all three, then less time to react. */
    @Test
    void volleysSpeedUp() {
        int[] spacing = new int[LeviathanRules.STAGES];
        boolean[] melee = new boolean[LeviathanRules.STAGES];
        for (int s = 0; s < LeviathanRules.STAGES; s++) {
            spacing[s] = LeviathanRules.osrsSpacing(s, false, false);
            melee[s] = LeviathanRules.melee(s, false);
        }
        assertEquals("[3, 2, 2, 1, 1, 1, 1]", java.util.Arrays.toString(spacing));
        assertEquals("[false, false, true, false, true, true, true]", java.util.Arrays.toString(melee));
        assertEquals(2, LeviathanRules.osrsReact(5, false));
        assertEquals(1, LeviathanRules.osrsReact(6, false));
        assertEquals(6, LeviathanRules.next(6), "the last speed stays");
        // enraged: every 2 ticks (awakened 1), magic and ranged only
        assertEquals(2, LeviathanRules.osrsSpacing(0, true, false));
        assertEquals(1, LeviathanRules.osrsSpacing(0, true, true));
        assertFalse(LeviathanRules.melee(4, true));
    }

    /**
     * Playability (round 41): as played, orbs are at least 2 ticks apart and fly at least 3 (a prayer switch per orb a human
     * can make); where OSRS is already slower its numbers stay.
     */
    @Test
    void playedVolleysHaveRealTimeFloors() {
        int[] spacing = new int[LeviathanRules.STAGES];
        int[] react = new int[LeviathanRules.STAGES];
        for (int s = 0; s < LeviathanRules.STAGES; s++) {
            spacing[s] = LeviathanRules.spacing(s, false, false);
            react[s] = LeviathanRules.react(s, false);
            assertEquals(Math.max(2, LeviathanRules.osrsSpacing(s, false, false)), spacing[s]);
            assertEquals(Math.max(3, LeviathanRules.osrsReact(s, false)), react[s]);
        }
        assertEquals("[3, 2, 2, 2, 2, 2, 2]", java.util.Arrays.toString(spacing));
        assertEquals("[3, 3, 3, 3, 3, 3, 3]", java.util.Arrays.toString(react));
        for (boolean awakened : new boolean[]{false, true}) {
            assertTrue(LeviathanRules.spacing(0, true, awakened) >= LeviathanRules.MIN_SPACING);
            assertTrue(LeviathanRules.react(0, true) >= LeviathanRules.MIN_REACT);
        }
    }

    @Test
    void stunsResetToALaterSpeedEachTime() {
        assertEquals(1, LeviathanRules.afterStun(1), "the first stun goes back to the second speed");
        assertEquals(2, LeviathanRules.afterStun(2));
        assertEquals(6, LeviathanRules.afterStun(9));
        assertTrue(LeviathanRules.quarterDealt(225, 900));
        assertFalse(LeviathanRules.quarterDealt(224, 900));
    }

    @Test
    void woundAndPathfinderHits() {
        assertEquals(10, LeviathanRules.stunnedHit(40, false, false, 50), "capped at 10 off the wound");
        assertEquals(33, LeviathanRules.stunnedHit(5, true, false, 50), "ranged on the wound: at least 65% of the max");
        assertEquals(40, LeviathanRules.stunnedHit(20, true, true, 50), "magic on the wound: double");
        assertEquals(10, LeviathanRules.enragedHit(5, true, false, 50), "inside the pathfinder: at least 20% of the max");
        assertEquals(10, LeviathanRules.enragedHit(5, true, true, 50), "magic inside: double");
        assertEquals(12, LeviathanRules.enragedHit(25, false, false, 50), "outside: less damage");
    }

    @Test
    void attacksAfterTheWiki() {
        BossDef b = ContentRegistry.boss("the_leviathan");
        java.util.Map<String, Integer> max = new java.util.HashMap<>();
        for (AttackDef a : b.attacks) {
            max.put(a.name(), a.maxHit());
        }
        assertEquals(24, max.get("boulder"));
        assertEquals(32, max.get("abyssal_orb"));
        assertEquals(50, max.get("shadow_strike"));
        assertEquals(25, max.get("falling_debris"), "the wiki's rocks: 25 (was 45)");
        assertEquals(59, max.get("smoke_blast"));
        assertEquals(50, max.get("bite"));
        assertEquals(70, AwakenedBosses.variant(b).maxHit(attack(b, "smoke_blast")));
    }

    private static AttackDef attack(BossDef b, String name) {
        return b.attacks.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow();
    }
}
