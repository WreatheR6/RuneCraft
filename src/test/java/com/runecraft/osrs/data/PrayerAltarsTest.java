package com.runecraft.osrs.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bone offering multipliers per altar (OSRS wiki, Prayer training). */
class PrayerAltarsTest {
    @Test
    void ordinaryAltarsTakeNoOfferings() {
        assertEquals(PrayerAltars.Kind.REGULAR, PrayerAltars.at(3243, 3207)); // Lumbridge church
        assertEquals(PrayerAltars.Kind.REGULAR, PrayerAltars.at(3236, 3634)); // Chaos Temple hut (level 12): ordinary
        assertEquals(0.0, PrayerAltars.Kind.REGULAR.multiplier);
    }

    @Test
    void chaosAltarAndEctofuntus() {
        assertEquals(PrayerAltars.Kind.CHAOS, PrayerAltars.at(2949, 3820));
        assertEquals(3.5, PrayerAltars.Kind.CHAOS.multiplier);
        assertEquals(0.5, PrayerAltars.CHAOS_SAVE_CHANCE);
        assertEquals(PrayerAltars.Kind.ECTOFUNTUS, PrayerAltars.at(3660, 3521));
        assertEquals(4.0, PrayerAltars.Kind.ECTOFUNTUS.multiplier);
    }

    @Test
    void gildedAltarBurners() {
        assertEquals(2.5, PrayerAltars.gilded(0));
        assertEquals(3.0, PrayerAltars.gilded(1));
        assertEquals(3.5, PrayerAltars.gilded(2));
        assertEquals(3.5, PrayerAltars.gilded(5));
        assertEquals(Bone.ALTAR_MULTIPLIER, PrayerAltars.gilded(2));
    }
}
