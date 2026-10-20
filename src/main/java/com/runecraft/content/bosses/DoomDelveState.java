package com.runecraft.content.bosses;

import com.runecraft.osrs.content.bosses.DoomDelves;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-save storage for Doom of Mokhaiotl delve runs: the last level each player completed, their stacks of demonic
 * charge, the loot their run holds in the burrow hole, where the run's last level was fought, and the loot left unclaimed
 * in the lobby chest (OSRS: leaving without claiming ends the run but keeps the loot in a chest by the scoreboard).
 */
final class DoomDelveState extends SavedData {
    static final String KEY = "runecraft_bosses_doom";

    /** One player's run and chest. */
    static final class Run {
        /** The last level completed on the current run (0: no run going; the next summon starts delve 1). */
        int completed;
        int stacks;
        /** Loot held in the burrow hole by the current run (lost on a death in the delve). */
        final List<DoomDelves.Loot> pending = new ArrayList<>();
        /** Loot left unclaimed when a run ended by leaving (kept until claimed; a death doesn't touch it). */
        final List<DoomDelves.Loot> chest = new ArrayList<>();
        /** Where the run's last level was fought (packed block position) and its dimension, or null. */
        long at;
        String dimension;

        boolean active() {
            return completed > 0;
        }

        boolean empty() {
            return completed <= 0 && pending.isEmpty() && chest.isEmpty();
        }
    }

    private final Map<UUID, Run> runs = new HashMap<>();

    static DoomDelveState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(DoomDelveState::new, DoomDelveState::fromNbt, null), KEY);
    }

    Run get(UUID uuid) {
        return runs.get(uuid);
    }

    Map<UUID, Run> all() {
        return runs;
    }

    Run getOrCreate(UUID uuid) {
        Run r = runs.get(uuid);
        if (r == null) {
            r = new Run();
            runs.put(uuid, r);
            setDirty();
        }
        return r;
    }

    Run remove(UUID uuid) {
        Run r = runs.remove(uuid);
        if (r != null) {
            setDirty();
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        runs.forEach((uuid, run) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("Completed", run.completed);
            c.putInt("Stacks", run.stacks);
            c.putString("Loot", DoomDelves.encode(run.pending));
            c.putString("Chest", DoomDelves.encode(run.chest));
            c.putLong("At", run.at);
            if (run.dimension != null) {
                c.putString("Dimension", run.dimension);
            }
            nbt.put(uuid.toString(), c);
        });
        return nbt;
    }

    static DoomDelveState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        DoomDelveState state = new DoomDelveState();
        for (String key : nbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag c = nbt.getCompound(key);
                Run run = new Run();
                run.completed = c.getInt("Completed");
                run.stacks = c.getInt("Stacks");
                run.pending.addAll(DoomDelves.decode(c.getString("Loot")));
                run.chest.addAll(DoomDelves.decode(c.getString("Chest")));
                run.at = c.getLong("At");
                run.dimension = c.contains("Dimension") ? c.getString("Dimension") : null;
                state.runs.put(uuid, run);
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
