package com.runecraft.collection;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Tracks the first time a player obtains each collection log item. */
public final class CollectionLog {
    private CollectionLog() {
    }

    public static void obtain(ServerPlayerEntity player, String item) {
        if (!CollectionLogData.tracked(item)) {
            return;
        }
        PlayerData data = Players.data(player);
        if (data.collectionLog().add(item)) {
            ItemDef def = ItemCatalog.get(item);
            player.sendMessage(Text.literal("New item added to your collection log: " + (def == null ? item : def.name()))
                    .formatted(Formatting.RED), false);
            ServerNetworking.sync(player);
        }
    }
}
