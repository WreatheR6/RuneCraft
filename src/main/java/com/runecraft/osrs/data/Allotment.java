package com.runecraft.osrs.data;

/** Allotment crops (3 seeds per patch). */
public enum Allotment {
    POTATO("potato", "Potato", 1, 8.0, 9.0, 0xB09060, 1),
    ONION("onion", "Onion", 5, 9.5, 10.5, 0xD0B070, 2),
    CABBAGE("cabbage", "Cabbage", 7, 10.0, 11.5, 0x70B040, 2),
    TOMATO("tomato", "Tomato", 12, 12.5, 14.0, 0xD03020, 3),
    SWEETCORN("sweetcorn", "Sweetcorn", 20, 17.0, 19.0, 0xE0D040, 9),
    STRAWBERRY("strawberry", "Strawberry", 31, 26.0, 29.0, 0xE02040, 18),
    WATERMELON("watermelon", "Watermelon", 47, 48.5, 54.5, 0x30A030, 48),
    SNAPE_GRASS("snape_grass", "Snape grass", 61, 82.0, 82.0, 0x608040, 60);

    private final String id;
    private final String name;
    private final int level;
    private final double plantXp;
    private final double harvestXp;
    private final int color;
    private final int value;

    Allotment(String id, String name, int level, double plantXp, double harvestXp, int color, int value) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.plantXp = plantXp;
        this.harvestXp = harvestXp;
        this.color = color;
        this.value = value;
    }

    public String produceItem() {
        return id;
    }

    public String seedItem() {
        return id + "_seed";
    }

    public String displayName() {
        return name;
    }

    public int level() {
        return level;
    }

    /** OSRS growth time in minutes (10-minute cycles: potato/onion/cabbage/tomato 4, sweetcorn/strawberry 6, snape grass 7, watermelon 8). */
    public int growthMinutes() {
        return switch (this) {
            case POTATO, ONION, CABBAGE, TOMATO -> 40;
            case SWEETCORN, STRAWBERRY -> 60;
            case SNAPE_GRASS -> 70;
            case WATERMELON -> 80;
        };
    }

    /** Harvest lives: chance (out of 256, before the +1) to save a life at Farming level 1 (OSRS wiki). */
    public int harvestLow() {
        return switch (this) {
            case POTATO -> 101;
            case ONION -> 105;
            case CABBAGE -> 107;
            case TOMATO -> 112;
            case SWEETCORN -> 88;
            case STRAWBERRY -> 103;
            case WATERMELON -> 126;
            case SNAPE_GRASS -> 148;
        };
    }

    /** Harvest lives: chance (out of 256, before the +1) to save a life at Farming level 99. */
    public int harvestHigh() {
        return this == SNAPE_GRASS ? 195 : 180;
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
