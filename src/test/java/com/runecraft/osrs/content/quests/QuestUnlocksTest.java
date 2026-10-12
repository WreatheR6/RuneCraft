package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Quest unlocks: RFD glove tiers, quest-locked gear and shops, quest-taught prayers, faithful rewards. */
class QuestUnlocksTest {
    private static final QuestBook BOOK = QuestBook.get();

    private static QuestBook.Shop shop(String npc, String item) {
        return BOOK.shops.stream().filter(s -> s.npc().equals(npc) && s.item().equals(item)).findFirst().orElseThrow();
    }

    @Test
    void rfdGlovesUnlockByNumberOfSubquests() {
        // Freeing only King Awowogei (any one council member) unlocks bronze gloves, not dragon gloves.
        Set<String> one = Set.of("rfd_awowogei");
        assertTrue(shop("rfd_gypsy", "rfd_bronze_gloves").flagsMet(one::contains));
        assertFalse(shop("rfd_gypsy", "rfd_iron_gloves").flagsMet(one::contains));
        assertFalse(shop("rfd_gypsy", "rfd_dragon_gloves").flagsMet(one::contains));
        Set<String> seven = Set.of("rfd_dwarf", "rfd_goblins", "rfd_pete", "rfd_guide", "rfd_dave", "rfd_skrach", "rfd_awowogei");
        assertTrue(shop("rfd_gypsy", "rfd_rune_gloves").flagsMet(seven::contains));
        assertFalse(shop("rfd_gypsy", "rfd_dragon_gloves").flagsMet(seven::contains));
        assertEquals("recipe_for_disaster", shop("rfd_gypsy", "barrows_gloves").quest());
    }

    @Test
    void questLockedGearAndShops() {
        assertEquals("dragon_slayer_i", BOOK.armourGates.get("rune_platebody"));
        assertEquals("dragon_slayer_i", BOOK.armourGates.get("green_dhide_body"));
        assertEquals("lost_city", BOOK.weaponGates.get("dragon_longsword"));
        assertEquals("lost_city", BOOK.weaponGates.get("dragon_dagger"));
        assertEquals("monkey_madness_i", BOOK.weaponGates.get("dragon_scimitar"));
        assertEquals("dragon_slayer_i", shop("oziach", "rune_platebody").quest());
        assertEquals("lost_city", shop("jukat", "dragon_longsword").quest());
        assertEquals(100000, shop("jukat", "dragon_longsword").price());
        assertEquals(30000, shop("jukat", "dragon_dagger").price());
        assertEquals("monkey_madness_i", shop("daga", "dragon_scimitar").quest());
    }

    @Test
    void chivalryAndPietyNeedKingsRansom() {
        SkillSet maxed = new SkillSet();
        for (Skill s : Skill.values()) {
            maxed.setXpTenths(s, 130_344_310);
        }
        assertNotNull(QuestUnlocks.prayer(Prayer.PIETY, maxed, q -> false));
        assertNotNull(QuestUnlocks.prayer(Prayer.CHIVALRY, maxed, q -> false));
        assertNull(QuestUnlocks.prayer(Prayer.PIETY, maxed, q -> q.equals("kings_ransom")));
        assertNull(QuestUnlocks.prayer(Prayer.PROTECT_FROM_MELEE, maxed, q -> false));
        SkillSet low = new SkillSet();
        assertNotNull(QuestUnlocks.prayer(Prayer.PIETY, low, q -> true), "Piety needs 70 Defence");
        assertNull(QuestUnlocks.prayer(Prayer.THICK_SKIN, low, q -> false));
    }

    @Test
    void faithfulRewardsAndRequirements() {
        QuestSpec sote = BOOK.spec("song_of_the_elves");
        for (Skill s : List.of(Skill.AGILITY, Skill.CONSTRUCTION, Skill.FARMING, Skill.HERBLORE, Skill.HUNTER, Skill.MINING,
                Skill.SMITHING, Skill.WOODCUTTING)) {
            assertEquals(20000.0, sote.def.xpRewards().get(s), "Song of the Elves " + s);
        }
        assertEquals(5000.0, BOOK.spec("the_fremennik_isles").def.xpRewards().get(Skill.CRAFTING));
        // MM2 needs RFD's Awowogei subquest; The Great Brain Robbery needs Pirate Pete; Dream Mentor combat 85.
        assertTrue(BOOK.spec("monkey_madness_ii").stages.get(0).triggers.get(0).needFlags.contains("rfd_awowogei"));
        assertTrue(BOOK.spec("the_great_brain_robbery").stages.get(0).triggers.get(0).needFlags.contains("rfd_pete"));
        assertEquals(85, BOOK.spec("dream_mentor").stages.get(0).triggers.get(0).minCombat);
        // Quest points of the audited quests.
        String[][] qp = {{"dragon_slayer_i", "2"}, {"lost_city", "3"}, {"heroes_quest", "1"}, {"legends_quest", "4"},
                {"recipe_for_disaster", "10"}, {"desert_treasure_i", "3"}, {"monkey_madness_i", "3"}, {"monkey_madness_ii", "4"},
                {"dragon_slayer_ii", "5"}, {"song_of_the_elves", "4"}, {"sins_of_the_father", "2"}, {"lunar_diplomacy", "2"},
                {"dream_mentor", "2"}, {"fremennik_trials", "3"}, {"the_fremennik_isles", "1"}, {"underground_pass", "5"},
                {"regicide", "3"}, {"the_grand_tree", "5"}, {"waterfall_quest", "1"}, {"tree_gnome_village", "2"},
                {"plague_city", "1"}, {"biohazard", "3"}, {"priest_in_peril", "1"}, {"animal_magnetism", "1"},
                {"horror_from_the_deep", "2"}, {"bone_voyage", "1"}, {"client_of_kourend", "1"}, {"a_kingdom_divided", "2"},
                {"black_knights_fortress", "3"}, {"ernest_the_chicken", "4"}, {"goblin_diplomacy", "5"}, {"pirates_treasure", "2"},
                {"prince_ali_rescue", "3"}, {"the_knights_sword", "1"}, {"shield_of_arrav", "1"}, {"kings_ransom", "1"}};
        for (String[] e : qp) {
            assertEquals(Integer.parseInt(e[1]), BOOK.spec(e[0]).def.questPoints(), e[0]);
        }
    }
}
