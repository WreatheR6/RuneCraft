package com.runecraft.content.dungeons;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-save storage for the Stronghold of Security reward chests each player has opened. */
final class DungeonState extends PersistentState {
    static final String KEY = "runecraft_dungeons";

    private final Map<UUID, Integer> claimed = new HashMap<>();

    static DungeonState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DungeonState::fromNbt, DungeonState::new, KEY);
    }

    boolean hasClaimed(UUID uuid, int chest) {
        return (claimed.getOrDefault(uuid, 0) & (1 << chest)) != 0;
    }

    void claim(UUID uuid, int chest) {
        claimed.merge(uuid, 1 << chest, (a, b) -> a | b);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        claimed.forEach((uuid, mask) -> nbt.putInt(uuid.toString(), mask));
        return nbt;
    }

    static DungeonState fromNbt(NbtCompound nbt) {
        DungeonState state = new DungeonState();
        for (String key : nbt.getKeys()) {
            try {
                state.claimed.put(UUID.fromString(key), nbt.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
