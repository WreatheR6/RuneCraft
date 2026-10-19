package com.runecraft.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Access to persistent player data. */
public final class Players {
    private Players() {
    }

    public static PlayerDataState state(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PlayerDataState::fromNbt, PlayerDataState::new, PlayerDataState.KEY);
    }

    public static PlayerData data(ServerPlayer player) {
        return state(player.getServer()).get(player.getUUID());
    }
}
