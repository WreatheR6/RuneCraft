package com.runecraft.platform;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric's server networking on top of {@link RawPayload}. Handlers run on the server thread (NeoForge's default for
 * payload handlers); the mod's handlers also hop with {@code server.execute}, which is harmless there.
 */
public final class ServerPlayNetworking {
    private ServerPlayNetworking() {
    }

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender);
    }

    private static final Map<ResourceLocation, PlayChannelHandler> RECEIVERS = new ConcurrentHashMap<>();

    public static boolean registerGlobalReceiver(ResourceLocation channel, PlayChannelHandler handler) {
        return RECEIVERS.putIfAbsent(channel, handler) == null;
    }

    /** Sends a packet to one player; does nothing for a fake player or a player without a connection. */
    public static void send(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        if (!canSend(player, channel)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, RawPayload.of(channel, buf));
    }

    /** Whether the player's client can receive RuneCraft packets (a real connection with the mod installed). */
    public static boolean canSend(ServerPlayer player, ResourceLocation channel) {
        return player != null && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && player.connection != null
                && player.connection.hasChannel(RawPayload.TYPE);
    }

    static void handle(RawPayload payload, IPayloadContext context) {
        PlayChannelHandler handler = RECEIVERS.get(payload.channel());
        if (handler == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        handler.receive(player.server, player, player.connection, buf, new PacketSender(player));
    }
}
