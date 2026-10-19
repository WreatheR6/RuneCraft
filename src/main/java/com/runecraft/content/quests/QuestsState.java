package com.runecraft.content.quests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-save storage for the quests module: per-player quest flags (side objectives such as
 * "Manni's vote") and accessories worn in the module's virtual gear slots (gloves, cape, amulet).
 */
public final class QuestsState extends SavedData {
    public static final String KEY = "runecraft_quests";

    private final Map<UUID, Set<String>> flags = new HashMap<>();
    private final Map<UUID, Map<String, String>> gear = new HashMap<>();
    /** Kill counters of multi-kill objectives ("desert_treasure:3:4:ice_troll" -> 2). */
    private final Map<UUID, Map<String, Integer>> kills = new HashMap<>();

    public static QuestsState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(QuestsState::fromNbt, QuestsState::new, KEY);
    }

    public static QuestsState get(ServerPlayer player) {
        return get(player.getServer());
    }

    private record Saved(Set<String> flags, Map<String, String> gear, Map<String, Integer> kills) {
    }

    /** Self-test hook ({@code com.runecraft.selftest}): an opaque copy of one player's flags, gear and kill counters. */
    public Object selfTestSave(UUID player) {
        return new Saved(new HashSet<>(flags.getOrDefault(player, Set.of())), new LinkedHashMap<>(gear.getOrDefault(player, Map.of())),
                new HashMap<>(kills.getOrDefault(player, Map.of())));
    }

    /** Self-test hook: puts back a {@link #selfTestSave} copy; null clears the player's flags, gear and kill counters. */
    public void selfTestRestore(UUID player, Object saved) {
        flags.remove(player);
        gear.remove(player);
        kills.remove(player);
        if (saved instanceof Saved s) {
            flags.put(player, new HashSet<>(s.flags()));
            gear.put(player, new LinkedHashMap<>(s.gear()));
            kills.put(player, new HashMap<>(s.kills()));
        }
        setDirty();
    }

    public boolean hasFlag(UUID player, String flag) {
        return flags.getOrDefault(player, Set.of()).contains(flag);
    }

    public void setFlag(UUID player, String flag) {
        if (flags.computeIfAbsent(player, u -> new HashSet<>()).add(flag)) {
            setDirty();
        }
    }

    /** Clears a quest flag (a failed puzzle run resets its progress flags). */
    public void clearFlag(UUID player, String flag) {
        Set<String> s = flags.get(player);
        if (s != null && s.remove(flag)) {
            setDirty();
        }
    }

    /** Kills counted so far towards a multi-kill objective. */
    public int kills(UUID player, String key) {
        return kills.getOrDefault(player, Map.of()).getOrDefault(key, 0);
    }

    /** Counts one more kill towards an objective; returns the new total. */
    public int addKill(UUID player, String key) {
        int n = kills.computeIfAbsent(player, u -> new HashMap<>()).merge(key, 1, Integer::sum);
        setDirty();
        return n;
    }

    /** Clears an objective's kill counter (once it has fired). */
    public void clearKills(UUID player, String key) {
        Map<String, Integer> m = kills.get(player);
        if (m != null && m.remove(key) != null) {
            setDirty();
        }
    }

    /** Item worn in a gear slot, or null. */
    public String worn(UUID player, String slot) {
        return gear.getOrDefault(player, Map.of()).get(slot);
    }

    public Map<String, String> wornAll(UUID player) {
        return gear.getOrDefault(player, Map.of());
    }

    /** Puts an item in a slot (null clears it); returns the previously worn item. */
    public String wear(UUID player, String slot, String item) {
        Map<String, String> slots = gear.computeIfAbsent(player, u -> new LinkedHashMap<>());
        String old = item == null ? slots.remove(slot) : slots.put(slot, item);
        setDirty();
        return old;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag flagsNbt = new CompoundTag();
        flags.forEach((uuid, set) -> {
            ListTag list = new ListTag();
            set.forEach(f -> list.add(StringTag.valueOf(f)));
            flagsNbt.put(uuid.toString(), list);
        });
        nbt.put("Flags", flagsNbt);
        CompoundTag gearNbt = new CompoundTag();
        gear.forEach((uuid, slots) -> {
            CompoundTag s = new CompoundTag();
            slots.forEach(s::putString);
            gearNbt.put(uuid.toString(), s);
        });
        nbt.put("Gear", gearNbt);
        CompoundTag killsNbt = new CompoundTag();
        kills.forEach((uuid, counters) -> {
            CompoundTag c = new CompoundTag();
            counters.forEach(c::putInt);
            killsNbt.put(uuid.toString(), c);
        });
        nbt.put("Kills", killsNbt);
        return nbt;
    }

    public static QuestsState fromNbt(CompoundTag nbt) {
        QuestsState state = new QuestsState();
        CompoundTag flagsNbt = nbt.getCompound("Flags");
        for (String key : flagsNbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                ListTag list = flagsNbt.getList(key, Tag.TAG_STRING);
                Set<String> set = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    set.add(list.getString(i));
                }
                state.flags.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        CompoundTag gearNbt = nbt.getCompound("Gear");
        for (String key : gearNbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag s = gearNbt.getCompound(key);
                Map<String, String> slots = new LinkedHashMap<>();
                for (String slot : s.getAllKeys()) {
                    slots.put(slot, s.getString(slot));
                }
                state.gear.put(uuid, slots);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        CompoundTag killsNbt = nbt.getCompound("Kills");
        for (String key : killsNbt.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag c = killsNbt.getCompound(key);
                Map<String, Integer> counters = new HashMap<>();
                for (String objective : c.getAllKeys()) {
                    counters.put(objective, c.getInt(objective));
                }
                state.kills.put(uuid, counters);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
