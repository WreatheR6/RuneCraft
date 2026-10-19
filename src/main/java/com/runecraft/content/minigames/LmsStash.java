package com.runecraft.content.minigames;

import com.runecraft.content.systems.SysState;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.LegacyStacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Last Man Standing puts a player's own things aside for the game: inventory, armour, off-hand, the extra worn slots, skill
 * experience and boosts, prayer points and health. They are saved with the world (so a crash or restart mid-game still gives
 * everything back on the next login) and restored when the player leaves the island, wiping every LMS item they carried.
 */
public final class LmsStash extends SavedData {
    public static final String KEY = "runecraft_minigames_lms";

    private final Map<UUID, CompoundTag> stashed = new HashMap<>();

    public static LmsStash get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LmsStash::new, LmsStash::fromNbt, null), KEY);
    }

    public static boolean has(ServerPlayer player) {
        return get(player.getServer()).stashed.containsKey(player.getUUID());
    }

    /** Saves and clears the player's belongings and stats. Does nothing if they already have a stash. */
    public static void stash(ServerPlayer player) {
        LmsStash state = get(player.getServer());
        if (state.stashed.containsKey(player.getUUID())) {
            return;
        }
        CompoundTag c = new CompoundTag();
        c.put("Inventory", player.getInventory().save(new ListTag()));
        CompoundTag worn = new CompoundTag();
        SysState.SysPlayer sp = SysState.of(player);
        for (Map.Entry<Jewellery.Slot, ItemStack> e : sp.worn.entrySet()) {
            if (!e.getValue().isEmpty()) {
                worn.put(e.getKey().name(), e.getValue().saveOptional(player.registryAccess()));
            }
        }
        c.put("Worn", worn);
        PlayerData data = Players.data(player);
        c.putIntArray("Xp", data.skills().rawXp());
        c.putIntArray("Boosts", data.skills().rawBoosts());
        c.putInt("Prayer", data.prayerPoints());
        c.putInt("Spec", sp.specEnergy);
        c.putFloat("Health", player.getHealth());
        state.stashed.put(player.getUUID(), c);
        state.setDirty();
        player.getInventory().clearContent();
        sp.worn.clear();
    }

    /** Gives back everything saved by {@link #stash}, throwing away whatever the player holds now. */
    public static void restore(ServerPlayer player) {
        LmsStash state = get(player.getServer());
        CompoundTag c = state.stashed.remove(player.getUUID());
        if (c == null) {
            return;
        }
        state.setDirty();
        player.getInventory().clearContent();
        player.getInventory().load(LegacyStacks.upgrade(c.getList("Inventory", Tag.TAG_COMPOUND)));
        SysState.SysPlayer sp = SysState.of(player);
        sp.worn.clear();
        CompoundTag worn = c.getCompound("Worn");
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            if (worn.contains(slot.name())) {
                ItemStack stack = LegacyStacks.parse(player.registryAccess(), worn.getCompound(slot.name()));
                if (!stack.isEmpty()) {
                    sp.worn.put(slot, stack);
                }
            }
        }
        sp.specEnergy = c.getInt("Spec");
        PlayerData data = Players.data(player);
        data.skills().load(c.getIntArray("Xp"), c.getIntArray("Boosts"));
        data.activePrayers().clear();
        data.setPrayerPoints(c.getInt("Prayer"));
        XpService.updateMaxHealth(player);
        player.setHealth(Math.max(1f, Math.min(player.getMaxHealth(), c.getFloat("Health"))));
        ServerNetworking.sync(player);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        stashed.forEach((uuid, c) -> root.put(uuid.toString(), c));
        nbt.put("Stashed", root);
        return nbt;
    }

    public static LmsStash fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        LmsStash state = new LmsStash();
        CompoundTag root = nbt.getCompound("Stashed");
        for (String key : root.getAllKeys()) {
            try {
                state.stashed.put(UUID.fromString(key), root.getCompound(key));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
