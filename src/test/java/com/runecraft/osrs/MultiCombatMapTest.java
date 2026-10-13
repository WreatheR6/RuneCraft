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
}
