package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** The elf saga: Underground Pass, Regicide and Song of the Elves. */
final class ElfQuests {
    private ElfQuests() {
    }

    static void register(QuestBook b) {
        b.spot("underground_pass_entrance", "Underground Pass entrance", 2434, 3315);
        b.place("underground_pass", "Underground Pass", 2420, 3300, PlaceDef.Style.CAVE);
        b.place("ibans_temple", "Temple of Iban", 2400, 3290, PlaceDef.Style.CAVE);
        b.place("tyras_camp", "Tyras camp", 2187, 3148, PlaceDef.Style.CAMP);
        b.place("prifddinas_ruins", "Prifddinas (ruins)", 2240, 3290, PlaceDef.Style.RUINS);
        undergroundPass(b);
        regicide(b);
        songOfTheElves(b);
    }

    private static void undergroundPass(QuestBook b) {
        b.npc("koftik", "Koftik", "underground_pass_entrance", 0, 0, 0xE0B89E, 0x6A5A3A, 0x4A3A2A, 0x5A3A1A);
        b.npc("kardia", "Kardia", "underground_pass", 3, 3, 0xB0C0A1, 0x3A2A3A, 0x2A1A2A, 0x2A2A2B);
        b.npc("niloof", "Niloof", "underground_pass", -3, 3, 0xD8A882, 0x8A4A2A, 0x4A2A1A, 0xA05A1A);
        b.item("doll_of_iban", "Doll of Iban", "creature", 0x3A2A2A, "A doll in the image of the dark lord Iban.");
        b.item("ibans_shadow", "Iban's shadow", "vial", 0x1A1A1A, "A vial of darkness.");
        b.item("ibans_conscience", "Iban's conscience", "mark", 0xE8E8E8, "The remains of a dove's heart.");
        b.item("ibans_ashes", "Iban's ashes", "pot", 0x6A6A6A, "The burnt remains of Iban.");
        b.item("kalrags_venom", "Kalrag's venom", "vial", 0x5A8A2A, "Venom from the giant spider Kalrag.");
        b.boss(BossDef.of("kalrag", "Kalrag").level(89).stats(78, 80, 80, 60, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 13, 2.5, 8).special(Special.POISON, 6))
                .attack(AttackDef.of("web", AttackType.RANGED, 6, 8, 2).special(Special.FREEZE, 4).particle("minecraft:item_snowball"))
                .drops(Drop.always("bones")).size(1.6f, 1.0f, 1.0f).at("underground_pass").arena(10, "crypt"));
        b.quest("underground_pass", "Underground Pass", Difficulty.EXPERIENCED, 5)
                .reqQuest("biohazard").req(Skill.RANGED, 25)
                .xp(Skill.AGILITY, 3000).xp(Skill.ATTACK, 3000).reward("ibans_staff")
                .stage("I can start this quest by speaking to King Lathas in Ardougne Castle. (Requires Biohazard, 25 Ranged.)")
                .talk("king_lathas").say("My scouts must reach the elven lands beyond the mountains. The only way is the Underground Pass, now ruled by the dark lord Iban. Find a way through.")
                .stage("I should meet Koftik at the Underground Pass entrance west of West Ardougne, then find a way across the chasm inside.")
                .talk("koftik").stay().unlessFlag("up_koftik").flag("up_koftik").travel("underground_pass")
                .say("The pass is full of traps. Follow me, and pray to Saradomin.",
                        "The bridge over the chasm is raised. If you could cut the rope holding it up... with a burning arrow, perhaps.")
                // OSRS: an oily cloth from the abandoned equipment, wrapped round an arrow, lit and shot at the bridge rope (25 Ranged).
                .reach("underground_pass", 10).needFlag("up_koftik").need("tinderbox").take("bronze_arrow").reqSkill(Skill.RANGED, 25)
                .say("* You find an oily cloth among some abandoned equipment and wrap it around an arrow.",
                        "* You light it and fire at the rope holding the bridge. The rope burns through and the bridge crashes down across the chasm.")
                .missing("* The bridge is raised on the far side. A burning arrow could cut its rope: you'll need an arrow and a tinderbox.")
                .stage("To destroy Iban I need a doll from the witch Kardia, plus Iban's shadow, conscience and ashes... and venom from the spider Kalrag.")
                .talk("kardia").stay().unless("doll_of_iban").give("doll_of_iban").say("Iban's doll? Take it... and leave me be! Imbue it with his shadow, conscience, ashes and blood.")
                .talk("niloof").stay().unless("ibans_shadow").give("ibans_shadow", "ibans_conscience", "ibans_ashes")
                .say("The dwarves of the pass will help! Here's a vial of shadow, a dove's heart and the ashes from Iban's tomb.")
                .kill("kalrag").stay().unless("kalrags_venom").give("kalrags_venom").say("* You drain venom from Kalrag's fangs.")
                .reach("ibans_temple", 10).take("doll_of_iban", "ibans_shadow", "ibans_conscience", "ibans_ashes", "kalrags_venom")
                .say("* You imbue the doll and throw it into the Well of Voyage. Iban screams as his body crumbles!",
                        "* In his chamber you find Iban's staff.")
                .stage("Iban is destroyed. I should report to King Lathas.")
                .talk("king_lathas").say("The pass is open! My scouts can reach Tirannwn. Keep Iban's staff; few could wield it.")
                .done("koftik", "The pass is safer now. Thanks to you.");
    }

    private static void regicide(QuestBook b) {
        b.npc("arianwyn", "Arianwyn", "LLETYA", 3, 3, 0xF4E0D0, 0x3A8A3A, 0x2A5A2A, 0xE0D090);
        b.npc("tracker_elf", "Elf tracker", "LLETYA", -3, 3, 0xF4E0D1, 0x5A6A2A, 0x3A4A1A, 0xD0B060);
        b.item("barrel_bomb", "Barrel bomb", "pot", 0x6A4A2A, "A barrel packed with explosive coal tar. Handle with care.");
        b.item("regicide_barrel", "Barrel", "pot", 0x8A6A3A, "An empty barrel from General Hining's camp.");
        b.item("barrel_of_coal_tar", "Barrel of coal tar", "pot", 0x2A2A2A, "A barrel of sticky coal tar.");
        b.item("barrel_of_naphtha", "Barrel of naphtha", "pot", 0x6A5A2A, "Distilled from coal tar. Highly flammable.");
        b.item("regicide_sulphur", "Ground sulphur", "spice", 0xE0D040, "Sulphur from beside the tar pit.");
        b.item("regicide_book", "Book on chemicals", "book", 0x5A3A2A, "Lord Iorwerth's book on making explosives.");
        b.spot("regicide_tar_pit", "Tar pit near the Elf Tracker", 2210, 3162); // approx
        Cast.npc(b, "general_hining", "General Hining", "tyras_camp", 3, 3, Cast.FAIR, 0x8A1A1A);
        // OSRS: the player never fights King Tyras (the barrel bomb kills him); the level-110 fight is a Tyras guard in the
        // dense forest. The id stays "king_tyras" so saves and the creature model keep working.
        b.boss(BossDef.of("king_tyras", "Tyras guard").level(110).stats(110, 85, 95, 100, 1, 0).speed(5) // DPS calculator
                .defenceBonus(87, 84, 76, -10, 79)
                .attack(AttackDef.of("halberd", AttackType.STAB, 15, 3, 10))
                .drops(Drop.always("bones")).skin(0xE8C0AD, 0xC0C0C8, 0x8A1A1A, 0xE0C030).at("tyras_camp").arena(10, "jungle"));
        b.quest("regicide", "Regicide", Difficulty.EXPERIENCED, 3)
                .reqQuest("underground_pass").req(Skill.AGILITY, 56).req(Skill.CRAFTING, 10)
                .xp(Skill.AGILITY, 13750).reward("coins:15000") // wiki: 15,000 coins
                .unlock("Access to Tirannwn and Lletya")
                .stage("I can start this quest by speaking to King Lathas in Ardougne Castle. (Requires Underground Pass, 56 Agility, 10 Crafting.)")
                .talk("king_lathas").travel("LLETYA").say("My brother Tyras leads an army in Tirannwn. He must be stopped. Go through the pass, find the elves and destroy his camp.",
                        "* You travel through the Underground Pass, down Iban's well and out into the elven lands.")
                // OSRS: Lord Iorwerth, then the Elf Tracker, whose tracks lead through the dense forest to the Tyras camp.
                .stage("I should speak to Lord Iorwerth and his Elf Tracker in Tirannwn.")
                .talk("lord_iorwerth").stay().unlessFlag("reg_iorwerth").flag("reg_iorwerth")
                .say("Lathas sent you? Tyras's army ravages our forests. My tracker can lead you to his camp, through the dense forest.")
                .talk("tracker_elf").needFlag("reg_iorwerth")
                .say("Tyras' scouts came this way. *The tracker points out the tracks north-west of the blue mushrooms.* Follow them into the forest; mind the tripwires.")
                .missing("Lord Iorwerth hasn't told me to help you, human.")
                .stage("I should follow the tracks through the dense forest to the Tyras camp and speak to General Hining.")
                .kill("king_tyras").stay().unlessFlag("reg_guard").flag("reg_guard")
                .say("* The Tyras guard who ambushed you in the dense forest falls.")
                .talk("general_hining").needFlag("reg_guard").give("regicide_barrel:2")
                .say("A visitor who got past the forest? The king can't see anyone. Make yourself useful and shift some of these barrels.")
                .missing("* A Tyras guard bars the way through the dense forest.")
                .stage("I need a barrel bomb for the camp's catapult: coal tar from the tar pit by the tracker, then the chemist in Rimmington can distil naphtha.")
                .reach("regicide_tar_pit", 6).stay().unless("barrel_of_coal_tar", "barrel_bomb").take("regicide_barrel").give("barrel_of_coal_tar", "regicide_sulphur")
                .say("* You fill a barrel with coal tar and pick up a lump of sulphur beside the pit.")
                .talk("lord_iorwerth").stay().unless("regicide_book", "barrel_bomb").give("regicide_book")
                .say("Coal tar, sulphur, quicklime... and a fuse. This book on chemicals explains it. The chemist in Rimmington has a still.")
                .talk("chemist").need("regicide_book", "pestle_and_mortar").take("barrel_of_coal_tar", "coal:20", "regicide_sulphur", "limestone", "strip_of_cloth")
                .give("barrel_bomb", "barrel_of_naphtha")
                .say("* You distil the coal tar over 20 coal in the fractionalising still: naphtha! You burn limestone to quicklime and grind it and the sulphur.",
                        "* Naphtha, quicklime and sulphur in a barrel, a strip of cloth for a fuse: a barrel bomb. You distil a spare barrel of naphtha too.")
                .missing("You'll need the coal tar, 20 coal, ground sulphur, limestone for quicklime, a strip of cloth for a fuse, a pestle and mortar and that chemicals book.")
                .stage("I should take the barrel bomb back through the pass and use it on the catapult north of the Tyras camp.")
                .reach("tyras_camp", 10).take("barrel_bomb", "minecraft:cooked_rabbit").need("tinderbox")
                .say("* You hand the catapult guard a cooked rabbit. While he eats, you load the barrel bomb and light the fuse...",
                        "* The bomb sails into the camp and explodes on King Tyras' tent!")
                .missing("* The catapult guard won't let you near. Perhaps something to eat would distract him: a cooked rabbit. And a tinderbox for the fuse.")
                .stage("I should tell Lord Iorwerth, then report to King Lathas.")
                .talk("lord_iorwerth").stay().unlessFlag("reg_reported").flag("reg_reported")
                .say("Tyras is dead, and the way to the east is open. Lathas will be pleased... more than you know.")
                .talk("king_lathas").needFlag("reg_reported")
                .say("My brother is dead? Then Tirannwn is open to my armies. You may enter Lletya freely; the elves owe you that much.")
                .missing("Have you dealt with my brother? Speak to Lord Iorwerth first.");
    }

    private static void songOfTheElves(QuestBook b) {
        b.npc("lord_iorwerth", "Lord Iorwerth", "prifddinas_ruins", 3, 3, 0xF4E0D2, 0x2A2A2A, 0x1A1A1A, 0xA0A0A0);
        b.npc("baxtorian_elf", "Baxtorian", "prifddinas_ruins", -3, 3, 0xF4E0D3, 0x3A8AA0, 0x2A5A6A, 0xE0E0E2);
        // quest-helper tiles: Ysgawyn on Lletya's upper platform, the four elf lords the player finds, the Mysterious Figure in the
        // Enchanted Valley (Lord Amlodd)
        b.npcAt("ysgawyn", "Ysgawyn", 2335, 3167, 0xF4E0D4, 0x2A2A2A, 0x1A1A1A, 0x6A6A6A);
        b.npcAt("lady_tangwen_trahaearn", "Lady Tangwen Trahaearn", 2334, 3174, 0xF4E0D5, 0x8A8A8A, 0x5A5A5A, 0xC0C0C0); // approx: in the Underground Pass (2334, 9574)
        b.npcAt("lady_hefin", "Lady Hefin", 2276, 3094, 0xF4E0D6, 0x3A8A3A, 0x2A5A2A, 0xE0D090);
        b.npcAt("lord_crwys", "Lord Crwys", 2177, 3210, 0xF4E0D7, 0x6A4A2A, 0x4A3A1A, 0x6A5A2A);
        b.item("ardougne_knight_helm", "Ardougne knight helm", "helmet", 0xA02A2A, "A red steel full helm of the Ardougne knights.");
        b.item("ardougne_knight_platebody", "Ardougne knight platebody", "platebody", 0x8A8A9A, "A steel platebody with the Ardougne tabard.");
        b.item("crystal_seed_sote", "Crystal seed", "gem", 0xB8F0FF, "The seed of the great crystal of Prifddinas.");
        b.item("sote_thick_liquid", "Thick clear liquid", "vial", 0xE0E0E8, "From Elena's cabinet. A sleeping draught, of sorts.");
        b.item("sote_enchanted_seed", "Enchanted crystal seed", "gem", 0x9AE0F0, "Baxtorian's crystal seed, enchanted by Eluned.");
        b.item("sote_inversion_potion", "Inversion potion", "potion", 0xB0E0F0, "Elder cadantine and crystal dust: it reverses an elf's corruption.");
        b.item("sote_orb_of_light", "Orb of light", "orb", 0xF0F0C0, "The orb the Iorwerth archers stole.");
        b.item("sote_explosive_potion", "Explosive potion", "potion", 0xE06A2A, "Elena's explosive potion, for the holes outside the dwarf camp.");
        // Spots (quest-helper tiles): Elena's house, the Rehnison house, the three grain sacks in West Ardougne, the market, the castle,
        // the herb patch in Lletya, the Underground Pass crevice, Isafdar's clearing, the Grand Library, the dwarf camp.
        b.spot("sote_elena_house", "Elena's house", 2590, 3338);
        b.spot("sote_rehnison", "The Rehnison household", 2533, 3333);
        b.spot("sote_grain_north", "Grain sacks by the clerk", 2517, 3315);
        b.spot("sote_grain_church", "Grain sacks in the church", 2524, 3285);
        b.spot("sote_grain_store", "Grain sacks by the general store", 2469, 3287);
        b.spot("sote_ardougne_market", "Ardougne Market", 2662, 3305);
        b.spot("sote_ardougne_castle", "Ardougne Castle", 2585, 3297);
        b.spot("sote_baxtorian_falls", "Baxtorian's tomb", 2566, 3505); // approx: the Waterfall Dungeon's throne room (2603, 9905)
        b.spot("sote_lletya_patch", "Lletya herb patch", 2322, 3152);
        b.spot("sote_up_crevice", "Underground Pass crevice", 2341, 3216); // approx: south of the Well of Voyage (2341, 9616)
        b.spot("sote_grand_library", "The Grand Library", 2240, 3300); // approx: the library lies beneath the ruins (2623, 6088)
        b.spot("sote_ysgawyn_clearing", "Isafdar clearing", 2256, 3168);
        b.spot("sote_lletya_statue", "Lletya's empty plinth", 2340, 3165); // approx: the centre of the village
        b.spot("sote_dig_swamp", "South Isafdar swamp", 2225, 3133);
        b.spot("sote_dig_zulandra", "North of Zul-Andra", 2203, 3061); // quest-helper tile
        b.spot("sote_dig_gates", "Outside Prifddinas' south gates", 2240, 3269); // quest-helper tile
        b.spot("sote_spin", "Western entry to the Underground Pass", 2309, 3215); // quest-helper tile
        // OSRS Wiki "Iorwerth Warrior": level 108, 105 HP, 95 Attack/Strength, 80 Defence, stab max hit 10 every 4 ticks.
        b.spot("sote_iorwerth_cave", "Iorwerth Camp cave", 2202, 3263); // the cave on the north side of the Iorwerth Camp (quest-helper tile)
        b.boss(BossDef.of("iorwerth_warrior", "Iorwerth Warrior").level(108).stats(105, 95, 95, 80, 1, 1).speed(4)
                .defenceBonus(50, 70, 70, 60, 50)
                .attack(AttackDef.of("stab", AttackType.STAB, 10, 2, 1))
                .drops(Drop.always("bones")).model("elf_warrior").at("sote_iorwerth_cave").arena(7, "jungle"));
        // The battles of West and East Ardougne (wiki): level-106 mourners (100 HP, stab up to 10) and the castle's level-121
        // paladins (82 HP, slash up to 14) and level-53 Knights of Ardougne (54 HP, slash up to 6), every 4-5 ticks.
        b.boss(BossDef.of("sote_mourner", "Mourner").level(106).stats(100, 95, 95, 80, 1, 1).speed(4)
                .defenceBonus(40, 60, 60, 50, 40)
                .attack(AttackDef.of("stab", AttackType.STAB, 10, 2, 1))
                .drops(Drop.always("bones")).skin(0xE0C0A0, 0x2A2A2A, 0x1A1A1A, 0x3A3A3A));
        b.boss(BossDef.of("sote_knight", "Knight of Ardougne").level(53).stats(54, 44, 52, 28, 1, 1).speed(4)
                .defenceBonus(20, 40, 40, 30, 20)
                .attack(AttackDef.of("slash", AttackType.SLASH, 6, 2, 1))
                .drops(Drop.always("bones")).skin(0xE0C0A0, 0x8A8A9A, 0x5A5A6A, 0xA02A2A));
        b.boss(BossDef.of("sote_paladin", "Paladin").level(121).stats(82, 54, 126, 64, 1, 1).speed(5)
                .defenceBonus(87, 84, 76, -10, 79)
                .attack(AttackDef.of("slash", AttackType.SLASH, 14, 2, 1))
                .drops(Drop.always("bones")).skin(0xE0C0A0, 0xC0C0C9, 0x8A8A9A, 0xE0E0E3));
        // Arianwyn turned traitor (wiki): level 212, 300 HP, ranged up to 38 every 5 ticks; purple arrows switch prayers off,
        // orange arrows land as bombs (up to 47).
        b.boss(BossDef.of("sote_arianwyn", "Arianwyn").level(212).stats(300, 0, 0, 102, 102, 368).speed(5)
                .defenceBonus(80, 80, 80, 260, 180)
                .attack(AttackDef.of("arrows", AttackType.RANGED, 38, 12, 6))
                .attack(AttackDef.of("purple_arrow", AttackType.RANGED, 38, 12, 1).special(Special.DRAIN_PRAYER, 99)
                        .message("A purple arrow switches your prayers off!").particle("minecraft:witch"))
                .attack(AttackDef.of("orange_arrow", AttackType.RANGED, 47, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(2, "Arianwyn looses a glowing orange arrow!").particle("minecraft:flame"))
                .drops(Drop.always("bones")).skin(0xF4E0D0, 0x3A8A3A, 0x2A5A2A, 0xE0D090));
        // Fragment of Seren and Essyllt: OSRS Wiki infoboxes (levels, hitpoints, combat stats, max hits, attack speed). Essyllt stabs
        // up to 40 and shoots up to 51, stuns and throws a stat-draining potion.
        b.boss(BossDef.of("sote_essyllt", "Essyllt").level(236).stats(320, 268, 268, 104, 104, 342).speed(4)
                .defenceBonus(40, 40, 20, 30, 120)
                .attack(AttackDef.of("sword", AttackType.STAB, 40, 2.5, 5))
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 51, 10, 5))
                .attack(AttackDef.of("stun", AttackType.STAB, 20, 2.5, 1).special(Special.FREEZE, 3).message("Essyllt stuns you!")) // approx
                .attack(AttackDef.of("potion", AttackType.RANGED, 10, 10, 1).special(Special.DRAIN_STAT, 0)
                        .message("Essyllt hurls a potion: you feel weaker.").particle("minecraft:item_slime")) // approx
                .drops(Drop.always("bones")).skin(0xF4E0D0, 0x1A1A1A, 0x1A1A1A, 0x2A2A2A).at("underground_pass").arena(10, "crypt"));
        // The Fragment (wiki): 1,000 HP, every 7 ticks two crystals of up to 12 each (3 through Protect from Missiles) or a melee
        // swipe; every fourth attack one of her four specials in order (QuestBossScripts.FragmentSpecials): she pulls the player in,
        // splits into shadows, summons healing beams, then pins the player for a hit of their max hitpoints minus 1-5. Magic is the
        // only effective style (Defence-based magic defence: her magic defence bonus is 10).
        b.boss(BossDef.of("fragment_of_seren", "Fragment of Seren").level(494).stats(1000, 242, 288, 102, 102, 235).speed(7)
                .defenceBonus(320, 220, 320, 10, 480)
                .attack(AttackDef.of("crystal_volley", AttackType.RANGED, 24, 10, 5)) // two hits of up to 12
                .attack(AttackDef.of("swipe", AttackType.SLASH, 24, 2.5, 2))
                .attack(AttackDef.of("pull", AttackType.SLASH, 24, 12, 1).special(Special.PULL, 0)
                        .telegraph(2, "Fragment of Seren: Come closer!"))
                .attack(AttackDef.of("shadows", AttackType.MAGIC, 0, 12, 1).special(Special.BLINK, 0)
                        .message("The Fragment splits into shadows! Strike the lighter one."))
                .attack(AttackDef.of("healing_beams", AttackType.MAGIC, 0, 12, 1).special(Special.HEAL_SELF, 30)
                        .message("Crystal beams surround the Fragment, healing her!").particle("minecraft:end_rod"))
                .attack(AttackDef.of("crystal_judgement", AttackType.MAGIC, 98, 12, 1).special(Special.FREEZE, 5)
                        .telegraph(4, "The Fragment pins you in place and gathers her power! Heal above your maximum!")
                        .particle("minecraft:end_rod"))
                .drops(Drop.always("coins")).size(0.8f, 2.4f, 1.3f).at("prifddinas_ruins").arena(14, "void"));
        b.quest("song_of_the_elves", "Song of the Elves", Difficulty.GRANDMASTER, 4)
                .xp(Skill.AGILITY, 40000).xp(Skill.CONSTRUCTION, 40000).xp(Skill.FARMING, 40000).xp(Skill.HERBLORE, 40000).xp(Skill.HUNTER, 40000).xp(Skill.MINING, 40000).xp(Skill.SMITHING, 40000).xp(Skill.WOODCUTTING, 40000) // OSRS (current quest XP rewards)
                .reqQuest("regicide", "plague_city").reqQuest("mournings_end_part_ii", "making_history", "DRUIDIC_RITUAL")
                .req(Skill.AGILITY, 70).req(Skill.CONSTRUCTION, 70).req(Skill.FARMING, 70).req(Skill.HERBLORE, 70)
                .req(Skill.HUNTER, 70).req(Skill.MINING, 70).req(Skill.SMITHING, 70).req(Skill.WOODCUTTING, 70)
                .unlock("Access to Prifddinas, the elven city").unlock("Access to Zalcano and the Gauntlet")
                .unlock("Ability to make divine potions and crystal tools, weapons and armour")
                .stage("I can start this quest by speaking to Edmond in East Ardougne. (Requires Regicide and the Mourning's End quests; 70 in eight skills.)")
                .talk("edmond").say("Elena has been arrested by the king's knights for stirring up trouble against the mourners! Please, help her.")
                // ---- The escape of Elena, Causing an insurrection (wiki quick guide) ----
                .stage("I should free Elena from Ardougne Castle's cells, stir both halves of Ardougne against King Lathas and lead the "
                        + "battles against his mourners and knights.")
                .talk("king_lathas").stay().unlessFlag("sote_lathas").flag("sote_lathas")
                .say("Elena is a troublemaker. She stays where she is. Now leave, I have a war with Iorwerth to fight.")
                .talk("edmond").stay().needFlag("sote_lathas").unlessFlag("sote_disguise_brief").flag("sote_disguise_brief")
                .say("> How do I get this disguise?", "Dye a steel full helm red, and Alrena can sew you the knights' tabard for a steel platebody.")
                .talk("alrena").stay().needFlag("sote_disguise_brief").unless("ardougne_knight_helm")
                .take("steel_full_helm", "steel_platebody", "redberries:3", "silk").give("ardougne_knight_helm", "ardougne_knight_platebody")
                .say("* You dye the steel full helm red.", "A tabard of the king's knights, sewn from your silk... there. On the platebody it goes.")
                .missing("To pass as one of the king's knights you need a steel full helm and red dye (redberries), and silk for the tabard on a steel platebody.")
                .talk("elena").stay().needFlag("sote_lathas").unlessFlag("sote_cell").need("ardougne_knight_helm", "ardougne_knight_platebody", "steel_platelegs")
                .flag("sote_cell")
                .say("* The guards let a fellow knight through.",
                        "You came! In my house, in the cabinet, there's a thick clear liquid. Bring it to me: I have a plan.")
                .missing("* The prison guards turn you away. Perhaps dressed as one of the king's knights (helm, platebody and steel platelegs)...")
                .reach("sote_elena_house", 3).stay().needFlag("sote_cell").unlessFlag("sote_escape").unless("sote_thick_liquid").give("sote_thick_liquid")
                .say("* In the cabinet of Elena's house you find the thick clear liquid.")
                .talk("elena").stay().needFlag("sote_cell").unlessFlag("sote_escape").take("sote_thick_liquid").flag("sote_escape")
                .say("* Elena drinks the liquid and drops as if dead. The guards carry her out of the castle.")
                .talk("edmond").stay().needFlag("sote_escape").unlessFlag("sote_hideout").flag("sote_hideout")
                .say("She's out! She's waiting for you in the Rehnison house in West Ardougne. There's a passage under the bed.")
                .reach("sote_rehnison", 4).stay().needFlag("sote_hideout").unlessFlag("sote_halgrive").flag("sote_halgrive")
                .say("* You climb through the bed into the hideout. Elena and Councillor Halgrive have a plan:",
                        "* burn the mourners' grain and spread word of new taxes, so both halves of the city rise against the king.")
                .reach("sote_grain_north", 3).stay().needFlag("sote_halgrive").unlessFlag("sote_grain1").need("tinderbox", "me_mourner_top")
                .flag("sote_grain1").say("* Dressed as a mourner, you set the grain sacks by the clerk alight.")
                .missing("* In mourner gear and with a tinderbox you could burn the mourners' grain.")
                .reach("sote_grain_church", 3).stay().needFlag("sote_halgrive").unlessFlag("sote_grain2").need("tinderbox", "me_mourner_top")
                .flag("sote_grain2").say("* You burn the sacks in the church. The priest and Nurse Sarah hear of the 'mourners' new taxes'.")
                .reach("sote_grain_store", 3).stay().needFlag("sote_halgrive").unlessFlag("sote_grain3").need("tinderbox", "me_mourner_top")
                .flag("sote_grain3").say("* The sacks outside the general store go up in flames. Chadwell is furious about the new taxes.")
                .reach("sote_ardougne_market", 8).stay().needFlag("sote_grain1", "sote_grain2", "sote_grain3").unlessFlag("sote_taxes")
                .need("ardougne_knight_helm", "ardougne_knight_platebody").flag("sote_taxes")
                .say("* As a knight of Ardougne you tell the silver merchant, the bakers, the gem merchant, the fur trader, the spice seller,",
                        "* the silk merchant, the town crier, Zenesha, the estate agent, Probita, Aemad and the priest about the king's new taxes.")
                .missing("* The market traders would listen to a knight of Ardougne.")
                .talk("elena").stay().needFlag("sote_taxes").unlessFlag("sote_battle_w").flag("sote_battle_w")
                .say("> I'm ready.", "Then the Battle of West Ardougne begins. To the mourners' headquarters!")
                .reach("west_ardougne_mourners", 8).stay().needFlag("sote_battle_w").unlessFlag("sote_west").spawn("sote_mourner")
                .say("* Mourners pour out of their headquarters!")
                .kill("sote_mourner", 21).stay().needFlag("sote_battle_w").unlessFlag("sote_west").flag("sote_west")
                .say("* Ten mourners at the headquarters and eleven in its basement fall. West Ardougne is free.")
                .talk("elena").stay().needFlag("sote_west").unlessFlag("sote_battle_e").flag("sote_battle_e")
                .say("> I'm ready.", "Now the castle. The Battle of East Ardougne!")
                .reach("sote_ardougne_castle", 8).stay().needFlag("sote_battle_e").unlessFlag("sote_knights").spawn("sote_knight")
                .say("* Knights of Ardougne block the castle halls!")
                .kill("sote_knight", 10).stay().needFlag("sote_battle_e").unlessFlag("sote_knights").flag("sote_knights")
                .say("* The knights fall back. The paladins make their stand upstairs.") // approx: the wiki's 15 knights and paladins split 10/5
                .reach("sote_ardougne_castle", 8).stay().needFlag("sote_knights").unlessFlag("sote_paladins").spawn("sote_paladin")
                .say("* A paladin bars the stairs!")
                .kill("sote_paladin", 5).stay().needFlag("sote_knights").unlessFlag("sote_paladins").flag("sote_paladins")
                .say("* The last paladin falls. The castle is yours.")
                .talk("king_lathas").needFlag("sote_paladins")
                .say("> We're done here, take him away.", "* Lathas is led away. Arianwyn arrives with news, and you travel with him to Lletya.")
                .missing("Guards! *The king's knights still hold the castle.*")
                // ---- Arianwyn's plan, Baxtorian Falls, Preparing to enter Prifddinas, the Grand Library, the elf lords, the orb,
                // Ysgawyn and the attack on Lletya (wiki quick guide) ----
                .stage("I should help Arianwyn and Baxtorian in Lletya: free Baxtorian's memories, gather the elf lords, open the Grand "
                        + "Library's seals, recover the orb and unmask the traitor.")
                .talk("arianwyn").stay().unlessFlag("sote_arianwyn").flag("sote_arianwyn")
                .say("The time has come to restore Prifddinas. Speak to Ysgawyn, and Elena, then join our meeting.")
                .talk("ysgawyn").stay().needFlag("sote_arianwyn").unlessFlag("sote_ysgawyn").flag("sote_ysgawyn")
                .say("Humans in Lletya... Arianwyn trusts too easily.")
                .talk("elena").stay().needFlag("sote_ysgawyn").unlessFlag("sote_elena_lletya").flag("sote_elena_lletya")
                .say("Lletya is beautiful. Arianwyn's meeting is about to begin.")
                .talk("arianwyn").stay().needFlag("sote_elena_lletya").unlessFlag("sote_meeting").flag("sote_meeting")
                .say("Baxtorian's memories lie in his tomb behind the waterfall. Meet me there.")
                .puzzle("sote_baxtorian_falls", SOTE_PILLARS).stay().needFlag("sote_meeting").unlessFlag("sote_pillars").need("rope")
                .take("nature_rune", "irit_leaf", "black_knife", "wine_of_zamorak", "adamant_chainbody", "cabbage").flag("sote_pillars")
                .say("* With the key from the crates and the 'Ode to eternity', the six pillars take their offerings. Baxtorian rises!")
                .reach("sote_lletya_patch", 3).stay().needFlag("sote_pillars").unlessFlag("sote_planted").take("cadantine_seed").need("seed_dibber")
                .flag("sote_planted").say("* At Elena's suggestion you plant a cadantine seed in the herb patch by the pond.")
                .talk("baxtorian_elf").stay().needFlag("sote_pillars").unlessFlag("sote_clues").give("crystal_seed_sote").flag("sote_clues")
                .say("> How do I find Lord Amlodd?", "With this crystal seed, once it is enchanted.",
                        "> How do I find Lady Trahaearn?", "She went into the Underground Pass, long ago. Look south of the Well of Voyage.")
                .talk("eluned").stay().needFlag("sote_clues").unless("sote_enchanted_seed").unlessFlag("sote_amlodd").take("crystal_seed_sote")
                .give("sote_enchanted_seed")
                .say("> I have a seed from Baxtorian that I think needs enchanting.", "*Eluned sings to the seed.* There.")
                .reach("sote_up_crevice", 4).stay().needFlag("sote_clues").unlessFlag("sote_crevice", "sote_trahaearn").needTool("pickaxe")
                .take("runite_bar").need("hammer").flag("sote_crevice")
                .say("* You break through the blocked crevice. An elderly elf lies trapped in a broken exoskeleton; you mend it with a runite bar.")
                .missing("* A crevice south of the Well of Voyage, blocked by rock: a pickaxe. Beyond it, something needs a hammer and a runite bar.")
                .talk("lady_tangwen_trahaearn").stay().needFlag("sote_crevice").unlessFlag("sote_trahaearn").flag("sote_trahaearn")
                .say("Baxtorian sent you? Then the Trahaearn clan will answer him.")
                .reach("LLETYA", 12).stay().needFlag("sote_trahaearn").unlessFlag("sote_amlodd").take("sote_enchanted_seed")
                .flag("sote_amlodd")
                .say("* You rub the crystal seed and it carries you to the Enchanted Valley. A mysterious figure reveals himself:",
                        "* Lord Ieuan Amlodd, who returns with you to Lletya.")
                .missing("* The enchanted crystal seed would carry you to Lord Amlodd.")
                .talk("arianwyn").stay().needFlag("sote_amlodd", "sote_planted").unlessFlag("sote_tonic").take("cadantine", "vial_of_water", "crystal_shard")
                .need("pestle_and_mortar").reqSkill(Skill.HERBLORE, 70).give("sote_inversion_potion").flag("sote_tonic")
                .say("* Elena hands you an elder cadantine from the patch; Arianwyn an old crystal. You grind it to dust and mix both in a vial of water.",
                        "An inversion potion. With this, Lord Amlodd can open the Grand Library.")
                .missing("The inversion potion needs the elder cadantine (a cadantine), crystal dust from a crystal shard and a vial of water, "
                        + "and a pestle and mortar.")
                .puzzle("sote_grand_library", SOTE_SEAL_CADARN).stay().needFlag("sote_tonic").unlessFlag("sote_library")
                .take("sote_inversion_potion").flag("sote_library")
                .say("* Lord Amlodd takes you into the Grand Library. Beam by beam the seals open; Cadarn's memory speaks of a fragment sealed away.")
                .talk("baxtorian_elf").stay().needFlag("sote_library").unlessFlag("sote_lords").flag("sote_lords")
                .say("> Lady Hefin?", "Past the stepping stones of the Poison Waste.", "> Lord Crwys?", "In a dead tree south of the Iorwerth Camp.")
                .talk("lady_hefin").stay().needFlag("sote_lords").unlessFlag("sote_hefin").flag("sote_hefin")
                .say("* Across the Poison Waste's stepping stones an elf hermit waits.", "I am Hefin. Tell Baxtorian my clan will come.")
                .reach("at_lord_crwys", 4).stay().needFlag("sote_lords").unlessFlag("sote_crwys_tree", "sote_crwys").needTool("axe")
                .flag("sote_crwys_tree").say("* You chop the dead tree north of the pond. An elf steps out of it.")
                .missing("* The dead tree north of the pond looks odd. An axe would tell.")
                .talk("lord_crwys").stay().needFlag("sote_crwys_tree").unlessFlag("sote_crwys").flag("sote_crwys")
                .say("Crwys, at your service. The Crwys clan stands with Baxtorian.")
                .kill("iorwerth_archer").stay().needFlag("sote_lords").unlessFlag("sote_archer", "sote_orb").flag("sote_archer")
                .say("* The Iorwerth archer guarding the cave falls. His warrior companion is still inside.")
                .kill("iorwerth_warrior").stay().needFlag("sote_archer").unlessFlag("sote_orb").flag("sote_orb").give("sote_orb_of_light")
                .say("* The Iorwerth warrior falls. In the chest you find the stolen orb of light.")
                .talk("baxtorian_elf").stay().needFlag("sote_orb", "sote_hefin", "sote_crwys").unlessFlag("sote_traitor_hunt").take("sote_orb_of_light")
                .flag("sote_traitor_hunt")
                .say("The orb! Someone told the Iorwerths where it was. Elena saw Ysgawyn slip out of Lletya. Follow him.")
                .reach("sote_ysgawyn_clearing", 6).stay().needFlag("sote_traitor_hunt").unlessFlag("sote_traitor").flag("sote_traitor")
                .say("* Mushrooms, rocks and lamps: his footsteps lead to a clearing in Isafdar. Ysgawyn is meeting Iorwerth's men!",
                        "* Smoke rises over Lletya. The Iorwerths are attacking!")
                .reach("LLETYA", 10).stay().needFlag("sote_traitor").unlessFlag("sote_arianwyn_dead").spawn("sote_arianwyn")
                .say("* You find Eoin under a table and Iona among the cooking pots. Then Arianwyn draws his bow on you: he was the traitor!")
                .kill("sote_arianwyn").stay().needFlag("sote_traitor").unlessFlag("sote_arianwyn_dead").flag("sote_arianwyn_dead")
                .say("* Near death, Arianwyn animates an arrow... and falls.")
                .reach("sote_lletya_statue", 4).stay().needFlag("sote_arianwyn_dead").unlessFlag("sote_ithell").take("limestone_brick:8")
                .need("hammer").flag("sote_ithell")
                .say("* The five glowing symbols lead you to the empty plinth. You build the statue from limestone bricks: Lady Ithell appears.")
                .missing("* Lady Ithell's statue needs 8 limestone bricks and a hammer.")
                .reach("sote_dig_swamp", 3).stay().needFlag("sote_ithell").unlessFlag("sote_clue1").need("spade").flag("sote_clue1")
                .say("* You dig by the swamp in the south of Isafdar: the next clue.")
                .reach("sote_dig_zulandra", 3).stay().needFlag("sote_clue1").unlessFlag("sote_clue2").need("spade").flag("sote_clue2")
                .say("* You dig north of Zul-Andra: another clue.")
                .reach("sote_dig_gates", 3).stay().needFlag("sote_clue2").unlessFlag("sote_clue3").need("spade").flag("sote_clue3")
                .say("* You dig outside Prifddinas' south gates: the last clue.")
                .reach("sote_spin", 3).stay().needFlag("sote_clue3").unlessFlag("sote_meilyr").emptyHanded().flag("sote_meilyr")
                .say("* With nothing in your hands you spin outside the Underground Pass. Lady Meilyr appears: every clan stands with Baxtorian.")
                .missing("* Lady Meilyr's clue: spin here with nothing equipped.")
                .talk("baxtorian_elf").needFlag("sote_meilyr")
                .say("All the clans are united. Now we must seal the Underground Pass before Iorwerth's forces reach the Temple of Light.")
                .missing("?!sote_meeting|Arianwyn will tell you of our plans.", "?sote_meeting|?!sote_meilyr|The clans are not yet united.")
                // ---- Sealing the Underground Pass and Fighting the Fragment (wiki quick guide) ----
                .stage("I must seal the Underground Pass at the dwarf camp, defeat Essyllt and destroy the corrupted Fragment of Seren.")
                .talk("elena").stay().unlessFlag("sote_holes").unless("sote_explosive_potion").give("sote_explosive_potion:7")
                .say("> Baxtorian said you had an idea to help our defence.", "Explosive potions! Seven of them, for the holes outside the camp.")
                .reach("underground_pass", 10).stay().unlessFlag("sote_holes").take("sote_explosive_potion:7").flag("sote_holes")
                .say("* You fill the seven holes outside the dwarf camp with explosive potions.")
                .kill("elf_warrior", 6).stay().needFlag("sote_holes").unlessFlag("sote_defended").flag("sote_defended") // approx: 4 minutes of attackers
                .say("* For four minutes you hold the barricade against Iorwerth's elves. The explosives seal the pass behind them.")
                .kill("sote_essyllt").stay().needFlag("sote_defended").unlessFlag("sote_essyllt").flag("sote_essyllt")
                .say("* Essyllt, Iorwerth's agent among the mourners, falls. The Underground Pass is sealed behind you.")
                .kill("fragment_of_seren").needFlag("sote_essyllt").give("crystal_seed_sote")
                .say("* In the Temple of Light, the Fragment of Seren falls silent. A crystal seed lies where she stood.")
                .missing("?!sote_holes|* The holes outside the dwarf camp are still empty.", "?sote_holes|?!sote_essyllt|* Essyllt still leads the attack.")
                .stage("I should give the crystal seed to Baxtorian so Prifddinas can be rebuilt.")
                .talk("baxtorian_elf").take("crystal_seed_sote").say("* Baxtorian sings to the seed. Crystal towers rise from the ruins: Prifddinas is restored!",
                        "The elven city welcomes you, hero of Prifddinas.")
                .done("arianwyn", "Welcome to Prifddinas, friend.");
    }

    /**
     * Song of the Elves, Baxtorian's tomb (wiki quick guide): six pillars, each with a number from 1 to 6 different for every
     * player, take the offerings 1 nature rune, 2 irit leaf (or flowers), 3 black knife, 4 wine of zamorak, 5 adamant chainbody,
     * 6 cabbage. The inscriptions: SW says its number; W what it isn't opposite (the SE pillar's); NW what it is next to (the W
     * pillar's); NE what it is opposite (the NW pillar's); E what it isn't next to (its own); SE what it isn't (the NE pillar's).
     */
    static final QuestPuzzle SOTE_PILLARS = sotePillars();
    /**
     * The Grand Library, the Seal of Cadarn (wiki quick guide's solution): seven hand mirrors and a red crystal guide the beam
     * east, down, south, west, south, through the red crystal, east and south to the seal.
     */
    static final QuestPuzzle SOTE_SEAL_CADARN = soteSealCadarn();

    private static QuestPuzzle sotePillars() {
        String[] items = {"Nature rune", "Irit leaf", "Black knife", "Wine of zamorak", "Adamant chainbody", "Cabbage"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("sote_pillars").room("pillars");
        // pillars in order SW, W, NW, NE, E, SE; a few dozen of the 720 numberings are enough to vary between players
        int[][] numberings = {{1, 2, 3, 4, 5, 6}, {3, 6, 1, 5, 2, 4}, {5, 1, 4, 2, 6, 3}, {2, 4, 6, 3, 1, 5}, {6, 3, 5, 1, 4, 2},
                {4, 5, 2, 6, 3, 1}, {2, 6, 4, 1, 5, 3}, {5, 3, 1, 6, 2, 4}, {1, 4, 5, 3, 6, 2}, {6, 1, 2, 5, 4, 3},
                {3, 5, 6, 2, 1, 4}, {4, 2, 3, 6, 5, 1}};
        for (int[] n : numberings) {
            b.variant(null).prompt("* Six pillars stand in Baxtorian's tomb. Their inscriptions read:",
                            "* South-west: 'I am " + n[0] + ".' West: 'I am not opposite " + n[5] + ".' North-west: 'I am next to " + n[1] + ".'",
                            "* North-east: 'I am opposite " + n[2] + ".' East: 'I am not next to " + n[4] + ".' South-east: 'I am not " + n[3] + ".'",
                            "* The Ode to eternity: 1 nature, 2 flowers, 3 a black blade, 4 Zamorak's wine, 5 adamant links, 6 cabbage.",
                            "* Make the offerings on the pillars in order: SW, W, NW, NE, E, SE.")
                    .options(items).solution(n[0] - 1, n[1] - 1, n[2] - 1, n[3] - 1, n[4] - 1, n[5] - 1);
        }
        return b.failLines("* The pillar rejects the offering. You take everything back and start again.").build();
    }

    private static QuestPuzzle soteSealCadarn() {
        return QuestPuzzle.builder("sote_seal_cadarn").room("cadarn").variant(null)
                .prompt("* The Seal of Cadarn: a beam of light enters the room. Seven hand mirrors and a red crystal from the dispenser.",
                        "* Mirror 1 stands west of mirror 2; mirror 2 over a hole down to mirror 3; mirror 4 south of mirror 3; mirror 5 west",
                        "* of mirror 4; the crystal pillar south of mirror 5; mirror 6 beyond it, mirror 7 east of mirror 6 and the seal south of 7.",
                        "* Place the mirrors and the crystal, in order, to carry the beam to the seal.")
                .options("North", "East", "South", "West", "Up", "Down", "Red crystal")
                .solution(1, 5, 2, 3, 2, 6, 1, 2)
                .failLines("* The beam scatters against the wall. You collect the mirrors and start again.").build();
    }
}
