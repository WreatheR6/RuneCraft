package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** Free-to-play quests. */
final class F2pQuests {
    private F2pQuests() {
    }

    static void register(QuestBook b) {
        places(b);
        dragonSlayer(b);
        blackKnightsFortress(b);
        ernestTheChicken(b);
        goblinDiplomacy(b);
        piratesTreasure(b);
        princeAliRescue(b);
        knightsSword(b);
        shieldOfArrav(b);
        xMarksTheSpot(b);
        belowIceMountain(b);
        misthalinMystery(b);
        corsairCurse(b);
        learningTheRopes(b);
    }

    private static void places(QuestBook b) {
        b.place("champions_guild", "Champions' Guild", 3191, 3362, PlaceDef.Style.TOWN);
        b.spot("port_sarim_jail", "Port Sarim jail", 3013, 3186);
        b.place("melzars_maze", "Melzar's Maze", 2933, 3250, PlaceDef.Style.RUINS);
        b.spot("ice_mountain", "Ice Mountain", 3008, 3477);
        b.place("crandor", "Crandor", 2851, 3238, PlaceDef.Style.RUINS);
        b.place("goblin_village", "Goblin Village", 2956, 3506, PlaceDef.Style.CAMP);
        b.spot("blue_moon_inn", "Blue Moon Inn", 3226, 3398);
        b.spot("falador_park", "Falador Park", 2994, 3376);
        b.spot("falador_castle", "White Knights' Castle", 2965, 3340);
        b.place("black_knights_fortress", "Black Knights' Fortress", 3016, 3514, PlaceDef.Style.RUINS);
        b.spot("draynor_jail", "Draynor jail", 3127, 3245);
        b.spot("mudskipper_point", "Mudskipper Point", 2997, 3145);
        b.spot("varrock_palace", "Varrock Palace", 3212, 3474);
        b.spot("varrock_museum", "Varrock Museum", 3256, 3448);
        b.spot("phoenix_hideout", "Phoenix Gang hideout", 3245, 3383);
        b.spot("black_arm_hideout", "Black Arm Gang hideout", 3185, 3385);
        b.spot("lumbridge_swamp_dock", "Lumbridge Swamp jetty", 3237, 3150);
        b.place("misthalin_mansion", "Lacey's mansion", 3240, 3128, PlaceDef.Style.RUINS);
        b.place("camdozaal", "Ruins of Camdozaal", 2997, 3494, PlaceDef.Style.CAVE);
        b.spot("dwarven_mine_north", "Dwarven Mine entrance", 3016, 3450);
        b.place("corsair_cove", "Corsair Cove", 2570, 2860, PlaceDef.Style.CAMP);
        b.place("ithoi_hut", "Ithoi's hut", 2529, 2840, PlaceDef.Style.CAMP);
        b.spot("varrock_church", "Varrock church", 3256, 3485);
    }

    // ---------------------------------------------------------------- Dragon Slayer I
    private static void dragonSlayer(QuestBook b) {
        b.npc("guildmaster", "Guildmaster", "champions_guild", 2, 2, 0xE8C4A0, 0x2A3A8A, 0x2A2A5A, 0x9A9A9A);
        b.npc("oziach", "Oziach", "EDGEVILLE", -6, 7, 0xD0A080, 0x5A4A3A, 0x3A2A1A, 0x8A6A4A);
        b.npc("wormbrain", "Wormbrain", "port_sarim_jail", 0, 1, 0x6A9A3A, 0x6A5A3A, 0x4A3A2A, 0x3A5A1A);
        b.npc("oracle", "Oracle", "ice_mountain", 0, 0, 0xF0D8C8, 0xE8E8F0, 0xE8E8F0, 0xF0F0F0);
        b.npc("klarense", "Klarense", "PORT_SARIM", 6, -6, 0xE0B890, 0x3A6A9A, 0x2A2A3A, 0x6A4A2A);
        b.npc("ned", "Ned", "DRAYNOR_VILLAGE", -5, 6, 0xE8C8B0, 0x8A7A5A, 0x5A4A3A, 0xD8D8D8);
        b.npc("hans", "Hans", "LUMBRIDGE", 7, 2, 0xE8C0A0, 0x8A2A2A, 0x3A3A3A, 0x5A3A1A);
        b.npc("emily_bartender", "Emily", "FALADOR", -6, -6, 0xF0C8A8, 0xA03A6A, 0x5A2A3A, 0xC08A3A);
        b.item("map_part_melzar", "Map part (Melzar)", "note", 0xD8C8A0, "A piece of the map to Crandor, from Melzar's maze.");
        b.item("map_part_lozar", "Map part (Lozar)", "note", 0xD8C098, "A piece of the map to Crandor, from Wormbrain.");
        b.item("map_part_thalzar", "Map part (Thalzar)", "note", 0xD0C0A0, "A piece of the map to Crandor, from behind the magic door.");
        b.item("crandor_map", "Crandor map", "note", 0xE0D0A0, "A complete map showing the route to Crandor.");
        b.item("elvargs_head", "Elvarg's head", "skull", 0x4A5A26, "The severed head of the great dragon Elvarg.");
        b.item("wizards_mind_bomb", "Wizard's mind bomb", "beer", 0xC0A040, "It's got strange bubbles in it.");

        b.boss(BossDef.of("melzar_the_mad", "Melzar the Mad").level(43).stats(44, 1, 1, 30, 52, 1).speed(5)
                .defenceBonus(0, 0, 0, 25, 0)
                .attack(AttackDef.of("fire_strike", AttackType.MAGIC, 8, 8, 6).particle("minecraft:flame"))
                .attack(AttackDef.of("staff", AttackType.CRUSH, 4, 2.5, 4))
                .drops(Drop.always("bones"), Drop.of("mind_rune", 5, 10, 2))
                .skin(0xE0C0A0, 0x6A2A8A, 0x4A1A5A, 0x9A9A9A).at("melzars_maze").arena(8, "crypt"));
        b.boss(BossDef.of("elvarg", "Elvarg").level(83).stats(80, 70, 70, 70, 70, 1).speed(4)
                .defenceBonus(40, 40, 40, 30, 40)
                .attack(AttackDef.of("claw", AttackType.SLASH, 8, 3, 6))
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 50, 10, 5).special(Special.DRAGONFIRE, 0)
                        .telegraph(2, "Elvarg rears back and breathes fire!").particle("minecraft:flame"))
                .phase(40, "Elvarg roars in fury!", 1.2, false)
                .drops(Drop.always("dragon_bones"))
                .size(2.2f, 2.0f, 1.0f).at("crandor").arena(14, "lava"));

        b.quest("dragon_slayer_i", "Dragon Slayer I", Difficulty.EXPERIENCED, 2)
                .qp(32).xp(Skill.STRENGTH, 18650).xp(Skill.DEFENCE, 18650)
                .unlock("Ability to wear rune platebodies and green d'hide bodies")
                .stage("I can start this quest by speaking to the Guildmaster in the Champions' Guild. (Requires 32 quest points.)")
                .talk("guildmaster").say("Greetings, champion. If you seek a challenge worthy of a hero, speak to Oziach in Edgeville.",
                        "> What does Oziach want?", "He sells the finest armour: rune platebodies. But he only sells to those who have proved themselves.")
                .stage("I should speak to Oziach in Edgeville about buying a rune platebody.")
                .talk("oziach").say("> I'd like to buy a rune platebody.", "Ha! Ye'll have to prove yerself first. Go and slay the dragon of Crandor!",
                        "> A dragon? Where is Crandor?", "Ask the Guildmaster, he knows more about such things than old Oziach.")
                .stage("I should ask the Guildmaster how to reach Crandor and slay Elvarg.")
                .talk("guildmaster").say("Elvarg! The great dragon who laid waste to Crandor. To get there you'll need a map, a ship and protection from her fire.",
                        "The map was split into three pieces. Melzar the Mad has one in his maze near Rimmington, Lozar's piece was stolen by the goblin Wormbrain, now in Port Sarim jail, and Thalzar's lies behind a magic door in the Dwarven Mine. The Oracle on Ice Mountain knows how to open it.",
                        "Klarense in Port Sarim has a ship for sale, and the Duke of Lumbridge has anti-dragon shields. Hans in the castle can fetch you one.")
                .stage("I need the three pieces of the Crandor map, the ship Lady Lumbridge from Klarense (patched with 3 planks and 90 steel nails), a captain (Ned in Draynor) and an anti-dragon shield.")
                .kill("melzar_the_mad").stay().unless("map_part_melzar", "crandor_map").give("map_part_melzar")
                .say("* Melzar collapses. Behind him you find a dusty chest holding a piece of the map.")
                .talk("wormbrain").stay().unless("map_part_lozar", "crandor_map").take("coins:10000").give("map_part_lozar")
                .say("> I hear you stole a map piece.", "Me no stupid. Me sell map piece. 10,000 coins!", "> Fine. Here.")
                .missing("Map piece cost 10,000 shiny coins. You not have. Go away.")
                .talk("oracle").stay().unless("map_part_thalzar", "crandor_map").take("unfired_bowl", "silk", "lobster_pot", "wizards_mind_bomb").give("map_part_thalzar")
                .say("The magic door seeks a bowl unfired, silk of a merchant, a pot for lobsters and a mind's bomb.",
                        "* You place the items before the door in the Dwarven Mine. It swings open, and inside is Thalzar's map piece.")
                .missing("The door seeks 'a thing which is not quite a bowl, silk that is fine, a pot for lobsters and that which makes the mind explode'.",
                        "* (You need an unfired bowl, silk, a lobster pot and a wizard's mind bomb.)")
                .talk("emily_bartender").stay().unless("wizards_mind_bomb").take("coins:3").give("wizards_mind_bomb")
                .say("One Wizard's Mind Bomb coming up. That's 3 coins.")
                .talk("hans").stay().unless("anti_dragon_shield").give("anti_dragon_shield")
                .say("The Duke heard you're going to fight a dragon. He asked me to give you this anti-dragon shield.")
                .talk("klarense").stay().unlessFlag("ds1_ship").take("coins:2000").flag("ds1_ship")
                .say("The Lady Lumbridge? She's yours for 2,000 coins. Bit of a hole in the hull, mind.", "> I'll patch it up.")
                .missing("She's a fine ship. 2,000 coins and she's yours.")
                // Wiki walkthrough: the Lady Lumbridge's hull is patched with 3 planks and 90 steel nails (30 a plank) and a hammer.
                .talk("klarense").stay().needFlag("ds1_ship").unlessFlag("ds1_hull").need("hammer").take("plank:3", "steel_nails:90").flag("ds1_hull")
                .say("> I've come to patch the hull.", "* You nail three planks over the hole in the Lady Lumbridge's hull.")
                .missing("She'll sink as she is. Patch the hole with three planks, 90 steel nails and a hammer.")
                .talk("ned").needFlag("ds1_ship", "ds1_hull").take("map_part_melzar", "map_part_lozar", "map_part_thalzar").need("anti_dragon_shield")
                .travel("crandor")
                .say("A ship and a map to Crandor? I've not sailed in years. Let's go!",
                        "* Ned pieces the map together and steers the Lady Lumbridge west. A rock shatters the hull as you run aground on Crandor!")
                .missing("I'd sail you anywhere, but you'll need a ship, all three map pieces and an anti-dragon shield first.")
                .hint("oziach", "Crandor's that way, past the sea. Bring me proof ye've slain the beast.")
                .stage("I've reached Crandor. I must slay Elvarg in her lair.")
                .kill("elvarg").give("elvargs_head").say("* With a final roar Elvarg crashes to the ground. You cut off her head as proof.")
                .talk("ned").stay().travel("PORT_SARIM").say("Back to the mainland? Hold on tight!")
                .stage("I should bring Elvarg's head to Oziach.")
                .talk("oziach").take("elvargs_head").say("Ye killed Elvarg? Well I'll be! Ye've proved yerself a true champion.",
                        "I'll sell ye a rune platebody whenever ye like. And ye've earned the right to wear one.")
                .done("oziach", "Ye can wear rune platebodies now, champion.")
                .done("guildmaster", "The slayer of Elvarg! Welcome, champion.");
        // Oziach's Armour: rune platebodies for those who have slain Elvarg. 84,500 coins (130% of value). // approx
        b.shop("oziach", "rune_platebody", 84500, "dragon_slayer_i", null, null, 0,
                "A rune platebody for the slayer of Elvarg? 84,500 coins, and worth every one.");
    }

    // ---------------------------------------------------------------- Black Knights' Fortress
    private static void blackKnightsFortress(QuestBook b) {
        b.npc("sir_amik_varze", "Sir Amik Varze", "falador_castle", 0, 0, 0xE8C8A8, 0xD8D8E0, 0xB0B0C0, 0xC0A060);
        b.npc("black_knight_witch", "Witch", "black_knights_fortress", 2, -2, 0xB0C090, 0x2A2A2A, 0x1A1A1A, 0x3A1A3A);
        b.quest("black_knights_fortress", "Black Knights' Fortress", Difficulty.INTERMEDIATE, 3)
                .qp(12).reward("coins:2500")
                .stage("I can start this quest by speaking to Sir Amik Varze in the White Knights' Castle in Falador. (Requires 12 quest points.)")
                .talk("sir_amik_varze").say("The Black Knights are brewing an invincibility potion in their fortress! We need a spy.",
                        "Disguise yourself as a guard: you'll need an iron chainbody and a bronze med helm.")
                .stage("I need to infiltrate the Black Knights' Fortress disguised as a guard (bronze med helm and iron chainbody).")
                .reach("black_knights_fortress", 10).need("bronze_med_helm", "iron_chainbody")
                .say("* Wearing your disguise, you slip past the guards. Through a grill you overhear: the potion needs only a cabbage to finish!")
                .stage("The potion only needs a cabbage. I should sabotage it by dropping a cabbage into the witch's cauldron.")
                .talk("black_knight_witch").take("cabbage")
                .say("* While the witch isn't looking you drop the cabbage down the hole into her cauldron.",
                        "What?! A cabbage from Draynor? The potion is ruined! Who did this?!")
                .missing("Hehehe, the potion just needs one more ingredient...")
                .stage("The potion is ruined. I should report back to Sir Amik Varze.")
                .talk("sir_amik_varze").say("Splendid work! The Black Knights' plans are foiled. Take this reward with Falador's thanks.")
                .done("sir_amik_varze", "The White Knights are in your debt.");
    }

    // ---------------------------------------------------------------- Ernest the Chicken
    private static void ernestTheChicken(QuestBook b) {
        b.npc("veronica", "Veronica", "DRAYNOR_MANOR", 0, 8, 0xF0D0B8, 0xD86A9A, 0xA04A7A, 0xE0C060);
        b.npc("professor_oddenstein", "Professor Oddenstein", "DRAYNOR_MANOR", -3, -2, 0xE8D0C0, 0xE8E8E8, 0x6A6A6A, 0xE0E0E0);
        b.npc("ernest", "Ernest", "DRAYNOR_MANOR", 3, -2, 0xE0B898, 0x3A5A8A, 0x2A3A5A, 0x5A3A1A);
        b.item("oil_can", "Oil can", "pot", 0x5A5A5A, "Its oily contents are the only thing that can save Ernest.");
        b.item("pressure_gauge", "Pressure gauge", "gem", 0xC0C0C0, "A pressure gauge, found in the fountain.");
        b.item("rubber_tube", "Rubber tube", "string", 0x2A2A2A, "A rubber tube from Draynor Manor.");
        b.item("fish_food", "Fish food", "pot", 0xC0A060, "Keeps your pet fish strong and healthy.");
        b.item("poison", "Poison", "potion", 0x6A8A2A, "This stuff looks nasty.");
        b.item("poisoned_fish_food", "Poisoned fish food", "pot", 0x8AA040, "Doesn't seem very nice to the poor fishes.");
        b.item("manor_key", "Key", "key", 0x8A7A5A, "A key from the compost heap behind Draynor Manor.");
        // Object locations from Zoinkwiz/quest-helper (OSRS tiles).
        b.spot("draynor_manor_fountain", "Draynor Manor fountain", 3088, 3335);
        b.spot("draynor_manor_compost", "Draynor Manor compost heap", 3085, 3361);
        b.spot("draynor_manor_poison_room", "Draynor Manor north-west room", 3097, 3366);
        b.spot("draynor_manor_tube_room", "Draynor Manor small room", 3111, 3367);
        b.spot("draynor_manor_secret_room", "Draynor Manor secret room", 3096, 3357);
        b.quest("ernest_the_chicken", "Ernest the Chicken", Difficulty.NOVICE, 4)
                .reward("coins:300")
                .stage("I can start this quest by speaking to Veronica outside Draynor Manor.")
                .talk("veronica").say("My fiance Ernest went into this manor hours ago and hasn't come back!", "> I'll look for him.")
                .stage("I should look for Ernest inside Draynor Manor.")
                .talk("professor_oddenstein").say("Ernest? Ah. I'm afraid my latest invention turned him into a chicken.",
                        "I can change him back, but the poltergeists have hidden parts of my machine: a pressure gauge, an oil can and a rubber tube.")
                .stage("Professor Oddenstein needs a pressure gauge, a rubber tube and an oil can to turn Ernest back into a human.")
                // Pressure gauge: fish food (upstairs) + poison (north-west room), fed to the fountain's piranhas.
                .reach("DRAYNOR_MANOR", 6).stay().unless("fish_food", "poisoned_fish_food", "pressure_gauge").give("fish_food")
                .say("* Upstairs in the manor you pick up some fish food.")
                .reach("draynor_manor_poison_room", 4).stay().unless("poison", "poisoned_fish_food", "pressure_gauge").give("poison")
                .say("* You pick up a bottle of poison from the shelf.")
                .reach("draynor_manor_fountain", 4).stay().unless("pressure_gauge").take("fish_food", "poison").give("pressure_gauge")
                .say("* You mix the poison into the fish food and drop it in the fountain. The piranhas die.",
                        "* You search the fountain and find a pressure gauge!")
                .missing("* There seems to be a pressure gauge in here... and a lot of very hungry piranhas.",
                        "* (You need fish food and poison to deal with them.)")
                // Rubber tube: the key in the compost heap (needs a spade) opens the small room north of the stairs.
                .reach("draynor_manor_compost", 4).stay().unless("manor_key", "rubber_tube").need("spade").give("manor_key")
                .say("* You dig through the compost heap and find a key.")
                .missing("* The compost heap smells awful. You'd need a spade to dig through it.")
                .reach("draynor_manor_tube_room", 3).stay().unless("rubber_tube").take("manor_key").give("rubber_tube")
                .say("* You unlock the door with the key and pick up a rubber tube.")
                .missing("* The door is locked.")
                // Oil can: behind the bookcase, down the ladder, through the lever puzzle (A, B, D, A, B up, F, E, C, E...).
                .reach("draynor_manor_secret_room", 3).stay().unless("oil_can").give("oil_can")
                .say("* Behind the bookcase a ladder leads to a basement of doors and levers.",
                        "* After pulling the levers in the right order you reach the last room and pick up an oil can.")
                .talk("professor_oddenstein").take("pressure_gauge", "rubber_tube", "oil_can")
                .say("Excellent! Stand back...", "* There is a flash and a puff of smoke. Ernest stands before you, a man once more.",
                        "@Ernest: Thank you! Being a chicken was terrible. Here, take this for your trouble.")
                .missing("I still need the pressure gauge, the rubber tube and the oil can. The fountain, the locked room and the basement, I'd wager.")
                .done("ernest", "Thanks again for turning me back into a human!")
                .done("veronica", "Ernest is back! Thank you, adventurer.");
    }

    // ---------------------------------------------------------------- Goblin Diplomacy
    private static void goblinDiplomacy(QuestBook b) {
        b.npc("general_bentnoze", "General Bentnoze", "goblin_village", 2, 0, 0x6A9A3A, 0x3A4ABA, 0x3A3A3A, 0x3A5A2A);
        b.npc("general_wartface", "General Wartface", "goblin_village", -2, 0, 0x5A8A2A, 0xC06A2A, 0x3A3A3A, 0x2A4A1A);
        b.npc("aggie", "Aggie", "DRAYNOR_VILLAGE", 6, -5, 0xC0D0A0, 0x2A2A4A, 0x1A1A2A, 0x2A2A2A);
        b.item("goblin_mail", "Goblin mail", "platebody", 0x8A7A5A, "Armour fit for a goblin.");
        b.item("orange_goblin_mail", "Orange goblin mail", "platebody", 0xD07A2A, "Armour fit for a goblin, dyed orange.");
        b.item("blue_goblin_mail", "Blue goblin mail", "platebody", 0x3A5AC0, "Armour fit for a goblin, dyed blue.");
        b.quest("goblin_diplomacy", "Goblin Diplomacy", Difficulty.NOVICE, 5)
                .xp(Skill.CRAFTING, 200).reward("gold_bar")
                .stage("I can start this quest by speaking to the goblin generals in the Goblin Village.")
                .talk("general_bentnoze").say("Red armour best!", "@General Wartface: No! Green armour best!",
                        "> Why not try a different colour entirely?", "Hmm... yes, human find us goblin mail of new colour!")
                .stage("The generals want goblin mail of a new colour. I should collect three goblin mails from goblins.")
                .drop("goblin", "goblin_mail", 4)
                .talk("aggie").take("goblin_mail:3", "coins:10").give("goblin_mail", "orange_goblin_mail", "blue_goblin_mail")
                .say("Want some armour dyed, dearie? Red and yellow make orange, and woad leaves make blue.",
                        "* Aggie dyes two of your goblin mails orange and blue.")
                .missing("Bring me three goblin mails and 10 coins and I'll dye them for you, dearie.")
                .hint("general_bentnoze", "Human find goblin mail? Goblins in village have some.")
                .stage("I should show the generals the orange goblin mail.")
                .talk("general_wartface").take("orange_goblin_mail").say("Orange armour? ...No. Too bright. Try another colour!")
                .stage("The generals didn't like orange. I should try the blue goblin mail.")
                .talk("general_bentnoze").take("blue_goblin_mail").say("Blue armour? Hmm. No, this not good either!")
                .stage("Blue didn't work either. Perhaps plain brown goblin mail?")
                .talk("general_wartface").take("goblin_mail").say("Brown armour! Brown armour best!",
                        "@General Bentnoze: Yes! Brown armour best! No more war! Human take shiny gold reward.")
                .done("general_bentnoze", "Brown armour best!");
    }

    // ---------------------------------------------------------------- Pirate's Treasure
    private static void piratesTreasure(QuestBook b) {
        b.npc("redbeard_frank", "Redbeard Frank", "PORT_SARIM", -6, 6, 0xD8A880, 0x8A1A1A, 0x2A2A2A, 0xA02020);
        b.npc("luthas", "Luthas", "MUSA_POINT", 4, 4, 0xB08060, 0xE0D0A0, 0x6A5A3A, 0x3A2A1A);
        b.npc("zambo", "Zambo", "MUSA_POINT", -4, 4, 0x8A5A3A, 0xD0B03A, 0x4A3A2A, 0x1A1A1A);
        b.npc("wyson_the_gardener", "Wyson the gardener", "falador_park", 2, 1, 0xE0B898, 0x5A8A3A, 0x6A4A2A, 0xC0B080);
        b.item("karamjan_rum", "Karamjan rum", "beer", 0x8A3A1A, "A very strong spirit brewed in Karamja.");
        b.item("chest_key", "Chest key", "key", 0xB09040, "A key to One-Eyed Hector's chest.");
        b.item("pirate_message", "Pirate message", "note", 0xE0D8B0, "'Visit the city of the White Knights. In the park, Saradomin points to the X which marks the spot.'");
        b.quest("pirates_treasure", "Pirate's Treasure", Difficulty.NOVICE, 2)
                .reward("coins:450", "gold_ring", "emerald")
                .stage("I can start this quest by speaking to Redbeard Frank in Port Sarim.")
                .talk("redbeard_frank").say("Arr, matey! I know where One-Eyed Hector's treasure be hidden. Fetch me some Karamjan rum and I'll tell ye.")
                .stage("Redbeard Frank wants Karamjan rum. Zambo sells it on Karamja; Luthas the plantation owner might help me smuggle it past customs.")
                .talk("luthas").stay().unlessFlag("pt_job").flag("pt_job")
                .say("Fill my crate with bananas and I'll ship it to Wydin's store in Port Sarim. Your rum can hide under the bananas.")
                .talk("zambo").needFlag("pt_job").take("coins:30").give("karamjan_rum")
                .say("Karamjan rum, 30 coins. *You hide the bottle in Luthas's banana crate and collect it from Wydin's store.*")
                .missing("Hey, I sell Karamjan rum. You'll never get it past customs without a crate though... ask Luthas.")
                .stage("I should take the rum to Redbeard Frank.")
                .talk("redbeard_frank").take("karamjan_rum").give("chest_key")
                .say("Arr, that be the stuff! Here be the key to Hector's chest, upstairs in the Blue Moon Inn in Varrock.")
                .stage("I should open One-Eyed Hector's chest upstairs in the Blue Moon Inn, Varrock.")
                .reach("blue_moon_inn", 6).take("chest_key").give("pirate_message")
                .say("* You unlock the chest. Inside is a message.")
                .stage("The message says to dig where Saradomin points in Falador Park.")
                .reach("falador_park", 6).say("* You dig beside the statue of Saradomin...",
                        "@Wyson the gardener: Oi! Stop digging up my flowers!", "* You find One-Eyed Hector's casket!")
                .finish()
                .done("redbeard_frank", "Arr, enjoy the treasure, matey!")
                .done("wyson_the_gardener", "Those flowers took me weeks to grow!");
    }

    // ---------------------------------------------------------------- Prince Ali Rescue
    private static void princeAliRescue(QuestBook b) {
        b.npc("hassan", "Hassan", "AL_KHARID", 4, -6, 0xB0784A, 0xE8E8E0, 0xD0B060, 0xE8E8E8);
        b.npc("osman", "Osman", "AL_KHARID", 6, -4, 0xA06A40, 0x3A3A3A, 0x2A2A2A, 0x5A1A1A);
        b.npc("leela", "Leela", "draynor_jail", 4, 4, 0xC08A5A, 0x8A3A6A, 0x3A2A3A, 0x2A1A1A);
        b.npc("lady_keli", "Lady Keli", "draynor_jail", -2, 0, 0xE0C0A0, 0x6A1A1A, 0x2A1A1A, 0xC0A030);
        b.npc("joe_jail_guard", "Joe", "draynor_jail", 1, -2, 0xE8C0A0, 0x6A6A7A, 0x3A3A4A, 0x5A5A6A);
        b.npc("prince_ali", "Prince Ali", "draynor_jail", 0, 2, 0xB08050, 0xF0E0A0, 0xE0C060, 0xF0F0F0);
        b.item("key_print", "Key print", "note", 0xA08060, "An imprint of the key to Prince Ali's cell, in soft clay.");
        b.item("bronze_key", "Bronze key", "key", 0x9A6A3A, "A copy of the key to the cell.");
        b.item("blonde_wig", "Wig", "wool", 0xF0E080, "A blonde wig made of wool.");
        b.item("skin_paste", "Paste", "pot", 0xE8C8A0, "A paste to make skin look lighter.");
        b.item("pink_skirt", "Pink skirt", "cloth", 0xF0A0C0, "A pink skirt.");
        b.quest("prince_ali_rescue", "Prince Ali Rescue", Difficulty.NOVICE, 3)
                .reward("coins:700")
                .stage("I can start this quest by speaking to Hassan in the Al Kharid palace.")
                .talk("hassan").say("The Emir's son, Prince Ali, has been kidnapped by Lady Keli! Please speak to Osman, our spymaster.")
                .stage("I should speak to Osman, the Emir's spymaster.")
                .talk("osman").say("Ali is held in the jail east of Draynor. You'll need a key, a disguise for the prince and a way to deal with Lady Keli.",
                        "Leela, our agent in Draynor, will help you. Bring her: an imprint of the key with a bronze bar, a blonde wig, skin paste, a pink skirt and some rope.")
                .stage("I should gather: a key print of Keli's key and a bronze bar, a wig, skin paste, a pink skirt, rope and beer for the guard.")
                .talk("lady_keli").stay().unless("key_print", "bronze_key").take("clay").give("key_print")
                .say("I'm Lady Keli, the most fearsome kidnapper in Gielinor! You want to see the key? Here... *You press it into your soft clay*")
                .missing("Ha! You'd need something soft to copy my key with. Not that I'd let you.")
                .talk("osman").stay().unless("bronze_key").take("key_print", "bronze_bar").give("bronze_key")
                .say("A key print and a bronze bar? I'll have a key made at once.")
                .talk("aggie").stay().unless("skin_paste").take("pot_of_flour", "vial_of_water").give("skin_paste")
                .say("Skin paste? Flour and water mixed with some of my special herbs. There you are, dearie.")
                .missing("Bring me a pot of flour and a vial of water and I'll make you paste, dearie.")
                .talk("ned").stay().unless("blonde_wig").take("ball_of_wool:3").give("blonde_wig")
                .say("A wig? I can knit one from three balls of wool. There you are!")
                .missing("Bring me three balls of wool and I'll knit you a wig.")
                .talk("leela").stay().unless("pink_skirt").take("coins:17").give("pink_skirt", "rope")
                .say("I can buy you a pink skirt from the clothes shop and rope from the general store. 17 coins.")
                .talk("leela").take("bronze_key", "blonde_wig", "skin_paste", "pink_skirt", "rope", "beer:3")
                .say("You have everything! I'll take care of the guard with the beer. Go, tie up Lady Keli and free the prince!")
                .missing("I need the key, the wig, the paste, the skirt, rope and three beers for the guard Joe.")
                .stage("With the guard drunk, I should tie up Lady Keli and free Prince Ali.")
                .talk("prince_ali").say("* You tie up Lady Keli and hand the disguise to Prince Ali.",
                        "Thank you, brave adventurer! I shall slip out disguised as Lady Keli. Tell my father Hassan I am safe!")
                .stage("I should return to Hassan in Al Kharid.")
                .talk("hassan").say("You have rescued the prince! The Emir thanks you. You may pass the Al Kharid gate for free from now on.")
                .done("hassan", "You are a friend of Al Kharid.")
                .done("joe_jail_guard", "Hic! Where'd the prinshe go?");
        // Real sources: wig from Ned (3 balls of wool + yellow dye), paste from Aggie (flour, water, ashes, redberries),
        // skirt from the Varrock clothes shop. Simplified to built-in items.
    }

    // ---------------------------------------------------------------- The Knight's Sword
    private static void knightsSword(QuestBook b) {
        b.npc("squire", "Squire", "falador_castle", 3, 2, 0xF0C8A8, 0xC0C0C8, 0x3A3A6A, 0x8A5A2A);
        b.npc("reldo", "Reldo", "varrock_palace", -3, 3, 0xE8C8B0, 0x6A3A8A, 0x4A2A5A, 0xD0D0D0);
        b.npc("thurgo", "Thurgo", "mudskipper_point", 0, 0, 0xD8A880, 0x8A4A2A, 0x3A2A1A, 0xC06A2A);
        b.npc("sir_vyvin", "Sir Vyvin", "falador_castle", -3, 2, 0xE0B898, 0xE0E0E8, 0xA0A0B0, 0x4A3A2A);
        b.item("portrait", "Portrait", "note", 0xC0A060, "A portrait of Sir Vyvin, showing his sword.");
        b.item("blurite_sword", "Blurite sword", "longsword", 0x4A6AC0, "Sir Vyvin's replacement sword.");
        b.quest("the_knights_sword", "The Knight's Sword", Difficulty.INTERMEDIATE, 1)
                .req(Skill.MINING, 10).xp(Skill.SMITHING, 12725)
                .stage("I can start this quest by speaking to the Squire in the courtyard of the White Knights' Castle.")
                .talk("squire").say("I've lost Sir Vyvin's sword! It was made by the Imcando dwarves. Could you help me find a replacement?")
                .stage("I should ask Reldo, the librarian in Varrock Palace, about the Imcando dwarves.")
                .talk("reldo").say("The Imcando dwarves? Only one is left, Thurgo, who lives near Mudskipper Point. They say he loves redberry pie.")
                .stage("I should take a redberry pie to Thurgo, south of Port Sarim.")
                .talk("thurgo").take("redberry_pie").say("Redberry pie! You've won me over. I can make the sword, but I need a picture of it.")
                .missing("Go away. Unless you've got pie.")
                .stage("Thurgo needs a picture of the sword. Sir Vyvin keeps a portrait in his cupboard.")
                .talk("sir_vyvin").give("portrait").say("* While Sir Vyvin isn't looking, you take the portrait from his cupboard.")
                .stage("I should take the portrait to Thurgo.")
                .talk("thurgo").take("portrait").say("Aye, I can make that. Bring me two iron bars and blurite ore from the cave below Mudskipper Point.")
                .stage("Thurgo needs 2 iron bars and a blurite ore. Ice warriors guard the blurite in the Asgarnian Ice Dungeon.")
                .drop("ice_giant", "blurite_ore", 2)
                .talk("thurgo").take("iron_bar:2", "blurite_ore").give("blurite_sword").say("* Thurgo hammers out a gleaming blue sword.")
                .missing("I need two iron bars and a blurite ore.")
                .stage("I should give the blurite sword to the Squire.")
                .talk("squire").take("blurite_sword").say("A perfect replica! Sir Vyvin will never know. Thank you!")
                .done("thurgo", "Got any more pie?");
    }

    // ---------------------------------------------------------------- Shield of Arrav
    private static void shieldOfArrav(QuestBook b) {
        b.npc("baraek", "Baraek", "VARROCK", 7, 0, 0xE0B898, 0x6A4A2A, 0x4A3A2A, 0x8A6A3A);
        b.npc("straven", "Straven", "phoenix_hideout", 0, 0, 0xE8C0A0, 0xB03A1A, 0x2A2A2A, 0x1A1A1A);
        b.npc("katrina", "Katrina", "black_arm_hideout", 0, 0, 0xF0C8A8, 0x1A1A1A, 0x2A2A2A, 0x2A1A0A);
        b.npc("king_roald", "King Roald", "varrock_palace", 0, 0, 0xE8C0A0, 0x8A1A3A, 0x5A1A2A, 0xD0B030);
        b.npc("curator_haig_halen", "Curator Haig Halen", "varrock_museum", 0, 0, 0xE0C8B0, 0x3A5A3A, 0x2A3A2A, 0xB0B0B0);
        b.boss(BossDef.of("jonny_the_beard", "Jonny the beard").level(2).stats(8, 2, 2, 2, 1, 1).speed(4)
                .attack(AttackDef.melee(1)).drops(Drop.always("bones")).skin(0xE0B898, 0x5A3A2A, 0x3A2A1A, 0x2A1A0A));
        b.boss(BossDef.of("phoenix_weapon_guard", "Phoenix Gang guard").level(30).stats(30, 25, 25, 25, 1, 1).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 4, 2.5, 10)).drops(Drop.always("bones")).skin(0xD8B090, 0xB03A1A, 0x2A2A2A, 0x1A1A1A));
        b.item("intel_report", "Intel report", "note", 0xE0D8C0, "A report on the Black Arm Gang, taken from Jonny the beard.");
        b.item("weapon_store_key", "Weapon store key", "key", 0x8A8A8A, "The key to the Phoenix Gang's weapon store.");
        b.item("phoenix_crossbow", "Phoenix crossbow", "crossbow", 0x8A3A1A, "A crossbow stolen from the Phoenix Gang.");
        b.item("shield_left_half", "Broken shield (left)", "kiteshield", 0xB08A3A, "Half of the Shield of Arrav.");
        b.item("shield_right_half", "Broken shield (right)", "kiteshield", 0xB0903A, "Half of the Shield of Arrav.");
        b.item("arrav_certificate", "Certificate", "note", 0xF0E8C0, "A certificate of authenticity for the Shield of Arrav.");
        b.quest("shield_of_arrav", "Shield of Arrav", Difficulty.NOVICE, 1)
                .reward("coins:600")
                .stage("I can start this quest by speaking to Reldo in Varrock Palace library.")
                .talk("reldo").say("The Shield of Arrav was stolen from the museum years ago. Rumour says a gang in Varrock has it. Baraek the fur trader may know more.")
                .stage("I should ask Baraek in Varrock Square about the gangs.")
                .talk("baraek").take("coins:20").say("For 20 coins... The Phoenix Gang hides south-east of the square; ask for Straven. The Black Arm Gang hangs around the alley to the west; Katrina leads them.")
                .missing("Information costs 20 coins, friend.")
                .stage("I should join the Phoenix Gang by killing Jonny the beard of the Black Arm Gang (in the Blue Moon Inn) for his intel report.")
                .talk("straven").stay().unless("intel_report").spawn("jonny_the_beard")
                .say("Want to join the Phoenix Gang? Prove yourself. Jonny the beard of the Black Arm is drinking nearby; get his intel report.")
                .kill("jonny_the_beard").stay().unless("intel_report").give("intel_report")
                .talk("straven").take("intel_report").give("weapon_store_key", "shield_left_half")
                .say("Excellent. Welcome to the Phoenix Gang! Here's a key to our weapon store... and you may as well have this old shield half.")
                .stage("I have one half of the shield. Katrina of the Black Arm Gang wants two Phoenix crossbows from the Phoenix weapon store for the other half.")
                .talk("katrina").stay().unlessFlag("arrav_katrina").flag("arrav_katrina").spawn("phoenix_weapon_guard")
                .say("Bring me two crossbows from the Phoenix weapon store. Their guard won't make it easy.")
                .kill("phoenix_weapon_guard").stay().need("weapon_store_key").give("phoenix_crossbow:2")
                .say("* You unlock the weapon store and grab two crossbows.")
                .talk("katrina").take("phoenix_crossbow:2").give("shield_right_half")
                .say("Nice work. Here's the other half of the shield, it's no use to us.")
                .stage("I should take both halves of the shield to Curator Haig Halen at the Varrock Museum.")
                .talk("curator_haig_halen").take("shield_left_half", "shield_right_half").give("arrav_certificate")
                .say("The Shield of Arrav! Take this certificate to King Roald to claim the reward.")
                .stage("I should take the certificate to King Roald.")
                .talk("king_roald").take("arrav_certificate").say("The Shield of Arrav returned! Here is your reward of 600 coins.")
                .done("king_roald", "Varrock thanks you.");
    }

    // ---------------------------------------------------------------- X Marks the Spot
    private static void xMarksTheSpot(QuestBook b) {
        b.npc("veos", "Veos", "LUMBRIDGE", -7, 2, 0xE0C0A0, 0x6A3A1A, 0x3A2A1A, 0xA08040);
        b.npc("veos_port_sarim", "Veos", "PORT_SARIM", 2, 7, 0xE0C0A1, 0x6A3A1B, 0x3A2A1B, 0xA08041);
        b.item("treasure_scroll", "Treasure scroll", "note", 0xE8D8A8, "A treasure scroll with a riddle.");
        b.item("ancient_casket", "Ancient casket", "casket", 0x6A4A2A, "An ancient casket for Veos.");
        b.quest("x_marks_the_spot", "X Marks the Spot", Difficulty.NOVICE, 1)
                .xp(Skill.HITPOINTS, 300).reward("coins:200", "clue_scroll_beginner")
                .stage("I can start this quest by speaking to Veos in the Sheared Ram pub in Lumbridge.")
                .talk("veos").give("treasure_scroll").say("I'm looking for a treasure hunter. This scroll leads to an ancient casket.",
                        "The first clue: 'Dig outside the castle of the Duke, where his statue stands.'")
                .stage("I should dig outside Lumbridge Castle.")
                .reach("LUMBRIDGE", 10).say("* You dig and find another clue: 'Dig where the wizards tower over the swamp.'")
                .stage("I should dig near the Wizards' Tower.")
                .reach("WIZARDS_TOWER", 12).say("* Another clue: 'Dig where the willows grow, by the Draynor market.'")
                .stage("I should dig in Draynor Village.")
                .reach("DRAYNOR_VILLAGE", 12).say("* Another clue: 'Dig beside the pig pen... in Port Sarim, where Veos waits.'")
                .stage("I should dig in Port Sarim.")
                .reach("PORT_SARIM", 12).take("treasure_scroll").give("ancient_casket").say("* You dig up an ancient casket!")
                .stage("I should take the casket to Veos at the Port Sarim docks.")
                .talk("veos_port_sarim").take("ancient_casket").say("You found it! Here is your reward, and a little something for any future treasure hunts.")
                .done("veos_port_sarim", "Fancy a trip to Great Kourend some time?");
    }

    // ---------------------------------------------------------------- Learning the Ropes
    /** Tutorial Island as a quest (OSRS, 22 October 2025): 1 QP, no requirements, starter kit. Condensed. // approx */
    private static void learningTheRopes(QuestBook b) {
        b.spot("ltr_quest_guide_house", "Quest Guide's house", 3086, 3122);
        b.spot("ltr_magic_instructor", "Magic Instructor's hill", 3141, 3088);
        b.npc("ltr_quest_guide", "Quest Guide", "ltr_quest_guide_house", 0, 0, 0xE8C8A8, 0x5A3A8A, 0x3A2A5A, 0x6A4A2A);
        b.npc("ltr_magic_instructor", "Magic Instructor", "ltr_magic_instructor", 0, 0, 0xE0C0A0, 0x2A3A9A, 0x1A2A6A, 0x2A3A9A);
        b.quest("learning_the_ropes", "Learning the Ropes", Difficulty.NOVICE, 1)
                .reward("bronze_axe", "bronze_pickaxe", "tinderbox", "small_fishing_net", "shrimps", "bronze_dagger", "bronze_sword",
                        "wooden_shield", "shortbow", "bronze_arrow:25", "air_rune:25", "mind_rune:15", "water_rune:6", "earth_rune:4",
                        "body_rune:2", "bread", "coins:25")
                .unlock("Access to the mainland and the Lumbridge Home Teleport")
                .stage("I can start this quest by speaking to the Quest Guide on Tutorial Island.")
                .talk("ltr_quest_guide").say("Every adventurer should know how quests work. Your quest journal keeps track of them.",
                        "Head down to the mines and the combat area, then visit the Magic Instructor before you leave.")
                .stage("I should speak to the Magic Instructor before leaving Tutorial Island.")
                .talk("ltr_magic_instructor").say("You've learned the ropes. I'll send you to the mainland, to Lumbridge.",
                        "Take this kit with you. Good luck, adventurer!").travel("LUMBRIDGE")
                .done("ltr_magic_instructor", "Remember: the Lumbridge Home Teleport will always bring you back to Lumbridge.");
    }

    // ---------------------------------------------------------------- Below Ice Mountain
    private static void belowIceMountain(QuestBook b) {
        b.npc("willow_bim", "Willow", "dwarven_mine_north", 0, 0, 0xF0C8A8, 0x3A6A8A, 0x2A3A4A, 0xE0A040);
        b.npc("checkal", "Checkal", "LUMBRIDGE", 7, -2, 0xD8B890, 0x9A7A3A, 0x5A4A2A, 0x3A2A1A);
        b.npc("marley", "Marley", "VARROCK", -7, -3, 0xE8C8A0, 0x2A2A2A, 0x1A1A1A, 0x8A6A3A);
        b.npc("burntof", "Burntof", "FALADOR", 6, -6, 0xD0A070, 0x5A3A2A, 0x3A2A1A, 0xC07A2A);
        b.item("bim_bridge_plans", "Ancient plans", "note", 0xC0B080, "Plans showing a way into the ruins under Ice Mountain.");
        b.boss(BossDef.of("ancient_guardian", "Ancient Guardian").level(98).stats(85, 70, 75, 85, 1, 1).speed(5)
                .defenceBonus(60, 60, 20, 60, 120)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 12, 3, 8))
                .attack(AttackDef.of("quake", AttackType.CRUSH, 10, 6, 3).special(Special.AOE, 4)
                        .telegraph(2, "The Ancient Guardian raises its arms, the ground shakes!").particle("minecraft:block_crumble"))
                .drops(Drop.always("coins")).size(1.2f, 2.6f, 1.0f).at("camdozaal").arena(10, "stone"));
        b.quest("below_ice_mountain", "Below Ice Mountain", Difficulty.NOVICE, 1)
                .qp(16).reward("coins:2000")
                .unlock("Access to the Ruins of Camdozaal")
                .stage("I can start this quest by speaking to Willow at the northern entrance of the Dwarven Mine. (Requires 16 quest points.)")
                .talk("willow_bim").say("I'm putting together a crew to raid the ruins beneath Ice Mountain! I need a muscle, a face and a chef.",
                        "Checkal in Lumbridge is strong, Marley in Varrock is a charmer and Burntof in Falador... can sort of cook.")
                .stage("I need to recruit Checkal (Lumbridge), Marley (Varrock) and Burntof (Falador) for Willow's heist.")
                .talk("checkal").stay().unlessFlag("bim_checkal").flag("bim_checkal").say("Show me you're strong! *You flex. He seems impressed.* Fine, I'm in.")
                .talk("marley").stay().unlessFlag("bim_marley").flag("bim_marley").take("beer").say("A drink for me? Then I'm in, partner.")
                .talk("burntof").stay().unlessFlag("bim_burntof").flag("bim_burntof").take("shrimps")
                .say("You want a chef? Hand me some shrimp to cook... *It burns instantly.* Right, I'm in!")
                .talk("willow_bim").needFlag("bim_checkal", "bim_marley", "bim_burntof").give("bim_bridge_plans")
                .say("The whole crew! Let's go. The entrance is on Ice Mountain, past the old bridge.")
                .missing("I still need my muscle, my face and my chef.")
                .stage("I should enter the Ruins of Camdozaal with the crew and defeat the Ancient Guardian.")
                .kill("ancient_guardian").say("* The Ancient Guardian crumbles to rubble. The ruins of Camdozaal are open.")
                .stage("I should report back to Willow.")
                .talk("willow_bim").take("bim_bridge_plans").say("The ruins are ours! Here's your share of the take.");
    }

    // ---------------------------------------------------------------- Misthalin Mystery
    private static void misthalinMystery(QuestBook b) {
        b.npc("abigale", "Abigale", "lumbridge_swamp_dock", 0, 0, 0xF0D0B8, 0x3A3A6A, 0x2A2A4A, 0x2A1A0A);
        b.npc("lacey", "Lacey", "misthalin_mansion", 2, 2, 0xF0D8C0, 0x8A2A5A, 0x5A1A3A, 0xE0C060);
        b.npc("mandy", "Mandy", "misthalin_mansion", -2, 2, 0xD8B090, 0x2A6A2A, 0x1A3A1A, 0x8A4A1A);
        b.npc("kingston", "Kingston", "misthalin_mansion", 0, -2, 0xE8C0A0, 0x1A1A1A, 0x1A1A1A, 0xA0A0A0);
        b.item("mm_notes", "Notes", "note", 0xE0D8C8, "Scribbled notes from the mansion.");
        b.boss(BossDef.of("misthalin_murderer", "Murderer").level(25).stats(30, 20, 20, 20, 1, 1).speed(4)
                .attack(AttackDef.of("knife", AttackType.STAB, 4, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0xE8C0A0, 0x1A1A1A, 0x1A1A1A, 0x6A1A1A));
        b.quest("misthalin_mystery", "Misthalin Mystery", Difficulty.NOVICE, 1)
                .xp(Skill.CRAFTING, 600).reward("coins:500", "uncut_sapphire", "uncut_emerald", "uncut_ruby")
                .stage("I can start this quest by speaking to Abigale on the jetty in Lumbridge Swamp.")
                .talk("abigale").travel("misthalin_mansion").say("A guest has gone missing from Lacey's mansion on the island. Will you help investigate?",
                        "* Abigale rows you across to the mansion.")
                .stage("I should question the guests of the mansion: Lacey, Mandy and Kingston.")
                .talk("lacey").stay().unlessFlag("mm_lacey").flag("mm_lacey").say("I was in the library all evening, I swear!")
                .talk("mandy").stay().unlessFlag("mm_mandy").flag("mm_mandy").say("I heard footsteps on the stairs, then a scream.")
                .talk("kingston").needFlag("mm_lacey", "mm_mandy").give("mm_notes").say("These notes... they're written in a hand I recognise. The killer is among us!",
                        "* The 'murderer' lunges at you from the shadows!").spawn("misthalin_murderer")
                .missing("Have you spoken to the others yet?")
                .stage("I must survive the murderer's attack.")
                .kill("misthalin_murderer").say("* The murderer falls. The mystery is solved.")
                .stage("I should report back to Abigale.")
                .talk("abigale").take("mm_notes").travel("lumbridge_swamp_dock").say("You solved it! Let me row you back. Please take these as thanks.");
    }

    // ---------------------------------------------------------------- The Corsair Curse
    private static void corsairCurse(QuestBook b) {
        b.npc("captain_tock", "Captain Tock", "corsair_cove", 0, 2, 0xD8A880, 0x6A1A1A, 0x2A2A2A, 0x1A1A1A);
        b.npc("cabin_boy_colin", "Cabin boy Colin", "corsair_cove", 2, -2, 0xF0D0B0, 0xD0D0C0, 0x3A3A5A, 0x6A4A2A);
        b.npc("gnocci_the_cook", "Gnocci the Cook", "corsair_cove", -2, -2, 0xE8C8A8, 0xF0F0F0, 0x8A8A8A, 0xF0F0F0);
        b.npc("arsen_the_thief", "Arsen the Thief", "corsair_cove", -3, 2, 0xC8A070, 0x3A3A2A, 0x2A2A1A, 0x5A5A3A);
        b.item("ogre_artefact", "Ogre artefact", "skull", 0x7A6A4A, "An ancient ogre artefact stolen by the Corsairs.");
        b.boss(BossDef.of("ithoi_the_navigator", "Ithoi the Navigator").level(35).stats(40, 30, 30, 30, 30, 1).speed(4)
                .attack(AttackDef.of("wind_strike", AttackType.MAGIC, 6, 8, 6).particle("minecraft:cloud"))
                .attack(AttackDef.of("punch", AttackType.CRUSH, 4, 2.5, 4))
                .drops(Drop.always("bones")).skin(0xB8D0D0, 0x2A4A6A, 0x1A2A3A, 0x5A5A5A).at("ithoi_hut").arena(8, "sand"));
        b.quest("the_corsair_curse", "The Corsair Curse", Difficulty.INTERMEDIATE, 2)
                .reward("coins:500")
                .unlock("Access to the Corsair Cove Dungeon")
                .stage("I can start this quest by speaking to Captain Tock in Corsair Cove (reached via Captain Tock's ship in Port Sarim).")
                .talk("captain_tock").say("My crew claims we're cursed. An ogre artefact we... found... is to blame, they say. Question my crew.")
                .stage("I should question Cabin boy Colin, Gnocci the Cook and Arsen the Thief.")
                .talk("cabin_boy_colin").stay().unlessFlag("cc_colin").flag("cc_colin").say("Ithoi the navigator keeps to his hut since he got sick. Strange, that.")
                .talk("gnocci_the_cook").stay().unlessFlag("cc_gnocci").flag("cc_gnocci").say("Ithoi always complained about my cooking. Then he got sick. Not my fault!")
                .talk("arsen_the_thief").needFlag("cc_colin", "cc_gnocci").give("ogre_artefact")
                .say("Alright, I took the artefact from the ogres. Here, take it back to them. But it's Ithoi you want to look at, he's faking!")
                .stage("Ithoi is faking the curse. I should confront Ithoi the Navigator in his hut.")
                .kill("ithoi_the_navigator").say("* Ithoi's ghostly disguise falls away. He staged the curse to take the ship for himself!")
                .stage("I should report back to Captain Tock.")
                .talk("captain_tock").take("ogre_artefact").say("Ithoi! I knew he was trouble. The curse is lifted. Thank you, matey.");
    }
}
