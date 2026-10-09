package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceTest {
    @Test
    void matchesOsrsTable() {
        assertEquals(0, Experience.xpForLevel(1));
        assertEquals(83, Experience.xpForLevel(2));
        assertEquals(174, Experience.xpForLevel(3));
        assertEquals(1154, Experience.xpForLevel(10));
        assertEquals(101_333, Experience.xpForLevel(50));
        assertEquals(737_627, Experience.xpForLevel(70));
        assertEquals(1_210_421, Experience.xpForLevel(75));
        assertEquals(6_517_253, Experience.xpForLevel(92));
        assertEquals(13_034_431, Experience.xpForLevel(99));
        assertEquals(188_884_740, Experience.xpForLevel(126));
    }

    @Test
    void levelForXp() {
        assertEquals(1, Experience.levelForXp(82));
        assertEquals(2, Experience.levelForXp(83));
        assertEquals(98, Experience.levelForXp(13_034_430));
        assertEquals(99, Experience.levelForXp(13_034_431));
        assertEquals(99, Experience.levelForXp(200_000_000));
        assertEquals(126, Experience.virtualLevelForXp(200_000_000));
    }

    @Test
    void capsAt200m() {
        assertEquals(Experience.MAX_XP_TENTHS, Experience.addTenths(Experience.MAX_XP_TENTHS - 5, 1000));
        assertEquals(Experience.MAX_XP_TENTHS, Experience.addTenths(Experience.MAX_XP_TENTHS, Integer.MAX_VALUE));
    }

    @Test
    void fractionalXpAccumulates() {
        SkillSet s = new SkillSet();
        for (int i = 0; i < 3; i++) {
            s.addXp(Skill.WOODCUTTING, Experience.tenths(37.5));
        }
        assertEquals(1125, s.xpTenths(Skill.WOODCUTTING));
        assertEquals("112.5", Experience.format(s.xpTenths(Skill.WOODCUTTING)));
    }

    @Test
    void freshAccountIsLevel3With10Hitpoints() {
        SkillSet s = new SkillSet();
        assertEquals(10, s.level(Skill.HITPOINTS));
        assertEquals(1, s.level(Skill.ATTACK));
        assertEquals(3, s.combatLevel());
        assertEquals(23 + 10, s.totalLevel(), "23 skills at 1 + hitpoints at 10");
        assertTrue(Skill.count() == 24);
    }
}
