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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypedActionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Accessories worn in virtual slots (hands, cape, neck) that the base equipment system lacks:
 * right-click to wear, {@code /questgear} to list or remove. Their attack and strength bonuses are
 * applied through {@link CombatHooks} as the ratio they add to the player's equipment bonus
 * (accuracy roll and max hit both scale with bonus + 64). Defence bonuses are recorded but not yet
 * applied (CombatHooks has no defence hook). Also enforces quest-locked weapons (dragon longsword,
 * dragon scimitar...): without the quest they deal no damage, as OSRS forbids wielding them.
 */
final class QuestGear {
    private static final Map<UUID, Long> LAST_WARNING = new HashMap<>();

    private QuestGear() {
    }

    static void init() {
        QuestBook book = QuestBook.get();
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            QuestBook.Gear gear = book.gear.get(Inv.idOf(stack));
            if (gear == null) {
                return TypedActionResult.pass(stack);
            }
            if (player instanceof ServerPlayerEntity sp) {
                wear(sp, gear, stack);
            }
            return TypedActionResult.success(stack, world.isClient());
        });
        // Quest-locked armour and shields (rune platebody, green d'hide body: Dragon Slayer I) can't be worn.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    enforceWornGates(p);
                }
            }
        });
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                if (locked(player)) {
                    return 0.0;
                }
                int extra = bonus(player).attack(type);
                if (extra == 0) {
                    return 1.0;
                }
                int base = Equipment.total(player, Players.data(player).skills()).attack(type);
                return Math.max(0.1, (base + extra + 64.0) / Math.max(1.0, base + 64.0));
            }

            @Override
            public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                if (locked(player)) {
                    return 0.0;
                }
                EquipmentStats extra = bonus(player);
                EquipmentStats base = Equipment.total(player, Players.data(player).skills());
                if (type.isMelee() && extra.meleeStrength() != 0) {
                    return (base.meleeStrength() + extra.meleeStrength() + 64.0) / Math.max(1.0, base.meleeStrength() + 64.0);
                }
                if (type == AttackType.RANGED && extra.rangedStrength() != 0) {
                    return (base.rangedStrength() + extra.rangedStrength() + 64.0) / Math.max(1.0, base.rangedStrength() + 64.0);
                }
                if (type == AttackType.MAGIC && extra.magicDamagePercent() != 0) {
                    return 1.0 + extra.magicDamagePercent() / 100.0;
                }
                return 1.0;
            }
        });
    }

    static void wear(ServerPlayerEntity player, QuestBook.Gear gear, ItemStack stack) {
        if (gear.requiredQuest() != null) {
            QuestInfo q = ContentRegistry.quest(gear.requiredQuest());
            if (q != null && !Players.data(player).questComplete(q)) {
                Msg.game(player, "You need to complete " + q.displayName() + " to wear that.");
                return;
            }
        }
        String slot = gear.slot().name().toLowerCase();
        String old = QuestsState.get(player).wear(player.getUuid(), slot, gear.item());
        stack.decrement(1);
        if (old != null) {
            Inv.give(player, old, 1);
        }
        Msg.game(player, "You wear the " + QuestInterpreter.describe(gear.item(), 1).replaceFirst("^an? ", "") + " (" + slot + " slot).");
        com.runecraft.network.ServerNetworking.sync(player);
    }

    static void remove(ServerPlayerEntity player, String slot) {
        String old = QuestsState.get(player).wear(player.getUuid(), slot, null);
        if (old == null) {
            Msg.game(player, "You aren't wearing anything in your " + slot + " slot.");
            return;
        }
        Inv.give(player, old, 1);
        Msg.game(player, "You remove your " + QuestInterpreter.describe(old, 1).replaceFirst("^an? ", "") + ".");
        com.runecraft.network.ServerNetworking.sync(player);
    }

    /** Total bonuses of the accessories a player wears. */
    static EquipmentStats bonus(ServerPlayerEntity player) {
        EquipmentStats total = EquipmentStats.NONE;
        Map<String, QuestBook.Gear> gear = QuestBook.get().gear;
        for (String item : QuestsState.get(player).wornAll(player.getUuid()).values()) {
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
    static void enforceWornGates(ServerPlayerEntity player) {
        Map<String, String> gates = QuestBook.get().weaponGates;
        for (EquipmentSlot slot : WORN) {
            ItemStack worn = player.getEquippedStack(slot);
            if (worn.isEmpty()) {
                continue;
            }
            String quest = gates.get(Inv.idOf(worn));
            QuestInfo q = quest == null ? null : ContentRegistry.quest(quest);
            if (q == null || Players.data(player).questComplete(q)) {
                continue;
            }
            ItemStack copy = worn.copy();
            player.equipStack(slot, ItemStack.EMPTY);
            if (!player.getInventory().insertStack(copy)) {
                player.dropItem(copy, false);
            }
            Msg.red(player, "You need to complete " + q.displayName() + " to wear that.");
        }
    }

    /** True when the wielded weapon needs a quest the player hasn't completed. */
    static boolean locked(ServerPlayerEntity player) {
        String weapon = Inv.idOf(player.getMainHandStack());
        String quest = QuestBook.get().weaponGates.get(weapon);
        if (quest == null) {
            return false;
        }
        QuestInfo q = ContentRegistry.quest(quest);
        if (q == null || Players.data(player).questComplete(q)) {
            return false;
        }
        long now = player.getWorld().getTime();
        if (now - LAST_WARNING.getOrDefault(player.getUuid(), -1000L) > 100) {
            LAST_WARNING.put(player.getUuid(), now);
            Msg.red(player, "You need to complete " + q.displayName() + " to wield that weapon.");
        }
        return true;
    }
}
