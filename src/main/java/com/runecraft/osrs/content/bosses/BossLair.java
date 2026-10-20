package com.runecraft.osrs.content.bosses;

/**
 * The lair of a boss summoned at its stone (OSRS bosses never leave their rooms, and an instance left behind resets):
 * where it stays, who still counts as fighting it, and when it resets. The Minecraft side ({@code DataBossEntity},
 * {@code BossEntity}) asks these rules every game tick; distances are squared blocks from where the boss was summoned
 * (its home) unless noted.
 *
 * <p>The leash only brings the boss back when its fight has left the lair: while its target stands within
 * {@link #reach} of home the boss chases it wherever it goes, so a player can't stand just past the leash and attack it
 * for free (it would walk out, be put back and forget them). Attacks from further than {@link #reach} do nothing: like
 * attacking an OSRS boss from outside its instance.
 */
public final class BossLair {
    private BossLair() {
    }

    /** Blocks within which a player keeps a summoned boss's fight going (measured from the boss). */
    public static final double PRESENCE_RANGE = 32;
    /** A summoned boss with nobody near it for this long (game ticks: one minute) resets, like an OSRS instance left. */
    public static final int RESET_TICKS = 100;
    /** Scoreboard tag prefix marking a minion summoned in a lair (followed by the home position as a long). */
    public static final String MINION_TAG = "rc_lair_";

    /** How far from home a player may stand and still be part of the fight: the presence range, or the leash if larger. */
    public static double reach(double leash) {
        return Math.max(PRESENCE_RANGE, leash);
    }

    /** Whether a target this far from home has left the lair (the boss forgets it and waits at home). */
    public static boolean targetLeft(double targetDistSq, double leash) {
        double r = reach(leash);
        return targetDistSq > r * r;
    }

    /**
     * Whether a boss this far from home is put back there: it is past its leash and has no target, or its target has
     * left the lair. A boss chasing a target that is still in the lair is never put back.
     */
    public static boolean returnsHome(double bossDistSq, double leash, boolean hasTarget, double targetDistSq) {
        return bossDistSq > leash * leash && (!hasTarget || targetLeft(targetDistSq, leash));
    }

    /** Whether an attack from a player this far from home reaches the boss (false: from outside its lair). */
    public static boolean inReach(double attackerDistSq, double leash) {
        return !targetLeft(attackerDistSq, leash);
    }

    /** One more game tick with ({@code someone}) or without a player near: the new count of lonely ticks. */
    public static int lonely(int lonelyTicks, boolean someone) {
        return someone ? 0 : lonelyTicks + 1;
    }

    /** Whether a boss alone this many game ticks resets. */
    public static boolean resets(int lonelyTicks) {
        return lonelyTicks >= RESET_TICKS;
    }

    /** The tag on minions summoned in the lair whose home is {@code home} (a packed block position). */
    public static String minionTag(long home) {
        return MINION_TAG + home;
    }

    // ---- quest bosses and terrain safespots

    /**
     * Leash of a quest boss spawned beside the player (no summoning stone): it stays within this many blocks of where it
     * appeared and vanishes a minute after its fight is left, like an OSRS quest boss when the player walks off.
     */
    public static final int QUEST_LEASH = 24;

    /**
     * Game ticks a melee-only lair boss may be kept out of reach by terrain before it closes the gap: Minecraft terrain (a
     * pillar, a ledge, a hole) can strand a melee boss where OSRS's flat lair floors never would, which would make it free to
     * kill from range.
     */
    public static final int SAFESPOT_TICKS = 5; // approx

    /** One more game tick with ({@code stranded}) or without its target out of its reach: the new count. */
    public static int stranded(int ticks, boolean stranded) {
        return stranded ? ticks + 1 : 0;
    }

    /** Whether a boss stranded this long steps up beside its target. */
    public static boolean breaksSafespot(int strandedTicks) {
        return strandedTicks >= SAFESPOT_TICKS;
    }

    /**
     * Whether a target is out of a melee boss's reach where the boss can't walk to it: further than a step from melee range
     * ({@code distance} beyond {@code meleeReach}) and no path that ends next to it.
     */
    public static boolean strandedBy(double distance, double meleeReach, boolean pathReaches) {
        return distance > meleeReach + 1.0 && !pathReaches;
    }

    /** Spots around a centre a stranded boss tries (eight compass points). */
    public static final int RING_SPOTS = 8;

    /** The {@link #RING_SPOTS} spots ({x, z}) around a centre at a radius, the first toward the angle {@code start} (radians). */
    public static double[][] ring(double cx, double cz, double radius, double start) {
        double[][] out = new double[RING_SPOTS][];
        for (int i = 0; i < RING_SPOTS; i++) {
            double a = start + i * 2 * Math.PI / RING_SPOTS;
            out[i] = new double[]{cx + Math.cos(a) * radius, cz + Math.sin(a) * radius};
        }
        return out;
    }

    /**
     * How a stranded boss closes the gap: it steps to ({@code knockDown} false) or knocks its target down to
     * ({@code knockDown} true) the spot {@code x, z}.
     */
    public record SafespotBreak(boolean knockDown, double x, double z) {
    }

    /**
     * How a melee boss stranded by terrain ({@link #breaksSafespot}) reaches its target: it steps to the first spot beside
     * the target where it fits ({@code bossFits}: open and on solid floor at the target's height). Where there is none (a
     * 1x1 pillar or a one-wide shaft: every spot lies past the edge or inside a wall) the target is knocked off their perch
     * instead, down to the first spot beside the boss where a player fits ({@code playerFits}), or onto the boss's own spot,
     * which is always standable (the boss stands on it). So no terrain keeps a melee boss from hitting back.
     */
    public static SafespotBreak breakSafespot(double[][] besideTarget, java.util.function.Predicate<double[]> bossFits,
                                              double[][] besideBoss, java.util.function.Predicate<double[]> playerFits,
                                              double bossX, double bossZ) {
        for (double[] spot : besideTarget) {
            if (bossFits.test(spot)) {
                return new SafespotBreak(false, spot[0], spot[1]);
            }
        }
        for (double[] spot : besideBoss) {
            if (playerFits.test(spot)) {
                return new SafespotBreak(true, spot[0], spot[1]);
            }
        }
        return new SafespotBreak(true, bossX, bossZ);
    }
}
