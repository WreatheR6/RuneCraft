package com.runecraft.osrs;

/**
 * Maps Minecraft block coordinates to Old School RuneScape world coordinates and kingdoms.
 *
 * <p>Lumbridge (OSRS 3222, 3218) sits at block (0, 0). Each OSRS tile is {@link #SCALE} blocks,
 * OSRS "y" grows northwards so it becomes negative Minecraft Z. Region boundaries follow the
 * real world map (e.g. the Wilderness begins at the ditch, y = 3520) with a little noise so
 * borders aren't perfectly straight.
 */
public final class GielinorMap {
    public static final int SCALE = 2;
    public static final int ORIGIN_X = 3222;
    public static final int ORIGIN_Y = 3218;
    /** OSRS y coordinate of the Wilderness ditch. */
    public static final int WILDERNESS_DITCH = 3520;

    private GielinorMap() {
    }

    public static int toBlockX(int osrsX) {
        return (osrsX - ORIGIN_X) * SCALE;
    }

    public static int toBlockZ(int osrsY) {
        return -(osrsY - ORIGIN_Y) * SCALE;
    }

    public static int toOsrsX(int blockX) {
        return Math.floorDiv(blockX, SCALE) + ORIGIN_X;
    }

    public static int toOsrsY(int blockZ) {
        return -Math.floorDiv(blockZ, SCALE) + ORIGIN_Y;
    }

    /** Wilderness level (1-56) at a block position, or 0 outside the Wilderness. */
    public static int wildernessLevel(int blockX, int blockZ) {
        int x = toOsrsX(blockX);
        int y = toOsrsY(blockZ);
        if (x < 2944 || x > 3392 || y < WILDERNESS_DITCH || y > 3967) {
            return 0;
        }
        return (y - WILDERNESS_DITCH) / 8 + 1;
    }

    public static Region regionAtBlock(int blockX, int blockZ) {
        // Small wobble (up to ~12 tiles) so borders look natural.
        double wx = Noise.value(blockX * 0.01, blockZ * 0.01, 17) * 12.0;
        double wy = Noise.value(blockX * 0.01, blockZ * 0.01, 91) * 12.0;
        return regionAt(toOsrsX(blockX) + wx, toOsrsY(blockZ) + wy);
    }

    /**
     * Kingdom at OSRS coordinates; areas are checked in priority order. The borders follow the OSRS
     * kingdoms as the wiki and Konar quo Maten's location list group places:
     * <ul>
     *   <li>Misthalin: Lumbridge, Draynor, the Wizards' Tower, Varrock, Barbarian Village, Edgeville;</li>
     *   <li>Asgarnia: Falador, Port Sarim, Rimmington, Mudskipper Point, Entrana, Ice Mountain, Goblin
     *       Village, Taverley (east of White Wolf Mountain) and Burthorpe with the Warriors' Guild;</li>
     *   <li>Kharidian Desert: Al Kharid (east of the River Lum, with its mine and the arenas), the
     *       desert west of the Shantay Pass (Kalphite Lair) and everything south to Menaphos and Uzer;</li>
     *   <li>Kandarin: Catherby to Yanille plus the Gnome Stronghold, Eagles' Peak, Piscatoris and
     *       Kraken Cove, and the Barbarian Outpost; Tirannwn starts west of the Arandar pass;</li>
     *   <li>Fremennik Province: Rellekka and the Lighthouse north of Seers' Village, the Fremennik Isles,
     *       Waterbirth Island and the Island of Stone; Troll Country: Death Plateau, Trollheim and
     *       Trollweiss.</li>
     * </ul>
     * Places off these coasts (Zeah, Fossil Island, Lithkren, Mos Le'Harmless...) are the Uncharted Isles.
     */
    public static Region regionAt(double x, double y) {
        if (in(x, y, 2944, 3392, WILDERNESS_DITCH, 3968)) {
            return Region.WILDERNESS;
        }
        // Fremennik islands: Waterbirth Island, the Island of Stone, and the Fremennik Isles (Jatizso, Neitiznot).
        if (ellipse(x, y, 2530, 3748, 30, 36) || ellipse(x, y, 2470, 3992, 22, 18) || in(x, y, 2296, 2450, 3758, 3862)) {
            return Region.FREMENNIK_PROVINCE;
        }
        // Morytania: east of the River Salve, from Burgh de Rott's south coast to the Slayer Tower.
        if (in(x, y, 3395, 3900, 3150, 3625)) {
            return Region.MORYTANIA;
        }
        // Al Kharid (east of the River Lum, with its mine and the arenas), the desert west of the
        // Shantay Pass down to the coast (Kalphite Lair), and the open desert to Uzer and Menaphos.
        if (in(x, y, 3268, 3395, 3125, 3335) || in(x, y, 3160, 3660, 3060, 3125) || in(x, y, 3130, 3660, 2600, 3060)
                || in(x, y, 3395, 3660, 3125, 3150)) {
            return Region.KHARIDIAN_DESERT;
        }
        if (in(x, y, 3135, 3268, 3125, 3197)) {
            return Region.LUMBRIDGE_SWAMP;
        }
        // Misthalin; south of Draynor only the Wizards' Tower peninsula (the bay west of it is Asgarnia's).
        if (in(x, y, 3070, 3395, 3195, WILDERNESS_DITCH) || in(x, y, 3080, 3268, 3125, 3197)) {
            return Region.MISTHALIN;
        }
        // Death Plateau, Trollheim, God Wars and north to Trollweiss.
        if (in(x, y, 2840, 2944, 3580, 3995)) {
            return Region.TROLL_COUNTRY;
        }
        // Crandor (off Karamja) before Asgarnia's west coast, and Entrana.
        if (in(x, y, 2800, 2875, 3220, 3302)) {
            return Region.KARAMJA;
        }
        if (in(x, y, 2795, 2880, 3302, 3376)) {
            return Region.ASGARNIA;
        }
        // Asgarnia: Rimmington to Falador, Taverley (east of White Wolf Mountain), Burthorpe with the
        // Warriors' Guild, and in the south Port Sarim's bay and Mudskipper Point.
        // (Catherby's shore runs east to x 2862, so Asgarnia's strip west of Taverley starts at 2865 north of Entrana.)
        if (in(x, y, 2895, 3070, 3195, WILDERNESS_DITCH) || in(x, y, 2845, 2895, 3245, 3400) || in(x, y, 2865, 2895, 3400, WILDERNESS_DITCH)
                || in(x, y, 2845, 2944, WILDERNESS_DITCH, 3580) || in(x, y, 2975, 3040, 3080, 3195)
                || in(x, y, 3040, 3080, 3140, 3195)) {
            return Region.ASGARNIA;
        }
        // Karamja (Brimhaven to Shilo Village, Cairn Isle, Musa Point).
        if (in(x, y, 2700, 2845, 2840, 3240) || in(x, y, 2845, 2975, 2840, 3195)) {
            return Region.KARAMJA;
        }
        // Fremennik Province: north of Seers' Village and the Barbarian Outpost, west of Troll Country.
        if (in(x, y, 2580, 2840, 3560, 3995) || in(x, y, 2490, 2580, 3600, 3995)) {
            return Region.FREMENNIK_PROVINCE;
        }
        // Tirannwn: west of the Arandar pass (Isafdar, Prifddinas, Lletya, Port Tyras, the Poison Waste).
        if (in(x, y, 2100, 2350, 2990, 3430)) {
            return Region.TIRANNWN;
        }
        // Kandarin, including the Gnome Stronghold, Eagles' Peak, Piscatoris and Kraken Cove.
        if (in(x, y, 2350, 2845, 3060, 3560) || in(x, y, 2845, 2865, 3400, 3560) || in(x, y, 2230, 2490, 3430, 3730)
                || in(x, y, 2490, 2580, 3560, 3600)) {
            return Region.KANDARIN;
        }
        if (in(x, y, 2350, 2700, 2780, 3060)) {
            return Region.FELDIP_HILLS;
        }
        return Region.UNCHARTED_ISLES;
    }

    private static boolean in(double x, double y, int minX, int maxX, int minY, int maxY) {
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }

    private static boolean ellipse(double x, double y, double cx, double cy, double rx, double ry) {
        double dx = (x - cx) / rx;
        double dy = (y - cy) / ry;
        return dx * dx + dy * dy <= 1;
    }

    /** Tiny deterministic value noise, so region borders are identical on client and server. */
    static final class Noise {
        private Noise() {
        }

        static double value(double x, double z, int seed) {
            int x0 = (int) Math.floor(x);
            int z0 = (int) Math.floor(z);
            double fx = smooth(x - x0);
            double fz = smooth(z - z0);
            double a = hash(x0, z0, seed);
            double b = hash(x0 + 1, z0, seed);
            double c = hash(x0, z0 + 1, seed);
            double d = hash(x0 + 1, z0 + 1, seed);
            double top = a + (b - a) * fx;
            double bottom = c + (d - c) * fx;
            return top + (bottom - top) * fz;
        }

        private static double smooth(double t) {
            return t * t * (3 - 2 * t);
        }

        /** Hash to [-1, 1]. */
        private static double hash(int x, int z, int seed) {
            long h = x * 374761393L + z * 668265263L + seed * 2147483647L;
            h = (h ^ (h >>> 13)) * 1274126177L;
            h ^= (h >>> 16);
            return ((h & 0xFFFF) / 32767.5) - 1.0;
        }
    }
}
