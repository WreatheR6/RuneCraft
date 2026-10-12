package com.runecraft.osrs.data;

import com.runecraft.osrs.Rune;

/**
 * Runecrafting altars: level, XP per essence and the levels at which you craft extra runes per
 * essence. Cosmic and above require pure essence. Blood runes use the True Blood Altar value for
 * pure essence (10.5); soul runes keep the Arceuus soul altar value (approx, since the mod binds pure
 * essence rather than dark essence fragments).
 */
public enum RuneAltar {
    AIR(Rune.AIR, 1, 5.0, false, 11, 22, 33, 44, 55, 66, 77, 88, 99),
    MIND(Rune.MIND, 2, 5.5, false, 14, 28, 42, 56, 70, 84, 98),
    WATER(Rune.WATER, 5, 6.0, false, 19, 38, 57, 76, 95),
    EARTH(Rune.EARTH, 9, 6.5, false, 26, 52, 78),
    FIRE(Rune.FIRE, 14, 7.0, false, 35, 70),
    BODY(Rune.BODY, 20, 7.5, false, 46, 92),
    COSMIC(Rune.COSMIC, 27, 8.0, true, 59),
    CHAOS(Rune.CHAOS, 35, 8.5, true, 74),
    ASTRAL(Rune.ASTRAL, 40, 8.7, true, 82),
    NATURE(Rune.NATURE, 44, 9.0, true, 91),
    LAW(Rune.LAW, 54, 9.5, true),
    DEATH(Rune.DEATH, 65, 10.0, true),
    WRATH(Rune.WRATH, 95, 8.0, true),
    BLOOD(Rune.BLOOD, 77, 10.5, true),
    SOUL(Rune.SOUL, 90, 29.7, true);

    private final Rune rune;
    private final int level;
    private final double xp;
    private final boolean pureOnly;
    private final int[] multiples;

    RuneAltar(Rune rune, int level, double xp, boolean pureOnly, int... multiples) {
        this.rune = rune;
        this.level = level;
        this.xp = xp;
        this.pureOnly = pureOnly;
        this.multiples = multiples;
    }

    public Rune rune() {
        return rune;
    }

    public String blockId() {
        return rune.name().toLowerCase(java.util.Locale.ROOT) + "_altar";
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public boolean pureOnly() {
        return pureOnly;
    }

    /** Runes crafted per essence at a given level. */
    public int runesPerEssence(int runecraftLevel) {
        int count = 1;
        for (int m : multiples) {
            if (runecraftLevel >= m) {
                count++;
            }
        }
        return count;
    }

    public static RuneAltar of(Rune rune) {
        for (RuneAltar a : values()) {
            if (a.rune == rune) {
                return a;
            }
        }
        return null;
    }
}
