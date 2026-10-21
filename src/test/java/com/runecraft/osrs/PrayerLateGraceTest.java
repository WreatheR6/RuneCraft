package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import java.util.function.LongPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The late side of the protection switch grace (round 42, // playability): a monster hit landing while its protection is off
 * waits up to {@link PrayerGrace#LATE_MC} Minecraft ticks for a switch pressed a hair late. These tests prove the window and,
 * by running the same path the server runs (CombatHandler.npcAttack rolls with the hit's own seeded Random at impact,
 * LateProtection holds it, the protection is read when it resolves), that prayer block amounts and max hits are unchanged.
 */
class PrayerLateGraceTest {
    private static final long IMPACT = 10_000;
    private static final long NEVER_ON = Long.MAX_VALUE;

    @Test
    void constants() {
        assertEquals(4, PrayerGrace.LATE_MC); // 0.2 s: the brief's 2-4 MC
        assertEquals(24, PrayerGrace.SWITCHING_MC); // two game ticks
        // Back grace plus late window: a switch within one OSRS game tick of the impact decides the hit.
        assertEquals(12, PrayerGrace.GRACE_MC + PrayerGrace.LATE_MC);
    }

    @Test
    void onlyAnUncoveredHitOnASwitchingPlayerWaits() {
        // the right protection already counts: nothing to wait for
        assertFalse(PrayerGrace.holdLate(true, true, true, IMPACT, IMPACT));
        // the player can't switch it on (level, points, a no-prayer rule)
        assertFalse(PrayerGrace.holdLate(false, false, true, IMPACT, IMPACT));
        // switching: another protection is on (the Jad switch), or one was switched within two game ticks
        assertTrue(PrayerGrace.holdLate(false, true, true, PrayerGrace.NEVER, IMPACT));
        assertTrue(PrayerGrace.holdLate(false, true, false, IMPACT - 24, IMPACT));
        assertFalse(PrayerGrace.holdLate(false, true, false, IMPACT - 25, IMPACT));
        // praying nothing, never switched: every hit lands on its tick
        assertFalse(PrayerGrace.holdLate(false, true, false, PrayerGrace.NEVER, IMPACT));
    }

    @Test
    void aHeldHitResolvesWhenTheProtectionCountsOrTheWindowEnds() {
        assertTrue(PrayerGrace.lateDue(true, IMPACT, IMPACT + 1));
        for (int t = 1; t < PrayerGrace.LATE_MC; t++) {
            assertFalse(PrayerGrace.lateDue(false, IMPACT, IMPACT + t), "due " + t + " MC in");
        }
        assertTrue(PrayerGrace.lateDue(false, IMPACT, IMPACT + PrayerGrace.LATE_MC));
    }

    @Test
    void lastSwitchIsTheLatestStamp() {
        long[] on = {PrayerGrace.NEVER, 500, PrayerGrace.NEVER};
        long[] off = {700, PrayerGrace.NEVER, 600};
        assertEquals(700, PrayerGrace.lastSwitch(on, off));
        long[] never = {PrayerGrace.NEVER, PrayerGrace.NEVER, PrayerGrace.NEVER};
        assertEquals(PrayerGrace.NEVER, PrayerGrace.lastSwitch(never, never));
    }

    /** The resolve tick of a hit landing at {@link #IMPACT} whose protection comes on at {@code onAt}, as LateProtection runs it. */
    private static long resolveAt(long onAt, boolean switching) {
        LongPredicate prot = t -> t >= onAt;
        if (!PrayerGrace.holdLate(prot.test(IMPACT), true, switching, PrayerGrace.NEVER, IMPACT)) {
            return IMPACT;
        }
        long t = IMPACT + 1;
        while (!PrayerGrace.lateDue(prot.test(t), IMPACT, t)) {
            t++;
        }
        return t;
    }

    /** One monster hit as npcAttack lands it: rolled at impact (its own seeded Random), the protection read when it resolves. */
    private static int land(long seed, int maxHit, IntUnaryOperator throughPrayer, long onAt, boolean switching) {
        int rolled = CombatFormulas.roll(new Random(seed), 9_000, 3_000, maxHit);
        return resolveAt(onAt, switching) >= onAt ? throughPrayer.applyAsInt(rolled) : rolled;
    }

    @Test
    void aSwitchUpToFourTicksLateBlocks() {
        for (int late = 1; late <= PrayerGrace.LATE_MC; late++) {
            assertEquals(IMPACT + late, resolveAt(IMPACT + late, true), "resolved the moment the prayer came on, " + late + " MC late");
            for (long seed = 0; seed < 200; seed++) {
                assertEquals(0, land(seed, 40, d -> 0, IMPACT + late, true), "a switch " + late + " MC late, seed " + seed);
            }
        }
        // one tick past the window: the hit landed unchanged at its end
        assertEquals(IMPACT + PrayerGrace.LATE_MC, resolveAt(IMPACT + PrayerGrace.LATE_MC + 1, true));
        int through = 0;
        for (long seed = 0; seed < 200; seed++) {
            int rolled = CombatFormulas.roll(new Random(seed), 9_000, 3_000, 40);
            assertEquals(rolled, land(seed, 40, d -> 0, IMPACT + PrayerGrace.LATE_MC + 1, true));
            through += rolled > 0 ? 1 : 0;
        }
        assertTrue(through > 100, "the test monster should mostly hit (" + through + "/200)");
        // a player who isn't switching gets no late window: on 1 MC late is too late
        for (long seed = 0; seed < 50; seed++) {
            int rolled = CombatFormulas.roll(new Random(seed), 9_000, 3_000, 40);
            assertEquals(rolled, land(seed, 40, d -> 0, IMPACT + 1, false));
        }
    }

    @Test
    void blockAmountsAndMaxHitsAreUnchanged() {
        // Monsters' protection blocks 100% (most) or lets a share through (damageThroughPrayer); a held hit gets exactly the
        // block an on-time hit would, from the same roll.
        List<IntUnaryOperator> blocks = List.of(d -> 0, d -> d * 3 / 10, d -> Math.min(d, 15));
        for (int maxHit : new int[]{1, 17, 40, 97}) {
            for (IntUnaryOperator block : blocks) {
                List<Integer> onTime = new ArrayList<>();
                List<Integer> unprayed = new ArrayList<>();
                List<Integer> lateUnprayed = new ArrayList<>();
                for (long seed = 0; seed < 2_000; seed++) {
                    int prayedOnTime = land(seed, maxHit, block, IMPACT - 30, true);
                    onTime.add(prayedOnTime);
                    for (int late = 0; late <= PrayerGrace.LATE_MC; late++) {
                        assertEquals(prayedOnTime, land(seed, maxHit, block, IMPACT + late, true),
                                "max " + maxHit + ", seed " + seed + ", switched " + late + " MC late");
                    }
                    unprayed.add(land(seed, maxHit, block, NEVER_ON, false));
                    lateUnprayed.add(land(seed, maxHit, block, NEVER_ON, true)); // held the whole window, then landed
                }
                // the same rolls, hit for hit: nothing the window adds or removes
                assertEquals(unprayed, lateUnprayed);
                int max = unprayed.stream().mapToInt(Integer::intValue).max().orElse(0);
                assertTrue(max <= maxHit, "max hit " + max + " > " + maxHit);
                assertEquals(maxHit, max, "2000 rolls reach the max hit " + maxHit);
                for (int i = 0; i < onTime.size(); i++) {
                    assertEquals(block.applyAsInt(unprayed.get(i)), onTime.get(i));
                }
            }
        }
    }

    @Test
    void togetherWithTheBackGraceOneGameTickAroundTheImpactDecides() {
        // Protect from Melee on since 1000, switched off for Magic at IMPACT - 8: still graced for melee at the impact.
        assertTrue(PrayerGrace.protectedWithGrace(false, 1000, IMPACT - PrayerGrace.GRACE_MC, IMPACT));
        assertFalse(PrayerGrace.protectedWithGrace(false, 1000, IMPACT - PrayerGrace.GRACE_MC - 1, IMPACT));
        // Magic switched on up to 4 MC after a magic impact: blocked too.
        assertEquals(IMPACT + PrayerGrace.LATE_MC, resolveAt(IMPACT + PrayerGrace.LATE_MC, true));
    }
}
