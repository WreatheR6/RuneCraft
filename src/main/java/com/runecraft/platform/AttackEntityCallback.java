package com.runecraft.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Fabric's attack-entity callback (non-PASS cancels the attack). Fired from NeoForge's {@code AttackEntityEvent}. */
@FunctionalInterface
public interface AttackEntityCallback {
    InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult);

    Event<AttackEntityCallback> EVENT = new Event<>(listeners -> (player, world, hand, entity, hit) -> {
        for (AttackEntityCallback l : listeners) {
            InteractionResult result = l.interact(player, world, hand, entity, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    });
}
