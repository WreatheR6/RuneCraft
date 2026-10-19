package com.runecraft.bank;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.bank.BankNotes;
import com.runecraft.util.Inv;
import com.runecraft.util.StackData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Bank notes on the Minecraft side: one {@code runecraft:bank_note} item whose NBT names the noted item
 * ({@code Noted}) and that item's own stack data ({@code NotedTag}, e.g. a potion's doses). Also stack/key helpers
 * shared by the bank (an item's bank key is its id plus its NBT as a string).
 */
public final class Notes {
    public static final String NOTED = "Noted";
    public static final String NOTED_TAG = "NotedTag";
    private static final String DAMAGE = "Damage";

    private Notes() {
    }

    // ------------------------------------------------------------------ stack <-> bank key

    /** The stack's NBT as a string ("" for none). */
    public static String tagOf(ItemStack stack) {
        CompoundTag n = StackData.get(stack);
        if (stack.isDamageableItem()) {
            n.putInt(DAMAGE, stack.getDamageValue());
        }
        return n.isEmpty() ? "" : n.getAsString();
    }

    /** A stack of a bank item (id + NBT string), or EMPTY for unknown items. */
    public static ItemStack stackOf(String item, String tag, int count) {
        Item it = BuiltInRegistries.ITEM.get(RuneCraft.resolve(item));
        if (it == Items.AIR || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(it, count);
        if (tag != null && !tag.isEmpty()) {
            try {
                CompoundTag n = TagParser.parseTag(tag);
                if (s.isDamageableItem() && n.contains(DAMAGE, Tag.TAG_ANY_NUMERIC)) {
                    s.setDamageValue(n.getInt(DAMAGE));
                    n.remove(DAMAGE);
                }
                StackData.set(s, n);
            } catch (CommandSyntaxException e) {
                RuneCraft.LOGGER.warn("Bad bank item data for {}: {}", item, tag);
            }
        }
        return s;
    }

    // ------------------------------------------------------------------ notes

    public static boolean isNote(ItemStack stack) {
        return !stack.isEmpty() && Inv.idOf(stack).equals(BankNotes.NOTE_ITEM) && StackData.contains(stack, NOTED);
    }

    /** The item a note stands for ("" if not a note). */
    public static String notedId(ItemStack stack) {
        return isNote(stack) ? StackData.get(stack).getString(NOTED) : "";
    }

    /** The noted item's own stack data ("" for none). */
    public static String notedTag(ItemStack stack) {
        return isNote(stack) ? StackData.get(stack).getString(NOTED_TAG) : "";
    }

    /** A single unnoted item for a note (EMPTY if the note is invalid). */
    public static ItemStack unnoted(ItemStack note, int count) {
        return stackOf(notedId(note), notedTag(note), count);
    }

    /**
     * Whether a bank item can be withdrawn as a note: OSRS rules ({@link BankNotes#canNote}) and no stack data
     * beyond item damage (potion doses), so charged or customised items stay real items.
     */
    public static boolean canNote(String item, String tag) {
        if (!BankNotes.canNote(item)) {
            return false;
        }
        if (tag == null || tag.isEmpty()) {
            return true;
        }
        ItemStack s = stackOf(item, tag, 1);
        CompoundTag n = StackData.get(s);
        for (String k : n.getAllKeys()) {
            if (!k.equals(DAMAGE)) {
                return false;
            }
        }
        return true;
    }

    /** A stack of notes for an item (EMPTY if the note item is missing). */
    public static ItemStack note(String item, String tag, int count) {
        ItemStack s = Inv.stack(BankNotes.NOTE_ITEM, count);
        if (s.isEmpty()) {
            return s;
        }
        CompoundTag n = new CompoundTag();
        n.putString(NOTED, item);
        if (tag != null && !tag.isEmpty()) {
            n.putString(NOTED_TAG, tag);
        }
        StackData.set(s, n);
        return s;
    }

    // ------------------------------------------------------------------ inventory helpers

    /** How many more of {@code proto} fit in the main inventory (36 slots), stacking with matching stacks. */
    public static int room(Player player, ItemStack proto) {
        Inventory inv = player.getInventory();
        long room = 0;
        int max = proto.getMaxStackSize();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) {
                room += max;
            } else if (ItemStack.isSameItemSameComponents(s, proto)) {
                room += Math.max(0, max - s.getCount());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, room);
    }

    /** Puts up to {@code count} copies of {@code proto} into the main inventory; returns how many went in. */
    public static int insert(Player player, ItemStack proto, int count) {
        Inventory inv = player.getInventory();
        int left = count;
        int max = proto.getMaxStackSize();
        for (int i = 0; i < inv.items.size() && left > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, proto) && s.getCount() < max) {
                int n = Math.min(left, max - s.getCount());
                s.grow(n);
                left -= n;
            }
        }
        for (int i = 0; i < inv.items.size() && left > 0; i++) {
            if (inv.items.get(i).isEmpty()) {
                int n = Math.min(left, max);
                inv.items.set(i, proto.copyWithCount(n));
                left -= n;
            }
        }
        inv.setChanged();
        return count - left;
    }

    /** Count of an item including notes of it (what general stores and the Grand Exchange accept). */
    public static int countWithNotes(Player player, String id) {
        long total = Inv.count(player, id);
        if (BankNotes.canNote(id)) {
            for (ItemStack s : player.getInventory().items) {
                if (isNote(s) && notedId(s).equals(id) && notedTag(s).isEmpty()) {
                    total += s.getCount();
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Removes items, real ones first, then notes of them; returns how many were removed. */
    public static int removeWithNotes(Player player, String id, int amount) {
        int removed = Inv.remove(player, id, amount);
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size() && removed < amount; i++) {
            ItemStack s = inv.items.get(i);
            if (isNote(s) && notedId(s).equals(id) && notedTag(s).isEmpty()) {
                int n = Math.min(amount - removed, s.getCount());
                s.shrink(n);
                removed += n;
            }
        }
        return removed;
    }

    /**
     * Gives items as the Grand Exchange collection box does: several of a noteable item come as notes (one slot),
     * otherwise as the items themselves (overflow is dropped at the player's feet, as {@link Inv#give} does).
     */
    public static void giveNotedIfMany(Player player, String id, int amount) {
        if (amount > 1 && BankNotes.canNote(id)) {
            ItemStack proto = note(id, "", 1);
            if (!proto.isEmpty()) {
                int left = amount;
                while (left > 0) {
                    int n = Math.min(left, proto.getMaxStackSize());
                    Inv.give(player, proto.copyWithCount(n));
                    left -= n;
                }
                return;
            }
        }
        Inv.give(player, id, amount);
    }
}
