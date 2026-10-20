package com.runecraft.item;

import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.util.Inv;
import com.runecraft.util.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

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
        CompoundTag nbt = StackData.get(stack);
        return nbt.contains(KEY) ? Charges.clamp(s, nbt.getInt(KEY)) : s.initial();
    }

    public static void set(ItemStack stack, int charges) {
        Charges.Spec s = spec(stack);
        if (s == null) {
            return;
        }
        StackData.update(stack, t -> t.putInt(KEY, Charges.clamp(s, charges)));
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

    /** Stack flag of a Wilderness weapon that holds its activation ether ({@link Charges#needsActivation}). */
    public static final String ACTIVATED = "RcActivated";

    /**
     * Whether a Wilderness weapon has been activated (items that need no activation always count as activated, and so does
     * a weapon still holding ether from before activation existed: older saves).
     */
    public static boolean activated(ItemStack stack) {
        return stack == null || stack.isEmpty() || !Charges.needsActivation(Inv.idOf(stack)) || StackData.get(stack).getBoolean(ACTIVATED)
                || get(stack) > 0;
    }

    public static void setActivated(ItemStack stack, boolean on) {
        StackData.update(stack, t -> {
            if (on) {
                t.putBoolean(ACTIVATED, true);
            } else {
                t.remove(ACTIVATED);
            }
        });
    }
}
