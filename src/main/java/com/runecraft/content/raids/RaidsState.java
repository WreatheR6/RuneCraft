package com.runecraft.content.raids;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

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
public final class RaidsState extends PersistentState {
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
        return server.getOverworld().getPersistentStateManager().getOrCreate(RaidsState::fromNbt, RaidsState::new, KEY);
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
        markDirty();
        return kc;
    }

    public void setLastEntrance(UUID player, BlockPos pos) {
        stats(player).lastEntrance = pos;
        markDirty();
    }

    public Set<Long> forcedChunks() {
        return forcedChunks;
    }

    public void addForced(long chunk) {
        if (forcedChunks.add(chunk)) {
            markDirty();
        }
    }

    public void removeForced(long chunk) {
        if (forcedChunks.remove(chunk)) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        players.forEach((uuid, s) -> {
            NbtCompound p = new NbtCompound();
            p.put("Completions", map(s.completions));
            p.put("Best", map(s.bestPoints));
            p.put("Purples", map(s.purples));
            if (s.lastEntrance != null) {
                p.putLong("Entrance", s.lastEntrance.asLong());
            }
            all.put(uuid.toString(), p);
        });
        nbt.put("Players", all);
        NbtList forced = new NbtList();
        for (long c : forcedChunks) {
            forced.add(NbtLong.of(c));
        }
        nbt.put("Forced", forced);
        return nbt;
    }

    private static NbtCompound map(Map<String, Integer> map) {
        NbtCompound c = new NbtCompound();
        map.forEach(c::putInt);
        return c;
    }

    private static void read(NbtCompound c, Map<String, Integer> into) {
        for (String k : c.getKeys()) {
            into.put(k, c.getInt(k));
        }
    }

    public static RaidsState fromNbt(NbtCompound nbt) {
        RaidsState state = new RaidsState();
        NbtCompound all = nbt.getCompound("Players");
        for (String key : all.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound p = all.getCompound(key);
                Stats s = state.stats(uuid);
                read(p.getCompound("Completions"), s.completions);
                read(p.getCompound("Best"), s.bestPoints);
                read(p.getCompound("Purples"), s.purples);
                if (p.contains("Entrance")) {
                    s.lastEntrance = BlockPos.fromLong(p.getLong("Entrance"));
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        NbtList forced = nbt.getList("Forced", NbtElement.LONG_TYPE);
        for (int i = 0; i < forced.size(); i++) {
            if (forced.get(i) instanceof NbtLong l) {
                state.forcedChunks.add(l.longValue());
            }
        }
        return state;
    }
}
