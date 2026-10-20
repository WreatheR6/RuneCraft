package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievements;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A summoned boss's lair: the leash never hands a player free hits, attacks from outside do nothing, and it resets alone. */
class BossLairTest {
    private static double sq(double d) {
        return d * d;
    }

    /**
     * General Graardor's pad lair (leash 16): a player shooting him from 22-30 blocks used to see him walk out, be put back at
     * 16 blocks and forget them, so he never attacked. Now he chases a target that is still in the lair.
     */
    @Test
    void aTargetInTheLairIsChasedPastTheLeash() {
        int leash = 16;
        for (double player : new double[]{17, 22, 30, 32}) {
            assertFalse(BossLair.returnsHome(sq(17), leash, true, sq(player)), "player at " + player);
            assertFalse(BossLair.targetLeft(sq(player), leash), "player at " + player);
            assertTrue(BossLair.inReach(sq(player), leash), "player at " + player);
        }
        // the player ran off: he goes back and waits there
        assertTrue(BossLair.returnsHome(sq(17), leash, true, sq(33)));
        assertTrue(BossLair.targetLeft(sq(33), leash));
        // nobody to chase: past the leash he walks back; within it he stays where he is
        assertTrue(BossLair.returnsHome(sq(17), leash, false, 0));
        assertFalse(BossLair.returnsHome(sq(15), leash, false, 0));
        assertFalse(BossLair.returnsHome(sq(15), leash, true, sq(60)), "inside the leash he is never moved");
    }

    /** Shooting him from outside the lair (where he no longer chases) does nothing, so there is no safe spot past it. */
    @Test
    void attacksFromOutsideTheLairDoNothing() {
        assertFalse(BossLair.inReach(sq(33), 16));
        assertFalse(BossLair.inReach(sq(60), 24));
        // an arena's leash (radius + 8, at most 26) is inside the presence range: the whole arena is in reach
        assertTrue(BossLair.inReach(sq(26), 26));
        assertEquals(BossLair.PRESENCE_RANGE, BossLair.reach(26));
        assertEquals(40, BossLair.reach(40));
        // whatever the leash, a boss chasing a target it can still hit is never put back
        for (int leash : new int[]{8, 16, 24, 26}) {
            for (double d = 0; d <= 60; d += 0.5) {
                if (BossLair.inReach(sq(d), leash)) {
                    assertFalse(BossLair.returnsHome(sq(leash + 10), leash, true, sq(d)), leash + "/" + d);
                }
            }
        }
    }

    /** A minute (100 game ticks) with nobody within 32 blocks resets it; anyone near restarts the count. */
    @Test
    void itResetsAfterAMinuteAlone() {
        int lonely = 0;
        for (int i = 0; i < 99; i++) {
            lonely = BossLair.lonely(lonely, false);
        }
        assertFalse(BossLair.resets(lonely));
        assertEquals(0, BossLair.lonely(lonely, true));
        assertTrue(BossLair.resets(BossLair.lonely(lonely, false)));
        assertEquals(100, BossLair.RESET_TICKS);
        assertEquals(32, BossLair.PRESENCE_RANGE);
    }

    /** Minions are tagged with their lair, so a reset clears exactly that lair's (also after a restart). */
    @Test
    void minionTagsNameTheirLair() {
        assertEquals("rc_lair_123", BossLair.minionTag(123));
        assertNotEquals(BossLair.minionTag(123), BossLair.minionTag(124));
        assertTrue(BossLair.minionTag(-5).startsWith(BossLair.MINION_TAG));
    }

    /**
     * Dark Memories (CA 502, "less than 6 seconds in the pre-enrage shadow realm"): realm time is counted while the player
     * kills lost souls (only hittable in the realm) and the task fails from 10 ticks. Ignoring the siphon costs nothing.
     */
    @Test
    void darkMemoriesCountsTimeSpentOnLostSouls() {
        String rule = "count<10:" + CombatAchievements.COUNT_SHADOW_REALM_TICKS;
        Metrics ignored = new Metrics();
        assertTrue(CombatAchievementRules.meets(rule, ignored), "the siphon ignored, as the wiki suggests");

        // a quick dip: souls killed on ticks 4 and 7 of the siphon -> 7 ticks in the realm
        Metrics quick = new Metrics();
        int counted = 0;
        for (int killTick : new int[]{4, 4, 7}) {
            int add = BossMechanics.whispererRealmTicks(killTick, counted);
            quick.count(CombatAchievements.COUNT_SHADOW_REALM_TICKS, add);
            counted += add;
        }
        assertEquals(7, counted);
        assertTrue(CombatAchievementRules.meets(rule, quick));

        // every soul hunted down until tick 14: too long
        Metrics slow = new Metrics();
        counted = 0;
        for (int killTick : new int[]{5, 9, 14}) {
            int add = BossMechanics.whispererRealmTicks(killTick, counted);
            slow.count(CombatAchievements.COUNT_SHADOW_REALM_TICKS, add);
            counted += add;
        }
        assertEquals(14, counted);
        assertFalse(CombatAchievementRules.meets(rule, slow));
        assertEquals(0, BossMechanics.whispererRealmTicks(3, 7), "never negative");
    }
}
