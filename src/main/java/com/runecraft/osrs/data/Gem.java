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

    /** Item left by a failed cut of a semi-precious gem. */
    public static final String CRUSHED_GEM = "crushed_gem";

    /**
     * Crafting level from which semi-precious gems can no longer be crushed (OSRS Wiki: the success
     * roll is overridden at 50 Crafting since the June 2025 Crafting changes; approx).
     */
    public static final int NO_CRUSH_LEVEL = 50;

    /** True if cutting this gem can fail and crush it (opal, jade and red topaz). */
    public boolean canCrush() {
        return crushLow() > 0;
    }

    /**
     * Cutting success "low" value out of 256 (OSRS Wiki uncut gem pages):
     * opal (L-1)*122/98+129, jade (L-1)*145/98+101, red topaz (L-1)*150/98+91, all over 256.
     */
    public int crushLow() {
        return switch (this) {
            case OPAL -> 128;
            case JADE -> 100;
            case RED_TOPAZ -> 90;
            default -> 0;
        };
    }

    /** Cutting success "high" value out of 256. */
    public int crushHigh() {
        return switch (this) {
            case OPAL -> 250;
            case JADE -> 245;
            case RED_TOPAZ -> 240;
            default -> 0;
        };
    }

    /** Crafting XP for crushing the gem (opal 3.8, jade 5, red topaz 6.3). */
    public double crushXp() {
        return switch (this) {
            case OPAL -> 3.8;
            case JADE -> 5.0;
            case RED_TOPAZ -> 6.3;
            default -> 0;
        };
    }

    /** Chance to cut the gem successfully at a Crafting level. */
    public double cutChance(int crafting) {
        if (!canCrush() || crafting >= NO_CRUSH_LEVEL) {
            return 1.0;
        }
        return com.runecraft.osrs.Chance.lerp(crafting, crushLow(), crushHigh());
    }
}
