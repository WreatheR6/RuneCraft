package com.runecraft.osrs;

import com.runecraft.osrs.content.skilling.BrewingData;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.HerbloreData;
import com.runecraft.osrs.data.Potion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared OSRS eat/potion/combo delays (round 41): every eaten or drunk item lands on a timer, and on the right one. */
class ConsumeRulesTest {
    private static ConsumeRules.Group group(String id) {
        return ConsumeRules.groupOf(id, ItemCatalog.get(id));
    }

    @Test
    void delaysAreThreeGameTicks() {
        assertEquals(36, ConsumeRules.FOOD_MC);
        assertEquals(36, ConsumeRules.POTION_MC);
        assertEquals(36, ConsumeRules.COMBO_MC);
    }

    @Test
    void standardFoodComboFoodAndPotions() {
        assertEquals(ConsumeRules.Group.FOOD, group("shark"));
        assertEquals(ConsumeRules.Group.FOOD, group("manta_ray"));
        assertEquals(ConsumeRules.Group.COMBO, group("cooked_karambwan"));
        assertEquals(ConsumeRules.Group.COMBO, group("halibut"));
        assertEquals(ConsumeRules.Group.COMBO, group("chocolate_bomb"));
        assertEquals(ConsumeRules.Group.POTION, group("prayer_potion"));
        assertEquals(ConsumeRules.Group.POTION, group("saradomin_brew"));
        // Barbarian mixes use the food timer (wiki: "non-barbarian potions respect a different delay timer").
        assertEquals(ConsumeRules.Group.FOOD, group("attack_mix"));
        assertEquals(ConsumeRules.Group.FOOD, group("stamina_mix"));
        // Not consumables.
        assertNull(group("coins"));
        assertNull(group("bronze_sword"));
        assertNull(ConsumeRules.groupOf(null, null));
    }

    @Test
    void everyComboFoodExists() {
        for (String id : ConsumeRules.COMBO_FOODS) {
            ItemDef def = ItemCatalog.get(id);
            assertNotNull(def, id);
            assertEquals(ItemDef.Kind.FOOD, def.kind(), id);
        }
    }

    @Test
    void everyHandledConsumableHasATimer() {
        List<String> food = new ArrayList<>();
        CookingData.BITES.forEach(b -> food.add(b.full()));
        BrewingData.DRINKS.forEach(d -> food.add(d.id()));
        BrewingData.BREWS.forEach(b -> {
            food.add(BrewingData.kegOf(b.id()));
            food.add(BrewingData.kegOf(b.mature()));
        });
        HerbloreData.MIXES.forEach(m -> food.add(m.id()));
        List<String> wrong = new ArrayList<>();
        for (String id : food) {
            ItemDef def = ItemCatalog.get(id);
            if (def != null && group(id) != ConsumeRules.Group.FOOD) {
                wrong.add(id + " (" + def.kind() + " -> " + group(id) + ")");
            }
        }
        assertEquals(List.of(), wrong, "should share the food timer");
        for (Potion p : Potion.values()) {
            assertEquals(ConsumeRules.Group.POTION, group(p.itemId()), p.itemId());
        }
        int potions = 0;
        for (HerbloreData.ModPotion p : HerbloreData.POTIONS) {
            ItemDef def = ItemCatalog.get(p.id());
            if (def == null) {
                continue;
            }
            ConsumeRules.Group g = group(p.id());
            assertTrue(g == ConsumeRules.Group.POTION || g == null && !def.drinkable(), p.id() + " -> " + g);
            potions++;
        }
        assertTrue(potions > 10);
    }

    @Test
    void piesAndPizzasAreFast() {
        // Wiki Food/Fast foods: a whole pie or pizza 1 tick; pizza halves and F2P pie halves 2; members' pie halves 1.
        assertEquals(12, ConsumeRules.eatDelayMc("summer_pie"));
        assertEquals(12, ConsumeRules.eatDelayMc("half_a_summer_pie"));
        assertEquals(12, ConsumeRules.eatDelayMc("apple_pie"));
        assertEquals(24, ConsumeRules.eatDelayMc("half_a_apple_pie"));
        assertEquals(24, ConsumeRules.eatDelayMc("half_a_redberry_pie"));
        assertEquals(12, ConsumeRules.eatDelayMc("anchovy_pizza"));
        assertEquals(24, ConsumeRules.eatDelayMc("half_anchovy_pizza"));
        assertEquals(36, ConsumeRules.eatDelayMc("shark"));
        assertEquals(36, ConsumeRules.delayMc("shark", ConsumeRules.Group.FOOD));
        assertEquals(36, ConsumeRules.delayMc("cooked_karambwan", ConsumeRules.Group.COMBO));
        assertEquals(36, ConsumeRules.delayMc("prayer_potion", ConsumeRules.Group.POTION));
        for (CookingData.Bite b : CookingData.BITES) {
            assertTrue(ConsumeRules.eatDelayMc(b.full()) > 0, b.full());
        }
    }
}
