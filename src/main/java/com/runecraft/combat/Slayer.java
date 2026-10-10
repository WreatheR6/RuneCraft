package com.runecraft.combat;

import com.runecraft.content.slayer.SlayerService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

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
    public static void talkToMaster(ServerPlayerEntity player) {
        SlayerService.talkToStation(player);
    }

    public static void checkTask(ServerPlayerEntity player) {
        SlayerService.checkTask(player);
    }

    /** Kills on task give Slayer XP equal to the monster's hitpoints (superiors: their OSRS XP). */
    public static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        SlayerService.onKill(player, entity, stats);
    }

    /** Black mask / slayer helmet: +16.67% melee max hit against the current task. */
    public static int boostMaxHit(ServerPlayerEntity player, LivingEntity target, int maxHit) {
        return SlayerService.boostMaxHit(player, target, maxHit);
    }
}
