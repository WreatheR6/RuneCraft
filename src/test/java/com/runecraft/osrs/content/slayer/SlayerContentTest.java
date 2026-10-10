package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerContentTest {
    private static MonsterDef monster(String id) {
        MonsterDef m = ContentRegistry.monster(id);
        assertNotNull(m, id);
        return m;
    }

    private static boolean drops(MonsterDef m, String item, int rarity) {
        return m.drops.stream().anyMatch(d -> d.item().equals(item) && d.rarity() == rarity);
    }

    @Test
    void faithfulKeyMonsters() {
        MonsterDef abyssal = monster("abyssal_demon");
        assertEquals(124, abyssal.combatLevel);
        assertEquals(150, abyssal.hitpoints);
        assertEquals(85, abyssal.slayerLevel);
        assertTrue(drops(abyssal, "abyssal_whip", 512));
        assertTrue(drops(abyssal, "abyssal_dagger", 32000));
        MonsterDef garg = monster("gargoyle");
        assertEquals(111, garg.combatLevel);
        assertEquals(75, garg.slayerLevel);
        assertTrue(drops(garg, "granite_maul", 512));
        assertEquals(85, monster("abyssal_demon").slayerLevel);
        assertEquals(93, monster("smoke_devil").slayerLevel);
        assertEquals(95, monster("hydra").slayerLevel);
        assertTrue(drops(monster("dark_beast"), "dark_bow", 512));
        assertTrue(drops(monster("cave_horror"), "black_mask", 512));
        assertTrue(drops(monster("kurask"), "leaf_bladed_sword", 384));
        assertEquals(227, monster("black_dragon").combatLevel);
        assertEquals(380, monster("rune_dragon").combatLevel);
        assertTrue(monster("cave_kraken").immuneTo.contains(com.runecraft.osrs.AttackType.SLASH));
    }

    @Test
    void adultDragonsBreatheFireAndDropBones() {
        for (MonsterDef m : SlayerContent.allMonsters()) {
            if (m.id.endsWith("_dragon") && !m.id.startsWith("baby_") && Superiors.byId(m.id) == null) {
                assertTrue(m.attacks.stream().anyMatch(a -> a.special() == AttackDef.Special.DRAGONFIRE), m.id);
                assertTrue(m.drops.stream().anyMatch(d -> d.item().endsWith("dragon_bones") && d.rarity() == 1), m.id);
            }
        }
    }

    @Test
    void monstersHaveCategoriesModelsAndSpawns() {
        for (MonsterDef m : SlayerContent.allMonsters()) {
            assertNotNull(m.slayerCategory, m.id);
            SlayerCategories.Category c = SlayerCategories.get(m.slayerCategory);
            assertNotNull(c, m.id + " category " + m.slayerCategory);
            assertTrue(m.slayerLevel >= c.level(), m.id + " slayer level below its category's");
            assertTrue(m.hitpoints <= Superiors.MAX_HP, m.id + " exceeds Minecraft's health cap");
            if (Superiors.byId(m.id) == null) {
                assertTrue(!m.spawns.isEmpty(), m.id + " never spawns");
            } else {
                assertTrue(m.spawns.isEmpty(), m.id + " superiors must not spawn naturally");
            }
            assertTrue(m.model == null || ContentRegistry.models().containsKey(m.model), m.id);
            for (Drop d : m.drops) {
                assertTrue(d.rarity() >= 1 && d.min() <= d.max(), m.id + " " + d);
            }
        }
        assertTrue(SlayerContent.allMonsters().size() >= 100);
    }

    @Test
    void builtInCategoriesPointAtRealMonsters() {
        for (SlayerCategories.Category c : SlayerCategories.all().values()) {
            for (String b : c.builtIns()) {
                assertNotNull(Monster.valueOf(b), c.id());
            }
            assertTrue(!c.regions().isEmpty(), c.id());
        }
        assertEquals("goblins", SlayerCategories.ofBuiltIn("GOBLIN").id());
    }

    @Test
    void mastersAreConsistent() {
        Set<String> npcs = new HashSet<>();
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            assertTrue(npcs.add(m.npcId()));
            assertNotNull(ContentRegistry.npc(m.npcId()), m.id() + " has no NPC");
            Set<String> seen = new HashSet<>();
            int available = 0;
            for (SlayerMasters.Assignment a : m.assignments()) {
                assertTrue(seen.add(a.category()), m.id() + " lists " + a.category() + " twice");
                assertNotNull(SlayerCategories.get(a.category()), m.id() + " " + a.category());
                assertTrue(a.weight() > 0 && a.min() > 0 && a.min() <= a.max(), m.id() + " " + a.category());
                assertTrue(a.extMin() <= a.extMax(), m.id() + " " + a.category());
                assertTrue(a.unlock() == null || SlayerRewards.unlock(a.unlock()) != null, a.unlock());
                if (SlayerTasks.available(a.category())) {
                    available++;
                    assertTrue(!SlayerTasks.regions(a.category()).isEmpty(), a.category());
                }
            }
            assertTrue(available >= 10, m.id() + " has only " + available + " assignable tasks");
        }
    }

    @Test
    void assignmentRespectsRequirements() {
        SlayerMasters.Master duradel = SlayerMasters.get("duradel");
        Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            SlayerMasters.Assignment a = duradel.pick(random,
                    x -> SlayerTasks.eligible(x, 60, 110, Set.of(), List.of("dark_beasts"), null));
            assertNotNull(a);
            assertTrue(SlayerCategories.get(a.category()).level() <= 60, a.category());
            assertTrue(a.unlock() == null, a.category());
            assertTrue(!a.category().equals("dark_beasts"));
            int amount = SlayerTasks.amount(a, false, random);
            assertTrue(amount >= a.min() && amount <= a.max());
        }
        assertNull(duradel.pick(random, x -> false));
        assertEquals("turael", SlayerMasters.bestFor(10, 1).id());
        assertEquals("vannaka", SlayerMasters.bestFor(50, 1).id());
        assertEquals("nieve", SlayerMasters.bestFor(100, 49).id());
        assertEquals("duradel", SlayerMasters.bestFor(100, 50).id());
    }

    @Test
    void slayerPoints() {
        assertEquals(0, SlayerMasters.points(15, 4));
        assertEquals(15, SlayerMasters.points(15, 5));
        assertEquals(75, SlayerMasters.points(15, 10));
        assertEquals(225, SlayerMasters.points(15, 50));
        assertEquals(375, SlayerMasters.points(15, 100));
        assertEquals(525, SlayerMasters.points(15, 250));
        assertEquals(750, SlayerMasters.points(15, 1000));
        assertEquals(0, SlayerMasters.get("turael").pointsFor(10));
        assertEquals(125, SlayerMasters.get("krystilia").pointsFor(20));
        assertEquals(18, SlayerMasters.get("konar").pointsFor(7));
    }

    @Test
    void rewardsReferenceRealItems() {
        for (SlayerRewards.ShopItem s : SlayerRewards.SHOP) {
            assertNotNull(ItemCatalog.get(s.item()), s.item());
        }
        for (SlayerRewards.PointBuy b : SlayerRewards.BUY) {
            assertNotNull(ItemCatalog.get(b.item()), b.item());
        }
        for (SlayerRewards.Craft c : SlayerRewards.CRAFTS) {
            assertNotNull(ItemCatalog.get(c.product()), c.product());
            c.inputs().forEach(i -> assertNotNull(ItemCatalog.get(i.item()), i.item()));
            assertTrue(c.tool() == null || ItemCatalog.get(c.tool()) != null, c.tool());
            assertTrue(c.unlock() == null || SlayerRewards.unlock(c.unlock()) != null, c.unlock());
        }
        Set<String> ids = new HashSet<>();
        for (SlayerRewards.Unlock u : SlayerRewards.UNLOCKS) {
            assertTrue(ids.add(u.id()), u.id());
            assertTrue(u.target() == null || SlayerCategories.get(u.target()) != null, u.id());
        }
        assertEquals("pedal_to_the_metals", SlayerRewards.extensionFor("steel_dragons").id());
        for (List<Drop> table : List.of(SlayerChests.BRIMSTONE_MAIN, SlayerChests.BRIMSTONE_RARE, SlayerChests.LARRAN_MAIN,
                SlayerChests.LARRAN_RARE)) {
            table.forEach(d -> assertTrue(ItemCatalog.exists(d.item()), d.item()));
        }
    }

    @Test
    void gearRules() {
        for (SlayerGear.Protection p : SlayerGear.PROTECTION) {
            assertNotNull(SlayerCategories.get(p.category()), p.category());
            assertTrue(p.items().stream().anyMatch(ItemCatalog::exists), p.category());
        }
        for (SlayerGear.Finisher f : SlayerGear.FINISHERS) {
            assertNotNull(SlayerCategories.get(f.category()));
            assertTrue(ItemCatalog.exists(f.item()), f.item());
            assertNotNull(SlayerRewards.unlock(f.unlock()), f.unlock());
        }
        SlayerGear.LEAF_BLADED_WEAPONS.forEach(w -> assertTrue(ItemCatalog.exists(w), w));
        SlayerGear.MELEE_BOOST_HEADGEAR.forEach(w -> assertTrue(ItemCatalog.exists(w), w));
        assertNotNull(SlayerGear.protection("banshees"));
    }

    @Test
    void keyChances() {
        assertEquals(100, SlayerChests.brimstoneKeyChance(100));
        assertEquals(50, SlayerChests.brimstoneKeyChance(400));
        assertTrue(SlayerChests.brimstoneKeyChance(50) > 100);
        assertTrue(SlayerChests.larranKeyChance(30) > SlayerChests.larranKeyChance(80));
        assertTrue(SlayerChests.larranKeyChance(300) >= 50);
    }

    @Test
    void superiors() {
        for (Superiors.Superior s : Superiors.all().values()) {
            MonsterDef base = monster(s.base());
            MonsterDef sup = monster(s.id());
            assertEquals(base.slayerCategory, sup.slayerCategory, s.id());
            assertTrue(sup.combatLevel > base.combatLevel, s.id());
            assertTrue(sup.drops.stream().anyMatch(d -> d.item().equals("imbued_heart")), s.id());
            assertTrue(s.xp() >= sup.hitpoints, s.id());
        }
        assertEquals(4200, Superiors.forBase("abyssal_demon").xp());
    }

    @Test
    void modelsAreReasonable() {
        List<ModelSpec> models = SlayerModels.all();
        assertTrue(models.size() >= 50);
        for (ModelSpec m : models) {
            int boxes = m.parts().stream().mapToInt(p -> p.boxes.size()).sum();
            assertTrue(boxes <= 40, m.id() + " has " + boxes + " boxes");
            assertTrue(m.texWidth() <= 256, m.id());
            Set<String> names = new HashSet<>();
            m.parts().forEach(p -> assertTrue(names.add(p.name), m.id() + " duplicate part " + p.name));
        }
    }
}
