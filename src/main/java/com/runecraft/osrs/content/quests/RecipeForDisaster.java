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
 * Recipe for Disaster: Another Cook's Quest, eight "Freeing..." subquests (each a flagged side
 * objective with its own XP reward and glove unlock) and the Culinaromancer's six-boss finale.
 * The Culinaromancer's chest (sold by the Gypsy) offers the best gloves unlocked so far.
 */
final class RecipeForDisaster {
    /** Subquest flag -> glove unlocked by it (approximation of OSRS's "N subquests done" unlocks). */
    static final String[][] GLOVE_UNLOCKS = {
            {"rfd_dwarf", "rfd_bronze_gloves"}, {"rfd_goblins", "rfd_iron_gloves"}, {"rfd_pete", "rfd_steel_gloves"},
            {"rfd_guide", "rfd_black_gloves"}, {"rfd_dave", "rfd_mithril_gloves"}, {"rfd_skrach", "rfd_adamant_gloves"},
            {"rfd_amik", "rfd_rune_gloves"}, {"rfd_awowogei", "rfd_dragon_gloves"},
    };

    private RecipeForDisaster() {
    }

    static void register(QuestBook b) {
        b.place("rfd_lumbridge_cellar", "Lumbridge Castle cellar", 3190, 3240, PlaceDef.Style.CAVE);
        b.spot("feldip_hills", "Feldip Hills", 2630, 2980);
        b.npc("rfd_gypsy", "Gypsy", "LUMBRIDGE", -7, -2, 0xE8C0B0, 0x8A2A6A, 0x5A1A4A, 0xE0A042);
        b.npc("rohak", "Rohak", "ice_mountain", 3, 2, 0xD8A884, 0x6A3A2A, 0x3A2A1C, 0xC07A2B);
        b.npc("pirate_pete", "Pirate Pete", "PORT_SARIM", 4, 7, 0xD8A885, 0x2A2A6A, 0x2A2A2B, 0x8A1A1B);
        b.npc("lumbridge_guide", "Lumbridge Guide", "LUMBRIDGE", 3, 7, 0xE8C0B1, 0x3A6A3A, 0x2A4A2B, 0xE0E0E4);
        b.npc("evil_dave", "Evil Dave", "EDGEVILLE", 6, -6, 0xE0B8A3, 0x1A1A1A, 0x2A2A2D, 0x2A1A0F);
        b.npc("skrach_uglogwee", "Skrach Uglogwee", "feldip_hills", 0, 0, 0x8A9A5A, 0x6A4A2A, 0x4A3A1A, 0x5A6A3A);

        b.boss(BossDef.of("agrith_na_na", "Agrith-Na-Na").level(146).stats(200, 120, 120, 100, 100, 1).speed(4)
                .attack(AttackDef.of("claws", AttackType.SLASH, 20, 3, 6))
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 18, 8, 4).particle("minecraft:flame"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("rfd_lumbridge_cellar").arena(9, "crypt"));
        b.boss(BossDef.of("flambeed", "Flambeed").level(149).stats(210, 120, 120, 100, 1, 1).speed(4)
                .attack(AttackDef.of("flaming_fists", AttackType.CRUSH, 20, 2.5, 10).particle("minecraft:flame"))
                .drops(Drop.always("bones")).skin(0xE88A4A, 0xF0F0F0, 0xE04A1A, 0xFFB040).size(0.9f, 2.4f, 1.2f)
                .at("rfd_lumbridge_cellar").arena(9, "lava"));
        b.boss(BossDef.of("karamel", "Karamel").level(136).stats(250, 100, 100, 100, 120, 1).speed(4)
                .attack(AttackDef.of("ice_spell", AttackType.MAGIC, 20, 8, 6).special(Special.DRAIN_STAT, Skill.ATTACK.ordinal()).particle("minecraft:snowflake"))
                .attack(AttackDef.of("punch", AttackType.CRUSH, 15, 2.5, 4))
                .drops(Drop.always("bones")).skin(0xE0C0A0, 0xA06A3A, 0x6A3A1A, 0xF0E0C0).at("rfd_lumbridge_cellar").arena(9, "ice"));
        b.boss(BossDef.of("dessourt", "Dessourt").level(121).stats(130, 100, 100, 100, 100, 1).speed(4)
                .attack(AttackDef.of("ghostly_blast", AttackType.MAGIC, 23, 8, 6).particle("minecraft:soul"))
                .attack(AttackDef.of("claw", AttackType.SLASH, 18, 2.5, 4))
                .drops(Drop.always("bones")).skin(0xC8E8E6, 0x6A8A8A, 0x4A6A6A, 0xA0C0C0).at("rfd_lumbridge_cellar").arena(9, "crypt"));
        b.boss(BossDef.of("gelatinnoth_mother", "Gelatinnoth Mother").level(130).stats(240, 100, 100, 100, 1, 1).speed(4)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 17, 2.5, 10))
                .drops(Drop.always("bones")).size(1.2f, 2.6f, 1.0f).at("rfd_lumbridge_cellar").arena(9, "water"));
        b.boss(BossDef.of("culinaromancer", "Culinaromancer").level(75).stats(150, 75, 75, 75, 75, 1).speed(4)
                .attack(AttackDef.of("rolling_pin", AttackType.CRUSH, 20, 2.5, 8))
                .attack(AttackDef.of("food_magic", AttackType.MAGIC, 16, 8, 3).particle("minecraft:happy_villager"))
                .phase(50, "The Culinaromancer: You can't beat me, I'm the greatest chef in Gielinor!", 1.2, false)
                .drops(Drop.always("bones")).skin(0xE8C0B2, 0xF0F0F2, 0x5A1A6A, 0xF0F0F3).at("rfd_lumbridge_cellar").arena(9, "stone"));

        b.item("rfd_rock_cake", "Dwarven rock cake", "cake", 0x8A7A6A, "A cake made of... rock? Rohak loves them.");
        // Subquest ingredients (OSRS Wiki quick guides). Shapes/colours are approximations.
        b.item("rfd_fishbowl_helmet", "Fishbowl helmet", "full_helm", 0xB8E0F0, "A fishbowl with bronze wire: Murphy says it'll let you breathe underwater.");
        b.item("rfd_mudskipper_hide", "Mudskipper hide", "hide", 0x6A7A4A, "Nung the fishman wants these for his crab pen.");
        b.item("rfd_ground_crab_meat", "Ground giant crab meat", "spice", 0xE0A080, "Giant crab meat, ground with a pestle and mortar.");
        b.item("rfd_ground_kelp", "Ground kelp", "spice", 0x3A6A3A, "Kelp from the sea bed, ground with a pestle and mortar.");
        b.item("rfd_enchanted_egg", "Enchanted egg", "bait", 0xF0E8D0, "Wizard Traiborn enchanted this egg.");
        b.item("rfd_enchanted_milk", "Enchanted milk", "vial_water", 0xF0F0FF, "Wizard Traiborn enchanted this milk.");
        b.item("rfd_enchanted_flour", "Enchanted flour", "pot_item", 0xF0F0E0, "Wizard Traiborn enchanted this flour.");
        b.item("rfd_hell_rat_spices", "Hell-rat spices", "spice", 0xC06A1A, "Yellow, orange, red and brown spices your cat chased out of the hell-rats.");
        b.item("rfd_raw_jubbly", "Raw jubbly", "meat_raw", 0xE09090, "A plucked jubbly bird. Skrach wants it roasted on a spit.");
        b.item("rfd_evil_chicken_egg", "Evil chicken's egg", "bait", 0x8A1A1A, "It's warm. And it's watching you.");
        b.item("rfd_dragon_token", "Dragon token", "coins", 0x2A2A2A, "Rub it and a black dragon flambes your brulee.");
        b.item("rfd_vanilla_pod", "Vanilla pod", "produce", 0xE8D8A0, "Picked from a vanilla plant in the Kharazi Jungle.");
        b.item("rfd_snake_corpse", "Snake corpse", "meat_raw", 0x5A7A3A, "A big snake from the pit on Crash Island.");
        b.item("rfd_red_banana", "Red banana", "produce", 0xC03A2A, "A red banana from a tree on Ape Atoll, sliced.");
        b.item("rfd_tchiki_nuts", "Tchiki monkey nuts", "seed", 0x8A6A3A, "Ground tchiki monkey nuts from the Ape Atoll agility course.");
        b.spot("port_khazard_rfd", "Port Khazard docks", 2674, 3160);
        b.spot("rfd_crab_pen", "Mogre camp crab pen (under the sea)", 2992, 3114); // approx: the Mudskipper Point dive
        b.spot("rfd_evil_daves_basement", "Evil Dave's basement", 3080, 3493);
        b.spot("rfd_evil_chicken_lair", "The Evil Chicken's lair (Zanaris)", 3176, 3150); // approx: Zanaris (2453, 4476 in OSRS) is placed beside its swamp shed
        b.spot("rfd_crash_island_pit", "Crash Island snake pit", 2896, 2722); // approx
        Cast.npc(b, "rfd_traiborn", "Wizard Traiborn", "WIZARDS_TOWER", -5, 4, Cast.PALE, 0x5A3A8A);
        Cast.npc(b, "rfd_murphy", "Murphy", "port_khazard_rfd", 0, 0, Cast.FAIR, 0x3A5A7A);
        // OSRS Wiki "Evil Chicken (Recipe for Disaster)": a fixed level 159 (only the random-event chicken scales):
        // 120 HP, 126 Defence, 200 Magic, magic attacks up to 21 every 4 ticks; summoned with a raw chicken at its shrine.
        b.boss(BossDef.of("rfd_evil_chicken", "Evil Chicken").level(159).stats(120, 1, 1, 126, 200, 1).speed(4)
                .attack(AttackDef.of("egg_bomb", AttackType.MAGIC, 21, 8, 1).particle("minecraft:flame"))
                .drops(Drop.always("bones"), Drop.always("minecraft:chicken")).skin(0x8A1A1A, 0x6A1A1A, 0x4A0A0A, 0xE0A020).size(0.7f, 1.2f, 1.0f)
                .at("rfd_evil_chicken_lair").arena(7, "stone"));
        // Subquest creatures (OSRS Wiki infoboxes), each raised at its spot while its objective is active.
        b.spot("rfd_mogre_crab_pen", "Mogre Camp crab pen (sea bed)", 2962, 3152); // approx: the undersea pen, offshore west of Mudskipper Point
        b.spot("rfd_jubbly_nest", "Jubbly bird nesting ground (Feldip Hills)", 2636, 2964); // approx: south-east of Rantz, by the coast
        b.boss(BossDef.of("rfd_crab", "Crab").level(21).stats(18, 17, 17, 22, 1, 1).speed(4) // the Mogre Camp crab pen crab
                .attack(AttackDef.of("pinch", AttackType.CRUSH, 3, 2, 1))
                .drops(Drop.always("bones")).skin(0xC05A2A, 0xA04A1A, 0x6A2A0A, 0xE0A060).size(0.9f, 0.8f, 1.0f)
                .at("rfd_mogre_crab_pen").arena(6, "water"));
        b.boss(BossDef.of("rfd_hell_rat", "Hell-Rat").level(3).stats(5, 2, 2, 2, 1, 1).speed(4) // approx: OSRS hell-rats have no combat stats; only a cat can catch them
                .attack(AttackDef.of("bite", AttackType.STAB, 1, 1.5, 1))
                .drops(Drop.always("ashes")).model("giant_rat").size(0.6f, 0.5f, 0.8f)
                .at("rfd_evil_daves_basement").arena(6, "crypt"));
        b.boss(BossDef.of("rfd_jubbly_bird", "Jubbly bird").level(9).stats(20, 5, 5, 6, 1, 1).speed(4).passive() // does not attack in OSRS
                .attack(AttackDef.of("flap", AttackType.CRUSH, 0, 1.5, 1))
                .drops(Drop.always("bones")).model("chompy_bird").size(0.8f, 1.2f, 1.0f)
                .at("rfd_jubbly_nest").arena(6, "jungle"));
        b.boss(BossDef.of("rfd_big_snake", "Big Snake").level(84).stats(120, 60, 60, 60, 1, 1).speed(6).poisonous()
                .attack(AttackDef.of("bite", AttackType.STAB, 14, 2, 1))
                .drops(Drop.always("bones")).model("sea_snake").size(1.0f, 0.8f, 1.0f)
                .at("rfd_crash_island_pit").arena(7, "jungle"));

        b.quest("recipe_for_disaster", "Recipe for Disaster", Difficulty.GRANDMASTER, 10)
                .reqQuest("COOKS_ASSISTANT").req(Skill.COOKING, 10)
                .xp(Skill.HERBLORE, 20000) // approx: OSRS gives a 20,000 XP lamp (any skill above 50) for defeating the Culinaromancer
                .unlock("The Culinaromancer's chest: food and RFD gloves, up to Barrows gloves")
                .stage("I can start this quest by speaking to the Gypsy in Lumbridge Castle's dining room. (Requires Cook's Assistant, 10 Cooking.)")
                .talk("rfd_gypsy").say("The Duke is holding a banquet for the Secret Council of Gielinor, and the Cook is in a terrible state. He needs ingredients!")
                // OSRS Another Cook's Quest: the Cook's "dirty blast" needs an eye of newt, Greenman's ale, a rotten tomato, a fruit blast and ashes.
                .stage("Another Cook's Quest: the Cook needs an eye of newt, a Greenman's ale, a rotten tomato, a fruit blast and some ashes.")
                .talk("rfd_gypsy").take("eye_of_newt", "greenmans_ale", "rotten_tomato", "fruit_blast", "ashes").flag("rfd_cook") // Another Cook's Quest: no XP in OSRS
                .say("* The Cook mixes his dirty blast and the banquet begins... and a sinister chef appears in a puff of smoke! It's the Culinaromancer!",
                        "@Culinaromancer: Fools! You'll never taste my cooking again! *He freezes the council in time.*",
                        "I have slowed time to stop him. Free each council member by cooking the dish that reminds them of who they are!")
                .missing("The Cook needs an eye of newt, a Greenman's ale, a rotten tomato, a fruit blast and some ashes for his dirty blast.")
                .stage("I must free the eight frozen council members by bringing each the dish that reminds them of who they are.")
                // --- Mountain Dwarf: get Rohak drunk on four Asgarnian ales for his gran's recipe, bake a rock cake (gloves: it's hot).
                .talk("rohak").stay().needFlag("rfd_dwarf_ale").unlessFlag("rfd_dwarf").take("rfd_rock_cake").flag("rfd_dwarf")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.SLAYER, 1000)
                .say("A dwarven rock cake, just like gran used to make! *The Mountain Dwarf is freed from the spell.*")
                .talk("rohak").stay().needFlag("rfd_dwarf_ale").unlessFlag("rfd_dwarf").unless("rfd_rock_cake")
                .take("minecraft:milk_bucket", "minecraft:egg", "pot_of_flour", "bowl_of_water").need("leather_gloves").give("rfd_rock_cake")
                .say("* You mix the batter as Rohak slurs out the recipe and bake it. Your gloves save your fingers from the red-hot cake.")
                .missing("Gran's rock cake needs a bucket of milk, an egg, a pot of flour and a bowl of water. And gloves: it comes out very hot.")
                .talk("rohak").stay().unlessFlag("rfd_dwarf_ale", "rfd_dwarf").reqDone("fishing_contest").take("asgarnian_ale:4").flag("rfd_dwarf_ale")
                .say("*hic* Four ales! Shure, I'll tell you gran's rock cake recipe... milk, egg, flour and water, *hic*, baked till it glowsh.")
                .missing("A dwarf can't talk recipes sober. Four Asgarnian ales should loosen my tongue. (Requires Fishing Contest.)")
                // --- Goblin generals: slop of compromise.
                .talk("general_bentnoze").stay().unlessFlag("rfd_goblins").reqDone("goblin_diplomacy")
                .take("bread", "orange", "spice", "fishing_bait", "minecraft:water_bucket", "charcoal").need("knife").flag("rfd_goblins")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.FARMING, 1000).grantXp(Skill.CRAFTING, 1000)
                .say("* The goblin cook soaks the bread in water and adds orange slices, spice, bait and charcoal for colour...",
                        "Slop of compromise! *The goblin generals stop arguing long enough to be freed.*")
                .missing("Goblins want slop! Bread soaked in water, orange slices (you'll need a knife), spice, fishing bait and charcoal!")
                // --- Pirate Pete: fishcake of ground cod, breadcrumbs, ground kelp and ground giant crab meat from under the sea.
                .talk("pirate_pete").stay().needFlag("rfd_pete_dive").unlessFlag("rfd_pete").reqSkill(Skill.COOKING, 31)
                .take("raw_cod", "bread", "rfd_ground_kelp", "rfd_ground_crab_meat").need("pestle_and_mortar", "knife").flag("rfd_pete")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.FISHING, 1000).grantXp(Skill.CRAFTING, 1000).grantXp(Skill.SMITHING, 1000)
                .say("* You grind the cod, crumb the bread, mix in the kelp and crab meat and fry the fishcake.",
                        "Fishcake! Arr, that's what I was missin'. *Pirate Pete is freed.*")
                .missing("Arr, a fishcake: ground cod, breadcrumbs, ground kelp and ground giant crab meat. Murphy in Port Khazard can take ye diving.")
                .talk("pirate_pete").stay().unlessFlag("rfd_pete_dive", "rfd_pete").reqSkill(Skill.COOKING, 31).flag("rfd_pete_dive")
                .say("The Cook says a fishcake's what I need. Kelp and giant crab live on the sea bed: Murphy in Port Khazard knows how to get down there.")
                .missing("Arr, I'd tell ye me favourite dish, but ye don't know the first thing about cookin'. (Requires 31 Cooking.)")
                .talk("rfd_murphy").stay().needFlag("rfd_pete_dive").unless("rfd_fishbowl_helmet").unlessFlag("rfd_pete")
                .take("bronze_bar").need("hammer", "needle").give("rfd_fishbowl_helmet")
                .say("Divin'? Ye'll need a fishbowl helmet. *You hammer the bar into wire and fit it to a fishbowl.* Now get down there!")
                .missing("Bring a bronze bar for wire, a hammer and a needle and I'll make ye a fishbowl helmet for the dive.")
                // OSRS: kill 5 mudskippers for 5 hides; Nung the fishman opens his crab pen for them; kill a crab for its meat.
                .drop("mudskipper", "rfd_mudskipper_hide", 1).times(5).needFlag("rfd_pete_dive").unlessFlag("rfd_pete_pen", "rfd_pete")
                .reach("rfd_crab_pen", 8).stay().needFlag("rfd_pete_dive").need("rfd_fishbowl_helmet", "pestle_and_mortar").take("rfd_mudskipper_hide:5")
                .unlessFlag("rfd_pete_pen", "rfd_pete").flag("rfd_pete_pen").give("rfd_ground_kelp")
                .say("* You pick kelp on the sea bed and grind it. Nung the fishman takes your five mudskipper hides and opens his crab pen.")
                .missing("* Nung the fishman won't open his crab pen without 5 mudskipper hides, and you'll need a fishbowl helmet to breathe down here.")
                .kill("rfd_crab").stay().needFlag("rfd_pete_pen").need("rfd_fishbowl_helmet", "pestle_and_mortar").unless("rfd_ground_crab_meat")
                .unlessFlag("rfd_pete").give("rfd_ground_crab_meat")
                .say("* The crab falls. You grind its giant crab meat with your pestle and mortar.")
                // --- Lumbridge Guide: cake of guidance from Wizard Traiborn's enchanted egg, milk and flour.
                .talk("rfd_traiborn").stay().unless("rfd_enchanted_egg").unlessFlag("rfd_guide").reqSkill(Skill.COOKING, 40)
                .reqDone("big_chompy_bird_hunting", "biohazard", "DEMON_SLAYER", "nature_spirit", "witchs_house", "murder_mystery")
                .take("minecraft:egg", "minecraft:milk_bucket", "pot_of_flour").give("rfd_enchanted_egg", "rfd_enchanted_milk", "rfd_enchanted_flour")
                .say("Who has a ponytail and a book? > Reldo. Ingredients in a cake? > Flour, egg and milk. Combat level of a Lumbridge goblin? > Two.",
                        "Splendid! *Traiborn enchants your egg, milk and flour.*")
                .missing("Enchant cake ingredients for the Guide? Bring me an egg, a bucket of milk and a pot of flour... and know your trivia. "
                        + "(Requires 40 Cooking, Big Chompy Bird Hunting, Biohazard, Demon Slayer, Murder Mystery, Nature Spirit and Witch's House.)")
                .talk("lumbridge_guide").stay().unlessFlag("rfd_guide").take("rfd_enchanted_egg", "rfd_enchanted_milk", "rfd_enchanted_flour", "cake_tin")
                .flag("rfd_guide").grantXp(Skill.COOKING, 2500).grantXp(Skill.MAGIC, 2500)
                .say("* You bake the enchanted ingredients in the cake tin: a cake of guidance!", "Just like the cakes I give to new adventurers. *The Lumbridge Guide is freed.*")
                .missing("A cake of guidance... Wizard Traiborn can enchant the ingredients. Bring them with a cake tin.")
                // --- Evil Dave: spicy stew with hell-rat spices caught by a cat in his basement.
                .talk("evil_dave").stay().needFlag("rfd_dave_basement").unlessFlag("rfd_dave").take("stew", "rfd_hell_rat_spices").flag("rfd_dave")
                .grantXp(Skill.COOKING, 7000)
                .say("* You stir the four colours of spice into the stew, a dose at a time, until Dave stops complaining.",
                        "Spicy stew! The darkness... it's so spicy... *Evil Dave is freed. He seems even more evil.*")
                .missing("Spicy stew. Proper spicy. The hell-rats in my basement have the spices; send a cat after them.")
                .talk("evil_dave").stay().unlessFlag("rfd_dave_basement", "rfd_dave").reqSkill(Skill.COOKING, 25).reqDone("gertrudes_cat", "shadow_of_the_storm")
                .flag("rfd_dave_basement")
                .say("What did I eat at the council? Why should I... oh, the magic requires it. Spicy stew. Mother keeps hell-rats in the basement for the spices.")
                .missing("Go away. (Requires 25 Cooking, Gertrude's Cat and Shadow of the Storm.)")
                // OSRS: the cat catches hell-rats, each dropping a random spice; four colours are needed (one catch each here).
                .kill("rfd_hell_rat", 4).stay().needFlag("rfd_dave_basement").need("gertrudes_kitten").unless("rfd_hell_rat_spices").unlessFlag("rfd_dave")
                .give("rfd_hell_rat_spices").say("* Your cat has caught four hell-rats. Between them they dropped yellow, orange, red and brown spices.")
                .missing("* Hell-rats scurry about Evil Dave's basement. Bring a cat to chase them.")
                // --- Skrach Uglogwee: a jubbly bird shot with an ogre bow, roasted on an iron spit.
                .talk("skrach_uglogwee").stay().needFlag("rfd_skrach_start").unlessFlag("rfd_skrach").take("rfd_raw_jubbly", "iron_bar", "logs")
                .need("tinderbox").flag("rfd_skrach").grantXp(Skill.COOKING, 1500).grantXp(Skill.RANGED, 1500).grantXp(Skill.CRAFTING, 1500)
                .say("* You hammer the iron bar into a spit and roast the jubbly over a fire.", "Jubbly! Roasted jubbly! *Skrach Uglogwee is freed.*")
                .missing("Skrach want roast jubbly! Cook on iron spit over logs. You need tinderbox.")
                .talk("skrach_uglogwee").stay().unlessFlag("rfd_skrach_start", "rfd_skrach").reqSkill(Skill.COOKING, 41).reqSkill(Skill.FIREMAKING, 20)
                .reqDone("big_chompy_bird_hunting").flag("rfd_skrach_start")
                .say("Skrach want jubbly bird! Rantz know jubbly: they live in Feldip Hills. Blow up toad for bait, then shoot with ogre bow.")
                .missing("Skrach no talk to weakling. (Requires 41 Cooking, 20 Firemaking and Big Chompy Bird Hunting.)")
                .reach("feldip_hills", 12).stay().needFlag("rfd_skrach_start").unless("rfd_raw_jubbly").unlessFlag("rfd_skrach", "rfd_jubbly_bait")
                .needTool("axe", "pickaxe").flag("rfd_jubbly_bait")
                .say("* You chop logs and mine a rock to help Rantz fix his boat. He shows you how to inflate a toad as jubbly bait.")
                .missing("* Rantz needs his boat fixed before he'll show you the jubblies: bring an axe and a pickaxe.")
                .kill("rfd_jubbly_bird").stay().needFlag("rfd_jubbly_bait").need("ogre_bow").take("ogre_arrow").unless("rfd_raw_jubbly").unlessFlag("rfd_skrach")
                .give("rfd_raw_jubbly").say("* The jubbly bird lands on your bloated toad and you shoot it with an ogre arrow. You pluck the jubbly.")
                .missing("* You'll need an ogre bow and ogre arrows to shoot the jubbly.")
                // --- Sir Amik Varze: brulee supreme with the Evil Chicken's egg, flambeed by a black dragon's token.
                .talk("sir_amik_varze").stay().needFlag("rfd_amik_start").unlessFlag("rfd_amik")
                .take("minecraft:milk_bucket", "pot_of_cream", "sweetcorn", "rfd_vanilla_pod", "rfd_evil_chicken_egg", "dramen_branch", "rfd_dragon_token")
                .need("pestle_and_mortar").flag("rfd_amik").grantXp(Skill.COOKING, 4000).grantXp(Skill.HITPOINTS, 4000)
                .say("* Milk and cream, cornflour, vanilla, the Evil Chicken's egg and cinnamon ground from a dramen branch. You rub the token: a dragon flambes it.",
                        "Brulee supreme! *Sir Amik Varze is freed.*")
                .missing("A brulee supreme: milk, cream, cornflour (sweetcorn), a vanilla pod, cinnamon from a dramen branch, the Evil Chicken's egg and a black dragon's token.")
                // OSRS: Lost City and Legends' Quest started; 107 QP (the Legends' start requirement) stands in for "started". // approx
                .talk("sir_amik_varze").stay().unlessFlag("rfd_amik_start", "rfd_amik").reqDone("lost_city").reqQp(107).flag("rfd_amik_start")
                .say("The Wise Old Man says my favourite was a brulee supreme. The egg must come from the Evil Chicken itself, in Zanaris.")
                .missing("You'll need access to Zanaris and the Kharazi Jungle. (Requires Lost City and Legends' Quest started: 107 quest points.)")
                .reach("kharazi_jungle", 12).stay().needFlag("rfd_amik_start").unless("rfd_vanilla_pod").unlessFlag("rfd_amik").needTool("axe")
                .give("rfd_vanilla_pod").say("* You hack through the jungle and pick a vanilla pod.")
                .kill("rfd_evil_chicken").stay().needFlag("rfd_amik_start").take("minecraft:chicken").unless("rfd_evil_chicken_egg").unlessFlag("rfd_amik")
                .give("rfd_evil_chicken_egg").say("* You offered a raw chicken at the shrine and its master came. The Evil Chicken drops a warm egg.")
                .missing("* The Evil Chicken only answers an offering of a raw chicken at its shrine.")
                .drop("black_dragon", "rfd_dragon_token", 1)
                // --- King Awowogei: stuffed snake with red banana and tchiki nuts, cooked on a hot rock under the temple.
                .talk("king_awowogei").stay().needFlag("rfd_awo_start").unlessFlag("rfd_awowogei").reqSkill(Skill.COOKING, 70)
                .take("rfd_snake_corpse", "rfd_red_banana", "rfd_tchiki_nuts").need("monkey_greegree").flag("rfd_awowogei")
                .grantXp(Skill.COOKING, 10000).grantXp(Skill.AGILITY, 10000)
                .say("* You stuff the snake and cook it on a hot rock beneath the Temple of Marimbo.",
                        "Ook! Stuffed snake! *King Awowogei is freed, and allows a monkey to join the council.*")
                .missing("Ook! The king wants stuffed snake: a big snake from Crash Island, a red banana and ground tchiki nuts.")
                .talk("king_awowogei").stay().unlessFlag("rfd_awo_start", "rfd_awowogei").reqDone("monkey_madness_i").reqSkill(Skill.COOKING, 70)
                .reqSkill(Skill.AGILITY, 48).need("monkey_greegree").flag("rfd_awo_start")
                .say("Ook? The king's favourite dish is stuffed snake. Big snakes live in the pit on Crash Island.")
                .missing("Ook ook! (Wear a monkey greegree. Requires Monkey Madness I, 70 Cooking, 48 Agility.)")
                .reach("rfd_crash_island_pit", 8).stay().needFlag("rfd_awo_start").unless("rfd_snake_corpse").unlessFlag("rfd_awowogei")
                .need("rope").unlessFlag("rfd_awo_pit").flag("rfd_awo_pit").say("* You tie your rope and climb down into the snake pit.")
                .missing("* The pit is too deep to climb down without a rope.")
                .kill("rfd_big_snake").stay().needFlag("rfd_awo_pit").unless("rfd_snake_corpse").unlessFlag("rfd_awowogei")
                .give("rfd_snake_corpse").say("* The big snake falls. You take its corpse.")
                .reach("ape_atoll", 16).stay().needFlag("rfd_awo_start").unless("rfd_tchiki_nuts").unlessFlag("rfd_awowogei")
                .need("rope", "knife", "pestle_and_mortar").reqSkill(Skill.AGILITY, 48).give("rfd_red_banana", "rfd_tchiki_nuts")
                .say("* As a gorilla you rope down a red banana; as a ninja monkey you run the agility course to the tchiki nut bush.",
                        "* You slice the banana and grind the nuts.")
                .missing("* You'll need a rope, a knife and a pestle and mortar to gather the stuffing.")
                .talk("rfd_gypsy").needFlag("rfd_dwarf", "rfd_goblins", "rfd_pete", "rfd_guide", "rfd_dave", "rfd_skrach", "rfd_amik", "rfd_awowogei")
                .reqQp(175).reqDone("desert_treasure_i", "horror_from_the_deep")
                .say("Every council member is free! Now the Culinaromancer himself must be defeated. His minions wait in the cellar.")
                .missing("Free all eight council members first. To face the Culinaromancer you will also need 175 quest points, Desert Treasure I and Horror from the Deep.")
                .stage("I must defeat the Culinaromancer's minions in order, then the Culinaromancer himself, in the Lumbridge Castle cellar.")
                .kill("agrith_na_na").stay().unlessFlag("rfd_agrith").flag("rfd_agrith").say("* Agrith-Na-Na is defeated! Next: Flambeed.")
                .kill("flambeed").stay().needFlag("rfd_agrith").unlessFlag("rfd_flambeed").flag("rfd_flambeed").say("* Flambeed is extinguished! Next: Karamel.")
                .kill("karamel").stay().needFlag("rfd_flambeed").unlessFlag("rfd_karamel").flag("rfd_karamel").say("* Karamel melts away! Next: Dessourt.")
                .kill("dessourt").stay().needFlag("rfd_karamel").unlessFlag("rfd_dessourt").flag("rfd_dessourt").say("* Dessourt dissipates! Next: the Gelatinnoth Mother.")
                .kill("gelatinnoth_mother").stay().needFlag("rfd_dessourt").unlessFlag("rfd_gelatinnoth").flag("rfd_gelatinnoth")
                .say("* The Gelatinnoth Mother wobbles and collapses! Only the Culinaromancer remains.")
                .kill("culinaromancer").needFlag("rfd_gelatinnoth").say("* The Culinaromancer is defeated! Time resumes in the dining room.")
                .stage("I should speak to the Gypsy in the dining room.")
                .talk("rfd_gypsy").say("You did it! The Culinaromancer is gone and the council is free. His chest is yours to plunder: it holds the finest gloves in Gielinor.")
                .done("rfd_gypsy", "The council thanks you.");
        // As in OSRS, freeing N council members (in any order) unlocks the N-th glove tier.
        StringBuilder flags = new StringBuilder();
        for (String[] unlock : GLOVE_UNLOCKS) {
            flags.append(flags.length() == 0 ? "" : ",").append(unlock[0]);
        }
        // Hardleather gloves: after the beginning of the quest (Another Cook's Quest); older saves count any freed member.
        b.shop("rfd_gypsy", "rfd_hardleather_gloves", price("rfd_hardleather_gloves"), null, "count:1:rfd_cook," + flags, null, 0,
                "From the Culinaromancer's chest: hardleather gloves.");
        for (int i = 0; i < GLOVE_UNLOCKS.length; i++) {
            String glove = GLOVE_UNLOCKS[i][1];
            b.shop("rfd_gypsy", glove, price(glove), null, "count:" + (i + 1) + ":" + flags, null, 0,
                    "From the Culinaromancer's chest: " + glove.replace("rfd_", "").replace('_', ' ') + ".");
        }
        b.shop("rfd_gypsy", "barrows_gloves", 130000, "recipe_for_disaster", null, null, 0,
                "From the Culinaromancer's chest: Barrows gloves, the finest gloves in Gielinor.");
    }

    static int price(String glove) {
        for (Object[] g : QuestItemsData.RFD_GLOVES) {
            if (g[0].equals(glove)) {
                return (Integer) g[5];
            }
        }
        throw new IllegalArgumentException(glove);
    }
}
