package com.runecraft.client.content.raids;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Raid completion counts, personal bests and purple counts (opened by {@code /raids}). */
final class RaidsScreen extends Screen {
    record Row(String name, int completions, int best, int purples, boolean tob) {
    }

    private final List<Row> rows;

    RaidsScreen(List<Row> rows) {
        super(Component.literal("Raids"));
        this.rows = rows;
    }

    static List<Row> read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new Row(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
        return rows;
    }

    @Override
    public void renderBackground(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int w = 280;
        int h = 40 + rows.size() * 34;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        ctx.fill(x, y, x + w, y + h, 0xE0302418);
        ctx.fill(x, y, x + w, y + 2, 0xFFB08A40);
        ctx.fill(x, y + h - 2, x + w, y + h, 0xFFB08A40);
        ctx.drawCenteredString(font, "Raids", x + w / 2, y + 8, 0xFFFFB000);
        int ry = y + 28;
        int[] colours = {0xFF6AA0E0, 0xFFE05050, 0xFFE0C060};
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            ctx.fill(x + 8, ry - 2, x + 11, ry + 26, colours[i % colours.length]);
            ctx.drawString(font, r.name(), x + 16, ry, 0xFFFFFFFF);
            ctx.drawString(font, "Completions: " + r.completions(), x + 16, ry + 12, 0xFFD0D0D0);
            String right = r.tob() ? "Purples: " + r.purples() : "Best: " + r.best() + "  Purples: " + r.purples();
            ctx.drawString(font, right, x + w - 10 - font.width(right), ry + 12, 0xFFC090FF);
            ry += 34;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
