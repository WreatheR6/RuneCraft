package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.minigames.MinigameRules.BrimhavenArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Brimhaven Agility Arena after the May 2024 rework (OSRS Wiki "Brimhaven Agility Arena"). */
class BrimhavenArenaTest {
    @Test
    void taggingPays30PerTenLevels() {
        assertEquals(0, BrimhavenArena.tagXp(9));
        assertEquals(30, BrimhavenArena.tagXp(10));
        assertEquals(120, BrimhavenArena.tagXp(40));
        assertEquals(270, BrimhavenArena.tagXp(99));
        assertEquals(300, BrimhavenArena.tagXp(100), "300 when boosted to 100");
        assertEquals(24, BrimhavenArena.DISPENSERS);
        assertEquals(100, BrimhavenArena.MOVE_TICKS, "the dispenser moves every minute");
        assertEquals(0.10, BrimhavenArena.DOUBLE_TICKET_CHANCE);
    }

    @Test
    void obstaclesFollowTheWikiTable() {
        assertEquals(20, BrimhavenArena.Obstacle.ROPE_SWING.xp);
        assertEquals(26, BrimhavenArena.Obstacle.PRESSURE_PAD.xp);
        assertEquals(20, BrimhavenArena.Obstacle.PRESSURE_PAD.level);
        assertEquals(24, BrimhavenArena.Obstacle.FLOOR_SPIKES.xp);
        assertEquals(40, BrimhavenArena.Obstacle.SPINNING_BLADES.level);
        assertEquals(30, BrimhavenArena.Obstacle.DARTS.xp);
        for (BrimhavenArena.Obstacle o : BrimhavenArena.Obstacle.values()) {
            assertEquals(0, BrimhavenArena.failChance(o, 99), o + " can't fail at 99");
            assertTrue(BrimhavenArena.failChance(o, Math.max(1, o.level)) > 0, o + " can fail at its level");
        }
        assertTrue(BrimhavenArena.neighbours(0, 1) && BrimhavenArena.neighbours(0, 5));
        assertTrue(!BrimhavenArena.neighbours(4, 5), "no wrapping across rows");
        assertEquals(BrimhavenArena.link(3, 8), BrimhavenArena.link(8, 3), "a link is the same obstacle both ways");
    }
}
