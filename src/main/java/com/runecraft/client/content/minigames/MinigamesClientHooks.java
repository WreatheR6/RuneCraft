package com.runecraft.client.content.minigames;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.minigames.MinigameNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Client side of the minigames module: the minigame overlay (wave, points, timers) and reward shops. */
public final class MinigamesClientHooks implements ClientModuleHooks {
    /** Current overlay lines (empty when not in a minigame). */
    static final List<String> HUD = new ArrayList<>();

    @Override
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(MinigameNet.HUD, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                lines.add(buf.readString());
            }
            client.execute(() -> {
                HUD.clear();
                HUD.addAll(lines);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(MinigameNet.SHOP, (client, handler, buf, sender) -> {
            RewardShopScreen.ShopData data = RewardShopScreen.ShopData.read(buf);
            client.execute(() -> {
                if (client.currentScreen instanceof RewardShopScreen screen && screen.shopId().equals(data.id())) {
                    screen.update(data);
                } else {
                    client.setScreen(new RewardShopScreen(data));
                }
            });
        });
        HudRenderCallback.EVENT.register(MinigamesClientHooks::renderHud);
    }

    private static void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (HUD.isEmpty() || mc.player == null || mc.options.hudHidden) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int width = 0;
        int lines = 0;
        for (String l : HUD) {
            if (!l.isEmpty()) {
                width = Math.max(width, tr.getWidth(l));
                lines++;
            }
        }
        int x = 4;
        int y = 4;
        ctx.fill(x - 2, y - 2, x + width + 4, y + lines * 10 + 1, 0xA0201810);
        boolean first = true;
        for (String l : HUD) {
            if (l.isEmpty()) {
                continue;
            }
            ctx.drawTextWithShadow(tr, l, x + 1, y, first ? 0xFFFF9800 : 0xFFFFFF60);
            y += 10;
            first = false;
        }
    }
}
