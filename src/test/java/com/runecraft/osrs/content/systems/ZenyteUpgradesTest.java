package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Necklace of rupture and confliction gauntlets (wiki bonuses and Crafting levels). */
class ZenyteUpgradesTest {
    @Test
    void upgrades() {
        assertEquals(20, Jewellery.worn("necklace_of_rupture").stats().rangedAttack());
        assertEquals(8, Jewellery.worn("necklace_of_rupture").stats().rangedStrength());
        assertEquals(3, Jewellery.worn("necklace_of_rupture").stats().prayer());
        assertEquals(20, Jewellery.worn("confliction_gauntlets").stats().magicAttack());
        assertEquals(7, Jewellery.worn("confliction_gauntlets").stats().magicDamagePercent());
        Recipe rupture = Recipes.get("make_necklace_of_rupture");
        assertNotNull(rupture);
        assertEquals(84, rupture.level());
        assertEquals(1, rupture.inputs().get("elder_venator_fang"));
        Recipe gauntlets = Recipes.get("make_confliction_gauntlets");
        assertEquals(83, gauntlets.level());
        assertEquals(10000, gauntlets.inputs().get("demon_tear"));
        assertEquals(1, gauntlets.inputs().get("tormented_bracelet"));
    }
}
