package com.runecraft.osrs;

/**
 * Attack styles and the invisible level bonus / XP split each one grants.
 * Melee: Accurate +3 Attack, Aggressive +3 Strength, Defensive +3 Defence, Controlled +1 to all.
 * Ranged: Accurate +3 Ranged, Rapid attacks one tick faster, Longrange +3 Defence.
 */
public enum CombatStyle {
    ACCURATE("Accurate", false),
    AGGRESSIVE("Aggressive", false),
    CONTROLLED("Controlled", false),
    DEFENSIVE("Defensive", false),
    RANGED_ACCURATE("Accurate", true),
    RAPID("Rapid", true),
    LONGRANGE("Longrange", true);

    private final String displayName;
    private final boolean ranged;

    CombatStyle(String displayName, boolean ranged) {
        this.displayName = displayName;
        this.ranged = ranged;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isRanged() {
        return ranged;
    }

    public int attackBonus() {
        return switch (this) {
            case ACCURATE -> 3;
            case CONTROLLED -> 1;
            default -> 0;
        };
    }

    public int strengthBonus() {
        return switch (this) {
            case AGGRESSIVE -> 3;
            case CONTROLLED -> 1;
            default -> 0;
        };
    }

    public int defenceBonus() {
        return switch (this) {
            case DEFENSIVE, LONGRANGE -> 3;
            case CONTROLLED -> 1;
            default -> 0;
        };
    }

    public int rangedBonus() {
        return this == RANGED_ACCURATE ? 3 : 0;
    }

    /** Converts a melee style to its ranged counterpart and vice-versa, keeping intent. */
    public CombatStyle forRanged() {
        if (ranged) {
            return this;
        }
        return switch (this) {
            case DEFENSIVE -> LONGRANGE;
            case AGGRESSIVE -> RAPID;
            default -> RANGED_ACCURATE;
        };
    }

    public CombatStyle forMelee() {
        if (!ranged) {
            return this;
        }
        return switch (this) {
            case LONGRANGE -> DEFENSIVE;
            case RAPID -> AGGRESSIVE;
            default -> ACCURATE;
        };
    }
}
