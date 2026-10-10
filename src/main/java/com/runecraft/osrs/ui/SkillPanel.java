package com.runecraft.osrs.ui;

import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure data/geometry for the OSRS side panel and its Skills tab, shared by the client renderer
 * ({@code com.runecraft.client.SidePanel} / {@code SkillsTab}) and tests.
 *
 * <p>All coordinates are in OSRS pixels: the fixed-mode side panel is {@value #PANEL_W} x
 * {@value #PANEL_H}, with a row of seven 33x36 stone tabs along the top and bottom and a
 * {@value #CONTENT_W} x {@value #CONTENT_H} interface area between them. The client scales the
 * whole panel to the screen (see {@link #pixelsPerUnit(int)}).
 */
public final class SkillPanel {
    // ---- Side panel geometry (OSRS pixels) ----
    public static final int PANEL_W = 241;
    public static final int PANEL_H = 334;
    public static final int TAB_W = 33;
    public static final int TAB_H = 36;
    public static final int TAB_X0 = 5;
    public static final int TABS_PER_ROW = 7;
    public static final int CONTENT_X = 25;
    public static final int CONTENT_Y = 37;
    public static final int CONTENT_W = 190;
    public static final int CONTENT_H = 261;

    // ---- Skills tab geometry, relative to the content area ----
    public static final int COLUMNS = 3;
    public static final int TILE_W = 62;
    public static final int TILE_H = 28;
    /** Visible tile size; the remaining 2px are the gap between tiles. */
    public static final int TILE_INNER_W = TILE_W - 2;
    public static final int TILE_INNER_H = TILE_H - 2;
    public static final int GRID_X = 2;
    public static final int GRID_Y = 2;
    public static final int TOTAL_H = 26;
    public static final int ICON_SIZE = 24;

    // ---- OSRS colours (RGB) ----
    public static final int YELLOW = 0xFFFF00;
    public static final int GREEN = 0x00FF00;
    public static final int RED = 0xFF0000;
    public static final int ORANGE = 0xFF981F;
    public static final int WHITE = 0xFFFFFF;
    public static final int TOOLTIP_BG = 0xFFFFA0;
    public static final int TOOLTIP_BORDER = 0x000000;
    public static final int STONE = 0x3E3529;
    public static final int STONE_DARK = 0x2B251C;
    public static final int STONE_EDGE = 0x18140E;
    public static final int STONE_LIGHT = 0x5A5040;
    public static final int TAB_ACTIVE = 0x6B241C;

    /** Fraction of the screen height the panel aims to fill (bottom half, below the HUD orbs). */
    public static final double SCREEN_FRACTION = 0.55;

    /** The skills tab in OSRS order: three columns, read left-to-right, top-to-bottom. */
    private static final String[] OSRS_ORDER = {
            "ATTACK", "HITPOINTS", "MINING",
            "STRENGTH", "AGILITY", "SMITHING",
            "DEFENCE", "HERBLORE", "FISHING",
            "RANGED", "THIEVING", "COOKING",
            "PRAYER", "CRAFTING", "FIREMAKING",
            "MAGIC", "FLETCHING", "WOODCUTTING",
            "RUNECRAFT", "SLAYER", "FARMING",
            "CONSTRUCTION", "HUNTER", "SAILING"};

    private static final List<Skill> LAYOUT;

    static {
        List<Skill> list = new ArrayList<>();
        Set<Skill> seen = EnumSet.noneOf(Skill.class);
        for (String name : OSRS_ORDER) {
            for (Skill s : Skill.values()) {
                if (s.name().equals(name) && seen.add(s)) {
                    list.add(s);
                }
            }
        }
        // Any skill the enum has that OSRS_ORDER does not know goes at the end.
        for (Skill s : Skill.values()) {
            if (seen.add(s)) {
                list.add(s);
            }
        }
        LAYOUT = Collections.unmodifiableList(list);
    }

    private SkillPanel() {
    }

    public static List<Skill> layout() {
        return LAYOUT;
    }

    public static int rows() {
        return (LAYOUT.size() + COLUMNS - 1) / COLUMNS;
    }

    public static int column(Skill skill) {
        return LAYOUT.indexOf(skill) % COLUMNS;
    }

    public static int row(Skill skill) {
        return LAYOUT.indexOf(skill) / COLUMNS;
    }

    /** Left edge of a tile, relative to the content area. */
    public static int tileX(Skill skill) {
        return GRID_X + column(skill) * TILE_W;
    }

    public static int tileY(Skill skill) {
        return GRID_Y + row(skill) * TILE_H;
    }

    /** The skill whose visible tile contains the content-local point, or null. */
    public static Skill skillAt(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y) || x < GRID_X || y < GRID_Y) {
            return null;
        }
        int col = (int) ((x - GRID_X) / TILE_W);
        int row = (int) ((y - GRID_Y) / TILE_H);
        if (col >= COLUMNS || row >= rows()) {
            return null;
        }
        if (x - GRID_X - col * TILE_W >= TILE_INNER_W || y - GRID_Y - row * TILE_H >= TILE_INNER_H) {
            return null;
        }
        int index = row * COLUMNS + col;
        return index < LAYOUT.size() ? LAYOUT.get(index) : null;
    }

    public static int totalBoxX() {
        return GRID_X;
    }

    public static int totalBoxY() {
        return GRID_Y + rows() * TILE_H;
    }

    public static int totalBoxW() {
        return COLUMNS * TILE_W - 2;
    }

    public static boolean inTotalBox(double x, double y) {
        return x >= totalBoxX() && x < totalBoxX() + totalBoxW() && y >= totalBoxY() && y < totalBoxY() + TOTAL_H;
    }

    // ---- Text ----

    /** "1,234,567", like every number in the OSRS interface. */
    public static String formatNumber(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** Whole-XP total needed for the next real level, or -1 at level 99 (or the 200M cap). */
    public static int nextLevelXp(int xp) {
        int level = Experience.levelForXp(xp);
        if (level >= Experience.MAX_LEVEL || xp >= Experience.MAX_XP) {
            return -1;
        }
        return Experience.xpForLevel(level + 1);
    }

    /** XP left until the next level, or 0 when there is no next level. */
    public static int remainingXp(int xp) {
        int next = nextLevelXp(xp);
        return next < 0 ? 0 : Math.max(0, next - xp);
    }

    /**
     * The OSRS skill tooltip: {@code "Attack XP: 1,234"}, {@code "Next level at: 1,358"},
     * {@code "Remaining XP: 124"}. At level 99 (and at the 200M cap) only the XP line is shown.
     */
    public static List<String> tooltip(Skill skill, int xpTenths) {
        int xp = Math.max(0, Math.min(Experience.MAX_XP, xpTenths / 10));
        List<String> lines = new ArrayList<>(3);
        lines.add(skill.displayName() + " XP: " + formatNumber(xp));
        int next = nextLevelXp(xp);
        if (next >= 0) {
            lines.add("Next level at: " + formatNumber(next));
            lines.add("Remaining XP: " + formatNumber(next - xp));
        }
        return lines;
    }

    /** Tooltip of the "Total level" box. */
    public static List<String> totalTooltip(SkillSet skills) {
        return List.of("Total XP: " + formatNumber(skills.totalXp()));
    }

    /** OSRS colours the boosted level: green above the base level, red when drained, else yellow. */
    public static int currentLevelColor(int current, int base) {
        return current > base ? GREEN : current < base ? RED : YELLOW;
    }

    // ---- Scaling ----

    /**
     * Real (framebuffer) pixels per OSRS pixel for a window of the given framebuffer height: the
     * panel fills about {@link #SCREEN_FRACTION} of the height, snapped down to half-pixel steps
     * (so pixel art stays even) and never smaller than 1:1 so the text stays legible.
     */
    public static double pixelsPerUnit(int framebufferHeight) {
        double target = framebufferHeight * SCREEN_FRACTION / PANEL_H;
        return Math.max(1.0, Math.floor(target * 2.0) / 2.0);
    }

    /** Index (0-13, top row then bottom row) of the tab under a panel-local point, or -1. */
    public static int tabAt(double x, double y) {
        int row;
        if (y >= 0 && y < TAB_H) {
            row = 0;
        } else if (y >= PANEL_H - TAB_H && y < PANEL_H) {
            row = 1;
        } else {
            return -1;
        }
        if (x < TAB_X0 || x >= TAB_X0 + TABS_PER_ROW * TAB_W) {
            return -1;
        }
        return row * TABS_PER_ROW + (int) ((x - TAB_X0) / TAB_W);
    }

    public static int tabX(int index) {
        return TAB_X0 + (index % TABS_PER_ROW) * TAB_W;
    }

    public static int tabY(int index) {
        return index < TABS_PER_ROW ? 0 : PANEL_H - TAB_H;
    }
}
