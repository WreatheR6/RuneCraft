package com.runecraft.util;

import com.runecraft.RuneCraft;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

/** Inventory helpers keyed by catalog item ids. Coins live in the player's money pouch. */
public final class Inv {
    public static final String COINS = "coins";

    private Inv() {
    }

    public static Item item(String id) {
        return Registries.ITEM.get(RuneCraft.resolve(id));
    }

    public static ItemStack stack(String id, int count) {
        Item item = item(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public static String idOf(ItemStack stack) {
        net.minecraft.util.Identifier key = Registries.ITEM.getId(stack.getItem());
        return key.getNamespace().equals(RuneCraft.MOD_ID) ? key.getPath() : key.toString();
    }

    public static int count(PlayerEntity player, String id) {
        if (COINS.equals(id) && player instanceof ServerPlayerEntity sp) {
            long coins = Players.data(sp).coins();
            return (int) Math.min(Integer.MAX_VALUE, coins);
        }
        Item item = item(id);
        PlayerInventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                total += s.getCount();
            }
        }
        return total;
    }

    public static boolean has(PlayerEntity player, String id, int amount) {
        return count(player, id) >= amount;
    }

    /** Removes items (main inventory first, then armour/offhand); returns how many were removed. */
    public static int remove(PlayerEntity player, String id, int amount) {
        if (COINS.equals(id) && player instanceof ServerPlayerEntity sp) {
            PlayerData data = Players.data(sp);
            long take = Math.min(amount, data.coins());
            data.setCoins(data.coins() - take);
            return (int) take;
        }
        Item item = item(id);
        PlayerInventory inv = player.getInventory();
        int left = amount;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                int n = Math.min(left, s.getCount());
                s.decrement(n);
                left -= n;
            }
        }
        return amount - left;
    }

    /** Gives items, dropping any overflow at the player's feet. */
    public static void give(PlayerEntity player, String id, int amount) {
        if (COINS.equals(id) && player instanceof ServerPlayerEntity sp) {
            PlayerData data = Players.data(sp);
            data.setCoins(data.coins() + amount);
            return;
        }
        Item item = item(id);
        if (item == Items.AIR) {
            RuneCraft.LOGGER.warn("Unknown item {}", id);
            return;
        }
        int left = amount;
        while (left > 0) {
            int n = Math.min(left, item.getMaxCount());
            give(player, new ItemStack(item, n));
            left -= n;
        }
    }

    public static void give(PlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) {
            ItemEntity entity = player.dropItem(stack, false);
            if (entity != null) {
                entity.resetPickupDelay();
            }
        }
    }

    /** True if one more of {@code id} would fit in the inventory. */
    public static boolean hasRoomFor(PlayerEntity player, String id) {
        if (COINS.equals(id)) {
            return true;
        }
        Item item = item(id);
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack s = inv.main.get(i);
            if (s.isEmpty() || (s.isOf(item) && s.isStackable() && s.getCount() < s.getMaxCount() && !s.hasNbt())) {
                return true;
            }
        }
        return false;
    }
}
