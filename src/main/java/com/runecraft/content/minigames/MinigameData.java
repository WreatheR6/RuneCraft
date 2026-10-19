package com.runecraft.content.minigames;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
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
public final class MinigameData extends SavedData {
    public static final String KEY = "runecraft_minigames";

    private final Map<UUID, Map<String, Integer>> points = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> stats = new HashMap<>();
    private final Map<UUID, Set<String>> flags = new HashMap<>();

    public static MinigameData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MinigameData::new, MinigameData::fromNbt, null), KEY);
    }

    public static MinigameData of(ServerPlayer player) {
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
        setDirty();
    }

    /** Points balance for a key such as "pest_control" (no "pts:" prefix). */
    public int points(UUID player, String key) {
        return points.getOrDefault(player, Map.of()).getOrDefault(key, 0);
    }

    public void addPoints(UUID player, String key, int amount) {
        points.computeIfAbsent(player, k -> new HashMap<>()).merge(key, amount, Integer::sum);
        setDirty();
    }

    public int stat(UUID player, String key) {
        return stats.getOrDefault(player, Map.of()).getOrDefault(key, 0);
    }

    public int incrementStat(UUID player, String key) {
        int v = stats.computeIfAbsent(player, k -> new HashMap<>()).merge(key, 1, Integer::sum);
        setDirty();
        return v;
    }

    public void setStat(UUID player, String key, int value) {
        stats.computeIfAbsent(player, k -> new HashMap<>()).put(key, value);
        setDirty();
    }

    public boolean flag(UUID player, String flag) {
        return flags.getOrDefault(player, Set.of()).contains(flag);
    }

    public void setFlag(UUID player, String flag) {
        flags.computeIfAbsent(player, k -> new HashSet<>()).add(flag);
        setDirty();
    }

    private static CompoundTag writeMap(Map<UUID, Map<String, Integer>> map) {
        CompoundTag root = new CompoundTag();
        map.forEach((uuid, values) -> {
            CompoundTag c = new CompoundTag();
            values.forEach(c::putInt);
            root.put(uuid.toString(), c);
        });
        return root;
    }

    private static void readMap(CompoundTag root, Map<UUID, Map<String, Integer>> into) {
        for (String key : root.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag c = root.getCompound(key);
                Map<String, Integer> values = new HashMap<>();
                for (String k : c.getAllKeys()) {
                    values.put(k, c.getInt(k));
                }
                into.put(uuid, values);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.put("Points", writeMap(points));
        nbt.put("Stats", writeMap(stats));
        CompoundTag f = new CompoundTag();
        flags.forEach((uuid, set) -> {
            ListTag list = new ListTag();
            set.forEach(s -> list.add(StringTag.valueOf(s)));
            f.put(uuid.toString(), list);
        });
        nbt.put("Flags", f);
        return nbt;
    }

    public static MinigameData fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        MinigameData data = new MinigameData();
        readMap(nbt.getCompound("Points"), data.points);
        readMap(nbt.getCompound("Stats"), data.stats);
        CompoundTag f = nbt.getCompound("Flags");
        for (String key : f.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                ListTag list = f.getList(key, Tag.TAG_STRING);
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
