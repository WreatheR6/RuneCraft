package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Town buildings traced tile by tile from the OSRS map (mejrs layers_osrs, round 31): each building is a
 * footprint mask (row 0 = the northmost tile row, as in {@link TileMask}) with one-block walls that follow
 * chamfers and wings, a floor per storey, a hip roof stepped over the footprint (stairs on the slopes, full
 * blocks on hips and the ridge), and doors / ladders on their OSRS tiles. Heights are relative to the
 * builder's ground: y 0 is the first block above the floor.
 */
final class MapTown {
    static final int H = 4;
    final StructureDef.Builder b;
    final int g;

    MapTown(StructureDef.Builder b) {
        this.b = b;
        this.g = b.ground();
    }

    /** A building over the tiles of {@code rows} whose character is in {@code chars} (null: any but ' ' and '.'). */
    Bld bld(int tx0, int tyTop, String chars, String... rows) {
        return new Bld(this, tx0, tyTop, chars, rows);
    }

    /** Fills every block of the masked tiles between y1 and y2 (relative); a null block clears them. */
    void mask(int tx0, int tyTop, String chars, int y1, int y2, String block, String... rows) {
        boolean[][] in = parse(chars, rows);
        emitRuns(in, bx(tx0), bz(tyTop), g + y1, g + y2, block);
    }

    static boolean[][] parse(String chars, String... rows) {
        int w = 0;
        for (String r : rows) {
            w = Math.max(w, r.length());
        }
        boolean[][] in = new boolean[w * 2][rows.length * 2];
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                char c = rows[j].charAt(i);
                if (chars == null ? (c != ' ' && c != '.') : chars.indexOf(c) >= 0) {
                    in[i * 2][j * 2] = true;
                    in[i * 2 + 1][j * 2] = true;
                    in[i * 2][j * 2 + 1] = true;
                    in[i * 2 + 1][j * 2 + 1] = true;
                }
            }
        }
        return in;
    }

    private void emitRuns(boolean[][] on, int x0, int z0, int y1, int y2, String block) {
        for (int j = 0; j < on[0].length; j++) {
            int run = -1;
            for (int i = 0; i <= on.length; i++) {
                boolean v = i < on.length && on[i][j];
                if (v && run < 0) {
                    run = i;
                } else if (!v && run >= 0) {
                    if (block == null) {
                        b.clear(x0 + run, y1, z0 + j, x0 + i - 1, y2, z0 + j);
                    } else {
                        b.fill(x0 + run, y1, z0 + j, x0 + i - 1, y2, z0 + j, block);
                    }
                    run = -1;
                }
            }
        }
    }

    /** A door in the wall on the given side ('n', 's', 'e', 'w') of tile (tx, ty), at relative height y. */
    void door(int tx, int ty, char side, int y, String wood) {
        int x = bx(tx);
        int z = bz(ty);
        int[][] p = switch (side) {
            case 'n' -> new int[][]{{x, z}, {x + 1, z}};
            case 's' -> new int[][]{{x, z + 1}, {x + 1, z + 1}};
            case 'w' -> new int[][]{{x, z}, {x, z + 1}};
            default -> new int[][]{{x + 1, z}, {x + 1, z + 1}};
        };
        String facing = switch (side) {
            case 'n' -> "south";
            case 's' -> "north";
            case 'w' -> "east";
            default -> "west";
        };
        // hinge=left on the first block for doors facing north / east, mirrored for south / west
        boolean leftFirst = side == 's' || side == 'w';
        b.clear(p[0][0], g + y, p[0][1], p[1][0], g + y + 2, p[1][1]);
        b.set(p[0][0], g + y, p[0][1], "minecraft:" + wood + "_door[facing=" + facing + ",half=lower,hinge="
                + (leftFirst ? "left" : "right") + "]");
        b.set(p[1][0], g + y, p[1][1], "minecraft:" + wood + "_door[facing=" + facing + ",half=lower,hinge="
                + (leftFirst ? "right" : "left") + "]");
    }

    /** An open doorway (no door) in the wall on the given side of tile (tx, ty). */
    void gap(int tx, int ty, char side, int y, int h) {
        int x = bx(tx);
        int z = bz(ty);
        switch (side) {
            case 'n' -> b.clear(x, g + y, z, x + 1, g + y + h - 1, z);
            case 's' -> b.clear(x, g + y, z + 1, x + 1, g + y + h - 1, z + 1);
            case 'w' -> b.clear(x, g + y, z, x, g + y + h - 1, z + 1);
            default -> b.clear(x + 1, g + y, z, x + 1, g + y + h - 1, z + 1);
        }
    }

    /**
     * A ladder at block (x, z) from relative y1 to y2 facing {@code facing}, with a support block behind it
     * and the floors above it opened.
     */
    void ladder(int x, int z, String facing, int y1, int y2, String support) {
        int sx = x;
        int sz = z;
        switch (facing) {
            case "north" -> sz = z + 1;
            case "south" -> sz = z - 1;
            case "east" -> sx = x - 1;
            default -> sx = x + 1;
        }
        b.fill(sx, g + y1, sz, sx, g + y2, sz, support);
        b.clear(x, g + y1, z, x, g + y2 + 1, z);
        b.fill(x, g + y1, z, x, g + y2, z, "minecraft:ladder[facing=" + facing + "]");
    }

    /** A straight staircase climbing toward {@code dir} from block (x, z): n steps, w blocks wide (to the right). */
    void stairs(int x, int z, String dir, int y, int n, int w, String stairs, String fill) {
        int dx = dir.equals("east") ? 1 : dir.equals("west") ? -1 : 0;
        int dz = dir.equals("south") ? 1 : dir.equals("north") ? -1 : 0;
        int wx = dz != 0 ? 1 : 0;
        int wz = dx != 0 ? 1 : 0;
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < w; k++) {
                int px = x + dx * i + wx * k;
                int pz = z + dz * i + wz * k;
                b.clear(px, g + y + i, pz, px, g + y + i + 3, pz);
                if (i > 0) {
                    b.fill(px, g + y, pz, px, g + y + i - 1, pz, fill);
                }
                b.set(px, g + y + i, pz, stairs + "[facing=" + dir + ",half=bottom]");
            }
        }
    }

    /** One traced building. */
    static final class Bld {
        private final MapTown t;
        private final boolean[][] in;
        private final int x0;
        private final int z0;
        private String wall = "minecraft:stone_bricks";
        private String trim;
        private String floor = "minecraft:spruce_planks";
        private String upper = "minecraft:spruce_planks";
        private String roofStairs = "minecraft:dark_oak_stairs";
        private String roofFull = "minecraft:dark_oak_planks";
        private String window = "minecraft:glass_pane";
        private int storeys = 1;
        private int roofCap = 5;
        private boolean flat;
        private String parapet;
        private final List<Runnable> after = new ArrayList<>();

        Bld(MapTown t, int tx0, int tyTop, String chars, String[] rows) {
            this.t = t;
            this.in = parse(chars, rows);
            this.x0 = bx(tx0);
            this.z0 = bz(tyTop);
        }

        Bld wall(String wall) {
            this.wall = wall;
            return this;
        }

        /** Corner posts and the beam under the eaves (timber framing); null for plain walls. */
        Bld trim(String trim) {
            this.trim = trim;
            return this;
        }

        Bld floor(String floor, String upper) {
            this.floor = floor;
            this.upper = upper;
            return this;
        }

        Bld roof(String stairs, String full) {
            this.roofStairs = stairs;
            this.roofFull = full;
            return this;
        }

        Bld window(String window) {
            this.window = window;
            return this;
        }

        Bld storeys(int n) {
            this.storeys = n;
            return this;
        }

        /** At most n roof layers; anything higher is a flat top. */
        Bld roofCap(int n) {
            this.roofCap = n;
            return this;
        }

        /** A flat roof with a parapet (null: none) instead of a hip roof. */
        Bld flat(String parapet) {
            this.flat = true;
            this.parapet = parapet;
            return this;
        }

        Bld door(int tx, int ty, char side, String wood) {
            after.add(() -> t.door(tx, ty, side, 0, wood));
            return this;
        }

        Bld gap(int tx, int ty, char side) {
            after.add(() -> t.gap(tx, ty, side, 0, 3));
            return this;
        }

        /** A ladder on tile (tx, ty) from the ground floor to the top storey; its support fills the rest of the tile. */
        Bld ladder(int tx, int ty, String facing) {
            int x = bx(tx) + (facing.equals("east") ? 1 : 0);
            int z = bz(ty) + (facing.equals("south") ? 1 : 0);
            after.add(() -> t.ladder(x, z, facing, 0, top() - H + 1, wall));
            return this;
        }

        int top() {
            return storeys * H - 1;
        }

        private boolean at(int i, int j) {
            return i >= 0 && j >= 0 && i < in.length && j < in[0].length && in[i][j];
        }

        private boolean edge(int i, int j) {
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (!at(i + di, j + dj)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean[][] select(int mode) {
            boolean[][] o = new boolean[in.length][in[0].length];
            for (int i = 0; i < in.length; i++) {
                for (int j = 0; j < in[0].length; j++) {
                    o[i][j] = in[i][j] && (mode == 0 || (mode == 1) == edge(i, j));
                }
            }
            return o;
        }

        StructureDef.Builder build() {
            StructureDef.Builder b = t.b;
            int g = t.g;
            int top = top();
            t.emitRuns(select(0), x0, z0, g - 1, g - 1, floor);
            t.emitRuns(select(2), x0, z0, g, g + top + 8, null);
            t.emitRuns(select(1), x0, z0, g, g + top, wall);
            for (int s = 1; s < storeys; s++) {
                t.emitRuns(select(2), x0, z0, g + s * H - 1, g + s * H - 1, upper);
            }
            // windows: every fourth block of a straight wall run, one per storey, never at corners
            for (int i = 0; i < in.length; i++) {
                for (int j = 0; j < in[0].length; j++) {
                    if (!in[i][j] || !edge(i, j) || ((i + j) & 3) != 1) {
                        continue;
                    }
                    boolean ns = !at(i, j - 1) || !at(i, j + 1);
                    boolean ew = !at(i - 1, j) || !at(i + 1, j);
                    boolean run = ns && !ew ? edgeAt(i - 1, j) && edgeAt(i + 1, j) && !at(i, j - 1) == !at(i - 1, j - 1)
                            && !at(i, j - 1) == !at(i + 1, j - 1) && !at(i, j + 1) == !at(i - 1, j + 1) && !at(i, j + 1) == !at(i + 1, j + 1)
                            : ew && !ns && edgeAt(i, j - 1) && edgeAt(i, j + 1) && !at(i - 1, j) == !at(i - 1, j - 1)
                            && !at(i - 1, j) == !at(i - 1, j + 1) && !at(i + 1, j) == !at(i + 1, j - 1) && !at(i + 1, j) == !at(i + 1, j + 1);
                    if (run) {
                        for (int s = 0; s < storeys; s++) {
                            b.set(x0 + i, g + s * H + 1, z0 + j, window);
                        }
                    }
                }
            }
            if (trim != null) {
                for (int i = 0; i < in.length; i++) {
                    for (int j = 0; j < in[0].length; j++) {
                        if (in[i][j] && edge(i, j) && corner(i, j)) {
                            b.fill(x0 + i, g, z0 + j, x0 + i, g + top, z0 + j, trim);
                        }
                    }
                }
                for (int s = 1; s <= storeys; s++) {
                    t.emitRuns(select(1), x0, z0, g + s * H - 1, g + s * H - 1, trim);
                }
            }
            for (Runnable r : after) {
                r.run();
            }
            if (flat) {
                t.emitRuns(select(0), x0, z0, g + top + 1, g + top + 1, roofFull);
                if (parapet != null) {
                    t.emitRuns(select(1), x0, z0, g + top + 2, g + top + 2, parapet);
                }
                return b;
            }
            int[][] d = dist();
            for (int i = 0; i < in.length; i++) {
                for (int j = 0; j < in[0].length; j++) {
                    int k = d[i][j];
                    if (k == 0) {
                        continue;
                    }
                    int y = g + top + Math.min(k, roofCap);
                    if (k > roofCap) {
                        b.set(x0 + i, y, z0 + j, roofFull);
                        continue;
                    }
                    String facing = null;
                    int lower = 0;
                    if (dd(d, i, j - 1) < k) {
                        facing = "south";
                        lower++;
                    }
                    if (dd(d, i, j + 1) < k) {
                        facing = "north";
                        lower++;
                    }
                    if (dd(d, i - 1, j) < k) {
                        facing = "east";
                        lower++;
                    }
                    if (dd(d, i + 1, j) < k) {
                        facing = "west";
                        lower++;
                    }
                    boolean peak = dd(d, i, j - 1) <= k && dd(d, i, j + 1) <= k && dd(d, i - 1, j) <= k && dd(d, i + 1, j) <= k
                            && (dd(d, i, j - 1) < k || dd(d, i, j + 1) < k) && (dd(d, i - 1, j) < k || dd(d, i + 1, j) < k);
                    if (lower == 1 && !peak) {
                        b.set(x0 + i, y, z0 + j, roofStairs + "[facing=" + facing + ",half=bottom]");
                    } else {
                        b.set(x0 + i, y, z0 + j, roofFull);
                    }
                }
            }
            return b;
        }

        private boolean edgeAt(int i, int j) {
            return at(i, j) && edge(i, j);
        }

        /** An outside corner (two perpendicular neighbours outside) or an inside one (only a diagonal outside). */
        private boolean corner(int i, int j) {
            boolean n = !at(i, j - 1);
            boolean s = !at(i, j + 1);
            boolean w = !at(i - 1, j);
            boolean e = !at(i + 1, j);
            if ((n || s) && (w || e)) {
                return true;
            }
            return !n && !s && !w && !e;
        }

        private static int dd(int[][] d, int i, int j) {
            return i < 0 || j < 0 || i >= d.length || j >= d[0].length ? 0 : d[i][j];
        }

        /** Chebyshev distance of each inside block to the outside (edge blocks are 1). */
        private int[][] dist() {
            int w = in.length;
            int h = in[0].length;
            int[][] d = new int[w][h];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    d[i][j] = in[i][j] ? Integer.MAX_VALUE : 0;
                }
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        if (d[i][j] == 0) {
                            continue;
                        }
                        int m = Integer.MAX_VALUE;
                        for (int di = -1; di <= 1; di++) {
                            for (int dj = -1; dj <= 1; dj++) {
                                m = Math.min(m, dd(d, i + di, j + dj));
                            }
                        }
                        if (m != Integer.MAX_VALUE && m + 1 < d[i][j]) {
                            d[i][j] = m + 1;
                            changed = true;
                        }
                    }
                }
            }
            return d;
        }
    }
}
