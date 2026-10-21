package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Kandarin, the Fremennik Province and Tirannwn: the Stronghold Slayer Cave below the Gnome
 * Stronghold, the Waterfall Dungeon and the Ancient Cavern at Baxtorian Falls, the Kraken Cove, the
 * Smoke Devil Dungeon, the Fremennik Slayer Dungeon and the Iorwerth Dungeon.
 */
final class DungeonsKandarin {
    private DungeonsKandarin() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(strongholdSlayerCave());
        out.add(waterfallDungeon());
        out.add(ancientCavern());
        out.add(krakenCove());
        out.add(smokeDevilDungeon());
        out.add(fremennikSlayerDungeon());
        out.add(iorwerthDungeon());
        out.add(CrashSiteCavern.build());
        out.add(KruksDungeon.build());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    // ------------------------------------------------------------------ Stronghold Slayer Cave

    // Cache spawn tiles (mejrs/data_osrs; the wiki's LocLines agree) of the Stronghold Slayer Cave since Diverse Dungeons
    // (27 April 2017): only bloodveld, hellhounds, aberrant spectres, ankou and fire giants were left in it.
    static final int[] SSC_BLOODVELD = {2434, 9817, 2434, 9824, 2440, 9821, 2447, 9822, 2451, 9817, 2453, 9822, 2465, 9830, 2470, 9834,
            2472, 9830, 2485, 9822, 2488, 9827, 2489, 9818};
    static final int[] SSC_ANKOU = {2468, 9802, 2470, 9803, 2472, 9797, 2475, 9798, 2475, 9808, 2477, 9807, 2481, 9799, 2483, 9801};
    static final int[] SSC_SPECTRES = {2443, 9785, 2444, 9777, 2456, 9777, 2470, 9782, 2471, 9776, 2452, 9794, 2460, 9793, 2441, 9775,
            2443, 9781, 2456, 9790, 2458, 9780, 2459, 9774, 2468, 9778};
    static final int[] SSC_HELLHOUNDS = {2409, 9786, 2412, 9784, 2415, 9786, 2426, 9773, 2426, 9782, 2429, 9770, 2429, 9786, 2430, 9775,
            2410, 9801, 2413, 9797, 2417, 9796, 2433, 9771, 2433, 9784};
    static final int[] SSC_FIRE_GIANTS = {2391, 9781, 2392, 9785, 2394, 9778, 2395, 9771, 2399, 9770, 2400, 9777, 2401, 9784, 2402, 9780,
            2403, 9769, 2410, 9777, 2412, 9774, 2415, 9772};

    /**
     * The Stronghold Slayer Cave (Nieve's cave) below the Tree Gnome Stronghold, on the cache's tiles (the cave by Nieve,
     * surface 2430, 3424, leads down to 2429, 9824): the bloodveld's long cavern east of the entrance, the ankou and the
     * aberrant spectres south-east, the hellhounds south and the fire giants in the far south-west, with the wiki's
     * counts (12 bloodveld, 8 ankou, 13 aberrant spectres, 13 hellhounds, 12 fire giants). Anyone may enter, but every
     * monster can only be fought on a Slayer task to kill it (OSRS Wiki). The 72 Agility tunnel (2429, 9806 to 2435,
     * 9806) cuts across to the ankou and spectres. Room outlines and the passages between them are approx; the black and
     * greater demons, nechryael and abyssal demons left in 2017 (Chasm of Fire, Slayer Tower basement).
     */
    static Dungeons.Dungeon strongholdSlayerCave() {
        Cave c = new Cave("dng_stronghold_slayer_cave", "Stronghold Slayer Cave", mc("deepslate_bricks"), mc("cobbled_deepslate"),
                mc("glowstone"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, -6400);
        int f = 18;
        int[] entry = {2424, 9818, 2433, 9830};
        int[] bloodveld = {2433, 9814, 2492, 9837};
        int[] ankou = {2465, 9797, 2486, 9811};
        int[] spectres = {2438, 9771, 2474, 9795};
        int[] houndsNorth = {2406, 9784, 2421, 9804};
        int[] houndsEast = {2422, 9767, 2436, 9790};
        int[] giants = {2388, 9766, 2418, 9787};
        int[][] rooms = {entry, bloodveld, ankou, spectres, houndsNorth, houndsEast, giants};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hallZ(2429, 9818, 2429, 9790, f);          // south from the entrance to the hellhounds
        s.hallZ(2476, 9814, 2476, 9811, f);          // the bloodveld down to the ankou
        s.hall(2436, 9780, 2438, 9780, f);           // the hellhounds east to the aberrant spectres
        s.hall(2435, 9806, 2465, 9806, f);           // from the tunnel's far end to the ankou
        c.carve();
        c.pool(s.x(2396), s.z(9776), s.x(2399), s.z(9773), f, mc("lava"));
        c.pool(s.x(2405), s.z(9783), s.x(2407), s.z(9781), f, mc("lava"));
        c.pool(s.x(2480), s.z(9835), s.x(2484), s.z(9832), f, mc("water"));
        for (int x = 2440; x <= 2488; x += 12) {
            c.pillar(s.x(x), s.z(9836), f, 7, mc("chiseled_deepslate"));
        }
        c.shortcut("stronghold_slayer_tunnel", s.x(2429), s.z(9806), f, s.x(2435), s.z(9806), f);
        // approx: the hut stands south of the Stronghold's grounds (the real cave, 2430, 3424 by Nieve, is inside the town's levelled ground).
        c.entrance(StructureDef.bx(2429), StructureDef.bz(3396), f, mc("mossy_stone_bricks"), mc("cobblestone"), 's');
        s.camps(rooms, f, CacheDungeons.RESPAWN, "bloodveld", SSC_BLOODVELD);
        s.camps(rooms, f, CacheDungeons.RESPAWN, "ankou", SSC_ANKOU);
        s.camps(rooms, f, CacheDungeons.RESPAWN, "aberrant_spectre", SSC_SPECTRES);
        s.camps(new int[][]{houndsNorth, houndsEast}, f, CacheDungeons.RESPAWN, "hellhound", SSC_HELLHOUNDS);
        s.camps(new int[][]{giants}, f, CacheDungeons.RESPAWN, "fire_giant", SSC_FIRE_GIANTS);
        return c.build();
    }

    // ------------------------------------------------------------------ Waterfall Dungeon

    /**
     * The Waterfall Dungeon behind Baxtorian Falls (entered from the riverbank, tile 2516, 3456):
     * the fire giants' hall, the skeletons guarding the way, and the chamber where Glarial's chalice
     * holds Baxtorian's treasure.
     */
    static Dungeons.Dungeon waterfallDungeon() {
        Cave c = new Cave("dng_waterfall_dungeon", "Waterfall Dungeon", mc("mossy_stone_bricks"), mc("stone_bricks"), mc("sea_lantern"));
        int f = 34;
        int ox = bx(2516);
        int oz = bz(3456);
        c.room(ox - 5, oz - 5, ox + 5, oz + 5, f, 5);
        c.hall(ox + 5, oz, ox + 20, oz, f, 3, 4);
        c.room(ox + 20, oz - 14, ox + 44, oz + 14, f, 8);       // fire giants
        c.hallZ(ox, oz - 5, ox, oz - 24, f, 3, 4);
        c.room(ox - 12, oz - 44, ox + 12, oz - 24, f, 5);       // skeletons
        c.hall(ox - 12, oz - 34, ox - 24, oz - 34, f, 3, 4);
        c.room(ox - 44, oz - 44, ox - 24, oz - 24, f, 6);       // Glarial's chalice
        c.carve();
        int cx = ox - 34;
        int cz = oz - 34;
        c.fill(cx - 1, f + 1, cz - 1, cx + 1, f + 1, cz + 1, mc("gold_block"));
        c.set(cx, f + 2, cz, mc("decorated_pot"));
        for (int[] p : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            c.pillar(cx + p[0], cz + p[1], f, 6, mc("chiseled_stone_bricks"));
        }
        c.pool(ox - 4, oz + 3, ox + 4, oz + 4, f, mc("water"));
        c.entrance(ox, oz, f, mc("mossy_cobblestone"), mc("mossy_cobblestone"), 'e');
        c.camp(ox + 22, oz - 12, ox + 42, oz + 12, f, 4, 50, "fire_giant");
        c.camp(ox - 10, oz - 42, ox + 10, oz - 26, f, 4, 25, "minecraft:skeleton");
        return c.build();
    }

    // ------------------------------------------------------------------ Ancient Cavern

    /**
     * The Ancient Cavern, reached through the whirlpool in the lake above Baxtorian Falls (entrance
     * on the shore, tile 2516, 3512): the landing pool, the waterfiends' grotto, the brutal green
     * dragons' hall, the barbarian forge and, up the stairs, the mithril dragons' chamber.
     */
    static Dungeons.Dungeon ancientCavern() {
        Cave c = new Cave("dng_ancient_cavern", "Ancient Cavern", mc("deepslate_tiles"), mc("polished_deepslate"), mc("sea_lantern"));
        int f = 4;
        int ox = bx(2516);
        int oz = bz(3512);
        c.room(ox - 8, oz - 8, ox + 8, oz + 8, f, 6);
        c.hall(ox + 8, oz, ox + 24, oz, f, 3, 5);
        c.room(ox + 24, oz - 16, ox + 52, oz + 16, f, 6);                 // waterfiends
        c.hallZ(ox, oz + 8, ox, oz + 24, f, 3, 5);
        c.room(ox - 24, oz + 24, ox + 24, oz + 60, f, 9);                 // brutal green dragons
        c.hall(ox - 8, oz, ox - 22, oz, f, 3, 5);
        c.room(ox - 40, oz - 10, ox - 22, oz + 10, f, 6);                 // the forge
        c.stairs(ox, oz - 8, 'n', f, f + 12, 3, 5);
        int mz = oz - 8 - 13;
        c.room(ox - 22, mz - 34, ox + 22, mz, f + 12, 9);               // mithril dragons
        c.carve();
        c.pool(ox - 5, oz - 5, ox + 5, oz + 5, f, mc("water"));
        c.pool(ox + 30, oz - 10, ox + 46, oz + 10, f, mc("water"));
        c.set(ox - 34, f + 1, oz, rc("anvil")).set(ox - 34, f + 1, oz + 3, rc("furnace"));
        c.set(ox - 36, f + 1, oz - 8, mc("magma_block"));
        c.entrance(ox + 6, oz - 6, f, mc("prismarine_bricks"), mc("prismarine_bricks"), 'e');
        c.camp(ox + 26, oz - 14, ox + 50, oz + 14, f, 4, 25, "waterfiend");
        c.camp(ox - 22, oz + 26, ox + 22, oz + 58, f, 4, 50, "brutal_green_dragon");
        c.camp(ox - 20, mz - 32, ox + 20, mz - 2, f + 12, 3, 50, "mithril_dragon");
        return c.build();
    }

    // ------------------------------------------------------------------ Kraken Cove

    /**
     * The Kraken Cove on the Piscatoris coast (entrance tile 2268, 3611): a flooded cave of pools
     * and walkways where cave krakens lurk as whirlpools, and the Kraken's own pool (the boss is
     * fought at its arena outside).
     */
    static Dungeons.Dungeon krakenCove() {
        Cave c = new Cave("dng_kraken_cove", "Kraken Cove", mc("dark_prismarine"), mc("prismarine_bricks"), mc("sea_lantern"));
        int f = 20;
        int ox = bx(2268);
        int oz = bz(3611);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hallZ(ox, oz + 6, ox, oz + 20, f, 3, 4);
        c.room(ox - 40, oz + 20, ox + 40, oz + 70, f, 8);
        c.hall(ox + 40, oz + 45, ox + 56, oz + 45, f, 3, 4);
        c.disc(ox + 76, oz + 45, 18, f, 9);
        c.carve();
        for (int x = ox - 36; x <= ox + 28; x += 16) {
            for (int z = oz + 24; z <= oz + 60; z += 14) {
                c.pool(x, z, x + 8, z + 6, f, mc("water"));
            }
        }
        return finishKraken(c, ox, oz, f);
    }

    private static Dungeons.Dungeon finishKraken(Cave c, int ox, int oz, int f) {
        int kx = ox + 76;
        int kz = oz + 45;
        for (int dz = -12; dz <= 12; dz++) {
            int half = (int) Math.floor(Math.sqrt(12.5 * 12.5 - dz * dz));
            c.pool(kx - half, kz + dz, kx + half, kz + dz, f, mc("water"));
        }
        c.entrance(ox, oz, f, mc("dark_prismarine"), mc("prismarine_bricks"), 'w');
        c.camp(ox - 38, oz + 22, ox + 38, oz + 68, f, 6, 25, "cave_kraken");
        return c.build();
    }

    // ------------------------------------------------------------------ Smoke Devil Dungeon

    /**
     * The Smoke Devil Dungeon west of Castle Wars (entrance tile 2400, 3060): smoke-filled tunnels
     * swarming with smoke devils, and the thermonuclear smoke devil's crevice at the far end.
     */
    static Dungeons.Dungeon smokeDevilDungeon() {
        Cave c = new Cave("dng_smoke_devil_dungeon", "Smoke Devil Dungeon", mc("basalt[axis=y]"), mc("blackstone"), mc("shroomlight"));
        int f = 16;
        int ox = bx(2400);
        int oz = bz(3060);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 24, oz - 20, f, 3, 4);
        c.room(ox + 10, oz - 60, ox + 50, oz - 20, f, 7);
        c.hall(ox + 50, oz - 40, ox + 70, oz - 40, f, 3, 4);
        c.room(ox + 70, oz - 52, ox + 94, oz - 28, f, 7);
        c.carve();
        for (int[] p : new int[][]{{14, -24}, {46, -24}, {14, -56}, {46, -56}, {30, -40}, {82, -40}}) {
            c.set(ox + p[0], f + 1, oz + p[1], mc("campfire[lit=true,signal_fire=true]"));
            c.set(ox + p[0], f, oz + p[1], mc("hay_block"));
        }
        c.entrance(ox, oz, f, mc("polished_basalt[axis=y]"), mc("blackstone"), 'w');
        c.camp(ox + 12, oz - 58, ox + 48, oz - 22, f, 6, 25, "smoke_devil");
        c.camp(ox + 72, oz - 50, ox + 92, oz - 30, f, 2, 25, "smoke_devil");
        return c.build();
    }

    // ------------------------------------------------------------------ Fremennik Slayer Dungeon

    /**
     * The Fremennik Slayer Dungeon south-east of Rellekka (entrance tile 2797, 3614): a chain of
     * caverns going deeper - cave crawlers, rock slugs, cockatrices, pyrefiends, basilisks, jellies,
     * turoth and finally kurask.
     */
    static Dungeons.Dungeon fremennikSlayerDungeon() {
        Cave c = new Cave("dng_fremennik_slayer_dungeon", "Fremennik Slayer Dungeon", mc("stone"), mc("cobblestone"), mc("glowstone"));
        int f = 22;
        int ox = bx(2797);
        int oz = bz(3614);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        String[][] chambers = {{"cave_crawler"}, {"rockslug"}, {"cockatrice"}, {"pyrefiend"}, {"basilisk"}, {"jelly"}, {"turoth"},
                {"kurask"}};
        // Chambers alternate south and east along a winding path.
        int[][] centres = new int[chambers.length][];
        int px = ox;
        int pz = oz;
        for (int i = 0; i < chambers.length; i++) {
            int nx = px + (i % 2 == 0 ? 0 : 36);
            int nz = pz + (i % 2 == 0 ? 34 : 0);
            c.hall(px, pz, nx, nz, f, 3, 4);
            c.room(nx - 12, nz - 10, nx + 12, nz + 10, f, i >= 6 ? 7 : 5);
            centres[i] = new int[]{nx, nz};
            px = nx;
            pz = nz;
        }
        c.carve();
        // The chasm between the pyrefiends and the basilisks.
        int[] py = centres[3];
        c.pool(py[0] - 2, py[1] + 11, py[0] + 2, py[1] + 11, f, mc("lava"));
        for (int i = 0; i < chambers.length; i++) {
            int[] p = centres[i];
            c.pillar(p[0] - 8, p[1] - 6, f, 4, mc("cobblestone"));
            c.pillar(p[0] + 8, p[1] + 6, f, 4, mc("mossy_cobblestone"));
            c.camp(p[0] - 11, p[1] - 9, p[0] + 11, p[1] + 9, f, 5, 25, chambers[i]);
        }
        // The strange floor (81) from the cave crawlers to the pyrefiends and the narrow crevice (62) from the
        // basilisks to the turoth.
        c.shortcut("fremennik_slayer_strange_floor", centres[0][0] + 10, centres[0][1] + 8, f, centres[3][0] - 10, centres[3][1] - 8, f);
        c.shortcut("fremennik_slayer_crevice", centres[4][0] + 10, centres[4][1] + 8, f, centres[6][0] - 10, centres[6][1] - 8, f);
        c.entrance(ox, oz, f, mc("spruce_planks"), mc("cobblestone"), 'n');
        return c.build();
    }

    // ------------------------------------------------------------------ Iorwerth Dungeon

    /**
     * The Iorwerth Dungeon below the north-west of Prifddinas (entrance by the city ruins, tile
     * 2232, 3300): crystal-lit halls with dark beasts and kurask (approx. - the real dungeon holds
     * more slayer creatures).
     */
    static Dungeons.Dungeon iorwerthDungeon() {
        Cave c = new Cave("dng_iorwerth_dungeon", "Iorwerth Dungeon", mc("calcite"), mc("smooth_quartz"), mc("sea_lantern"));
        int f = 10;
        int ox = bx(2232);
        int oz = bz(3300);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 24, oz, f, 3, 5);
        c.room(ox + 24, oz - 24, ox + 64, oz + 24, f, 8);
        c.hallZ(ox, oz - 6, ox, oz - 24, f, 3, 5);
        c.room(ox - 24, oz - 60, ox + 24, oz - 24, f, 8);
        c.carve();
        for (int x = ox + 30; x <= ox + 58; x += 14) {
            c.pillar(x, oz - 18, f, 8, mc("amethyst_block"));
            c.pillar(x, oz + 18, f, 8, mc("amethyst_block"));
        }
        c.set(ox, f + 1, oz - 42, mc("budding_amethyst"));
        c.entrance(ox, oz, f, mc("calcite"), mc("smooth_quartz"), 's');
        c.camp(ox + 26, oz - 22, ox + 62, oz + 22, f, 5, 25, "dark_beast");
        c.camp(ox - 22, oz - 58, ox + 22, oz - 26, f, 5, 25, "kurask");
        return c.build();
    }
}
