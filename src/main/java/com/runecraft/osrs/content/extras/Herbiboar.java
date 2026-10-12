package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.Chance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Herbiboar on Fossil Island (OSRS Wiki: Herbiboar and Module:Herbiboar).
 *
 * <p>Needs 80 Hunter and 31 Herblore. Each trail object searched gives 50 Hunter XP (about 2.56 per
 * hunt) and may turn up a fossil (small 1/50, medium 1/100, large 1/125, rare 1/500). Stunning and
 * harvesting the herbiboar gives 30 x min(level, 94) - 450 Hunter XP, plus 15 + 19 per level above
 * 95 from level 95. It drops 2 herbs (3 with magic secateurs); each herb is rolled in turn from
 * ranarr down to guam with the standard low/high success curve on the Herblore level. Herbi is 1/6500.
 */
public final class Herbiboar {
    private Herbiboar() {
    }

    public static final int HUNTER_LEVEL = 80;
    public static final int HERBLORE_LEVEL = 31;
    public static final double SEARCH_XP = 50.0;
    public static final double AVERAGE_SEARCHES = 2.56;
    public static final int PET_CHANCE = 6500;
    public static final int HERBS = 2;
    public static final int HERBS_WITH_SECATEURS = 3;

    /** A herb line: grimy herb, low/high out of 256 at Herblore 1/99. */
    public record HerbLine(String item, int low, int high) {
    }

    /** Rolled in this order; guam leaf always succeeds. */
    public static final List<HerbLine> HERB_ORDER = List.of(
            new HerbLine("grimy_ranarr_weed", -10, 20), new HerbLine("grimy_torstol", -70, 20),
            new HerbLine("grimy_snapdragon", -60, 20), new HerbLine("grimy_dwarf_weed", -50, 30),
            new HerbLine("grimy_lantadyme", -30, 40), new HerbLine("grimy_cadantine", -10, 50),
            new HerbLine("grimy_kwuarm", 10, 60), new HerbLine("grimy_avantoe", 20, 60),
            new HerbLine("grimy_irit_leaf", 30, 70), new HerbLine("grimy_tarromin", 70, -20),
            new HerbLine("grimy_harralander", 100, -30), new HerbLine("grimy_marrentill", 170, -40),
            new HerbLine("grimy_guam_leaf", 1000, 1000));

    /** Fossils from trail searches: item, 1 in N per search. */
    public static final Object[][] FOSSILS = {
            {"unidentified_rare_fossil", 500}, {"unidentified_large_fossil", 125},
            {"unidentified_medium_fossil", 100}, {"unidentified_small_fossil", 50}};

    /** Hunter XP for stunning and harvesting the herbiboar (searches not included). */
    public static double harvestXp(int hunter) {
        int l = Math.max(1, Math.min(99, hunter));
        double xp = 30.0 * Math.min(l, 94) - 450;
        if (l > 94) {
            xp += 15 + 19 * (l - 95);
        }
        return xp;
    }

    /** Average Hunter XP per herbiboar including the trail searches. */
    public static double averageXp(int hunter) {
        return harvestXp(hunter) + AVERAGE_SEARCHES * SEARCH_XP;
    }

    /** Chance (0-1) of each herb per roll at a Herblore level (sums to 1). */
    public static double[] herbChances(int herblore) {
        double[] out = new double[HERB_ORDER.size()];
        double rem = 1.0;
        for (int i = 0; i < out.length; i++) {
            HerbLine h = HERB_ORDER.get(i);
            double p = rem * Chance.lerp(herblore, h.low(), h.high());
            out[i] = p;
            rem -= p;
        }
        return out;
    }

    public static String rollHerb(int herblore, DoubleSupplier random) {
        for (HerbLine h : HERB_ORDER) {
            if (random.getAsDouble() < Chance.lerp(herblore, h.low(), h.high())) {
                return h.item();
            }
        }
        return HERB_ORDER.get(HERB_ORDER.size() - 1).item();
    }

    /** Herbs from one harvest. */
    public static List<String> harvest(int herblore, boolean magicSecateurs, DoubleSupplier random) {
        List<String> out = new ArrayList<>();
        int n = magicSecateurs ? HERBS_WITH_SECATEURS : HERBS;
        for (int i = 0; i < n; i++) {
            out.add(rollHerb(herblore, random));
        }
        return out;
    }

    /** Trail objects searched on one hunt: 2 or 3, averaging 2.56 (approx. of the trail lengths). */
    public static int searches(double random) {
        return random < AVERAGE_SEARCHES - 2 ? 3 : 2;
    }

    /** A fossil found on one trail search, or null. */
    public static String searchFossil(DoubleSupplier random) {
        for (Object[] f : FOSSILS) {
            if (random.getAsDouble() < 1.0 / (Integer) f[1]) {
                return (String) f[0];
            }
        }
        return null;
    }
}
