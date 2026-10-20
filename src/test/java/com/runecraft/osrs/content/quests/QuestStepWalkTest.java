package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                        && !walked.contains(t) && !plan.switchedOff().contains(t)) {
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

    /**
     * A player taking a quest's steps by hand from some stage, with some flags and quest items: a route other than the step
     * walk's order (the walk takes steps in listed order), or a save made before the quest was reworked.
     */
    private static final class Route implements QuestStepWalk.Player {
        final QuestSpec q;
        int stage;
        boolean done;
        final Set<String> flags = new LinkedHashSet<>();
        final Map<String, Integer> items = new HashMap<>();
        final Set<String> questItems;
        final Set<Trigger> firedHere = new LinkedHashSet<>();
        final List<Trigger> fired = new ArrayList<>();

        Route(QuestSpec q, int stage, String... flags) {
            this.q = q;
            this.stage = stage;
            this.flags.addAll(QuestStepWalk.outsideFlags(q));
            this.flags.addAll(List.of(flags));
            questItems = QuestStepWalk.questItems(q);
        }

        /** The step walk's own route up to (not into) {@code stage}. */
        static Route walkedTo(QuestSpec q, int stage) {
            Route r = new Route(q, 0);
            for (Trigger t : QuestStepWalk.stepPlan(q, "a-player", ItemCatalog::exists, RAISABLE).steps()) {
                if (r.stage >= stage) {
                    break;
                }
                r.fire(t);
            }
            assertEquals(stage, r.stage, q.id() + ": the step walk never reached stage " + stage);
            return r;
        }

        @Override
        public boolean flag(String flag) {
            return flags.contains(flag);
        }

        @Override
        public int carried(String item) {
            return items.getOrDefault(item, 0);
        }

        Route carry(String... carried) {
            for (String c : carried) {
                String[] parts = c.split(":");
                items.merge(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 1, Integer::sum);
            }
            return this;
        }

        void fire(Trigger t) {
            assertEquals(stage, t.stage, q.id() + ": " + QuestWalk.describe(t) + " belongs to another stage");
            assertNull(QuestStepWalk.blocked(t, this, questItems), q.id() + " stage " + stage + ": " + QuestWalk.describe(t));
            t.take.forEach((item, n) -> items.merge(item, -n, Integer::sum));
            flags.addAll(t.setFlags);
            if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(flags::contains)) {
                flags.add(QuestSpec.pickFlag("a-player", t.flagOneOf));
            }
            t.give.forEach((item, n) -> items.merge(item, t.kind == Trigger.Kind.DROP ? Math.max(1, t.count) : n, Integer::sum));
            fired.add(t);
            firedHere.add(t);
            if (t.completes()) {
                done = true;
            } else if (t.next != t.stage) {
                stage = t.next;
                firedHere.clear();
            }
        }

        /** Fires the first trigger of the current stage that matches. */
        Route fire(Predicate<Trigger> which, String what) {
            Trigger t = q.stages.get(stage).triggers.stream().filter(which).findFirst()
                    .orElseThrow(() -> new AssertionError(q.id() + " stage " + stage + " lacks " + what));
            fire(t);
            return this;
        }

        /** Takes every step a player can take from here on (side steps first, as the step walk does) until the quest is done. */
        Route finish() {
            for (int guard = 0; !done && guard < q.triggers().size() * 3; guard++) {
                QuestSpec.Stage s = q.stages.get(stage);
                Trigger t = QuestStepWalk.nextStep(s, this, questItems, firedHere);
                assertNotNull(t, q.id() + " stuck at stage " + stage + ": " + QuestStepWalk.whyStuck(s, this, questItems));
                fire(t);
            }
            assertTrue(done, q.id() + " not done after " + fired.size() + " steps (stage " + stage + ")");
            return this;
        }
    }

    @Test
    void dragonSlayerIIKeyPiecesComeInAnyOrder() {
        // OSRS (quest-helper: one quest state for all four pieces): Ungael and Vorkath may come before the mainland pieces
        QuestSpec ds2 = BOOK.spec("dragon_slayer_ii");
        Route r = Route.walkedTo(ds2, 3);
        r.fire(t -> t.target.equals("brundt"), "Brundt").fire(t -> t.target.equals("torfinn"), "Torfinn");
        assertEquals(4, r.stage);
        r.fire(t -> t.target.equals("amelia"), "Amelia, before Vorkath").fire(t -> t.puzzle == GrandmasterQuests.DS2_CRYPT, "the crypt");
        r.fire(t -> t.target.equals("ds2_vorkath"), "Vorkath");
        assertEquals(5, r.stage);
        r.finish();
        assertTrue(r.flags.contains("ds2_forged") && r.flags.contains("ds2_council"));
        assertTrue(r.fired.stream().anyMatch(t -> t.stage == 5 && t.give.containsKey("ds2_key_morytania")), "the Mort Myre piece after Vorkath");
        assertTrue(r.fired.stream().anyMatch(t -> t.stage == 5 && t.give.containsKey("ds2_key_karamja")), "the Kharazi piece after Vorkath");
    }

    @Test
    void savesFromBeforeTheReworkStillFinish() {
        QuestSpec ds2 = BOOK.spec("dragon_slayer_ii");
        // at the reforging stage, all four pieces held: the old Vorkath kill gave no ancient key for the mithril door
        Route r = new Route(ds2, 5, "ds2_house", "ds2_reldo")
                .carry("ds2_key_karamja", "ds2_key_morytania", "ds2_key_kourend", "ds2_key_ungael", "ds2_locator_orb").finish();
        assertTrue(r.fired.stream().anyMatch(t -> t.give.containsKey("ds2_ancient_key")), "the ancient key fallback");
        // sailed to Ungael early (the old Brundt needed only the map): only the Ungael piece
        new Route(ds2, 5, "ds2_house").carry("ds2_key_ungael").finish();
        QuestSpec wgs = BOOK.spec("while_guthix_sleeps");
        // at the chasm with no Dark Squall robes (the old quest never handed them out)
        r = new Route(wgs, 4, "wgs_assassins", "wgs_teleorb", "wgs_seed", "wgs_planted", "wgs_sketch", "wgs_elite", "wgs_silif",
                "wgs_surok").finish();
        assertTrue(r.fired.get(0).target.equals("idria") && r.fired.get(0).give.containsKey("dark_squall_hood"), "Idria brings the robes");
        // before round 33, at the spy stage: Ivy's old briefing goes on to the chasm
        new Route(wgs, 3).fire(t -> t.target.equals("ivy_sophista"), "Ivy's old briefing").finish();
        // rounds 33-36: Surok beaten, Akrisae not told yet; no strange teleorb and no robes
        r = new Route(wgs, 3, "wgs_assassins", "wgs_teleorb", "wgs_seed", "wgs_planted", "wgs_sketch", "wgs_elite", "wgs_silif",
                "wgs_surok").finish();
        assertTrue(r.fired.stream().anyMatch(t -> t.target.equals("silif") && t.give.containsKey("strange_teleorb")), "Silif's teleorb");
    }
}
