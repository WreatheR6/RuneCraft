package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.ConsumeRules;
import com.runecraft.osrs.ItemCatalog;
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
 * OSRS's shared consumption delays ({@link ConsumeRules}): one eat delay for all food, one potion delay for all potions,
 * combo food on its own timer.
 *
 * <p>Round 41 replaced the per-item cooldowns (three or four different foods could be eaten within 0.2 s, which undermined
 * every tuned boss max hit). A consumption stamps its group in {@link PlayerData#consumeReadyAt} (refused while active, so
 * items picked up later are covered too) and puts the vanilla item cooldown on every inventory item of the group, so the
 * hotbar shows one shared sweep.
 *
 * <p>Round 42: eating also adds to a weapon delay that is still running, as in OSRS (wiki Food/Fast foods): 3 game ticks
 * for food, 2 for combo food, nothing for potions ({@link ConsumeRules#attackDelayMc}); the attack timer bar shows the new
 * wait. Round 41 had left this out (// playability); it is kept because it is a rate, not a tick-precise input: it costs
 * the attacks eating costs in OSRS, so boss max hits and food stay balanced against each other, and eating before the next
 * attack (with the weapon ready) costs nothing, as in OSRS. Raid and minigame supplies (ToA, CoX, ToB, NMZ, the Gauntlet,
 * blighted supplies, sq'irkjuice) go through {@link #consume(ServerPlayer, String)} too, instead of per-item cooldowns.
 */
public final class ConsumeCooldowns {
    public static final int FOOD_MC = ConsumeRules.FOOD_MC;
    public static final int POTION_MC = ConsumeRules.POTION_MC;

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
    public static ConsumeRules.Group groupOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ItemDef def = ModItems.def(stack);
        return ConsumeRules.groupOf(def != null ? def.id() : Inv.idOf(stack), def);
    }

    public static ConsumeRules.Group groupOf(String id, ItemDef def) {
        return ConsumeRules.groupOf(id, def);
    }

    /** True while the group's delay runs (combo food is held only by its own timer, not by the eat delay of other food). */
    public static boolean blocked(ServerPlayer player, ConsumeRules.Group group) {
        return group != null && ActionManager.mcTick() < Players.data(player).consumeReadyAt[group.ordinal()];
    }

    public static boolean blocked(ServerPlayer player, ItemStack stack) {
        return blocked(player, groupOf(stack));
    }

    /** Starts the delay for eating or drinking the item {@code id}; an unclassified item counts as food. */
    public static void consume(ServerPlayer player, String id) {
        ConsumeRules.Group group = ConsumeRules.groupOf(id, ItemCatalog.get(id));
        if (group == null) {
            group = ConsumeRules.Group.FOOD;
        }
        consume(player, group, ConsumeRules.delayMc(id, group));
        // OSRS: food adds to a weapon delay that is still running (3 ticks, 2 for combo food); potions don't.
        com.runecraft.combat.AttackTimer.addDelay(player, ConsumeRules.attackDelayMc(id, group));
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
    public static void consume(ServerPlayer player, ConsumeRules.Group group, int delayMc) {
        PlayerData data = Players.data(player);
        long now = ActionManager.mcTick();
        stamp(data, group, now + delayMc);
        if (group == ConsumeRules.Group.COMBO) {
            stamp(data, ConsumeRules.Group.FOOD, now + ConsumeRules.FOOD_MC);
        }
        Set<Item> done = new HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            ConsumeRules.Group g = groupOf(s);
            if (g == null || !done.add(s.getItem())) {
                continue;
            }
            long left = data.consumeReadyAt[g.ordinal()] - now;
            if (left > 0 && (g == group || group == ConsumeRules.Group.COMBO && g == ConsumeRules.Group.FOOD)) {
                player.getCooldowns().addCooldown(s.getItem(), (int) left);
            }
        }
    }

    private static void stamp(PlayerData data, ConsumeRules.Group group, long readyAt) {
        data.consumeReadyAt[group.ordinal()] = Math.max(data.consumeReadyAt[group.ordinal()], readyAt);
    }
}
