package com.runecraft.client.model;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import java.util.function.Function;

/** OSRS looks for the vanilla mobs that Gielinor uses as RuneScape monsters (see Monster's vanilla entries). */
public final class GielinorLooks {
    private GielinorLooks() {
    }

    public static void registerAll() {
        look(EntityType.CHICKEN, "chicken", ChickenRenderer::new);
        look(EntityType.COW, "cow", CowRenderer::new);
        look(EntityType.SHEEP, "sheep", SheepRenderer::new);
        look(EntityType.PIG, "pig", PigRenderer::new);
        look(EntityType.SILVERFISH, "giant_rat", SilverfishRenderer::new);
        look(EntityType.CAVE_SPIDER, "spider", CaveSpiderRenderer::new);
        look(EntityType.SPIDER, "giant_spider", ctx -> new SpiderRenderer<Spider>(ctx));
        look(EntityType.ZOMBIE, "zombie", ZombieRenderer::new);
        look(EntityType.SKELETON, "skeleton", SkeletonRenderer::new);
        look(EntityType.WOLF, "wolf", WolfRenderer::new);
        look(EntityType.VILLAGER, "man", VillagerRenderer::new);
        look(EntityType.WITCH, "dark_wizard", WitchRenderer::new);
    }

    private static <T extends Mob> void look(EntityType<T> type, String model,
                                                   Function<EntityRendererProvider.Context, ? extends EntityRenderer<? super T>> vanilla) {
        ModelSpec spec = ContentRegistry.models().get(model);
        if (spec == null) {
            return;
        }
        EntityRendererRegistry.register(type, ctx -> new GielinorLookRenderer<>(ctx, vanilla.apply(ctx), spec, 1.0f));
    }
}
