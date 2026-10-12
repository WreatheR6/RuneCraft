package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Misthalin's dungeons: the Edgeville Dungeon (with its Wilderness half), the Varrock Sewers, the
 * Stronghold of Security under Barbarian Village, the caves below the Lumbridge Castle cellar with
 * the Dorgesh-Kaan mine and city, the Lumbridge Swamp Caves (with the Ancient Guthix Temple) and the Abyss (entered near the Mage of
 * Zamorak in the Wilderness).
 */
final class DungeonsMisthalin {
    /** Filled in by {@link #abyss()}. */
    static final List<Dungeons.Rift> RIFTS = new ArrayList<>();

    private DungeonsMisthalin() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(edgeville());
        out.add(varrockSewers());
        out.add(strongholdOfSecurity());
        out.add(lumbridgeCellar());
        out.add(lumbridgeSwampCaves());
        out.add(abyss());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    // ------------------------------------------------------------------ Edgeville Dungeon

    /**
     * Below the Edgeville trapdoor (tile 3097, 3468): the landing, the hill giants' hall to the
     * south-east, the corridor of skeletons, zombies and giant rats running north, the chaos druids'
     * tower room, and past the Wilderness gate the earth warriors' cave and the black demons.
     */
    static Dungeons.Dungeon edgeville() {
        Cave c = new Cave("dng_edgeville_dungeon", "Edgeville Dungeon", mc("stone_bricks"), mc("cobblestone"), mc("glowstone"));
        int f = 26;
        int tx = bx(3097);
        int tz = bz(3468) + 1;
        c.room(tx - 6, tz - 6, tx + 6, tz + 6, f, 5);
        // South-east: the hill giants' hall (behind the brass key door in OSRS).
        c.hall(tx + 6, tz, tx + 24, tz, f, 3, 4);
        c.room(tx + 20, tz - 2, tx + 54, tz + 28, f, 8);
        // North: the long corridor with skeletons, zombies and giant rats (the old jail).
        c.hallZ(tx, tz - 6, tx, tz - 30, f, 3, 4);
        c.room(tx - 14, tz - 60, tx + 10, tz - 30, f, 6);
        // The chaos druids' room to the north-east.
        c.hallZ(tx, tz - 60, tx + 14, tz - 82, f, 3, 4);
        c.room(tx + 14, tz - 96, tx + 38, tz - 72, f, 6);
        // The Wilderness gate (the Wilderness ditch is at tile y 3520) and beyond it.
        int ditch = bz(3520);
        c.hallZ(tx, tz - 60, tx, ditch - 16, f, 3, 4);
        c.room(tx - 12, ditch - 56, tx + 24, ditch - 16, f, 8);
        c.hall(tx - 12, ditch - 30, tx - 34, ditch - 30, f, 3, 4);
        c.room(tx - 58, ditch - 50, tx - 34, ditch - 14, f, 8);
        c.carve();
        DungeonsAsgarnia.gate(c, tx, ditch - 4, f, 'z');
        // Jail cells along the corridor room and cobwebs.
        for (int z = tz - 56; z <= tz - 36; z += 6) {
            c.fill(tx - 14, f + 1, z, tx - 9, f + 3, z, mc("iron_bars"));
        }
        c.set(tx + 9, f + 1, tz - 59, mc("cobweb")).set(tx - 13, f + 1, tz - 31, mc("cobweb"));
        c.pool(tx + 24, tz + 18, tx + 30, tz + 24, f, mc("water"));
        c.link(tx, tz, 52, f, "north");
        c.camp(tx + 22, tz, tx + 52, tz + 26, f, 6, 50, "hill_giant");
        c.camp(tx - 12, tz - 58, tx + 8, tz - 32, f, 6, 25, "minecraft:skeleton", "minecraft:zombie", "giant_rat");
        c.camp(tx + 16, tz - 94, tx + 36, tz - 74, f, 6, 25, "chaos_druid");
        c.camp(tx - 10, ditch - 54, tx + 22, ditch - 18, f, 6, 25, "earth_warrior");
        c.camp(tx - 56, ditch - 48, tx - 36, ditch - 16, f, 3, 50, "black_demon");
        return c.build();
    }

    // ------------------------------------------------------------------ Varrock Sewers

    /**
     * Below the manhole east of the palace (tile 3237, 3458): the main east-west sewer with its
     * channel of water, the moss giants' cavern to the west, zombies to the south and giant rats in
     * the north-eastern tunnels, ending at Scurrius' chamber in the east.
     */
    static Dungeons.Dungeon varrockSewers() {
        Cave c = new Cave("dng_varrock_sewers", "Varrock Sewers", mc("mossy_stone_bricks"), mc("stone_bricks"), mc("sea_lantern"));
        int f = 30;
        int x = bx(3237);
        int z = bz(3458);
        int cz = z + 2;
        c.room(x - 100, cz - 6, x + 100, cz + 6, f, 5);
        c.room(x - 140, cz - 26, x - 100, cz + 22, f, 7);   // moss giants
        c.hallZ(x + 30, cz + 6, x + 30, cz + 20, f, 3, 4);
        c.room(x + 16, cz + 20, x + 50, cz + 40, f, 5);     // zombies
        c.hallZ(x + 70, cz - 6, x + 70, cz - 24, f, 3, 4);
        c.room(x + 56, cz - 50, x + 96, cz - 24, f, 4);     // rats
        c.room(x + 100, cz - 12, x + 124, cz + 12, f, 6);   // Scurrius' chamber
        c.carve();
        c.pool(x - 100, cz, x + 100, cz, f, mc("water"));
        for (int xx = x - 96; xx <= x + 96; xx += 16) {
            c.set(xx, f, cz, mc("mossy_stone_bricks"));
        }
        c.set(x - 120, f + 1, cz - 20, mc("cobweb")).set(x - 104, f + 1, cz + 18, mc("cobweb"));
        c.set(x + 112, f + 1, cz, mc("bone_block")).set(x + 110, f + 1, cz + 4, mc("bone_block"));
        c.link(x + 6, z + 2, 52, f, "north");
        c.camp(x - 136, cz - 22, x - 104, cz + 18, f, 5, 50, "moss_giant");
        c.camp(x + 18, cz + 22, x + 48, cz + 38, f, 5, 25, "minecraft:zombie");
        c.camp(x + 58, cz - 48, x + 94, cz - 26, f, 7, 17, "giant_rat");
        c.camp(x - 90, cz - 5, x + 90, cz - 2, f, 3, 25, "giant_rat", "minecraft:zombie");
        return c.build();
    }

    // ------------------------------------------------------------------ Stronghold of Security

    /**
     * The Stronghold of Security below Barbarian Village (tile 3081, 3421): four floors - the
     * Vault of War, the Catacomb of Famine, the Pit of Pestilence and the Sepulchre of Death - each a
     * loop of rooms joined by doors that test your account security, ending at its reward chest (Gift
     * of Peace, Grain of Plenty, Box of Health and the Cradle of Life) with a ladder down to the next.
     */
    static Dungeons.Dungeon strongholdOfSecurity() {
        Cave c = new Cave("dng_stronghold_of_security", "Stronghold of Security", mc("cobbled_deepslate"),
                mc("polished_deepslate"), mc("shroomlight"));
        int ox = bx(3081);
        int oz = bz(3421) + 1;
        int[] floors = {36, 20, 4, -12};
        String[][] monsters = {
                {"goblin", "minotaur", "minecraft:wolf"},
                {"flesh_crawler", "minecraft:zombie", "giant_rat"},
                {"catablepon", "giant_spider", "scorpion"},
                {"ankou", "ghost", "minecraft:skeleton"}};
        // Floor k is centred on its arrival ladder; odd floors are mirrored so the ladders alternate
        // between the two columns.
        int[][] origin = new int[4][];
        for (int k = 0; k < 4; k++) {
            int s = k % 2 == 0 ? 1 : -1;
            origin[k] = k % 2 == 0 ? new int[]{ox, oz} : new int[]{ox - 30, oz + 28};
            sosRooms(c, origin[k][0], origin[k][1], s, floors[k]);
        }
        c.carve();
        String[] walls = {mc("cobblestone"), mc("mossy_cobblestone"), mc("moss_block"), mc("bone_block")};
        for (int k = 0; k < 4; k++) {
            int s = k % 2 == 0 ? 1 : -1;
            sosDetails(c, origin[k][0], origin[k][1], s, floors[k], DungeonBlocks.SOS_DOORS.get(k), DungeonBlocks.SOS_CHESTS.get(k),
                    walls[k], monsters[k]);
            if (k < 3) {
                c.ladderUp(origin[k][0] + s * -30, origin[k][1] + s * 28, floors[k + 1], floors[k]);
            }
        }
        c.link(ox, oz, 50, floors[0], "north");
        return c.build();
    }

    private static int[] box(int ox, int oz, int s, int x1, int z1, int x2, int z2) {
        return new int[]{ox + s * x1, oz + s * z1, ox + s * x2, oz + s * z2};
    }

    private static void sosRooms(Cave c, int ox, int oz, int s, int f) {
        int[][] rooms = {
                box(ox, oz, s, -4, -4, 4, 4),        // A: arrival
                box(ox, oz, s, 20, -8, 32, 8),       // B
                box(ox, oz, s, 16, -40, 36, -24),    // C
                box(ox, oz, s, -36, -44, -20, -20),  // D
                box(ox, oz, s, -36, 16, -20, 34),    // E: ladder down
                box(ox, oz, s, 8, 18, 20, 30)};      // F: reward chest
        for (int[] r : rooms) {
            c.room(r[0], r[1], r[2], r[3], f, 5);
        }
        c.hall(ox + s * 4, oz, ox + s * 20, oz, f, 3, 4);
        c.hallZ(ox + s * 26, oz - s * 8, ox + s * 26, oz - s * 24, f, 3, 4);
        c.hall(ox + s * 16, oz - s * 32, ox - s * 20, oz - s * 32, f, 3, 4);
        c.hallZ(ox - s * 28, oz - s * 20, ox - s * 28, oz + s * 16, f, 3, 4);
        c.hall(ox - s * 20, oz + s * 24, ox + s * 8, oz + s * 24, f, 3, 4);
    }

    private static void sosDetails(Cave c, int ox, int oz, int s, int f, String door, String chest, String deco, String[] monsters) {
        // Doors across each corridor (the security questions).
        sosDoorX(c, ox + s * 12, oz, f, door);
        sosDoorZ(c, ox + s * 26, oz - s * 16, f, door);
        sosDoorX(c, ox, oz - s * 32, f, door);
        sosDoorZ(c, ox - s * 28, oz, f, door);
        sosDoorX(c, ox - s * 4, oz + s * 24, f, door);
        // The reward chest on a dais.
        int chx = ox + s * 14;
        int chz = oz + s * 24;
        c.fill(chx - 1, f, chz - 1, chx + 1, f, chz + 1, mc("gold_block"));
        c.set(chx, f + 1, chz, rc(chest));
        // Decoration: pillars in the big rooms.
        int[][] pillars = {{22, -6}, {30, 6}, {18, -38}, {34, -26}, {-34, -42}, {-22, -22}, {-34, 18}, {-22, 32}};
        for (int[] p : pillars) {
            c.pillar(ox + s * p[0], oz + s * p[1], f, 5, deco);
        }
        int[][] boxes = {box(ox, oz, s, 21, -7, 31, 7), box(ox, oz, s, 17, -39, 35, -25), box(ox, oz, s, -35, -43, -21, -21),
                box(ox, oz, s, -35, 17, -21, 33)};
        for (int[] r : boxes) {
            c.camp(r[0], r[1], r[2], r[3], f, 3, 25, monsters);
        }
    }

    /** A door in a corridor running along x (the wall spans z - 1 .. z + 1). */
    private static void sosDoorX(Cave c, int x, int z, int f, String door) {
        c.barrier(x, z - 1, x, z + 1, f, 4, x, z, rc(door));
    }

    /** A door in a corridor running along z. */
    private static void sosDoorZ(Cave c, int x, int z, int f, String door) {
        c.barrier(x - 1, z, x + 1, z, f, 4, x, z, rc(door));
    }

    // ------------------------------------------------------------------ Lumbridge cellar and Dorgesh-Kaan

    /**
     * Below the Lumbridge Castle cellar: the crack in the cellar wall (The Lost Tribe) opens into a
     * cave and the Dorgesh-Kaan mine with its cave goblin guards, and a long stair leads down to the
     * lamp-lit city of Dorgesh-Kaan itself.
     */
    static Dungeons.Dungeon lumbridgeCellar() {
        Cave c = new Cave("dng_lumbridge_cellar_caves", "Lumbridge cellar caves and Dorgesh-Kaan", mc("tuff"), mc("cobbled_deepslate"),
                mc("ochre_froglight"));
        int f = 40;
        int lx = -29;
        int lz = 22;
        c.room(lx - 7, lz - 8, lx + 9, lz + 8, f, 5);
        // The Dorgesh-Kaan mine.
        c.hall(lx + 9, lz, 2, lz, f, 3, 4);
        c.room(2, lz - 18, 32, lz + 6, f, 6);
        // The stairs down to the city.
        c.stairs(32, lz - 6, 'e', f, 10, 5, 6);
        int cx1 = 64;
        int cx2 = 160;
        int cz1 = lz - 56;
        int cz2 = lz + 40;
        c.room(cx1, cz1, cx2, cz2, 10, 16, mc("polished_andesite"));
        c.carve();
        int fc = 10;
        // Mine rocks along the mine walls.
        c.rocks(3, lz - 17, 31, lz - 17, f + 1, 3, rc("iron_rock"), rc("coal_rock"), rc("silver_rock"), rc("coal_rock"));
        c.rocks(3, lz + 5, 31, lz + 5, f + 1, 4, rc("iron_rock"), rc("coal_rock"), rc("gold_rock"));
        c.set(lx - 6, f + 1, lz - 7, mc("barrel")).set(lx - 5, f + 1, lz - 7, mc("barrel"));
        // The city: houses in blocks along a grid of streets, lamps everywhere (the goblins love light).
        for (int hx = cx1 + 4; hx + 12 <= cx2 - 4; hx += 18) {
            for (int hz = cz1 + 4; hz + 10 <= cz2 - 4; hz += 16) {
                if (hx > cx1 + 40 && hx < cx1 + 60 && hz > cz1 + 30 && hz < cz1 + 60) {
                    continue; // the market square
                }
                house(c, hx, hz, hx + 11, hz + 9, fc);
            }
        }
        int mx = cx1 + 50;
        int mz = cz1 + 44;
        c.fill(mx - 2, fc + 1, mz - 2, mx + 2, fc + 1, mz + 2, mc("polished_blackstone"));
        c.fill(mx, fc + 2, mz, mx, fc + 5, mz, mc("polished_blackstone_wall"));
        c.set(mx, fc + 6, mz, mc("ochre_froglight"));
        for (int xx = cx1 + 2; xx <= cx2 - 2; xx += 9) {
            c.set(xx, fc + 1, cz1 + 2, mc("pearlescent_froglight")).set(xx, fc + 1, cz2 - 2, mc("pearlescent_froglight"));
        }
        // Train tunnel to Keldagrim (closed off) on the east wall.
        c.fill(cx2, fc + 1, lz - 10, cx2, fc + 4, lz - 6, mc("iron_bars"));
        c.link(lx, lz, 57, f, "east");
        c.camp(4, lz - 15, 30, lz + 3, f, 4, 25, "cave_goblin_guard");
        c.camp(cx1 + 2, cz1 + 2, cx2 - 2, cz2 - 2, fc, 10, 25, "cave_goblin", "cave_goblin", "cave_goblin_guard");
        return c.build();
    }

    /** A Dorgesh-Kaan house: a stone box with a door and lit windows. */
    private static void house(Cave c, int x1, int z1, int x2, int z2, int f) {
        String w = mc("stone_bricks");
        c.b.walls(x1, f + 1, z1, x2, f + 6, z2, w);
        c.fill(x1, f + 7, z1, x2, f + 7, z2, mc("polished_andesite"));
        int mx = (x1 + x2) / 2;
        c.b.clear(mx, f + 1, z2, mx, f + 2, z2);
        c.set(x1 + 2, f + 3, z1, mc("yellow_stained_glass")).set(x2 - 2, f + 3, z1, mc("yellow_stained_glass"));
        c.set(x1 + 2, f + 3, z2, mc("yellow_stained_glass")).set(x2 - 2, f + 3, z2, mc("yellow_stained_glass"));
        c.set(mx, f + 6, (z1 + z2) / 2, mc("ochre_froglight"));
    }

    // ------------------------------------------------------------------ Lumbridge Swamp Caves

    /**
     * Below the hole in Lumbridge Swamp (tile 3169, 3172): a maze of damp tunnels with cave bugs,
     * cave slime, cave crawlers and wall beasts, and the deep pool where the Tears of Guthix lie
     * beyond.
     */
    static Dungeons.Dungeon lumbridgeSwampCaves() {
        Cave c = new Cave("dng_lumbridge_swamp_caves", "Lumbridge Swamp Caves", mc("mossy_cobblestone"), mc("mud"), null);
        int f = 22;
        int ex = bx(3169) + 2;
        int ez = bz(3172);
        c.room(ex - 6, ez - 6, ex + 6, ez + 6, f, 5);
        c.hall(ex + 6, ez, ex + 40, ez + 20, f, 3, 4);
        c.room(ex + 30, ez + 20, ex + 54, ez + 40, f, 5);           // cave bugs
        c.hallZ(ex, ez + 6, ex - 12, ez + 30, f, 3, 4);
        c.room(ex - 40, ez + 26, ex - 12, ez + 46, f, 5);           // cave slime
        c.hall(ex - 6, ez, ex - 44, ez - 12, f, 3, 4);
        c.room(ex - 70, ez - 30, ex - 44, ez - 4, f, 6);            // cave crawlers
        c.hallZ(ex + 42, ez + 40, ex + 42, ez + 64, f, 3, 4);       // wall beast corridor
        c.room(ex + 30, ez + 64, ex + 60, ez + 82, f, 6);           // the Tears of Guthix pool
        c.hall(ex + 60, ez + 73, ex + 84, ez + 73, f, 3, 4);        // on past the pool (While Guthix Sleeps)
        c.room(ex + 84, ez + 60, ex + 116, ez + 88, f, 8);          // the Ancient Guthix Temple: tormented demons
        c.carve();
        for (int px : new int[]{ex + 88, ex + 112}) {
            for (int pz : new int[]{ez + 64, ez + 84}) {
                c.fill(px, f + 1, pz, px, f + 7, pz, mc("chiseled_stone_bricks"));
            }
        }
        c.pool(ex - 36, ez + 30, ex - 28, ez + 36, f, mc("water"));
        c.pool(ex + 38, ez + 68, ex + 52, ez + 78, f, mc("water"));
        for (int z = ez + 44; z <= ez + 62; z += 6) {
            c.set(ex + 40, f + 1, z, mc("mossy_cobblestone_wall"));
        }
        c.set(ex + 4, f + 1, ez - 5, mc("glow_lichen[down=true]"));
        c.entrance(ex, ez, f, mc("mossy_cobblestone"), mc("mud"), 'w');
        c.camp(ex + 32, ez + 22, ex + 52, ez + 38, f, 5, 25, "cave_bug");
        c.camp(ex - 38, ez + 28, ex - 14, ez + 44, f, 4, 25, "cave_slime");
        c.camp(ex - 68, ez - 28, ex - 46, ez - 6, f, 4, 25, "cave_crawler");
        c.camp(ex + 41, ez + 42, ex + 43, ez + 62, f, 2, 50, "wall_beast");
        c.camp(ex + 90, ez + 66, ex + 110, ez + 82, f, 3, 50, "tormented_demon"); // respawn approx
        return c.build();
    }

    // ------------------------------------------------------------------ The Abyss

    /**
     * The Abyss, reached through a shaft by the Mage of Zamorak's spot in the Wilderness (tile 3106,
     * 3556): an outer ring prowled by abyssal leeches, guardians and walkers, an inner wall passable
     * through its gaps and passages, and the inner ring with a rift to each rune altar.
     */
    static Dungeons.Dungeon abyss() {
        Cave c = new Cave("dng_the_abyss", "The Abyss", mc("crimson_nylium"), mc("netherrack"), mc("shroomlight"));
        int f = -24;
        int h = 10;
        int cx = bx(3106);
        int cz = bz(3556) - 42;
        int outer = 40;
        int wallIn = 21;
        int wallOut = 26;
        c.disc(cx, cz, outer, f, h);
        c.carve();
        // The inner wall.
        String fleshWall = mc("nether_wart_block");
        for (int dz = -wallOut; dz <= wallOut; dz++) {
            int ho = half(wallOut, dz);
            int hi = Math.abs(dz) <= wallIn ? half(wallIn, dz) : -1;
            if (hi < 0) {
                c.fill(cx - ho, f + 1, cz + dz, cx + ho, f + h, cz + dz, fleshWall);
            } else {
                c.fill(cx - ho, f + 1, cz + dz, cx - hi - 1, f + h, cz + dz, fleshWall);
                c.fill(cx + hi + 1, f + 1, cz + dz, cx + ho, f + h, cz + dz, fleshWall);
            }
        }
        // Gaps through the wall: north is open (the gap to squeeze through), the others are
        // obstacles (rocks, tendrils, boil, eyes) passed by using them.
        c.b.clear(cx - 1, f + 1, cz - wallOut - 1, cx + 1, f + 3, cz - wallIn + 1);
        c.fill(cx + wallIn - 1, f + 1, cz - 1, cx + wallOut + 1, f + 3, cz + 1, rc(DungeonBlocks.ABYSS_PASSAGE));
        c.fill(cx - 1, f + 1, cz + wallIn - 1, cx + 1, f + 3, cz + wallOut + 1, rc(DungeonBlocks.ABYSS_PASSAGE));
        c.fill(cx - wallOut - 1, f + 1, cz - 1, cx - wallIn + 1, f + 3, cz + 1, rc(DungeonBlocks.ABYSS_PASSAGE));
        // The rifts, in a circle in the inner ring (the thirteen altars the real Abyss leads to).
        String[][] rifts = {{"AIR_ALTAR", "air"}, {"MIND_ALTAR", "mind"}, {"WATER_ALTAR", "water"}, {"EARTH_ALTAR", "earth"},
                {"FIRE_ALTAR", "fire"}, {"BODY_ALTAR", "body"}, {"COSMIC_ALTAR", "cosmic"}, {"CHAOS_ALTAR", "chaos"},
                {"NATURE_ALTAR", "nature"}, {"LAW_ALTAR", "law"}, {"DEATH_ALTAR", "death"}, {"BLOOD_ALTAR", "blood"},
                {"SOUL_ALTAR", "soul"}};
        RIFTS.clear();
        for (int i = 0; i < rifts.length; i++) {
            double a = Math.PI * 2 * i / rifts.length;
            int rx = cx + (int) Math.round(Math.sin(a) * 14);
            int rz = cz - (int) Math.round(Math.cos(a) * 14);
            c.set(rx, f, rz, mc("crying_obsidian"));
            c.set(rx, f + 1, rz, rc(DungeonBlocks.RIFT));
            c.set(rx, f + 2, rz, mc("light")); // invisible light source above each rift
            RIFTS.add(new Dungeons.Rift(rx, f + 1, rz, rifts[i][0], rifts[i][1]));
        }
        int ex = cx;
        int ez = cz + outer - 6;
        c.entrance(ex, ez, f, mc("polished_blackstone_bricks"), mc("polished_blackstone"), 's');
        // Camps around the outer ring.
        int r = (wallOut + outer) / 2;
        int d = r * 7 / 10;
        int[][] spots = {{0, -r}, {r, 0}, {-r, 0}, {d, -d}, {-d, -d}, {d, d}, {-d, d}};
        for (int[] p : spots) {
            c.camp(cx + p[0] - 5, cz + p[1] - 5, cx + p[0] + 5, cz + p[1] + 5, f, 2, 25, "abyssal_leech", "abyssal_guardian",
                    "abyssal_walker");
        }
        return c.build();
    }

    private static int half(int r, int dz) {
        return (int) Math.floor(Math.sqrt((r + 0.5) * (r + 0.5) - dz * dz));
    }
}
