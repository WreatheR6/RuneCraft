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

    @Test
    void raidAndMinigameSuppliesShareTheTimers() {
        // Round 42: these used per-item cooldowns. Potions on the potion timer, food on the food timer (wiki: ToB bandages "act
        // as a food item ... can be combo eaten with potions and/or cooked karambwan").
        for (String id : com.runecraft.osrs.content.raids.ToaSupplies.ITEMS) {
            ConsumeRules.Group want = id.equals(com.runecraft.osrs.content.raids.ToaSupplies.HONEY_LOCUST)
                    ? ConsumeRules.Group.FOOD : ConsumeRules.Group.POTION;
            assertEquals(want, group(id), id);
        }
        for (com.runecraft.osrs.content.raids.CoxSupplies.Potion p : com.runecraft.osrs.content.raids.CoxSupplies.Potion.values()) {
            for (com.runecraft.osrs.content.raids.CoxSupplies.Strength s : com.runecraft.osrs.content.raids.CoxSupplies.Strength.values()) {
                assertEquals(ConsumeRules.Group.POTION, group(p.id(s)), p.id(s));
            }
        }
        for (com.runecraft.osrs.content.raids.CoxSupplies.Food f : com.runecraft.osrs.content.raids.CoxSupplies.FOODS) {
            assertEquals(ConsumeRules.Group.FOOD, group(f.cooked()), f.cooked());
        }
        assertEquals(ConsumeRules.Group.FOOD, group(com.runecraft.osrs.content.raids.TobRules.BANDAGES));
        for (String id : com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.POTIONS) {
            assertEquals(ConsumeRules.Group.POTION, group(id), id);
        }
        assertEquals(ConsumeRules.Group.POTION, group(com.runecraft.osrs.content.minigames.Gauntlet.EGNIOL));
        assertEquals(ConsumeRules.Group.FOOD, group(com.runecraft.osrs.content.minigames.Gauntlet.PADDLEFISH));
        assertEquals(ConsumeRules.Group.POTION, group("blighted_super_restore"));
        assertEquals(ConsumeRules.Group.COMBO, group("blighted_karambwan"));
        assertEquals(ConsumeRules.Group.FOOD, group("castle_wars_bandage"));
        for (String season : new String[]{"winter", "spring", "autumn", "summer"}) {
            assertEquals(ConsumeRules.Group.FOOD, group(season + "_sqirkjuice"), season);
        }
    }

    @Test
    void upgradedPaddlefishAreComboFood() {
        // Wiki Crystal/Corrupted paddlefish: "Like cooked karambwan, they can be eaten instantly with no delay after eating a
        // paddlefish."
        assertEquals(ConsumeRules.Group.COMBO, group("crystal_paddlefish"));
        assertEquals(ConsumeRules.Group.COMBO, group("corrupted_paddlefish"));
        assertEquals(ConsumeRules.Group.FOOD, group("paddlefish"));
    }

    @Test
    void eatingDelaysTheNextAttackPotionsDoNot() {
        // Wiki Food/Fast foods: standard food adds 3 ticks to a running weapon delay, combo food 2, potions nothing; a pie bite
        // keeps the 3-tick attack delay whatever its eat delay.
        assertEquals(36, ConsumeRules.attackDelayMc("shark", group("shark")));
        assertEquals(36, ConsumeRules.attackDelayMc("summer_pie", group("summer_pie")));
        assertEquals(36, ConsumeRules.attackDelayMc("anchovy_pizza", group("anchovy_pizza")));
        assertEquals(24, ConsumeRules.attackDelayMc("cooked_karambwan", group("cooked_karambwan")));
        assertEquals(24, ConsumeRules.attackDelayMc("halibut", group("halibut")));
        assertEquals(0, ConsumeRules.attackDelayMc("saradomin_brew", group("saradomin_brew")));
        assertEquals(0, ConsumeRules.attackDelayMc("nectar", group("nectar")));
        assertEquals(36, ConsumeRules.attackDelayMc(com.runecraft.osrs.content.raids.TobRules.BANDAGES,
                group(com.runecraft.osrs.content.raids.TobRules.BANDAGES)));
        assertEquals(0, ConsumeRules.attackDelayMc("coins", null));
        // Shark then karambwan in one tick: 3 + 2 = 5 ticks (wiki: "combo eating stacks the attack delay").
        assertEquals(60, ConsumeRules.attackDelayMc("shark", ConsumeRules.Group.FOOD)
                + ConsumeRules.attackDelayMc("cooked_karambwan", ConsumeRules.Group.COMBO));
    }
}
