package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A hand-built structure in Gielinor (a castle, a town, a wall, a temple...) described as an
 * ordered list of primitive block operations in absolute Minecraft block coordinates. The
 * Minecraft side applies the operations chunk by chunk as chunks load, so even a whole city is
 * built without generating extra chunks or stalling the server.
 *
 * <p>Use {@link #builder(String, String)} and the tile helpers: OSRS tile (x, y) covers blocks
 * {@code [toBlockX(x), toBlockX(x)+1]} x {@code [toBlockZ(y), toBlockZ(y)+1]} (one tile = 2x2 blocks).
 * Block states are strings in vanilla command syntax, e.g. {@code "minecraft:oak_stairs[facing=north]"}
 * or RuneCraft blocks like {@code "runecraft:bank_booth"}.
 */
public final class StructureDef {
    /** Default ground level (first air block above the floor) for flattened towns. */
    public static final int GROUND = 64;

    /** A primitive operation. Coordinates are inclusive block coordinates. */
    public record Op(Type type, int x1, int y1, int z1, int x2, int y2, int z2, String block, String extra) {
        public enum Type {
            /** Fill the box with {@code block}. */
            FILL,
            /** Only the vertical faces (walls) of the box. */
            WALLS,
            /** Fill the box with air. */
            CLEAR,
            /** Fill only blocks that are currently air or fluid. */
            FILL_AIR,
            /** Replace only blocks matching {@code extra} (a block id) with {@code block}. */
            REPLACE,
            /**
             * Flatten terrain in the XZ box so the ground's top surface is at y1-1 ({@code block} on top,
             * {@code extra} below, default dirt), clearing everything up to y2.
             */
            FLATTEN
        }

        /** Blocks around a FLATTEN box over which the levelled ground blends back into the terrain. */
        public static final int BLEND = 8;

        public int margin() {
            return type == Type.FLATTEN ? BLEND : 0;
        }

        public boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            int m = margin();
            return x2 + m >= minX && x1 - m <= minX + 15 && z2 + m >= minZ && z1 - m <= minZ + 15;
        }
    }

    private final String id;
    private final String name;
    private final List<Op> ops;
    private final String replacesLandmark;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    private StructureDef(String id, String name, List<Op> ops, String replacesLandmark) {
        this.id = id;
        this.name = name;
        this.ops = Collections.unmodifiableList(ops);
        this.replacesLandmark = replacesLandmark;
        int a = Integer.MAX_VALUE;
        int b = Integer.MAX_VALUE;
        int c = Integer.MIN_VALUE;
        int d = Integer.MIN_VALUE;
        for (Op op : ops) {
            a = Math.min(a, op.x1() - op.margin());
            b = Math.min(b, op.z1() - op.margin());
            c = Math.max(c, op.x2() + op.margin());
            d = Math.max(d, op.z2() + op.margin());
        }
        this.minX = a;
        this.minZ = b;
        this.maxX = c;
        this.maxZ = d;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<Op> ops() {
        return ops;
    }

    /** Landmark enum name (or place id) whose default generated plaza this structure replaces, or null. */
    public String replacesLandmark() {
        return replacesLandmark;
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxZ() {
        return maxZ;
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int cx = chunkX << 4;
        int cz = chunkZ << 4;
        return maxX >= cx && minX <= cx + 15 && maxZ >= cz && minZ <= cz + 15;
    }

    // ---------------- tile helpers ----------------

    /** First block X of OSRS tile x. */
    public static int bx(int osrsX) {
        return GielinorMap.toBlockX(osrsX);
    }

    /** First block Z of OSRS tile y. */
    public static int bz(int osrsY) {
        return GielinorMap.toBlockZ(osrsY);
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    /**
     * Fluent builder. Low-level methods take block coordinates; the {@code t*} methods take OSRS
     * tile coordinates (inclusive tile rectangles, any corner order) and heights in blocks relative to
     * the builder's ground level.
     */
    public static final class Builder {
        private final String id;
        private final String name;
        private final List<Op> ops = new ArrayList<>();
        private String replaces;
        private int ground = GROUND;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /** Replace the default generated plaza of this landmark/place (its NPCs and arenas are kept). */
        public Builder replaces(String landmarkOrPlace) {
            this.replaces = landmarkOrPlace;
            return this;
        }

        /** Ground level (first air block) used by relative-height helpers. Default 64. */
        public Builder ground(int y) {
            this.ground = y;
            return this;
        }

        public int ground() {
            return ground;
        }

        // ---- block-coordinate primitives ----

        public Builder op(Op.Type type, int x1, int y1, int z1, int x2, int y2, int z2, String block, String extra) {
            ops.add(new Op(type, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), block, extra));
            return this;
        }

        public Builder fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
            return op(Op.Type.FILL, x1, y1, z1, x2, y2, z2, block, null);
        }

        public Builder walls(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
            return op(Op.Type.WALLS, x1, y1, z1, x2, y2, z2, block, null);
        }

        public Builder clear(int x1, int y1, int z1, int x2, int y2, int z2) {
            return op(Op.Type.CLEAR, x1, y1, z1, x2, y2, z2, "minecraft:air", null);
        }

        public Builder set(int x, int y, int z, String block) {
            return fill(x, y, z, x, y, z, block);
        }

        public Builder replace(int x1, int y1, int z1, int x2, int y2, int z2, String from, String to) {
            return op(Op.Type.REPLACE, x1, y1, z1, x2, y2, z2, to, from);
        }

        /** Flatten ground in a block XZ rectangle so the surface is {@code top} at y = ground-1. */
        public Builder flatten(int x1, int z1, int x2, int z2, String top) {
            return op(Op.Type.FLATTEN, x1, ground, z1, x2, ground + 24, z2, top, "minecraft:dirt");
        }

        // ---- tile helpers (heights relative to ground) ----

        private int[] rect(int tx1, int ty1, int tx2, int ty2) {
            int x1 = bx(Math.min(tx1, tx2));
            int x2 = bx(Math.max(tx1, tx2)) + 1;
            int z1 = bz(Math.max(ty1, ty2));
            int z2 = bz(Math.min(ty1, ty2)) + 1;
            return new int[]{x1, z1, x2, z2};
        }

        /** Flatten the tile rectangle (grass by default). */
        public Builder tFlatten(int tx1, int ty1, int tx2, int ty2, String top) {
            int[] r = rect(tx1, ty1, tx2, ty2);
            return flatten(r[0], r[1], r[2], r[3], top);
        }

        /** Fill a tile rectangle between relative heights y1..y2 (0 = first block above ground). */
        public Builder tFill(int tx1, int ty1, int tx2, int ty2, int y1, int y2, String block) {
            int[] r = rect(tx1, ty1, tx2, ty2);
            return fill(r[0], ground + y1, r[1], r[2], ground + y2, r[3], block);
        }

        /** Ground-level floor (replaces the top ground block) over a tile rectangle. */
        public Builder tFloor(int tx1, int ty1, int tx2, int ty2, String block) {
            return tFill(tx1, ty1, tx2, ty2, -1, -1, block);
        }

        public Builder tWalls(int tx1, int ty1, int tx2, int ty2, int y1, int y2, String block) {
            int[] r = rect(tx1, ty1, tx2, ty2);
            return walls(r[0], ground + y1, r[1], r[2], ground + y2, r[3], block);
        }

        public Builder tClear(int tx1, int ty1, int tx2, int ty2, int y1, int y2) {
            int[] r = rect(tx1, ty1, tx2, ty2);
            return clear(r[0], ground + y1, r[1], r[2], ground + y2, r[3]);
        }

        /** Places a block at relative height y on the north-west block of tile (tx, ty). */
        public Builder tSet(int tx, int ty, int y, String block) {
            return set(bx(tx), ground + y, bz(ty), block);
        }

        /** Places a block at relative height y on all 4 blocks of a tile. */
        public Builder tSetTile(int tx, int ty, int y, String block) {
            return tFill(tx, ty, tx, ty, y, y, block);
        }

        /**
         * A complete building over a tile rectangle: floor, walls of {@code wallHeight} blocks per
         * storey, intermediate floors, a doorway on the given side ("north"|"south"|"east"|"west")
         * at the middle of that wall, windows, and a flat or pitched roof.
         */
        public Builder tBuilding(int tx1, int ty1, int tx2, int ty2, int storeys, String wall, String floor,
                                 String roof, String door, boolean pitchedRoof) {
            int[] r = rect(tx1, ty1, tx2, ty2);
            int x1 = r[0];
            int z1 = r[1];
            int x2 = r[2];
            int z2 = r[3];
            int h = 4;
            fill(x1, ground - 1, z1, x2, ground - 1, z2, floor);
            clear(x1 + 1, ground, z1 + 1, x2 - 1, ground + storeys * h - 1, z2 - 1);
            walls(x1, ground, z1, x2, ground + storeys * h - 1, z2, wall);
            for (int s = 1; s < storeys; s++) {
                fill(x1 + 1, ground + s * h - 1, z1 + 1, x2 - 1, ground + s * h - 1, z2 - 1, floor);
            }
            // windows every 3 blocks on every storey
            for (int s = 0; s < storeys; s++) {
                int wy = ground + s * h + 1;
                for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                    set(x, wy, z1, "minecraft:glass_pane");
                    set(x, wy, z2, "minecraft:glass_pane");
                }
                for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                    set(x1, wy, z, "minecraft:glass_pane");
                    set(x2, wy, z, "minecraft:glass_pane");
                }
            }
            if (door != null) {
                int mx = (x1 + x2) / 2;
                int mz = (z1 + z2) / 2;
                switch (door) {
                    case "north" -> clear(mx, ground, z1, mx + 1, ground + 2, z1);
                    case "south" -> clear(mx, ground, z2, mx + 1, ground + 2, z2);
                    case "east" -> clear(x2, ground, mz, x2, ground + 2, mz + 1);
                    default -> clear(x1, ground, mz, x1, ground + 2, mz + 1);
                }
            }
            int top = ground + storeys * h;
            if (pitchedRoof) {
                roof(x1, z1, x2, z2, top, roof);
            } else {
                fill(x1, top - 1, z1, x2, top - 1, z2, roof);
                for (int x = x1; x <= x2; x += 2) {
                    set(x, top, z1, wall);
                    set(x, top, z2, wall);
                }
                for (int z = z1; z <= z2; z += 2) {
                    set(x1, top, z, wall);
                    set(x2, top, z, wall);
                }
            }
            return this;
        }

        /**
         * A pitched roof of stairs/slabs over a block rectangle starting at {@code y}, ridge along the
         * longer axis. {@code material} is a base name such as "minecraft:spruce" (uses _stairs/_planks)
         * or "minecraft:stone_brick" / "minecraft:deepslate_tile".
         */
        public Builder roof(int x1, int z1, int x2, int z2, int y, String material) {
            String stairs = material + "_stairs";
            String full = material.endsWith("brick") || material.endsWith("tile") ? material + "s" : material + "_planks";
            boolean alongX = (x2 - x1) >= (z2 - z1);
            int layers = ((alongX ? z2 - z1 : x2 - x1) + 2) / 2;
            for (int i = 0; i < layers; i++) {
                int yy = y + i;
                if (alongX) {
                    int za = z1 - 1 + i;
                    int zb = z2 + 1 - i;
                    if (za > zb) {
                        break;
                    }
                    if (za == zb) {
                        fill(x1 - 1, yy, za, x2 + 1, yy, za, full);
                        break;
                    }
                    fill(x1 - 1, yy, za, x2 + 1, yy, za, stairs + "[facing=south]");
                    fill(x1 - 1, yy, zb, x2 + 1, yy, zb, stairs + "[facing=north]");
                    if (za + 1 <= zb - 1) {
                        fill(x1, yy, za + 1, x1, yy, zb - 1, full);
                        fill(x2, yy, za + 1, x2, yy, zb - 1, full);
                    }
                } else {
                    int xa = x1 - 1 + i;
                    int xb = x2 + 1 - i;
                    if (xa > xb) {
                        break;
                    }
                    if (xa == xb) {
                        fill(xa, yy, z1 - 1, xa, yy, z2 + 1, full);
                        break;
                    }
                    fill(xa, yy, z1 - 1, xa, yy, z2 + 1, stairs + "[facing=east]");
                    fill(xb, yy, z1 - 1, xb, yy, z2 + 1, stairs + "[facing=west]");
                    if (xa + 1 <= xb - 1) {
                        fill(xa + 1, yy, z1, xb - 1, yy, z1, full);
                        fill(xa + 1, yy, z2, xb - 1, yy, z2, full);
                    }
                }
            }
            return this;
        }

        /** A round-ish tower (square with chamfered corners) centred on a tile, with crenellations. */
        public Builder tTower(int tx, int ty, int radiusBlocks, int height, String wall, String floor) {
            int cx = bx(tx) + 1;
            int cz = bz(ty) + 1;
            int r = radiusBlocks;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r + 0.5) {
                        continue;
                    }
                    boolean edge = d > r - 0.7;
                    if (edge) {
                        fill(cx + dx, ground - 1, cz + dz, cx + dx, ground + height - 1, cz + dz, wall);
                        if ((dx + dz) % 2 == 0) {
                            set(cx + dx, ground + height, cz + dz, wall);
                        }
                    } else {
                        set(cx + dx, ground - 1, cz + dz, floor);
                        clear(cx + dx, ground, cz + dz, cx + dx, ground + height - 2, cz + dz);
                        set(cx + dx, ground + height - 1, cz + dz, floor);
                    }
                }
            }
            return this;
        }

        /** A wall with walkway along a tile line (from tile a to tile b, horizontal or vertical). */
        public Builder tWallLine(int tx1, int ty1, int tx2, int ty2, int height, String wall) {
            tFill(tx1, ty1, tx2, ty2, -1, height - 1, wall);
            return this;
        }

        public StructureDef build() {
            if (ops.isEmpty()) {
                throw new IllegalStateException(id + " has no operations");
            }
            return new StructureDef(id, name, new ArrayList<>(ops), replaces);
        }
    }
}
