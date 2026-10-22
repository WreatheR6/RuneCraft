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
 * their OSRS coordinates (the Sailing islands get an islet from their plaza). The step chains are
 * condensed and quest-boss stats are approximate.
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

    private static void bloodMoonRises(QuestBook b) {
        Cast.npc(b, "quest_sarius_guile", "Sarius Guile", "quest_icyene_graveyard", 0, 2, Cast.PALE, 0xE0D8A0);
        Cast.npc(b, "quest_efaritay_hallow", "Efaritay Hallow", "quest_icyene_graveyard", 4, -1, Cast.PALE, 0xC0C0E0);
        Cast.npc(b, "quest_roy_bartender", "Roy", "quest_slepe_pub", 0, 0, Cast.PALE, 0x5A4A3A);
        Cast.npc(b, "quest_sugadinti_vitur", "Sugadinti Vitur", "quest_ver_sinhaza", 6, 4, Cast.VAMPYRE, 0x6A1A1A);
        b.boss(BossDef.of("quest_lowerniel_drakan", "Lowerniel Drakan").level(927).stats(900, 400, 400, 350, 300, 1).speed(4) // approx
                .attack(AttackDef.of("claws", AttackType.SLASH, 40, 2.5, 8).special(Special.LIFESTEAL, 25))
                .attack(AttackDef.of("blood_bolt", AttackType.MAGIC, 38, 10, 4).particle("minecraft:crimson_spore"))
                .attack(AttackDef.of("crimson_cleave", AttackType.SLASH, 70, 5, 1).special(Special.AOE, 3)
                        .telegraph(3, "Lowerniel Drakan raises his claws, blood swirling around him!"))
                .phase(50, "Lowerniel Drakan takes on his true form!", 1.35, false)
                .drops(Drop.always("bones")).skin(0xC8C8D0, 0x2A0A1A, 0x1A0A0A, 0x8A0A1A).size(1.0f, 2.8f, 1.3f)
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
                .talk("quest_sarius_guile").say("A blood moon is coming. When it rises, Lowerniel Drakan means to finish what his sister began. The Myreque must be warned.")
                .stage("I should talk to Efaritay Hallow in the Icyene Graveyard.")
                .talk("quest_efaritay_hallow").say("My son Safalaan... he's not been himself. Find Ivan Strom; he'll know where Veliaf is.")
                .stage("I should find Ivan Strom of the Myreque.")
                .talk("ivan_strom").need("blisterwood_flail").missing("You'll need your blisterwood flail where we're going. Fetch it.").say("Veliaf went to Slepe chasing rumours of vyres. Roy at the pub might know more.")
                .stage("I should ask Roy the bartender in Slepe about Veliaf's whereabouts.")
                .talk("quest_roy_bartender").say("The Myreque fella? Headed up to Crombwick Manor, north of here. Place is crawling with vampyres.")
                .stage("I should find Veliaf.")
                .reach("quest_slepe_pub", 10).stay().unlessFlag("bmr_manor").need("blisterwood_flail").flag("bmr_manor")
                .say("* From Slepe you head north to Crombwick Manor and cut through the vampyres with your blisterwood flail.")
                .talk("veliaf_hurtz").needFlag("bmr_manor").missing("*Veliaf isn't here. Roy said he went north to Crombwick Manor.*").say("*You help Veliaf cut through the vampyres in Crombwick Manor.* Drakan has taken Vanescula. Sugadinti Vitur will help us; she waits at Ver Sinhaza.")
                .stage("I should talk to Sugadinti Vitur in front of the Theatre of Blood at Ver Sinhaza.")
                .talk("quest_sugadinti_vitur").needFlag("bmr_manor").say("My sister's theatre still has its uses. Arm yourself well; the way into Castle Drakan opens beneath the blood moon.")
                .stage("I must storm Castle Drakan and defeat Lowerniel Drakan beneath the blood moon.")
                .kill("quest_lowerniel_drakan").need("blisterwood_flail").flag("bmr_drakan").say("* Lowerniel Drakan falls. As the blood moon sets, the vampyres of Morytania waver.")
                .stage("I should return to Veliaf.")
                .talk("veliaf_hurtz").needFlag("bmr_drakan").say("Drakan is dead. After all these years... Morytania is free. The Myreque owe you everything, my friend.");
    }
}
