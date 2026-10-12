package com.runecraft.osrs.content.extras;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Playable Sailing (released November 2025): boats of three tiers (raft, skiff, sloop) bought or
 * built at a shipwright, sailed as a vehicle on water with speed by tier; ports with charter docks
 * (paid travel for anyone) and voyages (sail your own boat port to port for Sailing XP); level
 * requirements for boats, ports and features. Port tasks, trawling, salvaging and sea charting
 * live in the skilling module and work from these boats too. Boat levels (raft 1, skiff 15, sloop 50)
 * and shipwright prices (1,000 / 15,000 / 200,000) and the docking levels of the ports follow the
 * OSRS Wiki; voyage XP, charter fares and build materials are approx. Sailing your own boat needs
 * the Pandemonium quest.
 */
public final class Seafaring {
    /**
     * A boat tier: Sailing level to sail it, deed item, shipwright price, materials to have it built
     * instead (plus the Construction level), speed factor relative to a raft, voyage XP multiplier
     * and the vanilla boat look.
     */
    public record BoatTier(String id, String name, int level, String deed, int price, int buildLevel, Map<String, Integer> build,
                           double speed, double xpMultiplier, String variant, int color) {
    }

    private static Map<String, Integer> mats(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Map.copyOf(m);
    }

    public static final List<BoatTier> BOATS = List.of(
            new BoatTier("raft", "Raft", 1, "ext_raft_deed", 1_000, 1, mats("logs", 10, "rope", 4), 1.00, 1.0, "BAMBOO",
                    0xC0B060), // approx build materials
            new BoatTier("skiff", "Skiff", 15, "ext_skiff_deed", 15_000, 30, mats("oak_plank", 30, "iron_nails", 40, "bolt_of_cloth", 4,
                    "rope", 6), 1.02, 1.25, "OAK", 0x9A7A4A), // approx build level/materials
            new BoatTier("sloop", "Sloop", 50, "ext_sloop_deed", 200_000, 50, mats("teak_plank", 60, "steel_nails", 80,
                    "bolt_of_cloth", 10, "rope", 12), 1.04, 1.5, "DARK_OAK", 0x4A3A2A)); // approx build level/materials

    /** The quest that teaches Sailing and hands out the first boat. */
    public static final String SAILING_QUEST = "pandemonium";

    /** A port with a charter dock: id, OSRS coordinates of the dock, Sailing level to sail there yourself. */
    public record Port(String id, String name, int x, int y, int level, boolean shipwright) {
        public String place() {
            return "ext_port_" + id;
        }
    }

    /** Docking levels from the OSRS Wiki unless marked approx. */
    public static final List<Port> PORTS = List.of(
            new Port("port_sarim", "Port Sarim", 3029, 3217, 1, true),
            new Port("pandemonium", "The Pandemonium", 3050, 2972, 1, false),
            new Port("musa_point", "Musa Point", 2954, 3146, 10, false),
            new Port("catherby", "Catherby", 2804, 3421, 20, false),
            new Port("brimhaven", "Brimhaven", 2772, 3225, 25, false),
            new Port("port_khazard", "Port Khazard", 2674, 3149, 30, false),
            new Port("rellekka", "Rellekka", 2629, 3693, 62, true),
            new Port("port_phasmatys", "Port Phasmatys", 3702, 3487, 45, false), // approx level
            new Port("port_tyras", "Port Tyras", 2142, 3122, 50, false), // approx level
            new Port("corsair_cove", "Corsair Cove", 2572, 2861, 40, false),
            new Port("port_piscarilius", "Port Piscarilius", 1824, 3691, 15, true),
            new Port("lands_end", "Land's End", 1504, 3399, 5, false),
            new Port("sunset_coast", "Sunset Coast", 1514, 2972, 44, false), // approx dock
            new Port("aldarin", "Aldarin", 1393, 2928, 46, true), // approx dock
            new Port("etceteria", "Etceteria", 2612, 3895, 65, false)); // approx dock

    /** Sailing level requirements for features (for the /sailing info list and gating). approx where noted. */
    public static final Map<String, Integer> FEATURES = new LinkedHashMap<>();

    static {
        for (BoatTier b : BOATS) {
            FEATURES.put("Sail a " + b.name().toLowerCase(), b.level());
        }
        FEATURES.put("Charter ships", 0);
        FEATURES.put("Port tasks (courier)", 1);
        FEATURES.put("Sea charting", 1);
        FEATURES.put("Small shipwreck salvaging", 15);
        FEATURES.put("Fisherman's shipwreck salvaging", 26);
        FEATURES.put("Barracuda shipwreck salvaging", 35);
        FEATURES.put("Large shipwreck salvaging", 53);
        FEATURES.put("Pirate shipwreck salvaging", 64);
        FEATURES.put("Mercenary shipwreck salvaging", 73);
        FEATURES.put("Fremennik shipwreck salvaging", 80);
        FEATURES.put("Merchant shipwreck salvaging", 87);
        for (Port p : PORTS) {
            FEATURES.put("Voyage to " + p.name(), p.level());
        }
    }

    private Seafaring() {
    }

    public static BoatTier boat(String id) {
        for (BoatTier b : BOATS) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    public static BoatTier byDeed(String deed) {
        for (BoatTier b : BOATS) {
            if (b.deed().equals(deed)) {
                return b;
            }
        }
        return null;
    }

    public static Port port(String id) {
        for (Port p : PORTS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** OSRS tiles between two ports (Chebyshev, like OSRS distance). */
    public static int tiles(Port a, Port b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.y() - b.y()));
    }

    /** Charter fare in coins (scales with distance like the OSRS charter ships). approx */
    public static int charterFare(Port from, Port to) {
        return 200 + tiles(from, to) * 2;
    }

    /** Sailing XP for completing a voyage between two ports in a boat of the given tier. approx */
    public static double voyageXp(Port from, Port to, BoatTier boat) {
        double base = 30 + tiles(from, to) * 0.6 + to.level() * 4;
        return Math.round(base * boat.xpMultiplier() * 10) / 10.0;
    }

    /** Voyage cooldown so a pair of nearby ports can't be farmed back and forth instantly (game ticks). */
    public static final int VOYAGE_MIN_TICKS = 50;

    /** Why a player can't sail to a port in a boat, or null. */
    public static String canVoyage(int sailingLevel, BoatTier boat, Port from, Port to) {
        if (boat == null) {
            return "You need to be sailing your own boat to take a voyage.";
        }
        if (from == to) {
            return "You're already at " + to.name() + ".";
        }
        if (sailingLevel < boat.level()) {
            return "You need a Sailing level of " + boat.level() + " to sail " + Poh.article(boat.name().toLowerCase()) + ".";
        }
        if (sailingLevel < to.level()) {
            return "You need a Sailing level of " + to.level() + " to sail to " + to.name() + ".";
        }
        return null;
    }

    /** Terminal speed of a vanilla boat (blocks/tick) scaled per tick by {@code factor}: v = a*f / (1 - fr*f). */
    public static double terminalSpeed(double factor) {
        double accel = 0.04;
        double friction = 0.9;
        return accel * factor / (1 - friction * factor);
    }
}
