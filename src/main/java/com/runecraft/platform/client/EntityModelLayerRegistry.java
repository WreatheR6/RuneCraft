package com.runecraft.platform.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Fabric's model layer registry; layers are handed to NeoForge's {@code EntityRenderersEvent.RegisterLayerDefinitions}. */
public final class EntityModelLayerRegistry {
    private EntityModelLayerRegistry() {
    }

    static final Map<ModelLayerLocation, Supplier<LayerDefinition>> PENDING = new ConcurrentHashMap<>();

    public static void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> provider) {
        PENDING.put(layer, provider);
    }
}
