package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Round 43: the attack timer's food look shares the bar between the weapon's period and the food's added delay. */
class FoodDelayBarTest {
    @Test
    void weaponThenFood() {
        // a 4-tick weapon (48 MC) eaten on right after attacking: shark food adds 3 ticks (36 MC)
        FoodDelayBar.Look start = FoodDelayBar.look(48, 36, 48 + 36);
        assertEquals(48f / 84f, start.split(), 1e-6);
        assertEquals(0f, start.fill(), 1e-6);
        assertTrue(start.inWeapon());
        // halfway through the weapon's delay
        FoodDelayBar.Look mid = FoodDelayBar.look(48, 36, 24 + 36);
        assertEquals(24f / 84f, mid.fill(), 1e-6);
        assertTrue(mid.inWeapon());
        // the weapon's own delay is over: the food's part runs
        FoodDelayBar.Look food = FoodDelayBar.look(48, 36, 18);
        assertFalse(food.inWeapon());
        assertEquals(1f - 18f / 84f, food.fill(), 1e-6);
        // ready
        assertEquals(1f, FoodDelayBar.look(48, 36, 0).fill(), 1e-6);
        // the food's part is the last of the wait
        assertEquals(36, FoodDelayBar.foodLeft(36, 60));
        assertEquals(18, FoodDelayBar.foodLeft(36, 18));
        assertEquals(0, FoodDelayBar.foodLeft(36, 0));
        assertEquals(0, FoodDelayBar.foodLeft(0, 30));
    }
}
