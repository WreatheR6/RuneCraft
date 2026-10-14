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
                .talk("hazelmere").take("bark_sample").give("hazelmeres_scroll").say("Ka. Lu. Min... *he writes you a scroll in the old tongue* Take this to your king.")
                .stage("I should take Hazelmere's scroll to King Narnode.")
                .talk("king_narnode").take("hazelmeres_scroll").say("'The tree is being poisoned by humans'? Glough, my advisor, will investigate.")
                .stage("Glough has thrown me in jail! Charlie, the other prisoner, may know something.")
                .talk("charlie_prisoner").say("Glough's no friend of the king. He's building warships at the shipyard in Karamja. Look in his house!")
                .stage("I should search Glough's house for evidence and show it to the king.")
                .talk("glough").give("invasion_plans").say("You'll never stop me, human! *As he storms out, you grab papers from his cupboard.*")
                .stage("I should show the invasion plans to King Narnode.")
                .talk("king_narnode").take("invasion_plans").say("Invasion plans! Glough is a traitor! He fled into the mine beneath the tree... and summoned a demon!")
                .stage("I should go into the mine beneath the Grand Tree and defeat Glough's demon.")
                .kill("glough_black_demon").give("daconia_rock").say("* The black demon falls. In the mine you find a glowing daconia rock.")
                .stage("I should give the daconia rock to King Narnode to heal the tree.")
                .talk("king_narnode").take("daconia_rock").say("The daconia rock! The roots will heal. The Grand Tree is saved, my friend.")
                .done("femi", "Thanks for helping the king!");
    }

    private static void monkeyMadness(QuestBook b) {
        b.npc("glo_caranock", "G.L.O. Caranock", "karamja_shipyard", 0, 0, 0xC89868, 0x3A5A8A, 0x2A3A5A, 0x8A6A3A);
        b.npc("daero", "Daero", "grand_tree", -4, 3, 0xD8A87A, 0x2A2A2A, 0x1A1A1A, 0x3A3A3A);
        b.npc("garkor", "Garkor", "ape_atoll", 4, 4, 0xC09868, 0x3A5A2A, 0x2A3A1A, 0x6A6A3A);
        b.npc("zooknock", "Zooknock", "ape_atoll", -4, 4, 0xC8A070, 0x5A2A6A, 0x3A1A4A, 0x9A9A9A);
        b.npc("tutab", "Tutab", "ape_atoll", 4, -4, 0x7A5A3A, 0x6A3A1A, 0x4A2A1A, 0x5A3A1A);
        b.npc("king_awowogei", "King Awowogei", "ape_atoll", 0, -6, 0xA08060, 0xE0C030, 0xA02020, 0xE0D040);
        b.item("narnodes_orders", "Narnode's orders", "note", 0xE8D8B0, "Royal orders from King Narnode.");
        b.item("monkey_dentures", "Monkey dentures", "bones", 0xE8E0C8, "A set of ape dentures, perfect for an amulet of monkey speak.");
        b.item("monkeyspeak_amulet", "Amulet of monkey speak", "amulet", 0xC0A040, "Lets you understand the monkeys of Ape Atoll.");
        b.item("monkey_talisman", "Monkey talisman", "talisman", 0x8A6A3A, "A talisman of a monkey.");
        b.item("monkey_greegree", "Karamjan monkey greegree", "talisman", 0xA07A3A, "Turns its wielder into a monkey.");
        b.boss(BossDef.of("jungle_demon", "Jungle demon").level(195).stats(170, 170, 170, 150, 150, 1).speed(4)
                .defenceBonus(25, 25, 25, 50, 25)
                .attack(AttackDef.of("claws", AttackType.SLASH, 32, 3, 6))
                .attack(AttackDef.of("fire_wave", AttackType.MAGIC, 20, 10, 2).particle("minecraft:flame"))
                .attack(AttackDef.of("water_wave", AttackType.MAGIC, 20, 10, 2).particle("minecraft:splash"))
                .phase(30, "The jungle demon howls with rage!", 1.15, false)
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("ape_atoll_temple").arena(12, "jungle"));
        b.quest("monkey_madness_i", "Monkey Madness I", Difficulty.MASTER, 3)
                .reqQuest("tree_gnome_village", "the_grand_tree")
                .xp(Skill.ATTACK, 17500).xp(Skill.STRENGTH, 17500)
                .reward("coins:10000", "diamond:3")
                .unlock("Ability to wield the dragon scimitar")
                .stage("I can start this quest by speaking to King Narnode Shareen under the Grand Tree. (Requires Tree Gnome Village and The Grand Tree.)")
                .talk("king_narnode").give("narnodes_orders").say("The 10th squad was sent to Ape Atoll and hasn't reported back. G.L.O. Caranock oversees the shipyard in Karamja; take him my orders.")
                .stage("I should take the orders to G.L.O. Caranock at the Karamja shipyard.")
                .talk("glo_caranock").take("narnodes_orders").say("Orders from the king? Fine. Daero in the Grand Tree will arrange your flight. ...Mind the monkeys.")
                .stage("I should speak to Daero in the Grand Tree about getting to Ape Atoll.")
                .talk("daero").travel("ape_atoll").say("Ape Atoll? Waydar will fly you. Hold on... *The glider crash-lands on the atoll!*")
                .stage("I've crash-landed on Ape Atoll. I should find Garkor and the 10th squad.")
                .talk("garkor").say("The 10th squad was turned into monkeys by Zooknock's magic, as a disguise. The monkey king Awowogei is allied with Glough!",
                        "Zooknock can make you an amulet to speak with monkeys and a greegree to look like one.")
                .stage("Zooknock needs a gold bar and monkey dentures to make an amulet of monkey speak, and a monkey talisman for a greegree.")
                .talk("tutab").stay().unless("monkey_dentures", "monkeyspeak_amulet").take("coins:1000").give("monkey_dentures")
                .say("Ook! Monkey dentures, 1,000 coins. Very fine teeth.")
                .talk("tutab").stay().unless("monkey_talisman", "monkey_greegree").take("coins:1000").give("monkey_talisman")
                .say("Ook ook! Monkey talisman, 1,000 coins.")
                .talk("zooknock").stay().unless("monkeyspeak_amulet").take("gold_bar", "monkey_dentures").give("monkeyspeak_amulet")
                .say("* Zooknock enchants the dentures and gold into an amulet of monkey speak.")
                .talk("zooknock").need("monkeyspeak_amulet").take("monkey_talisman").give("monkey_greegree")
                .say("* Zooknock weaves a greegree from the talisman. Hold it and you'll look just like a monkey!")
                .missing("I need a gold bar, monkey dentures and a monkey talisman. Tutab's shop sells the last two.")
                .stage("Disguised as a monkey, I should speak to King Awowogei.")
                .talk("king_awowogei").need("monkey_greegree", "monkeyspeak_amulet")
                .say("Ook! A new monkey? You will serve me. Glough promised us the Grand Tree... his jungle demon will guard our temple.")
                .missing("* The monkey guards won't let a human near the king.")
                .stage("I should tell Garkor what Awowogei said.")
                .talk("garkor").say("A jungle demon! Glough means to unleash it. We must strike first: it's summoned in the temple of Marim. We'll fight with you.")
                .stage("I must defeat the jungle demon beneath the temple of Marim.")
                .kill("jungle_demon").travel("grand_tree").say("* The jungle demon is destroyed! Waydar flies you and the 10th squad home.")
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
