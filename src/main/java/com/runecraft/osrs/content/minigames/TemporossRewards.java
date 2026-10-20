package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Tempoross reward permits, the reward pool and Fishing experience, as on the OSRS Wiki
 * (Tempoross, Reward pool, Casket (Reward pool), Module:Tempoross reward pool and Module:Tempoross experience calculator).
 *
 * <p>Permits: one for the first 2,000 points, one more for every 700 points above that, and the
 * leftover points give a linearly scaling chance of one extra permit. A game scores at most
 * 32,000 points.
 *
 * <p>Each permit is one roll of the reward pool, fished with a small or big fishing net for 10 Fishing XP, at one
 * reward every 3 ticks (five with "Big-search"). The pool is one table out of 8,000: spirit flakes 1/4, casket 1/20,
 * soaked pages 149/8000, fish barrel and tackle box 1/400 each, big harpoonfish and Tome of Water 1/1600 each, tiny
 * tempor and dragon harpoon 1/8000 each; a fish barrel, tackle box or big harpoonfish already owned becomes 25 soaked
 * pages. About 45/80 is the fish sub-table, five fish chosen by the player's base Fishing level bracket (35-39, 40-45,
 * 46-49, 50-75, 76-78, 79-80, 81+) weighted 900/810/720/630/540 out of 6,400 (30-40, 20-30, 10-20, 7-12 and 5-10 of
 * them); the rest is the "other" supplies.
 */
public final class TemporossRewards {
    private TemporossRewards() {
    }

    public static final int MIN_POINTS = 2000;
    public static final int POINTS_PER_PERMIT = 700;
    public static final int MAX_POINTS = 32_000;
    public static final int MIN_FISHING = 35;
    /** Rewards the pool holds, Fishing XP per reward fished out, and the "Big-search" batch. */
    public static final int POOL_CAPACITY = 8000;
    public static final double POOL_XP = 10;
    public static final int BIG_SEARCH = 5;
    /** The nets that fish the reward pool (the Spirit Angler beside it hands out a small one). */
    public static final List<String> NETS = List.of("small_fishing_net", "big_fishing_net");

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
    /** The reward pool's own casket (opened for the casket table below), not the fishing casket. */
    public static final String CASKET = "reward_pool_casket";
    public static final List<Unique> UNIQUES = List.of(
            new Unique("spirit_flakes", 32, 64, 2000),
            new Unique(CASKET, 1, 1, 400),
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

    /** The fish sub-table's share of the pool: about 45/80 (wiki), 4,500 of the 8,000. */
    public static final int FISH_WEIGHT = 4500;

    /** One fish line of a level bracket: item, quantity range and weight out of {@link #UNIQUE_TOTAL}. */
    public record Fish(String item, int min, int max, int weight) {
    }

    /** A Fishing level bracket of the fish sub-table: from this base level on, these five fish. */
    public record FishBracket(int level, List<Fish> fish) {
    }

    /** Weights of the five fish of a bracket out of 8,000 (900/810/720/630/540 out of 6,400) and their quantities. */
    private static final int[] FISH_WEIGHTS = {1125, 1013, 900, 787, 675};
    private static final int[][] FISH_QTY = {{30, 40}, {20, 30}, {10, 20}, {7, 12}, {5, 10}};

    private static FishBracket bracket(int level, String... fish) {
        List<Fish> out = new ArrayList<>();
        for (int i = 0; i < fish.length; i++) {
            out.add(new Fish(fish[i], FISH_QTY[i][0], FISH_QTY[i][1], FISH_WEIGHTS[i]));
        }
        return new FishBracket(level, List.copyOf(out));
    }

    /** The wiki's brackets, highest first (Reward pool, "Fish"). */
    public static final List<FishBracket> FISH_BRACKETS = List.of(
            bracket(81, "raw_bass", "raw_swordfish", "raw_shark", "raw_sea_turtle", "raw_manta_ray"),
            bracket(79, "raw_lobster", "raw_bass", "raw_swordfish", "raw_shark", "raw_sea_turtle"),
            bracket(76, "raw_tuna", "raw_lobster", "raw_bass", "raw_swordfish", "raw_shark"),
            bracket(50, "raw_salmon", "raw_tuna", "raw_lobster", "raw_bass", "raw_swordfish"),
            bracket(46, "raw_pike", "raw_salmon", "raw_tuna", "raw_lobster", "raw_bass"),
            bracket(40, "raw_mackerel", "raw_pike", "raw_salmon", "raw_tuna", "raw_lobster"),
            bracket(35, "raw_herring", "raw_mackerel", "raw_pike", "raw_salmon", "raw_tuna"));

    /**
     * The "other" supplies, filling the rest of the 8,000 (899). Approx: the wiki's template isn't in the snapshot; items,
     * quantities and their relative weights follow oldschoolbot's copy (planks 350, oak planks 250, seaweed 200, steel nails,
     * feathers and fishing bait 100 each), scaled into the share the wiki's other rates leave.
     */
    public static final List<MinigameRules.Loot> OTHER = List.of(
            new MinigameRules.Loot("plank", 20, 30, 350), new MinigameRules.Loot("oak_plank", 15, 25, 250),
            new MinigameRules.Loot("seaweed", 20, 60, 200), new MinigameRules.Loot("steel_nails", 300, 500, 100),
            new MinigameRules.Loot("minecraft:feather", 800, 1600, 100), new MinigameRules.Loot("fishing_bait", 800, 1600, 100));

    /** The fish a Fishing level gets (empty below 35). */
    public static List<Fish> fishFor(int fishing) {
        for (FishBracket b : FISH_BRACKETS) {
            if (fishing >= b.level()) {
                return b.fish();
            }
        }
        return List.of();
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
        if (!fish.isEmpty() && r < FISH_WEIGHT) {
            for (Fish f : fish) {
                r -= f.weight();
                if (r < 0) {
                    return new MinigameReward(f.item(), f.min() + (int) (random.getAsDouble() * (f.max() - f.min() + 1)));
                }
            }
            Fish f = fish.get(fish.size() - 1);
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

    /** Chance (0-1) of a fish line per permit at a Fishing level. */
    public static double fishChance(int fishing, String item) {
        for (Fish f : fishFor(fishing)) {
            if (f.item().equals(item)) {
                return f.weight() / (double) UNIQUE_TOTAL;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ casket (reward pool)

    /** A casket line: item, quantity, weight out of {@link #CASKET_TOTAL} (OSRS Wiki "Casket (Reward pool)"). */
    public record CasketLine(String item, int min, int max, int weight) {
    }

    public static final int CASKET_TOTAL = 230;
    public static final List<CasketLine> CASKET_TABLE = List.of(
            new CasketLine("rune_med_helm", 1, 1, 9), new CasketLine("rune_full_helm", 1, 1, 9),
            new CasketLine("rune_chainbody", 1, 1, 9), new CasketLine("rune_platebody", 2, 3, 1),
            new CasketLine("rune_platelegs", 2, 3, 1), new CasketLine("rune_plateskirt", 2, 3, 1),
            new CasketLine("rune_sq_shield", 3, 5, 1), new CasketLine("rune_kiteshield", 3, 5, 1),
            new CasketLine("sapphire_ring", 10, 14, 9), new CasketLine("emerald_ring", 7, 11, 9),
            new CasketLine("ruby_ring", 6, 8, 9), new CasketLine("diamond_ring", 3, 4, 9),
            new CasketLine("sapphire_necklace", 10, 14, 9), new CasketLine("emerald_necklace", 7, 11, 9),
            new CasketLine("ruby_necklace", 6, 8, 9), new CasketLine("diamond_necklace", 3, 4, 9),
            new CasketLine("sapphire_bracelet", 10, 14, 9), new CasketLine("emerald_bracelet", 7, 11, 9),
            new CasketLine("ruby_bracelet", 6, 8, 9), new CasketLine("diamond_bracelet", 3, 4, 9),
            new CasketLine("sapphire_ring", 20, 28, 1), new CasketLine("emerald_ring", 14, 22, 1),
            new CasketLine("ruby_ring", 12, 16, 1), new CasketLine("diamond_ring", 6, 8, 1),
            new CasketLine("sapphire_necklace", 20, 28, 1), new CasketLine("emerald_necklace", 14, 22, 1),
            new CasketLine("ruby_necklace", 12, 16, 1), new CasketLine("diamond_necklace", 4, 8, 1),
            new CasketLine("sapphire_bracelet", 20, 28, 1), new CasketLine("emerald_bracelet", 14, 22, 1),
            new CasketLine("ruby_bracelet", 12, 16, 1), new CasketLine("diamond_bracelet", 3, 8, 1),
            new CasketLine("silver_bar", 50, 100, 9), new CasketLine("gold_bar", 50, 100, 9),
            new CasketLine("uncut_opal", 25, 50, 9), new CasketLine("uncut_jade", 25, 50, 9),
            new CasketLine("uncut_red_topaz", 10, 15, 9), new CasketLine("uncut_sapphire", 20, 30, 1),
            new CasketLine("uncut_emerald", 15, 25, 1), new CasketLine("uncut_ruby", 10, 20, 1),
            new CasketLine("uncut_diamond", 8, 13, 1), new CasketLine("uncut_dragonstone", 2, 3, 1),
            new CasketLine("coins", 4000, 6000, 9), new CasketLine("coins", 25_000, 50_000, 1),
            new CasketLine("loop_half_of_key", 1, 1, 9), new CasketLine("tooth_half_of_key", 1, 1, 9));
    /** The casket's tertiary clue scrolls: easy 5/100, medium 3/100, hard 2/100 (Mod Zuko). */
    public static final Object[][] CASKET_CLUES = {{"clue_scroll_easy", 5}, {"clue_scroll_medium", 3}, {"clue_scroll_hard", 2}};

    /** Opens a reward pool casket: one line of the table, plus a clue scroll on a 10/100 tertiary roll. */
    public static List<MinigameReward> openCasket(DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        int r = (int) (random.getAsDouble() * CASKET_TOTAL);
        CasketLine hit = CASKET_TABLE.get(CASKET_TABLE.size() - 1);
        for (CasketLine l : CASKET_TABLE) {
            r -= l.weight();
            if (r < 0) {
                hit = l;
                break;
            }
        }
        out.add(new MinigameReward(hit.item(), hit.min() + (int) (random.getAsDouble() * (hit.max() - hit.min() + 1))));
        int clue = (int) (random.getAsDouble() * 100);
        for (Object[] c : CASKET_CLUES) {
            clue -= (Integer) c[1];
            if (clue < 0) {
                out.add(new MinigameReward((String) c[0], 1));
                break;
            }
        }
        return out;
    }
}
