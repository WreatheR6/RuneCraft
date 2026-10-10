package com.runecraft.client.model;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.CaveSpiderEntityRenderer;
import net.minecraft.client.render.entity.ChickenEntityRenderer;
import net.minecraft.client.render.entity.CowEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PigEntityRenderer;
import net.minecraft.client.render.entity.SheepEntityRenderer;
import net.minecraft.client.render.entity.SilverfishEntityRenderer;
import net.minecraft.client.render.entity.SkeletonEntityRenderer;
import net.minecraft.client.render.entity.SpiderEntityRenderer;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.WitchEntityRenderer;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;

import java.util.function.Function;

/** OSRS looks for the vanilla mobs that Gielinor uses as RuneScape monsters (see Monster's vanilla entries). */
public final class GielinorLooks {
    private GielinorLooks() {
    }

    public static void registerAll() {
        look(EntityType.CHICKEN, "chicken", ChickenEntityRenderer::new);
        look(EntityType.COW, "cow", CowEntityRenderer::new);
        look(EntityType.SHEEP, "sheep", SheepEntityRenderer::new);
        look(EntityType.PIG, "pig", PigEntityRenderer::new);
        look(EntityType.SILVERFISH, "giant_rat", SilverfishEntityRenderer::new);
        look(EntityType.CAVE_SPIDER, "spider", CaveSpiderEntityRenderer::new);
        look(EntityType.SPIDER, "giant_spider", ctx -> new SpiderEntityRenderer<SpiderEntity>(ctx));
        look(EntityType.ZOMBIE, "zombie", ZombieEntityRenderer::new);
        look(EntityType.SKELETON, "skeleton", SkeletonEntityRenderer::new);
        look(EntityType.WOLF, "wolf", WolfEntityRenderer::new);
        look(EntityType.VILLAGER, "man", VillagerEntityRenderer::new);
        look(EntityType.WITCH, "dark_wizard", WitchEntityRenderer::new);
    }

    private static <T extends MobEntity> void look(EntityType<T> type, String model,
                                                   Function<EntityRendererFactory.Context, ? extends EntityRenderer<? super T>> vanilla) {
        ModelSpec spec = ContentRegistry.models().get(model);
        if (spec == null) {
            return;
        }
        EntityRendererRegistry.register(type, ctx -> new GielinorLookRenderer<>(ctx, vanilla.apply(ctx), spec, 1.0f));
    }
}
