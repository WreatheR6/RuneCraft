package com.runecraft.osrs.data;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where each agility course's obstacles stand, as OSRS tiles {x, y}, one per obstacle in course
 * order. The skilling module places a numbered obstacle block on the surface at each tile the first
 * time its chunk loads (only into open air, so it never overwrites a structure); stepping on them in
 * order runs the lap. Courses built by hand inside their town's structure (Gnome Stronghold, Seers',
 * Rellekka, Ardougne) have no entry here.
 *
 * <p>The tiles follow the real courses' obstacles, at ground level beneath the rooftop ones (the
 * towns' buildings don't have walkable roofs); they are approx. to within a few tiles.
 */
public final class AgilityLayouts {
    private static final Map<AgilityCourse, List<int[]>> TILES = new EnumMap<>(AgilityCourse.class);

    static {
        // Rough wall, tightrope, tightrope, narrow wall, wall, gap, crate (finish by the crate, 3103 3261).
        put(AgilityCourse.DRAYNOR, 3103, 3279, 3098, 3277, 3092, 3276, 3090, 3265, 3086, 3251, 3099, 3253, 3101, 3263);
        // Rough wall by the furnace, tightrope, cable, zip line, tropical tree, roof beams, tightrope, gap.
        put(AgilityCourse.AL_KHARID, 3270, 3198, 3272, 3181, 3265, 3162, 3306, 3159, 3316, 3168, 3320, 3178, 3300, 3184,
                3299, 3193);
        // Rough wall east of the Varrock square, clothes line, gaps, wall, gaps, ledge and edge.
        put(AgilityCourse.VARROCK, 3222, 3417, 3211, 3411, 3200, 3420, 3195, 3417, 3191, 3404, 3212, 3400, 3233, 3402,
                3236, 3406, 3239, 3419);
        // Tall tree, four gaps, pole-vault, two gaps around the Canifis square.
        put(AgilityCourse.CANIFIS, 3508, 3488, 3502, 3499, 3497, 3501, 3487, 3497, 3481, 3489, 3483, 3482, 3506, 3476,
                3507, 3480);
        // Rough wall east of the Falador square, round the north-east roofs and back west along the south.
        put(AgilityCourse.FALADOR, 3032, 3338, 3032, 3335, 3048, 3352, 3048, 3358, 3044, 3361, 3034, 3361, 3027, 3358,
                3012, 3348, 3010, 3346, 3012, 3334, 3015, 3332, 3017, 3331, 3025, 3332);
        // Basket, market stall, banner, gap, tree, rough wall, monkeybars, tree, drying line.
        put(AgilityCourse.POLLNIVNEACH, 3347, 2958, 3350, 2968, 3355, 2976, 3362, 2977, 3365, 2974, 3365, 2984, 3358, 2985,
                3359, 2995, 3361, 2999);
        // Rope swing, log balance, obstacle net, balancing ledge, three crumbling walls (south of the outpost).
        put(AgilityCourse.BARBARIAN_OUTPOST, 2551, 3554, 2551, 3546, 2539, 3546, 2536, 3547, 2537, 3553, 2540, 3553,
                2543, 3553);
        // Obstacle pipe, rope swing, stepping stones, log balance, rocks (inside the course's walled yard).
        put(AgilityCourse.WILDERNESS, 3004, 3928, 3002, 3941, 3007, 3948, 3006, 3957, 2996, 3943);
        // Shayzien Agility Course, east of the Shayzien combat ring (approx.).
        put(AgilityCourse.SHAYZIEN_BASIC, 1552, 3632, 1552, 3638, 1556, 3642, 1561, 3642, 1564, 3637, 1561, 3631);
        put(AgilityCourse.SHAYZIEN_ADVANCED, 1550, 3628, 1547, 3633, 1546, 3642, 1550, 3647, 1557, 3648, 1565, 3647,
                1568, 3640);
        // Colossal Wyrm Remains, south-west of Civitas illa Fortis (approx.).
        put(AgilityCourse.COLOSSAL_WYRM_BASIC, 1652, 2931, 1648, 2937, 1645, 2944, 1650, 2950, 1657, 2948, 1659, 2938);
        put(AgilityCourse.COLOSSAL_WYRM_ADVANCED, 1654, 2928, 1662, 2930, 1667, 2937, 1666, 2946, 1661, 2953, 1653, 2955,
                1645, 2951);
        // Prifddinas course: from the ladder in the north-east (approx.).
        put(AgilityCourse.PRIFDDINAS, 3252, 6108, 3256, 6116, 3262, 6119, 3269, 6118, 3274, 6113, 3279, 6108, 3283, 6102,
                3291, 6094, 3283, 6092, 3276, 6094, 3269, 6099);
    }

    private AgilityLayouts() {
    }

    private static void put(AgilityCourse course, int... xy) {
        if (xy.length != course.obstacles() * 2) {
            throw new IllegalStateException(course + ": " + xy.length / 2 + " tiles for " + course.obstacles() + " obstacles");
        }
        int[][] tiles = new int[xy.length / 2][];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new int[]{xy[2 * i], xy[2 * i + 1]};
        }
        TILES.put(course, List.of(tiles));
    }

    /** The obstacle tiles of a course in order, or an empty list if it is built by hand. */
    public static List<int[]> tiles(AgilityCourse course) {
        return TILES.getOrDefault(course, List.of());
    }
}
