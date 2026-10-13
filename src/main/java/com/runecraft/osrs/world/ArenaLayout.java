package com.runecraft.osrs.world;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each boss's arena is built. Arenas sit beside their place's centre (alternating east/west,
 * stepping south), but an arena that would land on a hand-built structure, or on another arena, is
 * pushed outward until it is clear, so arenas never dig into castles or towns. Shared by the world
 * builder, the terrain model and the smoke tests so they all agree.
 */
public final class ArenaLayout {
    /** An arena centre in block coordinates. */
    public record Arena(String bossId, String placeId, int x, int z, int radius) {
    }

    /** Extra blocks kept clear around an arena's radius. */
    private static final int MARGIN = 3;
    /** Surface ops reach at least this high; deeper ops are underground carvings. */
    private static final int SURFACE_Y = 56;
    private static final int CELL = 64;

    private static Map<String, Arena> byBoss;

    private ArenaLayout() {
    }

    /** Arena for a boss, or null if it has none (raid-only, or no place). */
    public static Arena of(String bossId) {
        return all().get(bossId);
    }

    /** Every arena, in boss registration order. */
    public static synchronized Map<String, Arena> all() {
        if (byBoss == null) {
            byBoss = compute();
        }
        return byBoss;
    }

    /** The original layout: index-th arena around a place centre. */
    public static int[] defaultPosition(int cx, int cz, int index, int radius) {
        return new int[]{cx + (index % 2 == 0 ? 1 : -1) * (14 + radius), cz + (index / 2) * (2 * radius + 6)};
    }

    private static Map<String, Arena> compute() {
        Map<Long, List<int[]>> grid = new HashMap<>();
        // Index individual surface ops, not whole-structure boxes: several towns leave a deliberate
        // gap for their arenas (Kourend courtyard, the Colosseum floor), which must still count as clear.
        for (StructureDef s : ContentRegistry.structures()) {
            for (StructureDef.Op op : s.ops()) {
                if (op.y2() < SURFACE_Y && op.type() != StructureDef.Op.Type.FLATTEN) {
                    continue;
                }
                int m = op.margin();
                index(grid, new int[]{op.x1() - m, op.z1() - m, op.x2() + m, op.z2() + m});
            }
        }
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Arena> out = new LinkedHashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            int[] centre = placeCentre(b.place);
            if (centre == null) {
                continue;
            }
            int index = counts.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            int[] pos = defaultPosition(centre[0], centre[1], index, r);
            int[] free = findFree(grid, pos[0], pos[1], r + MARGIN, Integer.signum(pos[0] - centre[0]));
            out.put(b.id, new Arena(b.id, b.place, free[0], free[1], r));
            int e = r + MARGIN;
            index(grid, new int[]{free[0] - e, free[1] - e, free[0] + e, free[1] + e});
        }
        return out;
    }

    private static int[] placeCentre(String placeId) {
        for (PlaceDef p : ContentRegistry.places()) {
            if (p.id().equals(placeId)) {
                return new int[]{p.blockX(), p.blockZ()};
            }
        }
        Landmark l = Landmark.byName(placeId);
        return l == null ? null : new int[]{l.blockX(), l.blockZ()};
    }

    /** Searches outward (away from the place centre first) for a spot whose square is clear. */
    private static int[] findFree(Map<Long, List<int[]>> grid, int x, int z, int half, int awayX) {
        if (clear(grid, x, z, half)) {
            return new int[]{x, z};
        }
        int dirX = awayX == 0 ? 1 : awayX;
        for (int k = 1; k <= 80; k++) {
            int d = 8 * k;
            int[][] candidates = {
                    {x + dirX * d, z}, {x, z + d}, {x, z - d}, {x - dirX * d, z},
                    {x + dirX * d, z + d}, {x + dirX * d, z - d}, {x - dirX * d, z + d}, {x - dirX * d, z - d}};
            for (int[] c : candidates) {
                if (clear(grid, c[0], c[1], half) && openGround(c[0], c[1], half)) {
                    return c;
                }
            }
        }
        return new int[]{x, z};
    }

    /**
     * True if a relocated arena would sit on ordinary ground rather than on a mountain ridge, a ring
     * range, a river or a lake (an arena terrace there would flatten a peak or dam the water).
     */
    static boolean openGround(int x, int z, int half) {
        double tx = x / 2.0 + 3222;
        double ty = -z / 2.0 + 3218;
        double pad = half / 2.0 + 2;
        for (GielinorGeography.Ridge r : GielinorGeography.RIDGES) {
            double[] p = r.xyh();
            for (int i = 0; i + 3 < p.length; i += 3) {
                if (segDist(tx, ty, p[i], p[i + 1], p[i + 3], p[i + 4]) < r.halfWidth() + pad) {
                    return false;
                }
            }
        }
        for (GielinorGeography.Ring r : GielinorGeography.RINGS) {
            if (Math.abs(Math.hypot(tx - r.x(), ty - r.y()) - r.radius()) < r.halfWidth() + pad) {
                return false;
            }
        }
        for (GielinorGeography.River r : GielinorGeography.RIVERS) {
            double[] p = r.xy();
            for (int i = 0; i + 3 < p.length; i += 2) {
                if (segDist(tx, ty, p[i], p[i + 1], p[i + 2], p[i + 3]) < r.width() + pad) {
                    return false;
                }
            }
        }
        for (GielinorGeography.Ellipse e : GielinorGeography.LAKES) {
            double dx = (tx - e.x()) / (e.rx() + pad);
            double dy = (ty - e.y()) / (e.ry() + pad);
            if (dx * dx + dy * dy < 1) {
                return false;
            }
        }
        return Elevation.lakeDistance(tx, ty, pad + 1) < -pad;
    }

    private static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double len = dx * dx + dy * dy;
        double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private static boolean clear(Map<Long, List<int[]>> grid, int x, int z, int half) {
        int x1 = x - half;
        int z1 = z - half;
        int x2 = x + half;
        int z2 = z + half;
        for (int cx = Math.floorDiv(x1, CELL); cx <= Math.floorDiv(x2, CELL); cx++) {
            for (int cz = Math.floorDiv(z1, CELL); cz <= Math.floorDiv(z2, CELL); cz++) {
                for (int[] b : grid.getOrDefault(key(cx, cz), List.of())) {
                    if (b[2] >= x1 && b[0] <= x2 && b[3] >= z1 && b[1] <= z2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void index(Map<Long, List<int[]>> grid, int[] box) {
        for (int cx = Math.floorDiv(box[0], CELL); cx <= Math.floorDiv(box[2], CELL); cx++) {
            for (int cz = Math.floorDiv(box[1], CELL); cz <= Math.floorDiv(box[3], CELL); cz++) {
                grid.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(box);
            }
        }
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
