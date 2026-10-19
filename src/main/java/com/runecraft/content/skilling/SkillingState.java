package com.runecraft.content.skilling;

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
                .computeIfAbsent(SkillingState::fromNbt, SkillingState::new, KEY);
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

    /** Skilling progress changes constantly; always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.put("Data", root.copy());
        return nbt;
    }

    public static SkillingState fromNbt(CompoundTag nbt) {
        SkillingState state = new SkillingState();
        state.root = nbt.getCompound("Data");
        return state;
    }
}
