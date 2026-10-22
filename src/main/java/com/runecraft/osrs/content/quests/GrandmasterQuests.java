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

import java.util.ArrayList;
import java.util.List;

/** Grandmaster quests: Monkey Madness II, Dragon Slayer II and While Guthix Sleeps. */
final class GrandmasterQuests {
    private GrandmasterQuests() {
    }

    static void register(QuestBook b) {
        b.place("kruks_dungeon", "Kruk's dungeon", 2775, 2765, PlaceDef.Style.CAVE);
        b.place("myths_guild", "Myths' Guild", 2458, 2847, PlaceDef.Style.TOWN);
        b.place("quest_ungael", "Ungael", 2270, 4050, PlaceDef.Style.RUINS);
        b.place("galveks_sea", "The Western Sea", 2440, 2880, PlaceDef.Style.ARENA);
        // the broken table over Movario's base, in the run-down house on the Khazard side of the battlefield (quest-helper tile)
        b.spot("movarios_base", "Movario's base", 2519, 3249);
        b.place("guthix_sanctum", "Guthix's sanctum", 3220, 3150, PlaceDef.Style.CAVE);
        monkeyMadnessII(b);
        dragonSlayerII(b);
        whileGuthixSleeps(b);
    }

    private static void monkeyMadnessII(QuestBook b) {
        b.npc("lumdo", "Lumdo", "ape_atoll", -4, -4, 0xC8A071, 0x3A5A2B, 0x2A3A1B, 0x6A6A3B);
        // quest-helper tiles: Assistant Lori by the balloon south of the Grand Tree, Awowogei on his throne in Marim, Assistant Le
        // Smith on the rooftops (one of four spots in OSRS), the monkey guard by the boat on Ape Atoll's north-west coast
        b.npcAt("assistant_lori", "Assistant Lori", 2476, 3460, 0xD8A070, 0x8A6A3A, 0x5A4A2A, 0x6A4A1A);
        b.npcAt("awowogei", "Awowogei", 2803, 2765, 0xC8A071, 0xC0A030, 0x8A6A1A, 0xE0C040);
        b.npcAt("assistant_le_smith", "Assistant Le Smith", 2791, 2770, 0xD8A070, 0x6A6A3A, 0x4A4A2A, 0x3A3A2A); // approx: one of four rooftops
        b.npcAt("mm2_monkey_guard", "Monkey guard", 2694, 2784, 0xC8A071, 0x5A5A5A, 0x3A3A3A, 0x6A6A6A);
        b.spot("mm2_glough_house", "Glough's house", 2485, 3464);
        b.spot("mm2_archers_hill", "Monkey archers' hill", 2713, 2758);
        b.spot("mm2_kruk_trapdoor", "Kruk's dungeon forks", 2720, 2782); // approx: by the jungle grass trapdoor (2714, 2788), clear of its entrance
        b.spot("mm2_platform", "Airship construction platform", 2690, 2790); // approx: reached by the monkey guard's boat
        b.item("royal_seed_pod", "Royal seed pod", "seed", 0xE0C040, "Teleports you to the Grand Tree.");
        b.item("kruks_paw", "Kruk's paw", "bones", 0x5A3A26, "Proof that Kruk is dead.");
        b.item("mm2_handkerchief", "Handkerchief", "cloth", 0xE0E0D0, "A handkerchief embroidered with the letter A.");
        // Kruk (wiki, Monkey Madness II version): combat 207, 210 HP, slash up to 33 and ranged up to 37 every 4 ticks.
        b.boss(BossDef.of("kruk", "Kruk").level(207).stats(210, 130, 130, 150, 130, 170).speed(4)
                .defenceBonus(100, 100, 100, 250, 100)
                .attack(AttackDef.of("scimitars", AttackType.SLASH, 33, 2.5, 6))
                .attack(AttackDef.of("boulder", AttackType.RANGED, 37, 10, 4).particle("minecraft:block_crumble"))
                .drops(Drop.always("big_bones")).size(1.3f, 2.4f, 1.0f).at("kruks_dungeon").arena(13, "jungle"));
        b.item("mm2_mysterious_note", "Mysterious note", "note", 0xE0D8B0, "A note from Glough's house, written in invisible ink.");
        b.item("mm2_kruk_greegree", "Kruk monkey greegree", "talisman", 0x5A3A26, "Zooknock's greegree, made from Kruk's paw.");
        b.item("mm2_deconstructed_onyx", "Deconstructed onyx", "gem", 0x2A2A2A, "The charged onyx from Glough's device, chiselled apart.");
        b.spot("glough_cave", "Glough's cave", 2490, 3520); // approx: north-east of the Gnome Stronghold
        Cast.npc(b, "kob", "Kob", "troll_stronghold", 4, 4, Cast.TROLL, 0x5A4A3A);
        Cast.npcAt(b, "keef", "Keef", 2542, 3031, Cast.TROLL, 0x6A5A2A); // quest-helper: on his ledge in Gu'Tanoth
        // The troll generals (wiki): Kob level 185, 200 HP, slashes up to 57 (27 through Protect from Melee); Keef level 178, 180 HP,
        // crush up to 47 (he also hits through prayer). Both every 5 ticks; they give up when beaten.
        b.boss(BossDef.of("mm2_kob", "Kob").level(185).stats(200, 180, 175, 80, 0, 0).speed(5)
                .defenceBonus(60, 85, 90, 200, 200)
                .attack(AttackDef.of("slash", AttackType.SLASH, 57, 2.5, 4))
                .attack(AttackDef.of("crushing_blow", AttackType.SLASH, 27, 2.5, 1).special(Special.PRAYER_PIERCING, 0))
                .drops(Drop.always("big_bones")).skin(Cast.TROLL, 0x5A4A3A, 0x3A2A1A, 0x4A3A2A).size(1.0f, 2.6f, 1.2f));
        b.boss(BossDef.of("mm2_keef", "Keef").level(178).stats(180, 165, 120, 165, 1, 1).speed(5)
                .defenceBonus(50, 50, 50, 0, 0)
                .attack(AttackDef.of("club", AttackType.CRUSH, 47, 2.5, 4))
                .attack(AttackDef.of("heavy_club", AttackType.CRUSH, 24, 2.5, 1).special(Special.PRAYER_PIERCING, 0)) // approx
                .drops(Drop.always("big_bones")).skin(Cast.TROLL, 0x6A5A2A, 0x4A3A1A, 0x5A4A2A).size(1.0f, 2.6f, 1.2f));
        // Glough (Monkey Madness II final boss): OSRS Wiki infobox (melee up to 61, ranged and magic 40+, pushes and pulls).
        b.boss(BossDef.of("mm2_glough", "Glough").level(431).stats(575, 260, 270, 248, 250, 262).speed(6)
                .attack(AttackDef.of("staff_slam", AttackType.CRUSH, 61, 2.5, 6))
                .attack(AttackDef.of("demonic_blast", AttackType.MAGIC, 45, 10, 3).particle("minecraft:soul_fire_flame")) // approx: 40+
                .attack(AttackDef.of("rock_throw", AttackType.RANGED, 45, 10, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "Glough hurls a rock into the air!"))
                .attack(AttackDef.of("shove", AttackType.CRUSH, 20, 3, 1).special(Special.KNOCKBACK, 4)) // approx: his push
                .drops(Drop.always("bones")).skin(0xC09060, 0x3A3A3A, 0x2A2A2A, 0x5A1A1A).size(0.9f, 2.0f, 1.2f)
                .at("glough_cave").arena(12, "crypt"));
        b.quest("monkey_madness_ii", "Monkey Madness II", Difficulty.GRANDMASTER, 4)
                .xp(Skill.SLAYER, 80000).xp(Skill.AGILITY, 60000).xp(Skill.THIEVING, 50000).xp(Skill.HUNTER, 50000) // OSRS (current quest XP rewards)
                .reqQuest("monkey_madness_i", "the_grand_tree", "troll_stronghold")
                .reqQuest("enlightened_journey", "the_eyes_of_glouphrie", "watchtower")
                .req(Skill.SLAYER, 69).req(Skill.CRAFTING, 70).req(Skill.HUNTER, 60).req(Skill.AGILITY, 55)
                .req(Skill.THIEVING, 55).req(Skill.FIREMAKING, 60)
                .reward("royal_seed_pod")
                .unlock("Access to demonic gorillas and the Kruk's dungeon").unlock("A gnome glider on Ape Atoll and a bank in Marim")
                .unlock("Ability to wield the heavy ballista").unlock("Access to the maniacal monkey hunting area")
                .stage("I can start this quest by speaking to King Narnode Shareen. (Requires Monkey Madness I, The Grand Tree, Troll Stronghold and more; 69 Slayer, 70 Crafting.)")
                .talk("king_narnode").needFlag("rfd_awowogei")
                .missing("King Awowogei is still frozen at the Lumbridge banquet. Free him first. (Requires Recipe for Disaster: Freeing King Awowogei.)")
                .say("Glough has escaped from his prison! And there are rumours of monkeys sneaking into the Stronghold. Investigate, please.")
                // ---- Chapter I (wiki quick guide): Glough's house, Anita, the invisible ink, the translation, Assistant Lori, Auguste ----
                .stage("I should search Glough's house, have the king translate what I find and ask Auguste on Entrana about it. Then on to Ape Atoll.")
                .reach("mm2_glough_house", 4).stay().unlessFlag("mm2_anita", "mm2_note").unless("mm2_handkerchief").give("mm2_handkerchief")
                .say("* Up the trees of Glough's house you investigate a branch: a handkerchief, embroidered with an A.")
                .talk("anita").stay().unlessFlag("mm2_anita").take("mm2_handkerchief").flag("mm2_anita")
                .say("> What's wrong?", "That's Glough's! He gave it to me... He's been creeping about his house again. Up the tree, where nobody goes.")
                .missing("Glough? I haven't seen him since they locked him away.")
                .reach("mm2_glough_house", 4).stay().needFlag("mm2_anita").unless("mm2_mysterious_note").unlessFlag("mm2_note")
                .need("pestle_and_mortar").take("lemon", "grapes").give("mm2_mysterious_note")
                .say("* At the top of the tree you turn the gnome statue: the cupboard opens on a book of spyology. In the crates: a brush.",
                        "* In the fire remains: a blank note. You warm it over the candles, coat the brush in lemon and grape juice and paint it.",
                        "* Writing appears!")
                .missing("* The note in the fire remains looks blank. Juice from a lemon and some grapes (and a pestle and mortar) might reveal it.")
                .talk("king_narnode").stay().take("mm2_mysterious_note").flag("mm2_note")
                .say("*He hands you a translation book.* It's in the gnome tongue, but half of it is code. Anita knows his writing.")
                .talk("anita").stay().needFlag("mm2_note").unlessFlag("mm2_translated").flag("mm2_translated")
                .say("> Could you translate these notes?", "'Airships'... 'Ape Atoll'... 'the 10th squad'... and 'Assistant Le Smith'.")
                .talk("assistant_lori").stay().needFlag("mm2_translated").unlessFlag("mm2_lori").flag("mm2_lori")
                .say("> Who are you?", "Lori, Auguste's assistant. Le Smith? My old colleague left for... somewhere hot. Auguste would know more.")
                .talk("auguste").stay().needFlag("mm2_lori").unlessFlag("mm2_auguste").flag("mm2_auguste")
                .say("> Talk about Assistant Le Smith.", "Giant flying machines? Over Karamja? Mon dieu, I saw them myself from my balloon! They flew south-west.")
                .talk("king_narnode").needFlag("mm2_auguste")
                .say("Airships over Karamja, flying to Ape Atoll. Garkor and the 10th squad are there: find out what Glough is building.")
                .missing("Find out what Glough was up to first: his house, Anita, Auguste.")
                .hint("lumdo", "Back to Ape Atoll? Ask the king first.")
                // ---- Chapter II (wiki quick guide): Garkor, Awowogei, the monkey archers, Kruk's dungeon, Kruk ----
                .stage("I should talk to Garkor on Ape Atoll and follow the monkey archers' tracks to whoever is leading them.")
                .talk("garkor").stay().unlessFlag("mm2_garkor").flag("mm2_garkor")
                .say("> King Narnode has sent me.", "Glough has allied with someone on the island. Speak to King Awowogei, carefully.")
                .talk("awowogei").stay().needFlag("mm2_garkor").unlessFlag("mm2_awowogei").need("monkey_greegree").flag("mm2_awowogei")
                .say("> I'm here to discuss military strategies.", "Kruk trains my archers on the west hill. Ask him yourself.")
                .missing("*Awowogei ignores you. Perhaps as a monkey...*")
                .talk("garkor").stay().needFlag("mm2_awowogei").unlessFlag("mm2_archers").flag("mm2_archers")
                .say("Kruk! A monkey general, and Glough's ally. Ask the monkey archers on the west hill where he goes.")
                .reach("mm2_archers_hill", 6).stay().needFlag("mm2_archers").unlessFlag("mm2_tracks").flag("mm2_tracks")
                .say("* A monkey archer tells you Kruk walks off alone. At the foot of the hill you find his tracks and follow them",
                        "* to a trapdoor hidden in the jungle grass by a palm tree.")
                .puzzle("mm2_kruk_trapdoor", MM2_MAZE).stay().needFlag("mm2_tracks").unlessFlag("mm2_maze").flag("mm2_maze")
                .say("* The last fork feels right. You unlock the shortcut and step into Kruk's cave.")
                .kill("kruk").needFlag("mm2_maze").give("kruks_paw")
                .say("* Kruk falls with a thunderous crash! You take his paw.")
                .missing("* Kruk waits at the end of the dungeon beneath the jungle grass.")
                // ---- Chapters III and IV (wiki quick guide) ----
                .stage("I must stop Glough: a Kruk greegree from Zooknock, the troll generals Kob and Keef, the airship platform, Glough's "
                        + "laboratory and his gorillas.")
                .talk("zooknock").stay().unless("mm2_kruk_greegree").take("kruks_paw", "monkey_talisman").give("mm2_kruk_greegree")
                .say("Kruk's paw! With a talisman I can make a greegree from it. Glough won't suspect a thing.")
                .talk("awowogei").stay().unlessFlag("mm2_as_kruk").need("mm2_kruk_greegree").flag("mm2_as_kruk")
                .say("Kruk! The trolls, Kob and Keef, are ready? Good. Glough's fleet will need their numbers.")
                .missing("*Awowogei only speaks of the war to Kruk.*")
                .talk("garkor").stay().needFlag("mm2_as_kruk").unlessFlag("mm2_generals").flag("mm2_generals")
                .say("Trolls too? Then stop them: Kob in the Troll Stronghold and Keef in Gu'Tanoth.")
                .talk("kob").stay().needFlag("mm2_generals").unlessFlag("mm2_kob").reqSkill(Skill.SLAYER, 69).spawn("mm2_kob")
                .say("> I know about your deal with the monkeys.", "Kob crush you!", "> I accept your challenge.")
                .kill("mm2_kob").stay().needFlag("mm2_generals").unlessFlag("mm2_kob").flag("mm2_kob")
                .say("* Kob gives up.", "@Kob: Kob not fight for Glough no more. Kob promise.")
                .talk("keef").stay().needFlag("mm2_kob").unlessFlag("mm2_keef").spawn("mm2_keef")
                .say("> I know about your deal with the monkeys.", "> I accept your challenge.")
                .kill("mm2_keef").stay().needFlag("mm2_kob").unlessFlag("mm2_keef").flag("mm2_keef")
                .say("* Keef gives up.", "@Keef: Okay, okay! Keef's trolls stay home.")
                .talk("garkor").stay().needFlag("mm2_keef").unlessFlag("mm2_smith_brief").flag("mm2_smith_brief")
                .say("The trolls are out. Now find Assistant Le Smith; he's somewhere up on Marim's rooftops.")
                .talk("assistant_le_smith").stay().needFlag("mm2_smith_brief").unlessFlag("mm2_smith").flag("mm2_smith")
                .say("> Awowogei has already informed me about the battleships.", "Then you know the fleet is at the construction platform, off the west coast.")
                .talk("garkor").stay().needFlag("mm2_smith").unlessFlag("mm2_platform_brief").flag("mm2_platform_brief")
                .say("Sabotage that platform. The monkey guard by the boat on the west side will take Kruk anywhere.")
                .talk("mm2_monkey_guard").stay().needFlag("mm2_platform_brief").unlessFlag("mm2_platform").need("mm2_kruk_greegree")
                .flag("mm2_platform").travel("mm2_platform")
                .say("> Can I visit the platform?", "Of course, General Kruk. *He rows you across.*")
                .puzzle("mm2_platform", MM2_SABOTAGE).stay().needFlag("mm2_platform").unlessFlag("mm2_sabotage").flag("mm2_sabotage")
                .say("* Six satchels of explosives in place. You slip back to the boat before the ninja monkeys see you.")
                .talk("garkor").stay().needFlag("mm2_sabotage").unlessFlag("mm2_lab_brief").flag("mm2_lab_brief")
                .say("Now Glough's laboratory: past the monkey bars in Kruk's dungeon, where only Kruk may go.")
                .reach("kruks_dungeon", 12).stay().needFlag("mm2_lab_brief").unlessFlag("mm2_lab").need("mm2_kruk_greegree", "chisel", "hammer")
                .flag("mm2_lab")
                .say("* As Kruk you swing across the monkey bars into Glough's laboratory and ride a stunted demonic gorilla downstairs,",
                        "* fighting three gorillas back into their cages. You chisel the device's charged onyx apart and jam it with the",
                        "* deconstructed onyx: the incubation chambers go dark.")
                .missing("* Glough's laboratory lies beyond monkey bars only Kruk could use. A chisel and a hammer would open his device.")
                .talk("awowogei").stay().needFlag("mm2_lab").unlessFlag("mm2_kruk_stripped").flag("mm2_kruk_stripped")
                .say("Sabotage! Kruk, you fool, you are stripped of your rank. Duke will lead my guard!")
                .talk("garkor").stay().needFlag("mm2_kruk_stripped").unlessFlag("mm2_stronghold").flag("mm2_stronghold")
                .say("Glough's gorillas are attacking the Gnome Stronghold! Get back to King Narnode!")
                .talk("king_narnode").stay().needFlag("mm2_stronghold").unlessFlag("mm2_nieve").flag("mm2_nieve")
                .say("Tortured gorillas in my Stronghold! Nieve, the slayer master, is fighting them south of the tree. Help her!")
                .kill("tortured_gorilla", 4).stay().needFlag("mm2_nieve").unlessFlag("mm2_gorillas").flag("mm2_gorillas")
                .say("* Nieve and you drive the tortured gorillas out of the Gnome Stronghold. Garkor has found Glough's cave.") // OSRS: kill 4 tortured gorillas
                .kill("tortured_gorilla", 2).stay().needFlag("mm2_gorillas").unlessFlag("mm2_cave_tortured").flag("mm2_cave_tortured")
                .say("* In the cavern north-east of the Stronghold, two more tortured gorillas fall.")
                .kill("demonic_gorilla", 2).stay().needFlag("mm2_gorillas").unlessFlag("mm2_cave_demonic").flag("mm2_cave_demonic")
                .say("* The demonic gorillas that leapt down beside them fall too. Glough flees deeper into his cave.")
                .kill("mm2_glough").needFlag("mm2_cave_tortured", "mm2_cave_demonic")
                .say("* Through three rooms Glough fights on. At last he falls. His gorilla army will never fly.")
                .missing("?!mm2_gorillas|* Help Nieve clear the tortured gorillas from the Stronghold first.",
                        "?mm2_gorillas|* Two tortured and two demonic gorillas guard the way to Glough.")
                .stage("I should report to King Narnode.")
                .talk("king_narnode").say("* Zooknock teleports you back to the Grand Tree.",
                        "Glough is dead and Kruk with him. Take this royal seed pod; it will always bring you home to the Grand Tree.")
                .done("garkor", "The 10th squad will remember you.");
    }

    /**
     * Monkey Madness II, Kruk's dungeon (wiki quick guide): at each fork one way "feels wrong" (an obstacle fails, a web won't cut,
     * a rock won't break) and the player must take the other; the route differs from player to player. Four forks, left or
     * right; a wrong way sends the player back to the start of the maze.
     */
    static final QuestPuzzle MM2_MAZE = mm2Maze();
    /**
     * The airship platform (wiki quick guide): six satchels of explosives on the two compromised floorboards, the two
     * compromised supports and the two gas cylinders, in any order, avoiding everything else.
     */
    static final QuestPuzzle MM2_SABOTAGE = mm2Sabotage();

    private static QuestPuzzle mm2Maze() {
        String[] forks = {"the dodgy ground or the webbed tunnel", "the vine or the rocks", "the metal door or the bronze door",
                "the eastern or western path to Kruk's room"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("mm2_maze").room("maze");
        for (int route = 0; route < 16; route++) {
            int[] sol = new int[4];
            for (int f = 0; f < 4; f++) {
                sol[f] = (route >> f) & 1;
            }
            b.variant(null).prompt("* Beneath the jungle grass, Kruk's dungeon forks four times: " + String.join("; ", forks) + ".",
                            "* At each fork one way feels wrong. Choose left or right at each fork in turn.")
                    .options("Left", "Right").solution(sol);
        }
        return b.failLines("* This route feels wrong... You fall into the pit of maniacal monkeys and climb back to the start.").build();
    }

    private static QuestPuzzle mm2Sabotage() {
        return QuestPuzzle.builder("mm2_sabotage").shuffle().room("platform").variant(null)
                .prompt("* The airship platform swarms with ninja monkeys. You have six satchels filled from the explosives barrel.",
                        "* Where do you plant them? (Six places, in any order.)")
                .options("Compromised floorboards (middle floor)", "Compromised floorboards (north fork)", "Compromised support (east)",
                        "Compromised support (west fork)", "Gas cylinder (top floor, south)", "Gas cylinder (top floor, north)",
                        "Satchel crate", "Explosives barrel", "The boat")
                .anyOrder(0, 1, 2, 3, 4, 5)
                .failLines("* A ninja monkey spots you! You're thrown back to the boat; the satchels you placed stay put.").build();
    }

    private static void dragonSlayerII(QuestBook b) {
        b.npc("alec_kincade", "Alec Kincade", "myths_guild", 0, 3, 0xE0B8A0, 0x2A3A2A, 0x1A2A1A, 0x8A8A8C);
        // Dallas Jones waits in the pub on Musa Point (quest-helper tile), not in the Varrock Museum.
        b.npcAt("dallas_jones", "Dallas Jones", 2924, 3146, 0xE8C0AE, 0x8A6A3A, 0x4A3A2A, 0x6A4A1A);
        b.npc("robert_the_strong", "Robert the Strong", "myths_guild", -3, 3, 0xD0C8B8, 0xC0C0C9, 0x5A5A6A, 0xE0E0E3);
        // quest-helper tiles: Jardric at the museum camp, the Sphinx in Sophanem, Sarah in Port Phasmatys, Amelia south-west of
        // the Shayzien archery store, Torfinn on the Rellekka docks
        b.npcAt("jardric", "Jardric", 3719, 3812, 0xE0B898, 0x6A5A3A, 0x4A3A2A, 0x5A4A2A);
        b.npcAt("sphinx", "Sphinx", 3301, 2785, 0xD8B060, 0xC09840, 0xA08030, 0x3A6AA0);
        b.npcAt("sarah_phasmatys", "Sarah", 3674, 3469, 0xC8D0C8, 0x5A6A5A, 0x3A4A3A, 0x6A4A2A);
        b.npcAt("amelia", "Amelia", 1540, 3545, 0xE8C0A0, 0x8A2A2A, 0x5A1A1A, 0x6A3A1A);
        b.npcAt("torfinn", "Torfinn", 2640, 3696, 0xF0C8A8, 0x6A4A2A, 0x4A3A1A, 0xC0A060);
        b.item("dragonkin_key", "Dragonkin key", "key", 0x8A2A2A, "An ancient key of the dragonkin.");
        b.item("ancient_mural_map", "Map of Ungael", "note", 0xC0B080, "A map copied from a dragonkin mural.");
        b.item("ds2_map_piece", "Map piece", "note", 0xC8B888, "A torn piece of an old map.");
        b.item("varrock_census_records", "Varrock census records", "book", 0x6A4A2A, "The census records of Varrock.");
        b.item("ds2_ancient_key", "Ancient key", "key", 0x5A6A7A, "An ancient key from the stone chest on Ungael.");
        // Spots (quest-helper tiles): Elvarg's lair under Crandor, the House on the Hill and the map-piece searches around it, the
        // rowboat on Fossil Island's west coast, Lithkren's courtyard, Varrock's library and dining room, the Kharazi maze stairs,
        // the Kourend crypt and the Ancient Cavern's whirlpool.
        b.spot("ds2_crandor_lair", "Elvarg's lair", 2858, 3240); // OSRS 2858, 9640, below the surface tile
        b.spot("ds2_house", "House on the Hill", 3762, 3868);
        b.spot("ds2_house_fungi", "Fungi by the House on the Hill", 3755, 3873);
        b.spot("ds2_house_briar", "Hook briar by the House on the Hill", 3765, 3862);
        b.spot("ds2_house_mushtree", "Mushtree east of the House on the Hill", 3791, 3877);
        b.spot("ds2_rowboat", "Fossil Island rowboat", 3659, 3849);
        b.spot("ds2_lithkren", "Lithkren", 3555, 4004);
        b.spot("ds2_varrock_library", "Varrock Palace library", 3211, 3495);
        b.spot("ds2_varrock_dining", "Varrock Palace dining room", 3222, 3479);
        b.spot("ds2_kharazi_maze", "Kharazi maze", 2944, 2895);
        b.spot("ds2_ancient_cavern", "Ancient Cavern", 2512, 3508);
        // Wiki infoboxes: the Spawn from the strange egg (level 100), Robert the Strong in Bob's dream (level 224, ranged, melee only).
        b.boss(BossDef.of("ds2_spawn", "Spawn").level(100).stats(80, 100, 100, 60, 1, 1).speed(4)
                .defenceBonus(5, 15, 15, 0, 5)
                .attack(AttackDef.of("claws", AttackType.SLASH, 11, 2, 1))
                .drops(Drop.always("bones")).skin(0xC0B090, 0x8A7A5A, 0x6A5A3A, 0x4A3A2A).size(0.8f, 1.0f, 0.8f));
        b.boss(BossDef.of("ds2_robert_the_strong", "Robert the Strong").level(224).stats(280, 1, 1, 140, 128, 328).speed(4)
                .defenceBonus(140, 180, 60, 940, 860).immune(AttackType.RANGED, AttackType.MAGIC)
                .attack(AttackDef.of("arrows", AttackType.RANGED, 34, 10, 6))
                .attack(AttackDef.of("hide_from_this", AttackType.RANGED, 60, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Robert the Strong: See if you can hide from this!")) // wiki: 50+, knocks prayers off; approx 60
                .drops(Drop.always("bones")).skin(0xD0C8B8, 0xC0C0C9, 0x5A5A6A, 0xE0E0E3));
        // The quest Vorkath (wiki "Vorkath (Dragon Slayer II)": level 392, 460 HP; MonsterStatsTable row). It fights with the main
        // Vorkath's script (bosses module VorkathScript, quest max hits: magic 30, ranged 28, melee 32, dragonfire 72, bomb 121).
        b.boss(BossDef.of("ds2_vorkath", "Vorkath").level(392).stats(460, 560, 268, 164, 148, 268).speed(5)
                .defenceBonus(66, 126, 126, 204, 80)
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 72, 12, 6).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .attack(AttackDef.of("venomous_bolt", AttackType.RANGED, 28, 12, 4).special(Special.VENOM, 0))
                .attack(AttackDef.of("bite", AttackType.SLASH, 32, 3, 3))
                .attack(AttackDef.of("firebomb", AttackType.MAGIC, 121, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Vorkath launches a fiery bomb high into the air!").particle("minecraft:flame"))
                .drops(Drop.always("superior_dragon_bones")).size(2.4f, 2.0f, 1.0f).at("quest_ungael").arena(14, "ice"));
        // Galvek (wiki): four forms of 300 HP each (1,200), every 6 ticks; slash, ranged and magic up to 28, dragonfire up to 121
        // and his ground-targeted fireballs. Phase 1 fire (fire traps), 2 air (a stat-draining projectile), 3 water (tsunamis from
        // the ends of the ship) and 4 earth (projectiles that bind).
        b.boss(BossDef.of("galvek", "Galvek").level(608).stats(1200, 632, 268, 188, 160, 246).speed(6)
                .defenceBonus(80, 140, 140, 280, 86)
                .attack(AttackDef.of("claws", AttackType.SLASH, 28, 4, 3))
                .attack(AttackDef.of("ranged", AttackType.RANGED, 28, 14, 3))
                .attack(AttackDef.of("magic", AttackType.MAGIC, 28, 14, 3).particle("minecraft:soul_fire_flame"))
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 121, 12, 2).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .attack(AttackDef.of("fireball", AttackType.MAGIC, 121, 14, 2).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Galvek hurls a fireball into the sky!").particle("minecraft:flame"))
                .attack(AttackDef.of("fire_trap", AttackType.MAGIC, 1, 14, 1).special(Special.HAZARD, 30)
                        .telegraph(2, "Galvek scatters fire traps across the deck!"))
                .phase(75, "Galvek turns to the air: a stat-draining projectile! (Phase 2 - air)", 1.0, false,
                        AttackDef.of("drain", AttackType.MAGIC, 28, 14, 3).special(Special.DRAIN_STAT, 4).particle("minecraft:cloud"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 121, 12, 2).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"),
                        AttackDef.of("ranged", AttackType.RANGED, 28, 14, 3))
                .phase(50, "Galvek calls the sea against you: tsunamis roll down the ship! (Phase 3 - water)", 1.0, false,
                        AttackDef.of("tsunami", AttackType.MAGIC, 121, 16, 3).special(Special.SKY_STRIKE, 0).telegraph(3, "A tsunami crashes towards you!"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 121, 12, 2).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"),
                        AttackDef.of("ranged", AttackType.RANGED, 28, 14, 3))
                .phase(25, "Galvek's scales turn to stone: earthen projectiles bind! (Phase 4 - earth)", 1.0, false,
                        AttackDef.of("earth_bind", AttackType.MAGIC, 28, 14, 3).special(Special.FREEZE, 5).particle("minecraft:block_crumble"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 121, 12, 2).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"),
                        AttackDef.of("ranged", AttackType.RANGED, 28, 14, 3))
                .drops(Drop.always("superior_dragon_bones")).size(2.6f, 2.2f, 1.0f).at("galveks_sea").arena(16, "water"));
        b.item("ds2_key_karamja", "Dragon key piece (Karamja)", "key", 0x8A2A2A, "A piece of the dragon key, from the maze under the Kharazi Jungle.");
        b.item("ds2_key_morytania", "Dragon key piece (Morytania)", "key", 0x8A2A2B, "A piece of the dragon key, dug up in Mort Myre Swamp.");
        b.item("ds2_key_kourend", "Dragon key piece (Kourend)", "key", 0x8A2A2C, "A piece of the dragon key, from a tomb in Great Kourend.");
        b.item("ds2_key_ungael", "Dragon key piece (Ungael)", "key", 0x8A2A2D, "A piece of the dragon key, from Vorkath's stone chest.");
        b.item("ds2_locator_orb", "Locator orb", "orb", 0x3A8A5A, "Ava's dragonstone locator. It hurts to use.");
        b.spot("ds2_mort_myre", "Mort Myre Swamp", 3440, 3400); // approx: the buried piece is at a random swamp tile in OSRS
        // Camorra's tomb in the south room of the Shayzien Crypts (quest-helper 1504, 9939), in the carved crypts below the Graveyard
        // of Heroes (dungeons ShayzienCrypts: shifted by 3550 - 9955); the puzzle is shown only down in the crypts
        b.spot("ds2_kourend_tomb", "Camorra's tomb, Shayzien Crypts", 1504, 9939 + 3550 - 9955);
        QuestSpec q = b.quest("dragon_slayer_ii", "Dragon Slayer II", Difficulty.GRANDMASTER, 5)
                .xp(Skill.SMITHING, 80000).xp(Skill.MINING, 60000).xp(Skill.AGILITY, 50000).xp(Skill.THIEVING, 50000) // OSRS (current quest XP rewards)
                .qp(200).reqQuest("legends_quest", "animal_magnetism", "ghosts_ahoy", "bone_voyage")
                .reqQuest("dream_mentor", "a_tail_of_two_cats", "client_of_kourend")
                .reqNote("Unlocked the Ancient Cavern during Barbarian Training")
                .req(Skill.MAGIC, 75).req(Skill.SMITHING, 70).req(Skill.MINING, 68).req(Skill.CRAFTING, 62)
                .req(Skill.AGILITY, 60).req(Skill.THIEVING, 60).req(Skill.CONSTRUCTION, 50).req(Skill.HITPOINTS, 50)
                .unlock("Access to the Myths' Guild and Vorkath").unlock("Ability to smith dragon platebodies and kiteshields")
                .stage("I can start this quest by speaking to Alec Kincade outside the Myths' Guild. (Requires 200 quest points, Legends' Quest and much more.)")
                .talk("alec_kincade").say("Dragons are stirring all over Gielinor. Something is uniting them.",
                        "An archaeologist, Dallas Jones, has been asking questions. You'll find him in the pub on Musa Point.")
                // ---- Starting off (wiki quick guide) ----
                .stage("I should speak to Dallas Jones at Musa Point and follow him into Elvarg's lair under Crandor.")
                .talk("dallas_jones").stay().unlessFlag("ds2_dallas").flag("ds2_dallas")
                .say("You slew Elvarg? Then you know Crandor. There's something in her lair I need to see. Meet me there, and bring a pickaxe.")
                .reach("ds2_crandor_lair", 10).underground().stay().needFlag("ds2_dallas").unlessFlag("ds2_mural").needTool("pickaxe")
                .flag("ds2_mural").spawn("ds2_spawn")
                .say("* You mine through the blockage in the north of Elvarg's lair. Beyond lies an ancient mural.",
                        "* As you investigate it, a strange egg cracks open and a spawn leaps at you!")
                .missing("* A blockage in the north wall of the lair. You'd need a pickaxe.")
                .kill("ds2_spawn").stay().needFlag("ds2_mural").unlessFlag("ds2_spawned").flag("ds2_spawned")
                .say("* The spawn dies. You investigate the mural again: dragonkin, and an island.")
                .reach("ds2_crandor_lair", 10).underground().stay().needFlag("ds2_mural").unlessFlag("ds2_spawned").spawn("ds2_spawn")
                .say("* The spawn is still loose in the tunnel!")
                .talk("dallas_jones").needFlag("ds2_spawned")
                .say("That mural matches a journal I found: it mentions a house on a hill on Fossil Island... and a dragon named Galvek.")
                .missing("Meet me in Elvarg's lair under Crandor. And bring a pickaxe.")
                // ---- The House on the Hill, To Lithkren!, Bob the Cat, Robert the Strong (wiki quick guide) ----
                .stage("I should investigate the House on the Hill on Fossil Island and sail to Lithkren with Dallas Jones, then find Bob "
                        + "the Cat and face Robert the Strong in a dream.")
                .talk("dallas_jones").stay().unlessFlag("ds2_house_brief").flag("ds2_house_brief")
                .say("> Ask about the island.", "In the basement of the House on the Hill: a map, torn into pieces. 24 of them are missing.")
                .reach("ds2_house", 4).stay().needFlag("ds2_house_brief").unlessFlag("ds2_pieces_house", "ds2_map").give("ds2_map_piece:8")
                .flag("ds2_pieces_house")
                .say("* The open chest north of the map holds 5 pieces; the stone chest upstairs another 3.")
                .reach("ds2_house_fungi", 3).stay().needFlag("ds2_house_brief").unlessFlag("ds2_pieces_fungi", "ds2_map").give("ds2_map_piece:4")
                .flag("ds2_pieces_fungi").say("* Among the fungi north of the stairs: 4 map pieces.")
                .reach("ds2_house_briar", 3).stay().needFlag("ds2_house_brief").unlessFlag("ds2_pieces_briar", "ds2_map").give("ds2_map_piece:7")
                .flag("ds2_pieces_briar").say("* In the hook briar south of the house: 7 map pieces.")
                .reach("ds2_house_mushtree", 3).stay().needFlag("ds2_house_brief").unlessFlag("ds2_pieces_tree", "ds2_map").give("ds2_map_piece:5")
                .flag("ds2_pieces_tree").say("* In the mushtree east of the house: 5 map pieces.")
                .talk("dallas_jones").stay().needFlag("ds2_house_brief").unlessFlag("ds2_map").take("ds2_map_piece:24").flag("ds2_map")
                .say("* Together you fit the 24 pieces into the map, rotating and swapping them until the coastline is whole.",
                        "An island to the north-west... Lithkren! Jardric at the museum camp knows those waters.")
                .missing("We need all 24 pieces: the chests in the house, the fungi, the hook briar and the mushtree outside.")
                .talk("jardric").stay().needFlag("ds2_map").unlessFlag("ds2_jardric").flag("ds2_jardric")
                .say("Lithkren? You'll not get there without a boat. There's an old rowboat on the west coast; it needs mending.")
                .reach("ds2_rowboat", 4).stay().needFlag("ds2_jardric").unlessFlag("ds2_boat").take("oak_plank:8", "swamp_paste:10", "iron_nails:12")
                .need("hammer").flag("ds2_boat")
                .say("* With 8 oak planks, 10 swamp paste and a dozen nails you build a rowboat on the west coast.")
                .missing("* The rowboat needs 8 oak planks, 10 swamp paste, 12 nails and a hammer.")
                .talk("dallas_jones").stay().needFlag("ds2_boat").unlessFlag("ds2_lithkren").flag("ds2_lithkren").travel("ds2_lithkren")
                .say("> I'm ready.", "* Dallas rows you across to Lithkren.")
                .reach("ds2_lithkren", 8).stay().needFlag("ds2_lithkren").unlessFlag("ds2_diary").flag("ds2_diary")
                .say("* Up the courtyard stairs, down the trapdoor and two staircases into the dragonkin laboratory.",
                        "* A skeleton in the far north: Aivas. You read his diary.")
                .talk("dallas_jones").stay().needFlag("ds2_diary").unlessFlag("ds2_aivas").give("ancient_mural_map").flag("ds2_aivas")
                .say("Aivas fought beside Robert the Strong! The vault here takes a dragon key, split in four long ago.",
                        "Robert's memories live on... in a cat called Bob. Find him. I copied the mural's map of Ungael for you.")
                .talk("bob_the_cat").stay().needFlag("ds2_aivas").unlessFlag("ds2_bob").flag("ds2_bob")
                .say("Meow. *Bob's memories of Robert the Strong are locked away. The Sphinx in Sophanem could unlock them.*")
                .talk("sphinx").stay().needFlag("ds2_bob").unlessFlag("ds2_sphinx").flag("ds2_sphinx")
                .say("> Can you help with Bob's memories?", "I will enchant you to speak with cats. The memories themselves lie in dreams: "
                        + "the Oneiromancer on Lunar Isle can take you there.")
                .talk("oneiromancer").stay().needFlag("ds2_sphinx").unlessFlag("ds2_dream").take("vial_of_water", "goutweed", "astral_rune")
                .need("seal_of_passage", "hammer", "pestle_and_mortar", "tinderbox").flag("ds2_dream")
                .say("Bob's memories? A dream potion: crush an astral rune, grind the shards and add them and goutweed to a dream vial of water.",
                        "* You light the ceremonial brazier and pour the dream potion onto the flame. Bob pads up beside you.")
                .missing("A dream potion needs a vial of water, goutweed and an astral rune (a hammer and a pestle and mortar to grind it), "
                        + "and a tinderbox for the brazier.")
                .talk("bob_the_cat").stay().needFlag("ds2_dream").unlessFlag("ds2_robert").spawn("ds2_robert_the_strong")
                .say("* In the dream, Bob and Not Bob argue. Beyond the barrier waits Robert the Strong himself!",
                        "@Robert the Strong: Prove yourself worthy of my memories!")
                .kill("ds2_robert_the_strong").stay().needFlag("ds2_dream").unlessFlag("ds2_robert").flag("ds2_robert")
                .say("* Robert the Strong lowers his bow. 'You are worthy. Galvek must not rise again.'")
                .talk("bob_the_cat").needFlag("ds2_robert")
                .say("Meow! *Bob remembers: the dragon key was split into four pieces, hidden in Karamja, Morytania, Kourend and Ungael.*")
                .missing("?!ds2_aivas|Meow? *Bob licks his paw.*", "?ds2_aivas|?!ds2_dream|Meow. *Bob's memories wait in a dream.*")
                // ---- The dragon key (wiki quick guide) ----
                .stage("I need the four pieces of the dragon key: the Kharazi maze, Mort Myre Swamp, a Kourend crypt, and Ungael beyond Rellekka.");
        // OSRS takes the four pieces in any order (quest-helper: one quest state, 125-129, for all four), so the three mainland
        // pieces stay open at the Vorkath and reforging stages too, until the key is forged: sailing to Ungael first can't
        // strand them.
        ds2MainlandPieces(q)
                .talk("brundt").stay().need("ancient_mural_map").unlessFlag("ds2_brundt").flag("ds2_brundt")
                .say("> Ask about the dragonkin fortress.", "The frozen island of Ungael? Torfinn on the docks will sail you. Mind the dragon.")
                .talk("torfinn").needFlag("ds2_brundt").need("ancient_mural_map").travel("quest_ungael")
                .say("Ungael? Aye. Hold on to something. *Your longship lands on Ungael.*")
                .missing("Brundt hasn't said anything about a voyage to me.")
                .stage("I must defeat the undead dragon Vorkath on Ungael.")
                .kill("ds2_vorkath").take("ancient_mural_map").give("ds2_key_ungael", "ds2_ancient_key")
                .say("* Vorkath collapses. In the cave to the north-west you pull the lever and run for the east room before the timer ends:",
                        "* the stone chest holds the last dragon key piece and an ancient key.");
        ds2MainlandPieces(q)
                // ---- Reforging the key and A World United (wiki quick guide) ----
                .stage("I should reforge the dragon key in the Ancient Cavern, open the dragonkin vault on Lithkren and unite the kingdoms "
                        + "against the dragons.")
                // saves from before round 37 reached this stage with the Ungael piece but no ancient key (Vorkath's chest gave only
                // the piece then): the key turns up here instead of stranding them before the mithril door
                .reach("ds2_ancient_cavern", 8).stay().unless("ds2_ancient_key").unlessFlag("ds2_heads", "ds2_forged").need("ds2_key_ungael")
                .give("ds2_ancient_key")
                .say("* At the bottom of your pack, under the Ungael key piece, you find the ancient key from Vorkath's stone chest.")
                .reach("ds2_ancient_cavern", 8).stay().unlessFlag("ds2_heads", "ds2_forged").need("ds2_ancient_key")
                .take("fire_rune:21", "air_rune:15", "blood_rune:3").reqSkill(Skill.MAGIC, 75).flag("ds2_heads")
                .say("* Past the mithril dragons, the ancient key opens the mithril door. You cast Fire Wave on each of the three dragon heads.")
                .missing("* The mithril door takes the ancient key, and the three dragon heads three casts of Fire Wave (21 fire, 15 air "
                        + "and 3 blood runes).")
                .reach("ds2_ancient_cavern", 8).stay().needFlag("ds2_heads").unlessFlag("ds2_forged")
                .take("ds2_key_karamja", "ds2_key_morytania", "ds2_key_kourend", "ds2_key_ungael").need("hammer").reqSkill(Skill.SMITHING, 70)
                .flag("ds2_forged").give("dragonkin_key")
                .say("* Up the rough-hewn steps in the south-west of the caverns, you forge the four pieces into the dragon key on an anvil.")
                .missing("* The anvils wait up the rough-hewn steps. You'll need all four key pieces, a hammer and 70 Smithing.");
        ds2MainlandPieces(q)
                .reach("ds2_lithkren", 8).stay().needFlag("ds2_forged").unlessFlag("ds2_vault").take("dragonkin_key").flag("ds2_vault")
                .say("* The dragon key turns in Lithkren's ancient doors. Inside, Dallas gasps: the vault is empty, and far away Galvek wakes.",
                        "@Bob the Cat: Meow. *The kingdoms must stand together. Start with King Roald.*")
                .talk("king_roald").stay().needFlag("ds2_vault").unlessFlag("ds2_roald").flag("ds2_roald")
                .say("> Talk about the dragon threat.", "Dragons, united? Varrock will stand. Gather the others and we'll meet in my dining room.")
                .talk("sir_amik_varze").stay().needFlag("ds2_roald").unlessFlag("ds2_amik").flag("ds2_amik")
                .say("> Talk about the dragon threat.", "The White Knights will sail with you.")
                .talk("king_lathas").stay().needFlag("ds2_roald").unlessFlag("ds2_lathas").flag("ds2_lathas")
                .say("> Talk about the dragon threat.", "Ardougne's fleet is yours.")
                .talk("brundt").stay().needFlag("ds2_roald").unlessFlag("ds2_brundt_army").flag("ds2_brundt_army")
                .say("> Talk about the dragon threat.", "The Fremennik fear no dragon. Our longships will be there.")
                .reach("ds2_varrock_dining", 4).stay().needFlag("ds2_roald", "ds2_amik", "ds2_lathas", "ds2_brundt_army").unlessFlag("ds2_council")
                .flag("ds2_council")
                .say("* Around King Roald's table the rulers agree on a plan: one fleet, sailing for Ungael.", "> It's a good plan.")
                .missing("* The rulers are not all here yet: Sir Amik Varze, King Lathas and Brundt must hear of the threat.")
                .talk("bob_the_cat").needFlag("ds2_council")
                .say("Meow. *Bob pads towards the docks. The fleet sails from Rellekka.*")
                .missing("?!ds2_forged|Meow. *Bob looks at your empty hands. The key isn't forged yet.*",
                        "?ds2_forged|?!ds2_council|Meow. *The rulers of Gielinor must meet first.*")
                // ---- The Assault and the final battle (wiki quick guide) ----
                .stage("I must sail with the allied fleet from Rellekka, survive the dragons' assault and defeat Galvek.")
                .talk("torfinn").stay().unlessFlag("ds2_afloat", "ds2_assault").flag("ds2_afloat").travel("galveks_sea")
                .say("To Ungael, and the fleet! Keep her afloat, friend.",
                        "* For four minutes you fill leaks, douse fires, heal injured Fremennik warriors and repair the masts.")
                .kill("red_dragon").stay().needFlag("ds2_afloat").unlessFlag("ds2_wave_red_way", "ds2_assault").flag("ds2_wave_red_way")
                .say("* A red dragon falls on the way to Varrock's grand ship.")
                .kill("iron_dragon").stay().needFlag("ds2_wave_red_way").unlessFlag("ds2_wave_iron_way", "ds2_assault").flag("ds2_wave_iron_way")
                .say("* An iron dragon falls.")
                .kill("brutal_green_dragon").stay().needFlag("ds2_wave_iron_way").unlessFlag("ds2_wave_way", "ds2_assault").flag("ds2_wave_way")
                .say("* A brutal green dragon falls. Varrock's grand ship is ahead.")
                // Wave 1: two green and two blue dragons battling the Varrockian soldiers (checkpoint).
                .kill("green_dragon", 2).stay().needFlag("ds2_wave_way").unlessFlag("ds2_wave_green", "ds2_assault").flag("ds2_wave_green")
                .say("* The two green dragons fall beside the Varrockian soldiers.")
                .kill("blue_dragon", 2).stay().needFlag("ds2_wave_way").unlessFlag("ds2_wave_blue", "ds2_assault").flag("ds2_wave_blue")
                .say("* The two blue dragons fall. The first wave is beaten.")
                // Wave 2: black, steel, then brutal red dragons land on the ship one at a time.
                .kill("black_dragon").stay().needFlag("ds2_wave_green", "ds2_wave_blue").unlessFlag("ds2_wave_black", "ds2_assault").flag("ds2_wave_black")
                .say("* A black dragon lands on the deck and falls. A steel dragon follows.")
                .kill("steel_dragon").stay().needFlag("ds2_wave_black").unlessFlag("ds2_wave_steel", "ds2_assault").flag("ds2_wave_steel")
                .say("* The steel dragon falls. A brutal red dragon swoops in.")
                .kill("brutal_red_dragon").stay().needFlag("ds2_wave_steel").unlessFlag("ds2_wave_red", "ds2_assault").flag("ds2_wave_red")
                .say("* The brutal red dragon falls. The second wave is beaten (checkpoint).")
                // Wave 3: mithril, adamant, then rune dragons.
                .kill("mithril_dragon").stay().needFlag("ds2_wave_red").unlessFlag("ds2_wave_mithril", "ds2_assault").flag("ds2_wave_mithril")
                .say("* The mithril dragon falls. An adamant dragon follows.")
                .kill("adamant_dragon").stay().needFlag("ds2_wave_mithril").unlessFlag("ds2_wave_adamant", "ds2_assault").flag("ds2_wave_adamant")
                .say("* The adamant dragon falls. A rune dragon crackles with lightning.")
                .kill("rune_dragon").stay().needFlag("ds2_wave_adamant").unlessFlag("ds2_assault").flag("ds2_assault")
                .say("* The rune dragon falls: the assault is broken. Galvek himself descends on the ship.")
                .kill("galvek").needFlag("ds2_assault").say("* Galvek falls into the sea. The dragon threat is ended.")
                .missing("* The dragons still hold the skies over the fleet.")
                .stage("I should report to Alec Kincade at the Myths' Guild.")
                .talk("alec_kincade").say("Galvek is dead! You are a true slayer of dragons. The Myths' Guild is open to you.")
                .done("robert_the_strong", "It was an honour to see you fight. Galvek will not rise again.");
    }

    /**
     * Dragon Slayer II, the three mainland dragon key pieces (wiki quick guide): the Kharazi maze; Reldo, the Varrock census,
     * Sarah in Port Phasmatys, Ava's locator orb and Mort Myre Swamp; Amelia and the Kourend crypt. Added to every stage from
     * the key hunt to the forge (the pieces come in any order), each switched off once its piece is held or the key forged.
     */
    private static QuestSpec ds2MainlandPieces(QuestSpec q) {
        return q
                .reach("ds2_kharazi_maze", 6).stay().unless("ds2_key_karamja").unlessFlag("ds2_forged").reqSkill(Skill.THIEVING, 60)
                .reqSkill(Skill.AGILITY, 60).need("machete").needTool("axe").give("ds2_key_karamja")
                .say("* Down the staircase on the Kharazi coast, past spikes and coloured golems (red melee, green ranged, blue magic),",
                        "* you take the dragon key piece from the plinth at the centre of the maze.")
                .missing("* The Kharazi Jungle is overgrown: you'd need a machete and an axe to reach the maze.")
                .talk("reldo").stay().unless("ds2_key_morytania").unlessFlag("ds2_reldo", "ds2_forged").flag("ds2_reldo")
                .say("> Ask about Tristan.", "Tristan, Robert's companion? The census records on the shelves might show his descendants.")
                .reach("ds2_varrock_library", 3).stay().needFlag("ds2_reldo").unlessFlag("ds2_census", "ds2_forged")
                .unless("varrock_census_records", "ds2_key_morytania")
                .give("varrock_census_records").say("* In the second bookcase from the north you find the Varrock census records.")
                .talk("reldo").stay().needFlag("ds2_reldo").unlessFlag("ds2_census", "ds2_forged").unless("ds2_key_morytania")
                .take("varrock_census_records").flag("ds2_census")
                .say("Tristan's line... Sarah, in Port Phasmatys. She may know where his key piece was hidden.")
                .talk("sarah_phasmatys").stay().needFlag("ds2_census").unlessFlag("ds2_sarah", "ds2_forged").unless("ds2_key_morytania")
                .need("ghostspeak_amulet").flag("ds2_sarah")
                .say("My ancestor's key piece? Buried somewhere in Mort Myre Swamp, they say. Ava in Draynor Manor could make a locator.")
                .missing("* Sarah's words are lost to you without a ghostspeak amulet.")
                .talk("ava").stay().needFlag("ds2_sarah").unless("ds2_locator_orb", "ds2_key_morytania").unlessFlag("ds2_forged")
                .take("dragonstone", "molten_glass:2").need("glassblowing_pipe", "chisel").give("ds2_locator_orb")
                .say("* You blow an inert orb around the dragonstone. Ava charges it into a locator orb.")
                .missing("A locator orb? Bring a dragonstone, 2 molten glass, a glassblowing pipe and a chisel.")
                .reach("ds2_mort_myre", 16).stay().need("ds2_locator_orb", "spade").unless("ds2_key_morytania").unlessFlag("ds2_forged").give("ds2_key_morytania")
                .say("* The locator stings you each time you feel it, but it leads you to a patch of swamp. You dig up a key piece.")
                .talk("amelia").stay().unless("ds2_key_kourend").unlessFlag("ds2_amelia", "ds2_forged").flag("ds2_amelia")
                .say("Camorra's tomb? In the crypt west of here. Take a light, and beware the skeletons.")
                .puzzle("ds2_kourend_tomb", DS2_CRYPT).underground().stay().needFlag("ds2_amelia").unless("ds2_key_kourend").unlessFlag("ds2_forged")
                .give("ds2_key_kourend")
                .say("* The last bust settles on its plinth. The tomb grinds open: a dragon key piece.");
    }

    /**
     * Dragon Slayer II, the Kourend crypt (wiki quick guide, quest-helper's CryptPuzzle): the plaque names where one hero sat,
     * whom another sat opposite and what a third asked; the four heroes' busts go on the north, south, west and east plinths.
     * Aivas: crossbow, Zartharim; Camorra: axe, Saranthium; Robert: bow, Arkney; Tristan: sword, Karville. The plaque is
     * different for every player.
     */
    static final QuestPuzzle DS2_CRYPT = ds2Crypt();

    private static QuestPuzzle ds2Crypt() {
        String[] heroes = {"Aivas", "Camorra", "Robert", "Tristan"};
        String[] weapons = {"crossbow", "axe", "bow", "sword"};
        String[] homes = {"Zartharim", "Saranthium", "Arkney", "Karville"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("ds2_crypt").room("busts");
        for (int n = 0; n < 4; n++) {
            for (int s = 0; s < 4; s++) {
                for (int w = 0; w < 4; w++) {
                    if (n == s || s == w || n == w) {
                        continue;
                    }
                    int e = 6 - n - s - w;
                    b.variant(null).prompt("* The tomb's plaque reads: 'The one from " + homes[n] + " sat at the north of the table, opposite "
                                            + "the one with the " + weapons[s] + ". The one with " + (w == 1 ? "an " : "a ") + weapons[w]
                                            + " asked the others to stand fast.'",
                                    "* Place the four busts on the plinths: north, south, west, then east.")
                            .options(heroes).solution(n, s, w, e);
                }
            }
        }
        return b.failLines("* The busts slide back off their plinths. Read the plaque again.").build();
    }
    private static void whileGuthixSleeps(QuestBook b) {
        // Ivy Sophista and Thaerisk Cemphier in the house with the ladder in Taverley (Thaerisk upstairs in OSRS), Akrisae and Idria in
        // the White Knights' Castle, the Khazard launderer by the Fight Arena, a shady stranger by Draynor bank, the Hunting expert in
        // the Feldip Hills and Silif in the Black Knights' catacombs (quest-helper tiles; Silif stands at the fortress door).
        b.npcAt("ivy_sophista", "Ivy Sophista", 2907, 3450, 0xF0D0BC, 0x3A6A9A, 0x2A4A6A, 0xE0A041);
        b.npcAt("thaerisk", "Thaerisk Cemphier", 2909, 3449, 0xE8C8B0, 0xE0E0F1, 0x3A3A6A, 0xC0A062);
        b.npcAt("akrisae", "Akrisae", 2989, 3342, 0xE8C0AF, 0xE0E0F0, 0x6A6AA0, 0xC0A061);
        b.npcAt("idria", "Idria", 2989, 3339, 0xF0D0B0, 0x5A3A6A, 0x3A2A4A, 0x2A1A10);
        b.npcAt("khazard_launderer", "Khazard Launderer", 2564, 3172, 0xD8B090, 0xE0E0D8, 0x6A5A4A, 0x8A6A4A);
        b.npcAt("shady_stranger", "Shady stranger", 3085, 3243, 0xC8A080, 0x2A2A30, 0x1A1A20, 0x101014);
        b.npcAt("silif", "Silif", 3017, 3512, 0xE0B8A0, 0x8A8A90, 0x4A4A50, 0xC0A063);
        b.npcAt("hunting_expert", "Hunting expert", 2525, 2916, 0xC89870, 0x5A6A2A, 0x4A3A1A, 0x6A5A2A);
        // Movario waits by the Chasm of Tears (quest-helper 3229, 9528), beyond the Tears of Guthix: in the carved Lumbridge Swamp
        // Caves he stands at the east end of the Tears' pool room (dungeons DungeonsMisthalin.lumbridgeSwampCaves), where the
        // passage on to the chasm and the Ancient Guthixian Temple begins (NpcStoreys: feet at y 23)
        b.npcAt("movario", "Movario", 3199, 3136, 0xD0A880, 0x2A2A3A, 0x1A1A2A, 0x6A1A3A);
        b.spot("wgs_pit_trap", "Feldip Hills pit trap", 2499, 2910); // west of the Hunting expert
        b.spot("wgs_bettys_counter", "Betty's counter", 3013, 3258);
        b.spot("wgs_snapdragon_patch", "White Knights' Castle snapdragon patch", 2962, 3338); // top of the castle
        b.spot("wgs_mcgrubors", "North of McGrubor's Wood", 2657, 3501); // Idria held by Lucien's mercenaries
        b.spot("wgs_wk_cell", "White Knights' Castle cell", 2989, 3344); // the cell north of Thaerisk
        b.spot("wgs_warriors_guild", "Warriors' Guild entrance", 2877, 3546);
        b.spot("wgs_lucien_camp", "Lucien's camp", 2942, 3822); // the ice wall by the Chaos Temple's roof
        // The light creatures over the Chasm of Tears (quest-helper 3228, 9525) and the Ancient Guthixian Temple beyond it (an
        // instance in OSRS): the passage past the Tears' pool and the temple cavern of the carved Lumbridge Swamp Caves, both
        // reached and solved only underground (a spot counts from its chunk's centre: the passage's west end and the temple
        // cavern's middle). // approx: the carved caves' own layout, not the OSRS tiles
        b.spot("wgs_light_creature", "Chasm of Tears", 3201, 3136);
        b.spot("wgs_temple", "Ancient Guthixian Temple", 3218, 3134);
        b.item("movarios_notes", "Movario's notes", "book", 0x3A2A4A, "Notes revealing the location of Guthix's resting place.");
        b.item("dirty_shirt", "Dirty shirt", "cloth", 0xB0A890, "One of Movario's shirts. It smells.");
        b.item("unconscious_broav", "Unconscious broav", "creature", 0x6A4A2A, "A broav, out cold. The Hunting expert can train it.");
        b.item("broav", "Broav", "creature", 0x7A5A3A, "A broav trained to follow a scent.");
        b.item("wgs_teleorb", "Teleorb", "orb", 0x8AD0E0, "Akrisae's teleorb: it swaps its bearer into the White Knights' cells.");
        b.item("pink_dye", "Pink dye", "vial", 0xE07AB0, "A little bottle of pink dye.");
        b.item("rose_tinted_lens", "Rose-tinted lens", "glass", 0xF0A0C0, "A lantern lens with a rosy tint.");
        b.item("enriched_snapdragon_seed", "Enriched snapdragon seed", "seed", 0xE06A9A, "A snapdragon seed bathed in rose-tinted light.");
        b.item("enriched_snapdragon", "Enriched snapdragon", "herb", 0xD04A8A, "A snapdragon grown from an enriched seed.");
        b.item("super_truth_serum", "Super truth serum", "vial", 0xB04AD0, "Thaerisk's truth serum, strengthened with an enriched snapdragon.");
        b.item("suspect_sketch", "Suspect sketch", "note", 0xE0D8C0, "A charcoal sketch of Lucien's spymaster.");
        b.item("wgs_cell_key", "Cell key", "key", 0x6A6A70, "The key to Silif's cell.");
        b.item("strange_teleorb", "Strange teleorb", "orb", 0x6A3A8A, "The Dark Squall's teleorb, tuned to Lucien's camp.");
        b.item("sapphire_lantern", "Sapphire lantern", "orb", 0x3A6AE0, "A lit lantern with a sapphire lens. Light creatures are drawn to it.");
        // Elite black armour (40 Defence) and the Dark Squall's robes, kept as the quest's item rewards: equipment.json bonuses.
        b.items(s -> {
            s.add("elite_black_full_helm", "Elite black full helm", Kind.ARMOUR, "full_helm", 0x1A1A1E).value(1000).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, -6, -3, 13, 14, 11, -1, 13, 0, 0, 0, 4), 40))
                    .examine("A full helmet of the elite black knights.");
            s.add("elite_black_platebody", "Elite black platebody", Kind.ARMOUR, "platebody", 0x1A1A1E).value(2000).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, -30, -15, 46, 44, 38, -6, 44, 0, 0, 0, 8), 40))
                    .examine("A platebody of the elite black knights.");
            s.add("elite_black_platelegs", "Elite black platelegs", Kind.ARMOUR, "platelegs", 0x1A1A1E).value(1500).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, -21, -11, 24, 22, 20, -4, 22, 0, 0, 0, 6), 40))
                    .examine("Platelegs of the elite black knights.");
            s.add("dark_squall_hood", "Dark squall hood", Kind.ARMOUR, "hood", 0x2A1A3A).value(1000).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), 1))
                    .examine("The hood of the Dark Squall's robes.");
            s.add("dark_squall_robe_top", "Dark squall robe top", Kind.ARMOUR, "robe_top", 0x2A1A3A).value(1000).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0), 1))
                    .examine("The robe top of the Dark Squall.");
            s.add("dark_squall_robe_bottom", "Dark squall robe bottom", Kind.ARMOUR, "robe_bottom", 0x2A1A3A).value(1000).stack(1)
                    .equipment(EquipmentDef.armour(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), 1))
                    .examine("The robe bottom of the Dark Squall.");
        });
        // Wiki: level 167, 275 HP, ranged up to 20 every 5 ticks; every 12 seconds one yells "Now!" and looses a glowing arrow
        // (21-40) while the other teleports in with a dragon dagger special (up to 72).
        b.boss(BossDef.of("wgs_assassin", "Assassin").level(167).stats(275, 50, 70, 70, 30, 110).speed(5)
                .defenceBonus(10, 10, 15, 30, 20)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 20, 8, 6))
                .attack(AttackDef.of("glowing_arrow", AttackType.RANGED, 40, 8, 1).telegraph(2, "Assassin: Now!"))
                .attack(AttackDef.of("dagger_special", AttackType.STAB, 72, 10, 1).special(Special.BLINK, 0)
                        .telegraph(2, "Assassin: I'm on it!"))
                .drops(Drop.always("bones")).skin(0xC8A080, 0x2A2A2A, 0x1A1A1A, 0x3A1A1A));
        // Wiki: Lucien's mercenaries north of McGrubor's Wood: two axemen (level 131, 110 HP, crush up to 22) and a mage (level
        // 112, 150 HP, magic up to 5), all every 5 ticks.
        b.boss(BossDef.of("wgs_mercenary_axeman", "Mercenary axeman").level(131).stats(110, 150, 150, 110, 40, 1).speed(5)
                .defenceBonus(110, 80, 80, 10, 70)
                .attack(AttackDef.of("axe", AttackType.CRUSH, 22, 2.5, 1))
                .drops(Drop.always("bones")).skin(0xD8B090, 0x6A4A2A, 0x3A2A1A, 0x5A5A5A));
        b.boss(BossDef.of("wgs_mercenary_mage", "Mercenary mage").level(112).stats(150, 40, 40, 90, 150, 1).speed(5)
                .defenceBonus(50, 40, 60, 10, 10)
                .attack(AttackDef.of("spell", AttackType.MAGIC, 5, 10, 1).particle("minecraft:enchant"))
                .drops(Drop.always("bones")).skin(0xD8B090, 0x3A2A6A, 0x2A1A4A, 0x3A2A6A));
        // Wiki: level 138, 90 HP, slashes up to 13 every 6 ticks.
        b.boss(BossDef.of("elite_black_knight", "Elite Black Knight").level(138).stats(90, 90, 120, 90, 70, 20).speed(6)
                .defenceBonus(110, 130, 40, -5, 135)
                .attack(AttackDef.of("slash", AttackType.SLASH, 13, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xD8B090, 0x1A1A1A, 0x101010, 0x2A2A2A));
        // Wiki (Surok Magis, Dagon'hai): level 265, 450 HP, magic only (QuestBossScripts.MagicOnly). He attacks like Vorkath: six
        // regular attacks, then a special every seventh (QuestBossScripts.MagicOnly runs the count): elemental surges or an explosive
        // orb (45+). The henchmen special (bind/weaken counters) is not modelled; the regular max hit is approx.
        b.boss(BossDef.of("dark_squall", "Surok Magis").level(265).stats(450, 1, 1, 160, 160, 1).speed(5)
                .defenceBonus(30, 20, 40, 0, 20)
                .attack(AttackDef.of("chaos_bolt", AttackType.MAGIC, 25, 12, 6).particle("minecraft:witch"))
                .attack(AttackDef.of("elemental_surge", AttackType.MAGIC, 45, 12, 1).telegraph(2, "Surok Magis: Witness true sorcery!"))
                .attack(AttackDef.of("explosive_orb", AttackType.MAGIC, 45, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Surok throws an explosive orb at your feet!"))
                .drops(Drop.always("bones")).skin(0xD8C0C0, 0x1A1A2A, 0x101018, 0x6A1A1D));
        // Wiki: level 524, 750 HP (275 in every stat but 286 Strength), 40 in each form every 6 ticks, four attacks per form.
        // From 550 HP (73%) it attacks every 5 ticks and prays against one style, from 200 HP (27%) every 2 ticks and against
        // two: QuestBossScripts.BalanceForms runs the forms, prayers and speeds (BalanceElemental); the phases only announce them.
        b.boss(BossDef.of("balance_elemental", "Balance Elemental").level(524).stats(750, 275, 286, 275, 275, 275).speed(6)
                .defenceBonus(5, 0, -40, -40, 0)
                .attack(AttackDef.of("melee_form", AttackType.CRUSH, 40, 3, 4))
                .attack(AttackDef.of("ranged_form", AttackType.RANGED, 40, 10, 4))
                .attack(AttackDef.of("magic_form", AttackType.MAGIC, 40, 10, 4).particle("minecraft:enchant"))
                // each form opens with its stat-draining special (up to 89, drains combat stats and Prayer unless prayed against)
                .attack(AttackDef.of("melee_drain", AttackType.CRUSH, BalanceElemental.DRAIN_MAX, 3, 0)
                        .telegraph(BalanceElemental.DRAIN_WARN_TICKS, "The Balance Elemental's head turns white with air! (melee drain)")
                        .particle("minecraft:cloud"))
                .attack(AttackDef.of("ranged_drain", AttackType.RANGED, BalanceElemental.DRAIN_MAX, 10, 0)
                        .telegraph(BalanceElemental.DRAIN_WARN_TICKS, "The Balance Elemental's head bursts into flame! (ranged drain)")
                        .particle("minecraft:flame"))
                .attack(AttackDef.of("magic_drain", AttackType.MAGIC, BalanceElemental.DRAIN_MAX, 10, 0)
                        .telegraph(BalanceElemental.DRAIN_WARN_TICKS, "The Balance Elemental's head turns to water! (magic drain)")
                        .particle("minecraft:splash"))
                .phase(73, "The Balance Elemental quickens and begins praying against your attacks!", 1.0, false)
                .phase(27, "The Balance Elemental quickens again and prays against two of your styles!", 1.0, false)
                .drops(Drop.always("coins")).size(1.2f, 2.6f, 1.0f).at("guthix_sanctum").arena(12, "void"));
        // The two tormented demons Lucien sets on the player at the Stone of Jas (fought beside the Guardians of Armadyl): the wiki's
        // Tormented Demon (level 450, 600 HP, 255 in every combat stat but 150 Defence, 31 in every style every 6 ticks).
        b.boss(BossDef.of("wgs_tormented_demon", "Tormented Demon").level(450).stats(600, 255, 255, 150, 255, 255).speed(6)
                .defenceBonus(75, 175, 68, 5, 150)
                .attack(AttackDef.of("claws", AttackType.SLASH, 31, 3.0, 4))
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 31, 10, 3).particle("minecraft:flame"))
                .attack(AttackDef.of("fire_spit", AttackType.RANGED, 31, 10, 3).particle("minecraft:small_flame"))
                .drops(Drop.always("ashes")).model("tormented_demon").size(1.6f, 3.0f, 1.0f));
        b.quest("while_guthix_sleeps", "While Guthix Sleeps", Difficulty.GRANDMASTER, 5)
                .reqQuest("defender_of_varrock", "the_path_of_glouphrie", "fight_arena", "dream_mentor", "the_hand_in_the_sand", "wanted", "temple_of_the_eye", "a_tail_of_two_cats", "tears_of_guthix", "nature_spirit")
                .reqNote("Access to the Warriors' Guild (130 combined Attack + Strength, or 99 in either)")
                .xp(Skill.THIEVING, 80000).xp(Skill.FARMING, 75000).xp(Skill.HERBLORE, 75000).xp(Skill.HUNTER, 50000) // OSRS (current quest XP rewards)
                // quest-helper's item rewards: the elite black armour and Dark Squall robes are kept (handed out during the quest),
                // Movario's base holds 100 death runes, fire runes, coal and magic logs
                .reward("death_rune:100", "fire_rune:100", "coal:100", "magic_logs:100")
                .unlock("Access to Tormented Demons in the Ancient Guthixian Temple").unlock("Access to the Black Knight Catacombs")
                .qp(180)
                .req(Skill.THIEVING, 72).req(Skill.MAGIC, 67).req(Skill.AGILITY, 66).req(Skill.FARMING, 65)
                .req(Skill.HERBLORE, 65).req(Skill.HUNTER, 62).req(Skill.FIREMAKING, 49) // 49 Firemaking: sapphire lantern
                .stage("I can start this quest by speaking to Ivy Sophista in Taverley. (Requires 180 quest points and much more.)")
                .talk("ivy_sophista").say("Lucien seeks the resting place of Guthix himself. The Temple Knights need an agent.",
                        "Thaerisk Cemphier will brief you. He's up the ladder in the house next door.")
                // ---- The Crux Eqal and Monitoring Movario (wiki quick guide) ----
                .stage("I should speak to Thaerisk, then track Lucien's man Movario: the Khazard launderer by the Fight Arena, a broav from "
                        + "the Hunting expert in the Feldip Hills and his base under the Khazard battlefield.")
                .talk("thaerisk").stay().unlessFlag("wgs_assassins").spawn("wgs_assassin")
                .say("?!wgs_assassin1|Lucien means to find Guthix and take his power. His agent, Movario, is our only lead...",
                        "?!wgs_assassin1|* Two assassins burst in! Pray against their arrows, but watch for the dagger.",
                        "?wgs_assassin1|The other assassin is still about! Deal with him!")
                .hint("thaerisk", "Movario has his laundry done by the Fight Arena. Start with the Khazard launderer.")
                .hint("ivy_sophista", "Thaerisk knows more about Lucien's plans than I do. Help him.")
                .hint("hunting_expert", "A creature to track a person? Not until you have something of theirs for it to sniff.")
                .kill("wgs_assassin").stay().unlessFlag("wgs_assassin1", "wgs_assassins").flag("wgs_assassin1").spawn("wgs_assassin")
                .say("* One assassin falls. The other leaps at you!")
                .kill("wgs_assassin").stay().needFlag("wgs_assassin1").unlessFlag("wgs_assassins").flag("wgs_assassins")
                .say("* The second assassin falls.", "@Thaerisk Cemphier: Lucien knows we're onto him. Movario has his laundry done by the "
                        + "Fight Arena: start there.")
                .talk("khazard_launderer").stay().needFlag("wgs_assassins").unlessFlag("wgs_shirt").take("coins:500").give("dirty_shirt")
                .flag("wgs_shirt")
                .say("Movario? Five hundred coins buys a lot of memory... He never collected this shirt. Lives out on the battlefield, I think.",
                        "* A creature with a good nose could follow that smell. The Hunting expert in the Feldip Hills would know one.")
                .missing("I don't talk about my customers. Not for nothing, anyway. (500 coins)")
                .talk("hunting_expert").stay().needFlag("wgs_shirt").unlessFlag("wgs_expert", "wgs_trapdoor").flag("wgs_expert")
                .say("> I need a creature to help track down a person.",
                        "A broav! Best nose in the Feldip Hills. Set a pit trap to the west of my camp, bait it with mort myre fungus, and bring "
                                + "me what falls in. I'll train it.")
                .reach("wgs_pit_trap", 4).stay().needFlag("wgs_expert").unlessFlag("wgs_trapdoor").unless("unconscious_broav", "broav")
                .need("knife").take("logs", "mort_myre_fungus").reqSkill(Skill.HUNTER, 62).give("unconscious_broav")
                .say("* You dig a pit, cover it with logs cut to size and bait it with the mort myre fungus.",
                        "* A broav sniffs at the fungus and tumbles in. You dismantle the trap and lift out an unconscious broav.")
                .missing("* A pit trap needs logs, a knife and some mort myre fungus for bait.")
                .talk("hunting_expert").stay().needFlag("wgs_expert").unlessFlag("wgs_trapdoor").unless("broav").take("unconscious_broav")
                .give("broav").say("* She brings the broav round and trains it to follow a scent.", "There. Let it sniff something of your man's.")
                .missing("Bring me a broav from a pit trap to the west and I'll train it.")
                .reach("movarios_base", 4).stay().needFlag("wgs_shirt").unlessFlag("wgs_trapdoor").need("broav", "dirty_shirt")
                .flag("wgs_trapdoor")
                .say("* You drop the broav and let it sniff the dirty shirt. It squeals at a broken table: beneath it, a trapdoor!",
                        "* You climb down into Movario's base and take the runes from the open chest.")
                .missing("* A broken table stands in a run-down house. Something's odd about it, but you'd need a nose to find out what.")
                .puzzle("movarios_base", WGS_DOOR).reqSkill(Skill.HUNTER, 62).needFlag("wgs_trapdoor").unlessFlag("wgs_door").flag("wgs_door").stay()
                .say("* The rune's power flows into the door's lock and it creaks open.")
                .puzzle("movarios_base", WGS_WIRES).needFlag("wgs_door").unlessFlag("wgs_wires").flag("wgs_wires").stay()
                .say("* The last bookcase clicks: the electric gate dies down. You search the spiral staircase for its trap and climb up.")
                .puzzle("movarios_base", WGS_WEIGHTS).needFlag("wgs_wires").unlessFlag("wgs_weights").flag("wgs_weights").stay()
                .say("* The gauge settles on your weight: the floor beneath the study stops humming and the way upstairs is safe.")
                .reach("movarios_base", 10).needFlag("wgs_door", "wgs_weights").give("movarios_notes")
                .say("* From the desk you take the first volume of Movario's notes; the waste-paper basket hides a ruby key.",
                        "* The ruby key opens a bookcase stair and the trapped chest by Movario's bed: the second volume. They point to "
                                + "Guthix's sanctum, deep below.")
                .missing("* Movario's base is sealed by a door marked PROHIBITED, an electrified gate and a pressure-gauged study.")
                .stage("I should take Movario's notes back to Thaerisk in Taverley.")
                .talk("thaerisk").take("movarios_notes").say("Guthix's sanctum! Lucien and his generals are already moving.",
                        "And Idria has been taken by Lucien's mercenaries, just north of McGrubor's Wood. Get her back; she'll meet us at the "
                                + "White Knights' Castle.")
                .missing("Movario's notes: did you find them in his base?")
                // ---- Forming a fellowship, Amassing the masters, The Dark Squall (wiki quick guide) ----
                .stage("I should rescue Idria, help Akrisae unmask Lucien's spy network with a teleorb and a snapdragon truth serum, gather "
                        + "allies, and infiltrate the Dark Squall's base under the Black Knights' Fortress.")
                .reach("wgs_mcgrubors", 8).stay().unlessFlag("wgs_ambush").flag("wgs_ambush").spawn("wgs_mercenary_axeman")
                .say("* Through the loose railing you find Idria bound in a shack, guarded by Lucien's mercenaries. An axeman charges!")
                .kill("wgs_mercenary_axeman").stay().needFlag("wgs_ambush").unlessFlag("wgs_axe1").flag("wgs_axe1").spawn("wgs_mercenary_axeman")
                .say("* The first axeman falls. Another takes his place!")
                .kill("wgs_mercenary_axeman").stay().needFlag("wgs_axe1").unlessFlag("wgs_axe2").flag("wgs_axe2").spawn("wgs_mercenary_mage")
                .say("* The second axeman falls. Their mage raises his hands!")
                .kill("wgs_mercenary_mage").stay().needFlag("wgs_axe2").unlessFlag("wgs_mercs").flag("wgs_mercs")
                .say("* The mercenary mage crumples. Idria is free.")
                .reach("wgs_mcgrubors", 8).stay().needFlag("wgs_ambush").unlessFlag("wgs_axe1").spawn("wgs_mercenary_axeman")
                .say("* A mercenary axeman blocks the way to Idria!")
                .reach("wgs_mcgrubors", 8).stay().needFlag("wgs_axe1").unlessFlag("wgs_axe2").spawn("wgs_mercenary_axeman")
                .say("* The second axeman is still here!")
                .reach("wgs_mcgrubors", 8).stay().needFlag("wgs_axe2").unlessFlag("wgs_mercs").spawn("wgs_mercenary_mage")
                .say("* The mercenary mage still guards Idria!")
                .talk("idria").stay().needFlag("wgs_mercs").unlessFlag("wgs_idria").flag("wgs_idria")
                .say("You came for me! Lucien knows the Temple Knights are watching him. Akrisae will know what to do: he's at the White "
                        + "Knights' Castle.")
                .missing("?!wgs_mercs|* Lucien's mercenaries still hold Idria north of McGrubor's Wood.")
                .talk("akrisae").stay().needFlag("wgs_idria").unlessFlag("wgs_teleorb").unless("wgs_teleorb").give("wgs_teleorb").travel("DRAYNOR_VILLAGE")
                .say("Lucien's spies are everywhere. Plant this teleorb on one of them and it will bring him straight to our cells.",
                        "Shady strangers have been seen around Draynor Village. I'll send you there.")
                .talk("shady_stranger").stay().unlessFlag("wgs_teleorb").take("wgs_teleorb").flag("wgs_teleorb")
                .say("What do you want? Move along.", "* You slip the teleorb into his pocket. With a flash, he's gone.")
                .talk("akrisae").stay().needFlag("wgs_teleorb").unlessFlag("wgs_spy").flag("wgs_spy")
                .say("The spy is in our cell, but he won't talk. Thaerisk can brew a truth serum, if it's strong enough.")
                .talk("thaerisk").stay().needFlag("wgs_spy").unlessFlag("wgs_seed").give("snapdragon_seed").flag("wgs_seed")
                .travel("PORT_SARIM")
                .say("> Could I have that seed?", "Take this snapdragon seed. Betty in Port Sarim can help you enrich it. I'll send you there.")
                .reach("wgs_bettys_counter", 3).stay().needFlag("wgs_seed").unlessFlag("wgs_planted")
                .unless("pink_dye", "rose_tinted_lens", "enriched_snapdragon_seed").take("coins:20").give("pink_dye")
                .say("* Betty sells you some pink dye for 20 coins.")
                .missing("* Betty sells pink dye for 20 coins.")
                .reach("wgs_bettys_counter", 3).stay().needFlag("wgs_seed").unlessFlag("wgs_planted").unless("rose_tinted_lens", "enriched_snapdragon_seed")
                .take("pink_dye", "lantern_lens").give("rose_tinted_lens")
                .say("* You dye a lantern lens pink: a rose-tinted lens.")
                .missing("* The pink dye would tint a lantern lens.")
                .reach("wgs_bettys_counter", 3).stay().needFlag("wgs_seed").unlessFlag("wgs_planted").unless("enriched_snapdragon_seed")
                .need("rose_tinted_lens").take("snapdragon_seed").give("enriched_snapdragon_seed")
                .say("* Standing in Betty's doorway, you focus the light through the rose-tinted lens onto the seed on her counter.",
                        "* It glows: an enriched snapdragon seed.")
                .reach("wgs_snapdragon_patch", 4).stay().needFlag("wgs_seed").unlessFlag("wgs_planted").take("enriched_snapdragon_seed")
                .need("seed_dibber").flag("wgs_planted")
                .say("* At the top of the castle you plant the enriched snapdragon seed in the special patch. It sprouts at once.")
                .missing("* The special patch at the top of the castle is ready for an enriched snapdragon seed (and a seed dibber).")
                .talk("idria").stay().needFlag("wgs_planted").unlessFlag("wgs_contacts").take("astral_rune:4", "cosmic_rune:4", "air_rune:8")
                .reqSkill(Skill.MAGIC, 67).flag("wgs_contacts")
                .say("We'll need allies against Lucien. The slayer masters must be warned.",
                        "* You cast NPC Contact four times. Turael, Mazchna, Cyrisus and Duradel each promise to stand against Lucien.")
                .missing("Contact the slayer masters with NPC Contact: Turael, Mazchna, Cyrisus and Duradel. (Four casts: 4 astral, 4 cosmic "
                        + "and 8 air runes, on the Lunar spellbook.)")
                .reach("wgs_snapdragon_patch", 4).stay().needFlag("wgs_contacts").unlessFlag("wgs_sketch").unless("enriched_snapdragon", "super_truth_serum")
                .give("enriched_snapdragon")
                .say("* The enriched snapdragon has grown. You harvest it.")
                .talk("thaerisk").stay().needFlag("wgs_contacts").unlessFlag("wgs_sketch").unless("super_truth_serum").take("enriched_snapdragon")
                .give("super_truth_serum")
                .say("Here's my truth serum. Add the enriched snapdragon... There: super truth serum. He'll talk now.")
                .missing("The serum needs that enriched snapdragon from the castle's special patch.")
                .reach("wgs_wk_cell", 3).stay().needFlag("wgs_contacts").unlessFlag("wgs_sketch").unless("suspect_sketch").take("super_truth_serum")
                .give("suspect_sketch")
                .say("* From the drawers south of the cell you take charcoal and papyrus, and give the shady stranger the super truth serum.",
                        "* He talks: of Lucien's spymaster, the Dark Squall. From his description you draw a suspect sketch.")
                .talk("idria").stay().needFlag("wgs_contacts").unlessFlag("wgs_sketch").take("suspect_sketch").flag("wgs_sketch")
                .say("The Dark Squall! Lucien's spymaster. We need more swords before we go after him: the Warriors' Guild.")
                .reach("wgs_warriors_guild", 6).stay().needFlag("wgs_sketch").unlessFlag("wgs_recruits").flag("wgs_recruits")
                .say("* Ghommal and Harrallak Menarous at the entrance, and Sloane upstairs, hear you out about Lucien. The Warriors' Guild "
                        + "will fight.")
                .talk("akrisae").stay().needFlag("wgs_recruits").unlessFlag("wgs_fortress").flag("wgs_fortress").travel("black_knights_fortress")
                .say("* Hazelmere appears in a vision: the Dark Squall hides beneath the Black Knights' Fortress.",
                        "Disguise yourself as a Black Knight, a bronze med helm and an iron chainbody will do, and find his base. I'll send you there.")
                .reach("black_knights_fortress", 8).stay().needFlag("wgs_fortress").unlessFlag("wgs_catacombs")
                .need("bronze_med_helm", "iron_chainbody").take("unpowered_orb", "cosmic_rune:3", "water_rune:30").reqSkill(Skill.MAGIC, 56)
                .flag("wgs_catacombs").spawn("elite_black_knight")
                .say("* Disguised, you push the hidden wall and climb down. You charge an orb on the carved tile and drop into the Black "
                                + "Knight Catacombs.",
                        "* Across the broken bridge an elite black knight and his dark warriors bar the way!")
                .missing("* The guards won't let you near the fortress unless you look like a Black Knight: a bronze med helm and an iron "
                        + "chainbody. The carved tile below takes a charged orb (an unpowered orb, 3 cosmic and 30 water runes).")
                .kill("elite_black_knight").stay().needFlag("wgs_catacombs").unlessFlag("wgs_ebk1", "wgs_elite").flag("wgs_ebk1")
                .spawn("elite_black_knight").say("* The elite black knight falls. Another steps out of the dark!")
                .kill("elite_black_knight").stay().needFlag("wgs_ebk1").unlessFlag("wgs_ebk2", "wgs_elite").flag("wgs_ebk2")
                .spawn("elite_black_knight").say("* A second knight falls. One more!")
                .kill("elite_black_knight").stay().needFlag("wgs_ebk2").unlessFlag("wgs_elite").flag("wgs_elite")
                .give("elite_black_full_helm", "elite_black_platebody", "elite_black_platelegs")
                .say("* The third falls: you now have a full set of elite black armour.")
                .reach("black_knights_fortress", 8).stay().needFlag("wgs_catacombs").unlessFlag("wgs_elite").spawn("elite_black_knight")
                .say("* An elite black knight still bars the way.")
                .reach("black_knights_fortress", 8).stay().needFlag("wgs_elite").unlessFlag("wgs_base").flag("wgs_base")
                .give("dark_squall_hood", "dark_squall_robe_top", "dark_squall_robe_bottom", "wgs_cell_key", "lobster", "restore_potion")
                .say("* In elite black armour you climb the wall and walk into the Dark Squall's inner base: a spare set of armour from one "
                        + "wardrobe, the Dark Squall's robes from the other,",
                        "* a lobster and a restore potion from the table, and the key to Silif's cell from the rack.")
                .talk("silif").stay().needFlag("wgs_base").unlessFlag("wgs_silif", "wgs_surok").take("wgs_cell_key", "lobster", "restore_potion")
                .flag("wgs_silif").spawn("dark_squall")
                .say("* You unlock the cell and feed Silif the lobster and the restore potion, and give him the spare armour.",
                        "Thank you, friend. Those maps on the wall... Lucien's camp. Take this teleorb, and beware: the Dark Squall is upstairs.",
                        "* You climb the ladder. The Dark Squall throws back his hood: Surok Magis! Only magic will touch him.")
                .missing("* Silif lies starving in a locked cell. You'd need the cell key, and food and a restore potion for him.")
                .talk("silif").stay().needFlag("wgs_silif").unlessFlag("wgs_surok").spawn("dark_squall")
                .say("The Dark Squall is still up there! Use magic: nothing else will touch Surok.")
                .kill("dark_squall").stay().needFlag("wgs_silif").unlessFlag("wgs_surok").flag("wgs_surok").give("strange_teleorb")
                .say("* Surok falls. You plant Silif's teleorb on him and he vanishes into the White Knights' cells.",
                        "* On the study's table lies the Dark Squall's strange teleorb.")
                // saves from before round 37 beat Surok without picking up his strange teleorb (the old fight left none)
                .talk("silif").stay().needFlag("wgs_surok").unlessFlag("wgs_camp", "wgs_orbed").unless("strange_teleorb").give("strange_teleorb")
                .say("You left this on the study table upstairs: the Dark Squall's strange teleorb. In his robes, it might take you "
                        + "wherever he was going.")
                .reach("wgs_wk_cell", 3).stay().needFlag("wgs_surok").unlessFlag("wgs_camp", "wgs_orbed")
                .need("strange_teleorb", "dark_squall_hood", "dark_squall_robe_top", "dark_squall_robe_bottom").flag("wgs_orbed")
                .travel("wgs_lucien_camp")
                .say("* Robed as the Dark Squall, you stand in the cell's magic circle and activate the strange teleorb...")
                .missing("* The strange teleorb answers only to the Dark Squall: wear his robes (hood, top and bottom).")
                .reach("wgs_lucien_camp", 8).stay().needFlag("wgs_orbed").unlessFlag("wgs_camp").flag("wgs_camp")
                .say("* In Lucien's camp you climb the ice wall and jump to the ledge over the Chaos Temple's roof.",
                        "* Below, the Mahjarrat gather: Lucien knows where Guthix sleeps!")
                .talk("akrisae").needFlag("wgs_camp")
                .say("Surok, the Dark Squall? And Lucien is at the Chasm of Tears already. Idria will lead the Temple Knights: go, now.")
                .missing("?!wgs_idria|Idria was taken by Lucien's mercenaries north of McGrubor's Wood. Free her first.",
                        "?wgs_idria|?!wgs_teleorb|Plant the teleorb on one of Lucien's spies in Draynor Village.",
                        "?wgs_teleorb|?!wgs_sketch|Thaerisk's serum needs an enriched snapdragon from the castle's special patch.",
                        "?wgs_sketch|?!wgs_surok|Find the Dark Squall's base under the Black Knights' Fortress.",
                        "?wgs_surok|?!wgs_camp|Use the Dark Squall's strange teleorb in our cell, in his robes, and see what Lucien is doing.")
                // saves from before round 33 reached this stage without the assassins' flag: the old briefing goes on to the chasm
                .talk("ivy_sophista").unlessFlag("wgs_assassins")
                .say("The Mahjarrat are converging on the sanctum. Something is guarding the way: a creature of pure balance.")
                // ---- Into the Chasm (wiki quick guide) ----
                .stage("I must follow Lucien into the Chasm of Tears in the Dark Squall's robes, open the Ancient Guthixian Temple and "
                        + "defeat the Balance Elemental guarding the Stone.")
                // saves from before round 37 came here without the Dark Squall's robes (the old quest never handed them out), and
                // Movario lets only his master by: Idria brings the set taken from the Dark Squall's base (every newer save has
                // wgs_base, set where the robes are found)
                .talk("idria").stay().unlessFlag("wgs_base", "wgs_movario").give("dark_squall_hood", "dark_squall_robe_top", "dark_squall_robe_bottom")
                .flag("wgs_base")
                .say("The Temple Knights searched the Dark Squall's base after you left. Take his robes: Movario will be at the chasm, and "
                        + "he'll only make way for his master.")
                .talk("idria").stay().unlessFlag("wgs_chasm").take("sapphire").need("tinderbox").reqSkill(Skill.FIREMAKING, 49)
                .give("sapphire_lantern").flag("wgs_chasm")
                .say("The Temple Knights follow you down. Light creatures carry travellers across the chasm, but only to a sapphire's glow.",
                        "* She hands you a bullseye lantern. You fit a cut sapphire as its lens and light it: a sapphire lantern.")
                .missing("A light creature will only follow a sapphire lantern. Bring a cut sapphire and a tinderbox (49 Firemaking).")
                .talk("movario").stay().needFlag("wgs_chasm").unlessFlag("wgs_movario")
                .need("dark_squall_hood", "dark_squall_robe_top", "dark_squall_robe_bottom").flag("wgs_movario")
                .say("Master Squall! The way down is open. Lucien waits below.", "* Movario takes you for the Dark Squall.")
                .missing("* Movario eyes you suspiciously. In the Dark Squall's robes he'd take you for his master.")
                .reach("wgs_light_creature", 6).underground().stay().needFlag("wgs_movario").unlessFlag("wgs_light").need("sapphire_lantern").flag("wgs_light")
                .say("* You hold the lit sapphire lantern out to a light creature. It carries you down into the chasm.",
                        "* From the skeletal remains you take a spade, a hammer and a chisel; from four braziers you chisel elemental orbs and "
                                + "shape them into blocks.")
                .missing("* The light creatures drift past. They'd follow a lit sapphire lantern.")
                .puzzle("wgs_light_creature", WGS_SKULLS).underground().needFlag("wgs_light").unlessFlag("wgs_skulls").flag("wgs_skulls").stay()
                .say("* The last block clicks into place and the stone cube in the fourth skull grinds open.")
                .reach("wgs_temple", 6).underground().stay().needFlag("wgs_skulls").unlessFlag("wgs_pouches").flag("wgs_pouches")
                .say("* In the temple's central chamber a skeleton holds a silver sickle (b) and six druid pouches. Druid spirits along the "
                        + "paths fill them with herblore ingredients.")
                .puzzle("wgs_temple", WGS_STATUES, 0).underground().needFlag("wgs_pouches").unlessFlag("wgs_statue1").flag("wgs_statue1").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 1).underground().needFlag("wgs_statue1").unlessFlag("wgs_statue2").flag("wgs_statue2").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 2).underground().needFlag("wgs_statue2").unlessFlag("wgs_statue3").flag("wgs_statue3").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 3).underground().needFlag("wgs_statue3").unlessFlag("wgs_statue4").flag("wgs_statue4").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 4).underground().needFlag("wgs_statue4").unlessFlag("wgs_statue5").flag("wgs_statue5").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 5).underground().needFlag("wgs_statue5").unlessFlag("wgs_statue6").flag("wgs_statue6").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 6).underground().needFlag("wgs_statue6").unlessFlag("wgs_statue7").flag("wgs_statue7").stay()
                .say("* The statue crumbles to a dolmen.")
                .puzzle("wgs_temple", WGS_STATUES, 7).underground().needFlag("wgs_statue7").unlessFlag("wgs_statues").flag("wgs_statues").stay()
                .say("* The eighth statue crumbles. You set the eight dolmens on the stone table and the huge door swings open.",
                        "* Beyond it the way climbs to Guthix's sanctum, where the Stone of Jas lies.")
                .kill("balance_elemental").needFlag("wgs_skulls", "wgs_statues")
                .say("* The Balance Elemental dissolves. The Stone of Jas hums beside a sleeping Guthix.")
                .missing("?!wgs_skulls|* The way into the temple is sealed by the three skulls' mechanisms.",
                        "?wgs_skulls|?!wgs_statues|* The huge door opens only to eight dolmens on the stone table.")
                // ---- Lucien's demons (wiki quick guide) ----
                .stage("I must fight off Lucien's tormented demons at the Stone of Jas, then report to Idria at the White Knights' Castle.")
                .reach("guthix_sanctum", 10).stay().unlessFlag("wgs_stone").flag("wgs_stone").spawn("wgs_tormented_demon")
                .say("* You touch the Stone of Jas. Lucien and Movario arrive with two tormented demons; the Guardians of Armadyl fight "
                        + "beside you!")
                .kill("wgs_tormented_demon").stay().needFlag("wgs_stone").unlessFlag("wgs_td1", "wgs_demons").flag("wgs_td1")
                .spawn("wgs_tormented_demon").say("* One tormented demon falls. The second roars!")
                .kill("wgs_tormented_demon").stay().needFlag("wgs_td1").unlessFlag("wgs_demons").flag("wgs_demons")
                .say("* The second tormented demon falls. Lucien flees, and Idria teleports you back to the White Knights' Castle.")
                .reach("guthix_sanctum", 10).stay().needFlag("wgs_stone").unlessFlag("wgs_demons").spawn("wgs_tormented_demon")
                .say("* A tormented demon still guards the Stone!")
                .talk("idria").needFlag("wgs_demons")
                .say("Guthix sleeps on, and Lucien has been stopped... for now. Gielinor owes you more than it will ever know.")
                .missing("Lucien's demons are still at the Stone of Jas. Help the Guardians of Armadyl drive them off!");
    }

    /** The wiki's statue table: keyword, then its accepted ingredient sets. */
    static final String[][] WGS_STATUE_TABLE = {
            {"Agility", "Toadflax + toad's legs"},
            {"Attack", "Guam leaf + eye of newt", "Irit leaf + eye of newt"},
            {"Balance", "Harralander + red spiders' eggs + garlic + silver dust"},
            {"Combat", "Harralander + goat horn dust"},
            {"Defence", "Ranarr weed + white berries"},
            {"Energy", "Avantoe + mort myre fungus", "Harralander + chocolate dust"},
            {"Fishing", "Avantoe + snape grass"},
            {"Hunter", "Avantoe + kebbit teeth dust"},
            {"Magic", "Lantadyme + potato cactus"},
            {"Prayer", "Ranarr weed + snape grass"},
            {"Ranging", "Dwarf weed + wine of zamorak"},
            {"Restoration", "Snapdragon + red spiders' eggs", "Harralander + red spiders' eggs"},
            {"Strength", "Tarromin + limpwurt root", "Kwuarm + limpwurt root"}};

    /**
     * While Guthix Sleeps, Movario's base (wiki quick guide): one letter of "PROHIBITED" on the battered door is stylised as a rune,
     * different for every player, and that rune opens it. The wiki lists five: O as a mind rune, H as an earth rune, E as an air
     * rune, E as a fire rune and D as a water rune.
     */
    static final QuestPuzzle WGS_DOOR = wgsDoor();
    /**
     * The electrified gate in the library: a bookcase clicks, and the floor wires light the way from bookcase to bookcase until the
     * gate dies down (quest-helper's seven wired bookcases). The order the wires light is per player. // approx: the wiki gives no
     * fixed route, so the walk is drawn from the seven bookcases
     */
    static final QuestPuzzle WGS_WIRES = wgsWires();
    /** The study's pressure gauge reads the player's weight on entering; they must weigh that again (1, 2 and 5 kg weights). */
    static final QuestPuzzle WGS_WEIGHTS = wgsWeights();
    /** The Chasm of Tears: an elemental block in each of three skulls' mechanisms, matching symbols individual to each player. */
    static final QuestPuzzle WGS_SKULLS = wgsSkulls();
    /**
     * The Ancient Guthixian Temple: 8 of the 13 statues (different for every player) at the ends of the paths, each opened by the
     * herblore ingredients for its keyword (the wiki's table) to give a dolmen.
     */
    static final QuestPuzzle WGS_STATUES = wgsStatues();

    private static QuestPuzzle wgsDoor() {
        String[] runes = {"Air", "Water", "Earth", "Fire", "Mind"};
        // the wiki's five stylised letters: {letter, how it is drawn, rune index}
        String[][] variants = {{"O", "an eye with an orange ring around it", "4"}, {"H", "a brown clod of earth", "2"},
                {"E", "a pale swirl of wind", "0"}, {"E", "a red flame", "3"}, {"D", "a blue droplet", "1"}};
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_door").room("door");
        for (String[] v : variants) {
            b.variant(null).prompt("* An old battered door. Painted across it: PROHIBITED.",
                            "* Looking closely, the " + v[0] + " is drawn as " + v[1] + ". Which rune do you use on the door?")
                    .options(runes).solution(Integer.parseInt(v[2]));
        }
        return b.failLines("* The door blasts you with a burst of magic. Its colour hints at the right rune.").build();
    }

    private static QuestPuzzle wgsWires() {
        String[] cases = {"East-facing bookcase (east)", "West-facing bookcase (west)", "Southern bookcase (west)",
                "North end of the west wall", "South end of the west wall", "Northern bookcase", "Eastern bookcase"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_wires").room("library");
        int[][] routes = {{0, 1, 2, 3, 4, 5, 6}, {0, 2, 4, 6, 1, 3, 5}, {0, 6, 5, 4, 3, 2, 1}, {0, 3, 6, 2, 5, 1, 4}};
        for (int[] route : routes) {
            StringBuilder lit = new StringBuilder();
            for (int i = 1; i < route.length; i++) {
                lit.append(i == 1 ? "" : ", then ").append(cases[route[i]].toLowerCase(java.util.Locale.ROOT));
            }
            b.variant(null).prompt("* Searching the bookcases, one clicks like a lever: the " + cases[route[0]].toLowerCase(java.util.Locale.ROOT) + ".",
                            "* Floor wires light up from it in turn: " + lit + ".",
                            "* Search the bookcases in the order the wires light.")
                    .options(cases).solution(route);
        }
        return b.failLines("* A jolt runs through you from the bookcase. The wires go dark; start again.").build();
    }

    private static QuestPuzzle wgsWeights() {
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_weights").room("gauge");
        for (int diff = 1; diff <= 8; diff++) {
            int gauge = 52 + diff * 3;
            b.variant(null).prompt("* Notes on pressure: the study floor remembers your weight as you came in.",
                            "* The pressure gauge reads " + gauge + " tickits (1 tickit = 1 kg). Without your gear you now weigh " + (gauge - diff) + " kg.",
                            "* How much do you take from the pile of 1, 2 and 5 kg weights?")
                    .options("1 kg", "2 kg", "3 kg", "4 kg", "5 kg", "6 kg", "7 kg", "8 kg").solution(diff - 1);
        }
        return b.failLines("* The gauge's needle swings wildly and a jolt runs through the floor. Try again.").build();
    }

    private static QuestPuzzle wgsSkulls() {
        String[] elements = {"Air", "Water", "Earth", "Fire"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_skulls").room("skulls");
        for (int l = 0; l < 4; l++) {
            for (int m = 0; m < 4; m++) {
                for (int r = 0; r < 4; r++) {
                    if (l == m || m == r || l == r) {
                        continue;
                    }
                    b.variant(null).prompt("* From the braziers you chisel four elemental orbs and turn them into blocks.",
                                    "* Crawling into the skulls' nose cavities you find the mechanisms: the left one bears the symbol of "
                                            + elements[l].toLowerCase(java.util.Locale.ROOT) + ", the middle one " + elements[m].toLowerCase(java.util.Locale.ROOT)
                                            + ", the right one " + elements[r].toLowerCase(java.util.Locale.ROOT) + ".",
                                    "* Which block goes in each, from left to right?")
                            .options("Air block", "Water block", "Earth block", "Fire block").solution(l, m, r);
                }
            }
        }
        return b.failLines("* The block won't fit the mechanism. You crawl back out to start again.").build();
    }

    private static QuestPuzzle wgsStatues() {
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_statues").picks(8).shuffle();
        for (int k = 0; k < WGS_STATUE_TABLE.length; k++) {
            String[] row = WGS_STATUE_TABLE[k];
            List<String> options = new ArrayList<>();
            for (int i = 1; i < row.length; i++) {
                options.add(row[i]);
            }
            // the other choices: the first ingredient sets of the next three keywords
            for (int d = 1; d <= 3; d++) {
                options.add(WGS_STATUE_TABLE[(k + d) % WGS_STATUE_TABLE.length][1]);
            }
            b.room(row[0].toLowerCase(java.util.Locale.ROOT)).variant(null)
                    .prompt("* At the end of a path stands a statue carved with one word: " + row[0].toUpperCase(java.util.Locale.ROOT) + ".",
                            "* Which ingredients from the druid pouches do you use on it?")
                    .options(options.toArray(new String[0]));
            for (int i = 1; i < row.length; i++) {
                b.solution(i - 1);
            }
        }
        return b.failLines("* The statue doesn't react. Perhaps another mix of ingredients.").build();
    }
}
