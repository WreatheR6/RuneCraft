package com.runecraft.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Fabric's server tick events, fired from NeoForge's {@code ServerTickEvent} and {@code LevelTickEvent} (see {@link PlatformEvents}). */
public final class ServerTickEvents {
    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndWorldTick {
        void onEndTick(ServerLevel world);
    }

    public static final Event<StartTick> START_SERVER_TICK = new Event<>(listeners -> server -> {
        for (StartTick l : listeners) {
            l.onStartTick(server);
        }
    });

    public static final Event<EndTick> END_SERVER_TICK = new Event<>(listeners -> server -> {
        for (EndTick l : listeners) {
            l.onEndTick(server);
        }
    });

    public static final Event<EndWorldTick> END_WORLD_TICK = new Event<>(listeners -> world -> {
        for (EndWorldTick l : listeners) {
            l.onEndTick(world);
        }
    });
}
