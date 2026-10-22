package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Morytania and the northern isles: the Slayer Tower basement (carved under the tower the world
 * module built, entered from outside it), the Barrows crypts and tunnels, the Fossil Island Wyvern
 * Cave, the Lithkren Vault and the Morytania Spider Cave.
 */
final class DungeonsEast {
    private DungeonsEast() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(slayerTowerBasement());
        out.add(wyvernCave());
        out.add(wyvernCaveSouth());
        out.add(lithkrenVault());
        out.add(Barrows.dungeon());
        out.add(morytaniaSpiderCave());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /**
     * The Slayer Tower basement, below the tower (the building itself is the world module's and is
     * left untouched; the way down is a hatch just south of it, tile 3428, 3520): a pillared crypt
     * with aberrant spectres, nechryael and abyssal demons (approx. - which creatures roam the
     * basement is simplified).
     */
    static Dungeons.Dungeon slayerTowerBasement() {
        Cave c = new Cave("dng_slayer_tower_basement", "Slayer Tower basement", mc("polished_blackstone_bricks"),
                mc("cracked_polished_blackstone_bricks"), mc("shroomlight"));
        int f = 40;
        int ex = bx(3428);
        int ez = bz(3520);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hallZ(ex, ez - 5, ex, ez - 30, f, 3, 4);
        int x1 = bx(3405);
        int x2 = bx(3438);
        int z1 = bz(3562);
        int z2 = ez - 30;
        c.room(x1, z1, x2, z2, f, 6);
        c.carve();
        for (int x = x1 + 6; x < x2 - 2; x += 10) {
            for (int z = z1 + 6; z < z2 - 2; z += 10) {
                c.pillar(x, z, f, 6, mc("chiseled_polished_blackstone"));
            }
        }
        int mid = (x1 + x2) / 2;
        c.b.walls(mid - 1, f + 1, z1, mid + 1, f + 6, z2 - 12, c.wall);
        c.entrance(ex, ez, f, mc("polished_blackstone_bricks"), mc("polished_blackstone"), 's');
        c.camp(x1 + 2, z1 + 2, mid - 3, z2 - 2, f, 5, 25, "aberrant_spectre", "nechryael");
        c.camp(mid + 3, z1 + 2, x2 - 2, z2 - 2, f, 4, 25, "abyssal_demon", "nechryael");
        return c.build();
    }

    // The Wyvern Cave's cache spawn tiles (mejrs/data_osrs; the wiki's LocLines agree, except that the wiki labels the two
    // long-tailed groups the other way round: the northern cave is the task-only one).
    static final int[] WYVERN_TASK_SPITTING = {3603, 10273, 3604, 10266, 3611, 10280, 3612, 10274};
    static final int[] WYVERN_TASK_TALONED = {3622, 10291, 3627, 10288, 3628, 10282, 3634, 10280};
    static final int[] WYVERN_TASK_LONG_TAILED = {3597, 10254, 3602, 10250, 3609, 10251, 3615, 10249};
    static final int[] WYVERN_TASK_ANCIENT = {3627, 10246, 3632, 10252, 3636, 10246};
    static final int[] WYVERN_SPITTING = {3623, 10189, 3623, 10195, 3624, 10185, 3630, 10193};
    static final int[] WYVERN_TALONED = {3605, 10215, 3608, 10204, 3610, 10215, 3612, 10210};
    static final int[] WYVERN_LONG_TAILED = {3596, 10195, 3599, 10186, 3601, 10200, 3602, 10193};
    static final int[] WYVERN_ANCIENT = {3634, 10205, 3634, 10212};
    static final int[] WYVERN_ICE_SPIDERS = {3593, 10217, 3603, 10223, 3631, 10228, 3635, 10233, 3636, 10227};

    /**
     * The Wyvern Cave on Fossil Island, a pair of caverns in one cache region. This is the northern cave, down the trapdoor
     * in the Mushroom Forest (surface 3677, 3854; the cache's 3595, 10291): it is task-only, Weve letting only players on a
     * Fossil Island wyvern task fight its 4 spitting, 4 taloned, 4 long-tailed and 3 ancient wyverns. Single-way and too
     * fungal for a cannon; the room outlines are approx. The southern cave is {@link #wyvernCaveSouth()}.
     */
    static Dungeons.Dungeon wyvernCave() {
        Cave c = new Cave("dng_wyvern_cave", "Wyvern Cave", mc("tuff"), mc("smooth_basalt"), mc("shroomlight"));
        int f = 14;
        CacheDungeons.Shifted north = new CacheDungeons.Shifted(c, 3677 - 3595, 3854 - 10291);
        int[] landing = {3591, 10285, 3601, 10296};
        int[] spitting = {3600, 10262, 3616, 10284};
        int[] taloned = {3617, 10276, 3638, 10295};
        int[] longTailed = {3594, 10244, 3618, 10257};
        int[] ancient = {3622, 10242, 3640, 10256};
        int[][] task = {landing, spitting, taloned, longTailed, ancient};
        for (int[] r : task) {
            north.room(r, f, 9);
        }
        north.hall(3600, 10288, 3604, 10284, f);
        north.hallZ(3608, 10262, 3608, 10257, f);
        north.hall(3618, 10250, 3622, 10250, f);
        c.carve();
        for (int[] p : new int[][]{{3606, 10276}, {3628, 10286}, {3605, 10250}, {3632, 10248}}) {
            c.pillar(north.x(p[0]), north.z(p[1]), f, 9, mc("basalt[axis=y]"));
        }
        // The dragonkin shrine of the northern cave.
        c.set(north.x(3631), f + 1, north.z(10292), mc("chiseled_polished_blackstone"));
        c.entrance(north.x(3595), north.z(10291), f, mc("tuff"), mc("smooth_basalt"), 'w');
        north.camps(task, f, CacheDungeons.DRAGON_RESPAWN, "spitting_wyvern", WYVERN_TASK_SPITTING);
        north.camps(task, f, CacheDungeons.DRAGON_RESPAWN, "taloned_wyvern", WYVERN_TASK_TALONED);
        north.camps(task, f, CacheDungeons.DRAGON_RESPAWN, "long_tailed_wyvern", WYVERN_TASK_LONG_TAILED);
        north.camps(task, f, CacheDungeons.DRAGON_RESPAWN, "ancient_wyvern", WYVERN_TASK_ANCIENT);
        for (int[] r : task) {
            north.taskOnly(r, f, 9, "fossil_island_wyverns", "Weve");
        }
        return c.build();
    }

    /**
     * The Wyvern Cave's southern cavern, entered south of the Museum Camp (surface 3746, 3779; the landing tile is approx):
     * 4 spitting, 4 taloned, 4 long-tailed and 2 ancient wyverns and 5 ice spiders on their cache tiles, open to all.
     */
    static Dungeons.Dungeon wyvernCaveSouth() {
        Cave c = new Cave("dng_wyvern_cave_south", "Wyvern Cave (south)", mc("tuff"), mc("smooth_basalt"), mc("shroomlight"));
        int f = 14;
        CacheDungeons.Shifted south = new CacheDungeons.Shifted(c, 3746 - 3615, 3779 - 10232);
        int[] spiders = {3590, 10214, 3640, 10236};
        int[] west = {3592, 10183, 3617, 10213};
        int[] east = {3618, 10182, 3640, 10213};
        int[][] open = {west, east, spiders};
        for (int[] r : open) {
            south.room(r, f, 9);
        }
        c.carve();
        for (int[] p : new int[][]{{3600, 10226}, {3626, 10222}, {3606, 10194}, {3628, 10200}}) {
            c.pillar(south.x(p[0]), south.z(p[1]), f, 9, mc("basalt[axis=y]"));
        }
        c.pool(south.x(3612), south.z(10199), south.x(3615), south.z(10195), f, mc("lava"));
        c.entrance(south.x(3615), south.z(10232), f, mc("tuff"), mc("smooth_basalt"), 'n');
        south.camps(open, f, CacheDungeons.DRAGON_RESPAWN, "spitting_wyvern", WYVERN_SPITTING);
        south.camps(open, f, CacheDungeons.DRAGON_RESPAWN, "taloned_wyvern", WYVERN_TALONED);
        south.camps(open, f, CacheDungeons.DRAGON_RESPAWN, "long_tailed_wyvern", WYVERN_LONG_TAILED);
        south.camps(open, f, CacheDungeons.DRAGON_RESPAWN, "ancient_wyvern", WYVERN_ANCIENT);
        south.camps(new int[][]{spiders}, f, CacheDungeons.RESPAWN, "ice_spider", WYVERN_ICE_SPIDERS);
        return c.build();
    }

    /**
     * The Lithkren Vault beneath the ruined Dragonkin laboratory on Lithkren (entrance tile 3550,
     * 3998): the vault's stair, the adamant dragons' hall and the rune dragons' hall.
     */
    static Dungeons.Dungeon lithkrenVault() {
        Cave c = new Cave("dng_lithkren_vault", "Lithkren Vault", mc("deepslate_tiles"), mc("polished_deepslate"), mc("sea_lantern"));
        int f = 0;
        int ox = bx(3550);
        int oz = bz(3998);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 6);
        c.hall(ox - 6, oz, ox - 20, oz, f, 3, 5);
        c.room(ox - 60, oz - 22, ox - 20, oz + 22, f, 10);              // adamant dragons
        c.hall(ox + 6, oz, ox + 20, oz, f, 3, 5);
        c.room(ox + 20, oz - 22, ox + 60, oz + 22, f, 10);              // rune dragons
        c.carve();
        for (int[] p : new int[][]{{-52, -14}, {-28, -14}, {-52, 14}, {-28, 14}, {28, -14}, {52, -14}, {28, 14}, {52, 14}}) {
            c.pillar(ox + p[0], oz + p[1], f, 10, mc("chiseled_deepslate"));
        }
        c.fill(ox - 1, f + 1, oz - 1, ox + 1, f + 1, oz + 1, mc("lodestone"));
        c.entrance(ox + 4, oz + 4, f, mc("deepslate_bricks"), mc("polished_deepslate"), 's');
        c.camp(ox - 58, oz - 20, ox - 22, oz + 20, f, 3, 50, "adamant_dragon");
        c.camp(ox + 22, oz - 20, ox + 58, oz + 20, f, 3, 50, "rune_dragon");
        return c.build();
    }

    // The wiki's araxyte LocLines (July 2026): the main cavern and the three task-only chambers.
    static final int[] ARAXYTES_MAIN = {3658, 9842, 3663, 9831, 3664, 9837, 3665, 9826, 3667, 9812, 3670, 9817, 3672, 9818, 3673, 9837,
            3674, 9807, 3678, 9804, 3679, 9834, 3680, 9840, 3681, 9802, 3684, 9835, 3687, 9836};
    static final int[] ARAXYTES_146_MAIN = {3660, 9841, 3663, 9835, 3664, 9824, 3667, 9817, 3674, 9834, 3675, 9837, 3681, 9835};
    static final int[] ARAXYTES_TASK = {3693, 9845, 3696, 9847, 3697, 9841, 3700, 9835, 3700, 9838, 3700, 9843, 3700, 9845, 3702, 9832,
            3703, 9845, 3704, 9837, 3705, 9834, 3705, 9840};
    static final int[] ARAXYTES_146_TASK = {3668, 9851, 3675, 9855, 3677, 9852, 3681, 9853, 3686, 9822, 3689, 9817, 3691, 9820,
            3692, 9813, 3695, 9821, 3697, 9814, 3700, 9817, 3701, 9820, 3702, 9812, 3666, 9858, 3669, 9859, 3675, 9859, 3679, 9859,
            3683, 9859, 3687, 9857, 3692, 9856, 3696, 9858};

    /**
     * The Morytania Spider Cave (the araxytes' nest), on the wiki's spawn tiles (dungeon y - 6400): the main cavern with 15
     * level-96 and 7 level-146 araxytes, and the task-only section behind Weave (OSRS Wiki): a northern chamber and a
     * southern chamber of level-146 araxytes and a middle chamber of level-96 ones (12, 9 and 12). The whole cave is
     * multicombat except the northern task-only chamber. Araxxor himself is fought at the bosses module's arena on the
     * real entrance tile (3657, 3407), so the way down is a hut just north of it (approx.); the tunnels between the
     * caverns are approx.
     */
    static Dungeons.Dungeon morytaniaSpiderCave() {
        Cave c = new Cave("dng_morytania_spider_cave", "Morytania Spider Cave", mc("deepslate"), mc("tuff"), mc("ochre_froglight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, -6400);
        int f = 16;
        int[] main = {3655, 9800, 3683, 9844};
        int[] mainEast = {3684, 9830, 3689, 9844};
        int[] taskSouth = {3685, 9810, 3704, 9825};
        int[] taskMiddle = {3692, 9830, 3707, 9848};
        int[] taskNorth = {3663, 9850, 3702, 9862};
        int[][] rooms = {main, mainEast, taskSouth, taskMiddle, taskNorth};
        for (int[] r : rooms) {
            s.room(r, f, 8);
        }
        for (int[] r : new int[][]{main, mainEast, taskSouth, taskMiddle}) {
            s.multi(r, f, 8);
        }
        s.hall(3683, 9815, 3685, 9815, f);           // a tunnel to the southern chamber
        s.hall(3689, 9838, 3692, 9838, f);           // a tunnel to the middle chamber
        s.hallZ(3670, 9844, 3670, 9850, f);          // a tunnel to the northern chamber
        c.carve();
        for (int[] p : new int[][]{{3662, 9810}, {3676, 9822}, {3668, 9838}, {3694, 9818}, {3700, 9842}, {3680, 9856}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 8, mc("deepslate_tiles"));
        }
        for (int[] p : new int[][]{{3660, 9806}, {3672, 9830}, {3686, 9840}, {3690, 9822}, {3704, 9846}, {3672, 9860}}) {
            c.set(s.x(p[0]), f + 1, s.z(p[1]), mc("cobweb"));
            c.set(s.x(p[0]) + 1, f + 8, s.z(p[1]), mc("cobweb"));
        }
        c.entrance(StructureDef.bx(3657), StructureDef.bz(3418), f, mc("deepslate_bricks"), mc("tuff"), 'e');
        s.camps(rooms, f, CacheDungeons.RESPAWN, "araxyte", ARAXYTES_MAIN);
        s.camps(rooms, f, CacheDungeons.RESPAWN, "araxyte_lvl146", ARAXYTES_146_MAIN);
        s.camps(rooms, f, CacheDungeons.RESPAWN, "araxyte", ARAXYTES_TASK);
        s.camps(rooms, f, CacheDungeons.RESPAWN, "araxyte_lvl146", ARAXYTES_146_TASK);
        for (int[] r : new int[][]{taskSouth, taskMiddle, taskNorth}) {
            s.taskOnly(r, f, 8, "araxytes", "Weave");
        }
        return c.build();
    }
}
