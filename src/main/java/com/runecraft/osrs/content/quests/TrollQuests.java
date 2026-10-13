package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** Troll Country quests: Death Plateau, Troll Stronghold and Eadgar's Ruse. */
final class TrollQuests {
    private TrollQuests() {
    }

    static void register(QuestBook b) {
        b.spot("toad_and_chicken", "Toad and Chicken inn", 2906, 3538);
        b.spot("tenzings_hut", "Tenzing's hut", 2820, 3555);
        b.place("death_plateau", "Death Plateau", 2860, 3590, PlaceDef.Style.RUINS);
        b.place("troll_arena", "Troll arena", 2910, 3610, PlaceDef.Style.ARENA);
        b.place("troll_stronghold", "Troll Stronghold", 2840, 3690, PlaceDef.Style.CAVE);
        b.spot("eadgars_cave", "Eadgar's cave", 2893, 3680);
        deathPlateau(b);
        trollStronghold(b);
        eadgarsRuse(b);
    }

    private static void deathPlateau(QuestBook b) {
        b.npc("denulth", "Denulth", "BURTHORPE", 5, -5, 0xE0B89B, 0xA02A2A, 0x5A1A1A, 0x8A8A8A);
        b.npc("harold", "Harold", "toad_and_chicken", 0, 0, 0xE8C0AA, 0x6A5A3A, 0x3A3A2A, 0x6A3A1A);
        b.npc("tenzing", "Tenzing", "tenzings_hut", 0, 0, 0xC89A6C, 0xC0402A, 0x5A3A2A, 0x2A1A0E);
        b.npc("dunstan", "Dunstan", "BURTHORPE", -5, 5, 0xE0B89C, 0x5A5A5A, 0x3A3A3A, 0xA06A2A);
        b.item("combination_iou", "IOU", "note", 0xE8E0C0, "An IOU from Harold, with the equipment room combination on the back.");
        b.item("climbing_boots", "Climbing boots", "boots", 0x6A4A2A, "Boots made for climbing.");
        b.item("spiked_boots", "Spiked boots", "boots", 0x7A7A7A, "Climbing boots with iron spikes.");
        b.item("secret_way_map", "Secret way map", "note", 0xD8C8A0, "A map of the secret path up Death Plateau.");
        b.quest("death_plateau", "Death Plateau", Difficulty.NOVICE, 1)
                .xp(Skill.ATTACK, 3000).reward("climbing_boots")
                .stage("I can start this quest by speaking to Denulth in the Imperial Guard camp in Burthorpe.")
                .talk("denulth").say("Trolls are attacking from Death Plateau! I need a secret path up there to ambush them. Our equipment room is locked; Harold forgot the combination.")
                .stage("I should find Harold in the Toad and Chicken inn; he has the combination for the equipment room.")
                .talk("harold").take("coins:200").give("combination_iou")
                .say("Combination? Let's play dice for it... *You win.* I can't pay! Here's an IOU. *The combination is written on the back.*")
                .missing("Fancy a game of dice? Put 200 coins on the table.")
                .stage("I should open the equipment room with the combination, then ask Tenzing the sherpa about a path up Death Plateau.")
                .talk("tenzing").take("combination_iou").say("A path? I know one. But I'll only show it to someone with spiked boots, and my old climbing boots need spikes. Dunstan the smith can add them.")
                .stage("I need spiked boots: Dunstan in Burthorpe can spike climbing boots with an iron bar.")
                .talk("dunstan").take("iron_bar").give("spiked_boots").say("Spikes on climbing boots? Easy. *clang clang* There you go.")
                .missing("Bring me an iron bar and I'll spike some boots for you.")
                .stage("I should give Tenzing the spiked boots.")
                .talk("tenzing").take("spiked_boots").give("secret_way_map").say("Perfect! Here's a map of the secret path up Death Plateau.")
                .stage("I should take the map to Denulth.")
                .talk("denulth").take("secret_way_map").say("The secret way! Now we can ambush the trolls. Thank you, warrior. Keep those climbing boots.")
                .done("tenzing", "Mind your step on the mountain.");
    }

    private static void trollStronghold(QuestBook b) {
        b.npc("mad_eadgar", "Mad Eadgar", "eadgars_cave", 0, 0, 0xE8C0AB, 0x5A5A3A, 0x3A3A2A, 0xC0C0C1);
        b.npc("godric", "Godric", "troll_stronghold", 3, 3, 0xE8C0AC, 0xA02A2B, 0x5A1A1B, 0x8A8A8B);
        b.item("prison_key", "Prison key", "key", 0x5A5A5A, "Opens the cells of the Troll Stronghold.");
        b.boss(BossDef.of("dad", "Dad").level(101).stats(120, 100, 100, 60, 1, 1).speed(5)
                .attack(AttackDef.of("club", AttackType.CRUSH, 18, 3, 8))
                .attack(AttackDef.of("stomp", AttackType.CRUSH, 12, 5, 2).special(Special.KNOCKBACK, 3).telegraph(1, "Dad raises his foot!"))
                .drops(Drop.always("big_bones")).size(1.2f, 2.6f, 1.0f).at("troll_arena").arena(11, "stone"));
        b.boss(BossDef.of("troll_general", "Troll General").level(113).stats(140, 100, 110, 80, 1, 1).speed(5)
                .attack(AttackDef.of("great_club", AttackType.CRUSH, 20, 3, 10))
                .drops(Drop.always("big_bones")).size(1.2f, 2.6f, 1.0f).at("troll_stronghold").arena(10, "stone"));
        b.quest("troll_stronghold", "Troll Stronghold", Difficulty.INTERMEDIATE, 1)
                .reqQuest("death_plateau").req(Skill.AGILITY, 15) // OSRS: no XP reward (law talisman, Trollheim access)
                .stage("I can start this quest by speaking to Denulth in Burthorpe. (Requires Death Plateau, 15 Agility.)")
                .talk("denulth").say("The trolls have captured Godric, one of my best men, and taken him to their stronghold. Will you rescue him?")
                .stage("I should go to the troll arena on Death Plateau and fight past the trolls' champion, Dad.")
                .kill("dad").say("* Dad is beaten! The way to the Troll Stronghold is open.")
                .stage("I should sneak into the Troll Stronghold. The Troll General holds the prison key.")
                .kill("troll_general").give("prison_key").say("* The Troll General falls with a mighty crash, dropping the prison key.")
                .stage("I should free Godric and Mad Eadgar from the prison.")
                .talk("godric").take("prison_key").say("You came for me! Free Eadgar too, the mad old hermit in the next cell. He lives on the mountain top.",
                        "* You unlock both cells. Eadgar scurries off home.")
                .stage("I should tell Denulth that Godric is free.")
                .talk("denulth").say("Godric is safe! The trolls will think twice now. Thank you.")
                .done("godric", "Thanks for getting me out of there.");
    }

    private static void eadgarsRuse(QuestBook b) {
        b.npc("burntmeat", "Burntmeat", "troll_stronghold", -3, 3, 0x6A7A5B, 0xE0E0D0, 0x5A6A4A, 0x7A8A6A);
        b.npc("brother_tranquility", "Brother Tranquility", "troll_stronghold", 0, -4, 0xE0B89D, 0x6A4A3B, 0x6A4A3B, 0x3A2A1B);
        b.item("troll_disguise", "Fake man", "creature", 0xC0A080, "A scarecrow dressed to look like a human. Trolls love humans... to eat.");
        b.item("troll_potion", "Troll potion", "potion", 0x6A8A4A, "Tastes like... troll.");
        b.quest("eadgars_ruse", "Eadgar's Ruse", Difficulty.INTERMEDIATE, 1)
                .reqQuest("DRUIDIC_RITUAL", "troll_stronghold").req(Skill.HERBLORE, 31)
                .xp(Skill.HERBLORE, 11000)
                .unlock("The Trollheim Teleport spell")
                .stage("I can start this quest by speaking to Mad Eadgar atop Trollheim: Sanfew of Taverley needs goutweed from the trolls' kitchen. (Requires Druidic Ritual, Troll Stronghold, 31 Herblore, 15 Agility.)")
                .talk("mad_eadgar").say("Sanfew needs goutweed? The trolls' cook Burntmeat keeps it. Trolls only want food: give him a human... a fake human!")
                .stage("Eadgar needs 10 logs and 3 balls of wool to build a fake man.")
                .talk("mad_eadgar").take("logs:10", "ball_of_wool:3").give("troll_disguise", "troll_potion")
                .say("*Eadgar builds a scarecrow 'human' and brews a troll potion so you smell like a troll.*")
                .missing("I need 10 logs and 3 balls of wool.")
                .stage("I should drink the troll potion and trade the fake man to Burntmeat for goutweed.")
                .talk("burntmeat").take("troll_disguise", "troll_potion").say("Mmm, human! Burntmeat cook him nice. Here, take smelly weed, Burntmeat not need it.",
                        "* While he's distracted you grab some goutweed from the storeroom.")
                .stage("I should tell Mad Eadgar that the ruse worked.")
                .talk("mad_eadgar").say("Ha ha! The trolls fell for it! I'll teach you a trick: the Trollheim Teleport. Mad, eh?")
                .done("brother_tranquility", "Peace be with you.");
    }
}
