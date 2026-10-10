package com.runecraft.osrs.content.zeah;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport into and around Zeah and Varlamore: Veos' ship from Port Sarim to Port Piscarilius
 * and Land's End, the Lovakengj minecart network, the Varlamore quetzal transport system and
 * Primio's passage between Varrock and Civitas illa Fortis.
 *
 * <p>Each stop is a place id (a module PlaceDef or a Landmark enum name); travellers arrive next
 * to its centre. Fares are the OSRS ones where there is a fare (minecarts cost 20 coins without
 * the Kourend & Kebos diary; Veos' ship and the quetzals are free).
 */
public final class ZeahTravel {
    private ZeahTravel() {
    }

    public record Stop(String place, String name) {
    }

    /** A transport network: talking to any of its NPCs (or using any of its blocks) travels between its stops. */
    public record Network(String id, String name, int fare, String verb, List<Stop> stops) {
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
    }

    public static final Network SHIP = new Network("veos_ship", "Veos' ship", 0, "You set sail for", List.of(
            new Stop("PORT_SARIM", "Port Sarim"),
            new Stop(ZeahPlaces.PORT_PISCARILIUS, "Port Piscarilius"),
            new Stop(ZeahPlaces.LANDS_END, "Land's End")));

    public static final Network MINECART = new Network("lovakengj_minecart", "Lovakengj minecart network", 20,
            "The minecart rattles off towards", List.of(
            new Stop(ZeahPlaces.ARCEUUS, "Arceuus"),
            new Stop(ZeahPlaces.MINECART_FARMING_GUILD, "Farming Guild"),
            new Stop(ZeahPlaces.HOSIDIUS, "Hosidius"),
            new Stop(ZeahPlaces.KOUREND_WOODLAND, "Kourend Woodland"),
            new Stop(ZeahPlaces.LOVAKENGJ, "Lovakengj"),
            new Stop(ZeahPlaces.MOUNT_QUIDAMORTEM, "Mount Quidamortem"),
            new Stop(ZeahPlaces.NORTHERN_TUNDRAS, "Northern Tundras"),
            new Stop(ZeahPlaces.PORT_PISCARILIUS, "Port Piscarilius"),
            new Stop(ZeahPlaces.SHAYZIEN, "Shayzien")));

    public static final Network QUETZAL = new Network("quetzal", "Quetzal transport system", 0, "The quetzal carries you to",
            List.of(
            new Stop(ZeahPlaces.CIVITAS, "Civitas illa Fortis"),
            new Stop(ZeahPlaces.HUNTER_GUILD, "the Hunter Guild"),
            new Stop(ZeahPlaces.SUNSET_COAST, "Sunset Coast"),
            new Stop(ZeahPlaces.QUETZACALLI_GORGE, "Quetzacalli Gorge"),
            new Stop(ZeahPlaces.FORTIS_COLOSSEUM, "the Fortis Colosseum"),
            new Stop(ZeahPlaces.OUTER_FORTIS, "Outer Fortis"),
            new Stop(ZeahPlaces.CAM_TORUM, "Cam Torum"),
            new Stop(ZeahPlaces.TAL_TEKLAN, "Tal Teklan"),
            new Stop(ZeahPlaces.ALDARIN, "Aldarin"),
            new Stop(ZeahPlaces.KASTORI, "Kastori"),
            new Stop(ZeahPlaces.SALVAGER_OVERLOOK, "Salvager Overlook"),
            new Stop(ZeahPlaces.AUBURNVALE, "Auburnvale")));

    public static final Network PRIMIO = new Network("primio", "Primio's passage", 0, "You make the long journey to", List.of(
            new Stop("VARROCK", "Varrock"),
            new Stop(ZeahPlaces.CIVITAS, "Civitas illa Fortis")));

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
