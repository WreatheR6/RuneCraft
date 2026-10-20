package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The master and late experienced quests' steps, puzzles and boss rules against the OSRS Wiki and quest-helper. */
class MasterQuestsTest {
    private static final QuestBook BOOK = QuestBook.get();

    private static QuestSpec spec(String id) {
        QuestSpec q = BOOK.spec(id);
        assertNotNull(q, id);
        return q;
    }

    /** The strict walk's steps as "kind target" in order. */
    private static List<String> walk(String id) {
        QuestStrictWalk.Plan plan = QuestStrictWalk.plan(spec(id), "tester");
        assertTrue(plan.completes(), plan.stuck());
        return plan.steps().stream().map(s -> s.kind().name().toLowerCase(java.util.Locale.ROOT) + " " + s.target()).toList();
    }

    /** {@code beats} appear in the walk in this order (other steps may come between them). */
    private static void inOrder(String id, String... beats) {
        List<String> steps = walk(id);
        int at = 0;
        for (String beat : beats) {
            int found = -1;
            for (int i = at; i < steps.size(); i++) {
                if (steps.get(i).equals(beat)) {
                    found = i;
                    break;
                }
            }
            assertTrue(found >= 0, id + ": no \"" + beat + "\" after step " + at + " in " + steps);
            at = found + 1;
        }
    }

    // ------------------------------------------------------------------ puzzles

    @Test
    void slidingBoardsSolveInTheirShortestMoves() {
        int[] board = {0, 1, 3, 5, 2, 7, 4, 8, 6};
        List<List<Integer>> sols = QuestPuzzle.shortestSlides(board);
        assertFalse(sols.isEmpty());
        for (List<Integer> s : sols) {
            int[] b = board;
            for (int m : s) {
                b = QuestPuzzle.slideOnce(b, m);
                assertNotNull(b);
            }
            assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0}, b);
        }
        // the gap in the top-left corner has nothing above it or left of it to slide in
        assertNull(QuestPuzzle.slideOnce(board, 0));
        assertNull(QuestPuzzle.slideOnce(board, 2));
        assertEquals(10, sols.get(0).size());
        for (QuestPuzzle.Variant v : KandarinGnomeQuests.MM_PANEL.rooms.get(0).variants()) {
            assertEquals(4, v.options().size());
            assertEquals(1, v.check(v.solutions().get(0)));
        }
    }

    // ------------------------------------------------------------------ Desert Treasure I

    @Test
    void desertTreasureWalksTheWikiRoute() {
        inOrder("desert_treasure_i", "talk archaeologist", "talk terry_balando", "talk archaeologist", "talk dt_bartender", "talk eblis",
                "reach dt_mirrors",
                "reach smoke_dungeon", "kill fareed",
                "talk rasolo", "reach bandit_camp", "talk rasolo", "kill damis",
                "talk troll_child", "kill ice_troll", "kill kamil", "reach ice_path", "talk dt_troll_mother",
                "talk malak", "talk ruantun", "talk high_priest_entrana", "talk malak", "reach dessous_graveyard", "kill dessous", "talk malak",
                "reach quest_jaldraocht_pyramid", "reach quest_jaldraocht_pyramid", "talk azzanadra");
    }

    @Test
    void desertTreasureBossRules() {
        assertTrue(QuestBossRules.spellHurts("fareed", "water"));
        assertFalse(QuestBossRules.spellHurts("fareed", "fire"));
        assertFalse(QuestBossRules.spellHurts("fareed", null));
        assertTrue(QuestBossRules.spellHurts("kamil", "fire"));
        assertFalse(QuestBossRules.spellHurts("kamil", "water"));
        assertTrue(QuestBossRules.spellHurts("damis", null));
        // Damis: two forms (DPS calculator), no immunities; his first form is the BossDef
        BossDef damis = ContentRegistry.boss("damis");
        assertEquals(QuestBossRules.DAMIS_FIRST[0], damis.combatLevel);
        assertEquals(QuestBossRules.DAMIS_FIRST[1], damis.hitpoints);
        assertTrue(damis.phases.isEmpty(), "the second form is a transformation, not a damage phase");
        assertEquals(List.of(174, 200), List.of(QuestBossRules.DAMIS_SECOND[0], QuestBossRules.DAMIS_SECOND[1]));
        // Dessous: melee 19 and the bat swarm
        BossDef dessous = ContentRegistry.boss("dessous");
        assertTrue(dessous.attacks.stream().anyMatch(a -> a.name().equals("bat_swarm") && a.maxHit() == QuestBossRules.DESSOUS_BATS_MAX));
        assertTrue(dessous.attacks.stream().anyMatch(a -> a.name().equals("vampyric_slash") && a.maxHit() == QuestBossRules.DESSOUS_MELEE_MAX));
        // the pot of blood is made once: the chain stops once it has been poured
        QuestSpec q = spec("desert_treasure_i");
        for (Trigger t : q.stages.get(2).triggers) {
            if (t.give.containsKey("dt_silver_pot") || t.give.containsKey("dt_blessed_pot") || t.give.containsKey("dt_pot_of_blood")) {
                assertTrue(t.unlessFlags.contains("dt_blood_poured"), QuestWalk.describe(t));
            }
        }
    }

    // ------------------------------------------------------------------ Monkey Madness I

    @Test
    void monkeyMadnessWalksTheWikiRoute() {
        inOrder("monkey_madness_i", "talk king_narnode", "talk glo_caranock", "talk king_narnode", "talk daero",
                "puzzle gnome_glider_hangar", "talk waydar", "talk mm_lumdo",
                "reach ape_atoll_jail", "talk garkor",
                "reach ape_atoll_crates", "reach ape_atoll_crates", "talk zooknock", "reach ape_atoll_temple", "talk monkey_child", "talk zooknock",
                "talk monkey_minder", "talk elder_guard", "talk mm_kruk", "talk king_awowogei",
                "talk garkor", "kill jungle_demon", "talk king_narnode");
        QuestSpec q = spec("monkey_madness_i");
        assertTrue(q.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("jungle_demon")
                && t.need.containsKey("tenth_squad_sigil")));
        assertEquals(20, QuestBossRules.JUNGLE_DEMON_REGEN_PERCENT);
    }
}
