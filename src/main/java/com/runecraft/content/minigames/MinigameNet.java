package com.runecraft.content.minigames;

import com.runecraft.RuneCraft;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minigame packet channels: the HUD overlay and reward shops. */
public final class MinigameNet {
    /** S2C: overlay lines (empty list hides the overlay). */
    public static final ResourceLocation HUD = RuneCraft.id("minigames/hud");
    /** S2C: open a reward shop. */
    public static final ResourceLocation SHOP = RuneCraft.id("minigames/shop");
    /** C2S: buy from a reward shop (shop id, entry index, quantity). */
    public static final ResourceLocation BUY = RuneCraft.id("minigames/buy");

    private static final Map<UUID, List<String>> LAST_HUD = new HashMap<>();

    private MinigameNet() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BUY, (server, player, handler, buf, sender) -> {
            String shop = buf.readUtf(64);
            int index = buf.readVarInt();
            int quantity = buf.readVarInt();
            server.execute(() -> RewardShopService.buy(player, shop, index, quantity));
        });
    }

    public static void sendHud(ServerPlayer player, List<String> lines) {
        List<String> last = LAST_HUD.get(player.getUUID());
        if (lines.equals(last)) {
            return;
        }
        LAST_HUD.put(player.getUUID(), List.copyOf(lines));
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(lines.size());
        for (String l : lines) {
            buf.writeUtf(l);
        }
        ServerPlayNetworking.send(player, HUD, buf);
    }

    static void forget(ServerPlayer player) {
        LAST_HUD.remove(player.getUUID());
    }
}
