package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.content.bosses.AwakenedBosses;
import com.runecraft.osrs.content.bosses.BossVariant;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.Dt2Loot;
import com.runecraft.osrs.data.Drop;
import com.runecraft.player.Players;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
            DataBossService.loot(id, AwakenedService::loot);
        }
    }

    /**
     * Desert Treasure II loot ({@link Dt2Loot}): one roll in order (unique table, orb, tablet, quartz, supplies, standard
     * table), with 50% more standard loot for a perfect kill (no avoidable damage taken over the fight).
     */
    private static List<Drops.Rolled> loot(ServerPlayer killer, DataBossEntity boss, List<Drop> table) {
        String id = boss.def().id;
        String rule = Dt2Bosses.PERFECT_RULE.get(id);
        boolean perfect = rule != null && CombatAchievementService.fightMeets(killer, boss, rule);
        Dt2Loot.Result r = Dt2Loot.roll(id, table, boss.awakened(), perfect, boss.getRandom()::nextInt);
        List<Drops.Rolled> out = new ArrayList<>();
        for (Drop d : r.items()) {
            out.add(new Drops.Rolled(d.baseItem(), d.min(), d.doses()));
        }
        return out;
    }

    private static BossVariant choose(ServerPlayer player, BossDef boss) {
        if (!AwakenedBosses.ORB.equals(Inv.idOf(player.getMainHandItem()))) {
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

    private static void killed(ServerPlayer player, DataBossEntity boss) {
        BossDef def = boss.def();
        if (def == null || !boss.awakened()) {
            return;
        }
        if (AwakenedBosses.earnsBloodKit(def.id, Players.data(player).killCounts())) {
            Inv.give(player, AwakenedBosses.BLOOD_KIT, 1);
            CollectionLog.obtain(player, AwakenedBosses.BLOOD_KIT);
            player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                    + " has defeated all four awakened bosses and received an ancient blood ornament kit!").withStyle(ChatFormatting.DARK_RED), false);
        }
    }
}
