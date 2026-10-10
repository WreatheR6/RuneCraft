package com.runecraft.osrs.content.skilling;

import java.util.List;

/**
 * One weighted line of a skilling loot table: {@code item} x[min, max] with relative {@code weight}.
 * Exactly one line is picked per roll (see {@link #pick(List, int)}).
 */
public record Loot(String item, int min, int max, int weight) {
    public static Loot of(String item, int min, int max, int weight) {
        return new Loot(item, min, max, weight);
    }

    public static Loot of(String item, int weight) {
        return new Loot(item, 1, 1, weight);
    }

    public static int totalWeight(List<Loot> table) {
        int total = 0;
        for (Loot l : table) {
            total += l.weight;
        }
        return total;
    }

    /** Picks the line for {@code roll} in [0, totalWeight). */
    public static Loot pick(List<Loot> table, int roll) {
        int r = roll;
        for (Loot l : table) {
            if (r < l.weight) {
                return l;
            }
            r -= l.weight;
        }
        return table.get(table.size() - 1);
    }

    /** Amount for a uniform roll in [0, 1). */
    public int amount(double u) {
        return min + (int) Math.floor(u * (max - min + 1));
    }
}
