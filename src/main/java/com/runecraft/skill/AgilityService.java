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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Obstacle courses: step on numbered obstacles in order; each gives its own OSRS XP and the last one
 * (which includes the lap bonus) completes a lap and counts it. Some obstacles can be failed (see
 * {@link AgilityCourse#failChance}): the player falls, takes damage and has to start the lap again.
 * Rooftop laps may give a Mark of grace. Courses with a quest or item requirement (Werewolf,
 * Penguin) check it; on the Werewolf course the Agility Trainer's stick, picked up at the stepping
 * stones, is handed back at the end of the lap for {@link AgilityCourse#STICK_XP}. Other lap rewards
 * (crystal shards, termites, Wilderness tickets) come from {@link #LAP_LISTENERS}.
 */
public final class AgilityService {
    /** Game tick each player last received a Mark of grace (3-minute cooldown; not persisted). */
    private static final Map<UUID, Long> LAST_MARK = new HashMap<>();
    /** Adjusts a lap's mark of grace chance (Kandarin diary Seers' bonus): (player, course, chance) -> chance. */
    public static MarkChance markChance = (p, course, chance) -> chance;
    /** Called after every completed lap (course rewards of the skilling module). */
    public static final List<LapListener> LAP_LISTENERS = new ArrayList<>();

    public interface MarkChance {
        double adjust(ServerPlayerEntity player, AgilityCourse course, double chance);
    }

    public interface LapListener {
        void lap(ServerPlayerEntity player, AgilityCourse course, int laps);
    }

    private AgilityService() {
    }

    /** Null if the player may use the course, else why not. */
    static String blocked(ServerPlayerEntity player, AgilityCourse course) {
        String quest = course.requiredQuest();
        if (quest != null) {
            boolean ok = course.questStartedEnough() ? com.runecraft.quest.QuestApi.stage(player, quest) > 0
                    : com.runecraft.quest.QuestApi.isComplete(player, quest);
            if (!ok) {
                com.runecraft.osrs.QuestInfo info = com.runecraft.osrs.content.ContentRegistry.quest(quest);
                String name = info == null ? quest : info.displayName();
                return "You need to " + (course.questStartedEnough() ? "have started " : "complete ") + name + " to use this course.";
            }
        }
        String item = course.requiredItem();
        if (item != null && !Inv.has(player, item, 1)) {
            return "The werewolves won't let you train here without a ring of charos.";
        }
        return null;
    }

    public static void step(ServerPlayerEntity player, AgilityCourse course, int step) {
        PlayerData data = Players.data(player);
        int level = data.skills().currentLevel(Skill.AGILITY);
        String blocked = level < course.level() ? "You need an Agility level of " + course.level() + " to attempt this course."
                : blocked(player, course);
        if (blocked != null) {
            if (ActionManager.mcTick() - data.lastAgilityWarning > 60) {
                data.lastAgilityWarning = ActionManager.mcTick();
                Msg.game(player, blocked);
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
            if (course == AgilityCourse.WEREWOLF && !Inv.has(player, AgilityCourse.WEREWOLF_STICK, 1)) {
                Inv.give(player, AgilityCourse.WEREWOLF_STICK, 1);
                Msg.game(player, "The Agility Trainer throws a stick, and you catch it. Bring it back to him at the end!");
            }
            return;
        }
        if (step != expected) {
            return;
        }
        if (player.getRandom().nextDouble() < course.failChance(step, level)) {
            data.setAgilityProgress(course, 0);
            int damage = 1 + player.getRandom().nextInt(Math.max(1, course.failDamage()));
            com.runecraft.combat.CombatHandler.applyDamage(null, player, damage);
            Msg.red(player, "You lose your footing and fall!");
            return;
        }
        XpService.add(player, Skill.AGILITY, course.obstacleXp(step));
        if (step >= course.obstacles() - 1) {
            data.setAgilityProgress(course, 0);
            int laps = data.addAgilityLap(course);
            Msg.game(player, "You complete a lap of the " + course.displayName() + ". Your lap count is: " + laps + ".");
            if (course == AgilityCourse.WEREWOLF && Inv.remove(player, AgilityCourse.WEREWOLF_STICK, 1) > 0) {
                XpService.add(player, Skill.AGILITY, AgilityCourse.STICK_XP);
                Msg.game(player, "You hand the stick to the Agility Trainer. \"Good doggy!\"");
            }
            long now = ActionManager.gameTick();
            Long last = LAST_MARK.get(player.getUuid());
            boolean cooledDown = last == null || now - last >= AgilityCourse.MARK_COOLDOWN_TICKS;
            if (cooledDown && player.getRandom().nextDouble() < markChance.adjust(player, course, course.markChance(level))) {
                LAST_MARK.put(player.getUuid(), now);
                Inv.give(player, "mark_of_grace", 1);
                CollectionLog.obtain(player, "mark_of_grace");
                Msg.red(player, "You find a Mark of grace!");
            }
            for (LapListener l : LAP_LISTENERS) {
                l.lap(player, course, laps);
            }
        } else {
            data.setAgilityProgress(course, step + 1);
        }
    }
}
