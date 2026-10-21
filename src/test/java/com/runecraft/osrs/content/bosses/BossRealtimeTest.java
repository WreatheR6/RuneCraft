package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.content.quests.QuestBossRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 42's real-time boss patches (docs/PLAYABILITY.md): every OSRS number of a sequence (max hits, patterns, cycle lengths,
 * block amounts) stays; only when its hits land relative to their cues changes, never under the shared floors.
 */
class BossRealtimeTest {
    @Test
    void floorsMatchTheSharedOnes() {
        assertEquals(ThreatTiming.TELEGRAPH_FLOOR_MC, BossRealtime.CUE_FLOOR_MC);
        assertEquals(ThreatTiming.STYLE_SPACING_MC, BossRealtime.STYLE_SPACING_MC);
        assertEquals(ArenaTiming.STRIKE_FLOOR_TICKS, BossRealtime.STRIKE_FLOOR_TICKS);
        assertEquals(ThreatTiming.MC_PER_GAME_TICK, BossRealtime.MC_PER_GAME_TICK);
    }

    /** Every volley pattern the Whisperer uses (BossMechanics.whispererVolley), normal and awakened. */
    @Test
    void whispererVolleysCanBePrayedAndKeepTheirCycle() {
        // her attack speed: 10 in game (MonsterStatsTable's wiki row), 5 in the BossDef literal; the cycle holds for both
        for (int speed : new int[]{5, 10}) {
            volleysAt(speed);
        }
    }

    private static void volleysAt(int speed) {
        for (boolean awakened : new boolean[]{false, true}) {
            for (int specials = 0; specials <= 8; specials++) {
                for (boolean first : new boolean[]{false, true}) {
                    boolean[] v = BossMechanics.whispererVolley(specials, awakened, first);
                    assertEquals(awakened ? 5 : 3, v.length, "OSRS volley size");
                    int[] at = BossRealtime.volleyLaunchTicks(v);
                    for (int i = 1; i < v.length; i++) {
                        int gapMc = (at[i] - at[i - 1]) * BossRealtime.MC_PER_GAME_TICK;
                        if (v[i] != v[i - 1]) {
                            assertTrue(gapMc >= BossRealtime.STYLE_SPACING_MC, "a switch " + gapMc + " MC after the last projectile");
                        } else {
                            assertEquals(BossRealtime.MC_PER_GAME_TICK, gapMc, "same style keeps the OSRS tick");
                        }
                    }
                    // the whole cycle (volley and rest) is the OSRS one: n-1 ticks of volley plus her attack speed
                    assertEquals(v.length - 1 + speed, BossRealtime.volleyCycleTicks(v, speed),
                            "cycle of " + java.util.Arrays.toString(v));
                }
            }
        }
        // the awakened alternating volley (after her seventh special): 0, 2, 4, 6, 8, then the rest less 4 ticks (5: 1 tick)
        boolean[] alt = BossMechanics.whispererVolley(7, true, true);
        assertArrayEquals(new boolean[]{true, false, true, false, true}, alt);
        assertArrayEquals(new int[]{0, 2, 4, 6, 8}, BossRealtime.volleyLaunchTicks(alt));
        assertEquals(speed - 4, BossRealtime.volleyRestTicks(speed, BossRealtime.volleyExtraTicks(alt)));
        // a single-style volley is the OSRS one exactly
        boolean[] same = BossMechanics.whispererVolley(0, false, false);
        assertArrayEquals(new int[]{0, 1, 2}, BossRealtime.volleyLaunchTicks(same));
        assertEquals(speed, BossRealtime.volleyRestTicks(speed, BossRealtime.volleyExtraTicks(same)));
    }

    @Test
    void zulrahJadPhaseIsCuedLikeJad() {
        assertTrue(BossRealtime.ZULRAH_JAD_LEAD_TICKS * BossRealtime.MC_PER_GAME_TICK >= BossRealtime.CUE_FLOOR_MC);
        assertEquals(36, ThreatTiming.leadMc(BossRealtime.ZULRAH_JAD_LEAD_TICKS, com.runecraft.osrs.content.AttackDef.Special.VENOM));
        // the Jad phase attacks every 3 ticks (ZulrahScript.attackDelay): alternating hits stay 36 MC apart
        assertTrue(3 * BossRealtime.MC_PER_GAME_TICK >= BossRealtime.STYLE_SPACING_MC);
    }

    @Test
    void cerberusSoulsAreCuedInOrder() {
        // OSRS: 6 ticks after the summon, then 2 apart
        assertEquals(6, BossRealtime.soulAttackTick(0));
        assertEquals(8, BossRealtime.soulAttackTick(1));
        assertEquals(10, BossRealtime.soulAttackTick(2));
        for (int i = 0; i < 3; i++) {
            int leadMc = (BossRealtime.soulAttackTick(i) - BossRealtime.soulCueTick(i)) * BossRealtime.MC_PER_GAME_TICK;
            assertTrue(leadMc >= BossRealtime.CUE_FLOOR_MC, "soul " + i + " cued " + leadMc + " MC ahead");
            assertTrue(BossRealtime.soulCueTick(i) > 0, "after the summon");
            if (i > 0) {
                assertTrue(BossRealtime.soulCueTick(i) > BossRealtime.soulCueTick(i - 1), "cued in order");
                int apartMc = (BossRealtime.soulAttackTick(i) - BossRealtime.soulAttackTick(i - 1)) * BossRealtime.MC_PER_GAME_TICK;
                assertTrue(apartMc >= BossRealtime.STYLE_SPACING_MC, "souls " + apartMc + " MC apart");
            }
        }
        // the triple attack's 2-tick gaps already leave each style 24 MC (the delivery spaces their landings)
        assertTrue(BossRealtime.CERB_TRIPLE_GAP_TICKS * BossRealtime.MC_PER_GAME_TICK >= BossRealtime.STYLE_SPACING_MC);
    }

    @Test
    void vardorvisGazesAreSpacedAndHisCadenceKept() {
        assertArrayEquals(new int[]{3}, BossRealtime.gazeLandTicks(new boolean[]{false}));
        assertArrayEquals(new int[]{3, 5}, BossRealtime.gazeLandTicks(new boolean[]{true, false}));
        assertArrayEquals(new int[]{3, 5}, BossRealtime.gazeLandTicks(new boolean[]{false, true}));
        assertArrayEquals(new int[]{3, 4}, BossRealtime.gazeLandTicks(new boolean[]{true, true}), "same style: OSRS one tick");
        // his melee waits as in OSRS (flight, one tick per extra gaze, 2 after), and still comes after the last gaze lands
        assertEquals(5, BossRealtime.gazeHoldTicks(1));
        assertEquals(6, BossRealtime.gazeHoldTicks(2));
        for (boolean[] set : new boolean[][]{{true}, {false}, {true, false}, {false, true}}) {
            int[] land = BossRealtime.gazeLandTicks(set);
            assertTrue(land[land.length - 1] < BossRealtime.gazeHoldTicks(set.length) + 1, "melee before the last gaze lands");
        }
        // the bleed runs every tick only on a sustained sprint
        assertTrue(BossRealtime.bleedRunning(true, true, 3.37));
        assertFalse(BossRealtime.bleedRunning(true, true, 2.59), "walking pace never counts");
        assertFalse(BossRealtime.bleedRunning(false, true, 3.4), "a sprint tapped this tick doesn't count yet");
        assertFalse(BossRealtime.bleedRunning(true, false, 3.4), "a sprint let go this tick doesn't count");
        assertFalse(BossRealtime.bleedRunning(true, true, 0), "sprint toggled on while standing");
        assertTrue(BossRealtime.BLEED_RUN_BLOCKS_PER_TICK > 4.317 * 0.6 && BossRealtime.BLEED_RUN_BLOCKS_PER_TICK < 5.612 * 0.6);
        // standing still the bleed is still OSRS's: 5 hits over 14 ticks
        BossMechanics.Bleed b = new BossMechanics.Bleed();
        b.start();
        int hits = 0;
        while (b.active()) {
            if (b.tick(false) > 0) {
                hits++;
            }
        }
        assertEquals(5, hits);
    }

    @Test
    void nightmareWindUpsMeetTheFloorAndPhosaniPhaseThreeWaitsLonger() {
        for (boolean phosani : new boolean[]{false, true}) {
            for (int phase = 0; phase <= 2; phase++) {
                for (boolean melee : new boolean[]{false, true}) {
                    int t = BossRealtime.nightmareWindUp(phosani, phase, false, melee);
                    assertTrue(t * BossRealtime.MC_PER_GAME_TICK >= BossRealtime.CUE_FLOOR_MC, "wind-up " + t);
                    assertTrue(t >= (melee ? NightmareRules.MELEE_WIND_UP : NightmareRules.RANGED_WIND_UP), "never under OSRS");
                    assertTrue(t < 6, "lands before her next attack (attack speed 6)");
                }
            }
        }
        assertEquals(3, BossRealtime.nightmareWindUp(false, 0, false, true), "OSRS 2: floored to 3");
        assertEquals(3, BossRealtime.nightmareWindUp(false, 0, false, false), "OSRS 3");
        assertEquals(3, BossRealtime.nightmareWindUp(true, 2, false, true), "Phosani phase three melee");
        assertEquals(4, BossRealtime.nightmareWindUp(true, 2, false, false), "Phosani phase three magic/ranged");
        assertEquals(3, BossRealtime.nightmareWindUp(true, 2, true, false), "desperation: no prayer deactivation");
        // husks: each still every 4 ticks, the ranged one 2 ticks after the magic one (24 MC: each can be prayed)
        int magic = 0;
        int ranged = 0;
        int lastMagic = -100;
        for (int t = 1; t <= 40; t++) {
            boolean m = BossRealtime.huskAttacks(0, t);
            boolean r = BossRealtime.huskAttacks(1, t);
            assertFalse(m && r, "both husks on tick " + t);
            if (m) {
                magic++;
                lastMagic = t;
            }
            if (r) {
                ranged++;
                assertTrue((t - lastMagic) * BossRealtime.MC_PER_GAME_TICK >= BossRealtime.STYLE_SPACING_MC);
            }
        }
        assertEquals(10, magic, "magic husk every 4 ticks");
        assertEquals(10, ranged, "ranged husk every 4 ticks");
    }

    @Test
    void araxxorAcidBallSweepsInsteadOfLandingTogether() {
        // OSRS: tile i lands 1 + i/3 ticks after the spit; the floor would make tiles 2-8 land together
        for (int tile = 2; tile <= 12; tile++) {
            int d = BossRealtime.acidBallDelay(tile);
            assertTrue(d >= BossRealtime.STRIKE_FLOOR_TICKS, "tile " + tile);
            assertEquals(1 + tile / 3 + 2, d);
            assertEquals(d, ArenaTiming.strikeDue(0, d, true), "the floor no longer changes it");
        }
        assertTrue(BossRealtime.acidBallDelay(2) < BossRealtime.acidBallDelay(6));
        assertTrue(BossRealtime.acidBallDelay(6) < BossRealtime.acidBallDelay(9));
        assertTrue(BossRealtime.acidBallSplatterDelay(12) > BossRealtime.acidBallDelay(12), "the splatter after the path");
    }

    @Test
    void nexIce() {
        // the 5x5 around her 3x3 body: 2.5 tiles from her centre to the edge, 3.5 to a corner
        assertTrue(BossRealtime.NEX_CONTAINMENT_RADIUS >= 2.5 && BossRealtime.NEX_CONTAINMENT_RADIUS <= 3.5);
        assertEquals(60, BossRealtime.nexIceDamage(60, false));
        assertEquals(30, BossRealtime.nexIceDamage(60, true), "Protect from Missiles halves it");
        assertEquals(37, BossRealtime.nexIceDamage(75, true));
    }

    @Test
    void glyphGraceAndCountdowns() {
        assertFalse(BossRealtime.offGlyphCounts(10, 10));
        assertFalse(BossRealtime.offGlyphCounts(11, 10), "one tick to see the moved glyph and step on");
        assertTrue(BossRealtime.offGlyphCounts(12, 10));
        // the shockwave countdown starts 36 MC ahead
        assertEquals(BossRealtime.CUE_FLOOR_MC, BossRealtime.DOOM_SHOCKWAVE_CUE_TICKS * BossRealtime.MC_PER_GAME_TICK);
        assertTrue(BossRealtime.shockwaveCountdown(97, 100));
        assertFalse(BossRealtime.shockwaveCountdown(96, 100));
        assertFalse(BossRealtime.shockwaveCountdown(101, 100));
        assertFalse(BossRealtime.shockwaveCountdown(5, -1));
        assertEquals(3, BossRealtime.muspahSpikeEruptTicks(), "OSRS 2: floored to 3");
    }

    /** The Fragment of Seren's special comes every fourth attack she makes (counted per attack, not per goal tick). */
    @Test
    void fragmentOfSerenSpecialEveryFourthAttack() {
        int specials = 0;
        for (int made = 0; made < 40; made++) {
            if (QuestBossRules.fragmentSpecialNext(made)) {
                specials++;
                assertEquals(3, made % 4, "three normal attacks first");
            }
        }
        assertEquals(10, specials);
    }
}
