package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Waking Duke Sucellus (OSRS Wiki "Duke Sucellus/Strategies"). */
class DukeRulesTest {
    @Test
    void twoPoisonsWakeHimThreeAwakened() {
        assertEquals(2, DukeRules.poisonsToWake(false));
        assertEquals(3, DukeRules.poisonsToWake(true));
        assertTrue(2 * DukeRules.POISON_VALUE >= DukeRules.TOLERANCE);
        assertTrue(3 * DukeRules.POISON_VALUE >= DukeRules.TOLERANCE_AWAKENED);
        // 40 of each powder fed directly (5 each) fill even the awakened meter
        assertTrue(80 * DukeRules.POWDER_VALUE >= DukeRules.TOLERANCE_AWAKENED);
    }

    /** 70 Herblore: three mushrooms of each kind for two poisons; 80+: two; two salt harvests at 80 Mining make twelve. */
    @Test
    void ingredients() {
        int per = DukeRules.PER_POISON * 2;
        assertEquals(3, (per + DukeRules.powderPerMushroom(70) - 1) / DukeRules.powderPerMushroom(70));
        assertEquals(2, (per + DukeRules.powderPerMushroom(80) - 1) / DukeRules.powderPerMushroom(80));
        assertEquals(12, 2 * DukeRules.saltPerHarvest(80));
        assertEquals(2, DukeRules.poisons(12, 12, 13));
        assertEquals(1, DukeRules.poisons(12, 6, 12));
    }

    @Test
    void itemsAndAsylum() {
        for (String id : new String[]{DukeRules.ARDER_MUSHROOM, DukeRules.MUSCA_MUSHROOM, DukeRules.ARDER_POWDER, DukeRules.MUSCA_POWDER,
                DukeRules.SALAX_SALT, DukeRules.POISON}) {
            assertNotNull(ItemCatalog.get(id), id);
            assertTrue(Untradeables.untradeable(id), id);
        }
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.id().equals("ghorrock_prison")
                && p.blocks().containsAll(java.util.List.of(DukeRules.ARDER_MUSHROOMS, DukeRules.MUSCA_MUSHROOMS, DukeRules.SALT_PILE,
                DukeRules.VAT))));
    }
}
