package com.runecraft.osrs;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real-player numbers of the round 44 walks (RealReaction) and their walking routes (GridPath): a human's pace and
 * reaction, the walk-out budget against the round 41 floors, the nearest safe spot, the prayer a set of cues asks for.
 */
class RealReactionTest {
    @Test
    void humanPaceAndReaction() {
        assertEquals(4.317, RealReaction.RUN_BLOCKS_PER_SECOND, 1e-9);
        assertTrue(RealReaction.REACTION_MC >= RealReaction.REACTION_MIN_MC && RealReaction.REACTION_MC <= RealReaction.REACTION_MAX_MC);
        assertEquals(6, RealReaction.REACTION_MIN_MC, "300 ms");
        assertEquals(10, RealReaction.REACTION_MAX_MC, "500 ms");
        // 4.317 blocks in 20 MC; 1 block takes 5 MC
        assertEquals(20, RealReaction.walkMc(4.317));
        assertEquals(5, RealReaction.walkMc(1.0));
        assertEquals(0, RealReaction.walkMc(0));
    }

    /** distance / 4.317 + 0.25 s <= lead: the round 41 floor of 36 MC (1.8 s) lets a player walk out of 6.69 blocks. */
    @Test
    void walkOutBudgetAgainstTheFloor() {
        long floor = ArenaTiming.MIN_LEAD_MC;
        assertEquals(36, floor);
        assertTrue(RealReaction.fits(0, floor));
        assertTrue(RealReaction.fits(3.0, floor), "a radius-3 strike centred on the player");
        assertTrue(RealReaction.fits(6.6, floor));
        assertFalse(RealReaction.fits(6.8, floor));
        // the budget's own margin: a strike with no lead can't be escaped even from its edge
        assertFalse(RealReaction.fits(0, 4));
        assertTrue(RealReaction.fits(0, 5));
        assertNull(RealReaction.budgetFailure("bomb", 3.0, floor));
        String why = RealReaction.budgetFailure("bomb", 8.0, floor);
        assertNotNull(why);
        assertTrue(why.startsWith("bomb: 8.0 blocks to safety"), why);
        assertTrue(why.contains("leads by 1.80 s"), why);
    }

    @Test
    void exitDistanceCountsTheEdge() {
        assertEquals(3.0 + RealReaction.EDGE_CLEARANCE, RealReaction.exitDistance(0, 0, 3.0), 1e-9);
        assertEquals(1.0 + RealReaction.EDGE_CLEARANCE, RealReaction.exitDistance(2.0, 0, 3.0), 1e-9);
        assertEquals(0, RealReaction.exitDistance(4.0, 0, 3.0), 1e-9);
    }

    @Test
    void safeSpotLeavesEveryDiscLandingInTime() {
        RealReaction.Disc a = new RealReaction.Disc(0, 0, 2.0, 100, "a");
        RealReaction.Disc later = new RealReaction.Disc(3, 0, 2.0, 500, "later");
        double[] spot = RealReaction.safeSpot(0, 0, List.of(a), 120, 10, (x, z) -> true);
        assertNotNull(spot);
        assertEquals(2.0 + RealReaction.EDGE_CLEARANCE, spot[2], 0.26);
        assertFalse(a.covers(spot[0], spot[1]));
        // a disc landing after the horizon doesn't count; one before it does
        double[] withLater = RealReaction.safeSpot(0, 0, List.of(a, later), 120, 10, (x, z) -> true);
        assertEquals(spot[2], withLater[2], 1e-9);
        double[] both = RealReaction.safeSpot(0, 0, List.of(a, later), 600, 10, (x, z) -> true);
        assertFalse(a.covers(both[0], both[1]) || later.covers(both[0], both[1]));
        // walls: only the west side is standable
        double[] west = RealReaction.safeSpot(0, 0, List.of(a), 120, 10, (x, z) -> x < -0.5);
        assertTrue(west[0] < -0.5 && !a.covers(west[0], west[1]));
        assertTrue(west[2] < 2.0 + RealReaction.EDGE_CLEARANCE + 0.6, "the nearest standable edge, not the far side: " + west[2]);
        // nowhere to go
        assertNull(RealReaction.safeSpot(0, 0, List.of(a), 120, 1.0, (x, z) -> true));
        // already safe
        assertEquals(0, RealReaction.safeSpot(5, 5, List.of(a), 120, 10, (x, z) -> true)[2], 1e-9);
    }

    /** The prayer to have up: the soonest hit not yet landed whose cue the player has had REACTION_MC to read. */
    @Test
    void wantedProtectionFollowsTheNextHit() {
        int magic = 0;
        int missiles = 1;
        int melee = 2;
        int[] prot = {magic, missiles, -1};
        long[] cue = {100, 104, 100};
        long[] land = {140, 124, 110};
        // before any reaction: nothing
        assertEquals(-1, RealReaction.wantedProtection(prot, cue, land, 105));
        // magic is readable at 108; the missile (lands sooner) only at 112
        assertEquals(magic, RealReaction.wantedProtection(prot, cue, land, 108));
        assertEquals(missiles, RealReaction.wantedProtection(prot, cue, land, 112));
        // the missile landed: back to magic
        assertEquals(magic, RealReaction.wantedProtection(prot, cue, land, 125));
        assertEquals(-1, RealReaction.wantedProtection(prot, cue, land, 141));
        assertEquals(melee, RealReaction.wantedProtection(new int[]{melee}, new long[]{0}, new long[]{50}, 8));
        assertEquals(2, RealReaction.protectionOf(AttackType.CRUSH));
        assertEquals(0, RealReaction.protectionOf(AttackType.MAGIC));
        assertEquals(1, RealReaction.protectionOf(AttackType.RANGED));
        assertEquals(-1, RealReaction.protectionOf((AttackType) null));
        assertEquals(-1, RealReaction.protectionOf(CastBar.TYPELESS));
        assertEquals(2, RealReaction.protectionOf(CastBar.MELEE));
    }

    // ------------------------------------------------------------------ GridPath

    /** A 20x20 room with a pillar: walls are NONE. */
    private static GridPath.Floor room(boolean[][] wall) {
        return (x, z, nearY) -> x < 0 || z < 0 || x >= wall.length || z >= wall[0].length || wall[x][z] ? GridPath.NONE : 64;
    }

    @Test
    void routesRoundAPillar() {
        boolean[][] wall = new boolean[20][20];
        for (int x = 8; x <= 11; x++) {
            for (int z = 4; z <= 15; z++) {
                wall[x][z] = true;
            }
        }
        GridPath.Floor floor = room(wall);
        List<int[]> path = GridPath.find(5, 10, 64, 14, 10, 32, 5000, floor);
        assertNotNull(path);
        int[] last = path.get(path.size() - 1);
        assertEquals(14, last[0]);
        assertEquals(10, last[2]);
        for (int[] c : path) {
            assertFalse(wall[c[0]][c[2]], "the route crosses the pillar at " + c[0] + "," + c[2]);
        }
        List<int[]> smooth = GridPath.smooth(new int[]{5, 64, 10}, path, floor);
        assertTrue(smooth.size() < path.size() && smooth.size() >= 2, "corners cut: " + smooth.size() + " of " + path.size());
        assertEquals(14, smooth.get(smooth.size() - 1)[0]);
        // already there; walled in
        assertTrue(GridPath.find(3, 3, 64, 3, 3, 32, 100, floor).isEmpty());
        boolean[][] box = new boolean[20][20];
        for (int i = 0; i < 20; i++) {
            box[i][6] = true;
        }
        assertNull(GridPath.find(3, 3, 64, 3, 12, 32, 5000, room(box)));
    }

    @Test
    void noCornerCuttingAndOneBlockSteps() {
        boolean[][] wall = new boolean[5][5];
        wall[1][0] = true;
        wall[0][1] = true;
        // (0,0) to (1,1) diagonally past two walls: no way
        assertNull(GridPath.find(0, 0, 64, 1, 1, 8, 500, room(wall)));
        // a two-block rise can't be climbed, a one-block one can
        GridPath.Floor steps = (x, z, nearY) -> x < 0 || x > 6 || z < 0 || z > 0 ? GridPath.NONE : x < 3 ? 64 : x == 3 ? 65 : 66;
        List<int[]> up = GridPath.find(0, 0, 64, 6, 0, 8, 500, steps);
        assertNotNull(up);
        assertEquals(66, up.get(up.size() - 1)[1]);
        GridPath.Floor cliff = (x, z, nearY) -> x < 0 || x > 6 || z != 0 ? GridPath.NONE : x < 3 ? 64 : 66;
        assertNull(GridPath.find(0, 0, 64, 6, 0, 8, 500, cliff));
    }

    /** A goal off the floor (past the self-test platform's barrier wall): the route stops at the floor's nearest column. */
    @Test
    void nearestReachableColumnForAGoalOffTheFloor() {
        boolean[][] none = new boolean[10][10];
        GridPath.Floor floor = room(none);
        List<int[]> path = GridPath.findNearest(2, 5, 64, 14, 5, 32, 5000, floor);
        assertNotNull(path);
        int[] last = path.get(path.size() - 1);
        assertEquals(9, last[0], "stops at the east edge");
        assertEquals(5, last[2]);
        assertNull(GridPath.find(2, 5, 64, 14, 5, 32, 5000, floor), "find still says there is no way");
        // a reachable goal: the same route as find
        assertEquals(GridPath.find(2, 5, 64, 7, 8, 32, 5000, floor).size(), GridPath.findNearest(2, 5, 64, 7, 8, 32, 5000, floor).size());
        // the start is already the nearest column: nowhere to go
        boolean[][] wallEast = new boolean[10][10];
        for (int z = 0; z < 10; z++) {
            wallEast[3][z] = true;
        }
        assertTrue(GridPath.findNearest(2, 5, 64, 8, 5, 32, 5000, room(wallEast)).isEmpty());
    }
}
