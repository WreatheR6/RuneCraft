package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Dungeons carved from the cache's spawn tiles (mejrs/data_osrs NPCList_OSRS.json) with one constant shift each, like
 * {@link KruksDungeon}: the Shadow Dungeon (Desert Treasure I), the Sophanem Dungeon (Contact!), the Lighthouse
 * basement and its dagannoth cave (Horror from the Deep), the Kalphite Cave, the Wilderness Slayer Cave, the Forthos
 * Dungeon, the Giants' Den, the Isle of Souls Dungeon, the Myths' Guild dungeon, the Deep Wilderness Dungeon, the Karamja
 * Volcano dungeon with the Crandor tunnels, the Ice Troll Caves and the Ourania Cave. Every camp holds exactly the cache's
 * number of each monster inside its room (rooms are boxes around the
 * cache clusters; the passages between them are approximate, from the wiki maps). The ladders and stairs stand on the
 * real surface tiles (from skretzo/shortest-path's transports) unless noted.
 */
final class CacheDungeons {
    /** Respawn of most dungeon monsters (25 ticks, approx) and of the dragons (50 ticks, approx). */
    static final int RESPAWN = 25;
    static final int DRAGON_RESPAWN = 50;

    private CacheDungeons() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(shadowDungeon());
        out.add(sophanemDungeon());
        out.add(lighthouseDungeon());
        out.add(kalphiteCave());
        out.add(wildernessSlayerCave());
        out.add(forthosDungeon());
        out.add(giantsDen());
        out.add(isleOfSoulsDungeon());
        out.add(mythsGuildDungeon());
        out.add(deepWildernessDungeon());
        out.add(karamjaVolcanoDungeon());
        out.add(iceTrollCaves());
        out.add(ouraniaCave());
        return out;
    }

    /** A dungeon whose cache tiles are shifted by (dx, dy) to the surface tiles above it. */
    static final class Shifted {
        final Cave c;
        final int dx;
        final int dy;

        Shifted(Cave c, int dx, int dy) {
            this.c = c;
            this.dx = dx;
            this.dy = dy;
        }

        int x(int t) {
            return StructureDef.bx(t + dx);
        }

        int z(int t) {
            return StructureDef.bz(t + dy);
        }

        /** A room over the cache tile box {x1, y1, x2, y2} (inclusive) on floor f. */
        void room(int[] r, int f, int h) {
            c.room(x(r[0]), z(r[3]), x(r[2]) + 1, z(r[1]) + 1, f, h);
        }

        /** A 3-wide passage between two cache tiles (along x first, then along y). */
        void hall(int ax, int ay, int bx, int by, int f) {
            c.hall(x(ax), z(ay), x(bx), z(by), f, 3, 5);
        }

        /** A 3-wide passage along y first, then along x. */
        void hallZ(int ax, int ay, int bx, int by, int f) {
            c.hallZ(x(ax), z(ay), x(bx), z(by), f, 3, 5);
        }

        /**
         * Camps one creature's cache spawns ({x, y, x, y, ...}): each tile counts in the first room containing it, and
         * each room gets a camp of exactly its count. A tile outside every room is an error.
         */
        void camps(int[][] rooms, int f, int respawn, String creature, int[] tiles) {
            int[] n = new int[rooms.length];
            for (int i = 0; i + 1 < tiles.length; i += 2) {
                int hit = -1;
                for (int r = 0; r < rooms.length && hit < 0; r++) {
                    int[] b = rooms[r];
                    if (tiles[i] >= b[0] && tiles[i] <= b[2] && tiles[i + 1] >= b[1] && tiles[i + 1] <= b[3]) {
                        hit = r;
                    }
                }
                if (hit < 0) {
                    throw new IllegalStateException(c.id + ": " + creature + " tile " + tiles[i] + "," + tiles[i + 1] + " is in no room");
                }
                n[hit]++;
            }
            for (int r = 0; r < rooms.length; r++) {
                if (n[r] > 0) {
                    int[] b = rooms[r];
                    c.camp(x(b[0]), z(b[3]), x(b[2]) + 1, z(b[1]) + 1, f, n[r], respawn, creature);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Shadow Dungeon

    /** Cache tile of the ladder's landing and the shift that puts it under the real ladder (2547, 3421). */
    static final int SHADOW_DX = 2547 - 2630;
    static final int SHADOW_DY = 3421 - 5071;
    static final int[] GIANT_SKELETONS = {2626, 5065, 2627, 5093, 2629, 5061, 2637, 5099, 2638, 5058, 2638, 5090, 2644, 5090,
            2651, 5105, 2654, 5079, 2658, 5082, 2663, 5116, 2666, 5097, 2680, 5074, 2683, 5059, 2685, 5111, 2693, 5065, 2693, 5075,
            2693, 5089, 2695, 5089, 2697, 5096, 2703, 5064, 2704, 5091, 2710, 5095, 2710, 5105, 2712, 5076, 2714, 5108, 2716, 5092,
            2719, 5071, 2719, 5078, 2719, 5112, 2720, 5096, 2722, 5061, 2726, 5086, 2729, 5096, 2732, 5087, 2735, 5061, 2739, 5078,
            2740, 5069, 2740, 5085, 2740, 5104, 2746, 5092, 2746, 5114, 2747, 5083};
    static final int[] SHADOW_HOUNDS = {2694, 5067, 2717, 5081, 2718, 5108, 2721, 5104, 2726, 5091, 2731, 5060, 2732, 5073,
            2740, 5083, 2742, 5103, 2747, 5094};
    static final int[] SHADOW_GIANT_BAT = {2709, 5086};
    static final int[] SHADOW_GIANT_RATS = {2721, 5102, 2737, 5062};
    static final int[] SHADOW_RATS = {2693, 5111, 2699, 5069, 2700, 5061, 2706, 5114, 2707, 5079, 2708, 5070, 2712, 5110, 2713, 5061,
            2721, 5076, 2724, 5113, 2739, 5112, 2741, 5077};
    static final int[] SHADOW_BATS = {2693, 5102, 2699, 5083, 2707, 5106, 2721, 5062, 2735, 5073, 2735, 5105, 2742, 5097, 2744, 5065};

    /**
     * The Shadow Dungeon east of Baxtorian Falls: the ladder's chamber, the western caverns and the great eastern hall
     * where Damis waits (the quests module fights him at its own arena). Its 43 giant skeletons, 10 shadow hounds, giant
     * bat, 2 giant rats, 12 rats and 8 bats stand on their cache tiles (a multicombat area, {@code MultiCombat.UNDERGROUND}). The ladder is only visible
     * with a ring of visibility in OSRS; here the systems module's gate asks for Desert Treasure I to be started. The
     * hut stands 16 tiles south of the real ladder, which is the quests module's Shadow Dungeon place (approx).
     */
    static Dungeons.Dungeon shadowDungeon() {
        Cave c = new Cave("dng_shadow_dungeon", "Shadow Dungeon", mc("blackstone"), mc("polished_blackstone"), mc("soul_lantern"));
        Shifted s = new Shifted(c, SHADOW_DX, SHADOW_DY);
        int f = 16;
        int[] ladderRoom = {2622, 5052, 2646, 5076};
        int[] west = {2623, 5084, 2670, 5120};
        int[] middle = {2648, 5074, 2664, 5086};
        int[] east = {2676, 5054, 2750, 5120};
        s.room(ladderRoom, f, 6);
        s.room(west, f, 6);
        s.room(middle, f, 5);
        s.room(east, f, 8);
        s.hallZ(2630, 5076, 2630, 5084, f);
        s.hall(2646, 5068, 2652, 5074, f);
        s.hall(2664, 5080, 2676, 5080, f);
        c.carve();
        for (int[] p : new int[][]{{2690, 5070}, {2705, 5100}, {2725, 5070}, {2735, 5095}, {2700, 5082}, {2745, 5074}, {2640, 5105}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 8, mc("polished_blackstone_bricks"));
        }
        c.set(s.x(2748), f + 1, s.z(5118), mc("skeleton_skull[rotation=6]")); // the far north-eastern corner (master clue)
        c.entrance(s.x(2630), s.z(5055), f, mc("blackstone"), mc("polished_blackstone"), 's');
        int[][] rooms = {ladderRoom, west, middle, east};
        s.camps(rooms, f, RESPAWN, "giant_skeleton", GIANT_SKELETONS);
        s.camps(rooms, f, RESPAWN, "shadow_hound", SHADOW_HOUNDS);
        s.camps(rooms, f, RESPAWN, "giant_bat", SHADOW_GIANT_BAT);
        s.camps(rooms, f, RESPAWN, "giant_rat", SHADOW_GIANT_RATS);
        s.camps(rooms, f, RESPAWN, "rat", SHADOW_RATS);
        s.camps(rooms, f, RESPAWN, "bat", SHADOW_BATS);
        return c.build();
    }

    // ------------------------------------------------------------------ Sophanem Dungeon

    /** Shift of the maze and cavern levels (cache 3266-3326, 9217-9279), which puts them under Sophanem. */
    static final int SOPH_DY = -6455;
    /** Cavern level (cache plane 0): 16 locust rangers, 11 lancers and 18 scarab mages. */
    static final int[] SOPH_RANGERS = {3273, 9251, 3274, 9223, 3274, 9262, 3280, 9240, 3280, 9251, 3284, 9247, 3290, 9232,
            3291, 9262, 3295, 9251, 3296, 9229, 3299, 9244, 3302, 9232, 3304, 9252, 3306, 9223, 3308, 9262, 3310, 9237};
    static final int[] SOPH_LANCERS = {3270, 9264, 3272, 9246, 3276, 9235, 3283, 9243, 3284, 9252, 3287, 9268, 3294, 9233,
            3296, 9261, 3299, 9254, 3303, 9266, 3305, 9234};
    static final int[] SOPH_MAGES = {3273, 9263, 3275, 9226, 3276, 9248, 3279, 9222, 3282, 9244, 3282, 9273, 3286, 9227,
            3288, 9265, 3296, 9248, 3297, 9232, 3298, 9225, 3300, 9241, 3301, 9252, 3303, 9235, 3309, 9224, 3309, 9265, 3316, 9255,
            3317, 9240};
    /** Maze level (cache plane 2): 10 scarab mages and 2 locust rangers. */
    static final int[] MAZE_MAGES = {3276, 9268, 3277, 9252, 3277, 9275, 3279, 9234, 3289, 9236, 3296, 9256, 3301, 9275,
            3305, 9256, 3308, 9221, 3311, 9226};
    static final int[] MAZE_RANGERS = {3292, 9256, 3299, 9220};

    /**
     * The Sophanem Dungeon below the town: the bank level (the quests module keeps the bank in the temple), the maze level
     * with its scarab mages and the dark cavern level of the locust riders, joined by the cache's ladders. In OSRS the way
     * down is the ladder in the church's basement (3315, 2796); that tile is inside a house of the world module, so the
     * hut stands just outside the east wall (approx). The maze's traps and trapdoors are not modelled (approx: a few
     * wall segments stand in for the labyrinth).
     */
    static Dungeons.Dungeon sophanemDungeon() {
        Cave c = new Cave("dng_sophanem_dungeon", "Sophanem Dungeon", mc("sandstone"), mc("smooth_sandstone"), mc("ochre_froglight"));
        Shifted s = new Shifted(c, 0, SOPH_DY);
        int bank = 40;
        int maze = 26;
        int cavern = 12;
        int[] level = {3266, 9217, 3326, 9279};
        // The bank level, under the hut (its tiles are the surface tiles: shift 0, 6455 back).
        int[] bankRoom = {3316, 2790 - SOPH_DY, 3340, 2802 - SOPH_DY};
        s.room(bankRoom, bank, 5);
        s.room(level, maze, 4);
        s.room(level, cavern, 7);
        int[] mummyRoom = {3256, 9276, 3268, 9290}; // the cavern's north-western corner, where its mummy lies
        s.room(mummyRoom, cavern, 5);
        c.carve();
        // The labyrinth: wall segments across the maze level.
        for (int tx = 3272; tx <= 3320; tx += 8) {
            int y0 = (tx / 8) % 2 == 0 ? 9220 : 9240;
            c.fill(s.x(tx), maze + 1, s.z(y0 + 30), s.x(tx), maze + 4, s.z(y0), c.wall);
        }
        for (int ty = 9230; ty <= 9270; ty += 10) {
            c.fill(s.x(3280), maze + 1, s.z(ty), s.x(3300), maze + 4, s.z(ty), c.wall);
        }
        // Sandstone columns in the cavern.
        for (int[] p : new int[][]{{3278, 9230}, {3292, 9240}, {3306, 9258}, {3285, 9270}, {3312, 9228}, {3270, 9255}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), cavern, 7, mc("chiseled_sandstone"));
        }
        // Bank level -> maze (approx), maze <-> cavern on the cache's ladder tiles.
        c.ladderUp(s.x(3318), s.z(2792 - SOPH_DY), maze, bank);
        c.ladderUp(s.x(3286), s.z(9274), cavern, maze);
        c.ladderUp(s.x(3317), s.z(9250), cavern, maze);
        c.ladderUp(s.x(3280), s.z(9255), cavern, maze);
        c.ladderUp(s.x(3271), s.z(9235), cavern, maze);
        c.entrance(s.x(3336), s.z(2796 - SOPH_DY), bank, mc("cut_sandstone"), mc("smooth_sandstone"), 'e');
        int[][] rooms = {level};
        s.camps(rooms, cavern, RESPAWN, "locust_rider_ranger", SOPH_RANGERS);
        s.camps(rooms, cavern, RESPAWN, "locust_rider", SOPH_LANCERS);
        s.camps(rooms, cavern, RESPAWN, "scarab_mage", SOPH_MAGES);
        s.camps(rooms, maze, RESPAWN, "scarab_mage", MAZE_MAGES);
        s.camps(rooms, maze, RESPAWN, "locust_rider_ranger", MAZE_RANGERS);
        s.camps(new int[][]{mummyRoom}, cavern, RESPAWN, "mummy", new int[]{3260, 9285});
        return c.build();
    }

    // ------------------------------------------------------------------ Lighthouse

    /** The Lighthouse's iron ladder (2510, 3644) lands on cache tile (2518, 9994). */
    static final int LH_DX = 2510 - 2518;
    static final int LH_DY = 3644 - 9994;
    static final int[] LH_DAGANNOTHS = {2513, 10018, 2514, 10012, 2515, 10030, 2518, 10013, 2518, 10027, 2520, 10018, 2521, 10015,
            2522, 10023, 2522, 10028, 2524, 10014, 2524, 10033, 2526, 10035, 2527, 10019, 2532, 10023, 2535, 10030};
    static final int[] LH_DAGANNOTHS_92 = {2511, 10020, 2514, 10027, 2515, 10016, 2519, 10031, 2523, 10019, 2527, 10024, 2534, 10025};

    /**
     * The Lighthouse basement and the dagannoth cave beyond its strange wall (Horror from the Deep): 15 level 74 and 7
     * level 92 dagannoths on their cache tiles. Carved below {@code TerrainAnchors.DEEP_UNDERGROUND_BELOW}, so the sea
     * bed above keeps its depth. The Dagannoth Mother herself is the quests module's boss at the Lighthouse. The real way
     * down is the iron ladder at (2510, 3644) inside the world module's Lighthouse grounds, so the hut stands on the rocky
     * shore at the south end of the basalt rocks (2516, 3614) with a passage north to the basement (approx).
     */
    static Dungeons.Dungeon lighthouseDungeon() {
        Cave c = new Cave("dng_lighthouse_dungeon", "Lighthouse dungeon", mc("prismarine_bricks"), mc("dark_prismarine"), mc("sea_lantern"));
        Shifted s = new Shifted(c, LH_DX, LH_DY);
        int f = 20;
        int[] basement = {2512, 9989, 2524, 10000};
        int[] cave = {2508, 10008, 2538, 10038};
        // The hut on the rocky shore south of the Lighthouse grounds (the world module's), cache tile (2524, 9964).
        int[] landing = {2521, 9963, 2527, 9969};
        s.room(landing, f, 5);
        s.hallZ(2524, 9969, 2520, 9989, f);
        s.room(basement, f, 5);
        s.hallZ(2515, 10000, 2515, 10008, f);
        s.room(cave, f, 6);
        c.carve();
        c.fill(s.x(2514), f + 1, s.z(10003), s.x(2516), f + 1, s.z(10003), mc("bookshelf")); // the strange wall (open)
        c.pool(s.x(2528), s.z(10012), s.x(2534), s.z(10009), f, mc("water"));
        for (int[] p : new int[][]{{2512, 10036}, {2536, 10012}, {2530, 10032}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("prismarine"));
        }
        c.set(s.x(2518), f + 1, s.z(9994), mc("iron_bars")); // where the Lighthouse's iron ladder comes down
        c.entrance(s.x(2524), s.z(9964), f, mc("stone_bricks"), mc("polished_andesite"), 'n');
        int[][] rooms = {cave};
        s.camps(rooms, f, RESPAWN, "dagannoth", LH_DAGANNOTHS);
        s.camps(rooms, f, RESPAWN, "dagannoth_lvl92", LH_DAGANNOTHS_92);
        return c.build();
    }

    // ------------------------------------------------------------------ Kalphite Cave

    /** The cave entrance (3321, 3122) leads to cache tile (3305, 9497). */
    static final int KC_DX = 3321 - 3305;
    static final int KC_DY = 3122 - 9497;
    static final int[] KC_WORKERS = {3272, 9514, 3273, 9519, 3277, 9512, 3277, 9522, 3278, 9517, 3281, 9521, 3281, 9526, 3282, 9516,
            3284, 9524, 3315, 9502, 3318, 9496, 3319, 9507, 3322, 9502, 3323, 9495, 3324, 9510, 3327, 9500, 3327, 9506, 3331, 9501};
    static final int[] KC_SOLDIERS = {3299, 9532, 3301, 9528, 3303, 9537, 3305, 9516, 3305, 9522, 3309, 9480, 3309, 9529, 3309, 9537,
            3311, 9521, 3315, 9481, 3315, 9528, 3317, 9476, 3317, 9522, 3318, 9486, 3322, 9478, 3322, 9483, 3327, 9482, 3329, 9487,
            3333, 9490, 3336, 9494};
    static final int[] KC_GUARDIANS = {3268, 9482, 3275, 9478, 3277, 9492, 3278, 9485, 3284, 9481};

    /**
     * The Kalphite Cave east of the Shantay Pass: 18 workers, 20 soldiers and 5 guardians around the larvae's central
     * chamber (the larvae are not carved). As in OSRS the cave is multicombat ({@code MultiCombat.UNDERGROUND}) and its
     * kalphites can only be hurt on a kalphite Slayer task ({@link Dungeons#TASK_ONLY}). The real entrance (3321, 3122)
     * lies in the world module's levelled Shantay Pass, so the hut stands
     * 21 tiles east of it, above the eastern workers' chamber (approx).
     */
    static Dungeons.Dungeon kalphiteCave() {
        Cave c = new Cave("dng_kalphite_cave", "Kalphite Cave", mc("sandstone"), mc("coarse_dirt"), mc("ochre_froglight"));
        Shifted s = new Shifted(c, KC_DX, KC_DY);
        int f = 30;
        int[] centre = {3294, 9489, 3314, 9506};
        int[] west = {3268, 9509, 3288, 9530};
        int[] north = {3295, 9512, 3321, 9541};
        int[] east = {3315, 9493, 3338, 9511};
        int[] southEast = {3305, 9472, 3340, 9492};
        int[] southWest = {3264, 9474, 3290, 9496};
        for (int[] r : new int[][]{centre, west, north, east, southEast, southWest}) {
            s.room(r, f, 6);
        }
        s.hall(3294, 9500, 3280, 9509, f);
        s.hallZ(3305, 9506, 3305, 9512, f);
        s.hallZ(3308, 9489, 3308, 9492, f);
        s.hall(3294, 9492, 3290, 9492, f);
        c.carve();
        for (int[] p : new int[][]{{3300, 9496}, {3310, 9530}, {3325, 9485}, {3278, 9520}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("smooth_sandstone"));
        }
        c.set(s.x(3305), f + 1, s.z(9497), mc("dead_bush")); // under the real cave entrance
        c.entrance(s.x(3326), s.z(9497), f, mc("sandstone"), mc("smooth_sandstone"), 'w');
        int[][] rooms = {centre, west, north, east, southEast, southWest};
        s.camps(rooms, f, RESPAWN, "kalphite_worker", KC_WORKERS);
        s.camps(rooms, f, RESPAWN, "kalphite_soldier", KC_SOLDIERS);
        s.camps(rooms, f, RESPAWN, "kalphite_guardian", KC_GUARDIANS);
        return c.build();
    }

    // ------------------------------------------------------------------ Wilderness Slayer Cave

    /** The stairs (3260, 3663) lead down to cache tile (3385, 10052). */
    static final int WSC_DX = 3260 - 3385;
    static final int WSC_DY = 3663 - 10052;
    static final int[] WSC_ICE_GIANTS = {3333, 10052, 3334, 10060, 3336, 10056, 3342, 10056, 3345, 10052, 3350, 10052};
    static final int[] WSC_ANKOU = {3348, 10075, 3351, 10084, 3353, 10080, 3357, 10076, 3360, 10081, 3364, 10074, 3364, 10079,
            3369, 10068, 3370, 10064, 3372, 10066};
    static final int[] WSC_LESSER = {3332, 10125, 3333, 10130, 3335, 10138, 3337, 10134, 3339, 10144, 3340, 10130, 3341, 10138};
    static final int[] WSC_BLACK_DEMONS = {3354, 10120, 3359, 10114, 3361, 10125, 3367, 10117};
    static final int[] WSC_BLACK_DRAGONS = {3356, 10157, 3361, 10159, 3366, 10155};
    static final int[] WSC_GREEN_DRAGONS = {3396, 10119, 3397, 10124, 3402, 10119, 3410, 10065, 3415, 10068, 3418, 10063};
    static final int[] WSC_GREATER = {3421, 10153, 3421, 10159, 3424, 10148, 3425, 10156, 3426, 10143, 3428, 10151, 3432, 10148,
            3432, 10152, 3433, 10139, 3434, 10144};
    static final int[] WSC_HELLHOUNDS = {3439, 10082, 3439, 10088, 3444, 10077, 3444, 10096, 3445, 10085};
    static final int[] WSC_RATS = {3419, 10078, 3433, 10061, 3440, 10072};
    static final int[] WSC_BANDIT = {3430, 10070};

    /**
     * The Wilderness Slayer Cave under the Wilderness north of Varrock (the cache snapshot's layout): ice giants by the
     * stairs, ankou, lesser, black and greater demons, green and black dragons and hellhounds in their own caverns off a
     * northern spine, and the level 130 bandit and 3 rats in the eastern corner.
     */
    static Dungeons.Dungeon wildernessSlayerCave() {
        Cave c = new Cave("dng_wilderness_slayer_cave", "Wilderness Slayer Cave", mc("deepslate_bricks"), mc("tuff"), mc("shroomlight"));
        Shifted s = new Shifted(c, WSC_DX, WSC_DY);
        int f = 20;
        int[] stairs = {3378, 10044, 3392, 10058};
        int[] iceGiants = {3328, 10047, 3356, 10064};
        int[] ankou = {3344, 10062, 3376, 10090};
        int[] lesser = {3328, 10120, 3346, 10148};
        int[] black = {3350, 10108, 3372, 10132};
        int[] blackDragons = {3350, 10150, 3372, 10165};
        int[] greensNorth = {3390, 10112, 3408, 10130};
        int[] greensSouth = {3404, 10058, 3424, 10074};
        int[] greater = {3416, 10134, 3440, 10164};
        int[] hellhounds = {3434, 10074, 3450, 10100};
        int[] bandit = {3416, 10058, 3442, 10080}; // the bandit's and the rats' corner between the green dragons and hellhounds
        int[][] rooms = {stairs, iceGiants, ankou, lesser, black, blackDragons, greensNorth, greensSouth, greater, hellhounds, bandit};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hallZ(3385, 10058, 3385, 10121, f);
        s.hall(3385, 10121, 3390, 10121, f);
        s.hall(3378, 10052, 3356, 10052, f);
        s.hall(3385, 10076, 3376, 10076, f);
        s.hall(3385, 10118, 3372, 10118, f);
        s.hall(3350, 10128, 3346, 10128, f);
        s.hallZ(3360, 10132, 3360, 10150, f);
        s.hall(3408, 10124, 3428, 10134, f);
        s.hall(3392, 10052, 3410, 10058, f);
        s.hall(3424, 10070, 3440, 10074, f);
        c.carve();
        c.pool(s.x(3362), s.z(10163), s.x(3370), s.z(10160), f, mc("lava"));
        c.entrance(s.x(3385), s.z(10052), f, mc("deepslate_bricks"), mc("polished_deepslate"), 's');
        s.camps(rooms, f, RESPAWN, "ice_giant", WSC_ICE_GIANTS);
        s.camps(rooms, f, RESPAWN, "ankou", WSC_ANKOU);
        s.camps(rooms, f, RESPAWN, "lesser_demon", WSC_LESSER);
        s.camps(rooms, f, RESPAWN, "black_demon", WSC_BLACK_DEMONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "black_dragon", WSC_BLACK_DRAGONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "green_dragon", WSC_GREEN_DRAGONS);
        s.camps(rooms, f, RESPAWN, "greater_demon", WSC_GREATER);
        s.camps(rooms, f, RESPAWN, "hellhound", WSC_HELLHOUNDS);
        s.camps(rooms, f, RESPAWN, "rat", WSC_RATS);
        s.camps(rooms, f, RESPAWN, "wilderness_bandit", WSC_BANDIT);
        return c.build();
    }

    // ------------------------------------------------------------------ Forthos Dungeon

    /** The ladder in the eastern ruin (1702, 3575) leads to cache tile (1830, 9973). */
    static final int FORTHOS_DX = 1702 - 1830;
    static final int FORTHOS_DY = 3575 - 9973;
    static final int[] FORTHOS_SPIDERS = {1829, 9960, 1830, 9958, 1832, 9960, 1833, 9957, 1834, 9953, 1835, 9960, 1836, 9950,
            1837, 9952, 1837, 9955, 1838, 9958, 1838, 9965, 1839, 9960, 1840, 9951, 1840, 9956, 1841, 9963, 1842, 9953, 1842, 9959,
            1843, 9957, 1844, 9961, 1845, 9954, 1845, 9958};
    static final int[] FORTHOS_DRUIDS = {1799, 9937, 1799, 9940, 1802, 9937, 1802, 9940, 1808, 9961, 1808, 9964, 1808, 9967,
            1808, 9970, 1812, 9961, 1812, 9964, 1812, 9967, 1812, 9970, 1847, 9939, 1848, 9937, 1848, 9941, 1851, 9937, 1851, 9941,
            1852, 9939};
    static final int[] FORTHOS_RED_DRAGONS = {1812, 9933, 1815, 9937, 1816, 9927, 1821, 9929, 1822, 9937, 1823, 9933};
    static final int[] FORTHOS_BABY_RED_DRAGONS = {1811, 9937, 1818, 9931, 1827, 9930, 1827, 9937, 1828, 9928};

    /**
     * The Forthos Dungeon beneath the Forthos Ruin in Hosidius: the Spider's Den (21 temple spiders), the red dragons'
     * hall (6 red and 5 baby red dragons) and the undead druids' three shrines (18). Two ways in, as in OSRS: the ladder
     * of the eastern ruin and the stairs of the Forthos Ruin (1670, 3569; the landing is two tiles north, approx).
     * Sarachnis' lair (south) is the bosses module's; its Forthos Dungeon place stands on the ladder tile, so the hut is
     * four tiles north of it (approx).
     */
    static Dungeons.Dungeon forthosDungeon() {
        Cave c = new Cave("dng_forthos_dungeon", "Forthos Dungeon", mc("mossy_stone_bricks"), mc("cracked_stone_bricks"), mc("shroomlight"));
        Shifted s = new Shifted(c, FORTHOS_DX, FORTHOS_DY);
        int f = 24;
        int[] ladderRoom = {1825, 9969, 1835, 9979};
        int[] spiders = {1826, 9946, 1849, 9968};
        int[] dragons = {1807, 9922, 1832, 9942};
        int[] druidsWest = {1795, 9933, 1806, 9944};
        int[] druidsNorth = {1804, 9958, 1816, 9974};
        int[] druidsEast = {1843, 9933, 1857, 9945};
        int[] stairsRoom = {1793, 9964, 1803, 9974};
        int[][] rooms = {ladderRoom, spiders, dragons, druidsWest, druidsNorth, druidsEast, stairsRoom};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hallZ(1830, 9969, 1830, 9968, f);
        s.hall(1849, 9950, 1850, 9945, f);
        s.hallZ(1828, 9946, 1828, 9942, f);
        s.hall(1807, 9938, 1806, 9938, f);
        s.hallZ(1798, 9944, 1798, 9964, f);
        s.hall(1803, 9968, 1804, 9968, f);
        c.carve();
        for (int[] p : new int[][]{{1810, 9926}, {1830, 9926}, {1810, 9940}, {1846, 9966}, {1828, 9948}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("chiseled_stone_bricks"));
        }
        c.set(s.x(1800), f + 1, s.z(9938), mc("lodestone")); // the druids' altars
        c.set(s.x(1810), f + 1, s.z(9966), mc("lodestone"));
        c.set(s.x(1850), f + 1, s.z(9939), mc("lodestone"));
        c.entrance(s.x(1830), s.z(9977), f, mc("mossy_stone_bricks"), mc("stone_bricks"), 'e');
        c.entrance(s.x(1798), s.z(9969), f, mc("mossy_stone_bricks"), mc("stone_bricks"), 'w');
        s.camps(rooms, f, RESPAWN, "temple_spider", FORTHOS_SPIDERS);
        s.camps(rooms, f, RESPAWN, "undead_druid", FORTHOS_DRUIDS);
        s.camps(rooms, f, DRAGON_RESPAWN, "red_dragon", FORTHOS_RED_DRAGONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_red_dragon", FORTHOS_BABY_RED_DRAGONS);
        return c.build();
    }

    // ------------------------------------------------------------------ Giants' Den

    /** The cave entrance (1420, 3588) leads to cache tile (1432, 9913). */
    static final int GD_DX = 1420 - 1432;
    static final int GD_DY = 3588 - 9913;
    static final int[] GD_MOSS = {1419, 9862, 1419, 9869, 1424, 9864, 1425, 9870, 1429, 9862, 1446, 9865, 1452, 9862, 1454, 9866,
            1459, 9867, 1460, 9862};
    static final int[] GD_HILL = {1426, 9887, 1428, 9884, 1429, 9880, 1432, 9888, 1433, 9879, 1436, 9883, 1439, 9886, 1444, 9881};
    static final int[] GD_FIRE_104 = {1451, 9908, 1455, 9906, 1457, 9897, 1461, 9903};
    static final int[] GD_FIRE_109 = {1451, 9900, 1455, 9902, 1460, 9894, 1460, 9907};

    /**
     * The Giants' Den below Zeah's Lovakengj-Shayzien hills: 10 moss giants, 8 hill giants and the eight level 104/109
     * fire giants on their cache tiles. The strange passage to the Catacombs of Kourend (1461, 9879) is not modelled.
     */
    static Dungeons.Dungeon giantsDen() {
        Cave c = new Cave("dng_giants_den", "Giants' Den", mc("cobbled_deepslate"), mc("coarse_dirt"), mc("shroomlight"));
        Shifted s = new Shifted(c, GD_DX, GD_DY);
        int f = 22;
        int[] landing = {1426, 9906, 1440, 9918};
        int[] fire = {1447, 9890, 1465, 9911};
        int[] hill = {1422, 9876, 1447, 9891};
        int[] moss = {1415, 9858, 1464, 9873};
        int[][] rooms = {landing, fire, hill, moss};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hallZ(1432, 9906, 1432, 9891, f);
        s.hallZ(1430, 9876, 1430, 9873, f);
        c.carve();
        c.pool(s.x(1456), s.z(9910), s.x(1462), s.z(9909), f, mc("lava")); // the fire giants' lava
        for (int[] p : new int[][]{{1440, 9866}, {1436, 9884}, {1456, 9900}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 7, mc("deepslate_bricks"));
        }
        c.entrance(s.x(1432), s.z(9913), f, mc("cobbled_deepslate"), mc("polished_deepslate"), 's');
        s.camps(rooms, f, RESPAWN, "moss_giant", GD_MOSS);
        s.camps(rooms, f, RESPAWN, "hill_giant", GD_HILL);
        s.camps(rooms, f, RESPAWN, "fire_giant_lvl104", GD_FIRE_104);
        s.camps(rooms, f, RESPAWN, "fire_giant_lvl109", GD_FIRE_109);
        return c.build();
    }

    // ------------------------------------------------------------------ Isle of Souls Dungeon

    /** Cache tile the hut's ladder lands on (approx: no transport row) and its surface tile on the island. */
    static final int IOS_DX = 2255 - 2142;
    static final int IOS_DY = 2925 - 9319;
    static final int[] IOS_BLUE = {2120, 9305, 2122, 9300, 2127, 9299, 2129, 9304};
    static final int[] IOS_BABY_BLUE = {2125, 9306, 2126, 9303, 2131, 9298};
    static final int[] IOS_FIRE = {2116, 9320, 2116, 9323, 2116, 9330, 2119, 9327, 2120, 9323, 2120, 9331, 2123, 9330, 2124, 9326};
    static final int[] IOS_GREATER = {2159, 9336, 2161, 9330, 2163, 9337, 2165, 9326, 2165, 9333, 2167, 9323, 2167, 9330};
    static final int[] IOS_IRON = {2148, 9293, 2153, 9290, 2160, 9294};
    static final int[] IOS_LESSER = {2149, 9337, 2152, 9332, 2155, 9335, 2156, 9330};
    static final int[] IOS_SKELETONS = {2131, 9322, 2150, 9298, 2151, 9316, 2153, 9299};
    /** The thieving chest (wiki map tile) in the hallway between the two dragon rooms. */
    static final int[] IOS_CHEST = {2139, 9299};

    /**
     * The Isle of Souls Dungeon: blue and baby blue dragons, fire giants, lesser and greater demons, iron dragons and
     * skeletons around a central cavern, at their cache counts. In OSRS the entrance is on the island's east coast
     * (2309, 2919), which lies off this world's traced island, so the hut stands inland to the west (approx), and the
     * landing cavern is a guess (no transport row). The dungeon's chests are the skilling module's.
     */
    static Dungeons.Dungeon isleOfSoulsDungeon() {
        Cave c = new Cave("dng_isle_of_souls_dungeon", "Isle of Souls Dungeon", mc("mossy_cobblestone"), mc("rooted_dirt"),
                mc("verdant_froglight"));
        Shifted s = new Shifted(c, IOS_DX, IOS_DY);
        int f = 18;
        int[] centre = {2126, 9308, 2158, 9328};
        int[] blues = {2112, 9294, 2136, 9310};
        int[] fire = {2110, 9316, 2128, 9336};
        int[] demons = {2146, 9320, 2170, 9340};
        int[] iron = {2144, 9286, 2164, 9308};
        int[][] rooms = {centre, blues, fire, demons, iron};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.room(new int[]{2136, 9296, 2144, 9302}, f, 4); // the hallway between the blue and iron dragon rooms
        c.carve();
        // The skilling module's Isle of Souls chest (28 Thieving), on its OSRS tile in that hallway.
        c.set(s.x(IOS_CHEST[0]), f + 1, s.z(IOS_CHEST[1]), rc("isle_of_souls_chest"));
        for (int[] p : new int[][]{{2134, 9318}, {2150, 9322}, {2118, 9300}, {2160, 9332}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 7, mc("mossy_stone_bricks"));
        }
        c.entrance(s.x(2142), s.z(9319), f, mc("mossy_cobblestone"), mc("mossy_stone_bricks"), 's');
        s.camps(rooms, f, DRAGON_RESPAWN, "blue_dragon", IOS_BLUE);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_blue_dragon", IOS_BABY_BLUE);
        s.camps(rooms, f, RESPAWN, "fire_giant", IOS_FIRE);
        s.camps(rooms, f, RESPAWN, "greater_demon", IOS_GREATER);
        s.camps(rooms, f, DRAGON_RESPAWN, "iron_dragon", IOS_IRON);
        s.camps(rooms, f, RESPAWN, "lesser_demon", IOS_LESSER);
        s.camps(rooms, f, RESPAWN, "skeleton_lvl25", IOS_SKELETONS);
        return c.build();
    }

    // ------------------------------------------------------------------ Myths' Guild dungeon

    /** The Mythic Statue (2457, 2849) leads to cache tile (1936, 9009, plane 1). */
    static final int MG_DX = 2457 - 1936;
    static final int MG_DY = 2849 - 9009;
    static final int[] MG_BABY_BLACK = {1888, 9003};
    static final int[] MG_BABY_BLUE = {1897, 9001, 1897, 9005, 1900, 8998};
    static final int[] MG_BABY_GREEN = {1900, 9010};
    static final int[] MG_BABY_RED = {1889, 8995, 1891, 8998};
    static final int[] MG_BLACK = {1942, 8967, 1946, 8971};
    static final int[] MG_BLUE = {1925, 8968, 1928, 8973, 1930, 8986, 1931, 8967, 1931, 8979};
    static final int[] MG_GREEN = {1939, 8991, 1944, 8996};
    static final int[] MG_RED = {1910, 8995, 1911, 9002, 1918, 9005};

    /**
     * The Myths' Guild dungeon (Dragon Slayer II): baby dragons in the west, then red, green, blue and black dragons, at
     * their cache counts. The way down is the Mythic Statue inside the world module's guild, so the hut stands east of
     * the guild with a passage to the statue's landing (approx); the systems module gates the guild itself.
     */
    static Dungeons.Dungeon mythsGuildDungeon() {
        Cave c = new Cave("dng_myths_guild_dungeon", "Myths' Guild dungeon", mc("deepslate_tiles"), mc("polished_deepslate"),
                mc("sea_lantern"));
        Shifted s = new Shifted(c, MG_DX, MG_DY);
        int f = 14;
        int[] babies = {1884, 8992, 1904, 9013};
        int[] reds = {1906, 8992, 1922, 9008};
        int[] landing = {1930, 9002, 1942, 9014};
        int[] greens = {1934, 8987, 1948, 9000};
        int[] blues = {1921, 8962, 1935, 8990};
        int[] blacks = {1936, 8962, 1950, 8976};
        int[] hut = {1960, 9004, 1966, 9014};
        int[][] rooms = {babies, reds, landing, greens, blues, blacks, hut};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hall(1904, 9000, 1906, 9000, f);
        s.hall(1922, 9004, 1930, 9004, f);
        s.hallZ(1938, 9002, 1938, 9000, f);
        s.hall(1935, 8970, 1936, 8970, f);
        s.hall(1942, 9009, 1960, 9009, f);
        c.carve();
        c.set(s.x(1936), f + 1, s.z(9009), mc("chiseled_deepslate")); // below the Mythic Statue
        c.entrance(s.x(1963), s.z(9009), f, mc("deepslate_tiles"), mc("polished_deepslate"), 'w');
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_black_dragon", MG_BABY_BLACK);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_blue_dragon", MG_BABY_BLUE);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_green_dragon", MG_BABY_GREEN);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_red_dragon", MG_BABY_RED);
        s.camps(rooms, f, DRAGON_RESPAWN, "black_dragon", MG_BLACK);
        s.camps(rooms, f, DRAGON_RESPAWN, "blue_dragon", MG_BLUE);
        s.camps(rooms, f, DRAGON_RESPAWN, "green_dragon", MG_GREEN);
        s.camps(rooms, f, DRAGON_RESPAWN, "red_dragon", MG_RED);
        return c.build();
    }

    // ------------------------------------------------------------------ Deep Wilderness Dungeon

    /** The staircase (3044, 3927) leads to cache tile (3044, 10322). */
    static final int DWD_DY = 3927 - 10322;
    static final int[] DWD_DWARVES = {3029, 10313, 3030, 10308, 3034, 10310, 3036, 10311};
    static final int[] DWD_FIRE = {3046, 10341, 3047, 10344, 3049, 10338, 3052, 10347, 3053, 10338};
    static final int[] DWD_HILL = {3042, 10317, 3044, 10306, 3045, 10308, 3045, 10311, 3045, 10321, 3046, 10315};
    static final int[] DWD_MONKS = {3020, 10355, 3060, 10333};
    static final int[] DWD_SPIDERS = {3015, 10318, 3016, 10323, 3019, 10313, 3020, 10322, 3020, 10329, 3025, 10330, 3026, 10339,
            3028, 10327, 3029, 10334, 3030, 10346, 3032, 10342, 3039, 10343};
    static final int[] DWD_SKELETONS = {2994, 10346, 2998, 10342, 2998, 10350, 3002, 10356, 3003, 10347, 3004, 10362, 3006, 10352,
            3006, 10357};

    /**
     * The Deep Wilderness Dungeon (level 50 Wilderness): hill giants by the stairs, chaos dwarves, shadow spiders, fire
     * giants, monks of Zamorak and the skeletons below the north-western ladder (3005, 3962; with this dungeon's single
     * shift its hut stands twelve tiles north of the real ladder, clear of the Wilderness Agility Course, approx).
     */
    static Dungeons.Dungeon deepWildernessDungeon() {
        Cave c = new Cave("dng_deep_wilderness_dungeon", "Deep Wilderness Dungeon", mc("blackstone"), mc("coarse_dirt"),
                mc("soul_lantern"));
        Shifted s = new Shifted(c, 0, DWD_DY);
        int f = 24;
        int[] stairs = {3038, 10302, 3050, 10326};
        int[] dwarves = {3026, 10304, 3038, 10316};
        int[] spiders = {3012, 10310, 3042, 10348};
        int[] fire = {3044, 10332, 3062, 10350};
        int[] north = {3012, 10348, 3026, 10358};
        int[] skeletons = {2990, 10340, 3010, 10372};
        int[][] rooms = {stairs, dwarves, spiders, fire, north, skeletons};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hall(3042, 10340, 3044, 10340, f);
        s.hall(3012, 10354, 3010, 10354, f);
        c.carve();
        c.set(s.x(3030), f + 1, s.z(10310), mc("anvil")); // the chaos dwarves' anvil
        c.entrance(s.x(3044), s.z(10322), f, mc("blackstone"), mc("polished_blackstone"), 's');
        c.entrance(s.x(3005), s.z(10369), f, mc("blackstone"), mc("polished_blackstone"), 's');
        s.camps(rooms, f, RESPAWN, "chaos_dwarf", DWD_DWARVES);
        s.camps(rooms, f, RESPAWN, "fire_giant", DWD_FIRE);
        s.camps(rooms, f, RESPAWN, "hill_giant", DWD_HILL);
        s.camps(rooms, f, RESPAWN, "monk_of_zamorak", DWD_MONKS);
        s.camps(rooms, f, RESPAWN, "shadow_spider", DWD_SPIDERS);
        s.camps(rooms, f, RESPAWN, "skeleton_lvl25", DWD_SKELETONS);
        return c.build();
    }

    // ------------------------------------------------------------------ Karamja Volcano dungeon

    /** The volcano's rocks (2856, 3167) lead to cache tile (2856, 9568). */
    static final int KV_DY = 3167 - 9568;
    static final int[] KV_LESSER = {2831, 9562, 2835, 9602, 2836, 9558, 2837, 9565, 2837, 9623, 2838, 9610, 2839, 9604, 2840, 9552,
            2841, 9559, 2843, 9557, 2845, 9612};
    static final int[] KV_SPIDERS = {2834, 9580, 2834, 9584, 2836, 9576, 2838, 9582, 2842, 9583};
    static final int[] KV_BATS = {2834, 9568, 2856, 9575, 2861, 9568};
    static final int[] KV_SKELETONS_22 = {2848, 9581, 2853, 9573, 2859, 9572, 2860, 9577, 2864, 9568};
    static final int[] KV_SKELETONS_25 = {2830, 9657, 2832, 9661, 2834, 9650, 2834, 9655, 2834, 9658, 2838, 9653, 2840, 9649};
    static final int[] KV_SKELETONS_45 = {2840, 9628, 2840, 9638, 2841, 9643, 2842, 9632, 2843, 9645, 2844, 9628, 2845, 9636};
    static final int[] KV_RATS = {2860, 9635, 2862, 9638};

    /**
     * The Karamja Volcano dungeon and the Crandor tunnels behind its hidden wall (Dragon Slayer I): skeletons and giant
     * bats by the rope, deadly red spiders and lesser demons along the western caverns, then the level 45 and 25
     * skeletons below Crandor, at their cache counts (Elvarg is the quests module's boss). The rope's tile is inside the
     * world module's volcano above the TzHaar city, so the hut stands 36 tiles south of it (approx). The Crandor hole
     * (2833, 3257) is not a way in here: the islands module's Crandor covers its tile.
     */
    static Dungeons.Dungeon karamjaVolcanoDungeon() {
        Cave c = new Cave("dng_karamja_volcano_dungeon", "Karamja Volcano dungeon", mc("basalt[axis=y]"), mc("blackstone"),
                mc("shroomlight"));
        Shifted s = new Shifted(c, 0, KV_DY);
        int f = -8; // below the TzHaar city
        int[] rope = {2846, 9562, 2868, 9584};
        int[] west = {2828, 9548, 2848, 9590};
        int[] westNorth = {2830, 9590, 2848, 9626};
        int[] crandor = {2836, 9626, 2850, 9648};
        int[] elvarg = {2850, 9630, 2866, 9648};
        int[] north = {2826, 9646, 2844, 9664};
        int[] hut = {2852, 9528, 2860, 9538};
        int[][] rooms = {rope, west, westNorth, crandor, elvarg, north, hut};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hallZ(2856, 9538, 2856, 9562, f);
        c.carve();
        c.pool(s.x(2852), s.z(9600), s.x(2846), s.z(9612), f, mc("lava"));
        c.fill(s.x(2835), f + 1, s.z(9599), s.x(2837), f + 3, s.z(9599), mc("cracked_deepslate_bricks")); // the hidden wall (open)
        c.set(s.x(2856), f + 1, s.z(9568), mc("chain")); // under the real rope
        c.entrance(s.x(2856), s.z(9532), f, mc("blackstone"), mc("polished_blackstone"), 's');

        s.camps(rooms, f, RESPAWN, "lesser_demon", KV_LESSER);
        s.camps(rooms, f, RESPAWN, "deadly_red_spider", KV_SPIDERS);
        s.camps(rooms, f, RESPAWN, "giant_bat", KV_BATS);
        s.camps(rooms, f, RESPAWN, "minecraft:skeleton", KV_SKELETONS_22);
        s.camps(rooms, f, RESPAWN, "skeleton_lvl25", KV_SKELETONS_25);
        s.camps(rooms, f, RESPAWN, "skeleton_lvl45", KV_SKELETONS_45);
        s.camps(rooms, f, RESPAWN, "rat", KV_RATS);
        return c.build();
    }

    // ------------------------------------------------------------------ Ice Troll Caves

    /** Jatizso's mine stairs (2399, 3812) lead to cache tile (2405, 10188). */
    static final int ITC_DX = 2399 - 2405;
    static final int ITC_DY = 3812 - 10188;
    /** The mine (cache plane 0). */
    static final int[] MINE_RUNTS = {2378, 10216, 2402, 10225, 2404, 10215, 2408, 10227, 2420, 10215};
    static final int[] MINE_FEMALES = {2401, 10217, 2403, 10223, 2409, 10223};
    static final int[] MINE_MALES = {2401, 10220, 2411, 10220};
    /** The caves (cache plane 1). */
    static final int[] ITC_FEMALES = {2381, 10285, 2382, 10275, 2388, 10272, 2388, 10283, 2393, 10280, 2398, 10278, 2416, 10272,
            2419, 10282, 2420, 10274};
    static final int[] ITC_GRUNTS = {2413, 10265, 2414, 10277, 2420, 10270, 2383, 10281, 2388, 10277, 2394, 10274};
    static final int[] ITC_MALES = {2376, 10280, 2378, 10271, 2386, 10268, 2397, 10271, 2401, 10285, 2415, 10284, 2421, 10266};
    static final int[] ITC_RUNTS = {2390, 10286, 2393, 10286, 2396, 10286, 2406, 10259, 2414, 10257};

    /**
     * Jatizso's mine with its northern troll-infested tunnels, and the Ice Troll Caves (The Fremennik Isles) beyond them:
     * ice troll runts, males, females and grunts at the cache's counts (the level 102 grunts count as level 100 ones,
     * and the Ice Troll King with his honour guards is the quests module's fight). In OSRS the caves are entered from
     * the islands' north coast (2317, 3893 and 2401, 3888), which this world's traced isles don't reach, so a passage
     * joins them to the mine (approx).
     */
    static Dungeons.Dungeon iceTrollCaves() {
        Cave c = new Cave("dng_ice_troll_caves", "Ice Troll Caves", mc("packed_ice"), mc("snow_block"), mc("sea_lantern"));
        Shifted s = new Shifted(c, ITC_DX, ITC_DY);
        int f = 16;
        int[] mine = {2374, 10183, 2424, 10230};
        int[] caves = {2372, 10254, 2425, 10298};
        int[][] rooms = {mine, caves};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hallZ(2400, 10230, 2400, 10254, f);
        c.carve();
        // Jatizso mine's rocks (wiki: 11 tin, 10 iron, 8 coal, 16 mithril, 14 adamantite) in rows south of the trolls
        // (rows and counts approx).
        c.rocks(s.x(2380), s.z(10192), s.x(2420), s.z(10192), f + 1, 8, rc("tin_rock"));
        c.rocks(s.x(2384), s.z(10196), s.x(2420), s.z(10196), f + 1, 8, rc("iron_rock"));
        c.rocks(s.x(2388), s.z(10200), s.x(2416), s.z(10200), f + 1, 8, rc("coal_rock"));
        c.rocks(s.x(2380), s.z(10204), s.x(2420), s.z(10204), f + 1, 5, rc("mithril_rock"));
        c.rocks(s.x(2380), s.z(10208), s.x(2420), s.z(10208), f + 1, 6, rc("adamantite_rock"));
        c.pool(s.x(2386), s.z(10262), s.x(2394), s.z(10260), f, mc("water")); // under the rope bridges
        // The stairs (2399, 3812) are inside the islands module's levelled Jatizso, so the hut stands 14 tiles west of
        // them, on the island's western rise (approx).
        c.entrance(s.x(2391), s.z(10186), f, mc("packed_ice"), mc("snow_block"), 's');
        s.camps(rooms, f, RESPAWN, "ice_troll_runt", MINE_RUNTS);
        s.camps(rooms, f, RESPAWN, "ice_troll_female", MINE_FEMALES);
        s.camps(rooms, f, RESPAWN, "ice_troll", MINE_MALES);
        s.camps(rooms, f, RESPAWN, "ice_troll_female", ITC_FEMALES);
        s.camps(rooms, f, RESPAWN, "ice_troll_grunt", ITC_GRUNTS);
        s.camps(rooms, f, RESPAWN, "ice_troll", ITC_MALES);
        s.camps(rooms, f, RESPAWN, "ice_troll_runt", ITC_RUNTS);
        return c.build();
    }

    // ------------------------------------------------------------------ Ourania Cave

    /** The cave's ladder (2453, 3231) lands next to Eniola, cache tile (3014, 5623). */
    static final int OUR_DX = 2453 - 3014;
    static final int OUR_DY = 3231 - 5623;
    /** The Ourania (ZMI) altar's cache tile (approx, the middle of the altar). */
    public static final int[] OURANIA_ALTAR = {3060, 5580};
    static final int[] OUR_LIZARDS = {3010, 5574, 3010, 5604, 3013, 5574, 3014, 5592, 3015, 5572, 3028, 5571, 3032, 5570, 3033, 5573,
            3045, 5579};
    static final int[] OUR_CRAFTERS = {3014, 5624, 3058, 5579};
    static final int[] OUR_MAGES = {3012, 5578, 3017, 5577, 3035, 5581};
    static final int[] OUR_RANGERS = {3011, 5596, 3045, 5576, 3031, 5574};
    static final int[] OUR_WARRIORS = {3015, 5583, 3028, 5574, 3015, 5606};

    /**
     * The Ourania Cave (Lunar Diplomacy's ZMI altar): Eniola's bank by the ladder, the western passage and the southern
     * cavern with the cave lizards and Zamorak's warriors, rangers, mages and crafters, and the Ourania altar in the
     * eastern chamber (the skilling module's altar block). The cave's shortcut and its Agility gaps are not modelled. The
     * hut stands a few tiles south-east of the real ladder, clear of the surface marker (approx).
     */
    static Dungeons.Dungeon ouraniaCave() {
        Cave c = new Cave("dng_ourania_cave", "Ourania Cave", mc("deepslate_bricks"), mc("cobbled_deepslate"), mc("shroomlight"));
        Shifted s = new Shifted(c, OUR_DX, OUR_DY);
        int f = 30;
        int[] entry = {3008, 5612, 3024, 5628};
        int[] west = {3008, 5590, 3020, 5612};
        int[] south = {3006, 5566, 3048, 5590};
        int[] altar = {3044, 5570, 3066, 5592};
        int[][] rooms = {entry, west, south, altar};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        c.carve();
        c.set(s.x(OURANIA_ALTAR[0]), f + 1, s.z(OURANIA_ALTAR[1]), rc("ourania_altar"));
        c.set(s.x(3012), f + 1, s.z(5626), rc("bank_booth")); // Eniola (approx: a plain bank booth)
        for (int[] p : new int[][]{{3030, 5582}, {3014, 5600}, {3054, 5586}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("purpur_pillar"));
        }
        c.entrance(s.x(3020), s.z(5618), f, mc("deepslate_bricks"), mc("polished_deepslate"), 'w');
        s.camps(rooms, f, RESPAWN, "cave_lizard", OUR_LIZARDS);
        s.camps(rooms, f, RESPAWN, "zamorak_crafter", OUR_CRAFTERS);
        s.camps(rooms, f, RESPAWN, "zamorak_mage", OUR_MAGES);
        s.camps(rooms, f, RESPAWN, "zamorak_ranger", OUR_RANGERS);
        s.camps(rooms, f, RESPAWN, "zamorak_warrior", OUR_WARRIORS);
        return c.build();
    }
}
