package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 30: the miniquests that were talk chains have their wiki objectives (stage counts unchanged: they are saved). */
class MiniquestObjectivesTest {
    private static QuestSpec spec(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q;
    }

    private static boolean has(QuestSpec q, Trigger.Kind kind, String target) {
        return q.triggers().stream().anyMatch(t -> t.kind == kind && t.target.equals(target));
    }

    @Test
    void architecturalAllianceNeedsAllFiveArchitects() {
        QuestSpec q = spec("architectural_alliance");
        assertEquals(3, q.stages.size());
        for (String a : List.of("quest_arcis", "quest_lovada", "quest_piliar", "quest_shayda")) {
            assertTrue(has(q, Trigger.Kind.TALK, a), a);
        }
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("quest_hosa")
                && t.needFlags.containsAll(List.of("aa_arcis", "aa_lovada", "aa_piliar", "aa_shayda"))));
    }

    @Test
    void curseOfTheEmptyLordVisitsTheSixGhostsForTheSixRobes() {
        QuestSpec q = spec("curse_of_the_empty_lord");
        assertEquals(3, q.stages.size());
        List<String> pieces = q.triggers().stream().flatMap(t -> t.give.keySet().stream()).toList();
        assertEquals(List.of("ghostly_robe_top", "ghostly_gloves", "ghostly_boots", "ghostly_robe_bottom", "ghostly_hood", "ghostly_cloak"), pieces);
        assertTrue(q.triggers().stream().allMatch(t -> t.need.containsKey("ring_of_visibility") && t.need.containsKey("ghostspeak_amulet")));
        assertEquals("glarials_tomb", com.runecraft.osrs.content.ContentRegistry.npc("quest_valdez").place(), "Valdez at Glarial's Tomb");
    }

    @Test
    void inSearchOfKnowledgeCollectsTheTomesAndPages() {
        QuestSpec q = spec("in_search_of_knowledge");
        assertEquals(3, q.stages.size());
        assertTrue(q.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.DROP && t.target.equals("temple_spider") && t.count == 4));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("quest_logosia") && t.completes() && t.take.size() == 3));
    }

    @Test
    void intoTheTombsNeedsTheWardensAndValeTotemsTheSpirits() {
        QuestSpec itt = spec("into_the_tombs");
        assertTrue(has(itt, Trigger.Kind.KILL, "tumekens_warden"));
        assertEquals("maisa", itt.stages.get(0).triggers.get(0).target);
        QuestSpec vt = spec("vale_totems");
        assertEquals(2, vt.stages.size());
        assertTrue(vt.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.PUZZLE && t.setFlags.contains("vt_carved")));
        assertTrue(vt.triggers().stream().anyMatch(t -> t.take.getOrDefault("oak_shortbow_u", 0) == 4 && t.completes()));
    }
}
