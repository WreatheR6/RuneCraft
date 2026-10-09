package com.runecraft.osrs;

import java.util.List;
import java.util.Map;

/** Common view of built-in ({@link Quest}) and module-defined ({@link com.runecraft.osrs.content.QuestDef}) quests. */
public interface QuestInfo {
    /** Stable id used for saving progress. */
    String questId();

    String displayName();

    Quest.Difficulty difficulty();

    int questPoints();

    Map<Skill, Integer> requirements();

    Map<Skill, Double> xpRewards();

    List<String> itemRewards();

    List<String> journal();

    default int finalStage() {
        return journal().size();
    }

    default String journalFor(int stage) {
        if (stage >= finalStage()) {
            return "QUEST COMPLETE!";
        }
        return journal().get(Math.max(0, stage));
    }
}
