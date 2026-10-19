package com.runecraft.platform.client;

import com.runecraft.platform.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Fabric's client disconnect event, fired from NeoForge's {@code ClientPlayerNetworkEvent.LoggingOut}; the handler may be null. */
public final class ClientPlayConnectionEvents {
    private ClientPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ClientPacketListener handler, Minecraft client);
    }

    public static final Event<Disconnect> DISCONNECT = new Event<>(listeners -> (handler, client) -> {
        for (Disconnect l : listeners) {
            l.onPlayDisconnect(handler, client);
        }
    });
}
