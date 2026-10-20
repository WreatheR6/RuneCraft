package com.runecraft.content.godwars;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-save storage for the Wilderness content of the godwars module, per player: whether the Revenant Caves' entry fee is
 * paid ({@link #REV_FEE}), and the Mage Arena's god spell casts inside the arena ({@code ma_casts_<god>}, 100 to learn a
 * spell for use outside it).
 */
final class WildernessState extends SavedData {
    static final String KEY = "runecraft_wilderness";
    /** 1 once the Revenant Caves' 100,000 coin fee is paid (until the player dies there or to another player). */
    static final String REV_FEE = "rev_fee";

    private final Map<UUID, Map<String, Integer>> values = new HashMap<>();

    static WildernessState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(WildernessState::new, WildernessState::fromNbt, null), KEY);
    }

    int get(UUID player, String key) {
        Map<String, Integer> m = values.get(player);
        return m == null ? 0 : m.getOrDefault(key, 0);
    }

    void set(UUID player, String key, int value) {
        Map<String, Integer> m = values.computeIfAbsent(player, u -> new HashMap<>());
        if (value == 0) {
            m.remove(key);
        } else {
            m.put(key, value);
        }
        setDirty();
    }

    /** A copy of a player's values (the self-test saves and restores them). */
    Map<String, Integer> snapshot(UUID player) {
        Map<String, Integer> m = values.get(player);
        return m == null ? new HashMap<>() : new HashMap<>(m);
    }

    void restore(UUID player, Map<String, Integer> saved) {
        if (saved == null || saved.isEmpty()) {
            values.remove(player);
        } else {
            values.put(player, new HashMap<>(saved));
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        values.forEach((uuid, m) -> {
            CompoundTag p = new CompoundTag();
            m.forEach(p::putInt);
            nbt.put(uuid.toString(), p);
        });
        return nbt;
    }

    static WildernessState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        WildernessState state = new WildernessState();
        for (String key : nbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag p = nbt.getCompound(key);
                Map<String, Integer> m = new HashMap<>();
                for (String k : p.getAllKeys()) {
                    m.put(k, p.getInt(k));
                }
                state.values.put(uuid, m);
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
