package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Surface multicombat zones against the OSRS multicombat map (RuneLite multi-lines polygons). */
class MultiCombatMapTest {
    @Test
    void townsAndWildernessFollowTheMulticombatMap() {
        assertTrue(MultiCombat.isMulti(3082, 3420), "Barbarian Village");
        assertTrue(MultiCombat.isMulti(3293, 3170), "Al Kharid palace");
        assertTrue(MultiCombat.isMulti(2965, 3380), "Falador");
        assertTrue(MultiCombat.isMulti(3110, 3160), "Wizards' Tower");
        assertTrue(MultiCombat.isMulti(2400, 3100), "Castle Wars arena");
        assertTrue(MultiCombat.isMulti(2850, 3490), "White Wolf Mountain");
        assertTrue(MultiCombat.isMulti(3200, 3530), "level 2 Wilderness north of Varrock");
        assertTrue(MultiCombat.isMulti(3030, 3880), "the Lava Maze");
        assertFalse(MultiCombat.isMulti(3360, 3700), "the eastern Wilderness strip east of x 3328");
        assertFalse(MultiCombat.isMulti(3100, 3560), "low Wilderness west of the multi line");
        assertFalse(MultiCombat.isMulti(3165, 3485), "Grand Exchange");
        assertFalse(MultiCombat.isMulti(2757, 3477), "Camelot");
    }

    @Test
    void zeahZonesAndSingleWayPockets() {
        assertTrue(MultiCombat.isMulti(1639, 3675), "Kourend Castle");
        assertTrue(MultiCombat.isMulti(1500, 3600), "Shayzien");
        assertFalse(MultiCombat.isMulti(1505, 3617), "Shayzien bank");
        assertTrue(MultiCombat.isMulti(1505, 3780), "Lovakengj");
        assertTrue(MultiCombat.isMulti(1436, 3708), "lizardman shamans");
        assertTrue(MultiCombat.isMulti(1860, 3560), "eastern Hosidius");
        assertTrue(MultiCombat.isMulti(1750, 3475), "Hosidius sand crab beach");
        assertTrue(MultiCombat.isMulti(3655, 3875), "Fossil Island ammonite crabs");
        assertFalse(MultiCombat.isMulti(1680, 3130), "Civitas illa Fortis");
        assertFalse(MultiCombat.isMulti(1762, 3620), "Hosidius' square");
        // Falador is multi, except the ground floor of the castle's south-west corner
        assertTrue(MultiCombat.isMulti(2970, 3340), "Falador Castle");
        assertFalse(MultiCombat.isMulti(2963, 3332), "Falador Castle's south-west corner");
        assertFalse(MultiCombat.isMulti(2960, 3328), "its corner tile");
        assertTrue(MultiCombat.isMulti(2968, 3336), "just outside the pocket");
    }
}
