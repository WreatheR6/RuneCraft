package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Fishing spots that move, like OSRS: every spot stays on its tile for a random time and then
 * relocates to another free tile of the same fishing area. In OSRS most spots move after somewhere
 * between roughly 4 and 9 minutes (the exact timers vary per spot and are not published, approx.).
 *
 * <p>Nothing is persisted: each spot gets a timer the first time it is seen (fished or ticked) and
 * remembers its "home" tile so repeated moves stay within its area instead of drifting along a
 * coast. After a server restart a spot simply starts a fresh timer where it is.
 */
public final class FishingSpotTimers {
    /** Shortest stay on one tile, in game ticks (~4 minutes, approx.). */
    public static final int MIN_STAY = 400;
    /** Longest stay on one tile, in game ticks (~9 minutes, approx.). */
    public static final int MAX_STAY = 900;
    /** How far (in blocks, horizontally) a spot may wander from its home tile. */
    public static final int AREA_RADIUS = 6;

    /** Checks whether a block position can hold a fishing spot (open water next to the shore). */
    @FunctionalInterface
    public interface TileCheck {
        boolean valid(int x, int y, int z);
    }

    private record Entry(long moveAt, int homeX, int homeY, int homeZ) {
    }

    private final Map<Long, Entry> timers = new HashMap<>();

    /** Random stay length in game ticks; {@code random.applyAsInt(n)} returns 0..n-1. */
    public static int rollStay(IntUnaryOperator random) {
        return MIN_STAY + random.applyAsInt(MAX_STAY - MIN_STAY + 1);
    }

    /**
     * Returns true when the spot at {@code key} should move now. The first call for a spot starts its
     * timer (with the given position as its home) and returns false.
     */
    public boolean due(long key, int x, int y, int z, long gameTick, IntUnaryOperator random) {
        Entry e = timers.get(key);
        if (e == null) {
            timers.put(key, new Entry(gameTick + rollStay(random), x, y, z));
            return false;
        }
        return gameTick >= e.moveAt;
    }

    /** Game tick the spot at {@code key} will move, or -1 if it has no timer yet. */
    public long moveAt(long key) {
        Entry e = timers.get(key);
        return e == null ? -1 : e.moveAt;
    }

    /**
     * Picks the tile the spot at ({@code x,y,z}) moves to: a valid tile within {@link #AREA_RADIUS}
     * of its home (same height, or one block up/down), not its current tile. Returns null when there
     * is nowhere to go (the spot then stays and gets a new timer).
     */
    public int[] destination(long key, int x, int y, int z, TileCheck check, IntUnaryOperator random) {
        Entry e = timers.get(key);
        int hx = e == null ? x : e.homeX;
        int hy = e == null ? y : e.homeY;
        int hz = e == null ? z : e.homeZ;
        List<int[]> options = candidates(hx, hy, hz, x, y, z, check);
        return options.isEmpty() ? null : options.get(random.applyAsInt(options.size()));
    }

    /** Every valid tile around a home tile except the current one (pure helper, used by tests too). */
    public static List<int[]> candidates(int hx, int hy, int hz, int x, int y, int z, TileCheck check) {
        List<int[]> out = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -AREA_RADIUS; dx <= AREA_RADIUS; dx++) {
                for (int dz = -AREA_RADIUS; dz <= AREA_RADIUS; dz++) {
                    if (dx * dx + dz * dz > AREA_RADIUS * AREA_RADIUS) {
                        continue;
                    }
                    int cx = hx + dx;
                    int cy = hy + dy;
                    int cz = hz + dz;
                    // Moving to a neighbouring tile would hardly look like a move; OSRS spots jump a few tiles.
                    if (Math.abs(cx - x) + Math.abs(cz - z) < 2) {
                        continue;
                    }
                    if (check.valid(cx, cy, cz)) {
                        out.add(new int[]{cx, cy, cz});
                    }
                }
            }
        }
        return out;
    }

    /**
     * Records that the spot at {@code oldKey} moved to {@code newKey}: the new tile keeps the old home
     * and gets a fresh stay timer. Also used with {@code oldKey == newKey} to restart the timer of a
     * spot that had nowhere to go.
     */
    public void moved(long oldKey, long newKey, int x, int y, int z, long gameTick, IntUnaryOperator random) {
        Entry e = timers.remove(oldKey);
        int hx = e == null ? x : e.homeX;
        int hy = e == null ? y : e.homeY;
        int hz = e == null ? z : e.homeZ;
        timers.put(newKey, new Entry(gameTick + rollStay(random), hx, hy, hz));
    }

    /** Forgets a spot (e.g. its block was removed). */
    public void forget(long key) {
        timers.remove(key);
    }

    public int size() {
        return timers.size();
    }
}
