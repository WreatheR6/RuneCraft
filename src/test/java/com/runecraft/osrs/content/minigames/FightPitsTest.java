package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightPitsTest {
    @Test
    void monstersJoinOnTheWikiSchedule() {
        // 90 s, 3.5, 5.5, 7.5 and 9.5 minutes; gas at 11.5 minutes (0.6 s ticks).
        assertTrue(FightPits.monsters(149).isEmpty());
        assertEquals(List.of("tz_kih"), FightPits.monsters(150));
        assertEquals(List.of("tz_kih", "tz_kek"), FightPits.monsters(350));
        assertEquals(List.of("tz_kih", "tz_kek", "tok_xil", "ket_zek", "tztok_jad"), FightPits.monsters(950));
        assertEquals(690, FightPits.GAS_TICK * 6 / 10);
        for (String id : FightPits.monsters(10_000)) {
            assertTrue(id.equals(TzHaarWaves.JAD) || ContentRegistry.monster(id) != null, id);
        }
    }

    @Test
    void championWinsTheDefeatedPlayersCombatLevels() {
        assertEquals(126 + 90 + 3, FightPits.tokkul(List.of(126, 90, 3)));
        assertTrue(ItemCatalog.exists("tokkul"));
        assertNotNull(Minigame.byId("fight_pits"));
        assertEquals(2398, Minigame.FIGHT_PITS.realX());
        assertEquals(5151, Minigame.FIGHT_PITS.realY());
        assertEquals(3600, FightPits.SKULL_TICKS * 6 / 10);
    }

    @Test
    void championSkullIsLostTheWikiWays() {
        long until = 1000 + FightPits.SKULL_TICKS;
        assertEquals(null, FightPits.skullLoss(1000, until, 5, true, false, false));
        assertEquals(FightPits.SkullLoss.EXPIRED, FightPits.skullLoss(until, until, 0, true, false, false));
        assertEquals(FightPits.SkullLoss.TELEPORT, FightPits.skullLoss(1000, until, 500, true, false, false));
        assertEquals(FightPits.SkullLoss.TELEPORT, FightPits.skullLoss(1000, until, 0, false, false, false));
        assertEquals(FightPits.SkullLoss.GEAR, FightPits.skullLoss(1000, until, 0, true, true, false));
        assertEquals(FightPits.SkullLoss.OVERHEAD_PRAYER, FightPits.skullLoss(1000, until, 0, true, false, true));
        // The champion's skull is a team of its own, separate from the PvP skull (which decides items kept on death).
        assertTrue(!com.runecraft.osrs.PvpSkull.CHAMPION_TEAM.equals(com.runecraft.osrs.PvpSkull.TEAM));
    }
}
