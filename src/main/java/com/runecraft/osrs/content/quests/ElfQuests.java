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
                .stage("I can start this quest by speaking to King Lathas in East Ardougne. (Requires Biohazard, 25 Ranged.)")
                .talk("king_lathas").say("My scouts must reach the elven lands beyond the mountains. The only way is the Underground Pass, now ruled by the dark lord Iban. Find a way through.")
                .stage("I should meet Koftik at the Underground Pass entrance west of West Ardougne.")
                .talk("koftik").travel("underground_pass").say("The pass is full of traps. Follow me, and pray to Saradomin.")
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
        b.boss(BossDef.of("king_tyras", "King Tyras").level(110).stats(110, 90, 100, 90, 1, 1).speed(4)
                .defenceBonus(60, 60, 60, 0, 60)
                .attack(AttackDef.of("halberd", AttackType.SLASH, 17, 3, 10))
                .drops(Drop.always("bones")).skin(0xE8C0AD, 0xC0C0C8, 0x8A1A1A, 0xE0C030).at("tyras_camp").arena(10, "jungle"));
        b.quest("regicide", "Regicide", Difficulty.MASTER, 3)
                .reqQuest("underground_pass").req(Skill.AGILITY, 56).req(Skill.CRAFTING, 10)
                .xp(Skill.AGILITY, 13750)
                .unlock("Access to Tirannwn and Lletya")
                .stage("I can start this quest by speaking to King Lathas in East Ardougne. (Requires Underground Pass, 56 Agility, 10 Crafting.)")
                .talk("king_lathas").travel("LLETYA").say("My brother Tyras leads an army in Tirannwn. He must be stopped. Go through the pass, find the elves and destroy his camp.",
                        "* You travel through the Underground Pass into the elven lands.")
                .stage("I should speak to the elf Arianwyn in Lletya.")
                .talk("arianwyn").say("Lathas sent you? Tyras's army ravages our forests. His camp lies west of here. You'll need something powerful to break its defences.")
                .stage("I need a barrel bomb to destroy Tyras's catapult. The chemist in Rimmington knows how to make coal tar explosives.")
                .talk("chemist").take("coal:5", "minecraft:gunpowder").give("barrel_bomb").say("Coal tar and... yes, this will make quite the bang. Careful!")
                .missing("A barrel bomb needs 5 coal and some gunpowder.")
                .stage("I should use the barrel bomb on Tyras's catapult and confront King Tyras.")
                .talk("tracker_elf").take("barrel_bomb").say("* The elf leads you to Tyras's camp. The barrel bomb destroys the catapult in a huge explosion!",
                        "Tyras himself is coming!")
                .stage("I must defeat King Tyras.")
                .kill("king_tyras").say("* King Tyras falls. His army scatters into the forest.")
                .stage("I should report to Arianwyn.")
                .talk("arianwyn").say("Tyras is dead... but I fear Lathas has his own plans. Still, Tirannwn is grateful. You may enter Lletya freely.");
    }

    private static void songOfTheElves(QuestBook b) {
        b.npc("lord_iorwerth", "Lord Iorwerth", "prifddinas_ruins", 3, 3, 0xF4E0D2, 0x2A2A2A, 0x1A1A1A, 0xA0A0A0);
        b.npc("baxtorian_elf", "Baxtorian", "prifddinas_ruins", -3, 3, 0xF4E0D3, 0x3A8AA0, 0x2A5A6A, 0xE0E0E2);
        b.item("crystal_seed_sote", "Crystal seed", "gem", 0xB8F0FF, "The seed of the great crystal of Prifddinas.");
        b.boss(BossDef.of("fragment_of_seren", "Fragment of Seren").level(494).stats(1000, 300, 200, 300, 400, 1).speed(4)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(AttackDef.of("crystal_slash", AttackType.SLASH, 30, 3, 5))
                .attack(AttackDef.of("crystal_spike", AttackType.MAGIC, 40, 10, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Crystals rain down from the sky!").particle("minecraft:end_rod"))
                .attack(AttackDef.of("fragment_drain", AttackType.MAGIC, 20, 10, 2).special(Special.DRAIN_PRAYER, 15))
                .phase(66, "The Fragment of Seren shatters into crystals and reforms!", 1.2, false)
                .phase(33, "Seren's fragment sings a terrible song!", 1.4, false)
                .drops(Drop.always("coins")).size(0.8f, 2.4f, 1.3f).at("prifddinas_ruins").arena(14, "void"));
        b.quest("song_of_the_elves", "Song of the Elves", Difficulty.GRANDMASTER, 4)
                .reqQuest("regicide", "plague_city").reqQuest("mournings_end_part_ii", "making_history")
                .req(Skill.AGILITY, 70).req(Skill.CONSTRUCTION, 70).req(Skill.FARMING, 70).req(Skill.HERBLORE, 70)
                .req(Skill.HUNTER, 70).req(Skill.MINING, 70).req(Skill.SMITHING, 70).req(Skill.WOODCUTTING, 70)
                .xp(Skill.AGILITY, 20000).xp(Skill.FARMING, 20000).xp(Skill.HERBLORE, 20000).xp(Skill.MINING, 20000)
                .xp(Skill.CONSTRUCTION, 20000).xp(Skill.HUNTER, 20000).xp(Skill.SMITHING, 20000).xp(Skill.WOODCUTTING, 20000)
                .unlock("Access to Prifddinas, the elven city")
                .stage("I can start this quest by speaking to Arianwyn in Lletya. (Requires Regicide and the Mourning's End quests; 70 in eight skills.)")
                .talk("arianwyn").say("The time has come to restore Prifddinas. But Lord Iorwerth's forces still hold the ruins, and something darker stirs beneath.")
                .stage("I should rally the elves and the people of Ardougne. Elena and Edmond may help.")
                .talk("elena").say("King Lathas has been fighting Iorwerth in secret all along? Then we stand with the elves.")
                .stage("I should travel to the ruins of Prifddinas and confront Lord Iorwerth.")
                .talk("lord_iorwerth").say("You're too late! I have corrupted the Fragment of Seren. The goddess's song will destroy you all!")
                .stage("I must defeat the corrupted Fragment of Seren.")
                .kill("fragment_of_seren").give("crystal_seed_sote").say("* The Fragment of Seren falls silent. A crystal seed lies where she stood.")
                .stage("I should give the crystal seed to Baxtorian so Prifddinas can be rebuilt.")
                .talk("baxtorian_elf").take("crystal_seed_sote").say("* Baxtorian sings to the seed. Crystal towers rise from the ruins: Prifddinas is restored!",
                        "The elven city welcomes you, hero of Prifddinas.")
                .done("arianwyn", "Welcome to Prifddinas, friend.");
    }
}
