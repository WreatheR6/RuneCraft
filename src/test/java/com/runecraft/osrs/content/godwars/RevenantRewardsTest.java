package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Jewellery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Revenant uniques and artefacts (wiki rates), revenant ether charges and the semi-precious enchants. */
class RevenantRewardsTest {
    @Test
    void uniqueTableMatchesTheWikiRates() {
        // Wiki "1 in N" unskulled / skulled / on task.
        assertEquals(29333.7, RevenantRewards.uniqueDenominator(7, false, false), 1);    // imp
        assertEquals(16133.54, RevenantRewards.uniqueDenominator(7, true, false), 1);
        assertEquals(19546.91, RevenantRewards.uniqueDenominator(15, false, false), 40); // goblin
        assertEquals(8373.44, RevenantRewards.uniqueDenominator(60, false, false), 20);  // hobgoblin (within 0.2%)
        assertEquals(1674.56, RevenantRewards.uniqueDenominator(60, false, true), 4);
        assertEquals(6506.75, RevenantRewards.uniqueDenominator(82, false, false), 15);   // cyclops = hellhound
        assertEquals(6506.75, RevenantRewards.uniqueDenominator(90, false, false), 15);
        assertEquals(5866.74, RevenantRewards.uniqueDenominator(120, false, false), 1);  // dark beast
        assertEquals(5333.4, RevenantRewards.uniqueDenominator(135, false, false), 1);   // dragon
        assertEquals(2933.37, RevenantRewards.uniqueDenominator(135, true, false), 1);
        assertEquals(7333.3, RevenantRewards.artefactDenominator("ancient_emblem", 7, false, false), 1);
        assertEquals(11000, RevenantRewards.artefactDenominator("ancient_totem", 7, false, false), 1);
        assertEquals(22000, RevenantRewards.artefactDenominator("ancient_statuette", 7, false, false), 1);
        assertEquals(44000, RevenantRewards.artefactDenominator("ancient_relic", 7, false, false), 1);
        // A unique roll: amulet of avarice on 0-1 out of 5, then each weapon.
        List<String> hit = RevenantRewards.roll(135, false, false, bound -> bound == 5 ? 0 : 0);
        assertEquals("amulet_of_avarice", hit.get(0));
        assertEquals("ancient_emblem", hit.get(1));
        assertEquals("viggoras_chainmace", RevenantRewards.roll(135, false, false, bound -> bound == 5 ? 4 : 0).get(0));
        assertTrue(RevenantRewards.roll(135, true, true, bound -> bound - 1).isEmpty());
        for (String w : RevenantRewards.WEAPONS) {
            assertNotNull(ItemCatalog.get(w), w);
        }
        assertNotNull(ItemCatalog.get("amulet_of_avarice"));
    }

    @Test
    void revenantTablesLeaveUniquesToTheRoll() {
        int revs = 0;
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.id.startsWith("revenant_")) {
                revs++;
                assertTrue(m.drops.stream().anyMatch(d -> d.item().equals("revenant_ether") && d.rarity() == 1), m.id);
                assertFalse(m.drops.stream().anyMatch(d -> d.item().startsWith("ancient_") || RevenantRewards.WEAPONS.contains(d.item())),
                        m.id);
            }
        }
        assertEquals(11, revs);
    }

    @Test
    void revenantWeaponsUseEther() {
        for (String w : List.of("craws_bow", "viggoras_chainmace", "thammarons_sceptre", "webweaver_bow", "ursine_chainmace",
                "accursed_sceptre")) {
            Charges.Spec s = Charges.of(w);
            assertNotNull(s, w);
            assertEquals(16_000, s.max());
            assertEquals(0, s.initial());
            assertEquals(Charges.Drain.ATTACK, s.drain());
            assertEquals(1, s.materials().get("revenant_ether"));
            assertTrue(Charges.depleted(s, 0));
        }
    }

    @Test
    void semiPreciousEnchants() {
        assertEquals("efaritays_aid", Jewellery.ENCHANT.get("topaz_ring"));
        assertEquals(3, Jewellery.ENCHANT_LEVEL.get("topaz_ring"));
        assertEquals("burning_amulet_5", Jewellery.ENCHANT.get("topaz_amulet"));
        assertEquals("dodgy_necklace", Jewellery.ENCHANT.get("opal_necklace"));
        assertEquals(1, Jewellery.ENCHANT_LEVEL.get("opal_necklace"));
        assertEquals("necklace_of_passage_5", Jewellery.ENCHANT.get("jade_necklace"));
        assertEquals("digsite_pendant_5", Jewellery.ENCHANT.get("ruby_necklace")); // was a dodgy necklace
        assertNotNull(ItemCatalog.get("topaz_ring"));
    }
}
