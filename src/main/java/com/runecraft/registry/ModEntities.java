package com.runecraft.registry;

import com.runecraft.RuneCraft;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** RuneCraft creatures, bosses and quest characters. */
public final class ModEntities {
    private static final Map<EntityType<?>, MonsterStats> STATS = new HashMap<>();
    private static final Map<EntityType<?>, Boss> BOSSES = new HashMap<>();
    public static final Map<Monster, EntityType<OsrsMonsterEntity>> MONSTERS = new LinkedHashMap<>();
    public static final Map<Boss, EntityType<BossEntity>> BOSS_TYPES = new LinkedHashMap<>();
    public static EntityType<QuestNpcEntity> QUEST_NPC;
    private static final Map<EntityType<?>, com.runecraft.osrs.content.BossDef> DATA_BOSSES = new HashMap<>();
    private static final Map<String, EntityType<?>> BY_ID = new HashMap<>();
    public static final Map<String, EntityType<OsrsMonsterEntity>> MODULE_MONSTERS = new LinkedHashMap<>();
    public static final Map<String, EntityType<com.runecraft.boss.DataBossEntity>> DATA_BOSS_TYPES = new LinkedHashMap<>();

    private ModEntities() {
    }

    /** Model scale for a creature id (used for hitboxes and rendering). */
    public static float scale(String id) {
        Boss boss = Boss.byEntity(id);
        if (boss != null) {
            return boss.scale();
        }
        return switch (id) {
            case "goblin" -> 0.8f;
            case "hill_giant", "moss_giant", "ice_giant" -> 2.0f;
            case "lesser_demon" -> 1.5f;
            case "greater_demon" -> 1.7f;
            case "black_demon" -> 2.0f;
            default -> 1.0f;
        };
    }

    private static DefaultAttributeContainer.Builder attributes(int hp, double speed) {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, hp)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    public static void register() {
        for (Monster m : Monster.values()) {
            if (!m.isCustom()) {
                continue;
            }
            float s = scale(m.entity());
            EntityType<OsrsMonsterEntity> type = FabricEntityTypeBuilder.<OsrsMonsterEntity>createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(OsrsMonsterEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f * s, 1.95f * s))
                    .defaultAttributes(() -> attributes(m.hitpoints(), 0.25))
                    .spawnRestriction(SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            HostileEntity::canSpawnIgnoreLightLevel)
                    .trackRangeBlocks(64)
                    .build();
            STATS.put(type, MonsterStats.from(m));
            Registry.register(Registries.ENTITY_TYPE, RuneCraft.id(m.entity()), type);
            MONSTERS.put(m, type);
        }
        for (Boss b : Boss.values()) {
            float s = b.scale();
            EntityType<BossEntity> type = FabricEntityTypeBuilder.<BossEntity>createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(BossEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f * s, 1.95f * s))
                    .defaultAttributes(() -> attributes(b.hitpoints(), 0.27))
                    .trackRangeBlocks(96)
                    .fireImmune()
                    .build();
            STATS.put(type, new MonsterStats(b.displayName(), b.combatLevel(), b.hitpoints(), b.attack(), b.strength(),
                    b.defence(), b.magic(), b.ranged(), b.defenceBonus(), b.attackSpeed(), b.style(), b.maxHit(), null, null, b));
            BOSSES.put(type, b);
            Registry.register(Registries.ENTITY_TYPE, RuneCraft.id(b.entityId()), type);
            BOSS_TYPES.put(b, type);
        }
        for (com.runecraft.osrs.content.MonsterDef m : com.runecraft.osrs.content.ContentRegistry.monsters()) {
            float sc = m.scale;
            // rock crabs lie disguised as rocks until a player comes close
            // demonic and tortured gorillas switch prayers and attack styles (slayer module)
            EntityType.EntityFactory<OsrsMonsterEntity> factory = "rock_crab".equals(m.id)
                    ? com.runecraft.entity.RockCrabEntity::new
                    : "demonic_gorilla".equals(m.id) || "tortured_gorilla".equals(m.id)
                    ? com.runecraft.content.slayer.GorillaEntity::new : OsrsMonsterEntity::new;
            EntityType<OsrsMonsterEntity> type = FabricEntityTypeBuilder.<OsrsMonsterEntity>createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(factory)
                    .dimensions(EntityDimensions.fixed(m.width * sc, m.height * sc))
                    .defaultAttributes(() -> attributes(m.hitpoints, m.moveSpeed))
                    .spawnRestriction(SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            HostileEntity::canSpawnIgnoreLightLevel)
                    .trackRangeBlocks(64)
                    .build();
            STATS.put(type, MonsterStats.from(m));
            Registry.register(Registries.ENTITY_TYPE, RuneCraft.id(m.id), type);
            MODULE_MONSTERS.put(m.id, type);
            BY_ID.put(m.id, type);
        }
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            float sc = b.scale;
            EntityType<com.runecraft.boss.DataBossEntity> type = FabricEntityTypeBuilder.<com.runecraft.boss.DataBossEntity>createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(com.runecraft.boss.DataBossEntity::new)
                    .dimensions(EntityDimensions.fixed(b.width * sc, b.height * sc))
                    .defaultAttributes(() -> attributes(b.hitpoints, b.moveSpeed))
                    .trackRangeBlocks(128)
                    .fireImmune()
                    .build();
            STATS.put(type, MonsterStats.from(b));
            DATA_BOSSES.put(type, b);
            Registry.register(Registries.ENTITY_TYPE, RuneCraft.id(b.id), type);
            DATA_BOSS_TYPES.put(b.id, type);
            BY_ID.put(b.id, type);
        }
        MONSTERS.forEach((m, t) -> BY_ID.put(m.entity(), t));
        BOSS_TYPES.forEach((b, t) -> BY_ID.put(b.entityId(), t));
        QUEST_NPC = Registry.register(Registries.ENTITY_TYPE, RuneCraft.id("quest_npc"),
                FabricEntityTypeBuilder.<QuestNpcEntity>createMob()
                        .spawnGroup(SpawnGroup.MISC)
                        .entityFactory(QuestNpcEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                        .defaultAttributes(QuestNpcEntity::createAttributes)
                        .build());
    }

    public static MonsterStats stats(EntityType<?> type) {
        return STATS.get(type);
    }

    public static Boss boss(EntityType<?> type) {
        return BOSSES.get(type);
    }

    public static com.runecraft.osrs.content.BossDef dataBoss(EntityType<?> type) {
        return DATA_BOSSES.get(type);
    }

    /** Entity type for a RuneCraft creature id (built-in, module monster or boss), or a vanilla "minecraft:" id. */
    public static EntityType<?> typeById(String id) {
        if (id.contains(":")) {
            return Registries.ENTITY_TYPE.get(new net.minecraft.util.Identifier(id));
        }
        return BY_ID.get(id);
    }
}
