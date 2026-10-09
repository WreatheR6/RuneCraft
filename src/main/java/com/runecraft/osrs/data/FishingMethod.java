package com.runecraft.osrs.data;

/** Fishing methods, the tool each needs and any bait consumed per catch. */
public enum FishingMethod {
    NET("Net", "small_fishing_net", null),
    BAIT("Bait", "fishing_rod", "fishing_bait"),
    LURE("Lure", "fly_fishing_rod", "minecraft:feather"),
    CAGE("Cage", "lobster_pot", null),
    HARPOON("Harpoon", "harpoon", null),
    DARK_BAIT("Cage", "lobster_pot", "dark_fishing_bait");

    private final String verb;
    private final String tool;
    private final String bait;

    FishingMethod(String verb, String tool, String bait) {
        this.verb = verb;
        this.tool = tool;
        this.bait = bait;
    }

    public String verb() {
        return verb;
    }

    public String tool() {
        return tool;
    }

    /** Bait item consumed per catch, or null. */
    public String bait() {
        return bait;
    }
}
