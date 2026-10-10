package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Game-tick driver for the systems module: delayed tasks and per-player upkeep. */
public final class SysTicks {
    private record Task(long due, Runnable run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<BiConsumer<ServerPlayerEntity, Long>> PLAYER_TICKS = new ArrayList<>();
    private static final List<BiConsumer<MinecraftServer, Long>> SERVER_TICKS = new ArrayList<>();
    private static long lastTick = -1;

    private SysTicks() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = ActionManager.gameTick();
            if (tick == lastTick) {
                return;
            }
            lastTick = tick;
            List<Task> due = new ArrayList<>();
            TASKS.removeIf(t -> {
                if (t.due() <= tick) {
                    due.add(t);
                    return true;
                }
                return false;
            });
            for (Task t : due) {
                try {
                    t.run().run();
                } catch (RuntimeException e) {
                    com.runecraft.RuneCraft.LOGGER.error("Systems delayed task failed", e);
                }
            }
            for (BiConsumer<MinecraftServer, Long> s : SERVER_TICKS) {
                s.accept(server, tick);
            }
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                for (BiConsumer<ServerPlayerEntity, Long> c : PLAYER_TICKS) {
                    try {
                        c.accept(p, tick);
                    } catch (RuntimeException e) {
                        com.runecraft.RuneCraft.LOGGER.error("Systems player tick failed", e);
                    }
                }
            }
        });
    }

    /** Runs a task after a number of game ticks (0 = next tick). */
    public static void later(int gameTicks, Runnable run) {
        TASKS.add(new Task(ActionManager.gameTick() + Math.max(1, gameTicks), run));
    }

    public static void everyPlayer(BiConsumer<ServerPlayerEntity, Long> c) {
        PLAYER_TICKS.add(c);
    }

    public static void everyTick(BiConsumer<MinecraftServer, Long> c) {
        SERVER_TICKS.add(c);
    }

    public static long now() {
        return ActionManager.gameTick();
    }
}
