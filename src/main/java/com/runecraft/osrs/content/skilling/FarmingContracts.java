package com.runecraft.osrs.content.skilling;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Farming contracts from Guildmaster Jane in the Farming Guild (OSRS wiki "Farming contracts"): she
 * asks for one crop to be grown and harvested (anywhere); handing it in pays a seed pack. Easy
 * contracts need Farming 45, medium 65 and hard 85.
 *
 * <p>The wiki's contract table lists, per difficulty, every crop Jane may ask for with the Farming
 * level needed to be given it (the level of the Farming Guild patch it grows in: herbs and trees 65,
 * fruit trees 85, Redwood 90, otherwise the crop's own level or the tier's) and the seed pack tier it
 * pays. Every entry the player meets the level of is equally likely. Easy pays tiers 1-3, medium 2-4
 * and hard 3-5 (wiki counts: easy 11/12/8, medium 4/10/8, hard 3/8/7 entries).
 *
 * <p>Seed packs (wiki "Seed pack"): a tier-N pack gives 5 + N rolls, split into low, medium and high
 * rolls (tier 1: 1-3 medium, rest low; tier 2: 2-3 medium, 1/11 chance of a high roll; tier 3: 0-1
 * high, 2-4 medium; tier 4: 1-2 high, 3-5 medium; tier 5: 1-3 high, 4-6 medium), each on its table.
 */
public final class FarmingContracts {
    private FarmingContracts() {
    }

    public enum Tier {
        EASY(45), MEDIUM(65), HARD(85);

        public final int level;

        Tier(int level) {
            this.level = level;
        }
    }

    /**
     * A contract entry: what counts as harvesting it ({@code cropId}), its name, the Farming level
     * needed to be given it, its difficulty and the seed pack tier it pays.
     */
    public record Contract(String cropId, String name, int level, Tier tier, int packTier) {
    }

    public static final List<Contract> CONTRACTS = new ArrayList<>();

    private static void c(Tier t, int level, String cropId, String name, int packTier) {
        CONTRACTS.add(new Contract(cropId, name, level, t, packTier));
    }

    static {
        Tier e = Tier.EASY;
        c(e, 45, "potato", "Potato", 1);
        c(e, 45, "marigold", "Marigold", 1);
        c(e, 45, "onion", "Onion", 1);
        c(e, 45, "cabbage", "Cabbage", 1);
        c(e, 45, "redberry", "Redberry", 2);
        c(e, 45, "cadavaberry", "Cadavaberry", 2);
        c(e, 45, "rosemary", "Rosemary", 1);
        c(e, 45, "tomato", "Tomato", 1);
        c(e, 45, "sweetcorn", "Sweetcorn", 1);
        c(e, 45, "nasturtium", "Nasturtium", 1);
        c(e, 45, "woad", "Woad", 1);
        c(e, 45, "limpwurt", "Limpwurt", 1);
        c(e, 45, "dwellberry", "Dwellberry", 2);
        c(e, 48, "jangerberry", "Jangerberry", 2);
        c(e, 55, "cactus", "Cactus", 3);
        c(e, 65, "guam_leaf", "Guam", 2);
        c(e, 65, "marrentill", "Marrentill", 2);
        c(e, 65, "oak", "Oak tree", 3);
        c(e, 65, "tarromin", "Tarromin", 2);
        c(e, 65, "harralander", "Harralander", 2);
        c(e, 65, "willow", "Willow tree", 3);
        c(e, 65, "ranarr_weed", "Ranarr", 2);
        c(e, 65, "toadflax", "Toadflax", 2);
        c(e, 65, "irit_leaf", "Irit", 2);
        c(e, 65, "maple", "Maple tree", 3);
        c(e, 65, "avantoe", "Avantoe", 2);
        c(e, 85, "apple", "Apple tree", 3);
        c(e, 85, "banana", "Banana tree", 3);
        c(e, 85, "orange", "Orange tree", 3);
        c(e, 85, "curry", "Curry tree", 3);

        Tier m = Tier.MEDIUM;
        c(m, 65, "strawberry", "Strawberry", 2);
        c(m, 65, "irit_leaf", "Irit", 3);
        c(m, 65, "maple", "Maple tree", 4);
        c(m, 65, "watermelon", "Watermelon", 3);
        c(m, 65, "jangerberry", "Jangerberry", 3);
        c(m, 65, "avantoe", "Avantoe", 3);
        c(m, 65, "cactus", "Cactus", 4);
        c(m, 65, "kwuarm", "Kwuarm", 3);
        c(m, 65, "white_lily", "White lily", 2);
        c(m, 65, "whiteberry", "Whiteberry", 3);
        c(m, 65, "yew", "Yew tree", 4);
        c(m, 65, "snape_grass", "Snape grass", 2);
        c(m, 65, "snapdragon", "Snapdragon", 3);
        c(m, 65, "potato_cactus", "Potato cactus", 2);
        c(m, 67, "cadantine", "Cadantine", 3);
        c(m, 70, "poison_ivy", "Poison ivy", 3);
        c(m, 73, "lantadyme", "Lantadyme", 3);
        c(m, 75, "magic", "Magic tree", 4);
        c(m, 85, "curry", "Curry tree", 4);
        c(m, 85, "pineapple", "Pineapple plant", 4);
        c(m, 85, "papaya", "Papaya tree", 4);
        c(m, 85, "palm", "Palm tree", 4);

        Tier h = Tier.HARD;
        c(h, 85, "maple", "Maple tree", 5);
        c(h, 85, "watermelon", "Watermelon", 4);
        c(h, 85, "white_lily", "White lily", 4); // approx. tier (fits the wiki's 3/8/7 count)
        c(h, 85, "whiteberry", "Whiteberry", 3);
        c(h, 85, "yew", "Yew tree", 5);
        c(h, 85, "snape_grass", "Snape grass", 3);
        c(h, 85, "snapdragon", "Snapdragon", 4);
        c(h, 85, "potato_cactus", "Potato cactus", 3);
        c(h, 85, "cadantine", "Cadantine", 4);
        c(h, 85, "palm", "Palm tree", 5);
        c(h, 85, "poison_ivy", "Poison ivy", 4);
        c(h, 85, "lantadyme", "Lantadyme", 4);
        c(h, 85, "magic", "Magic tree", 5);
        c(h, 85, "dwarf_weed", "Dwarf weed", 4);
        c(h, 85, "dragonfruit", "Dragonfruit tree", 5);
        c(h, 85, "celastrus", "Celastrus tree", 5);
        c(h, 85, "torstol", "Torstol", 4);
        c(h, 90, "redwood", "Redwood tree", 5);
    }

    /** The first contract for a crop (any tier), or null. */
    public static Contract byCrop(String cropId) {
        return byCrop(cropId, null);
    }

    /** The contract for a crop at a tier (the first of any tier if {@code tier} is null or has none), or null. */
    public static Contract byCrop(String cropId, Tier tier) {
        Contract any = null;
        for (Contract c : CONTRACTS) {
            if (c.cropId().equals(cropId)) {
                if (tier == null || c.tier() == tier) {
                    return c;
                }
                if (any == null) {
                    any = c;
                }
            }
        }
        return any;
    }

    /** The hardest contract tier a player with this Farming level may take, or null below 45. */
    public static Tier bestTier(int farming) {
        Tier best = null;
        for (Tier t : Tier.values()) {
            if (farming >= t.level) {
                best = t;
            }
        }
        return best;
    }

    /** Contracts of a tier the player can be given. */
    public static List<Contract> options(Tier tier, int farming) {
        return options(tier, farming, null);
    }

    /** Contracts of a tier the player can be given, leaving out {@code except} (the crop just swapped away). */
    public static List<Contract> options(Tier tier, int farming, String except) {
        List<Contract> out = new ArrayList<>();
        for (Contract c : CONTRACTS) {
            if (c.tier() == tier && c.level() <= farming && !c.cropId().equals(except)) {
                out.add(c);
            }
        }
        return out;
    }

    /** Picks a contract ({@code random.applyAsInt(n)} in 0..n-1), or null if none is available. */
    public static Contract assign(Tier tier, int farming, IntUnaryOperator random) {
        return assign(tier, farming, null, random);
    }

    /** Picks a contract other than {@code except}, every option equally likely, or null. */
    public static Contract assign(Tier tier, int farming, String except, IntUnaryOperator random) {
        List<Contract> opts = options(tier, farming, except);
        return opts.isEmpty() ? null : opts.get(random.applyAsInt(opts.size()));
    }

    /** Seed pack tier (1-5) paid for a contract. */
    public static int packTier(Contract c) {
        return c.packTier();
    }

    // ------------------------------------------------------------------ seed packs (OSRS wiki)

    /** Index of the low, medium and high seed tables in {@link #tables()}. */
    public static final int LOW = 0;
    public static final int MEDIUM = 1;
    public static final int HIGH = 2;

    private static final List<List<Loot>> PACK_TABLES = List.of(
            // low
            List.of(Loot.of("potato_seed", 8, 12, 2), Loot.of("onion_seed", 8, 12, 2), Loot.of("cabbage_seed", 8, 12, 2),
                    Loot.of("tomato_seed", 8, 12, 2), Loot.of("sweetcorn_seed", 8, 12, 2), Loot.of("strawberry_seed", 8, 12, 2),
                    Loot.of("barley_seed", 8, 14, 2), Loot.of("hammerstone_seed", 6, 8, 2), Loot.of("asgarnian_seed", 6, 8, 2),
                    Loot.of("jute_seed", 8, 12, 2), Loot.of("yanillian_seed", 6, 8, 2), Loot.of("krandorian_seed", 6, 8, 2),
                    Loot.of("acorn", 3, 5, 2), Loot.of("apple_seed", 3, 5, 2), Loot.of("banana_seed", 3, 5, 2),
                    Loot.of("orange_seed", 3, 5, 2), Loot.of("curry_seed", 3, 5, 2), Loot.of("redberry_seed", 6, 8, 2),
                    Loot.of("cadavaberry_seed", 6, 8, 2), Loot.of("dwellberry_seed", 6, 8, 2), Loot.of("jangerberry_seed", 6, 8, 2),
                    Loot.of("marigold_seed", 8, 12, 2), Loot.of("rosemary_seed", 8, 12, 2), Loot.of("nasturtium_seed", 8, 12, 2),
                    Loot.of("woad_seed", 8, 12, 2), Loot.of("guam_seed", 3, 5, 2), Loot.of("marrentill_seed", 3, 5, 2),
                    Loot.of("tarromin_seed", 3, 5, 2), Loot.of("harralander_seed", 3, 5, 2), Loot.of("mushroom_spore", 4, 6, 1),
                    Loot.of("belladonna_seed", 4, 6, 1)),
            // medium
            List.of(Loot.of("irit_seed", 2, 6, 3), Loot.of("limpwurt_seed", 4, 8, 3), Loot.of("watermelon_seed", 8, 12, 2),
                    Loot.of("snape_grass_seed", 6, 8, 2), Loot.of("wildblood_seed", 8, 12, 2), Loot.of("whiteberry_seed", 6, 8, 2),
                    Loot.of("poison_ivy_seed", 6, 8, 2), Loot.of("cactus_seed", 2, 6, 2), Loot.of("potato_cactus_seed", 2, 6, 2),
                    Loot.of("willow_seed", 2, 4, 1), Loot.of("pineapple_seed", 3, 5, 1), Loot.of("toadflax_seed", 1, 3, 1),
                    Loot.of("avantoe_seed", 1, 3, 1), Loot.of("kwuarm_seed", 1, 3, 1), Loot.of("cadantine_seed", 1, 3, 1),
                    Loot.of("lantadyme_seed", 1, 3, 1), Loot.of("dwarf_weed_seed", 1, 3, 1), Loot.of("calquat_tree_seed", 3, 6, 1),
                    Loot.of("teak_seed", 1, 3, 1)),
            // high
            List.of(Loot.of("papaya_seed", 1, 3, 5), Loot.of("palm_seed", 1, 2, 5), Loot.of("hespori_seed", 1, 1, 5),
                    Loot.of("ranarr_seed", 1, 2, 4), Loot.of("snapdragon_seed", 1, 1, 4), Loot.of("maple_seed", 1, 2, 4),
                    Loot.of("mahogany_seed", 1, 2, 4), Loot.of("yew_seed", 1, 1, 3), Loot.of("dragonfruit_seed", 1, 1, 3),
                    Loot.of("celastrus_seed", 1, 1, 2), Loot.of("torstol_seed", 1, 1, 2), Loot.of("magic_seed", 1, 1, 1),
                    Loot.of("spirit_seed", 1, 1, 1), Loot.of("redwood_tree_seed", 1, 1, 1)));

    /** A seed table: {@link #LOW}, {@link #MEDIUM} or {@link #HIGH}. */
    public static List<Loot> table(int index) {
        return PACK_TABLES.get(Math.max(LOW, Math.min(HIGH, index)));
    }

    /** The low, medium and high tables. */
    public static List<List<Loot>> tables() {
        return PACK_TABLES;
    }

    /** Total rolls of a tier-N pack: 6, 7, 8, 9, 10. */
    public static int rollCount(int packTier) {
        return 5 + Math.max(1, Math.min(5, packTier));
    }

    /**
     * The table ({@link #LOW}/{@link #MEDIUM}/{@link #HIGH}) of each of a pack's rolls, high rolls
     * first. {@code random.applyAsInt(n)} must return 0..n-1.
     */
    public static int[] rolls(int packTier, IntUnaryOperator random) {
        int t = Math.max(1, Math.min(5, packTier));
        int high;
        int medium;
        switch (t) {
            case 1 -> {
                high = 0;
                medium = 1 + random.applyAsInt(3);
            }
            case 2 -> {
                high = random.applyAsInt(11) == 0 ? 1 : 0;
                medium = 2 + random.applyAsInt(2);
            }
            case 3 -> {
                high = random.applyAsInt(2);
                medium = 2 + random.applyAsInt(3);
            }
            case 4 -> {
                high = 1 + random.applyAsInt(2);
                medium = 3 + random.applyAsInt(3);
            }
            default -> {
                high = 1 + random.applyAsInt(3);
                medium = 4 + random.applyAsInt(3);
            }
        }
        int total = rollCount(t);
        int[] out = new int[total];
        for (int i = 0; i < total; i++) {
            out[i] = i < high ? HIGH : i < high + medium ? MEDIUM : LOW;
        }
        return out;
    }
}
