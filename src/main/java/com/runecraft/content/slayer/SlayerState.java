package com.runecraft.content.slayer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-save storage for every player's slayer progress (key {@code runecraft_slayer}). */
public final class SlayerState extends SavedData {
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
        /** Krystilia's separate task streak. */
        public int wildyStreak;
        public int points;
        public int completed;
        public final Set<String> unlocks = new LinkedHashSet<>();
        public final List<String> blocked = new ArrayList<>();
        /** Whether the legacy PlayerData task has been migrated. */
        public boolean migrated;
        /** A boss task whose size the player hasn't chosen yet (OSRS: 3-35 kills, asked by the master). */
        public boolean chooseAmount;
        /** The task put aside with "Task Storage" (category id), or null; the other stored* fields are its state. */
        public String storedTask;
        public int storedRemaining;
        public int storedAmount;
        public String storedMaster;
        public String storedRegion;
        public boolean storedChooseAmount;
        /** Mortimer's modifier on the current task (Mortimer.Modifier name) and its value, or null. */
        public String modifier;
        public int modifierValue;
        public String storedModifier;
        public int storedModifierValue;
        /** Mortimer's tasks completed (they unlock his modifiers and a third choice). */
        public int mortimerTasks;

        public boolean hasTask() {
            return task != null && remaining > 0;
        }

        public boolean hasStoredTask() {
            return storedTask != null && storedRemaining > 0;
        }

        /** Swaps the current task with the stored one (either may be empty). */
        public void swapStored() {
            String t = task;
            int r = remaining;
            int a = amount;
            String m = master;
            String g = region;
            boolean c = chooseAmount;
            String mod = modifier;
            int modValue = modifierValue;
            modifier = storedModifier;
            modifierValue = storedModifierValue;
            task = storedTask;
            remaining = storedRemaining;
            amount = storedAmount;
            master = storedMaster;
            region = storedRegion;
            chooseAmount = storedChooseAmount;
            storedTask = t != null && r > 0 ? t : null;
            storedRemaining = storedTask == null ? 0 : r;
            storedAmount = storedTask == null ? 0 : a;
            storedMaster = storedTask == null ? null : m;
            storedRegion = storedTask == null ? null : g;
            storedChooseAmount = storedTask != null && c;
            storedModifier = storedTask == null ? null : mod;
            storedModifierValue = storedTask == null ? 0 : modValue;
        }

        CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("Task", task == null ? "" : task);
            nbt.putInt("Remaining", remaining);
            nbt.putInt("Amount", amount);
            nbt.putString("Master", master == null ? "" : master);
            nbt.putString("Region", region == null ? "" : region);
            nbt.putInt("Streak", streak);
            nbt.putInt("WildyStreak", wildyStreak);
            nbt.putInt("Points", points);
            nbt.putInt("Completed", completed);
            nbt.putBoolean("Migrated", migrated);
            nbt.putBoolean("ChooseAmount", chooseAmount);
            nbt.putString("Modifier", modifier == null ? "" : modifier);
            nbt.putInt("ModifierValue", modifierValue);
            nbt.putInt("MortimerTasks", mortimerTasks);
            if (hasStoredTask()) {
                nbt.putString("StoredTask", storedTask);
                nbt.putInt("StoredRemaining", storedRemaining);
                nbt.putInt("StoredAmount", storedAmount);
                nbt.putString("StoredMaster", storedMaster == null ? "" : storedMaster);
                nbt.putString("StoredRegion", storedRegion == null ? "" : storedRegion);
                nbt.putBoolean("StoredChooseAmount", storedChooseAmount);
                nbt.putString("StoredModifier", storedModifier == null ? "" : storedModifier);
                nbt.putInt("StoredModifierValue", storedModifierValue);
            }
            nbt.put("Unlocks", list(unlocks));
            nbt.put("Blocked", list(blocked));
            return nbt;
        }

        static Profile fromNbt(CompoundTag nbt) {
            Profile p = new Profile();
            p.task = emptyToNull(nbt.getString("Task"));
            p.remaining = nbt.getInt("Remaining");
            p.amount = nbt.getInt("Amount");
            p.master = emptyToNull(nbt.getString("Master"));
            p.region = emptyToNull(nbt.getString("Region"));
            p.streak = nbt.getInt("Streak");
            p.wildyStreak = nbt.getInt("WildyStreak");
            p.points = nbt.getInt("Points");
            p.completed = nbt.getInt("Completed");
            p.migrated = nbt.getBoolean("Migrated");
            p.chooseAmount = nbt.getBoolean("ChooseAmount");
            p.storedTask = emptyToNull(nbt.getString("StoredTask"));
            p.storedRemaining = nbt.getInt("StoredRemaining");
            p.storedAmount = nbt.getInt("StoredAmount");
            p.storedMaster = emptyToNull(nbt.getString("StoredMaster"));
            p.storedRegion = emptyToNull(nbt.getString("StoredRegion"));
            p.storedChooseAmount = nbt.getBoolean("StoredChooseAmount");
            p.modifier = emptyToNull(nbt.getString("Modifier"));
            p.modifierValue = nbt.getInt("ModifierValue");
            p.mortimerTasks = nbt.getInt("MortimerTasks");
            p.storedModifier = emptyToNull(nbt.getString("StoredModifier"));
            p.storedModifierValue = nbt.getInt("StoredModifierValue");
            ListTag unlocks = nbt.getList("Unlocks", Tag.TAG_STRING);
            for (int i = 0; i < unlocks.size(); i++) {
                p.unlocks.add(unlocks.getString(i));
            }
            ListTag blocked = nbt.getList("Blocked", Tag.TAG_STRING);
            for (int i = 0; i < blocked.size(); i++) {
                p.blocked.add(blocked.getString(i));
            }
            return p;
        }

        private static ListTag list(Iterable<String> values) {
            ListTag list = new ListTag();
            for (String s : values) {
                list.add(StringTag.valueOf(s));
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

    /** Self-test hook ({@code com.runecraft.selftest}): an opaque copy of one player's profile, or null if there is none. */
    public Object selfTestSave(UUID uuid) {
        Profile p = profiles.get(uuid);
        return p == null ? null : p.toNbt();
    }

    /** Self-test hook: puts back a {@link #selfTestSave} copy; null drops the profile (a fresh one is made on use). */
    public void selfTestRestore(UUID uuid, Object saved) {
        profiles.remove(uuid);
        if (saved instanceof CompoundTag n) {
            profiles.put(uuid, Profile.fromNbt(n.copy()));
        }
    }

    /** Profiles change on every task kill; always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        profiles.forEach((uuid, p) -> all.put(uuid.toString(), p.toNbt()));
        nbt.put("Profiles", all);
        return nbt;
    }

    public static SlayerState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        SlayerState state = new SlayerState();
        CompoundTag all = nbt.getCompound("Profiles");
        for (String key : all.getAllKeys()) {
            try {
                state.profiles.put(UUID.fromString(key), Profile.fromNbt(all.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // Corrupt key: skip.
            }
        }
        return state;
    }

    public static SlayerState of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SlayerState::new, SlayerState::fromNbt, null), KEY);
    }
}
