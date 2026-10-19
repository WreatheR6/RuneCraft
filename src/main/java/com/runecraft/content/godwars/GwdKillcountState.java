package com.runecraft.content.godwars;

import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-save storage for every player's God Wars kill count (one counter per god). */
final class GwdKillcountState extends SavedData {
    static final String KEY = "runecraft_godwars";

    private final Map<UUID, int[]> counts = new HashMap<>();

    static GwdKillcountState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(GwdKillcountState::new, GwdKillcountState::fromNbt, null), KEY);
    }

    private int[] of(UUID uuid) {
        return counts.computeIfAbsent(uuid, u -> new int[God.values().length]);
    }

    int get(UUID uuid, God god) {
        return of(uuid)[god.ordinal()];
    }

    void set(UUID uuid, God god, int value) {
        of(uuid)[god.ordinal()] = Math.max(0, value);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        counts.forEach((uuid, arr) -> nbt.putIntArray(uuid.toString(), arr));
        return nbt;
    }

    static GwdKillcountState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        GwdKillcountState state = new GwdKillcountState();
        for (String key : nbt.getAllKeys()) {
            try {
                int[] stored = nbt.getIntArray(key);
                int[] arr = new int[God.values().length];
                System.arraycopy(stored, 0, arr, 0, Math.min(stored.length, arr.length));
                state.counts.put(UUID.fromString(key), arr);
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
