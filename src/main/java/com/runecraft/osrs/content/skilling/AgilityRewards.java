package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.data.AgilityCourse;

import java.util.List;

/**
 * Course rewards beyond experience and Marks of grace:
 * <ul>
 *   <li><b>Prifddinas</b> (the Hefin district course): a crystal shard every lap (once from the
 *       course's shortcut portal; a guaranteed lap reward since the 2024 rework).</li>
 *   <li><b>Colossal Wyrm Remains</b>: termites scooped up during a lap, spent at Worm Tongue's
 *       stall: an amylase pack (100 amylase crystals) for 100, or the Varlamore Graceful recolour
 *       (a full Graceful set needed) for 650.</li>
 *   <li><b>Wilderness</b>: tagging the Agility dispenser after a lap gives a Wilderness agility
 *       ticket (exchanged there for Agility XP: 200 each, up to 230 each when handing in 101 or
 *       more). Paying the dispenser 150,000 coins starts a lap streak whose laps also give loot,
 *       doubled from lap 16, tripled from 31 and quadrupled from 61.</li>
 *   <li><b>Werewolf</b>: the Agility Trainer's stick, carried round and handed in at the end of
 *       the lap, for 380 XP ({@link AgilityCourse#STICK_XP}).</li>
 * </ul>
 */
public final class AgilityRewards {
    public static final String TERMITES = "termites";
    public static final String WILDERNESS_TICKET = "wilderness_agility_ticket";
    public static final String WORM_TONGUE = "worm_tongue";
    /** The Wilderness Agility dispenser's entry fee (coins). */
    public static final int WILDERNESS_FEE = 150_000;
    public static final int AMYLASE_PACK_TERMITES = 100;
    public static final int AMYLASE_PACK_CRYSTALS = 100;
    public static final int VARLAMORE_RECOLOUR_TERMITES = 650;

    private AgilityRewards() {
    }

    /** Crystal shards given for a Prifddinas lap (approx.: one, as from the old portal). */
    public static int crystalShards(AgilityCourse course) {
        return course == AgilityCourse.PRIFDDINAS ? 1 : 0;
    }

    /** Termites scooped up in one lap: {min, max} (approx., from players' reports); {0, 0} elsewhere. */
    public static int[] termites(AgilityCourse course) {
        return switch (course) {
            case COLOSSAL_WYRM_BASIC -> new int[]{4, 7};
            case COLOSSAL_WYRM_ADVANCED -> new int[]{8, 11};
            default -> new int[]{0, 0};
        };
    }

    /** Agility XP per Wilderness agility ticket when handing in {@code count} at once. */
    public static double ticketXp(int count) {
        if (count >= 101) {
            return 230;
        }
        if (count >= 51) {
            return 220; // approx. tier
        }
        if (count >= 11) {
            return 210; // approx. tier
        }
        return 200;
    }

    /** Laps taken off the Wilderness lap streak for logging out inside the course (25 before 2023). */
    public static final int LOGOUT_LAP_PENALTY = 10;

    /**
     * The Wilderness course's lap streak after logging out in the course: the fee stays paid, but ten laps
     * are lost. (The fee itself is lost on death or on leaving the course.)
     */
    public static int streakAfterLogout(int streak) {
        return Math.max(0, streak - LOGOUT_LAP_PENALTY);
    }

    /** Loot multiplier on the {@code streak}-th lap after paying the Wilderness fee. */
    public static int streakMultiplier(int streak) {
        if (streak >= 61) {
            return 4;
        }
        if (streak >= 31) {
            return 3;
        }
        if (streak >= 16) {
            return 2;
        }
        return 1;
    }

    /**
     * One roll of the Wilderness lap loot (amounts multiplied by the streak multiplier). The real
     * table's blighted supplies are not in the game, so food and potions stand in for them (approx.;
     * about 6,500 coins of value a lap at 1x, as on the wiki).
     */
    public static final List<Loot> WILDERNESS_LOOT = List.of(
            Loot.of("coins", 3000, 6000, 30), Loot.of("raw_shark", 2, 4, 15), Loot.of("anglerfish", 2, 3, 10),
            Loot.of("super_restore", 1, 2, 10), Loot.of("rune_scimitar", 1, 1, 8), Loot.of("rune_full_helm", 1, 1, 8),
            Loot.of("rune_platebody", 1, 1, 4), Loot.of("death_rune", 20, 40, 10), Loot.of("blood_rune", 10, 25, 5));
}
