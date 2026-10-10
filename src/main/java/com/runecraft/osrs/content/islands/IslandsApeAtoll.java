package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Ape Atoll: the walled monkey city of Marim with its two Monkey Madness gates (west and south),
 * King Awowogei's palace, bamboo huts and palms; the market square (the skilling module's stalls);
 * the stepped temple of Marim (the jungle demon's arena east of it belongs to the quests module); and
 * the Ape Atoll Agility Course south of the city with fairy ring CLR. Kruk's dungeon and his arena
 * south-east of the market are left untouched.
 */
final class IslandsApeAtoll {
    private IslandsApeAtoll() {
    }

    static List<StructureDef> all() {
        return List.of(marim(), market(), temple(), agilityCourse());
    }

    private static final String HUT_WALL = "minecraft:stripped_jungle_log";
    private static final String THATCH = "minecraft:hay_block";

    /** Marim: walls, gates, the palace and the huts. The market square in the middle is its own structure. */
    static StructureDef marim() {
        Builder b = StructureDef.builder("isl_marim", "Marim").replaces(IslandsPlaces.APE_ATOLL);
        // Ground: north town, west quarter (palace) and the south quarter. Kruk's dungeon (south-east)
        // and the temple (east) are not flattened.
        b.tFlatten(2726, 2789, 2776, 2814, GRASS);
        b.tFlatten(2726, 2758, 2755, 2788, GRASS);
        b.tFlatten(2756, 2758, 2769, 2776, GRASS);
        // City walls of jungle wood on stone footings.
        String w = "minecraft:stripped_jungle_wood";
        wall(b, 2727, 2759, 2727, 2783, 5, w);
        wall(b, 2727, 2789, 2727, 2813, 5, w);
        wall(b, 2727, 2813, 2775, 2813, 5, w);
        wall(b, 2727, 2759, 2759, 2759, 5, w);
        wall(b, 2765, 2759, 2769, 2759, 5, w);
        // The west gate (Monkey Madness: the main gate of Marim) and the south gate.
        b.tFill(2727, 2784, 2727, 2788, 4, 4, w);
        b.tFill(2726, 2783, 2728, 2783, 0, 6, "minecraft:jungle_log");
        b.tFill(2726, 2789, 2728, 2789, 0, 6, "minecraft:jungle_log");
        b.tFill(2760, 2759, 2764, 2759, 4, 4, w);
        b.tFill(2759, 2758, 2759, 2760, 0, 6, "minecraft:jungle_log");
        b.tFill(2765, 2758, 2765, 2760, 0, 6, "minecraft:jungle_log");
        torchPost(b, 2725, 2782, "minecraft:jungle_fence");
        torchPost(b, 2725, 2790, "minecraft:jungle_fence");
        b.tFloor(2728, 2785, 2751, 2787, PATH);
        b.tFloor(2761, 2760, 2763, 2772, PATH);
        // King Awowogei's palace in the west quarter: a great hall of jungle wood with a throne.
        b.tBuilding(2731, 2765, 2750, 2781, 2, "minecraft:jungle_planks", "minecraft:mud_bricks", "minecraft:jungle", "east", true);
        b.tFill(2733, 2772, 2735, 2774, -1, -1, "minecraft:red_wool");
        b.tSet(2734, 2773, 0, "minecraft:gold_block");
        b.tSet(2734, 2773, 1, "minecraft:jungle_stairs[facing=east]");
        b.tFill(2736, 2773, 2749, 2773, -1, -1, "minecraft:red_carpet");
        b.tFill(2731, 2765, 2731, 2765, 0, 10, "minecraft:jungle_log");
        b.tFill(2750, 2781, 2750, 2781, 0, 10, "minecraft:jungle_log");
        b.tFill(2750, 2765, 2750, 2765, 0, 10, "minecraft:jungle_log");
        b.tFill(2731, 2781, 2731, 2781, 0, 10, "minecraft:jungle_log");
        b.tSet(2741, 2770, 2, "minecraft:lantern[hanging=true]");
        b.tSet(2741, 2776, 2, "minecraft:lantern[hanging=true]");
        // Huts.
        int[][] huts = {{2734, 2798, 3}, {2742, 2806, 3}, {2733, 2808, 2}, {2750, 2801, 3}, {2760, 2808, 3}, {2770, 2805, 2},
                {2742, 2794, 2}, {2768, 2797, 2}, {2758, 2766, 2}, {2766, 2768, 2}};
        for (int[] h : huts) {
            hut(b, h[0], h[1], h[2] * 2, HUT_WALL, THATCH, "minecraft:jungle_planks", "south");
        }
        // Palms.
        for (int[] p : new int[][]{{2729, 2792}, {2752, 2810}, {2774, 2811}, {2737, 2790}, {2729, 2761}, {2754, 2761},
                {2774, 2793}, {2730, 2811}}) {
            palm(b, p[0], p[1], 6);
        }
        b.tFloor(2752, 2789, 2753, 2812, PATH);
        return b.build();
    }

    /** The market square in the heart of Marim (the skilling module's magic and scimitar stalls). */
    static StructureDef market() {
        Builder b = StructureDef.builder("isl_marim_market", "Marim market").replaces(IslandsPlaces.APE_ATOLL_MARKET);
        b.tFlatten(2756, 2777, 2768, 2788, PACKED_MUD);
        b.tFloor(2757, 2778, 2767, 2787, "minecraft:jungle_planks");
        b.tSet(2758, 2785, 0, rc("magic_stall"));
        b.tSet(2766, 2785, 0, rc("scimitar_stall"));
        b.tFill(2757, 2784, 2759, 2786, 3, 3, "minecraft:purple_wool");
        b.tFill(2765, 2784, 2767, 2786, 3, 3, "minecraft:red_wool");
        for (int[] p : new int[][]{{2757, 2784}, {2759, 2786}, {2765, 2784}, {2767, 2786}, {2757, 2786}, {2759, 2784},
                {2765, 2786}, {2767, 2784}}) {
            b.tFill(p[0], p[1], p[0], p[1], 0, 2, "minecraft:bamboo_fence");
        }
        // Banana crates and the fountain.
        b.tSet(2758, 2779, 0, "minecraft:hay_block");
        b.tSet(2766, 2779, 0, "minecraft:hay_block");
        b.tSet(2762, 2780, 0, "minecraft:barrel");
        b.tSetTile(2762, 2783, -1, "minecraft:water");
        lamp(b, 2757, 2781, "minecraft:bamboo_fence");
        lamp(b, 2767, 2781, "minecraft:bamboo_fence");
        return b.build();
    }

    /** The stepped temple of Marim, where the monkey zombies shamble. */
    static StructureDef temple() {
        Builder b = StructureDef.builder("isl_marim_temple", "Marim temple").replaces(IslandsPlaces.APE_ATOLL_TEMPLE);
        b.tFlatten(2781, 2789, 2795, 2807, GRASS);
        String s = MOSSY_STONE;
        b.tFill(2782, 2790, 2794, 2806, 0, 1, s);
        b.tFill(2784, 2792, 2792, 2804, 2, 3, s);
        b.tFill(2786, 2794, 2790, 2802, 4, 5, CRACKED);
        b.tFill(2787, 2796, 2789, 2800, 6, 9, "minecraft:chiseled_stone_bricks");
        b.tClear(2788, 2797, 2788, 2799, 6, 8);
        b.tSet(2788, 2798, 6, "minecraft:soul_campfire");
        // Stairs up the west face.
        for (int i = 0; i < 6; i++) {
            b.tFill(2782 + i, 2797, 2782 + i, 2799, i, i, "minecraft:mossy_stone_brick_stairs[facing=east]");
        }
        torchPost(b, 2781, 2795, "minecraft:mossy_cobblestone_wall");
        torchPost(b, 2781, 2801, "minecraft:mossy_cobblestone_wall");
        b.tSet(2786, 2794, 6, "minecraft:skeleton_skull");
        b.tSet(2790, 2802, 6, "minecraft:skeleton_skull");
        return b.build();
    }

    /** Agility obstacles of the Ape Atoll course, in order (tile, landing tile). */
    static final int[][] OBSTACLES = {
            {2754, 2742, 2752, 2741}, // stepping stone over the pool
            {2752, 2744, 2753, 2748}, // tropical tree (climb up)
            {2752, 2748, 2747, 2748}, // monkeybars
            {2746, 2748, 2742, 2745}, // skull slope
            {2742, 2744, 2756, 2738}, // rope swing
            {2756, 2737, 2769, 2745}, // tropical tree (climb down)
    };

    /** The Ape Atoll Agility Course and fairy ring CLR. */
    static StructureDef agilityCourse() {
        Builder b = StructureDef.builder("isl_ape_atoll_agility", "Ape Atoll Agility Course").replaces(IslandsPlaces.APE_ATOLL_AGILITY);
        b.tFlatten(2732, 2735, 2775, 2752, GRASS);
        b.tFloor(2732, 2735, 2775, 2737, SAND);
        // The pool with its stepping stone.
        b.tFill(2753, 2740, 2757, 2743, -2, -1, "minecraft:water");
        // Tropical trees (climbing obstacles), monkey bars and the skull slope.
        b.tFill(2752, 2744, 2752, 2744, 0, 7, JUNGLE_LOG);
        b.tFill(2751, 2743, 2753, 2745, 7, 8, "minecraft:jungle_leaves[persistent=true]");
        b.tFill(2747, 2748, 2752, 2748, 5, 5, "minecraft:chain[axis=x]");
        b.tFill(2747, 2748, 2747, 2748, 0, 4, JUNGLE_LOG);
        for (int i = 0; i < 4; i++) {
            b.tFill(2746 - i, 2748 - i, 2746 - i, 2748 - i, 0, 3 - i, "minecraft:bone_block");
        }
        b.tFill(2742, 2744, 2742, 2744, 0, 6, JUNGLE_LOG);
        b.tFill(2742, 2743, 2749, 2743, 6, 6, "minecraft:jungle_fence");
        b.tFill(2756, 2737, 2756, 2737, 0, 7, JUNGLE_LOG);
        b.tFill(2755, 2736, 2757, 2738, 7, 8, "minecraft:jungle_leaves[persistent=true]");
        String obstacle = rc(IslandsBlocks.APE_OBSTACLE);
        for (int[] o : OBSTACLES) {
            b.tSet(o[0], o[1], o[0] == 2754 ? -1 : 0, obstacle);
        }
        // Fairy ring CLR at the west of the island.
        fairyRing(b, IslandsStructures.APE_RING);
        for (int[] p : new int[][]{{2734, 2750}, {2762, 2751}, {2773, 2748}, {2766, 2740}, {2738, 2738}}) {
            palm(b, p[0], p[1], 6);
        }
        return b.build();
    }
}
