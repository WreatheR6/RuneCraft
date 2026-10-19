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
        SlayerService.onKill(player, entity, stats);
    }

    /** Black mask / slayer helmet: +16.67% melee max hit against the current task. */
    public static int boostMaxHit(ServerPlayer player, LivingEntity target, int maxHit) {
        return SlayerService.boostMaxHit(player, target, maxHit);
    }
}
