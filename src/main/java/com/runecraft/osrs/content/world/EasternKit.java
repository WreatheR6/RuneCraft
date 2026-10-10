package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.Random;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Shared building kit for the eastern regions (Kharidian Desert, Morytania, the Wilderness): palettes,
 * furnished houses with doors and ladders, walls with crenellations and gates, towers with roofs,
 * circles and domes, pyramids, ruins, trees and furniture. Wraps one {@link StructureDef.Builder}.
 */
final class EasternKit {
    // ---- desert palette ----
    static final String SAND = "minecraft:sand";
    static final String SS = "minecraft:sandstone";
    static final String CUT = "minecraft:cut_sandstone";
    static final String SMOOTH = "minecraft:smooth_sandstone";
    static final String CHIS = "minecraft:chiseled_sandstone";
    static final String SS_WALL = "minecraft:sandstone_wall";
    static final String SS_SLAB = "minecraft:smooth_sandstone_slab";
    static final String TERRA = "minecraft:terracotta";
    static final String ORANGE_TERRA = "minecraft:orange_terracotta";
    static final String WHITE_TERRA = "minecraft:white_terracotta";
    static final String RED_SS = "minecraft:cut_red_sandstone";
    static final String ORANGE_WOOL = "minecraft:orange_wool";
    // ---- morytania palette ----
    static final String DARK_PLANKS = "minecraft:dark_oak_planks";
    static final String DARK_LOG = "minecraft:dark_oak_log";
    static final String STRIPPED_DARK = "minecraft:stripped_dark_oak_log";
    static final String DEEP_BRICKS = "minecraft:deepslate_bricks";
    static final String CRACKED_DEEP = "minecraft:cracked_deepslate_bricks";
    static final String DEEP_TILES = "minecraft:deepslate_tiles";
    static final String POL_DEEP = "minecraft:polished_deepslate";
    static final String COB_DEEP = "minecraft:cobbled_deepslate";
    static final String BLACKSTONE = "minecraft:blackstone";
    static final String PBB = "minecraft:polished_blackstone_bricks";
    static final String CRACKED_PBB = "minecraft:cracked_polished_blackstone_bricks";
    static final String MOSSY_COBBLE = "minecraft:mossy_cobblestone";
    static final String COBWEB = "minecraft:cobweb";
    static final String SOUL_LANTERN = "minecraft:soul_lantern";
    // ---- stone palette ----
    static final String STONE_BRICKS = "minecraft:stone_bricks";
    static final String MOSSY_BRICKS = "minecraft:mossy_stone_bricks";
    static final String CRACKED_BRICKS = "minecraft:cracked_stone_bricks";
    static final String COBBLE = "minecraft:cobblestone";
    static final String GRAVEL = "minecraft:gravel";
    static final String COARSE = "minecraft:coarse_dirt";
    static final String PODZOL = "minecraft:podzol";
    static final String GRASS = "minecraft:grass_block";
    static final String AIR = "minecraft:air";
    static final String WATER = "minecraft:water";
    static final String LAVA = "minecraft:lava";
    // ---- runecraft stations ----
    static final String BANK = "runecraft:bank_booth";
    static final String FURNACE = "runecraft:furnace";
    static final String ANVIL = "runecraft:anvil";
    static final String RANGE = "runecraft:cooking_range";
    static final String ALTAR = "runecraft:prayer_altar";
    static final String GENERAL = "runecraft:general_store";
    static final String WEAPONS = "runecraft:weapon_shop";
    static final String TOOLS = "runecraft:tool_shop";
    static final String RUNES = "runecraft:rune_shop";
    static final String ARCHERY = "runecraft:archery_shop";
    static final String TANNER = "runecraft:tanning_rack";
    static final String CRAFTING = "runecraft:crafting_table";

    /** House style: walls, floor, roof material, pitched or flat, door block (null = open arch), corner posts, window block. */
    record Style(String wall, String floor, String roof, boolean pitched, String door, String corner, String window,
                 String light, String table, String chair, String bed) {
    }

    static final Style DESERT = new Style(CUT, SMOOTH, SMOOTH, false, null, SS, AIR, "minecraft:lantern",
            "minecraft:smooth_sandstone_slab[type=top]", "minecraft:sandstone_stairs", "minecraft:orange_carpet");
    static final Style DESERT_RICH = new Style(SMOOTH, "minecraft:orange_terracotta", "minecraft:cut_sandstone", false,
            "minecraft:jungle_door", CHIS, AIR, "minecraft:lantern", "minecraft:jungle_slab[type=top]",
            "minecraft:jungle_stairs", "minecraft:red_carpet");
    static final Style DESERT_MUD = new Style("minecraft:packed_mud", SMOOTH, "minecraft:mud_bricks", false, null,
            "minecraft:mud_bricks", AIR, "minecraft:lantern", "minecraft:jungle_slab[type=top]",
            "minecraft:jungle_stairs", "minecraft:yellow_carpet");
    static final Style MORY = new Style(DARK_PLANKS, "minecraft:spruce_planks", "minecraft:dark_oak", true,
            "minecraft:dark_oak_door", STRIPPED_DARK, "minecraft:gray_stained_glass_pane", SOUL_LANTERN,
            "minecraft:dark_oak_slab[type=top]", "minecraft:dark_oak_stairs", "minecraft:gray_carpet");
    static final Style MORY_STONE = new Style(DEEP_BRICKS, POL_DEEP, "minecraft:deepslate_tile", true,
            "minecraft:spruce_door", COB_DEEP, "minecraft:gray_stained_glass_pane", SOUL_LANTERN,
            "minecraft:spruce_slab[type=top]", "minecraft:spruce_stairs", "minecraft:black_carpet");
    static final Style PHASMATYS = new Style(MOSSY_BRICKS, "minecraft:spruce_planks", "minecraft:deepslate_tile", true,
            "minecraft:spruce_door", DARK_LOG, "minecraft:lime_stained_glass_pane", SOUL_LANTERN,
            "minecraft:spruce_slab[type=top]", "minecraft:spruce_stairs", "minecraft:green_carpet");
    static final Style DARKMEYER = new Style(PBB, "minecraft:red_nether_bricks", "minecraft:deepslate_tile", true,
            "minecraft:crimson_door", BLACKSTONE, "minecraft:red_stained_glass_pane", SOUL_LANTERN,
            "minecraft:crimson_slab[type=top]", "minecraft:crimson_stairs", "minecraft:red_carpet");
    static final Style WILD = new Style(CRACKED_BRICKS, COBBLE, "minecraft:stone_brick", true, "minecraft:spruce_door",
            MOSSY_BRICKS, "minecraft:iron_bars", "minecraft:lantern", "minecraft:spruce_slab[type=top]",
            "minecraft:spruce_stairs", "minecraft:brown_carpet");
    static final Style FEROX = new Style(STONE_BRICKS, "minecraft:polished_andesite", "minecraft:spruce", true,
            "minecraft:spruce_door", "minecraft:spruce_log", "minecraft:glass_pane", "minecraft:lantern",
            "minecraft:spruce_slab[type=top]", "minecraft:spruce_stairs", "minecraft:blue_carpet");

    final StructureDef.Builder b;
    final int g;

    EasternKit(String id, String name) {
        this.b = StructureDef.builder(id, name);
        this.g = b.ground();
    }

    EasternKit replaces(String place) {
        b.replaces(place);
        return this;
    }

    StructureDef build() {
        return b.build();
    }

    // ---------------- coordinates ----------------

    /** Block rectangle {x1, z1, x2, z2} covered by an inclusive tile rectangle. */
    static int[] r(int tx1, int ty1, int tx2, int ty2) {
        return new int[]{bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1};
    }

    /** Centre block X of a tile (its south-east block, so a radius-r shape is symmetric around the tile). */
    static int cx(int tx) {
        return bx(tx) + 1;
    }

    static int cz(int ty) {
        return bz(ty) + 1;
    }

    // ---------------- primitives ----------------

    EasternKit set(int x, int y, int z, String s) {
        b.set(x, y, z, s);
        return this;
    }

    EasternKit fill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
        b.fill(x1, y1, z1, x2, y2, z2, s);
        return this;
    }

    EasternKit clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        b.clear(x1, y1, z1, x2, y2, z2);
        return this;
    }

    EasternKit flat(int tx1, int ty1, int tx2, int ty2, String top) {
        b.tFlatten(tx1, ty1, tx2, ty2, top);
        return this;
    }

    EasternKit floor(int tx1, int ty1, int tx2, int ty2, String block) {
        b.tFloor(tx1, ty1, tx2, ty2, block);
        return this;
    }

    EasternKit tfill(int tx1, int ty1, int tx2, int ty2, int y1, int y2, String block) {
        b.tFill(tx1, ty1, tx2, ty2, y1, y2, block);
        return this;
    }

    /** Scatter single blocks over a tile rectangle at relative height y (deterministic). */
    EasternKit scatter(int tx1, int ty1, int tx2, int ty2, int y, String block, int count, long seed) {
        int[] q = r(tx1, ty1, tx2, ty2);
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            set(q[0] + rnd.nextInt(q[2] - q[0] + 1), g + y, q[1] + rnd.nextInt(q[3] - q[1] + 1), block);
        }
        return this;
    }

    /** Paints random patches of {@code block} into the ground surface of a tile rectangle. */
    EasternKit patches(int tx1, int ty1, int tx2, int ty2, String block, int count, long seed) {
        int[] q = r(tx1, ty1, tx2, ty2);
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            int x = q[0] + rnd.nextInt(Math.max(1, q[2] - q[0] - 2));
            int z = q[1] + rnd.nextInt(Math.max(1, q[3] - q[1] - 2));
            fill(x, g - 1, z, x + rnd.nextInt(3), g - 1, z + rnd.nextInt(3), block);
        }
        return this;
    }

    // ---------------- houses ----------------

    /**
     * A furnished building: {@link StructureDef.Builder#tBuilding} plus corner posts, windows in the style's
     * material, a framed doorway, a ladder in the north-west corner between storeys, and per storey a table with
     * chairs, a chest, a barrel, a hanging light and (top storey) a bed. Returns its block rectangle.
     */
    int[] house(int tx1, int ty1, int tx2, int ty2, int storeys, Style s, String doorSide) {
        int[] q = hall(tx1, ty1, tx2, ty2, storeys, s, doorSide);
        for (int st = 0; st < storeys; st++) {
            furnish(q, g + st * 4, s, st == storeys - 1);
        }
        return q;
    }

    /** An unfurnished building (walls, floors, roof, corner posts, windows, doorway, ladder, lights). */
    int[] hall(int tx1, int ty1, int tx2, int ty2, int storeys, Style s, String doorSide) {
        int[] q = r(tx1, ty1, tx2, ty2);
        b.tBuilding(tx1, ty1, tx2, ty2, storeys, s.wall(), s.floor(), s.roof(), doorSide, s.pitched());
        int top = g + storeys * 4;
        if (s.pitched()) {
            fill(q[0] + 1, top - 1, q[1] + 1, q[2] - 1, top - 1, q[3] - 1, s.floor());
        }
        if (s.corner() != null) {
            fill(q[0], g, q[1], q[0], top - 1, q[1], s.corner());
            fill(q[2], g, q[1], q[2], top - 1, q[1], s.corner());
            fill(q[0], g, q[3], q[0], top - 1, q[3], s.corner());
            fill(q[2], g, q[3], q[2], top - 1, q[3], s.corner());
        }
        if (s.window() != null && !s.window().equals("minecraft:glass_pane")) {
            b.replace(q[0], g, q[1], q[2], top, q[3], "minecraft:glass_pane", s.window());
        }
        if (doorSide != null) {
            door(q, doorSide, s.door(), s.wall());
        }
        if (storeys > 1) {
            fill(q[0] + 1, g, q[1] + 1, q[0] + 1, g + (storeys - 1) * 4, q[1] + 1, "minecraft:ladder[facing=south]");
        }
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        for (int st = 0; st < storeys; st++) {
            set(mx, g + st * 4 + 2, mz, s.light() + "[hanging=true]");
        }
        return q;
    }

    /** Removes the intermediate floors of a building so it becomes one tall hall (keeps the ladder harmlessly). */
    void openUp(int[] q, int storeys) {
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        for (int st = 1; st < storeys; st++) {
            clear(q[0] + 1, g + st * 4 - 1, q[1] + 1, q[2] - 1, g + st * 4 - 1, q[3] - 1);
            clear(mx, g + st * 4 - 2, mz, mx, g + st * 4 - 2, mz);
        }
        if (storeys > 1) {
            fill(mx, g + 3, mz, mx, g + storeys * 4 - 3, mz, "minecraft:chain");
            set(mx, g + 2, mz, "minecraft:lantern[hanging=true]");
        }
        clear(q[0] + 1, g, q[1] + 1, q[0] + 1, g + storeys * 4 - 2, q[1] + 1);
    }

    /** A well: stone ring around water, posts and a little roof. */
    void well(int x, int z, String rim, String roof) {
        ring(x, g - 1, g, z, 2.5, 1.5, rim);
        fill(x - 1, g - 6, z - 1, x + 1, g - 6, z + 1, rim);
        fill(x - 1, g - 5, z - 1, x + 1, g - 1, z + 1, WATER);
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                fill(x + dx, g, z + dz, x + dx, g + 2, z + dz, "minecraft:oak_fence");
            }
        }
        fill(x - 2, g + 3, z - 2, x + 2, g + 3, z + 2, roof);
        set(x, g + 2, z, "minecraft:chain");
    }

    /** Market stall (3x3 blocks) at a tile: posts, a wool canopy, a counter and the stall block. */
    void stall(int tx, int ty, String block, String wool) {
        int x = bx(tx);
        int z = bz(ty);
        for (int dx : new int[]{0, 2}) {
            for (int dz : new int[]{0, 2}) {
                fill(x + dx, g, z + dz, x + dx, g + 2, z + dz, "minecraft:oak_fence");
            }
        }
        fill(x, g + 3, z, x + 2, g + 3, z + 2, wool);
        set(x + 1, g, z + 2, "minecraft:oak_slab[type=top]");
        set(x + 1, g, z + 1, block);
    }

    /** Tent over a tile rectangle. */
    void tentT(int tx1, int ty1, int tx2, int ty2, String wool) {
        int[] q = r(tx1, ty1, tx2, ty2);
        tent(q[0], q[1], q[2], q[3], wool);
    }

    /** Clears 1x2 window slits every {@code step} blocks around the walls of a block rectangle at height y. */
    void slits(int[] q, int y, int step, String fillWith) {
        for (int x = q[0] + 2; x <= q[2] - 2; x += step) {
            fill(x, y, q[1], x, y + 1, q[1], fillWith);
            fill(x, y, q[3], x, y + 1, q[3], fillWith);
        }
        for (int z = q[1] + 2; z <= q[3] - 2; z += step) {
            fill(q[0], y, z, q[0], y + 1, z, fillWith);
            fill(q[2], y, z, q[2], y + 1, z, fillWith);
        }
    }

    /** Crenellations (every other block pair) around the top of a block rectangle at height y. */
    void crenels(int[] q, int y, String block) {
        for (int x = q[0]; x <= q[2]; x += 3) {
            fill(x, y, q[1], Math.min(x + 1, q[2]), y, q[1], block);
            fill(x, y, q[3], Math.min(x + 1, q[2]), y, q[3], block);
        }
        for (int z = q[1]; z <= q[3]; z += 3) {
            fill(q[0], y, z, q[0], y, Math.min(z + 1, q[3]), block);
            fill(q[2], y, z, q[2], y, Math.min(z + 1, q[3]), block);
        }
    }

    /**
     * Straight staircase of {@code n} steps rising along +x (east) from (x, z), 2 wide in z, whose top
     * step lies in the floor plane at g+n-1 (n = 4 for {@code tBuilding} storeys); opens the floor above.
     */
    void stairsEast(int x, int z, int n, String stairs, String fillBlock) {
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                fill(x + i, g, z, x + i, g + i - 1, z + 1, fillBlock);
            }
            fill(x + i, g + i, z, x + i, g + i, z + 1, stairs + "[facing=east]");
        }
        clear(x, g + n - 1, z, x + n - 2, g + n - 1, z + 1);
    }

    /** Door blocks (or an open arch with a lintel) in the middle of one wall of a block rectangle. */
    void door(int[] q, String side, String door, String wall) {
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        int ax;
        int az;
        int bx2;
        int bz2;
        switch (side) {
            case "north" -> {
                ax = mx;
                az = q[1];
                bx2 = mx + 1;
                bz2 = q[1];
            }
            case "south" -> {
                ax = mx;
                az = q[3];
                bx2 = mx + 1;
                bz2 = q[3];
            }
            case "east" -> {
                ax = q[2];
                az = mz;
                bx2 = q[2];
                bz2 = mz + 1;
            }
            default -> {
                ax = q[0];
                az = mz;
                bx2 = q[0];
                bz2 = mz + 1;
            }
        }
        set(ax, g + 2, az, wall);
        set(bx2, g + 2, bz2, wall);
        if (door != null) {
            // Two-part blocks (doors, beds) cannot be placed by the chunk placer, so the frame gets a
            // trapdoor "transom" in the door material instead.
            String trap = door.replace("_door", "_trapdoor");
            set(ax, g + 2, az, trap + "[half=top]");
            set(bx2, g + 2, bz2, trap + "[half=top]");
            setOutside(q, side, ax, az, "minecraft:stone_pressure_plate");
            setOutside(q, side, bx2, bz2, "minecraft:stone_pressure_plate");
        }
    }

    /** Sets a block on the floor one step outside a doorway position. */
    private void setOutside(int[] q, String side, int x, int z, String block) {
        switch (side) {
            case "north" -> set(x, g, z - 1, block);
            case "south" -> set(x, g, z + 1, block);
            case "east" -> set(x + 1, g, z, block);
            default -> set(x - 1, g, z, block);
        }
    }

    /** Table, chairs, chest, barrel, light and optionally a bed on the storey whose floor is at y-1. */
    void furnish(int[] q, int y, Style s, boolean bed) {
        int x1 = q[0] + 1;
        int z1 = q[1] + 1;
        int x2 = q[2] - 1;
        int z2 = q[3] - 1;
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        set(mx, y + 2, mz, s.light() + "[hanging=true]");
        if (x2 - x1 < 4 || z2 - z1 < 4) {
            return;
        }
        set(mx, y, mz + 1, s.table());
        set(mx - 1, y, mz + 1, s.chair() + "[facing=west]");
        set(mx + 1, y, mz + 1, s.chair() + "[facing=east]");
        set(x2, y, z1, "minecraft:chest[facing=west]");
        set(x2, y, z2, "minecraft:barrel[facing=up]");
        if (bed) {
            set(x1, y, z2, "minecraft:white_carpet");
            set(x1, y, z2 - 1, s.bed());
            set(x1 + 1, y, z2 - 1, s.bed());
        } else {
            set(x1, y, z2, "minecraft:bookshelf");
            set(x1, y + 1, z2, "minecraft:flower_pot");
        }
    }

    /** A furnished building with a shop counter across the back of the room holding a station block. */
    int[] shop(int tx1, int ty1, int tx2, int ty2, int storeys, Style s, String side, String station) {
        int[] q = house(tx1, ty1, tx2, ty2, storeys, s, side);
        switch (side) {
            case "south" -> counterX(q[0] + 2, q[2] - 2, q[1] + 2, g, s.table(), station);
            case "north" -> counterX(q[0] + 2, q[2] - 2, q[3] - 2, g, s.table(), station);
            case "east" -> counterZ(q[0] + 2, q[1] + 2, q[3] - 2, g, s.table(), station);
            default -> counterZ(q[2] - 2, q[1] + 2, q[3] - 2, g, s.table(), station);
        }
        return q;
    }

    /** Desert shop with an awning outside its doorway. */
    int[] desertShop(int tx1, int ty1, int tx2, int ty2, String side, String station, String awning) {
        int[] q = shop(tx1, ty1, tx2, ty2, 1, DESERT, side, station);
        awning(q, side, awning);
        return q;
    }

    /** Desert flat-roofed house with an open doorway, an orange awning, roof clutter and open windows. */
    int[] desertHouse(int tx1, int ty1, int tx2, int ty2, int storeys, String side, String awning) {
        int[] q = house(tx1, ty1, tx2, ty2, storeys, DESERT, side);
        awning(q, side, awning);
        int top = g + storeys * 4;
        set(q[0] + 1, top, q[1] + 1, "minecraft:barrel[facing=up]");
        set(q[2] - 1, top, q[3] - 1, "minecraft:potted_dead_bush");
        return q;
    }

    /** Wool awning on posts outside the doorway of a block rectangle. */
    void awning(int[] q, String side, String wool) {
        if (side == null || wool == null) {
            return;
        }
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        String post = "minecraft:oak_fence";
        switch (side) {
            case "north" -> {
                fill(mx - 1, g + 3, q[1] - 2, mx + 2, g + 3, q[1] - 1, wool);
                fill(mx - 1, g, q[1] - 2, mx - 1, g + 2, q[1] - 2, post);
                fill(mx + 2, g, q[1] - 2, mx + 2, g + 2, q[1] - 2, post);
            }
            case "south" -> {
                fill(mx - 1, g + 3, q[3] + 1, mx + 2, g + 3, q[3] + 2, wool);
                fill(mx - 1, g, q[3] + 2, mx - 1, g + 2, q[3] + 2, post);
                fill(mx + 2, g, q[3] + 2, mx + 2, g + 2, q[3] + 2, post);
            }
            case "east" -> {
                fill(q[2] + 1, g + 3, mz - 1, q[2] + 2, g + 3, mz + 2, wool);
                fill(q[2] + 2, g, mz - 1, q[2] + 2, g + 2, mz - 1, post);
                fill(q[2] + 2, g, mz + 2, q[2] + 2, g + 2, mz + 2, post);
            }
            default -> {
                fill(q[0] - 2, g + 3, mz - 1, q[0] - 1, g + 3, mz + 2, wool);
                fill(q[0] - 2, g, mz - 1, q[0] - 2, g + 2, mz - 1, post);
                fill(q[0] - 2, g, mz + 2, q[0] - 2, g + 2, mz + 2, post);
            }
        }
    }

    /** A shop counter of top slabs across the room at block z (west-east), with a station block in the middle. */
    void counterX(int x1, int x2, int z, int y, String slab, String station) {
        fill(x1, y, z, x2, y, z, slab);
        if (station != null) {
            set((x1 + x2) / 2, y, z, station);
        }
    }

    /** A shop counter running north-south at block x. */
    void counterZ(int x, int z1, int z2, int y, String slab, String station) {
        fill(x, y, z1, x, y, z2, slab);
        if (station != null) {
            set(x, y, (z1 + z2) / 2, station);
        }
    }

    /** Row of bank booths every other block along x at block z, counters between them. */
    void boothsX(int x1, int x2, int z, int y, String counter) {
        for (int x = x1; x <= x2; x++) {
            set(x, y, z, (x - x1) % 2 == 0 ? BANK : counter);
        }
    }

    void boothsZ(int x, int z1, int z2, int y, String counter) {
        for (int z = z1; z <= z2; z++) {
            set(x, y, z, (z - z1) % 2 == 0 ? BANK : counter);
        }
    }

    // ---------------- walls, gates, towers ----------------

    /** Town wall along a tile line, {@code h} blocks tall, with crenellations of {@code cap} on top. */
    void wall(int tx1, int ty1, int tx2, int ty2, int h, String block, String cap) {
        b.tWallLine(tx1, ty1, tx2, ty2, h, block);
        if (cap == null) {
            return;
        }
        int[] q = r(tx1, ty1, tx2, ty2);
        if (q[2] - q[0] >= q[3] - q[1]) {
            for (int x = q[0]; x <= q[2]; x += 3) {
                fill(x, g + h, q[1], Math.min(x + 1, q[2]), g + h, q[3], cap);
            }
        } else {
            for (int z = q[1]; z <= q[3]; z += 3) {
                fill(q[0], g + h, z, q[2], g + h, Math.min(z + 1, q[3]), cap);
            }
        }
    }

    /** Opens a gate through a wall over a tile rectangle, keeping a lintel at {@code h-1}. */
    void gate(int tx1, int ty1, int tx2, int ty2, int h) {
        b.tClear(tx1, ty1, tx2, ty2, 0, h - 2);
    }

    /** Clears one relative layer over a tile rectangle (e.g. a wall cap above a gate). */
    void tClearTop(int tx1, int ty1, int tx2, int ty2, int y) {
        b.tClear(tx1, ty1, tx2, ty2, y, y);
    }

    /** Opens a tower (centred on a tile, radius r) diagonally toward (sx, sz) at the storey starting at y. */
    void towerPassage(int tx, int ty, int sx, int sz, int y, int radius) {
        int x = cx(tx);
        int z = cz(ty);
        clear(Math.min(x + sx, x + radius * sx), y, Math.min(z + sz, z + radius * sz), Math.max(x + sx, x + radius * sx), y + 2,
                Math.max(z + sz, z + radius * sz));
    }

    /** Round tower with a doorway, an inner ladder to the top and an optional cone roof. */
    void tower(int tx, int ty, int radius, int h, String wall, String floor, String doorSide, String roof) {
        b.tTower(tx, ty, radius, h, wall, floor);
        int x = cx(tx);
        int z = cz(ty);
        switch (doorSide == null ? "" : doorSide) {
            case "north" -> clear(x, g, z - radius, x, g + 2, z - radius);
            case "south" -> clear(x, g, z + radius, x, g + 2, z + radius);
            case "east" -> clear(x + radius, g, z, x + radius, g + 2, z);
            case "west" -> clear(x - radius, g, z, x - radius, g + 2, z);
            default -> {
            }
        }
        if (radius >= 2) {
            fill(x, g, z - radius + 1, x, g + h - 1, z - radius + 1, "minecraft:ladder[facing=south]");
        }
        set(x, g + 2, z, "minecraft:lantern[hanging=true]");
        if (roof != null) {
            cone(x, z, g + h + 1, radius + 1, roof);
        }
    }

    /** Stacked shrinking discs forming a cone, starting at {@code y} with radius {@code r}. */
    void cone(int x, int z, int y, int radius, String block) {
        for (int i = 0; i <= radius; i++) {
            disc(x, y + i, z, radius - i + 0.4, block);
        }
    }

    // ---------------- circles ----------------

    void disc(int x, int y, int z, double radius, String block) {
        int rr = (int) Math.floor(radius);
        for (int dz = -rr; dz <= rr; dz++) {
            int w = (int) Math.floor(Math.sqrt(radius * radius - dz * dz));
            fill(x - w, y, z + dz, x + w, y, z + dz, block);
        }
    }

    /** A ring (annulus) between radii, from y1 to y2. */
    void ring(int x, int y1, int y2, int z, double outer, double inner, String block) {
        int ro = (int) Math.floor(outer);
        for (int dz = -ro; dz <= ro; dz++) {
            int wo = (int) Math.floor(Math.sqrt(outer * outer - dz * dz));
            if (dz * dz < inner * inner) {
                int wi = (int) Math.floor(Math.sqrt(inner * inner - dz * dz));
                if (wi < wo) {
                    fill(x - wo, y1, z + dz, x - wi - 1, y2, z + dz, block);
                    fill(x + wi + 1, y1, z + dz, x + wo, y2, z + dz, block);
                }
            } else {
                fill(x - wo, y1, z + dz, x + wo, y2, z + dz, block);
            }
        }
    }

    /** A solid dome (burial mound, hill) of {@code block} with an outer skin of {@code top}. */
    void dome(int x, int z, int y0, double radius, int h, String block, String top) {
        for (int i = 0; i < h; i++) {
            double t = (double) i / h;
            double rr = radius * Math.sqrt(1 - t * t);
            disc(x, y0 + i, z, rr, top);
            if (rr > 1.5) {
                disc(x, y0 + i, z, rr - 1.2, block);
            }
        }
    }

    // ---------------- pyramids ----------------

    /** Stepped pyramid over a block rectangle: each step insets {@code inset} blocks and rises {@code stepH}. */
    int steppedPyramid(int x1, int z1, int x2, int z2, int y, int inset, int stepH, String block, String trim) {
        int yy = y;
        while (x2 - x1 >= 2 && z2 - z1 >= 2) {
            fill(x1, yy, z1, x2, yy + stepH - 1, z2, block);
            if (trim != null) {
                b.walls(x1, yy + stepH - 1, z1, x2, yy + stepH - 1, z2, trim);
            }
            x1 += inset;
            z1 += inset;
            x2 -= inset;
            z2 -= inset;
            yy += stepH;
        }
        return yy;
    }

    /** Smooth pyramid with stair-block faces; returns the apex height. */
    int smoothPyramid(int x1, int z1, int x2, int z2, int y, String full, String stairs) {
        int yy = y;
        while (x2 - x1 >= 2 && z2 - z1 >= 2) {
            fill(x1 + 1, yy, z1 + 1, x2 - 1, yy, z2 - 1, full);
            fill(x1, yy, z1, x2, yy, z1, stairs + "[facing=south]");
            fill(x1, yy, z2, x2, yy, z2, stairs + "[facing=north]");
            fill(x1, yy, z1 + 1, x1, yy, z2 - 1, stairs + "[facing=east]");
            fill(x2, yy, z1 + 1, x2, yy, z2 - 1, stairs + "[facing=west]");
            x1++;
            z1++;
            x2--;
            z2--;
            yy++;
        }
        fill(x1, yy, z1, x2, yy, z2, full);
        return yy;
    }

    // ---------------- ruins & decoration ----------------

    /** Broken walls of random heights around a tile rectangle, a rubble-strewn floor and an opening. */
    void ruin(int tx1, int ty1, int tx2, int ty2, int h, String[] mats, String floor, long seed) {
        int[] q = r(tx1, ty1, tx2, ty2);
        Random rnd = new Random(seed);
        if (floor != null) {
            fill(q[0], g - 1, q[1], q[2], g - 1, q[3], floor);
        }
        int w = q[2] - q[0];
        int d = q[3] - q[1];
        int perim = 2 * (w + d);
        for (int i = 0; i < perim; i += 2) {
            int x;
            int z;
            if (i < w) {
                x = q[0] + i;
                z = q[1];
            } else if (i < w + d) {
                x = q[2];
                z = q[1] + (i - w);
            } else if (i < 2 * w + d) {
                x = q[2] - (i - w - d);
                z = q[3];
            } else {
                x = q[0];
                z = q[3] - (i - 2 * w - d);
            }
            if (rnd.nextInt(7) == 0) {
                continue;
            }
            int hh = Math.max(1, h - rnd.nextInt(h));
            int x2 = Math.min(x + ((i < w || (i >= w + d && i < 2 * w + d)) ? 1 : 0), q[2]);
            int z2 = Math.min(z + ((i < w || (i >= w + d && i < 2 * w + d)) ? 0 : 1), q[3]);
            fill(Math.min(x, x2), g, Math.min(z, z2), Math.max(x, x2), g + hh - 1, Math.max(z, z2), mats[rnd.nextInt(mats.length)]);
        }
        int area = (w + 1) * (d + 1);
        for (int i = 0; i < Math.min(60, area / 14); i++) {
            int x = q[0] + 1 + rnd.nextInt(Math.max(1, w - 1));
            int z = q[1] + 1 + rnd.nextInt(Math.max(1, d - 1));
            String m = mats[rnd.nextInt(mats.length)];
            set(x, g, z, rnd.nextBoolean() ? m : "minecraft:cobblestone_slab");
        }
    }

    /** A standing (optionally broken) pillar of {@code h} blocks with a capital. */
    void pillar(int x, int z, int h, String block, String cap) {
        fill(x, g, z, x, g + h - 1, z, block);
        if (cap != null) {
            set(x, g + h, z, cap);
        }
    }

    /** A palm tree: jungle trunk with a cross of persistent leaves. */
    void palm(int tx, int ty, int h) {
        int x = bx(tx);
        int z = bz(ty);
        fill(x, g, z, x, g + h - 1, z, "minecraft:jungle_log");
        String leaf = "minecraft:jungle_leaves[persistent=true]";
        fill(x - 3, g + h, z, x + 3, g + h, z, leaf);
        fill(x, g + h, z - 3, x, g + h, z + 3, leaf);
        fill(x - 1, g + h, z - 1, x + 1, g + h + 1, z + 1, leaf);
        set(x - 4, g + h - 1, z, leaf);
        set(x + 4, g + h - 1, z, leaf);
        set(x, g + h - 1, z - 4, leaf);
        set(x, g + h - 1, z + 4, leaf);
    }

    /** A leafy tree: a trunk with a rounded blob of persistent leaves. */
    void tree(int tx, int ty, int h, String log, String leaves) {
        int x = bx(tx);
        int z = bz(ty);
        String leaf = leaves + "[persistent=true]";
        fill(x - 2, g + h - 2, z - 2, x + 2, g + h - 1, z + 2, leaf);
        fill(x - 1, g + h, z - 1, x + 1, g + h, z + 1, leaf);
        set(x, g + h + 1, z, leaf);
        fill(x, g, z, x, g + h - 1, z, log);
    }

    /** A dead, leafless tree of dark logs with crooked branches. */
    void deadTree(int tx, int ty, int h) {
        int x = bx(tx);
        int z = bz(ty);
        fill(x, g, z, x, g + h - 1, z, "minecraft:dark_oak_log");
        fill(x + 1, g + h - 2, z, x + 2, g + h - 2, z, "minecraft:dark_oak_log[axis=x]");
        set(x + 2, g + h - 1, z, "minecraft:dark_oak_fence");
        fill(x, g + h - 3, z - 2, x, g + h - 3, z - 1, "minecraft:dark_oak_log[axis=z]");
        set(x, g + h - 2, z - 2, "minecraft:dark_oak_fence");
        set(x - 1, g + h, z, "minecraft:dark_oak_fence");
        set(x, g + h, z, "minecraft:dark_oak_fence");
    }

    /** A gravestone (stone block with a slab cap) and a skull at its foot sometimes. */
    void grave(int x, int z, int variant) {
        switch (variant % 4) {
            case 0 -> {
                set(x, g, z, "minecraft:cobblestone_wall");
            }
            case 1 -> {
                set(x, g, z, "minecraft:mossy_stone_bricks");
                set(x, g + 1, z, "minecraft:mossy_stone_brick_slab");
            }
            case 2 -> set(x, g, z, "minecraft:chiseled_stone_bricks");
            default -> {
                set(x, g, z, "minecraft:andesite_wall");
                set(x, g - 1, z + 1, "minecraft:coarse_dirt");
            }
        }
        set(x, g - 1, z + 1, "minecraft:podzol");
    }

    /** Grid of graves over a tile rectangle (every 3 blocks). */
    void graveyard(int tx1, int ty1, int tx2, int ty2, long seed) {
        int[] q = r(tx1, ty1, tx2, ty2);
        Random rnd = new Random(seed);
        for (int x = q[0] + 1; x < q[2]; x += 3) {
            for (int z = q[1] + 1; z < q[3]; z += 4) {
                if (rnd.nextInt(5) != 0) {
                    grave(x, z, rnd.nextInt(4));
                }
            }
        }
    }

    /** Fountain: stone basin with water and a central spout. */
    void fountain(int x, int z, int radius, String rim, String spout) {
        ring(x, g - 1, g, z, radius + 0.5, radius - 0.5, rim);
        disc(x, g - 2, z, radius - 0.5, rim);
        disc(x, g - 1, z, radius - 0.5, WATER);
        fill(x, g - 1, z, x, g + 1, z, spout);
    }

    /** A canvas tent (A-frame of wool) over a block rectangle running north-south. */
    void tent(int x1, int z1, int x2, int z2, String wool) {
        int w = x2 - x1;
        for (int i = 0; i <= w / 2; i++) {
            fill(x1 + i, g + i, z1, x1 + i, g + i, z2, wool);
            fill(x2 - i, g + i, z1, x2 - i, g + i, z2, wool);
        }
        clear(x1 + 1, g, z1, x2 - 1, g, z1);
        set((x1 + x2) / 2, g, (z1 + z2) / 2, "minecraft:white_carpet");
        set((x1 + x2) / 2, g, (z1 + z2) / 2 - 1, "minecraft:brown_carpet");
    }

    /** Campfire with logs to sit on. */
    void campfire(int x, int z) {
        set(x, g, z, "minecraft:campfire");
        set(x + 2, g, z, "minecraft:oak_log[axis=z]");
        set(x - 2, g, z, "minecraft:oak_log[axis=z]");
    }

    /** A fence line along a tile line (both 2-block rows). */
    void fence(int tx1, int ty1, int tx2, int ty2, String fence) {
        b.tFill(tx1, ty1, tx2, ty2, 0, 0, fence);
    }
}
