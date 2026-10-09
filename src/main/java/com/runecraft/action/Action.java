package com.runecraft.action;

import net.minecraft.server.network.ServerPlayerEntity;

/** A repeating skilling action processed once per OSRS game tick (0.6s). */
public interface Action {
    /** Called every game tick. Return false to stop the action. */
    boolean tick(ServerPlayerEntity player, long gameTick);

    /** Called when the action is replaced or interrupted. */
    default void stop(ServerPlayerEntity player) {
    }
}
