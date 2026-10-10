package com.runecraft.osrs.ui;

import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillPanelTest {
    @Test
    void layoutFollowsOsrsOrder() {
        List<Skill> l = SkillPanel.layout();
        assertEquals(Skill.count(), l.size());
        assertEquals(Skill.count(), EnumSet.copyOf(l).size());
        // Column 1: Attack, Strength, Defence, Ranged, Prayer, Magic, Runecraft, Construction.
        Skill[] col0 = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.PRAYER, Skill.MAGIC, Skill.RUNECRAFT,
                Skill.CONSTRUCTION};
        Skill[] col1 = {Skill.HITPOINTS, Skill.AGILITY, Skill.HERBLORE, Skill.THIEVING, Skill.CRAFTING, Skill.FLETCHING, Skill.SLAYER,
                Skill.HUNTER};
        Skill[] col2 = {Skill.MINING, Skill.SMITHING, Skill.FISHING, Skill.COOKING, Skill.FIREMAKING, Skill.WOODCUTTING, Skill.FARMING,
                Skill.SAILING};
        for (int row = 0; row < 8; row++) {
            assertEquals(col0[row], l.get(row * 3));
            assertEquals(col1[row], l.get(row * 3 + 1));
            assertEquals(col2[row], l.get(row * 3 + 2));
            assertEquals(0, SkillPanel.column(col0[row]));
            assertEquals(2, SkillPanel.column(col2[row]));
            assertEquals(row, SkillPanel.row(col1[row]));
        }
        assertEquals(8, SkillPanel.rows());
    }

    @Test
    void thousandSeparators() {
        assertEquals("0", SkillPanel.formatNumber(0));
        assertEquals("999", SkillPanel.formatNumber(999));
        assertEquals("1,000", SkillPanel.formatNumber(1000));
        assertEquals("1,234,567", SkillPanel.formatNumber(1_234_567));
        assertEquals("200,000,000", SkillPanel.formatNumber(200_000_000));
        assertEquals("4,800,000,000", SkillPanel.formatNumber(4_800_000_000L));
    }

    @Test
    void tooltipMidLevel() {
        List<String> t = SkillPanel.tooltip(Skill.ATTACK, 1_234_567 * 10 + 5);
        assertEquals(List.of("Attack XP: 1,234,567", "Next level at: 1,336,443", "Remaining XP: 101,876"), t);
        assertEquals(1_336_443, SkillPanel.nextLevelXp(1_234_567));
        assertEquals(101_876, SkillPanel.remainingXp(1_234_567));
    }

    @Test
    void tooltipAtLevelBoundaries() {
        assertEquals(List.of("Hitpoints XP: 1,154", "Next level at: 1,358", "Remaining XP: 204"),
                SkillPanel.tooltip(Skill.HITPOINTS, Skill.HITPOINTS.startingXpTenths()));
        assertEquals(List.of("Mining XP: 0", "Next level at: 83", "Remaining XP: 83"), SkillPanel.tooltip(Skill.MINING, 0));
        assertEquals(List.of("Mining XP: 13,034,430", "Next level at: 13,034,431", "Remaining XP: 1"),
                SkillPanel.tooltip(Skill.MINING, 13_034_430 * 10));
    }

    @Test
    void tooltipAt99AndCap() {
        assertEquals(List.of("Magic XP: 13,034,431"), SkillPanel.tooltip(Skill.MAGIC, 13_034_431 * 10));
        assertEquals(List.of("Magic XP: 150,000,000"), SkillPanel.tooltip(Skill.MAGIC, 150_000_000 * 10));
        assertEquals(List.of("Sailing XP: 200,000,000"), SkillPanel.tooltip(Skill.SAILING, Experience.MAX_XP_TENTHS));
        assertEquals(-1, SkillPanel.nextLevelXp(Experience.MAX_XP));
        assertEquals(0, SkillPanel.remainingXp(Experience.MAX_XP));
    }

    @Test
    void boostColours() {
        assertEquals(SkillPanel.GREEN, SkillPanel.currentLevelColor(118, 99));
        assertEquals(SkillPanel.RED, SkillPanel.currentLevelColor(50, 70));
        assertEquals(SkillPanel.YELLOW, SkillPanel.currentLevelColor(70, 70));
    }

    @Test
    void hitTesting() {
        assertEquals(Skill.ATTACK, SkillPanel.skillAt(SkillPanel.GRID_X + 1, SkillPanel.GRID_Y + 1));
        assertEquals(Skill.MINING, SkillPanel.skillAt(SkillPanel.GRID_X + 2 * SkillPanel.TILE_W + 5, SkillPanel.GRID_Y + 5));
        assertEquals(Skill.SAILING, SkillPanel.skillAt(SkillPanel.GRID_X + 2 * SkillPanel.TILE_W + 5,
                SkillPanel.GRID_Y + 7 * SkillPanel.TILE_H + 5));
        // the 2px gap between tiles belongs to no skill
        assertNull(SkillPanel.skillAt(SkillPanel.GRID_X + SkillPanel.TILE_W - 1, SkillPanel.GRID_Y + 5));
        assertNull(SkillPanel.skillAt(0, 0));
        // NaN = cursor captured by the game (HUD rendering): nothing is hovered
        assertNull(SkillPanel.skillAt(Double.NaN, Double.NaN));
        assertEquals(-1, SkillPanel.tabAt(Double.NaN, Double.NaN));
        assertFalse(SkillPanel.inTotalBox(Double.NaN, Double.NaN));
        assertEquals(Skill.DEFENCE, SkillPanel.skillAt(SkillPanel.tileX(Skill.DEFENCE) + 3, SkillPanel.tileY(Skill.DEFENCE) + 3));
        assertTrue(SkillPanel.inTotalBox(SkillPanel.totalBoxX() + 50, SkillPanel.totalBoxY() + 5));
        assertTrue(SkillPanel.totalBoxY() + SkillPanel.TOTAL_H <= SkillPanel.CONTENT_H);
        assertTrue(SkillPanel.totalBoxX() + SkillPanel.totalBoxW() <= SkillPanel.CONTENT_W);
    }

    @Test
    void tabsAndScaling() {
        assertEquals(0, SkillPanel.tabAt(SkillPanel.TAB_X0 + 1, 5));
        assertEquals(6, SkillPanel.tabAt(SkillPanel.TAB_X0 + 6 * SkillPanel.TAB_W + 3, 30));
        assertEquals(10, SkillPanel.tabAt(SkillPanel.TAB_X0 + 3 * SkillPanel.TAB_W + 3, SkillPanel.PANEL_H - 5));
        assertEquals(-1, SkillPanel.tabAt(100, 150));
        assertEquals(SkillPanel.PANEL_H - SkillPanel.TAB_H, SkillPanel.tabY(8));
        assertEquals(1.0, SkillPanel.pixelsPerUnit(480));
        assertEquals(1.5, SkillPanel.pixelsPerUnit(1080));
        assertEquals(3.5, SkillPanel.pixelsPerUnit(2160));
        // the panel never covers more than ~55% of a normal screen
        assertTrue(SkillPanel.pixelsPerUnit(1080) * SkillPanel.PANEL_H <= 1080 * SkillPanel.SCREEN_FRACTION);
    }

    @Test
    void totalTooltip() {
        SkillSet s = new SkillSet();
        assertEquals(List.of("Total XP: 1,154"), SkillPanel.totalTooltip(s));
        s.setXpTenths(Skill.ATTACK, 2_000_000 * 10);
        assertEquals(List.of("Total XP: 2,001,154"), SkillPanel.totalTooltip(s));
    }
}
