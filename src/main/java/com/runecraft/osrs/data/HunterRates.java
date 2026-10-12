package com.runecraft.osrs.data;

import com.runecraft.osrs.Chance;

import java.util.Map;

/**
 * Hunter catch chances and trap limits.
 *
 * <p>Traps use the standard OSRS skilling success curve ({@link Chance#lerp}): a "low" value at level 1
 * and a "high" value at level 99, out of 256. The values below come from the OSRS Wiki's catch-chance
 * charts for each creature (grey chinchompa 6/268 is the wiki's own pair; the others are solved from the
 * chart's figures at the requirement and at 99). Creatures without published charts use
 * {@link #fallback(int)}, which matches the shape every published chart has: about 56% at the level
 * requirement, rising to 100% at 99.
 *
 * <p>Trap limit (OSRS): one trap below level 20, then one more at 20, 40, 60 and 80; one extra in
 * the Wilderness.
 */
public final class HunterRates {
    private HunterRates() {
    }

    /** {low, high} out of 256, keyed by creature display name (lower case). */
    private static final Map<String, int[]> RATES = Map.ofEntries(
            // Wiki: 39% at level 1, 164% (always) at 99.
            Map.entry("crimson swift", new int[]{99, 419}),
            // Wiki: 6/255 at 1 (145/255 at 53), 268/255 at 99.
            Map.entry("grey chinchompa", new int[]{6, 268}),
            // Wiki: 115/255 at 63, 228/255 at 99.
            Map.entry("red chinchompa", new int[]{-80, 228}),
            // Wiki: 148/255 at 73, 228/255 at 99.
            Map.entry("black chinchompa", new int[]{-74, 228}),
            // Wiki: 55% at 29, 100% from 66.
            Map.entry("swamp lizard", new int[]{53, 358}),
            // Wiki: 56% at 47, 100% from 88.
            Map.entry("orange salamander", new int[]{16, 285}),
            // Wiki: 56% at 59, 94% at 99.
            Map.entry("red salamander", new int[]{1, 240}),
            // Wiki: 56% at 67, 83% at 99.
            Map.entry("black salamander", new int[]{0, 212}),
            // Wiki chart: 66% at 79; the level-99 end is approx.
            Map.entry("tecu salamander", new int[]{-69, 230}));

    /** Traps a hunter may have laid at once (OSRS: 1 + 1 per 20 levels, max 5; +1 in the Wilderness). */
    public static int trapLimit(int hunterLevel, boolean wilderness) {
        int base = Math.min(5, 1 + Math.max(0, hunterLevel) / 20);
        return base + (wilderness ? 1 : 0);
    }

    /** {low, high} for a creature: its wiki chart values, or the fallback curve for its level. */
    public static int[] rates(String creature, int requiredLevel) {
        int[] r = RATES.get(creature.toLowerCase(java.util.Locale.ROOT));
        return r != null ? r : fallback(requiredLevel);
    }

    /** True when the creature's rates come from a wiki chart rather than the fallback curve. */
    public static boolean published(String creature) {
        return RATES.containsKey(creature.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Fallback curve (approx): 142/256 (~56%) at the requirement, 255/256 at 99, the shape of every
     * published Hunter chart.
     */
    public static int[] fallback(int requiredLevel) {
        int req = Math.max(1, Math.min(97, requiredLevel));
        int high = 255;
        int low = (int) Math.round((142.0 * 98 - high * (req - 1)) / (99 - req));
        return new int[]{low, high};
    }

    /** Chance (0-1) that a laid trap catches the creature, 0 below the requirement. */
    public static double catchChance(String creature, int requiredLevel, int hunterLevel) {
        if (hunterLevel < requiredLevel) {
            return 0.0;
        }
        int[] r = rates(creature, requiredLevel);
        return Chance.lerp(hunterLevel, r[0], r[1]);
    }

    /** Chance for a bird snare / box trap creature. */
    public static double catchChance(HunterCreature c, int hunterLevel) {
        return catchChance(c.displayName(), c.level(), hunterLevel);
    }
}
