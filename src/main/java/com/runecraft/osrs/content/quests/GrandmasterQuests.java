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
                .stage("I should travel to Ape Atoll and find out what the monkeys are planning.")
                .talk("lumdo").travel("ape_atoll").say("Back to Ape Atoll? Hold on. *Lumdo sails you across.*")
                .stage("I should talk to Garkor on Ape Atoll.")
                .talk("garkor").say("Glough has allied with Kruk, a monkey general. Together they've made monstrous demonic gorillas. Kruk hides in the caverns beneath the atoll.")
                .stage("I must defeat Kruk in his dungeon beneath Ape Atoll.")
                .kill("kruk").give("kruks_paw").say("* Kruk falls with a thunderous crash! Glough flees but is captured by the 10th squad.")
                .stage("I should report to King Narnode.")
                .talk("king_narnode").take("kruks_paw").say("Kruk is dead and Glough is caught. Take this royal seed pod; it will always bring you home to the Grand Tree.")
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
        b.quest("dragon_slayer_ii", "Dragon Slayer II", Difficulty.GRANDMASTER, 5)
                .xp(Skill.SMITHING, 80000).xp(Skill.MINING, 60000).xp(Skill.AGILITY, 50000).xp(Skill.THIEVING, 50000) // OSRS (current quest XP rewards)
                .qp(200).reqQuest("legends_quest", "animal_magnetism", "ghosts_ahoy", "bone_voyage")
                .reqQuest("dream_mentor", "a_tail_of_two_cats", "client_of_kourend")
                .req(Skill.MAGIC, 75).req(Skill.SMITHING, 70).req(Skill.MINING, 68).req(Skill.CRAFTING, 62)
                .req(Skill.AGILITY, 60).req(Skill.THIEVING, 60).req(Skill.CONSTRUCTION, 50).req(Skill.HITPOINTS, 50)
                .unlock("Access to the Myths' Guild and Vorkath").unlock("Ability to smith dragon platebodies and kiteshields")
                .stage("I can start this quest by speaking to Alec Kincade outside the Myths' Guild. (Requires 200 quest points, Legends' Quest and much more.)")
                .talk("alec_kincade").say("Dragons are stirring all over Gielinor. Something is uniting them. An archaeologist, Dallas Jones, has found something in Varrock.")
                .stage("I should speak to Dallas Jones at the Varrock Museum.")
                .talk("dallas_jones").say("A dragonkin journal! It mentions a lab on Fossil Island. The dragonkin were creating something... and a dragon named Galvek.")
                .stage("I should search for dragonkin ruins on Fossil Island.")
                .reach("fossil_island", 12).give("ancient_mural_map").say("* Beneath Fossil Island you find a dragonkin mural. It shows an island in the far north: Ungael.")
                .stage("I need a ship to Ungael. Brundt of the Fremennik may lend me one.")
                .talk("brundt").need("ancient_mural_map").travel("quest_ungael").say("A voyage to the frozen north? The Fremennik will sail you. *Your longship lands on Ungael.*")
                .stage("I must defeat the undead dragon Vorkath on Ungael.")
                .kill("ds2_vorkath").take("ancient_mural_map").give("dragonkin_key").say("* Vorkath collapses. In its lair you find a dragonkin key.")
                .stage("I should take the dragonkin key to Alec Kincade.")
                .talk("alec_kincade").take("dragonkin_key").say("The key opens the dragonkin vault! But Galvek has risen and leads the dragons against us. Robert the Strong, the hero of old, has returned to fight beside you.")
                .stage("I must defeat Galvek, the dragon of the dragonkin, on the Western Sea.")
                .talk("robert_the_strong").stay().say("Together, as I fought beside Camorra of old! To the ships!")
                .kill("galvek").say("* Galvek falls into the sea. The dragon threat is ended.")
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
                .req(Skill.HERBLORE, 65).req(Skill.HUNTER, 62)
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
