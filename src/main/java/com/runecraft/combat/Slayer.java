package com.runecraft.combat;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.SlayerMaster;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Locale;

/** Slayer tasks from masters (by combat level). Kills on task give Slayer XP equal to the monster's hitpoints. */
public final class Slayer {
    private Slayer() {
    }

    public static void talkToMaster(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        SlayerMaster master = SlayerMaster.bestFor(data.skills().combatLevel());
        if (data.slayerTask() != null && data.slayerRemaining() > 0) {
            Msg.game(player, master.displayName() + ": You're still hunting " + plural(data.slayerTask()) + "; you have "
                    + data.slayerRemaining() + " to go. Come back when you've finished your task.");
            return;
        }
        List<Monster> options = master.assignments();
        Monster task = options.get(player.getRandom().nextInt(options.size()));
        int amount = master.minAmount() + player.getRandom().nextInt(master.maxAmount() - master.minAmount() + 1);
        data.setSlayerTask(task, amount);
        Msg.game(player, master.displayName() + ": Your new task is to kill " + amount + " " + plural(task) + ".");
        ServerNetworking.sync(player);
    }

    public static void checkTask(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        if (data.slayerTask() == null || data.slayerRemaining() <= 0) {
            Msg.game(player, "You need something new to hunt. Visit a Slayer master.");
        } else {
            Msg.game(player, "You're assigned to kill " + plural(data.slayerTask()) + "; only " + data.slayerRemaining() + " more to go.");
        }
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity entity, MonsterStats stats) {
        PlayerData data = Players.data(player);
        Monster task = data.slayerTask();
        if (task == null || data.slayerRemaining() <= 0 || stats.monster() == null) {
            return;
        }
        if (!stats.monster().displayName().equals(task.displayName())) {
            return;
        }
        XpService.add(player, Skill.SLAYER, stats.hitpoints());
        data.setSlayerTask(task, data.slayerRemaining() - 1);
        if (data.slayerRemaining() == 0) {
            data.setSlayerStreak(data.slayerStreak() + 1);
            Msg.red(player, "You have completed your task! You've completed " + data.slayerStreak()
                    + " tasks in a row. Return to a Slayer master.");
        }
        ServerNetworking.sync(player);
    }

    /** No slayer helmet yet: task monsters take normal damage. Hook kept for future gear. */
    public static int boostMaxHit(ServerPlayerEntity player, LivingEntity target, int maxHit) {
        return maxHit;
    }

    private static String plural(Monster m) {
        String n = m.displayName().toLowerCase(Locale.ROOT);
        if (n.endsWith("man")) {
            return n.substring(0, n.length() - 3) + "men";
        }
        return n.endsWith("s") ? n : n + "s";
    }
}
