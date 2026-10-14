package com.runecraft.osrs.content.dungeons;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * The Poison Waste Dungeon (The Path of Glouphrie): Arposandra's sewers below the canyon east of the Poison Waste, home
 * of the warped creatures. Its monsters stand on the wiki's LocLine tiles (Kiyogitpy/osrs-wiki-data, July 2026): 15
 * warped tortoises and 36 level 96 warped terrorbirds on the main and lower levels (the two pins at x 1352-1360 are the
 * quest's instance and are left out, matching the dungeon page's 15 tortoises). The entrance tile is not in the
 * transport data: the hut stands west of the Poison Waste spirit tree (2339, 3109) on the waste's edge, and the shift
 * puts it over the south end of the main level (approx). The terrorbird guards, the emergency button and the door to
 * Arposandra are not modelled.
 */
final class PoisonWasteDungeon {
    static final int RESPAWN = CacheDungeons.RESPAWN;
    /** Hut tile (2325, 3100, approx) over cache tile (1480, 4240). */
    static final int PW_DX = 2325 - 1480;
    static final int PW_DY = 3100 - 4240;
    /** Main level (plane 1). */
    static final int[] PW_TORTOISES = {1479, 4317, 1494, 4323, 1499, 4318, 1476, 4237, 1479, 4249, 1484, 4237, 1488, 4262, 1490, 4252,
            1503, 4263, 1505, 4282, 1507, 4277, 1514, 4253, 1519, 4262};
    static final int[] PW_TERRORBIRDS = {1487, 4291, 1487, 4300, 1492, 4337, 1497, 4335, 1498, 4291, 1498, 4342, 1499, 4303, 1513, 4291,
            1519, 4302, 1525, 4295, 1477, 4255, 1478, 4261, 1480, 4265, 1483, 4268, 1484, 4276, 1484, 4284, 1493, 4239, 1494, 4234,
            1495, 4264, 1498, 4230, 1499, 4253, 1502, 4237, 1503, 4268, 1505, 4232, 1508, 4252, 1508, 4262, 1510, 4230, 1515, 4239,
            1516, 4233, 1517, 4265, 1521, 4257, 1523, 4235, 1531, 4234};
    /** Lower level (plane 0). */
    static final int[] PW_LOWER_TORTOISES = {1526, 4249, 1527, 4241};
    static final int[] PW_LOWER_TERRORBIRDS = {1487, 4281, 1491, 4279, 1495, 4280};

    private PoisonWasteDungeon() {
    }

    static Dungeons.Dungeon dungeon() {
        Cave c = new Cave("dng_poison_waste_dungeon", "Poison Waste Dungeon", mc("mud_bricks"), mc("packed_mud"), mc("verdant_froglight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, PW_DX, PW_DY);
        int main = 28;
        int lower = 12;
        int[] south = {1473, 4227, 1535, 4286};
        int[] north = {1474, 4288, 1530, 4345};
        int[] lowerWest = {1484, 4275, 1499, 4285};
        int[] lowerEast = {1521, 4236, 1532, 4254};
        int[][] mainRooms = {south, north};
        int[][] lowerRooms = {lowerWest, lowerEast};
        for (int[] r : mainRooms) {
            s.room(r, main, 6);
        }
        for (int[] r : lowerRooms) {
            s.room(r, lower, 5);
        }
        s.hallZ(1490, 4286, 1490, 4288, main);
        s.hallZ(1512, 4286, 1512, 4288, main);
        s.hall(1499, 4280, 1521, 4250, lower); // the lower level's sewer tunnel (approx)
        c.carve();
        // Toxic sludge channels along the sewers (approx).
        c.pool(s.x(1504), s.z(4244), s.x(1508), s.z(4232), main, mc("mud"));
        c.pool(s.x(1500), s.z(4330), s.x(1520), s.z(4326), main, mc("mud"));
        c.ladderUp(s.x(1494), s.z(4283), lower, main);
        c.ladderUp(s.x(1529), s.z(4245), lower, main);
        c.entrance(s.x(1480), s.z(4240), main, mc("mud_bricks"), mc("packed_mud"), 'w');
        s.camps(mainRooms, main, RESPAWN, "warped_tortoise", PW_TORTOISES);
        s.camps(mainRooms, main, RESPAWN, "warped_terrorbird_lvl96", PW_TERRORBIRDS);
        s.camps(lowerRooms, lower, RESPAWN, "warped_tortoise", PW_LOWER_TORTOISES);
        s.camps(lowerRooms, lower, RESPAWN, "warped_terrorbird_lvl96", PW_LOWER_TERRORBIRDS);
        return c.build();
    }
}
