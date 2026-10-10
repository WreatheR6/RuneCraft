package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.QuestDef;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validates every quest script: NPCs, items, bosses, places and quest requirements must exist. */
class QuestsContentTest {
    private static final QuestBook BOOK = QuestBook.get();

    private static boolean itemExists(String id) {
        return id.startsWith("minecraft:") || ItemCatalog.exists(id);
    }

    private static boolean placeExists(String place) {
        return Landmark.byName(place) != null || ContentRegistry.place(place) != null;
    }

    private static boolean creatureExists(String id) {
        if (id.startsWith("minecraft:") || ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null
                || com.runecraft.osrs.data.Monster.byEntity(id) != null || com.runecraft.osrs.data.Boss.byEntity(id) != null) {
            return true;
        }
        for (com.runecraft.osrs.data.Monster m : com.runecraft.osrs.data.Monster.values()) {
            if (m.displayName().toLowerCase().replace(' ', '_').equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void questsAreRegistered() {
        assertTrue(BOOK.quests.size() >= 40, "only " + BOOK.quests.size() + " quests");
        for (String id : new String[]{"dragon_slayer_i", "lost_city", "recipe_for_disaster", "desert_treasure_i", "lunar_diplomacy",
                "monkey_madness_i", "fremennik_trials", "priest_in_peril", "animal_magnetism", "waterfall_quest"}) {
            assertNotNull(ContentRegistry.quest(id), id);
        }
    }

    @Test
    void everyTriggerReferencesRealThings() {
        Set<String> npcIds = new HashSet<>();
        BOOK.npcs.forEach(n -> npcIds.add(n.id()));
        for (QuestSpec q : BOOK.quests) {
            assertTrue(q.id().matches("[a-z0-9_]+"), q.id());
            assertFalse(q.stages.isEmpty(), q.id());
            assertEquals(q.stages.size(), q.def.finalStage(), q.id());
            for (String req : q.def.questRequirements()) {
                assertNotNull(ContentRegistry.quest(req), q.id() + " requires unknown quest " + req);
                assertFalse(req.equals(q.id()), q.id() + " requires itself");
            }
            for (String reward : q.def.itemRewards()) {
                assertTrue(itemExists(reward.split(":")[0]), q.id() + " reward " + reward);
            }
            // Stage 0 must be startable by talking to someone.
            assertTrue(q.stages.get(0).triggers.stream().anyMatch(t -> t.kind == QuestSpec.Trigger.Kind.TALK), q.id() + " has no start");
            boolean completes = false;
            for (QuestSpec.Stage s : q.stages) {
                assertFalse(s.journal.isBlank(), q.id());
                boolean advances = false;
                for (QuestSpec.Trigger t : s.triggers) {
                    String where = q.id() + " stage " + s.index + " " + t.kind + " " + t.target;
                    switch (t.kind) {
                        case TALK -> assertTrue(npcIds.contains(t.target), where + ": unknown NPC");
                        case KILL, DROP -> assertTrue(creatureExists(t.target), where + ": unknown creature");
                        case REACH -> assertTrue(placeExists(t.target), where + ": unknown place");
                    }
                    for (String item : t.items()) {
                        assertTrue(itemExists(item), where + ": unknown item " + item);
                    }
                    if (t.spawn != null) {
                        assertNotNull(ContentRegistry.boss(t.spawn), where + ": unknown boss " + t.spawn);
                    }
                    if (t.travel != null) {
                        assertTrue(placeExists(t.travel), where + ": unknown travel place " + t.travel);
                    }
                    for (String req : t.quests) {
                        assertNotNull(ContentRegistry.quest(req), where + ": unknown quest " + req);
                    }
                    assertTrue(t.next == QuestSpec.Trigger.COMPLETE || (t.next >= 0 && t.next < q.stages.size()), where + " next " + t.next);
                    if (t.next != s.index) {
                        advances = true;
                        assertTrue(t.next == QuestSpec.Trigger.COMPLETE || t.next > s.index, where + " goes backwards");
                    }
                    completes |= t.completes();
                }
                assertTrue(advances, q.id() + " stage " + s.index + " has no way forward");
            }
            assertTrue(completes, q.id() + " can never be completed");
            for (String npc : q.doneLines.keySet()) {
                assertTrue(npcIds.contains(npc), q.id() + " done lines for unknown NPC " + npc);
            }
            q.stages.forEach(s -> s.hints.keySet().forEach(npc -> assertTrue(npcIds.contains(npc), q.id() + " hint for unknown " + npc)));
        }
    }

    @Test
    void flagsThatAreNeededAreSet() {
        for (QuestSpec q : BOOK.quests) {
            Set<String> set = new HashSet<>();
            q.triggers().forEach(t -> set.addAll(t.setFlags));
            q.triggers().forEach(t -> t.needFlags.forEach(f -> assertTrue(set.contains(f), q.id() + " needs unset flag " + f)));
        }
    }

    @Test
    void npcsAreUniqueAndPlaced() {
        Set<String> spots = new HashSet<>();
        for (QuestNpc n : QuestNpc.values()) {
            spots.add(n.landmark().name() + "@" + n.dx() + "," + n.dz());
        }
        Set<String> looks = new HashSet<>();
        for (NpcDef n : BOOK.npcs) {
            assertTrue(placeExists(n.place()), n.id() + " at unknown place " + n.place());
            assertTrue(spots.add(n.place() + "@" + n.dx() + "," + n.dz()), n.id() + " stands on another NPC");
            assertTrue(looks.add(Arrays.toString(n.colors())), n.id() + " looks identical to another NPC");
        }
        // Every NPC is used by some quest.
        Set<String> used = new HashSet<>();
        for (QuestSpec q : BOOK.quests) {
            q.triggers().forEach(t -> used.add(t.target));
            used.addAll(q.doneLines.keySet());
            q.stages.forEach(s -> used.addAll(s.hints.keySet()));
        }
        for (NpcDef n : BOOK.npcs) {
            assertTrue(used.contains(n.id()) || n.id().equals("wise_old_man"), n.id() + " is never used");
        }
    }

    @Test
    void bossesAreQuestBossesWithModels() {
        Set<String> killable = new HashSet<>();
        for (QuestSpec q : BOOK.quests) {
            q.triggers().forEach(t -> {
                if (t.kind == QuestSpec.Trigger.Kind.KILL) {
                    killable.add(t.target);
                }
            });
        }
        Map<String, ModelSpec> models = new HashMap<>();
        BOOK.models.forEach(m -> models.put(m.id(), m));
        for (BossDef b : BOOK.bosses) {
            assertEquals("Quest bosses", b.category, b.id);
            assertTrue(killable.contains(b.id), b.id + " is never a quest objective");
        }
        for (ModelSpec m : BOOK.models) {
            assertNotNull(ContentRegistry.boss(m.id()), "model " + m.id() + " has no boss");
            assertTrue(m.parts().stream().mapToInt(p -> p.boxes.size()).sum() <= 40, m.id() + " too many boxes");
        }
        for (String id : new String[]{"elvarg", "dagannoth_mother", "galvek", "kamil", "damis", "fareed", "dessous", "vanstrom_klause"}) {
            assertNotNull(ContentRegistry.boss(id), id);
        }
        for (MonsterDef m : BOOK.monsters) {
            assertTrue(m.spawns.isEmpty() || !m.attacks.isEmpty(), m.id);
        }
    }

    @Test
    void gearAndGates() {
        BOOK.gear.forEach((id, g) -> assertTrue(ItemCatalog.exists(id), "gear " + id));
        BOOK.weaponGates.forEach((weapon, quest) -> {
            assertTrue(ItemCatalog.exists(weapon), weapon);
            assertNotNull(ContentRegistry.quest(quest), quest);
        });
        QuestBook.Gear gloves = BOOK.gear.get("barrows_gloves");
        assertEquals(12, gloves.stats().stabAttack());
        assertEquals(6, gloves.stats().magicAttack());
        assertEquals(12, gloves.stats().meleeStrength());
    }

    @Test
    void placesUseRealCoordinates() {
        for (PlaceDef p : BOOK.places) {
            assertTrue(p.osrsX() > 1000 && p.osrsX() < 4000 && p.osrsY() > 2400 && p.osrsY() < 4100, p.id() + " coordinates");
        }
        int qp = 0;
        for (QuestDef d : BOOK.questDefs()) {
            qp += d.questPoints();
        }
        assertEquals(qp, BOOK.questPoints());
        assertTrue(qp >= 100, "quest points " + qp);
    }
}
