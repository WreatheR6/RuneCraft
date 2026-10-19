package com.runecraft.platform.client;

import com.runecraft.platform.Event;
import net.minecraft.client.Minecraft;

/** Fabric's client tick event, fired from NeoForge's {@code ClientTickEvent.Post} (see {@link ClientPlatformEvents}). */
public final class ClientTickEvents {
    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(Minecraft client);
    }

    public static final Event<EndTick> END_CLIENT_TICK = new Event<>(listeners -> client -> {
        for (EndTick l : listeners) {
            l.onEndTick(client);
        }
    });
}
