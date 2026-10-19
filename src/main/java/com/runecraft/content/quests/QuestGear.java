package com.runecraft.content.quests;

import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Accessories worn in virtual slots (hands, cape, neck) that the base equipment system lacks:
 * right-click to wear, {@code /questgear} to list or remove. Their bonuses are added to the gear totals
 * {@link com.runecraft.combat.HitCalcBridge} feeds the attack and defence calculations (round 15; before, attack and
 * strength multiplied the roll separately and defence was ignored). Also enforces quest-locked weapons (dragon longsword,
 * dragon scimitar...): without the quest they deal no damage, as OSRS forbids wielding them.
 */
final class QuestGear {
    private static final Map<UUID, Long> LAST_WARNING = new HashMap<>();

    private QuestGear() {
    }

    static void init() {
        QuestBook book = QuestBook.get();
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            QuestBook.Gear gear = book.gear.get(Inv.idOf(stack));
            if (gear == null) {
                return InteractionResultHolder.pass(stack);
            }
            if (player instanceof ServerPlayer sp) {
                wear(sp, gear, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        });
        // Quest-locked armour and shields (rune platebody, green d'hide body: Dragon Slayer I) can't be worn.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    enforceWornGates(p);
                }
            }
        });
        // The worn quest gear is part of the gear totals PlayerHitCalc and the defence roll use (no separate multiplier).
        com.runecraft.combat.HitCalcBridge.extraWornStats = QuestGear::bonus;
        com.runecraft.combat.HitCalcBridge.extraWornIds = p -> QuestsState.get(p).wornAll(p.getUUID()).values();
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                return locked(player) ? 0.0 : 1.0;
            }

            @Override
            public double maxHit(ServerPlayer player, LivingEntity target, AttackType type) {
                return locked(player) ? 0.0 : 1.0;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0 && !book.armourGates.isEmpty()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    enforceArmourGates(player, book);
                }
            }
        });
    }

    private static final EquipmentSlot[] GATED_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

    /** Takes off quest-locked armour (rune platebody without Dragon Slayer I...), as OSRS refuses to equip it. */
    private static void enforceArmourGates(ServerPlayer player, QuestBook book) {
        for (EquipmentSlot slot : GATED_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String quest = book.armourGates.get(Inv.idOf(stack));
            if (quest == null) {
                continue;
            }
            QuestInfo q = ContentRegistry.quest(quest);
            if (q == null || Players.data(player).questComplete(q)) {
                continue;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            Inv.give(player, stack);
            Msg.red(player, "You need to complete " + q.displayName() + " to wear that.");
        }
    }

    static void wear(ServerPlayer player, QuestBook.Gear gear, ItemStack stack) {
        if (gear.requiredQuest() != null) {
            QuestInfo q = ContentRegistry.quest(gear.requiredQuest());
            if (q != null && !Players.data(player).questComplete(q)) {
                Msg.game(player, "You need to complete " + q.displayName() + " to wear that.");
                return;
            }
        }
        String slot = gear.slot().name().toLowerCase();
        String old = QuestsState.get(player).wear(player.getUUID(), slot, gear.item());
        stack.shrink(1);
        if (old != null) {
            Inv.give(player, old, 1);
        }
        Msg.game(player, "You wear the " + QuestInterpreter.describe(gear.item(), 1).replaceFirst("^an? ", "") + " (" + slot + " slot).");
        com.runecraft.network.ServerNetworking.sync(player);
    }

    static void remove(ServerPlayer player, String slot) {
        String old = QuestsState.get(player).wear(player.getUUID(), slot, null);
        if (old == null) {
            Msg.game(player, "You aren't wearing anything in your " + slot + " slot.");
            return;
        }
        Inv.give(player, old, 1);
        Msg.game(player, "You remove your " + QuestInterpreter.describe(old, 1).replaceFirst("^an? ", "") + ".");
        com.runecraft.network.ServerNetworking.sync(player);
    }

    /** Total bonuses of the accessories a player wears. */
    static EquipmentStats bonus(ServerPlayer player) {
        EquipmentStats total = EquipmentStats.NONE;
        Map<String, QuestBook.Gear> gear = QuestBook.get().gear;
        for (String item : QuestsState.get(player).wornAll(player.getUUID()).values()) {
            QuestBook.Gear g = gear.get(item);
            if (g != null) {
                total = total.plus(g.stats());
            }
        }
        return total;
    }

    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND};

    /** Takes off any worn armour or shield whose quest the player hasn't completed. */
    static void enforceWornGates(ServerPlayer player) {
        Map<String, String> gates = QuestBook.get().weaponGates;
        for (EquipmentSlot slot : WORN) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            String quest = gates.get(Inv.idOf(worn));
            QuestInfo q = quest == null ? null : ContentRegistry.quest(quest);
            if (q == null || Players.data(player).questComplete(q)) {
                continue;
            }
            ItemStack copy = worn.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            Msg.red(player, "You need to complete " + q.displayName() + " to wear that.");
        }
    }

    /** True when the wielded weapon needs a quest the player hasn't completed. */
    static boolean locked(ServerPlayer player) {
        String weapon = Inv.idOf(player.getMainHandItem());
        String quest = QuestBook.get().weaponGates.get(weapon);
        if (quest == null) {
            return false;
        }
        QuestInfo q = ContentRegistry.quest(quest);
        if (q == null || Players.data(player).questComplete(q)) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now - LAST_WARNING.getOrDefault(player.getUUID(), -1000L) > 100) {
            LAST_WARNING.put(player.getUUID(), now);
            Msg.red(player, "You need to complete " + q.displayName() + " to wield that weapon.");
        }
        return true;
    }
}
