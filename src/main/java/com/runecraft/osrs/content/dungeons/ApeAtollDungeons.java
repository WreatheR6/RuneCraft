package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * The two undead dungeons of Ape Atoll (Monkey Madness I), carved below Kruk's Dungeon's floors:
 *
 * <ul>
 *   <li>The Ape Atoll Dungeon (Zooknock's tunnels), entered in the southern jungle (tile 2763, 2703): the entrance
 *   cavern and the eastern caves of level 98 monkey zombies, the two halls of level 142 skeletons to the west, the
 *   northern tunnel back east, and Zooknock's cave at its end.</li>
 *   <li>The Temple of Marimbo Dungeon, below the temple's trapdoor: the first cave (small monkey zombies and spiders),
 *   the large monkey zombies by the monkey skull's room at its east end, and, through the trapped tunnel and the
 *   wooden door, the great western cave of monkey zombies.</li>
 * </ul>
 *
 * <p>Rooms cover the cache's monster clusters (mejrs/data_osrs NPCList_OSRS.json) and every camp keeps the cache's
 * number of each monster (the level 1 spiders are the core Spider). Approximations: the rooms are boxes around the
 * clusters rather than the real winding tunnels; the floor traps, falling rocks, the hot-rock cave and the locked doors
 * are left out; the temple's trapdoor is a hut just south of the temple (the islands module builds the temple), with
 * the dungeon shifted 3 tiles south to lie under it; Zooknock and his companions are not placed.
 */
final class ApeAtollDungeons {
    /** Surface entrance of the Ape Atoll Dungeon and the shift from its cache tiles. */
    static final int DX = 1;
    static final int DY = -6400;
    /** The temple dungeon's shift (its arrival tile 2790, 9186 lies under the hut at 2790, 2783). */
    static final int TEMPLE_DY = -6403;
    /** Floor height: below Kruk's Dungeon (22 and 36). */
    static final int FLOOR = 6;
    private static final String SPIDER = "minecraft:cave_spider";

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

    private static void room(Cave c, int dx, int dy, int tx1, int ty1, int tx2, int ty2, int h) {
        c.room(bx(tx1, dx), bz(ty2, dy), bx(tx2, dx) + 1, bz(ty1, dy) + 1, FLOOR, h);
    }

    private static void hall(Cave c, int dx, int dy, int ax, int ay, int bxT, int byT) {
        c.hall(bx(ax, dx), bz(ay, dy), bx(bxT, dx), bz(byT, dy), FLOOR, 3, 5);
    }

    private static void camp(Cave c, int dx, int dy, int tx1, int ty1, int tx2, int ty2, int cap, int respawn, String creature) {
        c.camp(bx(tx1, dx) + 1, bz(ty2, dy) + 1, bx(tx2, dx), bz(ty1, dy), FLOOR, cap, respawn, creature);
    }

    /** Zooknock's tunnels (cache 2690-2812, 9088-9152). */
    static Dungeons.Dungeon apeAtollDungeon() {
        Cave c = new Cave("dng_ape_atoll_dungeon", "Ape Atoll Dungeon", mc("mossy_cobblestone"), mc("coarse_dirt"),
                mc("ochre_froglight"));
        int dx = DX;
        int dy = DY;
        room(c, dx, dy, 2756, 9088, 2790, 9122, 6);           // the entrance cavern
        room(c, dx, dy, 2792, 9088, 2812, 9134, 6);           // the eastern caves
        room(c, dx, dy, 2760, 9125, 2785, 9134, 5);
        hall(c, dx, dy, 2790, 9110, 2792, 9110);
        hall(c, dx, dy, 2770, 9122, 2770, 9125);
        hall(c, dx, dy, 2785, 9130, 2792, 9130);
        hall(c, dx, dy, 2750, 9105, 2756, 9105);
        room(c, dx, dy, 2732, 9096, 2750, 9127, 6);           // skeleton hall (east)
        hall(c, dx, dy, 2730, 9100, 2732, 9100);
        room(c, dx, dy, 2698, 9088, 2730, 9113, 6);           // skeleton hall (west)
        room(c, dx, dy, 2690, 9114, 2720, 9150, 6);           // the north-western caves
        room(c, dx, dy, 2721, 9137, 2795, 9150, 5);           // the northern tunnel
        hall(c, dx, dy, 2795, 9145, 2797, 9145);
        room(c, dx, dy, 2797, 9138, 2810, 9152, 6);           // Zooknock's cave
        c.carve();
        // Rock ledges (the falling-rock shelters) and bones; Zooknock's brewing.
        for (int[] p : new int[][]{{2770, 9100}, {2800, 9110}, {2740, 9110}, {2710, 9100}, {2705, 9130}, {2760, 9144}}) {
            c.pillar(bx(p[0], dx), bz(p[1], dy), FLOOR, 6, mc("mossy_stone_bricks"));
        }
        c.set(bx(2738, dx), FLOOR + 1, bz(9120, dy), mc("bone_block[axis=x]"));
        c.set(bx(2712, dx), FLOOR + 1, bz(9104, dy), mc("skeleton_skull[rotation=2]"));
        c.set(bx(2805, dx), FLOOR + 1, bz(9147, dy), mc("cauldron"));
        c.set(bx(2803, dx), FLOOR + 1, bz(9149, dy), mc("brewing_stand"));
        c.entrance(bx(2762, dx), bz(9103, dy), FLOOR, mc("mossy_cobblestone"), mc("coarse_dirt"), 'n');
        // Monkey zombies respawn after 50 ticks, the skeletons after 100 (wiki); spiders approx.
        // (Camps of neighbouring rooms are merged: a camp counts its creatures a few blocks around its box.)
        camp(c, dx, dy, 2756, 9088, 2812, 9122, 18, 50, "monkey_zombie");
        camp(c, dx, dy, 2756, 9088, 2812, 9122, 12, 30, SPIDER);
        camp(c, dx, dy, 2732, 9096, 2750, 9127, 9, 100, "skeleton_lvl142");
        camp(c, dx, dy, 2732, 9096, 2750, 9127, 3, 30, SPIDER);
        camp(c, dx, dy, 2698, 9088, 2720, 9150, 8, 100, "skeleton_lvl142");
        camp(c, dx, dy, 2698, 9088, 2720, 9150, 8, 50, "monkey_zombie");
        camp(c, dx, dy, 2698, 9088, 2720, 9150, 7, 30, SPIDER);
        camp(c, dx, dy, 2730, 9137, 2795, 9150, 4, 50, "monkey_zombie");
        camp(c, dx, dy, 2730, 9137, 2795, 9150, 3, 30, SPIDER);
        return c.build();
    }

    /** The Temple of Marimbo Dungeon (cache 2758-2812, 9183-9211). */
    static Dungeons.Dungeon templeDungeon() {
        Cave c = new Cave("dng_temple_of_marimbo_dungeon", "Temple of Marimbo Dungeon", mc("mossy_stone_bricks"), mc("rooted_dirt"),
                mc("ochre_froglight"));
        int dx = 0;
        int dy = TEMPLE_DY;
        room(c, dx, dy, 2787, 9183, 2793, 9186, 5);           // below the trapdoor
        room(c, dx, dy, 2778, 9187, 2806, 9206, 6);           // the first cave
        room(c, dx, dy, 2807, 9188, 2812, 9211, 5);           // its eastern end, by the monkey skull's room
        hall(c, dx, dy, 2775, 9196, 2778, 9196);              // the trapped tunnel and the wooden door
        room(c, dx, dy, 2758, 9186, 2775, 9206, 6);           // the great western cave
        c.carve();
        // The two walls of flame where m'speak amulets are made, and old bones.
        c.set(bx(2796, dx), FLOOR + 1, bz(9205, dy), mc("soul_campfire"));
        c.set(bx(2800, dx), FLOOR + 1, bz(9205, dy), mc("soul_campfire"));
        c.set(bx(2766, dx), FLOOR + 1, bz(9196, dy), mc("bone_block[axis=y]"));
        c.set(bx(2770, dx), FLOOR + 1, bz(9188, dy), mc("skeleton_skull[rotation=6]"));
        c.entrance(bx(2790, dx), bz(9184, dy), FLOOR, mc("mossy_stone_bricks"), mc("rooted_dirt"), 'n');
        camp(c, dx, dy, 2758, 9188, 2812, 9206, 29, 50, "monkey_zombie_lvl82");
        camp(c, dx, dy, 2758, 9188, 2812, 9206, 12, 30, SPIDER);
        camp(c, dx, dy, 2758, 9186, 2775, 9206, 5, 50, "monkey_zombie");
        camp(c, dx, dy, 2758, 9186, 2775, 9206, 3, 12, "monkey_zombie_lvl129");
        camp(c, dx, dy, 2807, 9188, 2812, 9211, 4, 12, "monkey_zombie_lvl129");
        return c.build();
    }
}
