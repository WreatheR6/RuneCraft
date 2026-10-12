package com.runecraft.osrs;

import java.util.HashMap;
import java.util.Map;

/**
 * Forestry-style tree depletion timers (OSRS, June 2023). Each tree has a timer of
 * {@code despawnTicks} game ticks that counts down while at least one player is chopping it
 * (several players do not speed it up) and regenerates at the same rate while nobody is. When it
 * has run out, the tree falls on the next log received.
 *
 * <p>Callers report every woodcutting roll with {@link #chop}; rolls arrive every few game ticks
 * while a player keeps chopping, so a gap longer than {@link #CHOP_GAP} ticks counts as idle time.
 */
public final class TreeTimers {
    /** A pause between two rolls longer than this (in game ticks) means nobody was chopping. */
    public static final int CHOP_GAP = 8;
    /** Woodcutting rolls every 4 game ticks. */
    public static final int ROLL_TICKS = 4;

    /** key -> {remaining ticks, game tick of the last roll}. */
    private final Map<String, long[]> timers = new HashMap<>();

    /**
     * Records a woodcutting roll on a tree at {@code gameTick}. Returns true when the tree's timer
     * has run out, i.e. the next log should fell it. {@code despawnTicks <= 0} always returns true
     * (normal trees fall after one log).
     */
    public boolean chop(String key, int despawnTicks, long gameTick) {
        if (despawnTicks <= 0) {
            return true;
        }
        long[] t = timers.get(key);
        if (t == null) {
            timers.put(key, new long[]{Math.max(0, despawnTicks - ROLL_TICKS), gameTick});
            return false;
        }
        long elapsed = Math.max(0, gameTick - t[1]);
        if (elapsed <= CHOP_GAP) {
            t[0] -= elapsed;
        } else {
            // Nobody was chopping for most of the gap: regenerate, then count this roll's own ticks.
            t[0] = Math.min(despawnTicks, t[0] + (elapsed - ROLL_TICKS)) - ROLL_TICKS;
        }
        t[0] = Math.max(0, Math.min(despawnTicks, t[0]));
        t[1] = gameTick;
        return t[0] <= 0;
    }

    /** Ticks left on a tree's timer (the full {@code despawnTicks} for an untouched tree). */
    public long remaining(String key, int despawnTicks) {
        long[] t = timers.get(key);
        return t == null ? despawnTicks : t[0];
    }

    /** Forgets a tree's timer (call when it falls). */
    public void reset(String key) {
        timers.remove(key);
    }
}
