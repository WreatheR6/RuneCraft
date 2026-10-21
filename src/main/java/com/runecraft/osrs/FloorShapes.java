package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The block columns a ground mechanic's floor marker covers (round 43, // playability: OSRS tints the tiles a mechanic will
 * hit; a ring of particles is easy to miss in real time). Pure geometry on block columns {@code {x, z}}: a disc for a strike, a
 * square for a tile area, a line for a path, a ring for a shockwave. The hit tests stay what they were (round 41's horizontal
 * radius, which is a little more lenient than the blocks shown): the blocks show where the mechanic lands, not a new rule.
 *
 * <p>A column is in a disc when its centre lies less than the radius plus {@link #SLACK} from the centre point, so a strike centred
 * off a block's middle still marks every block its circle mostly covers; the block under the centre is always in. Shapes are
 * capped at {@link #MAX_BLOCKS} blocks: a larger disc marks only its rim (an area too big to fill is still outlined).
 */
public final class FloorShapes {
    /** How far (blocks) past a shape's edge a column's centre may lie and still be marked. */
    public static final double SLACK = 0.25;
    /** The most blocks one shape swaps; a larger disc marks its rim instead. */
    public static final int MAX_BLOCKS = 64;
    /** The width (blocks) of a rim. */
    public static final double RIM = 1.0;

    private FloorShapes() {
    }

    /** The column {@code {x, z}} a point stands in. */
    public static int[] column(double x, double z) {
        return new int[]{(int) Math.floor(x), (int) Math.floor(z)};
    }

    /** The columns of a disc of {@code radius} round ({@code cx}, {@code cz}); its rim alone when it would exceed the cap. */
    public static List<int[]> disc(double cx, double cz, double radius) {
        List<int[]> full = annulus(cx, cz, -1, radius);
        if (full.size() <= MAX_BLOCKS) {
            return full;
        }
        return ring(cx, cz, radius);
    }

    /** The rim of a disc: columns whose centre lies within {@link #RIM} inside its edge (or {@link #SLACK} outside it). */
    public static List<int[]> ring(double cx, double cz, double radius) {
        return annulus(cx, cz, Math.max(-1, radius - RIM), radius);
    }

    /**
     * The columns whose centre lies more than {@code inner} and at most {@code outer} (+{@link #SLACK}) from the centre; with
     * {@code inner < 0} the column under the centre is always in. Not capped (a shockwave ring is the whole point).
     */
    public static List<int[]> annulus(double cx, double cz, double inner, double outer) {
        Set<Long> seen = new LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        if (inner < 0) {
            add(out, seen, column(cx, cz));
        }
        double reach = Math.max(0, outer) + SLACK;
        int x0 = (int) Math.floor(cx - reach - 1);
        int x1 = (int) Math.floor(cx + reach + 1);
        int z0 = (int) Math.floor(cz - reach - 1);
        int z1 = (int) Math.floor(cz + reach + 1);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (d < reach && d > inner) {
                    add(out, seen, new int[]{x, z});
                }
            }
        }
        return out;
    }

    /**
     * A safe area's disc (round 43 repair): only the columns lying wholly inside the circle of {@code radius} round ({@code cx},
     * {@code cz}) (their farthest corner within it), and the column under the centre. A safe spot leans the other way from a
     * strike: every block shown cyan is safe wherever on it the player stands, and the test's circle stays a little larger than
     * what is shown. Capped like {@link #disc}: past {@link #MAX_BLOCKS} only a band two blocks deep inside the edge is kept.
     */
    public static List<int[]> discInside(double cx, double cz, double radius) {
        double r = Math.max(0, radius);
        List<int[]> full = new ArrayList<>();
        int[] middle = column(cx, cz);
        full.add(middle);
        for (int[] c : annulus(cx, cz, -1, r)) {
            if ((c[0] != middle[0] || c[1] != middle[1]) && farthestCorner(c, cx, cz) <= r) {
                full.add(c);
            }
        }
        if (full.size() <= MAX_BLOCKS) {
            return full;
        }
        List<int[]> rim = new ArrayList<>();
        for (int[] c : full) {
            if (farthestCorner(c, cx, cz) > r - 2 * RIM) {
                rim.add(c);
            }
        }
        return rim;
    }

    /** A test of a point ({@code x}, {@code z}) in blocks: whether a mechanic's area holds it. */
    @FunctionalInterface
    public interface PointTest {
        boolean test(double x, double z);
    }

    /** How far short of its east and south edges {@link #inside} samples a column (a column is [x, x+1) wide). */
    public static final double CORNER_INSET = 1e-9;

    /**
     * The columns of {@code shape} lying wholly in an area: all four corners of the column ([x, x+1) by [z, z+1)) pass
     * {@code test}. For a safe area whose test is a half-plane, a strip or a circle, a column whose corners are all safe is
     * safe all over.
     */
    public static List<int[]> inside(List<int[]> shape, PointTest test) {
        List<int[]> out = new ArrayList<>();
        double lo = 0;
        double hi = 1 - CORNER_INSET;
        for (int[] c : shape) {
            if (test.test(c[0] + lo, c[1] + lo) && test.test(c[0] + hi, c[1] + lo) && test.test(c[0] + lo, c[1] + hi)
                    && test.test(c[0] + hi, c[1] + hi)) {
                out.add(c);
            }
        }
        return out;
    }

    /** The distance from ({@code cx}, {@code cz}) to the farthest corner of column {@code c}. */
    public static double farthestCorner(int[] c, double cx, double cz) {
        double dx = Math.max(Math.abs(c[0] - cx), Math.abs(c[0] + 1 - cx));
        double dz = Math.max(Math.abs(c[1] - cz), Math.abs(c[1] + 1 - cz));
        return Math.hypot(dx, dz);
    }

    /** Every column of the block rectangle from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}), corners in either order. */
    public static List<int[]> square(int x0, int z0, int x1, int z1) {
        List<int[]> out = new ArrayList<>();
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                out.add(new int[]{x, z});
            }
        }
        return out;
    }

    /**
     * The columns of the square area {@code half} blocks either side of ({@code cx}, {@code cz}) (a tile area tested with
     * |dx| and |dz| at most {@code half}): every column whose centre lies inside it, and the column under the centre.
     */
    public static List<int[]> box(double cx, double cz, double half) {
        Set<Long> seen = new LinkedHashSet<>();
        List<int[]> out = new ArrayList<>();
        add(out, seen, column(cx, cz));
        int x0 = (int) Math.floor(cx - half - 1);
        int x1 = (int) Math.floor(cx + half + 1);
        int z0 = (int) Math.floor(cz - half - 1);
        int z1 = (int) Math.floor(cz + half + 1);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (Math.abs(x + 0.5 - cx) <= half + 1e-9 && Math.abs(z + 0.5 - cz) <= half + 1e-9) {
                    add(out, seen, new int[]{x, z});
                }
            }
        }
        return out;
    }

    /**
     * The columns within {@code halfWidth} (+{@link #SLACK}) of the segment from ({@code x0}, {@code z0}) to ({@code x1},
     * {@code z1}), ordered from its start to its end (a sweep marks them in this order). Capped at {@link #MAX_BLOCKS} times 2.
     */
    public static List<int[]> line(double x0, double z0, double x1, double z1, double halfWidth) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double len2 = dx * dx + dz * dz;
        double reach = Math.max(0, halfWidth) + SLACK;
        int bx0 = (int) Math.floor(Math.min(x0, x1) - reach - 1);
        int bx1 = (int) Math.floor(Math.max(x0, x1) + reach + 1);
        int bz0 = (int) Math.floor(Math.min(z0, z1) - reach - 1);
        int bz1 = (int) Math.floor(Math.max(z0, z1) + reach + 1);
        List<double[]> hits = new ArrayList<>();
        for (int x = bx0; x <= bx1; x++) {
            for (int z = bz0; z <= bz1; z++) {
                double px = x + 0.5 - x0;
                double pz = z + 0.5 - z0;
                double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, (px * dx + pz * dz) / len2));
                if (Math.hypot(px - dx * t, pz - dz * t) <= reach) {
                    hits.add(new double[]{x, z, t});
                }
            }
        }
        hits.sort((a, b) -> Double.compare(a[2], b[2]));
        List<int[]> out = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        add(out, seen, column(x0, z0));
        for (double[] h : hits) {
            if (out.size() >= MAX_BLOCKS * 2) {
                break;
            }
            add(out, seen, new int[]{(int) h[0], (int) h[1]});
        }
        return out;
    }

    /** Whether column {@code c} is in {@code shape}. */
    public static boolean contains(List<int[]> shape, int x, int z) {
        for (int[] c : shape) {
            if (c[0] == x && c[1] == z) {
                return true;
            }
        }
        return false;
    }

    private static void add(List<int[]> out, Set<Long> seen, int[] c) {
        if (seen.add(((long) c[0] << 32) ^ (c[1] & 0xffffffffL))) {
            out.add(c);
        }
    }
}
