package com.runecraft.osrs.content.systems;

/**
 * Run energy maths in hundredths of a percent (0-10000), per game tick. Drain: 67 + 67 * weight / 64
 * (weight clamped 0-64), 70% less under a stamina potion. Restore: 8 + agility / 6 (wiki's post-2023
 * formula, approximately), +30% with the Agility cape.
 */
public final class RunEnergy {
    public static final int MAX = 10000;

    private RunEnergy() {
    }

    public static int drain(int weightKg, boolean stamina) {
        int w = Math.max(0, Math.min(64, weightKg));
        int d = 67 + (67 * w) / 64;
        return stamina ? (int) Math.ceil(d * 0.3) : d;
    }

    public static int regen(int agility, boolean agilityCape) {
        int r = 8 + Math.max(1, agility) / 6;
        return agilityCape ? (int) Math.round(r * 1.3) : r;
    }

    /** Game ticks to run from full to empty. */
    public static int ticksToEmpty(int weightKg, boolean stamina) {
        return (int) Math.ceil(MAX / (double) drain(weightKg, stamina));
    }
}
