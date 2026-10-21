package com.runecraft.osrs;

/**
 * OSRS freezes as the server enforces them (round 42): the immunity after a freeze ends and the spot a frozen player is held
 * at. Pure, so the rules are unit-tested; {@code combat.FreezeRules} applies them to Minecraft's Slowness.
 *
 * <ul>
 *   <li>After a freeze ends, the target is immune to the next one for {@link #IMMUNITY_TICKS} game ticks (wiki Ice spells:
 *   "Targets are immune from being frozen again for 5 ticks (3 seconds) after the freeze time expires"). A freeze broken
 *   early (cured, a bind released when its holder dies) ends it there, so the immunity starts then too.</li>
 *   <li>A frozen player can't walk: Minecraft moves players on their own client, so the server holds a frozen player at the
 *   spot the freeze caught them ({@link #step}). A drift within {@link #SLACK_BLOCKS} (momentum from before the freeze) is
 *   left alone; anything further is put back. A jump of {@link #TELEPORT_BLOCKS} or more in one Minecraft tick is the server
 *   moving the player (a teleport, which OSRS allows while frozen, or a boss's pull), and the freeze holds them there.</li>
 * </ul>
 */
public final class FreezeLock {
    /** OSRS freeze immunity after a freeze ends: 5 game ticks. */
    public static final int IMMUNITY_TICKS = 5;
    /** Horizontal drift (blocks) from the held spot that is left alone. */
    public static final double SLACK_BLOCKS = 0.3;
    /** A horizontal jump (blocks) in one Minecraft tick that only the server makes (a frozen client moves well under it). */
    public static final double TELEPORT_BLOCKS = 1.5;

    /** What the server does with a frozen player this tick. */
    public enum Step {
        /** Hold here from now on (the freeze just started, or the server moved the player). */
        ANCHOR,
        /** Within the slack of the held spot: nothing to do. */
        STAY,
        /** Walked or hopped away: put back on the held spot. */
        RETURN
    }

    private FreezeLock() {
    }

    /** Whether a freeze can't land at {@code nowMc}: the last one ended at {@code endedMc} (Long.MIN_VALUE: never). */
    public static boolean immune(long endedMc, long nowMc, int mcPerTick) {
        return endedMc != Long.MIN_VALUE && nowMc >= endedMc && nowMc - endedMc < (long) IMMUNITY_TICKS * mcPerTick;
    }

    /**
     * One Minecraft tick of a frozen player at ({@code x}, {@code z}): {@code anchored} says whether a spot is held
     * ({@code ax}, {@code az}); ({@code lx}, {@code lz}) is where the player was last tick.
     */
    public static Step step(boolean anchored, double ax, double az, double lx, double lz, double x, double z) {
        if (!anchored || Math.hypot(x - lx, z - lz) >= TELEPORT_BLOCKS) {
            return Step.ANCHOR;
        }
        return Math.hypot(x - ax, z - az) > SLACK_BLOCKS ? Step.RETURN : Step.STAY;
    }
}
