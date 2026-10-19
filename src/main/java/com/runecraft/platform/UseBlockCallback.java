package com.runecraft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fabric's use-block callback: the first listener that returns something other than PASS decides the result and the
 * block's and item's own use are skipped. Fired from NeoForge's {@code PlayerInteractEvent.RightClickBlock}.
 */
@FunctionalInterface
public interface UseBlockCallback {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);

    Event<UseBlockCallback> EVENT = new Event<>(listeners -> (player, world, hand, hit) -> {
        for (UseBlockCallback l : listeners) {
            InteractionResult result = l.interact(player, world, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    });
}
