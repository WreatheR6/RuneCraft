package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.RunEnergy;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * OSRS run energy in Gielinor: sprinting drains it, standing/walking restores it (faster with Agility),
 * and at 0% you can no longer run. Stamina and energy potions restore it.
 */
public final class RunEnergyService {
    private RunEnergyService() {
    }

    public static void restore(ServerPlayer p, int hundredths) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + hundredths);
        SysNet.sendState(p);
    }

    public static void gameTick(ServerPlayer p, long tick) {
        if (!Gielinor.is(p) || p.isCreative() || p.isSpectator()) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        int before = sp.runEnergy;
        boolean stamina = sp.staminaUntil > tick;
        int agility = Players.data(p).skills().currentLevel(Skill.AGILITY);
        String cape = sp.wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE);
        // A full Graceful set restores energy 30% faster, the same boost as the Agility cape (they don't stack).
        boolean agilityCape = cape.startsWith("agility_cape") || cape.equals("max_cape")
                || com.runecraft.osrs.content.skilling.AgilityData.fullGraceful(
                        Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)),
                        Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)),
                        Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS)),
                        Inv.idOf(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)), cape,
                        sp.wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.HANDS));
        if (p.isSprinting() && p.getVehicle() == null) {
            sp.runEnergy = Math.max(0, sp.runEnergy - RunEnergy.drain(carriedWeight(p, sp), stamina));
            if (sp.runEnergy <= 0) {
                // Out of energy: slow the player to walking pace (cancels the sprint speed bonus).
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                p.setSprinting(false);
                if (before > 0) {
                    Msg.game(p, "You are out of run energy.");
                }
            }
        } else {
            sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + RunEnergy.regen(agility, agilityCape));
        }
        if (sp.runEnergy / 100 != before / 100) {
            SysNet.sendState(p);
        }
    }

    /**
     * Carried weight in whole kg: OSRS weights (osrs/data/ItemValues) of everything in the inventory, armour
     * slots, off-hand and accessory slots. Each inventory slot counts its item once, since a Minecraft stack
     * stands in for one OSRS inventory slot (approx); worn Graceful pieces weigh less than nothing.
     */
    static int carriedWeight(ServerPlayer p, SysState.SysPlayer sp) {
        double kg = 0;
        net.minecraft.world.entity.player.Inventory inv = p.getInventory();
        for (ItemStack stack : inv.items) {
            kg += weightOf(stack, false);
        }
        for (ItemStack stack : inv.armor) {
            kg += weightOf(stack, true);
        }
        for (ItemStack stack : inv.offhand) {
            kg += weightOf(stack, true);
        }
        for (ItemStack worn : sp.worn.values()) {
            kg += weightOf(worn, true);
        }
        return (int) Math.floor(kg);
    }

    private static double weightOf(ItemStack stack, boolean worn) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        com.runecraft.osrs.ItemDef def = com.runecraft.registry.ModItems.def(stack);
        if (def == null) {
            return 0;
        }
        return worn ? com.runecraft.osrs.data.ItemValues.wornWeight(def.id()) : def.weight();
    }

    /** Stamina potion / super energy / energy potion drinking (items defined by other modules, looked up by id). */
    public static boolean isEnergyPotion(String id) {
        return id.startsWith("stamina_potion") || id.startsWith("super_energy") || id.equals("stamina_mix");
    }

    public static void drink(ServerPlayer p, ItemStack stack, InteractionHand hand, String id) {
        if (p.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (id.startsWith("stamina")) {
            sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + 2000);
            sp.staminaUntil = ActionManager.gameTick() + 200;
            Msg.action(p, "You drink some of your stamina potion. You feel your energy draining more slowly.");
        } else {
            sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + 2000);
            Msg.action(p, "You drink some of your super energy potion.");
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        com.runecraft.skill.ConsumeCooldowns.consume(p, Inv.idOf(stack));
        if (stack.isDamageableItem()) {
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                p.setItemInHand(hand, Inv.stack("vial", 1));
            } else {
                stack.setDamageValue(used);
            }
        } else {
            stack.shrink(1);
            Inv.give(p, "vial", 1);
        }
        SysNet.sendState(p);
    }
}
