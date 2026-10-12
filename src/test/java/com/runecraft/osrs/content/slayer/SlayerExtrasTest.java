package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.world.SpawnPoint;
import com.runecraft.osrs.world.SpawnTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boss tasks, the newer reward unlocks and the task creatures added by {@link SlayerExtraMonsters}. */
class SlayerExtrasTest {
    @Test
    void newTaskCreaturesExistAndSpawn() {
        List<String> ids = List.of("sourhog", "crocodile", "shadow_warrior", "otherworldly_being", "ent", "mammoth", "ice_spider",
                "bandit", "wilderness_bandit");
        for (String id : ids) {
            MonsterDef m = ContentRegistry.monster(id);
            assertNotNull(m, id);
            assertTrue(ContentRegistry.models().containsKey(id), id + " has no model");
            assertTrue(SpawnTable.all().stream().map(SpawnPoint::monster).anyMatch(id::equals), id + " has no spawn point");
        }
        assertEquals(37, ContentRegistry.monster("sourhog").combatLevel);
        assertEquals(63, ContentRegistry.monster("crocodile").combatLevel);
        assertEquals(48, ContentRegistry.monster("shadow_warrior").combatLevel);
        assertEquals(64, ContentRegistry.monster("otherworldly_being").combatLevel);
        assertEquals(86, ContentRegistry.monster("ent").combatLevel);
        assertEquals(80, ContentRegistry.monster("mammoth").combatLevel);
        assertEquals(61, ContentRegistry.monster("ice_spider").combatLevel);
        assertEquals(130, ContentRegistry.monster("wilderness_bandit").combatLevel);
        assertEquals("spiders", SlayerTasks.categoryOf(ContentRegistry.monster("ice_spider")));
        assertEquals("bandits", SlayerTasks.categoryOf(ContentRegistry.monster("desert_bandit")));
        assertEquals("chaos_druids", SlayerTasks.categoryOf(ContentRegistry.monster("chaos_druid")));
        for (String c : List.of("sourhogs", "crocodiles", "shadow_warriors", "otherworldly_beings", "ents", "mammoths", "bandits",
                "chaos_druids")) {
            assertTrue(SlayerTasks.available(c), c);
        }
        assertNotNull(SlayerMasters.get("spria").assignment("sourhogs"));
        assertNotNull(SlayerMasters.get("vannaka").assignment("shadow_warriors"));
        assertNotNull(SlayerMasters.get("krystilia").assignment("mammoths"));
    }

    @Test
    void steveCopiesNieve() {
        SlayerMasters.Master steve = SlayerMasters.get("steve");
        assertNotNull(steve);
        assertEquals(SlayerMasters.get("nieve").assignments(), steve.assignments());
        assertEquals("monkey_madness_ii", steve.quest());
        assertEquals(90, SlayerRewards.blockCost("steve"));
    }

    @Test
    void unlocksFollowOsrs() {
        assertEquals(200, SlayerRewards.unlock("like_a_boss").cost());
        assertEquals(500, SlayerRewards.unlock("stop_the_wyvern").cost());
        assertEquals(80, SlayerRewards.unlock("actual_vampyre_slayer").cost());
        assertEquals(100, SlayerRewards.unlock("more_at_stake").cost());
        assertEquals(100, SlayerRewards.unlock("birds_of_a_feather").cost());
        assertEquals("more_at_stake", SlayerRewards.extensionFor("vampyres").id());
        assertEquals("birds_of_a_feather", SlayerRewards.extensionFor("aviansies").id());
        SlayerMasters.Assignment vamp = SlayerMasters.get("duradel").assignment("vampyres");
        assertEquals("actual_vampyre_slayer", vamp.unlock());
        assertEquals(200, vamp.extMin());
        assertNull(SlayerMasters.get("mazchna").assignment("vampyres").unlock());
        SlayerMasters.Assignment wyv = SlayerMasters.get("duradel").assignment("fossil_island_wyverns");
        assertTrue(SlayerTasks.eligible(wyv, 99, 126, Set.of(), List.of(), null));
        assertFalse(SlayerTasks.eligible(wyv, 99, 126, Set.of("stop_the_wyvern"), List.of(), null));
    }

    @Test
    void bossTasks() {
        assertEquals(12, SlayerMasters.get("duradel").assignment(SlayerBossTasks.CATEGORY).weight());
        for (String m : List.of("konar", "nieve", "steve", "krystilia")) {
            SlayerMasters.Assignment a = SlayerMasters.get(m).assignment(SlayerBossTasks.CATEGORY);
            assertEquals(8, a.weight(), m);
            assertEquals(SlayerBossTasks.UNLOCK, a.unlock(), m);
        }
        SlayerMasters.Assignment boss = SlayerMasters.get("duradel").assignment(SlayerBossTasks.CATEGORY);
        assertFalse(SlayerTasks.eligible(boss, 99, 126, Set.of(), List.of(), null));
        assertTrue(SlayerTasks.eligible(boss, 99, 126, Set.of("like_a_boss"), List.of(), null));
        for (SlayerBossTasks.BossTask t : SlayerBossTasks.all()) {
            assertTrue(SlayerBossTasks.exists(t), t.id() + " names no boss in the game");
            assertNotNull(SlayerCategories.get(t.id()), t.id());
            assertTrue(SlayerTasks.available(t.id()), t.id());
        }
        // Slayer level gates: Cerberus needs 91.
        assertTrue(SlayerBossTasks.choices(90, false, List.of(), null).stream().noneMatch(t -> t.id().equals("boss_cerberus")));
        assertTrue(SlayerBossTasks.choices(91, false, List.of(), null).stream().anyMatch(t -> t.id().equals("boss_cerberus")));
        // Krystilia only assigns Wilderness bosses.
        Random r = new Random(7);
        for (int i = 0; i < 50; i++) {
            assertTrue(SlayerBossTasks.pick(r, 99, true, List.of(), null).wilderness());
        }
        // Cerberus counts for both a hellhound task and a Cerberus boss task; Artio counts for Callisto's.
        assertTrue(SlayerTasks.countsFor("hellhounds", "cerberus", "hellhounds"));
        assertTrue(SlayerTasks.countsFor("boss_cerberus", "cerberus", "hellhounds"));
        assertTrue(SlayerTasks.countsFor("boss_callisto", "artio", null));
        assertFalse(SlayerTasks.countsFor("boss_callisto", "venenatis", null));
        assertTrue(SlayerTasks.countsFor("boss_general_graardor", "general_graardor", null));
        assertEquals("cave_kraken", ContentRegistry.boss("kraken").slayerCategory);
    }

    @Test
    void newDrops() {
        // OSRS Wiki rates (MonsterDropsTable): 1/10,000 for the Kebos dragon ammunition, wyvern granite 1/2,560 and 1/512.
        assertEquals(10000, rate("drake", "dragon_thrownaxe"));
        assertEquals(10000, rate("wyrm", "dragon_knife"));
        assertEquals(10001, rate("hydra", "dragon_knife"));
        assertEquals(10000, rate("hydra", "dragon_thrownaxe"));
        assertEquals(2560, rate("spitting_wyvern", "granite_boots"));
        assertEquals(512, rate("taloned_wyvern", "granite_longsword"));
        assertEquals(1, rate("bloodveld", "vile_ashes"));
        assertEquals(1, rate("imp", "fiendish_ashes"));
        assertEquals(1, rate("nechryael", "malicious_ashes"));
        assertEquals(5000, rate("hobgoblin", "hobgoblin_champion_scroll"));
        assertTrue(ContentRegistry.boss("cerberus").drops.stream().anyMatch(d -> d.item().equals("infernal_ashes")));
        assertTrue(com.runecraft.osrs.data.Monster.LESSER_DEMON.drops().stream()
                .anyMatch(d -> d.item().equals("lesser_demon_champion_scroll") && d.rarity() == 5000));
        assertTrue(com.runecraft.osrs.data.Monster.BLACK_DEMON.drops().stream().anyMatch(d -> d.item().equals("malicious_ashes")));
        for (String item : List.of("dragon_knife", "dragon_thrownaxe", "granite_boots", "granite_longsword", "rune_spear",
                "infernal_ashes", "giant_champion_scroll")) {
            assertNotNull(ItemCatalog.get(item), item);
        }
    }

    private static int rate(String monster, String item) {
        for (Drop d : ContentRegistry.monster(monster).drops) {
            if (d.item().equals(item)) {
                return d.rarity();
            }
        }
        return -1;
    }
}
