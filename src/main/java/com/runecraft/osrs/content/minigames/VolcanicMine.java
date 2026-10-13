package com.runecraft.osrs.content.minigames;

import java.util.function.DoubleSupplier;

/**
 * The Volcanic Mine on Fossil Island (OSRS Wiki: Volcanic Mine, Boulder (Volcanic Mine), Petrified Pete's
 * Ore Shop).
 *
 * <p>Wiki values: 50 Mining; Petrified Pete takes 30 numulite per entry (or 3,000 once for permanent
 * access); the volcano erupts ten minutes after the game starts. The boulder has five sizes (giant
 * attached, giant, large, medium, small) with about 40 hitpoints per player each (60 for the small one);
 * each successful hit takes one. A hit gives an ore fragment with chance 1/20, 1/12, 1/8, 1/5 and 1/3 by
 * size, otherwise "part of the boulder" (1 point, 5 XP). The fragment is rolled on the standard 1-99 curve
 * from runite (85) down to iron, the default: iron 15 points / 10 XP, silver 20/20, coal 25/30, gold
 * 34/40, mithril (55) 44/60, adamantite (70) 55/100, runite 70/150. Every successful hit also has a
 * 1/20 chance each of 1-4 calcite and pyrophosphite. Mining the small boulder away gives everyone 100
 * points. Blocking or unblocking a gas chamber ("capping") gives 50 points, at most 300 a game. Chamber
 * pressure should stay within 40-60% or the mine's stability falls; five minutes in, the mine shifts
 * and the pressures are randomised. In the last 30 seconds the boulder and chambers can't be used and
 * stability drops to 5% and stays there until the eruption. Rock golem: 1/(60,000 - 25 x level).
 *
 * <p>Approx: how fast pressure moves and how much stability a bad chamber costs are not published.
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
    /** The last 30 seconds: no mining or fixing; stability pinned at 5%. */
    public static final int FINAL_TICKS = 50;
    public static final int FINAL_STABILITY = 5;
    /** The mine shifts (chamber pressures randomised) five minutes in. */
    public static final int SHIFT_TICK = 500;
    public static final int BOULDER_STAGES = 5;
    public static final String[] BOULDER_NAMES = {"giant (attached)", "giant", "large", "medium", "small"};
    /** Fragment chance per successful hit by boulder size. */
    private static final int[] FRAGMENT_ONE_IN = {20, 12, 8, 5, 3};
    public static final int MISS_POINTS = 1;
    public static final double MISS_XP = 5.0;
    /** Points for every player when the small boulder is mined away. */
    public static final int COMPLETION_POINTS = 100;
    /** Points for blocking or unblocking a chamber, and the per-game limit. */
    public static final int CAP_POINTS = 50;
    public static final int CAP_LIMIT = 300;
    /** Calcite and pyrophosphite: 1/20 each per successful hit, 1-4 at a time. */
    public static final int BYPRODUCT_CHANCE = 20;
    public static final int PET_BASE = 60_000;
    public static final String[] VENTS = {"A", "B", "C"};
    public static final int START_STABILITY = 50;
    /** Chamber pressure that keeps the mine stable. */
    public static final int SAFE_LOW = 40;
    public static final int SAFE_HIGH = 60;

    /** Ore fragments, iron first: item stand-in name, Mining level, Ore Shop points, Mining XP, low/high of the cascade. */
    public static final String[] FRAGMENT_NAMES = {"iron ore", "silver ore", "coal", "gold ore", "mithril ore", "adamantite ore",
            "runite ore"};
    public static final int[] FRAGMENT_LEVELS = {50, 50, 50, 50, 55, 70, 85};
    public static final int[] FRAGMENT_POINTS = {15, 20, 25, 34, 44, 55, 70};
    public static final double[] FRAGMENT_XP = {10, 20, 30, 40, 60, 100, 150};
    private static final int[] FRAGMENT_LOW = {0, 0, -20, -40, -75, -90, -10};
    private static final int[] FRAGMENT_HIGH = {510, 185, 170, 160, 150, 90, 80};

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

    /** Chance (0-1) of an ore fragment per successful hit at a boulder stage (1-5). */
    public static double fragmentChance(int stage) {
        int s = Math.max(1, Math.min(BOULDER_STAGES, stage));
        return 1.0 / FRAGMENT_ONE_IN[s - 1];
    }

    /** Hitpoints of a boulder stage (1-5) with this many players. */
    public static int stageHp(int stage, int players) {
        return (stage >= BOULDER_STAGES ? 60 : 40) * Math.max(1, players);
    }

    /** Chance (0-1) that a swing hits the boulder at all (standard 1-99 curve, approx low/high). */
    public static double hitChance(int mining) {
        return com.runecraft.osrs.Chance.lerp(mining, 60, 190);
    }

    /** Result of one successful boulder hit: fragment tier (-1 = no fragment), points and XP. */
    public record Hit(int tier, int points, double xp) {
    }

    /** Fragment tier rolled once a fragment comes: runite down to silver in turn, iron when all fail. */
    public static int fragmentTier(int mining, DoubleSupplier random) {
        for (int i = FRAGMENT_LEVELS.length - 1; i > 0; i--) {
            if (mining >= FRAGMENT_LEVELS[i]
                    && random.getAsDouble() < com.runecraft.osrs.Chance.lerp(mining, FRAGMENT_LOW[i], FRAGMENT_HIGH[i])) {
                return i;
            }
        }
        return 0;
    }

    /** One successful hit at a stage. */
    public static Hit hit(int mining, int stage, DoubleSupplier random) {
        if (maxTier(mining) < 0 || random.getAsDouble() >= fragmentChance(stage)) {
            return new Hit(-1, MISS_POINTS, MISS_XP);
        }
        int tier = fragmentTier(mining, random);
        return new Hit(tier, FRAGMENT_POINTS[tier], FRAGMENT_XP[tier]);
    }

    /** Successful hits needed to move the boulder to the next stage. */
    public static int hitsPerStage(int players) {
        return stageHp(1, players);
    }

    /** Rock golem chance per ore fragment: 1 in (60,000 - 25 x level). */
    public static int petChance(int mining) {
        return PET_BASE - 25 * Math.max(1, Math.min(99, mining));
    }

    /** Capping points still available this game. */
    public static int capPoints(int earned) {
        return Math.max(0, Math.min(CAP_POINTS, CAP_LIMIT - earned));
    }

    /** Vent status change per update: a blocked chamber raises its vent, an open one lets it fall (approx). */
    public static int ventStep(int status, boolean blocked) {
        return Math.max(0, Math.min(100, status + (blocked ? 3 : -3)));
    }

    /**
     * Stability change per update from the three chamber pressures: +1 when all sit within 40-60%,
     * otherwise -1 for every 10% a chamber is outside that band (approx).
     */
    public static int stabilityDelta(int[] vents) {
        int loss = 0;
        for (int v : vents) {
            int out = v < SAFE_LOW ? SAFE_LOW - v : v > SAFE_HIGH ? v - SAFE_HIGH : 0;
            loss += (out + 9) / 10;
        }
        return loss == 0 ? 1 : -loss;
    }

    /** Stability after an update at {@code ticks}: pinned at 5% in the last 30 seconds. */
    public static int stability(int current, int[] vents, long ticks) {
        if (ticks >= GAME_TICKS - FINAL_TICKS) {
            return FINAL_STABILITY;
        }
        return Math.max(0, Math.min(100, current + stabilityDelta(vents)));
    }

    /** True while the boulder can be mined and the chambers fixed (not in the last 30 seconds). */
    public static boolean workable(long ticks) {
        return ticks < GAME_TICKS - FINAL_TICKS;
    }
}
