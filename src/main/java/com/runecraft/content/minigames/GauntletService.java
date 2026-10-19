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
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (id.equals(Gauntlet.EGNIOL)) {
                if (!sp.getCooldowns().isOnCooldown(stack.getItem())) {
                    drinkEgniol(sp, stack, hand);
                }
                return InteractionResultHolder.success(stack);
            }
            if (id.equals(Gauntlet.ESCAPE_CRYSTAL)) {
                if (MinigameManager.session(sp) instanceof GauntletSession) {
                    stack.shrink(1);
                    Msg.action(sp, "The crystal shatters and you escape the Gauntlet.");
                    MinigameManager.leave(sp);
                } else {
                    Msg.game(sp, "The crystal only works inside the Gauntlet.");
                }
                return InteractionResultHolder.success(stack);
            }
            for (Gauntlet.Mode m : Gauntlet.Mode.values()) {
                if (id.equals(Gauntlet.teleport(m))) {
                    if (MinigameManager.session(sp) instanceof GauntletSession g && g.teleportToStart(sp)) {
                        stack.shrink(1);
                        Msg.action(sp, "The crystal shatters and returns you to the starting room.");
                    } else if (!(MinigameManager.session(sp) instanceof GauntletSession)) {
                        Msg.game(sp, "The crystal only works inside the Gauntlet.");
                    }
                    return InteractionResultHolder.success(stack);
                }
            }
            return InteractionResultHolder.pass(stack);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (GauntletStash.has(p) && !(MinigameManager.session(p) instanceof GauntletSession)) {
                GauntletStash.restore(p);
                Msg.game(p, "Your belongings from before the Gauntlet have been returned.");
            }
        });
    }

    /** Worn crystal or corrupted armour lowers the Hunllef's and its tornadoes' hits by the sum of the pieces' tiers. */
    static int armour(ServerPlayer p, LivingEntity attacker, int damage) {
        if (damage <= 0 || attacker == null || !(MinigameManager.session(p) instanceof GauntletSession g)) {
            return damage;
        }
        String id = MinigameManager.creatureId(attacker);
        if (!id.equals(g.mode().boss) && !id.equals("hunllef_tornado")) {
            return damage;
        }
        int sum = Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.HEAD)))
                + Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.CHEST)))
                + Gauntlet.armourTier(Inv.idOf(p.getItemBySlot(EquipmentSlot.LEGS)));
        return (int) Math.round(damage * Gauntlet.hunllefDamageScale(g.mode(), sum));
    }

    /** Egniol potion: restores floor(Prayer / 4) + 7 prayer points and 40% run energy, and acts as a stamina potion. */
    static void drinkEgniol(ServerPlayer p, ItemStack stack, InteractionHand hand) {
        PlayerData data = Players.data(p);
        int level = data.skills().level(Skill.PRAYER);
        data.setPrayerPoints(Math.min(level, data.prayerPoints() + Gauntlet.egniolPrayer(level)));
        SysState.SysPlayer sp = SysState.of(p);
        sp.runEnergy = Math.min(RunEnergy.MAX, sp.runEnergy + RunEnergy.MAX * 40 / 100);
        sp.staminaUntil = com.runecraft.action.ActionManager.gameTick() + 200;
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        p.getCooldowns().addCooldown(stack.getItem(), 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
        int used = stack.getDamageValue() + 1;
        if (used >= 4) {
            p.setItemInHand(hand, Inv.stack(Gauntlet.VIAL, 1));
            Msg.action(p, "You drink the last of your egniol potion.");
        } else {
            stack.setDamageValue(used);
            Msg.action(p, "You drink some of your egniol potion. You have " + (4 - used) + " dose" + (used == 3 ? "" : "s") + " left.");
        }
        SysNet.sendState(p);
        com.runecraft.network.ServerNetworking.sync(p);
    }
}
