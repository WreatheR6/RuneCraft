package com.runecraft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fabric's use-item callback: the first listener that returns something other than PASS decides the result and the
 * vanilla use is skipped. Fired from NeoForge's {@code PlayerInteractEvent.RightClickItem} on both sides.
 */
@FunctionalInterface
public interface UseItemCallback {
    InteractionResultHolder<ItemStack> interact(Player player, Level world, InteractionHand hand);

    Event<UseItemCallback> EVENT = new Event<>(listeners -> (player, world, hand) -> {
        for (UseItemCallback l : listeners) {
            InteractionResultHolder<ItemStack> result = l.interact(player, world, hand);
            if (result.getResult() != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResultHolder.pass(ItemStack.EMPTY);
    });
}
