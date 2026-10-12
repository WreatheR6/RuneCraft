package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Herb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Farming contracts from Guildmaster Jane in the Farming Guild (OSRS): she asks for one crop to be
 * grown and harvested (anywhere); handing it in pays a seed pack. Easy contracts need Farming 45,
 * medium 65 and hard 85. Easy contracts ask for allotments, flowers and potato cacti; medium for
 * herbs, bushes and watermelons; hard for trees, fruit trees, cacti, celastrus and redwood. The pack's
 * tier (1-5) is 1-3 / 2-4 / 3-5 for easy / medium / hard, higher for the harder crops of the tier.
 * Seed pack contents are approx. (the wiki's exact per-tier weights aren't reproduced).
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

    /** A contract crop: what counts as harvesting it ({@code cropId}), its name and level. */
    public record Contract(String cropId, String name, int level, Tier tier) {
    }

    public static final List<Contract> CONTRACTS = new ArrayList<>();

    static {
        for (Allotment a : Allotment.values()) {
            CONTRACTS.add(new Contract(a.produceItem(), a.displayName(), a.level(),
                    a == Allotment.WATERMELON ? Tier.MEDIUM : Tier.EASY));
        }
        for (Herb h : Herb.values()) {
            CONTRACTS.add(new Contract(h.cleanItem(), h.shortName(), h.farmingLevel(), Tier.MEDIUM));
        }
        for (FarmingData.Crop c : FarmingData.CROPS) {
            Tier t = switch (c.patch()) {
                case FLOWER -> Tier.EASY;
                case BUSH -> Tier.MEDIUM;
                case TREE, FRUIT_TREE, CELASTRUS, REDWOOD -> Tier.HARD;
                case CACTUS -> c.id().equals("potato_cactus") ? Tier.EASY : Tier.HARD;
                default -> null;
            };
            if (t != null) {
                CONTRACTS.add(new Contract(c.id(), c.name(), c.level(), t));
            }
        }
    }

    public static Contract byCrop(String cropId) {
        for (Contract c : CONTRACTS) {
            if (c.cropId().equals(cropId)) {
                return c;
            }
        }
        return null;
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

    /** Crops of a tier the player can plant. */
    public static List<Contract> options(Tier tier, int farming) {
        List<Contract> out = new ArrayList<>();
        for (Contract c : CONTRACTS) {
            if (c.tier() == tier && c.level() <= farming) {
                out.add(c);
            }
        }
        return out;
    }

    /** Picks a contract ({@code random.applyAsInt(n)} in 0..n-1), or null if none is available. */
    public static Contract assign(Tier tier, int farming, IntUnaryOperator random) {
        List<Contract> opts = options(tier, farming);
        return opts.isEmpty() ? null : opts.get(random.applyAsInt(opts.size()));
    }

    /** Seed pack tier (1-5) paid for a contract: the tier's base plus 0-2 by where the crop's level falls in the tier. */
    public static int packTier(Contract c) {
        int lo = Integer.MAX_VALUE;
        int hi = 0;
        for (Contract o : CONTRACTS) {
            if (o.tier() == c.tier()) {
                lo = Math.min(lo, o.level());
                hi = Math.max(hi, o.level());
            }
        }
        int step = hi <= lo ? 0 : Math.min(2, (c.level() - lo) * 3 / (hi - lo + 1));
        return c.tier().ordinal() + 1 + step;
    }

    // ------------------------------------------------------------------ seed packs (approx.)

    private static final List<List<Loot>> PACK_TABLES = List.of(
            // tier 1: allotments, flowers, hops and low herbs
            List.of(Loot.of("potato_seed", 8, 16, 10), Loot.of("onion_seed", 8, 16, 10), Loot.of("cabbage_seed", 8, 16, 10),
                    Loot.of("tomato_seed", 6, 12, 10), Loot.of("sweetcorn_seed", 6, 12, 8), Loot.of("strawberry_seed", 4, 8, 6),
                    Loot.of("marigold_seed", 4, 8, 8), Loot.of("rosemary_seed", 4, 8, 6), Loot.of("nasturtium_seed", 4, 8, 6),
                    Loot.of("woad_seed", 4, 8, 6), Loot.of("barley_seed", 8, 16, 6), Loot.of("hammerstone_seed", 8, 16, 6),
                    Loot.of("asgarnian_seed", 6, 12, 6), Loot.of("jute_seed", 6, 12, 6), Loot.of("yanillian_seed", 6, 12, 6),
                    Loot.of("krandorian_seed", 4, 8, 4), Loot.of("wildblood_seed", 4, 8, 4), Loot.of("guam_seed", 3, 6, 6),
                    Loot.of("marrentill_seed", 3, 6, 6), Loot.of("redberry_seed", 3, 6, 4), Loot.of("cadavaberry_seed", 3, 6, 4)),
            // tier 2
            List.of(Loot.of("watermelon_seed", 6, 12, 8), Loot.of("snape_grass_seed", 4, 8, 8), Loot.of("limpwurt_seed", 4, 8, 8),
                    Loot.of("tarromin_seed", 3, 6, 8), Loot.of("harralander_seed", 3, 6, 8), Loot.of("dwellberry_seed", 3, 6, 6),
                    Loot.of("jangerberry_seed", 3, 6, 6), Loot.of("acorn", 1, 3, 6), Loot.of("willow_seed", 1, 2, 5),
                    Loot.of("apple_seed", 1, 3, 5), Loot.of("banana_seed", 1, 3, 5), Loot.of("orange_seed", 1, 2, 4),
                    Loot.of("mushroom_spore", 2, 4, 4), Loot.of("grape_seed", 3, 6, 4)),
            // tier 3
            List.of(Loot.of("ranarr_seed", 1, 3, 8), Loot.of("toadflax_seed", 2, 4, 8), Loot.of("irit_seed", 2, 4, 8),
                    Loot.of("avantoe_seed", 2, 4, 8), Loot.of("kwuarm_seed", 1, 3, 6), Loot.of("white_lily_seed", 2, 5, 6),
                    Loot.of("whiteberry_seed", 2, 5, 6), Loot.of("poison_ivy_seed", 2, 5, 6), Loot.of("cactus_seed", 2, 5, 6),
                    Loot.of("potato_cactus_seed", 2, 5, 6), Loot.of("belladonna_seed", 1, 3, 4), Loot.of("maple_seed", 1, 2, 4),
                    Loot.of("curry_seed", 1, 3, 4), Loot.of("pineapple_seed", 1, 2, 4), Loot.of("teak_seed", 1, 3, 4)),
            // tier 4
            List.of(Loot.of("snapdragon_seed", 1, 2, 8), Loot.of("cadantine_seed", 1, 3, 8), Loot.of("lantadyme_seed", 1, 3, 8),
                    Loot.of("dwarf_weed_seed", 1, 3, 8), Loot.of("yew_seed", 1, 1, 5), Loot.of("papaya_seed", 1, 2, 6),
                    Loot.of("palm_seed", 1, 2, 5), Loot.of("calquat_tree_seed", 1, 2, 5), Loot.of("mahogany_seed", 1, 2, 5)),
            // tier 5
            List.of(Loot.of("torstol_seed", 1, 2, 10), Loot.of("magic_seed", 1, 1, 6), Loot.of("dragonfruit_seed", 1, 1, 6),
                    Loot.of("celastrus_seed", 1, 1, 6), Loot.of("redwood_tree_seed", 1, 1, 4), Loot.of("spirit_seed", 1, 1, 4)));

    /** The seed table of a pack tier (1-5). */
    public static List<Loot> table(int tier) {
        return PACK_TABLES.get(Math.max(1, Math.min(5, tier)) - 1);
    }

    /** Every table, lowest first. */
    public static List<List<Loot>> tables() {
        return PACK_TABLES;
    }

    /**
     * Which table each of the pack's rolls uses: one roll on its own tier's table plus four rolls on
     * the two tiers below it (approx. of the wiki's per-tier roll counts).
     */
    public static int[] rollTiers(int packTier, IntUnaryOperator random) {
        int t = Math.max(1, Math.min(5, packTier));
        int[] out = new int[5];
        out[0] = t;
        for (int i = 1; i < out.length; i++) {
            out[i] = Math.max(1, t - 1 - random.applyAsInt(2));
        }
        return out;
    }
}
