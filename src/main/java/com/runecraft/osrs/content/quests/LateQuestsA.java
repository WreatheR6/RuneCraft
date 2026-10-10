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
 * Fairytale I and II, Garden of Tranquillity, The Path of Glouphrie, Elemental Workshop II, Eagles'
 * Peak, The Giant Dwarf, Olaf's Quest, The Fremennik Isles, Throne of Miscellania, Royal Trouble,
 * Cold War, Tower of Life, King's Ransom, Grim Tales and The Hand in the Sand. XP marked "approx" in
 * comments is a best-known value where OSRS gives a choice of lamps.
 */
final class LateQuestsA {
    private LateQuestsA() {
    }

    static void register(QuestBook b) {
        b.place("tanglefoot_lair", "Fairy Godfather's garden", 3150, 3140, PlaceDef.Style.CAVE);
        b.place("gorak_plane", "Gorak plane", 3135, 3150, PlaceDef.Style.ARENA);
        b.spot("queens_garden", "Varrock Palace garden", 3230, 3480);
        b.place("glouphrie_tunnels", "Glouphrie's tunnels", 2420, 3405, PlaceDef.Style.CAVE);
        b.place("eagles_peak", "Eagles' Peak", 2330, 3490, PlaceDef.Style.RUINS);
        b.spot("ardougne_zoo", "Ardougne Zoo", 2610, 3270);
        b.place("keldagrim", "Keldagrim", 2730, 3713, PlaceDef.Style.TOWN);
        b.place("ulfrics_tomb", "Ulfric's tomb", 2723, 3740, PlaceDef.Style.CAVE);
        b.place("neitiznot", "Neitiznot", 2335, 3800, PlaceDef.Style.TOWN);
        b.place("jatizso", "Jatizso", 2415, 3800, PlaceDef.Style.TOWN);
        b.place("ice_troll_cave", "Ice troll caves", 2400, 3850, PlaceDef.Style.CAVE);
        b.place("miscellania", "Miscellania", 2500, 3860, PlaceDef.Style.TOWN);
        b.place("etceteria_caves", "Miscellania dungeon", 2510, 3845, PlaceDef.Style.CAVE);
        b.place("penguin_iceberg", "Penguin iceberg", 2650, 4000, PlaceDef.Style.CAMP);
        b.place("tower_of_life", "Tower of Life", 2649, 3220, PlaceDef.Style.RUINS);
        b.place("castle_wars_knights", "Black Knights' prison", 2895, 3550, PlaceDef.Style.CAVE);
        b.place("grim_tales_tower", "Witch's tower", 2905, 3450, PlaceDef.Style.RUINS);
        b.spot("yanille_beach", "Yanille sandpit", 2550, 3100);
        fairytales(b);
        gardenOfTranquillity(b);
        pathOfGlouphrie(b);
        elementalWorkshopII(b);
        eaglesPeak(b);
        giantDwarf(b);
        olafsQuest(b);
        fremennikIsles(b);
        throneOfMiscellania(b);
        royalTrouble(b);
        coldWar(b);
        towerOfLife(b);
        kingsRansom(b);
        grimTales(b);
        handInTheSand(b);
    }

    private static void fairytales(QuestBook b) {
        b.npc("martin_the_master_gardener", "Martin the Master Gardener", "DRAYNOR_VILLAGE", -6, -5, 0xE8C0BC, 0x5A8A3B, 0x3A5A2B, 0xC0B081);
        b.npc("fairy_godfather", "Fairy Godfather", "quest_zanaris", 3, -3, 0xF0E0F1, 0x5A3A8A, 0x3A2A5A, 0xE0D061);
        b.npc("fairy_nuff", "Fairy Nuff", "quest_zanaris", -3, -3, 0xF0E0F2, 0xE0A0C0, 0xC080A0, 0xF0D062);
        b.npc("malignius_mortifer", "Malignius Mortifer", "lumbridge_swamp_camp", -3, 3, 0xC8C0C0, 0x2A1A3A, 0x1A0A2A, 0x1A1A20);
        b.items(s -> s.add("magic_secateurs", "Magic secateurs", Kind.TOOL, "knife", 0xA060E0).value(1).stack(1)
                .examine("Magic secateurs. Increase the yield of herbs, allotments and hops by 10%."));
        b.item("star_flower", "Star flower", "herb", 0xE0E080, "A rare flower, ingredient of the Fairy Queen's cure.");
        b.item("gorak_claws", "Gorak claws", "bones", 0x6A3A6A, "Claws from a gorak.");
        b.item("queens_secateurs", "Queen's secateurs", "knife", 0xE0C047, "The Fairy Queen's secateurs.");
        b.boss(BossDef.of("tanglefoot", "Tanglefoot").level(111).stats(102, 90, 90, 90, 1, 1).speed(5)
                .attack(AttackDef.of("vine_whip", AttackType.CRUSH, 15, 3, 7))
                .attack(AttackDef.of("entangle", AttackType.MAGIC, 8, 8, 3).special(Special.FREEZE, 5).particle("minecraft:composter"))
                .requiresWeapon("magic_secateurs")
                .drops(Drop.always("logs")).size(1.0f, 2.4f, 1.0f).at("tanglefoot_lair").arena(10, "jungle"));
        b.boss(BossDef.of("gorak", "Gorak").level(145).stats(112, 120, 120, 110, 1, 1).speed(4)
                .attack(AttackDef.of("claw", AttackType.SLASH, 16, 2.5, 10).special(Special.DRAIN_STAT, Skill.HITPOINTS.ordinal()))
                .drops(Drop.always("gorak_claws")).skin(0x6A3A6B, 0x4A2A4A, 0x3A1A3A, 0x8A4A8A).at("gorak_plane").arena(9, "void"));
        b.quest("fairytale_i_growing_pains", "Fairytale I - Growing Pains", Difficulty.EXPERIENCED, 2)
                .reqQuest("lost_city", "nature_spirit")
                .xp(Skill.FARMING, 3500).xp(Skill.ATTACK, 2000).xp(Skill.MAGIC, 1000).reward("magic_secateurs")
                .stage("I can start this quest by speaking to Martin the Master Gardener in Draynor Village. (Requires Lost City, Nature Spirit.)")
                .talk("martin_the_master_gardener").say("All my crops are dying! Something in Zanaris is to blame, I just know it. Ask the fairies.")
                .stage("I should ask the Fairy Godfather in Zanaris about the dying crops.")
                .talk("fairy_godfather").give("magic_secateurs").say("The Fairy Queen is missing and Tanglefoot, her guardian plant, has run amok. Kill it, with these magic secateurs.")
                .stage("I must defeat Tanglefoot using the magic secateurs.")
                .kill("tanglefoot").say("* Tanglefoot withers. The crops of Gielinor begin to recover.")
                .stage("I should tell Martin the crops will recover.")
                .talk("martin_the_master_gardener").say("My crops are healthy again! You keep those magic secateurs, they'll boost your harvests.");
        b.quest("fairytale_ii_cure_a_queen", "Fairytale II - Cure a Queen", Difficulty.EXPERIENCED, 2)
                .reqQuest("fairytale_i_growing_pains").req(Skill.THIEVING, 40).req(Skill.FARMING, 49).req(Skill.HERBLORE, 57)
                .xp(Skill.HERBLORE, 3500).xp(Skill.THIEVING, 2500)
                .unlock("Use of the fairy rings")
                .stage("I can start this quest by speaking to Martin the Master Gardener. (Requires Fairytale I, 40 Thieving, 49 Farming, 57 Herblore.)")
                .talk("martin_the_master_gardener").say("The fairies are acting strangely again. Something's still wrong in Zanaris.")
                .stage("I should find out what happened to the Fairy Queen. Fairy Nuff the healer knows.")
                .talk("fairy_nuff").say("The Queen is in a coma, and the Godfather has stolen her secateurs, her symbol of power! The necromancer Malignius knows the cure.")
                .stage("I should ask the necromancer Malignius Mortifer in Lumbridge Swamp how to cure the Queen.")
                .talk("malignius_mortifer").say("A cure? Star flowers and gorak claw powder, mixed into a potion. The goraks live on their own plane, reached by fairy ring.")
                .stage("I need a star flower, gorak claws (from the gorak plane) and a vial of water.")
                .kill("gorak").stay().unless("gorak_claws").give("gorak_claws").say("* You take the gorak's claws.")
                .talk("fairy_nuff").stay().unless("star_flower").take("ranarr_seed").give("star_flower").say("Plant this ranarr seed in my garden... *a star flower blooms.* Here!")
                .talk("fairy_nuff").take("star_flower", "gorak_claws", "vial_of_water")
                .say("*She brews the cure and gives it to the Queen.* She stirs! But she needs her secateurs back to regain her power.")
                .missing("I need a star flower, gorak claws and a vial of water.")
                .stage("I should steal back the Queen's secateurs from the Fairy Godfather.")
                .talk("fairy_godfather").give("queens_secateurs").say("*While the Godfather gloats, you swipe the secateurs from his pocket.*")
                .stage("I should return the secateurs to the Fairy Queen.")
                .talk("fairy_queen").take("queens_secateurs").say("My power returns! The fairy rings are yours to use, friend of the fairies.")
                .done("fairy_nuff", "The Queen is herself again, thanks to you.");
    }

    private static void gardenOfTranquillity(QuestBook b) {
        b.npc("queen_ellamaria", "Queen Ellamaria", "queens_garden", 0, 0, 0xF0D0C0, 0xC03A8A, 0x8A2A6A, 0xE0C048);
        b.item("white_tree_sapling", "White tree sapling", "seed", 0xE0E0E3, "A sapling of the white tree.");
        b.quest("garden_of_tranquillity", "Garden of Tranquillity", Difficulty.INTERMEDIATE, 2)
                .reqQuest("creature_of_fenkenstrain").req(Skill.FARMING, 25).xp(Skill.FARMING, 5000)
                .stage("I can start this quest by speaking to Queen Ellamaria in Varrock Palace garden. (Requires Creature of Fenkenstrain, 25 Farming.)")
                .talk("queen_ellamaria").say("I want a garden fit for my husband King Roald, full of rare plants. A white tree, delphiniums, roses, orchids...")
                .stage("I need rare plants for the Queen's garden: a white tree sapling, 4 tomato seeds' worth of delphiniums and rose cuttings.")
                .talk("martin_the_master_gardener").stay().unless("white_tree_sapling").take("coins:500").give("white_tree_sapling")
                .say("A white tree? I've a sapling from the ruins of Saradomin's garden. 500 coins.")
                .talk("queen_ellamaria").take("white_tree_sapling", "tomato_seed:4", "strawberry_seed:4")
                .say("*The garden blooms with a white tree, delphiniums and roses.* It's perfect! Roald will love it.")
                .missing("I still need the white tree sapling and seeds for the flower beds (4 tomato and 4 strawberry seeds will do).")
                .stage("I should fetch King Roald to see the garden.")
                .talk("king_roald").say("What a garden! Ellamaria, it's beautiful. Thank you, adventurer.");
    }

    private static void pathOfGlouphrie(QuestBook b) {
        b.npc("longramble", "Longramble", "glouphrie_tunnels", 2, 2, 0xD0A073, 0x3A6A3D, 0x2A4A2E, 0x8A6A3E);
        b.boss(BossDef.of("warped_terrorbird", "Warped terrorbird").level(138).stats(150, 110, 110, 100, 100, 1).speed(4)
                .attack(AttackDef.of("peck", AttackType.STAB, 18, 2.5, 6))
                .attack(AttackDef.of("warp_bolt", AttackType.MAGIC, 16, 8, 4).particle("minecraft:portal"))
                .drops(Drop.always("bones")).size(1.0f, 2.0f, 1.0f).at("glouphrie_tunnels").arena(10, "void"));
        b.quest("the_path_of_glouphrie", "The Path of Glouphrie", Difficulty.EXPERIENCED, 2)
                .reqQuest("the_eyes_of_glouphrie", "waterfall_quest", "tree_gnome_village")
                .req(Skill.STRENGTH, 60).req(Skill.SLAYER, 56).req(Skill.THIEVING, 56).req(Skill.RANGED, 47).req(Skill.AGILITY, 45)
                .xp(Skill.STRENGTH, 15000).xp(Skill.SLAYER, 15000) // approx: OSRS gives a choice of lamps
                .stage("I can start this quest by speaking to Longramble west of the Tree Gnome Stronghold. (Requires The Eyes of Glouphrie and more.)")
                .talk("longramble").say("Glouphrie's warped creatures are spilling out of his old tunnels! Gnomes are going missing. Will you go in?")
                .stage("I should search Glouphrie's tunnels for the source of the warping.")
                .reach("glouphrie_tunnels", 8).say("* Deep in the tunnels, a twisted terrorbird shrieks and charges!")
                .stage("I must defeat the warped terrorbird.")
                .kill("warped_terrorbird").say("* The warped terrorbird falls. You find Glouphrie's warping device and destroy it.")
                .stage("I should report to King Narnode.")
                .talk("king_narnode").say("The warping is stopped! Glouphrie's legacy haunts us still... thank you.");
    }

    private static void elementalWorkshopII(QuestBook b) {
        b.item("beaten_book", "Beaten book", "book", 0x6A5A3B, "A beaten book about mind magic.");
        b.items(s -> s.add("mind_shield", "Mind shield", Kind.SHIELD, "kiteshield", 0x8A3AC0).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, 0, 0, 12, 14, 12, 4, 12, 0, 0, 0, 0),
                        Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                .examine("A shield made from mind metal. Better against mind magic."));
        b.quest("elemental_workshop_ii", "Elemental Workshop II", Difficulty.INTERMEDIATE, 1)
                .reqQuest("elemental_workshop_i").req(Skill.MAGIC, 20).req(Skill.SMITHING, 30)
                .xp(Skill.CRAFTING, 7500).xp(Skill.SMITHING, 7500).reward("mind_shield")
                .stage("I can start this quest by reading the beaten book in the Digsite exam centre. (Requires Elemental Workshop I, 20 Magic, 30 Smithing.)")
                .talk("archaeological_expert").give("beaten_book").say("This old book was found in the elemental workshop... it speaks of 'mind' metal. You can read it if you like.")
                .stage("I should enter the second level of the Elemental Workshop and power up the mind machinery.")
                .reach("elemental_workshop", 6).take("beaten_book").say("* You restore the steam pumps and pipes. The mind machine hums with energy.")
                .stage("I should forge mind metal into a shield using elemental ore and mind runes.")
                .talk("workshop_book").take("elemental_ore", "mind_rune:20", "coal:4").need("hammer")
                .say("* You smelt the ore, infuse it with mind runes and forge a mind shield.")
                .missing("* The book says: elemental ore, 4 coal and 20 mind runes, then hammer it out.");
    }

    private static void eaglesPeak(QuestBook b) {
        b.npc("charlie_zookeeper", "Charlie the Zookeeper", "ardougne_zoo", 0, 0, 0xE8C0BD, 0x5A6A2E, 0x3A4A1E, 0x6A4A21);
        b.npc("nickolaus", "Nickolaus", "eagles_peak", 2, 2, 0xE0B8AD, 0x8A6A3F, 0x5A4A2F, 0xA0A0A3);
        b.item("golden_feather", "Golden feather", "mark", 0xE0C049, "A golden eagle feather.");
        b.quest("eagles_peak", "Eagles' Peak", Difficulty.NOVICE, 2)
                .req(Skill.HUNTER, 27).xp(Skill.HUNTER, 2500)
                .unlock("Eagle transport and box trap hunting")
                .stage("I can start this quest by speaking to Charlie the Zookeeper in Ardougne Zoo. (Requires 27 Hunter.)")
                .talk("charlie_zookeeper").say("I want a golden eagle for the zoo! Nickolaus the ranger knows the eagles of Eagles' Peak.")
                .stage("I should find Nickolaus near Eagles' Peak.")
                .talk("nickolaus").say("The eagles live inside the peak. Disguise yourself as an eagle with feathers and you might get in.")
                .stage("I should get inside Eagles' Peak and find a golden feather.")
                .reach("eagles_peak", 8).give("golden_feather").say("* Disguised in a feathered costume, you slip into the eyrie and take a golden feather.")
                .stage("I should take the feather back to Charlie.")
                .talk("charlie_zookeeper").take("golden_feather").say("No eagle, but a golden feather? I suppose that'll do. The eagles will even fly you places now!");
    }

    private static void giantDwarf(QuestBook b) {
        b.npc("commander_veldaban", "Commander Veldaban", "keldagrim", 0, 3, 0xD8A88A, 0x6A6A3D, 0x4A4A2D, 0xC07A2F);
        b.npc("riki_the_sculptor", "Riki the sculptor's model", "keldagrim", 3, -2, 0xD8A88B, 0x3A5A8D, 0x2A3A5D, 0x6A3A22);
        b.item("dwarven_lore_book", "Book on dwarven lore", "book", 0x6A3A2A, "A book on the history of the dwarves.");
        b.quest("the_giant_dwarf", "The Giant Dwarf", Difficulty.INTERMEDIATE, 2)
                .req(Skill.CRAFTING, 12).req(Skill.FIREMAKING, 16).req(Skill.MAGIC, 33).req(Skill.THIEVING, 14)
                .xp(Skill.CRAFTING, 2500).xp(Skill.MINING, 2500).xp(Skill.SMITHING, 2500).xp(Skill.MAGIC, 1500).xp(Skill.FIREMAKING, 1500)
                .unlock("Access to Keldagrim and the Grand Exchange of the dwarves")
                .stage("I can start this quest by speaking to Commander Veldaban in Keldagrim. (Requires 12 Crafting, 16 Firemaking, 33 Magic, 14 Thieving.)")
                .talk("commander_veldaban").say("A human in Keldagrim? You knocked over the statue of King Alvis on your way in! You'll need to repair it... somehow.")
                .stage("I should help the sculptor rebuild the statue: Riki needs a model and the statue needs 6 iron bars.")
                .talk("riki_the_sculptor").take("iron_bar:6").say("*You pose while Riki sculpts and the smiths cast the statue.* It's even better than the original!")
                .missing("The statue needs 6 iron bars.")
                .stage("I should join one of the dwarven trading companies. Veldaban wants a report.")
                .talk("commander_veldaban").give("dwarven_lore_book").say("The statue's fixed and you've joined the Consortium! Welcome to Keldagrim, friend. Take this book of our lore.");
    }

    private static void olafsQuest(QuestBook b) {
        b.npc("olaf_hradson", "Olaf Hradson", "ulfrics_tomb", 2, 2, 0xE8C0BE, 0x6A3A1F, 0x3A2A1F, 0xE0A044);
        b.boss(BossDef.of("ulfric", "Ulfric").level(100).stats(100, 90, 90, 90, 1, 1).speed(4)
                .attack(AttackDef.of("skeletal_axe", AttackType.SLASH, 14, 2.5, 10))
                .drops(Drop.always("bones")).skin(0xE0D8C0, 0x5A5A5A, 0x3A3A3A, 0xC0C0C0).at("ulfrics_tomb").arena(9, "crypt"));
        b.quest("olafs_quest", "Olaf's Quest", Difficulty.INTERMEDIATE, 1)
                .reqQuest("fremennik_trials").req(Skill.FIREMAKING, 40).req(Skill.WOODCUTTING, 50)
                .xp(Skill.DEFENCE, 12000).reward("swordfish:20")
                .stage("I can start this quest by speaking to Olaf Hradson, west of Rellekka. (Requires The Fremennik Trials, 40 Firemaking, 50 Woodcutting.)")
                .talk("olaf_hradson").say("My grandfather Ulfric's treasure is buried in his tomb! I've a map, but... it's in a strange script. Help me find it?")
                .stage("I should light a fire beside Olaf's hut to reveal the map's hidden ink, then find Ulfric's tomb.")
                .talk("olaf_hradson").need("tinderbox").take("logs:2").say("*The heat reveals markings on the map.* The tomb is under the hills! Let's go.")
                .missing("We'll need a tinderbox and a couple of logs to read this map.")
                .stage("Ulfric's skeleton guards his tomb!")
                .kill("ulfric").say("* Ulfric's bones clatter to the floor. Behind him lies the treasure... a chest of swordfish and rune armour.")
                .stage("I should tell Olaf what we found.")
                .talk("olaf_hradson").say("Grandfather's treasure! Take these swordfish, and the defence lessons I learned from him.");
    }

    private static void fremennikIsles(QuestBook b) {
        b.npc("mord_gunnars", "Mord Gunnars", "RELLEKKA", 2, 8, 0xE8C0BF, 0x3A3A6D, 0x2A2A3E, 0xE0A045);
        b.npc("mawnis_burowgar", "Mawnis Burowgar", "neitiznot", 0, 3, 0xE0B8AE, 0x6A6A6B, 0x3A3A3F, 0xE0E0EB);
        b.npc("gjuki_sorvott", "King Gjuki Sorvott IV", "jatizso", 0, 3, 0xE0B8AF, 0xC0A04A, 0x6A4A1F, 0xE0C04B);
        b.item("ice_troll_king_head", "Head of the troll king", "skull", 0x8AA0C0, "The head of the Ice Troll King.");
        b.items(s -> s.add("helm_of_neitiznot", "Helm of Neitiznot", Kind.ARMOUR, "full_helm", 0x8A8AA0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 31, 29, 34, 3, 30, 3, 0, 0, 3), 55))
                .examine("A sturdy helm worn only by those whom Mawnis Burowgar trusts."));
        b.boss(BossDef.of("ice_troll_king", "Ice Troll King").level(122).stats(150, 110, 110, 110, 1, 1).speed(5)
                .attack(AttackDef.of("club", AttackType.CRUSH, 20, 3, 8))
                .attack(AttackDef.of("ice_breath", AttackType.MAGIC, 15, 8, 3).special(Special.FREEZE, 4).particle("minecraft:snowflake"))
                .drops(Drop.always("big_bones")).size(1.2f, 2.6f, 1.0f).at("ice_troll_cave").arena(11, "ice"));
        b.quest("the_fremennik_isles", "The Fremennik Isles", Difficulty.EXPERIENCED, 1)
                .reqQuest("fremennik_trials").req(Skill.CONSTRUCTION, 20).req(Skill.AGILITY, 40)
                .xp(Skill.CONSTRUCTION, 5000).xp(Skill.CRAFTING, 10000).xp(Skill.WOODCUTTING, 10000).reward("helm_of_neitiznot")
                .stage("I can start this quest by speaking to Mord Gunnars on the Rellekka docks. (Requires The Fremennik Trials, 20 Construction, 40 Agility.)")
                .talk("mord_gunnars").travel("neitiznot").say("To the isles? Neitiznot and Jatizso are at war with each other... and with the ice trolls. Off we go.")
                .stage("I should speak to Mawnis Burowgar, the Burgher of Neitiznot.")
                .talk("mawnis_burowgar").say("The trolls attack us both. Spy on Jatizso for me, then we'll deal with the trolls together.")
                .stage("I should visit King Gjuki Sorvott of Jatizso and learn his plans.")
                .talk("gjuki_sorvott").say("Neitiznot sent you? Ha! The ice trolls are their problem. Though... their king is a threat to us all.")
                .stage("The islands must unite against the trolls. I should slay the Ice Troll King in the caves north of the isles.")
                .kill("ice_troll_king").give("ice_troll_king_head").say("* The Ice Troll King falls! You take his head as proof.")
                .stage("I should bring the troll king's head to Mawnis Burowgar.")
                .talk("mawnis_burowgar").take("ice_troll_king_head").say("The troll king is dead! Neitiznot is in your debt. Wear this helm with pride.");
    }

    private static void throneOfMiscellania(QuestBook b) {
        b.npc("king_vargas", "King Vargas", "miscellania", 0, 3, 0xE8C0C0, 0xA03A1F, 0x5A2A1F, 0xE0C04C);
        b.npc("princess_astrid", "Princess Astrid", "miscellania", 3, -2, 0xF0D0C1, 0x8A3A9A, 0x5A2A6A, 0xE0B04D);
        b.npc("prince_brand", "Prince Brand", "miscellania", -3, -2, 0xE8C0C1, 0x3A5A9A, 0x2A3A6A, 0xE0A04E);
        b.item("ring_of_miscellania", "Royal ring", "ring", 0xE0C04F, "A ring proposing marriage, fit for royalty.");
        b.quest("throne_of_miscellania", "Throne of Miscellania", Difficulty.EXPERIENCED, 1)
                .reqQuest("heroes_quest", "fremennik_trials").req(Skill.WOODCUTTING, 45).req(Skill.FARMING, 10).req(Skill.MINING, 30).req(Skill.FISHING, 35)
                .unlock("Rule the kingdom of Miscellania (managing the kingdom)")
                .stage("I can start this quest by speaking to King Vargas in Miscellania castle. (Requires Heroes' Quest, The Fremennik Trials and more.)")
                .talk("king_vargas").say("I am old and cursed. My kingdom needs a ruler, and whoever marries my son Brand or daughter Astrid will inherit the throne.")
                .stage("I should court Prince Brand or Princess Astrid: bring gifts and a royal ring.")
                .talk("princess_astrid").take("gold_ring", "cake").give("ring_of_miscellania")
                .say("A gift for me? How charming. *After many visits and gifts, you have a royal ring made.*")
                .missing("Courting royalty requires gifts... a gold ring and something sweet, perhaps a cake.")
                .stage("I should propose to the royal with the ring, then tell King Vargas.")
                .talk("king_vargas").take("ring_of_miscellania").say("A royal wedding! You shall rule Miscellania as regent. Keep your people happy and they'll work hard for you.")
                .done("prince_brand", "My sister and I are glad Miscellania has a ruler again.");
    }

    private static void royalTrouble(QuestBook b) {
        b.npc("advisor_ghrim", "Advisor Ghrim", "miscellania", 4, 4, 0xD8A88C, 0x6A4A20, 0x3A2A20, 0xA0A0A4);
        b.boss(BossDef.of("giant_sea_snake", "Giant sea snake").level(149).stats(100, 110, 100, 90, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 16, 3, 8).special(Special.POISON, 8))
                .drops(Drop.always("bones")).size(1.2f, 1.6f, 1.0f).at("etceteria_caves").arena(11, "water"));
        b.quest("royal_trouble", "Royal Trouble", Difficulty.EXPERIENCED, 1)
                .reqQuest("throne_of_miscellania").req(Skill.AGILITY, 40).req(Skill.SLAYER, 40)
                .xp(Skill.AGILITY, 5000).xp(Skill.SLAYER, 5000).xp(Skill.HITPOINTS, 5000)
                .stage("I can start this quest by speaking to Advisor Ghrim in Miscellania castle. (Requires Throne of Miscellania, 40 Agility, 40 Slayer.)")
                .talk("advisor_ghrim").say("Villagers are vanishing from Miscellania and Etceteria! The royal families blame each other. Investigate the caves.")
                .stage("I should search the caves beneath Miscellania for the missing villagers.")
                .reach("etceteria_caves", 8).say("* In the flooded caves you find the villagers' belongings... and a huge serpent rising from the water!")
                .stage("I must kill the giant sea snake.")
                .kill("giant_sea_snake").say("* The giant sea snake is slain. The missing villagers were being held in its lair, alive.")
                .stage("I should report to Advisor Ghrim.")
                .talk("advisor_ghrim").say("The villagers are safe and the kingdoms reconciled. Your reign is secure.");
    }

    private static void coldWar(QuestBook b) {
        b.npc("larry_penguin", "Larry", "ardougne_zoo", 3, 2, 0xE8C0C2, 0x2A2A6E, 0x1A1A3E, 0x8A6A3F);
        b.npc("penguin_spy", "Penguin agent", "penguin_iceberg", 0, 2, 0x1A1A1A, 0x2A2A2A, 0xE0E0E0, 0xE0A050);
        b.item("penguin_suit", "Penguin suit", "cloth", 0x1A1A1B, "A suit that makes you look like a penguin. Mostly.");
        b.item("war_plans_penguin", "Penguin war plans", "note", 0xE0E0F0, "The penguins' plans for world domination.");
        b.quest("cold_war", "Cold War", Difficulty.INTERMEDIATE, 1)
                .req(Skill.HUNTER, 10).req(Skill.AGILITY, 30).req(Skill.CRAFTING, 30).req(Skill.CONSTRUCTION, 34).req(Skill.THIEVING, 15)
                .xp(Skill.CRAFTING, 2000).xp(Skill.AGILITY, 5000).xp(Skill.CONSTRUCTION, 1500)
                .stage("I can start this quest by speaking to Larry at Ardougne Zoo. (Requires 10 Hunter, 30 Agility, 30 Crafting, 34 Construction, 15 Thieving.)")
                .talk("larry_penguin").say("The penguins are plotting something! I've seen them spying. We need someone on the inside... in a penguin suit.")
                .stage("I should make a penguin suit (2 leather and silk) and infiltrate the penguins' iceberg.")
                .talk("larry_penguin").take("leather:2", "silk").give("penguin_suit").travel("penguin_iceberg")
                .say("*You stitch a penguin suit and Larry rows you to the iceberg.*")
                .missing("A penguin suit needs 2 leather and some silk.")
                .stage("Disguised as a penguin, I should steal the penguins' war plans.")
                .talk("penguin_spy").need("penguin_suit").give("war_plans_penguin").travel("ardougne_zoo")
                .say("Noot noot? *The 'agent' hands you top-secret plans, mistaking you for a penguin commander.*")
                .stage("I should show Larry the war plans.")
                .talk("larry_penguin").take("war_plans_penguin", "penguin_suit").say("Plans to conquer Gielinor! With a giant... ice... machine. I knew it! Gielinor owes you, agent.");
    }

    private static void towerOfLife(QuestBook b) {
        b.npc("bonafido", "Bonafido", "tower_of_life", 3, 3, 0xE0B8B0, 0xC0A050, 0x6A5A2F, 0x8A6A40);
        b.npc("effigy", "Effigy", "tower_of_life", -3, 3, 0xE8D0C2, 0x6A6AA2, 0x4A4A7C, 0xC0C0C4);
        b.item("homunculus_key", "Tower key", "key", 0x8A8A8B, "The key to the Tower of Life.");
        b.quest("tower_of_life", "Tower of Life", Difficulty.NOVICE, 2)
                .req(Skill.CONSTRUCTION, 10).xp(Skill.THIEVING, 500).xp(Skill.CRAFTING, 500).xp(Skill.CONSTRUCTION, 1000)
                .unlock("The creature creation altars beneath the Tower of Life")
                .stage("I can start this quest by speaking to Bonafido outside the Tower of Life south of Ardougne. (Requires 10 Construction.)")
                .talk("bonafido").say("The builders walked off the job! Strange things happen in that tower. Finish building it and you'll get paid.")
                .stage("I should finish building the tower: 4 planks and 2 steel bars.")
                .talk("bonafido").take("plank:4", "steel_bar:2").give("homunculus_key").say("Splendid work! Here's the key. Mind the alchemists, they're... odd.")
                .missing("I need 4 planks and 2 steel bars to finish the tower.")
                .stage("I should investigate the tower's basement, where something cries for help.")
                .reach("tower_of_life", 6).take("homunculus_key").say("* In the basement you find a homunculus, a creature made by the alchemists. You set it free.")
                .stage("I should talk to Effigy, who leads the alchemists.")
                .talk("effigy").say("You freed the homunculus? It's for the best. Use our altars wisely; they create creatures from mixed ingredients.");
    }

    private static void kingsRansom(QuestBook b) {
        b.npc("gossip_kr", "Gossip", "SEERS_VILLAGE", 6, 6, 0xE8C0C3, 0x6A3A6F, 0x4A2A4F, 0xC08A3E);
        b.npc("sir_lancelot_prisoner", "Sir Lancelot (prisoner)", "castle_wars_knights", 2, 2, 0xF0C8A9, 0x8A8A8E, 0x2A2A8B, 0xE0C061);
        b.npc("black_knight_jailer", "Black Knight jailer", "castle_wars_knights", -2, 2, 0xE0B8B1, 0x1A1A1B, 0x1A1A1C, 0x2A2A2F);
        b.item("lockpick_kr", "Lockpick", "key", 0x5A5A5A, "Useful for opening locks.");
        b.quest("kings_ransom", "King's Ransom", Difficulty.EXPERIENCED, 1)
                .reqQuest("black_knights_fortress", "holy_grail", "murder_mystery").reqQuest("one_small_favour")
                .req(Skill.MAGIC, 45).req(Skill.DEFENCE, 65)
                .xp(Skill.DEFENCE, 33000).xp(Skill.MAGIC, 5000)
                .unlock("The Chivalry and Piety prayers (Knight Waves training ground)")
                .stage("I can start this quest by speaking to Gossip in Seers' Village. (Requires Black Knights' Fortress, Holy Grail, Murder Mystery, 45 Magic, 65 Defence.)")
                .talk("gossip_kr").say("Have you heard? The Knights of the Round Table have vanished! Folk say the Black Knights took them to a prison in the north.")
                .stage("I should break into the Black Knights' prison north of Burthorpe and free the knights. The jailer carries a lockpick.")
                .talk("black_knight_jailer").stay().unless("lockpick_kr").give("lockpick_kr")
                .say("Halt! Who goes... *You tell him the Black Knight captain wants him. While he's gone you take his lockpick.*")
                .talk("sir_lancelot_prisoner").take("lockpick_kr").say("*You pick the locks on the cells.* Free! The Black Knights captured all of us. To Camelot!")
                .missing("We're locked in! The jailer carries a lockpick.")
                .stage("I should tell King Arthur his knights are free.")
                .talk("king_arthur").say("My knights are free! As a reward, I grant you entry to the Knight Waves training ground, where you can learn Chivalry and Piety.");
    }

    private static void grimTales(QuestBook b) {
        b.npc("sylas", "Sylas", "grim_tales_tower", 3, 3, 0xE0B8B2, 0x5A6A3F, 0x3A4A2F, 0x9A9A9E);
        b.item("magic_beans", "Magic beans", "seed", 0x8A5AC0, "Some magic beans.");
        b.item("griffin_feather", "Griffin feather", "mark", 0xE0C050, "A feather from a griffin.");
        b.boss(BossDef.of("glod", "Glod").level(138).stats(160, 120, 130, 100, 1, 1).speed(5)
                .attack(AttackDef.of("giant_club", AttackType.CRUSH, 25, 3, 8))
                .attack(AttackDef.of("stomp", AttackType.CRUSH, 18, 5, 2).special(Special.KNOCKBACK, 3))
                .drops(Drop.always("big_bones")).skin(0xC0A080, 0x6A4A2A, 0x4A3A1A, 0x2A1A0A).size(1.0f, 2.6f, 2.0f)
                .at("grim_tales_tower").arena(11, "stone"));
        b.quest("grim_tales", "Grim Tales", Difficulty.MASTER, 1)
                .reqQuest("witchs_house").req(Skill.FARMING, 45).req(Skill.HERBLORE, 52).req(Skill.THIEVING, 58).req(Skill.AGILITY, 59).req(Skill.WOODCUTTING, 71)
                .xp(Skill.FARMING, 4000).xp(Skill.HERBLORE, 5000).xp(Skill.HITPOINTS, 5000)
                .stage("I can start this quest by speaking to Sylas, north-west of Taverley. (Requires Witch's House and more.)")
                .talk("sylas").say("The witch Grimgnash has a griffin feather I need for my magic beans... and a giant, Glod, lives above the clouds.")
                .stage("I should get a griffin feather from Grimgnash's nest.")
                .reach("grim_tales_tower", 10).give("griffin_feather").say("* You sneak past the sleeping griffin Grimgnash and pluck a feather.")
                .stage("I should take the feather to Sylas.")
                .talk("sylas").take("griffin_feather").give("magic_beans").say("Wonderful! Plant these magic beans and climb the beanstalk to Glod's castle in the clouds.")
                .stage("I should climb the beanstalk and defeat the giant Glod.")
                .kill("glod").take("magic_beans").say("* Glod topples from his cloud castle! His golden goblin is free.")
                .stage("I should report to Sylas.")
                .talk("sylas").say("Glod defeated! You're a hero of fairy tales. Here's your reward.");
    }

    private static void handInTheSand(QuestBook b) {
        b.npc("bert_sandman", "Bert", "yanille_beach", 0, 0, 0xE0B8B3, 0x8A6A40, 0x5A4A30, 0x6A4A22);
        b.npc("wizard_mirmanir", "Wizard Mirmanir", "YANILLE", 5, 5, 0xF0D8CD, 0x6A3A9B, 0x4A2A6B, 0xE0E0EC);
        b.item("sandy_hand", "Sandy hand", "skull", 0xD0B080, "A hand, found in a sand pit. Disturbing.");
        b.item("truth_serum", "Truth serum", "potion", 0x80E0E0, "A serum that makes people tell the truth.");
        b.quest("the_hand_in_the_sand", "The Hand in the Sand", Difficulty.NOVICE, 1)
                .req(Skill.THIEVING, 17).req(Skill.CRAFTING, 49).xp(Skill.THIEVING, 1000).xp(Skill.CRAFTING, 9000)
                .unlock("Bert delivers 84 buckets of sand a day")
                .stage("I can start this quest by speaking to Bert at the Yanille sandpit. (Requires 17 Thieving, 49 Crafting.)")
                .talk("bert_sandman").give("sandy_hand").say("I found a hand in the sand pit! A wizard's hand, by the look of it. Take it to the Yanille wizards.")
                .stage("I should show the hand to Wizard Mirmanir in Yanille.")
                .talk("wizard_mirmanir").take("sandy_hand").give("truth_serum").say("That's Clarence's hand! Someone's killed him. Use this truth serum on the sandpit's guard, Sandy.")
                .stage("I should use the truth serum on Sandy, the sand company's owner.")
                .talk("bert_sandman").take("truth_serum").say("*You slip the serum into Sandy's tea.* 'I killed Clarence! He was going to stop the sand deliveries!' Sandy is arrested!")
                .stage("I should tell Bert justice was done.")
                .talk("bert_sandman").say("Sandy's in jail and I've got my job back! I'll deliver sand to you every day from now on.");
    }
}
