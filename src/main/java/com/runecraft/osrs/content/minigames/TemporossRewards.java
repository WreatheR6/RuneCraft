package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Tempoross reward permits, the reward pool and Fishing experience, as on the OSRS Wiki
 * (Tempoross, Reward pool, Module:Tempoross reward pool and Module:Tempoross experience calculator).
 *
 * <p>Permits: one for the first 2,000 points, one more for every 700 points above that, and the
 * leftover points give a linearly scaling chance of one extra permit. A game scores at most
 * 32,000 points.
 *
 * <p>Each permit is one roll of the reward pool. The unique lines are rolled out of 8,000: spirit
 * flakes 1/4, casket 1/20, soaked pages 149/8000, fish barrel and tackle box 1/400 each, big
 * harpoonfish and Tome of Water 1/1600 each, tiny tempor and dragon harpoon 1/8000 each. A fish
 * barrel, tackle box or big harpoonfish already owned becomes 25 soaked pages. Everything else is
 * raw fish (depending on Fishing level) or the "other" supplies.
 */
public final class TemporossRewards {
    private TemporossRewards() {
    }

    public static final int MIN_POINTS = 2000;
    public static final int POINTS_PER_PERMIT = 700;
    public static final int MAX_POINTS = 32_000;
    public static final int MIN_FISHING = 35;

    /** Guaranteed permits for a score. */
    public static int permits(int points) {
        int p = Math.min(MAX_POINTS, points);
        return p < MIN_POINTS ? 0 : 1 + (p - MIN_POINTS) / POINTS_PER_PERMIT;
    }

    /** Permits including the chance of one extra permit for the leftover points. */
    public static int permits(int points, double random) {
        int base = permits(points);
        if (base == 0) {
            return 0;
        }
        int p = Math.min(MAX_POINTS, points);
        double extra = ((p - MIN_POINTS) % POINTS_PER_PERMIT) / (double) POINTS_PER_PERMIT;
        return random < extra ? base + 1 : base;
    }

    // ------------------------------------------------------------------ experience

    /**
     * The level modifier behind all Tempoross Fishing XP: 450 at level 35 rising to 1000 at 99, but
     * 890 from level 70 (floor(base + (1000 - base) * (L - baseLevel) / (99 - baseLevel))).
     */
    public static int xpModifier(int fishing) {
        int l = Math.max(MIN_FISHING, Math.min(99, fishing));
        int base = l >= 70 ? 890 : 450;
        int baseLevel = l >= 70 ? 70 : 35;
        return (int) Math.floor(base + (1000.0 - base) * (l - baseLevel) / (99.0 - baseLevel));
    }

    /** Fishing XP per harpoonfish caught (modifier / 100: 4.5 at 35, 10 at 99). */
    public static double catchXp(int fishing) {
        return xpModifier(fishing) / 100.0;
    }

    /** Fishing XP per harpoonfish loaded into an ammunition crate (modifier / 10: 45 at 35, 100 at 99). */
    public static double cannonXp(int fishing) {
        return xpModifier(fishing) / 10.0;
    }

    /** Fishing XP per successful harpoon strike on the spirit pool (modifier * 11 / 100). */
    public static double spiritPoolXp(int fishing) {
        return xpModifier(fishing) * 11 / 100.0;
    }

    // ------------------------------------------------------------------ reward pool

    /** A unique line: item, quantity, weight out of {@link #UNIQUE_TOTAL}. */
    public record Unique(String item, int min, int max, int weight) {
    }

    public static final int UNIQUE_TOTAL = 8000;
    public static final List<Unique> UNIQUES = List.of(
            new Unique("spirit_flakes", 32, 64, 2000),
            new Unique("casket", 1, 1, 400),
            new Unique("soaked_page", 5, 9, 149),
            new Unique("fish_barrel", 1, 1, 20),
            new Unique("tackle_box", 1, 1, 20),
            new Unique("big_harpoonfish", 1, 1, 5),
            new Unique("tome_of_water", 1, 1, 5),
            new Unique("tiny_tempor", 1, 1, 1),
            new Unique("dragon_harpoon", 1, 1, 1));
    /** Uniques replaced by 25 soaked pages once owned. */
    public static final List<String> REPLACED_WHEN_OWNED = List.of("fish_barrel", "tackle_box", "big_harpoonfish");
    public static final int REPLACEMENT_PAGES = 25;

    /** Raw fish of the reward pool: item, Fishing level from which it is offered, quantity. */
    public record Fish(String item, int level, int min, int max) {
    }

    /** The wiki splits the fish table at 35, 40, 46, 50, 76, 79 and 81 (each bracket adds a better fish). Quantities approx. */
    public static final List<Fish> FISH = List.of(
            new Fish("raw_tuna", 35, 15, 25), new Fish("raw_lobster", 40, 12, 20), new Fish("raw_bass", 46, 12, 20),
            new Fish("raw_swordfish", 50, 10, 18), new Fish("raw_shark", 76, 8, 15), new Fish("raw_sea_turtle", 79, 6, 12),
            new Fish("raw_manta_ray", 81, 5, 10));

    /** The "other" supplies (approx. quantities/weights). */
    public static final List<MinigameRules.Loot> OTHER = List.of(
            new MinigameRules.Loot("oak_plank", 20, 30, 6), new MinigameRules.Loot("teak_plank", 15, 25, 5),
            new MinigameRules.Loot("mahogany_plank", 10, 20, 3), new MinigameRules.Loot("seaweed_spore", 2, 5, 4),
            new MinigameRules.Loot("uncut_sapphire", 2, 4, 3), new MinigameRules.Loot("uncut_emerald", 2, 3, 3),
            new MinigameRules.Loot("uncut_ruby", 1, 2, 2), new MinigameRules.Loot("uncut_diamond", 1, 1, 1),
            new MinigameRules.Loot("harpoon", 1, 1, 1));
    /** Share of the non-unique rolls that go to the fish table (approx.). */
    public static final double FISH_SHARE = 0.5;

    /** The fish offered at a Fishing level (empty below 35). */
    public static List<Fish> fishFor(int fishing) {
        List<Fish> out = new ArrayList<>();
        for (Fish f : FISH) {
            if (fishing >= f.level()) {
                out.add(f);
            }
        }
        return out;
    }

    /** One reward-pool roll. {@code owned} lists the replaceable uniques the player already has. */
    public static MinigameReward roll(int fishing, List<String> owned, DoubleSupplier random) {
        int r = (int) (random.getAsDouble() * UNIQUE_TOTAL);
        for (Unique u : UNIQUES) {
            r -= u.weight();
            if (r < 0) {
                if (REPLACED_WHEN_OWNED.contains(u.item()) && owned.contains(u.item())) {
                    return new MinigameReward("soaked_page", REPLACEMENT_PAGES);
                }
                return new MinigameReward(u.item(), u.min() + (int) (random.getAsDouble() * (u.max() - u.min() + 1)));
            }
        }
        List<Fish> fish = fishFor(fishing);
        if (!fish.isEmpty() && random.getAsDouble() < FISH_SHARE) {
            Fish f = fish.get((int) (random.getAsDouble() * fish.size()));
            return new MinigameReward(f.item(), f.min() + (int) (random.getAsDouble() * (f.max() - f.min() + 1)));
        }
        MinigameRules.Loot l = MinigameRules.pick(OTHER, random.getAsDouble());
        return new MinigameReward(l.item(), l.min() + (int) (random.getAsDouble() * (l.max() - l.min() + 1)));
    }

    /** Chance (0-1) of a unique line per permit. */
    public static double uniqueChance(String item) {
        for (Unique u : UNIQUES) {
            if (u.item().equals(item)) {
                return u.weight() / (double) UNIQUE_TOTAL;
            }
        }
        return 0;
    }
}
