package com.runecraft.osrs.data;

/**
 * Fish: fishing level/XP, catch-rate low/high values out of 256 (the OSRS Wiki's skilling success
 * chart data; harpoon fish use the post-2023 rates) and cooking data
 * (level, XP, heal amount and the level at which you stop burning on a fire).
 */
public enum Fish {
    SHRIMPS("shrimps", "Shrimps", 1, 10.0, 48, 256, 1, 30.0, 3, 34, 0xE89070, 5),
    SARDINE("sardine", "Sardine", 5, 20.0, 32, 192, 1, 40.0, 4, 38, 0x8098A8, 10),
    HERRING("herring", "Herring", 10, 30.0, 24, 128, 5, 50.0, 5, 41, 0x7A8A9A, 15),
    ANCHOVIES("anchovies", "Anchovies", 15, 40.0, 24, 128, 1, 30.0, 1, 34, 0x5A6A8A, 15),
    TROUT("trout", "Trout", 20, 50.0, 32, 192, 15, 70.0, 7, 50, 0xC09070, 20),
    PIKE("pike", "Pike", 25, 60.0, 16, 96, 20, 80.0, 8, 64, 0x8A9A6A, 25),
    SALMON("salmon", "Salmon", 30, 70.0, 16, 96, 25, 90.0, 9, 58, 0xE07050, 50),
    TUNA("tuna", "Tuna", 35, 80.0, 10, 86, 30, 100.0, 10, 64, 0x7A7AA0, 100),
    LOBSTER("lobster", "Lobster", 40, 90.0, 6, 95, 40, 120.0, 12, 74, 0xC03020, 200),
    SWORDFISH("swordfish", "Swordfish", 50, 100.0, 5, 64, 45, 140.0, 14, 86, 0x6A5AA0, 400),
    MONKFISH("monkfish", "Monkfish", 62, 120.0, 48, 90, 62, 150.0, 16, 92, 0x9A7A5A, 300),
    SHARK("shark", "Shark", 76, 110.0, 4, 54, 80, 210.0, 20, 100, 0x8A8A9A, 800),
    ANGLERFISH("anglerfish", "Anglerfish", 82, 120.0, 3, 36, 84, 230.0, 22, 100, 0x6A3A5A, 1200),
    DARK_CRAB("dark_crab", "Dark crab", 85, 130.0, 3, 40, 90, 215.0, 22, 100, 0x3A2A4A, 1000);

    private final String id;
    private final String name;
    private final int level;
    private final double xp;
    private final int low;
    private final int high;
    private final int cookLevel;
    private final double cookXp;
    private final int heal;
    private final int stopBurn;
    private final int color;
    private final int value;

    Fish(String id, String name, int level, double xp, int low, int high, int cookLevel, double cookXp,
         int heal, int stopBurn, int color, int value) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.low = low;
        this.high = high;
        this.cookLevel = cookLevel;
        this.cookXp = cookXp;
        this.heal = heal;
        this.stopBurn = stopBurn;
        this.color = color;
        this.value = value;
    }

    public String rawItem() {
        return "raw_" + id;
    }

    public String rawName() {
        return "Raw " + name.toLowerCase(java.util.Locale.ROOT);
    }

    public String cookedItem() {
        return id;
    }

    public String cookedName() {
        return name;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    public int cookLevel() {
        return cookLevel;
    }

    public double cookXp() {
        return cookXp;
    }

    public int heal() {
        return heal;
    }

    /** Level at which you stop burning on a fire (100 means you can always burn it). */
    public int stopBurn() {
        return stopBurn;
    }

    /**
     * Stop-burn level with cooking gauntlets (Family Crest), or 0 when they make no difference:
     * lobster 64, swordfish 81, monkfish 90, shark 94, anglerfish 98 (approx).
     */
    public int gauntletStopBurn() {
        return switch (this) {
            case LOBSTER -> 64;
            case SWORDFISH -> 81;
            case MONKFISH -> 90;
            case SHARK -> 94;
            case ANGLERFISH -> 98;
            default -> 0;
        };
    }

    public int color() {
        return color;
    }

    public int value() {
        return value;
    }
}
