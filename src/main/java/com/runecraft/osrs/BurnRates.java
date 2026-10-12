package com.runecraft.osrs;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooking burn rates. OSRS decides every cook with the standard skilling roll
 * ({@link Chance#lerp}) using per-food low/high values, with separate values for some foods on a
 * range and while wearing cooking gauntlets. The table holds the known values (from the game's
 * original cooking data, as charted on the OSRS Wiki); food without an entry gets a curve derived
 * from its stop-burn level on the {@code high = 2 * low + 256} line that almost all foods share.
 */
public final class BurnRates {
    private BurnRates() {
    }

    /** Low/high values out of 256 for a fire, a range and with cooking gauntlets (0/0 = same as fire). */
    public record Rates(int fireLow, int fireHigh, int rangeLow, int rangeHigh, int gauntletLow, int gauntletHigh) {
        public boolean hasRange() {
            return rangeLow > 0 || rangeHigh > 0;
        }

        public boolean hasGauntlets() {
            return gauntletLow > 0 || gauntletHigh > 0;
        }
    }

    private static final Map<String, Rates> BY_ITEM = new HashMap<>();

    private static void put(int low, int high, String... cooked) {
        for (String c : cooked) {
            BY_ITEM.put(c, new Rates(low, high, 0, 0, 0, 0));
        }
    }

    private static void put(String cooked, int low, int high, int rangeLow, int rangeHigh, int gLow, int gHigh) {
        BY_ITEM.put(cooked, new Rates(low, high, rangeLow, rangeHigh, gLow, gHigh));
    }

    static {
        put(128, 512, "shrimps", "anchovies");
        put(118, 492, "sardine", "bread");
        put(108, 472, "herring");
        put(98, 452, "mackerel", "redberry_pie");
        put(88, 432, "trout");
        put(78, 412, "pike", "meat_pie");
        put(68, 392, "salmon", "stew");
        put(58, 372, "tuna", "apple_pie");
        put(48, 352, "plain_pizza");
        put(38, 332, "curry", "cake");
        put(33, 312, "bass");
        put("cod", 83, 422, 88, 432, 0, 0);
        put("lobster", 38, 332, 0, 0, 55, 368);
        put("swordfish", 18, 292, 30, 310, 0, 0);
        put("shark", 1, 202, 1, 232, 15, 270);
        put("sea_turtle", 1, 202, 1, 222, 0, 0);
        put("manta_ray", 1, 202, 1, 222, 0, 0);
    }

    /** Known rates for a cooked item, or null. */
    public static Rates of(String cookedItem) {
        return BY_ITEM.get(cookedItem);
    }

    /** First level at which a low/high pair never burns, or 100 if it never gets there. */
    public static int stopLevel(int low, int high) {
        for (int level = 1; level <= 99; level++) {
            if (Chance.lerp(level, low, high) >= 1.0) {
                return level;
            }
        }
        return 100;
    }

    /**
     * A low/high pair (on the {@code high = 2 * low + 256} line) that stops burning at
     * {@code stopLevel}. Food that never stops burning (100+) gets the shark's fire curve, 1/202.
     */
    public static int[] fromStopLevel(int stopLevel) {
        if (stopLevel >= 100) {
            return new int[]{1, 202};
        }
        for (int low = 0; low <= 256; low++) {
            if (stopLevel(low, 2 * low + 256) <= stopLevel) {
                return new int[]{low, 2 * low + 256};
            }
        }
        return new int[]{256, 768};
    }

    /**
     * Success chance (not burning) for a cooked item.
     *
     * @param stopBurn         the recipe's stop-burn level on a fire (used when the food has no table entry)
     * @param gauntletStopBurn stop-burn level with cooking gauntlets, or 0 if they don't help
     */
    public static double success(String cookedItem, int stopBurn, int gauntletStopBurn, int level, boolean range,
                                 boolean gauntlets) {
        Rates r = of(cookedItem);
        int low;
        int high;
        if (gauntlets && r != null && r.hasGauntlets()) {
            low = r.gauntletLow();
            high = r.gauntletHigh();
        } else if (gauntlets && gauntletStopBurn > 0) {
            int[] d = fromStopLevel(gauntletStopBurn);
            low = d[0];
            high = d[1];
        } else if (r != null) {
            low = range && r.hasRange() ? r.rangeLow() : r.fireLow();
            high = range && r.hasRange() ? r.rangeHigh() : r.fireHigh();
        } else {
            int[] d = fromStopLevel(stopBurn);
            low = d[0];
            high = d[1];
        }
        return Chance.lerp(level, low, high);
    }
}
