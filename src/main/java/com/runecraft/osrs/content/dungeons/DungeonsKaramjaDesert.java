package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Karamja, the desert and the eastern isles: the Brimhaven Dungeon, the TzHaar city below the
 * Karamja volcano, the Kalphite Lair, the Smoke Dungeon by Pollnivneach and the Mos Le'Harmless
 * caves.
 */
final class DungeonsKaramjaDesert {
    private DungeonsKaramjaDesert() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(brimhaven());
        out.add(tzhaarCity());
        out.add(kalphiteLair());
        out.add(smokeDungeon());
        out.add(mosLeHarmlessCaves());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    // ------------------------------------------------------------------ Brimhaven Dungeon

    /**
     * The Brimhaven Dungeon, south of Brimhaven (entrance tile 2744, 3132): vines to hack through,
     * the moss giants' cavern, stepping stones across the river, wild dogs and greater demons,
     * black demons, the red dragons' lair and the bronze, iron and steel dragons in the far east, and
     * the baby green dragons' cave out west past more vines (OSRS 2657-2677, 9570-9582: west and a
     * little north of the way in).
     */
    static Dungeons.Dungeon brimhaven() {
        Cave c = new Cave("dng_brimhaven_dungeon", "Brimhaven Dungeon", mc("mossy_cobblestone"), mc("moss_block"), mc("glowstone"));
        int f = 24;
        int ox = bx(2744);
        int oz = bz(3132);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hallZ(ox, oz + 6, ox, oz + 14, f, 3, 4);
        c.room(ox - 40, oz + 14, ox + 4, oz + 40, f, 7);                  // moss giants
        c.hall(ox + 4, oz + 27, ox + 30, oz + 27, f, 5, 5);               // the river crossing
        c.room(ox + 30, oz + 10, ox + 60, oz + 44, f, 8);                 // wild dogs, greater demons
        c.hallZ(ox + 45, oz + 44, ox + 45, oz + 50, f, 3, 4);
        c.room(ox + 30, oz + 50, ox + 64, oz + 80, f, 8);                 // black demons
        c.hall(ox + 30, oz + 65, ox + 20, oz + 65, f, 3, 4);
        c.room(ox - 24, oz + 50, ox + 20, oz + 96, f, 9);                 // red dragons
        c.hall(ox + 60, oz + 20, ox + 70, oz + 20, f, 3, 4);
        c.room(ox + 70, oz + 2, ox + 96, oz + 26, f, 8);                  // bronze dragons
        c.hallZ(ox + 83, oz + 26, ox + 83, oz + 32, f, 3, 4);
        c.room(ox + 70, oz + 32, ox + 96, oz + 56, f, 8);                 // iron dragons
        c.hallZ(ox + 83, oz + 56, ox + 83, oz + 62, f, 3, 4);
        c.room(ox + 70, oz + 62, ox + 96, oz + 86, f, 8);                 // steel dragons
        c.hall(ox - 6, oz, bx(2712), oz, f, 3, 4);
        c.room(bx(2690), bz(3144), bx(2712) + 1, bz(3128) + 1, f, 7);     // baby green dragons
        c.carve();
        // Vines blocking the way in (and before the dragons), hacked through with an axe.
        c.barrier(ox - 1, oz + 10, ox + 1, oz + 10, f, 4, ox, oz + 10, rc(DungeonBlocks.VINES));
        c.barrier(ox + 65, oz + 19, ox + 65, oz + 21, f, 4, ox + 65, oz + 20, rc(DungeonBlocks.VINES));
        c.barrier(ox + 25, oz + 64, ox + 25, oz + 66, f, 4, ox + 25, oz + 65, rc(DungeonBlocks.VINES));
        c.barrier(bx(2720), oz - 1, bx(2720), oz + 1, f, 4, bx(2720), oz, rc(DungeonBlocks.VINES));
        for (int x = bx(2692); x <= bx(2710); x += 6) {
            c.set(x, f + 1, bz(3143), mc("fern")).set(x + 2, f + 1, bz(3129), mc("large_fern[half=lower]"));
        }
        // The river with its stepping stones.
        c.pool(ox + 10, oz + 25, ox + 24, oz + 29, f, mc("water"));
        for (int x = ox + 11; x <= ox + 23; x += 3) {
            c.set(x, f, oz + 27, mc("mossy_stone_bricks"));
        }
        // Jungle growth.
        for (int x = ox - 36; x <= ox; x += 9) {
            c.set(x, f + 1, oz + 16, mc("fern")).set(x + 3, f + 1, oz + 38, mc("large_fern[half=lower]"));
        }
        c.pool(ox - 16, oz + 80, ox - 8, oz + 90, f, mc("lava"));
        c.entrance(ox, oz, f, mc("mossy_cobblestone"), mc("moss_block"), 'n');
        c.camp(ox - 38, oz + 16, ox + 2, oz + 38, f, 5, 50, "moss_giant");
        c.camp(ox + 32, oz + 12, ox + 58, oz + 42, f, 5, 25, "wild_dog", "greater_demon");
        c.camp(ox + 32, oz + 52, ox + 62, oz + 78, f, 4, 50, "black_demon");
        c.camp(ox - 22, oz + 52, ox + 18, oz + 78, f, 4, 50, "red_dragon");
        c.camp(ox + 72, oz + 4, ox + 94, oz + 24, f, 2, 50, "bronze_dragon");
        c.camp(ox + 72, oz + 34, ox + 94, oz + 54, f, 2, 50, "iron_dragon");
        c.camp(ox + 72, oz + 64, ox + 94, oz + 84, f, 2, 50, "steel_dragon");
        c.camp(bx(2691), bz(3143), bx(2712) - 1, bz(3129), f, 4, 35, "baby_green_dragon");
        return c.build();
    }

    // ------------------------------------------------------------------ TzHaar city

    /**
     * Mor Ul Rek / TzHaar city below the Karamja volcano, reached by a shaft west of the volcano's
     * foot (tile 2820, 3166; the surface gate to the Fight Caves and Inferno is the minigames
     * module's): a vast obsidian cavern with lava rivers, the TzHaar homes, the market and the Fight
     * Pit ring, peopled by TzHaar-Hur, -Ket, -Xil and -Mej.
     */
    static Dungeons.Dungeon tzhaarCity() {
        Cave c = new Cave("dng_tzhaar_city", "TzHaar city", mc("blackstone"), mc("polished_blackstone"), mc("shroomlight"));
        int f = 6;
        int ex = bx(2820);
        int ez = bz(3166);
        c.room(ex - 8, ez - 8, ex + 8, ez + 8, f, 6);
        c.hall(ex + 8, ez, ex + 14, ez, f, 5, 6);
        int x1 = ex + 14;
        int x2 = ex + 154;
        int z1 = ez - 54;
        int z2 = ez + 56;
        c.room(x1, z1, x2, z2, f, 18);
        c.carve();
        // Lava rivers.
        c.pool(x1 + 1, z1 + 30, x2 - 1, z1 + 33, f, mc("lava"));
        c.pool(x1 + 70, z1 + 1, x1 + 73, z2 - 1, f, mc("lava"));
        for (int x = x1 + 8; x < x2; x += 24) {
            c.fill(x, f, z1 + 30, x + 2, f, z1 + 33, mc("obsidian"));
        }
        for (int z = z1 + 8; z < z2; z += 24) {
            c.fill(x1 + 70, f, z, x1 + 73, f, z + 2, mc("obsidian"));
        }
        // TzHaar homes: domed obsidian huts.
        for (int hx = x1 + 6; hx < x2 - 10; hx += 20) {
            for (int hz = z1 + 40; hz < z2 - 8; hz += 18) {
                if (hx > x1 + 60 && hx < x1 + 80) {
                    continue;
                }
                c.b.walls(hx, f + 1, hz, hx + 8, f + 4, hz + 6, mc("obsidian"));
                c.fill(hx, f + 5, hz, hx + 8, f + 5, hz + 6, mc("basalt[axis=y]"));
                c.b.clear(hx + 4, f + 1, hz + 6, hx + 4, f + 2, hz + 6);
                c.set(hx + 4, f + 4, hz + 3, mc("magma_block"));
            }
        }
        // The Fight Pit: a sunken ring of obsidian north of the river.
        int px = x1 + 110;
        int pz = z1 + 14;
        c.b.walls(px - 12, f + 1, pz - 10, px + 12, f + 2, pz + 10, mc("obsidian"));
        c.fill(px - 11, f, pz - 9, px + 11, f, pz + 9, mc("polished_blackstone_bricks"));
        c.b.clear(px, f + 1, pz + 10, px + 1, f + 2, pz + 10);
        // The market: stalls and the bank by the entrance.
        int mx = x1 + 20;
        int mz = z1 + 14;
        c.set(mx, f + 1, mz, rc("tzhaar_store")).set(mx + 6, f + 1, mz, rc("bank_booth"));
        c.fill(mx - 2, f + 4, mz - 2, mx + 8, f + 4, mz + 2, mc("polished_blackstone_slab[type=bottom]"));
        c.entrance(ex, ez, f, mc("polished_blackstone_bricks"), mc("obsidian"), 'w');
        c.camp(x1 + 4, z1 + 36, x1 + 66, z2 - 4, f, 6, 25, "tzhaar_hur_citizen", "tzhaar_hur_citizen", "tzhaar_mej");
        c.camp(x1 + 78, z1 + 36, x2 - 4, z2 - 4, f, 6, 25, "tzhaar_hur_citizen", "tzhaar_ket", "tzhaar_xil");
        c.camp(x1 + 4, z1 + 4, x1 + 66, z1 + 28, f, 4, 25, "tzhaar_ket", "tzhaar_hur_citizen");
        return c.build();
    }

    // ------------------------------------------------------------------ Kalphite Lair

    /**
     * The Kalphite Lair below the crevice in the desert south of the Shantay Pass (tile 3227, 3108):
     * the worker tunnels, the soldiers' and guardians' chambers and the Queen's chamber, whose rope
     * climbs back up beside the Kalphite Queen's arena (the bosses module places the Queen there).
     */
    static Dungeons.Dungeon kalphiteLair() {
        Cave c = new Cave("dng_kalphite_lair", "Kalphite Lair", mc("smooth_sandstone"), mc("sand"), mc("ochre_froglight"));
        int f = 18;
        int lx = bx(3227) + 2;
        int lz = bz(3108) + 1;
        c.room(lx - 10, lz - 10, lx + 10, lz + 10, f, 5);                 // workers
        c.hall(lx + 10, lz, lx + 28, lz + 20, f, 3, 4);
        c.room(lx + 16, lz + 20, lx + 44, lz + 44, f, 6);                 // soldiers
        c.hall(lx + 44, lz + 32, lx + 62, lz + 32, f, 3, 5);
        c.room(lx + 62, lz + 16, lx + 90, lz + 48, f, 7);                 // guardians
        c.hallZ(lx + 76, lz + 16, lx + 76, lz - 2, f, 3, 5);
        c.room(lx + 60, lz - 30, lx + 96, lz - 2, f, 9);                  // the Queen's chamber
        c.carve();
        for (int[] p : new int[][]{{-6, -6}, {6, 6}, {24, 26}, {38, 38}, {70, 22}, {84, 42}}) {
            c.pillar(lx + p[0], lz + p[1], f, 5, mc("sandstone"));
        }
        c.fill(lx + 70, f, lz - 24, lx + 86, f, lz - 8, mc("soul_sand"));
        c.set(lx + 78, f + 1, lz - 16, mc("bone_block")).set(lx + 79, f + 1, lz - 15, mc("bone_block"));
        // The way up to the Queen's arena (centre 27 blocks east of the lair's place, radius 13).
        c.entrance(lx + 62, lz - 6, f, mc("sandstone"), mc("smooth_sandstone"), 'e');
        c.link(lx, lz, 55, f, "south");
        c.camp(lx - 8, lz - 8, lx + 8, lz + 8, f, 4, 25, "kalphite_worker");
        c.camp(lx + 18, lz + 22, lx + 42, lz + 42, f, 4, 25, "kalphite_soldier", "kalphite_worker");
        c.camp(lx + 64, lz + 18, lx + 88, lz + 46, f, 3, 25, "kalphite_guardian", "kalphite_soldier");
        return c.build();
    }

    // ------------------------------------------------------------------ Smoke Dungeon

    /**
     * The Smoke Dungeon by Pollnivneach (entrance tile 3302, 2958): smoky tunnels with dust devils,
     * pyrefiends and fire giants (bring a facemask), and Fareed's chamber (the Desert Treasure boss
     * is fought at its arena outside).
     */
    static Dungeons.Dungeon smokeDungeon() {
        Cave c = new Cave("dng_smoke_dungeon", "Smoke Dungeon", mc("sandstone"), mc("smooth_sandstone"), mc("shroomlight"));
        int f = 24;
        int ox = bx(3302);
        int oz = bz(2958);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hallZ(ox, oz + 6, ox, oz + 20, f, 3, 4);
        c.room(ox - 30, oz + 20, ox + 30, oz + 50, f, 6);                 // dust devils
        c.hall(ox + 30, oz + 35, ox + 46, oz + 35, f, 3, 4);
        c.room(ox + 46, oz + 20, ox + 76, oz + 50, f, 8);                 // fire giants and pyrefiends
        c.hallZ(ox - 20, oz + 50, ox - 20, oz + 62, f, 3, 4);
        c.room(ox - 34, oz + 62, ox - 6, oz + 82, f, 6);                  // Fareed's chamber
        c.carve();
        for (int[] p : new int[][]{{-26, 24}, {26, 24}, {-26, 46}, {26, 46}, {0, 35}, {60, 24}, {60, 46}, {-20, 72}}) {
            c.set(ox + p[0], f, oz + p[1], mc("hay_block"));
            c.set(ox + p[0], f + 1, oz + p[1], mc("campfire[lit=true,signal_fire=true]"));
        }
        c.pool(ox - 26, oz + 66, ox - 14, oz + 78, f, mc("lava"));
        c.entrance(ox, oz, f, mc("cut_sandstone"), mc("smooth_sandstone"), 'w');
        c.camp(ox - 28, oz + 22, ox + 28, oz + 48, f, 6, 25, "dust_devil");
        c.camp(ox + 48, oz + 22, ox + 74, oz + 48, f, 5, 25, "fire_giant", "pyrefiend");
        return c.build();
    }

    // ------------------------------------------------------------------ Mos Le'Harmless caves

    /**
     * The Mos Le'Harmless caves (entrance tile 3748, 2973): dark, winding tunnels where the cave
     * horrors live (bring a witchwood icon), opening into the great cavern of the Cave Horror's den.
     */
    static Dungeons.Dungeon mosLeHarmlessCaves() {
        Cave c = new Cave("dng_mos_le_harmless_caves", "Mos Le'Harmless caves", mc("mud_bricks"), mc("packed_mud"), null);
        int f = 20;
        int ox = bx(3748);
        int oz = bz(2973);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 30, oz + 24, f, 3, 4);
        c.room(ox + 18, oz + 24, ox + 50, oz + 50, f, 6);
        c.hall(ox + 18, oz + 37, ox - 20, oz + 60, f, 3, 4);
        c.room(ox - 50, oz + 60, ox + 10, oz + 100, f, 8);
        c.hallZ(ox, oz - 6, ox, oz - 30, f, 3, 4);
        c.room(ox - 20, oz - 56, ox + 20, oz - 30, f, 6);
        c.carve();
        for (int x = ox - 44; x <= ox + 4; x += 12) {
            c.pillar(x, oz + 80, f, 8, mc("mud_bricks"));
            c.set(x + 3, f + 1, oz + 64, mc("glow_lichen[down=true]"));
        }
        c.pool(ox - 30, oz + 88, ox - 18, oz + 96, f, mc("water"));
        c.entrance(ox, oz, f, mc("mud_bricks"), mc("packed_mud"), 'e');
        c.camp(ox + 20, oz + 26, ox + 48, oz + 48, f, 4, 25, "cave_horror");
        c.camp(ox - 48, oz + 62, ox + 8, oz + 98, f, 6, 25, "cave_horror");
        c.camp(ox - 18, oz - 54, ox + 18, oz - 32, f, 3, 25, "cave_horror");
        return c.build();
    }
}
