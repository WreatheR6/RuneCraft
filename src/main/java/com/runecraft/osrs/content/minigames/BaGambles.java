package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * Barbarian Assault gambles ("lucky dips"), bought from Commander Connad with the honour points of
 * one role of the player's choice (OSRS Wiki, Barbarian Assault/Gambles). Low and medium gambles
 * need the player to have reached wave 3 / wave 6 of a round that hasn't been reset or finished;
 * a high gamble costs 500 points and one Penance Queen kill. Every line of a tier's main table is
 * 1/32; the high table adds a 1/16,000 dragon med helm and dragon chainbody, and a high gamble also
 * rolls an elite clue scroll (1/15) and the pet penance queen (1/1,000) on top.
 */
public final class BaGambles {
    /** Prefix of the reward shop entries that run a gamble: {@code #ba_gamble_low} etc. */
    public static final String ITEM_PREFIX = "#ba_gamble_";
    public static final int ELITE_CLUE_CHANCE = 15;
    public static final int PET_CHANCE = 1000;
    /** Each dragon item is 1/16,000 on the high table (Mod Mat K). */
    public static final int DRAGON_CHANCE = 16_000;
    /** Re-rolls when a line's item isn't in RuneCraft (a safety net; see MISSING). */
    private static final int MAX_REROLLS = 50;

    public enum Tier {
        LOW(200, 3, false), MEDIUM(400, 6, false), HIGH(500, 1, true);

        private final int cost;
        private final int wave;
        private final boolean queen;

        Tier(int cost, int wave, boolean queen) {
            this.cost = cost;
            this.wave = wave;
            this.queen = queen;
        }

        /** Honour points of the chosen role. */
        public int cost() {
            return cost;
        }

        /** Wave the player must have reached in the current round (high gambles: any). */
        public int wave() {
            return wave;
        }

        /** Whether it also spends a Penance Queen kill. */
        public boolean queen() {
            return queen;
        }

        public String id() {
            return ITEM_PREFIX + name().toLowerCase(java.util.Locale.ROOT);
        }

        public String displayName() {
            return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT) + " gamble";
        }

        public static Tier of(String item) {
            for (Tier t : values()) {
                if (t.id().equals(item)) {
                    return t;
                }
            }
            return null;
        }
    }

    /** One table line: item id, quantity range and whether it is handed out as bank notes. */
    public record Line(String item, int min, int max, boolean noted) {
    }

    /** One reward of a gamble. */
    public record Prize(String item, int amount, boolean noted) {
    }

    private static Line l(String item, int min, int max, boolean noted) {
        return new Line(item, min, max, noted);
    }

    private static Line l(String item, int n, boolean noted) {
        return new Line(item, n, n, noted);
    }

    /**
     * Wiki lines left out of the tables because RuneCraft has no such item; the other lines share their chance.
     * // approx
     */
    public static final List<String> MISSING = List.of();

    /** Feathers are the vanilla item (fishing bait and fletching use it too). */
    public static final String FEATHER = "minecraft:feather";

    /** Low gamble table (32 lines in OSRS, 1/32 each). Rune dagger(p++) stands in as a plain rune dagger. // approx */
    public static final List<Line> LOW = List.of(
            l("harralander", 15, 25, true), l("toadflax", 15, 25, true), l("irit_leaf", 15, 25, true),
            l("harralander_seed", 5, 7, false), l("toadflax_seed", 5, 7, false), l("irit_seed", 5, 7, false),
            l("strawberry_seed", 20, false), l("pineapple_seed", 1, 2, false), l("maple_seed", 1, 2, false),
            l("rune_axe", 1, false), l("rune_med_helm", 1, false), l("rune_dagger", 1, false), l("lava_battlestaff", 1, false),
            l("adamant_sq_shield", 1, false), l("iron_ore", 200, true), l("coal", 150, true),
            l("mithril_ore", 20, true), l("mithril_bar", 15, true), l("flax", 50, 75, true), l("blue_dragonhide", 10, 20, true),
            l("uncut_emerald", 25, 30, true), l("maple_logs", 150, 200, true), l("oak_plank", 60, 80, true),
            l("mithril_arrow", 300, 400, false), l("raw_lobster", 125, 150, true), l("tuna", 90, 110, true),
            l("cosmic_rune", 150, 200, false), l("chaos_rune", 150, 200, false), l("limpwurt_root", 10, 15, true),
            l("unicorn_horn", 10, 15, true), l(FEATHER, 500, 750, false), l("coins", 15_000, 20_000, false));

    /** Medium gamble table (32 lines in OSRS, 1/32 each). */
    public static final List<Line> MEDIUM = List.of(
            l("avantoe", 25, 35, true), l("kwuarm", 25, 35, true), l("dwarf_weed", 25, 35, true),
            l("avantoe_seed", 10, 14, false), l("kwuarm_seed", 10, 14, false), l("dwarf_weed_seed", 10, 14, false),
            l("watermelon_seed", 20, false), l("papaya_seed", 1, 2, false), l("yew_seed", 1, 2, false),
            l("rune_battleaxe", 1, false), l("rune_sq_shield", 1, false), l("rune_full_helm", 1, false), l("mystic_lava_staff", 1, false),
            l("loop_half_of_key", 1, false), l("tooth_half_of_key", 1, false),
            l("coal", 400, true), l("adamantite_ore", 40, true), l("adamantite_bar", 25, true),
            l("big_bones", 120, true), l("red_dragonhide", 20, 30, true), l("uncut_ruby", 25, 30, true),
            l("yew_logs", 150, 200, true), l("teak_plank", 60, 80, true), l("adamant_arrow", 400, 500, false),
            l("nature_rune", 250, 300, false), l("death_rune", 250, 300, false), l("raw_swordfish", 180, 210, true),
            l("swordfish", 90, 110, true), l("limpwurt_root", 20, 30, true), l("unicorn_horn", 20, 30, true),
            l(FEATHER, 1200, 1500, false), l("coins", 30_000, 40_000, false));

    /** High gamble table (32 lines in OSRS after the 2/16,000 dragon roll). */
    public static final List<Line> HIGH = List.of(
            l("ranarr_weed", 20, 30, true), l("snapdragon", 20, 30, true), l("torstol", 20, 30, true),
            l("ranarr_seed", 3, 4, false), l("snapdragon_seed", 3, 4, false), l("torstol_seed", 3, 4, false),
            l("watermelon_seed", 50, false), l("palm_seed", 1, 2, false), l("magic_seed", 1, false),
            l("archer_helm", 1, false), l("berserker_helm", 1, false), l("warrior_helm", 1, false), l("farseer_helm", 1, false),
            l("granite_helm", 1, false), l("granite_shield", 1, false), l("rune_kiteshield", 1, false),
            l("coal", 750, true), l("runite_ore", 20, true), l("runite_bar", 15, true), l("dragon_bones", 50, 75, true),
            l("black_dragonhide", 30, 50, true), l("uncut_diamond", 25, 30, true), l("magic_logs", 50, 100, true),
            l("mahogany_plank", 60, 80, true), l("rune_arrow", 400, 500, false), l("raw_shark", 250, 300, true),
            l("shark", 75, 125, true), l("law_rune", 250, 300, false), l("blood_rune", 250, 300, false),
            l("limpwurt_root", 30, 50, true), l("unicorn_horn", 30, 50, true), l("coins", 50_000, 75_000, false));

    public static final List<String> DRAGON = List.of("dragon_med_helm", "dragon_chainbody");

    private BaGambles() {
    }

    public static List<Line> table(Tier tier) {
        return switch (tier) {
            case LOW -> LOW;
            case MEDIUM -> MEDIUM;
            case HIGH -> HIGH;
        };
    }

    /** Why a gamble can't be bought (wave not reached), or null. */
    public static String lock(Tier tier, int waveReached) {
        if (tier.wave() > 1 && waveReached < tier.wave()) {
            return "Requires reaching wave " + tier.wave() + " of a Barbarian Assault round";
        }
        return null;
    }

    /**
     * Rolls one gamble: one main-table prize, plus (high gambles) the elite clue and pet rolls.
     * Lines whose item RuneCraft lacks ({@code exists} false) are rolled again.
     */
    public static List<Prize> roll(Tier tier, DoubleSupplier random, Predicate<String> exists) {
        List<Prize> out = new ArrayList<>();
        Prize main = null;
        if (tier == Tier.HIGH) {
            double r = random.getAsDouble() * DRAGON_CHANCE;
            if (r < DRAGON.size()) {
                String dragon = DRAGON.get((int) r);
                if (exists.test(dragon)) {
                    main = new Prize(dragon, 1, false);
                }
            }
        }
        List<Line> table = table(tier);
        for (int i = 0; main == null && i < MAX_REROLLS; i++) {
            Line line = table.get(Math.min(table.size() - 1, (int) (random.getAsDouble() * table.size())));
            if (exists.test(line.item())) {
                int n = line.min() + (int) (random.getAsDouble() * (line.max() - line.min() + 1));
                main = new Prize(line.item(), Math.min(line.max(), n), line.noted());
            }
        }
        if (main == null) {
            main = new Prize("coins", table.get(table.size() - 1).min(), false);
        }
        out.add(main);
        if (tier == Tier.HIGH) {
            if (random.getAsDouble() * ELITE_CLUE_CHANCE < 1) {
                out.add(new Prize("clue_scroll_elite", 1, false));
            }
            if (random.getAsDouble() * PET_CHANCE < 1) {
                out.add(new Prize("pet_penance_queen", 1, false));
            }
        }
        return out;
    }
}
