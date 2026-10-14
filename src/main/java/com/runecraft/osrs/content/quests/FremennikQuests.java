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

/** Fremennik Province quests: The Fremennik Trials, Lunar Diplomacy, Mountain Daughter and Horror from the Deep. */
final class FremennikQuests {
    private FremennikQuests() {
    }

    static void register(QuestBook b) {
        b.place("draugen_hunt", "Hunting grounds", 2625, 3700, PlaceDef.Style.CAMP);
        b.place("rellekka_longhall_basement", "Longhall arena", 2655, 3690, PlaceDef.Style.ARENA);
        b.spot("swensens_maze", "Swensen's maze", 2635, 3660);
        b.spot("fossegrimen_lake", "Lake of the fossegrimen", 2620, 3612);
        b.place("quest_lunar_isle", "Lunar Isle", 2111, 3915, PlaceDef.Style.TOWN);
        b.place("lunar_dream_world", "Dream world", 2080, 3920, PlaceDef.Style.ARENA);
        b.place("mountain_camp", "Mountain Camp", 2810, 3672, PlaceDef.Style.CAMP);
        b.place("kendal_cave", "The Kendal's cave", 2790, 3700, PlaceDef.Style.CAVE);
        b.place("lighthouse", "Lighthouse", 2509, 3635, PlaceDef.Style.RUINS);
        b.spot("barbarian_outpost", "Barbarian Outpost", 2536, 3572);
        fremennikTrials(b);
        lunarDiplomacy(b);
        mountainDaughter(b);
        horrorFromTheDeep(b);
    }

    private static void fremennikTrials(QuestBook b) {
        b.npc("brundt", "Brundt the Chieftain", "RELLEKKA", 0, -7, 0xF0C8A8, 0x8A5A2A, 0x5A3A1A, 0xE0A040);
        b.npc("manni", "Manni the Reveller", "RELLEKKA", 4, -6, 0xF0C0A0, 0x6A2A2A, 0x3A2A1A, 0xC06A2A);
        b.npc("sigli", "Sigli the Huntsman", "RELLEKKA", -4, -6, 0xE8C0A0, 0x3A5A2A, 0x3A2A1A, 0x8A5A2A);
        b.npc("swensen", "Swensen the Navigator", "RELLEKKA", 6, -3, 0xF0C8A9, 0x2A4A6A, 0x2A2A3A, 0xD8D8D8);
        b.npc("peer", "Peer the Seer", "RELLEKKA", -6, -3, 0xE8D0C0, 0x6A3A8A, 0x3A2A4A, 0xE8E8E8);
        b.npc("thorvald", "Thorvald the Warrior", "RELLEKKA", 6, 3, 0xE0B898, 0x7A7A8A, 0x3A3A3A, 0xE0C060);
        b.npc("olaf", "Olaf the Bard", "RELLEKKA", -6, 3, 0xF0C8AA, 0xC0A03A, 0x5A4A2A, 0xC08030);
        b.npc("sigmund", "Sigmund the Merchant", "RELLEKKA", 4, 6, 0xE8C0A1, 0x3A6A6A, 0x2A3A3A, 0x6A4A2A);
        b.npc("fossegrimen", "Fossegrimen", "fossegrimen_lake", 0, 0, 0x8AC0C0, 0x3A8A8A, 0x2A5A5A, 0x5AA0A0);
        b.item("enchanted_lyre", "Enchanted lyre", "key", 0xE0C040, "A lyre blessed by the Fossegrimen.");
        b.item("fremennik_ballad", "Fremennik ballad", "note", 0xE0D8B0, "A ballad of the Fremennik, composed by you and Olaf.");
        b.boss(BossDef.of("draugen", "The Draugen").level(69).stats(60, 60, 60, 60, 1, 1).speed(5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 10, 2.5, 10))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("draugen_hunt").arena(9, "ice"));
        b.boss(BossDef.of("koschei_the_deathless", "Koschei the Deathless").level(160).stats(90, 80, 80, 80, 1, 1).speed(4)
                .defenceBonus(40, 40, 40, 0, 40)
                .attack(AttackDef.of("sword", AttackType.SLASH, 10, 2.5, 10))
                .phase(75, "Koschei rises again, stronger!", 1.2, false)
                .phase(50, "Koschei rises again, stronger still!", 1.4, false)
                .phase(25, "Koschei rises a final time!", 1.6, false)
                .drops(Drop.always("bones")).skin(0xE0C0A0, 0x5A5A6A, 0x3A3A3A, 0x8A8A8A).at("rellekka_longhall_basement").arena(9, "stone"));
        b.quest("fremennik_trials", "The Fremennik Trials", Difficulty.INTERMEDIATE, 3)
                // OSRS: no skill requirements (25 Fletching, 40 Woodcutting, 40 Crafting are one optional route).
                .xp(Skill.AGILITY, 2812.4).xp(Skill.ATTACK, 2812.4).xp(Skill.CRAFTING, 2812.4).xp(Skill.DEFENCE, 2812.4).xp(Skill.FISHING, 2812.4)
                .xp(Skill.FLETCHING, 2812.4).xp(Skill.HITPOINTS, 2812.4).xp(Skill.STRENGTH, 2812.4).xp(Skill.THIEVING, 2812.4).xp(Skill.WOODCUTTING, 2812.4)
                .unlock("A Fremennik name and citizenship of Rellekka")
                .stage("I can start this quest by speaking to Brundt the Chieftain in the longhall of Rellekka. (Requires 25 Fletching, 40 Woodcutting, 40 Crafting.)")
                .talk("brundt").say("An outerlander wants to become one of us? Then you must win the votes of seven members of the council:",
                        "Manni, Sigli, Swensen, Peer, Thorvald, Olaf and Sigmund. Each will set you a trial.")
                .stage("I need to earn the votes of Manni, Sigli, Swensen, Peer, Thorvald, Olaf and Sigmund.")
                .talk("manni").stay().unlessFlag("ft_manni").take("beer:2").flag("ft_manni")
                .say("A drinking contest! *You swap your drink for low-alcohol ale and drink Manni under the table.* Hic! You have my vote!")
                .missing("Beat me in a drinking contest! Bring two beers and we'll see who's still standing.")
                .kill("draugen").stay().unlessFlag("ft_draugen").flag("ft_draugen").say("* The Draugen is slain! Sigli will want to hear of this.")
                .talk("sigli").stay().needFlag("ft_draugen").unlessFlag("ft_sigli").flag("ft_sigli")
                .say("You hunted down the Draugen? Few could. You have my vote.")
                .missing("Hunt the Draugen, the walking dead of our hunting grounds, and you shall have my vote.")
                .reach("swensens_maze", 5).stay().unlessFlag("ft_maze").flag("ft_maze").say("* You find your way to the end of Swensen's maze.")
                .talk("swensen").stay().needFlag("ft_maze").unlessFlag("ft_swensen").flag("ft_swensen")
                .say("You navigated my maze. A true navigator! You have my vote.")
                .missing("Find your way through my maze of portals, west of the town.")
                .talk("peer").stay().unlessFlag("ft_peer").flag("ft_peer")
                .say("Answer my riddle: 'My first is in mage, but not in wizard...' *You answer: 'Mind'.* Correct. You have my vote.")
                .kill("koschei_the_deathless").stay().unlessFlag("ft_koschei").flag("ft_koschei").say("* Koschei the Deathless is finally beaten.")
                .talk("thorvald").stay().needFlag("ft_koschei").unlessFlag("ft_thorvald").flag("ft_thorvald")
                .say("You defeated Koschei the Deathless! A true warrior. You have my vote.")
                .missing("Face Koschei the Deathless in the arena beneath the longhall.")
                .talk("fossegrimen").stay().unless("enchanted_lyre", "fremennik_ballad").unlessFlag("ft_olaf").take("raw_shark").give("enchanted_lyre")
                .say("* You offer a raw shark to the Fossegrimen. Your lyre is enchanted.")
                .missing("* The Fossegrimen wants an offering of a raw shark.")
                .talk("olaf").stay().unlessFlag("ft_olaf").take("enchanted_lyre").give("fremennik_ballad").flag("ft_olaf")
                .say("Play for the longhall with your enchanted lyre! *You perform the ballad you wrote.* Wonderful! You have my vote.")
                .missing("A bard needs an instrument blessed by the Fossegrimen at the lake to the south.")
                .talk("sigmund").stay().unlessFlag("ft_sigmund").take("fremennik_ballad").flag("ft_sigmund")
                .say("A Fremennik ballad! The Sailor's lady will trade me anything for this. A fine trade. You have my vote.")
                .missing("I'm a merchant. Find me something worth trading... a love ballad, perhaps?")
                .talk("brundt").needFlag("ft_manni", "ft_sigli", "ft_swensen", "ft_peer", "ft_thorvald", "ft_olaf", "ft_sigmund")
                .say("All seven votes! From this day you are one of the Fremennik. Welcome, brother!")
                .missing("You need all seven votes, outerlander.")
                .done("brundt", "Welcome, Fremennik!");
    }

    private static void lunarDiplomacy(QuestBook b) {
        b.npc("lokar_searunner", "Lokar Searunner", "RELLEKKA", -4, 7, 0xE8C0A2, 0x5A3A6A, 0x3A2A3A, 0xE0E0E0);
        b.npc("oneiromancer", "Oneiromancer", "quest_lunar_isle", 0, 3, 0xF0E0D0, 0x8AA0C0, 0x6A80A0, 0xC0D0F0);
        b.npc("pauline_polaris", "Pauline Polaris", "quest_lunar_isle", 3, -2, 0xF0D8C8, 0x6A6AA0, 0x4A4A7A, 0xD0C060);
        b.item("seal_of_passage", "Seal of passage", "amulet", 0x8AA0C0, "Lets you be understood by the Moon Clan of Lunar Isle.");
        b.item("waking_sleep_vial", "Waking sleep vial", "potion", 0x8A8AE0, "A potion to enter the dream world.");
        b.item("lunar_moonclan_manual", "Moonclan manual", "book", 0x5A6A8A, "A manual on the Lunar arts.");
        b.item("ld_lunar_ore", "Lunar ore", "ore", 0x8AA0C0, "Ore from the Lunar Isle mine.");
        b.item("ld_emerald_lantern", "Emerald lantern", "lantern", 0x3AA05A, "A bullseye lantern with an emerald lens. It shows things hidden to the eye.");
        b.spot("lunar_isle_mine", "Lunar Isle mine", 2142, 3944); // approx: the ladder in the north-east of the isle
        b.boss(BossDef.of("me", "Me").level(79).stats(100, 70, 70, 70, 70, 70).speed(4)
                .attack(AttackDef.of("mirror_strike", AttackType.SLASH, 12, 2.5, 10))
                .attack(AttackDef.of("mirror_spell", AttackType.MAGIC, 12, 8, 4).particle("minecraft:end_rod"))
                .drops(Drop.always("coins")).skin(0xD8C8E8, 0x6A6AA0, 0x4A4A7A, 0xC0D0F0).at("lunar_dream_world").arena(9, "void"));
        b.quest("lunar_diplomacy", "Lunar Diplomacy", Difficulty.EXPERIENCED, 2)
                .reqQuest("fremennik_trials", "lost_city", "RUNE_MYSTERIES", "shilo_village")
                .req(Skill.HERBLORE, 5).req(Skill.CRAFTING, 61).req(Skill.DEFENCE, 40).req(Skill.FIREMAKING, 49)
                .req(Skill.MAGIC, 65).req(Skill.MINING, 60).req(Skill.WOODCUTTING, 55)
                .xp(Skill.MAGIC, 5000).xp(Skill.RUNECRAFT, 5000).reward("seal_of_passage", "lunar_staff")
                .unlock("The Lunar spellbook")
                .stage("I can start this quest by speaking to Lokar Searunner on the Rellekka docks. (Requires Fremennik Trials, Lost City, Rune Mysteries, Shilo Village and many skills.)")
                .talk("lokar_searunner").say("Lunar Isle? Few go there, and none without a seal of passage from Brundt. Get one and I'll take you... on my old crew's ship.")
                // OSRS: Brundt's seal, then Captain Bentley's pirate ship: the cabin boy's emerald lens on a lit lantern reveals five symbols to rub away.
                .stage("I need a seal of passage from Brundt; then Lokar's pirate friends will sail me to Lunar Isle, where I should speak to the Oneiromancer.")
                .talk("brundt").stay().unless("seal_of_passage").give("seal_of_passage")
                .say("The Moon Clan? Wear this seal of passage, or they won't understand a word you say. Be careful, outerlander.")
                // OSRS: the cabin boy's bullseye lantern has no oil: fill it with lamp oil (swamp tar in the Rimmington still) and fit the emerald lens.
                .talk("lokar_searunner").stay().need("seal_of_passage").unless("ld_emerald_lantern").unlessFlag("ld_sailed").take("swamp_tar").give("ld_emerald_lantern")
                .say("* The cabin boy gives you an empty bullseye lantern and an emerald lens. You fill it with lamp oil distilled from swamp tar and fit the lens.")
                .missing("The cabin boy's lantern has no oil. Bring some swamp tar to distil into lamp oil (Rimmington has a still).")
                .talk("lokar_searunner").stay().unlessFlag("ld_sailed").need("seal_of_passage", "tinderbox", "ld_emerald_lantern").flag("ld_sailed").travel("quest_lunar_isle")
                .say("* Captain Bentley's ship is cursed with Lunar symbols. The cabin boy gives you an emerald lens for a lantern; lit, it shows the symbols",
                        "* on the cannon, the wallchart, a chest, a support pillar and a crate. You rub them away and the ship sails for Lunar Isle.")
                .missing("Got your seal of passage? And bring a tinderbox to light the emerald lantern; that ship's always dark below decks.")
                .talk("oneiromancer").needFlag("ld_sailed").need("seal_of_passage").give("lunar_moonclan_manual")
                .say("You wear the seal, so my people understand you. The Fremennik want peace with the Moon Clan? Then prove yourself to us.",
                        "Make a waking sleep potion to enter the dream world, and dress in proper Lunar equipment. This manual will guide you.")
                .missing("* The Oneiromancer cannot understand you. You need a seal of passage.")
                // OSRS: a suqah tooth for the potion; the dramen staff charged at the air, fire, water and earth altars becomes the Lunar staff;
                // lunar ore from the mine smithed into a helm; suqah hides tanned and crafted into the torso, trousers, gloves and boots.
                .stage("I need a waking sleep potion (guam, marrentill, a vial of water and a ground suqah tooth), a Lunar staff from my dramen staff and a set of Lunar equipment.")
                .drop("suqah", "suqah_tooth", 3)
                .reach("AIR_ALTAR", 10).stay().need("dramen_staff").unlessFlag("ld_staff_air").flag("ld_staff_air")
                .say("* You hold the dramen staff over the air altar. It hums.")
                .reach("FIRE_ALTAR", 10).stay().need("dramen_staff").needFlag("ld_staff_air").unlessFlag("ld_staff_fire").flag("ld_staff_fire")
                .say("* The fire altar warms the staff.")
                .reach("WATER_ALTAR", 10).stay().need("dramen_staff").needFlag("ld_staff_fire").unlessFlag("ld_staff_water").flag("ld_staff_water")
                .say("* The water altar cools the staff.")
                .reach("EARTH_ALTAR", 10).stay().need("dramen_staff").needFlag("ld_staff_water").unlessFlag("ld_staff").flag("ld_staff")
                .say("* At the earth altar the staff shimmers: it has become a Lunar staff.")
                .reach("lunar_isle_mine", 8).stay().unless("ld_lunar_ore").unlessFlag("ld_equipment").needTool("pickaxe").reqSkill(Skill.MINING, 60)
                .give("ld_lunar_ore").say("* You mine a stalagmite in the Lunar mine for some lunar ore.")
                .missing("* Lunar ore can be mined from the stalagmites here, with a pickaxe and 60 Mining.")
                .talk("pauline_polaris").stay().unlessFlag("ld_equipment").take("ld_lunar_ore").need("hammer").flag("ld_equipment")
                .say("* You smelt the ore into a lunar bar and hammer out a helm.",
                        "A fine Lunar helm. Meteora, Rimae and Selene will help with the rest: tiara, robes from suqah hide, and the ring.")
                .missing("Lunar equipment starts with lunar ore from the mine below the isle. And a hammer.")
                .talk("oneiromancer").needFlag("ld_equipment", "ld_staff").take("guam_leaf", "marrentill", "vial_of_water", "suqah_tooth", "suqah_hide:4")
                .need("pestle_and_mortar", "spade").give("waking_sleep_vial")
                .say("* Rimae tans your suqah hides and you craft the torso, trousers, gloves and boots; you dig up Selene's ring with your spade.",
                        "*She brews the potion with your ground suqah tooth.* Drink this in the shrine. In the dream world you must face your greatest enemy... yourself.")
                .missing("I need a guam leaf, a marrentill, a vial of water and a suqah tooth (and a pestle and mortar), four suqah hides for your robes,"
                        + " and a spade for the ring. Pauline Polaris will help with the helm, and charge your dramen staff at the four elemental altars.")
                .stage("I should drink the waking sleep potion and defeat 'Me' in the dream world.")
                .talk("oneiromancer").stay().need("waking_sleep_vial").travel("lunar_dream_world").say("Drink the potion... *The world fades into a dream.*")
                // OSRS dream world: five challenges (chance, numbers, mimic, race, memory) before the fight with "Me". Simplified.
                .reach("lunar_dream_world", 10).stay().unlessFlag("ld_challenges").need("waking_sleep_vial").flag("ld_challenges")
                .say("* Ethereal Fluke's dice, the Numerator's patterns, the Mimic's emotes, the race and the memory tiles: you pass every challenge.",
                        "* The Ethereal Being points to the last platform. Your reflection steps out to meet you.")
                .kill("me").needFlag("ld_challenges").take("waking_sleep_vial").travel("quest_lunar_isle").say("* You defeat your dream self and wake up in the shrine.")
                .stage("I should tell the Oneiromancer I've defeated myself.")
                .talk("oneiromancer").take("lunar_moonclan_manual").say("You have shown great wisdom. The Moon Clan will make peace with the Fremennik. Our Lunar magic is yours to learn.")
                .done("oneiromancer", "May the moon guide your spells.");
    }

    private static void mountainDaughter(QuestBook b) {
        b.npc("hamal", "Hamal the Chieftain", "mountain_camp", 0, 2, 0xE8C0A3, 0x6A4A2A, 0x4A3A2A, 0x2A1A0A);
        b.npc("svidi", "Svidi", "mountain_camp", 3, -2, 0xE8C0A4, 0x3A3A6A, 0x2A2A3A, 0xE0A040);
        b.npc("jokul", "Jokul", "mountain_camp", -3, -2, 0xE8C0A5, 0x6A2A2A, 0x3A1A1A, 0x6A3A1A);
        b.item("asleifs_necklace", "Asleif's necklace", "necklace", 0xC0A040, "Asleif's necklace.");
        b.item("corpse_of_woman", "Corpse of woman", "bones", 0xD8D0C0, "The remains of Asleif.");
        b.items(s -> s.add("bearhead", "Bearhead", Kind.ARMOUR, "full_helm", 0x6A4A2A).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 2, 0, 3, 4, 2, 2, 0, 0, 0, 0, 0), 1))
                .examine("A spiritual bear's head. Smells funky."));
        b.boss(BossDef.of("the_kendal", "The Kendal").level(70).stats(50, 60, 70, 60, 1, 1).speed(4)
                .attack(AttackDef.of("swipe", AttackType.SLASH, 9, 2.5, 10))
                .drops(Drop.always("bones")).skin(0x6A4A2A, 0x5A3A1A, 0x4A2A1A, 0x3A2A1A).size(0.9f, 2.4f, 1.2f).at("kendal_cave").arena(8, "ice"));
        b.quest("mountain_daughter", "Mountain Daughter", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.PRAYER, 2000).xp(Skill.ATTACK, 1000) // OSRS (current quest XP rewards)
                .req(Skill.AGILITY, 20).reward("bearhead")
                .stage("I can start this quest by speaking to Hamal the Chieftain at the Mountain Camp east of Rellekka. (Requires 20 Agility.)")
                .talk("hamal").say("My daughter Asleif is missing. The tribe whispers about the Kendal, a beast in the mountains. Please find her.")
                .stage("I should ask around the camp: Svidi and Jokul may know something.")
                .talk("jokul").say("Asleif took her necklace to the mountain shrine. That's where the Kendal hunts.")
                .stage("I should find the Kendal's cave on the mountain.")
                .kill("the_kendal").give("asleifs_necklace", "corpse_of_woman").say("* The Kendal falls. In its cave you find Asleif's body, still wearing her necklace.")
                .stage("I should take Asleif's remains and necklace back to Hamal.")
                .talk("hamal").take("asleifs_necklace", "corpse_of_woman").say("My daughter... we will bury her beside the lake. Take this bearhead, a spirit of our people.")
                .done("svidi", "Thank you for bringing Asleif home.");
    }

    private static void horrorFromTheDeep(QuestBook b) {
        b.npc("larrissa", "Larrissa", "lighthouse", 2, 3, 0xF0C8AB, 0x3A6A8A, 0x2A4A5A, 0xE0C060);
        b.npc("gunnjorn", "Gunnjorn", "barbarian_outpost", 0, 0, 0xE0B898, 0x6A2A1A, 0x3A2A1A, 0xC06A2A);
        b.npc("jossik", "Jossik", "lighthouse", -2, 3, 0xE8C0A6, 0x2A5A6A, 0x2A2A3A, 0xA0A0A0);
        b.item("lighthouse_key", "Lighthouse key", "key", 0x8A8A9A, "The key to the lighthouse.");
        // The damaged god books (damaged_holy_book and the others) are defined in osrs/content/CommonEquipment.
        b.boss(BossDef.of("dagannoth_mother", "Dagannoth Mother").level(100).stats(120, 100, 100, 100, 1, 1).speed(4)
                .defenceBonus(50, 50, 50, 0, 50)
                .attack(AttackDef.of("bite", AttackType.STAB, 15, 2.5, 8))
                .attack(AttackDef.of("water_spit", AttackType.RANGED, 12, 8, 3).particle("minecraft:splash"))
                .drops(Drop.always("dagannoth_bones")).size(1.2f, 2.6f, 1.0f).at("lighthouse").arena(11, "water"));
        b.quest("horror_from_the_deep", "Horror from the Deep", Difficulty.INTERMEDIATE, 2)
                .req(Skill.AGILITY, 35).reqQuest("alfred_grimhands_barcrawl")
                .xp(Skill.MAGIC, 4662.5).xp(Skill.STRENGTH, 4662.5).xp(Skill.RANGED, 4662.5).reward("damaged_holy_book")
                .unlock("Access to the Lighthouse and the Dagannoth caves")
                .stage("I can start this quest by speaking to Larrissa outside the lighthouse north of the Barbarian Outpost. (Requires 35 Agility.)")
                .talk("larrissa").say("My boyfriend Jossik is the lighthouse keeper, and he's locked inside! The light is out. Gunnjorn at the Barbarian Outpost has the spare key.")
                .stage("I should get the spare lighthouse key from Gunnjorn at the Barbarian Outpost, then cross the basalt rocks to the lighthouse.")
                .talk("gunnjorn").stay().unless("lighthouse_key").give("lighthouse_key")
                .say("Larrissa sent you? Here's the key. Mind the agility course on your way out!")
                .reach("lighthouse", 6).need("lighthouse_key").reqSkill(Skill.AGILITY, 35)
                .say("* You leap from basalt rock to basalt rock and unlock the lighthouse door.")
                .missing("* The lighthouse stands beyond the basalt rocks. You'll need the spare key from Gunnjorn.")
                .stage("I should enter the lighthouse and repair the lighting mechanism (3 planks, 2 steel bars and a tinderbox).")
                .talk("larrissa").take("lighthouse_key", "plank:3", "steel_bar:2").need("tinderbox")
                .say("* You patch the bridge, repair the mechanism and light the lamp. The lighthouse shines again!",
                        "Jossik's not here! The basement door is open... he must have gone below!")
                .missing("We'll need 3 planks, 2 steel bars for nails and a tinderbox to fix the lighthouse.")
                .stage("I should go into the caves below the lighthouse. The strange door needs an air, water, earth and fire rune, a bronze arrow and a bronze sword.")
                .talk("jossik").stay().unlessFlag("hftd_door").take("air_rune", "water_rune", "earth_rune", "fire_rune", "bronze_arrow", "bronze_longsword").flag("hftd_door")
                .say("* You place the items in the door and it grinds open. Jossik is trapped inside!",
                        "Help! The mother of all Dagannoth is coming! It changes colour; each colour is weak to a different attack!")
                .missing("* The door has slots for an air, water, earth and fire rune, an arrow and a sword.")
                .kill("dagannoth_mother").needFlag("hftd_door").say("* The Dagannoth Mother sinks back into the depths.")
                .stage("I should check on Jossik.")
                .talk("jossik").say("You saved me! I found some damaged god books in the caves. Take this one, the book of Saradomin.")
                .stage("I should tell Larrissa that Jossik is safe.")
                .talk("larrissa").say("Jossik! You saved him! Thank you so much.")
                .done("jossik", "Bring me missing pages and I'll help you restore those books.");
        // Jossik's "salvaged prayer books": the other gods' damaged books unlock for 5,000 coins each (OSRS Wiki).
        for (String[] book : new String[][]{{"damaged_unholy_book", "Zamorak"}, {"damaged_book_of_balance", "Guthix"},
                {"damaged_book_of_war", "Bandos"}, {"damaged_book_of_law", "Armadyl"}, {"damaged_book_of_darkness", "Zaros"}}) {
            b.shop("jossik", book[0], 5000, "horror_from_the_deep", null, null, 0,
                    "More books keep washing up by the lighthouse. This one of " + book[1] + "'s is yours for 5,000 coins.");
        }
    }
}
