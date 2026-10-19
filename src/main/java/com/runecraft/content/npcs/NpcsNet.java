package com.runecraft.content.npcs;

import com.runecraft.RuneCraft;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/** Packet channels for NPC shops. */
public final class NpcsNet {
    /** S2C: open or refresh a shop (see {@link ShopService#send}). */
    public static final ResourceLocation SHOP = RuneCraft.id("npcs/shop");
    /** C2S: buy (shop id, item id, quantity). */
    public static final ResourceLocation BUY = RuneCraft.id("npcs/buy");
    /** C2S: sell (shop id, item id, quantity). */
    public static final ResourceLocation SELL = RuneCraft.id("npcs/sell");
    /** C2S: the shop screen was closed (shop id). */
    public static final ResourceLocation CLOSE = RuneCraft.id("npcs/close");

    private NpcsNet() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BUY, (server, player, handler, buf, sender) -> {
            String shop = buf.readUtf(64);
            String item = buf.readUtf(128);
            int quantity = buf.readVarInt();
            server.execute(() -> ShopService.buy(player, shop, item, quantity));
        });
        ServerPlayNetworking.registerGlobalReceiver(SELL, (server, player, handler, buf, sender) -> {
            String shop = buf.readUtf(64);
            String item = buf.readUtf(128);
            int quantity = buf.readVarInt();
            server.execute(() -> ShopService.sell(player, shop, item, quantity));
        });
        ServerPlayNetworking.registerGlobalReceiver(CLOSE, (server, player, handler, buf, sender) -> {
            String shop = buf.readUtf(64);
            server.execute(() -> ShopService.close(player, shop));
        });
    }
}
