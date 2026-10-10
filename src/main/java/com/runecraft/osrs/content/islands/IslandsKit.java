package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.content.world.EasternDetail;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;
import com.runecraft.osrs.world.StructureDef.Op;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/** Shared building helpers for the island structures (tile coordinates unless noted). */
final class IslandsKit {
    private IslandsKit() {
    }

    /** Absolute block height of the floor of the caves carved beneath cave entrances. */
    static final int CAVE_FLOOR_Y = 38;
    /** Steps in a stairwell from the surface down to a cave floor (one block north per step). */
    static final int STAIR_STEPS = StructureDef.GROUND - 2 - CAVE_FLOOR_Y;

    // Palette.
    static final String GRASS = "minecraft:grass_block";
    static final String SAND = "minecraft:sand";
    static final String PATH = "minecraft:dirt_path";
    static final String COBBLE = "minecraft:cobblestone";
    static final String MOSSY = "minecraft:mossy_cobblestone";
    static final String STONE = "minecraft:stone_bricks";
    static final String MOSSY_STONE = "minecraft:mossy_stone_bricks";
    static final String CRACKED = "minecraft:cracked_stone_bricks";
    static final String ANDESITE = "minecraft:polished_andesite";
    static final String SNOW = "minecraft:snow_block";
    static final String SPRUCE = "minecraft:spruce_planks";
    static final String SPRUCE_LOG = "minecraft:stripped_spruce_log";
    static final String OAK = "minecraft:oak_planks";
    static final String JUNGLE = "minecraft:jungle_planks";
    static final String JUNGLE_LOG = "minecraft:jungle_log";
    static final String BAMBOO = "minecraft:bamboo_block";
    static final String MUD = "minecraft:mud";
    static final String PACKED_MUD = "minecraft:packed_mud";
    static final String MUD_BRICKS = "minecraft:mud_bricks";
    static final String COARSE = "minecraft:coarse_dirt";
    static final String PODZOL = "minecraft:podzol";
    static final String BASALT = "minecraft:smooth_basalt";
    static final String BLACKSTONE = "minecraft:polished_blackstone_bricks";
    static final String DEEPSLATE = "minecraft:deepslate_bricks";
    static final String SANDSTONE = "minecraft:smooth_sandstone";
    static final String CUT_SANDSTONE = "minecraft:cut_sandstone";
    static final String PRISMARINE = "minecraft:prismarine_bricks";
    static final String QUARTZ = "minecraft:quartz_bricks";
    static final String PURPUR = "minecraft:purpur_block";

    static String rc(String id) {
        return "runecraft:" + id;
    }

    /** Block X of the centre of the chunk a place at OSRS x is built in (as {@code PlaceDef.blockX()}). */
    static int centreX(int osrsX) {
        return ((bx(osrsX) >> 4) << 4) + 8;
    }

    static int centreZ(int osrsY) {
        return ((bz(osrsY) >> 4) << 4) + 8;
    }

    /** Block offset (dx) of tile x from the centre of a place at OSRS x {@code placeX}. */
    static int dx(int tileX, int placeX) {
        return bx(tileX) - centreX(placeX);
    }

    static int dz(int tileY, int placeY) {
        return bz(tileY) - centreZ(placeY);
    }

    // ------------------------------------------------------------------ detailed buildings

    /** The facade look for an island building by its wall material (per-island styles). */
    static EasternDetail.Look lookFor(String wall) {
        return switch (wall) {
            // Fremennik timber: log walls on a stone footing, dark beams, carved gables
            case SPRUCE_LOG -> new EasternDetail.Look(SPRUCE_LOG, COBBLE, "minecraft:spruce_log", "minecraft:spruce_log", false,
                    "minecraft:spruce_stairs", "minecraft:spruce_trapdoor", SPRUCE, COBBLE, "minecraft:lantern", null, false,
                    "minecraft:dark_oak", false);
            case MOSSY_STONE -> new EasternDetail.Look(MOSSY_STONE, MOSSY, STONE, "minecraft:mossy_cobblestone", false,
                    "minecraft:mossy_stone_brick_stairs", "minecraft:spruce_trapdoor", SPRUCE, COBBLE, "minecraft:lantern", null, true,
                    "minecraft:mossy_cobblestone", true);
            case STONE -> new EasternDetail.Look(STONE, ANDESITE, "minecraft:chiseled_stone_bricks", "minecraft:polished_andesite",
                    false, "minecraft:stone_brick_stairs", null, null, STONE, "minecraft:lantern", null, true, null, false);
            case OAK -> new EasternDetail.Look(OAK, COBBLE, "minecraft:stripped_oak_log", "minecraft:oak_log", true,
                    "minecraft:oak_stairs", "minecraft:oak_trapdoor", null, COBBLE, "minecraft:lantern", null, true, "minecraft:spruce",
                    false);
            case "minecraft:spruce_planks" -> new EasternDetail.Look(SPRUCE, COBBLE, SPRUCE_LOG, "minecraft:spruce_log", true,
                    "minecraft:spruce_stairs", "minecraft:spruce_trapdoor", null, COBBLE, "minecraft:lantern", null, true, null, true);
            case "minecraft:dark_oak_planks" -> new EasternDetail.Look("minecraft:dark_oak_planks", COBBLE, "minecraft:dark_oak_log",
                    "minecraft:dark_oak_log", true, "minecraft:dark_oak_stairs", "minecraft:dark_oak_trapdoor", null, COBBLE,
                    "minecraft:lantern", null, true, "minecraft:spruce", true);
            case "minecraft:bricks" -> new EasternDetail.Look("minecraft:bricks", STONE, "minecraft:stripped_spruce_log",
                    "minecraft:spruce_log", false, "minecraft:brick_stairs", "minecraft:spruce_trapdoor", null, "minecraft:bricks",
                    "minecraft:lantern", null, true, null, false);
            // Ape Atoll jungle wood: bamboo posts and thatch-patched roofs
            case JUNGLE -> new EasternDetail.Look(JUNGLE, MUD_BRICKS, "minecraft:stripped_jungle_log", "minecraft:bamboo_block", true,
                    "minecraft:jungle_stairs", "minecraft:bamboo_trapdoor", "minecraft:bamboo_planks", null, "minecraft:lantern", null,
                    true, "minecraft:bamboo", false);
            case BLACKSTONE -> new EasternDetail.Look(BLACKSTONE, "minecraft:blackstone", "minecraft:polished_blackstone",
                    "minecraft:gilded_blackstone", false, "minecraft:polished_blackstone_brick_stairs", null, null, null,
                    "minecraft:soul_lantern", null, true, null, false);
            default -> new EasternDetail.Look(wall, null, null, null, false, "minecraft:spruce_stairs", null, null, null,
                    "minecraft:lantern", null, true, null, false);
        };
    }

    static EasternDetail.Furniture furnitureFor(String wall) {
        if (wall.equals(JUNGLE)) {
            return new EasternDetail.Furniture("minecraft:jungle", "minecraft:yellow_carpet", "minecraft:lime_carpet", "minecraft:yellow_bed",
                    "minecraft:lantern", false);
        }
        if (wall.equals(SPRUCE_LOG)) {
            return new EasternDetail.Furniture("minecraft:spruce", "minecraft:white_carpet", "minecraft:gray_carpet", "minecraft:white_bed",
                    "minecraft:lantern", false);
        }
        if (wall.equals(MOSSY_STONE) || wall.equals(BLACKSTONE)) {
            return new EasternDetail.Furniture("minecraft:dark_oak", "minecraft:gray_carpet", "minecraft:black_carpet", "minecraft:gray_bed",
                    "minecraft:lantern", true);
        }
        return new EasternDetail.Furniture("minecraft:oak", "minecraft:red_carpet", "minecraft:brown_carpet", "minecraft:red_bed",
                "minecraft:lantern", false);
    }

    /**
     * {@link Builder#tBuilding} plus the detail kit: a dressed facade, roof detail with chimney, a real door
     * where the wall material has a wood to make it from, a hanging light, and a furnished room in single
     * storey pitched buildings (multi-storey and flat-roofed ones keep their interiors for the caller).
     */
    static int[] building(Builder b, int tx1, int ty1, int tx2, int ty2, int storeys, String wall, String floor, String roof,
                          String door, boolean pitched) {
        b.tBuilding(tx1, ty1, tx2, ty2, storeys, wall, floor, roof, door, pitched);
        EasternDetail d = new EasternDetail(b);
        int g = b.ground();
        int[] q = {bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1};
        int top = g + storeys * 4;
        EasternDetail.Look l = lookFor(wall);
        d.facade(q, storeys, l, door, "minecraft:glass_pane");
        long seed = q[0] * 31L + q[1] * 17L + q[2];
        if (pitched) {
            b.fill(q[0] + 1, top - 1, q[1] + 1, q[2] - 1, top - 1, q[3] - 1, floor);
            d.roof(q, top, roof, l, door, seed);
            if (wall.equals(SPRUCE_LOG)) {
                // Fremennik crossed bargeboards (dragon horns) at the gable peaks
                boolean alongX = (q[2] - q[0]) >= (q[3] - q[1]);
                int depth = alongX ? q[3] - q[1] : q[2] - q[0];
                int ridge = top + (depth + 2) / 2;
                int c = alongX ? (q[1] + q[3]) / 2 : (q[0] + q[2]) / 2;
                for (int e : alongX ? new int[]{q[0] - 1, q[2] + 1} : new int[]{q[1] - 1, q[3] + 1}) {
                    if (alongX) {
                        b.fill(e, ridge, c - 1, e, ridge + 1, c - 1, "minecraft:spruce_fence");
                        b.fill(e, ridge, c + 2, e, ridge + 1, c + 2, "minecraft:spruce_fence");
                    } else {
                        b.fill(c - 1, ridge, e, c - 1, ridge + 1, e, "minecraft:spruce_fence");
                        b.fill(c + 2, ridge, e, c + 2, ridge + 1, e, "minecraft:spruce_fence");
                    }
                }
            }
        }
        String doorBlock = switch (wall) {
            case SPRUCE_LOG, "minecraft:spruce_planks" -> "minecraft:spruce_door";
            case OAK -> "minecraft:oak_door";
            case JUNGLE -> "minecraft:jungle_door";
            case "minecraft:dark_oak_planks", MOSSY_STONE -> "minecraft:dark_oak_door";
            default -> null;
        };
        if (door != null && doorBlock != null) {
            int mx = (q[0] + q[2]) / 2;
            int mz = (q[1] + q[3]) / 2;
            int[] a = switch (door) {
                case "north" -> new int[]{mx, q[1], mx + 1, q[1]};
                case "south" -> new int[]{mx, q[3], mx + 1, q[3]};
                case "east" -> new int[]{q[2], mz, q[2], mz + 1};
                default -> new int[]{q[0], mz, q[0], mz + 1};
            };
            String facing = EasternDetail.opposite(door);
            boolean aLeft = door.equals("south") || door.equals("west");
            b.set(a[0], g, a[1], doorBlock + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "left" : "right") + ",open=false]");
            b.set(a[2], g, a[3], doorBlock + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "right" : "left") + ",open=false]");
        }
        b.set((q[0] + q[2]) / 2, g + 2, (q[1] + q[3]) / 2, "minecraft:lantern[hanging=true]");
        if (storeys == 1 && pitched) {
            d.room(q, g, EasternDetail.Room.HOUSE, furnitureFor(wall), seed);
        }
        return q;
    }

    // ------------------------------------------------------------------ small pieces

    static void lamp(Builder b, int tx, int ty, String post) {
        b.tSet(tx, ty, 0, post);
        b.tSet(tx, ty, 1, post);
        b.tSet(tx, ty, 2, post);
        b.tSet(tx, ty, 3, "minecraft:lantern");
    }

    static void torchPost(Builder b, int tx, int ty, String post) {
        b.tSet(tx, ty, 0, post);
        b.tSet(tx, ty, 1, "minecraft:torch");
    }

    static void crenellate(Builder b, int tx1, int ty1, int tx2, int ty2, int y, String block) {
        if (tx1 == tx2) {
            for (int t = Math.min(ty1, ty2); t <= Math.max(ty1, ty2); t += 2) {
                b.tSet(tx1, t, y, block);
            }
        } else {
            for (int t = Math.min(tx1, tx2); t <= Math.max(tx1, tx2); t += 2) {
                b.tSet(t, ty1, y, block);
            }
        }
    }

    /** A wall line with crenellations along the top. */
    static void wall(Builder b, int tx1, int ty1, int tx2, int ty2, int height, String block) {
        b.tWallLine(tx1, ty1, tx2, ty2, height, block);
        crenellate(b, tx1, ty1, tx2, ty2, height, block);
    }

    /** A simple tree: a trunk and a leafy crown. */
    static void tree(Builder b, int tx, int ty, String log, String leaves, int height) {
        b.tSet(tx, ty, -1, "minecraft:dirt");
        b.tFill(tx - 1, ty - 1, tx + 1, ty + 1, height - 2, height, leaves);
        for (int y = 0; y < height; y++) {
            b.tSet(tx, ty, y, log);
        }
    }

    /** A palm: a tall jungle trunk with a cross of fronds. */
    static void palm(Builder b, int tx, int ty, int height) {
        int x = bx(tx);
        int z = bz(ty);
        b.set(x, b.ground() - 1, z, "minecraft:dirt");
        new EasternDetail(b).palm(x, z, height, tx * 31L + ty);
    }

    /** A giant mushroom (Fossil Island style): a stem and a wide cap. */
    static void mushroom(Builder b, int tx, int ty, int height, int radius, String cap) {
        int x = bx(tx);
        int z = bz(ty);
        int g = b.ground();
        b.fill(x, g, z, x + 1, g + height - 1, z + 1, "minecraft:mushroom_stem");
        b.fill(x - radius + 1, g + height, z - radius + 1, x + radius, g + height, z + radius, cap);
        b.fill(x - radius + 2, g + height + 1, z - radius + 2, x + radius - 1, g + height + 1, z + radius - 1, cap);
        b.walls(x - radius, g + height - 1, z - radius, x + radius + 1, g + height - 1, z + radius + 1, cap);
    }

    /**
     * A round hut centred on a tile: a ring of {@code wall} two/three blocks high with a doorway on the
     * given side and a conical roof of {@code roof} (thatch: hay, bamboo, wool...).
     */
    static void hut(Builder b, int tx, int ty, int r, String wall, String roof, String floor, String door) {
        int cx = bx(tx) + 1;
        int cz = bz(ty) + 1;
        int g = b.ground();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) {
                    continue;
                }
                b.set(cx + dx, g - 1, cz + dz, floor);
                if (d > r - 0.7) {
                    b.fill(cx + dx, g, cz + dz, cx + dx, g + 2, cz + dz, wall);
                } else {
                    b.clear(cx + dx, g, cz + dz, cx + dx, g + 2, cz + dz);
                }
            }
        }
        for (int layer = 0; layer <= r + 1; layer++) {
            int rr = r + 1 - layer;
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= rr + 0.5 && d > rr - 1.2) {
                        b.set(cx + dx, g + 3 + layer, cz + dz, roof);
                    }
                }
            }
        }
        switch (door) {
            case "north" -> b.clear(cx, g, cz - r, cx, g + 1, cz - r);
            case "south" -> b.clear(cx, g, cz + r, cx, g + 1, cz + r);
            case "east" -> b.clear(cx + r, g, cz, cx + r, g + 1, cz);
            default -> b.clear(cx - r, g, cz, cx - r, g + 1, cz);
        }
        // Door poles with a bone/skull charm, a sleeping mat, a water pot, a fire pit and a light.
        int ox = door.equals("east") ? 1 : door.equals("west") ? -1 : 0;
        int oz = door.equals("south") ? 1 : door.equals("north") ? -1 : 0;
        int dx = cx + ox * (r + 1);
        int dz = cz + oz * (r + 1);
        String pole = wall.contains("jungle") ? "minecraft:bamboo_fence" : "minecraft:spruce_fence";
        b.fill(dx + oz, g, dz + ox, dx + oz, g + 2, dz + ox, pole);
        b.fill(dx - oz, g, dz - ox, dx - oz, g + 2, dz - ox, pole);
        if (r >= 3) {
            b.set(cx - ox * (r - 1) + oz, g, cz - oz * (r - 1) + ox, "minecraft:yellow_carpet");
            b.set(cx - ox * (r - 1), g, cz - oz * (r - 1), "minecraft:yellow_carpet");
            b.set(cx + oz * (r - 1), g, cz + ox * (r - 1), "minecraft:decorated_pot");
            b.set(cx - oz * (r - 1), g, cz - ox * (r - 1), "minecraft:barrel[facing=up]");
        }
        b.set(cx, g + 2, cz, "minecraft:lantern[hanging=true]");
    }

    /** A ridge tent of wool over a tile rectangle (ridge along x), open at the west and east ends. */
    static void tent(Builder b, int tx1, int ty1, int tx2, int ty2, String wool) {
        int x1 = bx(Math.min(tx1, tx2));
        int x2 = bx(Math.max(tx1, tx2)) + 1;
        int z1 = bz(Math.max(ty1, ty2));
        int z2 = bz(Math.min(ty1, ty2)) + 1;
        int g = b.ground();
        b.fill(x1, g - 1, z1, x2, g - 1, z2, "minecraft:spruce_planks");
        for (int i = 0; z1 + i <= z2 - i; i++) {
            b.fill(x1, g + i, z1 + i, x2, g + i, z1 + i, wool);
            b.fill(x1, g + i, z2 - i, x2, g + i, z2 - i, wool);
        }
        b.set(x1, g, (z1 + z2) / 2, "minecraft:spruce_fence");
        b.set(x2, g, (z1 + z2) / 2, "minecraft:spruce_fence");
        // ridge pole out past both ends, guy ropes to pegs, a camp bed and a crate inside
        int mz = (z1 + z2) / 2;
        int ridge = g + (z2 - z1) / 2;
        b.fill(x1 - 1, ridge + 1, mz, x2 + 1, ridge + 1, mz, "minecraft:spruce_log[axis=x]");
        b.fill(x1 - 1, g, mz, x1 - 1, ridge, mz, "minecraft:spruce_fence");
        b.fill(x2 + 1, g, mz, x2 + 1, ridge, mz, "minecraft:spruce_fence");
        b.set(x1, g, z1 - 1, "minecraft:spruce_fence");
        b.set(x2, g, z1 - 1, "minecraft:spruce_fence");
        b.set(x1, g, z2 + 1, "minecraft:spruce_fence");
        b.set(x2, g, z2 + 1, "minecraft:spruce_fence");
        if (x2 - x1 >= 4 && z2 - z1 >= 4) {
            b.set(x1 + 2, g, mz, "minecraft:white_bed[facing=east,part=foot]");
            b.set(x2 - 1, g, mz, "minecraft:barrel[facing=up]");
            b.set(x2 - 1, g + 1, mz, "minecraft:lantern");
        }
    }

    /** A wooden pier deck one block above the sea with posts down into the water. */
    static void pier(Builder b, int tx1, int ty1, int tx2, int ty2, String deck) {
        b.tFill(tx1, ty1, tx2, ty2, -1, -1, deck);
        int xa = Math.min(tx1, tx2);
        int xb = Math.max(tx1, tx2);
        int ya = Math.min(ty1, ty2);
        int yb = Math.max(ty1, ty2);
        for (int x = xa; x <= xb; x += 3) {
            for (int y = ya; y <= yb; y += 3) {
                if (x == xa || x == xb || y == ya || y == yb) {
                    b.tSet(x, y, -8, "minecraft:spruce_log");
                    b.tFill(x, y, x, y, -7, -2, "minecraft:spruce_log");
                }
            }
        }
        for (int x = xa; x <= xb; x += 2) {
            b.tSet(x, ya, 0, "minecraft:spruce_fence");
            b.tSet(x, yb, 0, "minecraft:spruce_fence");
        }
        // bollards with rope and lanterns on posts at the ends, a few crates
        for (int x = xa; x <= xb; x += 6) {
            b.tSet(x, ya, 1, "minecraft:spruce_fence");
            b.tSet(x, ya, 2, "minecraft:lantern");
            b.tSet(x, yb, 1, "minecraft:spruce_slab");
        }
        b.tSet(xb, (ya + yb) / 2, 0, "minecraft:barrel[facing=up]");
        b.tSet(xb - 1, (ya + yb) / 2, 0, "minecraft:spruce_planks");
    }

    /** A ship hull moored along x (bow to the east), with a mast; deck at relative height 0. */
    static void ship(Builder b, int tx1, int ty, int length, String hull, String sail) {
        int x1 = bx(tx1);
        int cz = bz(ty) + 1;
        int g = b.ground();
        int len = length * 2;
        for (int i = 0; i < len; i++) {
            int half = i < 3 ? 1 : (i > len - 5 ? Math.max(0, (len - 1 - i) / 2) : 3);
            int x = x1 + i;
            b.fill(x, g - 3, cz - half, x, g - 3, cz + half, hull);
            b.fill(x, g - 2, cz - half - 1, x, g, cz - half - 1, hull);
            b.fill(x, g - 2, cz + half + 1, x, g, cz + half + 1, hull);
            b.fill(x, g - 1, cz - half, x, g - 1, cz + half, "minecraft:spruce_planks");
            b.clear(x, g, cz - half, x, g + 2, cz + half);
        }
        int mx = x1 + len / 2;
        b.fill(mx, g, cz, mx, g + 9, cz, "minecraft:spruce_log");
        b.fill(mx, g + 4, cz - 3, mx, g + 8, cz + 3, sail);
        b.set(x1 + len, g, cz, hull);
        // yard, crow's nest, pennant, gunwale rail, stern cabin, bowsprit, lanterns and cargo
        b.fill(mx, g + 9, cz - 4, mx, g + 9, cz + 4, "minecraft:spruce_log[axis=z]");
        b.fill(mx, g + 6, cz - 3, mx, g + 6, cz + 3, sail.contains("white") ? "minecraft:red_wool" : "minecraft:white_wool");
        b.walls(mx - 1, g + 10, cz - 1, mx + 1, g + 10, cz + 1, "minecraft:spruce_fence");
        b.fill(mx, g + 10, cz, mx, g + 12, cz, "minecraft:spruce_fence");
        b.set(mx + 1, g + 12, cz, "minecraft:red_wool");
        for (int i = 3; i < len - 4; i++) {
            b.set(x1 + i, g + 1, cz - 4, "minecraft:spruce_fence");
            b.set(x1 + i, g + 1, cz + 4, "minecraft:spruce_fence");
        }
        b.fill(x1 + 1, g, cz - 1, x1 + 3, g + 1, cz + 1, "minecraft:spruce_planks");
        b.set(x1 + 3, g + 1, cz, "minecraft:spruce_trapdoor[facing=east,open=true]");
        b.fill(x1 + 1, g + 2, cz - 2, x1 + 3, g + 2, cz + 2, "minecraft:spruce_slab");
        b.set(x1 + 2, g + 3, cz, "minecraft:lantern");
        b.fill(x1 + len, g + 1, cz, x1 + len, g + 2, cz, "minecraft:spruce_fence");
        b.set(mx + 3, g, cz + 1, "minecraft:barrel[facing=up]");
        b.set(mx + 3, g, cz - 1, "minecraft:barrel[facing=up]");
        b.set(mx - 3, g, cz, "minecraft:chest[facing=east]");
    }

    /** Broken walls of a ruined building: crumbled to varying heights with gaps. */
    static void ruins(Builder b, int tx1, int ty1, int tx2, int ty2, int height, String block, String rubble) {
        int xa = Math.min(tx1, tx2);
        int xb = Math.max(tx1, tx2);
        int ya = Math.min(ty1, ty2);
        int yb = Math.max(ty1, ty2);
        int i = 0;
        for (int x = xa; x <= xb; x++, i++) {
            int h = 1 + (i * 7 + x) % height;
            if (i % 5 != 3) {
                b.tFill(x, ya, x, ya, 0, h - 1, block);
            }
            if (i % 4 != 1) {
                b.tFill(x, yb, x, yb, 0, Math.max(0, height - h) , block);
            }
        }
        i = 0;
        for (int y = ya; y <= yb; y++, i++) {
            int h = 1 + (i * 5 + y) % height;
            if (i % 6 != 2) {
                b.tFill(xa, y, xa, y, 0, h - 1, block);
            }
            if (i % 3 != 0) {
                b.tFill(xb, y, xb, y, 0, Math.max(0, height - 1 - h), block);
            }
        }
        b.tFloor(xa + 1, ya + 1, xb - 1, yb - 1, rubble);
        EasternDetail d = new EasternDetail(b);
        String[] mats = {block, "minecraft:cobblestone", "minecraft:mossy_cobblestone"};
        d.rubble(bx(xa + 1) + 1, bz(ya + 1), mats, xa * 7L + ya);
        d.rubble(bx(xb - 1), bz(yb - 1) + 1, mats, xb * 5L + yb);
        d.overgrowth(bx(xa), bz(yb), bx(xb) + 1, bz(ya) + 1, Math.max(4, (xb - xa) * (yb - ya) / 2), xa + yb);
    }

    /** A fairy ring: the ring block on mycelium, ringed by mushrooms. */
    static void fairyRing(Builder b, int[] ring) {
        b.tFloor(ring[0] - 1, ring[1] - 1, ring[0] + 1, ring[1] + 1, "minecraft:mycelium");
        b.tSet(ring[0], ring[1], 0, rc(IslandsBlocks.FAIRY_RING));
        for (int[] m : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {0, -1}, {0, 1}}) {
            b.tSet(ring[0] + m[0], ring[1] + m[1], 0, m[0] == 0 ? "minecraft:red_mushroom" : "minecraft:brown_mushroom");
        }
    }

    /** A boulder cluster on a tile. */
    static void rocks(Builder b, int tx, int ty, String block) {
        b.tSetTile(tx, ty, 0, block);
        b.tSet(tx, ty, 1, block);
    }

    /** A Fremennik longhall: log walls, pitched spruce roof, doorway at the south. */
    static void longhall(Builder b, int tx1, int ty1, int tx2, int ty2, String door) {
        building(b, tx1, ty1, tx2, ty2, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", door, true);
    }

    /** A row of crop/flower blocks over a tile rectangle (on farmland). */
    static void field(Builder b, int tx1, int ty1, int tx2, int ty2, String crop) {
        b.tFloor(tx1, ty1, tx2, ty2, "minecraft:farmland");
        b.tFill(tx1, ty1, tx2, ty2, 0, 0, crop);
    }

    /** A fence ring around a tile rectangle with a gate on the south side. */
    static void pen(Builder b, int tx1, int ty1, int tx2, int ty2, String fence) {
        int x1 = bx(Math.min(tx1, tx2));
        int x2 = bx(Math.max(tx1, tx2)) + 1;
        int z1 = bz(Math.max(ty1, ty2));
        int z2 = bz(Math.min(ty1, ty2)) + 1;
        int g = b.ground();
        b.walls(x1, g, z1, x2, g, z2, fence);
        b.clear((x1 + x2) / 2, g, z2, (x1 + x2) / 2 + 1, g, z2);
    }

    /**
     * A stairwell from the surface at tile (tx, ty) going north down to {@link #CAVE_FLOOR_Y}, and a
     * cave room at its foot ({@code halfX}/{@code halfZ} blocks either side of the camp centre). The camp
     * centre (block coordinates) is {@link #caveCentreX}/{@link #caveCentreZ}.
     */
    static void cave(Builder b, int tx, int ty, int halfX, int halfZ, String wall, String floor) {
        int sx = bx(tx);
        int sz = bz(ty) + 1;
        int f = CAVE_FLOOR_Y;
        int g = b.ground();
        int cx = caveCentreX(tx);
        int cz = caveCentreZ(ty, halfZ);
        int x1 = cx - halfX;
        int x2 = cx + halfX;
        int z1 = cz - halfZ;
        int z2 = cz + halfZ;
        b.fill(x1, f, z1, x2, f + 7, z2, wall);
        b.clear(x1 + 1, f + 1, z1 + 1, x2 - 1, f + 6, z2 - 1);
        b.fill(x1 + 1, f, z1 + 1, x2 - 1, f, z2 - 1, floor);
        for (int x = x1 + 5; x < x2 - 3; x += 9) {
            for (int z = z1 + 5; z < z2 - 3; z += 9) {
                b.fill(x, f + 1, z, x, f + 6, z, wall);
                b.set(x + 1, f + 1, z, "minecraft:soul_lantern");
            }
        }
        for (int i = 0; i < STAIR_STEPS; i++) {
            int y = g - 2 - i;
            int z = sz - i;
            b.walls(sx - 1, y - 1, z, sx + 2, y + 4, z, wall);
            b.clear(sx, y + 1, z, sx + 1, y + 4, z);
            b.fill(sx, y, z, sx + 1, y, z, "minecraft:stone_brick_stairs[facing=south]");
        }
        // Cave mouth: an arch of rock over the top of the stairs.
        b.fill(sx - 1, g, sz + 1, sx + 2, g + 3, sz + 1, wall);
        b.clear(sx, g, sz + 1, sx + 1, g + 2, sz + 1);
        b.fill(sx - 1, g, sz - 2, sx - 1, g + 3, sz + 1, wall);
        b.fill(sx + 2, g, sz - 2, sx + 2, g + 3, sz + 1, wall);
        b.fill(sx - 1, g + 3, sz - 2, sx + 2, g + 3, sz + 1, wall);
        b.clear(sx, g - 1, sz - 1, sx + 1, g + 2, sz);
    }

    static int caveCentreX(int tx) {
        return bx(tx) + 1;
    }

    /** The cave room's centre lies north of the stairwell's foot. */
    static int caveCentreZ(int ty, int halfZ) {
        return bz(ty) + 1 - (STAIR_STEPS - 1) - halfZ + 2;
    }

    // ------------------------------------------------------------------ clipping

    /** A rectangle of blocks (inclusive) another module owns: a plaza, a boss arena or a structure. */
    record Zone(String owner, int x1, int z1, int x2, int z2) {
        boolean intersects(int ax1, int az1, int ax2, int az2) {
            return ax2 >= x1 && ax1 <= x2 && az2 >= z1 && az1 <= z2;
        }
    }

    /** True for operations entirely below the surface (caves), which never touch other plazas. */
    static boolean underground(Op op) {
        return op.type() != Op.Type.FLATTEN && op.y2() < StructureDef.GROUND - 1;
    }

    /**
     * Rebuilds a structure without any part of an operation (or a flatten's blend ring) that would
     * reach into a foreign zone. Normally nothing is clipped (the layouts already leave every plaza
     * and arena alone - see the module test); this is a safety net for places other modules add later.
     * Returns null when nothing would be left.
     */
    static StructureDef clip(StructureDef def, List<Zone> zones) {
        boolean touched = false;
        for (Op op : def.ops()) {
            if (!underground(op) && hits(op, zones)) {
                touched = true;
                break;
            }
        }
        if (!touched) {
            return def;
        }
        Builder b = StructureDef.builder(def.id(), def.name());
        if (def.replacesLandmark() != null) {
            b.replaces(def.replacesLandmark());
        }
        int kept = 0;
        for (Op op : def.ops()) {
            if (underground(op) || !hits(op, zones)) {
                b.op(op.type(), op.x1(), op.y1(), op.z1(), op.x2(), op.y2(), op.z2(), op.block(), op.extra());
                kept++;
                continue;
            }
            List<Op> parts = new ArrayList<>();
            if (op.type() == Op.Type.WALLS) {
                parts.add(new Op(Op.Type.FILL, op.x1(), op.y1(), op.z1(), op.x2(), op.y2(), op.z1(), op.block(), null));
                parts.add(new Op(Op.Type.FILL, op.x1(), op.y1(), op.z2(), op.x2(), op.y2(), op.z2(), op.block(), null));
                parts.add(new Op(Op.Type.FILL, op.x1(), op.y1(), op.z1(), op.x1(), op.y2(), op.z2(), op.block(), null));
                parts.add(new Op(Op.Type.FILL, op.x2(), op.y1(), op.z1(), op.x2(), op.y2(), op.z2(), op.block(), null));
            } else {
                parts.add(op);
            }
            for (Zone z : zones) {
                List<Op> next = new ArrayList<>();
                for (Op p : parts) {
                    next.addAll(subtract(p, z));
                }
                parts = next;
            }
            for (Op p : parts) {
                b.op(p.type(), p.x1(), p.y1(), p.z1(), p.x2(), p.y2(), p.z2(), p.block(), p.extra());
                kept++;
            }
        }
        return kept == 0 ? null : b.build();
    }

    private static boolean hits(Op op, List<Zone> zones) {
        int m = op.margin();
        for (Zone z : zones) {
            if (z.intersects(op.x1() - m, op.z1() - m, op.x2() + m, op.z2() + m)) {
                return true;
            }
        }
        return false;
    }

    /** The parts of an operation outside a zone (grown by the operation's margin). */
    private static List<Op> subtract(Op op, Zone zone) {
        int m = op.margin();
        int hx1 = zone.x1() - m;
        int hz1 = zone.z1() - m;
        int hx2 = zone.x2() + m;
        int hz2 = zone.z2() + m;
        if (op.x2() < hx1 || op.x1() > hx2 || op.z2() < hz1 || op.z1() > hz2) {
            return List.of(op);
        }
        List<Op> out = new ArrayList<>();
        if (op.x1() < hx1) {
            out.add(part(op, op.x1(), op.z1(), hx1 - 1, op.z2()));
        }
        if (op.x2() > hx2) {
            out.add(part(op, hx2 + 1, op.z1(), op.x2(), op.z2()));
        }
        int xa = Math.max(op.x1(), hx1);
        int xb = Math.min(op.x2(), hx2);
        if (op.z1() < hz1) {
            out.add(part(op, xa, op.z1(), xb, hz1 - 1));
        }
        if (op.z2() > hz2) {
            out.add(part(op, xa, hz2 + 1, xb, op.z2()));
        }
        return out;
    }

    private static Op part(Op op, int x1, int z1, int x2, int z2) {
        return new Op(op.type(), x1, op.y1(), z1, x2, op.y2(), z2, op.block(), op.extra());
    }
}
