package com.runecraft.content.systems;

import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.util.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Charges of a charged item, kept on the stack (NBT) or, for the items whose charges belong to the player in OSRS
 * ({@link Charges#perPlayer}: the ring of the elements, the camulet), in the player's systems state.
 */
final class TeleportCharges {
    /**
     * Stack NBT flag: this stack's {@link ItemCharges#KEY} only mirrors its holder's charges. A per-player item charged
     * before its charges moved to the player has the key without the flag; those charges are merged in once
     * ({@link Charges#perPlayerCharges}) and the stack is flagged on the next change.
     */
    private static final String MIRRORED = "RcChargesMirrored";

    private TeleportCharges() {
    }

    private static Charges.Spec perPlayerSpec(ItemStack stack) {
        Charges.Spec s = ItemCharges.spec(stack);
        return s != null && Charges.perPlayer(s.id()) ? s : null;
    }

    /** Charges a per-player stack held from before they moved to the player, or null. */
    private static Integer legacy(ItemStack stack) {
        CompoundTag nbt = StackData.get(stack);
        return nbt.contains(ItemCharges.KEY) && !nbt.getBoolean(MIRRORED) ? ItemCharges.get(stack) : null;
    }

    static int get(ServerPlayer p, ItemStack stack) {
        Charges.Spec s = perPlayerSpec(stack);
        if (s == null) {
            return ItemCharges.get(stack);
        }
        return Charges.perPlayerCharges(s, SysState.of(p).playerCharges.get(s.id()), legacy(stack));
    }

    static void set(ServerPlayer p, ItemStack stack, int charges) {
        Charges.Spec s = perPlayerSpec(stack);
        if (s == null) {
            ItemCharges.set(stack, charges);
            return;
        }
        SysState.of(p).playerCharges.put(s.id(), Charges.clamp(s, charges));
        ItemCharges.set(stack, charges); // mirrored on the stack for its tooltip
        StackData.update(stack, t -> t.putBoolean(MIRRORED, true));
    }
}
