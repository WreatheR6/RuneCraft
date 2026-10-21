package com.runecraft.osrs;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The floor marker shapes (round 43 playability): which block columns a strike, tile area, path or ring marks. */
class FloorShapesTest {
    @Test
    void discsCoverTheirTiles() {
        // a block-centred strike: radius 1.5 is the 3x3 tiles round it, radius 1 a plus, a small one just its block
        assertEquals(9, FloorShapes.disc(10.5, 20.5, 1.5).size());
        assertEquals(5, FloorShapes.disc(10.5, 20.5, 1.0).size());
        assertEquals(1, FloorShapes.disc(10.5, 20.5, 0.5).size());
        assertEquals(1, FloorShapes.disc(10.5, 20.5, 0.0).size(), "the block under the centre is always marked");
        // off-centre: the 2x2 blocks round the corner a 0.9 circle mostly covers
        List<int[]> off = FloorShapes.disc(10.3, 5.7, 0.9);
        assertEquals(4, off.size());
        assertTrue(FloorShapes.contains(off, 10, 5) && FloorShapes.contains(off, 9, 5) && FloorShapes.contains(off, 10, 6)
                && FloorShapes.contains(off, 9, 6));
        // every marked column overlaps the strike's circle, and the column under any point well inside it is marked
        for (double r : new double[]{0.8, 1.2, 2.0, 3.0}) {
            List<int[]> d = FloorShapes.disc(3.37, -7.81, r);
            for (int[] c : d) {
                double nx = Math.max(c[0], Math.min(c[0] + 1, 3.37));
                double nz = Math.max(c[1], Math.min(c[1] + 1, -7.81));
                assertTrue(Math.hypot(nx - 3.37, nz + 7.81) < r, "column " + c[0] + "," + c[1] + " is outside a " + r + " strike");
            }
            for (double a = 0; a < Math.PI * 2; a += 0.3) {
                double x = 3.37 + Math.cos(a) * (r - 0.55);
                double z = -7.81 + Math.sin(a) * (r - 0.55);
                int[] col = FloorShapes.column(x, z);
                if (r > 0.6) {
                    assertTrue(FloorShapes.contains(d, col[0], col[1]), "a point " + (r - 0.55) + " in a " + r + " strike isn't marked");
                }
            }
        }
    }

    /**
     * A safe spot's disc (review repair): every block shown is safe all over (its farthest corner inside the test's circle), so
     * a player anywhere on a cyan block is safe; every block wholly inside is shown; the block under the centre always is.
     */
    @Test
    void safeDiscsShowOnlyBlocksWhollyInside() {
        // block-centred: the Titans' 1.5 circle is its own tile (the 3x3's edges reach 1.58), the Eclipse's 1.6 a plus, the
        // Doom's 2.5-block earthen shield the 3x3 (the next ring out reaches 2.55)
        assertEquals(1, FloorShapes.discInside(10.5, 20.5, 1.5).size());
        assertEquals(5, FloorShapes.discInside(10.5, 20.5, 1.6).size());
        assertEquals(9, FloorShapes.discInside(10.5, 20.5, 2.5).size());
        assertEquals(1, FloorShapes.discInside(10.5, 20.5, 0.2).size(), "the block under the centre is always shown");
        // the reviewer's case: a safe spot at (100.3, 200.3) radius 1.5; the strike disc marked (101, 201), whose far corner
        // (102, 202) is 2.47 away, a player standing at (101.9, 201.9) is 1.98 away and takes the blast
        assertTrue(FloorShapes.contains(FloorShapes.disc(100.3, 200.3, 1.5), 101, 201), "the danger disc leans outward");
        List<int[]> titans = FloorShapes.discInside(100.3, 200.3, 1.5);
        assertFalse(FloorShapes.contains(titans, 101, 201));
        assertTrue(FloorShapes.contains(titans, 100, 200) && FloorShapes.contains(titans, 99, 200)
                && FloorShapes.contains(titans, 100, 199));
        java.util.Random rnd = new java.util.Random(43);
        for (int i = 0; i < 400; i++) {
            double cx = rnd.nextDouble() * 40 - 20;
            double cz = rnd.nextDouble() * 40 - 20;
            double r = 1.42 + rnd.nextDouble() * 2.5;
            List<int[]> d = FloorShapes.discInside(cx, cz, r);
            assertTrue(d.size() <= FloorShapes.MAX_BLOCKS);
            for (int[] c : d) {
                for (int k = 0; k < 20; k++) {
                    double px = c[0] + rnd.nextDouble();
                    double pz = c[1] + rnd.nextDouble();
                    assertTrue(Math.hypot(px - cx, pz - cz) <= r, "a point on shown block " + c[0] + "," + c[1] + " is outside the "
                            + r + " safe circle round " + cx + "," + cz);
                }
            }
            for (int x = (int) Math.floor(cx - r) - 1; x <= (int) Math.floor(cx + r) + 1; x++) {
                for (int z = (int) Math.floor(cz - r) - 1; z <= (int) Math.floor(cz + r) + 1; z++) {
                    if (FloorShapes.farthestCorner(new int[]{x, z}, cx, cz) <= r) {
                        assertTrue(FloorShapes.contains(d, x, z), "block " + x + "," + z + " wholly inside isn't shown");
                    }
                }
            }
            int[] mid = FloorShapes.column(cx, cz);
            assertTrue(FloorShapes.contains(d, mid[0], mid[1]));
        }
        // too big to fill: a band inside the edge, still only blocks wholly inside
        List<int[]> big = FloorShapes.discInside(0.5, 0.5, 9.0);
        assertTrue(big.size() < FloorShapes.annulus(0.5, 0.5, -1, 9.0).size());
        assertFalse(FloorShapes.contains(big, 0, 0));
        for (int[] c : big) {
            assertTrue(FloorShapes.farthestCorner(c, 0.5, 0.5) <= 9.0);
        }
    }

    /** {@link FloorShapes#inside} keeps the columns whose every corner passes the area's test. */
    @Test
    void insideKeepsWholeColumns() {
        List<int[]> row = FloorShapes.square(-3, 0, 3, 0);
        // a half-plane x >= 0.5: the column at 0 straddles it, 1..3 are wholly in
        List<int[]> in = FloorShapes.inside(row, (x, z) -> x >= 0.5);
        assertEquals(3, in.size());
        assertFalse(FloorShapes.contains(in, 0, 0));
        assertTrue(FloorShapes.contains(in, 1, 0) && FloorShapes.contains(in, 3, 0));
        assertEquals(Math.hypot(3, 1), FloorShapes.farthestCorner(new int[]{0, 0}, 3, 0), 1e-9);
        assertEquals(Math.hypot(0.5, 0.5), FloorShapes.farthestCorner(new int[]{0, 0}, 0.5, 0.5), 1e-9);
        assertEquals(Math.hypot(2.5, 1.5), FloorShapes.farthestCorner(new int[]{1, 1}, -0.5, 0.5), 1e-9);
    }

    @Test
    void bigDiscsMarkTheirRim() {
        List<int[]> big = FloorShapes.disc(0.5, 0.5, 8.0);
        assertTrue(big.size() <= FloorShapes.annulus(0.5, 0.5, -1, 8.0).size());
        assertFalse(FloorShapes.contains(big, 0, 0), "a disc too big to fill keeps its middle");
        assertTrue(FloorShapes.contains(big, 8, 0), "its rim is marked");
        assertTrue(FloorShapes.disc(0.5, 0.5, 4.0).size() <= FloorShapes.MAX_BLOCKS);
    }

    @Test
    void ringsAndAnnuli() {
        List<int[]> ring = FloorShapes.annulus(0.5, 0.5, 4.0, 5.0);
        for (int[] c : ring) {
            double d = Math.hypot(c[0] + 0.5 - 0.5, c[1] + 0.5 - 0.5);
            assertTrue(d > 4.0 && d <= 5.0 + FloorShapes.SLACK);
        }
        assertTrue(FloorShapes.contains(ring, 5, 0) && FloorShapes.contains(ring, 0, -5) && !FloorShapes.contains(ring, 0, 0));
    }

    @Test
    void squaresAndBoxes() {
        assertEquals(9, FloorShapes.square(2, 2, 0, 0).size());
        // a Zulrah cloud: three blocks either way of its centre
        assertEquals(49, FloorShapes.box(10.5, 10.5, 3.0).size());
        assertEquals(36, FloorShapes.box(10.0, 10.0, 3.0).size());
        assertTrue(FloorShapes.contains(FloorShapes.box(10.0, 10.0, 0.4), 10, 10), "the block under the centre is always in");
    }

    @Test
    void linesRunFromStartToEnd() {
        List<int[]> l = FloorShapes.line(0.5, 0.5, 8.5, 0.5, 0.5);
        assertEquals(9, l.size(), "a straight 1-wide line of 9 blocks");
        for (int i = 0; i < l.size(); i++) {
            assertEquals(i, l.get(i)[0], "ordered along the path");
            assertEquals(0, l.get(i)[1]);
        }
        List<int[]> wide = FloorShapes.line(0.5, 0.5, 8.5, 0.5, 1.5);
        assertEquals(33, wide.size(), "3 wide, with rounded ends");
        List<int[]> diag = FloorShapes.line(0.5, 0.5, 6.5, 6.5, 0.5);
        assertTrue(FloorShapes.contains(diag, 3, 3) && !FloorShapes.contains(diag, 0, 6));
    }
}
