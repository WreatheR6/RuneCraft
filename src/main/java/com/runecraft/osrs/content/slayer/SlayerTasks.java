package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (category.equals(m.slayerCategory) && Superiors.byId(m.id) == null) {
                return true;
            }
        }
        return false;
    }

    /** Creatures (any module) that count towards a category. */
    public static List<MonsterDef> creatures(String category) {
        List<MonsterDef> list = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (category.equals(m.slayerCategory)) {
                list.add(m);
            }
        }
        return list;
    }

    /** Regions Konar may restrict a task to: where its creatures spawn, else the category's fallback list. */
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
        return new ArrayList<>(set);
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
        SlayerCategories.Category c = SlayerCategories.get(a.category());
        if (c == null || slayerLevel < c.level() || combatLevel < a.minCombat()) {
            return false;
        }
        if (a.unlock() != null && !unlocks.contains(a.unlock())) {
            return false;
        }
        if (blocked.contains(a.category()) || a.category().equals(previous)) {
            return false;
        }
        return available(a.category());
    }

    /** Plural display name of a category ("abyssal demons"). */
    public static String displayName(String category) {
        SlayerCategories.Category c = SlayerCategories.get(category);
        return c == null ? category.replace('_', ' ') : c.name();
    }
}
