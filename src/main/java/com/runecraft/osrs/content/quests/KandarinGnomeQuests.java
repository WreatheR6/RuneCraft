package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** The gnome quests: Tree Gnome Village, The Grand Tree and Monkey Madness I. */
final class KandarinGnomeQuests {
    /**
     * Zooknock stands at the end of the Ape Atoll Dungeon on his cache tile (2804, 9145), below surface tile (2804, 2745):
     * the dungeons module's {@code zooknock_cave} place, whose centre snaps to its chunk's centre.
     */
    private static int zooknockDx() {
        int x = com.runecraft.osrs.world.StructureDef.bx(2804);
        return x - (((x >> 4) << 4) + 8);
    }

    private static int zooknockDz() {
        int z = com.runecraft.osrs.world.StructureDef.bz(2745);
        return z - (((z >> 4) << 4) + 8);
    }

    private KandarinGnomeQuests() {
    }

    static void register(QuestBook b) {
        b.place("khazard_battlefield", "Khazard battlefield", 2510, 3200, PlaceDef.Style.RUINS);
        b.spot("hazelmeres_island", "Hazelmere's island", 2677, 3088);
        b.spot("grand_tree", "The Grand Tree", 2466, 3496);
        b.place("grand_tree_mine", "Grand Tree mine", 2448, 3486, PlaceDef.Style.CAVE);
        b.spot("karamja_shipyard", "Karamja shipyard", 2955, 3025);
        b.place("ape_atoll", "Ape Atoll (Marim)", 2760, 2780, PlaceDef.Style.TOWN);
        b.place("ape_atoll_temple", "Marim temple", 2790, 2795, PlaceDef.Style.RUINS);
        treeGnomeVillage(b);
        grandTree(b);
        monkeyMadness(b);
    }

    private static void treeGnomeVillage(QuestBook b) {
        b.npc("king_bolren", "King Bolren", "tree_gnome_village", 0, 3, 0xD0A070, 0xC0A030, 0x6A4A1A, 0xE0C030);
        b.npc("commander_montai", "Commander Montai", "khazard_battlefield", 3, 3, 0xD0A071, 0x8A3A1A, 0x4A2A1A, 0x6A6A6A);
        // Tracker gnomes and the ballista at their OSRS tiles (Zoinkwiz/quest-helper).
        b.spot("tgv_tracker_1", "Tracker gnome 1", 2501, 3261);
        b.spot("tgv_tracker_2", "Tracker gnome 2", 2524, 3257);
        b.spot("tgv_tracker_3", "Tracker gnome 3", 2497, 3234);
        b.spot("tgv_ballista", "Gnome ballista", 2509, 3211);
        b.spot("khazard_stronghold", "Khazard stronghold", 2506, 3259);
        b.npc("tracker_gnome", "Tracker gnome 1", "tgv_tracker_1", 0, 0, 0xC89A68, 0x3A6A2A, 0x2A4A1A, 0x3A5A2A);
        b.npc("tracker_gnome_2", "Tracker gnome 2", "tgv_tracker_2", 0, 0, 0xC0946A, 0x2A5A6A, 0x2A3A4A, 0x5A3A2A);
        b.npc("tracker_gnome_3", "Tracker gnome 3", "tgv_tracker_3", 0, 0, 0xC89A68, 0x6A3A2A, 0x2A4A1A, 0x3A5A2A);
        b.item("orb_of_protection", "Orb of protection", "gem", 0x60C0E0, "One of the gnome village's orbs of protection.");
        b.npc("elkoy", "Elkoy", "tree_gnome_village", 3, -3, 0xD8A878, 0x6A8A3A, 0x3A4A1A, 0x8A6A2A);
        b.item("orbs_of_protection", "Orbs of protection", "gem", 0x60C0E0, "The gnome village's orbs of protection.");
        b.boss(BossDef.of("khazard_warlord", "Khazard warlord").level(112).stats(170, 80, 75, 80, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, 0, 60)
                .attack(AttackDef.of("slash", AttackType.SLASH, 16, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xD0A880, 0x3A3A3A, 0x1A1A1A, 0x5A1A1A).at("khazard_battlefield").arena(9, "stone"));
        b.quest("tree_gnome_village", "Tree Gnome Village", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.ATTACK, 11450).reward("gnome_amulet")
                .stage("I can start this quest by speaking to King Bolren in the Tree Gnome Village (follow Elkoy through the maze).")
                .talk("king_bolren").say("General Khazard's troops stole our orbs of protection! Without them the village will fall. Commander Montai leads our defence.")
                .stage("I should help Commander Montai at the battlefield. He needs 6 logs to reinforce the defences.")
                .talk("commander_montai").take("logs:6").say("Logs! Now the defences will hold. My trackers have scouted the Khazard stronghold; find all three and get the coordinates.")
                .missing("We need six logs to build defences.")
                .stage("I should gather the coordinates from the tracker gnomes and fire the ballista at the Khazard stronghold.")
                .talk("tracker_gnome").stay().unlessFlag("tgv_t1").flag("tgv_t1").say("I've scouted the stronghold. Its height coordinate is four.")
                .talk("tracker_gnome_2").stay().unlessFlag("tgv_t2").flag("tgv_t2").say("They caught me spying! The y coordinate is one. Tell Montai!")
                .talk("tracker_gnome_3").stay().unlessFlag("tgv_t3").flag("tgv_t3").say("*hic* The x coordinate? Somewhere between one and five... *hic*")
                .reach("tgv_ballista", 4).needFlag("tgv_t1", "tgv_t2", "tgv_t3")
                .say("* You load the ballista with the trackers' coordinates and fire. The stronghold wall collapses!")
                .stage("The wall is breached. An orb is kept in the Khazard stronghold; the Khazard warlord carries the rest.")
                .reach("khazard_stronghold", 4).stay().unless("orb_of_protection").unlessFlag("tgv_orb1").give("orb_of_protection")
                .say("* You climb the ladder in the stronghold and take an orb of protection from the chest.")
                .talk("king_bolren").stay().unlessFlag("tgv_orb1").take("orb_of_protection").flag("tgv_orb1")
                .say("An orb! But the Khazard warlord still holds the others, and his men are attacking the village. Please, get them back!")
                .kill("khazard_warlord").needFlag("tgv_orb1").give("orbs_of_protection").say("* The warlord falls, dropping the orbs of protection.")
                .stage("I should return the orbs to King Bolren.")
                .talk("king_bolren").take("orbs_of_protection").say("The orbs! Our village is safe. Please accept this gnome amulet of protection.")
                .done("elkoy", "Follow me through the maze any time.")
                .done("king_bolren", "Thank you, friend of the gnomes.");
    }

    private static void grandTree(QuestBook b) {
        b.npc("king_narnode", "King Narnode Shareen", "grand_tree", 0, 2, 0xD8A070, 0xE0C030, 0x6A3A8A, 0xE0D040);
        b.npc("hazelmere", "Hazelmere", "hazelmeres_island", 0, 0, 0xD0B080, 0x8A2A8A, 0x5A1A5A, 0xE0E0E0);
        b.npc("glough", "Glough", "grand_tree", 3, -2, 0xC09060, 0x3A3A3A, 0x2A2A2A, 0x5A1A1A);
        b.npc("charlie_prisoner", "Charlie", "grand_tree", -3, -2, 0xE0B898, 0x6A6A5A, 0x4A4A3A, 0x2A1A0A);
        b.npc("femi", "Femi", "grand_tree", 4, 3, 0xC89A6A, 0xC06A2A, 0x5A3A1A, 0xA05A1A);
        b.item("bark_sample", "Bark sample", "log", 0x6A4A2A, "A sample of the Grand Tree's bark.");
        b.item("hazelmeres_scroll", "Hazelmere's scroll", "note", 0xE0D8A0, "A scroll in an ancient gnome language.");
        b.item("invasion_plans", "Invasion plans", "note", 0xD0C0A0, "Glough's plans to invade with a fleet of gnome warships.");
        b.item("daconia_rock", "Daconia rock", "gem", 0x6A6AE0, "A rare magical rock. The roots of the Grand Tree feed on it.");
        // Glough's treehouse south of the Grand Tree and Anita's house in the north-west of the stronghold (wiki map tiles).
        b.spot("gt_glough_house", "Glough's house", 2477, 3463);
        b.spot("gt_anitas_house", "Anita's house", 2391, 3511);
        b.npc("anita", "Anita", "gt_anitas_house", 0, 0, 0xD8A878, 0xC05A8A, 0x6A3A5A, 0x6A3A1A);
        b.item("lumber_order", "Lumber order", "note", 0xE0D0A0, "An order for thousands of logs, delivered to Glough's shipyard.");
        b.item("glough_key", "Glough's key", "key", 0xA08A4A, "The key to Glough's chest.");
        b.item("gt_twigs", "Twigs", "log", 0x8A6A3A, "Four bundles of twigs, shaped like gnome letters: T, U, Z and O.");
        b.boss(BossDef.of("glough_black_demon", "Black demon").level(172).stats(157, 145, 148, 152, 1, 1).speed(4)
                .defenceBonus(0, 0, 0, -10, 0)
                .attack(AttackDef.of("claw", AttackType.SLASH, 16, 3, 10))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("grand_tree_mine").arena(10, "crypt"));
        b.quest("the_grand_tree", "The Grand Tree", Difficulty.INTERMEDIATE, 5)
                .xp(Skill.ATTACK, 18400).xp(Skill.AGILITY, 7900).xp(Skill.MAGIC, 2150) // OSRS (current quest XP rewards)
                .req(Skill.AGILITY, 25)
                .stage("I can start this quest by speaking to King Narnode Shareen under the Grand Tree. (Requires 25 Agility.)")
                .talk("king_narnode").give("bark_sample").say("The Grand Tree is dying! Take this bark sample to the wise gnome Hazelmere, on the island east of Yanille.")
                .stage("I should take the bark sample to Hazelmere.")
                .talk("king_narnode").stay().unless("bark_sample").give("bark_sample").say("Lost the sample? Here's another. Hurry, to Hazelmere!")
                .talk("hazelmere").take("bark_sample").give("hazelmeres_scroll")
                .say("*He studies the bark, then writes you a message in the old gnome tongue.* Take this to your king.")
                // The king asks for the translation (wiki): a man with the King's seal, daconia rocks, and they will kill the tree.
                .stage("I should take Hazelmere's scroll to King Narnode and translate it for him.")
                .talk("hazelmere").stay().unless("hazelmeres_scroll").give("hazelmeres_scroll").say("*He writes the message out again.*")
                .puzzle("king_narnode", GT_TRANSLATION).take("hazelmeres_scroll")
                .say("Someone forged my seal to take daconia rocks from Hazelmere! They were made as a fail-safe: the only thing that can kill the tree.",
                        "Speak to Glough, my head tree guardian. He lives in the treehouse south of the Grand Tree.")
                .stage("I should speak to Glough, then to the king, and question the human Glough has imprisoned at the top of the Grand Tree.")
                .talk("glough").stay().unlessFlag("gt_glough").flag("gt_glough").say("Humans! I knew they couldn't be trusted. I'll take care of this.")
                .talk("king_narnode").stay().needFlag("gt_glough").unlessFlag("gt_king").flag("gt_king")
                .say("Glough has caught a human with the daconia rocks! He's in the cells at the top of the tree.")
                .talk("charlie_prisoner").needFlag("gt_king")
                .say("Glough paid me to fetch the rocks from Hazelmere, I swear! I don't know what he's up to. Search his house for evidence.")
                .missing("Who are you? Leave me alone.")
                .hint("king_narnode", "Speak to Glough, my head tree guardian. He'll get to the bottom of this.")
                // Wiki: Glough's journal, the cells, Charlie's password Ka-Lu-Min, the glider to the Karamja shipyard, the foreman's
                // questions and lumber order, then Glough's key from Anita and the invasion plans in his chest.
                .stage("I should search Glough's house for evidence that will convince the king.")
                .talk("glough").stay().unlessFlag("gt_jailed").flag("gt_jailed")
                .say("* In Glough's cupboard you find his journal: he means to convince the gnomes that all humans are evil.",
                        "You read my journal? Guards! Lock this human up with the other one!")
                .talk("charlie_prisoner").stay().needFlag("gt_jailed").unlessFlag("gt_password").flag("gt_password").travel("karamja_shipyard")
                .say("You too? I delivered a lot of gold to the shipyard on Karamja for Glough. Find out what he's building there. "
                                + "The password at the gate is Ka-Lu-Min.",
                        "@King Narnode Shareen: I'm sorry! Glough's guards watch every way out. Quickly, take the glider at the top of the tree.",
                        "* The gnome glider crash lands just west of the shipyard.")
                .talk("charlie_prisoner").stay().needFlag("gt_password").unlessFlag("gt_lumber").travel("karamja_shipyard")
                .say("The shipyard on Karamja: the password is Ka-Lu-Min. Take the glider!")
                .puzzle("karamja_shipyard", GT_GATE).stay().needFlag("gt_password").unlessFlag("gt_gate", "gt_lumber").flag("gt_gate")
                .say("* The shipyard worker lets you through the gate.")
                .puzzle("karamja_shipyard", GT_FOREMAN).stay().needFlag("gt_gate").unlessFlag("gt_lumber").flag("gt_lumber").give("lumber_order")
                .say("Glough's man, all right. Here's the lumber order for his fleet; get it back to him. *Thousands of logs, for warships!*")
                .talk("king_narnode").stay().needFlag("gt_lumber").unlessFlag("gt_shown").need("lumber_order").flag("gt_shown")
                .say("A lumber order? Anyone could have written that. You're as paranoid as Glough! Leave, for your own safety.")
                .talk("charlie_prisoner").stay().needFlag("gt_shown").unlessFlag("gt_charlie_key").flag("gt_charlie_key")
                .say("I heard Glough mention a chest key he left at his girlfriend Anita's house, in the north-west of the stronghold.")
                .talk("anita").needFlag("gt_charlie_key").stay().unless("glough_key").give("glough_key")
                .say("You know my Glough? He left his key here again. Would you take it back to him?")
                .reach("gt_glough_house", 5).take("glough_key").give("invasion_plans")
                .say("* Glough's key opens the chest in his house. Inside: his plans to invade Gielinor with a fleet of gnome warships!")
                .missing("* Glough's chest is locked.")
                .stage("I should show the invasion plans to King Narnode.")
                .talk("king_narnode").take("invasion_plans").give("gt_twigs")
                .say("These could be anyone's plans. But here: four bundles of twigs, all my guards found in Glough's house. Make of them what you will.")
                // Wiki: on the watchtower above Glough's house (25 Agility) the twigs spell TUZO, "open", on four pillars, opening a
                // trapdoor to the tunnels where Glough sets a black demon on the player.
                .stage("I should find out what Glough's twigs open, then face whatever waits below.")
                .talk("king_narnode").stay().unless("gt_twigs").unlessFlag("gt_tuzo").give("gt_twigs")
                .say("Lost the twigs? My guards found a few more bundles in Glough's house.")
                .puzzle("gt_glough_house", GT_TUZO).stay().reqSkill(Skill.AGILITY, 25).take("gt_twigs").unlessFlag("gt_tuzo").flag("gt_tuzo")
                .say("* Gears turn, and a trapdoor opens in the watchtower floor. Below, Glough waits: 'You'll never stop me, human!'")
                .kill("glough_black_demon").needFlag("gt_tuzo").say("* The black demon falls. Along the passage King Narnode is waiting by a ladder.",
                        "@King Narnode Shareen: A demon? Guards, find Glough! ... He was hiding among the stones. Forgive me, human. Find the last daconia rock!")
                .stage("I should search the roots in the mine beneath the Grand Tree for the last daconia rock and give it to King Narnode.")
                .reach("grand_tree_mine", 10).stay().unless("daconia_rock").give("daconia_rock")
                .say("* You search the roots of the Grand Tree one after another... and find the last daconia rock.")
                .talk("king_narnode").take("daconia_rock").say("The daconia rock! The roots will heal. The Grand Tree is saved, my friend.")
                .missing("The last daconia rock is somewhere among the roots below. Search them all!")
                .done("femi", "Thanks for helping the king!");
    }

    /** The king's test of Hazelmere's message (wiki translation), the lines in order; the listing is shuffled per player. */
    static final QuestPuzzle GT_TRANSLATION = QuestPuzzle.builder("gt_translation").shuffle().variant(null)
            .prompt("What does Hazelmere's message say? *With the translation book you make out the gnome words: man, came, king's seal, gave, "
                    + "daconia rocks, will, kill, tree.* Give me the three lines in order.")
            .options("A man came to me with the King's seal.", "A man came to me with a sack of gold.", "I gave the man Daconia rocks.",
                    "I gave the man a bark sample.", "And Daconia rocks will kill the tree!", "And Daconia rocks will save the tree!")
            .solution(0, 2, 4).failLines("That makes no sense! Look at the message again.").build();

    /** The shipyard gate (wiki): Charlie's password, Ka-Lu-Min. */
    static final QuestPuzzle GT_GATE = QuestPuzzle.builder("gt_gate").shuffle().variant(null)
            .prompt("* A shipyard worker blocks the gate: 'Who sent you? Glough? Then you know the password.'")
            .options("Ka", "Lu", "Min", "Tuzo", "Zo", "Gnome")
            .solution(0, 1, 2).failLines("* 'That's not the password! Clear off!'").build();

    /** The foreman's questions about Glough (wiki answers: his wife has passed away, worm holes, Anita). */
    static final QuestPuzzle GT_FOREMAN = QuestPuzzle.builder("gt_foreman").variant(null)
            .prompt("* The foreman on the southern docks: 'Glough sent you, did he? Then answer me this. How's his wife? What's his favourite dish? "
                    + "And what's his new girlfriend's name?'")
            .options("His wife is well, thanks.", "Sadly his wife is no longer with us.", "He loves worm holes.", "He loves chocolate bombs.",
                    "Anita.", "Alice.")
            .solution(1, 2, 4).failLines("* 'You're no friend of Glough's! Get out of my shipyard!'").build();

    /** The watchtower (wiki): the four twig bundles spell TUZO ("open") on the pillars, from the far left. */
    static final QuestPuzzle GT_TUZO = QuestPuzzle.builder("gt_tuzo").shuffle().variant(null)
            .prompt("* Up Glough's watchtower stand a stone stand and four pillars. Your four bundles of twigs are shaped like the gnome letters "
                    + "T, U, Z and O; the translation book says the gnome word for 'open' is tuzo. Place them from the far left.")
            .options("Twigs shaped like T", "Twigs shaped like U", "Twigs shaped like Z", "Twigs shaped like O")
            .solution(0, 1, 2, 3).failLines("* Nothing happens. You take the twigs back off the pillars.").build();

    /**
     * The glider hangar's reinitialisation panel (quest-helper, wiki puzzle box guide): a sliding-tile puzzle, one of three
     * boards per player; a wrong slide puts the tiles back.
     */
    static final QuestPuzzle MM_PANEL = QuestPuzzle.builder("mm_panel").room("panel")
            .slide(null, "* The glider's reinitialisation panel is a sliding-tile puzzle.", 0, 1, 3, 5, 2, 7, 4, 8, 6)
            .slide(null, "* The glider's reinitialisation panel is a sliding-tile puzzle.", 0, 1, 3, 6, 2, 8, 4, 7, 5)
            .slide(null, "* The glider's reinitialisation panel is a sliding-tile puzzle.", 1, 3, 6, 7, 4, 2, 5, 8, 0)
            .failLines("* The tiles slide back to where they started.").build();

    private static void monkeyMadness(QuestBook b) {
        b.npc("glo_caranock", "G.L.O. Caranock", "karamja_shipyard", 0, 0, 0xC89868, 0x3A5A8A, 0x2A3A5A, 0x8A6A3A);
        b.npc("daero", "Daero", "grand_tree", -4, 3, 0xD8A87A, 0x2A2A2A, 0x1A1A1A, 0x3A3A3A);
        b.npc("garkor", "Garkor", "ape_atoll", 4, 4, 0xC09868, 0x3A5A2A, 0x2A3A1A, 0x6A6A3A);
        b.npc("zooknock", "Zooknock", "zooknock_cave", zooknockDx(), zooknockDz(), 0xC8A070, 0x5A2A6A, 0x3A1A4A, 0x9A9A9A);
        b.npc("tutab", "Tutab", "ape_atoll", 4, -4, 0x7A5A3A, 0x6A3A1A, 0x4A2A1A, 0x5A3A1A);
        b.npc("king_awowogei", "King Awowogei", "ape_atoll", 0, -6, 0xA08060, 0xE0C030, 0xA02020, 0xE0D040);
        // The flight crew, the jailers' island and the monkeys the quest needs, on their OSRS tiles (mejrs/data_osrs).
        b.npc("waydar", "Waydar", "gnome_glider_hangar", 3, 1, 0xD8A87B, 0x5A6A8A, 0x3A4A5A, 0x6A5A3A);
        b.spot("mm_crash_island", "Crash Island", 2891, 2724);
        b.npc("mm_lumdo", "Lumdo", "mm_crash_island", 0, 0, 0xD0A070, 0x6A4A2A, 0x4A3A1A, 0x5A4A2A);
        b.spot("ape_atoll_jail", "Ape Atoll jail", 2771, 2795);
        b.spot("ape_atoll_crates", "Marim storehouse", 2803, 2790);
        b.npcAt("monkey_child", "Monkey child", 2743, 2795, 0x8A6A4A, 0x6A4A2A, 0x4A3A1A, 0x8A6A4A);
        b.npcAt("elder_guard", "Elder Guard", 2798, 2760, 0x3A3A3A, 0x6A2A1A, 0x4A1A0A, 0xE0C030);
        b.npcAt("mm_kruk", "Kruk", 2726, 2765, 0x5A4A3A, 0x4A3A2A, 0x3A2A1A, 0x6A5A4A);
        b.npcAt("monkey_minder", "Monkey minder", 2602, 3274, 0xE0B898, 0x5A6A2A, 0x3A4A1A, 0x6A4A2A);
        b.item("narnodes_orders", "Narnode's orders", "note", 0xE8D8B0, "Royal orders from King Narnode.");
        b.item("monkey_dentures", "Monkey dentures", "bones", 0xE8E0C8, "A set of ape dentures, perfect for an amulet of monkey speak.");
        b.item("monkeyspeak_amulet", "Amulet of monkey speak", "amulet", 0xC0A040, "Lets you understand the monkeys of Ape Atoll.");
        b.item("monkey_talisman", "Monkey talisman", "talisman", 0x8A6A3A, "A talisman of a monkey.");
        b.item("monkey_greegree", "Karamjan monkey greegree", "talisman", 0xA07A3A, "Turns its wielder into a monkey.");
        b.item("gnome_royal_seal", "Gnome royal seal", "note", 0xC0302A, "King Narnode's royal seal.");
        b.item("mamulet_mould", "M'amulet mould", "mould", 0xA08A6A, "A mould for a monkey speak amulet.");
        b.item("enchanted_bar", "Enchanted bar", "bar", 0xE0C060, "A gold bar enchanted by Zooknock.");
        b.item("mm_karamjan_monkey", "Monkey", "creature", 0x8A6A3A, "A Karamjan monkey from the Ardougne Zoo. It's sitting in your bag.");
        b.item("tenth_squad_sigil", "10th squad sigil", "amulet", 0x6A8A3A, "Wear it to be teleported to the 10th squad.");
        b.boss(BossDef.of("jungle_demon", "Jungle demon").level(195).stats(170, 170, 170, 150, 150, 1).speed(4)
                .defenceBonus(25, 25, 25, 50, 25)
                .attack(AttackDef.of("claws", AttackType.SLASH, 32, 3, 6))
                .attack(AttackDef.of("fire_wave", AttackType.MAGIC, 20, 10, 2).particle("minecraft:flame"))
                .attack(AttackDef.of("water_wave", AttackType.MAGIC, 20, 10, 2).particle("minecraft:splash"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("ape_atoll_temple").arena(12, "jungle"));
        b.spot("gnome_glider_hangar", "Glider hangar beneath the Grand Tree", 2459, 3500); // approx: the hangar is underground (2390, 9886)
        b.quest("monkey_madness_i", "Monkey Madness I", Difficulty.MASTER, 3)
                .reqQuest("tree_gnome_village", "the_grand_tree")
                // Wiki: Daero gives 35,000 XP in each of the two skills the player focuses on (Attack and Defence, or Strength and
                // Hitpoints) and 20,000 in each of the other two. // approx: the Attack and Defence pair is picked here
                .xp(Skill.ATTACK, 35000).xp(Skill.DEFENCE, 35000).xp(Skill.STRENGTH, 20000).xp(Skill.HITPOINTS, 20000)
                .reward("coins:10000", "diamond:3")
                .unlock("Ability to wield the dragon scimitar")
                .stage("I can start this quest by speaking to King Narnode Shareen under the Grand Tree. (Requires Tree Gnome Village and The Grand Tree.)")
                .talk("king_narnode").give("gnome_royal_seal")
                .say("The 10th squad was sent to Ape Atoll and hasn't reported back. Take my royal seal to G.L.O. Caranock at the shipyard on Karamja; "
                        + "he'll tell you what he knows.")
                .stage("I should take the royal seal to G.L.O. Caranock at the Karamja shipyard.")
                .talk("glo_caranock").take("gnome_royal_seal").say("The royal seal? Fine. The 10th squad flew out of here weeks ago... "
                        + "Tell the king their glider never came back. ...Mind the monkeys.")
                // Saves from before the royal seal carry Narnode's orders instead.
                .talk("glo_caranock").take("narnodes_orders").say("Orders from the king? Fine. Daero in the Grand Tree will arrange your flight.")
                .stage("I should speak to Daero in the Grand Tree about getting to Ape Atoll.")
                // Quest-helper: back to Narnode for his orders, Daero takes the player to the hangar beneath the Grand Tree, the
                // reinitialisation panel's sliding puzzle wakes the glider, and Waydar flies to Crash Island, where Lumdo has the boat.
                .talk("king_narnode").stay().unlessFlag("mm_orders").flag("mm_orders").give("narnodes_orders")
                .say("Caranock knows nothing? Then the 10th squad is lost. Take these orders to Daero; he'll arrange a flight to Ape Atoll.")
                .talk("daero").stay().needFlag("mm_orders").unlessFlag("mm_daero").take("narnodes_orders").flag("mm_daero").travel("gnome_glider_hangar")
                .say("The 10th squad? Hush. Let's talk in the hangar beneath the Grand Tree. *He takes you there.*",
                        "The glider's controls need reinitialising first. Use the panel; it's a tile puzzle.")
                .missing("Orders from the king, please. I don't fly anyone without them.")
                .puzzle("gnome_glider_hangar", MM_PANEL).stay().needFlag("mm_daero").unlessFlag("mm_panel").flag("mm_panel")
                .say("* The last tile slides home and the glider's systems hum back to life.")
                .talk("waydar").stay().needFlag("mm_panel").unlessFlag("mm_crash_island").flag("mm_crash_island").travel("mm_crash_island")
                .say("Glider's ready? Hold on tight! *Waydar flies you to Crash Island, near Ape Atoll.*")
                .missing("The glider won't fly until its panel is reinitialised.")
                .talk("mm_lumdo").needFlag("mm_crash_island").travel("ape_atoll")
                .say("I'm not supposed to sail anyone to... oh, Waydar's orders. Fine! *Lumdo rows you to Ape Atoll.* "
                        + "*Guards spot you on the beach and throw you into a cell!*")
                .stage("I've crash-landed on Ape Atoll. I should find Garkor and the 10th squad.")
                // Wiki: the player is thrown in jail and picks the cell lock while the guards look away.
                .reach("ape_atoll_jail", 6).stay().unlessFlag("mm_escaped").take("lockpick").flag("mm_escaped")
                .say("* You watch the guards' rounds, pick the lock of your cell and slip out.")
                .missing("* You're stuck in a jail cell. A lockpick would open it.")
                .talk("garkor").needFlag("mm_escaped").say("The 10th squad was turned into monkeys by Zooknock's magic, as a disguise. The monkey king Awowogei is allied with Glough!",
                        "Zooknock can make you an amulet to speak with monkeys and a greegree to look like one. He's at the end of the dungeon to the west.")
                .stage("Zooknock needs a gold bar, monkey dentures and an M'amulet mould for an amulet of monkey speak, and a monkey talisman with Karamjan monkey bones for a greegree.")
                // Wiki: the dentures and the mould are in the U-shaped storehouse's crates (the mould in the cave below).
                .reach("ape_atoll_crates", 5).stay().unless("monkey_dentures", "enchanted_bar", "monkeyspeak_amulet").give("monkey_dentures")
                .say("* Keeping to the dark floor, you search the stacked crates: monkey dentures.")
                .reach("ape_atoll_crates", 5).stay().unless("mamulet_mould").give("mamulet_mould")
                .say("* A crate gives way under you and you drop into a cave. In the crates there: an M'amulet mould.")
                .talk("zooknock").stay().need("mamulet_mould").unless("enchanted_bar", "monkeyspeak_amulet").take("gold_bar", "monkey_dentures").give("enchanted_bar")
                .say("* Zooknock enchants the gold bar with the dentures' power and hands back your mould. Cast it in the temple's wall of flames.")
                .reach("ape_atoll_temple", 8).stay().need("mamulet_mould").unless("monkeyspeak_amulet").take("enchanted_bar", "ball_of_wool").give("monkeyspeak_amulet")
                .say("* Down the trapdoor in the temple, you cast the enchanted bar in the wall of flames using the mould and string it: an M'speak amulet.")
                .missing("* A wall of flames burns beneath the temple. With an enchanted bar, a mould and a ball of wool you could cast an amulet here.")
                // Wiki: the monkey child gives a talisman for five bananas while his aunt isn't looking.
                .talk("monkey_child").stay().need("monkeyspeak_amulet").unless("monkey_talisman", "monkey_greegree").take("banana:5").give("monkey_talisman")
                .say("Ook! You talk like a monkey! Well I'll be a monkey's uncle... Five bananas for Aunty? *He hands you a monkey talisman once she's gone.*")
                .missing("Ook ook! *You can't understand a word. Bring an M'speak amulet, and five bananas.*")
                .talk("tutab").stay().need("monkeyspeak_amulet").unless("monkey_talisman", "monkey_greegree").take("coins:1000").give("monkey_talisman")
                .say("Ook ook! Monkey talisman, 1,000 coins.")
                .talk("zooknock").need("monkeyspeak_amulet").take("monkey_talisman", "monkey_bones").give("monkey_greegree")
                .say("* Zooknock weaves a greegree from the talisman and the bones. Hold it and you'll look just like a monkey!")
                .missing("Bring me the dentures, a gold bar and an M'amulet mould for the amulet, then a monkey talisman and Karamjan monkey bones for a greegree.")
                .stage("Disguised as a monkey, I should speak to King Awowogei.")
                // Wiki: a Karamjan monkey from the Ardougne Zoo vouches for you; Garkor, the Elder Guard and Kruk get you to the king.
                .talk("monkey_minder").stay().need("monkey_greegree", "monkeyspeak_amulet").unless("mm_karamjan_monkey").unlessFlag("mm_awowogei")
                .give("mm_karamjan_monkey")
                .say("* Disguised as a monkey, you're put into the monkey enclosure. One of the Karamjan monkeys jumps into your bag. "
                        + "You change back and the minder lets you out.")
                .talk("elder_guard").stay().need("monkey_greegree", "monkeyspeak_amulet").unlessFlag("mm_elder")
                .flag("mm_elder").say("Ook. Garkor sent you? Then Kruk will take you to the king. He waits on the hill to the west.")
                .talk("mm_kruk").stay().needFlag("mm_elder").unlessFlag("mm_kruk").flag("mm_kruk")
                .say("Ook! A new recruit? Follow me to King Awowogei.")
                .talk("king_awowogei").needFlag("mm_kruk").need("monkey_greegree", "monkeyspeak_amulet").take("mm_karamjan_monkey").flag("mm_awowogei")
                .say("Ook! A monkey from the mainland? You will serve me. Glough promised us the Grand Tree... his jungle demon will guard our temple.")
                .missing("* The monkey guards won't let a human near the king, and the king wants proof you're one of his: a monkey from the mainland.")
                .stage("I should tell Garkor what Awowogei said.")
                .talk("garkor").give("tenth_squad_sigil")
                .say("A jungle demon! Glough means to unleash it. We must strike first: it's summoned in the temple of Marim. We'll fight with you.",
                        "Wear this 10th squad sigil when you're ready and you'll be brought to us.")
                .stage("I must defeat the jungle demon beneath the temple of Marim.")
                // a lost sigil (or a save from before it) is replaced by Garkor
                .talk("garkor").stay().unless("tenth_squad_sigil").give("tenth_squad_sigil")
                .say("Lost your sigil? Here's another. Wear it when you're ready to fight.")
                .kill("jungle_demon").need("tenth_squad_sigil").travel("grand_tree").say("* The jungle demon is destroyed! Waydar flies you and the 10th squad home.")
                .missing("* Garkor's 10th squad sigil takes you to the fight.")
                .stage("I should report to King Narnode.")
                .talk("king_narnode").say("The 10th squad is safe and Glough's plans are ruined again! Take this reward. And the dragon scimitar is now yours to wield.")
                .done("garkor", "The 10th squad salutes you.")
                .done("king_awowogei", "Ook. Human. Ook.")
                .done("daga", "Ook! Finest scimitars in Gielinor.");
        // Daga's Scimitar Smithy on Ape Atoll sells the dragon scimitar once Monkey Madness I is complete.
        b.npc("daga", "Daga", "ape_atoll", 6, 0, 0x8A6A4A, 0x5A3A1A, 0x3A2A1A, 0xA02020);
        b.shop("daga", "dragon_scimitar", 100000, "monkey_madness_i", null, null, 0,
                "Ook! You fought for the 10th squad. Daga sells you a dragon scimitar, 100,000 coins.");
    }
}
