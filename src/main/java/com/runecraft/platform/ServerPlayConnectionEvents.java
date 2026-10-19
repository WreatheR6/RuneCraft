package com.runecraft.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Fabric's play connection events, fired from NeoForge's {@code PlayerLoggedInEvent} / {@code PlayerLoggedOutEvent}. */
public final class ServerPlayConnectionEvents {
    private ServerPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface Join {
        void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server);
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server);
    }

    public static final Event<Join> JOIN = new Event<>(listeners -> (handler, sender, server) -> {
        for (Join l : listeners) {
            l.onPlayReady(handler, sender, server);
        }
    });

    public static final Event<Disconnect> DISCONNECT = new Event<>(listeners -> (handler, server) -> {
        for (Disconnect l : listeners) {
            l.onPlayDisconnect(handler, server);
        }
    });
}
