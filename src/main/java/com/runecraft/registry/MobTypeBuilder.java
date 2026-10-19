package com.runecraft.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds and registers a RuneCraft entity type (call it inside the entity type registration event). Default
 * attributes and ground spawn rules are kept and handed to NeoForge's attribute and spawn placement events.
 */
public final class MobTypeBuilder<T extends Entity> {
    private static final Map<EntityType<? extends LivingEntity>, Supplier<AttributeSupplier.Builder>> ATTRIBUTES = new LinkedHashMap<>();
    private static final List<Consumer<RegisterSpawnPlacementsEvent>> SPAWNS = new ArrayList<>();

    private final EntityType.Builder<T> builder;
    private Supplier<AttributeSupplier.Builder> attributes;
    private boolean groundSpawns;

    private MobTypeBuilder(EntityType.EntityFactory<T> factory, MobCategory category) {
        this.builder = EntityType.Builder.of(factory, category);
    }

    public static <T extends Entity> MobTypeBuilder<T> create(EntityType.EntityFactory<T> factory, MobCategory category) {
        return new MobTypeBuilder<>(factory, category);
    }

    public MobTypeBuilder<T> size(float width, float height) {
        builder.sized(width, height);
        return this;
    }

    public MobTypeBuilder<T> defaultAttributes(Supplier<AttributeSupplier.Builder> attributes) {
        this.attributes = attributes;
        return this;
    }

    /** Natural spawns on the ground at any light level, with monster spawn rules (the type must be a monster). */
    public MobTypeBuilder<T> groundSpawns() {
        this.groundSpawns = true;
        return this;
    }

    public MobTypeBuilder<T> trackRangeBlocks(int blocks) {
        builder.clientTrackingRange((blocks + 15) / 16);
        return this;
    }

    public MobTypeBuilder<T> trackRangeChunks(int chunks) {
        builder.clientTrackingRange(chunks);
        return this;
    }

    public MobTypeBuilder<T> fireImmune() {
        builder.fireImmune();
        return this;
    }

    public MobTypeBuilder<T> disableSaving() {
        builder.noSave();
        return this;
    }

    @SuppressWarnings("unchecked")
    public EntityType<T> register(ResourceLocation id) {
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(id.toString()));
        if (attributes != null) {
            ATTRIBUTES.put((EntityType<? extends LivingEntity>) type, attributes);
        }
        if (groundSpawns) {
            EntityType<net.minecraft.world.entity.monster.Monster> monster = (EntityType<net.minecraft.world.entity.monster.Monster>) type;
            SPAWNS.add(event -> event.register(monster, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        }
        return type;
    }

    static void registerAttributes(EntityAttributeCreationEvent event) {
        ATTRIBUTES.forEach((type, attributes) -> event.put(type, attributes.get().build()));
    }

    static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        SPAWNS.forEach(s -> s.accept(event));
    }
}
