package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

/** Tirannwn: Lletya, the Tyras camp and the crystal city of Prifddinas. */
final class KandarinElves {
    private KandarinElves() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(lletya());
        out.add(lletyaPatch());
        out.add(tyrasCamp());
        out.add(prifddinas());
    }

    private static final Pal ELF_HOUSE = ELF.roof("minecraft:dark_prismarine");

    static void crystalTree(KandarinKit k, int x, int z, int h) {
        k.blob(x, h, z, 3, 2, 3, "minecraft:light_blue_stained_glass", true);
        k.fill(x, 0, z, x, h, z, "minecraft:quartz_pillar");
        k.set(x, h + 1, z, "minecraft:sea_lantern");
    }

    static void crystalLamp(KandarinKit k, int x, int z) {
        k.fill(x, 0, z, x, 2, z, "minecraft:quartz_pillar");
        k.set(x, 3, z, "minecraft:sea_lantern");
        k.set(x, 4, z, "minecraft:amethyst_cluster");
    }

    /** An elven tree house: a big tree with a ring deck, railing and a ladder. */
    static void treeHouse(KandarinKit k, int x, int z, int deck) {
        k.blob(x, deck + 6, z, 6, 3, 6, "minecraft:dark_oak_leaves[persistent=true]", true);
        k.fill(x - 1, 0, z - 1, x, deck + 6, z, "minecraft:dark_oak_log");
        k.disc(x, z, deck, deck, 4, "minecraft:birch_planks");
        k.ring(x, z, deck + 1, deck + 1, 4, "minecraft:birch_fence");
        k.fill(x - 1, deck + 1, z - 1, x, deck + 1, z, "minecraft:dark_oak_log");
        k.set(x + 2, deck + 1, z + 2, "minecraft:white_wool");
        k.set(x - 2, deck + 1, z + 2, "minecraft:chest[facing=north]");
        k.set(x + 2, deck + 1, z - 2, "minecraft:lantern");
        if (!k.ladder(x + 1, z, 0, deck, "east")) {
            k.ladder(x - 2, z, 0, deck, "west");
        }
        k.clear(x + 1, deck + 1, z, x + 1, deck + 1, z);
        k.clear(x - 2, deck + 1, z, x - 2, deck + 1, z);
    }

    // ------------------------------------------------------------------ Lletya

    static StructureDef lletya() {
        KandarinKit k = new KandarinKit("lletya", "Lletya").replaces("LLETYA");
        k.flatten(2318, 3146, 2362, 3190, "minecraft:moss_block");
        k.road(2320, 3165, 2360, 3167, "minecraft:mossy_cobblestone", GRAVEL);
        k.road(2337, 3148, 2339, 3188, "minecraft:mossy_cobblestone", GRAVEL);
        int cx = KandarinKit.X(2338);
        int cz = KandarinKit.Z(3166);
        k.disc(cx, cz, -1, -1, 6, "minecraft:smooth_quartz");
        crystalLamp(k, cx - 5, cz - 5);
        crystalLamp(k, cx + 5, cz + 5);
        crystalLamp(k, cx + 5, cz - 5);
        crystalLamp(k, cx - 5, cz + 5);
        Bld bank = k.house(2350, 3169, 2356, 3175, 1, ELF_HOUSE, 'w', NO_LADDER);
        int bx = bank.x1() + 2;
        k.fill(bx, 0, bank.z1() + 1, bx, 0, bank.z2() - 1, "minecraft:birch_slab[type=top]");
        k.set(bx, 0, bank.z1() + 2, BANK);
        k.set(bx, 0, bank.z2() - 2, BANK);
        Bld gen = k.house(2324, 3169, 2330, 3175, 1, ELF_HOUSE, 'e', NO_LADDER);
        k.counter(gen, GENERAL, "birch");
        Bld rh = k.house(2341, 3177, 2347, 3183, 1, ELF_HOUSE, 's', FURN | NO_LADDER);
        k.set(rh.cx(), 0, rh.z1() + 1, RANGE);
        k.house(2324, 3150, 2330, 3156, 2, ELF_HOUSE, 'n', FURN);
        k.house(2328, 3178, 2334, 3184, 1, ELF_HOUSE, 'e', FURN);
        k.house(2350, 3178, 2356, 3184, 2, ELF_HOUSE, 'w', FURN);
        treeHouse(k, KandarinKit.X(2322), KandarinKit.Z(3160), 7);
        treeHouse(k, KandarinKit.X(2357), KandarinKit.Z(3150), 8);
        treeHouse(k, KandarinKit.X(2322), KandarinKit.Z(3186), 6);
        treeHouse(k, KandarinKit.X(2358), KandarinKit.Z(3188), 7);
        for (int[] t : new int[][]{{2333, 3148}, {2345, 3186}, {2319, 3176}, {2361, 3162}, {2346, 3148}}) {
            k.bigTree(KandarinKit.X(t[0]), KandarinKit.Z(t[1]), 9, 4, "minecraft:dark_oak_log", "minecraft:dark_oak_leaves");
        }
        k.meadow(KandarinKit.X(2318), KandarinKit.Z(3190), KandarinKit.X(2362), KandarinKit.Z(3146), 8);
        return k.build();
    }

    static StructureDef lletyaPatch() {
        KandarinKit k = new KandarinKit("lletya_fruit_patch", "Lletya fruit tree patch").replaces("lletya_fruit_patch");
        k.fence(2347, 3155, 2352, 3161, "minecraft:birch_fence");
        k.tClear(2347, 3158, 2347, 3158, 0, 0);
        k.floor(2349, 3157, 2351, 3159, "minecraft:podzol");
        k.at(2350, 3158, 0, "runecraft:fruit_tree_patch");
        return k.build();
    }

    // ------------------------------------------------------------------ Tyras camp

    /** King Tyras's camp. The camp's boss arena is generated 24 blocks east of its centre, so the east yard stays open. */
    static StructureDef tyrasCamp() {
        KandarinKit k = new KandarinKit("tyras_camp", "Tyras camp").replaces("tyras_camp");
        k.flatten(2170, 3134, 2206, 3166, "minecraft:coarse_dirt");
        String pal = "minecraft:stripped_spruce_wood";
        k.tFill(2172, 3164, 2205, 3164, 0, 3, pal);
        k.tFill(2172, 3136, 2205, 3136, 0, 3, pal);
        k.tFill(2172, 3136, 2172, 3164, 0, 3, pal);
        k.tFill(2205, 3136, 2205, 3164, 0, 3, pal);
        k.tClear(2205, 3149, 2205, 3151, 0, 3);
        k.tClear(2172, 3149, 2172, 3150, 0, 3);
        // tents in Tyras red and white
        for (int i = 0; i < 4; i++) {
            k.tent(KandarinKit.X(2175 + i * 4), KandarinKit.Z(3160), 6, i % 2 == 0 ? "red" : "white");
            k.tent(KandarinKit.X(2175 + i * 4), KandarinKit.Z(3140), 6, i % 2 == 0 ? "white" : "red");
        }
        // General Hining's tent
        Bld hq = k.house(2176, 3146, 2184, 3153, 1, new Pal("minecraft:red_wool", "minecraft:spruce_log", "minecraft:spruce_planks", "minecraft:spruce", "spruce", "minecraft:white_wool"), 'e', NO_LADDER | FURN);
        k.set(hq.x1() + 1, 0, hq.cz(), "minecraft:cartography_table");
        k.flag(hq.x2() + 1, 0, hq.z1() - 1, "red", "white");
        // catapult aimed west at the elven forest
        int cx = KandarinKit.X(2189);
        int cz = KandarinKit.Z(3158);
        k.fill(cx - 2, 0, cz - 1, cx + 2, 0, cz + 1, "minecraft:spruce_planks");
        k.fill(cx - 2, 1, cz - 1, cx - 2, 3, cz - 1, "minecraft:spruce_fence");
        k.fill(cx - 2, 1, cz + 1, cx - 2, 3, cz + 1, "minecraft:spruce_fence");
        k.fill(cx - 2, 4, cz - 1, cx - 2, 4, cz + 1, "minecraft:stripped_spruce_log[axis=z]");
        k.fill(cx - 1, 2, cz, cx + 3, 2, cz, "minecraft:spruce_log[axis=x]");
        k.set(cx + 3, 3, cz, "minecraft:cobblestone");
        k.set(cx + 1, 1, cz, "minecraft:chain");
        // campfire and supplies
        k.set(KandarinKit.X(2188), 0, KandarinKit.Z(3145), "minecraft:campfire[lit=true]");
        for (int i = 0; i < 4; i++) {
            k.set(KandarinKit.X(2190) + i, 0, KandarinKit.Z(3141), "minecraft:barrel[facing=up]");
        }
        k.set(KandarinKit.X(2174), 0, KandarinKit.Z(3146), "minecraft:anvil");
        return k.build();
    }

    // ------------------------------------------------------------------ Prifddinas

    /**
     * Prifddinas, the crystal city: circular walls with four gates, the Tower of Voices in the centre
     * with its reflecting pool, radial avenues and eight clan districts of crystal halls and trees.
     * The two Gauntlet arenas are generated either side of the Gauntlet plaza, which stays open.
     */
    static StructureDef prifddinas() {
        KandarinKit k = new KandarinKit("prifddinas", "Prifddinas").replaces("the_gauntlet");
        k.flatten(3196, 6036, 3332, 6172);
        int cx = KandarinKit.X(3264);
        int cz = KandarinKit.Z(6104);
        // city walls and gates
        k.ring(cx, cz, -1, 9, 126, "minecraft:calcite");
        k.ring(cx, cz, 10, 10, 126, "minecraft:quartz_slab");
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int gx = cx + d[0] * 126;
            int gz = cz + d[1] * 126;
            k.clear(gx - 2 - Math.abs(d[0]) * 1, 0, gz - 2 - Math.abs(d[1]), gx + 2 + Math.abs(d[0]), 5, gz + 2 + Math.abs(d[1]));
            k.fill(gx - 3 * Math.abs(d[1]), 0, gz - 3 * Math.abs(d[0]), gx - 3 * Math.abs(d[1]), 14, gz - 3 * Math.abs(d[0]), "minecraft:quartz_pillar");
            k.fill(gx + 3 * Math.abs(d[1]), 0, gz + 3 * Math.abs(d[0]), gx + 3 * Math.abs(d[1]), 14, gz + 3 * Math.abs(d[0]), "minecraft:quartz_pillar");
            k.set(gx - 3 * Math.abs(d[1]), 15, gz - 3 * Math.abs(d[0]), "minecraft:sea_lantern");
            k.set(gx + 3 * Math.abs(d[1]), 15, gz + 3 * Math.abs(d[0]), "minecraft:sea_lantern");
        }
        // avenues: four straight, four diagonal, and a ring road
        k.fill(cx - 124, -1, cz - 1, cx + 124, -1, cz + 1, "minecraft:smooth_quartz");
        k.fill(cx - 1, -1, cz - 124, cx + 1, -1, cz + 124, "minecraft:smooth_quartz");
        for (int i = 18; i <= 88; i++) {
            for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                k.fill(cx + d[0] * i - 1, -1, cz + d[1] * i - 1, cx + d[0] * i + 1, -1, cz + d[1] * i + 1, "minecraft:smooth_quartz");
            }
        }
        k.ring(cx, cz, -1, -1, 70, "minecraft:smooth_quartz");
        k.ring(cx, cz, -1, -1, 71, "minecraft:smooth_quartz");
        // Tower of Voices with plaza and reflecting pool
        k.disc(cx, cz, -1, -1, 26, "minecraft:smooth_quartz");
        k.ring(cx, cz, -1, -1, 18, WATER);
        k.ring(cx, cz, -1, -1, 19, WATER);
        k.ring(cx, cz, -1, -1, 20, WATER);
        k.ring(cx, cz, -2, -2, 19, "minecraft:prismarine");
        k.fill(cx - 1, -1, cz + 17, cx + 1, -1, cz + 21, "minecraft:smooth_quartz");
        k.fill(cx - 1, -1, cz - 21, cx + 1, -1, cz - 17, "minecraft:smooth_quartz");
        int y = 0;
        for (int r = 12; r >= 1; r--) {
            int layers = r > 8 ? 3 : (r > 4 ? 6 : 8);
            for (int s = 0; s < layers; s++) {
                String b = (y % 9 == 8) ? "minecraft:light_blue_stained_glass" : (r % 2 == 0 ? "minecraft:calcite" : "minecraft:quartz_block");
                k.ring(cx, cz, y, y, r, b);
                y++;
            }
        }
        k.fill(cx, y, cz, cx, y + 6, cz, "minecraft:light_blue_stained_glass");
        k.set(cx, y + 7, cz, "minecraft:sea_lantern");
        k.disc(cx, cz, 0, 4, 10, AIR);
        k.disc(cx, cz, -1, -1, 10, "minecraft:polished_diorite");
        k.clear(cx - 1, 0, cz + 11, cx + 1, 3, cz + 12);
        k.set(cx, 0, cz, "minecraft:beacon");
        k.set(cx - 4, 0, cz - 4, BANK);
        k.set(cx + 4, 0, cz - 4, BANK);
        k.set(cx - 4, 0, cz + 4, BANK);
        k.set(cx + 4, 0, cz + 4, BANK);
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4 + Math.PI / 8;
            crystalLamp(k, cx + (int) Math.round(23 * Math.cos(ang)), cz + (int) Math.round(23 * Math.sin(ang)));
        }
        // clan districts
        String[] clans = {"Iorwerth", "Ithell", "Amlodd", "Hefin", "Cadarn", "Crwys", "Meilyr", "Trahaearn"};
        int gx = KandarinKit.X(3234);
        int gz = KandarinKit.Z(6118);
        for (int c = 0; c < clans.length; c++) {
            double ang = c * Math.PI / 4 + Math.PI / 8;
            for (int j = 0; j < 3; j++) {
                double rad = 48 + j * 30;
                double a2 = ang + (j - 1) * 0.12;
                int hx = cx + (int) Math.round(rad * Math.cos(a2));
                int hz = cz + (int) Math.round(rad * Math.sin(a2));
                int w = 6 + (c + j) % 3;
                int d = 5 + (c * 2 + j) % 3;
                // keep the Gauntlet plaza and its arenas open
                if (Math.abs(hz - gz) <= 24 && hx >= gx - 52 && hx <= gx + 52) {
                    crystalTree(k, hx, hz + (hz < gz ? -26 : 26), 7);
                    continue;
                }
                char door = Math.abs(Math.cos(a2)) > Math.abs(Math.sin(a2)) ? (Math.cos(a2) > 0 ? 'w' : 'e') : (Math.sin(a2) > 0 ? 'n' : 's');
                Bld h = k.houseB(hx - w, hz - d, hx + w, hz + d, 1 + (j % 2), CRYSTAL, door, FURN);
                switch (c) {
                    case 1 -> k.set(h.x1() + 2, 0, h.z1() + 1, j == 0 ? FURNACE : ANVIL);
                    case 3 -> k.set(h.x1() + 2, 0, h.z1() + 1, ALTAR);
                    case 5 -> k.set(h.x1() + 2, 0, h.z1() + 1, j == 0 ? BANK : RANGE);
                    case 6 -> k.set(h.x1() + 2, 0, h.z1() + 1, HERB_TABLE);
                    case 7 -> k.set(h.x1() + 2, 0, h.z1() + 1, j == 0 ? GENERAL : TOOLS);
                    default -> k.set(h.x1() + 2, 0, h.z1() + 1, "minecraft:lectern[facing=south]");
                }
                crystalTree(k, hx + (int) Math.round(12 * Math.cos(a2 + 1.2)), hz + (int) Math.round(12 * Math.sin(a2 + 1.2)), 6 + j);
            }
        }
        // Gauntlet plaza with its crystal arch
        k.disc(gx, gz, -1, -1, 9, "minecraft:polished_diorite");
        k.fill(gx - 4, 0, gz - 8, gx - 4, 7, gz - 8, "minecraft:quartz_pillar");
        k.fill(gx + 4, 0, gz - 8, gx + 4, 7, gz - 8, "minecraft:quartz_pillar");
        k.fill(gx - 4, 8, gz - 8, gx + 4, 8, gz - 8, "minecraft:light_blue_stained_glass");
        k.set(gx, 9, gz - 8, "minecraft:sea_lantern");
        // groves between the districts
        for (int a = 0; a < 16; a++) {
            double ang = a * Math.PI / 8;
            int tx = cx + (int) Math.round(105 * Math.cos(ang));
            int tz = cz + (int) Math.round(105 * Math.sin(ang));
            if (Math.abs(tz - gz) <= 24 && tx >= gx - 52 && tx <= gx + 52) {
                continue;
            }
            k.bigTree(tx, tz, 8, 4, "minecraft:birch_log", "minecraft:cherry_leaves");
        }
        prifddinasFarm(k);
        return k.build();
    }

    /**
     * The Prifddinas farming patches (OSRS: two allotments, a flower patch, a compost bin and the crystal
     * tree patch, in the north-east of the city; exact spot approx.). Laid out in an open space between
     * the clan halls (checked against this structure's blocks) so nothing of the city is overwritten.
     * The gardener and tool leprechaun come from the farming module's "prifddinas_farm" place.
     */
    private static void prifddinasFarm(KandarinKit k) {
        int fx = KandarinKit.X(3290);
        int fz = KandarinKit.Z(6122);
        k.fill(fx, -1, fz, fx + 9, -1, fz + 6, "minecraft:coarse_dirt");
        k.fenceB(fx - 1, fz - 1, fx + 10, fz + 7, "minecraft:birch_fence");
        k.clear(fx + 4, 0, fz + 7, fx + 5, 0, fz + 7);
        k.fill(fx + 1, -1, fz + 2, fx + 5, -1, fz + 2, "minecraft:farmland[moisture=7]");
        k.fill(fx + 1, -1, fz + 4, fx + 5, -1, fz + 4, WATER);
        k.set(fx + 1, 0, fz + 2, "runecraft:allotment_patch");
        k.set(fx + 3, 0, fz + 2, "runecraft:flower_patch");
        k.set(fx + 5, 0, fz + 2, "runecraft:allotment_patch");
        k.set(fx + 8, 0, fz + 1, "runecraft:compost_bin");
        k.fill(fx + 7, -1, fz + 4, fx + 9, -1, fz + 6, "minecraft:moss_block");
        k.set(fx + 8, 0, fz + 5, "runecraft:crystal_tree_patch");
    }
}
