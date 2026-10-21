package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time adaptations of the three raids' mechanics (round 42 playability, CONTENT_GUIDE rule 5; the shared floors are in
 * {@code osrs/ArenaTiming} and {@code osrs/ThreatTiming}). OSRS resolves its raid sequences on consecutive game ticks and
 * expects a player to read a tile; a Minecraft player reacts in seconds and stands between blocks, so:
 * <ul>
 *   <li>hits that need different answers (a prayer switch, a step) land at least {@value #SEQUENCE_GAP_TICKS} game ticks
 *   (24 MC) apart, in the order they were announced ({@link #spacedTick}, {@link Timeline});</li>
 *   <li>a raid projectile flies at least {@value #PROJECTILE_MIN_TICKS} game ticks, drawn all the way;</li>
 *   <li>tile-exact safe spots get lenient areas: Zebak's shelter widens behind a rock ({@link #inShelterCone}), the
 *   Sotetseg maze forgives a foot over a tile edge ({@link #offPath}), Akkha's trailing orbs only follow real steps
 *   ({@link #inputStep});</li>
 *   <li>warnings name compass directions ({@link #compass}).</li>
 * </ul>
 * Every OSRS number (max hits, patterns, cycle lengths, prayer rules) stays: only when a hit resolves and how it is shown
 * change. Pure Java (unit-tested), no Minecraft types.
 */
public final class RaidRealtime {
    private RaidRealtime() {
    }

    /** Game ticks between two raid hits that need different answers (a different prayer, another step). 24 MC. */
    public static final int SEQUENCE_GAP_TICKS = 2;
    /** The shortest flight of a raid projectile (game ticks): 24 MC, the round 41 boss projectile band's top. */
    public static final int PROJECTILE_MIN_TICKS = 2;

    /**
     * The game tick a hit wanted on {@code wantTick} lands on, given another on the same player landed (or will land) on
     * {@code lastTick}: a hit needing a different answer that would land within {@link #SEQUENCE_GAP_TICKS} of it waits that
     * long after it; one needing the same answer (the same prayer, the same step away), or well apart, keeps its tick. Never
     * earlier than wanted. // playability: OSRS lands them a tick apart (a one-tick prayer flick)
     */
    public static long spacedTick(long wantTick, long lastTick, boolean differentAnswer) {
        if (!differentAnswer || Math.abs(wantTick - lastTick) >= SEQUENCE_GAP_TICKS) {
            return wantTick;
        }
        return Math.max(wantTick, lastTick + SEQUENCE_GAP_TICKS);
    }

    /**
     * A boss's queue of upcoming hits (Olm's specials, Sotetseg's orbs at one player): each new one lands no sooner than it
     * wants and, when it needs a different answer than one already queued, {@link #SEQUENCE_GAP_TICKS} away from it, so they
     * arrive one at a time in the order they were announced.
     */
    public static final class Timeline {
        private record Entry(long tick, String kind) {
        }

        private final List<Entry> pending = new ArrayList<>();

        /**
         * Schedules a hit of {@code kind} wanted {@code delay} game ticks after {@code now}; returns the game tick it lands on.
         * Kinds equal by name need the same answer and may land together.
         */
        public long schedule(long now, int delay, String kind) {
            pending.removeIf(e -> e.tick() < now);
            long tick = now + Math.max(0, delay);
            boolean moved = true;
            while (moved) {
                moved = false;
                for (Entry e : pending) {
                    if (!e.kind().equals(kind) && Math.abs(tick - e.tick()) < SEQUENCE_GAP_TICKS) {
                        tick = e.tick() + SEQUENCE_GAP_TICKS;
                        moved = true;
                    }
                }
            }
            pending.add(new Entry(tick, kind));
            pending.sort((a, b) -> Long.compare(a.tick(), b.tick()));
            return tick;
        }

        /** The kinds still to land after {@code now}, in landing order (repeats of one kind next to each other merged). */
        public List<String> order(long now) {
            List<String> out = new ArrayList<>();
            for (Entry e : pending) {
                if (e.tick() >= now && (out.isEmpty() || !out.get(out.size() - 1).equals(e.kind()))) {
                    out.add(e.kind());
                }
            }
            return out;
        }

        /** Game ticks until the next landing of {@code kind}, or -1 when none is queued. */
        public long ticksUntil(long now, String kind) {
            for (Entry e : pending) {
                if (e.tick() >= now && e.kind().equals(kind)) {
                    return e.tick() - now;
                }
            }
            return -1;
        }

        public void clear() {
            pending.clear();
        }
    }

    /**
     * The order cue for a queue of hits: "1. crystal burst · 2. lightning" (at most {@code max} of them). Empty for none.
     */
    public static String orderCue(List<String> kinds, int max) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(max, kinds.size()); i++) {
            if (i > 0) {
                b.append(" · ");
            }
            b.append(i + 1).append(". ").append(kinds.get(i));
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ areas

    /**
     * Whether a point at horizontal offset ({@code px}, {@code pz}) lies within {@code halfWidth} of the segment from
     * ({@code ax}, {@code az}) to ({@code bx}, {@code bz}) (a lightning lane, a wall of fire).
     */
    public static boolean inLane(double px, double pz, double ax, double az, double bx, double bz, double halfWidth) {
        double dx = bx - ax;
        double dz = bz - az;
        double len = dx * dx + dz * dz;
        double k = len <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len));
        double ex = ax + k * dx - px;
        double ez = az + k * dz - pz;
        return ex * ex + ez * ez <= halfWidth * halfWidth;
    }

    /** Zebak's rocks shelter up to this many tiles behind them (OSRS: the three tiles behind a rock, in its row). */
    public static final double SHELTER_DEPTH = 3.5;
    /** Half the width of the shelter right behind a rock (the rock's own row). */
    public static final double SHELTER_HALF_WIDTH = 0.5;
    /** How much the shelter widens per block further behind the rock. // playability */
    public static final double SHELTER_SPREAD = 0.25;

    /**
     * Whether a player {@code behind} blocks behind the centre of one of Zebak's rocks (away from him) and {@code lateral}
     * blocks to its side is sheltered from the Great Roar: within {@link #SHELTER_DEPTH} behind it, in a cone that starts as
     * the rock's own row and widens by {@link #SHELTER_SPREAD} a block. // playability: OSRS shelters exactly the three tiles
     * behind the rock in its row
     */
    public static boolean inShelterCone(double behind, double lateral) {
        if (behind <= 0.5 || behind > SHELTER_DEPTH) {
            return false;
        }
        return Math.abs(lateral) <= SHELTER_HALF_WIDTH + SHELTER_SPREAD * (behind - 0.5);
    }

    /**
     * How far (blocks) a foot may stray over the edge of a Sotetseg maze tile before the tile next to it counts. // playability:
     * OSRS judges the tile the player stands on, and a player standing between blocks straddles two
     */
    public static final double MAZE_EDGE_LENIENCY = 0.35;

    /**
     * Whether a player at ({@code x}, {@code z}) blocks from a maze grid's corner (tiles {@code tile} blocks wide) is off the
     * path: on a tile the path doesn't hold, and further than {@link #MAZE_EDGE_LENIENCY} from every path tile next to it.
     * {@code onPath} answers for (column, row); positions off the grid are never off the path.
     */
    public static boolean offPath(double x, double z, double tile, int cols, int rows, java.util.function.BiPredicate<Integer, Integer> onPath) {
        int c = (int) Math.floor(x / tile);
        int r = (int) Math.floor(z / tile);
        if (c < 0 || c >= cols || r < 0 || r >= rows || onPath.test(c, r)) {
            return false;
        }
        double fx = x - c * tile;
        double fz = z - r * tile;
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                if ((dc == 0 && dr == 0) || c + dc < 0 || c + dc >= cols || r + dr < 0 || r + dr >= rows || !onPath.test(c + dc, r + dr)) {
                    continue;
                }
                double ex = dc < 0 ? fx : dc > 0 ? tile - fx : 0;
                double ez = dr < 0 ? fz : dr > 0 ? tile - fz : 0;
                if (ex <= MAZE_EDGE_LENIENCY && ez <= MAZE_EDGE_LENIENCY) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Akkha's trailing orbs

    /** The shortest per-Minecraft-tick step that counts as walking (sneaking is about 0.065). */
    public static final double MIN_INPUT_STEP = 0.03;
    /** The longest per-Minecraft-tick step that counts as walking (a sprint-jump is about 0.5; a teleport is more). */
    public static final double MAX_INPUT_STEP = 0.75;
    /** Walked distance within a game tick that drops a trailing orb (OSRS: leaving a tile). */
    public static final double ORB_STEP = 0.5;

    /**
     * Whether one Minecraft tick's horizontal movement of {@code step} blocks was the player walking: on the ground, not
     * pressed against another creature (a boss shoving through them), not sliding on a slippery block, and neither standing
     * still nor teleported. Only such steps drop Akkha's trailing orbs. // playability: OSRS players move tile by tile only
     * when they click; a Minecraft player is also shoved by bodies and slides on ice
     */
    public static boolean inputStep(double step, boolean onGround, boolean pushed, boolean slippery) {
        return onGround && !pushed && !slippery && step >= MIN_INPUT_STEP && step <= MAX_INPUT_STEP;
    }

    // ------------------------------------------------------------------ words

    private static final String[] COMPASS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

    /**
     * The compass direction of horizontal offset ({@code dx}, {@code dz}) in Minecraft's axes (x east, z south), in eight
     * sectors; "centre" for none.
     */
    public static String compass(double dx, double dz) {
        if (Math.abs(dx) < 1e-6 && Math.abs(dz) < 1e-6) {
            return "centre";
        }
        double a = Math.toDegrees(Math.atan2(dz, dx)); // 0 east, 90 south
        int i = (int) Math.floorMod(Math.round(a / 45.0), 8);
        return COMPASS[i];
    }

    /** The name of a quarter of an arena: "north-west", "south-east"... */
    public static String quarter(boolean west, boolean north) {
        return (north ? "north" : "south") + "-" + (west ? "west" : "east");
    }

    /**
     * The phase 3 Warden's slam as a compass warning: the Warden faces west, so its right is north (OSRS: its right, its
     * left, then both sides; {@link ToaBossRules#slamHits}).
     */
    public static String wardenSlamCue(int kind) {
        return switch (kind % 3) {
            case 0 -> "Floor slam NORTH - stand south of the centre line!";
            case 1 -> "Floor slam SOUTH - stand north of the centre line!";
            default -> "Floor slams NORTH and SOUTH - stand in the centre three rows!";
        };
    }
}
