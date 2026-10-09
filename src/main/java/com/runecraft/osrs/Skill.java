package com.runecraft.osrs;

/**
 * The 24 Old School RuneScape skills, in the order they appear in the in-game skills tab
 * (three columns, read left-to-right, top-to-bottom).
 */
public enum Skill {
    ATTACK("Attack", 0x9B2020, true),
    HITPOINTS("Hitpoints", 0xB01818, true),
    MINING("Mining", 0x5D7A7E, false),
    STRENGTH("Strength", 0x1E8C4A, true),
    AGILITY("Agility", 0x1D2F8C, false),
    SMITHING("Smithing", 0x6E6E5A, false),
    DEFENCE("Defence", 0x6C82C0, true),
    HERBLORE("Herblore", 0x0E8A12, false),
    FISHING("Fishing", 0x6A8AB8, false),
    RANGED("Ranged", 0x6D8B19, true),
    THIEVING("Thieving", 0x6C3C78, false),
    COOKING("Cooking", 0x6E2C8A, false),
    PRAYER("Prayer", 0xD8D8D8, true),
    CRAFTING("Crafting", 0x8C6E44, false),
    FIREMAKING("Firemaking", 0xE07818, false),
    MAGIC("Magic", 0x3A48C8, true),
    FLETCHING("Fletching", 0x038C8C, false),
    WOODCUTTING("Woodcutting", 0x7E6A36, false),
    RUNECRAFT("Runecraft", 0xAAAA88, false),
    SLAYER("Slayer", 0x3A3A3A, false),
    FARMING("Farming", 0x2E6E1E, false),
    CONSTRUCTION("Construction", 0xA89878, false),
    HUNTER("Hunter", 0x5C5838, false),
    SAILING("Sailing", 0x2A7AB0, false);

    private static final Skill[] VALUES = values();

    private final String displayName;
    private final int color;
    private final boolean combat;

    Skill(String displayName, int color, boolean combat) {
        this.displayName = displayName;
        this.color = color;
        this.combat = combat;
    }

    public String displayName() {
        return displayName;
    }

    /** RGB colour used for XP drops and the skills tab. */
    public int color() {
        return color;
    }

    public boolean isCombat() {
        return combat;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** New characters start with 10 Hitpoints and level 1 everywhere else. */
    public int startingXpTenths() {
        return this == HITPOINTS ? Experience.xpForLevel(10) * 10 : 0;
    }

    public static Skill byIndex(int index) {
        return VALUES[index];
    }

    public static int count() {
        return VALUES.length;
    }

    public static Skill byId(String id) {
        for (Skill s : VALUES) {
            if (s.id().equalsIgnoreCase(id) || s.displayName.equalsIgnoreCase(id)) {
                return s;
            }
        }
        return null;
    }
}
