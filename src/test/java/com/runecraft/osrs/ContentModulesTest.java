package com.runecraft.osrs;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validates everything content modules contribute. */
class ContentModulesTest {
    private static boolean placeExists(String place) {
        return place == null || Landmark.byName(place) != null || ContentRegistry.place(place) != null;
    }

    private static boolean creatureExists(String id) {
        return id == null || id.startsWith("minecraft:") || ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null || com.runecraft.osrs.data.Boss.byEntity(id) != null;
    }

    @Test
    void modulesLoad() {
        assertNotNull(ContentRegistry.monsters());
        assertNotNull(ContentRegistry.bosses());
        assertNotNull(ContentRegistry.quests());
        assertNotNull(ContentRegistry.models());
        assertTrue(ItemCatalog.all().size() > 400);
        assertTrue(Recipes.all().size() > 100);
    }

    @Test
    void monstersAndBossesAreConsistent() {
        Set<String> ids = new HashSet<>();
        for (com.runecraft.osrs.data.Monster m : com.runecraft.osrs.data.Monster.values()) {
            ids.add(m.entity());
        }
        for (com.runecraft.osrs.data.Boss b : com.runecraft.osrs.data.Boss.values()) {
            ids.add(b.entityId());
        }
        for (MonsterDef m : ContentRegistry.monsters()) {
            assertTrue(ids.add(m.id), "duplicate creature id " + m.id);
            assertTrue(m.id.matches("[a-z0-9_]+"), m.id);
            assertTrue(m.hitpoints > 0, m.id);
            assertTrue(!m.attacks.isEmpty(), m.id + " has no attacks");
            for (Drop d : m.drops) {
                assertTrue(ItemCatalog.exists(d.item()), m.id + " drops unknown " + d.item());
            }
            assertTrue(m.model == null || ContentRegistry.models().containsKey(m.model), m.id + " model " + m.model);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            assertTrue(ids.add(b.id), "duplicate creature id " + b.id);
            assertTrue(b.id.matches("[a-z0-9_]+"), b.id);
            assertTrue(!b.attacks.isEmpty(), b.id + " has no attacks");
            for (Drop d : b.drops) {
                assertTrue(ItemCatalog.exists(d.item()), b.id + " drops unknown " + d.item());
            }
            for (BossDef.Phase p : b.phases) {
                for (AttackDef a : p.attacks()) {
                    assertNotNull(a.style(), b.id);
                }
            }
            assertTrue(b.key == null || ItemCatalog.get(b.key) != null, b.id + " key " + b.key);
            assertTrue(b.pet == null || ItemCatalog.get(b.pet) != null, b.id + " pet " + b.pet);
            assertTrue(b.raidOnly || placeExists(b.place), b.id + " place " + b.place);
            assertTrue(creatureExists(b.minion), b.id + " minion " + b.minion);
            assertTrue(b.model == null || ContentRegistry.models().containsKey(b.model), b.id + " model " + b.model);
        }
    }

    @Test
    void idlePlaceholderIsNeverARealAttack() {
        // A boss script's movement-only placeholder: the engine performs nothing for it, so it must never be a listed attack.
        AttackDef idle = AttackDef.idle(AttackType.CRUSH);
        assertTrue(idle.isIdle() && idle.melee() && idle.maxHit() == 0);
        assertTrue(!AttackDef.idle(AttackType.MAGIC).melee());
        for (MonsterDef m : ContentRegistry.monsters()) {
            assertTrue(m.attacks.stream().noneMatch(AttackDef::isIdle), m.id + " lists the idle placeholder");
        }
        for (BossDef b : ContentRegistry.bosses()) {
            assertTrue(b.attacks.stream().noneMatch(AttackDef::isIdle), b.id + " lists the idle placeholder");
            for (BossDef.Phase p : b.phases) {
                assertTrue(p.attacks().stream().noneMatch(AttackDef::isIdle), b.id + " phase lists the idle placeholder");
            }
        }
    }

    @Test
    void npcsPlacesAndModels() {
        for (NpcDef n : ContentRegistry.npcs()) {
            assertTrue(placeExists(n.place()), n.id() + " place " + n.place());
            assertTrue(n.id().matches("[a-z0-9_]+"), n.id());
        }
        for (PlaceDef p : ContentRegistry.places()) {
            for (String block : p.blocks()) {
                assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(block)) || block.contains(":"),
                        p.id() + " block " + block);
            }
        }
        for (ModelSpec m : ContentRegistry.models().values()) {
            assertTrue(m.id().matches("[a-z0-9_]+"), m.id());
            assertTrue(m.texWidth() <= 1024, m.id());
        }
        for (QuestInfo q : ContentRegistry.quests()) {
            assertTrue(q.finalStage() > 0, q.questId());
            for (String reward : q.itemRewards()) {
                assertNotNull(ItemCatalog.get(reward.split(":")[0]), q.questId() + " reward " + reward);
            }
        }
    }
}
