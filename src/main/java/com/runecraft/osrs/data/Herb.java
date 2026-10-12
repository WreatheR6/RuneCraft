package com.runecraft.osrs.data;

/** Herbs: cleaning level/XP (Herblore) and herb patch data (Farming plant/harvest XP). */
public enum Herb {
    GUAM("guam_leaf", "Guam leaf", 3, 2.5, 9, 11.0, 12.5, 0x5A9A3A, 3),
    MARRENTILL("marrentill", "Marrentill", 5, 3.8, 14, 13.5, 15.0, 0x3A8A5A, 5),
    TARROMIN("tarromin", "Tarromin", 11, 5.0, 19, 16.0, 18.0, 0x6A9A2A, 11),
    HARRALANDER("harralander", "Harralander", 20, 6.3, 26, 21.5, 24.0, 0x7A8A2A, 20),
    RANARR("ranarr_weed", "Ranarr weed", 25, 7.5, 32, 27.0, 30.5, 0x2A7A2A, 25),
    TOADFLAX("toadflax", "Toadflax", 30, 8.0, 38, 34.0, 38.5, 0x8A9A3A, 30),
    IRIT("irit_leaf", "Irit leaf", 40, 8.8, 44, 43.0, 48.5, 0x9AAA3A, 40),
    AVANTOE("avantoe", "Avantoe", 48, 10.0, 50, 54.5, 61.5, 0x4A8A6A, 48),
    KWUARM("kwuarm", "Kwuarm", 54, 11.3, 56, 69.0, 78.0, 0x6A7A2A, 54),
    SNAPDRAGON("snapdragon", "Snapdragon", 59, 11.8, 62, 87.5, 98.5, 0x3A6A2A, 59),
    CADANTINE("cadantine", "Cadantine", 65, 12.5, 67, 106.5, 120.0, 0x5A7A4A, 65),
    LANTADYME("lantadyme", "Lantadyme", 67, 13.1, 73, 134.5, 151.5, 0x7A9A5A, 67),
    DWARF_WEED("dwarf_weed", "Dwarf weed", 70, 13.8, 79, 170.5, 192.0, 0x5A8A2A, 70),
    TORSTOL("torstol", "Torstol", 75, 15.0, 85, 199.5, 224.5, 0x9A8A3A, 75),
    /** Varlamore herb (grown in herb patches at Farming 65); cleaning XP approx. Keep last: patch states store the ordinal. */
    HUASCA("huasca", "Huasca", 58, 11.8, 65, 86.0, 110.0, 0x6A9A4A, 58);

    private final String id;
    private final String name;
    private final int cleanLevel;
    private final double cleanXp;
    private final int farmingLevel;
    private final double plantXp;
    private final double harvestXp;
    private final int color;
    private final int value;

    Herb(String id, String name, int cleanLevel, double cleanXp, int farmingLevel, double plantXp, double harvestXp,
         int color, int value) {
        this.id = id;
        this.name = name;
        this.cleanLevel = cleanLevel;
        this.cleanXp = cleanXp;
        this.farmingLevel = farmingLevel;
        this.plantXp = plantXp;
        this.harvestXp = harvestXp;
        this.color = color;
        this.value = value;
    }

    public String cleanItem() {
        return id;
    }

    public String grimyItem() {
        return "grimy_" + id;
    }

    /** Base name used by seeds and unfinished potions, e.g. "guam", "ranarr", "dwarf_weed". */
    private String base() {
        return this == RANARR ? "ranarr" : id.replace("_leaf", "");
    }

    public String seedItem() {
        return base() + "_seed";
    }

    public String unfinishedPotion() {
        return base() + "_potion_unf";
    }

    public String displayName() {
        return name;
    }

    public String shortName() {
        return this == RANARR ? "Ranarr" : name.replace(" leaf", "");
    }

    public int cleanLevel() {
        return cleanLevel;
    }

    public double cleanXp() {
        return cleanXp;
    }

    /**
     * Herblore level to make this herb's unfinished potion: the level of the first potion it is
     * used in (e.g. tarromin 12 for strength potions, ranarr 30 for defence potions), as in-game.
     */
    public int unfLevel() {
        return switch (this) {
            case GUAM -> 3;
            case MARRENTILL -> 5;
            case TARROMIN -> 12;
            case HARRALANDER -> 22;
            case RANARR -> 30;
            case TOADFLAX -> 34;
            case IRIT -> 45;
            case AVANTOE -> 50;
            case KWUARM -> 55;
            case SNAPDRAGON -> 63;
            case CADANTINE -> 66;
            case LANTADYME -> 69;
            case DWARF_WEED -> 72;
            case TORSTOL -> 78;
            case HUASCA -> 58;
        };
    }

    public int farmingLevel() {
        return farmingLevel;
    }

    /** Harvest lives: chance (out of 256, before the +1) to save a life at Farming level 1 (OSRS wiki). */
    public int harvestLow() {
        return switch (this) {
            case GUAM -> 25;
            case MARRENTILL -> 28;
            case TARROMIN -> 31;
            case HARRALANDER -> 36;
            case RANARR -> 39;
            case TOADFLAX -> 43;
            case IRIT -> 46;
            case AVANTOE -> 50;
            case KWUARM -> 54;
            case SNAPDRAGON -> 57;
            case CADANTINE -> 60;
            case LANTADYME -> 64;
            case DWARF_WEED -> 67;
            case TORSTOL -> 71;
            case HUASCA -> 60; // approx (between cadantine and lantadyme, like its level)
        };
    }

    /** Harvest lives: chance (out of 256, before the +1) to save a life at Farming level 99; 80 for every herb. */
    public int harvestHigh() {
        return 80;
    }

    public double plantXp() {
        return plantXp;
    }

    public double harvestXp() {
        return harvestXp;
    }

    public int color() {
        return color;
    }

    public int value() {
        return value;
    }
}
