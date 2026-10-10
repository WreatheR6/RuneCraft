package com.runecraft.content.dungeons;

import com.runecraft.osrs.content.dungeons.BarrowsRun;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** World-save storage for each player's current Barrows run (see {@link BarrowsRun}). */
final class BarrowsState extends PersistentState {
    static final String KEY = "runecraft_barrows";

    private final Map<UUID, BarrowsRun> runs = new HashMap<>();

    static BarrowsState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(BarrowsState::fromNbt, BarrowsState::new, KEY);
    }

    /** The player's run, starting a fresh one if he has none. Call {@link #markDirty()} after changing it. */
    BarrowsRun run(UUID uuid) {
        BarrowsRun run = runs.get(uuid);
        if (run == null) {
            run = BarrowsRun.fresh(new Random());
            runs.put(uuid, run);
            markDirty();
        }
        return run;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        runs.forEach((uuid, run) -> {
            NbtCompound tag = new NbtCompound();
            tag.putInt("Killed", run.killedMask());
            tag.putInt("Summoned", run.summonedMask());
            tag.putInt("Tunnel", run.tunnel());
            tag.putLong("Seed", run.seed());
            tag.putBoolean("Hidden", run.hiddenSummoned());
            nbt.put(uuid.toString(), tag);
        });
        return nbt;
    }

    static BarrowsState fromNbt(NbtCompound nbt) {
        BarrowsState state = new BarrowsState();
        for (String key : nbt.getKeys()) {
            try {
                NbtCompound tag = nbt.getCompound(key);
                state.runs.put(UUID.fromString(key), BarrowsRun.of(tag.getInt("Killed"), tag.getInt("Summoned"),
                        tag.getInt("Tunnel"), tag.getLong("Seed"), tag.getBoolean("Hidden")));
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
