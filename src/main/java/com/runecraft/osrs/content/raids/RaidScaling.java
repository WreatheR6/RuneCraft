package com.runecraft.osrs.content.raids;

/**
 * Party size and raid level scaling (pure maths, unit tested). Values follow OSRS where they are
 * documented and are approximations otherwise (noted per method).
 */
public final class RaidScaling {
    /** CoX personal points cap (OSRS: 131,071). */
    public static final int COX_POINTS_CAP = 131_071;
    /** ToA personal points cap (OSRS: 64,000). */
    public static final int TOA_POINTS_CAP = 64_000;
    /** ToB points a surviving player earns per room (6 rooms = 18 points, as in OSRS). */
    public static final int TOB_POINTS_PER_ROOM = 3;
    /** ToB points lost per death (OSRS: 4). */
    public static final int TOB_DEATH_PENALTY = 4;

    private RaidScaling() {
    }

    /**
     * CoX monster hitpoints multiplier. OSRS scales each monster by party size (and combat level);
     * approximated here as +50% per extra player.
     */
    public static double coxHp(int partySize) {
        return 1.0 + 0.5 * Math.max(0, partySize - 1);
    }

    /** ToB boss hitpoints: 100% for 5 players, 87.5% for 4, 75% for 1-3 (OSRS). */
    public static double tobHp(int partySize) {
        if (partySize >= 5) {
            return 1.0;
        }
        return partySize == 4 ? 0.875 : 0.75;
    }

    /**
     * ToA boss hitpoints: +2% per 5 raid levels (0.4% per level), +90% for each of the 2nd and 3rd
     * players and +60% for each further player (OSRS).
     */
    public static double toaHp(int raidLevel, int partySize) {
        int n = Math.max(1, partySize);
        double party = 1.0 + 0.9 * Math.min(n - 1, 2) + 0.6 * Math.max(0, n - 3);
        return (1.0 + 0.004 * Math.max(0, raidLevel)) * party;
    }

    /** ToA boss damage multiplier: approximated as +1% per 10 raid levels (OSRS uses per-invocation effects). */
    public static double toaDamage(int raidLevel) {
        return 1.0 + Math.max(0, raidLevel) / 1000.0;
    }

    /** CoX purple chance: 1% per 8,676 total points, capped at 570,000 points (65.7%). */
    public static double coxPurpleChance(int totalPoints) {
        return Math.min(Math.max(0, totalPoints), 570_000) / 867_600.0;
    }

    /** ToB purple chance: 1/9.1 with every point, reduced proportionally by deaths (OSRS normal mode). */
    public static double tobPurpleChance(int teamPoints, int partySize) {
        int max = Math.max(1, partySize) * 6 * TOB_POINTS_PER_ROOM;
        return Math.max(0, Math.min(1.0, teamPoints / (double) max)) / 9.1;
    }

    /**
     * ToA purple chance (OSRS): points / (10,500 - 20 * (min(level, 400) + max(0, level - 400) / 3)) percent,
     * capped at 55%.
     */
    public static double toaPurpleChance(int totalPoints, int raidLevel) {
        double scaled = Math.min(raidLevel, 400) + Math.max(0, raidLevel - 400) / 3.0;
        double divisor = 10_500 - 20 * scaled;
        return Math.min(0.55, Math.max(0, totalPoints) / divisor / 100.0);
    }

    /** CoX: a death costs 40% of your personal points. */
    public static int coxDeathPenalty(int points) {
        return points - points * 40 / 100;
    }

    /** ToA: a death costs 20% of your personal points (approximation of the OSRS death penalty). */
    public static int toaDeathPenalty(int points) {
        return points - points * 20 / 100;
    }
}
