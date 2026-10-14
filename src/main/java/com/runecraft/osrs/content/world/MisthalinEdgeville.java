package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.world.MisthalinKit.Box;
import com.runecraft.osrs.content.world.MisthalinKit.Roof;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.MisthalinKit.*;

/**
 * Edgeville (bank, general store, furnace, Krystilia's house, the yews, the dungeon trapdoor,
 * Oziach's hut and the Monastery on the hill to the west) and Barbarian Village (the longhall,
 * the huts, Peksa's shop, the pottery, the Stronghold of Security entrance and the river).
 */
final class MisthalinEdgeville {
    private MisthalinEdgeville() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(edgeville());
        out.add(barbarianVillage());
        out.add(pottery());
    }

    static MisthalinKit.House stoneHouse(MisthalinKit k, Box b, int storeys) {
        return k.house(b).storeys(storeys).wall(STONE_BRICKS).upperWall(SPRUCE_PLANKS).corner(STRIPPED_SPRUCE)
                .beam(STRIPPED_SPRUCE).base(COBBLE).floor(SPRUCE_PLANKS).roof(Roof.SLATE).gable(SPRUCE_PLANKS);
    }

    // ------------------------------------------------------------------ Edgeville

    static StructureDef edgeville() {
        MisthalinKit k = new MisthalinKit("edgeville", "Edgeville", "EDGEVILLE");
        k.tFlatten(3074, 3462, 3118, 3515);
        k.tFlatten(3062, 3486, 3076, 3515);
        k.meadow(t(3074, 3462, 3118, 3515), 14);
        k.meadow(t(3062, 3486, 3073, 3515), 14);
        // roads
        k.tPath(3100, 3462, 3100, 3515, 2);
        k.tPath(3075, 3504, 3118, 3504, 2);
        k.tPath(3074, 3491, 3090, 3491, 2);
        bank(k);
        // general store north-west of the bank
        Box gs = t(3077, 3506, 3083, 3512);
        stoneHouse(k, gs, 1).doorAt('s', X(3080), 2, 3).build();
        Box gi = gs.inset(1);
        k.counterX(gi.x1() + 1, gi.x2() - 1, 0, gi.z1() + 4, GENERAL);
        k.shelvesX(gi.x1(), gi.x2(), 0, gi.z1(), "south");
        k.set(gi.x1(), 0, gi.z2(), BARREL).set(gi.x2(), 0, gi.z2(), "minecraft:chest[facing=west]");
        k.hang(gi.cx(), 2, gi.z2() - 2);
        // the furnace house east of the bank
        Box fh = t(3105, 3496, 3111, 3502);
        k.house(fh).storeys(1).height(5).wall(COBBLE).corner(STONE_BRICKS).base(COBBLE).floor(STONE_BRICKS)
                .roof(Roof.SLATE).doorAt('w', Z(3499), 2, 3).build();
        Box fi = fh.inset(1);
        k.fill(fi.x2(), 0, fi.z1(), fi.x2(), 3, fi.z1() + 3, "minecraft:bricks");
        k.set(fi.x2(), 0, fi.z1() + 1, FURNACE).set(fi.x2(), 0, fi.z1() + 2, "minecraft:blast_furnace[facing=west]");
        k.fill(fi.x2() - 1, 4, fi.z1() + 1, fi.x2(), 9, fi.z1() + 2, "minecraft:bricks");
        k.set(fi.x1(), 0, fi.z2(), BARREL).set(fi.x1() + 1, 0, fi.z2(), "minecraft:chest[facing=north]");
        k.set(fi.x2(), 0, fi.z2(), "minecraft:water_cauldron[level=3]");
        k.hang(fi.cx(), 3, fi.cz());
        // Krystilia's house (the Wilderness slayer master)
        Box ks = t(3105, 3508, 3111, 3514);
        stoneHouse(k, ks, 1).doorAt('w', Z(3511), 2, 3).build();
        Box ki = ks.inset(1);
        k.set(ki.cx(), 0, ki.cz(), SLAYER);
        k.set(ki.x2(), 0, ki.z1(), "minecraft:skeleton_skull").set(ki.x2(), 0, ki.z2(), "minecraft:chest[facing=west]");
        k.set(ki.x1(), 0, ki.z1(), "minecraft:red_wool").set(ki.x1() + 1, 0, ki.z1(), "minecraft:white_wool");
        k.set(ki.x2(), 2, ki.cz(), "minecraft:red_wall_banner[facing=west]");
        k.hang(ki.cx(), 2, ki.cz() + 2);
        // houses
        int[][] hs = {{3080, 3485, 3086, 3489, 1, 'e'}, {3103, 3484, 3109, 3490, 2, 'w'}, {3085, 3496, 3089, 3502, 1, 'e'},
                {3112, 3484, 3117, 3490, 1, 'w'}, {3088, 3508, 3094, 3513, 2, 's'}};
        String[] colours = {"brown", "green", "blue", "yellow", "red"};
        int i = 0;
        for (int[] h : hs) {
            Box b = t(h[0], h[1], h[2], h[3]);
            char side = (char) h[5];
            MisthalinKit.House hh = stoneHouse(k, b, h[4]);
            hh.doorAt(side, side == 'n' || side == 's' ? b.cx() : b.cz(), 2, 3).build();
            k.furnishHome(hh, side, colours[i++ % colours.length]);
        }
        // the yew trees south of the village and the dungeon trapdoor
        k.yew(X(3086), Z(3470)).yew(X(3088), Z(3476)).yew(X(3085), Z(3481));
        int tx = X(3097);
        int tz = Z(3468);
        k.fill(tx - 1, -1, tz - 1, tx + 2, -1, tz + 2, MOSSY_COBBLE);
        k.fill(tx - 2, -12, tz - 2, tx + 3, -2, tz + 3, COBBLE);
        k.clear(tx, -11, tz, tx + 1, -2, tz + 1);
        k.ladder(tx, tz + 1, "north", -11, -2);
        k.clear(tx - 1, -11, tz - 1, tx + 2, -9, tz + 2);
        k.set(tx + 1, -9, tz, LANTERN_HANG);
        k.set(tx, -1, tz + 1, "minecraft:spruce_trapdoor[half=top,facing=south]");
        k.set(tx + 1, -1, tz + 1, "minecraft:spruce_trapdoor[half=top,facing=south]");
        k.clear(tx + 1, -8, tz + 1, tx + 1, -2, tz + 1);
        k.fill(tx + 1, -8, tz + 1, tx + 1, -2, tz + 1, COBBLE);
        // trees and lamps
        for (int[] tr : new int[][]{{3078, 3466}, {3114, 3470}, {3076, 3498}, {3116, 3506}, {3094, 3464}}) {
            k.tOak(tr[0], tr[1]);
        }
        for (int y = 3466; y <= 3512; y += 9) {
            k.lampPost(X(3099), Z(y));
        }
        oziach(k);
        monastery(k);
        return k.build();
    }

    private static void bank(MisthalinKit k) {
        // Edgeville bank, door on the east side facing the main road
        Box b = t(3091, 3488, 3098, 3499);
        k.house(b).storeys(1).height(5).wall(STONE_BRICKS).corner(CHISELED).base(COBBLE).floor(POLISHED_ANDESITE)
                .roof(Roof.SLATE).doorAt('e', Z(3494), 4, 4).build();
        Box in = b.inset(1);
        k.boothsZ(in.x1() + 5, 0, in.z1() + 1, in.z2() - 1);
        k.fill(in.x1() + 5, 0, in.z1(), in.x1() + 5, 1, in.z1(), POLISHED_ANDESITE).fill(in.x1() + 5, 0, in.z2(), in.x1() + 5, 1, in.z2(), POLISHED_ANDESITE);
        k.carpet(new Box(in.x1() + 6, in.z1(), in.x2(), in.z2()), 0, "red");
        for (int z = in.z1(); z <= in.z2(); z += 2) {
            k.set(in.x1(), 0, z, "minecraft:chest[facing=east]");
            k.set(in.x1(), 0, z + 1, BARREL);
        }
        k.hang(in.x1() + 2, 3, in.cz()).hang(in.x2() - 2, 3, in.z1() + 4).hang(in.x2() - 2, 3, in.z2() - 4);
        k.floor(new Box(b.x2() + 1, Z(3494), b.x2() + 3, Z(3493) + 1), POLISHED_ANDESITE);
        k.set(b.x2() + 1, 2, Z(3494) - 1, "minecraft:wall_torch[facing=east]");
        k.set(b.x2() + 1, 2, Z(3493) + 2, "minecraft:wall_torch[facing=east]");
    }

    private static void oziach(MisthalinKit k) {
        // Oziach's hut by the Wilderness edge
        k.tFlatten(3062, 3505, 3073, 3515);
        Box b = t(3065, 3508, 3070, 3513);
        k.house(b).storeys(1).wall(SPRUCE_PLANKS).corner(SPRUCE_LOG).floor(SPRUCE_PLANKS).roof(Roof.SPRUCE)
                .doorAt('s', b.cx(), 2, 3).build();
        Box in = b.inset(1);
        k.set(in.x1(), 0, in.z1(), ANVIL);
        k.set(in.x2(), 0, in.z1(), BARREL).set(in.x2(), 0, in.z1() + 1, "minecraft:chest[facing=west]");
        k.set(in.x1(), 0, in.z2(), "minecraft:grindstone[face=floor,facing=east]");
        k.hang(in.cx(), 2, in.cz());
        k.tPath(3067, 3507, 3067, 3504, 1);
        k.tPath(3067, 3504, 3075, 3504, 2);
    }

    /**
     * Edgeville Monastery, traced from the OSRS map (mejrs layers_osrs, planes 0-1) and the shortest-path
     * ladders (3046, 3483) and (3057, 3483): a cloister of two-storey ranges around an open garden court
     * (3048-3055, 3485-3494) inside a chamfered perimeter wall (3042-3061, 3481-3499) with gateways north and
     * south. The chapel with the altar fills the north range's first floor (approx: room use from the wiki,
     * not the map).
     */
    private static void monastery(MisthalinKit k) {
        k.tFlatten(3038, 3476, 3064, 3504);
        k.meadow(t(3038, 3476, 3064, 3504), 8);
        // the perimeter wall with its north and south gateways
        Box outer = t(3042, 3481, 3061, 3499);
        Box ring = outer;
        for (int z = ring.z1(); z <= ring.z2(); z++) {
            int edge = Math.min(z - ring.z1(), ring.z2() - z);
            int cut = Math.max(0, 4 - edge);
            int x1 = ring.x1() + cut;
            int x2 = ring.x2() - cut;
            if (cut > 0 || z == ring.z1() || z == ring.z2()) {
                int run = edge == 0 ? 0 : 1;
                k.fill(x1, 0, z, x1 + run, 3, z, STONE_BRICKS).fill(x2 - run, 0, z, x2, 3, z, STONE_BRICKS);
                if (edge == 0) {
                    k.fill(x1, 0, z, x2, 3, z, STONE_BRICKS);
                }
            } else {
                k.fill(x1, 0, z, x1, 3, z, STONE_BRICKS).fill(x2, 0, z, x2, 3, z, STONE_BRICKS);
            }
        }
        k.clear(X(3050), 0, outer.z2(), X(3053) + 1, 3, outer.z2());
        k.clear(X(3046), 0, outer.z1(), X(3057) + 1, 3, outer.z1());
        k.fill(X(3046) - 1, 0, Z(3504), X(3046) - 1, 3, outer.z1(), STONE_BRICKS);
        k.fill(X(3057) + 2, 0, Z(3504), X(3057) + 2, 3, outer.z1(), STONE_BRICKS);
        k.floor(t(3047, 3500, 3056, 3504), GRAVEL);
        k.floor(t(3043, 3482, 3060, 3498), "minecraft:stone_bricks");

        // the ranges round the court: one box per block of the OSRS first floor, walls on shared tile edges
        Box north = mHull(3050, 3495, 3055, 3499);
        Box nw = mHull(3044, 3492, 3049, 3498);
        Box ne = mHull(3056, 3492, 3060, 3498);
        Box west = mHull(3044, 3485, 3047, 3491);
        Box east = mHull(3056, 3485, 3060, 3491);
        Box south = mHull(3044, 3481, 3060, 3484);
        Box[] ranges = {north, nw, ne, west, east, south};
        for (Box r : ranges) {
            k.fill(r, -3, -1, COBBLE).fill(r, 0, 9, STONE_BRICKS);
        }
        for (Box r : ranges) {
            k.walls(r, 4, 4, POLISHED_ANDESITE).walls(r, 0, 0, COBBLE);
        }
        for (Box r : ranges) {
            k.clear(r.inset(1), 0, 3).clear(r.inset(1), 5, 8);
            k.fill(r.inset(1), -1, -1, POLISHED_ANDESITE).fill(r.inset(1), 4, 4, SPRUCE_PLANKS);
        }
        for (Box r : ranges) {
            k.roof(r, 10, Roof.SLATE, STONE_BRICKS);
        }
        // doors: the gateways through the south and north ranges, into the court, and between the ranges
        mDoorZ(k, south.z2(), 3051, 0);
        mDoorZ(k, north.z1(), 3052, 0);
        for (int y : new int[]{0, 5}) {
            if (y == 0) {
                mDoorZ(k, south.z1(), 3051, 0);
                mDoorZ(k, north.z2(), 3052, 0);
                mDoorX(k, west.x2(), 3488, 0);
                mDoorX(k, east.x1(), 3488, 0);
                mDoorZ(k, nw.z2(), 3048, 0);
                mDoorZ(k, ne.z2(), 3057, 0);
            }
            mDoorZ(k, west.z2(), 3045, y);
            mDoorZ(k, east.z2(), 3058, y);
            mDoorZ(k, west.z1(), 3045, y);
            mDoorZ(k, east.z1(), 3058, y);
            mDoorX(k, north.x1(), 3497, y);
            mDoorX(k, north.x2(), 3497, y);
        }
        // windows on the outer faces and onto the court
        for (int y : new int[]{1, 6}) {
            for (int z = Z(3497); z <= Z(3482); z += 6) {
                k.replace(new Box(nw.x1(), z, nw.x1(), z + 1), y, y + 1, STONE_BRICKS, GLASS);
                k.replace(new Box(ne.x2(), z, ne.x2(), z + 1), y, y + 1, STONE_BRICKS, GLASS);
            }
            for (int x = X(3045); x <= X(3059); x += 6) {
                k.replace(new Box(x, south.z2(), x + 1, south.z2()), y, y + 1, STONE_BRICKS, GLASS);
            }
        }
        // the ladders (3046, 3483) and (3057, 3483) up to the first floor, each against a pillar
        for (int lx : new int[]{X(3046), X(3057)}) {
            k.fill(lx, 0, Z(3483) + 2, lx + 1, 3, Z(3483) + 2, CHISELED);
            k.ladder(lx, Z(3483) + 1, "north", 0, 4).ladder(lx + 1, Z(3483) + 1, "north", 0, 4);
        }

        // the garden court: grass, four ponds, trees, open to the sky
        Box court = t(3048, 3485, 3055, 3494);
        k.floor(court, GRASS);
        k.meadow(court, 5);
        for (int[] p : new int[][]{{3049, 3493}, {3054, 3493}, {3049, 3486}, {3054, 3486}}) {
            k.clear(X(p[0]), 0, Z(p[1]), X(p[0]) + 1, 0, Z(p[1]) + 1);
            k.fill(X(p[0]), -1, Z(p[1]), X(p[0]) + 1, -1, Z(p[1]) + 1, WATER);
        }
        k.floor(t(3051, 3485, 3052, 3494), GRAVEL);
        k.floor(t(3048, 3488, 3055, 3488), GRAVEL);
        k.bush(X(3050), Z(3491), OAK_LEAVES).bush(X(3053), Z(3491), OAK_LEAVES).bush(X(3050), Z(3489) + 1, OAK_LEAVES);

        // ground floor: refectory (west), kitchen (east), cloister walk (south), monks' cells north
        Box wi = west.inset(1);
        k.diningZ(wi.cx(), 0, wi.z1() + 1, wi.z2() - 1);
        Box ei = east.inset(1);
        k.set(ei.x2(), 0, ei.z1(), RANGE).set(ei.x2(), 0, ei.z1() + 1, BARREL).set(ei.x2(), 0, ei.z1() + 2, "minecraft:cauldron");
        k.table(ei.cx(), 0, ei.cz());
        Box si = south.inset(1);
        k.carpet(new Box(X(3048), si.z1() + 1, X(3055) + 1, si.z2() - 1), 0, "brown");
        for (Box r : new Box[]{nw, ne}) {
            Box in = r.inset(1);
            k.bed(in.x1(), 0, in.z1(), 's', "brown").bed(in.x1() + 3, 0, in.z1(), 's', "brown");
        }
        for (int x = X(3046); x <= X(3058); x += 6) {
            k.hang(x, 2, si.cz());
        }
        // first floor: the chapel in the north range, dormitories round the court
        Box ni = north.inset(1);
        k.set(ni.cx(), 5, ni.z1(), ALTAR);
        k.set(ni.cx() - 2, 5, ni.z1(), "minecraft:candle[candles=4,lit=true]").set(ni.cx() + 2, 5, ni.z1(), "minecraft:candle[candles=4,lit=true]");
        k.fill(ni.x1(), 4, ni.z1(), ni.x2(), 4, ni.z1() + 1, "minecraft:quartz_block");
        for (int z = ni.z1() + 3; z <= ni.z2(); z += 2) {
            k.fill(ni.x1(), 5, z, ni.cx() - 1, 5, z, "minecraft:spruce_stairs[facing=north]");
            k.fill(ni.cx() + 2, 5, z, ni.x2(), 5, z, "minecraft:spruce_stairs[facing=north]");
        }
        k.connect(north.x1() + 3, 6, north.z1(), north.x2() - 3, 7, north.z1(), "minecraft:yellow_stained_glass_pane");
        for (Box r : new Box[]{west, east, nw, ne}) {
            Box in = r.inset(1);
            k.bed(in.x1(), 5, in.z1(), 's', "brown");
            k.set(in.x2(), 5, in.z1(), "minecraft:chest[facing=west]");
            k.hang(in.cx(), 7, in.cz());
        }
        k.shelvesX(si.x1() + 3, si.x2() - 3, 5, si.z2(), "north");
        // bell turret over the south gateway
        int bx = X(3051) + 1;
        int bz = south.cz();
        k.fill(bx - 1, 14, bz - 1, bx + 1, 18, bz + 1, STONE_BRICKS);
        k.clear(bx, 15, bz - 1, bx, 17, bz + 1).clear(bx - 1, 15, bz, bx + 1, 17, bz);
        k.set(bx, 17, bz, "minecraft:bell[attachment=ceiling,facing=north]");
        k.fill(bx - 1, 19, bz - 1, bx + 1, 19, bz + 1, "minecraft:deepslate_tile_slab");
        k.tPath(3051, 3480, 3051, 3477, 2);
        k.tPath(3052, 3505, 3060, 3505, 2);
        k.tPath(3060, 3491, 3074, 3491, 2);
        k.tOak(3040, 3502).tOak(3063, 3502).tOak(3040, 3478).tOak(3063, 3478);
    }

    /** Block box of a tile rectangle whose east and south walls sit on the neighbouring tiles' edge blocks. */
    private static Box mHull(int tx1, int ty1, int tx2, int ty2) {
        Box b = t(tx1, ty1, tx2, ty2);
        return new Box(b.x1(), b.z1(), b.x2() + 1, b.z2() + 1);
    }

    private static void mDoorZ(MisthalinKit k, int z, int tx, int y) {
        k.clear(X(tx), y, z, X(tx) + 1, y + 2, z);
        k.set(X(tx), y, z, "minecraft:spruce_door[facing=south,half=lower,hinge=left]");
        k.set(X(tx) + 1, y, z, "minecraft:spruce_door[facing=south,half=lower,hinge=right]");
    }

    private static void mDoorX(MisthalinKit k, int x, int ty, int y) {
        k.clear(x, y, Z(ty), x, y + 2, Z(ty) + 1);
        k.set(x, y, Z(ty), "minecraft:spruce_door[facing=east,half=lower,hinge=left]");
        k.set(x, y, Z(ty) + 1, "minecraft:spruce_door[facing=east,half=lower,hinge=right]");
    }

    // ------------------------------------------------------------------ Barbarian Village

    static StructureDef barbarianVillage() {
        MisthalinKit k = new MisthalinKit("barbarian_village", "Barbarian Village", "BARBARIAN_VILLAGE");
        k.tFlatten(3066, 3398, 3114, 3452);
        Box area = t(3066, 3398, 3114, 3452);
        k.meadow(area, 10);
        k.scatter(area, -1, COARSE_DIRT, 8);
        // paths: east over the bridge towards Varrock, north to Edgeville
        k.tPath(3070, 3421, 3101, 3421, 3);
        k.tPath(3108, 3421, 3114, 3421, 3);
        k.tPath(3082, 3452, 3082, 3422, 2);
        k.tPath(3082, 3418, 3082, 3400, 2);
        // the river (fly fishing spots) and the bridge
        k.water(t(3102, 3398, 3106, 3452), 3);
        k.floor(t(3101, 3400, 3101, 3450), GRAVEL).floor(t(3107, 3400, 3107, 3450), GRAVEL);
        k.set(X(3104), -1, Z(3431), "runecraft:lure_bait_fishing_spot").set(X(3105), -1, Z(3437), "runecraft:lure_bait_fishing_spot");
        k.set(X(3103), -1, Z(3412), "runecraft:lure_bait_fishing_spot");
        Box br = t(3100, 3421, 3108, 3419);
        k.bridgeX(br.x1(), br.x2(), br.z1(), br.z2(), 0, "minecraft:spruce_planks", "minecraft:spruce_fence");
        k.fill(X(3102), -4, br.z1(), X(3102), -1, br.z1(), SPRUCE_LOG).fill(X(3102), -4, br.z2(), X(3102), -1, br.z2(), SPRUCE_LOG);
        k.fill(X(3106) + 1, -4, br.z1(), X(3106) + 1, -1, br.z1(), SPRUCE_LOG).fill(X(3106) + 1, -4, br.z2(), X(3106) + 1, -1, br.z2(), SPRUCE_LOG);
        longhall(k);
        huts(k);
        stronghold(k);
        // palisade along the west and north edges
        k.connect(X(3068), 0, Z(3450), X(3068), 0, Z(3400), "minecraft:spruce_fence");
        k.connect(X(3068), 1, Z(3450), X(3068), 1, Z(3400), "minecraft:spruce_fence");
        k.clear(X(3068), 0, Z(3421), X(3068), 1, Z(3419) + 1);
        for (int[] tr : new int[][]{{3071, 3447}, {3096, 3448}, {3110, 3445}, {3110, 3404}, {3071, 3402}, {3095, 3400}}) {
            k.tOak(tr[0], tr[1]);
        }
        // camp fires
        k.set(X(3090), 0, Z(3422), "minecraft:campfire[lit=true]");
        k.set(X(3076), 0, Z(3433), "minecraft:campfire[lit=true]");
        k.fill(X(3089), 0, Z(3424), X(3091), 0, Z(3424), "minecraft:spruce_log[axis=x]");
        return k.build();
    }

    private static void longhall(MisthalinKit k) {
        // the longhall where Gunthor the Brave holds court
        Box b = t(3073, 3436, 3091, 3446);
        k.house(b).storeys(1).height(6).wall("minecraft:spruce_log[axis=x]").corner(SPRUCE_LOG).base(COBBLE)
                .floor(SPRUCE_PLANKS).roof(Roof.THATCH).gable(SPRUCE_PLANKS).window(null).doorAt('s', X(3082), 2, 4).build();
        k.fill(b.x1(), 0, b.z1() + 1, b.x1(), 5, b.z2() - 1, "minecraft:spruce_log[axis=z]");
        k.fill(b.x2(), 0, b.z1() + 1, b.x2(), 5, b.z2() - 1, "minecraft:spruce_log[axis=z]");
        k.clear(X(3082), 0, b.z2(), X(3082) + 1, 3, b.z2());
        // shutters for light
        for (int x = b.x1() + 4; x < b.x2() - 2; x += 6) {
            k.clear(x, 2, b.z1(), x, 2, b.z1());
            k.clear(x, 2, b.z2(), x, 2, b.z2());
        }
        Box in = b.inset(1);
        // long hearth down the middle
        k.fill(in.x1() + 6, -1, in.cz(), in.x2() - 6, -1, in.cz() + 1, COBBLE);
        for (int x = in.x1() + 7; x <= in.x2() - 7; x += 4) {
            k.set(x, 0, in.cz(), "minecraft:campfire[lit=true]");
        }
        k.diningX(in.x1() + 6, in.x2() - 6, 0, in.z1() + 3);
        k.diningX(in.x1() + 6, in.x2() - 6, 0, in.z2() - 3);
        // Gunthor's seat at the west end, the cooking range at the east end
        k.fill(in.x1(), -1, in.cz() - 2, in.x1() + 2, -1, in.cz() + 3, "minecraft:spruce_planks");
        k.set(in.x1(), 0, in.cz(), "minecraft:dark_oak_stairs[facing=west]");
        k.set(in.x1(), 0, in.cz() + 1, "minecraft:dark_oak_stairs[facing=west]");
        k.set(in.x1(), 1, in.cz() - 1, "minecraft:skeleton_skull").set(in.x1(), 1, in.cz() + 2, "minecraft:skeleton_skull");
        k.set(in.x1(), 3, in.cz(), "minecraft:red_wall_banner[facing=east]");
        k.set(in.x2(), 0, in.z1(), RANGE).set(in.x2(), 0, in.z1() + 1, BARREL).set(in.x2() - 1, 0, in.z1(), BARREL);
        k.set(in.x2(), 0, in.z2(), BARREL).set(in.x2(), 1, in.z2(), BARREL).set(in.x2() - 1, 0, in.z2(), HAY);
        for (int x = in.x1() + 4; x <= in.x2() - 4; x += 5) {
            k.set(x, 0, in.z1(), "minecraft:grindstone[face=floor,facing=south]");
            k.hang(x, 4, in.cz() + 2);
        }
    }

    private static void huts(MisthalinKit k) {
        // Peksa's helmet shop and the barbarians' huts (log walls, thatched roofs)
        int[][] hs = {{3071, 3424, 3077, 3429, 'e'}, {3086, 3427, 3092, 3432, 'w'}, {3072, 3409, 3078, 3414, 'e'},
                {3093, 3410, 3099, 3415, 'w'}, {3093, 3438, 3098, 3444, 's'}, {3083, 3406, 3090, 3411, 'n'}};
        int i = 0;
        for (int[] h : hs) {
            Box b = t(h[0], h[1], h[2], h[3]);
            char side = (char) h[4];
            k.house(b).storeys(1).wall(SPRUCE_LOG).corner(STRIPPED_SPRUCE).base(COBBLE).floor(SPRUCE_PLANKS)
                    .roof(Roof.THATCH).gable(SPRUCE_PLANKS).window(null)
                    .doorAt(side, side == 'n' || side == 's' ? b.cx() : b.cz(), 2, 3).build();
            Box in = b.inset(1);
            if (i == 0) {
                // Peksa's helmet shop
                k.counterZ(in.x2() - 2, 0, in.z1() + 1, in.z2() - 1, TOOL_SHOP);
                k.set(in.x1(), 0, in.z1(), "minecraft:skeleton_skull").set(in.x1(), 0, in.z2(), BARREL);
                k.set(in.x1(), 0, in.cz(), "minecraft:smithing_table");
            } else if (i == 5) {
                // the pottery (wheel and oven are placed by the pottery structure)
                k.set(in.x1(), 0, in.z1(), "minecraft:clay").set(in.x2(), 0, in.z1(), "minecraft:decorated_pot");
                k.set(in.x2(), 0, in.z1() + 1, "minecraft:decorated_pot").set(in.x1(), 0, in.z1() + 1, BARREL);
            } else {
                k.set(in.x1(), 0, in.z1(), "minecraft:brown_wool").set(in.x1() + 1, 0, in.z1(), "minecraft:brown_wool");
                k.set(in.x2(), 0, in.z2(), BARREL).set(in.x2(), 0, in.z1(), "minecraft:chest[facing=west]");
                k.set(in.cx(), 0, in.cz(), "minecraft:campfire[lit=true]");
            }
            k.hang(in.cx() + (i == 5 ? 0 : 1), 2, in.cz());
            i++;
        }
    }

    private static void stronghold(MisthalinKit k) {
        // the entrance to the Stronghold of Security: a dark hole ringed with stones, a ladder down
        int x = X(3081);
        int z = Z(3421);
        k.fill(x - 2, -1, z - 2, x + 3, -1, z + 3, MOSSY_COBBLE);
        k.fill(x - 1, -1, z - 1, x + 2, -1, z + 2, "minecraft:cobbled_deepslate");
        k.set(x - 2, 0, z - 2, "minecraft:cobblestone_wall").set(x + 3, 0, z - 2, "minecraft:cobblestone_wall");
        k.set(x - 2, 0, z + 3, "minecraft:cobblestone_wall").set(x + 3, 0, z + 3, "minecraft:cobblestone_wall");
        k.set(x - 2, 1, z - 2, "minecraft:skeleton_skull").set(x + 3, 1, z + 3, "minecraft:skeleton_skull");
        k.fill(x - 2, -14, z - 2, x + 3, -2, z + 3, "minecraft:cobbled_deepslate");
        k.clear(x, -13, z, x + 1, -1, z + 1);
        k.ladder(x, z + 1, "north", -13, -1);
        k.fill(x + 1, -13, z + 1, x + 1, -2, z + 1, "minecraft:cobbled_deepslate");
        k.clear(x - 1, -13, z - 1, x + 2, -11, z);
        k.connect(x - 1, -13, z - 2, x + 2, -11, z - 2, BARS);
        k.set(x + 2, -11, z, "minecraft:soul_wall_torch[facing=west]");
        k.set(x + 1, -11, z, "minecraft:soul_lantern[hanging=true]");
    }

    // ------------------------------------------------------------------ pottery

    static StructureDef pottery() {
        MisthalinKit k = new MisthalinKit("barbarian_village_pottery", "Barbarian Village pottery", "barbarian_village_pottery");
        k.set(X(3085), 0, Z(3409), "runecraft:potters_wheel");
        k.set(X(3088), 0, Z(3409), "runecraft:pottery_oven");
        return k.build();
    }
}
