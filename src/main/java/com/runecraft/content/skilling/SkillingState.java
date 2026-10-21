package com.runecraft.content.skilling;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-save storage for the skilling module (key {@code runecraft_skilling}): farming patches,
 * compost bins, bird houses, Motherlode sacks, sea charting progress and port tasks. Everything is
 * kept as NBT sections keyed by a string so the format can grow without migrations.
 */
public final class SkillingState extends SavedData {
    public static final String KEY = "runecraft_skilling";

    private CompoundTag root = new CompoundTag();

    public static SkillingState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(SkillingState::new, SkillingState::fromNbt, null), KEY);
    }

    /** A named section (created on demand), e.g. "patches" or "sacks". */
    public CompoundTag section(String name) {
        if (!root.contains(name)) {
            root.put(name, new CompoundTag());
        }
        return root.getCompound(name);
    }

    /** An entry in a section (created on demand). */
    public CompoundTag entry(String section, String key) {
        CompoundTag s = section(section);
        if (!s.contains(key)) {
            s.put(key, new CompoundTag());
        }
        return s.getCompound(key);
    }

    public boolean has(String section, String key) {
        return section(section).contains(key);
    }

    public void remove(String section, String key) {
        section(section).remove(key);
    }

    /**
     * Self-test hook: a copy of every section entry keyed by this player's UUID (sea charting, port and bounty tasks,
     * boats and their cargo holds, crew, read schematics, trials, farming contracts...). Patches, sacks and other
     * entries keyed by position are not included.
     */
    public Object selfTestSave(java.util.UUID player) {
        CompoundTag out = new CompoundTag();
        String key = player.toString();
        for (String section : root.getAllKeys()) {
            CompoundTag s = root.getCompound(section);
            if (s.contains(key)) {
                out.put(section, s.get(key).copy());
            }
        }
        return out;
    }

    /** Self-test hook: puts back a {@link #selfTestSave} copy; null drops the player's entries (fresh ones are made on use). */
    public void selfTestRestore(java.util.UUID player, Object saved) {
        String key = player.toString();
        for (String section : root.getAllKeys()) {
            root.getCompound(section).remove(key);
        }
        if (saved instanceof CompoundTag t) {
            for (String section : t.getAllKeys()) {
                section(section).put(key, t.get(section).copy());
            }
        }
    }

    /** Skilling progress changes constantly; always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.put("Data", root.copy());
        return nbt;
    }

    public static SkillingState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        SkillingState state = new SkillingState();
        state.root = nbt.getCompound("Data");
        return state;
    }
}
