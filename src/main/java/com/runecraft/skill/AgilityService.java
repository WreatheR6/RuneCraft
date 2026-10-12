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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Obstacle courses: step on numbered obstacles in order; each gives its own OSRS XP and the last one
 * (which includes the lap bonus) completes a lap. Rooftop laps may give a Mark of grace.
 */
public final class AgilityService {
    /** Game tick each player last received a Mark of grace (3-minute cooldown; not persisted). */
    private static final Map<UUID, Long> LAST_MARK = new HashMap<>();

    private AgilityService() {
    }

    public static void step(ServerPlayerEntity player, AgilityCourse course, int step) {
        PlayerData data = Players.data(player);
        int level = data.skills().currentLevel(Skill.AGILITY);
        if (level < course.level()) {
            if (ActionManager.mcTick() - data.lastAgilityWarning > 60) {
                data.lastAgilityWarning = ActionManager.mcTick();
                Msg.game(player, "You need an Agility level of " + course.level() + " to attempt this course.");
            }
            return;
        }
        if (step >= course.obstacles()) {
            return;
        }
        int expected = data.agilityProgress(course);
        if (step == 0 && expected != 1) {
            data.setAgilityProgress(course, 1);
            XpService.add(player, Skill.AGILITY, course.obstacleXp(0));
            return;
        }
        if (step != expected) {
            return;
        }
        XpService.add(player, Skill.AGILITY, course.obstacleXp(step));
        if (step >= course.obstacles() - 1) {
            data.setAgilityProgress(course, 0);
            Msg.game(player, "You complete a lap of the " + course.displayName() + ".");
            long now = ActionManager.gameTick();
            Long last = LAST_MARK.get(player.getUuid());
            boolean cooledDown = last == null || now - last >= AgilityCourse.MARK_COOLDOWN_TICKS;
            if (cooledDown && player.getRandom().nextDouble() < course.markChance(level)) {
                LAST_MARK.put(player.getUuid(), now);
                Inv.give(player, "mark_of_grace", 1);
                CollectionLog.obtain(player, "mark_of_grace");
                Msg.red(player, "You find a Mark of grace!");
            }
        } else {
            data.setAgilityProgress(course, step + 1);
        }
    }
}
