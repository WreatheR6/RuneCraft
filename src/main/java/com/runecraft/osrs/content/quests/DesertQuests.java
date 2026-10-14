package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/**
 * Desert and archaeology quests: The Dig Site, Temple of Ikov, The Tourist Trap, Desert Treasure I,
 * Bone Voyage and Temple of the Eye.
 */
final class DesertQuests {
    private DesertQuests() {
    }

    static void register(QuestBook b) {
        b.place("exam_centre", "Exam Centre", 3362, 3337, PlaceDef.Style.TOWN);
        b.place("dig_site", "Digsite", 3370, 3420, PlaceDef.Style.RUINS);
        b.spot("flying_horse_inn", "Flying Horse Inn", 2574, 3323);
        b.place("temple_of_ikov", "Temple of Ikov", 2675, 3405, PlaceDef.Style.CAVE);
        b.spot("shantay_pass", "Shantay Pass", 3302, 3115);
        b.place("desert_mining_camp", "Desert Mining Camp", 3285, 3025, PlaceDef.Style.CAMP);
        b.place("bandit_camp", "Bandit Camp", 3171, 2976, PlaceDef.Style.CAMP);
        b.place("quest_jaldraocht_pyramid", "Jaldraocht Pyramid", 3233, 2898, PlaceDef.Style.RUINS);
        b.place("ice_path", "The Ice Path", 2832, 3807, PlaceDef.Style.CAVE);
        b.place("dessous_graveyard", "Morytania graveyard", 3567, 3403, PlaceDef.Style.RUINS);
        b.place("smoke_dungeon", "Smoke Dungeon", 3310, 2962, PlaceDef.Style.CAVE);
        b.place("shadow_dungeon", "Shadow Dungeon", 2547, 3421, PlaceDef.Style.CAVE);
        b.spot("rasolo", "Rasolo's camp", 2532, 3438);
        b.spot("trollheim_ice", "Trollheim's icy slopes", 2836, 3740);
        b.spot("digsite_barge", "Digsite barge", 3364, 3445);
        b.place("fossil_island", "Fossil Island", 3725, 3810, PlaceDef.Style.CAMP);
        b.spot("varrock_chaos_temple", "Varrock's Zamorakian temple", 3260, 3385);
        b.place("temple_of_the_eye", "Temple of the Eye", 3105, 3175, PlaceDef.Style.RUINS);
        digSite(b);
        templeOfIkov(b);
        touristTrap(b);
        desertTreasure(b);
        boneVoyage(b);
        templeOfTheEye(b);
    }

    private static void digSite(QuestBook b) {
        b.npc("examiner", "Examiner", "exam_centre", 0, 2, 0xE8C8A9, 0x5A3A2A, 0x3A2A1A, 0xA0A0A0);
        b.npc("archaeological_expert", "Archaeological expert", "exam_centre", 3, -2, 0xE0C0A0, 0x8A7A5A, 0x5A4A3A, 0xC0B080);
        b.npc("doug_deeping", "Doug Deeping", "dig_site", 2, 2, 0xD8B090, 0x8A5A2A, 0x5A3A1A, 0x6A4A2A);
        b.item("ancient_talisman", "Ancient talisman", "talisman", 0xA08A4A, "An unusual ancient talisman.");
        b.item("explosive_compound", "Explosive compound", "vial", 0xE0E080, "A dangerously unstable chemical compound.");
        b.item("stone_tablet", "Stone tablet", "note", 0x8A8A8A, "An ancient tablet carved with the history of Zaros.");
        b.item("unstamped_letter", "Unstamped letter", "note", 0xE8E0C8, "A letter of recommendation, not yet stamped.");
        b.item("sealed_letter", "Sealed letter", "note", 0xE8D8B0, "A letter of recommendation, sealed by the Varrock Museum curator.");
        b.item("teddy", "Teddy", "creature", 0xA06A3A, "A cuddly teddy bear.");
        b.item("special_cup", "Special cup", "pot", 0xE0C060, "A special cup, found while panning.");
        b.item("animal_skull", "Animal skull", "bones", 0xE0D8C0, "A skull of a dead animal.");
        b.item("specimen_jar", "Specimen jar", "vial", 0xC0D8E0, "A jar for holding soil samples.");
        b.item("charcoal", "Charcoal", "ore", 0x2A2A2A, "Hmm, a bit of charcoal.");
        b.item("arcenia_root", "Arcenia root", "herb", 0x6A8A3A, "The root of an arcenia plant.");
        b.item("chemical_powder", "Chemical powder", "pot", 0xE8E8E8, "An unidentified white powder.");
        b.item("unidentified_liquid", "Unidentified liquid", "vial", 0xD0E0C0, "An unidentified liquid.");
        b.item("invitation_letter", "Invitation letter", "note", 0xE0D8C0, "An invitation to use the private dig shafts.");
        // Quest-helper / wiki tiles around the Digsite.
        b.spot("exam_centre_cupboard", "Exam Centre cupboard", 3355, 3332);
        b.spot("digsite_bushes", "Bushes north of the Exam Centre", 3357, 3372);
        b.spot("digsite_panning", "Digsite panning point", 3384, 3381);
        b.spot("digsite_specimen_tray", "Digsite specimen tray", 3367, 3381); // approx
        b.spot("digsite_chest_tent", "Digsite tent chest", 3374, 3378);
        b.spot("digsite_barrel", "Digsite barrel", 3364, 3378);
        b.spot("digsite_west_winch", "Digsite west winch", 3353, 3417);
        b.spot("digsite_student_west", "Digsite (north-west)", 3345, 3425);
        b.spot("digsite_student_east", "Digsite (north-east)", 3369, 3419);
        b.spot("digsite_student_south", "Digsite (south)", 3362, 3398);
        b.npc("digsite_student_female", "Student", "digsite_student_west", 0, 0, 0xF0C8A8, 0xC05A8A, 0x3A3A6A, 0x8A5A2A);
        b.npc("digsite_student_orange", "Student", "digsite_student_east", 0, 0, 0xE0B898, 0xE08A2A, 0x3A3A3A, 0x2A1A0A);
        b.npc("digsite_student_green", "Student", "digsite_student_south", 0, 0, 0xD8B090, 0x3A8A3A, 0x3A3A3A, 0x6A4A2A);
        b.npc("panning_guide", "Panning guide", "digsite_panning", 1, 3, 0xE0C0A0, 0x6A5A3A, 0x4A3A2A, 0x8A6A3A);
        b.npc("quest_digsite_workman", "Digsite workman", "digsite_student_south", 6, -6, 0xD0A880, 0xC08A2A, 0x4A3A2A, 0xE0C030);
        b.quest("the_dig_site", "The Dig Site", Difficulty.INTERMEDIATE, 2)
                .req(Skill.AGILITY, 10).req(Skill.HERBLORE, 10).req(Skill.THIEVING, 25)
                .xp(Skill.MINING, 15300).xp(Skill.HERBLORE, 2000).reward("gold_bar:2")
                .stage("I can start this quest by speaking to the Examiner at the Exam Centre north of Al Kharid. (Requires 10 Agility, 10 Herblore, 25 Thieving.)")
                .talk("examiner").give("unstamped_letter")
                .say("You want to dig at the Digsite? First you must pass three Earth Sciences exams.",
                        "Take this letter to Curator Haig Halen at the Varrock Museum to have it stamped. Then study the students at the site.")
                .stage("I should have my letter stamped by the Varrock Museum curator, help the three students at the Digsite study, then pass the first exam.")
                .talk("curator_haig_halen").stay().unless("sealed_letter").take("unstamped_letter").give("sealed_letter")
                .say("A letter of recommendation for the Exam Centre? Certainly. *He stamps and seals it.*")
                .reach("digsite_bushes", 3).stay().unless("teddy").unlessFlag("ds_female1").give("teddy")
                .say("* Something is caught in the bushes: a teddy bear!")
                .talk("panning_guide").stay().unlessFlag("ds_tea").take("cup_of_tea").flag("ds_tea")
                .say("Ahh, a cup of tea! Thank you. You may pan in the river; there's a panning tray in the tent.")
                .missing("You'll need permission to pan here. I could murder a cup of tea, mind.")
                .reach("digsite_panning", 3).stay().needFlag("ds_tea").unless("special_cup").unlessFlag("ds_orange1").give("special_cup")
                .say("* You pick up a panning tray from the tent and pan the river. Among the mud there's a special cup!")
                .talk("quest_digsite_workman").stay().reqSkill(Skill.THIEVING, 25).unless("animal_skull").unlessFlag("ds_green1")
                .give("animal_skull", "specimen_brush")
                .say("* You pick the workman's pocket and find an animal skull and a specimen brush.")
                .talk("digsite_student_female").stay().unlessFlag("ds_female1").take("teddy").flag("ds_female1")
                .say("My lucky mascot! The first exam asks what the proper procedure is at the site... visitors must report to the Examiner.")
                .missing("I've lost my lucky teddy somewhere near the Exam Centre. I can't study without it.")
                .talk("digsite_student_orange").stay().unlessFlag("ds_orange1").take("special_cup").flag("ds_orange1")
                .say("My cup! I dropped it panning. The answer you want: finds must be handled carefully, with gloves.")
                .missing("I lost my special cup in the river while panning. Find it and I'll share my notes.")
                .talk("digsite_student_green").stay().unlessFlag("ds_green1").take("animal_skull").flag("ds_green1")
                .say("That's the skull a workman stole from me! Thanks. Remember: always use a trowel to remove soil.")
                .missing("One of the workmen pinched my animal skull. They're light-fingered... and so might you be.")
                .talk("examiner").needFlag("ds_female1", "ds_orange1", "ds_green1").take("sealed_letter")
                .say("A sealed letter from the curator. Very well.",
                        "Question one: what must all visitors to the Digsite do? > Report to the Examiner.", "Correct! You pass level one.")
                .missing("Bring me the curator's sealed letter, and make sure you've studied with the students before you sit the exam.")
                .stage("I should study with the students again and pass the second Earth Sciences exam.")
                .talk("digsite_student_female").stay().unlessFlag("ds_female2").flag("ds_female2")
                .say("Exam two? Finds should be cleaned with a specimen brush.")
                .talk("digsite_student_orange").stay().unlessFlag("ds_orange2").flag("ds_orange2")
                .say("Samples go in a specimen jar, of course.")
                .talk("digsite_student_green").stay().unlessFlag("ds_green2").flag("ds_green2")
                .say("Rock picks are for splitting rocks. And always wear gloves and boots.")
                .talk("examiner").needFlag("ds_female2", "ds_orange2", "ds_green2")
                .say("Question two: what is the proper way to handle finds? > Carefully, with gloves.", "Correct! Level two passed.")
                .missing("You're not ready. Study with the students first.")
                .stage("I should study with the students one last time (one wants an opal) and pass the third Earth Sciences exam.")
                .talk("digsite_student_female").stay().unlessFlag("ds_female3").take("opal").flag("ds_female3")
                .say("An opal! Lovely. For exam three: specimens are dated by the layers of soil they're found in.")
                .missing("I'll help you with the last exam... for an opal. You might find one panning in the river.")
                .talk("digsite_student_orange").stay().unlessFlag("ds_orange3").flag("ds_orange3")
                .say("The level three exam? Study the different layers of soil.")
                .talk("digsite_student_green").stay().unlessFlag("ds_green3").flag("ds_green3")
                .say("Gneiss, basalt and sandstone. Learn them!")
                .talk("examiner").needFlag("ds_female3", "ds_orange3", "ds_green3")
                .say("Final question: what should you use to carefully remove soil? > A trowel.", "Excellent! You may now dig at the site.")
                .missing("You're not ready for the final exam. Study with the students first.")
                .stage("I should dig at the Digsite with a trowel and specimen brush and show any finds to the Archaeological expert.")
                .reach("dig_site", 10).stay().need("trowel", "specimen_brush").unless("ancient_talisman").give("ancient_talisman")
                .say("* You carefully dig in the level 3 site and brush off an unusual talisman.")
                .missing("* You need a trowel to dig and a specimen brush to clean any finds.")
                .talk("archaeological_expert").take("ancient_talisman").give("invitation_letter")
                .say("Zarosian! Very rare. Here, an invitation to use the private dig shafts. Doug Deeping is blocked by rocks in the deeper shaft.")
                .stage("I should make an explosive compound (the Archaeological expert can identify chemicals from the Digsite) and blow up the rocks in the deep shaft.")
                .talk("doug_deeping").stay().unlessFlag("ds_doug").flag("ds_doug").give("chest_key")
                .say("Rocks blocking the shaft? You'd need an explosive. There's a chest of chemicals in the tent; here's the key.",
                        "And don't forget the arcenia root growing down the west shaft. It binds the compound together.")
                .reach("digsite_west_winch", 3).stay().need("rope", "invitation_letter").unless("arcenia_root", "explosive_compound")
                .give("arcenia_root")
                .say("* You show the workman your invitation, tie your rope to the winch and climb down. You pick some arcenia root.")
                .reach("digsite_chest_tent", 3).stay().take("chest_key").unless("chemical_powder").give("chemical_powder")
                .say("* You unlock the chest in the tent and take a jar of white powder.")
                .reach("digsite_barrel", 3).stay().take("vial").unless("unidentified_liquid").give("unidentified_liquid")
                .say("* You fill a vial with liquid from the barrel. Better handle it carefully.")
                .reach("exam_centre_cupboard", 2).stay().unless("specimen_jar", "charcoal", "explosive_compound").give("specimen_jar")
                .say("* You search the cupboard in the Exam Centre and find a specimen jar.")
                .reach("digsite_specimen_tray", 3).stay().need("specimen_jar").unless("charcoal", "explosive_compound").give("charcoal")
                .say("* You sift through the specimen tray and find a lump of charcoal.")
                .talk("archaeological_expert").stay().unless("explosive_compound")
                .take("chemical_powder", "unidentified_liquid", "charcoal", "arcenia_root").need("pestle_and_mortar").give("explosive_compound")
                .say("That powder is ammonium nitrate and the liquid... nitroglycerin! Careful!",
                        "* You mix the two, add charcoal ground with your pestle and mortar, then the arcenia root. You have an explosive compound.")
                .missing("Bring me any chemicals you find and I'll identify them. You'll also want charcoal, arcenia root and a pestle and mortar.")
                .reach("digsite_west_winch", 3).take("explosive_compound").need("tinderbox").give("stone_tablet")
                .say("* You pour the compound on the bricks blocking the shaft and light it. BOOM!",
                        "* The rocks collapse, revealing an ancient chamber with a stone tablet.")
                .stage("I should take the stone tablet to the Archaeological expert.")
                .talk("archaeological_expert").take("stone_tablet").say("A Zarosian tablet! This is the find of the century. Please accept this reward.");
    }

    private static void templeOfIkov(QuestBook b) {
        b.npc("lucien", "Lucien", "flying_horse_inn", 0, 0, 0xD8C8C8, 0x2A1A3A, 0x1A0A2A, 0x1A1A1A);
        b.npc("guardian_of_armadyl", "Guardian of Armadyl", "temple_of_ikov", 3, 3, 0xE8D0B0, 0xE0E0E8, 0x8A8AA0, 0xE0C040);
        b.npc("winelda", "Winelda", "temple_of_ikov", -3, 3, 0xC0C0A0, 0x3A2A4A, 0x2A1A3A, 0x2A2A2A);
        b.item("armadyl_pendant", "Pendant of Armadyl", "amulet", 0xE0E0F0, "The pendant of Armadyl's guardians.");
        b.item("ice_arrows", "Ice arrows", "arrow", 0x9AD0F0, "Arrows enchanted with ice. Deadly to fire creatures.");
        b.item("staff_of_armadyl", "Staff of Armadyl", "staff_orb", 0xE0E0F0, "A sacred staff of Armadyl.");
        b.boss(BossDef.of("fire_warrior_of_lesarkus", "Fire Warrior of Lesarkus").level(84).stats(59, 70, 80, 70, 1, 1).speed(4)
                .attack(AttackDef.of("burning_blade", AttackType.SLASH, 9, 2.5, 10).particle("minecraft:flame"))
                .drops(Drop.always("bones")).skin(0xE06A1A, 0xA02A0A, 0x6A1A0A, 0xFFB040).at("temple_of_ikov").arena(8, "lava"));
        b.quest("temple_of_ikov", "Temple of Ikov", Difficulty.INTERMEDIATE, 1)
                .req(Skill.THIEVING, 42).req(Skill.RANGED, 40).xp(Skill.RANGED, 10500).xp(Skill.FLETCHING, 8000)
                .stage("I can start this quest by speaking to Lucien in the Flying Horse Inn, East Ardougne. (Requires 42 Thieving, 40 Ranged.)")
                .talk("lucien").say("I need the Staff of Armadyl from the Temple of Ikov. I cannot enter myself... but you can. I'll reward you well.")
                .stage("I should enter the Temple of Ikov. A fire warrior guards the inner temple; Winelda the witch can make ice arrows.")
                .talk("winelda").stay().unless("ice_arrows").take("limpwurt_root:1").give("ice_arrows:20")
                .say("Ice arrows? Bring me a limpwurt root and I'll enchant some for you.")
                .missing("Ice arrows need a limpwurt root.")
                .kill("fire_warrior_of_lesarkus").say("* The Fire Warrior of Lesarkus is extinguished by your ice arrows.")
                .stage("I should find the Staff of Armadyl in the temple.")
                .talk("guardian_of_armadyl").give("staff_of_armadyl", "armadyl_pendant")
                .say("Stop! Lucien is a servant of Zamorak and seeks the staff for evil. Take the staff to keep it from him, and wear our pendant.")
                .stage("I've learned Lucien is evil. Wearing the Pendant of Armadyl, I should confront him and return the staff to the guardians.")
                .talk("lucien").need("armadyl_pendant").say("That pendant! You've been talking to those guardians. Curse you! *Lucien teleports away.*")
                .stage("I should return the staff to the Guardians of Armadyl.")
                .talk("guardian_of_armadyl").take("staff_of_armadyl").say("The staff is safe. Armadyl thanks you. Lucien's plans are thwarted, for now.");
    }

    private static void touristTrap(QuestBook b) {
        b.npc("irena", "Irena", "shantay_pass", 0, 0, 0xC8986A, 0xE0C080, 0xB09060, 0x2A1A0A);
        b.npc("captain_siad", "Captain Siad", "desert_mining_camp", 3, 3, 0xC08A5A, 0x6A2A1A, 0x3A1A0A, 0xE0E0E0);
        b.npc("ana", "Ana", "desert_mining_camp", -3, 3, 0xC8986B, 0xA08A6A, 0x6A5A3A, 0x2A1A0B);
        b.npc("al_shabim", "Al Shabim", "bandit_camp", 4, -4, 0xB07A4A, 0xF0F0E0, 0xD0C0A0, 0xF0F0F0);
        b.item("metal_key", "Metal key", "key", 0x8A8A8A, "Opens the gate of the Desert Mining Camp.");
        b.item("desert_disguise", "Desert disguise", "cloth", 0xD0B080, "Slave robes. Perfect for blending in.");
        b.item("ana_in_a_barrel", "Ana in a barrel", "pot", 0x8A6A3A, "Ana, stuffed into a barrel. She's not happy.");
        b.boss(BossDef.of("mercenary_captain", "Mercenary Captain").level(47).stats(80, 40, 50, 40, 1, 1).speed(4)
                .attack(AttackDef.of("scimitar", AttackType.SLASH, 8, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xC08A5A, 0x6A5A3A, 0x3A2A1A, 0x2A2A2A).at("desert_mining_camp").arena(8, "sand"));
        b.quest("the_tourist_trap", "The Tourist Trap", Difficulty.INTERMEDIATE, 2)
                .req(Skill.FLETCHING, 10).req(Skill.SMITHING, 20).xp(Skill.AGILITY, 4650).xp(Skill.THIEVING, 4650)
                .stage("I can start this quest by speaking to Irena at the Shantay Pass. (Requires 10 Fletching, 20 Smithing.)")
                .talk("irena").say("My daughter Ana was kidnapped and forced to work in the Desert Mining Camp! Please bring her home.")
                .stage("I should get into the Desert Mining Camp. The Mercenary Captain has the gate key.")
                .kill("mercenary_captain").give("metal_key").say("* The Mercenary Captain drops the metal key to the camp.")
                .stage("I need a disguise. Al Shabim at the Bedabin camp might have something.")
                .talk("al_shabim").give("desert_disguise").say("Help our tribe forge a prototype dart and we'll help you. *You smith a fine dart.* Here, slave robes. Blend in.")
                .stage("Disguised, I should find Ana in the camp and get Captain Siad out of the way.")
                .talk("captain_siad").need("desert_disguise", "metal_key").say("Hm? What are you looking at? *You distract him with talk of sailing while you study the cart schedule.*")
                .stage("I should smuggle Ana out of the camp in a barrel on the mine cart.")
                .talk("ana").take("metal_key", "desert_disguise").give("ana_in_a_barrel").say("You're here to rescue me? In a barrel? Ugh, fine. Get me out of here!")
                .stage("I should take Ana back to Irena.")
                .talk("irena").take("ana_in_a_barrel").say("Ana! My daughter! Thank you, thank you!");
    }

    private static void desertTreasure(QuestBook b) {
        b.npc("archaeologist", "Archaeologist", "bandit_camp", 0, 3, 0xE0C0A1, 0x8A7A5B, 0x5A4A3B, 0xC0B081);
        b.npc("eblis", "Eblis", "bandit_camp", -3, -3, 0xB8885A, 0x5A3A2A, 0x3A2A1A, 0x2A2A2A);
        b.npc("rasolo", "Rasolo", "rasolo", 0, 0, 0xC8A070, 0x5A2A5A, 0x3A1A3A, 0x8A6A3A);
        b.npc("malak", "Malak", "CANIFIS", 6, 6, 0xC8C8D0, 0x3A0A1A, 0x1A0A0A, 0x1A1A1A);
        b.npc("troll_child", "Troll child", "trollheim_ice", 0, 0, 0x7A8A6A, 0x5A6A4A, 0x4A5A3A, 0x6A7A5A);
        b.npc("azzanadra", "Azzanadra", "quest_jaldraocht_pyramid", 0, 3, 0x6A3A6A, 0x3A1A4A, 0x2A0A2A, 0xC0A040);
        b.item("ancient_diary", "Ancient diary", "book", 0x7A5A3A, "The Archaeologist's translation of an ancient diary.");
        b.item("smoke_diamond", "Smoke diamond", "gem", 0x8A8A8A, "One of the four Diamonds of Azzanadra.");
        b.item("shadow_diamond", "Shadow diamond", "gem", 0x2A2A3A, "One of the four Diamonds of Azzanadra.");
        b.item("ice_diamond", "Ice diamond", "gem", 0xA0D8F0, "One of the four Diamonds of Azzanadra.");
        b.item("blood_diamond", "Blood diamond", "gem", 0xA01A1A, "One of the four Diamonds of Azzanadra.");
        b.boss(BossDef.of("kamil", "Kamil").level(154).stats(130, 140, 130, 120, 80, 1).speed(4)
                .defenceBonus(25, 25, 25, 0, 25)
                .attack(AttackDef.of("ice_sword", AttackType.SLASH, 24, 2.5, 6))
                .attack(AttackDef.of("ice_barrage", AttackType.MAGIC, 16, 8, 4).special(Special.FREEZE, 8)
                        .telegraph(1, "Kamil casts a freezing spell!").particle("minecraft:snowflake"))
                .drops(Drop.always("bones")).skin(0xC0E0F0, 0x2A3A6A, 0x1A2A4A, 0xE0F0FF).at("ice_path").arena(10, "ice"));
        b.boss(BossDef.of("dessous", "Dessous").level(139).stats(200, 110, 110, 100, 100, 1).speed(4)
                .defenceBonus(50, 50, 50, 0, 50)
                .attack(AttackDef.of("vampyric_slash", AttackType.SLASH, 26, 2.5, 6).special(Special.LIFESTEAL, 25))
                .attack(AttackDef.of("blood_spell", AttackType.MAGIC, 20, 8, 3).special(Special.LIFESTEAL, 50).particle("minecraft:crimson_spore"))
                .drops(Drop.always("bones")).size(0.8f, 2.4f, 1.0f).at("dessous_graveyard").arena(10, "crypt"));
        b.boss(BossDef.of("fareed", "Fareed").level(167).stats(130, 120, 130, 130, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, -30, 60)
                .attack(AttackDef.of("flaming_scimitar", AttackType.SLASH, 26, 2.5, 8).particle("minecraft:flame"))
                .attack(AttackDef.of("fire_burst", AttackType.MAGIC, 18, 5, 3).special(Special.AOE, 3).particle("minecraft:flame"))
                .drops(Drop.always("bones")).skin(0xE06A1A, 0xA03A0A, 0x5A1A0A, 0xFFB040).at("smoke_dungeon").arena(10, "lava"));
        b.boss(BossDef.of("damis", "Damis").level(103).stats(90, 100, 100, 90, 1, 1).speed(4)
                .defenceBonus(30, 30, 30, 1000, 1000)
                .immune(AttackType.MAGIC, AttackType.RANGED)
                .attack(AttackDef.of("shadow_strike", AttackType.SLASH, 17, 2.5, 10).special(Special.DRAIN_PRAYER, 2))
                .phase(50, "Damis transforms into his second form! (level 174)", 1.6, false,
                        AttackDef.of("shadow_slash", AttackType.SLASH, 29, 2.5, 10).special(Special.DRAIN_PRAYER, 4))
                .drops(Drop.always("bones")).skin(0x2A2A3A, 0x1A1A2A, 0x0A0A1A, 0x6A2A8A).at("shadow_dungeon").arena(10, "void"));
        b.quest("desert_treasure_i", "Desert Treasure I", Difficulty.MASTER, 3)
                .reqQuest("the_dig_site", "temple_of_ikov", "the_tourist_trap", "troll_stronghold", "priest_in_peril", "waterfall_quest")
                .req(Skill.THIEVING, 53).req(Skill.FIREMAKING, 50).req(Skill.MAGIC, 50).req(Skill.SLAYER, 10)
                .xp(Skill.MAGIC, 20000)
                .unlock("The Ancient Magicks spellbook")
                .stage("I can start this quest by speaking to the Archaeologist in the Bandit Camp. (Requires Dig Site, Temple of Ikov, Tourist Trap, Troll Stronghold, Priest in Peril, Waterfall Quest and 53 Thieving, 50 Firemaking, 50 Magic, 10 Slayer.)")
                .talk("archaeologist").say("I've found an ancient diary that speaks of a fabulous treasure in the desert... Eblis, a bandit here, knows the old tongue.")
                .stage("I should ask Eblis about the treasure.")
                .talk("eblis").take("magic_logs:12", "steel_bar:6", "blood_rune", "bones").give("ancient_diary")
                .say("To scry where the four Diamonds of Azzanadra are, I must make mirrors and a ritual fire: 12 magic logs, 6 steel bars, a blood rune and bones.",
                        "*Eblis performs the ritual.* I see... four warriors of Zaros guard the diamonds: Damis of Shadow, Fareed of Smoke, Kamil of Ice and Dessous of Blood.")
                .missing("I need 12 magic logs, 6 steel bars, a blood rune and some bones for the scrying ritual.")
                .stage("I must recover the Smoke, Shadow, Ice and Blood diamonds from Fareed, Damis, Kamil and Dessous.")
                .talk("rasolo").stay().say("Damis hides in the Shadow Dungeon east of the Baxtorian Falls. He can only be fought in melee, and only seen with a ring of visibility.")
                .talk("malak").stay().say("Dessous, the vampyre? He lurks in the graveyard south-east of Canifis. Bring the strongest blows you have.")
                .talk("troll_child").stay().say("Kamil froze my mummy and daddy! He's up the Ice Path, north of Trollheim. Please help!")
                .kill("damis").stay().unless("shadow_diamond").give("shadow_diamond").say("* Damis fades into the shadows, leaving behind the Shadow diamond.")
                .kill("fareed").stay().unless("smoke_diamond").give("smoke_diamond").say("* Fareed's flames gutter out. You take the Smoke diamond.")
                .kill("kamil").stay().unless("ice_diamond").give("ice_diamond").say("* Kamil shatters. The troll child's parents are freed, and you take the Ice diamond.")
                .kill("dessous").stay().unless("blood_diamond").give("blood_diamond").say("* Dessous crumbles to dust. You take the Blood diamond.")
                .reach("quest_jaldraocht_pyramid", 10).take("smoke_diamond", "shadow_diamond", "ice_diamond", "blood_diamond")
                .say("* You place the four diamonds in the obelisks around the Jaldraocht Pyramid. The doors grind open...")
                .stage("I should enter the pyramid and speak to whoever was imprisoned within.")
                .talk("azzanadra").say("Free... after millennia! I am Azzanadra, servant of Zaros. You have my gratitude, mortal.",
                        "Pray at the altar in this pyramid and you may use the Ancient Magicks of my lord.")
                .done("azzanadra", "The Ancient Magicks are yours. Use them wisely.");
    }

    private static void boneVoyage(QuestBook b) {
        b.npc("barge_foreman", "Barge foreman", "digsite_barge", 0, 0, 0xE0B899, 0x8A6A2A, 0x4A3A2A, 0xC0A040);
        b.npc("lead_navigator", "Lead navigator", "digsite_barge", 3, 2, 0xE8C0A7, 0x2A3A6A, 0x1A2A4A, 0xE0E0E1);
        b.item("barge_permit", "Barge permit", "note", 0xE0D0A8, "Permission from the Varrock Museum to use the barge.");
        b.item("bone_charm", "Bone charm", "bead", 0xE8E0C8, "A charm made of bone. It might bring luck.");
        b.item("potion_of_sealegs", "Potion of sealegs", "potion", 0x6A8AC0, "Might help with seasickness.");
        b.quest("bone_voyage", "Bone Voyage", Difficulty.INTERMEDIATE, 1)
                .reqQuest("the_dig_site").reqNote("100 Kudos from the Varrock Museum")
                .unlock("Access to Fossil Island")
                .stage("I can start this quest by speaking to Curator Haig Halen in the Varrock Museum. (Requires The Dig Site.)")
                .talk("curator_haig_halen").give("barge_permit").say("We've found a strange island to the north-east, full of fossils! The museum's barge at the Digsite is nearly ready.")
                .stage("I should take the permit to the barge foreman at the Digsite.")
                .talk("barge_foreman").take("barge_permit", "plank:10", "steel_bar:4").say("Planks and steel for the hull! She'll be ready to sail. Talk to the lead navigator.")
                .missing("The barge still needs 10 planks and 4 steel bars.")
                .stage("I should speak to the lead navigator.")
                .talk("odd_old_man").stay().unless("bone_charm").give("bone_charm")
                .say("A charm for a navigator? Ah, take this bone charm. Luck's in the bones, they say.")
                .talk("lead_navigator").stay().unless("potion_of_sealegs").unlessFlag("bv_sealegs").take("vodka:2", "marrentill_potion_unf").give("potion_of_sealegs")
                .say("*The Apothecary in Varrock brews the vodka into your marrentill potion: a potion of sealegs.*")
                .missing("I get terribly seasick. The Apothecary in Varrock could brew something, with 2 vodka and a marrentill potion (unf).")
                .talk("lead_navigator").take("potion_of_sealegs", "bone_charm").travel("fossil_island").say("The junior navigator got us lost last time... I'll steer. Next stop, Fossil Island! *The barge sets sail.*")
                .missing("I need my potion of sealegs, and the junior navigator wants a lucky charm. The Odd Old Man has charms.")
                .finish()
                .done("lead_navigator", "Ready to sail to Fossil Island when you are.");
    }

    private static void templeOfTheEye(QuestBook b) {
        b.npc("wizard_persten", "Wizard Persten", "WIZARDS_TOWER", 5, -3, 0xF0D8C9, 0x6A2A8A, 0x4A1A6A, 0x6A3A1A);
        b.npc("mage_of_zamorak", "Mage of Zamorak", "varrock_chaos_temple", 0, 0, 0xD8B898, 0xA01A1A, 0x5A0A0A, 0x2A0A0A);
        b.npc("apprentice_felix", "Apprentice Felix", "temple_of_the_eye", 2, 2, 0xF0C8AC, 0x3A3AA0, 0x2A2A7A, 0xC06A2A);
        b.item("abyssal_incantation", "Abyssal incantation", "note", 0xA02A2A, "A strange incantation to open a rift.");
        b.quest("temple_of_the_eye", "Temple of the Eye", Difficulty.INTERMEDIATE, 1)
                .reqQuest("RUNE_MYSTERIES").req(Skill.RUNECRAFT, 10).reqQuest("enter_the_abyss")
                .xp(Skill.RUNECRAFT, 5000) // + 4,210 Runecraft XP earned during the quest
                .unlock("Access to Guardians of the Rift")
                .stage("I can start this quest by speaking to Wizard Persten in the Wizards' Tower. (Requires Rune Mysteries, 10 Runecraft.)")
                .talk("wizard_persten").say("I've found references to a lost temple of runecraft. The Zamorakian mages may know something. Visit the Mage of Zamorak in Varrock.")
                .stage("I should speak to the Mage of Zamorak in Varrock.")
                .talk("mage_of_zamorak").take("cup_of_tea").missing("I'll talk, but not before a strong cup of tea.").give("abyssal_incantation").say("You want to find the Temple of the Eye? Use this incantation in the Abyss, and an amulet... I need a strong tea first.")
                .stage("I should use the abyssal incantation near the Wizards' Tower to open a rift to the temple.")
                .reach("temple_of_the_eye", 8).take("abyssal_incantation").say("* The incantation tears a rift in the air. You step through into the ancient Temple of the Eye.")
                .stage("I should help Apprentice Felix and the wizards awaken the Great Guardian.")
                .talk("apprentice_felix").take("pure_essence:20").say("Craft guardian essence for the guardians! *You charge the cells and the Great Guardian awakens.*",
                        "The Great Guardian lives! The wizards can study the rift now. You should tell Persten!")
                .missing("We need runic energy... 20 pure essence should do.")
                .stage("I should report to Wizard Persten.")
                .talk("wizard_persten").say("The Temple of the Eye! You can help the guardians close the rifts any time.");
    }
}
