package com.runecraft.platform;

import net.minecraft.server.MinecraftServer;

/** Fabric's server lifecycle events, fired from NeoForge's {@code ServerStartedEvent} and {@code ServerStoppingEvent}. */
public final class ServerLifecycleEvents {
    private ServerLifecycleEvents() {
    }

    @FunctionalInterface
    public interface ServerStarted {
        void onServerStarted(MinecraftServer server);
    }

    @FunctionalInterface
    public interface ServerStopping {
        void onServerStopping(MinecraftServer server);
    }

    public static final Event<ServerStarted> SERVER_STARTED = new Event<>(listeners -> server -> {
        for (ServerStarted l : listeners) {
            l.onServerStarted(server);
        }
    });

    public static final Event<ServerStopping> SERVER_STOPPING = new Event<>(listeners -> server -> {
        for (ServerStopping l : listeners) {
            l.onServerStopping(server);
        }
    });
}
