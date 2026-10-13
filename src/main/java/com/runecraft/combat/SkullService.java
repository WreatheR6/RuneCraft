package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.PvpSkull;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft side of the PvP skull ({@link PvpSkull}): attacking a player unprovoked in the Wilderness skulls the
 * attacker, skulled players join the {@link PvpSkull#TEAM} scoreboard team (its prefix shows a skull before their name
 * above their head and in the player list; the HUD shows the player's own skull), a skulled death keeps no items
 * (see {@link PrayerEffectsService}) and dying removes the skull. The Emblem Trader skulls on request. Skull timers
 * live in memory: a server restart clears them.
 */
public final class SkullService {
    private static final PvpSkull SKULLS = new PvpSkull();
    /** Players who asked the Emblem Trader for a skull: world time of the question (confirm by talking again). */
    private static final Map<UUID, Long> EMBLEM_ASKED = new HashMap<>();

    private SkullService() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) {
                return;
            }
            for (UUID id : SKULLS.expire(ActionManager.gameTick())) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p != null) {
                    removeFromTeam(p);
                    Msg.game(p, "Your PK skull has faded.");
                }
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                SKULLS.clear(newPlayer.getUuid());
                removeFromTeam(newPlayer);
            }
        });
        // The scoreboard team is saved with the world but the timers are not: drop stale skulls on join.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (!SKULLS.isSkulled(p.getUuid(), ActionManager.gameTick())) {
                removeFromTeam(p);
            }
        });
    }

    /** A player attacked another player (any style, hit or miss). */
    public static void onPlayerAttack(ServerPlayerEntity attacker, ServerPlayerEntity victim) {
        if (attacker == victim || !Gielinor.is(attacker)) {
            return;
        }
        long now = ActionManager.gameTick();
        boolean wasSkulled = SKULLS.isSkulled(attacker.getUuid(), now);
        if (SKULLS.onAttack(attacker.getUuid(), victim.getUuid(), now, Gielinor.wildernessLevel(attacker) > 0)) {
            addToTeam(attacker);
            if (!wasSkulled) {
                Msg.red(attacker, "You have been skulled: you will lose all your items if you die.");
            }
        }
    }

    public static boolean isSkulled(ServerPlayerEntity player) {
        return SKULLS.isSkulled(player.getUuid(), ActionManager.gameTick());
    }

    /** Items the player keeps on death: 0 when skulled, otherwise 3; Protect Item adds one. */
    public static int itemsKept(ServerPlayerEntity player, boolean protectItem) {
        return SKULLS.itemsKept(player.getUuid(), ActionManager.gameTick(), protectItem);
    }

    /** The Emblem Trader's "Skull" option: the first talk asks, talking again within 10 seconds confirms. */
    public static void emblemTrader(ServerPlayerEntity player, String name) {
        long time = player.getWorld().getTime();
        Long asked = EMBLEM_ASKED.remove(player.getUuid());
        if (asked == null || time - asked > 200) {
            EMBLEM_ASKED.put(player.getUuid(), time);
            long left = SKULLS.ticksLeft(player.getUuid(), ActionManager.gameTick());
            QuestApi.say(player, name, left > 0
                    ? "You're already skulled, " + PvpSkull.minutes(left) + " minutes left. Talk to me again to make it last longer."
                    : "A PK skull? You'll lose everything if you die in the Wilderness. Talk to me again to confirm.");
            return;
        }
        SKULLS.skull(player.getUuid(), ActionManager.gameTick(), PvpSkull.EMBLEM_TRADER_TICKS);
        addToTeam(player);
        QuestApi.say(player, name, "There you go. Try not to die out there.");
        Msg.red(player, "You are now skulled.");
    }

    private static Team team(MinecraftServer server) {
        ServerScoreboard board = server.getScoreboard();
        Team team = board.getTeam(PvpSkull.TEAM);
        if (team == null) {
            team = board.addTeam(PvpSkull.TEAM);
            team.setPrefix(Text.literal("☠ ").formatted(Formatting.RED));
        }
        return team;
    }

    private static void addToTeam(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Team team = team(server);
        String name = player.getGameProfile().getName();
        if (server.getScoreboard().getPlayerTeam(name) != team) {
            server.getScoreboard().addPlayerToTeam(name, team);
        }
    }

    private static void removeFromTeam(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerScoreboard board = server.getScoreboard();
        Team team = board.getTeam(PvpSkull.TEAM);
        String name = player.getGameProfile().getName();
        if (team != null && board.getPlayerTeam(name) == team) {
            board.removePlayerFromTeam(name, team);
        }
    }
}
