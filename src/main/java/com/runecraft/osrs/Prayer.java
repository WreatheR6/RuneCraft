package com.runecraft.osrs;

/**
 * The standard prayer book. Drain effect values are the per-tick drain rates used by the game:
 * a counter accumulates the sum of active drain effects each tick and one prayer point is lost
 * whenever it exceeds the player's drain resistance ({@code 60 + 2 * prayer bonus}).
 */
public enum Prayer {
    THICK_SKIN("Thick Skin", 1, 1, Group.DEFENCE, 0, 0, 0.05),
    BURST_OF_STRENGTH("Burst of Strength", 4, 1, Group.STRENGTH, 0, 0.05, 0),
    CLARITY_OF_THOUGHT("Clarity of Thought", 7, 1, Group.ATTACK, 0.05, 0, 0),
    SHARP_EYE("Sharp Eye", 8, 1, Group.RANGED, 0, 0, 0),
    MYSTIC_WILL("Mystic Will", 9, 1, Group.MAGIC, 0, 0, 0),
    ROCK_SKIN("Rock Skin", 10, 6, Group.DEFENCE, 0, 0, 0.10),
    SUPERHUMAN_STRENGTH("Superhuman Strength", 13, 6, Group.STRENGTH, 0, 0.10, 0),
    IMPROVED_REFLEXES("Improved Reflexes", 16, 6, Group.ATTACK, 0.10, 0, 0),
    RAPID_RESTORE("Rapid Restore", 19, 1, Group.NONE, 0, 0, 0),
    RAPID_HEAL("Rapid Heal", 22, 2, Group.NONE, 0, 0, 0),
    PROTECT_ITEM("Protect Item", 25, 2, Group.NONE, 0, 0, 0),
    HAWK_EYE("Hawk Eye", 26, 6, Group.RANGED, 0, 0, 0),
    MYSTIC_LORE("Mystic Lore", 27, 6, Group.MAGIC, 0, 0, 0),
    STEEL_SKIN("Steel Skin", 28, 12, Group.DEFENCE, 0, 0, 0.15),
    ULTIMATE_STRENGTH("Ultimate Strength", 31, 12, Group.STRENGTH, 0, 0.15, 0),
    INCREDIBLE_REFLEXES("Incredible Reflexes", 34, 12, Group.ATTACK, 0.15, 0, 0),
    PROTECT_FROM_MAGIC("Protect from Magic", 37, 12, Group.OVERHEAD, 0, 0, 0),
    PROTECT_FROM_MISSILES("Protect from Missiles", 40, 12, Group.OVERHEAD, 0, 0, 0),
    PROTECT_FROM_MELEE("Protect from Melee", 43, 12, Group.OVERHEAD, 0, 0, 0),
    EAGLE_EYE("Eagle Eye", 44, 12, Group.RANGED, 0, 0, 0),
    MYSTIC_MIGHT("Mystic Might", 45, 12, Group.MAGIC, 0, 0, 0),
    RETRIBUTION("Retribution", 46, 3, Group.OVERHEAD, 0, 0, 0),
    REDEMPTION("Redemption", 49, 6, Group.OVERHEAD, 0, 0, 0),
    SMITE("Smite", 52, 18, Group.OVERHEAD, 0, 0, 0),
    PRESERVE("Preserve", 55, 2, Group.NONE, 0, 0, 0),
    CHIVALRY("Chivalry", 60, 24, Group.MELEE_ALL, 0.15, 0.18, 0.20),
    PIETY("Piety", 70, 24, Group.MELEE_ALL, 0.20, 0.23, 0.25),
    RIGOUR("Rigour", 74, 24, Group.RANGED_ALL, 0, 0, 0.25),
    AUGURY("Augury", 77, 24, Group.MAGIC_ALL, 0, 0, 0.25);

    /** Which prayers are mutually exclusive with each other. */
    public enum Group { NONE, ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC, OVERHEAD, MELEE_ALL, RANGED_ALL, MAGIC_ALL }

    private final String displayName;
    private final int level;
    private final int drain;
    private final Group group;
    private final double attack;
    private final double strength;
    private final double defence;

    Prayer(String displayName, int level, int drain, Group group, double attack, double strength, double defence) {
        this.displayName = displayName;
        this.level = level;
        this.drain = drain;
        this.group = group;
        this.attack = attack;
        this.strength = strength;
        this.defence = defence;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }

    public int drainEffect() {
        return drain;
    }

    public Group group() {
        return group;
    }

    public double attackMultiplier() {
        return 1.0 + attack;
    }

    public double strengthMultiplier() {
        return 1.0 + strength;
    }

    public double defenceMultiplier() {
        return 1.0 + defence;
    }

    public double rangedAttackMultiplier() {
        return switch (this) {
            case SHARP_EYE -> 1.05;
            case HAWK_EYE -> 1.10;
            case EAGLE_EYE -> 1.15;
            case RIGOUR -> 1.20;
            default -> 1.0;
        };
    }

    public double rangedStrengthMultiplier() {
        return switch (this) {
            case SHARP_EYE -> 1.05;
            case HAWK_EYE -> 1.10;
            case EAGLE_EYE -> 1.15;
            case RIGOUR -> 1.23;
            default -> 1.0;
        };
    }

    public double magicMultiplier() {
        return switch (this) {
            case MYSTIC_WILL -> 1.05;
            case MYSTIC_LORE -> 1.10;
            case MYSTIC_MIGHT -> 1.15;
            case AUGURY -> 1.25;
            default -> 1.0;
        };
    }

    /** Returns true if activating {@code this} should deactivate {@code other}. */
    public boolean conflictsWith(Prayer other) {
        if (other == this) {
            return false;
        }
        Group a = group;
        Group b = other.group;
        if (a == Group.NONE || b == Group.NONE) {
            return false;
        }
        if (a == b) {
            return true;
        }
        return touches(a, b) || touches(b, a);
    }

    private static boolean touches(Group all, Group single) {
        return switch (all) {
            case MELEE_ALL -> single == Group.ATTACK || single == Group.STRENGTH || single == Group.DEFENCE
                    || single == Group.RANGED || single == Group.MAGIC || single == Group.RANGED_ALL || single == Group.MAGIC_ALL;
            case RANGED_ALL -> single == Group.ATTACK || single == Group.STRENGTH || single == Group.DEFENCE
                    || single == Group.RANGED || single == Group.MAGIC || single == Group.MAGIC_ALL;
            case MAGIC_ALL -> single == Group.ATTACK || single == Group.STRENGTH || single == Group.DEFENCE
                    || single == Group.RANGED || single == Group.MAGIC;
            case RANGED, MAGIC -> single == Group.ATTACK || single == Group.STRENGTH || single == Group.RANGED
                    || single == Group.MAGIC;
            default -> false;
        };
    }

    public static Prayer byIndex(int i) {
        return values()[i];
    }
}
