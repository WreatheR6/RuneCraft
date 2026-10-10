package com.runecraft.content.minigames;

import com.runecraft.RuneCraft;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minigame packet channels: the HUD overlay and reward shops. */
public final class MinigameNet {
    /** S2C: overlay lines (empty list hides the overlay). */
    public static final Identifier HUD = RuneCraft.id("minigames/hud");
    /** S2C: open a reward shop. */
    public static final Identifier SHOP = RuneCraft.id("minigames/shop");
    /** C2S: buy from a reward shop (shop id, entry index, quantity). */
    public static final Identifier BUY = RuneCraft.id("minigames/buy");

    private static final Map<UUID, List<String>> LAST_HUD = new HashMap<>();

    private MinigameNet() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BUY, (server, player, handler, buf, sender) -> {
            String shop = buf.readString(64);
            int index = buf.readVarInt();
            int quantity = buf.readVarInt();
            server.execute(() -> RewardShopService.buy(player, shop, index, quantity));
        });
    }

    public static void sendHud(ServerPlayerEntity player, List<String> lines) {
        List<String> last = LAST_HUD.get(player.getUuid());
        if (lines.equals(last)) {
            return;
        }
        LAST_HUD.put(player.getUuid(), List.copyOf(lines));
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(lines.size());
        for (String l : lines) {
            buf.writeString(l);
        }
        ServerPlayNetworking.send(player, HUD, buf);
    }

    static void forget(ServerPlayerEntity player) {
        LAST_HUD.remove(player.getUuid());
    }
}
