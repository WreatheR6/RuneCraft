package com.runecraft.quest;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Helpers for scripting module quests (dialogue, stages, items, completion). */
public final class QuestApi {
    private QuestApi() {
    }

    public static QuestInfo quest(String id) {
        QuestInfo q = ContentRegistry.quest(id);
        if (q == null) {
            throw new IllegalArgumentException("Unknown quest " + id);
        }
        return q;
    }

    /** NPC dialogue line. */
    public static void say(ServerPlayer player, String speaker, String line) {
        player.displayClientMessage(Component.literal(speaker + ": ").withStyle(ChatFormatting.DARK_BLUE).append(Component.literal(line)), false);
    }

    /** Player dialogue line. */
    public static void you(ServerPlayer player, String line) {
        player.displayClientMessage(Component.literal("You: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(line)), false);
    }

    public static int stage(ServerPlayer player, String questId) {
        return Players.data(player).questStage(quest(questId));
    }

    /** Sets the stage (announces "You have started a new quest" when leaving stage 0). */
    public static void setStage(ServerPlayer player, String questId, int stage) {
        QuestManager.set(player, quest(questId), stage);
    }

    /** Starts the quest if requirements are met; returns false (and tells the player) otherwise. */
    public static boolean start(ServerPlayer player, String questId) {
        QuestInfo q = quest(questId);
        if (!QuestManager.meetsRequirements(player, q)) {
            return false;
        }
        QuestManager.set(player, q, Math.max(1, Players.data(player).questStage(q)));
        return true;
    }

    public static boolean complete(ServerPlayer player, String questId) {
        QuestManager.complete(player, quest(questId));
        return true;
    }

    public static boolean isComplete(ServerPlayer player, String questId) {
        return Players.data(player).questComplete(quest(questId));
    }

    /** Removes all the items if the player has every one of them; returns whether it did. */
    public static boolean takeAll(ServerPlayer player, Map<String, Integer> items) {
        return QuestManager.takeAll(player, items);
    }

    public static boolean has(ServerPlayer player, String item, int count) {
        return Inv.has(player, item, count);
    }

    public static void give(ServerPlayer player, String item, int count) {
        Inv.give(player, item, count);
        ServerNetworking.sync(player);
    }

    public static PlayerData data(ServerPlayer player) {
        return Players.data(player);
    }
}
