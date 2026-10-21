package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The protection-prayer switch grace (round 41 playability) and the quick-prayer mask. */
class PrayerGraceTest {
    @Test
    void constantsAreTheBriefsWindow() {
        assertEquals(8, PrayerGrace.GRACE_MC); // 0.4 s
        assertEquals(12, PrayerGrace.MIN_ON_MC); // one OSRS game tick
    }

    @Test
    void onNowAlwaysProtects() {
        assertTrue(PrayerGrace.protectedWithGrace(true, 100, PrayerGrace.NEVER, 100));
        assertTrue(PrayerGrace.protectedWithGrace(true, PrayerGrace.NEVER, PrayerGrace.NEVER, 5000));
    }

    @Test
    void offFiveTicksAgoAfterTwentyOnStillProtects() {
        long onAt = 100;
        long offAt = 120;
        assertTrue(PrayerGrace.protectedWithGrace(false, onAt, offAt, 125));
        // The window edge: exactly 8 ticks after the switch still counts, 9 doesn't.
        assertTrue(PrayerGrace.protectedWithGrace(false, onAt, offAt, 128));
        assertFalse(PrayerGrace.protectedWithGrace(false, onAt, offAt, 129));
    }

    @Test
    void offFiveTicksAgoAfterOnlySixOnDoesNot() {
        assertFalse(PrayerGrace.protectedWithGrace(false, 114, 120, 125));
        // Exactly one game tick on is enough.
        assertTrue(PrayerGrace.protectedWithGrace(false, 108, 120, 125));
    }

    @Test
    void offNineTicksAgoDoesNot() {
        assertFalse(PrayerGrace.protectedWithGrace(false, 100, 120, 129));
    }

    @Test
    void neverOnDoesNot() {
        assertFalse(PrayerGrace.protectedWithGrace(false, PrayerGrace.NEVER, PrayerGrace.NEVER, 0));
        assertFalse(PrayerGrace.protectedWithGrace(false, PrayerGrace.NEVER, PrayerGrace.NEVER, 50));
    }

    @Test
    void reactivatedThenForcedOffHasNoGrace() {
        // On at 100, voluntarily off at 130, on again at 140, then removed by force (no off stamp): the old off stamp is
        // older than the new on stamp, so nothing is graced.
        assertFalse(PrayerGrace.protectedWithGrace(false, 140, 130, 141));
    }

    @Test
    void indicesMatchStyles() {
        assertEquals(0, PrayerGrace.index(AttackType.MAGIC));
        assertEquals(1, PrayerGrace.index(AttackType.RANGED));
        assertEquals(2, PrayerGrace.index(AttackType.STAB));
        assertEquals(2, PrayerGrace.index(AttackType.SLASH));
        assertEquals(2, PrayerGrace.index(AttackType.CRUSH));
        for (AttackType t : AttackType.values()) {
            Prayer p = PrayerGrace.protectionFor(t);
            assertEquals(PrayerGrace.index(t), PrayerGrace.index(p));
            // Same mapping as the strict check the rest of the game uses.
            assertTrue(NpcHitCalc.protectedFrom(List.of(p), t));
        }
        assertEquals(-1, PrayerGrace.index(Prayer.PIETY));
        assertEquals(-1, PrayerGrace.index((Prayer) null));
    }

    @Test
    void quickPrayerMaskRoundTrips() {
        Set<Prayer> quick = EnumSet.of(Prayer.PIETY, Prayer.PROTECT_FROM_MELEE, Prayer.MYSTIC_VIGOUR, Prayer.THICK_SKIN);
        long mask = PrayerGrace.mask(quick);
        assertEquals(quick, PrayerGrace.fromMask(mask));
        assertEquals(EnumSet.noneOf(Prayer.class), PrayerGrace.fromMask(0));
        // Every prayer fits a long (the active-prayer sync uses an int: 31 prayers).
        assertTrue(Prayer.values().length <= 63);
        assertEquals(EnumSet.allOf(Prayer.class), PrayerGrace.fromMask(PrayerGrace.mask(EnumSet.allOf(Prayer.class))));
        // Bits past the enum are ignored.
        assertEquals(EnumSet.of(Prayer.THICK_SKIN), PrayerGrace.fromMask(1L | (1L << 62)));
    }

    @Test
    void overheadTagsAreDistinct() {
        assertEquals("MAG", PrayerGrace.overheadTag(Prayer.PROTECT_FROM_MAGIC));
        assertEquals("RNG", PrayerGrace.overheadTag(Prayer.PROTECT_FROM_MISSILES));
        assertEquals("MEL", PrayerGrace.overheadTag(Prayer.PROTECT_FROM_MELEE));
        assertEquals("RET", PrayerGrace.overheadTag(Prayer.RETRIBUTION));
        assertEquals("RED", PrayerGrace.overheadTag(Prayer.REDEMPTION));
        assertEquals("SMI", PrayerGrace.overheadTag(Prayer.SMITE));
        assertNull(PrayerGrace.overheadTag(Prayer.PIETY));
        Set<String> seen = new java.util.HashSet<>();
        for (Prayer p : Prayer.values()) {
            if (p.isOverhead()) {
                assertTrue(seen.add(PrayerGrace.overheadTag(p)), p + " shares its tag");
            }
        }
        assertEquals(0xFF4080FF, PrayerGrace.overheadColor(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(0xFF40C040, PrayerGrace.overheadColor(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(0xFFE03030, PrayerGrace.overheadColor(Prayer.PROTECT_FROM_MELEE));
    }

    @Test
    void optimisticSwitchClearsConflictsLikeTheServer() {
        long piety = 1L << Prayer.PIETY.ordinal();
        long melee = 1L << Prayer.PROTECT_FROM_MELEE.ordinal();
        long magic = 1L << Prayer.PROTECT_FROM_MAGIC.ordinal();
        long after = PrayerGrace.switched(piety | melee, Prayer.PROTECT_FROM_MAGIC, true);
        assertEquals(piety | magic, after);
        assertEquals(piety, PrayerGrace.switched(piety | magic, Prayer.PROTECT_FROM_MAGIC, false));
        // Rigour replaces Piety.
        assertEquals(1L << Prayer.RIGOUR.ordinal(), PrayerGrace.switched(piety, Prayer.RIGOUR, true));
    }
}
