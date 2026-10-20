package com.runecraft.content.godwars;

import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-save storage for every player's God Wars kill count ("essence": one counter per god, plus the Ancient Prison's
 * Zarosian essence) and the ropes they have tied (the hole north of Trollheim, the two rocks down to Saradomin's
 * Encampment), which OSRS keeps for good.
 */
final class GwdKillcountState extends SavedData {
    static final String KEY = "runecraft_godwars";
    /** Slot of the Zarosian essence, after the four gods (saved arrays from before it are padded). */
    static final int ZAROS = God.values().length;
    /** Slot of the rope flags. */
    private static final int FLAGS = ZAROS + 1;
    private static final int SLOTS = FLAGS + 1;

    /** Rope tied to the hole north of Trollheim. */
    static final int ROPE_HOLE = 1;
    /** Rope tied to the upper rock of Saradomin's Encampment. */
    static final int ROPE_UPPER_ROCK = 2;
    /** Rope tied to the lower rock of Saradomin's Encampment. */
    static final int ROPE_LOWER_ROCK = 4;

    private final Map<UUID, int[]> counts = new HashMap<>();

    static GwdKillcountState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(GwdKillcountState::new, GwdKillcountState::fromNbt, null), KEY);
    }

    private int[] of(UUID uuid) {
        return counts.computeIfAbsent(uuid, u -> new int[SLOTS]);
    }

    int get(UUID uuid, God god) {
        return of(uuid)[god.ordinal()];
    }

    void set(UUID uuid, God god, int value) {
        of(uuid)[god.ordinal()] = Math.max(0, value);
        setDirty();
    }

    int zaros(UUID uuid) {
        return of(uuid)[ZAROS];
    }

    void setZaros(UUID uuid, int value) {
        of(uuid)[ZAROS] = Math.max(0, value);
        setDirty();
    }

    boolean flag(UUID uuid, int flag) {
        return (of(uuid)[FLAGS] & flag) != 0;
    }

    void setFlag(UUID uuid, int flag) {
        of(uuid)[FLAGS] |= flag;
        setDirty();
    }

    /** True while the player holds any essence. */
    boolean anyEssence(UUID uuid) {
        int[] a = counts.get(uuid);
        if (a == null) {
            return false;
        }
        for (int i = 0; i <= ZAROS; i++) {
            if (a[i] > 0) {
                return true;
            }
        }
        return false;
    }

    /** Clears every essence counter (OSRS: leaving the dungeon removes it); the ropes stay tied. */
    void clearEssence(UUID uuid) {
        int[] a = counts.get(uuid);
        if (a == null) {
            return;
        }
        for (int i = 0; i <= ZAROS; i++) {
            a[i] = 0;
        }
        setDirty();
    }

    /** A copy of a player's counters and ropes (the self-test saves and restores them), or null when they have none. */
    int[] snapshot(UUID uuid) {
        int[] a = counts.get(uuid);
        return a == null ? null : a.clone();
    }

    /** Puts back a {@link #snapshot} (null: forget the player). */
    void restore(UUID uuid, int[] saved) {
        if (saved == null) {
            counts.remove(uuid);
        } else {
            counts.put(uuid, saved.clone());
        }
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
                int[] arr = new int[SLOTS];
                System.arraycopy(stored, 0, arr, 0, Math.min(stored.length, arr.length));
                state.counts.put(UUID.fromString(key), arr);
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
