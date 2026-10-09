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
                    b.defence(), b.magic(), b.ranged(), b.defenceBonus(), b.attackSpeed(), b.style(), b.maxHit(), null));
            BOSSES.put(type, b);
            Registry.register(Registries.ENTITY_TYPE, RuneCraft.id(b.entityId()), type);
            BOSS_TYPES.put(b, type);
        }
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
}
