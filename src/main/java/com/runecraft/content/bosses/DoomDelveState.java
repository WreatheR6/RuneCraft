package com.runecraft.content.bosses;

import com.runecraft.osrs.content.bosses.DoomDelves;
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
 * charge and the loot held for them (OSRS keeps unclaimed loot in a chest in the lobby, so it survives leaving).
 */
final class DoomDelveState extends SavedData {
    static final String KEY = "runecraft_bosses_doom";

    /** One player's run: the last level completed, demonic charge stacks and the loot waiting to be claimed. */
    static final class Run {
        int completed;
        int stacks;
        final List<DoomDelves.Loot> pending = new ArrayList<>();
    }

    private final Map<UUID, Run> runs = new HashMap<>();

    static DoomDelveState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(DoomDelveState::fromNbt, DoomDelveState::new, KEY);
    }

    Run get(UUID uuid) {
        return runs.get(uuid);
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
    public CompoundTag save(CompoundTag nbt) {
        runs.forEach((uuid, run) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("Completed", run.completed);
            c.putInt("Stacks", run.stacks);
            c.putString("Loot", DoomDelves.encode(run.pending));
            nbt.put(uuid.toString(), c);
        });
        return nbt;
    }

    static DoomDelveState fromNbt(CompoundTag nbt) {
        DoomDelveState state = new DoomDelveState();
        for (String key : nbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag c = nbt.getCompound(key);
                Run run = new Run();
                run.completed = c.getInt("Completed");
                run.stacks = c.getInt("Stacks");
                run.pending.addAll(DoomDelves.decode(c.getString("Loot")));
                state.runs.put(uuid, run);
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
