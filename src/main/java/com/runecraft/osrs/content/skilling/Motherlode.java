package com.runecraft.osrs.content.skilling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * The Motherlode Mine (OSRS Wiki: Motherlode Mine, Pay-dirt, Ore vein, Sack).
 *
 * <ul>
 *   <li>Mining a pay-dirt vein (30 Mining) gives 60 XP per pay-dirt. A vein depletes 23-27 seconds after
 *       its first pay-dirt (36-40 seconds on the upper level) and respawns after 100 ticks (lower level outer
 *       ring and upper level; 150 ticks for the lower level's inner ring). The upper level has 64 veins.</li>
 *   <li>Washed pay-dirt reaches the sack, which holds 108 (189 after the 200-nugget upgrade; since
 *       May 2024 it can no longer be overfilled). Washing gives bonus XP per ore: coal 15 (since the
 *       May 2024 rebalance), gold 15, mithril 30, adamantite 45, runite 75.</li>
 *   <li>Each pay-dirt rolls (when it is mined, not when it is cleaned), in order, a golden nugget (low/high 7/7 = 1/32), then runite (85, -20/5),
 *       adamantite (70, -90/50), mithril (55, -19/90) and gold (40, -40/126) on the standard 1-99 skilling
 *       curve (the wiki's cascading success chart); coal when every roll fails. At 99 this gives coal
 *       24.6%, nugget 3.13%, gold 24.22%, mithril 26.93%, adamantite 18.85% and runite 2.27% (the wiki's
 *       prose says 18.18% for adamantite, a typo: its row then sums to 99.33%).</li>
 *   <li>The upper level needs 57 Mining (not boostable; 72 before the 2022 change) and a one-off 100
 *       golden nuggets; its hopper costs another 50.</li>
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
    public static final int UPPER_LEVEL_MINING = 57;
    public static final int UPPER_LEVEL_NUGGETS = 100;
    public static final int UPPER_HOPPER_NUGGETS = 50;
    /** 1/32 per pay-dirt (low/high 7/7 on the 1-99 curve). */
    public static final int NUGGET_CHANCE = 32;
    /** Vein life after its first pay-dirt, in game ticks: 23-27 s lower, 36-40 s upper. */
    public static final int[] LOWER_VEIN_LIFE = {38, 45};
    public static final int[] UPPER_VEIN_LIFE = {60, 67};
    /** Depleted vein respawn in game ticks: upper level and the lower level's outer ring. */
    public static final int VEIN_RESPAWN = 100;
    /** Depleted vein respawn in game ticks for the lower level's inner ring (around the central pillar). */
    public static final int INNER_VEIN_RESPAWN = 150;
    /** Ore veins on the upper level (OSRS Wiki: "The upper level consists of 64 ore veins"). */
    public static final int UPPER_LEVEL_VEINS = 64;
    /** At most three golden nuggets reach the sack per hopper deposit. */
    public static final int MAX_NUGGETS_PER_DEPOSIT = 3;

    /**
     * Struts (OSRS Wiki): two struts hold up the water wheels; they work in rounds of 58 seconds (97 game ticks) and break
     * 2, 3 or 4 rounds after being repaired. While both are broken no pay-dirt is washed. Repairing one with a hammer gives
     * 1.5 x the current Smithing level in Smithing XP.
     */
    public static final int STRUTS = 2;
    public static final int STRUT_ROUND_TICKS = 97;

    /** The game tick a strut repaired (or first seen) at {@code now} breaks; {@code roll} is 0, 1 or 2. */
    public static long strutBreaksAt(long now, int roll) {
        return now + (long) (2 + Math.floorMod(roll, 3)) * STRUT_ROUND_TICKS;
    }

    /** Smithing XP (tenths) for repairing a strut at a Smithing level. */
    public static int strutRepairXpTenths(int smithingLevel) {
        return smithingLevel * 15;
    }

    /** An ore from pay-dirt: item, Mining level, bonus XP when washed, low/high of its roll. */
    public record Ore(String item, int level, double xp, int low, int high) {
        /** Chance of this roll succeeding at a Mining level (0 below the requirement). */
        public double chance(int miningLevel) {
            return miningLevel < level ? 0 : com.runecraft.osrs.Chance.lerp(miningLevel, low, high);
        }
    }

    public static final Ore NUGGET = new Ore("golden_nugget", 30, 0, 7, 7);
    /** Best tier first, as rolled. */
    public static final Ore[] ORES = {
            new Ore("runite_ore", 85, 75, -20, 5), new Ore("adamantite_ore", 70, 45, -90, 50),
            new Ore("mithril_ore", 55, 30, -19, 90), new Ore("gold_ore", 40, 15, -40, 126)};
    public static final Ore COAL = new Ore("coal", 30, 15, 0, 0);

    /** Ticks a vein lasts after its first pay-dirt, from a random value in [0, 1). */
    public static int veinLife(boolean upper, double random) {
        int[] r = upper ? UPPER_VEIN_LIFE : LOWER_VEIN_LIFE;
        return r[0] + (int) (random * (r[1] - r[0] + 1));
    }

    /** Nuggets that reach the sack from one deposit (the rest are lost). */
    public static int nuggetsKept(int washed) {
        return Math.min(MAX_NUGGETS_PER_DEPOSIT, washed);
    }

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
        return ore.chance(level);
    }

    /** Probability of each result per pay-dirt at a level: golden_nugget, the four ores and coal (sums to 1). */
    public static Map<String, Double> distribution(int level) {
        Map<String, Double> out = new LinkedHashMap<>();
        double nugget = NUGGET.chance(level);
        out.put(NUGGET.item(), nugget);
        double rem = 1.0 - nugget;
        for (Ore o : ORES) {
            double p = rem * conditional(o, level);
            out.put(o.item(), p);
            rem -= p;
        }
        out.put(COAL.item(), rem);
        return out;
    }

    /** Respawn ticks of a depleted vein. */
    public static int veinRespawn(boolean innerRing) {
        return innerRing ? INNER_VEIN_RESPAWN : VEIN_RESPAWN;
    }

    /**
     * What one pay-dirt washes into: "golden_nugget" or an ore id. Rolled when the pay-dirt is mined, at the
     * miner's (boosted) level then (OSRS Wiki, Mod Ash: "It'd stay as runite even if your level drops before
     * you clean it"); the hopper only washes it.
     */
    public static String wash(int level, DoubleSupplier random) {
        if (random.getAsDouble() < NUGGET.chance(level)) {
            return NUGGET.item();
        }
        for (Ore o : ORES) {
            if (random.getAsDouble() < conditional(o, level)) {
                return o.item();
            }
        }
        return COAL.item();
    }

    /** Results in a compact per-player queue: one character per mined pay-dirt (n, c, g, m, a, r). */
    private static final String CODES = "ncgmar";
    private static final String[] CODED = {"golden_nugget", "coal", "gold_ore", "mithril_ore", "adamantite_ore", "runite_ore"};

    /** The queue character for a roll result. */
    public static char code(String item) {
        for (int i = 0; i < CODED.length; i++) {
            if (CODED[i].equals(item)) {
                return CODES.charAt(i);
            }
        }
        return 'c';
    }

    /** The roll result a queue character stands for. */
    public static String decode(char code) {
        int i = CODES.indexOf(code);
        return i < 0 ? COAL.item() : CODED[i];
    }

    /** Bonus Mining XP for an item washed into the sack (0 for nuggets). */
    public static double collectXp(String item) {
        if (item.equals(COAL.item())) {
            return COAL.xp();
        }
        for (Ore o : ORES) {
            if (o.item().equals(item)) {
                return o.xp();
            }
        }
        return 0;
    }

    /** Average bonus XP per pay-dirt at a level (the wiki's 16.84 at 85 and 21.9 at 99 predate coal's 15 XP). */
    public static double averageCollectXp(int level) {
        double xp = 0;
        for (Map.Entry<String, Double> e : distribution(level).entrySet()) {
            xp += e.getValue() * collectXp(e.getKey());
        }
        return xp;
    }
}
