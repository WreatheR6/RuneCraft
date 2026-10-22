package com.runecraft.osrs.content.dungeons;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;

/**
 * Slayer dungeons of the Sailing islands (OSRS Wiki, Sailing release of 19 November 2025; Kiyogitpy/osrs-wiki-data, July
 * 2026), every monster on the wiki's LocLine tiles:
 * <ul>
 *   <li>the Grimstone Dungeon below Grimstone (cave entrance 2912, 4066): frost dragons in the central cavern among chilled
 *   jellies, and the cave west of it where they can only be killed on a frost dragon task (since 3 December 2025);</li>
 *   <li>the Great Conch's two gryphon caves: the north-western one (entrance 3120, 2489) open to all, the south-eastern one
 *   (entrance 3235, 2417) task-only, kept by Elder Kelmo; the southern room of each is multicombat.</li>
 * </ul>
 * The caves lie under their islands: each instance map is shifted so the landing is below its entrance (the landings
 * aren't in the transport data: approx). The Ancient Furnace, the ancient mould, the fairy ring (DLP), the rock-face
 * climbs to the west cave (the southern one 83 Agility) and the knife spawn are not built.
 */
final class SeaSlayerDungeons {
    static final int RESPAWN = CacheDungeons.RESPAWN;
    /** Frost dragons respawn after 7 ticks (OSRS Wiki infobox; 4.2 s since 3 December 2025, down from 18 s). */
    static final int FROST_DRAGON_RESPAWN = 7;
    /** Chilled jellies respawn after 15 ticks (OSRS Wiki infobox). */
    static final int CHILLED_JELLY_RESPAWN = 15;

    /** Grimstone's cave entrance (2912, 4066) over the instance tile (2912, 10466). */
    static final int GRIMSTONE_DY = 4066 - 10466;
    static final int[] FROST_DRAGONS = {2851, 10455, 2851, 10471, 2853, 10462, 2860, 10466, 2862, 10449, 2862, 10458, 2864, 10473,
            2873, 10451, 2872, 10463, 2874, 10471, 2889, 10466, 2890, 10454, 2894, 10461, 2895, 10467, 2898, 10456, 2898, 10473,
            2900, 10465, 2905, 10459, 2911, 10458, 2913, 10465};
    static final int[] GRIMSTONE_JELLIES = {2898, 10448, 2900, 10445, 2902, 10449, 2903, 10444, 2905, 10446, 2917, 10478,
            2919, 10476, 2919, 10481, 2921, 10480, 2922, 10477};
    /** The western cave where frost dragons are task-only. */
    static final int[] GRIMSTONE_WEST = {2846, 10445, 2864, 10477};
    static final int[] GRIMSTONE_CENTRAL = {2868, 10443, 2924, 10482};

    /** The north-western gryphon cave's entrance (3120, 2489) over its north-eastern corner (3120, 8878). */
    static final int GRYPHON_WEST_DY = 2489 - 8878;
    static final int[] GRYPHONS_WEST = {3084, 8868, 3088, 8864, 3089, 8870, 3089, 8878, 3092, 8874, 3093, 8865, 3103, 8856,
            3105, 8851, 3106, 8875, 3108, 8847, 3111, 8870, 3112, 8852, 3112, 8857, 3113, 8875, 3116, 8869, 3118, 8855, 3119, 8877,
            3123, 8865};
    /** The task-only south-eastern cave's entrance (3235, 2417) over its southern edge (3235, 8841). */
    static final int GRYPHON_EAST_DY = 2417 - 8841;
    static final int[] GRYPHONS_EAST = {3210, 8844, 3213, 8871, 3214, 8851, 3215, 8846, 3215, 8865, 3219, 8841, 3219, 8854,
            3219, 8870, 3220, 8847, 3222, 8875, 3226, 8871, 3227, 8877, 3233, 8859, 3235, 8865, 3237, 8854, 3240, 8861, 3245, 8857,
            3245, 8865};
    public static final String KELMO = "Elder Kelmo";

    private SeaSlayerDungeons() {
    }

    static List<Dungeons.Dungeon> all() {
        return List.of(grimstone(), gryphonCaveWest(), gryphonCaveEast());
    }

    static Dungeons.Dungeon grimstone() {
        Cave c = new Cave("dng_grimstone_dungeon", "Grimstone Dungeon", mc("packed_ice"), mc("snow_block"), mc("sea_lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, GRIMSTONE_DY);
        int f = 20;
        int[][] rooms = {GRIMSTONE_CENTRAL, GRIMSTONE_WEST};
        for (int[] r : rooms) {
            s.room(r, f, 8);
        }
        s.hall(2864, 10462, 2868, 10462, f);
        c.carve();
        for (int[] p : new int[][]{{2880, 10450}, {2880, 10475}, {2906, 10470}, {2855, 10450}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 8, mc("blue_ice"));
        }
        c.entrance(s.x(2912), s.z(10466), f, mc("polished_deepslate"), mc("packed_ice"), 'w');
        s.taskOnly(GRIMSTONE_WEST, f, 8, "frost_dragons", null);
        s.camps(rooms, f, FROST_DRAGON_RESPAWN, "frost_dragon", FROST_DRAGONS);
        s.camps(rooms, f, CHILLED_JELLY_RESPAWN, "chilled_jelly", GRIMSTONE_JELLIES);
        return c.build();
    }

    static Dungeons.Dungeon gryphonCaveWest() {
        Cave c = new Cave("dng_gryphon_cave_west", "Gryphon cave (north-west)", mc("tuff"), mc("sand"), mc("glowstone"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, GRYPHON_WEST_DY);
        int f = 22;
        int[] north = {3082, 8859, 3125, 8880};
        int[] south = {3100, 8844, 3122, 8860};
        int[][] rooms = {north, south};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        c.carve();
        c.entrance(s.x(3120), s.z(8878), f, mc("sandstone"), mc("smooth_sandstone"), 'w');
        s.multi(south, f, 7);
        s.camps(rooms, f, RESPAWN, "gryphon", GRYPHONS_WEST); // the wiki gives no gryphon respawn: approx
        return c.build();
    }

    static Dungeons.Dungeon gryphonCaveEast() {
        Cave c = new Cave("dng_gryphon_cave_east", "Gryphon cave (south-east)", mc("tuff"), mc("sand"), mc("glowstone"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, 0, GRYPHON_EAST_DY);
        int f = 22;
        int[] north = {3208, 8858, 3248, 8879};
        int[] south = {3208, 8839, 3248, 8859};
        int[][] rooms = {north, south};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        c.carve();
        c.entrance(s.x(3235), s.z(8841), f, mc("sandstone"), mc("smooth_sandstone"), 'n');
        s.multi(new int[]{3208, 8839, 3226, 8859}, f, 7);   // the south-western part
        for (int[] r : rooms) {
            s.taskOnly(r, f, 7, "gryphons", KELMO);
        }
        s.camps(rooms, f, RESPAWN, "gryphon", GRYPHONS_EAST);
        return c.build();
    }
}
