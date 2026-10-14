package com.runecraft.osrs.content.godwars;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The revenants' level-scaled unique and ancient artefact rolls (OSRS Wiki, Revenants "Drop mechanics"); the
 * ordinary lines (ether, coins, bracelet of ethereum...) are on each revenant's drop table.
 *
 * <p>The wiki's unique-table rates fit {@code 1 / (176000 / (3 * floor(sqrt(level))))} unskulled to within 0.2% (approx)
 * (imp 1/29,333.7, goblin 1/19,546.9, hobgoblin 1/8,373.4, cyclops and hellhound 1/6,506.8, dark beast 1/5,866.7,
 * knight and dragon 1/5,333.4), 0.55 times that denominator skulled (dragon 1/2,933.4) and a fifth of it on a
 * revenant Slayer task. A unique is an amulet of avarice 2/5 of the time and each revenant weapon 1/5. The
 * artefact rates are the imp's (emblem 1/7,333.3, totem 1/11,000, statuette 1/22,000, medallion/effigy/relic
 * 1/44,000) scaled the same way.
 */
public final class RevenantRewards {
    private RevenantRewards() {
    }

    /** One artefact line: item and its rate as a multiple of the unique-table chance. */
    private record Artefact(String item, double timesUnique) {
    }

    private static final List<Artefact> ARTEFACTS = List.of(new Artefact("ancient_emblem", 4.0),
            new Artefact("ancient_totem", 8.0 / 3), new Artefact("ancient_statuette", 4.0 / 3),
            new Artefact("ancient_medallion", 2.0 / 3), new Artefact("ancient_effigy", 2.0 / 3),
            new Artefact("ancient_relic", 2.0 / 3));

    public static final List<String> WEAPONS = List.of("craws_bow", "thammarons_sceptre", "viggoras_chainmace");

    /** {@code floor(sqrt(level))}, at least 1. */
    static int root(int level) {
        return Math.max(1, (int) Math.floor(Math.sqrt(Math.max(1, level))));
    }

    /** 1-in-N chance of the unique table (amulet of avarice or a revenant weapon) per kill. */
    public static double uniqueDenominator(int level, boolean skulled, boolean onTask) {
        double n = 176000.0 / (3 * root(level));
        if (skulled) {
            n *= 0.55;
        }
        if (onTask) {
            n /= 5;
        }
        return n;
    }

    /** 1-in-N chance of an ancient artefact per kill. */
    public static double artefactDenominator(String item, int level, boolean skulled, boolean onTask) {
        for (Artefact a : ARTEFACTS) {
            if (a.item().equals(item)) {
                double n = uniqueDenominator(level, skulled, onTask) / a.timesUnique();
                if (skulled && item.equals("ancient_emblem")) {
                    // Skulled players trade emblems for the better artefacts: the wiki has the emblem nearly 3.5
                    // times rarer skulled.
                    n = uniqueDenominator(level, false, onTask) / a.timesUnique() * 3.5; // approx
                }
                return n;
            }
        }
        throw new IllegalArgumentException(item);
    }

    /**
     * The extra items of one kill. {@code random.applyAsInt(bound)} returns 0..bound-1. Rates are rolled out of
     * 1,000,000 so the fractional wiki denominators keep their precision.
     */
    public static List<String> roll(int level, boolean skulled, boolean onTask, IntUnaryOperator random) {
        List<String> out = new ArrayList<>();
        if (hit(uniqueDenominator(level, skulled, onTask), random)) {
            int r = random.applyAsInt(5);
            out.add(r < 2 ? "amulet_of_avarice" : WEAPONS.get(r - 2));
        }
        for (Artefact a : ARTEFACTS) {
            if (hit(artefactDenominator(a.item(), level, skulled, onTask), random)) {
                out.add(a.item());
                break; // one artefact roll per kill
            }
        }
        return out;
    }

    private static boolean hit(double denominator, IntUnaryOperator random) {
        return random.applyAsInt(1_000_000) < Math.round(1_000_000 / denominator);
    }
}
