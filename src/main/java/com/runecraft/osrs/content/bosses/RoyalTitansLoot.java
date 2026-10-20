package com.runecraft.osrs.content.bosses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Royal Titans' rewards (OSRS Wiki "Royal Titans" Rewards, "Branda the Fire Queen" / "Eldric the Ice King" drops): once
 * both Titans fall, each player who fought chooses one of three options on one of them:
 * <ul>
 *   <li><b>Loot</b> that Titan: its pre-roll (staff crown and prayer scroll 1/75 each, giantsoul amulet 1/16), else two rolls
 *       on its standard table (/55 Branda, /56 Eldric);</li>
 *   <li><b>Sacrifice</b>: no loot, the pet rate doubled (1/3,000 to 1/1,500);</li>
 *   <li><b>Take pages</b>: 10-19 desiccated pages.</li>
 * </ul>
 * With two players, unique rates and quantities scale linearly with the player's contribution: the whole-number percent of
 * the damage they dealt to the Titans (elementals don't count). Under 1% a player still rolls twice but gets one of each
 * item. Tertiaries (hard clue 1/25, elite clue 1/100, the pet) don't depend on contribution and are rolled for every
 * option. Diffed against the wiki snapshot {@code bosses/royal_titans_loot.tsv} by {@code RoyalTitansLootTest}.
 */
public final class RoyalTitansLoot {
    private RoyalTitansLoot() {
    }

    public static final String BRANDA = "branda_the_fire_queen";
    public static final String ELDRIC = "eldric_the_ice_king";
    public static final String PAGE = "desiccated_page";
    /** Both Titans' hitpoints: contribution is a share of this. */
    public static final int TOTAL_HITPOINTS = 1200;
    public static final int PAGES_MIN = 10;
    public static final int PAGES_MAX = 19;
    public static final int HARD_CLUE = 25;
    public static final int ELITE_CLUE = 100;
    public static final int PET = 3000;
    public static final int SACRIFICE_PET = 1500;

    /** What a player does with a fallen Titan. */
    public enum Choice { LOOT, SACRIFICE, PAGES }

    /** A line of a Titan's table: quantities at 100% contribution, {@code weight} the 1/N (pre-roll) or slots (main table). */
    public record Line(String item, int min, int max, int weight) {
    }

    public record Loot(String item, int count) {
    }

    public static final List<Line> BRANDA_PRE = List.of(new Line("fire_element_staff_crown", 1, 1, 75),
            new Line("mystic_vigour_prayer_scroll", 1, 1, 75), new Line("giantsoul_amulet", 1, 1, 16));
    public static final List<Line> ELDRIC_PRE = List.of(new Line("ice_element_staff_crown", 1, 1, 75),
            new Line("deadeye_prayer_scroll", 1, 1, 75), new Line("giantsoul_amulet", 1, 1, 16));

    /** Branda's standard table, slots out of 55 per roll. */
    public static final List<Line> BRANDA_MAIN = List.of(
            new Line("mystic_fire_staff", 4, 4, 2), new Line("fire_battlestaff", 6, 6, 2), new Line("rune_plateskirt", 2, 2, 2),
            new Line("rune_scimitar", 2, 2, 2), new Line("rune_pickaxe", 2, 2, 2),
            new Line("fire_rune", 1500, 3000, 2), new Line("chaos_rune", 200, 400, 2), new Line("death_rune", 150, 300, 2),
            new Line("nature_rune", 100, 150, 2), new Line("law_rune", 100, 150, 2), new Line("soul_rune", 100, 150, 2),
            new Line("blood_rune", 100, 150, 2), new Line("rune_arrow", 100, 300, 2),
            new Line("coal", 280, 320, 2), new Line("gold_ore", 60, 100, 2), new Line("fire_orb", 20, 40, 2),
            new Line("grimy_avantoe", 6, 8, 2), new Line("grimy_cadantine", 6, 8, 2), new Line("grimy_dwarf_weed", 6, 8, 2),
            new Line("grimy_irit_leaf", 6, 8, 2), new Line("grimy_kwuarm", 6, 8, 2), new Line("grimy_lantadyme", 6, 8, 2),
            new Line("grimy_ranarr_weed", 6, 8, 2),
            new Line("maple_seed", 4, 6, 1), new Line("palm_seed", 2, 2, 1), new Line("yew_seed", 2, 2, 1),
            new Line("coins", 10000, 30000, 2), new Line("prayer_potion", 2, 4, 2), new Line(PAGE, 6, 24, 2));

    /** Eldric's standard table, slots out of 56 per roll. */
    public static final List<Line> ELDRIC_MAIN = List.of(
            new Line("mystic_water_staff", 4, 4, 2), new Line("water_battlestaff", 6, 6, 2), new Line("rune_platelegs", 2, 2, 2),
            new Line("rune_sq_shield", 2, 2, 2), new Line("rune_axe", 2, 2, 2), new Line("rune_pickaxe", 2, 2, 1),
            new Line("water_rune", 1500, 3000, 2), new Line("chaos_rune", 200, 400, 2), new Line("death_rune", 150, 300, 2),
            new Line("nature_rune", 100, 150, 2), new Line("law_rune", 100, 150, 2), new Line("soul_rune", 100, 150, 2),
            new Line("blood_rune", 100, 150, 2), new Line("rune_arrow", 100, 300, 2),
            new Line("coal", 280, 320, 2), new Line("gold_ore", 60, 100, 2), new Line("water_orb", 20, 40, 2),
            new Line("avantoe_seed", 4, 4, 2), new Line("cadantine_seed", 4, 4, 2), new Line("dwarf_weed_seed", 3, 3, 2),
            new Line("irit_seed", 4, 4, 2), new Line("kwuarm_seed", 4, 4, 2), new Line("lantadyme_seed", 4, 4, 2),
            new Line("ranarr_seed", 4, 4, 2),
            new Line("maple_seed", 4, 6, 1), new Line("palm_seed", 2, 2, 1), new Line("yew_seed", 2, 2, 1),
            new Line("coins", 10000, 30000, 2), new Line("prayer_potion", 2, 4, 2), new Line(PAGE, 6, 24, 2));

    public static List<Line> pre(String titan) {
        return ELDRIC.equals(titan) ? ELDRIC_PRE : BRANDA_PRE;
    }

    public static List<Line> main(String titan) {
        return ELDRIC.equals(titan) ? ELDRIC_MAIN : BRANDA_MAIN;
    }

    /** Slots of a Titan's standard table (55 Branda, 56 Eldric). */
    public static int mainWeight(String titan) {
        int total = 0;
        for (Line l : main(titan)) {
            total += l.weight();
        }
        return total;
    }

    /** Contribution as the wiki counts it: the whole-number percent of the Titans' hitpoints the player dealt (0-100). */
    public static int percent(int damage) {
        return Math.max(0, Math.min(100, damage * 100 / TOTAL_HITPOINTS));
    }

    /** A quantity at this contribution: linear above 1%, one of each below. */
    public static int scaled(int quantity, int percent) {
        return Math.max(1, quantity * percent / 100);
    }

    /** The pet rate of a choice (sacrificing doubles it). */
    public static int petRate(Choice choice) {
        return choice == Choice.SACRIFICE ? SACRIFICE_PET : PET;
    }

    /**
     * One player's rewards from {@code titan} for {@code damage} dealt to the Titans and their choice (the pet is the
     * caller's: {@link #petRate}). Nothing for a player who dealt no damage.
     */
    public static List<Loot> roll(String titan, Choice choice, int damage, Random random) {
        List<Loot> out = new ArrayList<>();
        if (damage <= 0) {
            return out;
        }
        int pct = percent(damage);
        switch (choice) {
            case LOOT -> {
                Loot unique = null;
                for (Line u : pre(titan)) {
                    // the unique rate scales with contribution: 1/75 at 100%, 1/150 at 50%
                    if (unique == null && random.nextDouble() * u.weight() * 100 < Math.max(pct, 0)) {
                        unique = new Loot(u.item(), 1);
                    }
                }
                if (unique != null) {
                    out.add(unique);
                } else {
                    for (int roll = 0; roll < 2; roll++) {
                        out.add(standard(titan, pct, random));
                    }
                }
            }
            case PAGES -> {
                int pages = (PAGES_MIN + random.nextInt(PAGES_MAX - PAGES_MIN + 1)) * pct / 100;
                if (pages > 0) {
                    out.add(new Loot(PAGE, pages));
                }
            }
            case SACRIFICE -> {
            }
        }
        if (random.nextInt(HARD_CLUE) == 0) {
            out.add(new Loot("clue_scroll_hard", 1));
        }
        if (random.nextInt(ELITE_CLUE) == 0) {
            out.add(new Loot("clue_scroll_elite", 1));
        }
        return out;
    }

    /** One roll on a Titan's standard table at this contribution. */
    static Loot standard(String titan, int percent, Random random) {
        int r = random.nextInt(mainWeight(titan));
        for (Line l : main(titan)) {
            r -= l.weight();
            if (r < 0) {
                int q = l.min() + (l.max() > l.min() ? random.nextInt(l.max() - l.min() + 1) : 0);
                return new Loot(l.item(), scaled(q, percent));
            }
        }
        throw new IllegalStateException("table weights");
    }

    /** Which Titan a choice command names ("branda"/"eldric"), or null. */
    public static String titan(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "branda", BRANDA -> BRANDA;
            case "eldric", ELDRIC -> ELDRIC;
            default -> null;
        };
    }
}
