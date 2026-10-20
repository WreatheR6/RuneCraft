package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plans the in-game self-test's walk of every declarative quest ({@link QuestWalk#plan}, the same advancing-trigger rule
 * {@code GameplayChecks.quest} uses): a walk that would get stuck fails its self-test check, so it must fail here first.
 * Also checks what a real player needs that the walk hands over for free: flags the advancing triggers require must be
 * set by some trigger, and an advancing trigger must not be switched off by a flag the walk itself sets.
 */
class QuestWalkTest {
    private static final QuestBook BOOK = QuestBook.get();

    @Test
    void everyQuestWalksToCompletion() {
        List<String> stuck = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (QuestSpec q : BOOK.quests) {
            QuestWalk.Plan plan = QuestWalk.plan(q, ItemCatalog::exists);
            boolean listed = QuestWalk.UNWALKABLE.containsKey(q.id());
            if (!plan.completes() && !listed) {
                stuck.add(q.id() + ": " + plan.stuck());
            } else if (plan.completes() && listed) {
                stale.add(q.id());
            }
        }
        assertTrue(stuck.isEmpty(), stuck.size() + " quests the self-test would get stuck on:\n" + String.join("\n", stuck));
        assertTrue(stale.isEmpty(), "listed as unwalkable but walk fine (remove from QuestWalk.UNWALKABLE): " + stale);
    }

    @Test
    void realPlayersCanFireEveryAdvancingTrigger() {
        Set<String> settable = new HashSet<>(QuestBossRules.EXTERNAL_FLAGS); // set by game systems (a Theatre of Blood run)
        for (QuestSpec q : BOOK.quests) {
            q.triggers().forEach(t -> {
                settable.addAll(t.setFlags);
                settable.addAll(t.flagOneOf);
            });
        }
        List<String> problems = new ArrayList<>();
        for (QuestSpec q : BOOK.quests) {
            QuestWalk.Plan plan = QuestWalk.plan(q, ItemCatalog::exists);
            Set<String> setSoFar = new HashSet<>();
            for (Trigger t : plan.steps()) {
                String where = q.id() + " stage " + t.stage + " (" + QuestWalk.describe(t) + ")";
                for (String f : t.needFlags) {
                    if (!settable.contains(f)) {
                        problems.add(where + " needs flag " + f + " that no trigger sets");
                    }
                }
                for (String f : t.unlessFlags) {
                    if (setSoFar.contains(f)) {
                        problems.add(where + " is skipped once flag " + f + " is set, which an earlier step of the walk sets");
                    }
                }
                // Talking fires the first satisfiable trigger for that NPC: a side dialogue before it that can always
                // fire and never switches itself off hides the advancing one for good.
                if (t.kind == Trigger.Kind.TALK) {
                    for (Trigger o : q.stages.get(t.stage).triggers) {
                        if (o == t) {
                            break;
                        }
                        if (o.kind == Trigger.Kind.TALK && o.target.equals(t.target) && o.next == o.stage && alwaysFires(o)) {
                            problems.add(where + " is hidden by an earlier side dialogue with " + o.target
                                    + " that always fires (give it unless()/unlessFlag())");
                        }
                    }
                }
                // Side objectives of this stage a player does before moving on.
                q.stages.get(t.stage).triggers.forEach(o -> setSoFar.addAll(o.setFlags));
            }
        }
        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems));
    }

    /** A trigger with no conditions and nothing that turns it off: it fires every time. */
    private static boolean alwaysFires(Trigger o) {
        return o.unlessItems.isEmpty() && o.unlessFlags.isEmpty() && o.need.isEmpty() && o.take.isEmpty() && o.tools.isEmpty()
                && o.needFlags.isEmpty() && o.quests.isEmpty() && o.skills.isEmpty() && o.minQuestPoints == 0 && o.minCombat == 0;
    }

    @Test
    void stuckWalksAreReported() {
        QuestSpec q = new QuestSpec("walk_test", "Walk test", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("Talk to Bob.").talk("bob").stay()
                .stage("Done.").talk("bob");
        q.seal();
        QuestWalk.Plan plan = QuestWalk.plan(q, id -> true);
        assertTrue(!plan.completes() && plan.stuck().contains("stage 0 has no trigger"), String.valueOf(plan.stuck()));
        QuestSpec loop = new QuestSpec("walk_loop", "Walk loop", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").talk("bob").to(1)
                .stage("B").talk("bob").to(0)
                .stage("C").talk("bob");
        loop.seal();
        assertTrue(QuestWalk.plan(loop, id -> true).stuck().startsWith("no completion"));
        QuestSpec item = new QuestSpec("walk_item", "Walk item", com.runecraft.osrs.Quest.Difficulty.NOVICE, 1)
                .stage("A").talk("bob").take("no_such_item");
        item.seal();
        assertTrue(QuestWalk.plan(item, ItemCatalog::exists).stuck().contains("takes unknown item no_such_item"));
    }
}
