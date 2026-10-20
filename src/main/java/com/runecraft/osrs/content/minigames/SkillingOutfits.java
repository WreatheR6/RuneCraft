package com.runecraft.osrs.content.minigames;

import java.util.Collection;
import java.util.List;

/**
 * The skilling outfits the minigames reward and the experience they add (OSRS Wiki, e.g. "Farmer's outfit"): the hat
 * 0.4%, the top 0.8%, the legs 0.6% and the boots 0.2%, 2.5% with the full set. The minigames apply them to the experience
 * they hand out themselves (Tithe Farm's harvest and deposits, Mahogany Homes' contracts, the trawler's catch); general
 * skilling XP elsewhere doesn't use them yet.
 */
public final class SkillingOutfits {
    private SkillingOutfits() {
    }

    /** Pieces in order: head, body, legs, feet. */
    public static final List<String> FARMER = List.of("farmers_strawhat", "farmers_jacket", "farmers_boro_trousers", "farmers_boots");
    public static final List<String> CARPENTER = List.of("carpenters_helmet", "carpenters_shirt", "carpenters_trousers",
            "carpenters_boots");
    public static final List<String> ANGLER = List.of("angler_hat", "angler_top", "angler_waders", "angler_boots");

    private static final double[] PIECE = {0.004, 0.008, 0.006, 0.002};
    private static final double FULL_SET = 0.025;

    /**
     * Experience boosted by an outfit multiplier, kept in tenths and rounded down as the game does per action (a 6 XP
     * golovanova harvest gives 6.1 in the full farmer's outfit, a 60 XP deposit 61.5; wiki "Golovanova seed").
     */
    public static double boosted(double xp, double multiplier) {
        return Math.floor(xp * multiplier * 10 + 1e-6) / 10.0;
    }

    /** The XP multiplier for the pieces of {@code outfit} among {@code worn} (1.025 for the whole set). */
    public static double multiplier(List<String> outfit, Collection<String> worn) {
        double bonus = 0;
        int pieces = 0;
        for (int i = 0; i < outfit.size(); i++) {
            if (worn.contains(outfit.get(i))) {
                bonus += PIECE[i];
                pieces++;
            }
        }
        return 1.0 + (pieces == outfit.size() ? FULL_SET : bonus);
    }
}
