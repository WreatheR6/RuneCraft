package com.runecraft.combat;

import com.runecraft.content.slayer.SlayerService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Slayer entry points used by core code (slayer master stations, enchanted gems, combat). The
 * implementation lives in the slayer content module ({@link SlayerService}): task categories that
 * include module monsters, every OSRS master (Turael to Duradel, Konar, Krystilia), slayer points,
 * streaks, extensions, blocks and the black mask / slayer helmet bonus.
 */
public final class Slayer {
    private Slayer() {
    }

    /** Generic slayer master station: assigns a task from the best master for the player's levels. */
    public static void talkToMaster(ServerPlayer player) {
        SlayerService.talkToStation(player);
    }

    public static void checkTask(ServerPlayer player) {
        SlayerService.checkTask(player);
    }

    /** Kills on task give Slayer XP equal to the monster's hitpoints (superiors: their OSRS XP). */
    public static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        if (entity instanceof com.runecraft.boss.DataBossEntity d && d.def() != null && d.variant() == null
                && com.runecraft.boss.DataBossService.isCompanion(d.def().id)) {
            // A companion's death is part of its main boss's kill (Dawn: OSRS gives task progress and Slayer XP on Dusk's).
            return;
        }
        if (entity instanceof com.runecraft.boss.DataBossEntity d
                && com.runecraft.osrs.content.bosses.AwakenedBosses.offTask(d.awakened())) {
            return; // an awakened Desert Treasure II boss is no Slayer task kill (OSRS Wiki)
        }
        if (stats != null && stats.profile() != null
                && !com.runecraft.osrs.content.bosses.SlayerBossRules.taskKill(stats.profile().id)) {
            // Araxxor's araxytes: Slayer XP while they are on task, but no task kill (OSRS Wiki).
            if (SlayerService.isOnTask(player, entity) && com.runecraft.skill.XpService.allowed(player, com.runecraft.osrs.Skill.SLAYER)
                    && com.runecraft.osrs.content.bosses.SlayerBossRules.taskXp(stats.profile().id)) {
                com.runecraft.skill.XpService.add(player, com.runecraft.osrs.Skill.SLAYER,
                        com.runecraft.osrs.data.MonsterTraits.slayerXp(stats.traitsKey(), stats.hitpoints()));
            }
            return;
        }
        SlayerService.onKill(player, entity, stats);
    }

    /** Black mask / slayer helmet: +16.67% melee max hit against the current task. */
    public static int boostMaxHit(ServerPlayer player, LivingEntity target, int maxHit) {
        return SlayerService.boostMaxHit(player, target, maxHit);
    }
}
