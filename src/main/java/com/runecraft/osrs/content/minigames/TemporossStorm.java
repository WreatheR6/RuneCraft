package com.runecraft.osrs.content.minigames;

/**
 * Tempoross' energy and essence, harpoonfish catching and cooking, and the torrent attack (OSRS Wiki "Tempoross",
 * "Fishing spot (Tempoross Cove)", "Harpoonfish", "Totem pole", "Fire (Tempoross Cove)", "Harpoonfish cannon").
 */
public final class TemporossStorm {
    private TemporossStorm() {
    }

    /**
     * Energy each player adds (hitsplat units): about 260 solo, falling linearly to about 180 a player with 40 players
     * (Mod Squid: "2.6k (1 player) - 1.8k (40 players)", hitsplats are x10 behind the scenes).
     */
    public static int energy(int players) {
        return pool(players, 260, 180);
    }

    /** Essence each player adds: about 250 solo, 170 a player with 40 players. */
    public static int essence(int players) {
        return pool(players, 250, 170);
    }

    private static int pool(int players, int solo, int forty) {
        int n = Math.max(1, Math.min(40, players));
        double each = solo - (solo - forty) * (n - 1) / 39.0;
        return (int) Math.round(n * each);
    }

    /** A raw harpoonfish fired from a cannon drains 10 energy, a cooked one 15. */
    public static final int RAW_ENERGY = 10;
    public static final int COOKED_ENERGY = 15;

    /** Harpoonfish catch chance: 170/256 at level 1 to 255/256 at 99 with a normal harpoon (78.13% at 35). */
    public static final int CATCH_LOW = 170;
    public static final int CATCH_HIGH = 255;
    /** Game ticks between catch attempts (the ordinary harpoon cadence; approx: not documented for harpoonfish). */
    public static final int CATCH_TICKS = 5;
    /** Harpoonfish cook in 3 ticks for a flat 10 Cooking XP and never burn. */
    public static final int COOK_TICKS = 3;
    public static final double COOK_XP = 10;

    /**
     * Essence one spirit pool scatter removes. The wiki says no player hits above 12 whatever their level; the scaling
     * below that is approx: up to max(4, min(12, level / 8)), at least half of it.
     */
    public static int scatterMax(int fishing) {
        return Math.max(4, Math.min(12, fishing / 8));
    }

    /** One scatter (and 55 points) every 4 ticks, each restoring 3% run energy. */
    public static final int SCATTER_TICKS = 4;

    /** Repairing a broken totem pole or mast gives 4 x Construction level XP. */
    public static double repairXp(int construction) {
        return 4.0 * construction;
    }

    /** Each fire spreads at most twice before it stabilises. */
    public static final int FIRE_SPREADS = 2;

    // ---- the essence phase and the storm (OSRS Wiki "Tempoross", "Tempoross/Strategies") ----

    /**
     * Game ticks Tempoross stays submerged with the spirit pool open while its energy recharges from 0% to full: about 18
     * spirit pool scatters (one every 4 ticks) per the strategy guide (approx; it was a fixed 25 ticks here).
     */
    public static final int ESSENCE_PHASE_TICKS = 72;
    /** Energy (percent) the energy-phase/essence-phase cycle can be restored: three times, then the storm is out of control. */
    public static final int MAX_RECOVERIES = 3;
    /**
     * The storm intensity bar, the energy phase's failure timer: the encounter fails when it reaches 100%, and it resets to 0%
     * whenever Tempoross submerges (OSRS Wiki "Tempoross"; the dev blog calls it the "phase one failure timer"), so each
     * energy phase has the whole bar. It builds while Tempoross is up: approx 1% every 4 ticks, 400 ticks (4 minutes) a
     * phase, sized from Tempoross/Strategies, where a solo player catches, cooks and loads about 36 harpoonfish in phase one
     * and must be loading by 93-95%, leaving 5-7% of the bar for the run from the shrine and the loading. Once Tempoross has
     * gathered too much energy (after its third recovery) it rises rapidly (approx 4% a tick, 25 ticks from empty) unless both
     * cannons force him under again at once.
     */
    public static final int STORM_TICKS_PER_PERCENT = 4;
    public static final int ENRAGED_STORM_PER_TICK = 4;
    /** Ticks of an energy phase before the storm bar fills: {@code 100 x} {@link #STORM_TICKS_PER_PERCENT}. */
    public static final int STORM_PHASE_TICKS = 100 * STORM_TICKS_PER_PERCENT;

    /** Energy (in hitsplat units) back after {@code ticksUnder} of the essence phase's {@link #ESSENCE_PHASE_TICKS}. */
    public static int recharged(int maxEnergy, long ticksUnder) {
        return (int) Math.min(maxEnergy, Math.max(0, maxEnergy * ticksUnder / ESSENCE_PHASE_TICKS));
    }

    /** Storm intensity (percent) when Tempoross submerges: the bar resets to 0% for the next energy phase (OSRS Wiki). */
    public static final int STORM_AFTER_SUBMERGE = 0;

    /** Storm intensity (percent) gained in one game tick: slow while Tempoross is up, fast once enraged, none while under. */
    public static int stormGain(boolean submerged, boolean enraged, long ticks) {
        if (submerged) {
            return 0;
        }
        if (enraged) {
            return ENRAGED_STORM_PER_TICK;
        }
        return ticks % STORM_TICKS_PER_PERCENT == 0 ? 1 : 0;
    }

    /**
     * Torrent attack: a charged torrent of water surges towards a cannon, disabling it for a while and stunning anyone
     * loading it. Interval, warning and durations are approx (the wiki gives none).
     */
    public static final int TORRENT_INTERVAL = 50;
    public static final int TORRENT_WARNING = 3;
    public static final int TORRENT_DISABLE_TICKS = 10;
    public static final int TORRENT_STUN_TICKS = 4;
}
