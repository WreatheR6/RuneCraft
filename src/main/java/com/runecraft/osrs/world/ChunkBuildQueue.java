package com.runecraft.osrs.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;

/**
 * Chunks waiting for their structure shares to be placed, a few per tick.
 *
 * <p>A chunk announced by a chunk-load event may no longer be loaded when its turn comes: a block
 * write at the edge of a chunk being built reads its neighbour (neighbour shape updates), which
 * loads that neighbour synchronously to full status on a ticket that expires a tick later. The
 * neighbour keeps its in-memory chunk at a lower ticket level, and when it is promoted to full
 * again (a player walks up, a forceload) Fabric's chunk-load event does <b>not</b> fire a second
 * time, because the chunk was never unloaded. Dropping such a chunk from the queue therefore meant
 * it was never built at all in that session (missing buildings and stations just outside loaded
 * areas). Instead such chunks wait here until they are loaded again, or until they really unload
 * (a later reload announces them again).
 */
public final class ChunkBuildQueue {
    private final Deque<Long> queue = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    /** Announced chunks that were not loaded when their turn came. */
    private final Set<Long> waiting = new LinkedHashSet<>();

    /** A chunk was loaded (announced): queue it unless it already is. Returns true if newly queued. */
    public boolean offer(long key) {
        waiting.remove(key);
        if (queued.add(key)) {
            queue.add(key);
            return true;
        }
        return false;
    }

    /** A chunk was really unloaded: forget it while it waits (a reload announces it again). */
    public void unloaded(long key) {
        waiting.remove(key);
    }

    /**
     * Up to {@code max} queued chunks that are loaded now, in queue order. Queued chunks that are not
     * loaded move to the waiting set instead of being dropped.
     */
    public List<Long> next(int max, LongPredicate loaded) {
        List<Long> out = new ArrayList<>(max);
        while (out.size() < max && !queue.isEmpty()) {
            long key = queue.poll();
            queued.remove(key);
            if (loaded.test(key)) {
                out.add(key);
            } else {
                waiting.add(key);
            }
        }
        return out;
    }

    /** Re-queues every waiting chunk that is loaded again; returns how many were re-queued. */
    public int recheck(LongPredicate loaded) {
        int n = 0;
        for (Iterator<Long> it = waiting.iterator(); it.hasNext(); ) {
            long key = it.next();
            if (loaded.test(key)) {
                it.remove();
                if (queued.add(key)) {
                    queue.add(key);
                }
                n++;
            }
        }
        return n;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int queuedCount() {
        return queue.size();
    }

    public int waitingCount() {
        return waiting.size();
    }

    public boolean isWaiting(long key) {
        return waiting.contains(key);
    }
}
