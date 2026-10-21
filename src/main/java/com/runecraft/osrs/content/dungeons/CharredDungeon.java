package com.runecraft.osrs.content.dungeons;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The Charred Dungeon below Charred Island (Sailing 60): the lava strykewyrms' cavern in the east and the fire giants,
 * hellhounds, lesser demons, black, red and baby black dragons, nechryael and black demons around it, every monster on
 * the wiki's LocLine tiles (Kiyogitpy/osrs-wiki-data, July 2026). The dark hole stands on the island (2637, 2416); its
 * landing tile is not in the transport data, so the shift puts it among the lesser demons by the south-west mine
 * (approx). The rooms are boxes around the clusters of the wiki's monster map; the mines' 10 coal, 3 adamantite and 2
 * runite rocks stand in the south-west (rows approx) and the 24 rubium rocks in the south-central mine on their wiki
 * tiles (Kragin, his two rubium deposits and the old chest are not built). In OSRS everything but the strykewyrms is
 * multicombat; here the whole dungeon is ({@code MultiCombat.UNDERGROUND}, approx for the strykewyrms).
 */
final class CharredDungeon {
    static final int RESPAWN = CacheDungeons.RESPAWN;
    static final int DRAGON_RESPAWN = CacheDungeons.DRAGON_RESPAWN;
    /** The dark hole (2637, 2416) over cache tile (2632, 8845). */
    static final int CD_DX = 2637 - 2632;
    static final int CD_DY = 2416 - 8845;
    static final int[] CD_FIRE_GIANTS = {2630, 8888, 2632, 8885, 2632, 8889, 2635, 8883, 2636, 8886, 2639, 8884};
    static final int[] CD_HELLHOUNDS = {2626, 8861, 2629, 8864, 2630, 8859, 2634, 8861, 2635, 8866};
    static final int[] CD_LESSER_DEMONS = {2627, 8841, 2628, 8835, 2630, 8838, 2631, 8836, 2637, 8837, 2642, 8836};
    static final int[] CD_BLACK_DRAGONS = {2643, 8851, 2644, 8869, 2647, 8863, 2649, 8853, 2652, 8858};
    static final int[] CD_RED_DRAGONS = {2661, 8867, 2666, 8861, 2668, 8868, 2672, 8863};
    static final int[] CD_BABY_BLACK_DRAGONS = {2648, 8875, 2652, 8879, 2657, 8876, 2659, 8843, 2664, 8847, 2667, 8842, 2670, 8848};
    static final int[] CD_NECHRYAEL = {2695, 8850, 2697, 8848, 2698, 8854, 2700, 8850, 2702, 8855, 2708, 8880, 2711, 8878, 2711, 8883,
            2713, 8881, 2715, 8879, 2718, 8881};
    static final int[] CD_BLACK_DEMONS = {2692, 8888, 2696, 8885, 2700, 8888};
    static final int[] CD_LAVA_STRYKEWYRMS = {2714, 8869, 2718, 8867, 2719, 8863, 2723, 8864, 2724, 8860, 2725, 8856, 2726, 8882,
            2728, 8887, 2730, 8881, 2733, 8884, 2734, 8888, 2737, 8883};

    /** The 24 rubium rocks of the south-central mine (wiki Rubium rocks object tiles; Kragin's two rubium deposits are not built). */
    static final int[] CD_RUBIUM = {2680, 8836, 2682, 8836, 2684, 8837, 2685, 8839, 2682, 8839, 2681, 8840, 2681, 8842, 2684, 8842,
            2683, 8843, 2684, 8844, 2680, 8844, 2679, 8845, 2683, 8846, 2678, 8848, 2681, 8848, 2682, 8849, 2679, 8850, 2680, 8854,
            2681, 8855, 2684, 8853, 2683, 8852, 2684, 8851, 2685, 8851, 2687, 8849};

    /** Kragin's two rubium deposits in the south-central mine, near his spot (2685, 8850) (wiki Rubium deposit object tiles). */
    static final int[] CD_RUBIUM_DEPOSITS = {2679, 8852, 2685, 8847};

    private CharredDungeon() {
    }

    static Dungeons.Dungeon dungeon() {
        Cave c = new Cave("dng_charred_dungeon", "Charred Dungeon", mc("blackstone"), mc("basalt[axis=y]"), mc("shroomlight"));
        CacheDungeons.Shifted s = new CacheDungeons.Shifted(c, CD_DX, CD_DY);
        int f = 20;
        int[] west = {2622, 8849, 2640, 8892};
        int[] south = {2620, 8828, 2680, 8848};
        int[] dragons = {2641, 8849, 2676, 8882};
        int[] east = {2688, 8845, 2741, 8892};
        int[] rubium = {2676, 8834, 2690, 8857};   // the south-central rubium mine (wiki object tiles)
        int[][] rooms = {west, south, dragons, east, rubium};
        for (int[] r : rooms) {
            s.room(r, f, 7);
        }
        s.hall(2676, 8866, 2688, 8866, f);
        c.carve();
        c.pool(s.x(2726), s.z(8876), s.x(2734), s.z(8870), f, mc("lava"));   // the strykewyrms' lava pools (approx)
        c.pool(s.x(2655), s.z(8872), s.x(2660), s.z(8868), f, mc("lava"));
        c.rocks(s.x(2622), s.z(8830), s.x(2640), s.z(8830), f + 1, 4, rc("coal_rock"));       // 10 coal
        c.rocks(s.x(2622), s.z(8832), s.x(2626), s.z(8832), f + 1, 4, rc("adamantite_rock")); // 3 adamantite
        c.rocks(s.x(2622), s.z(8834), s.x(2623), s.z(8834), f + 1, 2, rc("runite_rock"));     // 2 runite
        for (int i = 0; i < CD_RUBIUM.length; i += 2) {
            c.set(s.x(CD_RUBIUM[i]), f + 1, s.z(CD_RUBIUM[i + 1]), rc("rubium_rock"));         // the 24 rubium rocks
        }
        // Kragin's two rubium deposits (wiki Rubium deposit tiles; Kragin stands at 2685, 8850).
        for (int i = 0; i < CD_RUBIUM_DEPOSITS.length; i += 2) {
            c.set(s.x(CD_RUBIUM_DEPOSITS[i]), f + 1, s.z(CD_RUBIUM_DEPOSITS[i + 1]), rc("rubium_deposit"));
        }
        for (int[] p : new int[][]{{2650, 8880}, {2670, 8855}, {2705, 8865}, {2630, 8875}}) {
            c.pillar(s.x(p[0]), s.z(p[1]), f, 7, mc("polished_basalt[axis=y]"));
        }
        c.entrance(s.x(2632), s.z(8845), f, mc("polished_blackstone_bricks"), mc("blackstone"), 'n');
        s.camps(rooms, f, RESPAWN, "fire_giant", CD_FIRE_GIANTS);
        s.camps(rooms, f, RESPAWN, "hellhound", CD_HELLHOUNDS);
        s.camps(rooms, f, RESPAWN, "lesser_demon", CD_LESSER_DEMONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "black_dragon", CD_BLACK_DRAGONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "red_dragon", CD_RED_DRAGONS);
        s.camps(rooms, f, DRAGON_RESPAWN, "baby_black_dragon", CD_BABY_BLACK_DRAGONS);
        s.camps(rooms, f, RESPAWN, "nechryael", CD_NECHRYAEL);
        s.camps(rooms, f, RESPAWN, "black_demon", CD_BLACK_DEMONS);
        s.camps(rooms, f, RESPAWN, "lava_strykewyrm", CD_LAVA_STRYKEWYRMS);
        return c.build();
    }
}
