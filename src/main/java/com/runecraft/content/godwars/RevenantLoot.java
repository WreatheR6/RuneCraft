package com.runecraft.content.godwars;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.SkullService;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.util.Msg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Revenant kills roll the level-scaled unique table (amulet of avarice, revenant weapons) and ancient artefacts
 * ({@link RevenantRewards}): better skulled and on a revenant Slayer task.
 */
final class RevenantLoot {
    private RevenantLoot() {
    }

    static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        if (stats == null || stats.profile() == null || !stats.profile().id.startsWith("revenant_")) {
            return;
        }
        boolean skulled = SkullService.isSkulled(player);
        boolean onTask = SlayerService.isOnTask(player, entity);
        for (String item : RevenantRewards.roll(stats.combatLevel(), skulled, onTask, entity.getRandom()::nextInt)) {
            entity.spawnAtLocation(CombatHooks.dropStack(player, entity, item, 1));
            CollectionLog.obtain(player, item);
            Msg.game(player, "The revenant drops something valuable!");
        }
    }
}
