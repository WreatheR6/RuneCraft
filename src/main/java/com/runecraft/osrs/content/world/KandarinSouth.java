package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

/** Southern Kandarin: Yanille and the Watchtower, Port Khazard, the Fight Arena, the Observatory and Tree Gnome Village. */
final class KandarinSouth {
    private KandarinSouth() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(yanille());
        out.add(watchtower());
        out.add(portKhazard());
        out.add(fishingTrawler());
        out.add(fightArena());
        out.add(observatory());
        out.add(treeGnomeVillage());
    }

    static final Pal KHAZ = new Pal("minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:spruce_planks", "minecraft:dark_oak", "dark_oak", PANE);

    // ------------------------------------------------------------------ Yanille

    static StructureDef yanille() {
        KandarinKit k = new KandarinKit("yanille", "Yanille").replaces("YANILLE");
        k.flatten(2528, 3067, 2624, 3112);
        k.flatten(2536, 3113, 2562, 3126);
        k.cwallRect(2532, 3071, 2620, 3108, 8, SB);
        KandarinSeers.weather(k, KandarinKit.X(2532), KandarinKit.Z(3108), KandarinKit.X(2620) + 1, KandarinKit.Z(3071) + 1, 0, 7);
        k.gate(2620, 3093, 2620, 3094, 4, COBBLE);
        k.gate(2532, 3093, 2532, 3094, 4, COBBLE);
        k.gate(2576, 3108, 2577, 3108, 4, COBBLE);
        for (int[] c : new int[][]{{2532, 3071}, {2620, 3071}, {2532, 3108}, {2620, 3108}}) {
            k.tower(c[0], c[1], 4, 11, SB, "minecraft:spruce_planks", null, ' ');
        }
        k.tower(2620, 3091, 3, 11, SB, "minecraft:spruce_planks", null, 'w');
        k.tower(2620, 3096, 3, 11, SB, "minecraft:spruce_planks", null, 'w');
        k.road(2533, 3093, 2619, 3094, COBBLE, GRAVEL);
        k.road(2576, 3072, 2577, 3107, COBBLE, GRAVEL);
        k.road(2555, 3072, 2556, 3092, COBBLE, GRAVEL);
        k.road(2613, 3091, 2614, 3092, COBBLE, null);
        k.road(2576, 3109, 2577, 3112, PATH, null);

        // Bank
        Bld bank = k.house(2611, 3085, 2617, 3090, 1, YANILLE.roof("minecraft:spruce"), 'n', NO_LADDER | WIDE_DOOR);
        int bz = bank.z2() - 2;
        k.fill(bank.x1() + 1, 0, bz, bank.x2() - 1, 0, bz, "minecraft:spruce_slab[type=top]");
        for (int x = bank.x1() + 2; x <= bank.x2() - 2; x += 3) {
            k.set(x, 0, bz, BANK);
        }
        k.set(bank.cx(), 2, bank.cz() - 1, "minecraft:lantern[hanging=true]");

        // Wizards' Guild
        int wx = KandarinKit.X(2591) + 1;
        int wz = KandarinKit.Z(3086) + 1;
        k.disc(wx, wz, -1, -1, 12, GRAVEL);
        k.ring(wx, wz, 0, 1, 12, "minecraft:stone_brick_wall");
        k.clear(wx - 1, 0, wz - 12, wx + 1, 1, wz - 12);
        k.fill(wx - 1, -1, wz - 15, wx + 1, -1, wz - 9, COBBLE);
        k.towerB(wx, wz, 8, 26, SB, "minecraft:dark_oak_planks", "minecraft:blue_terracotta", 'n', 6);
        k.set(wx, 0, wz + 5, RUNES);
        k.fill(wx - 2, 0, wz + 4, wx + 2, 0, wz + 4, "minecraft:dark_oak_slab[type=top]");
        k.set(wx, 0, wz + 4, RUNES);
        k.ring(wx, wz, 0, 1, 6, "minecraft:bookshelf");
        k.clear(wx - 1, 0, wz - 6, wx + 1, 1, wz - 6);
        k.clear(wx + 6, 0, wz - 1, wx + 6, 1, wz + 1);
        k.clear(wx - 6, 0, wz - 1, wx - 6, 1, wz + 1);
        k.set(wx, 6, wz, "minecraft:enchanting_table");
        k.set(wx - 3, 6, wz - 3, "minecraft:brewing_stand");
        k.set(wx + 3, 6, wz + 3, "minecraft:lectern[facing=north]");
        k.set(wx, 12, wz, "minecraft:amethyst_block");
        k.set(wx, 13, wz, "minecraft:amethyst_cluster");
        k.set(wx, 18, wz, "minecraft:crying_obsidian");
        k.set(wx, 5, wz, "minecraft:lantern[hanging=true]");
        k.set(wx, 11, wz, "minecraft:lantern[hanging=true]");

        // Dragon Inn
        Bld inn = k.house(2549, 3077, 2558, 3084, 2, YANILLE, 'n', FURN | WIDE_DOOR);
        k.fill(inn.x1() + 1, 0, inn.z2() - 3, inn.x2() - 4, 0, inn.z2() - 3, "minecraft:spruce_slab[type=top]");
        k.fill(inn.x1() + 1, 0, inn.z2() - 1, inn.x2() - 4, 1, inn.z2() - 1, "minecraft:barrel[facing=north]");
        k.set(inn.x1() + 1, 0, inn.z2() - 2, RANGE);
        // Smithy and apothecary
        Bld smith = k.house(2596, 3099, 2601, 3104, 1, STONE.roof("minecraft:spruce"), 's', NO_LADDER);
        k.set(smith.cx(), 0, smith.cz(), ANVIL);
        k.set(smith.x1() + 1, 0, smith.z1() + 1, "minecraft:blast_furnace");
        k.set(smith.x2() - 1, 0, smith.z1() + 1, "minecraft:smithing_table");
        k.set(smith.x2() - 1, 0, smith.z1() + 2, "minecraft:water_cauldron[level=3]");
        Bld apo = k.house(2564, 3096, 2570, 3101, 1, YANILLE, 's', NO_LADDER);
        k.counter(apo, HERB_TABLE, "oak");
        k.set(apo.x1() + 1, 0, apo.z2() - 1, "minecraft:brewing_stand");
        // Houses
        int[][] hs = {{2537, 3096, 2543, 3102, 's'}, {2545, 3096, 2551, 3102, 's'}, {2537, 3075, 2543, 3081, 'n'},
                {2560, 3075, 2566, 3081, 'n'}, {2568, 3075, 2574, 3081, 'n'}, {2584, 3097, 2590, 3103, 's'},
                {2603, 3097, 2609, 3103, 's'}, {2603, 3074, 2609, 3080, 'n'}, {2557, 3096, 2562, 3101, 's'},
                {2538, 3084, 2544, 3090, 'n'}, {2560, 3084, 2566, 3090, 'n'}, {2611, 3075, 2617, 3081, 'n'},
                {2611, 3098, 2617, 3104, 's'}};
        for (int i = 0; i < hs.length; i++) {
            int[] h = hs[i];
            k.house(h[0], h[1], h[2], h[3], 1 + (i & 1), i % 3 == 0 ? YANILLE.roof("minecraft:deepslate_tile") : YANILLE, (char) h[4], FURN);
        }
        // Hops garden (left open for the farming patch) and trees
        k.fence(2573, 3098, 2583, 3106, "minecraft:oak_fence");
        k.tClear(2576, 3098, 2577, 3098, 0, 0);
        k.tClear(2576, 3106, 2577, 3106, 0, 0);
        for (int[] t : new int[][]{{2547, 3089}, {2595, 3080}, {2582, 3073}, {2535, 3105}, {2617, 3105}}) {
            k.oak(KandarinKit.X(t[0]), KandarinKit.Z(t[1]), 5);
        }
        return k.build();
    }

    static StructureDef watchtower() {
        KandarinKit k = new KandarinKit("watchtower", "Watchtower").replaces("watchtower");
        k.road(2549, 3109, 2550, 3112, PATH, null);
        Pal p = STONE.roof("flat:minecraft:stone_bricks");
        Bld t = k.house(2547, 3113, 2553, 3119, 3, p, 's', FLAT | ROOF_LADDER | FURN);
        int top = t.top() + 1;
        // the four power crystals and the shield on the roof
        for (int[] c : new int[][]{{t.x1() + 1, t.z1() + 1}, {t.x2() - 1, t.z1() + 1}, {t.x1() + 1, t.z2() - 1}, {t.x2() - 1, t.z2() - 1}}) {
            k.set(c[0], top, c[1], "minecraft:quartz_pillar");
            k.set(c[0], top + 1, c[1], "minecraft:amethyst_cluster");
        }
        k.set(t.cx(), top, t.cz(), "minecraft:beacon");
        k.fence(2545, 3111, 2555, 3121, "minecraft:spruce_fence");
        k.tClear(2549, 3111, 2550, 3111, 0, 0);
        k.spruce(KandarinKit.X(2558), KandarinKit.Z(3122), 9);
        k.spruce(KandarinKit.X(2540), KandarinKit.Z(3118), 8);
        return k.build();
    }

    // ------------------------------------------------------------------ Port Khazard

    static StructureDef portKhazard() {
        KandarinKit k = new KandarinKit("port_khazard", "Port Khazard").replaces("port_khazard_docks");
        k.flatten(2648, 3136, 2696, 3184);
        k.water(2678, 3136, 2696, 3184, 3);
        k.road(2672, 3138, 2677, 3182, COBBLE, "minecraft:mossy_cobblestone");
        k.road(2650, 3157, 2671, 3158, COBBLE, GRAVEL);
        k.road(2657, 3159, 2658, 3167, COBBLE, GRAVEL);
        k.pier(2678, 3150, 2690, 3152, "dark_oak", true);
        k.pier(2678, 3176, 2688, 3177, "dark_oak", true);
        k.tClear(2678, 3150, 2678, 3152, 0, 0);
        k.tClear(2678, 3176, 2678, 3177, 0, 0);
        k.longboat(KandarinKit.X(2685), KandarinKit.Z(3147), 15, true, "black");
        k.set(KandarinKit.X(2675), 0, KandarinKit.Z(3153), "runecraft:port_khazard_task_board");
        for (int y = 3140; y <= 3180; y += 8) {
            k.lamp(KandarinKit.X(2677) + 1, KandarinKit.Z(y), "minecraft:dark_oak_fence");
        }
        // Khazard's fort
        String fw = "minecraft:polished_blackstone_bricks";
        k.cwallRect(2650, 3168, 2666, 3182, 7, fw);
        k.gate(2657, 3168, 2658, 3168, 4, COBBLE);
        for (int[] c : new int[][]{{2650, 3168}, {2666, 3168}, {2650, 3182}, {2666, 3182}}) {
            k.tower(c[0], c[1], 3, 11, fw, "minecraft:dark_oak_planks", null, ' ');
        }
        Bld keep = k.house(2653, 3173, 2662, 3180, 2, KHAZ.roof("flat:minecraft:polished_deepslate"), 's', FLAT | FURN);
        k.flag(keep.cx(), keep.top() + 1, keep.cz(), "red", "black");
        // Town
        k.house(2652, 3140, 2658, 3146, 2, KHAZ, 'e', FURN);
        k.house(2660, 3140, 2666, 3146, 1, KHAZ, 'n', FURN);
        Bld gen = k.house(2662, 3150, 2668, 3155, 1, KHAZ, 'e', NO_LADDER);
        k.counter(gen, GENERAL, "dark_oak");
        Bld bar = k.house(2652, 3150, 2659, 3156, 2, KHAZ.roof("minecraft:spruce"), 'e', FURN);
        k.fill(bar.x1() + 1, 0, bar.z1() + 2, bar.x1() + 1, 0, bar.z2() - 2, "minecraft:spruce_slab[type=top]");
        k.set(bar.x1() + 1, 0, bar.z2() - 1, RANGE);
        k.house(2664, 3159, 2669, 3164, 1, KHAZ, 'e', FURN);
        k.house(2650, 3160, 2655, 3165, 1, KHAZ, 'e', FURN);
        for (int y = 3139; y <= 3147; y += 4) {
            k.set(KandarinKit.X(2676), 0, KandarinKit.Z(y), "minecraft:barrel[facing=up]");
        }
        return k.build();
    }

    static StructureDef fishingTrawler() {
        KandarinKit k = new KandarinKit("fishing_trawler_pier", "Fishing Trawler").replaces("mg_port_khazard_trawler");
        k.pier(2678, 3165, 2694, 3167, "spruce", true);
        k.tClear(2678, 3165, 2678, 3167, 0, 0);
        k.set(KandarinKit.X(2680), 0, KandarinKit.Z(3166), "runecraft:fishing_trawler_entrance");
        // the trawler: a broad hull with a wheelhouse and nets
        int x = KandarinKit.X(2686);
        int z = KandarinKit.Z(3162);
        k.fill(x - 8, -3, z - 2, x + 8, -3, z + 2, "minecraft:dark_oak_planks");
        k.walls(x - 9, -2, z - 3, x + 9, 0, z + 3, "minecraft:spruce_planks");
        k.fill(x - 8, -2, z - 2, x + 8, -2, z + 2, "minecraft:spruce_planks");
        k.clear(x - 8, -1, z - 2, x + 8, 0, z + 2);
        k.fill(x + 10, -2, z - 1, x + 10, 1, z + 1, "minecraft:spruce_planks");
        k.set(x + 11, 1, z, "minecraft:spruce_planks");
        k.walls(x - 7, -1, z - 2, x - 3, 3, z + 2, "minecraft:white_terracotta");
        k.fill(x - 7, 4, z - 2, x - 3, 4, z + 2, "minecraft:dark_oak_slab");
        k.clear(x - 3, -1, z, x - 3, 0, z);
        k.set(x - 5, 2, z - 2, PANE);
        k.set(x - 5, 2, z + 2, PANE);
        k.fill(x + 2, -1, z, x + 2, 7, z, "minecraft:spruce_fence");
        k.fill(x + 2, 6, z - 3, x + 2, 6, z + 3, "minecraft:spruce_log[axis=z]");
        k.fill(x + 3, 2, z - 2, x + 7, 2, z + 2, "minecraft:cobweb");
        k.set(x, -1, z, "minecraft:barrel[facing=up]");
        k.set(x + 5, -1, z - 1, "minecraft:cauldron");
        return k.build();
    }

    // ------------------------------------------------------------------ Fight Arena

    /**
     * The Fight Arena as on the OSRS map (mejrs layers_osrs): the walled octagon 2581-2606 x 3151-3172 with
     * three prisoners' cells on its east side (2607-2611 x 3157-3166, gates into the arena), Khazard's
     * buildings round it and the gap in the south-east corner to the barracks. The quest's boss arenas
     * ({@code ArenaLayout}) are pushed out beside the compound by its levelled ground, as before.
     */
    static StructureDef fightArena() {
        KandarinKit k = new KandarinKit("fight_arena", "Fight Arena").replaces("fight_arena");
        String dark = "minecraft:deepslate_bricks";
        k.flatten(2560, 3136, 2622, 3190, "minecraft:coarse_dirt");
        // octagon: 26 x 22 tiles with 5-tile corner cuts; centre on the block line X(2594) / Z(3161)
        int ox = KandarinKit.X(2594);
        int oz = KandarinKit.Z(3161);
        for (int x = ox - 26; x < ox + 26; x++) {
            for (int z = oz - 22; z < oz + 22; z++) {
                double u = Math.abs(x + 0.5 - ox);
                double v = Math.abs(z + 0.5 - oz);
                boolean in = u + v <= 38;
                boolean rim = in && (u > 25 || v > 21 || u + v > 36.6);
                // the south-east gap to the barracks (2604-2606, 3151-3153)
                boolean gap = x >= KandarinKit.X(2603) && z >= KandarinKit.Z(3153) && x + 0.5 - ox > 0 && z + 0.5 - oz > 0
                        && u + v > 30;
                if (rim && !gap) {
                    k.fill(x, 0, z, x, 3, z, dark);
                    k.set(x, 4, z, "minecraft:deepslate_brick_wall");
                }
            }
        }
        // the three prisoners' cells on the east side, iron gates into the arena
        int cx1 = KandarinKit.X(2607);
        int cx2 = KandarinKit.X(2611) + 1;
        k.fill(cx1, -1, KandarinKit.Z(3166), cx2, -1, KandarinKit.Z(3157) + 1, "minecraft:cobbled_deepslate");
        k.fill(cx1 + 1, 0, KandarinKit.Z(3166) - 1, cx2, 3, KandarinKit.Z(3157) + 1, dark);
        for (int[] c : new int[][]{{3164, 3166}, {3161, 3163}, {3157, 3160}}) {
            k.clear(cx1 - 1, 0, KandarinKit.Z(c[1]), cx2 - 1, 2, KandarinKit.Z(c[0]));
            int gz = KandarinKit.Z((c[0] + c[1]) / 2);
            k.fill(cx1 - 1, 0, KandarinKit.Z(c[1]), cx1 - 1, 2, KandarinKit.Z(c[0]), "minecraft:iron_bars");
            k.clear(cx1 - 1, 0, gz, cx1 - 1, 1, gz);
            k.set(cx2 - 1, 0, KandarinKit.Z(c[0]), "minecraft:hay_block");
        }
        k.fill(cx1, 4, KandarinKit.Z(3166) - 1, cx2, 4, KandarinKit.Z(3157) + 1, "minecraft:deepslate_tiles");
        // west: the L-shaped house (2563-2571 x 3170-3172 and 2568-2571 x 3168-3169), the hut 2563-2567 x
        // 3159-3166 and the stepped hall 2563-2572 x 3139-3154 // approx: the steps are rectangles
        k.house(2563, 3170, 2571, 3172, 1, KHAZ, ' ', FURN | NO_LADDER);
        k.house(2568, 3168, 2571, 3170, 1, KHAZ, 's', NO_LADDER);
        k.clear(KandarinKit.X(2568) + 1, 0, KandarinKit.Z(3170), KandarinKit.X(2571), 2, KandarinKit.Z(3170) + 1);
        k.house(2563, 3159, 2567, 3166, 1, KHAZ, 'e', FURN | NO_LADDER);
        Bld hall = k.house(2563, 3139, 2572, 3150, 1, KHAZ, 'n', FURN | NO_LADDER);
        k.house(2563, 3151, 2567, 3154, 1, KHAZ, ' ', NO_LADDER);
        k.clear(KandarinKit.X(2563) + 1, 0, KandarinKit.Z(3151) + 1, KandarinKit.X(2567), 2, KandarinKit.Z(3150));
        k.set(hall.x1() + 1, 0, hall.z1() + 1, RANGE);
        // south: the bar and its three side rooms (2585-2603 x 3139-3144), corridor east to the barracks
        Bld bar = k.house(2585, 3139, 2603, 3144, 1, KHAZ, 'w', FURN | NO_LADDER);
        k.fill(bar.x1() + 1, 0, KandarinKit.Z(3142) + 1, bar.x2() - 1, 2, KandarinKit.Z(3142) + 1, dark);
        k.fill(KandarinKit.X(2592), 0, bar.z1() + 1, KandarinKit.X(2592), 2, KandarinKit.Z(3142), dark);
        k.fill(KandarinKit.X(2597), 0, bar.z1() + 1, KandarinKit.X(2597), 2, KandarinKit.Z(3142), dark);
        for (int dx : new int[]{2589, 2595, 2600}) {
            k.clear(KandarinKit.X(dx), 0, KandarinKit.Z(3142) + 1, KandarinKit.X(dx), 1, KandarinKit.Z(3142) + 1);
        }
        k.fill(KandarinKit.X(2587), 0, KandarinKit.Z(3141), KandarinKit.X(2591), 0, KandarinKit.Z(3141),
                "minecraft:dark_oak_slab[type=top]");
        k.house(2604, 3140, 2608, 3141, 1, KHAZ, ' ', NO_LADDER | NO_WINDOWS);
        k.clear(bar.x2(), 0, KandarinKit.Z(3141), bar.x2(), 2, KandarinKit.Z(3140) + 1);
        // south-east: Khazard's barracks and quarters (2609-2619 x 3139-3145, 2613-2619 x 3146-3150) and the
        // east cell block 2614-2619 x 3155-3170 joined by a passage at 2616-2617
        k.clear(KandarinKit.X(2608), 0, KandarinKit.Z(3141), KandarinKit.X(2609) + 1, 2, KandarinKit.Z(3140) + 1);
        Bld kq = k.house(2609, 3139, 2619, 3145, 2, KHAZ, 'w', FURN);
        k.set(kq.x2() - 1, 0, kq.z1() + 1, RANGE);
        k.house(2613, 3146, 2619, 3150, 1, KHAZ, 'n', FURN | NO_LADDER);
        k.house(2605, 3146, 2611, 3152, 1, KHAZ, 'n', FURN | NO_LADDER);
        k.house(2616, 3151, 2617, 3154, 1, KHAZ, ' ', NO_LADDER | NO_WINDOWS);
        k.clear(KandarinKit.X(2616) + 1, 0, KandarinKit.Z(3150), KandarinKit.X(2617), 2, KandarinKit.Z(3155) + 1);
        Bld cells = k.house(2614, 3155, 2619, 3170, 1, KHAZ, ' ', NO_LADDER);
        k.fill(KandarinKit.X(2617), 0, cells.z1() + 1, KandarinKit.X(2617), 2, cells.z2() - 1, "minecraft:iron_bars");
        for (int cz : new int[]{3159, 3163, 3167}) {
            k.fill(cells.x1() + 1, 0, KandarinKit.Z(cz) + 1, KandarinKit.X(2616) + 1, 2, KandarinKit.Z(cz) + 1, dark);
            k.clear(KandarinKit.X(2617), 0, KandarinKit.Z(cz + 1), KandarinKit.X(2617), 1, KandarinKit.Z(cz + 1));
        }
        // north: the guards' hall (2589-2600 x 3180-3184 with its wing 2591-2598 x 3185-3187) and the
        // armoury 2605-2612 x 3180-3182 (2607-2610 to 3186) // approx
        k.house(2589, 3180, 2600, 3184, 1, KHAZ, 's', FURN | NO_LADDER);
        k.house(2591, 3185, 2598, 3187, 1, KHAZ, ' ', NO_LADDER);
        k.clear(KandarinKit.X(2591) + 1, 0, KandarinKit.Z(3185) + 1, KandarinKit.X(2598), 2, KandarinKit.Z(3184));
        k.house(2605, 3180, 2612, 3182, 1, KHAZ, 's', FURN | NO_LADDER);
        k.house(2607, 3183, 2610, 3186, 1, KHAZ, ' ', NO_LADDER);
        k.clear(KandarinKit.X(2607) + 1, 0, KandarinKit.Z(3183) + 1, KandarinKit.X(2610), 2, KandarinKit.Z(3182));
        // the prison hut in the far north-west (2562-2567 x 3184-3189) // approx
        k.house(2562, 3184, 2567, 3189, 1, TROLL, 'e', NO_LADDER);
        return k.build();
    }

    // ------------------------------------------------------------------ Observatory

    static StructureDef observatory() {
        KandarinKit k = new KandarinKit("observatory", "Observatory").replaces("observatory");
        k.flatten(2424, 3148, 2452, 3176);
        int cx = KandarinKit.X(2436) + 1;
        int cz = KandarinKit.Z(3164) + 1;
        k.disc(cx, cz, -1, -1, 11, GRAVEL);
        k.towerB(cx, cz, 8, 7, SB, "minecraft:spruce_planks", null, 's', 7);
        // dome shell
        for (int dy = 0; dy <= 8; dy++) {
            int r = (int) Math.round(Math.sqrt(Math.max(0, 64 - dy * dy)));
            if (r <= 0) {
                k.set(cx, 7 + dy, cz, "minecraft:white_concrete");
            } else {
                k.ring(cx, cz, 7 + dy, 7 + dy, r, "minecraft:white_concrete");
            }
        }
        k.disc(cx, cz, 7, 7, 7, AIR);
        // slit and telescope
        k.clear(cx - 1, 9, cz - 9, cx + 1, 16, cz - 2);
        k.clear(cx - 1, 14, cz - 3, cx + 1, 16, cz + 1);
        for (int i = 0; i < 7; i++) {
            k.set(cx, 7 + i, cz + 2 - i, i == 6 ? "minecraft:glass" : "minecraft:black_concrete");
        }
        k.fill(cx, 6, cz + 2, cx, 6, cz + 3, "minecraft:iron_block");
        k.set(cx + 2, 6, cz + 2, "minecraft:lectern[facing=west]");
        k.set(cx - 3, 0, cz - 2, "minecraft:cartography_table");
        k.set(cx + 3, 0, cz - 2, "minecraft:bookshelf");
        k.set(cx + 3, 1, cz - 2, "minecraft:bookshelf");
        k.set(cx, 5, cz, "minecraft:lantern[hanging=true]");
        // Professor's house and gnome hut
        k.house(2444, 3166, 2449, 3171, 1, STONE.roof("minecraft:spruce"), 'w', FURN | NO_LADDER);
        k.gnomeHut(KandarinKit.X(2428), KandarinKit.Z(3152), 3, "minecraft:jungle_planks", "minecraft:jungle_leaves[persistent=true]", 'e');
        k.road(2440, 3156, 2441, 3163, PATH, null);
        k.road(2436, 3155, 2441, 3156, PATH, null);
        k.oak(KandarinKit.X(2448), KandarinKit.Z(3154), 5);
        return k.build();
    }

    // ------------------------------------------------------------------ Tree Gnome Village

    static StructureDef treeGnomeVillage() {
        KandarinKit k = new KandarinKit("tree_gnome_village", "Tree Gnome Village").replaces("tree_gnome_village");
        k.flatten(2500, 3140, 2550, 3190);
        int cx = KandarinKit.X(2525);
        int cz = KandarinKit.Z(3165);
        String w = "minecraft:stripped_spruce_wood";
        int[] rings = {46, 36, 26};
        for (int r : rings) {
            k.walls(cx - r, 0, cz - r, cx + r, 3, cz + r, w);
        }
        // openings: outer north-east, middle south-west, inner west gate
        k.clear(cx + 20, 0, cz - 46, cx + 22, 3, cz - 46);
        k.clear(cx - 22, 0, cz + 36, cx - 20, 3, cz + 36);
        k.clear(cx - 26, 0, cz - 1, cx - 26, 3, cz + 1);
        // dead-end blockers between the rings
        k.fill(cx - 46, 0, cz - 2, cx - 36, 3, cz - 2, w);
        k.fill(cx + 36, 0, cz + 10, cx + 46, 3, cz + 10, w);
        k.fill(cx + 4, 0, cz + 26, cx + 4, 3, cz + 36, w);
        k.fill(cx - 10, 0, cz - 36, cx - 10, 3, cz - 26, w);
        k.fill(cx - 36, 0, cz + 30, cx - 30, 3, cz + 30, w);
        k.fill(cx + 30, 0, cz - 36, cx + 30, 3, cz - 30, w);
        k.fill(cx - 46, -1, cz - 46, cx + 46, -1, cz + 46, "minecraft:moss_block");
        k.fill(cx - 25, -1, cz - 25, cx + 25, -1, cz + 25, GRASS);
        // village: spirit tree, King Bolren's hut, huts, tree houses
        int sx = cx + 10;
        int sz = cz - 10;
        k.bigTree(sx, sz, 10, 5, "minecraft:oak_log", "minecraft:oak_leaves");
        k.fill(sx - 1, 0, sz - 1, sx + 1, 6, sz + 1, "minecraft:oak_wood");
        k.set(sx, 3, sz + 2, "minecraft:carved_pumpkin[facing=south]");
        k.gnomeHut(cx - 12, cz - 12, 5, "minecraft:jungle_planks", HAY, 's');
        k.set(cx - 12, 0, cz - 15, "minecraft:jungle_stairs[facing=north]");
        k.gnomeHut(cx + 12, cz + 12, 4, "minecraft:jungle_planks", "minecraft:jungle_leaves[persistent=true]", 'w');
        k.gnomeHut(cx - 12, cz + 12, 4, "minecraft:jungle_planks", HAY, 'e');
        // a tree house with a ladder
        int tx = cx + 16;
        int tz = cz + 2;
        k.fill(tx, 0, tz, tx, 9, tz, "minecraft:jungle_log");
        k.disc(tx, tz, 6, 6, 3, "minecraft:jungle_planks");
        k.ring(tx, tz, 7, 7, 3, "minecraft:jungle_fence");
        k.blob(tx, 11, tz, 4, 2, 4, "minecraft:jungle_leaves[persistent=true]", true);
        k.ladder(tx - 1, tz, 0, 6, "west");
        k.clear(tx - 1, 7, tz, tx - 1, 7, tz);
        // stream with a bridge
        k.fill(cx - 25, -2, cz + 22, cx + 25, -2, cz + 23, WATER);
        k.clear(cx - 25, -1, cz + 22, cx + 25, -1, cz + 23);
        k.fill(cx - 2, -1, cz + 21, cx + 2, -1, cz + 24, "minecraft:jungle_planks");
        k.meadow(cx - 25, cz - 25, cx + 25, cz + 21, 7);
        k.meadow(cx - 45, cz - 45, cx + 45, cz + 45, 15);
        return k.build();
    }
}
