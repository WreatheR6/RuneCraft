package com.runecraft.osrs;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 41 real-time delivery of monster attacks: the shared floors, and that they never change an OSRS number. */
class ThreatTimingTest {
    @Test
    void telegraphLeadHasARealTimeFloor() {
        assertEquals(36, ThreatTiming.leadMc(1, Special.NONE));
        assertEquals(36, ThreatTiming.leadMc(2, Special.AOE));
        assertEquals(60, ThreatTiming.leadMc(5, Special.NONE));
        assertEquals(36, ThreatTiming.leadMc(0, Special.SKY_STRIKE));
        assertEquals(48, ThreatTiming.leadMc(4, Special.SKY_STRIKE));
        assertEquals(0, ThreatTiming.leadMc(0, Special.NONE), "an untelegraphed attack has no lead");
        assertEquals(0, ThreatTiming.leadMc(0, Special.AOE));
    }

    @Test
    void projectileFlight() {
        assertEquals(10, ThreatTiming.flightMc(4, false));
        assertEquals(20, ThreatTiming.flightMc(16, false));
        assertEquals(24, ThreatTiming.flightMc(40, true));
        assertEquals(16, ThreatTiming.flightMc(2, true));
        assertEquals(10, ThreatTiming.flightMc(0, false));
        // a fast attacker's projectile lands before its next attack: Zilyana (2 ticks) at most 22 MC, a 1-tick one at the 10 floor
        assertEquals(22, ThreatTiming.latestLandingMc(24));
        assertEquals(10, ThreatTiming.latestLandingMc(12));
        assertEquals(46, ThreatTiming.latestLandingMc(48), "a 4-tick monster: no effect on a 24 MC flight + 20 MC spacing");
        for (double d = 0; d < 64; d += 0.25) {
            int f = ThreatTiming.flightMc(d, false);
            assertTrue(f >= 10 && f <= 24, "flight " + f + " at " + d);
            assertTrue(ThreatTiming.flightMc(d, true) >= 16);
        }
    }

    @Test
    void styleSpacingOnlyPushesADifferentPrayer() {
        // a magic hit 5 MC after a ranged one moves to 20 MC after it; another ranged one stays
        assertEquals(120, ThreatTiming.styleSpacedDue(100, AttackType.RANGED, 105, AttackType.MAGIC));
        assertEquals(105, ThreatTiming.styleSpacedDue(100, AttackType.RANGED, 105, AttackType.RANGED));
        // stab, slash and crush share Protect from Melee
        assertEquals(101, ThreatTiming.styleSpacedDue(100, AttackType.CRUSH, 101, AttackType.SLASH));
        assertEquals(120, ThreatTiming.styleSpacedDue(100, AttackType.CRUSH, 101, AttackType.MAGIC));
        // landing just before the other hit also counts; 20 or more apart is left alone
        assertEquals(120, ThreatTiming.styleSpacedDue(100, AttackType.MAGIC, 90, AttackType.RANGED));
        assertEquals(80, ThreatTiming.styleSpacedDue(100, AttackType.MAGIC, 80, AttackType.RANGED));
        assertEquals(125, ThreatTiming.styleSpacedDue(100, AttackType.MAGIC, 125, AttackType.RANGED));
        // against several scheduled hits, in landing order, and never more than 20 past its natural tick
        long[] dues = {100, 125};
        AttackType[] styles = {AttackType.MAGIC, AttackType.RANGED};
        assertEquals(145, ThreatTiming.spacedDue(125, AttackType.MAGIC, dues, styles));
        assertEquals(120, ThreatTiming.spacedDue(105, AttackType.RANGED, dues, styles));
        assertEquals(130, ThreatTiming.spacedDue(110, AttackType.STAB, dues, styles), "capped at natural + 20");
        assertEquals(200, ThreatTiming.spacedDue(200, AttackType.STAB, dues, styles), "far from both: untouched");
        // a one-tick volley (Demonic Brutus's ghosts): a boss's magic orb launched at 0 lands at 16; a melee swing launched at 12
        // (natural landing 12 + 8 wind-up = 20) is pushed to 36, then a ranged orb launched at 24 (natural 40) to 56
        long[] orb = {16};
        AttackType[] magic = {AttackType.MAGIC};
        long swing = ThreatTiming.spacedDue(12 + ThreatTiming.meleeWindupMc(2), AttackType.CRUSH, orb, magic);
        assertEquals(36, swing);
        assertEquals(56, ThreatTiming.spacedDue(40, AttackType.RANGED, new long[]{16, swing},
                new AttackType[]{AttackType.MAGIC, AttackType.CRUSH}));
        // a monster's bolt that already landed (at 10) still spaces a swing landing at 18
        assertEquals(30, ThreatTiming.spacedDue(18, AttackType.SLASH, new long[]{10}, magic));
    }

    @Test
    void meleeWindupByWidth() {
        assertEquals(6, ThreatTiming.meleeWindupMc(0.6));
        assertEquals(8, ThreatTiming.meleeWindupMc(1.5));
        assertEquals(8, ThreatTiming.meleeWindupMc(2.9));
        assertEquals(10, ThreatTiming.meleeWindupMc(3));
        assertEquals(10, ThreatTiming.meleeWindupMc(6));
    }

    @Test
    void meleeReachIsTheBoundingBoxGap() {
        // a goblin-sized monster (0.6 wide) beside a player (0.6 wide): centres 1.5 apart leave a 0.9 gap
        assertEquals(0.9, ThreatTiming.gap(1.5, 0, 0.6, 0.6), 1e-9);
        assertTrue(ThreatTiming.inMeleeReach(ThreatTiming.gap(1.5, 0, 0.6, 0.6), 0));
        assertFalse(ThreatTiming.inMeleeReach(ThreatTiming.gap(2.2, 0, 0.6, 0.6), 0), "gap 1.6");
        // a 3-wide giant: its body edge counts, not its centre
        assertTrue(ThreatTiming.inMeleeReach(ThreatTiming.gap(3.0, 0, 3, 0.6), 0), "gap 1.2");
        assertFalse(ThreatTiming.inMeleeReach(ThreatTiming.gap(3.5, 0, 3, 0.6), 0), "gap 1.7");
        // a 4-wide boss, diagonally
        assertEquals(0.0, ThreatTiming.gap(1, 1, 4, 0.6), 1e-9, "inside its box");
        assertTrue(ThreatTiming.inMeleeReach(ThreatTiming.gap(2.5, 2.5, 4, 0.6), 0));
        assertFalse(ThreatTiming.inMeleeReach(ThreatTiming.gap(3, 3, 4, 0.6), 0));
        // a floor above or below is out of reach
        assertFalse(ThreatTiming.inMeleeReach(0.5, 2.0));
        assertTrue(ThreatTiming.inMeleeReach(0.5, -1.5));
        // chase until 0.8; a wound-up hit still lands one block further
        assertTrue(ThreatTiming.keepChasing(0.81));
        assertFalse(ThreatTiming.keepChasing(0.8));
        assertTrue(ThreatTiming.meleeLands(2.5, 0, 1.5));
        assertFalse(ThreatTiming.meleeLands(2.6, 0, 1.5));
        assertFalse(ThreatTiming.meleeLands(1.0, 2.1, 1.5));
    }

    @Test
    void areaTestsAreHorizontal() {
        assertTrue(ThreatTiming.inSkyStrike(1.5, 1.9));
        assertFalse(ThreatTiming.inSkyStrike(1.51, 0));
        assertFalse(ThreatTiming.inSkyStrike(0.5, 2.0));
        assertTrue(ThreatTiming.inArea(3.0, 0.5, 3));
        assertFalse(ThreatTiming.inArea(3.5, 0, 3));
        assertTrue(ThreatTiming.hazardContact(0.99, 1.4));
        assertFalse(ThreatTiming.hazardContact(1.2, 0));
        assertFalse(ThreatTiming.hazardContact(0.5, 1.5));
    }

    @Test
    void hazardsArmThenHitOncePerGameTick() {
        long placed = 1000;
        long arm = placed + ThreatTiming.HAZARD_ARM_MC;
        assertFalse(ThreatTiming.hazardShouldHit(ThreatTiming.NEVER, placed, arm));
        assertFalse(ThreatTiming.hazardShouldHit(ThreatTiming.NEVER, arm - 1, arm));
        assertTrue(ThreatTiming.hazardShouldHit(ThreatTiming.NEVER, arm, arm));
        assertFalse(ThreatTiming.hazardShouldHit(arm, arm + 11, arm));
        assertTrue(ThreatTiming.hazardShouldHit(arm, arm + 12, arm));
        // standing in it from placement to 24 MC after: hits at 12 and 24, the old once-a-game-tick rate
        long last = ThreatTiming.NEVER;
        int hits = 0;
        for (long now = placed; now <= placed + 24; now++) {
            if (ThreatTiming.hazardShouldHit(last, now, arm)) {
                last = now;
                hits++;
            }
        }
        assertEquals(2, hits);
        // standing in it for 120 MC once armed: one hit per game tick, as before
        last = ThreatTiming.NEVER;
        hits = 0;
        for (long now = arm; now < arm + 120; now++) {
            if (ThreatTiming.hazardShouldHit(last, now, arm)) {
                last = now;
                hits++;
            }
        }
        assertEquals(10, hits);
    }

    @Test
    void engagingNeverHitsOnArrival() {
        // a 4-tick monster engaging at MC 1000: its first attack is ready 10 MC later (wind-up 6), not at once
        long last = ThreatTiming.engageLastAttack(1000, 4, 6);
        assertFalse(ThreatTiming.cadenceReady(1009, last, 4));
        assertTrue(ThreatTiming.cadenceReady(1010, last, 4));
        // a big monster's 10 MC wind-up already covers the floor
        assertEquals(1010, ThreatTiming.engageLastAttack(1000, 6, 10) + 6 * 12);
        // a switch of target never brings an attack forward: just after an attack at 990, the next still waits for 1038
        assertEquals(990, ThreatTiming.engage(990, 1000, 4, 6));
        assertFalse(ThreatTiming.cadenceReady(1010, ThreatTiming.engage(990, 1000, 4, 6), 4));
        assertTrue(ThreatTiming.cadenceReady(1038, ThreatTiming.engage(990, 1000, 4, 6), 4));
        assertEquals(ThreatTiming.engageLastAttack(1000, 4, 6), ThreatTiming.engage(-1000, 1000, 4, 6), "a fresh monster");
        assertEquals(1004, ThreatTiming.arrivalReadyMc(1000, 6), "launch 4 + wind-up 6 = hit 10 MC after arrival");
        assertEquals(1000, ThreatTiming.arrivalReadyMc(1000, 10));
    }

    @Test
    void deliveryDelaysNeverChangeTheAttackCount() {
        // OSRS cadence stamped at launch: whatever the delivery delay (wind-up, flight, telegraph, spacing), every attack started in
        // a 100-game-tick window lands, and as many start as the OSRS rate gives: floor(1200 / (speed * 12)) + 1.
        int[][] delays = {{0}, {6}, {10, 24, 16}, {36}, {44, 6, 30}, {60}};
        for (int speed = 1; speed <= 8; speed++) {
            for (int[] d : delays) {
                int[] r = ThreatTiming.simulate(speed, 1200, d);
                assertEquals(1200 / (speed * 12) + 1, r[0], "speed " + speed);
                if (d[0] <= ThreatTiming.FLIGHT_MAX_MC + ThreatTiming.MAX_SPACING_PUSH_MC) {
                    assertEquals(r[0], r[1], "speed " + speed + " delays " + java.util.Arrays.toString(d));
                }
            }
        }
    }

    @Test
    void finalNoteRises() {
        assertEquals(0f, ThreatTiming.finalNotePitch(100, 112));
        float a = ThreatTiming.finalNotePitch(103, 112);
        float b = ThreatTiming.finalNotePitch(106, 112);
        float c = ThreatTiming.finalNotePitch(109, 112);
        assertTrue(a > 0 && b > a && c > b);
        assertEquals(0f, ThreatTiming.finalNotePitch(112, 112));
    }

    /** Every telegraphed attack of every module monster and boss (all phases) gets at least the 36 MC floor. */
    @Test
    void everyContentTelegraphMeetsTheFloor() {
        List<AttackDef> all = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            all.addAll(m.attacks);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            all.addAll(b.attacks);
            for (BossDef.Phase p : b.phases) {
                all.addAll(p.attacks());
            }
        }
        int telegraphed = 0;
        for (AttackDef a : all) {
            if (ThreatTiming.telegraphed(a.telegraphTicks(), a.special())) {
                telegraphed++;
                int lead = ThreatTiming.leadMc(a.telegraphTicks(), a.special());
                assertTrue(lead >= ThreatTiming.TELEGRAPH_FLOOR_MC, a.name() + " lead " + lead);
                assertTrue(lead >= a.telegraphTicks() * 12, a.name() + " shorter than its OSRS telegraph");
            }
        }
        assertTrue(telegraphed > 50, "only " + telegraphed + " telegraphed attacks found");
    }
}
