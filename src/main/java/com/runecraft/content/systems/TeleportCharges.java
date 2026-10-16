package com.runecraft.content.systems;

import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.content.systems.Charges;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Charges of a charged item, kept on the stack (NBT) or, for the items whose charges belong to the player in OSRS
 * ({@link Charges#perPlayer}: the ring of the elements, the camulet), in the player's systems state.
 */
final class TeleportCharges {
    private TeleportCharges() {
    }

    private static Charges.Spec perPlayerSpec(ItemStack stack) {
        Charges.Spec s = ItemCharges.spec(stack);
        return s != null && Charges.perPlayer(s.id()) ? s : null;
    }

    static int get(ServerPlayerEntity p, ItemStack stack) {
        Charges.Spec s = perPlayerSpec(stack);
        if (s == null) {
            return ItemCharges.get(stack);
        }
        return Charges.clamp(s, SysState.of(p).playerCharges.getOrDefault(s.id(), s.initial()));
    }

    static void set(ServerPlayerEntity p, ItemStack stack, int charges) {
        Charges.Spec s = perPlayerSpec(stack);
        if (s == null) {
            ItemCharges.set(stack, charges);
            return;
        }
        SysState.of(p).playerCharges.put(s.id(), Charges.clamp(s, charges));
        ItemCharges.set(stack, charges); // mirrored on the stack for its tooltip
    }
}
