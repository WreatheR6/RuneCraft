package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Guardians of the Rift energy, reward points and the Rewards Guardian, as on the OSRS Wiki
 * (Guardians of the Rift, Rewards Guardian, guardian stones).
 *
 * <p>Imbuing guardian essence at an elemental or catalytic altar gives one guardian stone of that
 * type per essence (combination runes give polyelemental stones). Each stone used on the Great
 * Guardian gives 2 energy of its type (3 for poly stones) and powers him; points and experience
 * depend on the essence crafted, not on the altar's tier. At the end of a closed rift, players
 * with at least 150 total energy get one point per 100 energy of each type, the remainder giving a
 * proportional chance of one more point. One search of the Rewards Guardian costs one elemental
 * and one catalytic point.
 */
public final class GuardiansRewards {
    private GuardiansRewards() {
    }

    public static final int STONES_PER_ESSENCE = 1;
    public static final int ENERGY_PER_STONE = 2;
    public static final int ENERGY_PER_POLY_STONE = 3;
    /** Building a barrier gives 2 energy of each type. */
    public static final int BARRIER_ENERGY = 2;
    /** Minimum combined energy for any reward. */
    public static final int MIN_ENERGY = 150;
    public static final int ENERGY_PER_POINT = 100;
    /** Energy of each type is capped per game (approx: sources disagree on per-type vs combined). */
    public static final int ENERGY_CAP = 1000;
    /** The Great Guardian needs 250 guardian stones per player in the temple to reach 100% power. */
    public static final int STONES_PER_PLAYER = 250;
    /** Runecraft XP per guardian stone used on the Great Guardian (OSRS Wiki). */
    public static final double XP_PER_STONE = 5.0;
    /** Bonus Runecraft XP per level for a closed rift with 150+ energy (approx). */
    public static final double COMPLETION_XP_PER_LEVEL = 45.0;

    /** Energy from powering the Great Guardian with stones. */
    public static int stoneEnergy(int stones, int polyStones) {
        return stones * ENERGY_PER_STONE + polyStones * ENERGY_PER_POLY_STONE;
    }

    /** Guardian power (percent) added by one stone with {@code players} in the temple. */
    public static double powerPerStone(int players) {
        return 100.0 / (STONES_PER_PLAYER * Math.max(1, players));
    }

    /** True when a player's energy earns a reward at all. */
    public static boolean qualifies(int elemental, int catalytic) {
        return elemental + catalytic >= MIN_ENERGY;
    }

    /** Guaranteed points for one energy type (energy / 100, energy capped). */
    public static int points(int energy) {
        return Math.min(ENERGY_CAP, Math.max(0, energy)) / ENERGY_PER_POINT;
    }

    /** Points for one energy type, the remainder giving a chance of one more point. */
    public static int points(int energy, double random) {
        int e = Math.min(ENERGY_CAP, Math.max(0, energy));
        int base = e / ENERGY_PER_POINT;
        return random < (e % ENERGY_PER_POINT) / (double) ENERGY_PER_POINT ? base + 1 : base;
    }

    /** Searches affordable with stored points (one elemental + one catalytic point each). */
    public static int searches(int elementalPoints, int catalyticPoints) {
        return Math.max(0, Math.min(elementalPoints, catalyticPoints));
    }

    public static double completionXp(int runecraft) {
        return runecraft * COMPLETION_XP_PER_LEVEL;
    }

    // ------------------------------------------------------------------ Rewards Guardian

    /** A main-table line: item, quantity, weight out of 140 (out of 125 once every essence pouch is owned). */
    public record Line(String item, int min, int max, int weight) {
    }

    /**
     * The main table. Rune weights (4 elemental/mind/body, 10 catalytic), the 400-500 elemental runes,
     * 250-400 mind, 80-150 body, 61-150 chaos and the pearls' 18/140 are wiki values; the other
     * catalytic quantities, the pouch split and the intricate pouch's share of the remaining 23 are approx.
     */
    public static final List<Line> MAIN = List.of(
            new Line("air_rune", 400, 500, 4), new Line("water_rune", 400, 500, 4), new Line("earth_rune", 400, 500, 4),
            new Line("fire_rune", 400, 500, 4), new Line("mind_rune", 250, 400, 4), new Line("body_rune", 80, 150, 4),
            new Line("chaos_rune", 61, 150, 10), new Line("cosmic_rune", 40, 120, 10), new Line("nature_rune", 25, 150, 10),
            new Line("law_rune", 6, 120, 10), new Line("death_rune", 6, 120, 10), new Line("blood_rune", 6, 120, 10),
            new Line("abyssal_pearls", 10, 20, 18), new Line("intricate_pouch", 1, 1, 23),
            new Line("medium_pouch", 1, 1, 5), new Line("large_pouch", 1, 1, 5), new Line("giant_pouch", 1, 1, 5));
    /** Essence pouches are skipped once owned (the table then totals 125). */
    public static final List<String> POUCHES = List.of("medium_pouch", "large_pouch", "giant_pouch");

    /** Rare rolls made on every search before the main table: item, 1 in N. */
    public static final Object[][] RARE = {
            {"abyssal_protector", 4000}, {"abyssal_needle", 300}, {"abyssal_lantern", 700},
            {"abyssal_green_dye", 1200}, {"abyssal_blue_dye", 1200}, {"abyssal_red_dye", 1200},
            {"catalytic_talisman", 200}, {"elemental_talisman", 200}};

    /** Total main-table weight given the pouches already owned. */
    public static int mainTotal(List<String> owned) {
        int total = 0;
        for (Line l : MAIN) {
            if (!owned.contains(l.item())) {
                total += l.weight();
            }
        }
        return total;
    }

    /** Chance (0-1) of a main-table line per search. */
    public static double chance(String item, List<String> owned) {
        for (Line l : MAIN) {
            if (l.item().equals(item)) {
                return owned.contains(item) ? 0 : l.weight() / (double) mainTotal(owned);
            }
        }
        return 0;
    }

    /**
     * One search: every rare line rolls first (the first hit is the reward); otherwise one main-table
     * line. Runes the player can't craft yet come in a smaller quantity (approx: halved).
     */
    public static MinigameReward search(int runecraft, List<String> owned, DoubleSupplier random) {
        for (Object[] r : RARE) {
            if (random.getAsDouble() < 1.0 / (Integer) r[1]) {
                return new MinigameReward((String) r[0], 1);
            }
        }
        int roll = (int) (random.getAsDouble() * mainTotal(owned));
        Line hit = null;
        for (Line l : MAIN) {
            if (owned.contains(l.item())) {
                continue;
            }
            roll -= l.weight();
            if (roll < 0) {
                hit = l;
                break;
            }
        }
        if (hit == null) {
            hit = MAIN.get(0);
        }
        int qty = hit.min() + (int) (random.getAsDouble() * (hit.max() - hit.min() + 1));
        Integer level = runeLevel(hit.item());
        if (level != null && runecraft < level) {
            qty = Math.max(1, qty / 2);
        }
        return new MinigameReward(hit.item(), qty);
    }

    /** Runecraft level to craft a rune in the temple, or null when the item isn't a temple rune. */
    public static Integer runeLevel(String item) {
        for (Object[] a : MinigameRules.Guardians.ALTARS) {
            if (a[1].equals(item)) {
                return (Integer) a[2];
            }
        }
        return null;
    }

    /** Contents of an intricate pouch (approx.: a bigger rune, pearl or essence roll). */
    public static List<MinigameReward> openIntricatePouch(DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        if (random.getAsDouble() < 1.0 / 200) {
            out.add(new MinigameReward("blood_essence", 1));
        }
        String[] runes = {"nature_rune", "law_rune", "death_rune", "blood_rune", "cosmic_rune", "chaos_rune"};
        String rune = runes[(int) (random.getAsDouble() * runes.length)];
        out.add(new MinigameReward(rune, 50 + (int) (random.getAsDouble() * 101)));
        out.add(new MinigameReward("abyssal_pearls", 5 + (int) (random.getAsDouble() * 11)));
        out.add(new MinigameReward("pure_essence", 50 + (int) (random.getAsDouble() * 101)));
        return out;
    }

    /** Every item a search or intricate pouch can give. */
    public static List<String> allItems() {
        List<String> ids = new ArrayList<>();
        for (Line l : MAIN) {
            ids.add(l.item());
        }
        for (Object[] r : RARE) {
            ids.add((String) r[0]);
        }
        ids.addAll(List.of("blood_essence", "pure_essence"));
        return ids;
    }
}
