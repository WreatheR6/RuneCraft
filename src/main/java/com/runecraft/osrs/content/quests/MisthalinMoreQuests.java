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
 * More quests and two miniquests, mostly prerequisites of the big quests: Gertrude's Cat, Making
 * History, Dream Mentor, Client of Kourend, Tears of Guthix, The Feud, Icthlarin's Little Helper,
 * A Tail of Two Cats, The Golem, Shadow of the Storm, What Lies Below, Defender of Varrock, In Aid of
 * the Myreque, Recruitment Drive, The Lost Tribe, Wanted!, Enter the Abyss (miniquest) and Alfred
 * Grimhand's Barcrawl (miniquest).
 */
final class MisthalinMoreQuests {
    private MisthalinMoreQuests() {
    }

    static void register(QuestBook b) {
        b.spot("gertrudes_house", "Gertrude's house", 3151, 3413);
        b.spot("lumber_yard", "Lumber yard", 3305, 3492);
        b.place("rellekka_outpost", "Outpost", 2435, 3347, PlaceDef.Style.RUINS);
        b.place("dream_world_cyrisus", "Dream world (Cyrisus)", 2100, 3880, PlaceDef.Style.ARENA);
        b.place("great_kourend", "Great Kourend (Port Piscarilius)", 1803, 3748, PlaceDef.Style.TOWN);
        b.place("tears_of_guthix_cave", "Tears of Guthix cave", 3252, 3175, PlaceDef.Style.CAVE);
        b.place("sophanem", "Sophanem", 3300, 2780, PlaceDef.Style.TOWN);
        b.place("uzer", "Uzer", 3490, 3090, PlaceDef.Style.RUINS);
        b.place("uzer_golem_room", "Golem's chamber", 3495, 3095, PlaceDef.Style.NONE);
        b.place("demon_temple_storm", "Ruins of Uzer temple", 3480, 3085, PlaceDef.Style.CAVE);
        b.spot("rat_burgiss", "Rat Burgiss's camp", 3267, 3335);
        b.place("chaos_altar_surok", "Surok's lair", 3205, 3460, PlaceDef.Style.CAVE);
        b.place("varrock_sewers_arrav", "Varrock sewers", 3237, 3459, PlaceDef.Style.CAVE);
        b.place("burgh_de_rott", "Burgh de Rott", 3495, 3210, PlaceDef.Style.RUINS);
        b.spot("falador_temple_knights", "Falador Temple Knight training ground", 2996, 3373);
        b.place("dorgesh_kaan_mine", "Lumbridge cellar cave", 3230, 3215, PlaceDef.Style.CAVE);
        b.place("solus_hideout", "Solus Dellagar's hideout", 3018, 3248, PlaceDef.Style.CAVE);
        b.spot("dead_mans_chest", "Dead Man's Chest bar", 2796, 3157);
        b.spot("forester_arms", "Forester's Arms", 2691, 3493);
        b.spot("rising_sun_inn", "Rising Sun Inn", 2956, 3370);
        gertrudesCat(b);
        makingHistory(b);
        dreamMentor(b);
        clientOfKourend(b);
        tearsOfGuthix(b);
        theFeud(b);
        icthlarin(b);
        tailOfTwoCats(b);
        golem(b);
        shadowOfTheStorm(b);
        whatLiesBelow(b);
        defenderOfVarrock(b);
        inAidOfTheMyreque(b);
        recruitmentDrive(b);
        lostTribe(b);
        wanted(b);
        enterTheAbyss(b);
        barcrawl(b);
    }

    private static void gertrudesCat(QuestBook b) {
        b.npc("gertrude", "Gertrude", "gertrudes_house", 0, 0, 0xF0C8B2, 0x8A3A6C, 0x5A2A4C, 0x9A9A9B);
        b.npc("shilop", "Shilop", "VARROCK", -4, -7, 0xF0D0BF, 0x6A8A3A, 0x3A4A2A, 0x8A5A2C);
        b.npc("fluffs", "Fluffs", "lumber_yard", 0, 0, 0xE0A040, 0xD08A3A, 0xC07A2A, 0xE0B050);
        b.item("gertrudes_kitten", "Kitten", "pet", 0xE0B050, "A playful kitten from Fluffs' litter.");
        b.item("fluffs_kittens", "Fluffs' kitten", "pet", 0xE0A041, "One of Fluffs' lost kittens.");
        b.quest("gertrudes_cat", "Gertrude's Cat", Difficulty.NOVICE, 1)
                .xp(Skill.COOKING, 1525).reward("gertrudes_kitten", "cake")
                .stage("I can start this quest by speaking to Gertrude west of Varrock.")
                .talk("gertrude").say("My cat Fluffs is missing! My sons Shilop and Wilough might know where she went. They play in Varrock square.")
                .stage("I should ask Shilop in Varrock where Fluffs is.")
                .talk("shilop").take("coins:100").say("Fluffs? Maybe I know... for 100 coins. She's at the lumber yard north-east of Varrock!")
                .missing("I'll tell you where the cat is. 100 coins!")
                .stage("I should find Fluffs at the lumber yard. She's thirsty and hungry: bring milk and some sardine.")
                .talk("fluffs").take("minecraft:milk_bucket", "raw_sardine").say("*Purr* ...Fluffs laps up the milk and eats the sardine, but won't leave. She mews for her kittens.")
                .missing("*Hiss!* (Fluffs looks thirsty and hungry. Milk and a sardine might help.)")
                .stage("I should find Fluffs' kittens hidden in the lumber yard.")
                .reach("lumber_yard", 8).give("fluffs_kittens").say("* You find a kitten hiding behind the crates.")
                .stage("I should return the kitten to Fluffs, then tell Gertrude.")
                .talk("fluffs").take("fluffs_kittens").say("*Purr!* Fluffs grabs her kitten and runs home.")
                .stage("Fluffs has gone home. I should speak to Gertrude.")
                .talk("gertrude").say("Fluffs is home with her kittens! Please, take one of them, and some cake.");
    }

    private static void makingHistory(QuestBook b) {
        b.npc("historian_minas", "Historian Minas", "varrock_museum", -3, 2, 0xE8C0B7, 0x6A5A3B, 0x4A3A2B, 0xC0C0C3);
        b.npc("jorral", "Jorral", "rellekka_outpost", 0, 2, 0xE0B8A6, 0x3A6A3C, 0x2A4A2D, 0x8A6A3C);
        b.npc("droalak", "Droalak", "PORT_PHASMATYS", 6, -6, 0xC8E8E7, 0x5A5A8A, 0x3A3A6A, 0x9A9A9C);
        b.npc("dron", "Dron", "RELLEKKA", -2, 8, 0xE0B8A7, 0x6A2A2C, 0x3A1A1C, 0xE0A043);
        b.item("enchanted_key", "Enchanted key", "key", 0xC0A0E0, "It gets warmer the closer you get to treasure.");
        b.item("outpost_letter", "Letter", "note", 0xE0D8C2, "A historic letter about the Outpost.");
        b.item("outpost_scroll", "Scroll", "note", 0xE0D8C3, "A scroll from Droalak.");
        b.item("outpost_journal", "Journal", "book", 0x6A5A3A, "A journal about the Outpost's history.");
        b.quest("making_history", "Making History", Difficulty.INTERMEDIATE, 3)
                .reqQuest("priest_in_peril", "RESTLESS_GHOST")
                .xp(Skill.CRAFTING, 1000).xp(Skill.PRAYER, 1000).reward("coins:750")
                .stage("I can start this quest by speaking to Jorral at the Outpost north of West Ardougne. (Requires Priest in Peril, Restless Ghost.)")
                .talk("jorral").say("King Lathas wants to demolish my Outpost! I need proof of its history to save it. Three sources: a ghost, a Fremennik and a buried chest.")
                .stage("I should gather the Outpost's history from Droalak's ghost, Dron of Rellekka and a buried chest.")
                .talk("droalak").stay().need("ghostspeak_amulet").unless("outpost_scroll").give("outpost_scroll")
                .say("The Outpost? I wrote to my love Melina from there. Take this scroll.")
                .talk("dron").stay().unless("outpost_letter").give("outpost_letter")
                .say("My ancestors fought at the Outpost! Here's an old letter. Now leave, I'm busy being scary.")
                .reach("rellekka_outpost", 8).stay().unless("outpost_journal").give("outpost_journal", "enchanted_key")
                .say("* Using the enchanted key, you dig up an old chest near the Outpost with a journal inside.")
                .talk("jorral").take("outpost_scroll", "outpost_letter", "outpost_journal")
                .say("Wonderful! Let me write this up for King Lathas.")
                .missing("I need the letter from Dron, the scroll from Droalak and the journal from the buried chest.")
                .stage("I should take the history to King Lathas.")
                .talk("king_lathas").say("A fascinating history. Very well, the Outpost may stay, and become a museum. Here's a reward.")
                .done("historian_minas", "Jorral's Outpost is a museum now? Splendid! History preserved.");
    }

    private static void dreamMentor(QuestBook b) {
        b.npc("cyrisus", "Cyrisus", "quest_lunar_isle", -3, -2, 0xE8C0B8, 0x8A8AA0, 0x5A5A7A, 0x5A3A1E);
        b.boss(BossDef.of("the_inadequacy", "The Inadequacy").level(343).stats(340, 300, 300, 150, 1, 1).speed(5)
                .defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 24, 3, 6))
                .attack(AttackDef.of("despair", AttackType.MAGIC, 20, 10, 3).special(Special.DRAIN_STAT, Skill.DEFENCE.ordinal()))
                .phase(50, "The Inadequacy: You'll never be good enough!", 1.2, false)
                .drops(Drop.always("coins")).size(1.0f, 2.6f, 1.5f).at("dream_world_cyrisus").arena(11, "void"));
        b.quest("dream_mentor", "Dream Mentor", Difficulty.MASTER, 2)
                .reqQuest("lunar_diplomacy", "eadgars_ruse")                .xp(Skill.HITPOINTS, 15000).xp(Skill.MAGIC, 10000)
                .unlock("The Dream Lunar spell")
                .stage("I can start this quest by speaking to the Oneiromancer on Lunar Isle. (Requires Lunar Diplomacy, Eadgar's Ruse.)")
                .talk("oneiromancer").reqCombat(85).missing("Cyrisus's dreams are too dangerous for you yet. (Requires combat level 85.)")
                .say("Cyrisus, a Fremennik warrior, lies in a stupor in the mine. Help him regain his confidence through the dream world.")
                .stage("I should talk to Cyrisus and restore his spirit (bring him food and gear).")
                .talk("cyrisus").take("shark:5", "rune_full_helm").say("Food... armour... I feel stronger. But I fear what's in my dreams.")
                .missing("I'm too weak... I need food (5 sharks) and some armour (a rune full helm)...")
                .stage("I should enter Cyrisus's dream with him and face his fears.")
                .talk("oneiromancer").travel("dream_world_cyrisus").say("Drink this and share his dream... *The world fades.*")
                .stage("I must defeat The Inadequacy, Cyrisus's worst fear.")
                .kill("the_inadequacy").travel("quest_lunar_isle").say("* The Inadequacy fades away. Cyrisus wakes with new courage.")
                .stage("I should speak to Cyrisus.")
                .talk("cyrisus").say("I'm free of my fears! Thank you. The Oneiromancer says you've earned the Dream spell.");
    }

    private static void clientOfKourend(QuestBook b) {
        b.npc("leenz", "Leenz", "great_kourend", 0, 3, 0xE0B8A8, 0x6A4A2E, 0x4A3A1E, 0x6A3A1F);
        b.item("enchanted_scroll", "Enchanted scroll", "note", 0xD0C0F0, "A scroll asking for the Kourend houses' opinions.");
        b.item("kharedsts_memoirs", "Kharedst's memoirs", "book", 0x5A3A6A, "A book that can teleport you around Great Kourend.");
        b.quest("client_of_kourend", "Client of Kourend", Difficulty.NOVICE, 1)
                .reqQuest("x_marks_the_spot").xp(Skill.HITPOINTS, 500).xp(Skill.CRAFTING, 500).reward("kharedsts_memoirs")
                .stage("I can start this quest by speaking to Veos on the Port Sarim docks. (Requires X Marks the Spot.)")
                .talk("veos_port_sarim").give("enchanted_scroll").travel("great_kourend")
                .say("My client wants opinions on the five houses of Great Kourend. Take this enchanted scroll and ask around. *You sail to Great Kourend.*")
                .stage("I should ask the people of the five houses of Kourend for their opinions, starting with Leenz in Port Piscarilius.")
                .talk("leenz").say("The houses? Piscarilius has fish, Arceuus has magic, Hosidius has farms, Lovakengj has dwarves... and Shayzien has soldiers.",
                        "* Your enchanted scroll glows: it's recorded all five opinions.")
                .stage("I should return to Veos.")
                .talk("veos_port_sarim").take("enchanted_scroll").say("My client is delighted! Take this book of memoirs as thanks.");
    }

    private static void tearsOfGuthix(QuestBook b) {
        b.npc("juna", "Juna", "tears_of_guthix_cave", 0, 2, 0x5A6A5A, 0x3A5A3A, 0x2A3A2A, 0x4A6A4A);
        b.item("stone_bowl", "Stone bowl", "pot", 0x8A8A8A, "A bowl carved from light creature crystals.");
        b.quest("tears_of_guthix", "Tears of Guthix", Difficulty.INTERMEDIATE, 1)
                .qp(43).req(Skill.FIREMAKING, 49).req(Skill.CRAFTING, 20).req(Skill.MINING, 20)
                .xp(Skill.CRAFTING, 1000)
                .unlock("The weekly Tears of Guthix minigame")
                .stage("I can start this quest by speaking to Juna the serpent in the caves beneath Lumbridge Swamp. (Requires 43 quest points.)")
                .talk("juna").say("Tell me a story of your adventures, and I will let you collect the Tears of Guthix. You'll need a bowl carved from magic stone.")
                .stage("I need a stone bowl, carved from the rock by the light creatures' chasm (a chisel and a tinderbox for light).")
                .talk("juna").need("chisel", "tinderbox").give("stone_bowl").say("* By the light of your lantern you carve a bowl from the magic rock.")
                .missing("You'll need a chisel to carve the rock and a tinderbox to light your way.")
                .stage("I should tell Juna a story of my adventures.")
                .talk("juna").say("A fine tale. You may collect the Tears of Guthix once a week. Drink the blue tears, avoid the green.");
    }

    private static void theFeud(QuestBook b) {
        b.npc("ali_morrisane", "Ali Morrisane", "POLLNIVNEACH", 3, 3, 0xB07A4B, 0xE0C081, 0xB09061, 0xE0E0E9);
        b.npc("ali_the_operator", "Ali the Operator", "POLLNIVNEACH", -3, 3, 0xA06A41, 0x3A3A3B, 0x2A2A2F, 0x1A1A1E);
        b.items(s -> s.add("desert_disguise_feud", "Kharidian headpiece", Kind.SIMPLE, "cowl", 0xE0D0A0).value(1).examine("A disguise worn by the people of Pollnivneach."));
        b.quest("the_feud", "The Feud", Difficulty.INTERMEDIATE, 1)
                .req(Skill.THIEVING, 30).xp(Skill.THIEVING, 500).reward("coins:500")
                .stage("I can start this quest by speaking to Ali Morrisane in Al Kharid... he's in Pollnivneach looking for his nephew. (Requires 30 Thieving.)")
                .talk("ali_morrisane").give("desert_disguise_feud").say("My nephew vanished in Pollnivneach, a town ruled by two feuding gangs. Wear this disguise and blend in.")
                .stage("I should infiltrate the Menaphite gang. Ali the Operator runs the local rackets.")
                .talk("ali_the_operator").need("desert_disguise_feud").say("New recruit? Prove yourself: pickpocket the Villager. *You do so with style.* Fine. You'll find the nephew in the mayor's house.")
                .stage("I should rescue the nephew and tell Ali Morrisane.")
                .talk("ali_morrisane").take("desert_disguise_feud").say("You found him! The feud is over and my nephew is safe. Here's your reward.");
    }

    private static void icthlarin(QuestBook b) {
        b.npc("wanderer_sophanem", "Wanderer", "sophanem", 3, 3, 0xB07A4C, 0xE0D0A1, 0x8A6A3F, 0xE0E0EA);
        b.npc("high_priest_sophanem", "High Priest", "sophanem", -3, 3, 0xA86A42, 0xE0C044, 0xF0F0F1, 0xE0C045);
        b.item("holy_symbol_icthlarin", "Holy symbol of Icthlarin", "amulet", 0xE0C046, "A symbol of the god of the dead.");
        b.item("canopic_jars", "Canopic jars", "pot", 0xC0A061, "Four canopic jars for the mummification ritual.");
        b.quest("icthlarins_little_helper", "Icthlarin's Little Helper", Difficulty.INTERMEDIATE, 2)
                .reqQuest("gertrudes_cat").xp(Skill.THIEVING, 4500).xp(Skill.AGILITY, 4000).xp(Skill.WOODCUTTING, 1500)
                .unlock("Access to Sophanem")
                .stage("I can start this quest by speaking to the Wanderer south of the Shantay Pass, with my cat. (Requires Gertrude's Cat.)")
                .talk("wanderer_sophanem").need("gertrudes_kitten").say("A cat! The cats of Sophanem are sacred. Come, Sophanem is plagued and needs a cat to fulfil a prophecy.")
                .missing("You have no cat? Then I cannot help you.")
                .stage("I should go to Sophanem and speak to the High Priest.")
                .talk("high_priest_sophanem").say("The plague is a curse of Icthlarin! We must perform the burial ritual: bring four canopic jars and a holy symbol.")
                .stage("I need canopic jars (clay) and a holy symbol of Icthlarin (silver).")
                .talk("high_priest_sophanem").take("clay:4", "silver_bar").give("canopic_jars", "holy_symbol_icthlarin")
                .say("*You shape canopic jars from clay and he blesses a silver symbol.*")
                .missing("Bring 4 soft clay for the jars and a silver bar for the symbol.")
                .stage("I should perform the ritual in the pyramid.")
                .reach("sophanem", 10).take("canopic_jars").say("* You place the jars and pray. Icthlarin's presence fills the pyramid... the curse lifts!")
                .stage("I should report to the High Priest.")
                .talk("high_priest_sophanem").take("holy_symbol_icthlarin").say("The curse is lifted! Sophanem is open to you, friend of Icthlarin.");
    }

    private static void tailOfTwoCats(QuestBook b) {
        b.npc("unferth", "Unferth", "BURTHORPE", -5, -6, 0xE8C0B9, 0x6A6A3C, 0x4A4A2C, 0x8A6A3D);
        b.npc("bob_the_cat", "Bob the Cat", "VARROCK", 6, -7, 0x3A3A3A, 0x2A2A2A, 0x1A1A1A, 0x4A4A4A);
        b.item("doctors_hat", "Doctor's hat", "cowl", 0xF0F0F2, "A hat that makes you look medical.");
        b.quest("a_tail_of_two_cats", "A Tail of Two Cats", Difficulty.INTERMEDIATE, 2)
                .reqQuest("icthlarins_little_helper").xp(Skill.COOKING, 2500).xp(Skill.CRAFTING, 2500)
                .stage("I can start this quest by speaking to Unferth in Burthorpe. (Requires Icthlarin's Little Helper.)")
                .talk("unferth").say("My cat Bob has run off! He's no ordinary cat... he sees things. Find him, please.")
                .stage("I should find Bob the Cat. He's been seen in Varrock.")
                .talk("bob_the_cat").say("Meow. *Bob stares at you knowingly. You get the feeling he's the reincarnation of the hero Robert the Strong.*")
                .stage("Unferth is unwell after losing Bob. I should look after him (doctor's hat, food) and bring Bob home.")
                .talk("unferth").take("shark", "minecraft:milk_bucket").give("doctors_hat")
                .say("*You feed Unferth and he perks up.* Bob is home! I feel much better. Take this silly hat I wore as a doctor.")
                .missing("I feel awful... some food (a shark) and milk would help.")
                .stage("I should talk to Bob about his past.")
                .talk("bob_the_cat").say("Meow. *In a vision, Bob shows you a mighty warrior, Robert the Strong, and a dragon named Galvek. Strange...*");
    }

    private static void golem(QuestBook b) {
        b.npc("clay_golem", "Clay golem", "uzer_golem_room", 0, 0, 0xA87A5A, 0x8A6A4A, 0x6A4A3A, 0xA06A40);
        b.npc("elissa", "Elissa", "uzer", 3, 3, 0xF0C8B3, 0x6A6AA1, 0x4A4A7B, 0xC08A3D);
        b.item("statuette", "Statuette", "statue", 0xC0A062, "A statuette from the Varrock Museum.");
        b.quest("the_golem", "The Golem", Difficulty.NOVICE, 1)
                .req(Skill.CRAFTING, 20).req(Skill.THIEVING, 25).xp(Skill.CRAFTING, 1000).xp(Skill.THIEVING, 1000)
                .stage("I can start this quest by speaking to the clay golem in the ruins of Uzer. (Requires 20 Crafting, 25 Thieving.)")
                .talk("clay_golem").say("I must protect Uzer from the demon Thammaron! But I am cracked. Repair me with soft clay.")
                .stage("I should repair the golem with 4 soft clay.")
                .talk("clay_golem").take("clay:4").say("I am whole! Now I must find the demon. The archaeologist Elissa knows of Uzer's history.")
                .stage("I should ask Elissa about the demon of Uzer.")
                .talk("elissa").say("Thammaron was defeated long ago. A statuette in the Varrock Museum opens the throne room. Curator Haig Halen has it.")
                .stage("I should get the statuette from Curator Haig Halen.")
                .talk("curator_haig_halen").give("statuette").say("The Uzer statuette? I'll lend it to you, for history's sake.")
                .stage("I should open the throne room and give the golem a new program so it can rest.")
                .talk("clay_golem").take("statuette")
                .say("* You place the statuette in the alcove and the throne room opens. Inside lies the demon's skeleton.",
                        "* You write a new program for the golem: 'Your task is done.'")
                .stage("I should tell the golem its task is done.")
                .talk("clay_golem").say("Thammaron is dead and my task is complete. I may rest at last. Thank you.");
    }

    private static void shadowOfTheStorm(QuestBook b) {
        b.npc("father_reen", "Father Reen", "uzer", -3, 3, 0xE0B8A9, 0x2A2A2D, 0x1A1A1F, 0x6A6A6B);
        b.items(s -> s.add("darklight", "Darklight", Kind.MELEE_WEAPON, "longsword", 0x4A3A6A).value(1).stack(1).tier("silver")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(10, 16, -2, 0, 0, 0, 3, 2, 1, 0, 13, 0, 0, 0),
                        Map.of(), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE))
                .examine("The magical sword Silverlight, enhanced with the blood of Agrith-Naar. Deadly to demons."));
        b.boss(BossDef.of("agrith_naar", "Agrith-Naar").level(100).stats(100, 90, 90, 90, 90, 1).speed(4)
                .attack(AttackDef.of("claws", AttackType.SLASH, 15, 3, 7))
                .attack(AttackDef.of("fire", AttackType.MAGIC, 15, 8, 3).particle("minecraft:flame"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("demon_temple_storm").arena(10, "lava"));
        b.quest("shadow_of_the_storm", "Shadow of the Storm", Difficulty.INTERMEDIATE, 1)
                .reqQuest("DEMON_SLAYER", "the_golem").req(Skill.CRAFTING, 30).xp(Skill.STRENGTH, 10000).reward("darklight")
                .stage("I can start this quest by speaking to Father Reen in the ruins of Uzer. (Requires Demon Slayer, The Golem, 30 Crafting.)")
                .talk("father_reen").say("A cult in Uzer plans to summon the demon Agrith-Naar! Your Silverlight could stop it.")
                .stage("I should infiltrate the cult and stop the summoning, with Silverlight.")
                .talk("father_reen").need("silverlight").say("Good, you have Silverlight. Go to the temple ruins; the cult is gathering now!")
                .missing("You'll need Silverlight to face a demon.")
                .stage("The cult summoned Agrith-Naar! I must slay it.")
                .kill("agrith_naar").take("silverlight").say("* Agrith-Naar is banished. Its blood stains Silverlight dark: it has become Darklight!")
                .stage("I should tell Father Reen.")
                .talk("father_reen").say("The demon is gone! Keep Darklight, it is even deadlier to demons than Silverlight was.");
    }

    private static void whatLiesBelow(QuestBook b) {
        b.npc("rat_burgiss", "Rat Burgiss", "rat_burgiss", 0, 0, 0xE0B8AA, 0x6A4A2F, 0x4A3A1F, 0x5A3A20);
        b.npc("surok_magis", "Surok Magis", "VARROCK", 6, -4, 0xD8C0C0, 0x6A1A1D, 0x3A0A0D, 0x2A2A2E);
        b.item("rats_letter", "Rat's letter", "note", 0xE0D8C5, "A letter from Rat about stolen papers.");
        b.items(s -> s.add("beacon_ring", "Beacon ring", Kind.SIMPLE, "ring", 0x6A1A1A).value(1).examine("A ring that helps you call for help."));
        b.boss(BossDef.of("possessed_king_roald", "King Roald (possessed)").level(47).stats(100, 40, 40, 40, 1, 1).speed(4)
                .attack(AttackDef.of("punch", AttackType.CRUSH, 8, 2.5, 10)).drops(Drop.always("coins"))
                .skin(0xE8C0A0, 0x8A1A3B, 0x5A1A2B, 0xD0B031).at("chaos_altar_surok").arena(8, "crypt"));
        b.quest("what_lies_below", "What Lies Below", Difficulty.INTERMEDIATE, 1)
                .reqQuest("RUNE_MYSTERIES").req(Skill.RUNECRAFT, 35).xp(Skill.RUNECRAFT, 8000).xp(Skill.DEFENCE, 2000).reward("beacon_ring")
                .stage("I can start this quest by speaking to Rat Burgiss on the road south of Varrock. (Requires Rune Mysteries, 35 Runecraft.)")
                .talk("rat_burgiss").give("rats_letter").say("Outlaws stole my papers! The outlaw camp's west of Varrock. Then take this letter to Surok Magis in Varrock.")
                .stage("I should deliver Rat's letter to Surok Magis in Varrock.")
                .talk("surok_magis").take("rats_letter").say("Excellent. Now make me a chaos talisman... no, just leave it. You've been most helpful. *He smirks.*")
                .stage("Something is wrong. Surok is plotting to mind-control King Roald! I should stop him in his lair.")
                .kill("possessed_king_roald").say("* You knock King Roald out of his trance. Surok flees through a portal!")
                .stage("I should tell Rat Burgiss what happened.")
                .talk("rat_burgiss").say("Surok tried to control the king?! We owe you. Take this ring; it'll help you call for aid.");
    }

    private static void defenderOfVarrock(QuestBook b) {
        b.npc("captain_rovin", "Captain Rovin", "varrock_palace", 3, -3, 0xE8C0BA, 0xC0C0CA, 0x2A3A6C, 0x8A8A8D);
        b.boss(BossDef.of("arrav", "Arrav").level(159).stats(180, 130, 130, 120, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, 20, 60)
                .attack(AttackDef.of("zombie_sword", AttackType.SLASH, 22, 2.5, 10))
                .drops(Drop.always("bones")).skin(0x8AA07B, 0xA08A3A, 0x6A5A2A, 0x5A6A4A).at("varrock_sewers_arrav").arena(10, "crypt"));
        b.quest("defender_of_varrock", "Defender of Varrock", Difficulty.EXPERIENCED, 2)
                .reqQuest("shield_of_arrav", "temple_of_ikov", "below_ice_mountain", "family_crest", "what_lies_below", "ROMEO_AND_JULIET", "DEMON_SLAYER")
                .reqQuest("garden_of_tranquillity")
                .req(Skill.AGILITY, 51).req(Skill.HUNTER, 52).req(Skill.MINING, 59)
                .xp(Skill.SMITHING, 15000).xp(Skill.HUNTER, 15000)
                .stage("I can start this quest by speaking to Captain Rovin in Varrock Palace. (Requires Shield of Arrav, What Lies Below and more.)")
                .talk("captain_rovin").say("Zombies are rising beneath Varrock! An ancient hero, Arrav, leads them, raised by Zamorakians.")
                .stage("I should go into the sewers and confront the undead hero Arrav.")
                .kill("arrav").say("* Arrav, hero of Avarrocka, is freed from the Zamorakian curse. His spirit thanks you.")
                .stage("I should report to Captain Rovin.")
                .talk("captain_rovin").say("Varrock is safe! The city honours you, defender.");
    }

    private static void inAidOfTheMyreque(QuestBook b) {
        b.npc("cornelius", "Cornelius", "burgh_de_rott", 3, 3, 0xE0B8AB, 0x5A4A3A, 0x3A2A1A, 0x5A3A21);
        b.npc("florin", "Florin", "burgh_de_rott", -3, 3, 0xE0B8AC, 0x6A5A3C, 0x4A3A2C, 0x8A5A2D);
        b.items(s -> s.add("rod_of_ivandis", "Rod of Ivandis", Kind.SIMPLE, "staff", 0xC0C0D1).value(1).examine("A rod that can bind vampyre juvinates."));
        b.boss(BossDef.of("gadderanks", "Gadderanks").level(35).stats(40, 30, 30, 30, 1, 1).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 5, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xD0C8C8, 0x3A1A1A, 0x2A0A0A, 0x1A1A1A).at("burgh_de_rott").arena(8, "crypt"));
        b.quest("in_aid_of_the_myreque", "In Aid of the Myreque", Difficulty.INTERMEDIATE, 2)
                .reqQuest("in_search_of_the_myreque").req(Skill.CRAFTING, 25).req(Skill.MINING, 15).req(Skill.MAGIC, 7)
                .xp(Skill.ATTACK, 2000).xp(Skill.STRENGTH, 2000).xp(Skill.DEFENCE, 2000).xp(Skill.CRAFTING, 2000).reward("rod_of_ivandis")
                .stage("I can start this quest by speaking to Veliaf Hurtz. (Requires In Search of the Myreque, 25 Crafting, 15 Mining, 7 Magic.)")
                .talk("veliaf_hurtz").say("We're moving our base to Burgh de Rott. It needs defences and supplies. Help us.")
                .stage("I should help rebuild Burgh de Rott: Cornelius needs 6 planks and 2 steel bars for the defences.")
                .talk("cornelius").take("plank:6", "steel_bar:2").say("Excellent, the walls will hold. Now Florin's general store needs stock.")
                .missing("We need 6 planks and 2 steel bars for the walls.")
                .stage("I should stock Florin's store with food.")
                .talk("florin").take("shark:2", "lobster:2").say("Food! The villagers will survive. But watch out, Gadderanks the vampyre hunter-turned-vampyre is coming!")
                .missing("Bring me some food to sell: 2 sharks and 2 lobsters.")
                .stage("I must defend Burgh de Rott from Gadderanks.")
                .kill("gadderanks").say("* Gadderanks is slain! The Myreque have a new home.")
                .stage("I should report to Veliaf.")
                .talk("veliaf_hurtz").say("Burgh de Rott is ours. Take the Rod of Ivandis: it binds the juvinates.");
    }

    private static void recruitmentDrive(QuestBook b) {
        b.npc("sir_tiffy_training", "Sir Kuam Ferentse", "falador_temple_knights", 0, 0, 0xB07A4D, 0xE0E0F2, 0x3A3A8C, 0x1A1A1F);
        b.items(s -> s.add("initiate_sallet", "Initiate sallet", Kind.ARMOUR, "full_helm", 0xA0A0B1).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, -6, -2, 13, 16, 11, -1, 15, 0, 0, 0, 3), 20))
                .examine("A Temple Knight's initiate helm."));
        b.quest("recruitment_drive", "Recruitment Drive", Difficulty.NOVICE, 1)
                .reqQuest("black_knights_fortress", "DRUIDIC_RITUAL")
                .xp(Skill.PRAYER, 1000).xp(Skill.HERBLORE, 1000).xp(Skill.AGILITY, 1000).reward("initiate_sallet")
                .stage("I can start this quest by speaking to Sir Tiffy Cashien in Falador Park. (Requires Black Knights' Fortress, Druidic Ritual.)")
                .talk("sir_tiffy_cashien").travel("falador_temple_knights").say("I say, we're recruiting for the Temple Knights! Let's see if you pass the tests.")
                .stage("I must pass the Temple Knights' tests: logic, strength and courage.")
                .talk("sir_tiffy_training").say("Test one: logic. *You solve the riddle.* Test two: combat. *You defeat Sir Kuam in a sparring bout.* Splendid!")
                .stage("I should report back to Sir Tiffy.")
                .talk("sir_tiffy_cashien").say("Top-hole! You're a Temple Knight initiate now. Here's your sallet.");
    }

    private static void lostTribe(QuestBook b) {
        b.npc("sigmund_lumbridge", "Sigmund", "LUMBRIDGE", -3, 7, 0xE8C0BB, 0x3A5A8C, 0x2A3A5C, 0xA0A0A2);
        b.npc("mistag", "Mistag", "dorgesh_kaan_mine", 0, 2, 0xA0B8A0, 0x5A4A3D, 0x3A2A1F, 0x6A8A6A);
        b.item("silverware", "Silverware", "bar", 0xD0D0E0, "The Duke's stolen silverware.");
        b.quest("the_lost_tribe", "The Lost Tribe", Difficulty.INTERMEDIATE, 1)
                .reqQuest("goblin_diplomacy", "RUNE_MYSTERIES").req(Skill.AGILITY, 13).req(Skill.THIEVING, 13).req(Skill.MINING, 17)
                .xp(Skill.MINING, 3000).xp(Skill.AGILITY, 3000).xp(Skill.THIEVING, 3000)
                .unlock("Access to Dorgesh-Kaan, the cave goblin city")
                .stage("I can start this quest by speaking to Sigmund in Lumbridge Castle. (Requires Goblin Diplomacy, Rune Mysteries, 13 Agility, 13 Thieving, 17 Mining.)")
                .talk("sigmund_lumbridge").say("A hole in the cellar wall, and the Duke's silverware is missing! Goblins, I say. Investigate!")
                .stage("I should investigate the hole in the Lumbridge cellar.")
                .reach("dorgesh_kaan_mine", 8).say("* Beyond the hole is a tunnel. A strange grey goblin with big eyes watches you.")
                .stage("I should speak to the cave goblin, Mistag.")
                .talk("mistag").give("silverware").say("We are the Dorgeshuun, a lost tribe! We did not steal anything... but we found this silver in the tunnels. Your Sigmund was carrying it!")
                .stage("Sigmund framed the goblins! I should return the silverware to the Duke... and confront Sigmund.")
                .talk("sigmund_lumbridge").take("silverware").say("Curses! H.A.M. will see the goblins driven out! *Sigmund flees.* The Duke thanks you and makes peace with the Dorgeshuun.");
    }

    private static void wanted(QuestBook b) {
        b.item("commorb", "Commorb", "orb", 0x3A6AC0, "A Temple Knight communication orb.");
        b.boss(BossDef.of("solus_dellagar", "Solus Dellagar").level(76).stats(90, 50, 50, 60, 80, 1).speed(4)
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 14, 8, 6).particle("minecraft:flame"))
                .attack(AttackDef.of("ice_blitz", AttackType.MAGIC, 12, 8, 4).special(Special.FREEZE, 5).particle("minecraft:snowflake"))
                .drops(Drop.always("bones")).skin(0xD8C0B0, 0x2A0A3A, 0x1A0A2A, 0x6A1A6A).at("solus_hideout").arena(9, "crypt"));
        b.quest("wanted", "Wanted!", Difficulty.INTERMEDIATE, 1)
                .qp(32).reqQuest("recruitment_drive", "the_lost_tribe", "priest_in_peril", "enter_the_abyss")
                .xp(Skill.SLAYER, 5000)
                .stage("I can start this quest by speaking to Sir Amik Varze in Falador Castle. (Requires 32 quest points, Recruitment Drive, The Lost Tribe, Priest in Peril, Enter the Abyss.)")
                .talk("sir_amik_varze").give("commorb").say("The renegade mage Solus Dellagar is loose. As a Temple Knight initiate, you'll track him down. Use this commorb to report.")
                .stage("I should track down Solus Dellagar. He was last seen in the Dwarven Mine's lower caverns.")
                .kill("solus_dellagar").say("* Solus Dellagar is defeated. You report via the commorb.")
                .stage("I should report to Sir Amik Varze.")
                .talk("sir_amik_varze").take("commorb").say("Solus Dellagar is no more. You've earned your promotion. Proselyte armour is yours to buy.");
    }

    private static void enterTheAbyss(QuestBook b) {
        b.item("scrying_orb", "Scrying orb", "orb", 0x6A2A8A, "An orb that records teleport locations.");
        b.quest("enter_the_abyss", "Enter the Abyss (miniquest)", Difficulty.NOVICE, 0)
                .reqQuest("RUNE_MYSTERIES")
                .unlock("Access to the Abyss via the Mage of Zamorak")
                .stage("I can start this miniquest by speaking to the Mage of Zamorak in the Wilderness... or his acolyte in Varrock. (Requires Rune Mysteries.)")
                .talk("mage_of_zamorak").give("scrying_orb").say("The Zamorakians have found a shortcut to the runecrafting altars. Help me: teleport with Aubury, Sedridor and Distentor while carrying this orb.")
                .stage("I should be teleported to the essence mine by Aubury (Varrock), Sedridor (Wizards' Tower) and Brimstail (Gnome Stronghold), carrying the scrying orb.")
                .reach("VARROCK", 10).stay().unlessFlag("eta_aubury").need("scrying_orb").flag("eta_aubury").say("* Aubury teleports you to the essence mine. The orb glows.")
                .reach("WIZARDS_TOWER", 10).stay().unlessFlag("eta_sedridor").need("scrying_orb").flag("eta_sedridor").say("* Sedridor teleports you. The orb glows brighter.")
                .reach("GNOME_STRONGHOLD", 12).stay().unlessFlag("eta_brimstail").need("scrying_orb").flag("eta_brimstail").say("* Brimstail teleports you. The orb is fully charged!")
                .talk("mage_of_zamorak").needFlag("eta_aubury", "eta_sedridor", "eta_brimstail").take("scrying_orb")
                .say("The orb holds the teleport matrix! I can now send you into the Abyss whenever you wish.")
                .missing("Get the orb charged by Aubury, Sedridor and Brimstail's teleports.");
    }

    private static void barcrawl(QuestBook b) {
        b.npc("barbarian_guard", "Barbarian guard", "barbarian_outpost", 3, 3, 0xD8A889, 0x6A3A1F, 0x3A2A1F, 0xC06A2E);
        b.item("barcrawl_card", "Barcrawl card", "note", 0xE0D8C6, "The official Alfred Grimhand's Barcrawl card.");
        b.quest("alfred_grimhands_barcrawl", "Alfred Grimhand's Barcrawl (miniquest)", Difficulty.NOVICE, 0)
                .unlock("Entry to the Barbarian Outpost agility course")
                .stage("I can start this miniquest by speaking to the Barbarian guard at the Barbarian Outpost.")
                .talk("barbarian_guard").give("barcrawl_card").say("Want in? Complete the Alfred Grimhand Barcrawl! Drink at the Blue Moon Inn, Rising Sun Inn, Jolly Boar Inn, Flying Horse Inn, Dead Man's Chest and Forester's Arms.")
                .stage("I need to drink at six bars and have my barcrawl card signed.")
                .reach("blue_moon_inn", 5).stay().unlessFlag("bc_blue_moon").take("coins:50").flag("bc_blue_moon").say("* You down an Uncle Humphrey's Gutrot at the Blue Moon Inn. 50 coins. Your card is signed.")
                .reach("rising_sun_inn", 5).stay().unlessFlag("bc_rising_sun").take("coins:70").flag("bc_rising_sun").say("* You drink a Hand of Death cocktail at the Rising Sun. 70 coins. Signed!")
                .reach("jolly_boar_inn", 5).stay().unlessFlag("bc_jolly_boar").take("coins:10").flag("bc_jolly_boar").say("* A Olde Suspiciouse at the Jolly Boar. 10 coins. Signed!")
                .reach("flying_horse_inn", 5).stay().unlessFlag("bc_flying_horse").take("coins:8").flag("bc_flying_horse").say("* A Heart Stopper at the Flying Horse. 8 coins. Signed!")
                .reach("dead_mans_chest", 5).stay().unlessFlag("bc_dead_mans_chest").take("coins:15").flag("bc_dead_mans_chest").say("* A Supergrog at the Dead Man's Chest. 15 coins. Signed!")
                .reach("forester_arms", 5).stay().unlessFlag("bc_forester").take("coins:18").flag("bc_forester").say("* A Liverbane Ale at the Forester's Arms. 18 coins. Signed!")
                .talk("barbarian_guard").needFlag("bc_blue_moon", "bc_rising_sun", "bc_jolly_boar", "bc_flying_horse", "bc_dead_mans_chest", "bc_forester")
                .take("barcrawl_card").say("All six bars! You're a true barbarian. Welcome to the Outpost!")
                .missing("Your card isn't full yet. Six bars, six signatures!");
    }
}
