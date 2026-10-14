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

/** Southern Asgarnia: Port Sarim, Mudskipper Point, Rimmington, the Crafting Guild and Entrana. */
final class AsgarniaTowns {
    static final AsgarniaKit.Style TIMBER = new AsgarniaKit.Style(mc("oak_planks"), mc("spruce_log[axis=y]"),
            mc("spruce_planks"), mc("stripped_spruce_wood"), mc("dark_oak_stairs"), mc("dark_oak_planks"), mc("glass_pane"), true);
    static final AsgarniaKit.Style TIMBER2 = new AsgarniaKit.Style(mc("birch_planks"), mc("dark_oak_log[axis=y]"),
            mc("oak_planks"), mc("stripped_dark_oak_wood"), mc("spruce_stairs"), mc("spruce_planks"), mc("glass_pane"), true);
    static final AsgarniaKit.Style SARIM_STONE = new AsgarniaKit.Style(mc("stone_bricks"), mc("cobblestone"),
            mc("spruce_planks"), mc("spruce_planks"), mc("dark_oak_stairs"), mc("dark_oak_planks"), mc("glass_pane"), false);
    static final AsgarniaKit.Style RIMM = new AsgarniaKit.Style(mc("mud_bricks"), mc("oak_log[axis=y]"),
            mc("oak_planks"), mc("stripped_oak_wood"), mc("spruce_stairs"), mc("spruce_planks"), mc("glass_pane"), true);
    static final AsgarniaKit.Style GUILD = new AsgarniaKit.Style(mc("bricks"), mc("stone_bricks"),
            mc("oak_planks"), mc("stone_bricks"), mc("dark_oak_stairs"), mc("dark_oak_planks"), mc("glass_pane"), false);
    static final AsgarniaKit.Style ENTRANA = new AsgarniaKit.Style(mc("stone_bricks"), mc("chiseled_stone_bricks"),
            mc("spruce_planks"), mc("mossy_stone_bricks"), mc("dark_oak_stairs"), mc("dark_oak_planks"),
            mc("yellow_stained_glass_pane"), false);

    private AsgarniaTowns() {
    }

    static void build(List<StructureDef> out) {
        out.add(portSarim());
        out.add(portSarimDocks());
        out.add(iceDungeon());
        out.add(thurgo());
        out.add(rimmington());
        out.add(rimmingtonBush());
        out.add(craftingGuild());
        out.add(entrana());
        out.add(entranaHops());
    }

    // ------------------------------------------------------------------ Port Sarim

    static StructureDef portSarim() {
        StructureDef.Builder b = StructureDef.builder("port_sarim", "Port Sarim").replaces("PORT_SARIM");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(3004, 3172, 3036, 3264);
        k.flatten(3036, 3240, 3058, 3264);
        k.harbour(3028, 3172, 3064, 3238);
        // street, north lane and the quay
        k.path(3020, 3174, 3024, 3262, mc("dirt_path"), mc("cobblestone"));
        k.path(3018, 3251, 3039, 3253, mc("dirt_path"), mc("cobblestone"));
        k.floor(3025, 3174, 3027, 3238, mc("spruce_planks"));
        int[] q = rect(3027, 3174, 3027, 3238);
        for (int z = q[1]; z <= q[3]; z += 2) {
            k.s(q[2] + 1, -1, z, mc("spruce_planks"));
            k.f(q[2] + 1, -8, z, q[2] + 1, -2, z, mc("spruce_log[axis=y]"));
        }
        // the OSRS docks (mejrs layers_osrs map, gangplanks from shortest-path): the quay 3026-3029 from 3201
        // to 3240 with its wide landing (3215-3221), and three piers east: 3202-3205, 3234-3237 and the
        // south pier 3191-3194 reached by a walkway at 3030-3032
        k.pier(3028, 3201, 3029, 3240, mc("spruce_planks"), mc("spruce_fence"));
        k.pier(3028, 3215, 3030, 3221, mc("spruce_planks"), mc("spruce_fence"));
        k.pier(3030, 3202, 3050, 3205, mc("spruce_planks"), mc("spruce_fence"));
        k.pier(3030, 3195, 3032, 3201, mc("spruce_planks"), mc("spruce_fence"));
        k.pier(3027, 3191, 3050, 3194, mc("spruce_planks"), mc("spruce_fence"));
        k.pier(3030, 3234, 3052, 3237, mc("spruce_planks"), mc("spruce_fence"));
        // ships at their gangplanks: the Karamja ship off the landing (3032,3217), the monks' ship to
        // Entrana south of the north pier (3048,3231) and a merchant ship south of the south pier (3038,3189)
        k.ship(bx(3031), bz(3217) + 1, 26, 3, true, 1, 2, mc("dark_oak_planks"), mc("spruce_planks"), mc("white_wool"));
        k.ship(bx(3036), bz(3230) + 1, 28, 3, true, 1, 2, mc("spruce_planks"), mc("birch_planks"), mc("yellow_wool"));
        k.ship(bx(3029), bz(3188) + 1, 32, 3, true, 1, 3, mc("dark_oak_planks"), mc("spruce_planks"), mc("light_gray_wool"));
        k.rowboat(bx(3034), bz(3210), true);
        k.rowboat(bx(3036), bz(3225), true);
        // crates and barrels along the quay
        for (int y = 3176; y < 3238; y += 7) {
            k.st(3025, y, 0, mc("barrel[facing=up]"));
            k.st(3025, y + 1, 0, mc("spruce_planks"));
            k.st(3025, y + 1, 1, mc("barrel[facing=up]"));
        }
        for (int y = 3180; y < 3238; y += 12) {
            k.st(3024, y, 0, mc("spruce_fence"));
            k.st(3024, y, 1, mc("spruce_fence"));
            k.st(3024, y, 2, mc("lantern"));
        }

        // jail as on the OSRS map: an L of stone, 3010-3016 x 3179-3196 with the east wing 3017-3020 x 3179-3190;
        // three cells along the south (3179-3181), three cells up the north-west (3188-3196) opening west
        // onto the corridor, the hall 3182-3187 with the guards' room 3017-3020 x 3188-3190; door north at 3011
        String jw = SARIM_STONE.wall();
        k.house(SARIM_STONE, 3010, 3179, 3016, 3196, 1, "north", FLAT);
        k.house(SARIM_STONE, 3017, 3179, 3020, 3190, 1, "east", FLAT);
        k.f(bx(3011), 0, bz(3196), bx(3015) + 1, 2, bz(3196), jw);
        k.f(bx(3020) + 1, 0, bz(3189), bx(3020) + 1, 2, bz(3180) + 1, jw);
        k.c(bx(3011), 0, bz(3196), bx(3011) + 1, 2, bz(3196));
        k.s(bx(3011), 0, bz(3196), mc("spruce_door[facing=south,half=lower,hinge=left,open=false]"));
        k.s(bx(3011), 1, bz(3196), mc("spruce_door[facing=south,half=upper,hinge=left,open=false]"));
        k.c(bx(3016) + 1, 0, bz(3187), bx(3017), 2, bz(3182) + 1);
        // south cells: stone side walls, barred fronts with an open gate each
        k.f(bx(3010) + 1, 0, bz(3181), bx(3020), 2, bz(3181), mc("iron_bars"));
        k.f(bx(3013), 0, bz(3181) + 1, bx(3013), 2, bz(3179) + 1, jw);
        k.f(bx(3017), 0, bz(3181) + 1, bx(3017), 2, bz(3179) + 1, jw);
        for (int cell : new int[]{3011, 3014, 3018}) {
            k.c(bx(cell), 0, bz(3181), bx(cell), 1, bz(3181));
            k.st(cell + 1, 3179, 0, mc("hay_block"));
        }
        // north-west cells off the corridor (3010-3012)
        k.f(bx(3013), 0, bz(3196) + 1, bx(3013), 2, bz(3188) + 1, mc("iron_bars"));
        k.f(bx(3013) + 1, 0, bz(3194) + 1, bx(3016) + 1, 2, bz(3194) + 1, jw);
        k.f(bx(3013) + 1, 0, bz(3191) + 1, bx(3016) + 1, 2, bz(3191) + 1, jw);
        for (int cell : new int[]{3195, 3192, 3189}) {
            k.c(bx(3013), 0, bz(cell), bx(3013), 1, bz(cell));
            k.st(3016, cell, 0, mc("hay_block"));
        }
        // the wall between the hall and the north rooms, with the guards' room door at 3018
        k.f(bx(3013), 0, bz(3188) + 1, bx(3020), 2, bz(3188) + 1, jw);
        k.c(bx(3018), 0, bz(3188) + 1, bx(3018), 1, bz(3188) + 1);
        k.st(3020, 3190, 0, mc("chest[facing=west]"));
        k.st(3019, 3190, 0, mc("barrel[facing=up]"));
        k.st(3016, 3184, 0, mc("spruce_fence"));
        k.st(3016, 3184, 1, mc("spruce_pressure_plate"));
        k.st(3011, 3184, 0, mc("cauldron"));
        // west row of shops and houses
        // Wydin's Food Store (OSRS 3009-3016 x 3203-3210): the shop with the ladder up, and the back room
        // 3009-3011 behind its own door at 3204 (the north-east wall is a diagonal in OSRS // approx)
        AsgarniaRoom wydin = k.house(TIMBER, 3009, 3203, 3016, 3210, 2, "east", PITCHED);
        wydin.shop(rc("general_store"), mc("spruce_slab[type=top]"), mc("barrel[facing=up]"), mc("hay_block"),
                mc("melon"), mc("pumpkin"));
        wydin.set(1, wydin.depth() - 1, 0, mc("cake"));
        wydin.set(wydin.len() - 1, wydin.depth() - 1, 0, mc("chest[facing=west]"));
        k.f(bx(3012), 0, bz(3210) + 1, bx(3012), 2, bz(3203), TIMBER.wall());
        k.c(bx(3012), 0, bz(3204), bx(3012), 2, bz(3204));
        k.st(3009, 3210, 0, mc("barrel[facing=up]"));
        k.st(3010, 3210, 0, mc("barrel[facing=up]"));
        k.st(3009, 3209, 0, mc("hay_block"));
        k.house(TIMBER2, 3011, 3211, 3017, 3217, 2, "east", PITCHED).home("oak", "blue");
        AsgarniaRoom gerrant = k.house(TIMBER, 3011, 3220, 3018, 3227, 1, "east", PITCHED);
        gerrant.shop(rc("tool_shop"), mc("spruce_slab[type=top]"), mc("barrel[facing=up]"), mc("cobweb"),
                mc("dried_kelp_block"), mc("chest[facing=east]"));
        gerrant.set(1, gerrant.depth() - 1, 0, mc("water_cauldron[level=3]"));
        k.house(TIMBER2, 3008, 3229, 3016, 3236, 2, "east", PITCHED).home("spruce", "green");
        AsgarniaRoom grum = k.house(SARIM_STONE, 3010, 3243, 3017, 3249, 1, "east", PITCHED);
        grum.shop(rc("general_store"), mc("dark_oak_slab[type=top]"), mc("gold_block"), mc("chest[facing=east]"),
                mc("raw_gold_block"));
        AsgarniaRoom brian = k.house(TIMBER, 3025, 3244, 3031, 3250, 1, "west", PITCHED);
        brian.shop(rc("weapon_shop"), mc("spruce_slab[type=top]"), mc("grindstone[face=floor,facing=north]"),
                mc("barrel[facing=up]"), mc("iron_block"));
        // Betty's Magic Emporium (OSRS 3011-3016 x 3256-3261) with its chequered floor
        AsgarniaRoom betty = k.house(TIMBER2, 3011, 3256, 3016, 3261, 1, "east", PITCHED);
        for (int x = bx(3011) + 1; x <= bx(3016); x++) {
            for (int z = bz(3261) + 1; z <= bz(3256); z++) {
                k.s(x, -1, z, (((x >> 1) + (z >> 1)) & 1) == 0 ? mc("black_concrete") : mc("white_concrete"));
            }
        }
        betty.shop(rc("rune_shop"), mc("purpur_slab[type=top]"), mc("bookshelf"), mc("brewing_stand"), mc("amethyst_block"));
        betty.fill(1, betty.depth() - 2, betty.len() - 2, betty.depth() - 2, 0, 0, mc("purple_carpet"));
        betty.set(betty.len() - 1, betty.depth() - 1, 0, mc("cauldron"));
        k.house(TIMBER, 3034, 3242, 3039, 3247, 1, "north", PITCHED).home("spruce", "light_blue");
        // the Rusty Anchor
        AsgarniaRoom pub = k.house(TIMBER, 3040, 3250, 3050, 3259, 2, "west", PITCHED);
        pub.inn("dark_oak");
        AsgarniaRoom rooms = pub.level(1);
        for (int u = 2; u < rooms.len(); u += 3) {
            rooms.bed(u, 0, "red");
        }
        rooms.table(rooms.len() / 2, rooms.depth() - 3, "dark_oak");
        // trees behind the houses
        int[][] trees = {{3006, 3195}, {3006, 3214}, {3006, 3240}, {3008, 3252}, {3052, 3244}, {3020, 3264}};
        for (int[] t : trees) {
            k.tree(bx(t[0]), bz(t[1]), "oak", 6);
        }
        return k.build();
    }

    static StructureDef portSarimDocks() {
        StructureDef.Builder b = StructureDef.builder("port_sarim_docks", "Port Sarim docks").replaces("port_sarim_docks");
        AsgarniaKit k = new AsgarniaKit(b);
        k.pier(3028, 3197, 3046, 3199, mc("spruce_planks"), mc("spruce_fence"));
        k.st(3042, 3198, 0, rc("port_sarim_task_board"));
        k.st(3044, 3198, 0, mc("barrel[facing=up]"));
        k.st(3044, 3197, 0, mc("barrel[facing=up]"));
        k.st(3046, 3199, 0, mc("spruce_fence"));
        k.st(3046, 3199, 1, mc("lantern"));
        k.rowboat(bx(3048), bz(3199), false);
        return k.build();
    }

    static StructureDef iceDungeon() {
        StructureDef.Builder b = StructureDef.builder("asgarnian_ice_dungeon_entrance", "Asgarnian Ice Dungeon entrance")
                .replaces("royal_titans_lair");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2992, 3138, 3016, 3156);
        int cx = bx(3012);
        int cz = bz(3154);
        for (int y = 0; y < 5; y++) {
            k.disc(cx, cz, 5 - y, y, y, y % 2 == 0 ? mc("stone") : mc("packed_ice"));
        }
        k.disc(cx, cz, 2, 4, 4, mc("snow_block"));
        k.c(cx - 1, 0, cz + 2, cx + 1, 2, cz + 5);
        k.c(cx - 1, -8, cz - 2, cx + 1, 0, cz + 2);
        k.f(cx - 2, -9, cz - 3, cx + 2, -9, cz + 3, mc("packed_ice"));
        k.w(cx - 2, -8, cz - 3, cx + 2, -1, cz + 3, mc("blue_ice"));
        k.f(cx, -8, cz - 2, cx, 0, cz - 2, mc("ladder[facing=south]"));
        k.s(cx + 1, -8, cz + 2, rc("blurite_rock"));
        k.s(cx - 1, -8, cz + 2, mc("lantern"));
        k.f(cx - 2, -1, cz + 3, cx + 2, -1, cz + 5, mc("snow_block"));
        k.f(cx - 3, 0, cz + 6, cx + 3, 0, cz + 6, mc("snow[layers=2]"));
        return k.build();
    }

    static StructureDef thurgo() {
        StructureDef.Builder b = StructureDef.builder("port_sarim_thurgos_hut", "Thurgo's hut");
        AsgarniaKit k = new AsgarniaKit(b);
        AsgarniaRoom h = k.house(TIMBER, 2996, 3143, 3002, 3148, 1, "west", PITCHED);
        h.set(1, 0, 0, rc("anvil"));
        h.set(2, 0, 0, mc("blast_furnace[facing=west]"));
        h.bed(h.len() - 1, 0, "brown");
        h.table(h.len() / 2, h.depth() / 2 + 1, "spruce");
        h.set(h.len() - 1, h.depth() - 1, 0, mc("barrel[facing=up]"));
        k.st(2994, 3148, 0, mc("sweet_berry_bush[age=3]"));
        k.st(2995, 3149, 0, mc("sweet_berry_bush[age=3]"));
        k.st(2993, 3146, 0, mc("campfire[lit=true]"));
        k.path(2992, 3145, 2995, 3146, mc("dirt_path"), mc("gravel"));
        return k.build();
    }

    // ------------------------------------------------------------------ Rimmington

    static StructureDef rimmington() {
        StructureDef.Builder b = StructureDef.builder("rimmington", "Rimmington").replaces("RIMMINGTON");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2928, 3194, 2986, 3238);
        k.flatten(2968, 3238, 2986, 3250);
        k.path(2928, 3212, 2986, 3213, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2952, 3194, 2953, 3238, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2954, 3226, 2976, 3227, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2974, 3227, 2975, 3238, mc("dirt_path"), mc("coarse_dirt"));
        // well on the village green
        int wx = bx(2958);
        int wz = bz(3218);
        k.ring(wx, wz, 2, -1, 0, mc("cobblestone_wall"));
        k.disc(wx, wz, 1.4, -6, -1, mc("water"));
        k.f(wx - 2, 1, wz, wx - 2, 2, wz, mc("oak_fence"));
        k.f(wx + 2, 1, wz, wx + 2, 2, wz, mc("oak_fence"));
        k.f(wx - 2, 3, wz, wx + 2, 3, wz, mc("spruce_slab[type=bottom]"));
        // shops
        AsgarniaRoom rommik = k.house(RIMM, 2942, 3201, 2948, 3207, 1, "north", PITCHED);
        rommik.shop(rc("tool_shop"), mc("oak_slab[type=top]"), mc("loom[facing=north]"), mc("barrel[facing=up]"));
        rommik.set(1, rommik.depth() - 1, 0, rc("spinning_wheel"));
        AsgarniaRoom brian = k.house(RIMM, 2956, 3200, 2962, 3207, 1, "north", PITCHED);
        brian.shop(rc("archery_shop"), mc("oak_slab[type=top]"), mc("fletching_table"), mc("target"), mc("barrel[facing=up]"));
        AsgarniaRoom hetty = k.house(RIMM, 2966, 3203, 2972, 3209, 1, "west", PITCHED);
        hetty.set(hetty.len() / 2, hetty.depth() / 2, 0, mc("water_cauldron[level=3]"));
        hetty.set(hetty.len() / 2 + 1, hetty.depth() / 2, 0, mc("campfire[lit=false]"));
        hetty.fill(1, 0, 3, 0, 0, 1, mc("bookshelf"));
        hetty.set(4, 0, 0, mc("brewing_stand"));
        hetty.set(hetty.len() - 1, 0, 0, rc("cooking_range"));
        hetty.set(hetty.len() - 1, 1, 0, mc("cobweb"));
        hetty.set(1, hetty.depth() - 1, 0, mc("potted_dead_bush"));
        hetty.bed(hetty.len() - 1, 2, "black");
        AsgarniaRoom chem = k.house(RIMM, 2931, 3204, 2938, 3210, 1, "west", PITCHED);
        chem.shop(rc("apothecary_table"), mc("oak_slab[type=top]"), mc("brewing_stand"), mc("bookshelf"), mc("cauldron"));
        // the player-owned house portal and workbench
        int[] pr = rect(2946, 3224, 2949, 3224);
        k.f(pr[0], 0, pr[1], pr[2], 5, pr[1], mc("obsidian"));
        k.f(pr[0] + 1, 0, pr[1], pr[2] - 1, 4, pr[1], mc("purple_stained_glass"));
        k.f(pr[0], -1, pr[1] - 1, pr[2], -1, pr[1] + 2, mc("polished_blackstone"));
        k.st(2950, 3224, 0, rc("construction_workbench"));
        k.st(2951, 3224, 0, mc("oak_planks"));
        k.st(2951, 3224, 1, mc("oak_planks"));
        // houses
        k.house(RIMM, 2942, 3216, 2948, 3222, 1, "south", PITCHED).home("oak", "yellow");
        k.house(RIMM, 2962, 3216, 2968, 3222, 2, "south", PITCHED).home("spruce", "red");
        k.house(RIMM, 2960, 3230, 2966, 3236, 1, "south", PITCHED).home("oak", "green");
        k.house(RIMM, 2940, 3228, 2946, 3234, 1, "east", PITCHED).home("spruce", "blue");
        k.house(RIMM, 2975, 3200, 2981, 3206, 1, "west", PITCHED).home("oak", "orange");
        // wheat field
        int[] fr = rect(2971, 3214, 2984, 3224);
        k.f(fr[0], -1, fr[1], fr[2], -1, fr[3], mc("farmland[moisture=7]"));
        k.f(fr[0], 0, fr[1], fr[2], 0, fr[3], mc("wheat[age=7]"));
        for (int x = fr[0] + 4; x < fr[2]; x += 9) {
            k.f(x, -1, fr[1], x, -1, fr[3], mc("water"));
            k.f(x, 0, fr[1], x, 0, fr[3], mc("air"));
        }
        k.w(fr[0] - 1, 0, fr[1] - 1, fr[2] + 1, 0, fr[3] + 1, mc("oak_fence"));
        k.s(fr[0] + 6, 0, fr[3] + 1, mc("air"));
        k.s(fr[0] + 6, 1, fr[3] + 1, mc("air"));
        // Rimmington mine
        k.floor(2969, 3237, 2985, 3249, mc("gravel"));
        String[] rocks = {"copper_rock", "tin_rock", "iron_rock", "clay_rock", "copper_rock", "gold_rock", "tin_rock", "iron_rock"};
        int i = 0;
        for (int y = 3239; y <= 3248; y += 3) {
            for (int x = 2971 + (y % 2); x <= 2984; x += 3) {
                k.st(x, y, 0, rc(rocks[i++ % rocks.length]));
                k.st(x + 1, y, -1, mc("stone"));
            }
        }
        k.tree(bx(2934), bz(3200), "oak", 6);
        k.tree(bx(2950), bz(3236), "oak", 7);
        k.tree(bx(2983), bz(3209), "birch", 6);
        k.flowers(2955, 3209, 2957, 3210);
        return k.build();
    }

    static StructureDef rimmingtonBush() {
        StructureDef.Builder b = StructureDef.builder("rimmington_bush_patch", "Rimmington bush patch").replaces("rimmington_bush");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2936, 3220, 2940, 3224, mc("coarse_dirt"));
        k.fenceRing(2936, 3220, 2940, 3224, mc("oak_fence"));
        k.clearT(2940, 3222, 2940, 3222, 0, 0);
        k.st(2938, 3222, 0, rc("bush_patch"));
        k.st(2937, 3224, 0, rc("compost_bin"));
        return k.build();
    }

    // ------------------------------------------------------------------ Crafting Guild

    static StructureDef craftingGuild() {
        StructureDef.Builder b = StructureDef.builder("crafting_guild", "Crafting Guild").replaces("crafting_guild");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2920, 3268, 2946, 3298);
        AsgarniaRoom g0 = k.house(GUILD, 2926, 3283, 2939, 3292, 2, "south", PITCHED);
        g0.set(2, 0, 0, rc("potters_wheel"));
        g0.set(4, 0, 0, rc("potters_wheel"));
        g0.set(7, 0, 0, rc("pottery_oven"));
        g0.set(8, 0, 0, mc("bricks"));
        g0.set(9, 0, 0, rc("spinning_wheel"));
        g0.set(11, 0, 0, mc("loom[facing=south]"));
        g0.set(13, 0, 0, rc("crafting_table"));
        g0.set(g0.len() - 2, 0, 0, mc("cartography_table"));
        g0.fill(2, 3, 6, 3, 0, 0, mc("oak_slab[type=top]"));
        g0.set(3, 3, 1, mc("decorated_pot"));
        g0.set(5, 3, 1, mc("flower_pot"));
        g0.fill(g0.len() / 2 - 1, 2, g0.len() / 2, g0.depth() - 1, 0, 0, mc("brown_carpet"));
        g0.fill(g0.len() - 6, 4, g0.len() - 2, 4, 0, 0, mc("oak_slab[type=top]"));
        g0.set(g0.len() - 4, 4, 1, mc("candle[candles=2,lit=true]"));
        AsgarniaRoom g1 = g0.level(1);
        g1.set(2, 0, 0, rc("dairy_churn"));
        g1.set(4, 0, 0, rc("tanning_rack"));
        g1.set(6, 0, 0, rc("bank_booth"));
        g1.set(7, 0, 0, rc("bank_booth"));
        g1.set(9, 0, 0, mc("loom[facing=south]"));
        g1.set(11, 0, 0, mc("loom[facing=south]"));
        g1.set(g1.len() - 2, 0, 0, mc("chest[facing=south]"));
        g1.longTable(4, g1.len() - 5, g1.depth() / 2 + 1, "oak", mc("white_carpet"));
        // fenced mine yard with clay, silver and gold rocks
        k.floor(2926, 3272, 2944, 3281, mc("gravel"));
        k.fenceRing(2926, 3272, 2944, 3281, mc("oak_fence"));
        k.clearT(2932, 3281, 2933, 3281, 0, 0);
        k.path(2932, 3282, 2933, 3282, mc("dirt_path"), mc("dirt_path"));
        String[] rocks = {"clay_rock", "silver_rock", "gold_rock", "clay_rock", "gold_rock", "silver_rock"};
        int i = 0;
        for (int y = 3273; y <= 3279; y += 3) {
            for (int x = 2928; x <= 2943; x += 3) {
                k.st(x, y, 0, rc(rocks[i++ % rocks.length]));
            }
        }
        // cow pen for the dairy
        k.fenceRing(2940, 3284, 2945, 3292, mc("oak_fence"));
        k.st(2942, 3290, 0, mc("hay_block"));
        k.st(2943, 3286, 0, mc("water_cauldron[level=3]"));
        k.tree(bx(2922), bz(3295), "oak", 7);
        k.tree(bx(2922), bz(3274), "oak", 6);
        return k.build();
    }

    // ------------------------------------------------------------------ Entrana

    static StructureDef entrana() {
        StructureDef.Builder b = StructureDef.builder("entrana", "Entrana").replaces("entrana");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2806, 3318, 2864, 3364);
        k.harbour(2826, 3304, 2844, 3317);
        k.path(2830, 3330, 2838, 3338, mc("stone_bricks"), mc("mossy_stone_bricks"));
        k.path(2833, 3318, 2835, 3330, mc("gravel"), mc("stone_bricks"));
        k.path(2838, 3333, 2847, 3335, mc("gravel"), mc("stone_bricks"));
        k.path(2846, 3335, 2847, 3340, mc("gravel"), mc("stone_bricks"));
        k.path(2820, 3333, 2830, 3335, mc("gravel"), mc("stone_bricks"));
        k.path(2821, 3335, 2822, 3339, mc("gravel"), mc("stone_bricks"));
        k.pier(2833, 3304, 2835, 3318, mc("spruce_planks"), mc("spruce_fence"));
        k.ship(bz(3317), bx(2839), 22, 2, false, 1, 1, mc("spruce_planks"), mc("birch_planks"), mc("white_wool"));

        // the church
        AsgarniaRoom ch = k.house(ENTRANA, 2842, 3341, 2852, 3355, 2, "south", PITCHED);
        k.c(ch.x1, 3, ch.z1, ch.x2, 3, ch.z2);
        k.c(ch.wx(0, 0), 0, ch.wz(0, 0), ch.wx(0, 0), 4, ch.wz(0, 0));
        int len = ch.len();
        int dep = ch.depth();
        ch.fill(1, 0, len - 2, 2, 0, 0, mc("polished_andesite"));
        ch.set(len / 2, 1, 1, rc("prayer_altar"));
        ch.set(len / 2 - 1, 1, 1, mc("candle[candles=4,lit=true]"));
        ch.set(len / 2 + 1, 1, 1, mc("candle[candles=4,lit=true]"));
        ch.set(len / 2 - 1, 0, 1, mc("lectern[facing=south]"));
        for (int v = 4; v < dep - 3; v += 2) {
            ch.fill(1, v, len / 2 - 2, v, 0, 0, stair(mc("dark_oak_stairs"), "south"));
            ch.fill(len / 2 + 1, v, len - 2, v, 0, 0, stair(mc("dark_oak_stairs"), "south"));
        }
        ch.fill(len / 2 - 1, 3, len / 2, dep - 1, 0, 0, mc("red_carpet"));
        for (int v = 3; v < dep; v += 4) {
            ch.set(0, v, 6, mc("lantern[hanging=true]"));
            ch.set(len - 1, v, 6, mc("lantern[hanging=true]"));
        }
        k.tower(bx(2847) + 1, bz(3358), 3, 16, mc("stone_bricks"), mc("spruce_planks"), "south", mc("dark_oak_planks"));
        k.s(bx(2847) + 1, 13, bz(3358), mc("bell[attachment=ceiling,facing=north]"));

        // the monastery: north wing, west wing and a cloister garden
        AsgarniaRoom nw = k.house(ENTRANA, 2812, 3350, 2830, 3356, 2, "south", PITCHED);
        nw.longTable(3, nw.len() - 4, nw.depth() / 2, "spruce", mc("white_carpet"));
        nw.set(nw.len() - 1, 0, 0, rc("cooking_range"));
        nw.set(nw.len() - 2, 0, 0, mc("barrel[facing=up]"));
        nw.fill(2, 0, 5, 0, 0, 1, mc("bookshelf"));
        AsgarniaRoom cells = nw.level(1);
        for (int u = 2; u < cells.len() - 1; u += 3) {
            cells.bed(u, 0, "brown");
            cells.set(u + 1, 0, 0, mc("chest[facing=south]"));
        }
        AsgarniaRoom ww = k.house(ENTRANA, 2812, 3340, 2818, 3349, 1, "east", PITCHED);
        ww.fill(1, 0, ww.len() - 1, 0, 0, 2, mc("bookshelf"));
        ww.set(ww.len() / 2, 2, 0, mc("lectern[facing=east]"));
        ww.set(ww.len() / 2 + 2, 2, 0, mc("enchanting_table"));
        k.floor(2820, 3340, 2830, 3348, mc("grass_block"));
        k.fenceRing(2820, 3340, 2830, 3348, mc("stone_brick_wall"));
        k.clearT(2821, 3340, 2822, 3340, 0, 0);
        k.flowers(2822, 3342, 2824, 3346);
        int[] herb = rect(2826, 3342, 2829, 3346);
        k.f(herb[0], -1, herb[1], herb[2], -1, herb[3], mc("farmland[moisture=7]"));
        k.f(herb[0], 0, herb[1], herb[2], 0, herb[3], mc("potatoes[age=7]"));
        k.f(herb[0] + 2, 0, herb[1], herb[0] + 2, 0, herb[3], mc("air"));
        k.f(herb[0] + 2, -1, herb[1], herb[0] + 2, -1, herb[3], mc("water"));
        k.tree(bx(2825), bz(3347), "oak", 6);
        // houses and Frincos' herb store
        AsgarniaRoom frin = k.house(ENTRANA, 2840, 3322, 2846, 3328, 1, "west", PITCHED);
        frin.shop(rc("apothecary_table"), mc("spruce_slab[type=top]"), mc("brewing_stand"), mc("flower_pot"), mc("barrel[facing=up]"));
        AsgarniaRoom fire = k.house(ENTRANA, 2822, 3322, 2828, 3328, 1, "east", PITCHED);
        fire.shop(rc("general_store"), mc("spruce_slab[type=top]"), mc("barrel[facing=up]"), mc("chest[facing=east]"));
        k.house(ENTRANA, 2852, 3320, 2858, 3328, 1, "west", PITCHED).home("spruce", "white");
        // the balloon field
        int bxx = bx(2808);
        int bzz = bz(3354);
        k.w(bxx - 1, 0, bzz - 1, bxx + 1, 0, bzz + 1, mc("spruce_planks"));
        k.f(bxx - 1, 1, bzz - 1, bxx - 1, 3, bzz - 1, mc("spruce_fence"));
        k.f(bxx + 1, 1, bzz + 1, bxx + 1, 3, bzz + 1, mc("spruce_fence"));
        for (int y = 4; y <= 10; y++) {
            double rr = 3.5 * Math.sin(Math.PI * (y - 3) / 8.0) + 0.5;
            k.ring(bxx, bzz, rr, y, y, mc(y % 2 == 0 ? "red_wool" : "yellow_wool"));
        }
        k.disc(bxx, bzz, 1.5, 11, 11, mc("red_wool"));
        int[][] trees = {{2860, 3360}, {2808, 3320}, {2858, 3340}, {2836, 3360}, {2808, 3340}};
        for (int[] t : trees) {
            k.tree(bx(t[0]), bz(t[1]), "oak", 7);
        }
        return k.build();
    }

    static StructureDef entranaHops() {
        StructureDef.Builder b = StructureDef.builder("entrana_hops_patch", "Entrana hops patch").replaces("entrana_hops");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2808, 3332, 2812, 3336, mc("coarse_dirt"));
        k.fenceRing(2808, 3332, 2812, 3336, mc("oak_fence"));
        k.clearT(2812, 3334, 2812, 3334, 0, 0);
        k.st(2810, 3334, 0, rc("hops_patch"));
        k.st(2809, 3332, 0, rc("compost_bin"));
        return k.build();
    }
}
