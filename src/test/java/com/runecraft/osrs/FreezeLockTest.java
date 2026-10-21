package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OSRS freezes as the server enforces them (round 42): 5 ticks of immunity after any freeze ends, and the held spot. */
class FreezeLockTest {
    private static final int MC = 12;

    @Test
    void fiveTicksOfImmunityAfterAFreezeEnds() {
        assertEquals(5, FreezeLock.IMMUNITY_TICKS); // wiki Ice spells
        assertFalse(FreezeLock.immune(Long.MIN_VALUE, 1000, MC));
        assertTrue(FreezeLock.immune(1000, 1000, MC));
        assertTrue(FreezeLock.immune(1000, 1000 + 2 * MC, MC));
        assertTrue(FreezeLock.immune(1000, 1000 + 5 * MC - 1, MC));
        assertFalse(FreezeLock.immune(1000, 1000 + 5 * MC, MC));
        assertFalse(FreezeLock.immune(1000, 999, MC)); // a clock that moved back (self-test resets) never blocks
    }

    @Test
    void aFrozenPlayerIsHeldAtTheSpotTheFreezeCaughtThem() {
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(false, 0, 0, 0, 0, 10, 10));
        assertEquals(FreezeLock.Step.STAY, FreezeLock.step(true, 10, 10, 10, 10, 10.2, 10));
        // walked off a little at a time: put back once past the slack
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, 10, 10, 10.2, 10, 10.4, 10));
        // a sprint jump out (about half a block a tick) is put back
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, 10, 10, 10, 10, 10.5, 10.2));
        // falling straight down is fine (only horizontal moves count)
        assertEquals(FreezeLock.Step.STAY, FreezeLock.step(true, 10, 10, 10, 10, 10, 10));
    }

    @Test
    void aTeleportMovesTheHeldSpot() {
        // OSRS lets a frozen player teleport; a boss's pull moves them too: the freeze holds them at the new spot
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(true, 10, 10, 10, 10, 40, 10));
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(true, 10, 10, 10, 10, 10, 10 + FreezeLock.TELEPORT_BLOCKS));
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, 10, 10, 10, 10, 10, 10 + FreezeLock.TELEPORT_BLOCKS - 0.1));
    }
}
