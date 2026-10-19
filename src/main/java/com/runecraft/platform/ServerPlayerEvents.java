package com.runecraft.platform;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric's player respawn event: the old and the new player object, and whether the player was alive (left the End)
 * rather than dead. Fired from NeoForge's {@code PlayerRespawnEvent}, with the old player remembered from
 * {@code PlayerEvent.Clone}.
 */
public final class ServerPlayerEvents {
    private ServerPlayerEvents() {
    }

    @FunctionalInterface
    public interface AfterRespawn {
        void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive);
    }

    public static final Event<AfterRespawn> AFTER_RESPAWN = new Event<>(listeners -> (oldPlayer, newPlayer, alive) -> {
        for (AfterRespawn l : listeners) {
            l.afterRespawn(oldPlayer, newPlayer, alive);
        }
    });
}
