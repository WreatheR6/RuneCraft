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
        b.place("grim_tales_tower", "Witch's tower", 2968, 3466, PlaceDef.Style.RUINS); // the tower south of the Mind Altar
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
        // star_flower and gorak_claws live in CommonItems (Herblore makes magic essence from them).
        b.item("queens_secateurs", "Queen's secateurs", "knife", 0xE0C047, "The Fairy Queen's secateurs.");
        b.boss(BossDef.of("tanglefoot", "Tanglefoot").level(111).stats(102, 90, 90, 90, 1, 1).speed(5)
                .attack(AttackDef.of("vine_whip", AttackType.CRUSH, 15, 3, 7))
                .attack(AttackDef.of("entangle", AttackType.MAGIC, 8, 8, 3).special(Special.FREEZE, 5).particle("minecraft:composter"))
                .requiresWeapon("magic_secateurs")
                .drops(Drop.always("logs")).size(1.0f, 2.4f, 1.0f).at("tanglefoot_lair").arena(10, "jungle"));
        b.boss(BossDef.of("gorak", "Gorak").level(145).stats(112, 120, 120, 110, 1, 1).speed(4)
                .attack(AttackDef.of("claw", AttackType.SLASH, 16, 2.5, 10).special(Special.DRAIN_STAT, Skill.HITPOINTS.ordinal()))
                .drops(Drop.always("gorak_claws")).skin(0x6A3A6B, 0x4A2A4A, 0x3A1A3A, 0x8A4A8A).at("gorak_plane").arena(9, "void"));
        b.item("draynor_skull", "Draynor skull", "skull", 0xE0D8C0, "A skull from the grounds of Draynor Manor.");
        b.spot("draynor_manor_skull", "North of Draynor Manor", 3106, 3383);
        b.quest("fairytale_i_growing_pains", "Fairytale I - Growing Pains", Difficulty.INTERMEDIATE, 2)
                .reqQuest("lost_city", "nature_spirit")
                .xp(Skill.FARMING, 3500).xp(Skill.ATTACK, 2000).xp(Skill.MAGIC, 1000).reward("magic_secateurs")
                .stage("I can start this quest by speaking to Martin the Master Gardener in Draynor Village. (Requires Lost City, Nature Spirit.)")
                .talk("martin_the_master_gardener").say("All my crops are dying! Something in Zanaris is to blame, I just know it. Ask the fairies.")
                .stage("I should ask the Fairy Godfather in Zanaris about the dying crops, then find a way to fight Tanglefoot.")
                .talk("fairy_godfather").stay().unlessFlag("ft1_godfather").flag("ft1_godfather")
                .say("The Fairy Queen is... resting, and Tanglefoot, her guardian plant, has run amok. Only magic secateurs can cut it.",
                        "Ask that necromancer, Malignius Mortifer, west of Port Sarim. He knows about such things.")
                // Quest-helper: dig for the skull north of Draynor Manor (3106, 3383); the Nature Spirit enchants the secateurs.
                .reach("draynor_manor_skull", 2).stay().needFlag("ft1_godfather").need("spade").unless("draynor_skull").unlessFlag("ft1_malignius")
                .give("draynor_skull").say("* You dig in the earth north of Draynor Manor and unearth an old skull.")
                .talk("malignius_mortifer").stay().needFlag("ft1_godfather").unlessFlag("ft1_malignius").take("draynor_skull").flag("ft1_malignius")
                .say("A Draynor skull! Good. Fine, I'll tell you: take ordinary secateurs to the Nature Spirit in Mort Myre's grotto. She can enchant them.")
                .missing("Tanglefoot? Bring me a skull from the grounds of Draynor Manor and we'll talk.")
                .talk("nature_spirit").needFlag("ft1_malignius").take("secateurs").give("magic_secateurs")
                .say("Secateurs, for the fairies' sake? *She blesses them.* These will cut through Tanglefoot's vines.")
                .missing("Bring me a pair of secateurs and I will bless them.")
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
                .talk("fairy_godfather").reqSkill(Skill.THIEVING, 40).give("queens_secateurs")
                .missing("Hmph. What do you want? *His pockets look well guarded.* (Requires 40 Thieving.)").say("*While the Godfather gloats, you swipe the secateurs from his pocket.*")
                .stage("I should return the secateurs to the Fairy Queen.")
                .talk("fairy_queen").take("queens_secateurs").say("My power returns! The fairy rings are yours to use, friend of the fairies.")
                .done("fairy_nuff", "The Queen is herself again, thanks to you.");
    }

    private static void gardenOfTranquillity(QuestBook b) {
        b.npc("queen_ellamaria", "Queen Ellamaria", "queens_garden", 0, 0, 0xF0D0C0, 0xC03A8A, 0x8A2A6A, 0xE0C048);
        // OSRS walkthrough: the Wise Old Man charges the ring of charos; six gardeners trade rare seeds for favours; two
        // statues are wheeled to the garden on a trolley; everything is planted in the Queen's garden for King Roald.
        b.spot("got_lyra", "Lyra's allotments, west of Port Phasmatys", 3601, 3525);
        b.spot("got_kragen", "Kragen's allotments, north of Ardougne", 2666, 3374);
        b.spot("got_elstan", "Elstan's patches, south of Falador", 3053, 3308);
        b.spot("got_dantaera", "Dantaera's allotments, Catherby", 2809, 3463);
        b.spot("got_white_tree", "White Tree on Ice Mountain", 3006, 3497); // approx
        b.spot("got_edgeville_well", "Edgeville well", 3084, 3501); // approx
        b.spot("got_monastery_garden", "Edgeville Monastery rose garden", 3054, 3503); // approx
        b.spot("got_burthorpe_vines", "Vines behind the Burthorpe pub", 2921, 3534); // approx
        b.spot("got_lumbridge_statue", "Statue of a king, Lumbridge", 3222, 3206); // approx
        b.spot("got_falador_statue", "Saradomin statue, Falador square", 2965, 3382); // approx
        Cast.npc(b, "got_brother_althric", "Brother Althric", "got_monastery_garden", 2, 2, Cast.PALE, 0x6A4A2A);
        Cast.npc(b, "got_bernald", "Bernald", "got_burthorpe_vines", 2, 2, Cast.FAIR, 0x4A6A3A);
        b.item("got_orchid_seed", "Orchid seed", "seed", 0xC080C0, "A rare orchid seed from Lyra.");
        b.item("got_snowdrop_seed", "Snowdrop seed", "seed", 0xE0E0F0, "A snowdrop seed from Kragen.");
        b.item("got_delphinium_seed", "Delphinium seed", "seed", 0x4A6AD0, "A delphinium seed from Elstan.");
        b.item("got_white_tree_shoot", "White tree shoot", "plant_pot", 0xE0E0E3, "A cutting from the White Tree, potted and watered.");
        b.item("got_rose_seed", "Rose seed", "seed", 0xD03A4A, "Seeds from the red, pink and white roses of the monastery.");
        b.item("got_vine_seed", "Vine seed", "seed", 0x4A8A3A, "A seed from Bernald's vines.");
        b.item("got_trolley", "Trolley", "statue", 0x8A6A3A, "A trolley for moving heavy statues.");
        b.quest("garden_of_tranquillity", "Garden of Tranquillity", Difficulty.INTERMEDIATE, 2)
                .reqQuest("creature_of_fenkenstrain").req(Skill.FARMING, 25).xp(Skill.FARMING, 5000)
                .reward("apple_seed", "acorn", "guam_seed:5", "compost_potion")
                .unlock("The activated ring of charos; the Queen's garden and its white tree fruit")
                .stage("I can start this quest by speaking to Queen Ellamaria in Varrock Palace garden. (Requires Creature of Fenkenstrain, 25 Farming.)")
                .talk("queen_ellamaria").need("ring_of_charos")
                .say("I want a garden fit for my husband - a garden of peace and serenity. Rare plants, statues... and not a word to Rollie. Um, King Roald.",
                        "You wear a ring of charos? Have the Wise Old Man in Draynor wake it. The gardeners of Gielinor will be far more agreeable.")
                .missing("You'll need a way to charm the gardeners of Gielinor. A ring of charos, perhaps? (Creature of Fenkenstrain.)")
                .stage("I need orchids, snowdrops, delphiniums, a white tree, roses and vines from the gardeners, two statues, and the activated ring of charos.")
                .talk("quest_wise_old_man").stay().need("ring_of_charos").unlessFlag("got_ring").flag("got_ring")
                .say("A ring of charos! Answer my questions well and I'll wake it for you. ... Show them a range of colours... Ask me nicely...",
                        "* The ring glows faintly. It is now an activated ring of charos.")
                .reach("got_lyra", 6).stay().need("ring_of_charos").needFlag("got_ring").unless("got_orchid_seed").take("onion_seed:3").give("got_orchid_seed:2")
                .say("* Lyra, charmed by the ring, has you grow a crop of onions in her allotment. Once they're picked she hands over orchid seeds.")
                .missing("* Lyra wants a crop of onions grown first. Bring 3 onion seeds (and your activated ring of charos).")
                .reach("got_kragen", 6).stay().need("ring_of_charos").needFlag("got_ring").unless("got_snowdrop_seed").take("cabbage_seed:3").give("got_snowdrop_seed:2")
                .say("* Kragen grumbles about his cabbages until you grow him a crop. He pays you in snowdrop seeds.")
                .missing("* Kragen wants a crop of cabbages grown first. Bring 3 cabbage seeds.")
                .reach("got_elstan", 6).stay().need("ring_of_charos").needFlag("got_ring").unless("got_delphinium_seed").take("marigold_seed").give("got_delphinium_seed:2")
                .say("* You grow marigolds in Elstan's flower patch and give them to him. He gives you delphinium seeds.")
                .missing("* Elstan wants marigolds grown in his flower patch. Bring a marigold seed.")
                .reach("got_dantaera", 6).stay().need("ring_of_charos").needFlag("got_ring").unless("got_white_tree_shoot").unlessFlag("got_dantaera").flag("got_dantaera")
                .say("* Dantaera tells you of the White Tree on Ice Mountain. Take a cutting with secateurs and keep it in a plant pot.")
                .reach("got_white_tree", 6).stay().needFlag("got_dantaera").unless("got_white_tree_shoot").need("secateurs", "watering_can")
                .take("filled_plant_pot").give("got_white_tree_shoot")
                .say("* You take a cutting from the White Tree with your secateurs, pot it and water it.")
                .missing("* You need secateurs, a watering can and a filled plant pot to take a cutting of the White Tree.")
                .talk("got_brother_althric").stay().need("ring_of_charos", "fishing_rod").needFlag("got_ring").unless("got_rose_seed").give("got_rose_seed:3")
                .say("My roses? Only if you can... oh, how charming you are. Very well.",
                        "* At Edgeville you throw the ring down the well as a test of faith, take seeds from the red, pink and white roses,",
                        "* and fish your ring back out with your fishing rod.")
                .missing("Faith, my child, faith. Bring a fishing rod - you'll see why.")
                .talk("got_bernald").stay().need("ring_of_charos").needFlag("got_ring").unless("got_vine_seed").take("plant_cure:2", "rune_essence").need("pestle_and_mortar", "hammer")
                .give("got_vine_seed:2")
                .say("My vines are dying! Plant cure didn't work... Alain at the Taverley tree patch says to add rune dust.",
                        "* You crush rune essence into dust, mix it into the second plant cure and save the vines. Bernald gives you vine seeds.")
                .missing("My vines need two plant cures, and some rune essence crushed with a hammer and pestle and mortar.")
                .talk("queen_ellamaria").stay().need("ring_of_charos").needFlag("got_ring").unless("got_trolley").unlessFlag("got_statue_lumbridge", "got_statue_falador").give("got_trolley")
                .say("Statues! The king of Lumbridge's statue and Falador's Saradomin. Here's a trolley. Do be discreet.")
                .reach("got_lumbridge_statue", 4).stay().need("got_trolley").unlessFlag("got_statue_lumbridge").flag("got_statue_lumbridge")
                .say("* You load the statue of a king onto the trolley and push it east over the bridge to the eastern plinth in the garden.")
                .reach("got_falador_statue", 4).stay().need("got_trolley").unlessFlag("got_statue_falador").flag("got_statue_falador")
                .say("* You load the Saradomin statue and push it north through the gate. A player-killer jumps the guards; nobody notices you.",
                        "* The statue goes on the southern plinth.")
                .talk("queen_ellamaria").needFlag("got_statue_lumbridge", "got_statue_falador")
                .take("got_orchid_seed:2", "got_snowdrop_seed:2", "got_delphinium_seed:2", "got_white_tree_shoot", "got_rose_seed:3", "got_vine_seed:2", "compost:2")
                .need("ring_of_charos").needFlag("got_ring").flag("got_garden")
                .say("* You fill two plant pots with compost and plant everything. In a few minutes the garden is in full bloom.",
                        "It's perfect! Now, fetch Rollie... the King. Wear that ring, or he'll never come.")
                .missing("I still need the orchids, snowdrops, delphiniums, roses, vines and white tree, two buckets of compost, and both statues.")
                .stage("I should fetch King Roald to see the garden, wearing the activated ring of charos.")
                .talk("king_roald").needFlag("got_garden").need("ring_of_charos").needFlag("got_ring")
                .say("A garden? Now? Oh, very well... *In the garden, the King falls silent.* Ellamaria, it's beautiful. Thank you, adventurer.")
                .missing("I'm far too busy for gardens. *Perhaps the activated ring of charos would make him more agreeable.*");
    }

    private static void pathOfGlouphrie(QuestBook b) {
        b.npc("longramble", "Longramble", "glouphrie_tunnels", 2, 2, 0xD0A073, 0x3A6A3D, 0x2A4A2E, 0x8A6A3E);
        b.boss(BossDef.of("warped_terrorbird", "Warped terrorbird").level(138).stats(150, 110, 110, 100, 100, 1).speed(4)
                .attack(AttackDef.of("peck", AttackType.STAB, 18, 2.5, 6))
                .attack(AttackDef.of("warp_bolt", AttackType.MAGIC, 16, 8, 4).particle("minecraft:portal"))
                .drops(Drop.always("bones")).size(1.0f, 2.0f, 1.0f).at("glouphrie_tunnels").arena(10, "void"));
        b.quest("the_path_of_glouphrie", "The Path of Glouphrie", Difficulty.EXPERIENCED, 2)
                .xp(Skill.STRENGTH, 30000).xp(Skill.SLAYER, 20000).xp(Skill.THIEVING, 5000).xp(Skill.MAGIC, 5000) // OSRS (current quest XP rewards)
                .reqQuest("the_eyes_of_glouphrie", "waterfall_quest", "tree_gnome_village")
                .req(Skill.STRENGTH, 60).req(Skill.SLAYER, 56).req(Skill.THIEVING, 56).req(Skill.RANGED, 47).req(Skill.AGILITY, 45)
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
        b.npc("vermundi", "Vermundi", "keldagrim", 6, 4, 0xD8A88C, 0x8A3A6A, 0x5A2A4A, 0xC08A3F);
        b.npc("dromund", "Dromund", "keldagrim", -6, 4, 0xD8A88D, 0x5A4A2A, 0x3A2A1A, 0x8A6A40);
        b.npc("santiri", "Santiri", "keldagrim", -6, -4, 0xD8A88E, 0x6A6A6A, 0x3A3A3A, 0xA0A0A1);
        b.npc("quest_gd_secretary", "Purple Pewter secretary", "keldagrim", 4, 6, 0xD8A88F, 0x6A2A8A, 0x4A1A5A, 0xC0A0E0);
        b.npc("quest_gd_director", "Purple Pewter director", "keldagrim", -4, 6, 0xD8A890, 0x5A1A7A, 0x3A0A4A, 0xE0C062);
        b.item("exquisite_clothes", "Exquisite clothes", "cloth", 0xC0A0E0, "Clothes made of exquisite silk.");
        b.item("exquisite_boots", "Exquisite boots", "boots", 0x8A5A3A, "A pair of very fine boots.");
        b.item("gd_dwarven_battleaxe_broken", "Dwarven battleaxe", "battleaxe", 0x6A6A6A, "An old dwarven battleaxe. It needs repairing.");
        b.item("gd_dwarven_battleaxe", "Dwarven battleaxe", "battleaxe", 0x9A9AB0, "A fine dwarven battleaxe, set with sapphires.");
        b.quest("the_giant_dwarf", "The Giant Dwarf", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.CRAFTING, 2500).xp(Skill.MINING, 2500).xp(Skill.SMITHING, 2500).xp(Skill.MAGIC, 1500).xp(Skill.FIREMAKING, 1500).xp(Skill.THIEVING, 1500) // OSRS (current quest XP rewards)
                .req(Skill.CRAFTING, 12).req(Skill.FIREMAKING, 16).req(Skill.MAGIC, 33).req(Skill.THIEVING, 14)
                .unlock("Access to Keldagrim and the Grand Exchange of the dwarves")
                .stage("I can start this quest by speaking to Commander Veldaban in Keldagrim. (Requires 12 Crafting, 16 Firemaking, 33 Magic, 14 Thieving.)")
                .talk("commander_veldaban").say("A human in Keldagrim? You knocked over the statue of King Alvis on your way in! You'll need to repair it... somehow.")
                // OSRS: Blasidar's model needs King Alvis's costume: exquisite clothes from Vermundi, Dromund's boots and Santiri's axe.
                .stage("I should help Blasidar the sculptor: his model, Riki, needs exquisite clothes, exquisite boots and a dwarven battleaxe.")
                .talk("vermundi").stay().unless("exquisite_clothes").take("coins:200", "coal", "logs").need("tinderbox").give("exquisite_clothes")
                .say("Exquisite clothes? My spinning machine's broken... *You load coal and logs into it and light it with your tinderbox.*",
                        "It's working again! For you, the clothes are 200 coins.")
                .missing("The spinning machine needs coal and logs, and a tinderbox to light it. Then the clothes are 200 coins.")
                .talk("dromund").stay().unless("exquisite_boots").reqSkill(Skill.THIEVING, 14).reqSkill(Skill.MAGIC, 33).give("exquisite_boots")
                .say("My boots? Never! *While he isn't looking you take the left boot, then Telekinetic Grab the right one through the window.*")
                .missing("*You'd need 14 Thieving to snatch one boot and 33 Magic to telegrab the other.*")
                .talk("santiri").stay().unless("gd_dwarven_battleaxe_broken", "gd_dwarven_battleaxe").take("sapphire:3").give("gd_dwarven_battleaxe_broken")
                .say("King Alvis's axe? This one's close. *You set three sapphires in it.* It's damaged though; only Thurgo the Imcando could mend it.")
                .missing("The axe for the statue needs three sapphires.")
                .talk("thurgo").stay().unless("gd_dwarven_battleaxe").take("gd_dwarven_battleaxe_broken", "iron_bar").give("gd_dwarven_battleaxe")
                .say("An old dwarven axe? *Thurgo hammers an iron bar into the blade.* Good as new.")
                .talk("riki_the_sculptor").take("exquisite_clothes", "exquisite_boots", "gd_dwarven_battleaxe")
                .say("*Riki dresses as King Alvis and Blasidar sculpts the new statue.* It's even better than the original!")
                .missing("Blasidar needs me in King Alvis's clothes, his boots and his axe.")
                // OSRS: ore deliveries for a company's secretary, then bars for its director (any company but the Red Axe). // approx: quantities
                .stage("I should join one of the dwarven trading companies (ores for the secretary, bars for the director), then report to Veldaban.")
                .talk("quest_gd_secretary").stay().unlessFlag("gd_ores").take("iron_ore:5", "coal:5").flag("gd_ores")
                .say("Ores! Just what the Purple Pewter needs. Keep this up and the director will want to meet you.")
                .missing("Our company needs ores: 5 iron ore and 5 coal, unnoted.")
                .talk("quest_gd_director").stay().needFlag("gd_ores").unlessFlag("gd_joined").take("steel_bar:5").flag("gd_joined")
                .say("Bars too? You've worked hard for us. Blasidar sent you? Then join the Purple Pewter, and support the Commander.")
                .missing("Bring me bars and we'll talk. 5 steel bars.")
                .talk("commander_veldaban").needFlag("gd_joined").give("dwarven_lore_book")
                .say("The statue's fixed and you've joined a company! Welcome to Keldagrim, friend. Take this book of our lore.")
                .missing("Have you joined a company yet? Talk to the secretaries upstairs in the market.");
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
        b.npc("slug_hemligssen", "Slug Hemligssen", "neitiznot", -3, 3, 0xE0B8B0, 0x2A2A2A, 0x1A1A1A, 0x3A3A3A);
        b.item("jester_costume", "Jester costume", "cloth", 0xC03AA0, "A jester's hat, top, tights and boots.");
        b.quest("the_fremennik_isles", "The Fremennik Isles", Difficulty.EXPERIENCED, 1)
                .reqQuest("fremennik_trials").req(Skill.CONSTRUCTION, 20) // 40 Agility only for the optional mine shortcut
                .xp(Skill.CONSTRUCTION, 5000).xp(Skill.CRAFTING, 5000).xp(Skill.WOODCUTTING, 10000).reward("helm_of_neitiznot")
                // Plus two 10,000 XP rewards in Attack, Strength, Defence or Hitpoints (player's choice). // approx: fixed skills
                .xp(Skill.STRENGTH, 10000).xp(Skill.DEFENCE, 10000)
                .stage("I can start this quest by speaking to Mord Gunnars on the Rellekka docks. (Requires The Fremennik Trials, 20 Construction, 40 Agility.)")
                .talk("mord_gunnars").travel("neitiznot").say("To the isles? Neitiznot and Jatizso are at war with each other... and with the ice trolls. Off we go.")
                .stage("I should speak to Mawnis Burowgar, the Burgher of Neitiznot.")
                .talk("mawnis_burowgar").say("The trolls attack us both. Spy on Jatizso for me, then we'll deal with the trolls together.")
                .stage("I should get a jester's disguise from Slug Hemligssen, Neitiznot's spymaster, and learn King Gjuki Sorvott's plans on Jatizso.")
                .talk("slug_hemligssen").stay().unless("jester_costume").give("jester_costume")
                .say("A spy? Then you'll need a disguise. The king of Jatizso is looking for a new jester. Wear this and juggle well.")
                .talk("gjuki_sorvott").need("jester_costume")
                .say("* Dressed as a jester, you juggle for the king and listen in.",
                        "Neitiznot? Ha! The ice trolls are their problem. Though... their king is a threat to us all.")
                .missing("Who let you in? Guards! *You'll need a disguise to get close to the king.*")
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
                .reqNote("One of: 45 Woodcutting, 10 Farming, 30 Mining or 35 Fishing (to manage the kingdom)")
                .reqQuest("heroes_quest", "fremennik_trials")
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
        b.npc("kr_mansion_guard", "Guard", "sinclair_mansion", 4, -4, 0xE0B8B2, 0x3A3A6A, 0x2A2A4A, 0x8A8A8F);
        b.npc("kr_anna", "Anna", "SEERS_VILLAGE", -6, 6, 0xF0C8AA, 0x8A2A3A, 0x5A1A2A, 0xC08A40);
        b.item("kr_scrap_paper", "Scrap paper", "note", 0xE0D8C0, "A scrap of paper from the Sinclair Mansion.");
        b.item("kr_address_form", "Address form", "note", 0xE0D8C1, "A form with an address on it.");
        b.item("kr_black_knight_helm", "Black knight helm", "full_helm", 0x2A2A2A, "A helm worn by the Black Knights.");
        b.quest("kings_ransom", "King's Ransom", Difficulty.EXPERIENCED, 1)
                .reqQuest("black_knights_fortress", "holy_grail", "murder_mystery").reqQuest("one_small_favour")
                .req(Skill.MAGIC, 45).req(Skill.DEFENCE, 65)
                .xp(Skill.DEFENCE, 33000).xp(Skill.MAGIC, 5000)
                .unlock("The Chivalry and Piety prayers (Knight Waves training ground)")
                .stage("I can start this quest by speaking to Gossip in Seers' Village. (Requires Black Knights' Fortress, Holy Grail, Murder Mystery, 45 Magic, 65 Defence.)")
                .talk("gossip_kr").say("Have you heard? The Knights of the Round Table have vanished! Folk say the Black Knights took them to a prison in the north.")
                // OSRS: evidence from the Sinclair Mansion for the guard, then Anna's trial at the Seers' Village courthouse.
                .stage("I should find evidence in the Sinclair Mansion for the guard, then prove Anna innocent at her trial in the Seers' Village courthouse.")
                .reach("sinclair_mansion", 6).stay().unless("kr_address_form").unlessFlag("kr_guard").give("kr_scrap_paper", "kr_address_form", "kr_black_knight_helm")
                .say("* You climb in through the smashed window. By the dining-room fireplace you find a scrap of paper,",
                        "* in the library an address form, and in the western bookcase a Black Knight helm.")
                .talk("kr_mansion_guard").stay().unlessFlag("kr_guard").take("kr_scrap_paper", "kr_address_form", "kr_black_knight_helm").flag("kr_guard")
                .say("A Black Knight helm in the Sinclair house? And a forwarding address... Anna is being framed. Speak up for her at the trial.")
                .missing("I can't help Anna without evidence. Search the mansion.")
                .talk("kr_anna").needFlag("kr_guard")
                .say("* You call the dog handler (the poison), the butler (the dagger) and the maid (the night of the murder and the thread).",
                        "Not guilty! Thank you. But the Black Knights did this... and the Knights of the Round Table have vanished. Search the statue east of Camelot.")
                .missing("My trial is about to start, and I've nobody to speak for me.")
                // OSRS: the cell under the statue with Merlin, the lock puzzle, Cromperty's animate rock scroll and Arthur's statue.
                .stage("I should escape the cell with Merlin, get an animate rock scroll from Wizard Cromperty and free King Arthur in the Black Knights' Fortress.")
                .talk("black_knight_jailer").stay().unless("lockpick_kr").unlessFlag("kr_escaped").reqSkill(Skill.MAGIC, 33).give("lockpick_kr")
                .say("*From the vent you Telekinetic Grab the hair clip of the guard fixing his hair outside the cell.*")
                .missing("*You'd need 33 Magic to telegrab the guard's hair clip.*")
                .talk("sir_lancelot_prisoner").stay().unlessFlag("kr_escaped").take("lockpick_kr").flag("kr_escaped")
                .say("*You set the four tumblers and the lock springs open.* Free! King Arthur was turned to stone in the Black Knights' Fortress.",
                        "Merlin says only an animate rock scroll and some granite can undo it. Take the bronze helm and chainbody for the King.")
                .missing("We're locked in! The guard outside has a hair clip.")
                .talk("wizard_cromperty").stay().needFlag("kr_escaped").unless("quest_animate_rock_scroll").give("quest_animate_rock_scroll")
                .say("An animate rock scroll for a stone king? Here you are. Mind you read it next to him.")
                .reach("black_knights_fortress", 8).stay().needFlag("kr_escaped").unlessFlag("kr_arthur")
                .need("black_full_helm", "black_platebody", "black_platelegs")
                .take("quest_animate_rock_scroll", "granite_500g", "bronze_med_helm", "iron_chainbody").flag("kr_arthur")
                .say("* Disguised in black armour you go through the sturdy door, push the wall and climb down to the statue of King Arthur.",
                        "* You read the scroll over the granite: the stone cracks and the King stands free. You give him the helm and chainbody.")
                .missing("* The Black Knights would stop me: I need black armour (full helm, platebody, platelegs),",
                        "* and the animate rock scroll, granite, a bronze med helm and an iron chainbody for the King.")
                .talk("king_arthur").needFlag("kr_arthur")
                .say("My knights are free! As a reward, I grant you entry to the Knight Waves training ground, where you can learn Chivalry and Piety.")
                .missing("I am still a statue in the Black Knights' Fortress, as far as Camelot knows.");
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
                .xp(Skill.WOODCUTTING, 60000).xp(Skill.AGILITY, 25000).xp(Skill.THIEVING, 25000).xp(Skill.HERBLORE, 15000).xp(Skill.FARMING, 10000).xp(Skill.HITPOINTS, 5000) // OSRS (current quest XP rewards)
                .reward("dwarven_helmet")
                .reqQuest("witchs_house").req(Skill.FARMING, 45).req(Skill.HERBLORE, 52).req(Skill.THIEVING, 58).req(Skill.AGILITY, 59).req(Skill.WOODCUTTING, 71)
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
        b.quest("the_hand_in_the_sand", "The Hand in the Sand", Difficulty.INTERMEDIATE, 1)
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
