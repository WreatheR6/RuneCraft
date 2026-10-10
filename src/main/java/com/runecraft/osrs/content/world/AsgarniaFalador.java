package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.AsgarniaKit.FLAT;
import static com.runecraft.osrs.content.world.AsgarniaKit.PITCHED;
import static com.runecraft.osrs.content.world.AsgarniaKit.mc;
import static com.runecraft.osrs.content.world.AsgarniaKit.rc;
import static com.runecraft.osrs.content.world.AsgarniaKit.rect;
import static com.runecraft.osrs.content.world.AsgarniaKit.stair;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/** Falador: the white city walls, White Knights' Castle, Falador Park, Party Room, banks, shops and guild. */
final class AsgarniaFalador {
    static final AsgarniaKit.Style FAL = new AsgarniaKit.Style(mc("smooth_quartz"), mc("quartz_pillar[axis=y]"),
            mc("spruce_planks"), mc("polished_diorite"), mc("deepslate_tile_stairs"), mc("deepslate_tiles"), mc("glass_pane"), false);
    static final AsgarniaKit.Style FAL2 = new AsgarniaKit.Style(mc("calcite"), mc("polished_diorite"),
            mc("oak_planks"), mc("diorite"), mc("deepslate_tile_stairs"), mc("deepslate_tiles"), mc("glass_pane"), false);
    static final AsgarniaKit.Style FAL3 = new AsgarniaKit.Style(mc("white_concrete"), mc("quartz_pillar[axis=y]"),
            mc("birch_planks"), mc("smooth_quartz"), mc("polished_blackstone_brick_stairs"), mc("polished_blackstone_bricks"),
            mc("glass_pane"), false);
    static final AsgarniaKit.Style CASTLE = new AsgarniaKit.Style(mc("quartz_bricks"), mc("quartz_pillar[axis=y]"),
            mc("polished_diorite"), mc("smooth_quartz"), mc("smooth_quartz_stairs"), mc("smooth_quartz"),
            mc("light_blue_stained_glass_pane"), false);

    static final String PAVE = mc("polished_andesite");
    static final String PAVE2 = mc("stone_bricks");
    static final String WALL = mc("smooth_quartz");

    private AsgarniaFalador() {
    }

    static void build(List<StructureDef> out) {
        out.add(city());
        out.add(castle());
        out.add(park());
        out.add(treePatch());
        out.add(mahoganyHomes());
        out.add(miningYard());
        out.add(makeoverMage());
    }

    // ------------------------------------------------------------------ walls

    /** A white city wall one tile thick along a tile line, crenellated on the outer side. */
    static void cityWall(AsgarniaKit k, int tx1, int ty1, int tx2, int ty2, String outer) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        k.f(r[0], -3, r[1], r[2], 7, r[3], WALL);
        k.f(r[0], -1, r[1], r[2], 0, r[3], mc("polished_diorite"));
        k.f(r[0], 7, r[1], r[2], 7, r[3], mc("smooth_stone"));
        boolean alongX = r[2] - r[0] > r[3] - r[1];
        if (alongX) {
            int z = outer.equals("north") ? r[1] : r[3];
            for (int x = r[0]; x <= r[2]; x += 2) {
                k.s(x, 8, z, mc("quartz_bricks"));
            }
            for (int x = r[0] + 6; x <= r[2]; x += 14) {
                k.s(x, 4, z + (outer.equals("north") ? -1 : 1), mc("blue_wall_banner[facing=" + outer + "]"));
            }
        } else {
            int x = outer.equals("west") ? r[0] : r[2];
            for (int z = r[1]; z <= r[3]; z += 2) {
                k.s(x, 8, z, mc("quartz_bricks"));
            }
            for (int z = r[1] + 6; z <= r[3]; z += 14) {
                k.s(x + (outer.equals("west") ? -1 : 1), 4, z, mc("blue_wall_banner[facing=" + outer + "]"));
            }
        }
    }

    /** A gate through a wall: opening, portcullis bars and flanking towers. */
    static void gate(AsgarniaKit k, int tx1, int ty1, int tx2, int ty2, boolean alongX, int towerR, int towerH) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        k.c(r[0], 0, r[1], r[2], 4, r[3]);
        k.f(r[0], -1, r[1], r[2], -1, r[3], PAVE2);
        if (alongX) {
            k.f(r[0], 5, r[1], r[2], 5, r[1], mc("iron_bars"));
            int cz = r[1] + 1;
            k.tower(r[0] - towerR, cz, towerR, towerH, mc("quartz_bricks"), mc("polished_diorite"), null, null);
            k.tower(r[2] + towerR, cz, towerR, towerH, mc("quartz_bricks"), mc("polished_diorite"), null, null);
            k.s(r[0] - towerR, towerH + 1, cz, mc("blue_banner"));
            k.s(r[2] + towerR, towerH + 1, cz, mc("blue_banner"));
        } else {
            k.f(r[0], 5, r[1], r[0], 5, r[3], mc("iron_bars"));
            int cx = r[0] + 1;
            k.tower(cx, r[1] - towerR, towerR, towerH, mc("quartz_bricks"), mc("polished_diorite"), null, null);
            k.tower(cx, r[3] + towerR, towerR, towerH, mc("quartz_bricks"), mc("polished_diorite"), null, null);
            k.s(cx, towerH + 1, r[1] - towerR, mc("blue_banner"));
            k.s(cx, towerH + 1, r[3] + towerR, mc("blue_banner"));
        }
    }

    /** Street lamps (fence post with a lantern) every {@code step} tiles along a tile line. */
    static void lamps(AsgarniaKit k, int tx1, int ty1, int tx2, int ty2, int step) {
        for (int x = Math.min(tx1, tx2); x <= Math.max(tx1, tx2); x += step) {
            for (int y = Math.min(ty1, ty2); y <= Math.max(ty1, ty2); y += step) {
                k.st(x, y, 0, mc("dark_oak_fence"));
                k.st(x, y, 1, mc("dark_oak_fence"));
                k.st(x, y, 2, mc("lantern"));
            }
        }
    }

    /** A bench of two stairs facing {@code facing} (the back is on the opposite side). */
    static void bench(AsgarniaKit k, int x, int z, boolean alongX, String facing) {
        String st = stair(mc("spruce_stairs"), facing);
        k.s(x, 0, z, st);
        k.s(alongX ? x + 1 : x, 0, alongX ? z : z + 1, st);
    }

    // ------------------------------------------------------------------ city

    static StructureDef city() {
        StructureDef.Builder b = StructureDef.builder("falador_city", "Falador").replaces("FALADOR");
        AsgarniaKit k = new AsgarniaKit(b);
        k.doorWood = "birch";
        k.flatten(2928, 3304, 3070, 3402);

        // --- streets
        k.path(2956, 3375, 2972, 3390, PAVE, PAVE2);           // Falador square
        k.path(2961, 3390, 2964, 3398, PAVE, PAVE2);           // north road
        k.path(2936, 3374, 2956, 3378, PAVE, PAVE2);           // west road
        k.path(2972, 3376, 2981, 3378, PAVE, PAVE2);           // to the park
        k.path(2961, 3357, 2964, 3375, PAVE, PAVE2);           // castle road
        k.path(2936, 3358, 3066, 3361, PAVE, PAVE2);           // lower street, west gate to east gate
        k.path(3005, 3308, 3008, 3357, PAVE, PAVE2);           // south road
        k.path(2948, 3312, 2949, 3357, mc("andesite"), PAVE);  // west lane
        k.path(2948, 3320, 3004, 3321, mc("andesite"), PAVE);  // castle south lane
        k.path(3005, 3362, 3008, 3367, PAVE, PAVE2);           // park south gate
        k.path(3037, 3362, 3039, 3369, PAVE, PAVE2);           // party room
        k.path(3009, 3346, 3030, 3349, mc("andesite"), PAVE);  // bank/guild yard
        k.path(3025, 3376, 3029, 3378, PAVE, PAVE2);           // park east gate
        lamps(k, 2938, 3373, 2956, 3373, 9);
        lamps(k, 2938, 3362, 3062, 3362, 12);
        lamps(k, 3009, 3312, 3009, 3356, 11);
        lamps(k, 2960, 3364, 2960, 3374, 10);

        // --- the square: fountain, benches, statue of Saradomin
        int fx = bx(2967);
        int fz = bz(3380);
        k.fountain(fx, fz, 4, mc("smooth_quartz"), mc("quartz_pillar[axis=y]"));
        bench(k, fx - 2, fz - 6, true, "north");
        bench(k, fx - 2, fz + 6, true, "south");
        bench(k, fx + 6, fz - 1, false, "east");
        int sx = bx(2958);
        int sz = bz(3389);
        k.f(sx - 1, 0, sz - 1, sx + 1, 0, sz + 1, mc("chiseled_quartz_block"));
        k.f(sx, 1, sz, sx, 3, sz, mc("quartz_pillar[axis=y]"));
        k.s(sx, 4, sz, mc("white_concrete"));
        k.s(sx, 5, sz, mc("gold_block"));
        k.s(sx - 1, 3, sz, mc("end_rod[facing=west]"));
        k.s(sx, 0, sz + 2, rc("prayer_altar"));
        k.s(sx - 1, 0, sz + 2, mc("candle[candles=3,lit=true]"));
        k.s(sx + 1, 0, sz + 2, mc("candle[candles=3,lit=true]"));
        k.flowers(2970, 3389, 2972, 3390);
        // Falador farming patches outside the south wall
        int[] fp = rect(3044, 3304, 3058, 3308);
        k.f(fp[0], -1, fp[1], fp[2], -1, fp[3], mc("coarse_dirt"));
        k.w(fp[0], 0, fp[1], fp[2], 0, fp[3], mc("oak_fence"));
        k.clearT(3051, 3308, 3051, 3308, 0, 0);
        k.st(3046, 3306, 0, rc("allotment_patch"));
        k.st(3056, 3306, 0, rc("allotment_patch"));
        k.st(3051, 3305, 0, rc("herb_patch"));
        k.st(3049, 3307, 0, rc("compost_bin"));
        int[] fl = rect(3045, 3305, 3048, 3305);
        k.f(fl[0], -1, fl[1], fl[2], -1, fl[3], mc("farmland[moisture=7]"));
        k.f(fl[0], 0, fl[1], fl[2], 0, fl[3], mc("carrots[age=7]"));
        int[] fr2 = rect(3054, 3305, 3057, 3305);
        k.f(fr2[0], -1, fr2[1], fr2[2], -1, fr2[3], mc("farmland[moisture=7]"));
        k.f(fr2[0], 0, fr2[1], fr2[2], 0, fr2[3], mc("potatoes[age=7]"));
        k.flowers(2956, 3375, 2957, 3376);

        // --- walls and gates
        cityWall(k, 2936, 3396, 3064, 3396, "north");
        cityWall(k, 2936, 3310, 3064, 3310, "south");
        cityWall(k, 2936, 3310, 2936, 3396, "west");
        cityWall(k, 3064, 3310, 3064, 3396, "east");
        gate(k, 2961, 3396, 2964, 3396, true, 4, 12);
        gate(k, 2936, 3374, 2936, 3378, false, 4, 12);
        gate(k, 3064, 3358, 3064, 3361, false, 4, 12);
        gate(k, 3005, 3310, 3008, 3310, true, 4, 12);
        int[][] towers = {{2936, 3396}, {3064, 3396}, {2936, 3310}, {3064, 3310}, {3030, 3310}, {2936, 3340},
                {3064, 3335}, {2970, 3310}};
        for (int[] t : towers) {
            k.tower(bx(t[0]) + 1, bz(t[1]) + 1, 5, 13, mc("quartz_bricks"), mc("polished_diorite"), null, null);
            k.s(bx(t[0]) + 1, 14, bz(t[1]) + 1, mc("blue_banner"));
        }

        // --- west quarter
        AsgarniaRoom hair = k.house(FAL, 2939, 3381, 2945, 3387, 1, "south", PITCHED);
        hair.home("spruce", "pink");
        hair.set(1, 0, 0, mc("smooth_quartz_stairs[facing=north]"));
        hair.set(3, 0, 0, mc("smooth_quartz_stairs[facing=north]"));
        hair.fill(1, 0, 4, 0, 1, 1, mc("glass"));
        AsgarniaRoom gen = k.house(FAL2, 2947, 3381, 2954, 3388, 2, "east", PITCHED);
        gen.shop(rc("general_store"), mc("spruce_slab[type=top]"), mc("barrel[facing=up]"), mc("chest[facing=west]"),
                mc("hay_block"), mc("composter"));
        gen.level(1).home("spruce", "brown");
        AsgarniaRoom flynn = k.house(FAL3, 2939, 3389, 2945, 3394, 1, "east", PITCHED);
        flynn.shop(rc("weapon_shop"), mc("dark_oak_slab[type=top]"), mc("grindstone[face=floor,facing=north]"),
                mc("anvil[facing=north]"), mc("barrel[facing=up]"));
        k.house(FAL, 2947, 3390, 2954, 3394, 2, "east", PITCHED).home("oak", "light_blue");
        AsgarniaRoom bank = k.house(FAL, 2941, 3365, 2949, 3372, 2, "north", FLAT);
        bank.bank(mc("polished_diorite"));
        bank.level(1).shop(mc("lectern[facing=north]"), mc("spruce_slab[type=top]"), mc("chest[facing=north]"),
                mc("bookshelf"));
        AsgarniaRoom inn = k.house(FAL2, 2951, 3363, 2959, 3372, 2, "north", PITCHED);
        inn.inn("spruce");
        AsgarniaRoom up = inn.level(1);
        up.bed(up.len() - 1, 1, "red");
        up.bed(up.len() - 4, 1, "blue");
        up.bed(up.len() - 7, 1, "green");
        up.table(3, up.depth() - 3, "spruce");
        k.house(FAL3, 2939, 3341, 2947, 3348, 2, "east", PITCHED).home("birch", "blue");
        AsgarniaRoom herq = k.house(FAL, 2940, 3331, 2947, 3337, 1, "east", PITCHED);
        herq.shop(rc("gem_stall"), mc("quartz_slab[type=top]"), mc("amethyst_block"), mc("chest[facing=east]"),
                mc("emerald_block"), mc("diamond_block"));
        k.house(FAL2, 2939, 3314, 2947, 3322, 1, "east", PITCHED).home("oak", "yellow");
        k.house(FAL, 2939, 3350, 2946, 3356, 1, "east", PITCHED).home("spruce", "cyan");

        // --- around the square
        AsgarniaRoom cassie = k.house(FAL3, 2973, 3380, 2978, 3386, 1, "west", PITCHED);
        cassie.shop(rc("weapon_shop"), mc("spruce_slab[type=top]"), mc("polished_andesite"), mc("barrel[facing=up]"));
        for (int i = 0; i < 4; i++) {
            k.s(bx(2978) + 2, 2, bz(3385) + i * 3, mc("blue_wall_banner[facing=east]"));
        }
        k.house(FAL, 2973, 3388, 2978, 3394, 2, "west", PITCHED).home("spruce", "purple");
        AsgarniaRoom furnace = k.house(FAL2, 2968, 3364, 2976, 3372, 1, "west", FLAT);
        furnace.set(1, 0, 0, rc("furnace"));
        furnace.set(3, 0, 0, rc("furnace"));
        furnace.set(2, 0, 0, mc("bricks"));
        furnace.fill(1, 0, 3, 0, 1, 2, mc("bricks"));
        furnace.set(furnace.len() - 3, 2, 0, rc("anvil"));
        furnace.set(furnace.len() - 2, 0, 0, mc("barrel[facing=up]"));
        furnace.set(furnace.len() - 1, 0, 0, mc("coal_block"));
        furnace.set(furnace.len() - 1, 3, 0, mc("water_cauldron[level=3]"));
        k.f(furnace.wx(2, 0), 3, furnace.wz(2, 0), furnace.wx(2, 0), 8, furnace.wz(2, 0), mc("bricks"));
        k.s(furnace.wx(2, 0), 9, furnace.wz(2, 0), mc("campfire[lit=true]"));

        // --- east quarter: east bank, guild, party room, dwarven mine hut and houses
        AsgarniaRoom eb = k.house(FAL, 3009, 3350, 3018, 3356, 2, "north", FLAT);
        eb.bank(mc("polished_diorite"));
        eb.level(1).home("spruce", "red");
        AsgarniaRoom party = k.house(FAL3, 3030, 3370, 3046, 3384, 2, "south", PITCHED);
        String[] colors = {"red", "yellow", "lime", "light_blue", "magenta", "orange"};
        for (int u = 1; u < party.len(); u++) {
            for (int v = 0; v < party.depth() - 1; v++) {
                party.set(u, v, 0, mc(colors[(u / 2 + v / 2) % colors.length] + "_carpet"));
            }
        }
        party.fill(party.len() / 2 - 2, 0, party.len() / 2 + 2, 2, 0, 0, mc("quartz_slab[type=bottom]"));
        party.set(party.len() / 2, 1, 0, mc("chest[facing=south]"));
        party.set(party.len() / 2 + 1, 1, 0, mc("lever[face=floor,facing=south]"));
        AsgarniaRoom p2 = party.level(1);
        for (int u = 2; u < p2.len() - 1; u += 3) {
            for (int v = 2; v < p2.depth() - 1; v += 3) {
                p2.set(u, v, 2, mc(colors[(u + v) % colors.length] + "_wool"));
                p2.set(u, v, 1, mc(colors[(u + v + 1) % colors.length] + "_wool"));
            }
        }
        p2.inn("birch");
        AsgarniaRoom dm = k.house(FAL2, 3052, 3374, 3058, 3380, 1, "south", PITCHED);
        int lx = dm.wx(3, 2);
        int lz = dm.wz(3, 2);
        k.c(lx - 1, -8, lz - 1, lx + 1, -1, lz + 1);
        k.f(lx - 2, -9, lz - 2, lx + 2, -9, lz + 2, mc("stone"));
        k.w(lx - 2, -8, lz - 2, lx + 2, -1, lz + 2, mc("stone"));
        k.f(lx, -8, lz - 1, lx, -1, lz - 1, mc("ladder[facing=south]"));
        k.s(lx, -8, lz + 1, rc("iron_rock"));
        k.s(lx + 1, -8, lz, rc("coal_rock"));
        k.s(lx + 2, 0, lz + 2, mc("lantern"));
        dm.set(0, dm.depth() - 1, 0, mc("barrel[facing=up]"));
        dm.set(dm.len() - 1, 0, 0, mc("chest[facing=south]"));
        dm.set(dm.len() - 1, 1, 0, mc("iron_block"));
        k.house(FAL, 3028, 3387, 3034, 3394, 2, "south", PITCHED).home("spruce", "lime");
        k.house(FAL2, 3040, 3388, 3047, 3394, 1, "south", PITCHED).home("oak", "orange");
        k.house(FAL3, 3050, 3385, 3058, 3392, 2, "south", PITCHED).home("birch", "magenta");
        k.house(FAL, 3048, 3364, 3056, 3370, 1, "south", PITCHED).home("spruce", "gray");
        k.house(FAL2, 2995, 3345, 3002, 3352, 2, "north", PITCHED).home("oak", "red");
        k.house(FAL, 2994, 3330, 3002, 3338, 1, "east", PITCHED).home("spruce", "green");
        k.house(FAL3, 3022, 3350, 3030, 3356, 1, "north", PITCHED).home("birch", "light_gray");
        k.house(FAL2, 3030, 3314, 3040, 3324, 2, "north", PITCHED).home("oak", "blue");
        k.house(FAL, 3044, 3314, 3054, 3324, 1, "north", PITCHED).home("spruce", "brown");
        k.house(FAL3, 3053, 3336, 3060, 3346, 2, "west", PITCHED).home("birch", "cyan");
        k.house(FAL, 3050, 3350, 3058, 3356, 2, "north", PITCHED).home("spruce", "yellow");
        k.house(FAL2, 3032, 3352, 3040, 3356, 1, "north", PITCHED).home("oak", "pink");

        // Mining Guild: entrance building with a ladder down to the guild's coal and mithril rocks
        AsgarniaRoom mg = k.house(FAL2, 3014, 3333, 3027, 3344, 1, "north", FLAT);
        int gx1 = mg.x1 + 1;
        int gz1 = mg.z1 + 1;
        int gx2 = mg.x2 - 1;
        int gz2 = mg.z2 - 3;
        k.c(gx1, -9, gz1, gx2, -2, gz2);
        k.f(gx1 - 1, -10, gz1 - 1, gx2 + 1, -10, gz2 + 1, mc("cobbled_deepslate"));
        k.w(gx1 - 1, -9, gz1 - 1, gx2 + 1, -2, gz2 + 1, mc("stone"));
        k.f(gx1, -1, gz1, gx2, -1, gz2, mc("spruce_planks"));
        k.c(gx2, -1, gz2, gx2, -1, gz2);
        k.f(gx2, -9, gz2 + 1, gx2, -1, gz2 + 1, mc("stone"));
        k.f(gx2, -9, gz2, gx2, -1, gz2, mc("ladder[facing=north]"));
        String[] ores = {rc("coal_rock"), rc("coal_rock"), rc("mithril_rock"), rc("iron_rock"), rc("coal_rock"), rc("adamantite_rock")};
        int oi = 0;
        for (int x = gx1; x <= gx2 - 2; x += 2) {
            k.s(x, -9, gz1, ores[oi++ % ores.length]);
        }
        for (int z = gz1 + 2; z <= gz2; z += 2) {
            k.s(gx1, -9, z, ores[oi++ % ores.length]);
        }
        for (int x = gx1 + 2; x <= gx2 - 2; x += 4) {
            k.s(x, -6, (gz1 + gz2) / 2, mc("lantern[hanging=true]"));
            k.f(x, -5, (gz1 + gz2) / 2, x, -2, (gz1 + gz2) / 2, mc("chain"));
        }
        mg.set(1, mg.depth() - 2, 0, mc("smithing_table"));
        mg.set(mg.len() - 2, mg.depth() - 1, 0, mc("barrel[facing=up]"));

        // houses south of the castle and Wayne's Chains
        AsgarniaRoom wayne = k.house(FAL3, 2968, 3313, 2975, 3319, 1, "north", PITCHED);
        wayne.shop(rc("weapon_shop"), mc("spruce_slab[type=top]"), mc("chain"), mc("iron_block"), mc("barrel[facing=up]"));
        wayne.set(1, 1, 0, rc("anvil"));
        k.house(FAL, 2955, 3313, 2963, 3319, 1, "north", PITCHED).home("spruce", "white");
        k.house(FAL2, 2980, 3313, 2988, 3319, 2, "north", PITCHED).home("oak", "light_blue");
        k.house(FAL, 2992, 3313, 3001, 3319, 1, "north", PITCHED).home("spruce", "lime");

        // greenery
        int[][] trees = {{2938, 3363}, {2952, 3352}, {2952, 3340}, {2952, 3328}, {2993, 3341}, {3004, 3326}, {3012, 3318},
                {3024, 3328}, {3044, 3330}, {3058, 3326}, {3060, 3352}, {3045, 3366}, {3027, 3364}, {2984, 3364}};
        for (int i = 0; i < trees.length; i++) {
            k.tree(bx(trees[i][0]), bz(trees[i][1]), i % 3 == 0 ? "birch" : "oak", 6 + i % 3);
        }
        k.flowers(3009, 3341, 3012, 3344);
        k.flowers(2990, 3350, 2993, 3352);
        return k.build();
    }

    // ------------------------------------------------------------------ White Knights' Castle

    static StructureDef castle() {
        StructureDef.Builder b = StructureDef.builder("falador_white_knights_castle", "White Knights' Castle").replaces("falador_castle");
        AsgarniaKit k = new AsgarniaKit(b);
        int[] r = rect(2953, 3322, 2991, 3356);
        int x1 = r[0];
        int z1 = r[1];
        int x2 = r[2];
        int z2 = r[3];
        String qb = mc("quartz_bricks");
        // courtyard
        k.f(x1, -2, z1, x2, -1, z2, mc("grass_block"));
        k.c(x1, 0, z1, x2, 20, z2);
        // curtain wall, two blocks thick
        k.w(x1, -3, z1, x2, 9, z2, qb);
        k.w(x1 + 1, -3, z1 + 1, x2 - 1, 9, z2 - 1, qb);
        k.w(x1, -1, z1, x2, 0, z2, mc("polished_diorite"));
        k.w(x1 + 1, 9, z1 + 1, x2 - 1, 9, z2 - 1, mc("smooth_stone"));
        for (int x = x1; x <= x2; x += 2) {
            k.s(x, 10, z1, qb);
            k.s(x, 10, z2, qb);
        }
        for (int z = z1; z <= z2; z += 2) {
            k.s(x1, 10, z, qb);
            k.s(x2, 10, z, qb);
        }
        // corner towers
        int[][] corners = {{x1 + 1, z1 + 1}, {x2 - 1, z1 + 1}, {x1 + 1, z2 - 1}, {x2 - 1, z2 - 1}};
        for (int[] c : corners) {
            k.tower(c[0], c[1], 6, 18, qb, mc("polished_diorite"), null, null);
            k.d.spire(c[0], c[1], 5, k.g + 18, 2, mc("deepslate_tiles"), "blue");
        }
        // north gatehouse aligned with the castle road (tiles 2961..2964)
        int[] gr = rect(2961, 3356, 2964, 3356);
        k.c(gr[0], 0, z1, gr[2], 5, z1 + 1);
        k.f(gr[0], -1, z1, gr[2], -1, z1 + 1, PAVE2);
        k.f(gr[0], 6, z1, gr[2], 6, z1, mc("iron_bars"));
        k.tower(gr[0] - 4, z1 + 1, 4, 15, qb, mc("polished_diorite"), null, null);
        k.tower(gr[2] + 4, z1 + 1, 4, 15, qb, mc("polished_diorite"), null, null);
        for (int x = gr[0]; x <= gr[2]; x += 3) {
            k.s(x, 4, z1 - 1, mc("blue_wall_banner[facing=north]"));
        }
        // paths in the courtyard
        k.path(2961, 3340, 2964, 3355, PAVE, PAVE2);
        k.path(2961, 3340, 2970, 3343, PAVE, PAVE2);
        k.path(2963, 3330, 2966, 3340, PAVE, PAVE2);
        // well
        int wx = bx(2957);
        int wz = bz(3336);
        k.ring(wx, wz, 2, -1, 0, mc("cobblestone_wall"));
        k.disc(wx, wz, 1.4, -6, -1, mc("water"));
        k.ring(wx, wz, 2, -7, -2, mc("stone_bricks"));
        k.f(wx - 2, 1, wz, wx - 2, 2, wz, mc("oak_fence"));
        k.f(wx + 2, 1, wz, wx + 2, 2, wz, mc("oak_fence"));
        k.f(wx - 2, 3, wz, wx + 2, 3, wz, mc("oak_slab[type=bottom]"));
        k.s(wx, 2, wz, mc("chain"));
        // training yard with targets and hay
        for (int i = 0; i < 4; i++) {
            k.s(bx(2956) + i * 3, 0, bz(3352), mc("hay_block"));
            k.s(bx(2956) + i * 3, 1, bz(3352), mc("target"));
            k.s(bx(2956) + i * 3, 2, bz(3352), mc("carved_pumpkin[facing=south]"));
        }
        k.fenceRing(2955, 3349, 2959, 3353, mc("oak_fence"));
        k.flowers(2967, 3345, 2969, 3352);
        k.tree(bx(2967) + 1, bz(3350), "birch", 7);
        // smithy against the west wall
        AsgarniaRoom smithy = k.house(CASTLE, 2955, 3324, 2960, 3330, 1, "east", FLAT);
        smithy.set(1, 0, 0, rc("anvil"));
        smithy.set(3, 0, 0, mc("blast_furnace[facing=east]"));
        smithy.set(4, 0, 0, mc("smithing_table"));
        smithy.set(2, 2, 0, mc("grindstone[face=floor,facing=north]"));
        smithy.set(smithy.len() - 1, 0, 0, mc("water_cauldron[level=3]"));
        // great tower, south of the courtyard
        k.tower(bx(2965) + 1, bz(3327), 6, 30, qb, mc("polished_diorite"), "north", null);
        k.d.spire(bx(2965) + 1, bz(3327), 5, k.g + 30, 3, mc("deepslate_tiles"), "blue");
        k.d.banner(bx(2965) + 1, k.g + 27, bz(3327) + 7, 's', "blue");

        // the keep
        AsgarniaRoom hall = k.house(CASTLE, 2971, 3327, 2987, 3351, 4, "west", FLAT);
        int len = hall.len();
        int dep = hall.depth();
        // ground floor: great hall with long table, blue runner and throne
        hall.fill(len / 2 - 1, 2, len / 2, dep - 1, 0, 0, mc("blue_carpet"));
        hall.longTable(3, len / 2 - 4, dep / 2, "spruce", mc("white_carpet"));
        hall.longTable(len / 2 + 3, len - 4, dep / 2, "spruce", mc("white_carpet"));
        hall.fill(len / 2 - 3, 0, len / 2 + 2, 1, 0, 0, mc("smooth_quartz_slab[type=bottom]"));
        hall.set(len / 2 - 1, 0, 1, stair(mc("quartz_stairs"), hall.back()));
        hall.set(len / 2, 0, 1, stair(mc("quartz_stairs"), hall.back()));
        hall.set(len / 2 - 2, 0, 1, mc("gold_block"));
        hall.set(len / 2 + 1, 0, 1, mc("gold_block"));
        for (int u = 2; u < len - 1; u += 4) {
            hall.set(u, 0, 2, mc("blue_wall_banner[facing=" + hall.front() + "]"));
        }
        // first floor: Sir Vyvin's quarters (north half) and the chapel (south half)
        AsgarniaRoom l1 = hall.level(1);
        l1.fill(len / 2, 0, len / 2, dep - 1, 0, 2, qb);
        l1.fill(len / 2, dep / 2 - 1, len / 2, dep / 2, 0, 1, mc("air"));
        // Sir Vyvin: bed, cupboards with White Knight armour, desk, bookcases
        l1.bed(1, 1, "blue");
        l1.bed(2, 1, "blue");
        l1.fill(4, 0, len / 2 - 2, 0, 0, 2, mc("bookshelf"));
        l1.set(len / 2 - 1, 0, 0, mc("chest[facing=" + l1.front() + "]"));
        l1.set(len / 2 - 1, 1, 0, mc("chest[facing=" + l1.front() + "]"));
        l1.set(len / 2 - 1, 0, 1, mc("iron_block"));
        l1.table(5, dep / 2 + 2, "dark_oak");
        l1.set(5, dep / 2 + 4, 0, mc("lectern[facing=" + l1.back() + "]"));
        l1.fill(2, dep - 4, len / 2 - 2, dep - 2, 0, 0, mc("light_blue_carpet"));
        // chapel: pews, altar of Saradomin, candles and stained glass
        int cu1 = len / 2 + 1;
        l1.fill(cu1, 0, len - 1, 1, 0, 0, mc("smooth_quartz_slab[type=bottom]"));
        l1.set((cu1 + len) / 2, 0, 1, rc("prayer_altar"));
        l1.set((cu1 + len) / 2 - 1, 0, 1, mc("candle[candles=3,lit=true]"));
        l1.set((cu1 + len) / 2 + 1, 0, 1, mc("candle[candles=3,lit=true]"));
        for (int v = 3; v < dep - 2; v += 2) {
            l1.fill(cu1 + 1, v, cu1 + 3, v, 0, 0, stair(mc("spruce_stairs"), l1.front()));
            l1.fill(len - 4, v, len - 2, v, 0, 0, stair(mc("spruce_stairs"), l1.front()));
        }
        l1.fill(cu1 + 4, 2, len - 5, dep - 1, 0, 0, mc("blue_carpet"));
        // second floor: barracks and armoury
        AsgarniaRoom l2 = hall.level(2);
        for (int u = 2; u < len - 1; u += 3) {
            l2.bed(u, 0, "light_blue");
            l2.set(u, 3, 0, mc("chest[facing=" + l2.back() + "]"));
        }
        for (int u = 2; u < len - 1; u += 4) {
            l2.set(u, dep - 1, 0, mc("grindstone[face=floor,facing=north]"));
            l2.set(u + 1, dep - 1, 0, mc("barrel[facing=up]"));
        }
        l2.table(len / 2, dep / 2 + 2, "spruce");
        // twin blue-grey slate roofs over the keep behind a walkway, with the White Knights' flag
        int tx = (hall.x1 + hall.x2) / 2;
        int tz = (hall.z1 + hall.z2) / 2;
        k.pitched(hall.x1 + 2, hall.z1 + 2, tx - 1, hall.z2 - 2, 16, mc("deepslate_tile_stairs"), mc("deepslate_tiles"));
        k.pitched(tx + 1, hall.z1 + 2, hall.x2 - 2, hall.z2 - 2, 16, mc("deepslate_tile_stairs"), mc("deepslate_tiles"));
        k.d.gables(hall.x1 + 2, hall.z1 + 2, tx - 1, hall.z2 - 2, k.g + 16, false, mc("deepslate_tiles"), null, mc("light_blue_stained_glass_pane"));
        k.d.gables(tx + 1, hall.z1 + 2, hall.x2 - 2, hall.z2 - 2, k.g + 16, false, mc("deepslate_tiles"), null, mc("light_blue_stained_glass_pane"));
        int ridge = 16 + (tx - 1 - hall.x1 - 2 + 2) / 2 + 1;
        int fx = (hall.x1 + 2 + tx - 1) / 2;
        k.f(fx, ridge, tz, fx, ridge + 6, tz, mc("quartz_pillar[axis=y]"));
        k.f(fx + 1, ridge + 4, tz, fx + 3, ridge + 6, tz, mc("blue_wool"));
        k.f(fx + 1, ridge + 5, tz, fx + 3, ridge + 5, tz, mc("white_wool"));
        // third floor: the White Knights' trophy hall with armour displays and a map table
        AsgarniaRoom l3 = hall.level(3);
        l3.fill(1, 1, len - 2, dep - 2, 0, 0, mc("light_blue_carpet"));
        l3.longTable(3, len - 4, dep / 2, "dark_oak", mc("white_carpet"));
        for (int u = 2; u < len - 1; u += 3) {
            k.d.armourStand(l3.wx(u, 0), k.g + 12, l3.wz(u, 0), hall.front().charAt(0));
        }
        for (int u = 3; u < len - 2; u += 4) {
            l3.set(u, dep - 1, 0, mc("bookshelf"));
            l3.set(u, dep - 1, 1, mc("bookshelf"));
        }
        l3.set(len - 2, 2, 0, mc("cartography_table"));
        details(k, x1, z1, x2, z2, hall);
        return k.build();
    }

    /** Buttresses, banners, arrow slits and courtyard dressing for the White Knights' Castle. */
    private static void details(AsgarniaKit k, int x1, int z1, int x2, int z2, AsgarniaRoom hall) {
        String qb = mc("quartz_bricks");
        int g = k.g;
        // buttresses along the outer curtain walls, clear of the towers and the north gate
        k.d.buttresses(x1 + 9, z2, x2 - 9, z2, 's', 6, g, g + 8, qb, mc("quartz_stairs"));
        k.d.buttresses(x1, z1 + 9, x1, z2 - 9, 'w', 6, g, g + 8, qb, mc("quartz_stairs"));
        k.d.buttresses(x2, z1 + 9, x2, z2 - 9, 'e', 6, g, g + 8, qb, mc("quartz_stairs"));
        // arrow slits and blue-and-white banners on the outer faces
        for (int x = x1 + 12; x <= x2 - 12; x += 6) {
            k.d.slit(x, g + 5, g + 6, z2, qb);
            k.d.banner(x + 3, g + 7, z2 + 1, 's', (x / 6 & 1) == 0 ? "blue" : "white");
        }
        for (int z = z1 + 12; z <= z2 - 12; z += 6) {
            k.d.slit(x1, g + 5, g + 6, z, qb);
            k.d.slit(x2, g + 5, g + 6, z, qb);
            k.d.banner(x1 - 1, g + 7, z + 3, 'w', "blue");
            k.d.banner(x2 + 1, g + 7, z + 3, 'e', "blue");
        }
        k.d.textureSolid(x1, g, z1, x2, g + 9, z2, qb, 10);
        // a banner on every corner of the keep and window boxes of flowers on its courtyard face
        k.d.banner(hall.x1 - 2, g + 10, (hall.z1 + hall.z2) / 2 - 3, 'w', "blue");
        k.d.banner(hall.x1 - 2, g + 10, (hall.z1 + hall.z2) / 2 + 3, 'w', "blue");
        // courtyard: training dummies, benches and lamps
        for (int i = 0; i < 3; i++) {
            k.d.dummy(bx(2957) + i * 3, g, bz(3346));
        }
        k.d.bench(bx(2967), g, bz(3338), 'e', 3, 's', "spruce");
        k.d.bench(bx(2961), g, bz(3330), 'e', 3, 'n', "spruce");
        k.d.lamp(bx(2960), g, bz(3342), "minecraft:spruce_fence");
        k.d.lamp(bx(2967), g, bz(3342), "minecraft:spruce_fence");
        k.d.crates(bx(2956), g, bz(3332));
    }

    // ------------------------------------------------------------------ Falador Park

    static StructureDef park() {
        StructureDef.Builder b = StructureDef.builder("falador_park", "Falador Park").replaces("falador_mole_hills");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2980, 3368, 3024, 3393, mc("grass_block"));
        // hedge border with gates
        k.hedge(2980, 3393, 3024, 3393, 2);
        k.hedge(2980, 3368, 3024, 3368, 2);
        k.hedge(2980, 3368, 2980, 3393, 2);
        k.hedge(3024, 3368, 3024, 3393, 2);
        k.clearT(2980, 3376, 2980, 3378, 0, 2);
        k.clearT(3024, 3376, 3024, 3378, 0, 2);
        k.clearT(3005, 3368, 3008, 3368, 0, 2);
        // paths (the lawn east of the fountain is left open: the Giant Mole's arena is dug there)
        k.floor(2980, 3376, 3024, 3378, mc("dirt_path"));
        k.floor(3005, 3368, 3008, 3393, mc("dirt_path"));
        k.floor(2984, 3385, 2998, 3386, mc("gravel"));
        k.floor(2984, 3369, 2985, 3392, mc("gravel"));
        // fountain
        int fx = bx(2992);
        int fz = bz(3381);
        k.disc(fx, fz, 6, -1, -1, mc("polished_diorite"));
        k.fountain(fx, fz, 4, mc("smooth_quartz"), mc("quartz_pillar[axis=y]"));
        // statue of Saradomin in the north-west
        int sx = bx(2987);
        int sz = bz(3390);
        k.f(sx - 1, -1, sz - 1, sx + 2, -1, sz + 2, mc("polished_diorite"));
        k.f(sx, 0, sz, sx + 1, 0, sz + 1, mc("chiseled_quartz_block"));
        k.f(sx, 1, sz, sx + 1, 3, sz + 1, mc("smooth_quartz"));
        k.f(sx, 4, sz, sx + 1, 4, sz + 1, mc("white_concrete"));
        k.s(sx, 5, sz, mc("gold_block"));
        k.s(sx + 2, 3, sz, mc("end_rod[facing=east]"));
        // flower beds with low hedges
        k.flowers(3017, 3384, 3022, 3387);
        k.flowers(3017, 3369, 3022, 3373);
        k.flowers(2982, 3370, 2986, 3373);
        k.flowers(2995, 3388, 3003, 3391);
        k.flowers(2990, 3388, 2992, 3391);
        k.hedge(3016, 3383, 3023, 3383, 1);
        k.hedge(3016, 3374, 3023, 3374, 1);
        k.hedge(2994, 3387, 3004, 3387, 1);
        // trees
        int[][] trees = {{2983, 3391}, {2983, 3381}, {2996, 3392}, {3022, 3391}, {3022, 3379}, {2997, 3383},
                {3018, 3389}, {2987, 3375}};
        for (int i = 0; i < trees.length; i++) {
            k.tree(bx(trees[i][0]), bz(trees[i][1]), i % 2 == 0 ? "oak" : "birch", 6 + i % 3);
        }
        // mole hills (Giant Mole)
        int[][] moles = {{2995, 3373}, {2997, 3375}, {2994, 3371}, {3000, 3384}, {2999, 3389}};
        for (int[] m : moles) {
            k.st(m[0], m[1], 0, mc("rooted_dirt"));
            k.st(m[0], m[1], -1, mc("coarse_dirt"));
        }
        // benches and lamps along the paths
        bench(k, bx(2996), bz(3379), true, "north");
        bench(k, bx(3018), bz(3375), true, "south");
        bench(k, bx(3004), bz(3388), false, "west");
        bench(k, bx(2988), bz(3384), true, "south");
        lamps(k, 2984, 3375, 2994, 3375, 10);
        lamps(k, 3024, 3375, 3024, 3375, 10);
        lamps(k, 3009, 3384, 3009, 3392, 8);
        // gardener's shed
        AsgarniaRoom shed = k.house(FAL2, 3010, 3388, 3015, 3392, 1, "south", PITCHED);
        shed.set(0, 0, 0, mc("composter"));
        shed.set(1, 0, 0, mc("barrel[facing=up]"));
        shed.set(shed.len() - 1, 0, 0, mc("chest[facing=south]"));
        shed.set(shed.len() - 2, 0, 0, mc("potted_oak_sapling"));
        return k.build();
    }

    static StructureDef treePatch() {
        StructureDef.Builder b = StructureDef.builder("falador_park_tree_patch", "Falador tree patch").replaces("falador_tree_patch");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2988, 3369, 2992, 3372, mc("coarse_dirt"));
        k.fenceRing(2988, 3369, 2992, 3372, mc("oak_fence"));
        k.clearT(2992, 3370, 2992, 3370, 0, 0);
        k.st(2990, 3371, 0, rc("tree_patch"));
        k.st(2988, 3372, 1, mc("lantern"));
        k.st(2989, 3370, 0, rc("compost_bin"));
        return k.build();
    }

    static StructureDef mahoganyHomes() {
        StructureDef.Builder b = StructureDef.builder("falador_mahogany_homes", "Mahogany Homes office").replaces("mg_mahogany_homes_office");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2984, 3362, 2987, 3367, PAVE);
        AsgarniaRoom o = k.house(FAL2, 2988, 3362, 2995, 3367, 1, "west", PITCHED);
        o.shop(rc("mahogany_homes_rewards"), mc("dark_oak_slab[type=top]"), mc("dark_oak_planks"), mc("oak_planks"),
                mc("barrel[facing=up]"));
        o.set(1, o.depth() - 1, 0, rc("mahogany_homes_entrance"));
        o.set(o.len() - 1, o.depth() - 1, 0, rc("construction_workbench"));
        o.set(o.len() - 1, o.depth() - 2, 0, mc("stripped_dark_oak_log[axis=x]"));
        k.st(2985, 3367, 0, mc("oak_planks"));
        k.st(2985, 3367, 1, mc("oak_slab[type=bottom]"));
        return k.build();
    }

    static StructureDef miningYard() {
        StructureDef.Builder b = StructureDef.builder("falador_motherlode_yard", "Motherlode Mine pit").replaces("motherlode_mine");
        AsgarniaKit k = new AsgarniaKit(b);
        int[] r = rect(3034, 3334, 3050, 3350);
        int x1 = r[0];
        int z1 = r[1];
        int x2 = r[2];
        int z2 = r[3];
        k.f(x1, -1, z1, x2, -1, z2, mc("coarse_dirt"));
        k.c(x1, 0, z1, x2, 8, z2);
        k.fenceRing(3034, 3334, 3050, 3350, mc("spruce_fence"));
        k.c(x1, 0, (z1 + z2) / 2, x1, 0, (z1 + z2) / 2 + 1);
        int px1 = x1 + 3;
        int pz1 = z1 + 3;
        int px2 = x2 - 3;
        int pz2 = z2 - 3;
        k.c(px1, -6, pz1, px2, -1, pz2);
        k.f(px1, -7, pz1, px2, -7, pz2, mc("gravel"));
        k.w(px1 - 1, -7, pz1 - 1, px2 + 1, -1, pz2 + 1, mc("stone"));
        // pay-dirt veins in the pit walls
        for (int x = px1 + 2; x <= px2 - 2; x += 5) {
            k.s(x, -5, pz1 - 1, rc("pay_dirt_vein"));
            k.s(x, -5, pz2 + 1, rc("pay_dirt_vein"));
        }
        k.s(px1 - 1, -5, (pz1 + pz2) / 2, rc("pay_dirt_vein"));
        // hopper, sack, mine cart rails and a water wheel
        k.s(px2 - 2, -6, pz1 + 2, rc("pay_dirt_hopper"));
        k.s(px2 - 2, -6, pz2 - 2, rc("pay_dirt_sack"));
        k.f(px1 + 2, -6, (pz1 + pz2) / 2, px2 - 4, -6, (pz1 + pz2) / 2, mc("rail[shape=east_west]"));
        k.f(px2, -6, pz1 + 1, px2, -6, pz2 - 1, mc("water"));
        k.f(px2, -7, pz1 + 1, px2, -7, pz2 - 1, mc("stone"));
        int wz = (pz1 + pz2) / 2;
        k.ring(px2 + 1, wz, 3, -4, -4, mc("spruce_planks"));
        for (int d = -3; d <= 3; d++) {
            k.s(px2 - 1, -4 + d, wz, mc("stripped_spruce_wood"));
            k.s(px2 - 1, -4, wz + d, mc("stripped_spruce_wood"));
        }
        // ladder down
        k.f(px1, -6, pz1, px1, 0, pz1, mc("ladder[facing=south]"));
        k.s(px1 + 1, 0, pz1 - 2, mc("lantern"));
        for (int x = px1 + 3; x < px2; x += 5) {
            k.f(x, -2, wz, x, -1, wz, mc("chain"));
            k.s(x, -3, wz, mc("lantern[hanging=true]"));
            k.s(x, -1, wz, mc("spruce_planks"));
        }
        return k.build();
    }

    static StructureDef makeoverMage() {
        StructureDef.Builder b = StructureDef.builder("falador_makeover_mage", "Makeover Mage's house");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2911, 3314, 2926, 3330);
        AsgarniaKit.Style st = new AsgarniaKit.Style(mc("calcite"), mc("purpur_pillar[axis=y]"), mc("dark_oak_planks"),
                mc("polished_diorite"), mc("purpur_stairs"), mc("purpur_block"), mc("purple_stained_glass_pane"), false);
        AsgarniaRoom m = k.house(st, 2914, 3318, 2922, 3325, 2, "north", PITCHED);
        m.fill(1, 0, m.len() - 2, 0, 0, 2, mc("glass"));
        m.fill(1, 2, m.len() - 2, m.depth() - 3, 0, 0, mc("purple_carpet"));
        m.set(m.len() - 1, m.depth() - 2, 0, mc("brewing_stand"));
        m.set(m.len() - 1, m.depth() - 3, 0, mc("cauldron"));
        m.set(0, m.depth() - 2, 0, mc("bookshelf"));
        AsgarniaRoom up = m.level(1);
        up.bed(up.len() - 1, 0, "purple");
        up.fill(1, 0, 3, 0, 0, 1, mc("bookshelf"));
        up.table(up.len() / 2, up.depth() / 2, "dark_oak");
        up.set(1, up.depth() - 1, 0, mc("enchanting_table"));
        k.path(2917, 3326, 2918, 3330, mc("dirt_path"), mc("gravel"));
        k.flowers(2912, 3326, 2915, 3328);
        k.flowers(2920, 3326, 2924, 3328);
        k.fenceRing(2912, 3316, 2924, 3329, mc("oak_fence"));
        k.clearT(2917, 3329, 2918, 3329, 0, 0);
        return k.build();
    }
}
