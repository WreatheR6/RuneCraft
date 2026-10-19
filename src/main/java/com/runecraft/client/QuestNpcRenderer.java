package com.runecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.client.model.GenericEntityRenderer;
import com.runecraft.client.model.ModelSpecs;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders talk-to NPCs: the textured biped, unless a model spec exists under the NPC's id (e.g. the TzHaar
 * shopkeepers and Fight Cave/Pit officials, which use the shared TzHaar body like the TzHaar monsters).
 */
public class QuestNpcRenderer extends EntityRenderer<QuestNpcEntity> {
    private final EntityRendererProvider.Context ctx;
    private final OsrsBipedRenderer<QuestNpcEntity> biped;
    private final Map<String, EntityRenderer<QuestNpcEntity>> byNpc = new HashMap<>();

    public QuestNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.ctx = ctx;
        this.biped = new OsrsBipedRenderer<>(ctx, null, 1.0f);
        this.shadowRadius = 0.5f;
    }

    private EntityRenderer<QuestNpcEntity> pick(QuestNpcEntity npc) {
        String id = npc.npcId();
        if (id == null || id.isEmpty()) {
            return biped;
        }
        return byNpc.computeIfAbsent(id, key -> {
            ModelSpec spec = ContentRegistry.models().get(key);
            if (spec == null || ModelSpecs.layer(spec.id()) == null) {
                return biped;
            }
            return new GenericEntityRenderer<>(ctx, spec, 1.0f);
        });
    }

    @Override
    public void render(QuestNpcEntity npc, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        pick(npc).render(npc, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(QuestNpcEntity npc) {
        return pick(npc).getTextureLocation(npc);
    }
}
