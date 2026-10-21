package com.runecraft.osrs;

import java.util.Set;

/**
 * Which OSRS consumption timer an item uses, and its eat delay (OSRS Wiki, Food/Fast foods). Most food starts a 3-tick
 * eat delay and every (non-barbarian) potion a separate 3-tick potion delay, so a potion can follow food in the same tick.
 * Combo foods (cooked karambwan, halibut, the gnome cooking dishes) ignore the eat delay of other food but start it
 * themselves and can't follow each other ("food, brew, karambwan" in one tick). Barbarian mixes and drinks (ales, kegs,
 * cocktails, wine) share the food timer. Pies and pizzas have shorter eat delays.
 */
public final class ConsumeRules {
    /** Consumption timers; the ordinal indexes per-player state (append new values last). */
    public enum Group { FOOD, POTION, COMBO }

    /** One OSRS game tick in Minecraft ticks. */
    private static final int TICK_MC = 12;
    /** Eat delay of standard food: 3 game ticks. */
    public static final int FOOD_MC = 3 * TICK_MC;
    /** Potion delay: 3 game ticks, separate from the eat delay. */
    public static final int POTION_MC = 3 * TICK_MC;
    /** Combo food's own timer (two can't be eaten in a row): 3 game ticks. */
    public static final int COMBO_MC = 3 * TICK_MC;

    /** OSRS combo foods (Food/Fast foods): karambwan, halibut and the gnome cooking dishes. */
    public static final Set<String> COMBO_FOODS = Set.of("cooked_karambwan", "blighted_karambwan", "halibut",
            "toad_crunchies", "spicy_crunchies", "worm_crunchies", "chocchip_crunchies",
            "fruit_batta", "toad_batta", "worm_batta", "vegetable_batta", "cheese_and_tomato_batta",
            "worm_hole", "veg_ball", "tangled_toads_legs", "chocolate_bomb");

    /** Members' pies: 1-tick eat delay for both halves (an F2P pie's second half takes 2 ticks). */
    private static final Set<String> MEMBERS_PIES = Set.of("garden", "fish", "mushroom", "admiral", "dragonfruit", "wild",
            "summer");

    private ConsumeRules() {
    }

    /** The timer an item uses (its catalog entry may be null), or null when it isn't eaten or drunk. */
    public static Group groupOf(String id, ItemDef def) {
        if (id == null) {
            return null;
        }
        if (COMBO_FOODS.contains(id)) {
            return Group.COMBO;
        }
        // Barbarian mixes and drinks (ales, cocktails, the Wizard's mind bomb) use the food timer, whatever their kind.
        if (com.runecraft.osrs.content.skilling.HerbloreData.mix(id) != null
                || com.runecraft.osrs.content.skilling.BrewingData.drink(id) != null) {
            return Group.FOOD;
        }
        ItemDef.Kind kind = def == null ? null : def.kind();
        if (kind == ItemDef.Kind.FOOD) {
            return Group.FOOD;
        }
        if (kind == ItemDef.Kind.POTION) {
            if (id.endsWith("_mix") || id.endsWith("_keg")) {
                return Group.FOOD; // barbarian mixes and kegs of ale use the food timer
            }
            return def.drinkable() ? Group.POTION : null;
        }
        return null;
    }

    /**
     * The eat delay of one item in Minecraft ticks: 3 game ticks for most food; a full pie or pizza 1 tick, a pizza half
     * or an F2P pie's second half 2 ticks, a members' pie's second half 1 tick.
     */
    public static int eatDelayMc(String id) {
        if (id.endsWith("_pizza")) {
            return id.startsWith("half_") ? 2 * TICK_MC : TICK_MC;
        }
        if (id.endsWith("_pie")) {
            if (!id.startsWith("half_a_")) {
                return TICK_MC;
            }
            String pie = id.substring("half_a_".length(), id.length() - "_pie".length());
            return MEMBERS_PIES.contains(pie) ? TICK_MC : 2 * TICK_MC;
        }
        return FOOD_MC;
    }

    /** The delay one consumption of {@code id} starts on its own timer. */
    public static int delayMc(String id, Group group) {
        if (group == Group.POTION) {
            return POTION_MC;
        }
        return group == Group.COMBO ? COMBO_MC : eatDelayMc(id);
    }
}
