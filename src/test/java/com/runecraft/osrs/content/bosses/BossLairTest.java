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
    /** A melee boss stranded by terrain (no path that reaches its target, out of reach) steps up after a few game ticks. */
    @Test
    void terrainSafespotsDontLast() {
        double none = Double.POSITIVE_INFINITY; // no path at all
        assertTrue(BossLair.strandedBy(6, 0, none, 0));
        assertFalse(BossLair.strandedBy(6, 0, 0.8, 0), "a target it can walk into reach of is chased, not jumped to");
        assertFalse(BossLair.strandedBy(1.2, 0, none, 0), "already in melee reach (a 1.5-block gap)");
        // Branda summoned 3 blocks from her target (a 1.6-block gap) with her own walls in the way: her best path ends 1.76 away
        assertTrue(BossLair.strandedBy(1.6, 0, 1.76, 0), "just past the round-41 reach with no way in: no step's grace");
        assertTrue(BossLair.strandedBy(4, 3, 0.5, 3), "a path to the foot of a pillar ends out of reach (the height)");
        int t = 0;
        for (int i = 1; i < BossLair.SAFESPOT_TICKS; i++) {
            t = BossLair.stranded(t, true);
            assertFalse(BossLair.breaksSafespot(t));
        }
        t = BossLair.stranded(t, true);
        assertTrue(BossLair.breaksSafespot(t));
        assertEquals(0, BossLair.stranded(t, false), "a reachable tick resets the count");
        assertEquals(24, BossLair.QUEST_LEASH);
    }

    @Test
    void ringCirclesTheCentre() {
        double[][] ring = BossLair.ring(10, -4, 2.5, 0);
        assertEquals(BossLair.RING_SPOTS, ring.length);
        for (double[] spot : ring) {
            assertEquals(2.5, Math.hypot(spot[0] - 10, spot[1] + 4), 1e-9);
        }
        assertEquals(12.5, ring[0][0], 1e-9); // the first spot lies toward the start angle
        assertEquals(-4, ring[0][1], 1e-9);
    }

    /** A boss stranded beside a ledge steps up next to its target when there is floor beside the target. */
    @Test
    void strandedBossStepsUpBesideItsTarget() {
        double[][] beside = BossLair.ring(0, 0, 2, 0);
        BossLair.SafespotBreak b = BossLair.breakSafespot(beside, spot -> spot[1] > 1.5, // only the south spot has floor
                BossLair.ring(8, 0, 2, Math.PI), spot -> true, 8, 0);
        assertFalse(b.knockDown());
        assertEquals(0, b.x(), 1e-9);
        assertEquals(2, b.z(), 1e-9);
    }

    /**
     * On a 1x1 pillar (or down a one-wide shaft) no spot beside the target fits the boss: the target is knocked down beside
     * the boss instead, and when nothing there fits either, onto the boss's own spot. The safespot never survives.
     */
    @Test
    void noSpotBesideThePerchKnocksTheTargetDown() {
        double[][] beside = BossLair.ring(0, 0, 2, 0);
        double[][] besideBoss = BossLair.ring(8, 0, 1.5, Math.PI);
        BossLair.SafespotBreak b = BossLair.breakSafespot(beside, spot -> false, besideBoss, spot -> spot[1] < -1, 8, 0);
        assertTrue(b.knockDown());
        assertTrue(Math.hypot(b.x(), b.z()) > 6, "beside the boss, not beside the pillar");
        assertEquals(1.5, Math.hypot(b.x() - 8, b.z()), 1e-9);
        assertTrue(b.z() < -1);

        BossLair.SafespotBreak cramped = BossLair.breakSafespot(beside, spot -> false, besideBoss, spot -> false, 8, 0);
        assertTrue(cramped.knockDown());
        assertEquals(8, cramped.x(), 1e-9);
        assertEquals(0, cramped.z(), 1e-9);
    }

    @Test
    void minionTagsNameTheirLair() {
        assertEquals("rc_lair_123", BossLair.minionTag(123));
        assertNotEquals(BossLair.minionTag(123), BossLair.minionTag(124));
        assertTrue(BossLair.minionTag(-5).startsWith(BossLair.MINION_TAG));
    }

    /**
     * Dark Memories (CA 502, "less than 6 seconds in the pre-enrage shadow realm"): every pre-enrage tick a player stands in the
     * Shadow Realm counts, the task fails from 10. Ignoring the realm (or only peeking) passes; the enrage's realm doesn't count.
     */
    @Test
    void darkMemoriesCountsPreEnrageRealmTicks() {
        String rule = "count<" + WhispererRules.DARK_MEMORIES_TICKS + ":" + CombatAchievements.COUNT_SHADOW_REALM_TICKS;
        assertTrue(CombatAchievementRules.meets(rule, new Metrics()), "never entered the realm");

        WhispererRules.Sanity s = new WhispererRules.Sanity();
        Metrics quick = new Metrics();
        for (int t = 0; t < 7; t++) {
            boolean in = t < 3 || t >= 5; // two peeks: 3 + 2 ticks
            int before = s.realmTicks();
            s.tick(in, false, false);
            quick.count(CombatAchievements.COUNT_SHADOW_REALM_TICKS, s.realmTicks() - before);
        }
        for (int t = 0; t < 40; t++) {
            int before = s.realmTicks();
            s.tick(true, true, false); // the enrage: in the realm, but not counted
            quick.count(CombatAchievements.COUNT_SHADOW_REALM_TICKS, s.realmTicks() - before);
        }
        assertEquals(5, s.realmTicks());
        assertTrue(CombatAchievementRules.meets(rule, quick));

        Metrics slow = new Metrics();
        slow.count(CombatAchievements.COUNT_SHADOW_REALM_TICKS, WhispererRules.DARK_MEMORIES_TICKS);
        assertFalse(CombatAchievementRules.meets(rule, slow), "a whole soul siphon spent in the realm");
    }
}
