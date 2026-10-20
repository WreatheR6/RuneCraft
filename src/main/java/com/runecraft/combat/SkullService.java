package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.PvpSkull;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerPlayerEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft side of the PvP skull ({@link PvpSkull}): attacking a player unprovoked in the Wilderness skulls the
 * attacker, skulled players join the {@link PvpSkull#TEAM} scoreboard team (its prefix shows a skull before their name
 * above their head and in the player list; the HUD shows the player's own skull), a skulled death keeps no items
 * (see {@link PrayerEffectsService}) and dying removes the skull. The Emblem Trader skulls on request. Skull timers
 * live in memory: a server restart clears them.
 *
 * <p>The TzHaar Fight Pit champion's red skull uses the {@link PvpSkull#CHAMPION_TEAM} team instead: it is shown to others
 * the same way but is not a PvP skull (items kept on death are unaffected). A Wilderness skull takes precedence while both
 * apply. The minigames module decides when a champion gains or loses it ({@link #setChampionSkull}).
 */
public final class SkullService {
    private static final PvpSkull SKULLS = new PvpSkull();
    /** Players who asked the Emblem Trader for a skull: world time of the question (confirm by talking again). */
    private static final Map<UUID, Long> EMBLEM_ASKED = new HashMap<>();
    /** Players showing the Fight Pit champion's red skull. */
    private static final java.util.Set<UUID> CHAMPIONS = new java.util.HashSet<>();

    private SkullService() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            // Keep each online player's skull ticks in SysState so the timer survives logouts and restarts.
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                com.runecraft.content.systems.SysState.of(p).skullTicks = (int) SKULLS.ticksLeft(p.getUUID(), ActionManager.gameTick());
            }
            for (UUID id : SKULLS.expire(ActionManager.gameTick())) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    removeFromTeam(p);
                    Msg.game(p, "Your PK skull has faded.");
                    showChampion(p);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer p = handler.getPlayer();
            com.runecraft.content.systems.SysState.of(p).skullTicks = (int) SKULLS.ticksLeft(p.getUUID(), ActionManager.gameTick());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                com.runecraft.content.systems.SysState.of(newPlayer).skullTicks = 0;
                SKULLS.clear(newPlayer.getUUID());
                removeFromTeam(newPlayer);
                showChampion(newPlayer);
            }
        });
        // The skull's ticks left are saved in SysState: restore the timer on join, drop stale scoreboard skulls.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            int saved = com.runecraft.content.systems.SysState.of(p).skullTicks;
            if (saved > 0 && !SKULLS.isSkulled(p.getUUID(), ActionManager.gameTick())) {
                SKULLS.skull(p.getUUID(), ActionManager.gameTick(), saved);
                addToTeam(p);
            }
            if (!SKULLS.isSkulled(p.getUUID(), ActionManager.gameTick())) {
                removeFromTeam(p);
            }
            if (!CHAMPIONS.contains(p.getUUID())) {
                removeFromTeam(p, PvpSkull.CHAMPION_TEAM);
            }
        });
    }

    /** A player attacked another player (any style, hit or miss). */
    public static void onPlayerAttack(ServerPlayer attacker, ServerPlayer victim) {
        if (attacker == victim || !Gielinor.is(attacker)) {
            return;
        }
        long now = ActionManager.gameTick();
        boolean wasSkulled = SKULLS.isSkulled(attacker.getUUID(), now);
        if (SKULLS.onAttack(attacker.getUUID(), victim.getUUID(), now, Gielinor.wildernessLevel(attacker) > 0)) {
            addToTeam(attacker);
            if (!wasSkulled) {
                Msg.red(attacker, "You have been skulled: you will lose all your items if you die.");
            }
        }
    }

    public static boolean isSkulled(ServerPlayer player) {
        return SKULLS.isSkulled(player.getUUID(), ActionManager.gameTick());
    }

    /**
     * Skulls a player for at least {@code ticks} more game ticks (an equipped amulet of avarice keeps its wearer skulled; the
     * skull then fades like a PK skull once it is taken off). Never shortens a longer skull.
     */
    public static void skull(ServerPlayer player, int ticks) {
        long now = ActionManager.gameTick();
        if (SKULLS.ticksLeft(player.getUUID(), now) >= ticks) {
            return;
        }
        boolean was = SKULLS.isSkulled(player.getUUID(), now);
        SKULLS.skull(player.getUUID(), now, ticks);
        addToTeam(player);
        if (!was) {
            Msg.red(player, "You are now skulled.");
        }
    }

    /** Items the player keeps on death: 0 when skulled, otherwise 3; Protect Item adds one. */
    public static int itemsKept(ServerPlayer player, boolean protectItem) {
        return SKULLS.itemsKept(player.getUUID(), ActionManager.gameTick(), protectItem);
    }

    /** The Emblem Trader's "Skull" option: the first talk asks, talking again within 10 seconds confirms. */
    public static void emblemTrader(ServerPlayer player, String name) {
        long time = player.level().getGameTime();
        Long asked = EMBLEM_ASKED.remove(player.getUUID());
        if (asked == null || time - asked > 200) {
            EMBLEM_ASKED.put(player.getUUID(), time);
            long left = SKULLS.ticksLeft(player.getUUID(), ActionManager.gameTick());
            QuestApi.say(player, name, left > 0
                    ? "You're already skulled, " + PvpSkull.minutes(left) + " minutes left. Talk to me again to make it last longer."
                    : "A PK skull? You'll lose everything if you die in the Wilderness. Talk to me again to confirm.");
            return;
        }
        SKULLS.skull(player.getUUID(), ActionManager.gameTick(), PvpSkull.EMBLEM_TRADER_TICKS);
        addToTeam(player);
        QuestApi.say(player, name, "There you go. Try not to die out there.");
        Msg.red(player, "You are now skulled.");
    }

    /**
     * Shows or hides the TzHaar Fight Pit champion's red skull above a player's head. It doesn't count as a PvP skull, and a
     * Wilderness skull is shown instead while the player has one.
     */
    public static void setChampionSkull(ServerPlayer player, boolean on) {
        if (on) {
            CHAMPIONS.add(player.getUUID());
            showChampion(player);
        } else {
            CHAMPIONS.remove(player.getUUID());
            removeFromTeam(player, PvpSkull.CHAMPION_TEAM);
        }
    }

    public static boolean hasChampionSkull(ServerPlayer player) {
        return CHAMPIONS.contains(player.getUUID());
    }

    private static void showChampion(ServerPlayer player) {
        if (CHAMPIONS.contains(player.getUUID()) && !isSkulled(player)) {
            addToTeam(player, PvpSkull.CHAMPION_TEAM);
        }
    }

    private static PlayerTeam team(MinecraftServer server, String name) {
        ServerScoreboard board = server.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(name);
        if (team == null) {
            team = board.addPlayerTeam(name);
            if (name.equals(PvpSkull.CHAMPION_TEAM)) {
                team.setPlayerPrefix(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED));
                team.setColor(ChatFormatting.DARK_RED);
            } else {
                team.setPlayerPrefix(Component.literal("☠ ").withStyle(ChatFormatting.RED));
            }
        }
        return team;
    }

    private static void addToTeam(ServerPlayer player) {
        addToTeam(player, PvpSkull.TEAM);
    }

    private static void addToTeam(ServerPlayer player, String teamName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlayerTeam team = team(server, teamName);
        String name = player.getGameProfile().getName();
        if (server.getScoreboard().getPlayersTeam(name) != team) {
            server.getScoreboard().addPlayerToTeam(name, team);
        }
    }

    private static void removeFromTeam(ServerPlayer player) {
        removeFromTeam(player, PvpSkull.TEAM);
    }

    private static void removeFromTeam(ServerPlayer player, String teamName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerScoreboard board = server.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(teamName);
        String name = player.getGameProfile().getName();
        if (team != null && board.getPlayersTeam(name) == team) {
            board.removePlayerFromTeam(name, team);
        }
    }
}
