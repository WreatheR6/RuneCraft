package com.runecraft.player;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-save storage for all RuneCraft player data and which landmarks have been built. */
public final class PlayerDataState extends PersistentState {
    public static final String KEY = "runecraft";

    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Set<String> builtLandmarks = new HashSet<>();

    public PlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new PlayerData());
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public boolean isBuilt(String landmark) {
        return builtLandmarks.contains(landmark);
    }

    public void markBuilt(String landmark) {
        builtLandmarks.add(landmark);
        markDirty();
    }

    /** Player data changes constantly (XP, bank); always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, data) -> playersNbt.put(uuid.toString(), data.toNbt()));
        nbt.put("Players", playersNbt);
        NbtList built = new NbtList();
        for (String s : builtLandmarks) {
            built.add(NbtString.of(s));
        }
        nbt.put("Landmarks", built);
        return nbt;
    }

    public static PlayerDataState fromNbt(NbtCompound nbt) {
        PlayerDataState state = new PlayerDataState();
        NbtCompound playersNbt = nbt.getCompound("Players");
        for (String key : playersNbt.getKeys()) {
            try {
                PlayerData data = new PlayerData();
                data.fromNbt(playersNbt.getCompound(key));
                state.players.put(UUID.fromString(key), data);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        NbtList built = nbt.getList("Landmarks", NbtElement.STRING_TYPE);
        for (int i = 0; i < built.size(); i++) {
            state.builtLandmarks.add(built.getString(i));
        }
        return state;
    }
}
