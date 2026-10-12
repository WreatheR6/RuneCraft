package com.runecraft.osrs.data;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The shared OSRS drop tables that monster drops roll into: the gem drop table ({@code #gems}),
 * the rare drop table ({@code #rare}) and its mega-rare sub-table (weights out of 128 as on the OSRS
 * wiki; slots that are not listed are "nothing"), the herb drop table ({@code #herbs}) and the seed drop
 * tables ({@code #allotment_seeds}, {@code #rare_seeds}, {@code #uncommon_seeds}, {@code #tree_herb_seeds};
 * {@code #seeds} is the generic herb-seed table = the rare seed table). Each table has its own
 * {@link #denominator(String)}. The table structure is exact, a few weights are approx where the wiki
 * snapshots disagree.
 */
public final class DropTables {
    private DropTables() {
    }

    /** One slot: {@code item} x[min,max] with {@code weight}/128. Items starting with '#' are sub-tables. */
    public record Entry(String item, int min, int max, int weight) {
    }

    /** A rolled item and amount. */
    public record Result(String item, int count) {
    }

    public static final int DENOMINATOR = 128;

    private static Entry e(String item, int count, int weight) {
        return new Entry(item, count, count, weight);
    }

    /** Gem drop table: about half the rolls give nothing (60/128 here, approx). */
    public static final List<Entry> GEM = List.of(
            e("uncut_sapphire", 1, 32),
            e("uncut_emerald", 1, 16),
            e("uncut_ruby", 1, 8),
            e("uncut_diamond", 1, 2),
            e("chaos_talisman", 1, 3),
            e("nature_talisman", 1, 3),
            e("rune_javelin", 5, 1),
            e("loop_half_of_key", 1, 1),
            e("tooth_half_of_key", 1, 1),
            e("#rare", 1, 1));

    /** Rare drop table. */
    public static final List<Entry> RARE = List.of(
            // Runes and ammunition
            e("nature_rune", 67, 3),
            e("law_rune", 45, 2),
            e("death_rune", 45, 2),
            e("steel_arrow", 150, 2),
            e("rune_arrow", 42, 2),
            // Weapons and armour
            e("rune_2h_sword", 1, 3),
            e("rune_battleaxe", 1, 2),
            e("rune_sq_shield", 1, 2),
            e("dragon_med_helm", 1, 1),
            e("rune_kiteshield", 1, 1),
            // Other
            e("coins", 3000, 21),
            e("loop_half_of_key", 1, 20),
            e("tooth_half_of_key", 1, 20),
            e("runite_bar", 1, 5),
            e("dragonstone", 1, 2),
            e("silver_ore", 100, 2),
            // Sub-tables
            e("#gems", 1, 20),
            e("#mega", 1, 15));

    /** Mega-rare table, reached from the rare drop table: mostly nothing. */
    public static final List<Entry> MEGA = List.of(
            e("rune_spear", 1, 8),
            e("shield_left_half", 1, 4),
            e("dragon_spear", 1, 3));

    /** Herb drop table (OSRS wiki, out of 128): always gives one grimy herb. */
    public static final List<Entry> HERB = List.of(
            e("grimy_guam_leaf", 1, 32),
            e("grimy_marrentill", 1, 24),
            e("grimy_tarromin", 1, 18),
            e("grimy_harralander", 1, 14),
            e("grimy_ranarr_weed", 1, 11),
            e("grimy_irit_leaf", 1, 8),
            e("grimy_avantoe", 1, 6),
            e("grimy_kwuarm", 1, 5),
            e("grimy_cadantine", 1, 4),
            e("grimy_lantadyme", 1, 3),
            e("grimy_dwarf_weed", 1, 3));

    /** Allotment seed drop table (OSRS wiki, out of 1008). */
    public static final List<Entry> ALLOTMENT_SEEDS = List.of(
            e("potato_seed", 4, 368),
            e("onion_seed", 4, 276),
            e("cabbage_seed", 4, 184),
            e("tomato_seed", 3, 92),
            e("sweetcorn_seed", 3, 46),
            e("strawberry_seed", 2, 23),
            e("watermelon_seed", 2, 11),
            e("snape_grass_seed", 2, 8));

    /** Rare seed drop table (weights derived from the wiki's per-monster rates, out of 1002). */
    public static final List<Entry> RARE_SEEDS = List.of(
            e("limpwurt_seed", 1, 131),
            e("strawberry_seed", 1, 125),
            e("marrentill_seed", 1, 120),
            e("jangerberry_seed", 1, 88),
            e("tarromin_seed", 1, 81),
            e("wildblood_seed", 1, 79),
            e("watermelon_seed", 1, 60),
            e("harralander_seed", 1, 54),
            e("snape_grass_seed", 1, 38),
            e("ranarr_seed", 1, 37),
            e("whiteberry_seed", 1, 33),
            e("mushroom_spore", 1, 28),
            e("toadflax_seed", 1, 26),
            e("belladonna_seed", 1, 17),
            e("irit_seed", 1, 17),
            e("poison_ivy_seed", 1, 12),
            e("avantoe_seed", 1, 12),
            e("cactus_seed", 1, 12),
            e("kwuarm_seed", 1, 9),
            e("potato_cactus_seed", 1, 8),
            e("snapdragon_seed", 1, 5),
            e("cadantine_seed", 1, 4),
            e("lantadyme_seed", 1, 3),
            e("dwarf_weed_seed", 1, 2),
            e("torstol_seed", 1, 1));

    /** Uncommon seed drop table (weights derived from the wiki's per-monster rates, out of 999). */
    public static final List<Entry> UNCOMMON_SEEDS = List.of(
            e("toadflax_seed", 1, 198),
            e("irit_seed", 1, 136),
            e("belladonna_seed", 1, 131),
            e("poison_ivy_seed", 1, 95),
            e("avantoe_seed", 1, 93),
            e("cactus_seed", 1, 88),
            e("potato_cactus_seed", 1, 64),
            e("kwuarm_seed", 1, 63),
            e("snapdragon_seed", 1, 42),
            e("cadantine_seed", 1, 29),
            e("lantadyme_seed", 1, 21),
            e("snape_grass_seed", 3, 18),
            e("dwarf_weed_seed", 1, 13),
            e("torstol_seed", 1, 8));

    /** Tree-herb seed drop table (contents as on the wiki; weights approx, out of 209). */
    public static final List<Entry> TREE_HERB_SEEDS = List.of(
            e("ranarr_seed", 1, 15),
            e("snapdragon_seed", 1, 14),
            e("torstol_seed", 1, 11),
            e("watermelon_seed", 15, 21),
            e("willow_seed", 1, 20),
            e("mahogany_seed", 1, 18),
            e("maple_seed", 1, 18),
            e("teak_seed", 1, 18),
            e("yew_seed", 1, 16),
            e("papaya_seed", 1, 14),
            e("magic_seed", 1, 11),
            e("palm_seed", 1, 10),
            e("spirit_seed", 1, 8),
            e("dragonfruit_seed", 1, 6),
            e("celastrus_seed", 1, 5),
            e("redwood_tree_seed", 1, 4));

    /** Every marker {@link #table(String)} understands. */
    public static final List<String> MARKERS = List.of("#gems", "#rare", "#mega", "#herbs", "#seeds",
            "#allotment_seeds", "#rare_seeds", "#uncommon_seeds", "#tree_herb_seeds");

    public static List<Entry> table(String marker) {
        return switch (marker) {
            case "#gems" -> GEM;
            case "#rare" -> RARE;
            case "#mega" -> MEGA;
            case "#herbs" -> HERB;
            case "#allotment_seeds" -> ALLOTMENT_SEEDS;
            case "#rare_seeds", "#seeds" -> RARE_SEEDS;
            case "#uncommon_seeds" -> UNCOMMON_SEEDS;
            case "#tree_herb_seeds" -> TREE_HERB_SEEDS;
            default -> throw new IllegalArgumentException("Not a shared drop table: " + marker);
        };
    }

    /** The roll range of a table: unlisted slots up to it are "nothing". */
    public static int denominator(String marker) {
        return switch (marker) {
            case "#gems", "#rare", "#mega", "#herbs" -> DENOMINATOR;
            case "#allotment_seeds" -> 1008;
            case "#rare_seeds", "#seeds" -> 1002;
            case "#uncommon_seeds" -> 999;
            case "#tree_herb_seeds" -> 209;
            default -> throw new IllegalArgumentException("Not a shared drop table: " + marker);
        };
    }

    /**
     * Rolls a shared table, following sub-table links.
     *
     * @param nextInt returns a uniform int in [0, bound) for the given bound
     * @return the item, or null for a "nothing" slot
     */
    public static Result roll(String marker, IntUnaryOperator nextInt) {
        List<Entry> table = table(marker);
        int r = nextInt.applyAsInt(denominator(marker));
        for (Entry entry : table) {
            r -= entry.weight();
            if (r < 0) {
                if (entry.item().startsWith("#")) {
                    return roll(entry.item(), nextInt);
                }
                int count = entry.min() + (entry.max() > entry.min() ? nextInt.applyAsInt(entry.max() - entry.min() + 1) : 0);
                return new Result(entry.item(), count);
            }
        }
        return null;
    }
}
