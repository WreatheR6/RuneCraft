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

    /** A chamber with a short connecting hall from a corridor point (hx, hz) running along z. */
    private static void sideRoom(Cave c, int hx, int hz, int x1, int z1, int x2, int z2, int f, int h) {
        int towards = z1 > hz ? z1 : z2;
        c.hallZ(hx, hz, hx, towards, f, 3, 4);
        c.room(x1, z1, x2, z2, f, h);
    }

    // ------------------------------------------------------------------ Stronghold Slayer Cave

    /**
     * The Stronghold Slayer Cave (Nieve's cave) below the Tree Gnome Stronghold, entered just south
     * of the stronghold's grounds (tile 2430, 3396): a long gallery of chambers - aberrant spectres,
     * ankou, bloodveld, fire giants, hellhounds, black demons and greater demons - and to the north
     * the nechryael and abyssal demons.
     */
    static Dungeons.Dungeon strongholdSlayerCave() {
        Cave c = new Cave("dng_stronghold_slayer_cave", "Stronghold Slayer Cave", mc("deepslate_bricks"), mc("cobbled_deepslate"),
                mc("glowstone"));
        int f = 18;
        int ox = bx(2430);
        int oz = bz(3396);
        c.room(ox - 8, oz - 8, ox + 8, oz + 8, f, 6);
        c.hall(ox - 8, oz, ox - 120, oz, f, 3, 5);
        sideRoom(c, ox - 30, oz - 1, ox - 42, oz - 34, ox - 18, oz - 12, f, 6);   // aberrant spectres
        sideRoom(c, ox - 30, oz + 1, ox - 42, oz + 12, ox - 18, oz + 34, f, 6);   // ankou
        sideRoom(c, ox - 62, oz - 1, ox - 74, oz - 34, ox - 50, oz - 12, f, 6);   // bloodveld
        sideRoom(c, ox - 62, oz + 1, ox - 74, oz + 12, ox - 50, oz + 34, f, 8);   // fire giants
        sideRoom(c, ox - 94, oz - 1, ox - 106, oz - 34, ox - 82, oz - 12, f, 6);  // hellhounds
        sideRoom(c, ox - 94, oz + 1, ox - 106, oz + 12, ox - 82, oz + 34, f, 8);  // black demons
        c.room(ox - 146, oz - 22, ox - 120, oz + 22, f, 8);                      // greater demons
        c.hallZ(ox, oz - 8, ox, oz - 60, f, 3, 5);
        c.room(ox - 16, oz - 90, ox + 16, oz - 60, f, 7);                        // nechryael
        c.hall(ox + 16, oz - 75, ox + 40, oz - 75, f, 3, 5);
        c.room(ox + 40, oz - 96, ox + 72, oz - 54, f, 7);                        // abyssal demons
        c.carve();
        c.pool(ox - 140, oz - 4, ox - 128, oz + 4, f, mc("lava"));
        c.pool(ox - 68, oz + 26, ox - 58, oz + 32, f, mc("lava"));
        for (int x = ox - 112; x <= ox - 12; x += 20) {
            c.set(x, f + 1, oz - 1, mc("chiseled_deepslate")).set(x, f + 1, oz + 1, mc("chiseled_deepslate"));
        }
        // The 72 Agility tunnel from the entrance to the greater demons at the far end (approx. ends).
        c.shortcut("stronghold_slayer_tunnel", ox + 6, oz + 6, f, ox - 122, oz + 20, f);
        c.entrance(ox, oz, f, mc("mossy_stone_bricks"), mc("cobblestone"), 's');
        c.camp(ox - 41, oz - 33, ox - 19, oz - 13, f, 4, 25, "aberrant_spectre");
        c.camp(ox - 41, oz + 13, ox - 19, oz + 33, f, 4, 25, "ankou");
        c.camp(ox - 73, oz - 33, ox - 51, oz - 13, f, 4, 25, "bloodveld");
        c.camp(ox - 73, oz + 13, ox - 51, oz + 24, f, 4, 25, "fire_giant");
        c.camp(ox - 105, oz - 33, ox - 83, oz - 13, f, 4, 25, "hellhound");
        c.camp(ox - 105, oz + 13, ox - 83, oz + 33, f, 3, 50, "black_demon");
        c.camp(ox - 145, oz - 21, ox - 121, oz + 21, f, 4, 50, "greater_demon");
        c.camp(ox - 15, oz - 89, ox + 15, oz - 61, f, 4, 25, "nechryael");
        c.camp(ox + 41, oz - 95, ox + 71, oz - 55, f, 5, 25, "abyssal_demon");
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
