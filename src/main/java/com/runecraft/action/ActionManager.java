package com.runecraft.action;

import com.runecraft.combat.PrayerService;
import com.runecraft.player.PlayerEvents;
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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

    private record Running(Action action, Vec3 origin, boolean allowMovement) {
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

    public static void start(ServerPlayer player, Action action) {
        start(player, action, false);
    }

    public static void start(ServerPlayer player, Action action, boolean allowMovement) {
        cancel(player);
        ACTIONS.put(player.getUUID(), new Running(action, player.position(), allowMovement));
    }

    public static void cancel(ServerPlayer player) {
        Running old = ACTIONS.remove(player.getUUID());
        if (old != null) {
            old.action().stop(player);
        }
    }

    /**
     * Self-test hook ({@code com.runecraft.selftest}): advances a player's current action by one game tick right now,
     * for test players that are not in the server's player list. Returns whether the action is still running.
     */
    public static boolean tickNow(ServerPlayer player) {
        Running running = ACTIONS.get(player.getUUID());
        if (running == null) {
            return false;
        }
        if (!running.action().tick(player, gameTick)) {
            ACTIONS.remove(player.getUUID());
            running.action().stop(player);
            return false;
        }
        return true;
    }

    public static boolean isBusy(ServerPlayer player) {
        return ACTIONS.containsKey(player.getUUID());
    }

    private static void onServerTick(MinecraftServer server) {
        mcTicks++;
        PlayerEvents.everyMcTick(server, mcTicks);
        if (mcTicks % MC_TICKS_PER_GAME_TICK != 0) {
            return;
        }
        gameTick++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Running running = ACTIONS.get(player.getUUID());
            if (running != null) {
                boolean moved = !running.allowMovement() && player.position().distanceToSqr(running.origin()) > 1.0;
                if (moved || player.isDeadOrDying() || !running.action().tick(player, gameTick)) {
                    ACTIONS.remove(player.getUUID());
                    running.action().stop(player);
                }
            }
            PrayerService.gameTick(player, gameTick);
            PlayerEvents.gameTick(player, gameTick);
        }
        ACTIONS.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        com.runecraft.combat.CombatZones.gameTick(server, gameTick);
    }
}
