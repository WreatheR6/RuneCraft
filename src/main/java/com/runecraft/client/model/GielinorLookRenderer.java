package com.runecraft.client.model;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.world.Gielinor;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/**
 * Renders a vanilla mob with its OSRS look (a {@link ModelSpec} model) inside Gielinor and with the
 * normal vanilla renderer everywhere else, so cows, chickens, rats and spiders in Gielinor look like
 * their RuneScape counterparts without changing the overworld.
 */
public class GielinorLookRenderer<T extends MobEntity> extends EntityRenderer<T> {
    private final EntityRenderer<? super T> vanilla;
    private final GenericEntityRenderer<T> osrs;

    public GielinorLookRenderer(EntityRendererFactory.Context ctx, EntityRenderer<? super T> vanilla, ModelSpec spec, float scale) {
        super(ctx);
        this.vanilla = vanilla;
        this.osrs = new GenericEntityRenderer<>(ctx, spec, scale);
        this.shadowRadius = spec.shadowRadius() * scale;
    }

    private boolean osrs(T entity) {
        return Gielinor.is(entity);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (osrs(entity)) {
            osrs.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        } else {
            vanilla.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }

    @Override
    public Identifier getTexture(T entity) {
        return osrs(entity) ? osrs.getTexture(entity) : vanilla.getTexture(entity);
    }
}
