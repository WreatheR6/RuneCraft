package com.runecraft.osrs.content.islands;

import java.util.List;

/**
 * Block-based travel networks. Sneak-clicking a network block turns its dial to the next
 * destination; clicking travels to the chosen one. Each stop is a place (or Landmark) plus the block offset of the
 * network block from the place centre, where travellers arrive.
 *
 * <ul>
 *   <li>Fairy rings: the hub in Zanaris, the rings built into this module's structures, a ring at the
 *   OSRS tile of every other surface ring (codes, tiles and quest requirements as in OSRS, checked by
 *   TransportSnapshotTest), plus CKP (the Cosmic Altar's plane), BJR (the Fisher Realm) and DIR (Gorak's
 *   Plane), which lead to the places this world builds for those realms. Using them needs Fairytale II -
 *   Cure a Queen to have been started.</li>
 *   <li>The Mycelium transportation system: Fossil Island's four Magic Mushtrees.</li>
 * </ul>
 */
public final class IslandsTravel {
    private IslandsTravel() {
    }

    /**
     * A destination: {@code place} is a PlaceDef id or Landmark name; dx/dz the block offset of the ring/tree;
     * {@code quest} (or null) must be complete to travel there or from there.
     */
    public record Stop(String code, String name, String place, int dx, int dz, boolean hasBlock, String quest) {
    }

    public record Network(String id, String blockId, String quest, List<Stop> stops) {
        public Stop stop(String code) {
            for (Stop s : stops) {
                if (s.code().equals(code)) {
                    return s;
                }
            }
            return null;
        }

        public int indexOf(String code) {
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).code().equals(code)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /** Prefix of the places this class adds for fairy rings that have no hand-built structure around them. */
    public static final String RING_PLACE_PREFIX = "isl_fairy_ring_";

    /**
     * Fairy rings that stand on their own (OSRS code, tile of the ring, name, quest). Each gets a bare place at
     * the ring's tile; the Minecraft side lays the ring there. Rings in realms this world doesn't build (the
     * Abyss, Enchanted Valley, Yu'biusk, the Dorgeshuun caves, the Myreque hideout, the Abyssal Nexus, TzHaar,
     * the Great Conch) and the player-owned house ring (DIQ) are not included.
     */
    private static final Object[][] RINGS = {
            {"AIR", 2700, 3247, "Kandarin: South-east of Ardougne", null},
            {"AIS", 1429, 3324, "Varlamore: Auburn Valley", null},
            {"AJP", 1651, 3010, "Varlamore: Avium Savannah", "children_of_the_sun"},
            {"AJR", 2780, 3613, "Fremennik: Slayer cave south-east of Rellekka", null},
            {"AJS", 2500, 3896, "Islands: Penguins near Miscellania", null},
            {"AKP", 3284, 2706, "Kharidian Desert: Necropolis", null},
            {"AKQ", 2319, 3619, "Kandarin: Piscatoris Hunter area", null},
            {"AKR", 1826, 3540, "Kourend: Hosidius Vinery", null},
            {"ALP", 2503, 3636, "Fremennik: Lighthouse", null},
            {"ALQ", 3597, 3495, "Morytania: Haunted Woods east of Canifis", null},
            {"ALS", 2644, 3495, "Kandarin: McGrubor's Wood", null},
            {"BIP", 3410, 3324, "Morytania: South-west of Mort Myre", null},
            {"BIQ", 3251, 3095, "Kharidian Desert: Near the Kalphite Hive", null},
            {"BIS", 2635, 3266, "Kandarin: Ardougne Zoo unicorns", null},
            {"BJP", 2264, 2976, "Kandarin: Isle of Souls", null},
            {"BJS", 2150, 3070, "Islands: Near Zul-Andra", "regicide"},
            {"BKR", 3469, 3431, "Morytania: Mort Myre swamp, south of Canifis", null},
            {"BLR", 2740, 3351, "Kandarin: Legends' Guild", null},
            {"BLS", 1295, 3493, "Kourend: South of Mount Quidamortem", null},
            {"CIQ", 2528, 3127, "Kandarin: North-west of Yanille", null},
            {"CIR", 1302, 3762, "Kebos Lowlands: South of Mount Karuulm", null},
            {"CIS", 1639, 3868, "Kourend: North of the Arceuus Library", null},
            {"CJR", 2705, 3576, "Kandarin: Sinclair Mansion", null},
            {"CKQ", 1359, 2941, "Varlamore: Aldarin", "children_of_the_sun"},
            {"CKR", 2801, 3003, "Karamja: South of Tai Bwo Wannai", null},
            {"CKS", 3447, 3470, "Morytania: Canifis", null},
            {"CLP", 3082, 3206, "Islands: South of Draynor Village", null},
            {"CLS", 2682, 3081, "Islands: Jungle spiders near Yanille (Hazelmere's home)", null},
            {"DIS", 3108, 3149, "Misthalin: Wizards' Tower", null},
            {"DJP", 2658, 3230, "Kandarin: Tower of Life", null},
            {"DJR", 1455, 3658, "Kourend: Chasm of Fire", null},
            {"DKP", 2900, 3111, "Karamja: South of Musa Point", null},
            {"DKR", 3129, 3496, "Misthalin: Edgeville", null},
            {"DKS", 2744, 3719, "Fremennik: Polar Hunter area", null},
            {"DLQ", 3423, 3016, "Kharidian Desert: North of Nardah", null},
    };

    /** The bare places that hold the stand-alone fairy rings. */
    public static List<com.runecraft.osrs.content.PlaceDef> ringPlaces() {
        List<com.runecraft.osrs.content.PlaceDef> out = new java.util.ArrayList<>();
        for (Object[] r : RINGS) {
            out.add(com.runecraft.osrs.content.PlaceDef.of(RING_PLACE_PREFIX + ((String) r[0]).toLowerCase(),
                    "Fairy ring " + r[0], (Integer) r[1], (Integer) r[2], com.runecraft.osrs.content.PlaceDef.Style.NONE, List.of()));
        }
        return out;
    }

    private static List<Stop> fairyStops() {
        List<Stop> stops = new java.util.ArrayList<>(List.of(
                new Stop("ZANARIS", "Zanaris (BKS, fairy ring centre)", IslandsPlaces.ZANARIS, IslandsStructures.ZANARIS_RING_DX,
                        IslandsStructures.ZANARIS_RING_DZ, true, null),
                new Stop("AIQ", "Asgarnia: Mudskipper Point", IslandsPlaces.MUDSKIPPER_POINT, IslandsStructures.MUDSKIPPER_RING_DX,
                        IslandsStructures.MUDSKIPPER_RING_DZ, true, null),
                new Stop("AKS", "Feldip Hills: Feldip Hunter area", IslandsPlaces.FELDIP_HUNTER_AREA, IslandsStructures.FELDIP_RING_DX,
                        IslandsStructures.FELDIP_RING_DZ, true, null),
                new Stop("BJR", "Other realms: Fisher Realm", "fisher_realm", 3, 3, false, "holy_grail"),
                new Stop("BKP", "Feldip Hills: South of Castle Wars", IslandsPlaces.CASTLE_WARS, IslandsStructures.CASTLE_WARS_RING_DX,
                        IslandsStructures.CASTLE_WARS_RING_DZ, true, null),
                new Stop("CIP", "Islands: Miscellania", IslandsPlaces.MISCELLANIA, IslandsStructures.MISCELLANIA_RING_DX,
                        IslandsStructures.MISCELLANIA_RING_DZ, true, "fremennik_trials"),
                new Stop("CKP", "Other realms: Cosmic entity's plane", "COSMIC_ALTAR", 3, 3, false, null),
                new Stop("CLR", "Islands: Ape Atoll", IslandsPlaces.APE_ATOLL_AGILITY, IslandsStructures.APE_RING_DX,
                        IslandsStructures.APE_RING_DZ, true, "monkey_madness_i"),
                new Stop("DIR", "Other realms: Gorak's Plane", "gorak_plane", 3, 3, false, null),
                new Stop("DLR", "Islands: Poison Waste, south of Isafdar", IslandsPlaces.ZUL_ANDRA, IslandsStructures.ZUL_ANDRA_RING_DX,
                        IslandsStructures.ZUL_ANDRA_RING_DZ, true, null)));
        List<com.runecraft.osrs.content.PlaceDef> places = ringPlaces();
        for (int i = 0; i < RINGS.length; i++) {
            Object[] r = RINGS[i];
            com.runecraft.osrs.content.PlaceDef p = places.get(i);
            // Place centres snap to their chunk's centre; the ring itself stands on its exact tile.
            int dx = com.runecraft.osrs.GielinorMap.toBlockX(p.osrsX()) - p.blockX();
            int dz = com.runecraft.osrs.GielinorMap.toBlockZ(p.osrsY()) - p.blockZ();
            stops.add(new Stop((String) r[0], (String) r[3], p.id(), dx, dz, true, (String) r[4]));
        }
        // The dial runs in code order after the Zanaris hub, like the three wheels of an OSRS ring.
        Stop hub = stops.remove(0);
        stops.sort(java.util.Comparator.comparing(Stop::code));
        stops.add(0, hub);
        return List.copyOf(stops);
    }

    public static final Network FAIRY_RINGS = new Network("fairy_rings", IslandsBlocks.FAIRY_RING, "fairytale_ii_cure_a_queen",
            fairyStops());

    /** Magic Mushtrees: each stands on its OSRS tile (shortest-path magic_mushtrees.tsv). */
    public static final Network MUSHTREES = new Network("mycelium", IslandsBlocks.MUSHTREE, null, List.of(
            new Stop("HOUSE_ON_THE_HILL", "House on the Hill", IslandsPlaces.HOUSE_ON_THE_HILL, IslandsStructures.HOUSE_TREE_DX,
                    IslandsStructures.HOUSE_TREE_DZ, true, null),
            new Stop("VERDANT_VALLEY", "Verdant Valley", IslandsPlaces.VERDANT_VALLEY, IslandsStructures.VALLEY_TREE_DX,
                    IslandsStructures.VALLEY_TREE_DZ, true, null),
            new Stop("STICKY_SWAMP", "Sticky swamp", IslandsPlaces.MUSHROOM_FOREST, IslandsStructures.FOREST_TREE_DX,
                    IslandsStructures.FOREST_TREE_DZ, true, null),
            new Stop("MUSHROOM_MEADOW", "Mushroom Meadow", IslandsPlaces.MUSHROOM_MEADOW, IslandsStructures.MEADOW_TREE_DX,
                    IslandsStructures.MEADOW_TREE_DZ, true, null)));

    public static List<Network> networks() {
        return List.of(FAIRY_RINGS, MUSHTREES);
    }

    public static Network byBlock(String blockId) {
        for (Network n : networks()) {
            if (n.blockId().equals(blockId)) {
                return n;
            }
        }
        return null;
    }

    /** The destination after {@code code} on the dial (skipping {@code here}). */
    public static Stop next(Network n, String code, String here) {
        int i = Math.max(-1, n.indexOf(code));
        for (int k = 1; k <= n.stops().size(); k++) {
            Stop s = n.stops().get((i + k) % n.stops().size());
            if (!s.code().equals(here)) {
                return s;
            }
        }
        return n.stops().get(0);
    }
}
