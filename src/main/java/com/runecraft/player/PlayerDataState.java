package com.runecraft.player;

import com.runecraft.osrs.world.BuiltChunks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** World-save storage for all RuneCraft player data and which landmarks have been built. */
public final class PlayerDataState extends SavedData {
    public static final String KEY = "runecraft";

    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Set<String> builtLandmarks = new HashSet<>();
    /** Placed structure shares, one long set per structure (was one "s:id:cx,cz" string each). */
    private final BuiltChunks builtStructures = new BuiltChunks();

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
        setDirty();
    }

    /** True if this structure's share of the chunk has been placed. */
    public boolean isStructureBuilt(String structureId, int chunkX, int chunkZ) {
        return builtStructures.isBuilt(structureId, chunkX, chunkZ);
    }

    public void markStructureBuilt(String structureId, int chunkX, int chunkZ) {
        builtStructures.markBuilt(structureId, chunkX, chunkZ);
        setDirty();
    }

    /** Player data changes constantly (XP, bank); always save. */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> playersNbt.put(uuid.toString(), data.toNbt()));
        nbt.put("Players", playersNbt);
        ListTag built = new ListTag();
        for (String s : builtLandmarks) {
            built.add(StringTag.valueOf(s));
        }
        nbt.put("Landmarks", built);
        CompoundTag structures = new CompoundTag();
        for (String id : builtStructures.structures()) {
            structures.putLongArray(id, builtStructures.chunkKeys(id));
        }
        nbt.put("StructureChunks", structures);
        return nbt;
    }

    public static PlayerDataState fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        PlayerDataState state = new PlayerDataState();
        CompoundTag playersNbt = nbt.getCompound("Players");
        for (String key : playersNbt.getAllKeys()) {
            try {
                PlayerData data = new PlayerData();
                data.fromNbt(playersNbt.getCompound(key), registries);
                state.players.put(UUID.fromString(key), data);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        ListTag built = nbt.getList("Landmarks", Tag.TAG_STRING);
        for (int i = 0; i < built.size(); i++) {
            String key = built.getString(i);
            if (!state.builtStructures.parseLegacyKey(key)) { // older saves: one string per structure share
                state.builtLandmarks.add(key);
            }
        }
        CompoundTag structures = nbt.getCompound("StructureChunks");
        for (String id : structures.getAllKeys()) {
            state.builtStructures.addAll(id, structures.getLongArray(id));
        }
        return state;
    }
}
