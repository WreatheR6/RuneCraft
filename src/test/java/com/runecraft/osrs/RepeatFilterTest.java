package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The repeat filter on the single-way refusal line (round 43: a held attack key no longer re-sends it twice a second). */
class RepeatFilterTest {
    @Test
    void theSameLineWaitsOutItsWindow() {
        RepeatFilter<String> f = new RepeatFilter<>(60);
        assertTrue(f.fresh("p", "You are fighting the goblin (level 2).", 100));
        assertFalse(f.fresh("p", "You are fighting the goblin (level 2).", 110), "held attack: not again");
        assertFalse(f.fresh("p", "You are fighting the goblin (level 2).", 159));
        assertTrue(f.fresh("p", "You are fighting the goblin (level 2).", 160), "3 seconds later it is shown again");
        assertTrue(f.fresh("p", "The giant spider (level 27) is fighting that.", 161), "a different refusal at once");
        assertTrue(f.fresh("q", "The giant spider (level 27) is fighting that.", 161), "per player");
        assertTrue(f.fresh("p", "The giant spider (level 27) is fighting that.", 50), "a clock that went back");
        f.forget("p");
        assertTrue(f.fresh("p", "The giant spider (level 27) is fighting that.", 51));
    }
}
