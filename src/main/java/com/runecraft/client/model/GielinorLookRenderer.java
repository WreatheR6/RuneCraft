package com.runecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.world.Gielinor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Renders a vanilla mob with its OSRS look (a {@link ModelSpec} model) inside Gielinor and with the
 * normal vanilla renderer everywhere else, so cows, chickens, rats and spiders in Gielinor look like
 * their RuneScape counterparts without changing the overworld.
 */
public class GielinorLookRenderer<T extends Mob> extends EntityRenderer<T> {
    private final EntityRenderer<? super T> vanilla;
    private final GenericEntityRenderer<T> osrs;

    public GielinorLookRenderer(EntityRendererProvider.Context ctx, EntityRenderer<? super T> vanilla, ModelSpec spec, float scale) {
        super(ctx);
        this.vanilla = vanilla;
        this.osrs = new GenericEntityRenderer<>(ctx, spec, scale);
        this.shadowRadius = spec.shadowRadius() * scale;
    }

    private boolean osrs(T entity) {
        return Gielinor.is(entity);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if (osrs(entity)) {
            osrs.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        } else {
            vanilla.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return osrs(entity) ? osrs.getTextureLocation(entity) : vanilla.getTextureLocation(entity);
    }
}
