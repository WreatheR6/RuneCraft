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
}
