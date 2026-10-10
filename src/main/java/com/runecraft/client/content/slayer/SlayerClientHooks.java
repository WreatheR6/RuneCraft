package com.runecraft.client.content.slayer;

import com.runecraft.client.ClientState;
import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.slayer.SlayerNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

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
            String t = buf.readString();
            int r = buf.readVarInt();
            int pts = buf.readVarInt();
            int s = buf.readVarInt();
            String reg = buf.readString();
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

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || task.isEmpty() || remaining <= 0) {
            return;
        }
        // The core HUD draws its own (legacy) task line in the same spot; ours replaces it.
        if (!ClientState.slayerTask.isEmpty() && ClientState.slayerRemaining > 0) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        String line = "Slayer: " + remaining + " x " + task + (region.isEmpty() ? "" : " (" + region + ")");
        ctx.drawTextWithShadow(tr, line, w - tr.getWidth(line) - 4, 48, 0xFFC0C0C0);
        String sub = points + " pts, streak " + streak;
        ctx.drawTextWithShadow(tr, sub, w - tr.getWidth(sub) - 4, 58, 0xFF909090);
    }
}
