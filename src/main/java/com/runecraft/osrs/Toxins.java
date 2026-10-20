package com.runecraft.osrs;

/**
 * OSRS poison and venom, with separate poison and venom immunity timers.
 *
 * <ul>
 *   <li>Poison hits every 30 game ticks (18 s) at its current strength, dropping by 1 every 5 hits.</li>
 *   <li>Venom hits every 30 ticks, starting at 6 and rising by 2 per hit up to 20. It overrides poison.</li>
 *   <li>Antipoisons cure poison and give poison immunity; drunk while envenomed they only turn the venom into
 *       ordinary poison (starting at 6 damage).</li>
 *   <li>Anti-venoms cure both and give venom immunity plus a longer poison immunity.</li>
 * </ul>
 */
public final class Toxins {
    public static final int HIT_INTERVAL = 30;
    public static final int VENOM_START = 6;
    public static final int VENOM_STEP = 2;
    public static final int VENOM_MAX = 20;
    /** Poison damage left when an antipoison converts venom. */
    public static final int VENOM_TO_POISON = 6;

    /** Immunity (game ticks) given by each cure: {poison, venom}. */
    public static final int[] ANTIPOISON = {150, 0}; // 90 s
    public static final int[] SUPERANTIPOISON = {600, 0}; // 6 min
    public static final int[] ANTIDOTE_PLUS = {900, 0}; // 9 min
    public static final int[] ANTIDOTE_PLUS_PLUS = {1200, 0}; // 12 min
    public static final int[] ANTI_VENOM = {1200, 60}; // 12 min poison, ~36 s venom (approx: wiki gives 36-54 s)
    public static final int[] ANTI_VENOM_PLUS = {1500, 360}; // 15 min poison, ~3.6 min venom
    public static final int[] EXTENDED_ANTI_VENOM_PLUS = {1770, 630}; // OSRS Wiki: 17.7 min poison, ~6.3 min venom

    /** What changed after a cure. */
    public enum Cure { NOTHING, CURED, VENOM_TO_POISON }

    /** One entity's toxin state. */
    public static final class State {
        int damage;
        boolean venom;
        int hits;
        long nextTick;
        long poisonImmuneUntil;
        long venomImmuneUntil;

        public boolean poisoned() {
            return damage > 0 && !venom;
        }

        public boolean envenomed() {
            return damage > 0 && venom;
        }

        public boolean active() {
            return damage > 0;
        }

        public int damage() {
            return damage;
        }

        public boolean poisonImmune(long tick) {
            return poisonImmuneUntil > tick;
        }

        public boolean venomImmune(long tick) {
            return venomImmuneUntil > tick;
        }

        /** True when nothing is going on and the state can be forgotten. */
        public boolean idle(long tick) {
            return damage <= 0 && poisonImmuneUntil <= tick && venomImmuneUntil <= tick;
        }

        /** An independent copy (the self-test saves a player's state and puts it back afterwards). */
        public State copy() {
            State c = new State();
            c.damage = damage;
            c.venom = venom;
            c.hits = hits;
            c.nextTick = nextTick;
            c.poisonImmuneUntil = poisonImmuneUntil;
            c.venomImmuneUntil = venomImmuneUntil;
            return c;
        }
    }

    private Toxins() {
    }

    /** Poisons at {@code damage} unless immune, envenomed or already poisoned at least as strongly. */
    public static boolean poison(State s, int damage, long tick) {
        if (damage <= 0 || s.poisonImmune(tick) || s.venom && s.damage > 0 || s.damage >= damage) {
            return false;
        }
        s.damage = damage;
        s.venom = false;
        s.hits = 0;
        s.nextTick = tick + HIT_INTERVAL;
        return true;
    }

    /** Envenoms (replacing poison) unless immune to venom or already envenomed. */
    public static boolean venom(State s, long tick) {
        if (s.venomImmune(tick) || s.venom && s.damage > 0) {
            return false;
        }
        s.damage = VENOM_START;
        s.venom = true;
        s.hits = 0;
        s.nextTick = tick + HIT_INTERVAL;
        return true;
    }

    /**
     * An antipoison-type cure ({@code venomToo = false}) or a full cure ({@code venomToo = true}, e.g. an
     * anti-venom or a restoration pool), granting the given immunities.
     */
    public static Cure cure(State s, boolean venomToo, int poisonImmunity, int venomImmunity, long tick) {
        Cure result = Cure.NOTHING;
        if (s.venom && s.damage > 0 && !venomToo) {
            // Antipoison on venom: turns it into ordinary poison and grants no immunity.
            s.venom = false;
            s.damage = VENOM_TO_POISON;
            s.hits = 0;
            return Cure.VENOM_TO_POISON;
        }
        if (s.damage > 0) {
            s.damage = 0;
            s.venom = false;
            s.hits = 0;
            result = Cure.CURED;
        }
        if (poisonImmunity > 0) {
            s.poisonImmuneUntil = Math.max(s.poisonImmuneUntil, tick + poisonImmunity);
        }
        if (venomImmunity > 0) {
            s.venomImmuneUntil = Math.max(s.venomImmuneUntil, tick + venomImmunity);
        }
        return result;
    }

    /** Advances one game tick; returns the damage of the hit landing now (0 if none). */
    public static int tick(State s, long tick) {
        if (s.damage <= 0 || tick < s.nextTick) {
            return 0;
        }
        int damage = s.damage;
        s.hits++;
        s.nextTick = tick + HIT_INTERVAL;
        if (s.venom) {
            s.damage = Math.min(VENOM_MAX, damage + VENOM_STEP);
        } else if (s.hits % 5 == 0) {
            s.damage = damage - 1;
        }
        return damage;
    }
}
