package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 33: real puzzles and missing steps for ten quests (wiki solutions and steps; stage counts are saved). */
class QuestPuzzlesRound33Test {
    private static QuestSpec spec(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q;
    }

    private static Trigger puzzle(QuestSpec q, QuestPuzzle p) {
        return q.triggers().stream().filter(t -> t.puzzle == p).findFirst().orElseThrow(() -> new AssertionError(q.id() + " lacks " + p.key));
    }

    /** The options a variant's first solution clicks, in order. */
    private static List<String> answer(QuestPuzzle.Variant v) {
        List<String> out = new ArrayList<>();
        v.solutions().get(0).forEach(i -> out.add(v.options().get(i)));
        return out;
    }

    @Test
    void fremennikTrialsPeerAndSwensen() {
        QuestSpec ft = spec("fremennik_trials");
        assertEquals(2, ft.stages.size(), "saved stage count");
        // Peer's door: the wiki's six riddles, four-letter answers
        Set<String> words = new HashSet<>();
        for (QuestPuzzle.Variant v : FremennikQuests.PEER_DOOR.rooms.get(0).variants()) {
            words.add(String.join("", answer(v)));
        }
        assertEquals(Set.of("MIND", "TREE", "LIFE", "FIRE", "TIME", "WIND"), words);
        Trigger door = puzzle(ft, FremennikQuests.PEER_DOOR);
        assertTrue(door.emptyHanded && door.banksItems, "nothing may be taken in; Peer banks it");
        Trigger chest = puzzle(ft, FremennikQuests.PEER_CHEST);
        assertTrue(chest.emptyHanded && chest.needFlags.contains("ft_peer_door") && chest.setFlags.contains("ft_peer"));
        // the chest's answer leaves exactly four litres in the 5-litre bucket
        int bucket = 0;
        int jug = 0;
        for (int o : FremennikQuests.PEER_CHEST.rooms.get(0).variants().get(0).solutions().get(0)) {
            switch (o) {
                case 0 -> bucket = 5;
                case 1 -> jug = 3;
                case 2 -> {
                    int t = Math.min(bucket, 3 - jug);
                    bucket -= t;
                    jug += t;
                }
                case 3 -> {
                    int t = Math.min(jug, 5 - bucket);
                    jug -= t;
                    bucket += t;
                }
                case 4 -> bucket = 0;
                case 5 -> jug = 0;
                case 6 -> assertEquals(4, bucket, "the bucket put on the chest holds four litres");
                default -> throw new AssertionError("jug on the chest");
            }
        }
        // Swensen's maze spells his name
        StringBuilder way = new StringBuilder();
        answer(FremennikQuests.SWENSEN_MAZE.rooms.get(0).variants().get(0)).forEach(o -> way.append(o.charAt(0)));
        assertEquals("SWENSEN", way.toString());
        assertEquals("swensens_maze", puzzle(ft, FremennikQuests.SWENSEN_MAZE).target);
    }

    @Test
    void misthalinMysteryRiddlesAndMirror() {
        QuestSpec mm = spec("misthalin_mystery");
        assertEquals(4, mm.stages.size(), "saved stage count");
        assertEquals(List.of("D", "E", "A", "D"), answer(F2pQuests.MM_PIANO.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Sapphire", "Diamond", "Zenyte", "Emerald", "Onyx", "Ruby"), answer(F2pQuests.MM_HEARTH.rooms.get(0).variants().get(0)));
        assertTrue(puzzle(mm, F2pQuests.MM_PIANO).needFlags.contains("mm_ruby"));
        assertTrue(puzzle(mm, F2pQuests.MM_HEARTH).needFlags.contains("mm_emerald"));
        // the stage moves on once the sapphire key opens the westernmost door
        assertTrue(mm.stages.get(1).triggers.stream().anyMatch(t -> t.next == 2 && t.needFlags.contains("mm_sapphire")));
        // the showdown raises the unmasked Killer; killing her needs the mirror first
        Trigger mirror = puzzle(mm, F2pQuests.MM_MIRROR);
        assertEquals("misthalin_murderer", mirror.spawn);
        assertTrue(mm.stages.get(2).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.needFlags.contains("mm_mirror") && t.next == 3));
        // Mandy gives the rewards; the wiki lists no coins
        assertTrue(mm.stages.get(3).triggers.stream().anyMatch(t -> t.target.equals("mandy") && t.completes()));
        assertFalse(mm.def.itemRewards().stream().anyMatch(r -> r.startsWith("coins")));
    }

    @Test
    void grandTreeTranslationShipyardAndTuzo() {
        QuestSpec gt = spec("the_grand_tree");
        assertEquals(8, gt.stages.size(), "saved stage count");
        assertEquals(List.of("A man came to me with the King's seal.", "I gave the man Daconia rocks.", "And Daconia rocks will kill the tree!"),
                answer(KandarinGnomeQuests.GT_TRANSLATION.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Ka", "Lu", "Min"), answer(KandarinGnomeQuests.GT_GATE.rooms.get(0).variants().get(0)));
        assertEquals(List.of("Sadly his wife is no longer with us.", "He loves worm holes.", "Anita."),
                answer(KandarinGnomeQuests.GT_FOREMAN.rooms.get(0).variants().get(0)));
        StringBuilder word = new StringBuilder();
        answer(KandarinGnomeQuests.GT_TUZO.rooms.get(0).variants().get(0)).forEach(o -> word.append(o.charAt(o.length() - 1)));
        assertEquals("TUZO", word.toString());
        Trigger translation = puzzle(gt, KandarinGnomeQuests.GT_TRANSLATION);
        assertEquals(3, translation.next, "translating the scroll moves the quest on");
        assertTrue(gt.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("glough_black_demon")
                && t.needFlags.contains("gt_tuzo")));
        assertTrue(gt.triggers().stream().anyMatch(t -> t.target.equals("anita") && t.give.containsKey("glough_key")));
        assertTrue(puzzle(gt, KandarinGnomeQuests.GT_TUZO).skills.getOrDefault(com.runecraft.osrs.Skill.AGILITY, 0) == 25);
        // mid-quest saves past the old demon step still find the daconia rock among the roots
        assertTrue(gt.stages.get(7).triggers.stream().anyMatch(t -> t.kind == Trigger.Kind.REACH && t.give.containsKey("daconia_rock")));
    }

    @Test
    void kingsRansomTrialRebutsTheEvidenceThenTheThread() {
        QuestSpec kr = spec("kings_ransom");
        QuestPuzzle.Variant v = LateQuestsA.KR_TRIAL.rooms.get(0).variants().get(0);
        assertEquals(6, v.solutions().size(), "butler/dagger, handler/poison and maid/night in any order");
        for (List<Integer> s : v.solutions()) {
            assertEquals("Ask your witness about the thread", v.options().get(s.get(3)));
            Set<String> asked = new HashSet<>();
            s.subList(0, 3).forEach(i -> asked.add(v.options().get(i)));
            assertEquals(Set.of("Call the butler: ask about the dagger", "Call the dog handler: ask about the poison",
                    "Call the maid: ask about the night of the murder"), asked);
        }
        Trigger trial = puzzle(kr, LateQuestsA.KR_TRIAL);
        assertTrue(trial.needFlags.contains("kr_guard") && trial.next == 2);
    }

    @Test
    void forsakenTowerFourPuzzlesAndDinhsHammer() {
        QuestSpec fsk = spec("the_forsaken_tower");
        assertEquals(3, fsk.stages.size(), "saved stage count");
        assertEquals("lord_kandur_lovakengj", fsk.stages.get(0).triggers.get(0).target);
        assertEquals("Lady Vulcana Lovakengj", com.runecraft.osrs.content.ContentRegistry.npc("lord_kandur_lovakengj").name(), "OSRS start NPC");
        assertTrue(fsk.def.itemRewards().contains("coins:6000"));
        // the furnace: four gallons in the 5-gallon jug
        int five = 0;
        int eight = 0;
        for (int o : LateQuestsB.FSK_FURNACE.rooms.get(0).variants().get(0).solutions().get(0)) {
            switch (o) {
                case 0 -> five = 5;
                case 1 -> eight = 8;
                case 2 -> {
                    int t = Math.min(five, 8 - eight);
                    five -= t;
                    eight += t;
                }
                case 3 -> {
                    int t = Math.min(eight, 5 - five);
                    eight -= t;
                    five += t;
                }
                case 4 -> five = 0;
                case 5 -> eight = 0;
                case 6 -> assertEquals(4, five);
                default -> throw new AssertionError("the 8-gallon jug poured");
            }
        }
        // the potions: fluid A..E stand at 1, 2, 3, 5, 4 whichever one is the cleansing fluid
        List<QuestPuzzle.Variant> potions = LateQuestsB.FSK_POTION.rooms.get(0).variants();
        assertEquals(5, potions.size());
        Set<Integer> answers = new HashSet<>();
        potions.forEach(p -> answers.add(p.solutions().get(0).get(0)));
        assertEquals(Set.of(0, 1, 2, 3, 4), answers);
        // the pylons: legal Tower of Hanoi moves ending with all four disks on the centre pylon
        List<java.util.Deque<Integer>> pylons = List.of(new java.util.ArrayDeque<>(List.of(1, 2, 3, 4)), new java.util.ArrayDeque<>(),
                new java.util.ArrayDeque<>());
        int[][] moves = {{0, 1}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 1}}; // west, centre, east
        for (int o : LateQuestsB.FSK_PYLONS.rooms.get(0).variants().get(0).solutions().get(0)) {
            int disk = pylons.get(moves[o][0]).pop();
            Integer top = pylons.get(moves[o][1]).peek();
            assertTrue(top == null || top > disk, "a larger disk on a smaller one");
            pylons.get(moves[o][1]).push(disk);
        }
        assertEquals(List.of(1, 2, 3, 4), new ArrayList<>(pylons.get(1)));
        // the puzzles come one after another (place-presented puzzles at one place must not show at once)
        assertTrue(puzzle(fsk, LateQuestsB.FSK_POTION).needFlags.contains("fsk_furnace"));
        assertTrue(puzzle(fsk, LateQuestsB.FSK_PYLONS).needFlags.contains("fsk_potion"));
        assertTrue(fsk.stages.get(2).triggers.stream().anyMatch(t -> t.target.equals("undor") && t.take.containsKey("dinhs_hammer")));
        // Lady Vulcana only ends the quest once Undor has the hammer (or for a save from before Undor existed)
        for (Trigger t : fsk.stages.get(2).triggers) {
            if (t.target.equals("lord_kandur_lovakengj")) {
                assertTrue(t.needFlags.contains("fsk_doors") || t.unlessFlags.contains("fsk_undor"), "Undor skipped");
            }
        }
        assertTrue(fsk.stages.get(1).triggers.get(0).setFlags.contains("fsk_undor"), "every new save meets Undor first");
    }

    private static Trigger find(QuestSpec.Stage s, java.util.function.Predicate<Trigger> p, String what) {
        return s.triggers.stream().filter(p).findFirst().orElseThrow(() -> new AssertionError("stage " + s.index + " lacks " + what));
    }

    @Test
    void whileGuthixSleepsAssassinsBroavSnapdragonAndTheDarkSquall() {
        QuestSpec wgs = spec("while_guthix_sleeps");
        assertEquals(6, wgs.stages.size(), "saved stage count");
        assertEquals("ivy_sophista", wgs.stages.get(0).triggers.get(0).target);
        com.runecraft.osrs.content.PlaceDef ivy = com.runecraft.osrs.content.ContentRegistry.place("at_ivy_sophista");
        assertEquals(2907, ivy.osrsX(), "Ivy in Taverley (quest-helper tile)");
        com.runecraft.osrs.content.PlaceDef base = com.runecraft.osrs.content.ContentRegistry.place("movarios_base");
        assertEquals(List.of(2519, 3249), List.of(base.osrsX(), base.osrsY()), "the broken table on the Khazard battlefield");

        // Thaerisk's two assassins: the first kill raises the second, the second ends the fight
        QuestSpec.Stage track = wgs.stages.get(1);
        Trigger first = find(track, t -> t.kind == Trigger.Kind.KILL && "wgs_assassin".equals(t.spawn), "the first assassin");
        Trigger second = find(track, t -> t.kind == Trigger.Kind.KILL && t.setFlags.contains("wgs_assassins"), "the second assassin");
        assertTrue(first.setFlags.contains("wgs_assassin1") && second.needFlags.contains("wgs_assassin1"));
        assertEquals("wgs_assassin", second.target);
        assertEquals(167, com.runecraft.osrs.content.ContentRegistry.boss("wgs_assassin").combatLevel);
        // the launderer's 500 coins, the broav from the pit trap and its nose at the broken table
        assertEquals(500, find(track, t -> t.target.equals("khazard_launderer"), "the launderer").take.get("coins"));
        assertTrue(find(track, t -> t.target.equals("wgs_pit_trap"), "the pit trap").give.containsKey("broav"));
        Trigger table = find(track, t -> t.kind == Trigger.Kind.REACH && t.setFlags.contains("wgs_trapdoor"), "the broken table");
        assertTrue(table.need.containsKey("broav") && table.need.containsKey("dirty_shirt"));
        assertTrue(puzzle(wgs, GrandmasterQuests.WGS_DOOR).needFlags.contains("wgs_trapdoor"), "the rune door lies under the table");
        assertEquals("thaerisk", wgs.stages.get(2).triggers.get(0).target, "the notes go back to Thaerisk");

        // the snapdragon: Thaerisk's seed, Betty's counter, the castle's special patch
        QuestSpec.Stage spies = wgs.stages.get(3);
        assertTrue(find(spies, t -> t.give.containsKey("snapdragon_seed"), "the seed").setFlags.contains("wgs_seed"), "one free seed");
        Trigger betty = find(spies, t -> t.target.equals("wgs_bettys_counter"), "Betty's counter");
        assertTrue(betty.take.containsKey("snapdragon_seed") && betty.give.containsKey("enriched_snapdragon_seed"));
        Trigger patch = find(spies, t -> t.target.equals("wgs_snapdragon_patch"), "the patch");
        assertTrue(patch.take.containsKey("enriched_snapdragon_seed") && patch.need.containsKey("seed_dibber"));
        // the Dark Squall's base: the disguise, an elite black knight, Silif, then Surok (magic only, wiki stats)
        Trigger silif = find(spies, t -> t.target.equals("silif") && "elite_black_knight".equals(t.spawn), "Silif");
        assertTrue(silif.need.containsKey("bronze_med_helm") && silif.need.containsKey("iron_chainbody"));
        assertTrue(spies.triggers.stream().anyMatch(t -> t.target.equals("silif") && "dark_squall".equals(t.spawn)));
        com.runecraft.osrs.content.BossDef surok = com.runecraft.osrs.content.ContentRegistry.boss("dark_squall");
        assertEquals(265, surok.combatLevel);
        assertEquals(450, surok.hitpoints);
        Trigger onward = QuestWalk.advancing(spies);
        assertEquals("akrisae", onward.target);
        assertTrue(onward.needFlags.contains("wgs_surok"));
        assertTrue(spies.triggers.stream().anyMatch(t -> t.target.equals("ivy_sophista") && t.unlessFlags.contains("wgs_assassins")),
                "saves from before round 33 keep the old briefing");

        com.runecraft.osrs.content.BossDef balance = com.runecraft.osrs.content.ContentRegistry.boss("balance_elemental");
        assertEquals(524, balance.combatLevel);
        assertEquals(750, balance.hitpoints);
        assertEquals("idria", QuestWalk.advancing(wgs.stages.get(5)).target);
    }

    @Test
    void beneathCursedSandsStartsWithJamilaAndSolvesTheTomb() {
        QuestSpec bcs = spec("beneath_cursed_sands");
        assertEquals(4, bcs.stages.size(), "saved stage count");
        assertEquals("jamila", bcs.stages.get(0).triggers.get(0).target, "OSRS start: Jamila in Sophanem");
        StringBuilder code = new StringBuilder();
        answer(LateQuestsB.BCS_CHEST.rooms.get(0).variants().get(0)).forEach(code::append);
        assertEquals("1118513", code.toString(), "AKHEM as alphabet positions");
        assertEquals(24, LateQuestsB.BCS_URNS.rooms.get(0).variants().size(), "every order of the four emblems");
        assertTrue(bcs.def.itemRewards().containsAll(List.of("keris_partisan", "circlet_of_water")));
        assertEquals(379, com.runecraft.osrs.content.ContentRegistry.boss("champion_of_scabaras").combatLevel);
        assertTrue(bcs.triggers().stream().anyMatch(t -> t.kind == Trigger.Kind.KILL && t.target.equals("head_menaphite_guard")));
        // the guard is raised by talking to an entranced digger in the necropolis, where Maisa's cutscene leaves the player
        Trigger guard = bcs.stages.get(1).triggers.stream().filter(t -> "head_menaphite_guard".equals(t.spawn)).findFirst().orElseThrow();
        assertEquals("bcs_menaphite_citizen", guard.target);
        assertEquals("necropolis", bcs.stages.get(1).triggers.get(0).travel);
        assertEquals("maisa_bcs", bcs.stages.get(1).triggers.get(0).target, "Maisa's campsite east of Sophanem");
        // every urn order is answered by the riddle: the gods name the two northern urns, the offerings the two southern ones
        List<String> emblems = List.of("Baboon emblem", "Human emblem", "Crocodile emblem", "Scarab emblem");
        String[] gods = {"companionship", "health", "resourcefulness", "isolation"};
        String[] gifts = {"linen", "wine", "necklace", "carving"};
        for (QuestPuzzle.Variant v : LateQuestsB.BCS_URNS.rooms.get(0).variants()) {
            String riddle = String.join(" ", v.prompt());
            String clue = riddle.substring(riddle.indexOf("The riddle:"));
            List<String> order = answer(v);
            assertTrue(clue.indexOf(gods[emblems.indexOf(order.get(0))]) < clue.indexOf(gods[emblems.indexOf(order.get(1))]), riddle);
            assertTrue(clue.matches(".*next, he offered (some|a) " + gifts[emblems.indexOf(order.get(2))] + "\\..*"), riddle);
            assertTrue(clue.contains(gifts[emblems.indexOf(order.get(3))] + " came last"), riddle);
        }
        // the cure, the Menaphite Akh (wiki stats) and the High Priest in Sophanem at the end
        QuestSpec.Stage last = bcs.stages.get(3);
        assertTrue(last.triggers.stream().anyMatch(t -> t.target.equals("bcs_elid_lily") && t.give.containsKey("lily_of_the_elid")));
        assertTrue(last.triggers.stream().anyMatch(t -> t.target.equals("high_priest_sophanem") && t.take.containsKey("cure_crate")));
        assertTrue(last.triggers.stream().anyMatch(t -> "menaphite_akh".equals(t.spawn)));
        assertEquals(351, com.runecraft.osrs.content.ContentRegistry.boss("menaphite_akh").combatLevel);
        assertEquals("high_priest_sophanem", QuestWalk.advancing(last).target);
        assertTrue(QuestWalk.advancing(last).needFlags.contains("bcs_akh"));
        // saves from before round 33 (no Maisa flag) keep the old ending
        assertTrue(last.triggers.stream().anyMatch(t -> t.target.equals("tumeken_priest") && t.unlessFlags.contains("bcs_maisa") && t.completes()));
    }
}
