package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

/** Camelot, Seers' Village, Catherby, the Sorcerer's Tower, Keep Le Faye and the eastern Kandarin guilds. */
final class KandarinSeers {
    private KandarinSeers() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(seers());
        out.add(camelot());
        out.add(catherby());
        out.add(catherbyPier());
        out.add(keepLeFaye());
        out.add(sorcerersTower());
        out.add(legendsGuild());
        out.add(rangingGuild());
        out.add(hemenster());
        out.add(fishingGuild());
    }

    // ------------------------------------------------------------------ Seers' Village

    static StructureDef seers() {
        KandarinKit k = new KandarinKit("seers_village", "Seers' Village").replaces("SEERS_VILLAGE");
        // Ground for Seers' and Camelot (Camelot is built by its own structure right after this one).
        k.flatten(2686, 3455, 2742, 3510);
        k.flatten(2726, 3436, 2754, 3456);
        k.flatten(2743, 3466, 2782, 3526);

        String dirt = "minecraft:coarse_dirt";
        k.road(2686, 3483, 2737, 3484, PATH, dirt);
        k.road(2736, 3478, 2737, 3482, PATH, dirt);
        k.road(2736, 3478, 2755, 3479, PATH, dirt);
        k.road(2725, 3445, 2726, 3482, PATH, dirt);
        k.road(2725, 3445, 2737, 3446, PATH, dirt);
        k.road(2736, 3485, 2737, 3495, PATH, dirt);
        k.road(2703, 3485, 2704, 3485, PATH, null);
        k.road(2711, 3474, 2712, 3482, PATH, dirt);

        // --- Bank (rooftop course start) ---
        // OSRS map: a broad north hall 2719-2730 x 3494-3497, the chequered banking floor 2721-2729 x 3490-3493
        // and a porch 2724-2727 x 3487-3489 with the door south; booths across the hall facing south
        String[] bankPlan = {
                "############", // 3497
                "############", // 3496
                "############", // 3495
                "############", // 3494
                "  #########", // 3493
                "  #########", // 3492
                "  #########", // 3491
                "  #########", // 3490
                "     ####", // 3489
                "     ####", // 3488
                "     ####", // 3487
        };
        TileMask bankMask = TileMask.of(2719, 3497, bankPlan);
        bankMask.fill(k.b, k.g - 1, k.g - 1, ANDESITE).inside(k.b, k.g, k.g + 4, null)
                .outline(k.b, k.g, k.g + 3, SB).fill(k.b, k.g + 4, k.g + 4, "minecraft:smooth_stone");
        for (int tx = 2721; tx <= 2729; tx++) {
            for (int ty = 3490; ty <= 3495; ty++) {
                if (((tx + ty) & 1) == 0) {
                    k.floor(tx, ty, tx, ty, "minecraft:polished_diorite");
                }
            }
        }
        Bld bank = new Bld(KandarinKit.X(2721), KandarinKit.Z(3493), KandarinKit.X(2729) + 1, KandarinKit.Z(3490) + 1,
                1, 's', KandarinKit.X(2725) + 1, KandarinKit.Z(3487) + 1);
        k.tClear(2725, 3487, 2726, 3487, 0, 2);
        int bzc = KandarinKit.Z(3495) + 1;
        k.fill(KandarinKit.X(2720), 0, bzc, KandarinKit.X(2730), 0, bzc, "minecraft:spruce_slab[type=top]");
        for (int x = KandarinKit.X(2720) + 1; x <= KandarinKit.X(2730); x += 3) {
            k.set(x, 0, bzc, BANK);
        }
        k.fill(KandarinKit.X(2719) + 1, 0, KandarinKit.Z(3497) + 1, KandarinKit.X(2730), 0, KandarinKit.Z(3497) + 1, "minecraft:barrel[facing=south]");
        k.fill(bank.doorX(), 0, bzc + 1, bank.doorX() + 1, 0, bank.z2() + 4, "minecraft:red_carpet");
        k.set(KandarinKit.X(2722), 3, KandarinKit.Z(3492), "minecraft:lantern[hanging=true]");
        k.set(KandarinKit.X(2728), 3, KandarinKit.Z(3492), "minecraft:lantern[hanging=true]");
        for (int x = KandarinKit.X(2719); x <= KandarinKit.X(2730) + 1; x += 3) {
            k.set(x, 1, KandarinKit.Z(3497), PANE);
        }
        k.lamp(KandarinKit.X(2723), KandarinKit.Z(3486), "minecraft:spruce_fence");
        k.lamp(KandarinKit.X(2728), KandarinKit.Z(3486), "minecraft:spruce_fence");

        // --- Rooftop course houses (flat roofs) ---
        Pal flat = SEERS.roof("flat:minecraft:spruce_planks");
        Bld hb = k.house(2711, 3488, 2716, 3495, 1, flat, 's', FURN | FLAT | NO_LADDER);
        Bld hc = k.house(2709, 3466, 2715, 3473, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld hd = k.house(2700, 3466, 2706, 3472, 1, flat, 's', FURN | FLAT | NO_LADDER);
        Bld he = k.house(2691, 3467, 2697, 3473, 1, flat, 'n', FURN | FLAT | NO_LADDER);
        seersCourse(k, bank, hb, hc, hd, he);

        // --- Spinning wheel house (wheel upstairs) ---
        Bld spin = k.house(2702, 3486, 2707, 3492, 2, SEERS, 's', FURN);
        k.set(spin.x1() + 2, 4, spin.z2() - 2, WHEEL);
        k.set(spin.x1() + 1, 4, spin.z2() - 1, "minecraft:white_wool");
        k.set(spin.x1() + 1, 0, spin.z2() - 1, "minecraft:loom[facing=east]");

        // --- Forester's Arms: OSRS footprint 2689-2700 x 3487-3498 (mejrs layers_osrs), door south at 2694-2695,
        // the back room 2697-2700 x 3487-3493 behind its own door; OSRS has stairs up at 2698,3495, here a
        // ladder on the back-room wall // approx ---
        Bld pub = k.house(2689, 3487, 2700, 3498, 2, SEERS.roof("minecraft:spruce"), 's', NO_LADDER);
        k.fill(KandarinKit.X(2697), 0, KandarinKit.Z(3493), KandarinKit.X(2697), 2, pub.z2() - 1, SEERS.wall());
        k.fill(KandarinKit.X(2697) + 1, 0, KandarinKit.Z(3493), pub.x2() - 1, 2, KandarinKit.Z(3493), SEERS.wall());
        k.clear(KandarinKit.X(2698), 0, KandarinKit.Z(3493), KandarinKit.X(2698), 1, KandarinKit.Z(3493));
        k.set(KandarinKit.X(2699), 0, KandarinKit.Z(3491), "minecraft:barrel[facing=up]");
        k.set(KandarinKit.X(2700), 0, KandarinKit.Z(3491), "minecraft:barrel[facing=up]");
        k.ladder(KandarinKit.X(2699), KandarinKit.Z(3493) - 1, 0, 3, "north");
        int bar = pub.z1() + 3;
        k.fill(pub.x1() + 1, 0, bar, pub.x2() - 4, 0, bar, "minecraft:spruce_slab[type=top]");
        k.fill(pub.x1() + 1, 0, pub.z1() + 1, pub.x2() - 2, 0, pub.z1() + 1, "minecraft:barrel[facing=south]");
        k.set(pub.x1() + 2, 1, bar, "minecraft:lantern");
        k.set(pub.x2() - 2, 0, pub.z1() + 1, RANGE);
        k.table(pub.x1() + 3, 0, pub.z2() - 3, "spruce");
        k.table(pub.x2() - 3, 0, pub.z2() - 3, "spruce");
        k.set(pub.cx(), 2, pub.cz(), "minecraft:lantern[hanging=true]");
        for (int x = pub.x1() + 2; x <= pub.x2() - 2; x += 3) {
            k.bed(x, 4, pub.z1() + 1, "green");
        }
        k.set(pub.x1() - 1, 2, pub.z2() + 1, "minecraft:spruce_fence");

        // --- Elemental Workshop house ---
        Pal ewPal = STONE.wall("minecraft:deepslate_bricks").roof("minecraft:cobblestone");
        Bld ew = k.house(2716, 3475, 2721, 3481, 1, ewPal, 'n', NO_LADDER);
        k.fill(ew.x1() + 1, 0, ew.z2() - 1, ew.x2() - 1, 1, ew.z2() - 1, "minecraft:bookshelf");
        k.set(ew.cx(), 0, ew.z2() - 1, "minecraft:chiseled_bookshelf[facing=north]");
        k.fill(ew.x2(), 0, ew.z1() + 2, ew.x2(), 2, ew.z1() + 3, "minecraft:cracked_deepslate_bricks");
        k.set(ew.x1() + 1, 0, ew.z1() + 1, "minecraft:lectern[facing=south]");

        // --- Range house, archery, fillers ---
        Bld rh = k.house(2730, 3475, 2735, 3480, 1, SEERS, 'n', FURN | NO_LADDER);
        k.set(rh.cx(), 0, rh.z2() - 1, RANGE);
        Bld arch = k.house(2701, 3497, 2707, 3503, 1, SEERS, 's', NO_LADDER);
        k.counter(arch, ARCHERY, "oak");
        k.set(arch.x1() + 1, 0, arch.z2() - 1, "minecraft:fletching_table");
        k.house(2690, 3476, 2695, 3481, 1, SEERS, 'n', FURN);
        k.house(2699, 3476, 2704, 3481, 2, SEERS, 'n', FURN);
        k.house(2715, 3457, 2720, 3462, 1, SEERS, 'e', FURN);
        k.house(2722, 3497, 2728, 3502, 1, SEERS, 's', FURN);
        k.house(2709, 3497, 2715, 3503, 2, SEERS.roof("minecraft:spruce"), 's', FURN);

        // --- Courthouse: OSRS 2732-2740 x 3464-3471 on its pale stone terrace 2731-2740 x 3463-3474, door north at
        // 2735-2736, the court room with the two holding rooms along the south (doors 2734 and 2738) ---
        k.fill(KandarinKit.X(2731), -1, KandarinKit.Z(3474), KandarinKit.X(2740) + 1, -1, KandarinKit.Z(3463) + 1,
                "minecraft:smooth_sandstone");
        Bld ct = k.house(2732, 3464, 2740, 3471, 2, STONE.roof("minecraft:spruce"), 'n', WIDE_DOOR | NO_LADDER);
        int hold = KandarinKit.Z(3466);
        k.fill(ct.x1() + 1, 0, hold, ct.x2() - 1, 2, hold, SB);
        k.fill(KandarinKit.X(2736), 0, hold + 1, KandarinKit.X(2736), 2, ct.z2() - 1, SB);
        k.clear(KandarinKit.X(2734), 0, hold, KandarinKit.X(2734), 1, hold);
        k.clear(KandarinKit.X(2738), 0, hold, KandarinKit.X(2738), 1, hold);
        k.fill(ct.x1() + 1, -1, hold + 1, ct.x2() - 1, -1, ct.z2() - 1, "minecraft:deepslate_tiles");
        // the judge's bench against the south wall, public benches facing it, the stairs up at 2737,3468
        k.fill(ct.x1() + 2, 0, hold - 2, ct.x1() + 6, 0, hold - 2, "minecraft:spruce_planks");
        k.set(ct.x1() + 4, 1, hold - 1, "minecraft:spruce_stairs[facing=north]");
        k.set(ct.x1() + 4, 2, hold - 1, "minecraft:red_banner[rotation=0]");
        for (int z = ct.z1() + 2; z <= hold - 4; z += 2) {
            k.fill(ct.x1() + 2, 0, z, ct.x1() + 6, 0, z, "minecraft:spruce_stairs[facing=south]");
        }
        k.ladder(ct.x2() - 1, KandarinKit.Z(3468), 0, 3, "west"); // OSRS stairs at 2737,3468 // approx
        k.set(ct.cx(), 2, ct.cz(), "minecraft:lantern[hanging=true]");

        // --- Church with bell tower ---
        Pal churchPal = STONE.floor(ANDESITE).roof("minecraft:deepslate_tile");
        Bld ch = k.house(2733, 3496, 2741, 3505, 1, churchPal, 's', NO_LADDER | NO_CEILING);
        churchInterior(k, ch);
        int tx1 = ch.x1();
        int tz2 = ch.z2();
        k.walls(tx1, 0, tz2 - 4, tx1 + 4, 11, tz2, SB);
        k.clear(tx1 + 1, 0, tz2 - 3, tx1 + 3, 11, tz2 - 1);
        k.fill(tx1 + 1, 8, tz2 - 3, tx1 + 3, 8, tz2 - 1, "minecraft:spruce_planks");
        k.set(tx1 + 2, 10, tz2 - 2, "minecraft:bell[attachment=ceiling]");
        k.fill(tx1 + 2, 11, tz2 - 2, tx1 + 2, 11, tz2 - 2, SB);
        k.set(tx1 + 2, 9, tz2, AIR);
        k.set(tx1, 9, tz2 - 2, AIR);
        k.hipRoof(tx1, tz2 - 4, tx1 + 4, tz2, 12, "minecraft:deepslate_tile");
        k.ladder(tx1 + 1, tz2 - 1, 0, 8, "east");

        // --- Coal trucks ---
        k.fill(KandarinKit.X(2686), -1, KandarinKit.Z(3505), KandarinKit.X(2698) + 1, -1, KandarinKit.Z(3505) + 1, GRAVEL);
        k.fill(KandarinKit.X(2686), 0, KandarinKit.Z(3505), KandarinKit.X(2698) + 1, 0, KandarinKit.Z(3505), "minecraft:rail[shape=east_west]");
        for (int i = 0; i < 3; i++) {
            int x = KandarinKit.X(2688) + i * 6;
            int z = KandarinKit.Z(3505) + 1;
            k.walls(x, 0, z, x + 2, 1, z + 2, "minecraft:dark_oak_planks");
            k.fill(x + 1, 0, z + 1, x + 1, 1, z + 1, "minecraft:coal_block");
            k.set(x, -1, z + 3, "minecraft:dark_oak_fence");
        }
        k.fill(KandarinKit.X(2686), 0, KandarinKit.Z(3507), KandarinKit.X(2687), 2, KandarinKit.Z(3507) + 1, "minecraft:coal_ore");

        // --- Flax field, beehives, trees ---
        k.flax(2738, 3438, 2750, 3451);
        k.fence(2737, 3437, 2751, 3452, "minecraft:oak_fence");
        k.tClear(2737, 3445, 2737, 3445, 0, 0);
        for (int y = 3439; y <= 3449; y += 3) {
            k.at(2753, y, 0, "minecraft:beehive[facing=west]");
        }
        int[][] yews = {{2690, 3460}, {2696, 3458}, {2702, 3461}, {2708, 3457}};
        for (int[] t : yews) {
            k.bigTree(KandarinKit.X(t[0]), KandarinKit.Z(t[1]), 7, 3, "minecraft:dark_oak_log", "minecraft:dark_oak_leaves");
        }
        for (int x = 2716; x <= 2736; x += 5) {
            k.bigTree(KandarinKit.X(x), KandarinKit.Z(3507), 6, 2, "minecraft:oak_log", "minecraft:acacia_leaves");
        }
        k.oak(KandarinKit.X(2742), KandarinKit.Z(3488), 6);
        k.oak(KandarinKit.X(2720), KandarinKit.Z(3466), 5);
        k.well(KandarinKit.X(2733), KandarinKit.Z(3486));
        k.meadow(KandarinKit.X(2686), KandarinKit.Z(3510), KandarinKit.X(2742) + 1, KandarinKit.Z(3455) + 1, 11);
        seersDetails(k, pub, bank, spin);
        return k.build();
    }

    /**
     * Seers' Village life: cottage gardens, the Forester's Arms' sign, benches and beer barrels
     * outside the pub, a sign post at the crossroads, hay and crates, and upstairs rooms in the pub.
     */
    private static void seersDetails(KandarinKit k, Bld pub, Bld bank, Bld spin) {
        int g = k.g;
        // pub: hanging sign, benches and barrels out front, rooms upstairs
        k.d.air(pub.x2() + 1, g + 3, pub.z2() - 1, pub.x2() + 2, g + 3, pub.z2() - 1, "minecraft:spruce_fence");
        k.d.air(pub.x2() + 2, g + 2, pub.z2() - 1, pub.x2() + 2, g + 2, pub.z2() - 1, "minecraft:spruce_hanging_sign[rotation=4]");
        k.d.bench(pub.x1() + 1, g, pub.z2() + 3, 'e', 2, 'n', "spruce");
        k.d.bench(pub.x2() - 2, g, pub.z2() + 3, 'e', 2, 'n', "spruce");
        k.d.air(pub.x1() - 1, g, pub.z2() - 3, pub.x1() - 1, g + 1, pub.z2() - 3, "minecraft:barrel[facing=up]");
        k.d.air(pub.x1() - 1, g, pub.z2() - 4, pub.x1() - 1, g, pub.z2() - 4, "minecraft:barrel[facing=west]");
        k.d.air(pub.x1() + 1, g + 4, pub.z2() - 3, pub.x2() - 1, g + 4, pub.z2() - 1, "minecraft:green_carpet");
        k.d.air(pub.cx(), g + 4, pub.z2() - 2, pub.cx(), g + 4, pub.z2() - 2, "minecraft:spruce_slab[type=top]");
        k.d.air(pub.cx(), g + 5, pub.z2() - 2, pub.cx(), g + 5, pub.z2() - 2, "minecraft:candle[candles=3,lit=true]");
        k.d.air(pub.x2() - 1, g + 4, pub.cz(), pub.x2() - 1, g + 5, pub.cz(), "minecraft:bookshelf");
        // a strip of cottage flowers along the front of the spinning house
        String[] fl = {"minecraft:poppy", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:allium"};
        for (int x = spin.x1(); x <= spin.x2(); x++) {
            if (x < spin.doorX() - 1 || x > spin.doorX() + 1) {
                k.d.air(x, g, spin.z2() + 1, x, g, spin.z2() + 1, fl[Math.floorMod(x, fl.length)]);
            }
        }
        // a sign post at the crossroads by the bank, hay, crates and a bench at the well
        k.d.signpost(KandarinKit.X(2735), g, KandarinKit.Z(3483) + 2, "spruce");
        k.d.air(KandarinKit.X(2738), g, KandarinKit.Z(3475), KandarinKit.X(2739), g, KandarinKit.Z(3475), "minecraft:hay_block");
        k.d.air(KandarinKit.X(2738), g + 1, KandarinKit.Z(3475), KandarinKit.X(2738), g + 1, KandarinKit.Z(3475), "minecraft:hay_block");
        k.d.crates(bank.x2() + 2, g, bank.z1() + 1);
        k.d.bench(KandarinKit.X(2731), g, KandarinKit.Z(3486) + 4, 'e', 3, 'n', "spruce");
    }

    /** Seers' Village rooftop course (6 obstacles): bank wall, gap, tightrope, gap, gap, edge. */
    private static void seersCourse(KandarinKit k, Bld bank, Bld hb, Bld hc, Bld hd, Bld he) {
        int c = 6;
        int top = 4;
        // 0: climb the bank's east wall
        int lz = bank.z1() + 3;
        for (int dz = 0; dz < 6; dz++) {
            if (k.ladder(bank.x2() + 1, bank.z1() + 3 + dz, 0, top, "east")) {
                lz = bank.z1() + 3 + dz;
                break;
            }
        }
        k.set(bank.x2(), top + 1, lz, AIR);
        k.set(bank.x2() - 1, top, lz, obstacle(c, 0));
        // 1: jump the gap west onto house B
        int z1 = KandarinKit.Z(3491);
        k.set(bank.x1(), top + 1, z1, AIR);
        k.set(hb.x2(), top + 1, z1, AIR);
        k.fill(hb.x2() + 1, top, z1, bank.x1() - 1, top, z1, "minecraft:oak_planks");
        k.set((hb.x2() + bank.x1()) / 2, top, z1, AIR);
        k.set(hb.x2() - 1, top, z1, obstacle(c, 1));
        // 2: tightrope south over the main road onto house C
        int rx = KandarinKit.X(2713);
        k.set(rx, top + 1, hb.z2(), AIR);
        k.set(rx, top + 1, hc.z1(), AIR);
        k.fill(rx, top, hb.z2() + 1, rx, top, hc.z1() - 1, "minecraft:dark_oak_slab[type=top]");
        k.fill(rx, top - 1, hb.z2() + 1, rx, top - 1, hb.z2() + 1, "minecraft:dark_oak_fence");
        k.set(rx, top, hc.z1() + 1, obstacle(c, 2));
        // 3: jump west onto house D
        int z3 = KandarinKit.Z(3469);
        k.set(hc.x1(), top + 1, z3, AIR);
        k.set(hd.x2(), top + 1, z3, AIR);
        k.fill(hd.x2() + 1, top, z3, hc.x1() - 1, top, z3, "minecraft:oak_planks");
        k.set(hd.x2() + 2, top, z3, AIR);
        k.set(hd.x2() - 1, top, z3, obstacle(c, 3));
        // 4: jump west onto house E
        int z4 = KandarinKit.Z(3470);
        k.set(hd.x1(), top + 1, z4, AIR);
        k.set(he.x2(), top + 1, z4, AIR);
        k.fill(he.x2() + 1, top, z4, hd.x1() - 1, top, z4, "minecraft:oak_planks");
        k.set(he.x2() + 2, top, z4, AIR);
        k.set(he.x2() - 1, top, z4, obstacle(c, 4));
        // 5: jump off the edge onto the hay
        int gx = he.x1() - 4;
        k.set(he.x1(), top + 1, z4, AIR);
        k.fill(gx - 1, -1, z4 - 1, gx + 1, -1, z4 + 1, HAY);
        k.set(gx, -1, z4, obstacle(c, 5));
    }

    static void churchInterior(KandarinKit k, Bld ch) {
        int cx = ch.cx();
        k.fill(cx - 1, 0, ch.z1() + 1, cx + 1, 0, ch.z1() + 2, "minecraft:polished_andesite_slab");
        k.set(cx, 0, ch.z1() + 1, ALTAR);
        k.set(cx - 1, 1, ch.z1() + 1, "minecraft:candle[candles=3,lit=true]");
        k.set(cx + 1, 1, ch.z1() + 1, "minecraft:candle[candles=3,lit=true]");
        k.fill(ch.doorX(), 0, ch.z1() + 3, ch.doorX(), 0, ch.z2() - 1, "minecraft:red_carpet");
        for (int z = ch.z1() + 4; z <= ch.z2() - 2; z += 2) {
            if (cx - 1 > ch.x1() + 1) {
                k.fill(ch.x1() + 1, 0, z, ch.doorX() - 1, 0, z, "minecraft:spruce_stairs[facing=south]");
            }
            k.fill(ch.doorX() + 1, 0, z, ch.x2() - 1, 0, z, "minecraft:spruce_stairs[facing=south]");
        }
        for (int z = ch.z1() + 2; z <= ch.z2() - 2; z += 3) {
            k.set(ch.x1(), 1, z, "minecraft:yellow_stained_glass_pane");
            k.set(ch.x1(), 2, z, "minecraft:blue_stained_glass_pane");
            k.set(ch.x2(), 1, z, "minecraft:yellow_stained_glass_pane");
            k.set(ch.x2(), 2, z, "minecraft:blue_stained_glass_pane");
        }
        k.set(cx, 3, ch.cz(), "minecraft:lantern");
    }

    // ------------------------------------------------------------------ Camelot

    /**
     * Camelot Castle as on the OSRS map (mejrs layers_osrs, planes 0-2; ladders and stairs from shortest-path;
     * the knights from mejrs data_osrs): no curtain wall, but hedged grounds 2743-2774 x 3484-3518 with the gate
     * in the south fence (2757-2758, 3483). The keep 2749-2767 x 3504-3517 (chamfered north corners, three
     * storeys, door south at 2757-2758; the staircase at 2750-2751, 3508 climbs north to the first floor at
     * 3513) holds King Arthur's throne (2764, 3515) and upstairs the Round Table; two wings 2749-2751 /
     * 2765-2767 x 3496-3503 run south to the octagonal towers 2746-2751 / 2765-2770 x 3490-3495 (ladders at
     * 2748, 3493 and 2769, 3493, three storeys), joined on the first floor by the gallery over the courtyard's
     * south side (3492-3495); the courtyard 2752-2764 x 3489-3503 with its fountain (2756-2757, 3496-3497).
     * OSRS has no bank or altar in the castle; the landmark's booths sit in the west wing and the altar in the
     * east wing (approx).
     */
    static StructureDef camelot() {
        KandarinKit k = new KandarinKit("camelot_castle", "Camelot Castle").replaces("CAMELOT");
        k.flatten(2740, 3462, 2778, 3518);
        // the south lawn, paths and flower beds outside the grounds
        k.road(2756, 3466, 2759, 3483, GRAVEL, ANDESITE);
        k.road(2744, 3474, 2771, 3475, GRAVEL, null);
        String hedge = "minecraft:oak_leaves[persistent=true]";
        for (int[] bed : new int[][]{{2747, 3470}, {2766, 3470}, {2747, 3480}, {2766, 3480}}) {
            int x = KandarinKit.X(bed[0]);
            int z = KandarinKit.Z(bed[1]);
            k.fill(x, -1, z, x + 5, -1, z + 3, "minecraft:podzol");
            k.fill(x, 0, z, x + 5, 0, z + 3, "minecraft:red_tulip");
            k.fill(x + 1, 0, z + 1, x + 4, 0, z + 2, "minecraft:white_tulip");
            k.walls(x - 1, 0, z - 1, x + 6, 0, z + 4, "minecraft:spruce_fence");
            k.clear(x + 2, 0, z - 1, x + 3, 0, z - 1);
        }
        k.lamp(KandarinKit.X(2755), KandarinKit.Z(3482), "minecraft:dark_oak_fence");
        k.lamp(KandarinKit.X(2760) + 1, KandarinKit.Z(3482), "minecraft:dark_oak_fence");
        // the grounds: hedges west, north and east, the white fence south with the gate (2757-2758, 3483)
        k.fill(KandarinKit.X(2743), 0, KandarinKit.Z(3518), KandarinKit.X(2743) + 1, 1, KandarinKit.Z(3484) + 1, hedge);
        k.fill(KandarinKit.X(2774), 0, KandarinKit.Z(3518), KandarinKit.X(2774) + 1, 1, KandarinKit.Z(3484) + 1, hedge);
        k.fill(KandarinKit.X(2743), 0, KandarinKit.Z(3518), KandarinKit.X(2774) + 1, 1, KandarinKit.Z(3518) + 1, hedge);
        k.fill(KandarinKit.X(2743), 0, KandarinKit.Z(3483) + 1, KandarinKit.X(2774) + 1, 0, KandarinKit.Z(3483) + 1,
                "minecraft:birch_fence");
        k.clear(KandarinKit.X(2757), 0, KandarinKit.Z(3483) + 1, KandarinKit.X(2758) + 1, 0, KandarinKit.Z(3483) + 1);
        k.set(KandarinKit.X(2757), 0, KandarinKit.Z(3483) + 1, "minecraft:birch_fence_gate[facing=south]");
        k.set(KandarinKit.X(2758) + 1, 0, KandarinKit.Z(3483) + 1, "minecraft:birch_fence_gate[facing=south]");
        // paths in the grounds: up to the courtyard, round the castle, and the fountain's circle
        k.road(2757, 3484, 2758, 3503, GRAVEL, ANDESITE);
        k.road(2745, 3486, 2772, 3488, GRAVEL, null);
        k.road(2752, 3496, 2763, 3498, GRAVEL, null);
        k.floor(2753, 3489, 2756, 3495, "minecraft:coarse_dirt");
        k.floor(2759, 3489, 2763, 3495, "minecraft:coarse_dirt");
        MapTown t = new MapTown(k.b);
        // row 0 = 3517, column 0 = 2746: the keep k, the wings w / e, the towers W / E
        String[] plan = {
                ".....kkkkkkkkkkkkkkk", // 3517
                "....kkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk", // 3515
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk", // 3510
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk",
                "...kkkkkkkkkkkkkkkkkkk", // 3505
                "...kkkkkkkkkkkkkkkkkkk",
                "...www.............eee",
                "...www.............eee",
                "...www.............eee",
                "...www.............eee", // 3500
                "...www.............eee",
                "...www.............eee",
                "...www.............eee",
                "...www.............eee",
                ".WWWWW.............EEEEE", // 3495
                "WWWWWW.............EEEEEE",
                "WWWWWW.............EEEEEE",
                "WWWWWW.............EEEEEE",
                "WWWWWW.............EEEEEE",
                ".WWWW...............EEEE", // 3490
        };
        String[] towers = plan;
        String w = SB;
        String fl = "minecraft:polished_andesite";
        String up = "minecraft:spruce_planks";
        String crenel = "minecraft:stone_brick_wall";
        t.bld(2746, 3517, "wW", towers).wall(w).floor(fl, up).storeys(2).flat(crenel).build();
        t.bld(2746, 3517, "eE", towers).wall(w).floor(fl, up).storeys(2).flat(crenel).build();
        t.bld(2746, 3517, "W", towers).wall(w).floor(fl, up).storeys(3).flat(crenel).ladder(2748, 3493, "east").build();
        t.bld(2746, 3517, "E", towers).wall(w).floor(fl, up).storeys(3).flat(crenel).ladder(2769, 3493, "west").build();
        t.bld(2746, 3517, "k", towers).wall(w).floor(fl, up).storeys(3).flat(crenel).window("minecraft:white_stained_glass_pane")
                .door(2757, 3504, 's', "dark_oak").door(2758, 3504, 's', "dark_oak").build();
        int g = k.g;
        // wings to keep and towers on both floors, the wings' courtyard doors
        for (int y = 0; y <= 4; y += 4) {
            t.gap(2750, 3504, 's', y, 3);
            t.gap(2750, 3503, 'n', y, 3);
            t.gap(2766, 3504, 's', y, 3);
            t.gap(2766, 3503, 'n', y, 3);
            t.gap(2750, 3495, 'n', y, 3);
            t.gap(2750, 3496, 's', y, 3);
            t.gap(2766, 3495, 'n', y, 3);
            t.gap(2766, 3496, 's', y, 3);
        }
        t.door(2751, 3499, 'e', 0, "dark_oak");
        t.door(2765, 3499, 'w', 0, "dark_oak");
        // the gallery over the courtyard's south side, from tower to tower on the first floor
        int gx1 = KandarinKit.X(2752);
        int gx2 = KandarinKit.X(2764) + 1;
        int gz1 = KandarinKit.Z(3495);
        int gz2 = KandarinKit.Z(3492) + 1;
        k.b.fill(gx1, g + 3, gz1, gx2, g + 3, gz2, up);
        k.b.fill(gx1, g + 2, gz1, gx2, g + 2, gz1, w);
        k.b.fill(gx1, g + 2, gz2, gx2, g + 2, gz2, w);
        k.b.fill(gx1, g + 4, gz1, gx2, g + 4, gz1, crenel);
        k.b.fill(gx1, g + 4, gz2, gx2, g + 4, gz2, crenel);
        for (int x = gx1; x <= gx2; x += 5) {
            if (x >= KandarinKit.X(2757) - 1 && x <= KandarinKit.X(2758) + 2) {
                continue;
            }
            k.b.fill(x, g, gz1, x, g + 2, gz1, w);
            k.b.fill(x, g, gz2, x, g + 2, gz2, w);
        }
        t.gap(2751, 3493, 'e', 4, 3);
        t.gap(2751, 3494, 'e', 4, 3);
        t.gap(2765, 3493, 'w', 4, 3);
        t.gap(2765, 3494, 'w', 4, 3);
        // the keep: staircase at 2750-2751, 3508 north to the first floor, a ladder on to the second
        t.stairs(KandarinKit.X(2750), KandarinKit.Z(3508) + 1, "north", 0, 4, 4, "minecraft:stone_brick_stairs", w);
        t.ladder(KandarinKit.X(2749) + 1, KandarinKit.Z(3506), "east", 4, 8, w);
        // King Arthur's throne (2764, 3515) on a dais, banners and the hall's carpet
        k.fill(KandarinKit.X(2762), -1, KandarinKit.Z(3516), KandarinKit.X(2766) + 1, -1, KandarinKit.Z(3514) + 1,
                "minecraft:red_wool");
        k.set(KandarinKit.X(2764), 0, KandarinKit.Z(3516), "minecraft:gold_block");
        k.set(KandarinKit.X(2764), 0, KandarinKit.Z(3516) + 1, "minecraft:quartz_stairs[facing=north,half=bottom]");
        k.fill(KandarinKit.X(2757), -1, KandarinKit.Z(3513), KandarinKit.X(2758) + 1, -1, KandarinKit.Z(3505) + 1,
                "minecraft:red_wool");
        for (int x = KandarinKit.X(2752); x <= KandarinKit.X(2765); x += 4) {
            k.set(x, 2, KandarinKit.Z(3517), "minecraft:red_wall_banner[facing=south]");
            k.set(x, 2, KandarinKit.Z(3509), "minecraft:lantern[hanging=true]");
        }
        // the Round Table upstairs, the knights' beds on the second floor
        int rx = KandarinKit.X(2758);
        int rz = KandarinKit.Z(3511);
        k.fill(rx - 2, 4, rz - 2, rx + 3, 4, rz + 3, "minecraft:spruce_slab[type=top]");
        k.clear(rx, 4, rz, rx + 1, 4, rz + 1);
        k.set(rx, 4, rz, "minecraft:lantern");
        k.set(rx, 6, rz - 5, "minecraft:lantern[hanging=true]");
        for (int i = 0; i < 4; i++) {
            k.set(KandarinKit.X(2753) + i * 4, 8, KandarinKit.Z(3516), "minecraft:blue_bed[facing=north,part=foot]");
        }
        k.set(KandarinKit.X(2766), 8, KandarinKit.Z(3506), "minecraft:chest[facing=west]");
        // the wings: bank booths (west, approx) and an altar (east, approx); the kitchens' range
        k.set(KandarinKit.X(2749) + 1, 0, KandarinKit.Z(3501), BANK);
        k.set(KandarinKit.X(2749) + 1, 0, KandarinKit.Z(3502), BANK);
        k.set(KandarinKit.X(2767), 0, KandarinKit.Z(3501), ALTAR);
        k.set(KandarinKit.X(2767), 0, KandarinKit.Z(3502) + 1, "minecraft:candle[candles=3,lit=true]");
        k.set(KandarinKit.X(2747) + 1, 4, KandarinKit.Z(3491), "minecraft:chest[facing=north]");
        // the fountain
        int fx = KandarinKit.X(2756) + 1;
        int fz = KandarinKit.Z(3497);
        k.walls(fx - 2, 0, fz - 2, fx + 3, 0, fz + 3, "minecraft:stone_brick_slab[type=bottom]");
        k.fill(fx - 1, -1, fz - 1, fx + 2, -1, fz + 2, "minecraft:water");
        k.fill(fx, 0, fz, fx + 1, 1, fz + 1, "minecraft:chiseled_stone_bricks");
        k.set(fx, 2, fz, "minecraft:water");
        // flags on the towers
        k.flag(KandarinKit.X(2748) + 1, 14, KandarinKit.Z(3493), "red", "yellow");
        k.flag(KandarinKit.X(2768) + 1, 14, KandarinKit.Z(3493), "red", "yellow");
        return k.build();
    }

    /** Sprinkle mossy/cracked bricks over the outer faces of a wall box. */
    static void weather(KandarinKit k, int x1, int z1, int x2, int z2, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                int h = KandarinKit.hash(x * 3 + y, z1);
                if (h % 9 == 0) {
                    k.set(x, y, z1, h % 2 == 0 ? MOSSY_SB : CRACKED_SB);
                }
                h = KandarinKit.hash(x * 5 + y, z2);
                if (h % 9 == 0) {
                    k.set(x, y, z2, h % 2 == 0 ? MOSSY_SB : CRACKED_SB);
                }
            }
            for (int z = z1; z <= z2; z++) {
                int h = KandarinKit.hash(x1, z * 3 + y);
                if (h % 9 == 0) {
                    k.set(x1, y, z, h % 2 == 0 ? MOSSY_SB : CRACKED_SB);
                }
                h = KandarinKit.hash(x2, z * 5 + y);
                if (h % 9 == 0) {
                    k.set(x2, y, z, h % 2 == 0 ? MOSSY_SB : CRACKED_SB);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Catherby

    static StructureDef catherby() {
        KandarinKit k = new KandarinKit("catherby", "Catherby").replaces("CATHERBY");
        k.flatten(2788, 3404, 2842, 3470);
        k.floor(2788, 3404, 2842, 3434, SAND);
        k.water(2788, 3404, 2842, 3418, 3);
        k.road(2788, 3435, 2842, 3436, PATH, GRAVEL);
        k.road(2809, 3437, 2810, 3455, PATH, GRAVEL);
        k.road(2826, 3437, 2827, 3462, PATH, GRAVEL);

        Bld candle = k.house(2796, 3438, 2801, 3443, 1, CATHERBY, 's', NO_LADDER);
        k.counter(candle, GENERAL, "spruce");
        k.set(candle.x1() + 1, 1, candle.z2() - 1, "minecraft:candle[candles=4,lit=true]");

        // bank on its OSRS footprint 2806-2812 x 3438-3445 (mejrs map), the booths on their cache tiles
        // (2807, 2809, 2811 at 3442) in a counter across the middle, chequered floor in front
        Bld bank = k.house(2806, 3438, 2812, 3445, 1, STONE.roof("minecraft:spruce"), 's', NO_LADDER | WIDE_DOOR);
        int bz = KandarinKit.Z(3442) + 1;
        for (int x = bank.x1() + 1; x < bank.x2(); x++) {
            for (int z = bz + 1; z < bank.z2(); z++) {
                k.set(x, -1, z, (((x >> 1) + (z >> 1)) & 1) == 0 ? "minecraft:polished_andesite" : "minecraft:gray_concrete");
            }
        }
        k.fill(bank.x1() + 1, 0, bz, bank.x2() - 1, 0, bz, "minecraft:spruce_slab[type=top]");
        for (int tx : new int[]{2807, 2809, 2811}) {
            k.set(KandarinKit.X(tx), 0, bz, BANK).set(KandarinKit.X(tx) + 1, 0, bz, BANK);
        }
        k.set(bank.cx(), 2, bank.cz() + 1, "minecraft:lantern[hanging=true]");

        // Hickton's Archery Emporium (2820-2825 x 3440-3445, counter at 2822, 3443-3444)
        Bld hick = k.house(2820, 3440, 2825, 3445, 1, CATHERBY, 's', NO_LADDER);
        k.counter(hick, ARCHERY, "spruce");
        k.set(hick.x1() + 1, 0, hick.z2() - 1, "minecraft:fletching_table");
        k.set(hick.x2() - 1, 0, hick.z2() - 1, "minecraft:target");

        Bld harry = k.house(2830, 3438, 2835, 3443, 1, CATHERBY, 's', NO_LADDER);
        k.counter(harry, TOOLS, "spruce");
        k.set(harry.x1() + 1, 0, harry.z2() - 1, "minecraft:barrel[facing=up]");

        // Caleb's house between the bank and Hickton's (2815-2818 x 3439-3444): the cooking range on its tiles
        // (2817-2818, 3444) against the north wall, his table (2817, 3441)
        Bld caleb = k.house(2815, 3439, 2818, 3444, 1, CATHERBY, 's', NO_LADDER);
        k.set(KandarinKit.X(2817), 0, KandarinKit.Z(3444) + 1, RANGE).set(KandarinKit.X(2818), 0, KandarinKit.Z(3444) + 1, RANGE);
        k.table(KandarinKit.X(2817) + 1, 0, KandarinKit.Z(3441), "spruce");
        k.set(caleb.x1() + 1, 0, caleb.z1() + 1, "minecraft:barrel[facing=up]");
        k.house(2814, 3447, 2818, 3452, 1, CATHERBY, 's', FURN);

        Bld van = k.house(2820, 3452, 2825, 3457, 1, CATHERBY, 'w', NO_LADDER);
        k.counter(van, GENERAL, "spruce");
        k.set(van.x2() - 1, 0, van.z2() - 1, "minecraft:composter");

        Bld arh = k.house(2800, 3426, 2805, 3431, 1, CATHERBY.roof("minecraft:spruce"), 'n', NO_LADDER);
        k.counter(arh, GENERAL, "spruce");

        k.house(2789, 3438, 2794, 3443, 2, CATHERBY, 's', FURN);
        k.house(2836, 3438, 2840, 3444, 1, CATHERBY, 's', FURN);
        k.house(2795, 3448, 2800, 3453, 2, CATHERBY, 's', FURN);
        k.house(2829, 3448, 2834, 3453, 1, CATHERBY, 'w', FURN);
        k.house(2790, 3456, 2796, 3462, 2, CATHERBY, 'e', FURN);
        k.house(2829, 3458, 2835, 3464, 2, CATHERBY, 'w', FURN);
        k.house(2817, 3461, 2823, 3466, 1, CATHERBY, 's', FURN);

        // Farming patches (two allotments, flower and herb)
        k.fence(2802, 3455, 2815, 3468, "minecraft:oak_fence");
        k.tClear(2809, 3455, 2810, 3455, 0, 0);
        k.farm(2804, 3465, 2813, 3466, "minecraft:carrots[age=7]");
        k.farm(2804, 3457, 2813, 3458, "minecraft:potatoes[age=7]");
        k.fill(KandarinKit.X(2804), 0, KandarinKit.Z(3466), KandarinKit.X(2804) + 3, 0, KandarinKit.Z(3466), "runecraft:allotment_patch");
        k.fill(KandarinKit.X(2804), 0, KandarinKit.Z(3458), KandarinKit.X(2804) + 3, 0, KandarinKit.Z(3458), "runecraft:allotment_patch");
        k.floor(2806, 3461, 2807, 3462, "minecraft:podzol");
        k.tFill(2806, 3461, 2807, 3462, 0, 0, "minecraft:poppy");
        k.tFill(2809, 3461, 2809, 3461, 0, 0, "runecraft:flower_patch");
        k.tFill(2812, 3461, 2812, 3461, 0, 0, "runecraft:herb_patch");
        k.at(2804, 3461, 0, "runecraft:compost_bin");

        // Beach: upturned boats, nets, lobster pots
        for (int i = 0; i < 3; i++) {
            int x = KandarinKit.X(2814 + i * 7);
            int z = KandarinKit.Z(3428);
            k.fill(x, 0, z, x + 4, 0, z, "minecraft:spruce_stairs[facing=north,half=top]");
            k.fill(x, 0, z + 1, x + 4, 0, z + 1, "minecraft:spruce_stairs[facing=south,half=top]");
            k.set(x + 6, 0, z + 2, "minecraft:barrel[facing=up]");
            k.set(x + 6, 0, z - 2, "minecraft:cobweb");
        }
        k.lamp(KandarinKit.X(2808), KandarinKit.Z(3434), "minecraft:spruce_fence");
        k.lamp(KandarinKit.X(2822), KandarinKit.Z(3434), "minecraft:spruce_fence");
        for (int x = 2790; x <= 2840; x += 9) {
            k.oak(KandarinKit.X(x), KandarinKit.Z(3468), 5);
        }
        k.meadow(KandarinKit.X(2788), KandarinKit.Z(3470), KandarinKit.X(2842) + 1, KandarinKit.Z(3445), 10);
        return k.build();
    }

    static StructureDef catherbyPier() {
        KandarinKit k = new KandarinKit("catherby_pier", "Catherby docks").replaces("catherby_docks");
        k.pier(2801, 3409, 2803, 3434, "spruce", true);
        k.pier(2796, 3409, 2808, 3410, "spruce", true);
        k.tClear(2801, 3411, 2803, 3411, 0, 0);
        k.set(KandarinKit.X(2803) + 1, 0, KandarinKit.Z(3426), "runecraft:catherby_task_board");
        k.set(KandarinKit.X(2803) + 1, 0, KandarinKit.Z(3428), "minecraft:barrel[facing=up]");
        k.set(KandarinKit.X(2801), 0, KandarinKit.Z(3428), "minecraft:barrel[facing=up]");
        k.longboat(KandarinKit.X(2796), KandarinKit.Z(3413), 17, true, "blue");
        return k.build();
    }

    // ------------------------------------------------------------------ Keep Le Faye

    static StructureDef keepLeFaye() {
        KandarinKit k = new KandarinKit("keep_le_faye", "Keep Le Faye").replaces("keep_le_faye");
        String dark = "minecraft:deepslate_bricks";
        k.flatten(2756, 3388, 2782, 3414, "minecraft:podzol");
        k.cwallRect(2758, 3390, 2780, 3412, 9, dark);
        k.floor(2759, 3391, 2779, 3411, "minecraft:coarse_dirt");
        k.gate(2769, 3390, 2770, 3390, 5, "minecraft:cobbled_deepslate");
        int[][] t = {{2758, 3390}, {2780, 3390}, {2758, 3412}, {2780, 3412}};
        for (int[] c : t) {
            k.tower(c[0], c[1], 4, 15, "minecraft:cobbled_deepslate", "minecraft:dark_oak_planks", "minecraft:deepslate_tiles", ' ');
        }
        k.towerDiagDoor(KandarinKit.X(2758) + 1, KandarinKit.Z(3390) + 1, 4, 1, -1);
        k.towerDiagDoor(KandarinKit.X(2780) + 1, KandarinKit.Z(3390) + 1, 4, -1, -1);
        k.towerDiagDoor(KandarinKit.X(2758) + 1, KandarinKit.Z(3412) + 1, 4, 1, 1);
        k.towerDiagDoor(KandarinKit.X(2780) + 1, KandarinKit.Z(3412) + 1, 4, -1, 1);
        Pal kp = TROLL.wall(dark).roof("flat:minecraft:polished_deepslate");
        Bld keep = k.house(2761, 3402, 2775, 3410, 3, kp, 's', FLAT | WIDE_DOOR | FURN);
        k.set(keep.cx(), 0, keep.z1() + 1, "minecraft:enchanting_table");
        k.set(keep.cx() - 2, 0, keep.z1() + 1, "minecraft:brewing_stand");
        k.set(keep.cx() + 2, 0, keep.z1() + 1, "minecraft:cauldron");
        k.tower(2775, 3410, 3, 20, "minecraft:cobbled_deepslate", "minecraft:dark_oak_planks", "minecraft:deepslate_tiles", 'w');
        // ruin: broken merlons, cobwebs
        for (int x = 2760; x <= 2778; x += 5) {
            k.tClear(x, 3390, x + 1, 3390, 8, 9);
        }
        k.tClear(2780, 3396, 2780, 3400, 4, 9);
        k.tFill(2781, 3395, 2782, 3401, 0, 1, "minecraft:cobbled_deepslate");
        for (int i = 0; i < 8; i++) {
            k.set(keep.x1() + 1 + i * 3, 2, keep.z1() + 1 + (i % 3) * 3, "minecraft:cobweb");
        }
        k.at(2764, 3394, 0, "minecraft:skeleton_skull");
        k.at(2775, 3396, 0, "minecraft:cobweb");
        return k.build();
    }

    // ------------------------------------------------------------------ Sorcerer's Tower

    static StructureDef sorcerersTower() {
        KandarinKit k = new KandarinKit("sorcerers_tower", "Sorcerer's Tower").replaces("sorcerers_tower");
        // OSRS map: the tower is 9 tiles across (2698-2706 x 3401-3409, an octagon there // approx: round here) with
        // its door south at 2702, in a hedged garden 2694-2710 x 3396-3412 with flower beds by the gate
        k.flatten(2690, 3392, 2714, 3416);
        int cx = KandarinKit.X(2702) + 1;
        int cz = KandarinKit.Z(3405) + 1;
        k.disc(cx, cz, -1, -1, 13, GRAVEL);
        k.towerB(cx, cz, 9, 19, SB, "minecraft:dark_oak_planks", "minecraft:purple_terracotta", 's', 5);
        // ground: library; first: alchemy; second: observatory
        k.ring(cx, cz, 0, 1, 6, "minecraft:bookshelf");
        k.clear(cx - 1, 0, cz + 6, cx + 1, 1, cz + 6);
        k.clear(cx + 6, 0, cz - 1, cx + 6, 1, cz + 1);
        k.clear(cx - 6, 0, cz - 1, cx - 6, 1, cz + 1);
        k.set(cx, 0, cz, "minecraft:lectern[facing=south]");
        k.set(cx, 4, cz, "minecraft:enchanting_table");
        k.set(cx - 3, 4, cz - 3, "minecraft:brewing_stand");
        k.set(cx + 3, 4, cz - 3, "minecraft:cauldron");
        k.set(cx - 3, 4, cz + 3, "minecraft:chest[facing=north]");
        k.set(cx, 9, cz, "minecraft:amethyst_cluster");
        k.set(cx - 3, 9, cz, "minecraft:bookshelf");
        k.set(cx + 3, 9, cz, "minecraft:crafting_table");
        k.set(cx, 14, cz, "minecraft:respawn_anchor");
        k.set(cx, 8, cz, "minecraft:lantern[hanging=true]");
        k.set(cx, 3, cz, "minecraft:lantern[hanging=true]");
        // the hedge round the garden, open at the south gate, and the flower beds either side of the path
        int gx1 = KandarinKit.X(2694);
        int gx2 = KandarinKit.X(2710) + 1;
        int gz1 = KandarinKit.Z(3412);
        int gz2 = KandarinKit.Z(3396) + 1;
        k.walls(gx1, 0, gz1, gx2, 1, gz2, "minecraft:oak_leaves[persistent=true]");
        k.clear(cx - 2, 0, gz2, cx + 2, 1, gz2);
        k.fill(cx - 1, -1, cz + 10, cx + 1, -1, gz2, GRAVEL);
        for (int s = -1; s <= 1; s += 2) {
            k.fill(cx + s * 5, 0, gz2 - 4, cx + s * 4, 0, gz2 - 2, "minecraft:allium");
            k.fill(cx + s * 7, 0, gz2 - 4, cx + s * 6, 0, gz2 - 2, "minecraft:oxeye_daisy");
        }
        k.oak(gx1 + 3, gz1 + 3, 6);
        k.oak(gx2 - 3, gz1 + 3, 5);
        return k.build();
    }

    // ------------------------------------------------------------------ Legends' Guild

    static StructureDef legendsGuild() {
        KandarinKit k = new KandarinKit("legends_guild", "Legends' Guild").replaces("legends_guild");
        k.flatten(2713, 3343, 2747, 3392);
        k.fence(2716, 3346, 2744, 3390, "minecraft:dark_oak_fence");
        k.tClear(2729, 3346, 2730, 3346, 0, 0);
        k.tFill(2728, 3346, 2728, 3346, 0, 2, "minecraft:chiseled_stone_bricks");
        k.tFill(2731, 3346, 2731, 3346, 0, 2, "minecraft:chiseled_stone_bricks");
        k.road(2729, 3344, 2730, 3374, GRAVEL, ANDESITE);
        k.road(2722, 3360, 2737, 3361, GRAVEL, null);
        // Totem pole by the gate
        int tx = KandarinKit.X(2726);
        int tz = KandarinKit.Z(3349);
        String[] totem = {"minecraft:stripped_jungle_log", "minecraft:red_terracotta", "minecraft:carved_pumpkin[facing=south]",
                "minecraft:yellow_terracotta", "minecraft:stripped_jungle_log", "minecraft:blue_terracotta", "minecraft:carved_pumpkin[facing=south]"};
        for (int i = 0; i < totem.length; i++) {
            k.set(tx, i, tz, totem[i]);
        }
        k.set(tx - 1, 5, tz, "minecraft:jungle_trapdoor[facing=west,open=true]");
        k.set(tx + 1, 5, tz, "minecraft:jungle_trapdoor[facing=east,open=true]");
        // Mansion
        Pal lp = new Pal(SB, "minecraft:quartz_pillar", "minecraft:dark_oak_planks", "minecraft:deepslate_tile", "dark_oak", PANE);
        Bld m = k.house(2721, 3375, 2738, 3386, 2, lp, 's', FURN | WIDE_DOOR);
        k.longTable(m.x1() + 4, m.x2() - 4, 0, m.cz(), "dark_oak");
        k.fill(m.doorX(), 0, m.cz() + 2, m.doorX() + 1, 0, m.z2() - 1, "minecraft:blue_carpet");
        k.set(m.x1() + 2, 0, m.z1() + 1, "minecraft:dragon_head");
        k.set(m.x2() - 2, 0, m.z1() + 1, "minecraft:wither_skeleton_skull");
        k.fill(m.x1() + 1, 4, m.z1() + 2, m.x1() + 1, 5, m.z2() - 2, "minecraft:bookshelf");
        k.tower(2721, 3375, 3, 14, SB, "minecraft:dark_oak_planks", "minecraft:deepslate_tiles", 'n');
        k.tower(2738, 3375, 3, 14, SB, "minecraft:dark_oak_planks", "minecraft:deepslate_tiles", 'n');
        // Gardens
        int fx = KandarinKit.X(2729) + 1;
        int fz = KandarinKit.Z(3366) + 1;
        k.disc(fx, fz, -1, 0, 3, "minecraft:stone_bricks");
        k.disc(fx, fz, 0, 0, 2, WATER);
        k.fill(fx, 0, fz, fx, 2, fz, "minecraft:quartz_pillar");
        k.set(fx, 3, fz, "minecraft:sea_lantern");
        String hedge = "minecraft:spruce_leaves[persistent=true]";
        k.tFill(2718, 3352, 2726, 3352, 0, 1, hedge);
        k.tFill(2733, 3352, 2742, 3352, 0, 1, hedge);
        for (int y = 3354; y <= 3370; y += 8) {
            k.oak(KandarinKit.X(2720), KandarinKit.Z(y), 6);
            k.oak(KandarinKit.X(2740), KandarinKit.Z(y), 6);
        }
        k.meadow(KandarinKit.X(2717), KandarinKit.Z(3389), KandarinKit.X(2743), KandarinKit.Z(3347), 9);
        return k.build();
    }

    // ------------------------------------------------------------------ Ranging Guild & Hemenster

    static StructureDef rangingGuild() {
        KandarinKit k = new KandarinKit("ranging_guild", "Ranging Guild");
        k.flatten(2628, 3412, 2686, 3454);
        int cx = KandarinKit.X(2665) + 1;
        int cz = KandarinKit.Z(3433) + 1;
        k.ring(cx, cz, 0, 2, 28, "minecraft:spruce_fence");
        k.ring(cx, cz, -1, -1, 28, "minecraft:cobblestone");
        k.clear(cx - 28, 0, cz - 1, cx - 26, 2, cz + 1);
        k.fill(cx - 30, -1, cz - 1, cx - 7, -1, cz + 1, PATH);
        k.disc(cx, cz, -1, -1, 9, GRAVEL);
        k.towerB(cx, cz, 6, 15, SB, "minecraft:spruce_planks", "minecraft:spruce_planks", 'w', 5);
        k.set(cx + 3, 0, cz - 2, ARCHERY);
        k.set(cx + 3, 0, cz + 2, WEAPONS);
        k.fill(cx + 3, 0, cz - 1, cx + 3, 0, cz + 1, "minecraft:spruce_slab[type=top]");
        k.set(cx - 2, 0, cz - 3, "minecraft:fletching_table");
        k.set(cx + 2, 4, cz - 2, "minecraft:fletching_table");
        k.set(cx - 2, 4, cz + 2, "minecraft:barrel[facing=up]");
        k.set(cx, 3, cz, "minecraft:lantern[hanging=true]");
        k.set(cx, 8, cz, "minecraft:lantern[hanging=true]");
        // Target range (north-east) with the shooting line
        for (int i = 0; i < 4; i++) {
            int x = cx + 20;
            int z = cz - 18 + i * 5;
            k.set(x, 0, z, HAY);
            k.set(x, 1, z, "minecraft:target");
            k.set(x, 0, z + 1, "minecraft:spruce_fence");
            k.set(x, 0, z - 1, "minecraft:spruce_fence");
        }
        k.fill(cx + 8, 0, cz - 20, cx + 8, 0, cz - 1, "minecraft:spruce_fence");
        k.clear(cx + 8, 0, cz - 12, cx + 8, 0, cz - 10);
        k.fill(cx + 9, -1, cz - 20, cx + 19, -1, cz - 1, "minecraft:coarse_dirt");
        // Judge's hut
        k.houseB(cx + 3, cz - 22, cx + 7, cz - 18, 1, FREM.roof("minecraft:spruce"), 'e', NO_LADDER);
        // Guard platforms on the fence
        k.towerB(cx, cz - 28, 2, 7, "minecraft:spruce_planks", "minecraft:spruce_planks", null, 's', 6);
        k.towerB(cx, cz + 28, 2, 7, "minecraft:spruce_planks", "minecraft:spruce_planks", null, 'n', 6);
        for (int a = 0; a < 12; a++) {
            double ang = a * Math.PI / 6 + 0.3;
            int x = cx + (int) Math.round(22 * Math.cos(ang));
            int z = cz + (int) Math.round(22 * Math.sin(ang));
            if (x < cx + 6 || z > cz) {
                k.oak(x, z, 5);
            }
        }
        k.meadow(cx - 27, cz - 27, cx + 27, cz + 27, 12);
        return k.build();
    }

    static StructureDef hemenster() {
        KandarinKit k = new KandarinKit("hemenster", "Hemenster").replaces("hemenster");
        k.road(2629, 3439, 2651, 3440, PATH, "minecraft:coarse_dirt");
        k.road(2634, 3429, 2635, 3438, PATH, null);
        k.house(2630, 3442, 2635, 3447, 1, SEERS, 's', FURN);
        k.house(2637, 3443, 2642, 3448, 2, SEERS, 's', FURN);
        Bld bait = k.house(2645, 3442, 2649, 3447, 1, SEERS, 's', NO_LADDER);
        k.counter(bait, TOOLS, "oak");
        // Fishing contest pond
        k.water(2630, 3418, 2640, 3428, 3);
        k.fence(2629, 3417, 2641, 3429, "minecraft:oak_fence");
        k.tClear(2634, 3429, 2635, 3429, 0, 0);
        for (int x = 2631; x <= 2639; x += 2) {
            k.at(x, 3429, -1, "minecraft:oak_planks");
        }
        k.pier(2634, 3422, 2635, 3428, "oak", false);
        k.set(KandarinKit.X(2642), 0, KandarinKit.Z(3431), "minecraft:oak_sign[rotation=8]");
        k.oak(KandarinKit.X(2644), KandarinKit.Z(3423), 6);
        k.oak(KandarinKit.X(2632), KandarinKit.Z(3451), 5);
        return k.build();
    }

    // ------------------------------------------------------------------ Fishing Guild

    static StructureDef fishingGuild() {
        KandarinKit k = new KandarinKit("fishing_guild", "Fishing Guild").replaces("fishing_guild_big_net");
        k.flatten(2584, 3388, 2622, 3434);
        k.water(2584, 3417, 2622, 3434, 3);
        k.floor(2584, 3413, 2622, 3416, SAND);
        String w = SB;
        k.cwall(2586, 3390, 2620, 3390, 4, w);
        k.cwall(2586, 3390, 2586, 3415, 4, w);
        k.cwall(2620, 3390, 2620, 3415, 4, w);
        k.gate(2610, 3390, 2611, 3390, 3, GRAVEL);
        k.road(2610, 3391, 2611, 3416, GRAVEL, COBBLE);
        k.road(2600, 3411, 2609, 3412, GRAVEL, null);
        k.road(2600, 3411, 2602, 3416, GRAVEL, null);

        Bld hall = k.house(2590, 3398, 2602, 3410, 2, STONE.roof("minecraft:spruce"), 'e', WIDE_DOOR);
        k.fill(hall.x1() + 2, 0, hall.z1() + 1, hall.x1() + 2, 0, hall.z2() - 1, "minecraft:spruce_slab[type=top]");
        for (int z = hall.z1() + 2; z <= hall.z2() - 2; z += 3) {
            k.set(hall.x1() + 2, 0, z, BANK);
        }
        k.set(hall.cx(), 0, hall.z1() + 1, RANGE);
        k.set(hall.cx() + 2, 0, hall.z1() + 1, RANGE);
        k.fill(hall.cx() - 2, 0, hall.z1() + 1, hall.cx() - 1, 0, hall.z1() + 1, "minecraft:barrel[facing=south]");
        k.longTable(hall.cx() - 1, hall.x2() - 3, 0, hall.cz() + 2, "spruce");
        k.set(hall.cx(), 2, hall.cz(), "minecraft:lantern[hanging=true]");
        for (int x = hall.x1() + 2; x <= hall.x2() - 3; x += 3) {
            k.bed(x, 4, hall.z1() + 1, "cyan");
        }
        k.set(hall.cx(), 6, hall.cz(), "minecraft:lantern[hanging=true]");

        Bld shop = k.house(2606, 3398, 2612, 3404, 1, STONE.roof("minecraft:spruce"), 'w', NO_LADDER);
        k.counter(shop, TOOLS, "spruce");
        k.house(2614, 3394, 2618, 3399, 1, STONE.roof("minecraft:spruce"), 'n', FURN);

        k.pier(2600, 3417, 2602, 3431, "spruce", true);
        k.pier(2594, 3426, 2616, 3427, "spruce", true);
        k.pier(2614, 3417, 2615, 3425, "spruce", true);
        k.tClear(2600, 3426, 2602, 3427, 0, 0);
        k.tClear(2614, 3426, 2615, 3427, 0, 0);
        k.tClear(2600, 3417, 2602, 3417, 0, 0);
        k.tClear(2614, 3417, 2615, 3417, 0, 0);
        k.set(KandarinKit.X(2605), -2, KandarinKit.Z(3428), "runecraft:big_net_fishing_spot");
        k.set(KandarinKit.X(2611), -2, KandarinKit.Z(3428), "runecraft:big_net_fishing_spot");
        for (int x = 2596; x <= 2612; x += 4) {
            k.set(KandarinKit.X(x), 0, KandarinKit.Z(3427) + 1, "minecraft:barrel[facing=up]");
        }
        k.oak(KandarinKit.X(2589), KandarinKit.Z(3393), 5);
        k.oak(KandarinKit.X(2617), KandarinKit.Z(3405), 5);
        return k.build();
    }
}
