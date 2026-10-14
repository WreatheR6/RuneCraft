package com.runecraft.osrs.content.bosses;

/**
 * Skotizo's fight rules (OSRS wiki, "Skotizo/Strategies" and "Dark Ankou"): Skotizo randomly awakens the four altars
 * around his lair; each awakened altar cuts the damage he takes by 25% (15% with Arclight or Emberlight), up to 100%
 * (60%). Arclight and Emberlight disable an altar in one hit. After two thirds of his hitpoints are gone he summons
 * three reanimated demon spawns (at most three at a time), and while an altar is active a random timer summons a dark
 * ankou (one at a time).
 */
public final class SkotizoRules {
    private SkotizoRules() {
    }

    public static final int ALTARS = 4;
    /** Reanimated demon spawns per summon, and at most this many at once. */
    public static final int DEMON_SPAWNS = 3;
    /** Skotizo summons the spawns once below a third of his hitpoints. */
    public static final int SPAWN_BELOW_PERCENT = 33;
    /** Game ticks between Skotizo awakening altars. // approx: "randomly at any time" */
    public static final int ALTAR_MIN_TICKS = 20;
    public static final int ALTAR_MAX_TICKS = 50;
    /** The dark ankou timer that starts while an altar is active. // approx: Mod Ash, "a random duration" */
    public static final int ANKOU_MIN_TICKS = 25;
    public static final int ANKOU_MAX_TICKS = 75;
    /** Game ticks between re-summons of slain spawns. // approx */
    public static final int RESUMMON_TICKS = 30;

    /** Weapons that disable an altar in one hit and only suffer 15% per altar (not Silverlight/Darklight). */
    public static boolean altarBreaker(String weaponId) {
        return "arclight".equals(weaponId) || "emberlight".equals(weaponId);
    }

    /** Fraction of a hit's damage that reaches Skotizo with {@code awake} altars active. */
    public static double damageFactor(int awake, boolean altarBreaker) {
        int n = Math.max(0, Math.min(ALTARS, awake));
        return Math.max(0.0, 1.0 - n * (altarBreaker ? 0.15 : 0.25));
    }

    /** A hit's damage after the altar reduction (rounded down). */
    public static int damage(int damage, int awake, boolean altarBreaker) {
        return (int) Math.floor(damage * damageFactor(awake, altarBreaker) + 1e-9);
    }
}
