package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Zeah's dungeons (the zeah module carves only the Catacombs of Kourend): the Lizardman Caves, the
 * Karuulm Slayer Dungeon inside Mount Karuulm and the Chasm of Fire.
 */
final class DungeonsZeah {
    private DungeonsZeah() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(lizardmanCaves());
        out.add(karuulm());
        out.add(chasmOfFire());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /**
     * The Lizardman Caves below the Lizardman Settlement (entrance tile 1309, 3574): swampy
     * tunnels and pools where lizardman shamans gather.
     */
    static Dungeons.Dungeon lizardmanCaves() {
        Cave c = new Cave("dng_lizardman_caves", "Lizardman Caves", mc("mossy_cobblestone"), mc("mud"), mc("ochre_froglight"));
        int f = 18;
        int ox = bx(1309);
        int oz = bz(3574);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox - 6, oz, ox - 24, oz - 14, f, 3, 4);
        c.room(ox - 56, oz - 50, ox - 14, oz - 14, f, 6);
        c.hall(ox + 6, oz, ox + 24, oz - 14, f, 3, 4);
        c.room(ox + 12, oz - 46, ox + 50, oz - 14, f, 6);
        c.carve();
        c.pool(ox - 44, oz - 40, ox - 30, oz - 28, f, mc("water"));
        c.pool(ox + 24, oz - 36, ox + 36, oz - 26, f, mc("water"));
        for (int x = ox - 52; x <= ox + 46; x += 14) {
            c.set(x, f + 1, oz - 16, mc("big_dripleaf"));
        }
        c.entrance(ox, oz, f, mc("mossy_cobblestone"), mc("mud"), 's');
        c.camp(ox - 54, oz - 48, ox - 16, oz - 16, f, 5, 25, "lizardman_shaman");
        c.camp(ox + 14, oz - 44, ox + 48, oz - 16, f, 5, 25, "lizardman_shaman");
        return c.build();
    }

    /**
     * The Karuulm Slayer Dungeon inside Mount Karuulm (entrance at the mountain's western foot, tile
     * 1299, 3806 - the Alchemical Hydra's arena is the bosses module's, on the mountain): the wyrms'
     * hall on the first level and, below, the drakes' terrace and the hydras' lava cavern.
     */
    static Dungeons.Dungeon karuulm() {
        Cave c = new Cave("dng_karuulm_slayer_dungeon", "Karuulm Slayer Dungeon", mc("blackstone"), mc("basalt[axis=y]"),
                mc("shroomlight"));
        int f = 30;
        int f2 = 12;
        int ox = bx(1299);
        int oz = bz(3806);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 20, oz, f, 3, 5);
        c.room(ox + 20, oz - 30, ox + 66, oz + 26, f, 9);                // wyrms
        c.stairs(ox + 43, oz + 26, 's', f, f2, 3, 5);
        int lz = oz + 26 + f - f2 + 1;
        c.room(ox + 20, lz, ox + 66, lz + 36, f2, 9);                   // drakes
        c.hall(ox + 66, lz + 18, ox + 80, lz + 18, f2, 3, 5);
        c.room(ox + 80, lz - 4, ox + 120, lz + 40, f2, 10);             // hydras
        c.carve();
        c.pool(ox + 90, lz + 4, ox + 98, lz + 12, f2, mc("lava"));
        c.pool(ox + 104, lz + 24, ox + 112, lz + 34, f2, mc("lava"));
        c.pool(ox + 36, oz - 20, ox + 50, oz - 12, f, mc("lava"));
        for (int x = ox + 26; x <= ox + 60; x += 17) {
            c.pillar(x, oz + 18, f, 9, mc("polished_basalt[axis=y]"));
            c.pillar(x, lz + 30, f2, 9, mc("polished_basalt[axis=y]"));
        }
        c.entrance(ox, oz, f, mc("polished_blackstone_bricks"), mc("blackstone"), 'w');
        c.camp(ox + 22, oz - 28, ox + 64, oz + 16, f, 5, 25, "wyrm");
        c.camp(ox + 22, lz + 2, ox + 64, lz + 34, f2, 4, 25, "drake");
        c.camp(ox + 82, lz - 2, ox + 118, lz + 38, f2, 4, 25, "hydra");
        return c.build();
    }

    /**
     * The Chasm of Fire north-west of Shayzien (entrance tile 1433, 3671): a great shaft with three
     * ledges spiralling down - lesser demons, then greater demons, then black demons at the bottom -
     * joined by ladders.
     */
    static Dungeons.Dungeon chasmOfFire() {
        Cave c = new Cave("dng_chasm_of_fire", "Chasm of Fire", mc("blackstone"), mc("polished_blackstone"), mc("shroomlight"));
        int ox = bx(1433);
        int oz = bz(3671);
        int cx = ox;
        int cz = oz + 40;
        int[] floors = {30, 10, -10};
        for (int f : floors) {
            c.disc(cx, cz, 30, f, 8);
        }
        c.hallZ(ox, oz, ox, cz - 30, floors[0], 3, 5);
        c.room(ox - 4, oz - 4, ox + 4, oz + 4, floors[0], 5);
        c.carve();
        // A lava pit in the middle of each ledge (the chasm), ringed by blackstone.
        for (int f : floors) {
            for (int dz = -8; dz <= 8; dz++) {
                int half = (int) Math.floor(Math.sqrt(8.5 * 8.5 - dz * dz));
                c.pool(cx - half, cz + dz, cx + half, cz + dz, f, mc("lava"));
            }
        }
        c.ladderUp(cx + 20, cz, floors[1], floors[0]);
        c.ladderUp(cx - 20, cz, floors[2], floors[1]);
        c.entrance(ox, oz, floors[0], mc("polished_blackstone_bricks"), mc("blackstone"), 'n');
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[0], 5, 25, "lesser_demon");
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[1], 4, 25, "greater_demon");
        c.camp(cx - 20, cz - 20, cx + 20, cz + 20, floors[2], 3, 50, "black_demon");
        return c.build();
    }
}
