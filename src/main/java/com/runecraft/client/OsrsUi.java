package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.ui.SkillPanel;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Drawing helpers for the OSRS-style interface: tiled stone, bevelled boxes, skill/tab icons and the
 * yellow OSRS tooltip. Everything draws in whatever coordinate space the caller's matrix stack sets
 * up (the side panel scales OSRS pixels to the screen), so no scissor rectangles are used.
 */
public final class OsrsUi {
    public static final Identifier STONE = RuneCraft.id("textures/gui/skills/stone.png");
    private static final int STONE_SIZE = 64;

    public static final int YELLOW = 0xFF000000 | SkillPanel.YELLOW;
    public static final int ORANGE = 0xFF000000 | SkillPanel.ORANGE;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GREY = 0xFF9F9F9F;
    public static final int STONE_DARK = 0xFF000000 | SkillPanel.STONE_DARK;
    public static final int EDGE = 0xFF000000 | SkillPanel.STONE_EDGE;
    public static final int LIGHT = 0xFF000000 | SkillPanel.STONE_LIGHT;
    public static final int ACTIVE = 0xFF000000 | SkillPanel.TAB_ACTIVE;

    private OsrsUi() {
    }

    public static Identifier skillIcon(Skill skill) {
        return RuneCraft.id("textures/gui/skills/" + skill.id() + ".png");
    }

    public static Identifier tabIcon(String name) {
        return RuneCraft.id("textures/gui/skills/tab_" + name + ".png");
    }

    /** Draws a 32x32 icon texture scaled to {@code size} x {@code size}. */
    public static void icon(DrawContext ctx, Identifier icon, int x, int y, int size) {
        ctx.drawTexture(icon, x, y, size, size, 0, 0, 32, 32, 32, 32);
    }

    /** Fills a rectangle with the tiled stone texture. */
    public static void stone(DrawContext ctx, int x, int y, int w, int h) {
        for (int ty = y; ty < y + h; ty += STONE_SIZE) {
            int dh = Math.min(STONE_SIZE, y + h - ty);
            for (int tx = x; tx < x + w; tx += STONE_SIZE) {
                int dw = Math.min(STONE_SIZE, x + w - tx);
                ctx.drawTexture(STONE, tx, ty, dw, dh, 0, 0, dw, dh, STONE_SIZE, STONE_SIZE);
            }
        }
    }

    /** Stone panel with the OSRS double border (black outside, light stone inside). */
    public static void stonePanel(DrawContext ctx, int x, int y, int w, int h) {
        stone(ctx, x, y, w, h);
        ctx.drawBorder(x, y, w, h, 0xFF100C08);
        ctx.drawBorder(x + 1, y + 1, w - 2, h - 2, 0xFF5E5444);
    }

    /** A dark inset box: dark top/left edge, light bottom/right bevel, like OSRS skill tiles. */
    public static void inset(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, STONE_DARK);
        ctx.fill(x, y, x + w, y + 1, EDGE);
        ctx.fill(x, y, x + 1, y + h, EDGE);
        ctx.fill(x, y + h - 1, x + w, y + h, LIGHT);
        ctx.fill(x + w - 1, y, x + w, y + h, LIGHT);
    }

    /** A raised stone button; {@code active} gives it the red OSRS "selected" tint. */
    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean active, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, active ? ACTIVE : 0xFF352D22);
        ctx.fill(x, y, x + w, y + 1, active ? 0xFF8E3A2C : 0xFF554B3C);
        ctx.fill(x, y, x + 1, y + h, active ? 0xFF8E3A2C : 0xFF554B3C);
        ctx.fill(x, y + h - 1, x + w, y + h, EDGE);
        ctx.fill(x + w - 1, y, x + w, y + h, EDGE);
        if (hovered && !active) {
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x20FFFFFF);
        }
    }

    public static void centered(DrawContext ctx, TextRenderer tr, String text, int centerX, int y, int color) {
        ctx.drawTextWithShadow(tr, text, centerX - tr.getWidth(text) / 2, y, color);
    }

    /**
     * The OSRS tooltip: black border, pale yellow fill, black text without shadow. Placed below-right
     * of the cursor and kept inside {@code [0, boundW] x [0, boundH]} (the panel's own area).
     */
    public static void tooltip(DrawContext ctx, TextRenderer tr, List<String> lines, int mouseX, int mouseY, int boundW, int boundH) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int w = 0;
        for (String l : lines) {
            w = Math.max(w, tr.getWidth(l));
        }
        w += 6;
        int h = lines.size() * 10 + 3;
        int x = Math.max(0, Math.min(mouseX + 6, boundW - w));
        int y = mouseY + 12;
        if (y + h > boundH) {
            y = Math.max(0, mouseY - h - 2);
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        ctx.fill(x, y, x + w, y + h, 0xFF000000 | SkillPanel.TOOLTIP_BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | SkillPanel.TOOLTIP_BG);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), x + 3, y + 2 + i * 10, 0xFF000000, false);
        }
        ctx.getMatrices().pop();
    }
}
