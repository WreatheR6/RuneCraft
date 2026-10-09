package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;

/** Obstacle courses: step on numbered obstacles in order; the last obstacle completes a lap. */
public final class AgilityService {
    private AgilityService() {
    }

    public static void step(ServerPlayerEntity player, AgilityCourse course, int step) {
        PlayerData data = Players.data(player);
        if (data.skills().currentLevel(Skill.AGILITY) < course.level()) {
            if (ActionManager.mcTick() - data.lastAgilityWarning > 60) {
                data.lastAgilityWarning = ActionManager.mcTick();
                Msg.game(player, "You need an Agility level of " + course.level() + " to attempt this course.");
            }
            return;
        }
        int expected = data.agilityProgress(course);
        if (step == 0 && expected != 1) {
            data.setAgilityProgress(course, 1);
            XpService.add(player, Skill.AGILITY, course.obstacleXp());
            return;
        }
        if (step != expected) {
            return;
        }
        XpService.add(player, Skill.AGILITY, course.obstacleXp());
        if (step >= course.obstacles() - 1) {
            data.setAgilityProgress(course, 0);
            XpService.add(player, Skill.AGILITY, course.lapBonusXp());
            Msg.game(player, "You complete a lap of the " + course.displayName() + ".");
            if (course.rooftop() && player.getRandom().nextInt(3) == 0) {
                Inv.give(player, "mark_of_grace", 1);
                CollectionLog.obtain(player, "mark_of_grace");
                Msg.red(player, "You find a Mark of grace!");
            }
        } else {
            data.setAgilityProgress(course, step + 1);
        }
    }
}
