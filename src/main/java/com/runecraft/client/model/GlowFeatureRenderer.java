package com.runecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Renders a model's emissive "_glow" texture full-bright with additive blending (like vanilla's
 * spider/enderman eyes), so lava cracks, eyes and magic glow in the dark.
 */
public class GlowFeatureRenderer<T extends Mob> extends RenderLayer<T, GenericModel<T>> {
    private final RenderType layer;

    public GlowFeatureRenderer(RenderLayerParent<T, GenericModel<T>> context, ResourceLocation texture) {
        super(context);
        this.layer = RenderType.eyes(texture);
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, T entity, float limbAngle,
                       float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        this.getParentModel().renderGlow(matrices, consumer, 0xF00000, OverlayTexture.NO_OVERLAY);
    }
}
