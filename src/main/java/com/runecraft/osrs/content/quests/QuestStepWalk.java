package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The step walk (round 37): how the self-test ({@code quest:steps:<id>}) walks a quest the way a real player does, every
 * objective in order, side objectives included, earning each flag and quest item instead of being handed them (unlike
 * {@link QuestWalk}, which only fires each stage's advancing trigger). The same rule is planned here statically
 * ({@link #stepPlan}) so a unit test finds broken chains before CI does.
 */
public final class QuestStepWalk {
    private QuestStepWalk() {
    }

    /**
     * Quests the self-test walks step by step ({@code quest:steps:<id>}, round 37): every objective in order as a real
     * player does it, side objectives included, earning each flag and quest item instead of being handed them, talking
     * through the real dialogue, arriving at each place, fighting each foe and solving each puzzle.
     */
    public static final List<String> STEP_WALKED = List.of("while_guthix_sleeps", "dragon_slayer_ii", "monkey_madness_ii",
            "song_of_the_elves", "sins_of_the_father", "desert_treasure_ii", "secrets_of_the_north");

    /** What the step walk sees of the player: their quest flags and how many of an item they carry. */
    public interface Player {
        boolean flag(String flag);

        int carried(String item);
    }

    /**
     * Flags the quest needs but never sets: earned in another quest (Recipe for Disaster's freed Awowogei for Monkey Madness
     * II). Its prerequisites are done, so the step walk starts with these set.
     */
    public static Set<String> outsideFlags(QuestSpec q) {
        Set<String> set = new LinkedHashSet<>();
        Set<String> needed = new LinkedHashSet<>();
        for (Trigger t : q.triggers()) {
            set.addAll(t.setFlags);
            set.addAll(t.flagOneOf);
            needed.addAll(t.needFlags);
        }
        needed.removeAll(set);
        return needed;
    }

    /** Items the quest hands out itself (some trigger gives them): the step walk must earn these, never be handed them. */
    public static Set<String> questItems(QuestSpec q) {
        Set<String> items = new LinkedHashSet<>();
        for (Trigger t : q.triggers()) {
            items.addAll(t.give.keySet());
        }
        items.remove("coins");
        return items;
    }

    /**
     * Why a trigger can't fire for the step walk now, or null: it is switched off (an unless flag or item), a flag isn't
     * earned yet, or a quest item isn't. Items from outside the quest (tools, runes, coins) are the player's to bring.
     */
    public static String blocked(Trigger t, Player p, Set<String> questItems) {
        for (String f : t.unlessFlags) {
            if (p.flag(f)) {
                return "switched off by flag " + f;
            }
        }
        for (String item : t.unlessItems) {
            if (p.carried(item) > 0) {
                return "switched off while carrying " + item;
            }
        }
        if (t.kind == Trigger.Kind.DROP) {
            for (String item : t.give.keySet()) {
                if (p.carried(item) >= Math.max(1, t.count)) {
                    return "already carries " + item;
                }
            }
        }
        for (String f : t.needFlags) {
            if (!p.flag(f)) {
                return "needs flag " + f;
            }
        }
        for (Map<String, Integer> items : List.of(t.need, t.take)) {
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (questItems.contains(e.getKey()) && p.carried(e.getKey()) < e.getValue()) {
                    return "needs quest item " + e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : "");
                }
            }
        }
        return null;
    }

    /** Whether firing a side objective now moves the quest on: it sets a new flag, hands out an item or raises a foe. */
    public static boolean progresses(Trigger t, Player p) {
        for (String f : t.setFlags) {
            if (!p.flag(f)) {
                return true;
            }
        }
        if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(p::flag)) {
            return true;
        }
        return !t.give.isEmpty() || t.spawn != null || t.travel != null;
    }

    /**
     * The step a player takes next at a stage: the first side objective (one that stays at the stage) that can fire, moves
     * the quest on and hasn't fired on this visit, else the first trigger that can fire and leaves the stage; null if none.
     */
    public static Trigger nextStep(QuestSpec.Stage stage, Player p, Set<String> questItems, Set<Trigger> firedHere) {
        for (Trigger t : stage.triggers) {
            if (t.next == t.stage && !firedHere.contains(t) && blocked(t, p, questItems) == null && progresses(t, p)) {
                return t;
            }
        }
        for (Trigger t : stage.triggers) {
            if (t.next != t.stage && blocked(t, p, questItems) == null) {
                return t;
            }
        }
        return null;
    }

    /** Why no step can fire at a stage: each trigger that leaves it and what blocks it. */
    public static String whyStuck(QuestSpec.Stage stage, Player p, Set<String> questItems) {
        List<String> why = new ArrayList<>();
        for (Trigger t : stage.triggers) {
            if (t.next != t.stage) {
                why.add(QuestWalk.describe(t) + ": " + blocked(t, p, questItems));
            }
        }
        return why.isEmpty() ? "no trigger leaves it" : String.join("; ", why);
    }

    /**
     * The trigger talking to {@code t}'s NPC really fires when the player carries only the quest items earned so far and
     * the outside items {@code t} asks for: the dialogue takes the first satisfiable trigger for that NPC (a puzzle it
     * presents first). The walk expects {@code t} itself.
     */
    public static Trigger dispatched(QuestSpec.Stage stage, Trigger t, Player p, Set<String> questItems) {
        for (Trigger o : stage.triggers) {
            if (o.kind == Trigger.Kind.PUZZLE && o.target.equals(t.target) && blocked(o, p, questItems) == null) {
                return o;
            }
        }
        for (Trigger o : stage.triggers) {
            if (o.kind != Trigger.Kind.TALK || !o.target.equals(t.target) || blocked(o, p, questItems) != null) {
                continue;
            }
            if (o == t || outsideItemsCovered(o, t, questItems)) {
                return o;
            }
        }
        return null;
    }

    /** Whether the outside items and tools {@code o} asks for are among what {@code t} has the walk bring. */
    private static boolean outsideItemsCovered(Trigger o, Trigger t, Set<String> questItems) {
        for (Map<String, Integer> items : List.of(o.need, o.take)) {
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (questItems.contains(e.getKey())) {
                    continue;
                }
                int brought = t.need.getOrDefault(e.getKey(), 0) + t.take.getOrDefault(e.getKey(), 0);
                if (brought < e.getValue()) {
                    return false;
                }
            }
        }
        return t.tools.containsAll(o.tools);
    }

    /**
     * A planned step walk: the steps in order, why it gets stuck (null if it completes), steps it would skip, and the
     * triggers it left unfired because they were switched off by then (fallbacks for older saves, steps another route
     * already covered).
     */
    public record StepPlan(List<Trigger> steps, String stuck, List<String> problems, Set<Trigger> switchedOff) {
        public boolean completes() {
            return stuck == null;
        }
    }

    /**
     * Plans the step walk statically, keeping the player's flags and quest items as the triggers would. {@code itemExists}
     * says whether an item id exists; {@code raisable} whether a kill target can be met without a trigger raising it (a
     * creature of the world, or a quest boss with an arena and summoning stone): a quest boss without one must have been
     * raised by an earlier step. {@code player} picks the per-player routes ({@link QuestSpec#flagOneOf}).
     */
    public static StepPlan stepPlan(QuestSpec q, String player, Predicate<String> itemExists, Predicate<String> raisable) {
        Set<String> flags = new LinkedHashSet<>(outsideFlags(q));
        Map<String, Integer> items = new java.util.HashMap<>();
        Map<String, Integer> raised = new java.util.HashMap<>();
        Player view = new Player() {
            @Override
            public boolean flag(String flag) {
                return flags.contains(flag);
            }

            @Override
            public int carried(String item) {
                return items.getOrDefault(item, 0);
            }
        };
        Set<String> questItems = questItems(q);
        List<Trigger> steps = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Set<Trigger> switchedOff = new LinkedHashSet<>();
        Set<Trigger> firedHere = new LinkedHashSet<>();
        int total = q.triggers().size();
        int s = 0;
        for (int guard = 0; guard < total * 3 + q.stages.size() + 5; guard++) {
            QuestSpec.Stage stage = q.stages.get(s);
            Trigger t = nextStep(stage, view, questItems, firedHere);
            if (t == null) {
                return new StepPlan(steps, "stage " + s + ": no step can fire (" + whyStuck(stage, view, questItems) + ")", problems,
                        switchedOff);
            }
            String where = "stage " + s + " step " + (steps.size() + 1) + " (" + QuestWalk.describe(t) + ")";
            for (Map<String, Integer> m : List.of(t.need, t.take, t.give)) {
                for (String item : m.keySet()) {
                    if (!exists(item, itemExists)) {
                        return new StepPlan(steps, where + " uses unknown item " + item, problems, switchedOff);
                    }
                }
            }
            for (String tool : t.tools) {
                if (!exists(QuestSpec.toolItem(tool), itemExists)) {
                    return new StepPlan(steps, where + " needs " + tool + " but " + QuestSpec.toolItem(tool) + " does not exist", problems,
                            switchedOff);
                }
            }
            if (t.kind == Trigger.Kind.TALK) {
                Trigger d = dispatched(stage, t, view, questItems);
                if (d != t) {
                    return new StepPlan(steps, where + ": talking to " + t.target + " fires "
                            + (d == null ? "nothing" : "another step first (" + QuestWalk.describe(d) + ", trigger "
                            + stage.triggers.indexOf(d) + ")"), problems, switchedOff);
                }
            }
            if ((t.kind == Trigger.Kind.KILL || t.kind == Trigger.Kind.DROP) && !raisable.test(t.target)
                    && raised.getOrDefault(t.target, 0) <= 0) {
                return new StepPlan(steps, where + ": nothing raises " + t.target + " (no arena to summon it, no earlier step spawns it)",
                        problems, switchedOff);
            }
            // fire it
            t.take.forEach((item, n) -> items.merge(item, -n, Integer::sum));
            flags.addAll(t.setFlags);
            if (!t.flagOneOf.isEmpty() && t.flagOneOf.stream().noneMatch(flags::contains)) {
                flags.add(QuestSpec.pickFlag(player, t.flagOneOf));
            }
            t.give.forEach((item, n) -> items.merge(item, t.kind == Trigger.Kind.DROP ? Math.max(1, t.count) : n, Integer::sum));
            if (t.kind == Trigger.Kind.KILL && raised.getOrDefault(t.target, 0) > 0) {
                raised.merge(t.target, -1, Integer::sum);
            }
            if (t.spawn != null) {
                raised.put(t.spawn, 1); // a spawn step raises its foe once (it's "already nearby" if fired again)
            }
            steps.add(t);
            firedHere.add(t);
            if (t.next == t.stage) {
                continue;
            }
            for (Trigger o : stage.triggers) {
                if (o.next == o.stage && !firedHere.contains(o) && (!o.setFlags.isEmpty() || !o.give.isEmpty())
                        && progresses(o, view) && blocked(o, view, questItems) != null && !blocked(o, view, questItems).startsWith("switched off")
                        && !blocked(o, view, questItems).startsWith("already")) {
                    problems.add("stage " + s + ": side step " + QuestWalk.describe(o) + " (trigger " + stage.triggers.indexOf(o)
                            + ") never fired: " + blocked(o, view, questItems));
                }
            }
            for (Trigger o : stage.triggers) {
                String why = firedHere.contains(o) ? null : blocked(o, view, questItems);
                if (why != null && why.startsWith("switched off")) {
                    switchedOff.add(o);
                }
            }
            if (t.completes()) {
                return new StepPlan(steps, null, problems, switchedOff);
            }
            s = t.next;
            firedHere.clear();
        }
        return new StepPlan(steps, "no completion after " + steps.size() + " steps (now at stage " + s + ")", problems, switchedOff);
    }

    private static boolean exists(String item, Predicate<String> itemExists) {
        return item.equals("coins") || itemExists.test(item);
    }
}
