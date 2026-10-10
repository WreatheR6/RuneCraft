package com.runecraft.content.islands;

import com.runecraft.osrs.content.islands.Kingdom;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** World-save storage for every player's Kingdom of Miscellania and the day it was last updated. */
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
            c.putString("work", k.work().name());
            c.putLong("day", days.getOrDefault(uuid, -1L));
            NbtCompound stock = new NbtCompound();
            k.stock().forEach(stock::putInt);
            c.put("stock", stock);
            nbt.put(uuid.toString(), c);
        });
        return nbt;
    }

    static IslandsKingdomState fromNbt(NbtCompound nbt) {
        IslandsKingdomState state = new IslandsKingdomState();
        for (String key : nbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound c = nbt.getCompound(key);
                Kingdom.Work work = Kingdom.Work.WOODCUTTING;
                try {
                    work = Kingdom.Work.valueOf(c.getString("work"));
                } catch (IllegalArgumentException ignored) {
                    // Unknown allocation: keep the default.
                }
                Map<String, Integer> stock = new LinkedHashMap<>();
                NbtCompound s = c.getCompound("stock");
                for (String item : s.getKeys()) {
                    stock.put(item, s.getInt(item));
                }
                state.kingdoms.put(uuid, new Kingdom.State(c.getLong("coffer"), c.getInt("approval"), work, stock));
                state.days.put(uuid, c.getLong("day"));
            } catch (IllegalArgumentException ignored) {
                // Not a player entry.
            }
        }
        return state;
    }
}
