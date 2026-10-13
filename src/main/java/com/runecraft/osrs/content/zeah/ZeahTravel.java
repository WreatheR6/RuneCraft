package com.runecraft.osrs.content.zeah;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport into and around Zeah and Varlamore: Veos' ship from Port Sarim to Port Piscarilius
 * and Land's End, the Lovakengj minecart network, the Varlamore quetzal transport system and
 * Primio's passage between Varrock and Civitas illa Fortis.
 *
 * <p>Each stop is a place id (a module PlaceDef or a Landmark enum name); travellers arrive next
 * to its centre, or at the stop's OSRS tile where one is given. Fares are the OSRS ones (minecarts cost 20 coins
 * until The Forsaken Tower is complete; Veos' ship and the quetzals are free). Checked against the transport
 * snapshot by TransportSnapshotTest.
 */
public final class ZeahTravel {
    private ZeahTravel() {
    }

    /**
     * A stop: a place id (module PlaceDef or Landmark name) and, when non-zero, the OSRS tile travellers arrive
     * at (else next to the place centre).
     */
    public record Stop(String place, String name, int x, int y) {
        public Stop(String place, String name) {
            this(place, name, 0, 0);
        }
    }

    /**
     * A transport network: talking to any of its NPCs (or using any of its blocks) travels between its stops.
     * {@code quest} (or null) is needed to use it; the fare is waived once {@code freeAfter} (or null) is complete.
     */
    public record Network(String id, String name, int fare, String verb, List<Stop> stops, String quest, String freeAfter) {
        public Stop stop(String place) {
            for (Stop s : stops) {
                if (s.place().equals(place)) {
                    return s;
                }
            }
            return null;
        }

        /** The stops reachable from {@code from} (every other stop). */
        public List<Stop> destinations(String from) {
            List<Stop> out = new ArrayList<>();
            for (Stop s : stops) {
                if (!s.place().equals(from)) {
                    out.add(s);
                }
            }
            return out;
        }

        /** The fare for a player who has ({@code true}) or hasn't completed {@link #freeAfter()}. */
        public int fare(boolean freeAfterDone) {
            return freeAfter != null && freeAfterDone ? 0 : fare;
        }
    }

    public static final Network SHIP = new Network("veos_ship", "Veos' ship", 0, "You set sail for", List.of(
            new Stop("PORT_SARIM", "Port Sarim", 3055, 3245),
            new Stop(ZeahPlaces.PORT_PISCARILIUS, "Port Piscarilius", 1824, 3691),
            new Stop(ZeahPlaces.LANDS_END, "Land's End", 1504, 3399)), null, null);

    /**
     * Lovakengj minecarts: the twelve OSRS stations at their tiles; 20 coins a ride until The Forsaken Tower is
     * complete. (Stations whose town plaza is elsewhere have their own small station place.)
     */
    public static final Network MINECART = new Network("lovakengj_minecart", "Lovakengj minecart network", 20,
            "The minecart rattles off towards", List.of(
            new Stop(ZeahPlaces.MINECART_ARCEUUS, "Arceuus", 1670, 3833),
            new Stop(ZeahPlaces.MINECART_FARMING_GUILD, "Farming Guild", 1218, 3737),
            new Stop(ZeahPlaces.MINECART_HOSIDIUS_SOUTH, "Hosidius South", 1808, 3479),
            new Stop(ZeahPlaces.MINECART_HOSIDIUS_WEST, "Hosidius West", 1655, 3543),
            new Stop(ZeahPlaces.MINECART_KINGSTOWN, "Kingstown", 1699, 3660),
            new Stop(ZeahPlaces.MINECART_KOUREND_WOODLAND, "Kourend Woodland", 1572, 3466),
            new Stop(ZeahPlaces.MINECART_LOVAKENGJ, "Lovakengj", 1518, 3733),
            new Stop(ZeahPlaces.MOUNT_QUIDAMORTEM, "Mount Quidamortem", 1255, 3548),
            new Stop(ZeahPlaces.NORTHERN_TUNDRAS, "Northern Tundras", 1648, 3931),
            new Stop(ZeahPlaces.MINECART_PORT_PISCARILIUS, "Port Piscarilius", 1761, 3710),
            new Stop(ZeahPlaces.MINECART_SHAYZIEN_EAST, "Shayzien East", 1590, 3620),
            new Stop(ZeahPlaces.MINECART_SHAYZIEN_WEST, "Shayzien West", 1415, 3577)), null, "the_forsaken_tower");

    /** Quetzal transport system (Twilight's Promise): Renu and the quetzals wait at the OSRS landing sites. */
    public static final Network QUETZAL = new Network("quetzal", "Quetzal transport system", 0, "The quetzal carries you to",
            List.of(
            new Stop(ZeahPlaces.CIVITAS, "Civitas illa Fortis", 1697, 3140),
            new Stop(ZeahPlaces.HUNTER_GUILD, "the Hunter Guild", 1585, 3053),
            new Stop(ZeahPlaces.SUNSET_COAST, "Sunset Coast", 1548, 2995),
            new Stop(ZeahPlaces.QUETZACALLI_GORGE, "Quetzacalli Gorge", 1510, 3222),
            new Stop(ZeahPlaces.FORTIS_COLOSSEUM, "the Fortis Colosseum", 1779, 3111),
            new Stop(ZeahPlaces.OUTER_FORTIS, "Outer Fortis", 1700, 3037),
            new Stop(ZeahPlaces.CAM_TORUM, "Cam Torum", 1446, 3108),
            new Stop(ZeahPlaces.TAL_TEKLAN, "Tal Teklan", 1226, 3091),
            new Stop(ZeahPlaces.ALDARIN, "Aldarin", 1389, 2901),
            new Stop(ZeahPlaces.KASTORI, "Kastori", 1344, 3022),
            new Stop(ZeahPlaces.SALVAGER_OVERLOOK, "Salvager Overlook", 1613, 3300),
            new Stop(ZeahPlaces.AUBURNVALE, "Auburnvale", 1411, 3361),
            new Stop(ZeahPlaces.TEOMAT, "The Teomat", 1437, 3171),
            new Stop(ZeahPlaces.COLOSSAL_WYRM_REMAINS, "the Colossal Wyrm Remains", 1670, 2933)), "twilights_promise", null);

    public static final Network PRIMIO = new Network("primio", "Primio's passage", 0, "You make the long journey to", List.of(
            new Stop("VARROCK", "Varrock"),
            new Stop(ZeahPlaces.CIVITAS, "Civitas illa Fortis")), null, null);

    public static List<Network> networks() {
        return List.of(SHIP, MINECART, QUETZAL, PRIMIO);
    }

    /** Travel NPCs: npc id -> (network, the stop the NPC stands at). */
    public record Agent(String npc, Network network, String at) {
    }

    public static List<Agent> agents() {
        List<Agent> out = new ArrayList<>();
        out.add(new Agent(ZeahNpcs.CABIN_BOY_HERBERT, SHIP, "PORT_SARIM"));
        out.add(new Agent(ZeahNpcs.VEOS_PISCARILIUS, SHIP, ZeahPlaces.PORT_PISCARILIUS));
        out.add(new Agent(ZeahNpcs.CAPTAIN_MAGORO, SHIP, ZeahPlaces.LANDS_END));
        out.add(new Agent(ZeahNpcs.PRIMIO, PRIMIO, "VARROCK"));
        out.add(new Agent(ZeahNpcs.PRIMIO_CIVITAS, PRIMIO, ZeahPlaces.CIVITAS));
        for (Stop s : QUETZAL.stops()) {
            out.add(new Agent(ZeahNpcs.quetzal(s.place()), QUETZAL, s.place()));
        }
        return out;
    }

    public static Agent agent(String npc) {
        for (Agent a : agents()) {
            if (a.npc().equals(npc)) {
                return a;
            }
        }
        return null;
    }

    /** The minecart station whose place centre is nearest the given OSRS tile. */
    public static Stop nearestMinecart(int osrsX, int osrsY, java.util.function.Function<String, int[]> coords) {
        Stop best = null;
        long bestD = Long.MAX_VALUE;
        for (Stop s : MINECART.stops()) {
            int[] c = coords.apply(s.place());
            if (c == null) {
                continue;
            }
            long d = (long) (c[0] - osrsX) * (c[0] - osrsX) + (long) (c[1] - osrsY) * (c[1] - osrsY);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }
}
