package com.runecraft.osrs;

/** Skilling success-rate helpers used throughout OSRS. */
public final class Chance {
    private Chance() {
    }

    /**
     * The standard OSRS skilling success formula: linear interpolation between a "low" value at
     * level 1 and a "high" value at level 99, out of 256.
     * {@code (1 + floor(low * (99 - L) / 98 + high * (L - 1) / 98 + 0.5)) / 256}
     */
    public static double lerp(int level, double low, double high) {
        int l = Math.max(1, Math.min(99, level));
        double value = Math.floor(low * (99 - l) / 98.0 + high * (l - 1) / 98.0 + 0.5);
        return Math.max(0.0, Math.min(1.0, (1.0 + value) / 256.0));
    }

    /** Standard OSRS accuracy formula from an attack roll and defence roll. */
    public static double hitChance(long attackRoll, long defenceRoll) {
        if (attackRoll > defenceRoll) {
            return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        }
        return attackRoll / (2.0 * (defenceRoll + 1.0));
    }
}
