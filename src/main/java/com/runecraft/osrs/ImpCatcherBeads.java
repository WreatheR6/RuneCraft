package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Imp Catcher's beads. As in OSRS, imps carry Wizard Mizgog's beads: every imp has the four beads on
 * its ordinary drop table (TownMonsters), and while the quest is under way an imp kill also has a good
 * chance of dropping a bead the player still lacks, so the quest can always be finished in a sensible
 * number of kills (the plain table alone could keep repeating beads the player already has).
 */
public final class ImpCatcherBeads {
    public static final List<String> BEADS = List.of("black_bead", "red_bead", "yellow_bead", "white_bead");
    /** Monster id of the imp (npcs module, TownMonsters). */
    public static final String IMP = "imp";
    /** Quest bonus drop chance per imp kill while beads are still needed: 1 in this. */
    public static final int QUEST_DROP_ONE_IN = 2;

    private ImpCatcherBeads() {
    }

    /** True for the creatures that carry the beads (imps). */
    public static boolean carriesBeads(String monsterId, String name) {
        return IMP.equals(monsterId) || "Imp".equalsIgnoreCase(name);
    }

    /** The beads the player is still missing, in quest order. */
    public static List<String> missing(Predicate<String> has) {
        List<String> out = new ArrayList<>();
        for (String bead : BEADS) {
            if (!has.test(bead)) {
                out.add(bead);
            }
        }
        return out;
    }

    /**
     * The quest bead an imp kill drops, or null: with chance 1/{@link #QUEST_DROP_ONE_IN} ({@code roll}
     * is a uniform draw in [0, QUEST_DROP_ONE_IN)), one of the missing beads picked by {@code pick}
     * (any int; taken modulo the number missing). Nothing once all four are held.
     */
    public static String questDrop(Predicate<String> has, int roll, int pick) {
        if (roll != 0) {
            return null;
        }
        List<String> missing = missing(has);
        return missing.isEmpty() ? null : missing.get(Math.floorMod(pick, missing.size()));
    }
}
