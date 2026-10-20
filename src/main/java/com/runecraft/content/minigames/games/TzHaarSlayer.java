package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.slayer.SlayerNet;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * TzHaar Slayer tasks fought as TzTok-Jad or TzKal-Zuk tasks (OSRS Wiki "Slayer task/TzHaar"): a player with a TzHaar task
 * who enters the Fight Cave (or the Inferno, once Zuk has been killed off-task) has it turned into a one-kill task for that
 * minigame's boss. Every creature killed in the run then gives its Slayer experience, the boss its own (25,250 / 101,890),
 * the pet chance improves, and dying or leaving cancels the task without costing points or the streak.
 * // approx: in OSRS the master offers the choice when assigning; here entering the minigame with the task takes it.
 */
public final class TzHaarSlayer {
    private static final String TASK = "tzhaar";
    private static final String KEY = "tzhaar_boss_task:";

    private TzHaarSlayer() {
    }

    private static String bossName(Minigame game) {
        return game == Minigame.INFERNO ? "TzKal-Zuk" : "TzTok-Jad";
    }

    /** Turns a TzHaar task into this minigame's boss task on entry. */
    public static void enter(ServerPlayer player, Minigame game) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!p.hasTask() || !TASK.equals(p.task)) {
            return;
        }
        MinigameData data = MinigameData.of(player);
        if (game == Minigame.INFERNO && data.stat(player.getUUID(), "kc:" + Minigame.INFERNO.id()) < 1) {
            Msg.game(player, "Your Slayer master only offers TzKal-Zuk as a task once you have defeated him.");
            return;
        }
        p.remaining = 1;
        p.amount = 1;
        data.setStat(player.getUUID(), KEY + game.id(), 1);
        Msg.game(player, "Your TzHaar Slayer task is now to kill " + bossName(game) + ".");
        SlayerNet.sync(player);
    }

    /** Whether this run counts as the player's TzTok-Jad / TzKal-Zuk task. */
    public static boolean onTask(ServerPlayer player, Minigame game) {
        SlayerState.Profile p = SlayerService.profile(player);
        return p.hasTask() && TASK.equals(p.task) && MinigameData.of(player).stat(player.getUUID(), KEY + game.id()) > 0;
    }

    /** Slayer experience for a creature killed in the run. */
    public static void kill(ServerPlayer player, Minigame game, String creatureId, LivingEntity entity) {
        if (!onTask(player, game)) {
            return;
        }
        double xp = creatureId.equals(TzHaarWaves.JAD) ? TzHaarWaves.JAD_SLAYER_XP
                : creatureId.equals(TzHaarWaves.ZUK) ? TzHaarWaves.ZUK_SLAYER_XP
                : com.runecraft.osrs.data.MonsterTraits.slayerXp(creatureId, Math.round(entity.getMaxHealth()));
        XpService.add(player, Skill.SLAYER, xp);
    }

    /** The boss died: the task is done. */
    public static void complete(ServerPlayer player, Minigame game) {
        if (!onTask(player, game)) {
            return;
        }
        MinigameData.of(player).setStat(player.getUUID(), KEY + game.id(), 0);
        SlayerService.completeTask(player);
    }

    /** Dying or leaving the run cancels the task (no points spent, the streak kept). */
    public static void leave(ServerPlayer player, Minigame game) {
        if (!onTask(player, game)) {
            return;
        }
        SlayerState.Profile p = SlayerService.profile(player);
        p.task = null;
        p.remaining = 0;
        MinigameData.of(player).setStat(player.getUUID(), KEY + game.id(), 0);
        Msg.game(player, "You no longer have a slayer task as you left the " + (game == Minigame.INFERNO ? "Inferno." : "fight cave."));
        SlayerNet.sync(player);
    }

    /** A pet from a TzHaar boss, with the server announcement. */
    public static void pet(ServerPlayer player, String pet) {
        Inv.give(player, pet, 1);
        com.runecraft.collection.CollectionLog.obtain(player, pet);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
    }
}
