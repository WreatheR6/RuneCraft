package com.runecraft.osrs.content.dungeons;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Varlamore's Slayer dungeons, carved like {@link CacheDungeons} (one constant shift each, rooms boxed around the spawn
 * clusters): Neypotzli below Cam Torum, the Ruins of Tapoyauik below the Tower of Ascension, the Tonali Cavern below
 * the Crypt of Tonali and the Stalker Den below the Custodia Pass. The cache dump (mejrs/data_osrs) predates these
 * areas, so every monster stands on the tiles of the wiki's LocLine maps (Kiyogitpy/osrs-wiki-data, July 2026) and
 * every camp holds exactly that many. The passages between the rooms follow the wiki's layout maps where it has one
 * and are approximate elsewhere.
 */
final class VarlamoreDungeons {
    static final int RESPAWN = CacheDungeons.RESPAWN;
    static final int DRAGON_RESPAWN = CacheDungeons.DRAGON_RESPAWN;

    private VarlamoreDungeons() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(neypotzli());
        out.add(ruinsOfTapoyauik());
        out.add(tonaliCavern());
        out.add(stalkerDen());
        return out;
    }

    /** {x, y} repeated n times: a wiki pin that stands for several spawns. */
    static int[] repeat(int x, int y, int n) {
        int[] out = new int[2 * n];
        for (int i = 0; i < n; i++) {
            out[2 * i] = x;
            out[2 * i + 1] = y;
        }
        return out;
    }

    // ------------------------------------------------------------------ Neypotzli

    /**
     * Cam Torum's entrance (1435, 3128) leads to (1439, 9509); the same shift puts Neypotzli's entrance at the north end
     * of the city (1440, 9602) under surface tile (1440, 3221).
     */
    static final int NEY_DX = 0;
    static final int NEY_DY = 3128 - 9509;
    static final int[] NEY_WYRMLINGS = {1363, 9707, 1366, 9704, 1366, 9709, 1367, 9693, 1367, 9697, 1371, 9695};
    static final int[] NEY_GRIMY_LIZARDS = {1483, 9716, 1489, 9704, 1490, 9714, 1494, 9711, 1498, 9709, 1499, 9718, 1504, 9714,
            1510, 9715};

    /**
     * Neypotzli, the Moons of Peril's prison below Cam Torum (Perilous Moons): the antechamber with its obsidian
     * icosahedron, the Earthbound Cavern (north-west; 6 wyrmlings) and the Streambound Cavern (north-east; 8 grimy
     * lizards, its stream), joined by the passages of the wiki's layout map. The three moons' chambers are the bosses
     * module's arenas, the Ancient prison and shrine are not carved, and the moonlight cockatrices are not in the game
     * yet. In OSRS the way in is at the north end of the underground city; Cam Torum is a surface place here, so a hut
     * stands over the prison's entrance (approx).
     */
    static Dungeons.Dungeon neypotzli() {
        Cave c = new Cave("dng_neypotzli", "Neypotzli", mc("deepslate_tiles"), mc("polished_deepslate"), mc("verdant_froglight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, NEY_DX, NEY_DY);
        int f = 20;
        int[] antechamber = {1424, 9552, 1456, 9606};
        int[] earthbound = {1346, 9670, 1406, 9722};
        int[] streambound = {1478, 9668, 1528, 9724};
        int[][] rooms = {antechamber, earthbound, streambound};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hallZ(1426, 9606, 1406, 9696, f);   // antechamber - Earthbound cavern
        s.hallZ(1454, 9606, 1478, 9666, f);   // antechamber - Streambound cavern
        s.hall(1406, 9716, 1478, 9716, f);    // Earthbound cavern - Streambound cavern (north passage)
        c.carve();
        c.pillar(s.x(1440), s.z(9578), f, 7, mc("obsidian"));                 // the obsidian icosahedron (approx)
        c.set(s.x(1440), f + 1, s.z(9579), mc("crying_obsidian"));
        c.pool(s.x(1484), s.z(9690), s.x(1522), s.z(9698), f, mc("water"));  // the stream (bream spots)
        c.pool(s.x(1352), s.z(9712), s.x(1360), s.z(9718), f, mc("water"));
        for (int[] p : new int[][]{{1360, 9680}, {1390, 9700}, {1500, 9680}, {1515, 9710}, {1432, 9560}, {1448, 9560}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 7, mc("polished_deepslate"));
        }
        c.entrance(s.x(1440), s.z(9602), f, mc("deepslate_tiles"), mc("polished_deepslate"), 's');
        s.camps(rooms, f, RESPAWN, "wyrmling", NEY_WYRMLINGS);
        s.camps(rooms, f, RESPAWN, "grimy_lizard", NEY_GRIMY_LIZARDS);
        return c.build();
    }

    // ------------------------------------------------------------------ Ruins of Tapoyauik

    /**
     * The entrance under the Tower of Ascension (1693, 3232). Its landing tile inside is not in the transport data, so
     * the shift puts it on the top floor next to the cave crawlers (approx).
     */
    static final int TAP_DX = 0;
    static final int TAP_DY = 3232 - 9620;
    /** Top floor (plane 2). */
    static final int[] TAP_CAVE_CRAWLERS = {1673, 9625, 1671, 9619, 1671, 9616, 1669, 9612, 1672, 9611, 1673, 9607, 1677, 9605};
    /** Middle floor (plane 1). */
    static final int[] TAP_MID_FROST_CRABS = {1718, 9610, 1722, 9610, 1715, 9616, 1717, 9618, 1709, 9604, 1708, 9606, 1709, 9611,
            1718, 9605, 1718, 9607, 1723, 9612, 1720, 9611, 1719, 9611, 1718, 9617, 1716, 9606, 1707, 9604, 1710, 9611};
    static final int[] TAP_MID_ICE_WARRIORS = {1720, 9632, 1722, 9633, 1708, 9650, 1704, 9654};
    /** Bottom floor (plane 0). */
    static final int[] TAP_FROST_CRABS = {1704, 9631, 1704, 9626, 1704, 9630, 1703, 9630, 1702, 9626, 1704, 9625, 1700, 9624};
    static final int[] TAP_ICE_WARRIORS = {1681, 9656, 1678, 9653, 1676, 9650};
    static final int[] TAP_ICEFIENDS = {1684, 9640, 1684, 9642, 1682, 9640, 1685, 9644, 1690, 9644, 1688, 9646, 1682, 9644, 1687, 9643,
            1693, 9646, 1701, 9639, 1702, 9640, 1702, 9637, 1704, 9640, 1706, 9634, 1705, 9638};
    static final int[] TAP_CHILLED_JELLIES = {1649, 9653, 1650, 9658, 1650, 9648, 1654, 9654, 1656, 9657, 1660, 9656, 1659, 9652,
            1654, 9649, 1653, 9646, 1659, 9648};
    static final int[] TAP_FROST_NAGUA = {1627, 9623, 1627, 9638, 1631, 9618, 1631, 9643, 1633, 9623, 1634, 9638, 1638, 9630, 1639, 9623,
            1639, 9636, 1647, 9631, 1652, 9631};
    static final int[] TAP_ICE_SPIDERS = {1638, 9655, 1633, 9654, 1632, 9651, 1628, 9650, 1628, 9610, 1633, 9612, 1638, 9609};
    static final int[] TAP_ICE_GIANTS = {1608, 9647, 1615, 9649, 1615, 9656, 1607, 9657, 1610, 9652, 1596, 9656, 1591, 9656, 1594, 9651,
            1589, 9651, 1597, 9643, 1582, 9649, 1584, 9656, 1585, 9643, 1589, 9644};
    static final int[] TAP_BLUE_DRAGONS = {1610, 9608, 1575, 9611, 1576, 9605, 1581, 9614, 1588, 9611, 1589, 9605, 1592, 9617};
    static final int[] TAP_BABY_BLUE_DRAGONS = {1607, 9615, 1606, 9605, 1611, 9613, 1616, 9615, 1616, 9609, 1614, 9604, 1606, 9610};
    static final int[] TAP_BRUTAL_BLUE_DRAGONS = {1646, 9604, 1647, 9613, 1652, 9608, 1656, 9604, 1656, 9613};

    /**
     * The Ruins of Tapoyauik (The Heart of Darkness): the top floor's icy cave crawlers, the middle floor's frost crabs
     * and ice warriors and the great bottom floor - frost crabs, icefiends and ice warriors under the chain, the chilled
     * jellies north-east of the lift, the frost nagua before Amoxliatl's door, ice spiders, ice giants, and blue, baby
     * blue and brutal blue dragons to the south-west - all on their wiki tiles (the Varlamore variants are the regular
     * monsters here). The chain stands for the agility obstacles between the floors; the Tower of Ascension's lift,
     * the moon key chest and Amoxliatl's lair (the bosses module's arena) are not carved. The hut stands 6 tiles north
     * of the entrance tile, clear of the zeah module's Ruins of Tapoyauik marker and the plaza beside it (approx).
     */
    static Dungeons.Dungeon ruinsOfTapoyauik() {
        Cave c = new Cave("dng_ruins_of_tapoyauik", "Ruins of Tapoyauik", mc("packed_ice"), mc("snow_block"), mc("sea_lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, TAP_DX, TAP_DY);
        int top = 40;
        int mid = 26;
        int bottom = 12;
        int[] topFloor = {1664, 9601, 1706, 9628};
        int[] midFloor = {1700, 9600, 1727, 9658};
        int[] east = {1672, 9620, 1710, 9661};
        int[] jellies = {1644, 9640, 1664, 9661};
        int[] nagua = {1622, 9604, 1660, 9639};
        int[] spiders = {1624, 9640, 1642, 9659};
        int[] giants = {1578, 9640, 1620, 9661};
        int[] dragons = {1571, 9600, 1620, 9621};
        s.room(topFloor, top, 6);
        s.room(midFloor, mid, 6);
        int[][] bottomRooms = {east, jellies, nagua, spiders, giants, dragons};
        for (int[] r : bottomRooms) {
            s.room(r, bottom, 7);
        }
        s.hall(1660, 9630, 1672, 9630, bottom);   // the frost nagua's hall - the chain's cavern
        s.hall(1664, 9650, 1672, 9650, bottom);   // jellies - the chain's cavern
        s.hall(1620, 9612, 1622, 9612, bottom);   // dragons - nagua
        s.hall(1620, 9650, 1624, 9650, bottom);   // ice giants - spiders
        s.hallZ(1600, 9621, 1600, 9640, bottom);  // dragons - ice giants
        c.carve();
        c.ladderUp(s.x(1703), s.z(9612), mid, top);      // the chain, top to middle floor (approx)
        c.ladderUp(s.x(1706), s.z(9628), bottom, mid);   // the chain, middle to bottom floor (approx)
        c.fill(s.x(1622), bottom + 1, s.z(9628), s.x(1622) + 1, bottom + 5, s.z(9632) + 1, mc("blue_ice")); // Amoxliatl's door
        for (int[] p : new int[][]{{1586, 9658}, {1580, 9658}}) {
            c.set(s.x(p[0]), bottom + 1, s.z(p[1]), Cave.rc("runite_rock")); // the two runite rocks, north-west corner
        }
        for (int[] p : new int[][]{{1640, 9625}, {1690, 9650}, {1655, 9620}, {1595, 9650}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), bottom, 7, mc("packed_ice"));
        }
        c.pillar(s.x(1715), s.z(9640), mid, 6, mc("packed_ice"));
        c.entrance(s.x(1693), s.z(9626), top, mc("packed_ice"), mc("snow_block"), 'n');
        s.camps(new int[][]{topFloor}, top, RESPAWN, "cave_crawler", TAP_CAVE_CRAWLERS);
        s.camps(new int[][]{midFloor}, mid, RESPAWN, "frost_crab", TAP_MID_FROST_CRABS);
        s.camps(new int[][]{midFloor}, mid, RESPAWN, "ice_warrior", TAP_MID_ICE_WARRIORS);
        s.camps(bottomRooms, bottom, RESPAWN, "frost_crab", TAP_FROST_CRABS);
        s.camps(bottomRooms, bottom, RESPAWN, "ice_warrior", TAP_ICE_WARRIORS);
        s.camps(bottomRooms, bottom, RESPAWN, "icefiend", TAP_ICEFIENDS);
        s.camps(bottomRooms, bottom, RESPAWN, "chilled_jelly", TAP_CHILLED_JELLIES);
        s.camps(bottomRooms, bottom, RESPAWN, "frost_nagua", TAP_FROST_NAGUA);
        s.camps(bottomRooms, bottom, RESPAWN, "ice_spider", TAP_ICE_SPIDERS);
        s.camps(bottomRooms, bottom, RESPAWN, "ice_giant", TAP_ICE_GIANTS);
        s.camps(bottomRooms, bottom, DRAGON_RESPAWN, "blue_dragon", TAP_BLUE_DRAGONS);
        s.camps(bottomRooms, bottom, DRAGON_RESPAWN, "baby_blue_dragon", TAP_BABY_BLUE_DRAGONS);
        s.camps(bottomRooms, bottom, DRAGON_RESPAWN, "brutal_blue_dragon", TAP_BRUTAL_BLUE_DRAGONS);
        return c.build();
    }

    // ------------------------------------------------------------------ Tonali Cavern

    /**
     * The Crypt of Tonali's two entrances (1309, 3104 and 1305, 3033). The shift puts the northern one on the cavern's
     * north end (1309, 9454) and, with it, the southern one in the southern cavern (1305, 9383) (approx).
     */
    static final int TON_DX = 0;
    static final int TON_DY = 3104 - 9454;
    /** Bottom floor (plane 0). */
    static final int[] TON_NAGUA_BOTTOM = {1290, 9372, 1291, 9368, 1295, 9370, 1307, 9388, 1304, 9381, 1337, 9382, 1333, 9378, 1327, 9369,
            1322, 9371};
    static final int[] TON_GIANT_ROCK_CRABS = {1314, 9396, 1320, 9392, 1327, 9389, 1324, 9392, 1337, 9393, 1338, 9397, 1335, 9402,
            1330, 9405, 1331, 9413};
    static final int[] TON_MOSS_GIANTS_BOTTOM = {1289, 9399, 1286, 9396, 1284, 9391, 1287, 9384, 1289, 9380};
    static final int[] TON_SKELETONS_BOTTOM = {1308, 9410, 1308, 9413, 1306, 9414, 1316, 9413, 1317, 9410, 1319, 9412, 1316, 9415};
    /** Middle floor (plane 1). */
    static final int[] TON_NAGUA_MIDDLE = {1286, 9420, 1293, 9420, 1289, 9418, 1295, 9416, 1286, 9415, 1291, 9415, 1288, 9411, 1293, 9412};
    static final int[] TON_NAGUA_SUN = {1326, 9450, 1330, 9451, 1333, 9450, 1335, 9445, 1333, 9440, 1330, 9441, 1326, 9443, 1324, 9446};
    static final int[] TON_LIZARDS_MOON = {1296, 9454, 1293, 9454, 1288, 9453, 1286, 9448, 1288, 9445, 1285, 9444, 1286, 9441, 1289, 9437,
            1293, 9436, 1297, 9436};
    static final int[] TON_LIZARDS_SOUTH = {1292, 9391, 1295, 9391, 1294, 9387, 1294, 9384, 1297, 9386};
    static final int[] TON_MOSS_GIANTS_MIDDLE = {1323, 9400, 1323, 9404, 1327, 9403};
    static final int[] TON_ROCKSLUGS = {1321, 9382, 1323, 9382, 1322, 9380, 1324, 9379, 1322, 9377, 1323, 9377};
    /** Central section (plane 2). */
    static final int[] TON_SKELETONS_CENTRAL = {1311, 9401, 1313, 9404, 1311, 9409, 1312, 9413};

    /**
     * The Tonali Cavern below the Crypt of Tonali (The Final Dawn): the Moon and Sun Chambers (grimy lizards, earthen
     * nagua), the southern cavern's three floors (earthen nagua, grimy lizards, giant rock crabs, moss giants, rockslugs
     * and skeletons) and the central section, on the wiki's tiles: 25 earthen nagua and 15 grimy lizards (the wiki's
     * monster table, last updated before the chambers were mapped, says 17 and 5). The rooms and the ladders between the
     * floors are approximate; the lift, the tonali teleporters, the Agility shortcuts and the way on to the Ruins of
     * Mokhaiotl are not carved. The northern hut stands 7 tiles east of the crypt's entrance, clear of the plazas on
     * that tile (approx).
     */
    static Dungeons.Dungeon tonaliCavern() {
        Cave c = new Cave("dng_tonali_cavern", "Tonali Cavern", mc("mossy_cobblestone"), mc("rooted_dirt"), mc("ochre_froglight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, TON_DX, TON_DY);
        int bottom = 10;
        int middle = 24;
        int central = 38;
        // Bottom floor.
        int[] southCavern = {1282, 9364, 1341, 9400};
        int[] southChamber0 = {1303, 9400, 1341, 9417};
        // Middle floor.
        int[] hub = {1300, 9426, 1320, 9458};
        int[] moon = {1282, 9433, 1299, 9457};
        int[] sun = {1322, 9437, 1338, 9454};
        int[] midSouth = {1283, 9408, 1298, 9423};
        int[] southChamber1 = {1289, 9381, 1300, 9394};
        int[] mossRoom = {1320, 9397, 1330, 9407};
        int[] slugs = {1318, 9374, 1327, 9386};
        // Central section.
        int[] centre = {1307, 9398, 1317, 9421};
        int[][] bottomRooms = {southCavern, southChamber0};
        int[][] middleRooms = {hub, moon, sun, midSouth, southChamber1, mossRoom, slugs};
        for (int[] r : bottomRooms) {
            s.room(r, bottom, 7);
        }
        for (int[] r : middleRooms) {
            s.room(r, middle, 6);
        }
        s.room(centre, central, 6);
        s.hall(1297, 9445, 1302, 9445, middle);    // hub - Moon Chamber
        s.hall(1318, 9445, 1324, 9445, middle);    // hub - Sun Chamber
        s.hallZ(1302, 9428, 1296, 9416, middle);   // hub - southern cavern (middle floor)
        s.hallZ(1292, 9410, 1292, 9392, middle);   // down to the southern chamber
        s.hall(1298, 9386, 1320, 9386, middle);    // southern chamber - rockslugs
        s.hallZ(1324, 9384, 1324, 9399, middle);   // rockslugs - moss giants
        s.hallZ(1312, 9428, 1312, 9418, middle);   // hub - under the central section
        c.carve();
        c.ladderUp(s.x(1312), s.z(9420), middle, central);
        c.ladderUp(s.x(1295), s.z(9388), bottom, middle);
        for (int[] p : new int[][]{{1300, 9378}, {1320, 9408}, {1288, 9450}, {1330, 9447}, {1310, 9440}}) {
            int f = p[1] > 9425 ? middle : bottom;
            c.pillar(s.x(p[0]), s.z(p[1]), f, f == middle ? 6 : 7, mc("mossy_stone_bricks"));
        }
        c.pool(s.x(1312), s.z(9372), s.x(1318), s.z(9378), bottom, mc("water")); // the stepping-stone pool (approx)
        c.entrance(s.x(1316), s.z(9450), middle, mc("mossy_stone_bricks"), mc("rooted_dirt"), 'n');
        c.entrance(s.x(1305), s.z(9383), bottom, mc("mossy_stone_bricks"), mc("rooted_dirt"), 's');
        s.camps(bottomRooms, bottom, RESPAWN, "earthen_nagua", TON_NAGUA_BOTTOM);
        s.camps(bottomRooms, bottom, RESPAWN, "giant_rock_crab", TON_GIANT_ROCK_CRABS);
        s.camps(bottomRooms, bottom, RESPAWN, "moss_giant", TON_MOSS_GIANTS_BOTTOM);
        s.camps(bottomRooms, bottom, RESPAWN, "minecraft:skeleton", TON_SKELETONS_BOTTOM);
        s.camps(middleRooms, middle, RESPAWN, "earthen_nagua", TON_NAGUA_MIDDLE);
        s.camps(middleRooms, middle, RESPAWN, "earthen_nagua", TON_NAGUA_SUN);
        s.camps(middleRooms, middle, RESPAWN, "grimy_lizard", TON_LIZARDS_MOON);
        s.camps(middleRooms, middle, RESPAWN, "grimy_lizard", TON_LIZARDS_SOUTH);
        s.camps(middleRooms, middle, RESPAWN, "moss_giant", TON_MOSS_GIANTS_MIDDLE);
        s.camps(middleRooms, middle, RESPAWN, "rockslug", TON_ROCKSLUGS);
        s.camps(new int[][]{centre}, central, RESPAWN, "minecraft:skeleton", TON_SKELETONS_CENTRAL);
        return c.build();
    }

    // ------------------------------------------------------------------ Stalker Den

    /** The northern entrance (1296, 3375) leads to (1296, 9767). */
    static final int SD_DX = 0;
    static final int SD_DY = 3375 - 9767;
    static final int[] SD_JUVENILES = {1324, 9735, 1323, 9733, 1321, 9735, 1323, 9737, // single-way
            1257, 9738, 1257, 9742, 1253, 9741, 1252, 9743, 1249, 9741, 1252, 9738};  // multicombat
    /** The single-way cave's one pin stands for 5 spawns. */
    static final int[] SD_MATURE_SINGLE = repeat(1333, 9753, 5);
    static final int[] SD_MATURES = {1255, 9780, 1258, 9779, 1261, 9778, 1257, 9776, 1254, 9775, 1258, 9774, 1251, 9764, 1248, 9763,
            1255, 9761, 1248, 9760, 1251, 9759, 1256, 9758, 1253, 9756};
    static final int[] SD_ELDERS = {1296, 9818, 1297, 9821, 1299, 9817, 1301, 9824, 1303, 9818, 1304, 9822, 1322, 9806, 1323, 9808,
            1324, 9804, 1328, 9803, 1328, 9809, 1329, 9806, 1331, 9805, 1332, 9810, // single-way
            1263, 9767, 1265, 9770, 1266, 9764, 1267, 9773, 1268, 9767, 1270, 9770};  // multicombat
    static final int[] SD_ZYGOMITES = {1318, 9775, 1321, 9780, 1313, 9778, 1298, 9782, 1291, 9785, 1289, 9791, 1300, 9792, 1311, 9790,
            1315, 9795};

    /**
     * The Stalker Den below the Custodia Pass (Shadows of Custodia): the spring in the middle with its 9 ancient
     * zygomites, the south-western caverns (multicombat in OSRS; single-way here) with 6 juvenile, 13 mature and 6
     * elder custodian stalkers, the two single-way caves in the south-east (4 juveniles, 5 matures) and the two in the
     * north (14 elders), all on the wiki's tiles. Only the northern entrance is built: the southern squeeze-through
     * (59 Agility) and the stepping stone are not carved.
     */
    static Dungeons.Dungeon stalkerDen() {
        Cave c = new Cave("dng_stalker_den", "Stalker Den", mc("tuff"), mc("moss_block"), mc("shroomlight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, SD_DX, SD_DY);
        int f = 18;
        int[] spring = {1284, 9762, 1326, 9799};
        int[] southWest = {1244, 9733, 1275, 9785};
        int[] juvenileCave = {1317, 9728, 1328, 9741};
        int[] matureCave = {1328, 9747, 1339, 9759};
        int[] elderNorthWest = {1292, 9813, 1308, 9828};
        int[] elderNorthEast = {1318, 9800, 1336, 9814};
        int[][] rooms = {spring, southWest, juvenileCave, matureCave, elderNorthWest, elderNorthEast};
        for (int[] r : rooms) {
            s.room(r, f, 6);
        }
        s.hall(1284, 9770, 1275, 9770, f);        // spring - south-western caverns
        s.hallZ(1322, 9762, 1322, 9741, f);       // spring - juvenile cave
        s.hall(1326, 9766, 1333, 9759, f);        // spring - mature cave
        s.hallZ(1300, 9799, 1300, 9813, f);       // spring - north-western elders
        s.hallZ(1322, 9799, 1322, 9800, f);       // spring - north-eastern elders
        c.carve();
        c.pool(s.x(1300), s.z(9776), s.x(1312), s.z(9788), f, mc("water")); // the spring (fishing spots, stepping stone)
        for (int[] p : new int[][]{{1260, 9750}, {1250, 9770}, {1300, 9822}, {1330, 9808}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 6, mc("tuff"));
        }
        c.entrance(s.x(1296), s.z(9767), f, mc("tuff"), mc("moss_block"), 'n');
        s.camps(rooms, f, RESPAWN, "juvenile_custodian_stalker", SD_JUVENILES);
        s.camps(rooms, f, RESPAWN, "mature_custodian_stalker", SD_MATURE_SINGLE);
        s.camps(rooms, f, RESPAWN, "mature_custodian_stalker", SD_MATURES);
        s.camps(rooms, f, RESPAWN, "elder_custodian_stalker", SD_ELDERS);
        s.camps(rooms, f, RESPAWN, "ancient_zygomite", SD_ZYGOMITES);
        return c.build();
    }
}
