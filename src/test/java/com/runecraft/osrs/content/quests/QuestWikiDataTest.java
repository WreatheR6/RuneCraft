package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.QuestDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quest data checked against the OSRS Wiki (October 2026, cross-checked with the RuneLite Quest Helper
 * plugin's requirement and reward data): every quest and miniquest with its quest points, plus the
 * requirements and XP rewards that the quest audit corrected.
 */
class QuestWikiDataTest {
    /** Every OSRS quest and miniquest with its quest points. */
    private static final Object[][] QUEST_POINTS = {
            {"COOKS_ASSISTANT", 1}, {"SHEEP_SHEARER", 1}, {"DORICS_QUEST", 1}, {"RUNE_MYSTERIES", 1}, {"RESTLESS_GHOST", 1},
            {"IMP_CATCHER", 1}, {"WITCHS_POTION", 1}, {"ROMEO_AND_JULIET", 5}, {"DRUIDIC_RITUAL", 4}, {"VAMPYRE_SLAYER", 3},
            {"DEMON_SLAYER", 3}, {"dragon_slayer_i", 2}, {"black_knights_fortress", 3}, {"ernest_the_chicken", 4},
            {"goblin_diplomacy", 5}, {"pirates_treasure", 2}, {"prince_ali_rescue", 3}, {"the_knights_sword", 1},
            {"shield_of_arrav", 1}, {"x_marks_the_spot", 1}, {"below_ice_mountain", 1}, {"misthalin_mystery", 1},
            {"the_corsair_curse", 2}, {"lost_city", 3}, {"waterfall_quest", 1}, {"merlins_crystal", 6}, {"holy_grail", 2},
            {"family_crest", 1}, {"heroes_quest", 1}, {"sheep_herder", 4}, {"plague_city", 1}, {"biohazard", 3},
            {"fight_arena", 2}, {"hazeel_cult", 1}, {"clock_tower", 1}, {"witchs_house", 4}, {"tree_gnome_village", 2},
            {"the_grand_tree", 5}, {"monkey_madness_i", 3}, {"jungle_potion", 1}, {"shilo_village", 2}, {"legends_quest", 4},
            {"fremennik_trials", 3}, {"lunar_diplomacy", 2}, {"mountain_daughter", 2}, {"horror_from_the_deep", 2},
            {"the_dig_site", 2}, {"temple_of_ikov", 1}, {"the_tourist_trap", 2}, {"desert_treasure_i", 3},
            {"bone_voyage", 1}, {"temple_of_the_eye", 1}, {"priest_in_peril", 1}, {"nature_spirit", 2},
            {"animal_magnetism", 1}, {"creature_of_fenkenstrain", 2}, {"ghosts_ahoy", 2}, {"haunted_mine", 2},
            {"cabin_fever", 2}, {"in_search_of_the_myreque", 2}, {"darkness_of_hallowvale", 2}, {"a_taste_of_hope", 1},
            {"sins_of_the_father", 2}, {"death_plateau", 1}, {"troll_stronghold", 1}, {"eadgars_ruse", 1},
            {"underground_pass", 5}, {"regicide", 3}, {"song_of_the_elves", 4}, {"monkey_madness_ii", 4},
            {"dragon_slayer_ii", 5}, {"while_guthix_sleeps", 5}, {"the_slug_menace", 1}, {"monks_friend", 1},
            {"murder_mystery", 3}, {"tribal_totem", 1}, {"observatory_quest", 2}, {"sea_slug", 1}, {"watchtower", 4},
            {"big_chompy_bird_hunting", 2}, {"elemental_workshop_i", 1}, {"zogre_flesh_eaters", 1}, {"rum_deal", 2},
            {"enlightened_journey", 1}, {"the_eyes_of_glouphrie", 2}, {"roving_elves", 1}, {"mournings_end_part_i", 2},
            {"mournings_end_part_ii", 2}, {"scorpion_catcher", 1}, {"fishing_contest", 1}, {"dwarf_cannon", 1},
            {"gertrudes_cat", 1}, {"making_history", 3}, {"dream_mentor", 2}, {"client_of_kourend", 1},
            {"tears_of_guthix", 1}, {"the_feud", 1}, {"icthlarins_little_helper", 2}, {"a_tail_of_two_cats", 2},
            {"the_golem", 1}, {"shadow_of_the_storm", 1}, {"what_lies_below", 1}, {"defender_of_varrock", 2},
            {"in_aid_of_the_myreque", 2}, {"recruitment_drive", 1}, {"the_lost_tribe", 1}, {"wanted", 1},
            {"enter_the_abyss", 0}, {"alfred_grimhands_barcrawl", 0}, {"fairytale_i_growing_pains", 2},
            {"fairytale_ii_cure_a_queen", 2}, {"garden_of_tranquillity", 2}, {"the_path_of_glouphrie", 2},
            {"elemental_workshop_ii", 1}, {"eagles_peak", 2}, {"the_giant_dwarf", 2}, {"olafs_quest", 1},
            {"the_fremennik_isles", 1}, {"throne_of_miscellania", 1}, {"royal_trouble", 1}, {"cold_war", 1},
            {"tower_of_life", 2}, {"kings_ransom", 1}, {"grim_tales", 1}, {"the_hand_in_the_sand", 1},
            {"spirits_of_the_elid", 2}, {"contact", 1}, {"beneath_cursed_sands", 2}, {"swan_song", 2}, {"ratcatchers", 2},
            {"shades_of_mortton", 3}, {"rag_and_bone_man_i", 1}, {"the_depths_of_despair", 1}, {"the_queen_of_thieves", 1},
            {"tale_of_the_righteous", 1}, {"the_forsaken_tower", 1}, {"the_ascent_of_arceuus", 1}, {"getting_ahead", 1},
            {"a_kingdom_divided", 2}, {"recipe_for_disaster", 10}, {"death_to_the_dorgeshuun", 1},
            {"another_slice_of_ham", 1}, {"land_of_the_goblins", 2}, {"between_a_rock", 2}, {"forgettable_tale", 2},
            {"enakhras_lament", 2}, {"devious_minds", 1}, {"the_garden_of_death", 1}, {"secrets_of_the_north", 2},
            {"desert_treasure_ii", 5}, {"the_curse_of_arrav", 2}, {"my_arms_big_adventure", 1},
            {"making_friends_with_my_arm", 2}, {"tai_bwo_wannai_trio", 2}, {"troll_romance", 2}, {"one_small_favour", 2},
            {"rag_and_bone_man_ii", 1}, {"a_souls_bane", 1}, {"the_great_brain_robbery", 2}, {"the_fremennik_exiles", 2},
            {"a_night_at_the_theatre", 2}, {"a_porcine_of_interest", 1}, {"sleeping_giants", 1}, {"pandemonium", 1},
            {"children_of_the_sun", 1}, {"twilights_promise", 1}, {"perilous_moons", 2}, {"at_first_light", 1},
            {"the_ribbiting_tale_of_a_lily_pad_labour_dispute", 1}, {"death_on_the_isle", 2}, {"meat_and_greet", 1},
            {"the_heart_of_darkness", 2}, {"ethically_acquired_antiquities", 1}, {"scrambled", 1},
            {"shadows_of_custodia", 2}, {"the_final_dawn", 3}, {"the_ides_of_milk", 1}, {"prying_times", 1},
            {"current_affairs", 1}, {"troubled_tortugans", 1}, {"the_red_reef", 2}, {"fallen_from_grace", 2},
            {"the_blood_moon_rises", 4}, {"architectural_alliance", 0}, {"barbarian_training", 0}, {"bear_your_soul", 0},
            {"curse_of_the_empty_lord", 0}, {"daddys_home", 0}, {"enchanted_key", 0}, {"family_pest", 0},
            {"the_frozen_door", 0}, {"the_generals_shadow", 0}, {"his_faithful_servants", 0}, {"hopespears_will", 0},
            {"in_search_of_knowledge", 0}, {"into_the_tombs", 0}, {"lair_of_tarn_razorlor", 0}, {"mage_arena_i", 0},
            {"mage_arena_ii", 0}, {"skippy_and_the_mogres", 0}, {"vale_totems", 0},
    };

    @Test
    void everyOsrsQuestExistsWithItsQuestPoints() {
        int total = 0;
        Set<String> ids = new HashSet<>();
        for (Object[] row : QUEST_POINTS) {
            String id = (String) row[0];
            QuestInfo q = ContentRegistry.quest(id);
            assertNotNull(q, id);
            assertEquals((int) (Integer) row[1], q.questPoints(), id);
            assertTrue(ids.add(id), "duplicate " + id);
            total += (Integer) row[1];
        }
        assertEquals(340, total);
        // Nothing outside the wiki list (the legacy Barrows/Fight Caves entries) gives quest points.
        for (QuestInfo q : ContentRegistry.quests()) {
            assertTrue(ids.contains(q.questId()) || q.questPoints() == 0, q.questId() + " is not an OSRS quest");
        }
    }

    private static QuestDef def(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q.def;
    }

    @Test
    void correctedRequirements() {
        assertEquals(15, def("watchtower").requirements().get(Skill.MAGIC));
        assertFalse(def("while_guthix_sleeps").requirements().containsKey(Skill.DEFENCE));
        assertTrue(def("while_guthix_sleeps").questRequirements().containsAll(List.of("fight_arena", "the_hand_in_the_sand",
                "temple_of_the_eye", "a_tail_of_two_cats", "nature_spirit")));
        assertFalse(def("while_guthix_sleeps").questRequirements().contains("legends_quest"));
        assertEquals(42, def("what_lies_below").requirements().get(Skill.MINING));
        assertEquals(Map.of(Skill.SMITHING, 55, Skill.HUNTER, 52), def("defender_of_varrock").requirements());
        assertTrue(def("throne_of_miscellania").requirements().isEmpty(), "Throne needs any one of four skills");
        assertEquals(30, def("tai_bwo_wannai_trio").requirements().get(Skill.FIREMAKING));
        assertEquals(60, def("the_fremennik_exiles").requirements().get(Skill.MINING));
        assertEquals(Map.of(Skill.SLAYER, 54, Skill.FISHING, 45, Skill.CONSTRUCTION, 41, Skill.HUNTER, 36),
                def("shadows_of_custodia").requirements());
        assertEquals(Map.of(Skill.THIEVING, 66, Skill.FLETCHING, 52, Skill.RUNECRAFT, 52), def("the_final_dawn").requirements());
        assertEquals(Map.of(Skill.CONSTRUCTION, 38, Skill.COOKING, 36, Skill.SMITHING, 35), def("scrambled").requirements());
        assertTrue(def("secrets_of_the_north").questRequirements().contains("hazeel_cult"));
        assertEquals(List.of("rag_and_bone_man_i", "skippy_and_the_mogres"), def("rag_and_bone_man_ii").questRequirements());
        assertTrue(def("scorpion_catcher").questRequirements().contains("alfred_grimhands_barcrawl"));
        assertEquals(55, def("barbarian_training").requirements().get(Skill.FISHING));
        assertEquals(20, def("skippy_and_the_mogres").requirements().get(Skill.COOKING));
        assertTrue(def("hopespears_will").questRequirements().containsAll(List.of("desert_treasure_i", "fairytale_ii_cure_a_queen")));
    }

    @Test
    void correctedXpRewards() {
        assertEquals(18400.0, def("the_grand_tree").xpRewards().get(Skill.ATTACK));
        assertEquals(7900.0, def("the_grand_tree").xpRewards().get(Skill.AGILITY));
        assertEquals(2000.0, def("mountain_daughter").xpRewards().get(Skill.PRAYER));
        assertEquals(60000.0, def("mournings_end_part_ii").xpRewards().get(Skill.AGILITY));
        assertEquals(15000.0, def("the_feud").xpRewards().get(Skill.THIEVING));
        assertEquals(80000.0, def("dragon_slayer_ii").xpRewards().get(Skill.SMITHING));
        assertEquals(80000.0, def("monkey_madness_ii").xpRewards().get(Skill.SLAYER));
        assertEquals(80000.0, def("while_guthix_sleeps").xpRewards().get(Skill.THIEVING));
        assertEquals(20000.0, def("his_faithful_servants").xpRewards().get(Skill.PRAYER));
        assertEquals(55000.0, def("the_final_dawn").xpRewards().get(Skill.THIEVING));
    }

    @Test
    void newestQuestsFollowOsrs() {
        assertTrue(def("the_ides_of_milk").requirements().isEmpty());
        assertEquals(List.of("pandemonium", "the_knights_sword"), def("prying_times").questRequirements());
        assertEquals(Map.of(Skill.SAILING, 22, Skill.FISHING, 10), def("current_affairs").requirements());
        assertEquals(51, def("troubled_tortugans").requirements().get(Skill.SLAYER));
        assertEquals(List.of("troubled_tortugans"), def("the_red_reef").questRequirements());
        assertEquals(62, def("fallen_from_grace").requirements().get(Skill.SAILING));
        assertEquals(List.of("a_night_at_the_theatre", "sins_of_the_father"), def("the_blood_moon_rises").questRequirements());
        assertEquals(74, def("the_blood_moon_rises").requirements().get(Skill.SLAYER));
        assertEquals(10, def("the_blood_moon_rises").requirements().size());
    }
}
