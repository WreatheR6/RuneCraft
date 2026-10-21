package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport objects behind achievement diary tasks that no NPC runs (round 40): the eagle transport system (OSRS wiki "Eagle
 * transport system": after Eagles' Peak, a rope used on an eagle carries you from the Eagles' Peak eyrie to the desert, the
 * Rellekka Hunter area or the Feldip Hills, and each of those eyries' eagles carries you back) and the Dorgesh-Kaan train to
 * Keldagrim (after Another Slice of H.A.M.). Each stop is a scripted block; using it starts the trip to the chosen
 * destination (sneak-use to choose where several go) and reports "travel:" + route + ":" + destination slug, the key the
 * diary tasks match.
 */
public final class DiaryTransport {
    private DiaryTransport() {
    }

    public static final String EYRIE = "eagle_eyrie";
    public static final String TRAIN = "dorgesh_kaan_train";

    /**
     * A stop. {@code block} is the scripted block it uses; (x, y) its OSRS tile; {@code under} an absolute block position
     * {x, y, z} for a stop inside a carved dungeon (the block stands there, not on the surface), else null.
     */
    public record Stop(String id, String block, String name, int x, int y, int[] under) {
        /** The route id of the trips leaving this stop ("eagle_eagles_peak", "train_dorgesh_kaan"). */
        public String route() {
            return (block.equals(EYRIE) ? "eagle_" : "train_") + id;
        }

        /** Block X of the stop's block. */
        public int blockX() {
            return under != null ? under[0] : GielinorMap.toBlockX(x);
        }

        /** Block Z of the stop's block. */
        public int blockZ() {
            return under != null ? under[2] : GielinorMap.toBlockZ(y);
        }
    }

    /** A trip: from and to (stop ids), the item it uses up (null: none) and the quest it needs. */
    public record Link(String from, String to, String item, String quest) {
    }

    /**
     * The four eyries. The eyries are caves in OSRS (Eagles' Peak's inside the mountain, the others' at y + 6400 in mejrs'
     * NPC list: jungle eagles 2516, 9322, polar eagles 2727, 10217, desert eagles 3422, 9568); this world has no eyrie caves,
     * so each eyrie stands on the surface above its cave. // approx
     */
    public static final List<Stop> STOPS = List.of(
            new Stop("eagles_peak", EYRIE, "Eagles' Peak", 2328, 3496, null),
            new Stop("desert", EYRIE, "Desert", 3422, 3166, null),
            new Stop("rellekka_hunter_area", EYRIE, "Rellekka Hunter area", 2733, 3785, null), // the north-east of the area as built
            new Stop("feldip_hills", EYRIE, "Feldip Hills", 2515, 2922, null),
            // The Dorgesh-Kaan station by the closed train tunnel on the east wall of the city (the dungeons module's Lumbridge
            // cellar caves: city floor block y 10, east wall x 160), and the Keldagrim end beside the surface Keldagrim's cart
            // conductor (OSRS: both stations are underground). // approx
            new Stop("dorgesh_kaan", TRAIN, "Dorgesh-Kaan", GielinorMap.toOsrsX(157), GielinorMap.toOsrsY(14), new int[]{157, 11, 14}),
            new Stop("keldagrim", TRAIN, "Keldagrim", 2740, 3715, null));

    public static final List<Link> LINKS = List.of(
            new Link("eagles_peak", "desert", "rope", "eagles_peak"),
            new Link("eagles_peak", "rellekka_hunter_area", "rope", "eagles_peak"),
            new Link("eagles_peak", "feldip_hills", "rope", "eagles_peak"),
            new Link("desert", "eagles_peak", "rope", "eagles_peak"),
            new Link("rellekka_hunter_area", "eagles_peak", "rope", "eagles_peak"),
            new Link("feldip_hills", "eagles_peak", "rope", "eagles_peak"),
            new Link("dorgesh_kaan", "keldagrim", null, "another_slice_of_ham"),
            new Link("keldagrim", "dorgesh_kaan", null, "another_slice_of_ham"));

    public static Stop stop(String id) {
        for (Stop s : STOPS) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Trips leaving a stop. */
    public static List<Link> from(String stopId) {
        List<Link> out = new ArrayList<>();
        for (Link l : LINKS) {
            if (l.from().equals(stopId)) {
                out.add(l);
            }
        }
        return out;
    }

    /** The activity key a trip reports. */
    public static String key(Link l) {
        return "travel:" + stop(l.from()).route() + ":" + Diaries.slug(stop(l.to()).name());
    }

    /** Every key a trip can report. */
    public static List<String> keys() {
        List<String> out = new ArrayList<>();
        for (Link l : LINKS) {
            out.add(key(l));
        }
        return out;
    }

    /**
     * The stop whose block is at this block position: an underground stop by its exact block (within a block sideways), a
     * surface stop by its OSRS tile (within two tiles); null if none.
     */
    public static Stop at(String block, int bx, int by, int bz) {
        for (Stop s : STOPS) {
            if (!s.block().equals(block)) {
                continue;
            }
            if (s.under() != null) {
                if (Math.abs(bx - s.under()[0]) <= 1 && Math.abs(bz - s.under()[2]) <= 1 && Math.abs(by - s.under()[1]) <= 1) {
                    return s;
                }
            } else if (Math.abs(GielinorMap.toOsrsX(bx) - s.x()) <= 2 && Math.abs(GielinorMap.toOsrsY(bz) - s.y()) <= 2) {
                return s;
            }
        }
        return null;
    }
}
