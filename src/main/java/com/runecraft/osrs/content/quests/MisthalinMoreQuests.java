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
        b.item("jorrals_letter", "Letter", "note", 0xE0D8C4, "Jorral's history of the Outpost, for King Lathas.");
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
                .reach("rellekka_outpost", 8).stay().need("spade").unless("outpost_journal").give("outpost_journal", "enchanted_key")
                .say("* The enchanted key grows warm. You dig where it's hottest and unearth an old chest with a journal inside.")
                .missing("* The ground here is disturbed. You'd need a spade to dig.")
                .talk("jorral").take("outpost_scroll", "outpost_letter", "outpost_journal").give("jorrals_letter")
                .say("Wonderful! Here, I've written it all up. Take this letter to King Lathas.")
                .missing("I need the letter from Dron, the scroll from Droalak and the journal from the buried chest.")
                .stage("I should take the history to King Lathas.")
                .talk("king_lathas").take("jorrals_letter").say("A fascinating history. Very well, the Outpost may stay, and become a museum. Here's a reward.")
                .missing("Jorral's Outpost? Bring me something in writing.")
                .done("historian_minas", "Jorral's Outpost is a museum now? Splendid! History preserved.");
    }

    /** Cyrisus's bank in 'Birds-Eye' Jack's keeping (wiki): helmets, bodies, legs, boots and weapons of the three sets. */
    static final String[] DM_BANK = {"Dragon med helm", "Splitbark helm", "Robin Hood hat", "Ahrim's robetop", "Karil's leathertop",
            "Dragon chainbody", "Ahrim's robeskirt", "Torag's platelegs", "Black d'hide chaps", "Ranger boots", "Adamant boots",
            "Infinity boots", "Abyssal whip", "Magic shortbow", "Ancient staff"};

    /**
     * Dream Mentor (wiki, quest-helper): lifting Cyrisus's spirits. Each talk is three kind things to say among unkind ones,
     * in any order (quest-helper's dialogue choices).
     */
    static final QuestPuzzle DM_SPIRIT = QuestPuzzle.builder("dm_spirit").shuffle()
            .room("weak").variant(null)
            .prompt("* Cyrisus props himself up on an elbow. Lift his spirits: say three kind things.")
            .options("You're looking better now: you look and sound more lively.", "Are you looking forward to getting out? That's the spirit!",
                    "You seem like a nice guy. Just being honest.", "You still look half dead.", "Don't expect me to stay long.",
                    "I nearly left you for the suqah.")
            .anyOrder(0, 1, 2)
            .variant(null)
            .prompt("* Cyrisus manages a weak smile. Lift his spirits: say three kind things.")
            .options("When we get out of here I'll buy you a drink: whatever and wherever you want, my treat.",
                    "I'm very impressed you managed to get into this cave. I would have given up personally.",
                    "You'll survive this easily. Think of all the places you can visit when you get out!", "You smell like a troll's larder.",
                    "Real warriors don't faint in caves.", "Hurry up and get better, I'm busy.")
            .anyOrder(0, 1, 2)
            .room("stronger").variant(null)
            .prompt("* Cyrisus sits up on his own now. Lift his spirits: say three kind things.")
            .options("Just don't worry. Of course you'll be fine.", "What are you going to do when you get out of here? You could travel with me!",
                    "It's a good thing you have me to look after you. Not that I'm bragging or anything.", "You'll never be a warrior again.",
                    "This cave is a dump.", "I only came for the reward.")
            .anyOrder(0, 1, 2)
            .variant(null)
            .prompt("* Cyrisus is almost on his feet. Lift his spirits: say three kind things.")
            .options("Not long now and you'll be back on your feet!", "You're sounding much better. If you need anything, just let me know.",
                    "You're very safe in this little cave. The suqah will never fit through that tunnel.", "The suqah are waiting outside for you.",
                    "Your armour's probably been sold by now.", "Stop whining.")
            .anyOrder(0, 1, 2)
            .always("weak", "stronger").failLines("@Cyrisus: ...Maybe I'm better off alone. *His spirits sink. Try again.*").build();

    /**
     * Dream Mentor (wiki): 'Birds-Eye' Jack opens Cyrisus's bank and the right helmet, body, legs, boots and weapon go to him.
     * OSRS picks the set by the player's own combat style and leaves it to guesswork; here Cyrisus's note names it (approx).
     */
    static final QuestPuzzle DM_ARMOUR = QuestPuzzle.builder("dm_armour").room("cyrisus_bank")
            .variant(null).prompt("* 'Birds-Eye' Jack opens Cyrisus's bank. Choose his helmet, body, legs, boots and weapon, in that order.",
                    "@'Birds-Eye' Jack: He fights up close. His note says: a dragon's helm, a Barrows mage's robes, a ranger's boots and a demon's whip.")
            .options(DM_BANK).solution(0, 3, 6, 9, 12)
            .variant(null).prompt("* 'Birds-Eye' Jack opens Cyrisus's bank. Choose his helmet, body, legs, boots and weapon, in that order.",
                    "@'Birds-Eye' Jack: He fights from afar. His note says: a druid's bark helm, a Barrows archer's top, a Barrows warrior's "
                            + "legs, adamant boots and a magic bow.")
            .options(DM_BANK).solution(1, 4, 7, 10, 13)
            .variant(null).prompt("* 'Birds-Eye' Jack opens Cyrisus's bank. Choose his helmet, body, legs, boots and weapon, in that order.",
                    "@'Birds-Eye' Jack: He fights with spells. His note says: an outlaw's hat, a dragon's chainmail, black dragonhide legs, "
                            + "infinity boots and an ancient staff.")
            .options(DM_BANK).solution(2, 5, 8, 11, 14)
            .failLines("@'Birds-Eye' Jack: Cyrisus would never wear that together. Let's start over.").build();

    private static void dreamMentor(QuestBook b) {
        b.npc("cyrisus", "Cyrisus", "quest_lunar_isle", -3, -2, 0xE8C0B8, 0x8A8AA0, 0x5A5A7A, 0x5A3A1E);
        b.npcAt("birds_eye_jack", "'Birds-Eye' Jack", 2099, 3921, 0xE8C0A8, 0x6A6AA0, 0x4A4A7A, 0x8A6A3A); // the Lunar Isle bank
        b.spot("dm_moonclan_sink", "Moon Clan sink", 2091, 3922); // the house west of the bank
        b.spot("dm_dream_hall", "Dream Hall", 2073, 3912); // its brazier
        b.item("goutweed", "Goutweed", "herb", 0x6A8A3A, "A rather smelly weed. Trolls use it in their cooking.");
        b.item("dream_vial", "Dream vial", "vial", 0xC0C8E0, "A vial from the Oneiromancer, for a dream potion.");
        b.item("dream_potion", "Dream potion", "vial", 0x8A80C0, "Shares a dream with whoever drinks it beside you.");
        b.item("cyrisus_chest", "Cyrisus's chest", "package", 0x6A4A2A, "Cyrisus's helmet, armour, boots and weapon.");
        // The Inadequacy summons Doubts, which vanish when it falls (wiki: A Doubt).
        b.monster(MonsterDef.of("a_doubt", "A Doubt").level(78).stats(59, 72, 58, 85, 1, 1).speed(6)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 8, 2.5, 1))
                .skin(0x6A6A8A, 0x4A4A6A, 0x2A2A4A, 0x9A9AC0).size(0.8f, 1.8f, 0.8f));
        // Cyrisus's four fears, fought one after another in his dream (OSRS Wiki infoboxes: levels, HP, stats, max hits, speeds);
        // prayers don't work there (QuestBossScripts.DreamFear).
        b.boss(BossDef.of("the_inadequacy", "The Inadequacy").level(343).stats(180, 564, 170, 240, 1, 170).speed(4)
                .defenceBonus(0, 0, 0, 0, 0)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 18, 3, 6))
                .attack(AttackDef.of("doubt", AttackType.RANGED, 20, 10, 3))
                .attack(AttackDef.of("summon_doubts", AttackType.MAGIC, 0, 10, 1).special(Special.SUMMON, 2))
                .minion("a_doubt", 2)
                .phase(50, "The Inadequacy: You'll never be good enough!", 1.0, false)
                .drops(Drop.always("coins")).size(1.0f, 2.6f, 1.5f).at("dream_world_cyrisus").arena(11, "void"));
        b.boss(BossDef.of("the_everlasting", "The Everlasting").level(223).stats(230, 187, 231, 120, 1, 1).speed(6)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 24, 3, 1))
                .drops(Drop.always("coins")).skin(0x8A8AA0, 0x5A5A7A, 0x3A3A5A, 0xC0C0E0).size(1.0f, 2.6f, 1.3f).at("dream_world_cyrisus").arena(11, "void"));
        b.boss(BossDef.of("the_untouchable", "The Untouchable").level(274).stats(90, 187, 255, 434, 1, 1).speed(6)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 26, 3, 1))
                .drops(Drop.always("coins")).skin(0xA0A0B0, 0x6A6A8A, 0x4A4A6A, 0xE0E0F0).size(1.0f, 2.6f, 1.3f).at("dream_world_cyrisus").arena(11, "void"));
        b.boss(BossDef.of("the_illusive", "The Illusive").level(108).stats(144, 72, 71, 105, 1, 1).speed(6)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 8, 3, 1))
                .drops(Drop.always("coins")).skin(0x7A7A9A, 0x4A4A6A, 0x2A2A4A, 0xB0B0D0).size(0.9f, 2.4f, 1.2f).at("dream_world_cyrisus").arena(11, "void"));
        b.quest("dream_mentor", "Dream Mentor", Difficulty.MASTER, 2)
                .reqQuest("lunar_diplomacy", "eadgars_ruse")                .xp(Skill.HITPOINTS, 15000).xp(Skill.MAGIC, 10000)
                .unlock("The Dream Lunar spell")
                .stage("I can start this quest by talking to Cyrisus, a fallen Fremennik warrior on Lunar Isle. (Requires Lunar Diplomacy, Eadgar's Ruse, combat level 85.)")
                .talk("cyrisus").reqCombat(85).missing("* The fallen man is in a bad way. You don't feel ready to help him yet. (Requires combat level 85.)")
                .say("* The fallen man barely stirs. He's starving, his spirit is broken, and his armour is gone.",
                        "...Help me. Please.")
                // Wiki: health, spirit and armament in that order; 20 pieces of at least three kinds of food fed in turn (seven,
                // seven and six), four lots of talk, then Cyrisus's bank set from 'Birds-Eye' Jack.
                .stage("I should nurse Cyrisus back to health: feed him three kinds of food in turn (7 cabbages, 7 potatoes and 6 onions), "
                        + "lift his spirits, and fetch his armour from 'Birds-Eye' Jack in the Lunar Isle bank.")
                .talk("cyrisus").stay().unlessFlag("dm_fed_1").take("cabbage", "potato", "onion:2").flag("dm_fed_1")
                .say("* You feed him a cabbage, a potato and two onions, one kind after another. (Health: 20%)", "...More...")
                .missing("* He needs food: a cabbage, a potato and two onions, fed one kind after another.")
                .talk("cyrisus").stay().needFlag("dm_fed_1").unlessFlag("dm_fed_2").take("cabbage:2", "potato", "onion").flag("dm_fed_2")
                .say("* You feed him two cabbages, a potato and an onion. He opens his eyes. (Health: 40%)", "Thank you... friend.")
                .missing("* He needs more food: two cabbages, a potato and an onion.")
                .puzzle("cyrisus", DM_SPIRIT, 0).stay().needFlag("dm_fed_2").unlessFlag("dm_spirit_1").flag("dm_spirit_1")
                .say("@Cyrisus: You're right... maybe I will get out of here. (Spirit: 40%)")
                .talk("cyrisus").stay().needFlag("dm_spirit_1").unlessFlag("dm_fed_3").take("cabbage:2", "potato:2", "onion:2").flag("dm_fed_3")
                .say("* You feed him two of each. He sits up on his own. (Health: 70%)")
                .missing("* He needs more food: two cabbages, two potatoes and two onions.")
                .puzzle("cyrisus", DM_SPIRIT, 1).stay().needFlag("dm_fed_3").unlessFlag("dm_spirit_2").flag("dm_spirit_2")
                .say("@Cyrisus: I feel like myself again. But I can't face anything without my armour: 'Birds-Eye' Jack keeps my bank. (Spirit: 72%)")
                .puzzle("birds_eye_jack", DM_ARMOUR).stay().needFlag("dm_spirit_2").unless("cyrisus_chest").unlessFlag("dm_armed")
                .need("seal_of_passage").give("cyrisus_chest")
                .say("@'Birds-Eye' Jack: That's his set, all right. Here: his chest. Take it down to him.")
                .talk("cyrisus").stay().needFlag("dm_spirit_2").unlessFlag("dm_armed").take("cyrisus_chest").flag("dm_armed")
                .say("* Cyrisus pulls on his armour and grips his weapon. (Armament: 100%)", "Now I look like a warrior again.")
                .missing("I can't go anywhere like this. 'Birds-Eye' Jack in the bank keeps my things.")
                .talk("cyrisus").stay().needFlag("dm_armed").unlessFlag("dm_fed_4").take("cabbage:2", "potato:3", "onion").flag("dm_fed_4")
                .say("* You feed him the last of the food. He stands tall. (Health: 100%)")
                .missing("* He could eat a little more: two cabbages, three potatoes and an onion.")
                .talk("cyrisus").needFlag("dm_fed_4")
                .say("I'm strong again, I have my armour and my spirit... but I can't fight. Every time I raise my weapon, I freeze.",
                        "Will you take me to the Oneiromancer? Maybe she can help.")
                .stage("Cyrisus is afraid of combat. I should talk to the Oneiromancer, make a dream potion and light the brazier in the Dream Hall.")
                .talk("oneiromancer").stay().unless("dream_vial").unless("dream_potion").unlessFlag("dm_brazier").give("dream_vial")
                .say("His fear lives in his dreams. Share his dream and face it with him.",
                        "Take this dream vial. Fill it with water, add goutweed, then an astral rune: crush it with a hammer and grind it with a pestle and mortar.")
                // Goutweed: the Troll Stronghold storeroom, open to you since Eadgar's Ruse.
                .reach("troll_stronghold", 8).stay().unless("goutweed").unless("dream_potion").give("goutweed")
                .say("* You slip into the trolls' storeroom and take some goutweed.")
                .reach("dm_moonclan_sink", 6).stay().take("dream_vial", "goutweed", "astral_rune").need("hammer", "pestle_and_mortar").give("dream_potion")
                .say("* You fill the dream vial at the sink and add the goutweed. Your hammer cracks the astral rune into shards, the pestle and "
                        + "mortar grinds them, and the powder turns it all into a dream potion.")
                .missing("* A sink. The dream potion needs the Oneiromancer's vial, goutweed, an astral rune, a hammer and a pestle and mortar.")
                .reach("dm_dream_hall", 6).stay().need("dream_potion", "tinderbox").unlessFlag("dm_brazier").flag("dm_brazier")
                .say("* You light the brazier in the Dream Hall. Its smoke smells of sleep.")
                .missing("* The Dream Hall's brazier is cold. Light it with a tinderbox once you have the dream potion.")
                .talk("cyrisus").needFlag("dm_brazier").take("dream_potion").need("seal_of_passage").travel("dream_world_cyrisus")
                .say("Together, then. *You drink the dream potion beside the brazier, and the world fades.*",
                        "* Your prayers have no power here, and only the lectern leads out.")
                .missing("We need the dream potion, and the brazier in the Dream Hall lit.")
                .stage("I must defeat Cyrisus's four fears: The Inadequacy, The Everlasting, The Untouchable and The Illusive.")
                // each fear rises again where it left off if the player comes back to the dream
                .reach("dream_world_cyrisus", 12).stay().unlessFlag("dm_fear_1").spawn("the_inadequacy")
                .reach("dream_world_cyrisus", 12).stay().needFlag("dm_fear_1").unlessFlag("dm_fear_2").spawn("the_everlasting")
                .reach("dream_world_cyrisus", 12).stay().needFlag("dm_fear_2").unlessFlag("dm_fear_3").spawn("the_untouchable")
                .reach("dream_world_cyrisus", 12).stay().needFlag("dm_fear_3").spawn("the_illusive")
                .kill("the_inadequacy").stay().unlessFlag("dm_fear_1").flag("dm_fear_1").spawn("the_everlasting")
                .say("* The Inadequacy fades away, and its Doubts with it... The Everlasting rises in its place.")
                .kill("the_everlasting").stay().needFlag("dm_fear_1").unlessFlag("dm_fear_2").flag("dm_fear_2").spawn("the_untouchable")
                .say("* The Everlasting finally falls. The Untouchable steps out of the dark.")
                .kill("the_untouchable").stay().needFlag("dm_fear_2").unlessFlag("dm_fear_3").flag("dm_fear_3").spawn("the_illusive")
                .say("* The Untouchable shatters. Only The Illusive remains.")
                .kill("the_illusive").needFlag("dm_fear_3").travel("quest_lunar_isle")
                .say("* Cyrisus stomps The Illusive into nothing and wakes with new courage.")
                .stage("I should return to the Oneiromancer.")
                .talk("oneiromancer").say("Cyrisus is free of his fears. He wants to thank you with what he knows of lunar magic.",
                        "@Cyrisus: Thank you, friend. Take these spells, and may your dreams be kinder than mine.");
    }

    private static void clientOfKourend(QuestBook b) {
        b.npc("leenz", "Leenz", "great_kourend", 0, 3, 0xE0B8A8, 0x6A4A2E, 0x4A3A1E, 0x6A3A1F);
        b.item("enchanted_scroll", "Enchanted scroll", "note", 0xD0C0F0, "A scroll asking for the Kourend houses' opinions.");
        // One per stack: its charges live on the item (Charges), so a stack would share and duplicate them.
        b.items(s -> s.add("kharedsts_memoirs", "Kharedst's memoirs", com.runecraft.osrs.ItemDef.Kind.SIMPLE, "book", 0x5A3A6A).value(0)
                .stack(1).examine("A book that can teleport you around Great Kourend."));
        b.quest("client_of_kourend", "Client of Kourend", Difficulty.NOVICE, 1)
                .reqQuest("x_marks_the_spot").reward("kharedsts_memoirs") // and two 500 XP antique lamps (QuestLamps)
                .stage("I can start this quest by speaking to Veos on the Port Sarim docks. (Requires X Marks the Spot.)")
                .talk("veos_port_sarim").give("enchanted_scroll").travel("great_kourend")
                .say("My client wants opinions on the five houses of Great Kourend. Take this enchanted scroll and ask around. *You sail to Great Kourend.*")
                .stage("I should ask the shopkeepers of the five houses of Kourend for their opinions (Hosidius, Shayzien, Lovakengj, Arceuus), then Leenz in Port Piscarilius.")
                .reach("hosidius", 12).stay().unlessFlag("cok_hosidius").flag("cok_hosidius").say("* Horace in the Hosidius general store gives you his opinion of the house.")
                .reach("shayzien", 12).stay().unlessFlag("cok_shayzien").flag("cok_shayzien").say("* Jennifer in the Shayzien general store gives you her opinion of the house.")
                .reach("lovakengj", 12).stay().unlessFlag("cok_lovakengj").flag("cok_lovakengj").say("* Munty in the Lovakengj general store gives you his opinion of the house.")
                .reach("arceuus", 12).stay().unlessFlag("cok_arceuus").flag("cok_arceuus").say("* Regath in the Arceuus general store gives you his opinion of the house.")
                .talk("leenz").needFlag("cok_hosidius", "cok_shayzien", "cok_lovakengj", "cok_arceuus")
                .missing("Piscarilius? Fish and trade, that's us. Have you asked the shopkeepers of the other four houses yet?").say("The houses? Piscarilius has fish, Arceuus has magic, Hosidius has farms, Lovakengj has dwarves... and Shayzien has soldiers.",
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
                .talk("juna").need("chisel", "tinderbox").reqSkill(Skill.MINING, 20).reqSkill(Skill.CRAFTING, 20)
                .give("stone_bowl").say("* You light a sapphire lantern and shine it on a light creature, which carries you across the chasm.",
                        "* You mine a lump of magic stone and carve a bowl from it with your chisel.")
                .missing("You'll need a tinderbox for the lantern and a chisel to carve the magic rock (20 Mining and Crafting).")
                .stage("I should tell Juna a story of my adventures.")
                .talk("juna").need("stone_bowl").say("A fine tale. You may collect the Tears of Guthix once a week. Drink the blue tears, avoid the green.")
                .missing("Without a bowl of magic stone you cannot catch the tears. Carve one from the rock across the chasm.");
    }

    private static void theFeud(QuestBook b) {
        b.npcAt("ali_morrisane", "Ali Morrisane", 3304, 3211, 0xB07A4B, 0xE0C081, 0xB09061, 0xE0E0E9);
        b.npc("ali_the_operator", "Ali the Operator", "POLLNIVNEACH", -3, 3, 0xA06A41, 0x3A3A3B, 0x2A2A2F, 0x1A1A1E);
        b.items(s -> s.add("desert_disguise_feud", "Kharidian headpiece", Kind.SIMPLE, "cowl", 0xE0D0A0).value(1).examine("A disguise worn by the people of Pollnivneach."));
        b.quest("the_feud", "The Feud", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.THIEVING, 15000) // OSRS (current quest XP rewards)
                .req(Skill.THIEVING, 30).reward("coins:500")
                .stage("I can start this quest by speaking to Ali Morrisane east of the Gem Trader in northern Al Kharid. (Requires 30 Thieving.)")
                .talk("ali_morrisane").give("desert_disguise_feud").say("My nephew vanished in Pollnivneach, a town ruled by two feuding gangs. Wear this disguise and blend in.")
                .stage("I should infiltrate the Menaphite gang. Ali the Operator runs the local rackets.")
                .talk("ali_the_operator").need("desert_disguise_feud").say("New recruit? Prove yourself: pickpocket the Villager. *You do so with style.* Fine. You'll find the nephew in the mayor's house.")
                .stage("I should rescue the nephew and tell Ali Morrisane.")
                .talk("ali_morrisane").take("desert_disguise_feud").say("You found him! The feud is over and my nephew is safe. Here's your reward.");
    }

    private static void icthlarin(QuestBook b) {
        b.npcAt("wanderer_sophanem", "Wanderer", 3315, 2849, 0xB07A4C, 0xE0D0A1, 0x8A6A3F, 0xE0E0EA);
        b.npc("high_priest_sophanem", "High Priest", "sophanem", -3, 3, 0xA86A42, 0xE0C044, 0xF0F0F1, 0xE0C045);
        b.item("holy_symbol_icthlarin", "Holy symbol of Icthlarin", "amulet", 0xE0C046, "A symbol of the god of the dead.");
        b.item("canopic_jars", "Canopic jars", "pot", 0xC0A061, "Four canopic jars for the mummification ritual.");
        b.quest("icthlarins_little_helper", "Icthlarin's Little Helper", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.THIEVING, 4500).xp(Skill.AGILITY, 4000).xp(Skill.WOODCUTTING, 4000) // OSRS (current quest XP rewards)
                .reqQuest("gertrudes_cat")
                .unlock("Access to Sophanem")
                .stage("I can start this quest by speaking to the Wanderer in the camp west of the Agility Pyramid, with my cat. (Requires Gertrude's Cat.)")
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
        b.npc("quest_hild", "Hild", "BURTHORPE", 6, -6, 0xE8C0BA, 0x5A2A6A, 0x3A1A4A, 0x8A6A3E);
        b.quest("a_tail_of_two_cats", "A Tail of Two Cats", Difficulty.INTERMEDIATE, 2)
                .reqQuest("icthlarins_little_helper") // two 2,500 XP antique lamps, any skill of level 30+ (QuestLamps)
                .stage("I can start this quest by speaking to Unferth in Burthorpe. (Requires Icthlarin's Little Helper.)")
                .talk("unferth").say("My cat Bob has run off! He's no ordinary cat... he sees things. Find him, please.")
                // OSRS: Hild enchants the catspeak amulet with 5 death runes so you can talk to Bob.
                .stage("I should ask Hild in Burthorpe to enchant my catspeak amulet, then find Bob the Cat. He's been seen in Varrock.")
                .talk("quest_hild").stay().unlessFlag("totc_hild").take("death_rune:5").flag("totc_hild")
                .say("Bob? Speak to a cat? I can enchant your amulet so you understand him, for 5 death runes.")
                .missing("I'll need 5 death runes to enchant that amulet.")
                .talk("gertrude").stay().needFlag("totc_hild").unlessFlag("totc_gertrude").flag("totc_gertrude")
                .say("Bob? He's a friend of my Fluffs. He mentioned the library... ask Reldo, dear.")
                .talk("reldo").stay().needFlag("totc_gertrude").unlessFlag("totc_reldo").flag("totc_reldo")
                .say("Robert the Strong? There's a tale of a hero reborn as a cat... Bob may remember more than he lets on.")
                .talk("bob_the_cat").needFlag("totc_hild").missing("Meow? *Without an enchanted catspeak amulet you can't understand him.*").say("Meow. *Bob stares at you knowingly. You get the feeling he's the reincarnation of the hero Robert the Strong.*")
                .stage("Unferth is unwell after losing Bob. I should do his chores and visit him dressed as a doctor (white desert clothes) with some food.")
                // OSRS: Unferth's chores, then a visit dressed as a doctor (the Apothecary's advice: white desert clothes). // approx: no hat needed
                .talk("unferth").need("desert_shirt", "desert_robe").take("shark", "minecraft:milk_bucket")
                .say("*You feed Unferth and he perks up.* Bob is home! I feel much better. Take this silly hat I wore as a doctor.")
                .missing("I feel awful... Are you a doctor? Some food (a shark) and milk would help.")
                .stage("I should talk to Bob about his past.")
                .talk("bob_the_cat").say("Meow. *In a vision, Bob shows you a mighty warrior, Robert the Strong, and a dragon named Galvek. Strange...*");
    }

    private static void golem(QuestBook b) {
        b.npc("clay_golem", "Clay golem", "uzer_golem_room", 0, 0, 0xA87A5A, 0x8A6A4A, 0x6A4A3A, 0xA06A40);
        b.npc("elissa", "Elissa", "uzer", 3, 3, 0xF0C8B3, 0x6A6AA1, 0x4A4A7B, 0xC08A3D);
        b.item("statuette", "Statuette", "statue", 0xC0A062, "A statuette from the Varrock Museum.");
        b.quest("the_golem", "The Golem", Difficulty.INTERMEDIATE, 1)
                .req(Skill.CRAFTING, 20).req(Skill.THIEVING, 25).xp(Skill.CRAFTING, 1000).xp(Skill.THIEVING, 1000)
                .stage("I can start this quest by speaking to the clay golem in the ruins of Uzer. (Requires 20 Crafting, 25 Thieving.)")
                .talk("clay_golem").say("I must protect Uzer from the demon Thammaron! But I am cracked. Repair me with soft clay.")
                .stage("I should repair the golem with 4 soft clay.")
                .talk("clay_golem").take("clay:4").say("I am whole! Now I must find the demon. The archaeologist Elissa knows of Uzer's history.")
                .stage("I should ask Elissa about the demon of Uzer.")
                .talk("elissa").say("Thammaron was defeated long ago. A statuette in the Varrock Museum opens the throne room. Curator Haig Halen has it.")
                .stage("I should get the statuette from the Varrock Museum. Curator Haig Halen keeps the display case key on him.")
                .talk("curator_haig_halen").reqSkill(Skill.THIEVING, 25).give("statuette")
                .say("The Uzer statuette? Certainly not, it's a priceless exhibit!",
                        "* While he isn't looking, you pick his pocket for the display case key and take the statuette.")
                .missing("The Uzer statuette? Certainly not! *He pats the key in his pocket.* (Requires 25 Thieving.)")
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
        b.npcAt("father_reen_al_kharid", "Father Reen", 3271, 3159, 0xE0B8AA, 0x2A2A2E, 0x1A1A20, 0x6A6A6C); // cache tile
        b.quest("shadow_of_the_storm", "Shadow of the Storm", Difficulty.INTERMEDIATE, 1)
                .reqQuest("DEMON_SLAYER", "the_golem").req(Skill.CRAFTING, 30).reward("darklight")
                // 10,000 XP in a combat skill other than Prayer: the player picks (QuestLamps)
                .stage("I can start this quest by speaking to Father Reen south of the Al Kharid bank. (Requires Demon Slayer, The Golem, 30 Crafting.)")
                .talk("father_reen_al_kharid").say("A cult in Uzer plans to summon the demon Agrith-Naar! Your Silverlight could stop it.",
                        "Meet me in the ruins of Uzer.")
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
                // 42 Mining only for the optional Tunnel of Chaos route (OSRS Wiki: recommended, not required).
                .reqQuest("RUNE_MYSTERIES").req(Skill.RUNECRAFT, 35).xp(Skill.RUNECRAFT, 8000).xp(Skill.DEFENCE, 2000).reward("beacon_ring")
                .stage("I can start this quest by speaking to Rat Burgiss on the road south of Varrock. (Requires Rune Mysteries, 35 Runecraft, 42 Mining.)")
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
        b.npcAt("elias_white_dov", "Elias White", 3283, 3501, 0xE0B89B, 0x6A6A8B, 0x4A4A5C, 0x2A2A2C); // the Jolly Boar Inn
        b.boss(BossDef.of("arrav", "Arrav").level(159).stats(180, 130, 130, 120, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, 20, 60)
                .attack(AttackDef.of("zombie_sword", AttackType.SLASH, 22, 2.5, 10))
                .drops(Drop.always("bones")).skin(0x8AA07B, 0xA08A3A, 0x6A5A2A, 0x5A6A4A).at("varrock_sewers_arrav").arena(10, "crypt"));
        b.quest("defender_of_varrock", "Defender of Varrock", Difficulty.EXPERIENCED, 2)
                .req(Skill.SMITHING, 55).req(Skill.HUNTER, 52)
                .reqQuest("shield_of_arrav", "temple_of_ikov", "below_ice_mountain", "family_crest", "what_lies_below", "ROMEO_AND_JULIET", "DEMON_SLAYER")
                .reqQuest("garden_of_tranquillity")
                .xp(Skill.SMITHING, 15000).xp(Skill.HUNTER, 15000)
                .stage("I can start this quest by speaking to Elias White in the Jolly Boar Inn. (Requires Shield of Arrav, What Lies Below and more.)")
                .talk("elias_white_dov").say("Zombies are rising beneath Varrock! An ancient hero, Arrav, leads them, raised by Zamorakians.",
                        "Captain Rovin at the palace must be warned.")
                .stage("I should go into the sewers and confront the undead hero Arrav.")
                .reach("jolly_boar_inn", 8).stay().unlessFlag("dov_trail").flag("dov_trail")
                .say("* With Elias White you follow the trail of plants, rocks and bushes to a trapdoor, collecting a grubby key on the way.")
                .kill("arrav").needFlag("dov_trail").flag("dov_arrav").say("* Arrav, hero of Avarrocka, is freed from the Zamorakian curse. His spirit thanks you.")
                .stage("I should report to Captain Rovin.")
                .reach("camdozaal", 10).stay().needFlag("dov_arrav").unlessFlag("dov_shield").take("chaos_core").flag("dov_shield")
                .say("* Ramarno lets you use the Sacred Forge: you imbue barronite with a chaos core and forge a shield.")
                .missing("* The Sacred Forge needs a chaos core to imbue the barronite.")
                .talk("captain_rovin").needFlag("dov_shield").missing("Elias told me what you found. Ramarno in Camdozaal could forge a shield against Arrav.")
                .say("Varrock is safe! The city honours you, defender.");
    }

    private static void inAidOfTheMyreque(QuestBook b) {
        b.npc("cornelius", "Cornelius", "burgh_de_rott", 3, 3, 0xE0B8AB, 0x5A4A3A, 0x3A2A1A, 0x5A3A21);
        b.npc("florin", "Florin", "burgh_de_rott", -3, 3, 0xE0B8AC, 0x6A5A3C, 0x4A3A2C, 0x8A5A2D);
        // Wieldable (round 17): a silver weapon that deals full damage to tier 2 vampyres (juvinates). Charges not modelled.
        b.items(s -> s.add("rod_of_ivandis", "Rod of Ivandis", Kind.MELEE_WEAPON, "staff", 0xC0C0D1).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, -1, 7, 4, 0, 2, 3, 1, 4, 0, 3, 0, 0, 0),
                        Map.of(), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.MELEE))
                .examine("A rod that can bind vampyre juvinates."));
        b.boss(BossDef.of("gadderanks", "Gadderanks").level(35).stats(40, 30, 30, 30, 1, 1).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 5, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xD0C8C8, 0x3A1A1A, 0x2A0A0A, 0x1A1A1A).at("burgh_de_rott").arena(8, "crypt"));
        b.quest("in_aid_of_the_myreque", "In Aid of the Myreque", Difficulty.INTERMEDIATE, 2)
                .reqQuest("in_search_of_the_myreque").req(Skill.AGILITY, 25).req(Skill.CRAFTING, 25).req(Skill.MINING, 15).req(Skill.MAGIC, 7)
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
        // The test rooms (each recruit gets 5 of these 7 at random, see recruitmentDriveTests). Sir Leye: wiki infobox (level 20, 20 HP, max hit 3).
        b.npc("sir_spishyus", "Sir Spishyus", "falador_temple_knights", 4, 0, 0xE0C0A0, 0xE0E0F2, 0x3A3A8C, 0x6A4A2A);
        b.npc("lady_table", "Lady Table", "falador_temple_knights", -4, 0, 0xF0C8B0, 0xE0E0F2, 0x3A3A8C, 0xC08A3A);
        b.npc("sir_tinley", "Sir Tinley", "falador_temple_knights", 0, 4, 0xD8B090, 0xE0E0F2, 0x3A3A8C, 0x2A2A2A);
        b.npc("sir_ren_itchood", "Sir Ren Itchood", "falador_temple_knights", 0, -4, 0xE0B898, 0xE0E0F2, 0x3A3A8C, 0x8A6A3A);
        b.npc("miss_cheevers", "Miss Cheevers", "falador_temple_knights", 4, 4, 0xF0D0B8, 0xE0E0F2, 0x3A3A8C, 0x5A2A1A);
        b.npc("ms_hynn_terprett", "Ms Hynn Terprett", "falador_temple_knights", -4, 4, 0xF0C8A8, 0xE0E0F2, 0x3A3A8C, 0xE0C060);
        b.boss(BossDef.of("sir_leye", "Sir Leye").level(20).stats(20, 18, 18, 15, 1, 1).speed(5)
                .attack(AttackDef.of("warhammer", AttackType.CRUSH, 3, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xE0B898, 0xE0E0F2, 0x3A3A8C, 0x6A4A2A));
        QuestPuzzle tests = recruitmentDriveTests();
        b.quest("recruitment_drive", "Recruitment Drive", Difficulty.NOVICE, 1)
                .reqQuest("black_knights_fortress", "DRUIDIC_RITUAL")
                .xp(Skill.PRAYER, 1000.5).xp(Skill.HERBLORE, 1000.5).xp(Skill.AGILITY, 1000.5).reward("initiate_sallet", "coins:3000") // wiki: 3,000 coins and the sallet
                .stage("I can start this quest by speaking to Sir Amik Varze in the White Knights' Castle in Falador. (Requires Black Knights' Fortress, Druidic Ritual.)")
                .talk("sir_amik_varze").say("The Temple Knights are recruiting, and I've put your name forward. Sir Tiffy Cashien in Falador Park will test you.")
                .stage("I must pass five of the Temple Knights' tests in a row (they are picked at random; Sir Kuam's duel with Sir Leye is always one). "
                        + "Sir Tiffy Cashien in Falador Park takes me to the testing grounds, with an empty inventory and nothing worn.")
                // Wiki: "In order to be able to be tested, you need to have an empty inventory and no items equipped."
                .talk("sir_tiffy_cashien").stay().emptyHanded().travel("falador_temple_knights")
                .say("I say, we're recruiting for the Temple Knights! Five tests, one after another. Fail one and it's back to the start with you.")
                .missing("You can't take anything into the testing grounds, old bean. Bank everything you carry and wear, then come back.")
                .puzzle("falador_temple_knights", tests, 0).emptyHanded().unlessFlag("rd_test0").flag("rd_test0").stay()
                .say("Well done! *A portal opens to the next room.*")
                .puzzle("falador_temple_knights", tests, 1).emptyHanded().needFlag("rd_test0").unlessFlag("rd_test1").flag("rd_test1").stay()
                .say("Well done! *A portal opens to the next room.*")
                .puzzle("falador_temple_knights", tests, 2).emptyHanded().needFlag("rd_test1").flag("rd_test2")
                .say("Well done! *A portal opens to the next room.*")
                .stage("Three tests passed. I must pass the last two of the Temple Knights' tests (with an empty inventory and nothing worn).")
                .talk("sir_tiffy_cashien").stay().emptyHanded().travel("falador_temple_knights").say("Back to it! Two tests to go.")
                .missing("Nothing goes into the testing grounds, remember. Bank it all first.")
                .puzzle("falador_temple_knights", tests, 3).emptyHanded().unlessFlag("rd_test3").flag("rd_test3").stay()
                .say("Well done! *A portal opens to the last room.*")
                .puzzle("falador_temple_knights", tests, 4).emptyHanded().needFlag("rd_test3").travel("falador_park")
                .say("* Your fifth test passed, you are teleported back to Falador Park.",
                        "@Sir Tiffy Cashien: Top-hole! You're a Temple Knight initiate now. Here's your sallet.");
    }

    /**
     * Recruitment Drive's seven test rooms (wiki): each recruit gets five in a random order, Sir Kuam Ferentse's duel
     * with Sir Leye always among them; failing one sends the recruit back to the start with a fresh draw. Sir Ren
     * Itchood's riddle wording and Sir Tinley's "stand still" test (a dialogue choice here) are approx.
     */
    static QuestPuzzle recruitmentDriveTests() {
        QuestPuzzle.Builder t = QuestPuzzle.builder("rd_tests").picks(5).always("kuam")
                .failTo(1, "rd_test0", "rd_test1", "rd_test2", "rd_test3", "rd_test4")
                .failLines("I'm afraid you have failed this test.", "* You are sent back. Speak to Sir Tiffy Cashien to start the tests again.");
        t.room("kuam").variant("sir_tiffy_training")
                .prompt("Your test is combat: defeat Sir Leye. Saradomin has blessed him so that no blade may harm him.",
                        "* A sword, claws, a battleaxe and a warhammer hang on the rack. Which do you fight with?")
                .options("Steel sword", "Steel claws", "Steel battleaxe", "Steel warhammer", "Fight unarmed")
                .solution(3).solution(4).spawnKill("sir_leye", "sir_leye");
        t.room("spishyus").variant("sir_spishyus")
                .prompt("Get the fox, the chicken and the grain across the bridge. It holds you and one of them at a time.",
                        "Never leave the fox alone with the chicken, or the chicken alone with the grain!")
                .options("Take the chicken over", "Take the fox over", "Take the grain over", "Walk back alone", "Bring the chicken back")
                .solution(0, 3, 2, 4, 1, 3, 0).solution(0, 3, 1, 4, 2, 3, 0);
        String[] colours = {"Bronze", "Silver", "Gold"};
        String[] weapons = {"sword", "halberd", "greataxe", "mace"};
        String[] statues = new String[12];
        for (int i = 0; i < 12; i++) {
            statues[i] = colours[i / 4] + " " + weapons[i % 4];
        }
        int[] scramble = {5, 2, 9, 0, 7, 11, 3, 8, 1, 10, 4, 6};
        t.room("table");
        for (int missing = 0; missing < 12; missing++) {
            List<String> seen = new java.util.ArrayList<>();
            for (int i : scramble) {
                if (i != missing) {
                    seen.add(statues[i].toLowerCase(java.util.Locale.ROOT));
                }
            }
            t.variant("lady_table").prompt("Study these statues carefully. There should be twelve: four bronze, four silver and four gold.",
                            "* Before her stand eleven knight statues: " + String.join(", ", seen) + ".", "* A moment later the room shimmers and a twelfth statue stands among them. Which one was missing?")
                    .options(statues).solution(missing);
        }
        String[][] rhymes = {
                {"BITE", "Bright is the dawn when the herald calls,", "In every hall the banners rise,", "Time runs short for those who wait,", "Every door yields to the wise."},
                {"FISH", "Few who come here pass this door,", "In riddles lies the key you need,", "Search each line, and nothing more,", "Heed the start of each, and read."},
                {"LAST", "Long have knights stood at this gate,", "All who enter must think twice,", "Speak the word and do not wait,", "Truth lies first in my advice."},
                {"MEAT", "Many a squire has failed before,", "Each one hasty, none would think,", "All the answer lies in store,", "Tied to the start of every link."},
                {"RAIN", "Read my verse from first to last,", "All the letters you will need,", "In the opening of each are cast,", "Name the word, and so proceed."},
                {"TIME", "Twice I'll tell you, then no more,", "In my words the answer lies,", "Mark the letters, unlock the door,", "Each line's start will make you wise."}};
        t.room("ren");
        for (String[] r : rhymes) {
            t.variant("sir_ren_itchood").prompt("Let me tell you a riddle: the answer opens the door's combination lock.", r[1], r[2], r[3], r[4],
                    "* The lock has four lettered dials.").options(QuestPuzzle.letters()).solution(QuestPuzzle.word(r[0]));
        }
        t.room("tinley").variant("sir_tinley")
                .prompt("Ah, a recruit. *He says nothing more.*")
                .options("Ask him what the test is", "Walk to the far door", "Stand still and say nothing")
                .solution(2);
        // Miss Cheevers' two doors (wiki): the spade handle burnt off and the spade jammed in the handle hole with cupric
        // sulfate and liquid; then a gypsum mould of the key, tin and cupric ore powder melted into it and cut free.
        t.room("cheevers").variant("miss_cheevers")
                .prompt("Both doors are stuck fast: the first has lost its handle, the second is locked. Use what's in the shelves, crates and chest.",
                        "* You have a metal spade, a knife, a cake tin, gypsum, cupric sulfate, cupric ore powder, tin ore powder, nitrous oxide and vials of liquid.")
                .options("Heat the tin on the Bunsen burner", "Pour liquid into the tin", "Put the spade in the first door's handle hole",
                        "Drink the nitrous oxide", "Add tin ore powder to the tin", "Burn the spade's wooden handle on the Bunsen burner",
                        "Pour cupric sulfate on the spade in the door", "Press the tin onto the key by the portal", "Cut the key free with the knife",
                        "Pour liquid on the door", "Add gypsum to the tin", "Add cupric ore powder to the tin", "Unlock the second door")
                .solution(5, 2, 6, 9, 1, 10, 7, 4, 11, 0, 8, 12).solution(5, 2, 6, 9, 1, 10, 7, 11, 4, 0, 8, 12);
        t.room("hynn");
        t.variant("ms_hynn_terprett").prompt("You have been captured by an enemy, who lets you pick your own demise: drowned in a lake of acid, "
                        + "burned on a fire, thrown to wolves that have not been fed in over a month, or thrown from castle walls hundreds of feet high.")
                .options("The lake of acid", "The fire", "The wolves", "The castle walls").solution(2);
        t.variant("ms_hynn_terprett").prompt("I have both a husband and a daughter. My husband is four times older than my daughter. "
                        + "In twenty years' time he will be twice as old as her. How old is my daughter now?")
                .options("5", "10", "15", "20").solution(1); // approx: the in-game answer options
        t.variant("ms_hynn_terprett").prompt("I dropped four identical stones into four identical buckets of water: the first at 32 degrees "
                        + "Fahrenheit, then 33, 34 and 35. Which bucket's stone reached the bottom last?")
                .options("Bucket A (32 degrees)", "Bucket B (33 degrees)", "Bucket C (34 degrees)", "Bucket D (35 degrees)").solution(0);
        t.variant("ms_hynn_terprett").prompt("I estimate there to be a million inhabitants of Gielinor, creatures and people. What do you get if you "
                        + "multiply the number of fingers on everything's left hand, to the nearest million?")
                .options("0", "1,000,000", "4,000,000", "5,000,000").solution(0); // approx: the in-game answer options
        t.variant("ms_hynn_terprett").prompt("Which is true? The number of false statements here is one. The number of false statements here is two. "
                        + "The number of false statements here is three. The number of false statements here is four.")
                .options("One false", "Two false", "Three false", "Four false").solution(2);
        return t.build();
    }

    private static void lostTribe(QuestBook b) {
        b.npc("sigmund_lumbridge", "Sigmund", "LUMBRIDGE", -3, 7, 0xE8C0BB, 0x3A5A8C, 0x2A3A5C, 0xA0A0A2);
        b.npc("mistag", "Mistag", "dorgesh_kaan_mine", 0, 2, 0xA0B8A0, 0x5A4A3D, 0x3A2A1F, 0x6A8A6A);
        b.item("silverware", "Silverware", "bar", 0xD0D0E0, "The Duke's stolen silverware.");
        b.quest("the_lost_tribe", "The Lost Tribe", Difficulty.INTERMEDIATE, 1)
                .reqQuest("goblin_diplomacy", "RUNE_MYSTERIES").req(Skill.AGILITY, 13).req(Skill.THIEVING, 13).req(Skill.MINING, 17)
                .xp(Skill.MINING, 3000)
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
                .xp(Skill.RUNECRAFT, 1000) // OSRS (current quest XP rewards)
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
