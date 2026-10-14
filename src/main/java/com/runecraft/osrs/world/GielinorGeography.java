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

    /**
     * Great Kourend, the Kebos Lowlands and Varlamore: one landmass, traced from the OSRS world map (mejrs/layers_osrs
     * map squares, 1 px per tile, water told from land by colour, coast simplified to within 2 tiles). Rivers and
     * streams narrower than about 7 tiles are left out of the coast and listed in {@link #RIVERS}; lakes are in
     * {@link #TRACED_LAKES}.
     */
    public static final Polygon ZEAH = new Polygon("Zeah and Varlamore", new double[]{
            1657.5, 2881, 1642.5, 2882, 1637.5, 2887, 1619.5, 2890, 1609.5, 2887, 1603.5, 2898, 1592.5, 2899, 1587, 2903.5,
            1589, 2910.5, 1584, 2920.5, 1594.5, 2922, 1598, 2931.5, 1586, 2949.5, 1586.5, 2960, 1574.5, 2964, 1571, 2959.5,
            1575, 2952.5, 1561.5, 2950, 1543.5, 2959, 1537, 2970.5, 1530.5, 2975, 1513.5, 2971, 1511.5, 2979, 1494, 2984.5,
            1497, 2993.5, 1478, 3010.5, 1475.5, 3022, 1470.5, 3022, 1465.5, 3027, 1461, 3024.5, 1459.5, 3018, 1453.5, 3016,
            1440, 3028.5, 1442, 3032.5, 1434, 3046.5, 1435, 3056.5, 1429, 3068.5, 1409.5, 3090, 1391.5, 3100, 1383, 3114.5,
            1394, 3141.5, 1388, 3165.5, 1397.5, 3178, 1404, 3180.5, 1401.5, 3186, 1384, 3172.5, 1380, 3163.5, 1384, 3143.5,
            1373, 3124.5, 1366, 3124.5, 1365, 3117.5, 1372, 3096.5, 1393, 3072.5, 1392, 3050.5, 1402.5, 3045, 1406, 3060.5,
            1408.5, 3061, 1414, 3053.5, 1415, 3039.5, 1418.5, 3037, 1426.5, 3043, 1437, 3026.5, 1431.5, 3026, 1421.5, 3036,
            1418.5, 3033, 1407.5, 3036, 1396.5, 3034, 1389.5, 3025, 1383.5, 3030, 1380.5, 3029, 1374.5, 3019, 1368, 3017.5,
            1366.5, 3009, 1354.5, 3009, 1350.5, 3017, 1342.5, 3010, 1327.5, 3009, 1323.5, 3012, 1302.5, 2997, 1286.5, 3003,
            1278, 2994.5, 1277, 2988.5, 1268.5, 2982, 1252.5, 2982, 1245.5, 2985, 1238, 2992.5, 1243, 2998.5, 1241, 3001.5,
            1243, 3006.5, 1239.5, 3010, 1227, 3012.5, 1227, 3017.5, 1221, 3021.5, 1224, 3025.5, 1217, 3043.5, 1221, 3057.5,
            1220, 3067.5, 1228.5, 3071, 1231, 3077.5, 1219.5, 3084, 1204.5, 3082, 1194, 3092.5, 1194, 3099.5, 1183, 3113.5,
            1190, 3116.5, 1197.5, 3130, 1203.5, 3130, 1219, 3143.5, 1220, 3148.5, 1216, 3152.5, 1216, 3180.5, 1222, 3186.5,
            1218, 3192.5, 1219.5, 3199, 1224.5, 3200, 1229.5, 3194, 1244.5, 3190, 1248.5, 3186, 1254.5, 3188, 1257.5, 3185,
            1263.5, 3187, 1278.5, 3184, 1284.5, 3178, 1293.5, 3185, 1299.5, 3185, 1301.5, 3190, 1317.5, 3186, 1330.5, 3195,
            1343, 3197.5, 1348, 3204.5, 1347, 3209.5, 1355.5, 3214, 1359, 3220.5, 1356, 3229.5, 1359, 3237.5, 1357, 3247.5,
            1366, 3262.5, 1355, 3273.5, 1357, 3279.5, 1352, 3284.5, 1351, 3292.5, 1346, 3296.5, 1345, 3312.5, 1334.5, 3324,
            1327.5, 3318, 1316.5, 3318, 1307.5, 3324, 1299.5, 3323, 1294.5, 3326, 1281.5, 3317, 1269.5, 3314, 1254.5, 3319,
            1241.5, 3318, 1237, 3313.5, 1241, 3305.5, 1230.5, 3307, 1228, 3303.5, 1231, 3299.5, 1226, 3297.5, 1216, 3276.5,
            1203.5, 3280, 1195.5, 3264, 1189.5, 3274, 1177.5, 3278, 1171.5, 3284, 1166, 3282.5, 1166, 3290.5, 1159, 3292.5,
            1162, 3298.5, 1155.5, 3320, 1150.5, 3320, 1138.5, 3307, 1122.5, 3304, 1112, 3312.5, 1110, 3323.5, 1113, 3330.5,
            1101.5, 3329, 1094.5, 3332, 1090, 3339.5, 1091, 3354.5, 1095, 3362.5, 1091, 3370.5, 1096.5, 3372, 1106, 3383.5,
            1101.5, 3388, 1096.5, 3388, 1090, 3395.5, 1090, 3405.5, 1099, 3413.5, 1090, 3430.5, 1090, 3443.5, 1093, 3448.5,
            1096.5, 3452, 1115.5, 3452, 1131.5, 3442, 1140.5, 3452, 1148.5, 3455, 1168.5, 3450, 1181.5, 3455, 1192.5, 3452,
            1201.5, 3455, 1207, 3462.5, 1192.5, 3475, 1179.5, 3468, 1170.5, 3470, 1166, 3474.5, 1173.5, 3476, 1175, 3482.5,
            1165.5, 3490, 1162, 3486.5, 1165, 3476.5, 1160.5, 3477, 1155, 3482.5, 1154, 3497.5, 1170, 3514.5, 1174, 3526.5,
            1181, 3531.5, 1184, 3540.5, 1179, 3547.5, 1179, 3562.5, 1173, 3569.5, 1177.5, 3577, 1180.5, 3576, 1184, 3579.5,
            1176, 3589.5, 1180, 3606.5, 1166, 3618.5, 1164, 3622.5, 1168, 3627.5, 1167, 3632.5, 1175.5, 3645, 1179.5, 3646,
            1189.5, 3637, 1201, 3643.5, 1201, 3651.5, 1210, 3664.5, 1204, 3670.5, 1212, 3679.5, 1206, 3698.5, 1222, 3712.5,
            1220, 3720.5, 1200, 3734.5, 1210, 3753.5, 1205, 3759.5, 1213, 3783.5, 1220.5, 3787, 1266.5, 3766, 1285.5, 3767,
            1288, 3770.5, 1285.5, 3778, 1278.5, 3780, 1268.5, 3777, 1262, 3779.5, 1262, 3784.5, 1255, 3788.5, 1255, 3794.5,
            1261, 3797.5, 1257.5, 3801, 1254.5, 3796, 1248, 3798.5, 1250, 3805.5, 1246, 3815.5, 1252.5, 3813, 1258.5, 3815,
            1260, 3817.5, 1254, 3825.5, 1258.5, 3827, 1261.5, 3822, 1265.5, 3822, 1267, 3824.5, 1263, 3828.5, 1265, 3834.5,
            1279.5, 3840, 1284, 3849.5, 1290.5, 3854, 1297.5, 3853, 1302.5, 3862, 1315.5, 3851, 1320.5, 3851, 1323, 3855.5,
            1315, 3860.5, 1316.5, 3866, 1322, 3864.5, 1327.5, 3851, 1336.5, 3849, 1353, 3833.5, 1351, 3821.5, 1357, 3813.5,
            1358, 3805.5, 1349, 3799.5, 1349.5, 3795, 1366.5, 3791, 1374, 3793.5, 1371, 3800.5, 1376, 3809.5, 1366, 3816.5,
            1366, 3830.5, 1372.5, 3838, 1380.5, 3836, 1389.5, 3839, 1397, 3834.5, 1398, 3812.5, 1394.5, 3809, 1386.5, 3810,
            1383, 3802.5, 1391.5, 3797, 1397.5, 3797, 1407, 3810.5, 1407, 3832.5, 1404, 3836.5, 1407, 3840.5, 1396, 3857.5,
            1402, 3865.5, 1402, 3871.5, 1398, 3876.5, 1404, 3887.5, 1401, 3893.5, 1413.5, 3900, 1425, 3920.5, 1433, 3924.5,
            1433, 3930.5, 1426, 3933.5, 1433, 3944.5, 1442.5, 3947, 1445.5, 3952, 1455.5, 3954, 1465.5, 3941, 1472.5, 3940,
            1481.5, 3930, 1495.5, 3935, 1496, 3939.5, 1488, 3944.5, 1488, 3950.5, 1505, 3968.5, 1506, 3978.5, 1511.5, 3984,
            1518.5, 3984, 1526.5, 3977, 1537.5, 3974, 1539, 3981.5, 1547.5, 3990, 1556, 3993.5, 1555, 4007.5, 1566, 4015.5,
            1563, 4023.5, 1574.5, 4030, 1581.5, 4041, 1586.5, 4045, 1600.5, 4042, 1612.5, 4050, 1624.5, 4047, 1631.5, 4056,
            1643.5, 4052, 1653.5, 4054, 1663.5, 4043, 1677.5, 4048, 1698, 4039.5, 1700, 4036.5, 1689, 4028.5, 1697, 4016.5,
            1698, 4009.5, 1707, 4002.5, 1700, 3988.5, 1703, 3980.5, 1700, 3974.5, 1706.5, 3962, 1711.5, 3967, 1716.5, 3964,
            1723, 3967.5, 1733, 3983.5, 1732, 3992.5, 1734.5, 3995, 1746, 3995.5, 1745, 4001.5, 1739, 4005.5, 1743.5, 4016,
            1752.5, 4016, 1757, 4010.5, 1757, 4006.5, 1748, 3999.5, 1754.5, 3993, 1764.5, 4007, 1768.5, 4005, 1776.5, 4010,
            1783.5, 4009, 1787, 4000.5, 1778, 3992.5, 1789, 3984.5, 1789, 3972.5, 1781, 3968.5, 1781, 3963.5, 1785.5, 3962,
            1792.5, 3968, 1797.5, 3968, 1810.5, 3965, 1816.5, 3958, 1822.5, 3964, 1839.5, 3957, 1845, 3949.5, 1849, 3931.5,
            1860, 3918.5, 1863, 3908.5, 1865.5, 3907, 1874.5, 3913, 1888.5, 3906, 1891, 3902.5, 1886, 3891.5, 1902.5, 3889,
            1905, 3886.5, 1904, 3877.5, 1914, 3869.5, 1914, 3858.5, 1910.5, 3851, 1899.5, 3853, 1892.5, 3842, 1876.5, 3846,
            1863.5, 3845, 1860, 3841.5, 1860, 3831.5, 1843.5, 3830, 1836.5, 3818, 1822.5, 3820, 1815.5, 3813, 1802.5, 3815,
            1779, 3801.5, 1788.5, 3797, 1805.5, 3803, 1823.5, 3802, 1834.5, 3811, 1845.5, 3815, 1849.5, 3821, 1853, 3816.5,
            1845, 3799.5, 1852, 3791.5, 1852, 3781.5, 1841.5, 3779, 1831.5, 3768, 1825.5, 3770, 1818, 3765.5, 1826.5, 3750,
            1830, 3753.5, 1830.5, 3762, 1854, 3761.5, 1852, 3720.5, 1839.5, 3720, 1837.5, 3713, 1812.5, 3714, 1809, 3710.5,
            1810.5, 3693, 1848.5, 3692, 1849, 3688.5, 1824.5, 3687, 1822.5, 3673, 1819, 3673.5, 1819, 3684.5, 1815.5, 3688,
            1809, 3684.5, 1808.5, 3673, 1805, 3673.5, 1803.5, 3687, 1793.5, 3688, 1790, 3683.5, 1805, 3655.5, 1803, 3637.5,
            1810, 3629.5, 1810, 3621.5, 1816, 3617.5, 1815, 3611.5, 1819, 3606.5, 1816, 3594.5, 1821.5, 3590, 1829, 3598.5,
            1830, 3628.5, 1835, 3636.5, 1835, 3643.5, 1837.5, 3646, 1845, 3643.5, 1841, 3628.5, 1842, 3616.5, 1838, 3607.5,
            1842, 3602.5, 1841, 3595.5, 1854, 3580.5, 1854, 3571.5, 1862.5, 3566, 1871.5, 3569, 1882, 3558.5, 1886, 3549.5,
            1868.5, 3540, 1863.5, 3533, 1849.5, 3539, 1846, 3534.5, 1871.5, 3515, 1876.5, 3520, 1880, 3518.5, 1885, 3504.5,
            1879, 3490.5, 1885, 3479.5, 1865, 3468.5, 1865, 3464.5, 1870, 3461.5, 1867.5, 3457, 1861, 3459.5, 1860.5, 3465,
            1853.5, 3467, 1850, 3463.5, 1851, 3459.5, 1839.5, 3448, 1830.5, 3443, 1820.5, 3448, 1816, 3443.5, 1816.5, 3437,
            1811.5, 3438, 1802.5, 3434, 1802, 3438.5, 1796, 3442.5, 1801, 3451.5, 1796.5, 3460, 1781.5, 3457, 1757.5, 3467,
            1748.5, 3462, 1741.5, 3468, 1729, 3458.5, 1727.5, 3453, 1722.5, 3459, 1714.5, 3458, 1704.5, 3462, 1695.5, 3460,
            1689.5, 3453, 1683.5, 3452, 1685, 3458.5, 1692.5, 3460, 1693, 3464.5, 1684.5, 3473, 1670.5, 3470, 1669, 3478.5,
            1676, 3485.5, 1677.5, 3492, 1693.5, 3495, 1697, 3498.5, 1692.5, 3503, 1682.5, 3504, 1664.5, 3494, 1659, 3485.5,
            1659, 3476.5, 1643, 3466.5, 1642, 3461.5, 1649, 3447.5, 1640.5, 3440, 1634.5, 3442, 1629.5, 3439, 1627, 3430.5,
            1621.5, 3426, 1609.5, 3427, 1610, 3414.5, 1598.5, 3406, 1579.5, 3407, 1581, 3413.5, 1577.5, 3417, 1567.5, 3408,
            1559.5, 3407, 1555.5, 3413, 1544.5, 3419, 1533.5, 3414, 1530.5, 3405, 1522.5, 3411, 1508.5, 3406, 1506.5, 3399,
            1496, 3402.5, 1501, 3410.5, 1492, 3418.5, 1492, 3423.5, 1486, 3430.5, 1487, 3436.5, 1497, 3447.5, 1489, 3462.5,
            1476.5, 3469, 1466.5, 3479, 1457.5, 3480, 1452.5, 3485, 1448.5, 3484, 1444, 3487.5, 1443, 3494.5, 1433.5, 3499,
            1422, 3480.5, 1406.5, 3466, 1394.5, 3466, 1389.5, 3469, 1389, 3481.5, 1383.5, 3486, 1380, 3482.5, 1379.5, 3471,
            1370.5, 3475, 1361.5, 3474, 1356, 3468.5, 1352, 3455.5, 1362, 3441.5, 1369, 3423.5, 1379, 3416.5, 1366.5, 3408,
            1363, 3400.5, 1365.5, 3399, 1391.5, 3409, 1399, 3405.5, 1412.5, 3385, 1424.5, 3389, 1428.5, 3381, 1440, 3373.5,
            1440.5, 3365, 1446.5, 3365, 1453.5, 3374, 1457.5, 3374, 1463, 3368.5, 1459, 3360.5, 1461, 3353.5, 1458, 3350.5,
            1465, 3342.5, 1461, 3335.5, 1464.5, 3332, 1486.5, 3353, 1496.5, 3355, 1501.5, 3352, 1510.5, 3356, 1527, 3342.5,
            1528.5, 3337, 1551.5, 3333, 1558.5, 3328, 1568.5, 3328, 1575.5, 3320, 1583.5, 3318, 1583, 3301.5, 1585.5, 3300,
            1590.5, 3304, 1603.5, 3292, 1611, 3294.5, 1607, 3303.5, 1611.5, 3308, 1620.5, 3307, 1627.5, 3311, 1639.5, 3302,
            1643, 3305.5, 1640.5, 3311, 1644.5, 3310, 1654.5, 3315, 1656, 3313.5, 1649, 3303.5, 1650.5, 3300, 1656.5, 3299,
            1660.5, 3308, 1666.5, 3300, 1678.5, 3303, 1690.5, 3295, 1704, 3301.5, 1707.5, 3318, 1713, 3316.5, 1727, 3287.5,
            1724, 3278.5, 1726, 3268.5, 1721, 3255.5, 1727, 3245.5, 1715, 3238.5, 1710, 3224.5, 1701.5, 3213, 1688.5, 3208,
            1668.5, 3210, 1651.5, 3201, 1613.5, 3206, 1590.5, 3192, 1584.5, 3183, 1575.5, 3183, 1559.5, 3193, 1553.5, 3190,
            1549.5, 3193, 1546, 3189.5, 1547.5, 3187, 1552.5, 3189, 1567.5, 3178, 1579.5, 3175, 1585.5, 3182, 1610.5, 3194,
            1624.5, 3195, 1646.5, 3190, 1667.5, 3199, 1681.5, 3196, 1685.5, 3199, 1691.5, 3196, 1700.5, 3201, 1703.5, 3198,
            1707.5, 3200, 1710, 3191.5, 1721.5, 3186, 1736.5, 3189, 1740.5, 3193, 1749.5, 3193, 1754, 3190.5, 1755, 3156.5,
            1744, 3147.5, 1744, 3129.5, 1750.5, 3123, 1769.5, 3123, 1776, 3129.5, 1776, 3146.5, 1765, 3156.5, 1766, 3190.5,
            1786.5, 3195, 1792, 3176.5, 1783, 3170.5, 1783.5, 3164, 1787.5, 3164, 1794.5, 3156, 1815.5, 3156, 1825.5, 3151,
            1831.5, 3152, 1840.5, 3146, 1848.5, 3148, 1865, 3125.5, 1864, 3120.5, 1873, 3105.5, 1867, 3098.5, 1867, 3086.5,
            1862, 3083.5, 1852.5, 3068, 1833.5, 3056, 1821.5, 3056, 1814.5, 3052, 1808.5, 3054, 1797.5, 3047, 1778.5, 3049,
            1772, 3044.5, 1772, 3035.5, 1783.5, 3027, 1788.5, 3029, 1791, 3020.5, 1785, 3011.5, 1788, 2999.5, 1775, 2991.5,
            1776, 2982.5, 1771, 2969.5, 1772, 2964.5, 1783, 2952.5, 1779, 2945.5, 1780, 2939.5, 1760.5, 2933, 1755, 2925.5,
            1754.5, 2918, 1745.5, 2920, 1738.5, 2916, 1728.5, 2920, 1719.5, 2913, 1703.5, 2916, 1699, 2911.5, 1701, 2902.5,
            1697.5, 2899, 1682.5, 2895, 1672.5, 2888, 1663.5, 2888
    });

    // ---- the islands off Zeah and Varlamore, traced the same way

    public static final Polygon KEBOS_ICE_ISLET = new Polygon("Islet north-east of the Wintertodt", new double[]{
            1954.5, 4056, 1939, 4062.5, 1937.5, 4081, 1949.5, 4083, 1955, 4079.5, 1961, 4063.5
    });

    public static final Polygon SAND_CRAB_ISLET = new Polygon("Sand crab islet off Hosidius", new double[]{
            1749.5, 3451, 1744, 3454.5, 1748.5, 3461, 1752, 3456.5
    });

    public static final Polygon CRABCLAW_ISLE = new Polygon("Crabclaw Isle", new double[]{
            1790.5, 3402, 1783.5, 3402, 1775.5, 3409, 1767.5, 3405, 1760.5, 3410, 1750.5, 3405, 1746, 3408.5, 1748, 3426.5,
            1755, 3440.5, 1763.5, 3449, 1771.5, 3450, 1775, 3447.5, 1767, 3442.5, 1773.5, 3438, 1779.5, 3445, 1781, 3443.5,
            1781, 3415.5, 1792.5, 3412, 1796.5, 3416, 1798, 3412.5, 1797, 3406.5
    });

    public static final Polygon CRABCLAW_EAST_ISLET = new Polygon("Islet east of Crabclaw Isle", new double[]{
            1877.5, 3416, 1872, 3423.5, 1876, 3427.5, 1877, 3438.5, 1884.5, 3446, 1896, 3425.5, 1890.5, 3421, 1881.5, 3423
    });

    public static final Polygon PORT_ROBERTS = new Polygon("Port Roberts", new double[]{
            1894.5, 3270, 1881, 3270.5, 1881, 3279.5, 1887, 3284.5, 1880.5, 3292, 1875.5, 3291, 1873.5, 3286, 1860, 3286.5,
            1860, 3299.5, 1865, 3303.5, 1862, 3308.5, 1876.5, 3312, 1878.5, 3321, 1899.5, 3320, 1902.5, 3323, 1914.5, 3323,
            1913, 3280.5, 1896.5, 3280
    });

    public static final Polygon TEMPESTUS = new Polygon("Tempestus", new double[]{
            1301.5, 3200, 1291.5, 3202, 1278.5, 3214, 1266.5, 3201, 1252.5, 3205, 1251, 3208.5, 1269, 3229.5, 1264.5, 3235,
            1249.5, 3223, 1244.5, 3230, 1232.5, 3229, 1230.5, 3223, 1223, 3226.5, 1225, 3238.5, 1235.5, 3250, 1243, 3253.5,
            1247, 3260.5, 1244.5, 3266, 1238, 3268.5, 1236, 3274.5, 1239, 3295.5, 1255.5, 3307, 1267.5, 3307, 1273, 3295.5,
            1281.5, 3292, 1289.5, 3296, 1295, 3305.5, 1301.5, 3309, 1319.5, 3305, 1327, 3296.5, 1325, 3283.5, 1332.5, 3280,
            1339, 3270.5, 1337, 3253.5, 1340, 3238.5, 1334, 3232.5, 1336, 3223.5, 1331, 3212.5, 1324.5, 3211, 1320.5, 3204,
            1314.5, 3205
    });

    public static final Polygon TEMPEST_BAY_ISLET = new Polygon("Islet in Tempest Bay", new double[]{
            1206.5, 3221, 1199, 3225.5, 1199, 3230.5, 1208.5, 3238, 1216, 3228.5
    });

    public static final Polygon PORT_ROBERTS_SOUTH_ISLET = new Polygon("Islet south of Port Roberts", new double[]{
            1872.5, 3218, 1866, 3222.5, 1873.5, 3227
    });

    public static final Polygon COLOSSEUM_EAST_ISLET = new Polygon("Islet east of the Fortis Colosseum", new double[]{
            1970.5, 3088, 1963.5, 3089, 1957, 3099.5, 1954, 3109.5, 1960.5, 3118, 1970.5, 3115, 1975, 3108.5, 1975, 3096.5
    });

    public static final Polygon TLATI_RIVER_ISLET = new Polygon("Islet off the Tlati Rainforest", new double[]{
            1422.5, 3051, 1417, 3054.5, 1417.5, 3069, 1424, 3058.5
    });

    public static final Polygon ALDARIN_ISLE = new Polygon("Aldarin's island (Aldarin, Mistrock, Villa Lucens)", new double[]{
            1379.5, 2842, 1373.5, 2842, 1356, 2853.5, 1352, 2866.5, 1358, 2873.5, 1356.5, 2877, 1344.5, 2880, 1337.5, 2887,
            1332.5, 2884, 1324.5, 2894, 1317.5, 2886, 1305.5, 2883, 1298.5, 2889, 1293.5, 2889, 1291.5, 2895, 1283, 2899.5,
            1280, 2908.5, 1281, 2924.5, 1293, 2935.5, 1297.5, 2944, 1311.5, 2940, 1310, 2933.5, 1316, 2932.5, 1314, 2938.5,
            1319, 2942.5, 1320, 2950.5, 1315, 2955.5, 1314, 2963.5, 1318.5, 2977, 1331.5, 2981, 1337.5, 2987, 1355.5, 2988,
            1361.5, 2984, 1379.5, 2986, 1385, 2988.5, 1389.5, 2997, 1425.5, 3002, 1431, 2997.5, 1430, 2993.5, 1436, 2988.5,
            1439, 2980.5, 1444, 2978.5, 1445, 2971.5, 1456, 2969.5, 1455.5, 2967, 1448, 2965.5, 1447, 2959.5, 1450.5, 2957,
            1461.5, 2959, 1470.5, 2953, 1480, 2942.5, 1485, 2932.5, 1485, 2924.5, 1469.5, 2907, 1446, 2896.5, 1464, 2883.5,
            1461.5, 2865, 1457.5, 2872, 1438.5, 2880, 1424, 2868.5, 1433, 2859.5, 1427.5, 2850, 1411.5, 2843, 1401.5, 2846,
            1391.5, 2843, 1387.5, 2847
    });

    public static final Polygon SUNSET_COAST_EAST_ISLET = new Polygon("Islet east of the Sunset Coast", new double[]{
            1891.5, 2970, 1877.5, 2974, 1872, 2979.5, 1872, 2984.5, 1877.5, 2991, 1886.5, 2992, 1892.5, 2998, 1900.5, 2998,
            1905, 2993.5, 1899, 2985.5, 1901, 2974.5
    });

    public static final Polygon KASTORI_SOUTH_ISLET = new Polygon("Islet south-west of Kastori", new double[]{
            1196.5, 2816, 1183.5, 2816, 1181, 2821.5, 1193.5, 2830, 1207.5, 2828
    });

    /**
     * Traced from the OSRS world map (mejrs/layers_osrs map squares): the Tar Swamp reaching south to the
     * Deranged archaeologist (3683, 3706), the channel between the swamp and the Verdant Valley, the bay below the
     * Museum Camp, Mushroom Meadow and the north coast, the bay west of the House on the Hill, and the volcano.
     */
    public static final Polygon FOSSIL_ISLAND = new Polygon("Fossil Island", new double[]{
            // west coast of the Tar Swamp, then Mushroom Meadow and the north coast
            3636, 3800, 3640, 3817, 3650, 3828, 3656, 3840, 3655, 3870, 3655, 3883, 3667, 3893, 3680, 3898,
            3700, 3901, 3717, 3901, 3730, 3898, 3737, 3892,
            // the bay between Mushroom Meadow and the House on the Hill
            3731, 3877, 3730, 3860, 3735, 3848, 3746, 3851, 3743, 3873, 3750, 3885, 3765, 3890, 3780, 3884,
            3792, 3877, 3788, 3868, 3777, 3861, 3772, 3848, 3776, 3840,
            // the volcano's north and east coast, the south coast and the sandbar below the Verdant Valley
            3787, 3836, 3800, 3838, 3820, 3834, 3833, 3823, 3841, 3807, 3841, 3783, 3840, 3763, 3827, 3753,
            3807, 3740, 3787, 3727, 3767, 3727, 3753, 3727, 3745, 3711, 3733, 3737, 3727, 3753, 3725, 3773,
            // the channel west of the Verdant Valley up to the Museum Camp's bay, then down the Tar Swamp's east side
            3730, 3793, 3724, 3804, 3713, 3806, 3709, 3798, 3707, 3780, 3710, 3767, 3710, 3753, 3707, 3723,
            3700, 3707, 3688, 3696, 3673, 3697, 3660, 3707, 3656, 3723, 3647, 3740, 3640, 3767
    });

    /**
     * Traced from the OSRS world map: Marim along the north coast, the lagoon opening to the east below it, and the
     * jungle reaching south to y 2683 (the Arceuus Ape Atoll Teleport lands at 2770, 2703).
     */
    public static final Polygon APE_ATOLL = new Polygon("Ape Atoll", new double[]{
            2695, 2800, 2712, 2805, 2737, 2812, 2770, 2815, 2795, 2812, 2814, 2807, 2815, 2792, 2814, 2767,
            2808, 2752, 2810, 2745, 2795, 2745, 2775, 2744, 2770, 2735, 2780, 2728, 2800, 2726, 2810, 2730,
            2813, 2720, 2812, 2700, 2808, 2690, 2790, 2683, 2760, 2683, 2740, 2685, 2712, 2690, 2695, 2710,
            2690, 2732, 2691, 2750, 2694, 2775
    });

    /** Every traced landmass, followed by the Sailing-era sea islands ({@link SeaIslands}). */
    public static final List<Polygon> POLYGONS = concat(List.of(MAINLAND, KARAMJA, ZEAH, FOSSIL_ISLAND, APE_ATOLL,
            KEBOS_ICE_ISLET, SAND_CRAB_ISLET, CRABCLAW_ISLE, CRABCLAW_EAST_ISLET, PORT_ROBERTS,
            TEMPESTUS, TEMPEST_BAY_ISLET, PORT_ROBERTS_SOUTH_ISLET, COLOSSEUM_EAST_ISLET, TLATI_RIVER_ISLET, ALDARIN_ISLE, SUNSET_COAST_EAST_ISLET, KASTORI_SOUTH_ISLET),
            SeaIslands.ALL);

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> out = new java.util.ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    public static final List<Ellipse> ISLANDS = List.of(
            new Ellipse("Tutorial Island", 3104, 3102, 46, 34),
            new Ellipse("Crandor", 2836, 3262, 24, 38),
            new Ellipse("Entrana", 2836, 3340, 38, 30),
            new Ellipse("Cairn Isle", 2763, 2976, 15, 14),
            new Ellipse("Void Knights' Outpost", 2655, 2658, 42, 40),
            new Ellipse("Isle of Souls", 2210, 2900, 70, 60), // approx
            new Ellipse("Harmony Island", 3795, 2861, 30, 30),
            new Ellipse("Mos Le'Harmless", 3712, 2985, 66, 62),
            new Ellipse("Dragontooth Island", 3808, 3555, 26, 26),
            new Ellipse("Lithkren", 3563, 3995, 36, 30), // OSRS map: 3537-3600, 3965-4025
            new Ellipse("Miscellania", 2534, 3868, 54, 32),
            new Ellipse("Etceteria", 2618, 3880, 24, 24),
            new Ellipse("Waterbirth Island", 2530, 3748, 26, 32),
            new Ellipse("Neitiznot", 2336, 3804, 30, 36),
            new Ellipse("Jatizso", 2406, 3803, 34, 34),
            // The ice trolls' snowfields north of the two islands, across the rope bridges (cache troll spawns at
            // 2318-2379, 3858-3895 and 2387-2418, 3853-3869; shapes approx)
            new Ellipse("Neitiznot troll lands", 2350, 3879, 44, 27),
            new Ellipse("Jatizso troll lands", 2403, 3861, 22, 15),
            new Ellipse("Lunar Isle", 2110, 3905, 68, 58),
            new Ellipse("Pirates' Cove", 2212, 3805, 28, 28),
            new Ellipse("Ungael", 2256, 4048, 30, 30),
            new Ellipse("Iceberg", 2660, 4010, 14, 12),
            // Sailing-era islands of the Unquiet Ocean, placed and sized around their quest sites (approx); the traced ones
            // (the Pandemonium, the Onyx Crest, Sunbleak Island...) are in SeaIslands
            new Ellipse("The Great Conch", 3186, 2443, 62, 58),
            new Ellipse("Little Pearl", 3352, 2212, 16, 14),
            new Ellipse("Tortugan islet", 2962, 2605, 10, 9),
            new Ellipse("Red Rock", 2792, 2522, 15, 13),
            new Ellipse("The Zenith", 2878, 2505, 16, 14),
            new Ellipse("Last Light", 2855, 2327, 26, 24),
            new Ellipse("Wyrmscraig", 2566, 2250, 58, 62),
            new Ellipse("Deepfin Point", 1948, 2780, 16, 23), // wiki map centre; its charter dock (1943, 2757) on the south shore
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
            // Varlamore, placed on the map squares' rock and snow (wiki: Ralos' Rise 1400-1490 x 3050-3220, Mons Gratia
            // 1440,3250, the Darkfrost 1512,3285, the Proudspire 1570,3235, the Custodia Mountains around 1277,3390)
            new Ridge("Ralos' Rise", 24, new double[]{1440, 3205, 140, 1446, 3150, 166, 1452, 3115, 172, 1462, 3070, 140}),
            new Ridge("Mons Gratia and the Darkfrost", 18, new double[]{1430, 3228, 118, 1440, 3255, 140, 1470, 3290, 122,
                    1512, 3285, 150}),
            new Ridge("The Proudspire", 26, new double[]{1570, 3238, 152, 1571, 3239, 152}),
            new Ridge("Custodia Mountains (north)", 14, new double[]{1212, 3440, 118, 1236, 3438, 134, 1262, 3446, 118,
                    1295, 3446, 112}),
            new Ridge("Custodia Mountains (south)", 14, new double[]{1175, 3305, 118, 1203, 3329, 138, 1245, 3352, 118}),
            new Ridge("Custodia Mountains (east)", 11, new double[]{1318, 3370, 108, 1335, 3386, 116, 1340, 3398, 108}),
            new Ridge("The Stranglewood's peak", 16, new double[]{1121, 3421, 130, 1122, 3422, 130}), // approx name
            // the Onyx Crest's young volcano on the island's west side (lava crater at 2950-2970, 2252-2268 on the map; height approx)
            new Ridge("The Onyx Crest volcano", 9, new double[]{2957, 2258, 84, 2962, 2266, 80})
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
            // Zeah and Varlamore: every stream of the map squares at least 14 tiles long, traced along its centre line
            // (width = the drawn water's width, at least 2 tiles); the River Hos runs past Hosidius to the south coast.
            new River("River Hos", 3, new double[]{
                    1723.5, 3510.5, 1729.5, 3514.5, 1730.5, 3520.5, 1719.5, 3539.5, 1721.5, 3555.5, 1710.5, 3563.5, 1703.5, 3563.5, 1698.5, 3560.5, 1693.5, 3560.5, 1689.5, 3564.5,
                    1688.5, 3570.5, 1685.5, 3572.5, 1686.5, 3581.5, 1683.5, 3587.5, 1683.5, 3591.5, 1688.5, 3593.5, 1694.5, 3593.5, 1705.5, 3587.5, 1712.5, 3588.5, 1715.5, 3592.5}),
            new River("Stream near Hosidius", 3.5, new double[]{
                    1747.5, 3670.5, 1741.5, 3659.5, 1743.5, 3646.5, 1739.5, 3641.5, 1734.5, 3639.5, 1716.5, 3639.5, 1707.5, 3631.5, 1708.5, 3624.5, 1716.5, 3616.5, 1717.5, 3609.5,
                    1713.5, 3599.5, 1716.5, 3595.5}),
            new River("Stream near Quetzacalli Gorge", 3.5, new double[]{
                    1478.5, 3231.5, 1485.5, 3225.5, 1487.5, 3214.5, 1490.5, 3211.5, 1499.5, 3210.5, 1500.5, 3208.5, 1492.5, 3198.5, 1493.5, 3187.5, 1486.5, 3182.5, 1476.5, 3182.5,
                    1470.5, 3178.5, 1473.5, 3169.5}),
            new River("Stream near the Sunrise Palace", 2, new double[]{
                    1677.5, 3151.5, 1669.5, 3151.5, 1669.5, 3172.5, 1673.5, 3178.5, 1688.5, 3178.5, 1689.5, 3174.5, 1692.5, 3172.5, 1692.5, 3151.5, 1684.5, 3151.5}),
            new River("Stream near Tal Teklan", 2, new double[]{
                    1285.5, 3117.5, 1283.5, 3113.5, 1276.5, 3114.5, 1270.5, 3109.5, 1266.5, 3109.5, 1261.5, 3112.5, 1264.5, 3120.5, 1261.5, 3124.5, 1261.5, 3128.5, 1264.5, 3131.5,
                    1271.5, 3132.5, 1282.5, 3144.5}),
            new River("Stream near Nemus Retreat", 3, new double[]{
                    1391.5, 3298.5, 1394.5, 3300.5, 1396.5, 3305.5, 1397.5, 3318.5, 1406.5, 3324.5, 1418.5, 3321.5, 1421.5, 3324.5, 1423.5, 3337.5, 1429.5, 3338.5, 1433.5, 3341.5}),
            new River("Stream near the Woodcutting Guild", 3, new double[]{
                    1631.5, 3565.5, 1643.5, 3556.5, 1653.5, 3557.5, 1661.5, 3565.5, 1666.5, 3588.5, 1673.5, 3593.5}),
            new River("Stream near the Lizardman Settlement", 2, new double[]{
                    1301.5, 3458.5, 1304.5, 3458.5, 1307.5, 3461.5, 1319.5, 3461.5, 1326.5, 3469.5, 1336.5, 3470.5, 1348.5, 3477.5, 1360.5, 3476.5}),
            new River("Stream near the Lizardman Settlement", 3, new double[]{
                    1390.5, 3542.5, 1386.5, 3540.5, 1383.5, 3532.5, 1384.5, 3522.5, 1388.5, 3517.5, 1389.5, 3512.5, 1379.5, 3509.5, 1368.5, 3516.5}),
            new River("Stream near Ortus Farm", 2, new double[]{
                    1496.5, 3154.5, 1551.5, 3153.5}),
            new River("Stream near Quetzacalli Gorge", 3, new double[]{
                    1479.5, 3235.5, 1488.5, 3243.5, 1488.5, 3246.5, 1482.5, 3253.5, 1481.5, 3258.5, 1487.5, 3265.5, 1493.5, 3268.5, 1495.5, 3275.5}),
            new River("Stream near the Lizardman Settlement", 2, new double[]{
                    1313.5, 3515.5, 1316.5, 3515.5, 1325.5, 3506.5, 1331.5, 3503.5, 1342.5, 3504.5, 1352.5, 3510.5, 1354.5, 3513.5}),
            new River("Stream near Nemus Retreat", 2, new double[]{
                    1325.5, 3364.5, 1327.5, 3358.5, 1331.5, 3354.5, 1331.5, 3348.5, 1336.5, 3348.5, 1342.5, 3350.5, 1347.5, 3355.5, 1346.5, 3367.5}),
            new River("Stream near Port Piscarilius", 3, new double[]{
                    1776.5, 3727.5, 1757.5, 3726.5, 1757.5, 3742.5, 1760.5, 3742.5, 1761.5, 3753.5}),
            new River("Stream near the Proudspire", 3, new double[]{
                    1608.5, 3207.5, 1609.5, 3217.5, 1617.5, 3224.5, 1623.5, 3237.5, 1621.5, 3243.5, 1616.5, 3244.5}),
            new River("Stream near Tal Teklan", 2, new double[]{
                    1264.5, 3182.5, 1264.5, 3151.5, 1251.5, 3149.5}),
            new River("Stream near Kourend Castle", 2, new double[]{
                    1587.5, 3679.5, 1585.5, 3691.5, 1591.5, 3696.5, 1591.5, 3698.5, 1584.5, 3707.5, 1582.5, 3717.5}),
            new River("Stream near the Tlati Rainforest", 4.5, new double[]{
                    1318.5, 3152.5, 1329.5, 3144.5, 1332.5, 3137.5, 1336.5, 3133.5, 1341.5, 3132.5, 1347.5, 3135.5, 1352.5, 3135.5}),
            new River("Stream near the Lizardman Settlement", 2, new double[]{
                    1397.5, 3470.5, 1397.5, 3480.5, 1390.5, 3483.5, 1388.5, 3493.5, 1388.5, 3496.5, 1392.5, 3502.5, 1395.5, 3503.5}),
            new River("Stream near the Gloomthorn Trail", 4.5, new double[]{
                    1409.5, 3221.5, 1407.5, 3221.5, 1402.5, 3228.5, 1400.5, 3239.5, 1405.5, 3246.5, 1413.5, 3249.5}),
            new River("Stream near Ortus Farm", 2, new double[]{
                    1586.5, 3153.5, 1624.5, 3154.5}),
            new River("Stream near Kingstown", 3, new double[]{
                    1747.5, 3677.5, 1743.5, 3679.5, 1737.5, 3690.5, 1738.5, 3698.5, 1741.5, 3704.5, 1744.5, 3705.5}),
            new River("Stream near the Stranglewood", 3, new double[]{
                    1224.5, 3383.5, 1217.5, 3384.5, 1203.5, 3378.5, 1196.5, 3378.5, 1191.5, 3374.5}),
            new River("Stream near Tal Teklan", 2, new double[]{
                    1230.5, 3191.5, 1229.5, 3155.5}),
            new River("Stream near the Stranglewood", 5, new double[]{
                    1175.5, 3364.5, 1173.5, 3360.5, 1164.5, 3360.5, 1148.5, 3363.5, 1147.5, 3366.5}),
            new River("Stream near Quetzacalli Gorge", 5, new double[]{
                    1511.5, 3208.5, 1512.5, 3210.5, 1525.5, 3208.5, 1532.5, 3200.5, 1530.5, 3193.5}),
            new River("Stream near Tal Teklan", 2, new double[]{
                    1236.5, 3172.5, 1237.5, 3178.5, 1243.5, 3177.5, 1246.5, 3179.5, 1256.5, 3178.5, 1257.5, 3171.5}),
            new River("Stream near the Tlati Rainforest", 3, new double[]{
                    1313.5, 3152.5, 1312.5, 3150.5, 1299.5, 3144.5, 1284.5, 3144.5}),
            new River("River Molch", 3, new double[]{
                    1298.5, 3690.5, 1314.5, 3696.5, 1326.5, 3691.5}),
            new River("Stream near Kourend Castle", 2, new double[]{
                    1615.5, 3639.5, 1621.5, 3628.5, 1621.5, 3623.5, 1618.5, 3620.5, 1614.5, 3620.5, 1610.5, 3616.5}),
            new River("Stream near the Woodcutting Guild", 3.5, new double[]{
                    1719.5, 3510.5, 1708.5, 3495.5, 1699.5, 3494.5, 1698.5, 3497.5}),
            new River("Stream near the Lizardman Settlement", 3, new double[]{
                    1378.5, 3486.5, 1369.5, 3490.5, 1365.5, 3494.5, 1365.5, 3508.5}),
            new River("Stream near the Stranglewood", 3.5, new double[]{
                    1112.5, 3385.5, 1123.5, 3381.5, 1128.5, 3372.5, 1134.5, 3368.5}),
            new River("Stream near Mount Quidamortem", 3, new double[]{
                    1185.5, 3575.5, 1196.5, 3569.5, 1207.5, 3569.5, 1207.5, 3567.5, 1210.5, 3566.5}),
            new River("Stream near the Custodia Mountains", 3, new double[]{
                    1240.5, 3321.5, 1240.5, 3325.5, 1234.5, 3331.5, 1220.5, 3332.5}),
            new River("Stream near Mons Gratia", 2, new double[]{
                    1420.5, 3277.5, 1410.5, 3276.5, 1402.5, 3289.5}),
            new River("Stream near Port Piscarilius", 3, new double[]{
                    1765.5, 3760.5, 1760.5, 3760.5, 1759.5, 3769.5, 1749.5, 3770.5}),
            new River("Stream near Port Piscarilius", 3, new double[]{
                    1744.5, 3761.5, 1742.5, 3757.5, 1742.5, 3741.5, 1738.5, 3740.5}),
            new River("Stream near Port Piscarilius", 3, new double[]{
                    1819.5, 3728.5, 1821.5, 3724.5, 1820.5, 3719.5, 1805.5, 3718.5}),
            new River("Stream near Port Roberts", 3, new double[]{
                    1901.5, 3318.5, 1900.5, 3307.5, 1896.5, 3306.5, 1896.5, 3296.5}),
            new River("Stream near the Lizardman Settlement", 3, new double[]{
                    1383.5, 3568.5, 1384.5, 3561.5, 1389.5, 3554.5, 1388.5, 3546.5}),
            new River("Stream near the Lizardman Settlement", 3, new double[]{
                    1308.5, 3516.5, 1297.5, 3512.5, 1286.5, 3511.5}),
            new River("Stream near Ralos' Rise", 2, new double[]{
                    1451.5, 3144.5, 1473.5, 3147.5}),
            new River("Stream near the Teomat", 3, new double[]{
                    1486.5, 3182.5, 1489.5, 3172.5, 1489.5, 3160.5}),
            new River("Stream near Mistrock", 3, new double[]{
                    1316.5, 2900.5, 1313.5, 2902.5, 1310.5, 2896.5, 1314.5, 2892.5, 1320.5, 2894.5}),
            new River("Stream near Aldarin", 2, new double[]{
                    1444.5, 2898.5, 1437.5, 2899.5, 1431.5, 2895.5, 1423.5, 2896.5}),
            new River("Stream near Arceuus", 3.5, new double[]{
                    1746.5, 3802.5, 1743.5, 3802.5, 1738.5, 3797.5, 1738.5, 3790.5, 1741.5, 3786.5}),
            new River("Stream near Nemus Retreat", 3, new double[]{
                    1329.5, 3325.5, 1324.5, 3329.5, 1323.5, 3334.5, 1331.5, 3339.5}),
            new River("Stream near Port Piscarilius", 5, new double[]{
                    1799.5, 3767.5, 1779.5, 3767.5}),
            new River("Stream near the Stranglewood", 3, new double[]{
                    1134.5, 3359.5, 1130.5, 3359.5, 1126.5, 3356.5, 1123.5, 3351.5, 1124.5, 3346.5}),
            new River("Stream near the Twilight Temple", 3, new double[]{
                    1720.5, 3293.5, 1718.5, 3291.5, 1714.5, 3291.5, 1712.5, 3296.5, 1705.5, 3299.5}),
            new River("Stream near Kourend Castle", 3, new double[]{
                    1610.5, 3611.5, 1615.5, 3606.5, 1617.5, 3600.5, 1616.5, 3595.5}),
            new River("Stream near Crabclaw Isle", 2, new double[]{
                    1766.5, 3437.5, 1765.5, 3433.5, 1768.5, 3430.5, 1772.5, 3430.5, 1775.5, 3436.5}),
            new River("Stream near Port Piscarilius", 2, new double[]{
                    1820.5, 3735.5, 1816.5, 3736.5, 1815.5, 3741.5, 1817.5, 3745.5, 1821.5, 3745.5}),
            new River("Stream near Auburnvale", 3, new double[]{
                    1438.5, 3339.5, 1440.5, 3339.5, 1444.5, 3333.5, 1451.5, 3332.5}),
            new River("Stream near Port Piscarilius", 2, new double[]{
                    1806.5, 3760.5, 1807.5, 3766.5, 1814.5, 3766.5, 1816.5, 3764.5}),
            new River("Stream near the Lizardman Canyon", 2, new double[]{
                    1572.5, 3741.5, 1569.5, 3740.5, 1569.5, 3737.5, 1578.5, 3735.5}),
            new River("Stream near Mount Quidamortem", 3, new double[]{
                    1193.5, 3476.5, 1200.5, 3475.5, 1205.5, 3477.5, 1207.5, 3475.5}),
            new River("Stream near the Custodia Mountains", 3.5, new double[]{
                    1255.5, 3378.5, 1240.5, 3378.5}),
            new River("Stream near Tal Teklan", 2, new double[]{
                    1244.5, 3149.5, 1231.5, 3150.5, 1230.5, 3152.5}),
            new River("Stream near Mount Karuulm", 4.5, new double[]{
                    1388.5, 3785.5, 1376.5, 3793.5}),
            new River("Stream near Port Piscarilius", 2, new double[]{
                    1777.5, 3750.5, 1777.5, 3758.5, 1770.5, 3759.5}),
            new River("Stream near Kourend Castle", 2, new double[]{
                    1604.5, 3701.5, 1600.5, 3697.5, 1591.5, 3696.5}),
            new River("Stream near Kourend Castle", 2, new double[]{
                    1587.5, 3667.5, 1587.5, 3658.5, 1582.5, 3655.5}),
            new River("Stream near the Stranglewood", 2, new double[]{
                    1123.5, 3341.5, 1118.5, 3332.5, 1115.5, 3331.5})
    );

    public static final List<Ellipse> LAKES = List.of(
            new Ellipse("Baxtorian Lake", 2516, 3494, 12, 10),
            new Ellipse("Source of the Elid", 3392, 3080, 6, 6)
    );

    /** A lake traced as a polygon (x0, y0, x1, y1, ...), with the islands in it. */
    public record Lake(String name, double[] xy, double[]... islands) {
    }

    /**
     * Zeah's and Varlamore's lakes and wide river reaches, traced from the map squares like the coast (at least 100
     * tiles of water; Port Roberts' harbour basin and the meltwater pools on the Wintertodt snowfield are left out).
     * Lake Molch keeps Molch Island, where the aerial fishing boats land (1369, 3639).
     */
    public static final List<Lake> TRACED_LAKES = concat(List.of(
            new Lake("Pond near Arceuus", new double[]{
                    1765.5, 3796, 1745.5, 3803, 1746, 3810.5, 1750.5, 3814, 1767.5, 3807, 1772, 3801.5
            }),
            new Lake("Pond near Arceuus", new double[]{
                    1741.5, 3759, 1735, 3767.5, 1739.5, 3776, 1745, 3769.5, 1745, 3762.5
            }),
            new Lake("Lake near the Battlefront", new double[]{
                    1317.5, 3723, 1308.5, 3729, 1298, 3729.5, 1300, 3736.5, 1296, 3743.5, 1300.5, 3749, 1310.5, 3750, 1330, 3739.5,
                    1330, 3733.5
            }),
            new Lake("Pond near Port Piscarilius", new double[]{
                    1823.5, 3726, 1818, 3732.5, 1827.5, 3737, 1834, 3730.5
            }),
            new Lake("Pond near Port Piscarilius", new double[]{
                    1743.5, 3705, 1739, 3711.5, 1739, 3723.5, 1742.5, 3727, 1752, 3722.5, 1748, 3709.5
            }),
            new Lake("River Molch (wide reach)", new double[]{
                    1275.5, 3686, 1264, 3697.5, 1271.5, 3708, 1278, 3705.5, 1277, 3698.5, 1283, 3693.5
            }),
            new Lake("Pond near Port Piscarilius", new double[]{
                    1801.5, 3692, 1793.5, 3692, 1790, 3695.5, 1793, 3702.5, 1800.5, 3708, 1805, 3704.5, 1805, 3695.5
            }),
            new Lake("Pond near the Farming Guild", new double[]{
                    1227.5, 3680, 1222, 3688.5, 1230.5, 3697, 1236, 3691.5
            }),
            new Lake("River Molch (wide reach)", new double[]{
                    1268.5, 3664, 1262.5, 3665, 1257, 3670.5, 1260.5, 3674, 1266.5, 3672, 1271, 3677.5, 1268, 3680.5, 1272.5, 3684,
                    1276, 3680.5, 1273, 3677.5, 1276, 3671.5
            }),
            new Lake("Lake Molch", new double[]{
                    1392.5, 3579, 1371.5, 3595, 1345.5, 3601, 1339, 3605.5, 1334, 3620.5, 1340, 3631.5, 1337, 3640.5, 1343, 3646.5,
                    1333.5, 3649, 1330, 3656.5, 1340, 3668.5, 1339.5, 3679, 1349.5, 3682, 1356.5, 3678, 1361.5, 3680, 1366.5, 3674,
                    1375.5, 3675, 1381.5, 3672, 1383.5, 3665, 1391.5, 3670, 1421, 3646.5, 1421, 3640.5, 1414, 3628.5, 1414, 3619.5,
                    1408, 3613.5, 1418, 3596.5
            },
                    new double[]{1371.5, 3625, 1360, 3629.5, 1359, 3635.5, 1365.5, 3641, 1376, 3636.5, 1377, 3628.5}),
            new Lake("Pond near the Kebos Swamp", new double[]{
                    1235.5, 3659, 1222.5, 3661, 1217, 3665.5, 1230.5, 3673, 1246, 3667.5, 1243.5, 3661
            }),
            new Lake("Pond near Kourend Castle", new double[]{
                    1580.5, 3644, 1572.5, 3645, 1571, 3650.5, 1583.5, 3654, 1586, 3648.5
            }),
            new Lake("Lake near the Woodcutting Guild", new double[]{
                    1613.5, 3529, 1600.5, 3543, 1592.5, 3548, 1582.5, 3547, 1578, 3552.5, 1582.5, 3566, 1593.5, 3565, 1610.5, 3579,
                    1615, 3574.5, 1612, 3569.5, 1622, 3560.5, 1618, 3554.5, 1621, 3534.5
            }),
            new Lake("Lake near Mount Quidamortem", new double[]{
                    1285.5, 3511, 1276.5, 3513, 1269.5, 3525, 1262.5, 3521, 1263, 3528.5, 1252, 3534.5, 1252, 3540.5, 1276.5, 3544,
                    1278, 3540.5, 1275, 3537.5, 1279, 3529.5, 1277, 3521.5, 1288, 3516.5
            }),
            new Lake("Pond near the Lizardman Settlement", new double[]{
                    1362.5, 3506, 1355, 3512.5, 1359.5, 3518, 1368, 3514.5
            }),
            new Lake("Pond near Mount Quidamortem", new double[]{
                    1224.5, 3464, 1210.5, 3465, 1208, 3478.5, 1214.5, 3482, 1221.5, 3481, 1229, 3472.5
            }),
            new Lake("Custodia's lake", new double[]{
                    1256.5, 3378, 1253, 3381.5, 1257, 3396.5, 1252.5, 3398, 1251, 3402.5, 1257.5, 3409, 1267.5, 3409, 1274.5, 3397,
                    1295.5, 3401, 1306, 3393.5, 1295.5, 3382, 1287.5, 3382, 1283.5, 3386, 1277.5, 3382, 1265.5, 3385
            }),
            new Lake("Pond near the Custodia Mountains", new double[]{
                    1239.5, 3371, 1236.5, 3374, 1232.5, 3371, 1224.5, 3375, 1222, 3381.5, 1224.5, 3383, 1236.5, 3375, 1241.5, 3377,
                    1243, 3374.5
            }),
            new Lake("Pond near the Stranglewood", new double[]{
                    1178.5, 3359, 1176, 3364.5, 1186.5, 3374, 1190.5, 3374, 1190, 3367.5
            }),
            new Lake("Pond near the Gloomthorn Trail", new double[]{
                    1406.5, 3191, 1403, 3194.5, 1413, 3207.5, 1407, 3217.5, 1410.5, 3221, 1419, 3204.5
            }),
            new Lake("Pond near Ralos' Rise", new double[]{
                    1483.5, 3138, 1476.5, 3140, 1473, 3145.5, 1482.5, 3159, 1489.5, 3160, 1496, 3152.5, 1490, 3141.5
            }),
            new Lake("Pond near the Tlati Rainforest", new double[]{
                    1365.5, 3129, 1361.5, 3134, 1351.5, 3136, 1351, 3140.5, 1363.5, 3140, 1369, 3133.5
            }),
            new Lake("Pond near the Locus Oasis", new double[]{
                    1688.5, 2980, 1680.5, 2982, 1677, 2986.5, 1675, 2992.5, 1680.5, 3000, 1691, 2993.5, 1693, 2984.5
            })
    ), SeaIslands.LAGOONS);

    public static final List<Ellipse> SWAMPS = List.of(
            new Ellipse("Lumbridge Swamp", 3195, 3166, 58, 28),
            new Ellipse("Mort Myre Swamp", 3460, 3385, 40, 55),
            new Ellipse("Kharazi Jungle wetland", 2870, 2905, 75, 22),
            new Ellipse("Kebos Swamp", 1262, 3642, 58, 42) // the map squares' fen between Mount Quidamortem and Mount Karuulm (approx)
    );
}
