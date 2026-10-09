package com.runecraft.osrs.content;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A module quest. Stage 0 = not started; stage {@code journal.size()} = complete. Dialogue and stage
 * changes are scripted on the Minecraft side with {@code com.runecraft.quest.QuestScripts}.
 */
public final class QuestDef implements QuestInfo {
    private final String id;
    private final String name;
    private Quest.Difficulty difficulty = Quest.Difficulty.NOVICE;
    private int questPoints = 1;
    private final Map<Skill, Integer> requirements = new EnumMap<>(Skill.class);
    private final List<String> questRequirements = new ArrayList<>();
    private final Map<Skill, Double> xpRewards = new EnumMap<>(Skill.class);
    private final List<String> itemRewards = new ArrayList<>();
    private final List<String> journal = new ArrayList<>();

    public QuestDef(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static QuestDef of(String id, String name) {
        return new QuestDef(id, name);
    }

    public QuestDef difficulty(Quest.Difficulty d) {
        this.difficulty = d;
        return this;
    }

    public QuestDef points(int qp) {
        this.questPoints = qp;
        return this;
    }

    public QuestDef requires(Skill skill, int level) {
        requirements.put(skill, level);
        return this;
    }

    /** Requires another quest (by questId) to be complete before starting. */
    public QuestDef requiresQuest(String questId) {
        questRequirements.add(questId);
        return this;
    }

    public QuestDef xp(Skill skill, double xp) {
        xpRewards.put(skill, xp);
        return this;
    }

    /** "item" or "item:count". */
    public QuestDef reward(String item) {
        itemRewards.add(item);
        return this;
    }

    /** Adds the journal line shown while at the next stage (stage 0 first). */
    public QuestDef step(String journalLine) {
        journal.add(journalLine);
        return this;
    }

    public List<String> questRequirements() {
        return questRequirements;
    }

    @Override
    public String questId() {
        return id;
    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public Quest.Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public int questPoints() {
        return questPoints;
    }

    @Override
    public Map<Skill, Integer> requirements() {
        return requirements;
    }

    @Override
    public Map<Skill, Double> xpRewards() {
        return xpRewards;
    }

    @Override
    public List<String> itemRewards() {
        return itemRewards;
    }

    @Override
    public List<String> journal() {
        return journal;
    }
}
