package com.runecraft.osrs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * OSRS single-way combat rules. In a single-way area each fighter may only be in combat with one other: a
 * monster won't attack a player who was hit by (or hit) someone else in the last few ticks, and a player can't
 * attack a monster that is fighting someone else ("Someone else is fighting that.") or attack something new
 * while another fighter is still attacking them ("I'm already under attack."). Multi-way areas skip all of
 * this. Fighters are identified by an int key (the Minecraft entity id).
 */
public final class SingleCombat {
    /** Game ticks a hit keeps two fighters "in combat" with each other (NPC combat). */
    public static final int COMBAT_TICKS = 8;
    /** Player-vs-player combat lasts a little longer before someone else may attack. */
    public static final int PVP_COMBAT_TICKS = 10; // approx

    /** Why an attack is refused, or {@link #OK}. */
    public enum Verdict {
        OK(null),
        TARGET_BUSY("Someone else is fighting that."),
        ATTACKER_BUSY("I'm already under attack.");

        public final String message;

        Verdict(String message) {
            this.message = message;
        }
    }

    /** One fighter's most recent hits taken and dealt. */
    public static final class State {
        int lastAttacker = -1;
        long attackedAt = Long.MIN_VALUE / 2;
        int lastTarget = -1;
        long attackingAt = Long.MIN_VALUE / 2;

        public int lastAttacker() {
            return lastAttacker;
        }

        public long attackedAt() {
            return attackedAt;
        }
    }

    private final Map<Integer, State> states = new HashMap<>();

    /** Records that {@code attacker} hit (or splashed on) {@code target} on this game tick. */
    public synchronized void onHit(int attacker, int target, long tick) {
        State t = states.computeIfAbsent(target, k -> new State());
        t.lastAttacker = attacker;
        t.attackedAt = tick;
        State a = states.computeIfAbsent(attacker, k -> new State());
        a.lastTarget = target;
        a.attackingAt = tick;
    }

    public synchronized State state(int key) {
        return states.get(key);
    }

    public synchronized void forget(int key) {
        states.remove(key);
    }

    /** Drops fighters that have been out of combat for a while. */
    public synchronized void prune(long tick) {
        states.values().removeIf(s -> tick - Math.max(s.attackedAt, s.attackingAt) > 100);
    }

    /**
     * Whether {@code attacker} may attack {@code target} now. {@code active} tells whether a recorded fighter is
     * still around (alive and loaded); fighters that are gone never block anyone.
     */
    public synchronized Verdict check(int attacker, int target, long tick, boolean multi, boolean pvp, IntPredicate active) {
        if (multi) {
            return Verdict.OK;
        }
        int window = pvp ? PVP_COMBAT_TICKS : COMBAT_TICKS;
        State t = states.get(target);
        if (t != null) {
            if (busyWithOther(t.lastAttacker, t.attackedAt, attacker, tick, window, active)
                    || busyWithOther(t.lastTarget, t.attackingAt, attacker, tick, window, active)) {
                return Verdict.TARGET_BUSY;
            }
        }
        State a = states.get(attacker);
        if (a != null && busyWithOther(a.lastAttacker, a.attackedAt, target, tick, window, active)) {
            return Verdict.ATTACKER_BUSY;
        }
        return Verdict.OK;
    }

    /**
     * The check in a singles-plus area (wiki "Singles-plus combat"): single-way, but a player attacking a player only counts
     * the players either is fighting, so a player fighting a monster can still be attacked by another player (whose attack
     * takes priority) and can hit back. Monsters keep the single-way rules. {@code player} tells which fighters are players.
     */
    public Verdict checkSinglesPlus(int attacker, int target, long tick, boolean pvp, IntPredicate active, IntPredicate player) {
        if (!pvp) {
            return check(attacker, target, tick, false, false, active);
        }
        return check(attacker, target, tick, false, true, id -> player.test(id) && (active == null || active.test(id)));
    }

    private static boolean busyWithOther(int other, long at, int self, long tick, int window, IntPredicate active) {
        return other >= 0 && other != self && tick - at < window && (active == null || active.test(other));
    }
}
