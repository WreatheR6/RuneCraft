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
        double[] none = {};
        assertEquals(0.2, RaidRealtime.ownStep(0.2, 0, none, false), 1e-9, "walking");
        assertEquals(0.065, RaidRealtime.ownStep(0, 0.065, none, false), 1e-9, "sneaking");
        assertEquals(0, RaidRealtime.ownStep(0.01, 0, none, false), "standing still (drift)");
        assertEquals(0, RaidRealtime.ownStep(3.0, 0, none, false), "teleported");
        assertEquals(0, RaidRealtime.ownStep(0.2, 0, none, true), "sliding on ice");
        // In the air counts too (round 42 review): there is no on-ground condition any more, a jump is a step.
        assertTrue(RaidRealtime.ownStep(0.2, 0, none, false) > 0, "jumping (in the air)");
        assertEquals(0.6, RaidRealtime.ownStep(0.36, 0.48, none, false), 1e-9, "the jump tick of a sprint-jump");

        // Pressed against a body east of the player (the player is west of it: away = (-1, 0)).
        double[] west = {-0.5, 0};
        assertEquals(0, RaidRealtime.ownStep(-0.2, 0, west, false), "shoved straight away from a body");
        assertEquals(0.2, RaidRealtime.ownStep(0, 0.2, west, false), 1e-9, "walking round a body, pressed against it");
        assertEquals(0.2, RaidRealtime.ownStep(0.2, 0, west, false), 1e-9, "walking into a body");
        assertEquals(0.2, RaidRealtime.ownStep(-0.2, 0.2, west, false), 1e-9, "only the shove (away) part is taken off");
        assertEquals(0.2, RaidRealtime.ownStep(0, 0.2, new double[]{0, 0}, false), 1e-9, "a body centre on centre doesn't push");
        assertEquals(0, RaidRealtime.ownStep(-0.2, 0.2, new double[]{-1, 0, 0, 1}, false), "squeezed out between two bodies");

        // A sprint-jump across the arena: about 11 Minecraft ticks in the air for each one on the ground. The game tick's
        // steps add up past an orb's step either way (they used to count only the tick on the ground).
        double airborne = 0;
        for (int mc = 0; mc < 12; mc++) {
            airborne += RaidRealtime.ownStep(0.3, 0, none, false);
        }
        assertTrue(airborne >= RaidRealtime.ORB_STEP, "a game tick of jumping drops an orb (" + airborne + ")");
        // Walking round Akkha pressed against him (0.6 from his centre, 0.2 a Minecraft tick): the steps still add up.
        double round = 0;
        double r = 0.6;
        double a = 0;
        for (int mc = 0; mc < 12; mc++) {
            double b = a + 0.2 / r;
            double nx = r * Math.cos(b);
            double nz = r * Math.sin(b);
            round += RaidRealtime.ownStep(nx - r * Math.cos(a), nz - r * Math.sin(a), new double[]{nx, nz}, false);
            a = b;
        }
        assertTrue(round >= RaidRealtime.ORB_STEP, "a game tick walking round Akkha drops an orb (" + round + ")");
    }

    @Test
    void floorColumns() {
        // a 1.5-block disc round a block centre: the 3x3 plus the four columns two out along the axes (13)
        List<int[]> disc = RaidRealtime.discColumns(10.5, 20.5, 1.5);
        assertEquals(9, disc.size(), "the 3x3 (corners at 1.41) and nothing two out (2.0 > 1.5)");
        assertEquals(13, RaidRealtime.discColumns(10.5, 20.5, 2.0).size());
        // a tiny disc still marks the column under it
        List<int[]> dot = RaidRealtime.discColumns(3.2, -4.7, 0.1);
        assertEquals(1, dot.size());
        assertEquals(3, dot.get(0)[0]);
        assertEquals(-5, dot.get(0)[1]);
        // a lane one column wide from x 0 to 5 at z 0.5
        List<int[]> lane = RaidRealtime.laneColumns(0.5, 0.5, 5.5, 0.5, 0.4);
        assertEquals(6, lane.size());
        for (int[] c : lane) {
            assertEquals(0, c[1]);
        }
        assertEquals(12, RaidRealtime.rectColumns(0, 0, 2, 3).size());
        assertEquals(12, RaidRealtime.rectColumns(2, 3, 0, 0).size(), "corners in any order");
    }

    @Test
    void memoryBlastAndShadows() {
        // the OSRS four ticks stay; Feeling Special?'s two become the strike floor (36 MC)
        assertEquals(4, RaidRealtime.memoryBlastGap(4));
        assertEquals(3, RaidRealtime.memoryBlastGap(2));
        assertTrue(RaidRealtime.MEMORY_BLAST_MIN_GAP * 12 >= com.runecraft.osrs.ArenaTiming.MIN_LEAD_MC);
        assertTrue(RaidRealtime.SHADOW_SLAM_LEAD_TICKS * 12 >= com.runecraft.osrs.ArenaTiming.MIN_LEAD_MC);
        assertTrue(RaidRealtime.HET_BEAM_LEAD_TICKS * 12 >= com.runecraft.osrs.ArenaTiming.MIN_LEAD_MC);
    }

    @Test
    void olmZones() {
        // on the north wall the Olm faces south: its left is east
        assertEquals(RaidRealtime.OLM_MIDDLE, RaidRealtime.olmZone(0, false));
        assertEquals(RaidRealtime.OLM_LEFT, RaidRealtime.olmZone(10, false));
        assertEquals(RaidRealtime.OLM_RIGHT, RaidRealtime.olmZone(-10, false));
        // on the south wall it faces north: its left is west
        assertEquals(RaidRealtime.OLM_LEFT, RaidRealtime.olmZone(-10, true));
        assertEquals(RaidRealtime.OLM_RIGHT, RaidRealtime.olmZone(10, true));
        assertEquals("east", RaidRealtime.olmZoneCompass(RaidRealtime.OLM_LEFT, false));
        assertEquals("west", RaidRealtime.olmZoneCompass(RaidRealtime.OLM_LEFT, true));
        assertEquals("centre", RaidRealtime.olmZoneCompass(RaidRealtime.OLM_MIDDLE, true));
    }

    @Test
    void olmHeadSwerves() {
        RaidRealtime.OlmHead head = new RaidRealtime.OlmHead();
        int[] middle = {0, 1, 0};
        int[] left = {2, 0, 1};
        // a player in the zone it faces: it acts
        RaidRealtime.OlmHead.Step s = head.step(middle, true, -1);
        assertTrue(s.act() && !s.turned() && !s.catchUp());
        // nobody there: it swerves to the zone with the most players and skips the step
        s = head.step(left, true, -1);
        assertFalse(s.act());
        assertTrue(s.turned());
        assertEquals(RaidRealtime.OLM_LEFT, s.facing());
        // the skipped standard attack is caught up on the next step, beside that step's own action (a special here)
        s = head.step(left, false, -1);
        assertTrue(s.act() && s.catchUp());
        // ... once
        s = head.step(left, true, -1);
        assertTrue(s.act() && !s.catchUp());
        // a skipped special is never caught up
        s = head.step(new int[]{0, 0, 1}, false, -1);
        assertFalse(s.act());
        assertEquals(RaidRealtime.OLM_RIGHT, s.facing());
        s = head.step(new int[]{0, 0, 1}, true, -1);
        assertTrue(s.act() && !s.catchUp());
        // only the step just before is caught up: two skipped steps don't cascade
        head.step(new int[]{1, 0, 0}, true, -1); // turns left, owes one
        head.step(new int[]{0, 1, 0}, false, -1); // turns middle, skipping a special: the standard owed before is gone
        s = head.step(new int[]{0, 1, 0}, true, -1);
        assertTrue(s.act() && !s.catchUp());
        // a hand damaged within four ticks draws the head to its side when it scans nobody
        RaidRealtime.OlmHead h2 = new RaidRealtime.OlmHead();
        s = h2.step(new int[]{0, 0, 0}, true, RaidRealtime.OLM_RIGHT);
        assertEquals(RaidRealtime.OLM_RIGHT, s.facing());
        assertFalse(s.act());
        // nobody anywhere: it stays and skips
        RaidRealtime.OlmHead h3 = new RaidRealtime.OlmHead();
        s = h3.step(new int[]{0, 0, 0}, true, -1);
        assertEquals(RaidRealtime.OLM_MIDDLE, s.facing());
        assertFalse(s.turned() || s.act());
        // ties go to the middle
        RaidRealtime.OlmHead h4 = new RaidRealtime.OlmHead();
        h4.step(new int[]{1, 0, 0}, true, -1);
        s = h4.step(new int[]{0, 1, 1}, true, -1);
        assertEquals(RaidRealtime.OLM_MIDDLE, s.facing());
    }

    @Test
    void verzikLightningBall() {
        // two players either side of Verzik (at the origin): the ball arcs through her
        double[][] across = {{-4, 0}, {4, 0}};
        assertEquals(RaidRealtime.Chain.THROUGH_VERZIK, RaidRealtime.lightningBounce(1, 0, across, 0, 0, 0).chain());
        // two players side by side to her north: it jumps between them
        double[][] together = {{-2, -6}, {2, -6}};
        RaidRealtime.Bounce b = RaidRealtime.lightningBounce(1, 0, together, 0, 0, 0);
        assertEquals(RaidRealtime.Chain.NEXT, b.chain());
        assertEquals(1, b.next());
        b = RaidRealtime.lightningBounce(2, 1, together, 0, 0, 7);
        assertEquals(0, b.next(), "back to the only teammate in range");
        // the fourth player it reaches is struck (OSRS: "chained between 4 players")
        assertEquals(RaidRealtime.Chain.STRIKE, RaidRealtime.lightningBounce(RaidRealtime.VERZIK_LIGHTNING_PLAYERS, 0, together, 0, 0, 0).chain());
        // nobody within range: the one it reached is struck
        double[][] apart = {{-12, 0}, {12, 0}};
        assertEquals(RaidRealtime.Chain.STRIKE, RaidRealtime.lightningBounce(1, 0, apart, 0, -20, 0).chain());
        // solo: always struck
        assertEquals(RaidRealtime.Chain.STRIKE, RaidRealtime.lightningBounce(1, 0, new double[][]{{3, 3}}, 0, 0, 0).chain());
        // three players: the pick chooses among the teammates in range, never the holder
        double[][] three = {{0, -6}, {3, -6}, {-3, -6}};
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int pick = 0; pick < 6; pick++) {
            RaidRealtime.Bounce x = RaidRealtime.lightningBounce(1, 0, three, 0, 0, pick);
            assertEquals(RaidRealtime.Chain.NEXT, x.chain());
            seen.add(x.next());
        }
        assertEquals(Set.of(1, 2), seen);
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
