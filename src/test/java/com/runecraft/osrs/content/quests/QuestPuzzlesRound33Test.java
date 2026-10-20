package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 33: real puzzles for the Fremennik Trials, Misthalin Mystery and The Grand Tree (wiki solutions; stage counts are saved). */
class QuestPuzzlesRound33Test {
    private static QuestSpec spec(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q;
    }

    private static Trigger puzzle(QuestSpec q, QuestPuzzle p) {
        return q.triggers().stream().filter(t -> t.puzzle == p).findFirst().orElseThrow(() -> new AssertionError(q.id() + " lacks " + p.key));
    }

    /** The options a variant's first solution clicks, in order. */
    private static List<String> answer(QuestPuzzle.Variant v) {
        List<String> out = new ArrayList<>();
        v.solutions().get(0).forEach(i -> out.add(v.options().get(i)));
        return out;
    }

    @Test
    void fremennikTrialsPeerAndSwensen() {
        QuestSpec ft = spec("fremennik_trials");
        assertEquals(2, ft.stages.size(), "saved stage count");
        // Peer's door: the wiki's six riddles, four-letter answers
        Set<String> words = new HashSet<>();
        for (QuestPuzzle.Variant v : FremennikQuests.PEER_DOOR.rooms.get(0).variants()) {
            words.add(String.join("", answer(v)));
        }
        assertEquals(Set.of("MIND", "TREE", "LIFE", "FIRE", "TIME", "WIND"), words);
        Trigger door = puzzle(ft, FremennikQuests.PEER_DOOR);
        assertTrue(door.emptyHanded && door.banksItems, "nothing may be taken in; Peer banks it");
        Trigger chest = puzzle(ft, FremennikQuests.PEER_CHEST);
        assertTrue(chest.emptyHanded && chest.needFlags.contains("ft_peer_door") && chest.setFlags.contains("ft_peer"));
        // the chest's answer leaves exactly four litres in the 5-litre bucket
        int bucket = 0;
        int jug = 0;
        for (int o : FremennikQuests.PEER_CHEST.rooms.get(0).variants().get(0).solutions().get(0)) {
            switch (o) {
                case 0 -> bucket = 5;
                case 1 -> jug = 3;
                case 2 -> {
                    int t = Math.min(bucket, 3 - jug);
                    bucket -= t;
                    jug += t;
                }
                case 3 -> {
                    int t = Math.min(jug, 5 - bucket);
                    jug -= t;
                    bucket += t;
                }
                case 4 -> bucket = 0;
                case 5 -> jug = 0;
                case 6 -> assertEquals(4, bucket, "the bucket put on the chest holds four litres");
                default -> throw new AssertionError("jug on the chest");
            }
        }
        // Swensen's maze spells his name
        StringBuilder way = new StringBuilder();
        answer(FremennikQuests.SWENSEN_MAZE.rooms.get(0).variants().get(0)).forEach(o -> way.append(o.charAt(0)));
        assertEquals("SWENSEN", way.toString());
        assertEquals("swensens_maze", puzzle(ft, FremennikQuests.SWENSEN_MAZE).target);
    }

    @Test
    void misthalinMysteryRiddlesAndMirror() {
        QuestSpec mm = spec("misthalin_mystery");
        assertEquals(4, mm.stages.size(), "saved stage count");
        assertEquals(List.of("D", "E", "A", "D"), answer(F2pQuests.MM_PIANO.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Sapphire", "Diamond", "Zenyte", "Emerald", "Onyx", "Ruby"), answer(F2pQuests.MM_HEARTH.rooms.get(0).variants().get(0)));
        assertTrue(puzzle(mm, F2pQuests.MM_PIANO).needFlags.contains("mm_ruby"));
        assertTrue(puzzle(mm, F2pQuests.MM_HEARTH).needFlags.contains("mm_emerald"));
        // the stage moves on once the sapphire key opens the westernmost door
        assertTrue(mm.stages.get(1).triggers.stream().anyMatch(t -> t.next == 2 && t.needFlags.contains("mm_sapphire")));
        // the showdown raises the unmasked Killer; killing her needs the mirror first
        Trigger mirror = puzzle(mm, F2pQuests.MM_MIRROR);
        assertEquals("misthalin_murderer", mirror.spawn);
        assertTrue(mm.stages.get(2).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.needFlags.contains("mm_mirror") && t.next == 3));
        // Mandy gives the rewards; the wiki lists no coins
        assertTrue(mm.stages.get(3).triggers.stream().anyMatch(t -> t.target.equals("mandy") && t.completes()));
        assertFalse(mm.def.itemRewards().stream().anyMatch(r -> r.startsWith("coins")));
    }

    @Test
    void grandTreeTranslationShipyardAndTuzo() {
        QuestSpec gt = spec("the_grand_tree");
        assertEquals(8, gt.stages.size(), "saved stage count");
        assertEquals(List.of("A man came to me with the King's seal.", "I gave the man Daconia rocks.", "And Daconia rocks will kill the tree!"),
                answer(KandarinGnomeQuests.GT_TRANSLATION.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Ka", "Lu", "Min"), answer(KandarinGnomeQuests.GT_GATE.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Sadly his wife is no longer with us.", "He loves worm holes.", "Anita."),
                answer(KandarinGnomeQuests.GT_FOREMAN.rooms.get(0).variants().get(0)));
        StringBuilder word = new StringBuilder();
        answer(KandarinGnomeQuests.GT_TUZO.rooms.get(0).variants().get(0)).forEach(o -> word.append(o.charAt(o.length() - 1)));
        assertEquals("TUZO", word.toString());
        Trigger translation = puzzle(gt, KandarinGnomeQuests.GT_TRANSLATION);
        assertEquals(3, translation.next, "translating the scroll moves the quest on");
        assertTrue(gt.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("glough_black_demon")
                && t.needFlags.contains("gt_tuzo")));
        assertTrue(gt.triggers().stream().anyMatch(t -> t.target.equals("anita") && t.give.containsKey("glough_key")));
        assertTrue(puzzle(gt, KandarinGnomeQuests.GT_TUZO).skills.getOrDefault(com.runecraft.osrs.Skill.AGILITY, 0) == 25);
        // mid-quest saves past the old demon step still find the daconia rock among the roots
        assertTrue(gt.stages.get(7).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.REACH && t.give.containsKey("daconia_rock")));
    }
}
