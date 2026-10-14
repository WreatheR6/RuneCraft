package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Skotizo's altars, minions and the dark ankou (OSRS wiki). */
class SkotizoRulesTest {
    @Test
    void altarsReduceDamage() {
        assertEquals(40, SkotizoRules.damage(40, 0, false));
        assertEquals(30, SkotizoRules.damage(40, 1, false), "25% per altar");
        assertEquals(0, SkotizoRules.damage(40, 4, false), "four altars: immune");
        assertEquals(34, SkotizoRules.damage(40, 1, true), "Arclight/Emberlight: 15% per altar");
        assertEquals(16, SkotizoRules.damage(40, 4, true), "capped at 60% with Arclight");
        assertTrue(SkotizoRules.altarBreaker("arclight") && SkotizoRules.altarBreaker("emberlight"));
        assertFalse(SkotizoRules.altarBreaker("silverlight") || SkotizoRules.altarBreaker("darklight"));
    }

    @Test
    void skotizoDefinition() {
        BossDef s = ContentRegistry.boss("skotizo");
        assertEquals(250, s.strength);
        assertEquals(SkotizoRules.SPAWN_BELOW_PERCENT, s.phases.get(0).belowPercent());
        assertEquals("reanimated_demon_spawn", s.minion);
        assertEquals(3, s.minionCount);
        MonsterDef spawn = ContentRegistry.monster("reanimated_demon_spawn");
        assertEquals(85, spawn.hitpoints);
        MonsterDef ankou = ContentRegistry.monster("dark_ankou");
        assertNotNull(ankou);
        assertEquals(95, ankou.combatLevel);
        assertEquals(60, ankou.hitpoints);
        assertEquals("ankou", ankou.slayerCategory);
    }
}
