package com.runecraft.client;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.ui.SkillGuide;
import com.runecraft.osrs.ui.SkillPanel;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * The OSRS Skills tab: a 3-column grid of skill tiles (icon, current level over base level), the
 * "Total level" box, the XP tooltip and, after clicking a skill, its skill guide.
 *
 * <p>Draws in content-local OSRS pixels: (0,0) is the top-left of the
 * {@link SkillPanel#CONTENT_W} x {@link SkillPanel#CONTENT_H} interface area. Mouse coordinates are
 * given in the same space (NaN when the cursor is not free).
 */
public final class SkillsTab {
    private static final int GUIDE_HEADER = 20;
    private static final int GUIDE_ROW = 11;
    private static final int GUIDE_CLOSE = 14;

    private Skill guide;
    private int guideScroll;

    /** Renders the tab and returns the tooltip lines to show (or null). */
    public List<String> render(DrawContext ctx, TextRenderer tr, double mouseX, double mouseY) {
        return guide != null ? renderGuide(ctx, tr, mouseX, mouseY) : renderGrid(ctx, tr, mouseX, mouseY);
    }

    private List<String> renderGrid(DrawContext ctx, TextRenderer tr, double mouseX, double mouseY) {
        SkillSet s = ClientState.SKILLS;
        Skill hovered = SkillPanel.skillAt(mouseX, mouseY);
        for (Skill skill : SkillPanel.layout()) {
            int x = SkillPanel.tileX(skill);
            int y = SkillPanel.tileY(skill);
            int w = SkillPanel.TILE_INNER_W;
            int h = SkillPanel.TILE_INNER_H;
            OsrsUi.inset(ctx, x, y, w, h);
            if (skill == hovered) {
                ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x18FFFFFF);
            }
            OsrsUi.icon(ctx, OsrsUi.skillIcon(skill), x + 3, y + (h - SkillPanel.ICON_SIZE) / 2, SkillPanel.ICON_SIZE);
            int cur = s.currentLevel(skill);
            int base = s.level(skill);
            String c = String.valueOf(cur);
            String b = String.valueOf(base);
            ctx.drawTextWithShadow(tr, c, x + 41 - tr.getWidth(c) / 2, y + 3, 0xFF000000 | SkillPanel.currentLevelColor(cur, base));
            ctx.drawTextWithShadow(tr, "/", x + 44, y + 9, OsrsUi.YELLOW);
            ctx.drawTextWithShadow(tr, b, x + 52 - tr.getWidth(b) / 2, y + 15, OsrsUi.YELLOW);
        }
        int tx = SkillPanel.totalBoxX();
        int ty = SkillPanel.totalBoxY();
        int tw = SkillPanel.totalBoxW();
        OsrsUi.inset(ctx, tx, ty, tw, SkillPanel.TOTAL_H);
        OsrsUi.centered(ctx, tr, "Total level:", tx + tw / 2, ty + 4, OsrsUi.YELLOW);
        OsrsUi.centered(ctx, tr, String.valueOf(s.totalLevel()), tx + tw / 2, ty + 15, OsrsUi.YELLOW);

        if (hovered != null) {
            return SkillPanel.tooltip(hovered, s.xpTenths(hovered));
        }
        if (SkillPanel.inTotalBox(mouseX, mouseY)) {
            return SkillPanel.totalTooltip(s);
        }
        return null;
    }

    private List<String> renderGuide(DrawContext ctx, TextRenderer tr, double mouseX, double mouseY) {
        int w = SkillPanel.CONTENT_W;
        int h = SkillPanel.CONTENT_H;
        int level = ClientState.SKILLS.level(guide);
        List<SkillGuide.Entry> entries = SkillGuide.entries(guide);

        OsrsUi.inset(ctx, 0, 0, w, GUIDE_HEADER);
        OsrsUi.icon(ctx, OsrsUi.skillIcon(guide), 3, 2, 16);
        ctx.drawTextWithShadow(tr, guide.displayName() + " guide", 23, 6, OsrsUi.ORANGE);
        int cx = w - GUIDE_CLOSE - 3;
        boolean closeHover = inBox(mouseX, mouseY, cx, 3, GUIDE_CLOSE, GUIDE_CLOSE);
        OsrsUi.button(ctx, cx, 3, GUIDE_CLOSE, GUIDE_CLOSE, false, closeHover);
        OsrsUi.centered(ctx, tr, "x", cx + GUIDE_CLOSE / 2, 6, closeHover ? OsrsUi.WHITE : OsrsUi.ORANGE);

        int listY = GUIDE_HEADER + 2;
        OsrsUi.inset(ctx, 0, listY, w, h - listY);
        int rows = visibleRows();
        guideScroll = clampScroll(guideScroll, entries.size());
        List<String> tip = null;
        if (entries.isEmpty()) {
            ctx.drawTextWithShadow(tr, "Nothing to unlock yet.", 6, listY + 6, OsrsUi.GREY);
        }
        for (int i = 0; i < rows && guideScroll + i < entries.size(); i++) {
            SkillGuide.Entry e = entries.get(guideScroll + i);
            int y = listY + 3 + i * GUIDE_ROW;
            boolean unlocked = e.level() <= level;
            String lv = String.valueOf(e.level());
            ctx.drawTextWithShadow(tr, lv, 22 - tr.getWidth(lv), y, unlocked ? OsrsUi.YELLOW : OsrsUi.ORANGE);
            int maxW = w - 36;
            String text = e.text();
            String shown = tr.getWidth(text) > maxW ? tr.trimToWidth(text, maxW - tr.getWidth("..")) + ".." : text;
            ctx.drawTextWithShadow(tr, shown, 28, y, unlocked ? OsrsUi.WHITE : OsrsUi.GREY);
            if (inBox(mouseX, mouseY, 2, y - 1, w - 10, GUIDE_ROW)) {
                ctx.fill(2, y - 1, w - 8, y + GUIDE_ROW - 2, 0x18FFFFFF);
                if (!shown.equals(text)) {
                    tip = List.of(text, "Level " + e.level());
                }
            }
        }
        // Scrollbar
        if (entries.size() > rows) {
            int trackY = listY + 2;
            int trackH = h - listY - 4;
            ctx.fill(w - 7, trackY, w - 2, trackY + trackH, 0xFF1E1912);
            int thumbH = Math.max(10, trackH * rows / entries.size());
            int maxScroll = entries.size() - rows;
            int thumbY = trackY + (trackH - thumbH) * guideScroll / Math.max(1, maxScroll);
            ctx.fill(w - 7, thumbY, w - 2, thumbY + thumbH, 0xFF6E5E46);
        }
        if (closeHover) {
            tip = List.of("Close guide");
        }
        return tip;
    }

    private static int visibleRows() {
        return (SkillPanel.CONTENT_H - GUIDE_HEADER - 2 - 6) / GUIDE_ROW;
    }

    private static int clampScroll(int scroll, int size) {
        return Math.max(0, Math.min(scroll, size - visibleRows()));
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Handles a left click in content-local coordinates; returns true if consumed. */
    public boolean click(double mouseX, double mouseY) {
        if (guide != null) {
            if (inBox(mouseX, mouseY, SkillPanel.CONTENT_W - GUIDE_CLOSE - 3, 3, GUIDE_CLOSE, GUIDE_CLOSE)) {
                guide = null;
                return true;
            }
            return inBox(mouseX, mouseY, 0, 0, SkillPanel.CONTENT_W, SkillPanel.CONTENT_H);
        }
        Skill skill = SkillPanel.skillAt(mouseX, mouseY);
        if (skill != null) {
            openGuide(skill);
            return true;
        }
        return false;
    }

    public void openGuide(Skill skill) {
        guide = skill;
        // Start a couple of lines above the next locked unlock so the player sees what is coming.
        int unlocked = SkillGuide.unlockedCount(skill, ClientState.SKILLS.level(skill));
        guideScroll = clampScroll(unlocked - 3, SkillGuide.entries(skill).size());
    }

    public boolean scroll(double amount) {
        if (guide == null) {
            return false;
        }
        guideScroll = clampScroll(guideScroll - (int) Math.signum(amount) * 3, SkillGuide.entries(guide).size());
        return true;
    }

    public boolean guideOpen() {
        return guide != null;
    }

    public void closeGuide() {
        guide = null;
    }
}
