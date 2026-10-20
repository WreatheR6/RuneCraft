package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.data.Fish;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tempoross' energy, essence, harpoonfish and repairs against the OSRS Wiki. */
class TemporossStormTest {
    @Test
    void poolsScaleWithTheCrew() {
        // Mod Squid: each player adds ~2.6k energy solo down to ~1.8k with 40 (hitsplats x10).
        assertEquals(260, TemporossStorm.energy(1));
        assertEquals(250, TemporossStorm.essence(1));
        assertEquals(40 * 180, TemporossStorm.energy(40));
        assertEquals(40 * 170, TemporossStorm.essence(40));
        assertTrue(TemporossStorm.energy(10) < 10 * 260 && TemporossStorm.energy(10) > 10 * 180);
        // 17 cooked fish leave a solo Tempoross at ~2% energy, below the 10% where attacks stop (wiki strategy: "4%").
        int left = TemporossStorm.energy(1) - 17 * TemporossStorm.COOKED_ENERGY;
        assertTrue(left > 0 && left * 100 / TemporossStorm.energy(1) < MinigameRules.Tempoross.ATTACKS_STOP_ENERGY);
        assertEquals(10, MinigameRules.Tempoross.RAW_DAMAGE);
        assertEquals(15, MinigameRules.Tempoross.COOKED_DAMAGE);
    }

    @Test
    void harpoonfish() {
        // 78.13% at level 35, 100% at 99 (OSRS Wiki "Fishing spot (Tempoross Cove)").
        assertEquals(0.7813, Fish.catchChance(35, TemporossStorm.CATCH_LOW, TemporossStorm.CATCH_HIGH, 1.0), 1e-4);
        assertEquals(1.0, Fish.catchChance(99, TemporossStorm.CATCH_LOW, TemporossStorm.CATCH_HIGH, 1.0), 1e-9);
        // Cooking: a flat 10 XP at any level, 3 ticks a fish.
        assertEquals(10.0, MinigameRules.Tempoross.cookXp(99), 1e-9);
        assertEquals(10.0, MinigameRules.Tempoross.cookXp(35), 1e-9);
        assertEquals(3, TemporossStorm.COOK_TICKS);
        // No one scatters more than 12 essence.
        assertEquals(12, TemporossStorm.scatterMax(99));
        assertTrue(TemporossStorm.scatterMax(35) < 12);
    }

    @Test
    void repairsAndFires() {
        assertEquals(4.0 * 50, TemporossStorm.repairXp(50), 1e-9);
        assertEquals(2, TemporossStorm.FIRE_SPREADS);
    }
}
