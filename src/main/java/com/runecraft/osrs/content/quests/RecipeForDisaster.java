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

        b.quest("recipe_for_disaster", "Recipe for Disaster", Difficulty.GRANDMASTER, 10)
                .reqQuest("COOKS_ASSISTANT").req(Skill.COOKING, 10)
                .xp(Skill.COOKING, 20000).xp(Skill.HITPOINTS, 20000)
                .unlock("The Culinaromancer's chest: food and RFD gloves, up to Barrows gloves")
                .stage("I can start this quest by speaking to the Gypsy in Lumbridge Castle's dining room. (Requires Cook's Assistant, 10 Cooking.)")
                .talk("rfd_gypsy").say("The Duke is holding a banquet for the Secret Council of Gielinor, and the Cook is in a terrible state. He needs ingredients!")
                .stage("Another Cook's Quest: the Cook needs a bucket of milk, an egg and a pot of flour for the banquet.")
                .talk("rfd_gypsy").take("minecraft:milk_bucket", "minecraft:egg", "pot_of_flour").grantXp(Skill.COOKING, 1000)
                .say("* The banquet begins... and a sinister chef appears in a puff of smoke! It's the Culinaromancer!",
                        "@Culinaromancer: Fools! You'll never taste my cooking again! *He freezes the council in time.*",
                        "I have slowed time to stop him. Free each council member by cooking their favourite dish!")
                .missing("The Cook needs a bucket of milk, an egg and a pot of flour.")
                .stage("I must free the eight frozen council members by bringing each the dish that reminds them of who they are.")
                .talk("rohak").stay().unlessFlag("rfd_dwarf").take("beer:2", "rfd_rock_cake").flag("rfd_dwarf")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.SLAYER, 1000)
                .say("Asgarnian ale and a rock cake! *The Mountain Dwarf is freed from the spell.*")
                .missing("A dwarf likes his ale. And a good rock cake. Bring me two ales and a rock cake.")
                .talk("evil_dave").stay().unless("rfd_rock_cake").unlessFlag("rfd_dwarf").give("rfd_rock_cake")
                .say("Rock cakes? Mother bakes them by the dozen. Take one, just go away.")
                .talk("general_bentnoze").stay().unlessFlag("rfd_goblins").take("spice", "cake").flag("rfd_goblins")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.FARMING, 1000).grantXp(Skill.CRAFTING, 1000)
                .say("Slop of compromise! *The goblin generals stop arguing long enough to be freed.*")
                .missing("Goblins want slop! Spicy slop with cake in it!")
                .talk("pirate_pete").stay().unlessFlag("rfd_pete").take("raw_salmon:2").flag("rfd_pete")
                .grantXp(Skill.COOKING, 1000).grantXp(Skill.FISHING, 1000).grantXp(Skill.CRAFTING, 1000)
                .say("Fishcake! Arr, that's what I was missin'. *Pirate Pete is freed.*")
                .missing("Arr, a fishcake. Bring me two raw salmon and I'll show ye how.")
                .talk("lumbridge_guide").stay().unlessFlag("rfd_guide").reqSkill(Skill.COOKING, 40)
                .reqDone("biohazard", "DEMON_SLAYER", "nature_spirit", "witchs_house", "murder_mystery").take("cake", "chocolate_dust").flag("rfd_guide")
                .grantXp(Skill.COOKING, 2500).grantXp(Skill.MAGIC, 2500)
                .say("A cake with chocolate! Just like the ones I give to new adventurers. *The Lumbridge Guide is freed.*")
                .missing("I would love a chocolate cake... (Requires 40 Cooking, Biohazard, Demon Slayer, Murder Mystery, Nature Spirit and Witch's House.)")
                .talk("evil_dave").stay().unlessFlag("rfd_dave").reqSkill(Skill.COOKING, 25).reqDone("gertrudes_cat", "shadow_of_the_storm").take("spice:4", "potato").flag("rfd_dave")
                .grantXp(Skill.COOKING, 7000)
                .say("Spicy stew! The darkness... it's so spicy... *Evil Dave is freed. He seems even more evil.*")
                .missing("Bring me the spiciest stew: four spices and a potato. (Requires 25 Cooking, Gertrude's Cat, Shadow of the Storm.)")
                .talk("skrach_uglogwee").stay().unlessFlag("rfd_skrach").reqSkill(Skill.COOKING, 41).reqSkill(Skill.FIREMAKING, 20).reqDone("big_chompy_bird_hunting")
                .take("raw_bird_meat:2").flag("rfd_skrach").grantXp(Skill.COOKING, 1500).grantXp(Skill.WOODCUTTING, 1500).grantXp(Skill.CRAFTING, 1500)
                .say("Chompy! Roasted chompy! *Skrach Uglogwee is freed.*")
                .missing("Skrach want chompy bird. Two. Roasted. (Requires 41 Cooking, 20 Firemaking, Big Chompy Bird Hunting.)")
                .talk("sir_amik_varze").stay().unlessFlag("rfd_amik").reqDone("lost_city")
                .take("minecraft:egg", "minecraft:milk_bucket", "pot_of_flour").flag("rfd_amik").grantXp(Skill.COOKING, 4000).grantXp(Skill.HITPOINTS, 4000)
                .say("Brulee supreme, with an egg from the Evil Chicken of Zanaris! *Sir Amik Varze is freed.*")
                .missing("A brulee supreme: egg, milk and flour... and you'll need access to Zanaris. (Requires Lost City.)")
                .talk("king_awowogei").stay().unlessFlag("rfd_awowogei").reqDone("monkey_madness_i").reqSkill(Skill.COOKING, 70).reqSkill(Skill.AGILITY, 48)
                .take("raw_shark").flag("rfd_awowogei").grantXp(Skill.COOKING, 10000).grantXp(Skill.AGILITY, 10000)
                .say("Ook! Stuffed snake! *King Awowogei is freed, and allows a monkey to join the council.*")
                .missing("Ook! The king wants stuffed snake. (Requires Monkey Madness I, 70 Cooking, 48 Agility; bring a raw shark.)")
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
        for (String[] unlock : GLOVE_UNLOCKS) {
            b.shop("rfd_gypsy", unlock[1], price(unlock[1]), null, unlock[0], null, 0,
                    "From the Culinaromancer's chest: " + unlock[1].replace("rfd_", "").replace('_', ' ') + ".");
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
