package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Compact building kit for the Kandarin structures. Wraps a {@link StructureDef.Builder}; methods
 * starting with {@code t} or taking tile arguments use OSRS tiles, the rest absolute block X/Z.
 * All heights are relative to the ground level (0 = first block above the floor).
 *
 * <p>The structure placer post-processes every block against its neighbours, so two-block blocks
 * (doors, beds) are given as their lower/foot half; the placer adds the other half. Ladders and wall torches are only placed where their support is in the
 * same chunk.
 */
final class KandarinKit {
    static final String AIR = "minecraft:air";
    static final String HAY = "minecraft:hay_block";
    static final String PANE = "minecraft:glass_pane";
    static final String GRASS = "minecraft:grass_block";
    static final String WATER = "minecraft:water";
    static final String PATH = "minecraft:dirt_path";
    static final String GRAVEL = "minecraft:gravel";
    static final String COBBLE = "minecraft:cobblestone";
    static final String SB = "minecraft:stone_bricks";
    static final String MOSSY_SB = "minecraft:mossy_stone_bricks";
    static final String CRACKED_SB = "minecraft:cracked_stone_bricks";
    static final String ANDESITE = "minecraft:polished_andesite";
    static final String SAND = "minecraft:sand";

    static final String BANK = "runecraft:bank_booth";
    static final String RANGE = "runecraft:cooking_range";
    static final String FURNACE = "runecraft:furnace";
    static final String ANVIL = "runecraft:anvil";
    static final String ALTAR = "runecraft:prayer_altar";
    static final String GENERAL = "runecraft:general_store";
    static final String RUNES = "runecraft:rune_shop";
    static final String WEAPONS = "runecraft:weapon_shop";
    static final String TOOLS = "runecraft:tool_shop";
    static final String ARCHERY = "runecraft:archery_shop";
    static final String WHEEL = "runecraft:spinning_wheel";
    static final String TANNER = "runecraft:tanning_rack";
    static final String HERB_TABLE = "runecraft:apothecary_table";

    /** Flags for {@link #house}. */
    static final int FURN = 1;
    static final int FLAT = 2;
    static final int ROOF_LADDER = 4;
    static final int NO_LADDER = 8;
    static final int WIDE_DOOR = 16;
    static final int NO_WINDOWS = 32;
    static final int NO_CEILING = 64;

    /** Storey height in blocks (3 of air plus the floor above). */
    static final int H = 4;

    /** A building palette. {@code roof} is "thatch", "flat:&lt;block&gt;" or a stairs base such as "minecraft:spruce". */
    record Pal(String wall, String post, String floor, String roof, String wood, String window) {
        Pal roof(String r) {
            return new Pal(wall, post, floor, r, wood, window);
        }

        Pal wall(String w) {
            return new Pal(w, post, floor, roof, wood, window);
        }

        Pal post(String p) {
            return new Pal(wall, p, floor, roof, wood, window);
        }

        Pal floor(String f) {
            return new Pal(wall, post, f, roof, wood, window);
        }
    }

    /** A placed building: its block rectangle, storeys and where the door is. */
    record Bld(int x1, int z1, int x2, int z2, int storeys, char door, int doorX, int doorZ) {
        int cx() {
            return (x1 + x2) / 2;
        }

        int cz() {
            return (z1 + z2) / 2;
        }

        /** Relative y of the floor surface (first air block) of storey s. */
        int sy(int s) {
            return s * H;
        }

        int top() {
            return storeys * H;
        }
    }

    static final Pal SEERS = new Pal("minecraft:white_terracotta", "minecraft:dark_oak_log", "minecraft:oak_planks", "thatch", "oak", PANE);
    static final Pal CATHERBY = new Pal("minecraft:smooth_sandstone", "minecraft:stripped_dark_oak_log", "minecraft:spruce_planks", "thatch", "spruce", PANE);
    static final Pal STONE = new Pal(SB, "minecraft:chiseled_stone_bricks", "minecraft:spruce_planks", "minecraft:deepslate_tile", "spruce", PANE);
    static final Pal ARDY = new Pal(SB, ANDESITE, "minecraft:oak_planks", "minecraft:spruce", "dark_oak", PANE);
    static final Pal ARDY_RED = new Pal("minecraft:stone_bricks", "minecraft:polished_andesite", "minecraft:oak_planks", "minecraft:brick", "oak", PANE);
    static final Pal PLAGUE = new Pal("minecraft:andesite", "minecraft:cobblestone", "minecraft:cobblestone", "minecraft:stone_brick", "spruce", "minecraft:iron_bars");
    static final Pal GNOME = new Pal("minecraft:jungle_planks", "minecraft:jungle_log", "minecraft:jungle_planks", "minecraft:jungle", "jungle", PANE);
    static final Pal FREM = new Pal("minecraft:spruce_planks", "minecraft:spruce_log", "minecraft:spruce_planks", "thatch", "spruce", "minecraft:spruce_fence");
    static final Pal FREM_DARK = new Pal("minecraft:dark_oak_planks", "minecraft:stripped_spruce_log", "minecraft:spruce_planks", "minecraft:dark_oak", "dark_oak", "minecraft:dark_oak_fence");
    static final Pal YANILLE = new Pal("minecraft:stone_bricks", "minecraft:stripped_oak_log", "minecraft:oak_planks", "minecraft:spruce", "oak", PANE);
    static final Pal ELF = new Pal("minecraft:stripped_birch_log", "minecraft:birch_log", "minecraft:birch_planks", "minecraft:birch", "birch", "minecraft:light_blue_stained_glass_pane");
    static final Pal CRYSTAL = new Pal("minecraft:calcite", "minecraft:quartz_pillar", "minecraft:smooth_quartz", "minecraft:prismarine_brick", "birch", "minecraft:light_blue_stained_glass_pane");
    static final Pal TROLL = new Pal("minecraft:cobbled_deepslate", "minecraft:polished_blackstone", "minecraft:cobbled_deepslate", "minecraft:deepslate_tile", "dark_oak", "minecraft:iron_bars");

    final StructureDef.Builder b;
    final int g;
    /** Deferred finishing details (textures, shutters, chimneys...), appended on {@link #build}. */
    final MisthalinDetail d;

    KandarinKit(String id, String name) {
        this.b = StructureDef.builder(id, name);
        this.g = b.ground();
        this.d = new MisthalinDetail(id);
    }

    private KandarinKit(StructureDef.Builder b, int g, MisthalinDetail d) {
        this.b = b;
        this.g = g;
        this.d = d;
    }

    /** A view of the same structure whose relative heights start {@code dy} blocks higher (platforms, upper decks). */
    KandarinKit raised(int dy) {
        return new KandarinKit(b, g + dy, d);
    }

    KandarinKit replaces(String place) {
        b.replaces(place);
        return this;
    }

    StructureDef build() {
        d.flush(b);
        return b.build();
    }

    // ------------------------------------------------------------------ coordinates

    static int X(int tx) {
        return bx(tx);
    }

    static int Z(int ty) {
        return bz(ty);
    }

    /** West block x of a tile rect. */
    static int x1(int a, int c) {
        return bx(Math.min(a, c));
    }

    static int x2(int a, int c) {
        return bx(Math.max(a, c)) + 1;
    }

    /** North block z of a tile rect. */
    static int z1(int a, int c) {
        return bz(Math.max(a, c));
    }

    static int z2(int a, int c) {
        return bz(Math.min(a, c)) + 1;
    }

    // ------------------------------------------------------------------ primitives (block x/z, relative y)

    KandarinKit fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.fill(x1, g + y1, z1, x2, g + y2, z2, block);
        return this;
    }

    KandarinKit walls(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.walls(x1, g + y1, z1, x2, g + y2, z2, block);
        return this;
    }

    KandarinKit clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        b.clear(x1, g + y1, z1, x2, g + y2, z2);
        return this;
    }

    KandarinKit set(int x, int y, int z, String block) {
        b.set(x, g + y, z, block);
        return this;
    }

    KandarinKit fillAir(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        b.op(StructureDef.Op.Type.FILL_AIR, x1, g + y1, z1, x2, g + y2, z2, block, null);
        return this;
    }

    KandarinKit replace(int x1, int y1, int z1, int x2, int y2, int z2, String from, String to) {
        b.replace(x1, g + y1, z1, x2, g + y2, z2, from, to);
        return this;
    }

    // ------------------------------------------------------------------ tile primitives

    KandarinKit flatten(int tx1, int ty1, int tx2, int ty2) {
        b.tFlatten(tx1, ty1, tx2, ty2, GRASS);
        return this;
    }

    KandarinKit flatten(int tx1, int ty1, int tx2, int ty2, String top) {
        b.tFlatten(tx1, ty1, tx2, ty2, top);
        return this;
    }

    KandarinKit tFill(int tx1, int ty1, int tx2, int ty2, int y1, int y2, String block) {
        return fill(x1(tx1, tx2), y1, z1(ty1, ty2), x2(tx1, tx2), y2, z2(ty1, ty2), block);
    }

    KandarinKit tWalls(int tx1, int ty1, int tx2, int ty2, int y1, int y2, String block) {
        return walls(x1(tx1, tx2), y1, z1(ty1, ty2), x2(tx1, tx2), y2, z2(ty1, ty2), block);
    }

    KandarinKit tClear(int tx1, int ty1, int tx2, int ty2, int y1, int y2) {
        return clear(x1(tx1, tx2), y1, z1(ty1, ty2), x2(tx1, tx2), y2, z2(ty1, ty2));
    }

    /** Ground surface over a tile rect. */
    KandarinKit floor(int tx1, int ty1, int tx2, int ty2, String block) {
        return tFill(tx1, ty1, tx2, ty2, -1, -1, block);
    }

    /** A road: ground blocks with a sprinkle of a second block for texture. */
    KandarinKit road(int tx1, int ty1, int tx2, int ty2, String main, String speck) {
        int ax = x1(tx1, tx2);
        int az = z1(ty1, ty2);
        int cx = x2(tx1, tx2);
        int cz = z2(ty1, ty2);
        fill(ax, -1, az, cx, -1, cz, main);
        if (speck != null) {
            for (int x = ax; x <= cx; x++) {
                for (int z = az; z <= cz; z++) {
                    if (hash(x, z) % 7 == 0) {
                        set(x, -1, z, speck);
                    }
                }
            }
        }
        return this;
    }

    /** Places a block on the north-west block of a tile at height y. */
    KandarinKit at(int tx, int ty, int y, String block) {
        return set(bx(tx), y, bz(ty), block);
    }

    static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    // ------------------------------------------------------------------ circles

    private static int halfWidth(int r, int dx) {
        double v = (r + 0.5) * (r + 0.5) - dx * dx;
        return v < 0 ? -1 : (int) Math.floor(Math.sqrt(v));
    }

    /** Filled disc (cylinder from y1 to y2) centred on a block. */
    KandarinKit disc(int cx, int cz, int y1, int y2, int r, String block) {
        for (int dx = -r; dx <= r; dx++) {
            int w = halfWidth(r, dx);
            if (w >= 0) {
                fill(cx + dx, y1, cz - w, cx + dx, y2, cz + w, block);
            }
        }
        return this;
    }

    /** One-block-thick ring (hollow cylinder) from y1 to y2. */
    KandarinKit ring(int cx, int cz, int y1, int y2, int r, String block) {
        if (r <= 0) {
            return fill(cx, y1, cz, cx, y2, cz, block);
        }
        for (int dx = -r; dx <= r; dx++) {
            int wo = halfWidth(r, dx);
            int wi = Math.abs(dx) <= r - 1 ? halfWidth(r - 1, dx) : -1;
            if (wo < 0) {
                continue;
            }
            if (wi < 0) {
                fill(cx + dx, y1, cz - wo, cx + dx, y2, cz + wo, block);
            } else {
                fill(cx + dx, y1, cz - wo, cx + dx, y2, cz - wi - 1, block);
                fill(cx + dx, y1, cz + wi + 1, cx + dx, y2, cz + wo, block);
            }
        }
        return this;
    }

    /** Ellipsoid blob of {@code block} filling only air (canopies, rocks). */
    KandarinKit blob(int cx, int cy, int cz, int rx, int ry, int rz, String block, boolean onlyAir) {
        for (int dy = -ry; dy <= ry; dy++) {
            double f = 1.0 - (dy * dy) / (double) ((ry + 0.5) * (ry + 0.5));
            if (f <= 0) {
                continue;
            }
            double ax = (rx + 0.5) * Math.sqrt(f);
            double az = (rz + 0.5) * Math.sqrt(f);
            int ix = (int) Math.floor(ax);
            for (int dx = -ix; dx <= ix; dx++) {
                double v = 1.0 - (dx * dx) / (ax * ax);
                if (v < 0) {
                    continue;
                }
                int w = (int) Math.floor(az * Math.sqrt(v));
                if (onlyAir) {
                    fillAir(cx + dx, cy + dy, cz - w, cx + dx, cy + dy, cz + w, block);
                } else {
                    fill(cx + dx, cy + dy, cz - w, cx + dx, cy + dy, cz + w, block);
                }
            }
        }
        return this;
    }

    // ------------------------------------------------------------------ small fittings

    /** A ladder at (x,z) from y1 to y2 facing away from its support, only if the support shares the chunk. */
    boolean ladder(int x, int z, int y1, int y2, String facing) {
        int sx = x;
        int sz = z;
        switch (facing) {
            case "north" -> sz = z + 1;
            case "south" -> sz = z - 1;
            case "east" -> sx = x - 1;
            default -> sx = x + 1;
        }
        if ((sx >> 4) != (x >> 4) || (sz >> 4) != (z >> 4)) {
            return false;
        }
        fill(x, y1, z, x, y2, z, "minecraft:ladder[facing=" + facing + "]");
        return true;
    }

    /** A wall torch at (x,z) facing away from its support, skipped across chunk borders. */
    void torch(int x, int y, int z, String facing) {
        int sx = x;
        int sz = z;
        switch (facing) {
            case "north" -> sz = z + 1;
            case "south" -> sz = z - 1;
            case "east" -> sx = x - 1;
            default -> sx = x + 1;
        }
        if ((sx >> 4) == (x >> 4) && (sz >> 4) == (z >> 4)) {
            set(x, y, z, "minecraft:wall_torch[facing=" + facing + "]");
        }
    }

    /** A doorway (1 wide, 2 high) in a wall at (x,z) with a real door (the placer adds the upper half). */
    void doorway(int x, int z, char side, String wood) {
        clear(x, 0, z, x, 1, z);
        String facing = switch (side) {
            case 'n' -> "north";
            case 's' -> "south";
            case 'e' -> "east";
            default -> "west";
        };
        set(x, 0, z, "minecraft:" + wood + "_door[facing=" + facing + ",half=lower]");
    }

    /** A table (fence + pressure plate) with stair chairs either side along x. */
    void table(int x, int y, int z, String wood) {
        set(x, y, z, "minecraft:" + wood + "_fence");
        set(x, y + 1, z, "minecraft:" + wood + "_pressure_plate");
        set(x - 1, y, z, "minecraft:" + wood + "_stairs[facing=west]");
        set(x + 1, y, z, "minecraft:" + wood + "_stairs[facing=east]");
    }

    /** A long table of top slabs from x1 to x2 at z with benches either side. */
    void longTable(int x1, int x2, int y, int z, String wood) {
        fill(x1, y, z, x2, y, z, "minecraft:" + wood + "_slab[type=top]");
        fill(x1, y, z - 1, x2, y, z - 1, "minecraft:" + wood + "_stairs[facing=north]");
        fill(x1, y, z + 1, x2, y, z + 1, "minecraft:" + wood + "_stairs[facing=south]");
    }

    /** A real bed with its head to the north at (x,z) and its foot at (x,z+1). */
    void bed(int x, int y, int z, String colour) {
        set(x, y, z + 1, "minecraft:" + colour + "_bed[facing=north,part=foot]");
    }

    /** A market stall: the station block under a striped canopy on fence posts. */
    void stall(int x, int z, String stallBlock, String wool, String wood) {
        set(x, 0, z, stallBlock);
        set(x - 1, 0, z, "minecraft:" + wood + "_slab[type=top]");
        set(x + 1, 0, z, "minecraft:barrel[facing=up]");
        String post = "minecraft:" + wood + "_fence";
        fill(x - 1, 0, z - 1, x - 1, 2, z - 1, post);
        fill(x + 1, 0, z - 1, x + 1, 2, z - 1, post);
        fill(x - 1, 1, z + 1, x - 1, 2, z + 1, post);
        fill(x + 1, 1, z + 1, x + 1, 2, z + 1, post);
        fill(x - 1, 3, z - 1, x + 1, 3, z + 1, "minecraft:" + wool + "_wool");
        fill(x, 3, z - 1, x, 3, z + 1, "minecraft:white_wool");
        // a scalloped valance of banners under the front of the awning, a ridge and goods on the counter
        d.banner(x - 1, g + 3, z + 2, 's', wool);
        d.banner(x, g + 3, z + 2, 's', "white");
        d.banner(x + 1, g + 3, z + 2, 's', wool);
        d.air(x - 1, g + 4, z - 1, x + 1, g + 4, z - 1, "minecraft:" + wood + "_slab");
        String goods = stallGoods(stallBlock);
        if (goods != null) {
            d.air(x - 1, g + 1, z, x - 1, g + 1, z, goods);
        }
        d.air(x + 1, g + 1, z, x + 1, g + 1, z, "minecraft:" + wood + "_trapdoor[half=bottom]");
    }

    /** A block that reads as the wares of a market stall (placed on its counter), or null. */
    static String stallGoods(String stallBlock) {
        String s = stallBlock.toLowerCase();
        if (s.contains("cake") || s.contains("bakery")) {
            return "minecraft:cake";
        }
        if (s.contains("silk")) {
            return "minecraft:purple_carpet";
        }
        if (s.contains("silver")) {
            return "minecraft:chain";
        }
        if (s.contains("gem")) {
            return "minecraft:amethyst_cluster";
        }
        if (s.contains("spice")) {
            return "minecraft:potted_orange_tulip";
        }
        if (s.contains("fur")) {
            return "minecraft:brown_carpet";
        }
        if (s.contains("fish")) {
            return "minecraft:dried_kelp_block";
        }
        if (s.contains("veg") || s.contains("fruit")) {
            return "minecraft:melon";
        }
        if (s.contains("tea")) {
            return "minecraft:flower_pot";
        }
        return "minecraft:flower_pot";
    }

    /** Fence around a block rect at ground level (gate gaps left to the caller). */
    void fenceB(int x1, int z1, int x2, int z2, String fence) {
        walls(x1, 0, z1, x2, 0, z2, fence);
    }

    void fence(int tx1, int ty1, int tx2, int ty2, String fence) {
        fenceB(x1(tx1, tx2), z1(ty1, ty2), x2(tx1, tx2), z2(ty1, ty2), fence);
    }

    /** A lamp post: fence column with a lantern on top. */
    void lamp(int x, int z, String fence) {
        set(x, 0, z, "minecraft:cobblestone_wall");
        fill(x, 1, z, x, 2, z, fence);
        set(x, 3, z, "minecraft:lantern");
        d.air(x, g + 4, z, x, g + 4, z, "minecraft:spruce_trapdoor[half=bottom]");
    }

    /** A well: stone ring with water and a little roof. */
    void well(int x, int z) {
        fill(x - 1, -3, z - 1, x + 1, -1, z + 1, COBBLE);
        fill(x, -3, z, x, -1, z, WATER);
        walls(x - 1, 0, z - 1, x + 1, 0, z + 1, "minecraft:cobblestone_wall");
        fill(x - 1, 1, z - 1, x - 1, 2, z - 1, "minecraft:oak_fence");
        fill(x + 1, 1, z + 1, x + 1, 2, z + 1, "minecraft:oak_fence");
        fill(x - 1, 1, z + 1, x - 1, 2, z + 1, "minecraft:oak_fence");
        fill(x + 1, 1, z - 1, x + 1, 2, z - 1, "minecraft:oak_fence");
        fill(x - 1, 3, z - 1, x + 1, 3, z - 1, "minecraft:spruce_stairs[facing=south]");
        fill(x - 1, 3, z + 1, x + 1, 3, z + 1, "minecraft:spruce_stairs[facing=north]");
        fill(x - 1, 3, z, x + 1, 3, z, "minecraft:spruce_planks");
        fill(x - 1, 4, z, x + 1, 4, z, "minecraft:spruce_slab");
        set(x, 2, z, "minecraft:chain");
        set(x, 1, z, "minecraft:lantern[hanging=true]");
    }

    // ------------------------------------------------------------------ nature

    /** A deciduous tree. */
    void tree(int x, int z, int h, String log, String leaves) {
        String lv = leaves + "[persistent=true]";
        fill(x - 2, h - 3, z - 1, x + 2, h - 2, z + 1, lv);
        fill(x - 1, h - 3, z - 2, x + 1, h - 2, z + 2, lv);
        fill(x - 1, h - 1, z - 1, x + 1, h - 1, z + 1, lv);
        fill(x - 1, h, z, x + 1, h, z, lv);
        fill(x, h, z - 1, x, h, z + 1, lv);
        fill(x, 0, z, x, h - 1, z, log);
    }

    void oak(int x, int z, int h) {
        tree(x, z, h, "minecraft:oak_log", "minecraft:oak_leaves");
    }

    /** A big rounded tree (yews, maples, gnome trees). */
    void bigTree(int x, int z, int h, int r, String log, String leaves) {
        blob(x, h, z, r, Math.max(2, r - 1), r, leaves + "[persistent=true]", true);
        fill(x, 0, z, x, h, z, log);
        fill(x + 1, 0, z, x + 1, h / 2, z, log);
        fill(x, 0, z + 1, x, h / 3, z + 1, log);
    }

    /** A conical spruce. */
    void spruce(int x, int z, int h) {
        String lv = "minecraft:spruce_leaves[persistent=true]";
        int r = 2;
        for (int y = h; y >= 3; y--) {
            int rr = Math.min(r, (h - y + 1) / 2);
            if (rr <= 0) {
                set(x, y, z, lv);
            } else {
                fill(x - rr, y, z, x + rr, y, z, lv);
                fill(x, y, z - rr, x, y, z + rr, lv);
                if (rr >= 2) {
                    fill(x - 1, y, z - 1, x + 1, y, z + 1, lv);
                }
            }
        }
        set(x, h + 1, z, lv);
        fill(x, 0, z, x, h - 1, z, "minecraft:spruce_log");
    }

    /** Rows of a crop on hydrated farmland with a water channel every 8 rows (rows run along x). */
    void farm(int tx1, int ty1, int tx2, int ty2, String crop) {
        int ax = x1(tx1, tx2);
        int az = z1(ty1, ty2);
        int cx = x2(tx1, tx2);
        int cz = z2(ty1, ty2);
        fill(ax, -1, az, cx, -1, cz, "minecraft:farmland[moisture=7]");
        for (int z = az + 3; z <= cz; z += 8) {
            fill(ax, -1, z, cx, -1, z, WATER);
        }
        fill(ax, 0, az, cx, 0, cz, crop);
        for (int z = az + 3; z <= cz; z += 8) {
            clear(ax, 0, z, cx, 0, z);
        }
    }

    /** Flax: grass rows with blue cornflowers, a path every third row. */
    void flax(int tx1, int ty1, int tx2, int ty2) {
        int ax = x1(tx1, tx2);
        int az = z1(ty1, ty2);
        int cx = x2(tx1, tx2);
        int cz = z2(ty1, ty2);
        fill(ax, -1, az, cx, -1, cz, GRASS);
        for (int z = az; z <= cz; z++) {
            if ((z - az) % 3 == 2) {
                fill(ax, -1, z, cx, -1, z, "minecraft:coarse_dirt");
            } else {
                fill(ax, 0, z, cx, 0, z, "minecraft:cornflower");
            }
        }
    }

    /** Scatter flowers / grass on the open ground of a block rect (only on air at y 0). */
    void meadow(int ax, int az, int cx, int cz, int every) {
        String[] f = {"minecraft:grass", "minecraft:dandelion", "minecraft:poppy", "minecraft:grass", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:grass"};
        for (int x = ax; x <= cx; x++) {
            for (int z = az; z <= cz; z++) {
                int h = hash(x, z);
                if (h % every == 0) {
                    fillAir(x, 0, z, x, 0, z, f[(h / every) % f.length]);
                }
            }
        }
    }

    /** Sea/lake: water whose surface is one block below the ground. */
    void water(int tx1, int ty1, int tx2, int ty2, int depth) {
        waterB(x1(tx1, tx2), z1(ty1, ty2), x2(tx1, tx2), z2(ty1, ty2), depth);
    }

    void waterB(int ax, int az, int cx, int cz, int depth) {
        clear(ax, -1, az, cx, 2, cz);
        fill(ax, -1 - depth, az, cx, -2, cz, WATER);
        fill(ax, -2 - depth, az, cx, -2 - depth, cz, "minecraft:sand");
    }

    /** A wooden pier deck over water with log piles and rope rails. */
    void pier(int tx1, int ty1, int tx2, int ty2, String wood, boolean rails) {
        int ax = x1(tx1, tx2);
        int az = z1(ty1, ty2);
        int cx = x2(tx1, tx2);
        int cz = z2(ty1, ty2);
        fill(ax, -1, az, cx, -1, cz, "minecraft:" + wood + "_planks");
        String log = "minecraft:" + (wood.equals("dark_oak") ? "dark_oak" : wood) + "_log";
        for (int x = ax; x <= cx; x += 4) {
            fill(x, -6, az, x, 0, az, log);
            fill(x, -6, cz, x, 0, cz, log);
        }
        fill(cx, -6, az, cx, 0, az, log);
        fill(cx, -6, cz, cx, 0, cz, log);
        for (int z = az; z <= cz; z += 4) {
            fill(ax, -6, z, ax, 0, z, log);
            fill(cx, -6, z, cx, 0, z, log);
        }
        if (rails) {
            boolean alongX = cx - ax >= cz - az;
            String fence = "minecraft:" + wood + "_fence";
            if (alongX) {
                fill(ax, 0, az, cx, 0, az, fence);
                fill(ax, 0, cz, cx, 0, cz, fence);
            } else {
                fill(ax, 0, az, ax, 0, cz, fence);
                fill(cx, 0, az, cx, 0, cz, fence);
            }
        }
        for (int x = ax; x <= cx; x += 8) {
            set(x, 1, az, "minecraft:lantern");
        }
    }

    /**
     * A Fremennik longboat on the water centred on block (x,z): hull, shields, mast and a striped
     * sail. {@code alongX} gives the keel direction.
     */
    void longboat(int x, int z, int len, boolean alongX, String sail) {
        int h = len / 2;
        String plank = "minecraft:spruce_planks";
        String dark = "minecraft:dark_oak_planks";
        for (int i = -h; i <= h; i++) {
            int w = Math.abs(i) >= h - 1 ? 0 : (Math.abs(i) >= h - 3 ? 1 : 2);
            int bow = Math.abs(i) >= h - 1 ? 2 : 0;
            // keel
            boxAlong(alongX, x, z, i, 0, -3, -2, dark);
            // sides
            if (w > 0) {
                boxAlong(alongX, x, z, i, -w + 1, -2, -2, w - 1, plank);
                boxAlong(alongX, x, z, i, -w, -2, 0, plank);
                boxAlong(alongX, x, z, i, w, -2, 0, plank);
                boxAlong(alongX, x, z, i, -w + 1, -1, 0, w - 1, AIR);
                if (w == 2 && (i & 1) == 0) {
                    String shield = (i & 2) == 0 ? "minecraft:red_wool" : "minecraft:yellow_wool";
                    boxAlong(alongX, x, z, i, -w - 1, 0, 0, shield);
                    boxAlong(alongX, x, z, i, w + 1, 0, 0, shield);
                }
            } else {
                boxAlong(alongX, x, z, i, 0, -2, bow, dark);
            }
        }
        // mast and sail
        if (alongX) {
            fill(x, -1, z, x, 9, z, "minecraft:spruce_fence");
            fill(x, 9, z - 3, x, 9, z + 3, "minecraft:stripped_spruce_log[axis=z]");
            for (int y = 4; y <= 8; y++) {
                fill(x + 1, y, z - 3, x + 1, y, z + 3, "minecraft:" + ((y & 1) == 0 ? sail : "white") + "_wool");
            }
        } else {
            fill(x, -1, z, x, 9, z, "minecraft:spruce_fence");
            fill(x - 3, 9, z, x + 3, 9, z, "minecraft:stripped_spruce_log[axis=x]");
            for (int y = 4; y <= 8; y++) {
                fill(x - 3, y, z + 1, x + 3, y, z + 1, "minecraft:" + ((y & 1) == 0 ? sail : "white") + "_wool");
            }
        }
    }

    private void boxAlong(boolean alongX, int x, int z, int i, int across, int y1, int y2, String block) {
        if (alongX) {
            fill(x + i, y1, z + across, x + i, y2, z + across, block);
        } else {
            fill(x + across, y1, z + i, x + across, y2, z + i, block);
        }
    }

    private void boxAlong(boolean alongX, int x, int z, int i, int a1, int y1, int y2, int a2, String block) {
        if (alongX) {
            fill(x + i, y1, z + a1, x + i, y2, z + a2, block);
        } else {
            fill(x + a1, y1, z + i, x + a2, y2, z + i, block);
        }
    }

    // ------------------------------------------------------------------ roofs

    static String full(String base) {
        if (base.endsWith("brick") || base.endsWith("tile")) {
            return base + "s";
        }
        if (base.equals("minecraft:quartz")) {
            return "minecraft:quartz_block";
        }
        for (String w : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"}) {
            if (base.equals("minecraft:" + w)) {
                return base + "_planks";
            }
        }
        return base;
    }

    /**
     * Pitched roof over a block rect starting at relative height y (ridge along the longer axis), with
     * gables of {@code gable}. {@code base} is "thatch" or a stairs base.
     */
    void roof(int x1, int z1, int x2, int z2, int y, String base, String gable) {
        boolean th = base.equals("thatch");
        String st = th ? HAY : base + "_stairs";
        String fl = th ? HAY : full(base);
        boolean alongX = (x2 - x1) >= (z2 - z1);
        if (gable != null) {
            String log = MisthalinDetail.isStone(gable) ? null
                    : gable.contains("terracotta") ? "minecraft:stripped_dark_oak_log" : "minecraft:stripped_spruce_log";
            d.gables(x1, z1, x2, z2, g + y, alongX, gable, log, PANE);
        }
        if (th) {
            // thatch: dark eaves trim along the overhang and a timber ridge cap
            String eave = "minecraft:dark_oak_stairs";
            int span = alongX ? z2 - z1 : x2 - x1;
            int ridge = y + (span + 2) / 2;
            if (alongX) {
                d.replace(x1 - 1, g + y, z1 - 1, x2 + 1, g + y, z1 - 1, HAY, eave + "[facing=south]");
                d.replace(x1 - 1, g + y, z2 + 1, x2 + 1, g + y, z2 + 1, HAY, eave + "[facing=north]");
                d.air(x1 - 1, g + ridge + 1, (z1 + z2) / 2, x2 + 1, g + ridge + 1, (z1 + z2 + 1) / 2, "minecraft:dark_oak_slab");
            } else {
                d.replace(x1 - 1, g + y, z1 - 1, x1 - 1, g + y, z2 + 1, HAY, eave + "[facing=east]");
                d.replace(x2 + 1, g + y, z1 - 1, x2 + 1, g + y, z2 + 1, HAY, eave + "[facing=west]");
                d.air((x1 + x2) / 2, g + ridge + 1, z1 - 1, (x1 + x2 + 1) / 2, g + ridge + 1, z2 + 1, "minecraft:dark_oak_slab");
            }
        }
        for (int i = 0; ; i++) {
            int yy = y + i;
            if (alongX) {
                int a = z1 - 1 + i;
                int c = z2 + 1 - i;
                if (a > c) {
                    break;
                }
                if (a == c) {
                    fill(x1 - 1, yy, a, x2 + 1, yy, a, fl);
                    if (!th) {
                        d.air(x1 - 1, g + yy + 1, a, x2 + 1, g + yy + 1, a, base + "_slab");
                    }
                    break;
                }
                fill(x1 - 1, yy, a, x2 + 1, yy, a, th ? st : st + "[facing=south]");
                fill(x1 - 1, yy, c, x2 + 1, yy, c, th ? st : st + "[facing=north]");
                if (a + 1 <= c - 1) {
                    fill(x1, yy, a + 1, x1, yy, c - 1, gable);
                    fill(x2, yy, a + 1, x2, yy, c - 1, gable);
                } else if (!th) {
                    fill(x1 - 1, yy + 1, a, x2 + 1, yy + 1, c, base + "_slab");
                }
            } else {
                int a = x1 - 1 + i;
                int c = x2 + 1 - i;
                if (a > c) {
                    break;
                }
                if (a == c) {
                    fill(a, yy, z1 - 1, a, yy, z2 + 1, fl);
                    if (!th) {
                        d.air(a, g + yy + 1, z1 - 1, a, g + yy + 1, z2 + 1, base + "_slab");
                    }
                    break;
                }
                fill(a, yy, z1 - 1, a, yy, z2 + 1, th ? st : st + "[facing=east]");
                fill(c, yy, z1 - 1, c, yy, z2 + 1, th ? st : st + "[facing=west]");
                if (a + 1 <= c - 1) {
                    fill(a + 1, yy, z1, c - 1, yy, z1, gable);
                    fill(a + 1, yy, z2, c - 1, yy, z2, gable);
                } else if (!th) {
                    fill(a, yy + 1, z1 - 1, c, yy + 1, z2 + 1, base + "_slab");
                }
            }
        }
    }

    /** Hipped (pyramid) roof of stairs over a block rect. */
    void hipRoof(int x1, int z1, int x2, int z2, int y, String base) {
        boolean th = base.equals("thatch");
        String st = th ? HAY : base + "_stairs";
        for (int i = 0; ; i++) {
            int ax = x1 - 1 + i;
            int cx = x2 + 1 - i;
            int az = z1 - 1 + i;
            int cz = z2 + 1 - i;
            if (ax > cx || az > cz) {
                break;
            }
            if (ax == cx || az == cz) {
                fill(ax, y + i, az, cx, y + i, cz, th ? HAY : full(base));
                break;
            }
            fill(ax, y + i, az, cx, y + i, az, th ? st : st + "[facing=south]");
            fill(ax, y + i, cz, cx, y + i, cz, th ? st : st + "[facing=north]");
            fill(ax, y + i, az + 1, ax, y + i, cz - 1, th ? st : st + "[facing=east]");
            fill(cx, y + i, az + 1, cx, y + i, cz - 1, th ? st : st + "[facing=west]");
        }
    }

    /** Conical roof: shrinking rings from radius r at height y. {@code steep} = layers per radius step. */
    void cone(int cx, int cz, int y, int r, int steep, String block, String tip) {
        int yy = y;
        for (int rr = r; rr >= 1; rr--) {
            for (int s = 0; s < steep; s++) {
                ring(cx, cz, yy, yy, rr, block);
                yy++;
            }
        }
        fill(cx, yy, cz, cx, yy + 1, cz, block);
        if (tip != null) {
            set(cx, yy + 2, cz, tip);
        }
    }

    /** Merlons on the outline of a block rect at height y (every other block). */
    void crenel(int x1, int z1, int x2, int z2, int y, String block) {
        for (int x = x1; x <= x2; x += 2) {
            set(x, y, z1, block);
            set(x, y, z2, block);
        }
        for (int z = z1; z <= z2; z += 2) {
            set(x1, y, z, block);
            set(x2, y, z, block);
        }
        set(x2, y, z1, block);
        set(x2, y, z2, block);
        set(x1, y, z2, block);
    }

    // ------------------------------------------------------------------ buildings

    Bld house(int tx1, int ty1, int tx2, int ty2, int storeys, Pal p, char door, int flags) {
        return houseB(x1(tx1, tx2), z1(ty1, ty2), x2(tx1, tx2), z2(ty1, ty2), storeys, p, door, flags);
    }

    /**
     * A timber/stone house over a block rect: floor, walls with corner posts and beams, upper floors,
     * windows, a doorway on side 'n','s','e','w' (or ' ' for none), a ladder, an optional pitched or flat
     * roof, and simple furniture with {@link #FURN}.
     */
    Bld houseB(int x1, int z1, int x2, int z2, int storeys, Pal p, char door, int flags) {
        int top = storeys * H;
        boolean flat = (flags & FLAT) != 0 || p.roof().startsWith("flat");
        boolean log = p.post().contains("_log") || p.post().endsWith("_wood");
        fill(x1, -1, z1, x2, -1, z2, p.floor());
        clear(x1 + 1, 0, z1 + 1, x2 - 1, top + 1, z2 - 1);
        walls(x1, 0, z1, x2, top - 1, z2, p.wall());
        for (int s = 1; s < storeys; s++) {
            fill(x1 + 1, s * H - 1, z1 + 1, x2 - 1, s * H - 1, z2 - 1, p.floor());
        }
        // posts and beams
        fill(x1, -1, z1, x1, top - 1, z1, p.post());
        fill(x2, -1, z1, x2, top - 1, z1, p.post());
        fill(x1, -1, z2, x1, top - 1, z2, p.post());
        fill(x2, -1, z2, x2, top - 1, z2, p.post());
        String bx = log ? p.post() + "[axis=x]" : p.post();
        String bz = log ? p.post() + "[axis=z]" : p.post();
        for (int s = 1; s <= storeys; s++) {
            int y = s * H - 1;
            fill(x1 + 1, y, z1, x2 - 1, y, z1, bx);
            fill(x1 + 1, y, z2, x2 - 1, y, z2, bx);
            fill(x1, y, z1 + 1, x1, y, z2 - 1, bz);
            fill(x2, y, z1 + 1, x2, y, z2 - 1, bz);
        }
        if (log) {
            for (int x = x1 + 4; x <= x2 - 3; x += 4) {
                fill(x, 0, z1, x, top - 1, z1, p.post());
                fill(x, 0, z2, x, top - 1, z2, p.post());
            }
            for (int z = z1 + 4; z <= z2 - 3; z += 4) {
                fill(x1, 0, z, x1, top - 1, z, p.post());
                fill(x2, 0, z, x2, top - 1, z, p.post());
            }
        }
        if ((flags & NO_WINDOWS) == 0) {
            for (int s = 0; s < storeys; s++) {
                int wy = s * H + 1;
                for (int x = x1 + 2; x <= x2 - 2; x += 4) {
                    set(x, wy, z1, p.window());
                    set(x, wy, z2, p.window());
                }
                for (int z = z1 + 2; z <= z2 - 2; z += 4) {
                    set(x1, wy, z, p.window());
                    set(x2, wy, z, p.window());
                }
            }
        }
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        int dx = mx;
        int dz = mz;
        boolean wide = (flags & WIDE_DOOR) != 0;
        switch (door) {
            case 'n' -> dz = z1;
            case 's' -> dz = z2;
            case 'e' -> dx = x2;
            case 'w' -> dx = x1;
            default -> {
            }
        }
        if (door != ' ') {
            doorway(dx, dz, door, p.wood());
            if (wide) {
                if (door == 'n' || door == 's') {
                    clear(dx + 1, 0, dz, dx + 1, 1, dz);
                } else {
                    clear(dx, 0, dz + 1, dx, 1, dz + 1);
                }
            }
            // step and lintel
            int ox = door == 'e' ? 1 : door == 'w' ? -1 : 0;
            int oz = door == 's' ? 1 : door == 'n' ? -1 : 0;
            set(dx + ox, -1, dz + oz, "minecraft:" + p.wood() + "_planks");
        }
        finishHouse(x1, z1, x2, z2, storeys, p, door, flags, dx, dz, flat);
        // roof
        if (flat) {
            String rb = p.roof().startsWith("flat:") ? p.roof().substring(5) : p.floor();
            fill(x1, top, z1, x2, top, z2, rb);
            crenel(x1, z1, x2, z2, top + 1, p.wall());
        } else {
            if ((flags & NO_CEILING) == 0) {
                fill(x1 + 1, top - 1, z1 + 1, x2 - 1, top - 1, z2 - 1, p.floor());
            }
            roof(x1, z1, x2, z2, top, p.roof(), p.wall());
        }
        // ladder
        int lx = Integer.MIN_VALUE;
        int lz = 0;
        if ((flags & NO_LADDER) == 0 && (storeys > 1 || (flat && (flags & ROOF_LADDER) != 0))) {
            int ly2 = (flags & ROOF_LADDER) != 0 && flat ? top : (storeys - 1) * H;
            int[][] opts = {
                    {x2 - 1, z1 + 1, 0}, {x2 - 1, z1 + 1, 1}, {x1 + 1, z1 + 1, 0}, {x1 + 1, z1 + 1, 2},
                    {x2 - 1, z2 - 1, 3}, {x2 - 1, z2 - 1, 1}, {x1 + 1, z2 - 1, 3}, {x1 + 1, z2 - 1, 2}};
            String[] faces = {"south", "west", "east", "north"};
            for (int[] o : opts) {
                if (Math.abs(o[0] - dx) + Math.abs(o[1] - dz) <= 1) {
                    continue;
                }
                if (ladder(o[0], o[1], 0, ly2, faces[o[2]])) {
                    lx = o[0];
                    lz = o[1];
                    break;
                }
            }
        }
        if ((flags & FURN) != 0) {
            furnish(x1, z1, x2, z2, storeys, p, dx, dz, lx, lz);
        }
        return new Bld(x1, z1, x2, z2, storeys, door, dx, dz);
    }

    /** Wall weathering, plinth, window dressings, door lanterns and a chimney for {@link #houseB} (deferred). */
    private void finishHouse(int x1, int z1, int x2, int z2, int storeys, Pal p, char door, int flags, int dx, int dz, boolean flat) {
        int top = storeys * H;
        String wall = p.wall();
        boolean stone = MisthalinDetail.isStone(wall);
        if (stone) {
            d.texture(x1, z1, x2, z2, g + 1, g + top - 1, wall, 8);
        }
        d.plinth(x1, z1, x2, z2, g, wall, MisthalinDetail.plinthFor(wall));
        String log = p.post().contains("_log") ? MisthalinDetail.bare(p.post()) : "minecraft:stripped_" + p.wood() + "_log";
        if (!log.contains("stripped")) {
            log = log.replace("minecraft:", "minecraft:stripped_");
        }
        String wood = MisthalinDetail.shutterWood(wall, p.wood());
        boolean castle = flat && stone && (storeys >= 2 || x2 - x1 >= 14);
        if ((flags & NO_WINDOWS) == 0) {
            for (int s = 0; s < storeys; s++) {
                int wy = g + s * H + 1;
                for (int x = x1 + 2; x <= x2 - 2; x += 4) {
                    String l = stone ? MisthalinDetail.quoinFor(wall) : log + "[axis=x]";
                    dressWindow(castle, x, wy, z1, 'n', wall, wood, l);
                    dressWindow(castle, x, wy, z2, 's', wall, wood, l);
                }
                for (int z = z1 + 2; z <= z2 - 2; z += 4) {
                    String l = stone ? MisthalinDetail.quoinFor(wall) : log + "[axis=z]";
                    dressWindow(castle, x1, wy, z, 'w', wall, wood, l);
                    dressWindow(castle, x2, wy, z, 'e', wall, wood, l);
                }
            }
        }
        if (castle) {
            int y = g + top - 1;
            d.replace(x1, y, z1, x2, y, z1, wall, MisthalinDetail.quoinFor(wall));
            d.replace(x1, y, z2, x2, y, z2, wall, MisthalinDetail.quoinFor(wall));
            d.replace(x1, y, z1, x1, y, z2, wall, MisthalinDetail.quoinFor(wall));
            d.replace(x2, y, z1, x2, y, z2, wall, MisthalinDetail.quoinFor(wall));
        }
        if (door != ' ') {
            int ox = MisthalinDetail.dx(door);
            int oz = MisthalinDetail.dz(door);
            int px = oz != 0 ? 1 : 0;
            int pz = ox != 0 ? 1 : 0;
            boolean wide = (flags & WIDE_DOOR) != 0;
            d.wallLantern(dx + ox - px, g + 2, dz + oz - pz);
            d.wallLantern(dx + ox + px * (wide ? 2 : 1), g + 2, dz + oz + pz * (wide ? 2 : 1));
        }
        if (!flat && x2 - x1 >= 5 && z2 - z1 >= 5 && MisthalinDetail.noise(x1, 2, z1, 9) % 3 != 0) {
            boolean alongX = (x2 - x1) >= (z2 - z1);
            int span = alongX ? z2 - z1 : x2 - x1;
            String stack = stone ? COBBLE : "minecraft:bricks";
            char[] ends = alongX ? new char[]{'w', 'e'} : new char[]{'n', 's'};
            for (char e : ends) {
                int along = alongX ? z1 + 3 : x1 + 3;
                int mid = alongX ? dz : dx;
                if (door == e && Math.abs(along - mid) <= 2) {
                    continue;
                }
                int cx = alongX ? (e == 'w' ? x1 : x2) : along;
                int cz = alongX ? along : (e == 'n' ? z1 : z2);
                d.chimney(cx, cz, g + 1, g + top + span / 2 + 2, stack);
                break;
            }
        }
    }

    private void dressWindow(boolean castle, int x, int y, int z, char out, String wall, String wood, String lintel) {
        if (castle) {
            d.castleWindow(x, y, y, z, out, wall, lintel, g);
        } else {
            d.window(x, y, y, z, out, wall, wood, lintel, g);
        }
    }

    private void furnish(int x1, int z1, int x2, int z2, int storeys, Pal p, int dx, int dz, int lx, int lz) {
        int ix1 = x1 + 1;
        int ix2 = x2 - 1;
        int iz1 = z1 + 1;
        int iz2 = z2 - 1;
        if (ix2 - ix1 < 2 || iz2 - iz1 < 2) {
            return;
        }
        String w = p.wood();
        int[][] corners = {{ix1, iz1}, {ix2, iz2}, {ix1, iz2}, {ix2, iz1}};
        for (int s = 0; s < storeys; s++) {
            int y = s * H;
            String[] decor = s == 0
                    ? new String[]{"minecraft:barrel[facing=up]", "minecraft:crafting_table", "minecraft:chest[facing=east]", "minecraft:composter"}
                    : new String[]{"bed", "minecraft:bookshelf", "minecraft:chest[facing=west]", "minecraft:barrel[facing=up]"};
            for (int i = 0; i < corners.length; i++) {
                int cx = corners[i][0];
                int cz = corners[i][1];
                if ((cx == lx && cz == lz) || Math.abs(cx - dx) + Math.abs(cz - dz) <= 1) {
                    continue;
                }
                if (decor[i].equals("bed")) {
                    if (cz + 1 <= iz2 && !(cx == lx && cz + 1 == lz)) {
                        bed(cx, y, cz, s % 2 == 0 ? "red" : "blue");
                    }
                } else {
                    set(cx, y, cz, decor[i]);
                }
            }
            int cx = (ix1 + ix2) / 2;
            int cz = (iz1 + iz2) / 2;
            if (ix2 - ix1 >= 4 && iz2 - iz1 >= 3 && !(Math.abs(cx - lx) <= 1 && cz == lz)) {
                table(cx, y, cz, w);
            }
            set(cx, y + 2, cz, "minecraft:lantern[hanging=true]");
            // a rug, a plant and a candle on the bookshelf (only into free floor)
            String rug = s == 0 ? "brown" : "red";
            d.air(Math.max(ix1 + 1, cx - 2), g + y, Math.max(iz1 + 1, cz - 1), Math.min(ix2 - 1, cx + 2), g + y,
                    Math.min(iz2 - 1, cz + 1), "minecraft:" + rug + "_carpet");
            if (s > 0) {
                d.air(corners[1][0], g + y + 1, corners[1][1], corners[1][0], g + y + 1, corners[1][1], "minecraft:candle[candles=2,lit=true]");
            } else {
                d.air(corners[1][0], g + y + 1, corners[1][1], corners[1][0], g + y + 1, corners[1][1], "minecraft:potted_fern");
            }
        }
    }

    /**
     * A round stone tower centred on block (cx,cz): walls, floors every {@code storeyH} blocks with a
     * ladder, slit windows, a doorway and either a crenellated walkway ({@code coneBlock == null}) or a
     * conical roof.
     */
    void towerB(int cx, int cz, int r, int h, String wall, String floor, String coneBlock, char door, int storeyH) {
        d.roundTower(cx, cz, r, g, g + h, storeyH, wall, coneBlock == null && r >= 2);
        disc(cx, cz, -1, -1, r, floor);
        ring(cx, cz, -1, h - 1, r, wall);
        disc(cx, cz, 0, h + 1, r - 1, AIR);
        int lastFloor = 0;
        for (int y = storeyH - 1; y < h - 2; y += storeyH) {
            disc(cx, cz, y, y, r - 1, floor);
            lastFloor = y;
        }
        for (int y = 1; y < h - 1; y += storeyH) {
            set(cx + r, y, cz, "minecraft:glass_pane");
            set(cx - r, y, cz, "minecraft:glass_pane");
            set(cx, y, cz + r, "minecraft:glass_pane");
            set(cx, y, cz - r, "minecraft:glass_pane");
            set(cx + r, y + 1, cz, "minecraft:glass_pane");
            set(cx - r, y + 1, cz, "minecraft:glass_pane");
            set(cx, y + 1, cz + r, "minecraft:glass_pane");
            set(cx, y + 1, cz - r, "minecraft:glass_pane");
        }
        int dx = cx;
        int dz = cz;
        switch (door) {
            case 'n' -> dz = cz - r;
            case 's' -> dz = cz + r;
            case 'e' -> dx = cx + r;
            case 'w' -> dx = cx - r;
            default -> {
            }
        }
        if (door != ' ') {
            clear(dx, 0, dz, dx, 1, dz);
        }
        int top;
        if (coneBlock == null) {
            disc(cx, cz, h - 1, h - 1, r - 1, floor);
            for (int dxx = -r; dxx <= r; dxx++) {
                for (int dzz = -r; dzz <= r; dzz++) {
                    double d = Math.sqrt(dxx * dxx + dzz * dzz);
                    if (d <= r + 0.5 && d > r - 0.5 && ((dxx + dzz) & 1) == 0) {
                        set(cx + dxx, h, cz + dzz, wall);
                    }
                }
            }
            top = h - 1;
        } else {
            disc(cx, cz, h - 1, h - 1, r - 1, floor);
            cone(cx, cz, h, r + 1, 2, coneBlock, null);
            top = h - 1;
        }
        if (r >= 2) {
            // ladder against the wall opposite the door
            String[][] o = {{"w", "e"}, {"e", "w"}, {"n", "s"}, {"s", "n"}};
            for (String[] c : o) {
                char side = c[0].charAt(0);
                if (side == door) {
                    continue;
                }
                int lx = cx;
                int lz = cz;
                String face;
                switch (side) {
                    case 'e' -> {
                        lx = cx + r - 1;
                        face = "west";
                    }
                    case 'w' -> {
                        lx = cx - r + 1;
                        face = "east";
                    }
                    case 'n' -> {
                        lz = cz - r + 1;
                        face = "south";
                    }
                    default -> {
                        lz = cz + r - 1;
                        face = "north";
                    }
                }
                if (ladder(lx, lz, 0, coneBlock == null ? top : Math.max(0, h - 1), face)) {
                    break;
                }
            }
        }
    }

    void tower(int tx, int ty, int r, int h, String wall, String floor, String coneBlock, char door) {
        towerB(bx(tx) + 1, bz(ty) + 1, r, h, wall, floor, coneBlock, door, 5);
    }

    /** A crenellated curtain wall (2 blocks thick, one tile) between two tiles in a straight line. */
    void cwall(int tx1, int ty1, int tx2, int ty2, int h, String block) {
        int ax = x1(tx1, tx2);
        int az = z1(ty1, ty2);
        int cx = x2(tx1, tx2);
        int cz = z2(ty1, ty2);
        fill(ax, -1, az, cx, h - 1, cz, block);
        d.textureSolid(ax, g, az, cx, g + h - 1, cz, block, 6);
        d.replace(ax, g, az, cx, g, cz, block, MisthalinDetail.plinthFor(block));
        for (int x = ax; x <= cx; x += 2) {
            set(x, h, az, block);
            set(x, h, cz, block);
        }
        for (int z = az; z <= cz; z += 2) {
            set(ax, h, z, block);
            set(cx, h, z, block);
        }
    }

    /** Crenellated walls around a tile rect. */
    void cwallRect(int tx1, int ty1, int tx2, int ty2, int h, String block) {
        int ax = Math.min(tx1, tx2);
        int cx = Math.max(tx1, tx2);
        int ay = Math.min(ty1, ty2);
        int cy = Math.max(ty1, ty2);
        cwall(ax, cy, cx, cy, h, block);
        cwall(ax, ay, cx, ay, h, block);
        cwall(ax, ay, ax, cy, h, block);
        cwall(cx, ay, cx, cy, h, block);
    }

    /** Gate gap through a wall over a tile rect, with an arch lintel above and a path below. */
    void gate(int tx1, int ty1, int tx2, int ty2, int h, String floor) {
        tClear(tx1, ty1, tx2, ty2, 0, h - 1);
        floor(tx1, ty1, tx2, ty2, floor);
    }

    /** A gnome hut: round plank walls with a leafy/thatched cone roof. */
    void gnomeHut(int cx, int cz, int r, String wall, String roof, char door) {
        disc(cx, cz, -1, -1, r, "minecraft:jungle_planks");
        ring(cx, cz, 0, 3, r, wall);
        disc(cx, cz, 0, 3, r - 1, AIR);
        cone(cx, cz, 4, r + 1, 1, roof, null);
        set(cx, 0, cz, "minecraft:crafting_table");
        set(cx, 3, cz, "minecraft:lantern[hanging=true]");
        int dx = cx;
        int dz = cz;
        switch (door) {
            case 'n' -> dz = cz - r;
            case 's' -> dz = cz + r;
            case 'e' -> dx = cx + r;
            default -> dx = cx - r;
        }
        clear(dx, 0, dz, dx, 1, dz);
        set(cx + r, 1, cz, PANE);
        set(cx - r, 1, cz, PANE);
        set(cx, 1, cz + r, PANE);
        set(cx, 1, cz - r, PANE);
        if (door == 'n' || door == 's') {
            clear(dx, 1, dz, dx, 1, dz);
        }
    }

    /** A canvas tent (A-frame of wool) along x, open at the west end. */
    void tent(int x1, int z, int len, String wool) {
        String w = "minecraft:" + wool + "_wool";
        for (int i = 0; i < 3; i++) {
            fill(x1, i, z - 2 + i, x1 + len - 1, i, z - 2 + i, w);
            fill(x1, i, z + 2 - i, x1 + len - 1, i, z + 2 - i, w);
        }
        fill(x1, 0, z - 1, x1 + len - 1, 1, z + 1, w);
        fill(x1, 3, z, x1 + len - 1, 3, z, "minecraft:spruce_slab");
        fill(x1, 0, z - 1, x1 + len - 2, 0, z + 1, AIR);
        fill(x1, 1, z, x1 + len - 2, 1, z, AIR);
        set(x1 + len - 2, 0, z - 1, "minecraft:white_carpet");
        set(x1 + len - 2, 0, z + 1, "minecraft:chest[facing=west]");
    }

    /** A straight staircase rising {@code rise} blocks from (x,y,z) towards {@code dir}, clearing headroom. */
    void stairs(int x, int y, int z, char dir, int rise, String base) {
        int sx = dir == 'e' ? 1 : dir == 'w' ? -1 : 0;
        int sz = dir == 's' ? 1 : dir == 'n' ? -1 : 0;
        String facing = switch (dir) {
            case 'e' -> "east";
            case 'w' -> "west";
            case 's' -> "south";
            default -> "north";
        };
        for (int i = 0; i < rise; i++) {
            int px = x + sx * i;
            int pz = z + sz * i;
            clear(px, y + i + 1, pz, px, y + i + 3, pz);
            set(px, y + i, pz, base + "_stairs[facing=" + facing + "]");
            if (i > 0) {
                fill(px, y, pz, px, y + i - 1, pz, full(base));
            }
        }
    }

    /**
     * Shop fit-out: shelves on the back wall (opposite the door) and a counter with the station block
     * in its middle; small rooms just get the station against the back wall.
     */
    void counter(Bld h, String station, String wood) {
        char d = h.door();
        boolean ns = d == 's' || d == 'n' || d == ' ';
        int ua = ns ? h.x1() + 1 : h.z1() + 1;
        int ub = ns ? h.x2() - 1 : h.z2() - 1;
        int depth = ns ? h.z2() - h.z1() - 1 : h.x2() - h.x1() - 1;
        int back;
        int step;
        switch (d) {
            case 'n' -> {
                back = h.z2() - 1;
                step = -1;
            }
            case 'e' -> {
                back = h.x1() + 1;
                step = 1;
            }
            case 'w' -> {
                back = h.x2() - 1;
                step = -1;
            }
            default -> {
                back = h.z1() + 1;
                step = 1;
            }
        }
        int mid = (ua + ub) / 2;
        if (depth >= 6) {
            for (int u = ua; u <= ub; u++) {
                put(ns, u, back, 0, (u & 1) == 0 ? "minecraft:barrel[facing=up]" : "minecraft:bookshelf");
                put(ns, u, back, 1, (u & 1) == 0 ? "minecraft:bookshelf" : "minecraft:barrel[facing=up]");
            }
            int cv = back + 2 * step;
            for (int u = ua + 1; u <= ub - 1; u++) {
                put(ns, u, cv, 0, "minecraft:" + wood + "_slab[type=top]");
            }
            put(ns, mid, cv, 0, station);
            put(ns, ua + 1, cv, 1, "minecraft:lantern");
        } else {
            put(ns, mid, back, 0, station);
            if (ub - ua >= 2) {
                put(ns, mid - 1, back, 0, "minecraft:barrel[facing=up]");
                put(ns, mid + 1, back, 0, "minecraft:" + wood + "_slab[type=top]");
            }
        }
    }

    private void put(boolean ns, int u, int v, int y, String block) {
        if (ns) {
            set(u, y, v, block);
        } else {
            set(v, y, u, block);
        }
    }

    /** Agility course obstacle block state. */
    static String obstacle(int course, int step) {
        return "runecraft:agility_obstacle[course=" + course + ",step=" + step + "]";
    }

    /** A flag pole with a two-colour banner of wool. */
    void flag(int x, int y, int z, String c1, String c2) {
        fill(x, y, z, x, y + 5, z, "minecraft:spruce_fence");
        fill(x + 1, y + 3, z, x + 2, y + 3, z, "minecraft:" + c1 + "_wool");
        fill(x + 1, y + 4, z, x + 2, y + 4, z, "minecraft:" + c2 + "_wool");
        fill(x + 1, y + 5, z, x + 2, y + 5, z, "minecraft:" + c1 + "_wool");
    }

    /** Clears a diagonal passage through a round tower's wall towards (sx, sz) = (+-1, +-1). */
    void towerDiagDoor(int cx, int cz, int r, int sx, int sz) {
        int b = (int) Math.round(r * 0.7);
        for (int a = b - 1; a <= b; a++) {
            for (int c = b - 1; c <= b; c++) {
                clear(cx + sx * a, 0, cz + sz * c, cx + sx * a, 1, cz + sz * c);
            }
        }
    }

    /** A square bell tower (5x5 blocks) with its south-west corner at (x1, z2), a bell and a hipped roof. */
    void belfry(int x1, int z2, int h, String wall, String roofBase) {
        walls(x1, 0, z2 - 4, x1 + 4, h - 1, z2, wall);
        clear(x1 + 1, 0, z2 - 3, x1 + 3, h - 1, z2 - 1);
        fill(x1 + 1, h - 4, z2 - 3, x1 + 3, h - 4, z2 - 1, "minecraft:spruce_planks");
        fill(x1 + 2, h - 1, z2 - 2, x1 + 2, h - 1, z2 - 2, wall);
        set(x1 + 2, h - 2, z2 - 2, "minecraft:bell[attachment=ceiling]");
        clear(x1 + 2, h - 3, z2, x1 + 2, h - 2, z2);
        clear(x1, h - 3, z2 - 2, x1, h - 2, z2 - 2);
        clear(x1 + 4, h - 3, z2 - 2, x1 + 4, h - 2, z2 - 2);
        clear(x1 + 2, h - 3, z2 - 4, x1 + 2, h - 2, z2 - 4);
        hipRoof(x1, z2 - 4, x1 + 4, z2, h, roofBase);
        ladder(x1 + 1, z2 - 1, 0, h - 4, "east");
    }
}
