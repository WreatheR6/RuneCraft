package com.runecraft.content.minigames;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.minigames.LastManStanding;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Last Man Standing outside the island: Lisa (game info, rank and the preferred build), Justine (the reward shop and Victor's
 * capes), PvP between players of the same minigame for every attack style, giving back stashed items to players who log in
 * after a crash, and the LMS imbue scrolls (ring of wealth and magic shortbow scrolls) used on their item.
 */
public final class LmsService {
    /** Victor's capes in {@link LastManStanding#VICTOR_WINS} order. */
    private static final String[] VICTOR_CAPES = {"victors_cape_1", "victors_cape_10", "victors_cape_50", "victors_cape_100",
            "victors_cape_500", "victors_cape_1000"};

    private LmsService() {
    }

    static void register() {
        // PvP inside a minigame follows the minigame's rules for melee, ranged and magic alike (Last Man Standing, Castle Wars,
        // the Fight Pit); everywhere else the Wilderness rules apply.
        com.runecraft.combat.CombatHandler.pvpOverride = (attacker, target) -> {
            MinigameSession s = MinigameManager.session(attacker);
            if (s == null || s != MinigameManager.session(target)) {
                return null;
            }
            return s.canAttackPlayer(attacker, target);
        };
        // Prayer rules of the minigame the player is in: the LMS build's prayers, or an Emir's Arena No Prayer duel.
        com.runecraft.combat.PrayerService.minigameRule = (player, prayer) -> {
            MinigameSession s = MinigameManager.session(player);
            if (s instanceof com.runecraft.content.minigames.games.LmsSession lms) {
                return lms.prayerRule(prayer);
            }
            if (s instanceof com.runecraft.content.minigames.games.EmirsArenaSession duel) {
                return duel.prayerRule(prayer);
            }
            return null;
        };
        QuestScripts.onTalk(LastManStanding.LISA, (player, npc) -> lisa(player));
        QuestScripts.onTalk(LastManStanding.JUSTINE, (player, npc) -> justine(player));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (MinigameManager.session(p) == null && LmsStash.has(p)) {
                LmsStash.restore(p);
                Msg.game(p, "You get back the items and levels you had before your Last Man Standing game or ranked duel.");
            }
        });
    }

    /** Lisa: rank, points and wins; talking while sneaking changes the preferred build. */
    static void lisa(ServerPlayerEntity p) {
        MinigameData data = MinigameData.of(p);
        UUID u = p.getUuid();
        int pref = data.stat(u, LastManStanding.BUILD_PREFERENCE);
        if (p.isSneaking()) {
            pref = LastManStanding.Build.of(pref + 1).ordinal();
            data.setStat(u, LastManStanding.BUILD_PREFERENCE, pref);
        }
        int rank = data.stat(u, LastManStanding.RANK);
        rank = rank == 0 ? LastManStanding.START_RANK : rank;
        QuestApi.say(p, "Lisa", "Welcome to Last Man Standing! Step through the casual portal for a friendly game, or the "
                + "competitive one for points and rank. I'll look after your belongings while you're on the island.");
        Msg.game(p, "Preferred build: " + LastManStanding.Build.of(pref).displayName + " (talk to Lisa while sneaking to change it).");
        Msg.game(p, "Rank " + rank + " (" + LastManStanding.rankTitle(rank) + "), competitive wins "
                + data.stat(u, LastManStanding.WINS) + ", kills " + data.stat(u, LastManStanding.KILLS) + ", points "
                + data.points(u, com.runecraft.osrs.content.minigames.MinigameCurrencies.key(LastManStanding.POINTS)) + ".");
    }

    /** Justine: hands out any Victor's cape the player has earned and isn't carrying, then opens her shop. */
    static void justine(ServerPlayerEntity p) {
        int wins = MinigameData.of(p).stat(p.getUuid(), LastManStanding.WINS);
        for (int i = 0; i < VICTOR_CAPES.length; i++) {
            int w = LastManStanding.VICTOR_WINS[i];
            String cape = VICTOR_CAPES[i];
            if (wins >= w && !Inv.has(p, cape, 1) && !SysState.of(p).wornId(Jewellery.Slot.CAPE).equals(cape)) {
                Inv.give(p, cape, 1);
                CollectionLog.obtain(p, cape);
                QuestApi.say(p, "Justine", "You've won " + w + " game" + (w == 1 ? "" : "s") + "! Take this Victor's cape.");
            }
        }
        QuestApi.say(p, "Justine", "Have a look at my stuff for the Last Shopper Standing.");
        RewardShopService.open(p, LastManStanding.SHOP);
    }

    /**
     * Uses an imbue scroll on the first matching item the player carries or wears (ring of wealth in any charge state, magic
     * shortbow). Returns true if the scroll was used up.
     */
    static boolean useImbueScroll(ServerPlayerEntity p, String scroll) {
        SysState.SysPlayer sp = SysState.of(p);
        ItemStack ring = sp.worn(Jewellery.Slot.RING);
        String worn = LastManStanding.imbued(scroll, Inv.idOf(ring));
        if (worn != null) {
            sp.worn.put(Jewellery.Slot.RING, Inv.stack(worn, 1));
            imbuedMessage(p, worn);
            return true;
        }
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            String to = s.isEmpty() ? null : LastManStanding.imbued(scroll, Inv.idOf(s));
            if (to != null) {
                s.decrement(1);
                if (s.isEmpty()) {
                    inv.setStack(i, Inv.stack(to, 1));
                } else {
                    Inv.give(p, to, 1);
                }
                imbuedMessage(p, to);
                return true;
            }
        }
        String[] pair = LastManStanding.IMBUE_SCROLLS.get(scroll);
        ItemDef target = pair == null ? null : ItemCatalog.get(pair[0]);
        Msg.game(p, "Use this scroll with a " + (target == null ? "matching item" : target.name().toLowerCase()) + " to imbue it.");
        return false;
    }

    private static void imbuedMessage(ServerPlayerEntity p, String to) {
        ItemDef def = ItemCatalog.get(to);
        Msg.game(p, "You imbue the item with the scroll: you now have " + (def == null ? to : def.name()) + ".");
    }
}
