package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Chance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deep sea trawling shoals (OSRS Wiki, July 2026 dump: "Deep sea trawling" and each shoal's page; snapshot
 * {@code src/test/resources/osrs/sailing/trawling_shoals.tsv}). A shoal swims at its wiki map pins; a boat with a trawling
 * net in range rolls every 3 ticks for a token of Sailing XP and, at the shoal's trawling-net success chance, a catch of 1
 * to the net's max fish with the shoal's Fishing XP once per catch. Multi-depth shoals stop at the shallow (or deep)
 * position and rise back to moderate halfway through the stop; a net that can't reach the shoal's depth halves the fish
 * (rounded randomly) and the Sailing XP. When a shoal resumes moving there is a 10% chance it turns into a mixed shoal for
 * two stops. Crewmates roll every 5 ticks at a flat 4%. Baiting with offcuts gives each fish a 20% chance of an extra one.
 *
 * <p>RuneCraft simplifications: shoals stay on their pins (OSRS shoals follow a route between stops; here one stop cycle
 * follows the next on the spot), and the net is lowered or raised to the shoal's depth automatically when it can reach
 * it (playability: OSRS has a depth control in the Sailing Options tab). Rare fish and angler's paint are not modelled.
 */
public final class SailingShoals {
    private SailingShoals() {
    }

    /** Depths, shallowest first. */
    public static final int SHALLOW = 0;
    public static final int MODERATE = 1;
    public static final int DEEP = 2;

    /**
     * A shoal: its wiki name, the Fishing level, the deepest depth it swims at, the ticks of one stop, whether it takes
     * fine offcuts only, the fish (CookingData id), Fishing XP per catch, trawling-net success low/high out of 256 and
     * its map pins.
     */
    public record Shoal(String id, String name, int level, int maxDepth, int stopTicks, boolean fineBait, String fish, double xp,
                        int low, int high, List<int[]> pins) {
    }

    /** A mixed shoal: wiki name, Fishing level, deepest depth, net success low/high, the shoals it replaces and its fish weights. */
    public record Mixed(String id, String name, int level, int maxDepth, int low, int high, List<String> replaces,
                        Map<String, Integer> fish) {
    }

    public static final List<Shoal> SHOALS = List.of(
            new Shoal("giant_krill_shoal", "Giant krill shoal", 69, SHALLOW, 150, false, "giant_krill", 112.5, 15, 30,
                    pins(1564, 3364, 2784, 2633, 1493, 2924, 3021, 2510)),
            new Shoal("haddock_shoal", "Haddock shoal", 73, SHALLOW, 120, false, "haddock", 128.5, 12, 28,
                    pins(2814, 2373, 1404, 2722, 2978, 2223)),
            new Shoal("yellowfin_shoal", "Yellowfin shoal", 79, MODERATE, 100, false, "yellowfin", 155.5, 8, 27,
                    pins(1706, 2662, 1866, 2784, 2272, 2678)),
            new Shoal("halibut_shoal", "Halibut shoal", 83, MODERATE, 80, true, "halibut", 195.5, 6, 25, pins(1841, 3293, 1919, 2441)),
            new Shoal("bluefin_shoal", "Bluefin shoal", 87, DEEP, 70, true, "bluefin", 220.5, 5, 23, pins(2022, 3637, 2200, 2278)),
            new Shoal("marlin_shoal", "Marlin shoal", 91, DEEP, 50, true, "marlin", 265.5, 4, 20, pins(1932, 4041, 2815, 4013)));

    public static final List<Mixed> MIXED = List.of(
            new Mixed("shimmering_shoal", "Shimmering shoal", 69, SHALLOW, 15, 35, List.of("giant_krill_shoal", "haddock_shoal"),
                    weights("giant_krill", 45, "haddock", 40, "yellowfin", 10, "halibut", 5)),
            new Mixed("glistening_shoal", "Glistening shoal", 76, MODERATE, 8, 32, List.of("yellowfin_shoal", "halibut_shoal"),
                    weights("yellowfin", 45, "halibut", 40, "bluefin", 10, "marlin", 5)),
            new Mixed("vibrant_shoal", "Vibrant shoal", 86, DEEP, 5, 28, List.of("bluefin_shoal", "marlin_shoal"),
                    weights("bluefin", 50, "marlin", 50)));

    /** Rolls: every 3 ticks for the player, every 5 for a crewmate at a flat 4% (wiki). */
    public static final int PLAYER_ROLL_TICKS = 3;
    public static final int CREW_ROLL_TICKS = 5;
    public static final double CREW_CHANCE = 0.04;
    /** A shoal resuming its route turns mixed 10% of the time, for two stops (wiki). */
    public static final double MIXED_CHANCE = 0.10;
    public static final int MIXED_STOPS = 2;
    /** The nets hold 250 fish between them (wiki); here the catch goes straight to the inventory or cargo hold. */
    public static final int NET_CAPACITY = 250;
    /** Baited shoals: each fish caught has a 20% chance of an extra fish (wiki). */
    public static final double BAIT_EXTRA = 0.20;
    /** Tiles from a shoal's pin within which a net reaches it (approx: OSRS nets must overlap the moving shoal). */
    public static final int RANGE = 10;

    private static List<int[]> pins(int... xy) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < xy.length; i += 2) {
            out.add(new int[]{xy[i], xy[i + 1]});
        }
        return List.copyOf(out);
    }

    private static Map<String, Integer> weights(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    public static Shoal shoal(String id) {
        for (Shoal s : SHOALS) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** The mixed shoal that can spawn in place of a shoal. */
    public static Mixed mixedFor(Shoal s) {
        for (Mixed m : MIXED) {
            if (m.replaces().contains(s.id())) {
                return m;
            }
        }
        return null;
    }

    /** The shoal whose pin is nearest an OSRS tile, within {@code range} tiles (Chebyshev), or null. */
    public static Shoal nearest(int x, int y, int range) {
        Shoal best = null;
        int bestD = Integer.MAX_VALUE;
        for (Shoal s : SHOALS) {
            for (int[] p : s.pins()) {
                int d = Math.max(Math.abs(p[0] - x), Math.abs(p[1] - y));
                if (d <= range && d < bestD) {
                    bestD = d;
                    best = s;
                }
            }
        }
        return best;
    }

    /** The pin nearest an OSRS tile, of any shoal, as {x, y} (for the fathom stone and pearl), or null. */
    public static int[] nearestPin(int x, int y) {
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (Shoal s : SHOALS) {
            for (int[] p : s.pins()) {
                long d = (long) (p[0] - x) * (p[0] - x) + (long) (p[1] - y) * (p[1] - y);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    /**
     * The depth a shoal swims at {@code tick} ticks into a stop: single-depth shoals stay shallow; the others stop at the
     * shallow position, or the deep one for three-depth shoals ({@code deepStop}), and rise to moderate halfway (wiki).
     */
    public static int depthAt(int maxDepth, int stopTicks, int tick, boolean deepStop) {
        if (maxDepth == SHALLOW) {
            return SHALLOW;
        }
        if (tick * 2 >= stopTicks) {
            return MODERATE;
        }
        return maxDepth == DEEP && deepStop ? DEEP : SHALLOW;
    }

    /** The trawling-net success chance per roll at a Fishing level (wiki skilling success chart). */
    public static double catchChance(int fishingLevel, int low, int high) {
        return Chance.lerp(fishingLevel, low, high);
    }

    /**
     * Fish in one successful catch: 1 to the net's max, uniformly (approx: the wiki gives only the max); at the wrong depth
     * halved, an odd count rounded up or down at random (wiki); baited, each fish has a 20% chance of an extra one.
     */
    public static int fishCaught(int maxFish, boolean rightDepth, boolean baited, Random random) {
        int n = 1 + random.nextInt(Math.max(1, maxFish));
        if (!rightDepth) {
            n = n / 2 + (n % 2 == 1 && random.nextBoolean() ? 1 : 0);
        }
        if (baited) {
            int extra = 0;
            for (int i = 0; i < n; i++) {
                if (random.nextDouble() < BAIT_EXTRA) {
                    extra++;
                }
            }
            n += extra;
        }
        return n;
    }

    /** The fish a mixed shoal's catch gives (its wiki distribution). */
    public static String mixedFish(Mixed m, Random random) {
        int total = 0;
        for (int w : m.fish().values()) {
            total += w;
        }
        int r = random.nextInt(total);
        for (Map.Entry<String, Integer> e : m.fish().entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return m.fish().keySet().iterator().next();
    }

    /** The offcuts that bait a shoal: plain offcuts for the shallow shoals (and yellowfin), fine offcuts for the others. */
    public static List<String> bait(Shoal s) {
        return s.fineBait() ? List.of(FINE_OFFCUTS) : s.id().equals("yellowfin_shoal") ? List.of(OFFCUTS, FINE_OFFCUTS) : List.of(OFFCUTS);
    }

    public static final String OFFCUTS = "fish_offcuts";
    public static final String FINE_OFFCUTS = "fine_fish_offcuts";
}
