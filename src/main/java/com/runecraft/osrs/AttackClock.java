package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.List;

/**
 * The player attack clock in real time (round 41, CONTENT_GUIDE rule 5). OSRS starts an attack only on a game tick and
 * then waits the weapon's speed in ticks before the next one; a click in between queues the attack. Minecraft has no
 * game-tick boundaries a player can feel, so the server keeps one clock in Minecraft ticks (MC, 50 ms):
 * <ul>
 *   <li>an attack resolves no earlier than {@code lastAttack + period}, where the period is that of the attack that
 *       stamped {@code lastAttack} (speed in ticks x {@value #MC_PER_GAME_TICK} MC, chosen when the attack starts, as in
 *       OSRS);</li>
 *   <li>a click before that is buffered in one slot (a newer click replaces it) and fires on the exact ready tick, so
 *       attacks per second stay exactly 1 / (speed x 0.6 s) and DPS stays maxHit x accuracy / (speed x 0.6 s);</li>
 *   <li>a buffered click whose target stays invalid (out of reach, out of sight) for {@value #PENDING_GRACE_MC} MC past
 *       the ready tick is dropped.</li>
 * </ul>
 * {@code // playability}: OSRS resolves on 0.6 s ticks and queues one action; here the queue is one slot that fires on the
 * first MC tick the attack is allowed. {@link Buffer} is the exact logic the server's {@code combat.AttackTimer} runs, so
 * the unit tests of this class test the game.
 */
public final class AttackClock {
    public static final int MC_PER_GAME_TICK = 12;
    /** MC ticks a buffered click may wait past its ready tick for a valid target before it is dropped. */
    public static final int PENDING_GRACE_MC = 10;
    /** Auto-retaliate only swings at a target within this many degrees of where the player looks. */
    public static final double RETALIATE_FACING_DEGREES = 75.0;
    /** Auto-retaliate ends once its target stayed out of reach (plus a block) for longer than this many MC ticks. */
    public static final int WALK_AWAY_MC = 20;
    /** Hold-to-attack (client): the attack key held on a monster sends the click when the clock is this close to ready. */
    public static final int HOLD_SEND_EARLY_MC = 4;
    /** A held bow fires once drawn this many MC ticks (and the clock is ready); a tap fires from this draw on release. */
    public static final int HOLD_DRAW_MC = 4;
    public static final int TAP_DRAW_MC = 3;
    /** Vanilla's base attack speed (attacks per second) that the ATTACK_SPEED modifiers add to. */
    public static final double VANILLA_BASE_ATTACK_SPEED = 4.0;

    /** What an attack is; the ordinal is sent to the client (append new values last). */
    public enum Kind {
        MELEE, SPELL, RANGED, THROWN
    }

    private AttackClock() {
    }

    /** First MC tick the next attack may resolve. */
    public static long readyAt(long lastAttack, long periodMc) {
        return lastAttack + periodMc;
    }

    public static boolean ready(long now, long readyAt) {
        return now >= readyAt;
    }

    /** True once a buffered click has waited more than {@link #PENDING_GRACE_MC} past {@code readyAt} without resolving. */
    public static boolean pendingExpired(long now, long readyAt) {
        return now > readyAt + PENDING_GRACE_MC;
    }

    /** An attack speed in OSRS game ticks as MC ticks between attack starts. */
    public static int periodMc(int ticks) {
        return ticks * MC_PER_GAME_TICK;
    }

    /** Ranged attack speed in ticks: the weapon's speed, one tick faster on Rapid, never below 1 (OSRS). */
    public static int rangedSpeed(int weaponTicks, boolean rapid) {
        return Math.max(1, weaponTicks - (rapid ? 1 : 0));
    }

    /**
     * The vanilla ATTACK_SPEED modifier (added to the base 4 attacks per second) whose cooldown indicator fills in exactly
     * {@code ticks} OSRS game ticks: 1 / (ticks x 0.6 s) attacks per second. The same formula as ModItems' weapons.
     */
    public static float vanillaAttackSpeedModifier(int ticks) {
        return -4.0f + 1.0f / (ticks * 0.6f);
    }

    /** MC ticks vanilla's attack indicator takes to fill at this ATTACK_SPEED attribute value (attacks per second). */
    public static double vanillaPeriodMc(double attackSpeed) {
        return attackSpeed <= 0 ? Double.POSITIVE_INFINITY : 20.0 / attackSpeed;
    }

    /** Fraction of the cooldown that has passed (the timer HUD's fill), 0..1. */
    public static float fill(long remainingMc, long periodMc) {
        if (periodMc <= 0 || remainingMc <= 0) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, 1f - remainingMc / (float) periodMc));
    }

    /**
     * True when the horizontal direction to a target ({@code dx, dz}) lies within {@code maxDegrees} of the horizontal look
     * direction ({@code lookX, lookZ}). A target straight above or below counts as faced.
     */
    public static boolean facing(double lookX, double lookZ, double dx, double dz, double maxDegrees) {
        double look = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double to = Math.sqrt(dx * dx + dz * dz);
        if (look < 1e-6 || to < 1e-6) {
            return true;
        }
        double cos = (lookX * dx + lookZ * dz) / (look * to);
        return cos >= Math.cos(Math.toRadians(maxDegrees)) - 1e-9;
    }

    /** What a buffered click does on one MC tick. */
    public enum Step {
        /** Nothing is buffered. */
        IDLE,
        /** Still waiting for the ready tick or for the target to become valid. */
        WAIT,
        /** Resolve it now (the buffer is consumed). */
        FIRE,
        /** The target stayed invalid past the grace: dropped. */
        DROP
    }

    /**
     * One player's clock state apart from {@code lastAttack} itself (PlayerData.lastAttackTick, which boss scripts and
     * self-tests also write): the period of the attack that stamped it and the one buffered click.
     */
    public static final class Buffer {
        private int periodMc = -1;
        private boolean pending;
        private Kind pendingKind;
        private int pendingTarget = -1;
        private long queuedAt;

        /** The stamping attack's period, or {@code fallbackPeriodMc} (the wielded weapon's) when none was recorded. */
        public int periodMc(int fallbackPeriodMc) {
            return periodMc > 0 ? periodMc : fallbackPeriodMc;
        }

        public long readyAt(long lastAttack, int fallbackPeriodMc) {
            return AttackClock.readyAt(lastAttack, periodMc(fallbackPeriodMc));
        }

        public boolean hasPending() {
            return pending;
        }

        public Kind pendingKind() {
            return pendingKind;
        }

        /** Entity id of the buffered click's target, or -1 (a bow shot needs none). */
        public int pendingTarget() {
            return pendingTarget;
        }

        public long queuedAt() {
            return queuedAt;
        }

        /**
         * A click at {@code now}: true when the attack may resolve right now (the caller resolves it), false when it was
         * buffered instead, replacing any older buffered click.
         */
        public boolean click(long now, long lastAttack, int fallbackPeriodMc, Kind kind, int targetId) {
            if (ready(now, readyAt(lastAttack, fallbackPeriodMc))) {
                clearPending();
                return true;
            }
            pending = true;
            pendingKind = kind;
            pendingTarget = targetId;
            queuedAt = now;
            return false;
        }

        /** One MC tick for the buffered click; {@code valid} says whether its target can be hit from here right now. */
        public Step tick(long now, long lastAttack, int fallbackPeriodMc, boolean valid) {
            if (!pending) {
                return Step.IDLE;
            }
            long readyAt = readyAt(lastAttack, fallbackPeriodMc);
            if (!ready(now, readyAt)) {
                return Step.WAIT;
            }
            if (valid) {
                clearPending();
                return Step.FIRE;
            }
            if (pendingExpired(now, Math.max(readyAt, queuedAt))) {
                clearPending();
                return Step.DROP;
            }
            return Step.WAIT;
        }

        /** An attack resolved with this period (the caller stamps lastAttack = now): records it and consumes the buffer. */
        public void accept(int periodMc) {
            this.periodMc = periodMc;
            clearPending();
        }

        public void clearPending() {
            pending = false;
            pendingKind = null;
            pendingTarget = -1;
        }
    }

    /**
     * A small model of one player for tests: clicks and MC ticks go through the same {@link Buffer} the server uses, and
     * every resolution stamps {@code lastAttack = now} with the period of the weapon held at that moment.
     */
    public static final class Sim {
        public final Buffer buffer = new Buffer();
        public long lastAttack = Long.MIN_VALUE / 4;
        /** Period of the weapon currently held (an attack takes it when it resolves). */
        public int weaponPeriodMc;
        public final List<Long> resolved = new ArrayList<>();

        public Sim(int weaponPeriodMc) {
            this.weaponPeriodMc = weaponPeriodMc;
        }

        public boolean click(long now) {
            if (buffer.click(now, lastAttack, weaponPeriodMc, Kind.MELEE, 1)) {
                resolve(now);
                return true;
            }
            return false;
        }

        public Step tick(long now, boolean valid) {
            Step s = buffer.tick(now, lastAttack, weaponPeriodMc, valid);
            if (s == Step.FIRE) {
                resolve(now);
            }
            return s;
        }

        public Step tick(long now) {
            return tick(now, true);
        }

        private void resolve(long now) {
            lastAttack = now;
            buffer.accept(weaponPeriodMc);
            resolved.add(now);
        }
    }
}
