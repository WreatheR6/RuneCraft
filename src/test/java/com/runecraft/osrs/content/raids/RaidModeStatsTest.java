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
        // Verzik's Hard mode urnbombs hit ~20% harder (wiki "Theatre of Blood/Hard Mode"), applied by the room.
        assertEquals(1.0, RaidScaling.tobHardMaxHit("verzik_vitur_p2"));
        assertEquals(44, TobRules.verzikUrnMax(RaidMode.NORMAL));
        assertEquals(53, TobRules.verzikUrnMax(RaidMode.HARD));
        assertEquals(1.0, RaidScaling.tobHardMaxHit("verzik_vitur"));
        // Hard mode challenge times by team size (wiki).
        assertEquals(70 * 100L, RaidScaling.targetTicks(RaidType.TOB, RaidMode.HARD, 1));
        assertEquals(35 * 100L, RaidScaling.targetTicks(RaidType.TOB, RaidMode.HARD, 2));
        assertEquals(30 * 100L, RaidScaling.targetTicks(RaidType.TOB, RaidMode.HARD, 3));
        assertEquals(27 * 100L, RaidScaling.targetTicks(RaidType.TOB, RaidMode.HARD, 4));
        assertEquals(25 * 100L, RaidScaling.targetTicks(RaidType.TOB, RaidMode.HARD, 5));
        assertEquals(Long.MAX_VALUE, RaidScaling.targetTicks(RaidType.TOB, RaidMode.NORMAL, 5));
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
    void coxSoloCreaturesIgnoreParty() {
        assertEquals(30, RaidScaling.coxScaledHp("vespine_soldier", 30, 5, false));
        assertEquals(45, RaidScaling.coxScaledHp("vespine_soldier", 30, 5, true));
        assertEquals(149, RaidScaling.coxScaledDefence("vespine_soldier", 100, 5, true), "100 x 148/99");
    }
}
