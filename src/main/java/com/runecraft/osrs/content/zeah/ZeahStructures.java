package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.content.world.EasternDetail;
import com.runecraft.osrs.content.world.EasternDetail.Room;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.zeah.ZeahDetail.house;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

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
        b.tSet(tx, ty, 2, post);
        b.tSet(tx, ty, 3, "minecraft:lantern");
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
        for (int[] t : new int[][]{{1606, 3650}, {1672, 3650}, {1606, 3714}, {1672, 3714}}) {
            b.tTower(t[0], t[1], 4, 14, w, ANDESITE);
            castleTower(b, t[0], t[1], 4, 14);
        }
        b.tTower(1634, 3650, 3, 13, w, ANDESITE);
        b.tTower(1644, 3650, 3, 13, w, ANDESITE);
        castleTower(b, 1634, 3650, 3, 13);
        castleTower(b, 1644, 3650, 3, 13);
        b.tFloor(1637, 3646, 1641, 3654, COBBLE);
        // The gatehouse: an arch over the gate with a raised portcullis and purple Kourend banners.
        b.tFill(1637, 3650, 1641, 3650, 5, 7, w);
        b.tFill(1637, 3650, 1641, 3650, 4, 4, "minecraft:iron_bars");
        crenellate(b, 1637, 3650, 1641, 3650, 8, w);
        int gg = b.ground();
        for (int x = bx(1637); x <= bx(1641) + 1; x += 3) {
            b.set(x, gg + 6, bz(3650) + 2, "minecraft:purple_wall_banner[facing=south]");
        }
        EasternDetail cd = new EasternDetail(b);
        for (int[] wl : new int[][]{{1606, 3650, 1636, 3650}, {1642, 3650, 1672, 3650}, {1606, 3714, 1672, 3714}, {1606, 3650, 1606, 3714},
                {1672, 3650, 1672, 3714}}) {
            int[] q = ZeahDetail.rect(wl[0], wl[1], wl[2], wl[3]);
            cd.weather(q[0], gg, q[1], q[2], gg + 6, q[3], w, "minecraft:mossy_stone_bricks", 25, wl[0] + wl[1]);
            cd.weather(q[0], gg, q[1], q[2], gg + 6, q[3], w, "minecraft:cracked_stone_bricks", 15, wl[2] + wl[3]);
            b.replace(q[0], gg - 1, q[1], q[2], gg, q[3], w, "minecraft:andesite");
        }
        // The keep.
        int[] keep = house(b, 1622, 3690, 1656, 3708, 3, ZeahDetail.CASTLE, "south", Room.TEMPLE);
        b.tSet(1630, 3700, 0, rc("prayer_altar"));
        b.tSet(1650, 3700, 0, rc("cooking_range"));
        b.tFill(1637, 3704, 1641, 3706, -1, -1, "minecraft:purple_wool");
        b.tSet(1639, 3706, 0, "minecraft:gold_block");
        b.tTower(1622, 3708, 3, 26, w, ANDESITE);
        b.tTower(1656, 3708, 3, 26, w, ANDESITE);
        castleTower(b, 1622, 3708, 3, 26);
        castleTower(b, 1656, 3708, 3, 26);
        // The throne hall: purple runner up the middle, banners, braziers and the royal dais.
        int kg = b.ground();
        int kmx = (keep[0] + keep[2]) / 2;
        b.fill(kmx, kg, keep[1] + 4, kmx + 1, kg, keep[3] - 1, "minecraft:purple_carpet");
        for (int x = keep[0] + 6; x < keep[2] - 4; x += 8) {
            b.set(x, kg + 2, keep[1] + 1, "minecraft:purple_wall_banner[facing=south]");
            b.fill(x, kg, keep[3] - 5, x, kg + 2, keep[3] - 5, "minecraft:polished_andesite");
            b.set(x, kg + 3, keep[3] - 5, "minecraft:lantern");
        }
        for (int x = keep[0] + 4; x < keep[2] - 2; x += 6) {
            b.set(x, kg + 6, keep[1] - 1, "minecraft:purple_wall_banner[facing=north]");
        }
        // Barracks (west) and chapel (east) wings.
        house(b, 1609, 3656, 1619, 3684, 2, ZeahDetail.CASTLE, "east", Room.GUARD);
        house(b, 1667, 3656, 1670, 3684, 2, ZeahDetail.CASTLE, "west", Room.TEMPLE);
        lamp(b, 1624, 3688, "minecraft:stone_brick_wall");
        lamp(b, 1654, 3688, "minecraft:stone_brick_wall");
        catacombs(b);
        return b.build();
    }

    /** A castle tower's dressing: string courses, arrow slits, a slate cone roof and a purple pennant. */
    private static void castleTower(Builder b, int tx, int ty, int r, int h) {
        int g = b.ground();
        int x = bx(tx) + 1;
        int z = bz(ty) + 1;
        for (int y = g + 4; y < g + h - 1; y += 5) {
            for (int[] o : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}}) {
                b.fill(x + o[0], y, z + o[1], x + o[0], y + 1, z + o[1], "minecraft:air");
            }
            b.replace(x - r, y - 1, z - r, x + r, y - 1, z + r, STONE, "minecraft:polished_andesite");
        }
        for (int i = 0; i <= r + 1; i++) {
            double rr = r + 1.4 - i;
            int ri = (int) Math.floor(rr);
            for (int dz = -ri; dz <= ri; dz++) {
                int wd = (int) Math.floor(Math.sqrt(Math.max(0, rr * rr - dz * dz)));
                b.fill(x - wd, g + h + 1 + i, z + dz, x + wd, g + h + 1 + i, z + dz, "minecraft:deepslate_tiles");
            }
        }
        int tip = g + h + r + 3;
        b.fill(x, tip, z, x, tip + 2, z, "minecraft:spruce_fence");
        b.set(x + 1, tip + 2, z, "minecraft:purple_wool");
        b.set(x + 2, tip + 2, z, "minecraft:purple_wool");
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
        // The altar at the heart of the catacombs (Darklight + 3 ancient shards -> Arclight).
        b.set(StructureDef.bx(1639), f + 1, StructureDef.bz(3681), "runecraft:" + ZeahBlocks.CATACOMBS_ALTAR);
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
        house(b, 1672, 3612, 1684, 3622, 1, ZeahDetail.KITCHEN, "east", Room.KITCHEN);
        b.tSet(1676, 3617, 0, rc("bank_booth"));
        b.tSet(1680, 3614, 0, rc("cooking_range"));
        // Kingstown general store and tavern.
        house(b, 1694, 3628, 1702, 3636, 1, ZeahDetail.KINGSTOWN, "west", Room.SHOP);
        b.tSet(1698, 3632, 0, rc("general_store"));
        house(b, 1690, 3610, 1702, 3618, 2, ZeahDetail.KINGSTOWN, "north", Room.PUB);
        // Statue of King Rada I on a stepped plinth, flower beds round the square, benches and a well.
        b.tFill(1687, 3625, 1689, 3627, 0, 0, "minecraft:chiseled_stone_bricks");
        b.tFill(1688, 3626, 1688, 3626, 1, 2, STONE);
        b.tSet(1688, 3626, 3, "minecraft:purple_wool");
        b.tSet(1688, 3626, 4, "minecraft:gold_block");
        EasternDetail kd = new EasternDetail(b);
        kd.flowerBed(bx(1682), bz(3630), bx(1684) + 1, bz(3628) + 1, "minecraft:stone_brick_slab[type=top]", 601);
        kd.flowerBed(bx(1692), bz(3630), bx(1694) + 1, bz(3628) + 1, "minecraft:stone_brick_slab[type=top]", 602);
        kd.bench(bx(1685), bz(3623), true, "north", "minecraft:spruce");
        kd.bench(bx(1690), bz(3623), true, "north", "minecraft:spruce");
        kd.well(bx(1693) + 1, bz(3624), "minecraft:stone_bricks", "minecraft:spruce_fence", "minecraft:spruce");
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
        house(b, 1620, 3740, 1630, 3748, 1, ZeahDetail.ARCEUUS, "east", Room.BANK);
        b.tSet(1624, 3744, 0, rc("bank_booth"));
        b.tSet(1626, 3744, 0, rc("bank_booth"));
        // Rune shop and general store.
        house(b, 1640, 3760, 1648, 3768, 1, ZeahDetail.ARCEUUS, "south", Room.LIBRARY);
        b.tSet(1644, 3764, 0, rc("rune_shop"));
        house(b, 1652, 3740, 1660, 3748, 1, ZeahDetail.ARCEUUS, "west", Room.SHOP);
        b.tSet(1656, 3744, 0, rc("general_store"));
        // Arceuus spire: purpur bands, violet lancets and an amethyst crown above a ring of soul lights.
        b.tTower(1620, 3764, 4, 24, wall, "minecraft:purpur_block");
        int scx = bx(1620) + 1;
        int scz = bz(3764) + 1;
        int g = b.ground();
        for (int y = g + 5; y < g + 23; y += 6) {
            b.replace(scx - 4, y, scz - 4, scx + 4, y, scz + 4, wall, "minecraft:purpur_block");
            for (int[] o : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
                b.fill(scx + o[0], y + 2, scz + o[1], scx + o[0], y + 3, scz + o[1], "minecraft:purple_stained_glass");
            }
        }
        b.tFill(1619, 3763, 1621, 3765, 24, 25, "minecraft:amethyst_block");
        b.tSet(1620, 3764, 26, "minecraft:amethyst_block");
        b.tSet(1620, 3764, 27, "minecraft:end_rod");
        b.tSet(1619, 3765, 26, "minecraft:amethyst_cluster");
        b.clear(scx, g, scz + 4, scx, g + 2, scz + 4);
        // Dark reading gardens round the square: purple flowers, iron railings, braziers of soul fire.
        EasternDetail ad = new EasternDetail(b);
        for (int[] fb : new int[][]{{1628, 3758}, {1643, 3758}, {1628, 3747}, {1643, 3747}}) {
            ad.flowerBed(bx(fb[0]), bz(fb[1]), bx(fb[0]) + 3, bz(fb[1]) + 3, "minecraft:polished_blackstone", 701 + fb[0]);
        }
        b.tFill(1634, 3750, 1634, 3750, 0, 0, "minecraft:polished_blackstone_bricks");
        b.tSet(1634, 3750, 1, "minecraft:soul_campfire");
        b.tFill(1638, 3754, 1638, 3754, 0, 0, "minecraft:polished_blackstone_bricks");
        b.tSet(1638, 3754, 1, "minecraft:soul_campfire");
        // Minecart station.
        b.tSet(1636, 3738, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1630, 3738, 1642, 3738, 0, 0, "minecraft:rail");
        b.tSet(1636, 3738, 0, rc(ZeahBlocks.MINECART));
        lamp(b, 1631, 3747, "minecraft:polished_blackstone_wall");
        lamp(b, 1641, 3757, "minecraft:polished_blackstone_wall");
        // Dark yews and willows of the library grounds, soul-lit lamps along the north path.
        for (int[] t : new int[][]{{1614, 3736}, {1660, 3770}, {1650, 3734}, {1614, 3752}, {1662, 3756}, {1630, 3770}}) {
            ad.tree(bx(t[0]), bz(t[1]), 6, "minecraft:dark_oak_log", "minecraft:dark_oak_leaves", t[0] + t[1]);
        }
        for (int ty = 3736; ty <= 3744; ty += 4) {
            ad.lampPost(bx(1633), bz(ty), null, "minecraft:polished_blackstone_wall", "minecraft:soul_lantern", true);
        }
        ZeahDetail.props(b, new int[][]{{1617, 3758}, {1650, 3752}, {1632, 3764}}, "town", 702);
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
        house(b, 1744, 3595, 1752, 3603, 1, ZeahDetail.HOSIDIUS, "west", Room.BANK);
        b.tSet(1748, 3599, 0, rc("bank_booth"));
        b.tSet(1748, 3601, 0, rc("bank_booth"));
        // General store and spinning wheel.
        house(b, 1732, 3606, 1740, 3614, 1, ZeahDetail.HOSIDIUS, "east", Room.SHOP);
        b.tSet(1736, 3610, 0, rc("general_store"));
        house(b, 1732, 3590, 1740, 3598, 1, ZeahDetail.HOSIDIUS, "east", Room.HOUSE);
        b.tSet(1736, 3594, 0, rc("spinning_wheel"));
        // Market stall.
        ZeahDetail.stall(b, 1745, 3586, rc("vegetable_stall"), "minecraft:green_wool", "minecraft:white_wool");
        // Allotments and herb patch, fenced.
        b.tWalls(1732, 3572, 1750, 3582, 0, 0, "minecraft:oak_fence");
        b.tClear(1741, 3582, 1742, 3582, 0, 0);
        b.tFloor(1734, 3574, 1748, 3580, "minecraft:farmland");
        b.tSet(1736, 3577, 0, rc("allotment_patch"));
        b.tSet(1740, 3577, 0, rc("allotment_patch"));
        b.tSet(1746, 3577, 0, rc("herb_patch"));
        // Kitchen (range) and Lord Kandur's manor to the north-east.
        house(b, 1758, 3612, 1766, 3620, 1, ZeahDetail.HOSIDIUS, "south", Room.KITCHEN);
        b.tSet(1762, 3616, 0, rc("cooking_range"));
        house(b, 1772, 3612, 1784, 3624, 2, ZeahDetail.MANOR, "south", Room.LIBRARY);
        b.tFill(1776, 3612, 1780, 3612, 7, 7, "minecraft:green_wool");
        // Minecart station.
        b.tSet(1768, 3622, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1755, 3624, 1770, 3624, 0, 0, "minecraft:rail");
        lamp(b, 1743, 3605, "minecraft:oak_fence");
        lamp(b, 1743, 3590, "minecraft:oak_fence");
        // Farm life: a scarecrow in the allotments, hay, composters and pumpkins by the fence, fruit trees.
        b.tFill(1743, 3579, 1743, 3579, 0, 1, "minecraft:oak_fence");
        b.tSet(1743, 3579, 2, "minecraft:carved_pumpkin[facing=south]");
        b.set(bx(1743) - 1, b.ground() + 1, bz(3579), "minecraft:oak_fence");
        b.set(bx(1743) + 1, b.ground() + 1, bz(3579), "minecraft:oak_fence");
        ZeahDetail.props(b, new int[][]{{1733, 3584}, {1738, 3584}, {1749, 3584}, {1734, 3603}, {1750, 3606}, {1756, 3622}},
                "farm", 801);
        EasternDetail hd = new EasternDetail(b);
        hd.tree(bx(1748), bz(3610), 5, "minecraft:oak_log", "minecraft:flowering_azalea_leaves", 802);
        hd.tree(bx(1768), bz(3614), 5, "minecraft:oak_log", "minecraft:oak_leaves", 803);
        hd.kerb(bx(1742), bz(3622), bx(1746) + 1, bz(3588) + 1, "minecraft:cobblestone");
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
        house(b, 1798, 3786, 1808, 3794, 1, ZeahDetail.PISCARILIUS, "south", Room.BANK);
        b.tSet(1802, 3790, 0, rc("bank_booth"));
        b.tSet(1804, 3790, 0, rc("bank_booth"));
        // Tynan's Fishing Supplies, general store and a cook's house.
        house(b, 1812, 3786, 1820, 3794, 1, ZeahDetail.PISCARILIUS, "south", Room.STORE);
        b.tSet(1816, 3790, 0, rc("tool_shop"));
        house(b, 1788, 3796, 1796, 3804, 1, ZeahDetail.PISCARILIUS, "east", Room.SHOP);
        b.tSet(1792, 3800, 0, rc("general_store"));
        house(b, 1812, 3796, 1820, 3804, 1, ZeahDetail.PISCARILIUS, "west", Room.KITCHEN);
        b.tSet(1816, 3800, 0, rc("cooking_range"));
        ZeahDetail.stall(b, 1806, 3780, rc("fish_stall"), "minecraft:cyan_wool", "minecraft:white_wool");
        // Minecart station.
        b.tSet(1792, 3778, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1788, 3776, 1796, 3776, 0, 0, "minecraft:rail");
        // Piers on posts with mooring rope, fish crates, nets drying and a rowing boat.
        b.tFill(1826, 3776, 1831, 3800, -1, -1, "minecraft:spruce_planks");
        b.tWalls(1826, 3776, 1831, 3800, 0, 0, "minecraft:spruce_fence");
        b.tClear(1826, 3780, 1826, 3796, 0, 0);
        EasternDetail pd = new EasternDetail(b);
        int[] pr = ZeahDetail.rect(1826, 3776, 1831, 3800);
        for (int z = pr[1]; z <= pr[3]; z += 4) {
            b.fill(pr[2], b.ground() - 8, z, pr[2], b.ground(), z, "minecraft:spruce_log");
            b.set(pr[2], b.ground() + 1, z, "minecraft:spruce_slab");
        }
        for (int z = pr[1] + 3; z < pr[3] - 2; z += 8) {
            b.fill(pr[0] + 3, b.ground(), z, pr[0] + 3, b.ground() + 2, z, "minecraft:spruce_fence");
            b.set(pr[0] + 3, b.ground() + 3, z, "minecraft:lantern");
        }
        pd.crates(pr[0] + 6, pr[1] + 2, 901);
        pd.crates(pr[0] + 7, pr[3] - 4, 902);
        b.set(pr[0] + 8, b.ground(), pr[1] + 8, "minecraft:barrel[facing=up]");
        b.set(pr[0] + 8, b.ground() + 1, pr[1] + 8, "minecraft:sea_pickle");
        ZeahDetail.props(b, new int[][]{{1797, 3782}, {1810, 3782}, {1822, 3781}, {1800, 3806}, {1809, 3805}}, "harbour", 903);
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
        house(b, 1510, 3612, 1518, 3620, 1, ZeahDetail.SHAYZIEN, "east", Room.BANK);
        b.tSet(1514, 3616, 0, rc("bank_booth"));
        b.tSet(1514, 3618, 0, rc("bank_booth"));
        // Combat ring.
        b.tFloor(1528, 3612, 1542, 3626, "minecraft:coarse_dirt");
        b.tWalls(1528, 3612, 1542, 3626, 0, 0, "minecraft:dark_oak_fence");
        b.tClear(1528, 3618, 1528, 3620, 0, 0);
        // Armoury, general store and chapel.
        house(b, 1510, 3628, 1518, 3636, 1, ZeahDetail.SHAYZIEN, "east", Room.GUARD);
        b.tSet(1514, 3632, 0, rc("weapon_shop"));
        house(b, 1526, 3632, 1532, 3640, 1, ZeahDetail.SHAYZIEN, "west", Room.SHOP);
        b.tSet(1529, 3636, 0, rc("general_store"));
        int[] chapel = house(b, 1535, 3631, 1545, 3642, 2, ZeahDetail.SHAYZIEN, "west", Room.TEMPLE);
        b.tSet(1540, 3638, 0, rc("prayer_altar"));
        // Minecart station.
        b.tSet(1522, 3610, 0, rc(ZeahBlocks.MINECART));
        b.tFill(1516, 3610, 1527, 3610, 0, 0, "minecraft:rail");
        b.tSet(1522, 3610, 0, rc(ZeahBlocks.MINECART));
        lamp(b, 1519, 3624, "minecraft:red_nether_brick_wall");
        lamp(b, 1525, 3628, "minecraft:red_nether_brick_wall");
        // Military touches: red banners, training dummies and weapon racks round the combat ring, a
        // flagpole with the Shayzien colours and banners on the chapel.
        int g = b.ground();
        for (int tx = 1530; tx <= 1540; tx += 5) {
            b.tFill(tx, 3624, tx, 3624, 0, 1, "minecraft:spruce_fence");
            b.tSet(tx, 3624, 2, "minecraft:carved_pumpkin[facing=south]");
        }
        b.tSet(1541, 3614, 0, "minecraft:grindstone[face=floor,facing=north]");
        b.tSet(1529, 3614, 0, "minecraft:smithing_table");
        b.tFill(1524, 3626, 1524, 3626, 0, 7, "minecraft:spruce_fence");
        b.set(bx(1524) + 1, g + 7, bz(3626), "minecraft:red_wool");
        b.set(bx(1524) + 2, g + 7, bz(3626), "minecraft:red_wool");
        b.set(bx(1524) + 1, g + 6, bz(3626), "minecraft:white_wool");
        b.set(bx(1524) + 2, g + 6, bz(3626), "minecraft:red_wool");
        for (int z = chapel[1] + 2; z < chapel[3] - 1; z += 6) {
            b.set(chapel[0] - 1, g + 5, z, "minecraft:red_wall_banner[facing=west]");
        }
        ZeahDetail.props(b, new int[][]{{1526, 3615}, {1520, 3640}, {1544, 3628}}, "town", 1001);
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
        house(b, 1520, 3750, 1530, 3758, 1, ZeahDetail.LOVAKENGJ, "east", Room.BANK);
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
        // The forge: a brick hearth and chimney, quench trough, coal heaps and tool racks; copper roof trim.
        int g = b.ground();
        int[] fg = ZeahDetail.rect(1536, 3746, 1548, 3754);
        b.fill(fg[0] + 1, g, fg[1] + 1, fg[0] + 3, g + 1, fg[1] + 2, "minecraft:bricks");
        b.set(fg[0] + 2, g, fg[1] + 2, "minecraft:campfire[lit=true]");
        b.fill(fg[0] + 1, g + 2, fg[1] + 1, fg[0] + 2, g + 9, fg[1] + 2, "minecraft:bricks");
        b.set(fg[0] + 1, g + 10, fg[1] + 1, "minecraft:campfire[lit=true]");
        b.fill(fg[2] - 3, g, fg[3] - 1, fg[2] - 1, g, fg[3] - 1, "minecraft:water_cauldron[level=3]");
        b.set(fg[2] - 1, g, fg[1] + 1, "minecraft:grindstone[face=floor,facing=west]");
        b.set(fg[2] - 2, g, fg[1] + 1, "minecraft:smithing_table");
        b.walls(fg[0], g + 6, fg[1], fg[2], g + 6, fg[3], "minecraft:waxed_cut_copper_slab");
        for (int x = fg[0] + 4; x < fg[2] - 2; x += 4) {
            b.set(x, g + 4, (fg[1] + fg[3]) / 2, "minecraft:lantern[hanging=true]");
        }
        ZeahDetail.props(b, new int[][]{{1533, 3748}, {1550, 3758}, {1515, 3764}, {1548, 3740}}, "forge", 1101);
        // Tool shop.
        house(b, 1536, 3760, 1544, 3768, 1, ZeahDetail.LOVAKENGJ, "west", Room.STORE);
        b.tSet(1540, 3764, 0, rc("tool_shop"));
        // Minecart station and rails (the network was built by House Lovakengj).
        b.tFill(1512, 3742, 1556, 3742, 0, 0, "minecraft:rail");
        b.tSet(1518, 3742, 0, rc(ZeahBlocks.MINECART));
        // Lovakengj tower.
        b.tTower(1552, 3772, 4, 16, wall, floor);
        b.tFill(1551, 3771, 1553, 3773, 16, 16, "minecraft:copper_block");
        b.tFill(1552, 3772, 1552, 3772, 17, 18, "minecraft:cut_copper");
        b.tSet(1552, 3772, 19, "minecraft:lightning_rod");
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
        house(b, 1588, 3480, 1596, 3488, 1, ZeahDetail.GUILD, "east", Room.BANK);
        b.tSet(1592, 3484, 0, rc("bank_booth"));
        house(b, 1602, 3480, 1610, 3488, 1, ZeahDetail.GUILD, "east", Room.SHOP);
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
        // Log piles, a chopping block and the guild gateway.
        int g = b.ground();
        for (int[] lp : new int[][]{{1614, 3482}, {1626, 3482}, {1640, 3484}}) {
            b.tFill(lp[0], lp[1], lp[0] + 2, lp[1], 0, 0, "minecraft:oak_log[axis=x]");
            b.tFill(lp[0], lp[1], lp[0] + 1, lp[1], 1, 1, "minecraft:spruce_log[axis=x]");
        }
        b.tSet(1617, 3486, 0, "minecraft:oak_log");
        b.tSet(1618, 3486, 0, "minecraft:stripped_oak_log");
        b.tFill(1619, 3478, 1619, 3478, 0, 3, "minecraft:oak_log");
        b.tFill(1623, 3478, 1623, 3478, 0, 3, "minecraft:oak_log");
        b.fill(bx(1619), g + 4, bz(3478), bx(1623) + 1, g + 4, bz(3478), "minecraft:oak_log[axis=x]");
        b.set(bx(1621), g + 3, bz(3478), "minecraft:lantern[hanging=true]");
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
        // The statue of Ralos: a robed figure with a gilded sun crown on a stepped plinth, four fountains
        // in the quarters of the plaza, cypresses and benches.
        int g = b.ground();
        b.tFill(1678, 3128, 1682, 3132, 0, 0, "minecraft:smooth_quartz");
        b.tFill(1679, 3129, 1681, 3131, 1, 1, CUT_SANDSTONE);
        int sx = bx(1680);
        int sz = bz(3130);
        b.fill(sx, g + 2, sz, sx + 1, g + 6, sz + 1, "minecraft:white_concrete");
        b.fill(sx - 1, g + 2, sz, sx + 2, g + 3, sz + 1, "minecraft:yellow_concrete");
        b.fill(sx - 1, g + 5, sz, sx - 1, g + 7, sz, "minecraft:white_concrete");
        b.fill(sx + 2, g + 5, sz + 1, sx + 2, g + 6, sz + 1, "minecraft:white_concrete");
        b.fill(sx, g + 7, sz, sx + 1, g + 8, sz + 1, "minecraft:smooth_sandstone");
        b.fill(sx - 1, g + 9, sz - 1, sx + 2, g + 9, sz + 2, "minecraft:gold_block");
        b.set(sx - 1, g + 8, sz, "minecraft:gold_block");
        EasternDetail vd = new EasternDetail(b);
        for (int[] f : new int[][]{{1672, 3122}, {1688, 3122}, {1672, 3138}, {1688, 3138}}) {
            int fx = bx(f[0]) + 1;
            int fz = bz(f[1]) + 1;
            b.walls(fx - 2, g, fz - 2, fx + 2, g, fz + 2, "minecraft:smooth_quartz_slab");
            b.fill(fx - 1, g - 1, fz - 1, fx + 1, g - 1, fz + 1, "minecraft:water");
            b.fill(fx, g - 1, fz, fx, g + 1, fz, "minecraft:quartz_pillar");
            b.set(fx, g + 2, fz, "minecraft:water");
        }
        for (int[] c : new int[][]{{1669, 3119}, {1691, 3119}, {1669, 3141}, {1691, 3141}, {1677, 3119}, {1683, 3141}}) {
            ZeahDetail.cypress(b, bx(c[0]), bz(c[1]), 8);
        }
        vd.bench(bx(1676), bz(3126), true, "north", "minecraft:acacia");
        vd.bench(bx(1683), bz(3134), true, "south", "minecraft:acacia");
        // Bank.
        int[] bank = house(b, 1646, 3122, 1658, 3134, 1, ZeahDetail.CIVITAS_SAND, "east", Room.BANK);
        ZeahDetail.portico(b, bank, "east", "minecraft:quartz_pillar", "minecraft:smooth_quartz", "minecraft:brick_stairs", 4);
        b.tSet(1650, 3127, 0, rc("bank_booth"));
        b.tSet(1652, 3127, 0, rc("bank_booth"));
        b.tSet(1654, 3127, 0, rc("bank_booth"));
        // Market stalls south of the plaza.
        String[] stalls = {"general_store", "weapon_shop", "archery_shop", "rune_shop", "apothecary_table"};
        String[] awnings = {"minecraft:orange_wool", "minecraft:yellow_wool", "minecraft:red_wool", "minecraft:yellow_wool",
                "minecraft:orange_wool"};
        for (int i = 0; i < stalls.length; i++) {
            int x = 1668 + i * 6;
            ZeahDetail.stall(b, x, 3108, rc(stalls[i]), awnings[i], "minecraft:white_wool");
        }
        // Smithy and inn east of the plaza.
        house(b, 1706, 3118, 1718, 3126, 1, ZeahDetail.CIVITAS_SAND, "west", Room.STORE);
        b.tSet(1710, 3122, 0, rc("furnace"));
        b.tSet(1714, 3122, 0, rc("anvil"));
        int[] inn = house(b, 1706, 3134, 1718, 3144, 2, ZeahDetail.CIVITAS_WHITE, "west", Room.PUB);
        ZeahDetail.portico(b, inn, "west", "minecraft:quartz_pillar", "minecraft:smooth_quartz", "minecraft:brick_stairs", 4);
        b.tSet(1710, 3139, 0, rc("cooking_range"));
        b.tSet(1714, 3139, 0, rc("spinning_wheel"));
        // The Tower of Ascension (royal palace) at the head of the avenue.
        int[] pal = house(b, 1668, 3150, 1692, 3168, 3, ZeahDetail.CIVITAS_HALL, "south", Room.TEMPLE);
        b.tTower(1680, 3170, 5, 40, CUT_SANDSTONE, SAND_STONE);
        // The Tower of Ascension: gold rings and balconies up its height, tall windows, a crown and beacon.
        int tcx = bx(1680) + 1;
        int tcz = bz(3170) + 1;
        for (int y = g + 8; y < g + 40; y += 8) {
            b.replace(tcx - 5, y, tcz - 5, tcx + 5, y, tcz + 5, CUT_SANDSTONE, "minecraft:gold_block");
            for (int[] o : new int[][]{{5, 0}, {-5, 0}, {0, 5}}) {
                b.fill(tcx + o[0], y + 2, tcz + o[1], tcx + o[0], y + 4, tcz + o[1], "minecraft:yellow_stained_glass_pane");
            }
        }
        for (int[] o : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}, {-6, 0}, {6, 0}, {0, -6}, {0, 6}}) {
            b.fill(tcx + o[0], g + 40, tcz + o[1], tcx + o[0], g + 42, tcz + o[1], "minecraft:gold_block");
        }
        b.fill(tcx - 2, g + 40, tcz - 2, tcx + 2, g + 41, tcz + 2, "minecraft:smooth_sandstone");
        b.fill(tcx - 1, g + 42, tcz - 1, tcx + 1, g + 43, tcz + 1, "minecraft:gold_block");
        b.set(tcx, g + 44, tcz, "minecraft:beacon");
        b.set(tcx, g + 45, tcz, "minecraft:end_rod");
        // The palace: a colonnade along its front, gold-capped parapet and yellow Varlamore banners.
        for (int x = pal[0] + 2; x <= pal[2] - 2; x += 4) {
            b.fill(x, g, pal[3] + 3, x, g + 6, pal[3] + 3, "minecraft:quartz_pillar");
            b.set(x, g + 7, pal[3] + 3, "minecraft:chiseled_quartz_block");
            b.set(x, g + 5, pal[3] + 1, "minecraft:yellow_wall_banner[facing=south]");
        }
        b.fill(pal[0] + 1, g + 8, pal[3] + 1, pal[2] - 1, g + 8, pal[3] + 3, "minecraft:smooth_quartz_slab");
        b.fill(pal[0] + 1, g - 1, pal[3] + 1, pal[2] - 1, g - 1, pal[3] + 4, "minecraft:smooth_quartz");
        for (int x = pal[0]; x <= pal[2]; x += 4) {
            b.set(x, g + 12, pal[1], "minecraft:gold_block");
            b.set(x, g + 12, pal[3], "minecraft:gold_block");
        }
        // Temple of Ralos.
        int[] tem = house(b, 1730, 3146, 1750, 3164, 2, ZeahDetail.CIVITAS_WHITE, "south", Room.TEMPLE);
        ZeahDetail.portico(b, tem, "south", "minecraft:quartz_pillar", "minecraft:smooth_quartz", "minecraft:brick_stairs", 7);
        // the sun disc of Ralos on the pediment
        b.fill((tem[0] + tem[2]) / 2 - 1, g + 11, tem[3] + 1, (tem[0] + tem[2]) / 2 + 2, g + 12, tem[3] + 1, "minecraft:gold_block");
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
            String door = y < 3130 ? "north" : "south";
            house(b, x, y, x + 9, y + 7, 1 + i % 2, i % 2 == 0 ? ZeahDetail.CIVITAS_WHITE : ZeahDetail.CIVITAS_SAND, door,
                    i % 4 == 3 ? Room.SHOP : Room.HOUSE);
            // a little vine pergola or a potted cypress by the door
            int[] hq = ZeahDetail.rect(x, y, x + 9, y + 7);
            int hz = door.equals("north") ? hq[1] - 1 : hq[3] + 1;
            b.set(hq[0] + 2, g, hz, i % 2 == 0 ? "minecraft:potted_azalea_bush" : "minecraft:decorated_pot");
            b.set(hq[2] - 2, g, hz, "minecraft:flower_pot");
        }
        for (int x = 1650; x <= 1780; x += 16) {
            lamp(b, x, 3127, "minecraft:sandstone_wall");
            lamp(b, x, 3133, "minecraft:sandstone_wall");
        }
        colosseum(b);
        // Cypresses line the avenue up to the palace; the walls get weathered courses and gilded merlons.
        for (int ty = 3080; ty <= 3114; ty += 6) {
            ZeahDetail.cypress(b, bx(1696), bz(ty), 7);
            ZeahDetail.cypress(b, bx(1704) + 1, bz(ty), 7);
        }
        for (int ty = 3146; ty <= 3164; ty += 6) {
            ZeahDetail.cypress(b, bx(1696), bz(ty), 7);
            ZeahDetail.cypress(b, bx(1704) + 1, bz(ty), 7);
        }
        int[] cw = ZeahDetail.rect(1636, 3074, 1842, 3178);
        EasternDetail wd = new EasternDetail(b);
        wd.weather(cw[0], g, cw[1], cw[2], g + 7, cw[1] + 1, SAND_STONE, "minecraft:sandstone", 60, 1201);
        wd.weather(cw[0], g, cw[3] - 1, cw[2], g + 7, cw[3], SAND_STONE, "minecraft:sandstone", 60, 1202);
        b.replace(cw[0], g + 4, cw[1], cw[2], g + 4, cw[1] + 1, SAND_STONE, RED_SANDSTONE);
        b.replace(cw[0], g + 4, cw[3] - 1, cw[2], g + 4, cw[3], SAND_STONE, RED_SANDSTONE);
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
