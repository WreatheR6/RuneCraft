package com.runecraft.client.content.systems;

import com.runecraft.RuneCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Run energy and special attack orbs, drawn just left of the core HP/prayer orbs. */
public final class SystemsHud {
    private static final int W = 62;

    private SystemsHud() {
    }

    public static void render(GuiGraphics ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !SysClientState.received) {
            return;
        }
        boolean gielinor = mc.level != null && mc.level.dimension().location().equals(RuneCraft.id("gielinor"));
        Font tr = mc.font;
        int x = ctx.guiWidth() - 92 - W - 4;
        int y = 4;
        if (gielinor) {
            int runColor = SysClientState.stamina ? 0xFFC08030 : 0xFFC0B020;
            bar(ctx, tr, x, y, "Run " + SysClientState.run / 100 + "%", runColor, SysClientState.run / 10000f);
        }
        int specColor = SysClientState.specArmed ? 0xFF30D030 : SysClientState.specCost >= 0 && SysClientState.spec >= SysClientState.specCost
                ? 0xFF3A8A9A : 0xFF4A5A5A;
        bar(ctx, tr, x, y + 14, (SysClientState.specArmed ? "SPEC " : "Spec ") + SysClientState.spec + "%", specColor,
                SysClientState.spec / 100f);
        StringBuilder status = new StringBuilder();
        if (!"Standard".equals(SysClientState.book)) {
            status.append(SysClientState.book.startsWith("Ancient") ? "Anc" : SysClientState.book.substring(0, Math.min(3, SysClientState.book.length())));
        }
        if (SysClientState.vengeance) {
            status.append(status.length() > 0 ? " " : "").append("Veng");
        }
        if (SysClientState.thrall > 0) {
            status.append(status.length() > 0 ? " " : "").append("Thrall");
        }
        if (status.length() > 0) {
            ctx.fill(x, y + 28, x + W, y + 40, 0xA0201810);
            ctx.drawString(tr, status.toString(), x + 3, y + 30, 0xFFE0C0FF);
        }
        if (!SysClientState.autocast.isEmpty()) {
            String a = SysClientState.autocast;
            ctx.drawString(tr, a, x + W - tr.width(a), y + 42, 0xFFB0A0FF);
        }
    }

    private static void bar(GuiGraphics ctx, Font tr, int x, int y, String label, int color, float fill) {
        ctx.fill(x, y, x + W, y + 12, 0xA0201810);
        ctx.fill(x + 1, y + 1, x + 1 + (int) ((W - 2) * Math.max(0, Math.min(1, fill))), y + 11, color);
        ctx.drawString(tr, label, x + 3, y + 2, 0xFFFFFFFF);
    }
}
