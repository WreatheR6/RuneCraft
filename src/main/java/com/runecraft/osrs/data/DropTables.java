package com.runecraft.osrs.data;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The shared OSRS drop tables that monster drops roll into: the gem drop table ({@code #gems}),
 * the rare drop table ({@code #rare}) and its mega-rare sub-table. Weights are out of 128 as on the
 * OSRS wiki; slots that are not listed are "nothing". The table structure is exact, a few weights
 * are approx where the wiki snapshots disagree.
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

    public static List<Entry> table(String marker) {
        return switch (marker) {
            case "#gems" -> GEM;
            case "#rare" -> RARE;
            case "#mega" -> MEGA;
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
        int r = nextInt.applyAsInt(DENOMINATOR);
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
