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
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Map;

/**
 * Desert, Morytania, Piscatoris and Great Kourend quests: Spirits of the Elid, Contact!, Beneath
 * Cursed Sands, Swan Song, Ratcatchers, Shades of Mort'ton, Rag and Bone Man I, The Depths of
 * Despair, The Queen of Thieves, Tale of the Righteous, The Forsaken Tower, The Ascent of Arceuus,
 * Getting Ahead and A Kingdom Divided. Lamp rewards are real lamps the player rubs ({@link QuestLamps}).
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
                .xp(Skill.THIEVING, 7000).reward("keris") // and the combat lamp, two wishes of 7,000 XP (QuestLamps)
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
        // Wiki (the quest guide): shadow magic, melee up close, and after several attacks one of three specials: a shadow burst
        // around itself, a Shadow Rift that explodes unless destroyed, a scarab swarm (QuestBossScripts.ChampionOfScabaras).
        b.boss(BossDef.of("champion_of_scabaras", "Champion of Scabaras").level(379).stats(600, 160, 160, 160, 160, 1).speed(4)
                .defenceBonus(120, 160, 160, 340, 5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 24, 3, 6))
                .attack(AttackDef.of("shadow", AttackType.MAGIC, 24, 10, 6).particle("minecraft:smoke"))
                .attack(AttackDef.of("shadow_burst", AttackType.MAGIC, 40, 4, 0).special(Special.AOE, 4)
                        .telegraph(2, "A shadow spreads beneath the Champion of Scabaras!"))
                .attack(AttackDef.of("shadow_flames", AttackType.MAGIC, 8, 10, 0).special(Special.HAZARD, 4).particle("minecraft:soul_fire_flame"))
                .minion("scabaras_scarab_swarm", 1)
                .drops(Drop.always("bones")).size(1.6f, 1.4f, 1.2f).at("necropolis").arena(12, "sand"));
        b.monster(MonsterDef.of("shadow_rift", "Shadow Rift").level(1).stats(20, 1, 1, 1, 1, 1)
                .attack(AttackDef.of("inert", AttackType.MAGIC, 0, 0.5, 1)).passive().moveSpeed(0.0)
                .skin(0x1A0A2A, 0x2A1A3A, 0x0A0A1A, 0x6A3A9A).size(1.0f, 1.0f, 1.0f));
        b.monster(MonsterDef.of("scabaras_scarab_swarm", "Scarab swarm").level(5).stats(5, 1, 1, 1, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 3, 1.5, 1).special(Special.POISON, 4))
                .skin(0x3A2A1A, 0x2A1A0A, 0x1A0A0A, 0x8A6A2A).size(0.8f, 0.5f, 0.8f));
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
                .stage("I must defeat the Champion of Scabaras in the tomb beneath the Ruins of Ullek.")
                // wiki: the door to the champion stays unlocked after a death or teleport, so he waits at Ullek to be fought again
                .reach("bcs_ruins_of_ullek", 8).stay().spawn("champion_of_scabaras")
                .say("* You unlock the door upstairs in the tomb again: the Champion of Scabaras is waiting.")
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
        // mejrs/data_osrs tiles: Arnold in the colony bank, Frumscone in the Wizards' Guild basement (placed above it), the Master
        // Crafter in the Crafting Guild
        b.npcAt("arnold_lydspor", "Arnold Lydspor", 2330, 3691, 0xE0B8B0, 0x5A6A3A, 0x3A4A2A, 0x8A6A3A);
        b.npcAt("wizard_frumscone", "Wizard Frumscone", 2590, 3089, 0xF0D8C8, 0x3A3A8A, 0x2A2A5A, 0x9A9AA0);
        b.npcAt("master_crafter", "Master Crafter", 2937, 3290, 0xE0B898, 0x6A4A2A, 0x4A3A1A, 0x3A2A1A);
        b.item("iron_sheet", "Iron sheet", "bar", 0xA0A0A8, "A sheet of iron, pressed in the colony's metal press.");
        b.item("fresh_monkfish", "Fresh monkfish", "fish_raw", 0x8A7A5A, "A fresh monkfish from the colony's waters.");
        b.item("airtight_pot", "Airtight pot", "pot_item", 0xA06A3A, "A pot with a lid, sealed tight.");
        b.item("bone_seeds", "Bone seeds", "seed", 0xE8E0C8, "Malignius's seeds. Planted in the ground, they raise an army of the dead.");
        // the level-79 sea trolls that storm the colony (DPS calculator), raised outside its walls
        b.boss(BossDef.of("ss_sea_troll", "Sea troll").level(79).stats(100, 60, 60, 60, 1, 1).speed(3)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 7, 2.5, 1)).drops(Drop.always("bones"))
                .skin(0x4A7A6A, 0x3A6A5A, 0x2A4A3A, 0x6A9A8A).size(0.9f, 2.4f, 1.1f));
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
                .talk("herman_caranos").say("Sea trolls are attacking the colony! The wall is broken and we've no food. Help us!",
                        "A mage might drive them off. The Wise Old Man in Draynor, perhaps?")
                // Wiki: the Wise Old Man (for runes) comes to the colony; the sea trolls at the gate are fought off; Franklin's furnace,
                // metal press and the broken walls; Arnold's net, five fresh monkfish cooked for the colony.
                .stage("I should repair the colony wall (5 planks, 2 iron bars) and help Franklin cook for the fishermen.")
                .talk("quest_wise_old_man").stay().unlessFlag("ss_wom").take("blood_rune:5", "mist_rune:10", "lava_rune:10").flag("ss_wom")
                .say("Sea trolls at Piscatoris? I'll come, if you bring the runes I'll need: 5 blood runes, 10 mist runes and 10 lava runes. "
                        + "*He takes them.* Meet me at the colony.")
                .missing("Help a fishing colony? Bring me 5 blood runes, 10 mist runes and 10 lava runes and I'll come.")
                .reach("piscatoris", 12).stay().needFlag("ss_wom").unlessFlag("ss_gate").spawn("ss_sea_troll")
                .say("* Sea trolls swarm the colony's gate!")
                .kill("ss_sea_troll", 3).stay().needFlag("ss_wom").unlessFlag("ss_gate").flag("ss_gate")
                .say("* The sea trolls at the gate are beaten back and you get into the colony. Herman and the Wise Old Man wait in the east building.")
                .talk("franklin_caranos").stay().needFlag("ss_gate").unlessFlag("ss_franklin").flag("ss_franklin")
                .say("The walls! Light the furnace's firebox, press iron bars into sheets and patch the western wall. Then Arnold needs fish.")
                .reach("piscatoris", 12).stay().needFlag("ss_franklin").unlessFlag("ss_walls").take("logs", "iron_bar:5").need("tinderbox", "hammer")
                .reqSkill(Skill.SMITHING, 45).flag("ss_walls")
                .say("* You light the firebox, press the five iron bars into iron sheets and hammer them over the broken western walls.")
                .missing("* The furnace's firebox needs logs and a tinderbox; the metal press needs 5 iron bars; the walls need a hammer.")
                .talk("arnold_lydspor").stay().needFlag("ss_walls").unless("fresh_monkfish").unlessFlag("ss_fed").need("small_fishing_net")
                .reqSkill(Skill.FISHING, 62).give("fresh_monkfish:5")
                .say("Fish for the colony? The spot to the north-west is full of monkfish. *You net five fresh monkfish, fighting off a sea troll.*")
                .missing("You'll need a small fishing net for the monkfish. (Requires 62 Fishing.)")
                .talk("arnold_lydspor").stay().needFlag("ss_walls").unlessFlag("ss_fed").reqSkill(Skill.COOKING, 62).take("fresh_monkfish:5").flag("ss_fed")
                .say("* You cook the five monkfish in the kitchen. Arnold shares them out.", "That'll keep the fishermen going. Tell Franklin.")
                .talk("franklin_caranos").needFlag("ss_fed").say("Walls up, bellies full! The Wise Old Man and Herman want a word in the east building.",
                        "@Wise Old Man: The trolls will return with their queen. We'll need an army... of the dead. Wizard Frumscone in Yanille knows a necromancer.")
                .missing("Light the furnace, press the iron sheets and fix the walls, then see Arnold about food.")
                // Wiki: Frumscone sends the player to Malignius Mortifer; his brown apron gets them into the Crafting Guild, where the
                // Master Crafter helps make an airtight pot for the bone seeds.
                .stage("I must defeat the Sea Troll Queen.")
                .talk("wizard_frumscone").stay().unlessFlag("ss_frumscone").flag("ss_frumscone")
                .say("An army of the dead? Malignius Mortifer, south of Falador, can grow one. Tell him I sent you.")
                .talk("malignius_mortifer").stay().needFlag("ss_frumscone").unlessFlag("ss_apron").give("brown_apron").flag("ss_apron")
                .say("Bone seeds need an airtight pot. Wear this apron and the Crafting Guild will let you in to make one.")
                .talk("master_crafter").stay().needFlag("ss_apron").unless("airtight_pot", "bone_seeds").take("soft_clay:2").reqSkill(Skill.CRAFTING, 40)
                .give("airtight_pot").say("* At the Master Crafter's wheel and oven you make a pot and a lid and seal them together: an airtight pot.")
                .missing("Bring some soft clay and I'll show you the wheel and the oven. (Requires 40 Crafting.)")
                .talk("malignius_mortifer").stay().needFlag("ss_apron").unless("bone_seeds").take("airtight_pot", "bones:7").give("bone_seeds")
                .travel("piscatoris").say("Seven bones and an airtight pot. *He fills the pot with bone seeds and sends you back to the colony.*")
                .missing("I need an airtight pot and seven bones for the seeds.")
                .talk("herman_caranos").stay().need("bone_seeds").unlessFlag("ss_battle").flag("ss_battle").spawn("sea_troll_queen")
                .say("Bone seeds? Then plant them and fight! *The skeletons rise as the Sea Troll Queen storms the colony!*")
                .reach("piscatoris", 12).stay().needFlag("ss_battle").spawn("sea_troll_queen")
                .kill("sea_troll_queen").needFlag("ss_battle").say("* The Sea Troll Queen sinks beneath the waves. The colony is saved!")
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
        // Lovakengj's leader, Lady Vulcana (the id predates the rename), at the Lovakengj Assembly (wiki start tile 1483, 3748). The
        // only Lady Vulcana: the zeah module's town copy was dropped, and her lines there are her post-quest lines here.
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
        // A Kingdom Divided's Judge (wiki infobox; MonsterStatsTable): melee up close and waves of fire down the pier, max 12; only
        // melee hurts it (QuestBossScripts.JudgeOfYama).
        b.boss(BossDef.of("judge_of_yama", "Judge of Yama").level(168).stats(260, 186, 112, 84, 84, 1).speed(4)
                .defenceBonus(40, 40, 40, 600, 600)
                .attack(AttackDef.of("claws", AttackType.SLASH, 12, 3, 6))
                .attack(AttackDef.of("fire_wave", AttackType.MAGIC, 12, 12, 4).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "The Judge of Yama sends waves of fire down the pier! Find the gap.").particle("minecraft:soul_fire_flame"))
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
                .stage("I can start this quest by speaking to Lady Vulcana Lovakengj in the Lovakengj Assembly, in the south of Lovakengj. "
                        + "(Requires Client of Kourend.)")
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
                .say("The tower's power flows to our forges again? Then Kourend owes you. Take this, and ride our minecarts freely.")
                .done("lord_kandur_lovakengj", "House Lovakengj mines and smiths for all of Kourend. Our forges never cool.",
                        "The Lovakite mine and the Blast Mine feed our furnaces. Dynamite is cheap - mistakes are not.",
                        "The minecart network was built by my house. It will carry you anywhere in the kingdom for a few coins.");
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
        // A Kingdom Divided (wiki, quest-helper): the hunt for Sophia Hughes, Rose's trail, the mage Xamphur and the five houses.
        b.npcAt("commander_fullore", "Commander Fullore", 1612, 3669, 0xE0B8A0, 0x8A2A2A, 0x3A2A2A, 0xA0A0A8); // Kourend Castle
        b.npcAt("fuggy", "Fuggy", 1570, 3760, 0xC8A080, 0x6A5A3A, 0x3A3A2A, 0x8A6A3A); // The Deeper Lode, Lovakengj
        b.npcAt("cabin_boy_herbert", "Cabin Boy Herbert", 1826, 3691, 0xE0C0A0, 0x3A5A8A, 0x2A2A3A, 0x6A4A2A); // Veos's ship
        b.npcAt("jorra", "Jorra", 1496, 3557, 0xC8987A, 0x6A6A6C, 0x3A3A3F, 0xC07A30); // the Graveyard of Heroes
        b.spot("akd_hughes_house", "Councillor Hughes's house", 1680, 3684); // approx: north of the square
        b.spot("akd_settlement_ruins", "Settlement Ruins", 1550, 3880); // approx: south-west of the Wintertodt camp
        b.spot("akd_legless_faun", "The Legless Faun", 1838, 3672); // approx: Port Piscarilius's southern edge
        b.spot("akd_rose_shack", "Shack south of Mount Karuulm", 1275, 3760);
        b.spot("akd_lizard_eggs", "Lizardman egg piles", 1280, 3625); // approx: across the stone bridge west of the temple
        b.spot("akd_molch_temple", "Lizardman temple, Molch", 1300, 3670); // approx
        b.spot("akd_xerics_lookout", "Xeric's Lookout", 1591, 3528);
        b.spot("akd_shayzien_prison", "Shayzien prison", 1475, 3560); // approx: west of the Graveyard of Heroes
        b.spot("akd_barbarian_camp", "Barbarian camp, Kourend Woodland", 1575, 3475); // approx
        b.spot("akd_vinery_office", "Lord Hosidius's office, Hosidius Vinery", 1808, 3550); // approx: the vinery's southern end
        b.spot("akd_chasm_of_fire", "Chasm of Fire", 1435, 3670); // approx
        b.spot("akd_mount_karuulm", "Mount Karuulm", 1311, 3807);
        b.item("hughes_receipt", "Receipt", "note", 0xE0D8C0, "An order for a bed, delivered to The Deeper Lode.");
        b.item("roses_diary", "Rose's diary", "book", 0x8A3A5A, "The diary of Rose, the old king's daughter.");
        b.item("lizardman_egg", "Lizardman egg", "orb", 0x7A9A5A, "Kaht B'alam's last egg.");
        b.item("damp_key", "Damp key", "key", 0x5A6A5A, "Kaht B'alam's key to a door in the lizardman temple.");
        b.item("unkar_declaration", "Declaration", "note", 0xE0D0B0, "A plan to overthrow Lady Lovakengj, in Councillor Unkar's hand.");
        b.item("shayzien_journal", "Shayzien journal", "book", 0xA02A2C, "King Shayzien VII's journal.");
        b.item("dark_essence_block", "Dark essence block", "essence", 0x3A2A5A, "A block of dense essence, imbued at the Dark Altar.");
        b.item("dark_nullifier", "Dark nullifier", "orb", 0x2A1A3A, "Mori's device, to cut the cult of Yama off from Yama.");
        b.item("broken_redirector", "Broken redirector", "orb", 0x6A5A7A, "A broken magical redirector from Lord Arceuus.");
        b.item("shielding_potion", "Shielding potion", "potion", 0x6A8AC0, "The Tasakaal's potion, to strengthen the Doors of Dinh.");
        // Wiki infoboxes (A Kingdom Divided's versions).
        b.boss(BossDef.of("akd_assassin", "Assassin").level(132).stats(120, 112, 112, 92, 92, 112).speed(4)
                .defenceBonus(50, 50, 50, 20, 50)
                .attack(AttackDef.of("dragon_dagger", AttackType.STAB, 12, 2.5, 6))
                .attack(AttackDef.of("dragon_darts", AttackType.RANGED, 12, 8, 4))
                .drops(Drop.always("bones")).skin(0xD8B090, 0x2A2A2A, 0x1A1A1A, 0x2A2A2A).size(0.8f, 1.9f, 0.6f));
        b.boss(BossDef.of("barbarian_warlord", "Barbarian Warlord").level(91).stats(80, 90, 90, 50, 40, 1).speed(6)
                .defenceBonus(15, 20, 15, 0, 10)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 10, 2.5, 1))
                .drops(Drop.always("bones")).skin(0xD8A880, 0x8A5A2A, 0x5A3A1A, 0xC0A060).size(0.9f, 2.0f, 0.7f));
        b.monster(MonsterDef.of("phantom_hand", "Phantom hand").level(47).stats(12, 100, 50, 8, 8, 1).speed(4)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 6, 2.5, 1))
                .skin(0x3A2A4A, 0x2A1A3A, 0x1A0A2A, 0x8A6AC0).size(0.6f, 0.8f, 0.6f));
        // Immune to magic; every tenth attack the hands fall or two Phantom hands appear (QuestBossScripts.Xamphur).
        b.boss(BossDef.of("xamphur", "Xamphur").level(239).stats(450, 1, 1, 100, 140, 1).speed(5)
                .defenceBonus(180, 180, 180, 260, 30)
                .attack(AttackDef.of("dark_bolt", AttackType.MAGIC, 18, 10, 1))
                .attack(AttackDef.of("falling_hands", AttackType.MAGIC, 10, 12, 0).special(Special.HAZARD, 10)
                        .telegraph(2, "Hands fall from the ceiling!"))
                .attack(AttackDef.of("phantom_hands", AttackType.MAGIC, 0, 12, 0).special(Special.SUMMON, 2))
                .minion("phantom_hand", 2)
                .drops(Drop.always("bones")).skin(0xB0A8C0, 0x1A1A2A, 0x0A0A1A, 0x5A3A8A).size(1.0f, 2.4f, 1.0f));
        b.quest("a_kingdom_divided", "A Kingdom Divided", Difficulty.EXPERIENCED, 2)
                .reqQuest("the_depths_of_despair", "the_queen_of_thieves", "tale_of_the_righteous", "the_forsaken_tower", "the_ascent_of_arceuus")
                .req(Skill.AGILITY, 54).req(Skill.THIEVING, 52).req(Skill.WOODCUTTING, 52).req(Skill.HERBLORE, 50)
                .req(Skill.MINING, 42).req(Skill.CRAFTING, 38).req(Skill.MAGIC, 35)
                // Wiki and quest-helper: the book of the dead and two antique lamps of 10,000 XP in any skill of level 40 or above
                // (QuestLamps; one fewer for a player who claimed Architectural Alliance's)
                .reward("book_of_the_dead")
                .unlock("The Kourend Council's favour and the Kebos Lowlands")
                .stage("I can start this quest by speaking to Martin Holt at the entrance to Kourend Castle. (Requires the five Kourend house quests and more.)")
                .talk("martin_holt").say("The Council took the throne that should have gone to the old king's daughter, and now Councillor Sophia Hughes "
                        + "has vanished. Commander Fullore leads the search, inside the castle. Help her, and we'll learn the truth.")
                // Wiki: Fullore's orders, Hughes's house and the receipt, Tomas Lawry sent off to Mount Quidamortem, The Deeper
                // Lode, then Veos's ship, where Hughes summons a Judge of Yama.
                .stage("I should help Commander Fullore find Councillor Sophia Hughes.")
                .talk("commander_fullore").stay().unlessFlag("akd_fullore").flag("akd_fullore")
                .say("The Council has served Kourend well, whatever those protesters say. Help me find Hughes, and follow my orders.",
                        "@Tomas Lawry: The Council has given ME the authority to lead this investigation!",
                        "Search her house, north of the square.")
                .reach("akd_hughes_house", 6).stay().needFlag("akd_fullore").unlessFlag("akd_receipt").give("hughes_receipt").flag("akd_receipt")
                .say("* You search the house: roses, a map of Kourend, demonic incantations, a bloody knife, a cultist robe... and in the "
                        + "bedroom drawers, a receipt for a bed delivered to The Deeper Lode.", "* A lesser demon lurks on the top floor. You leave it be.")
                .talk("tomas_lawry").stay().needFlag("akd_receipt").unlessFlag("akd_lawry").flag("akd_lawry")
                .say("Found something, have you? Mount Quidamortem? I knew it! *He hurries off without a second thought.*")
                .talk("commander_fullore").stay().needFlag("akd_lawry").unlessFlag("akd_deeper_lode").flag("akd_deeper_lode")
                .say("A bed for The Deeper Lode? Then she went to Lovakengj. Find out who stayed there.")
                .talk("fuggy").stay().needFlag("akd_deeper_lode").unlessFlag("akd_fuggy").take("hughes_receipt").flag("akd_fuggy")
                .say("Councillors? Only one human lately, a woman who didn't look like one. Said she was catching a ship at Port Piscarilius...",
                        "* The ground shakes. ...Veos's ship, it was.")
                .missing("A bed? Show me the receipt and I'll tell you who it was for.")
                .talk("cabin_boy_herbert").needFlag("akd_fuggy").spawn("judge_of_yama")
                .say("Captain Veos wandered off in a trance right after the earthquake. The councillor? She's aboard: we sail soon!",
                        "@Sophia Hughes: The whole Council planned it, and now they betray me! *She summons a Judge of Yama... and it burns her to ash.*",
                        "@Judge of Yama: You too are judged. Defeat me, and your sins are forgiven.", "* Only melee can hurt the Judge.")
                .missing("Captain Veos isn't here. Who are you looking for?")
                // Wiki: on the pier the Judge is immune to magic and ranged, banishes you down the docks and sends waves of fire
                // (QuestBossScripts.JudgeOfYama); a portal by Veos's ship leads back into the fight.
                .stage("I must defeat the Judge of Yama on the docks of Port Piscarilius.")
                .reach("at_cabin_boy_herbert", 8).stay().spawn("judge_of_yama")
                .kill("judge_of_yama").flag("akd_judge").say("* The Judge of Yama is banished back to Yama!")
                .stage("I should find out what became of Rose, the old king's daughter, then unite the five houses of Kourend against the Council.")
                // Kourend's Last Princess (wiki): Martin, Rose's diary from the Library Historical Archive, then her notes: the
                // Forthos Ruins (ROSE on the stone piles), the Settlement Ruins (an assassin, a fire spell on the ice), the Legless
                // Faun (the statues in the panel's order) and the shack south of Mount Karuulm, where Councillor Orson confesses.
                .talk("councillor_andrews").stay().needFlag("akd_judge").unlessFlag("akd_andrews").flag("akd_andrews")
                .say("The Council thanks you. We will look into how Hughes escaped. Tell Martin Holt the matter is settled: his protesters "
                        + "can go home.", "* Commander Fullore asks you to meet her outside, south of the castle.")
                .talk("commander_fullore").stay().needFlag("akd_andrews").unlessFlag("akd_rose_task").flag("akd_rose_task")
                .say("Hughes said the Council plotted against the old king. He had a daughter, Rose. Martin Holt knows the royal family best.")
                .talk("martin_holt").stay().needFlag("akd_rose_task").unlessFlag("akd_archive").flag("akd_archive")
                .say("Rose wrote Kharedst's memoirs. Her own diary is in the Library Historical Archive: ask Archeio in the Arceuus Library.")
                .talk("quest_archeio").stay().needFlag("akd_archive").unless("roses_diary").unlessFlag("akd_note_1").give("roses_diary")
                .say("* Archeio teleports you to the Archive. You pick Istoria's pocket for a bluish key, open the purple display case and "
                        + "take Rose's diary.")
                .talk("martin_holt").stay().needFlag("akd_archive").unlessFlag("akd_note_1").take("roses_diary").flag("akd_note_1")
                .say("There's a note hidden in the cover! 'Where the old faith fell'... the Forthos Ruins. Bring an axe: I'll meet you there.")
                .missing("Rose's diary is in the Library Historical Archive. Archeio can take you there.")
                .reach("isk_forthos", 10).stay().needFlag("akd_note_1").unlessFlag("akd_vines").needTool("axe").reqSkill(Skill.WOODCUTTING, 52)
                .flag("akd_vines")
                .say("@Martin Holt: The four stone piles around the ruins each carry a letter and a number. Rose spelled her name, I'm sure of it.",
                        "* You read the stone piles, chop through the vines south of Martin and squeeze into a small room with a panel.")
                .missing("* Thick vines block the way. You'd need an axe to cut through.")
                .puzzle("isk_forthos", AKD_ROSE).stay().needFlag("akd_vines").unlessFlag("akd_note_2").flag("akd_note_2")
                .say("* The panel opens on another of Rose's notes: the Settlement Ruins, south-west of the Wintertodt camp.")
                .reach("akd_settlement_ruins", 10).stay().needFlag("akd_note_2").unlessFlag("akd_assassin_1").spawn("akd_assassin")
                .kill("akd_assassin").stay().needFlag("akd_note_2").unlessFlag("akd_assassin_1").flag("akd_assassin_1")
                .say("@Martin Holt: An assassin! The Council must want me dead... though he only ever named me.")
                .reach("akd_settlement_ruins", 10).stay().needFlag("akd_assassin_1").unlessFlag("akd_note_3")
                .take("fire_rune:4", "air_rune:3", "chaos_rune").reqSkill(Skill.MAGIC, 35).flag("akd_note_3")
                .say("* Your Fire Bolt melts the ice chunks. Inside is a cold key, and the panel on the west wall holds another note: "
                        + "The Legless Faun, in Port Piscarilius.")
                .missing("* A key is frozen in the ice. A fire spell (Fire Bolt or better: 4 fire, 3 air and a chaos rune) would melt it.")
                .reach("akd_legless_faun", 8).stay().needFlag("akd_note_3").unlessFlag("akd_faun").reqSkill(Skill.AGILITY, 54).flag("akd_faun")
                .say("* You climb the pillar to the upper floor. The panel on the north wall lists the cities of Kourend in order.")
                .puzzle("akd_legless_faun", AKD_STATUES).stay().needFlag("akd_faun").unlessFlag("akd_note_4").flag("akd_note_4")
                .say("* Click. Back upstairs, the panel holds Rose's last note. From the balcony you watch Tomas Lawry arrest Martin for "
                        + "inciting the protests.", "* The note points to a small house south of Mount Karuulm.")
                .reach("akd_rose_shack", 8).stay().needFlag("akd_note_4").unlessFlag("akd_orson").flag("akd_orson")
                .say("* A tatty note to Rose from 'L.O.'... Behind the crates, a trapdoor. Below lies a woman on a stone slab: Rose.",
                        "@Councillor Orson: We killed the king to save the kingdom. Her potions come from a mage who lives with the lizardmen.",
                        "@Commander Fullore: Then we find that mage.")
                // The Mysterious Mage (wiki): Kaht B'alam's last egg (a lizardman brute guards the piles), his damp key, then Xamphur.
                .talk("kaht_bbalam").stay().needFlag("akd_orson").unlessFlag("akd_egg_task").flag("akd_egg_task")
                .say("Yo'kal ahk, the mage? I lead you. But first, my last egg: in the piles west, across the stone bridge. Then I leave this war.")
                .reach("akd_lizard_eggs", 8).stay().needFlag("akd_egg_task").unless("lizardman_egg").unlessFlag("akd_damp_key").give("lizardman_egg")
                .say("* You search the egg piles until you find Kaht's. A lizardman brute charges, but you get away with the egg.")
                .talk("kaht_bbalam").stay().needFlag("akd_egg_task").unlessFlag("akd_damp_key").take("lizardman_egg").give("damp_key").flag("akd_damp_key")
                .say("Hsss, my egg! Take this key: the small door by the bones, in the temple corridor.")
                .missing("My egg, human. In the piles west, across the stone bridge.")
                .reach("akd_molch_temple", 8).stay().needFlag("akd_damp_key").unlessFlag("akd_xamphur").need("damp_key").spawn("xamphur")
                .kill("xamphur").stay().needFlag("akd_damp_key").unlessFlag("akd_xamphur").take("damp_key").flag("akd_xamphur")
                .say("@Xamphur: Amusing. *His bolt strikes you down... You come round as two lizardmen flee from the 'zombie'.*",
                        "* A note on the table: Xamphur was behind the troubles of The Ascent of Arceuus.")
                .reach("akd_rose_shack", 8).stay().needFlag("akd_xamphur").unlessFlag("akd_houses").flag("akd_houses")
                .say("@Commander Fullore: The potions were all that kept Rose alive. She has been gone for twenty years. *You bury her outside.*",
                        "@Commander Fullore: Only the five houses can bring down the Council. Ask their leaders to meet us at Xeric's Lookout.")
                // The Houses of Kourend (wiki): each leader is asked to Xeric's Lookout, then helped with their own doubt about the accord.
                .talk("lord_trobin_arceuus").stay().needFlag("akd_houses").unlessFlag("akd_ask_arceuus").flag("akd_ask_arceuus")
                .say("Xeric's Lookout? Very well. Arceuus will hear Fullore out.")
                .talk("lord_kandur_hosidius").stay().needFlag("akd_houses").unlessFlag("akd_ask_hosidius").flag("akd_ask_hosidius")
                .say("If the Council has rotted, Hosidius will not stand by. I will come.")
                .talk("lord_kandur_lovakengj").stay().needFlag("akd_houses").unlessFlag("akd_ask_lovakengj").flag("akd_ask_lovakengj")
                .say("Lovakengj will come, but I promise nothing.")
                .talk("lady_shauna_piscarilius").stay().needFlag("akd_houses").unlessFlag("akd_ask_piscarilius").flag("akd_ask_piscarilius")
                .say("A meeting at the lookout? The Warrens hear everything anyway. I'll be there.")
                .talk("lord_shiro_shayzien").stay().needFlag("akd_houses").unlessFlag("akd_ask_shayzien").flag("akd_ask_shayzien")
                .say("Shayzien answers when Kourend calls.")
                .reach("akd_xerics_lookout", 10).stay()
                .needFlag("akd_ask_arceuus", "akd_ask_hosidius", "akd_ask_lovakengj", "akd_ask_piscarilius", "akd_ask_shayzien")
                .unlessFlag("akd_accord").flag("akd_accord")
                .say("* The five leaders, Fullore and Orson meet at Xeric's Lookout. Orson is confronted, and Lord Hosidius sets out the Royal "
                        + "Accord of Twill. Each leader has a doubt to settle before they sign.")
                .missing("* Fullore waits at the lookout. Have you asked all five house leaders to come?")
                // Lady Lovakengj: Unkar's part in the plot, through Martin in the Shayzien prison and Jorra at the Graveyard of Heroes.
                .talk("lord_kandur_lovakengj").stay().needFlag("akd_accord").unlessFlag("akd_lova_task").flag("akd_lova_task")
                .say("I'd see Councillor Unkar replace Andrews. Prove he's no innocent, and I'll sign.")
                .talk("commander_fullore").stay().needFlag("akd_lova_task").unlessFlag("akd_lova_help").flag("akd_lova_help")
                .say("Unkar? Martin Holt might know. He's in the Shayzien prison, west of the Graveyard of Heroes.")
                .reach("akd_shayzien_prison", 8).stay().needFlag("akd_lova_help").unlessFlag("akd_passphrase").flag("akd_passphrase")
                .say("@Martin Holt: No proof, but the Royal Society meets at the Graveyard of Heroes. Tell the dwarf: 'Evil only triumphs when "
                        + "the good do nothing.'")
                .talk("jorra").stay().needFlag("akd_passphrase").unlessFlag("akd_assassin_2").spawn("akd_assassin")
                .say("Evil only triumphs... aye, you're with Martin. Unkar has been- *An assassin leaps from the graves!*")
                .kill("akd_assassin").stay().needFlag("akd_passphrase").unlessFlag("akd_assassin_2").flag("akd_assassin_2")
                .say("* The assassin falls.")
                .talk("jorra").stay().needFlag("akd_assassin_2").unless("unkar_declaration").unlessFlag("akd_lova_signed").give("unkar_declaration")
                .say("As I was saying: Unkar's own declaration. He meant to overthrow Lady Lovakengj.")
                .talk("lord_kandur_lovakengj").stay().needFlag("akd_assassin_2").unlessFlag("akd_lova_signed").take("unkar_declaration")
                .flag("akd_lova_signed").say("Unkar, against me? ...Then I will sign the accord.")
                // Lord Hosidius: the barbarians who seized Phileas Rimor.
                .talk("lord_kandur_hosidius").stay().needFlag("akd_accord").unlessFlag("akd_hosi_task").flag("akd_hosi_task")
                .say("Barbarians camp south of the lookout, and I hear Shayzien had dealings with them. Find out why.")
                .reach("akd_barbarian_camp", 8).stay().needFlag("akd_hosi_task").unlessFlag("akd_warlord").spawn("barbarian_warlord")
                .kill("barbarian_warlord").stay().needFlag("akd_hosi_task").unlessFlag("akd_warlord").flag("akd_warlord")
                .say("@Phileas Rimor: They held me for someone, and stole my package: King Shayzien VII's journal!")
                .talk("lord_kandur_hosidius").stay().needFlag("akd_warlord").unlessFlag("akd_hosi_signed").flag("akd_hosi_signed")
                .say("Shiro hides his house's ties to Xeric, then. Hosidius will sign.")
                // Lord Shayzien: King Shayzien VII's journal, hidden in Lord Hosidius's office under the vinery.
                .talk("lord_shiro_shayzien").stay().needFlag("akd_accord").unlessFlag("akd_shay_task").flag("akd_shay_task")
                .say("Hosidius has my ancestor's journal hidden in his office, under the vinery. Bring it to me.")
                .reach("akd_vinery_office", 8).stay().needFlag("akd_shay_task").unless("shayzien_journal").unlessFlag("akd_shay_signed")
                .reqSkill(Skill.THIEVING, 52).give("shayzien_journal")
                .say("* Behind the eastern wine barrel is a trapdoor. You picklock the chest below and take the Shayzien journal.")
                .talk("lord_shiro_shayzien").stay().needFlag("akd_shay_task").unlessFlag("akd_shay_signed").take("shayzien_journal")
                .flag("akd_shay_signed").say("My ancestor's journal. Shayzien will sign.")
                // Lady Piscarilius: Mori's dark nullifier for the Chasm of Fire.
                .talk("lady_shauna_piscarilius").stay().needFlag("akd_accord").unlessFlag("akd_pisc_task").flag("akd_pisc_task")
                .say("The cult of Yama worries me more than any Council. Mori of Arceuus has a plan to cut them off.")
                .reach("arceuus", 10).stay().needFlag("akd_pisc_task").unless("dark_essence_block", "dark_nullifier").unlessFlag("akd_nullifier")
                .needTool("pickaxe").need("chisel").reqSkill(Skill.MINING, 38).reqSkill(Skill.CRAFTING, 38).give("dark_essence_block")
                .say("* You mine a dense essence block, chisel it and imbue it at the Dark Altar.")
                .talk("mori").stay().needFlag("akd_pisc_task").unless("dark_nullifier").unlessFlag("akd_nullifier")
                .take("molten_glass", "dark_essence_block").give("dark_nullifier")
                .say("With these I can cut the Voice of Yama off from Yama himself. Hide this nullifier in the Chasm of Fire.")
                .missing("I need molten glass and a dark essence block for the nullifier.")
                .reach("akd_chasm_of_fire", 8).stay().needFlag("akd_pisc_task").unlessFlag("akd_nullifier").take("dark_nullifier").flag("akd_nullifier")
                .say("* A Disciple of Yama stops you at the chasm and takes the device... You wake in the forest, sure the nullifier is hidden.")
                .talk("lady_shauna_piscarilius").stay().needFlag("akd_nullifier").unlessFlag("akd_pisc_signed").flag("akd_pisc_signed")
                .say("It's done? Then I'll sign, though I still say it's a bad idea.")
                // Lord Arceuus: the Tasakaal read the broken redirector and enchant a shielding potion for the Doors of Dinh.
                .talk("lord_trobin_arceuus").stay().needFlag("akd_accord").unlessFlag("akd_arc_task").flag("akd_arc_task").give("broken_redirector")
                .say("Is your Xamphur the mage who served Xeric? Show the Tasakaal in Mount Karuulm this broken redirector.")
                .reach("akd_mount_karuulm", 8).stay().needFlag("akd_arc_task").unless("shielding_potion").unlessFlag("akd_doors")
                .take("broken_redirector", "defence_potion", "volcanic_sulphur").reqSkill(Skill.HERBLORE, 50).give("shielding_potion")
                .say("@Tasakaal: Touched by fire, and touched by darkness: one who served Xeric. We will shield the Doors of Dinh.",
                        "* You add volcanic sulphur to a defence potion and the Tasakaal enchant it into a shielding potion.")
                .missing("* The Tasakaal want the broken redirector, and a defence potion (3 doses) and volcanic sulphur for a potion.")
                .reach("fsk_wintertodt_camp", 8).stay().needFlag("akd_arc_task").unlessFlag("akd_doors").take("shielding_potion").flag("akd_doors")
                .say("* You pour the shielding potion over the Doors of Dinh.")
                .talk("lord_trobin_arceuus").stay().needFlag("akd_doors").unlessFlag("akd_arc_signed").flag("akd_arc_signed")
                .say("The same Xamphur, and a pyromancer in league with him... The Council comes first. Arceuus will sign.")
                // The Council's End (wiki): Rose's Accord, a coronation, an assassin, and Fullore's reward.
                .talk("commander_fullore").needFlag("akd_lova_signed", "akd_hosi_signed", "akd_shay_signed", "akd_pisc_signed", "akd_arc_signed")
                .say("Orson fell from the lookout. The five leaders signed Rose's Accord, and Lord Hosidius was crowned... and struck down by an "
                        + "assassin at his own banquet. His son is king now.", "We buried a king as fast as we crowned one. Take this, with Kourend's thanks.")
                .missing("Each house leader at the lookout has something to settle before they sign.");
    }

    /** A Kingdom Divided (wiki): each stone pile maps a letter to a number; the panel takes ROSE's numbers (per player). */
    static final QuestPuzzle AKD_ROSE = rosePanel();

    private static QuestPuzzle rosePanel() {
        QuestPuzzle.Builder b = QuestPuzzle.builder("akd_rose").room("panel");
        String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        int[][] codes = {{8, 6, 3, 1}, {2, 9, 4, 7}, {5, 0, 7, 2}, {3, 1, 8, 6}};
        String[] piles = {"north", "east", "south", "west"};
        for (int[] c : codes) {
            // the piles show R, O, S and E in a different place for each code
            int shift = c[0] % 4;
            String[] letters = {"R", "O", "S", "E"};
            StringBuilder shown = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                int letter = (i + shift) % 4;
                shown.append(i == 0 ? "" : ", ").append(piles[i]).append(" pile '").append(letters[letter]).append(" - ").append(c[letter]).append("'");
            }
            b.variant(null).prompt("* The stone piles read: " + shown + ". The panel wants four numbers.").options(digits).solution(c);
        }
        return b.failLines("* The panel clunks and resets.").build();
    }

    /** A Kingdom Divided (wiki): press the Legless Faun's statues in the order its upstairs panel lists the cities (per player). */
    static final QuestPuzzle AKD_STATUES = statues();

    private static QuestPuzzle statues() {
        String[] cities = {"Arceuus", "Hosidius", "Lovakengj", "Piscarilius", "Shayzien"};
        String[] options = new String[cities.length];
        for (int i = 0; i < cities.length; i++) {
            options[i] = "Press in the " + cities[i] + " statue";
        }
        QuestPuzzle.Builder b = QuestPuzzle.builder("akd_statues").room("faun");
        for (int[] order : new int[][]{{4, 2, 0, 1, 3}, {1, 3, 4, 0, 2}, {3, 0, 2, 4, 1}, {2, 4, 1, 3, 0}}) {
            StringBuilder shown = new StringBuilder();
            for (int c : order) {
                shown.append(shown.length() == 0 ? "" : ", ").append(cities[c]);
            }
            b.variant(null).prompt("* The panel upstairs lists: " + shown + ". Downstairs stand the five cities' statues.").options(options).solution(order);
        }
        return b.failLines("* Nothing happens. The statues slide back out.").build();
    }
}
