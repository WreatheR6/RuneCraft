package com.runecraft.client.model;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/**
 * Renders a model's emissive "_glow" texture full-bright with additive blending (like vanilla's
 * spider/enderman eyes), so lava cracks, eyes and magic glow in the dark.
 */
public class GlowFeatureRenderer<T extends MobEntity> extends FeatureRenderer<T, GenericModel<T>> {
    private final RenderLayer layer;

    public GlowFeatureRenderer(FeatureRendererContext<T, GenericModel<T>> context, Identifier texture) {
        super(context);
        this.layer = RenderLayer.getEyes(texture);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle,
                       float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        this.getContextModel().renderGlow(matrices, consumer, 0xF00000, OverlayTexture.DEFAULT_UV);
    }
}
