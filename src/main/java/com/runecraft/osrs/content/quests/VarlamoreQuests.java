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
 * Dawn. Quest points and requirements follow OSRS; XP-lamp rewards are real lamps ({@link QuestLamps}) and the
 * later quests' details are approximate.
 */
final class VarlamoreQuests {
    private VarlamoreQuests() {
    }

    static void register(QuestBook b) {
        b.place("quest_cam_torum", "Cam Torum", 1440, 3110, PlaceDef.Style.CAVE);
        b.spot("quest_villa_lucens", "Villa Lucens", 1450, 2968);
        b.place("quest_tal_teklan", "Tal Teklan", 1225, 3090, PlaceDef.Style.CAMP);
        // the Tower of Ascension east of the Quetzacalli Gorge (quest-helper: Forebearer Janus inside at 1638, 3224; it stood over the
        // Gorge's pub at 1500, 3230 before)
        b.spot("quest_tower_of_ascension", "Tower of Ascension", 1638, 3228); // approx: 4 tiles north, clear of the lift platform hut
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
        b.npcAt("ennius_tullus", "Ennius Tullus", 1687, 3141, 0xC8946B, 0xE0C060, 0x8A6A3A, 0x6A2A2A); // in front of the Sunrise Palace
        b.item("twilight_cult_orders", "Cult orders", "note", 0x3A2A5A, "Orders from the Twilight Emissaries.");
        b.quest("twilights_promise", "Twilight's Promise", Difficulty.INTERMEDIATE, 1)
                .reqQuest("children_of_the_sun")
                .xp(Skill.THIEVING, 3000) // approximate
                .unlock("Fortis Colosseum fights; the Twilight Emissaries' hideout")
                .stage("I can start this quest by speaking to Ennius Tullus in front of the Sunrise Palace. (Requires Children of the Sun.)")
                .talk("ennius_tullus").say("A cult calling themselves the Twilight Emissaries plans to strike during the colosseum games. The prince must be warned.")
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
        b.npcAt("marcellus", "Marcellus", 1683, 2973, 0xC8946C, 0x3A6A8A, 0x2A4A5A, 0xE0D0A0); // at the Locus Oasis
        b.item("quest_lily_pad_contract", "Labour contract", "note", 0xE0E0B0, "A fair contract for the frogs of the lily pond.");
        b.quest("the_ribbiting_tale_of_a_lily_pad_labour_dispute", "The Ribbiting Tale of a Lily Pad Labour Dispute", Difficulty.NOVICE, 1)
                .reqQuest("children_of_the_sun").req(Skill.WOODCUTTING, 15)
                .xp(Skill.WOODCUTTING, 2000) // approximate
                .stage("I can start this quest by speaking to Marcellus at the Locus Oasis. (Requires Children of the Sun, 15 Woodcutting.)")
                .talk("marcellus").say("The frogs at the lily pond have downed tools, and my business sinks with them. Talk to their foreman!")
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
        b.npcAt("emelio", "Emelio", 1753, 3074, 0xA86A46, 0xA05A3A, 0x6A3A2A, 0x2A1A16); // his home in Outer Fortis
        b.item("quest_feast_platter", "Feast platter", "meat_cooked", 0xA0603A, "A feast fit for two peoples.");
        b.quest("meat_and_greet", "Meat and Greet", Difficulty.EXPERIENCED, 1)
                .xp(Skill.COOKING, 8000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun")
                .unlock("Trade with Tal Teklan")
                .stage("I can start this quest by speaking to Emelio at his home in Outer Fortis. (Requires Children of the Sun.)")
                .talk("emelio").say("The people of Tal Teklan distrust the city. A shared feast could change that.",
                        "@Civitas envoy: Will you cook it? Come to me in the city when you're ready.")
                .stage("I should cook a feast platter (2 lobsters, 2 trout and some bread) for the meeting.")
                .talk("quest_civitas_envoy").take("lobster:2", "trout:2", "bread").give("quest_feast_platter").say("*You arrange the platter.* That smells wonderful. Take it to Tal Teklan.")
                .missing("A feast needs 2 lobsters, 2 trout and some bread.")
                .stage("I should take the feast to the Tal Teklan chieftain.")
                .talk("quest_tal_teklan_chief").take("quest_feast_platter").say("The city sends food made by an outlander's hands? Hm. It is good food. Tal Teklan will talk.");
    }

    /**
     * The Heart of Darkness after the wiki quick guide and quest-helper (TheHeartOfDarkness): the Quetzacalli Gorge's pub and
     * shopkeeper, the recruits at the Tower of Ascension, Forebearer Janus's four trials (the chests' keyword and directions and
     * the scraps' passcode, the waves of emissaries, the traitor who speaks of Mictl, Prince Itzla himself), the Twilight Temple,
     * the Ruins of Tapoyauik (the four levers and the elemental statues) and Amoxliatl. OSRS fights no "Twilight Emissary" boss
     * here: the emissaries are the second trial's waves.
     */
    private static void heartOfDarkness(QuestBook b) {
        // quest-helper tiles: the Gorge's pub (bartender 1499, 3224; the rented room 1505, 3223), the general store (1517, 3223),
        // the prince outside the Tower of Ascension (1656, 3219), Forebearer Janus inside it (1638, 3224; approx 1638, 3228 here,
        // since the lift platform hut stands on his tile), the Twilight Temple
        // (Forebearer Fides 1685, 3247) and the stairs down to the Ruins of Tapoyauik (1693, 3231)
        b.npcAt("hod_bartender", "Bartender", 1499, 3224, 0xC8946C, 0x6A4A2A, 0x4A3A1A, 0x8A6A3A);
        b.npcAt("itzla_gorge", "Prince Itzla Arkan", 1505, 3223, 0xC8946D, 0xE0B041, 0x9A7A2B, 0xE0C060);
        b.npcAt("hod_shopkeeper", "Shopkeeper", 1517, 3223, 0xC8946E, 0x5A6A3A, 0x3A4A2A, 0x6A5A2A);
        b.npcAt("itzla_tower", "Prince Itzla Arkan", 1656, 3219, 0xC8946F, 0xE0B042, 0x9A7A2C, 0xE0C061);
        b.npcAt("forebearer_janus", "Forebearer Janus", 1638, 3228, 0xC8946A, 0x2A1A4A, 0x1A0A2A, 0xC0C0E0);
        b.npcAt("forebearer_fides", "Forebearer Fides", 1685, 3247, 0xC8946B, 0x2A1A4B, 0x1A0A2B, 0xC0C0E1);
        // Cast (one serial, as the priest of Ralos it replaces took) so later NPCs keep their colours
        Cast.npcAt(b, "servius_teokan", "Servius, Teokan of Ralos", 1683, 3163, Cast.TAN, 0xE0D060);
        b.spot("hod_tapoyauik", "Ruins of Tapoyauik", 1693, 3231);
        b.item("hod_tower_key", "Tower key", "key", 0x8A8A9A, "A key to the Tower of Ascension's cells.");
        b.item("hod_book", "Book", "book", 0x3A2A5A, "An emissary's book, some of its words marked.");
        b.item("hod_scrap_of_paper", "Scrap of paper", "note", 0xE0D8C0, "Half of a torn note.");
        b.item("hod_emissary_robes", "Twilight emissary robes", "robe_top", 0x2A1A4A, "Robes of the Twilight Emissaries.");
        // The second trial's waves (osrs-dps-calc, Emissary Ascended: level 117, 80 HP, up to 12 every 4 ticks, melee and magic)
        b.boss(BossDef.of("quest_twilight_emissary", "Emissary Ascended").level(117).stats(80, 120, 110, 90, 40, 1).speed(4)
                .defenceBonus(30, 30, 50, 20, 30)
                .attack(AttackDef.of("moon_blade", AttackType.SLASH, 12, 2.5, 3))
                .attack(AttackDef.of("dusk_bolt", AttackType.MAGIC, 12, 9, 2).particle("minecraft:soul")) // approx: the casters' max hit
                .attack(AttackDef.of("eclipse", AttackType.MAGIC, 12, 9, 1).special(Special.AOE, 3) // the dodgeable 3x3
                        .telegraph(2, "The emissary blots out the light around you: step out!"))
                .drops(Drop.always("bones")).skin(0xC8946A, 0x2A1A4A, 0x1A0A2A, 0xC0C0E0));
        // Prince Itzla Arkan, the final trial (level 167, 250 HP, up to 12; a sword swipe that turns protection prayers off and a
        // telegraphed area attack dodged behind him)
        b.boss(BossDef.of("hod_prince_itzla", "Prince Itzla Arkan").level(167).stats(250, 150, 110, 75, 55, 1).speed(4)
                .defenceBonus(30, 30, 20, 10, 30)
                .attack(AttackDef.of("sword", AttackType.SLASH, 12, 2.5, 6))
                .attack(AttackDef.of("sword_swipe", AttackType.SLASH, 12, 3, 1).special(Special.PRAYER_PIERCING, 0)
                        .telegraph(2, "Prince Itzla: Your prayers won't save you!"))
                .attack(AttackDef.of("sunburst", AttackType.MAGIC, 12, 3, 1).special(Special.AOE, 3) // approx: its damage
                        .telegraph(3, "Prince Itzla raises his blade to the sun: get behind him!"))
                .drops(Drop.always("bones")).skin(0xC8946A, 0xE0B040, 0x9A7A2A, 0xE0C060).size(0.6f, 1.95f, 1.0f));
        // Amoxliatl, quest version (osrs-dps-calc: level 226, 400 HP, magic 140, defence 60, +200 ranged defence, up to 16)
        b.boss(BossDef.of("hod_amoxliatl", "Amoxliatl").level(226).stats(400, 1, 1, 60, 140, 1).speed(8)
                .defenceBonus(100, 100, 40, 100, 200)
                .attack(AttackDef.of("frost_bolt", AttackType.MAGIC, 16, 10, 5).particle("minecraft:snowflake"))
                .attack(AttackDef.of("icicle_crash", AttackType.MAGIC, 16, 10, 2).special(Special.SKY_STRIKE, 0) // approx: wiki gives no max
                        .telegraph(3, "Icicles hang above you: step away from the spikes!"))
                .attack(AttackDef.of("ice_pool", AttackType.MAGIC, 6, 10, 1).special(Special.HAZARD, 3)
                        .telegraph(2, "Ice pools spread across the floor!"))
                .drops(Drop.always("bones")).model("amoxliatl").size(1.2f, 1.4f, 1.8f));
        b.quest("the_heart_of_darkness", "The Heart of Darkness", Difficulty.EXPERIENCED, 2)
                .reqQuest("twilights_promise").req(Skill.MINING, 55).req(Skill.THIEVING, 48).req(Skill.SLAYER, 48).req(Skill.AGILITY, 46)
                .xp(Skill.MINING, 8000).xp(Skill.THIEVING, 8000).xp(Skill.SLAYER, 8000).xp(Skill.AGILITY, 8000) // approximate
                .unlock("Access to the Tower of Ascension")
                .stage("I can start this quest by speaking to Prince Itzla Arkan at the Sunrise Palace. (Requires Twilight's Promise, 55 Mining, 48 Thieving, 48 Slayer, 46 Agility.)")
                .talk("prince_itzla_arkan").say("The Emissaries are recruiting in the Tower of Ascension. I mean to join them, in disguise, and "
                        + "learn what they plan. Meet me at the pub in the Quetzacalli Gorge.")
                // ---- Starting out ----
                .stage("I should meet the prince at the pub in the Quetzacalli Gorge, then join the recruits at the Tower of Ascension.")
                .talk("hod_bartender").stay().unlessFlag("hod_room").take("coins:30").flag("hod_room")
                .say("> I'd like to rent the basement room.", "Thirty coins. There's no basement, mind: it's the room on the south-east.")
                .missing("The room's thirty coins.")
                .talk("itzla_gorge").stay().needFlag("hod_room").unlessFlag("hod_rested").flag("hod_rested")
                .say("* You rest on the bed. The prince slips in.", "We need someone who knows the Emissaries. Try the shopkeeper east of here.")
                .talk("hod_shopkeeper").stay().needFlag("hod_rested").unlessFlag("hod_shop").flag("hod_shop")
                .say("> I hear you can offer help to those in need.", "Recruits gather outside the Tower of Ascension, east past the proudspire.")
                .talk("itzla_tower").stay().needFlag("hod_shop").unlessFlag("hod_recruits").flag("hod_recruits")
                .say("> Alright, I'll get mingling.", "* You talk to the recruits Nova, Sergius, Felius and Caritta outside the tower.",
                        "Ximoua, the darkness they worship... Inside, Forebearer Janus will test us.")
                .talk("forebearer_janus").needFlag("hod_recruits")
                .say("Recruits. Four trials stand between you and the Emissaries' trust. Climb the stairs.")
                .missing("Only recruits may enter.")
                // ---- The four trials ----
                .stage("I must pass Forebearer Janus's four trials in the Tower of Ascension.")
                .reach("quest_tower_of_ascension", 8).stay().unlessFlag("hod_trial1").unless("hod_tower_key", "hod_book")
                .reqSkill(Skill.THIEVING, 48).give("hod_tower_key")
                .say("* On the first floor you pickpocket an emissary ascended in the cage for a tower key.")
                .missing("* Pickpocketing the emissary ascended takes 48 Thieving.")
                .reach("quest_tower_of_ascension", 8).stay().unlessFlag("hod_trial1").take("hod_tower_key").give("hod_book", "hod_scrap_of_paper")
                .say("* The key opens the south-east gate. In the chest: a book with marked words, and a scrap of paper.")
                .puzzle("quest_tower_of_ascension", HOD_KEYWORD_CHEST).stay().need("hod_book").unlessFlag("hod_keyword").unlessFlag("hod_trial1")
                .flag("hod_keyword")
                .say("* The south-western chest clicks open: inside lies a poem.")
                .talk("itzla_tower").stay().needFlag("hod_keyword").unlessFlag("hod_poem").unlessFlag("hod_trial1").flag("hod_poem")
                .say("Those are old Varlamorian words: takam is east, makt north, silam west and uitt south.")
                .puzzle("quest_tower_of_ascension", HOD_DIRECTION_CHEST).stay().needFlag("hod_poem").unlessFlag("hod_trial1")
                .take("hod_book", "hod_scrap_of_paper").flag("hod_trial1")
                .say("* In the north-western chest lies the other half of the scrap. Together they read a passcode, and you speak it to Janus.")
                .reach("quest_tower_of_ascension", 8).stay().needFlag("hod_trial1").unlessFlag("hod_trial2").spawn("quest_twilight_emissary")
                .say("* Janus: The second trial. Defend your prince! Emissaries pour into the room.")
                // wiki: melee and magic emissaries two at a time, then four at a time // approx: six in all
                .kill("quest_twilight_emissary", 6).stay().needFlag("hod_trial1").unlessFlag("hod_trial2").flag("hod_trial2")
                .say("* The last emissary falls. You bind the prince's wounds with bandages from the medical boxes.")
                .puzzle("quest_tower_of_ascension", HOD_TRAITOR).stay().needFlag("hod_trial2").unlessFlag("hod_trial3").flag("hod_trial3")
                .say("Janus: Well spotted. The Emissaries have no use for Mictl's faithful. One trial remains.")
                .reach("quest_tower_of_ascension", 8).stay().needFlag("hod_trial3").spawn("hod_prince_itzla")
                .say("* Janus: For the last trial, fight one of your own. Prince Itzla draws his blade!")
                .kill("hod_prince_itzla").needFlag("hod_trial3")
                .say("* The prince yields. Janus is satisfied: you are Emissaries now.")
                // ---- The Twilight Temple and the Ruins of Tapoyauik ----
                .stage("I should enter the Twilight Temple in emissary robes, then follow the Emissaries into the Ruins of Tapoyauik.")
                .talk("forebearer_janus").stay().unless("hod_emissary_robes").unlessFlag("hod_temple").give("hod_emissary_robes")
                .say("Take robes from the chest. The temple lies to the east.")
                .talk("forebearer_fides").stay().unlessFlag("hod_temple").need("hod_emissary_robes").flag("hod_temple")
                .say("* In the temple the Emissaries gather. The prince stays close.", "The heart of darkness waits below the ruins to "
                        + "the south. Go, and wake her.")
                .missing("Only Emissaries enter here.")
                .reach("hod_tapoyauik", 6).stay().needFlag("hod_temple").unlessFlag("hod_levers").needTool("pickaxe").reqSkill(Skill.MINING, 55)
                .reqSkill(Skill.AGILITY, 46).flag("hod_levers")
                .say("* Down in the ruins you mine through the rocks, slip past frost nagua, frost crabs and traps over icy ledges and "
                        + "frozen platforms, and pull the four levers. The braziers light and the great gate opens.")
                .missing("* Rocks block the ruins: you'll need a pickaxe (there's one in a barrel) and 55 Mining.")
                .puzzle("hod_tapoyauik", HOD_STATUES).stay().needFlag("hod_levers").unlessFlag("hod_statues").flag("hod_statues")
                .say("* The fourth statue glows and the frozen door cracks open.")
                .reach("hod_tapoyauik", 6).stay().needFlag("hod_statues").spawn("hod_amoxliatl")
                .say("* Beyond the frozen door, Amoxliatl stirs!")
                .kill("hod_amoxliatl").needFlag("hod_statues")
                .say("* Amoxliatl shatters into shards of ice. The Emissaries' plan lies in pieces.")
                .stage("I should report to Servius, Teokan of Ralos, at the Sunrise Palace.")
                .talk("servius_teokan").say("Amoxliatl, slain? The Emissaries will not recover from this. Varlamore thanks you.");
    }

    /**
     * The south-western chest's keyword (wiki: the first letters of the book's marked words; it differs per player). // approx:
     * the keywords and the book's marked words
     */
    static final QuestPuzzle HOD_KEYWORD_CHEST = hodKeywordChest();

    private static QuestPuzzle hodKeywordChest() {
        String[][] books = {{"DUSK", "Darkness", "Upon", "Sunset", "Knows"}, {"NIGHT", "Never", "Is", "Glory", "Hidden", "Too"},
                {"SHADE", "Silence", "Holds", "All", "Dawn's", "Ending"}, {"MOONS", "Many", "Offer", "Only", "Night's", "Sorrow"}};
        QuestPuzzle.Builder b = QuestPuzzle.builder("hod_keyword_chest").room("chest");
        for (String[] book : books) {
            StringBuilder words = new StringBuilder();
            for (int i = 1; i < book.length; i++) {
                words.append(i > 1 ? ", " : "").append('\'').append(book[i]).append('\'');
            }
            b.variant(null).prompt("* The south-western chest has a lock of letters. In the book, these words are marked in order: " + words + ".")
                    .options(QuestPuzzle.letters()).solution(QuestPuzzle.word(book[0]));
        }
        return b.failLines("* The chest stays locked.").build();
    }

    /** The north-western chest (wiki: the poem's marked words as directions: takam east, makt north, silam west, uitt south). */
    static final QuestPuzzle HOD_DIRECTION_CHEST = hodDirectionChest();

    private static QuestPuzzle hodDirectionChest() {
        String[] words = {"makt", "takam", "uitt", "silam"};
        int[][] codes = {{1, 0, 3, 2}, {3, 1, 1, 0}, {0, 2, 1, 3}, {2, 3, 0, 1}, {1, 1, 2, 0}};
        QuestPuzzle.Builder b = QuestPuzzle.builder("hod_direction_chest").room("chest");
        for (int[] code : codes) {
            StringBuilder marked = new StringBuilder();
            for (int i = 0; i < code.length; i++) {
                marked.append(i > 0 ? ", " : "").append(words[code[i]]);
            }
            b.variant(null).prompt("* The north-western chest's lock turns north, east, south or west. In the poem these words are marked, in "
                            + "order: " + marked + ".")
                    .options("North", "East", "South", "West").solution(code);
        }
        return b.failLines("* The lock spins back.").build();
    }

    /** The third trial (wiki: the traitor is the cultist who speaks of Mictl, whom the Emissaries don't follow). // approx: their words */
    static final QuestPuzzle HOD_TRAITOR = hodTraitor();

    private static QuestPuzzle hodTraitor() {
        String[] names = {"Tenoch", "Silia", "Adrius", "Eleuia"};
        String[] loyal = {"The twilight will cover Varlamore, and Ximoua will rise.", "I gave up my name at the tower's door, as we all did.",
                "The sun has blinded this land too long.", "When the moons are ours, the Emissaries will rule the dusk."};
        QuestPuzzle.Builder b = QuestPuzzle.builder("hod_traitor").room("trial");
        for (int traitor : new int[]{0, 1, 3}) {
            java.util.List<String> prompt = new java.util.ArrayList<>();
            prompt.add("Janus: One of these four is no Emissary. Question them, then accuse the traitor.");
            for (int i = 0; i < 4; i++) {
                prompt.add("@" + names[i] + ": " + (i == traitor ? "Mictl watches over us, even here in the dark." : loyal[i]));
            }
            b.variant(null).prompt(prompt.toArray(String[]::new))
                    .options("Accuse Tenoch", "Accuse Silia", "Accuse Adrius", "Accuse Eleuia").solution(traitor);
        }
        return b.failLines("Janus: Wrong. A loyal Emissary, accused. Try again, recruit.").build();
    }

    /**
     * The Ruins of Tapoyauik's statues (wiki: each corner's marking begins with Zema, Oma, Yeya or Naui, one to four, the order to
     * touch the statues; it differs per player).
     */
    static final QuestPuzzle HOD_STATUES = hodStatues();

    private static QuestPuzzle hodStatues() {
        String[] elements = {"air", "water", "earth", "fire"};
        String[] numbers = {"Zema", "Oma", "Yeya", "Naui"};
        int[][] orders = {{2, 0, 3, 1}, {1, 3, 0, 2}, {3, 2, 1, 0}, {0, 2, 1, 3}, {2, 3, 1, 0}, {1, 0, 2, 3}};
        QuestPuzzle.Builder b = QuestPuzzle.builder("hod_statues").room("statues");
        for (int[] order : orders) {
            // order[e] is the number marked by element e's statue
            int[] touch = new int[4];
            StringBuilder marks = new StringBuilder();
            for (int e = 0; e < 4; e++) {
                touch[order[e]] = e;
                marks.append(e > 0 ? "; " : "").append("by the ").append(elements[e]).append(" statue, '").append(numbers[order[e]])
                        .append(" ...'");
            }
            b.variant(null).prompt("* With the coloured icons the four statues are whole again. The murals count in the old tongue: one is "
                                    + "Zema, two Oma, three Yeya and four Naui.",
                            "* The markings in the corners: " + marks + ". Touch the statues in order.")
                    .options("Touch the air statue", "Touch the water statue", "Touch the earth statue", "Touch the fire statue")
                    .solution(touch);
        }
        return b.failLines("* The statues dim. The frozen door stays shut.").build();
    }

    private static void ethicallyAcquiredAntiquities(QuestBook b) {
        Cast.npc(b, "quest_varlamore_delegate", "Varlamorian delegate", "varrock_museum", -3, -3, Cast.TAN, 0xC0802A);
        b.item("quest_varlamore_relic", "Varlamorian relic", "statue", 0xE0B040, "A relic that belongs to Varlamore.");
        b.quest("ethically_acquired_antiquities", "Ethically Acquired Antiquities", Difficulty.NOVICE, 1)
                .xp(Skill.THIEVING, 6000) // OSRS (current quest XP rewards)
                .reward("coins:5000") // wiki: 5,000 coins
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
        b.npcAt("alan_tal_teok", "Alan", 1247, 3167, 0xE0B898, 0x5A7A3A, 0x3A4A2A, 0x8A6A2A); // the temple of Tal Teok
        b.items(s -> s.add("quest_quetzal_egg", "Quetzal egg", Kind.SIMPLE, "pot_item", 0xE0E0C0).value(0)
                .examine("A large speckled egg. Handle with care."));
        b.quest("scrambled", "Scrambled!", Difficulty.INTERMEDIATE, 1)
                .req(Skill.CONSTRUCTION, 38).req(Skill.COOKING, 36).req(Skill.SMITHING, 35)
                .xp(Skill.CONSTRUCTION, 5000).xp(Skill.COOKING, 5000).xp(Skill.SMITHING, 5000) // OSRS (current quest XP rewards)
                .reqQuest("children_of_the_sun")
                .stage("I can start this quest by speaking to Alan at the temple of Tal Teok, north of Tal Teklan. (Requires Children of the Sun, 38 Construction, 36 Cooking, 35 Smithing.)")
                .talk("alan_tal_teok").say("Someone's been raiding the quetzal nests on the savannah! Find the stolen eggs before they hatch somewhere awful.")
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

    /**
     * The Final Dawn (wiki quick guide, quest-helper): Servius starts it; Infiltration II (the temple's passcode, the blue
     * chest's tumblers, the Emissary Enforcer), Home Invasion (Janus's safe house: the dog's GUS code, the makeshift blackjack),
     * Dwarven Traitor (Teumo's barrels), Interlopers in Neypotzli (Lucius and Chimalli), Crypt Assault (six waves, Ennius
     * Tullus), the Tonali Cavern's sun and moon puzzles, and Augur Metzli before Mokhaiotl.
     */
    private static void finalDawn(QuestBook b) {
        // quest-helper tiles: Queen Zyanyi on the palace's top floor (1673, 3156), Captain Vibia behind Janus's safe house
        // (1652, 3088; the house 1643-1652, 3091-3096), Servius at Tal Teklan (1236, 3105), the Crypt of Tonali's entrance
        // (1305, 3034: the spot stands south of its hut) and the Twilight Temple (1687, 3247)
        b.npcAt("queen_zyanyi", "Queen Zyanyi Arkan", 1673, 3156, 0xC8946A, 0xE0B043, 0x9A7A2D, 0xF0D070);
        b.npcAt("captain_vibia", "Captain Vibia", 1652, 3088, 0xC8946B, 0x8A2A2B, 0x5A1A1B, 0xC0C0C1);
        b.npcAt("servius_tal_teklan", "Servius, Teokan of Ralos", 1236, 3105, 0xC8947A, 0xE0D061, 0xA08A3B, 0xF0E081);
        b.npcAt("itzla_crypt", "Prince Itzla Arkan", 1307, 3024, 0xC8946F, 0xE0B044, 0x9A7A2E, 0xE0C062);
        b.npc("galna", "Galna", "quest_cam_torum", -3, 3, 0xB07A5A, 0x6A4A6A, 0x4A3A4A, 0xA08060);
        b.npc("eyatlalli", "Eyatlalli", "neypotzli", -3, 3, 0xD8C8B0, 0x3A6A9A, 0x2A4A6A, 0xE0E0FF);
        b.spot("tfd_twilight_temple", "Twilight Temple", 1687, 3250);
        b.spot("tfd_safehouse", "Janus's safe house", 1648, 3094);
        b.spot("tfd_crypt_of_tonali", "Crypt of Tonali", 1305, 3024);
        b.item("tfd_emissary_scroll", "Emissary scroll", "scroll", 0x3A2A5A, "The Emissaries' plans, in Metzli's hand.");
        b.item("tfd_empty_sack", "Empty sack", "cloth", 0xA08A5A, "It held potatoes. Now it holds nothing.");
        b.item("tfd_makeshift_blackjack", "Makeshift blackjack", "tool", 0x6A4A2A, "A branch and a coin purse full of sand. Crude.");
        b.item("tfd_keystone_fragment", "Keystone fragment", "gem", 0x8A6ACA, "It hums near the keystone it came from.");
        // osrs-dps-calc / wiki infoboxes: Emissary Enforcer 196 (126 HP, up to 18; the charge 30+ and a 3x3 sweep); the two
        // cultists 160 (180 HP, up to 24; Lucius's triple projectile 34 every third attack); Emissary brawler 86 (55 HP, up to 8);
        // Ennius Tullus 306 (380 HP, up to 22, melee only: he prays magic and missiles); Metzli 396 (600 HP, up to 28)
        b.boss(BossDef.of("tfd_emissary_enforcer", "Emissary Enforcer").level(196).stats(126, 220, 170, 100, 80, 1).speed(4)
                .defenceBonus(70, 80, 50, 180, 100)
                .attack(AttackDef.of("sweep", AttackType.SLASH, 18, 2.5, 4).special(Special.AOE, 2))
                .attack(AttackDef.of("charge", AttackType.SLASH, 30, 4, 1).special(Special.SKY_STRIKE, 0) // approx: "30+"
                        .telegraph(2, "Emissary Enforcer: Thief! (step aside!)"))
                .drops(Drop.always("bones")).skin(0xC8946A, 0x1A1A2A, 0x0A0A1A, 0x8A2A2A));
        b.boss(BossDef.of("tfd_lucius", "Lucius").level(160).stats(180, 70, 70, 70, 200, 60).speed(4)
                .defenceBonus(90, 120, 120, 200, 80)
                .attack(AttackDef.of("sun_bolt", AttackType.MAGIC, 24, 10, 6).particle("minecraft:flame"))
                .attack(AttackDef.of("triple_projectile", AttackType.MAGIC, 34, 10, 0).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Golden circles flare on the floor: step into the sun!"))
                .drops(Drop.always("bones")).skin(0xC8946B, 0xE0B045, 0x2A1A4A, 0xE0C063));
        b.boss(BossDef.of("tfd_chimalli", "Chimalli").level(160).stats(180, 70, 70, 70, 60, 200).speed(4)
                .defenceBonus(70, 80, 80, 120, 120)
                .attack(AttackDef.of("arrow", AttackType.RANGED, 24, 10, 6))
                .attack(AttackDef.of("fire_arrows", AttackType.RANGED, 24, 10, 1).special(Special.SKY_STRIKE, 0) // approx: their hit
                        .telegraph(3, "Shadows of falling arrows darken the ground: move!"))
                .drops(Drop.always("bones")).skin(0xC8946C, 0x6A2A1A, 0x2A1A4A, 0xE07030));
        b.boss(BossDef.of("tfd_emissary_brawler", "Emissary brawler").level(86).stats(55, 90, 70, 70, 15, 1).speed(5)
                .defenceBonus(30, 30, 20, 5, 30)
                .attack(AttackDef.of("club", AttackType.CRUSH, 8, 2.5, 1))
                .drops(Drop.always("bones")).skin(0xC8946D, 0x2A1A4A, 0x1A0A2A, 0x6A5A8A));
        b.boss(BossDef.of("tfd_ennius_tullus", "Ennius Tullus").level(306).stats(380, 280, 210, 75, 75, 1).speed(3)
                .defenceBonus(70, 60, 50, 100, 80).immune(AttackType.RANGED, AttackType.MAGIC)
                .attack(AttackDef.of("sword", AttackType.SLASH, 22, 2.5, 6))
                .attack(AttackDef.of("sunfire_blast", AttackType.MAGIC, 22, 12, 1).special(Special.SKY_STRIKE, 0) // approx: its hit
                        .telegraph(3, "Circles glow around the platform: stand in one!"))
                .attack(AttackDef.of("sunfire_smite", AttackType.SLASH, 22, 2.5, 1).special(Special.PRAYER_PIERCING, 0)
                        .telegraph(2, "Ennius Tullus: The sun sees through your prayers!"))
                // wiki: at 0 HP he heals 120 and attacks faster, his Sunfire Strike three rows wide // approx: a phase at 25%
                .phase(25, "Ennius Tullus: The sun burns within me!", 1.2, false,
                        AttackDef.of("sunfire_strike", AttackType.MAGIC, 22, 12, 2).special(Special.SKY_STRIKE, 0)
                                .telegraph(2, "Sunfire races out from Ennius in three rows: step off them!"))
                .drops(Drop.always("bones")).skin(0xC8946E, 0xE0B046, 0x8A2A2C, 0xE0C064));
        b.boss(BossDef.of("tfd_augur_metzli", "Augur Metzli").level(396).stats(600, 1, 1, 160, 180, 180).speed(3)
                .defenceBonus(60, 80, 70, 100, 20)
                .attack(AttackDef.of("sun_arrow", AttackType.RANGED, 28, 10, 5))
                .attack(AttackDef.of("moon_bolt", AttackType.MAGIC, 28, 10, 5).particle("minecraft:soul"))
                .attack(AttackDef.of("sunfire_waves", AttackType.MAGIC, 28, 12, 0).special(Special.SKY_STRIKE, 0) // approx: its hit
                        .telegraph(3, "Waves of sunfire sweep the chamber: find the row without fire!"))
                .phase(10, "Metzli conjures walls of fire in every direction! Take the white portal to the next quadrant!", 1.0, false,
                        AttackDef.of("quadrant_fire", AttackType.MAGIC, 28, 16, 1).special(Special.AOE, 6) // approx: its hit
                                .telegraph(3, "The flames turn on your quadrant: take the portal!"))
                .drops(Drop.always("bones")).skin(0xC8946F, 0x2A1A4A, 0x1A0A2A, 0xE0C065));
        b.quest("the_final_dawn", "The Final Dawn", Difficulty.MASTER, 3)
                .req(Skill.THIEVING, 66).req(Skill.FLETCHING, 52).req(Skill.RUNECRAFT, 52)
                .xp(Skill.THIEVING, 55000).xp(Skill.FLETCHING, 25000).xp(Skill.RUNECRAFT, 25000) // OSRS (current quest XP rewards)
                .reqQuest("the_heart_of_darkness", "perilous_moons")
                .unlock("The Doom of Mokhaiotl; the conclusion of the Twilight Emissaries")
                .stage("I can start this quest by speaking to Servius, Teokan of Ralos, in the Sunrise Palace. (Requires The Heart of Darkness, Perilous Moons, 66 Thieving, 52 Fletching, 52 Runecraft.)")
                .talk("servius_teokan").say("Queen Zyanyi seeks an audience with you. The Emissaries hold Prince Itzla in the Twilight Temple, "
                        + "and Metzli means to parade him before her cult.", "> Yes.")
                // ---- Infiltration II and Home Invasion ----
                .stage("I must slip into the Twilight Temple in emissary robes and learn the Emissaries' plans, then help Captain Vibia take Janus at his safe house in Civitas illa Fortis.")
                .reach("quest_tower_of_ascension", 8).stay().unless("hod_emissary_robes").unlessFlag("tfd_stairs").give("hod_emissary_robes")
                .say("* You take a set of emissary robes from the chest at the Tower of Ascension.")
                .puzzle("tfd_twilight_temple", TFD_PASSCODE).stay().need("hod_emissary_robes").unlessFlag("tfd_stairs").flag("tfd_stairs")
                .say("* The Emissary Ascended steps aside. In the basement you creep past the Emissary Enforcer into the back rooms.")
                .missing("* Only Emissaries enter the temple: there are robes in the chest at the Tower of Ascension.")
                .reach("tfd_twilight_temple", 6).stay().needFlag("tfd_stairs").unlessFlag("tfd_painting").flag("tfd_painting")
                .say("* Metzli and Furia leave a room, arguing about the prince.",
                        "* Under the large bed in the south room lies a key; in a drawer, a canvas piece. It fits the painting at the eastern "
                                + "end, and behind the painting a hidden passage leads to a blue chest.")
                .puzzle("tfd_twilight_temple", TFD_TUMBLER_CHEST).stay().needFlag("tfd_painting").unlessFlag("tfd_chest")
                .reqSkill(Skill.THIEVING, 66).flag("tfd_chest")
                .say("* The last tumbler turns green and the chest opens: an emissary scroll. As you turn to leave, the Emissary Enforcer "
                        + "fills the passage.")
                .missing("* Picking the blue chest takes 66 Thieving.")
                .reach("tfd_twilight_temple", 6).stay().needFlag("tfd_chest").unlessFlag("tfd_enforcer").spawn("tfd_emissary_enforcer")
                .say("* Emissary Enforcer: Traitor! He snatches the scroll and swings!")
                .kill("tfd_emissary_enforcer").stay().needFlag("tfd_chest").unlessFlag("tfd_enforcer").flag("tfd_enforcer")
                .give("tfd_emissary_scroll")
                .say("* The enforcer falls. You pick up the emissary scroll and read it: the Emissaries are going for the Crypt of Tonali.")
                .talk("queen_zyanyi").stay().needFlag("tfd_enforcer").unlessFlag("tfd_queen").take("tfd_emissary_scroll").flag("tfd_queen")
                .say("The crypt... And Janus, their Forebearer, hides in a safe house south of the west bank. Captain Vibia is watching "
                        + "it. Help her bring him in.")
                .missing("Have you word of my son?")
                .talk("captain_vibia").stay().needFlag("tfd_queen").unlessFlag("tfd_vibia").flag("tfd_vibia")
                .say("Janus is inside. The door has a code lock, but the southernmost window on the east side isn't latched.")
                .reach("tfd_safehouse", 4).stay().needFlag("tfd_vibia").unlessFlag("tfd_dog").take("bones").flag("tfd_dog")
                .say("* You climb in through the window. A guard dog growls, until you give it the bones.",
                        "* You pet the dog. The tag on its collar reads 'GUS'.")
                .missing("* A guard dog snarls behind the window. It might calm down for some bones.") // wiki: or raw or cooked meat
                .puzzle("tfd_safehouse", TFD_DOOR_CODE).stay().needFlag("tfd_dog").unlessFlag("tfd_door").flag("tfd_door")
                .say("* The door's lock clicks open.")
                .reach("tfd_safehouse", 4).stay().needFlag("tfd_door").unlessFlag("tfd_blackjack")
                .give("tfd_empty_sack", "tfd_makeshift_blackjack").flag("tfd_blackjack")
                .say("* You empty the sack of potatoes and take the knife beside it. On the middle floor you empty a coin purse.",
                        "* On the top floor you cut a branch from the diseased off-green plant and fill the purse from the pot of sand.",
                        "* Branch and sandy purse make a makeshift blackjack.")
                .talk("captain_vibia").needFlag("tfd_blackjack").take("tfd_empty_sack", "tfd_makeshift_blackjack")
                .say("A sack over the head and a knock with this: Janus never saw it coming.",
                        "* You search the unconscious Janus for a key. In the basement Queen Zyanyi decides: the cult's helper is in Cam Torum.",
                        "> Yes, let's go.")
                // ---- Dwarven Traitor and Interlopers in Neypotzli ----
                .stage("I should help Attala unmask the traitor in Cam Torum, then stop the Emissaries' interlopers in Neypotzli.")
                .talk("attala").stay().unlessFlag("tfd_attala").flag("tfd_attala")
                .say("Only four may let humans into Cam Torum: the Highlord, Highseer Vandra, Teumo and I. Teumo, then. His house is east "
                        + "of the bank.")
                .talk("galna").stay().needFlag("tfd_attala").unlessFlag("tfd_galna").flag("tfd_galna")
                .say("* You fetch a beer from upstairs and hand it to Galna.", "Teumo? Down in the basement all day, him and his barrels.")
                .puzzle("quest_cam_torum", TFD_BARRELS).stay().needFlag("tfd_galna").unlessFlag("tfd_barrels").flag("tfd_barrels")
                .say("* A click: the fireplace goes out, revealing a lever. You pull it and climb through the hole in the south-east room.",
                        "* You overhear the Tullus twins with Teumo: he will open the doors in the Crypt of Tonali once they bring him an "
                                + "Old One keystone from Neypotzli.")
                .talk("attala").stay().needFlag("tfd_barrels").unlessFlag("tfd_twins").flag("tfd_twins")
                .say("A keystone from Neypotzli? Then that's where they'll be. Go!")
                .talk("eyatlalli").stay().needFlag("tfd_twins").unlessFlag("tfd_eyatlalli").give("tfd_keystone_fragment").flag("tfd_eyatlalli")
                .say("Interlopers tamper with the keystone behind me. An older one remains from when the prison was built. Take this "
                        + "fragment: it will lead you to it.")
                .reach("neypotzli", 8).stay().needFlag("tfd_eyatlalli").unlessFlag("tfd_symbol").take("tfd_keystone_fragment").flag("tfd_symbol")
                .say("* The fragment resonates on a tile in the Streambound Cavern, then in the Earthbound Cavern, and last in the south-east "
                        + "room of the Ancient Prison. A glowing symbol appears.",
                        "* You touch it. The Tullus twins already hold the keystone: Metzli teleports them away and leaves Lucius and "
                                + "Chimalli to deal with you.")
                // wiki: the two fight together, the survivor enraged // approx: one after the other
                .reach("neypotzli", 8).stay().needFlag("tfd_symbol").unlessFlag("tfd_lucius").spawn("tfd_lucius")
                .say("* Lucius raises his staff!")
                .kill("tfd_lucius").stay().needFlag("tfd_symbol").unlessFlag("tfd_lucius").flag("tfd_lucius")
                .say("* Lucius falls. Chimalli nocks her fire arrows.")
                .reach("neypotzli", 8).stay().needFlag("tfd_lucius").unlessFlag("tfd_chimalli").spawn("tfd_chimalli")
                .say("* Chimalli: You'll pay for him!")
                .kill("tfd_chimalli").stay().needFlag("tfd_lucius").unlessFlag("tfd_chimalli").flag("tfd_chimalli")
                .say("* Chimalli falls. Attala and her guards arrive.")
                .talk("attala").needFlag("tfd_chimalli")
                .say("The twins got away with the keystone, and Teumo's gone. This is a human matter: Cam Torum won't march. Survive, friend.")
                // ---- Crypt Assault, the Tonali Cavern and the truth of Mokhaiotl ----
                .stage("I must hold the Crypt of Tonali with Queen Zyanyi, then follow Metzli through the Tonali Cavern to Mokhaiotl.")
                .talk("servius_tal_teklan").stay().unlessFlag("tfd_tal_teklan").flag("tfd_tal_teklan")
                .say("The Queen holds the Crypt of Tonali, south-east of here. The cult comes tonight.")
                .reach("tfd_crypt_of_tonali", 8).stay().needFlag("tfd_tal_teklan").unlessFlag("tfd_waves").spawn("tfd_emissary_brawler")
                .say("* Emissary cultists pour into the crypt! Queen Zyanyi, Captain Vibia and the Tonali hold the line.")
                // wiki: six waves, 23 brawlers, archers and conjurers // approx: a brawler leads each wave
                .kill("tfd_emissary_brawler", 6).stay().needFlag("tfd_tal_teklan").unlessFlag("tfd_waves").flag("tfd_waves")
                .say("* The sixth wave breaks. A conjurer teleports in Metzli, the Tullus twins, Teumo and the prince; Teumo opens the "
                        + "doors and the leaders rush inside.")
                .reach("tfd_crypt_of_tonali", 8).stay().needFlag("tfd_waves").unlessFlag("tfd_ennius").spawn("tfd_ennius_tullus")
                .say("* On a platform below, Ennius Tullus refuses to surrender and draws his sword!")
                .kill("tfd_ennius_tullus").stay().needFlag("tfd_waves").unlessFlag("tfd_ennius").flag("tfd_ennius")
                .say("* Ennius falls for good; Furia flees, wounded. Prince Itzla insists on coming with you after Metzli.")
                .puzzle("tfd_crypt_of_tonali", TFD_SUN_ALTARS, 0).stay().needFlag("tfd_ennius").unlessFlag("tfd_sun1")
                .reqSkill(Skill.RUNECRAFT, 52).flag("tfd_sun1")
                .say("* The altar drinks the Kuhu essence and the statue's engraving shifts.")
                .missing("* Imbuing the altars with Kuhu essence takes 52 Runecraft.")
                .puzzle("tfd_crypt_of_tonali", TFD_SUN_ALTARS, 1).stay().needFlag("tfd_sun1").unlessFlag("tfd_sun2").flag("tfd_sun2")
                .say("* The altar drinks the Kuhu essence and the statue's engraving shifts again.")
                .puzzle("tfd_crypt_of_tonali", TFD_SUN_ALTARS, 2).stay().needFlag("tfd_sun2").unlessFlag("tfd_sun3").flag("tfd_sun3")
                .say("* The third imbuing lights the sun chamber. On to the moon chamber.")
                .puzzle("tfd_crypt_of_tonali", TFD_MOON_KINDLING, 0).stay().needFlag("tfd_sun3").unlessFlag("tfd_moon1")
                .reqSkill(Skill.FLETCHING, 52).flag("tfd_moon1")
                .say("* The moon statue takes the kindling, and the braziers change.")
                .missing("* Carving the ancient roots into root kindling takes 52 Fletching.")
                .puzzle("tfd_crypt_of_tonali", TFD_MOON_KINDLING, 1).stay().needFlag("tfd_moon1").unlessFlag("tfd_moon2").flag("tfd_moon2")
                .say("* The moon statue takes the kindling, and the braziers change again.")
                .puzzle("tfd_crypt_of_tonali", TFD_MOON_KINDLING, 2).stay().needFlag("tfd_moon2").unlessFlag("tfd_moon3").flag("tfd_moon3")
                .say("* The moon chamber is solved. Prince Itzla opens the northern doors: the way to Mokhaiotl lies open.")
                .reach("mokhaiotl_lair", 8).stay().needFlag("tfd_moon3").unlessFlag("tfd_metzli").spawn("tfd_augur_metzli")
                .say("* Metzli waits before the archive's doors: The Old Ones' gift will be mine!")
                .kill("tfd_augur_metzli").stay().needFlag("tfd_moon3").unlessFlag("tfd_metzli").flag("tfd_metzli")
                .say("* Metzli flees deeper. On a skeleton you find a stone tablet: refugees helped the Kuhu build the archive's defences.",
                        "* In the final chamber there is no archive at all. Furia steps from the shadows and throws a rune knife: Metzli is "
                                + "finished. You take Furia back to the crypt.")
                .talk("itzla_crypt").needFlag("tfd_metzli")
                .say("Spare Furia, Mother: with Metzli and Ennius martyred, the cult's last supporters hide in the shadows. Let her be "
                        + "our way to them.", "* Something stirs deep below, in Mokhaiotl. The sun rises on Varlamore.");
    }

    /** The temple stairs (wiki: the passcode is "Final Dawn"). */
    static final QuestPuzzle TFD_PASSCODE = QuestPuzzle.builder("tfd_passcode").room("stairs").variant(null)
            .prompt("* Metzli's sermon still rings in your ears: 'At the final dawn the twilight takes Varlamore.'",
                    "@Emissary Ascended: Halt. The passcode?")
            .options("Eternal twilight", "Final Dawn", "Ximoua rises", "Moonfall").solution(1)
            .shuffle().failLines("@Emissary Ascended: Wrong. Off with you.").build();

    /** One try's feedback per tumbler: 2 green (right number, right tumbler), 1 blue (used on another tumbler), 0 neither. */
    static int[] tfdTumblers(int[] code, int[] guess) {
        int[] res = new int[code.length];
        java.util.List<Integer> left = new java.util.ArrayList<>();
        for (int i = 0; i < code.length; i++) {
            if (guess[i] == code[i]) {
                res[i] = 2;
            } else {
                left.add(code[i]);
            }
        }
        for (int i = 0; i < code.length; i++) {
            if (res[i] == 0 && left.remove(Integer.valueOf(guess[i]))) {
                res[i] = 1;
            }
        }
        return res;
    }

    /** Every combination (four tumblers of 1-8) that would have given these tries the code's feedback. */
    static java.util.List<int[]> tfdConsistent(int[] code, java.util.List<int[]> tries) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int n = 0; n < 4096; n++) {
            int[] c = {n / 512 + 1, n / 64 % 8 + 1, n / 8 % 8 + 1, n % 8 + 1};
            boolean ok = true;
            for (int[] t : tries) {
                ok &= java.util.Arrays.equals(tfdTumblers(c, t), tfdTumblers(code, t));
            }
            if (ok) {
                out.add(c);
            }
        }
        return out;
    }

    /** A blue chest: its combination and the tries already made on it. */
    record TfdBoard(int[] code, java.util.List<int[]> tries) {
    }

    /** The boards (generated so the tries leave one combination; tfdConsistent checks it). // approx: four tumblers of 1-8 */
    static final java.util.List<TfdBoard> TFD_BOARDS = java.util.List.of(
            new TfdBoard(new int[]{3, 7, 1, 6}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{7, 7, 2, 6})),
            new TfdBoard(new int[]{8, 2, 5, 4}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8})),
            new TfdBoard(new int[]{6, 6, 1, 3}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{3, 3, 1, 1})),
            new TfdBoard(new int[]{2, 8, 7, 1}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{5, 3, 2, 2})),
            new TfdBoard(new int[]{5, 3, 8, 2}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{4, 6, 8, 3})),
            new TfdBoard(new int[]{4, 1, 6, 7}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{6, 7, 1, 1},
                    new int[]{4, 4, 4, 3})),
            new TfdBoard(new int[]{7, 5, 2, 8}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{7, 8, 6, 1})),
            new TfdBoard(new int[]{1, 4, 4, 5}, java.util.List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8}, new int[]{5, 5, 1, 1})));

    /**
     * The blue chest behind Metzli's painting (wiki: move the tumblers and confirm; a green light is the right number on the right
     * tumbler, a blue one a number used on another tumbler).
     */
    static final QuestPuzzle TFD_TUMBLER_CHEST = tfdTumblerChest();

    private static QuestPuzzle tfdTumblerChest() {
        String[] light = {"dark", "blue", "green"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("tfd_tumbler_chest").room("chest");
        for (TfdBoard board : TFD_BOARDS) {
            java.util.List<String> prompt = new java.util.ArrayList<>();
            prompt.add("* The blue chest's lock has four tumblers, each set from 1 to 8. On confirming, a light beside each tumbler "
                    + "shows green for the right number, blue for a number used on another tumbler, dark otherwise.");
            for (int[] t : board.tries()) {
                int[] fb = tfdTumblers(board.code(), t);
                prompt.add("* Tried " + t[0] + "-" + t[1] + "-" + t[2] + "-" + t[3] + ": " + light[fb[0]] + ", " + light[fb[1]] + ", "
                        + light[fb[2]] + ", " + light[fb[3]] + ".");
            }
            prompt.add("* Set the four tumblers, first to last.");
            int[] code = board.code();
            b.variant(null).prompt(prompt.toArray(String[]::new)).options("1", "2", "3", "4", "5", "6", "7", "8")
                    .solution(code[0] - 1, code[1] - 1, code[2] - 1, code[3] - 1);
        }
        return b.failLines("* The lights stay dark. The lock resets.").build();
    }

    /** The safe house door (wiki: the dog's tag gives the passcode, GUS). */
    static final QuestPuzzle TFD_DOOR_CODE = QuestPuzzle.builder("tfd_door_code").room("door").variant(null)
            .prompt("* The door between the safe house's rooms has a lock of three letter dials. The dog's tag read 'GUS'.")
            .options(QuestPuzzle.letters()).solution(QuestPuzzle.word("GUS"))
            .failLines("* The lock doesn't budge.").build();

    /**
     * Teumo's basement barrels (wiki: steamforge brew on the steaming barrel, dwarven stout on the black liquid, beer on the one
     * that smells like a pub, the mind bomb on the bubbling one, and the drunk beer's glass on the empty one).
     */
    static final QuestPuzzle TFD_BARRELS = QuestPuzzle.builder("tfd_barrels").room("barrels").variant(null)
            .prompt("* Teumo's basement: five barrels, each with a place for a drink. The cabinet holds steamforge brew, dwarven stout, "
                            + "beer and wizard's mind bomb; you drink one beer and keep the glass.",
                    "* South-west of the cabinet, a barrel trails steam. East of it, one holds a black liquid. The northern barrel smells "
                            + "like a standard Gielinorian pub; the south-eastern one is bubbling; the one by the stairs seems to be empty.",
                    "* Place a drink on each: the south-west barrel, the east one beside it, the north, the south-east, then the stairs.")
            .options("Steamforge brew", "Dwarven stout", "Beer", "Wizard's mind bomb", "Beer glass").solution(0, 1, 2, 3, 4)
            .shuffle().failLines("* Nothing happens. You take the drinks back.").build();

    /** The old numbers on the sun statue (wiki: Oma 2, Naui 4, Kuli 5, Chaki 6, Koma 7, Ueai 8, Makti 10). */
    static final java.util.Map<String, Integer> TFD_NUMBERS = java.util.Map.of("Oma", 2, "Naui", 4, "Kuli", 5, "Chaki", 6, "Koma", 7,
            "Ueai", 8, "Makti", 10);

    /**
     * The sun chamber (wiki: the altars count 1 at the north-east, clockwise to 8 at the north; the statue's first word puts the
     * prince at an altar and the second you, the third unused; three imbuings). // approx: the engravings
     */
    static final QuestPuzzle TFD_SUN_ALTARS = tfdSunAltars();

    private static QuestPuzzle tfdSunAltars() {
        String[][][] rooms = {
                {{"Oma", "Ueai", "Koma"}, {"Naui", "Koma", "Makti"}, {"Kuli", "Oma", "Chaki"}, {"Chaki", "Naui", "Oma"}},
                {{"Ueai", "Oma", "Kuli"}, {"Koma", "Kuli", "Makti"}, {"Oma", "Chaki", "Naui"}, {"Naui", "Ueai", "Koma"}},
                {{"Koma", "Oma", "Ueai"}, {"Chaki", "Ueai", "Makti"}, {"Kuli", "Naui", "Koma"}, {"Ueai", "Chaki", "Oma"}}};
        QuestPuzzle.Builder b = QuestPuzzle.builder("tfd_sun_altars");
        for (int r = 0; r < rooms.length; r++) {
            b.room("sun" + (r + 1));
            for (String[] words : rooms[r]) {
                int prince = TFD_NUMBERS.get(words[0]) - 1;
                int you = TFD_NUMBERS.get(words[1]) - 1;
                b.variant(null).prompt("* Eight altars ring the sun chamber; the first is the north-east one, and they count on clockwise "
                                        + "to the north altar, the eighth. You fill your hands with Kuhu essence from the urns.",
                                "* The sun statue reads: '" + words[0] + " " + words[1] + "-" + words[2] + "'. Prince Itzla knows the old "
                                        + "numbers: oma two, naui four, kuli five, chaki six, koma seven, ueai eight, makti ten.",
                                "* Send the prince to the first word's altar, then take the second word's altar yourself.")
                        .options("North-east altar", "East altar", "South-east altar", "South altar", "South-west altar", "West altar",
                                "North-west altar", "North altar")
                        .solution(prince, you).solution(you, prince); // wiki: either of you may take either altar
            }
        }
        return b.failLines("* The essence sputters and burns your hands.").build();
    }

    /**
     * The moon chamber (wiki: count the braziers lit with the prince on one pressure plate, then on the other, and deposit that
     * many root kindling in the moon statue; three times). // approx: the braziers
     */
    static final QuestPuzzle TFD_MOON_KINDLING = tfdMoonKindling();

    private static QuestPuzzle tfdMoonKindling() {
        String[][][][] rooms = {
                {{{"north", "east", "south-west"}, {"north-east", "south"}},
                        {{"north-west", "south", "east", "north-east"}, {"north", "south-west", "west"}},
                        {{"east"}, {"north", "south", "west", "south-east", "north-west"}}},
                {{{"north", "south"}, {"north-east", "south-east", "south-west", "north-west"}},
                        {{"east", "west", "north"}, {"east", "west", "north", "south"}},
                        {{"south-west"}, {"north", "north-east"}}},
                {{{"north", "north-east", "east", "south-east", "south"}, {"south-west", "west", "north-west", "north"}},
                        {{"north-west", "west"}, {"east", "south-east", "south"}},
                        {{"north", "east", "south", "west"}, {"north-east", "south-east", "south-west", "north-west", "north", "south"}}}};
        String[] options = new String[12];
        for (int i = 0; i < options.length; i++) {
            options[i] = "Deposit " + (i + 1) + " root kindling";
        }
        QuestPuzzle.Builder b = QuestPuzzle.builder("tfd_moon_kindling");
        for (int r = 0; r < rooms.length; r++) {
            b.room("moon" + (r + 1));
            for (String[][] plates : rooms[r]) {
                b.variant(null).prompt("* From the old tools you take a knife blade, pull ancient roots from around the room and carve them "
                                        + "into root kindling.",
                                "* With the prince on the west pressure plate, the lit braziers are: " + String.join(", ", plates[0]) + ".",
                                "* With him on the east plate: " + String.join(", ", plates[1]) + ".",
                                "* The moon statue wants as much kindling as the braziers lit, both times together.")
                        .options(options).solution(plates[0].length + plates[1].length - 1);
            }
        }
        return b.failLines("* The statue rejects the kindling, and moonfire licks at you.").build();
    }
}
