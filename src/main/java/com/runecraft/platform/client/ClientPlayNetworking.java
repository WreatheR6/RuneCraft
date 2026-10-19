package com.runecraft.platform.client;

import com.runecraft.platform.PlatformNetworking;
import com.runecraft.platform.RawPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric's client networking on top of {@link RawPayload}. Handlers run on the client thread (NeoForge's default for
 * payload handlers); the mod's handlers also hop with {@code client.execute}, which then runs the task at once.
 */
public final class ClientPlayNetworking {
    private ClientPlayNetworking() {
    }

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, ResponseSender responseSender);
    }

    /** Replies to the server from a client handler. */
    public static final class ResponseSender {
        public void sendPacket(ResourceLocation channel, FriendlyByteBuf buf) {
            send(channel, buf);
        }
    }

    private static final Map<ResourceLocation, PlayChannelHandler> RECEIVERS = new ConcurrentHashMap<>();
    private static final ResponseSender SENDER = new ResponseSender();

    public static boolean registerGlobalReceiver(ResourceLocation channel, PlayChannelHandler handler) {
        return RECEIVERS.putIfAbsent(channel, handler) == null;
    }

    /** Sends a packet to the server; does nothing while not connected or when the server lacks RuneCraft. */
    public static void send(ResourceLocation channel, FriendlyByteBuf buf) {
        if (canSend(channel)) {
            PacketDistributor.sendToServer(RawPayload.of(channel, buf));
        }
    }

    public static boolean canSend(ResourceLocation channel) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(RawPayload.TYPE);
    }

    static void install() {
        PlatformNetworking.clientDispatch = ClientPlayNetworking::handle;
    }

    private static void handle(RawPayload payload, IPayloadContext context) {
        PlayChannelHandler handler = RECEIVERS.get(payload.channel());
        Minecraft client = Minecraft.getInstance();
        if (handler == null || client.getConnection() == null) {
            return;
        }
        handler.receive(client, client.getConnection(), new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data())), SENDER);
    }
}
