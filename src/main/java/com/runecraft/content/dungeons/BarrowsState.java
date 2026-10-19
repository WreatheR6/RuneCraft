package com.runecraft.content.dungeons;

import com.runecraft.osrs.content.dungeons.BarrowsRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** World-save storage for each player's current Barrows run (see {@link BarrowsRun}). */
final class BarrowsState extends SavedData {
    static final String KEY = "runecraft_barrows";

    private final Map<UUID, BarrowsRun> runs = new HashMap<>();

    static BarrowsState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(BarrowsState::fromNbt, BarrowsState::new, KEY);
    }

    /** The player's run, starting a fresh one if he has none. Call {@link #setDirty()} after changing it. */
    BarrowsRun run(UUID uuid) {
        BarrowsRun run = runs.get(uuid);
        if (run == null) {
            run = BarrowsRun.fresh(new Random());
            runs.put(uuid, run);
            setDirty();
        }
        return run;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        runs.forEach((uuid, run) -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Killed", run.killedMask());
            tag.putInt("Summoned", run.summonedMask());
            tag.putInt("Tunnel", run.tunnel());
            tag.putLong("Seed", run.seed());
            tag.putBoolean("Hidden", run.hiddenSummoned());
            tag.putInt("Solved", run.solvedMask());
            tag.putInt("Monsters", run.monsterKills());
            tag.putInt("MonsterLevels", run.monsterLevels());
            nbt.put(uuid.toString(), tag);
        });
        return nbt;
    }

    static BarrowsState fromNbt(CompoundTag nbt) {
        BarrowsState state = new BarrowsState();
        for (String key : nbt.getAllKeys()) {
            try {
                CompoundTag tag = nbt.getCompound(key);
                state.runs.put(UUID.fromString(key), BarrowsRun.of(tag.getInt("Killed"), tag.getInt("Summoned"),
                        tag.getInt("Tunnel"), tag.getLong("Seed"), tag.getBoolean("Hidden"),
                        tag.getInt("Solved"), tag.getInt("Monsters"), tag.getInt("MonsterLevels")));
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
