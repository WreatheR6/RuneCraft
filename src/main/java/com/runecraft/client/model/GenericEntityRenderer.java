package com.runecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.RuneCraft;
import com.runecraft.osrs.model.ModelSpec;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renders any mob with a data-defined model (texture: textures/entity/model/&lt;spec id&gt;.png). */
public class GenericEntityRenderer<T extends Mob> extends MobRenderer<T, GenericModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public GenericEntityRenderer(EntityRendererProvider.Context ctx, ModelSpec spec, float entityScale) {
        super(ctx, new GenericModel<>(ctx.bakeLayer(ModelSpecs.layer(spec.id())), spec), spec.shadowRadius() * entityScale);
        this.texture = RuneCraft.id("textures/entity/model/" + spec.id() + ".png");
        this.scale = spec.scale() * entityScale;
        if (spec.hasGlow()) {
            this.addLayer(new GlowFeatureRenderer<>(this, RuneCraft.id("textures/entity/model/" + spec.id() + "_glow.png")));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
