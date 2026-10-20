package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

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
        b.item("royal_seed_pod", "Royal seed pod", "seed", 0xE0C040, "Teleports you to the Grand Tree.");
        b.item("kruks_paw", "Kruk's paw", "bones", 0x5A3A26, "Proof that Kruk is dead.");
        b.boss(BossDef.of("kruk", "Kruk").level(149).stats(320, 150, 150, 140, 100, 150).speed(4)
                .defenceBonus(80, 80, 80, 60, 80)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 30, 3, 6))
                .attack(AttackDef.of("boulder", AttackType.RANGED, 25, 10, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "Kruk hurls a boulder into the air!").particle("minecraft:block_crumble"))
                .attack(AttackDef.of("roar", AttackType.MAGIC, 18, 8, 2).special(Special.DRAIN_PRAYER, 8))
                .phase(50, "Kruk roars and pounds his chest!", 1.25, false)
                .drops(Drop.always("big_bones")).size(1.3f, 2.4f, 1.0f).at("kruks_dungeon").arena(13, "jungle"));
        b.item("mm2_mysterious_note", "Mysterious note", "note", 0xE0D8B0, "A note from Glough's house, written in invisible ink.");
        b.item("mm2_kruk_greegree", "Kruk monkey greegree", "talisman", 0x5A3A26, "Zooknock's greegree, made from Kruk's paw.");
        b.item("mm2_deconstructed_onyx", "Deconstructed onyx", "gem", 0x2A2A2A, "The charged onyx from Glough's device, chiselled apart.");
        b.spot("glough_cave", "Glough's cave", 2490, 3520); // approx: north-east of the Gnome Stronghold
        Cast.npc(b, "kob", "Kob", "troll_stronghold", 4, 4, Cast.TROLL, 0x5A4A3A);
        Cast.npc(b, "keef", "Keef", "gu_tanoth", 3, -3, Cast.TROLL, 0x6A5A2A);
        // Glough (Monkey Madness II final boss): OSRS Wiki infobox.
        b.boss(BossDef.of("mm2_glough", "Glough").level(431).stats(575, 260, 270, 248, 250, 262).speed(6)
                .attack(AttackDef.of("staff_slam", AttackType.CRUSH, 61, 2.5, 6))
                .attack(AttackDef.of("demonic_blast", AttackType.MAGIC, 45, 10, 3).particle("minecraft:soul_fire_flame")) // approx: 40+
                .attack(AttackDef.of("rock_throw", AttackType.RANGED, 45, 10, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "Glough hurls a rock into the air!"))
                .drops(Drop.always("bones")).skin(0xC09060, 0x3A3A3A, 0x2A2A2A, 0x5A1A1A).size(0.9f, 2.0f, 1.2f)
                .at("glough_cave").arena(12, "crypt"));
        b.quest("monkey_madness_ii", "Monkey Madness II", Difficulty.GRANDMASTER, 4)
                .xp(Skill.SLAYER, 80000).xp(Skill.AGILITY, 60000).xp(Skill.THIEVING, 50000).xp(Skill.HUNTER, 50000) // OSRS (current quest XP rewards)
                .reqQuest("monkey_madness_i", "the_grand_tree", "troll_stronghold")
                .reqQuest("enlightened_journey", "the_eyes_of_glouphrie", "watchtower")
                .req(Skill.SLAYER, 69).req(Skill.CRAFTING, 70).req(Skill.HUNTER, 60).req(Skill.AGILITY, 55)
                .req(Skill.THIEVING, 55).req(Skill.FIREMAKING, 60)
                .reward("royal_seed_pod")
                .unlock("Access to demonic gorillas and the Kruk's dungeon")
                .stage("I can start this quest by speaking to King Narnode Shareen. (Requires Monkey Madness I, The Grand Tree, Troll Stronghold and more; 69 Slayer, 70 Crafting.)")
                .talk("king_narnode").needFlag("rfd_awowogei")
                .missing("King Awowogei is still frozen at the Lumbridge banquet. Free him first. (Requires Recipe for Disaster: Freeing King Awowogei.)")
                .say("Glough has escaped from his prison! And there are rumours of monkeys sneaking into the Stronghold. Investigate, please.")
                // OSRS chapter I: Glough's house hides a note in invisible ink (lemon juice, grapes, a candle); Auguste on Entrana saw the airships.
                .stage("I should search Glough's house, have the king translate what I find and ask Auguste on Entrana about it. Then on to Ape Atoll.")
                .reach("GNOME_STRONGHOLD", 12).stay().unless("mm2_mysterious_note").unlessFlag("mm2_note").need("pestle_and_mortar")
                .take("lemon", "grapes").give("mm2_mysterious_note")
                .say("* Up the trees of Glough's house you turn the gnome statue and search the cupboard. In the fire remains: a blank note.",
                        "* You coat a brush in lemon and grape juice and paint the note. Writing appears!")
                .missing("* The note in the fire remains looks blank. Juice from a lemon and some grapes (and a pestle and mortar) might reveal it.")
                .talk("king_narnode").stay().take("mm2_mysterious_note").flag("mm2_note")
                .say("*He translates it.* 'Airships'... 'Ape Atoll'... 'the 10th squad'. Glough was building something. Ask Auguste on Entrana; he knows flying.")
                .talk("auguste").stay().needFlag("mm2_note").unlessFlag("mm2_auguste").flag("mm2_auguste")
                .say("Giant flying machines? Over Karamja? Mon dieu, I saw them myself from my balloon! They flew south-west.")
                .talk("lumdo").needFlag("mm2_auguste").travel("ape_atoll").say("Back to Ape Atoll? Hold on. *Lumdo sails you across.*")
                .missing("The king hasn't ordered me anywhere. Find out what Glough was up to first.")
                // OSRS chapter II: Garkor and Awowogei, the monkey archers' tracks, the maze under the island and Kruk.
                .stage("I should talk to Garkor on Ape Atoll and follow the monkey archers' tracks to whoever is leading them.")
                .talk("garkor").stay().unlessFlag("mm2_garkor").flag("mm2_garkor")
                .say("Glough has allied with Kruk, a monkey general. Together they've made monstrous demonic gorillas. Ask the archers on the west hill about Kruk.")
                .kill("kruk").needFlag("mm2_garkor").give("kruks_paw")
                .say("* You follow the tracks to a trapdoor in the jungle grass and through the maze beneath. Kruk falls with a thunderous crash!")
                .stage("I must stop Glough: a Kruk greegree from Zooknock, the troll generals Kob and Keef, Glough's laboratory and his gorillas.")
                .talk("zooknock").stay().unless("mm2_kruk_greegree").take("kruks_paw", "monkey_talisman").give("mm2_kruk_greegree")
                .say("Kruk's paw! With a talisman I can make a greegree from it. Glough won't suspect a thing.")
                .talk("kob").stay().unlessFlag("mm2_kob").reqSkill(Skill.SLAYER, 69).flag("mm2_kob")
                .say("* Kob fights until he gives up.", "Kob not fight for Glough no more. Kob promise.")
                .talk("keef").stay().needFlag("mm2_kob").unlessFlag("mm2_keef").flag("mm2_keef")
                .say("* Keef fights until he gives up.", "Okay, okay! Keef's trolls stay home.")
                .reach("kruks_dungeon", 12).stay().needFlag("mm2_keef").unlessFlag("mm2_lab").need("mm2_kruk_greegree", "chisel", "hammer").take("onyx")
                .flag("mm2_lab")
                .say("* As Kruk you swing across the monkey bars into Glough's laboratory and ride a stunted demonic gorilla downstairs.",
                        "* You chisel the charged onyx apart and jam the device: the incubation chambers go dark.")
                .missing("* Glough's laboratory lies beyond monkey bars only Kruk could use. You'd also want a chisel, a hammer and an onyx for his device.")
                .kill("tortured_gorilla", 4).stay().needFlag("mm2_lab").unlessFlag("mm2_gorillas").flag("mm2_gorillas")
                .say("* Nieve and you drive the tortured gorillas out of the Gnome Stronghold. Garkor has found Glough's cave.") // OSRS: kill 4 tortured gorillas
                .kill("mm2_glough").needFlag("mm2_gorillas").say("* Glough falls at last. His gorilla army will never fly.")
                .stage("I should report to King Narnode.")
                .talk("king_narnode").say("Glough is dead and Kruk with him. Take this royal seed pod; it will always bring you home to the Grand Tree.")
                .done("garkor", "The 10th squad will remember you.");
    }

    private static void dragonSlayerII(QuestBook b) {
        b.npc("alec_kincade", "Alec Kincade", "myths_guild", 0, 3, 0xE0B8A0, 0x2A3A2A, 0x1A2A1A, 0x8A8A8C);
        b.npc("dallas_jones", "Dallas Jones", "varrock_museum", 3, 2, 0xE8C0AE, 0x8A6A3A, 0x4A3A2A, 0x6A4A1A);
        b.npc("robert_the_strong", "Robert the Strong", "myths_guild", -3, 3, 0xD0C8B8, 0xC0C0C9, 0x5A5A6A, 0xE0E0E3);
        b.item("dragonkin_key", "Dragonkin key", "key", 0x8A2A2A, "An ancient key of the dragonkin.");
        b.item("ancient_mural_map", "Map of Ungael", "note", 0xC0B080, "A map copied from a dragonkin mural.");
        b.boss(BossDef.of("ds2_vorkath", "Vorkath").level(392).stats(750, 560, 308, 214, 150, 308).speed(5)
                .defenceBonus(26, 108, 108, 240, 26)
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 73, 12, 6).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .attack(AttackDef.of("venomous_bolt", AttackType.RANGED, 32, 12, 4).special(Special.VENOM, 0))
                .attack(AttackDef.of("bite", AttackType.STAB, 32, 3, 3))
                .attack(AttackDef.of("firebomb", AttackType.MAGIC, 121, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Vorkath launches a fiery bomb high into the air!").particle("minecraft:flame"))
                .drops(Drop.always("superior_dragon_bones")).size(2.4f, 2.0f, 1.0f).at("quest_ungael").arena(14, "ice"));
        b.boss(BossDef.of("galvek", "Galvek").level(608).stats(1200, 632, 350, 350, 250, 1).speed(5)
                .defenceBonus(100, 120, 120, 200, 200)
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 50, 12, 6).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .attack(AttackDef.of("fire_bomb", AttackType.MAGIC, 60, 12, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Galvek hurls a fireball into the sky!").particle("minecraft:flame"))
                .attack(AttackDef.of("tail_swipe", AttackType.CRUSH, 40, 4, 3).special(Special.KNOCKBACK, 3))
                .phase(75, "Galvek takes to the air: the sea churns! (Phase 2 - water)", 1.1, false,
                        AttackDef.of("tsunami", AttackType.MAGIC, 40, 14, 4).special(Special.HAZARD, 8).telegraph(3, "A tsunami crashes towards you!"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 50, 12, 6).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .phase(50, "Galvek's scales crack with lightning! (Phase 3 - air)", 1.2, false,
                        AttackDef.of("lightning", AttackType.MAGIC, 45, 14, 5).special(Special.SKY_STRIKE, 0).telegraph(2, "Lightning gathers above you!"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 50, 12, 4).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .phase(25, "Galvek erupts in flame! (Phase 4 - earth)", 1.35, false,
                        AttackDef.of("earthquake", AttackType.CRUSH, 35, 10, 4).special(Special.AOE, 6).telegraph(2, "The ground shakes violently!"),
                        AttackDef.of("dragonfire", AttackType.MAGIC, 55, 12, 5).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .drops(Drop.always("superior_dragon_bones")).size(2.6f, 2.2f, 1.0f).at("galveks_sea").arena(16, "water"));
        b.item("ds2_key_karamja", "Dragon key piece (Karamja)", "key", 0x8A2A2A, "A piece of the dragon key, from the maze under the Kharazi Jungle.");
        b.item("ds2_key_morytania", "Dragon key piece (Morytania)", "key", 0x8A2A2B, "A piece of the dragon key, dug up in Mort Myre Swamp.");
        b.item("ds2_key_kourend", "Dragon key piece (Kourend)", "key", 0x8A2A2C, "A piece of the dragon key, from a tomb in Great Kourend.");
        b.item("ds2_key_ungael", "Dragon key piece (Ungael)", "key", 0x8A2A2D, "A piece of the dragon key, from Vorkath's stone chest.");
        b.item("ds2_locator_orb", "Locator orb", "orb", 0x3A8A5A, "Ava's dragonstone locator. It hurts to use.");
        b.spot("ds2_mort_myre", "Mort Myre Swamp", 3440, 3400); // approx: the buried piece is at a random swamp tile in OSRS
        b.spot("ds2_kourend_tomb", "Kourend crypt", 1563, 3580); // approx
        b.quest("dragon_slayer_ii", "Dragon Slayer II", Difficulty.GRANDMASTER, 5)
                .xp(Skill.SMITHING, 80000).xp(Skill.MINING, 60000).xp(Skill.AGILITY, 50000).xp(Skill.THIEVING, 50000) // OSRS (current quest XP rewards)
                .qp(200).reqQuest("legends_quest", "animal_magnetism", "ghosts_ahoy", "bone_voyage")
                .reqQuest("dream_mentor", "a_tail_of_two_cats", "client_of_kourend")
                .req(Skill.MAGIC, 75).req(Skill.SMITHING, 70).req(Skill.MINING, 68).req(Skill.CRAFTING, 62)
                .req(Skill.AGILITY, 60).req(Skill.THIEVING, 60).req(Skill.CONSTRUCTION, 50).req(Skill.HITPOINTS, 50)
                .unlock("Access to the Myths' Guild and Vorkath").unlock("Ability to smith dragon platebodies and kiteshields")
                .stage("I can start this quest by speaking to Alec Kincade outside the Myths' Guild. (Requires 200 quest points, Legends' Quest and much more.)")
                .talk("alec_kincade").say("Dragons are stirring all over Gielinor. Something is uniting them. An archaeologist, Dallas Jones, has been asking questions.")
                .stage("I should speak to Dallas Jones.")
                .talk("dallas_jones").say("A dragonkin journal! It mentions a house on a hill on Fossil Island... and a dragon named Galvek.")
                // OSRS: the House on the Hill's map puzzle, then Lithkren with Dallas (Aivas' diary and the mural).
                .stage("I should search the House on the Hill on Fossil Island, then sail to Lithkren with Dallas Jones.")
                .reach("fossil_island", 12).stay().unlessFlag("ds2_house").flag("ds2_house")
                .say("* In the House on the Hill you piece the torn map together. Jardric at the museum camp recognises the island on it: Lithkren.")
                .talk("dallas_jones").needFlag("ds2_house").give("ancient_mural_map")
                .say("* On Lithkren you find Aivas' skeleton and his diary. A dragonkin mural shows a sealed vault, opened only by a dragon key...",
                        "The key was split in four: Karamja, Morytania, Kourend and a frozen island in the north. Ungael!")
                .missing("Find out where that map leads first. The House on the Hill, on Fossil Island.")
                // OSRS: the four dragon key pieces (Kharazi maze, Mort Myre via Reldo's research and Ava's locator orb, a Kourend tomb, Ungael).
                .stage("I need the four pieces of the dragon key: the Kharazi maze, Mort Myre Swamp, a Kourend tomb, and Ungael beyond Rellekka.")
                .reach("kharazi_jungle", 12).stay().unless("ds2_key_karamja").unlessFlag("ds2_forged").reqSkill(Skill.THIEVING, 60).reqSkill(Skill.AGILITY, 60)
                .give("ds2_key_karamja")
                .say("* Down the stairs on the Kharazi coast, past spikes and coloured golems, you take a key piece from the centre of the maze.")
                .talk("reldo").stay().unlessFlag("ds2_reldo").flag("ds2_reldo")
                .say("A dragon key piece in Morytania? This book mentions it: buried in Mort Myre. Sarah in Port Phasmatys says Ava could make a locator.")
                .talk("ava").stay().needFlag("ds2_reldo").unless("ds2_locator_orb", "ds2_key_morytania").unlessFlag("ds2_forged")
                .take("dragonstone", "molten_glass:2").need("glassblowing_pipe", "chisel").give("ds2_locator_orb")
                .say("* You blow an inert orb around the dragonstone. Ava charges it into a locator orb.")
                .missing("A locator orb? Bring a dragonstone, 2 molten glass, a glassblowing pipe and a chisel.")
                .reach("ds2_mort_myre", 16).stay().need("ds2_locator_orb", "spade").unless("ds2_key_morytania").unlessFlag("ds2_forged").give("ds2_key_morytania")
                .say("* The locator stings you each time you feel it, but it leads you to a patch of swamp. You dig up a key piece.")
                .reach("ds2_kourend_tomb", 10).stay().unless("ds2_key_kourend").unlessFlag("ds2_forged").give("ds2_key_kourend")
                .say("* You search the old tomb and find a dragon key piece.")
                .talk("brundt").need("ancient_mural_map").travel("quest_ungael").say("The dragonkin fortress in the frozen north? Torfinn will sail you. *Your longship lands on Ungael.*")
                .stage("I must defeat the undead dragon Vorkath on Ungael.")
                .kill("ds2_vorkath").take("ancient_mural_map").give("ds2_key_ungael").say("* Vorkath collapses. In the stone chest you find the last dragon key piece.")
                .stage("I should reforge the dragon key on Lithkren, open the dragonkin vault and tell Alec Kincade.")
                .reach("fossil_island", 12).stay().unlessFlag("ds2_forged").take("ds2_key_karamja", "ds2_key_morytania", "ds2_key_kourend", "ds2_key_ungael")
                .need("hammer").reqSkill(Skill.SMITHING, 70).flag("ds2_forged").give("dragonkin_key")
                .say("* On Lithkren's dragonkin anvil you forge the four pieces into the dragon key.")
                .missing("* The dragonkin anvil waits on Lithkren. You'll need all four key pieces, a hammer and 70 Smithing.")
                .talk("alec_kincade").take("dragonkin_key").say("The key opens the dragonkin vault! But Galvek has risen and leads the dragons against us. Robert the Strong, the hero of old, has returned to fight beside you.")
                .stage("I must defeat Galvek, the dragon of the dragonkin, on the Western Sea.")
                .talk("robert_the_strong").stay().say("Together, as I fought beside Camorra of old! To the ships!")
                // OSRS assault on the Myths' Guild: green, blue, black, steel, brutal red, mithril, adamant and rune dragons, then Galvek.
                // Wave 1: two green and two blue dragons battling the Varrockian soldiers (checkpoint).
                .kill("green_dragon", 2).stay().unlessFlag("ds2_wave_green", "ds2_assault").flag("ds2_wave_green")
                .say("* The two green dragons fall beside the Varrockian soldiers.")
                .kill("blue_dragon", 2).stay().unlessFlag("ds2_wave_blue", "ds2_assault").flag("ds2_wave_blue")
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
                .stage("I should report to Alec Kincade at the Myths' Guild.")
                .talk("alec_kincade").say("Galvek is dead! You are a true slayer of dragons. The Myths' Guild is open to you.")
                .done("robert_the_strong", "It was an honour to fight beside you.");
    }

    private static void whileGuthixSleeps(QuestBook b) {
        // Ivy Sophista and Thaerisk Cemphier in the house with the ladder in Taverley (Thaerisk upstairs in OSRS), Akrisae and Idria in
        // the White Knights' Castle, the Khazard launderer by the Fight Arena, a shady stranger by Draynor bank and Silif in the Black
        // Knights' catacombs (quest-helper tiles; Silif stands at the fortress door).
        b.npcAt("ivy_sophista", "Ivy Sophista", 2907, 3450, 0xF0D0BC, 0x3A6A9A, 0x2A4A6A, 0xE0A041);
        b.npcAt("thaerisk", "Thaerisk Cemphier", 2909, 3449, 0xE8C8B0, 0xE0E0F1, 0x3A3A6A, 0xC0A062);
        b.npcAt("akrisae", "Akrisae", 2989, 3342, 0xE8C0AF, 0xE0E0F0, 0x6A6AA0, 0xC0A061);
        b.npcAt("idria", "Idria", 2989, 3339, 0xF0D0B0, 0x5A3A6A, 0x3A2A4A, 0x2A1A10);
        b.npcAt("khazard_launderer", "Khazard Launderer", 2564, 3172, 0xD8B090, 0xE0E0D8, 0x6A5A4A, 0x8A6A4A);
        b.npcAt("shady_stranger", "Shady stranger", 3085, 3243, 0xC8A080, 0x2A2A30, 0x1A1A20, 0x101014);
        b.npcAt("silif", "Silif", 3017, 3512, 0xE0B8A0, 0x8A8A90, 0x4A4A50, 0xC0A063);
        b.spot("wgs_pit_trap", "Feldip Hills pit trap", 2499, 2910); // west of the Hunting expert
        b.spot("wgs_bettys_counter", "Betty's counter", 3013, 3258);
        b.spot("wgs_snapdragon_patch", "White Knights' Castle snapdragon patch", 2962, 3338); // top of the castle
        b.item("movarios_notes", "Movario's notes", "book", 0x3A2A4A, "Notes revealing the location of Guthix's resting place.");
        b.item("dirty_shirt", "Dirty shirt", "cloth", 0xB0A890, "One of Movario's shirts. It smells.");
        b.item("broav", "Broav", "creature", 0x7A5A3A, "A broav trained to follow a scent.");
        b.item("wgs_teleorb", "Teleorb", "orb", 0x8AD0E0, "Akrisae's teleorb: it swaps its bearer into the White Knights' cells.");
        b.item("enriched_snapdragon_seed", "Enriched snapdragon seed", "seed", 0xE06A9A, "A snapdragon seed bathed in rose-tinted light.");
        // Wiki: level 167, 275 HP, ranged up to 20 every 5 ticks; every 12 seconds one yells "Now!" and looses a glowing arrow
        // (21-40) while the other teleports in with a dragon dagger special (up to 72).
        b.boss(BossDef.of("wgs_assassin", "Assassin").level(167).stats(275, 50, 70, 70, 30, 110).speed(5)
                .defenceBonus(10, 10, 15, 30, 20)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 20, 8, 6))
                .attack(AttackDef.of("glowing_arrow", AttackType.RANGED, 40, 8, 1).telegraph(2, "Assassin: Now!"))
                .attack(AttackDef.of("dagger_special", AttackType.STAB, 72, 10, 1).special(Special.BLINK, 0)
                        .telegraph(2, "Assassin: I'm on it!"))
                .drops(Drop.always("bones")).skin(0xC8A080, 0x2A2A2A, 0x1A1A1A, 0x3A1A1A));
        // Wiki: level 138, 90 HP, slashes up to 13 every 6 ticks.
        b.boss(BossDef.of("elite_black_knight", "Elite Black Knight").level(138).stats(90, 90, 120, 90, 70, 20).speed(6)
                .defenceBonus(110, 130, 40, -5, 135)
                .attack(AttackDef.of("slash", AttackType.SLASH, 13, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xD8B090, 0x1A1A1A, 0x101010, 0x2A2A2A));
        // Wiki (Surok Magis, Dagon'hai): level 265, 450 HP, magic only (QuestBossScripts.MagicOnly), every seventh attack a special:
        // elemental surges or an explosive orb (45+). The henchmen special (bind/weaken counters) is not modelled; regular max hit approx.
        b.boss(BossDef.of("dark_squall", "Surok Magis").level(265).stats(450, 1, 1, 160, 160, 1).speed(5)
                .defenceBonus(30, 20, 40, 0, 20)
                .attack(AttackDef.of("chaos_bolt", AttackType.MAGIC, 25, 12, 6).particle("minecraft:witch"))
                .attack(AttackDef.of("elemental_surge", AttackType.MAGIC, 45, 12, 1).telegraph(2, "Surok Magis: Witness true sorcery!"))
                .attack(AttackDef.of("explosive_orb", AttackType.MAGIC, 45, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Surok throws an explosive orb at your feet!"))
                .drops(Drop.always("bones")).skin(0xD8C0C0, 0x1A1A2A, 0x101018, 0x6A1A1D));
        // Wiki: level 524, 750 HP (275 in every stat but 286 Strength), 40 in each form every 6 ticks; it prays against the
        // player's style from 50% and attacks harder from 25%.
        b.boss(BossDef.of("balance_elemental", "Balance Elemental").level(524).stats(750, 275, 286, 275, 275, 275).speed(6)
                .defenceBonus(5, 0, -40, -40, 0)
                .attack(AttackDef.of("melee_form", AttackType.CRUSH, 40, 3, 4))
                .attack(AttackDef.of("ranged_form", AttackType.RANGED, 40, 10, 4))
                .attack(AttackDef.of("magic_form", AttackType.MAGIC, 40, 10, 4).particle("minecraft:enchant"))
                .phase(50, "The Balance Elemental begins praying against your attacks!", 1.0, false)
                .phase(25, "The Balance Elemental shifts faster and hits harder!", 1.25, false)
                .drops(Drop.always("coins")).size(1.2f, 2.6f, 1.0f).at("guthix_sanctum").arena(12, "void"));
        b.quest("while_guthix_sleeps", "While Guthix Sleeps", Difficulty.GRANDMASTER, 5)
                .reqQuest("defender_of_varrock", "the_path_of_glouphrie", "fight_arena", "dream_mentor", "the_hand_in_the_sand", "wanted", "temple_of_the_eye", "a_tail_of_two_cats", "tears_of_guthix", "nature_spirit")
                .reqNote("Access to the Warriors' Guild (130 combined Attack + Strength, or 99 in either)")
                .xp(Skill.THIEVING, 80000).xp(Skill.FARMING, 75000).xp(Skill.HERBLORE, 75000).xp(Skill.HUNTER, 50000) // OSRS (current quest XP rewards)
                .qp(180)
                .req(Skill.THIEVING, 72).req(Skill.MAGIC, 67).req(Skill.AGILITY, 66).req(Skill.FARMING, 65)
                .req(Skill.HERBLORE, 65).req(Skill.HUNTER, 62).req(Skill.FIREMAKING, 49) // 49 Firemaking: sapphire lantern
                .stage("I can start this quest by speaking to Ivy Sophista in Taverley. (Requires 180 quest points and much more.)")
                .talk("ivy_sophista").say("Lucien seeks the resting place of Guthix himself. The Temple Knights need an agent.",
                        "Thaerisk Cemphier will brief you. He's here in the house.")
                // Wiki: Thaerisk and the two assassins; the Khazard launderer (500 coins); a broav from the Hunting expert's pit trap;
                // the broken table on the Khazard battlefield; Movario's base (rune door, wires, ruby key, pressure gauge).
                .stage("I should speak to Thaerisk, then track Lucien's man Movario: the Khazard launderer by the Fight Arena, a broav from "
                        + "the Feldip Hills and his base under the Khazard battlefield.")
                .talk("thaerisk").stay().unlessFlag("wgs_assassins").spawn("wgs_assassin")
                .say("?!wgs_assassin1|Lucien means to find Guthix and take his power. His agent, Movario, is our only lead...",
                        "?!wgs_assassin1|* Two assassins burst in! Pray against their arrows, but watch for the dagger.",
                        "?wgs_assassin1|The other assassin is still about! Deal with him!")
                .hint("thaerisk", "Movario has his laundry done by the Fight Arena. Start with the Khazard launderer.")
                .hint("ivy_sophista", "Thaerisk knows more about Lucien's plans than I do. Help him.")
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
                .reach("wgs_pit_trap", 4).stay().needFlag("wgs_shirt").unlessFlag("wgs_trapdoor").unless("broav").need("logs", "knife")
                .reqSkill(Skill.HUNTER, 62).give("broav")
                .say("* The Hunting expert suggests a broav. You set a pit trap west of her camp, bait it and catch an unconscious broav,",
                        "* which she trains to follow a scent.")
                .missing("* The Hunting expert says a broav could track a person. A pit trap needs logs and a knife.")
                .reach("movarios_base", 4).stay().needFlag("wgs_shirt").unlessFlag("wgs_trapdoor").need("broav", "dirty_shirt")
                .flag("wgs_trapdoor")
                .say("* You drop the broav and let it sniff the dirty shirt. It squeals at a broken table: beneath it, a trapdoor!",
                        "* You climb down into Movario's base and take the runes from the open chest.")
                .missing("* A broken table stands in a run-down house. Something's odd about it, but you'd need a nose to find out what.")
                .puzzle("movarios_base", WGS_DOOR).reqSkill(Skill.HUNTER, 62).needFlag("wgs_trapdoor").unlessFlag("wgs_door").flag("wgs_door").stay()
                .say("* The rune's power flows into the door's lock and it creaks open.")
                .puzzle("movarios_base", WGS_WEIGHTS).needFlag("wgs_door").unlessFlag("wgs_weights").flag("wgs_weights").stay()
                .say("* The gauge settles on your weight: the floor beneath the study stops humming and the way upstairs is safe.")
                .reach("movarios_base", 10).needFlag("wgs_door", "wgs_weights").give("movarios_notes")
                .say("* From the desk and the trapped bed chest you take both volumes of Movario's notes. They point to Guthix's sanctum, deep below.")
                .missing("* Movario's base is sealed by a door marked PROHIBITED and a pressure-gauged study.")
                .stage("I should take Movario's notes back to Thaerisk in Taverley.")
                .talk("thaerisk").take("movarios_notes").say("Guthix's sanctum! Lucien and his generals are already moving.",
                        "* Thaerisk sends you to rescue Idria from Lucien's mercenaries north of McGrubor's Wood; she joins you at the White "
                                + "Knights' Castle.")
                .missing("Movario's notes: did you find them in his base?")
                // Wiki: Akrisae's teleorb on a shady stranger, the enriched snapdragon (Betty's rose-tinted lens), the super truth serum
                // and the suspect sketch, then the Dark Squall's base under the Black Knights' Fortress, Silif and Surok Magis. NPC Contact
                // with the slayer masters and the Warriors' Guild recruits are narrated (approx).
                .stage("I should help Akrisae unmask Lucien's spy network: plant a teleorb on a spy, grow an enriched snapdragon for a truth "
                        + "serum, and infiltrate the Dark Squall's base under the Black Knights' Fortress.")
                .talk("akrisae").stay().unlessFlag("wgs_teleorb").unless("wgs_teleorb").give("wgs_teleorb").travel("DRAYNOR_VILLAGE")
                .say("Lucien's spies are everywhere. Plant this teleorb on one of them and it will bring him straight to our cells.",
                        "Shady strangers have been seen around Draynor Village. I'll send you there.")
                .talk("shady_stranger").stay().unlessFlag("wgs_teleorb").take("wgs_teleorb").flag("wgs_teleorb")
                .say("What do you want? Move along.", "* You slip the teleorb into his pocket. With a flash, he's gone.")
                .talk("akrisae").stay().needFlag("wgs_teleorb").unlessFlag("wgs_seed").give("snapdragon_seed").flag("wgs_seed")
                .travel("PORT_SARIM")
                .say("The spy is in our cell, but he won't talk. Thaerisk can brew a truth serum, if it's strong enough.",
                        "@Thaerisk Cemphier: Take this snapdragon seed. Betty in Port Sarim can help you enrich it. I'll send you there.")
                .reach("wgs_bettys_counter", 3).stay().needFlag("wgs_seed").unlessFlag("wgs_planted").unless("enriched_snapdragon_seed")
                .take("snapdragon_seed", "coins:20").give("enriched_snapdragon_seed")
                .say("* Betty sells you some pink dye for 20 coins. You tint a lantern lens rose and, standing in her doorway, focus the",
                        "* light onto the seed on her counter. It glows: an enriched snapdragon seed.")
                .missing("* Betty can enrich a snapdragon seed (and sells pink dye for 20 coins).")
                .reach("wgs_snapdragon_patch", 4).stay().needFlag("wgs_seed").unlessFlag("wgs_planted").take("enriched_snapdragon_seed")
                .need("seed_dibber").flag("wgs_planted")
                .say("* At the top of the castle you plant the enriched snapdragon seed in the special patch. It sprouts at once.")
                .missing("* The special patch at the top of the castle is ready for an enriched snapdragon seed (and a seed dibber).")
                .talk("akrisae").stay().needFlag("wgs_planted").unlessFlag("wgs_sketch").flag("wgs_sketch")
                .say("* You harvest the enriched snapdragon and add it to Thaerisk's truth serum. In the cell, the shady stranger talks, and",
                        "* you draw a sketch of his master from charcoal and papyrus.",
                        "@Idria: The Dark Squall! Lucien's spymaster. And the masters of the slayer guilds and the Warriors' Guild will stand with us.",
                        "Disguise yourself in Black Knight armour (a bronze med helm and an iron chainbody will do) and find his base under their "
                                + "fortress.")
                .talk("silif").stay().needFlag("wgs_sketch").unlessFlag("wgs_elite").need("bronze_med_helm", "iron_chainbody")
                .spawn("elite_black_knight")
                .say("* Disguised, you push the hidden wall, charge an orb on the carved tile and drop into the Black Knight catacombs.",
                        "* Silif, a Temple Knight, lies starving in a locked cell. An elite black knight spots you!")
                .missing("* The guards won't let you near the fortress unless you look like a Black Knight: a bronze med helm and an iron chainbody.")
                .kill("elite_black_knight").stay().needFlag("wgs_sketch").unlessFlag("wgs_elite").flag("wgs_elite")
                .say("* In its elite black armour you walk into the Dark Squall's base: a spare set of armour, the Dark Squall's robes,",
                        "* a lobster, a restore potion and the key to Silif's cell.")
                .talk("silif").stay().needFlag("wgs_elite").unlessFlag("wgs_silif", "wgs_surok").flag("wgs_silif").spawn("dark_squall")
                .say("* You feed Silif the lobster and the restore potion and give him the spare armour.",
                        "Thank you, friend. Those maps on the wall... Lucien's camp. Take this teleorb, and beware: the Dark Squall is upstairs.",
                        "* You climb the ladder. The Dark Squall throws back his hood: Surok Magis! Only magic will touch him.")
                .talk("silif").stay().needFlag("wgs_silif").unlessFlag("wgs_surok").spawn("dark_squall")
                .say("The Dark Squall is still up there! Use magic: nothing else will touch Surok.")
                .kill("dark_squall").stay().needFlag("wgs_elite").unlessFlag("wgs_surok").flag("wgs_surok")
                .say("* Surok falls. You plant Silif's teleorb on him and he vanishes into the White Knights' cells.",
                        "* In the Dark Squall's robes you use his strange teleorb and spy on Lucien's camp from the Chaos Temple's roof:",
                        "* the Mahjarrat gather at Guthix's resting place!")
                .talk("akrisae").needFlag("wgs_surok")
                .say("Surok, the Dark Squall? And Lucien is at the Chasm of Tears already. Idria will lead the Temple Knights: go, now.")
                .missing("?!wgs_teleorb|Plant the teleorb on one of Lucien's spies in Draynor Village.",
                        "?wgs_teleorb|?!wgs_planted|Thaerisk's serum needs an enriched snapdragon from the castle's special patch.",
                        "?wgs_planted|?!wgs_surok|Find the Dark Squall's base under the Black Knights' Fortress.")
                // saves from before round 33 reached this stage without the assassins' flag: the old briefing goes on to the chasm
                .talk("ivy_sophista").unlessFlag("wgs_assassins")
                .say("The Mahjarrat are converging on the sanctum. Something is guarding the way: a creature of pure balance.")
                .stage("I must open the way through the Chasm of Tears and defeat the Balance Elemental guarding Guthix's sanctum.")
                .puzzle("guthix_sanctum", WGS_SKULLS).unlessFlag("wgs_skulls").flag("wgs_skulls").stay()
                .say("* The last block clicks into place and the stone cube in the fourth skull grinds open.")
                .kill("balance_elemental").needFlag("wgs_skulls")
                .say("* The Balance Elemental dissolves. Guthix stirs... and the Temple Knights drive back the Mahjarrat.")
                .missing("* The way into the sanctum is sealed by the three skulls' mechanisms.")
                // Wiki: Idria teleports the player back to the White Knights' Castle and ends the quest there. The two tormented demons
                // fought beside the Guardians of Armadyl after the Balance Elemental are not modelled.
                .stage("I should report to Idria at the White Knights' Castle.")
                .talk("idria").say("Guthix sleeps on, and Lucien has been stopped... for now. Gielinor owes you more than it will ever know.");
    }

    /**
     * While Guthix Sleeps, Movario's base (wiki): one letter of "PROHIBITED" on the battered door is drawn as a rune's
     * symbol, different for every player, and that rune opens it. The symbol descriptions and which letter is styled
     * are approx.
     */
    static final QuestPuzzle WGS_DOOR = wgsDoor();
    /** The study's pressure gauge reads the player's weight on entering; they must weigh that again (1, 2 and 5 kg weights). */
    static final QuestPuzzle WGS_WEIGHTS = wgsWeights();
    /** The Chasm of Tears: an elemental block in each of three skulls' mechanisms, matching symbols individual to each player. */
    static final QuestPuzzle WGS_SKULLS = wgsSkulls();

    private static QuestPuzzle wgsDoor() {
        String[] runes = {"Air", "Water", "Earth", "Fire", "Mind", "Body", "Cosmic", "Chaos", "Nature", "Law", "Death", "Blood"};
        String[] symbols = {"a pale swirl", "a blue droplet", "a brown clod", "a red flame", "an orange-ringed eye", "a blue figure", "a yellow star",
                "an orange burst", "a green leaf", "a blue diamond", "a white skull", "a red drop"};
        String[] letters = {"P", "R", "O", "H", "I", "B", "T", "E", "D"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("wgs_door").room("door");
        for (int r = 0; r < runes.length; r++) {
            b.variant(null).prompt("* An old battered door. Painted across it: PROHIBITED.",
                            "* Looking closely, the " + letters[r % letters.length] + " is drawn as " + symbols[r] + ". Which rune do you use on the door?")
                    .options(runes).solution(r);
        }
        return b.failLines("* Nothing happens.").build();
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
}
