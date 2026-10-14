package com.runecraft.osrs.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Barrows set effects with the amulet of the damned (osrs-dps-calc's Ahrim's and Karil's transforms). */
class SetEffectsTest {
    @Test
    void damnedEffects() {
        assertEquals(39, SetEffects.ahrimDamned(30)); // trunc(h x 13 / 10)
        assertEquals(1, SetEffects.ahrimDamned(1));
        assertEquals(15, SetEffects.karilDamnedSecondHit(31)); // trunc(h / 2)
        assertSame(SetEffects.KARIL, SetEffects.barrows("karils_coif", "karils_leathertop", "karils_leatherskirt", "karils_crossbow"));
        assertEquals(0.25, SetEffects.BARROWS_PROC_CHANCE);
    }
}
