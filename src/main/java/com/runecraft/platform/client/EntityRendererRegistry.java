package com.runecraft.platform.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Fabric's entity renderer registry; registrations are replayed into NeoForge's {@code EntityRenderersEvent.RegisterRenderers}. */
public final class EntityRendererRegistry {
    private EntityRendererRegistry() {
    }

    static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> PENDING = new CopyOnWriteArrayList<>();

    public static <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> factory) {
        if (type == null) {
            throw new IllegalArgumentException("Entity renderer registered for a null entity type");
        }
        PENDING.add(event -> event.registerEntityRenderer(type, factory));
    }
}
