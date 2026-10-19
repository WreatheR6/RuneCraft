package com.runecraft.platform.client;

import com.runecraft.platform.Event;

/** Fabric's world render events; {@code AFTER_ENTITIES} fires from NeoForge's {@code RenderLevelStageEvent} at that stage. */
public final class WorldRenderEvents {
    private WorldRenderEvents() {
    }

    @FunctionalInterface
    public interface AfterEntities {
        void afterEntities(WorldRenderContext context);
    }

    public static final Event<AfterEntities> AFTER_ENTITIES = new Event<>(listeners -> context -> {
        for (AfterEntities l : listeners) {
            l.afterEntities(context);
        }
    });
}
