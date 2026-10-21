package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Zeah's dungeons (the zeah module carves only the Catacombs of Kourend): the Lizardman Caves, the
 * Karuulm Slayer Dungeon inside Mount Karuulm and the Chasm of Fire.
 */
final class DungeonsZeah {
    private DungeonsZeah() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(lizardmanCaves());
        out.add(karuulm());
        out.add(chasmOfFire());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /**
     * The Lizardman Caves below the Lizardman Settlement (entrance tile 1309, 3574): swampy
     * tunnels and pools where lizardman shamans gather.
     */
    static Dungeons.Dungeon lizardmanCaves() {
        Cave c = new Cave("dng_lizardman_caves", "Lizardman Caves", mc("mossy_cobblestone"), mc("mud"), mc("ochre_froglight"));
        int f = 18;
        int ox = bx(1309);
        int oz = bz(3574);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox - 6, oz, ox - 24, oz - 14, f, 3, 4);
        c.room(ox - 56, oz - 50, ox - 14, oz - 14, f, 6);
        c.hall(ox + 6, oz, ox + 24, oz - 14, f, 3, 4);
        c.room(ox + 12, oz - 46, ox + 50, oz - 14, f, 6);
        c.carve();
        c.pool(ox - 44, oz - 40, ox - 30, oz - 28, f, mc("water"));
        c.pool(ox + 24, oz - 36, ox + 36, oz - 26, f, mc("water"));
        for (int x = ox - 52; x <= ox + 46; x += 14) {
            c.set(x, f + 1, oz - 16, mc("big_dripleaf"));
        }
        c.entrance(ox, oz, f, mc("mossy_cobblestone"), mc("mud"), 's');
        c.camp(ox - 54, oz - 48, ox - 16, oz - 16, f, 5, 25, "lizardman_shaman");
        c.camp(ox + 14, oz - 44, ox + 48, oz - 16, f, 5, 25, "lizardman_shaman");
        return c.build();
    }

    /** The Mount Karuulm elevator (surface 1311, 3807) lands on cache tile (1311, 10188): the dungeon's shift. */
    static final int KARUULM_DX = 0;
    static final int KARUULM_DY = 3807 - 10188;
    /** Floors of the three levels (OSRS planes 0, 1 and 2), stacked under the mountain. */
    static final int KARUULM_F0 = 8;
    static final int KARUULM_F1 = 17;
    static final int KARUULM_F2 = 26;
    /** The entrance chamber with the Tasakaal (cache tiles): the only part of the dungeon whose floor doesn't burn. */
    static final int[] KARUULM_CHAMBER = {1296, 10184, 1321, 10214};
    // Cache spawn tiles (mejrs/data_osrs, the wiki's LocLines agree): planes 0, 1 and 2.
    static final int[] KARUULM_WYRMS = {1260, 10192, 1262, 10184, 1264, 10197, 1268, 10179, 1268, 10191, 1270, 10186, 1275, 10181,
            1275, 10189, 1280, 10186};
    static final int[] KARUULM_WYRMS_TASK = {1252, 10159, 1255, 10147, 1259, 10154, 1259, 10159, 1266, 10152, 1267, 10157, 1273, 10158};
    static final int[] KARUULM_HYDRAS = {1302, 10240, 1305, 10247, 1311, 10246, 1316, 10240, 1317, 10245, 1303, 10235, 1307, 10231,
            1310, 10235, 1315, 10235};
    static final int[] KARUULM_HYDRAS_TASK = {1304, 10260, 1304, 10267, 1309, 10266, 1310, 10261, 1311, 10271, 1324, 10267, 1326, 10261,
            1329, 10267};
    // The cache's east cluster holds 6 drakes where the wiki counts 7: the seventh tile is approx.
    static final int[] KARUULM_DRAKES = {1341, 10243, 1342, 10235, 1347, 10229, 1348, 10242, 1349, 10236, 1356, 10239, 1352, 10247};
    static final int[] KARUULM_DRAKES_TASK = {1301, 10240, 1307, 10232, 1308, 10253, 1309, 10246, 1310, 10239, 1317, 10234, 1319, 10241};
    static final int[] KARUULM_LIZARDS = {1326, 10179, 1326, 10183, 1327, 10172, 1330, 10183, 1331, 10178, 1333, 10171, 1334, 10189,
            1335, 10178, 1335, 10186, 1336, 10182, 1340, 10180, 1340, 10185, 1346, 10182};
    static final int[] KARUULM_GREATER_DEMONS = {1277, 10195, 1287, 10207, 1283, 10210, 1273, 10199, 1278, 10207, 1279, 10213,
            1284, 10199, 1272, 10204, 1279, 10200};
    static final int[] KARUULM_FIRE_GIANTS = {1273, 10212, 1277, 10215, 1281, 10205, 1269, 10211, 1274, 10197, 1272, 10194, 1275, 10209,
            1280, 10214, 1282, 10200, 1287, 10210, 1277, 10203, 1282, 10209, 1286, 10203};
    static final int[] KARUULM_HELLHOUNDS = {1330, 10194, 1334, 10192, 1334, 10200, 1336, 10212, 1339, 10209, 1340, 10198, 1340, 10202,
            1344, 10199, 1345, 10204, 1349, 10197, 1351, 10194, 1354, 10198};

    /**
     * The Karuulm Slayer Dungeon inside Mount Karuulm, on the cache's tiles (the elevator lands on 1311, 10188 below the
     * surface's 1311, 3807; the Alchemical Hydra's arena is the bosses module's). Its hut stands at the mountain's western
     * foot (surface tile 1299, 3806; approx: the summit holds the bosses' arena and Konar's camp) and its ladder lands in the
     * entrance chamber with the Tasakaal. Lower level: the wyrms to the west, past a lava gap the task-only wyrms (Garbek quo
     * Maten), the hydras to the north and past another lava gap the task-only hydras (Tarfol quo Maten) before the
     * Alchemical Hydra's door. Middle level (up the steps east of the chamber): the sulphur lizards, the greater demons and
     * the drakes, through a tunnel the task-only drakes (Kormar quo Maten). Upper level: the fire giants and the hellhounds.
     * Spawn counts are the wiki's (wyrms 9 + 7 task-only, hydras 9 + 8, drakes 7 + 7, sulphur lizards 13, greater demons 9,
     * fire giants 13, hellhounds 12); single-way combat throughout. Room outlines around the spawn clusters, the passages and
     * the lair's antechamber are approx (from the transports: rocks, lava gaps, steps and the tunnel).
     */
    static Dungeons.Dungeon karuulm() {
        Cave c = new Cave("dng_karuulm_slayer_dungeon", "Karuulm Slayer Dungeon", mc("blackstone"), mc("basalt[axis=y]"),
                mc("shroomlight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, KARUULM_DX, KARUULM_DY);
        int f0 = KARUULM_F0;
        int f1 = KARUULM_F1;
        int f2 = KARUULM_F2;
        // Lower level.
        int[] chamber = KARUULM_CHAMBER;
        int[] wyrms = {1256, 10176, 1284, 10200};
        int[] wyrmsTask = {1248, 10144, 1279, 10166};
        int[] hydras = {1298, 10221, 1322, 10250};
        int[] hydrasTask = {1300, 10255, 1333, 10274};
        int[] lair = {1338, 10228, 1356, 10262};          // outside the Alchemical Hydra's door
        int[][] lower = {chamber, wyrms, wyrmsTask, hydras, hydrasTask, lair};
        for (int[] r : lower) {
            s.room(r, f0, 6);
        }
        s.hall(1296, 10205, 1284, 10195, f0);              // the rocks west of the chamber to the wyrms
        s.hallZ(1270, 10176, 1270, 10166, f0);             // the lava gap (1270, 10170) to the task-only wyrms
        s.hallZ(1312, 10214, 1312, 10221, f0);             // the rocks north of the chamber (1312, 10216) to the hydras
        s.hallZ(1308, 10250, 1308, 10255, f0);             // the lava gap (1308, 10252) to the task-only hydras
        s.hall(1333, 10265, 1338, 10265, f0);              // the rocks (1351, 10250) and the lair's antechamber
        // Middle level: the steps east of the chamber (1329, 10205) climb to (1334, 10205).
        int[] hub1 = {1334, 10196, 1344, 10212};
        int[] lizards = {1322, 10168, 1350, 10192};
        int[] demons = {1268, 10191, 1291, 10217};
        int[] drakes = {1337, 10225, 1360, 10256};
        int[] drakesTask = {1297, 10228, 1330, 10257};
        int[] stepsUp = {1306, 10185, 1313, 10193};
        int[][] middle = {hub1, lizards, demons, drakes, drakesTask, stepsUp};
        for (int[] r : middle) {
            s.room(r, f1, 6);
        }
        s.hall(1321, 10205, 1329, 10205, f0);
        c.stairs(s.x(1329), s.z(10205), 'e', f0, f1, 3, 5);
        s.hallZ(1336, 10196, 1336, 10192, f1);             // to the sulphur lizards
        s.hall(1334, 10210, 1291, 10210, f1);              // west to the greater demons
        s.hallZ(1310, 10210, 1310, 10193, f1);             // and down to the steps up
        s.hallZ(1340, 10212, 1340, 10225, f1);             // north to the drakes
        c.hall(s.x(1330), s.z(10238), s.x(1337), s.z(10238), f1, 2, 3); // the tunnel to the task-only drakes
        // Upper level: the steps (1313, 10189) climb to (1318, 10189).
        int[] hub2 = {1318, 10183, 1326, 10195};
        int[] giants = {1265, 10190, 1291, 10219};
        int[] hounds = {1327, 10188, 1358, 10216};
        int[][] upper = {hub2, giants, hounds};
        for (int[] r : upper) {
            s.room(r, f2, 7);
        }
        c.stairs(s.x(1313), s.z(10189), 'e', f1, f2, 3, 5);
        s.hall(1318, 10190, 1291, 10204, f2);              // west to the fire giants
        s.hall(1326, 10190, 1327, 10190, f2);
        c.carve();
        // Lava: the gaps and pools of the lower level.
        c.pool(s.x(1264), s.z(10171), s.x(1268), s.z(10170), f0, mc("lava"));
        c.pool(s.x(1273), s.z(10171), s.x(1277), s.z(10170), f0, mc("lava"));
        c.pool(s.x(1302), s.z(10253), s.x(1306), s.z(10252), f0, mc("lava"));
        c.pool(s.x(1310), s.z(10253), s.x(1314), s.z(10252), f0, mc("lava"));
        c.pool(s.x(1316), s.z(10270), s.x(1320), s.z(10264), f0, mc("lava"));
        c.pool(s.x(1352), s.z(10236), s.x(1355), s.z(10231), f0, mc("lava"));
        for (int x = 1262; x <= 1278; x += 8) {
            c.pillar(s.x(x), s.z(10196), f0, 6, mc("polished_basalt[axis=y]"));
        }
        // The Tasakaal's statues in the entrance chamber, and the Alchemical Hydra's door.
        c.set(s.x(1305), f0 + 1, s.z(10208), mc("chiseled_polished_blackstone"));
        c.set(s.x(1315), f0 + 1, s.z(10208), mc("chiseled_polished_blackstone"));
        c.arenaDoor(DungeonBlocks.IMPOSING_DOOR, s.x(1355), s.z(10258), f0, "alchemical_hydra",
                "You open the Alchemical door and step into the Alchemical Hydra's laboratory.");
        c.entrance(s.x(1299), s.z(10187), f0, mc("polished_blackstone_bricks"), mc("blackstone"), 'w');
        // Task-only rooms (OSRS Wiki "Slayer guardians").
        s.taskOnly(wyrmsTask, f0, 6, "wyrms", "Garbek quo Maten");
        s.taskOnly(hydrasTask, f0, 6, "hydras", "Tarfol quo Maten");
        s.taskOnly(drakesTask, f1, 6, "drakes", "Kormar quo Maten");
        s.camps(lower, f0, CacheDungeons.RESPAWN, "wyrm", KARUULM_WYRMS);
        s.camps(lower, f0, CacheDungeons.RESPAWN, "wyrm", KARUULM_WYRMS_TASK);
        s.camps(lower, f0, CacheDungeons.RESPAWN, "hydra", KARUULM_HYDRAS);
        s.camps(lower, f0, CacheDungeons.RESPAWN, "hydra", KARUULM_HYDRAS_TASK);
        s.camps(middle, f1, CacheDungeons.RESPAWN, "drake", KARUULM_DRAKES);
        s.camps(middle, f1, CacheDungeons.RESPAWN, "drake", KARUULM_DRAKES_TASK);
        s.camps(middle, f1, CacheDungeons.RESPAWN, "sulphur_lizard", KARUULM_LIZARDS);
        s.camps(middle, f1, CacheDungeons.RESPAWN, "greater_demon", KARUULM_GREATER_DEMONS);
        s.camps(upper, f2, CacheDungeons.RESPAWN, "fire_giant", KARUULM_FIRE_GIANTS);
        s.camps(upper, f2, CacheDungeons.RESPAWN, "hellhound", KARUULM_HELLHOUNDS);
        return c.build();
    }

    /** True if block column (x, z) lies in the Karuulm Slayer Dungeon's entrance chamber (cache tiles {@link #KARUULM_CHAMBER}). */
    static boolean inKaruulmChamber(int x, int z) {
        int[] r = KARUULM_CHAMBER;
        return x >= StructureDef.bx(r[0] + KARUULM_DX) && x <= StructureDef.bx(r[2] + KARUULM_DX) + 1
                && z >= StructureDef.bz(r[3] + KARUULM_DY) && z <= StructureDef.bz(r[1] + KARUULM_DY) + 1;
    }

    /**
     * The Chasm of Fire north-west of Shayzien (entrance tile 1433, 3671): a great shaft with three
     * ledges spiralling down - lesser demons, then greater demons, then black demons at the bottom -
     * joined by ladders.
     */
    static Dungeons.Dungeon chasmOfFire() {
        Cave c = new Cave("dng_chasm_of_fire", "Chasm of Fire", mc("blackstone"), mc("polished_blackstone"), mc("shroomlight"));
        int ox = bx(1433);
        int oz = bz(3671);
        int cx = ox;
        int cz = oz + 40;
        int[] floors = {30, 10, -10};
        for (int f : floors) {
            c.disc(cx, cz, 30, f, 8);
        }
        c.hallZ(ox, oz, ox, cz - 30, floors[0], 3, 5);
        c.room(ox - 4, oz - 4, ox + 4, oz + 4, floors[0], 5);
        c.carve();
        // A lava pit in the middle of each ledge (the chasm), ringed by blackstone.
        for (int f : floors) {
            for (int dz = -8; dz <= 8; dz++) {
                int half = (int) Math.floor(Math.sqrt(8.5 * 8.5 - dz * dz));
                c.pool(cx - half, cz + dz, cx + half, cz + dz, f, mc("lava"));
            }
        }
        c.ladderUp(cx + 20, cz, floors[1], floors[0]);
        c.ladderUp(cx - 20, cz, floors[2], floors[1]);
        c.entrance(ox, oz, floors[0], mc("polished_blackstone_bricks"), mc("blackstone"), 'n');
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[0], 5, 25, "lesser_demon");
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[1], 4, 25, "greater_demon");
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[2], 3, 50, "black_demon");
        return c.build();
    }
}
