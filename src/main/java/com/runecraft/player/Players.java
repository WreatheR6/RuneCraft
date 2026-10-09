package com.runecraft.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Access to persistent player data. */
public final class Players {
    private Players() {
    }

    public static PlayerDataState state(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(PlayerDataState::fromNbt, PlayerDataState::new, PlayerDataState.KEY);
    }

    public static PlayerData data(ServerPlayerEntity player) {
        return state(player.getServer()).get(player.getUuid());
    }
}
