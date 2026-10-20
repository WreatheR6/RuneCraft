package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The master and late experienced quests' steps, puzzles and boss rules against the OSRS Wiki and quest-helper. */
class MasterQuestsTest {
    private static final QuestBook BOOK = QuestBook.get();

    private static QuestSpec spec(String id) {
        QuestSpec q = BOOK.spec(id);
        assertNotNull(q, id);
        return q;
    }

    /** The strict walk's steps as "kind target" in order. */
    private static List<String> walk(String id) {
        QuestStrictWalk.Plan plan = QuestStrictWalk.plan(spec(id), "tester");
        assertTrue(plan.completes(), plan.stuck());
        return plan.steps().stream().map(s -> s.kind().name().toLowerCase(java.util.Locale.ROOT) + " " + s.target()).toList();
    }

    /** {@code beats} appear in the walk in this order (other steps may come between them). */
    private static void inOrder(String id, String... beats) {
        List<String> steps = walk(id);
        int at = 0;
        for (String beat : beats) {
            int found = -1;
            for (int i = at; i < steps.size(); i++) {
                if (steps.get(i).equals(beat)) {
                    found = i;
                    break;
                }
            }
            assertTrue(found >= 0, id + ": no \"" + beat + "\" after step " + at + " in " + steps);
            at = found + 1;
        }
    }

    // ------------------------------------------------------------------ puzzles

    @Test
    void slidingBoardsSolveInTheirShortestMoves() {
        int[] board = {0, 1, 3, 5, 2, 7, 4, 8, 6};
        List<List<Integer>> sols = QuestPuzzle.shortestSlides(board);
        assertFalse(sols.isEmpty());
        for (List<Integer> s : sols) {
            int[] b = board;
            for (int m : s) {
                b = QuestPuzzle.slideOnce(b, m);
                assertNotNull(b);
            }
            assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 0}, b);
        }
        // the gap in the top-left corner has nothing above it or left of it to slide in
        assertNull(QuestPuzzle.slideOnce(board, 0));
        assertNull(QuestPuzzle.slideOnce(board, 2));
        assertEquals(10, sols.get(0).size());
        for (QuestPuzzle.Variant v : KandarinGnomeQuests.MM_PANEL.rooms.get(0).variants()) {
            assertEquals(4, v.options().size());
            assertEquals(1, v.check(v.solutions().get(0)));
        }
    }

    // ------------------------------------------------------------------ Desert Treasure I

    @Test
    void desertTreasureWalksTheWikiRoute() {
        inOrder("desert_treasure_i", "talk archaeologist", "talk terry_balando", "talk archaeologist", "talk dt_bartender", "talk eblis",
                "reach dt_mirrors",
                "reach smoke_dungeon", "kill fareed",
                "talk rasolo", "reach bandit_camp", "talk rasolo", "kill damis",
                "talk troll_child", "kill ice_troll", "kill kamil", "reach ice_path", "talk dt_troll_mother",
                "talk malak", "talk ruantun", "talk high_priest_entrana", "talk malak", "reach dessous_graveyard", "kill dessous", "talk malak",
                "reach quest_jaldraocht_pyramid", "reach quest_jaldraocht_pyramid", "talk azzanadra");
    }

    @Test
    void desertTreasureBossRules() {
        assertTrue(QuestBossRules.spellHurts("fareed", "water"));
        assertFalse(QuestBossRules.spellHurts("fareed", "fire"));
        assertFalse(QuestBossRules.spellHurts("fareed", null));
        assertTrue(QuestBossRules.spellHurts("kamil", "fire"));
        assertFalse(QuestBossRules.spellHurts("kamil", "water"));
        assertTrue(QuestBossRules.spellHurts("damis", null));
        // Damis: two forms (DPS calculator), no immunities; his first form is the BossDef
        BossDef damis = ContentRegistry.boss("damis");
        assertEquals(QuestBossRules.DAMIS_FIRST[0], damis.combatLevel);
        assertEquals(QuestBossRules.DAMIS_FIRST[1], damis.hitpoints);
        assertTrue(damis.phases.isEmpty(), "the second form is a transformation, not a damage phase");
        assertEquals(List.of(174, 200), List.of(QuestBossRules.DAMIS_SECOND[0], QuestBossRules.DAMIS_SECOND[1]));
        // Dessous: melee 19 and the bat swarm
        BossDef dessous = ContentRegistry.boss("dessous");
        assertTrue(dessous.attacks.stream().anyMatch(a -> a.name().equals("bat_swarm") && a.maxHit() == QuestBossRules.DESSOUS_BATS_MAX));
        assertTrue(dessous.attacks.stream().anyMatch(a -> a.name().equals("vampyric_slash") && a.maxHit() == QuestBossRules.DESSOUS_MELEE_MAX));
        // the pot of blood is made once: the chain stops once it has been poured
        QuestSpec q = spec("desert_treasure_i");
        for (Trigger t : q.stages.get(2).triggers) {
            if (t.give.containsKey("dt_silver_pot") || t.give.containsKey("dt_blessed_pot") || t.give.containsKey("dt_pot_of_blood")) {
                assertTrue(t.unlessFlags.contains("dt_blood_poured"), QuestWalk.describe(t));
            }
        }
    }

    // ------------------------------------------------------------------ Dragon Slayer I

    @Test
    void dragonSlayerWalksMelzarsMazeAndTheMagicDoor() {
        inOrder("dragon_slayer_i", "talk guildmaster", "talk oziach", "talk guildmaster",
                "reach melzars_maze", "kill zombie_rat", "kill maze_ghost", "kill maze_skeleton", "kill maze_zombie", "kill melzar_the_mad",
                "kill maze_lesser_demon", "talk wormbrain", "talk oracle", "reach dwarven_mine_north", "talk klarense", "talk klarense", "talk ned",
                "kill elvarg", "talk oziach");
        BossDef elvarg = ContentRegistry.boss("elvarg");
        assertTrue(elvarg.attacks.stream().anyMatch(a -> a.name().equals("dragonfire") && a.maxHit() == QuestBossRules.ELVARG_DRAGONFIRE_MAX
                && a.style() == com.runecraft.osrs.AttackType.RANGED));
        assertTrue(elvarg.phases.isEmpty());
        // the maze's key-holders (DPS calculator levels), each called up by the kill before it
        assertEquals(List.of(19, 22, 24, 82), List.of("maze_ghost", "maze_skeleton", "maze_zombie", "maze_lesser_demon").stream()
                .map(id -> ContentRegistry.boss(id).combatLevel).toList());
        QuestSpec q = spec("dragon_slayer_i");
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("zombie_rat") && "maze_ghost".equals(t.spawn)));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("melzar_the_mad") && "maze_lesser_demon".equals(t.spawn)));
    }

    // ------------------------------------------------------------------ Recipe for Disaster

    @Test
    void recipeForDisasterSubquestsAndFinalFights() {
        inOrder("recipe_for_disaster", "talk rfd_gypsy", "talk rfd_gypsy",
                "talk emily_bartender", "talk rohak", "talk rohak", "talk rohak",
                "talk general_bentnoze",
                "talk pirate_pete", "talk rfd_murphy", "drop mudskipper", "reach rfd_crab_pen", "kill rfd_crab", "talk pirate_pete",
                "puzzle rfd_traiborn", "puzzle rfd_traiborn", "puzzle rfd_traiborn", "talk lumbridge_guide",
                "talk evil_dave", "kill rfd_hell_rat", "puzzle evil_dave",
                "talk skrach_uglogwee", "reach feldip_hills", "kill rfd_jubbly_bird", "talk skrach_uglogwee",
                "talk sir_amik_varze", "reach kharazi_jungle", "kill rfd_evil_chicken", "drop black_dragon", "talk sir_amik_varze",
                "talk king_awowogei", "reach rfd_crash_island_pit", "kill rfd_big_snake", "reach ape_atoll", "talk king_awowogei",
                "talk rfd_gypsy");
        List<String> steps = walk("recipe_for_disaster");
        List<String> fights = steps.stream().filter(s -> s.startsWith("kill ")).map(s -> s.substring(5))
                .filter(QuestBossRules.CULINAROMANCER_FIGHTS::contains).toList();
        assertEquals(QuestBossRules.CULINAROMANCER_FIGHTS, fights);
        // the Culinaromancer's magic: 25, landing one time in three
        BossDef c = ContentRegistry.boss("culinaromancer");
        assertTrue(c.attacks.stream().anyMatch(a -> a.style() == com.runecraft.osrs.AttackType.MAGIC && a.maxHit() == QuestBossRules.CULINAROMANCER_MAGIC_MAX));
        assertTrue(QuestBossRules.culinaromancerHits(0) && !QuestBossRules.culinaromancerHits(1) && !QuestBossRules.culinaromancerHits(2));
        assertTrue(c.phases.isEmpty());
        // Traiborn's three tests and Evil Dave's stew are real puzzles
        for (QuestPuzzle p : List.of(RecipeForDisaster.RFD_TRAIBORN_EGG, RecipeForDisaster.RFD_TRAIBORN_MILK, RecipeForDisaster.RFD_TRAIBORN_FLOUR)) {
            for (QuestPuzzle.Variant v : p.rooms.get(0).variants()) {
                assertEquals(1, v.solutions().size());
                assertEquals(1, v.length());
            }
        }
        assertEquals(10, RecipeForDisaster.RFD_TRAIBORN_EGG.rooms.get(0).variants().size());
        assertEquals(14, RecipeForDisaster.RFD_TRAIBORN_MILK.rooms.get(0).variants().size());
        for (QuestPuzzle.Variant v : RecipeForDisaster.RFD_SPICY_STEW.rooms.get(0).variants()) {
            assertEquals(4, v.length(), "a dose count for each of the four spices");
            assertEquals(3, v.options().size());
        }
    }

    // ------------------------------------------------------------------ Legends' Quest

    @Test
    void legendsQuestWalksTheJungleTheCavesAndThreeDemonFights() {
        inOrder("legends_quest", "talk radimus_erkle", "reach kharazi_jungle", "reach kharazi_jungle", "reach kharazi_jungle",
                "talk jungle_forester", "talk gujuo",
                "reach kharazi_cave", "talk gujuo", "reach lq_water_pool", "reach kharazi_cave", "puzzle kharazi_cave", "talk ungadulu",
                "kill nezikchened", "talk ungadulu",
                "talk gujuo", "reach kharazi_cave", "kill san_tojalon", "kill irvig_senay", "kill ranalph_devere", "reach kharazi_cave",
                "talk echned_zekin", "talk ungadulu", "talk echned_zekin", "kill nezikchened", "reach lq_water_pool", "reach lq_fertile_soil",
                "reach kharazi_jungle", "kill nezikchened", "talk gujuo", "talk radimus_erkle");
        QuestPuzzle.Variant wall = KaramjaQuests.LQ_MARKED_WALL.rooms.get(0).variants().get(0);
        List<String> smell = wall.solutions().get(0).stream().map(i -> wall.options().get(i)).toList();
        assertEquals(List.of("Soul rune", "Mind rune", "Earth rune", "Law rune", "Law rune"), smell);
        assertEquals(List.of(106, 100, 92), List.of("san_tojalon", "irvig_senay", "ranalph_devere").stream()
                .map(id -> ContentRegistry.boss(id).combatLevel).toList());
        assertEquals(9, QuestBossRules.nezikchenedPrayerCap(99));
    }

    // ------------------------------------------------------------------ the elves, Haunted Mine, Lunar Diplomacy

    @Test
    void mourningsEndTwoSolvesTheTempleOfLight() {
        inOrder("mournings_end_part_ii", "talk arianwyn", "talk essyllt", "reach temple_of_light", "talk arianwyn", "reach temple_of_light",
                "puzzle temple_of_light", "puzzle temple_of_light", "puzzle temple_of_light", "puzzle temple_of_light",
                "kill shadow_of_light", "talk arianwyn");
        // chest 1 (wiki): mirror 1 north, 2 west, 3 south, the yellow crystal, mirror 4 east
        QuestPuzzle.Variant v = KandarinMoreQuests.ME2_CHEST_YELLOW.rooms.get(0).variants().get(0);
        assertEquals(List.of("Point it north", "Point it west", "Point it south", "Place the yellow crystal", "Point it east"),
                v.solutions().get(0).stream().map(i -> v.options().get(i)).toList());
        QuestPuzzle.Variant g = KandarinMoreQuests.ME2_CHEST_GREEN.rooms.get(0).variants().get(0);
        assertTrue(g.solutions().get(0).contains(7) && g.solutions().get(0).contains(6), "green light needs the cyan and the yellow crystal");
    }

    @Test
    void rovingElvesTakesTheSeedFromGlarialsTombToTheFalls() {
        inOrder("roving_elves", "talk islwyn", "talk eluned", "kill moss_guardian", "talk eluned", "reach baxtorian_falls", "talk islwyn");
        assertTrue(spec("roving_elves").triggers().stream().anyMatch(t -> t.target.equals("glarials_tomb") && "moss_guardian".equals(t.spawn)
                && t.need.containsKey("glarials_pebble")));
    }

    @Test
    void hauntedMineSetsThePointsAndFightsTreusDayth() {
        inOrder("haunted_mine", "talk zealot", "talk zealot", "reach abandoned_mine", "puzzle abandoned_mine", "reach abandoned_mine",
                "kill treus_dayth", "reach abandoned_mine");
        for (QuestPuzzle.Variant v : MorytaniaQuests.HM_POINTS.rooms.get(0).variants()) {
            List<Integer> s = v.solutions().get(0);
            assertEquals(11, s.get(s.size() - 1), "the cart is started last");
            for (int i = 1; i < s.size() - 1; i++) {
                assertTrue(s.get(i) > s.get(i - 1), "levers are pulled from A to K");
            }
        }
        BossDef treus = ContentRegistry.boss("treus_dayth");
        assertTrue(treus.attacks.stream().anyMatch(a -> a.style() == com.runecraft.osrs.AttackType.RANGED && a.maxHit() == 15));
        assertEquals(2, QuestBossRules.treusShiftOneIn(0.1f));
    }

    @Test
    void lunarDiplomacyRunsTheDreamChallenges() {
        inOrder("lunar_diplomacy", "talk lokar_searunner", "talk brundt", "talk oneiromancer", "drop suqah", "reach AIR_ALTAR", "reach FIRE_ALTAR",
                "reach WATER_ALTAR", "reach EARTH_ALTAR", "reach lunar_isle_mine", "talk pauline_polaris", "talk oneiromancer",
                "puzzle lunar_dream_world", "puzzle lunar_dream_world", "puzzle lunar_dream_world", "puzzle lunar_dream_world",
                "reach lunar_dream_world", "kill me", "talk oneiromancer");
        assertEquals(4, FremennikQuests.LD_DREAM.picks);
        // every accepted dice answer flips dice to the target sum
        for (int target : new int[]{12, 30, 17}) {
            List<List<Integer>> sols = FremennikQuests.diceSolutions(target);
            assertFalse(sols.isEmpty(), "sum " + target);
            for (List<Integer> s : sols) {
                int sum = 0;
                for (int i = 0; i < FremennikQuests.LD_DICE.length; i++) {
                    sum += s.contains(i) ? 7 - FremennikQuests.LD_DICE[i] : FremennikQuests.LD_DICE[i];
                }
                assertEquals(target, sum);
            }
        }
    }

    // ------------------------------------------------------------------ Monkey Madness I

    @Test
    void monkeyMadnessWalksTheWikiRoute() {
        inOrder("monkey_madness_i", "talk king_narnode", "talk glo_caranock", "talk king_narnode", "talk daero",
                "puzzle gnome_glider_hangar", "talk waydar", "talk mm_lumdo",
                "reach ape_atoll_jail", "talk garkor",
                "reach ape_atoll_crates", "reach ape_atoll_crates", "talk zooknock", "reach ape_atoll_temple", "talk monkey_child", "talk zooknock",
                "talk monkey_minder", "talk elder_guard", "talk mm_kruk", "talk king_awowogei",
                "talk garkor", "kill jungle_demon", "talk king_narnode");
        QuestSpec q = spec("monkey_madness_i");
        assertTrue(q.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("jungle_demon")
                && t.need.containsKey("tenth_squad_sigil")));
        assertEquals(20, QuestBossRules.JUNGLE_DEMON_REGEN_PERCENT);
    }
}
