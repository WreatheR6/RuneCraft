package com.runecraft.osrs.content.slayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Mortimer's task choice (OSRS Wiki "Mortimer" and "Slayer Master", July 2026; Wyrmscraig, 29 July 2026). Instead of one
 * task he offers two (three once 100 of his tasks are done), never the same monster twice, each with a "Mortifier": a
 * modifier that makes it worth more. His tasks give no Slayer points except through the points modifier; skipping one
 * costs 100 points and he has only two block slots, at 120 points each.
 *
 * <p>The wiki gives the modifiers and when each unlocks but not their sizes ("X"): the ranges below are guesses (approx).
 * The clue scroll and superior unique table modifiers are left out until their numbers are known (and those drops belong
 * to other modules).
 */
public final class Mortimer {
    public static final String ID = "mortimer";
    public static final String QUEST = "fallen_from_grace";
    public static final int SKIP_COST = 100;
    public static final int BLOCK_COST = 120;
    public static final int BLOCK_SLOTS = 2;
    /** His tasks done before he offers a third choice. */
    public static final int THIRD_CHOICE_TASKS = 100;

    /** A modifier: when it unlocks (his tasks done) and its range. */
    public enum Modifier {
        /** "Task awards X Slayer Points on completion." approx: 15-30. */
        POINTS(0, 15, 30, "awards %d Slayer points on completion"),
        /** "Task quantity modified by X%": fewer monsters to kill. approx: 10-30%. */
        QUANTITY(0, 10, 30, "has %d%% fewer monsters"),
        /** "Slayer creatures award X% more Slayer XP" (50 tasks). approx: 5-20%. */
        XP(50, 5, 20, "gives %d%% more Slayer experience");

        private final int unlockTasks;
        private final int min;
        private final int max;
        private final String text;

        Modifier(int unlockTasks, int min, int max, String text) {
            this.unlockTasks = unlockTasks;
            this.min = min;
            this.max = max;
            this.text = text;
        }

        public int unlockTasks() {
            return unlockTasks;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        public String describe(int value) {
            return String.format(text, value);
        }
    }

    /** One offered task: its category, size, modifier and the modifier's value. */
    public record Offer(String category, int amount, Modifier modifier, int value) {
    }

    private Mortimer() {
    }

    /** Tasks he offers at once: two, three after {@link #THIRD_CHOICE_TASKS} of his tasks. */
    public static int choices(int tasksDone) {
        return tasksDone >= THIRD_CHOICE_TASKS ? 3 : 2;
    }

    /** The modifiers unlocked after {@code tasksDone} of his tasks. */
    public static List<Modifier> unlocked(int tasksDone) {
        List<Modifier> out = new ArrayList<>();
        for (Modifier m : Modifier.values()) {
            if (tasksDone >= m.unlockTasks()) {
                out.add(m);
            }
        }
        return out;
    }

    /** A task's size with its modifier: the quantity modifier takes its percentage off, at least one monster. */
    public static int amount(int base, Modifier modifier, int value) {
        return modifier == Modifier.QUANTITY ? Math.max(1, base * (100 - value) / 100) : base;
    }

    /** Slayer experience of a kill with the task's modifier. */
    public static double xp(double base, Modifier modifier, int value) {
        return modifier == Modifier.XP ? base * (100 + value) / 100.0 : base;
    }

    /** Points for completing the task: only the points modifier gives any. */
    public static int points(Modifier modifier, int value) {
        return modifier == Modifier.POINTS ? value : 0;
    }

    /** The modifier named {@code name}, or null. */
    public static Modifier modifier(String name) {
        if (name == null) {
            return null;
        }
        for (Modifier m : Modifier.values()) {
            if (m.name().equals(name)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Rolls his offer: up to {@link #choices} different tasks the player may be given ({@code eligible}), each with a
     * modifier unlocked by {@code tasksDone}; {@code extended} says which tasks the player has extended.
     */
    public static List<Offer> offers(Random random, Predicate<SlayerMasters.Assignment> eligible, Set<String> unlocks,
                                     int tasksDone, Predicate<SlayerMasters.Assignment> extended) {
        SlayerMasters.Master m = SlayerMasters.get(ID);
        List<Modifier> mods = unlocked(tasksDone);
        Set<String> taken = new HashSet<>();
        List<Offer> out = new ArrayList<>();
        for (int i = 0; i < choices(tasksDone); i++) {
            SlayerMasters.Assignment a = m.pick(random, x -> !taken.contains(x.category()) && eligible.test(x), unlocks);
            if (a == null) {
                break;
            }
            taken.add(a.category());
            Modifier mod = mods.get(random.nextInt(mods.size()));
            int value = mod.min() + random.nextInt(mod.max() - mod.min() + 1);
            int base = SlayerTasks.amount(a, extended.test(a), random);
            out.add(new Offer(a.category(), amount(base, mod, value), mod, value));
        }
        return out;
    }
}
