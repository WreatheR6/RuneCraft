package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/**
 * Quests released from late 2025: The Ides of Milk (free-to-play), the Sailing quests Prying Times,
 * Current Affairs, Troubled Tortugans, The Red Reef and Fallen from Grace, and the Myreque finale The
 * Blood Moon Rises. Quest points, skill/quest requirements and XP rewards follow OSRS; NPCs stand at
 * their OSRS coordinates (the Sailing islands get an islet from their plaza). The Sailing quests' step
 * chains are condensed and their quest-boss stats approximate; The Blood Moon Rises follows the wiki's
 * quick guide and quest-helper, with its creatures' wiki stats.
 */
final class LateQuestsD {
    private LateQuestsD() {
    }

    static void register(QuestBook b) {
        // Lumbridge (The Ides of Milk)
        b.spot("quest_cassius_pond", "Pond north-west of Lumbridge", 3171, 3277);
        b.spot("quest_lumbridge_cow_field", "Lumbridge cow field", 3253, 3270);
        b.spot("quest_groats_farmhouse", "Seth Groats' farmhouse", 3228, 3291);
        b.place("quest_bull_pen", "Bull pen", 3262, 3294, PlaceDef.Style.CAMP);
        // The Pandemonium and Catherby (Prying Times, Current Affairs)
        b.place("quest_the_pandemonium", "The Pandemonium", 3050, 2966, PlaceDef.Style.CAMP);
        b.spot("quest_catherby_council", "Catherby council house", 2825, 3454);
        // The Great Conch and the southern seas (Troubled Tortugans, The Red Reef)
        b.place("quest_tortugan_islet", "Islet north-west of The Great Conch", 2962, 2605, PlaceDef.Style.CAMP);
        b.place("quest_great_conch", "The Great Conch", 3186, 2405, PlaceDef.Style.TOWN);
        b.place("quest_gryphon_lair", "Gryphon's lair", 3177, 2477, PlaceDef.Style.CAVE);
        b.place("quest_little_pearl", "Little Pearl", 3352, 2211, PlaceDef.Style.CAMP);
        b.place("quest_sacred_grove", "The Sacred Grove", 3196, 2466, PlaceDef.Style.CAMP);
        b.place("quest_red_rock", "Red Rock", 2792, 2522, PlaceDef.Style.CAMP);
        b.place("quest_last_light", "Last Light", 2855, 2325, PlaceDef.Style.RUINS);
        b.place("quest_zenith", "The Zenith", 2878, 2505, PlaceDef.Style.CAMP);
        // Wyrmscraig (Fallen from Grace)
        b.place("quest_wyrmscraig", "Auchrie, Wyrmscraig", 2576, 2255, PlaceDef.Style.TOWN);
        b.place("quest_wyrmscraig_landing", "Wyrmscraig landing", 2575, 2296, PlaceDef.Style.CAMP);
        b.spot("quest_sunstone_monolith", "Sunstone monolith", 2598, 2247);
        b.place("quest_wyrmscraig_cave", "Wyrmscraig sea cave", 2562, 2203, PlaceDef.Style.CAVE);
        b.place("quest_wyrmscraig_cathedral", "Wyrmscraig cathedral", 2533, 2216, PlaceDef.Style.RUINS);
        // Morytania (The Blood Moon Rises)
        b.place("quest_icyene_graveyard", "Icyene Graveyard", 3697, 3184, PlaceDef.Style.RUINS);
        b.spot("quest_slepe_pub", "Slepe pub", 3750, 3296);
        b.place("quest_castle_drakan", "Castle Drakan", 3560, 3360, PlaceDef.Style.RUINS); // approx (instanced in OSRS)
        idesOfMilk(b);
        pryingTimes(b);
        currentAffairs(b);
        troubledTortugans(b);
        theRedReef(b);
        fallenFromGrace(b);
        bloodMoonRises(b);
    }

    private static void idesOfMilk(QuestBook b) {
        Cast.npc(b, "quest_cassius", "Cassius", "quest_cassius_pond", 0, 2, Cast.TAN, 0x6A5A3A);
        Cast.npc(b, "quest_gillie_groats", "Gillie Groats", "quest_lumbridge_cow_field", 0, 0, Cast.FAIR, 0xE0E0D0);
        Cast.npc(b, "quest_seth_groats", "Seth Groats", "quest_groats_farmhouse", 0, 0, Cast.FAIR, 0x7A5A3A);
        b.item("quest_groats_principles", "The groats principles", "book", 0xC0A060, "Seth Groats' book on running a dairy.");
        b.boss(BossDef.of("quest_bull", "Bull").level(30).stats(50, 25, 25, 25, 1, 1).speed(4) // approx
                .attack(AttackDef.of("headbutt", AttackType.CRUSH, 3, 2.5, 10))
                .attack(AttackDef.of("ground_slam", AttackType.CRUSH, 8, 4, 2).special(Special.AOE, 2)
                        .telegraph(3, "The Bull snorts and paws at the ground!"))
                .attack(AttackDef.of("charge", AttackType.CRUSH, 8, 6, 2).special(Special.KNOCKBACK, 2)
                        .telegraph(3, "The Bull growls and lowers its horns!"))
                .drops(Drop.always("big_bones")).skin(0x5A3A2A, 0x3A2A1A, 0x2A1A0A, 0xD0C0A0).size(1.2f, 1.4f, 1.0f)
                .at("quest_bull_pen").arena(8, "stone"));
        b.item("quest_milk_sample", "Milk sample", "vial", 0xF0F0E8, "A sample of sour milk from the Lumbridge cows.");
        b.quest("the_ides_of_milk", "The Ides of Milk", Difficulty.NOVICE, 1)
                // Gillie Groats' magic lamp: 1,000 XP in a combat skill or Prayer (QuestLamps)
                .unlock("Access to the Bull, the cow boss in the Lumbridge cow field")
                .unlock("A cow bell amulet from Gillie Groats")
                .stage("I can start this quest by speaking to Cassius by the pond north-west of Lumbridge.")
                .talk("quest_cassius").say("The milk round here has gone sour, and the cows have gone strange. Something's riling them up.",
                        "Gillie Groats milks the cows east of Lumbridge. Ask her what's going on.")
                .stage("I should speak to Gillie Groats at the cow field east of Lumbridge.")
                .talk("quest_gillie_groats").say("The cows won't settle since that Bull turned up in the pen. My brother Seth kept the old dairy records.")
                .stage("I should speak to Seth Groats at his farmhouse east of the River Lum.")
                .talk("quest_seth_groats").give("quest_groats_principles")
                .say("*You search the shelves and find 'The groats principles'.* Take it to Cassius. He'll know what to make of it.")
                .stage("I should return to Cassius with 'The groats principles'.")
                .talk("quest_cassius").take("quest_groats_principles").give("quest_milk_sample")
                .say("It's all here: a bull in the herd turns the milk. Taste this sample... sour, see? Show Gillie; she'll give you a permit for that Bull.")
                .stage("I must open the bull pen in the north-east of the cow field and defeat the Bull (level 30).")
                // OSRS: drink a milk sample in front of Gillie Groats, who then gives you the permit for dealing with the bull.
                .talk("quest_gillie_groats").stay().take("quest_milk_sample").unlessFlag("iom_permit").flag("iom_permit")
                .say("*You drink the sour milk sample in front of her.* Ugh, see? That Bull's to blame. Here's a permit: the pen's yours.")
                .kill("quest_bull").needFlag("iom_permit").flag("iom_bull").say("* The Bull crashes to the ground. The cows already seem calmer.")
                .stage("I should return to Cassius.")
                .talk("quest_cassius").needFlag("iom_bull").say("Sweet milk again! Gillie says you've earned a cow bell amulet. Ask her for it.")
                .done("quest_gillie_groats", "The Bull's back in his pen now, and grumpier than ever. Fancy another round with him?");
    }

    private static void pryingTimes(QuestBook b) {
        Cast.npc(b, "quest_steve_beanie", "'Squawking' Steve Beanie", "quest_the_pandemonium", 0, 2, Cast.TAN, 0x8A2A2A);
        b.item("quest_crate_key", "'Key'", "key", 0x8A8A9A, "Thurgo's idea of a key. It looks a lot like a crowbar.");
        b.quest("prying_times", "Prying Times", Difficulty.INTERMEDIATE, 1)
                .reqQuest("pandemonium", "the_knights_sword").req(Skill.SAILING, 12).req(Skill.SMITHING, 30)
                .xp(Skill.SAILING, 800).xp(Skill.SMITHING, 1000)
                .unlock("Unlimited crowbars from Steve's crate of crowbars")
                .unlock("Charting forgotten drinks")
                .stage("I can start this quest by speaking to 'Squawking' Steve Beanie behind the Pandemonium bar. (Requires Pandemonium, The Knight's Sword, 12 Sailing, 30 Smithing.)")
                .talk("quest_steve_beanie").say("Squawk! My crate o' crowbars is sealed shut, and the crowbars to open it are in the crate!",
                        "That dwarf by Mudskipper Point owes ye a favour, don't he? Get him to make me a key.")
                .stage("I should ask Thurgo, in his hut near Mudskipper Point, to make a 'key' for Steve's crate.")
                .talk("thurgo").reqSkill(Skill.SMITHING, 30).take("steel_bar").need("hammer").give("quest_crate_key")
                .say("A key for a crate? Nothing a bit of steel can't pry open. *Thurgo hammers out something that looks a lot like a crowbar.*")
                .missing("Bring me a steel bar and a hammer and I'll knock something up. You'll want 30 Smithing to help, mind.")
                .stage("I should give the 'key' to 'Squawking' Steve Beanie on the Pandemonium.")
                .talk("quest_steve_beanie").take("quest_crate_key")
                .say("*The crate creaks open.* Crowbars! Hundreds of 'em! Help yerself any time, matey. Squawk!");
    }

    private static void currentAffairs(QuestBook b) {
        Cast.npc(b, "quest_ca_arhein", "Arhein", "catherby_docks", 2, 0, Cast.FAIR, 0x3A5A8A);
        Cast.npc(b, "quest_councillor_catherine", "Councillor Catherine", "quest_catherby_council", 0, 0, Cast.FAIR, 0x5A2A6A);
        b.item("quest_form_cr4p", "Form cr-4p", "note", 0xE0E0D0, "A council form. Every box is mandatory.");
        b.item("quest_mayor_of_catherby", "Mayor of Catherby", "fish_raw", 0xE0A030, "A goldfish in a mayoral fishbowl. He seems to take the job seriously.");
        b.item("quest_current_duck", "Current duck", "creature", 0xE0D040, "A rubber duck that follows the sea currents.");
        b.quest("current_affairs", "Current Affairs", Difficulty.NOVICE, 1)
                .reqQuest("pandemonium").req(Skill.SAILING, 22).req(Skill.FISHING, 10)
                .xp(Skill.SAILING, 1400).xp(Skill.FISHING, 1000)
                .reward("quest_current_duck", "quest_mayor_of_catherby")
                .unlock("Charting sea currents")
                .stage("I can start this quest by speaking to Arhein on the northern part of the Catherby docks. (Requires Pandemonium, 22 Sailing, 10 Fishing.)")
                .talk("quest_ca_arhein").say("I want to chart the currents off Catherby, but the council says I need a permit. Talk to Councillor Catherine, would you?")
                .stage("I should talk to Councillor Catherine in the north-east of Catherby.")
                .talk("quest_councillor_catherine").give("quest_form_cr4p")
                .say("A charting permit? Fill in form cr-4p. And the mayor has to sign it. We... don't currently have a mayor.")
                .stage("Catherby needs a new mayor. Harry's aquarium in the fish shop might have a candidate: I'll need a small fishing net.")
                // OSRS: Harry sells a mayoral fishbowl and tiny net for 50 coins; Arhein gives the fish a mayoral chain;
                // Catherine checks the form's answers and the mayor signs form 7r4-5h.
                .talk("quest_ca_arhein").stay().take("coins:50").unless("quest_mayor_of_catherby").give("quest_mayor_of_catherby")
                .say("*Harry sells you a mayoral fishbowl and a tiny net for 50 coins. You fish a very dignified goldfish out of his aquarium.*",
                        "A fine mayor! Here, he'll need this mayoral chain. Now show him to Catherine.")
                .missing("Harry in the fish shop can sell you a mayoral fishbowl and a tiny net. Bring 50 coins.")
                .talk("quest_councillor_catherine").need("quest_mayor_of_catherby").take("quest_form_cr4p")
                .say("*Catherine quizzes you on form cr-4p; you give the same answers as before.* Form 7r4-5h, then. *The Mayor of Catherby splashes it.* Approved.")
                .missing("A mayor? I don't see one. And bring back form cr-4p.")
                .stage("I should give Arhein the good news and get a current duck from him.")
                .talk("quest_ca_arhein").give("quest_current_duck").say("A permit at last! Take this current duck: drop it in the bay and follow it with your boat.")
                .stage("I should sail east to the ripple just west of the Obelisk of Water, drop the current duck and follow it.")
                .reach("obelisk_of_water_site", 12).stay().need("quest_current_duck").unlessFlag("ca_ripple").flag("ca_ripple")
                .say("* You find a ripple in the water just west of the Obelisk of Water and set the current duck on it. It starts to drift.")
                .reach("witchaven", 16).needFlag("ca_ripple").need("quest_current_duck").flag("ca_charted")
                .say("* The duck bobs along the current all the way to the shore where Holgart keeps his boat. You chart its course and collect the duck.")
                .stage("I should share what I've learned with Arhein on the Catherby docks.")
                .talk("quest_ca_arhein").needFlag("ca_charted").say("The currents run all the way to Entrana! Keep the duck. There are plenty more currents to chart out there.");
    }

    private static void troubledTortugans(QuestBook b) {
        Cast.npc(b, "quest_floopa", "Floopa", "quest_tortugan_islet", 0, 2, Cast.FROG, 0x4A7A4A);
        Cast.npc(b, "quest_elder_korel", "Elder Korel", "quest_great_conch", -6, -8, Cast.FROG, 0x6A5A3A);
        Cast.npc(b, "quest_elder_raley", "Elder Raley", "quest_great_conch", 0, 0, Cast.FROG, 0x8A6A3A);
        Cast.npc(b, "quest_elder_blunn", "Elder Blunn", "quest_great_conch", -6, 6, Cast.FROG, 0x5A6A7A);
        b.item("quest_tortugan_shield", "Tortugan shield", "kiteshield", 0x5A8A5A, "A shield made from an old Tortugan shell.");
        b.boss(BossDef.of("quest_gryphon", "Gryphon").level(95).stats(110, 80, 85, 70, 1, 1).speed(4) // approx
                .attack(AttackDef.of("talons", AttackType.SLASH, 12, 2.5, 10))
                .attack(AttackDef.of("dive", AttackType.CRUSH, 16, 6, 3).telegraph(2, "The Gryphon takes to the air!"))
                .drops(Drop.always("bones")).skin(0xC09A50, 0x8A6A3A, 0x6A4A2A, 0xE0E0E0).size(1.4f, 1.8f, 1.0f)
                .at("quest_gryphon_lair").arena(10, "stone"));
        b.boss(BossDef.of("quest_shellbane_gryphon", "Shellbane gryphon").level(235).stats(320, 180, 180, 160, 120, 1).speed(4) // approx
                .attack(AttackDef.of("talons", AttackType.SLASH, 24, 2.5, 10))
                .attack(AttackDef.of("gale", AttackType.MAGIC, 22, 10, 4).special(Special.KNOCKBACK, 3))
                .attack(AttackDef.of("dive", AttackType.CRUSH, 35, 8, 2).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Shellbane gryphon soars overhead!"))
                .phase(50, "The Shellbane gryphon screeches and tears at the shell rocks!", 1.25, false)
                .drops(Drop.always("bones")).skin(0x8A7A60, 0x5A4A3A, 0x3A2A1A, 0xA0C0C0).size(1.8f, 2.2f, 1.0f)
                .at("quest_little_pearl").arena(12, "sand"));
        b.quest("troubled_tortugans", "Troubled Tortugans", Difficulty.EXPERIENCED, 1)
                .reqQuest("pandemonium").req(Skill.SAILING, 45).req(Skill.SLAYER, 51).req(Skill.CONSTRUCTION, 48)
                .req(Skill.HUNTER, 45).req(Skill.WOODCUTTING, 40).req(Skill.CRAFTING, 34)
                .xp(Skill.SAILING, 10000).xp(Skill.SLAYER, 8000)
                .unlock("Access to The Great Conch")
                .stage("I can start this quest by speaking to the injured Tortugan on the small island north-west of The Great Conch. (Requires Pandemonium, 45 Sailing, 51 Slayer, 48 Construction, 45 Hunter, 40 Woodcutting, 34 Crafting.)")
                .talk("quest_floopa").reqSkill(Skill.CRAFTING, 34)
                .say("*You bind the Tortugan's wounds with palm leaves.* Thank you... I'm Floopa. A gryphon attacked me. Please, take me home to The Great Conch.")
                .stage("I should sail Floopa to the docks at the southern edge of The Great Conch and talk to Elder Korel.")
                .talk("quest_elder_korel").say("Floopa! You brought her home. The gryphons have been raiding the Summer Shore. Elder Raley will want to hear from you.")
                .stage("I should talk to Elder Raley in The Summer Shore and help with the town repairs.")
                .talk("quest_elder_raley").reqSkill(Skill.CONSTRUCTION, 48).take("plank:4").need("hammer")
                .say("*You patch up the battered huts.* Good work. Now: the monument in the middle of town has the gryphon's tracks all over it. Hunt it down.")
                .missing("The huts need patching first: bring 4 planks and a hammer. You'll want 48 Construction.")
                .stage("I should follow the Gryphon's tracks from the monument to its cave north-east of town and defeat it.")
                .kill("quest_gryphon").say("* The Gryphon falls. Among its nest you find a feather far larger than its own.")
                .stage("I should return to Elder Raley with news of the Gryphon's defeat, and get a Tortugan shield from Elder Blunn.")
                .talk("quest_elder_blunn").stay().unless("quest_tortugan_shield").give("quest_tortugan_shield")
                .say("You'll want this against what's coming. A Tortugan shell stops a gryphon's talons.")
                .talk("quest_elder_raley").need("quest_tortugan_shield")
                .say("That feather... the Shellbane gryphon. It nests on Little Pearl, south-east of here. End this, adventurer.")
                .missing("Get a Tortugan shield from Elder Blunn before you go.")
                .stage("I should sail to Little Pearl island, south-east of The Great Conch, and defeat the Shellbane gryphon.")
                .kill("quest_shellbane_gryphon").say("* The Shellbane gryphon crashes onto the rocks. Elder Korel, who followed you out, cheers.")
                .stage("I should return to Elder Raley at The Great Conch.")
                .talk("quest_elder_raley").say("The skies are clear again. The Great Conch is open to you, friend of the Tortugans.");
    }

    private static void theRedReef(QuestBook b) {
        Cast.npc(b, "quest_tortugan_finn", "Finn", "quest_great_conch", 8, 4, Cast.FROG, 0x3A6A8A);
        Cast.npc(b, "quest_elder_katt", "Elder Katt", "quest_sacred_grove", 0, -4, Cast.FROG, 0x2A5A2A);
        Cast.npc(b, "quest_grove_floopa", "Floopa", "quest_sacred_grove", 0, 4, Cast.FROG, 0x4A7A4A);
        Cast.npc(b, "quest_theodore_paxton", "Theodore Paxton", "quest_red_rock", 2, -3, Cast.PALE, 0x2A2A4A);
        Cast.npc(b, "quest_spencer_brentwood", "Spencer Brentwood", "quest_zenith", 0, 2, Cast.TAN, 0xE0E0E0);
        b.item("quest_red_coral", "Red coral", "gem", 0xD03A3A, "Coral from the Red Reef, stolen from the Sacred Grove.");
        b.boss(BossDef.of("quest_black_eye_bethel", "Black Eye Bethel").level(191).stats(220, 150, 150, 120, 1, 150).speed(4) // approx
                .attack(AttackDef.of("cutlass", AttackType.SLASH, 22, 2.5, 8))
                .attack(AttackDef.of("flintlock", AttackType.RANGED, 25, 10, 4))
                .attack(AttackDef.of("powder_keg", AttackType.RANGED, 35, 8, 1).special(Special.AOE, 3)
                        .telegraph(3, "Black Eye Bethel lights a powder keg!"))
                .drops(Drop.always("bones")).skin(0xC8946A, 0x1A1A1A, 0x6A1A1A, 0x1A1A1A)
                .at("quest_last_light").arena(10, "stone"));
        b.boss(BossDef.of("quest_giant_lobster", "Giant lobster").level(112).stats(150, 90, 100, 120, 1, 1).speed(5) // approx
                .attack(AttackDef.of("pincer", AttackType.CRUSH, 18, 2.5, 10))
                .drops(Drop.always("raw_lobster")).skin(0xC03A2A, 0x8A2A1A, 0x6A1A0A, 0x1A1A1A).size(1.6f, 1.2f, 1.0f)
                .at("quest_zenith").arena(9, "water"));
        b.quest("the_red_reef", "The Red Reef", Difficulty.EXPERIENCED, 2)
                .reqQuest("troubled_tortugans").req(Skill.SAILING, 52).req(Skill.SMITHING, 48)
                .xp(Skill.SAILING, 15000).xp(Skill.SMITHING, 5000)
                .unlock("Access to the Great Conch's Sacred Grove")
                .unlock("The bosun's workbench schematic")
                .stage("I can start this quest by speaking to Elder Raley at his home in The Great Conch. (Requires Troubled Tortugans, 52 Sailing, 48 Smithing.)")
                .talk("quest_elder_raley").say("Floopa has gone missing again, and someone has been stripping coral from the reef. Finn saw her last.")
                .stage("I should ask Finn, south-east of Elder Raley's house, where Floopa went.")
                .talk("quest_tortugan_finn").say("She went up to the Sacred Grove. Elder Katt guards it; you'll need his say-so to go in.")
                .stage("I should head to the Sacred Grove, talk to Elder Katt and find Floopa.")
                .talk("quest_elder_katt").say("The grove's red coral has been taken. Floopa went in to see the damage. Go on through.")
                .talk("quest_grove_floopa").say("Someone took the coral in boatloads. I saw the ship: it had the mark of the Red Rock museum!")
                .stage("I should sail to Red Rock, north-west of The Great Conch, and question Theodore Paxton.")
                .talk("quest_theodore_paxton").say("Stolen coral in my museum? Preposterous! ...Unless Black Eye Bethel sold it to us. Her pirates hold Last Light, to the south.")
                .stage("I must sail south to Last Light, deal with Black Eye Bethel and her pirates.")
                .kill("quest_black_eye_bethel").give("quest_red_coral").say("* Bethel falls. In her hold are crates of red coral.")
                .stage("I should talk to Spencer Brentwood aboard the Zenith and repair the coral dredgers below.")
                .talk("quest_spencer_brentwood").reqSkill(Skill.SMITHING, 48).take("steel_bar:2").need("hammer")
                .say("*You dive down and repair the coral dredger.* Careful! Something big is stirring in the reef...")
                .missing("The dredgers need two steel bars and a hammer, and 48 Smithing to fix.")
                .stage("I must defeat the Giant lobster guarding the reef beneath the Zenith.")
                .kill("quest_giant_lobster").say("* The Giant lobster sinks into the coral. The reef is quiet again.")
                .stage("I should return the red coral to the Sacred Grove and tell Floopa.")
                .talk("quest_grove_floopa").take("quest_red_coral")
                .say("The coral's home where it belongs! The elders say you may visit the Sacred Grove whenever you like.");
    }

    private static void fallenFromGrace(QuestBook b) {
        Cast.npc(b, "quest_ffion", "Ffion", "quest_wyrmscraig_landing", 0, 2, Cast.PALE, 0x3A6A3A);
        Cast.npc(b, "quest_cormac", "Cormac", "quest_wyrmscraig", 0, 0, Cast.PALE, 0x6A4A2A);
        Cast.npc(b, "quest_muriel", "Muriel", "quest_wyrmscraig", 6, 1, Cast.PALE, 0x8A6A8A);
        Cast.npc(b, "quest_mortimer", "Mortimer", "quest_wyrmscraig_cave", 0, 3, Cast.FAIR, 0x2A2A2A);
        b.item("quest_sunstone_fragment", "Sunstone fragment", "gem", 0xF0B030, "A warm fragment of sunstone.");
        b.item("quest_sunstone_core", "Sunstone core", "orb", 0xF0C040, "A carved core of sunstone. It hums with light.");
        b.boss(BossDef.of("quest_mad_angel", "Mad Angel").level(270).stats(350, 200, 200, 180, 220, 1).speed(4) // approx
                .attack(AttackDef.of("blade", AttackType.SLASH, 28, 2.5, 8))
                .attack(AttackDef.of("radiance", AttackType.MAGIC, 30, 10, 5).particle("minecraft:end_rod"))
                .attack(AttackDef.of("judgement", AttackType.MAGIC, 45, 10, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Mad Angel raises her blade to the heavens!"))
                .phase(50, "The Mad Angel's halo flares with a blinding light!", 1.3, false)
                .drops(Drop.always("bones")).skin(0xF0E0D0, 0xE0E0F0, 0xC0C0D0, 0xF0D060).size(1.0f, 2.6f, 1.2f)
                .at("quest_wyrmscraig_cathedral").arena(12, "crypt"));
        b.quest("fallen_from_grace", "Fallen from Grace", Difficulty.EXPERIENCED, 2)
                .reqQuest("pandemonium").req(Skill.SAILING, 62).req(Skill.CRAFTING, 60).req(Skill.MINING, 53).req(Skill.RUNECRAFT, 47)
                .xp(Skill.SAILING, 12500).xp(Skill.CRAFTING, 10000).xp(Skill.MINING, 5000).xp(Skill.RUNECRAFT, 5000)
                .unlock("Access to the Mad Angel boss")
                .unlock("Sunstone golem Crafting and Mining on Wyrmscraig")
                .unlock("Wyrmscraig teleports on the Slayer ring and necklace of passage")
                .stage("I can start this quest by sailing to Wyrmscraig and speaking to Ffion at the landing. (Requires Pandemonium, 62 Sailing, 60 Crafting, 53 Mining, 47 Runecraft.)")
                .talk("quest_ffion").say("Strangers aren't welcome since the cathedral fell silent. Cormac in Auchrie decides who stays.")
                .stage("I should talk to Cormac in Auchrie village on Wyrmscraig.")
                .talk("quest_cormac").say("Keenan went into the old cathedral weeks ago and never came out. Muriel knows the old golems; ask her.")
                .stage("I should talk to Muriel in the house north-east of Cormac.")
                .talk("quest_muriel").say("The cathedral golems ran on sunstone. Mine a fragment from the monolith on the south-east coast; you'll need a pickaxe and chisel.")
                .stage("I should mine a sunstone fragment from the monolith on the coast south-east of Muriel's house.")
                .reach("quest_sunstone_monolith", 6).reqSkill(Skill.MINING, 53).needTool("pickaxe").need("chisel").give("quest_sunstone_fragment")
                .say("* You chip a glowing fragment off the sunstone monolith.")
                .missing("* I need 53 Mining, a pickaxe and a chisel to take a fragment.")
                .stage("I should take the sunstone fragment back to Cormac.")
                .talk("quest_cormac").reqSkill(Skill.CRAFTING, 60).need("chisel").take("quest_sunstone_fragment").give("quest_sunstone_core")
                .say("*You carve the fragment into a core.* That should wake a golem. The sea cave in the south-west leads under the cathedral.")
                .missing("Carving a sunstone core takes 60 Crafting and a chisel.")
                .stage("I should sail into the cave in the south-west of the island and talk to Mortimer.")
                .talk("quest_mortimer").take("quest_sunstone_core")
                .say("*Mountain trolls lurk in the dark; you drive them off and power the broken golem.* The way up to the cathedral is open. She's up there. Keenan's... gone.")
                .stage("I must climb into the cathedral and defeat the Mad Angel.")
                .kill("quest_mad_angel").say("* The Mad Angel falls. On the floor of the church lies Keenan's body.")
                .stage("I should return to Cormac to tell him what happened.")
                .talk("quest_cormac").say("So the light that watched over us went mad... Thank you. Wyrmscraig's sunstone is yours to work, if you've the skill.");
    }

    /**
     * The Blood Moon Rises after the wiki quick guide and quest-helper (TheBloodMoonRises, CastleDrakan): the vyrewatch in the
     * Meiyerditch hideout, Slepe, Crombwick Manor and Paterdomus (the six books on their plinths), the tunnel from Ivandis' tomb
     * under the Canifis hideout, the puzzles of Castle Drakan in Vampyrium (busts, clocks, the crescent moon chest, the
     * statues' weapons, the lockbox and the dagger's door, the gilded bookcase, the basins and the refiner), Sangvesti and the
     * woods, Hallowvale's preparations and the siege, the wyrd, and the four fights with Lowerniel Drakan. Castle Drakan and
     * Vampyrium are instances in OSRS: their steps happen at the castle's place in Morytania. The eight saved stages are the
     * quest's chapters.
     */
    private static void bloodMoonRises(QuestBook b) {
        Cast.npc(b, "quest_sarius_guile", "Sarius Guile", "quest_icyene_graveyard", 0, 2, Cast.PALE, 0xE0D8A0);
        Cast.npc(b, "quest_efaritay_hallow", "Efaritay Hallow", "quest_icyene_graveyard", 4, -1, Cast.PALE, 0xC0C0E0);
        Cast.npc(b, "quest_roy_bartender", "Roy", "quest_slepe_pub", 0, 0, Cast.PALE, 0x5A4A3A);
        Cast.npc(b, "quest_sugadinti_vitur", "Sugadinti Vitur", "quest_ver_sinhaza", 6, 4, Cast.VAMPYRE, 0x6A1A1A);
        // quest-helper tiles: the Meiyerditch hideout under Old Man Ral's house (3605, 3215), Ivan by the Slepe church (3727, 3310),
        // Crombwick Manor and Veliaf there (3725, 3357 / 3731, 3359), the Burgh de Rott hideout (3490, 3232), the ruined building
        // north-east of Burgh de Rott where the wyrd is fought (3543, 3257) and the fence north of Barrows (3562, 3323)
        b.spot("bmr_meiyerditch_hideout", "Myreque hideout, Meiyerditch", 3605, 3215);
        b.spot("bmr_crombwick_manor", "Crombwick Manor", 3725, 3357);
        b.spot("bmr_burgh_hideout", "Myreque hideout, Burgh de Rott", 3490, 3232);
        b.spot("bmr_burgh_ruin", "Ruined building north-east of Burgh de Rott", 3543, 3257);
        b.spot("bmr_barrows_fence", "Fence north of Barrows", 3562, 3323);
        b.npcAt("ivan_strom_meiyerditch", "Ivan Strom", 3606, 3213, 0xF0D0BC, 0x6A2A6B, 0x3A1A3B, 0xC08A3B);
        b.npcAt("ivan_strom_slepe", "Ivan Strom", 3727, 3310, 0xF0D0BD, 0x6A2A6C, 0x3A1A3C, 0xC08A3C);
        b.npcAt("veliaf_crombwick", "Veliaf Hurtz", 3731, 3359, 0xE0B89B, 0x5A3A2B, 0x3A2A1B, 0x6A3A1B);
        b.item("bmr_squires_journal", "Squire's journal", "book", 0x6A5A3A, "The journal of a squire who served Ivandis Seergaze.");
        b.item("bmr_ivandis_writings", "Ivandis' writings", "book", 0x8A2A2A, "Ivandis Seergaze's own account of the blessing of the Salve.");
        b.item("bmr_crescent_moon_key", "Crescent moon key", "key", 0xC0C0D0, "A key shaped like a crescent moon.");
        b.item("bmr_new_moon_key", "New moon key", "key", 0x2A2A3A, "A dark key shaped like a new moon.");
        b.item("bmr_gilded_key", "Gilded key", "key", 0xE0C040, "An ornate gilded key.");
        b.item("bmr_full_moon_key", "Full moon key", "key", 0xF0F0E0, "A round key, pale as the full moon.");
        b.item("bmr_solid_key", "Solid key", "key", 0x6A6A6A, "A heavy, solid key.");
        b.item("bmr_serpent_rope", "Serpent rope", "rope", 0x8A1A1A, "Three dead blood serpents knotted into a rope.");
        // Vyrewatch (wiki, level 105: 90 HP, crush up to 10 every 4 ticks) and the Vyrewatch Sentinel (level 151, 150 HP, slash up to 17)
        // // approx: which vyrewatch level the quest's encounters use
        b.boss(BossDef.of("bmr_vyrewatch", "Vyrewatch").level(105).stats(90, 105, 86, 85, 105, 105).speed(4)
                .attack(AttackDef.of("claws", AttackType.CRUSH, 10, 2.5, 1))
                .drops(Drop.always("vampyre_dust")).skin(0x7A6A7A, 0x3A1A2A, 0x2A1A1A, 0x6A2A3A).size(0.8f, 2.2f, 1.0f));
        b.boss(BossDef.of("bmr_vyrewatch_sentinel", "Vyrewatch Sentinel").level(151).stats(150, 120, 160, 180, 120, 120).speed(4)
                .defenceBonus(0, 0, 0, 0, 100)
                .attack(AttackDef.of("claws", AttackType.SLASH, 17, 2.5, 4))
                // the sentinels' blood orbs (wiki: they raise their damage and hurt anyone within two tiles) // approx: an orb of up to 10
                .attack(AttackDef.of("blood_orb", AttackType.MAGIC, 10, 8, 1).special(Special.HAZARD, 3)
                        .telegraph(2, "The sentinel conjures a blood orb: keep two steps away!").particle("minecraft:crimson_spore"))
                .drops(Drop.always("vampyre_dust")).skin(0x8A7A8A, 0x4A1A2A, 0x2A1A1A, 0x8A2A3A).size(0.8f, 2.3f, 1.0f));
        // Castle Drakan's creatures (wiki infoboxes): sanguidae (level 106, 10 HP, stab up to 10 every 3 ticks) and venators (level
        // 246, 345 HP, stab up to 21 every 5 ticks, ranged from afar)
        b.boss(BossDef.of("bmr_sanguidae", "Sanguidae").level(106).stats(10, 200, 90, 30, 30, 1).speed(3)
                .defenceBonus(10, 5, 10, 5, 10)
                .attack(AttackDef.of("proboscis", AttackType.STAB, 10, 2.0, 1))
                .drops(Drop.always("bones")).skin(0x6A1A1A, 0x3A0A0A, 0x2A0A0A, 0xA02020).size(0.5f, 0.6f, 0.6f));
        b.boss(BossDef.of("bmr_venator", "Venator").level(246).stats(345, 200, 200, 120, 100, 75).speed(5)
                .defenceBonus(-5, 20, 90, 100, 100)
                .attack(AttackDef.of("bite", AttackType.STAB, 21, 2.5, 3))
                .attack(AttackDef.of("spines", AttackType.RANGED, 21, 9, 2)) // approx: its ranged max hit
                .drops(Drop.always("bones")).skin(0x4A3A3A, 0x2A1A1A, 0x1A1A1A, 0x8A6A4A).size(1.1f, 1.8f, 1.3f));
        // Ancient feral vyres (level 117, 81 HP, slash up to 10) in Vampyrium's woods
        b.boss(BossDef.of("bmr_ancient_feral_vyre", "Ancient feral vyre").level(117).stats(81, 144, 88, 88, 1, 1).speed(4)
                .defenceBonus(0, 20, 20, 0, 0)
                .attack(AttackDef.of("claws", AttackType.SLASH, 10, 2.5, 1))
                .drops(Drop.always("bones")).skin(0x6A5A5A, 0x3A2A2A, 0x2A1A1A, 0x5A1A1A).size(0.8f, 2.0f, 1.0f));
        // The wyrd (level 564, 1,100 HP, crush up to 30 with a knockback every punch, a screech and a double slam; the bloody screech
        // from 60%; 78+ when it bursts). The fight ends at 10% of its health: this version holds the 990 hitpoints fought.
        b.boss(BossDef.of("bmr_wyrd", "Wyrd").level(564).stats(990, 250, 250, 180, 250, 250).speed(5)
                .defenceBonus(30, 30, 30, 30, 150)
                .attack(AttackDef.of("punch", AttackType.CRUSH, 30, 2.5, 4).special(Special.KNOCKBACK, 2))
                .attack(AttackDef.of("screech", AttackType.MAGIC, 20, 8, 1)) // approx: the screech's damage
                .attack(AttackDef.of("double_slam", AttackType.CRUSH, 30, 3, 1).special(Special.AOE, 2)
                        .telegraph(3, "The wyrd raises both fists: get clear of the slam!"))
                .phase(60, "The wyrd's screech turns bloody!", 1.0, false,
                        AttackDef.of("bloody_screech", AttackType.MAGIC, 30, 8, 1).special(Special.AOE, 3) // approx
                                .telegraph(3, "The wyrd draws a bloody breath: take the open path!").particle("minecraft:crimson_spore"))
                .drops(Drop.always("bones")).skin(0x5A4A4A, 0x3A2A2A, 0x2A1A1A, 0x8A1A1A).size(1.4f, 3.0f, 1.4f));
        // Lowerniel Drakan (wiki infobox: level 1063, 3,000 HP, attack 400, strength/magic/ranged 320, defence 100, +150 ranged
        // defence, stab up to 69 every 4 ticks). He is fought four times: in the woods until 85% (450 damage), at the siege until
        // 58% (1,260), beneath the blood moon until 30% (2,100), then to the end (phases at 2,100 and 900 HP). The first three
        // versions hold the hitpoints each fight takes.
        b.boss(drakan("bmr_drakan_woods", 450));
        b.boss(drakan("bmr_drakan_siege", 1260)
                .attack(AttackDef.of("cleave", AttackType.SLASH, 45, 3, 2).special(Special.AOE, 2)
                        .telegraph(3, "Drakan swings his spear back: get out of the cleave in front of and behind him!")));
        b.boss(drakan("bmr_drakan_blood_moon", 2100)
                .attack(AttackDef.of("cleave", AttackType.SLASH, 50, 3, 2).special(Special.AOE, 2)
                        .telegraph(3, "Drakan swings his spear back: get out of the cleave in front of and behind him!")));
        b.boss(drakan("quest_lowerniel_drakan", 3000)
                .attack(AttackDef.of("cleave", AttackType.SLASH, 50, 3, 2).special(Special.AOE, 2)
                        .telegraph(3, "Drakan swings his spear back: get out of the cleave in front of and behind him!"))
                // below 2,100: blood waves and four blood strikes of 80+ unless Protect from Magic is up
                .phase(70, "Drakan vanishes in a tide of blood!", 1.0, false,
                        AttackDef.of("blood_strikes", AttackType.MAGIC, 80, 12, 1).telegraph(3, "Four blood strikes fly at you: pray Protect from Magic!")
                                .particle("minecraft:crimson_spore"))
                // below 900: spear dashes through the blood pools and lightning
                .phase(30, "Blood pools spread and lightning strikes as Drakan dashes from the dark!", 1.1, false,
                        AttackDef.of("spear_dash", AttackType.STAB, 30, 10, 2).special(Special.SKY_STRIKE, 0)
                                .telegraph(3, "Drakan circles in the dark: step aside from his dash!"))
                .at("quest_castle_drakan").arena(14, "crypt"));
        b.quest("the_blood_moon_rises", "The Blood Moon Rises", Difficulty.GRANDMASTER, 4)
                .reqQuest("a_night_at_the_theatre", "sins_of_the_father")
                .req(Skill.SLAYER, 74).req(Skill.WOODCUTTING, 74).req(Skill.SMITHING, 72).req(Skill.COOKING, 72)
                .req(Skill.FLETCHING, 70).req(Skill.MINING, 66).req(Skill.HUNTER, 65).req(Skill.CRAFTING, 64)
                .req(Skill.HERBLORE, 64).req(Skill.MAGIC, 57)
                // OSRS: a tome of experience, 6 x 30,000 XP in any skill of level 70 or higher (QuestLamps)
                .unlock("The Sunspear")
                .unlock("Access to Vampyrium and Veliaf's pub in Burgh de Rott")
                .unlock("The vyres of Morytania no longer attack you without a vyre noble outfit")
                .stage("I can start this quest by speaking to Sarius Guile in the Icyene Graveyard. (Requires A Night at the Theatre, Sins of the Father, 74 Slayer, 74 Woodcutting, 72 Smithing, 72 Cooking and more.)")
                .talk("quest_sarius_guile").say("A blood moon is coming. When it rises, Lowerniel Drakan means to finish what his sister began.",
                        "He holds Safalaan and Vanescula. The Myreque must be told: their hideout lies under Old Man Ral's house in Meiyerditch.")
                // ---- Starting off: the Meiyerditch hideout ----
                .stage("I should go to the Myreque hideout under Old Man Ral's house in Meiyerditch and find Ivan Strom. I'll need my vyre "
                        + "noble outfit and my blisterwood flail.")
                .reach("bmr_meiyerditch_hideout", 6).stay().unlessFlag("bmr_shrine").need("sotf_vyre_noble_clothing").flag("bmr_shrine")
                .say("* In your vyre noble outfit you inspect the makeshift shrine in the hideout. Ivan arrives: you tell him of Drakan's "
                        + "announcement and Safalaan's imprisonment.")
                .missing("* The hideout's chest holds vyre noble clothing, should you have left yours behind.")
                .talk("ivan_strom_meiyerditch").stay().needFlag("bmr_shrine").unlessFlag("bmr_ivan").need("blisterwood_flail").flag("bmr_ivan")
                .say("> So you're ready to go to Darkmeyer?", "I'm ready. But vyrewatch are coming down the steps. Keep them off me!")
                .missing("Bring your blisterwood flail. We'll need it.")
                // the defence (wiki: three vyrewatch at first, replaced as they fall, then the sentinels) // approx: three and one
                .reach("bmr_meiyerditch_hideout", 8).stay().needFlag("bmr_ivan").unlessFlag("bmr_watch").spawn("bmr_vyrewatch")
                .say("* A vyrewatch swoops down into the hideout!")
                .kill("bmr_vyrewatch", 3).stay().needFlag("bmr_ivan").unlessFlag("bmr_watch").flag("bmr_watch")
                .say("* The vyrewatch fall back. A sentinel glides in behind them, a blood orb swirling in its claws.")
                .reach("bmr_meiyerditch_hideout", 8).stay().needFlag("bmr_watch").unlessFlag("bmr_defended").spawn("bmr_vyrewatch_sentinel")
                .say("* A vyrewatch sentinel lands among the barricades!")
                .kill("bmr_vyrewatch_sentinel").stay().needFlag("bmr_watch").unlessFlag("bmr_defended").flag("bmr_defended")
                .say("* The sentinel crumbles to dust. Ivan is still standing.")
                .talk("ivan_strom_meiyerditch").needFlag("bmr_defended")
                .say("We held them. Veliaf was last seen around Slepe. Meet me by the church there.")
                .missing("?bmr_ivan|They're still coming! Keep them off me!")
                // ---- The Truth Hurtz ----
                .stage("I should meet Ivan by the church in Slepe and find Veliaf.")
                .talk("ivan_strom_slepe").stay().unlessFlag("bmr_slepe").flag("bmr_slepe")
                .say("> Let's get going.", "Roy at the pub hears everything. Ask him.")
                .talk("quest_roy_bartender").stay().needFlag("bmr_slepe").unlessFlag("bmr_roy").flag("bmr_roy")
                .say("> We're looking for a friend of ours.", "The Myreque fella? Went into the Sisterhood Sanctuary under the church, then up "
                        + "into Crombwick Manor. Place is crawling with vyres.")
                .reach("bmr_crombwick_manor", 8).stay().needFlag("bmr_roy").unlessFlag("bmr_manor").spawn("bmr_vyrewatch")
                .say("* Up the ladder from Kroy's laboratory, Veliaf is fighting for his life against the vyrewatch of Crombwick Manor!")
                .kill("bmr_vyrewatch", 5).stay().needFlag("bmr_roy").unlessFlag("bmr_manor").flag("bmr_manor")
                .say("* The fifth vyrewatch falls.")
                .talk("veliaf_crombwick").stay().needFlag("bmr_manor").unlessFlag("bmr_veliaf").flag("bmr_veliaf")
                .say("You came for me? After I left you both... Ivan's off to Paterdomus. Says he's Ivandis' descendant. I always knew.")
                .missing("?!bmr_roy|* Veliaf isn't here. Roy at the pub might know where he went.")
                // Legacy of Seergaze: Drezel, the squire's journal, the monks and the six books
                .talk("drezel").stay().needFlag("bmr_veliaf").unless("bmr_squires_journal").unlessFlag("bmr_books").give("bmr_squires_journal")
                .say("Ivan Strom? He's here. And this belonged to one of Ivandis' squires. He hid Ivandis' writings somewhere in this temple.",
                        "* The squire writes that Ivandis left for Vampyrium to kill Drakan himself.")
                .kill("monk_of_zamorak", 3).near("paterdomus", 24).stay().needFlag("bmr_veliaf").unlessFlag("bmr_monks").flag("bmr_monks")
                .say("* The last monk of Zamorak in the temple falls. Ivan waits upstairs.") // approx: how many monks there are
                .puzzle("paterdomus", BMR_PLINTHS).stay().needFlag("bmr_monks").need("bmr_squires_journal").unlessFlag("bmr_books")
                .take("bmr_squires_journal").flag("bmr_books").give("bmr_ivandis_writings")
                .say("* The last book disappears in a puff of smoke and something clicks in the desk behind Ivan: Ivandis' writings.",
                        "* The blessing of the Salve came from haemalchemy, and Ivandis began to turn. A tunnel by his tomb led straight to Castle Drakan.")
                // Inside job: the tunnel from Ivandis' tomb (wiki: below the Canifis hideout)
                .reach("myreque_hideout", 8).stay().needFlag("bmr_books").unlessFlag("bmr_blockage").needTool("pickaxe").flag("bmr_blockage")
                .say("* Veliaf leads you to Ivandis' tomb below the hideout. You mine through the blockage into a cavern of mine carts full of "
                        + "daeyalt ore.")
                .missing("* A blockage seals the tunnel by Ivandis' tomb. You'll need a pickaxe.")
                .reach("myreque_hideout", 8).stay().needFlag("bmr_blockage").unlessFlag("bmr_tunnel").spawn("bmr_vyrewatch_sentinel")
                .say("* Vyrewatch sentinels rise from the refinery's shadows!")
                .kill("bmr_vyrewatch_sentinel", 2).stay().needFlag("bmr_blockage").unlessFlag("bmr_tunnel").flag("bmr_tunnel")
                .say("* The second sentinel falls. You free the slaves of the daeyalt refinery and climb towards the castle.")
                .reach("quest_castle_drakan", 10).needFlag("bmr_tunnel")
                .say("* You pray at the vampyric shrine by the door and step through the portal at the heart of Castle Drakan...",
                        "* Venators chase you through Vampyrium's Castle Drakan. You split up and hide.")
                .missing("* The way into Castle Drakan runs through the tunnel by Ivandis' tomb.")
                // ---- Escape from Castle Drakan (Vampyrium) ----
                .stage("I'm loose in Castle Drakan in Vampyrium. I must find my way through its locked doors to Veliaf and destroy Drakan's "
                        + "blood stockpile.")
                .puzzle("quest_castle_drakan", BMR_BUSTS).stay().unlessFlag("bmr_busts").flag("bmr_busts")
                .say("* The last lever clicks; the throne opens on a half moon key. On the shelves to the south lies a small clock hand.")
                .puzzle("quest_castle_drakan", BMR_CLOCKS).stay().needFlag("bmr_busts").unlessFlag("bmr_clocks").flag("bmr_clocks")
                .say("* Both clocks chime. Something drops in the fireplace: a Drakan emblem.")
                .puzzle("quest_castle_drakan", BMR_CRESCENT_CHEST).stay().needFlag("bmr_clocks").unless("bmr_crescent_moon_key")
                .unlessFlag("bmr_lab").give("bmr_crescent_moon_key")
                .say("* With the emblems you work through the receptacle doors to Veliaf, and the chest on the northern wall opens: the "
                        + "crescent moon key.")
                // the new moon key: the laboratory's sanguidae, the syringe, the venator and the statues
                .reach("quest_castle_drakan", 10).stay().need("bmr_crescent_moon_key").unlessFlag("bmr_lab").spawn("bmr_sanguidae")
                .say("* Sanguidae buzz up from the laboratory's vats!")
                .kill("bmr_sanguidae", 3).stay().need("bmr_crescent_moon_key").unlessFlag("bmr_lab").flag("bmr_lab") // approx: how many
                .say("* With the laboratory clear you assemble a syringe from its barrel, plunger and needle, fill it from a venator's stomach "
                        + "and melt open the chest: the left crest half.")
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_lab").unlessFlag("bmr_venator").spawn("bmr_venator")
                .say("* A venator stalks the statue room in the basement!")
                .kill("bmr_venator").stay().needFlag("bmr_lab").unlessFlag("bmr_venator").flag("bmr_venator")
                .say("* The venator falls. Weapons hang on the rack at the east end of the room.")
                .puzzle("quest_castle_drakan", BMR_STATUES).stay().needFlag("bmr_venator").unless("bmr_new_moon_key").unlessFlag("bmr_lockbox")
                .give("bmr_new_moon_key")
                .say("* The chest opens on the right crest half. With the full crest in the fireplace you find the new moon key.")
                // the gilded key: Safalaan freed, the gibbous moon key, the lockbox, the dagger's door, the venator head, the display case
                .puzzle("quest_castle_drakan", BMR_LOCKBOX).stay().need("bmr_new_moon_key").unlessFlag("bmr_lockbox").take("bmr_new_moon_key")
                .flag("bmr_lockbox")
                .say("* In the jail below you free Safalaan and Vanescula and pick up a gibbous moon key. Upstairs, the lockbox gives up a fancy gem.")
                .puzzle("quest_castle_drakan", BMR_DAGGER_DOOR).stay().needFlag("bmr_lockbox").unless("bmr_gilded_key").unlessFlag("bmr_bookcase")
                .give("bmr_gilded_key")
                .say("* Behind the door, a mysterious book holds the second fancy gem. With both in the venator head's eyes you take an ornate "
                        + "hourglass, and with the skull and the knife the display case opens: the gilded key.")
                .puzzle("quest_castle_drakan", BMR_GILDED_BOOKCASE).stay().need("bmr_gilded_key").unlessFlag("bmr_bookcase").take("bmr_gilded_key")
                .flag("bmr_bookcase").give("bmr_full_moon_key")
                .say("* The gilded bookcase swings aside. On the table in the secret passage: the full moon key.")
                .puzzle("quest_castle_drakan", BMR_BASINS).stay().need("bmr_full_moon_key").unlessFlag("bmr_basins").take("bmr_full_moon_key")
                .flag("bmr_basins").give("bmr_solid_key")
                .say("* The altar yields an ancient symbol. Fixed to the ancient shield on its mount, it releases the solid key.")
                .puzzle("quest_castle_drakan", BMR_REFINER).needFlag("bmr_basins").take("bmr_solid_key")
                .say("* The panel reads 53. The north door unlocks and you smash Drakan's blood stockpile.",
                        "* Ivan, Veliaf, Safalaan and Vanescula find you in the ruins. Vanescula: The drawbridge at Sangvesti must be lowered.")
                // ---- Blood Hunt and the woods ----
                .stage("I should lower the drawbridge in the ruined village of Sangvesti, then flee through Vampyrium's woods.")
                // approx: Sangvesti's houses, keys and crank told in one step (OSRS walks them one by one, Drakan in pursuit)
                .reach("quest_castle_drakan", 10).stay().unlessFlag("bmr_sangvesti").flag("bmr_sangvesti")
                .say("* With Drakan hunting you through Sangvesti you work the crank, read the dusty book for the Vitur key, open the shed "
                        + "('TOOLS') for bolt cutters, join the key halves, bucket water into the Myrmel barrel for the Shadum key,",
                        "* open the altar house's chest ('35158') for the Jovkai key and bring the smithy's old cog back to Vanescula. The "
                                + "drawbridge comes down.",
                        "@Vanescula Drakan: He waits in the clearing where he killed our father. Be ready.")
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_sangvesti").unlessFlag("bmr_first_blood").spawn("bmr_drakan_woods")
                .say("* In the clearing Lowerniel Drakan raises his spear. Watch the marks of blood: dodge to the glimmering side!")
                .kill("bmr_drakan_woods").stay().needFlag("bmr_sangvesti").unlessFlag("bmr_first_blood").flag("bmr_first_blood")
                .say("* At 85% of his health Drakan lunges: the group scatters into the forest and you are separated.")
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_first_blood").unlessFlag("bmr_ferals").spawn("bmr_ancient_feral_vyre")
                .say("* An ancient feral vyre bursts from the undergrowth!")
                .kill("bmr_ancient_feral_vyre", 4).stay().needFlag("bmr_first_blood").unlessFlag("bmr_ferals").flag("bmr_ferals")
                .say("* The fourth feral vyre falls.")
                // approx: the darkwood trees, maxilla beasts and nylocas horde told in one step
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_ferals").unlessFlag("bmr_woods").needTool("axe").flag("bmr_woods")
                .say("* You fell the darkwood trees from two sides each, walk softly past the blind maxilla beasts, and hold off the nylocas "
                        + "horde with thrown spines.")
                .missing("* Darkwood trees block the way. You'll need an axe (there's a bronze one on a stump).")
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_woods").unless("bmr_serpent_rope").unlessFlag("bmr_river").give("bmr_serpent_rope")
                .say("* Creeping up behind each of the three blood serpents, you wrangle them and knot them into a serpent rope.")
                .reach("quest_castle_drakan", 10).needFlag("bmr_woods").take("bmr_serpent_rope").flag("bmr_river")
                .say("* You swing across the river on the long branched tree. Drakan corners you, but aranei swarm him and a winged woman "
                        + "carries you off: Efaritay Hallow, Safalaan's mother.")
                // ---- Hallowed Preparations and Drakan's Siege ----
                .stage("I should help Efaritay Hallow heal Safalaan, prepare the hallowed flail and defend the palace from Drakan.")
                .talk("quest_efaritay_hallow").stay().unlessFlag("bmr_stew").flag("bmr_stew")
                .say("* You pick amitire leaves, boil a potato in a bowl of water, cook the impaler meat and stew it all at the range.",
                        "* Safalaan eats the amitire stew.", "My son... thank you. Take these hallowed marks for the flail.")
                .talk("quest_sugadinti_vitur").stay().needFlag("bmr_stew").unlessFlag("bmr_flail").need("blisterwood_flail").flag("bmr_flail")
                .say("* At the workbench you set a diamond, an enchant diamond tablet and blisterwood logs into a blessed silver sickle, and "
                        + "forge the hallowed flail at the anvil.", "Good. Now the aranei must hold the bridge.")
                .missing("Bring your blisterwood flail: the hallowed flail is made from it.")
                .reach("quest_ver_sinhaza", 10).stay().needFlag("bmr_flail").unlessFlag("bmr_defence").flag("bmr_defence")
                .say("* From the ballista you shoot down Drakan's portals, then heal the aranei and repair the pillars and windows.")
                .reach("quest_ver_sinhaza", 10).stay().needFlag("bmr_defence").spawn("bmr_drakan_siege")
                .say("* Drakan breaches the palace!")
                .kill("bmr_drakan_siege").needFlag("bmr_defence")
                .say("* At 58% of his health Drakan breaks off. The aranei and Efaritay drive him back.")
                // ---- Another Night at the Theatre, the wyrd and the night of the blood moon ----
                .stage("I should help Sugadinti at Ver Sinhaza and the Myreque at Burgh de Rott before Drakan's blood moon rises.")
                // approx: the theatre's waves (nylocas, the Maiden of Sugadinti and the Nylocas Vasilias) are told, not fought
                .talk("quest_sugadinti_vitur").stay().unlessFlag("bmr_theatre").flag("bmr_theatre")
                .say("* At the Theatre you cut through the waves: nylocas, the Maiden of Sugadinti, more nylocas and the Nylocas Vasilias.",
                        "The Nylocas Queen stands with us now. Veliaf is at Burgh de Rott.")
                .reach("bmr_burgh_ruin", 8).stay().needFlag("bmr_theatre").unlessFlag("bmr_wyrd").spawn("bmr_wyrd")
                .say("* Through the entry in the ruined building, the wyrd turns on you. Keep space behind you: every punch knocks you back.")
                .kill("bmr_wyrd").stay().needFlag("bmr_theatre").unlessFlag("bmr_wyrd").flag("bmr_wyrd")
                .say("* At 10% of its health the wyrd crashes through the fence and escapes.")
                .reach("bmr_barrows_fence", 6).stay().needFlag("bmr_wyrd").unlessFlag("bmr_trail").flag("bmr_trail")
                .say("* You climb over the broken fence, follow the bloody trail and inspect the fence north of Barrows. Drakan is coming.")
                .reach("bmr_burgh_hideout", 8).stay().needFlag("bmr_trail").spawn("bmr_drakan_blood_moon")
                .say("* Beneath the blood moon, Drakan descends on Burgh de Rott!")
                .kill("bmr_drakan_blood_moon").needFlag("bmr_trail")
                .say("* At 30% Drakan falls back as Safalaan tears off his wing. Drakan retreats to Vampyrium, and Efaritay waits for him.")
                // ---- The Blood Moon Falls ----
                .stage("I should talk to Efaritay in the Icyene Graveyard and follow Drakan through the portal for the final battle.")
                .talk("quest_efaritay_hallow").stay().unlessFlag("bmr_portal").flag("bmr_portal").travel("quest_castle_drakan")
                .say("Saving my son can't come at the price of Drakan's return. End it.", "* You step through the portal into Vampyrium.")
                .reach("quest_castle_drakan", 10).stay().needFlag("bmr_portal").spawn("quest_lowerniel_drakan")
                .say("* Lowerniel Drakan waits on the roof of his castle, healed to full strength.")
                .kill("quest_lowerniel_drakan").needFlag("bmr_portal")
                .say("* Lowerniel Drakan falls. As the blood moon sets, the vampyres of Morytania kneel to Sugadinti.");
    }

    /** One of Lowerniel Drakan's four fights (see {@link #bloodMoonRises}), holding the hitpoints that fight takes. */
    private static BossDef drakan(String id, int hitpoints) {
        return BossDef.of(id, "Lowerniel Drakan").level(1063).stats(hitpoints, 400, 320, 100, 320, 320).speed(4)
                .defenceBonus(0, 0, 0, 0, 150)
                // his melee chips through prayer and heals him slightly (wiki)
                .attack(AttackDef.of("spear", AttackType.STAB, 30, 2.5, 6).special(Special.LIFESTEAL, 10))
                // playability: OSRS asks for a tick-perfect dodge roll to the glimmering side; here a marked strike to step out of
                .attack(AttackDef.of("spear_lunge", AttackType.STAB, 40, 6, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Drakan draws a mark of blood: dodge to the glimmering side!").particle("minecraft:crimson_spore"))
                .attack(AttackDef.of("blood_magic", AttackType.MAGIC, 20, 10, 1).particle("minecraft:crimson_spore"))
                .drops(Drop.always("bones")).skin(0xC8C8D0, 0x2A0A1A, 0x1A0A0A, 0x8A0A1A).size(1.0f, 2.8f, 1.3f);
    }

    /**
     * Paterdomus' six plinths (wiki: from north to south Pious proceedings, The Life of Friar, Scruffy notebook, From Misthalin to
     * Morytania, Sarl's journal, Essiandar's notes): each book describes the subject of the stained-glass portrait behind its
     * plinth. // approx: the portraits are named here (OSRS shows them only in glass)
     */
    static final QuestPuzzle BMR_PLINTHS = QuestPuzzle.builder("bmr_plinths").room("plinths").variant(null)
            .prompt("* Six plinths stand before six stained-glass portraits. From north to south they show Erysail the Pious, Friar "
                            + "Twiblick, Derygull Templeton, Iriandul Caistlyn, Sarl Dunegun and Essiandar Gar.",
                    "* Each of the six books you found describes one of them. Place a book on each plinth, north to south.")
            .options("Pious proceedings", "The Life of Friar", "Scruffy notebook", "From Misthalin to Morytania", "Sarl's journal",
                    "Essiandar's notes")
            .solution(0, 1, 2, 3, 4, 5).shuffle()
            .failLines("* The books slide off the plinths. The portraits seem to frown.").build();

    /** The throne room's busts (wiki: from north to south, the third, fourth, first and second). // approx: the hint's wording */
    static final QuestPuzzle BMR_BUSTS = QuestPuzzle.builder("bmr_busts").room("busts").variant(null)
            .prompt("* Searching the throne reveals levers in the four busts to the east. Scratched into the throne's arm: 'Third, fourth, "
                    + "first, second, counting from the north.'")
            .options("Pull the northernmost bust's lever", "Pull the second bust's lever", "Pull the third bust's lever",
                    "Pull the southernmost bust's lever")
            .solution(2, 3, 0, 1)
            .failLines("* The levers spring back into the busts.").build();

    /**
     * The dining room's grandfather clocks (wiki, Poem scroll): Apsul is the east clock and Virer the west; the father is the
     * big hand and the son the small one. The father went north (12) and the son vanquished four prey (4) in Apsul; in Virer the
     * father killed eleven (11) and the son departed to the west (9).
     */
    static final QuestPuzzle BMR_CLOCKS = QuestPuzzle.builder("bmr_clocks").room("clocks").variant(null)
            .prompt("* With both hands found, you can set the two grandfather clocks. The poem scroll reads:",
                    "* 'In Apsul the father then went north, and his son, for he vanquished four prey. They met again in Virer: the father "
                            + "killed eleven, and the son then departed to the West.'",
                    "* The map in the hall puts Apsul on the right (the east clock) and Virer on the left (the west clock). Set the east "
                            + "clock's big hand, then its small hand, then the west clock's big hand and small hand.")
            .options("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
            .solution(11, 3, 10, 8)
            .failLines("* The clocks whir and the hands spin back to midnight.").build();

    /**
     * The chest by Veliaf (wiki: right, down, right, left, left: the vampyres in the southern plaque's order, each pointing as in
     * their painting). // approx: the plaque's and paintings' wording
     */
    static final QuestPuzzle BMR_CRESCENT_CHEST = QuestPuzzle.builder("bmr_crescent_chest").room("chest").variant(null)
            .prompt("* The chest's lock has four arrows. The southern plaque lists five vampyres in order; around the room their "
                            + "paintings show them pointing:",
                    "* the first to the right, the second down, the third to the right, the fourth and fifth to the left.")
            .options("Up", "Down", "Left", "Right")
            .solution(3, 1, 3, 2, 2)
            .failLines("* The lock clunks and the arrows reset.").build();

    /** The basement statues (wiki: mirrored north and south; east to west battleaxe, spear, mace, sword). */
    static final QuestPuzzle BMR_STATUES = QuestPuzzle.builder("bmr_statues").room("statues").variant(null)
            .prompt("* Four statues line each side of the room. The northern ones hold, from east to west, a battleaxe, a spear, a mace "
                    + "and a sword; the southern ones stand empty-handed. Arm the southern statues from the weapons rack, east to west.")
            .options("Battleaxe", "Spear", "Mace", "Sword")
            .solution(0, 1, 2, 3).shuffle()
            .failLines("* The statues' arms grind back. The chest stays shut.").build();

    /**
     * The lockbox (wiki: the venator head reads 426: the four banners' symbol, the two shields' and the six braziers'). //
     * approx: the symbols' names and the candles (a decoy)
     */
    static final QuestPuzzle BMR_LOCKBOX = QuestPuzzle.builder("bmr_lockbox").room("lockbox").variant(null)
            .prompt("* The lockbox has three symbol dials. The mounted venator head is engraved 4-2-6.",
                    "* In the room hang four banners bearing six dots, two shields bearing a curled hook, six braziers marked with four dots "
                            + "and three candles under a crescent.")
            .options("Six dots", "Curled hook", "Four dots", "Crescent")
            .solution(0, 1, 2).shuffle()
            .failLines("* The dials spin back.").build();

    /**
     * The south-west door's passcode (wiki: SPEAR, from the ancestral dagger's numbers and the grid note). // approx: the
     * dagger's numbers, read here through a five-by-five grid with I and J sharing a square
     */
    static final QuestPuzzle BMR_DAGGER_DOOR = QuestPuzzle.builder("bmr_dagger_door").room("door").variant(null)
            .prompt("* The ancestral dagger is engraved 43 35 15 11 42. The grid note: row 1 ABCDE, row 2 FGHIK, row 3 LMNOP, row 4 QRSTU, "
                    + "row 5 VWXYZ (row first, then column). Enter the door's five-letter passcode.")
            .options(QuestPuzzle.letters())
            .solution(QuestPuzzle.word("SPEAR"))
            .failLines("* The door stays locked.").build();

    /** The gilded bookcase (wiki: the symbols of the Vampyric Houses in alphabetical order). */
    static final QuestPuzzle BMR_GILDED_BOOKCASE = QuestPuzzle.builder("bmr_gilded_bookcase").room("bookcase").variant(null)
            .prompt("* The gilded bookcase holds eight books, each marked with a house symbol from the vampyre book. Put them in "
                    + "alphabetical order of their houses, left to right.")
            .options("House Alzeph", "House Drakan", "House Ghrazi", "House Jovkai", "House Myrmel", "House Pyrah", "House Shadum",
                    "House Vitur")
            .solution(0, 1, 2, 3, 4, 5, 6, 7).shuffle()
            .failLines("* The books slide back into their old places.").build();

    /** The basins (wiki: each potion to the basin whose inscription it answers). */
    static final QuestPuzzle BMR_BASINS = QuestPuzzle.builder("bmr_basins").room("basins").variant(null)
            .prompt("* Four basins, four inscriptions. North-west: 'To be a river that runs through the body's land.' North-east: 'To be a "
                            + "faithful follower that flees the light.'",
                    "* South-west: 'To rise without wings and choke all who are there.' South-east: 'To crack with no voice and slip with no "
                            + "legs.' Pour a potion into each basin: north-west, north-east, south-west, south-east.")
            .options("Cloudy grey potion", "Weightless black potion", "Thick red potion", "Cold bluish-white potion")
            .solution(2, 1, 0, 3).shuffle()
            .failLines("* The basins drain. You'll have to start again.").build();

    /**
     * The refiner (wiki: two vials of water, three of blood and three pure essence; blood and essence refine to 17 haemalchemic
     * potential, blood and water to 13, essence and water to 6; the panel must read 53: 17 + 17 + 13 + 6).
     */
    static final QuestPuzzle BMR_REFINER = QuestPuzzle.builder("bmr_refiner").room("refiner").variant(null)
            .prompt("* The basin's panel wants 53 haemalchemic potential. The shelves hold two vials of water, three vials of blood and three "
                            + "pure essence.",
                    "* The refiner turns two of them into a chemical vial: blood and essence 17, blood and water 13, essence and water 6. "
                            + "Refine vials and pour them into the basin until it reads 53.")
            .options("Refine blood and essence (17)", "Refine blood and water (13)", "Refine essence and water (6)")
            .anyOrder(0, 0, 1, 2)
            .failLines("* The panel overflows and drains. You fetch fresh ingredients.").build();
}
