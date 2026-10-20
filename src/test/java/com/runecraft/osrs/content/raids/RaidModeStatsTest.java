package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ToB Hard mode and CoX Challenge Mode stat changes (OSRS wiki DPS calculator data and scaling code). */
class RaidModeStatsTest {
    @Test
    void tobHardModeHitpoints() {
        assertEquals(2400, RaidScaling.tobScaledHp("pestilent_bloat", 2000, 5, RaidMode.HARD), "Bloat +20%");
        assertEquals(1800, RaidScaling.tobScaledHp("pestilent_bloat", 2000, 3, RaidMode.HARD), "scaled 6/8 for a trio");
        assertEquals(2000, RaidScaling.tobScaledHp("pestilent_bloat", 2000, 5, RaidMode.NORMAL));
        assertEquals(6000, RaidScaling.tobScaledHp("xarpus", 5000, 5, RaidMode.HARD));
        assertEquals(5000, RaidScaling.tobScaledHp("xarpus", 5000, 5, RaidMode.NORMAL));
        assertEquals(3500, RaidScaling.tobScaledHp("the_maiden_of_sugadinti", 3500, 5, RaidMode.HARD), "Maiden unchanged");
        assertEquals(4000, RaidScaling.tobScaledHp("sotetseg", 4000, 5, RaidMode.HARD), "Sotetseg unchanged");
    }

    @Test
    void tobHardModeLevelsAndHits() {
        assertArrayEquals(new int[]{350, -1, -1, -1, -1}, RaidScaling.tobHardLevels("sotetseg"));
        assertArrayEquals(new int[]{-1, -1, 200, -1, -1}, RaidScaling.tobHardLevels("xarpus"));
        assertNull(RaidScaling.tobHardLevels("the_maiden_of_sugadinti"));
        assertEquals(105, Math.round(70 * RaidScaling.tobHardMaxHit("nylocas_vasilias")));
        assertEquals(44, Math.round(47 * RaidScaling.tobHardMaxHit("verzik_vitur_p2")));
        assertEquals(1.0, RaidScaling.tobHardMaxHit("verzik_vitur"));
    }

    @Test
    void coxDefenceScaling() {
        // Solo normal raids keep the base Defence.
        assertEquals(205, RaidScaling.coxScaledDefence("tekton", 205, 1, false));
        // 5 players: +1% per isqrt(4) = 2, +0.7% x 4 = 2 -> 104%.
        assertEquals(213, RaidScaling.coxScaledDefence("tekton", 205, 5, false));
        // Challenge Mode: Tekton +20% below 4 players, +35% from 4; others +50%.
        assertEquals(246, RaidScaling.coxScaledDefence("tekton", 205, 1, true));
        assertEquals(287, RaidScaling.coxScaledDefence("tekton", 205, 5, true));
        assertEquals(244, RaidScaling.coxScaledDefence("vanguard_melee", 160, 3, true));
        assertEquals(175, RaidScaling.coxScaledDefence("vasa_crystal", 175, 1, true), "crystals unchanged");
        assertEquals(1, RaidScaling.coxScaledDefence("great_olm", 1, 5, true), "levels of 1 stay 1");
        assertEquals(50, RaidScaling.coxScaledDefence("cox_lizardman_spawn", 20, 1, false), "at least 50");
        assertTrue(RaidScaling.coxMagicIsDefensive("tekton"));
        assertFalse(RaidScaling.coxMagicIsDefensive("great_olm"));
    }

    @Test
    void coxChallengeModeTargetTimesAndDeaths() {
        // OSRS wiki "Chambers of Xeric/Challenge Mode": the target time by team size.
        int[][] want = {{1, 70}, {2, 65}, {3, 50}, {4, 45}, {5, 42}, {10, 42}, {11, 45}, {15, 45}, {16, 60}, {23, 60}, {24, 80},
                {100, 80}};
        for (int[] w : want) {
            assertEquals(w[1], RaidScaling.coxCmTargetMinutes(w[0]), "party " + w[0]);
            assertEquals(w[1] * 100L, RaidScaling.targetTicks(RaidType.COX, RaidMode.CHALLENGE, w[0]));
        }
        assertEquals(Long.MAX_VALUE, RaidScaling.targetTicks(RaidType.COX, RaidMode.NORMAL, 1));
        assertEquals(5_000, RaidScaling.COX_CM_TARGET_BONUS);
        // A death costs 40% of your points; with under 5% of the team's points it costs the team 5% instead.
        assertEquals(600, RaidScaling.coxDeathPenalty(1000));
        assertFalse(RaidScaling.coxDeathHitsTeam(1000, 10_000));
        assertTrue(RaidScaling.coxDeathHitsTeam(400, 10_000));
        assertTrue(RaidScaling.coxDeathHitsTeam(0, 0));
        assertEquals(9_500, RaidScaling.coxTeamDeathPenalty(10_000));
    }

    @Test
    void coxSoloCreaturesIgnoreParty() {
        assertEquals(30, RaidScaling.coxScaledHp("vespine_soldier", 30, 5, false));
        assertEquals(45, RaidScaling.coxScaledHp("vespine_soldier", 30, 5, true));
        assertEquals(149, RaidScaling.coxScaledDefence("vespine_soldier", 100, 5, true), "100 x 148/99");
    }
}
