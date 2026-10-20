package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.slayer.SlayerBossTasks;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every Slayer boss needs a task it counts towards (OSRS Wiki), not only its Slayer level. */
class SlayerBossRulesTest {
    @Test
    void everySlayerBossNeedsItsTask() {
        for (BossDef d : SlayerBosses.all()) {
            if (d.raidOnly) {
                continue; // later forms (Hydra phases) and Dawn are never summoned
            }
            SlayerBossRules.Access a = SlayerBossRules.of(d.id);
            assertNotNull(a, d.id + " has no task rule");
            BossDef def = ContentRegistry.boss(d.id);
            assertTrue(a.allows(def.slayerCategory), d.id + ": its own category " + def.slayerCategory + " must let it be fought");
            assertFalse(a.allows(null), d.id + " fought with no task");
            assertFalse(a.allows("cows"), d.id + " fought on a cow task");
            assertTrue(a.allows("boss_" + (d.id.equals("dusk") ? "grotesque_guardians" : d.id)), d.id + ": its boss task");
        }
        assertEquals(7, SlayerBossRules.all().size());
    }

    @Test
    void rulesNameRealTasks() {
        for (SlayerBossRules.Access a : SlayerBossRules.all().values()) {
            assertNotNull(ContentRegistry.boss(a.bossId()), a.bossId());
            for (String t : a.tasks()) {
                SlayerBossTasks.BossTask boss = SlayerBossTasks.get(t);
                assertTrue(SlayerCategories.get(t) != null || boss != null, a.bossId() + ": unknown task " + t);
                if (boss != null) {
                    assertTrue(boss.creatures().contains(a.bossId()), t + " doesn't count " + a.bossId() + " kills");
                }
            }
        }
        // Araxxor: a spider task works too (OSRS Wiki "he may only be killed while on a spider or araxyte Slayer task").
        assertTrue(SlayerBossRules.allows("araxxor", "spiders"));
        assertTrue(SlayerBossRules.allows("araxxor", "araxytes"));
        assertFalse(SlayerBossRules.allows("cerberus", "abyssal_demons"));
        assertTrue(SlayerBossRules.allows("zulrah", null), "bosses without a rule are free to fight");
    }

    @Test
    void araxxorsAraxytesAreNoTaskKills() {
        // OSRS Wiki: Slayer XP and the helmet bonus as araxytes, but no kill off the task.
        for (String id : new String[]{"acidic_araxyte", "mirrorback_araxyte", "ruptura_araxyte"}) {
            assertFalse(SlayerBossRules.taskKill(id), id);
            assertEquals("araxytes", ContentRegistry.monster(id).slayerCategory, id + " keeps its category (helmet bonus)");
        }
        assertEquals(58, com.runecraft.osrs.data.MonsterTraits.slayerXp("acidic_araxyte", 1), 1e-9);
        assertEquals(65, com.runecraft.osrs.data.MonsterTraits.slayerXp("ruptura_araxyte", 1), 1e-9);
        assertTrue(SlayerBossRules.taskKill("araxxor"));
        assertTrue(SlayerBossRules.taskKill("araxyte"));
        assertTrue(SlayerBossRules.taskKill(null));
    }

    @Test
    void brittleKey() {
        assertNotNull(ItemCatalog.get(SlayerBossRules.BRITTLE_KEY));
        assertEquals(150, SlayerBossRules.BRITTLE_KEY_RATE);
        assertNotNull(SlayerCategories.get(SlayerBossRules.BRITTLE_KEY_TASK));
    }
}
