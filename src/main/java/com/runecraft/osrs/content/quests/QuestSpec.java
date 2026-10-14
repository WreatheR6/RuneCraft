package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.QuestDef;
import com.runecraft.osrs.data.ToolTier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative quest script: a state machine of {@link Stage}s. Each stage has a journal line and a
 * list of {@link Trigger}s (talk to an NPC, kill a creature, reach a place, or a kill that drops a
 * quest item) that check conditions, run dialogue, move items and advance the stage. The
 * Minecraft-side interpreter ({@code com.runecraft.content.quests.QuestInterpreter}) executes it.
 *
 * <p>Dialogue lines: plain text is spoken by the trigger's NPC, {@code "> text"} by the player,
 * {@code "* text"} is narration and {@code "Name: text"} after a {@code "@"} ({@code "@Name: text"})
 * is spoken by someone else.
 *
 * <pre>
 * book.quest("lost_city", "Lost City", Quest.Difficulty.INTERMEDIATE, 3)
 *     .req(Skill.CRAFTING, 31).req(Skill.WOODCUTTING, 36)
 *     .stage("Speak to the adventurers camping in Lumbridge Swamp.")
 *         .talk("lc_warrior").say("We're looking for Zanaris.", "> Zanaris?")
 *     .stage("Find the leprechaun hiding in a tree in Lumbridge Swamp.")
 *         .talk("shamus").say(...)
 *     ...
 * </pre>
 */
public final class QuestSpec {
    /** One state of the quest. */
    public static final class Stage {
        public final int index;
        public final String journal;
        public final List<Trigger> triggers = new ArrayList<>();
        /** NPC id -> idle lines spoken at this stage when no trigger applies. */
        public final Map<String, List<String>> hints = new LinkedHashMap<>();

        Stage(int index, String journal) {
            this.index = index;
            this.journal = journal;
        }
    }

    public final QuestDef def;
    /** Quest points needed to start (QuestDef has no field for this). */
    public int minQuestPoints;
    public final List<Stage> stages = new ArrayList<>();
    /** NPC id -> lines spoken after the quest is complete. */
    public final Map<String, List<String>> doneLines = new LinkedHashMap<>();
    /** Free-text unlocks recorded for the journal ("Ability to wield dragon longswords"). */
    public final List<String> unlocks = new ArrayList<>();
    /** Requirements recorded but not enforced because the quest isn't implemented (shown in the journal). */
    public final List<String> missingRequirements = new ArrayList<>();
    private Trigger current;

    QuestSpec(String id, String name, Quest.Difficulty difficulty, int qp) {
        this.def = QuestDef.of(id, name).difficulty(difficulty).points(qp);
    }

    public String id() {
        return def.questId();
    }

    // ---------------- Quest-level ----------------

    public QuestSpec req(Skill skill, int level) {
        def.requires(skill, level);
        return this;
    }

    public QuestSpec reqQuest(String... ids) {
        for (String id : ids) {
            def.requiresQuest(id);
        }
        return this;
    }

    /** A real OSRS requirement that this mod doesn't implement (documented, not enforced). */
    public QuestSpec reqNote(String text) {
        missingRequirements.add(text);
        return this;
    }

    public QuestSpec qp(int questPoints) {
        this.minQuestPoints = questPoints;
        return this;
    }

    public QuestSpec xp(Skill skill, double xp) {
        def.xp(skill, xp);
        return this;
    }

    public QuestSpec reward(String... items) {
        for (String item : items) {
            def.reward(item);
        }
        return this;
    }

    public QuestSpec unlock(String text) {
        unlocks.add(text);
        return this;
    }

    /** Lines an NPC says once the quest is complete. */
    public QuestSpec done(String npc, String... lines) {
        doneLines.put(npc, List.of(lines));
        return this;
    }

    /** Starts a new stage with its journal line. */
    public QuestSpec stage(String journal) {
        stages.add(new Stage(stages.size(), journal));
        def.step(journal);
        current = null;
        return this;
    }

    private Stage last() {
        if (stages.isEmpty()) {
            throw new IllegalStateException(id() + ": define a stage first");
        }
        return stages.get(stages.size() - 1);
    }

    /** Idle lines for an NPC at the current stage. */
    public QuestSpec hint(String npc, String... lines) {
        last().hints.put(npc, List.of(lines));
        return this;
    }

    // ---------------- Triggers ----------------

    private QuestSpec add(Trigger.Kind kind, String target) {
        Stage s = last();
        current = new Trigger(kind, target, s.index);
        s.triggers.add(current);
        return this;
    }

    /** Talking to an NPC. */
    public QuestSpec talk(String npc) {
        return add(Trigger.Kind.TALK, npc);
    }

    /** Killing a creature (module monster/boss id, built-in monster entity id, or name in snake_case). */
    public QuestSpec kill(String creature) {
        return add(Trigger.Kind.KILL, creature);
    }

    /** Arriving within {@code radius} blocks of a place (Landmark name or PlaceDef id). */
    public QuestSpec reach(String place, int radius) {
        add(Trigger.Kind.REACH, place);
        current.radius = radius;
        return this;
    }

    /**
     * Killing {@code count} of a creature (5 ice trolls, 4 tortured gorillas): the interpreter counts the kills per
     * player (saved with the world) and fires the trigger on the last one.
     */
    public QuestSpec kill(String creature, int count) {
        add(Trigger.Kind.KILL, creature);
        current.count = Math.max(1, count);
        return this;
    }

    /**
     * How many times the current objective must happen: kills for a {@link #kill} trigger, and for a {@link #drop}
     * how many of the item to collect (the drop keeps rolling until the player carries that many).
     */
    public QuestSpec times(int count) {
        Trigger t = cur();
        if (t.kind != Trigger.Kind.KILL && t.kind != Trigger.Kind.DROP) {
            throw new IllegalStateException(id() + ": times() only applies to kill and drop objectives");
        }
        t.count = Math.max(1, count);
        return this;
    }

    /** While at this stage, killing {@code creature} drops {@code item} with a 1/{@code chance} chance (once). */
    public QuestSpec drop(String creature, String item, int chance) {
        add(Trigger.Kind.DROP, creature);
        current.give.put(item, 1);
        current.chance = chance;
        current.next = current.stage;
        return this;
    }

    private Trigger cur() {
        if (current == null) {
            throw new IllegalStateException(id() + ": define a trigger first");
        }
        return current;
    }

    public QuestSpec say(String... lines) {
        cur().lines.addAll(List.of(lines));
        return this;
    }

    /** Lines spoken when the trigger's conditions aren't met. */
    public QuestSpec missing(String... lines) {
        cur().missing.addAll(List.of(lines));
        return this;
    }

    /** Items that must be carried (not consumed): "item" or "item:count". */
    public QuestSpec need(String... items) {
        parse(cur().need, items);
        return this;
    }

    /**
     * Requires any tool of a kind ("pickaxe", "axe"), optionally of at least a metal tier
     * ("pickaxe:mithril"): bronze to dragon, crystal and infernal tools all count (crystal/infernal as dragon).
     */
    public QuestSpec needTool(String... tools) {
        for (String tool : tools) {
            String kind = tool.contains(":") ? tool.substring(0, tool.indexOf(':')) : tool;
            if (!TOOL_KINDS.containsKey(kind)) {
                throw new IllegalArgumentException(id() + ": unknown tool kind " + tool);
            }
            if (tool.contains(":") && toolTier(tool) < 0) {
                throw new IllegalArgumentException(id() + ": unknown tool tier " + tool);
            }
            cur().tools.add(tool);
        }
        return this;
    }

    /** Tool kind -> item kind and the cheapest tool of that kind (the self-test hands it over). */
    public static final Map<String, ItemDef.Kind> TOOL_KINDS = Map.of("pickaxe", ItemDef.Kind.PICKAXE, "axe", ItemDef.Kind.AXE);

    /** Minimum {@link ToolTier} ordinal of a tool requirement ("pickaxe:rune"), 0 for any; -1 if unknown. */
    public static int toolTier(String tool) {
        int colon = tool.indexOf(':');
        if (colon < 0) {
            return 0;
        }
        String metal = tool.substring(colon + 1);
        for (ToolTier t : ToolTier.values()) {
            if (t.metal().id().equals(metal)) {
                return t.ordinal();
            }
        }
        return -1;
    }

    /** Whether an item satisfies a tool requirement ("pickaxe", "axe:mithril"). */
    public static boolean toolSatisfies(String tool, ItemDef item) {
        if (item == null) {
            return false;
        }
        String kind = tool.contains(":") ? tool.substring(0, tool.indexOf(':')) : tool;
        if (item.kind() != TOOL_KINDS.get(kind)) {
            return false;
        }
        int min = toolTier(tool);
        for (ToolTier t : ToolTier.values()) {
            if (t.metal().id().equals(item.tier())) {
                return t.ordinal() >= min;
            }
        }
        return min == 0;
    }

    /** The item the self-test hands over for a tool requirement: the cheapest tool that satisfies it. */
    public static String toolItem(String tool) {
        String kind = tool.contains(":") ? tool.substring(0, tool.indexOf(':')) : tool;
        ToolTier tier = ToolTier.values()[Math.max(0, toolTier(tool))];
        return kind.equals("pickaxe") ? tier.pickaxeId() : tier.axeId();
    }

    /** How a tool requirement reads in messages: "a pickaxe", "a mithril axe or better". */
    public static String toolName(String tool) {
        String kind = tool.contains(":") ? tool.substring(0, tool.indexOf(':')) : tool;
        if (!tool.contains(":")) {
            return (kind.startsWith("a") ? "an " : "a ") + kind;
        }
        return "a " + tool.substring(tool.indexOf(':') + 1) + " " + kind + " or better";
    }

    /** Items consumed by the trigger (also required). */
    public QuestSpec take(String... items) {
        parse(cur().take, items);
        return this;
    }

    public QuestSpec give(String... items) {
        parse(cur().give, items);
        return this;
    }

    /** Skip this trigger if the player carries any of these items. */
    public QuestSpec unless(String... items) {
        cur().unlessItems.addAll(List.of(items));
        return this;
    }

    /** Sets a persistent per-player quest flag. Combined with {@link #stay()} the trigger runs once. */
    public QuestSpec flag(String... flags) {
        cur().setFlags.addAll(List.of(flags));
        return this;
    }

    /** Requires flags to be set. */
    public QuestSpec needFlag(String... flags) {
        cur().needFlags.addAll(List.of(flags));
        return this;
    }

    /** Skip this trigger if any of these flags is set. */
    public QuestSpec unlessFlag(String... flags) {
        cur().unlessFlags.addAll(List.of(flags));
        return this;
    }

    public QuestSpec reqQp(int qp) {
        cur().minQuestPoints = qp;
        return this;
    }

    /** Requires a combat level (Dream Mentor: 85). */
    public QuestSpec reqCombat(int level) {
        cur().minCombat = level;
        return this;
    }

    public QuestSpec reqSkill(Skill skill, int level) {
        cur().skills.put(skill, level);
        return this;
    }

    public QuestSpec reqDone(String... questIds) {
        cur().quests.addAll(List.of(questIds));
        return this;
    }

    /** Stay at the current stage (a side objective). */
    public QuestSpec stay() {
        cur().next = cur().stage;
        return this;
    }

    /** Jump to a specific stage. */
    public QuestSpec to(int stage) {
        cur().next = stage;
        return this;
    }

    /** Completes the quest when the trigger fires. */
    public QuestSpec finish() {
        cur().next = Trigger.COMPLETE;
        return this;
    }

    /** Spawns a quest boss (BossDef id) next to the player when the trigger fires. */
    public QuestSpec spawn(String bossId) {
        cur().spawn = bossId;
        return this;
    }

    /** Moves the player to a place (boat trips, teleports) when the trigger fires. */
    public QuestSpec travel(String place) {
        cur().travel = place;
        return this;
    }

    /** Grants XP when the trigger fires (sub-quest rewards). */
    public QuestSpec grantXp(Skill skill, double xp) {
        cur().xp.put(skill, xp);
        return this;
    }

    private static void parse(Map<String, Integer> into, String... items) {
        for (String s : items) {
            int colon = s.lastIndexOf(':');
            if (colon > 0 && s.substring(colon + 1).matches("\\d+")) {
                into.merge(s.substring(0, colon), Integer.parseInt(s.substring(colon + 1)), Integer::sum);
            } else {
                into.merge(s, 1, Integer::sum);
            }
        }
    }

    /** Resolves default "next stage" values; called once the quest is fully declared. */
    void seal() {
        for (Stage s : stages) {
            for (Trigger t : s.triggers) {
                if (t.next == Trigger.DEFAULT) {
                    t.next = s.index + 1;
                }
                if (t.next >= stages.size()) {
                    t.next = Trigger.COMPLETE;
                }
            }
        }
    }

    /** All triggers in all stages. */
    public List<Trigger> triggers() {
        List<Trigger> all = new ArrayList<>();
        stages.forEach(s -> all.addAll(s.triggers));
        return all;
    }

    /** One objective / transition of a stage. */
    public static final class Trigger {
        public enum Kind { TALK, KILL, REACH, DROP }

        public static final int DEFAULT = -2;
        public static final int COMPLETE = -1;

        public final Kind kind;
        public final String target;
        public final int stage;
        public final List<String> lines = new ArrayList<>();
        public final List<String> missing = new ArrayList<>();
        public final Map<String, Integer> need = new LinkedHashMap<>();
        public final Map<String, Integer> take = new LinkedHashMap<>();
        /** Tool requirements ("pickaxe", "axe:mithril"): any tool of that kind and tier carried. */
        public final List<String> tools = new ArrayList<>();
        public final Map<String, Integer> give = new LinkedHashMap<>();
        public final List<String> unlessItems = new ArrayList<>();
        public final List<String> setFlags = new ArrayList<>();
        public final List<String> needFlags = new ArrayList<>();
        public final List<String> unlessFlags = new ArrayList<>();
        public final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        public final List<String> quests = new ArrayList<>();
        public final Map<Skill, Double> xp = new EnumMap<>(Skill.class);
        public int minQuestPoints;
        public int minCombat;
        public int next = DEFAULT;
        public int chance = 1;
        /** Kills needed (KILL) or items to collect (DROP); see {@link QuestSpec#times}. */
        public int count = 1;
        public int radius = 8;
        public String spawn;
        public String travel;

        Trigger(Kind kind, String target, int stage) {
            this.kind = kind;
            this.target = target;
            this.stage = stage;
        }

        /** Key of this trigger's saved kill counter: quest, stage, position in the stage and creature. */
        public String counterKey(String questId, int indexInStage) {
            return questId + ":" + stage + ":" + indexInStage + ":" + target;
        }

        /** Whether the trigger ends the quest. */
        public boolean completes() {
            return next == COMPLETE;
        }

        /** Every item id this trigger references. */
        public List<String> items() {
            List<String> all = new ArrayList<>(need.keySet());
            all.addAll(take.keySet());
            all.addAll(give.keySet());
            all.addAll(unlessItems);
            return all;
        }
    }
}
