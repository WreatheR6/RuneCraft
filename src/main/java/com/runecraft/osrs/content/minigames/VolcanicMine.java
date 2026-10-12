package com.runecraft.osrs.content.minigames;

import java.util.function.DoubleSupplier;

/**
 * The Volcanic Mine on Fossil Island (OSRS Wiki: Volcanic Mine, Ore fragment).
 *
 * <p>Known values: 50 Mining; Petrified Pete takes 30 numulite per entry (or 3,000 once for
 * permanent access); the volcano erupts ten minutes after the game starts. Each successful hit on
 * the boulder gives Mining XP and Ore Shop points: an ore fragment (more likely the smaller the
 * boulder gets) worth 15 points / 10 XP at the level-50 tier up to 70 points / 150 XP at the
 * level-85 tier, otherwise 1 point / 5 XP. Rock golem: 1/(60,000 - 25 x level).
 *
 * <p>Approx: the intermediate fragment tiers are interpolated; the boulder has six stages; the vent
 * and stability model below is a simplification of the three gas chambers (blocked chambers push
 * their vent up, open ones let it fall; vents far from 50% erode stability).
 */
public final class VolcanicMine {
    private VolcanicMine() {
    }

    public static final int MINING_LEVEL = 50;
    public static final int ENTRY_NUMULITE = 30;
    public static final int PERMANENT_ACCESS_NUMULITE = 3000;
    /** Ten minutes of game ticks until the eruption. */
    public static final int GAME_TICKS = 1000;
    /** Warning before the eruption (one minute). */
    public static final int ERUPTION_WARNING = 100;
    public static final int BOULDER_STAGES = 6;
    /** Successful hits to push the boulder one stage, per player in the mine (approx). */
    public static final int HITS_PER_STAGE = 12;
    public static final int MISS_POINTS = 1;
    public static final double MISS_XP = 5.0;
    public static final int PET_BASE = 60_000;
    public static final String[] VENTS = {"A", "B", "C"};
    public static final int START_STABILITY = 50;

    /** Ore fragment tiers: Mining level, Ore Shop points, Mining XP (ends from the wiki, middle approx). */
    public static final int[] FRAGMENT_LEVELS = {50, 55, 60, 65, 70, 75, 80, 85};
    public static final int[] FRAGMENT_POINTS = {15, 23, 31, 39, 46, 54, 62, 70};
    public static final double[] FRAGMENT_XP = {10, 30, 50, 70, 90, 110, 130, 150};

    /** Highest fragment tier index a Mining level can get (-1 below 50). */
    public static int maxTier(int mining) {
        int best = -1;
        for (int i = 0; i < FRAGMENT_LEVELS.length; i++) {
            if (mining >= FRAGMENT_LEVELS[i]) {
                best = i;
            }
        }
        return best;
    }

    /** Chance (0-1) of an ore fragment per successful hit at a boulder stage (1-6): bigger as the boulder shrinks (approx). */
    public static double fragmentChance(int stage) {
        int s = Math.max(1, Math.min(BOULDER_STAGES, stage));
        return 0.15 + 0.10 * (s - 1);
    }

    /** Chance (0-1) that a swing hits the boulder at all (standard 1-99 curve, approx low/high). */
    public static double hitChance(int mining) {
        return com.runecraft.osrs.Chance.lerp(mining, 60, 190);
    }

    /** Result of one successful boulder hit: fragment tier (-1 = no fragment), points and XP. */
    public record Hit(int tier, int points, double xp) {
    }

    /** One successful hit at a stage. The fragment tier is random up to the best the level and stage allow (approx). */
    public static Hit hit(int mining, int stage, DoubleSupplier random) {
        int max = maxTier(mining);
        if (max < 0 || random.getAsDouble() >= fragmentChance(stage)) {
            return new Hit(-1, MISS_POINTS, MISS_XP);
        }
        int cap = Math.min(max, Math.max(0, stage + 1));
        int tier = (int) (random.getAsDouble() * (cap + 1));
        return new Hit(tier, FRAGMENT_POINTS[tier], FRAGMENT_XP[tier]);
    }

    /** Successful hits needed to move the boulder to the next stage. */
    public static int hitsPerStage(int players) {
        return HITS_PER_STAGE * Math.max(1, players);
    }

    /** Rock golem chance per ore fragment: 1 in (60,000 - 25 x level). */
    public static int petChance(int mining) {
        return PET_BASE - 25 * Math.max(1, Math.min(99, mining));
    }

    /** Vent status change per update: a blocked chamber raises its vent, an open one lets it fall (approx). */
    public static int ventStep(int status, boolean blocked) {
        return Math.max(0, Math.min(100, status + (blocked ? 3 : -3)));
    }

    /**
     * Stability change per update from the three vent statuses: +2 when all sit near 50%, falling by
     * one point for every 10% of average distance from 50% (approx).
     */
    public static int stabilityDelta(int[] vents) {
        double dev = 0;
        for (int v : vents) {
            dev += Math.abs(v - 50);
        }
        dev /= vents.length;
        return 2 - (int) Math.floor(dev / 10.0);
    }
}
