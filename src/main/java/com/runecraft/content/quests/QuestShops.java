package com.runecraft.content.quests;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/** Quest-locked NPC shops (Ava's devices, Culinaromancer's chest, quest point cape). */
final class QuestShops {
    private QuestShops() {
    }

    static boolean qualifies(ServerPlayerEntity p, QuestBook.Shop s) {
        PlayerData data = Players.data(p);
        if (s.quest() != null) {
            if (s.quest().equals("*")) {
                if (data.questPoints() < Quest.totalQuestPoints()) {
                    return false;
                }
            } else if (!QuestInterpreter.complete(p, s.quest())) {
                return false;
            }
        }
        if (!s.flagsMet(f -> QuestInterpreter.flag(p, f))) {
            return false;
        }
        return s.skill() == null || data.skills().level(s.skill()) >= s.level();
    }

    static boolean owns(ServerPlayerEntity p, String item) {
        return Inv.has(p, item, 1) || QuestsState.get(p).wornAll(p.getUuid()).containsValue(item);
    }

    /** Offers the best qualifying item this NPC sells that the player doesn't own; returns whether it said anything. */
    static boolean offer(ServerPlayerEntity p, String npcId, String name) {
        List<QuestBook.Shop> shops = QuestBook.get().shops.stream().filter(s -> s.npc().equals(npcId)).toList();
        for (int i = shops.size() - 1; i >= 0; i--) {
            QuestBook.Shop s = shops.get(i);
            if (!qualifies(p, s) || owns(p, s.item())) {
                continue;
            }
            QuestApi.say(p, name, s.pitch());
            if (!Inv.has(p, Inv.COINS, s.price())) {
                Msg.game(p, "You need " + String.format("%,d", s.price()) + " coins.");
                return true;
            }
            Inv.remove(p, Inv.COINS, s.price());
            QuestApi.give(p, s.item(), 1);
            com.runecraft.collection.CollectionLog.obtain(p, s.item());
            Msg.game(p, "You buy " + QuestInterpreter.describe(s.item(), 1) + " for " + String.format("%,d", s.price()) + " coins.");
            return true;
        }
        return false;
    }
}
