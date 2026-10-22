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

    /**
     * The Blood Moon Rises (wiki quick guide, quest-helper): its eight saved stages hold the real chapters and puzzles, and the
     * puzzles take the wiki's answers.
     */
    @Test
    void bloodMoonRisesPuzzlesTakeTheWikiAnswers() {
        QuestSpec q = BOOK.spec("the_blood_moon_rises");
        assertEquals(8, q.stages.size(), "the saved stage count");
        for (QuestPuzzle p : List.of(LateQuestsD.BMR_PLINTHS, LateQuestsD.BMR_BUSTS, LateQuestsD.BMR_CLOCKS, LateQuestsD.BMR_CRESCENT_CHEST,
                LateQuestsD.BMR_STATUES, LateQuestsD.BMR_LOCKBOX, LateQuestsD.BMR_DAGGER_DOOR, LateQuestsD.BMR_GILDED_BOOKCASE,
                LateQuestsD.BMR_BASINS, LateQuestsD.BMR_REFINER)) {
            assertTrue(hasPuzzle("the_blood_moon_rises", p), p.key);
        }
        // north to south: Pious proceedings, The Life of Friar, Scruffy notebook, From Misthalin to Morytania, Sarl's journal,
        // Essiandar's notes
        Variant plinths = LateQuestsD.BMR_PLINTHS.rooms.get(0).variants().get(0);
        assertEquals(List.of("Pious proceedings", "The Life of Friar", "Scruffy notebook", "From Misthalin to Morytania", "Sarl's journal",
                "Essiandar's notes"), plinths.solutions().get(0).stream().map(plinths.options()::get).toList());
        // the busts: third, fourth, first, second
        assertEquals(List.of(2, 3, 0, 1), LateQuestsD.BMR_BUSTS.rooms.get(0).variants().get(0).solutions().get(0));
        // the clocks: east 12 and 4, west 11 and 9
        Variant clocks = LateQuestsD.BMR_CLOCKS.rooms.get(0).variants().get(0);
        assertEquals(List.of("12", "4", "11", "9"), clocks.solutions().get(0).stream().map(clocks.options()::get).toList());
        // the crescent moon chest: right, down, right, left, left
        Variant chest = LateQuestsD.BMR_CRESCENT_CHEST.rooms.get(0).variants().get(0);
        assertEquals(List.of("Right", "Down", "Right", "Left", "Left"), chest.solutions().get(0).stream().map(chest.options()::get).toList());
        // the gilded bookcase: the eight houses in alphabetical order
        Variant books = LateQuestsD.BMR_GILDED_BOOKCASE.rooms.get(0).variants().get(0);
        List<String> houses = books.solutions().get(0).stream().map(books.options()::get).toList();
        assertEquals(houses.stream().sorted().toList(), houses);
        assertEquals(8, houses.size());
        // the refiner: every accepted answer makes 53 from two waters, three bloods and three essences
        Variant refiner = LateQuestsD.BMR_REFINER.rooms.get(0).variants().get(0);
        int[] potential = {17, 13, 6};
        for (List<Integer> answer : refiner.solutions()) {
            assertEquals(53, answer.stream().mapToInt(i -> potential[i]).sum());
            long blood = answer.stream().filter(i -> i <= 1).count();
            long essence = answer.stream().filter(i -> i == 0 || i == 2).count();
            long water = answer.stream().filter(i -> i >= 1).count();
            assertTrue(blood <= 3 && essence <= 3 && water <= 2, answer.toString());
        }
        assertEquals(1, refiner.check(List.of(2, 0, 1, 0)));
        assertEquals(-1, refiner.check(List.of(1, 1)));
        // the passcode door: SPEAR, read through the grid
        assertEquals(1, LateQuestsD.BMR_DAGGER_DOOR.rooms.get(0).variants().get(0).check(java.util.Arrays.stream(QuestPuzzle.word("SPEAR"))
                .boxed().toList()));
    }

    /** Lowerniel Drakan's wiki infobox (level 1063, 3,000 HP) and the hitpoints each of his four fights takes. */
    @Test
    void bloodMoonRisesFightsDrakanFourTimes() {
        QuestSpec q = BOOK.spec("the_blood_moon_rises");
        List<String> kills = q.triggers().stream().filter(t -> t.kind == QuestSpec.Trigger.Kind.KILL).map(t -> t.target).toList();
        assertTrue(kills.containsAll(List.of("bmr_drakan_woods", "bmr_drakan_siege", "bmr_drakan_blood_moon", "quest_lowerniel_drakan",
                "bmr_wyrd", "bmr_vyrewatch", "bmr_vyrewatch_sentinel", "bmr_venator", "bmr_ancient_feral_vyre")), kills.toString());
        java.util.Map<String, Integer> hp = java.util.Map.of("bmr_drakan_woods", 450, "bmr_drakan_siege", 1260, "bmr_drakan_blood_moon", 2100,
                "quest_lowerniel_drakan", 3000, "bmr_wyrd", 990);
        hp.forEach((id, h) -> {
            var boss = BOOK.bosses.stream().filter(b -> b.id.equals(id)).findFirst().orElseThrow();
            assertEquals(h, boss.hitpoints, id);
        });
        var drakan = BOOK.bosses.stream().filter(b -> b.id.equals("quest_lowerniel_drakan")).findFirst().orElseThrow();
        assertEquals(1063, drakan.combatLevel);
        assertEquals(2, drakan.phases.size());
        // the final fight ends the quest (the tome of experience is its reward)
        assertTrue(q.stages.get(7).triggers.stream().anyMatch(t -> t.kind == QuestSpec.Trigger.Kind.KILL
                && t.target.equals("quest_lowerniel_drakan") && t.completes()));
    }

    /** Grim Tales (wiki): Grimgnash's story, the witch's piano and the mouse hole's nail walls. */
    @Test
    void grimTalesPuzzlesTakeTheWikiAnswers() {
        QuestSpec q = BOOK.spec("grim_tales");
        assertEquals(5, q.stages.size(), "the saved stage count");
        for (QuestPuzzle p : List.of(LateQuestsA.GT_BEDTIME_STORY, LateQuestsA.GT_PIANO, LateQuestsA.GT_NAILS)) {
            assertTrue(hasPuzzle("grim_tales", p), p.key);
        }
        Variant story = LateQuestsA.GT_BEDTIME_STORY.rooms.get(0).variants().get(0);
        assertEquals(List.of("I heard you were a great and mighty Griffin!", "There once was a graveyard filled with undead.",
                "There lived a skeleton named Skullrot.", "Skullrot was insane!", "Skullrot hungrily grabbed the gnome's hair.",
                "Started to strangle the poor gnome.", "He saw a hole he could just fit through."),
                story.solutions().get(0).stream().map(story.options()::get).toList());
        // upper E-F-E-D-C, then lower A-E-G-A
        Variant piano = LateQuestsA.GT_PIANO.rooms.get(0).variants().get(0);
        assertEquals(List.of("Upper E", "Upper F", "Upper E", "Upper D", "Upper C", "Lower A", "Lower E", "Lower G", "Lower A"),
                piano.solutions().get(0).stream().map(piano.options()::get).toList());
        Variant nails = LateQuestsA.GT_NAILS.rooms.get(0).variants().get(0);
        assertFalse(nails.solutions().get(0).stream().map(nails.options()::get).anyMatch(o -> o.contains("grate")), "the grate sends you out");
        var glod = BOOK.bosses.stream().filter(b -> b.id.equals("glod")).findFirst().orElseThrow();
        assertEquals(138, glod.combatLevel);
        assertEquals(160, glod.hitpoints);
        assertEquals(4, glod.attackSpeed);
        assertTrue(q.triggers().stream().anyMatch(t -> t.kind == QuestSpec.Trigger.Kind.KILL && t.target.equals("glod")));
    }

    /**
     * The Fremennik Exiles (wiki quick guide): the cave door is Mastermind with six runes, and every board's guesses leave exactly
     * one code; Typhor and the Jormungand are its last fights, and no Lady V.
     */
    @Test
    void fremennikExilesDoorAndFights() {
        QuestSpec q = BOOK.spec("the_fremennik_exiles");
        assertEquals(5, q.stages.size(), "the saved stage count");
        assertTrue(hasPuzzle("the_fremennik_exiles", LateQuestsC.FE_CAVE_DOOR));
        // Mastermind checks: a repeated rune is counted once per match
        assertArrayEquals(new int[]{1, 1}, LateQuestsC.feChecks(new int[]{0, 0, 1, 2}, new int[]{0, 3, 0, 5}));
        assertArrayEquals(new int[]{4, 0}, LateQuestsC.feChecks(new int[]{4, 4, 4, 4}, new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{0, 2}, LateQuestsC.feChecks(new int[]{0, 1, 2, 3}, new int[]{1, 0, 4, 4}));
        assertEquals(8, LateQuestsC.FE_BOARDS.size());
        for (int i = 0; i < LateQuestsC.FE_BOARDS.size(); i++) {
            LateQuestsC.FeBoard board = LateQuestsC.FE_BOARDS.get(i);
            List<int[]> left = LateQuestsC.feConsistent(board.code(), board.guesses());
            assertEquals(1, left.size(), "board " + i + " has one answer");
            assertArrayEquals(board.code(), left.get(0));
            assertTrue(board.guesses().size() <= 8);
            Variant v = LateQuestsC.FE_CAVE_DOOR.rooms.get(0).variants().get(i);
            assertEquals(1, v.check(java.util.Arrays.stream(board.code()).boxed().toList()));
        }
        List<String> kills = q.triggers().stream().filter(t -> t.kind == QuestSpec.Trigger.Kind.KILL).map(t -> t.target).toList();
        assertEquals(List.of("quest_basilisk_boss", "fe_monstrous_basilisk", "typhor", "the_jormungand"), kills);
        java.util.Map<String, int[]> stats = java.util.Map.of("typhor", new int[]{218, 360}, "the_jormungand", new int[]{363, 600},
                "fe_monstrous_basilisk", new int[]{135, 170}, "quest_basilisk_boss", new int[]{57, 60});
        stats.forEach((id, lh) -> {
            var boss = BOOK.bosses.stream().filter(b -> b.id.equals(id)).findFirst().orElseThrow();
            assertEquals(lh[0], boss.combatLevel, id);
            assertEquals(lh[1], boss.hitpoints, id);
        });
        assertTrue(q.triggers().stream().noneMatch(t -> t.target.equals("quest_lady_v")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.give.containsKey("vs_shield")), "the player restores V's shield");
    }
}
