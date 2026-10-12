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
     * hellhounds lies the far west (OSRS 2774-2835, 9803-9841): the baby black dragons' cave with two
     * black dragons, and up a stair the black dragons' upper cavern.
     */
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
        // The far west: the baby black dragons' cave and, up a stair, the black dragons' cavern.
        int bw = bx(2818);
        c.hall(ox - 54, oz - 46, bx(2836) + 1, oz - 46, f, 3, 4);
        c.room(bw, bz(3427), bx(2836) + 1, bz(3411) + 1, f, 6);         // baby black dragons
        int top = f + 6;
        c.stairs(bw, bz(3419), 'w', f, top, 3, 5);
        int bx2 = bw - (top - f) - 1;
        c.room(bx(2774), bz(3442), bx2, bz(3402) + 1, top, 10);         // black dragons
        c.carve();
        // The dusty-key gate before the blue dragons.
        gate(c, ox + 37, oz - 20, f, 'z');
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
        for (int x = bx(2780); x < bx2 - 4; x += 16) {
            c.pillar(x, bz(3434), top, 10, mc("chiseled_stone_bricks"));
            c.pillar(x, bz(3410), top, 10, mc("chiseled_stone_bricks"));
        }
        c.set(bx(2777), top + 1, bz(3405), mc("bone_block[axis=y]")).set(bx(2778), top + 1, bz(3405), mc("skeleton_skull"));
        c.entrance(ox, oz, f, mc("mossy_stone_bricks"), mc("cobblestone"), 'w');
        c.camp(ox - 5, oz - 5, ox + 5, oz + 5, f, 2, 25, "minecraft:skeleton", "ghost");
        c.camp(ox + 31, oz - 7, ox + 43, oz + 7, f, 4, 25, "chaos_druid");
        c.camp(ox + 26, oz - 60, ox + 58, oz - 32, f, 4, 50, "blue_dragon");
        c.camp(ox + 62, oz - 8, ox + 76, oz + 8, f, 3, 25, "baby_blue_dragon");
        c.camp(ox - 5, oz - 37, ox + 5, oz - 31, f, 3, 50, "lesser_demon");
        c.camp(ox - 52, oz - 60, ox - 20, oz - 32, f, 5, 25, "hellhound");
        c.camp(ox - 42, oz + 32, ox - 6, oz + 62, f, 4, 50, "black_demon");
        c.camp(ox + 24, oz + 38, ox + 48, oz + 58, f, 5, 25, "poison_spider");
        c.camp(bw + 2, bz(3426), bx(2836) - 1, bz(3412), f, 4, 35, "baby_black_dragon", "baby_black_dragon", "black_dragon");
        c.camp(bx(2775), bz(3441), bx2 - 2, bz(3403), top, 4, 50, "black_dragon");
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
        c.link(lx, lz, 55, f, "south");
        c.camp(lx - 40, lz - 14, lx - 18, lz + 14, f, 4, 25, "pirate", "mugger");
        c.camp(lx + 22, lz - 4, lx + 42, lz + 18, f, 4, 25, "hobgoblin");
        c.camp(lx + 12, lz - 68, lx + 68, lz - 22, f, 7, 25, "ice_warrior", "ice_giant");
        c.camp(lx + 92, lz - 74, lx + 138, lz - 22, f, 4, 50, "skeletal_wyvern");
        return c.build();
    }
}
