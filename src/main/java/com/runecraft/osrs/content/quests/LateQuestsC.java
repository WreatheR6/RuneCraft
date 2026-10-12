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
 * Remaining members' quests across the world: Tai Bwo Wannai Trio, Troll Romance, One Small Favour,
 * Rag and Bone Man II, A Soul's Bane, The Great Brain Robbery, The Fremennik Exiles, A Night at the
 * Theatre, A Porcine of Interest, Sleeping Giants and Pandemonium. XP-lamp rewards are given as fixed
 * XP (approximate where the player would choose the skill).
 */
final class LateQuestsC {
    private LateQuestsC() {
    }

    static void register(QuestBook b) {
        b.place("quest_trollweiss", "Trollweiss Mountain", 2780, 3860, PlaceDef.Style.CAMP);
        b.place("quest_white_wolf_cave", "White Wolf Mountain cave", 2838, 3502, PlaceDef.Style.CAVE);
        b.place("quest_tolnas_rift", "Tolna's Rift", 3308, 3452, PlaceDef.Style.CAVE);
        b.place("quest_harmony_island", "Harmony Island", 3795, 2852, PlaceDef.Style.CAMP);
        b.place("quest_island_of_stone", "Island of Stone", 2470, 3990, PlaceDef.Style.RUINS);
        b.place("quest_sourhog_cave", "Sourhog cave", 3155, 3345, PlaceDef.Style.CAVE);
        b.spot("quest_jungle_forester_camp", "Kharazi forester camp", 2865, 2945);
        taiBwoWannaiTrio(b);
        trollRomance(b);
        oneSmallFavour(b);
        ragAndBoneManII(b);
        aSoulsBane(b);
        greatBrainRobbery(b);
        fremennikExiles(b);
        nightAtTheTheatre(b);
        porcineOfInterest(b);
        sleepingGiants(b);
        pandemonium(b);
    }

    private static void taiBwoWannaiTrio(QuestBook b) {
        Cast.npc(b, "timfraku", "Timfraku", "tai_bwo_wannai", -3, -3, Cast.DARK, 0xC08A3A);
        Cast.npc(b, "tamayu", "Tamayu", "tai_bwo_wannai", 4, 2, Cast.DARK, 0x8A3A2A);
        Cast.npc(b, "tiadeche", "Tiadeche", "tai_bwo_wannai", -4, 2, Cast.DARK, 0x2A6A8A);
        Cast.npc(b, "tinsay", "Tinsay", "tai_bwo_wannai", 0, -5, Cast.DARK, 0x6A8A2A);
        b.item("karambwan_paste", "Karambwan paste", "pot", 0x8A6A3A, "Poisonous paste made from raw karambwan.");
        b.boss(BossDef.of("quest_shaikahan", "Shaikahan").level(83).stats(100, 70, 75, 60, 1, 1).speed(4)
                .attack(AttackDef.of("claws", AttackType.SLASH, 12, 2.5, 6))
                .attack(AttackDef.of("pounce", AttackType.SLASH, 14, 4, 2).special(Special.KNOCKBACK, 2))
                .drops(Drop.always("big_bones")).skin(0xC08A3A, 0x8A5A2A, 0x6A4A1A, 0x2A1A0A).size(1.4f, 1.2f, 1.0f)
                .at("tai_bwo_wannai").arena(9, "jungle"));
        b.quest("tai_bwo_wannai_trio", "Tai Bwo Wannai Trio", Difficulty.INTERMEDIATE, 2)
                .req(Skill.FIREMAKING, 30)
                .reqQuest("jungle_potion").req(Skill.AGILITY, 15).req(Skill.COOKING, 30).req(Skill.FISHING, 5)
                .xp(Skill.ATTACK, 2500).xp(Skill.STRENGTH, 2500).xp(Skill.COOKING, 5000).xp(Skill.FISHING, 5000).xp(Skill.AGILITY, 2500)
                .unlock("Karambwan fishing with a karambwan vessel; cooking karambwans thoroughly")
                .stage("I can start this quest by speaking to Timfraku in Tai Bwo Wannai. (Requires Jungle Potion, 15 Agility, 30 Cooking, 5 Fishing, 30 Firemaking.)")
                .talk("timfraku").say("My sons Tamayu, Tiadeche and Tinsay each seek greatness and none listen to their father. Help them, and you help the village.")
                .stage("I should help Tiadeche catch karambwans with a karambwan vessel and karambwanji.")
                .talk("tiadeche").stay().unlessFlag("tbwt_tiadeche").take("raw_karambwanji:5").need("karambwan_vessel").give("raw_karambwan")
                .flag("tbwt_tiadeche").say("*You fish together off the coast.* A karambwan! Now I will be a great fisherman.")
                .missing("I need a karambwan vessel and 5 raw karambwanji for bait.")
                .talk("tinsay").stay().unlessFlag("tbwt_tinsay").take("raw_karambwan").give("karambwan_paste").flag("tbwt_tinsay")
                .say("Raw karambwan! Pound it into paste... careful, it's poisonous. Put it on Tamayu's spear.")
                .missing("Bring me a raw karambwan; my brother Tiadeche can help you catch one.")
                .talk("tamayu").needFlag("tbwt_tiadeche", "tbwt_tinsay").take("karambwan_paste")
                .say("Poison for my spear! Now I can hunt the Shaikahan, the great jungle beast. Come with me!")
                .missing("I need poison from Tinsay for my spear before I can face the Shaikahan.")
                .stage("I must help Tamayu defeat the Shaikahan.")
                .kill("quest_shaikahan").say("* The Shaikahan falls to Tamayu's poisoned spear and your blows.")
                .stage("I should tell Timfraku about his sons' success.")
                .talk("timfraku").say("My sons are great men now, all three! You have the thanks of Tai Bwo Wannai. Cook your karambwans thoroughly, friend.");
    }

    private static void trollRomance(QuestBook b) {
        Cast.npc(b, "ug", "Ug", "troll_stronghold", 4, -1, Cast.TROLL, 0x6A5A2A);
        Cast.npc(b, "aga", "Aga", "troll_stronghold", -4, 1, Cast.TROLL, 0x8A4A6A);
        Cast.npc(b, "quest_dunstan_sled", "Dunstan's apprentice", "BURTHORPE", 7, -3, 0xD8A88E, 0x5A3A2A);
        b.item("quest_sled", "Sled", "plank", 0x8A6A3A, "A sled for travelling down snowy slopes.");
        b.item("trollweiss_flower", "Trollweiss", "herb", 0xE0E0F0, "A rare flower from Trollweiss Mountain.");
        b.boss(BossDef.of("quest_arrg", "Arrg").level(113).stats(140, 90, 100, 80, 1, 1).speed(5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 20, 3, 6))
                .attack(AttackDef.of("rock_throw", AttackType.RANGED, 16, 9, 3))
                .drops(Drop.always("big_bones")).skin(0x8A8A78, 0x6A5A3A, 0x4A3A2A, 0x3A3A2A).size(1.3f, 2.8f, 1.2f)
                .at("quest_trollweiss").arena(10, "ice"));
        b.quest("troll_romance", "Troll Romance", Difficulty.EXPERIENCED, 2)
                .reqQuest("troll_stronghold").req(Skill.AGILITY, 28)
                .xp(Skill.AGILITY, 8000).xp(Skill.STRENGTH, 4000).reward("diamond", "trollweiss_flower")
                .unlock("Access to Trollweiss Mountain and the sled run")
                .stage("I can start this quest by speaking to Ug in the Troll Stronghold. (Requires Troll Stronghold, 28 Agility.)")
                .talk("ug").say("Ug love Aga. Aga not love Ug. Ug need present! Aga like flowers... Trollweiss flowers from top of mountain.")
                .stage("I need a sled to get down Trollweiss Mountain. Dunstan's apprentice in Burthorpe can make one (2 maple logs, rope).")
                .talk("quest_dunstan_sled").take("maple_logs:2", "rope").give("quest_sled").say("*He builds you a sled.* Wax the runners and hold on tight!")
                .missing("I'll need 2 maple logs and a rope for the sled.")
                .stage("I should climb Trollweiss Mountain and pick a Trollweiss flower. Arrg, a jealous troll, guards the summit.")
                .reach("quest_trollweiss", 8).need("quest_sled").say("* At the summit, Arrg bellows: 'Aga is Arrg's!'")
                .missing("* The slope is too steep to come back down without a sled.")
                .stage("I must defeat Arrg.")
                .kill("quest_arrg").give("trollweiss_flower").say("* Arrg is beaten. You pick a Trollweiss flower and sled down the mountain.")
                .stage("I should give the flower to Ug, who will give it to Aga.")
                .talk("ug").take("trollweiss_flower", "quest_sled").say("Flower! *Ug gives it to Aga.*", "@Aga: Ug... is sweet. Aga go for walk with Ug.",
                        "> Er, congratulations.", "Ug happy! Take shiny rock. And extra flower Ug find.")
                .done("aga", "Ug is... sweet. For a troll.");
    }

    private static void oneSmallFavour(QuestBook b) {
        Cast.npc(b, "yanni_salika", "Yanni Salika", "SHILO_VILLAGE", 5, -5, Cast.DARK, 0x8A6A4A);
        Cast.npc(b, "jungle_forester", "Jungle forester", "quest_jungle_forester_camp", 0, 2, Cast.TAN, 0x3A5A2A);
        Cast.npc(b, "quest_brian_archery", "Brian", "PORT_SARIM", -7, 3, Cast.FAIR, 0x6A3A2A);
        Cast.npc(b, "quest_horvik", "Horvik", "VARROCK", 8, 3, Cast.TAN, 0x5A5A5A);
        Cast.npc(b, "petra_fiyed", "Petra Fiyed", "quest_white_wolf_cave", 0, 3, Cast.PALE, 0x6A6A8A);
        b.item("quest_animate_rock_scroll", "Animate rock scroll", "note", 0xC0B090, "A scroll that animates a rock golem.");
        b.item("quest_broken_cauldron", "Broken cauldron", "pot", 0x3A3A3A, "Aggie's cauldron. It has a hole in it.");
        b.boss(BossDef.of("quest_slagilith", "Slagilith").level(92).stats(60, 1, 80, 120, 1, 1).speed(5)
                .defenceBonus(100, 100, 30, 100, 100)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 15, 3, 6))
                .drops(Drop.always("iron_ore")).skin(0x6A6A6A, 0x5A5A5A, 0x4A4A4A, 0x8A8A8A).size(1.2f, 2.2f, 1.0f)
                .at("quest_white_wolf_cave").arena(8, "stone"));
        b.quest("one_small_favour", "One Small Favour", Difficulty.EXPERIENCED, 2)
                .reqQuest("RUNE_MYSTERIES", "shilo_village", "DRUIDIC_RITUAL")
                .req(Skill.AGILITY, 36).req(Skill.CRAFTING, 25).req(Skill.HERBLORE, 18).req(Skill.SMITHING, 30)
                .xp(Skill.SMITHING, 10000).xp(Skill.AGILITY, 10000) // two 10,000 XP lamps (skill chosen by the player)
                .stage("I can start this quest by speaking to Yanni Salika in Shilo Village. (Requires Rune Mysteries, Shilo Village, Druidic Ritual, 36 Agility, 25 Crafting, 18 Herblore, 30 Smithing.)")
                .talk("yanni_salika").say("Could you do me one small favour? Ask the Jungle forester near Kharazi if he's finished my wooden carving.")
                .stage("I should visit the Jungle forester.")
                .talk("jungle_forester").say("The carving? I'd finish it if my axe weren't blunt. Brian in Port Sarim could sharpen it. Small favour?")
                .stage("I should ask Brian in Port Sarim to sharpen the forester's axe.")
                .talk("quest_brian_archery").say("Sharpen an axe? I'd love to, but my whetstone's at Aggie's. Ask her for it... she'll want a favour too.")
                .stage("Aggie in Draynor will part with the whetstone if her cauldron is repaired. Horvik in Varrock can fix it (needs a steel bar).")
                .talk("aggie").stay().unless("quest_broken_cauldron").give("quest_broken_cauldron").say("My cauldron's got a hole! Get it fixed and you can have the stone.")
                .talk("quest_horvik").take("quest_broken_cauldron", "steel_bar").say("*Horvik patches the cauldron.* Tell Aggie I want a discount on dyes.")
                .missing("I'll fix a cauldron, but I need a steel bar for the patch.")
                .stage("With Aggie's cauldron fixed, Petra Fiyed in the White Wolf Mountain cave needs help with a rock golem before the chain of favours can finish.")
                .talk("petra_fiyed").give("quest_animate_rock_scroll").say("A golem made of slag is loose in my cave! Read this scroll near it to make it fight you, then smash it.")
                .stage("I must defeat the Slagilith.")
                .kill("quest_slagilith").take("quest_animate_rock_scroll").say("* The Slagilith crumbles. Word travels quickly: every favour in the chain is done.")
                .stage("I should return to Yanni Salika with his carving.")
                .talk("yanni_salika").say("My carving! Was it any trouble? No? Splendid. Here's a little something for your one small favour.");
    }

    private static void ragAndBoneManII(QuestBook b) {
        b.item("quest_bone_crate", "Crate of bones", "package", 0xC0B090, "A crate full of rare bones for the Odd Old Man.");
        b.item("bonesack", "Bonesack", "cape", 0xD0C0A0, "A sack of bones, worn on the back.");
        b.items(s -> s.add("ram_skull_helm", "Ram skull helm", Kind.ARMOUR, "full_helm", 0xE0D8C0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, -6, -2, 10, 12, 8, -1, 10, 0, 0, 0, 2), 1))
                .examine("A helmet made from a ram's skull."));
        b.gear("bonesack", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 2), "rag_and_bone_man_ii");
        b.quest("rag_and_bone_man_ii", "Rag and Bone Man II", Difficulty.INTERMEDIATE, 1)
                .reqQuest("rag_and_bone_man_i", "skippy_and_the_mogres")
                .req(Skill.SLAYER, 40).req(Skill.DEFENCE, 20)
                .xp(Skill.PRAYER, 5000).reward("bonesack", "ram_skull_helm")
                .stage("I can start this quest by speaking to the Odd Old Man north-east of Varrock. (Requires Rag and Bone Man I, Skippy and the Mogres, 40 Slayer, 20 Defence.)")
                .talk("odd_old_man").say("More bones! I need a proper collection now: bats, wyverns, dagannoths, baby dragons... and big beasts.")
                .stage("I need bones from a giant bat, a skeletal wyvern, a dagannoth and a baby dragon, plus 5 big bones, all cleaned in vinegar.")
                .drop("giant_bat", "bat_bones", 2)
                .talk("odd_old_man").take("bat_bones", "wyvern_bones", "dagannoth_bones", "babydragon_bones", "big_bones:5").need("tinderbox")
                .give("quest_bone_crate").say("*You boil each bone clean.* Marvellous specimens! Pack them in this crate for me.")
                .missing("I need bat bones, wyvern bones, dagannoth bones, baby dragon bones and 5 big bones, and a tinderbox to boil them.")
                .stage("I should hand over the crate of bones.")
                .talk("odd_old_man").take("quest_bone_crate").say("My museum is complete! Take this bonesack and ram skull helm. Every collector should look the part.");
    }

    private static void aSoulsBane(QuestBook b) {
        Cast.npc(b, "launa", "Launa", "quest_tolnas_rift", 0, 3, Cast.FAIR, 0x6A3A5A);
        Cast.npc(b, "tolna", "Tolna", "quest_tolnas_rift", 3, -3, Cast.PALE, 0x3A3A6A);
        b.boss(BossDef.of("quest_fear_reaper", "Fear reaper").level(42).stats(55, 30, 35, 30, 1, 1).speed(4)
                .attack(AttackDef.of("scythe", AttackType.SLASH, 8, 2.5, 6))
                .attack(AttackDef.of("dread", AttackType.MAGIC, 6, 8, 3).special(Special.DRAIN_STAT, 2))
                .drops(Drop.always("bones")).skin(0x2A2A2A, 0x1A1A1A, 0x0A0A0A, 0xC0C0C0)
                .at("quest_tolnas_rift").arena(8, "void"));
        b.quest("a_souls_bane", "A Soul's Bane", Difficulty.NOVICE, 1)
                .xp(Skill.DEFENCE, 500).xp(Skill.HITPOINTS, 500)
                .unlock("Tolna's Rift, home of angry, fearful and confused souls")
                .stage("I can start this quest by speaking to Launa by the rift north-east of Varrock.")
                .talk("launa").say("My son Tolna climbed into this rift years ago and never came back. Will you go down and find him? Take a rope.")
                .stage("I should climb down into Tolna's Rift with a rope and face the manifestations of anger, fear, confusion and hopelessness.")
                .reach("quest_tolnas_rift", 6).need("rope").say("* Angry bears and unicorns, hopeless creatures and confused shapes swarm around you. You push through until a reaper blocks the way.")
                .missing("* The rift is too deep to climb without a rope.")
                .stage("I must defeat the Fear reaper.")
                .kill("quest_fear_reaper").say("* The reaper dissolves. At the bottom of the rift sits a young man, alone with his memories.")
                .stage("I should talk to Tolna.")
                .talk("tolna").say("Mother? She's still waiting for me? I let my anger and fear trap me here... I'll go up.")
                .stage("I should tell Launa that Tolna is safe.")
                .talk("launa").say("My boy! You brought him back to me. Thank you, adventurer, thank you.");
    }

    private static void greatBrainRobbery(QuestBook b) {
        Cast.npc(b, "quest_tranquility_harmony", "Brother Tranquility", "quest_harmony_island", 0, 3, 0xE0B89E, 0x6A4A3C);
        Cast.npc(b, "quest_brother_fintan", "Brother Fintan", "quest_harmony_island", 3, -3, Cast.FAIR, 0x6A4A3D);
        b.item("quest_prayer_book_fintan", "Prayer book", "book", 0x6A4A2A, "A book of prayers. It feels holier than most.");
        b.items(s -> s.add("barrelchest_anchor", "Barrelchest anchor", Kind.MELEE_WEAPON, "warhammer", 0x5A5A5A).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(-2, 10, 92, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0),
                        Map.of(Skill.ATTACK, 60, Skill.STRENGTH, 40), 6, AttackType.CRUSH, EquipmentDef.WeaponClass.MELEE))
                .examine("The anchor wielded by the Barrelchest. Its special attack lowers a target's stats."));
        b.boss(BossDef.of("quest_barrelchest", "Barrelchest").level(190).stats(134, 150, 180, 140, 1, 1).speed(6)
                .defenceBonus(80, 80, 60, 60, 120)
                .attack(AttackDef.of("anchor", AttackType.CRUSH, 32, 3, 6))
                .attack(AttackDef.of("ground_slam", AttackType.CRUSH, 18, 3, 2).special(Special.DRAIN_STAT, 3).telegraph(2, "The Barrelchest raises its anchor!"))
                .drops(Drop.always("big_bones"), Drop.of("barrelchest_anchor", 1, 1, 1)).skin(0x7A6A5A, 0x5A3A2A, 0x3A2A1A, 0x5A5A5A)
                .size(1.4f, 2.4f, 1.2f).at("quest_harmony_island").arena(10, "stone"));
        b.quest("the_great_brain_robbery", "The Great Brain Robbery", Difficulty.EXPERIENCED, 2)
                .reqQuest("creature_of_fenkenstrain", "cabin_fever")                .req(Skill.CRAFTING, 16).req(Skill.CONSTRUCTION, 30).req(Skill.PRAYER, 50)
                .xp(Skill.PRAYER, 6000).xp(Skill.CRAFTING, 2000).xp(Skill.CONSTRUCTION, 2000).reward("barrelchest_anchor")
                .unlock("Access to the Barrelchest; the Harmony Island monastery's prayer altar")
                .stage("I can start this quest by speaking to Brother Tranquility on Harmony Island. (Requires Creature of Fenkenstrain, Cabin Fever, Recipe for Disaster (Pirate Pete), 16 Crafting, 30 Construction, 50 Prayer.)")
                .talk("quest_tranquility_harmony").needFlag("rfd_pete")
                .missing("Come back once you've helped Pirate Pete at the Lumbridge banquet. (Requires Recipe for Disaster: Freeing Pirate Pete.)")
                .say("My brothers have vanished! Fenkenstrain's old lab assistant has been seen on the island with a sack of brains. Monk brains.")
                .stage("I should ask Dr Fenkenstrain what his assistant is up to.")
                .talk("dr_fenkenstrain").say("Mi-Gor! My assistant. He's building... an army, with the help of pirates. Brains in barrels. Undead pirates with monk brains. Hah! Ingenious.")
                .stage("I should rebuild the Harmony Island chapel's defences (5 planks, 10 steel nails) and recover Brother Fintan's prayer book.")
                .talk("quest_brother_fintan").take("plank:5", "steel_nails:10").need("hammer").give("quest_prayer_book_fintan")
                .say("*You barricade the chapel.* Bless you. Read from this holy book when the undead come; their monk brains may remember.")
                .missing("We need 5 planks and 10 steel nails, and you'll need a hammer.")
                .stage("I must stop Mi-Gor's creation: the Barrelchest.")
                .kill("quest_barrelchest").need("quest_prayer_book_fintan").say("* The Barrelchest topples, its anchor clanging to the floor. The stolen brains are rescued.")
                .missing("* Without a holy book, the undead monks will not hear you.")
                .stage("I should tell Brother Tranquility his brothers are safe.")
                .talk("quest_tranquility_harmony").take("quest_prayer_book_fintan").say("Our brothers' minds are restored. Keep the Barrelchest's anchor; it is too heavy for monks anyway.");
    }

    private static void fremennikExiles(QuestBook b) {
        Cast.npc(b, "quest_lady_v", "Lady V", "quest_island_of_stone", 0, 3, Cast.PALE, 0x3A3A5A);
        b.items(s -> s.add("vs_shield", "V's shield", Kind.SHIELD, "kiteshield", 0x5A4A3A).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, -8, -2, 50, 52, 48, 0, 50, 0, 0, 0, 0), 60))
                .examine("The shield of V. Its special attack damages nearby enemies."));
        b.boss(BossDef.of("quest_basilisk_boss", "Basilisk Youngling").level(158).stats(300, 150, 150, 160, 150, 150).speed(4)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(AttackDef.of("bite", AttackType.STAB, 24, 3, 6))
                .attack(AttackDef.of("gaze", AttackType.MAGIC, 18, 9, 3).special(Special.DRAIN_STAT, 3).telegraph(2, "The basilisk's eyes glow!"))
                .minion("basilisk_knight", 2)
                .drops(Drop.always("big_bones")).skin(0x5A7A4A, 0x3A5A2A, 0x2A3A1A, 0xE0C040).size(1.6f, 1.6f, 1.2f)
                .at("quest_island_of_stone").arena(12, "stone"));
        b.quest("the_fremennik_exiles", "The Fremennik Exiles", Difficulty.MASTER, 2)
                .req(Skill.MINING, 60)
                .xp(Skill.SLAYER, 50000).xp(Skill.CRAFTING, 50000).xp(Skill.RUNECRAFT, 30000) // OSRS (current quest XP rewards)
                .reqQuest("the_fremennik_isles", "lunar_diplomacy", "mountain_daughter", "heroes_quest")
                .req(Skill.CRAFTING, 65).req(Skill.SLAYER, 60).req(Skill.SMITHING, 60).req(Skill.FISHING, 60).req(Skill.RUNECRAFT, 55)
                .reward("vs_shield")
                .unlock("Basilisk Knights in Jormungand's Prison; the Neitiznot faceguard")
                .stage("I can start this quest by speaking to Brundt the Chieftain in Rellekka. (Requires The Fremennik Isles, Lunar Diplomacy, Mountain Daughter, Heroes' Quest, 65 Crafting, 60 Slayer, 60 Smithing, 60 Fishing, 60 Mining, 55 Runecraft.)")
                .talk("brundt").say("Basilisks attack our fishermen! They come from the Island of Stone. Some say an exile commands them.")
                .stage("I should sail to the Island of Stone and find the exile.")
                .talk("quest_lady_v").say("I am V's descendant, exiled for... reasons. The basilisks guard Jormungand's prison, and their young one grows strong. Stop it.")
                .stage("I must craft a mirror shield's worth of protection and kill the Basilisk Youngling in Jormungand's prison.")
                .kill("quest_basilisk_boss").say("* The Basilisk Youngling falls. Its gaze fades from the stone.")
                .stage("I should report to Lady V.")
                .talk("quest_lady_v").say("You did what my people could not. Take V's shield; it was always meant to protect the Fremennik.")
                .stage("I should tell Brundt the basilisks are no longer a threat.")
                .talk("brundt").say("Basilisks gone and an exile found! Rellekka will sing of this. Skol!");
    }

    private static void nightAtTheTheatre(QuestBook b) {
        Cast.npc(b, "quest_mysterious_stranger_tob", "Mysterious Stranger", "quest_ver_sinhaza", 3, -3, Cast.VAMPYRE, 0x2A2A2A);
        b.item("quest_spider_venom_sample", "Spider venom sample", "vial", 0x5A8A3A, "A sample of venom from the spiders beneath Ver Sinhaza.");
        b.quest("a_night_at_the_theatre", "A Night at the Theatre", Difficulty.MASTER, 2)
                .reqQuest("a_taste_of_hope")
                .xp(Skill.SLAYER, 20000).xp(Skill.HITPOINTS, 20000) // two 20,000 XP combat lamps (approximate)
                .unlock("The Theatre of Blood entry mode; Ver Sinhaza's spider caves")
                .stage("I can start this quest by speaking to Veliaf Hurtz in the Myreque hideout. (Requires A Taste of Hope.)")
                .talk("veliaf_hurtz").say("Lady Verzik Vitur runs a theatre where the condemned fight for her amusement. A stranger in Ver Sinhaza says he can get us in.")
                .stage("I should meet the Mysterious Stranger in Ver Sinhaza.")
                .talk("quest_mysterious_stranger_tob").say("You want to stop Verzik? First, there's a poison that could weaken her vampyres. It comes from the spiders below.")
                .stage("I should collect a venom sample from the giant spiders in the caves beneath Ver Sinhaza.")
                .drop("giant_spider", "quest_spider_venom_sample", 2)
                .talk("quest_mysterious_stranger_tob").take("quest_spider_venom_sample")
                .say("This will do. Now: the Theatre of Blood. You'll go in as a challenger, and survive. I'll handle the rest.")
                .missing("Spider venom. Giant spiders. Below. Go.")
                .stage("I must survive a run through the Theatre of Blood's entry mode.")
                .reach("quest_ver_sinhaza", 10).say("* You fight through the Maiden, the Bloat, the Nylocas, Sotetseg and Xarpus... and Verzik herself watches, amused.")
                .stage("I should report to Veliaf.")
                .talk("veliaf_hurtz").say("You survived the Theatre! And the venom may be our weapon against Verzik. Hope lives in Morytania yet.");
    }

    private static void porcineOfInterest(QuestBook b) {
        Cast.npc(b, "spria", "Spria", "DRAYNOR_VILLAGE", -7, 2, Cast.FAIR, 0x3A6A4A);
        Cast.npc(b, "sarah_farmer", "Sarah", "quest_sourhog_cave", 3, 3, Cast.FAIR, 0x8A5A3A);
        b.item("sourhog_foot", "Sourhog foot", "meat_raw", 0xC08A7A, "The foot of the Sourhog. Proof of the kill.");
        b.boss(BossDef.of("quest_sourhog", "Sourhog").level(37).stats(60, 30, 30, 25, 1, 1).speed(4)
                .attack(AttackDef.of("gore", AttackType.STAB, 6, 2.5, 6))
                .attack(AttackDef.of("stench", AttackType.MAGIC, 4, 4, 2).special(Special.AOE, 3))
                .drops(Drop.always("bones")).skin(0xC08A7A, 0x8A5A4A, 0x6A3A2A, 0x3A2A1A).size(1.2f, 1.0f, 1.0f)
                .at("quest_sourhog_cave").arena(8, "stone"));
        b.quest("a_porcine_of_interest", "A Porcine of Interest", Difficulty.NOVICE, 1)
                .xp(Skill.SLAYER, 1000)
                .unlock("Spria, the Draynor Village Slayer master")
                .stage("I can start this quest by reading the notice board in Draynor Village and speaking to Spria. ")
                .talk("spria").say("A giant pig has been terrorising Sarah's farm. Folk call it the Sourhog. Fancy a bit of slaying?")
                .stage("I should talk to Sarah at the cave near her farm.")
                .talk("sarah_farmer").say("It's in that cave! Bring a rope to get down, and mind the smell.")
                .stage("I must climb into the cave with a rope and kill the Sourhog.")
                .kill("quest_sourhog").need("rope").give("sourhog_foot").say("* The Sourhog falls. You take its foot as proof.")
                .missing("* I need a rope to climb down into the cave.")
                .stage("I should take the Sourhog's foot to Spria.")
                .talk("spria").take("sourhog_foot").say("That's the Sourhog all right. You've a knack for this. I'll give you Slayer assignments any time.");
    }

    private static void sleepingGiants(QuestBook b) {
        Cast.npc(b, "quest_kovac", "Kovac", "mg_giants_foundry", -3, 3, Cast.TAN, 0x6A4A3A);
        b.item("quest_giant_mould", "Giant sword mould", "mould", 0x8A6A4A, "A mould for a giant-sized sword.");
        b.quest("sleeping_giants", "Sleeping Giants", Difficulty.INTERMEDIATE, 1)
                .req(Skill.SMITHING, 15)
                .xp(Skill.SMITHING, 6000) // approximate
                .unlock("Giants' Foundry minigame")
                .stage("I can start this quest by speaking to Kovac outside the Giants' Foundry, east of Al Kharid. (Requires 15 Smithing.)")
                .talk("quest_kovac").say("The giants' old foundry! The fire giant who ran it is asleep inside. Help me wake the furnace and we can forge swords for giants again.")
                .stage("I should help Kovac fix the foundry: bring 2 bronze bars and 2 iron bars for the moulds.")
                .talk("quest_kovac").take("bronze_bar:2", "iron_bar:2").need("hammer").give("quest_giant_mould")
                .say("*You repair the moulds and stoke the crucible.* It's working! Now pour a sword into that mould.")
                .missing("2 bronze bars and 2 iron bars, and a hammer, please.")
                .stage("I should hand the finished giant sword to Kovac.")
                .talk("quest_kovac").take("quest_giant_mould").say("A fine giant sword! The foundry lives again. Come and smith for the giants whenever you like.");
    }

    private static void pandemonium(QuestBook b) {
        Cast.npc(b, "quest_captain_dushan", "Captain Dushan", "PORT_SARIM", 7, -2, Cast.TAN, 0x2A3A6A);
        Cast.npc(b, "quest_pandemonium_quartermaster", "Quartermaster", "port_sarim_docks", 3, -3, Cast.FAIR, 0x6A2A2A);
        b.item("quest_sailing_charts", "Sailing charts", "note", 0xE0D0A0, "Charts of the waters around Port Sarim.");
        // Sailing's introductory quest (2025). Requirements and rewards are approximate.
        b.quest("pandemonium", "Pandemonium", Difficulty.NOVICE, 1)
                .xp(Skill.SAILING, 300) // OSRS (current quest XP rewards)
                .unlock("Your first boat and the Sailing skill's ports")
                .stage("I can start this quest by speaking to Captain Dushan in Port Sarim.")
                .talk("quest_captain_dushan").say("You want to sail? Every sailor starts by swabbing decks. Report to the quartermaster at the docks.")
                .stage("I should report to the quartermaster at the Port Sarim docks with 3 planks to patch the hull.")
                .talk("quest_pandemonium_quartermaster").take("plank:3").need("hammer").give("quest_sailing_charts")
                .say("*You patch the hull.* Not bad. Take these charts out to Mudskipper Point and back.")
                .missing("Three planks and a hammer. The hull won't patch itself.")
                .stage("I should sail out to Mudskipper Point with the charts.")
                .reach("mudskipper_point", 8).need("quest_sailing_charts").say("* The boat skims across the bay. You mark the reefs on the chart.")
                .stage("I should return the charts to Captain Dushan.")
                .talk("quest_captain_dushan").take("quest_sailing_charts").say("You've the makings of a sailor. Here, she's a small boat, but she's yours. Welcome to the open sea.");
    }
}
