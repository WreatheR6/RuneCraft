package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * How a Desert Treasure II boss kill is looted (OSRS Wiki "Duke Sucellus", "Vardorvis"... Drops/Mechanics, from the
 * "Desert Treasure II - Tweaks and Drop Rates" news post): the rolls below are made in order until one succeeds.
 * <ol>
 *   <li>1/n for the tradeable unique table (3/n awakened): 3/8 vestige progress, 3/8 chromium ingot, 1/8 the boss's
 *       weapon/axe piece, 1/8 a virtus piece (1/3 each);</li>
 *   <li>1/orb for the awakener's orb, then 1/25 for the boss's tablet, then 1/200 for its quartz;</li>
 *   <li>1/5 for the supply table, whose items are all dropped together;</li>
 *   <li>otherwise one roll on the standard table, 50% more of it for a perfect kill (the wiki's "normal,perfect"
 *       quantities, {@link Dt2Bosses#perfectQuantity}).</li>
 * </ol>
 * After that a clue scroll is rolled at 1/40 (easy, medium, hard or elite alike; the pet is rolled by the caller).
 *
 * <p>The boss's {@code drops} list still describes the table for the drop viewer, the collection log and the snapshot
 * tests; its standard lines (everything that isn't a unique, orb, tablet, quartz, supply or clue) give the standard
 * table's weights as 1/rarity.
 */
public final class Dt2Loot {
    private Dt2Loot() {
    }

    /** OSRS: a clue scroll at 1/40 after the main roll, easy/medium/hard/elite at 1/160 each. */
    public static final int CLUE_RATE = 40;
    public static final List<String> CLUES = List.of("clue_scroll_easy", "clue_scroll_medium", "clue_scroll_hard", "clue_scroll_elite");
    /** A vestige-progress roll that doesn't complete the vestige drops a gold ring (OSRS: one, then two). */
    public static final String GOLD_RING = "gold_ring";

    /** Which of the five rolls a kill's main drop came from. */
    public enum Kind { UNIQUE, ORB, TABLET, QUARTZ, SUPPLIES, STANDARD }

    /** A rolled kill: which roll succeeded and the items (min == max, potions with a dose suffix). */
    public record Result(Kind kind, List<Drop> items) {
    }

    /**
     * Rolls a kill of {@code bossId}. {@code table} is the boss's (or awakened variant's) drop list; {@code nextInt}
     * returns a uniform int in [0, bound).
     */
    public static Result roll(String bossId, List<Drop> table, boolean awakened, boolean perfect, IntUnaryOperator nextInt) {
        int idx = AwakenedBosses.BOSSES.indexOf(bossId);
        if (idx < 0) {
            throw new IllegalArgumentException(bossId);
        }
        List<Drop> out = new ArrayList<>();
        Kind kind;
        int n = AwakenedBosses.uniqueTableRate(bossId);
        if (nextInt.applyAsInt(n) < (awakened ? 3 : 1)) {
            kind = Kind.UNIQUE;
            out.add(unique(bossId, nextInt));
        } else if (nextInt.applyAsInt(AwakenedBosses.orbRate(bossId)) == 0) {
            kind = Kind.ORB;
            out.add(one(AwakenedBosses.ORB, 1));
        } else if (nextInt.applyAsInt(Dt2Bosses.TABLET_RATE) == 0) {
            kind = Kind.TABLET;
            out.add(one(Dt2Bosses.TABLETS.get(idx), 1));
        } else if (nextInt.applyAsInt(Dt2Bosses.QUARTZ_RATE) == 0) {
            // approx: OSRS improves the first quartz's rate from 1/200 to 1/50 over the first 300 kills.
            kind = Kind.QUARTZ;
            out.add(one(Dt2Bosses.QUARTZES.get(idx), 1));
        } else if (nextInt.applyAsInt(Dt2Bosses.SUPPLY_RATE) == 0) {
            kind = Kind.SUPPLIES;
            for (Drop d : Dt2Bosses.supplies(bossId)) {
                out.add(one(d.item(), d.min() + (d.max() > d.min() ? nextInt.applyAsInt(d.max() - d.min() + 1) : 0)));
            }
        } else {
            kind = Kind.STANDARD;
            Drop d = standard(bossId, table, nextInt);
            if (d != null) {
                int count = d.min() + (d.max() > d.min() ? nextInt.applyAsInt(d.max() - d.min() + 1) : 0);
                out.add(one(d.item(), perfect ? Dt2Bosses.perfectQuantity(bossId, d.item(), count) : count));
            }
        }
        if (nextInt.applyAsInt(CLUE_RATE) == 0) {
            out.add(one(CLUES.get(nextInt.applyAsInt(CLUES.size())), 1));
        }
        return new Result(kind, out);
    }

    /** The tradeable unique table: vestige progress 3/8 (a third of those rolls give the vestige), chromium 3/8, piece 1/8, virtus 1/8. */
    static Drop unique(String bossId, IntUnaryOperator nextInt) {
        int r = nextInt.applyAsInt(8);
        if (r < 3) {
            // approx: OSRS counts the vestige rolls per player (gold ring, two gold rings, then the vestige); here each
            // roll completes it with chance 1/3, which keeps the vestige's rate.
            int step = nextInt.applyAsInt(3);
            return step == 2 ? one(vestige(bossId), 1) : one(GOLD_RING, step + 1);
        }
        if (r < 6) {
            return one("chromium_ingot", 1);
        }
        if (r == 6) {
            return one(piece(bossId), 1);
        }
        return one(List.of("virtus_mask", "virtus_robe_top", "virtus_robe_bottom").get(nextInt.applyAsInt(3)), 1);
    }

    public static String vestige(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> "magus_vestige";
            case "the_leviathan" -> "venator_vestige";
            case "the_whisperer" -> "bellator_vestige";
            case "vardorvis" -> "ultor_vestige";
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    public static String piece(String bossId) {
        return switch (bossId) {
            case "duke_sucellus" -> "eye_of_the_duke";
            case "the_leviathan" -> "leviathans_lure";
            case "the_whisperer" -> "sirens_staff";
            case "vardorvis" -> "executioners_axe_head";
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    /** The standard-table lines of a boss's drop list (weights 1/rarity). */
    public static List<Drop> standardLines(String bossId, List<Drop> table) {
        Set<String> supplies = new java.util.HashSet<>();
        for (Drop d : Dt2Bosses.supplies(bossId)) {
            supplies.add(d.item());
        }
        List<Drop> out = new ArrayList<>();
        for (Drop d : table) {
            String item = d.item();
            if (AwakenedBosses.UNIQUE_TABLE.contains(item) || item.equals(AwakenedBosses.ORB) || Dt2Bosses.TABLETS.contains(item)
                    || Dt2Bosses.QUARTZES.contains(item) || supplies.contains(item) || item.startsWith("clue_scroll_")
                    || item.equals(AwakenedBosses.BLOOD_KIT) || item.startsWith("#") || d.rarity() <= 1) {
                continue;
            }
            out.add(d);
        }
        return out;
    }

    /** One standard-table line, weighted by 1/rarity (the wiki's k/100 rarities). */
    static Drop standard(String bossId, List<Drop> table, IntUnaryOperator nextInt) {
        List<Drop> lines = standardLines(bossId, table);
        if (lines.isEmpty()) {
            return null;
        }
        // Weights in 1/1000ths of 1/rarity, so 1/12 (8/100) and 1/11 (9/100) keep their difference.
        int[] w = new int[lines.size()];
        int total = 0;
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.max(1, Math.round(100_000f / lines.get(i).rarity()));
            total += w[i];
        }
        int r = nextInt.applyAsInt(total);
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r < 0) {
                return lines.get(i);
            }
        }
        return lines.get(lines.size() - 1);
    }

    private static Drop one(String item, int count) {
        return new Drop(item, count, count, 1);
    }
}
