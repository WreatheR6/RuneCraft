package com.runecraft.osrs.content.skilling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * The Motherlode Mine (OSRS Wiki: Motherlode Mine, Pay-dirt, Sack).
 *
 * <ul>
 *   <li>Mining a pay-dirt vein (30 Mining) gives 60 XP per pay-dirt.</li>
 *   <li>Washed pay-dirt reaches the sack, which holds 108 (189 after the 200-nugget upgrade; since
 *       May 2024 it can no longer be overfilled). Collecting ore gives bonus XP per ore: coal 0,
 *       gold 15, mithril 30, adamantite 45, runite 75.</li>
 *   <li>Each pay-dirt is a golden nugget 1/32 of the time; otherwise the ore is rolled from the best
 *       tier the Mining level allows downwards (runite 85, adamantite 70, mithril 55, gold 40, else
 *       coal). At 99 the wiki gives coal 24.6%, nugget 3.13%, gold 24.22%, mithril 26.93%,
 *       adamantite 18.18% and runite 2.27%; below 99 each ore's chance scales down to a third of its
 *       level-99 value at its requirement (approx, matching the wiki's average bonus XP per pay-dirt
 *       of 16.84 at 85 and 21.9 at 99).</li>
 *   <li>The upper level needs 72 Mining (not boostable) and a one-off 100 golden nuggets.</li>
 * </ul>
 */
public final class Motherlode {
    private Motherlode() {
    }

    public static final int MINING_LEVEL = 30;
    public static final double PAY_DIRT_XP = 60.0;
    public static final int SACK_CAPACITY = 108;
    public static final int SACK_CAPACITY_UPGRADED = 189;
    public static final int SACK_UPGRADE_NUGGETS = 200;
    public static final int UPPER_LEVEL_MINING = 72;
    public static final int UPPER_LEVEL_NUGGETS = 100;
    /** 1/32 per pay-dirt (Pay-dirt page; the Motherlode Mine page still lists 7/255). */
    public static final int NUGGET_CHANCE = 32;

    /** An ore from pay-dirt: item, Mining level, bonus XP when collected, conditional chance at 99. */
    public record Ore(String item, int level, double xp, double chanceAt99) {
    }

    /** Best tier first. The conditional chances reproduce the wiki's level-99 percentages. */
    public static final Ore[] ORES = {
            new Ore("runite_ore", 85, 75, 0.02343), new Ore("adamantite_ore", 70, 45, 0.19217),
            new Ore("mithril_ore", 55, 30, 0.3524), new Ore("gold_ore", 40, 15, 0.4894)};
    public static final Ore COAL = new Ore("coal", 30, 0, 1.0);
    /** Fraction of the level-99 chance an ore has at its own level requirement (approx). */
    public static final double CHANCE_AT_REQUIREMENT = 1.0 / 3.0;

    public static int sackCapacity(boolean upgraded) {
        return upgraded ? SACK_CAPACITY_UPGRADED : SACK_CAPACITY;
    }

    /** Pay-dirt the hopper accepts: never more than the sack has room for. */
    public static int deposit(int carried, int inSack, boolean upgraded) {
        return Math.max(0, Math.min(carried, sackCapacity(upgraded) - inSack));
    }

    public static boolean canUseUpperLevel(int realMiningLevel, boolean unlocked) {
        return unlocked && realMiningLevel >= UPPER_LEVEL_MINING;
    }

    /** Conditional chance of an ore once the better tiers have missed. */
    public static double conditional(Ore ore, int level) {
        if (level < ore.level()) {
            return 0;
        }
        int l = Math.min(99, level);
        double f = CHANCE_AT_REQUIREMENT + (1 - CHANCE_AT_REQUIREMENT) * (l - ore.level()) / (99.0 - ore.level());
        return ore.chanceAt99() * f;
    }

    /** Probability of each result per pay-dirt at a level: golden_nugget, the four ores and coal (sums to 1). */
    public static Map<String, Double> distribution(int level) {
        Map<String, Double> out = new LinkedHashMap<>();
        double nugget = 1.0 / NUGGET_CHANCE;
        out.put("golden_nugget", nugget);
        double rem = 1.0 - nugget;
        for (Ore o : ORES) {
            double p = rem * conditional(o, level);
            out.put(o.item(), p);
            rem -= p;
        }
        out.put(COAL.item(), rem);
        return out;
    }

    /** What one pay-dirt washes into: "golden_nugget" or an ore id. */
    public static String wash(int level, DoubleSupplier random) {
        if (random.getAsDouble() < 1.0 / NUGGET_CHANCE) {
            return "golden_nugget";
        }
        for (Ore o : ORES) {
            if (random.getAsDouble() < conditional(o, level)) {
                return o.item();
            }
        }
        return COAL.item();
    }

    /** Bonus Mining XP for collecting an item from the sack (0 for coal and nuggets). */
    public static double collectXp(String item) {
        for (Ore o : ORES) {
            if (o.item().equals(item)) {
                return o.xp();
            }
        }
        return 0;
    }

    /** Average bonus XP per pay-dirt at a level (the wiki gives 16.84 at 85 and 21.9 at 99). */
    public static double averageCollectXp(int level) {
        double xp = 0;
        for (Map.Entry<String, Double> e : distribution(level).entrySet()) {
            xp += e.getValue() * collectXp(e.getKey());
        }
        return xp;
    }
}
