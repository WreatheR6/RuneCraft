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

    /** Kingdom at OSRS coordinates; rectangles are checked in priority order. */
    public static Region regionAt(double x, double y) {
        if (in(x, y, 2944, 3392, WILDERNESS_DITCH, 3968)) {
            return Region.WILDERNESS;
        }
        if (in(x, y, 3395, 3900, 3150, 3600)) {
            return Region.MORYTANIA;
        }
        if (in(x, y, 3268, 3395, 3060, 3330) || in(x, y, 3130, 3560, 2700, 3060)) {
            return Region.KHARIDIAN_DESERT;
        }
        if (in(x, y, 3140, 3265, 3140, 3195)) {
            return Region.LUMBRIDGE_SWAMP;
        }
        if (in(x, y, 3070, 3395, 3140, WILDERNESS_DITCH)) {
            return Region.MISTHALIN;
        }
        if (in(x, y, 2840, 2944, 3600, 3780)) {
            return Region.TROLL_COUNTRY;
        }
        if (in(x, y, 2860, 3070, 3195, WILDERNESS_DITCH) || in(x, y, 2860, 2944, WILDERNESS_DITCH, 3600)) {
            return Region.ASGARNIA;
        }
        if (in(x, y, 2740, 2990, 2880, 3195)) {
            return Region.KARAMJA;
        }
        if (in(x, y, 2540, 2840, 3560, 3900)) {
            return Region.FREMENNIK_PROVINCE;
        }
        if (in(x, y, 2440, 2860, 3060, 3560)) {
            return Region.KANDARIN;
        }
        if (in(x, y, 2440, 2740, 2800, 3060)) {
            return Region.FELDIP_HILLS;
        }
        if (in(x, y, 2150, 2440, 3050, 3420)) {
            return Region.TIRANNWN;
        }
        return Region.UNCHARTED_ISLES;
    }

    private static boolean in(double x, double y, int minX, int maxX, int minY, int maxY) {
        return x >= minX && x < maxX && y >= minY && y < maxY;
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
