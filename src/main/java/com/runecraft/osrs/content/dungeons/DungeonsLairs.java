package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Smaller lairs whose monsters used to wait at their entrances on the surface: the Haunted Mine and
 * Tarn's Lair, the Mort'ton shade catacombs, the Brine Rat Cavern, the Sourhog Cave, the Legends'
 * Guild dungeon, the Wilderness God Wars Dungeon and Jormungand's Prison below the Island of Stone.
 *
 * <p>Layouts follow the OSRS maps of the real dungeons (OSRS dungeon tile y minus 6400 gives the
 * surface tile above, which is roughly where each room is carved here), re-anchored on the surface
 * entrance where the two differ. Room positions are given in OSRS tiles.
 */
final class DungeonsLairs {
    private DungeonsLairs() {
    }

    static List<Dungeons.Dungeon> all() {
        List<Dungeons.Dungeon> out = new ArrayList<>();
        out.add(tarnsLair());
        out.add(shadeCatacombs());
        out.add(brineRatCavern());
        out.add(sourhogCave());
        out.add(legendsGuildDungeon());
        out.add(wildernessGodWars());
        out.add(jormungandsPrison());
        return out;
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /** A room over the OSRS tile rectangle (inclusive) on floor f, h blocks high. */
    static void tRoom(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int h) {
        c.room(bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1, f, h);
    }

    static void tRoom(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int h, String floor) {
        c.room(bx(Math.min(tx1, tx2)), bz(Math.max(ty1, ty2)), bx(Math.max(tx1, tx2)) + 1, bz(Math.min(ty1, ty2)) + 1, f, h, floor);
    }

    /** A camp over the OSRS tile rectangle (inclusive), shrunk by one block so it stays off the walls. */
    static void tCamp(Cave c, int tx1, int ty1, int tx2, int ty2, int f, int cap, int respawn, String... creatures) {
        c.camp(bx(Math.min(tx1, tx2)) + 1, bz(Math.max(ty1, ty2)) + 1, bx(Math.max(tx1, tx2)), bz(Math.min(ty1, ty2)), f, cap,
                respawn, creatures);
    }

    // ------------------------------------------------------------------ Haunted Mine & Tarn's Lair

    /**
     * The Haunted Mine (cart tunnel entrance tile 3445, 3223 - a few tiles south-east of the OSRS one, to
     * stay clear of the quest spot and Tarn's arena - south-west of Mort'ton) and, below its
     * deepest level, Tarn's Lair: the mine's rail tunnel and cart cavern, the stair down into the
     * lair, the terror dogs' halls (OSRS: the instanced lair at 3140-3158, 4645-4656 - nine terror
     * dogs) and Tarn's own chamber at the end. Tarn himself is fought at the quest's arena above.
     * The lair's traps and jumping stones are left out (approx).
     */
    static Dungeons.Dungeon tarnsLair() {
        Cave c = new Cave("dng_tarns_lair", "Haunted Mine and Tarn's Lair", mc("cobbled_deepslate"), mc("gravel"),
                mc("verdant_froglight"));
        int mine = 32;
        int lair = 22;
        int ex = bx(3445);
        int ez = bz(3223);
        c.room(ex - 6, ez - 6, ex + 6, ez + 6, mine, 5);
        c.hall(ex - 6, ez, bx(3414), bz(3230), mine, 3, 4);                    // the cart tunnel west
        tRoom(c, 3406, 3226, 3420, 3240, mine, 7);                             // the mine's cart cavern
        // Down the deep shaft: a long stair north from the cart cavern into the lair.
        int sx = bx(3412);
        c.stairs(sx, bz(3240), 'n', mine, lair, 3, 5);
        int lz = bz(3240) - (mine - lair) - 1;
        c.hallZ(sx, lz, sx, bz(3248), lair, 3, 4);
        tRoom(c, 3400, 3248, 3418, 3262, lair, 7);                             // first terror dog hall
        c.hall(bx(3418), bz(3255), bx(3424), bz(3255), lair, 3, 4);
        tRoom(c, 3424, 3246, 3436, 3264, lair, 7);                             // second hall
        c.hall(bx(3436), bz(3255), bx(3442), bz(3255), lair, 3, 4);
        tRoom(c, 3442, 3250, 3452, 3260, lair, 6);                             // Tarn's chamber
        c.carve();
        // Rails and abandoned carts in the mine.
        c.fill(ex - 5, mine + 1, ez, bx(3414) + 1, mine + 1, ez, mc("rail[shape=east_west]"));
        c.set(bx(3410), mine + 1, bz(3230), mc("chest_minecart"));
        c.set(bx(3416), mine + 1, bz(3236), mc("minecart"));
        for (int x = bx(3408); x <= bx(3418); x += 6) {
            c.pillar(x, bz(3228), mine, 7, mc("stripped_dark_oak_log"));
            c.pillar(x, bz(3238), mine, 7, mc("stripped_dark_oak_log"));
        }
        // The lair: bone heaps, pillars and the green glow of Tarn's experiments.
        for (int x = bx(3403); x <= bx(3415); x += 8) {
            c.pillar(x, bz(3251), lair, 7, mc("polished_basalt[axis=y]"));
            c.pillar(x, bz(3260), lair, 7, mc("polished_basalt[axis=y]"));
        }
        c.pool(bx(3428), bz(3252), bx(3432), bz(3250), lair, mc("soul_soil"));
        c.set(bx(3427), lair + 1, bz(3260), mc("bone_block[axis=y]"));
        c.set(bx(3433), lair + 1, bz(3248), mc("bone_block[axis=y]"));
        c.set(bx(3450), lair + 1, bz(3255), mc("enchanting_table"));
        c.set(bx(3450), lair + 1, bz(3257), mc("brewing_stand"));
        c.set(bx(3449), lair + 1, bz(3252), mc("skeleton_skull[rotation=4]"));
        c.entrance(ex, ez, mine, mc("cobbled_deepslate"), mc("gravel"), 'w');
        tCamp(c, 3401, 3249, 3417, 3261, lair, 4, 35, "terror_dog");
        tCamp(c, 3425, 3247, 3435, 3263, lair, 4, 35, "terror_dog");
        return c.build();
    }

    // ------------------------------------------------------------------ Mort'ton shade catacombs

    /**
     * The shade catacombs below Mort'ton, entered just north of the general store (tile 3482, 3313;
     * in OSRS the entrance is inside the walls - here it is just outside them): the phrin shades'
     * crypt by the stair, then behind gates (the shade-key doors of OSRS; no keys needed here,
     * approx) the riyl shades to the east, the asyn shades to the south-east and the fiyr shades in
     * the deepest crypt to the south-west, each lined with coffins.
     */
    static Dungeons.Dungeon shadeCatacombs() {
        Cave c = new Cave("dng_shade_catacombs", "Mort'ton catacombs", mc("deepslate_bricks"), mc("cobbled_deepslate"),
                mc("pearlescent_froglight"));
        int f = 28;
        int ex = bx(3482);
        int ez = bz(3313);
        c.room(ex - 6, ez - 5, ex + 6, ez + 5, f, 5);
        c.hallZ(ex, ez + 5, ex, bz(3300), f, 3, 4);
        tRoom(c, 3472, 3288, 3490, 3300, f, 6);                                // phrin shades
        c.hall(bx(3490), bz(3294), bx(3494), bz(3294), f, 3, 4);
        tRoom(c, 3494, 3286, 3510, 3302, f, 6);                                // riyl shades
        c.hallZ(bx(3502), bz(3286), bx(3502), bz(3282), f, 3, 4);
        tRoom(c, 3494, 3266, 3510, 3282, f, 6);                                // asyn shades
        c.hall(bx(3494), bz(3274), bx(3490), bz(3274), f, 3, 4);
        tRoom(c, 3472, 3264, 3490, 3284, f, 7);                                // fiyr shades
        c.carve();
        // The shade-key gates.
        DungeonsAsgarnia.gate(c, bx(3492), bz(3294), f, 'x');
        DungeonsAsgarnia.gate(c, bx(3502), bz(3284), f, 'z');
        DungeonsAsgarnia.gate(c, bx(3492), bz(3274), f, 'x');
        // Coffins along the crypt walls, cobwebs and soul fires.
        coffins(c, 3473, 3289, 3489, 3299, f);
        coffins(c, 3495, 3287, 3509, 3301, f);
        coffins(c, 3495, 3267, 3509, 3281, f);
        coffins(c, 3473, 3265, 3489, 3283, f);
        for (int[] p : new int[][]{{3481, 3294}, {3502, 3294}, {3502, 3274}, {3481, 3274}}) {
            c.set(bx(p[0]), f + 1, bz(p[1]), mc("soul_campfire"));
        }
        for (int x = bx(3474); x <= bx(3508); x += 7) {
            c.set(x, f + 6, bz(3299), mc("cobweb")).set(x + 2, f + 6, bz(3267), mc("cobweb"));
        }
        c.entrance(ex, ez, f, mc("deepslate_bricks"), mc("cobbled_deepslate"), 'n');
        tCamp(c, 3473, 3289, 3489, 3299, f, 3, 35, "phrin_shade");
        tCamp(c, 3495, 3287, 3509, 3301, f, 3, 35, "riyl_shade");
        tCamp(c, 3495, 3267, 3509, 3281, f, 3, 35, "asyn_shade");
        tCamp(c, 3473, 3265, 3489, 3283, f, 3, 50, "fiyr_shade");
        return c.build();
    }

    /** Rows of stone coffins against the north and south walls of a crypt (tile rectangle inside it). */
    private static void coffins(Cave c, int tx1, int ty1, int tx2, int ty2, int f) {
        for (int x = bx(tx1) + 2; x <= bx(tx2) - 1; x += 4) {
            c.fill(x, f + 1, bz(ty2), x + 1, f + 1, bz(ty2), mc("polished_deepslate_slab"));
            c.fill(x, f + 1, bz(ty1) + 1, x + 1, f + 1, bz(ty1) + 1, mc("polished_deepslate_slab"));
        }
    }

    // ------------------------------------------------------------------ Brine Rat Cavern

    /**
     * The Brine Rat Cavern (Olaf's Quest), dug into from the hill east of Rellekka (tile 2748, 3733):
     * a long, damp tunnel west to the round cavern of brine pools where the brine rats live (OSRS
     * 2702-2712, 10131-10137: seven brine rats).
     */
    static Dungeons.Dungeon brineRatCavern() {
        Cave c = new Cave("dng_brine_rat_cavern", "Brine Rat Cavern", mc("stone"), mc("gravel"), mc("sea_lantern"));
        int f = 24;
        int ex = bx(2748);
        int ez = bz(3733);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hall(ex - 5, ez, bx(2722), ez + 6, f, 3, 4);
        c.hall(bx(2722), ez + 6, bx(2716), ez + 6, f, 3, 4);
        int cx = bx(2706);
        int cz = bz(3734);
        c.disc(cx, cz, 22, f, 7);
        c.disc(cx - 12, cz - 18, 10, f, 6);
        c.carve();
        c.pool(cx - 6, cz - 4, cx + 2, cz + 3, f, mc("water"));
        c.pool(cx - 16, cz - 22, cx - 10, cz - 16, f, mc("water"));
        c.pool(cx + 10, cz + 8, cx + 15, cz + 12, f, mc("water"));
        for (int[] p : new int[][]{{-14, 6}, {12, -10}, {4, 14}, {-4, -16}}) {
            c.pillar(cx + p[0], cz + p[1], f, 7, mc("dripstone_block"));
        }
        c.set(cx + 16, f + 1, cz - 4, mc("kelp_plant")).set(cx - 18, f + 1, cz + 6, mc("sea_pickle"));
        c.entrance(ex, ez, f, mc("stone"), mc("gravel"), 'e');
        c.camp(cx - 14, cz - 14, cx + 14, cz + 14, f, 4, 35, "brine_rat");
        return c.build();
    }

    // ------------------------------------------------------------------ Sourhog Cave

    /**
     * The Sourhog Cave (A Porcine of Interest), east of Draynor Manor: the strange hole is the quest's
     * spot by the river (tile 3155, 3345), so the way down here is a hut on the west bank (tile 3140,
     * 3345 - approx), and a muddy passage leads under the river into the long cave of sourhogs (OSRS
     * 3157-3177, 9673-9699: eight sourhogs), with mud wallows and roots.
     */
    static Dungeons.Dungeon sourhogCave() {
        Cave c = new Cave("dng_sourhog_cave", "Sourhog Cave", mc("packed_mud"), mc("coarse_dirt"), mc("glowstone"));
        int f = 28;
        int ex = bx(3140);
        int ez = bz(3345);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hall(ex + 5, ez, bx(3150), bz(3336), f, 3, 4);
        tRoom(c, 3144, 3322, 3162, 3336, f, 6);                                // the north cave
        c.hall(bx(3162), bz(3328), bx(3168), bz(3322), f, 3, 4);
        tRoom(c, 3160, 3308, 3176, 3322, f, 6);                                // the south-east hollow
        c.carve();
        c.pool(bx(3150), bz(3330), bx(3154), bz(3327), f, mc("mud"));
        c.pool(bx(3166), bz(3316), bx(3170), bz(3313), f, mc("mud"));
        for (int x = bx(3146); x <= bx(3160); x += 7) {
            c.set(x, f + 6, bz(3324), mc("hanging_roots")).set(x + 3, f + 6, bz(3334), mc("hanging_roots"));
        }
        c.set(bx(3174), f + 1, bz(3310), mc("hay_block"));
        c.set(bx(3173), f + 1, bz(3310), mc("carrots[age=7]"));
        c.entrance(ex, ez, f, mc("packed_mud"), mc("coarse_dirt"), 'w');
        tCamp(c, 3145, 3323, 3161, 3335, f, 3, 35, "sourhog");
        tCamp(c, 3161, 3309, 3175, 3321, f, 2, 35, "sourhog");
        return c.build();
    }

    // ------------------------------------------------------------------ Legends' Guild dungeon

    /**
     * The dungeon below the Legends' Guild, its shadow warriors' long cavern running north-south
     * west of the guild (OSRS 2693-2708, 9753-9781: nineteen shadow warriors). In OSRS the stairs
     * down are inside the guild; here a hut just west of its grounds (tile 2706, 3369) leads down
     * (approx).
     */
    static Dungeons.Dungeon legendsGuildDungeon() {
        Cave c = new Cave("dng_legends_guild_dungeon", "Legends' Guild dungeon", mc("stone_bricks"), mc("andesite"),
                mc("glowstone"));
        int f = 26;
        int ex = bx(2706);
        int ez = bz(3369);
        tRoom(c, 2692, 3364, 2710, 3384, f, 7);                                // northern cavern
        c.hallZ(bx(2700), bz(3364), bx(2700), bz(3360), f, 5, 5);
        tRoom(c, 2694, 3348, 2712, 3360, f, 7);                                // southern cavern
        c.carve();
        for (int[] p : new int[][]{{2696, 3368}, {2696, 3380}, {2704, 3380}, {2700, 3352}, {2708, 3356}}) {
            c.pillar(bx(p[0]), bz(p[1]), f, 7, mc("chiseled_stone_bricks"));
        }
        c.fill(bx(2693), f + 1, bz(3384), bx(2693), f + 1, bz(3376), mc("bookshelf"));
        c.set(bx(2709), f + 1, bz(3350), mc("chest[facing=west]"));
        c.entrance(ex, ez, f, mc("stone_bricks"), mc("andesite"), 'w');
        tCamp(c, 2693, 3365, 2709, 3383, f, 4, 35, "shadow_warrior");
        tCamp(c, 2695, 3349, 2711, 3359, f, 3, 35, "shadow_warrior");
        return c.build();
    }

    // ------------------------------------------------------------------ Wilderness God Wars Dungeon

    /**
     * The Wilderness God Wars Dungeon (entrance cave tile 3017, 3738, level 26-ish Wilderness): a
     * passage east into the great cavern where the four gods' spiritual warriors, rangers and mages
     * fight on (OSRS 3022-3047, 10115-10160), with the bloodveld nest to the south. Being in the
     * Wilderness, it is a PvP area like the cave in OSRS.
     */
    static Dungeons.Dungeon wildernessGodWars() {
        Cave c = new Cave("dng_wilderness_god_wars", "Wilderness God Wars Dungeon", mc("deepslate"), mc("tuff"),
                mc("shroomlight"));
        int f = 16;
        int ex = bx(3017);
        int ez = bz(3738);
        c.room(ex - 5, ez - 5, ex + 5, ez + 5, f, 5);
        c.hall(ex + 5, ez, bx(3024), ez, f, 3, 4);
        tRoom(c, 3024, 3724, 3050, 3758, f, 10);                               // the great cavern
        c.hallZ(bx(3036), bz(3724), bx(3036), bz(3718), f, 3, 4);
        tRoom(c, 3028, 3708, 3046, 3718, f, 6);                                // bloodveld nest
        c.carve();
        // Each god's corner: banners of Armadyl (north-west), Bandos (north-east), Saradomin
        // (south-west) and Zamorak (south-east).
        int x1 = bx(3024) + 2;
        int x2 = bx(3050) - 1;
        int z1 = bz(3758) + 2;
        int z2 = bz(3724) - 1;
        c.pillar(x1, z1, f, 10, mc("white_wool")).set(x1, f + 10, z1, mc("light_blue_wool"));
        c.pillar(x2, z1, f, 10, mc("brown_wool")).set(x2, f + 10, z1, mc("green_wool"));
        c.pillar(x1, z2, f, 10, mc("blue_wool")).set(x1, f + 10, z2, mc("yellow_wool"));
        c.pillar(x2, z2, f, 10, mc("red_wool")).set(x2, f + 10, z2, mc("black_wool"));
        c.pool(bx(3035), bz(3743), bx(3039), bz(3739), f, mc("crying_obsidian"));
        for (int x = bx(3028); x <= bx(3046); x += 9) {
            c.pillar(x, bz(3750), f, 10, mc("basalt[axis=y]"));
            c.pillar(x, bz(3730), f, 10, mc("basalt[axis=y]"));
        }
        c.entrance(ex, ez, f, mc("deepslate"), mc("tuff"), 'w');
        tCamp(c, 3025, 3725, 3049, 3757, f, 7, 35, "spiritual_warrior", "spiritual_ranger", "spiritual_mage");
        tCamp(c, 3029, 3709, 3045, 3717, f, 3, 35, "bloodveld");
        return c.build();
    }

    // ------------------------------------------------------------------ Jormungand's Prison

    /**
     * Jormungand's Prison (The Fremennik Exiles), below the Island of Stone north of the Fremennik
     * Isles: the island's west end (a levelled shelf of rock beside the quest's ruins, with the
     * entrance at tile 2461, 3993), the shaft down, and the prison's two halls of basilisk knights (OSRS 2403-2434 and 2450-2460,
     * 10375-10397: twenty knights) beside the bones of the great serpent.
     */
    static Dungeons.Dungeon jormungandsPrison() {
        Cave c = new Cave("dng_jormungands_prison", "Jormungand's Prison", mc("deepslate_tiles"), mc("polished_deepslate"),
                mc("sea_lantern"));
        int f = 8;
        int ex = bx(2461);
        int ez = bz(3993);
        c.room(ex - 6, ez - 6, ex + 6, ez + 6, f, 6);
        c.hall(ex - 6, ez, bx(2462), ez, f, 3, 5);
        tRoom(c, 2446, 3974, 2462, 3996, f, 9);                                // eastern hall
        c.hall(bx(2446), bz(3986), bx(2436), bz(3986), f, 5, 6);
        tRoom(c, 2400, 3972, 2436, 3999, f, 12);                               // the serpent's hall
        c.carve();
        // Jormungand's bones: a spine curling through the western hall, with ribs.
        int sz = bz(3986);
        for (int x = bx(2404); x <= bx(2432); x++) {
            int z = sz + (int) Math.round(8 * Math.sin((x - bx(2404)) / 9.0));
            c.set(x, f + 1, z, mc("bone_block[axis=x]"));
            if ((x - bx(2404)) % 6 == 0) {
                c.fill(x, f + 2, z - 3, x, f + 2, z + 3, mc("bone_block[axis=z]"));
            }
        }
        c.fill(bx(2433), f + 1, sz - 3, bx(2435), f + 4, sz + 3, mc("bone_block[axis=y]"));
        c.pool(bx(2450), bz(3992), bx(2454), bz(3988), f, mc("water"));
        for (int[] p : new int[][]{{2448, 3976}, {2460, 3976}, {2448, 3994}, {2460, 3994}}) {
            c.pillar(bx(p[0]), bz(p[1]), f, 9, mc("chiseled_deepslate"));
        }
        // The Island of Stone: its ruins are the quest module's (an islet around tile 2470, 3990); a
        // shelf of rock levelled west of them carries the entrance hut and counts as part of the
        // entrance (the only part of the prison above ground). Levelled first, so the hut stands on it.
        int ix1 = bx(2454);
        int ix2 = bx(2468) + 1;
        int iz1 = bz(4000);
        int iz2 = bz(3986) + 1;
        c.b.flatten(ix1, iz1, ix2, iz2, mc("stone"));
        c.entrances.add(new Dungeons.Entrance(ix1, iz1, ix2, iz2, StructureDef.GROUND + 24, true));
        c.entrance(ex, ez, f, mc("deepslate_bricks"), mc("polished_deepslate"), 's');
        tCamp(c, 2401, 3973, 2435, 3998, f, 4, 50, "basilisk_knight");
        tCamp(c, 2447, 3975, 2461, 3995, f, 3, 50, "basilisk_knight");
        return c.build();
    }
}
