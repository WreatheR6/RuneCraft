package com.runecraft.osrs.content.dungeons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** The Charred Dungeon's south-central mine holds the 24 rubium rocks of the wiki (round 30: moved off the island surface). */
class CharredDungeonRubiumTest {
    @Test
    void rubiumRocksAreInTheDungeon() {
        assertEquals(48, CharredDungeon.CD_RUBIUM.length);
        long rocks = CharredDungeon.dungeon().structure().ops().stream().filter(o -> "runecraft:rubium_rock".equals(o.block())).count();
        assertEquals(24, rocks);
    }

    /** Kragin's two rubium deposits stand on the wiki's tiles (Rubium deposit ObjectLocLine), clear of the rubium rocks. */
    @Test
    void rubiumDepositsAreOnTheWikiTiles() {
        assertArrayEquals(new int[]{2679, 8852, 2685, 8847}, CharredDungeon.CD_RUBIUM_DEPOSITS);
        for (int i = 0; i < CharredDungeon.CD_RUBIUM_DEPOSITS.length; i += 2) {
            for (int j = 0; j < CharredDungeon.CD_RUBIUM.length; j += 2) {
                assertFalse(CharredDungeon.CD_RUBIUM_DEPOSITS[i] == CharredDungeon.CD_RUBIUM[j]
                        && CharredDungeon.CD_RUBIUM_DEPOSITS[i + 1] == CharredDungeon.CD_RUBIUM[j + 1], "deposit on a rubium rock");
            }
        }
        long deposits = CharredDungeon.dungeon().structure().ops().stream().filter(o -> "runecraft:rubium_deposit".equals(o.block())).count();
        assertEquals(2, deposits);
    }
}
