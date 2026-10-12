package com.runecraft.action;

import com.runecraft.combat.PrayerService;
import com.runecraft.player.PlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the OSRS game tick. Every 12 Minecraft ticks (0.6s) each player's current action is
 * advanced, prayer drains and stats regenerate. Moving away from where an action started
 * cancels it, like clicking elsewhere in-game.
 */
public final class ActionManager {
    public static final int MC_TICKS_PER_GAME_TICK = 12;

    private static final Map<UUID, Running> ACTIONS = new HashMap<>();
    private static long mcTicks;
    private static long gameTick;

    private record Running(Action action, Vec3d origin, boolean allowMovement) {
    }

    private ActionManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ActionManager::onServerTick);
    }

    public static long gameTick() {
        return gameTick;
    }

    /** Current Minecraft server tick count (for cooldowns). */
    public static long mcTick() {
        return mcTicks;
    }

    public static void start(ServerPlayerEntity player, Action action) {
        start(player, action, false);
    }

    public static void start(ServerPlayerEntity player, Action action, boolean allowMovement) {
        cancel(player);
        ACTIONS.put(player.getUuid(), new Running(action, player.getPos(), allowMovement));
    }

    public static void cancel(ServerPlayerEntity player) {
        Running old = ACTIONS.remove(player.getUuid());
        if (old != null) {
            old.action().stop(player);
        }
    }

    public static boolean isBusy(ServerPlayerEntity player) {
        return ACTIONS.containsKey(player.getUuid());
    }

    private static void onServerTick(MinecraftServer server) {
        mcTicks++;
        PlayerEvents.everyMcTick(server, mcTicks);
        if (mcTicks % MC_TICKS_PER_GAME_TICK != 0) {
            return;
        }
        gameTick++;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Running running = ACTIONS.get(player.getUuid());
            if (running != null) {
                boolean moved = !running.allowMovement() && player.getPos().squaredDistanceTo(running.origin()) > 1.0;
                if (moved || player.isDead() || !running.action().tick(player, gameTick)) {
                    ACTIONS.remove(player.getUuid());
                    running.action().stop(player);
                }
            }
            PrayerService.gameTick(player, gameTick);
            PlayerEvents.gameTick(player, gameTick);
        }
        ACTIONS.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) == null);
        com.runecraft.combat.CombatZones.gameTick(server, gameTick);
    }
}
