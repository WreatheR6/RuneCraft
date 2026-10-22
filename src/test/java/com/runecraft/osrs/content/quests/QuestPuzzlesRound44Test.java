package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestPuzzle.Variant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 44's puzzle quests (quest-helper's step lists and solvers, the wiki quick guides): the puzzles a real player solves
 * instead of a reach or talk step that did it for them.
 */
class QuestPuzzlesRound44Test {
    private static final QuestBook BOOK = QuestBook.get();

    private static boolean hasPuzzle(String quest, QuestPuzzle puzzle) {
        return BOOK.spec(quest).triggers().stream().anyMatch(t -> t.puzzle == puzzle);
    }

    /** quest-helper MetalDoorSolver: the code key's letters read through the four decoder strips' windows. */
    @Test
    void curseOfArravMetalDoorsReadTheGridThroughTheStrips() {
        // quest-helper's own example code: I through strip 1 (6th column) 0, F through strip 2 (4th) 2, C through strip 3 (8th)
        // 3, B through strip 4 (2nd) 5
        assertArrayEquals(new int[]{0, 2, 3, 5}, MahjarratQuests.coaDoorPassword("IFCB"));
        // the solver's mapping table, row A: strip 4 7, strip 2 9, strip 1 6, strip 3 4
        assertArrayEquals(new int[]{6, 9, 4, 7}, MahjarratQuests.coaDoorPassword("AAAA"));
        for (QuestPuzzle.Room r : MahjarratQuests.COA_METAL_DOOR.rooms) {
            for (Variant v : r.variants()) {
                assertEquals(1, v.solutions().size());
                assertEquals(4, v.length());
                String code = v.prompt().get(0).replaceAll(".*reads ([A-I]{4}).*", "$1");
                int[] want = MahjarratQuests.coaDoorPassword(code);
                assertEquals(1, v.check(List.of(want[0], want[1], want[2], want[3])), code);
                assertEquals(-1, v.check(List.of((want[0] + 1) % 10)), code);
            }
        }
        assertTrue(hasPuzzle("the_curse_of_arrav", MahjarratQuests.COA_METAL_DOOR));
    }

    /** The Mastaba's coloured floor: one pair of colours all the way across, one row's step at a time. */
    @Test
    void curseOfArravFloorKeepsToOnePairOfColours() {
        int[][] floor = {{0, 2, 1, 3, 2, 0}, {3, 1, 2, 0, 1, 3}, {2, 3, 0, 2, 3, 1}};
        List<int[]> paths = MahjarratQuests.coaTilePaths(floor);
        assertFalse(paths.isEmpty());
        for (int[] p : paths) {
            int pair = floor[p[0]][0] / 2;
            for (int c = 0; c < 6; c++) {
                assertEquals(pair, floor[p[c]][c] / 2, "every tile of one pair");
                if (c > 0) {
                    assertTrue(Math.abs(p[c] - p[c - 1]) <= 1, "one row at a time");
                }
            }
        }
        // north blue, middle green, north green, middle blue, middle green, north blue: blue and green all the way
        assertTrue(paths.stream().anyMatch(p -> java.util.Arrays.equals(p, new int[]{0, 1, 0, 1, 1, 0})));
        // starting on blue and stepping onto yellow fails
        assertFalse(paths.stream().anyMatch(p -> p[0] == 0 && p[1] == 0));
        for (QuestPuzzle.Room r : MahjarratQuests.COA_TILES.rooms) {
            for (Variant v : r.variants()) {
                assertFalse(v.solutions().isEmpty());
                assertEquals(6, v.length());
            }
        }
        assertTrue(hasPuzzle("the_curse_of_arrav", MahjarratQuests.COA_TILES));
    }

    /** OSRS fights no Zemouregal in The Curse of Arrav: Arrav (level 339, 600 HP) and the golem guard are its fights. */
    @Test
    void curseOfArravFightsArravAndTheGolemGuard() {
        QuestSpec q = BOOK.spec("the_curse_of_arrav");
        List<String> kills = q.triggers().stream().filter(t -> t.kind == QuestSpec.Trigger.Kind.KILL).map(t -> t.target).toList();
        assertEquals(List.of("coa_golem_guard", "arrav"), kills);
        var arrav = BOOK.bosses.stream().filter(b -> b.id.equals("arrav")).findFirst().orElseThrow();
        assertEquals(339, arrav.combatLevel);
        assertEquals(600, arrav.hitpoints);
        assertTrue(QuestBossRules.nthIsSpecial(3, QuestBossRules.COA_ARRAV_AXE_EVERY));
        assertFalse(QuestBossRules.nthIsSpecial(4, QuestBossRules.COA_ARRAV_AXE_EVERY));
        assertEquals(5, q.stages.size(), "the saved stage count");
    }
}
