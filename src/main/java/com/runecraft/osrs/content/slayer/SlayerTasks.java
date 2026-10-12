package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/** Pure task rules shared by the Minecraft side and the tests. */
public final class SlayerTasks {
    private SlayerTasks() {
    }

    /** True if at least one creature in the game counts towards the category. */
    public static boolean available(String category) {
        SlayerCategories.Category c = SlayerCategories.get(category);
        if (c == null) {
            return false;
        }
        if (!c.builtIns().isEmpty()) {
            return true;
        }
        SlayerBossTasks.BossTask boss = SlayerBossTasks.get(category);
        if (boss != null) {
            return SlayerBossTasks.exists(boss);
        }
        if (category.equals(SlayerBossTasks.CATEGORY)) {
            return SlayerBossTasks.all().stream().anyMatch(SlayerBossTasks::exists);
        }
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (category.equals(categoryOf(m)) && Superiors.byId(m.id) == null) {
                return true;
            }
        }
        return false;
    }

    /** Creatures (any module) that count towards a category. */
    public static List<MonsterDef> creatures(String category) {
        List<MonsterDef> list = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (category.equals(categoryOf(m))) {
                list.add(m);
            }
        }
        return list;
    }

    /** The task category a module creature counts towards (declared, else {@link SlayerCategories#members()}). */
    public static String categoryOf(MonsterDef m) {
        return SlayerCategories.categoryOf(m.id, m.slayerCategory);
    }

    /**
     * Regions Konar may restrict a task to: where its creatures spawn, else the category's fallback list,
     * never the Wilderness. Each must hold at least one of the creatures' spawn points or camps
     * (SpawnTable; a carved dungeon counts as its OSRS kingdom, see {@code TaskRegions}).
     */
    public static List<Region> regions(String category) {
        Set<Region> set = new LinkedHashSet<>();
        for (MonsterDef m : creatures(category)) {
            for (MonsterDef.Spawn s : m.spawns) {
                set.add(s.region());
            }
        }
        SlayerCategories.Category c = SlayerCategories.get(category);
        if (set.isEmpty() && c != null) {
            set.addAll(c.regions());
        }
        // Konar never sends players into the Wilderness (that is Krystilia's job); only boss tasks may go there.
        set.remove(Region.WILDERNESS);
        return new ArrayList<>(set);
    }

    /** An assignment's weight: "Chance of Heavy Frost" raises frost dragons from 5 to 8 (OSRS). */
    public static int weight(SlayerMasters.Assignment a, Set<String> unlocks) {
        if (a.category().equals("frost_dragons") && unlocks.contains("chance_of_heavy_frost")) {
            return 8;
        }
        return a.weight();
    }

    /** Kills one creature counts for: two for the Grotesque Guardians with "Double Trouble" (OSRS). */
    public static int killCredit(String task, Set<String> unlocks) {
        return "boss_grotesque_guardians".equals(task) && unlocks.contains("double_trouble") ? 2 : 1;
    }

    /** The task amount for an assignment, using the extension range when that unlock is owned. */
    public static int amount(SlayerMasters.Assignment a, boolean extended, Random random) {
        int lo = extended && a.extMin() > 0 ? a.extMin() : a.min();
        int hi = extended && a.extMax() > 0 ? a.extMax() : a.max();
        return lo + random.nextInt(Math.max(1, hi - lo + 1));
    }

    /**
     * Whether a player may be assigned this line: Slayer level, combat level, unlocks, block list,
     * and the category actually existing in the game.
     */
    public static boolean eligible(SlayerMasters.Assignment a, int slayerLevel, int combatLevel, Set<String> unlocks,
                                   List<String> blocked, String previous) {
        return eligible(a, slayerLevel, combatLevel, unlocks, blocked, previous, q -> true);
    }

    /**
     * As above, also requiring the task's quests: {@code questMet} gets each requirement as listed on the
     * assignment ({@code "priest_in_peril"}, or {@code "~dragon_slayer_i"} when starting the quest is enough).
     */
    public static boolean eligible(SlayerMasters.Assignment a, int slayerLevel, int combatLevel, Set<String> unlocks,
                                   List<String> blocked, String previous, Predicate<String> questMet) {
        SlayerCategories.Category c = SlayerCategories.get(a.category());
        if (c == null || slayerLevel < c.level() || combatLevel < a.minCombat()) {
            return false;
        }
        for (String q : a.quests()) {
            if (!questMet.test(q)) {
                return false;
            }
        }
        if (a.unlock() != null && !unlocks.contains(a.unlock())) {
            return false;
        }
        if (blocked.contains(a.category()) || a.category().equals(previous)) {
            return false;
        }
        // "Stop the Wyvern" removes Fossil Island wyvern tasks without using a block slot.
        if (a.category().equals("fossil_island_wyverns") && unlocks.contains("stop_the_wyvern")) {
            return false;
        }
        if (a.category().equals(SlayerBossTasks.CATEGORY)) {
            return !SlayerBossTasks.choices(slayerLevel, false, blocked, previous).isEmpty();
        }
        return available(a.category());
    }

    /**
     * Whether killing a creature counts towards the task: its own slayer category (or listed membership)
     * matches, or the task is a boss task for that boss (Cerberus counts for both hellhounds and a
     * Cerberus boss task).
     */
    public static boolean countsFor(String task, String creatureId, String declaredCategory) {
        if (task == null) {
            return false;
        }
        if (task.equals(SlayerCategories.categoryOf(creatureId, declaredCategory))) {
            return true;
        }
        return SlayerBossTasks.counts(task, creatureId);
    }

    /** Plural display name of a category ("abyssal demons"). */
    public static String displayName(String category) {
        SlayerCategories.Category c = SlayerCategories.get(category);
        return c == null ? category.replace('_', ' ') : c.name();
    }
}
