package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The two undead dungeons of Ape Atoll (Monkey Madness I), carved tile by tile from the OSRS map ({@link ApeAtollMaps})
 * below Kruk's Dungeon's floors:
 *
 * <ul>
 *   <li>The Ape Atoll Dungeon (Zooknock's tunnels), entered by the bamboo ladder in the southern jungle (tile 2763, 2703;
 *   skretzo/shortest-path): the winding maze of monkey zombies, level 142 skeletons and spiders, its spike traps and
 *   the two stone traps under rock ledges (wiki map), falling rocks away from the ledges, and Zooknock's cave at its
 *   north-eastern end, where Zooknock, Bunkwicket and Waymottin stand on their cache tiles.</li>
 *   <li>The Temple of Marimbo Dungeon, below the temple's trapdoor: the first area with the two walls of flame, the
 *   crevice to the small cave of hot rocks (the long hot rock where stuffed snakes are cooked), the trapped tunnel to
 *   the locked wooden door, the great western cave with the monkey skull behind it (the hard Western Provinces Diary
 *   opens the door), and the separate southern cave of spiders.</li>
 * </ul>
 *
 * <p>Every monster stands on its cache tile (mejrs/data_osrs NPCList_OSRS.json; one camp per tile). Approximations: the
 * temple's trapdoor is a hut just south of the temple (the islands module builds the temple), with the dungeon shifted
 * 10 tiles south to lie under it; the southern cave's entrance is a hut 21 tiles east of the real crate in Marim; the
 * hot-rock cave (an instance in OSRS) is carved beside the southern cave; trap damage is checked once a second.
 */
final class ApeAtollDungeons {
    /** Shift from the Ape Atoll Dungeon's cache tiles to the surface (its ladder 2763, 9103 is below 2763, 2703). */
    static final int DX = 0;
    static final int DY = -6400;
    /** The temple dungeon's shift (its arrival tile 2790, 9191 lies under the hut at 2790, 2781, clear of the temple's grounds). */
    static final int TEMPLE_DY = -6410;
    /**
     * Floor heights: both dungeons lie below Kruk's Dungeon (22 and 36) and above its western hordes (-10); the temple
     * dungeon's southern cave ends one tile north of Zooknock's tunnels.
     */
    static final int FLOOR = 6;
    static final int TEMPLE_FLOOR = 6;
    private static final String SPIDER = "minecraft:cave_spider";

    /** The rock ledges of the Ape Atoll Dungeon (cache tiles): standing within a block or two of one shelters a player. */
    static final int[][] LEDGES = {{2770, 9100}, {2800, 9110}, {2740, 9110}, {2710, 9100}, {2705, 9130}, {2760, 9144}};
    /** Spike traps of Zooknock's tunnels (wiki map; each of the four multi-trap patches is one trap here, approx). */
    static final int[][] SPIKE_TRAPS = {{2809, 9111}, {2740, 9135}, {2715, 9119}, {2733, 9105}, {2725, 9097}, {2691, 9115},
            {2701, 9142}, {2744, 9102}, {2708, 9108}, {2740, 9122}, {2712, 9133}};
    /** The two stone traps ("a rock") under rock ledges: 20% of the player's hitpoints per tick while standing on them. */
    static final int[][] STONE_TRAPS = {{2693, 9109}, {2743, 9146}};
    /** Zooknock, Bunkwicket and Waymottin (cache tiles). */
    static final int[] ZOOKNOCK = {2804, 9145};
    /** The temple dungeon's trapped tunnel (one tile wide) and its locked wooden door just west of the trap. */
    static final int TRAP_X = 2778;
    static final int TRAP_Y = 9195;
    static final int DOOR_X = 2777;
    /** The crevice in the middle of the first area and the hot-rock cave it leads to (cache tiles, approx). */
    static final int[] CREVICE = {2796, 9200};
    static final int HOT_X1 = 2808;
    static final int HOT_X2 = 2814;
    static final int HOT_Y1 = 9166;
    static final int HOT_Y2 = 9172;
    /** The line of hot rocks across the cave (a tile column) and the long hot rock beyond it. */
    static final int HOT_LINE_X = 2810;

    /** What hurts a player standing at a block. */
    enum Hazard { NONE, TUNNEL_TRAP, SPIKE_TRAP, STONE_TRAP, HOT_ROCKS, FALLING_ROCKS }

    private ApeAtollDungeons() {
    }

    static List<Dungeons.Dungeon> all() {
        return List.of(apeAtollDungeon(), templeDungeon());
    }

    private static int bx(int t, int dx) {
        return StructureDef.bx(t + dx);
    }

    private static int bz(int t, int dy) {
        return StructureDef.bz(t + dy);
    }

    /** True if the feet block (x, z) is on the cache tile (tx, ty) under the given shift. */
    private static boolean onTile(int x, int z, int tx, int ty, int dx, int dy) {
        return x >= bx(tx, dx) && x <= bx(tx, dx) + 1 && z >= bz(ty, dy) && z <= bz(ty, dy) + 1;
    }

    /**
     * True if a player whose feet are at block (x, y, z) stands on the trapped tunnel's floor traps (the temple
     * dungeon's tunnel between its first area and the wooden door).
     */
    static boolean trapAt(int x, int y, int z) {
        return y == TEMPLE_FLOOR + 1 && onTile(x, z, TRAP_X, TRAP_Y, 0, TEMPLE_DY);
    }

    /** True if the player at (x, z) is on the western (locked) side of the temple dungeon's wooden door. */
    static boolean insideWoodenDoor(double x) {
        return x < bx(DOOR_X, 0);
    }

    /**
     * True if rocks can fall on a player whose feet are at block (x, y, z): inside the Ape Atoll Dungeon (Zooknock's
     * tunnels) and not sheltering by a rock ledge.
     */
    static boolean fallingRocks(int x, int y, int z) {
        if (y < FLOOR + 1 || y > FLOOR + 4 || x < bx(2690, DX) || x > bx(2812, DX) + 1 || z < bz(9152, DY) || z > bz(9088, DY) + 1) {
            return false;
        }
        for (int[] p : LEDGES) {
            if (Math.abs(x - bx(p[0], DX)) <= 2 && Math.abs(z - bz(p[1], DY)) <= 2) {
                return false;
            }
        }
        return true;
    }

    /** The hazard at a player's feet block, if any. */
    static Hazard hazardAt(int x, int y, int z) {
        if (trapAt(x, y, z)) {
            return Hazard.TUNNEL_TRAP;
        }
        if (y == TEMPLE_FLOOR + 1 && x >= bx(HOT_LINE_X, 0) && x <= bx(HOT_LINE_X, 0) + 1 && z >= bz(HOT_Y2, TEMPLE_DY)
                && z <= bz(HOT_Y1, TEMPLE_DY) + 1) {
            return Hazard.HOT_ROCKS;
        }
        if (y == FLOOR + 1) {
            for (int[] t : SPIKE_TRAPS) {
                if (x == bx(t[0], DX) && z == bz(t[1], DY)) {
                    return Hazard.SPIKE_TRAP;
                }
            }
            for (int[] t : STONE_TRAPS) {
                if (onTile(x, z, t[0], t[1], DX, DY)) {
                    return Hazard.STONE_TRAP;
                }
            }
        }
        return fallingRocks(x, y, z) ? Hazard.FALLING_ROCKS : Hazard.NONE;
    }

    /** Block {x, y, z} of the wooden door's lower northern block. */
    static int[] woodenDoor() {
        return new int[]{bx(DOOR_X, 0), TEMPLE_FLOOR + 1, bz(TRAP_Y, TEMPLE_DY)};
    }

    /** Feet block {x, y, z} of the temple dungeon's crevice (in the first area). */
    static int[] crevice() {
        return new int[]{bx(CREVICE[0], 0), TEMPLE_FLOOR + 1, bz(CREVICE[1], TEMPLE_DY)};
    }

    /** One camp per cache spawn tile ({x, y, x, y, ...}): the tile's 2x2 blocks, one creature each. */
    private static void tileCamps(Cave c, int dx, int dy, int f, int respawn, String creature, int... tiles) {
        for (int i = 0; i + 1 < tiles.length; i += 2) {
            int x = bx(tiles[i], dx);
            int z = bz(tiles[i + 1], dy);
            c.camp(x, z, x + 1, z + 1, f, 1, respawn, creature);
        }
    }

    /** Zooknock's tunnels (cache 2689-2814, 9089-9151). */
    static Dungeons.Dungeon apeAtollDungeon() {
        Cave c = new Cave("dng_ape_atoll_dungeon", "Ape Atoll Dungeon", mc("mossy_cobblestone"), mc("coarse_dirt"),
                mc("ochre_froglight"));
        int dx = DX;
        int dy = DY;
        c.traced(ApeAtollMaps.DUNGEON, ApeAtollMaps.DUNGEON_X0, ApeAtollMaps.DUNGEON_TOP, dx, dy, FLOOR, 5);
        c.carve();
        // Rock ledges (the falling-rock shelters), the traps, bones; Zooknock's brewing.
        for (int[] p : LEDGES) {
            c.set(bx(p[0], dx), FLOOR + 1, bz(p[1], dy), mc("mossy_cobblestone_wall"));
            c.set(bx(p[0], dx), FLOOR + 5, bz(p[1], dy), mc("mossy_stone_bricks"));
        }
        for (int[] t : SPIKE_TRAPS) {
            c.set(bx(t[0], dx), FLOOR + 1, bz(t[1], dy), mc("polished_blackstone_pressure_plate"));
        }
        for (int[] t : STONE_TRAPS) {
            c.fill(bx(t[0], dx), FLOOR + 1, bz(t[1], dy), bx(t[0], dx) + 1, FLOOR + 1, bz(t[1], dy) + 1, mc("stone_pressure_plate"));
        }
        c.set(bx(2738, dx), FLOOR + 1, bz(9120, dy), mc("bone_block[axis=x]"));
        c.set(bx(2712, dx), FLOOR + 1, bz(9104, dy), mc("skeleton_skull[rotation=2]"));
        c.set(bx(2807, dx), FLOOR + 1, bz(9146, dy), mc("cauldron"));
        c.set(bx(2807, dx), FLOOR + 1, bz(9148, dy), mc("brewing_stand"));
        c.entrance(bx(2763, dx), bz(9103, dy), FLOOR, mc("mossy_cobblestone"), mc("coarse_dirt"), 'n');
        // Monkey zombies respawn after 50 ticks, the skeletons after 100 (wiki); spiders approx.
        tileCamps(c, dx, dy, FLOOR, 50, "monkey_zombie", 2694, 9115, 2697, 9130, 2698, 9145, 2702, 9093, 2714, 9095, 2719, 9147,
                2726, 9107, 2728, 9097, 2740, 9138, 2742, 9146, 2752, 9144, 2763, 9118, 2764, 9133, 2765, 9113, 2769, 9094, 2772, 9110,
                2774, 9093, 2777, 9144, 2779, 9119, 2779, 9129, 2781, 9092, 2788, 9103, 2793, 9129, 2794, 9117, 2799, 9112, 2801, 9097,
                2807, 9131, 2809, 9104, 2810, 9111, 2810, 9123);
        tileCamps(c, dx, dy, FLOOR, 100, "skeleton_lvl142", 2704, 9109, 2707, 9112, 2708, 9103, 2710, 9135, 2711, 9109, 2711, 9130,
                2714, 9135, 2715, 9132, 2735, 9119, 2736, 9121, 2738, 9124, 2740, 9120, 2741, 9103, 2741, 9124, 2743, 9105, 2744, 9100,
                2745, 9102);
        tileCamps(c, dx, dy, FLOOR, 30, SPIDER, 2694, 9104, 2694, 9138, 2698, 9127, 2704, 9104, 2711, 9137, 2712, 9108, 2716, 9096,
                2716, 9146, 2729, 9118, 2732, 9130, 2741, 9122, 2743, 9107, 2748, 9100, 2748, 9143, 2761, 9094, 2761, 9120, 2761, 9144,
                2764, 9103, 2766, 9133, 2768, 9111, 2781, 9140, 2784, 9093, 2788, 9102, 2793, 9117, 2793, 9130, 2798, 9138, 2799, 9099,
                2810, 9104, 2810, 9124);
        return c.build();
    }

    /** The Temple of Marimbo Dungeon (cache 2755-2815, 9158-9213). */
    static Dungeons.Dungeon templeDungeon() {
        Cave c = new Cave("dng_temple_of_marimbo_dungeon", "Temple of Marimbo Dungeon", mc("mossy_stone_bricks"), mc("rooted_dirt"),
                mc("ochre_froglight"));
        int dx = 0;
        int dy = TEMPLE_DY;
        int f = TEMPLE_FLOOR;
        c.traced(ApeAtollMaps.TEMPLE, ApeAtollMaps.TEMPLE_X0, ApeAtollMaps.TEMPLE_TOP, dx, dy, f, 5);
        // The small cave of hot rocks beyond the crevice (an instance in OSRS: carved east of the southern cave).
        c.room(bx(HOT_X1, dx), bz(HOT_Y2, dy), bx(HOT_X2, dx) + 1, bz(HOT_Y1, dy) + 1, f, 4);
        c.carve();
        // The two walls of flame where m'speak amulets are made, beside the large monkey zombies at the east end.
        c.set(bx(2810, dx), f + 1, bz(9191, dy), mc("soul_campfire[lit=true]"));
        c.set(bx(2810, dx), f + 1, bz(9210, dy), mc("soul_campfire[lit=true]"));
        // The trapped tunnel's floor traps (its whole width) and the locked wooden door to the great western cave.
        c.fill(bx(TRAP_X, dx), f + 1, bz(TRAP_Y, dy), bx(TRAP_X, dx) + 1, f + 1, bz(TRAP_Y, dy) + 1,
                mc("polished_blackstone_pressure_plate"));
        c.fill(bx(DOOR_X, dx), f + 1, bz(TRAP_Y, dy), bx(DOOR_X, dx), f + 2, bz(TRAP_Y, dy) + 1, rc(DungeonBlocks.MARIMBO_DOOR));
        // The monkey skull's spot in the western cave, old bones.
        c.set(bx(2769, dx), f + 1, bz(9203, dy), mc("skeleton_skull[rotation=6]"));
        c.set(bx(2766, dx), f + 1, bz(9196, dy), mc("bone_block[axis=y]"));
        // The hot rocks across the small cave and the long hot rock beyond them.
        c.fill(bx(HOT_LINE_X, dx), f, bz(HOT_Y2, dy), bx(HOT_LINE_X, dx) + 1, f, bz(HOT_Y1, dy) + 1, mc("orange_terracotta"));
        c.fill(bx(HOT_X2, dx), f + 1, bz(9170, dy), bx(HOT_X2, dx) + 1, f + 1, bz(9168, dy) + 1, mc("magma_block"));
        int[] cr = crevice();
        c.passage2(DungeonBlocks.MARIMBO_CREVICE, cr[0], cr[2], f, "You squeeze through the crevice into a small, hot cave.",
                bx(HOT_X1, dx), bz(9169, dy), f, "You squeeze back through the crevice.");
        c.entrance(bx(2790, dx), bz(9191, dy), f, mc("mossy_stone_bricks"), mc("rooted_dirt"), 'n');
        // The southern cave (entered through the crate in Marim in OSRS).
        c.entrance(bx(2790, dx), bz(9165, dy), f, mc("bamboo_planks"), mc("coarse_dirt"), 's');
        // Monkey zombies respawn after 50 ticks (wiki); the large ones and the spiders approx.
        tileCamps(c, dx, dy, f, 50, "monkey_zombie_lvl82", 2761, 9193, 2761, 9204, 2763, 9205, 2764, 9193, 2764, 9197, 2765, 9188,
                2765, 9202, 2765, 9203, 2766, 9191, 2766, 9196, 2768, 9197, 2768, 9201, 2770, 9188, 2770, 9192, 2770, 9199, 2771, 9195,
                2782, 9193, 2785, 9200, 2786, 9204, 2790, 9194, 2798, 9196, 2798, 9199, 2798, 9204, 2800, 9196, 2800, 9198, 2800, 9203,
                2806, 9201, 2807, 9200, 2807, 9202);
        tileCamps(c, dx, dy, f, 50, "monkey_zombie", 2761, 9197, 2763, 9201, 2764, 9191, 2766, 9199, 2769, 9190);
        tileCamps(c, dx, dy, f, 12, "monkey_zombie_lvl129", 2768, 9203, 2769, 9202, 2770, 9202, 2808, 9191, 2808, 9210, 2809, 9193,
                2809, 9208);
        tileCamps(c, dx, dy, f, 30, SPIDER, 2761, 9189, 2764, 9195, 2767, 9163, 2768, 9187, 2769, 9193, 2770, 9201, 2773, 9194,
                2776, 9163, 2779, 9163, 2784, 9201, 2785, 9164, 2785, 9190, 2787, 9169, 2788, 9196, 2789, 9172, 2792, 9167, 2796, 9170,
                2796, 9175, 2797, 9198, 2799, 9205, 2800, 9162, 2801, 9198);
        return c.build();
    }
}
