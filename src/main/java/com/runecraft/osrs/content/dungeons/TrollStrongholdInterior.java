package com.runecraft.osrs.content.dungeons;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * The inside of the Troll Stronghold (Troll Stronghold quest), carved from the cache like {@link CacheDungeons}. In OSRS
 * the stronghold is an instance-free dungeon area at 2820-2872, 10049-10111 on three planes: the door at the surface tile
 * (2840, 3690) leads onto the top floor (plane 2, 2837, 10090) with its troll guards and the Troll Generals' hall west of
 * the entrance corridor; stone staircases at both ends (2845, 10051 and 2845, 10108) lead down to plane 1 with
 * Burntmeat's kitchen, the goat pen, the trolls' hall (Ug, Aga, Arrg) and the locked prison door (2847, 10107); behind it
 * and from the goat pen stairs lead down to the plane-0 prison where Godric, Mad Eadgar and the guards Twig and Berry
 * are (2827-2832, 10077-10083), with the storeroom (2869, 10084) east of it.
 *
 * <p>Every floor keeps the cache's tile layout under one shift ({@link #DX}, {@link #DY}) that puts the entrance
 * corridor under the real door, deep below the fortress the world module built on the mountain at y 108. The surface
 * door is a ladder from the courtyard just south of the keep (tile {@link #LADDER_X}, {@link #LADDER_Y}, approx: OSRS
 * walks straight in). Rooms are boxes around the cache spawns and transport tiles (approx shapes); the staircases are
 * ladders at the stair tiles (the two ends of each OSRS staircase are a few tiles apart, the ladder stands between
 * them). The mountain trolls are the cache's 8 on plane 2 and 8 on plane 1, the three Troll generals stand in their
 * hall on the cache tiles (the quests module's "troll_general" stays the quest's boss, fought at its arena in the
 * courtyard), and the guards Twig and Berry stand by the cells, where the dungeons module's hooks move Godric and put a
 * copy of Mad Eadgar (the quests module's NPCs; Burntmeat and the other named trolls stay at the surface place, approx).
 * A tunnel south from the prison leads to its secret exit (2823, 10050), which comes out on the mountain's south slope
 * (2827, 3646).
 */
final class TrollStrongholdInterior {
    /** Cache entrance tile (2837, 10090) under the real door (2840, 3690). */
    static final int DX = 3;
    static final int DY = -6400;
    /** Floors: plane 2 (entrance), plane 1 (kitchen) and plane 0 (prison). */
    static final int F2 = 40;
    static final int F1 = 30;
    static final int F0 = 20;
    /** Bottom of the ladder shaft the world module's fortress (ground 108) sinks from its courtyard. */
    static final int SHAFT_BOTTOM = 55;
    /** Surface tile of the ladder down (just south-west of the real door at 2840, 3690, outside the keep's wall). */
    static final int LADDER_X = 2838;
    static final int LADDER_Y = 3688;
    static final int RESPAWN = 35; // approx (OSRS mid-level respawn)

    /** Cache spawns of the three Troll generals (plane 2) and of Twig and Berry (plane 0). */
    static final int[] GENERALS = {2831, 10086, 2822, 10073, 2829, 10100};
    static final int[] TWIG = {2832, 10079};
    static final int[] BERRY = {2832, 10083};
    /** Cache tiles of Godric and Eadgar in their cells. */
    static final int[][] PRISONERS = {{2827, 10077}, {2829, 10083}};
    /** The secret exit (cache tile) and where it comes out on the surface. */
    static final int[] SECRET_EXIT = {2823, 10050};
    static final int[] SECRET_EXIT_SURFACE = {2827, 3646};

    /** Cache spawns (mejrs/data_osrs NPCList_OSRS.json): mountain trolls on planes 2 and 1. */
    static final int[] TROLLS_P2 = {2836, 10074, 2836, 10082, 2836, 10087, 2836, 10095, 2838, 10098, 2851, 10052, 2852, 10107,
            2855, 10054};
    static final int[] TROLLS_P1 = {2825, 10086, 2827, 10081, 2829, 10079, 2829, 10083, 2830, 10086, 2836, 10077, 2836, 10089,
            2839, 10101};

    /** Plane-2 rooms: the entrance corridor, the generals' hall, the south and north stair landings. */
    static final int[] P2_CORRIDOR = {2834, 10058, 2840, 10103};
    static final int[] P2_GENERALS = {2820, 10058, 2833, 10104};
    static final int[] P2_SOUTH = {2840, 10049, 2858, 10057};
    static final int[] P2_NORTH = {2840, 10104, 2856, 10111};
    /** Plane-1 rooms: the kitchen, the goat pen, the trolls' hall, the prison passage. */
    static final int[] P1_KITCHEN = {2838, 10049, 2849, 10062};
    static final int[] P1_GOATS = {2850, 10052, 2861, 10063};
    static final int[] P1_HALL = {2822, 10063, 2840, 10106};
    static final int[] P1_NORTH = {2838, 10104, 2856, 10111};
    /** Plane-0 rooms: the two stair landings, the prison, the storeroom. */
    static final int[] P0_NORTH = {2846, 10098, 2856, 10107};
    static final int[] P0_SOUTH = {2846, 10060, 2856, 10068};
    static final int[] P0_PRISON = {2822, 10072, 2838, 10090};
    static final int[] P0_STORE = {2865, 10080, 2872, 10090};
    /** The end of the tunnel to the secret exit. */
    static final int[] P0_EXIT = {2822, 10048, 2826, 10053};

    private TrollStrongholdInterior() {
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_troll_stronghold", "Troll Stronghold", mc("cobbled_deepslate"), mc("deepslate_tiles"), mc("lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, DX, DY);
        // ---- plane 2
        int[][] p2 = {P2_CORRIDOR, P2_GENERALS, P2_SOUTH, P2_NORTH};
        for (int[] r : p2) {
            s.room(r, F2, 6);
        }
        // ---- plane 1
        int[][] p1 = {P1_KITCHEN, P1_GOATS, P1_HALL, P1_NORTH};
        for (int[] r : p1) {
            s.room(r, F1, 6);
        }
        s.hall(2840, 10060, 2840, 10064, F1);
        // ---- plane 0
        int[][] p0 = {P0_NORTH, P0_SOUTH, P0_PRISON, P0_STORE, P0_EXIT};
        for (int[] r : p0) {
            s.room(r, F0, 5);
        }
        s.hallZ(2851, 10068, 2851, 10098, F0);
        s.hall(2851, 10084, 2838, 10084, F0);
        s.hall(2851, 10084, 2865, 10084, F0);
        s.hallZ(2824, 10072, 2824, 10053, F0); // the tunnel to the secret exit
        c.carve();

        // Staircases (ladders between the two ends of each OSRS staircase).
        c.ladderUp(s.x(2843), s.z(10051), F1, F2);
        c.ladderUp(s.x(2843), s.z(10108), F1, F2);
        c.ladderUp(s.x(2852), s.z(10062), F0, F1);
        c.ladderUp(s.x(2852), s.z(10106), F0, F1);
        // The prison door on plane 1 (2847, 10107) in a wall across the passage (approx: the prison key is not checked).
        c.barrier(s.x(2847), s.z(10111), s.x(2847), s.z(10104) + 1, F1, 3, s.x(2847), s.z(10107), mc("spruce_door[facing=east,half=lower]"));
        // Prison cells: iron bars between Godric's, Eadgar's and the guards' corner (approx cell walls).
        c.fill(s.x(2825), F0 + 1, s.z(10080), s.x(2834), F0 + 3, s.z(10080), mc("iron_bars"));
        c.fill(s.x(2825), F0 + 1, s.z(10086), s.x(2834), F0 + 3, s.z(10086), mc("iron_bars"));
        c.fill(s.x(2830), F0 + 1, s.z(10088), s.x(2830), F0 + 3, s.z(10074), mc("iron_bars"));
        c.set(s.x(2827), F0 + 1, s.z(10077), mc("red_bed[facing=north,part=foot]"));
        c.set(s.x(2833), F0 + 1, s.z(10082), mc("barrel[facing=up]"));
        // Burntmeat's kitchen and the goat pen.
        c.set(s.x(2844), F1 + 1, s.z(10058), mc("campfire[lit=true]"));
        c.set(s.x(2846), F1 + 1, s.z(10055), mc("cauldron"));
        c.fill(s.x(2850), F1 + 1, s.z(10063), s.x(2850), F1 + 1, s.z(10058), mc("spruce_fence"));
        c.fill(s.x(2856), F1 + 1, s.z(10057), s.x(2859), F1 + 1, s.z(10057), mc("hay_block"));
        // The generals' hall: bones and boulders; the storeroom's crates.
        c.set(s.x(2824), F2 + 1, s.z(10073), mc("bone_block[axis=y]"));
        c.set(s.x(2828), F2 + 1, s.z(10096), mc("skeleton_skull[rotation=4]"));
        c.set(s.x(2830), F2 + 1, s.z(10061), mc("cobblestone"));
        c.fill(s.x(2867), F0 + 1, s.z(10088), s.x(2871), F0 + 1, s.z(10088), mc("barrel[facing=up]"));
        for (int[] p : new int[][]{{2826, 10066}, {2826, 10100}, {2836, 10060}, {2836, 10102}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), F2, 6, mc("polished_deepslate"));
        }

        // The way in: a ladder from the fortress courtyard down to the entrance corridor (cache 2835, 10088).
        int lx = s.x(LADDER_X - DX);
        int lz = s.z(LADDER_Y - DY);
        // The world module's fortress builds the shaft from the courtyard down to y SHAFT_BOTTOM; it carries on here.
        c.link(lx, lz, SHAFT_BOTTOM, F2, Cave.autoFacing(lx, lz));

        s.camps(p2, F2, RESPAWN, "mountain_troll", TROLLS_P2);
        s.camps(p1, F1, RESPAWN, "mountain_troll", TROLLS_P1);
        s.camps(p2, F2, RESPAWN, "stronghold_troll_general", GENERALS);
        // The two guards each keep to their spot by the cells.
        c.camp(s.x(TWIG[0]), s.z(TWIG[1]), s.x(TWIG[0]) + 1, s.z(TWIG[1]) + 1, F0, 1, RESPAWN, "twig_troll");
        c.camp(s.x(BERRY[0]), s.z(BERRY[1]), s.x(BERRY[0]) + 1, s.z(BERRY[1]) + 1, F0, 1, RESPAWN, "berry_troll");
        // The secret exit at the end of the tunnel.
        c.passage(DungeonBlocks.SECRET_EXIT, s.x(SECRET_EXIT[0]), s.z(SECRET_EXIT[1]), F0,
                com.runecraft.osrs.world.StructureDef.bx(SECRET_EXIT_SURFACE[0]), Dungeons.SURFACE,
                com.runecraft.osrs.world.StructureDef.bz(SECRET_EXIT_SURFACE[1]),
                "You push the secret door open and squeeze out onto the mountainside.");
        return c.build();
    }

    /** Feet block {x, y, z} of a prisoner's cell spot (0 = Godric, 1 = Eadgar). */
    static int[] prisoner(int which) {
        int[] t = PRISONERS[which];
        return new int[]{com.runecraft.osrs.world.StructureDef.bx(t[0] + DX), F0 + 1, com.runecraft.osrs.world.StructureDef.bz(t[1] + DY)};
    }
}
