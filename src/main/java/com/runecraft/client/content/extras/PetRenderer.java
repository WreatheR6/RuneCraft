package com.runecraft.client.content.extras;

import com.runecraft.client.OsrsBipedRenderer;
import com.runecraft.client.model.GenericEntityRenderer;
import com.runecraft.client.model.ModelSpecs;
import com.runecraft.content.extras.PetEntity;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.model.ModelSpec;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders a pet follower with its source's model scaled down (a boss pet looks like a small
 * version of its boss), or the module's own pet model for skilling pets.
 */
public class PetRenderer extends EntityRenderer<PetEntity> {
    /** Height in blocks a follower is drawn at. */
    private static final float PET_HEIGHT = 0.8f;

    private final EntityRendererFactory.Context ctx;
    private final Map<String, EntityRenderer<PetEntity>> delegates = new HashMap<>();

    public PetRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.ctx = ctx;
        this.shadowRadius = 0.3f;
    }

    private EntityRenderer<PetEntity> delegate(PetEntity pet) {
        return delegates.computeIfAbsent(pet.petId(), this::create);
    }

    /** Unscaled height of the creature a model belongs to (boss/monster hitbox height), or the pet's own value. */
    private static float lookHeight(Pets.PetDef def) {
        for (BossDef b : ContentRegistry.bosses()) {
            if (def.look().equals(b.model) || def.look().equals(b.id)) {
                return b.height;
            }
        }
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (def.look().equals(m.model) || def.look().equals(m.id)) {
                return m.height;
            }
        }
        return def.lookHeight();
    }

    private EntityRenderer<PetEntity> create(String petId) {
        Pets.PetDef def = Pets.get(petId);
        String look = def == null ? "ext_pet_beaver" : def.look();
        float scale = def == null ? 1.0f : Math.max(0.08f, Math.min(1.0f, PET_HEIGHT / Math.max(0.3f, lookHeight(def))));
        ModelSpec spec = ContentRegistry.models().get(look);
        if (spec != null && ModelSpecs.layer(spec.id()) != null) {
            return new GenericEntityRenderer<>(ctx, spec, scale);
        }
        return new OsrsBipedRenderer<>(ctx, look, scale);
    }

    @Override
    public void render(PetEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light) {
        delegate(entity).render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PetEntity entity) {
        return delegate(entity).getTexture(entity);
    }
}
