package com.runecraft.content.bosses;

import com.runecraft.osrs.content.bosses.DoomDelves;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-save storage for Doom of Mokhaiotl delve runs: the last level each player completed, their stacks of demonic
 * charge and the loot held for them (OSRS keeps unclaimed loot in a chest in the lobby, so it survives leaving).
 */
final class DoomDelveState extends PersistentState {
    static final String KEY = "runecraft_bosses_doom";

    /** One player's run: the last level completed, demonic charge stacks and the loot waiting to be claimed. */
    static final class Run {
        int completed;
        int stacks;
        final List<DoomDelves.Loot> pending = new ArrayList<>();
    }

    private final Map<UUID, Run> runs = new HashMap<>();

    static DoomDelveState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(DoomDelveState::fromNbt, DoomDelveState::new, KEY);
    }

    Run get(UUID uuid) {
        return runs.get(uuid);
    }

    Run getOrCreate(UUID uuid) {
        Run r = runs.get(uuid);
        if (r == null) {
            r = new Run();
            runs.put(uuid, r);
            markDirty();
        }
        return r;
    }

    Run remove(UUID uuid) {
        Run r = runs.remove(uuid);
        if (r != null) {
            markDirty();
        }
        return r;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        runs.forEach((uuid, run) -> {
            NbtCompound c = new NbtCompound();
            c.putInt("Completed", run.completed);
            c.putInt("Stacks", run.stacks);
            c.putString("Loot", DoomDelves.encode(run.pending));
            nbt.put(uuid.toString(), c);
        });
        return nbt;
    }

    static DoomDelveState fromNbt(NbtCompound nbt) {
        DoomDelveState state = new DoomDelveState();
        for (String key : nbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound c = nbt.getCompound(key);
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
