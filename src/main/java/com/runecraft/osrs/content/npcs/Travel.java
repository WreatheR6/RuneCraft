package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paid and free transport offered by NPCs: boats, charter ships, gnome gliders, magic carpets, canoes,
 * spirit trees, the balloon transport system and the Keldagrim minecarts.
 *
 * <p>Destinations are OSRS arrival tiles and fares/requirements are the OSRS ones, checked against a
 * snapshot of the RuneLite Shortest Path plugin's transport tables (wiki-derived) in
 * {@code src/test/resources/osrs/transport} by {@code TransportSnapshotTest}. Places the mod keeps on the
 * surface instead of underground (Keldagrim's rail network) use the surface equivalent and are marked approx.
 */
public final class Travel {
    /**
     * One destination: fare in coins, an optional skill level, the quests that must be complete, and an
     * optional item consumed per trip (balloon logs).
     */
    public record Dest(String name, int x, int y, int price, Skill skill, int level, List<String> quests, String item,
                       int itemCount) {
        public Dest {
            quests = quests == null ? List.of() : List.copyOf(quests);
        }

        static Dest of(String name, int x, int y, int price) {
            return new Dest(name, x, y, price, null, 0, List.of(), null, 0);
        }

        static Dest of(String name, int x, int y, int price, String... quests) {
            return new Dest(name, x, y, price, null, 0, List.of(quests), null, 0);
        }

        /** The first required quest, or null. */
        public String quest() {
            return quests.isEmpty() ? null : quests.get(0);
        }
    }

    /** A route menu: {@code verb} is how the trip is described ("sail", "fly", "ride", "paddle", "travel", "float", "cart"). */
    public record Route(String id, String verb, List<Dest> dests) {
    }

    /**
     * A travel NPC this class generates (charter crew, glider pilots, canoe stations, spirit trees, balloon
     * assistants, cart conductors): id, name, area, OSRS tile, route, a look key and its greeting.
     */
    public record Agent(String id, String name, String area, int x, int y, String route, String look, String line) {
    }

    private static final Map<String, Route> ROUTES = new LinkedHashMap<>();
    private static final List<Agent> AGENTS = new ArrayList<>();

    private Travel() {
    }

    public static List<Route> all() {
        return Collections.unmodifiableList(new ArrayList<>(ROUTES.values()));
    }

    public static Route get(String id) {
        return ROUTES.get(id);
    }

    public static List<Agent> agents() {
        return Collections.unmodifiableList(AGENTS);
    }

    /**
     * The fare actually charged: Jarvald takes players who have completed The Fremennik Trials to Waterbirth
     * Island for free (OSRS).
     */
    public static int fareFor(String routeId, Dest d, java.util.function.Predicate<String> questComplete) {
        if ("boat_rellekka_waterbirth".equals(routeId) && questComplete.test("fremennik_trials")) {
            return 0;
        }
        return d.price();
    }

    private static void route(String id, String verb, Dest... dests) {
        route(id, verb, List.of(dests));
    }

    private static void route(String id, String verb, List<Dest> dests) {
        if (ROUTES.put(id, new Route(id, verb, List.copyOf(dests))) != null) {
            throw new IllegalStateException("Duplicate route " + id);
        }
    }

    private static String slug(String name) {
        return name.toLowerCase().replaceAll("[^a-z]+", "_").replaceAll("^_|_$", "");
    }

    // ------------------------------------------------------------------ charter ships

    /**
     * Charter ports (the first eight keep their original order so route/NPC ids stay stable). Tiles are the
     * docks the Trader Crewmembers stand at, which are also where the ship lands. The Sailing-era ports Port Roberts
     * (a floating platform), Deepfin Point and The Summer Shore come last; they need 50 / 67 Sailing or Troubled Tortugans.
     */
    static final String[] CHARTER_PORTS = {"Port Sarim", "Catherby", "Brimhaven", "Karamja Ship Yard", "Port Khazard",
            "Port Phasmatys", "Mos Le'Harmless", "Corsair Cove", "Musa Point", "Port Tyras", "Prifddinas", "Port Piscarilius",
            "Land's End", "Civitas illa Fortis", "Aldarin", "Sunset Coast", "The Pandemonium", "Red Rock", "Port Roberts",
            "Deepfin Point", "The Summer Shore"};
    static final int[][] CHARTER_TILES = {{3038, 3192}, {2792, 3414}, {2760, 3238}, {3001, 3032}, {2674, 3144},
            {3702, 3503}, {3671, 2931}, {2589, 2851}, {2954, 3158}, {2142, 3122}, {2157, 3330}, {1808, 3679},
            {1496, 3403}, {1743, 3136}, {1455, 2968}, {1514, 2971}, {3064, 2999}, {2805, 2502}, {1871, 3309}, {1943, 2757},
            {3189, 2369}};
    /** Quest needed to charter a ship to each port (OSRS). */
    static final String[] CHARTER_QUESTS = {null, null, null, "monkey_madness_i", null, "priest_in_peril", "cabin_fever",
            null, null, "regicide", "song_of_the_elves", null, null, "children_of_the_sun", "children_of_the_sun",
            "children_of_the_sun", "pandemonium", "the_red_reef", null, null, "troubled_tortugans"};
    /** Sailing level needed to charter a ship to each port (OSRS: Port Roberts 50, Deepfin Point 67), 0 = none. */
    static final int[] CHARTER_SAILING = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 67, 0};
    static final String[] CHARTER_AREAS = {"Port Sarim", "Catherby", "Brimhaven", "Karamja", "Port Khazard", "Port Phasmatys",
            "Mos Le'Harmless", "Corsair Cove", "Musa Point", "Port Tyras", "Prifddinas", "Port Piscarilius", "Land's End",
            "Civitas illa Fortis", "Aldarin", "Sunset Coast", "The Pandemonium", "Red Rock", "Port Roberts", "Deepfin Point",
            "The Summer Shore"};

    /** OSRS charter fares (row = from, column = to); 0 = no charter on that route. */
    static final int[][] CHARTER_FARES = {
            {0, 1000, 1600, 400, 1280, 1300, 650, 1200, 0, 3200, 4800, 0, 0, 3000, 3100, 3100, 0, 1700, 3500, 2600, 1700}, // Port Sarim
            {1000, 0, 480, 1600, 1600, 3500, 1250, 1000, 480, 3200, 3560, 2000, 2200, 2400, 2500, 2500, 1200, 2000, 3400, 2200, 2000}, // Catherby
            {1600, 480, 0, 400, 400, 2900, 1950, 680, 480, 3200, 3450, 2000, 2200, 2400, 2500, 2500, 400, 1900, 3200, 2000, 1900}, // Brimhaven
            {400, 1600, 400, 0, 1600, 3200, 550, 800, 200, 3200, 4000, 2600, 2800, 3000, 3100, 3100, 200, 1600, 3600, 2800, 1600}, // Shipyard
            {1280, 1600, 400, 1600, 0, 4100, 550, 600, 400, 3200, 2800, 1800, 2000, 2200, 2300, 2300, 700, 1800, 3000, 1800, 1800}, // Port Khazard
            {1300, 3500, 2900, 3200, 4100, 0, 0, 4040, 1100, 3200, 5200, 4000, 4200, 4400, 4500, 4500, 1000, 2200, 4600, 3400, 2000}, // Port Phasmatys
            {650, 1250, 1950, 550, 550, 0, 0, 2040, 2050, 1600, 2475, 2100, 2200, 2300, 2350, 2350, 500, 1000, 2200, 1600, 900}, // Mos Le'Harmless
            {1200, 1000, 680, 800, 600, 4040, 2040, 0, 800, 3200, 1420, 1500, 1800, 2000, 2100, 2100, 800, 1500, 2200, 1600, 1600}, // Corsair Cove
            {0, 480, 480, 200, 400, 1100, 2050, 800, 0, 3200, 4400, 2500, 2700, 2900, 3000, 3000, 0, 1700, 3500, 2400, 1700}, // Musa Point
            {3200, 3200, 3200, 3200, 3200, 3200, 1600, 3200, 3200, 0, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200}, // Port Tyras
            {4800, 3560, 3450, 4000, 2800, 5200, 2475, 1420, 4400, 3200, 0, 1200, 1500, 1700, 1800, 1800, 3800, 2000, 600, 1200, 2200}, // Prifddinas
            {0, 2000, 2000, 2600, 1800, 4000, 2100, 1500, 2500, 3200, 1200, 0, 0, 1000, 1100, 1100, 2400, 2400, 1000, 1600, 2600}, // Port Piscarilius
            {0, 2200, 2200, 2800, 2000, 4200, 2200, 1800, 2700, 3200, 1500, 0, 0, 800, 900, 900, 2600, 2200, 800, 1400, 2400}, // Land's End
            {3000, 2400, 2400, 3000, 2200, 4400, 2300, 2000, 2900, 3200, 1700, 1000, 800, 0, 600, 500, 2800, 2000, 600, 1200, 2200}, // Civitas
            {3100, 2500, 2500, 3100, 2300, 4500, 2350, 2100, 3000, 3200, 1800, 1100, 900, 600, 0, 0, 3000, 2200, 1400, 1200, 2400}, // Aldarin
            {3100, 2500, 2500, 3100, 2300, 4500, 2350, 2100, 3000, 3200, 1800, 1100, 900, 500, 0, 0, 3000, 2200, 1400, 1200, 2400}, // Sunset Coast
            {0, 1200, 400, 200, 700, 1000, 500, 800, 0, 3200, 3800, 2400, 2600, 2800, 3000, 3000, 0, 1600, 3600, 2800, 1600}, // The Pandemonium
            {1700, 2000, 1900, 1600, 1800, 2200, 2000, 1500, 1700, 3200, 2000, 2400, 2200, 2000, 2200, 2200, 1600, 0, 2000, 1400, 1000}, // Red Rock
            {3500, 3400, 3200, 3600, 3000, 4600, 2200, 2200, 3500, 3200, 600, 1000, 800, 600, 1400, 1400, 3600, 2000, 0, 1000, 2500}, // Port Roberts
            {2600, 2200, 2000, 2800, 1800, 3400, 1600, 1600, 2400, 3200, 1200, 1600, 1400, 1200, 1200, 1200, 2800, 1400, 1000, 0, 1800}, // Deepfin Point
            {1700, 2000, 1900, 1600, 1800, 2000, 900, 1600, 1700, 3200, 2200, 2600, 2400, 2200, 2400, 2400, 1600, 1000, 2500, 1800, 0}, // The Summer Shore
    };

    /** OSRS charter fare between two ports (indices into {@link #CHARTER_PORTS}); 0 if no ship sails that route. */
    public static int charterFare(int from, int to) {
        return CHARTER_FARES[from][to];
    }

    /** Charter fare by port name (as in {@link #CHARTER_PORTS}, "Shipyard" accepted too), or -1 if unknown. */
    public static int charterFare(String from, String to) {
        int a = charterIndex(from);
        int b = charterIndex(to);
        return a < 0 || b < 0 || CHARTER_FARES[a][b] == 0 ? -1 : CHARTER_FARES[a][b];
    }

    static int charterIndex(String name) {
        String n = "Shipyard".equalsIgnoreCase(name) ? "Karamja Ship Yard" : name;
        for (int i = 0; i < CHARTER_PORTS.length; i++) {
            if (CHARTER_PORTS[i].equalsIgnoreCase(n)) {
                return i;
            }
        }
        return -1;
    }

    public static int charterPorts() {
        return CHARTER_PORTS.length;
    }

    public static String charterRoute(int port) {
        return "charter_" + slug(CHARTER_PORTS[port]);
    }

    // ------------------------------------------------------------------ canoes

    /**
     * River Lum canoe stations (index 4, the Wilderness Pond, is a landing spot only: OSRS has no station
     * there) and the arrival tiles. {@link #CANOE_POS} is each stop's position along the river.
     */
    static final String[] CANOE_STATIONS = {"Lumbridge", "Champions' Guild", "Barbarian Village", "Edgeville", "Wilderness Pond",
            "Ferox Enclave"};
    static final int[][] CANOE_TILES = {{3240, 3242}, {3199, 3344}, {3109, 3415}, {3128, 3503}, {3141, 3796}, {3154, 3638}};
    /** Tiles of the canoe stations themselves (null: no station). */
    static final int[][] CANOE_STATION_TILES = {{3243, 3237}, {3202, 3343}, {3112, 3411}, {3132, 3510}, null, {3154, 3630}};
    private static final int[] CANOE_POS = {0, 1, 2, 3, 5, 4};
    private static final boolean[] CANOE_WILDERNESS = {false, false, false, false, true, true};

    /** Kandarin canoe stations (River Ardougne), upstream to downstream. */
    static final String[] WEST_CANOE_STATIONS = {"Castle Wars", "Tree Gnome Village", "The Clock Tower", "Chaos Druid Tower",
            "Tree Gnome Stronghold"};
    static final int[][] WEST_CANOE_TILES = {{2436, 3134}, {2483, 3188}, {2577, 3261}, {2571, 3360}, {2523, 3408}};
    static final int[][] WEST_CANOE_STATION_TILES = {{2439, 3135}, {2485, 3192}, {2579, 3260}, {2573, 3358}, {2525, 3408}};

    /** Woodcutting level by stops travelled: log 12 (1), dugout 27 (2), stable dugout 42 (3), waka 57 (4+). */
    static int canoeLevelForStops(int stops) {
        return switch (stops) {
            case 1 -> 12;
            case 2 -> 27;
            case 3 -> 42;
            default -> 57;
        };
    }

    /**
     * Woodcutting level for a River Lum trip. Only a waka (57) can paddle into the Wilderness (Ferox Enclave or
     * the Wilderness Pond); from Ferox Enclave back down the river the level goes by stops again.
     */
    public static int canoeLevel(int from, int to) {
        if (CANOE_WILDERNESS[to] && (!CANOE_WILDERNESS[from] || to == 4)) {
            return 57;
        }
        return canoeLevelForStops(Math.abs(CANOE_POS[from] - CANOE_POS[to]));
    }

    public static String canoeRoute(int station) {
        return "canoe_" + slug(CANOE_STATIONS[station]);
    }

    public static String westCanoeRoute(int station) {
        return "canoe_west_" + slug(WEST_CANOE_STATIONS[station]);
    }

    // ------------------------------------------------------------------ gliders

    /** Glider stations; Lemanto Andra (Digsite) can be flown to but has no glider leaving it. */
    static final String[] GLIDER_NAMES = {"Ta Quir Priw (Gnome Stronghold)", "Sindarpos (White Wolf Mountain)",
            "Kar-Hewo (Al Kharid)", "Lemantolly Undri (Feldip Hills)", "Gandius (Karamja)", "Lemanto Andra (Digsite)",
            "Ookookolly Undri (Ape Atoll)"};
    /** Landing tiles. */
    static final int[][] GLIDER_TILES = {{2465, 3501}, {2850, 3498}, {3284, 3210}, {2550, 2970}, {2971, 2968}, {3321, 3429},
            {2711, 2803}};
    /** Where each pilot stands (null: no pilot). */
    static final int[][] GLIDER_PILOT_TILES = {{2466, 3501}, {2847, 3498}, {3284, 3212}, {2544, 2973}, {2969, 2973}, null,
            {2710, 2802}};
    static final String[] GLIDER_PILOTS = {"Captain Errdo", "Captain Bleemadge", "Captain Dalbur", "Gnormadium Avlafrim",
            "Captain Klemfoodle", null, "Captain Shoracks"};
    static final String[] GLIDER_AREAS = {"Tree Gnome Stronghold", "White Wolf Mountain", "Al Kharid", "Feldip Hills", "Karamja",
            "Digsite", "Ape Atoll"};
    /** Quests beyond The Grand Tree needed for a station (OSRS: One Small Favour, Monkey Madness II). */
    static final String[] GLIDER_QUESTS = {null, null, null, "one_small_favour", null, null, "monkey_madness_ii"};

    public static String gliderRoute(int station) {
        return "glider_" + station;
    }

    // ------------------------------------------------------------------ spirit trees

    /** A spirit tree: name, OSRS tile (the tree, where travellers arrive) and the quests needed to use it. */
    public record SpiritTree(String id, String name, int x, int y, List<String> quests) {
    }

    /**
     * The naturally growing spirit trees (all need Tree Gnome Village). Player-grown trees at the farming
     * patches (Port Sarim, Etceteria, Brimhaven, Hosidius, Farming Guild) are handled by the skilling module
     * and can travel to these. Prifddinas' tree stands in the elven city, which this world builds at its OSRS
     * tile (3274, 6123).
     */
    public static final List<SpiritTree> SPIRIT_TREES = List.of(
            new SpiritTree("tree_gnome_village", "Tree Gnome Village", 2542, 3170, List.of("tree_gnome_village")),
            new SpiritTree("gnome_stronghold", "Gnome Stronghold", 2461, 3444, List.of("tree_gnome_village", "the_grand_tree")),
            new SpiritTree("battlefield_of_khazard", "Battlefield of Khazard", 2555, 3259, List.of("tree_gnome_village")),
            new SpiritTree("grand_exchange", "Grand Exchange", 3185, 3508, List.of("tree_gnome_village")),
            new SpiritTree("feldip_hills", "Feldip Hills", 2488, 2850, List.of("tree_gnome_village")),
            new SpiritTree("poison_waste", "Poison Waste", 2339, 3109, List.of("tree_gnome_village", "the_path_of_glouphrie")),
            new SpiritTree("prifddinas", "Prifddinas", 3274, 6123, List.of("tree_gnome_village", "song_of_the_elves")));

    public static String spiritTreeRoute(String treeId) {
        return "spirit_tree_" + treeId;
    }

    /** Destinations offered by a spirit tree at {@code fromId} (null: a player-grown tree, which reaches every natural tree). */
    public static List<Dest> spiritTreeDests(String fromId) {
        List<String> base = new ArrayList<>();
        for (SpiritTree t : SPIRIT_TREES) {
            if (t.id().equals(fromId)) {
                base.addAll(t.quests());
            }
        }
        List<Dest> out = new ArrayList<>();
        for (SpiritTree t : SPIRIT_TREES) {
            if (t.id().equals(fromId)) {
                continue;
            }
            List<String> q = new ArrayList<>(base);
            for (String s : t.quests()) {
                if (!q.contains(s)) {
                    q.add(s);
                }
            }
            out.add(new Dest(t.name(), t.x(), t.y(), 0, null, 0, q, null, 0));
        }
        return out;
    }

    // ------------------------------------------------------------------ balloons

    /**
     * Balloon transport system (Enlightened Journey): each flight burns one log of the destination's kind and
     * needs its Firemaking level. OSRS also asks for 10 logs to unlock the four later destinations once; that
     * one-off deposit is not modelled (approx).
     */
    static final String[] BALLOON_STOPS = {"Entrana", "Taverley", "Crafting Guild", "Varrock", "Castle Wars", "Grand Tree"};
    static final int[][] BALLOON_TILES = {{2808, 3354}, {2941, 3420}, {2923, 3302}, {3299, 3482}, {2459, 3110}, {2478, 3456}};
    static final int[][] BALLOON_BASKETS = {{2806, 3357}, {2939, 3423}, {2924, 3303}, {3296, 3483}, {2461, 3111}, {2481, 3459}};
    static final String[] BALLOON_LOGS = {"logs", "logs", "oak_logs", "willow_logs", "yew_logs", "magic_logs"};
    static final int[] BALLOON_FIREMAKING = {20, 20, 30, 40, 50, 60};
    static final String[] BALLOON_ASSISTANTS = {"Auguste", "Assistant Serf", "Assistant Brock", "Assistant Marrow",
            "Assistant Le Smith", "Assistant Stan"};

    public static String balloonRoute(int stop) {
        return "balloon_" + slug(BALLOON_STOPS[stop]);
    }

    // ------------------------------------------------------------------ Keldagrim minecarts

    /**
     * Keldagrim's rail network runs underground in OSRS; this world keeps Keldagrim on the surface, so the
     * Keldagrim end and the Ice Mountain / White Wolf Mountain stations use surface tiles (approx: OSRS y - 6400).
     */
    static final int[] KELDAGRIM_STATION = {2742, 3712}; // approx (relocated Keldagrim)
    static final int[] GE_TRAPDOOR = {3141, 3504};

    static {
        // Boats with fixed fares (OSRS ships between towns).
        route("boat_port_sarim_karamja", "sail", Dest.of("Musa Point, Karamja", 2956, 3146, 30),
                Dest.of("The Pandemonium", 3064, 3003, 30, "pandemonium"));
        route("boat_karamja_port_sarim", "sail", Dest.of("Port Sarim", 3029, 3217, 30),
                Dest.of("The Pandemonium", 3064, 3003, 30, "pandemonium"));
        route("boat_pandemonium", "sail", Dest.of("Port Sarim", 3029, 3217, 30, "pandemonium"),
                Dest.of("Musa Point, Karamja", 2956, 3146, 30, "pandemonium"));
        route("boat_ardougne_brimhaven", "sail", Dest.of("Brimhaven", 2772, 3234, 30), Dest.of("Rimmington", 2915, 3225, 30));
        route("boat_brimhaven_ardougne", "sail", Dest.of("Ardougne", 2683, 3271, 30), Dest.of("Rimmington", 2915, 3225, 30));
        route("boat_rimmington", "sail", Dest.of("Ardougne", 2683, 3271, 30), Dest.of("Brimhaven", 2772, 3234, 30));
        route("boat_rimmington_corsair_cove", "sail", Dest.of("Corsair Cove", 2578, 2840, 0, "the_corsair_curse"));
        route("boat_corsair_cove_rimmington", "sail", Dest.of("Rimmington", 2909, 3226, 0, "the_corsair_curse"));
        route("boat_void_outpost", "sail", Dest.of("Void Knights' Outpost", 2659, 2676, 0));
        route("boat_void_port_sarim", "sail", Dest.of("Port Sarim", 3041, 3202, 0));
        route("boat_rellekka_miscellania", "sail", Dest.of("Miscellania", 2581, 3847, 0, "fremennik_trials"));
        route("boat_miscellania_rellekka", "sail", Dest.of("Rellekka", 2629, 3693, 0, "fremennik_trials"));
        // Jarvald: 1,000 coins, or free once The Fremennik Trials is complete (TravelService waives the fare).
        route("boat_rellekka_waterbirth", "sail", Dest.of("Waterbirth Island", 2544, 3761, 1000));
        route("boat_waterbirth_rellekka", "sail", Dest.of("Rellekka", 2621, 3683, 0));
        route("boat_rellekka_jatizso", "sail", Dest.of("Jatizso", 2421, 3781, 0, "fremennik_trials"));
        route("boat_jatizso_rellekka", "sail", Dest.of("Rellekka", 2644, 3709, 0, "fremennik_trials"));
        route("boat_rellekka_neitiznot", "sail", Dest.of("Neitiznot", 2311, 3781, 0, "fremennik_trials"));
        route("boat_neitiznot_rellekka", "sail", Dest.of("Rellekka", 2644, 3710, 0, "fremennik_trials"));
        route("boat_sunset_coast_aldarin", "sail", Dest.of("Aldarin", 1443, 2977, 20));
        route("boat_aldarin_sunset_coast", "sail", Dest.of("Sunset Coast", 1494, 2985, 20));

        // Charter ships: every route the OSRS charter table has.
        for (int i = 0; i < CHARTER_PORTS.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < CHARTER_PORTS.length; j++) {
                if (i != j && CHARTER_FARES[i][j] > 0) {
                    String name = j == 3 ? "Shipyard" : CHARTER_PORTS[j];
                    dests.add(new Dest(name, CHARTER_TILES[j][0], CHARTER_TILES[j][1], CHARTER_FARES[i][j],
                            CHARTER_SAILING[j] > 0 ? Skill.SAILING : null, CHARTER_SAILING[j],
                            CHARTER_QUESTS[j] == null ? List.of() : List.of(CHARTER_QUESTS[j]), null, 0));
                }
            }
            route(charterRoute(i), "sail", dests);
            AGENTS.add(new Agent("npc_" + charterRoute(i).replace("charter_", "charter_crew_"), "Trader Crewmember",
                    CHARTER_AREAS[i], CHARTER_TILES[i][0] + 1, CHARTER_TILES[i][1] + 1, charterRoute(i), "sailor",
                    "Do you want to charter a ship? Tell me where you want to go."));
        }

        // Gnome gliders (free; The Grand Tree, plus the station's own quest at both ends).
        for (int i = 0; i < GLIDER_NAMES.length; i++) {
            if (GLIDER_PILOTS[i] == null) {
                continue;
            }
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < GLIDER_NAMES.length; j++) {
                if (i != j) {
                    List<String> q = new ArrayList<>(List.of("the_grand_tree"));
                    for (String s : new String[]{GLIDER_QUESTS[i], GLIDER_QUESTS[j]}) {
                        if (s != null && !q.contains(s)) {
                            q.add(s);
                        }
                    }
                    dests.add(new Dest(GLIDER_NAMES[j], GLIDER_TILES[j][0], GLIDER_TILES[j][1], 0, null, 0, q, null, 0));
                }
            }
            route(gliderRoute(i), "fly", dests);
            AGENTS.add(new Agent("npc_glider_pilot_" + i, GLIDER_PILOTS[i], GLIDER_AREAS[i], GLIDER_PILOT_TILES[i][0],
                    GLIDER_PILOT_TILES[i][1], gliderRoute(i), "gnome", "Would you like to take the glider somewhere?"));
        }

        // Magic carpets (200 coins; Uzer needs The Golem, Sophanem and Menaphos Icthlarin's Little Helper).
        route("carpet_shantay", "ride", Dest.of("Pollnivneach", 3350, 3001, 200), Dest.of("Uzer", 3467, 3110, 200, "the_golem"),
                Dest.of("Bedabin Camp", 3182, 3043, 200));
        route("carpet_pollnivneach_north", "ride", Dest.of("Shantay Pass", 3311, 3109, 200));
        route("carpet_pollnivneach_south", "ride", Dest.of("Nardah", 3401, 2918, 200),
                Dest.of("Sophanem", 3287, 2813, 200, "icthlarins_little_helper"),
                Dest.of("Menaphos", 3243, 2813, 200, "icthlarins_little_helper"));
        route("carpet_nardah", "ride", Dest.of("Pollnivneach", 3348, 2942, 200));
        route("carpet_sophanem", "ride", Dest.of("Pollnivneach", 3348, 2942, 200, "icthlarins_little_helper"));
        route("carpet_menaphos", "ride", Dest.of("Pollnivneach", 3348, 2942, 200, "icthlarins_little_helper"));
        route("carpet_bedabin", "ride", Dest.of("Shantay Pass", 3311, 3109, 200));
        route("carpet_uzer", "ride", Dest.of("Shantay Pass", 3311, 3109, 200, "the_golem"));

        // River Lum canoes (free, Woodcutting level by distance; only a waka enters the Wilderness).
        for (int i = 0; i < CANOE_STATIONS.length; i++) {
            if (CANOE_STATION_TILES[i] == null) {
                continue;
            }
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < CANOE_STATIONS.length; j++) {
                if (i != j) {
                    dests.add(new Dest(CANOE_STATIONS[j], CANOE_TILES[j][0], CANOE_TILES[j][1], 0, Skill.WOODCUTTING,
                            canoeLevel(i, j), List.of(), null, 0));
                }
            }
            route(canoeRoute(i), "paddle", dests);
            AGENTS.add(new Agent("npc_" + canoeRoute(i).replace("canoe_", "canoe_station_"), "Canoe station", CANOE_STATIONS[i],
                    CANOE_STATION_TILES[i][0], CANOE_STATION_TILES[i][1], canoeRoute(i), "canoe",
                    "A canoe station. Chop a canoe and paddle down the river - the further you go, the better the canoe you need."));
        }
        for (int i = 0; i < WEST_CANOE_STATIONS.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < WEST_CANOE_STATIONS.length; j++) {
                if (i != j) {
                    dests.add(new Dest(WEST_CANOE_STATIONS[j], WEST_CANOE_TILES[j][0], WEST_CANOE_TILES[j][1], 0, Skill.WOODCUTTING,
                            canoeLevelForStops(Math.abs(i - j)), List.of(), null, 0));
                }
            }
            route(westCanoeRoute(i), "paddle", dests);
            AGENTS.add(new Agent("npc_" + westCanoeRoute(i).replace("canoe_west_", "canoe_station_west_"), "Canoe station",
                    WEST_CANOE_STATIONS[i], WEST_CANOE_STATION_TILES[i][0], WEST_CANOE_STATION_TILES[i][1], westCanoeRoute(i), "canoe",
                    "A canoe station on the River Ardougne. The further you go, the better the canoe you need."));
        }

        // Spirit trees.
        for (SpiritTree t : SPIRIT_TREES) {
            route(spiritTreeRoute(t.id()), "travel", spiritTreeDests(t.id()));
            AGENTS.add(new Agent("npc_spirit_tree_" + t.id(), "Spirit tree", t.name(), t.x(), t.y(), spiritTreeRoute(t.id()), "tree",
                    "Hello gnome friend. Where would you like to go?"));
        }

        // Balloon transport system (Enlightened Journey).
        for (int i = 0; i < BALLOON_STOPS.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < BALLOON_STOPS.length; j++) {
                if (i != j) {
                    dests.add(new Dest(BALLOON_STOPS[j], BALLOON_TILES[j][0], BALLOON_TILES[j][1], 0, Skill.FIREMAKING,
                            BALLOON_FIREMAKING[j], List.of("enlightened_journey"), BALLOON_LOGS[j], 1));
                }
            }
            route(balloonRoute(i), "float", dests);
            AGENTS.add(new Agent("npc_" + balloonRoute(i).replace("balloon_", "balloon_assistant_"), BALLOON_ASSISTANTS[i],
                    BALLOON_STOPS[i], BALLOON_BASKETS[i][0] + 1, BALLOON_BASKETS[i][1] - 1, balloonRoute(i), "balloon",
                    "Fancy a flight? It costs one log of the right kind for where you're going."));
        }

        // Keldagrim minecarts (The Giant Dwarf). Grand Exchange is free; Ice Mountain 150 coins, White Wolf Mountain
        // 100 coins and Fishing Contest.
        route("minecart_grand_exchange", "cart", Dest.of("Keldagrim", KELDAGRIM_STATION[0], KELDAGRIM_STATION[1], 0, "the_giant_dwarf"));
        route("minecart_keldagrim", "cart", Dest.of("Grand Exchange", GE_TRAPDOOR[0], GE_TRAPDOOR[1], 0, "the_giant_dwarf"),
                Dest.of("Ice Mountain", 2995, 3435, 150, "the_giant_dwarf"), // approx (surface above the dwarven mine)
                Dest.of("White Wolf Mountain", 2876, 3468, 100, "the_giant_dwarf", "fishing_contest")); // approx
        route("minecart_ice_mountain", "cart", Dest.of("Keldagrim", KELDAGRIM_STATION[0], KELDAGRIM_STATION[1], 150, "the_giant_dwarf"));
        route("minecart_white_wolf_mountain", "cart",
                Dest.of("Keldagrim", KELDAGRIM_STATION[0], KELDAGRIM_STATION[1], 100, "the_giant_dwarf", "fishing_contest"));
        AGENTS.add(new Agent("npc_cart_conductor_grand_exchange", "Cart conductor", "Grand Exchange", GE_TRAPDOOR[0] - 1,
                GE_TRAPDOOR[1], "minecart_grand_exchange", "dwarf", "Down the trapdoor and onto the train to Keldagrim!"));
        AGENTS.add(new Agent("npc_cart_conductor_keldagrim", "Cart conductor", "Keldagrim", KELDAGRIM_STATION[0] + 1,
                KELDAGRIM_STATION[1], "minecart_keldagrim", "dwarf", "Where are you headed? The carts go all over."));
        AGENTS.add(new Agent("npc_cart_conductor_ice_mountain", "Cart conductor", "Ice Mountain", 2996, 3435,
                "minecart_ice_mountain", "dwarf", "Back to Keldagrim? That'll be 150 coins."));
        AGENTS.add(new Agent("npc_cart_conductor_white_wolf_mountain", "Cart conductor", "White Wolf Mountain", 2877, 3468,
                "minecart_white_wolf_mountain", "dwarf", "Back to Keldagrim? That'll be 100 coins."));

        // Round 40 (achievement diary tasks). The Shilo Village cart system after Shilo Village: Hajedy's cart south of the
        // Brimhaven dock and Vigroy west of the Shilo Village bank. OSRS's fare scales with the coins carried (10-200 coins);
        // this is the "Pay-fare" price, which is always 200. // approx
        route("cart_brimhaven_shilo", "cart", Dest.of("Shilo Village", 2832, 2954, 200, "shilo_village"));
        route("cart_shilo_brimhaven", "cart", Dest.of("Brimhaven", 2777, 3212, 200, "shilo_village"));
        AGENTS.add(new Agent("npc_cart_hajedy", "Hajedy's cart", "Brimhaven", 2780, 3213, "cart_brimhaven_shilo", "canoe",
                "Hajedy's cart to Shilo Village. A ride costs 200 coins."));
        AGENTS.add(new Agent("npc_cart_vigroy", "Vigroy", "Shilo Village", 2834, 2955, "cart_shilo_brimhaven", "sailor",
                "My cart can take you to Brimhaven. 200 coins."));
        // The Lady of the Waves: Captain Shanks's ship from south of Cairn Isle to Port Khazard or Port Sarim (shortest-path tiles;
        // OSRS needs one of his ship tickets, not modelled). // approx
        route("boat_lady_of_the_waves", "sail", Dest.of("Port Khazard", 2680, 3150, 0, "shilo_village"),
                Dest.of("Port Sarim", 3050, 3192, 0, "shilo_village"));
        AGENTS.add(new Agent("npc_captain_shanks", "Captain Shanks", "Cairn Isle", 2763, 2960, "boat_lady_of_the_waves", "sailor",
                "The Lady of the Waves sails to Port Khazard and Port Sarim."));
        // The Hollows' swamp boat to Mort'ton (free after Nature Spirit) and back (10 coins; shortest-path tiles).
        route("boat_swamp_hollows", "sail", Dest.of("Mort'ton", 3522, 3285, 0, "nature_spirit"));
        route("boat_swamp_mortton", "sail", Dest.of("The Hollows", 3498, 3380, 10, "nature_spirit"));
        AGENTS.add(new Agent("npc_swamp_boat_hollows", "Swamp boat", "The Hollows", 3500, 3379, "boat_swamp_hollows", "canoe",
                "A swamp boat. It can carry you through the Mort Myre Swamp to Mort'ton."));
        AGENTS.add(new Agent("npc_swamp_boat_mortton", "Swamp boat", "Mort'ton", 3523, 3283, "boat_swamp_mortton", "canoe",
                "A swamp boat back to the Hollows. 10 coins."));
    }
}
