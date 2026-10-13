package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.data.Drop;

/** Misthalin, Asgarnia and Kandarin members' quests (Lost City, Waterfall, Arthurian quests, Ardougne quests...). */
final class AsgarniaKandarinQuests {
    private AsgarniaKandarinQuests() {
    }

    static void register(QuestBook b) {
        places(b);
        lostCity(b);
        waterfall(b);
        merlinsCrystal(b);
        holyGrail(b);
        familyCrest(b);
        heroesQuest(b);
        sheepHerder(b);
        plagueCity(b);
        biohazard(b);
        fightArena(b);
        hazeelCult(b);
        clockTower(b);
        witchsHouse(b);
    }

    private static void places(QuestBook b) {
        b.place("lumbridge_swamp_camp", "Adventurers' camp", 3150, 3206, PlaceDef.Style.CAMP);
        b.spot("zanaris_shed", "Lumbridge Swamp shed", 3202, 3169);
        // Zanaris is an instanced realm in OSRS (2412, 4434); it is placed beside the shed that leads to it.
        b.places.add(PlaceDef.town("quest_zanaris", "Zanaris", 3168, 3158, Station.BANK_BOOTH, Station.GENERAL_STORE));
        b.place("entrana", "Entrana", 2834, 3335, PlaceDef.Style.TOWN);
        b.spot("almeras_house", "Almera's house", 2521, 3495);
        b.spot("hadleys_house", "Hadley's house", 2518, 3429);
        b.spot("glarials_tomb", "Glarial's tomb", 2557, 3444);
        b.place("baxtorian_falls", "Baxtorian Falls", 2511, 3463, PlaceDef.Style.RUINS);
        b.place("tree_gnome_village", "Tree Gnome Village", 2525, 3165, PlaceDef.Style.CAMP);
        b.place("keep_le_faye", "Keep Le Faye", 2768, 3402, PlaceDef.Style.RUINS);
        b.spot("lady_of_the_lake", "Lake near Taverley", 2924, 3404);
        b.place("fisher_realm", "Fisher Realm", 2805, 3080, PlaceDef.Style.RUINS);
        b.spot("galahads_house", "Galahad's house", 2612, 3475);
        b.spot("jolly_boar_inn", "Jolly Boar Inn", 3279, 3500);
        b.place("edgeville_dungeon_chronozon", "Edgeville Dungeon (Chronozon)", 3100, 3510, PlaceDef.Style.CAVE);
        b.spot("quest_heroes_guild", "Heroes' Guild", 2902, 3510);
        b.place("ice_queens_lair", "Ice Queen's lair", 2866, 3520, PlaceDef.Style.CAVE);
        b.spot("ardougne_wall", "Ardougne wall", 2616, 3300);
        b.place("west_ardougne", "West Ardougne", 2530, 3305, PlaceDef.Style.RUINS);
        b.spot("mourner_hq", "Mourner headquarters", 2550, 3326);
        b.spot("rimmington_chemist", "Rimmington chemist", 2932, 3210);
        b.place("fight_arena", "Fight Arena", 2585, 3160, PlaceDef.Style.ARENA);
        b.spot("servil_house", "Servil house", 2565, 3196);
        b.spot("carnillean_house", "Carnillean house", 2570, 3270);
        b.place("hazeel_cult_hideout", "Hazeel cult hideout", 2565, 3242, PlaceDef.Style.CAVE);
        b.place("clock_tower", "Clock Tower", 2568, 3248, PlaceDef.Style.RUINS);
        b.place("witchs_house", "Witch's house", 2900, 3474, PlaceDef.Style.RUINS);
    }

    // ---------------------------------------------------------------- Lost City
    private static void lostCity(QuestBook b) {
        b.npc("lc_warrior", "Warrior", "lumbridge_swamp_camp", 2, 1, 0xE0B898, 0x8A8A9A, 0x5A5A6A, 0x6A4A2A);
        b.npc("lc_archer", "Archer", "lumbridge_swamp_camp", -2, 1, 0xF0C8A8, 0x3A6A2A, 0x5A4A2A, 0x3A5A2A);
        b.npc("lc_monk", "Monk", "lumbridge_swamp_camp", 0, -2, 0xD8B090, 0x8A6A3A, 0x8A6A3A, 0x3A2A1A);
        b.npc("lc_wizard", "Wizard", "lumbridge_swamp_camp", 2, -2, 0xE8D0B8, 0x3A3AAA, 0x2A2A8A, 0x3A3AAA);
        b.npc("shamus", "Shamus", "zanaris_shed", 3, 3, 0xC0D080, 0x2A8A2A, 0x1A5A1A, 0x2A8A2A);
        b.npc("monk_of_entrana", "Monk of Entrana", "PORT_SARIM", -6, -4, 0xE0C0A0, 0x9A7A4A, 0x9A7A4A, 0x9A7A4A);
        b.npc("cave_monk", "Cave monk", "entrana", 3, 0, 0xD8B898, 0x7A5A3A, 0x7A5A3A, 0x5A3A1A);
        b.npc("fairy_queen", "Fairy Queen", "quest_zanaris", 0, 3, 0xF0E0F0, 0xC0A0E0, 0xA080C0, 0xF0D060);
        b.item("dramen_branch", "Dramen branch", "log", 0x6A8A3A, "A branch from the magical Dramen tree.");
        b.boss(BossDef.of("tree_spirit", "Tree spirit").level(101).stats(85, 90, 80, 85, 1, 1).speed(5)
                .defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("branch_slam", AttackType.CRUSH, 13, 3, 8))
                .attack(AttackDef.of("root_snare", AttackType.CRUSH, 6, 7, 3).special(Special.FREEZE, 4)
                        .telegraph(2, "Roots burst from the ground!").particle("minecraft:composter"))
                .drops(Drop.always("logs")).size(1.0f, 2.4f, 1.0f).at("entrana").arena(10, "jungle"));
        b.quest("lost_city", "Lost City", Difficulty.INTERMEDIATE, 3)
                .req(Skill.CRAFTING, 31).req(Skill.WOODCUTTING, 36)
                .unlock("Access to Zanaris")
                .unlock("Ability to wield dragon longswords and dragon daggers")
                .stage("I can start this quest by speaking to the adventurers camping in Lumbridge Swamp. (Requires 31 Crafting, 36 Woodcutting.)")
                .talk("lc_warrior").say("We're searching for Zanaris, the lost city of the fairies!", "> Zanaris? Can I help?",
                        "@Monk: We know a leprechaun in these woods knows the way. They hide in trees, so look carefully.")
                .stage("I should find the leprechaun hiding in a tree in Lumbridge Swamp.")
                .talk("shamus").say("Ay, you caught me! Zanaris? You'll need a staff of Dramen wood to get in.",
                        "The Dramen tree grows in the caves beneath Entrana. Then enter the shed here in the swamp holding the staff.",
                        "* Shamus vanishes in a puff of green smoke.")
                .stage("I need to get to Entrana. The Monk of Entrana at Port Sarim takes passengers who carry no weapons or armour.")
                .talk("monk_of_entrana").travel("entrana").say("Entrana is a holy island. Are you carrying weapons or armour?", "> No.",
                        "Then step aboard. *The ship sails to Entrana.*")
                .stage("I should find the Dramen tree in the caves beneath Entrana. A tree spirit guards it.")
                .talk("cave_monk").stay().say("The Dramen tree lies below. Be warned: a tree spirit protects it.")
                .kill("tree_spirit").give("dramen_branch").say("* The tree spirit is defeated. You cut a branch from the Dramen tree.")
                .stage("I should fashion the branch into a Dramen staff with a knife.")
                .talk("cave_monk").need("knife").take("dramen_branch").give("dramen_staff").travel("PORT_SARIM")
                .say("You'll want to carve that branch with your knife. *You carve a Dramen staff.* Now, let me guide you back to the boat.")
                .missing("You'll need a knife to carve that branch into a staff.")
                .stage("Holding the Dramen staff, I should enter the shed in Lumbridge Swamp.")
                .reach("zanaris_shed", 5).need("dramen_staff").travel("quest_zanaris")
                .say("* You step into the shed holding the Dramen staff... and the world shimmers. You have found Zanaris!")
                .finish()
                .done("fairy_queen", "Welcome to Zanaris, adventurer. You may come and go with your Dramen staff.")
                .done("lc_warrior", "You found Zanaris? Incredible!")
                .done("lc_archer", "Zanaris! Who would have thought it was in a shed?")
                .done("lc_monk", "Saradomin guided you to the lost city.")
                .done("lc_wizard", "The magic of Zanaris is astonishing, isn't it?")
                .done("jukat", "Dragon blades, finest in all the realms.");
        // Jukat's Dragon Sword Shop in the Zanaris market: the only shop selling dragon longswords and daggers.
        b.npc("jukat", "Jukat", "quest_zanaris", 4, 0, 0x8A3A2A, 0x3A2A2A, 0x2A1A1A, 0xC08A2A);
        b.shop("jukat", "dragon_dagger", 30000, "lost_city", null, null, 0, "Dragon dagger? 30,000 coins. Finest steel in all the realms!");
        b.shop("jukat", "dragon_longsword", 100000, "lost_city", null, null, 0, "Dragon longsword? 100,000 coins. Finest steel in all the realms!");
    }

    // ---------------------------------------------------------------- Waterfall Quest
    private static void waterfall(QuestBook b) {
        b.npc("almera", "Almera", "almeras_house", 0, 0, 0xF0C8B0, 0x6A3A8A, 0x4A2A5A, 0xC08A3A);
        b.npc("hudon", "Hudon", "baxtorian_falls", 2, 3, 0xF0D0B0, 0x3A8A8A, 0x2A4A5A, 0xE0B050);
        b.npc("hadley", "Hadley", "hadleys_house", 0, 0, 0xE0C0A0, 0x8A6A3A, 0x4A4A3A, 0xA0A0A0);
        b.npc("golrie", "Golrie", "tree_gnome_village", -3, 3, 0xD0A070, 0x8A3A3A, 0x4A2A2A, 0x6A4A2A);
        b.item("book_on_baxtorian", "Book on Baxtorian", "book", 0x6A4A2A, "A book on the history of the elf king Baxtorian.");
        b.item("glarials_pebble", "Glarial's pebble", "gem", 0x8AB0C0, "A small pebble with elven inscriptions.");
        b.item("glarials_amulet", "Glarial's amulet", "amulet", 0x3AA0A0, "A bright green gem set in a necklace.");
        b.item("glarials_urn", "Glarial's urn", "pot", 0xC0B080, "An urn containing Glarial's ashes.");
        b.item("mithril_seeds", "Mithril seeds", "seed", 0x9090C0, "Magical seeds in a mithril case. Plant them to grow flowers.");
        b.quest("waterfall_quest", "Waterfall Quest", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.ATTACK, 13750).xp(Skill.STRENGTH, 13750).reward("diamond:2", "gold_bar:2", "mithril_seeds:40")
                .stage("I can start this quest by speaking to Almera in her house near the Baxtorian Falls.")
                .talk("almera").say("My son Hudon went treasure hunting at the falls. I'm so worried about him!", "> I'll find him.")
                .stage("I should find Hudon at the Baxtorian Falls.")
                .talk("hudon").say("I'm going to find the treasure of the falls! Don't you dare steal it!", "> Your mother is worried.", "Bah.")
                .stage("Hudon's no help. Hadley the tourist guide, south of the falls, might know about the treasure.")
                .talk("hadley").give("book_on_baxtorian").say("The falls? Legend says the elf king Baxtorian hid his treasure there. Here, read this book.")
                .stage("The book says the key to Glarial's tomb is held by Golrie, a gnome in the Tree Gnome Village dungeon.")
                .talk("golrie").take("book_on_baxtorian").give("glarials_pebble")
                .say("You freed me from this cell! Here, take this old pebble. My grandfather got it from an elf.")
                .stage("I should use Glarial's pebble on her tombstone near the falls.")
                .reach("glarials_tomb", 6).take("glarials_pebble").give("glarials_amulet", "glarials_urn")
                .say("* The pebble fits the tombstone. Inside the tomb you find Glarial's amulet and urn.")
                .stage("Wearing Glarial's amulet and carrying her urn, plus 6 air, water and earth runes, I should enter the waterfall dungeon.")
                .reach("baxtorian_falls", 6).need("glarials_amulet").take("glarials_urn", "air_rune:6", "water_rune:6", "earth_rune:6")
                .say("* The amulet opens the door beneath the falls. You place a rune on each pillar and lay Glarial's ashes on the chalice.",
                        "* The chalice fills with treasure: diamonds, gold bars and mithril seeds!")
                .finish()
                .done("almera", "Thank you for keeping Hudon safe.");
    }

    // ---------------------------------------------------------------- Merlin's Crystal
    private static void merlinsCrystal(QuestBook b) {
        b.npc("king_arthur", "King Arthur", "CAMELOT", 0, 3, 0xE8C8A8, 0xA01A1A, 0x6A1A1A, 0xE0C030);
        b.npc("sir_gawain", "Sir Gawain", "CAMELOT", 3, 3, 0xE0B898, 0xC0C0C8, 0x2A6A2A, 0x6A4A2A);
        b.npc("sir_lancelot", "Sir Lancelot", "CAMELOT", -3, 3, 0xF0C8A8, 0xC0C0C9, 0x2A2A8A, 0xE0C060);
        b.npc("merlin", "Merlin", "CAMELOT", 0, -3, 0xF0D8C8, 0x5A2A8A, 0x3A1A6A, 0xE8E8E8);
        b.npc("morgan_le_faye", "Morgan Le Faye", "keep_le_faye", 2, 2, 0xF0D0C0, 0x2A0A2A, 0x1A0A1A, 0x1A1A1A);
        b.npc("the_lady_of_the_lake", "The Lady of the Lake", "lady_of_the_lake", 0, 0, 0xE8E8F8, 0x6AA0E0, 0x4A80C0, 0xE0E0F0);
        b.npc("candle_maker", "Candle maker", "CATHERBY", 5, 5, 0xE0B898, 0xE0E0A0, 0x6A5A3A, 0x8A6A3A);
        b.npc("thrantax", "Thrantax the Mighty", "CAMELOT", 5, -4, 0x8A2A2A, 0x3A0A0A, 0x2A0A0A, 0xE0D8B0);
        b.item("black_candle", "Lit black candle", "pot", 0x1A1A1A, "A black candle. It burns with an eerie flame.");
        b.item("bat_bones", "Bat bones", "bones", 0x8A8070, "The bones of a bat.");
        b.boss(BossDef.of("sir_mordred", "Sir Mordred").level(39).stats(38, 34, 37, 34, 1, 1).speed(4)
                .defenceBonus(60, 60, 50, -10, 50)
                .attack(AttackDef.of("sword", AttackType.SLASH, 7, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xE0B898, 0x1A1A1A, 0x1A1A1A, 0x3A3A3A).at("keep_le_faye").arena(8, "crypt"));
        b.quest("merlins_crystal", "Merlin's Crystal", Difficulty.INTERMEDIATE, 6)
                .reward("excalibur")
                .stage("I can start this quest by speaking to King Arthur in Camelot.")
                .talk("king_arthur").say("Merlin has been trapped in a magic crystal by the sorceress Morgan Le Faye. Will you free him?")
                .stage("I should ask the knights of Camelot for information, starting with Sir Gawain.")
                .talk("sir_gawain").say("Morgan Le Faye is behind this, that's certain. Ask Lancelot, he's always eavesdropping.")
                .stage("I should talk to Sir Lancelot.")
                .talk("sir_lancelot").say("Morgan hides in Keep Le Faye, north of Catherby. Her son Sir Mordred guards it.")
                .stage("I should storm Keep Le Faye and defeat Sir Mordred.")
                .kill("sir_mordred").say("* As Mordred falls, Morgan Le Faye begs for his life.")
                .stage("I should speak to Morgan Le Faye.")
                .talk("morgan_le_faye").say("Spare my son! I'll tell you how to free Merlin.",
                        "Summon the demon Thrantax at Camelot with a lit black candle and bat bones, holding Excalibur. Command him with the magic words and he will free Merlin.",
                        "The Lady of the Lake holds Excalibur. The candle maker in Catherby makes candles.")
                .stage("I need Excalibur (Lady of the Lake), a lit black candle (Catherby candle maker) and bat bones; then summon Thrantax at Camelot.")
                .talk("the_lady_of_the_lake").stay().unless("excalibur").give("excalibur")
                .say("You seek the sword Excalibur? Prove your worth... you showed kindness to a beggar. The sword is yours.")
                .talk("candle_maker").stay().unless("black_candle").take("minecraft:honeycomb").give("black_candle")
                .say("A black candle? I'll need black wax... I'll dye this wax for you. *He lights it for you.*")
                .missing("Black candles need wax. Bring me some wax from the beehives (a honeycomb).")
                .drop("giant_bat", "bat_bones", 1)
                .drop("minecraft:skeleton", "bat_bones", 6)
                .talk("thrantax").need("excalibur").take("black_candle", "bat_bones")
                .say("* You light the candle, drop the bat bones and chant: 'Snarthon Candtrick Termanto!'",
                        "WHO DARES SUMMON THRANTAX? ...I AM BOUND TO OBEY. MERLIN SHALL BE FREED.",
                        "* The crystal shatters and Merlin is free!")
                .missing("* Nothing answers. You need Excalibur, a lit black candle and bat bones to summon Thrantax.")
                .stage("Merlin is free. I should tell King Arthur.")
                .talk("king_arthur").say("Merlin is free! You have done Camelot a great service. Keep Excalibur, you've earned it.")
                .done("merlin", "Thank you for freeing me from that wretched crystal.")
                .done("king_arthur", "Welcome back, friend of Camelot.");
    }

    // ---------------------------------------------------------------- Holy Grail
    private static void holyGrail(QuestBook b) {
        b.npc("sir_galahad", "Sir Galahad", "galahads_house", 0, 0, 0xE8C0A0, 0xE0E0E8, 0x5A3A8A, 0xC0A050);
        b.npc("high_priest_entrana", "High Priest of Entrana", "entrana", -3, 2, 0xE8D0C0, 0xF0F0E0, 0xE0D0A0, 0xE0C060);
        b.npc("fisher_king", "The Fisher King", "fisher_realm", 2, 2, 0xD8D0C8, 0x5A5A7A, 0x3A3A5A, 0x8A8A8A);
        b.npc("sir_percival", "Sir Percival", "goblin_village", 0, 4, 0xE0C0A0, 0xC0C0D0, 0x6A4A2A, 0xB08040);
        b.item("magic_whistle", "Magic whistle", "key", 0xC0C0D0, "A small silver whistle. Blowing it in the right place opens a portal.");
        b.item("holy_grail", "Holy Grail", "pot", 0xE0C040, "The Holy Grail!");
        b.boss(BossDef.of("black_knight_titan", "Black Knight Titan").level(120).stats(142, 91, 100, 91, 1, 1).speed(5)
                .defenceBonus(60, 60, 60, 0, 60)
                .attack(AttackDef.of("titan_sword", AttackType.SLASH, 18, 3, 10))
                .requiresWeapon("excalibur")
                .drops(Drop.always("big_bones")).skin(0xE0B898, 0x1A1A1A, 0x1A1A1A, 0x2A2A2A)
                .size(0.9f, 2.4f, 2.0f).at("fisher_realm").arena(10, "crypt"));
        b.quest("holy_grail", "Holy Grail", Difficulty.INTERMEDIATE, 2)
                .reqQuest("merlins_crystal").req(Skill.ATTACK, 20).xp(Skill.PRAYER, 11000).xp(Skill.DEFENCE, 15300)
                .stage("I can start this quest by speaking to King Arthur in Camelot. (Requires Merlin's Crystal, 20 Attack.)")
                .talk("king_arthur").say("Now that Merlin is free, I wish to send my knights on a quest for the Holy Grail. Will you go too?")
                .stage("I should ask Merlin about the Holy Grail.")
                .talk("merlin").say("The Grail lies in the realm of the Fisher King. Sir Galahad sought it once; he lives west of McGrubor's Wood.")
                .stage("I should speak to Sir Galahad.")
                .talk("sir_galahad").say("I saw the Grail castle once... a magical whistle is needed to reach it. Ask the High Priest on Entrana.")
                .stage("I should ask the High Priest of Entrana how to reach the Fisher Realm.")
                .talk("high_priest_entrana").give("magic_whistle", "magic_whistle").travel("fisher_realm")
                .say("The whistles are kept in Draynor Manor... but take these two. Blow one where the Grail is near. *You blow the whistle.*")
                .stage("I'm in the Fisher Realm. A Black Knight Titan bars the way; only Excalibur can harm it.")
                .kill("black_knight_titan").say("* Excalibur cleaves the Titan in two. The way to the Grail castle is open.")
                .stage("I should speak to the Fisher King in his castle.")
                .talk("fisher_king").say("My realm withers... only my son, Sir Percival, can restore it. Find him and bring him here.")
                .stage("I should find Sir Percival. Rumour says he's somewhere near the Goblin Village.")
                .talk("sir_percival").say("Thank goodness! The goblins tied me in a sack. I'm the Fisher King's son? Then I must go to him.")
                .stage("I should return to the Fisher Realm and take the Holy Grail.")
                .talk("fisher_king").take("magic_whistle").give("holy_grail").say("My son has returned! Take the Holy Grail to Arthur with my blessing.")
                .stage("I should bring the Holy Grail to King Arthur.")
                .talk("king_arthur").take("holy_grail").say("The Holy Grail! You are truly a knight of Camelot.");
    }

    // ---------------------------------------------------------------- Family Crest
    private static void familyCrest(QuestBook b) {
        b.npc("dimintheis", "Dimintheis", "VARROCK", 7, 6, 0xE0C8B0, 0x5A2A5A, 0x3A1A3A, 0xC0C0C0);
        b.npc("caleb", "Caleb", "CATHERBY", -5, 5, 0xE0B898, 0xF0F0F0, 0x6A6A6A, 0xF0F0F0);
        b.npc("avan", "Avan", "AL_KHARID", -6, 4, 0xC8A070, 0x2A3A8A, 0x1A2A5A, 0x5A3A1A);
        b.npc("johnathon", "Johnathon", "jolly_boar_inn", 0, 0, 0xE8C0A0, 0x6A6A2A, 0x3A3A1A, 0x6A4A2A);
        b.item("crest_part_caleb", "Crest part (Caleb)", "gem", 0xB09030, "A third of the Fitzharmon family crest.");
        b.item("crest_part_avan", "Crest part (Avan)", "gem", 0xB09031, "A third of the Fitzharmon family crest.");
        b.item("crest_part_johnathon", "Crest part (Johnathon)", "gem", 0xB09032, "A third of the Fitzharmon family crest.");
        b.boss(BossDef.of("chronozon", "Chronozon").level(170).stats(60, 120, 120, 80, 100, 1).speed(4)
                .defenceBonus(0, 0, 0, -10, 0)
                .attack(AttackDef.of("claw", AttackType.SLASH, 10, 3, 6))
                .attack(AttackDef.of("blood_blast", AttackType.MAGIC, 15, 8, 4).special(Special.LIFESTEAL, 25).particle("minecraft:crimson_spore"))
                .phase(50, "Chronozon screams: 'Only the four elements can harm me!'", 1.1, false)
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("edgeville_dungeon_chronozon").arena(9, "crypt"));
        b.quest("family_crest", "Family Crest", Difficulty.EXPERIENCED, 1)
                .req(Skill.MINING, 40).req(Skill.SMITHING, 40).req(Skill.MAGIC, 59).req(Skill.CRAFTING, 40)
                .reward("steel_gauntlets")
                .stage("I can start this quest by speaking to Dimintheis in south-east Varrock. (Requires 40 Mining, Smithing, Crafting and 59 Magic.)")
                .talk("dimintheis").say("My family crest was split between my three sons, Caleb, Avan and Johnathon. I will not die until it is whole again.",
                        "Caleb is a chef in Catherby, Avan seeks gold in Al Kharid and Johnathon was last seen at the Jolly Boar Inn.")
                .stage("I should collect the three crest pieces from Dimintheis's sons.")
                .talk("caleb").stay().unless("crest_part_caleb").take("shrimps", "salmon", "tuna", "lobster", "swordfish").give("crest_part_caleb")
                .say("A meal for the ages! Shrimp, salmon, tuna, lobster and swordfish. Here's my piece of the crest.")
                .missing("I'm making a five-fish salad: shrimp, salmon, tuna, lobster and swordfish. Bring them and the crest piece is yours.")
                .talk("avan").stay().unless("crest_part_avan").take("ruby_ring", "gold_necklace").give("crest_part_avan")
                .say("Perfect gold jewellery! A ruby ring and a gold necklace. Here, take my crest piece.")
                .missing("I want the most perfect gold jewellery: a ruby ring and a gold necklace.")
                .talk("johnathon").stay().unlessFlag("fc_chronozon").take("antipoison").flag("fc_cured")
                .say("*cough* Thank you, the poison is fading. The demon Chronozon took my crest piece. He lurks in the Edgeville Dungeon.",
                        "Only blasts of all four elements can kill him.")
                .missing("*cough* Poisoned... need antipoison...")
                .kill("chronozon").stay().unless("crest_part_johnathon").give("crest_part_johnathon").flag("fc_chronozon")
                .say("* Chronozon is destroyed. Among his remains is a piece of the crest.")
                .talk("dimintheis").take("crest_part_caleb", "crest_part_avan", "crest_part_johnathon")
                .say("My family crest, whole again! Take these gauntlets. My sons can enchant them for you.")
                .missing("Bring me all three pieces of the crest.")
                .done("dimintheis", "My family is reunited, thanks to you.");
    }

    // ---------------------------------------------------------------- Heroes' Quest
    private static void heroesQuest(QuestBook b) {
        b.npc("achietties", "Achietties", "quest_heroes_guild", 0, 0, 0xF0C8A8, 0xC03A3A, 0x6A1A1A, 0xE0A030);
        b.npc("grip", "Grip", "BRIMHAVEN", 3, 3, 0xD0A070, 0x3A3A3A, 0x2A2A2A, 0x6A2A1A);
        b.npc("alfonse_the_waiter", "Alfonse the Waiter", "BRIMHAVEN", -3, 3, 0xE0B898, 0xF0F0F0, 0x1A1A1A, 0x2A1A0A);
        b.item("entranan_firebird_feather", "Entranan firebird feather", "mark", 0xFF6A1A, "A feather from an Entranan firebird. It's still hot.");
        b.item("thieves_armband", "Master thieves' armband", "ring", 0x3A3A3A, "The armband of a master thief.");
        b.item("cooked_lava_eel", "Lava eel", "fish_cooked", 0xC03A1A, "A strange eel that lives in lava.");
        b.item("ice_gloves", "Ice gloves", "gloves", 0xA0D0F0, "These gloves are cold to the touch.");
        b.boss(BossDef.of("ice_queen", "Ice Queen").level(111).stats(104, 95, 95, 102, 80, 1).speed(4)
                .defenceBonus(30, 30, 30, 50, 30)
                .attack(AttackDef.of("ice_blast", AttackType.MAGIC, 17, 8, 6).special(Special.FREEZE, 5).particle("minecraft:snowflake"))
                .attack(AttackDef.of("staff", AttackType.CRUSH, 14, 2.5, 4))
                .drops(Drop.always("bones")).skin(0xD8F0FF, 0x80C0F0, 0x6090C0, 0xE0F8FF).at("ice_queens_lair").arena(10, "ice"));
        b.boss(BossDef.of("entranan_firebird", "Entranan firebird").level(6).stats(5, 5, 5, 5, 1, 1).speed(4)
                .attack(AttackDef.of("peck", AttackType.STAB, 2, 2, 10)).drops(Drop.always("bones"))
                .skin(0xFF6A1A, 0xFFA040, 0xC03A1A, 0xFFD040).size(0.6f, 0.9f, 1.0f));
        b.quest("heroes_quest", "Heroes' Quest", Difficulty.EXPERIENCED, 1)
                .qp(55).req(Skill.COOKING, 53).req(Skill.FISHING, 53).req(Skill.HERBLORE, 25).req(Skill.MINING, 50)
                .reqQuest("shield_of_arrav", "lost_city", "merlins_crystal", "dragon_slayer_i")
                .xp(Skill.ATTACK, 3075).xp(Skill.DEFENCE, 3075).xp(Skill.STRENGTH, 3075).xp(Skill.HITPOINTS, 3075)
                .xp(Skill.RANGED, 2075).xp(Skill.FISHING, 2725).xp(Skill.COOKING, 2825).xp(Skill.WOODCUTTING, 1575)
                .xp(Skill.FIREMAKING, 1575).xp(Skill.SMITHING, 2275).xp(Skill.MINING, 2575).xp(Skill.HERBLORE, 1325)
                .unlock("Access to the Heroes' Guild").unlock("Ability to wield the dragon battleaxe")
                .stage("I can start this quest by speaking to Achietties outside the Heroes' Guild in Burthorpe. (Requires 55 quest points.)")
                .talk("achietties").say("To join the Heroes' Guild you must bring me three items: a feather of an Entranan firebird, a master thieves' armband and a cooked lava eel.")
                .stage("I need an Entranan firebird feather (ice gloves from the Ice Queen protect from its heat), a master thieves' armband (Brimhaven gangs) and a cooked lava eel.")
                .kill("ice_queen").stay().unless("ice_gloves").give("ice_gloves").say("* The Ice Queen shatters. You take her ice gloves.")
                .talk("high_priest_entrana").stay().need("ice_gloves").unless("entranan_firebird_feather").spawn("entranan_firebird")
                .say("The firebird nests on Entrana. Its feathers burn bare hands, but those ice gloves will protect you.")
                .kill("entranan_firebird").stay().need("ice_gloves").unless("entranan_firebird_feather").give("entranan_firebird_feather")
                .talk("alfonse_the_waiter").stay().unlessFlag("hq_grip").flag("hq_grip")
                .say("The Brimhaven gang's boss Grip has the key to the treasure room. Get him out of the way and the armband is yours for the taking.")
                .talk("grip").stay().needFlag("hq_grip").unless("thieves_armband").give("thieves_armband")
                .say("* You distract Grip and slip into the treasure room. You take the master thieves' armband.")
                .talk("caleb").stay().unless("cooked_lava_eel").take("raw_swordfish", "coins:500").give("cooked_lava_eel")
                .say("Lava eels live in the Taverley Dungeon's lava; you need an oily rod. I'll trade you one I caught for a raw swordfish and 500 coins.")
                .talk("achietties").take("entranan_firebird_feather", "thieves_armband", "cooked_lava_eel")
                .say("You have proven yourself a true hero. Welcome to the Heroes' Guild!")
                .missing("Bring me the feather of an Entranan firebird, a master thieves' armband and a cooked lava eel.")
                .done("achietties", "Welcome, hero.");
    }

    // ---------------------------------------------------------------- Sheep Herder
    private static void sheepHerder(QuestBook b) {
        b.npc("councillor_halgrive", "Councillor Halgrive", "ardougne_wall", 0, 0, 0xE8C8A8, 0x6A1A6A, 0x4A1A4A, 0xC0C0C0);
        b.npc("doctor_orbon", "Doctor Orbon", "ARDOUGNE", 7, -5, 0xE0C0A0, 0xE8E8E8, 0x3A3A3A, 0x6A6A6A);
        b.npc("farmer_brumty", "Farmer Brumty", "ardougne_wall", 4, 3, 0xD8A880, 0x7A6A3A, 0x4A3A2A, 0xC0A040);
        b.item("protective_clothing", "Plague protective clothing", "cloth", 0xE0E0C0, "A full suit of protective clothing against the plague.");
        b.item("plague_sheep_remains", "Plague sheep remains", "bones", 0x8A9A6A, "Diseased remains. Must be incinerated.");
        b.boss(BossDef.of("plague_sheep", "Plague sheep").level(4).stats(8, 1, 1, 1, 1, 1).speed(5)
                .attack(AttackDef.melee(1)).passive().drops(Drop.always("plague_sheep_remains"))
                .skin(0xA0B080, 0xD0D8B0, 0xA0B080, 0x8A9A6A).size(0.9f, 1.3f, 1.0f));
        b.quest("sheep_herder", "Sheep Herder", Difficulty.NOVICE, 4)
                .reward("coins:3100")
                .stage("I can start this quest by speaking to Councillor Halgrive by the Ardougne wall.")
                .talk("councillor_halgrive").say("Plague-infected sheep have escaped from Farmer Brumty's farm! They must be killed and their remains burnt.",
                        "Doctor Orbon sells protective clothing. Don't go near those sheep without it.")
                .stage("I should buy protective clothing from Doctor Orbon in East Ardougne.")
                .talk("doctor_orbon").take("coins:100").give("protective_clothing").say("Protective clothing, 100 coins. Don't take it off near those sheep!")
                .missing("A suit of protective clothing costs 100 coins.")
                .stage("Wearing the protective clothing, I should herd and kill the four plague sheep on Farmer Brumty's farm and incinerate the remains.")
                .talk("farmer_brumty").stay().need("protective_clothing").spawn("plague_sheep")
                .say("There's one of 'em! Get it into the pen and finish it off.")
                .missing("Don't come near my farm without protective clothing!")
                .kill("plague_sheep").stay().need("protective_clothing")
                .talk("councillor_halgrive").take("plague_sheep_remains:4")
                .say("* You incinerate the four sets of remains. Halgrive is very pleased. Here is your reward.")
                .missing("I need proof that all four sheep are dead: their remains, for the incinerator.");
    }

    // ---------------------------------------------------------------- Plague City
    private static void plagueCity(QuestBook b) {
        b.npc("edmond", "Edmond", "ARDOUGNE", -7, 6, 0xE0B898, 0x5A6A3A, 0x3A4A2A, 0x6A4A2A);
        b.npc("alrena", "Alrena", "ARDOUGNE", -7, 3, 0xF0C8A8, 0x8A4A6A, 0x5A2A4A, 0x8A5A2A);
        b.npc("jethick", "Jethick", "west_ardougne", 3, 3, 0xD8B898, 0x6A6A5A, 0x4A4A3A, 0x3A3A2A);
        b.npc("martha_rehnison", "Martha Rehnison", "west_ardougne", -3, 3, 0xE8C0A0, 0x6A3A3A, 0x4A2A2A, 0x9A9A9A);
        b.npc("bravek", "Bravek", "west_ardougne", 0, -4, 0xE8C8A8, 0x8A6A1A, 0x5A4A1A, 0x2A2A2A);
        b.npc("elena", "Elena", "west_ardougne", -4, -4, 0xF0D0B8, 0x3A6A3A, 0x2A4A2A, 0xC06A2A);
        b.item("gas_mask", "Gas mask", "full_helm", 0x6A6A5A, "Stops me from breathing in plague-infected air.");
        b.item("warrant", "Warrant", "note", 0xE8E0C0, "A warrant from Bravek to release Elena.");
        b.item("magic_scroll_ardougne", "A magic scroll", "note", 0xD0C0F0, "Teaches the Ardougne Teleport spell.");
        b.quest("plague_city", "Plague City", Difficulty.NOVICE, 1)
                .xp(Skill.MINING, 2425).reward("magic_scroll_ardougne")
                .stage("I can start this quest by speaking to Edmond in north-west East Ardougne.")
                .talk("edmond").say("My daughter Elena went into West Ardougne to help the plague victims and hasn't come back. Please help!",
                        "We can dig under the wall through the sewers. Soften the soil with four buckets of water, and my wife Alrena can make you a gas mask.")
                .stage("I need 4 buckets of water to soften the soil and a gas mask from Alrena (she needs dwellberries; any berries will do).")
                .talk("alrena").stay().unless("gas_mask").give("gas_mask").say("Here's the gas mask. Always wear it in West Ardougne!")
                .talk("edmond").need("gas_mask").take("minecraft:water_bucket:4").give("minecraft:bucket:4").travel("west_ardougne")
                .say("* You soften the soil and dig through to the sewer. With Edmond's help you pull the grill free and climb into West Ardougne.")
                .missing("We need four buckets of water and you'll need a gas mask.")
                .stage("I'm in West Ardougne. I should ask the locals about Elena.")
                .talk("jethick").say("Elena? She stayed with the Rehnisons, the house to the north.")
                .stage("I should ask Martha Rehnison about Elena.")
                .talk("martha_rehnison").say("Elena went to visit the plague house and never came back. Only Bravek, the city warder, can order it opened.")
                .stage("Bravek is too hungover to help. I need to make a hangover cure: chocolate dust, snape grass and a bucket of milk.")
                .talk("bravek").take("chocolate_dust", "snape_grass", "minecraft:milk_bucket").give("warrant")
                .say("*Bravek gulps the cure* ...Ugh. Much better. Elena? Here's a warrant to open the plague house.")
                .missing("Ugh... my head... go away unless you have a hangover cure. Chocolate dust, snape grass and milk...")
                .stage("I should use the warrant to free Elena from the plague house.")
                .talk("elena").take("warrant").travel("ARDOUGNE")
                .say("You came for me! There's no plague here, I think... but let's go. Thank you!", "* You escape back through the sewers.")
                .stage("I should tell Edmond that Elena is safe.")
                .talk("edmond").say("Elena is home! Thank you. Take this magic scroll, it teaches the Ardougne Teleport.")
                .done("elena", "Something about this plague doesn't add up...");
    }

    // ---------------------------------------------------------------- Biohazard
    private static void biohazard(QuestBook b) {
        b.npc("jerico", "Jerico", "ARDOUGNE", -4, 7, 0xE0C0A0, 0x6A5A3A, 0x4A3A2A, 0xB0B0B0);
        b.npc("omart", "Omart", "ardougne_wall", -3, 3, 0xD0A880, 0x3A5A3A, 0x2A3A2A, 0x2A2A1A);
        b.npc("chemist", "Chemist", "rimmington_chemist", 0, 0, 0xE8D0C0, 0xF0F0F0, 0x5A5A5A, 0x8A8A8A);
        b.npc("guidor", "Guidor", "VARROCK", -7, 6, 0xE0C8B0, 0x3A3A6A, 0x2A2A4A, 0xE0E0E0);
        b.npc("king_lathas", "King Lathas", "ARDOUGNE", 0, -7, 0xE8C8A8, 0x6A1A8A, 0x4A1A5A, 0xE0C030);
        b.item("distillator", "Distillator", "pot", 0xA0A0C0, "Elena's distillator, for analysing the plague sample.");
        b.item("plague_sample", "Plague sample", "vial", 0x8AA05A, "A sample of the 'plague'.");
        b.boss(BossDef.of("head_mourner", "Head mourner").level(30).stats(40, 25, 25, 25, 1, 1).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 5, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xE0C0A0, 0x2A2A2A, 0x2A2A2A, 0x1A1A1A).at("mourner_hq").arena(7, "stone"));
        b.quest("biohazard", "Biohazard", Difficulty.NOVICE, 3)
                .reqQuest("plague_city").xp(Skill.THIEVING, 1250)
                .stage("I can start this quest by speaking to Elena in East Ardougne. (Requires Plague City.)")
                .talk("elena").say("The mourners stole my distillator. I need it to analyse the plague! Jerico may help you get into their headquarters.")
                .stage("I should ask Jerico, who keeps carrier pigeons, for help.")
                .talk("jerico").say("Release my pigeons over the mourners' yard to distract them. Omart can get you over the wall with a rope ladder.")
                .stage("I should ask Omart to help me over the wall.")
                .talk("omart").travel("mourner_hq").say("* The pigeons scatter the mourners. Omart throws a rope ladder over the wall and you climb into the mourners' yard.")
                .stage("I should find the distillator in the mourner headquarters. The head mourner has the key.")
                .kill("head_mourner").give("distillator").say("* You take the key from the head mourner and find Elena's distillator.")
                .stage("I should return the distillator to Elena.")
                .talk("elena").take("distillator").give("plague_sample").say("My distillator! Take this plague sample to Guidor in Varrock. The chemist in Rimmington can help you smuggle it.")
                .stage("I should take the plague sample to Guidor in Varrock.")
                .talk("chemist").stay().say("Hide the sample among my chemicals and the guards won't search you. Off you go to Varrock.")
                .talk("guidor").take("plague_sample").say("This sample... it's just water and food colouring! There is no plague. King Lathas must be told.")
                .stage("There is no plague! I should tell King Lathas in East Ardougne.")
                .talk("king_lathas").say("I know. The plague was a ruse to keep West Ardougne under control, for reasons I cannot yet share.",
                        "You have been very helpful. I may call on you again.")
                .done("king_lathas", "My brother Tyras must be dealt with. In time.");
    }

    // ---------------------------------------------------------------- Fight Arena
    private static void fightArena(QuestBook b) {
        b.npc("lady_servil", "Lady Servil", "servil_house", 0, 0, 0xF0C8A8, 0x8A2A6A, 0x5A1A4A, 0x5A2A1A);
        b.npc("hengrad", "Hengrad", "fight_arena", 4, 4, 0xC8A070, 0x6A6A6A, 0x3A3A3A, 0x2A2A2A);
        b.npc("khazard_barman", "Khazard barman", "fight_arena", -4, 4, 0xE0B898, 0xE0E0D0, 0x3A2A1A, 0x6A4A2A);
        b.npc("jeremy_servil", "Jeremy Servil", "fight_arena", 4, -4, 0xF0D0B0, 0x8A6A3A, 0x5A4A2A, 0x8A5A2A);
        b.item("khali_brew", "Khali brew", "beer", 0x6A3A1A, "A bottle of Khazard's worst brew.");
        b.item("khazard_cell_keys", "Khazard cell keys", "key", 0x6A6A6A, "Keys to the cells of the Fight Arena.");
        b.boss(BossDef.of("bouncer", "Bouncer").level(137).stats(116, 140, 140, 60, 1, 1).speed(4)
                .defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("bite", AttackType.STAB, 17, 2.5, 10))
                .attack(AttackDef.of("fire_breath", AttackType.MAGIC, 12, 6, 3).particle("minecraft:flame"))
                .drops(Drop.always("bones")).size(1.0f, 1.2f, 1.0f).at("fight_arena").arena(10, "sand"));
        b.boss(BossDef.of("general_khazard", "General Khazard").level(112).stats(170, 70, 75, 75, 1, 1).speed(4)
                .defenceBonus(80, 80, 80, 0, 80)
                .attack(AttackDef.of("slash", AttackType.SLASH, 26, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xD0A880, 0x5A1A1A, 0x2A2A2A, 0x1A1A1A).size(0.9f, 2.4f, 1.2f).at("fight_arena").arena(10, "sand"));
        b.quest("fight_arena", "Fight Arena", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.ATTACK, 12175).xp(Skill.THIEVING, 2175).reward("coins:1000")
                .stage("I can start this quest by speaking to Lady Servil, south-east of the Fight Arena.")
                .talk("lady_servil").say("General Khazard has captured my husband and son! He makes them fight for sport in his arena. Please save them!")
                .stage("I should disguise myself as a Khazard guard and find a way into the cells.")
                .talk("khazard_barman").take("coins:5").give("khali_brew").say("Khali brew? 5 coins. Only the guards drink it.")
                .missing("Khali brew's 5 coins.")
                .stage("The guard Hengrad holds the cell keys. A Khali brew might loosen him up.")
                .talk("hengrad").take("khali_brew").give("khazard_cell_keys").say("Khali brew! *glug glug* ...zzz...", "* You take the keys from the snoring guard.")
                .stage("I should free Jeremy Servil.")
                .talk("jeremy_servil").take("khazard_cell_keys").say("You came for me! But Khazard's men have seen us... we'll have to fight our way out!",
                        "* Khazard throws you into the arena!")
                .stage("I must survive the arena: defeat Khazard's champion, the Bouncer.")
                .kill("bouncer").say("* The Bouncer falls. General Khazard is furious!")
                .stage("General Khazard himself challenges me.")
                .kill("general_khazard").say("* Khazard flees, beaten. The Servils are free!")
                .stage("I should return to Lady Servil.")
                .talk("lady_servil").say("My family is home! Please, take this reward.");
    }

    // ---------------------------------------------------------------- Hazeel Cult
    private static void hazeelCult(QuestBook b) {
        b.npc("ceril_carnillean", "Ceril Carnillean", "carnillean_house", 0, 0, 0xF0D0B8, 0x3A3A8A, 0x2A2A5A, 0xC0A050);
        b.npc("clivet", "Clivet", "hazeel_cult_hideout", 2, 2, 0xC0B0A0, 0x2A1A3A, 0x1A0A2A, 0x1A1A1A);
        b.npc("butler_jones", "Butler Jones", "carnillean_house", 3, 2, 0xE8C8A8, 0x1A1A1A, 0x1A1A1A, 0xA0A0A0);
        b.item("carnillean_armour", "Carnillean armour", "platebody", 0xA0A0B0, "The Carnillean family armour.");
        b.boss(BossDef.of("alomone", "Alomone").level(13).stats(20, 10, 10, 10, 1, 1).speed(4)
                .attack(AttackDef.of("dagger", AttackType.STAB, 3, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xC0B0A0, 0x3A0A3A, 0x1A0A1A, 0x0A0A0A).at("hazeel_cult_hideout").arena(7, "crypt"));
        b.quest("hazeel_cult", "Hazeel Cult", Difficulty.NOVICE, 1)
                .xp(Skill.THIEVING, 1500).reward("coins:2000")
                .stage("I can start this quest by speaking to Ceril Carnillean in his house south of the Ardougne market.")
                .talk("ceril_carnillean").say("Our family armour was stolen and someone poisoned our dog! The Hazeel cult must be behind it.")
                .stage("I should find the Hazeel cult's hideout in the sewers west of the Clock Tower.")
                .talk("clivet").say("You've found us. Hazeel will rise again! Join us... or don't. Alomone, our leader, will deal with you.")
                .stage("I should defeat Alomone, leader of the Hazeel cult.")
                .kill("alomone").give("carnillean_armour").say("* Alomone is slain. In the hideout you find the stolen Carnillean armour.")
                .stage("I should return the armour to Ceril. Butler Jones is the one who poisoned the dog!")
                .talk("ceril_carnillean").take("carnillean_armour").say("Our armour! And Jones a cultist all along? Guards! Take this, with our thanks.")
                .done("butler_jones", "...I'm sure I don't know what you mean.");
    }

    // ---------------------------------------------------------------- Clock Tower
    private static void clockTower(QuestBook b) {
        b.npc("brother_kojo", "Brother Kojo", "clock_tower", 0, 2, 0xE0B898, 0x6A4A3A, 0x6A4A3A, 0x4A3A2A);
        b.item("red_cog", "Red cog", "gem", 0xC03A3A, "A red cog for the clock tower.");
        b.item("blue_cog", "Blue cog", "gem", 0x3A5AC0, "A blue cog for the clock tower.");
        b.item("black_cog", "Black cog", "gem", 0x2A2A2A, "A black cog for the clock tower.");
        b.item("white_cog", "White cog", "gem", 0xE8E8E8, "A white cog for the clock tower.");
        b.quest("clock_tower", "Clock Tower", Difficulty.NOVICE, 1)
                .reward("coins:500")
                .stage("I can start this quest by speaking to Brother Kojo at the Clock Tower south of Ardougne.")
                .talk("brother_kojo").say("The clock has stopped! The four cogs have been taken: red, blue, black and white. Rats in the dungeon may have dragged them off.")
                .stage("I need to find the red, blue, black and white cogs in the Clock Tower dungeon.")
                .drop("minecraft:silverfish", "red_cog", 3)
                .drop("minecraft:cave_spider", "blue_cog", 3)
                .drop("minecraft:skeleton", "black_cog", 3)
                .drop("minecraft:zombie", "white_cog", 3)
                .talk("brother_kojo").take("red_cog", "blue_cog", "black_cog", "white_cog")
                .say("* You fit each cog onto its spindle. The great clock begins to tick!", "Wonderful! The people of Ardougne will know the time again. Take this.")
                .missing("I still need the red, blue, black and white cogs. Rats, spiders and the dead of the dungeon have taken them.");
    }

    // ---------------------------------------------------------------- Witch's House
    private static void witchsHouse(QuestBook b) {
        b.npc("witchs_house_boy", "Boy", "witchs_house", 3, 4, 0xF0D0B0, 0x3A6AC0, 0x3A3A6A, 0xC08A3A);
        b.item("childs_ball", "Ball", "bead", 0xE03A3A, "A child's ball.");
        b.item("door_key_witch", "Door key", "key", 0x8A8A8A, "The key to the witch's house.");
        b.boss(BossDef.of("witchs_experiment", "Witch's experiment").level(53).stats(91, 50, 50, 40, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 8, 2.5, 10))
                .phase(75, "The experiment changes into a spider!", 1.1, false, AttackDef.of("bite", AttackType.STAB, 9, 2.5, 10))
                .phase(50, "The experiment changes into a bear!", 1.2, false, AttackDef.of("maul", AttackType.CRUSH, 10, 2.5, 10))
                .phase(25, "The experiment changes into a wolf!", 1.3, false, AttackDef.of("tear", AttackType.SLASH, 11, 2.5, 10))
                .drops(Drop.always("bones")).size(1.0f, 1.2f, 1.0f).at("witchs_house").arena(8, "crypt"));
        b.quest("witchs_house", "Witch's House", Difficulty.INTERMEDIATE, 4)
                .xp(Skill.HITPOINTS, 6325)
                .stage("I can start this quest by speaking to the boy outside the witch's house in Taverley.")
                .talk("witchs_house_boy").say("*sniff* I kicked my ball into the witch's garden and she won't give it back!")
                .stage("I should get into the witch's house. A key may be hidden near the door.")
                .reach("witchs_house", 6).give("door_key_witch").say("* You find a key under the doormat.")
                .stage("The ball is in the witch's garden shed. Her experiment guards it!")
                .kill("witchs_experiment").need("door_key_witch").give("childs_ball")
                .say("* The experiment is finally destroyed. You grab the ball from the shed.")
                .stage("I should return the ball to the boy.")
                .talk("witchs_house_boy").take("childs_ball", "door_key_witch").say("My ball! Thank you so much!");
    }
}
