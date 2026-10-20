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

/** The in-world puzzle framework ({@link QuestPuzzle}) and the quests that use it. */
class QuestPuzzleTest {
    private static QuestSpec spec(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q;
    }

    private static List<Trigger> puzzles(QuestSpec q) {
        return q.triggers().stream().filter(t -> t.kind == Trigger.Kind.PUZZLE).toList();
    }

    @Test
    void sequencesFailOnTheFirstWrongClickAndAcceptAlternatives() {
        QuestPuzzle p = QuestPuzzle.builder("t").variant(null).prompt("x").options("a", "b", "c").solution(0, 1).solution(0, 2).build();
        QuestPuzzle.Variant v = p.variant(1, 0);
        assertEquals(0, v.check(List.of(0)));
        assertEquals(1, v.check(List.of(0, 1)));
        assertEquals(1, v.check(List.of(0, 2)));
        assertEquals(-1, v.check(List.of(1)));
        assertEquals(-1, v.check(List.of(0, 0)));
        QuestPuzzle any = QuestPuzzle.builder("u").variant(null).prompt("x").options("a", "b", "c", "d").anyOrder(0, 2, 3).build();
        assertEquals(6, any.rooms.get(0).variants().get(0).solutions().size());
        assertEquals(1, any.variant(5, 0).check(List.of(3, 0, 2)));
    }

    @Test
    void everyPuzzleTriggerIsSolvableAndItsPresenterExists() {
        Set<String> npcs = new HashSet<>();
        QuestBook.get().npcs.forEach(n -> npcs.add(n.id()));
        int count = 0;
        for (QuestSpec q : QuestBook.get().quests) {
            for (Trigger t : puzzles(q)) {
                count++;
                assertNotNull(t.puzzle, q.id());
                assertTrue(t.slot >= 0 && t.slot < t.puzzle.picks, q.id() + " slot");
                for (QuestPuzzle.Room r : t.puzzle.rooms) {
                    for (QuestPuzzle.Variant v : r.variants()) {
                        String who = v.npc() != null ? v.npc() : t.target;
                        assertTrue(npcs.contains(who) || (com.runecraft.osrs.Landmark.byName(who) != null || com.runecraft.osrs.content.ContentRegistry.place(who) != null), q.id() + ": unknown presenter " + who);
                        for (List<Integer> s : v.solutions()) {
                            assertEquals(1, v.check(s), q.id() + "/" + r.id());
                        }
                        if (v.spawn() != null) {
                            assertNotNull(QuestBook.get().bosses.stream().filter(b -> b.id.equals(v.spawn())).findFirst().orElse(null), v.spawn());
                        }
                    }
                }
                if (t.puzzle.failStage >= 0) {
                    assertTrue(t.puzzle.failStage < q.stages.size(), q.id() + " fail stage");
                }
            }
        }
        assertTrue(count >= 12, "puzzle triggers: " + count);
    }

    @Test
    void recruitmentDriveDrawsFiveOfSevenRoomsWithSirKuamAlways() {
        QuestSpec rd = spec("recruitment_drive");
        assertEquals(3, rd.stages.size(), "saved stage count");
        List<Trigger> slots = puzzles(rd);
        assertEquals(5, slots.size());
        QuestPuzzle tests = slots.get(0).puzzle;
        assertEquals(7, tests.rooms.size());
        assertEquals(5, tests.picks);
        Set<List<Integer>> draws = new HashSet<>();
        Set<Integer> slotOfKuam = new HashSet<>();
        for (int player = 0; player < 200; player++) {
            long seed = QuestPuzzle.seed("player-" + player, tests.key, 0);
            List<Integer> d = tests.draw(seed);
            assertEquals(5, d.size());
            assertEquals(5, new HashSet<>(d).size(), "no room twice");
            assertTrue(d.contains(0), "Sir Kuam's duel is always drawn");
            assertEquals(d, tests.draw(seed), "deterministic");
            slotOfKuam.add(d.indexOf(0));
            draws.add(d);
        }
        assertEquals(5, slotOfKuam.size(), "Sir Kuam's room can be in any slot");
        assertTrue(draws.size() > 50, "rooms and order vary between players");
        // a failure re-draws
        long a = QuestPuzzle.seed("p", tests.key, 0);
        long b = QuestPuzzle.seed("p", tests.key, 1);
        assertFalse(a == b);
        assertEquals(1, tests.failStage);
        assertEquals(List.of("rd_test0", "rd_test1", "rd_test2", "rd_test3", "rd_test4"), tests.failFlags);
        // Sir Leye's room is a fight: the right weapon raises him and his death passes the room
        QuestPuzzle.Variant kuam = tests.rooms.get(0).variants().get(0);
        assertEquals("sir_leye", kuam.kill());
        assertEquals(1, kuam.check(List.of(3)), "the warhammer");
        assertEquals(-1, kuam.check(List.of(0)), "a blade can't harm him");
        assertTrue(slots.get(4).completes());
        assertEquals("sir_amik_varze", rd.stages.get(0).triggers.get(0).target, "OSRS start: Sir Amik Varze");
    }

    @Test
    void murderMysteryPuzzlesAgreeOnARandomMurderer() {
        QuestSpec mm = spec("murder_mystery");
        assertEquals(5, mm.stages.size(), "saved stage count");
        List<Trigger> ps = puzzles(mm);
        assertEquals(2, ps.size());
        Set<Integer> murderers = new HashSet<>();
        for (int player = 0; player < 60; player++) {
            long seed = QuestPuzzle.seed("player-" + player, ps.get(0).puzzle.key, 0);
            QuestPuzzle.Variant prints = ps.get(0).puzzle.variant(seed, 0);
            QuestPuzzle.Variant accuse = ps.get(1).puzzle.variant(QuestPuzzle.seed("player-" + player, ps.get(1).puzzle.key, 0), 0);
            int m = accuse.solutions().get(0).get(0);
            murderers.add(m);
            String matched = prints.options().get(prints.solutions().get(0).get(0));
            assertTrue(matched.startsWith(KandarinMoreQuests.SUSPECTS[m] + "'s"), matched + " vs " + KandarinMoreQuests.SUSPECTS[m]);
        }
        assertEquals(6, murderers.size(), "any of the six can be the murderer");
        // a save from before the puzzles reaches the accusation without the print (David took it): the guard lifts another
        assertTrue(mm.stages.get(4).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.TALK && t.next == t.stage
                && t.unlessItems.contains("killers_print") && t.give.containsKey("killers_print")));
    }

    @Test
    void tribalTotemDoorTakesKurt() {
        QuestSpec tt = spec("tribal_totem");
        assertEquals(4, tt.stages.size());
        Trigger lock = puzzles(tt).get(0);
        QuestPuzzle.Variant v = lock.puzzle.variant(0, 0);
        List<Integer> kurt = new ArrayList<>();
        for (int i : QuestPuzzle.word("KURT")) {
            kurt.add(i);
        }
        assertEquals(1, v.check(kurt));
        assertTrue(tt.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.REACH && t.needFlags.contains("tt_door")
                && t.skills.containsKey(com.runecraft.osrs.Skill.THIEVING)));
    }

    @Test
    void whileGuthixSleepsHasMovariosBaseAndTheChasmPuzzles() {
        QuestSpec wgs = spec("while_guthix_sleeps");
        assertEquals(6, wgs.stages.size(), "saved stage count");
        assertEquals("ivy_sophista", wgs.stages.get(0).triggers.get(0).target, "OSRS start: Ivy Sophista");
        // the rune door, the wired bookcases, the pressure gauge, the skulls and 8 of the 13 temple statues (round 37)
        assertEquals(12, puzzles(wgs).size());
        assertTrue(wgs.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("balance_elemental")
                && t.needFlags.contains("wgs_skulls")));
        assertTrue(wgs.triggers().stream().anyMatch(t -> t.give.containsKey("movarios_notes") && t.needFlags.containsAll(List.of("wgs_door", "wgs_weights"))));
    }
}
