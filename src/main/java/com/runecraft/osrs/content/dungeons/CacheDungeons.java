package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Dungeons carved from the cache's spawn tiles (mejrs/data_osrs NPCList_OSRS.json) with one constant shift each, like
 * {@link KruksDungeon}: the Shadow Dungeon (Desert Treasure I), the Sophanem Dungeon (Contact!), the Lighthouse
 * basement and its dagannoth cave (Horror from the Deep), the Kalphite Cave, the Wilderness Slayer Cave and the Forthos
 * Dungeon. Every camp holds exactly the cache's number of each monster inside its room (rooms are boxes around the
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

    /**
     * The Shadow Dungeon east of Baxtorian Falls: the ladder's chamber, the western caverns and the great eastern hall
     * where Damis waits (the quests module fights him at its own arena). Its 43 giant skeletons, 10 shadow hounds, giant
     * bat and 2 giant rats stand on their cache tiles (the 12 rats and 8 bats are left out). The ladder is only visible
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
     * chamber (the larvae are not carved; in OSRS the cave's kalphites can only be fought on a Slayer task, not enforced
     * here, approx). The real entrance (3321, 3122) lies in the world module's levelled Shantay Pass, so the hut stands
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

    /**
     * The Wilderness Slayer Cave under the Wilderness north of Varrock (the cache snapshot's layout): ice giants by the
     * stairs, ankou, lesser, black and greater demons, green and black dragons and hellhounds in their own caverns off a
     * northern spine (the bandit and rats are left out).
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
        int[][] rooms = {stairs, iceGiants, ankou, lesser, black, blackDragons, greensNorth, greensSouth, greater, hellhounds};
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
}
