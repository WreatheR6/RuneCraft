package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paid and free transport offered by NPCs: boats, charter ships, gnome gliders, magic carpets and
 * canoes. Destinations are OSRS tiles. Fares are OSRS fares (charter fares, which OSRS lists per
 * route, are approximated from the sailing distance).
 */
public final class Travel {
    /** One destination: fare in coins, an optional skill level and an optional quest that must be complete. */
    public record Dest(String name, int x, int y, int price, Skill skill, int level, String quest) {
        static Dest of(String name, int x, int y, int price) {
            return new Dest(name, x, y, price, null, 0, null);
        }
    }

    /** A route menu: {@code verb} is how the trip is described ("sail", "fly", "ride", "paddle"). */
    public record Route(String id, String verb, List<Dest> dests) {
    }

    private static final Map<String, Route> ROUTES = new LinkedHashMap<>();

    private Travel() {
    }

    public static List<Route> all() {
        return Collections.unmodifiableList(new ArrayList<>(ROUTES.values()));
    }

    public static Route get(String id) {
        return ROUTES.get(id);
    }

    private static void route(String id, String verb, Dest... dests) {
        if (ROUTES.put(id, new Route(id, verb, List.of(dests))) != null) {
            throw new IllegalStateException("Duplicate route " + id);
        }
    }

    // ------------------------------------------------------------------ charter ships

    /** Charter ports: name and the tile the ship docks at. */
    static final String[] CHARTER_PORTS = {"Port Sarim", "Catherby", "Brimhaven", "Karamja Ship Yard", "Port Khazard",
            "Port Phasmatys", "Mos Le'Harmless", "Corsair Cove"};
    static final int[][] CHARTER_TILES = {{3038, 3192}, {2792, 3414}, {2760, 3238}, {2998, 3032}, {2674, 3144},
            {3702, 3503}, {3671, 2931}, {2587, 2851}};

    /** Charter fare between two ports: about 2.7 coins per tile sailed, rounded to 10 (approx. of the OSRS table). */
    public static int charterFare(int from, int to) {
        double dx = CHARTER_TILES[from][0] - CHARTER_TILES[to][0];
        double dy = CHARTER_TILES[from][1] - CHARTER_TILES[to][1];
        int fare = (int) Math.round(Math.sqrt(dx * dx + dy * dy) * 2.7 / 10.0) * 10;
        return Math.max(200, fare);
    }

    public static String charterRoute(int port) {
        return "charter_" + CHARTER_PORTS[port].toLowerCase().replaceAll("[^a-z]+", "_").replaceAll("_$", "");
    }

    // ------------------------------------------------------------------ canoes

    static final String[] CANOE_STATIONS = {"Lumbridge", "Champions' Guild", "Barbarian Village", "Edgeville", "Wilderness Pond"};
    static final int[][] CANOE_TILES = {{3241, 3235}, {3202, 3344}, {3112, 3409}, {3132, 3510}, {3139, 3796}};

    /**
     * Woodcutting level for a canoe trip of {@code stops} stations: log 12 (1 stop), dugout 27 (2),
     * stable dugout 42 (3), waka 57 (any distance; the only canoe that reaches the Wilderness Pond).
     */
    public static int canoeLevel(int from, int to) {
        if (from == 4 || to == 4) {
            return 57;
        }
        return switch (Math.abs(from - to)) {
            case 1 -> 12;
            case 2 -> 27;
            case 3 -> 42;
            default -> 57;
        };
    }

    public static String canoeRoute(int station) {
        return "canoe_" + CANOE_STATIONS[station].toLowerCase().replaceAll("[^a-z]+", "_").replaceAll("_$", "");
    }

    // ------------------------------------------------------------------ gliders

    static final String[] GLIDER_NAMES = {"Ta Quir Priw (Gnome Stronghold)", "Sindarpos (White Wolf Mountain)",
            "Kar-Hewo (Al Kharid)", "Lemantolly Undri (Feldip Hills)", "Gandius (Karamja)"};
    static final int[][] GLIDER_TILES = {{2465, 3501}, {2848, 3497}, {3284, 3213}, {2544, 2970}, {2970, 2972}};

    public static String gliderRoute(int station) {
        return "glider_" + station;
    }

    static {
        // Boats with fixed fares.
        route("boat_port_sarim_karamja", "sail", Dest.of("Musa Point, Karamja", 2956, 3143, 30));
        route("boat_karamja_port_sarim", "sail", Dest.of("Port Sarim", 3029, 3217, 30));
        route("boat_ardougne_brimhaven", "sail", Dest.of("Brimhaven", 2772, 3234, 30));
        route("boat_brimhaven_ardougne", "sail", Dest.of("Ardougne", 2683, 3271, 30));
        route("boat_void_outpost", "sail", Dest.of("Void Knights' Outpost", 2659, 2676, 0));
        route("boat_void_port_sarim", "sail", Dest.of("Port Sarim", 3041, 3202, 0));
        route("boat_rellekka_miscellania", "sail",
                new Dest("Miscellania", 2581, 3845, 0, null, 0, "fremennik_trials"));
        route("boat_miscellania_rellekka", "sail", Dest.of("Rellekka", 2629, 3693, 0));
        route("boat_rellekka_waterbirth", "sail", Dest.of("Waterbirth Island", 2544, 3759, 1000));
        route("boat_waterbirth_rellekka", "sail", Dest.of("Rellekka", 2620, 3686, 0));

        // Charter ships: every port to every other port.
        for (int i = 0; i < CHARTER_PORTS.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < CHARTER_PORTS.length; j++) {
                if (i != j) {
                    dests.add(Dest.of(CHARTER_PORTS[j], CHARTER_TILES[j][0], CHARTER_TILES[j][1], charterFare(i, j)));
                }
            }
            route(charterRoute(i), "sail", dests.toArray(new Dest[0]));
        }

        // Gnome gliders (free, need The Grand Tree).
        for (int i = 0; i < GLIDER_NAMES.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < GLIDER_NAMES.length; j++) {
                if (i != j) {
                    dests.add(new Dest(GLIDER_NAMES[j], GLIDER_TILES[j][0], GLIDER_TILES[j][1], 0, null, 0, "the_grand_tree"));
                }
            }
            route(gliderRoute(i), "fly", dests.toArray(new Dest[0]));
        }

        // Magic carpets (200 coins each).
        route("carpet_shantay", "ride", Dest.of("Pollnivneach", 3349, 3003, 200), Dest.of("Uzer", 3469, 3113, 200),
                Dest.of("Bedabin Camp", 3181, 3045, 200));
        route("carpet_pollnivneach_north", "ride", Dest.of("Shantay Pass", 3308, 3110, 200));
        route("carpet_pollnivneach_south", "ride", Dest.of("Nardah", 3401, 2916, 200), Dest.of("Sophanem", 3285, 2813, 200),
                Dest.of("Menaphos", 3245, 2813, 200));
        route("carpet_nardah", "ride", Dest.of("Pollnivneach", 3351, 2942, 200));
        route("carpet_sophanem", "ride", Dest.of("Pollnivneach", 3351, 2942, 200));
        route("carpet_bedabin", "ride", Dest.of("Shantay Pass", 3308, 3110, 200));
        route("carpet_uzer", "ride", Dest.of("Shantay Pass", 3308, 3110, 200));

        // River Lum canoes (free, Woodcutting level by distance).
        for (int i = 0; i < CANOE_STATIONS.length; i++) {
            List<Dest> dests = new ArrayList<>();
            for (int j = 0; j < CANOE_STATIONS.length; j++) {
                if (i != j) {
                    dests.add(new Dest(CANOE_STATIONS[j], CANOE_TILES[j][0], CANOE_TILES[j][1], 0, Skill.WOODCUTTING,
                            canoeLevel(i, j), null));
                }
            }
            route(canoeRoute(i), "paddle", dests.toArray(new Dest[0]));
        }
    }
}
