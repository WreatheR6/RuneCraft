package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A quest walked the way a real player walks it, every step in order ({@link QuestWalk} only takes each stage's advancing
 * trigger and hands over whatever it needs). The walker carries only what the quest itself has handed out so far: a quest
 * item (one a trigger of the quest gives) must come from that trigger, and quest flags only from the triggers that set
 * them. Items the quest never hands out (logs, bars, runes, a cake) are gathered just before the step that needs them,
 * like a player bringing them along.
 *
 * <p>At every stage the walker first does the side objectives that still make progress (a {@code stay()} trigger that
 * sets a new flag, hands out an item not yet carried, or moves the quest on), in declaration order, and only then the
 * trigger that leaves the stage. Each step is an action the game dispatches the way it really does: talking to an NPC
 * fires the first satisfiable talk trigger for it (or shows a puzzle it presents), a kill fires the first satisfiable kill
 * trigger for that creature, arriving at a place the first satisfiable reach trigger there. When the dispatched trigger is
 * not the one the walker wanted and makes no progress, a real player would be stuck: the walk reports it.
 *
 * <p>The in-game self-test (QuestWalkChecks, {@code quest:walk:<id>}) performs the same steps with real actions and
 * checks each step's effect; {@code QuestStrictWalkTest} plans the master and experienced quests here first.
 */
public final class QuestStrictWalk {
    /** Most times one trigger may fire in a walk (a side objective repeating without end is a bug). */
    static final int MAX_FIRES = 4;

    /**
     * The master and late experienced quests walked step by step with real actions by the self-test
     * ({@code quest:walk:<id>}), whose plans must also leave no step unfired ({@code QuestStrictWalkTest}).
     */
    public static final List<String> WALKED = List.of("desert_treasure_i", "monkey_madness_i", "dream_mentor", "lunar_diplomacy",
            "legends_quest", "regicide", "mournings_end_part_i", "mournings_end_part_ii", "roving_elves", "haunted_mine",
            "dragon_slayer_i", "the_fremennik_isles", "swan_song", "recipe_for_disaster", "making_friends_with_my_arm",
            "a_kingdom_divided", "the_path_of_glouphrie", "beneath_cursed_sands", "a_night_at_the_theatre",
            // round 44: puzzle quests given their real steps and puzzles
            "the_golem", "hazeel_cult", "the_curse_of_arrav", "defender_of_varrock", "the_blood_moon_rises", "grim_tales",
            "the_fremennik_exiles");

    private QuestStrictWalk() {
    }

    /**
     * One thing the player does ({@code kind} on {@code target}, {@code count} times for kills; a {@code DROP} kills until
     * the item drops) and the trigger it fires. {@code handed} are the outside items gathered for it.
     */
    public record Step(Trigger.Kind kind, String target, int count, Trigger fired, Map<String, Integer> handed, List<String> outsideFlags) {
        @Override
        public String toString() {
            return kind.name().toLowerCase(java.util.Locale.ROOT) + " " + target + (count > 1 ? " x" + count : "")
                    + " (stage " + fired.stage + ")";
        }
    }

    /**
     * A planned walk. {@code stuck} is null when the quest completes; {@code dead} are the triggers that could make progress
     * (set a flag, hand out an item, move the stage) but never fired.
     */
    public record Plan(List<Step> steps, String stuck, List<Trigger> dead) {
        public boolean completes() {
            return stuck == null;
        }
    }

    /** What the walker carries and knows. */
    private static final class State {
        int stage;
        final Map<String, Integer> carried = new LinkedHashMap<>();
        final Set<String> flags = new LinkedHashSet<>();
        final Map<Trigger, Integer> fires = new IdentityHashMap<>();
        /** Flags no trigger of this quest sets (another quest's progress): a player who may start the quest has them. */
        final Set<String> outside = new HashSet<>();

        int has(String item) {
            return carried.getOrDefault(item, 0);
        }
    }

    /**
     * Quest items a trigger of the quest hands out: the walker only gets these from the quest itself. Items the quests
     * module doesn't define (fungi, bars, a cake) can be gathered elsewhere even when a quest step also hands them out.
     */
    public static Set<String> questItems(QuestSpec q) {
        Set<String> made = givenItems(q);
        made.retainAll(bookItems());
        return made;
    }

    /** Every item a trigger of the quest hands out: the walker keeps these between steps (outside supplies it doesn't). */
    public static Set<String> givenItems(QuestSpec q) {
        Set<String> given = new HashSet<>();
        for (Trigger t : q.triggers()) {
            given.addAll(t.give.keySet());
        }
        return given;
    }

    private static Set<String> bookItems;

    /** Every item the quests module defines. */
    static synchronized Set<String> bookItems() {
        if (bookItems == null) {
            Set<String> ids = new HashSet<>();
            QuestBook.get().items.forEach(adder -> adder.accept((id, name, kind, shape, color) -> {
                ids.add(id);
                return new com.runecraft.osrs.ItemDef(id, name, kind, shape, color);
            }));
            bookItems = ids;
        }
        return bookItems;
    }

    /** Plans the walk for a player (their id picks puzzle rooms and per-player routes, as in game). */
    public static Plan plan(QuestSpec q, String player) {
        Set<String> made = questItems(q);
        Set<String> given = givenItems(q);
        State st = new State();
        Set<String> settable = new HashSet<>();
        for (Trigger t : q.triggers()) {
            settable.addAll(t.setFlags);
            settable.addAll(t.flagOneOf);
        }
        for (Trigger t : q.triggers()) {
            t.needFlags.stream().filter(f -> !settable.contains(f)).forEach(st.outside::add);
        }
        List<Step> steps = new ArrayList<>();
        int guard = 0;
        while (true) {
            if (++guard > 400) {
                return finish(q, steps, "no completion after 400 steps (at stage " + st.stage + ")", st);
            }
            if (st.stage < 0 || st.stage >= q.stages.size()) {
                return finish(q, steps, "stage " + st.stage + " is outside the " + q.stages.size() + " stages", st);
            }
            QuestSpec.Stage stage = q.stages.get(st.stage);
            Trigger want = choose(stage, st, made, player);
            if (want == null) {
                return finish(q, steps, "stage " + st.stage + " (\"" + stage.journal + "\"): nothing a player can do moves it on"
                        + blockers(stage, st, made), st);
            }
            Trigger fired = dispatch(q, stage, want, st, made, player);
            if (fired == null) {
                return finish(q, steps, "stage " + st.stage + ": " + QuestWalk.describe(want) + " can't be done"
                        + (st.stage == 0 ? " (a quest only starts by talking to someone)" : ""), st);
            }
            if (fired != want && !progress(fired, st, made)) {
                return finish(q, steps, "stage " + st.stage + ": " + action(want) + " fires an earlier trigger that makes no progress ("
                        + QuestWalk.describe(fired) + ") instead of " + QuestWalk.describe(want), st);
            }
            Map<String, Integer> handed = new LinkedHashMap<>();
            for (Map<String, Integer> m : List.of(fired.need, fired.take)) {
                m.forEach((item, n) -> {
                    if (!made.contains(item) && st.has(item) < n) {
                        handed.merge(item, n - st.has(item), Integer::sum);
                        st.carried.merge(item, n - st.has(item), Integer::sum);
                    }
                });
            }
            for (String tool : fired.tools) {
                String item = QuestSpec.toolItem(tool);
                if (st.has(item) == 0) {
                    handed.merge(item, 1, Integer::sum);
                    st.carried.merge(item, 1, Integer::sum);
                }
            }
            List<String> outsideFlags = fired.needFlags.stream().filter(st.outside::contains).toList();
            steps.add(new Step(want.kind == Trigger.Kind.DROP ? Trigger.Kind.DROP : fired.kind, target(fired, player),
                    fired.kind == Trigger.Kind.PUZZLE ? 1 : fired.count, fired, handed, outsideFlags));
            apply(fired, st, player);
            // outside supplies are gathered afresh for each step that needs them (the in-game walk empties them out too)
            st.carried.keySet().removeIf(item -> !given.contains(item));
            if (fired.completes()) {
                return finish(q, steps, null, st);
            }
        }
    }

    /** The trigger the walker wants next: a side objective that still makes progress, else the way out of the stage. */
    private static Trigger choose(QuestSpec.Stage stage, State st, Set<String> made, String player) {
        for (Trigger t : stage.triggers) {
            if (t.next == t.stage && enabled(t, st, made) && progress(t, st, made) && st.fires.getOrDefault(t, 0) < MAX_FIRES
                    && (stage.index > 0 || t.kind == Trigger.Kind.TALK)) {
                return t;
            }
        }
        Trigger drop = null;
        for (Trigger t : stage.triggers) {
            if (t.next != t.stage && enabled(t, st, made)) {
                if (t.kind != Trigger.Kind.DROP) {
                    return t;
                }
                if (drop == null) {
                    drop = t;
                }
            }
        }
        return drop;
    }

    /** What the game fires for the action behind {@code want}, or null if that action does nothing. */
    private static Trigger dispatch(QuestSpec q, QuestSpec.Stage stage, Trigger want, State st, Set<String> made, String player) {
        switch (want.kind) {
            case TALK -> {
                if (stage.index == 0) {
                    // QuestInterpreter.talk: the first stage-0 talk trigger for the NPC starts the quest (or shows what is missing)
                    for (Trigger t : stage.triggers) {
                        if (t.kind == Trigger.Kind.TALK && t.target.equals(want.target) && !skipped(t, st)) {
                            return enabled(t, st, made) ? t : null;
                        }
                    }
                    return null;
                }
                for (Trigger t : stage.triggers) {
                    if (t.kind == Trigger.Kind.PUZZLE && !skipped(t, st) && enabled(t, st, made) && presenter(t, player).equals(want.target)) {
                        return t == want ? t : null; // the NPC shows its puzzle instead
                    }
                }
                return first(stage, Trigger.Kind.TALK, want.target, st, made);
            }
            case KILL -> {
                return first(stage, Trigger.Kind.KILL, want.target, st, made);
            }
            case DROP -> {
                // QuestInterpreter.onKill: a kill trigger for the same creature fires (and ends the dispatch) on the first kill
                for (Trigger t : stage.triggers) {
                    if (t == want) {
                        return t;
                    }
                    if (t.kind == Trigger.Kind.KILL && t.target.equals(want.target) && enabled(t, st, made)) {
                        return t;
                    }
                }
                return null;
            }
            case REACH -> {
                if (stage.index == 0) {
                    return null;
                }
                return first(stage, Trigger.Kind.REACH, want.target, st, made);
            }
            case PUZZLE -> {
                return stage.index == 0 ? null : want;
            }
            default -> {
                return null;
            }
        }
    }

    private static Trigger first(QuestSpec.Stage stage, Trigger.Kind kind, String target, State st, Set<String> made) {
        for (Trigger t : stage.triggers) {
            if (t.kind == kind && t.target.equals(target) && enabled(t, st, made)) {
                return t;
            }
        }
        return null;
    }

    /** Who shows a puzzle trigger to this player: its room variant's NPC, else the trigger's target. */
    public static String presenter(Trigger t, String player) {
        if (t.kind != Trigger.Kind.PUZZLE) {
            return t.target;
        }
        String npc = t.puzzle.variant(QuestPuzzle.seed(player, t.puzzle.key, 0), t.slot).npc();
        return npc != null ? npc : t.target;
    }

    private static String target(Trigger t, String player) {
        return t.kind == Trigger.Kind.PUZZLE ? presenter(t, player) : t.target;
    }

    private static String action(Trigger t) {
        return switch (t.kind) {
            case TALK -> "talking to " + t.target;
            case KILL, DROP -> "killing " + t.target;
            case REACH -> "arriving at " + t.target;
            case PUZZLE -> "the puzzle at " + t.target;
        };
    }

    /** QuestInterpreter.skipped: an item or flag that switches the trigger off, or a drop already collected. */
    static boolean skipped(Trigger t, State st) {
        for (String item : t.unlessItems) {
            if (st.has(item) >= t.unlessCount(item)) {
                return true;
            }
        }
        for (String f : t.unlessFlags) {
            if (st.flags.contains(f)) {
                return true;
            }
        }
        if (t.kind == Trigger.Kind.DROP) {
            for (String item : t.give.keySet()) {
                if (st.has(item) >= Math.max(1, t.count)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Not skipped and every condition met: the quest flags set and the quest items carried (outside items can be gathered). */
    static boolean enabled(Trigger t, State st, Set<String> made) {
        if (skipped(t, st)) {
            return false;
        }
        for (String f : t.needFlags) {
            if (!st.flags.contains(f) && !st.outside.contains(f)) {
                return false;
            }
        }
        for (Map<String, Integer> m : List.of(t.need, t.take)) {
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                if (made.contains(e.getKey()) && st.has(e.getKey()) < e.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether firing moves anything on: the stage, a new flag, or an item not yet carried. */
    static boolean progress(Trigger t, State st, Set<String> made) {
        if (t.next != t.stage) {
            return true;
        }
        for (String f : t.setFlags) {
            if (!st.flags.contains(f)) {
                return true;
            }
        }
        if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(st.flags::contains)) {
            return true;
        }
        for (String item : t.give.keySet()) {
            if (st.has(item) < (t.kind == Trigger.Kind.DROP ? Math.max(1, t.count) : 1)) {
                return true;
            }
        }
        return false;
    }

    private static void apply(Trigger t, State st, String player) {
        t.take.forEach((item, n) -> {
            int left = st.has(item) - n;
            if (left > 0) {
                st.carried.put(item, left);
            } else {
                st.carried.remove(item);
            }
        });
        st.flags.addAll(t.setFlags);
        if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(st.flags::contains)) {
            st.flags.add(QuestSpec.pickFlag(player, t.flagOneOf));
        }
        if (t.kind == Trigger.Kind.DROP) {
            t.give.keySet().forEach(item -> st.carried.put(item, Math.max(st.has(item), Math.max(1, t.count))));
        } else {
            t.give.forEach((item, n) -> st.carried.merge(item, n, Integer::sum));
        }
        st.fires.merge(t, 1, Integer::sum);
        if (t.next != t.stage) {
            st.stage = t.next;
        } else if (st.stage == 0) {
            st.stage = 1; // QuestApi.start puts a started quest at stage 1 at least
        }
    }

    /** Why nothing in a stage can fire: each trigger's first blocker. */
    private static String blockers(QuestSpec.Stage stage, State st, Set<String> made) {
        List<String> why = new ArrayList<>();
        for (Trigger t : stage.triggers) {
            String reason = null;
            if (skipped(t, st)) {
                reason = "switched off";
            } else {
                for (String f : t.needFlags) {
                    if (reason == null && !st.flags.contains(f) && !st.outside.contains(f)) {
                        reason = "needs flag " + f + " (" + (setter(stage, f) ? "set in this stage" : "set by nothing before") + ")";
                    }
                }
                for (Map<String, Integer> m : List.of(t.need, t.take)) {
                    for (Map.Entry<String, Integer> e : m.entrySet()) {
                        if (reason == null && made.contains(e.getKey()) && st.has(e.getKey()) < e.getValue()) {
                            reason = "needs quest item " + e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : "");
                        }
                    }
                }
                if (reason == null && !progress(t, st, made)) {
                    reason = "no progress left";
                }
            }
            why.add(QuestWalk.describe(t) + ": " + (reason == null ? "?" : reason));
        }
        return why.isEmpty() ? "" : " [" + String.join("; ", why) + "]";
    }

    private static boolean setter(QuestSpec.Stage stage, String flag) {
        return stage.triggers.stream().anyMatch(t -> t.setFlags.contains(flag) || t.flagOneOf.contains(flag));
    }

    private static Plan finish(QuestSpec q, List<Step> steps, String stuck, State st) {
        List<Trigger> dead = new ArrayList<>();
        for (Trigger t : q.triggers()) {
            boolean matters = t.next != t.stage || !t.setFlags.isEmpty() || !t.flagOneOf.isEmpty() || !t.give.isEmpty();
            if (matters && !st.fires.containsKey(t)) {
                dead.add(t);
            }
        }
        return new Plan(List.copyOf(steps), stuck, List.copyOf(dead));
    }
}
