package com.runecraft.item;

import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.util.Inv;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Per-stack charges/degradation for the items in {@link Charges} (Barrows, crystal, tridents,
 * sanguinesti staff, serpentine helms, blowpipe), kept in the stack's NBT under {@value #KEY}. A stack
 * without the tag holds the item's starting charges (fully repaired Barrows, a Kraken's full trident,
 * an uncharged sanguinesti staff...).
 */
public final class ItemCharges {
    public static final String KEY = "RcCharges";

    private ItemCharges() {
    }

    /** The charge spec of this stack's item, or null for items without charges. */
    public static Charges.Spec spec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return Charges.of(Inv.idOf(stack));
    }

    public static int get(ItemStack stack) {
        Charges.Spec s = spec(stack);
        if (s == null) {
            return 0;
        }
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(KEY) ? Charges.clamp(s, nbt.getInt(KEY)) : s.initial();
    }

    public static void set(ItemStack stack, int charges) {
        Charges.Spec s = spec(stack);
        if (s == null) {
            return;
        }
        stack.getOrCreateNbt().putInt(KEY, Charges.clamp(s, charges));
    }

    /** Drains charges; returns true when this made the item run out. */
    public static boolean drain(ItemStack stack, int amount) {
        Charges.Spec s = spec(stack);
        if (s == null || amount <= 0) {
            return false;
        }
        int before = get(stack);
        int after = Charges.drain(s, before, amount);
        set(stack, after);
        return before > 0 && after <= 0;
    }

    /** True for a charged item that has run out (gives no bonuses; weapons won't attack). */
    public static boolean depleted(ItemStack stack) {
        Charges.Spec s = spec(stack);
        return s != null && Charges.depleted(s, get(stack));
    }
}
