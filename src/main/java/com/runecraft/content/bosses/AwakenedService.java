package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.AwakenedBosses;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Awakened Desert Treasure II bosses: using the summoning stone while holding an awakener's orb (after Desert Treasure
 * II) consumes the orb and starts the awakened fight. Awakened kills count separately; the first kill that completes
 * all four gives the ancient blood ornament kit.
 */
final class AwakenedService {
    private AwakenedService() {
    }

    static void init() {
        for (String id : AwakenedBosses.BOSSES) {
            DataBossService.variant(id, AwakenedService::choose);
            DataBossService.onKilled(id, AwakenedService::killed);
        }
    }

    private static BossVariant choose(ServerPlayerEntity player, BossDef boss) {
        if (!AwakenedBosses.ORB.equals(Inv.idOf(player.getMainHandStack()))) {
            return null;
        }
        var quest = com.runecraft.osrs.content.ContentRegistry.quest("desert_treasure_ii");
        if (quest != null && !Players.data(player).questComplete(quest)) {
            Msg.game(player, "The orb doesn't respond. (Complete Desert Treasure II to awaken the bosses.)");
            return null;
        }
        Inv.remove(player, AwakenedBosses.ORB, 1);
        BossVariant v = AwakenedBosses.variant(boss);
        Msg.red(player, "The awakener's orb shatters as its power floods into " + boss.name + "...");
        return v;
    }

    private static void killed(ServerPlayerEntity player, DataBossEntity boss) {
        BossDef def = boss.def();
        if (def == null || !boss.awakened()) {
            return;
        }
        if (AwakenedBosses.earnsBloodKit(def.id, Players.data(player).killCounts())) {
            Inv.give(player, AwakenedBosses.BLOOD_KIT, 1);
            CollectionLog.obtain(player, AwakenedBosses.BLOOD_KIT);
            player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                    + " has defeated all four awakened bosses and received an ancient blood ornament kit!").formatted(Formatting.DARK_RED), false);
        }
    }
}
