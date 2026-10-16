package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.AsgarniaKit.PITCHED;
import static com.runecraft.osrs.content.world.AsgarniaKit.dxOf;
import static com.runecraft.osrs.content.world.AsgarniaKit.dzOf;
import static com.runecraft.osrs.content.world.AsgarniaKit.mc;
import static com.runecraft.osrs.content.world.AsgarniaKit.rc;
import static com.runecraft.osrs.content.world.AsgarniaKit.rect;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/** Karamja: Musa Point, Brimhaven, Shilo Village, Tai Bwo Wannai and the volcano with the TzHaar gate. */
final class AsgarniaKaramja {
    static final AsgarniaKit.Style KAR = new AsgarniaKit.Style(mc("bamboo_block[axis=y]"), mc("jungle_log[axis=y]"),
            mc("bamboo_mosaic"), mc("stripped_jungle_wood"), mc("bamboo_mosaic_stairs"), mc("hay_block[axis=y]"),
            mc("jungle_fence"), true);
    static final AsgarniaKit.Style KAR2 = new AsgarniaKit.Style(mc("jungle_planks"), mc("jungle_log[axis=y]"),
            mc("bamboo_planks"), mc("stripped_jungle_wood"), mc("bamboo_stairs"), mc("bamboo_planks"),
            mc("jungle_fence"), true);
    static final AsgarniaKit.Style KAR_STONE = new AsgarniaKit.Style(mc("mossy_stone_bricks"), mc("jungle_log[axis=y]"),
            mc("jungle_planks"), mc("mossy_cobblestone"), mc("bamboo_mosaic_stairs"), mc("hay_block[axis=y]"),
            mc("glass_pane"), false);

    private AsgarniaKaramja() {
    }

    static void build(List<StructureDef> out) {
        out.add(musaPoint());
        out.add(musaDocks());
        out.add(brimhaven());
        out.add(brimhavenDocks());
        out.add(brimhavenPatches());
        out.add(agilityArena());
        out.add(shilo());
        out.add(taiBwoWannai());
        out.add(volcano());
        out.add(tzhaarGate());
        out.add(eelPool());
    }

    /** A round hut with a thatched cone roof; door on the given side. */
    static void roundHut(AsgarniaKit k, int cx, int cz, int r, String wall, String roof, String door) {
        k.disc(cx, cz, r, -2, -1, mc("bamboo_mosaic"));
        k.disc(cx, cz, r - 1, 0, r + 3, mc("air"));
        k.ring(cx, cz, r, 0, 2, wall);
        for (int i = 0; i <= r + 1; i++) {
            double rr = r + 1 - i;
            if (rr <= 1) {
                k.disc(cx, cz, 1, 3 + i, 3 + i, roof);
                k.s(cx, 4 + i, cz, roof);
                break;
            }
            k.ring(cx, cz, rr, 3 + i, 3 + i, roof);
        }
        int ox = cx + dxOf(door) * r;
        int oz = cz + dzOf(door) * r;
        k.c(ox, 0, oz, ox, 1, oz);
        if (dxOf(door) != 0) {
            k.c(ox, 0, oz + 1, ox, 1, oz + 1);
        } else {
            k.c(ox + 1, 0, oz, ox + 1, 1, oz);
        }
        // bedroll, chest and a lantern opposite the door
        int bx0 = cx - dxOf(door) * (r - 1);
        int bz0 = cz - dzOf(door) * (r - 1);
        k.s(bx0, 0, bz0, mc("chest"));
        k.s(cx + dzOf(door) * (r - 2), 0, cz + dxOf(door) * (r - 2), mc("red_carpet"));
        k.s(cx + dzOf(door) * (r - 2) - dxOf(door), 0, cz + dxOf(door) * (r - 2) - dzOf(door), mc("white_carpet"));
        k.s(cx - dzOf(door) * (r - 2), 0, cz - dxOf(door) * (r - 2), mc("lantern"));
        k.s(cx - dzOf(door) * (r - 2), -1, cz - dxOf(door) * (r - 2), mc("stone"));
    }

    static void palms(AsgarniaKit k, int[][] tiles) {
        for (int i = 0; i < tiles.length; i++) {
            k.palm(bx(tiles[i][0]), bz(tiles[i][1]), 6 + i % 3);
        }
    }

    // ------------------------------------------------------------------ Musa Point

    static StructureDef musaPoint() {
        StructureDef.Builder b = StructureDef.builder("musa_point", "Musa Point").replaces("MUSA_POINT");
        AsgarniaKit k = new AsgarniaKit(b);
        k.doorWood = "jungle";
        k.flatten(2890, 3136, 2948, 3188);
        k.path(2895, 3149, 2948, 3150, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2913, 3140, 2915, 3188, mc("dirt_path"), mc("coarse_dirt"));
        k.floor(2909, 3170, 2919, 3178, mc("coarse_dirt"));
        // Luthas's banana plantation
        int[] pr = rect(2918, 3156, 2944, 3168);
        k.f(pr[0], -1, pr[1], pr[2], -1, pr[3], mc("podzol"));
        k.w(pr[0], 0, pr[1], pr[2], 0, pr[3], mc("bamboo_fence"));
        k.clearT(2918, 3162, 2918, 3163, 0, 0);
        for (int x = 2921; x <= 2942; x += 3) {
            for (int y = 3158; y <= 3166; y += 4) {
                k.palm(bx(x), bz(y), 5 + (x + y) % 2);
                k.s(bx(x) + 1, 4, bz(y), mc("yellow_wool"));
            }
        }
        AsgarniaRoom shed = k.house(KAR2, 2936, 3151, 2942, 3155, 1, "north", PITCHED);
        shed.fill(1, 0, shed.len() - 1, 0, 0, 1, mc("barrel[facing=up]"));
        shed.set(shed.len() - 1, 2, 0, mc("chest[facing=west]"));
        shed.set(1, 2, 0, mc("yellow_wool"));
        // the general store, Zambo's spirits and huts
        AsgarniaRoom store = k.house(KAR2, 2902, 3141, 2909, 3147, 1, "north", PITCHED);
        store.shop(rc("general_store"), mc("bamboo_slab[type=top]"), mc("barrel[facing=up]"), mc("chest[facing=north]"),
                mc("melon"));
        AsgarniaRoom zambo = k.house(KAR, 2922, 3141, 2928, 3147, 1, "north", PITCHED);
        zambo.inn("bamboo");
        k.house(KAR, 2896, 3160, 2902, 3166, 1, "east", PITCHED).home("jungle", "orange");
        k.house(KAR2, 2900, 3176, 2906, 3182, 1, "east", PITCHED).home("jungle", "yellow");
        k.house(KAR, 2920, 3178, 2926, 3184, 1, "west", PITCHED).home("jungle", "lime");
        roundHut(k, bx(2934), bz(3180), 4, mc("bamboo_block[axis=y]"), mc("hay_block"), "west");
        palms(k, new int[][]{{2894, 3140}, {2898, 3170}, {2892, 3184}, {2910, 3186}, {2930, 3186}, {2944, 3180},
                {2946, 3142}, {2918, 3144}, {2932, 3138}, {2908, 3156}});
        return k.build();
    }

    static StructureDef musaDocks() {
        StructureDef.Builder b = StructureDef.builder("musa_point_docks", "Musa Point docks").replaces("musa_point_docks");
        AsgarniaKit k = new AsgarniaKit(b);
        k.harbour(2949, 3136, 2972, 3162);
        k.pier(2947, 3149, 2964, 3151, mc("jungle_planks"), mc("jungle_fence"));
        k.pier(2949, 3155, 2958, 3156, mc("jungle_planks"), mc("jungle_fence"));
        k.st(2954, 3151, 0, rc("musa_point_task_board"));
        k.st(2960, 3149, 0, mc("barrel[facing=up]"));
        k.st(2961, 3149, 0, mc("barrel[facing=up]"));
        k.ship(bx(2950), bz(3145), 26, 3, true, 1, 2, mc("dark_oak_planks"), mc("spruce_planks"), mc("white_wool"));
        k.rowboat(bx(2960), bz(3158), true);
        return k.build();
    }

    // ------------------------------------------------------------------ Brimhaven

    static StructureDef brimhaven() {
        StructureDef.Builder b = StructureDef.builder("brimhaven", "Brimhaven").replaces("BRIMHAVEN");
        AsgarniaKit k = new AsgarniaKit(b);
        k.doorWood = "jungle";
        k.flatten(2738, 3140, 2812, 3236);
        k.harbour(2746, 3237, 2802, 3254);
        k.path(2756, 3168, 2768, 3180, mc("coarse_dirt"), mc("cobblestone"));
        k.path(2762, 3140, 2764, 3186, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2762, 3200, 2764, 3236, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2765, 3184, 2786, 3186, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2738, 3172, 2812, 3174, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2794, 3161, 2795, 3171, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2769, 3183, 2769, 3185, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2748, 3167, 2749, 3171, mc("dirt_path"), mc("coarse_dirt"));
        // Footprints from the OSRS map (mejrs layers_osrs): the Shrimp and Parrot is the big hall 2787-2800 x
        // 3180-3193 (door south at 2794), the Dead Man's Chest the long building 2791-2799 x 3153-3169 (door
        // east at 3167), and the block of houses north-west of the square 2759-2782 x 3187-3199 (which is
        // which in OSRS is approx)
        AsgarniaRoom sp = k.house(KAR2, 2787, 3180, 2800, 3193, 1, "south", PITCHED);
        sp.inn("jungle");
        sp.set(1, sp.depth() - 1, 0, mc("smoker[facing=west]"));
        k.s(sp.x1 - 3, 3, (sp.z1 + sp.z2) / 2 - 2, mc("jungle_fence"));
        k.s(sp.x1 - 3, 2, (sp.z1 + sp.z2) / 2 - 2, mc("red_wool"));
        // the Dead Man's Chest pub
        AsgarniaRoom dm = k.house(KAR, 2791, 3153, 2799, 3169, 2, "east", PITCHED);
        dm.inn("dark_oak");
        AsgarniaRoom up = dm.level(1);
        up.bed(up.len() - 1, 0, "black");
        up.bed(up.len() - 3, 0, "red");
        up.set(1, up.depth() - 1, 0, mc("chest[facing=north]"));
        // general store and houses
        AsgarniaRoom gs = k.house(KAR2, 2745, 3160, 2752, 3166, 1, "north", PITCHED);
        gs.shop(rc("general_store"), mc("jungle_slab[type=top]"), mc("barrel[facing=up]"), mc("chest[facing=north]"),
                mc("hay_block"));
        k.house(KAR, 2745, 3190, 2751, 3196, 1, "east", PITCHED).home("jungle", "orange");
        k.house(KAR2, 2759, 3187, 2765, 3199, 2, "south", PITCHED).home("jungle", "yellow");
        k.house(KAR, 2766, 3187, 2777, 3195, 1, "south", PITCHED).home("jungle", "lime");
        k.house(KAR2, 2766, 3196, 2777, 3199, 1, "north", PITCHED).home("jungle", "red");
        k.house(KAR, 2778, 3187, 2782, 3199, 1, "east", PITCHED).home("jungle", "purple");
        k.house(KAR2, 2789, 3194, 2797, 3199, 1, "north", PITCHED).home("jungle", "white");
        k.house(KAR, 2787, 3173, 2792, 3176, 1, "north", PITCHED).home("jungle", "blue");
        k.house(KAR, 2766, 3150, 2772, 3156, 1, "north", PITCHED).home("jungle", "green");
        k.house(KAR2, 2742, 3176, 2748, 3182, 1, "east", PITCHED).home("jungle", "cyan");
        k.house(KAR, 2776, 3216, 2782, 3222, 1, "west", PITCHED).home("jungle", "brown");
        k.house(KAR2, 2746, 3216, 2752, 3222, 1, "east", PITCHED).home("jungle", "pink");
        roundHut(k, bx(2752), bz(3150), 4, mc("bamboo_block[axis=y]"), mc("hay_block"), "east");
        roundHut(k, bx(2802), bz(3150), 4, mc("bamboo_block[axis=y]"), mc("hay_block"), "west");
        palms(k, new int[][]{{2742, 3144}, {2758, 3146}, {2776, 3144}, {2806, 3146}, {2740, 3204}, {2770, 3206},
                {2790, 3208}, {2742, 3230}, {2796, 3228}, {2806, 3170}, {2752, 3186}, {2774, 3202}, {2784, 3178}});
        return k.build();
    }

    static StructureDef brimhavenDocks() {
        StructureDef.Builder b = StructureDef.builder("brimhaven_docks", "Brimhaven docks").replaces("brimhaven_docks");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2748, 3231, 2800, 3236, mc("jungle_planks"));
        int[] q = rect(2748, 3236, 2800, 3236);
        for (int x = q[0]; x <= q[2]; x += 2) {
            k.f(x, -8, q[1], x, -2, q[1], mc("jungle_log[axis=y]"));
        }
        k.pier(2770, 3237, 2772, 3250, mc("jungle_planks"), mc("jungle_fence"));
        k.pier(2786, 3237, 2788, 3248, mc("jungle_planks"), mc("jungle_fence"));
        k.ship(bz(3237), bx(2766), 24, 3, false, -1, 2, mc("dark_oak_planks"), mc("jungle_planks"), mc("white_wool"));
        k.ship(bz(3237), bx(2793), 20, 2, false, -1, 1, mc("jungle_planks"), mc("bamboo_planks"), mc("red_wool"));
        k.st(2778, 3232, 0, rc("brimhaven_task_board"));
        for (int x = 2750; x <= 2798; x += 6) {
            k.st(x, 3234, 0, mc("barrel[facing=up]"));
        }
        for (int x = 2752; x <= 2798; x += 12) {
            k.st(x, 3236, 0, mc("jungle_fence"));
            k.st(x, 3236, 1, mc("lantern"));
        }
        return k.build();
    }

    static StructureDef brimhavenPatches() {
        StructureDef.Builder b = StructureDef.builder("brimhaven_farming_patches", "Brimhaven patches").replaces("brimhaven_patches");
        AsgarniaKit k = new AsgarniaKit(b);
        k.floor(2754, 3210, 2760, 3218, mc("coarse_dirt"));
        k.fenceRing(2754, 3210, 2760, 3218, mc("bamboo_fence"));
        k.clearT(2760, 3214, 2760, 3214, 0, 0);
        k.st(2756, 3212, 0, rc("fruit_tree_patch"));
        k.st(2757, 3216, 0, rc("spirit_tree_patch"));
        k.st(2759, 3211, 0, mc("composter"));
        return k.build();
    }

    static StructureDef agilityArena() {
        StructureDef.Builder b = StructureDef.builder("brimhaven_agility_arena", "Brimhaven Agility Arena")
                .replaces("mg_brimhaven_agility_arena");
        AsgarniaKit k = new AsgarniaKit(b);
        AsgarniaRoom a = k.house(KAR2, 2802, 3192, 2817, 3205, 1, "south", PITCHED);
        // raised platforms with obstacles between them
        for (int u = 2; u < a.len() - 2; u += 5) {
            for (int v = 1; v < a.depth() - 4; v += 5) {
                a.fill(u, v, u + 1, v + 1, 0, 0, mc("jungle_planks"));
                if ((u + v) % 2 == 0) {
                    a.set(u + 3, v, 0, rc("agility_obstacle"));
                } else {
                    a.set(u, v + 3, 0, mc("bamboo_fence"));
                }
            }
        }
        a.set(a.len() / 2 - 3, a.depth() - 1, 0, rc("brimhaven_agility_arena_entrance"));
        a.set(a.len() / 2 + 2, a.depth() - 1, 0, rc("brimhaven_agility_arena_rewards"));
        a.set(1, a.depth() - 1, 0, mc("barrel[facing=up]"));
        k.path(2808, 3175, 2811, 3191, mc("dirt_path"), mc("coarse_dirt"));
        return k.build();
    }

    // ------------------------------------------------------------------ Shilo Village

    /**
     * Shilo Village as on the OSRS map (mejrs layers_osrs, plane 0; ladders from shortest-path; NPC spawns from
     * mejrs data_osrs): the river crossing the village west to east (the bridge at 2831-2832), bamboo huts
     * with thatched roofs on their footprints. The bank is the three joined huts 2843-2861 x 2951-2957
     * (ladders at 2845 and 2859, 2954), the furnace house 2852-2861 x 2963-2968 entered between its two feet
     * by Yohnus (2857, 2963), Obli's general store 2822-2827 x 2956-2961 (ladder 2824, 2958), Yanni's antiques
     * the double hut 2832-2839 x 2980-2993 (ladder 2835, 2988), Fernahei's fishing shop 2869-2873 x 2966-2972
     * (ladder 2871, 2971) and Duradel's hut 2868-2870 x 2981-2983. The palisade closes the village, entered
     * through the gate yard in the south-east (2867-2878 x 2944-2962, Mosol Rei outside); doors (the map has no
     * door marks here), the range in the antiques hut and Kaleb's raised house (not built) are approx.
     */
    static StructureDef shilo() {
        StructureDef.Builder b = StructureDef.builder("shilo_village", "Shilo Village").replaces("SHILO_VILLAGE");
        AsgarniaKit k = new AsgarniaKit(b);
        k.doorWood = "bamboo";
        k.flatten(2814, 2940, 2882, 3010);
        int[] r = rect(2817, 2944, 2878, 3006);
        // the palisade with the gate yard in the south-east corner: inner gate at 2867, outer at 2878
        String log = mc("stripped_jungle_log[axis=y]");
        k.w(r[0], -1, r[1], r[2], 4, r[3], log);
        k.w(r[0], 5, r[1], r[2], 5, r[3], mc("jungle_fence"));
        int[] yard = rect(2867, 2944, 2878, 2962);
        k.f(yard[0], -1, yard[1], yard[0], 4, yard[3], log);
        k.f(yard[0], -1, yard[1], yard[2], 4, yard[1], log);
        k.f(yard[0], 5, yard[1], yard[0], 5, yard[3], mc("jungle_fence"));
        k.clearT(2867, 2954, 2867, 2956, 0, 5);
        k.clearT(2878, 2951, 2878, 2953, 0, 5);
        for (int[] p : new int[][]{{bx(2867), bz(2956) - 1}, {bx(2867), bz(2953)}, {bx(2878) + 1, bz(2953) - 1}, {bx(2878) + 1, bz(2950)}}) {
            k.f(p[0], -1, p[1], p[0], 6, p[1], mc("jungle_log[axis=y]"));
            k.s(p[0], 7, p[1], mc("skeleton_skull"));
        }
        // the river and the bridge
        MapTown t = new MapTown(b);
        String[] river = {
                "...................................bbbbbbb........bbbbbbbb", // 2977
                "...............................bbbbbbbbbbbbbbb..bbbbbbbbbbbbb",
                "........................bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", // 2975
                "...........aaa..bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "aa.......aaaaa..bbbbbbbbbbbbbbbbbbbbb...bbbbbbbbbbbbbbbbbbbbbbbb",
                "aaaaaaaaaaaaaa..bbbbbbbbbbbbbbbbbb........bbbbbbbb",
                "aaaaaaaaaaaa......bbbbbbbbbbbb",
                "aaaaaaaaaaa", // 2970
                "...aaaaaa",
        };
        t.mask(2817, 2977, "ab", -4, -4, mc("sand"), river);
        t.mask(2817, 2977, "ab", -3, -1, mc("water"), river);
        k.fillT(2831, 2970, 2832, 2976, -1, -1, mc("jungle_planks"));
        k.f(bx(2831), 0, bz(2976), bx(2831), 0, bz(2970) + 1, mc("jungle_fence"));
        k.f(bx(2832) + 1, 0, bz(2976), bx(2832) + 1, 0, bz(2970) + 1, mc("jungle_fence"));
        // paths: north-south through the village to the bridge, east to the gate, the cart track
        k.path(2850, 2958, 2853, 2962, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2850, 2978, 2853, 3004, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2818, 2957, 2866, 2959, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2862, 2954, 2870, 2956, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2870, 2951, 2878, 2956, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2831, 2960, 2832, 2969, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2831, 2977, 2832, 2980, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2829, 2950, 2835, 2955, mc("coarse_dirt"), mc("gravel"));
        // the village, row 0 = 2993, column 0 = 2822
        String[] plan = {
                "...........cccccc",
                "...........cccccc",
                "...........cccccc",
                "...........cccccc", // 2990
                "...........cccccc",
                "............cccc",
                "............cccc",
                "...........cccccc",
                "..........cccccccc", // 2985
                "..........cccccccc",
                "..........cccccccc............................fff",
                "..........cccccccc............................fff",
                "..........cccccccc............................fff",
                "...........cccccc", // 2980
                "",
                "",
                "",
                "",
                "", // 2975
                "",
                "",
                "...............................................gggg",
                "...............................................gggg",
                "...............................................gggg", // 2970
                "...............................................gggg",
                "...............................eeeeeeee........ggggg",
                "..............................eeeeeeeeee.......ggggg",
                "..............................eeeeeeeeee.......gggg",
                "..............................eeeeeeeeee", // 2965
                "..............................eeeeeeeeee",
                "...............................eee..eee",
                "",
                ".aaaa",
                "aaaaaa", // 2960
                "aaaaaa",
                "aaaaaa",
                "aaaaaa......................ddddd",
                ".aaaa................dddd..ddddddd..dddd",
                ".....................ddddddddddddddddddd", // 2955
                ".....................ddddddddddddddddddd",
                ".....................ddddddddddddddddddd",
                ".....................dddd..ddddddd..dddd",
                "............................ddddd",
        };
        String wall = mc("jungle_planks");
        String trim = mc("stripped_jungle_log[axis=y]");
        String thatchS = mc("bamboo_mosaic_stairs");
        String thatch = mc("bamboo_mosaic");
        String fl = mc("jungle_planks");
        t.bld(2822, 2993, "a", plan).wall(wall).trim(trim).floor(fl, fl).roof(thatchS, thatch).storeys(2)
                .door(2827, 2958, 'e', "bamboo").ladder(2824, 2958, "east").build();
        t.bld(2822, 2993, "c", plan).wall(wall).trim(trim).floor(fl, fl).roof(thatchS, thatch).storeys(2)
                .door(2835, 2980, 's', "bamboo").ladder(2835, 2988, "east").build();
        t.bld(2822, 2993, "d", plan).wall(mc("mossy_stone_bricks")).trim(trim).floor(mc("polished_andesite"), fl)
                .roof(thatchS, thatch).storeys(2).door(2851, 2957, 'n', "bamboo").door(2852, 2951, 's', "bamboo")
                .ladder(2845, 2954, "east").ladder(2859, 2954, "west").build();
        t.bld(2822, 2993, "e", plan).wall(mc("mossy_stone_bricks")).trim(trim).floor(mc("stone_bricks"), fl)
                .roof(thatchS, thatch).door(2856, 2964, 's', "bamboo").door(2857, 2964, 's', "bamboo").build();
        t.bld(2822, 2993, "f", plan).wall(wall).trim(trim).floor(fl, fl).roof(thatchS, thatch).door(2869, 2981, 's', "bamboo").build();
        t.bld(2822, 2993, "g", plan).wall(wall).trim(trim).floor(fl, fl).roof(thatchS, thatch).storeys(2)
                .door(2869, 2968, 'w', "bamboo").ladder(2871, 2971, "east").build();
        // stations: the bank's booths in the middle hut (bankers at 2851, 2954 / 2853, 2955), the furnace,
        // Obli's store, Fernahei's tackle, the range (approx) and Duradel's skulls
        for (int tx = 2849; tx <= 2855; tx++) {
            k.fillT(tx, 2953, tx, 2953, 0, 0, tx == 2850 || tx == 2852 || tx == 2854 ? rc("bank_booth") : mc("mossy_stone_brick_slab[type=top]"));
        }
        k.st(2854, 2966, 0, rc("furnace"));
        k.st(2855, 2966, 0, rc("anvil"));
        k.st(2859, 2967, 0, mc("coal_block"));
        k.st(2825, 2960, 0, rc("general_store"));
        k.st(2823, 2957, 0, mc("barrel[facing=up]"));
        k.st(2872, 2969, 0, rc("tool_shop"));
        k.st(2870, 2967, 0, mc("barrel[facing=up]"));
        k.st(2837, 2982, 0, rc("cooking_range"));
        k.st(2834, 2984, 0, mc("chest[facing=east]"));
        k.st(2836, 2991, 0, mc("bookshelf"));
        k.st(2869, 2982, 0, mc("skeleton_skull"));
        // the Shilo gem mine (OSRS: a ladder down at 2825, 2998; here gem rocks on the surface)
        k.floor(2820, 2994, 2830, 3003, mc("gravel"));
        for (int x = 2821; x <= 2829; x += 3) {
            for (int y = 2995; y <= 3002; y += 3) {
                k.st(x, y, 0, rc("gem_rock"));
            }
        }
        k.fenceRing(2820, 2994, 2830, 3003, mc("jungle_fence"));
        k.clearT(2830, 2998, 2830, 2999, 0, 0);
        palms(k, new int[][]{{2820, 2965}, {2822, 2984}, {2840, 2966}, {2846, 2980}, {2876, 2985}, {2862, 2996},
                {2846, 3002}, {2836, 3004}, {2858, 2946}, {2840, 2946}, {2826, 2948}, {2864, 2988}});
        return k.build();
    }

    // ------------------------------------------------------------------ Tai Bwo Wannai

    static StructureDef taiBwoWannai() {
        StructureDef.Builder b = StructureDef.builder("tai_bwo_wannai", "Tai Bwo Wannai").replaces("tai_bwo_wannai");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2776, 3036, 2812, 3068);
        int[] r = rect(2778, 3038, 2810, 3066);
        k.w(r[0], 0, r[1], r[2], 1, r[3], mc("jungle_fence"));
        k.clearT(2793, 3038, 2795, 3038, 0, 1);
        k.clearT(2793, 3066, 2795, 3066, 0, 1);
        k.clearT(2778, 3051, 2778, 3053, 0, 1);
        k.floor(2788, 3048, 2800, 3060, mc("coarse_dirt"));
        k.path(2793, 3038, 2795, 3048, mc("dirt_path"), mc("coarse_dirt"));
        k.path(2779, 3051, 2788, 3053, mc("dirt_path"), mc("coarse_dirt"));
        // totem pole
        int tx = bx(2798);
        int tz = bz(3059);
        String[] totem = {"jungle_log[axis=y]", "orange_terracotta", "jungle_log[axis=y]", "red_terracotta",
                "yellow_terracotta", "jungle_log[axis=y]", "carved_pumpkin[facing=south]"};
        for (int i = 0; i < totem.length; i++) {
            k.s(tx, i, tz, mc(totem[i]));
        }
        k.s(tx - 1, 4, tz, mc("jungle_fence"));
        k.s(tx + 1, 4, tz, mc("jungle_fence"));
        // huts in a ring and the chieftain's long hut
        AsgarniaRoom chief = k.house(KAR, 2785, 3060, 2793, 3065, 1, "south", PITCHED);
        chief.home("jungle", "red");
        chief.set(chief.len() / 2, 0, 0, mc("gold_block"));
        int[][] huts = {{2783, 3044}, {2803, 3044}, {2805, 3056}, {2782, 3055}, {2788, 3041}};
        String[] doors = {"east", "west", "west", "east", "east"};
        for (int i = 0; i < huts.length; i++) {
            roundHut(k, bx(huts[i][0]), bz(huts[i][1]), 4, mc("bamboo_block[axis=y]"), mc("hay_block"), doors[i]);
        }
        // trade store and the fire pit
        AsgarniaRoom store = k.house(KAR2, 2801, 3060, 2807, 3065, 1, "south", PITCHED);
        store.shop(rc("general_store"), mc("bamboo_slab[type=top]"), mc("barrel[facing=up]"), mc("chest[facing=south]"));
        int fx = bx(2790);
        int fz = bz(3050);
        k.ring(fx, fz, 1.5, -1, -1, mc("cobblestone"));
        k.s(fx, 0, fz, mc("campfire[lit=true]"));
        palms(k, new int[][]{{2780, 3040}, {2808, 3040}, {2808, 3064}, {2780, 3064}, {2800, 3050}, {2786, 3052}});
        return k.build();
    }

    // ------------------------------------------------------------------ Karamja volcano

    static StructureDef volcano() {
        StructureDef.Builder b = StructureDef.builder("karamja_volcano", "Karamja volcano").replaces("MOR_UL_REK");
        AsgarniaKit k = new AsgarniaKit(b);
        k.flatten(2828, 3148, 2880, 3192);
        int cx = bx(2862);
        int cz = bz(3168);
        int rr = 32;
        int rim = 24;
        int crater = 8;
        for (int dx = -rr; dx <= rr; dx++) {
            for (int dz = -rr; dz <= rr; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > rr) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                int hash = AsgarniaKit.noise(dx, dz) & 15;
                if (d <= crater) {
                    k.f(x, -3, z, x, 13, z, mc("blackstone"));
                    k.s(x, 14, z, d > crater - 1.2 ? mc("magma_block") : mc("lava"));
                    continue;
                }
                int h;
                if (d <= crater + 2.5) {
                    h = rim + (hash < 4 ? 1 : 0);
                } else {
                    h = (int) Math.round(rim * Math.pow((rr - d) / (rr - crater - 2.5), 1.25)) + (hash < 3 ? 1 : 0);
                }
                if (h <= 0) {
                    continue;
                }
                String body = d < 16 ? mc("blackstone") : mc("stone");
                String top;
                if (d < 14) {
                    top = hash < 5 ? mc("magma_block") : hash < 10 ? mc("blackstone") : mc("basalt[axis=y]");
                } else if (d < 22) {
                    top = hash < 6 ? mc("tuff") : hash < 11 ? mc("basalt[axis=y]") : mc("smooth_basalt");
                } else if (d < 27) {
                    top = hash < 7 ? mc("coarse_dirt") : hash < 12 ? mc("tuff") : mc("gravel");
                } else {
                    top = hash < 9 ? mc("grass_block") : mc("coarse_dirt");
                }
                k.f(x, -3, z, x, h - 2, z, body);
                k.s(x, h - 1, z, top);
            }
        }
        // signal smoke rising from the rim
        k.s(cx + 9, rim, cz, mc("hay_block"));
        k.s(cx + 9, rim + 1, cz, mc("campfire[lit=true,signal_fire=true]"));
        k.s(cx - 6, rim, cz - 7, mc("hay_block"));
        k.s(cx - 6, rim + 1, cz - 7, mc("campfire[lit=true,signal_fire=true]"));
        k.s(cx - 2, rim, cz + 9, mc("hay_block"));
        k.s(cx - 2, rim + 1, cz + 9, mc("campfire[lit=true,signal_fire=true]"));
        // the TzHaar gateway at the western foot: plaza, carved tunnel, bank and altar
        k.path(2838, 3160, 2847, 3172, mc("polished_basalt[axis=y]"), mc("obsidian"));
        int tz = bz(3166);
        int tx0 = bx(2846);
        k.c(tx0, 0, tz - 2, tx0 + 16, 4, tz + 3);
        k.f(tx0, -1, tz - 2, tx0 + 16, -1, tz + 3, mc("polished_blackstone"));
        k.f(tx0, 5, tz - 2, tx0 + 16, 5, tz + 3, mc("basalt[axis=y]"));
        k.f(tx0 + 16, 0, tz - 2, tx0 + 16, 4, tz + 3, mc("obsidian"));
        for (int x = tx0; x <= tx0 + 15; x += 5) {
            k.f(x, 0, tz - 2, x, 4, tz - 2, mc("polished_basalt[axis=y]"));
            k.f(x, 0, tz + 3, x, 4, tz + 3, mc("polished_basalt[axis=y]"));
            k.s(x + 2, 4, tz, mc("shroomlight"));
        }
        k.f(tx0 + 15, 0, tz - 1, tx0 + 15, 2, tz + 2, mc("crying_obsidian"));
        k.s(tx0 + 12, 0, tz - 1, rc("bank_booth"));
        k.s(tx0 + 12, 0, tz + 2, rc("prayer_altar"));
        k.s(tx0 + 11, 0, tz + 2, mc("magma_block"));
        k.f(tx0 - 1, 0, tz - 3, tx0 - 1, 6, tz - 3, mc("obsidian"));
        k.f(tx0 - 1, 0, tz + 4, tx0 - 1, 6, tz + 4, mc("obsidian"));
        k.f(tx0 - 1, 6, tz - 3, tx0 - 1, 6, tz + 4, mc("obsidian"));
        // jungle at the foot
        palms(k, new int[][]{{2832, 3152}, {2834, 3184}, {2878, 3150}, {2878, 3188}, {2846, 3190}, {2830, 3176}});
        return k.build();
    }

    static StructureDef tzhaarGate() {
        StructureDef.Builder b = StructureDef.builder("karamja_tzhaar_gate", "TzHaar cave gate").replaces("mg_tzhaar_city");
        AsgarniaKit k = new AsgarniaKit(b);
        int[] r = rect(2830, 3162, 2837, 3170);
        k.f(r[0], -1, r[1], r[2], -1, r[3], mc("obsidian"));
        k.w(r[0], -1, r[1], r[2], -1, r[3], mc("polished_blackstone"));
        // basalt pillars around the edge
        for (int x = r[0]; x <= r[2]; x += 5) {
            k.f(x, 0, r[1], x, 3, r[1], mc("basalt[axis=y]"));
            k.f(x, 0, r[3], x, 3, r[3], mc("basalt[axis=y]"));
            k.s(x, 4, r[1], mc("magma_block"));
            k.s(x, 4, r[3], mc("magma_block"));
        }
        // the Fight Caves and Inferno entrances as basalt arches on the west side
        int cz = (r[1] + r[3]) / 2;
        for (int[] e : new int[][]{{cz - 4, 0}, {cz + 3, 1}}) {
            int z = e[0];
            k.f(r[0], 0, z - 1, r[0], 4, z + 1, mc("blackstone"));
            k.f(r[0] + 1, 0, z, r[0] + 1, 2, z, mc("air"));
            k.s(r[0] + 1, 0, z, e[1] == 0 ? rc("fight_caves_entrance") : rc("inferno_entrance"));
            k.s(r[0], 3, z, mc("crying_obsidian"));
        }
        // the TzHaar store stall
        k.s(r[2] - 1, 0, cz, rc("tzhaar_store"));
        k.f(r[2] - 2, 0, cz - 1, r[2] - 2, 2, cz - 1, mc("polished_blackstone_wall"));
        k.f(r[2] - 2, 0, cz + 1, r[2] - 2, 2, cz + 1, mc("polished_blackstone_wall"));
        k.f(r[2] - 2, 3, cz - 1, r[2], 3, cz + 1, mc("polished_blackstone_slab[type=bottom]"));
        return k.build();
    }

    static StructureDef eelPool() {
        StructureDef.Builder b = StructureDef.builder("karamja_volcano_eel_pool", "Infernal eel pool").replaces("mor_ul_rek_eels");
        AsgarniaKit k = new AsgarniaKit(b);
        int cx = bx(2858);
        int cz = bz(3192);
        k.disc(cx, cz, 4.5, -2, -1, mc("obsidian"));
        k.disc(cx, cz, 2.5, -1, -1, mc("lava"));
        k.ring(cx, cz, 4, 0, 0, mc("polished_blackstone_wall"));
        k.c(cx - 4, 0, cz, cx - 4, 0, cz);
        k.s(cx - 1, 0, cz, rc("infernal_eel_fishing_spot"));
        k.s(cx + 1, 0, cz + 1, rc("infernal_eel_fishing_spot"));
        k.s(cx, 0, cz - 4, mc("soul_lantern"));
        return k.build();
    }
}
