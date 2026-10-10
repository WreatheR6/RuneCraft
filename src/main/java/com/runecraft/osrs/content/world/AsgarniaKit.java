package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Drawing helpers shared by the Asgarnia and Karamja structures. Tile methods take OSRS tile
 * coordinates; block methods take absolute block x/z. All heights are relative to the builder's
 * ground ({@code dy = 0} is the first block above the floor).
 *
 * <p>Doors and beds are given as one half (lower door half, bed foot); the placer adds the other.
 */
final class AsgarniaKit {
    static final int FLAT = 0;
    static final int PITCHED = 1;
    static final int NONE = 2;

    final StructureDef.Builder b;
    final int g;
    /** Wood of the doors hung in {@link #house} doorways (null leaves doorways open). */
    String doorWood = "spruce";

    /** Deferred finishing details (textures, shutters, chimneys...), appended on {@link #build}. */
    final MisthalinDetail d = new MisthalinDetail("asgarnia");

    AsgarniaKit(StructureDef.Builder b) {
        this.b = b;
        this.g = b.ground();
    }

    /** Appends the deferred details and builds the structure. */
    StructureDef build() {
        d.flush(b);
        return b.build();
    }

    // ------------------------------------------------------------------ block strings

    static String mc(String id) {
        return "minecraft:" + id;
    }

    static String rc(String id) {
        return "runecraft:" + id;
    }

    static String leaves(String wood) {
        return mc(wood + "_leaves[persistent=true]");
    }

    static String stair(String stairs, String facing) {
        return stairs + "[facing=" + facing + "]";
    }

    static String stairTop(String stairs, String facing) {
        return stairs + "[facing=" + facing + ",half=top]";
    }

    /** Deterministic per-column noise in [0, 65535]. */
    static int noise(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0xFFFF;
    }

    static String opposite(String d) {
        return switch (d) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    /** Clockwise of a direction (north -> east). */
    static String cw(String d) {
        return switch (d) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            default -> "north";
        };
    }

    static int dxOf(String d) {
        return d.equals("east") ? 1 : d.equals("west") ? -1 : 0;
    }

    static int dzOf(String d) {
        return d.equals("south") ? 1 : d.equals("north") ? -1 : 0;
    }

    // ------------------------------------------------------------------ coordinates

    /** Block rectangle {x1, z1, x2, z2} of an inclusive tile rectangle. */
    static int[] rect(int tx1, int ty1, int tx2, int ty2) {
        return new int[]{bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1};
    }

    // ------------------------------------------------------------------ block primitives (dy relative)

    void s(int x, int dy, int z, String block) {
        b.set(x, g + dy, z, block);
    }

    void f(int x1, int dy1, int z1, int x2, int dy2, int z2, String block) {
        b.fill(x1, g + dy1, z1, x2, g + dy2, z2, block);
    }

    void w(int x1, int dy1, int z1, int x2, int dy2, int z2, String block) {
        b.walls(x1, g + dy1, z1, x2, g + dy2, z2, block);
    }

    void c(int x1, int dy1, int z1, int x2, int dy2, int z2) {
        b.clear(x1, g + dy1, z1, x2, g + dy2, z2);
    }

    void fa(int x1, int dy1, int z1, int x2, int dy2, int z2, String block) {
        b.op(StructureDef.Op.Type.FILL_AIR, x1, g + dy1, z1, x2, g + dy2, z2, block, null);
    }

    // ------------------------------------------------------------------ tile primitives

    void flatten(int tx1, int ty1, int tx2, int ty2) {
        b.tFlatten(tx1, ty1, tx2, ty2, mc("grass_block"));
    }

    void flatten(int tx1, int ty1, int tx2, int ty2, String top) {
        b.tFlatten(tx1, ty1, tx2, ty2, top);
    }

    void floor(int tx1, int ty1, int tx2, int ty2, String block) {
        b.tFloor(tx1, ty1, tx2, ty2, block);
    }

    void fillT(int tx1, int ty1, int tx2, int ty2, int dy1, int dy2, String block) {
        b.tFill(tx1, ty1, tx2, ty2, dy1, dy2, block);
    }

    void clearT(int tx1, int ty1, int tx2, int ty2, int dy1, int dy2) {
        b.tClear(tx1, ty1, tx2, ty2, dy1, dy2);
    }

    void wallsT(int tx1, int ty1, int tx2, int ty2, int dy1, int dy2, String block) {
        b.tWalls(tx1, ty1, tx2, ty2, dy1, dy2, block);
    }

    /** One block on the north-west corner of a tile. */
    void st(int tx, int ty, int dy, String block) {
        b.tSet(tx, ty, dy, block);
    }

    /** All four blocks of a tile. */
    void tile(int tx, int ty, int dy, String block) {
        b.tSetTile(tx, ty, dy, block);
    }

    /** A path (floor) with a checker of two blocks. */
    void path(int tx1, int ty1, int tx2, int ty2, String a, String bb) {
        floor(tx1, ty1, tx2, ty2, a);
        int[] r = rect(tx1, ty1, tx2, ty2);
        for (int x = r[0]; x <= r[2]; x += 3) {
            for (int z = r[1] + (x / 3 & 1); z <= r[3]; z += 3) {
                s(x, -1, z, bb);
            }
        }
    }

    /** A hedge of persistent leaves over a tile rectangle. */
    void hedge(int tx1, int ty1, int tx2, int ty2, int height) {
        fillT(tx1, ty1, tx2, ty2, 0, height - 1, leaves("oak"));
    }

    /** A fence (or wall) outline one block high around a tile rectangle, with optional gate gaps (tile x/y of gaps). */
    void fenceRing(int tx1, int ty1, int tx2, int ty2, String block) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        w(r[0], 0, r[1], r[2], 0, r[3], block);
    }

    // ------------------------------------------------------------------ shapes

    /** Filled disc of radius r (blocks) centred on a block. */
    void disc(int cx, int cz, double r, int dy1, int dy2, String block) {
        int ir = (int) Math.ceil(r);
        for (int dz = -ir; dz <= ir; dz++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, r * r - dz * dz)));
            if (r * r - dz * dz < 0) {
                continue;
            }
            f(cx - half, dy1, cz + dz, cx + half, dy2, cz + dz, block);
        }
    }

    /** Ring (circle outline) of radius r (blocks). */
    void ring(int cx, int cz, double r, int dy1, int dy2, String block) {
        int ir = (int) Math.ceil(r) + 1;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= r + 0.5 && d > r - 0.7) {
                    f(cx + dx, dy1, cz + dz, cx + dx, dy2, cz + dz, block);
                }
            }
        }
    }

    /**
     * Round tower centred on a block: walls, floors every 5 blocks joined by a ladder on the north
     * inner face, arrow slits, a doorway on {@code door} (or null) and a crenellated top. If
     * {@code cone} is non-null a pointed cap of that block is added instead of open battlements.
     */
    void tower(int cx, int cz, int r, int height, String wall, String floor, String door, String cone) {
        d.roundTower(cx, cz, r, g, g + height, 5, wall, cone == null);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (d > r - 0.7) {
                    f(x, -2, z, x, height - 1, z, wall);
                    if (cone == null && (dx + dz) % 2 == 0) {
                        s(x, height, z, wall);
                    }
                } else {
                    s(x, -1, z, floor);
                    c(x, 0, z, x, height - 2, z);
                    for (int y = 4; y < height - 1; y += 5) {
                        s(x, y, z, floor);
                    }
                    s(x, height - 1, z, floor);
                }
            }
        }
        // ladder up the inner face opposite a north door (north face otherwise), through every floor
        boolean south = "north".equals(door);
        int lz = south ? cz + r - 1 : cz - r + 1;
        c(cx, 0, lz, cx, height - 1, lz);
        f(cx, 0, lz, cx, height - 1, lz, mc(south ? "ladder[facing=north]" : "ladder[facing=south]"));
        // arrow slits on the east and west faces and the face without the ladder
        for (int y = 2; y < height - 1; y += 5) {
            s(cx + r, y, cz, mc("glass_pane"));
            s(cx - r, y, cz, mc("glass_pane"));
            s(cx, y, south ? cz - r : cz + r, mc("glass_pane"));
        }
        // lantern on each storey
        for (int y = 3; y < height - 1; y += 5) {
            if (r >= 3) {
                s(cx + 1, y, cz + 1, mc("lantern[hanging=true]"));
            }
        }
        if (door != null) {
            int ox = cx + dxOf(door) * r;
            int oz = cz + dzOf(door) * r;
            c(ox, 0, oz, ox, 2, oz);
            if (dxOf(door) != 0) {
                c(ox, 0, oz - 1, ox, 1, oz + 1);
                c(ox, 0, oz, ox, 2, oz);
            } else {
                c(ox - 1, 0, oz, ox + 1, 1, oz);
            }
        }
        if (cone != null) {
            for (int k = 0; k <= r + 1; k++) {
                double rr = r + 0.6 - k * (r + 0.6) / (r + 1.5);
                if (rr < 0.4) {
                    s(cx, height + k, cz, cone);
                    s(cx, height + k + 1, cz, mc("lightning_rod"));
                    break;
                }
                disc(cx, cz, rr, height + k, height + k, cone);
            }
        }
    }

    /** Pitched (gabled) roof over a block rectangle starting at dy, ridge along the longer axis. */
    void pitched(int x1, int z1, int x2, int z2, int dy, String stairs, String full) {
        boolean alongX = (x2 - x1) >= (z2 - z1);
        int layers = ((alongX ? z2 - z1 : x2 - x1) + 2) / 2 + 1;
        for (int i = 0; i < layers; i++) {
            int y = dy + i;
            if (alongX) {
                int za = z1 - 1 + i;
                int zb = z2 + 1 - i;
                if (za > zb) {
                    break;
                }
                if (za == zb) {
                    f(x1 - 1, y, za, x2 + 1, y, za, full);
                    ridgeCap(x1 - 1, y + 1, za, x2 + 1, za, stairs);
                    break;
                }
                f(x1 - 1, y, za, x2 + 1, y, za, stair(stairs, "south"));
                f(x1 - 1, y, zb, x2 + 1, y, zb, stair(stairs, "north"));
                if (za + 1 == zb) {
                    ridgeCap(x1 - 1, y + 1, za, x2 + 1, zb, stairs);
                }
                if (za + 1 <= zb - 1) {
                    f(x1, y, za + 1, x1, y, zb - 1, full);
                    f(x2, y, za + 1, x2, y, zb - 1, full);
                }
            } else {
                int xa = x1 - 1 + i;
                int xb = x2 + 1 - i;
                if (xa > xb) {
                    break;
                }
                if (xa == xb) {
                    f(xa, y, z1 - 1, xa, y, z2 + 1, full);
                    ridgeCap(xa, y + 1, z1 - 1, xa, z2 + 1, stairs);
                    break;
                }
                f(xa, y, z1 - 1, xa, y, z2 + 1, stair(stairs, "east"));
                f(xb, y, z1 - 1, xb, y, z2 + 1, stair(stairs, "west"));
                if (xa + 1 == xb) {
                    ridgeCap(xa, y + 1, z1 - 1, xb, z2 + 1, stairs);
                }
                if (xa + 1 <= xb - 1) {
                    f(xa + 1, y, z1, xb - 1, y, z1, full);
                    f(xa + 1, y, z2, xb - 1, y, z2, full);
                }
            }
        }
    }

    /** A slab ridge cap (deferred, only into air) along a roof ridge; skipped for non-stair roofs. */
    void ridgeCap(int x1, int dy, int z1, int x2, int z2, String stairs) {
        if (stairs.endsWith("_stairs")) {
            d.air(x1, g + dy, z1, x2, g + dy, z2, stairs.substring(0, stairs.length() - 7) + "_slab");
        }
    }

    /** Hipped / pyramid roof: concentric rings of stairs rising to a point. */
    void hipped(int x1, int z1, int x2, int z2, int dy, String stairs, String full) {
        int i = 0;
        int ax = x1 - 1;
        int az = z1 - 1;
        int bxx = x2 + 1;
        int bzz = z2 + 1;
        while (ax <= bxx && az <= bzz) {
            int y = dy + i;
            if (ax == bxx || az == bzz) {
                f(ax, y, az, bxx, y, bzz, full);
                break;
            }
            f(ax, y, az, bxx, y, az, stair(stairs, "south"));
            f(ax, y, bzz, bxx, y, bzz, stair(stairs, "north"));
            f(ax, y, az + 1, ax, y, bzz - 1, stair(stairs, "east"));
            f(bxx, y, az + 1, bxx, y, bzz - 1, stair(stairs, "west"));
            ax++;
            az++;
            bxx--;
            bzz--;
            i++;
        }
    }

    // ------------------------------------------------------------------ buildings

    /** A building palette. */
    record Style(String wall, String post, String floor, String beam, String roofStairs, String roofFull,
                 String window, boolean timber) {
    }

    /**
     * A rectangular building over a tile rectangle: floor, walls (4 blocks per storey), corner posts
     * (and timber framing), storey bands, windows, an open doorway in the middle of the {@code door}
     * side, a ladder in the back-left corner joining the storeys, hanging lanterns, and a roof.
     * Returns the ground-floor room for furnishing.
     */
    AsgarniaRoom house(Style st, int tx1, int ty1, int tx2, int ty2, int storeys, String door, int roof) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        return houseB(st, r[0], r[1], r[2], r[3], storeys, door, roof);
    }

    AsgarniaRoom houseB(Style st, int x1, int z1, int x2, int z2, int storeys, String door, int roof) {
        int h = storeys * 4;
        f(x1, -3, z1, x2, -1, z2, st.floor);
        w(x1, -3, z1, x2, -1, z2, st.beam);
        c(x1 + 1, 0, z1 + 1, x2 - 1, h - 2, z2 - 1);
        w(x1, 0, z1, x2, h - 1, z2, st.wall);
        int[][] corners = {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}};
        for (int[] p : corners) {
            f(p[0], -1, p[1], p[0], h - 1, p[1], st.post);
        }
        if (st.timber) {
            for (int x = x1 + 4; x < x2 - 1; x += 4) {
                f(x, 0, z1, x, h - 1, z1, st.post);
                f(x, 0, z2, x, h - 1, z2, st.post);
            }
            for (int z = z1 + 4; z < z2 - 1; z += 4) {
                f(x1, 0, z, x1, h - 1, z, st.post);
                f(x2, 0, z, x2, h - 1, z, st.post);
            }
        }
        for (int k = 1; k <= storeys; k++) {
            int y = 4 * k - 1;
            w(x1, y, z1, x2, y, z2, st.beam);
            f(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, st.floor);
        }
        for (int k = 0; k < storeys; k++) {
            int y = 4 * k + 1;
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                f(x, y, z1, x, y + 1, z1, st.window);
                f(x, y, z2, x, y + 1, z2, st.window);
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                f(x1, y, z, x1, y + 1, z, st.window);
                f(x2, y, z, x2, y + 1, z, st.window);
            }
        }
        AsgarniaRoom room = new AsgarniaRoom(this, x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, door);
        // doorway in the middle of the door wall, with torches beside it
        int len = room.len();
        int mid = len / 2;
        boolean wide = len >= 5;
        String outside = door;
        for (int u = wide ? mid - 1 : mid; u <= mid; u++) {
            int dx = room.wx(u, room.depth() - 1) + dxOf(outside);
            int dz = room.wz(u, room.depth() - 1) + dzOf(outside);
            c(dx, 0, dz, dx, 2, dz);
            s(dx, -1, dz, st.beam);
            if (doorWood != null) {
                String hinge = (wide && u == mid) ? "right" : "left";
                s(dx, 0, dz, mc(doorWood + "_door[facing=" + opposite(outside) + ",half=lower,hinge=" + hinge
                        + ",open=false]"));
            }
        }
        int tl = wide ? mid - 2 : mid - 1;
        int tr = mid + 1;
        for (int u : new int[]{tl, tr}) {
            if (u < 0 || u >= len) {
                continue;
            }
            int wx = room.wx(u, room.depth() - 1) + 2 * dxOf(outside);
            int wz = room.wz(u, room.depth() - 1) + 2 * dzOf(outside);
            s(wx, 2, wz, mc("wall_torch[facing=" + outside + "]"));
        }
        // ladders between storeys in the back-left corner
        if (storeys > 1) {
            int lx = room.wx(0, 0);
            int lz = room.wz(0, 0);
            for (int k = 1; k < storeys; k++) {
                c(lx, 4 * k - 1, lz, lx, 4 * k - 1, lz);
            }
            f(lx, 0, lz, lx, 4 * (storeys - 1), lz, mc("ladder[facing=" + door + "]"));
        }
        // lanterns
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        for (int k = 0; k < storeys; k++) {
            s(mx, 4 * k + 2, mz, mc("lantern[hanging=true]"));
        }
        finishHouse(st, x1, z1, x2, z2, storeys, door, roof);
        if (roof == PITCHED) {
            pitched(x1, z1, x2, z2, h, st.roofStairs, st.roofFull);
            String gableLog = MisthalinDetail.isStone(st.wall) ? null : MisthalinDetail.bare(st.post).replace("minecraft:", "minecraft:stripped_")
                    .replace("stripped_stripped_", "stripped_");
            if (gableLog != null && !gableLog.endsWith("_log")) {
                gableLog = "minecraft:stripped_spruce_log";
            }
            d.gables(x1, z1, x2, z2, g + h, (x2 - x1) >= (z2 - z1), st.roofFull, gableLog, mc("glass_pane"));
        } else if (roof == FLAT) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                    if (edge && ((x + z) & 1) == 0) {
                        s(x, h, z, st.wall);
                    }
                }
            }
        }
        return room;
    }

    private void dressWindow(boolean castle, int x, int y, int z, char out, String wall, String wood, String lintel) {
        if (castle) {
            d.castleWindow(x, y, y + 1, z, out, wall, lintel, g);
        } else {
            d.window(x, y, y + 1, z, out, wall, wood, lintel, g);
        }
    }

    /** Wall weathering, plinth, quoins, window dressings and a chimney for {@link #houseB} (deferred). */
    private void finishHouse(Style st, int x1, int z1, int x2, int z2, int storeys, String door, int roof) {
        int h = storeys * 4;
        String wall = st.wall;
        boolean stone = MisthalinDetail.isStone(wall);
        if (stone) {
            d.texture(x1, z1, x2, z2, g + 1, g + h - 1, wall, 8);
        }
        d.plinth(x1, z1, x2, z2, g, wall, MisthalinDetail.plinthFor(wall));
        String log = st.post.contains("_log") ? MisthalinDetail.bare(st.post) : "minecraft:stripped_spruce_log";
        String wood = MisthalinDetail.shutterWood(wall, doorWood != null ? doorWood : "spruce");
        boolean castle = roof != PITCHED && stone && (storeys >= 2 || x2 - x1 >= 14 || z2 - z1 >= 14);
        for (int k = 0; k < storeys; k++) {
            int y = 4 * k + 1;
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                String lx = stone ? MisthalinDetail.quoinFor(wall) : log + "[axis=x]";
                dressWindow(castle, x, g + y, z1, 'n', wall, wood, lx);
                dressWindow(castle, x, g + y, z2, 's', wall, wood, lx);
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                String lz = stone ? MisthalinDetail.quoinFor(wall) : log + "[axis=z]";
                dressWindow(castle, x1, g + y, z, 'w', wall, wood, lz);
                dressWindow(castle, x2, g + y, z, 'e', wall, wood, lz);
            }
        }
        if (roof == PITCHED && x2 - x1 >= 5 && z2 - z1 >= 5 && MisthalinDetail.noise(x1, 1, z1, 5) % 3 != 0) {
            boolean alongX = (x2 - x1) >= (z2 - z1);
            int span = alongX ? z2 - z1 : x2 - x1;
            String stack = stone ? mc("cobblestone") : mc("bricks");
            char ds = door == null ? ' ' : door.charAt(0);
            char[] ends = alongX ? new char[]{'w', 'e'} : new char[]{'n', 's'};
            for (char e : ends) {
                int along = alongX ? z1 + 3 : x1 + 3;
                int mid = alongX ? (z1 + z2) / 2 : (x1 + x2) / 2;
                if (ds == e && Math.abs(along - mid) <= 2) {
                    continue;
                }
                int cx = alongX ? (e == 'w' ? x1 : x2) : along;
                int cz = alongX ? along : (e == 'n' ? z1 : z2);
                d.chimney(cx, cz, g + 1, g + h + span / 2 + 2, stack);
                break;
            }
        }
    }

    // ------------------------------------------------------------------ nature

    /** A broadleaf tree with a round crown (persistent leaves). */
    void tree(int x, int z, String wood, int height) {
        String log = mc(wood + "_log[axis=y]");
        String leaf = leaves(wood);
        disc(x, z, 2.5, height - 3, height - 2, leaf);
        disc(x, z, 1.5, height - 1, height - 1, leaf);
        s(x, height, z, leaf);
        f(x, 0, z, x, height - 1, z, log);
    }

    /** A jungle / palm tree: tall trunk with drooping fronds. */
    void palm(int x, int z, int height) {
        String leaf = leaves("jungle");
        f(x, 0, z, x, height - 1, z, mc("jungle_log[axis=y]"));
        disc(x, z, 1.5, height, height, leaf);
        for (String d : new String[]{"north", "south", "east", "west"}) {
            int ddx = dxOf(d);
            int ddz = dzOf(d);
            s(x + 2 * ddx, height, z + 2 * ddz, leaf);
            s(x + 3 * ddx, height - 1, z + 3 * ddz, leaf);
            s(x + 3 * ddx, height - 2, z + 3 * ddz, leaf);
        }
        s(x, height + 1, z, leaf);
    }

    /** A flower bed of mixed flowers on grass over a tile rectangle. */
    void flowers(int tx1, int ty1, int tx2, int ty2) {
        String[] fl = {"poppy", "dandelion", "blue_orchid", "allium", "azure_bluet", "red_tulip", "oxeye_daisy", "cornflower", "pink_tulip"};
        int[] r = rect(tx1, ty1, tx2, ty2);
        f(r[0], -1, r[1], r[2], -1, r[3], mc("grass_block"));
        int i = 0;
        for (int x = r[0]; x <= r[2]; x++) {
            for (int z = r[1]; z <= r[3]; z++) {
                s(x, 0, z, mc(fl[Math.floorMod(i++ * 7 + x + z * 3, fl.length)]));
            }
        }
    }

    /** A fountain: round basin with a two-tier spout. */
    void fountain(int cx, int cz, int r, String rim, String spout) {
        disc(cx, cz, r, -3, -2, rim);
        disc(cx, cz, r - 1, -1, -1, mc("water"));
        ring(cx, cz, r, -1, 0, rim);
        f(cx, -1, cz, cx, 2, cz, spout);
        disc(cx, cz, 1, 2, 2, rim);
        s(cx, 3, cz, spout);
        s(cx, 4, cz, mc("water"));
    }

    // ------------------------------------------------------------------ water and boats

    /** Digs a harbour basin over a tile rectangle: water up to sea level (dy -2) on a sand bed. */
    void harbour(int tx1, int ty1, int tx2, int ty2) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        c(r[0], -1, r[1], r[2], 14, r[3]);
        f(r[0], -10, r[1], r[2], -8, r[3], mc("dirt"));
        f(r[0], -7, r[1], r[2], -7, r[3], mc("sand"));
        f(r[0], -6, r[1], r[2], -2, r[3], mc("water"));
    }

    /**
     * A wooden pier over a tile rectangle: plank deck at ground level on log piles, rails of fences
     * along the long sides and lanterns on posts.
     */
    void pier(int tx1, int ty1, int tx2, int ty2, String deck, String rail) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        f(r[0], -1, r[1], r[2], -1, r[3], deck);
        boolean alongX = (r[2] - r[0]) >= (r[3] - r[1]);
        for (int x = r[0]; x <= r[2]; x += 3) {
            for (int z : new int[]{r[1], r[3]}) {
                f(x, -9, z, x, -2, z, mc("spruce_log[axis=y]"));
            }
        }
        for (int z = r[1]; z <= r[3]; z += 3) {
            for (int x : new int[]{r[0], r[2]}) {
                f(x, -9, z, x, -2, z, mc("spruce_log[axis=y]"));
            }
        }
        if (alongX) {
            for (int x = r[0]; x <= r[2]; x += 6) {
                s(x, 0, r[1], rail);
                s(x, 1, r[1], mc("lantern"));
                s(x, 0, r[3], rail);
            }
        } else {
            for (int z = r[1]; z <= r[3]; z += 6) {
                s(r[0], 0, z, rail);
                s(r[0], 1, z, mc("lantern"));
                s(r[2], 0, z, rail);
            }
        }
    }

    /**
     * A sailing ship. Hull of {@code hull} planks with a {@code deck} deck at dy -1 (sits in water
     * whose surface is dy -2), rails, a stern cabin, masts with wool sails and a bowsprit.
     *
     * @param x0     block x (axisX) or z of the stern
     * @param c0     block z (axisX) or x of the centre line
     * @param length length in blocks
     * @param hw     half width in blocks (2..4)
     * @param axisX  true if the ship lies along x
     * @param dir    +1 if the bow points toward increasing coordinate, -1 otherwise
     * @param masts  number of masts (1..3)
     * @param sail   sail block, e.g. minecraft:white_wool
     */
    void ship(int x0, int c0, int length, int hw, boolean axisX, int dir, int masts, String hull, String deck, String sail) {
        String fence = mc("dark_oak_fence");
        for (int a = 0; a < length; a++) {
            int fromBow = length - 1 - a;
            int w = hw;
            if (fromBow < 5) {
                w = Math.max(0, (int) Math.round(hw * fromBow / 5.0));
            } else if (a < 2) {
                w = hw - 1;
            }
            // keel and hull layers
            sh(x0, c0, axisX, dir, a, 0, -4, hull);
            for (int cc = -Math.max(0, w - 2); cc <= Math.max(0, w - 2); cc++) {
                sh(x0, c0, axisX, dir, a, cc, -3, hull);
            }
            for (int cc = -Math.max(0, w - 1); cc <= Math.max(0, w - 1); cc++) {
                sh(x0, c0, axisX, dir, a, cc, -2, hull);
            }
            for (int cc = -w; cc <= w; cc++) {
                sh(x0, c0, axisX, dir, a, cc, -1, Math.abs(cc) == w ? hull : deck);
            }
            // gunwale and rail
            sh(x0, c0, axisX, dir, a, -w, 0, hull);
            sh(x0, c0, axisX, dir, a, w, 0, hull);
            if (w > 0) {
                sh(x0, c0, axisX, dir, a, -w, 1, fence);
                sh(x0, c0, axisX, dir, a, w, 1, fence);
            }
        }
        // transom at the stern
        for (int cc = -(hw - 1); cc <= hw - 1; cc++) {
            sh(x0, c0, axisX, dir, 0, cc, 0, hull);
            sh(x0, c0, axisX, dir, 0, cc, 1, fence);
        }
        // stem post and bowsprit
        sh(x0, c0, axisX, dir, length, 0, -1, hull);
        sh(x0, c0, axisX, dir, length, 0, 0, hull);
        sh(x0, c0, axisX, dir, length, 0, 1, hull);
        for (int k = 1; k <= 3; k++) {
            sh(x0, c0, axisX, dir, length + k, 0, 1 + (k + 1) / 2, fence);
        }
        // stern cabin (raised quarterdeck)
        int cab = Math.min(5, length / 4);
        int cw = Math.max(1, hw - 1);
        for (int a = 1; a <= cab; a++) {
            for (int cc = -cw; cc <= cw; cc++) {
                boolean edge = Math.abs(cc) == cw || a == cab || a == 1;
                for (int y = 0; y <= 2; y++) {
                    sh(x0, c0, axisX, dir, a, cc, y, edge ? mc("spruce_planks") : mc("air"));
                }
                sh(x0, c0, axisX, dir, a, cc, 3, deck);
            }
            sh(x0, c0, axisX, dir, a, -hw + 1, 4, fence);
            sh(x0, c0, axisX, dir, a, hw - 1, 4, fence);
        }
        sh(x0, c0, axisX, dir, cab, 0, 0, mc("air"));
        sh(x0, c0, axisX, dir, cab, 0, 1, mc("air"));
        sh(x0, c0, axisX, dir, 2, cw, 1, mc("glass_pane"));
        sh(x0, c0, axisX, dir, 2, -cw, 1, mc("glass_pane"));
        sh(x0, c0, axisX, dir, 2, 0, 2, mc("lantern[hanging=true]"));
        sh(x0, c0, axisX, dir, 1, 0, 4, mc("lantern"));
        sh(x0, c0, axisX, dir, cab - 1, 0, 0, mc("chest"));
        // masts and sails
        for (int m = 0; m < masts; m++) {
            int a = cab + 2 + (length - cab - 6) * (m + 1) / (masts + 1);
            int top = 9 + (m == masts / 2 ? 3 : 0);
            for (int y = 0; y <= top; y++) {
                sh(x0, c0, axisX, dir, a, 0, y, mc("spruce_log[axis=y]"));
            }
            for (int cc = -hw; cc <= hw; cc++) {
                sh(x0, c0, axisX, dir, a, cc, top - 1, fence);
                sh(x0, c0, axisX, dir, a, cc, 3, fence);
            }
            for (int y = 4; y <= top - 2; y++) {
                int sw = hw - (y == top - 2 ? 1 : 0);
                for (int cc = -sw; cc <= sw; cc++) {
                    sh(x0, c0, axisX, dir, a + 1, cc, y, sail);
                }
            }
            sh(x0, c0, axisX, dir, a, 0, top + 1, mc("red_wool"));
            sh(x0, c0, axisX, dir, a + 1, 0, top + 1, mc("red_wool"));
        }
        // a couple of barrels and rope coils on deck
        sh(x0, c0, axisX, dir, length - 6, hw - 1, 0, mc("barrel[facing=up]"));
        sh(x0, c0, axisX, dir, length - 7, hw - 1, 0, mc("barrel[facing=up]"));
        sh(x0, c0, axisX, dir, length - 6, -hw + 1, 0, mc("brown_carpet"));
    }

    private void sh(int x0, int c0, boolean axisX, int dir, int a, int cc, int dy, String block) {
        if (axisX) {
            s(x0 + dir * a, dy, c0 + cc, block);
        } else {
            s(c0 + cc, dy, x0 + dir * a, block);
        }
    }

    /** A small rowing boat (open hull) lying along x or z. */
    void rowboat(int x, int z, boolean axisX) {
        String hull = mc("oak_planks");
        for (int a = 0; a < 5; a++) {
            int w = a == 0 || a == 4 ? 0 : 1;
            for (int cc = -w; cc <= w; cc++) {
                int xx = axisX ? x + a : x + cc;
                int zz = axisX ? z + cc : z + a;
                s(xx, -2, zz, hull);
                if (Math.abs(cc) == w) {
                    s(xx, -1, zz, a == 2 ? mc("oak_slab[type=bottom]") : hull);
                } else {
                    s(xx, -1, zz, mc("oak_slab[type=bottom]"));
                }
            }
        }
    }
}
