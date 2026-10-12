package com.runecraft.osrs;

import java.util.function.IntUnaryOperator;

/**
 * OSRS harvest lives (wiki "Harvest lives"): herb, allotment (and similar) patches start with 3
 * lives, +1 / +2 / +3 for compost / supercompost / ultracompost. Every harvest gives one produce and
 * uses a life unless it is "saved". The save chance out of 256 is
 * {@code floor((floor(lerp(low, high, level)) + 1) * (1 + itemBonus) * (1 + diaryBonus))}, where
 * {@code lerp} interpolates linearly between the crop's level-1 and level-99 values.
 * Item bonuses add up: magic secateurs +10%, Farming cape +5% (herbs), a growing attas plant +5%.
 */
public final class FarmingYield {
    public static final double MAGIC_SECATEURS = 0.10;
    public static final double FARMING_CAPE = 0.05;
    public static final double ATTAS = 0.05;

    private FarmingYield() {
    }

    /** Harvest lives a patch starts with for compost level 0..3. */
    public static int lives(int compost) {
        return 3 + Math.max(0, Math.min(3, compost));
    }

    /** Chance out of 256 to save a harvest life. */
    public static int saveChance(int low, int high, int level, double itemBonus, double diaryBonus) {
        int l = Math.max(1, Math.min(99, level));
        int lerp = (low * (99 - l) + high * (l - 1)) / 98;
        return Math.min(255, (int) Math.floor((lerp + 1) * (1 + itemBonus) * (1 + diaryBonus)));
    }

    /**
     * Rolls a whole harvest: produce picked until the lives run out. {@code random.applyAsInt(256)}
     * must return 0..255.
     */
    public static int harvest(int lives, int chance256, IntUnaryOperator random) {
        int yield = 0;
        int left = lives;
        int c = Math.min(255, chance256);
        while (left > 0 && yield < 1000) {
            yield++;
            if (random.applyAsInt(256) >= c) {
                left--;
            }
        }
        return yield;
    }

    /** Average produce from a harvest: lives / (1 - p). */
    public static double expected(int lives, int chance256) {
        return lives / (1.0 - Math.min(255, chance256) / 256.0);
    }

    /**
     * Random-tick odds that make a crop with {@code steps} visible growth steps take about
     * {@code growthMinutes} in total (one random tick hits a block every ~68.27 s on average).
     */
    public static int randomTickOdds(int growthMinutes, int steps) {
        double perStepSeconds = growthMinutes * 60.0 / Math.max(1, steps);
        return Math.max(1, (int) Math.round(perStepSeconds / 68.27));
    }
}
