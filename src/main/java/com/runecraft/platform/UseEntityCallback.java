package com.runecraft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Fabric's use-entity callback. As on Fabric it runs for both of the client's interaction packets: with a hit result
 * ({@code PlayerInteractEvent.EntityInteractSpecific}) and without one ({@code EntityInteract}, hit result null).
 */
@FunctionalInterface
public interface UseEntityCallback {
    InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult);

    Event<UseEntityCallback> EVENT = new Event<>(listeners -> (player, world, hand, entity, hit) -> {
        for (UseEntityCallback l : listeners) {
            InteractionResult result = l.interact(player, world, hand, entity, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    });
}
