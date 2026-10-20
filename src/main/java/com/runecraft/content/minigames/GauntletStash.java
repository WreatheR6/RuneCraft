package com.runecraft.content.minigames;

import com.runecraft.content.systems.SysState;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.LegacyStacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Gauntlet stores everything a player carries and wears for the run (OSRS: "Players cannot bring any items into the
 * Gauntlet") and removes pre-applied boosts. Unlike Last Man Standing, skill experience gained inside is real and kept. The
 * stash is saved with the world, so a crash mid-run still gives everything back on the next login; on the way out every
 * Gauntlet-only item is thrown away and anything else picked up (the Hunllef's drops) is kept.
 */
public final class GauntletStash extends SavedData {
    public static final String KEY = "runecraft_minigames_gauntlet";
    private static final Set<String> RUN_ITEMS = new HashSet<>(Gauntlet.runItems());

    private final Map<UUID, CompoundTag> stashed = new HashMap<>();
    /** Loot waiting in the lobby's reward chest (OSRS: claimed from the Reward Chest after leaving). */
    private final Map<UUID, ListTag> chest = new HashMap<>();

    public static GauntletStash get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(GauntletStash::new, GauntletStash::fromNbt, null), KEY);
    }

    public static boolean has(ServerPlayer player) {
        return get(player.getServer()).stashed.containsKey(player.getUUID());
    }

    /** Saves and clears the player's belongings and resets boosted or drained skills. Does nothing if already stashed. */
    public static void stash(ServerPlayer player) {
        GauntletStash state = get(player.getServer());
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
        state.stashed.put(player.getUUID(), c);
        state.setDirty();
        player.getInventory().clearContent();
        sp.worn.clear();
        // "Any pre-applied skill boosts will also be removed upon entry."
        var skills = Players.data(player).skills();
        for (com.runecraft.osrs.Skill s : com.runecraft.osrs.Skill.values()) {
            if (s != com.runecraft.osrs.Skill.HITPOINTS && s != com.runecraft.osrs.Skill.PRAYER) {
                skills.setBoost(s, 0);
            }
        }
        ServerNetworking.sync(player);
    }

    /**
     * Puts a run's loot into the player's reward chest in the lobby (saved with the world). Loot amounts (200 cosmic runes,
     * 1,000 mithril arrows) are saved as stacks of at most the item's stack size, the most a saved stack can hold.
     */
    public static void addToChest(ServerPlayer player, List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return;
        }
        GauntletStash state = get(player.getServer());
        ListTag list = state.chest.computeIfAbsent(player.getUUID(), u -> new ListTag());
        for (ItemStack s : loot) {
            for (int left = s.getCount(); left > 0; ) {
                int n = Math.min(left, s.getMaxStackSize());
                list.add(s.copyWithCount(n).save(player.registryAccess()));
                left -= n;
            }
        }
        state.setDirty();
    }

    /** The number of stacks waiting in the player's reward chest. */
    public static int chestSize(ServerPlayer player) {
        ListTag list = get(player.getServer()).chest.get(player.getUUID());
        return list == null ? 0 : list.size();
    }

    /** Hands the reward chest's loot to the player; returns how many stacks were claimed. */
    public static int claimChest(ServerPlayer player) {
        GauntletStash state = get(player.getServer());
        ListTag list = state.chest.remove(player.getUUID());
        if (list == null) {
            return 0;
        }
        state.setDirty();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = LegacyStacks.parse(player.registryAccess(), list.getCompound(i));
            if (!s.isEmpty()) {
                Inv.give(player, s);
            }
        }
        ServerNetworking.sync(player);
        return list.size();
    }

    /** Whether the player's stashed belongings (inventory and worn jewellery slots) hold this item. */
    public static boolean stashHas(ServerPlayer player, String item) {
        CompoundTag c = get(player.getServer()).stashed.get(player.getUUID());
        if (c == null) {
            return false;
        }
        ListTag inv = c.getList("Inventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = LegacyStacks.parse(player.registryAccess(), inv.getCompound(i));
            if (!s.isEmpty() && item.equals(Inv.idOf(s))) {
                return true;
            }
        }
        CompoundTag worn = c.getCompound("Worn");
        for (String key : worn.getAllKeys()) {
            ItemStack s = LegacyStacks.parse(player.registryAccess(), worn.getCompound(key));
            if (!s.isEmpty() && item.equals(Inv.idOf(s))) {
                return true;
            }
        }
        return false;
    }

    /** Whether an item exists only inside the Gauntlet. */
    public static boolean runItem(String id) {
        return RUN_ITEMS.contains(id);
    }

    /**
     * Gives back everything saved by {@link #stash}. Gauntlet items are thrown away; anything else the player holds now (the
     * Hunllef's drops, a pet) is kept and handed back after the stash.
     */
    public static void restore(ServerPlayer player) {
        GauntletStash state = get(player.getServer());
        CompoundTag c = state.stashed.remove(player.getUUID());
        if (c == null) {
            return;
        }
        state.setDirty();
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && !runItem(Inv.idOf(s))) {
                keep.add(s.copy());
            }
        }
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
        for (ItemStack s : keep) {
            Inv.give(player, s);
        }
        ServerNetworking.sync(player);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        stashed.forEach((uuid, c) -> root.put(uuid.toString(), c));
        nbt.put("Stashed", root);
        CompoundTag chests = new CompoundTag();
        chest.forEach((uuid, list) -> chests.put(uuid.toString(), list));
        nbt.put("Chest", chests);
        return nbt;
    }

    public static GauntletStash fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        GauntletStash state = new GauntletStash();
        CompoundTag root = nbt.getCompound("Stashed");
        for (String key : root.getAllKeys()) {
            try {
                state.stashed.put(UUID.fromString(key), root.getCompound(key));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        CompoundTag chests = nbt.getCompound("Chest");
        for (String key : chests.getAllKeys()) {
            try {
                state.chest.put(UUID.fromString(key), chests.getList(key, Tag.TAG_COMPOUND));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return state;
    }
}
