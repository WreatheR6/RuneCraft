package com.runecraft.osrs;

import java.util.List;
import java.util.Locale;

/**
 * What a real player can do in the time a cue gives (round 44 playability, CONTENT_GUIDE rule 5): the numbers the self-test's
 * real-player walks play by, so CI proves the real-time windows of rounds 41-43 are fair for a human and not only correct on
 * paper.
 * <ul>
 *   <li>The walking player moves at {@value #RUN_BLOCKS_PER_SECOND} blocks a second, Minecraft's walking pace (a human holding
 *   the forward key, no sprint), through the world's collision.</li>
 *   <li>It reacts {@value #REACTION_MC} Minecraft ticks (0.4 s, within a human's 300-500 ms) after a cue: the cast bar row, the
 *   telegraph line, a projectile or a wind-up appearing, a strike's marker drawn on the floor.</li>
 *   <li>A telegraphed strike or hazard can be escaped when the walk out of it fits its lead: distance to safety / run speed +
 *   {@value #MARGIN_SECONDS} s &lt;= the time from the cue to the landing ({@link #fits}).</li>
 * </ul>
 * OSRS numbers are not touched by any of this: it only measures whether the timings the game gives can be met.
 */
public final class RealReaction {
    private RealReaction() {
    }

    public static final int MC_PER_SECOND = 20;
    /** The walking player's pace, blocks a second: Minecraft's walking speed (OSRS: running two tiles a tick). */
    public static final double RUN_BLOCKS_PER_SECOND = 4.317;
    /** The same pace per Minecraft tick. */
    public static final double RUN_BLOCKS_PER_MC = RUN_BLOCKS_PER_SECOND / MC_PER_SECOND;
    /** The fastest human reaction the walks assume (300 ms). */
    public static final int REACTION_MIN_MC = 6;
    /** The slowest (500 ms). */
    public static final int REACTION_MAX_MC = 10;
    /** The reaction the walks play with: a prayer key or the first step comes this many Minecraft ticks after its cue. */
    public static final int REACTION_MC = 8;
    /** The walk-out budget's margin: a strike must leave this much time beyond the walk out of it. */
    public static final double MARGIN_SECONDS = 0.25;
    /** Blocks past a strike's edge a dodging player aims for (its hit test counts the edge itself as inside). */
    public static final double EDGE_CLEARANCE = 0.3;
    /**
     * Blocks past a strike's edge the walk-out budget measures to: the distance to safety itself (the hit test counts the edge
     * as inside), not the dodge's comfort margin.
     */
    public static final double BUDGET_CLEARANCE = 0.05;

    /** Seconds a walk of {@code distance} blocks takes at the run pace, plus the margin. */
    public static double walkOutSeconds(double distance) {
        return Math.max(0, distance) / RUN_BLOCKS_PER_SECOND + MARGIN_SECONDS;
    }

    /** Minecraft ticks the player needs to walk {@code distance} blocks (rounded up). */
    public static int walkMc(double distance) {
        return (int) Math.ceil(Math.max(0, distance) / RUN_BLOCKS_PER_MC - 1e-9);
    }

    /** Whether a walk of {@code distance} blocks out of a strike fits a cue that leads its landing by {@code leadMc}. */
    public static boolean fits(double distance, long leadMc) {
        return walkOutSeconds(distance) <= leadMc / (double) MC_PER_SECOND + 1e-9;
    }

    /**
     * Null when walking {@code distance} blocks fits {@code name}'s lead of {@code leadMc}; otherwise why not, with the
     * numbers ("dragonfire_bomb: 3.4 blocks to safety need 1.04 s (0.79 s walking + 0.25 s), its cue leads by 0.90 s").
     */
    public static String budgetFailure(String name, double distance, long leadMc) {
        if (fits(distance, leadMc)) {
            return null;
        }
        return String.format(Locale.ROOT, "%s: %.1f blocks to safety need %.2f s (%.2f s walking + %.2f s), its cue leads by %.2f s",
                name, distance, walkOutSeconds(distance), Math.max(0, distance) / RUN_BLOCKS_PER_SECOND, MARGIN_SECONDS,
                leadMc / (double) MC_PER_SECOND);
    }

    /**
     * Blocks the player's centre must move to be out of a disc of {@code radius} whose centre is ({@code dx}, {@code dz}) away
     * horizontally, with {@link #EDGE_CLEARANCE} past its edge (0 when already clear).
     */
    public static double exitDistance(double dx, double dz, double radius) {
        return exitDistance(dx, dz, radius, EDGE_CLEARANCE);
    }

    /** {@link #exitDistance(double, double, double)} with {@code clearance} past the edge. */
    public static double exitDistance(double dx, double dz, double radius, double clearance) {
        double d = Math.sqrt(dx * dx + dz * dz);
        return d > radius + clearance ? 0 : radius + clearance - d;
    }

    /** A danger on the floor: a disc that lands on Minecraft tick {@code landMc}. */
    public static final class Disc {
        public final double x;
        public final double z;
        public final double radius;
        public final long landMc;
        public final String name;

        public Disc(double x, double z, double radius, long landMc, String name) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.landMc = landMc;
            this.name = name;
        }

        /** Whether a player standing at ({@code px}, {@code pz}) is hit by it (its edge counts, as the landing test does). */
        public boolean covers(double px, double pz) {
            return covers(px, pz, EDGE_CLEARANCE);
        }

        /** {@link #covers(double, double)} with {@code clearance} past the edge. */
        public boolean covers(double px, double pz, double clearance) {
            double dx = px - x;
            double dz = pz - z;
            return dx * dx + dz * dz <= (radius + clearance) * (radius + clearance);
        }
    }

    /** Where a player may stand: a column check by the caller (floor, room for the body, no marked danger). */
    public interface Standable {
        boolean ok(double x, double z);
    }

    /**
     * The nearest spot to ({@code px}, {@code pz}) that none of {@code discs} landing by {@code byMc} covers and that
     * {@code standable} accepts, searched on rings a quarter block apart out to {@code maxDistance}; null when there is none.
     * Returns {x, z, distance}.
     */
    public static double[] safeSpot(double px, double pz, List<Disc> discs, long byMc, double maxDistance, Standable standable) {
        return safeSpot(px, pz, discs, byMc, maxDistance, EDGE_CLEARANCE, standable);
    }

    /** {@link #safeSpot(double, double, List, long, double, Standable)} keeping {@code clearance} past every disc's edge. */
    public static double[] safeSpot(double px, double pz, List<Disc> discs, long byMc, double maxDistance, double clearance,
            Standable standable) {
        if (clear(px, pz, discs, byMc, clearance) && standable.ok(px, pz)) {
            return new double[]{px, pz, 0};
        }
        for (double r = 0.25; r <= maxDistance + 1e-9; r += 0.25) {
            int points = Math.max(8, (int) Math.ceil(2 * Math.PI * r / 0.25));
            double[] best = null;
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                double x = px + Math.cos(a) * r;
                double z = pz + Math.sin(a) * r;
                if (clear(x, z, discs, byMc, clearance) && standable.ok(x, z)) {
                    best = new double[]{x, z, r};
                    break;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static boolean clear(double x, double z, List<Disc> discs, long byMc, double clearance) {
        for (Disc d : discs) {
            if (d.landMc <= byMc && d.covers(x, z, clearance)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The protection a cast-bar style asks for ({@link CastBar#MELEE}, {@link CastBar#RANGED}, {@link CastBar#MAGIC}), as
     * {@link PrayerGrace#PROTECTIONS} index (0 magic, 1 missiles, 2 melee), or -1 for typeless.
     */
    public static int protectionOf(int castStyle) {
        return castStyle == CastBar.TYPELESS ? -1 : CastBar.protectionOf(castStyle);
    }

    /** The protection an attack style asks for (0 magic, 1 missiles, 2 melee; -1 for none, a typeless hit). */
    public static int protectionOf(AttackType style) {
        if (style == null) {
            return -1;
        }
        if (style == AttackType.MAGIC) {
            return 0;
        }
        if (style == AttackType.RANGED) {
            return 1;
        }
        return 2;
    }

    /**
     * Of the hits on their way ({@code protections[i]} and {@code landMc[i]}, cued on {@code cueMc[i]}), the protection to have
     * up on {@code nowMc}: the one landing soonest that is not past (a hit already landed asks nothing) and whose cue the player
     * has had {@link #REACTION_MC} to react to; -1 when none asks yet. A hit asking for no prayer (-1) is skipped.
     */
    public static int wantedProtection(int[] protections, long[] cueMc, long[] landMc, long nowMc) {
        int best = -1;
        long bestLand = Long.MAX_VALUE;
        for (int i = 0; i < protections.length; i++) {
            if (protections[i] < 0 || landMc[i] < nowMc || nowMc - cueMc[i] < REACTION_MC) {
                continue;
            }
            if (landMc[i] < bestLand) {
                bestLand = landMc[i];
                best = protections[i];
            }
        }
        return best;
    }
}
