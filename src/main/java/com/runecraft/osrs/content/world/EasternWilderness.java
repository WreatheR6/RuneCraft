package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;
import java.util.Random;

import static com.runecraft.osrs.content.world.EasternKit.*;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

import com.runecraft.osrs.content.world.EasternKit.Style;

/**
 * The Wilderness at real OSRS tiles: the ditch along y=3520, the Chaos Temple, the Graveyard of Shadows,
 * the Dark Warriors' Fortress, the Bandit Camp, the Ferox Enclave, the Lava Maze, the Mage Arena, the
 * Wilderness Agility Course, the Deserted Keep, the Resource Area, the Demonic Ruins, Scorpia's pit,
 * Rogues' Castle, the Chaos Altar temple, the Pirates' Hideout, boss ruins and scattered ruins.
 */
final class EasternWilderness {
    private static final String[] RUIN = {MOSSY_BRICKS, CRACKED_BRICKS, COBBLE, MOSSY_COBBLE, STONE_BRICKS};
    private static final String[] RUIN_BLACK = {BLACKSTONE, PBB, CRACKED_PBB, "minecraft:basalt", COBBLE};
    private static final String NETHERRACK = "minecraft:netherrack";

    private EasternWilderness() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(ditch("wilderness_ditch_west", "Wilderness ditch (west)", new int[][]{{2945, 2999}, {3036, 3200}}));
        out.add(ditch("wilderness_ditch_east", "Wilderness ditch (east)", new int[][]{{3201, 3390}}));
        out.add(chaosTempleHut());
        out.add(graveyardOfShadows());
        out.add(darkWarriorsFortress());
        out.add(banditCamp());
        out.add(feroxEnclave());
        out.add(lavaMaze());
        out.add(mageArena());
        out.add(agilityCourse());
        out.add(desertedKeep());
        out.add(resourceArea());
        out.add(demonicRuins());
        out.add(scorpiasPit());
        out.add(roguesCastle());
        out.add(roguesCastleGrounds());
        out.add(chaosAltarTemple());
        out.add(chaosFanaticRuins());
        out.add(piratesHideout());
        out.add(vetionRuins());
        out.add(crazyArchaeologistDig());
        out.add(scatteredRuins());
    }

    // ------------------------------------------------------------------ the ditch

    /**
     * The Wilderness ditch: a trench along OSRS y 3520-3521 with a low stone wall on its north lip and
     * stepped crossings every 16 blocks. Built with raw ops (a narrow cleared band), no flattening.
     */
    static StructureDef ditch(String id, String name, int[][] segments) {
        EasternKit k = new EasternKit(id, name);
        int g = k.g;
        String[] mats = {COBBLE, MOSSY_COBBLE, STONE_BRICKS, MOSSY_BRICKS, CRACKED_BRICKS};
        for (int[] seg : segments) {
            int x1 = bx(seg[0]);
            int x2 = bx(seg[1]) + 1;
            int zN = bz(3523);
            int zS = bz(3519) + 1;
            int tN = bz(3521);
            int tS = bz(3520) + 1;
            int wz = tN - 1;
            k.clear(x1, g, zN, x2, g + 12, zS);
            k.fill(x1, g - 6, zN, x2, g - 2, zS, "minecraft:dirt");
            k.fill(x1, g - 1, zN, x2, g - 1, zS, GRASS);
            k.clear(x1, g - 2, tN, x2, g - 1, tS);
            k.fill(x1, g - 3, tN, x2, g - 3, tS, COARSE);
            int i = 0;
            for (int x = x1; x <= x2; x += 24) {
                k.fill(x, g - 2, wz, Math.min(x + 23, x2), g, wz, mats[i++ % mats.length]);
            }
            Random rnd = new Random(seg[0]);
            for (int x = x1 + 3; x <= x2; x += 8) {
                if (rnd.nextInt(3) > 0) {
                    k.set(x, g + 1, wz, rnd.nextBoolean() ? "minecraft:mossy_cobblestone_wall" : "minecraft:cobblestone_wall");
                }
                if (rnd.nextInt(4) == 0) {
                    k.set(x + 2, g - 2, tN + 1 + rnd.nextInt(2), GRAVEL);
                }
            }
            for (int x = x1 + 7; x < x2; x += 16) {
                k.clear(x, g, wz, x + 1, g + 1, wz);
                k.fill(x, g - 2, tN, x + 1, g - 2, tN, "minecraft:cobblestone_stairs[facing=north]");
                k.fill(x, g - 2, tS, x + 1, g - 2, tS, "minecraft:cobblestone_stairs[facing=south]");
            }
        }
        return k.build();
    }

    // ------------------------------------------------------------------ temples & graveyards

    /** The Chaos Temple hut (level 12): small stone chapel with the chaos altar inside a fenced yard. */
    static StructureDef chaosTempleHut() {
        EasternKit k = new EasternKit("chaos_temple_hut", "Chaos Temple");
        int g = k.g;
        k.flat(3228, 3627, 3244, 3643, COARSE);
        k.patches(3228, 3627, 3244, 3643, GRAVEL, 8, 301);
        int[] t = k.hall(3232, 3631, 3240, 3638, 1, WILD, "south");
        int mx = (t[0] + t[2]) / 2;
        k.fill(mx - 1, g, t[1] + 1, mx + 2, g, t[1] + 2, "minecraft:polished_blackstone");
        k.set(mx, g + 1, t[1] + 2, ALTAR);
        k.set(mx - 1, g + 1, t[1] + 1, "minecraft:candle[candles=3,lit=true]");
        k.set(mx + 2, g + 1, t[1] + 1, "minecraft:candle[candles=3,lit=true]");
        k.set(mx, g + 2, t[1], "minecraft:red_wall_banner[facing=south]");
        k.set(mx + 1, g + 2, t[1], "minecraft:red_wall_banner[facing=south]");
        k.fill(mx, g, t[1] + 3, mx + 1, g, t[3] - 1, "minecraft:red_carpet");
        int[] y = r(3229, 3628, 3243, 3642);
        k.b.walls(y[0], g, y[1], y[2], g, y[3], "minecraft:spruce_fence");
        k.clear(mx, g, y[3], mx + 1, g, y[3]);
        k.graveyard(3229, 3639, 3231, 3642, 302);
        k.deadTree(3242, 3641, 5);
        return k.build();
    }

    /** The Graveyard of Shadows (level 18-20): broken walls, rows of graves, dead trees and a crypt. */
    static StructureDef graveyardOfShadows() {
        EasternKit k = new EasternKit("graveyard_of_shadows", "Graveyard of Shadows");
        int g = k.g;
        k.flat(3150, 3660, 3172, 3682, PODZOL);
        k.patches(3150, 3660, 3172, 3682, COARSE, 20, 311);
        k.ruin(3151, 3661, 3171, 3681, 3, RUIN, null, 312);
        k.graveyard(3153, 3663, 3169, 3679, 313);
        int[] c = k.hall(3158, 3668, 3163, 3673, 1, WILD, "south");
        k.set((c[0] + c[2]) / 2, g, c[1] + 1, "minecraft:skeleton_skull");
        k.set(c[0] + 1, g, c[1] + 1, "minecraft:candle[candles=2,lit=true]");
        k.deadTree(3154, 3678, 6);
        k.deadTree(3168, 3664, 5);
        k.deadTree(3167, 3678, 6);
        k.scatter(3151, 3661, 3171, 3681, 0, "minecraft:bone_block", 4, 314);
        k.scatter(3151, 3661, 3171, 3681, 0, COBWEB, 6, 315);
        return k.build();
    }

    // ------------------------------------------------------------------ forts & camps

    /** The Dark Warriors' Fortress: square curtain walls, corner towers, a south gate and a central keep. */
    static StructureDef darkWarriorsFortress() {
        EasternKit k = new EasternKit("dark_warriors_fortress", "Dark Warriors' Fortress");
        int g = k.g;
        k.flat(3012, 3615, 3048, 3647, COARSE);
        String cap = "minecraft:stone_brick_wall";
        k.wall(3016, 3619, 3044, 3619, 7, CRACKED_BRICKS, cap);
        k.wall(3016, 3643, 3044, 3643, 7, CRACKED_BRICKS, cap);
        k.wall(3016, 3619, 3016, 3643, 7, CRACKED_BRICKS, cap);
        k.wall(3044, 3619, 3044, 3643, 7, CRACKED_BRICKS, cap);
        k.gate(3029, 3619, 3031, 3619, 6);
        k.fill(bx(3029), g + 4, bz(3619), bx(3031) + 1, g + 4, bz(3619) + 1, "minecraft:iron_bars");
        for (int[] t : new int[][]{{3016, 3619}, {3044, 3619}, {3016, 3643}, {3044, 3643}}) {
            k.tower(t[0], t[1], 3, 11, STONE_BRICKS, COBBLE, null, null);
        }
        k.floor(3017, 3620, 3043, 3642, GRAVEL);
        k.floor(3029, 3620, 3031, 3630, COBBLE);
        int[] keep = k.house(3024, 3631, 3036, 3640, 2, WILD, "south");
        k.set(keep[0] + 3, g, keep[1] + 2, WEAPONS);
        k.set(keep[2] - 3, g + 4, keep[1] + 2, ANVIL);
        k.fill(keep[0] + 1, g, keep[3] - 2, keep[0] + 1, g + 1, keep[3] - 2, "minecraft:iron_bars");
        // Walkway along the inside of the walls.
        k.tfill(3017, 3620, 3043, 3620, 4, 4, "minecraft:spruce_slab[type=top]");
        k.tfill(3017, 3642, 3043, 3642, 4, 4, "minecraft:spruce_slab[type=top]");
        k.campfire(bx(3021), bz(3624));
        k.set(bx(3040), g, bz(3624), "minecraft:hay_block");
        k.set(bx(3040), g, bz(3625), "minecraft:barrel[facing=up]");
        // Black banners on the towers, a weapon rack and training dummies in the yard, rubble by the walls.
        for (int[] t : new int[][]{{3016, 3619}, {3044, 3619}, {3016, 3643}, {3044, 3643}}) {
            k.set(cx(t[0]), g + 8, cz(t[1]) + 3, "minecraft:black_wall_banner[facing=south]");
        }
        k.set((keep[0] + keep[2]) / 2 - 2, g + 3, keep[3] + 1, "minecraft:black_wall_banner[facing=south]");
        for (int x = bx(3020); x <= bx(3026); x += 3) {
            k.fill(x, g, bz(3627), x, g + 1, bz(3627), "minecraft:spruce_fence");
            k.set(x, g + 2, bz(3627), "minecraft:carved_pumpkin[facing=south]");
        }
        k.dressTown(3017, 3620, 3043, 3642, "wild", 8, 322);
        return k.build();
    }

    /** The Wilderness Bandit Camp: palisade, tents, campfire and Noterazzo's shop. */
    static StructureDef banditCamp() {
        EasternKit k = new EasternKit("wilderness_bandit_camp", "Bandit Camp (Wilderness)");
        int g = k.g;
        k.flat(3026, 3690, 3054, 3712, COARSE);
        k.patches(3026, 3690, 3054, 3712, GRAVEL, 12, 321);
        k.fence(3028, 3692, 3052, 3692, "minecraft:spruce_fence");
        k.fence(3028, 3710, 3052, 3710, "minecraft:spruce_fence");
        k.fence(3028, 3692, 3028, 3710, "minecraft:spruce_fence");
        k.fence(3052, 3692, 3052, 3710, "minecraft:spruce_fence");
        k.b.tClear(3039, 3692, 3041, 3692, 0, 0);
        k.b.tClear(3028, 3700, 3028, 3702, 0, 0);
        int[] shop = k.shop(3043, 3700, 3050, 3707, 1, WILD, "west", GENERAL);
        k.set(shop[0] + 1, g, shop[1] + 1, "minecraft:barrel[facing=up]");
        k.tentT(3031, 3703, 3034, 3708, "minecraft:brown_wool");
        k.tentT(3036, 3703, 3039, 3708, "minecraft:red_wool");
        k.tentT(3031, 3694, 3034, 3699, "minecraft:black_wool");
        k.tentT(3044, 3694, 3047, 3698, "minecraft:brown_wool");
        k.campfire(bx(3040), bz(3700));
        k.set(bx(3038), g, bz(3697), "minecraft:chest[facing=east]");
        k.dressTown(3028, 3692, 3052, 3710, "wild", 8, 323);
        return k.build();
    }

    /** The Ferox Enclave: a walled safe haven with gates, the bank, the pool of refreshment and an inn. */
    static StructureDef feroxEnclave() {
        EasternKit k = new EasternKit("ferox_enclave", "Ferox Enclave");
        int g = k.g;
        k.flat(3120, 3614, 3158, 3650, GRASS);
        k.patches(3120, 3614, 3158, 3650, COARSE, 12, 331);
        String cap = "minecraft:stone_brick_wall";
        k.wall(3123, 3617, 3155, 3617, 6, STONE_BRICKS, cap);
        k.wall(3123, 3647, 3155, 3647, 6, STONE_BRICKS, cap);
        k.wall(3123, 3617, 3123, 3647, 6, STONE_BRICKS, cap);
        k.wall(3155, 3617, 3155, 3647, 6, STONE_BRICKS, cap);
        for (int[] gt : new int[][]{{3138, 3617, 3140, 3617}, {3138, 3647, 3140, 3647}, {3123, 3631, 3123, 3633}, {3155, 3631, 3155, 3633}}) {
            k.gate(gt[0], gt[1], gt[2], gt[3], 6);
            k.tfill(gt[0], gt[1], gt[2], gt[3], 2, 3, "minecraft:light_blue_stained_glass_pane");
        }
        for (int[] t : new int[][]{{3123, 3617}, {3155, 3617}, {3123, 3647}, {3155, 3647}}) {
            k.tower(t[0], t[1], 3, 10, STONE_BRICKS, "minecraft:spruce_planks", null, "minecraft:spruce_planks");
        }
        k.floor(3124, 3631, 3154, 3633, "minecraft:stone_bricks");
        k.floor(3138, 3618, 3140, 3646, "minecraft:stone_bricks");
        k.floor(3134, 3627, 3144, 3637, "minecraft:polished_andesite");
        // Bank (west), pool of refreshment (north-west), inn (east), Ferox shops (south).
        int[] bank = k.hall(3125, 3620, 3134, 3628, 1, FEROX, "north");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[3] - 3, g, "minecraft:spruce_slab[type=top]");
        int[] pool = k.hall(3125, 3636, 3134, 3645, 1, FEROX, "south");
        int pmx = (pool[0] + pool[2]) / 2;
        int pmz = (pool[1] + pool[3]) / 2;
        // The chapel's two pools of refreshment (the godwars module's FeroxService: no special attack restored, unlike a
        // house's ornate pool).
        k.set(pmx - 1, g, pmz, "runecraft:ferox_pool_of_refreshment");
        k.set(pmx + 1, g, pmz, "runecraft:ferox_pool_of_refreshment");
        k.ring(pmx, g - 1, g - 1, pmz, 3.5, 1.0, "minecraft:prismarine_bricks");
        k.set(pool[0] + 1, g, pool[1] + 1, ALTAR);
        int[] inn = k.hall(3145, 3636, 3154, 3645, 2, FEROX, "south");
        k.counterX(inn[0] + 2, inn[2] - 2, inn[1] + 3, g, "minecraft:spruce_slab[type=top]", RANGE);
        k.fill(inn[0] + 2, g, inn[1] + 1, inn[2] - 2, g, inn[1] + 1, "minecraft:barrel[facing=up]");
        k.shop(3145, 3620, 3154, 3628, 1, FEROX, "north", GENERAL);
        // Central garden.
        int cxg = bx(3139);
        int czg = bz(3632);
        k.fountain(cxg + 1, czg + 1, 2, STONE_BRICKS, "minecraft:chiseled_stone_bricks");
        k.tree(3135, 3628, 5, "minecraft:oak_log", "minecraft:oak_leaves");
        k.tree(3143, 3636, 5, "minecraft:oak_log", "minecraft:oak_leaves");
        k.tree(3135, 3636, 4, "minecraft:birch_log", "minecraft:birch_leaves");
        k.tree(3143, 3628, 4, "minecraft:birch_log", "minecraft:birch_leaves");
        // Blue-and-white banners of the enclave over each gate, portcullises raised in the gate arches,
        // guard posts with weapon racks, clipped hedges and flower beds round the garden, benches, and the
        // inn's sign and beer garden.
        for (int[] gt : new int[][]{{3138, 3617, 3140, 3617, 0}, {3138, 3647, 3140, 3647, 1}, {3123, 3631, 3123, 3633, 2},
                {3155, 3631, 3155, 3633, 3}}) {
            int[] q = r(gt[0], gt[1], gt[2], gt[3]);
            String[] face = {"north", "south", "west", "east"};
            String[] in = {"south", "north", "east", "west"};
            k.fill(q[0], g + 4, q[1], q[2], g + 4, q[3], "minecraft:iron_bars");
            if (gt[4] < 2) {
                int z = gt[4] == 0 ? q[1] - 1 : q[3] + 1;
                int zi = gt[4] == 0 ? q[3] + 1 : q[1] - 1;
                k.set(q[0] - 1, g + 4, z, "minecraft:blue_wall_banner[facing=" + face[gt[4]] + "]");
                k.set(q[2] + 1, g + 4, z, "minecraft:white_wall_banner[facing=" + face[gt[4]] + "]");
                k.set(q[0] - 1, g + 3, zi, "minecraft:lantern[hanging=true]");
                k.set(q[2] + 1, g + 3, zi, "minecraft:lantern[hanging=true]");
                k.set(q[0] - 2, g, zi, "minecraft:barrel[facing=up]");
                k.set(q[2] + 2, g, zi, "minecraft:grindstone[face=floor,facing=" + in[gt[4]] + "]");
            } else {
                int x = gt[4] == 2 ? q[0] - 1 : q[2] + 1;
                int xi = gt[4] == 2 ? q[2] + 1 : q[0] - 1;
                k.set(x, g + 4, q[1] - 1, "minecraft:blue_wall_banner[facing=" + face[gt[4]] + "]");
                k.set(x, g + 4, q[3] + 1, "minecraft:white_wall_banner[facing=" + face[gt[4]] + "]");
                k.set(xi, g + 3, q[1] - 1, "minecraft:lantern[hanging=true]");
                k.set(xi, g + 3, q[3] + 1, "minecraft:lantern[hanging=true]");
                k.set(xi, g, q[1] - 2, "minecraft:barrel[facing=up]");
            }
        }
        for (int[] t : new int[][]{{3123, 3617}, {3155, 3617}, {3123, 3647}, {3155, 3647}}) {
            k.set(cx(t[0]), g + 9, cz(t[1]) + 3, "minecraft:blue_wall_banner[facing=south]");
        }
        // Garden: hedges along the paths, flower beds in the corners, benches facing the fountain.
        int[] gd = r(3134, 3627, 3144, 3637);
        for (int[] h : new int[][]{{gd[0], gd[1], gd[0] + 5, gd[1]}, {gd[2] - 5, gd[1], gd[2], gd[1]}, {gd[0], gd[3], gd[0] + 5, gd[3]},
                {gd[2] - 5, gd[3], gd[2], gd[3]}}) {
            k.fill(h[0], g, h[1], h[2], g, h[3], "minecraft:oak_leaves[persistent=true]");
        }
        k.d.flowerBed(gd[0] + 1, gd[1] + 2, gd[0] + 4, gd[1] + 5, "minecraft:stone_brick_slab[type=top]", 332);
        k.d.flowerBed(gd[2] - 4, gd[3] - 5, gd[2] - 1, gd[3] - 2, "minecraft:stone_brick_slab[type=top]", 333);
        k.d.bench(cxg - 1, czg - 3, true, "south", "minecraft:spruce");
        k.d.bench(cxg + 2, czg + 5, true, "north", "minecraft:spruce");
        // The inn: sign, a beer garden with tables, kegs by the wall.
        k.set((inn[0] + inn[2]) / 2 - 2, g + 3, inn[1] - 1, "minecraft:blue_wall_banner[facing=north]");
        for (int x = inn[0] + 2; x < inn[2] - 2; x += 5) {
            k.set(x, g, inn[1] - 3, "minecraft:spruce_slab[type=top]");
            k.set(x - 1, g, inn[1] - 3, "minecraft:spruce_stairs[facing=east]");
            k.set(x + 1, g, inn[1] - 3, "minecraft:spruce_stairs[facing=west]");
        }
        k.fill(inn[2] - 1, g, inn[3] + 1, inn[2] - 1, g + 1, inn[3] + 1, "minecraft:barrel[facing=north]");
        k.take(gd[0] - 1, gd[1] - 1, gd[2] + 1, gd[3] + 1);
        k.take(inn[0], inn[1] - 4, inn[2], inn[1]);
        k.dressTown(3123, 3617, 3155, 3647, "ferox", 14, 334);
        return k.build();
    }

    // ------------------------------------------------------------------ deep wilderness

    /** The Lava Maze: netherrack wastes cut by lava channels with basalt bridges, blackstone walls, the KBD ruin. */
    static StructureDef lavaMaze() {
        EasternKit k = new EasternKit("lava_maze", "Lava Maze").replaces("kbd_lair");
        int g = k.g;
        k.flat(3024, 3818, 3104, 3882, NETHERRACK);
        k.patches(3024, 3818, 3104, 3882, "minecraft:magma_block", 30, 341);
        k.patches(3024, 3818, 3104, 3882, BLACKSTONE, 30, 342);
        k.patches(3024, 3818, 3104, 3882, "minecraft:basalt", 20, 343);
        Random rnd = new Random(344);
        int x1 = bx(3026);
        int x2 = bx(3102) + 1;
        for (int ty : new int[]{3826, 3836, 3846, 3858, 3868, 3876}) {
            int z = bz(ty);
            k.fill(x1, g - 2, z, x2, g - 2, z + 1, "minecraft:magma_block");
            k.fill(x1, g - 1, z, x2, g - 1, z + 1, LAVA);
            for (int b = 0; b < 3; b++) {
                int bxp = x1 + 6 + rnd.nextInt(x2 - x1 - 12);
                k.fill(bxp, g - 1, z, bxp + 2, g - 1, z + 1, "minecraft:basalt");
            }
        }
        for (int i = 0; i < 6; i++) {
            int tx = 3030 + rnd.nextInt(66);
            int ty = 3829 + 10 * (i % 5);
            int[] q = r(tx, ty, tx + 2 + rnd.nextInt(10), ty + 1);
            k.fill(q[0], g, q[1], q[2], g + 1, q[1], i % 2 == 0 ? BLACKSTONE : "minecraft:polished_basalt");
        }
        for (int i = 0; i < 4; i++) {
            int tx = 3036 + 16 * i;
            int z1 = bz(3870);
            k.fill(bx(tx), g - 2, z1, bx(tx) + 1, g - 1, z1 + 10, "minecraft:magma_block");
            k.fill(bx(tx), g - 1, z1 + 2, bx(tx) + 1, g - 1, z1 + 8, LAVA);
        }
        // The King Black Dragon lair ruin with its ladder down.
        k.ruin(3030, 3848, 3036, 3854, 4, RUIN_BLACK, "minecraft:polished_blackstone", 345);
        int lx = cx(3033);
        int lz = cz(3851);
        k.clear(lx - 1, g - 6, lz - 1, lx + 1, g - 1, lz + 1);
        k.fill(lx - 1, g - 7, lz - 1, lx + 1, g - 7, lz + 1, BLACKSTONE);
        k.fill(lx, g - 6, lz + 1, lx, g - 1, lz + 1, "minecraft:ladder[facing=north]");
        k.fill(lx - 2, g, lz - 2, lx + 2, g, lz - 2, PBB);
        k.set(lx, g + 1, lz - 2, "minecraft:wither_skeleton_skull");
        k.ruin(3090, 3820, 3098, 3828, 3, RUIN_BLACK, null, 346);
        k.scatter(3024, 3818, 3104, 3882, 0, "minecraft:fire", 10, 347);
        k.scatter(3024, 3818, 3104, 3882, 0, "minecraft:bone_block", 10, 348);
        return k.build();
    }

    /** The Mage Arena: a great circular stone wall around a rocky arena with three god statues. */
    static StructureDef mageArena() {
        EasternKit k = new EasternKit("mage_arena", "Mage Arena");
        int g = k.g;
        k.flat(3088, 3917, 3122, 3951, COARSE);
        int x = cx(3105);
        int z = cz(3934);
        k.disc(x, g - 1, z, 28.5, GRAVEL);
        k.ring(x, g - 1, g + 5, z, 30.5, 28.5, STONE_BRICKS);
        k.ring(x, g + 2, g + 2, z, 30.5, 28.5, CRACKED_BRICKS);
        k.ring(x, g + 6, g + 6, z, 30.5, 29.5, "minecraft:stone_brick_wall");
        k.clear(x - 1, g, z + 28, x + 1, g + 3, z + 31);
        k.fill(x - 2, g, z + 31, x - 2, g + 6, z + 31, "minecraft:chiseled_stone_bricks");
        k.fill(x + 2, g, z + 31, x + 2, g + 6, z + 31, "minecraft:chiseled_stone_bricks");
        k.patches(3092, 3921, 3118, 3947, COBBLE, 15, 351);
        k.scatter(3092, 3921, 3118, 3947, 0, COBBLE, 14, 352);
        statue(k, x, z - 8, "minecraft:quartz_block", "minecraft:white_wool", "minecraft:gold_block");
        statue(k, x - 7, z + 5, "minecraft:mossy_stone_bricks", "minecraft:green_wool", "minecraft:moss_block");
        statue(k, x + 7, z + 5, "minecraft:red_nether_bricks", "minecraft:red_wool", "minecraft:black_wool");
        k.disc(x, g - 1, z, 3.5, "minecraft:chiseled_stone_bricks");
        // Braziers on the rim, cracked flagstones and scorch marks from the duels.
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4 + Math.PI / 8;
            int px = x + (int) Math.round(Math.cos(ang) * 26);
            int pz = z + (int) Math.round(Math.sin(ang) * 26);
            k.fill(px, g, pz, px, g + 1, pz, "minecraft:chiseled_stone_bricks");
            k.set(px, g + 2, pz, a % 2 == 0 ? "minecraft:campfire" : "minecraft:soul_campfire");
        }
        k.d.weather(x - 28, g - 1, z - 28, x + 28, g - 1, z + 28, GRAVEL, "minecraft:black_concrete_powder", 20, 353);
        k.d.weather(x - 31, g, z - 31, x + 31, g + 5, z + 31, STONE_BRICKS, MOSSY_BRICKS, 50, 354);
        return k.build();
    }

    /** A simple robed statue on a plinth. */
    private static void statue(EasternKit k, int x, int z, String base, String robe, String head) {
        int g = k.g;
        k.fill(x - 1, g, z - 1, x + 1, g, z + 1, base);
        k.fill(x, g + 1, z, x, g + 3, z, robe);
        k.set(x - 1, g + 3, z, robe);
        k.set(x + 1, g + 3, z, robe);
        k.set(x, g + 4, z, head);
    }

    /** The Wilderness Agility Course: walled yard with pipe, rope swing pit, stepping stones, log balance, rocks. */
    static StructureDef agilityCourse() {
        EasternKit k = new EasternKit("wilderness_agility_course", "Wilderness Agility Course");
        int g = k.g;
        k.flat(2988, 3914, 3010, 3966, COARSE);
        String cap = "minecraft:stone_brick_wall";
        k.wall(2990, 3916, 3008, 3916, 5, STONE_BRICKS, cap);
        k.wall(2990, 3965, 3008, 3965, 5, STONE_BRICKS, cap);
        k.wall(2990, 3916, 2990, 3965, 5, STONE_BRICKS, cap);
        k.wall(3008, 3916, 3008, 3965, 5, STONE_BRICKS, cap);
        k.wall(2990, 3930, 3008, 3930, 5, STONE_BRICKS, cap);
        k.gate(2997, 3916, 2999, 3916, 5);
        k.gate(3003, 3930, 3005, 3930, 4);
        k.floor(2991, 3917, 3007, 3929, GRAVEL);
        int[] lodge = k.hall(2992, 3919, 2996, 3925, 1, WILD, "east");
        k.set(lodge[0] + 1, g, lodge[1] + 1, "runecraft:agility_ticket_dispenser");
        // Obstacle pipe (north from the inner gate).
        int[] p = r(3004, 3931, 3004, 3940);
        k.fill(p[0] - 1, g - 1, p[1], p[2] + 1, g + 2, p[3], COBBLE);
        k.clear(p[0], g, p[1], p[2], g + 1, p[3]);
        // Rope swing over a spiked pit.
        int[] pit = r(2998, 3942, 3007, 3946);
        k.clear(pit[0], g - 4, pit[1], pit[2], g - 1, pit[3]);
        k.fill(pit[0], g - 5, pit[1], pit[2], g - 5, pit[3], STONE_BRICKS);
        k.fill(pit[0], g - 4, pit[1], pit[2], g - 4, pit[3], "minecraft:pointed_dripstone");
        int sx = (pit[0] + pit[2]) / 2;
        k.fill(sx - 4, g + 6, (pit[1] + pit[3]) / 2, sx + 4, g + 6, (pit[1] + pit[3]) / 2, "minecraft:spruce_log[axis=x]");
        k.fill(sx, g, (pit[1] + pit[3]) / 2, sx, g + 5, (pit[1] + pit[3]) / 2, "minecraft:chain");
        k.fill(sx - 4, g, (pit[1] + pit[3]) / 2, sx - 4, g + 5, (pit[1] + pit[3]) / 2, "minecraft:spruce_log");
        // Stepping stones across lava.
        int[] lv = r(2992, 3950, 3006, 3956);
        k.fill(lv[0], g - 2, lv[1], lv[2], g - 2, lv[3], "minecraft:magma_block");
        k.fill(lv[0], g - 1, lv[1], lv[2], g - 1, lv[3], LAVA);
        for (int x = lv[0] + 1; x < lv[2]; x += 3) {
            k.set(x, g - 1, (lv[1] + lv[3]) / 2, "minecraft:cobblestone");
            k.set(x, g, (lv[1] + lv[3]) / 2, "minecraft:cobblestone_slab");
        }
        // Log balance over a gap.
        int[] lg = r(2992, 3958, 3006, 3962);
        k.clear(lg[0], g - 4, lg[1], lg[2], g - 1, lg[3]);
        k.fill(lg[0], g - 5, lg[1], lg[2], g - 5, lg[3], "minecraft:water");
        k.fill(lg[0], g - 1, (lg[1] + lg[3]) / 2, lg[2], g - 1, (lg[1] + lg[3]) / 2, "minecraft:spruce_log[axis=x]");
        // Rock climb on the west side.
        int[] rc = r(2991, 3933, 2994, 3940);
        k.fill(rc[0], g, rc[1], rc[2], g + 3, rc[1] + 1, COBBLE);
        for (int j = 0; j < 4; j++) {
            k.fill(rc[0] + 1, g + j, rc[1] + 5 - j, rc[2] - 1, g + j, rc[1] + 5 - j, "minecraft:cobblestone_stairs[facing=north]");
        }
        return k.build();
    }

    /** The Deserted Keep: a ruined castle keep with broken towers and the lever room. */
    static StructureDef desertedKeep() {
        EasternKit k = new EasternKit("deserted_keep", "Deserted Keep");
        int g = k.g;
        k.flat(3142, 3936, 3168, 3962, COARSE);
        k.ruin(3145, 3939, 3165, 3959, 9, RUIN, COBBLE, 361);
        k.ruin(3150, 3944, 3160, 3954, 6, RUIN, "minecraft:spruce_planks", 362);
        for (int[] t : new int[][]{{3145, 3939}, {3165, 3939}, {3145, 3959}, {3165, 3959}}) {
            k.b.tTower(t[0], t[1], 3, 6 + (t[0] + t[1]) % 5, MOSSY_BRICKS, COBBLE);
        }
        int lx = bx(3155);
        int lz = bz(3949);
        k.fill(lx - 1, g, lz - 1, lx + 1, g + 2, lz - 1, MOSSY_BRICKS);
        k.set(lx, g + 1, lz, "minecraft:lever[face=wall,facing=south]");
        k.scatter(3146, 3940, 3164, 3958, 0, COBWEB, 8, 363);
        return k.build();
    }

    /** The Resource Area: walled enclosure with valuable rocks, a furnace and an anvil. */
    static StructureDef resourceArea() {
        EasternKit k = new EasternKit("resource_area", "Resource Area");
        int g = k.g;
        k.flat(3172, 3920, 3198, 3948, GRAVEL);
        String cap = "minecraft:stone_brick_wall";
        k.wall(3174, 3922, 3196, 3922, 6, STONE_BRICKS, cap);
        k.wall(3174, 3946, 3196, 3946, 6, STONE_BRICKS, cap);
        k.wall(3174, 3922, 3174, 3946, 6, STONE_BRICKS, cap);
        k.wall(3196, 3922, 3196, 3946, 6, STONE_BRICKS, cap);
        k.gate(3184, 3946, 3186, 3946, 5);
        k.fill(bx(3184), g, bz(3946), bx(3186) + 1, g, bz(3946), "minecraft:iron_bars");
        k.clear(bx(3185), g, bz(3946), bx(3185) + 1, g + 2, bz(3946) + 1);
        String[] rocks = {"runecraft:runite_rock", "runecraft:runite_rock", "runecraft:adamantite_rock", "runecraft:adamantite_rock",
                "runecraft:adamantite_rock", "runecraft:mithril_rock", "runecraft:mithril_rock", "runecraft:coal_rock",
                "runecraft:coal_rock", "runecraft:coal_rock", "runecraft:gold_rock", "runecraft:iron_rock", "runecraft:iron_rock"};
        int i = 0;
        for (int ty = 3926; ty <= 3940 && i < rocks.length; ty += 4) {
            for (int tx = 3177; tx <= 3193 && i < rocks.length; tx += 4) {
                k.set(bx(tx), g, bz(ty), rocks[i++]);
            }
        }
        k.set(bx(3192), g, bz(3943), FURNACE);
        k.set(bx(3190), g, bz(3943), ANVIL);
        k.set(bx(3178), g, bz(3943), "runecraft:yew_tree_log");
        k.set(bx(3181), g, bz(3943), "runecraft:magic_tree_log");
        k.tower(3174, 3922, 2, 8, STONE_BRICKS, COBBLE, null, null);
        k.tower(3196, 3922, 2, 8, STONE_BRICKS, COBBLE, null, null);
        return k.build();
    }

    /** The Demonic Ruins: a blackstone temple ring with broken demonic pillars on netherrack. */
    static StructureDef demonicRuins() {
        EasternKit k = new EasternKit("demonic_ruins", "Demonic Ruins");
        int g = k.g;
        k.flat(3278, 3873, 3302, 3897, NETHERRACK);
        k.patches(3278, 3873, 3302, 3897, BLACKSTONE, 20, 371);
        int x = cx(3290);
        int z = cz(3885);
        k.disc(x, g - 1, z, 18.5, "minecraft:polished_blackstone");
        k.ring(x, g - 1, g - 1, z, 18.5, 16.5, PBB);
        Random rnd = new Random(372);
        for (int a = 0; a < 12; a++) {
            double ang = a * Math.PI / 6;
            int px = x + (int) Math.round(Math.cos(ang) * 14);
            int pz = z + (int) Math.round(Math.sin(ang) * 14);
            k.fill(px, g, pz, px, g + 1 + rnd.nextInt(6), pz, rnd.nextBoolean() ? BLACKSTONE : CRACKED_PBB);
        }
        k.ruin(3286, 3881, 3294, 3889, 5, RUIN_BLACK, "minecraft:crying_obsidian", 373);
        k.fill(x - 1, g, z - 1, x + 1, g, z + 1, "minecraft:obsidian");
        k.set(x, g + 1, z, "minecraft:soul_campfire");
        k.scatter(3279, 3874, 3301, 3896, 0, "minecraft:fire", 6, 374);
        return k.build();
    }

    /** Scorpia's pit: a crater rimmed with rocks, a ladder down into a webbed hollow. */
    static StructureDef scorpiasPit() {
        EasternKit k = new EasternKit("scorpias_pit", "Scorpia's pit").replaces("scorpias_cave");
        int g = k.g;
        k.flat(3226, 3934, 3242, 3950, COARSE);
        int x = cx(3234);
        int z = cz(3942);
        k.ring(x, g, g + 1, z, 9.5, 7.5, "minecraft:stone");
        k.ring(x, g + 2, g + 2, z, 9.5, 8.5, "minecraft:cobblestone");
        for (int y = g - 7; y <= g - 1; y++) {
            k.disc(x, y, z, 7.5 - (g - 1 - y) * 0.5, AIR);
        }
        k.disc(x, g - 8, z, 5.0, GRAVEL);
        k.fill(x, g - 7, z - 5, x, g - 1, z - 5, "minecraft:ladder[facing=south]");
        k.fill(x, g - 7, z - 6, x, g - 1, z - 6, "minecraft:stone");
        k.scatter(3230, 3938, 3238, 3946, -7, COBWEB, 10, 381);
        k.scatter(3230, 3938, 3238, 3946, -7, "minecraft:bone_block", 4, 382);
        return k.build();
    }

    /** Rogues' Castle: walled castle with four towers, keep and the rogues' chests. */
    static StructureDef roguesCastle() {
        EasternKit k = new EasternKit("rogues_castle", "Rogues' Castle").replaces("rogues_castle_chests");
        int g = k.g;
        k.flat(3254, 3898, 3300, 3948, COARSE);
        k.patches(3254, 3898, 3300, 3948, GRAVEL, 20, 391);
        String cap = "minecraft:stone_brick_wall";
        k.wall(3276, 3922, 3296, 3922, 8, CRACKED_BRICKS, cap);
        k.wall(3276, 3944, 3296, 3944, 8, CRACKED_BRICKS, cap);
        k.wall(3276, 3922, 3276, 3944, 8, CRACKED_BRICKS, cap);
        k.wall(3296, 3922, 3296, 3944, 8, CRACKED_BRICKS, cap);
        k.gate(3285, 3922, 3287, 3922, 6);
        for (int[] t : new int[][]{{3276, 3922}, {3296, 3922}, {3276, 3944}, {3296, 3944}}) {
            k.tower(t[0], t[1], 4, 14, STONE_BRICKS, COBBLE, null, "minecraft:stone_bricks");
        }
        k.floor(3277, 3923, 3295, 3943, COBBLE);
        int[] keep = k.hall(3280, 3930, 3292, 3941, 2, WILD, "south");
        k.set(keep[0] + 3, g + 4, keep[1] + 2, "runecraft:rogues_castle_chest");
        k.set(keep[2] - 3, g + 4, keep[1] + 2, "runecraft:rogues_castle_chest");
        k.set(keep[0] + 3, g, keep[1] + 2, "minecraft:chest[facing=south]");
        k.fill(keep[0] + 2, g, keep[3] - 3, keep[2] - 2, g, keep[3] - 3, "minecraft:spruce_slab[type=top]");
        k.set(keep[0] + 5, g, keep[1] + 4, "minecraft:spruce_slab[type=top]");
        k.set(keep[0] + 5, g + 4, keep[3] - 2, "minecraft:red_carpet");
        k.set(keep[0] + 6, g + 4, keep[3] - 2, "minecraft:red_carpet");
        k.campfire(bx(3290), bz(3926));
        k.set(bx(3279), g, bz(3925), "minecraft:barrel[facing=up]");
        k.set(bx(3280), g, bz(3925), "minecraft:barrel[facing=up]");
        // Red banners, a weathered keep, stacked loot crates and a broken cart in the bailey.
        for (int[] t : new int[][]{{3276, 3922}, {3296, 3922}, {3276, 3944}, {3296, 3944}}) {
            k.set(cx(t[0]), g + 10, cz(t[1]) + 4, "minecraft:red_wall_banner[facing=south]");
        }
        k.buttresses(keep, 8, STONE_BRICKS, "minecraft:stone_brick_stairs", 3);
        k.d.crates(bx(3283), bz(3926), 392);
        k.d.crates(bx(3292), bz(3940), 393);
        k.dressTown(3277, 3923, 3295, 3943, "wild", 6, 394);
        return k.build();
    }

    /** The grounds south-west of Rogues' Castle: ruined stables and a graveyard. */
    static StructureDef roguesCastleGrounds() {
        EasternKit k = new EasternKit("rogues_castle_grounds", "Rogues' Castle grounds").replaces("rogues_castle");
        int g = k.g;
        k.ruin(3258, 3904, 3272, 3916, 4, RUIN, "minecraft:spruce_planks", 395);
        k.set(bx(3262), g, bz(3908), "minecraft:hay_block");
        k.set(bx(3263), g, bz(3908), "minecraft:hay_block");
        k.graveyard(3258, 3918, 3270, 3922, 396);
        k.deadTree(3256, 3900, 6);
        k.deadTree(3274, 3902, 5);
        return k.build();
    }

    /** The Chaos Temple (level 38) with the Chaos Altar. */
    static StructureDef chaosAltarTemple() {
        EasternKit k = new EasternKit("chaos_altar_temple", "Chaos Temple (Chaos Altar)");
        int g = k.g;
        k.flat(2944, 3810, 2962, 3830, COARSE);
        Style s = new Style(PBB, "minecraft:polished_blackstone", "minecraft:deepslate_tile", true, null, BLACKSTONE,
                "minecraft:red_stained_glass_pane", SOUL_LANTERN, "minecraft:polished_blackstone_slab[type=top]",
                "minecraft:blackstone_stairs", "minecraft:red_carpet");
        int[] t = k.hall(2946, 3814, 2958, 3826, 2, s, "south");
        k.openUp(t, 2);
        int mx = (t[0] + t[2]) / 2;
        k.fill(mx - 2, g, t[1] + 1, mx + 3, g, t[1] + 4, "minecraft:gilded_blackstone");
        k.set(mx, g + 1, t[1] + 2, ALTAR);
        k.set(mx + 1, g + 1, t[1] + 2, ALTAR);
        k.set(mx - 2, g + 1, t[1] + 1, "minecraft:candle[candles=4,lit=true]");
        k.set(mx + 3, g + 1, t[1] + 1, "minecraft:candle[candles=4,lit=true]");
        k.fill(mx, g, t[1] + 5, mx + 1, g, t[3] - 1, "minecraft:red_carpet");
        for (int z = t[1] + 6; z < t[3] - 1; z += 3) {
            k.fill(t[0] + 2, g, z, mx - 2, g, z, "minecraft:blackstone_stairs[facing=south]");
            k.fill(mx + 3, g, z, t[2] - 2, g, z, "minecraft:blackstone_stairs[facing=south]");
        }
        k.set(mx, g + 6, t[1] + 1, "minecraft:red_wall_banner[facing=south]");
        k.set(mx + 1, g + 6, t[1] + 1, "minecraft:red_wall_banner[facing=south]");
        // Blackstone buttresses, braziers at the door and bone-strewn approach.
        k.buttresses(t, 8, BLACKSTONE, "minecraft:blackstone_stairs", 3);
        int dmx = (t[0] + t[2]) / 2;
        for (int x : new int[]{dmx - 3, dmx + 4}) {
            k.set(x, g, t[3] + 2, "minecraft:polished_blackstone_bricks");
            k.set(x, g + 1, t[3] + 2, "minecraft:soul_campfire");
        }
        k.dressTown(2944, 3810, 2962, 3830, "wild", 5, 303);
        return k.build();
    }

    /** The Chaos Fanatic's broken altar circle. */
    static StructureDef chaosFanaticRuins() {
        EasternKit k = new EasternKit("chaos_fanatic_ruins", "Chaos Fanatic's altar").replaces("chaos_fanatic_altar");
        int g = k.g;
        k.flat(2972, 3840, 2986, 3854, COARSE);
        int x = cx(2979);
        int z = cz(3847);
        k.disc(x, g - 1, z, 7.5, "minecraft:polished_blackstone");
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            int px = x + (int) Math.round(Math.cos(ang) * 6);
            int pz = z + (int) Math.round(Math.sin(ang) * 6);
            k.pillar(px, pz, 2 + a % 3, BLACKSTONE, a % 2 == 0 ? "minecraft:skeleton_skull" : null);
        }
        k.fill(x - 1, g, z, x + 1, g, z, CRACKED_PBB);
        k.set(x, g + 1, z, "minecraft:candle[candles=4,lit=true]");
        return k.build();
    }

    /** The Pirates' Hideout: walled compound on the north coast with Larran's big chest. */
    static StructureDef piratesHideout() {
        EasternKit k = new EasternKit("pirates_hideout", "Pirates' Hideout").replaces("pirates_hideout");
        int g = k.g;
        k.flat(3030, 3940, 3054, 3964, COARSE);
        String cap = "minecraft:cobblestone_wall";
        k.wall(3034, 3944, 3050, 3944, 5, MOSSY_COBBLE, cap);
        k.wall(3034, 3962, 3050, 3962, 5, MOSSY_COBBLE, cap);
        k.wall(3034, 3944, 3034, 3962, 5, MOSSY_COBBLE, cap);
        k.wall(3050, 3944, 3050, 3962, 5, MOSSY_COBBLE, cap);
        k.gate(3041, 3944, 3043, 3944, 4);
        k.floor(3035, 3945, 3049, 3961, "minecraft:spruce_planks");
        int[] hut = k.house(3037, 3952, 3047, 3959, 1, WILD, "south");
        k.set((hut[0] + hut[2]) / 2, g, hut[1] + 1, "runecraft:larrans_big_chest");
        k.fill(hut[0] + 1, g, hut[1] + 1, hut[0] + 3, g, hut[1] + 1, "minecraft:barrel[facing=up]");
        k.set(bx(3036), g, bz(3946), "minecraft:barrel[facing=up]");
        k.set(bx(3037), g, bz(3946), "minecraft:barrel[facing=up]");
        k.set(bx(3048), g, bz(3946), "minecraft:chest[facing=north]");
        k.campfire(bx(3042), bz(3948));
        k.fill(bx(3040), g, bz(3949), bx(3040), g + 4, bz(3949), "minecraft:spruce_fence");
        k.set(bx(3040), g + 5, bz(3949), "minecraft:black_wool");
        // The Jolly Roger, stacked cargo and a cannon by the gate.
        k.set(bx(3040) + 1, g + 5, bz(3949), "minecraft:black_wool");
        k.set(bx(3040) + 1, g + 4, bz(3949), "minecraft:white_wool");
        k.d.crates(bx(3046), bz(3950), 556);
        k.set(bx(3043), g, bz(3945), "minecraft:polished_blackstone_bricks");
        k.set(bx(3043), g + 1, bz(3945), "minecraft:black_concrete");
        k.set(bx(3044), g + 1, bz(3945), "minecraft:anvil[facing=east]");
        return k.build();
    }

    // ------------------------------------------------------------------ boss ruins & scattered ruins

    /** Vet'ion's ruins: a collapsed hall with standing columns. */
    static StructureDef vetionRuins() {
        EasternKit k = new EasternKit("vetion_ruins", "Vet'ion's ruins").replaces("vetion_ruins");
        k.flat(3208, 3780, 3228, 3800, COARSE);
        k.ruin(3210, 3782, 3226, 3798, 7, RUIN, STONE_BRICKS, 401);
        int[] q = r(3210, 3782, 3226, 3798);
        for (int x = q[0] + 4; x < q[2] - 2; x += 6) {
            k.pillar(x, q[1] + 6, 4 + (x % 3), "minecraft:chiseled_stone_bricks", "minecraft:stone_brick_slab");
            k.pillar(x, q[3] - 6, 3 + (x % 4), "minecraft:chiseled_stone_bricks", null);
        }
        k.scatter(3211, 3783, 3225, 3797, 0, "minecraft:bone_block", 6, 402);
        return k.build();
    }

    /** The Crazy Archaeologist's dig: ruins with dig pits and scaffolding. */
    static StructureDef crazyArchaeologistDig() {
        EasternKit k = new EasternKit("crazy_archaeologist_dig", "Crazy Archaeologist's dig").replaces("crazy_archaeologist_ruins");
        int g = k.g;
        k.flat(2968, 3694, 2988, 3710, COARSE);
        k.ruin(2970, 3696, 2986, 3708, 5, RUIN, null, 411);
        for (int[] d : new int[][]{{2974, 3700}, {2981, 3704}, {2979, 3698}}) {
            int x = bx(d[0]);
            int z = bz(d[1]);
            k.clear(x, g - 3, z, x + 2, g - 1, z + 2);
            k.fill(x, g - 4, z, x + 2, g - 4, z + 2, GRAVEL);
            k.fill(x - 1, g, z - 1, x - 1, g + 3, z - 1, "minecraft:scaffolding");
        }
        k.tentT(2982, 3696, 2985, 3700, "minecraft:white_wool");
        return k.build();
    }

    /** Ruins scattered across the Wilderness, each on a small levelled site. */
    static StructureDef scatteredRuins() {
        EasternKit k = new EasternKit("wilderness_ruins", "Wilderness ruins");
        int[][] sites = {{2970, 3610}, {3013, 3590}, {3081, 3655}, {3192, 3593}, {3300, 3625}, {3126, 3790}, {3306, 3800},
                {3020, 3790}, {3176, 3758}, {3364, 3910}, {2990, 3880}, {3245, 3705}};
        long seed = 420;
        for (int[] s : sites) {
            int x = s[0];
            int y = s[1];
            k.flat(x - 6, y - 6, x + 6, y + 6, COARSE);
            k.patches(x - 6, y - 6, x + 6, y + 6, GRAVEL, 6, seed++);
            int w = 3 + (int) (seed % 3);
            k.ruin(x - w, y - w, x + w, y + w, 3 + (int) (seed % 4), (seed % 2 == 0) ? RUIN : RUIN_BLACK, null, seed++);
            k.pillar(bx(x - w - 2), bz(y + w + 2), 2 + (int) (seed % 3), MOSSY_BRICKS, null);
            k.pillar(bx(x + w + 2), bz(y - w - 2), 3, CRACKED_BRICKS, "minecraft:stone_brick_slab");
            k.scatter(x - w, y - w, x + w, y + w, 0, COBWEB, 2, seed++);
            k.scatter(x - 6, y - 6, x + 6, y + 6, 0, "minecraft:dead_bush", 4, seed++);
        }
        return k.build();
    }
}
