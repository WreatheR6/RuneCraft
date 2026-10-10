package com.runecraft.content.extras;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * World-save storage for the extras module (key {@code runecraft_extras}): house layouts and plots,
 * followers and pet house contents, Sailing voyages. NBT sections keyed by name.
 */
public final class ExtrasState extends PersistentState {
    public static final String KEY = "runecraft_extras";

    private NbtCompound root = new NbtCompound();

    public static ExtrasState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(ExtrasState::fromNbt, ExtrasState::new, KEY);
    }

    public NbtCompound section(String name) {
        if (!root.contains(name)) {
            root.put(name, new NbtCompound());
        }
        return root.getCompound(name);
    }

    public NbtCompound entry(String section, String key) {
        NbtCompound s = section(section);
        if (!s.contains(key)) {
            s.put(key, new NbtCompound());
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
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("Data", root.copy());
        return nbt;
    }

    public static ExtrasState fromNbt(NbtCompound nbt) {
        ExtrasState state = new ExtrasState();
        state.root = nbt.getCompound("Data");
        return state;
    }
}
