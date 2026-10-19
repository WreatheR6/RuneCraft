package com.runecraft.client.content.slayer;

import com.runecraft.client.ClientState;
import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.slayer.SlayerNet;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Client-side behaviour for the slayer content module: the slayer task counter on the HUD. */
public final class SlayerClientHooks implements ClientModuleHooks {
    private static String task = "";
    private static int remaining;
    private static int points;
    private static int streak;
    private static String region = "";

    @Override
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(SlayerNet.TASK, (client, handler, buf, sender) -> {
            String t = buf.readUtf();
            int r = buf.readVarInt();
            int pts = buf.readVarInt();
            int s = buf.readVarInt();
            String reg = buf.readUtf();
            client.execute(() -> {
                task = t;
                remaining = r;
                points = pts;
                streak = s;
                region = reg;
            });
        });
        HudRenderCallback.EVENT.register(SlayerClientHooks::render);
    }

    private static void render(GuiGraphics ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || task.isEmpty() || remaining <= 0) {
            return;
        }
        // The core HUD draws its own (legacy) task line in the same spot; ours replaces it.
        if (!ClientState.slayerTask.isEmpty() && ClientState.slayerRemaining > 0) {
            return;
        }
        Font tr = mc.font;
        int w = ctx.guiWidth();
        String line = "Slayer: " + remaining + " x " + task + (region.isEmpty() ? "" : " (" + region + ")");
        ctx.drawString(tr, line, w - tr.width(line) - 4, 48, 0xFFC0C0C0);
        String sub = points + " pts, streak " + streak;
        ctx.drawString(tr, sub, w - tr.width(sub) - 4, 58, 0xFF909090);
    }
}
