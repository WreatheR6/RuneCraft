package com.runecraft.osrs.content.systems;

/**
 * Blessed bone shard sacrifices (OSRS Wiki "Blessed bone shards" / "Libation bowl"): 30 Prayer and at least 2 prayer
 * points; the bowl must hold blessed wine, and each jug poured in allows 400 shards; up to 100 shards are sacrificed per
 * action for 5 Prayer XP each (6 with blessed sunfire wine), and each sacrifice drains 2 prayer points.
 *
 * <p>RuneCraft has no Teomat yet, so the sacrifice is made at any altar and a jug of wine stands in for the jug of blessed
 * wine (in OSRS it is blessed at the exposed altar south of the Teomat). approx
 */
public final class Libation {
    public static final String SHARDS = "blessed_bone_shards";
    public static final String WINE = "jug_of_wine";
    /** Where the poured wine's remaining shard allowance is kept (per-player charges). */
    public static final String CHARGE_KEY = "libation_bowl";
    public static final int LEVEL = 30;
    public static final int MIN_PRAYER_POINTS = 2;
    /** Prayer points drained by each sacrifice. */
    public static final int PRAYER_COST = 2;
    public static final int SHARDS_PER_JUG = 400;
    public static final int MAX_PER_ACTION = 100;
    public static final int XP_PER_SHARD = 5;

    private Libation() {
    }

    /** One sacrifice: shards offered, jugs of wine poured for it, and the allowance left in the bowl afterwards. */
    public record Offer(int shards, int jugsUsed, int allowanceLeft) {
        public double xp() {
            return shards * (double) XP_PER_SHARD;
        }
    }

    /**
     * Plans one sacrifice: up to {@link #MAX_PER_ACTION} of the carried shards, pouring jugs (400 shards each) only when the
     * bowl's allowance runs out. Returns an offer of 0 shards when nothing can be sacrificed.
     */
    public static Offer offer(int shardsCarried, int allowance, int jugsCarried) {
        int want = Math.max(0, Math.min(MAX_PER_ACTION, shardsCarried));
        int jugs = 0;
        int have = Math.max(0, allowance);
        while (have < want && jugs < jugsCarried) {
            jugs++;
            have += SHARDS_PER_JUG;
        }
        int shards = Math.min(want, have);
        return new Offer(shards, jugs, have - shards);
    }
}
