package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The quest list matches OSRS: every quest and miniquest exists with its real quest points. */
class QuestListCompletenessTest {
    /** Quests added to complete the list, with their OSRS quest points. */
    private static final Map<String, Integer> LATER_QUESTS = Map.ofEntries(
            Map.entry("death_to_the_dorgeshuun", 1), Map.entry("another_slice_of_ham", 1), Map.entry("land_of_the_goblins", 2),
            Map.entry("between_a_rock", 2), Map.entry("forgettable_tale", 2), Map.entry("enakhras_lament", 2),
            Map.entry("devious_minds", 1), Map.entry("the_garden_of_death", 1), Map.entry("secrets_of_the_north", 2),
            Map.entry("desert_treasure_ii", 5), Map.entry("the_curse_of_arrav", 2), Map.entry("my_arms_big_adventure", 1),
            Map.entry("making_friends_with_my_arm", 2), Map.entry("tai_bwo_wannai_trio", 2), Map.entry("troll_romance", 2),
            Map.entry("one_small_favour", 2), Map.entry("rag_and_bone_man_ii", 1), Map.entry("a_souls_bane", 1),
            Map.entry("the_great_brain_robbery", 2), Map.entry("the_fremennik_exiles", 2), Map.entry("a_night_at_the_theatre", 2),
            Map.entry("a_porcine_of_interest", 1), Map.entry("sleeping_giants", 1), Map.entry("pandemonium", 1),
            Map.entry("children_of_the_sun", 1), Map.entry("twilights_promise", 1), Map.entry("perilous_moons", 2),
            Map.entry("at_first_light", 1), Map.entry("the_ribbiting_tale_of_a_lily_pad_labour_dispute", 1),
            Map.entry("death_on_the_isle", 2), Map.entry("meat_and_greet", 1), Map.entry("the_heart_of_darkness", 2),
            Map.entry("ethically_acquired_antiquities", 1), Map.entry("scrambled", 1), Map.entry("shadows_of_custodia", 1),
            Map.entry("the_final_dawn", 2));

    private static final String[] MINIQUESTS = {"alfred_grimhands_barcrawl", "enter_the_abyss", "architectural_alliance",
            "barbarian_training", "bear_your_soul", "curse_of_the_empty_lord", "daddys_home", "enchanted_key", "family_pest",
            "the_frozen_door", "the_generals_shadow", "his_faithful_servants", "hopespears_will", "in_search_of_knowledge",
            "into_the_tombs", "lair_of_tarn_razorlor", "mage_arena_i", "mage_arena_ii", "skippy_and_the_mogres", "vale_totems"};

    @Test
    void laterQuestsExistWithRealQuestPoints() {
        LATER_QUESTS.forEach((id, qp) -> {
            QuestInfo q = ContentRegistry.quest(id);
            assertNotNull(q, id);
            assertEquals((int) qp, q.questPoints(), id);
        });
    }

    @Test
    void miniquestsGiveNoQuestPoints() {
        for (String id : MINIQUESTS) {
            QuestInfo q = ContentRegistry.quest(id);
            assertNotNull(q, id);
            assertEquals(0, q.questPoints(), id);
            assertTrue(q.displayName().endsWith("(miniquest)"), id);
        }
    }

    @Test
    void totalQuestPointsMatchOsrs() {
        // 321 quest points through The Curse of Arrav, plus later releases (Scrambled!, Shadows of Custodia, The Final Dawn, Pandemonium).
        assertEquals(326, Quest.totalQuestPoints());
        assertEquals(304, QuestBook.get().questPoints());
        assertTrue(ContentRegistry.quests().size() >= 196, "quests: " + ContentRegistry.quests().size());
    }

    @Test
    void faithfulRequirements() {
        QuestSpec dt2 = QuestBook.get().spec("desert_treasure_ii");
        assertTrue(dt2.def.questRequirements().containsAll(java.util.List.of("desert_treasure_i", "secrets_of_the_north",
                "enakhras_lament", "temple_of_the_eye", "the_garden_of_death", "below_ice_mountain", "his_faithful_servants")));
        assertEquals(75, dt2.def.requirements().get(Skill.MAGIC));
        assertTrue(QuestBook.get().spec("swan_song").def.questRequirements().contains("one_small_favour"));
        assertTrue(QuestBook.get().spec("the_generals_shadow").def.questRequirements().contains("curse_of_the_empty_lord"));
        assertEquals(60, QuestBook.get().spec("mage_arena_i").def.requirements().get(Skill.MAGIC));
    }
}
