package com.runecraft.osrs.content.dungeons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Charred Dungeon's south-central mine holds the 24 rubium rocks of the wiki (round 30: moved off the island surface). */
class CharredDungeonRubiumTest {
    @Test
    void rubiumRocksAreInTheDungeon() {
        assertEquals(48, CharredDungeon.CD_RUBIUM.length);
        long rocks = CharredDungeon.dungeon().structure().ops().stream().filter(o -> "runecraft:rubium_rock".equals(o.block())).count();
        assertEquals(24, rocks);
    }
}
