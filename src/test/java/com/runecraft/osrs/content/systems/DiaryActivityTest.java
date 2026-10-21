package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Activity (EVENT), WEAR and one-go diary tasks, 3-dose Herblore output and non-drinkable potions. */
class DiaryActivityTest {
    @Test
    void activityKeysAndWornSets() {
        assertTrue(Diaries.eventMatches("minigame_start:fishing_trawler", "minigame_start:fishing_trawler"));
        assertFalse(Diaries.eventMatches("minigame_start:fishing_trawler", "minigame_win:fishing_trawler"));
        assertTrue(Diaries.eventMatches("a|eat:cooked_oomlie_wrap", "eat:cooked_oomlie_wrap"));
        assertEquals("xerics_heart", Diaries.slug("Xeric's Heart"));
        String voidSet = "void_knight_top|elite_void_top&void_knight_robe|elite_void_robe&void_knight_gloves"
                + "&void_melee_helm|void_ranger_helm|void_mage_helm";
        assertTrue(Diaries.wearingAll(voidSet, List.of("elite_void_top", "void_knight_robe", "void_knight_gloves", "void_mage_helm")));
        assertFalse(Diaries.wearingAll(voidSet, List.of("void_knight_top", "void_knight_robe", "void_knight_gloves")));
    }

    @Test
    void convertedTasksAreFaithful() {
        int events = 0;
        for (Diaries.Task t : Diaries.tasks()) {
            if (t.type() == Diaries.Type.EVENT || t.type() == Diaries.Type.WEAR) {
                events++;
                // Round 40: praying at Taverley's Altar of Guthix is real, but full Initiate armour can't be worn (only the sallet
                // exists), so that task stays marked approx.
                if (!t.id().equals("falador_medium_8")) {
                    assertFalse(Diaries.isApprox(t), t.id() + " uses a real trigger");
                }
            }
        }
        assertTrue(events >= 10, "activity/wear tasks: " + events);
        Diaries.Task mind = Diaries.tasks().stream().filter(t -> t.description().startsWith("Craft 140 Mind runes")).findFirst().orElseThrow();
        assertTrue(Diaries.inOneGo(mind));
        assertEquals(140, mind.count());
    }

    @Test
    void herbPotionsComeAsThreeDoses() {
        ContentRegistry.monsters();
        Recipe brew = null;
        Recipe superCombat = null;
        for (Recipe r : Recipes.all()) {
            if (r.id().equals("mix_saradomin_brew")) {
                brew = r;
            }
            if (r.menu() == MenuType.HERBLORE && r.outputs().containsKey("super_combat_potion")) {
                superCombat = r;
            }
        }
        assertNotNull(brew);
        assertEquals(3, brew.outputDoses(), "Saradomin brew(3), as in OSRS");
        if (superCombat != null) {
            assertEquals(0, superCombat.outputDoses(), "made from finished potions: keeps full doses");
        }
    }

    @Test
    void usedOnThingsNotDrunk() {
        ContentRegistry.monsters();
        for (String id : new String[]{"guthix_balance", "olive_oil", "sacred_oil"}) {
            assertNotNull(ItemCatalog.get(id), id);
            assertFalse(ItemCatalog.get(id).drinkable(), id);
        }
        assertTrue(ItemCatalog.get("saradomin_brew").drinkable());
    }
}
