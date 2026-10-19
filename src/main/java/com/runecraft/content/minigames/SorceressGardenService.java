package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.RunEnergyService;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.MinigameWorld;
import com.runecraft.osrs.content.minigames.SorceressGarden;
import com.runecraft.osrs.content.minigames.SorceressGarden.Season;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import java.util.function.BiConsumer;

/**
 * Sorceress's Garden outside the instance: crushing sq'irks into sq'irkjuice (pestle and mortar and a
 * beer glass, 5 Cooking XP), drinking the juice (Thieving boost and run energy), Osman's Thieving XP
 * for the juice, the free beer glasses on the sorceress's shelves and her apprentice.
 */
public final class SorceressGardenService {
    private static boolean wrapped;

    private SorceressGardenService() {
    }

    static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            Season fruit = Season.byFruit(id);
            if (fruit != null) {
                startJuicing(sp, fruit);
                return InteractionResultHolder.success(stack);
            }
            Season juice = Season.byJuice(id);
            if (juice != null) {
                drink(sp, stack, juice);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
        BlockHooks.register(MinigameWorld.SORCERESS_SHELVES, (player, pos, state) -> {
            if (!Inv.hasRoomFor(player, "beer_glass")) {
                Msg.game(player, "You don't have enough inventory space.");
                return;
            }
            Inv.give(player, "beer_glass", 1);
            Msg.game(player, "You search the shelves and find an empty beer glass.");
        });
        QuestScripts.onTalk("mg_sorceress_apprentice", (player, npc) -> QuestApi.say(player, "Apprentice",
                "The sorceress grows sq'irks in her seasonal gardens. Step through the portal and I'll send you in - the "
                        + "elementals teleport out anyone they catch within two paces. Take beer glasses from the shelves "
                        + "and a pestle and mortar to crush the fruit."));
        // Osman's quest dialogue is registered by the quests module; wrap it once everything is loaded.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrapOsman());
    }

    // ------------------------------------------------------------------ sq'irkjuice

    private static boolean canJuice(ServerPlayer p, Season s) {
        return Inv.has(p, "pestle_and_mortar", 1) && Inv.has(p, "beer_glass", 1) && Inv.has(p, s.fruit(), s.perGlass());
    }

    private static void startJuicing(ServerPlayer p, Season s) {
        if (!Inv.has(p, "pestle_and_mortar", 1)) {
            Msg.game(p, "You need a pestle and mortar to crush the sq'irks.");
            return;
        }
        if (!Inv.has(p, "beer_glass", 1)) {
            Msg.game(p, "You need an empty beer glass to hold the juice.");
            return;
        }
        if (!Inv.has(p, s.fruit(), s.perGlass())) {
            Msg.game(p, "You need " + s.perGlass() + " " + s.id() + " sq'irks to fill a glass.");
            return;
        }
        squeeze(p, s);
        ActionManager.start(p, (player, tick) -> {
            if (!canJuice(player, s)) {
                return false;
            }
            squeeze(player, s);
            return true;
        });
    }

    private static void squeeze(ServerPlayer p, Season s) {
        Inv.remove(p, s.fruit(), s.perGlass());
        Inv.remove(p, "beer_glass", 1);
        Inv.give(p, s.juice(), 1);
        XpService.add(p, Skill.COOKING, SorceressGarden.JUICE_COOKING_XP);
        Msg.action(p, "You squeeze " + s.perGlass() + " sq'irks into an empty glass.");
    }

    private static void drink(ServerPlayer p, ItemStack stack, Season s) {
        if (p.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        p.getCooldowns().addCooldown(stack.getItem(), 3 * ActionManager.MC_TICKS_PER_GAME_TICK);
        stack.shrink(1);
        if (s.boost() > 0) {
            Players.data(p).skills().applyBoost(Skill.THIEVING, s.boost(), 0);
        }
        RunEnergyService.restore(p, s.energy() * 100);
        Inv.give(p, "beer_glass", 1); // approx: the wiki doesn't say whether the glass is kept
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(p, "You drink the sq'irkjuice. You feel more agile" + (s.boost() > 0 ? " and light-fingered." : "."));
        ServerNetworking.sync(p);
    }

    // ------------------------------------------------------------------ Osman

    private static void wrapOsman() {
        if (wrapped) {
            return;
        }
        wrapped = true;
        BiConsumer<ServerPlayer, QuestNpcEntity> quest = QuestScripts.handler("osman");
        QuestScripts.onTalk("osman", (player, npc) -> {
            if (!tradeJuice(player) && quest != null) {
                quest.accept(player, npc);
            }
        });
    }

    /** Hands every glass of sq'irkjuice to Osman for Thieving XP; false if there was nothing to trade. */
    static boolean tradeJuice(ServerPlayer player) {
        if (ContentRegistry.quest(SorceressGarden.QUEST) != null && !QuestApi.isComplete(player, SorceressGarden.QUEST)) {
            return false;
        }
        double total = 0;
        int glasses = 0;
        for (Season s : Season.values()) {
            int n = Inv.count(player, s.juice());
            if (n > 0) {
                Inv.remove(player, s.juice(), n);
                total += (double) n * s.osmanXp();
                glasses += n;
            }
        }
        if (glasses == 0) {
            return false;
        }
        XpService.add(player, Skill.THIEVING, total);
        QuestApi.say(player, "Osman", "Ah, sweet sq'irkjuice! Thank you, my friend. Let me teach you a trick or two in return.");
        Msg.game(player, "Osman takes " + glasses + " glass" + (glasses == 1 ? "" : "es") + " of sq'irkjuice and teaches you some "
                + "Thieving.");
        return true;
    }
}
