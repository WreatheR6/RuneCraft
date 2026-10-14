package com.runecraft.osrs.content.systems;

import java.util.List;

/** Teleport destinations at real OSRS world coordinates (surface equivalents for underground spots). */
public final class Teleports {
    /** A named destination at OSRS world coordinates; {@code quest} (or null) must be complete to use it. */
    public record Destination(String name, int x, int y, String quest) {
    }

    private Teleports() {
    }

    public static Destination at(String name, int x, int y) {
        return new Destination(name, x, y, null);
    }

    public static Destination at(String name, int x, int y, String quest) {
        return new Destination(name, x, y, quest);
    }

    /*
     * Jewellery: OSRS arrival tiles (checked against the snapshot in src/test/resources/osrs/transport by
     * TransportSnapshotTest). Destinations OSRS puts underground or on another plane go to the place this world
     * builds for them instead (each listed in TransportSnapshotTest.RELOCATED).
     */
    public static final List<Destination> GLORY = List.of(at("Edgeville", 3087, 3496), at("Karamja", 2918, 3176),
            at("Draynor Village", 3105, 3251), at("Al Kharid", 3293, 3163));
    public static final List<Destination> DUELING = List.of(at("Emir's Arena", 3315, 3235), at("Castle Wars", 2440, 3090),
            at("Ferox Enclave", 3151, 3635), at("Fortis Colosseum", 1793, 3107));
    public static final List<Destination> GAMES = List.of(at("Burthorpe", 2899, 3553), at("Barbarian Outpost", 2520, 3571),
            at("Corporeal Beast", 3206, 3681), // relocated: the Corporeal Beast's lair
            at("Tears of Guthix", 3252, 3175), // relocated: the Tears of Guthix cave
            at("Wintertodt Camp", 1631, 3940));
    public static final List<Destination> SKILLS = List.of(at("Fishing Guild", 2611, 3390),
            at("Mining Guild", 3016, 3339), // relocated: the guild's surface entrance (OSRS lands underground)
            at("Crafting Guild", 2933, 3295), at("Cooks' Guild", 3144, 3438), at("Woodcutting Guild", 1662, 3505),
            at("Farming Guild", 1248, 3719));
    public static final List<Destination> COMBAT = List.of(at("Warriors' Guild", 2882, 3547), at("Champions' Guild", 3192, 3368),
            at("Edgeville Monastery", 3052, 3490), at("Ranging Guild", 2653, 3439));
    public static final List<Destination> WEALTH = List.of(at("Miscellania", 2534, 3862, "throne_of_miscellania"),
            at("Grand Exchange", 3163, 3478), at("Falador Park", 2995, 3375),
            at("Dondakan's Rock", 2826, 3504, "between_a_rock")); // relocated: the mine entrance (OSRS lands underground)
    public static final List<Destination> DIGSITE = List.of(at("Digsite", 3341, 3445),
            at("Fossil Island", 3763, 3869, "bone_voyage"),
            at("Lithkren", 3549, 3999, "dragon_slayer_ii")); // relocated: Lithkren's surface (OSRS lands in the vault)
    public static final List<Destination> PASSAGE = List.of(at("Wizards' Tower", 3114, 3179), at("The Outpost", 2430, 3349),
            at("Eagles' Eyrie", 3405, 3158));
    public static final List<Destination> BURNING = List.of(at("Chaos Temple", 3234, 3634), at("Bandit Camp", 3038, 3651),
            at("Lava Maze", 3028, 3842));
    /** Xeric's Honour (Mount Quidamortem): unlocked for good by using an ancient tablet (Chambers of Xeric) on the talisman. */
    public static final Destination XERICS_HONOUR = at("Xeric's Honour", 1254, 3560);
    public static final List<Destination> XERIC = List.of(at("Xeric's Lookout", 1579, 3530), at("Xeric's Glade", 1752, 3566),
            at("Xeric's Inferno", 1504, 3815), at("Xeric's Heart", 1644, 3673), XERICS_HONOUR);
    /** The item that unlocks {@link #XERICS_HONOUR} when used on Xeric's talisman (consumed; the unlock is permanent). */
    public static final String ANCIENT_TABLET = "ancient_tablet";
    /** Slayer ring (8 charges; the eternal ring is unlimited). */
    public static final List<Destination> SLAYER = List.of(at("Stronghold Slayer Cave", 2431, 3422),
            at("Slayer Tower", 3421, 3537, "priest_in_peril"),
            at("Fremennik Slayer Dungeon", 2797, 3614), // relocated: the dungeon entrance (OSRS lands inside it)
            at("Tarn's Lair", 3445, 3223, "haunted_mine"), // relocated: the Haunted Mine entrance to the lair as built here
            at("Dark Beasts", 2232, 3300, "mournings_end_part_ii")); // relocated: the Iorwerth Dungeon, where dark beasts live here

    /** The unlock item a destination needs (used on the teleport item once), or null. */
    public static String unlockItem(Destination d) {
        return XERICS_HONOUR.equals(d) ? ANCIENT_TABLET : null;
    }
    /** Camulet (Enakhra's Lament): the temple entrance; 4 charges, refilled with ugthanki dung in OSRS. */
    public static final List<Destination> CAMULET = List.of(at("Enakhra's Temple", 3104, 2932, "enakhras_lament"));
    /** Skull sceptre: the Stronghold of Security entrance in Barbarian Village (usable up to level 20 Wilderness). */
    public static final Destination SKULL_SCEPTRE = at("Barbarian Village", 3081, 3421);
    public static final List<Destination> DRAKAN = List.of(at("Ver Sinhaza", 3649, 3230),
            at("Darkmeyer", 3592, 3337, "sins_of_the_father"),
            at("Slepe", 3720, 3322)); // relocated: Slepe village (OSRS lands in the Sisterhood Sanctuary below it) approx
    public static final List<Destination> ECTOPHIAL = List.of(at("Ectofuntus", 3660, 3522));
    public static final List<Destination> CRAFTING_CAPE = List.of(at("Crafting Guild", 2931, 3286));

    /*
     * Round 17 teleport items (OSRS arrival tiles from the shortest-path snapshot, checked by TransportSnapshotTest).
     */
    /**
     * Pharaoh's sceptre (Pyramid Plunder): the four great pyramids, one charge each. Jaltevas must first be unlocked by
     * communing with the Necropolis obelisk while carrying the sceptre ({@link #needsVisit}).
     */
    public static final List<Destination> PHARAOH = List.of(
            at("Jalsavrah", 3288, 2800), // relocated: OSRS lands in the plane-3 Pyramid Plunder lobby; the minigame's pyramid here
            at("Jaleustrophos", 3341, 2827), at("Jaldraocht", 3232, 2897), at("Jaltevas", 3313, 2718));
    /** Ring of the elements (Guardians of the Rift): beside the four elemental altars' entrances. */
    public static final List<Destination> ELEMENTS = List.of(at("Air Altar", 2981, 3276), at("Water Altar", 3170, 3155),
            at("Earth Altar", 3288, 3468), at("Fire Altar", 3314, 3279));
    /**
     * Ring of returning (jade ring, Lvl-2 Enchant): the player's respawn point. RuneCraft always respawns players in
     * Lumbridge, so this is the wiki's Lumbridge respawn tile.
     */
    public static final List<Destination> RETURNING = List.of(at("Respawn point (Lumbridge)", 3221, 3219));
    /**
     * Giantsoul amulet (Royal Titans): the lairs of Bryophyta, Obor and the Royal Titans. All three are underground in
     * OSRS; here they go to the summoning stones this world builds for them (approx tiles beside each lair).
     */
    public static final List<Destination> GIANTSOUL = List.of(
            at("Bryophyta", 3211, 3424), // relocated: Bryophyta's summoning stone south of Varrock's square (approx)
            at("Obor", 3090, 3489), // relocated: Obor's summoning stone south of Edgeville (approx)
            at("Branda and Eldric", 3004, 3146)); // relocated: the Asgarnian Ice Dungeon entrance where the Titans' lair is built
    /**
     * Pendant of ates (frost nagua, Amoxliatl): six Varlamore statues of Ates. Each must be activated first, which here
     * happens when the pendant's menu is opened near the statue's spot ({@link #needsVisit}; the Aldarin icon step of
     * Death on the Isle is not modelled - approx).
     */
    public static final List<Destination> ATES = List.of(at("The Darkfrost", 1491, 3283), at("Twilight Temple", 1668, 3223),
            at("Ralos' Rise", 1459, 3138), at("North Aldarin", 1426, 2995), at("Kastori", 1366, 3086),
            at("Nemus Retreat", 1364, 3277));
    /** How close (tiles) a player must be to a {@link #needsVisit} destination to activate it. approx */
    public static final int ACTIVATE_RADIUS = 12;

    /** Destinations unlocked by visiting them once with the item (the statues of Ates, the Necropolis obelisk). */
    public static boolean needsVisit(Destination d) {
        return ATES.contains(d) || PHARAOH.get(3).equals(d);
    }

    /**
     * Highest Wilderness level an item's teleport works from (OSRS): dragonstone jewellery (glory, wealth,
     * skills, combat) and the slayer ring work up to level 30, everything else up to level 20.
     */
    public static int wildernessLimit(String itemId) {
        String id = itemId == null ? "" : itemId;
        if (id.startsWith("amulet_of_glory") || id.startsWith("amulet_of_eternal_glory") || id.startsWith("combat_bracelet")
                || id.startsWith("skills_necklace") || id.startsWith("ring_of_wealth") || id.startsWith("slayer_ring")) {
            return 30;
        }
        return 20;
    }

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
