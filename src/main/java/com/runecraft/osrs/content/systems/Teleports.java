package com.runecraft.osrs.content.systems;

import java.util.List;

/** Teleport destinations at real OSRS world coordinates (surface equivalents for underground spots). */
public final class Teleports {
    /** A named destination at OSRS world coordinates. */
    public record Destination(String name, int x, int y) {
    }

    private Teleports() {
    }

    public static Destination at(String name, int x, int y) {
        return new Destination(name, x, y);
    }

    // Jewellery
    public static final List<Destination> GLORY = List.of(at("Edgeville", 3087, 3496), at("Karamja", 2918, 3176),
            at("Draynor Village", 3105, 3251), at("Al Kharid", 3293, 3163));
    public static final List<Destination> DUELING = List.of(at("PvP Arena", 3315, 3235), at("Castle Wars", 2441, 3090),
            at("Ferox Enclave", 3151, 3635), at("Fortis Colosseum", 1798, 3104));
    public static final List<Destination> GAMES = List.of(at("Burthorpe", 2898, 3553), at("Barbarian Outpost", 2520, 3571),
            at("Corporeal Beast", 3244, 3784), at("Tears of Guthix", 3245, 3190), at("Wintertodt Camp", 1630, 3940));
    public static final List<Destination> SKILLS = List.of(at("Fishing Guild", 2611, 3390), at("Mining Guild", 3016, 3339),
            at("Crafting Guild", 2933, 3290), at("Cooks' Guild", 3143, 3442), at("Woodcutting Guild", 1662, 3505),
            at("Farming Guild", 1249, 3718));
    public static final List<Destination> COMBAT = List.of(at("Warriors' Guild", 2882, 3548), at("Champions' Guild", 3191, 3367),
            at("Edgeville Monastery", 3052, 3488), at("Ranging Guild", 2655, 3441));
    public static final List<Destination> WEALTH = List.of(at("Miscellania", 2535, 3862), at("Grand Exchange", 3164, 3478),
            at("Falador Park", 2995, 3375), at("Dondakan's Rock", 2831, 3165));
    public static final List<Destination> DIGSITE = List.of(at("Digsite", 3341, 3445), at("Fossil Island", 3724, 3808),
            at("Lithkren", 3549, 3999));
    public static final List<Destination> PASSAGE = List.of(at("Wizards' Tower", 3113, 3179), at("The Outpost", 2430, 3348),
            at("Eagles' Eyrie", 3406, 3157));
    public static final List<Destination> BURNING = List.of(at("Chaos Temple", 3236, 3635), at("Bandit Camp", 3038, 3651),
            at("Lava Maze", 3028, 3842));
    public static final List<Destination> XERIC = List.of(at("Xeric's Lookout", 1576, 3528), at("Xeric's Glade", 1752, 3566),
            at("Xeric's Inferno", 1504, 3817), at("Xeric's Heart", 1640, 3674));
    public static final List<Destination> DRAKAN = List.of(at("Ver Sinhaza", 3649, 3230), at("Darkmeyer", 3592, 3337));
    public static final List<Destination> ECTOPHIAL = List.of(at("Ectofuntus", 3660, 3522));
    public static final List<Destination> CRAFTING_CAPE = List.of(at("Crafting Guild", 2933, 3290));

    // Home teleports
    public static final Destination EDGEVILLE_HOME = at("Edgeville", 3087, 3496);
    public static final Destination LUNAR_HOME = at("Lunar Isle", 2100, 3914);
    public static final Destination ARCEUUS_HOME = at("Arceuus", 1712, 3883);

    /** Distance check helper (tiles). */
    public static boolean within(int x1, int y1, int x2, int y2, int radius) {
        long dx = x1 - x2;
        long dy = y1 - y2;
        return dx * dx + dy * dy <= (long) radius * radius;
    }
}
