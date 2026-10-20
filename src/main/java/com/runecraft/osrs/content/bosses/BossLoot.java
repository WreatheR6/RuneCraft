package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * How a boss kill rolls its drop list, as OSRS rolls a boss's drop table (OSRS Wiki "Drop table"): every 100% line drops,
 * tertiary lines (clue scrolls, jars) are rolled on their own, and the other lines are the main table, rolled once a kill
 * (or {@link #rolls} times), so a kill always lands on one of its lines. Rolling every line on its own instead, as an
 * ordinary monster's list is, left about a third of the kills of a boss without a 100% drop (Kraken, Scorpia, Sarachnis,
 * the Moons...) with nothing at all and gave others three or four drops.
 *
 * <p>The main lines keep their own 1/N rates: they are laid end to end, each as long as its rate, in a random order, and a
 * kill takes the lines under one random point and every whole step after it (systematic sampling). A kill therefore drops
 * as many main lines as their rates add up to (one for a table that adds up to one roll), never none from a full table. The
 * wiki rarities are rounded to whole 1/N and a few items aren't in RuneCraft, so a full table can add up to a little less
 * than a roll: from {@link #FULL_TABLE} of a roll on, the missing share is spread over the common lines (rarer than
 * {@link #COMMON_BELOW} lines, the uniques, keep their exact rates). A sparse list (Nex's generals' 1/1000 robes) stays
 * sparse: most of its kills drop nothing, as in OSRS.
 */
public final class BossLoot {
    /** Main lines adding up to at least this share of a roll are a full main table (the rest is rounding). */
    public static final double FULL_TABLE = 0.9;
    /** Lines more common than 1/50 take a full table's missing share (the boss drop patcher's rare-line boundary too). */
    public static final int COMMON_BELOW = 50;
    /** Item id prefixes of tertiary drops, rolled on their own beside the main table (OSRS Wiki "Drop table#Tertiary"). */
    private static final List<String> TERTIARY = List.of("clue_scroll", "jar_of");
    /** Bosses whose kill rolls the main table more than once (their lines give the rate per roll). */
    private static final Map<String, Integer> ROLLS = Map.of(
            // OSRS Wiki "Gemstone Crab": the top damagers mine three uncut gems from its shell (rolls=3 in its drop table).
            "gemstone_crab", 3);

    private BossLoot() {
    }

    /** Main-table rolls of a kill of {@code bossId}. */
    public static int rolls(String bossId) {
        return ROLLS.getOrDefault(bossId, 1);
    }

    /** Whether a line is a tertiary drop (rolled on its own). */
    public static boolean tertiary(Drop d) {
        for (String p : TERTIARY) {
            if (d.item().startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The lines a kill drops (quantities and shared {@code #tables} are resolved by the caller). {@code nextInt} returns a
     * uniform int in [0, bound).
     */
    public static List<Drop> pick(List<Drop> table, int rolls, IntUnaryOperator nextInt) {
        List<Drop> out = new ArrayList<>();
        List<Drop> main = new ArrayList<>();
        for (Drop d : table) {
            if (d.rarity() <= 1) {
                out.add(d);
            } else if (tertiary(d)) {
                if (nextInt.applyAsInt(d.rarity()) == 0) {
                    out.add(d);
                }
            } else {
                main.add(d);
            }
        }
        if (main.isEmpty()) {
            return out;
        }
        int r = Math.max(1, rolls);
        double total = 0;
        double common = 0;
        for (Drop d : main) {
            total += (double) r / d.rarity();
            if (d.rarity() < COMMON_BELOW) {
                common += (double) r / d.rarity();
            }
        }
        // A full table short of a whole number of rolls only by rounding: the common lines take the missing share.
        double stretch = total < r && total >= FULL_TABLE * r && common > 0 ? 1 + (r - total) / common : 1;
        // A random order each kill, so which lines can drop together doesn't depend on the order of the list.
        for (int i = main.size() - 1; i > 0; i--) {
            Collections.swap(main, i, nextInt.applyAsInt(i + 1));
        }
        double u = nextInt.applyAsInt(1 << 24) / (double) (1 << 24);
        double at = 0;
        for (Drop d : main) {
            double w = Math.min(1, (double) r / d.rarity() * (d.rarity() < COMMON_BELOW ? stretch : 1));
            if (points(at + w, u) > points(at, u)) {
                out.add(d);
            }
            at += w;
        }
        return out;
    }

    /** How many of the points u, u + 1, u + 2... lie below {@code x}. */
    private static long points(double x, double u) {
        return x <= u ? 0 : (long) Math.ceil(x - u);
    }
}
