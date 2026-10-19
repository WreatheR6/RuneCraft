package com.runecraft.collection;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Tracks the first time a player obtains each collection log item. */
public final class CollectionLog {
    private CollectionLog() {
    }

    /** Logs an item and fires the obtain event for one of it. */
    public static void obtain(ServerPlayer player, String item) {
        com.runecraft.content.GameEvents.fireObtain(player, item, 1);
        record(player, item);
    }

    /** Logs an item without firing the obtain event (for callers that fire it themselves with the real amount). */
    public static void record(ServerPlayer player, String item) {
        if (!CollectionLogData.tracked(item)) {
            return;
        }
        PlayerData data = Players.data(player);
        if (data.collectionLog().add(item)) {
            ItemDef def = ItemCatalog.get(item);
            player.displayClientMessage(Component.literal("New item added to your collection log: " + (def == null ? item : def.name()))
                    .withStyle(ChatFormatting.RED), false);
            ServerNetworking.sync(player);
        }
    }
}
