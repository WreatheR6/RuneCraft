package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multi-kill quest objectives (OSRS Wiki quick guides) and the quest creatures added so steps are real kills
 * instead of place visits (round 24).
 */
class QuestKillCountTest {
    private static final QuestBook BOOK = QuestBook.get();

    private static Trigger find(String quest, Trigger.Kind kind, String target) {
        QuestSpec q = BOOK.spec(quest);
        assertNotNull(q, quest);
        for (Trigger t : q.triggers()) {
            if (t.kind == kind && t.target.equals(target)) {
                return t;
            }
        }
        throw new AssertionError(quest + " has no " + kind + " " + target);
    }

    @Test
    void multiKillObjectivesUseWikiCounts() {
        assertEquals(5, find("desert_treasure_i", Trigger.Kind.KILL, "ice_troll").count);
        assertEquals(4, find("monkey_madness_ii", Trigger.Kind.KILL, "tortured_gorilla").count);
        assertEquals(2, find("dragon_slayer_ii", Trigger.Kind.KILL, "green_dragon").count);
        assertEquals(2, find("dragon_slayer_ii", Trigger.Kind.KILL, "blue_dragon").count);
        for (String dragon : new String[]{"black_dragon", "steel_dragon", "brutal_red_dragon", "mithril_dragon", "adamant_dragon", "rune_dragon"}) {
            assertEquals(1, find("dragon_slayer_ii", Trigger.Kind.KILL, dragon).count, dragon);
        }
        assertEquals(5, find("recipe_for_disaster", Trigger.Kind.DROP, "mudskipper").count);
        assertEquals(4, find("recipe_for_disaster", Trigger.Kind.KILL, "rfd_hell_rat").count);
        assertEquals(5, find("a_souls_bane", Trigger.Kind.KILL, "quest_fear_reaper").count);
        // Nung takes all five hides.
        assertEquals(5, find("recipe_for_disaster", Trigger.Kind.REACH, "rfd_crab_pen").take.get("rfd_mudskipper_hide"));
    }

    @Test
    void questCreaturesAreRealKills() {
        String[][] kills = {
                {"recipe_for_disaster", "rfd_big_snake"}, {"recipe_for_disaster", "rfd_crab"}, {"recipe_for_disaster", "rfd_jubbly_bird"},
                {"recipe_for_disaster", "rfd_hell_rat"}, {"song_of_the_elves", "iorwerth_warrior"}, {"mournings_end_part_i", "me_mourner"},
                {"dream_mentor", "the_inadequacy"}, {"dream_mentor", "the_everlasting"}, {"dream_mentor", "the_untouchable"},
                {"dream_mentor", "the_illusive"},
        };
        for (String[] k : kills) {
            find(k[0], Trigger.Kind.KILL, k[1]);
            BossDef boss = ContentRegistry.boss(k[1]);
            assertNotNull(boss, k[1]);
            assertNotNull(boss.place, k[1] + " has no summoning spot");
        }
        // OSRS Wiki infobox levels and hitpoints.
        int[][] stats = {{84, 120}, {21, 18}, {9, 20}, {108, 105}, {108, 105}, {343, 180}, {223, 230}, {274, 90}, {108, 144}, {159, 120}, {167, 130}};
        String[] ids = {"rfd_big_snake", "rfd_crab", "rfd_jubbly_bird", "iorwerth_warrior", "me_mourner", "the_inadequacy", "the_everlasting",
                "the_untouchable", "the_illusive", "rfd_evil_chicken", "fareed"};
        for (int i = 0; i < ids.length; i++) {
            BossDef b = ContentRegistry.boss(ids[i]);
            assertEquals(stats[i][0], b.combatLevel, ids[i] + " level");
            assertEquals(stats[i][1], b.hitpoints, ids[i] + " hitpoints");
        }
        // The Evil Chicken answers an offering of a raw chicken.
        assertTrue(find("recipe_for_disaster", Trigger.Kind.KILL, "rfd_evil_chicken").take.containsKey("minecraft:chicken"));
    }

    @Test
    void dreamMentorAndLunarItems() {
        assertTrue(BOOK.spec("dream_mentor").triggers().stream().anyMatch(t -> t.target.equals("dm_moonclan_sink") && t.take.containsKey("goutweed")));
        assertTrue(BOOK.spec("eadgars_ruse").triggers().stream().anyMatch(t -> t.target.equals("burntmeat") && t.give.containsKey("goutweed")));
        boolean lantern = false;
        for (Trigger t : BOOK.spec("lunar_diplomacy").triggers()) {
            lantern |= t.target.equals("lokar_searunner") && t.travel != null && t.need.containsKey("ld_emerald_lantern");
        }
        assertTrue(lantern, "the Lunar ship needs the emerald lantern");
    }

    @Test
    void timesOnlyAppliesToKillsAndDrops() {
        QuestSpec q = new QuestSpec("x_test", "X", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1);
        q.stage("a").talk("someone");
        assertThrows(IllegalStateException.class, () -> q.times(3));
        q.kill("goblin").times(3);
        assertEquals(3, q.stages.get(0).triggers.get(1).count);
    }
}
