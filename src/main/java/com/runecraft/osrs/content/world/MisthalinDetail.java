package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Op;

import java.util.ArrayList;
import java.util.List;

/**
 * Finishing details shared by the western building kits (Misthalin, Asgarnia/Karamja and
 * Kandarin): weathered wall textures, window dressings (shutters, sills, lintels, window pots),
 * gable timbering and attic windows, ridge caps, chimneys with campfire smoke, wall lanterns,
 * banners, plinths, quoins, buttresses and arrow slits, plus street furniture (benches, sign posts,
 * crate stacks, training dummies, market awnings).
 *
 * <p>Everything here is <em>deferred</em>: the operations are collected and appended when the
 * structure is built ({@link #flush}), after all of its walls and furniture exist, and any of them
 * that would reach outside the structure's own footprint is dropped, so detailing never grows a
 * structure into its neighbours. Wall textures are {@code REPLACE} operations (they only touch the
 * wall's own material, never windows, doors or beams), and anything hung on the outside of a wall
 * is {@code FILL_AIR}, so it never cuts into a neighbouring building or a later street.
 *
 * <p>Heights are relative to the ground passed in; x/z are absolute block coordinates.
 */
final class MisthalinDetail {
    private record Pending(Op.Type type, int x1, int y1, int z1, int x2, int y2, int z2, String block, String extra) {
    }

    private final List<Pending> pending = new ArrayList<>();
    private int seed;

    MisthalinDetail(String id) {
        this.seed = id.hashCode();
    }

    // ------------------------------------------------------------------ randomness

    /** Deterministic noise in [0, 65535] for a position and salt. */
    static int noise(int x, int y, int z, int salt) {
        int h = x * 374761393 + y * 1442695041 + z * 668265263 + salt * 1103515245;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0xFFFF;
    }

    private int next(int bound) {
        seed = seed * 1103515245 + 12345;
        return Math.floorMod(seed >>> 8, bound);
    }

    // ------------------------------------------------------------------ deferred primitives (absolute y)

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        add(Op.Type.FILL, x1, y1, z1, x2, y2, z2, block, null);
    }

    void air(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        add(Op.Type.FILL_AIR, x1, y1, z1, x2, y2, z2, block, null);
    }

    void replace(int x1, int y1, int z1, int x2, int y2, int z2, String from, String to) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        add(Op.Type.REPLACE, x1, y1, z1, x2, y2, z2, to, from);
    }

    private void add(Op.Type t, int x1, int y1, int z1, int x2, int y2, int z2, String block, String extra) {
        pending.add(new Pending(t, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2),
                Math.max(z1, z2), block, extra));
    }

    /**
     * Appends the collected details to the builder, dropping any that would reach outside the
     * footprint of the structure's own operations (so the structure's bounds never change).
     */
    void flush(StructureDef.Builder b) {
        if (pending.isEmpty()) {
            return;
        }
        StructureDef core = b.build();
        int sx1 = Integer.MAX_VALUE;
        int sz1 = Integer.MAX_VALUE;
        int sx2 = Integer.MIN_VALUE;
        int sz2 = Integer.MIN_VALUE;
        for (Op op : core.ops()) {
            if (op.type() != Op.Type.FLATTEN && op.y2() < StructureDef.GROUND - 1) {
                continue;
            }
            int m = op.margin();
            sx1 = Math.min(sx1, op.x1() - m);
            sz1 = Math.min(sz1, op.z1() - m);
            sx2 = Math.max(sx2, op.x2() + m);
            sz2 = Math.max(sz2, op.z2() + m);
        }
        for (Pending p : pending) {
            if (p.x1() < sx1 || p.x2() > sx2 || p.z1() < sz1 || p.z2() > sz2) {
                continue;
            }
            if (p.y1() < -64 || p.y2() >= 320) {
                continue;
            }
            b.op(p.type(), p.x1(), p.y1(), p.z1(), p.x2(), p.y2(), p.z2(), p.block(), p.extra());
        }
        pending.clear();
    }

    // ------------------------------------------------------------------ directions

    static int dx(char d) {
        return d == 'e' ? 1 : d == 'w' ? -1 : 0;
    }

    static int dz(char d) {
        return d == 's' ? 1 : d == 'n' ? -1 : 0;
    }

    static String facing(char d) {
        return switch (d) {
            case 'n' -> "north";
            case 's' -> "south";
            case 'e' -> "east";
            default -> "west";
        };
    }

    static char side(String facing) {
        return facing.charAt(0);
    }

    // ------------------------------------------------------------------ materials

    static String bare(String block) {
        int i = block.indexOf('[');
        return i < 0 ? block : block.substring(0, i);
    }

    static boolean isStone(String block) {
        String n = bare(block);
        return n.contains("stone") || n.contains("cobble") || n.contains("andesite") || n.contains("deepslate")
                || n.contains("blackstone") || n.contains("brick") || n.contains("tuff") || n.contains("diorite")
                || n.contains("granite") || n.contains("quartz") || n.contains("calcite") || n.contains("sandstone");
    }

    static boolean isWood(String block) {
        String n = bare(block);
        return n.endsWith("_planks") || n.endsWith("_log") || n.endsWith("_wood") || n.contains("bamboo");
    }

    static boolean isPlaster(String block) {
        String n = bare(block);
        return n.endsWith("terracotta") || n.endsWith("concrete") || n.contains("wool") || n.equals("minecraft:mud_bricks")
                || n.equals("minecraft:packed_mud");
    }

    /** Weathering variants for a wall material (empty when it should stay uniform). */
    static String[] variants(String wall) {
        return switch (bare(wall)) {
            case "minecraft:stone_bricks" -> new String[]{"minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks",
                    "minecraft:cracked_stone_bricks", "minecraft:andesite", "minecraft:tuff", "minecraft:mossy_stone_bricks"};
            case "minecraft:cobblestone" -> new String[]{"minecraft:mossy_cobblestone", "minecraft:andesite", "minecraft:stone",
                    "minecraft:mossy_cobblestone", "minecraft:tuff"};
            case "minecraft:mossy_stone_bricks" -> new String[]{"minecraft:stone_bricks", "minecraft:cracked_stone_bricks",
                    "minecraft:mossy_cobblestone"};
            case "minecraft:andesite" -> new String[]{"minecraft:cobblestone", "minecraft:stone", "minecraft:tuff"};
            case "minecraft:polished_andesite" -> new String[]{"minecraft:andesite", "minecraft:smooth_stone"};
            case "minecraft:stone" -> new String[]{"minecraft:andesite", "minecraft:cobblestone", "minecraft:tuff"};
            case "minecraft:cobbled_deepslate" -> new String[]{"minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks",
                    "minecraft:tuff", "minecraft:deepslate"};
            case "minecraft:deepslate_bricks" -> new String[]{"minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles",
                    "minecraft:cobbled_deepslate"};
            case "minecraft:bricks" -> new String[]{"minecraft:granite", "minecraft:mud_bricks"};
            case "minecraft:quartz_bricks" -> new String[]{"minecraft:smooth_quartz", "minecraft:calcite", "minecraft:chiseled_quartz_block"};
            case "minecraft:smooth_quartz" -> new String[]{"minecraft:quartz_bricks", "minecraft:calcite"};
            case "minecraft:calcite" -> new String[]{"minecraft:diorite", "minecraft:smooth_quartz"};
            case "minecraft:polished_diorite" -> new String[]{"minecraft:diorite", "minecraft:calcite"};
            case "minecraft:white_concrete" -> new String[]{"minecraft:calcite", "minecraft:smooth_quartz"};
            case "minecraft:mud_bricks" -> new String[]{"minecraft:packed_mud", "minecraft:mud_bricks"};
            case "minecraft:white_terracotta" -> new String[]{"minecraft:white_terracotta"};
            case "minecraft:spruce_planks" -> new String[]{"minecraft:stripped_spruce_wood"};
            case "minecraft:dark_oak_planks" -> new String[]{"minecraft:stripped_dark_oak_wood"};
            case "minecraft:polished_blackstone_bricks" -> new String[]{"minecraft:cracked_polished_blackstone_bricks",
                    "minecraft:blackstone"};
            default -> new String[0];
        };
    }

    /** A plinth (base course) material for a wall. */
    static String plinthFor(String wall) {
        String n = bare(wall);
        if (n.contains("quartz") || n.contains("calcite") || n.contains("concrete") || n.contains("diorite")) {
            return "minecraft:polished_diorite";
        }
        if (n.contains("deepslate") || n.contains("blackstone")) {
            return "minecraft:polished_blackstone_bricks";
        }
        if (n.contains("sandstone")) {
            return "minecraft:cut_sandstone";
        }
        if (n.equals("minecraft:stone_bricks")) {
            return "minecraft:cobblestone";
        }
        if (n.equals("minecraft:cobblestone")) {
            return "minecraft:mossy_cobblestone";
        }
        if (n.contains("jungle") || n.contains("bamboo")) {
            return "minecraft:mossy_cobblestone";
        }
        return "minecraft:cobblestone";
    }

    /** A quoin (dressed corner stone) for a stone wall. */
    static String quoinFor(String wall) {
        String n = bare(wall);
        if (n.contains("quartz") || n.contains("calcite") || n.contains("concrete") || n.contains("diorite")) {
            return "minecraft:quartz_bricks";
        }
        if (n.contains("deepslate")) {
            return "minecraft:polished_deepslate";
        }
        if (n.contains("blackstone")) {
            return "minecraft:polished_blackstone";
        }
        if (n.contains("sandstone")) {
            return "minecraft:smooth_sandstone";
        }
        if (n.equals("minecraft:cobblestone") || n.equals("minecraft:andesite")) {
            return "minecraft:stone_bricks";
        }
        return "minecraft:polished_andesite";
    }

    /** Base name of the stair/slab/wall family matching a masonry block ("minecraft:stone_brick"). */
    static String family(String wall) {
        String n = bare(wall);
        return switch (n) {
            case "minecraft:cobblestone", "minecraft:mossy_cobblestone" -> n;
            case "minecraft:quartz_bricks", "minecraft:smooth_quartz", "minecraft:quartz_block", "minecraft:calcite",
                    "minecraft:white_concrete", "minecraft:polished_diorite" -> "minecraft:smooth_quartz";
            case "minecraft:deepslate_bricks", "minecraft:cobbled_deepslate", "minecraft:deepslate_tiles" -> "minecraft:deepslate_brick";
            case "minecraft:polished_blackstone_bricks", "minecraft:blackstone" -> "minecraft:polished_blackstone_brick";
            case "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:cut_sandstone" -> "minecraft:sandstone";
            case "minecraft:polished_andesite", "minecraft:andesite" -> n;
            case "minecraft:bricks" -> "minecraft:brick";
            case "minecraft:mud_bricks" -> "minecraft:mud_brick";
            case "minecraft:mossy_stone_bricks" -> "minecraft:mossy_stone_brick";
            default -> "minecraft:stone_brick";
        };
    }

    static String slabFor(String wall) {
        return family(wall) + "_slab";
    }

    static String stairsFor(String wall) {
        return family(wall) + "_stairs";
    }

    static String wallFor(String wall) {
        String f = family(wall);
        return f.equals("minecraft:smooth_quartz") ? "minecraft:diorite_wall" : f.contains("andesite") ? "minecraft:andesite_wall" : f + "_wall";
    }

    /** The stripped log used for timber framing next to a wood type ("spruce", "dark_oak", ...). */
    static String beamFor(String wood) {
        return "minecraft:stripped_" + (wood == null ? "spruce" : wood) + "_log";
    }

    /** Wood used for shutters and sills next to a wall. */
    static String shutterWood(String wall, String fallback) {
        String n = bare(wall);
        if (n.contains("jungle") || n.contains("bamboo")) {
            return "jungle";
        }
        if (n.contains("birch") || n.contains("calcite") || n.contains("quartz")) {
            return "birch";
        }
        return fallback == null ? "spruce" : fallback;
    }

    // ------------------------------------------------------------------ walls

    /**
     * Weathers the four outer faces of a block box between absolute heights y1..y2: about one
     * block in {@code every} of material {@code wall} becomes a variant (cracked, mossy, cobble,
     * tuff...). Only blocks that are still {@code wall} are touched.
     */
    void texture(int x1, int z1, int x2, int z2, int y1, int y2, String wall, int every) {
        String[] v = variants(wall);
        if (v.length == 0 || y2 < y1) {
            return;
        }
        int w = x2 - x1 + 1;
        int d = z2 - z1 + 1;
        int h = y2 - y1 + 1;
        int per = Math.max(1, 2 * (w + d) - 4);
        int n = Math.max(1, per * h / Math.max(2, every));
        for (int i = 0; i < n; i++) {
            int k = next(per);
            int x;
            int z;
            if (k < w) {
                x = x1 + k;
                z = z1;
            } else if (k < 2 * w) {
                x = x1 + k - w;
                z = z2;
            } else if (k < 2 * w + d - 2) {
                x = x1;
                z = z1 + 1 + (k - 2 * w);
            } else {
                x = x2;
                z = z1 + 1 + Math.floorMod(k - 2 * w - (d - 2), Math.max(1, d - 2));
            }
            int y = y1 + next(h);
            String to = v[next(v.length)];
            // short horizontal runs read as patches rather than noise
            boolean run = next(3) == 0;
            if (run && (z == z1 || z == z2) && x + 1 <= x2) {
                replace(x, y, z, x + 1, y, z, wall, to);
            } else if (run && (x == x1 || x == x2) && z + 1 <= z2) {
                replace(x, y, z, x, y, z + 1, wall, to);
            } else {
                replace(x, y, z, x, y, z, wall, to);
            }
        }
    }

    /** Weathers a solid wall run (a curtain wall or bridge pier) over its whole box. */
    void textureSolid(int x1, int y1, int z1, int x2, int y2, int z2, String wall, int every) {
        String[] v = variants(wall);
        if (v.length == 0) {
            return;
        }
        long vol = (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        long faces = Math.min(vol, 2L * ((x2 - x1 + 1) + (z2 - z1 + 1)) * (y2 - y1 + 1));
        int n = (int) Math.max(1, Math.min(600, faces / Math.max(2, every)));
        for (int i = 0; i < n; i++) {
            int x = x1 + next(x2 - x1 + 1);
            int y = y1 + next(y2 - y1 + 1);
            int z = z1 + next(z2 - z1 + 1);
            replace(x, y, z, x, y, z, wall, v[next(v.length)]);
        }
    }

    /** A plinth course: the wall's bottom row (absolute y) of a box shell becomes {@code plinth}. */
    void plinth(int x1, int z1, int x2, int z2, int y, String wall, String plinth) {
        replace(x1, y, z1, x2, y, z1, wall, plinth);
        replace(x1, y, z2, x2, y, z2, wall, plinth);
        replace(x1, y, z1, x1, y, z2, wall, plinth);
        replace(x2, y, z1, x2, y, z2, wall, plinth);
    }

    /** Alternating dressed quoins up the four corners of a box shell. */
    void quoins(int x1, int z1, int x2, int z2, int y1, int y2, String wall, String quoin) {
        for (int y = y1; y <= y2; y += 2) {
            replace(x1, y, z1, x1, y, z1, wall, quoin);
            replace(x2, y, z1, x2, y, z1, wall, quoin);
            replace(x1, y, z2, x1, y, z2, wall, quoin);
            replace(x2, y, z2, x2, y, z2, wall, quoin);
        }
    }

    /**
     * Timber framing on a plaster/plank shell: corner posts, a beam at each floor line and studs
     * every {@code every} blocks (only over blocks that are still the infill material).
     */
    void timber(int x1, int z1, int x2, int z2, int y1, int y2, int storeyH, String infill, String log, int every) {
        String lx = log + "[axis=x]";
        String lz = log + "[axis=z]";
        String ly = log + "[axis=y]";
        for (int[] c : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
            replace(c[0], y1, c[1], c[0], y2, c[1], infill, ly);
        }
        for (int y = y1 + storeyH - 1; y <= y2; y += storeyH) {
            replace(x1, y, z1, x2, y, z1, infill, lx);
            replace(x1, y, z2, x2, y, z2, infill, lx);
            replace(x1, y, z1, x1, y, z2, infill, lz);
            replace(x2, y, z1, x2, y, z2, infill, lz);
        }
        for (int x = x1 + every; x <= x2 - 2; x += every) {
            replace(x, y1, z1, x, y2, z1, infill, ly);
            replace(x, y1, z2, x, y2, z2, infill, ly);
        }
        for (int z = z1 + every; z <= z2 - 2; z += every) {
            replace(x1, y1, z, x1, y2, z, infill, ly);
            replace(x2, y1, z, x2, y2, z, infill, ly);
        }
    }

    /**
     * Dresses a window opening at (x, y1..y2, z) in a wall facing {@code out}: a lintel over it
     * (replacing {@code wall}), open shutters either side, and on upper floors a sill with, now and
     * then, a pot of flowers.
     */
    void window(int x, int y1, int y2, int z, char out, String wall, String wood, String lintel, int groundY) {
        int ox = x + dx(out);
        int oz = z + dz(out);
        int px = dz(out) != 0 ? 1 : 0;
        int pz = dx(out) != 0 ? 1 : 0;
        if (lintel != null && wall != null) {
            replace(x, y2 + 1, z, x, y2 + 1, z, wall, lintel);
        }
        String shutter = "minecraft:" + wood + "_trapdoor[facing=" + facing(out) + ",half=bottom,open=true]";
        air(ox - px, y1, oz - pz, ox - px, y2, oz - pz, shutter);
        air(ox + px, y1, oz + pz, ox + px, y2, oz + pz, shutter);
        if (y1 - groundY >= 2) {
            air(ox, y1 - 1, oz, ox, y1 - 1, oz, "minecraft:" + wood + "_trapdoor[facing=" + facing(out) + ",half=top]");
            int r = noise(x, y1, z, 7) % 5;
            if (r == 0) {
                air(ox, y1, oz, ox, y1, oz, "minecraft:potted_red_tulip");
            } else if (r == 1) {
                air(ox, y1, oz, ox, y1, oz, "minecraft:potted_azure_bluet");
            } else if (r == 2) {
                air(ox, y1, oz, ox, y1, oz, "minecraft:potted_cornflower");
            }
        }
    }

    /**
     * A castle window: a dressed lintel over the opening (replacing {@code wall}) and, above the
     * ground floor, a stone sill outside it.
     */
    void castleWindow(int x, int y1, int y2, int z, char out, String wall, String lintel, int groundY) {
        if (lintel != null && wall != null) {
            replace(x, y2 + 1, z, x, y2 + 1, z, wall, lintel);
        }
        if (y1 - groundY >= 2) {
            air(x + dx(out), y1 - 1, z + dz(out), x + dx(out), y1 - 1, z + dz(out), slabFor(wall) + "[type=top]");
        }
    }

    /** A lantern hung from a short iron bracket on the outside of a wall (x,z is the block in front). */
    void wallLantern(int x, int y, int z) {
        air(x, y + 1, z, x, y + 1, z, "minecraft:spruce_fence");
        air(x, y, z, x, y, z, "minecraft:lantern[hanging=true]");
    }

    /** A wall banner hung on the outside of a wall (x,z is the block in front of the wall). */
    void banner(int x, int y, int z, char out, String colour) {
        air(x, y, z, x, y, z, "minecraft:" + colour + "_wall_banner[facing=" + facing(out) + "]");
    }

    /** A chimney stack (absolute heights) topped with a smoking campfire. */
    void chimney(int x, int z, int y1, int y2, String block) {
        fill(x, y1, z, x, y2, z, block);
        fill(x, y2 + 1, z, x, y2 + 1, z, "minecraft:campfire[lit=true]");
    }

    /**
     * Arrow slits: replaces the wall material by iron bars at (x, y1..y2, z) and adds a stone sill
     * outside (only where the opening is still wall).
     */
    void slit(int x, int y1, int y2, int z, String wall) {
        replace(x, y1, z, x, y2, z, wall, "minecraft:iron_bars");
    }

    /**
     * Stepped buttresses along a straight wall face: from (x1,z1) to (x2,z2) on the outer face
     * pointing {@code out}, every {@code every} blocks, rising to absolute {@code top}.
     */
    void buttresses(int x1, int z1, int x2, int z2, char out, int every, int y0, int top, String block, String stairs) {
        boolean alongX = dz(out) != 0;
        int a = alongX ? Math.min(x1, x2) : Math.min(z1, z2);
        int c = alongX ? Math.max(x1, x2) : Math.max(z1, z2);
        int line = alongX ? z1 : x1;
        for (int u = a; u <= c; u += every) {
            int x = alongX ? u : line + dx(out);
            int z = alongX ? line + dz(out) : u;
            int x2b = alongX ? u : line + 2 * dx(out);
            int z2b = alongX ? line + 2 * dz(out) : u;
            int mid = y0 + (top - y0) / 2;
            air(x, y0, z, x, top - 1, z, block);
            air(x2b, y0, z2b, x2b, mid - 1, z2b, block);
            air(x2b, mid, z2b, x2b, mid, z2b, stairs + "[facing=" + facing(opposite(out)) + "]");
            air(x, top, z, x, top, z, stairs + "[facing=" + facing(opposite(out)) + "]");
        }
    }

    /**
     * Round-tower dressing: weathering, a plinth, string courses every {@code storeyH} blocks and,
     * for open battlements, a corbelled parapet one block outside the wall (slabs below, a parapet
     * ring and alternate merlons). Heights absolute; {@code top} is the battlement walk level.
     */
    void roundTower(int cx, int cz, int r, int y0, int top, int storeyH, String wall, boolean corbel) {
        textureSolid(cx - r, y0, cz - r, cx + r, top - 1, cz + r, wall, 5);
        replace(cx - r, y0, cz - r, cx + r, y0, cz + r, wall, plinthFor(wall));
        String trim = quoinFor(wall);
        for (int y = y0 + storeyH - 1; y < top - 1; y += storeyH) {
            replace(cx - r, y, cz - r, cx + r, y, cz + r, wall, trim);
        }
        if (!corbel) {
            return;
        }
        String slab = slabFor(wall) + "[type=top]";
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double dd = Math.sqrt(dx * dx + dz * dz);
                if (dd <= r + 0.5 || dd > r + 1.5) {
                    continue;
                }
                air(cx + dx, top - 1, cz + dz, cx + dx, top - 1, cz + dz, slab);
                air(cx + dx, top, cz + dz, cx + dx, top, cz + dz, wall);
                if (((dx + dz) & 1) != 0) {
                    air(cx + dx, top + 1, cz + dz, cx + dx, top + 1, cz + dz, wall);
                }
            }
        }
    }

    /**
     * A steep conical spire (only into air) from absolute height y: discs shrinking by one block
     * of radius every {@code steep} layers, a finial and a pennant on top.
     */
    void spire(int cx, int cz, int r, int y, int steep, String block, String pennant) {
        int yy = y;
        for (int rr = r; rr >= 1; rr--) {
            for (int s = 0; s < steep; s++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    double v = (rr + 0.4) * (rr + 0.4) - dz * dz;
                    if (v < 0) {
                        continue;
                    }
                    int w = (int) Math.floor(Math.sqrt(v));
                    air(cx - w, yy, cz + dz, cx + w, yy, cz + dz, block);
                }
                yy++;
            }
        }
        air(cx, yy, cz, cx, yy + 1, cz, block);
        air(cx, yy + 2, cz, cx, yy + 4, cz, "minecraft:spruce_fence");
        if (pennant != null) {
            air(cx + 1, yy + 4, cz, cx + 2, yy + 4, cz, "minecraft:" + pennant + "_wool");
            air(cx + 1, yy + 3, cz, cx + 1, yy + 3, cz, "minecraft:" + pennant + "_wool");
        }
    }

    static char opposite(char d) {
        return switch (d) {
            case 'n' -> 's';
            case 's' -> 'n';
            case 'e' -> 'w';
            default -> 'e';
        };
    }

    // ------------------------------------------------------------------ roofs

    /**
     * Gable dressing for a pitched roof whose gable walls are at the two ends of the ridge: a tie
     * beam over the eaves, a king post and a small attic window, all only over blocks that are
     * still {@code gable} material. {@code alongX} is the ridge direction; y is the first roof layer.
     */
    void gables(int x1, int z1, int x2, int z2, int y, boolean alongX, String gable, String log, String window) {
        int span = alongX ? z2 - z1 : x2 - x1;
        if (span < 4 || gable == null) {
            return;
        }
        int layers = span / 2;
        int[] ends = alongX ? new int[]{x1, x2} : new int[]{z1, z2};
        int a = alongX ? z1 : x1;
        int c = alongX ? z2 : x2;
        int mid = (a + c) / 2;
        String beam = log == null ? null : log + (alongX ? "[axis=z]" : "[axis=x]");
        String post = log == null ? null : log + "[axis=y]";
        for (int e : ends) {
            if (alongX) {
                replace(e, y, a, e, y, c, gable, beam);
                if (span >= 7) {
                    replace(e, y + 1, mid, e, y + layers, mid, gable, post);
                }
                if (span >= 7) {
                    replace(e, y + 2, mid - 2, e, y + 2, mid - 2, gable, window);
                    replace(e, y + 2, mid + 2 + ((c - a) & 1), e, y + 2, mid + 2 + ((c - a) & 1), gable, window);
                } else {
                    replace(e, y + 1, mid + 1, e, y + 1, mid + 1, gable, window);
                }
            } else {
                replace(a, y, e, c, y, e, gable, beam);
                if (span >= 7) {
                    replace(mid, y + 1, e, mid, y + layers, e, gable, post);
                }
                if (span >= 7) {
                    replace(mid - 2, y + 2, e, mid - 2, y + 2, e, gable, window);
                    replace(mid + 2 + ((c - a) & 1), y + 2, e, mid + 2 + ((c - a) & 1), y + 2, e, gable, window);
                } else {
                    replace(mid + 1, y + 1, e, mid + 1, y + 1, e, gable, window);
                }
            }
        }
    }

    // ------------------------------------------------------------------ street furniture (absolute heights)

    /** A bench of stairs with trapdoor arm rests, {@code len} long, seats facing {@code faces}. */
    void bench(int x, int y, int z, char along, int len, char faces, String wood) {
        int ux = dx(along);
        int uz = dz(along);
        String seat = "minecraft:" + wood + "_stairs[facing=" + facing(opposite(faces)) + "]";
        for (int i = 0; i < len; i++) {
            air(x + ux * i, y, z + uz * i, x + ux * i, y, z + uz * i, seat);
        }
        String arm = "minecraft:" + wood + "_trapdoor[facing=" + facing(along) + ",open=true]";
        String arm2 = "minecraft:" + wood + "_trapdoor[facing=" + facing(opposite(along)) + ",open=true]";
        air(x - ux, y, z - uz, x - ux, y, z - uz, arm2);
        air(x + ux * len, y, z + uz * len, x + ux * len, y, z + uz * len, arm);
    }

    /** A sign post: a fence post carrying two arm boards. */
    void signpost(int x, int y, int z, String wood) {
        air(x, y, z, x, y + 2, z, "minecraft:" + wood + "_fence");
        air(x + 1, y + 2, z, x + 1, y + 2, z, "minecraft:" + wood + "_trapdoor[facing=west,half=top]");
        air(x, y + 1, z - 1, x, y + 1, z - 1, "minecraft:" + wood + "_trapdoor[facing=south,half=top]");
        air(x, y + 3, z, x, y + 3, z, "minecraft:" + wood + "_pressure_plate");
    }

    /** A stack of crates and barrels with a sack (hay) on top. */
    void crates(int x, int y, int z) {
        air(x, y, z, x, y, z, "minecraft:barrel[facing=up]");
        air(x + 1, y, z, x + 1, y, z, "minecraft:spruce_planks");
        air(x, y, z + 1, x, y, z + 1, "minecraft:barrel[facing=north]");
        air(x, y + 1, z, x, y + 1, z, "minecraft:hay_block");
        air(x + 1, y + 1, z, x + 1, y + 1, z, "minecraft:spruce_trapdoor[half=bottom]");
    }

    /** A training dummy: post, straw body, cross-arm and sack head. */
    void dummy(int x, int y, int z) {
        air(x, y, z, x, y, z, "minecraft:oak_fence");
        air(x, y + 1, z, x, y + 1, z, "minecraft:hay_block");
        air(x - 1, y + 1, z, x - 1, y + 1, z, "minecraft:oak_trapdoor[facing=west,open=true]");
        air(x + 1, y + 1, z, x + 1, y + 1, z, "minecraft:oak_trapdoor[facing=east,open=true]");
        air(x, y + 2, z, x, y + 2, z, "minecraft:carved_pumpkin[facing=south]");
    }

    /** An armour display: a stand of wall, iron body, helm and a weapon rack beside it. */
    void armourStand(int x, int y, int z, char faces) {
        air(x, y, z, x, y, z, "minecraft:polished_blackstone_wall");
        air(x, y + 1, z, x, y + 1, z, "minecraft:iron_block");
        air(x, y + 2, z, x, y + 2, z, "minecraft:skeleton_skull[rotation=" + rotation(faces) + "]");
    }

    static int rotation(char faces) {
        return switch (faces) {
            case 's' -> 0;
            case 'w' -> 4;
            case 'n' -> 8;
            default -> 12;
        };
    }

    /** A flower tub: a block of rooted dirt with a flower or bush on top. */
    void planter(int x, int y, int z, int variant) {
        air(x, y, z, x, y, z, "minecraft:rooted_dirt");
        String[] p = {"minecraft:rose_bush[half=lower]", "minecraft:peony[half=lower]", "minecraft:azalea", "minecraft:lilac[half=lower]",
                "minecraft:flowering_azalea"};
        air(x, y + 1, z, x, y + 1, z, p[Math.floorMod(variant, p.length)]);
    }

    /** A lamp post: stone base, post, cross-arm and a lantern hanging either side. */
    void lamp(int x, int y, int z, String post) {
        air(x, y, z, x, y, z, "minecraft:stone_brick_wall");
        air(x, y + 1, z, x, y + 3, z, post);
        air(x - 1, y + 3, z, x + 1, y + 3, z, post);
        air(x - 1, y + 2, z, x - 1, y + 2, z, "minecraft:lantern[hanging=true]");
        air(x + 1, y + 2, z, x + 1, y + 2, z, "minecraft:lantern[hanging=true]");
        air(x, y + 4, z, x, y + 4, z, "minecraft:stone_brick_slab");
    }
}
