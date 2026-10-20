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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * TzHaar Slayer tasks fought as TzTok-Jad or TzKal-Zuk tasks (OSRS Wiki "Slayer task/TzHaar", "TzTok-Jad"): a player assigned
 * TzHaar (the "Hot stuff" unlock) may take TzTok-Jad instead, or TzKal-Zuk once they have killed him. The choice is offered by
 * the Slayer master on assignment, by TzHaar-Mej-Jal and TzHaar-Ket-Keh, and on entering the cave with the task, and must be
 * made before the first wave. Taking it makes the task one kill of that boss: every creature killed in the run gives its Slayer
 * experience, the boss gives its own (25,250 / 101,890) and the pet is likelier. Dying, dropping out in combat or leaving
 * cancels the task without costing points or the streak. A TzHaar task the player did not take that way is never touched.
 * // approx: Master / Grandmaster Combat Achievements let OSRS players take 2 or 3 Jads per task; here it is always one.
 */
public final class TzHaarSlayer {
    private static final String TASK = "tzhaar";
    private static final String KEY = "tzhaar_boss_task:";

    private TzHaarSlayer() {
    }

    private static String bossName(Minigame game) {
        return game == Minigame.INFERNO ? "TzKal-Zuk" : "TzTok-Jad";
    }

    private static boolean tzhaarTask(SlayerState.Profile p) {
        return p.hasTask() && TASK.equals(p.task);
    }

    /** Whether the player may take TzKal-Zuk as a task: only once they have defeated him. */
    private static boolean zukOffered(ServerPlayer player) {
        return MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.INFERNO.id()) >= 1;
    }

    private static MutableComponent option(String label, String choice, String hover) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minigame tzhaar " + choice))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    /**
     * Offers to fight a TzHaar task as a TzTok-Jad (or TzKal-Zuk) task: clickable options running {@code /minigame tzhaar
     * task_jad|task_zuk}. Nothing for a player without a TzHaar task, or who already took one.
     */
    public static void offer(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!tzhaarTask(p) || onTask(player, Minigame.FIGHT_CAVES) || onTask(player, Minigame.INFERNO)) {
            return;
        }
        String risk = " Dying or leaving before the kill cancels the task (no points lost).";
        MutableComponent line = Component.literal("Your TzHaar task may be fought as a boss task instead: ").withStyle(ChatFormatting.GRAY)
                .append(option("Kill TzTok-Jad", "task_jad", "Your task becomes one TzTok-Jad kill; the whole Fight Cave run gives Slayer"
                        + " experience." + risk));
        if (zukOffered(player)) {
            line.append(Component.literal(" ")).append(option("Kill TzKal-Zuk", "task_zuk", "Your task becomes one TzKal-Zuk kill; the"
                    + " whole Inferno run gives Slayer experience." + risk));
        }
        player.displayClientMessage(line, false);
    }

    /** "/minigame tzhaar task_jad|task_zuk": takes the TzHaar task as this minigame's boss task (outside a run or before wave 1). */
    public static boolean take(ServerPlayer player, Minigame game) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!tzhaarTask(p)) {
            Msg.game(player, "You need a TzHaar Slayer task to do that.");
            return false;
        }
        if (onTask(player, game)) {
            Msg.game(player, "Your Slayer task is already to kill " + bossName(game) + ".");
            return false;
        }
        if (onTask(player, Minigame.FIGHT_CAVES) || onTask(player, Minigame.INFERNO)) {
            Msg.game(player, "You have already taken your TzHaar task as a boss task.");
            return false;
        }
        if (game == Minigame.INFERNO && !zukOffered(player)) {
            Msg.game(player, "Your Slayer master only offers TzKal-Zuk as a task once you have defeated him.");
            return false;
        }
        if (com.runecraft.content.minigames.MinigameManager.session(player) instanceof TzHaarSession run && run.wave() >= 1) {
            Msg.game(player, "You can only take it before the fight starts.");
            return false;
        }
        MinigameData data = MinigameData.of(player);
        data.setStat(player.getUUID(), KEY + Minigame.FIGHT_CAVES.id(), 0);
        data.setStat(player.getUUID(), KEY + Minigame.INFERNO.id(), 0);
        data.setStat(player.getUUID(), KEY + game.id(), 1);
        p.amount = 1;
        p.remaining = 1;
        Msg.game(player, "Your TzHaar Slayer task is now to kill " + bossName(game) + ". Dying or leaving the "
                + (game == Minigame.INFERNO ? "Inferno" : "Fight Cave") + " before the kill cancels it.");
        SlayerNet.sync(player);
        return true;
    }

    /**
     * On entry: a task taken as this minigame's boss task counts here; an ordinary TzHaar task is left alone, and offered when
     * {@code canTake} (a fresh run, not one resumed past its first wave).
     */
    public static void enter(ServerPlayer player, Minigame game, boolean canTake) {
        if (onTask(player, game)) {
            Msg.game(player, "Your Slayer task is to kill " + bossName(game) + ": every creature in here gives Slayer experience.");
        } else if (canTake) {
            offer(player);
        }
    }

    /**
     * Whether this run counts as the player's TzTok-Jad / TzKal-Zuk task: a TzHaar task the player took as this boss's task
     * (the flag, and the one-kill task it made; a later TzHaar task of 90+ kills is not one).
     */
    public static boolean onTask(ServerPlayer player, Minigame game) {
        SlayerState.Profile p = SlayerService.profile(player);
        return tzhaarTask(p) && p.amount == 1 && MinigameData.of(player).stat(player.getUUID(), KEY + game.id()) > 0;
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
