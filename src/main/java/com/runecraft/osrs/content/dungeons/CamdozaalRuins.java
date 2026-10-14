package com.runecraft.osrs.content.dungeons;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The Ruins of Camdozaal under Ice Mountain (Below Ice Mountain), carved like {@link CacheDungeons}: the ruins entrance
 * on the mountain's west side (2998, 3494) leads to the cache tile (2952, 5762) (skretzo/shortest-path transports).
 * Every room keeps the cache's tile layout under one shift; the rooms are boxes around the wiki/cache features and
 * spawns, the passages between them approx:
 *
 * <ul>
 *   <li>the landing south of the city (2944-2960, 5756-5768);</li>
 *   <li>the central hall with the Sacred Forge (2956, 5810), the Barronite Crusher (2956, 5807), an anvil and the bank
 *       chest east of Ramarno (approx: a bank booth);</li>
 *   <li>the western fishing cavern: four fishing spots, the two preparation tables (2935, 5774 and 2935, 5769) and the
 *       altar (2937, 5770);</li>
 *   <li>the Camdozaal Mines north-west (wiki: 18 barronite rocks, 3 clay, 3 tin, 3 copper) with the other three fishing
 *       spots on its stream;</li>
 *   <li>the golem halls east: 8 flawed and 6 mind golems (cache rubble tiles), 6 body and 7 chaos golems (wiki LocLine).</li>
 * </ul>
 *
 * The quests module's Camdozaal place sits on the real entrance tile, so the hut stands 6 tiles north of it (approx).
 * Not built: the Camdozaal Vault, the bottomless pit to the Lassar Undercity, Ramarno (the forge takes his part in
 * forging the barronite mace), the bronze pickaxe barrels and the frogspawn spot.
 */
final class CamdozaalRuins {
    /** Hut tile (2995, 3500), 6 tiles north of the real entrance; cache landing (2952, 5762). */
    static final int DX = 2995 - 2952;
    static final int DY = 3500 - 5762;
    static final int F = 16;
    static final int RESPAWN = 25; // approx

    static final int[] LANDING = {2944, 5756, 2960, 5768};
    static final int[] FORGE = {2944, 5798, 2972, 5818};
    static final int[] FISHING = {2912, 5764, 2941, 5792};
    static final int[] MINE = {2904, 5804, 2942, 5824};
    static final int[] GOLEMS_WEST = {2972, 5766, 3006, 5792};
    static final int[] GOLEMS_EAST = {3010, 5768, 3028, 5816};

    /** Fishing spots (wiki map pins): four by the altar, three on the mine's stream. */
    static final int[] SPOTS = {2926, 5784, 2928, 5779, 2928, 5777, 2928, 5776, 2922, 5814, 2921, 5807, 2927, 5813};

    /** The 18 barronite rocks (positions approx: rows across the mine east of the stream). */
    static final int[] BARRONITE = {2926, 5822, 2929, 5822, 2932, 5822, 2935, 5822, 2938, 5822, 2925, 5818, 2929, 5818, 2933, 5818,
            2937, 5818, 2940, 5818, 2925, 5811, 2929, 5811, 2933, 5811, 2937, 5811, 2940, 5811, 2929, 5807, 2933, 5807, 2937, 5807};

    private CamdozaalRuins() {
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_ruins_of_camdozaal", "Ruins of Camdozaal", mc("deepslate_bricks"), mc("polished_deepslate"),
                mc("soul_lantern"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, DX, DY);
        int[][] rooms = {LANDING, FORGE, FISHING, MINE, GOLEMS_WEST, GOLEMS_EAST};
        for (int[] r : rooms) {
            s.room(r, F, r == FORGE ? 9 : 7);
        }
        s.hallZ(2952, 5768, 2952, 5798, F);
        s.hall(2944, 5762, 2938, 5766, F);
        s.hallZ(2925, 5792, 2925, 5804, F);
        s.hall(2942, 5812, 2944, 5812, F);
        s.hall(2972, 5800, 2985, 5792, F);
        s.hall(3006, 5780, 3010, 5780, F);
        c.carve();

        // The icy water of the fishing cavern and the mine's stream; the spots stand at their edge.
        c.pool(s.x(2920), s.z(5786), s.x(2927), s.z(5772), F, mc("water"));
        c.pool(s.x(2916), s.z(5816), s.x(2920), s.z(5805), F, mc("water"));
        for (int i = 0; i < SPOTS.length; i += 2) {
            c.set(s.x(SPOTS[i]), F + 1, s.z(SPOTS[i + 1]), rc("camdozaal_fishing_spot"));
        }
        c.set(s.x(2935), F + 1, s.z(5774), rc("camdozaal_preparation_table"));
        c.set(s.x(2935), F + 1, s.z(5769), rc("camdozaal_preparation_table"));
        c.set(s.x(2937), F + 1, s.z(5771), rc("camdozaal_altar"));

        // The Camdozaal Mines: 18 barronite rocks in three rows, and the clay, tin and copper rocks by the entrance.
        for (int i = 0; i < BARRONITE.length; i += 2) {
            c.set(s.x(BARRONITE[i]), F + 1, s.z(BARRONITE[i + 1]), rc("barronite_rock"));
        }
        c.rocks(s.x(2906), s.z(5822), s.x(2914), s.z(5822), F + 1, 8, rc("clay_rock"));
        c.rocks(s.x(2906), s.z(5818), s.x(2914), s.z(5818), F + 1, 8, rc("tin_rock"));
        c.rocks(s.x(2906), s.z(5810), s.x(2914), s.z(5810), F + 1, 8, rc("copper_rock"));

        // The central hall: the Sacred Forge, the Barronite Crusher, an anvil and the bank chest.
        c.set(s.x(2956), F + 1, s.z(5810), rc("camdozaal_sacred_forge"));
        c.set(s.x(2956), F + 1, s.z(5807), rc("barronite_crusher"));
        c.set(s.x(2953), F + 1, s.z(5807), rc("anvil"));
        c.set(s.x(2963), F + 1, s.z(5809), rc("bank_booth"));
        for (int[] p : new int[][]{{2947, 5801}, {2947, 5815}, {2969, 5801}, {2969, 5815}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), F, 9, mc("chiseled_deepslate"));
        }
        c.fill(s.x(2955), F + 1, s.z(5813), s.x(2958), F + 1, s.z(5813), mc("magma_block"));

        // Fallen masonry in the golem halls.
        c.set(s.x(2988), F + 1, s.z(5788), mc("cobbled_deepslate"));
        c.set(s.x(3019), F + 1, s.z(5792), mc("cobbled_deepslate_wall"));

        c.entrance(s.x(2952), s.z(5762), F, mc("deepslate_bricks"), mc("polished_deepslate"), 'w');

        for (com.runecraft.osrs.content.skilling.Camdozaal.Golem g : com.runecraft.osrs.content.skilling.Camdozaal.GOLEMS) {
            s.camps(rooms, F, RESPAWN, g.id(), g.tiles());
        }
        return c.build();
    }
}
