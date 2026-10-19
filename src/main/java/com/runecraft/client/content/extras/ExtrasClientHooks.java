package com.runecraft.client.content.extras;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.extras.ExtrasEntities;
import com.runecraft.content.extras.SailBoatEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.EntityType;

/** Client side of the extras module: renderers for pet followers and Sailing boats. */
public final class ExtrasClientHooks implements ClientModuleHooks {
    @Override
    public void initClient() {
        if (ExtrasEntities.PET != null) {
            EntityRendererRegistry.register(ExtrasEntities.PET, PetRenderer::new);
        }
        for (EntityType<SailBoatEntity> type : ExtrasEntities.BOATS.values()) {
            EntityRendererRegistry.register(type, ctx -> new BoatRenderer(ctx, false));
        }
    }
}
