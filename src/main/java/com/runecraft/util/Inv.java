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
import net.minecraft.world.phys.AABB;

/**
 * Inventory helpers keyed by catalog item ids. Coins and tokkul live in pouches ({@link #pouched}): OSRS stacks either in one
 * inventory slot up to 2,147,483,647, but a Minecraft stack holds 64 (and a saved one at most 99), so a Fight Cave's 8,032
 * tokkul would need 126 slots.
 */
public final class Inv {
    public static final String COINS = "coins";
    public static final String TOKKUL = "tokkul";

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

    /**
     * Whether {@code id} is a currency kept in a pouch in {@link PlayerData} instead of 64-item stacks: coins (the money pouch)
     * and tokkul. Their items in the inventory are moved into the pouch every tick, and with a full pack a pouch that holds some
     * picks more up off the ground ({@link #pickUpPouched}, from {@code PlayerEvents}). Pouches stay with the player on death
     * (see {@code PrayerEffectsService.keepItems}: OSRS never drops untradeable tokkul for a killer).
     */
    public static boolean pouched(String id) {
        return COINS.equals(id) || TOKKUL.equals(id);
    }

    /** The pouch balance of a {@link #pouched} currency. */
    public static long pouch(PlayerData data, String id) {
        return COINS.equals(id) ? data.coins() : data.tokkul();
    }

    /** Sets the pouch balance of a {@link #pouched} currency (clamped to 0..2,147,483,647, OSRS's largest stack). */
    public static void setPouch(PlayerData data, String id, long amount) {
        if (COINS.equals(id)) {
            data.setCoins(amount);
        } else {
            data.setTokkul(amount);
        }
    }

    /** How many of {@code id} the player holds: a pouch (coins, tokkul) plus the inventory's stacks. */
    public static int count(Player player, String id) {
        long pouch = 0;
        if (pouched(id) && player instanceof ServerPlayer sp) {
            pouch = pouch(Players.data(sp), id);
        }
        return (int) Math.min(Integer.MAX_VALUE, pouch + stacked(player, id));
    }

    /** The items of {@code id} held in inventory slots (armour and offhand included), pouches aside. */
    private static long stacked(Player player, String id) {
        Item item = item(id);
        Inventory inv = player.getInventory();
        long total = 0;
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

    /** Removes items (a pouch first, then the main inventory, then armour/offhand); returns how many were removed. */
    public static int remove(Player player, String id, int amount) {
        int left = amount;
        if (pouched(id) && player instanceof ServerPlayer sp && left > 0) {
            PlayerData data = Players.data(sp);
            long take = Math.min(left, pouch(data, id));
            setPouch(data, id, pouch(data, id) - take);
            left -= (int) take;
        }
        Item item = item(id);
        Inventory inv = player.getInventory();
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

    /** Gives items (coins and tokkul into their pouch), dropping any overflow at the player's feet. */
    public static void give(Player player, String id, int amount) {
        give(player, id, amount, null);
    }

    /**
     * {@link #give(Player, String, int)}; {@code dropped} (nullable) is handed each item entity the overflow lands in at the
     * player's feet (to mark it, e.g. a minigame reward).
     */
    public static void give(Player player, String id, int amount, Consumer<ItemEntity> dropped) {
        int left = amount;
        if (pouched(id) && player instanceof ServerPlayer sp && left > 0) {
            left -= toPouch(sp, id, left);
        }
        if (left <= 0) {
            return;
        }
        Item item = item(id);
        if (item == Items.AIR) {
            RuneCraft.LOGGER.warn("Unknown item {}", id);
            return;
        }
        while (left > 0) {
            int n = Math.min(left, item.getDefaultMaxStackSize());
            give(player, new ItemStack(item, n), dropped);
            left -= n;
        }
    }

    /** Gives a stack (plain coins or tokkul into their pouch), dropping any overflow at the player's feet. */
    public static void give(Player player, ItemStack stack) {
        give(player, stack, null);
    }

    /** {@link #give(Player, ItemStack)}; {@code dropped} (nullable) is handed each item entity the overflow lands in. */
    public static void give(Player player, ItemStack stack, Consumer<ItemEntity> dropped) {
        ItemStack rest = insert(player, stack);
        if (!rest.isEmpty()) {
            drop(player, rest, true, dropped);
        }
    }

    /**
     * Puts a stack in the inventory (plain coins or tokkul in their pouch) and returns what didn't fit, empty when all of it
     * did. {@code stack} is used up as it goes in.
     */
    public static ItemStack insert(Player player, ItemStack stack) {
        if (!stack.isEmpty() && player instanceof ServerPlayer sp && stack.getComponentsPatch().isEmpty() && pouched(idOf(stack))) {
            stack.shrink(toPouch(sp, idOf(stack), stack.getCount()));
        }
        if (!stack.isEmpty()) {
            player.getInventory().add(stack);
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /**
     * Puts up to {@code amount} of a {@link #pouched} currency in its pouch, which stops at 2,147,483,647 (OSRS's largest
     * stack); returns how many went in.
     */
    private static int toPouch(ServerPlayer player, String id, int amount) {
        PlayerData data = Players.data(player);
        long have = pouch(data, id);
        long in = Math.max(0, Math.min(amount, (long) Integer.MAX_VALUE - have));
        setPouch(data, id, have + in);
        return (int) in;
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
        spawnAtLocation(entity, stack, null);
    }

    /** {@link #spawnAtLocation(Entity, ItemStack)}; {@code spawned} (nullable) is handed each item entity. */
    public static void spawnAtLocation(Entity entity, ItemStack stack, Consumer<ItemEntity> spawned) {
        splitStacks(stack, s -> {
            ItemEntity e = entity.spawnAtLocation(s);
            if (e != null && spawned != null) {
                spawned.accept(e);
            }
        });
    }

    /** {@link Player#drop(ItemStack, boolean)} at the player's feet, one item entity per full stack. */
    public static void drop(Player player, ItemStack stack, boolean noPickUpDelay) {
        drop(player, stack, noPickUpDelay, null);
    }

    /** {@link #drop(Player, ItemStack, boolean)}; {@code dropped} (nullable) is handed each item entity. */
    public static void drop(Player player, ItemStack stack, boolean noPickUpDelay, Consumer<ItemEntity> dropped) {
        splitStacks(stack, s -> {
            ItemEntity entity = player.drop(s, false);
            if (entity != null) {
                if (noPickUpDelay) {
                    entity.setNoPickUpDelay();
                }
                if (dropped != null) {
                    dropped.accept(entity);
                }
            }
        });
    }

    /**
     * Picks plain coins or tokkul lying within reach into their pouch while the pack is full: OSRS adds to a stack the
     * inventory already holds without a free slot, but vanilla pickup needs one (with a free slot vanilla takes the stack, and
     * it moves into the pouch on the next tick). An empty pouch is no stack held, so it takes nothing, as a full OSRS inventory
     * without that item can't. Called every tick ({@code PlayerEvents}); returns how many items it took.
     */
    public static int pickUpPouched(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.getInventory().getFreeSlot() >= 0) {
            return 0;
        }
        PlayerData data = Players.data(player);
        if (data.coins() <= 0 && data.tokkul() <= 0) {
            return 0;
        }
        int took = 0;
        // Vanilla's pickup reach (Player.aiStep); an item dropped for someone else stays theirs, as in ItemEntity.playerTouch.
        AABB reach = player.getBoundingBox().inflate(1.0, 0.5, 1.0);
        for (ItemEntity e : player.serverLevel().getEntitiesOfClass(ItemEntity.class, reach, x -> x.isAlive() && !x.hasPickUpDelay()
                && (x.getTarget() == null || x.getTarget().equals(player.getUUID())))) {
            ItemStack s = e.getItem();
            String id = idOf(s);
            if (!pouched(id) || !s.getComponentsPatch().isEmpty() || pouch(data, id) <= 0) {
                continue;
            }
            int in = toPouch(player, id, s.getCount());
            if (in <= 0) {
                continue;
            }
            player.take(e, in);
            took += in;
            if (in >= s.getCount()) {
                e.discard();
            } else {
                e.setItem(s.copyWithCount(s.getCount() - in));
            }
        }
        return took;
    }

    /** True if one more of {@code id} would fit in the inventory (coins and tokkul: in their pouch, below its cap). */
    public static boolean hasRoomFor(Player player, String id) {
        if (pouched(id) && player instanceof ServerPlayer sp) {
            if (pouch(Players.data(sp), id) < Integer.MAX_VALUE) {
                return true;
            }
            // A full pouch leaves the item in a pack slot (PlayerEvents), so a slot can still take it.
        } else if (COINS.equals(id)) {
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
