package com.runecraft.content.dungeons;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-save storage for the Stronghold of Security reward chests each player has opened. */
final class DungeonState extends SavedData {
    static final String KEY = "runecraft_dungeons";

    private final Map<UUID, Integer> claimed = new HashMap<>();

    static DungeonState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(DungeonState::new, DungeonState::fromNbt, null), KEY);
    }

    boolean hasClaimed(UUID uuid, int chest) {
        return (claimed.getOrDefault(uuid, 0) & (1 << chest)) != 0;
    }

    void claim(UUID uuid, int chest) {
        claimed.merge(uuid, 1 << chest, (a, b) -> a | b);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        claimed.forEach((uuid, mask) -> nbt.putInt(uuid.toString(), mask));
        return nbt;
    }

    static DungeonState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        DungeonState state = new DungeonState();
        for (String key : nbt.getAllKeys()) {
            try {
                state.claimed.put(UUID.fromString(key), nbt.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
