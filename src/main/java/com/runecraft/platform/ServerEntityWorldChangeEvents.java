package com.runecraft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Fabric's dimension change event for players, fired from NeoForge's {@code PlayerChangedDimensionEvent}. */
public final class ServerEntityWorldChangeEvents {
    private ServerEntityWorldChangeEvents() {
    }

    @FunctionalInterface
    public interface AfterPlayerChange {
        void afterChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination);
    }

    public static final Event<AfterPlayerChange> AFTER_PLAYER_CHANGE_WORLD = new Event<>(listeners -> (player, origin, destination) -> {
        for (AfterPlayerChange l : listeners) {
            l.afterChangeWorld(player, origin, destination);
        }
    });
}
