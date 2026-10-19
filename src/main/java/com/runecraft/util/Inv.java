package com.runecraft.util;

import com.runecraft.RuneCraft;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Inventory helpers keyed by catalog item ids. Coins live in the player's money pouch. */
public final class Inv {
    public static final String COINS = "coins";

    private Inv() {
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.get(RuneCraft.resolve(id));
    }

    public static ItemStack stack(String id, int count) {
        Item item = item(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public static String idOf(ItemStack stack) {
        net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals(RuneCraft.MOD_ID) ? key.getPath() : key.toString();
    }

    public static int count(Player player, String id) {
        if (COINS.equals(id) && player instanceof ServerPlayer sp) {
            long coins = Players.data(sp).coins();
            return (int) Math.min(Integer.MAX_VALUE, coins);
        }
        Item item = item(id);
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                total += s.getCount();
            }
        }
        return total;
    }

    public static boolean has(Player player, String id, int amount) {
        return count(player, id) >= amount;
    }

    /** Removes items (main inventory first, then armour/offhand); returns how many were removed. */
    public static int remove(Player player, String id, int amount) {
        if (COINS.equals(id) && player instanceof ServerPlayer sp) {
            PlayerData data = Players.data(sp);
            long take = Math.min(amount, data.coins());
            data.setCoins(data.coins() - take);
            return (int) take;
        }
        Item item = item(id);
        Inventory inv = player.getInventory();
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int n = Math.min(left, s.getCount());
                s.shrink(n);
                left -= n;
            }
        }
        return amount - left;
    }

    /** Gives items, dropping any overflow at the player's feet. */
    public static void give(Player player, String id, int amount) {
        if (COINS.equals(id) && player instanceof ServerPlayer sp) {
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
            int n = Math.min(left, item.getDefaultMaxStackSize());
            give(player, new ItemStack(item, n));
            left -= n;
        }
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            drop(player, stack, true);
        }
    }

    /**
     * Hands {@code stack} to {@code sink} one full stack at a time, for spawning item entities: one holding more than
     * 99 items can't be saved with its chunk.
     */
    public static void splitStacks(ItemStack stack, Consumer<ItemStack> sink) {
        for (int left = stack.getCount(); left > 0; ) {
            int n = Math.min(left, stack.getMaxStackSize());
            sink.accept(stack.copyWithCount(n));
            left -= n;
        }
    }

    /** {@link Entity#spawnAtLocation(ItemStack)}, one item entity per full stack. */
    public static void spawnAtLocation(Entity entity, ItemStack stack) {
        splitStacks(stack, entity::spawnAtLocation);
    }

    /** {@link Player#drop(ItemStack, boolean)} at the player's feet, one item entity per full stack. */
    public static void drop(Player player, ItemStack stack, boolean noPickUpDelay) {
        splitStacks(stack, s -> {
            ItemEntity entity = player.drop(s, false);
            if (entity != null && noPickUpDelay) {
                entity.setNoPickUpDelay();
            }
        });
    }

    /** True if one more of {@code id} would fit in the inventory. */
    public static boolean hasRoomFor(Player player, String id) {
        if (COINS.equals(id)) {
            return true;
        }
        Item item = item(id);
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || (s.is(item) && s.isStackable() && s.getCount() < s.getMaxStackSize() && s.getComponentsPatch().isEmpty())) {
                return true;
            }
        }
        return false;
    }
}
