package com.runecraft.content.slayer;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-save storage for every player's slayer progress (key {@code runecraft_slayer}). */
public final class SlayerState extends PersistentState {
    public static final String KEY = "runecraft_slayer";

    /** One player's slayer progress. */
    public static final class Profile {
        /** Current task category id, or null. */
        public String task;
        public int remaining;
        public int amount;
        /** Master id who assigned the task. */
        public String master;
        /** Konar's region restriction (Region enum name), or null. */
        public String region;
        public int streak;
        public int points;
        public int completed;
        public final Set<String> unlocks = new LinkedHashSet<>();
        public final List<String> blocked = new ArrayList<>();
        /** Whether the legacy PlayerData task has been migrated. */
        public boolean migrated;

        public boolean hasTask() {
            return task != null && remaining > 0;
        }

        NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("Task", task == null ? "" : task);
            nbt.putInt("Remaining", remaining);
            nbt.putInt("Amount", amount);
            nbt.putString("Master", master == null ? "" : master);
            nbt.putString("Region", region == null ? "" : region);
            nbt.putInt("Streak", streak);
            nbt.putInt("Points", points);
            nbt.putInt("Completed", completed);
            nbt.putBoolean("Migrated", migrated);
            nbt.put("Unlocks", list(unlocks));
            nbt.put("Blocked", list(blocked));
            return nbt;
        }

        static Profile fromNbt(NbtCompound nbt) {
            Profile p = new Profile();
            p.task = emptyToNull(nbt.getString("Task"));
            p.remaining = nbt.getInt("Remaining");
            p.amount = nbt.getInt("Amount");
            p.master = emptyToNull(nbt.getString("Master"));
            p.region = emptyToNull(nbt.getString("Region"));
            p.streak = nbt.getInt("Streak");
            p.points = nbt.getInt("Points");
            p.completed = nbt.getInt("Completed");
            p.migrated = nbt.getBoolean("Migrated");
            NbtList unlocks = nbt.getList("Unlocks", NbtElement.STRING_TYPE);
            for (int i = 0; i < unlocks.size(); i++) {
                p.unlocks.add(unlocks.getString(i));
            }
            NbtList blocked = nbt.getList("Blocked", NbtElement.STRING_TYPE);
            for (int i = 0; i < blocked.size(); i++) {
                p.blocked.add(blocked.getString(i));
            }
            return p;
        }

        private static NbtList list(Iterable<String> values) {
            NbtList list = new NbtList();
            for (String s : values) {
                list.add(NbtString.of(s));
            }
            return list;
        }

        private static String emptyToNull(String s) {
            return s == null || s.isEmpty() ? null : s;
        }
    }

    private final Map<UUID, Profile> profiles = new HashMap<>();

    public Profile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, u -> new Profile());
    }

    /** Profiles change on every task kill; always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        profiles.forEach((uuid, p) -> all.put(uuid.toString(), p.toNbt()));
        nbt.put("Profiles", all);
        return nbt;
    }

    public static SlayerState fromNbt(NbtCompound nbt) {
        SlayerState state = new SlayerState();
        NbtCompound all = nbt.getCompound("Profiles");
        for (String key : all.getKeys()) {
            try {
                state.profiles.put(UUID.fromString(key), Profile.fromNbt(all.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // Corrupt key: skip.
            }
        }
        return state;
    }

    public static SlayerState of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SlayerState::fromNbt, SlayerState::new, KEY);
    }
}
