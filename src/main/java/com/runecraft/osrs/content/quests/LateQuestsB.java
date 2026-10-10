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
        b.place("elid_cave", "Source of the Elid", 3370, 2970, PlaceDef.Style.CAVE);
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
        b.npc("awusah", "Awusah the Mayor", "NARDAH", 0, 5, 0xB07A4E, 0xE0C082, 0xB09062, 0xE0E0ED);
        b.npc("ghaslor", "Ghaslor the Elder", "NARDAH", 5, 0, 0xA86A43, 0xE0D0A2, 0x8A6A41, 0xE0E0EE);
        b.item("ancestral_key", "Ancestral key", "key", 0xC0A063, "The key to the Elid's source.");
        b.quest("spirits_of_the_elid", "Spirits of the Elid", Difficulty.INTERMEDIATE, 2)
                .req(Skill.MAGIC, 33).req(Skill.RANGED, 37).req(Skill.MINING, 37).req(Skill.THIEVING, 37)
                .xp(Skill.PRAYER, 8000).xp(Skill.THIEVING, 1000).xp(Skill.MAGIC, 1000)
                .unlock("Nardah's water supply restored")
                .stage("I can start this quest by speaking to Awusah the Mayor in Nardah. (Requires 33 Magic, 37 Ranged, Mining and Thieving.)")
                .talk("awusah").say("The River Elid has dried up! Nardah will die without water. Something blocks the source in the mountains to the north.")
                .stage("I should ask Ghaslor the Elder about the Elid's source.")
                .talk("ghaslor").give("ancestral_key").say("Our ancestors sealed the source with three spirits. Take the ancestral key; the spirits are angry that the people stopped honouring them.")
                .stage("I should go to the source of the Elid, appease the three spirits and remove the blockage.")
                .reach("elid_cave", 8).take("ancestral_key").say("* You defeat the three golem guardians and speak to the spirits: they forgive the people. The water flows again!")
                .stage("I should tell Awusah the water is back.")
                .talk("awusah").say("The Elid flows! Nardah is saved. Thank you, my friend.");
    }

    private static void contact(QuestBook b) {
        b.npc("jex", "Jex", "sophanem", 4, -4, 0xA86A44, 0x5A4A3F, 0x3A2A20, 0x2A1A11);
        b.items(s -> s.add("keris", "Keris", Kind.MELEE_WEAPON, "dagger", 0xB0A060).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(40, 20, -10, 2, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0),
                        Map.of(Skill.ATTACK, 50), 4, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("A dagger that deals extra damage to kalphites and scarabs."));
        b.boss(BossDef.of("giant_scarab", "Giant scarab").level(191).stats(130, 150, 140, 130, 120, 1).speed(4)
                .defenceBonus(80, 80, 80, 0, 80)
                .attack(AttackDef.of("mandibles", AttackType.STAB, 20, 3, 6).special(Special.POISON, 8))
                .attack(AttackDef.of("swarm", AttackType.RANGED, 12, 8, 3).special(Special.AOE, 4).telegraph(2, "The giant scarab summons a swarm!"))
                .drops(Drop.always("bones")).size(1.6f, 1.2f, 1.0f).at("sophanem_dungeon").arena(10, "sand"));
        b.quest("contact", "Contact!", Difficulty.MASTER, 1)
                .reqQuest("prince_ali_rescue", "icthlarins_little_helper")
                .xp(Skill.THIEVING, 7000).xp(Skill.RANGED, 7000).reward("keris")
                .stage("I can start this quest by speaking to the High Priest in Sophanem. (Requires Prince Ali Rescue, Icthlarin's Little Helper.)")
                .talk("high_priest_sophanem").say("Sophanem and Menaphos have lost contact! Osman, the Emir's spymaster, may know why.")
                .stage("I should ask Osman in Al Kharid.")
                .talk("osman").say("Our agent in Sophanem has gone silent. Find Jex, a retired adventurer there.")
                .stage("I should find Jex in Sophanem.")
                .talk("jex").say("The tunnels beneath Sophanem lead to Menaphos. I lost my map down there, along with my nerve. Something huge lives below.")
                .stage("I must explore the dungeon beneath Sophanem and defeat the giant scarab.")
                .kill("giant_scarab").say("* The giant scarab is slain. Behind it you find the passage to Menaphos and a strange dagger.")
                .stage("I should report to Osman.")
                .talk("osman").say("Contact with Menaphos restored! Keep that keris; it'll serve you well against scarabs and kalphites.");
    }

    private static void beneathCursedSands(QuestBook b) {
        b.npc("tumeken_priest", "Priest of Tumeken", "necropolis", 3, 3, 0xA86A45, 0xE0C051, 0xF0F0F4, 0xE0C052);
        b.boss(BossDef.of("champion_of_scabaras", "Champion of Scabaras").level(225).stats(300, 200, 200, 180, 150, 1).speed(4)
                .defenceBonus(100, 100, 100, 80, 100)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 28, 3, 6))
                .attack(AttackDef.of("locust_swarm", AttackType.MAGIC, 18, 9, 3).special(Special.AOE, 5).telegraph(2, "Locusts swarm from the champion's shell!"))
                .attack(AttackDef.of("sand_spike", AttackType.RANGED, 22, 10, 3).special(Special.SKY_STRIKE, 0).telegraph(2, "The sand ripples beneath you!"))
                .drops(Drop.always("bones")).size(1.6f, 1.4f, 1.2f).at("necropolis").arena(12, "sand"));
        b.quest("beneath_cursed_sands", "Beneath Cursed Sands", Difficulty.MASTER, 2)
                .reqQuest("contact").req(Skill.AGILITY, 62).req(Skill.CRAFTING, 55).req(Skill.FIREMAKING, 55)
                .xp(Skill.AGILITY, 20000).xp(Skill.CRAFTING, 20000).xp(Skill.FIREMAKING, 20000)
                .unlock("Access to the Necropolis and the Tombs of Amascut")
                .stage("I can start this quest by speaking to the Priest of Tumeken near the Necropolis, south of Sophanem. (Requires Contact! and more.)")
                .talk("tumeken_priest").say("The Ruins of Unkah are cursed with sand, and the cult of Amascut stirs. Will you investigate the Necropolis?")
                .stage("I should investigate the ruins beneath the sands.")
                .reach("necropolis", 10).say("* Beneath the sand you find a temple to Scabaras... and a monstrous champion awakens!")
                .stage("I must defeat the Champion of Scabaras.")
                .kill("champion_of_scabaras").say("* The Champion of Scabaras falls. The curse on Unkah lifts.")
                .stage("I should report to the Priest of Tumeken.")
                .talk("tumeken_priest").say("The curse is broken! The Necropolis is open, and Amascut's tombs await the brave.");
    }

    private static void swanSong(QuestBook b) {
        b.npc("herman_caranos", "Herman Caranos", "piscatoris", 0, 3, 0xE0B8B4, 0x3A5A8E, 0x2A3A5E, 0xA0A0A5);
        b.npc("franklin_caranos", "Franklin Caranos", "piscatoris", 3, -2, 0xE0B8B5, 0x6A5A40, 0x4A3A30, 0x6A4A23);
        b.boss(BossDef.of("sea_troll_queen", "Sea Troll Queen").level(170).stats(200, 160, 160, 140, 140, 1).speed(5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 25, 3, 6))
                .attack(AttackDef.of("water_blast", AttackType.MAGIC, 22, 10, 4).particle("minecraft:splash"))
                .drops(Drop.always("big_bones")).size(1.2f, 2.6f, 1.0f).at("piscatoris").arena(12, "water"));
        b.quest("swan_song", "Swan Song", Difficulty.MASTER, 2)
                .qp(100).reqQuest("garden_of_tranquillity").reqQuest("one_small_favour")
                .req(Skill.MAGIC, 66).req(Skill.COOKING, 62).req(Skill.FISHING, 62).req(Skill.SMITHING, 45).req(Skill.FIREMAKING, 42).req(Skill.CRAFTING, 40)
                .xp(Skill.MAGIC, 15000).xp(Skill.HITPOINTS, 10000)
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
        b.item("sacred_oil", "Sacred oil", "potion", 0xE0E0A0, "Blessed olive oil, for the pyre.");
        b.quest("shades_of_mortton", "Shades of Mort'ton", Difficulty.INTERMEDIATE, 3)
                .reqQuest("priest_in_peril").req(Skill.CRAFTING, 20).req(Skill.HERBLORE, 15).req(Skill.FIREMAKING, 5)
                .xp(Skill.CRAFTING, 2000).xp(Skill.HERBLORE, 2000).xp(Skill.HITPOINTS, 2000)
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

    private static void kourend(QuestBook b) {
        b.npc("lord_kandur_hosidius", "Lord Kandur Hosidius", "hosidius", 0, 3, 0xE8C0C4, 0x3A8A3A, 0x2A5A2A, 0xE0C053);
        b.npc("lady_shauna_piscarilius", "Lady Shauna Piscarilius", "great_kourend", 3, -2, 0xF0D0C3, 0x3A5A9B, 0x2A3A6B, 0xC08A3F);
        b.npc("lord_shiro_shayzien", "Lord Shiro Shayzien", "shayzien", 0, 3, 0xC8986D, 0xA02A2C, 0x5A1A1C, 0x2A2A30);
        b.npc("lord_kandur_lovakengj", "Lord Kandur Lovakengj", "lovakengj", 0, 3, 0xD8A88D, 0x6A6A6C, 0x3A3A3F, 0xC07A30);
        b.npc("lord_trobin_arceuus", "Lord Trobin Arceuus", "arceuus", 0, 3, 0xE8D0C3, 0x5A2A8D, 0x3A1A5D, 0xE0E0EF);
        b.npc("kaht_bbalam", "Kaht B'alam", "kebos_lowlands", 2, 2, 0x8A9A6B, 0x3A6A3E, 0x2A4A2F, 0x6A8A4B);
        b.npc("councillor_andrews", "Councillor Andrews", "great_kourend", -3, -2, 0xE0B8B7, 0x2A2A2E, 0x1A1A1F, 0xA0A0A6);
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
                .reqQuest("client_of_kourend").req(Skill.AGILITY, 18).xp(Skill.AGILITY, 1500).xp(Skill.HITPOINTS, 1500)
                .stage("I can start this quest by speaking to Lord Kandur Hosidius in Hosidius. (Requires Client of Kourend, 18 Agility.)")
                .talk("lord_kandur_hosidius").say("An ancient Hosidius heir, Lady Arceuus? No... a relic of our house was lost in the Crabclaw Caves. Find it.")
                .stage("I should search for the lost Hosidius relic beneath Hosidius.")
                .reach("hosidius", 10).say("* In the caves beneath Hosidius you find the relic: the Book of the Dead's lost pages.")
                .stage("I should return to Lord Kandur Hosidius.")
                .talk("lord_kandur_hosidius").say("The relic is home! Hosidius is grateful.");
        b.quest("the_queen_of_thieves", "The Queen of Thieves", Difficulty.NOVICE, 1)
                .reqQuest("client_of_kourend").req(Skill.THIEVING, 20).xp(Skill.THIEVING, 2000)
                .stage("I can start this quest by speaking to Lady Shauna Piscarilius in Port Piscarilius. (Requires Client of Kourend, 20 Thieving.)")
                .talk("lady_shauna_piscarilius").say("Councillor Hughes is corrupt, and only a thief can prove it. The Queen of Thieves runs the Piscarilius underworld. Help her, and she'll help us.")
                .stage("I should steal Councillor Hughes's ledger from his house.")
                .reach("great_kourend", 8).say("* You sneak into Hughes's house and copy his ledger: proof of bribery!")
                .stage("I should take the evidence to Lady Shauna.")
                .talk("lady_shauna_piscarilius").say("Hughes is finished! The Queen of Thieves sends her regards. So do I.");
        b.quest("tale_of_the_righteous", "Tale of the Righteous", Difficulty.NOVICE, 1)
                .reqQuest("client_of_kourend").req(Skill.STRENGTH, 16).req(Skill.MINING, 10)
                .xp(Skill.STRENGTH, 8000).xp(Skill.MINING, 500)
                .stage("I can start this quest by speaking to Lord Shiro Shayzien in Shayzien. (Requires Client of Kourend, 16 Strength, 10 Mining.)")
                .talk("lord_shiro_shayzien").say("A historian discovered that a lizardman corrupted by the old shrine threatens our soldiers. Investigate Lizardman Canyon.")
                .stage("I must defeat the corrupt lizardman in Lizardman Canyon.")
                .kill("corrupt_lizardman").say("* The corrupt lizardman falls. The shrine's dark influence fades.")
                .stage("I should report to Lord Shiro Shayzien.")
                .talk("lord_shiro_shayzien").say("Shayzien is safer for your bravery. Thank you.");
        b.quest("the_forsaken_tower", "The Forsaken Tower", Difficulty.NOVICE, 1)
                .reqQuest("client_of_kourend").xp(Skill.MINING, 500).xp(Skill.SMITHING, 500)
                .stage("I can start this quest by speaking to Lord Kandur Lovakengj in Lovakengj. (Requires Client of Kourend.)")
                .talk("lord_kandur_lovakengj").give("lovakengj_plans").say("The old Forsaken Tower could power our forges again. Get its machinery working: here are the plans.")
                .stage("I should restore the Forsaken Tower's power: 4 coal and 2 steel bars for the furnace and generator.")
                .reach("forsaken_tower", 8).take("lovakengj_plans", "coal:4", "steel_bar:2").say("* You fix the furnace, solve the power puzzle and the tower hums to life.")
                .stage("I should report to Lord Kandur Lovakengj.")
                .talk("lord_kandur_lovakengj").say("The tower's power flows to our forges! Lovakengj thanks you.");
        b.quest("the_ascent_of_arceuus", "The Ascent of Arceuus", Difficulty.NOVICE, 1)
                .reqQuest("client_of_kourend").req(Skill.HUNTER, 12).xp(Skill.RUNECRAFT, 1500).xp(Skill.HUNTER, 500)
                .stage("I can start this quest by speaking to Lord Trobin Arceuus in Arceuus. (Requires Client of Kourend, 12 Hunter.)")
                .talk("lord_trobin_arceuus").say("Strange tormented souls are rising near the Dark Altar. My house's magic may be to blame. Investigate.")
                .stage("I should investigate the Dark Altar and collect dark essence fragments.")
                .reach("arceuus", 10).give("dark_essence_fragment:10").say("* Near the Dark Altar you trap the tormented souls and gather dark essence fragments.")
                .stage("I should bring the fragments to Lord Trobin.")
                .talk("lord_trobin_arceuus").take("dark_essence_fragment:10").say("The souls are at rest. Arceuus's magic is safe... for now.");
        b.quest("getting_ahead", "Getting Ahead", Difficulty.INTERMEDIATE, 1)
                .reqQuest("client_of_kourend").req(Skill.CRAFTING, 30).req(Skill.CONSTRUCTION, 26)
                .xp(Skill.CRAFTING, 4000).xp(Skill.CONSTRUCTION, 3200)
                .stage("I can start this quest by speaking to Councillor Andrews in Port Piscarilius, who needs help for the Kebos farmers. (Requires Client of Kourend, 30 Crafting, 26 Construction.)")
                .talk("councillor_andrews").say("A beast terrorises the farms of Kebos. The farmers want to scare it off with a fake head. Help them build one.")
                .stage("I should build a scary fake beast head (2 planks, clay and a ball of wool).")
                .talk("councillor_andrews").take("plank:2", "clay", "ball_of_wool").say("*You craft a terrifying head and mount it on the farm gate.* The beast flees!")
                .missing("We need 2 planks, some clay and a ball of wool for the head.")
                .stage("I should report the beast is gone.")
                .talk("councillor_andrews").give("kourend_favour_letter").say("The farms are safe! Take this letter of thanks from the council.");
        b.quest("a_kingdom_divided", "A Kingdom Divided", Difficulty.EXPERIENCED, 2)
                .reqQuest("the_depths_of_despair", "the_queen_of_thieves", "tale_of_the_righteous", "the_forsaken_tower", "the_ascent_of_arceuus", "getting_ahead")
                .req(Skill.AGILITY, 54).req(Skill.THIEVING, 52).req(Skill.WOODCUTTING, 52).req(Skill.HERBLORE, 50)
                .req(Skill.MINING, 42).req(Skill.CRAFTING, 38).req(Skill.MAGIC, 35)
                .xp(Skill.AGILITY, 10000).xp(Skill.THIEVING, 10000).xp(Skill.WOODCUTTING, 10000).xp(Skill.HERBLORE, 10000)
                .unlock("The Kourend Council's favour and the Kebos Lowlands")
                .stage("I can start this quest by speaking to Councillor Andrews in Port Piscarilius. (Requires all five Kourend house quests and more.)")
                .talk("councillor_andrews").say("The Kourend Council is being picked off one by one. Someone wants the throne. Investigate the attacks.")
                .stage("I should speak to Kaht B'alam, the lizardman shaman in the Kebos Lowlands, about the dark magic behind the attacks.")
                .talk("kaht_bbalam").say("The Judge of Yama, a demon of the void, has been summoned. Its servant hides among your council. Face it, and the kingdom may unite.")
                .stage("I must defeat the Judge of Yama in the Kebos Lowlands.")
                .kill("judge_of_yama").say("* The Judge of Yama is banished back to the void!")
                .stage("I should report to Councillor Andrews.")
                .talk("councillor_andrews").take("kourend_favour_letter").say("The council is safe and the five houses united. Great Kourend is in your debt.")
                .missing("Bring the council's royal letter back as proof of your service.");
    }
}
