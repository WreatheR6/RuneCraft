package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A walking route over block columns (round 44, the self-test's real-player walks): A* over the columns a player can stand
 * in, eight ways (a diagonal step only past two open sides, no corner cutting), one block up or down at a time, as a player
 * walks round a pillar or a pile of debris to a spot instead of being teleported there. The columns are the caller's
 * ({@link Floor}): the Minecraft side looks at the blocks, a test at a grid.
 */
public final class GridPath {
    private GridPath() {
    }

    /** No floor to stand on (a wall, a hole, unloaded). */
    public static final int NONE = Integer.MIN_VALUE;

    /** The feet height a player standing in column ({@code x}, {@code z}) would have near {@code nearY}, or {@link #NONE}. */
    public interface Floor {
        int feet(int x, int z, int nearY);
    }

    private record Node(int x, int z, int y) {
    }

    private record Open(Node node, double f) {
    }

    /**
     * The columns from ({@code sx}, {@code sz}) at feet {@code sy} to ({@code gx}, {@code gz}), each {x, y, z}, start excluded
     * and goal included; an empty list when already there, null when there is no way within {@code maxRadius} columns of the
     * start or {@code maxNodes} visits.
     */
    public static List<int[]> find(int sx, int sz, int sy, int gx, int gz, int maxRadius, int maxNodes, Floor floor) {
        return find(sx, sz, sy, gx, gz, maxRadius, maxNodes, floor, false);
    }

    /**
     * As {@link #find}, but when the goal can't be reached (a wall, a hole, past the search) the route to the column nearest the
     * goal that can be: a player walking to a spot off the arena's floor stops at its edge instead of walking off it. An empty
     * list when the start is that nearest column; never null.
     */
    public static List<int[]> findNearest(int sx, int sz, int sy, int gx, int gz, int maxRadius, int maxNodes, Floor floor) {
        return find(sx, sz, sy, gx, gz, maxRadius, maxNodes, floor, true);
    }

    private static List<int[]> find(int sx, int sz, int sy, int gx, int gz, int maxRadius, int maxNodes, Floor floor, boolean nearest) {
        if (sx == gx && sz == gz) {
            return new ArrayList<>();
        }
        Node start = new Node(sx, sz, sy);
        Map<Long, Double> best = new HashMap<>();
        Map<Long, Node> from = new HashMap<>();
        PriorityQueue<Open> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        best.put(key(sx, sz), 0.0);
        open.add(new Open(start, heuristic(sx, sz, gx, gz)));
        Node closest = start;
        double closestH = heuristic(sx, sz, gx, gz);
        int visits = 0;
        while (!open.isEmpty() && visits++ < maxNodes) {
            Open o = open.poll();
            Node n = o.node;
            double g = best.get(key(n.x, n.z));
            if (o.f - heuristic(n.x, n.z, gx, gz) > g + 1e-9) {
                continue; // a stale entry
            }
            if (n.x == gx && n.z == gz) {
                return pathTo(n, start, from);
            }
            double h = heuristic(n.x, n.z, gx, gz);
            if (h < closestH - 1e-9) {
                closest = n;
                closestH = h;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    int x = n.x + dx;
                    int z = n.z + dz;
                    if (Math.abs(x - sx) > maxRadius || Math.abs(z - sz) > maxRadius) {
                        continue;
                    }
                    int y = step(n, x, z, floor);
                    if (y == NONE) {
                        continue;
                    }
                    if (dx != 0 && dz != 0 && (step(n, n.x + dx, n.z, floor) == NONE || step(n, n.x, n.z + dz, floor) == NONE)) {
                        continue; // no cutting a corner past a wall
                    }
                    double cost = g + (dx != 0 && dz != 0 ? Math.sqrt(2) : 1.0) + (y != n.y ? 0.5 : 0.0);
                    long k = key(x, z);
                    Double had = best.get(k);
                    if (had == null || cost < had - 1e-9) {
                        best.put(k, cost);
                        Node next = new Node(x, z, y);
                        from.put(k, n);
                        open.add(new Open(next, cost + heuristic(x, z, gx, gz)));
                    }
                }
            }
        }
        return nearest ? pathTo(closest, start, from) : null;
    }

    private static List<int[]> pathTo(Node n, Node start, Map<Long, Node> from) {
        List<int[]> path = new ArrayList<>();
        for (Node at = n; at != null && at != start; at = from.get(key(at.x, at.z))) {
            path.add(new int[]{at.x, at.y, at.z});
        }
        Collections.reverse(path);
        return path;
    }

    /** The feet height stepping from {@code n} into column ({@code x}, {@code z}): at most a block up or down, else NONE. */
    private static int step(Node n, int x, int z, Floor floor) {
        int y = floor.feet(x, z, n.y);
        return y == NONE || Math.abs(y - n.y) > 1 ? NONE : y;
    }

    private static double heuristic(int x, int z, int gx, int gz) {
        int dx = Math.abs(x - gx);
        int dz = Math.abs(z - gz);
        return Math.max(dx, dz) + (Math.sqrt(2) - 1) * Math.min(dx, dz);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * Drops the corners of a route that a straight walk can cut ({@code start} {x, y, z} first): a point is kept only where the
     * straight line from the last kept point to the next one would leave the standable columns or change height.
     */
    public static List<int[]> smooth(int[] start, List<int[]> path, Floor floor) {
        if (path == null || path.size() < 2) {
            return path;
        }
        List<int[]> out = new ArrayList<>();
        int[] anchor = start;
        for (int i = 0; i < path.size(); i++) {
            int[] next = i + 1 < path.size() ? path.get(i + 1) : null;
            if (next == null || !straight(anchor, next, floor)) {
                out.add(path.get(i));
                anchor = path.get(i);
            }
        }
        return out;
    }

    /** Whether every column on the line from {@code a} to {@code b} (cell centres, a body's width either side) is level ground. */
    static boolean straight(int[] a, int[] b, Floor floor) {
        if (a[1] != b[1]) {
            return false;
        }
        double ax = a[0] + 0.5;
        double az = a[2] + 0.5;
        double bx = b[0] + 0.5;
        double bz = b[2] + 0.5;
        double len = Math.hypot(bx - ax, bz - az);
        int samples = (int) Math.ceil(len / 0.25);
        double sideX = len < 1e-9 ? 0 : -(bz - az) / len * 0.35;
        double sideZ = len < 1e-9 ? 0 : (bx - ax) / len * 0.35;
        for (int i = 0; i <= samples; i++) {
            double t = samples == 0 ? 0 : i / (double) samples;
            double x = ax + (bx - ax) * t;
            double z = az + (bz - az) * t;
            for (int s = -1; s <= 1; s++) {
                int cx = (int) Math.floor(x + sideX * s);
                int cz = (int) Math.floor(z + sideZ * s);
                if (floor.feet(cx, cz, a[1]) != a[1]) {
                    return false;
                }
            }
        }
        return true;
    }
}
