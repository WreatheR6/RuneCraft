package com.runecraft.osrs;

import com.runecraft.osrs.content.bosses.LeviathanRules;
import com.runecraft.osrs.content.minigames.FortisColosseum;
import com.runecraft.osrs.content.minigames.SolHeredit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-time floors of the scripted arenas (round 41 playability plan, scope arena-strikes-warnings): only when strikes land
 * and how they are shown change; the OSRS numbers behind them stay (their own tests check those).
 */
class ArenaTimingTest {
    @Test
    void damagingStrikesGetTheFloor() {
        assertEquals(3, ArenaTiming.STRIKE_FLOOR_TICKS);
        assertEquals(36, ArenaTiming.MIN_LEAD_MC);
        for (int delay = 0; delay <= 2; delay++) {
            assertEquals(103, ArenaTiming.strikeDue(100, delay, true), "delay " + delay);
        }
        assertEquals(103, ArenaTiming.strikeDue(100, 3, true));
        assertEquals(105, ArenaTiming.strikeDue(100, 5, true), "a slower OSRS strike keeps its delay");
        // logic-only strikes (no damage of their own) keep their tick
        assertEquals(100, ArenaTiming.strikeDue(100, 0, false));
        assertEquals(101, ArenaTiming.strikeDue(100, 1, false));
        assertEquals(102, ArenaTiming.strikeDue(100, 2, false));
        assertEquals(100, ArenaTiming.strikeDue(100, -1, false), "never in the past");
    }

    /** A strike made between game ticks still gets 36 Minecraft ticks from the moment it appears to the game tick it lands. */
    @Test
    void strikesMadeBetweenGameTicksKeepTheirLead() {
        for (int into = 0; into < ArenaTiming.MC_PER_GAME_TICK; into++) {
            for (int delay = 0; delay <= 6; delay++) {
                long due = ArenaTiming.strikeDue(50, delay, true, into);
                long lead = (due - 50) * ArenaTiming.MC_PER_GAME_TICK - into;
                assertTrue(lead >= ArenaTiming.MIN_LEAD_MC, "into " + into + ", delay " + delay + ": lead " + lead);
                assertTrue(due >= 50 + delay, "an OSRS delay is never shortened");
                assertTrue(lead < ArenaTiming.MIN_LEAD_MC + ArenaTiming.MC_PER_GAME_TICK || due == 50 + delay,
                        "floored no more than a game tick beyond the minimum");
            }
        }
        assertEquals(53, ArenaTiming.strikeDue(50, 0, true, 0));
        assertEquals(54, ArenaTiming.strikeDue(50, 0, true, 1));
        assertEquals(54, ArenaTiming.strikeDue(50, 0, true, 11));
        assertEquals(51, ArenaTiming.strikeDue(50, 1, false, 7), "logic-only strikes ignore the Minecraft ticks");
        assertEquals(50 * 12 + 36, ArenaTiming.dueMc(50 * 12, 50, 53));
    }

    /**
     * The raid clock between game ticks. On the boundary Minecraft tick the server's game tick has already moved on when the
     * arena's handler redraws its markers (its own game tick runs a moment later): the count must restart at 0 there, or a
     * strike landing that very tick reads 11 MC overdue (no final cue, no bell) and one a whole game tick away reads 1 MC left.
     */
    @Test
    void theArenaClockRestartsOnEachGameTick() {
        assertEquals(0, ArenaTiming.mcIntoTick(120, 120), "the game tick itself");
        assertEquals(5, ArenaTiming.mcIntoTick(125, 120));
        assertEquals(11, ArenaTiming.mcIntoTick(131, 120));
        assertEquals(0, ArenaTiming.mcIntoTick(132, 120), "the next boundary, before the arena's own game tick has run");
        assertEquals(3, ArenaTiming.mcIntoTick(147, 120), "game ticks missed keep their phase");
        assertEquals(0, ArenaTiming.mcIntoTick(118, 120), "a boundary stamped ahead (self-test drivers) never goes negative");
        // what the redraw on that boundary sees: game tick 11 (132 MC) has begun, a strike due on it is in its last cue
        long now = 11;
        long clock = now * ArenaTiming.MC_PER_GAME_TICK + ArenaTiming.mcIntoTick(132, 120);
        assertTrue(ArenaTiming.finalCue(11 * ArenaTiming.MC_PER_GAME_TICK, clock), "the strike landing this tick");
        assertFalse(ArenaTiming.finalCue(12 * ArenaTiming.MC_PER_GAME_TICK, clock), "a strike a whole game tick away");
    }

    @Test
    void whatIsDrawnIsWhatHits() {
        assertTrue(ArenaTiming.inStrike(1.9, 0.5, 0, 2.0), "1.9 blocks off, half a block up, radius 2");
        assertFalse(ArenaTiming.inStrike(2.1, 0.5, 0, 2.0), "2.1 blocks off");
        assertTrue(ArenaTiming.inStrike(1.2, -1.5, 1.2, 2.0), "diagonal inside the ring, a step below");
        assertFalse(ArenaTiming.inStrike(1.5, 0, 1.5, 2.0), "diagonal outside the ring");
        assertFalse(ArenaTiming.inStrike(0, 2.0, 0, 2.0), "two blocks above the ring is out of reach");
        assertFalse(ArenaTiming.inStrike(0, -2.5, 0, 5.0));
    }

    @Test
    void markersAreRedrawnAndWarningsMerge() {
        assertFalse(ArenaTiming.redrawDue(100, 103));
        assertTrue(ArenaTiming.redrawDue(100, 104));
        assertTrue(ArenaTiming.finalCue(136, 126));
        assertTrue(ArenaTiming.finalCue(136, 136));
        assertFalse(ArenaTiming.finalCue(136, 125));
        assertFalse(ArenaTiming.finalCue(136, 137));
        assertTrue(ArenaTiming.mergesTelegraph(100, 105), "two warnings 5 MC apart share a line");
        assertTrue(ArenaTiming.mergesTelegraph(100, 109));
        assertFalse(ArenaTiming.mergesTelegraph(100, 110));
        assertFalse(ArenaTiming.mergesTelegraph(100, 99));
        for (double r = 0.3; r <= 12; r += 0.1) {
            int alone = ArenaTiming.ringPoints(r, 1);
            assertTrue(alone >= 10 && alone <= 48, r + ": " + alone);
            int crowd = ArenaTiming.ringPoints(r, 30);
            assertTrue(crowd >= 8 && crowd <= alone, r + " with 30 pending: " + crowd);
        }
        assertEquals(48, ArenaTiming.ringPoints(2.0, 1));
    }

    @Test
    void strikeTracesJudgeFairness() {
        assertNull(new ArenaTiming.StrikeTrace(120, 120, 156, 9, 2, 10, true, false).unfair());
        assertNotNull(new ArenaTiming.StrikeTrace(120, 120, 144, 6, 2, 10, true, false).unfair(), "24 MC is too fast");
        assertNotNull(new ArenaTiming.StrikeTrace(120, 132, 168, 6, 2, 10, true, false).unfair(), "drawn a game tick late");
        assertNull(new ArenaTiming.StrikeTrace(120, 120, 132, 3, 2, 10, true, true).unfair(), "exact logic keeps its tick");
        assertNull(new ArenaTiming.StrikeTrace(120, 120, 132, 3, 2, 0, false, false).unfair(), "harmless markers are free");
        assertEquals(36, new ArenaTiming.StrikeTrace(120, 120, 156, 9, 2, 10, true, false).leadMc());
    }

    /** The Leviathan's volleys as played: orbs at least 2 ticks apart, at least 3 ticks in flight, at every stage. */
    @Test
    void leviathanOrbsHaveRealTimeFloors() {
        for (int stage = 0; stage < LeviathanRules.STAGES; stage++) {
            for (boolean enraged : new boolean[]{false, true}) {
                for (boolean awakened : new boolean[]{false, true}) {
                    assertTrue(LeviathanRules.spacing(stage, enraged, awakened) >= 2, "spacing at stage " + stage);
                }
                assertTrue(LeviathanRules.react(stage, enraged) >= 3, "reaction time at stage " + stage);
                assertEquals(Math.max(2, LeviathanRules.osrsSpacing(stage, enraged, false)), LeviathanRules.spacing(stage, enraged, false));
            }
        }
    }

    /** Manticore orbs: the first no sooner than 3 ticks after the launch, then 2 apart; the pattern and max hits unchanged. */
    @Test
    void manticoreOrbsLandAfterALead() {
        for (long launch : new long[]{0, 17, 1000}) {
            long previous = Long.MIN_VALUE / 2;
            for (int i = 0; i < 3; i++) {
                long at = FortisColosseum.manticoreOrbTick(launch, i);
                assertTrue(at >= launch + 3, "orb " + i + " lands at " + (at - launch));
                assertTrue(at - previous >= 2, "orbs " + (i - 1) + " and " + i + " land " + (at - previous) + " apart");
                previous = at;
            }
        }
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.MAGIC));
        assertEquals(36, FortisColosseum.manticoreOrbMaxHit(AttackType.RANGED));
        assertEquals(31, FortisColosseum.manticoreOrbMaxHit(AttackType.CRUSH));
    }

    /** Sol Heredit's triple parry: Protect from Melee on as the hit lands blocks it, however long it has been on. */
    @Test
    void solTripleParryIsReadAtImpact() {
        for (long since : new long[]{-1, 0, 50, 98, 99, 100}) {
            assertTrue(SolHeredit.parried(true, since, 100), "on since " + since);
            assertFalse(SolHeredit.parried(false, since, 100));
        }
        assertEquals(15, SolHeredit.tripleDamage(0, 2));
        assertEquals(45, SolHeredit.tripleDamage(2, 4));
        assertTrue(SolHeredit.AOE_LAND_TICKS >= ArenaTiming.STRIKE_FLOOR_TICKS, "the spear/shield dust gets the strike floor");
    }
}
