package com.runecraft.osrs.world;

import java.util.List;

/**
 * Hand-authored physical geography of Gielinor in real Old School RuneScape tile coordinates
 * (x grows east, y grows north; Lumbridge is 3222, 3218). {@link Elevation} turns this into terrain
 * heights. Heights are absolute Minecraft Y (sea level 62, towns at 64).
 *
 * <p>Coastlines were traced from the OSRS world map and checked against the hand-built harbours
 * of the town structures (Port Sarim, Musa Point, Brimhaven, Catherby, Rellekka, the Lighthouse,
 * Piscatoris, Port Khazard, Witchaven). Points marked {@code approx} are from memory of the map.
 */
public final class GielinorGeography {
    private GielinorGeography() {
    }

    /** A closed polygon (land) as x0, y0, x1, y1, ... */
    public record Polygon(String name, double[] xy) {
    }

    /** An elliptical island/landmass or area. */
    public record Ellipse(String name, double x, double y, double rx, double ry) {
    }

    /** A mountain ridge: polyline points (x, y, peakY) with a half-width in tiles. */
    public record Ridge(String name, double halfWidth, double[] xyh) {
    }

    /** A ring of mountains around a centre (e.g. Prifddinas). */
    public record Ring(String name, double x, double y, double radius, double halfWidth, double peakY) {
    }

    /** A flat-topped plateau with steep edges. */
    public record Mesa(String name, double x, double y, double radius, double topY) {
    }

    /** A river: polyline (x, y, ...) with a width in tiles. */
    public record River(String name, double width, double[] xy) {
    }

    // ------------------------------------------------------------------ landmasses

    /** The main continent: Misthalin, Asgarnia, Kandarin, Wilderness, Morytania, the desert, Feldip, Tirannwn, Fremennik. */
    public static final Polygon MAINLAND = new Polygon("Gielinor mainland", new double[]{
            // Rimmington strait (Karamja lies just south), then around the Port Sarim / Draynor inlet
            2975, 3196, 2983, 3170, 2984, 3125, 2992, 3090, 3015, 3088, 3026, 3115, 3026, 3170, 3027, 3200,
            3029, 3240, 3060, 3240, 3072, 3220, 3112, 3219, 3134, 3220, 3141, 3210, 3134, 3196,
            // Wizards' Tower peninsula and the Lumbridge Swamp coast
            3086, 3194, 3080, 3165, 3084, 3132, 3110, 3127, 3135, 3130, 3175, 3137, 3215, 3139, 3245, 3134,
            // Al Kharid's shrimp bay, then the Kharidian Desert's west coast (approx)
            3262, 3141, 3272, 3134, 3255, 3125, 3225, 3119, 3196, 3110, 3172, 3088, 3150, 3045, 3132, 2985,
            3124, 2900, 3122, 2820, 3140, 2750, 3180, 2714,
            // south coast of the desert (Menaphos, Sophanem, the Necropolis) and the east coast (approx)
            3260, 2700, 3340, 2690, 3410, 2686, 3480, 2706, 3515, 2760, 3535, 2840, 3548, 2920, 3556, 3000,
            3562, 3075, 3595, 3120, 3645, 3146,
            // Morytania: Ver Sinhaza, Slepe, Port Phasmatys, the north coast to the River Salve (approx)
            3700, 3158, 3726, 3200, 3748, 3250, 3772, 3300, 3770, 3345, 3748, 3400, 3738, 3450, 3708, 3490,
            3712, 3515, 3735, 3545, 3700, 3572, 3640, 3588, 3570, 3598, 3490, 3605, 3425, 3618,
            // the Wilderness: east coast and the frozen north coast
            3418, 3700, 3420, 3800, 3412, 3900, 3398, 3985, 3300, 3988, 3200, 3990, 3100, 3988, 3000, 3990,
            2900, 3986, 2845, 3970,
            // Weiss / Trollweiss, then the Fremennik coast to Rellekka's harbour
            2850, 3900, 2830, 3840, 2800, 3790, 2760, 3758, 2700, 3738, 2656, 3697, 2604, 3695, 2597, 3680,
            // the Lighthouse rocks, a small bay, Piscatoris
            2560, 3666, 2532, 3646, 2497, 3645, 2496, 3620, 2470, 3608, 2452, 3635, 2440, 3672, 2400, 3684,
            2372, 3660, 2326, 3659, 2300, 3652, 2275, 3628, 2252, 3590,
            // Tirannwn's west coast (approx), Port Tyras, Zul-Andra
            2226, 3540, 2192, 3480, 2162, 3430, 2142, 3370, 2131, 3290, 2123, 3210, 2117, 3170, 2118, 3135,
            2122, 3102, 2150, 3094, 2170, 3066, 2176, 3030,
            // the Poison Waste coast, Feldip Hills' south coast (Myths' Guild, Corsair Cove)
            2230, 3010, 2300, 3014, 2355, 3004, 2392, 2965, 2410, 2900, 2418, 2846, 2470, 2832, 2530, 2834,
            2562, 2826, 2600, 2838, 2640, 2878,
            // Feldip's east coast, Yanille, Port Khazard, Witchaven, Legends' Guild, Keep Le Faye
            2675, 2930, 2690, 2990, 2676, 3040, 2652, 3078, 2648, 3110, 2674, 3128, 2677, 3150, 2677, 3186,
            2700, 3206, 2722, 3232, 2735, 3262, 2735, 3305, 2750, 3335, 2748, 3380, 2766, 3384, 2788, 3398,
            // Catherby beach, Taverley's shore, the Asgarnian west coast down to Rimmington
            2790, 3419, 2845, 3420, 2862, 3404, 2876, 3384, 2900, 3360, 2903, 3330, 2900, 3290, 2902, 3240,
            2905, 3197
    });

    public static final Polygon KARAMJA = new Polygon("Karamja", new double[]{
            2735, 3236, 2746, 3236, 2802, 3236, 2812, 3215, 2860, 3206, 2890, 3190, 2948, 3188, 2948, 3165,
            2946, 3132, 2965, 3110, 2972, 3060, 2985, 3000, 2978, 2940, 2955, 2895, 2900, 2878, 2840, 2872,
            2800, 2880, 2785, 2905, 2795, 2935, 2790, 2965, 2782, 2995, 2768, 3022, 2745, 3055, 2722, 3110,
            2720, 3180, 2730, 3220
    });

    /** Great Kourend (Zeah) and Varlamore, a separate landmass far to the west. Mostly approx. */
    public static final Polygon ZEAH = new Polygon("Zeah and Varlamore", new double[]{
            1160, 3880, 1250, 3905, 1380, 3890, 1470, 3905, 1550, 3915, 1575, 4045, 1700, 4050, 1725, 3925,
            1790, 3885, 1842, 3840, 1852, 3780, 1846, 3700, 1842, 3620, 1832, 3545, 1800, 3498, 1760, 3478,
            1725, 3445, 1770, 3405, 1840, 3350, 1880, 3250, 1892, 3150, 1872, 3050, 1842, 2965, 1750, 2922,
            1650, 2905, 1550, 2930, 1450, 2928, 1370, 2950, 1290, 2990, 1232, 3060, 1200, 3150, 1190, 3250,
            1182, 3330, 1172, 3420, 1172, 3500, 1205, 3560, 1180, 3640, 1170, 3720, 1160, 3800
    });

    public static final Polygon FOSSIL_ISLAND = new Polygon("Fossil Island", new double[]{
            3650, 3735, 3700, 3722, 3790, 3726, 3842, 3765, 3852, 3830, 3832, 3898, 3770, 3910, 3700, 3900,
            3648, 3884, 3636, 3800
    });

    public static final List<Polygon> POLYGONS = List.of(MAINLAND, KARAMJA, ZEAH, FOSSIL_ISLAND);

    public static final List<Ellipse> ISLANDS = List.of(
            new Ellipse("Tutorial Island", 3104, 3102, 46, 34),
            new Ellipse("Crandor", 2836, 3262, 24, 38),
            new Ellipse("Entrana", 2836, 3340, 38, 30),
            new Ellipse("Cairn Isle", 2763, 2976, 15, 14),
            new Ellipse("Ape Atoll", 2755, 2768, 58, 55),
            new Ellipse("Void Knights' Outpost", 2655, 2658, 42, 40),
            new Ellipse("Isle of Souls", 2210, 2900, 70, 60), // approx
            new Ellipse("Harmony Island", 3795, 2861, 30, 30),
            new Ellipse("Mos Le'Harmless", 3712, 2985, 66, 62),
            new Ellipse("Dragontooth Island", 3808, 3555, 26, 26),
            new Ellipse("Lithkren", 3548, 3998, 32, 30),
            new Ellipse("Miscellania", 2534, 3868, 54, 32),
            new Ellipse("Etceteria", 2618, 3880, 24, 24),
            new Ellipse("Waterbirth Island", 2530, 3748, 26, 32),
            new Ellipse("Neitiznot", 2336, 3804, 30, 36),
            new Ellipse("Jatizso", 2406, 3803, 34, 34),
            new Ellipse("Lunar Isle", 2110, 3905, 68, 58),
            new Ellipse("Pirates' Cove", 2212, 3805, 28, 28),
            new Ellipse("Ungael", 2256, 4048, 30, 30),
            new Ellipse("Iceberg", 2660, 4010, 14, 12),
            // instanced areas that the mod places at their OSRS instance coordinates
            new Ellipse("Zanaris", 2412, 4432, 48, 44),
            new Ellipse("Prifddinas", 3180, 6105, 170, 95)
    );

    // ------------------------------------------------------------------ mountains

    public static final List<Ridge> RIDGES = List.of(
            new Ridge("White Wolf Mountain", 18, new double[]{2800, 3500, 110, 2822, 3492, 124, 2846, 3480, 120, 2858, 3455, 104}),
            new Ridge("Ice Mountain", 22, new double[]{3008, 3478, 124, 3009, 3479, 124}),
            new Ridge("Death Plateau and Trollheim", 30, new double[]{2852, 3604, 118, 2872, 3640, 140, 2890, 3678, 128,
                    2912, 3720, 160, 2930, 3760, 152, 2905, 3810, 168, 2870, 3860, 152, 2830, 3882, 130}),
            new Ridge("Troll Stronghold crags", 24, new double[]{2805, 3660, 124, 2843, 3690, 118, 2880, 3705, 140}),
            new Ridge("Weiss", 30, new double[]{2870, 3935, 142, 2871, 3936, 142}), // approx
            new Ridge("Wilderness western range", 20, new double[]{2952, 3620, 108, 2956, 3700, 128, 2946, 3790, 140,
                    2962, 3880, 146, 2992, 3942, 128}),
            new Ridge("Wilderness north-east mountains", 28, new double[]{3300, 3820, 112, 3350, 3880, 132, 3330, 3950, 122}),
            new Ridge("Fremennik mountains", 26, new double[]{2680, 3600, 108, 2720, 3640, 136, 2760, 3680, 152,
                    2790, 3720, 146, 2800, 3762, 128}),
            new Ridge("Karamja highlands", 22, new double[]{2800, 3110, 90, 2850, 3090, 102, 2905, 3060, 96, 2920, 3020, 86}),
            new Ridge("Feldip Hills", 20, new double[]{2600, 3000, 110, 2640, 2965, 106, 2655, 2920, 96}),
            new Ridge("Arandar mountains (south)", 22, new double[]{2360, 3205, 130, 2348, 3250, 160, 2335, 3288, 140}),
            new Ridge("Arandar mountains (north)", 22, new double[]{2326, 3342, 150, 2305, 3395, 176, 2265, 3430, 166, 2210, 3430, 140}),
            new Ridge("Eagles' Peak", 20, new double[]{2330, 3490, 140, 2331, 3491, 140}), // approx
            new Ridge("Morytania southern ridge", 18, new double[]{3450, 3165, 104, 3520, 3150, 116, 3600, 3150, 110, 3660, 3135, 94}),
            new Ridge("Haunted Woods hills", 20, new double[]{3560, 3440, 94, 3620, 3470, 104}), // approx
            new Ridge("Bedabin ridge", 14, new double[]{3165, 3060, 94, 3190, 3075, 100}), // approx
            new Ridge("Eastern desert hills", 18, new double[]{3480, 2980, 90, 3500, 3040, 96}), // approx
            new Ridge("Mount Quidamortem", 34, new double[]{1245, 3565, 176, 1246, 3566, 176}),
            new Ridge("Mount Karuulm", 28, new double[]{1311, 3807, 166, 1312, 3808, 166}),
            new Ridge("Lovakengj mountains", 25, new double[]{1430, 3820, 120, 1490, 3870, 132}),
            new Ridge("Wintertodt plateau", 45, new double[]{1630, 3990, 112, 1631, 3991, 112}),
            new Ridge("Arceuus hill", 18, new double[]{1715, 3880, 100, 1716, 3881, 100}), // approx
            new Ridge("Ralos' Rise", 40, new double[]{1450, 3115, 172, 1451, 3116, 172}), // approx
            new Ridge("Varlamore cliffs", 18, new double[]{1260, 3330, 120, 1400, 3370, 130, 1560, 3385, 116}) // approx
    );

    public static final List<Ring> RINGS = List.of(
            new Ring("Prifddinas' mountains (Tirannwn)", 2240, 3300, 50, 14, 172),
            new Ring("Prifddinas' mountains", 3264, 6104, 95, 16, 166)
    );

    public static final List<Mesa> MESAS = List.of(
            new Mesa("Kharidian plateau", 3282, 2950, 24, 84) // approx
    );

    // ------------------------------------------------------------------ water

    public static final List<River> RIVERS = List.of(
            // From Barbarian Village south past Draynor Manor and the Lumbridge farms into the River Lum.
            new River("Barbarian Village river", 4, new double[]{3104, 3454, 3104, 3398, 3112, 3391, 3130, 3388, 3143, 3374,
                    3150, 3348, 3165, 3330, 3185, 3325, 3220, 3326, 3238, 3320, 3243.5, 3300}),
            // Between the chicken farm and the cow field, east of Lumbridge Castle (matches the town's
            // hand-built river), bending east below the bridge and south through the swamp to the sea.
            new River("River Lum", 4, new double[]{3243.5, 3300, 3243.5, 3270, 3241, 3262, 3241, 3221, 3256, 3221,
                    3256, 3197, 3256, 3182, 3250, 3160, 3245, 3142, 3245, 3126}),
            // The Morytanian border: from the north coast past the Slayer Tower and Paterdomus into Mort Myre.
            new River("River Salve", 6, new double[]{3396, 3628, 3398, 3560, 3404, 3528, 3432, 3510, 3442, 3485,
                    3442, 3440, 3430, 3390, 3424, 3345}),
            // Through the desert between Pollnivneach and Nardah to the sea at the Necropolis.
            new River("River Elid", 5, new double[]{3392, 3075, 3390, 3010, 3390, 2940, 3390, 2880, 3392, 2830,
                    3388, 2785, 3385, 2740, 3388, 2680}),
            // From Baxtorian Lake past the Fishing Guild to Hemenster's fishing pond (approx).
            new River("Baxtorian river", 4, new double[]{2518, 3482, 2545, 3462, 2572, 3446, 2595, 3432,
                    2620, 3426}),
            new River("Isafdar river", 5, new double[]{2305, 3240, 2265, 3215, 2210, 3205, 2165, 3196, 2110, 3192}), // approx
            new River("Feldip river", 4, new double[]{2600, 3005, 2630, 3030, 2660, 3048, 2690, 3052}), // approx
            new River("Varlamore river", 6, new double[]{1450, 3070, 1470, 3000, 1480, 2918}) // approx
    );

    public static final List<Ellipse> LAKES = List.of(
            new Ellipse("Baxtorian Lake", 2516, 3494, 12, 10),
            new Ellipse("Lake Molch", 1366, 3632, 16, 12),
            new Ellipse("Source of the Elid", 3392, 3080, 6, 6)
    );

    public static final List<Ellipse> SWAMPS = List.of(
            new Ellipse("Lumbridge Swamp", 3195, 3166, 58, 28),
            new Ellipse("Mort Myre Swamp", 3460, 3385, 40, 55),
            new Ellipse("Kharazi Jungle wetland", 2870, 2905, 75, 22),
            new Ellipse("Kebos Swamp", 1255, 3655, 50, 45) // approx
    );
}
