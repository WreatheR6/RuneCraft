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
        b.item("ardougne_knight_helm", "Ardougne knight helm", "helmet", 0xA02A2A, "A red steel full helm of the Ardougne knights.");
        b.item("ardougne_knight_platebody", "Ardougne knight platebody", "platebody", 0x8A8A9A, "A steel platebody with the Ardougne tabard.");
        b.item("crystal_seed_sote", "Crystal seed", "gem", 0xB8F0FF, "The seed of the great crystal of Prifddinas.");
        // The cave on the north side of the Iorwerth Camp, where an archer and a warrior guard the stolen orb.
        b.spot("sote_iorwerth_cave", "Iorwerth Camp cave", 2201, 3262); // approx
        // OSRS Wiki "Iorwerth Warrior": level 108, 105 HP, 95 Attack/Strength, 80 Defence, stab max hit 10 every 4 ticks.
        b.boss(BossDef.of("iorwerth_warrior", "Iorwerth Warrior").level(108).stats(105, 95, 95, 80, 1, 1).speed(4)
                .defenceBonus(50, 70, 70, 60, 50)
                .attack(AttackDef.of("stab", AttackType.STAB, 10, 2, 1))
                .drops(Drop.always("bones")).model("elf_warrior").at("sote_iorwerth_cave").arena(7, "jungle"));
        // Fragment of Seren and Essyllt: OSRS Wiki infoboxes (levels, hitpoints, combat stats, max hits, attack speed).
        b.boss(BossDef.of("sote_essyllt", "Essyllt").level(236).stats(320, 268, 268, 104, 104, 342).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 40, 2.5, 5))
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 51, 10, 5))
                .drops(Drop.always("bones")).skin(0xF4E0D0, 0x1A1A1A, 0x1A1A1A, 0x2A2A2A).at("west_ardougne_mourners").arena(10, "crypt"));
        b.boss(BossDef.of("fragment_of_seren", "Fragment of Seren").level(494).stats(1000, 242, 288, 102, 102, 235).speed(7)
                .defenceBonus(100, 100, 100, 100, 100) // approx
                .attack(AttackDef.of("crystal_volley", AttackType.RANGED, 24, 10, 5)) // two hits of up to 12
                .attack(AttackDef.of("crystal_spike", AttackType.MAGIC, 40, 10, 3).special(Special.SKY_STRIKE, 0) // approx: OSRS special hits max HP minus 1
                        .telegraph(3, "Crystals rain down from the sky!").particle("minecraft:end_rod"))
                .attack(AttackDef.of("fragment_drain", AttackType.MAGIC, 20, 10, 2).special(Special.DRAIN_PRAYER, 15))
                .phase(66, "The Fragment of Seren shatters into crystals and reforms!", 1.2, false)
                .phase(33, "Seren's fragment sings a terrible song!", 1.4, false)
                .drops(Drop.always("coins")).size(0.8f, 2.4f, 1.3f).at("prifddinas_ruins").arena(14, "void"));
        b.quest("song_of_the_elves", "Song of the Elves", Difficulty.GRANDMASTER, 4)
                .xp(Skill.AGILITY, 40000).xp(Skill.CONSTRUCTION, 40000).xp(Skill.FARMING, 40000).xp(Skill.HERBLORE, 40000).xp(Skill.HUNTER, 40000).xp(Skill.MINING, 40000).xp(Skill.SMITHING, 40000).xp(Skill.WOODCUTTING, 40000) // OSRS (current quest XP rewards)
                .reqQuest("regicide", "plague_city").reqQuest("mournings_end_part_ii", "making_history", "DRUIDIC_RITUAL")
                .req(Skill.AGILITY, 70).req(Skill.CONSTRUCTION, 70).req(Skill.FARMING, 70).req(Skill.HERBLORE, 70)
                .req(Skill.HUNTER, 70).req(Skill.MINING, 70).req(Skill.SMITHING, 70).req(Skill.WOODCUTTING, 70)
                .unlock("Access to Prifddinas, the elven city")
                .stage("I can start this quest by speaking to Edmond in East Ardougne. (Requires Regicide and the Mourning's End quests; 70 in eight skills.)")
                .talk("edmond").say("Elena has been arrested by the king's knights for stirring up trouble against the mourners! Please, help her.")
                .stage("I should ask King Lathas about Elena, disguise myself as an Ardougne knight (Alrena can help) and speak to Elena in her cell.")
                .talk("king_lathas").stay().unlessFlag("sote_lathas").flag("sote_lathas")
                .say("Elena is a troublemaker. She stays where she is. Now leave, I have a war with Iorwerth to fight.")
                .talk("alrena").stay().needFlag("sote_lathas").unless("ardougne_knight_helm")
                .take("steel_full_helm", "steel_platebody", "redberries:3").give("ardougne_knight_helm", "ardougne_knight_platebody")
                .say("A knight's disguise? Dye a steel full helm red and sew the Ardougne tabard onto a steel platebody... there.")
                .missing("To pass as one of the king's knights you need a steel full helm, a steel platebody and redberries for dye.")
                .talk("elena").needFlag("sote_lathas").need("ardougne_knight_helm", "ardougne_knight_platebody")
                .say("* The guards let a fellow knight through.",
                        "You came! Lathas has been fighting Iorwerth in secret all along. Get word to the elves in Lletya: Ardougne will stand with them.")
                .missing("* The prison guards turn you away. Perhaps dressed as one of the king's knights...")
                .stage("I should travel to Lletya, help Baxtorian prepare (he needs a crystal tonic: cadantine and crystal dust in water) "
                        + "and confront Lord Iorwerth.")
                .talk("arianwyn").stay().unlessFlag("sote_arianwyn").flag("sote_arianwyn")
                .say("The time has come to restore Prifddinas. But Lord Iorwerth's forces still hold the ruins. Baxtorian has returned to lead us.")
                .talk("baxtorian_elf").stay().needFlag("sote_arianwyn").unlessFlag("sote_tonic")
                .take("cadantine", "vial_of_water", "crystal_shard").need("pestle_and_mortar").reqSkill(Skill.HERBLORE, 70).flag("sote_tonic")
                .say("* You grind the crystal shard to dust and mix it with cadantine in a vial of water.",
                        "A crystal tonic! With this I can sing to the crystal again. Now, to Iorwerth.")
                .missing("I need a tonic to sing to the crystal: cadantine, a crystal shard and a vial of water, ground and mixed with a pestle and mortar.")
                .kill("iorwerth_archer").stay().needFlag("sote_arianwyn").unlessFlag("sote_archer", "sote_orb").flag("sote_archer")
                .say("* The Iorwerth archer guarding the cave falls. His warrior companion is still inside.")
                .kill("iorwerth_warrior").stay().needFlag("sote_archer").unlessFlag("sote_orb").flag("sote_orb")
                .say("* The Iorwerth warrior falls. You recover the stolen orb.")
                .talk("lord_iorwerth").needFlag("sote_tonic", "sote_orb")
                .say("You're too late! I have corrupted the Fragment of Seren. The goddess's song will destroy you all!")
                .missing("* Iorwerth's guards bar the way. Baxtorian needs his tonic, and the orb the Iorwerth archers stole must be recovered.")
                .stage("I must defeat the corrupted Fragment of Seren.")
                .kill("sote_essyllt").stay().unlessFlag("sote_essyllt").flag("sote_essyllt")
                .say("* Essyllt, Iorwerth's agent among the mourners, falls. The Underground Pass is sealed behind you.")
                .kill("fragment_of_seren").needFlag("sote_essyllt").give("crystal_seed_sote").say("* The Fragment of Seren falls silent. A crystal seed lies where she stood.")
                .stage("I should give the crystal seed to Baxtorian so Prifddinas can be rebuilt.")
                .talk("baxtorian_elf").take("crystal_seed_sote").say("* Baxtorian sings to the seed. Crystal towers rise from the ruins: Prifddinas is restored!",
                        "The elven city welcomes you, hero of Prifddinas.")
                .done("arianwyn", "Welcome to Prifddinas, friend.");
    }
}
