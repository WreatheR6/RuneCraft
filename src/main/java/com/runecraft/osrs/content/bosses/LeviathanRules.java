package com.runecraft.osrs.content.bosses;

/**
 * The Leviathan (OSRS Wiki "The Leviathan", "The Leviathan/Strategies") as pure rules for its fight script.
 *
 * <ul>
 *   <li>Volleys of orbs (blue magic 32, green ranged 24, orange melee 50; protection prayers block them fully), each volley
 *   faster than the last: 3-tick magic/ranged, 2-tick magic/ranged, 2-tick all three, 1-tick magic/ranged, 1-tick all
 *   three, then all three with less time to react (2 ticks, then 1). After each volley it roars (up to 7) and debris falls
 *   (up to 25; one on the player stays as a blocker, five in a + awakened), then it alternates its specials: Lightning
 *   Barrage (a current chasing the player, orbs across the arena) and Smoke Blast (ten debris on the player, then a blast of
 *   up to 59, awakened 70, for anyone it can see). It takes 33% less damage during a special.</li>
 *   <li>Melee can't reach the lure. A shadow spell during a volley stuns it for 15 ticks (awakened 7): hits are capped at 10,
 *   except on the gaping wound on its back, where ranged hits for at least 65% of the max and magic for double; that breaks
 *   the stun, starts a special in retaliation and resets the volleys to the second speed on the first stun (the third on
 *   the second...). Dealing a quarter of its health during a stun resets them too.</li>
 *   <li>Below 20% it enrages: attacks every 2 ticks (awakened 1), magic and ranged only, debris keeps falling, and an abyssal
 *   pathfinder circles the arena: inside its 5x5 (awakened 7x7) the player's ranged attacks never miss and hit for at least
 *   20% of the max (magic for double); outside, its attacks pierce prayer and the player's deal less.</li>
 * </ul>
 * approx: volley lengths, the barrage's and the debris' timings, the pathfinder's speed and how much less damage the player
 * deals outside it.
 */
public final class LeviathanRules {
    private LeviathanRules() {
    }

    /** Ticks between the orbs of each volley stage (OSRS). */
    private static final int[] SPACING = {3, 2, 2, 1, 1, 1, 1};
    /** Whether a stage mixes in the melee orb. */
    private static final boolean[] MELEE = {false, false, true, false, true, true, true};
    /** Ticks the player has to react to an orb (its flight time). approx: 3 until the last two stages */
    private static final int[] REACT = {3, 3, 3, 3, 3, 2, 1};
    public static final int STAGES = SPACING.length;
    /**
     * Real-time floors: orbs at least 2 ticks (24 Minecraft ticks) apart and at least 3 ticks (1.8 s) in flight, so each
     * prayer switch can be made in time. // playability: OSRS speeds the volleys up to 1-tick orbs with 1-2 ticks to react,
     * a prayer flick a tick; the orb counts, styles, max hits and the speed-up order stay
     */
    public static final int MIN_SPACING = 2;
    public static final int MIN_REACT = 3;

    public static final int ROAR_MAX = 7;
    public static final int DEBRIS_MAX = 25;
    public static final int SMOKE_BLAST_MAX = 59;
    public static final int SMOKE_BLAST_MAX_AWAKENED = 70;
    public static final int SMOKE_DEBRIS = 10;
    public static final int BARRAGE_TICKS = 10;
    public static final int STUN_TICKS = 15;
    public static final int STUN_TICKS_AWAKENED = 7;
    public static final int STUNNED_CAP = 10;
    public static final double WOUND_RANGED_MIN = 0.65;
    public static final double SPECIAL_DAMAGE = 0.67;
    public static final double ENRAGE_PERCENT = 20;
    public static final double PATHFINDER_MIN = 0.20;
    /** The pathfinder's area: 5x5 (awakened 7x7) as half-widths in blocks. approx: blocks for tiles */
    public static final double PATHFINDER_HALF = 2.5;
    public static final double PATHFINDER_HALF_AWAKENED = 3.5;
    /** Outside the pathfinder the player's attacks deal less: halved. approx */
    public static final double OUTSIDE_PATHFINDER = 0.5;

    /** Ticks between orbs as played: the OSRS spacing, floored at {@link #MIN_SPACING}. // playability */
    public static int spacing(int stage, boolean enraged, boolean awakened) {
        return Math.max(MIN_SPACING, osrsSpacing(stage, enraged, awakened));
    }

    /** The OSRS spacing: 3, 2, 2, 1, 1, 1, 1 by stage; enraged every 2 ticks (awakened 1). */
    public static int osrsSpacing(int stage, boolean enraged, boolean awakened) {
        if (enraged) {
            return awakened ? 1 : 2;
        }
        return SPACING[clamp(stage)];
    }

    public static boolean melee(int stage, boolean enraged) {
        return !enraged && MELEE[clamp(stage)];
    }

    /** Ticks an orb flies as played: the OSRS reaction time, floored at {@link #MIN_REACT}. // playability */
    public static int react(int stage, boolean enraged) {
        return Math.max(MIN_REACT, osrsReact(stage, enraged));
    }

    /** The OSRS reaction time: 3 until the last two stages (2, then 1); 2 enraged. */
    public static int osrsReact(int stage, boolean enraged) {
        return enraged ? 2 : REACT[clamp(stage)];
    }

    /** The next volley's stage after {@code stage}. */
    public static int next(int stage) {
        return clamp(stage + 1);
    }

    /** The stage the volleys reset to after the {@code n}th (from 1) stun that hit the wound (the 2nd speed on the first...). */
    public static int afterStun(int n) {
        return clamp(n);
    }

    private static int clamp(int stage) {
        return Math.max(0, Math.min(STAGES - 1, stage));
    }

    /** A hit on the Leviathan while it is stunned: capped at 10 unless it found the gaping wound. */
    public static int stunnedHit(int damage, boolean wound, boolean magic, int maxHit) {
        if (!wound) {
            return Math.min(damage, STUNNED_CAP);
        }
        if (magic) {
            return damage * 2;
        }
        return Math.max(damage, (int) Math.ceil(WOUND_RANGED_MIN * maxHit));
    }

    /** A player's hit in the enrage, by whether they stand in the pathfinder's area. */
    public static int enragedHit(int damage, boolean inside, boolean magic, int maxHit) {
        if (!inside) {
            return (int) Math.floor(damage * OUTSIDE_PATHFINDER);
        }
        if (magic) {
            return damage * 2;
        }
        return Math.max(damage, (int) Math.ceil(PATHFINDER_MIN * maxHit));
    }

    /** Whether a quarter of its hitpoints was dealt during one stun (that resets the volleys too). */
    public static boolean quarterDealt(int dealtDuringStun, int hitpoints) {
        return dealtDuringStun * 4 >= hitpoints;
    }
}
