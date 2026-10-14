package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/**
 * Mahjarrat and northern storylines: Enakhra's Lament, Devious Minds, The Garden of Death, Secrets of
 * the North, Desert Treasure II - The Fallen Empire, The Curse of Arrav, My Arm's Big Adventure and
 * Making Friends with My Arm. XP-lamp rewards are given as fixed XP in the usual lamp skills
 * (approximate where the player would choose).
 */
final class MahjarratQuests {
    private MahjarratQuests() {
    }

    static void register(QuestBook b) {
        b.place("quest_enakhras_temple", "Enakhra's Temple", 3104, 2932, PlaceDef.Style.RUINS);
        b.spot("quest_paterdomus_hills", "Hills north of Paterdomus", 3406, 3505);
        b.spot("quest_garden_of_death", "The Garden of Death", 1255, 3625);
        b.place("quest_the_vault", "The Vault", 3180, 2862, PlaceDef.Style.RUINS);
        b.place("quest_zemouregals_fort", "Zemouregal's fort", 3375, 3480, PlaceDef.Style.RUINS);
        b.spot("quest_troll_stronghold_roof", "Troll Stronghold rooftop", 2829, 3694);
        b.place("quest_weiss", "Weiss", 2848, 3936, PlaceDef.Style.CAMP);
        enakhrasLament(b);
        deviousMinds(b);
        gardenOfDeath(b);
        secretsOfTheNorth(b);
        desertTreasureII(b);
        curseOfArrav(b);
        myArmsBigAdventure(b);
        makingFriendsWithMyArm(b);
    }

    private static void enakhrasLament(QuestBook b) {
        Cast.npc(b, "lazim", "Lazim", "quest_enakhras_temple", 0, 3, Cast.BROWN, 0xC0A060);
        Cast.npc(b, "akthanakos", "Akthanakos", "quest_enakhras_temple", 3, -3, Cast.BROWN, 0x6A4A2A);
        b.item("quest_sandstone_statue", "Sandstone statue", "statue", 0xE0C890, "A statue of Enakhra, carved from sandstone.");
        b.items(s -> s.add("camulet", "Camulet", Kind.SIMPLE, "amulet", 0xC0A060).value(1).stack(1)
                .examine("An amulet that lets you talk to camels, and teleports you to Enakhra's Temple."));
        b.boss(BossDef.of("quest_boneguard", "Boneguard").level(132).stats(150, 90, 90, 120, 1, 1).speed(5)
                .defenceBonus(60, 60, 0, 60, 60)
                .attack(AttackDef.of("bone_club", AttackType.CRUSH, 16, 3, 6))
                .attack(AttackDef.of("stomp", AttackType.CRUSH, 12, 3, 2).special(Special.AOE, 3).telegraph(2, "The Boneguard raises its foot!"))
                .drops(Drop.always("big_bones")).skin(0xE8E0C8, 0xD8D0B8, 0xC8C0A8, 0x8A8070).size(1.2f, 2.6f, 1.1f)
                .at("quest_enakhras_temple").arena(9, "sand"));
        b.quest("enakhras_lament", "Enakhra's Lament", Difficulty.EXPERIENCED, 2)
                .req(Skill.CRAFTING, 50).req(Skill.FIREMAKING, 45).req(Skill.PRAYER, 43).req(Skill.MAGIC, 39)
                .xp(Skill.CRAFTING, 7000).xp(Skill.MINING, 7000).xp(Skill.FIREMAKING, 7000).xp(Skill.MAGIC, 7000).reward("camulet")
                .unlock("Access to Enakhra's Temple; talking to camels with the camulet")
                .stage("I can start this quest by speaking to Lazim at the temple south of the Bandit Camp. (Requires 50 Crafting, 45 Firemaking, 43 Prayer, 39 Magic.)")
                .talk("lazim").say("I need a statue carved for this ancient temple. Bring me sandstone and granite from the Desert Quarry and I'll show you where to carve.")
                .stage("I should carve a statue for Lazim from 10kg of sandstone and 5kg of granite, using a chisel.")
                .talk("lazim").take("sandstone_10kg", "granite_5kg").need("chisel").give("quest_sandstone_statue")
                .say("*You carve the head and body of a woman.* Excellent... she looks just like Enakhra. Now place it within.")
                .missing("I need 10kg of sandstone, 5kg of granite and a chisel.")
                .stage("I should set the statue in the temple and light the four braziers with fire spells and logs.")
                .reach("quest_enakhras_temple", 6).take("quest_sandstone_statue", "logs:4", "fire_rune:4").need("tinderbox")
                .say("* The statue clicks into place and the braziers roar. The ancient doors grind open... and a Boneguard rises from the dust!")
                .missing("* I need the statue, 4 logs, 4 fire runes and a tinderbox to light the temple's braziers.")
                .stage("I must defeat the Boneguard.")
                .kill("quest_boneguard").say("* The Boneguard collapses. A camel in the next chamber begins to speak: it is the Mahjarrat Akthanakos, cursed by Enakhra!")
                .stage("I should speak to Akthanakos and free him from Enakhra's curse.")
                .talk("akthanakos").say("Lazim is Enakhra in disguise! She cursed me into this camel form. *You break the spell.* Free at last. Take this camulet, friend.",
                        "* Enakhra and Akthanakos vanish, to settle their quarrel elsewhere.")
                .stage("I should take a last look around the temple with Lazim gone.")
                .reach("quest_enakhras_temple", 10).say("* The temple is silent. The camulet lets you return here, and talk to camels.");
    }

    private static void deviousMinds(QuestBook b) {
        Cast.npc(b, "devious_monk", "Monk", "quest_paterdomus_hills", 0, 0, Cast.FAIR, 0x6A4A2A);
        b.item("slender_blade", "Slender blade", "dagger", 0x8A9AB0, "A fine blade made from mithril.");
        b.item("bow_sword", "Bow-sword", "longbow", 0x8A9AB0, "A yew longbow with a blade fixed to it.");
        b.item("devious_package", "Package", "package", 0xA08060, "A package from the monk.");
        b.quest("devious_minds", "Devious Minds", Difficulty.EXPERIENCED, 1)
                .reqQuest("wanted", "troll_stronghold", "DORICS_QUEST", "enter_the_abyss")
                .req(Skill.SMITHING, 65).req(Skill.RUNECRAFT, 50).req(Skill.FLETCHING, 50)
                .xp(Skill.FLETCHING, 5000).xp(Skill.RUNECRAFT, 5000).xp(Skill.SMITHING, 6500)
                .unlock("Access to the Abyss via the Entrana Zamorakian portal")
                .stage("I can start this quest by speaking to the Monk in the hills north of Paterdomus. (Requires Wanted!, Troll Stronghold, Doric's Quest, Enter the Abyss, 65 Smithing, 50 Runecraft, 50 Fletching.)")
                .talk("devious_monk").say("I seek a weapon of a peculiar kind: a bow and a sword, joined. A bow-sword. Make me one, and I'll reward you.")
                .stage("I should smith a slender blade (a mithril bar) and attach it to a yew longbow to make a bow-sword.")
                .talk("devious_monk").take("mithril_bar", "yew_longbow").need("hammer").give("devious_package")
                .say("*You hammer out a slender blade and fix it to the bow.* Perfect. Now take this package to the Entrana priest... it's a gift. No questions.")
                .missing("Bring me a mithril bar and a yew longbow, and a hammer to smith the blade.")
                .stage("I should deliver the monk's package to the High Priest of Entrana. No weapons are allowed on the island.")
                .talk("high_priest_entrana").take("devious_package").say("A package? *It bursts open: a Zamorakian portal!* Doomsayers pour through and steal our holy relic!")
                .stage("The monk was a Zamorakian! I must chase the thieves into the Abyss and recover the relic.")
                .kill("abyssal_demon").say("* Among the abyssal creatures you find the relic, dropped by a fleeing doomsayer.")
                .stage("I should return the relic to the High Priest of Entrana.")
                .talk("high_priest_entrana").say("The relic is safe! You were deceived, but you made it right. The portal the Zamorakians made still leads into the Abyss, if you dare.");
    }

    private static void gardenOfDeath(QuestBook b) {
        Cast.npc(b, "kasonde", "Kasonde", "kebos_lowlands", -3, 3, Cast.BROWN, 0x5A6A3A);
        b.item("garden_tablet_fragment", "Stone tablet fragment", "rune", 0x8A8A7A, "A fragment of an ancient stone tablet.");
        b.quest("the_garden_of_death", "The Garden of Death", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.FARMING, 10000) // OSRS (current quest XP rewards)
                .req(Skill.FARMING, 20)
                .unlock("Access to the Garden of Death beneath the Kebos Swamp")
                .stage("I can start this quest by speaking to Kasonde in the Kebos Lowlands. (Requires 20 Farming.)")
                .talk("kasonde").say("I'm looking for a garden that was buried long ago, beneath the swamp. The old tablets say it was tended by the dead.")
                .stage("I should search the Kebos swamp for the entrance to the Garden of Death.")
                .reach("quest_garden_of_death", 6).need("spade").give("garden_tablet_fragment:3")
                .say("* You dig through the mud into a buried garden of dead plants. Three tablet fragments lie among the roots.")
                .missing("* The ground is too muddy to search by hand. I need a spade.")
                .stage("I should bring the tablet fragments to Kasonde.")
                .talk("kasonde").take("garden_tablet_fragment:3").say("These speak of a medallion of the old empire... and of a vault in the desert. Interesting. Very interesting. Thank you.");
    }

    private static void secretsOfTheNorth(QuestBook b) {
        Cast.npc(b, "evelot", "Evelot", "ARDOUGNE", 8, 2, Cast.FAIR, 0x6A2A5A);
        Cast.npc(b, "quest_hazel", "Hazel", "ARDOUGNE", -8, -2, Cast.PALE, 0x3A6A3A);
        Cast.npc(b, "jhallan", "Jhallan", "ghorrock_prison", 2, 2, 0x6A7A8A, 0x2A2A3A);
        b.quest("secrets_of_the_north", "Secrets of the North", Difficulty.MASTER, 2)
                .reqQuest("making_friends_with_my_arm", "the_generals_shadow", "devious_minds", "hazeel_cult")
                .xp(Skill.AGILITY, 60000).xp(Skill.THIEVING, 50000).xp(Skill.HUNTER, 40000) // OSRS (current quest XP rewards)
                .req(Skill.AGILITY, 69).req(Skill.THIEVING, 64).req(Skill.HUNTER, 56)
                .unlock("The Phantom Muspah boss in the Ghorrock Dungeon")
                .stage("I can start this quest by speaking to Evelot in East Ardougne. (Requires Making Friends with My Arm, The General's Shadow, Devious Minds, Hazeel Cult, 69 Agility, 64 Thieving, 56 Hunter.)")
                .talk("evelot").say("There's been a murder in my house! The victim's body is covered in frost... in summer. The Ardougne guards are useless.")
                .stage("I should ask Hazel, the victim's friend, what she knows.")
                .reach("ARDOUGNE", 10).stay().unlessFlag("sotn_scene").flag("sotn_scene")
                .say("* You search Evelot's house: frost on the body, a broken window and footprints leading north.")
                .talk("quest_hazel").needFlag("sotn_scene").missing("Have you looked at the house yet? The guards trampled everything.").say("He spoke of Ghorrock, the old Zarosian fortress in the frozen north... and of someone who kept him prisoner there.")
                .stage("I must travel to the Ghorrock Dungeon in the Wilderness and find out who is held there.")
                .reach("ghorrock_dungeon", 10).say("* In the frozen halls, the ice cracks. A Mahjarrat lies imprisoned in the ice: Jhallan.")
                .stage("I should speak to Jhallan in the Ghorrock prison.")
                .talk("jhallan").say("Free me and you shall know everything. *Behind you, something shimmers: a muspah, its prismatic form shifting.*")
                .stage("I must defeat the Phantom Muspah.")
                .kill("phantom_muspah").flag("sotn_muspah").say("* The Phantom Muspah shatters into shards of ice and shadow.")
                .stage("I should report to Evelot.")
                .talk("evelot").needFlag("sotn_muspah").say("Jhallan... a Mahjarrat? Then the murder was only the beginning. Thank you; the north holds more secrets yet.");
    }

    private static void desertTreasureII(QuestBook b) {
        Cast.npc(b, "asgarnia_smith", "Asgarnia Smith", "exam_centre", -3, 3, Cast.FAIR, 0x8A6A3A);
        Cast.npc(b, "quest_ancient_guardian_vault", "Ancient Guardian", "quest_the_vault", 0, 3, 0x9A9AA0, 0x5A3A6A);
        b.item("vardorvis_medallion_piece", "Vardorvis' medallion", "amulet", 0x8A2A2A, "A piece of the ancient medallion, taken from Vardorvis.");
        b.item("sucellus_medallion_piece", "Sucellus' medallion", "amulet", 0x2A8A8A, "A piece of the ancient medallion, taken from Duke Sucellus.");
        b.item("whisperer_medallion_piece", "Whisperer's medallion", "amulet", 0x2A2A8A, "A piece of the ancient medallion, taken from the Whisperer.");
        b.item("leviathan_medallion_piece", "Leviathan's medallion", "amulet", 0x2A6A2A, "A piece of the ancient medallion, taken from the Leviathan.");
        b.items(s -> s.add("ring_of_shadows", "Ring of shadows", Kind.SIMPLE, "ring", 0x3A2A4A).value(1).stack(1)
                .examine("A ring that reveals the shadow realm and teleports to the four strongholds of the old empire."));
        b.quest("desert_treasure_ii", "Desert Treasure II - The Fallen Empire", Difficulty.GRANDMASTER, 5)
                .reqQuest("desert_treasure_i", "secrets_of_the_north", "enakhras_lament", "temple_of_the_eye", "the_garden_of_death",
                        "below_ice_mountain", "his_faithful_servants")
                .req(Skill.FIREMAKING, 75).req(Skill.MAGIC, 75).req(Skill.THIEVING, 70).req(Skill.HERBLORE, 62)
                .req(Skill.RUNECRAFT, 60).req(Skill.CONSTRUCTION, 60)
                .xp(Skill.MAGIC, 35000).xp(Skill.FIREMAKING, 35000).xp(Skill.THIEVING, 35000) // approximate lamp rewards
                .reward("ring_of_shadows")
                .unlock("Awakened versions of Vardorvis, Duke Sucellus, the Whisperer and the Leviathan; the Lassar Undercity")
                .stage("I can start this quest by speaking to Asgarnia Smith at the Digsite Exam Centre. (Requires Desert Treasure I, Secrets of the North, Enakhra's Lament, Temple of the Eye, The Garden of Death, Below Ice Mountain, His Faithful Servants, 75 Firemaking, 75 Magic and more.)")
                .talk("asgarnia_smith").say("A vault of the old Zarosian empire has been uncovered in the desert! I need someone brave, and preferably expendable, to open it.")
                .stage("I should investigate the Vault in the Kharidian Desert.")
                .reach("quest_the_vault", 8).say("* The vault door bears four empty sockets, shaped like pieces of a medallion. An ancient guardian stirs.")
                .stage("I should speak to the Ancient Guardian of the Vault.")
                .talk("quest_ancient_guardian_vault").say("The medallion was split among the four strongholds of the empire: the Stranglewood, Ghorrock, Lassar and the Scar. Their keepers have... changed.")
                .stage("I must recover the four medallion pieces from Vardorvis, Duke Sucellus, the Whisperer and the Leviathan.")
                .drop("vardorvis", "vardorvis_medallion_piece", 1).drop("duke_sucellus", "sucellus_medallion_piece", 1)
                .drop("the_whisperer", "whisperer_medallion_piece", 1).drop("the_leviathan", "leviathan_medallion_piece", 1)
                .talk("quest_ancient_guardian_vault").need("vardorvis_medallion_piece", "sucellus_medallion_piece", "whisperer_medallion_piece", "leviathan_medallion_piece")
                .say("All four pieces. Place them in the door, and the vault will open.")
                .missing("The medallion is incomplete. Vardorvis, Duke Sucellus, the Whisperer and the Leviathan each hold a piece.")
                .stage("I should open the vault with the reassembled medallion.")
                .reach("quest_the_vault", 6).take("vardorvis_medallion_piece", "sucellus_medallion_piece", "whisperer_medallion_piece", "leviathan_medallion_piece")
                .say("* The vault opens. Inside, a shadow moves: someone has come for the empire's secret before you. It is a trap!",
                        "* After a desperate struggle the intruders flee, but something terrible has been woken.")
                .stage("I should tell Azzanadra in the Jaldraocht Pyramid what happened at the vault.")
                .talk("azzanadra").say("The empire stirs. You have done Zaros a great service, and earned a great warning. Keep this ring of shadows; it will guide you back to the four strongholds.");
    }

    private static void curseOfArrav(QuestBook b) {
        Cast.npc(b, "elias_white", "Elias White", "quest_zemouregals_fort", 0, 3, Cast.FAIR, 0x6A6A8A);
        b.item("arravs_heart", "Arrav's heart", "orb", 0x8A1A1A, "The heart of Arrav, kept by Zemouregal.");
        b.boss(BossDef.of("quest_zemouregal", "Zemouregal").level(252).stats(300, 180, 180, 200, 220, 150).speed(4)
                .defenceBonus(100, 100, 100, 150, 100)
                .attack(AttackDef.of("ancient_blast", AttackType.MAGIC, 28, 9, 6).particle("minecraft:soul"))
                .attack(AttackDef.of("undead_summon", AttackType.MAGIC, 12, 9, 2).special(Special.SUMMON, 2).telegraph(2, "Zemouregal raises his undead!"))
                .attack(AttackDef.of("claw", AttackType.SLASH, 24, 3, 3))
                .minion("minecraft:zombie", 3)
                .drops(Drop.always("bones")).skin(0x6A7A6A, 0x2A1A2A, 0x1A0A1A, 0x8A2A2A).size(1.0f, 2.6f, 1.1f)
                .at("quest_zemouregals_fort").arena(12, "crypt"));
        b.quest("the_curse_of_arrav", "The Curse of Arrav", Difficulty.MASTER, 2)
                .reqQuest("defender_of_varrock", "troll_romance")
                .req(Skill.MINING, 64).req(Skill.RANGED, 62).req(Skill.THIEVING, 62).req(Skill.AGILITY, 61).req(Skill.STRENGTH, 58).req(Skill.SLAYER, 37)
                .xp(Skill.MINING, 40000).xp(Skill.THIEVING, 40000).xp(Skill.AGILITY, 40000) // approximate lamp rewards
                .unlock("Arrav freed from Zemouregal's control")
                .stage("I can start this quest by speaking to Elias White near Zemouregal's fort. (Requires Defender of Varrock, Troll Romance, 64 Mining, 62 Ranged, 62 Thieving, 61 Agility, 58 Strength, 37 Slayer.)")
                .talk("elias_white").say("Arrav, the hero of Avarrocka, is still Zemouregal's undead slave. His heart is the key: Zemouregal keeps it in his fort.")
                .stage("I should break into Zemouregal's fort and steal Arrav's heart.")
                .reach("quest_zemouregals_fort", 8).give("arravs_heart").say("* Past the zombies and traps, you find a beating heart in a jar. Zemouregal will not be pleased.")
                .stage("Arrav attacks under Zemouregal's command. I must subdue him.")
                .kill("arrav").need("arravs_heart").say("* Arrav falls to his knees. Holding his heart, you feel Zemouregal's hold on him weaken.")
                .missing("* Without the heart, Arrav cannot be freed.")
                .stage("I must face Zemouregal himself.")
                .kill("quest_zemouregal").say("* Zemouregal retreats into the shadows, cursing your name. Arrav's will is his own again.")
                .stage("I should return Arrav's heart to Elias White.")
                .talk("elias_white").take("arravs_heart").say("Arrav is free. After all these centuries... Varrock owes you more than it knows.");
    }

    private static void myArmsBigAdventure(QuestBook b) {
        Cast.npc(b, "my_arm", "My Arm", "quest_troll_stronghold_roof", 0, 2, Cast.TROLL, 0x5A4A3A);
        Cast.npc(b, "murcaily", "Murcaily", "tai_bwo_wannai", 3, -3, Cast.BROWN, 0x3A6A2A);
        b.item("goutweed_tuber", "Goutweed tuber", "seed", 0x8A9A4A, "A tuber of the herb goutweed.");
        b.boss(BossDef.of("quest_giant_roc", "Giant Roc").level(172).stats(250, 120, 120, 120, 1, 120).speed(5)
                .attack(AttackDef.of("peck", AttackType.STAB, 20, 3, 5))
                .attack(AttackDef.of("boulder_drop", AttackType.RANGED, 18, 10, 3).special(Special.SKY_STRIKE, 0).telegraph(2, "The Giant Roc drops a boulder!"))
                .drops(Drop.always("big_bones")).skin(0x8A6A3A, 0x6A4A2A, 0x5A3A1A, 0xE0C040).size(2.0f, 2.4f, 1.3f).flying()
                .at("quest_troll_stronghold_roof").arena(10, "stone"));
        b.quest("my_arms_big_adventure", "My Arm's Big Adventure", Difficulty.EXPERIENCED, 1)
                .xp(Skill.HERBLORE, 10000).xp(Skill.FARMING, 5000) // OSRS (current quest XP rewards)
                .reqQuest("eadgars_ruse", "the_feud", "jungle_potion").req(Skill.WOODCUTTING, 10).req(Skill.FARMING, 29)
                .unlock("My Arm's herb patch on the Troll Stronghold roof")
                .stage("I can start this quest by speaking to Burntmeat in the Troll Stronghold kitchen. (Requires Eadgar's Ruse, The Feud, Jungle Potion, 10 Woodcutting, 29 Farming.)")
                .talk("burntmeat").say("My Arm want to grow things on roof! Silly troll. You go help him.")
                .stage("I should talk to My Arm on the Troll Stronghold roof.")
                .talk("my_arm").say("My Arm want to be farmer! Need compost, lots. And special plant, goutweed. Humans in jungle have it.")
                .stage("I should fetch a goutweed tuber from Murcaily in Tai Bwo Wannai's hardwood grove, and bring 3 supercompost.")
                .talk("murcaily").stay().unless("goutweed_tuber").give("goutweed_tuber").say("Goutweed for a troll? Ha! Take it, and stay away from my teak.")
                .talk("my_arm").take("goutweed_tuber", "supercompost:3").say("*You and My Arm plant the goutweed in the rooftop patch.* It growing! ...What that shadow?")
                .missing("My Arm need goutweed tuber from jungle man, and 3 supercompost.")
                .stage("A Giant Roc swoops down to protect its nest near the patch! I must defeat it.")
                .kill("quest_giant_roc").flag("mab_roc").say("* The Giant Roc flees, shedding feathers. My Arm's goutweed survives.")
                .stage("I should tell My Arm the patch is safe.")
                .talk("my_arm").needFlag("mab_roc").say("My Arm farmer now! You can use patch too. Thank you, human friend.");
    }

    private static void makingFriendsWithMyArm(QuestBook b) {
        Cast.npc(b, "snowflake", "Snowflake", "quest_weiss", 0, 3, Cast.TROLL, 0xE0E8F0);
        Cast.npc(b, "quest_odd_mushroom", "Odd Mushroom", "troll_stronghold", -4, -1, 0xC0A0C0, 0x8A4A8A);
        b.item("bucket_of_salt", "Bucket of salt", "pot", 0xF0F0F0, "Salt from the Weiss salt mine.");
        b.boss(BossDef.of("quest_mother", "Mother").level(255).stats(350, 200, 200, 180, 150, 1).speed(5)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 25, 3, 6))
                .attack(AttackDef.of("ice_breath", AttackType.MAGIC, 20, 9, 3).special(Special.FREEZE, 4).particle("minecraft:snowflake"))
                .attack(AttackDef.of("fire_breath", AttackType.MAGIC, 20, 9, 3).particle("minecraft:flame"))
                .drops(Drop.always("big_bones")).skin(0x8A8A78, 0x5A5A4A, 0x4A4A3A, 0xE0E8F0).size(1.4f, 3.0f, 1.3f)
                .at("quest_weiss").arena(12, "ice"));
        b.quest("making_friends_with_my_arm", "Making Friends with My Arm", Difficulty.MASTER, 2)
                .xp(Skill.MINING, 50000).xp(Skill.AGILITY, 50000).xp(Skill.FIREMAKING, 40000).xp(Skill.CONSTRUCTION, 10000) // OSRS (current quest XP rewards)
                .reqQuest("my_arms_big_adventure", "swan_song", "cold_war", "ROMEO_AND_JULIET")
                .req(Skill.FIREMAKING, 66).req(Skill.MINING, 72).req(Skill.CONSTRUCTION, 35).req(Skill.AGILITY, 68)
                .unlock("Access to Weiss, its salt mine and the Weiss fire pit teleport")
                .stage("I can start this quest by speaking to My Arm on the Troll Stronghold roof. (Requires My Arm's Big Adventure, Swan Song, Cold War, Romeo & Juliet, 66 Firemaking, 72 Mining, 35 Construction, 68 Agility.)")
                .talk("my_arm").say("My Arm's old home is Weiss, in the north. My Arm want to go back and see Mother... but trolls there not friendly.")
                .stage("I should ask the Odd Mushroom in the Troll Stronghold how to reach Weiss.")
                .talk("quest_odd_mushroom").say("Weiss lies past the ice. You'll need a boat... and a lot of nerve. *The mushroom winks. Mushrooms shouldn't wink.*")
                .stage("I should sail to Weiss and speak to Snowflake.")
                .talk("snowflake").travel("quest_weiss").say("You come with My Arm? Mother is angry. Mother always angry. Bring salt from the mine to show you are friend.")
                .stage("I should mine salt in the Weiss salt mine and build a fire pit for the trolls (4 logs).")
                .talk("snowflake").take("logs:4").need("tinderbox").give("bucket_of_salt")
                .say("*You mine salt and light a great fire pit.* Warm! Trolls like fire. Now Mother come...")
                .missing("Need 4 logs and a tinderbox for fire pit.")
                .stage("Mother, the troll queen of Weiss, attacks! I must defeat her.")
                .kill("quest_mother").say("* Mother is defeated. The trolls of Weiss bow to My Arm, their new leader.")
                .stage("I should return to My Arm.")
                .talk("my_arm").take("bucket_of_salt").say("My Arm is chief of Weiss now! You always welcome. Use fire pit to come back any time.");
    }
}
