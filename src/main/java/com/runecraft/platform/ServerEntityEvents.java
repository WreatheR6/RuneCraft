package com.runecraft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Fabric's entity load event, fired from NeoForge's {@code EntityJoinLevelEvent} on server levels (spawns and chunk loads). */
public final class ServerEntityEvents {
    private ServerEntityEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onLoad(Entity entity, ServerLevel world);
    }

    public static final Event<Load> ENTITY_LOAD = new Event<>(listeners -> (entity, world) -> {
        for (Load l : listeners) {
            l.onLoad(entity, world);
        }
    });
}
