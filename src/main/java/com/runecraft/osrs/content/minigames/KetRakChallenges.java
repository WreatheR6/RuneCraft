package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TzHaar-Ket-Rak's Challenges (OSRS Wiki, "TzHaar-Ket-Rak's Challenges"): a solo minigame in the inner city of Mor Ul Rek
 * (TzHaar-Ket-Rak sits at 2545, 5110, south of the bank). Challenge N pits the player against N JalTok-Jads at once (the
 * Inferno's Jad, 350 hitpoints, max hit 113, healed by Yt-HurKot below half hitpoints). Challenges 1 and 2 are open to anyone
 * who can enter the inner city; challenges 3 to 6 and the final endurance challenge (an endless stream of JalTok-Jads: each
 * one that dies is replaced) need an Inferno completion. Completions pay tokkul, best times go on the scoreboard beside him,
 * and completing all six unlocks the "Metamorphosis" of the TzRek-Jad pet into JalRek-Jad.
 */
public final class KetRakChallenges {
    private KetRakChallenges() {
    }

    /** The numbered challenges. */
    public static final int CHALLENGES = 6;
    /** The final (endurance) challenge, stored as stage 7. */
    public static final int FINAL = 7;
    /** Tokkul paid for completing challenges 1-6 (OSRS Wiki "Rewards"). */
    private static final int[] TOKKUL = {80, 190, 480, 1190, 2970, 7430};
    /**
     * Yt-HurKot each JalTok-Jad summons at half hitpoints, healing like the Inferno's (untagged and adjacent only, OSRS Wiki
     * "TzTok-Jad"). // approx: the wiki gives no count for the challenges; the Inferno's multi-Jad wave (68) and Zuk's Jad
     * summon three.
     */
    public static final int HEALERS_PER_JAD = 3;
    /** Game ticks between the challenge starting and the Jads emerging. // approx */
    public static final int START_DELAY = 3;
    /** Game ticks between two Jads joining in the final challenge after one dies. // approx */
    public static final int FINAL_RESPAWN_DELAY = 5;

    /** TzHaar-Ket-Rak's tile (and the scoreboard in the wall beside him). */
    public static final int KET_RAK_X = 2545;
    public static final int KET_RAK_Y = 5110;

    /** Minigame stats (in {@code MinigameData}). */
    public static final String KC_PREFIX = "kc:tzhaar_ket_rak_";
    public static final String BEST_PREFIX = "ketrak_best_";
    public static final String DEATHS_PREFIX = "ketrak_deaths_";
    /** Endurance record: most JalTok-Jads killed in one final challenge. */
    public static final String FINAL_RECORD = "ketrak_final_record";
    /** The last challenge attempted: + its number when completed, - its number when failed (for "It Wasn't a Fluke"). */
    public static final String LAST_RESULT = "ketrak_last_result";
    /** Flag set once all six challenges have been completed (JalRek-Jad metamorphosis). */
    public static final String METAMORPHOSIS = "jalrek_jad_unlocked";

    /** Combat Achievement flags reported for a run. */
    public static final String FLAG_EMPTY_INVENTORY = "noinv";
    public static final String FLAG_MULTI_STYLE = "multistyle";
    public static final String FLAG_FLUKE = "fluke";

    /** Tokkul for completing a challenge (0 for the final challenge, which can't be completed). */
    public static int tokkul(int challenge) {
        return challenge >= 1 && challenge <= CHALLENGES ? TOKKUL[challenge - 1] : 0;
    }

    /** JalTok-Jads fought at once: the challenge number (six at a time in the final challenge). */
    public static int jads(int challenge) {
        return Math.min(challenge, CHALLENGES);
    }

    /** Challenges 3+ (and the final one) need an Inferno completion. */
    public static boolean requiresInferno(int challenge) {
        return challenge >= 3;
    }

    /** Entrance block of a challenge: the minigame's own entrance is the first challenge. */
    public static String entrance(int challenge) {
        if (challenge <= 1) {
            return Minigame.TZHAAR_KET_RAK.entranceBlock();
        }
        return challenge == FINAL ? "ket_rak_final_challenge_entrance" : "ket_rak_challenge_" + challenge + "_entrance";
    }

    /** Extra entrance blocks (challenges 2-6 and the final challenge). */
    public static List<String> extraEntrances() {
        List<String> out = new ArrayList<>();
        for (int c = 2; c <= FINAL; c++) {
            out.add(entrance(c));
        }
        return out;
    }

    /** The challenge an entrance block starts (1 for anything unknown). */
    public static int challengeOf(String entranceBlock) {
        for (int c = 2; c <= FINAL; c++) {
            if (entrance(c).equals(entranceBlock)) {
                return c;
            }
        }
        return 1;
    }

    public static String name(int challenge) {
        return challenge == FINAL ? "TzHaar-Ket-Rak's final challenge" : "TzHaar-Ket-Rak's " + ordinal(challenge) + " challenge";
    }

    public static String ordinal(int challenge) {
        return switch (challenge) {
            case 1 -> "first";
            case 2 -> "second";
            case 3 -> "third";
            case 4 -> "fourth";
            case 5 -> "fifth";
            case 6 -> "sixth";
            default -> "final";
        };
    }

    /** Whether a completion time beats the stored personal best (0 = none yet). */
    public static boolean newBest(int storedBestTicks, long ticks) {
        return storedBestTicks <= 0 || ticks < storedBestTicks;
    }

    /** "It Wasn't a Fluke": the sixth challenge right after completing the fifth, with no failure in between. */
    public static boolean fluke(int lastResult, int challenge) {
        return challenge == 6 && lastResult == 5;
    }

    /** Whether every one of the six challenges has been completed. */
    public static boolean allComplete(java.util.function.IntUnaryOperator completions) {
        for (int c = 1; c <= CHALLENGES; c++) {
            if (completions.applyAsInt(c) <= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * "Multi-Style Specialist": every JalTok-Jad was attacked with a single combat style (melee, ranged or magic) and no two
     * Jads with the same one. {@code stylesPerJad} holds the style classes used on each Jad of the challenge.
     */
    public static boolean multiStyle(Collection<Set<String>> stylesPerJad, int jads) {
        if (stylesPerJad.size() != jads) {
            return false;
        }
        Set<String> used = new HashSet<>();
        for (Set<String> styles : stylesPerJad) {
            if (styles.size() != 1 || !used.add(styles.iterator().next())) {
                return false;
            }
        }
        return true;
    }

    /** Style class of an attack type name (STAB/SLASH/CRUSH are melee). */
    public static String styleClass(String attackType) {
        String t = attackType.toUpperCase(Locale.ROOT);
        return t.equals("RANGED") ? "ranged" : t.equals("MAGIC") ? "magic" : "melee";
    }
}
