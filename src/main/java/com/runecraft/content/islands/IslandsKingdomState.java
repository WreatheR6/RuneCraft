package com.runecraft.content.islands;

import com.runecraft.osrs.content.islands.Kingdom;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-save storage for every player's Kingdom of Miscellania and the (UTC) day it was last
 * advanced to. Saves from before the allocation rework (one "work" for all ten subjects, items
 * already produced in "stock", Minecraft-day clock) load with ten subjects on that work, the stock
 * kept for collection and the clock restarted today.
 */
final class IslandsKingdomState extends PersistentState {
    static final String KEY = "runecraft_islands";

    private final Map<UUID, Kingdom.State> kingdoms = new HashMap<>();
    private final Map<UUID, Long> days = new HashMap<>();

    static IslandsKingdomState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(IslandsKingdomState::fromNbt, IslandsKingdomState::new, KEY);
    }

    Kingdom.State kingdom(UUID uuid) {
        return kingdoms.getOrDefault(uuid, Kingdom.State.fresh());
    }

    /** The day the kingdom was last advanced to, or -1 if never. */
    long day(UUID uuid) {
        return days.getOrDefault(uuid, -1L);
    }

    void set(UUID uuid, Kingdom.State state, long day) {
        kingdoms.put(uuid, state);
        days.put(uuid, day);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        kingdoms.forEach((uuid, k) -> {
            NbtCompound c = new NbtCompound();
            c.putLong("coffer", k.coffer());
            c.putInt("approval", k.approval());
            c.putLong("points", k.points());
            c.putString("clock", "utc");
            NbtCompound workers = new NbtCompound();
            k.workers().forEach((w, n) -> workers.putInt(w.name(), n));
            c.put("workers", workers);
            c.putLong("day", days.getOrDefault(uuid, -1L));
            NbtCompound stock = new NbtCompound();
            k.stock().forEach(stock::putInt);
            c.put("stock", stock);
            nbt.put(uuid.toString(), c);
        });
        return nbt;
    }

    private static Kingdom.Work work(String name) {
        try {
            return Kingdom.Work.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static IslandsKingdomState fromNbt(NbtCompound nbt) {
        IslandsKingdomState state = new IslandsKingdomState();
        for (String key : nbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound c = nbt.getCompound(key);
                Map<Kingdom.Work, Integer> workers = new EnumMap<>(Kingdom.Work.class);
                if (c.contains("workers")) {
                    NbtCompound w = c.getCompound("workers");
                    for (String name : w.getKeys()) {
                        Kingdom.Work work = work(name);
                        if (work != null) {
                            workers.put(work, w.getInt(name));
                        }
                    }
                } else {
                    Kingdom.Work old = work(c.getString("work"));
                    workers.put(old == null ? Kingdom.Work.WOODCUTTING : old, Kingdom.WORKERS);
                }
                Map<String, Integer> stock = new LinkedHashMap<>();
                NbtCompound s = c.getCompound("stock");
                for (String item : s.getKeys()) {
                    stock.put(item, s.getInt(item));
                }
                boolean utc = c.getString("clock").equals("utc");
                state.kingdoms.put(uuid, new Kingdom.State(c.getLong("coffer"), c.getInt("approval"), workers,
                        c.getLong("points"), stock));
                state.days.put(uuid, utc ? c.getLong("day") : IslandsKingdomService.today());
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
