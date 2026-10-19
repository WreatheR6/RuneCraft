package com.runecraft.content.islands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.islands.Kingdom;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Managing Miscellania: the royal ledger in King Vargas' keep (click to collect what the islanders
 * gathered and hear the kingdom's state, sneak-click to pay coins into the coffer) and the
 * workforce board (click to see the allocation, with [+]/[-] links that move subjects between
 * labours; {@code /kingdom withdraw <coins>} takes money back out). After Royal Trouble, Advisor
 * Ghrim does the same as the ledger. Gathering on Miscellania and Etceteria raises approval (wiki
 * table, see {@link Kingdom#approvalFor}); reaching 100% fires the {@code kingdom_approval:100}
 * diary activity. Kingdom days are real days (midnight UTC), as in OSRS.
 */
final class IslandsKingdomService {
    private static final String ADVISOR = "Advisor Ghrim";
    private static boolean wrapped;

    private IslandsKingdomService() {
    }

    static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrapGhrim());
        GameEvents.onObtain((player, item, count) -> onObtain(player, item, count));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("kingdom")
                        .then(Commands.literal("assign").then(Commands.argument("work", StringArgumentType.word())
                                .then(Commands.argument("delta", IntegerArgumentType.integer(-10, 10)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        assign(p, StringArgumentType.getString(ctx, "work"), IntegerArgumentType.getInteger(ctx, "delta"));
                                    }
                                    return 1;
                                }))))
                        .then(Commands.literal("withdraw").then(Commands.argument("coins",
                                IntegerArgumentType.integer(1)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        withdraw(p, IntegerArgumentType.getInteger(ctx, "coins"));
                                    }
                                    return 1;
                                })))));
    }

    private static void wrapGhrim() {
        if (wrapped) {
            return;
        }
        wrapped = true;
        BiConsumer<ServerPlayer, QuestNpcEntity> quest = QuestScripts.handler("advisor_ghrim");
        QuestScripts.onTalk("advisor_ghrim", (player, npc) -> {
            if (quest != null && !QuestApi.isComplete(player, "royal_trouble")) {
                quest.accept(player, npc);
                return;
            }
            ledger(player);
        });
    }

    private static boolean royalTrouble(ServerPlayer player) {
        return QuestApi.isComplete(player, "royal_trouble");
    }

    private static boolean unlocked(ServerPlayer player) {
        if (QuestApi.isComplete(player, "throne_of_miscellania")) {
            return true;
        }
        QuestApi.say(player, ADVISOR, "Only the ruler of Miscellania may manage the kingdom. Complete Throne of Miscellania first.");
        return false;
    }

    /** Today's day number (days since 1970 in UTC): the kingdom pays wages and gathers at midnight UTC. */
    static long today() {
        return Math.floorDiv(System.currentTimeMillis(), 86_400_000L);
    }

    /** The player's kingdom, advanced to today. */
    private static Kingdom.State current(ServerPlayer player, IslandsKingdomState store) {
        UUID uuid = player.getUUID();
        Kingdom.State s = store.kingdom(uuid);
        long last = store.day(uuid);
        long now = today();
        if (last >= 0 && now > last) {
            s = Kingdom.advance(s, (int) Math.min(Kingdom.MAX_DAYS, now - last), royalTrouble(player));
        }
        store.set(uuid, s, now);
        return s;
    }

    static void ledger(ServerPlayer player) {
        if (!unlocked(player)) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        boolean rt = royalTrouble(player);
        if (player.isShiftKeyDown()) {
            long fits = Kingdom.depositable(s, Inv.count(player, Inv.COINS), rt);
            if (fits <= 0) {
                QuestApi.say(player, ADVISOR, s.coffer() >= Kingdom.maxCoffer(rt)
                        ? "The coffer is full: " + String.format("%,d", Kingdom.maxCoffer(rt)) + " coins."
                        : "You have no coins to pay into the coffer.");
                return;
            }
            Inv.remove(player, Inv.COINS, (int) fits);
            s = Kingdom.deposit(s, fits, rt);
            store.set(player.getUUID(), s, today());
            QuestApi.say(player, ADVISOR, "You pay " + String.format("%,d", fits) + " coins into the coffer. It now holds "
                    + String.format("%,d", s.coffer()) + ".");
            return;
        }
        Map<String, Integer> loot = Kingdom.collect(s, rt);
        if (!loot.isEmpty()) {
            for (Map.Entry<String, Integer> e : loot.entrySet()) {
                Inv.give(player, e.getKey(), e.getValue());
            }
            Msg.game(player, "You collect what your subjects have gathered.");
            s = Kingdom.collected(s);
            store.set(player.getUUID(), s, today());
        }
        QuestApi.say(player, ADVISOR, "Coffer: " + String.format("%,d", s.coffer()) + " coins (wages "
                + String.format("%,d", Kingdom.dailySpend(s.coffer(), rt)) + " a day). Approval: " + s.approvalPercent() + "%.");
        Msg.game(player, "(Sneak and use the ledger to pay all your coins into the coffer; the workforce board assigns your subjects.)");
        if (s.approval() >= Kingdom.MAX_APPROVAL) {
            GameEvents.fireActivity(player, "kingdom_approval:100");
        }
    }

    static void workforce(ServerPlayer player) {
        if (!unlocked(player)) {
            return;
        }
        Kingdom.State s = current(player, IslandsKingdomState.get(player.getServer()));
        boolean rt = royalTrouble(player);
        QuestApi.say(player, ADVISOR, "Your subjects: " + s.assigned() + " of " + Kingdom.maxWorkers(rt) + " at work.");
        for (Kingdom.Work w : Kingdom.Work.values()) {
            if (w.needsRoyalTrouble() && !rt) {
                continue;
            }
            String cmd = "/kingdom assign " + w.name().toLowerCase(java.util.Locale.ROOT) + " ";
            MutableComponent line = Component.literal(" " + w.label() + ": " + s.workers(w) + " ").withStyle(ChatFormatting.GRAY)
                    .append(link("+", cmd + "1", "Assign a subject", ChatFormatting.GREEN)).append(Component.literal(" "))
                    .append(link("-", cmd + "-1", "Take a subject off", ChatFormatting.RED));
            player.displayClientMessage(line, false);
        }
    }

    private static MutableComponent link(String label, String command, String hover, ChatFormatting color) {
        return Component.literal("[" + label + "]").withStyle(st -> st.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    static void assign(ServerPlayer player, String work, int delta) {
        if (!unlocked(player)) {
            return;
        }
        Kingdom.Work w;
        try {
            w = Kingdom.Work.valueOf(work.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Msg.game(player, "There is no such labour.");
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        Kingdom.State next = Kingdom.assign(s, w, delta, royalTrouble(player));
        if (next == s) {
            Msg.game(player, delta > 0 ? "You can't assign another subject to that (ten at most per labour, and only idle subjects)."
                    : "Nobody is working on that.");
            return;
        }
        store.set(player.getUUID(), next, today());
        Msg.game(player, w.label() + ": " + next.workers(w) + " subject" + (next.workers(w) == 1 ? "" : "s") + ". ("
                + next.assigned() + " assigned.)");
    }

    static void withdraw(ServerPlayer player, int coins) {
        if (!unlocked(player)) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        Kingdom.State next = Kingdom.withdraw(s, coins);
        long taken = s.coffer() - next.coffer();
        if (taken <= 0) {
            Msg.game(player, "The coffer is empty.");
            return;
        }
        store.set(player.getUUID(), next, today());
        Inv.give(player, Inv.COINS, (int) taken);
        QuestApi.say(player, ADVISOR, "You withdraw " + String.format("%,d", taken) + " coins. The coffer holds "
                + String.format("%,d", next.coffer()) + ".");
    }

    /** Gathering on the islands raises approval (wiki table). */
    static void onObtain(ServerPlayer player, String item, int count) {
        if (!Gielinor.is(player) || count <= 0) {
            return;
        }
        int x = GielinorMap.toOsrsX(player.getBlockX());
        int y = GielinorMap.toOsrsY(player.getBlockZ());
        if (!Kingdom.onIslands(x, y) || !QuestApi.isComplete(player, "throne_of_miscellania")) {
            return;
        }
        int gain = 0;
        for (int i = 0; i < count; i++) {
            gain += Kingdom.approvalFor(item, player.getRandom().nextDouble(), royalTrouble(player), 0); // approx: no diary bonus
        }
        if (gain <= 0) {
            return;
        }
        IslandsKingdomState store = IslandsKingdomState.get(player.getServer());
        Kingdom.State s = current(player, store);
        if (s.approval() >= Kingdom.MAX_APPROVAL) {
            return;
        }
        Kingdom.State next = Kingdom.work(s, gain);
        store.set(player.getUUID(), next, today());
        Msg.game(player, "Thanks!");
        if (next.approval() >= Kingdom.MAX_APPROVAL) {
            Msg.game(player, "Your subjects' approval of you is now 100%.");
            GameEvents.fireActivity(player, "kingdom_approval:100");
        }
    }
}
