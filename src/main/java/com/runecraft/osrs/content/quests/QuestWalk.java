package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * How the in-game self-test ({@code com.runecraft.selftest}) walks a {@link QuestSpec}: at every stage it takes the
 * {@linkplain #advancing advancing trigger}, hands over what it needs and fires it, until the quest completes. The same
 * rules are planned here statically ({@link #plan}) so a unit test can find quests the walk would get stuck on before CI
 * does.
 */
public final class QuestWalk {
    /**
     * Quests the self-test's fake player legitimately can't walk, with the reason. A stuck walk of any other quest fails
     * its check (and {@code QuestWalkTest} fails for any quest whose plan gets stuck and isn't listed here, or that is
     * listed but walks).
     */
    public static final Map<String, String> UNWALKABLE = Map.of();

    private QuestWalk() {
    }

    /** The trigger that moves a stage on: the first non-drop trigger that leaves the stage, else a drop that does. */
    public static Trigger advancing(QuestSpec.Stage stage) {
        Trigger drop = null;
        for (Trigger t : stage.triggers) {
            if (t.next == t.stage) {
                continue;
            }
            if (t.kind != Trigger.Kind.DROP) {
                return t;
            }
            if (drop == null) {
                drop = t;
            }
        }
        return drop;
    }

    /** Most triggers a walk fires before it is considered stuck in a loop. */
    public static int maxSteps(QuestSpec q) {
        return q.stages.size() * 3 + 3;
    }

    /** A planned walk: the advancing triggers in order, and why it gets stuck (null if it completes). */
    public record Plan(List<Trigger> steps, String stuck) {
        public boolean completes() {
            return stuck == null;
        }
    }

    /**
     * Plans the self-test's walk of a quest. {@code itemExists} says whether an item id can be handed to the player (the
     * walk gives every needed and taken item; a missing one makes the trigger's take fail).
     */
    public static Plan plan(QuestSpec q, Predicate<String> itemExists) {
        List<Trigger> steps = new ArrayList<>();
        Set<Integer> visited = new LinkedHashSet<>();
        int s = 0;
        for (int guard = 0; guard < maxSteps(q); guard++) {
            if (s < 0 || s >= q.stages.size()) {
                return new Plan(steps, "stage " + s + " is outside the " + q.stages.size() + " stages");
            }
            visited.add(s);
            Trigger t = advancing(q.stages.get(s));
            if (t == null) {
                return new Plan(steps, "stage " + s + " has no trigger that moves it on");
            }
            String where = "stage " + s + " (" + describe(t) + ")";
            for (String item : t.take.keySet()) {
                if (!exists(item, itemExists)) {
                    return new Plan(steps, where + " takes unknown item " + item);
                }
            }
            for (String item : t.need.keySet()) {
                if (!exists(item, itemExists)) {
                    return new Plan(steps, where + " needs unknown item " + item);
                }
            }
            for (String tool : t.tools) {
                if (!exists(QuestSpec.toolItem(tool), itemExists)) {
                    return new Plan(steps, where + " needs " + tool + " but " + QuestSpec.toolItem(tool) + " does not exist");
                }
            }
            steps.add(t);
            if (t.completes()) {
                return new Plan(steps, null);
            }
            s = t.next;
        }
        return new Plan(steps, "no completion after " + maxSteps(q) + " triggers (loops through stages " + visited + ")");
    }

    private static boolean exists(String item, Predicate<String> itemExists) {
        return item.equals("coins") || itemExists.test(item);
    }

    /** "talk reldo", "kill jonny_the_beard x5", "reach LUMBRIDGE". */
    public static String describe(Trigger t) {
        return t.kind.name().toLowerCase(java.util.Locale.ROOT) + " " + t.target + (t.count > 1 ? " x" + t.count : "");
    }
}
