package com.runecraft.content.raids;

import com.runecraft.osrs.ArenaTiming;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The human-budget recorder of a raid self-test run (round 41 playability, extended in round 42 to the CoX and ToB walks and
 * the ToA deaths and rejoin runs): every raid strike that lands must have been drawn on creation and land at least
 * {@link ArenaTiming#MIN_LEAD_MC} after it was made (unless exact), and every raid projectile must fly at least
 * {@link com.runecraft.osrs.content.raids.RaidRealtime#PROJECTILE_MIN_TICKS} game ticks. Chains onto the listeners already
 * installed and puts them back on {@link #close}.
 */
final class RaidStrikeRecorder implements AutoCloseable {
    private final Consumer<ArenaTiming.StrikeTrace> strikesBefore = RaidEffects.strikeListener;
    private final Consumer<RaidEffects.ProjectileTrace> projectilesBefore = RaidEffects.projectileListener;
    private final List<String> unfair = new ArrayList<>();
    private int strikes;
    private int projectiles;
    private boolean closed;

    private RaidStrikeRecorder() {
    }

    /** Starts recording. */
    static RaidStrikeRecorder start() {
        RaidStrikeRecorder r = new RaidStrikeRecorder();
        Consumer<ArenaTiming.StrikeTrace> s = r.strikesBefore;
        Consumer<RaidEffects.ProjectileTrace> p = r.projectilesBefore;
        RaidEffects.strikeListener = t -> {
            s.accept(t);
            r.strikes++;
            String why = t.unfair();
            if (why != null && r.unfair.size() < 5) {
                r.unfair.add("strike r=" + t.radius() + ": " + why);
            }
        };
        RaidEffects.projectileListener = t -> {
            p.accept(t);
            r.projectiles++;
            long min = (long) com.runecraft.osrs.content.raids.RaidRealtime.PROJECTILE_MIN_TICKS * ArenaTiming.MC_PER_GAME_TICK;
            if (t.flightMc() < min && r.unfair.size() < 5) {
                r.unfair.add("projectile " + t.style() + " flew " + t.flightMc() + " MC (floor " + min + ")");
            }
        };
        return r;
    }

    /** Raid strikes that landed while recording. */
    int strikes() {
        return strikes;
    }

    /** Raid projectiles that landed or fizzled while recording. */
    int projectiles() {
        return projectiles;
    }

    /** Fails (an AssertionError naming {@code where}) when a strike or projectile broke the real-time floors. */
    void check(String where) {
        if (!unfair.isEmpty()) {
            throw new AssertionError(where + ": " + unfair.size() + " raid strike(s)/projectile(s) broke the real-time floor (drawn on "
                    + "creation, landing at least " + ArenaTiming.MIN_LEAD_MC + " MC later; projectiles at least "
                    + com.runecraft.osrs.content.raids.RaidRealtime.PROJECTILE_MIN_TICKS + " game ticks): " + unfair);
        }
    }

    /** A short note for a run's summary. */
    String summary() {
        return strikes + " strikes and " + projectiles + " projectiles landed fair";
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            RaidEffects.strikeListener = strikesBefore;
            RaidEffects.projectileListener = projectilesBefore;
        }
    }
}
