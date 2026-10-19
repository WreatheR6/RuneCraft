package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;

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
 *   Cure a Queen to have been started. The realms this world builds elsewhere (BJR, DIR, DIP, DLS and BLQ, Yu'biusk)
 *   have a ring of their own beside their place ({@link #REALM_RINGS}) to leave from; BKQ leads to a small Enchanted
 *   Valley carved at its OSRS tile ({@link #enchantedValley()}).</li>
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
     * the ring's tile; the Minecraft side lays the ring there. The dungeon realms (TzHaar, the Abyss, the Dorgeshuun
     * caves) and the Enchanted Valley are {@link #DUNGEON_RINGS}, the realms built elsewhere are {@link #REALM_RINGS},
     * and the house ring (DIQ) is a virtual stop.
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
            {"CJQ", 3178, 2447, "Islands: The Great Conch", "troubled_tortugans"},
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

    /**
     * Stop place of the player-owned house's fairy ring (DIQ): travelling there is a Teleport to House, and only works with
     * a fairy ring built in the house.
     */
    public static final String HOUSE_PLACE = "isl_house_ring";

    /**
     * A fairy ring in a dungeon realm: the ring block stands at (x, floorY + 1, z) on that dungeon's floor (block
     * coordinates; these dungeons are hand-carved, not at their OSRS tiles, so each spot is picked inside the matching
     * cavern and checked against the dungeon's carved air by TransportSnapshotTest).
     */
    public record DungeonRing(String code, String dungeon, int x, int floorY, int z) {
    }

    public static final List<DungeonRing> DUNGEON_RINGS = List.of(
            // TzHaar city (DungeonsKaramjaDesert.tzhaarCity): the market cavern, west of the lava river and north of the
            // east-west one (OSRS: south-west of the city, near the bank).
            new DungeonRing("BLP", "dng_tzhaar_city", StructureDef.bx(2820) + 54, 6, StructureDef.bz(3166) - 34),
            // The Abyss (DungeonsMisthalin.abyss): its outer ring, east-north-east of the centre, outside the inner wall.
            new DungeonRing("ALR", "dng_the_abyss", StructureDef.bx(3106) + 30, -24, StructureDef.bz(3556) - 52),
            // Dorgesh-Kaan (DungeonsMisthalin.lumbridgeCellar): the south-west corner of the city cavern, beside the houses.
            new DungeonRing("AJQ", "dng_lumbridge_cellar_caves", 66, 10, 56),
            // The Enchanted Valley (enchantedValley()): the ring on its own OSRS tile (3041, 4532), in the carved valley.
            new DungeonRing("BKQ", "isl_enchanted_valley", StructureDef.bx(3041), 20, StructureDef.bz(4532)));

    /** Structure id of the Enchanted Valley (BKQ). */
    public static final String ENCHANTED_VALLEY = "isl_enchanted_valley";
    /** The Enchanted Valley's tile box {x1, y1, x2, y2} around its fairy ring (3041, 4532). approx layout */
    public static final int[] ENCHANTED_VALLEY_TILES = {3024, 4516, 3058, 4546};

    /**
     * The Enchanted Valley (BKQ), an outdoor realm in OSRS (tiles around 3041, 4532, north of the map): here a grassy
     * valley sealed in rock deep under the far northern sea (its top stays below y 34, so the sea bed above is left alone),
     * lit from above, with its pond, trees and flowers. Its fairy ring is laid by the Minecraft side like the other dungeon
     * rings, on the ring's own OSRS tile.
     */
    public static StructureDef enchantedValley() {
        DungeonRing ring = dungeonRing("BKQ");
        int[] t = ENCHANTED_VALLEY_TILES;
        int x1 = StructureDef.bx(t[0]);
        int x2 = StructureDef.bx(t[2]) + 1;
        int z1 = StructureDef.bz(t[3]);
        int z2 = StructureDef.bz(t[1]) + 1;
        int f = ring.floorY();
        StructureDef.Builder b = StructureDef.builder(ENCHANTED_VALLEY, "Enchanted Valley");
        b.fill(x1 - 1, f - 2, z1 - 1, x2 + 1, f + 11, z2 + 1, "minecraft:stone");
        b.clear(x1, f + 1, z1, x2, f + 10, z2);
        b.fill(x1, f - 1, z1, x2, f - 1, z2, "minecraft:dirt");
        b.fill(x1, f, z1, x2, f, z2, "minecraft:grass_block");
        // Mossy valley sides and a lit "sky".
        b.walls(x1, f + 1, z1, x2, f + 3, z2, "minecraft:mossy_cobblestone");
        for (int x = x1 + 4; x <= x2 - 4; x += 8) {
            for (int z = z1 + 4; z <= z2 - 4; z += 8) {
                b.set(x, f + 11, z, "minecraft:shroomlight");
            }
        }
        // The pond in the west of the valley (its fishing spot in OSRS) and the trees the tree spirit guards.
        int px = StructureDef.bx(3028);
        int pz = StructureDef.bz(4538);
        b.fill(px, f - 1, pz, px + 9, f, pz + 11, "minecraft:clay");
        b.fill(px + 1, f, pz + 1, px + 8, f, pz + 10, "minecraft:water");
        for (int[] tree : new int[][]{{3049, 4541}, {3053, 4527}, {3034, 4522}, {3046, 4520}, {3055, 4538}}) {
            int x = StructureDef.bx(tree[0]);
            int z = StructureDef.bz(tree[1]);
            b.fill(x - 2, f + 4, z - 2, x + 2, f + 5, z + 2, "minecraft:oak_leaves[persistent=true]");
            b.fill(x - 1, f + 6, z - 1, x + 1, f + 6, z + 1, "minecraft:oak_leaves[persistent=true]");
            b.fill(x, f + 1, z, x, f + 5, z, "minecraft:oak_log[axis=y]");
        }
        String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:blue_orchid"};
        int[][] spots = {{3038, 4527}, {3044, 4537}, {3036, 4541}, {3051, 4533}, {3043, 4524}, {3050, 4545}, {3026, 4520}};
        for (int i = 0; i < spots.length; i++) {
            b.set(StructureDef.bx(spots[i][0]), f + 1, StructureDef.bz(spots[i][1]), flowers[i % flowers.length]);
        }
        return b.build();
    }

    /** OSRS code of the Yu'biusk ring: it opens once Land of the Goblins has been started (OSRS: part-way through). approx */
    public static final String YUBIUSK = "BLQ";
    public static final String YUBIUSK_QUEST = "land_of_the_goblins";

    /** Block offset of a {@link #REALM_RINGS} ring from its place's centre: just south of the place's plaza. */
    public static final int REALM_RING_DX = 0;
    public static final int REALM_RING_DZ = 7;

    /**
     * Rings of realms this world builds as places elsewhere (the Fisher Realm, Gorak's Plane, the Abyssal Nexus, the Myreque
     * hideout and Yu'biusk): a ring beside each place to leave from, laid when the place is built or a traveller arrives.
     */
    public static final java.util.Set<String> REALM_RINGS = java.util.Set.of("BJR", "DIR", "DIP", "DLS", YUBIUSK);

    /** The dungeon ring with this code, or null. */
    public static DungeonRing dungeonRing(String code) {
        for (DungeonRing d : DUNGEON_RINGS) {
            if (d.code().equals(code)) {
                return d;
            }
        }
        return null;
    }

    /** Where a traveller arriving at a dungeon ring stands: two blocks east of the ring, on the same floor. */
    public static int[] dungeonArrival(DungeonRing d) {
        return new int[]{d.x() + 2, d.floorY() + 1, d.z()};
    }

    /**
     * The bare places that hold the stand-alone fairy rings, then one over each dungeon ring (the Minecraft side lays
     * that ring on the dungeon's floor below once the dungeon is carved).
     */
    public static List<com.runecraft.osrs.content.PlaceDef> ringPlaces() {
        List<com.runecraft.osrs.content.PlaceDef> out = new java.util.ArrayList<>();
        for (Object[] r : RINGS) {
            out.add(com.runecraft.osrs.content.PlaceDef.of(RING_PLACE_PREFIX + ((String) r[0]).toLowerCase(),
                    "Fairy ring " + r[0], (Integer) r[1], (Integer) r[2], com.runecraft.osrs.content.PlaceDef.Style.NONE, List.of()));
        }
        for (DungeonRing d : DUNGEON_RINGS) {
            out.add(com.runecraft.osrs.content.PlaceDef.of(RING_PLACE_PREFIX + d.code().toLowerCase(), "Fairy ring " + d.code(),
                    com.runecraft.osrs.GielinorMap.toOsrsX(d.x()), com.runecraft.osrs.GielinorMap.toOsrsY(d.z()),
                    com.runecraft.osrs.content.PlaceDef.Style.NONE, List.of()));
        }
        return out;
    }

    /** The place id over a dungeon ring. */
    public static String ringPlace(String code) {
        return RING_PLACE_PREFIX + code.toLowerCase();
    }

    private static List<Stop> fairyStops() {
        List<Stop> stops = new java.util.ArrayList<>(List.of(
                new Stop("ZANARIS", "Zanaris (BKS, fairy ring centre)", IslandsPlaces.ZANARIS, IslandsStructures.ZANARIS_RING_DX,
                        IslandsStructures.ZANARIS_RING_DZ, true, null),
                new Stop("AIQ", "Asgarnia: Mudskipper Point", IslandsPlaces.MUDSKIPPER_POINT, IslandsStructures.MUDSKIPPER_RING_DX,
                        IslandsStructures.MUDSKIPPER_RING_DZ, true, null),
                new Stop("AKS", "Feldip Hills: Feldip Hunter area", IslandsPlaces.FELDIP_HUNTER_AREA, IslandsStructures.FELDIP_RING_DX,
                        IslandsStructures.FELDIP_RING_DZ, true, null),
                new Stop("BJR", "Other realms: Fisher Realm", "fisher_realm", REALM_RING_DX, REALM_RING_DZ, true, "holy_grail"),
                new Stop("BKP", "Feldip Hills: South of Castle Wars", IslandsPlaces.CASTLE_WARS, IslandsStructures.CASTLE_WARS_RING_DX,
                        IslandsStructures.CASTLE_WARS_RING_DZ, true, null),
                new Stop("CIP", "Islands: Miscellania", IslandsPlaces.MISCELLANIA, IslandsStructures.MISCELLANIA_RING_DX,
                        IslandsStructures.MISCELLANIA_RING_DZ, true, "fremennik_trials"),
                new Stop("CKP", "Other realms: Cosmic entity's plane", "COSMIC_ALTAR", 3, 3, false, null),
                new Stop("CLR", "Islands: Ape Atoll", IslandsPlaces.APE_ATOLL_AGILITY, IslandsStructures.APE_RING_DX,
                        IslandsStructures.APE_RING_DZ, true, "monkey_madness_i"),
                new Stop("DIR", "Other realms: Gorak's Plane", "gorak_plane", REALM_RING_DX, REALM_RING_DZ, true, null),
                new Stop("DLR", "Islands: Poison Waste, south of Isafdar", IslandsPlaces.ZUL_ANDRA, IslandsStructures.ZUL_ANDRA_RING_DX,
                        IslandsStructures.ZUL_ANDRA_RING_DZ, true, null),
                // Realms built as places elsewhere in this world, each with a ring of its own beside the place (REALM_RINGS).
                new Stop("DIP", "Other realms: Abyssal Nexus", "abyssal_nexus", REALM_RING_DX, REALM_RING_DZ, true, null),
                new Stop("DLS", "Morytania: Myreque hideout under Canifis", "myreque_hideout", REALM_RING_DX, REALM_RING_DZ, true,
                        "in_search_of_the_myreque"),
                // Yu'biusk: the goblins' realm, where this world's Land of the Goblins builds it.
                new Stop(YUBIUSK, "Other realms: Yu'biusk", "quest_yubiusk", REALM_RING_DX, REALM_RING_DZ, true, null),
                // The player-owned house's ring: a virtual stop (Teleport to House).
                new Stop("DIQ", "Player-owned house", HOUSE_PLACE, 0, 0, false, null)));
        List<com.runecraft.osrs.content.PlaceDef> places = ringPlaces();
        for (int i = 0; i < RINGS.length; i++) {
            Object[] r = RINGS[i];
            com.runecraft.osrs.content.PlaceDef p = places.get(i);
            // Place centres snap to their chunk's centre; the ring itself stands on its exact tile.
            int dx = com.runecraft.osrs.GielinorMap.toBlockX(p.osrsX()) - p.blockX();
            int dz = com.runecraft.osrs.GielinorMap.toBlockZ(p.osrsY()) - p.blockZ();
            stops.add(new Stop((String) r[0], (String) r[3], p.id(), dx, dz, true, (String) r[4]));
        }
        // Dungeon realms: rings on the floors of this world's dungeons (DUNGEON_RINGS), under their bare places.
        String[][] realms = {{"BLP", "Dungeons: TzHaar area", null}, {"ALR", "Other realms: The Abyss", null},
                {"AJQ", "Dungeons: Cave south of Dorgesh-Kaan", "death_to_the_dorgeshuun"},
                {"BKQ", "Other realms: Enchanted Valley", null}};
        for (int i = 0; i < DUNGEON_RINGS.size(); i++) {
            DungeonRing d = DUNGEON_RINGS.get(i);
            com.runecraft.osrs.content.PlaceDef p = places.get(RINGS.length + i);
            stops.add(new Stop(d.code(), realms[i][1], p.id(), d.x() - p.blockX(), d.z() - p.blockZ(), true, realms[i][2]));
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
