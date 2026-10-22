package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Asgarnia's dungeons: Taverley Dungeon, the Dwarven Mine (entered from Ice Mountain and Falador)
 * with the Mining Guild and the Motherlode Mine beside it, and the Asgarnian Ice Dungeon below
 * Mudskipper Point.
 */
final class DungeonsAsgarnia {
    private DungeonsAsgarnia() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(taverley());
        out.add(dwarvenMine());
        out.add(iceDungeon());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    // ------------------------------------------------------------------ Taverley Dungeon

    /**
     * Taverley Dungeon, down the ladder south of Taverley (tile 2878, 3398 - the real one is a few
     * tiles east, where Cerberus' lair now stands): skeletons and ghosts at the entrance, the chaos
     * druids, the blue dragons behind the dusty-key gate to the north-east, baby blue dragons to the
     * east, the caged lesser demons, the hellhounds by the way to Cerberus in the north-west, the
     * black demons' cavern to the south-west and the poison spiders' pit to the south-east. Past the
     * hellhounds lies the far west: the two black dragons' cave (OSRS 2829-2835, 9824-9826). The upper level
     * (OSRS plane 1, on its tiles: dungeon y - 6400), where the dragons can only be fought on a Slayer task: up the steps
     * north of the entrance, Slieve's black dragon side with the five baby black dragons (2817-2862, 9818-9828) and the
     * twelve black dragons (2774-2807, 9803-9841); up the steps east of the blue dragons, Eve's twelve blue dragons
     * (2889-2955, 9804-9827). The main level's rooms and the steps' positions are approx.
     */
    /** Floor of Taverley Dungeon's upper level (OSRS plane 1), above every main-level cavern. */
    static final int TAVERLEY_UPPER = 32;
    // Cache spawn tiles of the upper level (mejrs/data_osrs plane 1; the blue dragons are the wiki's 12 LocLine tiles).
    static final int[] TAVERLEY_BABY_BLACKS = {2817, 9818, 2820, 9819, 2824, 9818, 2858, 9828, 2862, 9826};
    static final int[] TAVERLEY_UPPER_BLACKS = {2775, 9834, 2785, 9808, 2798, 9809, 2798, 9828, 2774, 9812, 2783, 9838, 2785, 9823,
            2799, 9818, 2775, 9803, 2777, 9826, 2796, 9841, 2807, 9814};
    static final int[] TAVERLEY_UPPER_BLUES = {2889, 9804, 2891, 9810, 2896, 9813, 2917, 9812, 2922, 9809, 2926, 9812, 2942, 9818,
            2944, 9809, 2946, 9824, 2951, 9814, 2953, 9827, 2955, 9821};

    static Dungeons.Dungeon taverley() {
        Cave c = new Cave("dng_taverley_dungeon", "Taverley Dungeon", mc("stone_bricks"), mc("cobblestone"), mc("glowstone"));
        int f = 20;
        int ox = bx(2872);
        int oz = bz(3398);
        c.room(ox - 6, oz - 6, ox + 6, oz + 6, f, 5);
        c.hall(ox + 6, oz, ox + 30, oz, f, 3, 4);
        c.room(ox + 30, oz - 8, ox + 44, oz + 8, f, 6);                 // chaos druids
        c.hallZ(ox + 37, oz - 8, ox + 37, oz - 30, f, 3, 5);
        c.room(ox + 24, oz - 62, ox + 60, oz - 30, f, 9);              // blue dragons
        c.hall(ox + 44, oz, ox + 60, oz, f, 3, 4);
        c.room(ox + 60, oz - 10, ox + 78, oz + 10, f, 6);              // baby blue dragons
        c.hallZ(ox, oz - 6, ox, oz - 26, f, 3, 4);
        c.room(ox - 10, oz - 42, ox + 10, oz - 26, f, 6);              // lesser demons' cage
        c.hall(ox - 6, oz, ox - 30, oz - 30, f, 3, 4);
        c.room(ox - 54, oz - 62, ox - 18, oz - 30, f, 7);              // hellhounds
        c.hallZ(ox, oz + 6, ox, oz + 30, f, 3, 4);
        c.room(ox - 44, oz + 30, ox - 4, oz + 64, f, 9);               // black demons
        c.hallZ(ox + 37, oz + 8, ox + 37, oz + 36, f, 3, 4);
        c.room(ox + 22, oz + 36, ox + 50, oz + 60, f, 5);              // poison spiders
        // The far west: the two black dragons' cave.
        int bw = bx(2818);
        c.hall(ox - 54, oz - 46, bx(2836) + 1, oz - 46, f, 3, 4);
        c.room(bw, bz(3427), bx(2836) + 1, bz(3411) + 1, f, 6);         // black dragons
        // The upper level (task-only), on the OSRS tiles.
        CacheDungeons.Shifted up = new CacheDungeons.Shifted(c, 0, -6400);
        int top = TAVERLEY_UPPER;
        int[] slieve = {2872, 9819, 2884, 9830};
        int[] babies = {2814, 9814, 2866, 9832};
        int[] blacks = {2770, 9800, 2810, 9844};
        int[] blues = {2886, 9800, 2959, 9830};
        int[][] upper = {slieve, babies, blacks, blues};
        for (int[] r : upper) {
            up.room(r, top, 8);
        }
        up.hall(2866, 9825, 2872, 9825, top);
        up.hall(2810, 9820, 2814, 9820, top);
        // The black dragons' steps: north of the entrance (the main hall's branch) up to Slieve's landing.
        c.hallZ(ox + 16, oz, ox + 16, oz - 28, f, 3, 4);
        c.stairs(ox + 16, oz - 28, 'n', f, top, 3, 5);
        // The blue dragons' steps: east of the blue dragons' cavern, under the upper level, up to Eve's cavern.
        c.hall(ox + 60, bz(3415), bx(2966), bz(3415), f, 3, 4);
        c.stairs(bx(2966), bz(3415), 'w', f, top, 3, 5);
        c.carve();
        // The dusty-key gate before the blue dragons.
        gate(c, ox + 37, oz - 20, f, 'z');
        // The Cauldron of Thunder (Druidic Ritual) in the corridor before the gate, OSRS tile (2890, 9806) here. // approx
        c.set(ox + 38, f + 1, oz - 16, mc("water_cauldron[level=3]"));
        // The 70 Agility obstacle pipe from the chaos druids into the blue dragons' cavern, past the gate.
        c.shortcut("taverley_dungeon_pipe", ox + 42, oz - 7, f, ox + 42, oz - 31, f);
        // The lesser demons' cage (iron bars).
        c.b.walls(ox - 6, f + 1, oz - 38, ox + 6, f + 5, oz - 30, mc("iron_bars"));
        c.b.clear(ox - 5, f + 1, oz - 37, ox + 5, f + 5, oz - 31);
        // Pillars and lava in the black demons' cavern, cobwebs in the spider pit.
        for (int x = ox - 38; x <= ox - 10; x += 14) {
            for (int z = oz + 36; z <= oz + 58; z += 11) {
                c.pillar(x, z, f, 9, mc("chiseled_stone_bricks"));
            }
        }
        c.pool(ox - 30, oz + 52, ox - 22, oz + 60, f, mc("lava"));
        for (int x = ox + 24; x <= ox + 48; x += 6) {
            c.set(x, f + 5, oz + 37, mc("cobweb")).set(x + 2, f + 1, oz + 59, mc("cobweb"));
        }
        c.set(ox - 5, f + 1, oz - 5, mc("skeleton_skull[rotation=3]"));
        for (int x = bx(2780); x < bx(2806); x += 16) {
            c.pillar(x, bz(3434), top, 8, mc("chiseled_stone_bricks"));
            c.pillar(x, bz(3410), top, 8, mc("chiseled_stone_bricks"));
        }
        c.set(bx(2777), top + 1, bz(3405), mc("bone_block[axis=y]")).set(bx(2778), top + 1, bz(3405), mc("skeleton_skull"));
        // Blue dragon eggs in Eve's cavern.
        c.set(bx(2950), top + 1, bz(3405), mc("sniffer_egg")).set(bx(2952), top + 1, bz(3406), mc("sniffer_egg"));
        c.entrance(ox, oz, f, mc("mossy_stone_bricks"), mc("cobblestone"), 'w');
        c.camp(ox - 5, oz - 5, ox + 5, oz + 5, f, 2, 25, "minecraft:skeleton", "ghost");
        c.camp(ox + 31, oz - 7, ox + 43, oz + 7, f, 4, 25, "chaos_druid");
        c.camp(ox + 26, oz - 60, ox + 58, oz - 32, f, 4, 50, "blue_dragon");
        c.camp(ox + 62, oz - 8, ox + 76, oz + 8, f, 3, 25, "baby_blue_dragon");
        c.camp(ox - 5, oz - 37, ox + 5, oz - 31, f, 3, 50, "lesser_demon");
        c.camp(ox - 52, oz - 60, ox - 20, oz - 32, f, 5, 25, "hellhound");
        c.camp(ox - 42, oz + 32, ox - 6, oz + 62, f, 4, 50, "black_demon");
        c.camp(ox + 24, oz + 38, ox + 48, oz + 58, f, 5, 25, "poison_spider");
        c.camp(bw + 2, bz(3426), bx(2836) - 1, bz(3412), f, 2, 50, "black_dragon");
        up.camps(upper, top, CacheDungeons.RESPAWN, "baby_black_dragon", TAVERLEY_BABY_BLACKS);
        up.camps(upper, top, CacheDungeons.DRAGON_RESPAWN, "black_dragon", TAVERLEY_UPPER_BLACKS);
        up.camps(upper, top, CacheDungeons.DRAGON_RESPAWN, "blue_dragon", TAVERLEY_UPPER_BLUES);
        up.taskOnly(slieve, top, 8, "black_dragons", "Slieve");
        up.taskOnly(babies, top, 8, "black_dragons", "Slieve");
        up.taskOnly(blacks, top, 8, "black_dragons", "Slieve");
        up.taskOnly(blues, top, 8, "blue_dragons", "Eve");
        return c.build();
    }

    /** A gate across a 3-wide corridor running along x ('x') or z ('z'): a fence gate between walls. */
    static void gate(Cave c, int x, int z, int f, char along) {
        if (along == 'z') {
            c.fill(x - 1, f + 1, z, x - 1, f + 4, z, c.wall).fill(x + 1, f + 1, z, x + 1, f + 4, z, c.wall);
            c.fill(x, f + 3, z, x, f + 4, z, c.wall);
            c.set(x, f + 1, z, mc("oak_fence_gate[facing=north]"));
        } else {
            c.fill(x, f + 1, z - 1, x, f + 4, z - 1, c.wall).fill(x, f + 1, z + 1, x, f + 4, z + 1, c.wall);
            c.fill(x, f + 3, z, x, f + 4, z, c.wall);
            c.set(x, f + 1, z, mc("oak_fence_gate[facing=east]"));
        }
    }

    // ------------------------------------------------------------------ Dwarven Mine

    /**
     * The Dwarven Mine between Ice Mountain and Falador: the ladder down from the Ice Mountain hut
     * and the stairs from the Falador house, the great mine cavern with copper, tin, iron, coal,
     * gold, mithril and adamantite, its dwarves and scorpions; the Mining Guild behind its door (60
     * Mining) below the guild's own ladder in Falador; and the Motherlode Mine to the east with its
     * pay-dirt veins, hopper, sack and water wheel.
     */
    static Dungeons.Dungeon dwarvenMine() {
        Cave c = new Cave("dng_dwarven_mine", "Dwarven Mine", mc("stone"), mc("gravel"), mc("ochre_froglight"));
        int f = 30;
        int ix = -408;  // Ice Mountain ladder
        int iz = -468;
        int fx = -336;  // Falador house ladder
        int fz = -322;
        int gx = -391;  // Mining Guild ladder
        int gz = -233;
        c.room(ix - 8, iz - 6, ix + 8, iz + 10, f, 5);
        c.hallZ(ix, iz + 10, ix, iz + 48, f, 3, 4);
        int m1x = -446;
        int m2x = -330;
        int m1z = iz + 48;
        int m2z = iz + 118;
        c.room(m1x, m1z, m2x, m2z, f, 8);                              // the main mine cavern
        c.hallZ(fx, m2z, fx, fz, f, 3, 4);
        c.room(fx - 6, fz - 6, fx + 6, fz + 6, f, 5);                   // below the Falador stairs
        // The Mining Guild (south, through its door).
        int guildDoorZ = m2z + 8;
        c.hallZ(-400, m2z, -400, gz - 50, f, 3, 4);
        c.room(-436, gz - 50, -366, gz + 8, f, 7, mc("cobbled_deepslate"));
        // The Motherlode Mine (east).
        int mlz = fz + 2;
        int ml1x = fx + 10;
        c.hall(fx + 6, mlz, ml1x, mlz, f, 3, 4);
        c.room(ml1x, mlz - 26, ml1x + 52, mlz + 26, f, 8);
        c.carve();
        // Ore rocks in clusters through the cavern.
        String[] cluster1 = {rc("copper_rock"), rc("tin_rock"), rc("copper_rock"), rc("tin_rock")};
        String[] cluster2 = {rc("iron_rock"), rc("coal_rock"), rc("iron_rock"), rc("gold_rock")};
        String[] cluster3 = {rc("coal_rock"), rc("mithril_rock"), rc("coal_rock"), rc("adamantite_rock")};
        c.rocks(m1x + 1, m1z + 1, m1x + 31, m1z + 1, f + 1, 3, cluster1);
        c.rocks(m1x + 1, m1z + 4, m1x + 1, m2z - 1, f + 1, 4, cluster2);
        c.rocks(m2x - 31, m2z - 1, m2x - 1, m2z - 1, f + 1, 3, cluster3);
        c.rocks(m1x + 40, m1z + 30, m1x + 60, m1z + 40, f + 1, 10, rc("iron_rock"), rc("coal_rock"));
        // Mine carts and rails.
        int railZ = (m1z + m2z) / 2;
        c.fill(m1x + 10, f + 1, railZ, m2x - 10, f + 1, railZ, mc("rail[shape=east_west]"));
        c.set(m1x + 30, f + 1, railZ + 1, mc("chest"));
        // The Mining Guild door and the guild's rocks.
        c.barrier(-401, guildDoorZ, -399, guildDoorZ, f, 4, -400, guildDoorZ, rc(DungeonBlocks.MINING_GUILD_DOOR));
        c.rocks(-434, gz - 48, -368, gz - 48, f + 1, 3, rc("coal_rock"), rc("coal_rock"), rc("mithril_rock"), rc("iron_rock"));
        c.rocks(-434, gz - 44, -434, gz + 6, f + 1, 4, rc("coal_rock"), rc("mithril_rock"), rc("adamantite_rock"));
        c.rocks(-372, gz - 40, -368, gz - 20, f + 1, 4, rc("runite_rock"), rc("amethyst_rock"), rc("coal_rock"));
        c.set(-380, f + 1, gz + 4, mc("smithing_table"));
        // The Motherlode Mine: pay-dirt veins along the walls, the hopper, the sack and the water wheel.
        for (int x = ml1x + 3; x <= ml1x + 49; x += 4) {
            c.set(x, f + 1, mlz - 26, rc("pay_dirt_vein")).set(x, f + 1, mlz + 26, rc("pay_dirt_vein"));
        }
        c.set(ml1x + 26, f + 1, mlz - 4, rc("pay_dirt_hopper"));
        c.set(ml1x + 26, f + 1, mlz + 4, rc("pay_dirt_sack"));
        c.pool(ml1x + 30, mlz - 1, ml1x + 50, mlz + 1, f, mc("water"));
        for (int d = -3; d <= 3; d++) {
            c.set(ml1x + 46, f + 4 + d, mlz, mc("stripped_spruce_wood"));
            c.set(ml1x + 46, f + 4, mlz + d, mc("stripped_spruce_wood"));
        }
        c.fill(ml1x + 2, f + 1, mlz, ml1x + 24, f + 1, mlz, mc("rail[shape=east_west]"));
        // The lower level's inner ring: veins around a central rock pillar (150-tick respawn instead of 100).
        c.fill(ml1x + 9, f + 1, mlz + 11, ml1x + 15, f + 7, mlz + 17, mc("stone"));
        for (int d = 0; d <= 6; d += 2) {
            c.set(ml1x + 9 + d, f + 1, mlz + 10, rc("inner_pay_dirt_vein")).set(ml1x + 9 + d, f + 1, mlz + 18, rc("inner_pay_dirt_vein"));
            c.set(ml1x + 8, f + 1, mlz + 11 + d, rc("inner_pay_dirt_vein")).set(ml1x + 16, f + 1, mlz + 11 + d, rc("inner_pay_dirt_vein"));
        }
        // The upper level (57 Mining, 100 nuggets to Prospector Percy): a raised ledge with its 64 closely packed veins
        // in four rows of 16 and its own hopper, reached by steps on its east side.
        c.fill(ml1x + 4, f + 1, mlz - 21, ml1x + 36, f + 3, mlz - 10, mc("stone"));
        for (int x = ml1x + 5; x <= ml1x + 35; x += 2) {
            for (int z = mlz - 21; z <= mlz - 15; z += 2) {
                c.set(x, f + 4, z, rc("upper_pay_dirt_vein"));
            }
        }
        c.set(ml1x + 18, f + 4, mlz - 11, rc("upper_pay_dirt_hopper"));
        for (int d = 0; d < 3; d++) {
            if (d > 0) {
                c.fill(ml1x + 39 - d, f + 1, mlz - 12, ml1x + 39 - d, f + d, mlz - 11, mc("stone"));
            }
            c.fill(ml1x + 39 - d, f + 1 + d, mlz - 12, ml1x + 39 - d, f + 1 + d, mlz - 11, mc("stone_brick_stairs[facing=west]"));
        }
        c.link(ix, iz, 55, f, "south");
        c.link(fx, fz, 55, f, "south");
        c.link(gx, gz, 54, f, "north");
        c.camp(m1x + 4, m1z + 4, m2x - 4, m2z - 4, f, 6, 25, "dwarf", "dwarf", "scorpion");
        return c.build();
    }

    // ------------------------------------------------------------------ Asgarnian Ice Dungeon

    /**
     * Below Mudskipper Point (tile 3012, 3154): pirates and muggers by the entrance, hobgoblins to
     * the east, the frozen cavern of ice warriors and ice giants with the blurite rocks, and the
     * skeletal wyverns' lair beyond the icy passage.
     */
    static Dungeons.Dungeon iceDungeon() {
        Cave c = new Cave("dng_asgarnian_ice_dungeon", "Asgarnian Ice Dungeon", mc("packed_ice"), mc("snow_block"), mc("sea_lantern"));
        int f = 26;
        int lx = -420;
        int lz = 126;
        c.room(lx - 8, lz - 8, lx + 8, lz + 8, f, 5);
        c.hall(lx - 8, lz, lx - 16, lz, f, 3, 4);
        c.room(lx - 42, lz - 16, lx - 16, lz + 16, f, 5);                 // pirates and muggers
        c.hall(lx + 8, lz + 4, lx + 20, lz + 4, f, 3, 4);
        c.room(lx + 20, lz - 6, lx + 44, lz + 20, f, 5);                  // hobgoblins
        c.hallZ(lx + 30, lz - 6, lx + 30, lz - 20, f, 3, 4);
        c.room(lx + 10, lz - 70, lx + 70, lz - 20, f, 8, mc("packed_ice")); // ice warriors and ice giants
        c.hall(lx + 70, lz - 45, lx + 90, lz - 45, f, 3, 5);
        c.room(lx + 90, lz - 76, lx + 140, lz - 20, f, 10, mc("blue_ice")); // skeletal wyverns
        c.carve();
        c.rocks(lx + 12, lz - 68, lx + 30, lz - 68, f + 1, 6, rc("blurite_rock"));
        for (int x = lx + 16; x <= lx + 64; x += 12) {
            c.pillar(x, lz - 45, f, 8, mc("blue_ice"));
        }
        c.pool(lx + 100, lz - 60, lx + 110, lz - 50, f, mc("water"));
        c.fill(lx + 100, f, lz - 60, lx + 110, f, lz - 50, mc("ice"));
        c.set(lx - 40, f + 1, lz - 14, mc("barrel")).set(lx - 39, f + 1, lz - 14, mc("barrel"));
        // The 72 Agility tunnel from the hobgoblins to the skeletal wyverns (approx. ends).
        c.shortcut("asgarnian_ice_tunnel", lx + 42, lz + 18, f, lx + 92, lz - 22, f);
        c.link(lx, lz, 55, f, "south");
        c.camp(lx - 40, lz - 14, lx - 18, lz + 14, f, 4, 25, "pirate", "mugger");
        c.camp(lx + 22, lz - 4, lx + 42, lz + 18, f, 4, 25, "hobgoblin");
        c.camp(lx + 12, lz - 68, lx + 68, lz - 22, f, 7, 25, "ice_warrior", "ice_giant");
        c.camp(lx + 92, lz - 74, lx + 138, lz - 22, f, 4, 50, "skeletal_wyvern");
        return c.build();
    }
}
