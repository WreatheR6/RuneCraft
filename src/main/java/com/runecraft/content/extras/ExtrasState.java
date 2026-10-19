package com.runecraft.content.extras;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-save storage for the extras module (key {@code runecraft_extras}): house layouts and plots,
 * followers and pet house contents, Sailing voyages. NBT sections keyed by name.
 */
public final class ExtrasState extends SavedData {
    public static final String KEY = "runecraft_extras";

    private CompoundTag root = new CompoundTag();

    public static ExtrasState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ExtrasState::new, ExtrasState::fromNbt, null), KEY);
    }

    public CompoundTag section(String name) {
        if (!root.contains(name)) {
            root.put(name, new CompoundTag());
        }
        return root.getCompound(name);
    }

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

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.put("Data", root.copy());
        return nbt;
    }

    public static ExtrasState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        ExtrasState state = new ExtrasState();
        state.root = nbt.getCompound("Data");
        return state;
    }
}
