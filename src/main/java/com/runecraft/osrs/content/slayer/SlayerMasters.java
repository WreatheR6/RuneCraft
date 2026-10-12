package com.runecraft.osrs.content.slayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Slayer masters with their real combat requirements, task point values and assignment tables. The tables are
 * the OSRS Wiki's (snapshot of July 2026 in {@code src/test/resources/osrs/slayer/master_assignments.tsv},
 * checked by {@code SlayerWikiSnapshotTest}): weights, amounts, extended amounts, the combat level and quests
 * each task needs, and the reward unlock that enables it. Tasks whose creatures RuneCraft doesn't have yet
 * (aquanites, frost dragons, gryphons...) stay listed; {@link SlayerTasks#available} keeps them from being
 * assigned until such a creature exists.
 */
public final class SlayerMasters {
    /**
     * One line of a master's assignment table.
     *
     * @param unlock    reward unlock needed before this task can be assigned (e.g. "seeing_red"), or null
     * @param extMin    amount range used when the player bought the task's extension unlock (0 = none)
     * @param minCombat combat level the master wants before assigning it
     * @param quests    quests that must be done first; a leading {@code ~} means started (partially completed) is enough
     */
    public record Assignment(String category, int weight, int min, int max, int extMin, int extMax, String unlock,
                             int minCombat, List<String> quests) {
        public Assignment withUnlock(String id) {
            return new Assignment(category, weight, min, max, extMin, extMax, id, minCombat, quests);
        }

        public Assignment ext(int lo, int hi) {
            return new Assignment(category, weight, min, max, lo, hi, unlock, minCombat, quests);
        }

        public Assignment combat(int level) {
            return new Assignment(category, weight, min, max, extMin, extMax, unlock, level, quests);
        }

        public Assignment quests(String... ids) {
            return new Assignment(category, weight, min, max, extMin, extMax, unlock, minCombat, List.of(ids));
        }
    }

    /**
     * @param npcId              NpcDef id of the master's NPC (talk to them for their own tasks)
     * @param combatLevel        combat level needed to be assigned tasks
     * @param slayerLevel        Slayer level needed (Duradel: 50)
     * @param points             base points per task (0 for Turael and Spria)
     * @param wilderness         Krystilia: kills only count in the Wilderness; her tasks keep their own streak
     * @param locationRestricted Konar: kills only count in the assigned region; drops brimstone keys
     * @param quest              quest that must be complete before the master gives tasks (Chaeldar: Lost City,
     *                           Spria: A Porcine of Interest, Steve: Monkey Madness II), or null
     * @param diary              achievement diary area whose elite tier raises the base points (Konar: Kourend &amp;
     *                           Kebos, 18 to 20; Nieve and Steve: Western Provinces, 12 to 15), or null
     * @param diaryPoints        base points with that elite diary done
     */
    public record Master(String id, String name, String npcId, int combatLevel, int slayerLevel, int points,
                         boolean wilderness, boolean locationRestricted, String quest, String diary, int diaryPoints,
                         List<Assignment> assignments) {
        /** Points for completing a task when the streak (including this task) is {@code streak}. */
        public int pointsFor(int streak) {
            return SlayerMasters.points(points, streak);
        }

        /** As {@link #pointsFor(int)}, with the master's elite diary bonus when {@code eliteDiary} is done. */
        public int pointsFor(int streak, boolean eliteDiary) {
            return SlayerMasters.points(eliteDiary && diary != null ? diaryPoints : points, streak);
        }

        /** Turael (not Spria) swaps another master's task for one of his own, resetting the streak. */
        public boolean changesTasks() {
            return id.equals("turael");
        }

        public Assignment assignment(String category) {
            for (Assignment a : assignments) {
                if (a.category().equals(category)) {
                    return a;
                }
            }
            return null;
        }

        /** Weighted random pick among assignments accepted by the filter, or null when none qualify. */
        public Assignment pick(Random random, Predicate<Assignment> allowed) {
            return pick(random, allowed, java.util.Set.of());
        }

        /** As {@link #pick(Random, Predicate)}, applying weight unlocks ("Chance of Heavy Frost"). */
        public Assignment pick(Random random, Predicate<Assignment> allowed, java.util.Set<String> unlocks) {
            List<Assignment> ok = new ArrayList<>();
            int total = 0;
            for (Assignment a : assignments) {
                if (allowed.test(a)) {
                    ok.add(a);
                    total += SlayerTasks.weight(a, unlocks);
                }
            }
            if (total <= 0) {
                return null;
            }
            int roll = random.nextInt(total);
            for (Assignment a : ok) {
                roll -= SlayerTasks.weight(a, unlocks);
                if (roll < 0) {
                    return a;
                }
            }
            return ok.get(ok.size() - 1);
        }
    }

    private static final Map<String, Master> ALL = new LinkedHashMap<>();

    private SlayerMasters() {
    }

    private static Assignment a(String category, int weight, int min, int max) {
        return new Assignment(category, weight, min, max, 0, 0, null, 0, List.of());
    }

    private static void master(String id, String name, String npcId, int combat, int slayer, int points, boolean wildy,
                               boolean konar, String quest, String diary, int diaryPoints, Assignment... list) {
        ALL.put(id, new Master(id, name, npcId, combat, slayer, points, wildy, konar, quest, diary, diaryPoints,
                List.of(list)));
    }

    private static Assignment[] nieveTasks() {
        return new Assignment[]{
                a("aberrant_spectres", 6, 120, 185).ext(200, 250).combat(65).quests("priest_in_peril"),
                a("abyssal_demons", 9, 120, 185).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("ankou", 5, 50, 90).ext(91, 150).combat(40),
                a("araxytes", 8, 40, 60).ext(200, 250).quests("priest_in_peril"),
                a("aviansies", 6, 120, 185).ext(200, 250).withUnlock("watch_the_birdie"),
                a("aquanites", 5, 40, 60).ext(150, 200).withUnlock("lured_in"),
                a("basilisks", 6, 120, 185).ext(200, 250).withUnlock("basilocked"),
                a("black_demons", 9, 120, 185).ext(200, 250).combat(80),
                a("black_dragons", 6, 10, 20).ext(40, 60).combat(80).quests("~dragon_slayer_i"),
                a("bloodveld", 9, 120, 185).ext(200, 250).combat(50).quests("priest_in_peril"),
                a("blue_dragons", 4, 120, 185).combat(65).quests("~dragon_slayer_i"),
                a(SlayerBossTasks.CATEGORY, 8, 3, 35).withUnlock(SlayerBossTasks.UNLOCK),
                a("brine_rats", 3, 120, 185).combat(45).quests("olafs_quest"),
                a("cave_horrors", 5, 120, 180).ext(200, 250).combat(85).quests("cabin_fever"),
                a("cave_kraken", 6, 100, 120).ext(150, 200).combat(80),
                a("custodian_stalkers", 8, 110, 170).ext(200, 250).quests("shadows_of_custodia"),
                a("dagannoth", 8, 120, 185).combat(75).quests("horror_from_the_deep"),
                a("dark_beasts", 5, 10, 20).ext(110, 135).combat(90).quests("~mournings_end_part_ii"),
                a("drakes", 7, 30, 95),
                a("dust_devils", 6, 120, 185).ext(200, 250).combat(70).quests("~desert_treasure_i"),
                a("elves", 4, 60, 90).combat(70).quests("regicide"),
                a("fire_giants", 9, 120, 185).combat(65),
                a("fossil_island_wyverns", 5, 5, 25).ext(55, 75).combat(60).quests("bone_voyage", "elemental_workshop_i"),
                a("frost_dragons", 5, 60, 100).ext(180, 240).combat(85),
                a("gargoyles", 6, 120, 185).ext(200, 250).combat(80).quests("priest_in_peril"),
                a("greater_demons", 7, 120, 185).ext(200, 250).combat(75),
                a("gryphons", 7, 110, 170).ext(190, 250).withUnlock("wings_spread").quests("troubled_tortugans"),
                a("hellhounds", 8, 120, 185).combat(75),
                a("kalphite", 9, 120, 185).combat(15),
                a("kurask", 3, 120, 185).combat(65),
                a("lizardmen", 8, 90, 120).withUnlock("reptile_got_ripped"),
                a("metal_dragons", 12, 30, 40).ext(150, 200).quests("~dragon_slayer_i"),
                a("mutated_zygomites", 2, 10, 25).combat(60).quests("lost_city"),
                a("nechryael", 7, 110, 170).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("red_dragons", 5, 30, 80).combat(68).withUnlock("seeing_red").quests("~dragon_slayer_i"),
                a("scabarites", 4, 30, 60).ext(130, 170).combat(85).quests("~contact"),
                a("skeletal_wyverns", 5, 5, 15).ext(50, 70).combat(70).quests("elemental_workshop_i"),
                a("smoke_devils", 7, 120, 185).combat(85),
                a("spiritual_creatures", 6, 120, 185).ext(181, 250).combat(60).quests("death_plateau"),
                a("suqahs", 8, 120, 185).ext(186, 250).combat(85).quests("~lunar_diplomacy"),
                a("trolls", 6, 120, 185).combat(60),
                a("turoth", 3, 120, 185).combat(60),
                a("tzhaar", 10, 110, 180).withUnlock("hot_stuff"),
                a("vampyres", 6, 110, 170).ext(200, 250).withUnlock("actual_vampyre_slayer"),
                a("warped_creatures", 6, 120, 185).withUnlock("warped_reality"),
                a("wyrms", 7, 80, 145).ext(200, 250)
        };
    }

    /**
     * OSRS points per task: base, x5 every 10th task, x15 every 50th, x25 every 100th, x35 every
     * 250th and x50 every 1000th. No points are awarded for the first four tasks.
     */
    public static int points(int base, int streak) {
        if (base <= 0 || streak < 5) {
            return 0;
        }
        if (streak % 1000 == 0) {
            return base * 50;
        }
        if (streak % 250 == 0) {
            return base * 35;
        }
        if (streak % 100 == 0) {
            return base * 25;
        }
        if (streak % 50 == 0) {
            return base * 15;
        }
        if (streak % 10 == 0) {
            return base * 5;
        }
        return base;
    }

    static {
        master("turael", "Turael", "turael", 3, 1, 0, false, false, null, null, 0,
                a("banshees", 8, 15, 30).combat(20).quests("priest_in_peril"),
                a("bats", 7, 15, 30).combat(5),
                a("bears", 7, 10, 20).combat(13),
                a("birds", 6, 15, 30),
                a("cave_bugs", 8, 10, 30),
                a("cave_crawlers", 8, 15, 30).combat(10),
                a("cave_slime", 8, 10, 20).combat(15),
                a("cows", 8, 15, 30),
                a("crawling_hands", 8, 15, 30).quests("priest_in_peril"),
                a("dogs", 7, 15, 30).combat(15),
                a("dwarves", 7, 10, 25).combat(6),
                a("ghosts", 7, 15, 30).combat(13),
                a("goblins", 7, 15, 30),
                a("icefiends", 8, 15, 20).combat(20),
                a("kalphite", 6, 15, 30).combat(15),
                a("lizards", 8, 15, 30),
                a("minotaurs", 7, 10, 20).combat(7),
                a("monkeys", 6, 15, 30),
                a("rats", 7, 15, 30),
                a("scorpions", 7, 15, 30).combat(7),
                a("skeletons", 7, 15, 30).combat(15),
                a("spiders", 6, 15, 30),
                a("wolves", 7, 15, 30).combat(20),
                a("zombies", 7, 15, 30).combat(10));
        master("spria", "Spria", "spria", 3, 1, 0, false, false, "a_porcine_of_interest", null, 0,
                a("banshees", 8, 15, 30).combat(20).quests("priest_in_peril"),
                a("bats", 7, 15, 30).combat(5),
                a("bears", 7, 10, 20).combat(13),
                a("birds", 6, 15, 30),
                a("cave_bugs", 8, 10, 30),
                a("cave_crawlers", 8, 15, 30).combat(10),
                a("cave_slime", 8, 10, 20).combat(15),
                a("cows", 8, 15, 30).combat(5),
                a("crawling_hands", 8, 15, 30).quests("priest_in_peril"),
                a("dogs", 7, 15, 30).combat(15),
                a("dwarves", 7, 10, 25).combat(6),
                a("ghosts", 7, 15, 30).combat(13),
                a("goblins", 7, 15, 30),
                a("icefiends", 8, 15, 20).combat(20),
                a("kalphite", 6, 15, 30).combat(15),
                a("lizards", 8, 15, 30),
                a("minotaurs", 7, 10, 20).combat(7),
                a("monkeys", 6, 15, 30),
                a("rats", 7, 15, 30),
                a("scorpions", 7, 15, 30).combat(7),
                a("skeletons", 7, 15, 30).combat(15),
                a("sourhogs", 6, 15, 25).quests("a_porcine_of_interest"),
                a("spiders", 6, 15, 30),
                a("wolves", 7, 15, 30).combat(20),
                a("zombies", 7, 15, 30).combat(10));
        master("mazchna", "Mazchna", "mazchna", 20, 1, 6, false, false, null, null, 0,
                a("banshees", 8, 30, 50).combat(20).quests("priest_in_peril"),
                a("bats", 7, 30, 50).combat(5),
                a("bears", 6, 30, 50).combat(13),
                a("catablepon", 8, 20, 30).combat(35),
                a("cave_bugs", 8, 10, 20),
                a("cave_crawlers", 8, 30, 50).combat(10),
                a("cave_slime", 8, 10, 20).combat(15),
                a("cockatrice", 8, 30, 50).combat(25),
                a("crabs", 8, 30, 50),
                a("crawling_hands", 8, 30, 50).quests("priest_in_peril"),
                a("dogs", 7, 30, 50).combat(15),
                a("flesh_crawlers", 7, 15, 25).combat(15),
                a("ghosts", 7, 30, 50).combat(13),
                a("ghouls", 7, 10, 20).combat(25).quests("priest_in_peril"),
                a("hill_giants", 7, 30, 50).combat(25),
                a("hobgoblins", 7, 30, 50).combat(20),
                a("ice_warriors", 7, 40, 50).combat(45),
                a("kalphite", 6, 30, 50).combat(15),
                a("killerwatts", 6, 30, 50).combat(50).quests("ernest_the_chicken"),
                a("lizards", 8, 30, 50),
                a("mogres", 8, 30, 50).combat(30).quests("skippy_and_the_mogres"),
                a("pyrefiends", 8, 30, 50).combat(25),
                a("rockslugs", 8, 30, 50).combat(20),
                a("scorpions", 7, 30, 50).combat(7),
                a("shades", 8, 30, 70).combat(30),
                a("skeletons", 7, 30, 50).combat(15),
                a("vampyres", 6, 10, 20).ext(200, 250).combat(35).quests("priest_in_peril"),
                a("wall_beasts", 7, 10, 20).combat(30),
                a("wolves", 7, 30, 50).combat(20),
                a("zombies", 7, 30, 50).combat(10));
        master("vannaka", "Vannaka", "vannaka", 40, 1, 8, false, false, null, null, 0,
                a("aberrant_spectres", 8, 40, 90).ext(200, 250).combat(65).quests("priest_in_peril"),
                a("abyssal_demons", 5, 40, 90).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("ankou", 8, 25, 35).ext(91, 150).combat(40),
                a("basilisks", 8, 40, 90).ext(200, 250).combat(40),
                a("bloodveld", 8, 40, 90).ext(200, 250).combat(50).quests("priest_in_peril"),
                a("blue_dragons", 7, 40, 90).combat(65).quests("~dragon_slayer_i"),
                a("brine_rats", 7, 40, 90).combat(45).quests("~olafs_quest"),
                a("cockatrice", 8, 40, 90).combat(25),
                a("crabs", 8, 40, 90),
                a("crocodiles", 6, 40, 90).combat(50),
                a("dagannoth", 7, 40, 90).combat(75).quests("horror_from_the_deep"),
                a("dust_devils", 8, 40, 90).ext(200, 250).combat(70).quests("~desert_treasure_i"),
                a("elves", 7, 30, 70).combat(70).quests("regicide"),
                a("fever_spiders", 7, 30, 90).combat(40).quests("~rum_deal"),
                a("fire_giants", 7, 40, 90).combat(65),
                a("gargoyles", 5, 40, 90).ext(200, 250).combat(80).quests("priest_in_peril"),
                a("ghouls", 7, 10, 40).combat(25).quests("priest_in_peril"),
                a("gryphons", 10, 30, 80).ext(110, 160).quests("troubled_tortugans"),
                a("harpie_bug_swarms", 8, 40, 90).combat(45),
                a("hellhounds", 7, 30, 60).combat(75),
                a("hill_giants", 7, 40, 90).combat(25),
                a("hobgoblins", 7, 40, 90).combat(20),
                a("ice_giants", 7, 30, 80).combat(50),
                a("ice_warriors", 7, 40, 90).combat(45),
                a("infernal_mages", 8, 40, 90).combat(40).quests("priest_in_peril"),
                a("jellies", 8, 40, 90).combat(57),
                a("jungle_horrors", 8, 40, 90).combat(65).quests("cabin_fever"),
                a("kalphite", 7, 40, 90).combat(15),
                a("kurask", 7, 40, 90).combat(65),
                a("lesser_demons", 7, 40, 90).combat(60),
                a("mogres", 7, 40, 90).combat(30).quests("skippy_and_the_mogres"),
                a("molanisks", 7, 40, 50).combat(50).quests("death_to_the_dorgeshuun"),
                a("moss_giants", 7, 40, 90).combat(40),
                a("nechryael", 5, 40, 90).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("ogres", 7, 40, 90).combat(40),
                a("otherworldly_beings", 8, 40, 90).combat(40).quests("lost_city"),
                a("pyrefiends", 8, 40, 90).combat(25),
                a("sea_snakes", 6, 40, 90).combat(50).quests("royal_trouble"),
                a("shades", 8, 40, 90).combat(30),
                a("shadow_warriors", 8, 30, 80).combat(60).quests("legends_quest"),
                a("spiritual_creatures", 8, 40, 90).ext(181, 250).combat(60).quests("death_plateau"),
                a("terror_dogs", 6, 20, 45).combat(60).quests("haunted_mine"),
                a("trolls", 7, 40, 90).combat(60),
                a("turoth", 8, 30, 90).combat(60),
                a("vampyres", 7, 10, 20).ext(200, 250).combat(35).quests("priest_in_peril"),
                a("werewolves", 7, 30, 60).combat(60).quests("priest_in_peril"));
        master("chaeldar", "Chaeldar", "chaeldar", 70, 1, 10, false, false, "lost_city", null, 0,
                a("aberrant_spectres", 8, 70, 130).ext(200, 250).combat(65).quests("priest_in_peril"),
                a("abyssal_demons", 12, 70, 130).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("aviansies", 9, 70, 130).ext(130, 250).withUnlock("watch_the_birdie"),
                a("basilisks", 7, 70, 130).ext(200, 250).combat(40),
                a("black_demons", 10, 70, 130).ext(200, 250).combat(80),
                a("bloodveld", 8, 70, 130).ext(200, 250).combat(50).quests("priest_in_peril"),
                a("blue_dragons", 8, 70, 130).combat(65).quests("~dragon_slayer_i"),
                a("brine_rats", 7, 70, 130).combat(45).quests("olafs_quest"),
                a("cave_horrors", 10, 70, 130).ext(200, 250).combat(85).quests("cabin_fever"),
                a("cave_kraken", 12, 30, 50).ext(150, 200).combat(80),
                a("crabs", 8, 70, 130),
                a("custodian_stalkers", 11, 70, 130).ext(200, 250).quests("shadows_of_custodia"),
                a("dagannoth", 11, 70, 130).combat(75).quests("horror_from_the_deep"),
                a("dust_devils", 9, 70, 130).ext(200, 250).combat(70).quests("~desert_treasure_i"),
                a("elves", 8, 70, 130).combat(70).quests("regicide"),
                a("fever_spiders", 7, 70, 130).combat(40).quests("~rum_deal"),
                a("fire_giants", 12, 70, 130).combat(65),
                a("fossil_island_wyverns", 7, 10, 20).ext(55, 75).combat(60).quests("bone_voyage", "elemental_workshop_i"),
                a("gargoyles", 11, 70, 130).ext(200, 250).combat(80).quests("priest_in_peril"),
                a("greater_demons", 9, 70, 130).ext(200, 250).combat(70),
                a("gryphons", 10, 60, 100).ext(140, 180).quests("troubled_tortugans"),
                a("hellhounds", 9, 70, 130).combat(75),
                a("jellies", 10, 70, 130).combat(57),
                a("jungle_horrors", 10, 70, 130).combat(65).quests("cabin_fever"),
                a("kalphite", 11, 70, 130).combat(15),
                a("kurask", 12, 70, 130).combat(65),
                a("lesser_demons", 9, 70, 130).combat(60),
                a("lesser_nagua", 4, 50, 100).quests("perilous_moons"),
                a("lizardmen", 8, 50, 90).withUnlock("reptile_got_ripped"),
                a("mutated_zygomites", 7, 8, 15).combat(60).quests("lost_city"),
                a("nechryael", 12, 70, 130).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("shadow_warriors", 8, 70, 130).combat(60).quests("legends_quest"),
                a("skeletal_wyverns", 7, 10, 20).ext(50, 70).combat(70).quests("elemental_workshop_i"),
                a("spiritual_creatures", 12, 70, 130).ext(181, 250).combat(60).quests("death_plateau"),
                a("trolls", 11, 70, 130).combat(60),
                a("turoth", 10, 70, 130).combat(60),
                a("tzhaar", 8, 90, 150).withUnlock("hot_stuff"),
                a("vampyres", 6, 80, 100).ext(200, 250).withUnlock("actual_vampyre_slayer"),
                a("warped_creatures", 6, 70, 130).withUnlock("warped_reality"),
                a("wyrms", 6, 60, 100).ext(200, 250));
        master("konar", "Konar quo Maten", "konar", 75, 1, 18, false, true, null, "kourend", 20,
                a("aberrant_spectres", 6, 120, 170).ext(200, 250).combat(65),
                a("abyssal_demons", 9, 120, 170).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("ankou", 5, 50, 50).ext(91, 150).combat(40),
                a("aviansies", 6, 120, 170).ext(200, 250).withUnlock("watch_the_birdie"),
                a("basilisks", 5, 110, 170).ext(200, 250).withUnlock("basilocked"),
                a("black_demons", 9, 120, 170).ext(200, 250).combat(80),
                a("black_dragons", 6, 10, 15).ext(40, 60).combat(80).quests("~dragon_slayer_i"),
                a("bloodveld", 9, 120, 170).ext(200, 250).combat(50),
                a("blue_dragons", 4, 120, 170).combat(65).quests("~dragon_slayer_i"),
                a(SlayerBossTasks.CATEGORY, 8, 3, 35).withUnlock(SlayerBossTasks.UNLOCK),
                a("brine_rats", 2, 120, 170).combat(45).quests("olafs_quest"),
                a("cave_kraken", 9, 80, 100).ext(150, 200).combat(80),
                a("dagannoth", 8, 120, 170).combat(75).quests("horror_from_the_deep"),
                a("dark_beasts", 5, 10, 15).ext(110, 135).combat(90).quests("~mournings_end_part_ii"),
                a("drakes", 10, 75, 140),
                a("dust_devils", 6, 120, 170).ext(200, 250).combat(70).quests("~desert_treasure_i"),
                a("fire_giants", 9, 120, 170).combat(65),
                a("fossil_island_wyverns", 5, 15, 30).ext(55, 75).combat(60).quests("bone_voyage", "elemental_workshop_i"),
                a("gargoyles", 6, 120, 170).ext(200, 250).combat(80).quests("priest_in_peril"),
                a("greater_demons", 7, 120, 170).ext(200, 250).combat(75),
                a("hellhounds", 8, 120, 170).combat(75),
                a("hydras", 10, 125, 190),
                a("jellies", 6, 120, 170).combat(57),
                a("kalphite", 9, 120, 170).combat(15),
                a("kurask", 3, 120, 170).combat(65),
                a("lesser_nagua", 2, 55, 120).quests("perilous_moons"),
                a("lizardmen", 8, 90, 110).withUnlock("reptile_got_ripped"),
                a("metal_dragons", 15, 30, 40).ext(150, 200).quests("~dragon_slayer_i"),
                a("mutated_zygomites", 2, 10, 25).combat(60).quests("lost_city"),
                a("nechryael", 7, 110, 110).ext(200, 250).combat(85),
                a("red_dragons", 5, 30, 50).combat(68).withUnlock("seeing_red").quests("~dragon_slayer_i"),
                a("skeletal_wyverns", 5, 5, 12).ext(50, 70).combat(70).quests("elemental_workshop_i"),
                a("smoke_devils", 7, 120, 170).combat(85),
                a("trolls", 6, 120, 170).combat(60),
                a("turoth", 3, 120, 170).combat(60),
                a("vampyres", 4, 100, 160).ext(200, 250).withUnlock("actual_vampyre_slayer"),
                a("warped_creatures", 4, 110, 170).withUnlock("warped_reality"),
                a("waterfiends", 2, 120, 170).combat(75),
                a("wyrms", 10, 125, 190).ext(200, 250));
        master("nieve", "Nieve", "nieve", 85, 1, 12, false, false, null, "western", 15, nieveTasks());
        // Steve replaces Nieve after Monkey Madness II and assigns the same tasks.
        master("steve", "Steve", "steve", 85, 1, 12, false, false, "monkey_madness_ii", "western", 15, nieveTasks());
        master("duradel", "Duradel", "duradel", 100, 50, 15, false, false, null, null, 0,
                a("aberrant_spectres", 7, 130, 200).ext(200, 250).combat(65).quests("priest_in_peril"),
                a("abyssal_demons", 12, 130, 200).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("ankou", 5, 50, 80).ext(91, 150).combat(40),
                a("aquanites", 5, 30, 50).ext(150, 200).withUnlock("lured_in"),
                a("araxytes", 10, 60, 80).ext(200, 250).quests("priest_in_peril"),
                a("aviansies", 8, 120, 200).ext(200, 250).withUnlock("watch_the_birdie"),
                a("basilisks", 7, 130, 200).ext(200, 250).withUnlock("basilocked"),
                a("black_demons", 8, 130, 200).ext(200, 250).combat(80),
                a("black_dragons", 9, 10, 20).ext(40, 60).combat(80).quests("~dragon_slayer_i"),
                a("bloodveld", 8, 130, 200).ext(200, 250).combat(50).quests("priest_in_peril"),
                a("blue_dragons", 4, 110, 170).combat(65).quests("~dragon_slayer_i"),
                a(SlayerBossTasks.CATEGORY, 12, 3, 35).withUnlock(SlayerBossTasks.UNLOCK),
                a("cave_horrors", 4, 130, 200).ext(200, 250).combat(85).quests("cabin_fever"),
                a("cave_kraken", 9, 100, 120).ext(150, 200).combat(80),
                a("dagannoth", 9, 130, 200).combat(75).quests("horror_from_the_deep"),
                a("dark_beasts", 11, 10, 20).ext(110, 135).combat(90).quests("~mournings_end_part_ii"),
                a("drakes", 8, 50, 110),
                a("dust_devils", 5, 130, 200).ext(200, 250).combat(70).quests("~desert_treasure_i"),
                a("elves", 4, 110, 170).combat(70).quests("regicide"),
                a("fire_giants", 7, 130, 200).combat(65),
                a("fossil_island_wyverns", 7, 20, 50).ext(55, 75).combat(60).quests("bone_voyage", "elemental_workshop_i"),
                a("frost_dragons", 5, 70, 120).ext(180, 240).combat(85),
                a("gargoyles", 8, 130, 200).ext(200, 250).combat(80).quests("priest_in_peril"),
                a("greater_demons", 9, 130, 200).ext(200, 250).combat(75),
                a("gryphons", 7, 100, 210).ext(180, 290).withUnlock("wings_spread").quests("troubled_tortugans"),
                a("hellhounds", 10, 130, 200).combat(75),
                a("kalphite", 9, 130, 200).combat(15),
                a("kurask", 4, 130, 200).combat(65),
                a("lizardmen", 10, 130, 210).withUnlock("reptile_got_ripped"),
                a("metal_dragons", 14, 35, 45).ext(150, 200).quests("~dragon_slayer_i"),
                a("mutated_zygomites", 2, 20, 30).combat(60).quests("lost_city"),
                a("nechryael", 9, 130, 200).ext(200, 250).combat(85).quests("priest_in_peril"),
                a("red_dragons", 8, 30, 65).combat(68).withUnlock("seeing_red").quests("~dragon_slayer_i"),
                a("skeletal_wyverns", 7, 20, 40).ext(50, 70).combat(70).quests("elemental_workshop_i"),
                a("smoke_devils", 9, 130, 200).combat(85),
                a("spiritual_creatures", 7, 130, 200).ext(181, 250).combat(60).quests("death_plateau"),
                a("suqahs", 8, 60, 90).ext(186, 250).combat(85).quests("~lunar_diplomacy"),
                a("trolls", 6, 130, 200).combat(60),
                a("tzhaar", 10, 130, 199).withUnlock("hot_stuff"),
                a("vampyres", 8, 100, 210).ext(200, 250).withUnlock("actual_vampyre_slayer"),
                a("warped_creatures", 8, 130, 200).withUnlock("warped_reality"),
                a("waterfiends", 2, 130, 200).combat(75),
                a("wyrms", 8, 100, 160).ext(200, 250));
        master("krystilia", "Krystilia", "krystilia", 1, 1, 25, true, false, null, null, 0,
                a("abyssal_demons", 5, 75, 125).ext(200, 250).withUnlock("i_wildy_more_slayer"),
                a("ankou", 6, 75, 125).ext(91, 150),
                a("aviansies", 7, 75, 125).ext(200, 250).withUnlock("watch_the_birdie"),
                a("bandits", 4, 75, 125),
                a("bears", 6, 65, 100),
                a("black_demons", 7, 100, 150).ext(200, 250),
                a("black_dragons", 4, 8, 16).ext(40, 60),
                a("black_knights", 3, 75, 125),
                a("bloodveld", 4, 70, 110).ext(200, 250),
                a("chaos_druids", 5, 50, 90),
                a("dark_warriors", 4, 75, 125),
                a("dust_devils", 5, 75, 125).ext(200, 250).withUnlock("i_wildy_more_slayer"),
                a("earth_warriors", 6, 75, 125),
                a("ents", 5, 35, 60),
                a("fire_giants", 7, 75, 125),
                a("greater_demons", 8, 100, 150).ext(200, 250),
                a("green_dragons", 4, 65, 100),
                a("hellhounds", 7, 75, 125),
                a("hill_giants", 3, 75, 125),
                a("ice_giants", 6, 100, 150),
                a("ice_warriors", 7, 100, 150),
                a("jellies", 5, 100, 150).withUnlock("i_wildy_more_slayer"),
                a("lava_dragons", 3, 35, 60),
                a("lesser_demons", 6, 80, 120),
                a("magic_axes", 7, 75, 125),
                a("mammoths", 6, 75, 125),
                a("moss_giants", 4, 100, 150),
                a("nechryael", 5, 75, 125).ext(200, 250).withUnlock("i_wildy_more_slayer"),
                a("pirates", 3, 62, 75),
                a("revenants", 5, 40, 100).ext(100, 150),
                a("rogues", 5, 75, 125),
                a("scorpions", 6, 65, 100),
                a("skeletons", 5, 65, 100),
                a("spiders", 6, 65, 100),
                a("spiritual_creatures", 6, 100, 150).ext(181, 250),
                a("zombies", 3, 75, 125),
                a(SlayerBossTasks.CATEGORY, 8, 3, 35).withUnlock(SlayerBossTasks.UNLOCK));
    }

    public static Master get(String id) {
        return ALL.get(id);
    }

    public static List<Master> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    /** Masters that the generic "Slayer master" station picks between (Konar and Krystilia are NPC-only). */
    public static Master bestFor(int combatLevel, int slayerLevel) {
        return bestFor(combatLevel, slayerLevel, m -> true);
    }

    /** As {@link #bestFor(int, int)}, skipping masters the player can't use yet (e.g. Chaeldar before Lost City). */
    public static Master bestFor(int combatLevel, int slayerLevel, Predicate<Master> usable) {
        Master best = ALL.get("turael");
        for (String id : List.of("turael", "mazchna", "vannaka", "chaeldar", "nieve", "duradel")) {
            Master m = ALL.get(id);
            if (combatLevel >= m.combatLevel() && slayerLevel >= m.slayerLevel() && usable.test(m)) {
                best = m;
            }
        }
        return best;
    }

    public static Master byNpc(String npcId) {
        for (Master m : ALL.values()) {
            if (m.npcId().equals(npcId)) {
                return m;
            }
        }
        return null;
    }
}
