package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Party size, mode and raid level scaling (pure maths, unit tested). The CoX, ToB and ToA hitpoint
 * formulas are the OSRS ones (as used by the wiki's DPS calculator, assuming a maxed party); other
 * values are approximations and are marked as such.
 */
public final class RaidScaling {
    /** CoX personal points cap (OSRS: 131,071). */
    public static final int COX_POINTS_CAP = 131_071;
    /** CoX team points per unique roll: a roll with this many points has its maximum 65.7% chance. */
    public static final int COX_POINTS_PER_ROLL = 570_000;
    /** Most unique rolls a CoX raid gets (one per 570,000 team points). // approx: the cap */
    public static final int COX_MAX_UNIQUES = 3;
    /** ToA personal points cap (OSRS: 64,000). */
    public static final int TOA_POINTS_CAP = 64_000;
    /** ToB points a surviving player earns per room (6 rooms = 18 points, as in OSRS). */
    public static final int TOB_POINTS_PER_ROOM = 3;
    /** ToB points lost per death (OSRS: 4). */
    public static final int TOB_DEATH_PENALTY = 4;
    /** Highest ToA path level (OSRS: 6). */
    public static final int TOA_MAX_PATH_LEVEL = 6;

    /** ToB Entry mode base hitpoints (OSRS entry-mode variants; 5-player values). */
    public static final Map<String, Integer> TOB_ENTRY_HP = Map.of(
            "the_maiden_of_sugadinti", 2000,
            "pestilent_bloat", 1280,
            "nylocas_vasilias", 1440,
            "sotetseg", 2240,
            "xarpus", 3400,
            "verzik_vitur", 1200,
            "verzik_vitur_p2", 1600,
            "verzik_vitur_p3", 2400);

    private RaidScaling() {
    }

    private static int clampParty(int partySize, int max) {
        return Math.max(1, Math.min(max, partySize));
    }

    /** Integer square root. */
    static int isqrt(int n) {
        int r = (int) Math.sqrt(Math.max(0, n));
        while (r * r > n) {
            r--;
        }
        while ((r + 1) * (r + 1) <= n) {
            r++;
        }
        return r;
    }

    // ------------------------------------------------------------------ Chambers of Xeric

    /**
     * CoX monster hitpoints multiplier (OSRS): {@code hp += hp * floor(partySize / 2)}, i.e. x1 solo,
     * x2 for 2-3 players, x3 for 4-5 and so on (party size capped at 100).
     */
    public static double coxHp(int partySize) {
        return 1 + clampParty(partySize, 100) / 2;
    }

    /** Party-size factor of the Great Olm's hitpoints: each extra phase (per 8 players) takes 3 players' worth. */
    public static int olmPartyFactor(int partySize) {
        int n = clampParty(partySize, 100);
        return Math.min(n - 1, 50) - 3 * (Math.min(n, 50) / 8);
    }

    /** Great Olm phases (OSRS): 3, plus one for every 8 players. */
    public static int olmPhases(int partySize) {
        return 3 + Math.min(clampParty(partySize, 100), 50) / 8;
    }

    /**
     * Scaled hitpoints of a CoX creature (OSRS formula for a maxed party). The Great Olm's head has
     * 800 + 400 per party factor and each claw 600 + 300, unaffected by Challenge Mode; everything
     * else is multiplied by {@link #coxHp(int)} and gets +50% in Challenge Mode (except Vasa's crystals).
     */
    public static int coxScaledHp(String id, int baseHp, int partySize, boolean challenge) {
        int n = clampParty(partySize, 100);
        if (id.equals("great_olm")) {
            return 800 + 400 * olmPartyFactor(n);
        }
        if (id.equals("olm_left_claw") || id.equals("olm_right_claw")) {
            return 600 + 300 * olmPartyFactor(n);
        }
        int hp = baseHp + baseHp * (n * 50 / 100);
        if (challenge && !id.equals("vasa_crystal")) {
            hp += hp * 50 / 100;
        }
        return Math.max(1, Math.min(30_000, hp));
    }

    /**
     * CoX offensive stat multiplier (OSRS): +7% per integer square root of the extra players plus 1%
     * per extra player, and +50% in Challenge Mode. Applied to the damage CoX creatures deal.
     */
    public static double coxOffence(int partySize, boolean challenge) {
        int m1 = clampParty(partySize, 100) - 1;
        double pct = (100 + isqrt(m1) * 7 + m1) / 100.0;
        return challenge ? pct * 1.5 : pct;
    }

    /** CoX purple chance of one roll: 1% per 8,676 points, capped at 570,000 points (65.7%). */
    public static double coxPurpleChance(int totalPoints) {
        return Math.min(Math.max(0, totalPoints), COX_POINTS_PER_ROLL) / 867_600.0;
    }

    /**
     * The chance of each CoX unique roll: team points are split into 570,000-point rolls (the last
     * one takes the remainder), up to {@link #COX_MAX_UNIQUES}.
     */
    public static List<Double> coxUniqueRolls(int totalPoints) {
        List<Double> rolls = new ArrayList<>();
        int left = Math.max(0, totalPoints);
        do {
            int chunk = Math.min(left, COX_POINTS_PER_ROLL);
            rolls.add(coxPurpleChance(chunk));
            left -= chunk;
        } while (left > 0 && rolls.size() < COX_MAX_UNIQUES);
        return rolls;
    }

    /** CoX: a death costs 40% of your personal points. */
    public static int coxDeathPenalty(int points) {
        return points - points * 40 / 100;
    }

    // ------------------------------------------------------------------ Theatre of Blood

    /** ToB boss hitpoints (OSRS): (party + 3) / 8 with the party clamped to 3-5, i.e. 100% / 87.5% / 75%. */
    public static double tobHp(int partySize) {
        int n = Math.max(3, Math.min(5, partySize));
        return (n + 3) / 8.0;
    }

    /**
     * Scaled ToB hitpoints. Normal and Hard mode use {@link #tobHp(int)}; Entry mode swaps in the
     * entry-mode base hitpoints and scales them by 10/19/27/34/40 fortieths for 1-5 players (OSRS).
     */
    public static int tobScaledHp(String id, int baseHp, int partySize, RaidMode mode) {
        if (mode == RaidMode.ENTRY) {
            int[] f = {10, 19, 27, 34, 40};
            // Creatures without a known entry-mode variant get 60% of their normal hitpoints. // approx
            int base = TOB_ENTRY_HP.getOrDefault(id, baseHp * 6 / 10);
            return Math.max(1, base * f[clampParty(partySize, 5) - 1] / 40);
        }
        return Math.max(1, (int) (baseHp * tobHp(partySize)));
    }

    /** ToB purple chance: 1/9.1 with every point, reduced proportionally by deaths (OSRS normal mode). */
    public static double tobPurpleChance(int teamPoints, int partySize) {
        return tobPurpleChance(teamPoints, partySize, RaidMode.NORMAL);
    }

    /** ToB purple chance by mode: 1/9.1 normal, 1/7.7 hard mode, none in entry mode (scaled by points kept). */
    public static double tobPurpleChance(int teamPoints, int partySize, RaidMode mode) {
        if (mode == RaidMode.ENTRY) {
            return 0;
        }
        int max = Math.max(1, partySize) * 6 * TOB_POINTS_PER_ROOM;
        double base = mode == RaidMode.HARD ? 1 / 7.7 : 1 / 9.1;
        return Math.max(0, Math.min(1.0, teamPoints / (double) max)) * base;
    }

    // ------------------------------------------------------------------ Tombs of Amascut

    /**
     * ToA hitpoints multiplier ignoring path level and rounding: +4% per 10 raid levels (truncated to
     * whole percent), x1.9 for 2 players, x2.8 for 3 and +0.6 for each further player (OSRS).
     */
    public static double toaHp(int raidLevel, int partySize) {
        int n = clampParty(partySize, 8);
        double party = 1.0 + 0.9 * Math.min(n - 1, 2) + 0.6 * Math.max(0, n - 3);
        return (1.0 + (4 * Math.max(0, raidLevel) / 10) / 100.0) * party;
    }

    /**
     * Exact ToA hitpoints (OSRS): raid level (+0.4% per level, truncated to whole percent), path level
     * for path monsters (+3% + 5% per level), party size, then rounded to 5 (over 100) or 10 (over 300).
     */
    public static int toaScaledHp(int baseHp, int raidLevel, int pathLevel, int partySize, boolean pathMonster) {
        int hp = baseHp;
        hp += hp * (4 * Math.max(0, raidLevel) / 10) / 100;
        int path = Math.max(0, Math.min(TOA_MAX_PATH_LEVEL, pathLevel));
        if (pathMonster && path >= 1) {
            hp = hp * (100 + 3 + 5 * path) / 100;
        }
        int n = clampParty(partySize, 8);
        if (n >= 2) {
            int factor = 9 * (n >= 3 ? 2 : 1);
            if (n >= 4) {
                factor += 6 * (n - 3);
            }
            hp = hp * (10 + factor) / 10;
        }
        if (hp > 100) {
            int roundTo = hp > 300 ? 10 : 5;
            hp = (hp + roundTo / 2) / roundTo * roundTo;
        }
        return Math.max(1, hp);
    }

    /** ToA boss damage multiplier: approximated as +1% per 10 raid levels (OSRS uses per-invocation effects). // approx */
    public static double toaDamage(int raidLevel) {
        return 1.0 + Math.max(0, raidLevel) / 1000.0;
    }

    /** Path level damage bonus of path monsters (OSRS): +8% at path level 1 and +5% per further level. */
    public static double toaPathMultiplier(int pathLevel) {
        int path = Math.max(0, Math.min(TOA_MAX_PATH_LEVEL, pathLevel));
        return path == 0 ? 1.0 : 1.0 + (3 + 5 * path) / 100.0;
    }

    /**
     * Path level of the next path (OSRS): Pathseeker/Pathfinder/Pathmaster level every path 1/2/3
     * times on entry, and with Walk the Path each completed path levels the remaining ones.
     */
    public static int toaPathLevel(Set<Invocation> invocations, int pathsCompleted) {
        int level = 0;
        if (invocations.contains(Invocation.PATHSEEKER)) {
            level = 1;
        } else if (invocations.contains(Invocation.PATHFINDER)) {
            level = 2;
        } else if (invocations.contains(Invocation.PATHMASTER)) {
            level = 3;
        }
        if (invocations.contains(Invocation.WALK_THE_PATH)) {
            level += Math.max(0, pathsCompleted);
        }
        return Math.min(TOA_MAX_PATH_LEVEL, level);
    }

    /** ToA's effective raid level for loot: levels above 400 count a third. */
    static double toaLootLevel(int raidLevel) {
        return Math.min(raidLevel, 400) + Math.max(0, raidLevel - 400) / 3.0;
    }

    /**
     * ToA purple chance (OSRS): points / (10,500 - 20 * (min(level, 400) + max(0, level - 400) / 3)) percent,
     * capped at 55%.
     */
    public static double toaPurpleChance(int totalPoints, int raidLevel) {
        double divisor = 10_500 - 20 * toaLootLevel(raidLevel);
        return Math.min(0.55, Math.max(0, totalPoints) / divisor / 100.0);
    }

    /** Tumeken's guardian chance: points / (350,000 - 700 * effective level) percent. // approx */
    public static double toaPetChance(int totalPoints, int raidLevel) {
        double divisor = 350_000 - 700 * toaLootLevel(raidLevel);
        return Math.min(1.0, Math.max(0, totalPoints) / divisor / 100.0);
    }

    /** ToA: a death costs 20% of your personal points (approximation of the OSRS death penalty). */
    public static int toaDeathPenalty(int points) {
        return points - points * 20 / 100;
    }
}
