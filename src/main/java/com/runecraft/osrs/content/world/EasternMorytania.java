package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.EasternKit.*;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * Morytania at real OSRS tiles: Paterdomus, Canifis, the Slayer Tower, Port Phasmatys with the Ectofuntus
 * and its docks, the Barrows, Mort'ton, Burgh de Rott, Ver Sinhaza, Darkmeyer with the Hallowed Sepulchre,
 * Fenkenstrain's Castle and Slepe.
 */
final class EasternMorytania {
    private static final String ANDESITE = "minecraft:polished_andesite";
    private static final String MUD = "minecraft:mud";
    private static final String DEEP_TILE_ROOF = "minecraft:deepslate_tiles";
    private static final String[] RUIN_STONE = {MOSSY_COBBLE, COBBLE, MOSSY_BRICKS, CRACKED_BRICKS, STONE_BRICKS};
    private static final String[] RUIN_DARK = {COB_DEEP, DEEP_BRICKS, CRACKED_DEEP, MOSSY_COBBLE, "minecraft:deepslate_tiles"};

    /** Grey stone church style (Paterdomus, Mort'ton temple). */
    static final Style TEMPLE = new Style(STONE_BRICKS, ANDESITE, "minecraft:stone_brick", true, "minecraft:spruce_door",
            MOSSY_BRICKS, "minecraft:light_blue_stained_glass_pane", "minecraft:lantern", "minecraft:spruce_slab[type=top]",
            "minecraft:spruce_stairs", "minecraft:white_carpet");

    private EasternMorytania() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(paterdomus());
        out.add(canifis());
        out.add(slayerTower());
        out.add(portPhasmatys());
        out.add(phasmatysDocks());
        out.add(barrows());
        out.add(mortton());
        out.add(morttonTemple());
        out.add(burghDeRott());
        out.add(verSinhaza());
        out.add(verSinhazaBridge());
        out.add(darkmeyer());
        out.add(hallowedSepulchre());
        out.add(fenkenstrain());
        out.add(slepe());
    }

    // ------------------------------------------------------------------ Paterdomus

    /** The temple of Paterdomus on the River Salve: nave with pews, altar, Drezel's floor, steeple, graveyard. */
    static StructureDef paterdomus() {
        EasternKit k = new EasternKit("paterdomus", "Paterdomus").replaces("paterdomus");
        int g = k.g;
        k.flat(3402, 3476, 3426, 3500, GRASS);
        k.patches(3402, 3476, 3426, 3500, PODZOL, 18, 101);
        int[] t = k.hall(3406, 3480, 3420, 3494, 2, TEMPLE, "west");
        int mz = (t[1] + t[3]) / 2;
        k.fill(t[0] + 1, g, mz, t[2] - 5, g, mz + 1, "minecraft:red_carpet");
        for (int x = t[0] + 4; x <= t[2] - 9; x += 3) {
            k.fill(x, g, t[1] + 3, x, g, mz - 2, "minecraft:spruce_stairs[facing=west]");
            k.fill(x, g, mz + 3, x, g, t[3] - 3, "minecraft:spruce_stairs[facing=west]");
        }
        k.fill(t[2] - 4, g, t[1] + 3, t[2] - 1, g, t[3] - 3, ANDESITE);
        k.set(t[2] - 2, g + 1, mz, ALTAR);
        k.set(t[2] - 2, g + 1, mz + 1, "minecraft:lectern[facing=west]");
        k.set(t[2] - 2, g + 1, t[1] + 4, "minecraft:candle[candles=3,lit=true]");
        k.set(t[2] - 2, g + 1, t[3] - 4, "minecraft:candle[candles=3,lit=true]");
        k.set(t[2] - 1, g + 2, mz, "minecraft:light_blue_wall_banner[facing=west]");
        k.set(t[2] - 6, g - 1, t[3] - 2, "minecraft:spruce_trapdoor[half=top]");
        // Drezel's upper floor.
        k.set(t[2] - 3, g + 4, mz, ALTAR);
        k.fill(t[2] - 2, g + 4, t[1] + 2, t[2] - 1, g + 5, t[1] + 2, "minecraft:bookshelf");
        k.set(t[0] + 3, g + 4, t[3] - 1, "minecraft:white_carpet");
        k.fill(t[0] + 3, g + 4, t[3] - 2, t[0] + 4, g + 4, t[3] - 2, "minecraft:blue_carpet");
        // Steeple with a bell.
        k.tower(3420, 3494, 3, 32, STONE_BRICKS, ANDESITE, null, DEEP_TILE_ROOF);
        k.towerPassage(3420, 3494, -1, 1, g, 3);
        k.towerPassage(3420, 3494, -1, 1, g + 4, 3);
        k.set(cx(3420), g + 30, cz(3494), "minecraft:bell[attachment=ceiling]");
        for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}}) {
            k.fill(cx(3420) + o[0], g + 27, cz(3494) + o[1], cx(3420) + o[0], g + 29, cz(3494) + o[1], AIR);
        }
        // Graveyard and fence.
        k.graveyard(3407, 3496, 3419, 3499, 102);
        k.fence(3404, 3500, 3424, 3500, "minecraft:spruce_fence");
        k.floor(3402, 3487, 3405, 3488, GRAVEL);
        k.deadTree(3404, 3478, 6);
        k.deadTree(3423, 3478, 5);
        // Buttresses along the nave, a lych-gate in the graveyard fence and the river landing.
        k.buttresses(t, 7, STONE_BRICKS, "minecraft:stone_brick_stairs", 6);
        int lg = bx(3414);
        int lz = bz(3500);
        k.clear(lg, g, lz, lg + 1, g, lz + 1);
        k.fill(lg - 1, g, lz, lg - 1, g + 2, lz, "minecraft:spruce_log");
        k.fill(lg + 2, g, lz, lg + 2, g + 2, lz, "minecraft:spruce_log");
        k.fill(lg - 2, g + 3, lz, lg + 3, g + 3, lz, "minecraft:spruce_stairs[facing=south]");
        k.fill(lg - 2, g + 3, lz + 1, lg + 3, g + 3, lz + 1, "minecraft:spruce_stairs[facing=north]");
        k.set(lg, g + 2, lz, "minecraft:lantern[hanging=true]");
        k.d.weather(t[0], g, t[1], t[2], g + 7, t[3], STONE_BRICKS, MOSSY_BRICKS, 25, 103);
        k.d.overgrowth(bx(3402), bz(3500), bx(3426), bz(3476), 40, 104);
        return k.build();
    }

    // ------------------------------------------------------------------ Canifis

    /** Canifis: dark timber houses around the village green, pub, bank, Mazchna, shops and the herb patch. */
    static StructureDef canifis() {
        EasternKit k = new EasternKit("canifis", "Canifis").replaces("CANIFIS");
        int g = k.g;
        k.flat(3468, 3462, 3522, 3514, PODZOL);
        k.patches(3468, 3462, 3522, 3514, COARSE, 30, 111);
        k.patches(3468, 3462, 3522, 3514, MUD, 12, 112);
        k.floor(3483, 3476, 3507, 3500, GRAVEL);
        k.floor(3485, 3478, 3505, 3498, GRASS);
        k.floor(3494, 3500, 3496, 3501, GRAVEL);
        // North side: the pub, general store, a house.
        int[] pub = k.hall(3486, 3502, 3495, 3510, 2, MORY, "south");
        k.counterX(pub[0] + 2, pub[2] - 6, pub[1] + 3, g, "minecraft:dark_oak_slab[type=top]", null);
        k.fill(pub[0] + 2, g, pub[1] + 1, pub[2] - 6, g, pub[1] + 1, "minecraft:barrel[facing=up]");
        k.set(pub[2] - 2, g, pub[1] + 1, RANGE);
        for (int x = pub[0] + 3; x < pub[2] - 2; x += 5) {
            k.set(x, g, pub[3] - 3, "minecraft:dark_oak_slab[type=top]");
            k.set(x - 1, g, pub[3] - 3, "minecraft:dark_oak_stairs[facing=west]");
            k.set(x + 1, g, pub[3] - 3, "minecraft:dark_oak_stairs[facing=east]");
        }
        k.shop(3497, 3503, 3503, 3509, 1, MORY, "south", GENERAL);
        k.house(3476, 3502, 3483, 3508, 2, MORY, "south");
        // East side: the bank, Rufus' meat emporium, a house.
        int[] bank = k.hall(3509, 3475, 3516, 3484, 1, MORY_STONE, "west");
        k.boothsZ(bank[2] - 3, bank[1] + 2, bank[3] - 2, g, POL_DEEP);
        k.fill(bank[0] + 1, g, (bank[1] + bank[3]) / 2, bank[2] - 4, g, (bank[1] + bank[3]) / 2 + 1, "minecraft:gray_carpet");
        int[] rufus = k.shop(3510, 3488, 3516, 3494, 1, MORY, "west", null);
        k.set(rufus[2] - 1, g, rufus[1] + 2, "minecraft:smoker[facing=west]");
        k.fill(rufus[2] - 1, g + 2, rufus[1] + 3, rufus[2] - 1, g + 2, rufus[3] - 2, "minecraft:chain");
        k.house(3510, 3497, 3516, 3503, 2, MORY, "west");
        // South side: a house, Mazchna the slayer master, Sbott the tanner.
        k.house(3486, 3466, 3492, 3473, 1, MORY, "north");
        k.shop(3494, 3466, 3501, 3473, 1, MORY_STONE, "north", "runecraft:slayer_master");
        k.shop(3503, 3466, 3509, 3472, 1, MORY, "north", TANNER);
        // West side: houses and the fur trader.
        k.house(3471, 3478, 3479, 3485, 2, MORY, "east");
        k.shop(3471, 3489, 3479, 3496, 1, MORY, "east", "runecraft:fur_stall");
        k.house(3472, 3466, 3479, 3472, 1, MORY, "east");
        // The green: fenced herb patch, dead trees, lantern posts.
        int hx = bx(3498);
        int hz = bz(3482);
        k.b.walls(hx - 2, g, hz - 2, hx + 2, g, hz + 2, "minecraft:dark_oak_fence");
        k.set(hx, g, hz + 2, "minecraft:dark_oak_fence_gate[facing=south]");
        k.fill(hx - 1, g - 1, hz - 1, hx + 1, g - 1, hz + 1, "minecraft:farmland");
        k.set(hx, g, hz, "runecraft:herb_patch");
        k.deadTree(3489, 3483, 7);
        k.deadTree(3501, 3494, 6);
        k.deadTree(3490, 3495, 5);
        k.set(bx(3495), g, bz(3488), "minecraft:mossy_cobblestone");
        for (int[] p : new int[][]{{3483, 3476}, {3507, 3476}, {3483, 3500}, {3507, 3500}, {3495, 3476}, {3483, 3488},
                {3507, 3488}}) {
            int x = bx(p[0]);
            int z = bz(p[1]);
            k.fill(x, g, z, x, g + 2, z, "minecraft:dark_oak_fence");
            k.set(x, g + 3, z, SOUL_LANTERN);
        }
        k.scatter(3468, 3462, 3522, 3514, 0, "minecraft:dead_bush", 16, 113);
        k.scatter(3485, 3478, 3505, 3498, 0, "minecraft:fern", 14, 114);
        // The pub sign (a black banner over the door), a muddy cart track round the green, picket fences
        // and back-garden clutter; lamp posts along the lanes and gloomy props on free ground.
        k.set((pub[0] + pub[2]) / 2 - 2, g + 3, pub[3] + 1, "minecraft:black_wall_banner[facing=south]");
        k.d.weather(bx(3483), g - 1, bz(3500), bx(3507) + 1, g - 1, bz(3476) + 1, GRAVEL, MUD, 30, 115);
        k.d.weather(bx(3483), g - 1, bz(3500), bx(3507) + 1, g - 1, bz(3476) + 1, GRAVEL, COARSE, 20, 116);
        k.d.flowerBed(bx(3486), bz(3497), bx(3488) + 1, bz(3496) + 1, "minecraft:dark_oak_planks", 117);
        k.set(bx(3492), g, bz(3486), "minecraft:dark_oak_fence");
        k.set(bx(3492), g + 1, bz(3486), "minecraft:carved_pumpkin[facing=south]");
        k.take(bx(3483), bz(3500), bx(3507) + 1, bz(3476) + 1);
        k.dressTown(3468, 3462, 3522, 3514, "mory", 26, 118);
        return k.build();
    }

    // ------------------------------------------------------------------ Slayer Tower

    /** The Slayer Tower: a huge grey three-storey keep with corner turrets, gargoyles and a rooftop. */
    static StructureDef slayerTower() {
        EasternKit k = new EasternKit("slayer_tower", "Slayer Tower").replaces("slayer_tower_roof");
        int g = k.g;
        k.flat(3408, 3530, 3448, 3570, COARSE);
        k.patches(3408, 3530, 3448, 3570, GRAVEL, 20, 121);
        int[] q = r(3412, 3534, 3444, 3566);
        int[] in = r(3420, 3542, 3436, 3558);
        k.fill(q[0], g - 1, q[1], q[2], g - 1, q[3], ANDESITE);
        k.clear(q[0] + 1, g, q[1] + 1, q[2] - 1, g + 20, q[3] - 1);
        k.b.walls(q[0], g, q[1], q[2], g + 20, q[3], STONE_BRICKS);
        for (int y : new int[]{g, g + 6, g + 13, g + 20}) {
            k.b.walls(q[0], y, q[1], q[2], y, q[3], "minecraft:andesite");
        }
        k.fill(q[0] + 1, g + 6, q[1] + 1, q[2] - 1, g + 6, q[3] - 1, ANDESITE);
        k.fill(q[0] + 1, g + 13, q[1] + 1, q[2] - 1, g + 13, q[3] - 1, ANDESITE);
        k.fill(q[0] + 1, g + 20, q[1] + 1, q[2] - 1, g + 20, q[3] - 1, STONE_BRICKS);
        k.crenels(q, g + 21, STONE_BRICKS);
        k.slits(q, g + 2, 6, "minecraft:iron_bars");
        k.slits(q, g + 8, 6, "minecraft:iron_bars");
        k.slits(q, g + 15, 6, "minecraft:iron_bars");
        // Inner chamber walls through all storeys with doorways on every side.
        k.b.walls(in[0], g, in[1], in[2], g + 19, in[3], CRACKED_BRICKS);
        int imx = (in[0] + in[2]) / 2;
        int imz = (in[1] + in[3]) / 2;
        for (int y : new int[]{g, g + 7, g + 14}) {
            k.clear(imx, y, in[1], imx + 1, y + 2, in[1]);
            k.clear(imx, y, in[3], imx + 1, y + 2, in[3]);
            k.clear(in[0], y, imz, in[0], y + 2, imz + 1);
            k.clear(in[2], y, imz, in[2], y + 2, imz + 1);
        }
        // Grand entrance on the south side with a raised portcullis.
        int mx = (q[0] + q[2]) / 2;
        k.clear(mx - 1, g, q[3], mx + 2, g + 4, q[3]);
        k.fill(mx - 1, g + 4, q[3], mx + 2, g + 4, q[3], "minecraft:iron_bars");
        k.fill(mx - 2, g, q[3] + 1, mx - 2, g + 6, q[3] + 1, "minecraft:andesite");
        k.fill(mx + 3, g, q[3] + 1, mx + 3, g + 6, q[3] + 1, "minecraft:andesite");
        k.fill(mx - 1, g, q[3] + 1, mx + 2, g + 5, q[3] + 1, AIR);
        k.floor(3426, 3526, 3429, 3533, GRAVEL);
        k.fill(mx - 1, g, q[3] - 1, mx + 2, g, in[3] + 1, "minecraft:gray_carpet");
        // Ladders to every storey and the roof.
        k.fill(q[0] + 1, g, q[1] + 1, q[0] + 1, g + 20, q[1] + 1, "minecraft:ladder[facing=south]");
        k.fill(q[2] - 1, g, q[1] + 1, q[2] - 1, g + 13, q[1] + 1, "minecraft:ladder[facing=south]");
        // Gloomy interior.
        for (int y : new int[]{0, 7, 14}) {
            k.scatter(3413, 3535, 3443, 3565, y, COBWEB, 20, 122 + y);
            k.scatter(3413, 3535, 3443, 3565, y, "minecraft:bone_block", 6, 140 + y);
            k.scatter(3413, 3535, 3443, 3565, y + 5, "minecraft:chain", 12, 160 + y);
        }
        for (int y : new int[]{g + 4, g + 11, g + 18}) {
            for (int x = q[0] + 4; x < q[2] - 2; x += 10) {
                k.set(x, y, q[1] + 4, SOUL_LANTERN + "[hanging=true]");
                k.set(x, y, q[3] - 4, SOUL_LANTERN + "[hanging=true]");
            }
            k.set(imx, y, imz, SOUL_LANTERN + "[hanging=true]");
        }
        k.set(imx, g, imz, "minecraft:skeleton_skull");
        k.fill(imx - 1, g, imz - 1, imx + 1, g, imz - 1, "minecraft:chiseled_stone_bricks");
        // Corner turrets with cone roofs.
        for (int[] t : new int[][]{{3412, 3566, 1, 1}, {3444, 3566, -1, 1}, {3412, 3534, 1, -1}, {3444, 3534, -1, -1}}) {
            k.tower(t[0], t[1], 5, 34, STONE_BRICKS, ANDESITE, null, DEEP_TILE_ROOF);
            for (int y : new int[]{g, g + 7, g + 14, g + 21}) {
                k.towerPassage(t[0], t[1], t[2], t[3], y, 5);
            }
        }
        // Gargoyles on the roof.
        for (int[] c : new int[][]{{mx - 8, q[3] - 1}, {mx + 9, q[3] - 1}, {mx - 8, q[1] + 1}, {mx + 9, q[1] + 1}}) {
            k.fill(c[0], g + 21, c[1], c[0], g + 22, c[1], "minecraft:blackstone");
            k.set(c[0], g + 23, c[1], "minecraft:wither_skeleton_skull");
        }
        k.deadTree(3409, 3531, 7);
        k.deadTree(3447, 3532, 6);
        // The belfry: a second stage rising from the roof with barred lancets, a steep slate roof and
        // gargoyles at its eaves, reached through an arch from the roof walk.
        int[] sp = r(3424, 3546, 3432, 3554);
        k.clear(sp[0] + 1, g + 21, sp[1] + 1, sp[2] - 1, g + 29, sp[3] - 1);
        k.b.walls(sp[0], g + 21, sp[1], sp[2], g + 30, sp[3], STONE_BRICKS);
        k.d.ringReplace(sp, g + 21, g + 21, STONE_BRICKS, "minecraft:andesite");
        k.d.ringReplace(sp, g + 27, g + 27, STONE_BRICKS, "minecraft:andesite");
        k.slits(sp, g + 23, 4, "minecraft:iron_bars");
        k.fill(sp[0] + 1, g + 30, sp[1] + 1, sp[2] - 1, g + 30, sp[3] - 1, ANDESITE);
        k.b.roof(sp[0], sp[1], sp[2], sp[3], g + 31, "minecraft:deepslate_tile");
        int spm = (sp[0] + sp[2]) / 2;
        k.clear(spm, g + 21, sp[3], spm + 1, g + 23, sp[3]);
        k.fill(spm, g + 21, sp[3] - 1, spm + 1, g + 21, (sp[1] + sp[3]) / 2, "minecraft:gray_carpet");
        k.set(spm, g + 28, (sp[1] + sp[3]) / 2, "minecraft:bell[attachment=ceiling]");
        k.set(spm, g + 25, (sp[1] + sp[3]) / 2, SOUL_LANTERN + "[hanging=true]");
        k.fill(spm, g + 26, (sp[1] + sp[3]) / 2, spm, g + 27, (sp[1] + sp[3]) / 2, "minecraft:chain");
        for (int[] c : new int[][]{{sp[0] - 1, sp[1] - 1}, {sp[2] + 1, sp[1] - 1}, {sp[0] - 1, sp[3] + 1}, {sp[2] + 1, sp[3] + 1}}) {
            k.fill(c[0], g + 30, c[1], c[0], g + 31, c[1], "minecraft:blackstone");
            k.set(c[0], g + 32, c[1], "minecraft:wither_skeleton_skull");
        }
        // Buttresses down the faces of the keep, a sooty plinth, moss creeping up from the ground.
        for (int x = q[0] + 9; x <= q[2] - 9; x += 8) {
            if (x >= mx - 3 && x <= mx + 4) {
                continue;
            }
            for (int[] f : new int[][]{{q[1] - 1, -1}, {q[3] + 1, 1}}) {
                k.fill(x, g - 1, f[0], x, g + 15, f[0], STONE_BRICKS);
                k.set(x, g + 16, f[0], "minecraft:stone_brick_stairs[facing=" + (f[1] < 0 ? "south" : "north") + "]");
                k.fill(x, g - 1, f[0] + f[1], x, g + 6, f[0] + f[1], STONE_BRICKS);
                k.set(x, g + 7, f[0] + f[1], "minecraft:stone_brick_stairs[facing=" + (f[1] < 0 ? "south" : "north") + "]");
            }
        }
        for (int z = q[1] + 9; z <= q[3] - 9; z += 8) {
            for (int[] f : new int[][]{{q[0] - 1, -1}, {q[2] + 1, 1}}) {
                k.fill(f[0], g - 1, z, f[0], g + 15, z, STONE_BRICKS);
                k.set(f[0], g + 16, z, "minecraft:stone_brick_stairs[facing=" + (f[1] < 0 ? "east" : "west") + "]");
                k.fill(f[0] + f[1], g - 1, z, f[0] + f[1], g + 6, z, STONE_BRICKS);
                k.set(f[0] + f[1], g + 7, z, "minecraft:stone_brick_stairs[facing=" + (f[1] < 0 ? "east" : "west") + "]");
            }
        }
        k.d.weather(q[0], g, q[1], q[2], g + 4, q[3], STONE_BRICKS, MOSSY_BRICKS, 60, 123);
        k.d.weather(q[0], g + 5, q[1], q[2], g + 20, q[3], STONE_BRICKS, CRACKED_BRICKS, 50, 124);
        k.graveyard(3409, 3530, 3418, 3532, 125);
        k.graveyard(3438, 3530, 3447, 3532, 126);
        return k.build();
    }

    // ------------------------------------------------------------------ Port Phasmatys

    /** Port Phasmatys: walled ghost town with bank, pub, shops and the Ectofuntus temple to the north-west. */
    static StructureDef portPhasmatys() {
        EasternKit k = new EasternKit("port_phasmatys", "Port Phasmatys").replaces("PORT_PHASMATYS");
        int g = k.g;
        k.flat(3650, 3458, 3702, 3528, PODZOL);
        k.patches(3650, 3458, 3702, 3528, COARSE, 25, 131);
        k.patches(3650, 3458, 3702, 3528, MUD, 15, 132);
        String cap = "minecraft:mossy_stone_brick_wall";
        k.wall(3656, 3462, 3700, 3462, 6, MOSSY_BRICKS, cap);
        k.wall(3656, 3512, 3700, 3512, 6, MOSSY_BRICKS, cap);
        k.wall(3656, 3462, 3656, 3512, 6, MOSSY_BRICKS, cap);
        k.wall(3700, 3462, 3700, 3512, 6, MOSSY_BRICKS, cap);
        k.gate(3656, 3484, 3656, 3487, 6);
        k.tfill(3656, 3484, 3656, 3487, 3, 4, "minecraft:lime_stained_glass_pane");
        k.gate(3700, 3500, 3700, 3503, 6);
        k.gate(3662, 3512, 3665, 3512, 6);
        for (int[] t : new int[][]{{3656, 3462}, {3700, 3462}, {3656, 3512}, {3700, 3512}, {3656, 3481}, {3656, 3490}}) {
            k.tower(t[0], t[1], 3, 9, MOSSY_BRICKS, "minecraft:spruce_planks", null, DEEP_TILE_ROOF);
        }
        k.floor(3657, 3500, 3699, 3503, MOSSY_COBBLE);
        k.floor(3678, 3463, 3680, 3511, MOSSY_COBBLE);
        k.floor(3657, 3484, 3677, 3487, MOSSY_COBBLE);
        k.floor(3662, 3504, 3665, 3511, MOSSY_COBBLE);
        k.floor(3681, 3504, 3697, 3510, "minecraft:mossy_stone_bricks");
        // Bank (south), pub, general store, ghost houses.
        int[] bank = k.hall(3683, 3464, 3691, 3471, 1, PHASMATYS, "north");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[3] - 3, g, "minecraft:spruce_slab[type=top]");
        int[] pub = k.hall(3664, 3492, 3672, 3499, 2, PHASMATYS, "east");
        k.counterZ(pub[0] + 3, pub[1] + 2, pub[3] - 2, g, "minecraft:spruce_slab[type=top]", null);
        k.fill(pub[0] + 1, g, pub[1] + 2, pub[0] + 1, g, pub[3] - 2, "minecraft:barrel[facing=up]");
        k.set(pub[2] - 3, g, pub[1] + 3, "minecraft:spruce_slab[type=top]");
        k.set(pub[2] - 3, g, pub[3] - 3, "minecraft:spruce_slab[type=top]");
        k.shop(3682, 3490, 3688, 3496, 1, PHASMATYS, "north", GENERAL);
        k.house(3660, 3466, 3666, 3472, 1, PHASMATYS, "north");
        k.house(3668, 3466, 3675, 3472, 2, PHASMATYS, "north");
        k.house(3692, 3476, 3698, 3482, 1, PHASMATYS, "west");
        k.house(3692, 3486, 3698, 3492, 2, PHASMATYS, "west");
        k.house(3667, 3504, 3674, 3510, 1, PHASMATYS, "south");
        k.house(3660, 3476, 3667, 3482, 1, PHASMATYS, "east");
        k.house(3669, 3476, 3676, 3482, 1, PHASMATYS, "north");
        k.house(3683, 3476, 3689, 3482, 2, PHASMATYS, "west");
        k.deadTree(3684, 3507, 6);
        k.deadTree(3694, 3508, 5);
        k.set(bx(3689), g, bz(3507), "minecraft:mossy_stone_bricks");
        k.set(bx(3689), g + 1, bz(3507), SOUL_LANTERN);
        k.scatter(3657, 3463, 3699, 3511, 0, COBWEB, 12, 133);
        // The Ectofuntus temple with its grinder tower.
        k.floor(3660, 3513, 3664, 3514, MOSSY_COBBLE);
        int[] e = k.hall(3654, 3515, 3666, 3526, 2, PHASMATYS, "south");
        int ex = cx(3660);
        int ez = cz(3521);
        k.tower(3660, 3521, 3, 24, MOSSY_BRICKS, "minecraft:spruce_planks", null, DEEP_TILE_ROOF);
        k.clear(ex - 1, g, ez + 3, ex + 1, g + 2, ez + 3);
        k.disc(ex, g - 1, ez, 2.0, "minecraft:slime_block");
        k.ring(ex, g, g, ez, 2.5, 1.5, "minecraft:lime_stained_glass");
        k.set(ex, g, ez - 2, "minecraft:ladder[facing=south]");
        k.set(ex, g, ez, ALTAR);
        k.disc(ex, g + 21, ez, 1.5, "minecraft:spruce_planks");
        k.set(ex, g + 21, ez, "minecraft:lime_stained_glass");
        k.set(ex, g + 22, ez - 1, "minecraft:grindstone[face=floor,facing=north]");
        k.set(ex, g + 22, ez + 1, "minecraft:hopper");
        k.fill(e[0] + 1, g, e[1] + 1, e[0] + 1, g, e[3] - 1, "minecraft:bone_block");
        k.set(e[2] - 1, g, e[1] + 1, "minecraft:cauldron");
        k.set(e[2] - 1, g, e[3] - 1, "minecraft:water_cauldron[level=3]");
        // The Ectofuntus glows: sea lanterns under the green glass ring, slime-green windows up the grinder
        // tower, buttresses along the temple and ectoplasm buckets by the door.
        k.ring(ex, g - 1, g - 1, ez, 2.5, 1.5, "minecraft:sea_lantern");
        for (int y = g + 5; y < g + 22; y += 5) {
            k.fill(ex + 3, y, ez, ex + 3, y + 1, ez, "minecraft:lime_stained_glass_pane");
            k.fill(ex - 3, y, ez, ex - 3, y + 1, ez, "minecraft:lime_stained_glass_pane");
        }
        k.buttresses(e, 8, MOSSY_COBBLE, "minecraft:mossy_cobblestone_stairs");
        k.set(e[0] + 3, g, e[3] + 1, "minecraft:water_cauldron[level=2]");
        k.set(e[2] - 3, g, e[3] + 1, "minecraft:cauldron");
        // The square by the pub: a statue of Necrovarus on a plinth, benches and a notice board.
        int sx = bx(3689);
        int sz = bz(3504);
        k.fill(sx - 1, g, sz - 1, sx + 1, g, sz + 1, "minecraft:chiseled_stone_bricks");
        k.fill(sx, g + 1, sz, sx, g + 3, sz, "minecraft:light_gray_wool");
        k.set(sx - 1, g + 3, sz, "minecraft:light_gray_wool");
        k.set(sx + 1, g + 3, sz, "minecraft:light_gray_wool");
        k.set(sx, g + 4, sz, "minecraft:skeleton_skull[rotation=8]");
        k.d.bench(bx(3683), bz(3509), true, "north", "minecraft:spruce");
        k.d.bench(bx(3694), bz(3509), true, "north", "minecraft:spruce");
        k.fill(bx(3697), g, bz(3505), bx(3697), g + 1, bz(3505), "minecraft:spruce_fence");
        k.fill(bx(3697), g + 2, bz(3505), bx(3697) + 1, g + 2, bz(3505), "minecraft:spruce_planks");
        k.set(bx(3697), g + 2, bz(3505) + 1, "minecraft:spruce_wall_sign[facing=south]");
        k.d.weather(bx(3657), g - 1, bz(3511), bx(3699) + 1, g - 1, bz(3463) + 1, MOSSY_COBBLE, GRAVEL, 40, 134);
        k.take(bx(3681) - 1, bz(3510) - 1, bx(3697) + 2, bz(3504) + 2);
        k.take(bx(3654) - 3, bz(3526) - 3, bx(3666) + 3, bz(3513) + 2);
        k.dressTown(3656, 3462, 3700, 3512, "mory", 22, 135);
        return k.build();
    }

    /** The Port Phasmatys docks: jetty, pier on posts, task board and the moored ghost ship. */
    static StructureDef phasmatysDocks() {
        EasternKit k = new EasternKit("port_phasmatys_docks", "Port Phasmatys docks").replaces("port_phasmatys_docks");
        int g = k.g;
        int[] jet = r(3701, 3500, 3714, 3503);
        int[] pier = r(3714, 3486, 3716, 3516);
        for (int[] d : new int[][]{jet, pier}) {
            k.clear(d[0], g, d[1], d[2], g + 8, d[3]);
            k.fill(d[0], g - 1, d[1], d[2], g - 1, d[3], "minecraft:spruce_planks");
            for (int x = d[0]; x <= d[2]; x += 4) {
                k.fill(x, g - 10, d[1], x, g - 2, d[1], "minecraft:spruce_log");
                k.fill(x, g - 10, d[3], x, g - 2, d[3], "minecraft:spruce_log");
            }
            for (int z = d[1]; z <= d[3]; z += 4) {
                k.fill(d[0], g - 10, z, d[0], g - 2, z, "minecraft:spruce_log");
                k.fill(d[2], g - 10, z, d[2], g - 2, z, "minecraft:spruce_log");
            }
        }
        k.fill(jet[0], g, jet[1], jet[2] - 1, g, jet[1], "minecraft:spruce_fence");
        k.fill(jet[0], g, jet[3], jet[2] - 1, g, jet[3], "minecraft:spruce_fence");
        k.fill(pier[0], g, pier[1], pier[0], g, jet[1] - 1, "minecraft:spruce_fence");
        k.fill(pier[0], g, jet[3] + 1, pier[0], g, pier[3], "minecraft:spruce_fence");
        k.set(jet[0] + 6, g, jet[1] + 1, "runecraft:port_phasmatys_task_board");
        k.set(jet[0] + 8, g, jet[1] + 1, "minecraft:barrel[facing=up]");
        k.set(jet[0] + 9, g, jet[1] + 1, "minecraft:barrel[facing=up]");
        k.set(pier[0] + 1, g, pier[1] + 2, "minecraft:barrel[facing=up]");
        for (int z = pier[1] + 4; z < pier[3]; z += 10) {
            k.fill(pier[2], g, z, pier[2], g + 2, z, "minecraft:spruce_fence");
            k.set(pier[2], g + 3, z, SOUL_LANTERN);
        }
        // The ghost ship, moored east of the pier.
        int x1 = pier[2] + 2;
        int x2 = x1 + 8;
        int z1 = bz(3512);
        int z2 = bz(3490);
        ship(k, x1, z1, x2, z2);
        k.fill(pier[2] + 1, g, (z1 + z2) / 2, x1 - 1, g, (z1 + z2) / 2 + 1, "minecraft:spruce_slab[type=top]");
        // Mooring posts with rope along the jetty, nets drying on the rail, cargo, lanterns on poles and two
        // rowing boats tied up south of the jetty.
        k.d.mooringX(jet[0] + 1, jet[2] - 3, jet[3] + 1, g - 1, 4);
        for (int x = jet[0] + 2; x < jet[2] - 2; x += 5) {
            k.set(x, g + 1, jet[1], COBWEB);
        }
        k.d.crates(jet[0] + 2, jet[1] + 1, 136);
        k.d.crates(pier[0] + 1, pier[3] - 3, 137);
        k.set(pier[0] + 1, g, pier[3] - 6, "minecraft:chest[facing=east]");
        k.d.rowboat(jet[0] + 3, jet[3] + 3, g - 2, "minecraft:dark_oak");
        k.d.rowboat(jet[0] + 9, jet[3] + 4, g - 2, "minecraft:spruce");
        k.fill(jet[0], g, jet[1] + 1, jet[0], g + 2, jet[1] + 1, "minecraft:spruce_fence");
        k.set(jet[0], g + 3, jet[1] + 1, SOUL_LANTERN);
        return k.build();
    }

    /** A tattered ghost ship hull along z (bow to the north). */
    private static void ship(EasternKit k, int x1, int z1, int x2, int z2) {
        int g = k.g;
        String hull = DARK_PLANKS;
        int mx = (x1 + x2) / 2;
        k.clear(x1 - 1, g + 2, z1 - 5, x2 + 1, g + 24, z2 + 1);
        for (int i = 0; i <= 5; i++) {
            int y = g - 4 + i;
            int in = Math.max(0, 3 - i);
            k.clear(x1 + in + 1, y, z1 + in + 1, x2 - in - 1, y, z2 - in - 1);
            k.b.walls(x1 + in, y, z1 + in, x2 - in, y, z2 - in, hull);
            for (int j = 1; j <= 4 - in / 2; j++) {
                if (x1 + in + j <= x2 - in - j) {
                    k.fill(x1 + in + j, y, z1 + in - j, x2 - in - j, y, z1 + in - j, hull);
                }
            }
        }
        k.fill(x1 + 1, g - 4, z1 + 4, x2 - 1, g - 4, z2 - 4, hull);
        k.fill(x1 + 1, g + 1, z1 + 1, x2 - 1, g + 1, z2 - 1, "minecraft:spruce_planks");
        k.b.walls(x1, g + 2, z1, x2, g + 2, z2, "minecraft:dark_oak_fence");
        k.clear(x1, g + 2, (z1 + z2) / 2, x1, g + 2, (z1 + z2) / 2 + 1);
        // Stern castle with the captain's cabin.
        k.b.walls(x1 + 1, g + 2, z2 - 9, x2 - 1, g + 4, z2 - 1, hull);
        k.fill(x1 + 1, g + 5, z2 - 9, x2 - 1, g + 5, z2 - 1, "minecraft:spruce_planks");
        k.b.walls(x1, g + 6, z2 - 9, x2, g + 6, z2, "minecraft:dark_oak_fence");
        k.clear(mx, g + 2, z2 - 9, mx, g + 3, z2 - 9);
        k.set(mx, g + 4, z2 - 5, SOUL_LANTERN + "[hanging=true]");
        k.set(x2 - 2, g + 2, z2 - 2, "minecraft:chest[facing=west]");
        k.fill(mx, g + 6, z2 - 3, mx, g + 6, z2 - 3, "minecraft:dark_oak_fence");
        // Masts, yards and tattered sails.
        for (int mz : new int[]{z1 + 8, (z1 + z2) / 2, z2 - 13}) {
            k.fill(mx, g + 2, mz, mx, g + 21, mz, DARK_LOG);
            k.fill(x1 - 1, g + 18, mz, x2 + 1, g + 18, mz, "minecraft:dark_oak_log[axis=x]");
            k.fill(x1 - 1, g + 10, mz, x2 + 1, g + 10, mz, "minecraft:dark_oak_log[axis=x]");
            k.fill(x1, g + 11, mz + 1, x2, g + 17, mz + 1, "minecraft:light_gray_wool");
            k.set(x1 + 1, g + 12, mz + 1, AIR);
            k.set(x1 + 2, g + 13, mz + 1, COBWEB);
            k.fill(x2 - 2, g + 11, mz + 1, x2 - 1, g + 12, mz + 1, AIR);
            k.set(mx + 2, g + 15, mz + 1, AIR);
            k.set(x1 - 1, g + 17, mz, SOUL_LANTERN + "[hanging=true]");
            k.set(x2 + 1, g + 17, mz, SOUL_LANTERN + "[hanging=true]");
        }
        k.fill(mx, g + 2, z1 - 3, mx, g + 2, z1 + 1, "minecraft:dark_oak_log[axis=z]");
        k.set(mx, g + 3, z1 - 3, "minecraft:skeleton_skull");
    }

    // ------------------------------------------------------------------ Barrows

    /** The Barrows: six burial mounds with the brothers' stones, crypt doors and the mausoleum. */
    static StructureDef barrows() {
        EasternKit k = new EasternKit("barrows", "Barrows").replaces("BARROWS");
        int g = k.g;
        k.flat(3543, 3266, 3589, 3310, PODZOL);
        k.patches(3543, 3266, 3589, 3310, COARSE, 40, 141);
        k.patches(3543, 3266, 3589, 3310, MUD, 20, 142);
        k.patches(3547, 3270, 3585, 3306, WATER, 8, 143);
        k.ruin(3545, 3268, 3587, 3308, 3, RUIN_STONE, null, 144);
        k.b.tClear(3562, 3308, 3564, 3308, 0, 4);
        k.b.tClear(3545, 3286, 3545, 3288, 0, 4);
        Object[][] brothers = {{"verac", 3557, 3298}, {"dharok", 3575, 3298}, {"ahrim", 3565, 3289}, {"guthan", 3577, 3283},
                {"torag", 3553, 3283}, {"karil", 3566, 3275}};
        for (Object[] b : brothers) {
            int x = cx((Integer) b[1]);
            int z = cz((Integer) b[2]);
            k.dome(x, z, g, 9.5, 5, "minecraft:dirt", GRASS);
            k.set(x, g + 5, z, "runecraft:" + b[0] + "_summoning_stone");
            // Crypt doorway on the south flank of the mound.
            k.clear(x - 1, g, z + 4, x + 1, g + 2, z + 10);
            k.fill(x - 1, g - 1, z + 4, x + 1, g - 1, z + 10, STONE_BRICKS);
            k.fill(x - 2, g, z + 6, x - 2, g + 3, z + 6, MOSSY_BRICKS);
            k.fill(x + 2, g, z + 6, x + 2, g + 3, z + 6, MOSSY_BRICKS);
            k.fill(x - 2, g + 3, z + 6, x + 2, g + 3, z + 6, CRACKED_BRICKS);
            k.fill(x - 1, g, z + 3, x + 1, g + 2, z + 3, CRACKED_DEEP);
            k.fill(x - 1, g, z + 4, x + 1, g + 1, z + 4, "minecraft:iron_bars");
            k.set(x - 1, g + 2, z + 5, SOUL_LANTERN + "[hanging=true]");
        }
        // The mausoleum with the chest, bank chest and altar.
        int[] m = k.hall(3555, 3270, 3560, 3275, 1, MORY_STONE, "north");
        int mmx = (m[0] + m[2]) / 2;
        k.set(mmx, g, m[3] - 2, ALTAR);
        k.set(m[0] + 1, g, m[3] - 1, BANK);
        k.set(m[2] - 1, g, m[3] - 1, "minecraft:chest[facing=north]");
        k.set(mmx + 1, g, m[3] - 1, "minecraft:skeleton_skull");
        k.set(m[0] + 1, g, m[1] + 1, "minecraft:candle[candles=4,lit=true]");
        k.set(m[2] - 1, g, m[1] + 1, "minecraft:candle[candles=4,lit=true]");
        for (int[] t : new int[][]{{3550, 3292}, {3570, 3304}, {3584, 3290}, {3549, 3274}, {3582, 3272}, {3560, 3306}, {3571, 3282}}) {
            k.deadTree(t[0], t[1], 5 + (t[0] % 3));
        }
        k.graveyard(3577, 3268, 3586, 3276, 145);
        k.scatter(3545, 3268, 3587, 3308, 0, "minecraft:dead_bush", 20, 146);
        k.scatter(3545, 3268, 3587, 3308, 0, COBWEB, 6, 147);
        return k.build();
    }

    // ------------------------------------------------------------------ Mort'ton & Burgh de Rott

    /** Mort'ton: the ruined walled town with its two surviving shops, ruins and funeral pyres. */
    static StructureDef mortton() {
        EasternKit k = new EasternKit("mort_ton", "Mort'ton").replaces("mort_ton");
        int g = k.g;
        k.flat(3472, 3268, 3512, 3306, COARSE);
        k.patches(3472, 3268, 3512, 3306, PODZOL, 20, 151);
        k.patches(3472, 3268, 3512, 3306, GRAVEL, 15, 152);
        k.ruin(3474, 3270, 3510, 3304, 6, RUIN_STONE, null, 153);
        k.b.tClear(3474, 3285, 3474, 3288, 0, 6);
        k.b.tClear(3490, 3304, 3492, 3304, 0, 6);
        k.floor(3475, 3285, 3493, 3288, GRAVEL);
        k.floor(3490, 3289, 3492, 3303, GRAVEL);
        k.shop(3478, 3292, 3484, 3298, 1, MORY, "south", GENERAL);
        k.shop(3486, 3292, 3492, 3298, 1, MORY, "south", TOOLS);
        k.ruin(3478, 3274, 3484, 3280, 4, RUIN_STONE, "minecraft:spruce_planks", 154);
        k.ruin(3486, 3274, 3492, 3279, 3, RUIN_STONE, COBBLE, 155);
        k.ruin(3505, 3292, 3509, 3300, 4, RUIN_STONE, null, 156);
        k.ruin(3505, 3274, 3509, 3282, 3, RUIN_STONE, "minecraft:spruce_planks", 157);
        k.ruin(3494, 3295, 3502, 3301, 4, RUIN_STONE, null, 158);
        k.set(bx(3486), g, bz(3281), "runecraft:funeral_pyre");
        k.set(bx(3493), g, bz(3281), "runecraft:funeral_pyre");
        k.deadTree(3477, 3301, 6);
        k.deadTree(3482, 3283, 5);
        k.deadTree(3506, 3288, 6);
        k.graveyard(3476, 3271, 3478, 3273, 159);
        k.scatter(3475, 3271, 3509, 3303, 0, COBWEB, 10, 160);
        // Plague-town clutter: barricades of planks, abandoned carts, bone heaps and smouldering pyres.
        k.d.weather(bx(3474), g - 1, bz(3304), bx(3510) + 1, g - 1, bz(3270) + 1, GRAVEL, MUD, 25, 162);
        k.dressTown(3474, 3270, 3510, 3304, "mory", 16, 163);
        return k.build();
    }

    /** The temple of Mort'ton (Shades of Mort'ton) with its sacred flame and broken roof. */
    static StructureDef morttonTemple() {
        EasternKit k = new EasternKit("mort_ton_temple", "Mort'ton temple").replaces("mg_mortton");
        int g = k.g;
        int[] t = k.hall(3494, 3280, 3504, 3292, 2, TEMPLE, "west");
        k.openUp(t, 2);
        k.clear(t[0] + 4, g + 7, t[1] + 3, t[0] + 8, g + 14, t[1] + 8);
        k.b.replace(t[0], g, t[1], t[2], g + 8, t[3], STONE_BRICKS, MOSSY_BRICKS);
        int mz = (t[1] + t[3]) / 2;
        k.fill(t[2] - 4, g, mz - 2, t[2] - 1, g, mz + 2, ANDESITE);
        k.set(t[2] - 2, g + 1, mz, "minecraft:soul_campfire");
        k.set(t[2] - 2, g + 1, mz - 2, "runecraft:shades_of_mortton_entrance");
        k.set(t[2] - 2, g + 1, mz + 2, "runecraft:shade_chest");
        for (int x = t[0] + 3; x <= t[2] - 7; x += 3) {
            k.fill(x, g, t[1] + 2, x, g, mz - 2, "minecraft:spruce_stairs[facing=west]");
            k.fill(x, g, mz + 2, x, g, t[3] - 2, "minecraft:spruce_stairs[facing=west]");
        }
        k.set(t[0] + 3, g, t[1] + 2, "minecraft:cobblestone_slab");
        k.scatter(3495, 3281, 3503, 3291, 0, COBWEB, 6, 161);
        return k.build();
    }

    /** Burgh de Rott: ruined walls, a well square, the bank and general store, broken houses. */
    static StructureDef burghDeRott() {
        EasternKit k = new EasternKit("burgh_de_rott", "Burgh de Rott").replaces("burgh_de_rott");
        int g = k.g;
        k.flat(3478, 3192, 3514, 3236, COARSE);
        k.patches(3478, 3192, 3514, 3236, MUD, 20, 171);
        k.ruin(3480, 3194, 3512, 3234, 5, RUIN_STONE, null, 172);
        k.b.tClear(3496, 3194, 3498, 3194, 0, 5);
        k.b.tClear(3480, 3205, 3480, 3207, 0, 5);
        k.floor(3494, 3202, 3502, 3208, COBBLE);
        k.floor(3496, 3195, 3498, 3201, GRAVEL);
        k.well(cx(3498), cz(3205), COBBLE, "minecraft:spruce_slab");
        int[] bank = k.hall(3490, 3210, 3497, 3216, 1, MORY_STONE, "south");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[1] + 3, g, POL_DEEP);
        k.shop(3500, 3210, 3506, 3215, 1, MORY, "south", GENERAL);
        k.house(3483, 3207, 3489, 3213, 1, MORY, "east");
        k.ruin(3483, 3197, 3489, 3203, 4, RUIN_STONE, null, 173);
        k.ruin(3483, 3220, 3490, 3227, 5, RUIN_STONE, "minecraft:spruce_planks", 174);
        k.ruin(3505, 3197, 3510, 3204, 3, RUIN_STONE, null, 175);
        k.ruin(3500, 3220, 3508, 3228, 4, RUIN_STONE, COBBLE, 176);
        k.ruin(3492, 3222, 3497, 3230, 3, RUIN_STONE, null, 177);
        k.deadTree(3487, 3216, 6);
        k.deadTree(3509, 3209, 5);
        k.scatter(3481, 3195, 3511, 3233, 0, COBWEB, 8, 178);
        k.dressTown(3480, 3194, 3512, 3234, "mory", 14, 179);
        return k.build();
    }

    // ------------------------------------------------------------------ Ver Sinhaza & Darkmeyer

    /** Ver Sinhaza: the vampyre plaza with its blood fountain, bank and the Theatre of Blood. */
    static StructureDef verSinhaza() {
        EasternKit k = new EasternKit("ver_sinhaza", "Ver Sinhaza").replaces("ver_sinhaza");
        int g = k.g;
        k.flat(3634, 3194, 3692, 3238, COARSE);
        k.floor(3638, 3212, 3666, 3234, PBB);
        k.floor(3640, 3214, 3664, 3232, "minecraft:polished_blackstone");
        int fx = cx(3652);
        int fz = cz(3223);
        k.ring(fx, g - 1, g, fz, 4.5, 3.5, PBB);
        k.disc(fx, g - 2, fz, 3.5, PBB);
        k.disc(fx, g - 1, fz, 3.5, LAVA);
        k.fill(fx, g - 1, fz, fx, g + 3, fz, "minecraft:red_nether_bricks");
        k.set(fx, g + 4, fz, "minecraft:wither_skeleton_skull");
        int[] bank = k.hall(3642, 3226, 3650, 3233, 1, DARKMEYER, "south");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[1] + 3, g, "minecraft:polished_blackstone_slab[type=top]");
        k.house(3655, 3226, 3663, 3233, 2, DARKMEYER, "south");
        // The Theatre of Blood.
        int[] th = k.hall(3668, 3206, 3690, 3234, 3, DARKMEYER, "west");
        k.openUp(th, 3);
        int tmz = (th[1] + th[3]) / 2;
        k.set(th[0] + 3, g, tmz, "runecraft:theatre_of_blood_entrance");
        k.fill(th[0] + 1, g, tmz, th[2] - 2, g, tmz + 1, "minecraft:red_carpet");
        k.fill(th[0], g + 4, tmz - 2, th[0], g + 9, tmz + 3, "minecraft:red_stained_glass");
        for (int z = th[1] + 4; z < th[3] - 2; z += 6) {
            k.fill(th[0] + 6, g, z, th[0] + 6, g + 10, z, BLACKSTONE);
            k.fill(th[2] - 6, g, z, th[2] - 6, g + 10, z, BLACKSTONE);
        }
        k.fill(th[2] - 5, g, th[1] + 3, th[2] - 1, g + 1, th[3] - 3, "minecraft:red_nether_bricks");
        k.tower(3668, 3206, 3, 18, PBB, "minecraft:red_nether_bricks", null, DEEP_TILE_ROOF);
        k.tower(3668, 3234, 3, 18, PBB, "minecraft:red_nether_bricks", null, DEEP_TILE_ROOF);
        // Vyrewatch statues and lamps around the plaza.
        for (int tx = 3640; tx <= 3664; tx += 6) {
            k.pillar(bx(tx), bz(3213), 4, PBB, "minecraft:skeleton_skull");
            k.fill(bx(tx), g, bz(3235), bx(tx), g + 2, bz(3235), "minecraft:polished_blackstone_brick_wall");
            k.set(bx(tx), g + 3, bz(3235), SOUL_LANTERN);
        }
        // Crimson banners on the theatre, braziers and benches round the plaza.
        for (int z = th[1] + 3; z < th[3] - 2; z += 6) {
            k.set(th[0] - 1, g + 7, z, "minecraft:red_wall_banner[facing=west]");
        }
        k.buttresses(th, 12, PBB, "minecraft:polished_blackstone_brick_stairs", 6);
        k.d.bench(bx(3641), bz(3220), false, "east", "minecraft:crimson");
        k.d.bench(bx(3663), bz(3220), false, "west", "minecraft:crimson");
        k.take(bx(3638) - 1, bz(3234) - 1, bx(3666) + 2, bz(3212) + 2);
        k.dressTown(3634, 3194, 3692, 3238, "dark", 12, 183);
        return k.build();
    }

    /** The causeway south from Ver Sinhaza with balustrades, statues and an arch. */
    static StructureDef verSinhazaBridge() {
        EasternKit k = new EasternKit("ver_sinhaza_causeway", "Ver Sinhaza causeway").replaces("quest_ver_sinhaza");
        int g = k.g;
        k.floor(3648, 3196, 3656, 3211, PBB);
        k.floor(3650, 3196, 3654, 3211, "minecraft:polished_blackstone");
        k.tfill(3648, 3196, 3648, 3211, 0, 0, "minecraft:polished_blackstone_brick_wall");
        k.tfill(3656, 3196, 3656, 3211, 0, 0, "minecraft:polished_blackstone_brick_wall");
        for (int ty = 3198; ty <= 3210; ty += 4) {
            k.pillar(bx(3648), bz(ty), 4, PBB, "minecraft:wither_skeleton_skull");
            k.pillar(bx(3656) + 1, bz(ty), 4, PBB, "minecraft:wither_skeleton_skull");
        }
        int[] a = r(3648, 3196, 3656, 3196);
        k.fill(a[0], g, a[1], a[0] + 1, g + 6, a[3], PBB);
        k.fill(a[2] - 1, g, a[1], a[2], g + 6, a[3], PBB);
        k.fill(a[0], g + 7, a[1], a[2], g + 7, a[3], "minecraft:polished_blackstone_brick_slab");
        k.fill(a[0] + 2, g + 6, a[1], a[2] - 2, g + 6, a[3], "minecraft:polished_blackstone_brick_stairs[facing=north,half=top]");
        k.set((a[0] + a[2]) / 2, g + 5, a[1], SOUL_LANTERN + "[hanging=true]");
        return k.build();
    }

    /** Darkmeyer: the vampyre city walls, gates, towers, blood fountain, bank and gothic houses. */
    static StructureDef darkmeyer() {
        EasternKit k = new EasternKit("darkmeyer", "Darkmeyer").replaces("darkmeyer");
        int g = k.g;
        k.flat(3594, 3330, 3668, 3398, COARSE);
        k.patches(3594, 3330, 3668, 3398, "minecraft:soul_soil", 25, 181);
        String cap = "minecraft:polished_blackstone_brick_wall";
        k.wall(3594, 3332, 3666, 3332, 9, PBB, cap);
        k.wall(3594, 3396, 3666, 3396, 9, PBB, cap);
        k.wall(3594, 3332, 3594, 3396, 9, PBB, cap);
        k.wall(3666, 3332, 3666, 3396, 9, PBB, cap);
        for (int[] l : new int[][]{{3594, 3332, 3666, 3332}, {3594, 3396, 3666, 3396}, {3594, 3332, 3594, 3396}, {3666, 3332, 3666, 3396}}) {
            k.tfill(l[0], l[1], l[2], l[3], 4, 4, CRACKED_PBB);
        }
        k.gate(3594, 3362, 3594, 3365, 7);
        k.gate(3628, 3332, 3631, 3332, 7);
        for (int[] t : new int[][]{{3594, 3332}, {3666, 3332}, {3594, 3396}, {3666, 3396}, {3594, 3359}, {3594, 3368},
                {3625, 3332}, {3634, 3332}, {3630, 3396}}) {
            k.tower(t[0], t[1], 4, 14, PBB, "minecraft:red_nether_bricks", null, DEEP_TILE_ROOF);
        }
        k.floor(3595, 3362, 3665, 3365, "minecraft:polished_blackstone");
        k.floor(3628, 3333, 3631, 3395, "minecraft:polished_blackstone");
        k.floor(3626, 3358, 3633, 3369, PBB);
        int fx = bx(3630);
        int fz = bz(3364);
        k.ring(fx, g - 1, g, fz, 4.5, 3.5, PBB);
        k.disc(fx, g - 2, fz, 3.5, PBB);
        k.disc(fx, g - 1, fz, 3.5, LAVA);
        k.fill(fx, g - 1, fz, fx, g + 4, fz, "minecraft:red_nether_bricks");
        k.set(fx, g + 5, fz, "minecraft:soul_lantern");
        int[] bank = k.hall(3600, 3367, 3608, 3374, 1, DARKMEYER, "south");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[1] + 3, g, "minecraft:polished_blackstone_slab[type=top]");
        int[][] houses = {
                {3611, 3368, 3618, 3374, 0}, {3597, 3378, 3604, 3384, 1}, {3607, 3378, 3614, 3384, 3}, {3617, 3378, 3624, 3384, 3},
                {3597, 3387, 3604, 3393, 1}, {3607, 3387, 3614, 3393, 0}, {3617, 3387, 3624, 3393, 0},
                {3639, 3370, 3645, 3375, 0}, {3658, 3368, 3663, 3374, 0}, {3634, 3378, 3641, 3384, 1}, {3634, 3387, 3641, 3393, 1},
                {3659, 3378, 3664, 3385, 3}, {3645, 3390, 3655, 3394, 0},
                {3597, 3352, 3604, 3359, 1}, {3607, 3352, 3614, 3359, 2}, {3617, 3352, 3624, 3359, 2},
                {3597, 3336, 3604, 3343, 1}, {3607, 3336, 3614, 3343, 2}, {3617, 3336, 3624, 3343, 1},
                {3634, 3352, 3641, 3359, 2}, {3644, 3352, 3651, 3359, 2}, {3654, 3352, 3663, 3359, 2},
                {3634, 3336, 3641, 3343, 1}, {3644, 3336, 3651, 3343, 2}, {3654, 3336, 3663, 3343, 3}};
        String[] sides = {"south", "east", "north", "west"};
        int i = 0;
        for (int[] h : houses) {
            k.house(h[0], h[1], h[2], h[3], i++ % 3 == 0 ? 3 : 2, DARKMEYER, sides[h[4]]);
        }
        // Arboretum of dead trees and iron railings.
        k.floor(3597, 3345, 3625, 3350, "minecraft:crimson_nylium");
        for (int tx = 3599; tx <= 3623; tx += 6) {
            k.deadTree(tx, 3348, 6);
        }
        k.fence(3597, 3351, 3625, 3351, "minecraft:iron_bars");
        for (int tx = 3598; tx <= 3662; tx += 8) {
            if (tx == 3630) {
                continue;
            }
            k.fill(bx(tx), g, bz(3366), bx(tx), g + 2, bz(3366), "minecraft:polished_blackstone_brick_wall");
            k.set(bx(tx), g + 3, bz(3366), SOUL_LANTERN);
        }
        // Crimson banners on the towers, iron spikes along the wall walk, blood-red glass in the houses'
        // lancets and dressing in the streets (not inside the Hallowed Sepulchre, its own structure).
        for (int[] t : new int[][]{{3594, 3332}, {3666, 3332}, {3594, 3396}, {3666, 3396}, {3625, 3332}, {3634, 3332}}) {
            int bxx = cx(t[0]);
            int bzz = cz(t[1]);
            k.set(bxx, g + 10, bzz + 4, "minecraft:red_wall_banner[facing=south]");
            k.set(bxx, g + 10, bzz - 4, "minecraft:red_wall_banner[facing=north]");
        }
        for (int[] l : new int[][]{{3594, 3332, 3666, 3332}, {3594, 3396, 3666, 3396}}) {
            int[] q = r(l[0], l[1], l[2], l[3]);
            for (int x = q[0] + 1; x < q[2]; x += 3) {
                k.set(x, g + 9, q[1], "minecraft:iron_bars");
            }
        }
        k.take(bx(3644) - 3, bz(3388) - 3, bx(3656) + 4, bz(3376) + 4);
        k.dressTown(3594, 3332, 3666, 3396, "dark", 30, 182);
        return k.build();
    }

    /** The Hallowed Sepulchre entrance mausoleum in Darkmeyer. */
    static StructureDef hallowedSepulchre() {
        EasternKit k = new EasternKit("hallowed_sepulchre", "Hallowed Sepulchre").replaces("mg_darkmeyer_sepulchre");
        int g = k.g;
        Style crypt = new Style(PBB, "minecraft:polished_blackstone", "minecraft:deepslate_tile", true, null,
                "minecraft:gilded_blackstone", "minecraft:yellow_stained_glass_pane", SOUL_LANTERN,
                "minecraft:polished_blackstone_slab[type=top]", "minecraft:blackstone_stairs", "minecraft:red_carpet");
        int[] m = k.hall(3644, 3376, 3656, 3388, 2, crypt, "south");
        k.openUp(m, 2);
        int mx = (m[0] + m[2]) / 2;
        k.fill(mx - 2, g, m[1] + 1, mx + 3, g, m[1] + 4, "minecraft:gilded_blackstone");
        k.set(mx, g + 1, m[1] + 3, "runecraft:hallowed_sepulchre_entrance");
        k.set(mx + 2, g + 1, m[1] + 3, "runecraft:hallowed_sepulchre_rewards");
        k.set(m[0] + 2, g, m[1] + 2, "runecraft:sep_statue");
        k.set(m[2] - 2, g, m[1] + 2, "runecraft:sep_statue");
        k.set(m[0] + 2, g, m[3] - 3, "runecraft:sep_coffin");
        k.set(m[2] - 2, g, m[3] - 3, "runecraft:sep_coffin");
        k.fill(mx, g, m[1] + 5, mx + 1, g, m[3] - 1, "minecraft:red_carpet");
        k.set(m[0] - 2, g, m[3] + 2, "runecraft:sep_statue");
        k.set(m[2] + 2, g, m[3] + 2, "runecraft:sep_statue");
        return k.build();
    }

    // ------------------------------------------------------------------ Fenkenstrain's Castle & Slepe

    /** Fenkenstrain's Castle: a gloomy two-storey keep with four turrets, the lightning tower, lab and graves. */
    static StructureDef fenkenstrain() {
        EasternKit k = new EasternKit("fenkenstrain_castle", "Fenkenstrain's Castle").replaces("fenkenstrain_castle");
        int g = k.g;
        k.flat(3534, 3528, 3566, 3562, PODZOL);
        k.patches(3534, 3528, 3566, 3562, COARSE, 25, 191);
        int[] q = r(3540, 3536, 3560, 3556);
        k.fill(q[0], g - 1, q[1], q[2], g - 1, q[3], POL_DEEP);
        k.clear(q[0] + 1, g, q[1] + 1, q[2] - 1, g + 10, q[3] - 1);
        k.b.walls(q[0], g, q[1], q[2], g + 9, q[3], DEEP_BRICKS);
        k.b.walls(q[0], g + 4, q[1], q[2], g + 4, q[3], DEEP_TILES);
        k.fill(q[0] + 1, g + 4, q[1] + 1, q[2] - 1, g + 4, q[3] - 1, "minecraft:dark_oak_planks");
        k.fill(q[0] + 1, g + 9, q[1] + 1, q[2] - 1, g + 9, q[3] - 1, DEEP_BRICKS);
        k.crenels(q, g + 10, DEEP_BRICKS);
        k.slits(q, g + 1, 5, "minecraft:gray_stained_glass_pane");
        k.slits(q, g + 6, 5, "minecraft:gray_stained_glass_pane");
        int mx = (q[0] + q[2]) / 2;
        int mz = bz(3546);
        // Partitions: east-west wall through the middle, north-south in the north half.
        k.fill(q[0] + 1, g, mz, q[2] - 1, g + 8, mz, DEEP_BRICKS);
        k.fill(mx, g, q[1] + 1, mx, g + 8, mz, DEEP_BRICKS);
        for (int y : new int[]{g, g + 5}) {
            k.clear(mx - 6, y, mz, mx - 5, y + 2, mz);
            k.clear(mx + 5, y, mz, mx + 6, y + 2, mz);
            k.clear(mx, y, mz - 6, mx, y + 2, mz - 5);
        }
        // Entrance on the south side.
        k.clear(mx - 1, g, q[3], mx + 1, g + 3, q[3]);
        k.fill(mx - 2, g, q[3] + 1, mx - 2, g + 4, q[3] + 1, COB_DEEP);
        k.fill(mx + 2, g, q[3] + 1, mx + 2, g + 4, q[3] + 1, COB_DEEP);
        k.fill(mx - 1, g, q[3] - 1, mx + 1, g, mz + 1, "minecraft:red_carpet");
        k.floor(3549, 3529, 3551, 3535, GRAVEL);
        // South hall: stairs to the first floor, fireplace.
        k.stairsEast(q[0] + 2, q[3] - 3, 5, "minecraft:dark_oak_stairs", DARK_PLANKS);
        k.fill(q[2] - 1, g, mz + 4, q[2] - 1, g + 3, mz + 7, "minecraft:bricks");
        k.set(q[2] - 1, g, mz + 5, "minecraft:campfire");
        k.set(q[2] - 1, g, mz + 6, "minecraft:campfire");
        k.set(mx, g + 3, mz + 6, "minecraft:chain");
        k.set(mx, g + 2, mz + 6, SOUL_LANTERN + "[hanging=true]");
        // North-west laboratory.
        k.fill(q[0] + 2, g, q[1] + 4, q[0] + 7, g, q[1] + 5, "minecraft:dark_oak_slab[type=top]");
        k.set(q[0] + 3, g + 1, q[1] + 4, "minecraft:brewing_stand");
        k.set(q[0] + 5, g + 1, q[1] + 4, "minecraft:skeleton_skull");
        k.set(q[0] + 6, g + 1, q[1] + 5, "minecraft:redstone_torch");
        k.set(q[0] + 1, g, q[1] + 1, "minecraft:cauldron");
        k.set(q[0] + 2, g, q[1] + 1, "minecraft:water_cauldron[level=3]");
        k.fill(q[0] + 2, g, q[1] + 8, q[0] + 5, g, q[1] + 8, "minecraft:white_carpet");
        k.set(q[0] + 2, g + 1, q[1] + 8, "minecraft:skeleton_skull");
        k.fill(mx - 3, g, q[1] + 1, mx - 1, g + 1, q[1] + 1, "minecraft:lightning_rod");
        // North-east library.
        k.fill(mx + 1, g, q[1] + 1, q[2] - 1, g + 2, q[1] + 1, "minecraft:bookshelf");
        k.fill(q[2] - 1, g, q[1] + 2, q[2] - 1, g + 2, mz - 2, "minecraft:bookshelf");
        k.set(mx + 5, g, q[1] + 6, "minecraft:lectern[facing=north]");
        k.set(mx + 3, g, q[1] + 6, "minecraft:dark_oak_stairs[facing=west]");
        // Upper floor: Fenkenstrain's quarters.
        k.fill(q[0] + 2, g + 5, q[1] + 2, q[0] + 3, g + 5, q[1] + 2, "minecraft:red_carpet");
        k.set(q[0] + 1, g + 5, q[1] + 2, "minecraft:white_carpet");
        k.set(q[2] - 2, g + 5, q[1] + 2, "minecraft:chest[facing=south]");
        k.fill(q[0] + 1, g + 5, mz + 2, q[0] + 1, g + 7, q[3] - 4, "minecraft:bookshelf");
        k.set(mx, g + 7, mz + 6, SOUL_LANTERN + "[hanging=true]");
        k.set(mx, g + 7, q[1] + 6, SOUL_LANTERN + "[hanging=true]");
        // Turrets; the north-east one is the lightning tower.
        for (int[] t : new int[][]{{3540, 3556, 1, 1, 16}, {3560, 3556, -1, 1, 24}, {3540, 3536, 1, -1, 16}, {3560, 3536, -1, -1, 16}}) {
            k.tower(t[0], t[1], 4, t[4], DEEP_BRICKS, "minecraft:dark_oak_planks", null, DEEP_TILE_ROOF);
            k.towerPassage(t[0], t[1], t[2], t[3], g, 4);
            k.towerPassage(t[0], t[1], t[2], t[3], g + 5, 4);
        }
        k.fill(cx(3560), g + 30, cz(3556), cx(3560), g + 32, cz(3556), "minecraft:lightning_rod");
        // Grounds: graves, dead trees, iron railings.
        k.graveyard(3535, 3540, 3537, 3552, 192);
        k.graveyard(3562, 3540, 3565, 3554, 193);
        k.fence(3534, 3528, 3548, 3528, "minecraft:iron_bars");
        k.fence(3552, 3528, 3566, 3528, "minecraft:iron_bars");
        k.deadTree(3536, 3531, 6);
        k.deadTree(3564, 3531, 6);
        k.deadTree(3536, 3560, 7);
        k.scatter(3541, 3537, 3559, 3555, 0, COBWEB, 8, 194);
        // Buttresses, a tall chimney stack from the hall fireplace, ivy-dark weathering and a gibbet.
        k.buttresses(q, 10, DEEP_BRICKS, "minecraft:deepslate_brick_stairs", 8);
        k.fill(q[2] + 1, g, mz + 5, q[2] + 1, g + 14, mz + 6, "minecraft:bricks");
        k.set(q[2] + 1, g + 15, mz + 5, "minecraft:campfire[lit=true]");
        k.d.weather(q[0], g, q[1], q[2], g + 9, q[3], DEEP_BRICKS, CRACKED_DEEP, 40, 195);
        int gx = bx(3545);
        int gz = bz(3531);
        k.fill(gx, g, gz, gx, g + 4, gz, DARK_LOG);
        k.fill(gx + 1, g + 4, gz, gx + 2, g + 4, gz, "minecraft:dark_oak_log[axis=x]");
        k.fill(gx + 2, g + 2, gz, gx + 2, g + 3, gz, "minecraft:chain");
        k.set(gx + 2, g + 1, gz, "minecraft:skeleton_skull");
        return k.build();
    }

    /** Slepe: the Sisterhood's church over the sanctuary, cottages and a graveyard. */
    static StructureDef slepe() {
        EasternKit k = new EasternKit("slepe", "Slepe").replaces("sisterhood_sanctuary");
        int g = k.g;
        k.flat(3706, 3294, 3742, 3348, PODZOL);
        k.patches(3706, 3294, 3742, 3348, COARSE, 20, 201);
        k.floor(3718, 3296, 3721, 3346, GRAVEL);
        k.floor(3708, 3321, 3740, 3324, GRAVEL);
        int[] c = k.hall(3724, 3296, 3736, 3308, 2, MORY_STONE, "west");
        k.openUp(c, 2);
        int mz = (c[1] + c[3]) / 2;
        for (int x = c[0] + 4; x <= c[2] - 8; x += 3) {
            k.fill(x, g, c[1] + 2, x, g, mz - 2, "minecraft:spruce_stairs[facing=west]");
            k.fill(x, g, mz + 2, x, g, c[3] - 2, "minecraft:spruce_stairs[facing=west]");
        }
        k.fill(c[2] - 4, g, mz - 2, c[2] - 1, g, mz + 2, POL_DEEP);
        k.set(c[2] - 2, g + 1, mz, ALTAR);
        k.set(c[2] - 2, g + 1, mz - 2, "minecraft:candle[candles=4,lit=true]");
        k.set(c[2] - 2, g + 1, mz + 2, "minecraft:candle[candles=4,lit=true]");
        k.fill(c[0] + 1, g, mz, c[2] - 5, g, mz + 1, "minecraft:purple_carpet");
        k.set(c[0] + 3, g - 1, c[1] + 2, "minecraft:spruce_trapdoor[half=top]");
        k.tower(3736, 3308, 2, 18, DEEP_BRICKS, POL_DEEP, null, DEEP_TILE_ROOF);
        k.towerPassage(3736, 3308, -1, 1, g, 2);
        k.house(3710, 3300, 3716, 3306, 1, MORY, "east");
        k.house(3710, 3310, 3716, 3316, 2, MORY, "east");
        k.house(3723, 3313, 3729, 3319, 1, MORY, "south");
        k.house(3731, 3313, 3737, 3319, 2, MORY, "south");
        k.house(3709, 3326, 3716, 3332, 1, MORY, "east");
        k.house(3723, 3327, 3729, 3333, 1, MORY, "west");
        k.house(3731, 3327, 3738, 3333, 2, MORY, "west");
        k.house(3710, 3337, 3716, 3343, 1, MORY, "east");
        k.graveyard(3727, 3336, 3740, 3346, 202);
        k.deadTree(3724, 3343, 6);
        k.deadTree(3740, 3310, 5);
        // Buttressed church, picket fences round the cottage gardens, mud in the lanes.
        k.buttresses(c, 8, DEEP_BRICKS, "minecraft:deepslate_brick_stairs", 5);
        k.d.weather(bx(3708), g - 1, bz(3346), bx(3740) + 1, g - 1, bz(3296) + 1, GRAVEL, MUD, 30, 203);
        k.take(bx(3727), bz(3346), bx(3740) + 1, bz(3336) + 1);
        k.dressTown(3706, 3294, 3742, 3348, "mory", 18, 204);
        return k.build();
    }
}
