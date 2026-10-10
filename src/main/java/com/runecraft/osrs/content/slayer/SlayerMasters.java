package com.runecraft.osrs.content.slayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Slayer masters with their real combat requirements, task point values and assignment tables
 * (weights and amounts from the OSRS wiki; a few rare/quest-locked entries are left out).
 */
public final class SlayerMasters {
    /**
     * One line of a master's assignment table.
     *
     * @param unlock reward unlock needed before this task can be assigned (e.g. "seeing_red"), or null
     * @param extMin amount range used when the player bought the task's extension unlock (0 = none)
     */
    public record Assignment(String category, int weight, int min, int max, int extMin, int extMax, String unlock,
                             int minCombat) {
        public Assignment withUnlock(String id) {
            return new Assignment(category, weight, min, max, extMin, extMax, id, minCombat);
        }

        public Assignment ext(int lo, int hi) {
            return new Assignment(category, weight, min, max, lo, hi, unlock, minCombat);
        }

        public Assignment combat(int level) {
            return new Assignment(category, weight, min, max, extMin, extMax, unlock, level);
        }
    }

    /**
     * @param npcId            NpcDef id of the master's NPC (talk to them for their own tasks)
     * @param combatLevel      combat level needed to be assigned tasks
     * @param slayerLevel      Slayer level needed (Duradel: 50)
     * @param points           base points per task (0 for Turael)
     * @param wilderness       Krystilia: kills only count in the Wilderness
     * @param locationRestricted Konar: kills only count in the assigned region; drops brimstone keys
     */
    public record Master(String id, String name, String npcId, int combatLevel, int slayerLevel, int points,
                         boolean wilderness, boolean locationRestricted, List<Assignment> assignments) {
        /** Points for completing a task when the streak (including this task) is {@code streak}. */
        public int pointsFor(int streak) {
            return SlayerMasters.points(points, streak);
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
            List<Assignment> ok = new ArrayList<>();
            int total = 0;
            for (Assignment a : assignments) {
                if (allowed.test(a)) {
                    ok.add(a);
                    total += a.weight();
                }
            }
            if (total <= 0) {
                return null;
            }
            int roll = random.nextInt(total);
            for (Assignment a : ok) {
                roll -= a.weight();
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
        return new Assignment(category, weight, min, max, 0, 0, null, 0);
    }

    private static void master(String id, String name, String npcId, int combat, int slayer, int points, boolean wildy,
                               boolean konar, Assignment... list) {
        ALL.put(id, new Master(id, name, npcId, combat, slayer, points, wildy, konar, List.of(list)));
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
        master("turael", "Turael", "turael", 3, 1, 0, false, false,
                a("bats", 7, 15, 50), a("bears", 7, 10, 20), a("birds", 6, 15, 50), a("cave_bugs", 8, 10, 30),
                a("cave_crawlers", 8, 15, 50), a("cave_slime", 8, 10, 20), a("cows", 8, 15, 50),
                a("crawling_hands", 8, 15, 50), a("ghosts", 7, 15, 50), a("goblins", 7, 15, 50),
                a("icefiends", 8, 15, 20), a("kalphite", 6, 15, 50), a("lizards", 8, 15, 50),
                a("minotaurs", 7, 10, 20), a("monkeys", 6, 15, 50), a("rats", 7, 15, 50),
                a("scorpions", 7, 15, 50), a("skeletons", 7, 15, 50), a("spiders", 6, 15, 50),
                a("wolves", 7, 15, 50), a("zombies", 7, 15, 50));
        master("mazchna", "Mazchna", "mazchna", 20, 1, 6, false, false,
                a("banshees", 8, 40, 70), a("bats", 7, 40, 70), a("bears", 6, 40, 70), a("cave_bugs", 8, 10, 20),
                a("cave_crawlers", 8, 40, 70), a("cave_slime", 8, 10, 20), a("cockatrice", 8, 40, 70),
                a("crawling_hands", 8, 40, 70), a("lizards", 8, 40, 70), a("earth_warriors", 6, 40, 70),
                a("ghosts", 7, 40, 70), a("ghouls", 7, 10, 20), a("hill_giants", 7, 40, 70),
                a("hobgoblins", 7, 40, 70), a("ice_warriors", 7, 40, 70), a("kalphite", 6, 40, 70),
                a("killerwatts", 6, 30, 80), a("mogres", 8, 40, 70), a("pyrefiends", 8, 40, 70),
                a("rockslugs", 8, 40, 70), a("scorpions", 7, 40, 70), a("skeletons", 7, 40, 70),
                a("wall_beasts", 7, 10, 20), a("wolves", 7, 40, 70), a("zombies", 7, 40, 70),
                a("vampyres", 6, 10, 20));
        master("vannaka", "Vannaka", "vannaka", 40, 1, 8, false, false,
                a("aberrant_spectres", 8, 40, 90), a("abyssal_demons", 5, 40, 90), a("ankou", 7, 25, 35),
                a("basilisks", 8, 40, 90), a("bloodveld", 8, 40, 90), a("blue_dragons", 7, 40, 90),
                a("brine_rats", 7, 40, 90), a("cockatrice", 8, 40, 90), a("dagannoth", 7, 40, 90),
                a("dust_devils", 8, 40, 90), a("earth_warriors", 6, 40, 90), a("elves", 7, 30, 70),
                a("fever_spiders", 7, 30, 90), a("fire_giants", 7, 40, 90), a("gargoyles", 5, 40, 90),
                a("ghouls", 7, 10, 40), a("green_dragons", 6, 40, 90), a("harpie_bug_swarms", 8, 40, 90),
                a("hellhounds", 7, 30, 60), a("hill_giants", 7, 40, 90), a("hobgoblins", 7, 40, 90),
                a("ice_giants", 7, 30, 80), a("ice_warriors", 7, 40, 90), a("infernal_mages", 8, 40, 90),
                a("jellies", 8, 40, 90), a("kalphite", 7, 40, 90), a("killerwatts", 6, 30, 80),
                a("kurask", 7, 40, 90), a("lesser_demons", 7, 40, 90), a("mogres", 7, 40, 90),
                a("molanisks", 7, 39, 50), a("moss_giants", 7, 40, 90), a("nechryael", 5, 40, 90),
                a("ogres", 7, 40, 90), a("pyrefiends", 8, 40, 90), a("sea_snakes", 6, 40, 90),
                a("spiritual_creatures", 8, 40, 90).combat(60), a("terror_dogs", 6, 20, 45), a("trolls", 7, 40, 90),
                a("turoth", 8, 30, 90), a("vampyres", 7, 10, 20), a("wall_beasts", 6, 10, 20),
                a("cave_crawlers", 7, 40, 90), a("crawling_hands", 7, 40, 90));
        master("chaeldar", "Chaeldar", "chaeldar", 70, 1, 10, false, false,
                a("aberrant_spectres", 8, 70, 130).ext(200, 250), a("abyssal_demons", 12, 70, 130).ext(200, 250),
                a("aviansies", 9, 70, 130).withUnlock("watch_the_birdie"),
                a("basilisks", 7, 70, 130).ext(200, 250), a("black_demons", 10, 70, 130).ext(200, 250),
                a("bloodveld", 8, 70, 130).ext(200, 250), a("blue_dragons", 8, 70, 130),
                a("brine_rats", 7, 70, 130), a("cave_horrors", 10, 70, 130).ext(200, 250),
                a("cave_kraken", 12, 30, 50).ext(150, 200), a("dagannoth", 11, 70, 130),
                a("dust_devils", 9, 70, 130).ext(200, 250), a("elves", 8, 70, 130), a("fever_spiders", 7, 70, 130),
                a("fire_giants", 12, 70, 130), a("fossil_island_wyverns", 7, 10, 20).ext(55, 75),
                a("gargoyles", 11, 70, 130).ext(200, 250), a("greater_demons", 9, 70, 130).ext(150, 200),
                a("hellhounds", 9, 70, 130), a("infernal_mages", 7, 70, 130), a("jellies", 10, 70, 130),
                a("kalphite", 11, 70, 130), a("kurask", 12, 70, 130), a("lesser_demons", 9, 70, 130),
                a("lizardmen", 8, 50, 110).withUnlock("reptile_got_ripped"), a("mutated_zygomites", 7, 8, 15),
                a("nechryael", 12, 70, 130).ext(200, 250), a("skeletal_wyverns", 7, 10, 20).ext(50, 70),
                a("spiritual_creatures", 12, 70, 130).ext(181, 250), a("trolls", 11, 70, 130),
                a("turoth", 10, 70, 130), a("tzhaar", 8, 90, 150).withUnlock("hot_stuff"),
                a("wyrms", 6, 60, 120));
        master("konar", "Konar quo Maten", "konar", 75, 1, 18, false, true,
                a("aberrant_spectres", 6, 120, 170).ext(200, 250), a("abyssal_demons", 9, 120, 170).ext(200, 250),
                a("adamant_dragons", 5, 3, 6).ext(20, 30), a("ankou", 5, 50, 50).ext(90, 150),
                a("basilisks", 5, 110, 170).ext(200, 250), a("black_demons", 9, 120, 170).ext(200, 250),
                a("black_dragons", 6, 10, 15).ext(40, 60), a("bloodveld", 9, 120, 170).ext(200, 250),
                a("blue_dragons", 4, 120, 170), a("brine_rats", 2, 120, 170), a("bronze_dragons", 5, 30, 50).ext(30, 50),
                a("cave_kraken", 9, 80, 100).ext(150, 200), a("dagannoth", 8, 120, 170),
                a("dark_beasts", 5, 10, 15).ext(110, 135), a("drakes", 10, 75, 140), a("dust_devils", 6, 120, 170).ext(200, 250),
                a("fire_giants", 9, 120, 170), a("gargoyles", 6, 120, 170).ext(200, 250),
                a("greater_demons", 7, 120, 170).ext(150, 200), a("hellhounds", 8, 120, 170),
                a("hydras", 10, 125, 190), a("iron_dragons", 5, 30, 50).ext(60, 100), a("jellies", 6, 120, 170),
                a("kalphite", 9, 120, 170), a("kurask", 3, 120, 170), a("lizardmen", 8, 90, 110).withUnlock("reptile_got_ripped"),
                a("mithril_dragons", 5, 3, 6).ext(20, 30).withUnlock("i_hope_you_mith_me"),
                a("mutated_zygomites", 2, 10, 25), a("nechryael", 7, 110, 110).ext(200, 250),
                a("red_dragons", 5, 30, 50), a("rune_dragons", 5, 3, 6).ext(30, 60),
                a("skeletal_wyverns", 5, 5, 12).ext(50, 70), a("smoke_devils", 7, 120, 170),
                a("spiritual_creatures", 4, 120, 170).ext(181, 250), a("steel_dragons", 5, 30, 50).ext(40, 60),
                a("trolls", 6, 120, 170), a("turoth", 3, 120, 170), a("waterfiends", 2, 120, 170),
                a("wyrms", 10, 125, 190), a("basilisk_knights", 5, 60, 70).withUnlock("basilocked"));
        master("nieve", "Nieve", "nieve", 85, 1, 12, false, false,
                a("aberrant_spectres", 6, 120, 185).ext(200, 250), a("abyssal_demons", 9, 120, 185).ext(200, 250),
                a("adamant_dragons", 2, 3, 7).ext(20, 30), a("ankou", 5, 50, 90).ext(90, 150),
                a("aviansies", 6, 120, 185).withUnlock("watch_the_birdie"), a("basilisks", 6, 120, 180).ext(200, 250),
                a("black_demons", 9, 120, 185).ext(200, 250), a("black_dragons", 6, 10, 20).ext(40, 60),
                a("bloodveld", 9, 120, 185).ext(200, 250), a("blue_dragons", 4, 120, 185),
                a("brine_rats", 3, 120, 185), a("cave_horrors", 5, 120, 180).ext(200, 250),
                a("cave_kraken", 6, 100, 120).ext(150, 200), a("dagannoth", 8, 120, 185),
                a("dark_beasts", 5, 10, 20).ext(110, 135), a("drakes", 7, 30, 95), a("dust_devils", 6, 120, 185).ext(200, 250),
                a("elves", 4, 60, 90), a("fire_giants", 9, 120, 185), a("fossil_island_wyverns", 5, 5, 25).ext(55, 75),
                a("gargoyles", 6, 120, 185).ext(200, 250), a("greater_demons", 7, 120, 185).ext(150, 200),
                a("hellhounds", 8, 120, 185), a("hydras", 10, 125, 190), a("iron_dragons", 5, 25, 60).ext(60, 100),
                a("kalphite", 9, 120, 185), a("kurask", 3, 120, 185), a("lizardmen", 8, 90, 120).withUnlock("reptile_got_ripped"),
                a("mithril_dragons", 5, 4, 9).ext(20, 30).withUnlock("i_hope_you_mith_me"),
                a("mutated_zygomites", 2, 10, 25), a("nechryael", 7, 110, 170).ext(200, 250),
                a("red_dragons", 5, 30, 80).withUnlock("seeing_red"), a("rune_dragons", 2, 3, 8).ext(30, 60),
                a("skeletal_wyverns", 5, 5, 15).ext(50, 70), a("smoke_devils", 7, 120, 185),
                a("spiritual_creatures", 6, 120, 185).ext(181, 250), a("steel_dragons", 5, 30, 60).ext(40, 60),
                a("trolls", 6, 120, 185), a("turoth", 3, 120, 185), a("tzhaar", 10, 110, 180).withUnlock("hot_stuff"),
                a("wyrms", 7, 80, 145), a("basilisk_knights", 5, 60, 70).withUnlock("basilocked"));
        master("duradel", "Duradel", "duradel", 100, 50, 15, false, false,
                a("aberrant_spectres", 7, 130, 200).ext(200, 250), a("abyssal_demons", 12, 130, 200).ext(200, 250),
                a("adamant_dragons", 2, 4, 9).ext(20, 30), a("ankou", 5, 50, 80).ext(91, 150),
                a("aviansies", 8, 120, 200).withUnlock("watch_the_birdie"), a("basilisks", 7, 130, 200).ext(200, 250),
                a("black_demons", 8, 130, 200).ext(200, 250), a("black_dragons", 9, 10, 20).ext(40, 60),
                a("bloodveld", 8, 130, 200).ext(200, 250), a("blue_dragons", 4, 110, 170),
                a("cave_horrors", 4, 130, 200).ext(200, 250), a("cave_kraken", 9, 130, 200).ext(150, 200),
                a("dagannoth", 9, 130, 200), a("dark_beasts", 11, 10, 20).ext(110, 135), a("drakes", 8, 50, 110),
                a("dust_devils", 5, 130, 200).ext(200, 250), a("elves", 4, 110, 170), a("fire_giants", 7, 130, 200),
                a("fossil_island_wyverns", 7, 20, 50).ext(55, 75), a("gargoyles", 8, 130, 200).ext(200, 250),
                a("greater_demons", 9, 130, 200).ext(150, 200), a("hellhounds", 10, 130, 200), a("hydras", 10, 125, 190),
                a("iron_dragons", 5, 40, 60).ext(60, 100), a("kalphite", 9, 130, 200), a("kurask", 4, 130, 200),
                a("lizardmen", 10, 130, 210).withUnlock("reptile_got_ripped"),
                a("mithril_dragons", 9, 5, 10).ext(20, 30).withUnlock("i_hope_you_mith_me"),
                a("mutated_zygomites", 2, 20, 30), a("nechryael", 9, 130, 200).ext(200, 250),
                a("red_dragons", 8, 30, 65).withUnlock("seeing_red"), a("rune_dragons", 2, 3, 8).ext(30, 60),
                a("skeletal_wyverns", 7, 20, 40).ext(50, 70), a("smoke_devils", 9, 130, 200),
                a("spiritual_creatures", 7, 130, 200).ext(181, 250), a("steel_dragons", 7, 10, 20).ext(40, 60),
                a("trolls", 6, 130, 200), a("tzhaar", 10, 130, 199).withUnlock("hot_stuff"),
                a("waterfiends", 2, 130, 200), a("wyrms", 8, 100, 160), a("basilisk_knights", 8, 60, 70).withUnlock("basilocked"));
        master("krystilia", "Krystilia", "krystilia", 1, 1, 25, true, false,
                a("ankou", 6, 75, 125), a("bears", 6, 65, 100), a("black_demons", 7, 100, 150),
                a("black_dragons", 4, 8, 16), a("dust_devils", 5, 75, 125), a("earth_warriors", 6, 75, 125),
                a("fire_giants", 7, 75, 125), a("greater_demons", 8, 100, 150), a("green_dragons", 4, 65, 100),
                a("hellhounds", 7, 75, 125), a("hill_giants", 3, 75, 125), a("ice_giants", 6, 100, 150),
                a("ice_warriors", 7, 100, 150), a("jellies", 5, 100, 150), a("lava_dragons", 3, 35, 60),
                a("lesser_demons", 6, 80, 120), a("moss_giants", 4, 60, 100), a("nechryael", 5, 75, 125),
                a("revenants", 5, 40, 100), a("scorpions", 6, 65, 100), a("skeletons", 5, 65, 100),
                a("spiders", 6, 65, 100), a("spiritual_creatures", 6, 100, 150), a("abyssal_demons", 5, 75, 125),
                a("zombies", 3, 75, 125), a("bats", 4, 65, 100), a("hobgoblins", 4, 75, 125));
    }

    public static Master get(String id) {
        return ALL.get(id);
    }

    public static List<Master> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    /** Masters that the generic "Slayer master" station picks between (Konar and Krystilia are NPC-only). */
    public static Master bestFor(int combatLevel, int slayerLevel) {
        Master best = ALL.get("turael");
        for (String id : List.of("turael", "mazchna", "vannaka", "chaeldar", "nieve", "duradel")) {
            Master m = ALL.get(id);
            if (combatLevel >= m.combatLevel() && slayerLevel >= m.slayerLevel()) {
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
