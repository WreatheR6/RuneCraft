package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Tombs of Amascut's challenge rooms (OSRS wiki, "Tombs of Amascut/Strategies"). */
class ToaPuzzlesTest {
    @Test
    void scabarasPuzzlesAreAlwaysSolvable() {
        Random r = new Random(3);
        for (int i = 0; i < 500; i++) {
            int start = ToaPuzzles.lightsStart(r);
            assertNotEquals(ToaPuzzles.LIGHTS_SOLVED, start);
            List<Integer> presses = ToaPuzzles.lightsSolution(start);
            assertNotNull(presses);
            int s = start;
            for (int p : presses) {
                s = ToaPuzzles.lightsPress(s, p);
            }
            assertEquals(ToaPuzzles.LIGHTS_SOLVED, s);
        }
        // Stepping on the centre flips it and its four neighbours.
        assertEquals(0b010_111_010, ToaPuzzles.lightsPress(0, 4));
        assertEquals(0b000_001_011, ToaPuzzles.lightsPress(0, 0));
        for (int t = 20; t <= 45; t++) {
            int[] values = ToaPuzzles.additionValues(r);
            List<Integer> tiles = ToaPuzzles.additionSolution(values, t);
            assertNotNull(tiles, "target " + t);
            int sum = 0;
            for (int i : tiles) {
                sum += values[i];
            }
            assertEquals(t, sum);
            assertEquals(tiles.size(), new HashSet<>(tiles).size(), "each tile once");
        }
        for (int i = 0; i < 200; i++) {
            int t = ToaPuzzles.additionTarget(r);
            assertTrue(t >= 20 && t <= 45);
            List<Integer> seq = ToaPuzzles.sequence(ToaPuzzles.SEQUENCE_LENGTH, r);
            for (int k = 1; k < seq.size(); k++) {
                assertNotEquals(seq.get(k - 1), seq.get(k), "no tile twice in a row");
            }
        }
        int[][] boards = ToaPuzzles.matchingBoards(r);
        for (int b = 0; b < 2; b++) {
            Set<Integer> syms = new HashSet<>();
            for (int s : boards[b]) {
                syms.add(s);
            }
            assertEquals(9, syms.size(), "each symbol once per board");
        }
        assertEquals(4, ToaPuzzles.matchingPresolved(1));
        assertEquals(0, ToaPuzzles.matchingPresolved(3));
    }

    @Test
    void hetPuzzlesNeedTheMirrorsAndBarriers() {
        Random r = new Random(11);
        for (int i = 0; i < 300; i++) {
            int party = 1 + i % 4;
            ToaPuzzles.HetPuzzle p = ToaPuzzles.hetPuzzle(7, party, r);
            Set<Long> broken = new HashSet<>();
            for (int[] b : p.barriers()) {
                broken.add(ToaPuzzles.key(b[0], b[1]));
            }
            assertFalse(ToaPuzzles.trace(p, broken).hit(), "a fresh puzzle is not already solved, even with the barriers broken");
            assertEquals(ToaPuzzles.MOVABLE_MIRRORS, p.movable().size(), "three movable mirrors");
            List<int[]> plan = ToaPuzzles.hetSolution(p);
            // OSRS: "the light can always be reflected correctly using 1-3 mirrors".
            assertTrue(plan.size() >= 1 && plan.size() <= 3, "one to three mirrors to place: " + plan.size());
            assertFalse(p.barriers().isEmpty());
            for (int[] step : plan) {
                assertTrue(p.canPlace(step[1], step[2], broken), "the turn is free for a mirror");
                int[] m = p.movable().get(step[0]);
                m[0] = step[1];
                m[1] = step[2];
                m[2] = step[3];
            }
            for (int[] f : p.fixed()) {
                if (party == 1) {
                    assertEquals(0, f[3], "solo mirrors are never dirty");
                }
                f[3] = 0;
            }
            assertFalse(ToaPuzzles.trace(p, Set.of()).hit(), "the worn barriers block the beam");
            assertTrue(ToaPuzzles.trace(p, broken).hit(), "the placed mirrors and broken barriers lead the beam to the statue");
        }
        // '/' turns an eastbound beam north, '\' south.
        assertEquals(List.of(0, -1), List.of(ToaPuzzles.reflect(ToaPuzzles.SLASH, 1, 0)[0], ToaPuzzles.reflect(ToaPuzzles.SLASH, 1, 0)[1]));
        assertEquals(List.of(0, 1), List.of(ToaPuzzles.reflect(ToaPuzzles.BACKSLASH, 1, 0)[0], ToaPuzzles.reflect(ToaPuzzles.BACKSLASH, 1, 0)[1]));
    }

    @Test
    void scabarasBranches() {
        Random r = new Random(4);
        for (int i = 0; i < 50; i++) {
            List<ToaPuzzles.ScabarasPuzzle> layout = ToaPuzzles.scabarasLayout(r);
            assertEquals(4, new HashSet<>(layout).size(), "each puzzle in one room");
        }
        boolean[] none = new boolean[4];
        assertTrue(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.NORTH_FIRST, none, true));
        assertTrue(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.SOUTH_FIRST, none, false));
        assertFalse(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.NORTH_SECOND, none, true));
        // Solo: the north first room opens the south second room, and those two open the tablet.
        boolean[] northFirst = {true, false, false, false};
        assertTrue(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.SOUTH_SECOND, northFirst, true));
        assertFalse(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.NORTH_SECOND, northFirst, true));
        assertFalse(ToaPuzzles.scabarasAdditionOpen(northFirst, true));
        assertTrue(ToaPuzzles.scabarasAdditionOpen(new boolean[]{true, false, false, true}, true));
        assertTrue(ToaPuzzles.scabarasAdditionOpen(new boolean[]{false, true, true, false}, true));
        // Groups: each branch in turn, and both branches before the tablet.
        assertTrue(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.NORTH_SECOND, northFirst, false));
        assertFalse(ToaPuzzles.scabarasRoomOpen(ToaPuzzles.SOUTH_SECOND, northFirst, false));
        assertFalse(ToaPuzzles.scabarasAdditionOpen(new boolean[]{true, false, false, true}, false));
        assertTrue(ToaPuzzles.scabarasAdditionOpen(new boolean[]{true, true, true, true}, false));
        // Obelisks: five, struck in order; a wrong one puts them all out.
        List<Integer> order = ToaPuzzles.obeliskOrder(r);
        assertEquals(5, new HashSet<>(order).size());
        assertEquals(1, ToaPuzzles.obeliskStrike(order, 0, order.get(0)));
        assertEquals(1, ToaPuzzles.obeliskStrike(order, 1, order.get(0)), "a lit obelisk does nothing");
        assertEquals(-1, ToaPuzzles.obeliskStrike(order, 1, order.get(2)));
        int lit = 0;
        for (int o : order) {
            lit = ToaPuzzles.obeliskStrike(order, lit, o);
        }
        assertEquals(ToaPuzzles.OBELISKS, lit);
    }

    @Test
    void hetSeal() {
        assertEquals(119, ToaPuzzles.sealHitpoints(1));
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            // Mod Ash: 8 / 15 / 17 below 85 / below 100 / 100+, +2 dragon or better, -3 below rune, plus 0-2.
            int dragon99 = ToaPuzzles.sealDamage(99, "dragon_pickaxe", r);
            assertTrue(dragon99 >= 17 && dragon99 <= 19, "" + dragon99);
            int rune85 = ToaPuzzles.sealDamage(85, "rune_pickaxe", r);
            assertTrue(rune85 >= 15 && rune85 <= 17, "" + rune85);
            int bronze60 = ToaPuzzles.sealDamage(60, "bronze_pickaxe", r);
            assertTrue(bronze60 >= 5 && bronze60 <= 7, "" + bronze60);
            int crystal100 = ToaPuzzles.sealDamage(100, "crystal_pickaxe", r);
            assertTrue(crystal100 >= 19 && crystal100 <= 21, "" + crystal100);
        }
        assertEquals(15, ToaPuzzles.SEAL_WEAKENED_TICKS);
    }

    @Test
    void apmekenAndCrondis() {
        Random r = new Random(2);
        List<List<String>> solo = ToaPuzzles.apmekenWaves(1, r);
        assertEquals(8, solo.size(), "eight waves");
        assertEquals(List.of(ToaPuzzles.BRAWLER, ToaPuzzles.BRAWLER, ToaPuzzles.SHAMAN), solo.get(0));
        assertEquals(5, solo.get(3).size());
        List<List<String>> trio = ToaPuzzles.apmekenWaves(3, r);
        for (int w = 0; w < 8; w++) {
            assertEquals(solo.get(w).size() + 1, trio.get(w).size(), "groups get one more combat baboon a wave");
            for (String id : trio.get(w)) {
                assertNotNull(ContentRegistry.monster(id), id);
            }
        }
        assertEquals(1, ToaPuzzles.apmekenFixesNeeded(1));
        assertEquals(3, ToaPuzzles.apmekenFixesNeeded(3));
        assertEquals(175, ToaPuzzles.palmWaterNeeded(1));
        assertEquals(300, ToaPuzzles.palmWaterNeeded(2));
        assertEquals(50, ToaPuzzles.spillFromBite(100));
        assertEquals(0, ToaPuzzles.spillFromBite(8));
        assertEquals(18, ToaPuzzles.crocodileDamage(0));
        assertEquals(24, ToaPuzzles.crocodileDamage(2));
        assertEquals(36, ToaPuzzles.crocodileDamage(9));
        assertNotNull(ContentRegistry.monster("toa_crocodile"));
        assertNotNull(ContentRegistry.monster("toa_path_scarab"));
        assertNotNull(ContentRegistry.monster("toa_energy_siphon"));
        assertTrue(ContentRegistry.monster("toa_energy_siphon").immuneTo.contains(AttackType.RANGED));
    }

    @Test
    void guaranteedMaxHits() {
        assertTrue(ToaPuzzles.alwaysMaxHit(ToaPuzzles.BRAWLER, AttackType.MAGIC));
        assertFalse(ToaPuzzles.alwaysMaxHit(ToaPuzzles.BRAWLER, AttackType.RANGED));
        assertTrue(ToaPuzzles.alwaysMaxHit(ToaPuzzles.THROWER, AttackType.SLASH));
        assertTrue(ToaPuzzles.alwaysMaxHit(ToaPuzzles.MAGE, AttackType.RANGED));
        assertTrue(ToaPuzzles.alwaysMaxHit(ToaPuzzles.SHAMAN, AttackType.RANGED));
        assertTrue(ToaPuzzles.alwaysMaxHit("toa_baba_baboon", AttackType.CRUSH));
        assertTrue(ToaPuzzles.alwaysMaxHit("toa_energy_siphon", AttackType.STAB));
        assertFalse(ToaPuzzles.alwaysMaxHit("toa_energy_siphon", AttackType.MAGIC));
        assertTrue(ToaPuzzles.alwaysMaxHit("toa_scarab_swarm", AttackType.MAGIC));
        assertFalse(ToaPuzzles.alwaysMaxHit("akkha", AttackType.STAB));
    }
}
