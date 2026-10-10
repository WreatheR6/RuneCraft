package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-built Great Kourend and Varlamore at their real tile coordinates: Kourend Castle (with the
 * Catacombs of Kourend carved beneath it), Kingstown, the town centres of Arceuus, Hosidius,
 * Port Piscarilius, Shayzien and Lovakengj, the Woodcutting Guild, and Civitas illa Fortis with
 * its walls, market, Tower of Ascension and the Fortis Colosseum.
 *
 * <p>Other modules own some places inside these towns (the Catacombs entrance and Skotizo's arena
 * in the castle courtyard, the quest squares in Hosidius and Shayzien, the Colosseum and Sol
 * Heredit's arena). Those spots are left untouched - no flattening (including its blend ring) and
 * no buildings - so their own plazas and boss arenas are never overwritten.
 */
public final class ZeahStructures {
    private ZeahStructures() {
    }

    /** Absolute block height of the Catacombs of Kourend floor (the first air block is one above). */
    public static final int CATACOMBS_FLOOR_Y = 38;

    // Palette.
    static final String STONE = "minecraft:stone_bricks";
    static final String ANDESITE = "minecraft:polished_andesite";
    static final String COBBLE = "minecraft:cobblestone";
    static final String GRASS = "minecraft:grass_block";
    static final String PATH = "minecraft:dirt_path";
    static final String SAND_STONE = "minecraft:smooth_sandstone";
    static final String CUT_SANDSTONE = "minecraft:cut_sandstone";
    static final String RED_SANDSTONE = "minecraft:cut_red_sandstone";

    static String rc(String id) {
        return "runecraft:" + id;
    }

    public static List<StructureDef> all() {
        List<StructureDef> out = new ArrayList<>();
        out.add(kourendCastle());
        out.add(kingstown());
        out.add(arceuus());
        out.add(hosidius());
        out.add(portPiscarilius());
        out.add(shayzien());
        out.add(lovakengj());
        out.add(woodcuttingGuild());
        out.add(civitas());
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static void lamp(Builder b, int tx, int ty, String post) {
        b.tSet(tx, ty, 0, post);
        b.tSet(tx, ty, 1, post);
        b.tSet(tx, ty, 2, "minecraft:lantern");
    }

    private static void crenellate(Builder b, int tx1, int ty1, int tx2, int ty2, int y, String block) {
        if (tx1 == tx2) {
            for (int t = Math.min(ty1, ty2); t <= Math.max(ty1, ty2); t += 2) {
                b.tSet(tx1, t, y, block);
            }
        } else {
            for (int t = Math.min(tx1, tx2); t <= Math.max(tx1, tx2); t += 2) {
                b.tSet(t, ty1, y, block);
            }
        }
    }

    /** A wall line with crenellations along the top. */
    private static void wall(Builder b, int tx1, int ty1, int tx2, int ty2, int height, String block) {
        b.tWallLine(tx1, ty1, tx2, ty2, height, block);
        crenellate(b, tx1, ty1, tx2, ty2, height, block);
    }

    /** A simple tree: trunk of a choppable RuneCraft log block and a leafy crown. */
    private static void tree(Builder b, int tx, int ty, String log, String leaves, int height) {
        b.tSet(tx, ty, -1, "minecraft:dirt");
        b.tFill(tx - 1, ty - 1, tx + 1, ty + 1, height - 2, height, leaves);
        for (int y = 0; y < height; y++) {
            b.tSet(tx, ty, y, log);
        }
    }

    // ------------------------------------------------------------------ Kourend Castle

    /**
     * The castle: curtain walls with corner towers, the keep at the north of the courtyard (chapel
     * altar and kitchen range), barracks and chapel wings, and the Catacombs of Kourend beneath, reached
     * by a stairwell in the courtyard. The courtyard around the Catacombs statue (and Skotizo's arena)
     * is left to the bosses module.
     */
    static StructureDef kourendCastle() {
        Builder b = StructureDef.builder("zeah_kourend_castle", "Kourend Castle").replaces(ZeahPlaces.KOUREND_CASTLE);
        // Ground: everything except the courtyard centre.
        b.tFlatten(1604, 3685, 1674, 3716, GRASS);
        b.tFlatten(1604, 3646, 1674, 3654, GRASS);
        b.tFlatten(1604, 3655, 1620, 3684, GRASS);
        b.tFlatten(1666, 3655, 1674, 3684, GRASS);
        // Curtain walls with a south gatehouse and corner towers.
        String w = STONE;
        wall(b, 1606, 3650, 1636, 3650, 7, w);
        wall(b, 1642, 3650, 1672, 3650, 7, w);
        wall(b, 1606, 3714, 1672, 3714, 7, w);
        wall(b, 1606, 3650, 1606, 3714, 7, w);
        wall(b, 1672, 3650, 1672, 3714, 7, w);
        b.tTower(1606, 3650, 4, 11, w, ANDESITE);
        b.tTower(1672, 3650, 4, 11, w, ANDESITE);
        b.tTower(1606, 3714, 4, 11, w, ANDESITE);
        b.tTower(1672, 3714, 4, 11, w, ANDESITE);
        b.tTower(1634, 3650, 3, 10, w, ANDESITE);
        b.tTower(1644, 3650, 3, 10, w, ANDESITE);
        b.tFloor(1637, 3646, 1641, 3654, COBBLE);
        // Purple Kourend banners over the gate.
        b.tFill(1637, 3650, 1641, 3650, 7, 7, "minecraft:purple_wool");
        // The keep.
        b.tBuilding(1622, 3690, 1656, 3708, 3, w, ANDESITE, "minecraft:deepslate_tile", "south", true);
        b.tSet(1630, 3700, 0, rc("prayer_altar"));
        b.tSet(1650, 3700, 0, rc("cooking_range"));
        b.tFill(1637, 3704, 1641, 3706, -1, -1, "minecraft:purple_wool");
        b.tSet(1639, 3706, 0, "minecraft:gold_block");
        b.tTower(1622, 3708, 3, 16, w, ANDESITE);
        b.tTower(1656, 3708, 3, 16, w, ANDESITE);
        // Barracks (west) and chapel (east) wings.
        b.tBuilding(1609, 3656, 1619, 3684, 2, w, "minecraft:spruce_planks", "minecraft:deepslate_tile", "east", true);
        b.tBuilding(1667, 3656, 1670, 3684, 2, w, "minecraft:spruce_planks", "minecraft:deepslate_tile", "west", true);
        lamp(b, 1624, 3688, "minecraft:stone_brick_wall");
        lamp(b, 1654, 3688, "minecraft:stone_brick_wall");
        catacombs(b);
        return b.build();
    }

    /** The Catacombs of Kourend: a pillared crypt under the castle and a stairwell down from the courtyard. */
    private static void catacombs(Builder b) {
        int x1 = StructureDef.bx(1604);
        int x2 = StructureDef.bx(1674) + 1;
        int z1 = StructureDef.bz(3716);
        int z2 = StructureDef.bz(3646) + 1;
        int f = CATACOMBS_FLOOR_Y;
        b.fill(x1, f, z1, x2, f + 7, z2, "minecraft:deepslate_bricks");
        b.clear(x1 + 1, f + 1, z1 + 1, x2 - 1, f + 6, z2 - 1);
        b.fill(x1 + 1, f, z1 + 1, x2 - 1, f, z2 - 1, "minecraft:polished_deepslate");
        for (int x = x1 + 8; x < x2 - 4; x += 16) {
            for (int z = z1 + 8; z < z2 - 4; z += 16) {
                b.fill(x, f + 1, z, x + 1, f + 6, z + 1, "minecraft:chiseled_deepslate");
                b.set(x - 1, f + 1, z, "minecraft:soul_lantern");
                b.set(x + 2, f + 1, z + 1, "minecraft:soul_lantern");
            }
        }
        // Stairwell: from the courtyard (tile 1628, 3686) down towards the north into the crypt.
        int sx = StructureDef.bx(1628);
        int sz = StructureDef.bz(3686) + 1;
        int top = b.ground() - 1;
        for (int i = 0; top - 1 - i > f; i++) {
            int y = top - 1 - i;
            int z = sz - i;
            b.clear(sx - 1, y + 1, z, sx + 2, y + 4, z);
            b.walls(sx - 1, y - 1, z, sx + 2, y + 4, z, "minecraft:deepslate_bricks");
            b.clear(sx, y + 1, z, sx + 1, y + 4, z);
            b.fill(sx, y, z, sx + 1, y, z, "minecraft:deepslate_brick_stairs[facing=south]");
        }
        b.tFill(1627, 3685, 1630, 3687, 0, 0, "minecraft:air");
        b.tSet(1627, 3686, 0, "minecraft:deepslate_brick_wall");
        b.tSet(1630, 3686, 0, "minecraft:deepslate_brick_wall");
    }

    // ------------------------------------------------------------------ Kingstown

    static StructureDef kingstown() {
        Builder b = StructureDef.builder("zeah_kingstown", "Kingstown").replaces(ZeahPlaces.KINGSTOWN);
        b.tFlatten(1670, 3608, 1708, 3644, GRASS);
        b.tFloor(1682, 3622, 1694, 3630, COBBLE);
        b.tFloor(1686, 3631, 1690, 3644, PATH);
        // The Kourend Kitchen: bank and the bonus range.
        b.tBuilding(1672, 3612, 1684, 3622, 1, "minecraft:bricks", "minecraft:spruce_planks", "minecraft:spruce", "east", true);
        b.tSet(1676, 3617, 0, rc("bank_booth"));
        b.tSet(1680, 3614, 0, rc("cooking_range"));
        // Kingstown general store and tavern.
        b.tBuilding(1694, 3628, 1702, 3636, 1, "minecraft:spruce_planks", "minecraft:oak_planks", "minecraft:dark_oak", "west", true);
        b.tSet(1698, 3632, 0, rc("general_store"));
        b.tBuilding(1690, 3610, 1702, 3618, 2, "minecraft:stripped_spruce_log", "minecraft:spruce_planks", "minecraft:spruce", "north",
                true);
        // Statue of King Rada I.
        b.tFill(1688, 3626, 1688, 3626, 0, 1, STONE);
        b.tSet(1688, 3626, 2, "minecraft:gold_block");
        lamp(b, 1683, 3623, "minecraft:spruce_fence");
        lamp(b, 1693, 3629, "minecraft:spruce_fence");
        return b.build();
    }

    // ------------------------------------------------------------------ Arceuus

    static StructureDef arceuus() {
        Builder b = StructureDef.builder("zeah_arceuus", "Arceuus").replaces(ZeahPlaces.ARCEUUS);
        String wall = "minecraft:polished_blackstone_bricks";
        String roof = "minecraft:polished_blackstone_brick";
        b.tFlatten(1610, 3732, 1666, 3774, GRASS);
        b.tFloor(1630, 3746, 1642, 3758, wall);
        b.tFloor(1634, 3750, 1638, 3754, "minecraft:purple_terracotta");
        b.tFloor(1634, 3734, 1638, 3745, PATH);
        // Bank.
        b.tBuilding(1620, 3740, 1630, 3748, 1, wall, "minecraft:dark_oak_planks", roof, "east", true);
        b.tSet(1624, 3744, 0, rc("bank_booth"));
        b.tSet(1626, 3744, 0, rc("bank_booth"));
        // Rune shop and general store.
        b.tBuilding(1640, 3760, 1648, 3768, 1, wall, "minecraft:dark_oak_planks", roof, "south", true);
        b.tSet(1644, 3764, 0, rc("rune_shop"));
        b.tBuilding(1652, 3740, 1660, 3748, 1, wall, "minecraft:dark_oak_planks", roof, "west", true);
        b.tSet(1656, 3744, 0, rc("general_store"));
        // Arceuus spire.
        b.tTower(1620, 3764, 4, 18, wall, "minecraft:purpur_block");
        b.tFill(1619, 3763, 1621, 3765, 18, 19, "minecraft:amethyst_block");
        // Minecart station.
        b.tSet(1636, 3738, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1630, 3738, 1642, 3738, 0, 0, "minecraft:rail");
        b.tSet(1636, 3738, 0, rc(ZeahBlocks.MINECART));
        lamp(b, 1631, 3747, "minecraft:polished_blackstone_wall");
        lamp(b, 1641, 3757, "minecraft:polished_blackstone_wall");
        return b.build();
    }

    // ------------------------------------------------------------------ Hosidius

    /**
     * Hosidius: bank, general store, spinning wheel, kitchen, the allotments to the south and Lord
     * Kandur's manor to the north-east. The quests module's Hosidius square (east of the bank) and
     * Sarachnis' arena by Forthos Dungeon (west) are left untouched.
     */
    static StructureDef hosidius() {
        Builder b = StructureDef.builder("zeah_hosidius", "Hosidius").replaces(ZeahPlaces.HOSIDIUS);
        String wall = "minecraft:stripped_birch_log";
        String floor = "minecraft:oak_planks";
        b.tFlatten(1731, 3570, 1752, 3626, GRASS);
        b.tFlatten(1753, 3611, 1786, 3626, GRASS);
        b.tFloor(1742, 3588, 1746, 3622, PATH);
        // Bank (the real Hosidius bank is at 1748, 3599).
        b.tBuilding(1744, 3595, 1752, 3603, 1, wall, floor, "minecraft:spruce", "west", true);
        b.tSet(1748, 3599, 0, rc("bank_booth"));
        b.tSet(1748, 3601, 0, rc("bank_booth"));
        // General store and spinning wheel.
        b.tBuilding(1732, 3606, 1740, 3614, 1, wall, floor, "minecraft:spruce", "east", true);
        b.tSet(1736, 3610, 0, rc("general_store"));
        b.tBuilding(1732, 3590, 1740, 3598, 1, wall, floor, "minecraft:spruce", "east", true);
        b.tSet(1736, 3594, 0, rc("spinning_wheel"));
        // Market stall.
        b.tSet(1745, 3586, 0, rc("vegetable_stall"));
        // Allotments and herb patch, fenced.
        b.tWalls(1732, 3572, 1750, 3582, 0, 0, "minecraft:oak_fence");
        b.tClear(1741, 3582, 1742, 3582, 0, 0);
        b.tFloor(1734, 3574, 1748, 3580, "minecraft:farmland");
        b.tSet(1736, 3577, 0, rc("allotment_patch"));
        b.tSet(1740, 3577, 0, rc("allotment_patch"));
        b.tSet(1746, 3577, 0, rc("herb_patch"));
        // Kitchen (range) and Lord Kandur's manor to the north-east.
        b.tBuilding(1758, 3612, 1766, 3620, 1, wall, floor, "minecraft:spruce", "south", true);
        b.tSet(1762, 3616, 0, rc("cooking_range"));
        b.tBuilding(1772, 3612, 1784, 3624, 2, "minecraft:mossy_stone_bricks", floor, "minecraft:dark_oak", "south", true);
        b.tFill(1776, 3612, 1780, 3612, 7, 7, "minecraft:green_wool");
        // Minecart station.
        b.tSet(1768, 3622, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1755, 3624, 1770, 3624, 0, 0, "minecraft:rail");
        lamp(b, 1743, 3605, "minecraft:oak_fence");
        lamp(b, 1743, 3590, "minecraft:oak_fence");
        return b.build();
    }

    // ------------------------------------------------------------------ Port Piscarilius

    static StructureDef portPiscarilius() {
        Builder b = StructureDef.builder("zeah_port_piscarilius", "Port Piscarilius").replaces(ZeahPlaces.PORT_PISCARILIUS);
        String wall = "minecraft:spruce_planks";
        String floor = "minecraft:spruce_planks";
        String roof = "minecraft:prismarine_brick";
        b.tFlatten(1786, 3772, 1832, 3808, "minecraft:gravel");
        b.tFloor(1796, 3776, 1824, 3784, COBBLE);
        // Bank (real: 1803, 3790).
        b.tBuilding(1798, 3786, 1808, 3794, 1, "minecraft:stripped_spruce_log", floor, roof, "south", true);
        b.tSet(1802, 3790, 0, rc("bank_booth"));
        b.tSet(1804, 3790, 0, rc("bank_booth"));
        // Tynan's Fishing Supplies, general store and a cook's house.
        b.tBuilding(1812, 3786, 1820, 3794, 1, wall, floor, roof, "south", true);
        b.tSet(1816, 3790, 0, rc("tool_shop"));
        b.tBuilding(1788, 3796, 1796, 3804, 1, wall, floor, roof, "east", true);
        b.tSet(1792, 3800, 0, rc("general_store"));
        b.tBuilding(1812, 3796, 1820, 3804, 1, wall, floor, roof, "west", true);
        b.tSet(1816, 3800, 0, rc("cooking_range"));
        b.tSet(1806, 3780, 0, rc("fish_stall"));
        // Minecart station.
        b.tSet(1792, 3778, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1788, 3776, 1796, 3776, 0, 0, "minecraft:rail");
        // Piers.
        b.tFill(1826, 3776, 1831, 3800, -1, -1, "minecraft:spruce_planks");
        b.tWalls(1826, 3776, 1831, 3800, 0, 0, "minecraft:spruce_fence");
        b.tClear(1826, 3780, 1826, 3796, 0, 0);
        lamp(b, 1797, 3777, "minecraft:spruce_fence");
        lamp(b, 1823, 3783, "minecraft:spruce_fence");
        return b.build();
    }

    // ------------------------------------------------------------------ Shayzien

    static StructureDef shayzien() {
        Builder b = StructureDef.builder("zeah_shayzien", "Shayzien").replaces(ZeahPlaces.SHAYZIEN);
        String wall = "minecraft:red_nether_bricks";
        String roof = "minecraft:red_nether_brick";
        String floor = "minecraft:polished_andesite";
        b.tFlatten(1508, 3610, 1546, 3644, GRASS);
        b.tFloor(1520, 3612, 1524, 3644, PATH);
        // Bank.
        b.tBuilding(1510, 3612, 1518, 3620, 1, wall, floor, roof, "east", true);
        b.tSet(1514, 3616, 0, rc("bank_booth"));
        b.tSet(1514, 3618, 0, rc("bank_booth"));
        // Combat ring.
        b.tFloor(1528, 3612, 1542, 3626, "minecraft:coarse_dirt");
        b.tWalls(1528, 3612, 1542, 3626, 0, 0, "minecraft:dark_oak_fence");
        b.tClear(1528, 3618, 1528, 3620, 0, 0);
        // Armoury, general store and chapel.
        b.tBuilding(1510, 3628, 1518, 3636, 1, wall, floor, roof, "east", true);
        b.tSet(1514, 3632, 0, rc("weapon_shop"));
        b.tBuilding(1526, 3632, 1532, 3640, 1, "minecraft:bricks", floor, "minecraft:brick", "west", true);
        b.tSet(1529, 3636, 0, rc("general_store"));
        b.tBuilding(1535, 3631, 1545, 3642, 2, wall, floor, roof, "west", true);
        b.tSet(1540, 3638, 0, rc("prayer_altar"));
        // Minecart station.
        b.tSet(1522, 3610, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1516, 3610, 1527, 3610, 0, 0, "minecraft:rail");
        b.tSet(1522, 3610, 0, rc(ZeahBlocks.MINECART));
        lamp(b, 1519, 3624, "minecraft:red_nether_brick_wall");
        lamp(b, 1525, 3628, "minecraft:red_nether_brick_wall");
        return b.build();
    }

    // ------------------------------------------------------------------ Lovakengj

    static StructureDef lovakengj() {
        Builder b = StructureDef.builder("zeah_lovakengj", "Lovakengj").replaces(ZeahPlaces.LOVAKENGJ);
        String wall = "minecraft:deepslate_bricks";
        String roof = "minecraft:deepslate_tile";
        String floor = "minecraft:polished_deepslate";
        b.tFlatten(1510, 3736, 1558, 3778, "minecraft:coarse_dirt");
        b.tFloor(1512, 3740, 1556, 3744, "minecraft:gravel");
        // Bank.
        b.tBuilding(1520, 3750, 1530, 3758, 1, wall, floor, roof, "east", true);
        b.tSet(1524, 3754, 0, rc("bank_booth"));
        b.tSet(1526, 3754, 0, rc("bank_booth"));
        // Forge (open-sided smithy).
        b.tFill(1536, 3746, 1548, 3754, -1, -1, floor);
        for (int[] p : new int[][]{{1536, 3746}, {1548, 3746}, {1536, 3754}, {1548, 3754}}) {
            b.tFill(p[0], p[1], p[0], p[1], 0, 4, wall);
        }
        b.tFill(1536, 3746, 1548, 3754, 5, 5, roof + "s");
        b.tSet(1540, 3750, 0, rc("furnace"));
        b.tSet(1544, 3750, 0, rc("anvil"));
        // Tool shop.
        b.tBuilding(1536, 3760, 1544, 3768, 1, wall, floor, roof, "west", true);
        b.tSet(1540, 3764, 0, rc("tool_shop"));
        // Minecart station and rails (the network was built by House Lovakengj).
        b.tFill(1512, 3742, 1556, 3742, 0, 0, "minecraft:rail");
        b.tSet(1518, 3742, 0, rc(ZeahBlocks.MINECART));
        // Lovakengj tower.
        b.tTower(1552, 3772, 4, 16, wall, floor);
        b.tFill(1551, 3771, 1553, 3773, 16, 16, "minecraft:copper_block");
        lamp(b, 1533, 3745, "minecraft:deepslate_brick_wall");
        lamp(b, 1533, 3758, "minecraft:deepslate_brick_wall");
        return b.build();
    }

    // ------------------------------------------------------------------ Woodcutting Guild

    static StructureDef woodcuttingGuild() {
        Builder b = StructureDef.builder("zeah_woodcutting_guild", "Woodcutting Guild").replaces(ZeahPlaces.WOODCUTTING_GUILD);
        b.tFlatten(1584, 3476, 1660, 3516, GRASS);
        b.tWalls(1586, 3478, 1658, 3514, 0, 0, "minecraft:oak_fence");
        b.tClear(1620, 3478, 1622, 3478, 0, 0);
        b.tFloor(1620, 3478, 1622, 3500, PATH);
        // Bank chest hut (real bank at 1591, 3479) and Perry's Chop-chop Shop.
        b.tBuilding(1588, 3480, 1596, 3488, 1, "minecraft:stripped_oak_log", "minecraft:oak_planks", "minecraft:oak", "east", true);
        b.tSet(1592, 3484, 0, rc("bank_booth"));
        b.tBuilding(1602, 3480, 1610, 3488, 1, "minecraft:stripped_oak_log", "minecraft:oak_planks", "minecraft:oak", "east", true);
        b.tSet(1606, 3484, 0, rc("tool_shop"));
        // Trees: maples and yews near the entrance, magic trees, and the redwoods (the only ones in Gielinor).
        String leaves = "minecraft:oak_leaves[persistent=true]";
        tree(b, 1600, 3498, rc("maple_tree_log"), "minecraft:birch_leaves[persistent=true]", 6);
        tree(b, 1606, 3504, rc("maple_tree_log"), "minecraft:birch_leaves[persistent=true]", 6);
        tree(b, 1596, 3508, rc("yew_tree_log"), "minecraft:dark_oak_leaves[persistent=true]", 7);
        tree(b, 1612, 3510, rc("yew_tree_log"), "minecraft:dark_oak_leaves[persistent=true]", 7);
        tree(b, 1628, 3508, rc("magic_tree_log"), "minecraft:azalea_leaves[persistent=true]", 7);
        tree(b, 1634, 3502, rc("magic_tree_log"), "minecraft:azalea_leaves[persistent=true]", 7);
        for (int[] r : new int[][]{{1644, 3490}, {1652, 3490}, {1644, 3506}, {1652, 3506}}) {
            tree(b, r[0], r[1], rc("redwood_tree_log"), "minecraft:spruce_leaves[persistent=true]", 12);
        }
        tree(b, 1616, 3492, rc("oak_tree_log"), leaves, 5);
        lamp(b, 1619, 3490, "minecraft:oak_fence");
        return b.build();
    }

    // ------------------------------------------------------------------ Civitas illa Fortis

    /**
     * Civitas illa Fortis: sandstone city walls with gatehouses, the central plaza with a statue of
     * Ralos, the bank, the market, the smithy, an inn, the Tower of Ascension, the Temple of Ralos,
     * residential streets and the Fortis Colosseum in the south-east. The Colosseum floor (the bosses
     * module's arena and Sol Heredit's ring) is left untouched.
     */
    static StructureDef civitas() {
        Builder b = StructureDef.builder("zeah_civitas_illa_fortis", "Civitas illa Fortis").replaces(ZeahPlaces.CIVITAS);
        String roof = "minecraft:mud_brick";
        // Ground, avoiding the Colosseum floor.
        b.tFlatten(1634, 3072, 1789, 3180, GRASS);
        b.tFlatten(1834, 3072, 1844, 3180, GRASS);
        b.tFlatten(1790, 3072, 1833, 3086, GRASS);
        b.tFlatten(1790, 3119, 1833, 3180, GRASS);
        // City walls, gates and towers.
        int h = 8;
        wall(b, 1636, 3074, 1697, 3074, h, SAND_STONE);
        wall(b, 1703, 3074, 1842, 3074, h, SAND_STONE);
        wall(b, 1636, 3178, 1677, 3178, h, SAND_STONE);
        wall(b, 1683, 3178, 1842, 3178, h, SAND_STONE);
        wall(b, 1636, 3074, 1636, 3127, h, SAND_STONE);
        wall(b, 1636, 3133, 1636, 3178, h, SAND_STONE);
        wall(b, 1842, 3074, 1842, 3178, h, SAND_STONE);
        for (int[] t : new int[][]{{1636, 3074}, {1842, 3074}, {1636, 3178}, {1842, 3178}, {1696, 3074}, {1704, 3074},
                {1676, 3178}, {1684, 3178}, {1636, 3126}, {1636, 3134}}) {
            b.tTower(t[0], t[1], 3, 12, CUT_SANDSTONE, SAND_STONE);
        }
        // Streets: the main avenue from the south gate to the Tower of Ascension, and the east-west road.
        b.tFloor(1698, 3075, 1702, 3177, SAND_STONE);
        b.tFloor(1637, 3128, 1788, 3132, SAND_STONE);
        // Central plaza with the statue of Ralos.
        b.tFloor(1668, 3118, 1692, 3142, SAND_STONE);
        b.tFloor(1678, 3118, 1682, 3142, RED_SANDSTONE);
        b.tFloor(1668, 3128, 1692, 3132, RED_SANDSTONE);
        b.tFill(1679, 3129, 1681, 3131, 0, 0, CUT_SANDSTONE);
        b.tFill(1680, 3130, 1680, 3130, 1, 3, CUT_SANDSTONE);
        b.tSet(1680, 3130, 4, "minecraft:gold_block");
        // Bank.
        b.tBuilding(1646, 3122, 1658, 3134, 1, CUT_SANDSTONE, SAND_STONE, roof, "east", true);
        b.tSet(1650, 3127, 0, rc("bank_booth"));
        b.tSet(1652, 3127, 0, rc("bank_booth"));
        b.tSet(1654, 3127, 0, rc("bank_booth"));
        // Market stalls south of the plaza.
        String[] stalls = {"general_store", "weapon_shop", "archery_shop", "rune_shop", "apothecary_table"};
        String[] awnings = {"minecraft:orange_wool", "minecraft:yellow_wool", "minecraft:red_wool", "minecraft:yellow_wool",
                "minecraft:orange_wool"};
        for (int i = 0; i < stalls.length; i++) {
            int x = 1668 + i * 6;
            b.tFill(x - 1, 3107, x + 1, 3109, 3, 3, awnings[i]);
            b.tFill(x - 1, 3107, x - 1, 3107, 0, 2, "minecraft:spruce_fence");
            b.tFill(x + 1, 3107, x + 1, 3107, 0, 2, "minecraft:spruce_fence");
            b.tFill(x - 1, 3109, x - 1, 3109, 0, 2, "minecraft:spruce_fence");
            b.tFill(x + 1, 3109, x + 1, 3109, 0, 2, "minecraft:spruce_fence");
            b.tSet(x, 3108, 0, rc(stalls[i]));
        }
        // Smithy and inn east of the plaza.
        b.tBuilding(1706, 3118, 1718, 3126, 1, CUT_SANDSTONE, SAND_STONE, roof, "west", true);
        b.tSet(1710, 3122, 0, rc("furnace"));
        b.tSet(1714, 3122, 0, rc("anvil"));
        b.tBuilding(1706, 3134, 1718, 3144, 2, CUT_SANDSTONE, "minecraft:spruce_planks", roof, "west", true);
        b.tSet(1710, 3139, 0, rc("cooking_range"));
        b.tSet(1714, 3139, 0, rc("spinning_wheel"));
        // The Tower of Ascension (royal palace) at the head of the avenue.
        b.tBuilding(1668, 3150, 1692, 3168, 3, CUT_SANDSTONE, SAND_STONE, SAND_STONE, "south", false);
        b.tTower(1680, 3170, 5, 34, CUT_SANDSTONE, SAND_STONE);
        b.tFill(1679, 3169, 1681, 3171, 34, 35, "minecraft:gold_block");
        // Temple of Ralos.
        b.tBuilding(1730, 3146, 1750, 3164, 2, SAND_STONE, RED_SANDSTONE, "minecraft:brick", "south", true);
        b.tSet(1740, 3160, 0, rc("prayer_altar"));
        // Residential streets.
        int[][] houses = {{1642, 3082}, {1656, 3082}, {1642, 3098}, {1656, 3098}, {1642, 3142}, {1656, 3142}, {1642, 3158},
                {1656, 3158}, {1712, 3082}, {1726, 3082}, {1740, 3082}, {1754, 3082}, {1768, 3082}, {1712, 3098}, {1726, 3098},
                {1740, 3098}, {1754, 3098}, {1768, 3098}, {1726, 3114}, {1740, 3114}, {1754, 3114}, {1768, 3114},
                {1762, 3138}, {1762, 3154}, {1712, 3154}, {1712, 3166}, {1730, 3170}, {1750, 3170}, {1770, 3170},
                {1800, 3130}, {1814, 3130}, {1800, 3150}, {1814, 3150}, {1800, 3166}, {1814, 3166}, {1828, 3140}, {1828, 3160}};
        for (int i = 0; i < houses.length; i++) {
            int x = houses[i][0];
            int y = houses[i][1];
            String r = i % 3 == 0 ? "minecraft:brick" : roof;
            String door = y < 3130 ? "north" : "south";
            b.tBuilding(x, y, x + 9, y + 7, 1 + i % 2, i % 2 == 0 ? SAND_STONE : CUT_SANDSTONE, "minecraft:spruce_planks", r, door,
                    true);
        }
        for (int x = 1650; x <= 1780; x += 16) {
            lamp(b, x, 3127, "minecraft:sandstone_wall");
            lamp(b, x, 3133, "minecraft:sandstone_wall");
        }
        colosseum(b);
        return b.build();
    }

    /** The Fortis Colosseum: a two-tier sandstone ring around the arena floor, entered from the west. */
    private static void colosseum(Builder b) {
        // Outer wall with arches.
        b.tWallLine(1790, 3085, 1833, 3085, 10, SAND_STONE);
        b.tWallLine(1790, 3118, 1833, 3118, 10, SAND_STONE);
        b.tWallLine(1790, 3085, 1790, 3118, 10, SAND_STONE);
        b.tWallLine(1833, 3085, 1833, 3118, 10, SAND_STONE);
        for (int x = 1793; x <= 1830; x += 4) {
            b.tClear(x, 3085, x, 3085, 1, 3);
            b.tClear(x, 3118, x, 3118, 1, 3);
            b.tSet(x, 3085, 9, "minecraft:gold_block");
            b.tSet(x, 3118, 9, "minecraft:gold_block");
        }
        for (int y = 3088; y <= 3115; y += 4) {
            b.tClear(1833, y, 1833, y, 1, 3);
            b.tSet(1790, y, 9, "minecraft:gold_block");
            b.tSet(1833, y, 9, "minecraft:gold_block");
        }
        // Inner tier (spectator stands) and the west entrance.
        b.tWallLine(1794, 3089, 1829, 3089, 5, CUT_SANDSTONE);
        b.tWallLine(1794, 3114, 1829, 3114, 5, CUT_SANDSTONE);
        b.tWallLine(1794, 3089, 1794, 3114, 5, CUT_SANDSTONE);
        b.tWallLine(1829, 3089, 1829, 3114, 5, CUT_SANDSTONE);
        b.tFill(1791, 3086, 1832, 3088, 0, 1, RED_SANDSTONE);
        b.tFill(1791, 3115, 1832, 3117, 0, 1, RED_SANDSTONE);
        b.tFill(1791, 3089, 1793, 3114, 0, 1, RED_SANDSTONE);
        b.tFill(1830, 3089, 1832, 3114, 0, 1, RED_SANDSTONE);
        b.tClear(1790, 3100, 1794, 3103, 0, 3);
        b.tFloor(1784, 3100, 1794, 3103, SAND_STONE);
    }
}
