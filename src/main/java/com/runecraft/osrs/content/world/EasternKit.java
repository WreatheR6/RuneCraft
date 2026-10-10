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
    static final Style MORY = new Style("minecraft:spruce_planks", "minecraft:spruce_planks", "minecraft:dark_oak", true,
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
    /** The detail kit dressing this builder's buildings. */
    final EasternDetail d;
    /**
     * The Barrows keep the plain kit exactly as it was (another rework owns that structure); everything else
     * gets the detailed buildings, furniture, trees and graves.
     */
    final boolean legacy;

    EasternKit(String id, String name) {
        this.b = StructureDef.builder(id, name);
        this.g = b.ground();
        this.d = new EasternDetail(b);
        this.legacy = id.equals("barrows");
    }

    // ---------------- looks ----------------

    static final EasternDetail.Look L_DESERT = new EasternDetail.Look(CUT, "minecraft:sandstone", ORANGE_TERRA, SMOOTH, false,
            null, null, null, null, "minecraft:lantern", "minecraft:stripped_jungle_log", false, null, false);
    static final Style DESERT_SMOOTH = new Style(SMOOTH, SMOOTH, SMOOTH, false, null, CUT, AIR, "minecraft:lantern",
            "minecraft:smooth_sandstone_slab[type=top]", "minecraft:sandstone_stairs", "minecraft:yellow_carpet");
    static final Style DESERT_ROUGH = new Style(SS, SMOOTH, SMOOTH, false, null, CUT, AIR, "minecraft:lantern",
            "minecraft:smooth_sandstone_slab[type=top]", "minecraft:sandstone_stairs", "minecraft:red_carpet");
    static final EasternDetail.Look L_DESERT_SMOOTH = new EasternDetail.Look(SMOOTH, CUT, "minecraft:terracotta", CUT, false,
            null, null, null, null, "minecraft:lantern", "minecraft:stripped_jungle_log", false, null, false);
    static final EasternDetail.Look L_DESERT_ROUGH = new EasternDetail.Look(SS, SMOOTH, RED_SS, CUT, false,
            "minecraft:sandstone_stairs", "minecraft:jungle_trapdoor", null, null, "minecraft:lantern", null, false, null, false);
    static final EasternDetail.Look L_DESERT_RICH = new EasternDetail.Look(SMOOTH, CUT, ORANGE_TERRA, CHIS, false,
            "minecraft:smooth_sandstone_stairs", null, null, null, "minecraft:lantern", CHIS, true, null, false);
    static final EasternDetail.Look L_DESERT_MUD = new EasternDetail.Look("minecraft:packed_mud", "minecraft:mud_bricks",
            "minecraft:stripped_jungle_log", "minecraft:mud_bricks", false, null, null, null, null, "minecraft:lantern",
            "minecraft:stripped_spruce_log", false, null, false);
    static final EasternDetail.Look L_MORY = new EasternDetail.Look("minecraft:spruce_planks", COB_DEEP, DARK_LOG, DARK_LOG, true,
            "minecraft:dark_oak_stairs", "minecraft:dark_oak_trapdoor", "minecraft:stripped_dark_oak_log", COB_DEEP, SOUL_LANTERN,
            null, true, "minecraft:deepslate_tile", true);
    static final EasternDetail.Look L_MORY_STONE = new EasternDetail.Look(DEEP_BRICKS, COB_DEEP, DEEP_TILES, POL_DEEP, false,
            "minecraft:deepslate_brick_stairs", "minecraft:spruce_trapdoor", null, COB_DEEP, SOUL_LANTERN, null, true,
            "minecraft:cobbled_deepslate", true);
    static final EasternDetail.Look L_PHASMATYS = new EasternDetail.Look(MOSSY_BRICKS, MOSSY_COBBLE, "minecraft:stripped_spruce_log",
            DARK_LOG, false, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor", "minecraft:spruce_planks", MOSSY_COBBLE,
            SOUL_LANTERN, null, true, "minecraft:mossy_stone_brick", true);
    static final EasternDetail.Look L_DARKMEYER = new EasternDetail.Look(PBB, "minecraft:polished_blackstone", CRACKED_PBB,
            BLACKSTONE, false, "minecraft:polished_blackstone_brick_stairs", "minecraft:crimson_trapdoor", null, BLACKSTONE,
            SOUL_LANTERN, null, true, null, false);
    static final EasternDetail.Look L_WILD = new EasternDetail.Look(CRACKED_BRICKS, MOSSY_COBBLE, MOSSY_BRICKS, MOSSY_BRICKS, false,
            "minecraft:stone_brick_stairs", "minecraft:spruce_trapdoor", "minecraft:spruce_planks", COBBLE, "minecraft:lantern",
            null, true, "minecraft:mossy_stone_brick", true);
    static final EasternDetail.Look L_FEROX = new EasternDetail.Look(STONE_BRICKS, "minecraft:polished_andesite",
            "minecraft:stripped_spruce_log", "minecraft:spruce_log", false, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor",
            "minecraft:stripped_spruce_log", STONE_BRICKS, "minecraft:lantern", null, true, null, false);
    static final EasternDetail.Look L_TEMPLE = new EasternDetail.Look(STONE_BRICKS, "minecraft:polished_andesite",
            "minecraft:chiseled_stone_bricks", MOSSY_BRICKS, false, "minecraft:stone_brick_stairs", null, null, null,
            "minecraft:lantern", null, true, "minecraft:mossy_stone_brick", false);

    static final EasternDetail.Furniture F_DESERT = new EasternDetail.Furniture("minecraft:jungle", "minecraft:orange_carpet",
            "minecraft:red_carpet", "minecraft:orange_bed", "minecraft:lantern", false);
    static final EasternDetail.Furniture F_MORY = new EasternDetail.Furniture("minecraft:dark_oak", "minecraft:gray_carpet",
            "minecraft:black_carpet", "minecraft:gray_bed", SOUL_LANTERN, true);
    static final EasternDetail.Furniture F_DARK = new EasternDetail.Furniture("minecraft:crimson", "minecraft:red_carpet",
            "minecraft:black_carpet", "minecraft:red_bed", SOUL_LANTERN, true);
    static final EasternDetail.Furniture F_WILD = new EasternDetail.Furniture("minecraft:spruce", "minecraft:brown_carpet",
            "minecraft:gray_carpet", "minecraft:brown_bed", "minecraft:lantern", true);
    static final EasternDetail.Furniture F_FEROX = new EasternDetail.Furniture("minecraft:spruce", "minecraft:blue_carpet",
            "minecraft:light_gray_carpet", "minecraft:blue_bed", "minecraft:lantern", false);

    /** The detail look for a style (styles defined elsewhere get a plain look derived from their blocks). */
    static EasternDetail.Look look(Style s) {
        if (s == DESERT) {
            return L_DESERT;
        } else if (s == DESERT_SMOOTH) {
            return L_DESERT_SMOOTH;
        } else if (s == DESERT_ROUGH) {
            return L_DESERT_ROUGH;
        } else if (s == DESERT_RICH) {
            return L_DESERT_RICH;
        } else if (s == DESERT_MUD) {
            return L_DESERT_MUD;
        } else if (s == MORY) {
            return L_MORY;
        } else if (s == MORY_STONE) {
            return L_MORY_STONE;
        } else if (s == PHASMATYS) {
            return L_PHASMATYS;
        } else if (s == DARKMEYER) {
            return L_DARKMEYER;
        } else if (s == WILD) {
            return L_WILD;
        } else if (s == FEROX) {
            return L_FEROX;
        } else if (s == EasternMorytania.TEMPLE) {
            return L_TEMPLE;
        } else if (s.wall().equals(CUT) || s.wall().equals(SMOOTH) || s.wall().equals(SS)) {
            return L_DESERT.withWall(s.wall());
        }
        return new EasternDetail.Look(s.wall(), null, null, s.corner(), false, s.chair(), null, null, null, s.light(), null, true,
                null, false);
    }

    static EasternDetail.Furniture furniture(Style s) {
        if (s == MORY || s == MORY_STONE || s == PHASMATYS || s == EasternMorytania.TEMPLE) {
            return F_MORY;
        } else if (s == DARKMEYER) {
            return F_DARK;
        } else if (s == WILD) {
            return F_WILD;
        } else if (s == FEROX) {
            return F_FEROX;
        } else if (s.wall().equals(CUT) || s.wall().equals(SMOOTH) || s.wall().equals(SS) || s == DESERT_MUD) {
            return F_DESERT;
        }
        return F_WILD;
    }

    static long seed(int[] q) {
        return q[0] * 341873128712L + q[1] * 132897987541L + q[2] * 31L + q[3];
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
        int[] q = r(tx1, ty1, tx2, ty2);
        take(q[0] - 1, q[1] - 1, q[2] + 1, q[3] + 1);
        int w = q[2] - q[0];
        int dd = q[3] - q[1];
        if (Math.min(w, dd) <= 7 && Math.max(w, dd) >= 12) {
            roads.add(q);
        }
        return this;
    }

    // ---------------- occupancy (for scattering props without blocking doors, roads or buildings) ----------------

    /** Block rectangles already used by buildings, roads, walls, stalls, trees... */
    final java.util.List<int[]> taken = new java.util.ArrayList<>();
    /** Long, narrow paved rectangles (streets) that get lamp posts along their edges. */
    final java.util.List<int[]> roads = new java.util.ArrayList<>();

    void take(int x1, int z1, int x2, int z2) {
        taken.add(new int[]{Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2)});
    }

    boolean free(int x1, int z1, int x2, int z2) {
        for (int[] t : taken) {
            if (x2 >= t[0] && x1 <= t[2] && z2 >= t[1] && z1 <= t[3]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Street dressing for a settlement over a tile rectangle: lamp posts along its streets and {@code count}
     * props (crates, pots, benches, planters, carts...) in the theme ("desert", "mory", "dark", "wild",
     * "ferox") on free ground only.
     */
    void dressTown(int tx1, int ty1, int tx2, int ty2, String theme, int count, long seed) {
        if (legacy) {
            return;
        }
        int[] area = r(tx1, ty1, tx2, ty2);
        Random rnd = new Random(seed);
        String post = switch (theme) {
            case "desert" -> SS_WALL;
            case "mory" -> "minecraft:dark_oak_fence";
            case "dark" -> "minecraft:polished_blackstone_brick_wall";
            case "ferox" -> "minecraft:stone_brick_wall";
            default -> "minecraft:spruce_fence";
        };
        String lantern = theme.equals("mory") || theme.equals("dark") ? SOUL_LANTERN : "minecraft:lantern";
        // lamps along the streets
        for (int[] rd : roads) {
            boolean alongX = rd[2] - rd[0] >= rd[3] - rd[1];
            int len = alongX ? rd[2] - rd[0] : rd[3] - rd[1];
            for (int i = 4; i < len - 2; i += 12) {
                for (int side = 0; side < 2; side++) {
                    int x = alongX ? rd[0] + i + side * 6 : (side == 0 ? rd[0] - 1 : rd[2] + 1);
                    int z = alongX ? (side == 0 ? rd[1] - 1 : rd[3] + 1) : rd[1] + i + side * 6;
                    if (x < area[0] || x > area[2] || z < area[1] || z > area[3]) {
                        continue;
                    }
                    if (freeExceptRoads(x, z, x, z)) {
                        d.lampPost(x, z, null, post, lantern, false);
                        take(x - 1, z - 1, x + 1, z + 1);
                    }
                }
            }
        }
        int placed = 0;
        for (int attempt = 0; attempt < count * 12 && placed < count; attempt++) {
            int x = area[0] + 2 + rnd.nextInt(Math.max(1, area[2] - area[0] - 4));
            int z = area[1] + 2 + rnd.nextInt(Math.max(1, area[3] - area[1] - 4));
            if (!free(x - 1, z - 1, x + 2, z + 2)) {
                continue;
            }
            prop(x, z, theme, rnd);
            take(x - 1, z - 1, x + 2, z + 2);
            placed++;
        }
    }

    /** Free of everything except the 1-block margins around roads (lamps stand on the kerb). */
    private boolean freeExceptRoads(int x1, int z1, int x2, int z2) {
        for (int[] t : taken) {
            boolean isRoadMargin = false;
            for (int[] rd : roads) {
                if (t[0] == rd[0] - 1 && t[1] == rd[1] - 1 && t[2] == rd[2] + 1 && t[3] == rd[3] + 1) {
                    isRoadMargin = true;
                    break;
                }
            }
            if (isRoadMargin) {
                continue;
            }
            if (x2 >= t[0] && x1 <= t[2] && z2 >= t[1] && z1 <= t[3]) {
                return false;
            }
        }
        for (int[] rd : roads) {
            if (x2 >= rd[0] && x1 <= rd[2] && z2 >= rd[1] && z1 <= rd[3]) {
                return false;
            }
        }
        return true;
    }

    /** One street prop at (x, z) (uses x..x+1, z..z+1). */
    private void prop(int x, int z, String theme, Random rnd) {
        int kind = rnd.nextInt(6);
        switch (theme) {
            case "desert" -> {
                switch (kind) {
                    case 0 -> d.crates(x, z, rnd.nextLong());
                    case 1 -> {
                        set(x, g, z, "minecraft:decorated_pot");
                        set(x + 1, g, z, "minecraft:decorated_pot");
                        set(x, g, z + 1, "minecraft:flower_pot");
                    }
                    case 2 -> {
                        set(x, g - 1, z, SAND);
                        fill(x, g, z, x, g + 1 + rnd.nextInt(2), z, "minecraft:cactus");
                        set(x + 1, g, z + 1, "minecraft:dead_bush");
                    }
                    case 3 -> d.bench(x, z, true, "south", "minecraft:jungle");
                    case 4 -> {
                        // a handcart
                        fill(x, g, z, x + 1, g, z, "minecraft:jungle_slab[type=top]");
                        set(x, g + 1, z, "minecraft:hay_block");
                        set(x - 1, g, z, "minecraft:jungle_fence");
                        set(x, g, z + 1, "minecraft:jungle_trapdoor[facing=south,open=true]");
                        set(x + 1, g, z + 1, "minecraft:jungle_trapdoor[facing=south,open=true]");
                    }
                    default -> {
                        fill(x, g, z, x + 1, g, z + 1, "minecraft:orange_carpet");
                        set(x, g, z, "minecraft:red_carpet");
                        set(x + 1, g, z + 1, "minecraft:decorated_pot");
                    }
                }
            }
            case "mory", "dark" -> {
                switch (kind) {
                    case 0 -> d.crates(x, z, rnd.nextLong());
                    case 1 -> grave(x, z, rnd.nextInt(6));
                    case 2 -> {
                        set(x, g, z, "minecraft:barrel[facing=up]");
                        set(x, g + 1, z, COBWEB);
                        set(x + 1, g, z, "minecraft:dead_bush");
                    }
                    case 3 -> d.bench(x, z, true, "south", theme.equals("dark") ? "minecraft:crimson" : "minecraft:dark_oak");
                    case 4 -> {
                        set(x, g - 1, z, "minecraft:soul_soil");
                        set(x, g, z, "minecraft:soul_campfire[lit=false]");
                        set(x + 1, g, z, "minecraft:bone_block");
                    }
                    default -> {
                        fill(x, g, z, x, g + 1, z, theme.equals("dark") ? "minecraft:iron_bars" : "minecraft:dark_oak_fence");
                        set(x, g + 2, z, "minecraft:skeleton_skull");
                        set(x + 1, g - 1, z, "minecraft:mud");
                    }
                }
            }
            case "ferox" -> {
                switch (kind) {
                    case 0, 1 -> d.crates(x, z, rnd.nextLong());
                    case 2 -> d.bench(x, z, true, "south", "minecraft:spruce");
                    case 3 -> d.flowerBed(x - 1, z - 1, x + 2, z + 2, "minecraft:stone_bricks", rnd.nextLong());
                    case 4 -> {
                        set(x, g, z, "minecraft:hay_block");
                        set(x + 1, g, z, "minecraft:hay_block");
                        set(x, g + 1, z, "minecraft:hay_block");
                    }
                    default -> {
                        set(x, g, z, "minecraft:barrel[facing=up]");
                        set(x + 1, g, z, "minecraft:water_cauldron[level=3]");
                    }
                }
            }
            default -> {
                switch (kind) {
                    case 0, 1 -> d.rubble(x, z, new String[]{MOSSY_COBBLE, COBBLE, CRACKED_BRICKS}, rnd.nextLong());
                    case 2 -> {
                        set(x, g, z, "minecraft:bone_block");
                        set(x + 1, g, z, "minecraft:skeleton_skull[rotation=3]");
                    }
                    case 3 -> {
                        set(x, g, z, "minecraft:mossy_cobblestone");
                        set(x + 1, g, z, "minecraft:cobblestone_slab");
                        set(x, g + 1, z, "minecraft:mossy_cobblestone_wall");
                    }
                    case 4 -> {
                        set(x, g, z, "minecraft:campfire[lit=false]");
                        set(x + 1, g, z, "minecraft:spruce_log[axis=z]");
                    }
                    default -> {
                        set(x, g, z, "minecraft:dead_bush");
                        set(x + 1, g, z + 1, "minecraft:dead_bush");
                        set(x, g - 1, z + 1, COARSE);
                    }
                }
            }
        }
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

    /** A furnished room of a given kind on the storey whose floor is at y-1 (detail kit; legacy: plain furnish). */
    void room(int[] q, int y, Style s, EasternDetail.Room type) {
        if (legacy) {
            furnish(q, y, s, type == EasternDetail.Room.BEDROOM);
            return;
        }
        d.room(q, y, type, furniture(s), seed(q) + y);
    }

    /** An unfurnished building (walls, floors, roof, corner posts, windows, doorway, ladder, lights). */
    int[] hall(int tx1, int ty1, int tx2, int ty2, int storeys, Style s, String doorSide) {
        int[] q = r(tx1, ty1, tx2, ty2);
        take(q[0] - 3, q[1] - 3, q[2] + 3, q[3] + 3);
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
        if (!legacy) {
            EasternDetail.Look l = look(s);
            d.facade(q, storeys, l, doorSide, s.window() == null ? "minecraft:glass_pane" : s.window());
            if (s.pitched()) {
                d.roof(q, top, s.roof(), l, doorSide, seed(q));
            } else {
                boolean desert = s.wall().equals(CUT) || s.wall().equals(SMOOTH) || s.wall().equals(SS) || s == DESERT_MUD;
                d.flatRoof(q, top, s.wall(), desert ? (s == DESERT_RICH ? CHIS : SS_WALL) : null,
                        desert ? "minecraft:stripped_jungle_log" : null, s == DESERT_RICH ? null : canopyFor(q), seed(q));
            }
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

    /**
     * Stepped buttresses every 6 blocks along the long sides of block rectangle q, {@code h} blocks tall,
     * skipping the middle (doorways). {@code stairs} is a stairs block id for the weathered slopes.
     */
    void buttresses(int[] q, int h, String block, String stairs) {
        buttresses(q, h, block, stairs, 3);
    }

    /** As {@link #buttresses(int[], int, String, String)}, keeping {@code ends} blocks clear at each corner (towers). */
    void buttresses(int[] q, int h, String block, String stairs, int ends) {
        boolean alongX = q[2] - q[0] >= q[3] - q[1];
        int mid = alongX ? (q[0] + q[2]) / 2 : (q[1] + q[3]) / 2;
        int lo = alongX ? q[0] : q[1];
        int hi = alongX ? q[2] : q[3];
        for (int p = lo + ends; p <= hi - ends; p += 6) {
            if (p >= mid - 2 && p <= mid + 3) {
                continue;
            }
            for (int s = 0; s < 2; s++) {
                int o = s == 0 ? -1 : 1;
                String out = alongX ? (s == 0 ? "north" : "south") : (s == 0 ? "west" : "east");
                String in = EasternDetail.opposite(out);
                int w1 = alongX ? q[s == 0 ? 1 : 3] : q[s == 0 ? 0 : 2];
                if (alongX) {
                    fill(p, g - 1, w1 + o, p, g + h - 2, w1 + o, block);
                    set(p, g + h - 1, w1 + o, stairs + "[facing=" + in + "]");
                    fill(p, g - 1, w1 + 2 * o, p, g + h / 2 - 2, w1 + 2 * o, block);
                    set(p, g + h / 2 - 1, w1 + 2 * o, stairs + "[facing=" + in + "]");
                } else {
                    fill(w1 + o, g - 1, p, w1 + o, g + h - 2, p, block);
                    set(w1 + o, g + h - 1, p, stairs + "[facing=" + in + "]");
                    fill(w1 + 2 * o, g - 1, p, w1 + 2 * o, g + h / 2 - 2, p, block);
                    set(w1 + 2 * o, g + h / 2 - 1, p, stairs + "[facing=" + in + "]");
                }
            }
        }
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
        take(x - 3, z - 3, x + 3, z + 3);
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
        take(x - 1, z - 1, x + 3, z + 4);
        if (!legacy) {
            String stripe = wool.equals("minecraft:white_wool") ? "minecraft:yellow_wool" : "minecraft:white_wool";
            d.stall(x, z, block, wool, stripe, "minecraft:jungle_fence");
            return;
        }
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
        if (!legacy) {
            if (door != null) {
                // A double door (the placer adds the upper halves) under a trapdoor transom.
                String facing = EasternDetail.opposite(side);
                boolean aLeft = side.equals("south") || side.equals("west");
                set(ax, g, az, door + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "left" : "right") + ",open=false]");
                set(bx2, g, bz2, door + "[facing=" + facing + ",half=lower,hinge=" + (aLeft ? "right" : "left") + ",open=false]");
                String trap = door.replace("_door", "_trapdoor");
                set(ax, g + 2, az, trap + "[half=top,facing=" + facing + "]");
                set(bx2, g + 2, bz2, trap + "[half=top,facing=" + facing + "]");
            } else {
                // An open archway: the lintel steps in with upside-down stairs.
                String st = archStairs(wall);
                if (st != null) {
                    boolean x = side.equals("north") || side.equals("south");
                    set(ax, g + 2, az, st + "[facing=" + (x ? "west" : "north") + ",half=top]");
                    set(bx2, g + 2, bz2, st + "[facing=" + (x ? "east" : "south") + ",half=top]");
                }
            }
            return;
        }
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
        if (!legacy) {
            d.room(q, y, bed ? EasternDetail.Room.BEDROOM : EasternDetail.Room.HOUSE, furniture(s), seed(q) + y);
            return;
        }
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
        if (legacy) {
            int[] q = house(tx1, ty1, tx2, ty2, storeys, s, side);
            switch (side) {
                case "south" -> counterX(q[0] + 2, q[2] - 2, q[1] + 2, g, s.table(), station);
                case "north" -> counterX(q[0] + 2, q[2] - 2, q[3] - 2, g, s.table(), station);
                case "east" -> counterZ(q[0] + 2, q[1] + 2, q[3] - 2, g, s.table(), station);
                default -> counterZ(q[2] - 2, q[1] + 2, q[3] - 2, g, s.table(), station);
            }
            return q;
        }
        int[] q = hall(tx1, ty1, tx2, ty2, storeys, s, side);
        d.room(q, g, EasternDetail.Room.SHOP, furniture(s), seed(q));
        for (int st = 1; st < storeys; st++) {
            furnish(q, g + st * 4, s, st == storeys - 1);
        }
        // The counter with wares at its ends; a shelf of goods on the wall behind the shopkeeper.
        String wood = furniture(s).wood();
        switch (side) {
            case "south" -> {
                counterX(q[0] + 2, q[2] - 2, q[1] + 2, g, s.table(), station);
                set(q[0] + 2, g + 1, q[1] + 2, EasternDetail.goods(station, 0));
                set(q[2] - 2, g + 1, q[1] + 2, EasternDetail.goods(station, 1));
                shelfX(q[0] + 2, q[2] - 2, q[1] + 1, wood, station);
            }
            case "north" -> {
                counterX(q[0] + 2, q[2] - 2, q[3] - 2, g, s.table(), station);
                set(q[0] + 2, g + 1, q[3] - 2, EasternDetail.goods(station, 0));
                set(q[2] - 2, g + 1, q[3] - 2, EasternDetail.goods(station, 1));
                shelfX(q[0] + 2, q[2] - 2, q[3] - 1, wood, station);
            }
            case "east" -> {
                counterZ(q[0] + 2, q[1] + 2, q[3] - 2, g, s.table(), station);
                set(q[0] + 2, g + 1, q[1] + 2, EasternDetail.goods(station, 0));
                set(q[0] + 2, g + 1, q[3] - 2, EasternDetail.goods(station, 1));
                shelfZ(q[0] + 1, q[1] + 2, q[3] - 2, wood, station);
            }
            default -> {
                counterZ(q[2] - 2, q[1] + 2, q[3] - 2, g, s.table(), station);
                set(q[2] - 2, g + 1, q[1] + 2, EasternDetail.goods(station, 0));
                set(q[2] - 2, g + 1, q[3] - 2, EasternDetail.goods(station, 1));
                shelfZ(q[2] - 1, q[1] + 2, q[3] - 2, wood, station);
            }
        }
        return q;
    }

    /** Stock behind the counter: a shelf board over the shopkeeper's head and crates at both ends. */
    void shelfX(int x1, int x2, int z, String wood, String station) {
        fill(x1, g + 2, z, x2, g + 2, z, wood + "_slab[type=top]");
        set(x1, g, z, "minecraft:barrel[facing=up]");
        set(x1, g + 1, z, EasternDetail.goods(station, 0));
        set(x2, g, z, "minecraft:barrel[facing=up]");
        set(x2, g + 1, z, EasternDetail.goods(station, 1));
    }

    void shelfZ(int x, int z1, int z2, String wood, String station) {
        fill(x, g + 2, z1, x, g + 2, z2, wood + "_slab[type=top]");
        set(x, g, z1, "minecraft:barrel[facing=up]");
        set(x, g + 1, z1, EasternDetail.goods(station, 0));
        set(x, g, z2, "minecraft:barrel[facing=up]");
        set(x, g + 1, z2, EasternDetail.goods(station, 1));
    }

    /** Stairs matching a wall material for arch lintels, or null. */
    static String archStairs(String wall) {
        return switch (wall) {
            case CUT, SS -> "minecraft:sandstone_stairs";
            case SMOOTH -> "minecraft:smooth_sandstone_stairs";
            case "minecraft:packed_mud", "minecraft:mud_bricks" -> "minecraft:mud_brick_stairs";
            case STONE_BRICKS, CRACKED_BRICKS -> "minecraft:stone_brick_stairs";
            case MOSSY_BRICKS -> "minecraft:mossy_stone_brick_stairs";
            case PBB, CRACKED_PBB -> "minecraft:polished_blackstone_brick_stairs";
            case DEEP_BRICKS, CRACKED_DEEP -> "minecraft:deepslate_brick_stairs";
            default -> null;
        };
    }

    /** A wool colour for a rooftop shade canopy (deterministic per building; sometimes none). */
    static String canopyFor(int[] q) {
        String[] c = {ORANGE_WOOL, "minecraft:white_wool", null, "minecraft:red_wool", "minecraft:yellow_wool", null};
        return c[(int) Math.floorMod(seed(q), (long) c.length)];
    }

    /** Desert shop with an awning outside its doorway. */
    int[] desertShop(int tx1, int ty1, int tx2, int ty2, String side, String station, String awning) {
        int[] q = shop(tx1, ty1, tx2, ty2, 1, DESERT, side, station);
        awning(q, side, awning);
        return q;
    }

    /** Desert flat-roofed house with an open doorway, an orange awning, roof clutter and open windows. */
    int[] desertHouse(int tx1, int ty1, int tx2, int ty2, int storeys, String side, String awning) {
        if (legacy) {
            int[] q = house(tx1, ty1, tx2, ty2, storeys, DESERT, side);
            awning(q, side, awning);
            int top = g + storeys * 4;
            set(q[0] + 1, top, q[1] + 1, "minecraft:barrel[facing=up]");
            set(q[2] - 1, top, q[3] - 1, "minecraft:potted_dead_bush");
            return q;
        }
        // Three kinds of sandstone house so a street doesn't repeat: cut, smooth and rough sandstone.
        Style[] kinds = {DESERT, DESERT_SMOOTH, DESERT_ROUGH};
        Style s = kinds[Math.floorMod(tx1 * 7 + ty1 * 13, 3)];
        int[] q = house(tx1, ty1, tx2, ty2, storeys, s, side);
        awning(q, side, awning);
        int top = g + storeys * 4;
        set(q[2] - 1, top, q[3] - 1, "minecraft:potted_dead_bush");
        if (storeys > 1) {
            // roof hatch over the ladder
            set(q[0] + 1, top - 1, q[1] + 1, "minecraft:jungle_trapdoor[half=top,facing=south]");
            fill(q[0] + 1, top - 3, q[1] + 1, q[0] + 1, top - 2, q[1] + 1, "minecraft:ladder[facing=south]");
        }
        // a water jar and a crate by the door
        int[] p = outsideDoor(q, side);
        if (p != null) {
            set(p[0], g, p[1], "minecraft:decorated_pot");
            set(p[2], g, p[3], Math.floorMod(tx1 + ty1, 2) == 0 ? "minecraft:barrel[facing=up]" : "minecraft:flower_pot");
        }
        return q;
    }

    /** Two cells just outside a doorway, flanking it ({x, z, x, z}), or null. */
    int[] outsideDoor(int[] q, String side) {
        if (side == null) {
            return null;
        }
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        return switch (side) {
            case "north" -> new int[]{mx - 2, q[1] - 1, mx + 3, q[1] - 1};
            case "south" -> new int[]{mx - 2, q[3] + 1, mx + 3, q[3] + 1};
            case "east" -> new int[]{q[2] + 1, mz - 2, q[2] + 1, mz + 3};
            default -> new int[]{q[0] - 1, mz - 2, q[0] - 1, mz + 3};
        };
    }

    /** Wool awning on posts outside the doorway of a block rectangle. */
    void awning(int[] q, String side, String wool) {
        if (side == null || wool == null) {
            return;
        }
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        String post = "minecraft:oak_fence";
        if (!legacy) {
            // Striped canvas on jungle-wood poles, a lantern under it and a mat at the door.
            String stripe = wool.equals("minecraft:white_wool") ? "minecraft:light_gray_wool" : "minecraft:white_wool";
            post = "minecraft:jungle_fence";
            switch (side) {
                case "north" -> {
                    d.awning(mx - 1, q[1] - 2, mx + 2, q[1] - 1, g + 3, wool, stripe, true);
                    fill(mx - 1, g, q[1] - 2, mx - 1, g + 2, q[1] - 2, post);
                    fill(mx + 2, g, q[1] - 2, mx + 2, g + 2, q[1] - 2, post);
                    set(mx + 1, g + 2, q[1] - 2, "minecraft:lantern[hanging=true]");
                }
                case "south" -> {
                    d.awning(mx - 1, q[3] + 1, mx + 2, q[3] + 2, g + 3, wool, stripe, true);
                    fill(mx - 1, g, q[3] + 2, mx - 1, g + 2, q[3] + 2, post);
                    fill(mx + 2, g, q[3] + 2, mx + 2, g + 2, q[3] + 2, post);
                    set(mx + 1, g + 2, q[3] + 2, "minecraft:lantern[hanging=true]");
                }
                case "east" -> {
                    d.awning(q[2] + 1, mz - 1, q[2] + 2, mz + 2, g + 3, wool, stripe, false);
                    fill(q[2] + 2, g, mz - 1, q[2] + 2, g + 2, mz - 1, post);
                    fill(q[2] + 2, g, mz + 2, q[2] + 2, g + 2, mz + 2, post);
                    set(q[2] + 2, g + 2, mz + 1, "minecraft:lantern[hanging=true]");
                }
                default -> {
                    d.awning(q[0] - 2, mz - 1, q[0] - 1, mz + 2, g + 3, wool, stripe, false);
                    fill(q[0] - 2, g, mz - 1, q[0] - 2, g + 2, mz - 1, post);
                    fill(q[0] - 2, g, mz + 2, q[0] - 2, g + 2, mz + 2, post);
                    set(q[0] - 2, g + 2, mz + 1, "minecraft:lantern[hanging=true]");
                }
            }
            return;
        }
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
        int[] q = r(tx1, ty1, tx2, ty2);
        take(q[0] - 2, q[1] - 2, q[2] + 2, q[3] + 2);
        if (!legacy) {
            // Weathered patches, a footing course and buttress pilasters on both faces.
            String v = variantOf(block);
            if (v != null) {
                int len = Math.max(q[2] - q[0], q[3] - q[1]);
                d.weather(q[0], g, q[1], q[2], g + h - 1, q[3], block, v, len / 3, seed(q));
            }
            String acc = accentOf(block);
            String pil = acc != null ? acc : block;
            boolean alongX = q[2] - q[0] >= q[3] - q[1];
            if (alongX) {
                for (int x = q[0] + 6; x < q[2] - 3; x += 10) {
                    fill(x, g - 1, q[1] - 1, x, g + h - 2, q[1] - 1, pil);
                    fill(x, g - 1, q[3] + 1, x, g + h - 2, q[3] + 1, pil);
                }
            } else {
                for (int z = q[1] + 6; z < q[3] - 3; z += 10) {
                    fill(q[0] - 1, g - 1, z, q[0] - 1, g + h - 2, z, pil);
                    fill(q[2] + 1, g - 1, z, q[2] + 1, g + h - 2, z, pil);
                }
            }
            if (acc != null) {
                b.replace(q[0], g, q[1], q[2], g, q[3], block, acc);
            }
        }
        if (cap == null) {
            return;
        }
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
        if (!legacy) {
            // keep the way clear of the wall's pilasters, and frame the gate with them instead
            int[] q = r(tx1, ty1, tx2, ty2);
            boolean alongX = q[2] - q[0] >= q[3] - q[1];
            if (alongX) {
                clear(q[0], g, q[1] - 1, q[2], g + h - 2, q[1] - 1);
                clear(q[0], g, q[3] + 1, q[2], g + h - 2, q[3] + 1);
            } else {
                clear(q[0] - 1, g, q[1], q[0] - 1, g + h - 2, q[3]);
                clear(q[2] + 1, g, q[1], q[2] + 1, g + h - 2, q[3]);
            }
        }
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
        take(cx(tx) - radius - 2, cz(ty) - radius - 2, cx(tx) + radius + 2, cz(ty) + radius + 2);
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
        if (!legacy && radius >= 2) {
            towerDetail(x, z, radius, h, wall, roof);
        }
    }

    /**
     * Tower dressing: a plinth course, string courses every five blocks, arrow slits on the four faces
     * (skipping the doorway), a corbelled parapet or an eave under the cone, and a pennant on top.
     */
    void towerDetail(int x, int z, int radius, int h, String wall, String roof) {
        String accent = accentOf(wall);
        if (accent != null) {
            ringReplaceRound(x, z, radius, g, wall, accent);
            for (int y = g + 5; y < g + h - 1; y += 5) {
                ringReplaceRound(x, z, radius, y, wall, accent);
            }
        }
        for (int y = g + 4; y < g + h - 2; y += 5) {
            String bars = y % 2 == 0 ? "minecraft:air" : "minecraft:iron_bars";
            // (not the north face: the inner ladder hangs on it)
            int[][] faces = {{radius, 0}, {-radius, 0}, {0, radius}};
            for (int[] f : faces) {
                fill(x + f[0], y, z + f[1], x + f[0], y + 1, z + f[1], bars);
            }
        }
        if (roof == null) {
            String flag = wall.contains("sandstone") ? "minecraft:orange_wool" : wall.contains("blackstone")
                    ? "minecraft:red_wool" : wall.contains("deepslate") ? "minecraft:black_wool" : "minecraft:white_wool";
            fill(x, g + h, z, x, g + h + 3, z, "minecraft:spruce_fence");
            fill(x + 1, g + h + 2, z, x + 2, g + h + 3, z, flag);
        } else {
            set(x, g + h + radius + 3, z, "minecraft:lightning_rod");
        }
    }

    /** Replaces {@code from} with {@code to} on a round tower's shell at one height. */
    void ringReplaceRound(int x, int z, int r, int y, String from, String to) {
        for (int dz = -r; dz <= r; dz++) {
            int w = (int) Math.floor(Math.sqrt((r + 0.5) * (r + 0.5) - dz * dz));
            b.replace(x - w, y, z + dz, x - w + 1, y, z + dz, from, to);
            b.replace(x + w - 1, y, z + dz, x + w, y, z + dz, from, to);
        }
        b.replace(x - r, y, z - r, x + r, y, z - r, from, to);
        b.replace(x - r, y, z + r, x + r, y, z + r, from, to);
    }

    /** A weathered variant of a wall material (for patches), or null. */
    static String variantOf(String wall) {
        return switch (wall) {
            case CUT -> SS;
            case SS -> "minecraft:chiseled_sandstone";
            case SMOOTH -> SS;
            case STONE_BRICKS -> MOSSY_BRICKS;
            case CRACKED_BRICKS -> MOSSY_BRICKS;
            case MOSSY_BRICKS -> CRACKED_BRICKS;
            case PBB -> CRACKED_PBB;
            case DEEP_BRICKS -> CRACKED_DEEP;
            case MOSSY_COBBLE -> COBBLE;
            case COBBLE -> MOSSY_COBBLE;
            default -> null;
        };
    }

    /** A contrasting trim block for a wall material, or null. */
    static String accentOf(String wall) {
        return switch (wall) {
            case CUT, SS -> SMOOTH;
            case SMOOTH -> CUT;
            case STONE_BRICKS -> "minecraft:polished_andesite";
            case CRACKED_BRICKS, MOSSY_BRICKS -> STONE_BRICKS;
            case PBB -> "minecraft:polished_blackstone";
            case DEEP_BRICKS -> POL_DEEP;
            case "minecraft:spruce_log" -> "minecraft:stripped_spruce_log";
            default -> null;
        };
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

    /**
     * A steep (2:1) pyramid: every course is a full block layer capped by a ring of stairs, insetting one block
     * per two of height; every fourth course gets a {@code trim} band. Returns the apex height.
     */
    int steepPyramid(int x1, int z1, int x2, int z2, int y, String full, String stairs, String trim) {
        int yy = y;
        int course = 0;
        while (x2 - x1 >= 2 && z2 - z1 >= 2) {
            fill(x1, yy, z1, x2, yy, z2, full);
            if (trim != null && course % 4 == 3) {
                b.walls(x1, yy, z1, x2, yy, z2, trim);
            }
            fill(x1 + 1, yy + 1, z1 + 1, x2 - 1, yy + 1, z2 - 1, full);
            fill(x1, yy + 1, z1, x2, yy + 1, z1, stairs + "[facing=south]");
            fill(x1, yy + 1, z2, x2, yy + 1, z2, stairs + "[facing=north]");
            fill(x1, yy + 1, z1 + 1, x1, yy + 1, z2 - 1, stairs + "[facing=east]");
            fill(x2, yy + 1, z1 + 1, x2, yy + 1, z2 - 1, stairs + "[facing=west]");
            x1++;
            z1++;
            x2--;
            z2--;
            yy += 2;
            course++;
        }
        fill(x1, yy, z1, x2, yy, z2, full);
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
        take(q[0] - 1, q[1] - 1, q[2] + 1, q[3] + 1);
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
        if (!legacy) {
            // Fallen stones heaped against the walls, weeds in the cracks, a doorway arch fragment.
            int piles = Math.max(2, Math.min(8, (w + d) / 8));
            for (int i = 0; i < piles; i++) {
                boolean xs = rnd.nextBoolean();
                int x = xs ? q[0] + 1 + rnd.nextInt(Math.max(1, w - 2)) : (rnd.nextBoolean() ? q[0] + 1 : q[2] - 1);
                int z = xs ? (rnd.nextBoolean() ? q[1] + 1 : q[3] - 1) : q[1] + 1 + rnd.nextInt(Math.max(1, d - 2));
                this.d.rubble(x, z, mats, seed + 1000 + i);
            }
            this.d.overgrowth(q[0], q[1], q[2], q[3], Math.min(80, area / 10), seed + 77);
            for (String m : mats) {
                String v = variantOf(m);
                if (v != null) {
                    this.d.weather(q[0], g, q[1], q[2], g + h, q[3], m, v, Math.max(2, (w + d) / 10), seed + m.length());
                }
            }
        }
    }

    /** A standing (optionally broken) pillar of {@code h} blocks with a capital. */
    void pillar(int x, int z, int h, String block, String cap) {
        take(x - 1, z - 1, x + 1, z + 1);
        fill(x, g, z, x, g + h - 1, z, block);
        if (cap != null) {
            set(x, g + h, z, cap);
        }
    }

    /** A palm tree: jungle trunk with a cross of persistent leaves. */
    void palm(int tx, int ty, int h) {
        int x = bx(tx);
        int z = bz(ty);
        take(x - 2, z - 2, x + 2, z + 2);
        if (!legacy) {
            d.palm(x, z, h + 1, tx * 31L + ty);
            return;
        }
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
        take(x - 2, z - 2, x + 2, z + 2);
        if (!legacy) {
            d.tree(x, z, h + 1, log, leaves, tx * 17L + ty);
            return;
        }
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
        take(x - 2, z - 2, x + 2, z + 2);
        if (!legacy) {
            d.deadTree(x, z, h + 1, tx * 13L + ty);
            return;
        }
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
        if (!legacy) {
            // A sunken mound with a headstone at its head; some with a candle, flowers, a skull or a broken stone.
            set(x, g - 1, z + 1, variant % 2 == 0 ? "minecraft:podzol" : "minecraft:coarse_dirt");
            set(x, g - 1, z + 2, "minecraft:rooted_dirt");
            switch (variant % 6) {
                case 0 -> {
                    set(x, g, z, "minecraft:cobblestone_wall");
                    set(x, g + 1, z, "minecraft:stone_brick_slab");
                }
                case 1 -> {
                    set(x, g, z, MOSSY_BRICKS);
                    set(x, g + 1, z, "minecraft:mossy_stone_brick_wall");
                    set(x, g, z + 1, "minecraft:moss_carpet");
                }
                case 2 -> {
                    set(x, g, z, "minecraft:chiseled_stone_bricks");
                    set(x, g + 1, z, "minecraft:candle[candles=1,lit=true]");
                }
                case 3 -> {
                    set(x, g, z, "minecraft:andesite_wall");
                    set(x, g + 1, z, "minecraft:andesite_wall");
                    set(x, g, z + 1, "minecraft:wither_rose");
                }
                case 4 -> {
                    set(x, g, z, "minecraft:cracked_stone_bricks");
                    set(x, g, z + 1, "minecraft:skeleton_skull[rotation=8]");
                }
                default -> set(x, g, z, "minecraft:cobblestone_slab");
            }
            return;
        }
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
                    grave(x, z, rnd.nextInt(legacy ? 4 : 6));
                }
            }
        }
        if (!legacy && q[2] - q[0] >= 6 && q[3] - q[1] >= 6) {
            // a stone cross and a lantern post among the graves
            int cx = (q[0] + q[2]) / 2 + 1;
            int cz = (q[1] + q[3]) / 2 + 2;
            fill(cx, g, cz, cx, g + 3, cz, "minecraft:stone_bricks");
            set(cx - 1, g + 2, cz, "minecraft:stone_brick_stairs[facing=east,half=top]");
            set(cx + 1, g + 2, cz, "minecraft:stone_brick_stairs[facing=west,half=top]");
            set(cx, g + 4, cz, "minecraft:stone_brick_slab");
        }
    }

    /** Fountain: stone basin with water and a central spout. */
    void fountain(int x, int z, int radius, String rim, String spout) {
        take(x - radius - 2, z - radius - 2, x + radius + 2, z + radius + 2);
        ring(x, g - 1, g, z, radius + 0.5, radius - 0.5, rim);
        disc(x, g - 2, z, radius - 0.5, rim);
        disc(x, g - 1, z, radius - 0.5, WATER);
        fill(x, g - 1, z, x, g + 1, z, spout);
    }

    /** A canvas tent (A-frame of wool) over a block rectangle running north-south. */
    void tent(int x1, int z1, int x2, int z2, String wool) {
        take(x1 - 2, z1 - 2, x2 + 2, z2 + 2);
        int w = x2 - x1;
        for (int i = 0; i <= w / 2; i++) {
            fill(x1 + i, g + i, z1, x1 + i, g + i, z2, wool);
            fill(x2 - i, g + i, z1, x2 - i, g + i, z2, wool);
        }
        clear(x1 + 1, g, z1, x2 - 1, g, z1);
        set((x1 + x2) / 2, g, (z1 + z2) / 2, "minecraft:white_carpet");
        set((x1 + x2) / 2, g, (z1 + z2) / 2 - 1, "minecraft:brown_carpet");
        if (legacy) {
            return;
        }
        // Ridge pole sticking out at both ends, guy ropes to pegs, a bedroll, a chest and a lantern.
        int mx = (x1 + x2) / 2;
        int top = g + w / 2;
        fill(mx, top + 1, z1 - 1, mx, top + 1, z2 + 1, "minecraft:spruce_log[axis=z]");
        set(mx, g, z1 - 1, "minecraft:spruce_fence");
        fill(mx, g + 1, z1 - 1, mx, top, z1 - 1, "minecraft:spruce_fence");
        fill(mx, g, z2 + 1, mx, top, z2 + 1, "minecraft:spruce_fence");
        set(x1 - 1, g, z1 + 1, "minecraft:spruce_fence");
        set(x2 + 1, g, z1 + 1, "minecraft:spruce_fence");
        set(x1 - 1, g, z2 - 1, "minecraft:spruce_fence");
        set(x2 + 1, g, z2 - 1, "minecraft:spruce_fence");
        fill(x1 + 1, g - 1, z1, x2 - 1, g - 1, z2, "minecraft:spruce_planks");
        set(x1 + 1, g, z2 - 1, "minecraft:white_bed[facing=north,part=foot]");
        set(x2 - 1, g, z2, "minecraft:chest[facing=west]");
        if (top - 1 > g + 1) {
            set(mx, top - 1, (z1 + z2) / 2, "minecraft:lantern[hanging=true]");
        }
    }

    /** Campfire with logs to sit on. */
    void campfire(int x, int z) {
        set(x, g, z, "minecraft:campfire");
        set(x + 2, g, z, "minecraft:oak_log[axis=z]");
        set(x - 2, g, z, "minecraft:oak_log[axis=z]");
        if (!legacy) {
            // a ring of stones, a cooking spit and a log pile
            set(x - 1, g - 1, z, "minecraft:cobblestone");
            set(x + 1, g - 1, z, "minecraft:cobblestone");
            set(x, g - 1, z - 1, "minecraft:cobblestone");
            set(x, g - 1, z + 1, "minecraft:cobblestone");
            set(x - 1, g, z - 1, "minecraft:spruce_fence");
            set(x + 1, g, z - 1, "minecraft:spruce_fence");
            set(x - 1, g + 1, z - 1, "minecraft:spruce_fence");
            set(x + 1, g + 1, z - 1, "minecraft:spruce_fence");
            set(x, g + 1, z - 1, "minecraft:chain[axis=x]");
            set(x, g, z + 2, "minecraft:oak_log[axis=x]");
        }
    }

    /** A fence line along a tile line (both 2-block rows). */
    void fence(int tx1, int ty1, int tx2, int ty2, String fence) {
        b.tFill(tx1, ty1, tx2, ty2, 0, 0, fence);
    }
}
