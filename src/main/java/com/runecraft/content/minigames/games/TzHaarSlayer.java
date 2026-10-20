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
 * made before the first wave. Taking it makes the task 1 to {@link SlayerService#tzhaarBossTaskKills} kills of that boss (one;
 * two from the master Combat Achievement tier, three from grandmaster: the player is offered 1 up to that many). Each kill is
 * a run of its own (the run ends at the kill) and takes one off the task, which completes with the last; every creature killed
 * in such a run gives its Slayer experience, the boss gives its own (25,250 / 101,890) and the pet is likelier. Dying,
 * dropping out in combat or leaving a run before its kill cancels the whole task (kills already made included) without
 * costing points or the streak. A TzHaar task the player did not take that way is never touched.
 */
public final class TzHaarSlayer {
    private static final String TASK = "tzhaar";
    /** Per game: the size (1-3) of the boss task taken for it, 0 when none (older saves hold 1 for their one-kill task). */
    private static final String KEY = "tzhaar_boss_task:";

    private TzHaarSlayer() {
    }

    private static String bossName(Minigame game) {
        return game == Minigame.INFERNO ? "TzKal-Zuk" : "TzTok-Jad";
    }

    private static String place(Minigame game) {
        return game == Minigame.INFERNO ? "Inferno" : "Fight Cave";
    }

    /** "TzTok-Jad" for one kill, "2 TzTok-Jad" for more. */
    private static String kills(int n, Minigame game) {
        return n == 1 ? bossName(game) : n + " " + bossName(game);
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

    /** One option per size the player may take: "[Kill TzTok-Jad]" with one allowed, "[Kill 1 TzTok-Jad] [Kill 2 TzTok-Jad]" with two. */
    private static void options(MutableComponent line, Minigame game, int allowed, String choice) {
        String risk = " Each kill is a " + place(game) + " run of its own; dying or leaving a run before its kill cancels the"
                + " whole task (no points lost).";
        String xp = " Every creature in the " + place(game) + " gives Slayer experience.";
        if (allowed <= 1) {
            line.append(option("Kill " + bossName(game), choice, "Your task becomes one " + bossName(game) + " kill." + xp + risk));
            return;
        }
        for (int n = 1; n <= allowed; n++) {
            if (n > 1) {
                line.append(Component.literal(" "));
            }
            line.append(option("Kill " + n + " " + bossName(game), choice + "_" + n, "Your task becomes " + n + " " + bossName(game)
                    + (n == 1 ? " kill." : " kills.") + xp + risk));
        }
    }

    /**
     * Offers to fight a TzHaar task as a TzTok-Jad (or TzKal-Zuk) task: clickable options running {@code /minigame tzhaar
     * task_jad|task_zuk} (with Combat Achievements, {@code task_jad_<n>} for each size up to the allowance). Nothing for a
     * player without a TzHaar task, or who already took one.
     */
    public static void offer(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!tzhaarTask(p) || onTask(player, Minigame.FIGHT_CAVES) || onTask(player, Minigame.INFERNO)) {
            return;
        }
        int allowed = SlayerService.tzhaarBossTaskKills(player);
        MutableComponent line = Component.literal("Your TzHaar task may be fought as a boss task instead: ").withStyle(ChatFormatting.GRAY);
        options(line, Minigame.FIGHT_CAVES, allowed, "task_jad");
        if (zukOffered(player)) {
            line.append(Component.literal(" "));
            options(line, Minigame.INFERNO, allowed, "task_zuk");
        }
        player.displayClientMessage(line, false);
    }

    /**
     * "/minigame tzhaar task_jad_&lt;n&gt;|task_zuk_&lt;n&gt;": takes the TzHaar task as {@code n} kills of that boss. False for
     * anything else (or a refused take).
     */
    public static boolean choose(ServerPlayer player, String choice) {
        Minigame game = choice.startsWith("task_jad_") ? Minigame.FIGHT_CAVES : choice.startsWith("task_zuk_") ? Minigame.INFERNO : null;
        if (game == null) {
            return false;
        }
        int n;
        try {
            n = Integer.parseInt(choice.substring("task_jad_".length()));
        } catch (NumberFormatException e) {
            return false;
        }
        return take(player, game, n);
    }

    /**
     * "/minigame tzhaar task_jad|task_zuk": takes the TzHaar task as this minigame's boss task, as many kills as the player's
     * Combat Achievements allow (outside a run or before wave 1).
     */
    public static boolean take(ServerPlayer player, Minigame game) {
        return take(player, game, SlayerService.tzhaarBossTaskKills(player));
    }

    /** As {@link #take(ServerPlayer, Minigame)}, for {@code kills} kills: 1 up to what the player's Combat Achievements allow. */
    public static boolean take(ServerPlayer player, Minigame game, int kills) {
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
        int allowed = SlayerService.tzhaarBossTaskKills(player);
        if (!TzHaarWaves.slayerBossTaskSize(kills, allowed)) {
            Msg.game(player, allowed == 1 ? "Your Slayer master only offers one " + bossName(game) + " per task."
                    : "Your Slayer master offers 1 to " + allowed + " " + bossName(game) + " per task.");
            return false;
        }
        if (com.runecraft.content.minigames.MinigameManager.session(player) instanceof TzHaarSession run && run.wave() >= 1) {
            Msg.game(player, "You can only take it before the fight starts.");
            return false;
        }
        MinigameData data = MinigameData.of(player);
        data.setStat(player.getUUID(), KEY + Minigame.FIGHT_CAVES.id(), 0);
        data.setStat(player.getUUID(), KEY + Minigame.INFERNO.id(), 0);
        data.setStat(player.getUUID(), KEY + game.id(), kills);
        p.amount = kills;
        p.remaining = kills;
        Msg.game(player, "Your TzHaar Slayer task is now to kill " + kills(kills, game) + "." + (kills > 1 ? " Each kill is a "
                + place(game) + " run of its own." : "") + " Dying or leaving the " + place(game) + " before a kill cancels the"
                + " whole task.");
        SlayerNet.sync(player);
        return true;
    }

    /**
     * On entry: a task taken as this minigame's boss task counts here; an ordinary TzHaar task is left alone, and offered when
     * {@code canTake} (a fresh run, not one resumed past its first wave).
     */
    public static void enter(ServerPlayer player, Minigame game, boolean canTake) {
        if (onTask(player, game)) {
            int left = SlayerService.profile(player).remaining;
            Msg.game(player, "Your Slayer task is to kill " + bossName(game) + (left > 1 ? " (" + left + " more)" : "")
                    + ": every creature in here gives Slayer experience.");
        } else if (canTake) {
            offer(player);
        }
    }

    /**
     * Whether this run counts as the player's TzTok-Jad / TzKal-Zuk task: the flag of a TzHaar task the player took as this
     * boss's task, set to that task's size. The task it made must still be the current one (the same size: a later ordinary
     * TzHaar task, 90 kills or more, never is), so a flag left behind by a task skipped or blocked at a master is ignored.
     */
    public static boolean onTask(ServerPlayer player, Minigame game) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!tzhaarTask(p)) {
            return false;
        }
        int size = MinigameData.of(player).stat(player.getUUID(), KEY + game.id());
        return size > 0 && size == p.amount;
    }

    /** Whether the player's TzHaar task is taken as a TzTok-Jad or TzKal-Zuk task (then the city's TzHaar don't count). */
    public static boolean bossTaskTaken(ServerPlayer player) {
        return onTask(player, Minigame.FIGHT_CAVES) || onTask(player, Minigame.INFERNO);
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

    /**
     * The boss died (the run ends at the kill): one kill off the task, which completes with its streak and points on the last.
     * The flag stays for the next run until then.
     */
    public static void complete(ServerPlayer player, Minigame game) {
        if (!onTask(player, game)) {
            return;
        }
        SlayerState.Profile p = SlayerService.profile(player);
        p.remaining = TzHaarWaves.slayerBossTaskLeft(p.remaining);
        if (p.remaining > 0) {
            Msg.game(player, "You're assigned to kill " + bossName(game) + "; only " + p.remaining + " more to go. Each is a "
                    + place(game) + " run of its own.");
            SlayerNet.sync(player);
            return;
        }
        MinigameData.of(player).setStat(player.getUUID(), KEY + game.id(), 0);
        SlayerService.completeTask(player);
    }

    /**
     * Dying or leaving a run before its kill cancels the whole task, kills made in earlier runs included (no points spent,
     * the streak kept; OSRS Wiki "Slayer task/TzHaar": "If the player dies or leaves, the task is cancelled").
     */
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
