package com.runecraft.osrs;

/**
 * Real-time floors for scripted boss and raid arenas (round 41 playability plan, scope arena-strikes-warnings; CONTENT_GUIDE
 * rule 5). OSRS resolves a ground strike on an exact game tick and expects the player to read a tile marker the moment it
 * appears; a Minecraft player reacts in seconds, so:
 * <ul>
 *   <li>a damaging ground strike lands at least {@value #STRIKE_FLOOR_TICKS} game ticks ({@value #MIN_LEAD_MC} Minecraft
 *   ticks, 1.8 s) after it is created, measured from the Minecraft tick it was created on;</li>
 *   <li>its marker is drawn on the creation tick at its true radius and redrawn every {@value #REDRAW_MC} Minecraft ticks,
 *   brightening (with a rising bell) in the last {@value #FINAL_CUE_MC};</li>
 *   <li>what is drawn is what hits: the landing test is horizontal (within the radius) with a {@value #Y_TOLERANCE}-block
 *   height tolerance, matching the ring drawn on the floor;</li>
 *   <li>warnings within {@value #TELEGRAPH_MERGE_MC} Minecraft ticks of each other are shown as one line.</li>
 * </ul>
 * Every OSRS number (max hits, patterns, cycle lengths) stays: only when a strike resolves and how it is shown change.
 */
public final class ArenaTiming {
    private ArenaTiming() {
    }

    /** Minecraft ticks per OSRS game tick (ActionManager.MC_TICKS_PER_GAME_TICK; this class stays free of Minecraft). */
    public static final int MC_PER_GAME_TICK = 12;
    /** A damaging strike lands no sooner than this many game ticks after it is created. // playability: OSRS 0-2 ticks */
    public static final int STRIKE_FLOOR_TICKS = 3;
    /** The floor in Minecraft ticks: 1.8 seconds from the first visible marker to the hit. */
    public static final int MIN_LEAD_MC = STRIKE_FLOOR_TICKS * MC_PER_GAME_TICK;
    /** Pending strike markers are redrawn this often (Minecraft ticks). */
    public static final int REDRAW_MC = 4;
    /** In the last this-many Minecraft ticks before it lands a marker brightens and ticks a rising bell. */
    public static final int FINAL_CUE_MC = 10;
    /** Height tolerance of the horizontal landing test (blocks): a player standing on a step or a slab is still hit. */
    public static final double Y_TOLERANCE = 2.0;
    /** Warnings to one player within this many Minecraft ticks merge into one line. */
    public static final int TELEGRAPH_MERGE_MC = 10;
    /** At most this many warnings share a merged line (older parts drop off). */
    public static final int TELEGRAPH_MAX_PARTS = 3;

    /**
     * The game tick a strike created on game tick {@code now} with OSRS delay {@code delay} lands on. A damaging strike is
     * floored to {@link #STRIKE_FLOOR_TICKS}; a logic-only one (no damage of its own) keeps its delay.
     */
    public static long strikeDue(long now, int delay, boolean damaging) {
        return strikeDue(now, delay, damaging, 0);
    }

    /**
     * As {@link #strikeDue(long, int, boolean)} for a strike created {@code mcIntoTick} Minecraft ticks after game tick
     * {@code now} began (strikes land on game-tick boundaries): a damaging strike still gets its full
     * {@link #MIN_LEAD_MC} from the Minecraft tick it was created on.
     */
    public static long strikeDue(long now, int delay, boolean damaging, int mcIntoTick) {
        int d = Math.max(0, delay);
        if (!damaging) {
            return now + d;
        }
        int into = Math.max(0, Math.min(MC_PER_GAME_TICK - 1, mcIntoTick));
        int floor = (MIN_LEAD_MC + into + MC_PER_GAME_TICK - 1) / MC_PER_GAME_TICK;
        return now + Math.max(d, floor);
    }

    /**
     * Minecraft ticks into the current game tick of a clock whose game-tick count moves on by itself every
     * {@link #MC_PER_GAME_TICK} Minecraft ticks (the server's), when the arena last ran its own game tick on
     * {@code boundaryMc}. A gap of a whole game tick or more means a new game tick has begun that the arena hasn't run yet
     * (the server raises its game tick before the arena's handler runs on the same Minecraft tick): the count restarts from
     * that game tick's start, instead of sticking at the last Minecraft tick of the old one.
     */
    public static int mcIntoTick(long nowMc, long boundaryMc) {
        long d = nowMc - boundaryMc;
        if (d <= 0) {
            return 0;
        }
        return (int) (d % MC_PER_GAME_TICK);
    }

    /** The Minecraft tick a strike due on game tick {@code due} lands, when game tick {@code now} began on {@code nowMc}. */
    public static long dueMc(long nowMc, long now, long due) {
        return nowMc + (due - now) * MC_PER_GAME_TICK;
    }

    /**
     * Whether a player at offset ({@code dx}, {@code dy}, {@code dz}) from a strike's centre is inside it: horizontally within
     * the radius (the ring drawn on the floor) and within {@link #Y_TOLERANCE} blocks of its height.
     */
    public static boolean inStrike(double dx, double dy, double dz, double radius) {
        return dx * dx + dz * dz <= radius * radius && Math.abs(dy) < Y_TOLERANCE;
    }

    /** Whether a pending marker drawn last on {@code lastDrawMc} is due for a redraw on {@code nowMc}. */
    public static boolean redrawDue(long lastDrawMc, long nowMc) {
        return nowMc - lastDrawMc >= REDRAW_MC;
    }

    /** Whether a marker landing on {@code dueMc} is in its final, brightened stretch on {@code nowMc}. */
    public static boolean finalCue(long dueMc, long nowMc) {
        long left = dueMc - nowMc;
        return left >= 0 && left <= FINAL_CUE_MC;
    }

    /** Whether a warning on {@code nowMc} merges into the line shown on {@code lastMc}. */
    public static boolean mergesTelegraph(long lastMc, long nowMc) {
        return nowMc >= lastMc && nowMc - lastMc < TELEGRAPH_MERGE_MC;
    }

    /**
     * Points of a strike's outline ring: about 8 a block of circumference, 10 to 48, fewer when many markers are pending at
     * once (so a wall of thirty fire tiles doesn't flood the client with particles).
     */
    public static int ringPoints(double radius, int pending) {
        int points = (int) Math.ceil(2 * Math.PI * Math.max(0.1, radius) * 8);
        points = Math.max(10, Math.min(48, points));
        if (pending > 6) {
            points = Math.max(8, points * 6 / pending);
        }
        return points;
    }

    /**
     * What a strike recorder sees for one strike (self-tests): when it was created, first drawn and landed (Minecraft
     * ticks), how often it was drawn, its radius and max hit, whether it can hurt (a max hit or a landing callback) and
     * whether it was created exact (logic that keeps its OSRS tick).
     */
    public record StrikeTrace(long createdMc, long firstDrawnMc, long landedMc, int draws, double radius, int maxDamage,
                              boolean damaging, boolean exact) {
        /** Minecraft ticks from creation to landing. */
        public long leadMc() {
            return landedMc - createdMc;
        }

        /** Null when the strike was fair (drawn on creation, at least the floor from creation to landing); else why not. */
        public String unfair() {
            if (!damaging || exact) {
                return null;
            }
            if (firstDrawnMc != createdMc) {
                return "first drawn " + (firstDrawnMc - createdMc) + " MC after it was created";
            }
            if (leadMc() < MIN_LEAD_MC) {
                return "landed " + leadMc() + " MC after it was created (floor " + MIN_LEAD_MC + ")";
            }
            return null;
        }
    }
}
