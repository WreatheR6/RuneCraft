package com.runecraft.osrs.content.minigames;

import java.util.Collection;
import java.util.List;

/**
 * The skilling outfits the minigames reward and the experience they add (OSRS Wiki, e.g. "Farmer's outfit", an
 * experience-boosting set for all of its skill's experience): the hat 0.4%, the top 0.8%, the legs 0.6% and the boots 0.2%,
 * 2.5% with the full set. A boost is worked out per action in tenths and rounded down. Experience the minigames hand out
 * themselves is boosted there, per action ({@link #boosted}: Tithe Farm's harvest and deposits but not the 75th fruit's
 * bonus, Mahogany Homes' contracts, the trawler's catch); every other XP drop in the skill gets {@link #bonusTenths} from
 * the general hook ({@code content.minigames.MinigameCombat}), which leaves the minigames' own boosted XP alone.
 */
public final class SkillingOutfits {
    private SkillingOutfits() {
    }

    /** Pieces in order: head, body, legs, feet. */
    public static final List<String> FARMER = List.of("farmers_strawhat", "farmers_jacket", "farmers_boro_trousers", "farmers_boots");
    public static final List<String> CARPENTER = List.of("carpenters_helmet", "carpenters_shirt", "carpenters_trousers",
            "carpenters_boots");
    public static final List<String> ANGLER = List.of("angler_hat", "angler_top", "angler_waders", "angler_boots");

    /** Bonus per piece in tenths of a percent (head, body, legs, feet), and for the whole set. */
    private static final int[] PIECE_PERMILLE = {4, 8, 6, 2};
    private static final int FULL_SET_PERMILLE = 25;

    /**
     * Experience boosted by an outfit multiplier, kept in tenths and rounded down as the game does per action (a 6 XP
     * golovanova harvest gives 6.1 in the full farmer's outfit, a 60 XP deposit 61.5; wiki "Golovanova seed").
     */
    public static double boosted(double xp, double multiplier) {
        return Math.floor(xp * multiplier * 10 + 1e-6) / 10.0;
    }

    /** The XP multiplier for the pieces of {@code outfit} among {@code worn} (1.025 for the whole set). */
    public static double multiplier(List<String> outfit, Collection<String> worn) {
        return 1.0 + permille(outfit, worn) / 1000.0;
    }

    /** The bonus in tenths of a percent for the pieces of {@code outfit} among {@code worn} (25 for the whole set). */
    public static int permille(List<String> outfit, Collection<String> worn) {
        int bonus = 0;
        int pieces = 0;
        for (int i = 0; i < outfit.size(); i++) {
            if (worn.contains(outfit.get(i))) {
                bonus += PIECE_PERMILLE[i];
                pieces++;
            }
        }
        return pieces == outfit.size() ? FULL_SET_PERMILLE : bonus;
    }

    /**
     * The extra tenths an XP drop of {@code tenths} gains from an outfit bonus of {@code permille}, rounded down in exact
     * integer tenths (a 6 XP drop gains 0.1 with the full set, a 230 XP one 5.7).
     */
    public static int bonusTenths(int tenths, int permille) {
        return (int) ((long) tenths * permille / 1000);
    }
}
