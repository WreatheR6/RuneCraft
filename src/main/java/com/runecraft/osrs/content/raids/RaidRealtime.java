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
 *   Sotetseg maze forgives a foot over a tile edge ({@link #offPath}), Akkha's trailing orbs only follow the player's own steps
 *   ({@link #ownStep});</li>
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
     * The part of one Minecraft tick's horizontal movement ({@code dx}, {@code dz}) the player made of their own accord, in
     * blocks, or 0 when none of it counts. Only such steps drop Akkha's trailing orbs. On the ground and in the air alike: a
     * jump is a step too (there is no jumping in OSRS, so a jump arc can't carry a player past the orbs). Standing still
     * (drift), a teleport and sliding on a slippery block don't count. For each body the player overlaps, {@code away} holds
     * the offset from that body's centre to the player's (x, z pairs): a body shoves straight away from its centre, so the
     * part of the step pointing away from it is taken off and the rest (walking round it, or into it) still counts.
     * // playability: OSRS players move tile by tile only when they click; a Minecraft player also jumps, is shoved by the
     * bodies they touch and slides on ice
     */
    public static double ownStep(double dx, double dz, double[] away, boolean slippery) {
        if (slippery || Math.hypot(dx, dz) > MAX_INPUT_STEP) {
            return 0;
        }
        double x = dx;
        double z = dz;
        for (int i = 0; away != null && i + 1 < away.length; i += 2) {
            double len = Math.hypot(away[i], away[i + 1]);
            if (len < 1e-6) {
                continue; // centre on centre: Minecraft doesn't push either
            }
            double ux = away[i] / len;
            double uz = away[i + 1] / len;
            double out = x * ux + z * uz;
            if (out > 0) {
                x -= ux * out;
                z -= uz * out;
            }
        }
        double own = Math.hypot(x, z);
        return own >= MIN_INPUT_STEP ? own : 0;
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

    // ------------------------------------------------------------------ floor marks (round 43)

    /**
     * The floor columns (block x, z) a circle of {@code radius} blocks round ({@code cx}, {@code cz}) covers: every column whose
     * centre lies inside it, and always the column under the centre. Raid floor marks colour these blocks.
     */
    public static List<int[]> discColumns(double cx, double cz, double radius) {
        List<int[]> out = new ArrayList<>();
        int x0 = (int) Math.floor(cx - radius);
        int x1 = (int) Math.floor(cx + radius);
        int z0 = (int) Math.floor(cz - radius);
        int z1 = (int) Math.floor(cz + radius);
        int ux = (int) Math.floor(cx);
        int uz = (int) Math.floor(cz);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double dx = x + 0.5 - cx;
                double dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= radius * radius || (x == ux && z == uz)) {
                    out.add(new int[]{x, z});
                }
            }
        }
        return out;
    }

    /**
     * The floor columns a lane covers: every column whose centre lies within {@code halfWidth} of the segment from ({@code ax},
     * {@code az}) to ({@code bx}, {@code bz}) ({@link #inLane}), and always the columns under its two ends.
     */
    public static List<int[]> laneColumns(double ax, double az, double bx, double bz, double halfWidth) {
        List<int[]> out = new ArrayList<>();
        int x0 = (int) Math.floor(Math.min(ax, bx) - halfWidth);
        int x1 = (int) Math.floor(Math.max(ax, bx) + halfWidth);
        int z0 = (int) Math.floor(Math.min(az, bz) - halfWidth);
        int z1 = (int) Math.floor(Math.max(az, bz) + halfWidth);
        int ax0 = (int) Math.floor(ax);
        int az0 = (int) Math.floor(az);
        int bx0 = (int) Math.floor(bx);
        int bz0 = (int) Math.floor(bz);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean end = (x == ax0 && z == az0) || (x == bx0 && z == bz0);
                if (end || inLane(x + 0.5, z + 0.5, ax, az, bx, bz, halfWidth)) {
                    out.add(new int[]{x, z});
                }
            }
        }
        return out;
    }

    /** The floor columns of the block rectangle from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}), both corners included. */
    public static List<int[]> rectColumns(int x0, int z0, int x1, int z1) {
        List<int[]> out = new ArrayList<>();
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                out.add(new int[]{x, z});
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ Akkha (round 43)

    /**
     * The shortest gap (game ticks) between two of Akkha's Memory Blasts: the real-time strike floor (36 MC), so the step into
     * the next safe quadrant gets a fair window. // playability: OSRS gives four ticks, two with Feeling Special?
     */
    public static final int MEMORY_BLAST_MIN_GAP = 3;

    /** The gap between Memory Blasts for the invocations' OSRS gap ({@code osrsGap}: 4, or 2 with Feeling Special?). */
    public static int memoryBlastGap(int osrsGap) {
        return Math.max(MEMORY_BLAST_MIN_GAP, osrsGap);
    }

    /**
     * Game ticks between a quadrant shadow's full charge (its warning) and its spear slam enveloping the quadrant. // playability:
     * OSRS slams as the hourglass fills (the element spreads from the edge to the centre, so a quick step still escapes it)
     */
    public static final int SHADOW_SLAM_LEAD_TICKS = 3;

    // ------------------------------------------------------------------ the Great Olm's head (round 43)

    /** The Olm's attack zones, as it faces the chamber from its wall. */
    public static final int OLM_LEFT = 0;
    public static final int OLM_MIDDLE = 1;
    public static final int OLM_RIGHT = 2;
    /** Blocks either side of the Olm's head that its middle zone spans. // approx: the chamber in thirds */
    public static final double OLM_MIDDLE_HALF_WIDTH = 6.5;

    /**
     * The Olm's attack zone holding a player {@code dxEast} blocks east of its head. The head on the north wall faces south, so
     * its left is east; on the south wall its left is west.
     */
    public static int olmZone(double dxEast, boolean southWall) {
        if (Math.abs(dxEast) < OLM_MIDDLE_HALF_WIDTH) {
            return OLM_MIDDLE;
        }
        boolean east = dxEast > 0;
        return east != southWall ? OLM_LEFT : OLM_RIGHT;
    }

    /** "left", "middle" or "right". */
    public static String olmZoneName(int zone) {
        return zone == OLM_LEFT ? "left" : zone == OLM_RIGHT ? "right" : "middle";
    }

    /** The compass side of an Olm zone: its left is east on the north wall, west on the south wall. */
    public static String olmZoneCompass(int zone, boolean southWall) {
        if (zone == OLM_MIDDLE) {
            return "centre";
        }
        return (zone == OLM_LEFT) != southWall ? "east" : "west";
    }

    /**
     * The Great Olm's head (OSRS wiki, Great Olm): it faces one of three zones and attacks only players in it, prioritising the
     * zone with the most players. At each step of its rotation it scans the zone it faces: with a player there it acts (and
     * catches up a standard attack it skipped on the step before), first swerving to another zone that holds strictly more
     * players ("If a player is in his attack zone but Olm swerves his head regardless, no attacks are skipped"); with nobody
     * there it swerves to the zone with the most players (to a hand's side when that hand was damaged within four ticks) and
     * skips the step: a skipped standard attack is caught up on the next step, a skipped special (Crystal Burst, Lightning,
     * Teleport) is lost.
     */
    public static final class OlmHead {
        /** One step's outcome: where the head now faces, whether it turned, acts, and catches up a skipped standard attack. */
        public record Step(int facing, boolean turned, boolean act, boolean catchUp) {
        }

        private int facing = OLM_MIDDLE;
        private boolean owed;

        public int facing() {
            return facing;
        }

        /** A new phase: the head rises facing the middle, owing nothing. */
        public void reset() {
            facing = OLM_MIDDLE;
            owed = false;
        }

        /**
         * One rotation step. {@code counts}: players in the left, middle and right zones; {@code standard}: this step is a
         * standard attack; {@code damagedHandZone}: the zone of a hand damaged within four ticks, or -1.
         */
        public Step step(int[] counts, boolean standard, int damagedHandZone) {
            if (counts[facing] > 0) {
                // a busier zone draws the head over without skipping the step; ties stay put
                int to = facing;
                for (int z : new int[]{OLM_MIDDLE, OLM_LEFT, OLM_RIGHT}) {
                    if (counts[z] > counts[to]) {
                        to = z;
                    }
                }
                boolean turned = to != facing;
                facing = to;
                boolean catchUp = owed;
                owed = false;
                return new Step(facing, turned, true, catchUp);
            }
            int to = facing;
            if (damagedHandZone >= 0) {
                to = damagedHandZone;
            } else {
                int best = -1;
                // the most players; ties go to the middle, then the left
                for (int z : new int[]{OLM_MIDDLE, OLM_LEFT, OLM_RIGHT}) {
                    if (counts[z] > 0 && (best < 0 || counts[z] > counts[best])) {
                        best = z;
                    }
                }
                if (best >= 0) {
                    to = best;
                }
            }
            boolean turned = to != facing;
            facing = to;
            // only the step just skipped can be caught up (no cascade); specials never are
            owed = standard;
            return new Step(facing, turned, false, false);
        }
    }

    // ------------------------------------------------------------------ Verzik's lightning ball (round 43)

    /** Players the lightning ball reaches before it strikes the last of them (OSRS wiki: "chained between 4 players"). */
    public static final int VERZIK_LIGHTNING_PLAYERS = 4;
    /** Blocks within which a teammate can take the ball on. // approx: the wiki only says "too far from any teammates" */
    public static final double VERZIK_LIGHTNING_CHAIN_RANGE = 10.0;
    /** Blocks either side of the ball's path within which Verzik's body catches it (about half her width). */
    public static final double VERZIK_LIGHTNING_CATCH = 1.6;

    /** What the lightning ball does on reaching a player. */
    public enum Chain {
        /** It jumps on to the next player. */
        NEXT,
        /** Verzik stands in its path to the next player: it arcs through her (15-20 to her). */
        THROUGH_VERZIK,
        /** It strikes the player it reached (up to 48; 25 in insulated boots). */
        STRIKE
    }

    /** The lightning ball's next move: what it does and the player it jumps to (-1 when it doesn't jump). */
    public record Bounce(Chain chain, int next) {
    }

    /**
     * Verzik's lightning ball reaching player {@code holder}, the {@code reached}th player it has reached (OSRS wiki, Verzik
     * Vitur and the ToB strategies): the fourth is struck; a player with no teammate within {@link #VERZIK_LIGHTNING_CHAIN_RANGE}
     * is struck; otherwise it jumps to the {@code pick}th (modulo) teammate in range, a random sequence, and when Verzik stands
     * within {@link #VERZIK_LIGHTNING_CATCH} of that path it arcs through her instead. {@code xz}: every player's horizontal
     * position (x, z), the holder's included; ({@code vx}, {@code vz}): Verzik's. // playability: OSRS resolves the chain as the
     * ball leaves; here each jump is decided as the ball arrives, from where everyone stands then, so the team can still move
     */
    public static Bounce lightningBounce(int reached, int holder, double[][] xz, double vx, double vz, int pick) {
        if (reached >= VERZIK_LIGHTNING_PLAYERS) {
            return new Bounce(Chain.STRIKE, -1);
        }
        List<Integer> mates = new ArrayList<>();
        for (int i = 0; i < xz.length; i++) {
            if (i != holder && Math.hypot(xz[i][0] - xz[holder][0], xz[i][1] - xz[holder][1]) <= VERZIK_LIGHTNING_CHAIN_RANGE) {
                mates.add(i);
            }
        }
        if (mates.isEmpty()) {
            return new Bounce(Chain.STRIKE, -1);
        }
        int next = mates.get(Math.floorMod(pick, mates.size()));
        if (inLane(vx, vz, xz[holder][0], xz[holder][1], xz[next][0], xz[next][1], VERZIK_LIGHTNING_CATCH)) {
            return new Bounce(Chain.THROUGH_VERZIK, -1);
        }
        return new Bounce(Chain.NEXT, next);
    }

    // ------------------------------------------------------------------ the Path of Het's beam (round 43)

    /**
     * Game ticks the caster statue's beam path glows before the beam fires. // playability: OSRS fires it with the statue's
     * animation, the path shown as it burns
     */
    public static final int HET_BEAM_LEAD_TICKS = 3;
    /**
     * Blocks either side of a beam's line that it burns: a player whose foot is just over a beam tile's edge is not burnt.
     * // playability: OSRS burns the tiles the beam crosses
     */
    public static final double HET_BEAM_HALF_WIDTH = 0.35;
}
