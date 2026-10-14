package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.ArenaLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-way combat areas. Everywhere in Gielinor is single-way combat (one attacker per target) except the
 * areas listed here, which follow the OSRS multicombat map: the God Wars Dungeon, the Wilderness multi zones,
 * the Revenant Caves, the Catacombs of Kourend, the busy slayer dungeons and group boss lairs. Raids and
 * minigame instances are always multi; the Minecraft side checks those dynamically.
 *
 * <p>Areas are either OSRS tile boxes (real world-map coordinates) or a radius around a place (a module
 * {@link PlaceDef} id or a {@link Landmark} name), because many of this world's dungeons are laid out on the
 * surface around their entrance instead of in the real underground map squares.
 */
public final class MultiCombat {
    /** An inclusive OSRS tile box. */
    public record Box(String name, int x1, int y1, int x2, int y2) {
        public boolean contains(int x, int y) {
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
        }
    }

    /**
     * A multicombat polygon given by its tile corners (x0, y0, x1, y1, ...), as RuneLite draws the multi lines: tile
     * (x, y) is inside when its centre (x + 0.5, y + 0.5) is.
     */
    public record Zone(String name, int... xy) {
        public boolean contains(int x, int y) {
            double px = x + 0.5;
            double py = y + 0.5;
            boolean in = false;
            for (int i = 0, j = xy.length - 2; i < xy.length; j = i, i += 2) {
                if ((xy[i + 1] > py) != (xy[j + 1] > py)
                        && px < (double) (xy[j] - xy[i]) * (py - xy[i + 1]) / (xy[j + 1] - xy[i + 1]) + xy[i]) {
                    in = !in;
                }
            }
            return in;
        }

        /** The zone's bounding tile box. */
        public Box bounds() {
            int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE, x2 = Integer.MIN_VALUE, y2 = Integer.MIN_VALUE;
            for (int i = 0; i < xy.length; i += 2) {
                x1 = Math.min(x1, xy[i]);
                y1 = Math.min(y1, xy[i + 1]);
                x2 = Math.max(x2, xy[i] - 1);
                y2 = Math.max(y2, xy[i + 1] - 1);
            }
            return new Box(name, x1, y1, x2, y2);
        }
    }

    /** A square of {@code radius} tiles around a place (module place id or Landmark name). */
    public record Around(String name, String place, int radius) {
    }

    /**
     * OSRS world-map multicombat boxes. The surface areas outside Zeah are the plane-0 polygons of RuneLite's
     * multi-lines map (MapLocations.java, as kept in SanLiteOSRS/SanLite), cut into tile boxes; areas Jagex changed
     * since (the 2023 Wilderness boss lairs, Kourend's rework) may differ (approx). Zeah's and Fossil Island's zones are
     * the polygons in {@link #ZONES}; Zeah's lairs are in {@link #AROUND}.
     */
    public static final List<Box> BOXES = List.of(
            new Box("Wilderness: main multi area", 3136, 3520, 3327, 3583),
            new Box("Wilderness: main multi area", 3152, 3584, 3327, 3592),
            new Box("Wilderness: main multi area", 3151, 3593, 3327, 3593),
            new Box("Wilderness: main multi area", 3150, 3594, 3327, 3594),
            new Box("Wilderness: main multi area", 3149, 3595, 3327, 3595),
            new Box("Wilderness: main multi area", 3147, 3596, 3327, 3597),
            new Box("Wilderness: main multi area", 3146, 3598, 3327, 3615),
            new Box("Wilderness: main multi area", 3147, 3616, 3327, 3618),
            new Box("Wilderness: main multi area", 3151, 3619, 3327, 3619),
            new Box("Wilderness: main multi area", 3152, 3620, 3161, 3621),
            new Box("Wilderness: main multi area", 3153, 3622, 3161, 3622),
            new Box("Wilderness: main multi area", 3154, 3623, 3161, 3624),
            new Box("Wilderness: main multi area", 3155, 3625, 3161, 3625),
            new Box("Wilderness: main multi area", 3187, 3620, 3327, 3635),
            new Box("Wilderness: main multi area", 3183, 3636, 3327, 3636),
            new Box("Wilderness: main multi area", 3177, 3637, 3327, 3640),
            new Box("Wilderness: main multi area", 3176, 3641, 3327, 3644),
            new Box("Wilderness: main multi area", 3174, 3645, 3327, 3647),
            new Box("Wilderness: main multi area", 3192, 3648, 3327, 3751),
            new Box("Wilderness: main multi area", 3152, 3752, 3327, 3839),
            new Box("Wilderness: main multi area", 3008, 3856, 3047, 3863),
            new Box("Wilderness: main multi area", 3136, 3840, 3391, 3871),
            new Box("Wilderness: main multi area", 3008, 3864, 3055, 3871),
            new Box("Wilderness: main multi area", 3112, 3872, 3391, 3879),
            new Box("Wilderness: main multi area", 3008, 3872, 3047, 3895),
            new Box("Wilderness: main multi area", 3072, 3880, 3391, 3895),
            new Box("Wilderness: main multi area", 3008, 3896, 3391, 3903),
            new Box("Wilderness: main multi area", 3200, 3904, 3391, 3967),
            new Box("South of wilderness agility training arena", 2984, 3912, 3007, 3927),
            new Box("Wilderness Zamorak temple", 2944, 3816, 2959, 3831),
            new Box("Wilderness bandit camp", 3008, 3600, 3071, 3711),
            new Box("Chaos temple north of Falador", 2928, 3512, 2943, 3519),
            new Box("Burthorpe", 2880, 3520, 2903, 3543),
            new Box("White Wolf Mountain", 2816, 3456, 2879, 3519),
            new Box("Death Plateau", 2848, 3600, 2879, 3607),
            new Box("Trollheim/God Wars", 2896, 3688, 2919, 3695),
            new Box("Trollheim/God Wars", 2880, 3696, 2911, 3727),
            new Box("Trollheim/God Wars", 2888, 3728, 2911, 3743),
            new Box("Trollheim/God Wars", 2880, 3744, 2911, 3775),
            new Box("Northern Rellekka", 2656, 3712, 2735, 3727),
            new Box("Northern Rellekka", 2656, 3728, 2703, 3735),
            new Box("Northern Rellekka", 2712, 3728, 2735, 3735),
            new Box("Northern Fremennik Isles", 2352, 3816, 2367, 3823),
            new Box("Northern Fremennik Isles", 2304, 3824, 2367, 3839),
            new Box("Northern Fremennik Isles", 2304, 3840, 2431, 3903),
            new Box("Pirates Cove", 2176, 3776, 2239, 3839),
            new Box("Lunar Isle", 2048, 3840, 2175, 3967),
            new Box("Piscatoris Fishing Colony", 2304, 3648, 2367, 3711),
            new Box("Ranging Guild", 2656, 3408, 2679, 3415),
            new Box("Ranging Guild", 2648, 3416, 2687, 3439),
            new Box("Ranging Guild", 2656, 3440, 2679, 3447),
            new Box("Necromancer house, southeast of Ardougne", 2664, 3216, 2679, 3231),
            new Box("Necromancer house, southeast of Ardougne", 2656, 3232, 2679, 3255),
            new Box("Battlefield north of Tree Gnome Village", 2504, 3208, 2551, 3231),
            new Box("Battlefield north of Tree Gnome Village", 2504, 3232, 2543, 3247),
            new Box("Castle Wars", 2368, 3072, 2431, 3135),
            new Box("Jiggig", 2456, 3032, 2495, 3055),
            new Box("East Feldip hills, near Rantz", 2648, 2952, 2655, 2975),
            new Box("Ape Atoll", 2688, 2688, 2815, 2815),
            new Box("Pest Control", 2624, 2560, 2687, 2623),
            new Box("Desert Bandit Camp", 3152, 2960, 3191, 2999),
            new Box("Al Kharid", 3264, 3136, 3327, 3199),
            new Box("Wizards Tower", 3094, 3144, 3125, 3175),
            new Box("Draynor Village", 3104, 3232, 3135, 3255),
            new Box("Draynor Village", 3112, 3256, 3135, 3263),
            new Box("Falador", 2944, 3304, 3015, 3327),
            new Box("Falador", 2944, 3328, 3007, 3455),
            new Box("Barbarian Village", 3048, 3392, 3135, 3407),
            new Box("Barbarian Village", 3056, 3408, 3135, 3439),
            new Box("Barbarian Village", 3064, 3440, 3135, 3447),
            new Box("Barbarian Village", 3072, 3448, 3135, 3455),
            // ---- stand-ins for areas built away from their real map squares
            new Box("God Wars Dungeon", 2830, 3695, 2960, 3795)); // the chambers around the GWD landmark (2917, 3745)

    /**
     * Great Kourend's, Kebos' and Fossil Island's surface multicombat polygons, exactly as RuneLite's multi-lines map
     * (MapLocations.java, 2018) has them: Arceuus' streets and houses, Kourend Castle, Hosidius, Shayzien, Lovakengj, the
     * lizardman areas and the ammonite crabs. Kourend's 2019-2021 reworks (Shayzien's rebuild, the Kebos Lowlands) may
     * have moved some lines since (approx); Varlamore's areas are not in the source.
     */
    public static final List<Zone> ZONES = List.of(
            new Zone("Fossil Island: ammonite crabs (north-west)", 3648, 3885, 3663, 3885, 3663, 3882, 3664, 3882, 3664, 3872, 3663, 3872, 3663, 3868, 3648, 3868),
            new Zone("Fossil Island: ammonite crabs (north coast)", 3680, 3904, 3744, 3904, 3744, 3856, 3756, 3856, 3756, 3852, 3755, 3852, 3755, 3851, 3754, 3851,
                    3754, 3850, 3751, 3850, 3751, 3849, 3750, 3849, 3750, 3848, 3749, 3848, 3749, 3847, 3748, 3847,
                    3748, 3846, 3747, 3846, 3747, 3845, 3746, 3845, 3746, 3844, 3742, 3844, 3742, 3845, 3740, 3845,
                    3740, 3844, 3732, 3844, 3732, 3843, 3730, 3843, 3730, 3842, 3724, 3842, 3724, 3843, 3717, 3843,
                    3717, 3842, 3712, 3842, 3712, 3846, 3710, 3846, 3710, 3847, 3709, 3847, 3709, 3848, 3708, 3848,
                    3708, 3859, 3709, 3859, 3709, 3860, 3710, 3860, 3710, 3861, 3712, 3861, 3712, 3866, 3713, 3866,
                    3713, 3870, 3714, 3870, 3714, 3873, 3713, 3873, 3713, 3876, 3712, 3876, 3712, 3881, 3710, 3881,
                    3710, 3888, 3712, 3888, 3712, 3890, 3714, 3890, 3714, 3891, 3716, 3891, 3716, 3892, 3717, 3892,
                    3717, 3893, 3716, 3893, 3716, 3894, 3714, 3894, 3714, 3895, 3713, 3895, 3713, 3896, 3712, 3896,
                    3712, 3897, 3705, 3897, 3705, 3898, 3704, 3898, 3704, 3899, 3692, 3899, 3692, 3898, 3688, 3898,
                    3688, 3897, 3686, 3897, 3686, 3896, 3680, 3896),
            new Zone("Kebos: ice giants and wolves south-west of the Wintertodt", 1540, 3898, 1543, 3898, 1543, 3901, 1546, 3901, 1546, 3903, 1547, 3903, 1547, 3904, 1550, 3904,
                    1550, 3903, 1553, 3903, 1553, 3904, 1559, 3904, 1559, 3902, 1564, 3902, 1564, 3903, 1565, 3903,
                    1565, 3904, 1568, 3904, 1568, 3903, 1569, 3903, 1569, 3902, 1570, 3902, 1570, 3901, 1573, 3901,
                    1573, 3898, 1577, 3898, 1577, 3899, 1578, 3899, 1578, 3902, 1579, 3902, 1579, 3903, 1584, 3903,
                    1584, 3902, 1586, 3902, 1586, 3901, 1590, 3901, 1590, 3891, 1588, 3891, 1588, 3887, 1572, 3887,
                    1572, 3872, 1567, 3872, 1567, 3868, 1563, 3868, 1563, 3867, 1558, 3867, 1558, 3868, 1557, 3868,
                    1557, 3870, 1549, 3870, 1549, 3874, 1545, 3874, 1545, 3876, 1543, 3876, 1543, 3877, 1542, 3877,
                    1542, 3879, 1541, 3879, 1541, 3882, 1539, 3882, 1539, 3887, 1540, 3887, 1540, 3888, 1539, 3888,
                    1539, 3894, 1540, 3894),
            new Zone("Arceuus", 1664, 3776, 1664, 3785, 1667, 3785, 1667, 3805, 1671, 3805, 1671, 3811, 1675, 3811, 1675, 3819,
                    1690, 3819, 1690, 3814, 1695, 3814, 1695, 3806, 1719, 3806, 1719, 3787, 1725, 3787, 1725, 3778,
                    1711, 3778, 1711, 3776),
            new Zone("Arceuus: lectern house", 1667, 3772, 1679, 3772, 1679, 3775, 1691, 3775, 1691, 3761, 1679, 3761, 1679, 3764, 1667, 3764),
            new Zone("Arceuus: house east of the lectern house", 1696, 3775, 1708, 3775, 1708, 3763, 1696, 3763),
            new Zone("Arceuus: house east of the lectern house", 1713, 3775, 1727, 3775, 1727, 3763, 1724, 3763, 1724, 3752, 1716, 3752, 1716, 3763, 1713, 3763),
            new Zone("Arceuus: rune shop", 1716, 3750, 1728, 3750, 1728, 3736, 1716, 3736),
            new Zone("Arceuus: general store", 1717, 3732, 1725, 3732, 1725, 3715, 1715, 3715, 1715, 3725, 1717, 3725),
            new Zone("Arceuus: pub", 1683, 3732, 1691, 3732, 1691, 3725, 1697, 3725, 1697, 3730, 1703, 3730, 1703, 3712, 1683, 3712),
            new Zone("Arceuus: staff store", 1664, 3732, 1676, 3732, 1676, 3720, 1664, 3720),
            new Zone("Arceuus: house west of the staff store", 1647, 3738, 1655, 3738, 1655, 3726, 1658, 3726, 1658, 3714, 1644, 3714, 1644, 3726, 1647, 3726),
            new Zone("Arceuus: house north of the staff store", 1647, 3762, 1657, 3762, 1657, 3752, 1655, 3752, 1655, 3745, 1647, 3745),
            new Zone("Arceuus: magic tree house", 1682, 3755, 1692, 3755, 1692, 3745, 1690, 3745, 1690, 3738, 1682, 3738),
            new Zone("Arceuus: west of the magic tree house", 1667, 3756, 1675, 3756, 1675, 3740, 1665, 3740, 1665, 3746, 1667, 3746),
            new Zone("Arceuus and Port Piscarilius' west side", 1728, 3808, 1792, 3808, 1792, 3764, 1856, 3764, 1856, 3712, 1792, 3712, 1792, 3648, 1664, 3648,
                    1664, 3706, 1665, 3706, 1665, 3705, 1668, 3705, 1668, 3706, 1671, 3706, 1671, 3705, 1675, 3705,
                    1675, 3704, 1683, 3704, 1683, 3701, 1684, 3701, 1684, 3700, 1686, 3700, 1686, 3702, 1687, 3702,
                    1687, 3700, 1688, 3700, 1688, 3701, 1690, 3701, 1690, 3703, 1689, 3703, 1689, 3704, 1690, 3704,
                    1690, 3705, 1704, 3705, 1704, 3707, 1706, 3707, 1706, 3712, 1711, 3712, 1711, 3711, 1710, 3711,
                    1710, 3710, 1712, 3710, 1712, 3707, 1728, 3707),
            new Zone("Kourend Castle", 1614, 3691, 1619, 3691, 1619, 3690, 1620, 3690, 1620, 3689, 1653, 3689, 1653, 3690, 1654, 3690,
                    1654, 3691, 1657, 3691, 1657, 3690, 1658, 3690, 1658, 3689, 1659, 3689, 1659, 3686, 1658, 3686,
                    1658, 3685, 1657, 3685, 1657, 3662, 1658, 3662, 1658, 3661, 1659, 3661, 1659, 3658, 1658, 3658,
                    1658, 3657, 1657, 3657, 1657, 3656, 1654, 3656, 1654, 3657, 1653, 3657, 1653, 3658, 1620, 3658,
                    1620, 3657, 1619, 3657, 1619, 3656, 1614, 3656, 1614, 3657, 1613, 3657, 1613, 3661, 1612, 3661,
                    1612, 3662, 1611, 3662, 1611, 3663, 1600, 3663, 1600, 3662, 1599, 3662, 1599, 3661, 1594, 3661,
                    1594, 3662, 1593, 3662, 1593, 3685, 1594, 3685, 1594, 3686, 1599, 3686, 1599, 3685, 1600, 3685,
                    1600, 3684, 1611, 3684, 1611, 3685, 1612, 3685, 1612, 3686, 1613, 3686, 1613, 3690, 1614, 3690),
            new Zone("Hosidius (west), the Woodcutting Guild and the western sand crabs", 1650, 3648, 1664, 3648, 1664, 3520, 1689, 3520, 1689, 3496, 1707, 3496, 1707, 3485, 1708, 3485,
                    1708, 3484, 1710, 3484, 1710, 3483, 1713, 3483, 1713, 3482, 1720, 3482, 1720, 3481, 1721, 3481,
                    1721, 3480, 1722, 3480, 1722, 3479, 1723, 3479, 1723, 3478, 1724, 3478, 1724, 3477, 1726, 3477,
                    1726, 3476, 1728, 3476, 1728, 3472, 1708, 3472, 1708, 3456, 1600, 3456, 1600, 3584, 1608, 3584,
                    1608, 3616, 1650, 3616),
            new Zone("Hosidius: sand crab beach", 1740, 3478, 1741, 3478, 1741, 3479, 1745, 3479, 1745, 3480, 1751, 3480, 1751, 3479, 1752, 3479,
                    1752, 3478, 1753, 3478, 1753, 3477, 1755, 3477, 1755, 3476, 1757, 3476, 1757, 3475, 1758, 3475,
                    1758, 3474, 1759, 3474, 1759, 3473, 1779, 3473, 1779, 3474, 1781, 3474, 1781, 3475, 1786, 3475,
                    1786, 3476, 1800, 3476, 1800, 3475, 1805, 3475, 1805, 3474, 1807, 3474, 1807, 3473, 1808, 3473,
                    1808, 3472, 1810, 3472, 1810, 3471, 1833, 3471, 1833, 3470, 1834, 3470, 1834, 3469, 1852, 3469,
                    1852, 3449, 1792, 3449, 1792, 3424, 1800, 3424, 1800, 3449, 1800, 3400, 1728, 3400, 1728, 3462,
                    1729, 3462, 1729, 3466, 1730, 3466, 1730, 3469, 1731, 3469, 1731, 3470, 1732, 3470, 1732, 3471,
                    1733, 3471, 1733, 3473, 1734, 3473, 1734, 3474, 1736, 3474, 1736, 3475, 1737, 3475, 1737, 3476,
                    1738, 3476, 1738, 3477, 1740, 3477),
            new Zone("Hosidius (east)", 1834, 3584, 1888, 3584, 1888, 3528, 1856, 3528, 1856, 3520, 1834, 3520, 1834, 3522, 1833, 3522,
                    1833, 3535, 1834, 3535, 1834, 3538, 1835, 3538, 1835, 3539, 1836, 3539, 1836, 3540, 1837, 3540,
                    1837, 3541, 1838, 3541, 1838, 3542, 1840, 3542, 1840, 3543, 1841, 3543, 1841, 3545, 1842, 3545,
                    1842, 3546, 1844, 3546, 1844, 3547, 1845, 3547, 1845, 3548, 1851, 3548, 1851, 3551, 1853, 3551,
                    1853, 3563, 1851, 3563, 1851, 3566, 1847, 3566, 1847, 3567, 1845, 3567, 1845, 3568, 1844, 3568,
                    1844, 3569, 1843, 3569, 1843, 3571, 1842, 3571, 1842, 3573, 1841, 3573, 1841, 3574, 1840, 3574,
                    1840, 3575, 1839, 3575, 1839, 3576, 1838, 3576, 1838, 3577, 1837, 3577, 1837, 3578, 1836, 3578,
                    1836, 3579, 1835, 3579, 1835, 3581, 1834, 3581),
            new Zone("Hosidius (east): a single multi tile", 1849, 3563, 1849, 3564, 1850, 3564, 1850, 3563),
            new Zone("Hosidius: cows, chickens and pigs", 1792, 3513, 1802, 3513, 1802, 3520, 1810, 3520, 1810, 3513, 1816, 3513, 1816, 3512, 1836, 3512,
                    1836, 3494, 1796, 3494, 1796, 3495, 1792, 3495),
            new Zone("Hosidius: south-east of the Tithe Farm", 1777, 3597, 1794, 3597, 1794, 3561, 1777, 3561, 1777, 3591, 1779, 3591, 1779, 3592, 1777, 3592),
            new Zone("West of Shayzien", 1408, 3584, 1408, 3582, 1486, 3582, 1486, 3568, 1528, 3568, 1528, 3520, 1408, 3520, 1408, 3464,
                    1380, 3464, 1380, 3486, 1377, 3486, 1377, 3488, 1373, 3488, 1373, 3492, 1364, 3492, 1364, 3512,
                    1358, 3512, 1358, 3520, 1356, 3520, 1356, 3532, 1358, 3532, 1358, 3540, 1359, 3540, 1359, 3542,
                    1360, 3542, 1360, 3557, 1356, 3557, 1356, 3560, 1351, 3560, 1351, 3570, 1354, 3570, 1354, 3581,
                    1346, 3581, 1346, 3584),
            new Zone("South of the Chambers of Xeric", 1261, 3489, 1259, 3489, 1259, 3488, 1255, 3488, 1255, 3487, 1243, 3487, 1243, 3490, 1234, 3490,
                    1234, 3480, 1192, 3480, 1192, 3568, 1209, 3568, 1209, 3548, 1215, 3548, 1215, 3544, 1217, 3544,
                    1217, 3536, 1235, 3536, 1235, 3532, 1249, 3532, 1249, 3525, 1248, 3525, 1248, 3517, 1254, 3517,
                    1254, 3513, 1274, 3513, 1274, 3510, 1296, 3510, 1296, 3511, 1300, 3511, 1300, 3501, 1287, 3501,
                    1287, 3490, 1280, 3490, 1280, 3489, 1264, 3489, 1264, 3490, 1261, 3490),
            new Zone("Lizardman shamans (Lizardman Canyon)", 1416, 3728, 1456, 3728, 1456, 3688, 1416, 3688),
            new Zone("Lizardmen near Shayzien (west)", 1472, 3712, 1510, 3712, 1510, 3702, 1509, 3702, 1509, 3701, 1506, 3701, 1506, 3696, 1500, 3696,
                    1500, 3680, 1472, 3680),
            new Zone("Lizardmen near Shayzien (east)", 1538, 3704, 1560, 3704, 1560, 3672, 1538, 3672),
            new Zone("Lovakengj", 1600, 3712, 1472, 3712, 1472, 3840, 1547, 3840, 1547, 3816, 1556, 3816, 1556, 3809, 1562, 3809,
                    1562, 3800, 1568, 3800, 1568, 3793, 1571, 3793, 1571, 3816, 1571, 3776, 1600, 3776),
            new Zone("Shayzien", 1475, 3587, 1475, 3641, 1534, 3641, 1534, 3587),
            new Zone("Shayzien: general store", 1539, 3640, 1551, 3640, 1551, 3621, 1539, 3621),
            new Zone("Kourend Woodland: barbarian camp", 1572, 3442, 1591, 3442, 1591, 3424, 1572, 3424));

    /** Single-way pockets inside multicombat areas (RuneLite's NOT_MULTICOMBAT polygons). */
    public static final List<Zone> SINGLE_WAY = List.of(
            new Zone("Falador Castle's south-west corner (ground floor)", 2968, 3336, 2968, 3328, 2960, 3328, 2960, 3336),
            new Zone("Crabclaw Isle: the landing tile", 1777, 3416, 1777, 3417, 1778, 3417, 1778, 3416),
            new Zone("Shayzien bank", 1495, 3622, 1515, 3622, 1515, 3612, 1495, 3612));

    /** Multicombat dungeons and lairs, by the place they are built around. */
    public static final List<Around> AROUND = List.of(
            new Around("Revenant Caves", "revenant_caves", 25),
            new Around("Scorpia's cave", "scorpias_cave", 15),
            new Around("Catacombs of Kourend", "catacombs_of_kourend", 20),
            new Around("Smoke Devil Dungeon", "smoke_devil_dungeon", 20),
            new Around("Kalphite Lair", "kalphite_lair", 20),
            new Around("Kraken Cove", "kraken_cove", 15),
            new Around("Abyssal Nexus", "abyssal_nexus", 15),
            new Around("Corporeal Beast's lair", "corporeal_beast_lair", 15),
            new Around("Waterbirth Island Dungeon", "waterbirth_island", 15),
            new Around("Grotesque Guardians' rooftop", "slayer_tower_roof", 10),
            new Around("Zalcano's prison", "zalcano_chamber", 20),
            new Around("The Hueycoatl's lair", "hueycoatl_lair", 20),
            new Around("Royal Titans' lair", "royal_titans_lair", 15),
            new Around("Lizardman Settlement", "zeah_lizardman_settlement", 20), // approx
            new Around("Lizardman Canyon", "lizardman_canyon", 15),
            new Around("Bandit Camp (desert)", "bandit_camp", 15)); // approx

    /** Bosses fought in multicombat lairs: their arenas ({@link ArenaLayout}) are multi-way. */
    public static final List<String> MULTI_BOSS_ARENAS = List.of("kreearra", "commander_zilyana", "nex", "callisto",
            "venenatis", "vetion", "scorpia", "chaos_elemental", "corporeal_beast", "kalphite_queen", "giant_mole",
            "sarachnis", "thermonuclear_smoke_devil", "abyssal_sire", "dusk", "zalcano", "the_hueycoatl",
            "branda_the_fire_queen", "scurrius"); // the Royal Titans share Branda's arena

    /**
     * Multicombat dungeons carved from the cache's tiles beneath their surface entrances, by structure id: their carved
     * air (the structure's CLEAR boxes below y {@link #UNDERGROUND_TOP}) is multi-way, the surface above them is not.
     * OSRS wiki: Waterbirth Island Dungeon (with the Dagannoth Kings' lair), the Lighthouse dungeon, Kruk's Dungeon and the
     * Charred Dungeon (all but its lava strykewyrms, which are single-way in OSRS: approx) are multicombat throughout. The Stalker
     * Den (south-western sections only) and Tonali Cavern (the Moon and Sun chambers) are only partly multi and stay single here.
     */
    public static final List<String> UNDERGROUND = List.of("dng_kalphite_cave", "dng_shadow_dungeon", "dng_waterbirth_dungeon",
            "dng_lighthouse_dungeon", "dng_kruks_dungeon", "dng_charred_dungeon");
    /** Highest block y that counts as underground for {@link #UNDERGROUND} (the dungeons' DEEP_TOP). */
    public static final int UNDERGROUND_TOP = 50;

    private static volatile List<Box> resolved;
    private static volatile List<com.runecraft.osrs.world.StructureDef.Op> undergroundAir;

    private MultiCombat() {
    }

    /** Every multi area as a tile box (place areas resolved to their coordinates; unknown places skipped). */
    public static List<Box> areas() {
        List<Box> r = resolved;
        if (r == null) {
            List<Box> all = new ArrayList<>(BOXES);
            for (Around a : AROUND) {
                int[] c = placeCentre(a.place());
                if (c != null) {
                    all.add(new Box(a.name(), c[0] - a.radius(), c[1] - a.radius(), c[0] + a.radius(), c[1] + a.radius()));
                }
            }
            for (String boss : MULTI_BOSS_ARENAS) {
                ArenaLayout.Arena arena = ArenaLayout.of(boss);
                if (arena != null) {
                    int e = arena.radius() + 4;
                    all.add(new Box(boss + " arena", GielinorMap.toOsrsX(arena.x() - e), GielinorMap.toOsrsY(arena.z() - e),
                            GielinorMap.toOsrsX(arena.x() + e), GielinorMap.toOsrsY(arena.z() + e)));
                }
            }
            r = List.copyOf(all);
            resolved = r;
        }
        return r;
    }

    /** OSRS coordinates of a module place or Landmark, or null if unknown. */
    public static int[] placeCentre(String place) {
        PlaceDef p = ContentRegistry.place(place);
        if (p != null) {
            return new int[]{p.osrsX(), p.osrsY()};
        }
        Landmark l = Landmark.byName(place);
        return l == null ? null : new int[]{l.osrsX(), l.osrsY()};
    }

    /** True if the OSRS tile is in a multicombat area. */
    public static boolean isMulti(int osrsX, int osrsY) {
        return area(osrsX, osrsY) != null;
    }

    /** The multicombat area containing the tile, or null in single-way combat. */
    public static Box area(int osrsX, int osrsY) {
        for (Zone z : SINGLE_WAY) {
            if (z.contains(osrsX, osrsY)) {
                return null;
            }
        }
        for (Box b : areas()) {
            if (b.contains(osrsX, osrsY)) {
                return b;
            }
        }
        for (Zone z : ZONES) {
            if (z.contains(osrsX, osrsY)) {
                return z.bounds();
            }
        }
        return null;
    }

    /** Multicombat check at a Minecraft block position. */
    public static boolean isMultiAtBlock(int blockX, int blockZ) {
        return isMulti(GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ));
    }

    /** Multicombat check at a Minecraft block position, including the {@link #UNDERGROUND} dungeons. */
    public static boolean isMultiAtBlock(int blockX, int blockY, int blockZ) {
        return isMultiAtBlock(blockX, blockZ) || inUndergroundMulti(blockX, blockY, blockZ);
    }

    /** True if the block is in the carved air of an {@link #UNDERGROUND} multicombat dungeon. */
    public static boolean inUndergroundMulti(int blockX, int blockY, int blockZ) {
        if (blockY > UNDERGROUND_TOP) {
            return false;
        }
        List<com.runecraft.osrs.world.StructureDef.Op> air = undergroundAir;
        if (air == null) {
            air = new ArrayList<>();
            for (com.runecraft.osrs.world.StructureDef s : ContentRegistry.structures()) {
                if (UNDERGROUND.contains(s.id())) {
                    for (com.runecraft.osrs.world.StructureDef.Op op : s.ops()) {
                        if (op.type() == com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR && op.y2() <= UNDERGROUND_TOP) {
                            air.add(op);
                        }
                    }
                }
            }
            air = List.copyOf(air);
            undergroundAir = air;
        }
        for (com.runecraft.osrs.world.StructureDef.Op op : air) {
            if (blockX >= op.x1() && blockX <= op.x2() && blockZ >= op.z1() && blockZ <= op.z2() && blockY >= op.y1() - 1
                    && blockY <= op.y2()) {
                return true;
            }
        }
        return false;
    }
}
