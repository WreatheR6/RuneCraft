package com.runecraft.content.minigames;

import com.runecraft.content.GameEvents;
import com.runecraft.content.minigames.games.GauntletSession;
import com.runecraft.content.systems.SysNet;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.systems.RunEnergy;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

import java.util.Random;

/**
 * Gauntlet behaviour outside the session itself: the reward chest's main-table rolls on every Hunllef kill, the armour's
 * damage reduction against the Hunllef and its tornadoes, the egniol potion and teleport crystal, and giving a stash back
 * to a player who logs in after a crash mid-run.
 */
public final class GauntletService {
    private static final Random RANDOM = new Random();

    private GauntletService() {
    }

    static void register() {
        // Wiki Reward Chest: a Crystalline Hunllef kill rolls the regular table twice, a Corrupted one the corrupted table three
        // times (the shards, seeds, clue and cape drop from the boss itself). Summoning-stone kills outside the minigame too.
        GameEvents.onKill((player, entity, stats) -> {
            Gauntlet.Mode mode = Gauntlet.modeOfBoss(MinigameManager.creatureId(entity));
            if (mode == null) {
                return;
            }
            Gauntlet.Table table = Gauntlet.completion(mode);
            MinigameSession s = MinigameManager.session(player);
            for (int i = 0; i < table.rolls(); i++) {
                Gauntlet.Loot l = table.roll(RANDOM);
                int n = l.min() + RANDOM.nextInt(l.max() - l.min() + 1);
                if (s instanceof GauntletSession g) {
                    g.owe(l.item(), n);
                } else {
                    Inv.give(player, l.item(), n);
                }
            }
        });
        com.runecraft.combat.CombatHooks.addIncomingModifier(GauntletService::armour);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || stack.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (id.equals(Gauntlet.EGNIOL)) {
                if (!sp.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    drinkEgniol(sp, stack, hand);
                }
                return TypedActionResult.success(stack);
            }
            if (id.equals(Gauntlet.ESCAPE_CRYSTAL)) {
                if (MinigameManager.session(sp) instanceof GauntletSession) {
                    stack.decrement(1);
                    Msg.action(sp, "The crystal shatters and you escape the Gauntlet.");
                    MinigameManager.leave(sp);
                } else {
                    Msg.game(sp, "The crystal only works inside the Gauntlet.");
                }
                return TypedActionResult.success(stack);
            }
            for (Gauntlet.Mode m : Gauntlet.Mode.values()) {
                if (id.equals(Gauntlet.teleport(m))) {
                    if (MinigameManager.session(sp) instanceof GauntletSession g && g.teleportToStart(sp)) {
                        stack.decrement(1);
                        Msg.action(sp, "The crystal shatters and returns you to the starting room.");
                    } else if (!(MinigameManager.session(sp) instanceof GauntletSession)) {
                        Msg.game(sp, "The crystal only works inside the Gauntlet.");
                    }
                    return TypedActionResult.success(stack);
                }
            }
            return TypedActionResult.pass(stack);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (GauntletStash.has(p) && !(MinigameManager.session(p) instanceof GauntletSession)) {
                GauntletStash.restore(p);
                Msg.game(p, "Your belongings from before the Gauntlet have been returned.");
            }
        });
    }

    /** Worn crystal or corrupted armour lowers the Hunllef's and its tornadoes' hits by the sum of the pieces' tiers. */
    static int armour(ServerPlayerEntity p, LivingEntity attacker, int damage) {
        if (damage <= 0 || attacker == null || !(MinigameManager.session(p) instanceof GauntletSession g)) {
            return damage;
        }
        String id = MinigameManager.creatureId(attacker);
        if (!id.equals(g.mode().boss) && !id.equals("hunllef_tornado")) {
            return damage;
        }
        int sum = Gauntlet.armourTier(Inv.idOf(p.getEquippedStack(EquipmentSlot.HEAD)))
                + Gauntlet.armourTier(Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST)))
                + Gauntlet.armourTier(Inv.idOf(p.getEquippedStack(EquipmentSlot.LEGS)));
        return (int) Math.round(damage * Gauntlet.hunllefDamageScale(g.mode(), sum));
    }

    /** Egniol potion: restores floor(Prayer / 4) + 7 prayer points and 40% run energy, and acts as a stamina potion. */
    static void drinkEgniol(ServerPlayerEntity p, ItemStack stack, Hand hand) {
        PlayerData data = Players.data(p);
        int level = data.skills().level(Skill.PRAYER);
        data.setPrayerPoints(Math.min(level, data.prayerPoints() + Gauntlet.egniolPrayer(level)));
        SysState.SysPlayer sp = SysState.of(p);
        sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + RunEnergy.MAX * 40 / 100);
        sp.staminaUntil = com.runecraft.action.ActionManager.gameTick() + 200;
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
        p.getItemCooldownManager().set(stack.getItem(), 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
        int used = stack.getDamage() + 1;
        if (used >= 4) {
            p.setStackInHand(hand, Inv.stack(Gauntlet.VIAL, 1));
            Msg.action(p, "You drink the last of your egniol potion.");
        } else {
            stack.setDamage(used);
            Msg.action(p, "You drink some of your egniol potion. You have " + (4 - used) + " dose" + (used == 3 ? "" : "s") + " left.");
        }
        SysNet.sendState(p);
        com.runecraft.network.ServerNetworking.sync(p);
    }
}
