package com.runecraft.osrs.data;

/** Gems: cutting level/XP (Crafting) and value. */
public enum Gem {
    OPAL("opal", "Opal", 1, 15.0, 0xE0E0F0, 15),
    JADE("jade", "Jade", 13, 20.0, 0x50C060, 30),
    RED_TOPAZ("red_topaz", "Red topaz", 16, 25.0, 0xE06050, 40),
    SAPPHIRE("sapphire", "Sapphire", 20, 50.0, 0x3050E0, 50),
    EMERALD("emerald", "Emerald", 27, 67.5, 0x30C050, 100),
    RUBY("ruby", "Ruby", 34, 85.0, 0xE02030, 200),
    DIAMOND("diamond", "Diamond", 43, 107.5, 0xF0F0FF, 400),
    DRAGONSTONE("dragonstone", "Dragonstone", 55, 137.5, 0xB040E0, 2000),
    ONYX("onyx", "Onyx", 67, 167.5, 0x303030, 20000),
    ZENYTE("zenyte", "Zenyte", 89, 200.0, 0xF0A020, 40000);

    private final String id;
    private final String name;
    private final int level;
    private final double xp;
    private final int color;
    private final int value;

    Gem(String id, String name, int level, double xp, int color, int value) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.color = color;
        this.value = value;
    }

    public String cutItem() {
        return id;
    }

    public String uncutItem() {
        return "uncut_" + id;
    }

    public String displayName() {
        return name;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public int color() {
        return color;
    }

    public int value() {
        return value;
    }
}
