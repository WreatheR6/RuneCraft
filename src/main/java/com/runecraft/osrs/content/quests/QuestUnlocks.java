package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;

import java.util.function.Predicate;

/**
 * Quest unlocks checked outside the quest scripts: prayers that a quest teaches. Each method returns
 * the message to show when the player may not use the unlock, or null when they may.
 */
public final class QuestUnlocks {
    private QuestUnlocks() {
    }

    /**
     * Chivalry and Piety are taught at the Knight Waves training ground, open after King's Ransom
     * (and need 65 / 70 Defence). Rigour and Augury need 70 Defence (their prayer scrolls are
     * checked by PrayerService).
     */
    public static String prayer(Prayer prayer, SkillSet skills, Predicate<String> questDone) {
        int defence = switch (prayer) {
            case CHIVALRY -> 65;
            case PIETY, RIGOUR, AUGURY -> 70;
            default -> 0;
        };
        if (prayer == Prayer.CHIVALRY || prayer == Prayer.PIETY) {
            if (!questDone.test("kings_ransom")) {
                return "You need to complete King's Ransom and the Knight Waves training to use " + prayer.displayName() + ".";
            }
        }
        if (defence > 0 && skills.level(Skill.DEFENCE) < defence) {
            return "You need a Defence level of " + defence + " to use " + prayer.displayName() + ".";
        }
        return null;
    }
}
