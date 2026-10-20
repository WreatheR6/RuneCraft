package com.runecraft.osrs.content.minigames;

/**
 * Barbarian Assault's ten waves (OSRS Wiki): how many Penance of each type spawn at first and how many are held in reserve
 * (one comes out each time one of its type dies), every type's stats per wave (wave 10 uses wave 7's), the Penance Queen,
 * her spawn and the wave timers. Diffed against {@code src/test/resources/osrs/minigames/ba_waves.tsv} (BaWavesTest).
 */
public final class BaWaves {
    public static final int WAVES = 10;
    /** Penance types, in the order of {@link MinigameRules.BarbarianAssault#PENANCE}. */
    public static final int FIGHTER = 0, RANGER = 1, RUNNER = 2, HEALER = 3;
    public static final int TYPES = 4;

    /** Combat stats of one Penance creature. */
    public record Stats(int combat, int hitpoints, int attack, int strength, int defence, int ranged, int maxHit) {
    }

    /** [wave - 1][type] = {initial, reserve} (Barbarian Assault/Strategies, wave information). */
    private static final int[][][] COUNTS = {
            {{2, 2}, {2, 2}, {2, 0}, {2, 0}},
            {{2, 3}, {3, 1}, {2, 1}, {3, 0}},
            {{5, 0}, {3, 3}, {2, 2}, {2, 1}},
            {{5, 1}, {3, 3}, {3, 1}, {3, 1}},
            {{3, 3}, {5, 1}, {4, 1}, {4, 1}},
            {{5, 1}, {5, 2}, {4, 2}, {4, 2}},
            {{5, 2}, {6, 1}, {5, 1}, {4, 3}},
            {{7, 0}, {5, 3}, {5, 2}, {5, 2}},
            {{6, 2}, {7, 1}, {5, 4}, {6, 2}},
            {{5, 2}, {6, 1}, {5, 1}, {4, 3}},
    };

    /** Waves 1-9 (wave 10 = wave 7): combat, hitpoints, attack, strength, defence, ranged, max hit (each Penance page). */
    private static final Stats[] FIGHTERS = {
            new Stats(30, 28, 26, 26, 25, 1, 4), new Stats(32, 29, 28, 29, 27, 1, 3), new Stats(37, 32, 33, 32, 34, 1, 4),
            new Stats(42, 37, 36, 38, 37, 1, 5), new Stats(47, 38, 41, 43, 44, 1, 5), new Stats(56, 49, 50, 48, 48, 1, 6),
            new Stats(61, 50, 56, 55, 52, 1, 6), new Stats(68, 55, 60, 61, 62, 1, 7), new Stats(77, 56, 71, 73, 66, 1, 8)};
    private static final Stats[] RANGERS = {
            new Stats(21, 20, 22, 1, 21, 23, 3), new Stats(25, 28, 27, 1, 29, 24, 4), new Stats(32, 29, 32, 1, 33, 34, 4),
            new Stats(38, 34, 41, 1, 42, 40, 5), new Stats(43, 41, 44, 1, 46, 45, 6), new Stats(51, 50, 51, 1, 54, 52, 6),
            new Stats(57, 50, 63, 1, 61, 62, 7), new Stats(64, 54, 69, 1, 68, 70, 8), new Stats(72, 58, 79, 1, 80, 78, 9)};
    /** Runners are non-combat with 5 hitpoints in every wave. */
    private static final Stats RUNNER_STATS = new Stats(0, 5, 1, 1, 1, 1, 0);
    private static final Stats[] HEALERS = {
            new Stats(0, 27, 9, 8, 9, 1, 0), new Stats(0, 32, 13, 15, 14, 1, 0), new Stats(0, 37, 20, 18, 19, 1, 0),
            new Stats(0, 43, 22, 23, 21, 1, 0), new Stats(0, 49, 26, 27, 25, 1, 0), new Stats(0, 55, 30, 32, 28, 1, 0),
            new Stats(0, 60, 33, 36, 34, 1, 0), new Stats(0, 67, 40, 38, 36, 1, 0), new Stats(0, 76, 40, 44, 38, 1, 0)};

    /** The Penance Queen: 250 hitpoints, a ranged volley of up to 13 at every player and a melee hit of up to 5. */
    public static final Stats QUEEN = new Stats(209, 250, 260, 92, 132, 116, 13);
    public static final int QUEEN_MELEE_MAX_HIT = 5;
    /** Queen spawns: level 63, 45 hitpoints, melee up to 7; anyone may fight them with any style. */
    public static final Stats QUEEN_SPAWN = new Stats(63, 45, 60, 62, 50, 1, 7);

    /** Penance healers poison a player for 2. */
    public static final int HEALER_POISON = 2;

    // ------------------------------------------------------------------ timers (game ticks)

    /** A new Penance of a type comes out every 6 seconds while its type has initial ones left to spawn. */
    public static final int SPAWN_INTERVAL_TICKS = 10;
    /** The calls change every 30 seconds. */
    public static final int CALL_TICKS = 50;
    /** The next wave starts by itself 60 seconds after the last one ended (the leader's ladder starts it at once). */
    public static final int NEXT_WAVE_TICKS = 100;
    /** Wave 10: the game checks every 10 ticks whether every Penance is dead... */
    public static final int QUEEN_CHECK_TICKS = 10;
    /** ...and the queen comes out of the trapdoor 25 ticks later... */
    public static final int QUEEN_EMERGE_TICKS = 25;
    /** ...or 4 minutes into the wave, whichever comes first (Penance Queen page). */
    public static final int QUEEN_TIMEOUT_TICKS = 400;
    /** The queen's unblockable volley at every player, every 15 ticks (9 seconds). */
    public static final int QUEEN_VOLLEY_TICKS = 15;
    /** A queen spawn every 6 seconds, at most four alive. */
    public static final int QUEEN_SPAWN_TICKS = 10;
    public static final int QUEEN_SPAWN_MAX = 4;
    /** A yellow egg next to the queen every 6 seconds. */
    public static final int YELLOW_EGG_TICKS = 10;

    private BaWaves() {
    }

    private static int row(int wave) {
        return Math.max(1, Math.min(WAVES, wave)) - 1;
    }

    /** Penance of {@code type} out at the start of {@code wave}. */
    public static int initial(int wave, int type) {
        return COUNTS[row(wave)][type][0];
    }

    /** Penance of {@code type} held in reserve, one released each time one of its type dies. */
    public static int reserve(int wave, int type) {
        return COUNTS[row(wave)][type][1];
    }

    /** Every Penance of {@code type} the wave holds. */
    public static int total(int wave, int type) {
        return initial(wave, type) + reserve(wave, type);
    }

    /** Stats of a Penance type in a wave (wave 10 uses wave 7's, as on the wiki). */
    public static Stats stats(int wave, int type) {
        int w = row(wave) == WAVES - 1 ? 6 : row(wave);
        switch (type) {
            case FIGHTER:
                return FIGHTERS[w];
            case RANGER:
                return RANGERS[w];
            case RUNNER:
                return RUNNER_STATS;
            default:
                return HEALERS[w];
        }
    }
}
