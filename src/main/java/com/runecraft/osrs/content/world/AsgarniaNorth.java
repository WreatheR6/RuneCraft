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

/**
 * Northern Asgarnia: Taverley and the druids' stone circle, Burthorpe with its castle and the
 * Warriors' and Heroes' Guilds, Goblin Village, the Black Knights' Fortress and Ice Mountain.
 */
final class AsgarniaNorth {
    static final AsgarniaKit.Style STONE = new AsgarniaKit.Style(mc("stone_bricks"), mc("polished_andesite"),
            mc("oak_planks"), mc("cobblestone"), mc("spruce_stairs"), mc("spruce_planks"), mc("glass_pane"), false);
    static final AsgarniaKit.Style STONE2 = new AsgarniaKit.Style(mc("cobblestone"), mc("stripped_oak_log[axis=y]"),
            mc("spruce_planks"), mc("stone_bricks"), mc("dark_oak_stairs"), mc("dark_oak_planks"), mc("glass_pane"), false);
    static final AsgarniaKit.Style BURTH = new AsgarniaKit.Style(mc("polished_andesite"), mc("stone_bricks"),
            mc("spruce_planks"), mc("stone_bricks"), mc("dark_oak_stairs"), mc("dark_oak_planks"), mc("glass_pane"), false);
    static final AsgarniaKit.Style WARRIOR = new AsgarniaKit.Style(mc("stone_bricks"), mc("chiseled_stone_bricks"),
            mc("polished_andesite"), mc("polished_andesite"), mc("stone_brick_stairs"), mc("stone_bricks"),
            mc("glass_pane"), false);
    static final AsgarniaKit.Style HERO = new AsgarniaKit.Style(mc("stone_bricks"), mc("polished_granite"),
            mc("dark_oak_planks"), mc("polished_granite"), mc("deepslate_tile_stairs"), mc("deepslate_tiles"),
            mc("glass_pane"), false);
    static final AsgarniaKit.Style GOBLIN = new AsgarniaKit.Style(mc("packed_mud"), mc("spruce_log[axis=y]"),
            mc("coarse_dirt"), mc("mud_bricks"), mc("mud_brick_stairs"), mc("mud_bricks"), mc("spruce_fence"), false);
    static final AsgarniaKit.Style BLACK = new AsgarniaKit.Style(mc("polished_blackstone_bricks"), mc("deepslate_bricks"),
            mc("dark_oak_planks"), mc("polished_blackstone"), mc("deepslate_tile_stairs"), mc("deepslate_tiles"),
            mc("red_stained_glass_pane"), false);
    static final AsgarniaKit.Style WITCH = new AsgarniaKit.Style(mc("deepslate_bricks"), mc("dark_oak_log[axis=y]"),
            mc("dark_oak_planks"), mc("cobbled_deepslate"), mc("dark_oak_stairs"), mc("dark_oak_planks"),
            mc("purple_stained_glass_pane"), true);

    private AsgarniaNorth() {
    }

    static void build(List<StructureDef> out) {
        out.add(taverley());
        out.add(witchsTower());
        out.add(witchsHouse());
        out.add(lavaEelCave());
        out.add(taverleyTreePatch());
        out.add(druidsCircle());
        out.add(burthorpe());
        out.add(burthorpeCastle());
        out.add(roguesDen());
        out.add(warriorsGuild());
        out.add(heroesGuild());
        out.add(goblinVillage());
        out.add(blackKnightsFortress());
        out.add(iceMountain());
    }

    /** A low stone wall along a tile line. */
    static void lowWall(AsgarniaKit k, int tx1, int ty1, int tx2, int ty2) {
        int[] r = rect(tx1, ty1, tx2, ty2);
        k.f(r[0], -1, r[1], r[2], 1, r[3], mc("stone_bricks"));
        k.f(r[0], 2, r[1], r[2], 2, r[3], mc("mossy_cobblestone_wall"));
    }

    /** A thick castle wall with walkway and crenellations on both edges. */
    static void castleWall(AsgarniaKit k, int x1, int z1, int x2, int z2, int h, String block) {
        k.w(x1, -3, z1, x2, h - 1, z2, block);
        k.w(x1 + 1, -3, z1 + 1, x2 - 1, h - 1, z2 - 1, block);
        for (int x = x1; x <= x2; x += 2) {
            k.s(x, h, z1, block);
            k.s(x, h, z2, block);
        }
        for (int z = z1; z <= z2; z += 2) {
            k.s(x1, h, z, block);
            k.s(x2, h, z, block);
        }
    }

    // ------------------------------------------------------------------ Taverley

    static StructureDef taverley() {
        StructureDef.Builder b = StructureDef.builder("taverley", "Taverley").replaces("TAVERLEY");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2874, 3414, 2942, 3492);
        k.path(2896, 3414, 2898, 3462, mc("dirt_path"), mc("gravel"));
        k.path(2876, 3448, 2936, 3450, mc("dirt_path"), mc("gravel"));
        k.path(2899, 3462, 2900, 3468, mc("dirt_path"), mc("gravel"));
        k.path(2892, 3442, 2903, 3447, mc("grass_block"), mc("moss_block"));
        // walls: east wall with the Falador gate and the south wall
        lowWall(k, 2934, 3418, 2934, 3446);
        lowWall(k, 2934, 3452, 2934, 3462);
        lowWall(k, 2876, 3416, 2894, 3416);
        lowWall(k, 2900, 3416, 2934, 3416);
        for (int y : new int[]{3447, 3451}) {
            k.fillT(2934, y, 2934, y, -1, 3, mc("stone_bricks"));
            k.st(2934, y, 4, mc("lantern"));
        }
        // the well
        int wx = bx(2901);
        int wz = bz(3440);
        k.ring(wx, wz, 2, -1, 0, mc("cobblestone_wall"));
        k.disc(wx, wz, 1.4, -6, -1, mc("water"));
        k.f(wx - 2, 1, wz, wx - 2, 2, wz, mc("spruce_fence"));
        k.f(wx + 2, 1, wz, wx + 2, 2, wz, mc("spruce_fence"));
        k.f(wx - 2, 3, wz, wx + 2, 3, wz, mc("spruce_slab[type=bottom]"));
        // garden plot (Taverley has no herb patch in OSRS; its farming patch is the tree patch south-east of town)
        k.floor(2903, 3435, 2906, 3437, mc("coarse_dirt"));
        k.fenceRing(2903, 3435, 2906, 3437, mc("spruce_fence"));
        k.clearT(2904, 3437, 2904, 3437, 0, 0);
        k.st(2905, 3436, 0, mc("potted_fern"));
        // shops
        AsgarniaRoom jatix = k.house(STONE, 2895, 3425, 2901, 3431, 1, "north", PITCHED);
        jatix.shop(rc("apothecary_table"), mc("spruce_slab[type=top]"), mc("brewing_stand"), mc("flower_pot"),
                mc("potted_fern"), mc("barrel[facing=up]"));
        jatix.set(jatix.len() - 1, jatix.depth() - 1, 0, mc("water_cauldron[level=3]"));
        AsgarniaRoom gem = k.house(STONE2, 2884, 3436, 2890, 3442, 1, "east", PITCHED);
        gem.shop(rc("gem_stall"), mc("spruce_slab[type=top]"), mc("amethyst_block"), mc("chest[facing=east]"), mc("emerald_block"));
        AsgarniaRoom gaius = k.house(STONE, 2884, 3452, 2890, 3458, 1, "east", PITCHED);
        gaius.shop(rc("weapon_shop"), mc("spruce_slab[type=top]"), mc("grindstone[face=floor,facing=north]"),
                mc("iron_block"), mc("barrel[facing=up]"));
        AsgarniaRoom slayer = k.house(STONE2, 2906, 3424, 2913, 3431, 1, "north", PITCHED);
        slayer.shop(rc("slayer_master"), mc("dark_oak_slab[type=top]"), mc("skeleton_skull"), mc("bookshelf"),
                mc("chest[facing=north]"));
        // houses
        k.house(STONE, 2878, 3422, 2884, 3428, 1, "east", PITCHED).home("spruce", "green");
        k.house(STONE2, 2915, 3431, 2922, 3438, 2, "west", PITCHED).home("spruce", "brown");
        k.house(STONE, 2913, 3453, 2921, 3459, 1, "south", PITCHED).home("oak", "lime");
        k.house(STONE2, 2876, 3436, 2881, 3443, 1, "east", PITCHED).home("spruce", "yellow");
        k.house(STONE, 2926, 3422, 2932, 3428, 1, "west", PITCHED).home("oak", "light_gray");
        k.house(STONE2, 2924, 3453, 2931, 3459, 2, "west", PITCHED).home("spruce", "cyan");
        // trees and flowers
        int[][] trees = {{2880, 3418}, {2890, 3420}, {2910, 3418}, {2928, 3434}, {2878, 3460}, {2884, 3478},
                {2912, 3464}, {2880, 3474}, {2934, 3474}, {2886, 3486}, {2940, 3456}};
        for (int i = 0; i < trees.length; i++) {
            k.tree(bx(trees[i][0]), bz(trees[i][1]), i % 2 == 0 ? "oak" : "birch", 6 + i % 2);
        }
        k.flowers(2891, 3430, 2893, 3432);
        return k.build();
    }

    static StructureDef witchsTower() {
        StructureDef.Builder b = StructureDef.builder("taverley_witchs_tower", "Witch's tower").replaces("grim_tales_tower");
        AsgarniaKit k = new AsgarniaKit(b);
        int cx = bx(2906) + 1;
        int cz = bz(3452);
        k.tower(cx, cz, 5, 24, mc("mossy_stone_bricks"), mc("dark_oak_planks"), "south", mc("deepslate_tiles"));
        k.s(cx - 3, 0, cz, mc("water_cauldron[level=3]"));
        k.s(cx - 3, 0, cz + 1, mc("brewing_stand"));
        k.f(cx + 3, 0, cz - 1, cx + 3, 2, cz + 1, mc("bookshelf"));
        k.s(cx - 2, 5, cz + 2, mc("cobweb"));
        k.s(cx + 2, 5, cz + 2, mc("white_carpet"));
        k.s(cx + 2, 5, cz + 1, mc("purple_carpet"));
        k.s(cx - 3, 10, cz, mc("chest[facing=east]"));
        k.s(cx + 3, 10, cz, mc("enchanting_table"));
        k.path(2905, 3446, 2906, 3449, mc("dirt_path"), mc("gravel"));
        return k.build();
    }

    static StructureDef witchsHouse() {
        StructureDef.Builder b = StructureDef.builder("taverley_witchs_house", "Witch's house").replaces("witchs_house");
        AsgarniaKit k = new AsgarniaKit(b);
        AsgarniaRoom h = k.house(WITCH, 2887, 3464, 2895, 3472, 2, "east", PITCHED);
        h.set(h.len() / 2, 2, 0, mc("water_cauldron[level=3]"));
        h.fill(1, 0, 4, 0, 0, 2, mc("bookshelf"));
        h.set(5, 0, 0, mc("brewing_stand"));
        h.set(h.len() - 1, 0, 0, mc("cobweb"));
        h.set(h.len() - 1, h.depth() - 1, 0, mc("potted_dead_bush"));
        h.set(1, h.depth() - 1, 0, mc("cauldron"));
        AsgarniaRoom up = h.level(1);
        up.bed(up.len() - 1, 0, "black");
        up.table(up.len() / 2, up.depth() / 2, "dark_oak");
        up.set(1, up.depth() - 1, 0, mc("chest[facing=east]"));
        // the garden with its fence, pumpkins and the shed
        k.floor(2896, 3462, 2903, 3474, mc("podzol"));
        k.fenceRing(2896, 3462, 2903, 3474, mc("dark_oak_fence"));
        k.clearT(2899, 3462, 2900, 3462, 0, 0);
        k.st(2899, 3466, 0, mc("pumpkin"));
        k.clearT(2896, 3467, 2896, 3468, 0, 0);
        k.st(2898, 3471, 0, mc("carved_pumpkin[facing=west]"));
        k.st(2897, 3464, 0, mc("dead_bush"));
        k.st(2902, 3465, 0, mc("sweet_berry_bush[age=2]"));
        AsgarniaRoom shed = k.house(WITCH, 2900, 3471, 2903, 3474, 1, "west", PITCHED);
        shed.set(0, 0, 0, mc("barrel[facing=up]"));
        shed.set(shed.len() - 1, 0, 0, mc("chest[facing=east]"));
        return k.build();
    }

    static StructureDef lavaEelCave() {
        StructureDef.Builder b = StructureDef.builder("taverley_dungeon_lava_cave", "Taverley lava cave")
                .replaces("taverley_dungeon_lava_eels");
        AsgarniaKit k = new AsgarniaKit(b);
        int cx = bx(2898);
        int cz = bz(3481);
        for (int y = 0; y < 5; y++) {
            k.disc(cx, cz, 5 - y, y, y, y % 2 == 0 ? mc("stone") : mc("andesite"));
        }
        k.disc(cx, cz, 1.5, 4, 4, mc("mossy_cobblestone"));
        // tunnel into the outcrop from the south
        k.c(cx - 1, 0, cz, cx + 1, 2, cz + 6);
        // chamber below with a lava pool
        k.f(cx - 4, -9, cz - 5, cx + 4, -9, cz + 2, mc("blackstone"));
        k.w(cx - 4, -8, cz - 5, cx + 4, -1, cz + 2, mc("basalt[axis=y]"));
        k.c(cx - 3, -8, cz - 4, cx + 3, -2, cz + 1);
        k.f(cx - 3, -1, cz - 4, cx + 3, -1, cz + 1, mc("stone"));
        k.c(cx, -1, cz + 1, cx, -1, cz + 1);
        k.f(cx, -8, cz + 1, cx, -1, cz + 1, mc("ladder[facing=north]"));
        k.f(cx - 3, -9, cz - 4, cx + 3, -9, cz - 2, mc("magma_block"));
        k.f(cx - 2, -9, cz - 4, cx + 2, -9, cz - 3, mc("lava"));
        k.s(cx, -8, cz - 3, rc("lava_eel_fishing_spot"));
        k.f(cx - 3, -5, cz + 1, cx - 3, -2, cz + 1, mc("chain"));
        k.s(cx - 3, -6, cz + 1, mc("lantern[hanging=true]"));
        return k.build();
    }

    static StructureDef taverleyTreePatch() {
        StructureDef.Builder b = StructureDef.builder("taverley_tree_patch", "Taverley tree patch").replaces("taverley_tree_patch");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2936, 3436, 2940, 3440, mc("coarse_dirt"));
        k.fenceRing(2936, 3436, 2940, 3440, mc("spruce_fence"));
        k.clearT(2936, 3438, 2936, 3438, 0, 0);
        k.st(2938, 3438, 0, rc("tree_patch"));
        k.st(2939, 3436, 0, rc("compost_bin"));
        return k.build();
    }

    static StructureDef druidsCircle() {
        StructureDef.Builder b = StructureDef.builder("taverley_druids_circle", "Druids' stone circle");
        AsgarniaKit k = new AsgarniaKit(b);
        int cx = bx(2925);
        int cz = bz(3483);
        k.disc(cx, cz, 15, -1, -1, mc("grass_block"));
        k.disc(cx, cz, 11, -1, -1, mc("moss_block"));
        k.ring(cx, cz, 8, -1, -1, mc("stone_bricks"));
        String[] st = {mc("stone"), mc("mossy_cobblestone"), mc("andesite"), mc("cobblestone")};
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            int x = cx + (int) Math.round(13 * Math.cos(a));
            int z = cz + (int) Math.round(13 * Math.sin(a));
            k.f(x, 0, z, x + 1, 3 + (i % 3 == 0 ? 1 : 0), z + 1, st[i % st.length]);
            k.s(x, 4 + (i % 3 == 0 ? 1 : 0), z, mc("moss_carpet"));
        }
        // altar of Guthix in the centre
        k.f(cx - 2, 0, cz - 1, cx + 2, 0, cz + 1, mc("polished_andesite"));
        k.s(cx, 1, cz, rc("prayer_altar"));
        k.s(cx - 2, 1, cz, mc("water_cauldron[level=3]"));
        k.s(cx + 2, 1, cz, mc("candle[candles=4,lit=true]"));
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            k.s(cx + (int) Math.round(6 * Math.cos(a)), 0, cz + (int) Math.round(6 * Math.sin(a)), mc("fern"));
        }
        k.tree(cx - 18, cz - 6, "oak", 7);
        k.tree(cx + 17, cz + 8, "oak", 6);
        return k.build();
    }

    // ------------------------------------------------------------------ Burthorpe

    static StructureDef burthorpe() {
        StructureDef.Builder b = StructureDef.builder("burthorpe", "Burthorpe").replaces("BURTHORPE");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2850, 3500, 2924, 3574);
        k.path(2892, 3536, 2904, 3551, mc("cobblestone"), mc("stone_bricks"));
        k.path(2896, 3517, 2898, 3536, mc("dirt_path"), mc("gravel"));
        k.path(2899, 3517, 2910, 3518, mc("dirt_path"), mc("gravel"));
        k.path(2872, 3547, 2891, 3549, mc("dirt_path"), mc("gravel"));
        k.path(2904, 3533, 2918, 3535, mc("dirt_path"), mc("gravel"));
        k.path(2904, 3552, 2912, 3554, mc("dirt_path"), mc("gravel"));
        k.path(2884, 3552, 2885, 3576, mc("dirt_path"), mc("gravel"));
        // a market cross on the square
        int mx = bx(2895);
        int mz = bz(3539);
        k.f(mx, 0, mz, mx, 3, mz, mc("stone_brick_wall"));
        k.s(mx, 4, mz, mc("lantern"));
        k.f(mx - 1, -1, mz - 1, mx + 1, -1, mz + 1, mc("chiseled_stone_bricks"));
        // the Toad and Chicken
        AsgarniaRoom pub = k.house(BURTH, 2900, 3526, 2908, 3532, 2, "north", PITCHED);
        pub.inn("spruce");
        AsgarniaRoom rooms = pub.level(1);
        rooms.bed(rooms.len() - 1, 0, "red");
        rooms.bed(rooms.len() - 3, 0, "green");
        rooms.table(4, rooms.depth() - 3, "spruce");
        // Turael's house and other houses
        AsgarniaRoom turael = k.house(BURTH, 2884, 3538, 2890, 3545, 1, "east", PITCHED);
        turael.shop(rc("slayer_master"), mc("spruce_slab[type=top]"), mc("skeleton_skull"), mc("chest[facing=east]"),
                mc("bookshelf"));
        k.house(BURTH, 2884, 3528, 2890, 3534, 2, "east", PITCHED).home("spruce", "blue");
        k.house(STONE2, 2907, 3540, 2913, 3546, 1, "west", PITCHED).home("spruce", "red");
        k.house(BURTH, 2876, 3520, 2881, 3526, 1, "east", PITCHED).home("oak", "brown");
        k.house(STONE2, 2914, 3540, 2920, 3546, 2, "west", PITCHED).home("spruce", "orange");
        // the Games Room
        AsgarniaRoom games = k.house(BURTH, 2914, 3556, 2921, 3564, 1, "west", PITCHED);
        games.set(1, 0, 0, rc("bank_booth"));
        games.set(2, 0, 0, rc("bank_booth"));
        for (int u = 4; u < games.len() - 1; u += 4) {
            for (int v = 3; v < games.depth() - 2; v += 4) {
                games.set(u, v, 0, mc("spruce_slab[type=top]"));
                games.set(u, v, 1, mc((u + v) % 8 == 0 ? "black_carpet" : "white_carpet"));
                games.set(u - 1, v, 0, stair(mc("spruce_stairs"), games.minusU()));
                games.set(u + 1, v, 0, stair(mc("spruce_stairs"), games.plusU()));
            }
        }
        games.set(games.len() - 1, 0, 0, mc("jukebox"));
        // Death Plateau path climbing north-west
        for (int i = 0; i < 12; i++) {
            int tx = 2884 - 2 * i;
            int ty = 3576 + i;
            int h = i / 2;
            k.fillT(tx, ty, tx + 1, ty, -4, h - 1, mc("cobblestone"));
            k.fillT(tx, ty, tx + 1, ty, h - 1, h - 1, i % 2 == 0 ? mc("gravel") : mc("stone_bricks"));
            k.clearT(tx, ty, tx + 1, ty, h, h + 4);
            k.st(tx, ty + 1, h, mc("spruce_fence"));
            if (i % 4 == 0) {
                k.st(tx, ty + 1, h + 1, mc("lantern"));
            }
        }
        k.fillT(2860, 3587, 2862, 3589, -3, 5, mc("cobblestone"));
        k.fillT(2861, 3588, 2861, 3588, 6, 7, mc("mossy_cobblestone_wall"));
        // trees
        int[][] trees = {{2880, 3550}, {2876, 3560}, {2922, 3530}, {2920, 3550}, {2856, 3520}, {2856, 3565}, {2910, 3504},
                {2878, 3510}, {2922, 3570}};
        for (int[] t : trees) {
            k.tree(bx(t[0]), bz(t[1]), "spruce", 8);
        }
        return k.build();
    }

    static StructureDef burthorpeCastle() {
        StructureDef.Builder b = StructureDef.builder("burthorpe_castle", "Burthorpe Castle").replaces("castle_wars_knights");
        AsgarniaKit k = new AsgarniaKit(b);
        int[] r = rect(2886, 3552, 2910, 3572);
        String sb = mc("stone_bricks");
        k.f(r[0], -1, r[1], r[2], -1, r[3], mc("cobblestone"));
        castleWall(k, r[0], r[1], r[2], r[3], 9, sb);
        int[][] corners = {{r[0] + 1, r[1] + 1}, {r[2] - 1, r[1] + 1}, {r[0] + 1, r[3] - 1}, {r[2] - 1, r[3] - 1}};
        for (int[] c : corners) {
            k.tower(c[0], c[1], 4, 15, sb, mc("spruce_planks"), null, null);
            k.s(c[0], 16, c[1], mc("red_banner"));
        }
        // south gate
        int[] gr = rect(2897, 3552, 2899, 3552);
        k.c(gr[0], 0, r[3] - 1, gr[2], 4, r[3]);
        k.f(gr[0], 5, r[3], gr[2], 5, r[3], mc("iron_bars"));
        k.s(gr[0] - 1, 3, r[3] + 1, mc("red_wall_banner[facing=south]"));
        k.s(gr[2] + 1, 3, r[3] + 1, mc("red_wall_banner[facing=south]"));
        // training dummies of the Imperial Guard in the ward west of the keep
        for (int ty : new int[]{3558, 3562, 3566}) {
            k.st(2888, ty, 0, mc("spruce_fence"));
            k.st(2888, ty, 1, mc("hay_block"));
            k.st(2888, ty, 2, mc("carved_pumpkin[facing=east]"));
        }
        k.path(2897, 3553, 2899, 3557, mc("stone_bricks"), mc("cobblestone"));
        // the keep as on the OSRS map (mejrs layers_osrs, planes 0-1): a cross-shaped two-storey hall with the
        // chequered floor (2893-2904 x 3558-3569, red-carpeted upstairs) and four corner rooms 2891-2895 /
        // 2902-2906 x 3555-3560 / 3566-3571, each with its ladder; the door south at 2898-2899, the stairs
        // up at 2897-2898,3565 (to 3569 upstairs) and the stairs down to the Games Room at 2899-2900,3565
        // (shortest-path; the Games Room itself is not modelled)
        String[] plan = {
                "aaaaa      bbbbb", // 3571
                "aaaaa      bbbbb", // 3570
                "aaaaaccccccbbbbb", // 3569
                "aaaaaccccccbbbbb", // 3568
                "aaaaaccccccbbbbb", // 3567
                "aaaaaccccccbbbbb", // 3566
                "  cccccccccccc", // 3565
                "  cccccccccccc", // 3564
                "  cccccccccccc", // 3563
                "  cccccccccccc", // 3562
                "  cccccccccccc", // 3561
                "dddddcccccceeeee", // 3560
                "dddddcccccceeeee", // 3559
                "dddddcccccceeeee", // 3558
                "ddddd      eeeee", // 3557
                "ddddd      eeeee", // 3556
                "ddddd      eeeee", // 3555
        };
        TileMask.of(2891, 3571, plan).fill(b, k.g - 1, k.g - 1, mc("spruce_planks")).fill(b, k.g, k.g + 12, null);
        for (int tx = 2893; tx <= 2904; tx++) {
            for (int ty = 3558; ty <= 3569; ty++) {
                k.fillT(tx, ty, tx, ty, -1, -1, ((tx + ty) & 1) == 0 ? mc("polished_andesite") : mc("polished_blackstone"));
            }
        }
        TileMask hall = TileMask.of(2891, 3571, "c", plan);
        hall.fill(b, k.g + 3, k.g + 3, mc("red_wool")).fill(b, k.g + 7, k.g + 7, mc("stone_brick_slab"))
                .outline(b, k.g, k.g + 7, sb).crenels(b, k.g + 8, sb);
        for (String c : new String[]{"a", "b", "d", "e"}) {
            TileMask.of(2891, 3571, c, plan).fill(b, k.g + 3, k.g + 3, mc("spruce_planks"))
                    .fill(b, k.g + 7, k.g + 7, mc("spruce_planks")).fill(b, k.g + 11, k.g + 11, mc("stone_brick_slab"))
                    .outline(b, k.g, k.g + 11, sb).crenels(b, k.g + 12, sb);
        }
        // doors: the corner rooms open onto the hall on both floors, the hall's door south
        for (int y = 0; y <= 4; y += 4) {
            k.clearT(2895, 3567, 2896, 3567, y, y + 1);
            k.clearT(2901, 3567, 2902, 3567, y, y + 1);
            k.clearT(2895, 3559, 2896, 3559, y, y + 1);
            k.clearT(2901, 3559, 2902, 3559, y, y + 1);
        }
        k.clearT(2898, 3558, 2899, 3558, 0, 2);
        k.s(bx(2898), 0, bz(3558) + 1, mc("spruce_door[facing=north,half=lower,hinge=left]"));
        k.s(bx(2899) + 1, 0, bz(3558) + 1, mc("spruce_door[facing=north,half=lower,hinge=right]"));
        // the staircase up and the trapdoor down to the Games Room
        for (int x = bx(2897); x <= bx(2898) + 1; x++) {
            for (int i = 0; i < 3; i++) {
                k.c(x, i + 1, bz(3565) - i, x, i + 3, bz(3565) - i);
                k.s(x, i, bz(3565) - i, stair(mc("stone_brick_stairs"), "north"));
                if (i > 0) {
                    k.f(x, 0, bz(3565) - i, x, i - 1, bz(3565) - i, sb);
                }
            }
        }
        k.st(2899, 3564, 0, mc("spruce_trapdoor[half=bottom,open=false]"));
        k.st(2900, 3564, 0, mc("spruce_trapdoor[half=bottom,open=false]"));
        // ladders in the corner rooms (OSRS 2892,3556 / 2905,3556 / 2905,3571 / 2892,3571), ground to top
        for (int[] l : new int[][]{{bx(2891) + 1, bz(3556), 0}, {bx(2906), bz(3556), 1}, {bx(2906), bz(3570), 1}, {bx(2891) + 1, bz(3570), 0}}) {
            k.f(l[0], 0, l[1], l[0], 7, l[1], mc(l[2] == 0 ? "ladder[facing=east]" : "ladder[facing=west]"));
        }
        // Prince Anlaf's throne at the north end of the hall, banners; upstairs the feast table, the
        // armoury (anvil) in the north-east room and the guards' beds in the south-west room
        k.fillT(2898, 3569, 2899, 3569, 0, 0, mc("spruce_slab[type=bottom]"));
        k.s(bx(2898) + 1, 1, bz(3569), stair(mc("dark_oak_stairs"), "south"));
        k.s(bx(2899), 1, bz(3569), mc("gold_block"));
        k.fillT(2898, 3559, 2899, 3568, -1, -1, mc("red_wool"));
        for (int tx = 2894; tx <= 2903; tx += 3) {
            k.s(bx(tx), 2, bz(3565), mc("lantern[hanging=true]"));
        }
        k.f(bx(2901), 4, bz(3563), bx(2903), 4, bz(3563), mc("dark_oak_slab[type=top]"));
        k.f(bx(2901), 4, bz(3562), bx(2903), 4, bz(3562), stair(mc("dark_oak_stairs"), "north"));
        k.st(2904, 3569, 4, rc("anvil"));
        k.st(2903, 3570, 4, mc("smithing_table"));
        for (int ty : new int[]{3557, 3559}) {
            k.s(bx(2893), 4, bz(ty), mc("red_bed[facing=west,part=foot]"));
        }
        k.f(bx(2892), 4, bz(3567), bx(2892), 5, bz(3569), mc("bookshelf"));
        return k.build();
    }

    static StructureDef roguesDen() {
        StructureDef.Builder b = StructureDef.builder("burthorpe_rogues_den", "Rogues' Den entrance").replaces("mg_rogues_den");
        AsgarniaKit k = new AsgarniaKit(b);
        int[] r = rect(2911, 3520, 2918, 3528);
        k.f(r[0], -1, r[1], r[2], -1, r[3], mc("cobblestone"));
        k.w(r[0], 0, r[1], r[2], 0, r[3], mc("cobblestone_wall"));
        k.clearT(2914, 3528, 2915, 3528, 0, 0);
        // stair shaft down into the den
        int sx = bx(2912);
        int sz = bz(3521);
        k.c(sx, -8, sz, sx + 7, -1, sz + 2);
        k.w(sx - 1, -9, sz - 1, sx + 8, -1, sz + 3, mc("stone_bricks"));
        k.f(sx - 1, -9, sz - 1, sx + 8, -9, sz + 3, mc("stone_bricks"));
        for (int i = 0; i < 7; i++) {
            k.f(sx + i, -1 - i, sz, sx + i, -1 - i, sz + 2, stair(mc("stone_brick_stairs"), "west"));
            k.f(sx + i, -8, sz, sx + i, -2 - i, sz + 2, mc("stone_bricks"));
        }
        k.f(sx - 1, 0, sz - 1, sx + 8, 0, sz - 1, mc("spruce_fence"));
        k.f(sx + 8, 0, sz, sx + 8, 0, sz + 2, mc("spruce_fence"));
        k.s(sx + 7, -8, sz + 1, rc("rogues_den_entrance"));
        k.s(sx + 7, -8, sz + 2, mc("lantern"));
        k.path(2914, 3529, 2915, 3532, mc("dirt_path"), mc("gravel"));
        // the jewel trader's stall
        int tx = bx(2917);
        int tz = bz(3526);
        k.s(tx, 0, tz, rc("rogues_den_jewel_trader"));
        k.f(tx - 1, 0, tz - 1, tx - 1, 2, tz - 1, mc("spruce_fence"));
        k.f(tx + 1, 0, tz - 1, tx + 1, 2, tz - 1, mc("spruce_fence"));
        k.f(tx - 1, 3, tz - 2, tx + 1, 3, tz, mc("black_wool"));
        return k.build();
    }

    static StructureDef warriorsGuild() {
        // The guild on its OSRS footprint (mejrs layers_osrs): 2837-2876 x 3534-3555, three storeys, entered by the
        // east door at 3546 (shortest-path 2876/2877). Ground floor: two small rooms and the round-pit room along
        // the north (2837-2860 x 3547-3555), the red-carpeted hall (2841-2848 x 3535-3546) and the dining room
        // with Lidio (2849-2860 x 3535-3546), the big animation room east (2861-2876); first floor: the dummy
        // room north-west, the shot put lanes north-east (2861-2876 x 3546-3555); top floor: the cyclops hall.
        StructureDef.Builder b = StructureDef.builder("warriors_guild", "Warriors' Guild").replaces("mg_warriors_guild");
        AsgarniaKit k = new AsgarniaKit(b);
        k.path(2877, 3536, 2880, 3556, mc("stone_bricks"), mc("polished_andesite"));
        k.house(WARRIOR, 2837, 3534, 2876, 3555, 3, "east", FLAT);
        String wall = mc("stone_bricks");
        // ground-floor partitions (doors as on the map)
        k.f(bx(2838), 0, bz(3547) + 1, bx(2860) + 1, 2, bz(3547) + 1, wall);
        k.f(bx(2844), 0, bz(3555), bx(2844), 2, bz(3547) + 1, wall);
        k.f(bx(2851), 0, bz(3555), bx(2851), 2, bz(3547) + 1, wall);
        k.f(bx(2861), 0, bz(3555), bx(2861), 2, bz(3535) + 1, wall);
        k.f(bx(2849), 0, bz(3546), bx(2849), 2, bz(3535) + 1, wall);
        k.c(bx(2840), 0, bz(3547) + 1, bx(2840) + 1, 1, bz(3547) + 1);
        k.c(bx(2847), 0, bz(3547) + 1, bx(2847) + 1, 1, bz(3547) + 1);
        k.c(bx(2854), 0, bz(3547) + 1, bx(2855) + 1, 1, bz(3547) + 1);
        k.c(bx(2849), 0, bz(3541), bx(2849), 1, bz(3540) + 1);
        k.c(bx(2861), 0, bz(3546), bx(2861), 1, bz(3545) + 1);
        k.c(bx(2861), 0, bz(3541), bx(2861), 1, bz(3540) + 1);
        // the entrance just inside the east door, Anton's weapon counter in the hall, Lidio's range and
        // round tables in the dining room, the five animator pedestals in the east room
        k.st(2875, 3546, 0, rc("warriors_guild_entrance"));
        k.f(bx(2842), 0, bz(3538), bx(2846) + 1, 0, bz(3538), mc("spruce_slab[type=top]"));
        k.st(2844, 3538, 0, rc("weapon_shop"));
        k.f(bx(2842), 0, bz(3536) + 1, bx(2846) + 1, 0, bz(3536) + 1, mc("grindstone[face=floor,facing=north]"));
        k.f(bx(2843), -1, bz(3545), bx(2846) + 1, -1, bz(3539), mc("red_wool"));
        k.f(bx(2851), 0, bz(3545), bx(2859) + 1, 0, bz(3545), mc("spruce_slab[type=top]"));
        k.st(2855, 3545, 0, rc("cooking_range"));
        k.f(bx(2851), 0, bz(3546), bx(2859) + 1, 0, bz(3546), mc("barrel[facing=up]"));
        k.c(bx(2854), 0, bz(3546), bx(2855) + 1, 0, bz(3545));
        for (int tx : new int[]{2852, 2858}) {
            k.st(tx, 3537, 0, mc("spruce_fence"));
            k.st(tx, 3537, 1, mc("spruce_pressure_plate"));
        }
        String[] armour = {"iron_block", "gold_block", "diamond_block", "emerald_block", "netherite_block"};
        for (int i = 0; i < armour.length; i++) {
            int tx = 2863 + i * 3;
            k.st(tx, 3551, 0, mc("polished_andesite"));
            k.st(tx, 3551, 1, mc(armour[i]));
        }
        k.f(bx(2862), -1, bz(3546), bx(2874) + 1, -1, bz(3546) + 1, mc("red_wool"));
        // the round pit in the north room
        int px = bx(2856);
        int pz = bz(3551);
        k.disc(px, pz, 2.5, -1, -1, mc("sand"));
        k.ring(px, pz, 3.5, 0, 0, mc("spruce_fence"));
        k.c(px, 0, pz + 3, px, 0, pz + 3);
        // first floor: the dummy room north-west and the shot put lanes north-east
        k.f(bx(2849), 4, bz(3555), bx(2849), 6, bz(3544) + 1, wall);
        k.f(bx(2838), 4, bz(3544) + 1, bx(2848) + 1, 6, bz(3544) + 1, wall);
        k.f(bx(2859), 4, bz(3555), bx(2859), 6, bz(3546), wall);
        k.f(bx(2860), 4, bz(3546), bx(2875) + 1, 6, bz(3546), wall);
        k.c(bx(2849), 4, bz(3549), bx(2849), 5, bz(3548) + 1);
        k.c(bx(2859), 4, bz(3549), bx(2859), 5, bz(3548) + 1);
        k.c(bx(2846), 4, bz(3544) + 1, bx(2846) + 1, 5, bz(3544) + 1);
        for (int tx = 2839; tx <= 2847; tx += 2) {
            k.st(tx, 3552, 4, mc("hay_block"));
            k.st(tx, 3552, 5, mc("target"));
        }
        k.f(bx(2862), 4, bz(3549), bx(2875) + 1, 4, bz(3549), mc("smooth_stone_slab[type=bottom]"));
        k.f(bx(2862), 4, bz(3553), bx(2875) + 1, 4, bz(3553), mc("smooth_stone_slab[type=bottom]"));
        k.st(2861, 3551, 4, mc("cobblestone"));
        k.st(2861, 3547, 4, mc("cobblestone"));
        // top floor: the cyclops hall behind its barred gate (approx: the gate across the hall at 2848)
        k.f(bx(2848), 8, bz(3555), bx(2848), 10, bz(3535) + 1, mc("iron_bars"));
        k.st(2848, 3545, 8, rc("warriors_guild_cyclops_entrance"));
        k.f(bx(2850), 7, bz(3545), bx(2874) + 1, 7, bz(3545) + 1, mc("red_wool"));
        // banners on the east front
        for (int z = bz(3555) + 2; z <= bz(3534) - 1; z += 6) {
            k.s(bx(2876) + 2, 5, z, mc("red_wall_banner[facing=east]"));
        }
        return k.build();
    }

    static StructureDef heroesGuild() {
        StructureDef.Builder b = StructureDef.builder("heroes_guild", "Heroes' Guild").replaces("heroes_guild");
        AsgarniaKit k = new AsgarniaKit(b);
        k.path(2902, 3504, 2910, 3516, mc("stone_bricks"), mc("polished_andesite"));
        AsgarniaRoom g = k.house(HERO, 2890, 3504, 2901, 3516, 2, "east", PITCHED);
        int len = g.len();
        int dep = g.depth();
        g.longTable(3, len - 4, dep / 2, "dark_oak", mc("red_carpet"));
        g.fill(1, 0, len - 1, 0, 0, 0, mc("chest[facing=east]"));
        g.set(2, 0, 1, mc("dragon_head"));
        g.set(5, 0, 1, mc("wither_skeleton_skull"));
        g.set(8, 0, 1, mc("skeleton_skull"));
        AsgarniaRoom up = g.level(1);
        up.set(1, 0, 0, rc("cooking_range"));
        up.set(3, 0, 0, rc("bank_booth"));
        up.set(4, 0, 0, rc("bank_booth"));
        up.set(6, 0, 0, rc("anvil"));
        up.set(8, 0, 0, rc("furnace"));
        up.table(len / 2, dep / 2, "dark_oak");
        k.tower(g.x1, g.z1, 3, 16, mc("stone_bricks"), mc("dark_oak_planks"), null, mc("deepslate_tiles"));
        // the Fountain of Heroes in the forecourt
        int fx = bx(2909);
        int fz = bz(3513);
        k.fountain(fx, fz, 3, mc("polished_granite"), mc("chiseled_stone_bricks"));
        k.s(fx + 3, 0, fz, mc("air"));
        k.s(fx - 1, 0, fz + 4, rc("fountain_of_heroes"));
        k.s(fx - 4, 0, fz - 3, mc("lantern"));
        k.s(fx - 4, -1, fz - 3, mc("chiseled_stone_bricks"));
        return k.build();
    }

    // ------------------------------------------------------------------ Goblin Village

    static StructureDef goblinVillage() {
        StructureDef.Builder b = StructureDef.builder("goblin_village", "Goblin Village").replaces("goblin_village");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2942, 3490, 2968, 3516, mc("coarse_dirt"));
        k.floor(2950, 3496, 2960, 3504, mc("packed_mud"));
        k.path(2954, 3490, 2955, 3504, mc("dirt_path"), mc("podzol"));
        // the generals' hall and huts
        AsgarniaRoom hall = k.house(GOBLIN, 2950, 3505, 2960, 3513, 1, "south", PITCHED);
        hall.longTable(2, hall.len() - 3, hall.depth() / 2, "spruce", mc("bone_block"));
        hall.set(1, 0, 0, mc("red_banner"));
        hall.set(hall.len() - 2, 0, 0, mc("green_banner"));
        hall.set(hall.len() / 2, 0, 0, mc("chest[facing=south]"));
        k.house(GOBLIN, 2944, 3494, 2949, 3499, 1, "east", PITCHED).home("spruce", "brown");
        k.house(GOBLIN, 2959, 3493, 2965, 3498, 1, "west", PITCHED).home("spruce", "green");
        k.house(GOBLIN, 2962, 3502, 2967, 3508, 1, "west", PITCHED).home("spruce", "red");
        k.house(GOBLIN, 2943, 3502, 2948, 3508, 1, "east", PITCHED).home("spruce", "lime");
        // palisade with gaps
        int[] r = rect(2942, 3490, 2968, 3516);
        k.w(r[0], 0, r[1], r[2], 1, r[3], mc("spruce_fence"));
        k.clearT(2954, 3490, 2955, 3490, 0, 1);
        k.clearT(2954, 3516, 2955, 3516, 0, 1);
        k.clearT(2942, 3502, 2942, 3503, 0, 1);
        k.clearT(2968, 3500, 2968, 3501, 0, 1);
        for (int x = r[0]; x <= r[2]; x += 4) {
            k.s(x, 2, r[1], mc("skeleton_skull"));
            k.s(x, 2, r[3], mc("skeleton_skull"));
        }
        // camp fire, bones, mud and the goblin banners
        int fx = bx(2957);
        int fz = bz(3496);
        k.s(fx, 0, fz, mc("campfire[lit=true]"));
        k.ring(fx, fz, 1.5, -1, -1, mc("cobblestone"));
        k.st(2951, 3500, 0, mc("bone_block[axis=x]"));
        k.st(2958, 3501, 0, mc("hay_block"));
        k.st(2952, 3515, 0, mc("bone_block[axis=y]"));
        k.st(2964, 3512, 0, mc("mud"));
        k.st(2946, 3512, 0, mc("mud"));
        k.st(2950, 3504, 0, mc("spruce_fence"));
        k.st(2950, 3504, 1, mc("red_banner"));
        k.st(2959, 3504, 0, mc("spruce_fence"));
        k.st(2959, 3504, 1, mc("green_banner"));
        return k.build();
    }

    // ------------------------------------------------------------------ Black Knights' Fortress

    static StructureDef blackKnightsFortress() {
        StructureDef.Builder b = StructureDef.builder("black_knights_fortress", "Black Knights' Fortress").replaces("black_knights_fortress");
        AsgarniaKit k = new AsgarniaKit(b);
        k.doorWood = "dark_oak";
        k.flatten(3002, 3500, 3034, 3532);
        int[] r = rect(3006, 3504, 3030, 3528);
        String bk = mc("polished_blackstone_bricks");
        k.path(3007, 3505, 3029, 3527, mc("polished_blackstone"), mc("blackstone"));
        castleWall(k, r[0], r[1], r[2], r[3], 12, bk);
        k.w(r[0], -1, r[1], r[2], 1, r[3], mc("cracked_polished_blackstone_bricks"));
        int[][] corners = {{r[0] + 1, r[1] + 1}, {r[2] - 1, r[1] + 1}, {r[0] + 1, r[3] - 1}, {r[2] - 1, r[3] - 1}};
        for (int[] c : corners) {
            k.tower(c[0], c[1], 5, 19, mc("deepslate_bricks"), mc("dark_oak_planks"), null, null);
            k.s(c[0], 20, c[1], mc("black_banner"));
        }
        // west gate with grill
        int[] gr = rect(3006, 3509, 3006, 3511);
        k.c(r[0], 0, gr[1], r[0] + 1, 4, gr[3]);
        k.f(r[0], 0, gr[1], r[0], 3, gr[3], mc("iron_bars"));
        k.c(r[0], 0, gr[1] + 2, r[0], 2, gr[1] + 3);
        k.path(2998, 3509, 3005, 3511, mc("cobbled_deepslate"), mc("blackstone"));
        k.tower(r[0], gr[1] - 4, 3, 15, mc("deepslate_bricks"), mc("dark_oak_planks"), null, null);
        k.tower(r[0], gr[3] + 4, 3, 15, mc("deepslate_bricks"), mc("dark_oak_planks"), null, null);
        for (int z = r[1] + 4; z < r[3]; z += 8) {
            k.s(r[0] - 1, 7, z, mc("black_wall_banner[facing=west]"));
        }
        // the keep: hall, war room and Greldo's cauldron room
        AsgarniaRoom keep = k.house(BLACK, 3019, 3515, 3026, 3524, 3, "west", FLAT);
        keep.longTable(2, keep.len() - 3, keep.depth() / 2, "dark_oak", mc("black_carpet"));
        keep.set(keep.len() - 1, 0, 0, rc("cooking_range"));
        keep.set(keep.len() - 2, 0, 0, mc("barrel[facing=up]"));
        AsgarniaRoom war = keep.level(1);
        war.fill(war.len() / 2 - 2, war.depth() / 2 - 1, war.len() / 2 + 1, war.depth() / 2, 0, 0, mc("dark_oak_slab[type=top]"));
        war.fill(war.len() / 2 - 2, war.depth() / 2 - 1, war.len() / 2 + 1, war.depth() / 2, 1, 1, mc("brown_carpet"));
        war.fill(1, 0, 4, 0, 0, 2, mc("bookshelf"));
        war.set(war.len() - 1, 0, 0, mc("chest[facing=west]"));
        AsgarniaRoom top = keep.level(2);
        top.set(top.len() / 2, top.depth() / 2, 0, mc("water_cauldron[level=3]"));
        top.set(top.len() / 2, top.depth() / 2 + 1, 0, mc("campfire[lit=false]"));
        top.fill(1, 0, 3, 0, 0, 1, mc("brewing_stand"));
        top.set(top.len() - 1, 0, 0, mc("cobweb"));
        // barracks and jail cells
        AsgarniaRoom bar = k.house(BLACK, 3010, 3517, 3016, 3524, 2, "south", FLAT);
        for (int u = 2; u < bar.len(); u += 3) {
            bar.bed(u, 0, "black");
        }
        AsgarniaRoom jail = bar.level(1);
        for (int u = 1; u < jail.len(); u += 3) {
            jail.fill(u, 0, u, 2, 0, 2, mc("iron_bars"));
        }
        jail.fill(1, 3, jail.len() - 1, 3, 0, 2, mc("iron_bars"));
        jail.fill(jail.len() / 2, 3, jail.len() / 2, 3, 0, 1, mc("air"));
        // soul lanterns and an anvil in the yard
        for (int i = 0; i < 4; i++) {
            int x = bx(3010) + i * 8;
            int z = bz(3507);
            k.s(x, 0, z, mc("polished_blackstone_wall"));
            k.s(x, 1, z, mc("soul_lantern"));
        }
        k.st(3026, 3508, 0, rc("anvil"));
        k.st(3027, 3508, 0, mc("grindstone[face=floor,facing=north]"));
        return k.build();
    }

    // ------------------------------------------------------------------ Ice Mountain

    static StructureDef iceMountain() {
        StructureDef.Builder b = StructureDef.builder("ice_mountain", "Ice Mountain");
        AsgarniaKit k = new AsgarniaKit(b);
        // Dwarven Mine north entrance, south of the mountain
        k.flatten(3010, 3440, 3026, 3456);
        int ex = bx(3018);
        int ez = bz(3450);
        for (int y = 0; y < 6; y++) {
            k.disc(ex, ez, 7 - y, y, y, y % 2 == 0 ? mc("stone") : mc("cobblestone"));
        }
        k.c(ex - 1, 0, ez - 4, ex + 1, 3, ez + 7);
        k.f(ex - 2, 0, ez + 6, ex - 2, 3, ez + 6, mc("stripped_spruce_wood"));
        k.f(ex + 2, 0, ez + 6, ex + 2, 3, ez + 6, mc("stripped_spruce_wood"));
        k.f(ex - 2, 4, ez + 6, ex + 2, 4, ez + 6, mc("stripped_spruce_wood"));
        k.c(ex - 1, -8, ez - 4, ex + 1, -1, ez - 1);
        k.f(ex - 2, -9, ez - 5, ex + 2, -9, ez, mc("cobbled_deepslate"));
        k.w(ex - 2, -8, ez - 5, ex + 2, -1, ez, mc("stone"));
        k.f(ex, -8, ez - 4, ex, 0, ez - 4, mc("ladder[facing=south]"));
        k.s(ex + 1, 3, ez - 1, mc("lantern[hanging=true]"));
        k.f(ex - 1, 0, ez + 1, ex - 1, 0, ez + 6, mc("rail[shape=north_south]"));
        k.s(ex - 1, -8, ez - 1, rc("iron_rock"));
        k.s(ex + 1, -8, ez - 1, rc("copper_rock"));
        // The mountain itself (peak ~3010, 3478) is natural terrain from Elevation; the Oracle's
        // standing stones on the summit are built on the real ground by TerrainFeatures.
        return k.build();
    }
}
