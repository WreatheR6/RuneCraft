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
        long atk = attackRoll < 0 ? Math.min(0, attackRoll + 2) : attackRoll;
        long def = defenceRoll < 0 ? Math.min(0, defenceRoll + 2) : defenceRoll;
        if (atk >= 0 && def >= 0) {
            return standard(atk, def);
        }
        if (atk >= 0) {
            // A negative defence roll (negative defence bonus below -64): the hit almost always lands.
            return 1.0 - 1.0 / (-def + 1.0) / (atk + 1.0);
        }
        return def >= 0 ? 0.0 : standard(-def, -atk);
    }

    private static double standard(long attackRoll, long defenceRoll) {
        if (attackRoll > defenceRoll) {
            return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        }
        return attackRoll / (2.0 * (defenceRoll + 1.0));
    }

    /**
     * Osmumten's fang (stab): the attack roll is made twice and either may beat the defence roll
     * (DPS calculator {@code getFangAccuracyRoll}).
     */
    public static double fangHitChance(long attackRoll, long defenceRoll) {
        double a = attackRoll < 0 ? Math.min(0, attackRoll + 2) : attackRoll;
        double d = defenceRoll < 0 ? Math.min(0, defenceRoll + 2) : defenceRoll;
        if (a >= 0 && d >= 0) {
            return a > d ? 1 - (d + 2) * (2 * d + 3) / (a + 1) / (a + 1) / 6
                    : a * (4 * a + 5) / 6 / (a + 1) / (d + 1);
        }
        if (a >= 0) {
            return 1 - 1 / (-d + 1) / (a + 1);
        }
        if (d >= 0) {
            return 0;
        }
        double ra = -d;
        double rd = -a;
        return 1 - (rd + 2) * (2 * rd + 3) / 6 / (rd + 1) / (ra + 1);
    }
}
