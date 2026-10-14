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
 * Varlamore and its neighbours (2024-2025): Children of the Sun, Twilight's Promise, Perilous Moons,
 * At First Light, The Ribbiting Tale of a Lily Pad Labour Dispute, Death on the Isle, Meat and Greet,
 * The Heart of Darkness, Ethically Acquired Antiquities, Scrambled!, Shadows of Custodia and The Final
 * Dawn. Quest points and requirements follow OSRS; XP-lamp rewards are given as fixed XP and the
 * later quests' details are approximate.
 */
final class VarlamoreQuests {
    private VarlamoreQuests() {
    }

    static void register(QuestBook b) {
        b.place("quest_cam_torum", "Cam Torum", 1440, 3110, PlaceDef.Style.CAVE);
        b.spot("quest_villa_lucens", "Villa Lucens", 1450, 2968);
        b.place("quest_tal_teklan", "Tal Teklan", 1225, 3090, PlaceDef.Style.CAMP);
        b.place("quest_ralos_rise", "Ralos' Rise", 1460, 3330, PlaceDef.Style.RUINS);
        b.place("quest_tower_of_ascension", "Tower of Ascension", 1500, 3230, PlaceDef.Style.RUINS);
        b.spot("quest_civitas_lily_pond", "Civitas lily pond", 1712, 3098);
        b.spot("quest_custodia_pass", "Custodia Pass", 1255, 3335);
        b.spot("quest_sunrise_palace", "Sunrise Palace", 1680, 3160);
        childrenOfTheSun(b);
        twilightsPromise(b);
        perilousMoons(b);
        atFirstLight(b);
        ribbitingTale(b);
        deathOnTheIsle(b);
        meatAndGreet(b);
        heartOfDarkness(b);
        ethicallyAcquiredAntiquities(b);
        scrambled(b);
        shadowsOfCustodia(b);
        finalDawn(b);
    }

    private static void childrenOfTheSun(QuestBook b) {
        Cast.npc(b, "alina", "Alina", "VARROCK", 8, -3, Cast.TAN, 0xC08A2A);
        Cast.npc(b, "quest_suspicious_guard", "Guard", "VARROCK", -8, 3, Cast.FAIR, 0x8A8A9A);
        // Wiki walkthrough: hiding spots along the guard's route (Lowe's Archery Emporium, Old Man Yarlo's house, the house by
        // Aubury's), Sergeant Tobyn (3211, 3437) and the four bandits posing as guards (Aris's tent, Benny's stand, outside Lowe's,
        // the path to the east bank). // approx tiles
        b.spot("cots_hide_archery", "Lowe's Archery Emporium", 3232, 3423);
        b.spot("cots_hide_yarlo", "Old Man Yarlo's house", 3241, 3395);
        b.spot("cots_bandit_house", "House beside Aubury's Rune Shop", 3255, 3398);
        b.spot("cots_tobyn", "Varrock Square (north)", 3211, 3437);
        b.spot("cots_bandit_aris", "By Aris's tent", 3204, 3424);
        b.spot("cots_bandit_benny", "By Benny's news stand", 3219, 3431);
        b.spot("cots_bandit_lowe", "Outside Lowe's Archery Emporium", 3234, 3420);
        b.spot("cots_bandit_east", "Path to Varrock east bank", 3245, 3425);
        Cast.npc(b, "sergeant_tobyn", "Sergeant Tobyn", "cots_tobyn", 0, 0, Cast.FAIR, 0x8A8A9A);
        Cast.npc(b, "cots_bandit_1", "Guard", "cots_bandit_aris", 0, 0, Cast.FAIR, 0x8A8A9A);
        Cast.npc(b, "cots_bandit_2", "Guard", "cots_bandit_benny", 0, 0, Cast.TAN, 0x8A8A9A);
        Cast.npc(b, "cots_bandit_3", "Guard", "cots_bandit_lowe", 0, 0, Cast.BROWN, 0x8A8A9A);
        Cast.npc(b, "cots_bandit_4", "Guard", "cots_bandit_east", 0, 0, Cast.FAIR, 0x8A8A9A);
        b.quest("children_of_the_sun", "Children of the Sun", Difficulty.NOVICE, 1)
                .unlock("Travel to Varlamore by quetzal from Varrock")
                .stage("I can start this quest by speaking to Alina in Varrock Square.")
                .talk("alina").say("A delegation from Varlamore is arriving today. But look, that guard with the big bag... he's up to something. Follow him, and don't let him see you!")
                .stage("I should follow the suspicious guard to south-east Varrock, hiding whenever he turns around: in Lowe's Archery Emporium, "
                        + "then behind Old Man Yarlo's house.")
                .hint("quest_suspicious_guard", "* Better not let him see you. Stay out of sight and follow him.")
                .reach("cots_hide_archery", 3).stay().unlessFlag("cots_hide1").flag("cots_hide1")
                .say("* You duck into the archery shop as the guard turns around. He doesn't see you.")
                .reach("cots_hide_yarlo", 3).stay().needFlag("cots_hide1").unlessFlag("cots_hide2").flag("cots_hide2")
                .say("* You flatten yourself against the wall of Old Man Yarlo's house. The guard looks around, then walks on.")
                .reach("cots_bandit_house", 4).needFlag("cots_hide1", "cots_hide2")
                .say("* The guard slips into a house beside Aubury's Rune Shop. Through the window you see him talking with bandits.",
                        "@Bandit: When the Varlamorians come through, we strike. Our friends in the guard will look the other way.",
                        "* They're planning to attack the delegation! You should tell Sergeant Tobyn in Varrock Square.")
                .missing("* You need to follow the guard without being seen. Hide in the archery shop, then behind Old Man Yarlo's house.")
                .stage("I should tell Sergeant Tobyn in Varrock Square and identify the four bandits posing as Varrock guards.")
                .talk("sergeant_tobyn").needFlag("cots_b1", "cots_b2", "cots_b3", "cots_b4")
                .say("All four arrested. Come with me, we'll see what they have to say.",
                        "* On the palace roof, Prince Itzla Arkan himself helps you interrogate a bandit. Someone paid them to kill Servius, Teokan of Ralos.",
                        "@Prince Itzla Arkan: You have my thanks, outlander. Varlamore is open to you; Regulus Cento will take you there by quetzal.")
                .missing("Point out the four bandits pretending to be my guards. Look for anything out of place.")
                .talk("sergeant_tobyn").stay().unlessFlag("cots_tobyn").flag("cots_tobyn")
                .say("Bandits disguised as my guards? Then point them out to me. Look for anything out of place.")
                .talk("cots_bandit_1").stay().needFlag("cots_tobyn").unlessFlag("cots_b1").flag("cots_b1")
                .say("* No weapon, hands on hips outside Aris's tent. You mark him as a bandit.")
                .talk("cots_bandit_2").stay().needFlag("cots_tobyn").unlessFlag("cots_b2").flag("cots_b2")
                .say("* A guard without a helmet by Benny's news stand. You mark her as a bandit.")
                .talk("cots_bandit_3").stay().needFlag("cots_tobyn").unlessFlag("cots_b3").flag("cots_b3")
                .say("* Leaning on the wall with arms crossed and no weapon. You mark him as a bandit.")
                .talk("cots_bandit_4").stay().needFlag("cots_tobyn").unlessFlag("cots_b4").flag("cots_b4")
                .say("* Varrock guards carry swords, not maces. You mark him as a bandit.")
                .done("alina", "Thank you. The delegation is safe, thanks to you.");
    }

    private static void twilightsPromise(QuestBook b) {
        Cast.npc(b, "prince_itzla_arkan", "Prince Itzla Arkan", "quest_sunrise_palace", 0, 2, Cast.TAN, 0xE0B040);
        Cast.npc(b, "quest_civitas_guard_captain", "Guard captain", "zeah_civitas_illa_fortis", 3, 3, Cast.BROWN, 0x8A2A2A);
        b.item("twilight_cult_orders", "Cult orders", "note", 0x3A2A5A, "Orders from the Twilight Emissaries.");
        b.quest("twilights_promise", "Twilight's Promise", Difficulty.INTERMEDIATE, 1)
                .reqQuest("children_of_the_sun")
                .xp(Skill.THIEVING, 3000) // approximate
                .unlock("Fortis Colosseum fights; the Twilight Emissaries' hideout")
                .stage("I can start this quest by speaking to the Guard captain in Civitas illa Fortis. (Requires Children of the Sun.)")
                .talk("quest_civitas_guard_captain").say("A cult calling themselves the Twilight Emissaries plans to strike during the colosseum games. The prince must be warned.")
                .stage("I should find the cult's agents in the city.")
                .drop("dark_wizard", "twilight_cult_orders", 2)
                .talk("quest_civitas_guard_captain").need("twilight_cult_orders").say("These orders... they mean to kill Prince Itzla in the colosseum! Take them to him at once.")
                .missing("Find proof. The cultists wear dark robes and carry their orders with them.")
                .stage("I should warn Prince Itzla Arkan at the Sunrise Palace.")
                .talk("prince_itzla_arkan").take("twilight_cult_orders").say("So the Emissaries move against my family. You have my thanks, outlander, and my promise: Varlamore will not forget.");
    }

    private static BossDef moon(String id, String name, AttackType style, Special special, int skin, int glow) {
        return BossDef.of(id, name).level(160).stats(250, 140, 140, 120, 140, 140).speed(4)
                .defenceBonus(80, 80, 80, 80, 80)
                .attack(AttackDef.of("strike", style, 22, 3, 6))
                .attack(AttackDef.of("moonfall", AttackType.MAGIC, 18, 9, 3).special(special, 4).telegraph(2, name + " calls down the moonlight!"))
                .drops(Drop.always("bones")).skin(skin, glow, skin, glow).size(1.0f, 2.4f, 1.1f).at("neypotzli").arena(10, "stone");
    }

    private static void perilousMoons(QuestBook b) {
        Cast.npc(b, "attala", "Attala", "quest_cam_torum", 0, 3, Cast.BROWN, 0x3A5A8A);
        Cast.npc(b, "quest_lunar_scholar", "Lunar scholar", "neypotzli", 3, 3, Cast.TAN, 0x5A3A8A);
        b.boss(moon("quest_blood_moon", "Blood Moon", AttackType.SLASH, Special.LIFESTEAL, 0x6A1A1A, 0xC02020));
        b.boss(moon("quest_blue_moon", "Blue Moon", AttackType.CRUSH, Special.FREEZE, 0x1A2A6A, 0x40A0E0));
        b.boss(moon("quest_eclipse_moon", "Eclipse Moon", AttackType.STAB, Special.BLINK, 0x2A2A2A, 0xE0C040));
        b.quest("perilous_moons", "Perilous Moons", Difficulty.MASTER, 2)
                .reqQuest("twilights_promise")
                .req(Skill.SLAYER, 48).req(Skill.HUNTER, 20).req(Skill.FISHING, 20).req(Skill.RUNECRAFT, 20).req(Skill.CONSTRUCTION, 10)
                .xp(Skill.SLAYER, 40000).xp(Skill.RUNECRAFT, 5000).xp(Skill.HUNTER, 5000).xp(Skill.FISHING, 5000) // approximate
                .unlock("The Moons of Peril boss encounter in Neypotzli")
                .stage("I can start this quest by speaking to Attala in Cam Torum. (Requires Twilight's Promise, 48 Slayer, 20 Hunter, 20 Fishing, 20 Runecraft, 10 Construction.)")
                .talk("attala").say("The Twilight Emissaries have taken over the shrines of the moons in Neypotzli. Their champions now wear the moons' power.")
                .stage("I should go to Neypotzli and speak to the Lunar scholar.")
                .reach("neypotzli", 10).stay().unlessFlag("pm_camps").need("hammer").flag("pm_camps")
                .say("* With the building supplies from the crate you build the three camp sites in the corners of Neypotzli.")
                .missing("* The camp sites need building: bring a hammer.")
                .talk("quest_lunar_scholar").needFlag("pm_camps").missing("We can't go deeper without camps to fall back to.").say("Three champions: the Blood Moon, the Blue Moon and the Eclipse Moon. Each must be defeated at its own shrine.")
                .stage("I must defeat the Blood Moon, the Blue Moon and the Eclipse Moon.")
                .kill("quest_blood_moon").stay().unlessFlag("pm_blood").flag("pm_blood").say("* The Blood Moon falls.")
                .kill("quest_blue_moon").stay().unlessFlag("pm_blue").flag("pm_blue").say("* The Blue Moon shatters.")
                .kill("quest_eclipse_moon").stay().unlessFlag("pm_eclipse").flag("pm_eclipse").say("* The Eclipse Moon fades.")
                .talk("quest_lunar_scholar").needFlag("pm_blood", "pm_blue", "pm_eclipse").say("All three moons are free of the cult. The shrines are ours again.")
                .missing("The moons' champions still stand.")
                .stage("I should report to Attala in Cam Torum.")
                .talk("attala").needFlag("pm_blood", "pm_blue", "pm_eclipse").say("The moons are safe, and the Emissaries scattered. Cam Torum is in your debt.");
    }

    private static void atFirstLight(QuestBook b) {
        Cast.npc(b, "guildmaster_apatura", "Guildmaster Apatura", "zeah_hunter_guild", 0, 3, Cast.TAN, 0x5A6A2A);
        Cast.npc(b, "quest_renu_keeper", "Quetzal keeper", "avium_savannah", 2, 2, Cast.BROWN, 0x2A8A6A);
        b.item("quest_quetzal_feed", "Quetzal feed", "bait", 0xC0A040, "Food fit for a quetzal chick.");
        b.quest("at_first_light", "At First Light", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.HUNTER, 4500).xp(Skill.CONSTRUCTION, 800).xp(Skill.HERBLORE, 500) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun", "eagles_peak")
                .req(Skill.HUNTER, 46).req(Skill.HERBLORE, 30).req(Skill.CONSTRUCTION, 27)
                .unlock("Hunter Guild rumours; quetzal whistle")
                .stage("I can start this quest by speaking to Guildmaster Apatura in the Hunter Guild. (Requires Children of the Sun, Eagles' Peak, 46 Hunter, 30 Herblore, 27 Construction.)")
                .talk("guildmaster_apatura").say("A quetzal chick has been found alone on the Avium Savannah. Its mother is missing. Help the keeper look after it.")
                .stage("I should make quetzal feed (a raw karambwanji and a guam leaf) and take it to the quetzal keeper on the Avium Savannah.")
                .talk("quest_renu_keeper").take("raw_karambwanji", "guam_leaf").give("quest_quetzal_feed")
                .say("*You mash the feed together.* Now build a nest from those branches and feed the chick at first light.")
                .missing("The chick eats karambwanji mixed with a guam leaf.")
                .stage("I should build a nest (2 planks) on the savannah and feed the quetzal chick.")
                .reach("avium_savannah", 8).take("quest_quetzal_feed", "plank:2").need("hammer")
                .say("* At first light the chick eats from your hand, then spreads its wings and flies to the guild.")
                .stage("I should tell Guildmaster Apatura.")
                .talk("guildmaster_apatura").say("The chick is thriving! You've a hunter's heart. The guild's rumours are open to you.");
    }

    private static void ribbitingTale(QuestBook b) {
        Cast.npc(b, "quest_frog_foreman", "Frog foreman", "quest_civitas_lily_pond", 0, 2, Cast.FROG, 0x8A6A2A);
        Cast.npc(b, "quest_lily_pad_worker", "Lily pad worker", "quest_civitas_lily_pond", 3, -2, Cast.FROG, 0x2A6A8A);
        b.item("quest_lily_pad_contract", "Labour contract", "note", 0xE0E0B0, "A fair contract for the frogs of the lily pond.");
        b.quest("the_ribbiting_tale_of_a_lily_pad_labour_dispute", "The Ribbiting Tale of a Lily Pad Labour Dispute", Difficulty.NOVICE, 1)
                .reqQuest("children_of_the_sun").req(Skill.WOODCUTTING, 15)
                .xp(Skill.WOODCUTTING, 2000) // approximate
                .stage("I can start this quest by speaking to the Frog foreman at the lily pond in Civitas illa Fortis. (Requires Children of the Sun, 15 Woodcutting.)")
                .talk("quest_frog_foreman").say("Ribbit! The workers have downed tools. No more lily pads until they get better... ribbits. Talk to them!")
                .stage("I should hear the lily pad workers' grievances.")
                .talk("quest_lily_pad_worker").say("We want shade! Cut us some logs for a shelter and a fair contract, and we'll go back to work.")
                .stage("I should cut 5 logs for the workers' shelter and get the foreman to sign a fair contract.")
                .talk("quest_frog_foreman").take("logs:5").give("quest_lily_pad_contract").say("*Croak.* Fine, fine. Shade and fair hours. Sign here.")
                .missing("Ribbit. If they want a shelter, bring 5 logs.")
                .stage("I should bring the contract to the lily pad workers.")
                .talk("quest_lily_pad_worker").take("quest_lily_pad_contract").say("A fair deal! Back to the pads, everyone! Ribbit!");
    }

    private static void deathOnTheIsle(QuestBook b) {
        Cast.npc(b, "constantinius", "Constantinius", "quest_villa_lucens", 0, 2, Cast.TAN, 0x6A2A6A);
        Cast.npc(b, "quest_villa_steward", "Steward", "quest_villa_lucens", 3, -2, Cast.TAN, 0xE0E0D0);
        Cast.npc(b, "quest_aldarin_guard", "Aldarin guard", "zeah_aldarin", 3, 3, Cast.BROWN, 0x8A8A3A);
        b.item("quest_poisoned_goblet", "Goblet", "pot_item", 0xC0A040, "A goblet with a strange residue.");
        b.item("doti_butler_uniform", "Butler's uniform", "platebody", 0x2A2A2A, "A uniform for the servants of Villa Lucens.");
        b.quest("death_on_the_isle", "Death on the Isle", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.THIEVING, 10000).xp(Skill.AGILITY, 7500).xp(Skill.CRAFTING, 5000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun").req(Skill.THIEVING, 34).req(Skill.AGILITY, 32)
                .unlock("Villa Lucens wine cellar")
                .stage("I can start this quest by speaking to the Aldarin guard. (Requires Children of the Sun, 34 Thieving, 32 Agility.)")
                .talk("quest_aldarin_guard").say("There's been a death at Villa Lucens, at a party of all places. We need someone discreet to look into it.")
                .stage("I should go to Villa Lucens and question Constantinius, the host.")
                .reach("quest_villa_lucens", 8).stay().unless("doti_butler_uniform").unlessFlag("doti_inside").reqSkill(Skill.THIEVING, 34).give("doti_butler_uniform")
                .say("* You climb in through a house window, wait for the guard to move away and take a uniform from the wardrobe.")
                .talk("constantinius").need("doti_butler_uniform").flag("doti_inside")
                .missing("*The Head Butler turns you away: only staff in uniform may enter the villa.*").say("A guest collapsed during the toast. Poison, they say! In my house! Find who did this.")
                .stage("I should search the villa for clues and question the steward.")
                .reach("quest_villa_lucens", 8).stay().needFlag("doti_inside").unlessFlag("doti_cellar").reqSkill(Skill.AGILITY, 32).flag("doti_cellar")
                .say("* In the wine cellar you investigate the antique wine and find a man lying in the room to the east.")
                .talk("quest_villa_steward").needFlag("doti_cellar").missing("I've nothing to say. Have you even looked in the wine cellar?").give("quest_poisoned_goblet").say("I poured the wine myself! But... the goblet I served wasn't from our set. Someone swapped it.")
                .stage("I should take the goblet to the Aldarin guard and name the culprit.")
                .talk("quest_aldarin_guard").take("quest_poisoned_goblet").say("A swapped goblet... it was the victim's own business partner. Well done, detective.");
    }

    private static void meatAndGreet(QuestBook b) {
        Cast.npc(b, "quest_tal_teklan_chief", "Tal Teklan chieftain", "quest_tal_teklan", 0, 3, Cast.BROWN, 0x8A5A2A);
        Cast.npc(b, "quest_civitas_envoy", "Civitas envoy", "zeah_civitas_illa_fortis", -3, 3, Cast.TAN, 0xE0C060);
        b.item("quest_feast_platter", "Feast platter", "meat_cooked", 0xA0603A, "A feast fit for two peoples.");
        b.quest("meat_and_greet", "Meat and Greet", Difficulty.EXPERIENCED, 1)
                .xp(Skill.COOKING, 8000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun")
                .unlock("Trade with Tal Teklan")
                .stage("I can start this quest by speaking to the Civitas envoy in Civitas illa Fortis. (Requires Children of the Sun.)")
                .talk("quest_civitas_envoy").say("The people of Tal Teklan distrust the city. A shared feast could change that. Will you cook it?")
                .stage("I should cook a feast platter (2 lobsters, 2 trout and some bread) for the meeting.")
                .talk("quest_civitas_envoy").take("lobster:2", "trout:2", "bread").give("quest_feast_platter").say("*You arrange the platter.* That smells wonderful. Take it to Tal Teklan.")
                .missing("A feast needs 2 lobsters, 2 trout and some bread.")
                .stage("I should take the feast to the Tal Teklan chieftain.")
                .talk("quest_tal_teklan_chief").take("quest_feast_platter").say("The city sends food made by an outlander's hands? Hm. It is good food. Tal Teklan will talk.");
    }

    private static void heartOfDarkness(QuestBook b) {
        Cast.npc(b, "quest_ralos_priest", "Priest of Ralos", "quest_ralos_rise", 0, 3, Cast.TAN, 0xE0A040);
        b.item("quest_sun_key", "Sun key", "key", 0xE0C040, "A key marked with the sun of Ralos.");
        b.boss(BossDef.of("quest_twilight_emissary", "Twilight Emissary").level(210).stats(300, 160, 160, 150, 180, 150).speed(4)
                .defenceBonus(90, 90, 90, 120, 90)
                .attack(AttackDef.of("moon_blade", AttackType.SLASH, 24, 3, 5))
                .attack(AttackDef.of("dusk_bolt", AttackType.MAGIC, 22, 9, 4).particle("minecraft:soul"))
                .attack(AttackDef.of("eclipse", AttackType.MAGIC, 16, 9, 2).special(Special.AOE, 4).telegraph(2, "The Emissary blots out the light!"))
                .drops(Drop.always("bones")).skin(0xC8946A, 0x2A1A4A, 0x1A0A2A, 0xC0C0E0)
                .at("quest_tower_of_ascension").arena(11, "crypt"));
        b.quest("the_heart_of_darkness", "The Heart of Darkness", Difficulty.EXPERIENCED, 2)
                .reqQuest("twilights_promise").req(Skill.MINING, 55).req(Skill.THIEVING, 48).req(Skill.SLAYER, 48).req(Skill.AGILITY, 46)
                .xp(Skill.MINING, 8000).xp(Skill.THIEVING, 8000).xp(Skill.SLAYER, 8000).xp(Skill.AGILITY, 8000) // approximate
                .unlock("Access to the Tower of Ascension")
                .stage("I can start this quest by speaking to Prince Itzla Arkan at the Sunrise Palace. (Requires Twilight's Promise, 55 Mining, 48 Thieving, 48 Slayer, 46 Agility.)")
                .talk("prince_itzla_arkan").say("The Emissaries have retreated to the Tower of Ascension. To enter, we need the blessing of the priests of Ralos.")
                .stage("I should seek the Priest of Ralos at Ralos' Rise.")
                .talk("quest_ralos_priest").give("quest_sun_key").say("The Tower was sealed long ago. This key, blessed by the sun, opens it. Do not let the darkness take you.")
                .stage("I should climb the Tower of Ascension and confront the Twilight Emissary at its heart.")
                .reach("quest_tower_of_ascension", 8).need("quest_sun_key").say("* The key turns. Trials of agility and strength lead up the tower, and at the top waits the Emissary.")
                .stage("I must defeat the Twilight Emissary.")
                .kill("quest_twilight_emissary").say("* The Emissary falls. The darkness at the tower's heart fades.")
                .stage("I should return to Prince Itzla Arkan.")
                .talk("prince_itzla_arkan").take("quest_sun_key").say("The tower is ours again. Varlamore will remember your courage.");
    }

    private static void ethicallyAcquiredAntiquities(QuestBook b) {
        Cast.npc(b, "quest_varlamore_delegate", "Varlamorian delegate", "varrock_museum", -3, -3, Cast.TAN, 0xC0802A);
        b.item("quest_varlamore_relic", "Varlamorian relic", "statue", 0xE0B040, "A relic that belongs to Varlamore.");
        b.quest("ethically_acquired_antiquities", "Ethically Acquired Antiquities", Difficulty.NOVICE, 1)
                .xp(Skill.THIEVING, 6000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun", "shield_of_arrav").req(Skill.THIEVING, 25)
                .unlock("New Varlamorian displays in the Varrock Museum")
                .stage("I can start this quest by speaking to the Varlamorian delegate in the Varrock Museum. (Requires Children of the Sun, Shield of Arrav, 25 Thieving.)")
                .talk("quest_varlamore_delegate").say("Your museum displays a relic stolen from Varlamore. We want it back, and your curator won't listen.")
                .stage("I should persuade Curator Haig Halen to return the relic.")
                .talk("curator_haig_halen").reqSkill(Skill.THIEVING, 25).give("quest_varlamore_relic")
                .missing("*He won't hand it over. With 25 Thieving you could pickpocket his storeroom key.*").say("Stolen? Our records show... oh dear. Very well. Take it back to them, quietly please.")
                .stage("I should return the relic to the Varlamorian delegate.")
                .talk("quest_varlamore_delegate").take("quest_varlamore_relic").say("Returned at last. Varlamore will lend the museum other pieces, honestly this time.");
    }

    private static void scrambled(QuestBook b) {
        Cast.npc(b, "quest_egg_collector", "Egg collector", "zeah_hunter_guild", 3, -3, Cast.TAN, 0xE0D0A0);
        b.items(s -> s.add("quest_quetzal_egg", "Quetzal egg", Kind.SIMPLE, "pot_item", 0xE0E0C0).value(0)
                .examine("A large speckled egg. Handle with care."));
        b.quest("scrambled", "Scrambled!", Difficulty.INTERMEDIATE, 1)
                .req(Skill.CONSTRUCTION, 38).req(Skill.COOKING, 36).req(Skill.SMITHING, 35)
                .xp(Skill.CONSTRUCTION, 5000).xp(Skill.COOKING, 5000).xp(Skill.SMITHING, 5000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun")
                .stage("I can start this quest by speaking to the Egg collector at the Hunter Guild. (Requires Children of the Sun, 38 Construction, 36 Cooking, 35 Smithing.)")
                .talk("quest_egg_collector").say("Someone's been raiding the quetzal nests on the savannah! Find the stolen eggs before they hatch somewhere awful.")
                .stage("I should search the Avium Savannah for the stolen eggs.")
                .reach("avium_savannah", 8).give("quest_quetzal_egg:3").say("* Hidden in the grass you find three eggs, still warm.")
                .stage("I should return the eggs to the Egg collector.")
                .talk("quest_egg_collector").take("quest_quetzal_egg:3").say("All three, unbroken! The quetzals will be very grateful. Well, as grateful as birds get.");
    }

    private static void shadowsOfCustodia(QuestBook b) {
        Cast.npc(b, "quest_custodia_warden", "Custodia warden", "quest_custodia_pass", 0, 2, Cast.BROWN, 0x3A4A3A);
        b.quest("shadows_of_custodia", "Shadows of Custodia", Difficulty.EXPERIENCED, 2)
                .reqQuest("children_of_the_sun")
                .req(Skill.SLAYER, 54).req(Skill.FISHING, 45).req(Skill.CONSTRUCTION, 41).req(Skill.HUNTER, 36)
                .xp(Skill.SLAYER, 10000).xp(Skill.HUNTER, 4000).xp(Skill.FISHING, 3000).xp(Skill.CONSTRUCTION, 3000) // OSRS (current quest XP rewards)
                .unlock("Access through the Custodia Pass")
                .stage("I can start this quest by speaking to the Custodia warden at the Custodia Pass. (Requires Children of the Sun, 54 Slayer, 45 Fishing, 41 Construction, 36 Hunter.)")
                .talk("quest_custodia_warden").say("Shadows move in the pass at night: wolves that don't cast shadows of their own. Clear them out.")
                .stage("I must hunt down the shadow wolves in the Custodia Pass.")
                .kill("wolf").say("* The wolf dissolves into smoke. The pass grows quiet.")
                .stage("I should report to the Custodia warden.")
                .talk("quest_custodia_warden").take("willow_longbow:4", "maple_logs:4").need("hammer")
                .missing("Before nightfall the guards need 4 willow longbows, and the west wall needs 4 maple logs and a hammer.")
                .say("The shadows are gone. The pass is open to travellers again, thanks to you.");
    }

    private static void finalDawn(QuestBook b) {
        b.quest("the_final_dawn", "The Final Dawn", Difficulty.MASTER, 3)
                .req(Skill.THIEVING, 66).req(Skill.FLETCHING, 52).req(Skill.RUNECRAFT, 52)
                .xp(Skill.THIEVING, 55000).xp(Skill.FLETCHING, 25000).xp(Skill.RUNECRAFT, 25000) // OSRS (current quest XP rewards)
                .reqQuest("the_heart_of_darkness", "perilous_moons")
                .unlock("The conclusion of the Varlamore storyline")
                .stage("I can start this quest by speaking to Prince Itzla Arkan at the Sunrise Palace. (Requires The Heart of Darkness, Perilous Moons, 66 Thieving, 52 Fletching, 52 Runecraft.)")
                .talk("prince_itzla_arkan").say("The Emissaries mean to plunge Varlamore into eternal twilight. This is the final battle. Will you stand with us?")
                .stage("I should rally the guard of Civitas illa Fortis.")
                .reach("quest_tower_of_ascension", 10).stay().unlessFlag("tfd_temple").reqSkill(Skill.THIEVING, 66).flag("tfd_temple")
                .say("* In emissary robes from the chest at the Tower of Ascension you enter the Twilight Temple (passcode: Final Dawn).",
                        "* Behind the painting you picklock the blue chest and take an emissary scroll: the plan of attack.")
                .talk("quest_civitas_guard_captain").needFlag("tfd_temple").missing("Rally the guard on a rumour? Bring me proof of the Emissaries' plans.").say("The guard is ready. To the Fortis Colosseum: the Emissaries gather there.")
                .stage("I must break the Emissaries' assault at the Fortis Colosseum.")
                .reach("fortis_colosseum", 10).flag("tfd_battle").say("* The battle rages until dawn breaks over the colosseum. The Emissaries are routed.")
                .stage("I should return to Prince Itzla Arkan.")
                .talk("prince_itzla_arkan").needFlag("tfd_battle").say("The sun rises on a free Varlamore. You will always have a home here, friend.");
    }
}
