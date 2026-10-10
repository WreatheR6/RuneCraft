package com.runecraft.client.model;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.model.ModelSpec;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/** Renders any mob with a data-defined model (texture: textures/entity/model/&lt;spec id&gt;.png). */
public class GenericEntityRenderer<T extends MobEntity> extends MobEntityRenderer<T, GenericModel<T>> {
    private final Identifier texture;
    private final float scale;

    public GenericEntityRenderer(EntityRendererFactory.Context ctx, ModelSpec spec, float entityScale) {
        super(ctx, new GenericModel<>(ctx.getPart(ModelSpecs.layer(spec.id())), spec), spec.shadowRadius() * entityScale);
        this.texture = RuneCraft.id("textures/entity/model/" + spec.id() + ".png");
        this.scale = spec.scale() * entityScale;
        if (spec.hasGlow()) {
            this.addFeature(new GlowFeatureRenderer<>(this, RuneCraft.id("textures/entity/model/" + spec.id() + "_glow.png")));
        }
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
