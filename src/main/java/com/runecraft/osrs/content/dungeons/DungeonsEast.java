package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Morytania and the northern isles: the Slayer Tower basement (carved under the tower the world
 * module built, entered from outside it), the Fossil Island Wyvern Cave and the Lithkren Vault.
 */
final class DungeonsEast {
    private DungeonsEast() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(slayerTowerBasement());
        out.add(wyvernCave());
        out.add(lithkrenVault());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /**
     * The Slayer Tower basement, below the tower (the building itself is the world module's and is
     * left untouched; the way down is a hatch just south of it, tile 3428, 3520): a pillared crypt
     * with aberrant spectres, nechryael and abyssal demons (approx. - which creatures roam the
     * basement is simplified).
     */
    static Dungeons.Dungeon slayerTowerBasement() {
        Cave c = new Cave("dng_slayer_tower_basement", "Slayer Tower basement", mc("polished_blackstone_bricks"),
                mc("cracked_polished_blackstone_bricks"), mc("shroomlight"));
        int f = 40;
        int ex = bx(3428);
        int ez = bz(3520);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hallZ(ex, ez - 5, ex, ez - 30, f, 3, 4);
        int x1 = bx(3405);
        int x2 = bx(3438);
        int z1 = bz(3562);
        int z2 = ez - 30;
        c.room(x1, z1, x2, z2, f, 6);
        c.carve();
        for (int x = x1 + 6; x < x2 - 2; x += 10) {
            for (int z = z1 + 6; z < z2 - 2; z += 10) {
                c.pillar(x, z, f, 6, mc("chiseled_polished_blackstone"));
            }
        }
        int mid = (x1 + x2) / 2;
        c.b.walls(mid - 1, f + 1, z1, mid + 1, f + 6, z2 - 12, c.wall);
        c.entrance(ex, ez, f, mc("polished_blackstone_bricks"), mc("polished_blackstone"), 's');
        c.camp(x1 + 2, z1 + 2, mid - 3, z2 - 2, f, 5, 25, "aberrant_spectre", "nechryael");
        c.camp(mid + 3, z1 + 2, x2 - 2, z2 - 2, f, 4, 25, "abyssal_demon", "nechryael");
        return c.build();
    }

    /**
     * The Wyvern Cave on Fossil Island (entrance tile 3677, 3854): an ash-strewn cavern with
     * spitting and taloned wyverns (the long-tailed and ancient wyverns are not in the game yet).
     */
    static Dungeons.Dungeon wyvernCave() {
        Cave c = new Cave("dng_wyvern_cave", "Wyvern Cave", mc("tuff"), mc("smooth_basalt"), mc("shroomlight"));
        int f = 14;
        int ox = bx(3677);
        int oz = bz(3854);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 20, oz, f, 3, 5);
        c.room(ox + 20, oz - 30, ox + 64, oz + 24, f, 10);
        c.hallZ(ox + 42, oz + 24, ox + 42, oz + 36, f, 3, 5);
        c.room(ox + 20, oz + 36, ox + 64, oz + 70, f, 10);
        c.carve();
        c.pool(ox + 36, oz - 6, ox + 48, oz + 2, f, mc("lava"));
        for (int x = ox + 26; x <= ox + 58; x += 16) {
            c.pillar(x, oz - 22, f, 10, mc("basalt[axis=y]"));
            c.pillar(x, oz + 62, f, 10, mc("basalt[axis=y]"));
        }
        c.entrance(ox, oz, f, mc("tuff"), mc("smooth_basalt"), 'w');
        c.camp(ox + 22, oz - 28, ox + 62, oz + 22, f, 4, 50, "spitting_wyvern");
        c.camp(ox + 22, oz + 38, ox + 62, oz + 68, f, 4, 50, "taloned_wyvern");
        return c.build();
    }

    /**
     * The Lithkren Vault beneath the ruined Dragonkin laboratory on Lithkren (entrance tile 3550,
     * 3998): the vault's stair, the adamant dragons' hall and the rune dragons' hall.
     */
    static Dungeons.Dungeon lithkrenVault() {
        Cave c = new Cave("dng_lithkren_vault", "Lithkren Vault", mc("deepslate_tiles"), mc("polished_deepslate"), mc("sea_lantern"));
        int f = 0;
        int ox = bx(3550);
        int oz = bz(3998);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 6);
        c.hall(ox - 6, oz, ox - 20, oz, f, 3, 5);
        c.room(ox - 60, oz - 22, ox - 20, oz + 22, f, 10);              // adamant dragons
        c.hall(ox + 6, oz, ox + 20, oz, f, 3, 5);
        c.room(ox + 20, oz - 22, ox + 60, oz + 22, f, 10);              // rune dragons
        c.carve();
        for (int[] p : new int[][]{{-52, -14}, {-28, -14}, {-52, 14}, {-28, 14}, {28, -14}, {52, -14}, {28, 14}, {52, 14}}) {
            c.pillar(ox + p[0], oz + p[1], f, 10, mc("chiseled_deepslate"));
        }
        c.fill(ox - 1, f + 1, oz - 1, ox + 1, f + 1, oz + 1, mc("lodestone"));
        c.entrance(ox + 4, oz + 4, f, mc("deepslate_bricks"), mc("polished_deepslate"), 's');
        c.camp(ox - 58, oz - 20, ox - 22, oz + 20, f, 3, 50, "adamant_dragon");
        c.camp(ox + 22, oz - 20, ox + 58, oz + 20, f, 3, 50, "rune_dragon");
        return c.build();
    }
}
