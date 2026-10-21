package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The raids' real-time adaptations (round 42 playability): spacing, lenient areas and compass words. */
class RaidRealtimeTest {
    @Test
    void spacing() {
        // a hit needing another answer waits two game ticks (24 MC) after the last; the same answer keeps its tick
        assertEquals(12, RaidRealtime.spacedTick(10, 10, true));
        assertEquals(12, RaidRealtime.spacedTick(11, 10, true));
        assertEquals(12, RaidRealtime.spacedTick(12, 10, true));
        assertEquals(15, RaidRealtime.spacedTick(15, 10, true));
        assertEquals(10, RaidRealtime.spacedTick(10, 10, false));
        // never earlier than wanted, and one well before or after the other keeps its tick
        assertEquals(20, RaidRealtime.spacedTick(20, 30, false));
        assertEquals(5, RaidRealtime.spacedTick(5, 30, true));
        assertEquals(9, RaidRealtime.spacedTick(9, 11, true), "two ticks before it is far enough");
        assertEquals(13, RaidRealtime.spacedTick(10, 11, true), "a tick before it waits until two after it");
        assertTrue(RaidRealtime.SEQUENCE_GAP_TICKS * 12 >= 20, "at least the round 41 style spacing (20 MC)");
        assertTrue(RaidRealtime.PROJECTILE_MIN_TICKS * 12 >= 16, "at least the round 41 boss projectile flight (16 MC)");
    }

    @Test
    void timeline() {
        RaidRealtime.Timeline t = new RaidRealtime.Timeline();
        assertEquals(14, t.schedule(10, 4, "crystal burst"));
        // another kind wanted on the same tick lands two ticks later; a third kind after that
        assertEquals(16, t.schedule(10, 4, "lightning"));
        assertEquals(18, t.schedule(11, 3, "teleport"));
        // the same kind may land with its own
        assertEquals(14, t.schedule(12, 2, "crystal burst"));
        assertEquals(List.of("crystal burst", "lightning", "teleport"), t.order(12));
        assertEquals("1. crystal burst · 2. lightning", RaidRealtime.orderCue(t.order(12), 2));
        assertEquals(4, t.ticksUntil(12, "lightning"));
        assertEquals(-1, t.ticksUntil(12, "acid"));
        // landed ones drop off; a hit wanted well apart keeps its tick
        assertEquals(List.of("teleport"), t.order(17));
        assertEquals(30, t.schedule(20, 10, "fire wall"));
        // a delay is never shortened
        for (int d = 0; d < 8; d++) {
            RaidRealtime.Timeline u = new RaidRealtime.Timeline();
            u.schedule(0, 3, "a");
            assertTrue(u.schedule(0, d, "b") >= d);
        }
    }

    @Test
    void lanesAndShelter() {
        assertTrue(RaidRealtime.inLane(0, 0.9, -10, 0, 10, 0, 1.0));
        assertFalse(RaidRealtime.inLane(0, 1.1, -10, 0, 10, 0, 1.0));
        assertFalse(RaidRealtime.inLane(11.5, 0, -10, 0, 10, 0, 1.0), "past the lane's end");
        // Zebak: the three tiles behind a rock in its row always shelter (OSRS), and the cone widens behind it
        for (double behind = 0.6; behind <= 3.5; behind += 0.5) {
            assertTrue(RaidRealtime.inShelterCone(behind, 0.0));
            assertTrue(RaidRealtime.inShelterCone(behind, 0.49), "the rock's own row at " + behind);
        }
        assertFalse(RaidRealtime.inShelterCone(0.4, 0), "beside the rock, not behind it");
        assertFalse(RaidRealtime.inShelterCone(3.6, 0), "more than three tiles behind");
        assertFalse(RaidRealtime.inShelterCone(1.0, 0.8), "near the rock the shelter is its row");
        assertTrue(RaidRealtime.inShelterCone(3.0, 1.1), "further back it widens");
        assertFalse(RaidRealtime.inShelterCone(3.0, 1.5));
    }

    @Test
    void mazeLeniency() {
        // a 3x3 grid of 2-block tiles; the path holds the middle column
        Set<Integer> path = Set.of(1);
        java.util.function.BiPredicate<Integer, Integer> on = (c, r) -> path.contains(c);
        assertFalse(RaidRealtime.offPath(3.0, 3.0, 2, 3, 3, on), "on the path");
        assertTrue(RaidRealtime.offPath(1.0, 3.0, 2, 3, 3, on), "in the middle of a wrong tile");
        assertFalse(RaidRealtime.offPath(1.7, 3.0, 2, 3, 3, on), "a foot over the edge of the path tile");
        assertTrue(RaidRealtime.offPath(1.6, 3.0, 2, 3, 3, on), "more than the leniency over it");
        assertFalse(RaidRealtime.offPath(4.3, 3.0, 2, 3, 3, on), "the other side, a foot over");
        assertFalse(RaidRealtime.offPath(-1, 3.0, 2, 3, 3, on), "off the grid");
    }

    @Test
    void trailingOrbSteps() {
        assertTrue(RaidRealtime.inputStep(0.2, true, false, false), "walking");
        assertTrue(RaidRealtime.inputStep(0.065, true, false, false), "sneaking");
        assertFalse(RaidRealtime.inputStep(0.01, true, false, false), "standing still (drift)");
        assertFalse(RaidRealtime.inputStep(3.0, true, false, false), "teleported");
        assertFalse(RaidRealtime.inputStep(0.2, true, true, false), "shoved by a body");
        assertFalse(RaidRealtime.inputStep(0.2, true, false, true), "sliding on ice");
        assertFalse(RaidRealtime.inputStep(0.2, false, false, false), "in the air");
    }

    @Test
    void words() {
        assertEquals("north", RaidRealtime.compass(0, -5));
        assertEquals("south-east", RaidRealtime.compass(3, 3));
        assertEquals("west", RaidRealtime.compass(-4, 0.5));
        assertEquals("centre", RaidRealtime.compass(0, 0));
        assertEquals("north-west", RaidRealtime.quarter(true, true));
        // the slam cue names the side ToaBossRules.slamHits hits (the Warden faces west: its right is north, z < 0)
        for (int kind = 0; kind < 3; kind++) {
            String cue = RaidRealtime.wardenSlamCue(kind);
            boolean north = ToaBossRules.slamHits(kind, -3);
            boolean south = ToaBossRules.slamHits(kind, 3);
            assertEquals(north, cue.contains("NORTH"), cue);
            assertEquals(south, cue.contains("SOUTH"), cue);
        }
    }
}
