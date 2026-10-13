package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpSkullTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    @Test
    void unprovokedWildernessAttackSkullsForThirtyMinutes() {
        PvpSkull s = new PvpSkull();
        assertEquals(3000, PvpSkull.ATTACK_TICKS); // 30 minutes of 0.6 s ticks
        assertTrue(s.onAttack(a, b, 100, true));
        assertTrue(s.isSkulled(a, 100));
        assertFalse(s.isSkulled(b, 100));
        assertEquals(3000, s.ticksLeft(a, 100));
        assertTrue(s.isSkulled(a, 3099));
        assertFalse(s.isSkulled(a, 3100));
        assertEquals(30, PvpSkull.minutes(3000));
    }

    @Test
    void retaliationDoesNotSkull() {
        PvpSkull s = new PvpSkull();
        s.onAttack(a, b, 0, true);
        assertFalse(s.onAttack(b, a, 5, true), "b fights back against its attacker");
        assertFalse(s.isSkulled(b, 5));
        assertTrue(s.onAttack(b, c, 6, true), "attacking a third player is unprovoked");
        assertTrue(s.isSkulled(b, 6));
    }

    @Test
    void outsideTheWildernessNothingHappens() {
        PvpSkull s = new PvpSkull();
        assertFalse(s.onAttack(a, b, 0, false));
        assertFalse(s.isSkulled(a, 0));
        // ... and an attack outside does not count as provocation later.
        assertTrue(s.onAttack(b, a, 1, true));
    }

    @Test
    void attackingAgainRefreshesTheTimer() {
        PvpSkull s = new PvpSkull();
        s.onAttack(a, b, 0, true);
        s.onAttack(a, c, 1000, true);
        assertEquals(3000, s.ticksLeft(a, 1000));
    }

    @Test
    void emblemTraderSkullKeepsTheLongerTimer() {
        PvpSkull s = new PvpSkull();
        s.skull(a, 0, PvpSkull.EMBLEM_TRADER_TICKS);
        assertTrue(s.isSkulled(a, 1999));
        s.onAttack(a, b, 10, true);
        s.skull(a, 20, PvpSkull.EMBLEM_TRADER_TICKS);
        assertEquals(3010, s.ticksLeft(a, 0));
    }

    @Test
    void deathAndExpiry() {
        PvpSkull s = new PvpSkull();
        s.onAttack(a, b, 0, true);
        s.onAttack(c, b, 0, true);
        s.clear(a);
        assertFalse(s.isSkulled(a, 1));
        assertEquals(List.of(), s.expire(2999));
        assertEquals(List.of(c), s.expire(3000));
        assertFalse(s.isSkulled(c, 3000));
    }

    @Test
    void itemsKeptOnDeath() {
        PvpSkull s = new PvpSkull();
        assertEquals(3, s.itemsKept(a, 0, false));
        assertEquals(4, s.itemsKept(a, 0, true));
        s.onAttack(a, b, 0, true);
        assertEquals(0, s.itemsKept(a, 1, false));
        assertEquals(1, s.itemsKept(a, 1, true));
    }
}
