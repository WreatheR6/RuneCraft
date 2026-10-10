package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.content.world.EasternDetail;
import com.runecraft.osrs.content.world.EasternDetail.Furniture;
import com.runecraft.osrs.content.world.EasternDetail.Look;
import com.runecraft.osrs.content.world.EasternDetail.Room;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.Random;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * House styles and detailed building helpers for Great Kourend and Varlamore: each of the five houses of
 * Kourend has its own look (Arceuus dark and purple, Hosidius whitewashed farm timber, Piscarilius tarred
 * harbour planks, Shayzien military stone with red roofs, Lovakengj dwarven deepslate and copper), Kingstown
 * and the castle are royal stone and timber, and Civitas illa Fortis is white and terracotta with columns and
 * red tiled roofs. Built on the shared {@link EasternDetail} kit.
 */
final class ZeahDetail {
    private ZeahDetail() {
    }

    /** A building style: shell blocks, door and window blocks, the facade look and the furniture palette. */
    record House(String wall, String floor, String roof, boolean pitched, String door, String window, Look look, Furniture furn) {
    }

    static final House ARCEUUS = new House("minecraft:polished_blackstone_bricks", "minecraft:dark_oak_planks",
            "minecraft:polished_blackstone_brick", true, "minecraft:dark_oak_door", "minecraft:purple_stained_glass_pane",
            new Look("minecraft:polished_blackstone_bricks", "minecraft:blackstone", "minecraft:purple_terracotta",
                    "minecraft:polished_blackstone", false, "minecraft:polished_blackstone_brick_stairs", null,
                    "minecraft:purple_terracotta", "minecraft:blackstone", "minecraft:soul_lantern", null, true, null, false),
            new Furniture("minecraft:dark_oak", "minecraft:purple_carpet", "minecraft:black_carpet", "minecraft:purple_bed",
                    "minecraft:soul_lantern", false));
    static final House HOSIDIUS = new House("minecraft:white_terracotta", "minecraft:oak_planks", "minecraft:spruce", true,
            "minecraft:spruce_door", "minecraft:glass_pane",
            new Look("minecraft:white_terracotta", "minecraft:cobblestone", "minecraft:spruce_log", "minecraft:spruce_log", true,
                    "minecraft:spruce_stairs", "minecraft:warped_trapdoor", "minecraft:white_terracotta", "minecraft:bricks",
                    "minecraft:lantern", null, true, null, false),
            new Furniture("minecraft:oak", "minecraft:green_carpet", "minecraft:brown_carpet", "minecraft:green_bed",
                    "minecraft:lantern", false));
    static final House PISCARILIUS = new House("minecraft:spruce_planks", "minecraft:spruce_planks", "minecraft:prismarine_brick",
            true, "minecraft:spruce_door", "minecraft:glass_pane",
            new Look("minecraft:spruce_planks", "minecraft:cobblestone", "minecraft:stripped_spruce_log", "minecraft:spruce_log", true,
                    "minecraft:spruce_stairs", "minecraft:spruce_trapdoor", null, "minecraft:cobblestone", "minecraft:lantern", null,
                    true, "minecraft:dark_prismarine", true),
            new Furniture("minecraft:spruce", "minecraft:cyan_carpet", "minecraft:blue_carpet", "minecraft:cyan_bed",
                    "minecraft:lantern", true));
    static final House SHAYZIEN = new House("minecraft:stone_bricks", "minecraft:polished_andesite", "minecraft:red_nether_brick",
            true, "minecraft:spruce_door", "minecraft:glass_pane",
            new Look("minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:red_nether_bricks", "minecraft:polished_andesite",
                    false, "minecraft:stone_brick_stairs", null, null, "minecraft:stone_bricks", "minecraft:lantern", null, true, null,
                    false),
            new Furniture("minecraft:spruce", "minecraft:red_carpet", "minecraft:gray_carpet", "minecraft:red_bed",
                    "minecraft:lantern", false));
    static final House LOVAKENGJ = new House("minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:deepslate_tile",
            true, "minecraft:spruce_door", "minecraft:orange_stained_glass_pane",
            new Look("minecraft:deepslate_bricks", "minecraft:cobbled_deepslate", "minecraft:waxed_cut_copper",
                    "minecraft:polished_deepslate", false, "minecraft:deepslate_brick_stairs", "minecraft:iron_trapdoor", null,
                    "minecraft:deepslate_bricks", "minecraft:lantern", null, true, "minecraft:cobbled_deepslate", false),
            new Furniture("minecraft:dark_oak", "minecraft:orange_carpet", "minecraft:brown_carpet", "minecraft:orange_bed",
                    "minecraft:lantern", false));
    static final House KINGSTOWN = new House("minecraft:white_terracotta", "minecraft:spruce_planks", "minecraft:dark_oak", true,
            "minecraft:dark_oak_door", "minecraft:glass_pane",
            new Look("minecraft:white_terracotta", "minecraft:stone_bricks", "minecraft:dark_oak_log", "minecraft:dark_oak_log", true,
                    "minecraft:dark_oak_stairs", "minecraft:crimson_trapdoor", "minecraft:white_terracotta", "minecraft:bricks",
                    "minecraft:lantern", null, true, null, false),
            new Furniture("minecraft:spruce", "minecraft:purple_carpet", "minecraft:brown_carpet", "minecraft:purple_bed",
                    "minecraft:lantern", false));
    static final House KITCHEN = new House("minecraft:bricks", "minecraft:spruce_planks", "minecraft:spruce", true,
            "minecraft:spruce_door", "minecraft:glass_pane",
            new Look("minecraft:bricks", "minecraft:stone_bricks", "minecraft:stripped_spruce_log", "minecraft:spruce_log", false,
                    "minecraft:brick_stairs", "minecraft:spruce_trapdoor", "minecraft:spruce_planks", "minecraft:bricks",
                    "minecraft:lantern", null, true, null, false),
            new Furniture("minecraft:spruce", "minecraft:purple_carpet", "minecraft:brown_carpet", "minecraft:purple_bed",
                    "minecraft:lantern", false));
    static final House CASTLE = new House("minecraft:stone_bricks", "minecraft:polished_andesite", "minecraft:deepslate_tile", true,
            "minecraft:dark_oak_door", "minecraft:glass_pane",
            new Look("minecraft:stone_bricks", "minecraft:andesite", "minecraft:polished_andesite", "minecraft:chiseled_stone_bricks",
                    false, "minecraft:stone_brick_stairs", null, null, "minecraft:stone_bricks", "minecraft:lantern", null, true, null,
                    false),
            new Furniture("minecraft:dark_oak", "minecraft:purple_carpet", "minecraft:black_carpet", "minecraft:purple_bed",
                    "minecraft:lantern", false));
    static final House GUILD = new House("minecraft:stripped_oak_log", "minecraft:oak_planks", "minecraft:oak", true,
            "minecraft:oak_door", "minecraft:glass_pane",
            new Look("minecraft:stripped_oak_log", "minecraft:cobblestone", "minecraft:oak_log", "minecraft:oak_log", false,
                    "minecraft:oak_stairs", "minecraft:oak_trapdoor", "minecraft:oak_planks", "minecraft:cobblestone",
                    "minecraft:lantern", null, true, "minecraft:spruce", false),
            new Furniture("minecraft:oak", "minecraft:green_carpet", "minecraft:brown_carpet", "minecraft:green_bed",
                    "minecraft:lantern", false));
    static final House MANOR = new House("minecraft:mossy_stone_bricks", "minecraft:oak_planks", "minecraft:dark_oak", true,
            "minecraft:dark_oak_door", "minecraft:glass_pane",
            new Look("minecraft:mossy_stone_bricks", "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:dark_oak_log", false,
                    "minecraft:stone_brick_stairs", "minecraft:warped_trapdoor", "minecraft:dark_oak_planks", "minecraft:bricks",
                    "minecraft:lantern", null, true, "minecraft:spruce", false),
            new Furniture("minecraft:dark_oak", "minecraft:green_carpet", "minecraft:lime_carpet", "minecraft:green_bed",
                    "minecraft:lantern", false));
    static final House CIVITAS_WHITE = new House("minecraft:calcite", "minecraft:spruce_planks", "minecraft:brick", true,
            "minecraft:acacia_door", "minecraft:glass_pane",
            new Look("minecraft:calcite", "minecraft:terracotta", "minecraft:smooth_sandstone", "minecraft:quartz_pillar", false,
                    "minecraft:smooth_quartz_stairs", "minecraft:acacia_trapdoor", "minecraft:calcite", null, "minecraft:lantern", null,
                    true, "minecraft:granite", false),
            new Furniture("minecraft:acacia", "minecraft:orange_carpet", "minecraft:yellow_carpet", "minecraft:orange_bed",
                    "minecraft:lantern", false));
    static final House CIVITAS_SAND = new House("minecraft:smooth_sandstone", "minecraft:spruce_planks", "minecraft:brick", true,
            "minecraft:acacia_door", "minecraft:glass_pane",
            new Look("minecraft:smooth_sandstone", "minecraft:terracotta", "minecraft:cut_sandstone", "minecraft:quartz_pillar", false,
                    "minecraft:smooth_sandstone_stairs", "minecraft:jungle_trapdoor", "minecraft:smooth_sandstone", null,
                    "minecraft:lantern", null, true, "minecraft:mud_brick", false),
            new Furniture("minecraft:jungle", "minecraft:red_carpet", "minecraft:yellow_carpet", "minecraft:yellow_bed",
                    "minecraft:lantern", false));
    static final House CIVITAS_HALL = new House("minecraft:cut_sandstone", "minecraft:smooth_sandstone", "minecraft:smooth_sandstone",
            false, "minecraft:acacia_door", "minecraft:glass_pane",
            new Look("minecraft:cut_sandstone", "minecraft:terracotta", "minecraft:smooth_sandstone", "minecraft:quartz_pillar", false,
                    "minecraft:smooth_sandstone_stairs", null, null, null, "minecraft:lantern", null, true, null, false),
            new Furniture("minecraft:jungle", "minecraft:yellow_carpet", "minecraft:orange_carpet", "minecraft:yellow_bed",
                    "minecraft:lantern", false));

    static int[] rect(int tx1, int ty1, int tx2, int ty2) {
        return new int[]{bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1};
    }

    static long seed(int[] q) {
        return q[0] * 73856093L ^ q[1] * 19349663L ^ q[2] * 83492791L ^ q[3];
    }

    /**
     * A detailed building in a house style over a tile rectangle: shell, dressed facade, roof, double door,
     * ladder between storeys, lights and furnished rooms (the ground floor as {@code ground}). Station blocks
     * are placed by the caller afterwards. Returns the block rectangle.
     */
    static int[] house(Builder b, int tx1, int ty1, int tx2, int ty2, int storeys, House h, String door, Room ground) {
        EasternDetail d = new EasternDetail(b);
        int g = b.ground();
        int[] q = rect(tx1, ty1, tx2, ty2);
        b.tBuilding(tx1, ty1, tx2, ty2, storeys, h.wall(), h.floor(), h.roof(), door, h.pitched());
        int top = g + storeys * 4;
        if (h.pitched()) {
            b.fill(q[0] + 1, top - 1, q[1] + 1, q[2] - 1, top - 1, q[3] - 1, h.floor());
        }
        if (!h.window().equals("minecraft:glass_pane")) {
            b.replace(q[0], g, q[1], q[2], top, q[3], "minecraft:glass_pane", h.window());
        }
        d.facade(q, storeys, h.look(), door, h.window());
        if (h.pitched()) {
            d.roof(q, top, h.roof(), h.look(), door, seed(q));
        } else {
            d.flatRoof(q, top, h.wall(), h.look().post(), null, null, seed(q));
        }
        if (door != null && h.door() != null) {
            doors(b, q, door, h.door());
        }
        if (storeys > 1) {
            b.fill(q[0] + 1, g, q[1] + 1, q[0] + 1, g + (storeys - 1) * 4, q[1] + 1, "minecraft:ladder[facing=south]");
        }
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        for (int s = 0; s < storeys; s++) {
            b.set(mx, g + s * 4 + 2, mz, h.furn().light() + "[hanging=true]");
            Room r = s == 0 ? ground : (s == storeys - 1 ? Room.BEDROOM : Room.HOUSE);
            if (r != null) {
                d.room(q, g + s * 4, r, h.furn(), seed(q) + s);
            }
        }
        return q;
    }

    /** A double door (lower halves; the placer adds the uppers) in the doorway tBuilding cut, with a transom. */
    static void doors(Builder b, int[] q, String side, String door) {
        int g = b.ground();
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        int ax;
        int az;
        int cx;
        int cz;
        switch (side) {
            case "north" -> {
                ax = mx;
                az = q[1];
                cx = mx + 1;
                cz = q[1];
            }
            case "south" -> {
                ax = mx;
                az = q[3];
                cx = mx + 1;
                cz = q[3];
            }
            case "east" -> {
                ax = q[2];
                az = mz;
                cx = q[2];
                cz = mz + 1;
            }
            default -> {
                ax = q[0];
                az = mz;
                cx = q[0];
                cz = mz + 1;
            }
        }
        String facing = EasternDetail.opposite(side);
        boolean aLeft = side.equals("south") || side.equals("west");
        b.set(ax, g, az, door + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "left" : "right") + ",open=false]");
        b.set(cx, g, cz, door + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "right" : "left") + ",open=false]");
        String trap = door.replace("_door", "_trapdoor");
        b.set(ax, g + 2, az, trap + "[half=top,facing=" + facing + "]");
        b.set(cx, g + 2, cz, trap + "[half=top,facing=" + facing + "]");
    }

    /**
     * A Roman portico in front of a building's door side: a row of columns two blocks out carrying an
     * entablature, a lean-to roof of {@code roofStairs}, steps up to it and lanterns.
     */
    static void portico(Builder b, int[] q, String side, String column, String beam, String roofStairs, int h) {
        int g = b.ground();
        boolean alongX = side.equals("north") || side.equals("south");
        int mid = alongX ? (q[0] + q[2]) / 2 : (q[1] + q[3]) / 2;
        int a = mid - 4;
        int c = mid + 5;
        int out = switch (side) {
            case "north" -> q[1] - 3;
            case "south" -> q[3] + 3;
            case "east" -> q[2] + 3;
            default -> q[0] - 3;
        };
        int wall = switch (side) {
            case "north" -> q[1];
            case "south" -> q[3];
            case "east" -> q[2];
            default -> q[0];
        };
        int step = out > wall ? 1 : -1;
        String face = EasternDetail.opposite(side);
        for (int p = a; p <= c; p += 3) {
            if (alongX) {
                b.fill(p, g, out, p, g + h - 1, out, column);
            } else {
                b.fill(out, g, p, out, g + h - 1, p, column);
            }
        }
        for (int o = wall + step; o != out + step; o += step) {
            if (alongX) {
                b.fill(a, g + h, o, c, g + h, o, beam);
            } else {
                b.fill(o, g + h, a, o, g + h, c, beam);
            }
        }
        if (alongX) {
            b.fill(a - 1, g + h + 1, out + step, c + 1, g + h + 1, out + step, roofStairs + "[facing=" + face + "]");
            b.fill(a - 1, g + h + 1, out, c + 1, g + h + 1, wall + step, roofStairs + "[facing=" + face + "]");
            b.fill(a, g - 1, wall + step, c, g - 1, out + step, "minecraft:smooth_quartz");
            b.fill(a + 1, g + h - 1, out - step, c - 1, g + h - 1, out - step, roofStairs + "[facing=" + side + ",half=top]");
            b.set(mid - 2, g + h - 1, wall + step, "minecraft:lantern[hanging=true]");
            b.set(mid + 3, g + h - 1, wall + step, "minecraft:lantern[hanging=true]");
        } else {
            b.fill(out + step, g + h + 1, a - 1, out + step, g + h + 1, c + 1, roofStairs + "[facing=" + face + "]");
            b.fill(out, g + h + 1, a - 1, wall + step, g + h + 1, c + 1, roofStairs + "[facing=" + face + "]");
            b.fill(wall + step, g - 1, a, out + step, g - 1, c, "minecraft:smooth_quartz");
            b.fill(out - step, g + h - 1, a + 1, out - step, g + h - 1, c - 1, roofStairs + "[facing=" + side + ",half=top]");
            b.set(wall + step, g + h - 1, mid - 2, "minecraft:lantern[hanging=true]");
            b.set(wall + step, g + h - 1, mid + 3, "minecraft:lantern[hanging=true]");
        }
    }

    /** A cypress: a tall narrow evergreen column. */
    static void cypress(Builder b, int x, int z, int h) {
        int g = b.ground();
        String leaf = "minecraft:spruce_leaves[persistent=true]";
        b.fill(x, g, z, x, g + 1, z, "minecraft:spruce_log");
        b.fill(x, g + 2, z, x, g + h - 1, z, leaf);
        b.fill(x - 1, g + 2, z, x + 1, g + h - 3, z, leaf);
        b.fill(x, g + 2, z - 1, x, g + h - 3, z + 1, leaf);
        b.set(x, g + h, z, leaf);
        b.fill(x, g + 2, z, x, g + h - 3, z, "minecraft:spruce_log");
    }

    /** A covered market stall over 3x3 tiles centred on (tx, ty) with a striped awning, counter and wares. */
    static void stall(Builder b, int tx, int ty, String station, String wool, String stripe) {
        int g = b.ground();
        int[] q = rect(tx - 1, ty - 1, tx + 1, ty + 1);
        for (int[] c : new int[][]{{q[0], q[1]}, {q[2], q[1]}, {q[0], q[3]}, {q[2], q[3]}}) {
            b.fill(c[0], g, c[1], c[0], g + 2, c[1], "minecraft:spruce_fence");
        }
        for (int x = q[0]; x <= q[2]; x++) {
            b.fill(x, g + 3, q[1], x, g + 3, q[3], (x - q[0]) % 2 == 0 ? wool : stripe);
        }
        b.fill(q[0] + 1, g + 4, q[1] + 1, q[2] - 1, g + 4, q[3] - 1, (q[2] - q[0]) % 4 == 0 ? wool : stripe);
        b.fill(q[0] + 1, g, q[3] - 1, q[2] - 1, g, q[3] - 1, "minecraft:spruce_slab[type=top]");
        b.set(q[0] + 1, g + 1, q[3] - 1, EasternDetail.goods(station, 0));
        b.set(q[2] - 1, g + 1, q[3] - 1, EasternDetail.goods(station, 1));
        b.set(q[0] + 1, g, q[1] + 1, "minecraft:barrel[facing=up]");
        b.set(q[2] - 1, g, q[1] + 1, "minecraft:barrel[facing=up]");
        b.set(q[2] - 1, g + 1, q[1] + 1, "minecraft:decorated_pot");
        b.set((q[0] + q[2]) / 2, g + 2, (q[1] + q[3]) / 2 - 1, "minecraft:lantern[hanging=true]");
        b.set(bx(tx), g, bz(ty), station);
    }

    /** Scatters {@code count} props of a theme over free cells of a tile rectangle (cells must be air-level ground). */
    static void props(Builder b, int[][] spots, String theme, long seed) {
        EasternDetail d = new EasternDetail(b);
        int g = b.ground();
        Random rnd = new Random(seed);
        for (int[] s : spots) {
            int x = bx(s[0]);
            int z = bz(s[1]);
            switch (theme) {
                case "harbour" -> {
                    switch (rnd.nextInt(4)) {
                        case 0 -> d.crates(x, z, rnd.nextLong());
                        case 1 -> {
                            b.set(x, g, z, "minecraft:barrel[facing=up]");
                            b.set(x + 1, g, z, "minecraft:barrel[facing=up]");
                            b.set(x, g + 1, z, "minecraft:barrel[facing=up]");
                        }
                        case 2 -> {
                            b.fill(x, g, z, x, g + 1, z, "minecraft:spruce_fence");
                            b.fill(x + 2, g, z, x + 2, g + 1, z, "minecraft:spruce_fence");
                            b.fill(x, g + 2, z, x + 2, g + 2, z, "minecraft:spruce_slab");
                            b.set(x + 1, g + 1, z, "minecraft:cobweb");
                        }
                        default -> {
                            b.set(x, g, z, "minecraft:cauldron");
                            b.set(x + 1, g, z, "minecraft:spruce_trapdoor[half=bottom]");
                        }
                    }
                }
                case "farm" -> {
                    switch (rnd.nextInt(4)) {
                        case 0 -> {
                            b.set(x, g, z, "minecraft:hay_block");
                            b.set(x + 1, g, z, "minecraft:hay_block");
                            b.set(x, g + 1, z, "minecraft:hay_block");
                        }
                        case 1 -> {
                            b.set(x, g, z, "minecraft:composter[level=4]");
                            b.set(x + 1, g, z, "minecraft:barrel[facing=up]");
                        }
                        case 2 -> {
                            b.set(x, g, z, "minecraft:pumpkin");
                            b.set(x + 1, g, z, "minecraft:carved_pumpkin[facing=south]");
                            b.set(x, g, z + 1, "minecraft:melon");
                        }
                        default -> d.crates(x, z, rnd.nextLong());
                    }
                }
                case "forge" -> {
                    switch (rnd.nextInt(3)) {
                        case 0 -> {
                            b.set(x, g, z, "minecraft:coal_block");
                            b.set(x + 1, g, z, "minecraft:raw_iron_block");
                            b.set(x, g + 1, z, "minecraft:coal_block");
                        }
                        case 1 -> {
                            b.fill(x, g, z, x + 2, g, z, "minecraft:rail[shape=east_west]");
                            b.set(x + 1, g, z + 1, "minecraft:raw_iron_block");
                        }
                        default -> d.crates(x, z, rnd.nextLong());
                    }
                }
                default -> {
                    switch (rnd.nextInt(4)) {
                        case 0 -> d.crates(x, z, rnd.nextLong());
                        case 1 -> d.bench(x, z, true, "south", "minecraft:spruce");
                        case 2 -> {
                            b.set(x, g, z, "minecraft:decorated_pot");
                            b.set(x + 1, g, z, "minecraft:potted_azalea_bush");
                        }
                        default -> d.flowerBed(x - 1, z - 1, x + 2, z + 2, "minecraft:stone_brick_slab[type=top]", rnd.nextLong());
                    }
                }
            }
        }
    }

}
