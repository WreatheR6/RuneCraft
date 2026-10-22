package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quest boss a player loses to (or walks away from) vanishes a minute after the fight is left (BossLair.QUEST_LEASH), so it
 * must be possible to raise it again: from its summoning stone (an arena at its place), or from a step that calls it up and
 * still applies after a failed fight. A step that calls the boss up and switches itself off with the flag its kill needs runs
 * only once, and a boss without a place has no stone: the player would be stuck.
 */
class QuestBossRetryTest {
    private static final QuestBook BOOK = QuestBook.get();

    @Test
    void everyQuestBossCanBeRaisedAgainAfterALostFight() {
        List<String> stuck = new ArrayList<>();
        for (QuestSpec q : BOOK.quests) {
            for (Trigger raise : q.triggers()) {
                // a step that doesn't switch itself off by the flags it sets (re-solving Misthalin Mystery's mirror) raises it again
                if (raise.spawn == null || raise.setFlags.isEmpty() || raise.unlessFlags.stream().noneMatch(raise.setFlags::contains)) {
                    continue;
                }
                BossDef boss = BOOK.bosses.stream().filter(b -> b.id.equals(raise.spawn)).findFirst().orElse(null);
                if (boss == null || boss.place != null) {
                    continue;
                }
                for (Trigger kill : q.triggers()) {
                    if (kill.kind == Trigger.Kind.KILL && kill.target.equals(raise.spawn)
                            && kill.needFlags.stream().anyMatch(raise.setFlags::contains)) {
                        boolean again = q.triggers().stream().anyMatch(t -> t != raise && raise.spawn.equals(t.spawn)
                                && t.setFlags.stream().noneMatch(kill.needFlags::contains));
                        if (!again) {
                            stuck.add(q.id() + ": " + QuestWalk.describe(raise) + " raises " + raise.spawn + " once (it sets "
                                    + raise.setFlags + ") and the boss has no summoning stone");
                        }
                    }
                }
            }
        }
        assertTrue(stuck.isEmpty(), stuck.size() + " quest bosses a player can't fight again after losing:\n" + String.join("\n", stuck));
    }
}
