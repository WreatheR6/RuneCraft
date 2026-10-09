package com.runecraft.osrs;

/**
 * The exact Old School RuneScape experience curve.
 *
 * <p>XP needed for level L is {@code floor(1/4 * sum_{n=1}^{L-1} floor(n + 300 * 2^(n/7)))}.
 * Level 2 needs 83 XP, level 99 needs 13,034,431 XP, and experience is capped at 200,000,000
 * per skill. Like the real game, experience is stored internally in tenths of a point so that
 * fractional rewards (e.g. 37.5 XP for an oak log) accumulate exactly.
 */
public final class Experience {
    public static final int MAX_LEVEL = 99;
    /** Highest "virtual" level shown once a skill passes 99 (reached at 200M XP). */
    public static final int MAX_VIRTUAL_LEVEL = 126;
    public static final int MAX_XP = 200_000_000;
    public static final int MAX_XP_TENTHS = MAX_XP * 10;

    private static final int[] XP_TABLE = new int[MAX_VIRTUAL_LEVEL + 2];

    static {
        double points = 0;
        XP_TABLE[1] = 0;
        for (int level = 1; level <= MAX_VIRTUAL_LEVEL; level++) {
            points += Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            XP_TABLE[level + 1] = (int) Math.floor(points / 4.0);
        }
    }

    private Experience() {
    }

    /** Total experience required to reach {@code level} (1-127). */
    public static int xpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level > MAX_VIRTUAL_LEVEL + 1) {
            level = MAX_VIRTUAL_LEVEL + 1;
        }
        return XP_TABLE[level];
    }

    /** Real level (capped at 99) for an amount of experience in whole points. */
    public static int levelForXp(int xp) {
        return Math.min(MAX_LEVEL, virtualLevelForXp(xp));
    }

    /** Virtual level (up to 126) for an amount of experience in whole points. */
    public static int virtualLevelForXp(int xp) {
        int level = 1;
        while (level < MAX_VIRTUAL_LEVEL && xp >= XP_TABLE[level + 1]) {
            level++;
        }
        return level;
    }

    public static int levelForTenths(int tenths) {
        return levelForXp(tenths / 10);
    }

    /** Adds experience (both in tenths), clamping at the 200M cap and guarding overflow. */
    public static int addTenths(int currentTenths, int gainedTenths) {
        long sum = (long) currentTenths + Math.max(0, gainedTenths);
        return (int) Math.min(MAX_XP_TENTHS, sum);
    }

    /** Converts a decimal XP reward (e.g. 37.5) to tenths. */
    public static int tenths(double xp) {
        return (int) Math.round(xp * 10.0);
    }

    /** Formats tenths the way the game does: "1,234.5" or "1,234". */
    public static String format(int tenths) {
        int whole = tenths / 10;
        int frac = tenths % 10;
        String w = String.format(java.util.Locale.ROOT, "%,d", whole);
        return frac == 0 ? w : w + "." + frac;
    }
}
