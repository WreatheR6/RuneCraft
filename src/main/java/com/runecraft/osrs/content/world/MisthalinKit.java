package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Building kit shared by the Misthalin structures: block-level primitives with heights relative to
 * the ground, tile rectangles, houses with storeys/doors/windows/roofs, round towers, stairs, ladders,
 * trees, fences, rivers and furniture.
 *
 * <p>Blocks are placed with neighbour post-processing but without neighbour updates, so connecting
 * blocks (fences, panes, bars, walls) are laid twice, and attached blocks (ladders, torches, carpets,
 * plants) are always placed after what supports them. Doors and beds cannot survive that and are not
 * used: doorways are left open.
 */
final class MisthalinKit {
    // ---- palettes ----
    static final String AIR = "minecraft:air";
    static final String STONE_BRICKS = "minecraft:stone_bricks";
    static final String MOSSY_BRICKS = "minecraft:mossy_stone_bricks";
    static final String CRACKED_BRICKS = "minecraft:cracked_stone_bricks";
    static final String CHISELED = "minecraft:chiseled_stone_bricks";
    static final String COBBLE = "minecraft:cobblestone";
    static final String MOSSY_COBBLE = "minecraft:mossy_cobblestone";
    static final String ANDESITE = "minecraft:andesite";
    static final String POLISHED_ANDESITE = "minecraft:polished_andesite";
    static final String SMOOTH_STONE = "minecraft:smooth_stone";
    static final String STONE = "minecraft:stone";
    static final String GRAVEL = "minecraft:gravel";
    static final String DIRT_PATH = "minecraft:dirt_path";
    static final String COARSE_DIRT = "minecraft:coarse_dirt";
    static final String GRASS = "minecraft:grass_block";
    static final String DIRT = "minecraft:dirt";
    static final String MUD = "minecraft:mud";
    static final String WATER = "minecraft:water";
    static final String GLASS = "minecraft:glass_pane";
    static final String BARS = "minecraft:iron_bars";
    static final String SPRUCE_PLANKS = "minecraft:spruce_planks";
    static final String OAK_PLANKS = "minecraft:oak_planks";
    static final String DARK_PLANKS = "minecraft:dark_oak_planks";
    static final String BIRCH_PLANKS = "minecraft:birch_planks";
    static final String SPRUCE_LOG = "minecraft:spruce_log";
    static final String OAK_LOG = "minecraft:oak_log";
    static final String DARK_LOG = "minecraft:dark_oak_log";
    static final String STRIPPED_SPRUCE = "minecraft:stripped_spruce_log";
    static final String STRIPPED_DARK = "minecraft:stripped_dark_oak_log";
    static final String WHITE_PLASTER = "minecraft:white_terracotta";
    static final String OAK_LEAVES = "minecraft:oak_leaves[persistent=true]";
    static final String DARK_LEAVES = "minecraft:dark_oak_leaves[persistent=true]";
    static final String SPRUCE_LEAVES = "minecraft:spruce_leaves[persistent=true]";
    static final String BIRCH_LEAVES = "minecraft:birch_leaves[persistent=true]";
    static final String JUNGLE_LEAVES = "minecraft:jungle_leaves[persistent=true]";
    static final String LANTERN_HANG = "minecraft:lantern[hanging=true]";
    static final String LANTERN = "minecraft:lantern";
    static final String BOOKSHELF = "minecraft:bookshelf";
    static final String BARREL = "minecraft:barrel[facing=up]";
    static final String CRAFTING = "minecraft:crafting_table";
    static final String HAY = "minecraft:hay_block";

    // ---- runecraft stations ----
    static final String BANK = "runecraft:bank_booth";
    static final String FURNACE = "runecraft:furnace";
    static final String ANVIL = "runecraft:anvil";
    static final String RANGE = "runecraft:cooking_range";
    static final String ALTAR = "runecraft:prayer_altar";
    static final String GENERAL = "runecraft:general_store";
    static final String RUNE_SHOP = "runecraft:rune_shop";
    static final String WEAPON_SHOP = "runecraft:weapon_shop";
    static final String TOOL_SHOP = "runecraft:tool_shop";
    static final String ARCHERY_SHOP = "runecraft:archery_shop";
    static final String SPINNING = "runecraft:spinning_wheel";
    static final String TANNER = "runecraft:tanning_rack";
    static final String WORKBENCH = "runecraft:construction_workbench";
    static final String PORTAL = "runecraft:gielinor_portal";
    static final String SLAYER = "runecraft:slayer_master";

    /** A roof material: stairs, full block for gables and ridge, slab for the ridge cap. */
    record Roof(String stairs, String full, String slab) {
        static final Roof SLATE = new Roof("minecraft:deepslate_tile_stairs", "minecraft:deepslate_tiles", "minecraft:deepslate_tile_slab");
        static final Roof SPRUCE = new Roof("minecraft:spruce_stairs", SPRUCE_PLANKS, "minecraft:spruce_slab");
        static final Roof DARK_OAK = new Roof("minecraft:dark_oak_stairs", DARK_PLANKS, "minecraft:dark_oak_slab");
        static final Roof OAK = new Roof("minecraft:oak_stairs", OAK_PLANKS, "minecraft:oak_slab");
        static final Roof BRICK = new Roof("minecraft:brick_stairs", "minecraft:bricks", "minecraft:brick_slab");
        static final Roof STONE = new Roof("minecraft:stone_brick_stairs", STONE_BRICKS, "minecraft:stone_brick_slab");
        static final Roof BLACKSTONE = new Roof("minecraft:polished_blackstone_brick_stairs", "minecraft:polished_blackstone_bricks",
                "minecraft:polished_blackstone_brick_slab");
        static final Roof MUD = new Roof("minecraft:mud_brick_stairs", "minecraft:mud_bricks", "minecraft:mud_brick_slab");
        static final Roof THATCH = new Roof("minecraft:spruce_stairs", HAY, "minecraft:spruce_slab");
        static final Roof COBBLE = new Roof("minecraft:cobblestone_stairs", "minecraft:cobblestone", "minecraft:cobblestone_slab");
    }

    /** An inclusive block rectangle in the XZ plane. */
    record Box(int x1, int z1, int x2, int z2) {
        Box inset(int n) {
            return new Box(x1 + n, z1 + n, x2 - n, z2 - n);
        }

        Box grow(int n) {
            return inset(-n);
        }

        int cx() {
            return (x1 + x2) / 2;
        }

        int cz() {
            return (z1 + z2) / 2;
        }

        int w() {
            return x2 - x1 + 1;
        }

        int d() {
            return z2 - z1 + 1;
        }
    }

    final StructureDef.Builder b;
    final int g;
    final Random rnd;
    /** Deferred finishing details (textures, shutters, chimneys...), appended on {@link #build}. */
    final MisthalinDetail d;

    MisthalinKit(String id, String name) {
        this(id, name, null);
    }

    MisthalinKit(String id, String name, String replaces) {
        b = StructureDef.builder(id, name);
        if (replaces != null) {
            b.replaces(replaces);
        }
        g = b.ground();
        rnd = new Random(id.hashCode());
        d = new MisthalinDetail(id);
    }

    StructureDef build() {
        d.flush(b);
        return b.build();
    }

    // ---------------- tile coordinates ----------------

    /** West block of tile x. */
    static int X(int tx) {
        return bx(tx);
    }

    /** North block of tile y. */
    static int Z(int ty) {
        return bz(ty);
    }

    /** Block rectangle covering a tile rectangle (any corner order). */
    static Box t(int tx1, int ty1, int tx2, int ty2) {
        return new Box(bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1);
    }

    // ---------------- primitives (y relative to ground) ----------------

    MisthalinKit fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.fill(x1, g + y1, z1, x2, g + y2, z2, block);
        return this;
    }

    MisthalinKit fill(Box r, int y1, int y2, String block) {
        return fill(r.x1, y1, r.z1, r.x2, y2, r.z2, block);
    }

    MisthalinKit walls(Box r, int y1, int y2, String block) {
        b.walls(r.x1, g + y1, r.z1, r.x2, g + y2, r.z2, block);
        return this;
    }

    MisthalinKit clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        b.clear(x1, g + y1, z1, x2, g + y2, z2);
        return this;
    }

    MisthalinKit clear(Box r, int y1, int y2) {
        return clear(r.x1, y1, r.z1, r.x2, y2, r.z2);
    }

    MisthalinKit set(int x, int y, int z, String block) {
        b.set(x, g + y, z, block);
        return this;
    }

    MisthalinKit fillAir(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.op(StructureDef.Op.Type.FILL_AIR, x1, g + y1, z1, x2, g + y2, z2, block, null);
        return this;
    }

    MisthalinKit replace(Box r, int y1, int y2, String from, String to) {
        b.replace(r.x1, g + y1, r.z1, r.x2, g + y2, r.z2, from, to);
        return this;
    }

    /** Fill twice so connecting blocks (fences, panes, bars, walls) join to each other. */
    MisthalinKit connect(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        fill(x1, y1, z1, x2, y2, z2, block);
        return fill(x1, y1, z1, x2, y2, z2, block);
    }

    /** Ground surface (y = -1) over a block rectangle. */
    MisthalinKit floor(Box r, String block) {
        return fill(r, -1, -1, block);
    }

    MisthalinKit flatten(Box r) {
        b.flatten(r.x1, r.z1, r.x2, r.z2, GRASS);
        return this;
    }

    MisthalinKit flatten(Box r, String top) {
        b.flatten(r.x1, r.z1, r.x2, r.z2, top);
        return this;
    }

    /** Flatten a tile rectangle with grass. */
    MisthalinKit tFlatten(int tx1, int ty1, int tx2, int ty2) {
        b.tFlatten(tx1, ty1, tx2, ty2, GRASS);
        return this;
    }

    /** Ground surface over a tile rectangle. */
    MisthalinKit tFloor(int tx1, int ty1, int tx2, int ty2, String block) {
        return floor(t(tx1, ty1, tx2, ty2), block);
    }

    /** Scatter single blocks over a rectangle at height y (about one per {@code every} blocks). */
    MisthalinKit scatter(Box r, int y, String block, int every) {
        int n = Math.max(1, r.w() * r.d() / every);
        for (int i = 0; i < n; i++) {
            set(r.x1 + rnd.nextInt(r.w()), y, r.z1 + rnd.nextInt(r.d()), block);
        }
        return this;
    }

    /** A cobbled street: cobblestone with patches of andesite, stone and gravel. */
    MisthalinKit street(Box r) {
        clear(r, 0, 0);
        floor(r, COBBLE);
        scatter(r, -1, ANDESITE, 9);
        scatter(r, -1, STONE, 14);
        scatter(r, -1, GRAVEL, 30);
        scatter(r, -1, MOSSY_COBBLE, 25);
        return this;
    }

    /** A worn dirt path with ragged edges. */
    MisthalinKit dirtPath(Box r) {
        clear(r, 0, 0);
        floor(r, DIRT_PATH);
        scatter(r, -1, COARSE_DIRT, 12);
        scatter(r, -1, GRAVEL, 40);
        return this;
    }

    /** A dirt path along a tile segment (horizontal or vertical) {@code wTiles} tiles wide. */
    MisthalinKit tPath(int tx1, int ty1, int tx2, int ty2, int wTiles) {
        return dirtPath(tLine(tx1, ty1, tx2, ty2, wTiles));
    }

    MisthalinKit tStreet(int tx1, int ty1, int tx2, int ty2, int wTiles) {
        return street(tLine(tx1, ty1, tx2, ty2, wTiles));
    }

    /** A tile segment widened to {@code wTiles} tiles (widening east/south of the line). */
    static Box tLine(int tx1, int ty1, int tx2, int ty2, int wTiles) {
        if (ty1 == ty2) {
            return t(tx1, ty1, tx2, ty1 - (wTiles - 1));
        }
        return t(tx1, ty1, tx1 + wTiles - 1, ty2);
    }

    // ---------------- roofs ----------------

    /**
     * A pitched roof over a block rectangle, eaves overhanging one block, the first layer at
     * relative height y, ridge along the longer axis. Gable ends are filled with {@code gable}.
     */
    MisthalinKit roof(Box r, int y, Roof m, String gable) {
        int x1 = r.x1, z1 = r.z1, x2 = r.x2, z2 = r.z2;
        String gab = gable == null ? m.full() : gable;
        boolean alongX = (x2 - x1) >= (z2 - z1);
        int span = alongX ? z2 - z1 : x2 - x1;
        String gableLog = MisthalinDetail.isStone(gab) ? null : m == Roof.SLATE || m == Roof.DARK_OAK ? STRIPPED_DARK : STRIPPED_SPRUCE;
        d.gables(x1, z1, x2, z2, g + y, alongX, gab, gableLog, GLASS);
        int layers = span / 2 + 2;
        for (int i = 0; i < layers; i++) {
            int yy = y + i;
            int a = (alongX ? z1 : x1) - 1 + i;
            int c = (alongX ? z2 : x2) + 1 - i;
            if (a > c) {
                break;
            }
            if (a == c) {
                if (alongX) {
                    fill(x1 - 1, yy, a, x2 + 1, yy, a, m.full());
                    d.air(x1 - 1, g + yy + 1, a, x2 + 1, g + yy + 1, a, m.slab());
                } else {
                    fill(a, yy, z1 - 1, a, yy, z2 + 1, m.full());
                    d.air(a, g + yy + 1, z1 - 1, a, g + yy + 1, z2 + 1, m.slab());
                }
                break;
            }
            if (alongX) {
                fill(x1 - 1, yy, a, x2 + 1, yy, a, m.stairs() + "[facing=south]");
                fill(x1 - 1, yy, c, x2 + 1, yy, c, m.stairs() + "[facing=north]");
                if (a + 1 <= c - 1) {
                    fill(x1, yy, a + 1, x1, yy, c - 1, gab);
                    fill(x2, yy, a + 1, x2, yy, c - 1, gab);
                    if (i == 0) {
                        fill(x1, yy, a + 1, x2, yy, a + 1, gab);
                        fill(x1, yy, c - 1, x2, yy, c - 1, gab);
                    }
                }
            } else {
                fill(a, yy, z1 - 1, a, yy, z2 + 1, m.stairs() + "[facing=east]");
                fill(c, yy, z1 - 1, c, yy, z2 + 1, m.stairs() + "[facing=west]");
                if (a + 1 <= c - 1) {
                    fill(a + 1, yy, z1, c - 1, yy, z1, gab);
                    fill(a + 1, yy, z2, c - 1, yy, z2, gab);
                    if (i == 0) {
                        fill(a + 1, yy, z1, a + 1, yy, z2, gab);
                        fill(c - 1, yy, z1, c - 1, yy, z2, gab);
                    }
                }
            }
            if (a + 1 == c) {
                if (alongX) {
                    fill(x1 - 1, yy + 1, a, x2 + 1, yy + 1, c, m.slab());
                } else {
                    fill(a, yy + 1, z1 - 1, c, yy + 1, z2 + 1, m.slab());
                }
                break;
            }
        }
        return this;
    }

    /** A flat roof with a crenellated parapet: slab at y, parapet ring at y+1, merlons at y+2. */
    MisthalinKit crenelRoof(Box r, int y, String floor, String parapet) {
        fill(r, y, y, floor);
        walls(r, y + 1, y + 1, parapet);
        merlons(r, y + 2, parapet);
        return this;
    }

    /** Alternate merlons around the edge of a rectangle at height y. */
    MisthalinKit merlons(Box r, int y, String block) {
        for (int x = r.x1; x <= r.x2; x += 2) {
            set(x, y, r.z1, block);
            set(x, y, r.z2, block);
        }
        for (int z = r.z1; z <= r.z2; z += 2) {
            set(r.x1, y, z, block);
            set(r.x2, y, z, block);
        }
        set(r.x2, y, r.z1, block);
        set(r.x2, y, r.z2, block);
        set(r.x1, y, r.z2, block);
        return this;
    }

    // ---------------- houses ----------------

    House house(Box box) {
        return new House(box);
    }

    House tHouse(int tx1, int ty1, int tx2, int ty2) {
        return new House(t(tx1, ty1, tx2, ty2));
    }

    enum RoofKind { PITCHED, FLAT, CRENEL, NONE }

    /** A rectangular building: walls, corner posts, storeys, doors, windows and a roof. */
    final class House {
        final Box box;
        int storeys = 1;
        int h = 4;
        String wall = STONE_BRICKS;
        String upperWall = null;
        String corner = null;
        String floor = SPRUCE_PLANKS;
        String upperFloor = null;
        String base = null;
        String beam = null;
        String window = GLASS;
        String doorBlock = "minecraft:spruce_door";
        RoofKind roofKind = RoofKind.PITCHED;
        Roof roof = Roof.SLATE;
        String gable = null;
        String parapet = null;
        int windowEvery = 3;
        boolean detail = true;
        final List<int[]> doors = new ArrayList<>();

        House(Box box) {
            this.box = box;
        }

        House storeys(int n) {
            storeys = n;
            return this;
        }

        House height(int blocksPerStorey) {
            h = blocksPerStorey;
            return this;
        }

        House wall(String w) {
            wall = w;
            return this;
        }

        House upperWall(String w) {
            upperWall = w;
            return this;
        }

        House corner(String c) {
            corner = c;
            return this;
        }

        House floor(String f) {
            floor = f;
            return this;
        }

        House upperFloor(String f) {
            upperFloor = f;
            return this;
        }

        House base(String b) {
            base = b;
            return this;
        }

        House beam(String b) {
            beam = b;
            return this;
        }

        /** Door block for two-wide doorways (a pair of doors), or null to leave them open. */
        House doors(String d) {
            doorBlock = d;
            return this;
        }

        House window(String w) {
            window = w;
            return this;
        }

        House windowEvery(int n) {
            windowEvery = n;
            return this;
        }

        House roof(Roof r) {
            roof = r;
            roofKind = RoofKind.PITCHED;
            return this;
        }

        House gable(String g) {
            gable = g;
            return this;
        }

        House flat() {
            roofKind = RoofKind.FLAT;
            return this;
        }

        House crenel(String parapetBlock) {
            roofKind = RoofKind.CRENEL;
            parapet = parapetBlock;
            return this;
        }

        House noRoof() {
            roofKind = RoofKind.NONE;
            return this;
        }

        /** Leaves out the finishing details (textures, shutters, lanterns, chimney). */
        House plain() {
            detail = false;
            return this;
        }

        /** A doorway one tile wide on side 'n','s','e','w' at tile coordinate {@code tile} along that wall. */
        House door(char side, int tile) {
            int from = side == 'n' || side == 's' ? bx(tile) : bz(tile);
            doors.add(new int[]{side, from, 2, 3});
            return this;
        }

        /** A doorway at block coordinate {@code from} along the wall, {@code width} blocks wide. */
        House doorAt(char side, int from, int width, int height) {
            doors.add(new int[]{side, from, width, height});
            return this;
        }

        /** Relative y of storey s's walking level. */
        int level(int s) {
            return s * h;
        }

        int top() {
            return storeys * h;
        }

        Box inner() {
            return box.inset(1);
        }

        private boolean nearDoor(char side, int along, int y) {
            for (int[] d : doors) {
                if (d[0] == side && y < d[3] + 1 && along >= d[1] - 1 && along <= d[1] + d[2]) {
                    return true;
                }
            }
            return false;
        }

        /** Wall textures, quoins, plinth, timbering, window dressings, door lanterns and a chimney. */
        private void finish(List<int[]> wins) {
            int top = top();
            int lowerTop = upperWall != null ? h - 1 : top - 1;
            boolean castle = roofKind == RoofKind.CRENEL;
            int y0 = base != null ? 1 : 0;
            if (MisthalinDetail.isStone(wall)) {
                d.texture(box.x1, box.z1, box.x2, box.z2, g + y0, g + lowerTop, wall, castle ? 6 : 8);
                if (corner == null) {
                    d.quoins(box.x1, box.z1, box.x2, box.z2, g + y0, g + lowerTop, wall, MisthalinDetail.quoinFor(wall));
                }
            }
            if (upperWall != null && MisthalinDetail.isStone(upperWall)) {
                d.texture(box.x1, box.z1, box.x2, box.z2, g + h, g + top - 1, upperWall, 8);
            }
            if (base == null) {
                d.plinth(box.x1, box.z1, box.x2, box.z2, g, wall, MisthalinDetail.plinthFor(wall));
            }
            String log = beam != null && beam.contains("_log") ? MisthalinDetail.bare(beam)
                    : corner != null && corner.contains("_log") ? MisthalinDetail.bare(corner) : STRIPPED_SPRUCE;
            if (MisthalinDetail.isPlaster(wall) || wall.endsWith("_planks")) {
                d.timber(box.x1, box.z1, box.x2, box.z2, g + y0, g + lowerTop, h, wall, log, 3);
            }
            if (upperWall != null && (MisthalinDetail.isPlaster(upperWall) || upperWall.endsWith("_planks"))) {
                d.timber(box.x1, box.z1, box.x2, box.z2, g + h, g + top - 1, h, upperWall, log, 3);
            }
            // windows
            for (int[] p : wins) {
                char side = p[2] == box.z1 ? 'n' : p[2] == box.z2 ? 's' : p[0] == box.x1 ? 'w' : 'e';
                int s = (p[1] - 1) / h;
                String w = s > 0 && upperWall != null ? upperWall : wall;
                boolean stone = MisthalinDetail.isStone(w);
                String lintel = stone ? MisthalinDetail.quoinFor(w)
                        : log + (side == 'n' || side == 's' ? "[axis=x]" : "[axis=z]");
                if (castle) {
                    d.replace(p[0], g + p[3] + 1, p[2], p[0], g + p[3] + 1, p[2], w, lintel);
                } else {
                    d.window(p[0], g + p[1], g + p[3], p[2], side, w, MisthalinDetail.shutterWood(w, "spruce"), lintel, g);
                }
            }
            // lanterns either side of ordinary doorways
            for (int[] dr : doors) {
                if (dr[2] > 4 || dr[3] > 4) {
                    continue;
                }
                int from = dr[1];
                int to = dr[1] + dr[2] - 1;
                switch ((char) dr[0]) {
                    case 'n' -> {
                        d.wallLantern(from - 1, g + 2, box.z1 - 1);
                        d.wallLantern(to + 1, g + 2, box.z1 - 1);
                    }
                    case 's' -> {
                        d.wallLantern(from - 1, g + 2, box.z2 + 1);
                        d.wallLantern(to + 1, g + 2, box.z2 + 1);
                    }
                    case 'e' -> {
                        d.wallLantern(box.x2 + 1, g + 2, from - 1);
                        d.wallLantern(box.x2 + 1, g + 2, to + 1);
                    }
                    default -> {
                        d.wallLantern(box.x1 - 1, g + 2, from - 1);
                        d.wallLantern(box.x1 - 1, g + 2, to + 1);
                    }
                }
            }
            // a chimney stack built into one gable wall
            if (roofKind == RoofKind.PITCHED && box.w() >= 6 && box.d() >= 6
                    && MisthalinDetail.noise(box.x1, 0, box.z1, 3) % 3 != 0) {
                boolean alongX = (box.x2 - box.x1) >= (box.z2 - box.z1);
                int span = alongX ? box.z2 - box.z1 : box.x2 - box.x1;
                int ridge = top + span / 2 + 2;
                String stack = MisthalinDetail.isStone(wall) ? COBBLE : "minecraft:bricks";
                char[] ends = alongX ? new char[]{'w', 'e'} : new char[]{'n', 's'};
                for (char e : ends) {
                    int along = alongX ? box.z1 + 3 : box.x1 + 3;
                    boolean blocked = false;
                    for (int[] dr : doors) {
                        if (dr[0] == e && along >= dr[1] - 2 && along <= dr[1] + dr[2] + 1) {
                            blocked = true;
                        }
                    }
                    if (blocked) {
                        continue;
                    }
                    int cx = alongX ? (e == 'w' ? box.x1 : box.x2) : along;
                    int cz = alongX ? along : (e == 'n' ? box.z1 : box.z2);
                    d.chimney(cx, cz, g + 1, g + ridge, stack);
                    break;
                }
            }
        }

        House build() {
            Box in = box.inset(1);
            int top = top();
            // foundations reach below uneven ground
            fill(box, -3, -2, base != null ? base : COBBLE);
            fill(box, -1, -1, base != null ? base : COBBLE);
            fill(in, -1, -1, floor);
            clear(in, 0, top - 1);
            for (int s = 0; s < storeys; s++) {
                String w = s > 0 && upperWall != null ? upperWall : wall;
                walls(box, s * h, s * h + h - 1, w);
                if (s > 0) {
                    fill(in, s * h - 1, s * h - 1, upperFloor != null ? upperFloor : floor);
                    if (beam != null) {
                        walls(box, s * h - 1, s * h - 1, beam);
                    }
                }
            }
            if (base != null) {
                walls(box, 0, 0, base);
            }
            if (corner != null) {
                fill(box.x1, 0, box.z1, box.x1, top - 1, box.z1, corner);
                fill(box.x2, 0, box.z1, box.x2, top - 1, box.z1, corner);
                fill(box.x1, 0, box.z2, box.x1, top - 1, box.z2, corner);
                fill(box.x2, 0, box.z2, box.x2, top - 1, box.z2, corner);
            }
            // windows (twice so panes join each other)
            List<int[]> wins = new ArrayList<>();
            if (window != null) {
                for (int s = 0; s < storeys; s++) {
                    int wy = s * h + 1;
                    int wy2 = h >= 5 ? wy + 1 : wy;
                    for (int x = box.x1 + 2; x <= box.x2 - 2; x += windowEvery) {
                        if (!nearDoor('n', x, wy)) {
                            wins.add(new int[]{x, wy, box.z1, wy2});
                        }
                        if (!nearDoor('s', x, wy)) {
                            wins.add(new int[]{x, wy, box.z2, wy2});
                        }
                    }
                    for (int z = box.z1 + 2; z <= box.z2 - 2; z += windowEvery) {
                        if (!nearDoor('w', z, wy)) {
                            wins.add(new int[]{box.x1, wy, z, wy2});
                        }
                        if (!nearDoor('e', z, wy)) {
                            wins.add(new int[]{box.x2, wy, z, wy2});
                        }
                    }
                }
                for (int pass = 0; pass < 2; pass++) {
                    for (int[] p : wins) {
                        fill(p[0], p[1], p[2], p[0], p[3], p[2], window);
                    }
                }
            }
            for (int[] d : doors) {
                int from = d[1];
                int to = d[1] + d[2] - 1;
                switch (d[0]) {
                    case 'n' -> clear(from, 0, box.z1, to, d[3] - 1, box.z1);
                    case 's' -> clear(from, 0, box.z2, to, d[3] - 1, box.z2);
                    case 'e' -> clear(box.x2, 0, from, box.x2, d[3] - 1, to);
                    default -> clear(box.x1, 0, from, box.x1, d[3] - 1, to);
                }
                // threshold
                switch (d[0]) {
                    case 'n' -> fill(from, -1, box.z1, to, -1, box.z1, floor);
                    case 's' -> fill(from, -1, box.z2, to, -1, box.z2, floor);
                    case 'e' -> fill(box.x2, -1, from, box.x2, -1, to, floor);
                    default -> fill(box.x1, -1, from, box.x1, -1, to, floor);
                }
                // a pair of doors in ordinary two-wide doorways (the placer adds the upper halves)
                if (doorBlock != null && d[2] == 2 && d[3] <= 3) {
                    String facing = switch (d[0]) {
                        case 'n' -> "north";
                        case 's' -> "south";
                        case 'e' -> "east";
                        default -> "west";
                    };
                    String a = doorBlock + "[facing=" + facing + ",half=lower,hinge=left]";
                    String c = doorBlock + "[facing=" + facing + ",half=lower,hinge=right]";
                    switch (d[0]) {
                        case 'n' -> set(from, 0, box.z1, c).set(to, 0, box.z1, a);
                        case 's' -> set(from, 0, box.z2, a).set(to, 0, box.z2, c);
                        case 'e' -> set(box.x2, 0, from, c).set(box.x2, 0, to, a);
                        default -> set(box.x1, 0, from, a).set(box.x1, 0, to, c);
                    }
                }
            }
            if (detail) {
                finish(wins);
            }
            switch (roofKind) {
                case PITCHED -> {
                    fill(in, top - 1, top - 1, upperFloor != null ? upperFloor : floor);
                    MisthalinKit.this.roof(box, top, roof, gable != null ? gable : (upperWall != null ? upperWall : wall));
                }
                case FLAT -> {
                    fill(box, top - 1, top - 1, roof.full());
                    walls(box, top, top, roof.slab());
                }
                case CRENEL -> {
                    fill(in, top - 1, top - 1, roof.full());
                    String p = parapet != null ? parapet : wall;
                    walls(box, top, top, p);
                    merlons(box, top + 1, p);
                }
                default -> {
                }
            }
            return this;
        }
    }

    // ---------------- vertical circulation ----------------

    /**
     * A straight flight of {@code n} stairs starting at (x, y, z) climbing towards {@code dir}
     * ('n','s','e','w'), clearing headroom (cuts the floor openings it passes through).
     */
    MisthalinKit stairs(int x, int y, int z, char dir, int n, String stairBase) {
        int dx = dir == 'e' ? 1 : dir == 'w' ? -1 : 0;
        int dz = dir == 's' ? 1 : dir == 'n' ? -1 : 0;
        String facing = switch (dir) {
            case 'n' -> "north";
            case 's' -> "south";
            case 'e' -> "east";
            default -> "west";
        };
        for (int i = 0; i < n; i++) {
            int xx = x + dx * i;
            int zz = z + dz * i;
            clear(xx, y + i + 1, zz, xx, y + i + 3, zz);
            set(xx, y + i, zz, stairBase + "[facing=" + facing + "]");
        }
        return this;
    }

    /** A stair flight on a solid stone ramp (support filled down to the ground). */
    MisthalinKit stairsSolid(int x, int y, int z, char dir, int n, String stairBase, String support) {
        int dx = dir == 'e' ? 1 : dir == 'w' ? -1 : 0;
        int dz = dir == 's' ? 1 : dir == 'n' ? -1 : 0;
        for (int i = 1; i < n; i++) {
            fill(x + dx * i, -1, z + dz * i, x + dx * i, y + i - 1, z + dz * i, support);
        }
        return stairs(x, y, z, dir, n, stairBase);
    }

    /** Two-wide stair flight. {@code side} offsets the second column ('n','s','e','w'). */
    MisthalinKit stairs2(int x, int y, int z, char dir, int n, String stairBase, char side) {
        stairs(x, y, z, dir, n, stairBase);
        int ox = side == 'e' ? 1 : side == 'w' ? -1 : 0;
        int oz = side == 's' ? 1 : side == 'n' ? -1 : 0;
        return stairs(x + ox, y, z + oz, dir, n, stairBase);
    }

    /**
     * A ladder column from y1 to y2 at (x, z) facing {@code facing} (away from the supporting wall),
     * cutting through the floors it passes.
     */
    MisthalinKit ladder(int x, int z, String facing, int y1, int y2) {
        clear(x, y1, z, x, y2, z);
        return fill(x, y1, z, x, y2, z, "minecraft:ladder[facing=" + facing + "]");
    }

    // ---------------- round towers ----------------

    /** Row half-widths of a disc of radius r. */
    static int half(double r, int dz) {
        double v = (r + 0.5) * (r + 0.5) - dz * dz;
        return v < 0 ? -1 : (int) Math.floor(Math.sqrt(v));
    }

    MisthalinKit disc(int cx, int cz, double r, int y1, int y2, String block) {
        int R = (int) Math.ceil(r + 0.5);
        for (int dz = -R; dz <= R; dz++) {
            int w = half(r, dz);
            if (w >= 0) {
                fill(cx - w, y1, cz + dz, cx + w, y2, cz + dz, block);
            }
        }
        return this;
    }

    MisthalinKit discClear(int cx, int cz, double r, int y1, int y2) {
        int R = (int) Math.ceil(r + 0.5);
        for (int dz = -R; dz <= R; dz++) {
            int w = half(r, dz);
            if (w >= 0) {
                clear(cx - w, y1, cz + dz, cx + w, y2, cz + dz);
            }
        }
        return this;
    }

    /** A ring (disc of r minus disc of r - thick). */
    MisthalinKit ring(int cx, int cz, double r, double thick, int y1, int y2, String block) {
        int R = (int) Math.ceil(r + 0.5);
        for (int dz = -R; dz <= R; dz++) {
            int w = half(r, dz);
            if (w < 0) {
                continue;
            }
            int wi = half(r - thick, dz);
            if (wi < 0) {
                fill(cx - w, y1, cz + dz, cx + w, y2, cz + dz, block);
            } else {
                if (cx - w <= cx - wi - 1) {
                    fill(cx - w, y1, cz + dz, cx - wi - 1, y2, cz + dz, block);
                }
                if (cx + wi + 1 <= cx + w) {
                    fill(cx + wi + 1, y1, cz + dz, cx + w, y2, cz + dz, block);
                }
            }
        }
        return this;
    }

    /** Points on the outer edge of a disc (for crenellations). */
    List<int[]> rim(int cx, int cz, double r) {
        List<int[]> pts = new ArrayList<>();
        int R = (int) Math.ceil(r + 0.5);
        for (int dz = -R; dz <= R; dz++) {
            int w = half(r, dz);
            if (w < 0) {
                continue;
            }
            int wi = half(r - 1, dz);
            for (int dx = -w; dx <= w; dx++) {
                if (wi < 0 || Math.abs(dx) > wi) {
                    pts.add(new int[]{cx + dx, cz + dz});
                }
            }
        }
        return pts;
    }

    /**
     * A round tower centred on block (cx, cz): radius r, walls up to {@code height}-1, floors every
     * {@code h} blocks, arrow-slit windows, and either crenellations or a stepped conical roof
     * ({@code cone} non-null). A ladder against the north wall links all floors.
     */
    MisthalinKit tower(int cx, int cz, int r, int height, int h, String wall, String floor, Roof cone, boolean ladder) {
        disc(cx, cz, r, -4, -1, wall);
        disc(cx, cz, r - 1, -1, -1, floor);
        discClear(cx, cz, r - 1, 0, height - 1);
        ring(cx, cz, r, 1, 0, height - 1, wall);
        for (int y = h - 1; y < height - 1; y += h) {
            disc(cx, cz, r - 1, y, y, floor);
        }
        // arrow slits at the compass points of each level (not the north wall, which carries the ladder)
        for (int y = 1; y < height - 1; y += h) {
            for (int pass = 0; pass < 2; pass++) {
                set(cx, y, cz + r, GLASS).set(cx, y + 1, cz + r, GLASS);
                set(cx - r, y, cz, GLASS).set(cx - r, y + 1, cz, GLASS);
                set(cx + r, y, cz, GLASS).set(cx + r, y + 1, cz, GLASS);
            }
        }
        // lanterns on each level
        for (int y = h - 2; y < height - 1; y += h) {
            hang(cx, y, cz);
        }
        // weathering, string courses at each floor and a corbelled parapet (deferred details)
        d.textureSolid(cx - r, g, cz - r, cx + r, g + height - 1, cz + r, wall, 5);
        String trim = MisthalinDetail.quoinFor(wall);
        for (int y = h - 1; y < height - 1; y += h) {
            d.replace(cx - r, g + y, cz - r, cx + r, g + y, cz + r, wall, trim);
        }
        d.replace(cx - r, g, cz - r, cx + r, g, cz + r, wall, MisthalinDetail.plinthFor(wall));
        if (cone == null) {
            corbel(cx, cz, r, height, wall);
        }
        if (cone == null) {
            disc(cx, cz, r - 1, height - 1, height - 1, floor);
            ring(cx, cz, r, 1, height, height, wall);
            for (int[] p : rim(cx, cz, r)) {
                if (((p[0] + p[1]) & 1) == 0) {
                    set(p[0], height + 1, p[1], wall);
                }
            }
            if (ladder) {
                ladder(cx, cz - r + 1, "south", 0, height - 1);
            }
        } else {
            disc(cx, cz, r - 1, height - 1, height - 1, floor);
            for (int k = 0; k <= r + 1; k++) {
                double rr = r + 1 - k;
                if (rr < 0) {
                    break;
                }
                if (rr >= 1) {
                    ring(cx, cz, rr, 1.2, height + k, height + k, cone.full());
                } else {
                    set(cx, height + k, cz, cone.full());
                }
            }
            set(cx, height + r + 2, cz, "minecraft:spruce_fence");
            set(cx, height + r + 3, cz, "minecraft:spruce_fence");
            int last = -1;
            for (int y = h - 1; y < height - 1; y += h) {
                last = y;
            }
            if (ladder && last > 0) {
                ladder(cx, cz - r + 1, "south", 0, last);
            }
        }
        return this;
    }

    /**
     * A corbelled parapet round a crenellated tower (deferred, only into air): a ring of upturned
     * slabs one block outside the wall just below the top, a parapet ring on them and merlons.
     */
    MisthalinKit corbel(int cx, int cz, int r, int height, String wall) {
        String slab = MisthalinDetail.slabFor(wall) + "[type=top]";
        int R = r + 2;
        for (int dz = -R; dz <= R; dz++) {
            int w = half(r + 1, dz);
            if (w < 0) {
                continue;
            }
            int wi = half(r, dz);
            int a1 = cx - w;
            int a2 = wi < 0 ? cx + w : cx - wi - 1;
            int c1 = wi < 0 ? a2 + 1 : cx + wi + 1;
            int c2 = cx + w;
            for (int[] seg : new int[][]{{a1, a2}, {c1, c2}}) {
                if (seg[0] > seg[1]) {
                    continue;
                }
                d.air(seg[0], g + height - 1, cz + dz, seg[1], g + height - 1, cz + dz, slab);
                d.air(seg[0], g + height, cz + dz, seg[1], g + height, cz + dz, wall);
            }
        }
        for (int[] p : rim(cx, cz, r + 1)) {
            if (((p[0] + p[1]) & 1) == 1) {
                d.air(p[0], g + height + 1, p[1], p[0], g + height + 1, p[1], wall);
            }
        }
        return this;
    }

    // ---------------- landscaping ----------------

    /** A broadleaf tree with its trunk on block (x, z). */
    MisthalinKit tree(int x, int z, int trunk, String log, String leaves) {
        int top = trunk;
        fillAir(x - 2, top - 2, z - 2, x + 2, top - 1, z + 2, leaves);
        fillAir(x - 1, top - 2, z - 3, x + 1, top - 1, z + 3, leaves);
        fillAir(x - 3, top - 2, z - 1, x + 3, top - 1, z + 1, leaves);
        fillAir(x - 1, top, z - 2, x + 1, top, z + 2, leaves);
        fillAir(x - 2, top, z - 1, x + 2, top, z + 1, leaves);
        fillAir(x - 1, top + 1, z - 1, x + 1, top + 1, z + 1, leaves);
        fill(x, 0, z, x, top - 1, z, log);
        return this;
    }

    MisthalinKit oak(int x, int z) {
        return tree(x, z, 5 + Math.floorMod(x * 7 + z * 3, 3), OAK_LOG, OAK_LEAVES);
    }

    MisthalinKit tOak(int tx, int ty) {
        return oak(X(tx), Z(ty));
    }

    /** A drooping willow (choppable runecraft willow trunk). */
    MisthalinKit willow(int x, int z) {
        int top = 6;
        fill(x, 0, z, x, 1, z, "runecraft:willow_tree_log");
        fill(x, 2, z, x, top - 1, z, OAK_LOG);
        fillAir(x - 3, top - 1, z - 3, x + 3, top, z + 3, JUNGLE_LEAVES);
        fillAir(x - 2, top + 1, z - 2, x + 2, top + 1, z + 2, JUNGLE_LEAVES);
        // drooping fronds
        for (int[] d : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}, {-2, -2}, {2, 2}, {-2, 2}, {2, -2}, {-3, 2}, {3, -2}}) {
            fillAir(x + d[0], top - 4, z + d[1], x + d[0], top - 2, z + d[1], JUNGLE_LEAVES);
        }
        return this;
    }

    /** A dark yew (choppable runecraft yew trunk). */
    MisthalinKit yew(int x, int z) {
        fill(x, 0, z, x + 1, 1, z + 1, "runecraft:yew_tree_log");
        fill(x, 2, z, x + 1, 6, z + 1, DARK_LOG);
        fillAir(x - 3, 4, z - 3, x + 4, 7, z + 4, DARK_LEAVES);
        fillAir(x - 2, 8, z - 2, x + 3, 9, z + 3, DARK_LEAVES);
        fillAir(x - 4, 5, z - 1, x + 5, 6, z + 2, DARK_LEAVES);
        fillAir(x - 1, 5, z - 4, x + 2, 6, z + 5, DARK_LEAVES);
        return this;
    }

    /** A leafless dead tree. */
    MisthalinKit deadTree(int x, int z) {
        fill(x, 0, z, x, 4, z, "minecraft:stripped_dark_oak_log");
        set(x + 1, 3, z, "minecraft:dark_oak_fence");
        set(x + 2, 4, z, "minecraft:dark_oak_fence");
        set(x - 1, 4, z, "minecraft:dark_oak_fence");
        set(x, 5, z + 1, "minecraft:dark_oak_fence");
        set(x, 5, z - 1, "minecraft:dark_oak_fence");
        return this;
    }

    MisthalinKit bush(int x, int z, String leaves) {
        return fillAir(x, 0, z, x + 1, 0, z, leaves);
    }

    /** Fence along a block line (inclusive), joined, at ground level. */
    MisthalinKit fence(int x1, int z1, int x2, int z2, String fence) {
        return connect(Math.min(x1, x2), 0, Math.min(z1, z2), Math.max(x1, x2), 0, Math.max(z1, z2), fence);
    }

    /** Fence around a block rectangle with gaps closed by gates (list of {x,z,facing}). */
    MisthalinKit fenceRing(Box r, String fence) {
        connect(r.x1, 0, r.z1, r.x2, 0, r.z1, fence);
        connect(r.x1, 0, r.z2, r.x2, 0, r.z2, fence);
        connect(r.x1, 0, r.z1, r.x1, 0, r.z2, fence);
        connect(r.x2, 0, r.z1, r.x2, 0, r.z2, fence);
        return this;
    }

    MisthalinKit gate(int x, int z, String gateBase, String facing) {
        return set(x, 0, z, gateBase + "[facing=" + facing + "]");
    }

    /** Water channel filled to the ground surface (y = -1) and {@code depth} deep. */
    MisthalinKit water(Box r, int depth) {
        fill(r, -depth - 1, -depth - 1, DIRT);
        clear(r, 0, 3);
        return fill(r, -depth, -1, WATER);
    }

    /** Flowers and grass tufts scattered on a grassy rectangle. */
    MisthalinKit meadow(Box r, int every) {
        scatter(r, 0, "minecraft:grass", every);
        scatter(r, 0, "minecraft:dandelion", every * 4);
        scatter(r, 0, "minecraft:poppy", every * 5);
        scatter(r, 0, "minecraft:oxeye_daisy", every * 6);
        return this;
    }

    // ---------------- furniture ----------------

    MisthalinKit table(int x, int y, int z) {
        set(x, y, z, "minecraft:oak_fence");
        return set(x, y + 1, z, "minecraft:oak_pressure_plate");
    }

    MisthalinKit chair(int x, int y, int z, String facing) {
        return set(x, y, z, "minecraft:oak_stairs[facing=" + facing + "]");
    }

    MisthalinKit darkChair(int x, int y, int z, String facing) {
        return set(x, y, z, "minecraft:dark_oak_stairs[facing=" + facing + "]");
    }

    /** A long table with chairs on both long sides, along x from x1 to x2. */
    MisthalinKit diningX(int x1, int x2, int y, int z) {
        fill(x1, y, z, x2, y, z, "minecraft:spruce_slab[type=top]");
        for (int x = x1; x <= x2; x++) {
            chair(x, y, z - 1, "south");
            chair(x, y, z + 1, "north");
        }
        return this;
    }

    MisthalinKit diningZ(int x, int y, int z1, int z2) {
        fill(x, y, z1, x, y, z2, "minecraft:spruce_slab[type=top]");
        for (int z = z1; z <= z2; z++) {
            chair(x - 1, y, z, "east");
            chair(x + 1, y, z, "west");
        }
        return this;
    }

    MisthalinKit carpet(Box r, int y, String colour) {
        return fill(r, y, y, "minecraft:" + colour + "_carpet");
    }

    /**
     * A bed with its pillow (head) on (x, z) and its foot one block towards {@code dir}. Only the foot
     * is given; the placer adds the head half.
     */
    MisthalinKit bed(int x, int y, int z, char dir, String colour) {
        int dx = dir == 'e' ? 1 : dir == 'w' ? -1 : 0;
        int dz = dir == 's' ? 1 : dir == 'n' ? -1 : 0;
        String facing = switch (dir) {
            case 'n' -> "south";
            case 's' -> "north";
            case 'e' -> "west";
            default -> "east";
        };
        clear(x, y, z, x, y, z);
        return set(x + dx, y, z + dz, "minecraft:" + colour + "_bed[facing=" + facing + ",part=foot]");
    }

    /**
     * Furnishes an ordinary home: table and chairs, hearth, storage on the ground floor, a corner
     * ladder (north-west, or north-east when the door is on the west wall) and bedrooms upstairs.
     */
    MisthalinKit furnishHome(House h, char doorSide, String colour) {
        Box in = h.inner();
        boolean ladderEast = doorSide == 'w';
        int lx = ladderEast ? in.x2() : in.x1();
        // ground floor
        int tx = in.cx();
        int tz = in.cz();
        table(tx, 0, tz);
        chair(tx - 1, 0, tz, "east");
        chair(tx + 1, 0, tz, "west");
        int fx = ladderEast ? in.x1() : in.x2();
        set(fx, 0, in.z1(), "minecraft:furnace[facing=" + (ladderEast ? "east" : "west") + "]");
        set(fx, 0, in.z2(), "minecraft:chest[facing=north]");
        set(lx, 0, in.z2(), BARREL);
        if (in.w() > 5) {
            set(fx + (ladderEast ? 1 : -1), 0, in.z2(), CRAFTING);
        }
        hang(tx, h.h - 2, tz);
        // a rug round the table, a plant, a bookcase and a hearth rug (only into free floor)
        d.air(tx - 2, g, tz - 1, tx + 2, g, tz + 1, "minecraft:" + colour + "_carpet");
        d.air(fx, g + 1, in.z2(), fx, g + 1, in.z2(), "minecraft:potted_fern");
        if (in.d() > 5) {
            d.air(fx, g, in.z1() + 2, fx, g + 1, in.z1() + 2, BOOKSHELF);
            d.air(fx, g + 2, in.z1() + 2, fx, g + 2, in.z1() + 2, "minecraft:candle[candles=2,lit=true]");
        }
        if (h.storeys > 1) {
            ladder(lx, in.z1(), "south", 0, h.h * (h.storeys - 1));
            for (int s = 1; s < h.storeys; s++) {
                int y = h.level(s);
                carpet(new Box(in.x1() + 1, in.z1() + 1, in.x2() - 1, in.z2() - 1), y, colour);
                bed(fx, y, in.z1(), 's', colour);
                set(fx, y, in.z2(), "minecraft:chest[facing=north]");
                set(lx, y, in.z2(), BOOKSHELF);
                if (in.d() > 4) {
                    set(lx, y, in.z2() - 1, BOOKSHELF);
                }
                hang(tx, y + h.h - 2, tz);
            }
        } else {
            bed(lx, 0, in.z1(), 's', colour);
        }
        return this;
    }

    /** A hanging lantern at y on a short chain (the chain only fills air, up to three blocks). */
    MisthalinKit hang(int x, int y, int z) {
        fillAir(x, y + 1, z, x, y + 3, z, "minecraft:chain");
        return set(x, y, z, LANTERN_HANG);
    }

    MisthalinKit wallTorch(int x, int y, int z, String facing) {
        return set(x, y, z, "minecraft:wall_torch[facing=" + facing + "]");
    }

    MisthalinKit pot(int x, int y, int z, String plant) {
        return set(x, y, z, "minecraft:potted_" + plant);
    }

    /** A shop counter (along x or z) of top slabs over barrels with the station block in it. */
    MisthalinKit counterX(int x1, int x2, int y, int z, String station) {
        fill(x1, y, z, x2, y, z, "minecraft:spruce_planks");
        fill(x1, y + 1, z, x2, y + 1, z, "minecraft:spruce_slab");
        if (station != null) {
            set((x1 + x2) / 2, y, z, station);
            set((x1 + x2) / 2, y + 1, z, AIR);
        }
        return this;
    }

    MisthalinKit counterZ(int x, int y, int z1, int z2, String station) {
        fill(x, y, z1, x, y, z2, "minecraft:spruce_planks");
        fill(x, y + 1, z1, x, y + 1, z2, "minecraft:spruce_slab");
        if (station != null) {
            set(x, y, (z1 + z2) / 2, station);
            set(x, y + 1, (z1 + z2) / 2, AIR);
        }
        return this;
    }

    /** Shelving along a wall line: alternating bookshelves, barrels and chests, two high. */
    MisthalinKit shelvesX(int x1, int x2, int y, int z, String facing) {
        for (int x = x1; x <= x2; x++) {
            int k = Math.floorMod(x, 3);
            set(x, y, z, k == 0 ? BARREL : k == 1 ? "minecraft:chest[facing=" + facing + "]" : BOOKSHELF);
            set(x, y + 1, z, k == 1 ? "minecraft:flower_pot" : BOOKSHELF);
        }
        return this;
    }

    MisthalinKit shelvesZ(int x, int y, int z1, int z2, String facing) {
        for (int z = z1; z <= z2; z++) {
            int k = Math.floorMod(z, 3);
            set(x, y, z, k == 0 ? BARREL : k == 1 ? "minecraft:chest[facing=" + facing + "]" : BOOKSHELF);
            set(x, y + 1, z, k == 1 ? "minecraft:flower_pot" : BOOKSHELF);
        }
        return this;
    }

    /** A row of bank booths along x at z, with polished stone either side. */
    MisthalinKit boothsX(int x1, int x2, int y, int z) {
        for (int x = x1; x <= x2; x++) {
            set(x, y, z, Math.floorMod(x - x1, 2) == 0 ? BANK : "minecraft:polished_andesite");
            set(x, y + 1, z, Math.floorMod(x - x1, 2) == 0 ? AIR : "minecraft:polished_andesite_slab");
        }
        set(x2, y, z, BANK);
        return this;
    }

    MisthalinKit boothsZ(int x, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            set(x, y, z, Math.floorMod(z - z1, 2) == 0 ? BANK : "minecraft:polished_andesite");
            set(x, y + 1, z, Math.floorMod(z - z1, 2) == 0 ? AIR : "minecraft:polished_andesite_slab");
        }
        set(x, y, z2, BANK);
        return this;
    }

    /** Kitchen: range against a wall, worktop, barrels and a table. */
    MisthalinKit kitchen(Box in, int y) {
        set(in.x1, y, in.z1, RANGE);
        set(in.x1 + 1, y, in.z1, "minecraft:smoker[facing=south]");
        fill(in.x1 + 2, y, in.z1, Math.min(in.x2, in.x1 + 4), y, in.z1, "minecraft:spruce_planks");
        set(in.x1 + 2, y + 1, in.z1, "minecraft:cauldron");
        set(in.x2, y, in.z2, BARREL);
        set(in.x2, y + 1, in.z2, BARREL);
        set(in.x2 - 1, y, in.z2, "minecraft:chest[facing=north]");
        table(in.cx(), y, in.cz());
        return this;
    }

    /** A free-standing market stall: four posts, a striped awning and the stall block. */
    MisthalinKit stall(int x, int z, String block, String wool) {
        fill(x - 1, 0, z - 1, x + 1, 0, z - 1, "minecraft:spruce_planks");
        set(x, 0, z - 1, block);
        set(x - 2, 0, z - 2, "minecraft:spruce_fence").set(x - 2, 1, z - 2, "minecraft:spruce_fence");
        set(x + 2, 0, z - 2, "minecraft:spruce_fence").set(x + 2, 1, z - 2, "minecraft:spruce_fence");
        set(x - 2, 0, z, "minecraft:spruce_fence").set(x - 2, 1, z, "minecraft:spruce_fence");
        set(x + 2, 0, z, "minecraft:spruce_fence").set(x + 2, 1, z, "minecraft:spruce_fence");
        for (int dx = -2; dx <= 2; dx++) {
            fill(x + dx, 2, z - 2, x + dx, 2, z, Math.floorMod(dx, 2) == 0 ? "minecraft:" + wool + "_wool" : "minecraft:white_wool");
        }
        set(x - 1, 0, z - 2, BARREL);
        set(x + 1, 0, z - 2, "minecraft:composter");
        return this;
    }

    /** A well: stone ring, water, roof on posts. */
    MisthalinKit well(int x, int z) {
        fill(x - 1, -3, z - 1, x + 1, 0, z + 1, COBBLE);
        fill(x, -3, z, x, 0, z, WATER);
        set(x - 1, 1, z - 1, "minecraft:spruce_fence").set(x - 1, 2, z - 1, "minecraft:spruce_fence");
        set(x + 1, 1, z + 1, "minecraft:spruce_fence").set(x + 1, 2, z + 1, "minecraft:spruce_fence");
        set(x - 1, 1, z + 1, "minecraft:spruce_fence").set(x - 1, 2, z + 1, "minecraft:spruce_fence");
        set(x + 1, 1, z - 1, "minecraft:spruce_fence").set(x + 1, 2, z - 1, "minecraft:spruce_fence");
        // a little gabled roof with a winch beam, rope and bucket
        fill(x - 2, 3, z - 1, x + 2, 3, z - 1, "minecraft:spruce_stairs[facing=south]");
        fill(x - 2, 3, z + 1, x + 2, 3, z + 1, "minecraft:spruce_stairs[facing=north]");
        fill(x - 2, 3, z, x + 2, 3, z, "minecraft:spruce_planks");
        fill(x - 2, 4, z, x + 2, 4, z, "minecraft:spruce_slab");
        set(x, 2, z, "minecraft:chain");
        set(x, 1, z, "minecraft:lantern[hanging=true]");
        set(x - 1, 1, z, "minecraft:spruce_trapdoor[facing=east,half=top]");
        return this;
    }

    /** A gravestone facing south. */
    MisthalinKit grave(int x, int z) {
        set(x, 0, z, "minecraft:stone_brick_wall");
        set(x, 0, z + 1, "minecraft:coarse_dirt");
        return set(x, -1, z + 1, "minecraft:coarse_dirt");
    }

    /** Wooden plank bridge deck between two block points along x (z1..z2 wide) at deck height y. */
    MisthalinKit bridgeX(int x1, int x2, int z1, int z2, int y, String deck, String rail) {
        fill(x1, y, z1, x2, y, z2, deck);
        clear(x1, y + 1, z1, x2, y + 3, z2);
        connect(x1, y + 1, z1, x2, y + 1, z1, rail);
        connect(x1, y + 1, z2, x2, y + 1, z2, rail);
        return this;
    }

    MisthalinKit bridgeZ(int x1, int x2, int z1, int z2, int y, String deck, String rail) {
        fill(x1, y, z1, x2, y, z2, deck);
        clear(x1, y + 1, z1, x2, y + 3, z2);
        connect(x1, y + 1, z1, x1, y + 1, z2, rail);
        connect(x2, y + 1, z1, x2, y + 1, z2, rail);
        return this;
    }

    /** Lamp post with a lantern on top. */
    MisthalinKit lampPost(int x, int z) {
        set(x, 0, z, "minecraft:stone_brick_wall");
        fill(x, 1, z, x, 2, z, "minecraft:spruce_fence");
        set(x, 3, z, LANTERN);
        d.air(x, g + 4, z, x, g + 4, z, "minecraft:spruce_trapdoor[half=bottom]");
        return this;
    }

    /** Crops on moist farmland over a block rectangle with an irrigation channel along its middle row. */
    MisthalinKit field(Box r, String crop) {
        fill(r, -1, -1, "minecraft:farmland[moisture=7]");
        clear(r, 0, 1);
        fill(r, 0, 0, crop);
        for (int mz = r.z1 + 4; mz <= r.z2; mz += 9) {
            fill(r.x1, -2, mz, r.x2, -2, mz, DIRT);
            fill(r.x1, -1, mz, r.x2, -1, mz, WATER);
            clear(r.x1, 0, mz, r.x2, 0, mz);
        }
        return this;
    }

    /** Hay bale and crate clutter. */
    MisthalinKit crates(int x, int z) {
        set(x, 0, z, BARREL);
        set(x + 1, 0, z, "minecraft:chest[facing=south]");
        set(x, 1, z, HAY);
        return this;
    }
}
