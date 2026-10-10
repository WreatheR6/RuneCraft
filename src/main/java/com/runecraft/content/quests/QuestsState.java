package com.runecraft.content.quests;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

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
public final class QuestsState extends PersistentState {
    public static final String KEY = "runecraft_quests";

    private final Map<UUID, Set<String>> flags = new HashMap<>();
    private final Map<UUID, Map<String, String>> gear = new HashMap<>();

    public static QuestsState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(QuestsState::fromNbt, QuestsState::new, KEY);
    }

    public static QuestsState get(ServerPlayerEntity player) {
        return get(player.getServer());
    }

    public boolean hasFlag(UUID player, String flag) {
        return flags.getOrDefault(player, Set.of()).contains(flag);
    }

    public void setFlag(UUID player, String flag) {
        if (flags.computeIfAbsent(player, u -> new HashSet<>()).add(flag)) {
            markDirty();
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
        markDirty();
        return old;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound flagsNbt = new NbtCompound();
        flags.forEach((uuid, set) -> {
            NbtList list = new NbtList();
            set.forEach(f -> list.add(NbtString.of(f)));
            flagsNbt.put(uuid.toString(), list);
        });
        nbt.put("Flags", flagsNbt);
        NbtCompound gearNbt = new NbtCompound();
        gear.forEach((uuid, slots) -> {
            NbtCompound s = new NbtCompound();
            slots.forEach(s::putString);
            gearNbt.put(uuid.toString(), s);
        });
        nbt.put("Gear", gearNbt);
        return nbt;
    }

    public static QuestsState fromNbt(NbtCompound nbt) {
        QuestsState state = new QuestsState();
        NbtCompound flagsNbt = nbt.getCompound("Flags");
        for (String key : flagsNbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtList list = flagsNbt.getList(key, NbtElement.STRING_TYPE);
                Set<String> set = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    set.add(list.getString(i));
                }
                state.flags.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        NbtCompound gearNbt = nbt.getCompound("Gear");
        for (String key : gearNbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound s = gearNbt.getCompound(key);
                Map<String, String> slots = new LinkedHashMap<>();
                for (String slot : s.getKeys()) {
                    slots.put(slot, s.getString(slot));
                }
                state.gear.put(uuid, slots);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
