package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plans the self-test's step walk ({@code quest:steps:<id>}, {@link QuestStepWalk#stepPlan}) of the grandmaster quests: every
 * objective in order, side objectives included, each flag and quest item earned by an earlier step, every talk reaching
 * the step it means, every quest boss either summonable at its arena or raised by an earlier step, and no side objective
 * left behind when a stage is left.
 */
class QuestStepWalkTest {
    private static final QuestBook BOOK = QuestBook.get();

    /** Kill targets met without a step raising them: anything but a quest boss without an arena. */
    static final Predicate<String> RAISABLE = target -> {
        for (BossDef b : BOOK.bosses) {
            if (b.id.equals(target)) {
                return b.place != null;
            }
        }
        return true;
    };

    @Test
    void grandmasterQuestsWalkStepByStep() {
        List<String> failures = new ArrayList<>();
        for (String id : QuestStepWalk.STEP_WALKED) {
            QuestSpec q = BOOK.spec(id);
            assertNotNull(q, id);
            for (String player : List.of("a-player", "another-player", "a-third-player")) {
                QuestStepWalk.StepPlan plan = QuestStepWalk.stepPlan(q, player, ItemCatalog::exists, RAISABLE);
                if (!plan.completes()) {
                    failures.add(id + ": " + plan.stuck());
                }
                plan.problems().forEach(p -> failures.add(id + ": " + p));
                Set<Integer> visited = new LinkedHashSet<>();
                plan.steps().forEach(t -> visited.add(t.stage));
                if (plan.completes() && visited.size() != q.stages.size()) {
                    failures.add(id + ": the walk skips stages (visits " + visited + " of " + q.stages.size() + ")");
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " step-walk failures:\n" + String.join("\n", new LinkedHashSet<>(failures)));
    }

    @Test
    void everyFlaggedObjectiveIsWalked() {
        // Each trigger that sets a flag or hands out an item is a step a player takes: the walk must take it (or it must be
        // a fallback switched off by the time the walk gets there).
        List<String> missed = new ArrayList<>();
        for (String id : QuestStepWalk.STEP_WALKED) {
            QuestSpec q = BOOK.spec(id);
            QuestStepWalk.StepPlan plan = QuestStepWalk.stepPlan(q, "a-player", ItemCatalog::exists, RAISABLE);
            Set<Trigger> walked = new LinkedHashSet<>(plan.steps());
            for (Trigger t : q.triggers()) {
                if ((!t.setFlags.isEmpty() || !t.give.isEmpty() || t.kind == Trigger.Kind.KILL || t.kind == Trigger.Kind.PUZZLE)
                        && !walked.contains(t) && !t.unlessFlags.contains("wgs_assassins")) {
                    missed.add(id + " stage " + t.stage + " trigger " + q.stages.get(t.stage).triggers.indexOf(t) + " (" + QuestWalk.describe(t) + ")");
                }
            }
        }
        assertTrue(missed.isEmpty(), missed.size() + " objectives the step walk never takes:\n" + String.join("\n", missed));
    }

    @Test
    void brokenChainsAreReported() {
        QuestSpec q = new QuestSpec("step_test", "Step test", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").talk("bob").stay().give("step_note")
                .talk("ann").need("step_note").flag("a")
                .stage("B").talk("carl").stay().needFlag("b").flag("b") // set only by a step that needs it
                .talk("bob").needFlag("b");
        q.seal();
        QuestStepWalk.StepPlan plan = QuestStepWalk.stepPlan(q, "p", id -> true, id -> true);
        assertEquals(2, plan.steps().size());
        assertTrue(plan.stuck().contains("needs flag b"), plan.stuck());
        QuestSpec shadow = new QuestSpec("step_shadow", "Step shadow", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").talk("bob").stay().say("hello")
                .talk("bob").flag("x")
                .stage("B").talk("bob");
        shadow.seal();
        assertTrue(QuestStepWalk.stepPlan(shadow, "p", id -> true, id -> true).stuck().contains("fires another step first"));
        QuestSpec unraised = new QuestSpec("step_raise", "Step raise", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").kill("hidden_boss");
        unraised.seal();
        assertTrue(QuestStepWalk.stepPlan(unraised, "p", id -> true, id -> false).stuck().contains("nothing raises hidden_boss"));
        QuestSpec left = new QuestSpec("step_left", "Step left", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").talk("bob").stay().needFlag("y").flag("y")
                .talk("ann")
                .stage("B").talk("bob");
        left.seal();
        assertTrue(QuestStepWalk.stepPlan(left, "p", id -> true, id -> true).problems().get(0).contains("never fired"));
    }
}
