package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tormented Demons: wiki / DPS calculator stats, uniques, slayer task membership and their temple camp. */
class TormentedDemonsTest {
    private static MonsterDef td() {
        return SlayerContent.allMonsters().stream().filter(m -> m.id.equals(TormentedDemons.ID)).findFirst().orElseThrow();
    }

    @Test
    void stats() {
        MonsterDef m = td();
        assertEquals(450, m.combatLevel);
        assertEquals(600, m.hitpoints);
        assertEquals(150, m.defence);
        assertEquals(6, m.attackSpeed);
        assertEquals(400, m.attackBonus(AttackType.SLASH));
        assertEquals(300, m.attackBonus(AttackType.MAGIC));
        assertEquals(175, m.defenceBonus(AttackType.SLASH));
        assertEquals(68, m.defenceBonus(AttackType.CRUSH));
        assertEquals(5, m.defenceBonus(AttackType.MAGIC));
        assertEquals(31, m.attacks.stream().filter(a -> a.name().equals("claws")).findFirst().orElseThrow().maxHit());
        assertEquals(45, m.attacks.stream().filter(a -> a.name().equals("fire_bomb")).findFirst().orElseThrow().maxHit());
        assertTrue(m.hasAttribute("demon"));
        assertEquals("greater_demons", m.slayerCategory);
    }

    @Test
    void uniquesAndCollectionLog() {
        MonsterDef m = td();
        assertTrue(m.drops.contains(Drop.of("tormented_synapse", 1, 1, 500)));
        assertTrue(m.drops.contains(Drop.of("burning_claw", 1, 1, 500)));
        assertTrue(m.drops.contains(Drop.always("infernal_ashes")));
        CollectionLogData.Page page = new SlayerContent().collectionLog().stream()
                .filter(p -> p.name().equals("Tormented Demons")).findFirst().orElseThrow();
        assertTrue(page.items().contains("tormented_synapse"));
        for (String id : new String[]{"burning_claws", "emberlight", "scorching_bow", "purging_staff"}) {
            ItemDef d = ItemCatalog.get(id);
            assertNotNull(d, id);
            assertNotNull(d.equipment(), id);
        }
        assertEquals(124, ItemCatalog.get("scorching_bow").equipment().stats().rangedAttack());
        assertEquals(70, ItemCatalog.get("emberlight").equipment().stats().attack(AttackType.SLASH));
        assertEquals(77, ItemCatalog.get("purging_staff").equipment().requirements().get(Skill.MAGIC));
        Recipe bow = Recipes.get("fletch_scorching_bow");
        assertEquals(74, bow.level());
        assertEquals(1, bow.inputs().get("tormented_synapse"));
        assertEquals(2, Recipes.get("make_burning_claws").inputs().get("burning_claw"));
        assertEquals(74, Recipes.get("craft_purging_staff").level());
    }

    @Test
    void mechanics() {
        assertEquals(80, TormentedDemons.throughShield(100, true, "abyssal_whip"));
        assertEquals(100, TormentedDemons.throughShield(100, true, "emberlight"));
        assertEquals(100, TormentedDemons.throughShield(100, false, "abyssal_whip"));
        assertFalse(TormentedDemons.swapsPrayer(149, 600));
        assertTrue(TormentedDemons.swapsPrayer(150, 600));
    }

    @Test
    void assignedAsGreaterDemonsAndCamped() {
        boolean assigned = false;
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            if (m.npcId().equals("duradel") || m.npcId().equals("nieve") || m.npcId().equals("konar")) {
                assigned |= m.assignments().stream().anyMatch(a -> a.category().equals("greater_demons"));
            }
        }
        assertTrue(assigned);
        assertTrue(Dungeons.all().stream().flatMap(d -> d.camps().stream())
                .anyMatch(c -> c.creatures().contains(TormentedDemons.ID)));
    }
}
