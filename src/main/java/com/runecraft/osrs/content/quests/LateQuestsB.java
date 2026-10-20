package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Map;

/**
 * Desert, Morytania, Piscatoris and Great Kourend quests: Spirits of the Elid, Contact!, Beneath
 * Cursed Sands, Swan Song, Ratcatchers, Shades of Mort'ton, Rag and Bone Man I, The Depths of
 * Despair, The Queen of Thieves, Tale of the Righteous, The Forsaken Tower, The Ascent of Arceuus,
 * Getting Ahead and A Kingdom Divided. Lamp rewards are given as fixed XP (approximate).
 */
final class LateQuestsB {
    private LateQuestsB() {
    }

    static void register(QuestBook b) {
        b.place("elid_cave", "Source of the Elid", 3369, 3132, PlaceDef.Style.CAVE); // the root over the waterfall cave (Water Ravine)
        b.place("sophanem_dungeon", "Sophanem dungeon", 3290, 2770, PlaceDef.Style.CAVE);
        b.place("necropolis", "Necropolis", 3340, 2730, PlaceDef.Style.RUINS);
        b.place("piscatoris", "Piscatoris Fishing Colony", 2342, 3650, PlaceDef.Style.CAMP);
        b.spot("rat_pits", "Varrock rat pits", 3268, 3402);
        b.place("mort_ton", "Mort'ton", 3490, 3290, PlaceDef.Style.RUINS);
        b.spot("odd_old_man", "Odd Old Man's tent", 3362, 3505);
        b.place("hosidius", "Hosidius", 1760, 3600, PlaceDef.Style.TOWN);
        b.place("shayzien", "Shayzien", 1500, 3600, PlaceDef.Style.TOWN);
        b.place("lovakengj", "Lovakengj", 1500, 3800, PlaceDef.Style.TOWN);
        b.place("arceuus", "Arceuus", 1680, 3800, PlaceDef.Style.TOWN);
        b.place("forsaken_tower", "The Forsaken Tower", 1385, 3825, PlaceDef.Style.RUINS);
        b.place("lizardman_canyon", "Lizardman Canyon", 1515, 3700, PlaceDef.Style.CAVE);
        b.place("kebos_lowlands", "Kebos Lowlands", 1240, 3640, PlaceDef.Style.RUINS);
        spiritsOfTheElid(b);
        contact(b);
        beneathCursedSands(b);
        swanSong(b);
        ratcatchers(b);
        shadesOfMortton(b);
        ragAndBoneMan(b);
        kourend(b);
    }

    private static void spiritsOfTheElid(QuestBook b) {
        b.npc("awusah", "Awusah the Mayor", "NARDAH", 32, -36, // in the Mayor's house (OSRS 3442, 2912)
                0xB07A4E, 0xE0C082, 0xB09062, 0xE0E0ED);
        b.npc("ghaslor", "Ghaslor the Elder", "NARDAH", 30, -78, // in his house north of the Mayor's (OSRS 3441, 2933)
                0xA86A43, 0xE0D0A2, 0x8A6A41, 0xE0E0EE);
        b.item("ancestral_key", "Ancestral key", "key", 0xC0A063, "The key to the Elid's source.");
        b.quest("spirits_of_the_elid", "Spirits of the Elid", Difficulty.INTERMEDIATE, 2)
                .req(Skill.MAGIC, 33).req(Skill.RANGED, 37).req(Skill.MINING, 37).req(Skill.THIEVING, 37)
                .xp(Skill.PRAYER, 8000).xp(Skill.THIEVING, 1000).xp(Skill.MAGIC, 1000)
                .unlock("Nardah's water supply restored")
                .stage("I can start this quest by speaking to Awusah the Mayor in Nardah. (Requires 33 Magic, 37 Ranged, Mining and Thieving.)")
                .talk("awusah").say("The River Elid has dried up! Nardah will die without water. Something blocks the source in the mountains to the north.")
                .stage("I should ask Ghaslor the Elder about the Elid's source.")
                .talk("ghaslor").need("needle", "thread").reqSkill(Skill.MAGIC, 33).give("ancestral_key")
                .missing("The key lies in the house west of mine, with the old robes of Elidinis. Mend them with a needle and thread.").say("Our ancestors sealed the source with three spirits. Take the ancestral key; the spirits are angry that the people stopped honouring them.")
                .stage("I should go to the source of the Elid, appease the three spirits and remove the blockage.")
                .reach("elid_cave", 8).take("ancestral_key").need("rope").reqSkill(Skill.THIEVING, 37).reqSkill(Skill.MINING, 37)
                .reqSkill(Skill.RANGED, 37).flag("soe_spirits")
                .missing("* I need a rope for the root over the cave, and 37 Thieving, Mining and Ranged to clear the three channels.").say("* You defeat the three golem guardians and speak to the spirits: they forgive the people. The water flows again!")
                .stage("I should tell Awusah the water is back.")
                .talk("awusah").needFlag("soe_spirits").say("The Elid flows! Nardah is saved. Thank you, my friend.");
    }

    private static void contact(QuestBook b) {
        b.npc("jex", "Jex", "sophanem", 4, -4, 0xA86A44, 0x5A4A3F, 0x3A2A20, 0x2A1A11);
        b.npc("maisa", "Maisa", "sophanem_dungeon", -4, 2, 0xA86A46, 0x6A3A5A, 0x3A1A2A, 0x2A1A12);
        b.item("contact_parchment", "Parchment", "note", 0xD8C8A0, "A parchment found on Kaleef's body.");
        b.items(s -> s.add("keris", "Keris", Kind.MELEE_WEAPON, "dagger", 0xB0A060).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(40, 20, -10, 2, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0),
                        Map.of(Skill.ATTACK, 50), 4, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("A dagger that deals extra damage to kalphites and scarabs."));
        b.boss(BossDef.of("giant_scarab", "Giant scarab").level(191).stats(130, 150, 140, 130, 120, 1).speed(4)
                .defenceBonus(80, 80, 80, 0, 80)
                .attack(AttackDef.of("mandibles", AttackType.STAB, 20, 3, 6).special(Special.POISON, 8))
                .attack(AttackDef.of("swarm", AttackType.RANGED, 12, 8, 3).special(Special.AOE, 4).telegraph(2, "The giant scarab summons a swarm!"))
                .drops(Drop.always("bones")).size(1.6f, 1.2f, 1.0f).at("sophanem_dungeon").arena(10, "sand"));
        b.quest("contact", "Contact!", Difficulty.EXPERIENCED, 1)
                .reqQuest("prince_ali_rescue", "icthlarins_little_helper")
                .xp(Skill.THIEVING, 7000).xp(Skill.RANGED, 7000).reward("keris")
                .stage("I can start this quest by speaking to the High Priest in Sophanem. (Requires Prince Ali Rescue, Icthlarin's Little Helper.)")
                .talk("high_priest_sophanem").say("Sophanem and Menaphos have lost contact! Is there any way into Menaphos from below?",
                        "Jex, a retired adventurer, lives in the north-easternmost building of the city. He knows the tunnels.")
                .stage("I should find Jex in the north-easternmost building of Sophanem.")
                .talk("jex").say("The tunnels beneath Sophanem lead to Menaphos. I lost my nerve down there. Something huge lives below.",
                        "Better get down there. Take a light: the ladder in my house leads down to a trapdoor.")
                // OSRS: the Sophanem Dungeon labyrinth (floor and wall traps, crushers, sand pits), Kaleef's body and Maisa.
                .stage("I should explore the dungeon beneath Sophanem with a light source, search the body I find there and speak to Maisa.")
                .reach("sophanem_dungeon", 10).stay().unless("contact_parchment").unlessFlag("ct_kaleef").need("tinderbox").flag("ct_kaleef").give("contact_parchment")
                .say("* You light your way past floor traps, wall traps and crusher blocks, and skirt the sand pits.",
                        "* Near a ladder you find the body of Kaleef, Osman's agent. A parchment is clutched in his hand.")
                .missing("* It's pitch black down here. You'll need a tinderbox to light your way.")
                .talk("maisa").needFlag("ct_kaleef").say("You found Kaleef? He was my contact. Tell Osman in Al Kharid: Draynor Village... and Leela.",
                        "Those words will tell him you speak for me.")
                .missing("Who are you? I'm waiting for someone. He should have come through the tunnels by now.")
                .stage("I should take Kaleef's parchment to Osman in front of the Al Kharid palace.")
                .talk("osman").take("contact_parchment").say("Draynor Village, Leela... you've met Maisa. And Kaleef is dead.",
                        "A route between Sophanem and Menaphos would drive a wedge between the Menaphite cities. Meet me outside Sophanem's gates.")
                .missing("I want to talk to you about Sophanem? Then bring me something more than words. What did Kaleef carry?")
                .stage("I must navigate the tunnels again with Osman and kill the giant scarab, then tell the High Priest.")
                .kill("giant_scarab").stay().unlessFlag("ct_scarab").flag("ct_scarab")
                .say("* The giant scarab is slain. Behind it you find the passage to Menaphos and a strange dagger: a keris.",
                        "@Osman: Contact with Menaphos restored! Keep that keris; it'll serve you well against scarabs and kalphites.")
                .talk("high_priest_sophanem").needFlag("ct_scarab").say("The way to Menaphos is open again? Icthlarin be praised! Sophanem thanks you.")
                .missing("The tunnels still crawl with scarabs. Osman waits for you at the gates.");
    }

    private static void beneathCursedSands(QuestBook b) {
        b.npc("tumeken_priest", "Priest of Tumeken", "necropolis", 3, 3, 0xA86A45, 0xE0C051, 0xF0F0F4, 0xE0C052);
        // Wiki monster stats: level 379, 600 HP, 160 in every combat stat but Ranged, melee and magic up to 24, every 4 ticks.
        b.boss(BossDef.of("champion_of_scabaras", "Champion of Scabaras").level(379).stats(600, 160, 160, 160, 160, 1).speed(4)
                .defenceBonus(120, 160, 160, 340, 5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 24, 3, 6))
                .attack(AttackDef.of("locust_swarm", AttackType.MAGIC, 24, 9, 3).special(Special.AOE, 5).telegraph(2, "Locusts swarm from the champion's shell!"))
                .attack(AttackDef.of("sand_spike", AttackType.MAGIC, 24, 10, 3).special(Special.SKY_STRIKE, 0).telegraph(2, "The sand ripples beneath you!"))
                .drops(Drop.always("bones")).size(1.6f, 1.4f, 1.2f).at("necropolis").arena(12, "sand"));
        // Wiki: level 174, 180 HP, stabs up to 16; an overhead protection prayer costs a third of the player's Hitpoints
        // (QuestBossScripts.FalseGods).
        b.boss(BossDef.of("head_menaphite_guard", "Head Menaphite Guard").level(174).stats(180, 180, 150, 150, 150, 1).speed(4)
                .defenceBonus(10, 30, 30, 0, 50)
                .attack(AttackDef.of("stab", AttackType.STAB, 16, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xA86A45, 0xE0C050, 0x6A4A2A, 0xC0A030));
        // Wiki: level 351, 480 HP, slashes with a dragon scimitar (max hit unknown: 22 from her stats, approx) and every seventh
        // action a lightning bolt in front of her for up to 80 that can be walked out of. Her Menaphite Shadow is not modelled.
        b.boss(BossDef.of("menaphite_akh", "Menaphite Akh").level(351).stats(480, 150, 150, 140, 150, 150).speed(4)
                .defenceBonus(5, 30, 30, 280, 180)
                .attack(AttackDef.of("scimitar", AttackType.SLASH, 22, 2.5, 6))
                .attack(AttackDef.of("lightning", AttackType.MAGIC, 80, 6, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "The Menaphite Akh twitches, and lightning gathers in front of her!"))
                .drops(Drop.always("bones")).skin(0xA86A45, 0x8A1A2A, 0x3A1A1A, 0xE0C050));
        b.spot("bcs_jamila", "Jamila's stall", 3311, 2780);
        b.npc("jamila", "Jamila", "bcs_jamila", 0, 0, 0xA86A47, 0xC05A3A, 0x6A3A2A, 0x2A1A13);
        b.spot("bcs_ruins_of_ullek", "Ruins of Ullek", 3402, 2812); // the furnace and well (quest-helper zone 3388-3416, 2803-2821)
        // Maisa at her campsite by the cliffs east of Sophanem, and in the north of the necropolis for the end (quest-helper tiles).
        b.npcAt("maisa_bcs", "Maisa", 3378, 2792, 0xA86A46, 0x6A3A5B, 0x3A1A2A, 0x2A1A12);
        b.npcAt("maisa_necropolis", "Maisa", 3327, 2740, 0xA86A46, 0x6A3A5C, 0x3A1A2A, 0x2A1A12);
        b.npcAt("bcs_menaphite_citizen", "Entranced citizen", 3347, 2718, 0xA86A48, 0xC0B090, 0x6A5A3A, 0x2A1A14);
        b.npcAt("zahur", "Zahur", 3425, 2909, 0xA86A49, 0x3A6A5A, 0x2A3A2A, 0x2A1A15); // her shop south of Nardah's fountain
        b.spot("bcs_elid_lily", "Lily of the Elid island", 3353, 2927); // the stepping stones west of Nardah
        b.item("bcs_message", "Message", "note", 0xE0D0B0, "A message slipped to you by Jamila.");
        b.item("lily_of_the_elid", "Lily of the Elid", "herb", 0xF0F0E0, "A rare lily that grows on an island in the River Elid.");
        b.item("cure_crate", "Cure crate", "package", 0xA08050, "A crate of Zahur's cure for the pox.");
        b.items(s -> s.add("keris_partisan", "Keris partisan", Kind.MELEE_WEAPON, "spear", 0xC0A060).value(60000).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(58, -2, 57, 2, 0, 0, 0, 0, 0, 0, 45, 0, 0, 3),
                        Map.of(Skill.ATTACK, 65), 4, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("A partisan with a keris blade, deadly to kalphites and scarabs."));
        b.items(s -> s.add("circlet_of_water", "Circlet of water", Kind.ARMOUR, "full_helm", 0x5A9AD0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 1))
                .examine("A circlet that keeps its wearer cool in the desert heat."));
        b.quest("beneath_cursed_sands", "Beneath Cursed Sands", Difficulty.MASTER, 2)
                .xp(Skill.AGILITY, 50000) // OSRS (current quest XP rewards)
                .reqQuest("contact").req(Skill.AGILITY, 62).req(Skill.CRAFTING, 55).req(Skill.FIREMAKING, 55)
                .reward("keris_partisan", "circlet_of_water")
                .unlock("Access to the Necropolis and the Tombs of Amascut")
                .stage("I can start this quest by speaking to Jamila at her stall in Sophanem. (Requires Contact!, 62 Agility, 55 Crafting, 55 Firemaking.)")
                .talk("jamila").give("bcs_message")
                .say("For helping Sophanem, an exclusive piece from my stall! *As she fits a gold ring on your finger, she slips you a message.*",
                        "Oh dear, it's too small. Never mind.", "* The message asks you to meet its sender at a campsite by the cliffs east of the city.")
                // Wiki: Maisa and the necropolis, the entranced Head Menaphite Guard, the Ruins of Ullek (the furnace, the AKHEM chest,
                // the iron scarab emblem, the scarab mages), the lever maze, the riddle of the tomb's urns and Mehhar's spirit.
                .stage("I should meet Maisa at her campsite east of Sophanem, find out what the Menaphites are digging up in the necropolis and "
                        + "follow the scabarites to the Ruins of Ullek.")
                .talk("maisa_bcs").stay().unlessFlag("bcs_maisa").flag("bcs_maisa").travel("necropolis")
                .say("You came! Citizens of Menaphos have slipped the quarantine to dig in the old necropolis, and they act... strangely. Come with me.",
                        "* You and Maisa arrive at the necropolis, where entranced citizens and guards dig at the Jaltevas Pyramid.",
                        "* Its entry is blocked. Perhaps one of the diggers will talk.")
                .talk("bcs_menaphite_citizen").stay().needFlag("bcs_maisa").unlessFlag("bcs_guard").spawn("head_menaphite_guard")
                .say("...dig... the goddess wills it... dig...", "@Maisa: Look out! The Head Menaphite Guard! Don't pray against him: his "
                        + "false-gods trick hurts more than his spear.")
                .kill("head_menaphite_guard").stay().needFlag("bcs_maisa").unlessFlag("bcs_guard").flag("bcs_guard")
                .say("* The guard snaps out of his trance: Amascut hypnotised them and unsealed her tomb in the pyramid. Scabarites helped her,",
                        "* coming from ruins to the north-east. Maisa asks you to meet her back at her camp.")
                .talk("maisa_bcs").stay().needFlag("bcs_guard").unlessFlag("bcs_explore").flag("bcs_explore")
                .say("Explore the cliffs and find those scabarites. I'll work out how to get back into Menaphos.",
                        "There's a tinderbox in the camping equipment and a spade north of the camp if you need them.")
                .puzzle("bcs_ruins_of_ullek", BCS_CHEST).stay().needFlag("bcs_explore").unlessFlag("bcs_chest")
                .need("tinderbox", "spade").take("coal", "iron_bar").flag("bcs_chest")
                .say("* The chest holds a scarab mould. You cast an iron scarab emblem at the furnace, set it in the pillar facing down and the",
                        "* stairs open. Two scarab mages fall on you, but you beat them off, climb down and pull the maze's two levers in time.")
                .puzzle("bcs_ruins_of_ullek", BCS_URNS).stay().needFlag("bcs_chest").unlessFlag("bcs_urns").flag("bcs_urns")
                .say("* You pull the lever, and the door to the inner tomb grinds open.")
                .reach("bcs_ruins_of_ullek", 8).needFlag("bcs_urns").spawn("champion_of_scabaras")
                .say("* The spirit of Mehhar, son of Osmumten, teaches you an incantation to free the High Priest of Scabaras. You take a rusty",
                        "* key from the urn at the back and unlock the door upstairs: the Champion of Scabaras!")
                .stage("I must defeat the Champion of Scabaras.")
                .kill("champion_of_scabaras")
                .say("* The Champion of Scabaras falls. You free the High Priest of Scabaras: Amascut has captured the lesser gods in Osmumten's",
                        "* tomb and controls the Pharaoh. He confesses to the locusts, the frogs and the poisoned cows, but not to the pox.")
                // Wiki: Zahur and Maisa in Nardah, the Lily of the Elid past the crocodile, Zahur's chemistry equipment, the cure crate to
                // the High Priest in Sophanem, then the Menaphite Akh after freeing the necropolis with Maisa.
                .stage("I should help Zahur in Nardah make a cure for the pox, take it to the High Priest in Sophanem and free the necropolis "
                        + "with Maisa.")
                .talk("zahur").stay().unlessFlag("bcs_zahur").flag("bcs_zahur")
                .say("@Maisa: The pox comes from the bark of the cadavaberry bush. Zahur can treat it.",
                        "I can, with a Lily of the Elid. They grow on the island west of here. Take some raw beef for the crocodile.")
                .reach("bcs_elid_lily", 6).stay().needFlag("bcs_zahur").unlessFlag("bcs_cure").unless("lily_of_the_elid", "cure_crate")
                .take("minecraft:beef").give("lily_of_the_elid")
                .say("* A hungry crocodile blocks the stepping stones. You throw it the meat (you name it Roger) and pick a Lily of the Elid.")
                .missing("* A hungry crocodile guards the stepping stones. Some raw beef might distract it.")
                .talk("zahur").stay().needFlag("bcs_zahur").unlessFlag("bcs_cure").take("lily_of_the_elid").give("cure_crate")
                .say("* You warm up her chemistry equipment until every heat sits at the mark.",
                        "There: the cure. Take this crate to the High Priest in Sophanem.")
                .missing("I need a Lily of the Elid from the island west of Nardah.")
                .talk("high_priest_sophanem").stay().needFlag("bcs_zahur").unlessFlag("bcs_cure").take("cure_crate").flag("bcs_cure")
                .say("A cure for the pox! Sophanem will recover. Maisa waits for you at the necropolis.")
                .talk("maisa_necropolis").stay().needFlag("bcs_cure").unlessFlag("bcs_akh").spawn("menaphite_akh")
                .say("* You chant the incantation, and everyone in the necropolis is freed from Amascut's influence.",
                        "@Maisa: Osman! Spying on me, invisible? I'm leaving.",
                        "* As you argue with Osman, lightning strikes the sand and scarlet flames burn: a Menaphite Akh appears!")
                .kill("menaphite_akh").stay().needFlag("bcs_cure").unlessFlag("bcs_akh").flag("bcs_akh")
                .say("@Osman: Amascut? I still don't believe it... but there's more to this than I thought. Maisa will have gone to Sophanem.")
                .talk("high_priest_sophanem").needFlag("bcs_akh")
                .say("The plagues are ended for good! Come, meet the Menaphites at the bridge.",
                        "* Pharaoh Kemesis himself appears: Menaphos will prepare to end the quarantine.")
                .missing("?!bcs_zahur|The pox spreads. Zahur in Nardah knows herbs; perhaps she can make a cure.",
                        "?bcs_zahur|?!bcs_cure|Zahur is making a cure for the pox? Bring it to me when it's ready.",
                        "?bcs_cure|Maisa needs you at the necropolis. Amascut's servants are still about.")
                // saves from before round 33 reached this stage without Maisa's flags: the old ending (report to the Priest of Tumeken)
                .talk("tumeken_priest").unlessFlag("bcs_maisa")
                .say("Amascut's curse on the Menaphites is broken, and the way into her tombs is open. Take these, with Sophanem's thanks.");
    }

    /** The scabarites' chest (wiki): its passcode is AKHEM as alphabet positions, 1 11 8 5 13. */
    static final QuestPuzzle BCS_CHEST = QuestPuzzle.builder("bcs_chest").variant(null)
            .prompt("* The stone tablet from the well: a chest lies buried in the southernmost ritual pillar, and 'Akhem keeps the count'.",
                    "* You dig it up. Its lock takes a passcode of digits.")
            .options("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
            .solution(1, 1, 1, 8, 5, 1, 3).failLines("* The lock doesn't open.").build();

    /**
     * The riddle of the tomb (wiki): which emblem goes in which urn, north to south, drawn per player. As in OSRS the two northern
     * urns are named by their gods ("the god 1 arrived just before the god 2") and the two southern ones by the offerings; the
     * exact wording is not in the dump (approx).
     */
    static final QuestPuzzle BCS_URNS = urnPuzzle();

    private static QuestPuzzle urnPuzzle() {
        String[] emblems = {"Baboon emblem", "Human emblem", "Crocodile emblem", "Scarab emblem"};
        String[] gods = {"goddess of companionship", "god of health", "goddess of resourcefulness", "god of isolation"};
        String[] gifts = {"some linen", "some wine", "a necklace", "a carving"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("bcs_urns").room("urns");
        List<int[]> orders = new java.util.ArrayList<>();
        permute(new int[]{0, 1, 2, 3}, 0, orders);
        for (int[] o : orders) {
            // o = emblem in the northern, centre-north, centre-south and southern urn
            b.variant(null).prompt("* Four urns stand in a row from north to south. The south-western plaque gives you four emblems; faded carvings",
                            "* show Apmeken, goddess of companionship, with linen (baboon); Het, god of health, with wine (human); Crondis, goddess",
                            "* of resourcefulness, with a necklace (crocodile); and Scabaras, god of isolation, with a carving (scarab). The riddle:",
                            "* 'The " + gods[o[0]] + " arrived just before the " + gods[o[1]] + ". To the one that arrived next, he offered "
                                    + gifts[o[2]] + ". The one that was offered " + gifts[o[3]] + " came last of all.' The first to arrive rests "
                                    + "furthest north.",
                            "* Place the emblems from the northern urn to the southern one.")
                    .options(emblems).solution(o);
        }
        return b.failLines("* As you pull the lever the urns spit poison at you. The emblems must be in the wrong urns.").build();
    }

    private static void permute(int[] a, int k, List<int[]> out) {
        if (k == a.length) {
            out.add(a.clone());
            return;
        }
        for (int i = k; i < a.length; i++) {
            int t = a[k];
            a[k] = a[i];
            a[i] = t;
            permute(a, k + 1, out);
            a[i] = a[k];
            a[k] = t;
        }
    }

    private static void swanSong(QuestBook b) {
        b.npc("herman_caranos", "Herman Caranos", "piscatoris", 0, 3, 0xE0B8B4, 0x3A5A8E, 0x2A3A5E, 0xA0A0A5);
        b.npc("franklin_caranos", "Franklin Caranos", "piscatoris", 3, -2, 0xE0B8B5, 0x6A5A40, 0x4A3A30, 0x6A4A23);
        b.boss(BossDef.of("sea_troll_queen", "Sea Troll Queen").level(170).stats(200, 160, 160, 140, 140, 1).speed(5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 25, 3, 6))
                .attack(AttackDef.of("water_blast", AttackType.MAGIC, 22, 10, 4).particle("minecraft:splash"))
                .drops(Drop.always("big_bones")).size(1.2f, 2.6f, 1.0f).at("piscatoris").arena(12, "water"));
        b.quest("swan_song", "Swan Song", Difficulty.MASTER, 2)
                .xp(Skill.FISHING, 50000).xp(Skill.MAGIC, 15000).xp(Skill.PRAYER, 10000) // OSRS (current quest XP rewards)
                .reward("coins:25000") // wiki: 25,000 coins
                .qp(100).reqQuest("garden_of_tranquillity").reqQuest("one_small_favour")
                .req(Skill.MAGIC, 66).req(Skill.COOKING, 62).req(Skill.FISHING, 62).req(Skill.SMITHING, 45).req(Skill.FIREMAKING, 42).req(Skill.CRAFTING, 40)
                .unlock("Fishing monkfish at the Piscatoris Fishing Colony")
                .stage("I can start this quest by speaking to Herman Caranos in the Piscatoris Fishing Colony. (Requires 100 quest points, Garden of Tranquillity and more.)")
                .talk("herman_caranos").say("Sea trolls are attacking the colony! The wall is broken and we've no food. Help us!")
                .stage("I should repair the colony wall (5 planks, 2 iron bars) and help Franklin cook for the fishermen.")
                .talk("franklin_caranos").take("plank:5", "iron_bar:2", "raw_monkfish:5").say("*You fix the wall and cook monkfish for the colony.* The sea trolls are coming again, and their queen leads them!")
                .missing("We need 5 planks and 2 iron bars for the wall, and 5 raw monkfish to feed everyone.")
                .stage("I must defeat the Sea Troll Queen.")
                .kill("sea_troll_queen").say("* The Sea Troll Queen sinks beneath the waves. The colony is saved!")
                .stage("I should tell Herman Caranos.")
                .talk("herman_caranos").say("The colony is safe! You can fish here any time, friend.");
    }

    private static void ratcatchers(QuestBook b) {
        b.npc("phingspet", "Phingspet", "rat_pits", 0, 0, 0xE0B8B6, 0x5A4A41, 0x3A2A21, 0x2A1A12);
        b.item("rat_pole", "Rat pole", "staff", 0x8A6A41, "A pole with dead rats tied to it.");
        b.boss(BossDef.of("rat_king", "King rat").level(53).stats(80, 50, 50, 40, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 9, 2.5, 10).special(Special.POISON, 4))
                .drops(Drop.always("bones")).size(1.0f, 1.0f, 1.0f).at("rat_pits").arena(8, "stone"));
        b.quest("ratcatchers", "Ratcatchers", Difficulty.INTERMEDIATE, 2)
                .reqQuest("icthlarins_little_helper", "gertrudes_cat").xp(Skill.THIEVING, 4500)
                .unlock("The Varrock rat pits")
                .stage("I can start this quest by speaking to Phingspet near the Varrock rat pits. (Requires Icthlarin's Little Helper, Gertrude's Cat.)")
                .talk("phingspet").say("The Rat Charmer's gang wants a ratcatcher with a good cat. Prove yours: clear rats from Varrock.")
                .stage("I should hunt rats with my cat around Varrock and bring back a rat pole as proof.")
                .drop("minecraft:silverfish", "rat_pole", 2)
                .talk("phingspet").take("rat_pole").need("gertrudes_kitten").say("Nice pole! Now the big job: the King rat in the pits.")
                .missing("Bring me a rat pole, and you'll need your cat.")
                .stage("I must defeat the King rat in the Varrock rat pits.")
                .kill("rat_king").say("* The King rat is defeated! The ratcatchers cheer.")
                .stage("I should report to Phingspet.")
                .talk("phingspet").say("You and your cat are true ratcatchers! The rat pits are open to you.");
    }

    private static void shadesOfMortton(QuestBook b) {
        b.npc("razmire_keelgan", "Razmire Keelgan", "mort_ton", 0, 3, 0xD8C8B8, 0x6A6A41, 0x4A4A31, 0x6A4A24);
        // sacred_oil is in CommonItems (also made and used for pyre logs by the minigames module).
        b.quest("shades_of_mortton", "Shades of Mort'ton", Difficulty.INTERMEDIATE, 3)
                .reqQuest("priest_in_peril").req(Skill.CRAFTING, 20).req(Skill.HERBLORE, 15).req(Skill.FIREMAKING, 5)
                .xp(Skill.CRAFTING, 2000).xp(Skill.HERBLORE, 2000)
                .unlock("The Mort'ton temple and shade burning")
                .stage("I can start this quest by speaking to Razmire Keelgan in Mort'ton. (Requires Priest in Peril, 20 Crafting, 15 Herblore, 5 Firemaking.)")
                .talk("razmire_keelgan").say("The afflicted plague has struck Mort'ton, and shades walk the streets. The temple's flame must be relit.")
                .stage("I should repair the temple (4 planks) and make sacred oil (olive oil blessed at the temple: a vial of water and a guam leaf).")
                .talk("razmire_keelgan").take("plank:4", "vial_of_water", "guam_leaf").give("sacred_oil").say("*You repair the temple walls and bless oil at the altar.*")
                .missing("We need 4 planks for the temple, and a vial of water and a guam leaf for the oil.")
                .stage("I should light the temple's sacred flame with the sacred oil.")
                .reach("mort_ton", 8).take("sacred_oil").need("tinderbox").say("* The sacred flame roars to life. The shades flee from its light!")
                .stage("I should tell Razmire.")
                .talk("razmire_keelgan").say("The flame burns! The townspeople can be cured. Thank you, friend.");
    }

    private static void ragAndBoneMan(QuestBook b) {
        b.npc("odd_old_man", "Odd Old Man", "odd_old_man", 0, 0, 0xD8C8B9, 0x6A5A42, 0x4A3A32, 0xC0C0C5);
        b.item("bone_in_vinegar", "Bones in vinegar", "pot", 0xE0D8C7, "Bones soaking in vinegar, to be cleaned.");
        b.quest("rag_and_bone_man_i", "Rag and Bone Man I", Difficulty.NOVICE, 1)
                .xp(Skill.COOKING, 500).xp(Skill.PRAYER, 500)
                .stage("I can start this quest by speaking to the Odd Old Man at the Limestone Mine north-east of Varrock.")
                .talk("odd_old_man").say("I collect bones for my museum! Bring me bones from a goblin, a giant rat... and some big bones. Cleaned in vinegar, mind.")
                .stage("I need 4 bones and 2 big bones, cleaned in vinegar over a fire (a pot and a tinderbox).")
                .talk("odd_old_man").take("bones:4", "big_bones:2").need("tinderbox").give("bone_in_vinegar")
                .say("*You boil the bones in vinegar and polish them.* Lovely, lovely specimens.")
                .missing("I need 4 bones and 2 big bones, and you'll need a tinderbox to boil them clean.")
                .stage("I should hand over the cleaned bones.")
                .talk("odd_old_man").take("bone_in_vinegar").say("My collection grows! Splendid. Come back if you find more interesting bones.");
    }

    /** The Forsaken Furnace (wiki): four gallons of coolant in the 5-gallon jug, with an 8-gallon jug (the unique 11-action way). */
    static final QuestPuzzle FSK_FURNACE = QuestPuzzle.builder("fsk_furnace").variant(null)
            .prompt("* The furnace's coolant mechanism must hold exactly four gallons, poured from the 5-gallon jug. In the back room's cupboards",
                    "* you find a 5-gallon jug and an 8-gallon jug; a coolant dispenser stands in the north-west corner.")
            .options("Fill the 5-gallon jug", "Fill the 8-gallon jug", "Pour the 5-gallon jug into the 8-gallon jug",
                    "Pour the 8-gallon jug into the 5-gallon jug", "Empty the 5-gallon jug", "Empty the 8-gallon jug",
                    "Pour the 5-gallon jug into the coolant mechanism", "Pour the 8-gallon jug into the coolant mechanism")
            .solution(0, 2, 0, 2, 5, 2, 0, 2, 0, 2, 6)
            .failLines("* That won't leave four gallons where you need them. You empty both jugs and start again.").build();

    /** The Pernicious Potions (wiki): the old notes' layout puts fluids A to E at positions 1, 2, 3, 5 and 4; which is the cleansing fluid varies. */
    static final QuestPuzzle FSK_POTION = potionPuzzle();
    /** The Pylons (wiki): four energy disks from the western pylon onto the middle one, the 15-move Tower of Hanoi solution. */
    static final QuestPuzzle FSK_PYLONS = QuestPuzzle.builder("fsk_pylons").variant(null)
            .prompt("* Four weighted energy disks of different sizes are stacked on the western of three pylons. Stack them, in order, on the",
                    "* middle pylon: one disk at a time, never a larger disk on a smaller one, in as few moves as possible (15).")
            .options("West to centre", "West to east", "Centre to west", "Centre to east", "East to west", "East to centre")
            .solution(1, 0, 5, 1, 2, 3, 1, 0, 5, 4, 2, 5, 1, 0, 5)
            .failLines("* The disks topple back onto the western pylon.").build();

    private static QuestPuzzle potionPuzzle() {
        String[] roles = {"A", "B", "C", "D", "E"};
        int[] position = {1, 2, 3, 5, 4};
        QuestPuzzle.Builder b = QuestPuzzle.builder("fsk_potion").room("refinery");
        for (int cleansing = 0; cleansing < roles.length; cleansing++) {
            String[] name = new String[roles.length];
            for (int i = 0; i < roles.length; i++) {
                name[i] = i == cleansing ? "the cleansing fluid" : "fluid " + roles[i];
            }
            b.variant(null).prompt("* Five unknown fluids stand in a row on the table, numbered 1 to 5 from the left. Only the cleansing fluid will",
                            "* cleanse the refinery. The old notes from the cupboard read:",
                            "* " + cap(name[0]) + " is directly left of " + name[1] + ". " + cap(name[1]) + " is next to " + name[2] + ".",
                            "* " + cap(name[3]) + " is directly right of " + name[4] + ". " + cap(name[0]) + " is opposite " + name[3] + ".")
                    .options("Unknown fluid 1", "Unknown fluid 2", "Unknown fluid 3", "Unknown fluid 4", "Unknown fluid 5")
                    .solution(position[cleansing] - 1);
        }
        return b.failLines("* The refinery spits the fluid back at you, burning and poisoning you. That was the wrong one.").build();
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void kourend(QuestBook b) {
        b.npc("lord_kandur_hosidius", "Lord Kandur Hosidius", "hosidius", 0, 3, 0xE8C0C4, 0x3A8A3A, 0x2A5A2A, 0xE0C053);
        b.npc("lady_shauna_piscarilius", "Lady Shauna Piscarilius", "great_kourend", 3, -2, 0xF0D0C3, 0x3A5A9B, 0x2A3A6B, 0xC08A3F);
        b.npc("lord_shiro_shayzien", "Lord Shiro Shayzien", "shayzien", 0, 3, 0xC8986D, 0xA02A2C, 0x5A1A1C, 0x2A2A30);
        // Lovakengj's leader, Lady Vulcana (the id predates the rename), at the Lovakengj Assembly (wiki start tile 1483, 3748).
        b.spot("fsk_lovakengj_assembly", "Lovakengj Assembly", 1483, 3748);
        b.npc("lord_kandur_lovakengj", "Lady Vulcana Lovakengj", "fsk_lovakengj_assembly", 0, 0, 0xD8A88D, 0x6A6A6C, 0x3A3A3F, 0xC07A30);
        // Undor on the west side of the Wintertodt camp. // approx tile
        b.spot("fsk_wintertodt_camp", "Wintertodt camp (Undor)", 1622, 3944);
        b.npc("undor", "Undor", "fsk_wintertodt_camp", 0, 0, 0xD8A880, 0x5A4A3A, 0x3A2A1A, 0x8A5A2A);
        b.item("dinhs_hammer", "Dinh's hammer", "hammer", 0x8A8AA0, "The hammer of Dinh, maker of the Doors of Dinh.");
        b.npc("lord_trobin_arceuus", "Lord Trobin Arceuus", "arceuus", 0, 3, 0xE8D0C3, 0x5A2A8D, 0x3A1A5D, 0xE0E0EF);
        b.npc("kaht_bbalam", "Kaht B'alam", "kebos_lowlands", 2, 2, 0x8A9A6B, 0x3A6A3E, 0x2A4A2F, 0x6A8A4B);
        b.npc("councillor_andrews", "Councillor Andrews", "great_kourend", -3, -2, 0xE0B8B7, 0x2A2A2E, 0x1A1A1F, 0xA0A0A6);
        b.npcAt("gordon_kebos", "Gordon", 1248, 3686, 0xD8A880, 0x6A5A2A, 0x3A3A2A, 0x8A6A3A); // his farm south of the Farming Guild
        b.npcAt("tomas_lawry", "Tomas Lawry", 1796, 3782, 0xE0B898, 0x3A4A5A, 0x2A2A3A, 0x5A3A2A); // outside the Port Piscarilius bank
        b.npcAt("phileas_rimor", "Phileas Rimor", 1543, 3571, 0xE8C8B0, 0x6A2A2A, 0x3A2A2A, 0xD0D0D0); // his house in Shayzien
        b.npcAt("mori", "Mori", 1698, 3742, 0xE0C0A8, 0x4A2A6A, 0x2A1A3A, 0x2A2A2A); // in Arceuus
        b.npcAt("martin_holt", "Martin Holt", 1664, 3670, 0xE0B8A0, 0x2A4A6A, 0x1A2A3A, 0x8A7A5A); // at the entrance to Kourend Castle
        b.item("lovakengj_plans", "Forsaken tower plans", "note", 0xD0C0A2, "Plans of the Forsaken Tower.");
        b.item("dark_essence_fragment", "Dark essence fragments", "essence", 0x3A2A5A, "Fragments of dark essence.");
        b.item("kourend_favour_letter", "Royal letter", "note", 0xE0D8C8, "A royal letter from the Kourend Council.");
        b.boss(BossDef.of("corrupt_lizardman", "Corrupt lizardman").level(150).stats(150, 120, 120, 110, 100, 1).speed(4)
                .attack(AttackDef.of("spear", AttackType.STAB, 18, 3, 6))
                .attack(AttackDef.of("acid", AttackType.RANGED, 14, 8, 3).special(Special.HAZARD, 3))
                .drops(Drop.always("bones")).skin(0x4A6A3A, 0x2A4A2A, 0x1A3A1A, 0x6A8A4A).size(0.9f, 2.4f, 1.3f).at("lizardman_canyon").arena(10, "jungle"));
        b.boss(BossDef.of("judge_of_yama", "Judge of Yama").level(168).stats(180, 150, 150, 130, 150, 1).speed(4)
                .defenceBonus(60, 60, 60, 40, 60)
                .attack(AttackDef.of("claws", AttackType.SLASH, 24, 3, 6))
                .attack(AttackDef.of("void_flare", AttackType.MAGIC, 22, 10, 4).special(Special.DRAIN_PRAYER, 6).particle("minecraft:soul_fire_flame"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("kebos_lowlands").arena(12, "void"));

        b.quest("the_depths_of_despair", "The Depths of Despair", Difficulty.INTERMEDIATE, 1)
                .reqQuest("client_of_kourend").req(Skill.AGILITY, 18).xp(Skill.AGILITY, 1500).reward("coins:4000") // wiki: 4,000 coins
                .stage("I can start this quest by speaking to Lord Kandur Hosidius in Hosidius. (Requires Client of Kourend, 18 Agility.)")
                .talk("lord_kandur_hosidius").say("An ancient Hosidius heir, Lady Arceuus? No... a relic of our house was lost in the Crabclaw Caves. Find it.")
                .stage("I should search for the lost Hosidius relic beneath Hosidius.")
                .reach("arceuus", 12).stay().unlessFlag("dod_book").flag("dod_book")
                .say("* In the Arceuus Library Galana helps you find the Varlamore envoy book. It mentions Artur Hosidius and the Crabclaw Caves.")
                .reach("hosidius", 10).needFlag("dod_book").flag("dod_chest").missing("* I don't know where to look yet. The Arceuus Library may have records.")
                .say("* In the Crabclaw Caves you meet the ghost of Artur Hosidius, kill the Sand Snake (level 36) and search the chest.",
                        "* In the caves beneath Hosidius you find the relic: the Book of the Dead's lost pages.")
                .stage("I should return to Lord Kandur Hosidius.")
                .talk("lord_kandur_hosidius").needFlag("dod_chest").missing("Any news from the Crabclaw Caves?").say("The relic is home! Hosidius is grateful.");
        b.item("qot_letter", "Letter", "note", 0xE0D8C2, "A letter found in Councillor Hughes's chest.");
        b.quest("the_queen_of_thieves", "The Queen of Thieves", Difficulty.INTERMEDIATE, 1)
                .reqQuest("client_of_kourend").req(Skill.THIEVING, 20).xp(Skill.THIEVING, 2000).reward("coins:2000") // wiki: 2,000 coins
                .stage("I can start this quest by speaking to Tomas Lawry outside the Port Piscarilius bank. (Requires Client of Kourend, 20 Thieving.)")
                .talk("tomas_lawry").say("I'm investigating Councillor Hughes for Lady Shauna Piscarilius.", "@Lady Shauna Piscarilius: Councillor Hughes is corrupt, and only a thief can prove it. The Queen of Thieves runs the Piscarilius underworld. Help her, and she'll help us.")
                .stage("I should prove myself to the Queen of Thieves, then steal evidence from Councillor Hughes's house in Kingstown.")
                .reach("great_kourend", 8).stay().unlessFlag("qot_warrens").take("stew").flag("qot_warrens")
                .say("* Robert O'Reilly takes your stew and shows you the manhole into the Warrens. Devan Rutter sends you to 'deal with' Conrad King.",
                        "* You stage Conrad's murder on the dock, and the Queen of Thieves receives you in her tent.")
                .missing("* Robert O'Reilly won't talk to an empty-handed stranger. He's hungry: bring him a stew.")
                .reach("quest_kourend_castle", 10).needFlag("qot_warrens").reqSkill(Skill.THIEVING, 20).give("qot_letter")
                .say("* In Hughes's house in Kingstown you picklock the chest in the east of the room and find a letter: proof of bribery!")
                .missing("* The Queen of Thieves hasn't vouched for me yet; the Warrens come first.")
                .stage("I should take the evidence to Lady Shauna.")
                .talk("lady_shauna_piscarilius").take("qot_letter").missing("Do you have the evidence from Hughes's house?").say("Hughes is finished! The Queen of Thieves sends her regards. So do I.");
        b.quest("tale_of_the_righteous", "Tale of the Righteous", Difficulty.INTERMEDIATE, 1)
                .reqQuest("client_of_kourend").req(Skill.STRENGTH, 16).req(Skill.MINING, 10)
                .reward("coins:8000") // OSRS: no XP reward
                .stage("I can start this quest by speaking to Phileas Rimor in his house in Shayzien. (Requires Client of Kourend, 16 Strength, 10 Mining.)")
                .talk("phileas_rimor").say("I'm a historian, and I've found something Lord Shiro must hear.", "@Lord Shiro Shayzien: A historian discovered that a lizardman corrupted by the old shrine threatens our soldiers. Investigate Lizardman Canyon.")
                .stage("I must defeat the corrupt lizardman in Lizardman Canyon.")
                .talk("quest_archeio").stay().unlessFlag("totr_prison").need("rope").flag("totr_prison")
                .say("Pagida can send you to the Library Historical Archive's Tower of Magic prison.",
                        "* You push the strange device and attack it with magic, melee and ranged, and investigate the skeleton in the northern cell.")
                .missing("Phileas Rimor sent you? Bring a rope: the trail ends in a crevice on Mount Quidamortem.")
                .kill("corrupt_lizardman").flag("totr_lizard").say("* The corrupt lizardman falls. The shrine's dark influence fades.")
                .stage("I should report to Lord Shiro Shayzien.")
                .talk("lord_shiro_shayzien").needFlag("totr_lizard").say("Shayzien is safer for your bravery. Thank you.");
        // Wiki: Lady Vulcana Lovakengj at the Assembly; Undor at the Wintertodt camp needs Dinh's hammer from the Forsaken Tower, whose
        // display case opens when its four puzzles are solved (the furnace's coolant, the power grid, the refinery's cleansing fluid
        // and the pylons' energy disks).
        b.quest("the_forsaken_tower", "The Forsaken Tower", Difficulty.INTERMEDIATE, 1)
                .reqQuest("client_of_kourend").xp(Skill.MINING, 500).xp(Skill.SMITHING, 500).reward("coins:6000")
                .unlock("Free use of the Lovakengj Minecart Network")
                .stage("I can start this quest by speaking to Lady Vulcana Lovakengj in the Lovakengj Assembly. (Requires Client of Kourend.)")
                .talk("lord_kandur_lovakengj").say("The Doors of Dinh are failing, and only Dinh knew their secrets.",
                        "@Councillor Unkar: The blacksmith Undor has found the cause. He's at the Wintertodt camp; go and help him.")
                .stage("I should help Undor at the Wintertodt camp, then recover Dinh's hammer from the Forsaken Tower west of Lovakengj.")
                .talk("undor").stay().unlessFlag("fsk_undor").flag("fsk_undor")
                .say("Someone damaged the Doors on purpose. The hole is small, but enough for the Wintertodt to draw power through.",
                        "I can fix it, but only with Dinh's hammer. It should be in the Forsaken Tower, on the peninsula west of Lovakengj.")
                .puzzle("forsaken_tower", FSK_FURNACE).stay().needFlag("fsk_undor").unlessFlag("fsk_furnace").flag("fsk_furnace")
                .say("* The coolant mechanism fills, and you light the furnace with the tinderbox from the cupboard. The first lock turns green.")
                .puzzle("forsaken_tower", FSK_POTION).stay().needFlag("fsk_furnace").unlessFlag("fsk_potion").flag("fsk_potion")
                .say("* The cleansing fluid scours the refinery, and it starts up. In the basement you crank the steam generator and align",
                        "* the power grid. Two more locks turn green.")
                .puzzle("forsaken_tower", FSK_PYLONS).stay().needFlag("fsk_potion").unlessFlag("fsk_pylons").flag("fsk_pylons")
                .say("* The last disk settles on the middle pylon, and the runic altar glows.")
                .reach("forsaken_tower", 8).needFlag("fsk_pylons").give("dinhs_hammer")
                .say("* Every lock on the display case is green. Inside lies Dinh's hammer.")
                .stage("I should take Dinh's hammer to Undor at the Wintertodt camp, then tell Lady Vulcana Lovakengj the Doors of Dinh will hold.")
                .talk("undor").stay().unlessFlag("fsk_doors").take("dinhs_hammer").flag("fsk_doors")
                .say("Dinh's hammer! With this I can mend the Doors, though the pyromancers will have to keep subduing the Wintertodt.",
                        "Tell Lady Vulcana the Doors will hold.")
                .missing("Dinh's hammer should be in the Forsaken Tower, west of Lovakengj.")
                .talk("lord_kandur_lovakengj").needFlag("fsk_doors")
                .say("The Doors will hold? Then Kourend owes you. Take this, and ride our minecarts freely.")
                .missing("You have Dinh's hammer? Then take it to Undor at the Wintertodt camp at once!")
                // saves from before round 33 reached this stage without meeting Undor (and without the hammer): the old ending
                .talk("lord_kandur_lovakengj").unlessFlag("fsk_undor")
                .say("The tower's power flows to our forges again? Then Kourend owes you. Take this, and ride our minecarts freely.");
        b.quest("the_ascent_of_arceuus", "The Ascent of Arceuus", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.HUNTER, 1500).xp(Skill.RUNECRAFT, 500) // OSRS (current quest XP rewards)
                .reward("coins:2000") // wiki: 2,000 coins
                .reqQuest("client_of_kourend").req(Skill.HUNTER, 12)
                .stage("I can start this quest by speaking to Mori in Arceuus. (Requires Client of Kourend, 12 Hunter.)")
                .talk("mori").say("Something is wrong in Arceuus. Lord Trobin must hear of it.", "@Lord Trobin Arceuus: Strange tormented souls are rising near the Dark Altar. My house's magic may be to blame. Investigate.")
                .stage("I should investigate the Dark Altar and collect dark essence fragments.")
                .reach("arceuus", 10).give("dark_essence_fragment:10").say("* Near the Dark Altar you trap the tormented souls and gather dark essence fragments.")
                .stage("I should bring the fragments to Lord Trobin.")
                .talk("lord_trobin_arceuus").take("dark_essence_fragment:10").say("The souls are at rest. Arceuus's magic is safe... for now.");
        b.quest("getting_ahead", "Getting Ahead", Difficulty.INTERMEDIATE, 1)
                .req(Skill.CRAFTING, 30).req(Skill.CONSTRUCTION, 26)
                .xp(Skill.CRAFTING, 4000).xp(Skill.CONSTRUCTION, 3200).reward("coins:3000") // wiki: 3,000 coins
                .stage("I can start this quest by speaking to Gordon on his farm south of the Farming Guild. (Requires 30 Crafting, 26 Construction.)")
                .talk("gordon_kebos").say("A beast terrorises the farms of Kebos. The farmers want to scare it off with a fake head. Help them build one.")
                .stage("I should follow the flour trail to the beast, then make a fake head (soft clay, fur) and mount it with 2 planks and nails.")
                .reach("kebos_lowlands", 12).stay().unlessFlag("ga_beast").take("pot_of_flour").flag("ga_beast")
                .say("* You pour flour on the gate by the cow pen and follow the trail to a cave in the Kebos Lowlands mine.",
                        "* The Headless Beast attacks! You defeat it, but the farmers want something to scare off the next one.")
                .missing("* The beast leaves no tracks. A pot of flour on the gate by the cow pen would show its trail.")
                .talk("gordon_kebos").needFlag("ga_beast").need("knife", "needle", "thread", "hammer")
                .take("soft_clay", "bear_fur", "plank:2", "steel_nails:6").reqSkill(Skill.CRAFTING, 30).reqSkill(Skill.CONSTRUCTION, 26)
                .flag("ga_head").say("*You carve a clay head, cover it in fur, paint it red and mount it on the wall with two planks and six nails.* The beast's kin will flee!")
                .missing("A head needs soft clay and bear fur (knife, needle and thread), then 2 planks and 6 nails to mount it, and a hammer.")
                .stage("I should report the beast is gone.")
                .talk("gordon_kebos").needFlag("ga_head").give("kourend_favour_letter").say("The farms are safe! Take this, and a letter of thanks for the council.");
        b.quest("a_kingdom_divided", "A Kingdom Divided", Difficulty.EXPERIENCED, 2)
                .reqQuest("the_depths_of_despair", "the_queen_of_thieves", "tale_of_the_righteous", "the_forsaken_tower", "the_ascent_of_arceuus")
                .req(Skill.AGILITY, 54).req(Skill.THIEVING, 52).req(Skill.WOODCUTTING, 52).req(Skill.HERBLORE, 50)
                .req(Skill.MINING, 42).req(Skill.CRAFTING, 38).req(Skill.MAGIC, 35)
                .xp(Skill.AGILITY, 10000).xp(Skill.THIEVING, 10000).xp(Skill.WOODCUTTING, 10000).xp(Skill.HERBLORE, 10000)
                .unlock("The Kourend Council's favour and the Kebos Lowlands")
                .stage("I can start this quest by speaking to Martin Holt at the entrance to Kourend Castle. (Requires the five Kourend house quests and more.)")
                .talk("martin_holt").say("The Kourend Council is being picked off one by one. Someone wants the throne. Investigate the attacks.")
                .stage("I should speak to Kaht B'alam, the lizardman shaman in the Kebos Lowlands, about the dark magic behind the attacks.")
                .reach("kebos_lowlands", 12).stay().unlessFlag("akd_shrine").flag("akd_shrine")
                .say("* In the swamps of the Kebos Lowlands you find the remains of a ritual circle, still warm. Kaht B'alam's hut is close by.")
                .talk("kaht_bbalam").needFlag("akd_shrine").missing("Hsss. Show me you have seen the ritual site in the lowlands first.").say("The Judge of Yama, a demon of the void, has been summoned. Its servant hides among your council. Face it, and the kingdom may unite.")
                .stage("I must defeat the Judge of Yama in the Kebos Lowlands.")
                .kill("judge_of_yama").flag("akd_judge").say("* The Judge of Yama is banished back to the void!")
                .stage("I should report to Councillor Andrews.")
                .talk("councillor_andrews").needFlag("akd_judge").say("The council is safe and the five houses united. Great Kourend is in your debt.");
    }
}
