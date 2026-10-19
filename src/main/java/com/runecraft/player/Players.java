package com.runecraft.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Access to persistent player data. */
public final class Players {
    private Players() {
    }

    public static PlayerDataState state(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(PlayerDataState::new, PlayerDataState::fromNbt, null), PlayerDataState.KEY);
    }

    public static PlayerData data(ServerPlayer player) {
        return state(player.getServer()).get(player.getUUID());
    }
}
