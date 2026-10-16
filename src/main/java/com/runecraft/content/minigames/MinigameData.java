package com.runecraft.content.minigames;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-save storage for the minigames module: per-player points balances (commendation points,
 * honour points, pizazz...), unlock flags (Bones to Peaches, Inferno cape sacrificed) and stats
 * (completion counts, personal bests).
 */
public final class MinigameData extends PersistentState {
    public static final String KEY = "runecraft_minigames";

    private final Map<UUID, Map<String, Integer>> points = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> stats = new HashMap<>();
    private final Map<UUID, Set<String>> flags = new HashMap<>();

    public static MinigameData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(MinigameData::fromNbt, MinigameData::new, KEY);
    }

    public static MinigameData of(ServerPlayerEntity player) {
        return get(player.getServer());
    }

    private record Saved(Map<String, Integer> points, Map<String, Integer> stats, Set<String> flags) {
    }

    /** Self-test hook ({@code com.runecraft.selftest}): an opaque copy of one player's points, stats and flags. */
    public Object selfTestSave(UUID player) {
        return new Saved(new HashMap<>(points.getOrDefault(player, Map.of())), new HashMap<>(stats.getOrDefault(player, Map.of())),
                new HashSet<>(flags.getOrDefault(player, Set.of())));
    }

    /** Self-test hook: puts back a {@link #selfTestSave} copy; null clears the player's points, stats and flags. */
    public void selfTestRestore(UUID player, Object saved) {
        points.remove(player);
        stats.remove(player);
        flags.remove(player);
        if (saved instanceof Saved s) {
            points.put(player, new HashMap<>(s.points()));
            stats.put(player, new HashMap<>(s.stats()));
            flags.put(player, new HashSet<>(s.flags()));
        }
        markDirty();
    }

    /** Points balance for a key such as "pest_control" (no "pts:" prefix). */
    public int points(UUID player, String key) {
        return points.getOrDefault(player, Map.of()).getOrDefault(key, 0);
    }

    public void addPoints(UUID player, String key, int amount) {
        points.computeIfAbsent(player, k -> new HashMap<>()).merge(key, amount, Integer::sum);
        markDirty();
    }

    public int stat(UUID player, String key) {
        return stats.getOrDefault(player, Map.of()).getOrDefault(key, 0);
    }

    public int incrementStat(UUID player, String key) {
        int v = stats.computeIfAbsent(player, k -> new HashMap<>()).merge(key, 1, Integer::sum);
        markDirty();
        return v;
    }

    public void setStat(UUID player, String key, int value) {
        stats.computeIfAbsent(player, k -> new HashMap<>()).put(key, value);
        markDirty();
    }

    public boolean flag(UUID player, String flag) {
        return flags.getOrDefault(player, Set.of()).contains(flag);
    }

    public void setFlag(UUID player, String flag) {
        flags.computeIfAbsent(player, k -> new HashSet<>()).add(flag);
        markDirty();
    }

    private static NbtCompound writeMap(Map<UUID, Map<String, Integer>> map) {
        NbtCompound root = new NbtCompound();
        map.forEach((uuid, values) -> {
            NbtCompound c = new NbtCompound();
            values.forEach(c::putInt);
            root.put(uuid.toString(), c);
        });
        return root;
    }

    private static void readMap(NbtCompound root, Map<UUID, Map<String, Integer>> into) {
        for (String key : root.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound c = root.getCompound(key);
                Map<String, Integer> values = new HashMap<>();
                for (String k : c.getKeys()) {
                    values.put(k, c.getInt(k));
                }
                into.put(uuid, values);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("Points", writeMap(points));
        nbt.put("Stats", writeMap(stats));
        NbtCompound f = new NbtCompound();
        flags.forEach((uuid, set) -> {
            NbtList list = new NbtList();
            set.forEach(s -> list.add(NbtString.of(s)));
            f.put(uuid.toString(), list);
        });
        nbt.put("Flags", f);
        return nbt;
    }

    public static MinigameData fromNbt(NbtCompound nbt) {
        MinigameData data = new MinigameData();
        readMap(nbt.getCompound("Points"), data.points);
        readMap(nbt.getCompound("Stats"), data.stats);
        NbtCompound f = nbt.getCompound("Flags");
        for (String key : f.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtList list = f.getList(key, NbtElement.STRING_TYPE);
                Set<String> set = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    set.add(list.getString(i));
                }
                data.flags.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return data;
    }
}
