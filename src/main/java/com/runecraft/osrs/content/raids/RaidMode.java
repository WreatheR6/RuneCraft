package com.runecraft.osrs.content.raids;

/**
 * Raid difficulty modes. CoX has Normal and Challenge Mode; ToB has Entry, Normal and Hard mode.
 * ToA expresses difficulty through its raid level instead and always runs in {@link #NORMAL}.
 */
public enum RaidMode {
    NORMAL("Normal"),
    /** CoX Challenge Mode: every creature but the Great Olm has +50% hitpoints and combat stats. */
    CHALLENGE("Challenge Mode"),
    /** ToB Entry mode: weaker monsters, no uniques and no pet. */
    ENTRY("Entry mode"),
    /** ToB Hard mode: better purple chance and pet rate. */
    HARD("Hard mode");

    private final String displayName;

    RaidMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether a raid offers this mode. */
    public boolean availableFor(RaidType type) {
        return switch (this) {
            case NORMAL -> true;
            case CHALLENGE -> type == RaidType.COX;
            case ENTRY, HARD -> type == RaidType.TOB;
        };
    }

    public static RaidMode byName(String name) {
        for (RaidMode m : values()) {
            if (m.name().equalsIgnoreCase(name) || (m == CHALLENGE && name.equalsIgnoreCase("cm"))
                    || (m == HARD && name.equalsIgnoreCase("hm"))) {
                return m;
            }
        }
        return null;
    }
}
