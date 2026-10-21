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
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(false, false, 0, 0, 10, 10));
        assertEquals(FreezeLock.Step.STAY, FreezeLock.step(true, false, 10, 10, 10.2, 10));
        // walked off a little at a time: put back once past the slack
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, false, 10, 10, 10.4, 10));
        // a sprint jump out (about half a block a tick) is put back
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, false, 10, 10, 10.5, 10.2));
        // falling straight down is fine (only horizontal moves count)
        assertEquals(FreezeLock.Step.STAY, FreezeLock.step(true, false, 10, 10, 10, 10));
    }

    @Test
    void aClientsOwnLongStepIsPutBackHoweverFar() {
        // vanilla accepts client moves of up to ~10 blocks a packet: a client that ignores the freeze and steps 1.6 or 3
        // blocks a tick is still put back (before: any move of 1.5 blocks or more counted as a teleport and moved the spot)
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, false, 10, 10, 11.6, 10));
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, false, 10, 10, 13, 10));
        assertEquals(FreezeLock.Step.RETURN, FreezeLock.step(true, false, 10, 10, 40, 10));
    }

    @Test
    void onlyAServerMoveMovesTheHeldSpot() {
        // OSRS lets a frozen player teleport; a boss's pull moves them too: the freeze holds them at the new spot
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(true, true, 10, 10, 40, 10));
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(true, true, 10, 10, 13, 10));
        assertEquals(FreezeLock.Step.ANCHOR, FreezeLock.step(true, true, 10, 10, 10.1, 10));
    }

    @Test
    void putBackNeverInsideTheLedgeTheySlidOff() {
        // held at y = 64 on a ledge's edge, slid 0.6 blocks over and dropped a block: at y = 63 over the held spot is the
        // ledge itself, so they go back to the held spot's height (before: their own height, inside the block)
        assertEquals(64.0, FreezeLock.returnY(64.0, 63.0, false));
        // caught mid-jump (held at 65) and landed beside the spot: put back on the ground, not lifted into the air
        assertEquals(64.0, FreezeLock.returnY(65.0, 64.0, true));
        // a low ceiling over the held spot at their height: the held spot's height
        assertEquals(64.0, FreezeLock.returnY(64.0, 65.0, false));
    }
}
