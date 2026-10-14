package com.runecraft.osrs.content.dungeons;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * The Shayzien Crypts under the Graveyard of Heroes (Dragon Slayer II; Kourend & Kebos hard diary: kill a zombie here):
 * three floors (cache planes 1-3, tiles 1481-1526, 9927-9979) with the cache's 48 zombies and 46 skeletons, each spawn in
 * its melee, ranged or magic version (mejrs/data_osrs ids 8067-8072). Each floor is one hall over the cache's spawn box
 * (approx: the crypt corridors and tombs are not carved), joined by ladders; a multicombat area like every underground
 * room ({@code MultiCombat.UNDERGROUND}). The way in is a hut at the real entrance tile (1483, 3550, approx).
 */
final class ShayzienCrypts {
    /** The hut at the crypt entrance in the Graveyard of Heroes; the floors lie under the graveyard (shift approx). */
    static final int ENTRANCE_X = 1483;
    static final int ENTRANCE_Y = 3550;
    static final int DY = 3550 - 9955;

    static final int[] SKELETON_MAGIC_1 = {1481, 9950, 1495, 9947, 1504, 9930, 1511, 9961};
    static final int[] SKELETON_MAGIC_2 = {1481, 9945, 1488, 9955, 1492, 9970, 1502, 9941, 1507, 9966, 1513, 9950, 1519, 9938, 1526, 9960};
    static final int[] SKELETON_MAGIC_3 = {1491, 9957, 1516, 9932};
    static final int[] SKELETON_MELEE_1 = {1496, 9958, 1509, 9945, 1511, 9965, 1512, 9932, 1521, 9960, 1522, 9973};
    static final int[] SKELETON_MELEE_2 = {1486, 9933, 1487, 9962, 1502, 9956, 1515, 9969, 1519, 9955, 1526, 9950};
    static final int[] SKELETON_MELEE_3 = {1483, 9943, 1504, 9964, 1507, 9937, 1524, 9948};
    static final int[] SKELETON_RANGED_1 = {1483, 9961, 1484, 9944, 1496, 9968, 1503, 9948, 1514, 9950, 1515, 9979};
    static final int[] SKELETON_RANGED_2 = {1481, 9950, 1493, 9958, 1505, 9942, 1507, 9978, 1516, 9930, 1520, 9940};
    static final int[] SKELETON_RANGED_3 = {1499, 9951, 1503, 9932, 1506, 9978, 1516, 9957};
    static final int[] ZOMBIE_MAGIC_1 = {1484, 9973, 1497, 9942, 1500, 9954, 1523, 9962};
    static final int[] ZOMBIE_MAGIC_2 = {1488, 9939, 1494, 9930, 1498, 9979, 1503, 9952, 1520, 9962};
    static final int[] ZOMBIE_MAGIC_3 = {1483, 9963, 1501, 9966, 1501, 9977, 1506, 9950, 1524, 9962};
    static final int[] ZOMBIE_MELEE_1 = {1485, 9964, 1488, 9950, 1501, 9973, 1510, 9958, 1511, 9939, 1515, 9974};
    static final int[] ZOMBIE_MELEE_2 = {1481, 9939, 1497, 9950, 1500, 9936, 1500, 9964, 1503, 9971, 1514, 9942, 1521, 9945};
    static final int[] ZOMBIE_MELEE_3 = {1491, 9932, 1503, 9957, 1516, 9943, 1516, 9971, 1524, 9937};
    static final int[] ZOMBIE_RANGED_1 = {1492, 9978, 1496, 9932, 1504, 9969, 1510, 9953, 1523, 9946};
    static final int[] ZOMBIE_RANGED_2 = {1481, 9964, 1493, 9943, 1502, 9943, 1506, 9927, 1506, 9934, 1526, 9934};
    static final int[] ZOMBIE_RANGED_3 = {1483, 9966, 1491, 9945, 1491, 9971, 1503, 9943, 1523, 9935};

    private ShayzienCrypts() {
    }

    static Dungeons.Dungeon dungeon() {
        Cave c = new Cave("dng_shayzien_crypts", "Shayzien Crypts", mc("deepslate_bricks"), mc("polished_deepslate"),
                mc("soul_lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, DY);
        int[] floors = {40, 28, 16}; // cache planes 1, 2 and 3
        int[] hall = {1478, 9924, 1529, 9982};
        for (int f : floors) {
            s.room(hall, f, 6);
        }
        c.carve();
        for (int f : floors) {
            for (int x = 1486; x <= 1522; x += 12) {
                for (int y = 9932; y <= 9976; y += 11) {
                    c.pillar(s.x(x), s.z(y), f, 6, mc("chiseled_deepslate"));
                }
            }
        }
        c.ladderUp(s.x(1527), s.z(9926), floors[1], floors[0]);
        c.ladderUp(s.x(1480), s.z(9980), floors[2], floors[1]);
        c.entrance(s.x(ENTRANCE_X), s.z(ENTRANCE_Y - DY), floors[0], mc("deepslate_bricks"), mc("polished_deepslate"), 'e');
        int[][] rooms = {hall};
        int[][][] byPlane = {
                {ZOMBIE_MELEE_1, ZOMBIE_RANGED_1, ZOMBIE_MAGIC_1, SKELETON_MELEE_1, SKELETON_RANGED_1, SKELETON_MAGIC_1},
                {ZOMBIE_MELEE_2, ZOMBIE_RANGED_2, ZOMBIE_MAGIC_2, SKELETON_MELEE_2, SKELETON_RANGED_2, SKELETON_MAGIC_2},
                {ZOMBIE_MELEE_3, ZOMBIE_RANGED_3, ZOMBIE_MAGIC_3, SKELETON_MELEE_3, SKELETON_RANGED_3, SKELETON_MAGIC_3}};
        String[] ids = {"zombie_melee", "zombie_ranged", "zombie_magic", "skeleton_melee", "skeleton_ranged", "skeleton_magic"};
        for (int p = 0; p < floors.length; p++) {
            for (int i = 0; i < ids.length; i++) {
                s.camps(rooms, floors[p], CacheDungeons.RESPAWN, "shayzien_crypt_" + ids[i], byPlane[p][i]);
            }
        }
        return c.build();
    }
}
