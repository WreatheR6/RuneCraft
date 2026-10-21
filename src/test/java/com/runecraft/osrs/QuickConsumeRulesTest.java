package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuickConsumeRulesTest {
    private static final List<QuickConsumeRules.Food> FOODS = List.of(
            new QuickConsumeRules.Food(0, "shark", 20, false),
            new QuickConsumeRules.Food(1, "lobster", 12, false),
            new QuickConsumeRules.Food(2, "cooked_karambwan", 18, true),
            new QuickConsumeRules.Food(3, "trout", 7, false));

    @Test
    void eatsTheBiggestHealThatFitsElseTheLeastWaste() {
        assertEquals(-1, QuickConsumeRules.chooseFood(FOODS, 0, true, true), "full health eats nothing");
        assertEquals(1, QuickConsumeRules.chooseFood(FOODS, 15, true, true), "15 missing: the lobster (12), not the shark");
        assertEquals(0, QuickConsumeRules.chooseFood(FOODS, 40, true, true), "40 missing: the shark");
        assertEquals(3, QuickConsumeRules.chooseFood(FOODS, 5, true, true), "5 missing: the trout wastes least");
        // the eat delay running: combo food still goes down (OSRS karambwan combo eating)
        assertEquals(2, QuickConsumeRules.chooseFood(FOODS, 40, false, true));
        assertEquals(-1, QuickConsumeRules.chooseFood(FOODS, 40, false, false));
        // only combo food left
        assertEquals(0, QuickConsumeRules.chooseFood(List.of(new QuickConsumeRules.Food(5, "cooked_karambwan", 18, true)), 30,
                true, true));
    }

    private static QuickConsumeRules.State state(int hp, int prayer, boolean poisoned, boolean venom, int style, boolean boosted) {
        return new QuickConsumeRules.State(hp, 99, prayer, 99, poisoned, venom, style, boosted, boosted, boosted);
    }

    @Test
    void drinksByNeed() {
        List<QuickConsumeRules.Potion> pack = List.of(
                new QuickConsumeRules.Potion(0, "super_combat_potion", 4),
                new QuickConsumeRules.Potion(1, "prayer_potion", 4),
                new QuickConsumeRules.Potion(2, "prayer_potion", 2),
                new QuickConsumeRules.Potion(3, "saradomin_brew", 4),
                new QuickConsumeRules.Potion(4, "antipoison", 3),
                new QuickConsumeRules.Potion(5, "anti_venom", 4),
                new QuickConsumeRules.Potion(6, "ranging_potion", 4));
        assertEquals(5, QuickConsumeRules.choosePotion(pack, state(99, 99, false, true, 0, true)), "envenomed: anti-venom");
        assertEquals(4, QuickConsumeRules.choosePotion(pack, state(99, 99, true, false, 0, true)), "poisoned: the 3-dose antipoison");
        assertEquals(3, QuickConsumeRules.choosePotion(pack, state(40, 10, false, false, 0, true)), "below half health: the brew first");
        assertEquals(2, QuickConsumeRules.choosePotion(pack, state(80, 10, false, false, 0, true)),
                "prayer low: the prayer potion with fewer doses left");
        assertEquals(-1, QuickConsumeRules.choosePotion(pack, state(99, 99 - 20, false, false, 0, true)),
                "20 prayer missing at level 99 is less than a dose (7 + 24 = 31): no waste");
        assertEquals(3, QuickConsumeRules.choosePotion(pack, state(70, 99, false, false, 0, true)), "a brew's 16 fits 29 missing");
        assertEquals(0, QuickConsumeRules.choosePotion(pack, state(99, 99, false, false, 0, false)), "melee, unboosted: super combat");
        assertEquals(6, QuickConsumeRules.choosePotion(pack, state(99, 99, false, false, 1, false)), "ranged, unboosted: ranging");
        assertEquals(-1, QuickConsumeRules.choosePotion(pack, state(99, 99, false, false, 2, false)), "magic: no magic potion");
        assertEquals(-1, QuickConsumeRules.choosePotion(pack, state(99, 99, false, false, 0, true)), "nothing needed");
        List<QuickConsumeRules.Potion> brews = List.of(new QuickConsumeRules.Potion(0, "ancient_brew", 1),
                new QuickConsumeRules.Potion(1, "prayer_potion", 4));
        assertEquals(1, QuickConsumeRules.choosePotion(brews, state(99, 10, false, false, 0, true)),
                "a plain prayer potion before an ancient brew (which drains melee stats)");
        assertEquals(0, QuickConsumeRules.choosePotion(brews.subList(0, 1), state(99, 10, false, false, 0, true)));
    }

    @Test
    void wikiDoseAmounts() {
        assertEquals(31, QuickConsumeRules.prayerRestore(99)); // 7 + floor(99/4)
        assertEquals(16, QuickConsumeRules.brewHeal(99)); // 2 + floor(99 * 0.15)
        assertEquals(QuickConsumeRules.Need.CURE, QuickConsumeRules.need("sanfew_serum"));
        assertEquals(true, QuickConsumeRules.restoresPrayer("sanfew_serum"));
        assertEquals(QuickConsumeRules.Need.OTHER, QuickConsumeRules.need("stamina_potion"));
    }
}
