package com.runecraft.osrs.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkBuildQueueTest {
    private static final long A = StructureIndex.key(-5, -7);
    private static final long B = StructureIndex.key(-5, -8);

    /**
     * The missing-station scenario: building chunk A (at the edge of a loaded area) touches its
     * neighbour B, which is loaded for a moment (announced) and then demoted again before its turn.
     * B must not be lost: when it is promoted later (forceload / player) without a second load
     * event, it still gets built.
     */
    @Test
    void chunkUnloadedBeforeItsTurnIsBuiltWhenLoadedAgainWithoutASecondEvent() {
        ChunkBuildQueue q = new ChunkBuildQueue();
        Set<Long> loaded = new HashSet<>();
        List<Long> built = new ArrayList<>();
        loaded.add(A);
        q.offer(A);
        // building A loads B synchronously for one tick: the load event announces it
        loaded.add(B);
        q.offer(B);
        built.addAll(q.next(1, loaded::contains));
        loaded.remove(B); // the temporary ticket expired; B stays in memory at a lower level
        built.addAll(q.next(2, loaded::contains));
        assertEquals(List.of(A), built);
        assertTrue(q.isWaiting(B), "B must wait, not be dropped");
        assertTrue(q.isEmpty());
        // nothing happens while B stays unloaded
        assertEquals(0, q.recheck(loaded::contains));
        // promoted to full again: no new load event (the chunk never unloaded), only the recheck sees it
        loaded.add(B);
        assertEquals(1, q.recheck(loaded::contains));
        built.addAll(q.next(2, loaded::contains));
        assertEquals(List.of(A, B), built);
        assertEquals(0, q.waitingCount());
    }

    @Test
    void reannouncedChunkLeavesTheWaitingSetAndIsQueuedOnce() {
        ChunkBuildQueue q = new ChunkBuildQueue();
        q.offer(B);
        assertTrue(q.next(4, k -> false).isEmpty());
        assertTrue(q.isWaiting(B));
        assertTrue(q.offer(B));
        assertFalse(q.offer(B), "already queued");
        assertFalse(q.isWaiting(B));
        assertEquals(1, q.queuedCount());
        assertEquals(List.of(B), q.next(4, k -> true));
    }

    @Test
    void reallyUnloadedChunkIsForgottenUntilAnnouncedAgain() {
        ChunkBuildQueue q = new ChunkBuildQueue();
        q.offer(B);
        q.next(1, k -> false);
        q.unloaded(B);
        assertEquals(0, q.waitingCount());
        assertEquals(0, q.recheck(k -> true));
        q.offer(B);
        assertEquals(List.of(B), q.next(1, k -> true));
    }

    @Test
    void nextKeepsQueueOrderAndLimit() {
        ChunkBuildQueue q = new ChunkBuildQueue();
        for (int i = 0; i < 5; i++) {
            q.offer(StructureIndex.key(i, 0));
        }
        assertEquals(List.of(StructureIndex.key(0, 0), StructureIndex.key(1, 0)), q.next(2, k -> true));
        assertEquals(3, q.queuedCount());
    }
}
