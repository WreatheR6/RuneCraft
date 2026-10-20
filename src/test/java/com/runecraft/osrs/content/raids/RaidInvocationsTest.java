package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ToA raid level scaling, Walk the Path and the boss invocations (OSRS wiki values), plus ToB room numbers. */
class RaidInvocationsTest {
    @Test
    void raidLevelScalesAccuracyAndDefence() {
        // OSRS wiki: +2% accuracy and defence per 5 raid levels; the DPS calculator's defence roll factor (250 + level) / 250.
        assertEquals(1.0, RaidScaling.toaAccuracy(0), 1e-9);
        assertEquals(1.4, RaidScaling.toaAccuracy(100), 1e-9);
        assertEquals(2.2, RaidScaling.toaAccuracy(300), 1e-9);
        assertEquals(3.0, RaidScaling.toaAccuracy(500), 1e-9);
        assertEquals(2.2, RaidScaling.toaDefence("kephri", 300), 1e-9);
        // Kephri's overlords keep their defence (DPS calculator).
        assertEquals(1.0, RaidScaling.toaDefence("toa_soldier_scarab", 300), 1e-9);
        assertEquals(1.0, RaidScaling.toaDefence("toa_arcane_scarab", 500), 1e-9);
        // Damage caps at +150%, accuracy doesn't.
        assertEquals(2.5, RaidScaling.toaDamage(500), 1e-9);
    }

    @Test
    void walkThePathGivesFourRandomLevelUps() {
        for (int seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            int[] ups = new int[4];
            boolean[] done = new boolean[4];
            List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3));
            java.util.Collections.shuffle(order, random);
            int total = 0;
            for (int k = 0; k < 4; k++) {
                done[order.get(k)] = true;
                List<Integer> remaining = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    if (!done[i]) {
                        remaining.add(i);
                    }
                }
                List<Integer> got = RaidScaling.walkThePathLevelUps(remaining, k + 1, random);
                assertEquals(k == 0 ? 2 : k == 3 ? 0 : 1, got.size(), "completion " + (k + 1));
                assertEquals(got.size(), Set.copyOf(got).size());
                for (int i : got) {
                    assertFalse(done[i], "a completed path never levels up");
                    ups[i]++;
                }
                total += got.size();
            }
            assertEquals(4, total, "four level-ups per raid");
            // The path done first never levels up; the final path always gets the third completion's level-up.
            assertEquals(0, ups[order.get(0)]);
            assertTrue(ups[order.get(3)] >= 1);
        }
        Set<Invocation> walk = EnumSet.of(Invocation.WALK_THE_PATH, Invocation.PATHSEEKER);
        assertEquals(1, RaidScaling.toaPathLevel(walk, 0));
        assertEquals(4, RaidScaling.toaPathLevel(walk, 3));
        assertEquals(3, RaidScaling.toaPathLevel(EnumSet.of(Invocation.PATHMASTER), 3), "level-ups need Walk the Path");
    }

    @Test
    void invocationRequirements() {
        assertFalse(Invocation.valid(EnumSet.of(Invocation.ARTERIAL_SPRAY)));
        assertFalse(Invocation.valid(EnumSet.of(Invocation.BLOOD_THINNERS)));
        assertTrue(Invocation.valid(EnumSet.of(Invocation.NOT_JUST_A_HEAD, Invocation.BLOOD_THINNERS, Invocation.ARTERIAL_SPRAY)));
        assertFalse(Invocation.valid(EnumSet.of(Invocation.INSANITY, Invocation.OVERCLOCKED_2)));
        for (Invocation.Preset p : Invocation.presets()) {
            assertTrue(Invocation.valid(p.invocations()), p.name());
        }
    }

    @Test
    void bossInvocationEffects() {
        Set<Invocation> none = EnumSet.noneOf(Invocation.class);
        Set<Invocation> all = EnumSet.allOf(Invocation.class);
        // Akkha
        assertEquals(1, Invocation.akkhaSpecialsAtOnce(none));
        assertEquals(2, Invocation.akkhaSpecialsAtOnce(all));
        assertTrue(Invocation.akkhaKeepBack(all));
        assertEquals(2, Invocation.akkhaMemoryBlastTicks(all));
        assertEquals(2, Invocation.akkhaTrailingOrbs(all));
        assertFalse(Invocation.akkhaDetonateDiagonals(all, 1), "Feeling Special's detonate change needs a group");
        assertTrue(Invocation.akkhaDetonateDiagonals(all, 2));
        assertEquals(7, Invocation.akkhaSpecialEvery(none));
        assertEquals(6, Invocation.akkhaSpecialEvery(all));
        // Ba-Ba
        assertEquals(0, Invocation.babaBananaPeels(none));
        assertEquals(2, Invocation.babaBananaPeels(all));
        assertEquals(2, Invocation.babaSlamRadius(none));
        assertEquals(3, Invocation.babaSlamRadius(all));
        assertTrue(Invocation.babaMindTheGap(all));
        assertEquals(0, Invocation.babaFaithDamage(none, 10, 99));
        assertTrue(Invocation.babaFaithDamage(all, 10, 99) > Invocation.babaFaithDamage(all, 99, 99), "lower prayer, more damage");
        assertEquals(0, Invocation.babaFaithDamage(all, 99, 99));
        // Kephri
        assertEquals(1, Invocation.kephriDungTargets(all, 1), "Blowing Mud has no effect solo");
        assertEquals(2, Invocation.kephriDungTargets(all, 4));
        assertEquals(1, Invocation.kephriDungTargets(none, 4));
        assertEquals("toa_soldier_scarab", Invocation.kephriExtraOverlord(all, 1));
        assertEquals("toa_spitting_scarab", Invocation.kephriExtraOverlord(all, 2));
        assertNull(Invocation.kephriExtraOverlord(none, 1));
        assertEquals(2, Invocation.kephriEggs(none));
        assertEquals(4, Invocation.kephriEggs(all));
        assertEquals(1, Invocation.kephriFireballRadius(all));
        // Wardens
        assertEquals(0, Invocation.wardensSlamFaster(none));
        assertEquals(1, Invocation.wardensSlamFaster(EnumSet.of(Invocation.OVERCLOCKED)));
        assertEquals(2, Invocation.wardensSlamFaster(EnumSet.of(Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2)));
        assertEquals(3, Invocation.wardensSlamFaster(all));
        assertTrue(Invocation.wardensChargeEvery(all) < Invocation.wardensChargeEvery(none));
        assertTrue(Invocation.wardensAttackEvery(all) < Invocation.wardensAttackEvery(none));
        assertTrue(Invocation.wardensObeliskDamage(all) > 1.0);
        // Overly Draining
        assertEquals(100, Invocation.specialCost(all));
        assertEquals(-1, Invocation.specialCost(none));
    }

    @Test
    void theatreRoomNumbers() {
        // Maiden: two Nylocas Matomenos per player up to 10, always 10 in Hard mode.
        assertEquals(2, RaidScaling.tobMaidenNylocas(1, false));
        assertEquals(8, RaidScaling.tobMaidenNylocas(4, false));
        assertEquals(10, RaidScaling.tobMaidenNylocas(5, false));
        assertEquals(10, RaidScaling.tobMaidenNylocas(1, true));
        // Bloat: first walk 39-47 ticks, later walks 34-42, 19.2 s down.
        assertEquals(38, RaidScaling.TOB_BLOAT_FIRST_WALK);
        assertEquals(34, RaidScaling.TOB_BLOAT_WALK);
        assertEquals(32, RaidScaling.TOB_BLOAT_DOWN);
        assertEquals(188, RaidScaling.TOB_SOTETSEG_BALL);
    }
}
