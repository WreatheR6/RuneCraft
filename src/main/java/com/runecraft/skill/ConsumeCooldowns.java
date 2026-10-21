package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.ItemDef;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * OSRS's shared consumption delays. Eating any food starts one 3-tick eat delay, drinking any (non-barbarian) potion one
 * 3-tick potion delay; the two are separate, so a potion can follow food at once. Combo foods (cooked karambwan, halibut,
 * gnome cooking dishes) ignore the eat delay of other food but start it themselves, and can't follow each other (OSRS Wiki,
 * Food/Fast foods: "food > brew > karambwan" in one tick). Barbarian mixes and drinks (ales, kegs, cocktails) share the
 * food timer.
 *
 * <p>Round 41 replaced the per-item cooldowns (three or four different foods could be eaten within 0.2 s, which undermined
 * every tuned boss max hit). A consumption stamps its group in {@link PlayerData#consumeReadyAt} (refused while active, so
 * items picked up later are covered too) and puts the vanilla item cooldown on every inventory item of the group, so the
 * hotbar shows one shared sweep.
 *
 * <p>// playability: OSRS food also delays the next attack by 3 ticks (2 for combo food); RuneCraft's eating does not
 * touch the attack timer.
 */
public final class ConsumeCooldowns {
    /** Consumption groups; the ordinal indexes {@link PlayerData#consumeReadyAt}. Append new values last. */
    public enum Group { FOOD, POTION, COMBO }

    /** OSRS eat delay: 3 game ticks. */
    public static final int FOOD_MC = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;
    /** OSRS potion delay: 3 game ticks, separate from the eat delay. */
    public static final int POTION_MC = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;
    /** Combo food: can't be eaten twice in a row (its own 3-tick timer) and starts the normal eat delay. */
    public static final int COMBO_MC = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;

    /** OSRS combo foods (Food/Fast foods): karambwan, halibut and the gnome cooking dishes. */
    static final Set<String> COMBO_FOODS = Set.of("cooked_karambwan", "blighted_karambwan", "halibut",
            "toad_crunchies", "spicy_crunchies", "worm_crunchies", "chocchip_crunchies",
            "fruit_batta", "toad_batta", "worm_batta", "vegetable_batta", "cheese_and_tomato_batta",
            "worm_hole", "veg_ball", "tangled_toads_legs", "chocolate_bomb");

    /** OSRS members' pies (1-tick eat delay for both halves); the F2P pies' second half takes 2 ticks. */
    private static final Set<String> MEMBERS_PIES = Set.of("garden", "fish", "mushroom", "admiral", "dragonfruit", "wild",
            "summer");

    private ConsumeCooldowns() {
    }

    /** Registers the refusal for every right-click route (registered before the modules' item callbacks). */
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            return blocked(sp, stack) ? InteractionResultHolder.fail(stack) : InteractionResultHolder.pass(stack);
        });
    }

    /** The consumption group of a stack, or null when it isn't eaten or drunk. */
    public static Group groupOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ItemDef def = ModItems.def(stack);
        return groupOf(def != null ? def.id() : Inv.idOf(stack), def);
    }

    /** The consumption group of an item id (with its catalog entry, nullable), or null. */
    public static Group groupOf(String id, ItemDef def) {
        if (id == null) {
            return null;
        }
        if (COMBO_FOODS.contains(id)) {
            return Group.COMBO;
        }
        ItemDef.Kind kind = def == null ? null : def.kind();
        if (id.endsWith("_mix") && kind != ItemDef.Kind.SIMPLE) {
            return Group.FOOD; // barbarian mixes use the food timer (wiki, Food/Fast foods)
        }
        if (kind == ItemDef.Kind.FOOD) {
            return Group.FOOD;
        }
        if (kind == ItemDef.Kind.POTION) {
            if (id.endsWith("_keg")) {
                return Group.FOOD; // a keg of ale is drunk like the pint it holds
            }
            return def.drinkable() ? Group.POTION : null;
        }
        return null;
    }

    private static ItemDef def(String id) {
        Item item = ModItems.get(id);
        return item == null ? null : ModItems.def(item);
    }

    /** True while the group's delay runs (combo food is held only by its own timer, not by the eat delay of other food). */
    public static boolean blocked(ServerPlayer player, Group group) {
        return group != null && ActionManager.mcTick() < Players.data(player).consumeReadyAt[group.ordinal()];
    }

    public static boolean blocked(ServerPlayer player, ItemStack stack) {
        return blocked(player, groupOf(stack));
    }

    /** Minecraft ticks left on a group's delay (0 when free). */
    public static long remaining(ServerPlayer player, Group group) {
        return Math.max(0, Players.data(player).consumeReadyAt[group.ordinal()] - ActionManager.mcTick());
    }

    /**
     * The eat delay of one item in Minecraft ticks: 3 game ticks for most food; pies and pizzas are faster (wiki,
     * Food/Fast foods: a full pie or pizza 1 tick, a pizza half or an F2P pie half 2 ticks, members' pie halves 1 tick).
     */
    public static int eatDelayMc(String id) {
        int tick = ActionManager.MC_TICKS_PER_GAME_TICK;
        if (id.endsWith("_pizza")) {
            return id.startsWith("half_") ? 2 * tick : tick;
        }
        if (id.endsWith("_pie")) {
            if (!id.startsWith("half_a_")) {
                return tick;
            }
            String pie = id.substring("half_a_".length(), id.length() - "_pie".length());
            return MEMBERS_PIES.contains(pie) ? tick : 2 * tick;
        }
        return FOOD_MC;
    }

    /** Starts the delay for eating or drinking the item {@code id} (classified as {@link #groupOf}); food by default. */
    public static void consume(ServerPlayer player, String id) {
        Group group = groupOf(id, def(id));
        consume(player, group == null ? Group.FOOD : group, group == Group.POTION ? POTION_MC
                : group == Group.COMBO ? COMBO_MC : eatDelayMc(id));
    }

    /**
     * A dose used from a potion stack: drinking it starts the shared delay; a dose used on something else (weapon poison,
     * compost) only holds that item for 3 ticks, as before.
     */
    public static void consume(ServerPlayer player, ItemStack stack, boolean drank) {
        if (drank) {
            consume(player, Inv.idOf(stack));
        } else {
            player.getCooldowns().addCooldown(stack.getItem(), POTION_MC);
        }
    }

    /**
     * Starts a group's delay: combo food also starts the eat delay (food can't follow a karambwan). Every inventory item of
     * a delayed group gets the vanilla cooldown for the time left, so the hotbar sweeps together.
     */
    public static void consume(ServerPlayer player, Group group, int delayMc) {
        PlayerData data = Players.data(player);
        long now = ActionManager.mcTick();
        stamp(data, group, now + delayMc);
        if (group == Group.COMBO) {
            stamp(data, Group.FOOD, now + FOOD_MC);
        }
        Set<Item> done = new HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            Group g = groupOf(s);
            if (g == null || !done.add(s.getItem())) {
                continue;
            }
            long left = data.consumeReadyAt[g.ordinal()] - now;
            if (left > 0 && (g == group || group == Group.COMBO && g == Group.FOOD)) {
                player.getCooldowns().addCooldown(s.getItem(), (int) left);
            }
        }
    }

    private static void stamp(PlayerData data, Group group, long readyAt) {
        data.consumeReadyAt[group.ordinal()] = Math.max(data.consumeReadyAt[group.ordinal()], readyAt);
    }
}
