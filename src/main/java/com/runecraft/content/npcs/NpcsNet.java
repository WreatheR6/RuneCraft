package com.runecraft.content.npcs;

import com.runecraft.RuneCraft;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/** Packet channels for NPC shops. */
public final class NpcsNet {
    /** S2C: open or refresh a shop (see {@link ShopService#send}). */
    public static final Identifier SHOP = RuneCraft.id("npcs/shop");
    /** C2S: buy (shop id, item id, quantity). */
    public static final Identifier BUY = RuneCraft.id("npcs/buy");
    /** C2S: sell (shop id, item id, quantity). */
    public static final Identifier SELL = RuneCraft.id("npcs/sell");
    /** C2S: the shop screen was closed (shop id). */
    public static final Identifier CLOSE = RuneCraft.id("npcs/close");

    private NpcsNet() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BUY, (server, player, handler, buf, sender) -> {
            String shop = buf.readString(64);
            String item = buf.readString(128);
            int quantity = buf.readVarInt();
            server.execute(() -> ShopService.buy(player, shop, item, quantity));
        });
        ServerPlayNetworking.registerGlobalReceiver(SELL, (server, player, handler, buf, sender) -> {
            String shop = buf.readString(64);
            String item = buf.readString(128);
            int quantity = buf.readVarInt();
            server.execute(() -> ShopService.sell(player, shop, item, quantity));
        });
        ServerPlayNetworking.registerGlobalReceiver(CLOSE, (server, player, handler, buf, sender) -> {
            String shop = buf.readString(64);
            server.execute(() -> ShopService.close(player, shop));
        });
    }
}
