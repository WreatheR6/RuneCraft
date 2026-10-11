package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.world.MisthalinKit.Box;
import com.runecraft.osrs.content.world.MisthalinKit.Roof;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.runecraft.osrs.content.world.MisthalinKit.*;

/**
 * Lumbridge Castle at landmark detail. The keep stands west of the courtyard with its two great
 * front towers flanking the entrance (each holding a spiral staircase from the ground floor to the
 * battlements), four slate-capped corner towers, the north and south wings closing the courtyard and
 * an east curtain wall with a gatehouse. Inside: the dining hall and the Cook's kitchen (range,
 * cellar trapdoor) on the ground floor, Duke Horacio's chamber, the audience gallery and the spinning
 * room on the first floor, the bank and the archive on the top floor and a walkable crenellated roof
 * with the castle flag. Courtyard (3222, 3218) is block (0, 0) and stays open round the portal.
 */
final class MisthalinLumbridgeCastle {
    private MisthalinLumbridgeCastle() {
    }

    /** Storey height of the keep and towers (5 blocks of room plus the floor). */
    private static final int H = 6;
    private static final String WALL = STONE_BRICKS;
    private static final String TRIM = POLISHED_ANDESITE;
    private static final String SLATE_STAIRS = "minecraft:deepslate_tile_stairs";
    private static final String SLATE = "minecraft:deepslate_tiles";
    private static final String CANDLE = "minecraft:candle[candles=3,lit=true]";

    private static final Box KEEP = new Box(-40, -26, -17, 25);
    private static final Box NORTH_WING = new Box(-17, -26, 9, -15);
    private static final Box SOUTH_WING = new Box(-17, 14, 9, 25);
    /** Front towers flanking the entrance (centre x, centre z). */
    private static final int FX = -14;
    private static final int[] FZ = {-9, 8};
    /** The balcony over the entrance between the front towers. */
    private static final Box BALCONY = new Box(-16, -2, -10, 1);
    /** Keep corner towers. */
    private static final int[][] CORNERS = {{-40, -26}, {-40, 25}, {-17, -26}, {-17, 25}};
    /** Wing-end towers by the curtain wall. */
    private static final int[][] EAST_TOWERS = {{10, -21}, {10, 20}};

    static void build(MisthalinKit k) {
        shells(k);
        courtyardGround(k);
        towers(k);
        gatehouse(k);
        groundFloor(k);
        firstFloor(k);
        topFloor(k);
        roofs(k);
        wings(k);
        courtyard(k);
    }

    // ------------------------------------------------------------------ shells

    private static void shells(MisthalinKit k) {
        k.house(KEEP).storeys(3).height(H).wall(WALL).base(COBBLE).beam(TRIM).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(WALL).doorAt('e', -2, 4, 4).build();
        k.house(NORTH_WING).storeys(2).height(H).wall(WALL).base(COBBLE).beam(TRIM).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(WALL).doorAt('s', -6, 2, 3).doorAt('s', 2, 2, 3).build();
        k.house(SOUTH_WING).storeys(2).height(H).wall(WALL).base(COBBLE).beam(TRIM).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(WALL).doorAt('n', -6, 2, 3).doorAt('n', 2, 2, 3).build();
        // the keep's east wall is shared with the wings
        k.fill(-17, 0, -26, -17, 3 * H - 2, -15, WALL).fill(-17, 0, 14, -17, 3 * H - 2, 25, WALL);
        // inner cross walls on every storey: north rooms | great hall | south rooms
        for (int lv = 0; lv < 3 * H; lv += H) {
            k.fill(-39, lv, -14, -18, lv + H - 2, -14, WALL).clear(-29, lv, -14, -28, lv + 2, -14);
            k.fill(-39, lv, 14, -18, lv + H - 2, 14, WALL).clear(-29, lv, 14, -28, lv + 2, 14);
        }
        // stepped buttresses on the outer faces of the keep
        k.d.buttresses(-40, -19, -40, 19, 'w', 6, k.g, k.g + 3 * H - 2, WALL, "minecraft:stone_brick_stairs");
        k.d.buttresses(-33, -26, -24, -26, 'n', 4, k.g, k.g + 3 * H - 2, WALL, "minecraft:stone_brick_stairs");
        k.d.buttresses(-33, 25, -24, 25, 's', 4, k.g, k.g + 3 * H - 2, WALL, "minecraft:stone_brick_stairs");
    }

    // ------------------------------------------------------------------ towers

    private static void towers(MisthalinKit k) {
        Box[] solid = {KEEP, NORTH_WING, SOUTH_WING};
        // the four keep corner towers: slate cones, an attic under the cone reached by ladder
        for (int[] c : CORNERS) {
            int cx = c[0];
            int cz = c[1];
            int sx = cx < -30 ? 1 : -1;
            int sz = cz < 0 ? 1 : -1;
            coneTower(k, cx, cz, 6, 4 * H, solid, true);
            k.clear(cx + 6 * sx, 3 * H, cz + 2 * sz, cx + 6 * sx, 3 * H + 2, cz + 2 * sz); // door onto the keep roof
            if (cx == -17) {
                k.clear(cx + 6, 2 * H, cz + 2 * sz, cx + 6, 2 * H + 2, cz + 2 * sz); // door onto the wing roof
            }
            k.ladder(cx - 5 * sx, cz + sz, sx > 0 ? "east" : "west", 3 * H, 4 * H - 1);
            k.set(cx + 2 * sx, 4 * H, cz - 3 * sz, "minecraft:chest[facing=" + (sz > 0 ? "south" : "north") + "]");
            k.set(cx + 3 * sx, 4 * H, cz - 3 * sz, BARREL);
            k.set(cx, 3 * H, cz - 4 * sz, "minecraft:red_bed[facing=" + (sz > 0 ? "north" : "south") + ",part=foot]");
            k.set(cx + 1, 3 * H, cz - 3 * sz, "minecraft:chest[facing=" + (sz > 0 ? "south" : "north") + "]");
            k.set(cx, H, cz - 4 * sz, "minecraft:white_bed[facing=" + (sz > 0 ? "north" : "south") + ",part=foot]");
        }
        // the two great front towers flanking the entrance, with spiral stairs to the battlements
        for (int cz : FZ) {
            coneTower(k, FX, cz, 6, 5 * H, solid, false);
            spiral(k, FX, cz, 0, 3 * H, "minecraft:stone_brick_stairs", CHISELED);
            for (int lv = 0; lv <= 3 * H; lv += H) {
                k.clear(-20, lv, cz - 1, -20, lv + 2, cz); // doorway from the keep (or its roof)
                hang(k, FX + 3, lv + H - 2, cz + 3);
                hang(k, FX - 3, lv + H - 2, cz - 3);
            }
            // look-out room above the battlements
            k.ladder(FX + 5, cz + 1, "west", 3 * H, 4 * H - 1);
            hang(k, FX + 3, 5 * H - 2, cz - 3);
            hang(k, FX - 3, 5 * H - 2, cz + 3);
            k.set(FX - 3, 4 * H, cz + 3, "minecraft:chest[facing=east]");
            k.set(FX - 4, 4 * H, cz, "minecraft:cartography_table");
            k.set(FX + 3, 4 * H, cz - 3, BARREL);
            // the door from each tower onto the balcony over the entrance
            int bz = cz < 0 ? cz + 6 : cz - 6;
            k.clear(FX, H, bz, FX + 1, H + 2, bz);
        }
        // the wing-end towers stay open-topped: battlements, corbels and the castle flags
        for (int[] t : EAST_TOWERS) {
            k.tower(t[0], t[1], 6, 4 * H, H, WALL, SPRUCE_PLANKS, null, true);
            int sz = t[1] < 0 ? 1 : -1;
            k.clear(t[0] - 6, 2 * H, t[1], t[0] - 6, 2 * H + 2, t[1]);      // door onto the wing roof
            k.clear(9, H, t[1] + 6 * sz, 10, H + 2, t[1] + 6 * sz);           // door onto the curtain walk
            flag(k, t[0], 4 * H, t[1], 5);
            k.set(t[0] + 2, H, t[1] - sz, "minecraft:grindstone[face=floor,facing=north]");
            k.set(t[0] - 2, H, t[1] - 2 * sz, BARREL).set(t[0] - 3, H, t[1] - 2 * sz, BARREL);
            k.set(t[0] + 2, 2 * H, t[1] + 2 * sz, "minecraft:chest[facing=west]");
        }
    }

    /**
     * A round tower with floors every storey, tall windows on the faces that are not built against
     * the keep or a wing, a corbelled eave and a slate cone with a flag. {@code attic} keeps the top
     * floor closed (an attic reached by ladder) instead of the open stair well of the front towers.
     */
    private static void coneTower(MisthalinKit k, int cx, int cz, int r, int height, Box[] solid, boolean attic) {
        k.disc(cx, cz, r, -4, -1, WALL);
        k.disc(cx, cz, r - 1, -1, -1, POLISHED_ANDESITE);
        k.discClear(cx, cz, r - 1, 0, height - 1);
        k.ring(cx, cz, r, 1, 0, height - 1, WALL);
        for (int y = H - 1; y < height - 1; y += H) {
            k.disc(cx, cz, r - 1, y, y, SPRUCE_PLANKS);
        }
        k.disc(cx, cz, r - 1, height - 1, height - 1, SPRUCE_PLANKS);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 1; y + 2 < height - 1; y += H) {
            for (int[] d : dirs) {
                int px = cx + d[0] * r;
                int pz = cz + d[1] * r;
                if (inside(solid, px, pz) || y < 2 * H && inside(new Box[]{BALCONY}, px + d[0], pz + d[1])) {
                    continue;
                }
                k.connect(px, y, pz, px, y + 2, pz, GLASS);
                char out = d[0] > 0 ? 'e' : d[0] < 0 ? 'w' : d[1] > 0 ? 's' : 'n';
                k.d.castleWindow(px, k.g + y, k.g + y + 2, pz, out, WALL, CHISELED, k.g);
            }
        }
        if (attic) {
            for (int y = H - 2; y < height - 1; y += H) {
                hang(k, cx, y, cz);
            }
        }
        k.d.roundTower(cx, cz, r, k.g, k.g + height, H, WALL, false);
        // corbelled eave: upside-down stairs one block out under the cone
        Map<Long, String> eave = new LinkedHashMap<>();
        for (int dz = -r - 1; dz <= r + 1; dz++) {
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                if (in(dx, dz, r + 1) && !in(dx, dz, r)) {
                    eave.put(key(dx, dz), "minecraft:stone_brick_stairs[facing=" + inward(dx, dz) + ",half=top]");
                }
            }
        }
        emit(k, cx, cz, height - 1, eave);
        cone(k, cx, cz, r + 1, height, attic ? 1.6 : 1.8);
    }

    /** A slate cone: rings of deepslate tiles with stairs on every step, a finial and a flag. */
    private static void cone(MisthalinKit k, int cx, int cz, int radius, int y0, double rise) {
        int layers = (int) Math.ceil(radius * rise) + 1;
        int y = y0;
        for (int i = 0; i < layers; i++) {
            double ra = radius - i / rise;
            double rb = radius - (i + 1) / rise;
            if (ra < -0.2) {
                break;
            }
            Map<Long, String> m = new LinkedHashMap<>();
            int R = (int) Math.ceil(ra + 1);
            for (int dz = -R; dz <= R; dz++) {
                for (int dx = -R; dx <= R; dx++) {
                    if (!in(dx, dz, ra)) {
                        continue;
                    }
                    if (!in(dx, dz, rb)) {
                        m.put(key(dx, dz), ra < 0.6 ? SLATE : SLATE_STAIRS + "[facing=" + inward(dx, dz) + "]");
                    } else if (!in(dx, dz, rb - 1.6)) {
                        m.put(key(dx, dz), SLATE);
                    }
                }
            }
            emit(k, cx, cz, y, m);
            y++;
        }
        k.fill(cx, y, cz, cx, y, cz, "minecraft:polished_deepslate_wall");
        flag(k, cx, y + 1, cz, 3);
    }

    /** A flag pole of {@code pole} fences from y with a blue and yellow Lumbridge flag. */
    private static void flag(MisthalinKit k, int x, int y, int z, int pole) {
        k.fill(x, y, z, x, y + pole - 1, z, "minecraft:spruce_fence");
        k.set(x, y + pole, z, "minecraft:lightning_rod");
        k.fill(x + 1, y + pole - 2, z, x + 3, y + pole - 1, z, "minecraft:blue_wool");
        k.set(x + 2, y + pole - 1, z, "minecraft:yellow_wool").set(x + 2, y + pole - 2, z, "minecraft:yellow_wool");
    }

    /**
     * A spiral stair round a 3x3 core: steps on the 16-block square ring two blocks from the
     * centre, one block up per step, with headroom cut through the floors it passes.
     */
    private static void spiral(MisthalinKit k, int cx, int cz, int y0, int steps, String stairs, String core) {
        int[][] ring = new int[16][];
        int n = 0;
        for (int dz = 2; dz >= -2; dz--) {
            ring[n++] = new int[]{-2, dz};
        }
        for (int dx = -1; dx <= 2; dx++) {
            ring[n++] = new int[]{dx, -2};
        }
        for (int dz = -1; dz <= 2; dz++) {
            ring[n++] = new int[]{2, dz};
        }
        for (int dx = 1; dx >= -1; dx--) {
            ring[n++] = new int[]{dx, 2};
        }
        k.fill(cx - 1, y0, cz - 1, cx + 1, y0 + steps, cz + 1, core);
        k.fill(cx - 1, y0, cz - 1, cx + 1, y0, cz + 1, POLISHED_ANDESITE);
        for (int i = 0; i < steps; i++) {
            int[] a = ring[i % 16];
            int[] b = ring[(i + 1) % 16];
            String facing = b[0] > a[0] ? "east" : b[0] < a[0] ? "west" : b[1] > a[1] ? "south" : "north";
            int x = cx + a[0];
            int z = cz + a[1];
            k.clear(x, y0 + i + 1, z, x, y0 + i + 3, z);
            k.set(x, y0 + i, z, stairs + "[facing=" + facing + "]");
        }
    }

    private static boolean in(int dx, int dz, double r) {
        return r >= -0.4 && dx * dx + dz * dz <= (r + 0.5) * (r + 0.5);
    }

    private static String inward(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? "west" : "east";
        }
        return dz > 0 ? "north" : "south";
    }

    private static long key(int dx, int dz) {
        return ((long) dz << 32) ^ (dx & 0xffffffffL);
    }

    /** Writes a layer of cells (offsets from cx, cz) as row fills, merging equal neighbours along x. */
    private static void emit(MisthalinKit k, int cx, int cz, int y, Map<Long, String> cells) {
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (long key : cells.keySet()) {
            int dz = (int) (key >> 32);
            int dx = (int) key;
            minZ = Math.min(minZ, dz);
            maxZ = Math.max(maxZ, dz);
            minX = Math.min(minX, dx);
            maxX = Math.max(maxX, dx);
        }
        for (int dz = minZ; dz <= maxZ; dz++) {
            int dx = minX;
            while (dx <= maxX) {
                String b = cells.get(key(dx, dz));
                if (b == null) {
                    dx++;
                    continue;
                }
                int e = dx;
                while (e + 1 <= maxX && b.equals(cells.get(key(e + 1, dz)))) {
                    e++;
                }
                k.fill(cx + dx, y, cz + dz, cx + e, y, cz + dz, b);
                dx = e + 1;
            }
        }
    }

    private static boolean inside(Box[] boxes, int x, int z) {
        for (Box b : boxes) {
            if (x >= b.x1() && x <= b.x2() && z >= b.z1() && z <= b.z2()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ curtain wall and gatehouse

    private static void gatehouse(MisthalinKit k) {
        // east curtain wall between the wing towers, a wall walk at y 6 with an outer parapet
        k.fill(9, -1, -14, 11, H - 1, 13, WALL);
        k.d.textureSolid(9, k.g, -14, 11, k.g + H - 1, 13, WALL, 6);
        k.fill(11, H, -14, 11, H, 13, WALL);
        for (int z = -14; z <= 13; z += 2) {
            k.set(11, H + 1, z, WALL);
        }
        k.fill(12, H - 1, -14, 12, H - 1, 13, "minecraft:stone_brick_slab[type=top]");
        // the gatehouse: a vaulted passage, a gate room over it and two little slate-capped turrets
        Box gh = new Box(9, -6, 15, 5);
        k.fill(gh, -1, 9, WALL);
        k.clear(9, 0, -3, 15, 3, 2);
        k.fill(9, -1, -3, 15, -1, 2, POLISHED_ANDESITE);
        k.fill(15, 3, -3, 15, 3, 2, "minecraft:stone_brick_stairs[facing=east,half=top]");
        k.fill(9, 3, -3, 9, 3, 2, "minecraft:stone_brick_stairs[facing=west,half=top]");
        k.connect(12, 3, -3, 12, 3, 2, BARS); // the raised portcullis
        k.fill(15, 4, -4, 15, 4, 3, CHISELED);
        k.clear(10, H, -5, 14, H + 2, 4);
        k.clear(10, H, -6, 10, H + 2, -6).clear(10, H, 5, 10, H + 2, 5);
        k.carpet(new Box(11, -4, 13, 3), H, "blue");
        k.set(14, H, -5, "minecraft:chest[facing=west]").set(14, H, 4, BARREL);
        k.set(12, H + 2, -1, LANTERN_HANG);
        k.crenelRoof(gh, 9, WALL, WALL);
        flag(k, 12, 10, -1, 4);
        for (int z = -4; z <= 3; z += 7) {
            k.d.slit(15, k.g + H, k.g + H + 1, z == -4 ? -2 : 1, WALL);
            k.d.wallLantern(16, k.g + 2, z);
            k.set(8, 2, z, "minecraft:wall_torch[facing=west]");
        }
        for (int z : new int[]{-6, 5}) {
            k.disc(15, z, 2, -2, 11, WALL);
            k.d.textureSolid(13, k.g, z - 2, 17, k.g + 11, z + 2, WALL, 6);
            cone(k, 15, z, 3, 12, 1.4);
        }
        // steps up onto the wall walk from the courtyard
        k.stairsSolid(8, 0, 12, 'n', 6, "minecraft:stone_brick_stairs", WALL);
        k.stairsSolid(8, 0, -13, 's', 6, "minecraft:stone_brick_stairs", WALL);
    }

    // ------------------------------------------------------------------ ground floor

    private static void groundFloor(MisthalinKit k) {
        // ---- the great dining hall
        k.carpet(new Box(-35, -2, -18, 1), 0, "red");
        k.carpet(new Box(-35, -3, -18, -3), 0, "yellow").carpet(new Box(-35, 2, -18, 2), 0, "yellow");
        // the high table on its dais at the west end
        k.fill(-39, 0, -8, -37, 0, 7, DARK_PLANKS);
        k.fill(-36, 0, -8, -36, 0, 7, "minecraft:dark_oak_stairs[facing=west]");
        k.fill(-38, 1, -4, -38, 1, 3, "minecraft:dark_oak_slab[type=top]");
        k.fill(-39, 1, -4, -39, 1, 3, "minecraft:dark_oak_stairs[facing=west]");
        k.set(-39, 1, -1, "minecraft:gold_block").set(-39, 1, 0, "minecraft:gold_block");
        k.set(-38, 2, -3, CANDLE).set(-38, 2, 2, CANDLE).set(-38, 2, 0, "minecraft:cake");
        for (int z = -7; z <= 5; z += 3) {
            k.set(-39, 3, z, "minecraft:" + (Math.floorMod(z, 2) == 0 ? "yellow" : "blue") + "_wall_banner[facing=east]");
        }
        // two long tables with candles and chandeliers
        for (int z : new int[]{-7, 6}) {
            k.diningX(-33, -24, 0, z);
            for (int x = -32; x <= -24; x += 4) {
                k.set(x, 1, z, CANDLE);
            }
            chandelier(k, -31, 4, z);
            chandelier(k, -26, 4, z);
        }
        // the hearth in the north wall
        k.fill(-36, -1, -13, -32, 4, -13, "minecraft:bricks");
        k.clear(-35, 0, -13, -33, 1, -13);
        k.set(-34, 0, -13, "minecraft:campfire[lit=true]");
        k.fill(-36, 2, -12, -32, 2, -12, "minecraft:brick_slab[type=top]");
        k.set(-35, 3, -12, CANDLE).set(-33, 3, -12, CANDLE);
        k.set(-34, 3, -12, "minecraft:skeleton_skull[rotation=0]");
        k.fill(-36, -1, -12, -32, -1, -11, "minecraft:bricks");
        // sideboards, banners and the entrance armour
        k.shelvesX(-37, -31, 0, 13, "north");
        for (int x = -26; x <= -20; x += 3) {
            k.set(x, 3, 13, "minecraft:blue_wall_banner[facing=north]");
            k.set(x, 3, -13, "minecraft:blue_wall_banner[facing=south]");
        }
        k.set(-21, 0, -12, "minecraft:polished_blackstone_wall").set(-21, 1, -12, "minecraft:iron_block")
                .set(-21, 2, -12, "minecraft:skeleton_skull[rotation=0]");
        k.set(-21, 0, 12, "minecraft:polished_blackstone_wall").set(-21, 1, 12, "minecraft:iron_block")
                .set(-21, 2, 12, "minecraft:skeleton_skull[rotation=8]");
        hang(k, -20, 3, -1);
        hang(k, -20, 3, 0);
        k.set(-18, 0, -4, "minecraft:potted_azalea_bush").set(-18, 0, 3, "minecraft:potted_azalea_bush");

        // ---- the Cook's kitchen (south): range under a brick hood, sink, worktops, larder, cellar
        k.floor(new Box(-39, 15, -18, 24), "minecraft:bricks");
        k.floor(new Box(-34, 17, -23, 22), "minecraft:smooth_stone");
        k.fill(-39, 2, 24, -35, 4, 24, "minecraft:bricks");
        k.fill(-39, 2, 23, -35, 2, 23, "minecraft:brick_slab[type=top]");
        k.set(-38, 0, 24, RANGE).set(-37, 0, 24, "minecraft:smoker[facing=north]");
        k.set(-36, 0, 24, "minecraft:furnace[facing=north]").set(-35, 0, 24, "minecraft:blast_furnace[facing=north]");
        k.set(-39, 0, 24, "minecraft:water_cauldron[level=3]");
        k.fill(-39, 0, 16, -39, 0, 21, SPRUCE_PLANKS).fill(-39, 1, 16, -39, 1, 21, "minecraft:spruce_slab");
        k.set(-39, 1, 17, "minecraft:flower_pot").set(-39, 1, 19, "minecraft:cauldron");
        k.set(-39, 0, 22, "minecraft:composter").set(-39, 0, 15, CRAFTING);
        // the big prep table with bread and pies
        k.diningX(-33, -27, 0, 19);
        k.set(-32, 1, 19, "minecraft:cake").set(-29, 1, 19, CANDLE);
        k.set(-30, 0, 16, "minecraft:barrel[facing=up]").set(-31, 0, 16, "minecraft:hay_block");
        // larder against the south wall
        for (int x = -24; x <= -19; x++) {
            k.set(x, 0, 24, Math.floorMod(x, 2) == 0 ? BARREL : "minecraft:chest[facing=north]");
            k.set(x, 1, 24, Math.floorMod(x, 3) == 0 ? HAY : BARREL);
        }
        k.fill(-20, 0, 15, -19, 0, 16, HAY).set(-19, 1, 15, "minecraft:pumpkin").set(-20, 1, 16, "minecraft:melon");
        hang(k, -30, 4, 18);
        hang(k, -24, 4, 20);
        hang(k, -36, 4, 20);
        // cellar below the kitchen, down through a trapdoor
        k.fill(-30, -8, 19, -24, -2, 25, COBBLE);
        k.clear(-29, -7, 20, -25, -3, 24);
        k.ladder(-27, 24, "north", -7, -1);
        k.set(-27, -1, 24, "minecraft:spruce_trapdoor[half=top,facing=north]");
        k.set(-29, -7, 20, BARREL).set(-28, -7, 20, BARREL).set(-25, -7, 20, "minecraft:chest[facing=south]");
        k.set(-29, -7, 21, BARREL).set(-29, -6, 20, HAY);
        k.set(-29, -7, 23, "minecraft:cobweb").set(-25, -3, 22, "minecraft:cobweb").set(-28, -3, 24, "minecraft:cobweb");
        k.set(-27, -3, 21, LANTERN_HANG);

        // ---- north rooms: the castle stores and the guards' mess
        k.carpet(new Box(-36, -24, -24, -16), 0, "gray");
        k.diningX(-34, -28, 0, -20);
        k.set(-31, 1, -20, CANDLE);
        k.crates(-37, -24);
        k.crates(-37, -17);
        k.shelvesX(-30, -21, 0, -25, "south");
        k.set(-22, 0, -16, "minecraft:polished_blackstone_wall").set(-22, 1, -16, "minecraft:iron_block")
                .set(-22, 2, -16, "minecraft:skeleton_skull[rotation=8]");
        k.set(-20, 0, -16, "minecraft:grindstone[face=floor,facing=north]");
        hang(k, -31, 4, -20);
        hang(k, -24, 4, -20);
    }

    /** A lantern hung at y on a chain reaching up to the ceiling of its storey (floors every 6 blocks). */
    private static void hang(MisthalinKit k, int x, int y, int z) {
        int ceil = Math.floorDiv(y, H) * H + H - 1;
        if (y + 1 < ceil) {
            k.fillAir(x, y + 1, z, x, ceil - 1, z, "minecraft:chain");
        }
        k.set(x, y, z, LANTERN_HANG);
    }

    /** A wrought-iron chandelier: chain from the ceiling, a fence cross and four lanterns. */
    private static void chandelier(MisthalinKit k, int x, int y, int z) {
        k.set(x, y, z, "minecraft:chain");
        k.set(x, y - 1, z, "minecraft:dark_oak_fence");
        k.connect(x - 1, y - 1, z, x + 1, y - 1, z, "minecraft:dark_oak_fence");
        k.set(x - 1, y - 2, z, LANTERN_HANG).set(x + 1, y - 2, z, LANTERN_HANG);
        k.set(x, y - 2, z, LANTERN_HANG);
    }

    // ------------------------------------------------------------------ first floor

    private static void firstFloor(MisthalinKit k) {
        int y = H;
        // ---- Duke Horacio's chamber (north): throne dais, desk, bookcases, bed and wardrobe
        k.carpet(new Box(-36, -24, -19, -16), y, "blue");
        k.carpet(new Box(-35, -23, -20, -17), y, "light_blue");
        k.fill(-39, y, -23, -37, y, -17, DARK_PLANKS);
        k.fill(-36, y, -23, -36, y, -17, "minecraft:dark_oak_slab");
        k.set(-38, y + 1, -20, "minecraft:dark_oak_stairs[facing=west]");
        k.set(-38, y + 1, -21, "minecraft:dark_oak_trapdoor[facing=south,open=true]");
        k.set(-38, y + 1, -19, "minecraft:dark_oak_trapdoor[facing=north,open=true]");
        k.fill(-39, y + 1, -20, -39, y + 2, -20, "minecraft:gold_block");
        k.set(-39, y + 3, -20, "minecraft:yellow_wall_banner[facing=east]");
        k.set(-39, y + 3, -22, "minecraft:blue_wall_banner[facing=east]").set(-39, y + 3, -18, "minecraft:blue_wall_banner[facing=east]");
        k.set(-37, y + 1, -23, LANTERN).set(-37, y + 1, -17, LANTERN);
        // the desk with its papers, candle and a chair
        k.fill(-28, y, -18, -26, y, -18, "minecraft:dark_oak_slab[type=top]");
        k.set(-28, y + 1, -18, CANDLE).set(-26, y + 1, -18, "minecraft:flower_pot");
        k.set(-27, y, -19, "minecraft:dark_oak_stairs[facing=north]");
        k.set(-25, y, -18, "minecraft:lectern[facing=north]");
        k.fill(-34, y, -25, -27, y + 1, -25, BOOKSHELF);
        k.set(-30, y + 2, -25, "minecraft:candle[candles=2,lit=true]");
        // the duke's bed, chests and wardrobe
        k.bed(-21, y, -25, 's', "red");
        k.set(-22, y, -25, "minecraft:chest[facing=south]").set(-20, y, -25, "minecraft:chest[facing=south]");
        k.fill(-19, y, -25, -19, y + 1, -22, "minecraft:spruce_planks");
        k.set(-19, y, -23, BARREL).set(-19, y, -24, BARREL);
        chandelier(k, -28, y + 4, -20);

        // ---- the audience gallery (centre)
        k.carpet(new Box(-37, -12, -21, 12), y, "red");
        k.carpet(new Box(-37, -2, -21, 1), y, "yellow");
        k.diningZ(-33, y, -8, -4);
        k.diningZ(-33, y, 4, 8);
        for (int z : new int[]{-11, -8, -5, 4, 7, 10}) {
            k.set(-39, y + 2, z, (Math.floorMod(z, 2) == 0 ? "minecraft:yellow" : "minecraft:blue") + "_wall_banner[facing=east]");
        }
        for (int x = -37; x <= -24; x += 4) {
            k.set(x, y + 2, -13, "minecraft:red_wall_banner[facing=south]");
            k.set(x, y + 2, 13, "minecraft:red_wall_banner[facing=north]");
        }
        k.fill(-39, y, -2, -39, y + 1, 1, BOOKSHELF);
        k.set(-38, y, -2, "minecraft:polished_blackstone_wall").set(-38, y + 1, -2, "minecraft:iron_block")
                .set(-38, y + 2, -2, "minecraft:skeleton_skull[rotation=12]");
        k.set(-38, y, 1, "minecraft:polished_blackstone_wall").set(-38, y + 1, 1, "minecraft:iron_block")
                .set(-38, y + 2, 1, "minecraft:skeleton_skull[rotation=12]");
        chandelier(k, -33, y + 4, -6);
        chandelier(k, -33, y + 4, 6);
        chandelier(k, -26, y + 4, 0);
        k.d.bench(-25, k.g + y, -11, 'e', 3, 's', "spruce");
        k.d.bench(-25, k.g + y, 11, 'e', 3, 'n', "spruce");
        // the balcony over the entrance, between the front towers
        k.fill(-16, H - 1, -2, -10, H - 1, 1, WALL);
        k.fill(-16, H - 1, -2, -10, H - 1, 1, "minecraft:polished_andesite");
        k.connect(-10, H, -2, -10, H, 1, "minecraft:stone_brick_wall");
        k.fill(-16, H - 2, -2, -10, H - 2, -2, "minecraft:stone_brick_stairs[facing=north,half=top]");
        k.fill(-16, H - 2, 1, -10, H - 2, 1, "minecraft:stone_brick_stairs[facing=south,half=top]");
        k.fill(-10, H - 2, -2, -10, H - 2, 1, CHISELED);
        k.set(-9, H - 2, -2, "minecraft:blue_wall_banner[facing=east]").set(-9, H - 2, 1, "minecraft:yellow_wall_banner[facing=east]");
        k.set(-10, H + 1, -2, LANTERN).set(-10, H + 1, 1, LANTERN);
        hang(k, -13, H - 3, -1);
        hang(k, -13, H - 3, 0);

        // ---- the spinning room (south)
        k.carpet(new Box(-37, 16, -19, 23), y, "light_gray");
        k.set(-32, y, 22, SPINNING).set(-28, y, 22, SPINNING);
        k.set(-32, y, 23, "minecraft:white_wool").set(-28, y, 23, "minecraft:white_wool");
        k.fill(-33, y, 16, -27, y, 16, "minecraft:white_wool");
        k.fill(-33, y + 1, 16, -31, y + 1, 16, "minecraft:white_wool");
        k.set(-36, y, 22, "minecraft:loom[facing=east]").set(-36, y, 20, "minecraft:loom[facing=east]");
        k.fill(-24, y, 24, -20, y, 24, HAY);
        k.set(-22, y + 1, 24, HAY);
        k.set(-38, y, 24, "minecraft:chest[facing=north]").set(-37, y, 24, BARREL).set(-39, y, 24, BARREL);
        k.diningX(-25, -21, y, 19);
        k.set(-23, y + 1, 19, "minecraft:white_carpet");
        chandelier(k, -30, y + 4, 19);
        hang(k, -22, y + 3, 22);
        k.bed(-39, y, 17, 'e', "light_blue");
    }

    // ------------------------------------------------------------------ top floor

    private static void topFloor(MisthalinKit k) {
        int y = 2 * H;
        // ---- the Lumbridge bank: a counter of booths, the bankers' vault behind it
        k.carpet(new Box(-25, -12, -21, 12), y, "green");
        k.carpet(new Box(-39, -12, -27, 12), y, "lime");
        k.boothsZ(-26, y, -8, 8);
        k.fill(-26, y, -12, -26, y + 1, -9, POLISHED_ANDESITE).fill(-26, y, 9, -26, y + 1, 12, POLISHED_ANDESITE);
        k.fill(-26, y + 2, -12, -26, y + 2, 12, "minecraft:polished_andesite_slab[type=top]");
        for (int z = -11; z <= 11; z += 2) {
            k.set(-39, y, z, Math.floorMod(z, 4) == 1 ? "minecraft:chest[facing=east]" : BARREL);
        }
        k.fill(-39, y + 1, -11, -39, y + 1, 11, "minecraft:chiseled_bookshelf[facing=east]");
        k.set(-37, y, -12, "minecraft:gold_block").set(-36, y, -12, "minecraft:gold_block").set(-37, y + 1, -12, "minecraft:gold_block");
        k.set(-37, y, 12, "minecraft:iron_block").set(-36, y, 12, "minecraft:emerald_block");
        k.fill(-33, y, -2, -31, y, 1, "minecraft:spruce_slab[type=top]");
        k.set(-32, y + 1, -1, CANDLE).set(-32, y + 1, 0, "minecraft:flower_pot");
        k.set(-22, y, -12, "minecraft:lectern[facing=south]");
        k.d.bench(-23, k.g + y, 12, 'e', 3, 'n', "dark_oak");
        for (int z = -12; z <= 12; z += 6) {
            chandelier(k, -22, y + 4, z);
            chandelier(k, -33, y + 4, z);
        }
        // ---- the archive (north)
        for (int z = -24; z <= -18; z += 3) {
            k.fill(-36, y, z, -24, y + 2, z, BOOKSHELF);
            k.clear(-31, y, z, -30, y + 2, z);
        }
        k.fill(-39, y, -25, -37, y + 2, -25, BOOKSHELF);
        k.set(-21, y, -24, "minecraft:lectern[facing=south]").set(-21, y, -17, "minecraft:enchanting_table");
        k.fill(-23, y, -20, -20, y, -20, "minecraft:spruce_slab[type=top]");
        k.set(-22, y + 1, -20, CANDLE).set(-20, y + 1, -20, "minecraft:flower_pot");
        k.carpet(new Box(-36, -16, -20, -16), y, "brown");
        chandelier(k, -30, y + 4, -16);
        hang(k, -22, y + 3, -22);
        // ---- the bankers' quarters and strong room (south)
        k.carpet(new Box(-38, 16, -20, 23), y, "cyan");
        k.bed(-38, y, 23, 'n', "cyan").bed(-35, y, 23, 'n', "cyan").bed(-32, y, 23, 'n', "cyan");
        k.set(-37, y, 24, "minecraft:chest[facing=north]").set(-34, y, 24, "minecraft:chest[facing=north]");
        k.diningX(-27, -22, y, 18);
        k.fill(-21, y, 22, -19, y + 1, 24, "minecraft:iron_block");
        k.clear(-20, y, 23, -19, y + 1, 24);
        k.set(-20, y, 23, "minecraft:gold_block").set(-19, y, 24, "minecraft:chest[facing=west]");
        k.clear(-21, y, 23, -21, y + 1, 23);
        hang(k, -30, y + 3, 19);
        hang(k, -24, y + 3, 21);
    }

    // ------------------------------------------------------------------ roofs and battlements

    private static void roofs(MisthalinKit k) {
        int y = 3 * H;
        // weathered roof decks with a paved walk inside the parapets
        Box deck = KEEP.inset(1);
        k.walls(deck, y - 1, y - 1, POLISHED_ANDESITE);
        k.scatter(deck.inset(1), y - 1, CRACKED_BRICKS, 8);
        k.scatter(deck.inset(1), y - 1, MOSSY_BRICKS, 12);
        k.scatter(deck.inset(1), y - 1, ANDESITE, 16);
        for (Box w : new Box[]{NORTH_WING, SOUTH_WING}) {
            k.scatter(new Box(-11, w.z1() + 1, 3, w.z2() - 1), 2 * H - 1, CRACKED_BRICKS, 7);
            k.scatter(new Box(-11, w.z1() + 1, 3, w.z2() - 1), 2 * H - 1, MOSSY_BRICKS, 10);
        }
        // the castle flag on the keep roof
        k.fill(-30, y, -2, -26, y, 2, CHISELED);
        k.fill(-28, y + 1, 0, -28, y + 9, 0, "minecraft:spruce_fence");
        k.set(-28, y + 10, 0, "minecraft:lightning_rod");
        k.fill(-27, y + 6, 0, -22, y + 9, 0, "minecraft:blue_wool");
        k.fill(-26, y + 7, 0, -23, y + 8, 0, "minecraft:yellow_wool");
        k.set(-29, y + 1, -1, "minecraft:stone_brick_wall").set(-27, y + 1, 1, "minecraft:stone_brick_wall");
        // chimney stacks over the kitchen and the hearth, and lanterns along the walk
        k.fill(-38, y, 22, -37, y + 2, 23, "minecraft:bricks");
        k.set(-38, y + 3, 22, "minecraft:campfire[lit=true]").set(-37, y + 3, 23, "minecraft:campfire[lit=true]");
        k.fill(-35, y, -14, -33, y + 2, -14, "minecraft:bricks");
        k.set(-34, y + 3, -14, "minecraft:campfire[lit=true]");
        for (int z = -16; z <= 16; z += 8) {
            k.set(-39, y, z, LANTERN);
            k.set(-21, y, z + 4, LANTERN);
        }
        // smaller flags on the wing parapets
        flag(k, -4, 2 * H + 1, -26, 4);
        flag(k, -4, 2 * H + 1, 25, 4);
        // banners down the courtyard faces of the wings and the keep front
        for (int x = -10; x <= 6; x += 4) {
            if (x != -6 && x != 2) {
                k.d.banner(x, k.g + 9, -14, 's', Math.floorMod(x, 8) == 2 ? "yellow" : "blue");
                k.d.banner(x, k.g + 9, 13, 'n', Math.floorMod(x, 8) == 2 ? "yellow" : "blue");
            }
        }
    }

    // ------------------------------------------------------------------ wings

    private static void wings(MisthalinKit k) {
        // north wing: armoury below, guard quarters above
        for (int x = -12; x <= 3; x += 3) {
            k.set(x, 0, -25, "minecraft:polished_blackstone_wall").set(x, 1, -25, "minecraft:iron_block")
                    .set(x, 2, -25, "minecraft:skeleton_skull[rotation=0]");
            k.set(x + 1, 0, -25, BARREL);
            k.set(x + 1, 3, -25, "minecraft:white_wall_banner[facing=south]");
        }
        k.fill(-9, 0, -20, -3, 0, -20, SPRUCE_PLANKS).fill(-9, 1, -20, -3, 1, -20, "minecraft:spruce_slab");
        k.set(-6, 0, -20, "minecraft:smithing_table").set(-4, 0, -20, "minecraft:grindstone[face=floor,facing=north]");
        k.set(-10, 0, -17, "minecraft:anvil[facing=east]");
        k.ladder(-1, -25, "south", 0, H - 1);
        k.ladder(5, -16, "north", H, 2 * H - 1);
        k.carpet(new Box(-12, -24, 2, -17), H, "cyan");
        k.bed(-9, H, -25, 's', "cyan").bed(-6, H, -25, 's', "cyan").bed(-3, H, -25, 's', "cyan").bed(-12, H, -25, 's', "cyan");
        k.set(-8, H, -25, "minecraft:chest[facing=south]").set(-5, H, -25, "minecraft:chest[facing=south]");
        k.diningX(-9, -4, H, -19);
        // south wing: barracks below, the guards' hall above
        for (int x = -12; x <= 3; x += 3) {
            k.bed(x, 0, 24, 'n', "blue");
            k.set(x + 1, 0, 24, "minecraft:chest[facing=north]");
        }
        k.diningX(-9, -4, 0, 18);
        k.ladder(-1, 24, "north", 0, H - 1);
        k.ladder(5, 15, "south", H, 2 * H - 1);
        k.carpet(new Box(-12, 16, 2, 23), H, "brown");
        k.diningX(-9, -2, H, 20);
        k.set(-12, H, 24, "minecraft:target").set(-11, H, 24, "minecraft:target");
        k.set(2, H, 24, "minecraft:fletching_table").set(1, H, 24, BARREL);
        for (int x = -9; x <= 3; x += 6) {
            hang(k, x, H - 2, -20);
            hang(k, x, 2 * H - 2, -20);
            hang(k, x, H - 2, 19);
            hang(k, x, 2 * H - 2, 19);
        }
    }

    // ------------------------------------------------------------------ courtyard

    private static void courtyardGround(MisthalinKit k) {
        Box yard = new Box(-16, -14, 8, 13);
        k.floor(yard, GRASS);
        k.clear(new Box(-7, -14, 8, 13), 0, 3);
        k.floor(new Box(-16, -3, 22, 2), STONE_BRICKS);
        k.scatter(new Box(-16, -3, 14, 2), -1, CRACKED_BRICKS, 10);
        k.scatter(new Box(-16, -3, 14, 2), -1, MOSSY_BRICKS, 12);
        k.floor(new Box(-1, -11, 1, -4), POLISHED_ANDESITE);
        k.floor(new Box(-1, 3, 1, 12), POLISHED_ANDESITE);
        k.floor(new Box(-6, -14, -5, -4), GRAVEL).floor(new Box(2, -14, 3, -12), GRAVEL);
        k.floor(new Box(-6, 3, -5, 13), GRAVEL).floor(new Box(2, 11, 3, 13), GRAVEL);
    }

    private static void courtyard(MisthalinKit k) {
        // flower beds edged with stone slabs
        Box[] beds = {new Box(-4, -11, -2, -6), new Box(3, -11, 7, -6), new Box(-4, 6, -2, 10), new Box(3, 6, 7, 10)};
        String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:allium",
                "minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:red_tulip"};
        int f = 0;
        for (Box b : beds) {
            k.floor(b, "minecraft:rooted_dirt");
            k.walls(b, 0, 0, "minecraft:stone_brick_slab");
            for (int z = b.z1() + 1; z <= b.z2() - 1; z++) {
                k.fill(b.x1() + 1, 0, z, b.x2() - 1, 0, z, flowers[f++ % flowers.length]);
            }
            k.set(b.cx(), 0, b.cz(), "minecraft:flowering_azalea");
        }
        // trees in the corners, benches by the path, lamps and the portal
        k.oak(-6, -12);
        k.oak(6, -13);
        k.oak(-6, 12);
        k.oak(6, 12);
        k.d.bench(3, k.g, -4, 'e', 4, 's', "spruce");
        k.d.bench(-4, k.g, -4, 'e', 2, 's', "spruce");
        k.d.bench(3, k.g, 3, 'e', 4, 'n', "spruce");
        k.d.bench(-4, k.g, 3, 'e', 2, 'n', "spruce");
        k.lampPost(-7, -4).lampPost(-7, 3).lampPost(8, -4).lampPost(8, 3);
        k.d.signpost(6, k.g, 4 + 1, "spruce");
        // the Gielinor portal stands against the north wing
        k.fill(-1, -1, -14, 1, -1, -12, CHISELED);
        k.set(0, 0, -13, PORTAL);
        k.set(-1, 0, -13, "minecraft:stone_brick_wall").set(1, 0, -13, "minecraft:stone_brick_wall");
        k.set(-1, 1, -13, LANTERN).set(1, 1, -13, LANTERN);
        k.d.banner(0, k.g + 4, -14, 's', "blue");
        // flower tubs at the keep door and the gate
        k.d.planter(-9, k.g, -3, 0);
        k.d.planter(-9, k.g, 2, 1);
        k.d.planter(16, k.g, -5, 2);
        k.d.planter(16, k.g, 4, 3);
    }
}
