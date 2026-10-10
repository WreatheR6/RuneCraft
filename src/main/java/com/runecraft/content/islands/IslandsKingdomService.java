package com.runecraft.content.islands;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.islands.Kingdom;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Managing Miscellania: the royal ledger in King Vargas' keep (click to collect what the islanders
 * gathered and hear the kingdom's state, sneak-click to pay coins into the coffer) and the
 * workforce board (click to change what the islanders work on). Gathering resources on Miscellania
 * and Etceteria yourself raises the islanders' approval. A kingdom day is a Minecraft day.
 */
final class IslandsKingdomService {
    private static final String ADVISOR = "Advisor Ghrim";

    private IslandsKingdomService() {
    }

    private static boolean unlocked(ServerPlayerEntity player) {
        if (QuestApi.isComplete(player, "throne_of_miscellania")) {
            return true;
        }
        QuestApi.say(player, ADVISOR, "Only the ruler of Miscellania may manage the kingdom. Complete Throne of Miscellania first.");
        return false;
    }

    private static long today(ServerPlayerEntity player) {
        return player.getWorld().getTimeOfDay() / 24000L;
    }

    /** The player's kingdom, advanced to today. */
    private static Kingdom.State current(ServerPlayerEntity player, IslandsKingdomState store) {
        UUID uuid = player.getUuid();
        Kingdom.State s = store.kingdom(uuid);
        long last = store.day(uuid);
        long now = today(player);
        if (last >= 0 && now > last) {
            s = Kingdom.advance(s, (int) Math.min(Kingdom.MAX_DAYS, now - last), QuestApi.isComplete(player, "royal_trouble"));
        }
        store.set(uuid, s, now);
        return s;
    }

    static void ledger(ServerPlayerEntity player) {
        if (!unlocked(player)) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        if (player.isSneaking()) {
            long fits = Kingdom.depositable(s, Inv.count(player, Inv.COINS));
            if (fits <= 0) {
                QuestApi.say(player, ADVISOR, s.coffer() >= Kingdom.MAX_COFFER ? "The coffer is full: 7,500,000 coins."
                        : "You have no coins to pay into the coffer.");
                return;
            }
            Inv.remove(player, Inv.COINS, (int) fits);
            s = Kingdom.deposit(s, fits);
            store.set(player.getUuid(), s, today(player));
            QuestApi.say(player, ADVISOR, "You pay " + String.format("%,d", fits) + " coins into the coffer. It now holds "
                    + String.format("%,d", s.coffer()) + ".");
            return;
        }
        if (!s.stock().isEmpty()) {
            for (Map.Entry<String, Integer> e : s.stock().entrySet()) {
                Inv.give(player, e.getKey(), e.getValue());
            }
            Msg.game(player, "You collect what your subjects have gathered.");
            s = Kingdom.collected(s);
            store.set(player.getUuid(), s, today(player));
        }
        QuestApi.say(player, ADVISOR, "Coffer: " + String.format("%,d", s.coffer()) + " coins. Approval: " + s.approvalPercent()
                + "%. The people are working on " + s.work().label() + ".");
        Msg.game(player, "(Sneak and use the ledger to pay all your coins into the coffer.)");
    }

    static void workforce(ServerPlayerEntity player) {
        if (!unlocked(player)) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = Kingdom.reallocate(current(player, store));
        store.set(player.getUuid(), s, today(player));
        QuestApi.say(player, ADVISOR, "Very well. Your subjects will now work on " + s.work().label() + ".");
    }

    /** Working on the islands raises approval. */
    static void onXp(ServerPlayerEntity player, Skill skill) {
        if (!Gielinor.is(player) || !Kingdom.HELPFUL_SKILLS.contains(skill.name())) {
            return;
        }
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        if (x < 2480 || x > 2640 || y < 3835 || y > 3900 || !QuestApi.isComplete(player, "throne_of_miscellania")) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        if (s.approval() < Kingdom.MAX_APPROVAL) {
            store.set(player.getUuid(), Kingdom.work(s, 1), today(player));
        }
    }
}
