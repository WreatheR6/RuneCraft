package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Terrain height model of Gielinor, faithful to the Old School RuneScape world map.
 *
 * <p>{@link #height(int, int)} gives the target surface height at a block column: the first air
 * block above the ground is at about {@code height}, so towns (ground 64) sit at height 64 and
 * columns below {@link #SEA_LEVEL} are under water. The model combines
 * <ul>
 *   <li>coastlines and islands ({@link GielinorGeography}) with a shelving sea floor,</li>
 *   <li>rolling hills whose size depends on the kingdom (flat Misthalin farmland, rugged Wilderness,
 *       desert dunes),</li>
 *   <li>mountain ridges, rings and plateaus,</li>
 *   <li>swamps just above/below sea level, rivers and lakes cut down to sea level,</li>
 *   <li>{@link TerrainAnchors}: ground levelled under every hand-built structure and place, with
 *       slopes limited around them, and rock kept above dungeon rooms.</li>
 * </ul>
 * The full model is evaluated on a 4-block grid in lazily built 64x64 tiles and interpolated, so a
 * lookup is cheap; the Minecraft side samples it from a density function.
 */
public final class Elevation {
    /** Water fills every block below this y (the dimension's sea level). */
    public static final int SEA_LEVEL = 62;
    /** Town ground level: the first air block above flattened ground. */
    public static final int GROUND = StructureDef.GROUND;

    /** Blocks over which levelled cores blend back into the clamped terrain. */
    static final double CORE_BLEND = 10;
    /** Anchors further than this (blocks) from a tile can't affect it. */
    static final double ANCHOR_REACH = TerrainAnchors.STRUCTURE_RADIUS + (190 - GROUND) / TerrainAnchors.STRUCTURE_SLOPE;

    private static final int GRID = 4;
    private static final int TILE = 64;
    private static final int N = TILE / GRID + 1;
    private static final ConcurrentHashMap<Long, float[]> TILES = new ConcurrentHashMap<>();
    private static final int MAX_TILES = 8_000;

    private Elevation() {
    }

    // ------------------------------------------------------------------ public API

    /** Target surface height at a block column (interpolated from the cached model grid). */
    public static double height(int blockX, int blockZ) {
        return sampleTile(blockX, blockZ, 0);
    }

    /** The y of the top solid block of a column. */
    public static int surfaceY(int blockX, int blockZ) {
        return (int) Math.ceil(height(blockX, blockZ)) - 1;
    }

    /** True if the column is under water (sea, river or lake). */
    public static boolean isWater(int blockX, int blockZ) {
        return height(blockX, blockZ) <= SEA_LEVEL;
    }

    /** Signed distance in OSRS tiles to the nearest coastline (positive inland, negative at sea). */
    public static double coastDistance(int blockX, int blockZ) {
        return sampleTile(blockX, blockZ, 1);
    }

    /** True for open sea (not rivers, lakes or harbours). */
    public static boolean isOcean(int blockX, int blockZ) {
        return coastDistance(blockX, blockZ) < -3 && isWater(blockX, blockZ);
    }

    /** Snow-capped mountain tops: very high ground anywhere, or high ground in the cold north. */
    public static boolean isSnowyPeak(int blockX, int blockZ) {
        double h = height(blockX, blockZ);
        if (h >= 140) {
            return true;
        }
        if (h < 96) {
            return false;
        }
        Region r = GielinorMap.regionAt(GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ));
        return r == Region.ASGARNIA || r == Region.TROLL_COUNTRY || r == Region.FREMENNIK_PROVINCE
                || r == Region.WILDERNESS || GielinorMap.toOsrsY(blockZ) >= 3850;
    }

    /** The full model at a block position (not cached). */
    public static double rawHeight(double blockX, double blockZ) {
        return evaluate(blockX, blockZ, null)[0];
    }

    /** The model without anchors (natural landscape only), for previews and tests. */
    public static double naturalHeight(double blockX, double blockZ) {
        return natural(osrsX(blockX), osrsY(blockZ))[0];
    }

    /** True if the column lies in (or on the banks of) a river, lake or hand-built water channel. */
    public static boolean isWaterFeature(int blockX, int blockZ) {
        double ox = osrsX(blockX);
        double oy = osrsY(blockZ);
        for (GielinorGeography.River r : GielinorGeography.RIVERS) {
            double[] p = r.xy();
            for (int i = 0; i + 3 < p.length; i += 2) {
                if (segDist(ox, oy, p[i], p[i + 1], p[i + 2], p[i + 3]) <= r.width() / 2 + 6) {
                    return true;
                }
            }
        }
        for (GielinorGeography.Ellipse l : GielinorGeography.LAKES) {
            if (ellipseK(l, ox, oy) <= 1 + 6 / Math.min(l.rx(), l.ry())) {
                return true;
            }
        }
        for (TerrainAnchors.WaterBox w : TerrainAnchors.get().water()) {
            if (blockX >= w.x1() - 4 && blockX <= w.x2() + 4 && blockZ >= w.z1() - 4 && blockZ <= w.z2() + 4) {
                return true;
            }
        }
        return false;
    }

    /** Clears the cached grid (tests). */
    static void clearCache() {
        TILES.clear();
    }

    // ------------------------------------------------------------------ coordinates

    /** Continuous OSRS x of a block position (tile x spans [x, x+1)). */
    static double osrsX(double blockX) {
        return GielinorMap.ORIGIN_X + (blockX + 0.5) / GielinorMap.SCALE;
    }

    /** Continuous OSRS y of a block position (tile y spans [y, y+1)). */
    static double osrsY(double blockZ) {
        return GielinorMap.ORIGIN_Y + 1 - (blockZ + 0.5) / GielinorMap.SCALE;
    }

    // ------------------------------------------------------------------ tile cache

    private static double sampleTile(int blockX, int blockZ, int channel) {
        int tx = Math.floorDiv(blockX, TILE);
        int tz = Math.floorDiv(blockZ, TILE);
        float[] g = tile(tx, tz);
        int lx = blockX - tx * TILE;
        int lz = blockZ - tz * TILE;
        int gx = lx / GRID;
        int gz = lz / GRID;
        double fx = (lx - gx * GRID) / (double) GRID;
        double fz = (lz - gz * GRID) / (double) GRID;
        int base = channel * N * N;
        double a = g[base + gz * N + gx];
        double b = g[base + gz * N + gx + 1];
        double c = g[base + (gz + 1) * N + gx];
        double d = g[base + (gz + 1) * N + gx + 1];
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static float[] tile(int tx, int tz) {
        long key = ((long) tx << 32) | (tz & 0xFFFFFFFFL);
        float[] g = TILES.get(key);
        if (g != null) {
            return g;
        }
        g = buildTile(tx, tz);
        if (TILES.size() > MAX_TILES) {
            TILES.clear();
        }
        float[] prev = TILES.putIfAbsent(key, g);
        return prev != null ? prev : g;
    }

    private static float[] buildTile(int tx, int tz) {
        int x0 = tx * TILE;
        int z0 = tz * TILE;
        List<TerrainAnchors.Anchor> near = new ArrayList<>();
        for (TerrainAnchors.Anchor a : TerrainAnchors.get().anchors()) {
            double reach = a.kind() == TerrainAnchors.Kind.FLOOR ? 40 : ANCHOR_REACH;
            if (a.x2() + reach >= x0 && a.x1() - reach <= x0 + TILE && a.z2() + reach >= z0 && a.z1() - reach <= z0 + TILE) {
                near.add(a);
            }
        }
        List<TerrainAnchors.WaterBox> water = new ArrayList<>();
        for (TerrainAnchors.WaterBox w : TerrainAnchors.get().water()) {
            if (w.x2() + 4 >= x0 && w.x1() - 4 <= x0 + TILE && w.z2() + 4 >= z0 && w.z1() - 4 <= z0 + TILE) {
                water.add(w);
            }
        }
        Context ctx = new Context(near, water);
        float[] g = new float[2 * N * N];
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                double[] v = evaluate(x0 + i * GRID, z0 + j * GRID, ctx);
                g[j * N + i] = (float) v[0];
                g[N * N + j * N + i] = (float) v[1];
            }
        }
        return g;
    }

    /** Anchors and water boxes relevant to one tile. */
    private record Context(List<TerrainAnchors.Anchor> anchors, List<TerrainAnchors.WaterBox> water) {
    }

    // ------------------------------------------------------------------ the model

    /** Returns {height, coastDistance}. */
    private static double[] evaluate(double bx, double bz, Context ctx) {
        if (ctx == null) {
            List<TerrainAnchors.Anchor> near = new ArrayList<>();
            for (TerrainAnchors.Anchor a : TerrainAnchors.get().anchors()) {
                if (a.distance(bx, bz) < ANCHOR_REACH) {
                    near.add(a);
                }
            }
            ctx = new Context(near, TerrainAnchors.get().water());
        }
        double ox = osrsX(bx);
        double oy = osrsY(bz);
        double[] nat = natural(ox, oy);
        double h = nat[0];
        h = applyAnchors(h, bx, bz, ctx.anchors());
        h = carveWater(h, ox, oy, bx, bz, ctx);
        h = applyFloors(h, bx, bz, ctx.anchors());
        return new double[]{h, nat[1]};
    }

    /** Natural landscape at OSRS coordinates: {height, coastDistance}. */
    static double[] natural(double ox, double oy) {
        // Coastline, warped a little so it isn't made of straight segments.
        double wx = fbm(ox / 40, oy / 40, 11, 3) * 5 + noise(ox / 9, oy / 9, 12) * 1.5;
        double wy = fbm(ox / 40, oy / 40, 13, 3) * 5 + noise(ox / 9, oy / 9, 14) * 1.5;
        double sd = landDistance(ox + wx, oy + wy);
        double h;
        if (sd >= 0) {
            h = 62.6 + 2.4 * smooth(sd / 6);
        } else {
            h = 62.6 - 1.6 * Math.min(1, -sd / 2) - 21 * smooth(-sd / 30) + 1.5 * noise(ox / 25, oy / 25, 21);
        }
        if (sd > -2) {
            double inland = smooth(sd / 14);
            double[] mix = regionMix(ox, oy);
            double amp = mix[0];
            double desert = mix[1];
            double n = fbm(ox / 55, oy / 55, 3, 4);
            double hills = amp * (0.1 + 0.7 * n) + 2.5 * fbm(ox / 18, oy / 18, 4, 2);
            double dunes = desert * 6 * sq(1 - Math.abs(noise(ox / 22 + oy / 70, oy / 9, 5)));
            h += inland * (hills * (1 - 0.6 * desert) + dunes);
            if (sd > 2) {
                h = Math.max(h, 63.4 + 0.3 * smooth((sd - 2) / 4));
            }
            // mountains rise out of the hills
            double coastFade = smooth((sd + 2) / 6);
            double m = mountains(ox, oy);
            if (m > 0) {
                double rough = 0.78 + 0.22 * ridged(ox / 16, oy / 16, 7) + 0.08 * noise(ox / 5, oy / 5, 8);
                double mh = GROUND - 1 + m * rough;
                double peak = Math.max(h, mh + 0.4 * (h - (GROUND - 1)));
                h += (peak - h) * coastFade;
            }
            for (GielinorGeography.Mesa me : GielinorGeography.MESAS) {
                double d = Math.hypot(ox - me.x(), oy - me.y());
                double edge = smooth((me.radius() - d + 3 * noise(ox / 7, oy / 7, 9)) / 3);
                if (edge > 0) {
                    h = Math.max(h, h + (me.topY() + noise(ox / 10, oy / 10, 10) - h) * edge);
                }
            }
            // swamps: mud and pools around sea level
            for (GielinorGeography.Ellipse s : GielinorGeography.SWAMPS) {
                double k = ellipseK(s, ox + 6 * noise(ox / 20, oy / 20, 15), oy + 6 * noise(ox / 20, oy / 20, 16));
                double w = (1 - smooth((k - 0.65) / 0.35)) * smooth(sd / 3);
                if (w > 0) {
                    double bog = 62.4 + 1.5 * noise(ox / 6, oy / 6, 17) + 0.6 * noise(ox / 2.5, oy / 2.5, 18);
                    h += (bog - h) * w;
                }
            }
        }
        return new double[]{h, sd};
    }

    /** Signed distance (tiles) to the nearest coast of any landmass. */
    static double landDistance(double x, double y) {
        double best = -45;
        for (GielinorGeography.Polygon p : GielinorGeography.POLYGONS) {
            best = Math.max(best, polygonDistance(p, x, y, -best));
        }
        for (GielinorGeography.Ellipse e : GielinorGeography.ISLANDS) {
            double k = ellipseK(e, x, y);
            best = Math.max(best, (1 - k) * Math.min(e.rx(), e.ry()));
        }
        return Math.min(best, 45);
    }

    private static final java.util.Map<GielinorGeography.Polygon, double[]> BOUNDS = new java.util.IdentityHashMap<>();

    static {
        for (GielinorGeography.Polygon p : GielinorGeography.POLYGONS) {
            double[] xy = p.xy();
            double a = Double.MAX_VALUE, b = Double.MAX_VALUE, c = -Double.MAX_VALUE, d = -Double.MAX_VALUE;
            for (int i = 0; i < xy.length; i += 2) {
                a = Math.min(a, xy[i]);
                b = Math.min(b, xy[i + 1]);
                c = Math.max(c, xy[i]);
                d = Math.max(d, xy[i + 1]);
            }
            BOUNDS.put(p, new double[]{a, b, c, d});
        }
    }

    private static double polygonDistance(GielinorGeography.Polygon p, double x, double y, double far) {
        double[] bb = BOUNDS.get(p);
        double bdx = Math.max(0, Math.max(bb[0] - x, x - bb[2]));
        double bdy = Math.max(0, Math.max(bb[1] - y, y - bb[3]));
        double bd = Math.hypot(bdx, bdy);
        if (bd > far) {
            return -bd;
        }
        double[] xy = p.xy();
        int n = xy.length / 2;
        boolean inside = false;
        double best = Double.MAX_VALUE;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = xy[2 * i];
            double yi = xy[2 * i + 1];
            double xj = xy[2 * j];
            double yj = xy[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
            best = Math.min(best, segDist2(x, y, xi, yi, xj, yj));
        }
        double d = Math.sqrt(best);
        return inside ? d : -d;
    }

    /** Height above ground of the mountains at a point (0 = none). */
    static double mountains(double ox, double oy) {
        double best = 0;
        for (GielinorGeography.Ridge r : GielinorGeography.RIDGES) {
            double[] p = r.xyh();
            double w = r.halfWidth() * RIDGE_WIDTH;
            for (int i = 0; i + 5 < p.length; i += 3) {
                double ax = p[i], ay = p[i + 1], bxx = p[i + 3], byy = p[i + 4];
                if (ox < Math.min(ax, bxx) - w || ox > Math.max(ax, bxx) + w || oy < Math.min(ay, byy) - w || oy > Math.max(ay, byy) + w) {
                    continue;
                }
                double t = segT(ox, oy, ax, ay, bxx, byy);
                double px = ax + (bxx - ax) * t;
                double py = ay + (byy - ay) * t;
                double d = Math.hypot(ox - px, oy - py);
                // wobble the flanks so ridges aren't perfect tubes
                double ww = w * (0.85 + 0.25 * noise(ox / 18, oy / 18, 31));
                if (d >= ww) {
                    continue;
                }
                double peak = p[i + 2] + (p[i + 5] - p[i + 2]) * t;
                best = Math.max(best, (peak - GROUND) * profile(d / ww));
            }
        }
        for (GielinorGeography.Ring r : GielinorGeography.RINGS) {
            double ang = Math.atan2(oy - r.y(), ox - r.x());
            double radius = r.radius() * (1 + 0.18 * noise(Math.cos(ang) * 1.6 + 7, Math.sin(ang) * 1.6, 34));
            double d = Math.abs(Math.hypot(ox - r.x(), oy - r.y()) - radius);
            double ww = r.halfWidth() * (0.85 + 0.25 * noise(ox / 18, oy / 18, 32));
            if (d < ww) {
                double peak = r.peakY() - 25 * (0.5 + 0.5 * noise(ox / 30, oy / 30, 33));
                best = Math.max(best, (peak - GROUND) * profile(d / ww));
            }
        }
        return best;
    }

    /** Scale applied to every ridge's half-width. */
    static final double RIDGE_WIDTH = 1.4;

    /** Mountain cross-section: 1 on the crest, 0 at the foot, concave flanks. */
    private static double profile(double t) {
        double s = 1 - smooth(t);
        return Math.pow(s, 1.25);
    }

    /** Blended kingdom character at a point: {hill amplitude, desert weight}. */
    private static double[] regionMix(double ox, double oy) {
        double amp = 0;
        double desert = 0;
        double[][] offs = {{0, 0}, {16, 0}, {-16, 0}, {0, 16}, {0, -16}};
        for (double[] o : offs) {
            Region r = GielinorMap.regionAt(ox + o[0], oy + o[1]);
            amp += hillAmplitude(r);
            desert += r == Region.KHARIDIAN_DESERT ? 1 : 0;
        }
        return new double[]{amp / offs.length, desert / offs.length};
    }

    /** Typical height (blocks) of the rolling hills in each kingdom. */
    static double hillAmplitude(Region r) {
        return switch (r) {
            case MISTHALIN -> 10;
            case LUMBRIDGE_SWAMP -> 1.5;
            case ASGARNIA -> 13;
            case WILDERNESS -> 18;
            case KHARIDIAN_DESERT -> 4;
            case MORYTANIA -> 9;
            case KANDARIN -> 13;
            case FELDIP_HILLS -> 18;
            case FREMENNIK_PROVINCE -> 18;
            case TROLL_COUNTRY -> 26;
            case KARAMJA -> 14;
            case TIRANNWN -> 14;
            case UNCHARTED_ISLES -> 13;
        };
    }

    // ------------------------------------------------------------------ anchors and water

    /**
     * Levels and clamps the terrain around anchors: first places/arenas, then structures, so a
     * structure's ground always wins where the two disagree.
     */
    private static double applyAnchors(double h, double bx, double bz, List<TerrainAnchors.Anchor> anchors) {
        h = applyAnchorPass(h, bx, bz, anchors, true);
        return applyAnchorPass(h, bx, bz, anchors, false);
    }

    private static double applyAnchorPass(double h, double bx, double bz, List<TerrainAnchors.Anchor> anchors, boolean places) {
        double upper = Double.MAX_VALUE;
        double lower = -Double.MAX_VALUE;
        double coreW = 0;
        double coreT = GROUND;
        for (TerrainAnchors.Anchor a : anchors) {
            if (a.kind() == TerrainAnchors.Kind.FLOOR || a.source().contains(":") != places) {
                continue;
            }
            double d = a.distance(bx, bz);
            if (a.kind() == TerrainAnchors.Kind.CORE) {
                double w = 1 - smooth(d / CORE_BLEND);
                if (w > coreW) {
                    coreW = w;
                    coreT = a.target();
                }
            }
            double slack = TerrainAnchors.BAND + a.slope() * Math.max(0, d - a.radius());
            upper = Math.min(upper, a.target() + slack);
            lower = Math.max(lower, a.target() - slack);
        }
        if (upper < Double.MAX_VALUE) {
            h = smin(h, upper, 4);
        }
        if (lower > -Double.MAX_VALUE) {
            h = smax(h, lower, 4);
        }
        return h + (coreT - h) * coreW;
    }

    private static double carveWater(double h, double ox, double oy, double bx, double bz, Context ctx) {
        // Natural rivers and lakes stop at the edge of levelled structure ground: the town fills it
        // in and builds its own channels (which are carved below as water boxes).
        boolean inCore = false;
        for (TerrainAnchors.Anchor a : ctx.anchors()) {
            if (a.kind() == TerrainAnchors.Kind.CORE && !a.source().contains(":") && a.distance(bx, bz) == 0) {
                inCore = true;
                break;
            }
        }
        if (!inCore) {
            h = carveNatural(h, ox, oy);
        }
        for (TerrainAnchors.WaterBox w : ctx.water()) {
            double dx = Math.max(0, Math.max(w.x1() - bx, bx - w.x2()));
            double dz = Math.max(0, Math.max(w.z1() - bz, bz - w.z2()));
            double d = Math.max(dx, dz);
            if (d <= 0) {
                h = Math.min(h, w.bedTop());
            } else if (d < 3) {
                h = Math.min(h, w.bedTop() + (h - w.bedTop()) * smooth(d / 3));
            }
        }
        return h;
    }

    private static double carveNatural(double h, double ox, double oy) {
        for (GielinorGeography.River r : GielinorGeography.RIVERS) {
            double half = r.width() / 2;
            double[] p = r.xy();
            double d = Double.MAX_VALUE;
            for (int i = 0; i + 3 < p.length; i += 2) {
                if (ox < Math.min(p[i], p[i + 2]) - 30 || ox > Math.max(p[i], p[i + 2]) + 30
                        || oy < Math.min(p[i + 1], p[i + 3]) - 30 || oy > Math.max(p[i + 1], p[i + 3]) + 30) {
                    continue;
                }
                d = Math.min(d, segDist(ox, oy, p[i], p[i + 1], p[i + 2], p[i + 3]));
            }
            h = carve(h, d, half);
        }
        for (GielinorGeography.Ellipse l : GielinorGeography.LAKES) {
            double k = ellipseK(l, ox, oy);
            double r = Math.min(l.rx(), l.ry());
            h = carve(h, Math.max(0, (k - 1) * r + r), r);
        }
        return h;
    }

    /** Cuts a channel of half-width {@code half} (tiles) at distance {@code d}, with banks sloping back up. */
    private static double carve(double h, double d, double half) {
        if (d >= half + 40) {
            return h;
        }
        if (d <= half) {
            double t = d / half;
            return Math.min(h, 57.5 + 3.5 * t * t);
        }
        double bank = 2.5 + Math.max(0, h - 63) * 0.7;
        double t = (d - half) / bank;
        if (t >= 1) {
            return h;
        }
        return Math.min(h, 61 + (h - 61) * smooth(t));
    }

    private static double applyFloors(double h, double bx, double bz, List<TerrainAnchors.Anchor> anchors) {
        for (TerrainAnchors.Anchor a : anchors) {
            if (a.kind() != TerrainAnchors.Kind.FLOOR) {
                continue;
            }
            double d = a.distance(bx, bz);
            double need = a.target() - Math.max(0, d - 2);
            if (need > h) {
                h = need;
            }
        }
        return h;
    }

    // ------------------------------------------------------------------ math

    static double ellipseK(GielinorGeography.Ellipse e, double x, double y) {
        double dx = (x - e.x()) / e.rx();
        double dy = (y - e.y()) / e.ry();
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double segT(double px, double py, double ax, double ay, double bx, double by) {
        double vx = bx - ax;
        double vy = by - ay;
        double len = vx * vx + vy * vy;
        if (len == 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, ((px - ax) * vx + (py - ay) * vy) / len));
    }

    static double segDist2(double px, double py, double ax, double ay, double bx, double by) {
        double t = segT(px, py, ax, ay, bx, by);
        double dx = px - (ax + (bx - ax) * t);
        double dy = py - (ay + (by - ay) * t);
        return dx * dx + dy * dy;
    }

    static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        return Math.sqrt(segDist2(px, py, ax, ay, bx, by));
    }

    static double smooth(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return t * t * (3 - 2 * t);
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Smooth minimum of a and b with blend width k. */
    static double smin(double a, double b, double k) {
        double h = Math.max(0, Math.min(1, 0.5 + 0.5 * (b - a) / k));
        return b + (a - b) * h - k * h * (1 - h);
    }

    static double smax(double a, double b, double k) {
        return -smin(-a, -b, k);
    }

    /** 2D gradient noise in roughly [-1, 1]. */
    static double noise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double u = fade(fx);
        double v = fade(fy);
        double n00 = grad(hash(x0, y0, seed), fx, fy);
        double n10 = grad(hash(x0 + 1, y0, seed), fx - 1, fy);
        double n01 = grad(hash(x0, y0 + 1, seed), fx, fy - 1);
        double n11 = grad(hash(x0 + 1, y0 + 1, seed), fx - 1, fy - 1);
        double a = n00 + (n10 - n00) * u;
        double b = n01 + (n11 - n01) * u;
        return (a + (b - a) * v) * 1.41;
    }

    static double fbm(double x, double y, int seed, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise(x, y, seed + i * 101);
            norm += amp;
            amp *= 0.5;
            x *= 2.03;
            y *= 2.03;
        }
        return sum / norm;
    }

    /** Ridged noise in [0, 1] (sharp crests). */
    static double ridged(double x, double y, int seed) {
        double a = 1 - Math.abs(noise(x, y, seed));
        double b = 1 - Math.abs(noise(x * 2.1, y * 2.1, seed + 7));
        return Math.max(0, Math.min(1, 0.65 * a * a + 0.35 * b * b));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static int hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static double grad(int h, double x, double y) {
        return switch (h & 7) {
            case 0 -> x + y;
            case 1 -> x - y;
            case 2 -> -x + y;
            case 3 -> -x - y;
            case 4 -> x;
            case 5 -> -x;
            case 6 -> y;
            default -> -y;
        } * 0.7071;
    }
}
