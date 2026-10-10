package com.runecraft.osrs.ui;

import com.runecraft.osrs.Skill;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillGuideTest {
    @Test
    void everySkillHasAGuide() {
        for (Skill s : Skill.values()) {
            assertFalse(SkillGuide.entries(s).isEmpty(), s + " has no guide entries");
        }
    }

    @Test
    void sortedDedupedAndInRange() {
        for (Skill s : Skill.values()) {
            List<SkillGuide.Entry> list = SkillGuide.entries(s);
            Set<String> seen = new HashSet<>();
            int last = 0;
            for (SkillGuide.Entry e : list) {
                assertTrue(e.level() >= 1 && e.level() <= 99, s + ": " + e);
                assertTrue(e.level() >= last, s + " not sorted at " + e);
                assertTrue(seen.add(e.text().toLowerCase(Locale.ROOT)), s + " duplicate " + e);
                assertFalse(e.text().endsWith("(flatpack)"));
                last = e.level();
            }
        }
    }

    @Test
    void knownUnlocks() {
        assertTrue(SkillGuide.entries(Skill.WOODCUTTING).contains(new SkillGuide.Entry(15, "Oak tree")));
        assertTrue(SkillGuide.entries(Skill.MINING).contains(new SkillGuide.Entry(85, "Runite rocks")));
        assertTrue(SkillGuide.entries(Skill.PRAYER).contains(new SkillGuide.Entry(1, "Thick Skin")));
        assertTrue(SkillGuide.entries(Skill.FIREMAKING).contains(new SkillGuide.Entry(15, "Light oak logs")));
        assertTrue(SkillGuide.entries(Skill.RUNECRAFT).contains(new SkillGuide.Entry(1, "Air rune")));
    }

    @Test
    void unlockedCount() {
        int all = SkillGuide.entries(Skill.WOODCUTTING).size();
        assertEquals(all, SkillGuide.unlockedCount(Skill.WOODCUTTING, 99));
        assertTrue(SkillGuide.unlockedCount(Skill.WOODCUTTING, 1) < all);
    }
}
