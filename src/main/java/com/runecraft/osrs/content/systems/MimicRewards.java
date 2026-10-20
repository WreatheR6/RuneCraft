package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import java.util.List;
import java.util.Random;

/**
 * The Mimic (OSRS Wiki "The Mimic"): the pre-roll (a 3rd age item, each piece equally likely, and six times that chance of a
 * ring of 3rd age: master 1/228 on the first attempt and 1/240 after, elite 1/250 and 1/264), the attempts a mimic casket
 * allows (six from a master casket, five from an elite one), the casket rolls and main-drop quantities lost to each failed
 * attempt, and the one-time guaranteed Mimic on the 10th master or 25th elite casket. Rolled by the systems module when the
 * Mimic dies, since it owns the clue reward items.
 */
public final class MimicRewards {
    public static final String MIMIC = "the_mimic";
    public static final String RING = "ring_of_3rd_age";
    /** The master pre-roll table on the first attempt. */
    public static final int TABLE = 228;
    public static final int RING_SLOTS = 6;
    /** The one-time guaranteed Mimic: on this master or elite casket (counting it), whichever comes first. */
    public static final int GUARANTEED_MASTER = 10;
    public static final int GUARANTEED_ELITE = 25;
    /** The 23 3rd age pieces of the pre-roll. */
    public static final List<String> THIRD_AGE = List.of("3rd_age_full_helmet", "3rd_age_platebody", "3rd_age_platelegs",
            "3rd_age_plateskirt", "3rd_age_kiteshield", "3rd_age_range_coif", "3rd_age_range_top", "3rd_age_range_legs",
            "3rd_age_vambraces", "3rd_age_mage_hat", "3rd_age_robe_top", "3rd_age_robe", "3rd_age_amulet", "3rd_age_longsword",
            "3rd_age_bow", "3rd_age_wand", "3rd_age_cloak", "3rd_age_druidic_robe_top", "3rd_age_druidic_robe_bottoms",
            "3rd_age_druidic_cloak", "3rd_age_druidic_staff", "3rd_age_pickaxe", "3rd_age_axe");

    private MimicRewards() {
    }

    /** One master kill's pre-roll on the first attempt: the ring of 3rd age, a 3rd age piece, or null. */
    public static String roll(Random random) {
        return roll(Tier.MASTER, 1, random);
    }

    /** One kill's pre-roll for a casket of the tier killed on the given attempt (1 = first try). */
    public static String roll(Tier tier, int attempt, Random random) {
        int x = random.nextInt(table(tier, attempt));
        if (x < RING_SLOTS) {
            return RING;
        }
        return x == RING_SLOTS ? THIRD_AGE.get(random.nextInt(THIRD_AGE.size())) : null;
    }

    /** The pre-roll's table size: master 228 on the first attempt and 240 after; elite 250 and 264. */
    public static int table(Tier tier, int attempt) {
        boolean first = attempt <= 1;
        return tier == Tier.ELITE ? (first ? 250 : 264) : (first ? TABLE : 240);
    }

    /** How many tries a mimic casket gives: six from a master casket, five from an elite one. */
    public static int attempts(Tier tier) {
        return tier == Tier.MASTER ? 6 : tier == Tier.ELITE ? 5 : 0;
    }

    /**
     * A main-drop quantity on the given attempt: each failure takes 1/5 (elite) or 1/6 (master) of the maximum away, rounded
     * down (master blood runes 500, 416, 333, 250, 166, 83).
     */
    public static int quantity(Tier tier, int max, int attempt) {
        int n = attempts(tier);
        if (n <= 0 || attempt <= 1) {
            return max;
        }
        int left = n - Math.min(attempt, n) + 1;
        return Math.max(1, (int) ((long) max * left / n));
    }

    /** Whether this casket (the {@code completed}-th of its tier) is the one-time guaranteed Mimic. */
    public static boolean guaranteed(Tier tier, int completed, boolean seenOne) {
        if (seenOne) {
            return false;
        }
        return tier == Tier.MASTER ? completed >= GUARANTEED_MASTER : tier == Tier.ELITE && completed >= GUARANTEED_ELITE;
    }

    /**
     * A mimic casket: its tier, the item rolls the casket held, and how many fights it has started (0 before the first).
     * Saved on the mimic item ({@link #encode()}: "TIER|rolls|attempt").
     */
    public record Casket(Tier tier, int rolls, int attempt) {
        public String encode() {
            return tier.name() + "|" + rolls + "|" + attempt;
        }

        /** The casket of an encoding; a bare tier name is a casket with its tier's fewest rolls; null when unreadable. */
        public static Casket decode(String s) {
            if (s == null || s.isEmpty()) {
                return null;
            }
            String[] parts = s.split("\\|");
            try {
                Tier tier = Tier.valueOf(parts[0]);
                if (attempts(tier) <= 0) {
                    return null;
                }
                int rolls = parts.length > 1 ? Integer.parseInt(parts[1]) : tier.minRolls();
                int attempt = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return new Casket(tier, Math.max(0, rolls), Math.max(0, attempt));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /** The same casket as its next fight starts. */
        public Casket nextAttempt() {
            return new Casket(tier, rolls, attempt + 1);
        }

        /** Whether every try has been used (the casket's loot is lost). */
        public boolean exhausted() {
            return attempt >= attempts(tier);
        }

        /** The casket's item rolls for a kill on the current attempt: one fewer for each failed attempt before it. */
        public int rollsNow() {
            return Math.max(0, rolls - Math.max(0, attempt - 1));
        }
    }
}
