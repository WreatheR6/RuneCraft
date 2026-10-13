package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for one underground dungeon. Rooms, halls, stairs and discs are collected as air boxes
 * (absolute block coordinates) and {@link #carve()} emits them in three passes - a solid shell
 * around every box first, then every box is cleared, then the floors - so overlapping rooms join up
 * and the dungeon is sealed off from natural caves, aquifers and lava. Details (pillars, pools,
 * rocks, chests, ladders) are added after carving.
 *
 * <p>Everything sits below {@link Dungeons#DEEP_TOP} except the entrances: a small hut on open
 * ground with a ladder shaft ({@link #entrance}) or a ladder continuing down through the floor of an
 * entrance pit another module already built ({@link #link}).
 */
final class Cave {
    /** Surface floor block height (towns are flattened so their first air block is y = 64). */
    static final int SURFACE = StructureDef.GROUND - 1;

    final String id;
    final String name;
    final StructureDef.Builder b;
    final String wall;
    final String floor;
    final String light;
    private final List<int[]> areas = new ArrayList<>();
    private final List<String> areaFloors = new ArrayList<>();
    private final List<int[]> stairBlocks = new ArrayList<>();
    private final List<String> stairStates = new ArrayList<>();
    final List<Dungeons.Entrance> entrances = new ArrayList<>();
    final List<Dungeons.Camp> camps = new ArrayList<>();
    final List<com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds> shortcuts = new ArrayList<>();
    private boolean carved;

    Cave(String id, String name, String wall, String floor, String light) {
        this.id = id;
        this.name = name;
        this.b = StructureDef.builder(id, name);
        this.wall = wall;
        this.floor = floor;
        this.light = light;
    }

    static String mc(String id) {
        return "minecraft:" + id;
    }

    static String rc(String id) {
        return "runecraft:" + id;
    }

    // ------------------------------------------------------------------ areas

    private void area(int x1, int y1, int z1, int x2, int y2, int z2, String floorBlock) {
        if (carved) {
            throw new IllegalStateException(id + ": area added after carve()");
        }
        areas.add(new int[]{Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2),
                Math.max(z1, z2)});
        areaFloors.add(floorBlock);
    }

    /** A room: air over x1..x2, z1..z2 from f+1 to f+h, floor block at y = f. */
    Cave room(int x1, int z1, int x2, int z2, int f, int h) {
        return room(x1, z1, x2, z2, f, h, floor);
    }

    Cave room(int x1, int z1, int x2, int z2, int f, int h, String floorBlock) {
        area(x1, f + 1, z1, x2, f + h, z2, floorBlock);
        return this;
    }

    /** A room given by its centre and half sizes. */
    Cave roomC(int cx, int cz, int hx, int hz, int f, int h) {
        return room(cx - hx, cz - hz, cx + hx, cz + hz, f, h);
    }

    /** An L-shaped corridor of width w: along x from (xa, za) to (xb, za), then along z to (xb, zb). */
    Cave hall(int xa, int za, int xb, int zb, int f, int w, int h) {
        int lo = (w - 1) / 2;
        int hi = w / 2;
        if (xa != xb) {
            area(xa, f + 1, za - lo, xb, f + h, za + hi, floor);
        }
        if (za != zb) {
            area(xb - lo, f + 1, za, xb + hi, f + h, zb, floor);
        }
        if (xa == xb && za == zb) {
            area(xa - lo, f + 1, za - lo, xa + hi, f + h, za + hi, floor);
        }
        return this;
    }

    /** An L-shaped corridor going along z first, then along x. */
    Cave hallZ(int xa, int za, int xb, int zb, int f, int w, int h) {
        int lo = (w - 1) / 2;
        int hi = w / 2;
        if (za != zb) {
            area(xa - lo, f + 1, za, xa + hi, f + h, zb, floor);
        }
        if (xa != xb) {
            area(xa, f + 1, zb - lo, xb, f + h, zb + hi, floor);
        }
        return this;
    }

    /**
     * A straight flight of stairs of width w starting at the landing column (x, z) on floor
     * {@code fromF} and descending in direction {@code dir} ('n', 's', 'e', 'w') to floor {@code toF}
     * (if toF is higher the flight climbs instead). The flight is |fromF - toF| + 2 columns long: the
     * two landings and one stair block per block of height.
     */
    Cave stairs(int x, int z, char dir, int fromF, int toF, int w, int h) {
        int dx = dir == 'e' ? 1 : dir == 'w' ? -1 : 0;
        int dz = dir == 's' ? 1 : dir == 'n' ? -1 : 0;
        int n = Math.abs(fromF - toF);
        if (toF > fromF) {
            // Build it as a descent from the far (top) end.
            return stairs(x + dx * (n + 1), z + dz * (n + 1), opposite(dir), toF, fromF, w, h);
        }
        int lo = (w - 1) / 2;
        int hi = w / 2;
        String facing = switch (opposite(dir)) {
            case 'n' -> "north";
            case 's' -> "south";
            case 'e' -> "east";
            default -> "west";
        };
        for (int i = 0; i <= n + 1; i++) {
            int cx = x + dx * i;
            int cz = z + dz * i;
            int f = i == 0 ? fromF : i == n + 1 ? toF : fromF - i + 1;
            int ax1 = dx == 0 ? cx - lo : cx;
            int ax2 = dx == 0 ? cx + hi : cx;
            int az1 = dz == 0 ? cz - lo : cz;
            int az2 = dz == 0 ? cz + hi : cz;
            area(ax1, f + 1, az1, ax2, f + h, az2, floor);
            if (i > 0 && i <= n) {
                stairBlocks.add(new int[]{ax1, f, az1, ax2, f, az2});
                stairStates.add(stairMaterial() + "[facing=" + facing + "]");
            }
        }
        return this;
    }

    private static char opposite(char d) {
        return switch (d) {
            case 'n' -> 's';
            case 's' -> 'n';
            case 'e' -> 'w';
            default -> 'e';
        };
    }

    private String stairMaterial() {
        String base = floor.replace("minecraft:", "");
        return switch (base) {
            case "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks" -> mc("stone_brick_stairs");
            case "deepslate_bricks", "polished_deepslate", "deepslate_tiles", "cobbled_deepslate" -> mc("cobbled_deepslate_stairs");
            case "sandstone", "smooth_sandstone", "cut_sandstone" -> mc("sandstone_stairs");
            case "blackstone", "polished_blackstone", "polished_blackstone_bricks", "basalt" -> mc("blackstone_stairs");
            case "packed_ice", "snow_block", "blue_ice" -> mc("stone_brick_stairs");
            case "end_stone_bricks" -> mc("end_stone_brick_stairs");
            case "mud_bricks", "packed_mud" -> mc("mud_brick_stairs");
            case "prismarine_bricks", "dark_prismarine", "prismarine" -> mc("prismarine_brick_stairs");
            default -> mc("cobblestone_stairs");
        };
    }

    /** A round cavern of radius r, made of one strip per row. */
    Cave disc(int cx, int cz, int r, int f, int h) {
        return disc(cx, cz, r, f, h, floor);
    }

    Cave disc(int cx, int cz, int r, int f, int h, String floorBlock) {
        for (int dz = -r; dz <= r; dz++) {
            int half = (int) Math.floor(Math.sqrt((r + 0.5) * (r + 0.5) - dz * dz));
            area(cx - half, f + 1, cz + dz, cx + half, f + h, cz + dz, floorBlock);
        }
        return this;
    }

    /** True if the block is inside the air of any area. */
    boolean inside(int x, int y, int z) {
        for (int[] a : areas) {
            if (x >= a[0] && x <= a[3] && y >= a[1] && y <= a[4] && z >= a[2] && z <= a[5]) {
                return true;
            }
        }
        return false;
    }

    /** Shells, clears, floors, ceiling lights and stair blocks of every area. */
    Cave carve() {
        carved = true;
        for (int[] a : areas) {
            b.fill(a[0] - 1, a[1] - 1, a[2] - 1, a[3] + 1, a[4] + 1, a[5] + 1, wall);
        }
        for (int[] a : areas) {
            b.clear(a[0], a[1], a[2], a[3], a[4], a[5]);
        }
        for (int i = 0; i < areas.size(); i++) {
            int[] a = areas.get(i);
            b.fill(a[0], a[1] - 1, a[2], a[3], a[1] - 1, a[5], areaFloors.get(i));
        }
        if (light != null) {
            for (int[] a : areas) {
                int y = a[4] + 1;
                for (int x = a[0] + 1; x <= a[3]; x += 6) {
                    for (int z = a[2] + 1; z <= a[5]; z += 6) {
                        if (!inside(x, y, z) && !inside(x, a[1] - 1, z)) {
                            b.set(x, y, z, light);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < stairBlocks.size(); i++) {
            int[] s = stairBlocks.get(i);
            b.fill(s[0], s[1], s[2], s[3], s[4], s[5], stairStates.get(i));
        }
        return this;
    }

    // ------------------------------------------------------------------ ladders and entrances

    /** Support offset {dx, dz} of a ladder facing this way. */
    private static int[] support(String facing) {
        return switch (facing) {
            case "south" -> new int[]{0, -1};
            case "north" -> new int[]{0, 1};
            case "east" -> new int[]{-1, 0};
            default -> new int[]{1, 0};
        };
    }

    /** A facing whose supporting block is in the same chunk as the ladder. */
    static String autoFacing(int x, int z) {
        return (z & 15) != 0 ? "south" : "north";
    }

    /**
     * A ladder column at (x, z) from yBot to yTop (inclusive) on a support column, sealed with a
     * shell wherever it passes through rock below {@code shellTop}.
     */
    Cave ladder(int x, int z, int yBot, int yTop, String facing, int shellTop, int supportTop) {
        int[] s = support(facing);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int run = Integer.MIN_VALUE;
                for (int y = yBot; y <= shellTop + 1; y++) {
                    boolean rock = y <= shellTop && !inside(x + dx, y, z + dz);
                    if (rock && run == Integer.MIN_VALUE) {
                        run = y;
                    } else if (!rock && run != Integer.MIN_VALUE) {
                        b.fill(x + dx, run, z + dz, x + dx, y - 1, z + dz, wall);
                        run = Integer.MIN_VALUE;
                    }
                }
            }
        }
        b.fill(x + s[0], yBot, z + s[1], x + s[0], supportTop, z + s[1], wall);
        b.clear(x, yBot, z, x, yTop, z);
        b.fill(x, yBot, z, x, yTop, z, mc("ladder[facing=" + facing + "]"));
        return this;
    }

    /** A ladder between two floors of this dungeon (from just above floor fLow up to through floor fHigh). */
    Cave ladderUp(int x, int z, int fLow, int fHigh) {
        return ladder(x, z, fLow + 1, fHigh + 1, autoFacing(x, z), fHigh, fHigh + 1);
    }

    /**
     * A surface entrance at (x, z): a 7x7 plinth levelled at the surface, a small hut of
     * {@code hutWall} with a doorway on side {@code door}, and a ladder down to floor {@code f}.
     */
    Cave entrance(int x, int z, int f, String hutWall, String hutFloor, char door) {
        int s = SURFACE;
        b.op(StructureDef.Op.Type.FILL_AIR, x - 3, s - 11, z - 3, x + 3, s - 1, z + 3, wall, null);
        b.fill(x - 3, s, z - 3, x + 3, s, z + 3, hutFloor);
        b.clear(x - 3, s + 1, z - 3, x + 3, s + 8, z + 3);
        b.walls(x - 2, s + 1, z - 2, x + 2, s + 3, z + 2, hutWall);
        b.fill(x - 2, s + 4, z - 2, x + 2, s + 4, z + 2, hutWall);
        switch (door) {
            case 'n' -> b.clear(x, s + 1, z - 2, x, s + 2, z - 2);
            case 's' -> b.clear(x, s + 1, z + 2, x, s + 2, z + 2);
            case 'e' -> b.clear(x + 2, s + 1, z, x + 2, s + 2, z);
            default -> b.clear(x - 2, s + 1, z, x - 2, s + 2, z);
        }
        b.set(x + 1, s + 3, z + 1, mc("lantern[hanging=true]"));
        ladder(x, z, f + 1, s + 1, autoFacing(x, z), s - 1, s + 2);
        entrances.add(new Dungeons.Entrance(x - 3, z - 3, x + 3, z + 3, s + 8, true));
        return this;
    }

    /**
     * A crop circle entrance at (x, z): a 9x9 patch of standing wheat (hay bales) with a flattened ring
     * in its middle, levelled at the surface, and a ladder down to floor {@code f} at its centre.
     */
    Cave cropCircle(int x, int z, int f) {
        int s = SURFACE;
        b.op(StructureDef.Op.Type.FILL_AIR, x - 4, s - 11, z - 4, x + 4, s - 1, z + 4, wall, null);
        b.fill(x - 4, s, z - 4, x + 4, s, z + 4, mc("grass_block"));
        b.clear(x - 4, s + 1, z - 4, x + 4, s + 8, z + 4);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 >= 5 && d2 <= 10) {
                    b.set(x + dx, s, z + dz, mc("hay_block[axis=" + (Math.abs(dx) > Math.abs(dz) ? "z" : "x") + "]"));
                } else if (d2 > 13) {
                    b.set(x + dx, s + 1, z + dz, mc("hay_block"));
                }
            }
        }
        ladder(x, z, f + 1, s + 1, autoFacing(x, z), s - 1, s + 1);
        entrances.add(new Dungeons.Entrance(x - 4, z - 4, x + 4, z + 4, s + 8, true));
        return this;
    }

    /**
     * Continues the ladder of an entrance pit another module built: replaces its floor block at
     * (x, theirFloor, z) with ladder and carries the ladder on down to floor {@code f}.
     */
    Cave link(int x, int z, int theirFloor, int f, String facing) {
        ladder(x, z, f + 1, theirFloor, facing, theirFloor - 1, theirFloor - 1);
        entrances.add(new Dungeons.Entrance(x - 1, z - 1, x + 1, z + 1, theirFloor, false));
        return this;
    }

    // ------------------------------------------------------------------ details

    Cave set(int x, int y, int z, String block) {
        b.set(x, y, z, block);
        return this;
    }

    Cave fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.fill(x1, y1, z1, x2, y2, z2, block);
        return this;
    }

    /** A pillar from the floor f to the ceiling (f + h). */
    Cave pillar(int x, int z, int f, int h, String block) {
        b.fill(x, f + 1, z, x, f + h, z, block);
        return this;
    }

    /** A pool of fluid (lava, water) sunk into the floor: replaces the floor blocks, one deep. */
    Cave pool(int x1, int z1, int x2, int z2, int f, String fluid) {
        b.fill(x1, f, z1, x2, f, z2, fluid);
        return this;
    }

    /** A doorway blocker: a 1-wide gap at (x, z) in a wall across a corridor, filled with a block. */
    Cave barrier(int x1, int z1, int x2, int z2, int f, int h, int gx, int gz, String gapBlock) {
        b.fill(x1, f + 1, z1, x2, f + h, z2, wall);
        b.set(gx, f + 1, gz, gapBlock);
        return this;
    }

    /** Rocks along a row (scripted mining rocks of other modules / the core). */
    Cave rocks(int x1, int z1, int x2, int z2, int y, int step, String... blocks) {
        int i = 0;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x += step) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z += step) {
                b.set(x, y, z, blocks[i++ % blocks.length]);
            }
        }
        return this;
    }

    /** Spawns {@code cap} of the creatures inside the box on floor f. */
    Cave camp(int x1, int z1, int x2, int z2, int f, int cap, int respawnTicks, String... creatures) {
        camps.add(new Dungeons.Camp(id, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2), f + 1, cap,
                respawnTicks, List.of(creatures)));
        return this;
    }

    /**
     * An indoor Agility shortcut ({@code AgilityShortcuts.INDOOR}) between (ax, az) on floor fa and
     * (bx, bz) on floor fb: a shortcut block stands on each floor (after {@link #carve()}), and
     * using one carries the player to the other.
     */
    Cave shortcut(String shortcutId, int ax, int az, int fa, int bx, int bz, int fb) {
        if (com.runecraft.osrs.content.skilling.AgilityShortcuts.indoor(shortcutId) == null) {
            throw new IllegalArgumentException(id + ": unknown shortcut " + shortcutId);
        }
        if (!carved || !inside(ax, fa + 1, az) || !inside(bx, fb + 1, bz)) {
            throw new IllegalStateException(id + ": shortcut " + shortcutId + " must stand in carved air");
        }
        b.set(ax, fa + 1, az, rc(com.runecraft.osrs.content.skilling.AgilityShortcuts.BLOCK));
        b.set(bx, fb + 1, bz, rc(com.runecraft.osrs.content.skilling.AgilityShortcuts.BLOCK));
        shortcuts.add(new com.runecraft.osrs.content.skilling.AgilityShortcuts.BlockEnds(shortcutId, ax, fa + 1, az, bx, fb + 1, bz));
        return this;
    }

    /**
     * Places one obstacle of an agility course built into this dungeon: the numbered obstacle block
     * in the floor at (x, z) (stepping onto it crosses the obstacle).
     */
    Cave obstacle(int x, int z, int f, int course, int step) {
        b.set(x, f, z, rc("agility_obstacle[course=" + course + ",step=" + step + "]"));
        return this;
    }

    Dungeons.Dungeon build() {
        return new Dungeons.Dungeon(id, name, b.build(), List.copyOf(entrances), List.copyOf(camps), List.copyOf(shortcuts));
    }
}
