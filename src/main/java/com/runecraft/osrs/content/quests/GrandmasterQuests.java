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
        b.spot("temple_knight_hq", "Temple Knight headquarters", 2962, 3382);
        b.place("movarios_base", "Movario's base", 3240, 3170, PlaceDef.Style.CAVE);
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
        b.npc("akrisae", "Akrisae", "temple_knight_hq", 0, 0, 0xE8C0AF, 0xE0E0F0, 0x6A6AA0, 0xC0A061);
        b.npc("ivy_sophista", "Ivy Sophista", "temple_knight_hq", 3, 2, 0xF0D0BC, 0x3A6A9A, 0x2A4A6A, 0xE0A041);
        b.item("movarios_notes", "Movario's notes", "book", 0x3A2A4A, "Notes revealing the location of Guthix's resting place.");
        b.boss(BossDef.of("balance_elemental", "Balance Elemental").level(454).stats(800, 300, 300, 250, 300, 300).speed(4)
                .defenceBonus(150, 150, 150, 150, 150)
                .attack(AttackDef.of("melee_form", AttackType.CRUSH, 40, 3, 4))
                .attack(AttackDef.of("ranged_form", AttackType.RANGED, 40, 10, 4))
                .attack(AttackDef.of("magic_form", AttackType.MAGIC, 40, 10, 4).particle("minecraft:enchant"))
                .phase(66, "The Balance Elemental shifts its form!", 1.1, false)
                .phase(33, "The Balance Elemental shifts its form again!", 1.2, false)
                .drops(Drop.always("coins")).size(1.2f, 2.6f, 1.0f).at("guthix_sanctum").arena(12, "void"));
        b.quest("while_guthix_sleeps", "While Guthix Sleeps", Difficulty.GRANDMASTER, 5)
                .reqQuest("defender_of_varrock", "the_path_of_glouphrie", "fight_arena", "dream_mentor", "the_hand_in_the_sand", "wanted", "temple_of_the_eye", "a_tail_of_two_cats", "tears_of_guthix", "nature_spirit")
                .reqNote("Access to the Warriors' Guild (130 combined Attack + Strength, or 99 in either)")
                .xp(Skill.THIEVING, 80000).xp(Skill.FARMING, 75000).xp(Skill.HERBLORE, 75000).xp(Skill.HUNTER, 50000) // OSRS (current quest XP rewards)
                .qp(180)
                .req(Skill.THIEVING, 72).req(Skill.MAGIC, 67).req(Skill.AGILITY, 66).req(Skill.FARMING, 65)
                .req(Skill.HERBLORE, 65).req(Skill.HUNTER, 62).req(Skill.FIREMAKING, 49) // 49 Firemaking: sapphire lantern
                .stage("I can start this quest by speaking to Akrisae at the Temple Knight headquarters in Falador. (Requires 180 quest points and much more.)")
                .talk("akrisae").say("Lucien seeks the resting place of Guthix himself. The Temple Knights need an agent to infiltrate his followers.")
                .stage("I should find the Zamorakian Movario's base beneath the Lumbridge Swamp.")
                .reach("movarios_base", 10).give("movarios_notes").say("* Past deadly traps you find Movario's notes. They point to Guthix's sanctum, deep below.")
                .stage("I should show Movario's notes to Akrisae.")
                .talk("akrisae").take("movarios_notes").say("Guthix's sanctum! Lucien and his generals are already moving. Go, and hurry. Ivy will brief you.")
                .stage("I should speak to Ivy Sophista, then descend to Guthix's sanctum.")
                .talk("ivy_sophista").say("The Mahjarrat are converging on the sanctum. Something is guarding the way: a creature of pure balance.")
                .stage("I must defeat the Balance Elemental guarding Guthix's sanctum.")
                .kill("balance_elemental").say("* The Balance Elemental dissolves. Guthix stirs... and the Temple Knights drive back the Mahjarrat.")
                .stage("I should report to Akrisae.")
                .talk("akrisae").say("Guthix sleeps on, and Lucien has been stopped... for now. Gielinor owes you more than it will ever know.");
    }
}
