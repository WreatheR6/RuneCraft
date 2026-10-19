package com.runecraft.content.raids;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-save storage for the raids module: completion counts, personal bests and purple counts per
 * player, the entrance each player last used (so anyone stranded in an instance after a restart is
 * sent back), and the instance chunks that are force-loaded.
 */
public final class RaidsState extends SavedData {
    public static final String KEY = "runecraft_raids";

    /** Per-player raid statistics, keyed by raid id. */
    public static final class Stats {
        public final Map<String, Integer> completions = new HashMap<>();
        public final Map<String, Integer> bestPoints = new HashMap<>();
        public final Map<String, Integer> purples = new HashMap<>();
        public BlockPos lastEntrance;
    }

    private final Map<UUID, Stats> players = new HashMap<>();
    private final Set<Long> forcedChunks = new LinkedHashSet<>();

    public static RaidsState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RaidsState::fromNbt, RaidsState::new, KEY);
    }

    public Stats stats(UUID player) {
        return players.computeIfAbsent(player, u -> new Stats());
    }

    public int completions(UUID player, String raid) {
        Stats s = players.get(player);
        return s == null ? 0 : s.completions.getOrDefault(raid, 0);
    }

    public int addCompletion(UUID player, String raid, int points, boolean purple) {
        Stats s = stats(player);
        int kc = s.completions.merge(raid, 1, Integer::sum);
        s.bestPoints.merge(raid, points, Math::max);
        if (purple) {
            s.purples.merge(raid, 1, Integer::sum);
        }
        setDirty();
        return kc;
    }

    public void setLastEntrance(UUID player, BlockPos pos) {
        stats(player).lastEntrance = pos;
        setDirty();
    }

    public Set<Long> forcedChunks() {
        return forcedChunks;
    }

    public void addForced(long chunk) {
        if (forcedChunks.add(chunk)) {
            setDirty();
        }
    }

    public void removeForced(long chunk) {
        if (forcedChunks.remove(chunk)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag all = new CompoundTag();
        players.forEach((uuid, s) -> {
            CompoundTag p = new CompoundTag();
            p.put("Completions", map(s.completions));
            p.put("Best", map(s.bestPoints));
            p.put("Purples", map(s.purples));
            if (s.lastEntrance != null) {
                p.putLong("Entrance", s.lastEntrance.asLong());
            }
            all.put(uuid.toString(), p);
        });
        nbt.put("Players", all);
        ListTag forced = new ListTag();
        for (long c : forcedChunks) {
            forced.add(LongTag.valueOf(c));
        }
        nbt.put("Forced", forced);
        return nbt;
    }

    private static CompoundTag map(Map<String, Integer> map) {
        CompoundTag c = new CompoundTag();
        map.forEach(c::putInt);
        return c;
    }

    private static void read(CompoundTag c, Map<String, Integer> into) {
        for (String k : c.getAllKeys()) {
            into.put(k, c.getInt(k));
        }
    }

    public static RaidsState fromNbt(CompoundTag nbt) {
        RaidsState state = new RaidsState();
        CompoundTag all = nbt.getCompound("Players");
        for (String key : all.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag p = all.getCompound(key);
                Stats s = state.stats(uuid);
                read(p.getCompound("Completions"), s.completions);
                read(p.getCompound("Best"), s.bestPoints);
                read(p.getCompound("Purples"), s.purples);
                if (p.contains("Entrance")) {
                    s.lastEntrance = BlockPos.of(p.getLong("Entrance"));
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        ListTag forced = nbt.getList("Forced", Tag.TAG_LONG);
        for (int i = 0; i < forced.size(); i++) {
            if (forced.get(i) instanceof LongTag l) {
                state.forcedChunks.add(l.getAsLong());
            }
        }
        return state;
    }
}
