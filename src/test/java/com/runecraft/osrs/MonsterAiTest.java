package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Open-world monster AI rules (round 42): aggression, ranges, leash, retaliation, the single-way lock and kill credit. */
class MonsterAiTest {
    @Test
    void combatLevelRule() {
        // wiki example: a level 28 hobgoblin attacks a level 51 player but not a level 57 one
        assertTrue(MonsterAi.aggressiveTo(28, 51, false));
        assertTrue(MonsterAi.aggressiveTo(28, 56, false));
        assertFalse(MonsterAi.aggressiveTo(28, 57, false));
        assertTrue(MonsterAi.aggressiveTo(28, 126, true), "aggressive regardless of level (Wilderness, bosses)");
        assertTrue(MonsterAi.aggressiveTo(63, 126, false), "level 63+ attacks every player");
        assertFalse(MonsterAi.aggressiveTo(27, 126, false), "a giant spider ignores a maxed player");
    }

    @Test
    void ranges() {
        assertEquals(14.0, MonsterAi.MAX_RANGE_BLOCKS, "OSRS max range 7 tiles");
        assertEquals(16.0, MonsterAi.aggressionRange(1.5), "melee: max range + 1 tile");
        assertEquals(24.0, MonsterAi.aggressionRange(10), "a 10-block ranged attack");
        assertTrue(MonsterAi.inAggressionRange(15.9, 1.5));
        assertFalse(MonsterAi.inAggressionRange(16.1, 1.5));
        assertEquals(10.0, MonsterAi.huntBlocks("giant_spider"));
        assertTrue(MonsterAi.huntBlocks("jungle_horror") > MonsterAi.HUNT_BLOCKS);
        assertTrue(MonsterAi.huntBlocks("mountain_troll") < MonsterAi.HUNT_BLOCKS);
        assertTrue(MonsterAi.HUNT_BLOCKS >= 8 && MonsterAi.HUNT_BLOCKS <= 12);
    }

    @Test
    void leash() {
        assertFalse(MonsterAi.giveUp(10, 12, 1.5));
        assertTrue(MonsterAi.giveUp(20.5, 12, 1.5), "past the leash");
        assertTrue(MonsterAi.giveUp(12, 20.5, 1.5), "target fled past the aggression range + slack");
        assertFalse(MonsterAi.giveUp(12, 20.5, 10), "a ranged monster reaches further");
        assertTrue(MonsterAi.LEASH_BLOCKS >= 16 && MonsterAi.LEASH_BLOCKS <= 20);
    }

    @Test
    void retaliationIsSticky() {
        assertTrue(MonsterAi.switchTarget(false, false, false, true), "no target: retaliate");
        assertFalse(MonsterAi.switchTarget(true, true, true, true), "a valid target is kept");
        assertTrue(MonsterAi.switchTarget(true, false, true, true), "dead or gone target");
        assertTrue(MonsterAi.switchTarget(true, true, false, true), "unreachable target, reachable attacker");
        assertFalse(MonsterAi.switchTarget(true, true, false, false), "both out of reach: keep");
    }

    @Test
    void singleWayLockReleasesForAMonsterThatLeft() {
        assertTrue(MonsterAi.stillEngaged(true, 3, 1.5, true));
        assertFalse(MonsterAi.stillEngaged(true, 7, 1.5, true), "wandered off");
        assertFalse(MonsterAi.stillEngaged(true, 3, 1.5, false), "out of sight");
        assertFalse(MonsterAi.stillEngaged(false, 3, 1.5, true), "after someone else / gone home");
        assertTrue(MonsterAi.stillEngaged(true, 9, 10, true), "a ranged monster within its own range");

        SingleCombat sc = new SingleCombat();
        int player = 1;
        int goblin = 2;
        int spider = 3;
        sc.onHit(goblin, player, 100);
        // the goblin still holds the player: the spider may not attack, and the player may not attack the spider
        assertEquals(new SingleCombat.Refusal(SingleCombat.Verdict.TARGET_BUSY, goblin),
                sc.refusal(spider, player, 101, false, false, id -> true, id -> true));
        assertEquals(new SingleCombat.Refusal(SingleCombat.Verdict.ATTACKER_BUSY, goblin),
                sc.refusal(player, spider, 101, false, false, id -> true, id -> true));
        // the goblin walked away: the lock is released at once instead of 8 game ticks later
        assertEquals(SingleCombat.Refusal.NONE, sc.refusal(spider, player, 101, false, false, id -> id != goblin, id -> true));
        assertEquals(SingleCombat.Refusal.NONE, sc.refusal(player, spider, 101, false, false, id -> true, id -> id != goblin));
        // the old one-predicate check is unchanged
        assertEquals(SingleCombat.Verdict.TARGET_BUSY, sc.check(spider, player, 101, false, false, id -> true));
        assertEquals(SingleCombat.Verdict.OK, sc.check(spider, player, 101 + SingleCombat.COMBAT_TICKS, false, false, id -> true));
    }

    @Test
    void topDamagerGetsTheKill() {
        MonsterAi.DamageLedger<String> ledger = new MonsterAi.DamageLedger<>();
        assertNull(ledger.top());
        ledger.add("alice", 5);
        ledger.add("bob", 9);
        ledger.add("alice", 3);
        ledger.add("carol", 0);
        assertEquals("bob", ledger.top(), "most damage wins, not the last hit");
        ledger.add("alice", 1);
        assertEquals("alice", ledger.top(), "9 each: the first to hit wins the tie");
        assertEquals(18, ledger.total());
        assertEquals(0, ledger.of("carol"));
    }

    @Test
    void lootPrivacy() {
        assertTrue(MonsterAi.lootPrivate(0, true, false));
        assertTrue(MonsterAi.lootPrivate(MonsterAi.PRIVATE_LOOT_MC - 1, true, false));
        assertFalse(MonsterAi.lootPrivate(MonsterAi.PRIVATE_LOOT_MC, true, false), "visible to all after 60 s");
        assertFalse(MonsterAi.lootPrivate(0, true, true), "the Wilderness shows drops at once");
        assertTrue(MonsterAi.lootPrivate(100_000, false, true), "untradeables stay the winner's");
        assertEquals(3 * 60 * 20, MonsterAi.LOOT_LIFESPAN_MC);
    }
}
