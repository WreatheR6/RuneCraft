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
        Pal bankPal = new Pal(SB, "minecraft:chiseled_stone_bricks", ANDESITE, "flat:minecraft:smooth_stone", "spruce", PANE);
        Bld bank = k.house(2721, 3487, 2730, 3494, 1, bankPal, 's', FLAT | NO_LADDER | WIDE_DOOR);
        int bzc = bank.z1() + 3;
        k.fill(bank.x1() + 2, 0, bzc, bank.x2() - 2, 0, bzc, "minecraft:spruce_slab[type=top]");
        for (int x = bank.x1() + 3; x <= bank.x2() - 3; x += 3) {
            k.set(x, 0, bzc, BANK);
        }
        k.fill(bank.x1() + 1, 0, bank.z1() + 1, bank.x2() - 1, 0, bank.z1() + 1, "minecraft:barrel[facing=south]");
        k.fill(bank.doorX(), 0, bzc + 1, bank.doorX() + 1, 0, bank.z2() - 1, "minecraft:red_carpet");
        k.set(bank.x1() + 3, 3, bank.cz(), "minecraft:lantern[hanging=true]");
        k.set(bank.x2() - 3, 3, bank.cz(), "minecraft:lantern[hanging=true]");
        k.lamp(bank.doorX() - 2, bank.z2() + 2, "minecraft:spruce_fence");
        k.lamp(bank.doorX() + 3, bank.z2() + 2, "minecraft:spruce_fence");

        // --- Rooftop course houses (flat roofs) ---
        Pal flat = SEERS.roof("flat:minecraft:spruce_planks");
        Bld hb = k.house(2711, 3488, 2718, 3495, 1, flat, 's', FURN | FLAT | NO_LADDER);
        Bld hc = k.house(2709, 3466, 2715, 3473, 1, flat, 'w', FURN | FLAT | NO_LADDER);
        Bld hd = k.house(2700, 3466, 2706, 3472, 1, flat, 's', FURN | FLAT | NO_LADDER);
        Bld he = k.house(2691, 3467, 2697, 3473, 1, flat, 'n', FURN | FLAT | NO_LADDER);
        seersCourse(k, bank, hb, hc, hd, he);

        // --- Spinning wheel house (wheel upstairs) ---
        Bld spin = k.house(2700, 3486, 2706, 3492, 2, SEERS, 's', FURN);
        k.set(spin.x1() + 2, 4, spin.z2() - 2, WHEEL);
        k.set(spin.x1() + 1, 4, spin.z2() - 1, "minecraft:white_wool");
        k.set(spin.x1() + 1, 0, spin.z2() - 1, "minecraft:loom[facing=east]");

        // --- Forester's Arms ---
        Bld pub = k.house(2689, 3488, 2697, 3497, 2, SEERS.roof("minecraft:spruce"), 's', 0);
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

        // --- Courthouse ---
        Bld ct = k.house(2729, 3458, 2738, 3468, 2, STONE.roof("minecraft:spruce"), 'w', WIDE_DOOR);
        k.fill(ct.x2() - 3, 0, ct.z1() + 2, ct.x2() - 1, 0, ct.z2() - 2, "minecraft:spruce_planks");
        k.fill(ct.x2() - 3, 1, ct.z1() + 2, ct.x2() - 3, 1, ct.z2() - 2, "minecraft:spruce_slab[type=bottom]");
        k.set(ct.x2() - 2, 1, ct.cz(), "minecraft:spruce_stairs[facing=east]");
        k.set(ct.x2() - 1, 1, ct.cz(), "minecraft:red_wool");
        for (int x = ct.x1() + 2; x <= ct.x2() - 6; x += 2) {
            k.fill(x, 0, ct.z1() + 2, x, 0, ct.cz() - 1, "minecraft:spruce_stairs[facing=west]");
            k.fill(x, 0, ct.cz() + 2, x, 0, ct.z2() - 2, "minecraft:spruce_stairs[facing=west]");
        }
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

    static StructureDef camelot() {
        KandarinKit k = new KandarinKit("camelot_castle", "Camelot Castle").replaces("CAMELOT");
        String w = SB;
        // Lawn, paths, hedges
        k.road(2756, 3466, 2759, 3484, GRAVEL, ANDESITE);
        k.road(2744, 3474, 2771, 3475, GRAVEL, null);
        int hx1 = KandarinKit.X(2744);
        int hx2 = KandarinKit.X(2772) + 1;
        String hedge = "minecraft:oak_leaves[persistent=true]";
        k.fill(hx1, 0, KandarinKit.Z(3467), KandarinKit.X(2755) + 1, 1, KandarinKit.Z(3467), hedge);
        k.fill(KandarinKit.X(2760), 0, KandarinKit.Z(3467), hx2, 1, KandarinKit.Z(3467), hedge);
        for (int[] bed : new int[][]{{2747, 3470}, {2766, 3470}, {2747, 3480}, {2766, 3480}}) {
            int x = KandarinKit.X(bed[0]);
            int z = KandarinKit.Z(bed[1]);
            k.fill(x, -1, z, x + 5, -1, z + 3, "minecraft:podzol");
            k.fill(x, 0, z, x + 5, 0, z + 3, "minecraft:red_tulip");
            k.fill(x + 1, 0, z + 1, x + 4, 0, z + 2, "minecraft:white_tulip");
            k.walls(x - 1, 0, z - 1, x + 6, 0, z + 4, "minecraft:spruce_fence");
            k.clear(x + 2, 0, z - 1, x + 3, 0, z - 1);
        }
        k.lamp(KandarinKit.X(2755), KandarinKit.Z(3483), "minecraft:dark_oak_fence");
        k.lamp(KandarinKit.X(2760) + 1, KandarinKit.Z(3483), "minecraft:dark_oak_fence");

        // Curtain walls with a weathered face
        k.cwallRect(2744, 3485, 2772, 3519, 10, w);
        weather(k, KandarinKit.X(2744), KandarinKit.Z(3519), KandarinKit.X(2772) + 1, KandarinKit.Z(3485) + 1, 0, 9);
        // Courtyard
        k.floor(2745, 3486, 2771, 3518, GRASS);
        k.road(2757, 3486, 2758, 3499, GRAVEL, ANDESITE);
        k.road(2746, 3492, 2771, 3493, GRAVEL, null);
        // Gate with portcullis
        k.gate(2757, 3485, 2758, 3485, 5, GRAVEL);
        int gz = KandarinKit.Z(3485);
        k.fill(KandarinKit.X(2757), 4, gz, KandarinKit.X(2758) + 1, 4, gz, "minecraft:iron_bars");
        k.tower(2754, 3485, 3, 13, w, "minecraft:spruce_planks", null, 'n');
        k.tower(2761, 3485, 3, 13, w, "minecraft:spruce_planks", null, 'n');
        // Corner and mid towers
        int[][] corners = {{2744, 3485, 1, -1}, {2772, 3485, -1, -1}, {2744, 3519, 1, 1}, {2772, 3519, -1, 1}};
        for (int[] c : corners) {
            k.tower(c[0], c[1], 5, 16, w, "minecraft:spruce_planks", null, ' ');
            k.towerDiagDoor(KandarinKit.X(c[0]) + 1, KandarinKit.Z(c[1]) + 1, 5, c[2], c[3]);
            k.flag(KandarinKit.X(c[0]) + 1, 16, KandarinKit.Z(c[1]) + 1, "red", "yellow");
        }
        k.tower(2744, 3502, 4, 13, w, "minecraft:spruce_planks", null, 'e');
        k.tower(2772, 3502, 4, 13, w, "minecraft:spruce_planks", null, 'w');

        // Keep / great hall
        Pal keepPal = new Pal(w, ANDESITE, "minecraft:spruce_planks", "flat:minecraft:stone_bricks", "dark_oak", PANE);
        Bld keep = k.house(2750, 3500, 2766, 3514, 3, keepPal, 's', FLAT | NO_LADDER | WIDE_DOOR);
        for (int x = keep.x1() + 2; x <= keep.x2() - 2; x += 4) {
            k.set(x, 2, keep.z2(), PANE);
            k.set(x, 2, keep.z1(), PANE);
        }
        int cx = keep.cx();
        int cz = keep.cz() + 2;
        // Round table
        k.disc(cx, cz, 0, 0, 4, "minecraft:dark_oak_slab[type=top]");
        k.disc(cx, cz, 0, 0, 1, "minecraft:dark_oak_planks");
        k.set(cx, 1, cz, "minecraft:candle[candles=4,lit=true]");
        for (int a = 0; a < 16; a++) {
            double ang = a * Math.PI / 8;
            int dx = (int) Math.round(5.6 * Math.cos(ang));
            int dz = (int) Math.round(5.6 * Math.sin(ang));
            String f = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
            k.set(cx + dx, 0, cz + dz, "minecraft:dark_oak_stairs[facing=" + f + "]");
        }
        // Throne dais
        k.fill(cx - 3, 0, keep.z1() + 1, cx + 3, 0, keep.z1() + 3, "minecraft:polished_andesite");
        k.fill(cx - 3, 0, keep.z1() + 4, cx + 3, 0, keep.z1() + 4, "minecraft:polished_andesite_slab");
        k.set(cx, 1, keep.z1() + 2, "minecraft:dark_oak_stairs[facing=north]");
        k.set(cx, 2, keep.z1() + 1, "minecraft:gold_block");
        k.set(cx - 1, 1, keep.z1() + 2, "minecraft:dark_oak_slab");
        k.set(cx + 1, 1, keep.z1() + 2, "minecraft:dark_oak_slab");
        k.fill(cx, 0, cz + 6, cx + 1, 0, keep.z2() - 1, "minecraft:red_carpet");
        k.fill(cx - 1, 1, keep.z1() + 5, cx + 1, 1, keep.z1() + 5, "minecraft:red_carpet");
        // Tapestries and lights
        for (int z = keep.z1() + 3; z <= keep.z2() - 3; z += 5) {
            k.fill(keep.x1() + 1, 1, z, keep.x1() + 1, 2, z, "minecraft:red_wool");
            k.fill(keep.x2() - 1, 1, z, keep.x2() - 1, 2, z, "minecraft:blue_wool");
        }
        for (int x = keep.x1() + 4; x <= keep.x2() - 4; x += 6) {
            k.set(x, 2, keep.z1() + 6, "minecraft:lantern[hanging=true]");
            k.set(x, 2, keep.z2() - 6, "minecraft:lantern[hanging=true]");
        }
        // Bank counter by the door
        k.fill(keep.x1() + 1, 0, keep.z2() - 6, keep.x1() + 3, 0, keep.z2() - 6, "minecraft:spruce_slab[type=top]");
        k.set(keep.x1() + 2, 0, keep.z2() - 6, BANK);
        k.set(keep.x1() + 2, 0, keep.z2() - 8, BANK);
        k.fill(keep.x1() + 3, 0, keep.z2() - 8, keep.x1() + 3, 0, keep.z2() - 7, "minecraft:spruce_slab[type=top]");
        // Stairs up through the keep
        k.stairs(keep.x2() - 2, 0, keep.z2() - 2, 'n', 4, "minecraft:stone_brick");
        k.stairs(keep.x2() - 2, 4, keep.z2() - 7, 'n', 4, "minecraft:stone_brick");
        k.stairs(keep.x2() - 2, 8, keep.z2() - 12, 'n', 5, "minecraft:stone_brick");
        // First floor: knights' quarters
        for (int x = keep.x1() + 2; x <= keep.x2() - 6; x += 3) {
            k.bed(x, 4, keep.z1() + 1, "red");
            k.set(x + 1, 4, keep.z1() + 1, "minecraft:chest[facing=south]");
        }
        k.fill(keep.x1() + 1, 4, keep.z1() + 6, keep.x1() + 1, 5, keep.z2() - 2, "minecraft:bookshelf");
        k.longTable(keep.x1() + 5, keep.x2() - 8, 4, keep.z2() - 5, "dark_oak");
        k.set(cx, 6, keep.cz(), "minecraft:lantern[hanging=true]");
        // Second floor: armoury
        k.set(keep.x1() + 2, 8, keep.z1() + 2, "minecraft:smithing_table");
        k.set(keep.x1() + 4, 8, keep.z1() + 2, "minecraft:grindstone[face=floor,facing=north]");
        k.fill(keep.x1() + 1, 8, keep.z1() + 4, keep.x1() + 1, 8, keep.z2() - 2, "minecraft:barrel[facing=east]");
        k.set(cx, 10, keep.cz(), "minecraft:lantern[hanging=true]");
        // Keep corner towers with flags
        k.tower(2750, 3514, 4, 22, w, "minecraft:spruce_planks", null, 's');
        k.tower(2766, 3514, 4, 22, w, "minecraft:spruce_planks", null, 's');
        k.flag(KandarinKit.X(2750) + 1, 22, KandarinKit.Z(3514) + 1, "red", "white");
        k.flag(KandarinKit.X(2766) + 1, 22, KandarinKit.Z(3514) + 1, "blue", "white");

        // Chapel (west) with altar
        Pal chapelPal = new Pal(w, "minecraft:chiseled_stone_bricks", ANDESITE, "minecraft:deepslate_tile", "spruce", "minecraft:yellow_stained_glass_pane");
        Bld chap = k.house(2746, 3488, 2751, 3497, 1, chapelPal, 'e', NO_LADDER);
        k.set(chap.cx(), 0, chap.z1() + 1, ALTAR);
        k.set(chap.cx() - 1, 0, chap.z1() + 1, "minecraft:candle[candles=2,lit=true]");
        k.set(chap.cx() + 1, 0, chap.z1() + 1, "minecraft:candle[candles=2,lit=true]");
        for (int z = chap.z1() + 4; z <= chap.z2() - 2; z += 2) {
            k.fill(chap.x1() + 1, 0, z, chap.x2() - 2, 0, z, "minecraft:spruce_stairs[facing=south]");
        }
        // Kitchen and stables (east)
        Bld kit = k.house(2765, 3488, 2770, 3497, 1, STONE.roof("minecraft:spruce"), 'w', NO_LADDER);
        k.set(kit.x2() - 1, 0, kit.z1() + 1, RANGE);
        k.set(kit.x2() - 1, 0, kit.z1() + 2, "minecraft:smoker[facing=west]");
        k.fill(kit.x2() - 1, 0, kit.z1() + 4, kit.x2() - 1, 0, kit.z2() - 1, "minecraft:barrel[facing=west]");
        k.longTable(kit.x1() + 2, kit.x2() - 3, 0, kit.cz(), "spruce");
        k.set(kit.cx(), 2, kit.cz(), "minecraft:lantern[hanging=true]");
        Bld st = k.house(2765, 3504, 2770, 3512, 1, STONE.roof("minecraft:spruce"), 'w', NO_LADDER | NO_WINDOWS);
        for (int z = st.z1() + 1; z <= st.z2() - 1; z += 3) {
            k.fill(st.x1() + 3, 0, z, st.x2() - 1, 0, z, "minecraft:spruce_fence");
            k.set(st.x2() - 1, 0, z + 1, HAY);
        }
        // Courtyard details: well, dummies, trees
        k.well(KandarinKit.X(2753), KandarinKit.Z(3489));
        for (int i = 0; i < 3; i++) {
            int x = KandarinKit.X(2760) + i * 3;
            int z = KandarinKit.Z(3489);
            k.set(x, 0, z, "minecraft:oak_fence");
            k.set(x, 1, z, HAY);
            k.set(x, 2, z, "minecraft:carved_pumpkin[facing=west]");
        }
        k.oak(KandarinKit.X(2747), KandarinKit.Z(3507), 6);
        k.oak(KandarinKit.X(2747), KandarinKit.Z(3513), 5);
        k.oak(KandarinKit.X(2768), KandarinKit.Z(3516), 5);
        k.meadow(KandarinKit.X(2738), KandarinKit.Z(3483), KandarinKit.X(2782), KandarinKit.Z(3466), 13);
        camelotDetails(k, keep);
        return k.build();
    }

    /**
     * Camelot's finishing: a slate roof over the keep behind its battlement walk, buttresses and
     * red-and-gold banners on the curtain walls, arrow slits, and a furnished courtyard.
     */
    private static void camelotDetails(KandarinKit k, Bld keep) {
        int g = k.g;
        String w = SB;
        // the keep's great slate roof (the battlement walk stays clear round it)
        int top = keep.top();
        k.roof(keep.x1() + 3, keep.z1() + 3, keep.x2() - 3, keep.z2() - 3, top, "minecraft:deepslate_tile", w);
        k.d.texture(keep.x1(), keep.z1(), keep.x2(), keep.z2(), g + 1, g + top - 1, w, 6);
        for (int x = keep.x1() + 4; x <= keep.x2() - 4; x += 8) {
            k.d.banner(x, g + 10, keep.z2() + 1, 's', "red");
            k.d.banner(x + 4, g + 10, keep.z2() + 1, 's', "yellow");
        }
        // curtain walls: buttresses, slits and banners on the outer faces
        int x1 = KandarinKit.X(2744);
        int x2 = KandarinKit.X(2772) + 1;
        int z1 = KandarinKit.Z(3519);
        int z2 = KandarinKit.Z(3485) + 1;
        k.d.buttresses(x1, z1 + 10, x1, z2 - 10, 'w', 7, g, g + 8, w, "minecraft:stone_brick_stairs");
        k.d.buttresses(x2, z1 + 10, x2, z2 - 10, 'e', 7, g, g + 8, w, "minecraft:stone_brick_stairs");
        k.d.buttresses(x1 + 10, z1, x2 - 10, z1, 'n', 7, g, g + 8, w, "minecraft:stone_brick_stairs");
        for (int x = x1 + 13; x <= x2 - 13; x += 7) {
            k.d.slit(x, g + 5, g + 6, z1, w);
        }
        for (int z = z1 + 13; z <= z2 - 13; z += 7) {
            k.d.slit(x1, g + 5, g + 6, z, w);
            k.d.slit(x2, g + 5, g + 6, z, w);
        }
        for (int x : new int[]{KandarinKit.X(2755) - 1, KandarinKit.X(2760) + 2}) {
            k.d.banner(x, g + 8, z2 + 1, 's', "red");
            k.d.banner(x + (x < KandarinKit.X(2757) ? -2 : 2), g + 8, z2 + 1, 's', "yellow");
        }
        // courtyard: benches, lamp posts, a sign post, crates by the kitchen and flower tubs
        k.d.bench(KandarinKit.X(2750), g, KandarinKit.Z(3499), 'e', 3, 'n', "dark_oak");
        k.d.bench(KandarinKit.X(2762), g, KandarinKit.Z(3499), 'e', 3, 'n', "dark_oak");
        k.d.lamp(KandarinKit.X(2756) - 1, g, KandarinKit.Z(3495), "minecraft:dark_oak_fence");
        k.d.lamp(KandarinKit.X(2759) + 2, g, KandarinKit.Z(3495), "minecraft:dark_oak_fence");
        k.d.signpost(KandarinKit.X(2756) - 1, g, KandarinKit.Z(3490), "spruce");
        k.d.crates(KandarinKit.X(2764), g, KandarinKit.Z(3490));
        for (int i = 0; i < 4; i++) {
            k.d.planter(keep.x1() + 1 + i * 2, g, keep.z2() + 2, i);
            k.d.planter(keep.x2() - 1 - i * 2, g, keep.z2() + 2, i + 1);
        }
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

        Bld bank = k.house(2804, 3437, 2812, 3442, 1, STONE.roof("minecraft:spruce"), 's', NO_LADDER | WIDE_DOOR);
        int bz = bank.z1() + 2;
        k.fill(bank.x1() + 1, 0, bz, bank.x2() - 1, 0, bz, "minecraft:spruce_slab[type=top]");
        for (int x = bank.x1() + 2; x <= bank.x2() - 2; x += 3) {
            k.set(x, 0, bz, BANK);
        }
        k.set(bank.cx(), 2, bank.cz() + 1, "minecraft:lantern[hanging=true]");

        Bld hick = k.house(2819, 3437, 2825, 3443, 1, CATHERBY, 's', NO_LADDER);
        k.counter(hick, ARCHERY, "spruce");
        k.set(hick.x1() + 1, 0, hick.z2() - 1, "minecraft:fletching_table");
        k.set(hick.x2() - 1, 0, hick.z2() - 1, "minecraft:target");

        Bld harry = k.house(2830, 3438, 2835, 3443, 1, CATHERBY, 's', NO_LADDER);
        k.counter(harry, TOOLS, "spruce");
        k.set(harry.x1() + 1, 0, harry.z2() - 1, "minecraft:barrel[facing=up]");

        Bld caleb = k.house(2814, 3446, 2818, 3451, 1, CATHERBY, 's', FURN | NO_LADDER);
        k.set(caleb.cx(), 0, caleb.z1() + 1, RANGE);

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
        k.flatten(2693, 3396, 2711, 3412);
        int cx = KandarinKit.X(2702) + 1;
        int cz = KandarinKit.Z(3404) + 1;
        k.disc(cx, cz, -1, -1, 9, GRAVEL);
        k.towerB(cx, cz, 6, 19, SB, "minecraft:dark_oak_planks", "minecraft:purple_terracotta", 's', 5);
        // ground: library; first: alchemy; second: observatory
        k.ring(cx, cz, 0, 1, 4, "minecraft:bookshelf");
        k.clear(cx - 1, 0, cz + 4, cx + 1, 1, cz + 4);
        k.clear(cx + 4, 0, cz - 1, cx + 4, 1, cz + 1);
        k.clear(cx - 4, 0, cz - 1, cx - 4, 1, cz + 1);
        k.set(cx, 0, cz, "minecraft:lectern[facing=south]");
        k.set(cx, 4, cz, "minecraft:enchanting_table");
        k.set(cx - 2, 4, cz - 2, "minecraft:brewing_stand");
        k.set(cx + 2, 4, cz - 2, "minecraft:cauldron");
        k.set(cx - 2, 4, cz + 2, "minecraft:chest[facing=north]");
        k.set(cx, 9, cz, "minecraft:amethyst_cluster");
        k.set(cx - 2, 9, cz, "minecraft:bookshelf");
        k.set(cx + 2, 9, cz, "minecraft:crafting_table");
        k.set(cx, 14, cz, "minecraft:respawn_anchor");
        k.set(cx, 8, cz, "minecraft:lantern[hanging=true]");
        k.set(cx, 3, cz, "minecraft:lantern[hanging=true]");
        k.ring(cx, cz, 0, 0, 9, "minecraft:spruce_fence");
        k.clear(cx - 1, 0, cz + 9, cx + 1, 0, cz + 9);
        k.oak(cx - 8, cz - 8, 6);
        k.oak(cx + 8, cz - 7, 5);
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
