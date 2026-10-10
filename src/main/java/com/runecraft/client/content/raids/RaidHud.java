package com.runecraft.client.content.raids;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.PacketByteBuf;

/** The raid overlay in the top-right corner: room, points, timer, party and boss health. */
final class RaidHud {
    private static boolean active;
    private static String raid = "";
    private static String room = "";
    private static int roomIndex;
    private static int roomCount;
    private static int personal;
    private static int total;
    private static int raidLevel = -1;
    private static int seconds;
    private static boolean downed;
    private static String boss = "";
    private static int bossHp;
    private static int bossMax;
    private static int alive;
    private static int partySize;
    private static boolean tob;

    private RaidHud() {
    }

    /** Reads a HUD packet (network thread) and applies it on the client thread. */
    static void read(MinecraftClient client, PacketByteBuf buf) {
        boolean on = buf.readBoolean();
        if (!on) {
            client.execute(() -> active = false);
            return;
        }
        String r = buf.readString();
        String rm = buf.readString();
        int ri = buf.readVarInt();
        int rc = buf.readVarInt();
        int pp = buf.readVarInt();
        int tp = buf.readVarInt();
        int lvl = buf.readVarInt();
        int secs = buf.readVarInt();
        boolean dn = buf.readBoolean();
        String b = buf.readString();
        int bh = buf.readVarInt();
        int bm = buf.readVarInt();
        int al = buf.readVarInt();
        int ps = buf.readVarInt();
        boolean isTob = buf.readBoolean();
        client.execute(() -> {
            active = true;
            raid = r;
            room = rm;
            roomIndex = ri;
            roomCount = rc;
            personal = pp;
            total = tp;
            raidLevel = lvl;
            seconds = secs;
            downed = dn;
            boss = b;
            bossHp = bh;
            bossMax = bm;
            alive = al;
            partySize = ps;
            tob = isTob;
        });
    }

    static void clear() {
        active = false;
    }

    static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client.player == null || client.options.hudHidden) {
            return;
        }
        TextRenderer tr = client.textRenderer;
        int width = 150;
        int x = ctx.getScaledWindowWidth() - width - 6;
        int y = 6;
        int lines = 5 + (raidLevel >= 0 ? 1 : 0) + (boss.isEmpty() ? 0 : 2);
        ctx.fill(x - 4, y - 4, x + width + 2, y + lines * 11 + 2, 0xA0201810);
        ctx.fill(x - 4, y - 4, x + width + 2, y - 3, 0xFFB08A40);
        ctx.drawTextWithShadow(tr, raid, x, y, 0xFFFFB000);
        y += 11;
        ctx.drawTextWithShadow(tr, room + " (" + (roomIndex + 1) + "/" + roomCount + ")", x, y, 0xFFFFFFFF);
        y += 11;
        if (tob) {
            ctx.drawTextWithShadow(tr, "Points: " + personal, x, y, 0xFFE0E0E0);
        } else {
            ctx.drawTextWithShadow(tr, "Points: " + personal + "  Total: " + total, x, y, 0xFFE0E0E0);
        }
        y += 11;
        if (raidLevel >= 0) {
            ctx.drawTextWithShadow(tr, "Raid level: " + raidLevel, x, y, 0xFFE0C060);
            y += 11;
        }
        ctx.drawTextWithShadow(tr, String.format("Time: %d:%02d", seconds / 60, seconds % 60), x, y, 0xFFC0C0C0);
        y += 11;
        ctx.drawTextWithShadow(tr, "Party: " + alive + "/" + partySize + (downed ? "  (you are down)" : ""), x, y,
                downed ? 0xFFFF6060 : 0xFFC0C0C0);
        y += 11;
        if (!boss.isEmpty() && bossMax > 0) {
            ctx.drawTextWithShadow(tr, boss + ": " + bossHp + "/" + bossMax, x, y, 0xFFFF8080);
            y += 11;
            int bar = (int) (width * Math.max(0, Math.min(1, bossHp / (double) bossMax)));
            ctx.fill(x, y, x + width, y + 6, 0xFF400000);
            ctx.fill(x, y, x + bar, y + 6, 0xFF20C020);
        }
    }
}
