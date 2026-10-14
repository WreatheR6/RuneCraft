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
 * More Kandarin, Feldip and Tirannwn quests: Monk's Friend, Murder Mystery, Tribal Totem, Observatory
 * Quest, Sea Slug, Watchtower, Big Chompy Bird Hunting, Elemental Workshop I, Zogre Flesh Eaters, Rum
 * Deal, Enlightened Journey, The Eyes of Glouphrie, Roving Elves, Mourning's End I and II, Scorpion
 * Catcher, Fishing Contest and Dwarf Cannon.
 */
final class KandarinMoreQuests {
    private KandarinMoreQuests() {
    }

    static void register(QuestBook b) {
        b.place("ardougne_monastery", "Ardougne Monastery", 2606, 3210, PlaceDef.Style.TOWN);
        b.place("sinclair_mansion", "Sinclair Mansion", 2740, 3575, PlaceDef.Style.RUINS);
        b.spot("handelmort_mansion", "Handelmort Mansion", 2637, 3320);
        b.place("observatory", "Observatory", 2440, 3160, PlaceDef.Style.TOWN);
        b.place("fishing_platform", "Fishing Platform", 2782, 3285, PlaceDef.Style.RUINS);
        b.place("watchtower", "Watchtower", 2549, 3112, PlaceDef.Style.RUINS);
        b.place("gu_tanoth", "Gu'Tanoth", 2520, 3040, PlaceDef.Style.CAMP);
        b.place("rantz_camp", "Rantz's camp", 2628, 2972, PlaceDef.Style.CAMP);
        b.spot("elemental_workshop", "Elemental Workshop house", 2711, 3481);
        b.place("jiggig", "Jiggig", 2465, 3045, PlaceDef.Style.RUINS);
        b.place("braindeath_island", "Braindeath Island", 2140, 3150, PlaceDef.Style.CAMP);
        b.spot("entrana_balloon", "Entrana balloon field", 2808, 3354);
        b.place("glouphrie_cave", "Brimstail's cave", 2402, 3420, PlaceDef.Style.CAVE);
        b.place("elf_camp", "Elf camp", 2200, 3255, PlaceDef.Style.CAMP);
        b.place("temple_of_light", "Temple of Light", 2275, 3210, PlaceDef.Style.RUINS);
        b.place("west_ardougne_mourners", "Mourner headquarters (West Ardougne)", 2547, 3325, PlaceDef.Style.NONE);
        b.spot("sorcerers_tower", "Sorcerer's Tower", 2701, 3405);
        b.place("hemenster", "Hemenster", 2638, 3440, PlaceDef.Style.CAMP);
        b.place("coal_trucks", "Dwarf cannon camp", 2565, 3460, PlaceDef.Style.CAMP);
        monksFriend(b);
        murderMystery(b);
        tribalTotem(b);
        observatory(b);
        seaSlug(b);
        watchtower(b);
        bigChompy(b);
        elementalWorkshop(b);
        zogre(b);
        rumDeal(b);
        enlightenedJourney(b);
        eyesOfGlouphrie(b);
        rovingElves(b);
        mourningsEnd(b);
        scorpionCatcher(b);
        fishingContest(b);
        dwarfCannon(b);
    }

    private static void monksFriend(QuestBook b) {
        b.npc("brother_omad", "Brother Omad", "ardougne_monastery", 0, 2, 0xE0B8A4, 0x8A6A3D, 0x8A6A3D, 0x5A3A1D);
        b.npc("brother_cedric", "Brother Cedric", "ardougne_monastery", 4, -4, 0xE8A8A0, 0x8A6A3E, 0x8A6A3E, 0x3A2A1E);
        b.item("childs_blanket", "Child's blanket", "cloth", 0x8AA0E0, "A child's blanket.");
        b.quest("monks_friend", "Monk's Friend", Difficulty.NOVICE, 1)
                .xp(Skill.WOODCUTTING, 2000).reward("law_rune:8")
                .stage("I can start this quest by speaking to Brother Omad in the monastery south of Ardougne.")
                .talk("brother_omad").say("Thieves stole the blanket of a child we look after. He cannot sleep without it! They hide in a cave to the west.")
                .stage("I should recover the child's blanket from the thieves' cave west of the monastery.")
                .drop("man", "childs_blanket", 2)
                .talk("brother_omad").take("childs_blanket").say("The blanket! The child sleeps soundly. Now, we want a party... but Brother Cedric has wandered off with the wine.")
                .missing("Please, find the child's blanket. The thieves took it.")
                .stage("I should find Brother Cedric in the woods; he's drunk and needs water and wood to fix his cart.")
                .talk("brother_cedric").take("minecraft:water_bucket", "logs").say("Hic! Water... *glug* better. And wood to fix my cart. Tell Omad I'm coming!")
                .missing("Hic... need water... and some wood for my cart...")
                .stage("I should tell Brother Omad that Cedric is on his way.")
                .talk("brother_omad").say("Cedric is back! Let the party begin! Take these law runes, with our thanks.");
    }

    private static void murderMystery(QuestBook b) {
        b.npc("sinclair_guard", "Guard", "sinclair_mansion", 0, 3, 0xE8C0B4, 0x5A5A7A, 0x3A3A5A, 0x5A5A7B);
        b.npc("david_sinclair", "David", "sinclair_mansion", 3, -2, 0xE8C0B5, 0x2A6A2A, 0x1A3A1A, 0x2A1A10);
        b.item("killers_print", "Killer's fingerprint", "note", 0xD0C0A0, "A fingerprint lifted from the murder weapon.");
        b.item("silver_dagger_evidence", "Silver dagger", "dagger", 0xC0C0D0, "The murder weapon, a silver dagger.");
        b.quest("murder_mystery", "Murder Mystery", Difficulty.NOVICE, 3)
                .xp(Skill.CRAFTING, 1406).reward("coins:2000")
                .stage("I can start this quest by speaking to the guard at the Sinclair Mansion north of Camelot.")
                .talk("sinclair_guard").say("Lord Sinclair has been murdered! The family all have alibis. Will you help find the killer?")
                .stage("I should search the mansion for clues.")
                .reach("sinclair_mansion", 8).give("silver_dagger_evidence").say("* Beneath the window you find a silver dagger, the murder weapon.")
                .stage("I should lift the fingerprint from the dagger using flour, and compare it with the suspects'.")
                .talk("sinclair_guard").take("pot_of_flour").give("killers_print").say("Dust it with flour... there, a clear print. Now compare it with the family's.")
                .missing("You'll need a pot of flour to dust the dagger for prints.")
                .stage("I should compare prints with the family. David seems nervous.")
                .talk("david_sinclair").take("killers_print").say("That print... it's mine. Fine! I did it! Father was going to cut me out of his will!")
                .stage("I should tell the guard who the murderer is.")
                .talk("sinclair_guard").take("silver_dagger_evidence").say("David?! Arrest him! Excellent work, detective. Here's your reward.");
    }

    private static void tribalTotem(QuestBook b) {
        b.npc("kangai_mau", "Kangai Mau", "BRIMHAVEN", -5, -5, 0x7A4A2B, 0xC0A041, 0x6A4A1B, 0x3A2A1F);
        b.npc("wizard_cromperty", "Wizard Cromperty", "ARDOUGNE", 6, 6, 0xF0D8CA, 0x3A3AAB, 0x2A2A8B, 0xE8E8E9);
        b.item("tribal_totem", "Totem", "statue", 0x8A5A2A, "The sacred totem of the Rantuki tribe.");
        b.quest("tribal_totem", "Tribal Totem", Difficulty.INTERMEDIATE, 1)
                .req(Skill.THIEVING, 21).xp(Skill.THIEVING, 1775).reward("swordfish:5")
                .stage("I can start this quest by speaking to Kangai Mau in Brimhaven. (Requires 21 Thieving.)")
                .talk("kangai_mau").say("Lord Handelmort stole our tribe's sacred totem! It sits in his mansion in Ardougne. Please bring it back.")
                .stage("I need a way into Handelmort Mansion. Wizard Cromperty's teleporter might help.")
                .talk("wizard_cromperty").travel("handelmort_mansion").say("My experimental teleporter! Let's send you... somewhere. *ZAP* *You land inside the mansion.*")
                .stage("I should get past the traps and take the totem from the top floor.")
                .reach("handelmort_mansion", 6).give("tribal_totem").say("* You disarm the trapped stairs and take the totem from its chest.")
                .stage("I should return the totem to Kangai Mau.")
                .talk("kangai_mau").take("tribal_totem").say("The totem returns! The Rantuki tribe thanks you. Take these swordfish.");
    }

    private static void observatory(QuestBook b) {
        b.npc("observatory_professor", "Observatory professor", "observatory", 0, 2, 0xE8D0C0, 0x3A2A6A, 0x2A1A4A, 0xC0C0C2);
        b.item("observatory_lens", "Observatory lens", "gem", 0xC0E0F0, "A lens for the observatory telescope.");
        b.item("lens_mould", "Lens mould", "mould", 0x8A7A6A, "A mould for making a telescope lens.");
        b.quest("observatory_quest", "Observatory Quest", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.CRAFTING, 2250).reward("uncut_sapphire")
                .stage("I can start this quest by speaking to the Observatory professor south of the Tree Gnome Stronghold.")
                .talk("observatory_professor").say("Goblins smashed our telescope! I need planks, bronze and molten glass to repair it.")
                .stage("I need 3 planks, a bronze bar and a lens. The goblins in the dungeon took the lens mould.")
                .drop("goblin", "lens_mould", 3)
                .talk("observatory_professor").take("plank:3", "bronze_bar", "lens_mould").give("observatory_lens")
                .say("*He casts a lens in the mould.* Now the telescope is fixed! Let's look at the stars...")
                .missing("I need 3 planks, a bronze bar and the lens mould.")
                .stage("I should look through the telescope with the professor.")
                .talk("observatory_professor").take("observatory_lens").say("That constellation is Scorpius! Its gem is the sapphire. Take one as thanks.");
    }

    private static void seaSlug(QuestBook b) {
        b.npc("caroline", "Caroline", "witchaven", 3, 3, 0xF0C8B1, 0x8A3A6B, 0x5A2A4B, 0xC08A3B);
        b.npc("kennith", "Kennith", "fishing_platform", 2, 2, 0xF0D0BD, 0x3A6AC1, 0x3A3A6B, 0xC08A3C);
        b.boss(BossDef.of("sea_slug", "Sea slug").level(23).stats(25, 20, 20, 20, 1, 1).speed(5)
                .attack(AttackDef.of("slime", AttackType.CRUSH, 4, 2.5, 10)).drops(Drop.always("bones"))
                .skin(0x8A9A5B, 0x7A8A4A, 0x6A7A3A, 0x5A6A2A));
        b.quest("sea_slug", "Sea Slug", Difficulty.INTERMEDIATE, 1)
                .req(Skill.FIREMAKING, 30).xp(Skill.FISHING, 7175).reward("coins:500")
                .stage("I can start this quest by speaking to Caroline in Witchaven. (Requires 30 Firemaking.)")
                .talk("caroline").say("My husband Kent and son Kennith are on the fishing platform out at sea and haven't come home. Holgart can row you out.")
                .stage("I should go to the fishing platform and find Kennith.")
                .talk("holgart").stay().travel("fishing_platform").say("To the platform? Hop in.")
                .talk("kennith").need("tinderbox").spawn("sea_slug").say("The fishermen are all controlled by slugs! I'm hiding. Light a torch to scare them off!",
                        "* You light a torch. A slug lunges from the water!")
                .missing("Help! It's dark and the slugs are everywhere! Have you got a tinderbox?")
                .stage("I must fight off the sea slug and get Kennith off the platform.")
                .kill("sea_slug").travel("witchaven").say("* The slug is destroyed. You help Kennith down to the rowing boat and return to Witchaven.")
                .stage("I should tell Caroline her son is safe.")
                .talk("caroline").say("Kennith! And Kent was found on an island nearby. Thank you so much! Take this reward.");
    }

    private static void watchtower(QuestBook b) {
        b.npc("watchtower_wizard", "Watchtower Wizard", "watchtower", 0, 3, 0xF0D8CB, 0x6A3A9A, 0x4A2A6A, 0xE0E0E5);
        b.npc("og_ogre", "Og", "gu_tanoth", 3, 3, 0x9AA06A, 0x6A4A2C, 0x4A3A1C, 0x5A6A3B);
        b.item("power_crystal", "Power crystal", "gem", 0x80F0E0, "A crystal that powers the watchtower's shield.");
        b.boss(BossDef.of("ogre_shaman", "Ogre shaman").level(113).stats(110, 80, 80, 80, 90, 1).speed(4)
                .attack(AttackDef.of("staff", AttackType.CRUSH, 14, 2.5, 6))
                .attack(AttackDef.of("ogre_magic", AttackType.MAGIC, 16, 8, 4).particle("minecraft:witch"))
                .drops(Drop.always("big_bones")).skin(0x9AA06B, 0x5A2A6A, 0x3A1A4A, 0xC0A040).size(1.0f, 2.6f, 1.4f)
                .at("gu_tanoth").arena(9, "jungle"));
        b.quest("watchtower", "Watchtower", Difficulty.INTERMEDIATE, 4)
                .req(Skill.MAGIC, 14).req(Skill.THIEVING, 15).req(Skill.AGILITY, 25).req(Skill.HERBLORE, 14).req(Skill.MINING, 40)
                .xp(Skill.MAGIC, 15250).reward("coins:5000")
                .unlock("The Watchtower Teleport spell")
                .stage("I can start this quest by speaking to the Watchtower Wizard north of Yanille. (Requires 15 Magic, 15 Thieving, 25 Agility, 14 Herblore, 40 Mining.)")
                .talk("watchtower_wizard").say("The ogres have stolen the four power crystals of my watchtower's shield! Without them Yanille is defenceless.")
                .stage("I should win the trust of the ogres of Gu'Tanoth and recover the power crystals. Og knows where they are.")
                .talk("og_ogre").take("big_bones:2").say("Human bring bones? Og like! Crystals with shamans. Shamans bad. Kill them, Og not care.")
                .missing("Og hungry. Bring Og two big bones.")
                .stage("I must defeat the ogre shamans holding the crystals.")
                .kill("ogre_shaman").give("power_crystal:4").say("* The ogre shamans are defeated. You recover the four power crystals.")
                .stage("I should return the crystals to the Watchtower Wizard.")
                .talk("watchtower_wizard").take("power_crystal:4").say("The shield is restored! Yanille is safe. Learn this teleport spell, and take this reward.");
    }

    private static void bigChompy(QuestBook b) {
        b.npc("rantz", "Rantz", "rantz_camp", 0, 2, 0x8A9A5B, 0x6A4A2D, 0x4A3A1D, 0x5A6A3C);
        b.item("ogre_bow", "Ogre bow", "longbow", 0x8A6A3A, "A large bow, made for ogres.");
        b.item("ogre_arrow", "Ogre arrow", "arrow", 0x8A6A3B, "A large ogre arrow.");
        b.item("bloated_toad", "Bloated toad", "creature", 0x6A8A3A, "A toad inflated with swamp gas. Chompy birds love them.");
        b.item("raw_chompy", "Raw chompy", "meat_raw", 0xC08A6A, "A raw chompy bird.");
        b.boss(BossDef.of("chompy_bird", "Chompy bird").level(6).stats(10, 1, 1, 1, 1, 1).speed(5)
                .attack(AttackDef.melee(1)).passive().drops(Drop.always("raw_chompy"))
                .skin(0x8A9A5C, 0xC0B080, 0x6A7A4A, 0xE0A040).size(0.8f, 1.2f, 1.0f));
        b.quest("big_chompy_bird_hunting", "Big Chompy Bird Hunting", Difficulty.INTERMEDIATE, 2)
                .req(Skill.FLETCHING, 5).req(Skill.COOKING, 30).req(Skill.RANGED, 30)
                .xp(Skill.FLETCHING, 262).xp(Skill.COOKING, 1470).xp(Skill.RANGED, 735).reward("ogre_bow", "ogre_arrow:30")
                .unlock("Chompy bird hunting in the Feldip Hills")
                .stage("I can start this quest by speaking to Rantz, the ogre in the Feldip Hills. (Requires 5 Fletching, 30 Cooking, 30 Ranged.)")
                .talk("rantz").say("Me want chompy bird! Me have big bow. You make arrows: shafts and wolf bone tips. Chompy come to bloated toads.")
                .stage("I should make ogre arrows (arrow shafts and feathers) and inflate toads from the swamp with bellows.")
                .talk("rantz").take("arrow_shaft:6").give("bloated_toad").say("*Rantz fletches big arrows and blows up a toad.* Put toad down. Chompy come!")
                .missing("You make arrows! Need six arrow shafts.")
                .stage("I should place the bloated toad and shoot the chompy bird that comes for it.")
                .talk("rantz").stay().need("bloated_toad").spawn("chompy_bird").say("Chompy coming! Shoot it!")
                .kill("chompy_bird").take("bloated_toad")
                .stage("I should cook the chompy for Rantz and his children (onion, potato, tomato seasoning).")
                .talk("rantz").take("raw_chompy", "onion", "potato").say("*You roast the chompy with onion and potato.* Yum yum! Big tasty chompy! You good human. Take bow!");
    }

    private static void elementalWorkshop(QuestBook b) {
        b.npc("workshop_book", "Old book", "elemental_workshop", 0, 0, 0xC0A060, 0x8A6A3A, 0x5A4A2A, 0x6A4A20);
        b.item("battered_key", "Battered key", "key", 0x6A6A5A, "A battered old key from inside a book.");
        b.item("elemental_ore", "Elemental ore", "ore", 0xA06A3A, "An ore that shifts colours.");
        b.items(s -> s.add("elemental_shield", "Elemental shield", Kind.SHIELD, "kiteshield", 0xB06A3A).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, 0, 0, 10, 12, 10, 0, 10, 0, 0, 0, 0),
                        Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                .examine("A shield made of elemental metal."));
        b.quest("elemental_workshop_i", "Elemental Workshop I", Difficulty.NOVICE, 1)
                .req(Skill.MINING, 20).req(Skill.SMITHING, 20).req(Skill.CRAFTING, 20)
                .xp(Skill.CRAFTING, 5000).xp(Skill.SMITHING, 5000).reward("elemental_shield")
                .stage("I can start this quest by reading the strange book in a house in Seers' Village. (Requires 20 Mining, Smithing, Crafting.)")
                .talk("workshop_book").give("battered_key").say("* The book tells of an elemental workshop hidden beneath Seers' Village. A battered key falls out!")
                .stage("I should use the key to enter the Elemental Workshop and restore its water wheel, bellows and furnace.")
                .reach("elemental_workshop", 6).take("battered_key").give("elemental_ore:2")
                .say("* You fix the water wheel, the bellows and the furnace. You mine some elemental ore from the rocks.")
                .stage("I should smelt the elemental ore with coal and craft an elemental shield.")
                .talk("workshop_book").take("elemental_ore:2", "coal:8").need("hammer")
                .say("* You smelt the ore into elemental metal and follow the book's instructions to forge a shield.")
                .missing("* The book says: smelt elemental ore with 4 coal each, then hammer it on the anvil.");
    }

    private static void zogre(QuestBook b) {
        b.npc("grish", "Grish", "jiggig", 0, 3, 0x8A9A5D, 0x5A3A2A, 0x3A2A1F, 0x6A7A4A);
        b.item("zogre_black_prism", "Black prism", "gem", 0x2A2A3A, "A dark prism found in the zogre tombs.");
        b.boss(BossDef.of("slash_bash", "Slash Bash").level(111).stats(100, 90, 90, 80, 1, 1).speed(5)
                .defenceBonus(0, 0, 0, 0, -10)
                .attack(AttackDef.of("bash", AttackType.CRUSH, 16, 3, 10).special(Special.POISON, 6))
                .drops(Drop.always("big_bones")).skin(0x7A8A6A, 0x5A6A4A, 0x3A4A2A, 0x8A9A7A).size(1.0f, 2.6f, 1.5f)
                .at("jiggig").arena(9, "crypt"));
        b.quest("zogre_flesh_eaters", "Zogre Flesh Eaters", Difficulty.INTERMEDIATE, 1)
                .reqQuest("big_chompy_bird_hunting", "jungle_potion").req(Skill.SMITHING, 4).req(Skill.HERBLORE, 8).req(Skill.RANGED, 30)
                .xp(Skill.FLETCHING, 2000).xp(Skill.RANGED, 2000).xp(Skill.HERBLORE, 2000)
                .stage("I can start this quest by speaking to Grish at Jiggig in the Feldip Hills. (Requires Big Chompy Bird Hunting, Jungle Potion.)")
                .talk("grish").say("Zogres! Undead ogres rising from Jiggig's tombs! Someone curse our sacred place. Find out who!")
                .stage("I should investigate the tombs beneath Jiggig.")
                .reach("jiggig", 8).give("zogre_black_prism").say("* In the tombs you find a black prism and signs of a human wizard's ritual.")
                .stage("The undead ogre Slash Bash guards the tombs. I must defeat him (zogres are weak to crush and ranged brutal arrows).")
                .kill("slash_bash").say("* Slash Bash crumbles. The curse weakens.")
                .stage("I should tell Grish what I found.")
                .talk("grish").take("zogre_black_prism").say("A human wizard, Sithik Ints, did this! Grish will deal with him. You good. Take reward.");
    }

    private static void rumDeal(QuestBook b) {
        b.npc("captain_braindeath", "Captain Braindeath", "braindeath_island", 0, 3, 0xD8A886, 0x6A1A1C, 0x2A2A2E, 0x1A1A1C);
        b.item("blindweed", "Blindweed", "herb", 0x5A6A3A, "A key ingredient of Braindeath rum.");
        b.item("stagnant_water", "Bucket of stagnant water", "pot", 0x5A7A5A, "Water from the island's foulest pool.");
        b.quest("rum_deal", "Rum Deal", Difficulty.EXPERIENCED, 2)
                .reqQuest("zogre_flesh_eaters", "priest_in_peril")
                .req(Skill.CRAFTING, 42).req(Skill.FISHING, 50).req(Skill.FARMING, 40).req(Skill.PRAYER, 47).req(Skill.SLAYER, 42)
                .xp(Skill.FISHING, 7000).xp(Skill.FARMING, 7000).xp(Skill.PRAYER, 7000)
                .stage("I can start this quest by speaking to Pirate Pete in Port Phasmatys. (Requires Zogre Flesh Eaters, Priest in Peril and more.)")
                .talk("pirate_pete").travel("braindeath_island").say("Captain Braindeath's rum distillery has been overrun by zombie pirates! He needs a hand. *Pete sails you to Braindeath Island.*")
                .stage("I should help Captain Braindeath restart his rum distillery.")
                .talk("captain_braindeath").need("rake", "seed_dibber", "small_fishing_net").missing("Arr! Ye'll need a rake, a seed dibber and a small fishing net for the work here.")
                .say("Arr! I need blindweed from the patch, stagnant water from the pool, and someone to keep those zombies off!")
                .stage("I need blindweed and a bucket of stagnant water for the rum, and to fight off the zombie pirates.")
                .reach("braindeath_island", 10).stay().unless("blindweed").give("blindweed", "stagnant_water")
                .say("* You grow blindweed in the patch and fill a bucket at the stagnant pool.")
                .drop("minecraft:zombie", "blindweed", 4)
                .talk("captain_braindeath").take("blindweed", "stagnant_water")
                .say("* The still bubbles. Braindeath tastes the 'rum' and does a jig.", "Arr, that be the finest rum on the seas! Here's yer reward.")
                .missing("I need blindweed and stagnant water, matey.");
    }

    private static void enlightenedJourney(QuestBook b) {
        b.npc("auguste", "Auguste", "entrana_balloon", 0, 0, 0xF0D0BE, 0x8A6AC0, 0x5A4A8A, 0xE0E0E6);
        b.item("origami_balloon", "Origami balloon", "note", 0xE8E8C0, "A tiny paper balloon.");
        b.quest("enlightened_journey", "Enlightened Journey", Difficulty.INTERMEDIATE, 1)
                .qp(20).req(Skill.FIREMAKING, 20).req(Skill.FARMING, 30).req(Skill.CRAFTING, 36)
                .xp(Skill.CRAFTING, 2000).xp(Skill.FARMING, 3000).xp(Skill.WOODCUTTING, 1500).xp(Skill.FIREMAKING, 4000)
                .unlock("Balloon transport system")
                .stage("I can start this quest by speaking to Auguste on Entrana. (Requires 20 quest points, 20 Firemaking, 30 Farming, 36 Crafting.)")
                .talk("auguste").say("I wish to fly! I need help building a hot air balloon. First, a test: make small origami balloons from papyrus... or silk.")
                .stage("Auguste needs 3 silk for origami test balloons.")
                .talk("auguste").take("silk:3").give("origami_balloon").say("*The tiny balloons float up!* It works! Now the full-sized balloon: 10 sandbags, 8 silk, a bowl, a yellow candle... 8 logs will do for fuel.")
                .stage("I should gather 8 silk, 10 logs and some willow branches for the balloon.")
                .talk("auguste").take("silk:8", "logs:10", "origami_balloon").travel("TAVERLEY")
                .say("*The balloon rises from Entrana!* We're flying! Look, Taverley below! *You land safely.*")
                .missing("We need 8 silk for the envelope and 10 logs to burn.")
                .stage("I should speak to Auguste after the flight.")
                .talk("auguste").say("A triumph! I shall build a network of balloons across Gielinor. Thank you, friend.");
    }

    private static void eyesOfGlouphrie(QuestBook b) {
        b.npc("brimstail", "Brimstail", "glouphrie_cave", 0, 2, 0xD0A072, 0x5A2A8B, 0x3A1A5B, 0xE0E0E7);
        b.item("oaknocks_disc", "Disc", "gem", 0x8A6AC0, "A strange disc from Oaknock's machine.");
        b.boss(BossDef.of("evil_creature", "Evil Creature").level(1).stats(20, 1, 1, 1, 1, 1).speed(6)
                .attack(AttackDef.melee(1)).drops(Drop.always("bones"))
                .skin(0x6A2A6A, 0x4A1A4A, 0x3A0A3A, 0x8A3A8A).size(0.8f, 1.2f, 1.0f).at("glouphrie_cave").arena(7, "void"));
        b.quest("the_eyes_of_glouphrie", "The Eyes of Glouphrie", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.MAGIC, 12000).xp(Skill.RUNECRAFT, 6000).xp(Skill.WOODCUTTING, 2500).xp(Skill.CONSTRUCTION, 250) // OSRS (current quest XP rewards)
                .reqQuest("the_grand_tree").req(Skill.CONSTRUCTION, 5).req(Skill.MAGIC, 46)
                .stage("I can start this quest by speaking to Brimstail in his cave in the Tree Gnome Stronghold. (Requires The Grand Tree, 5 Construction, 46 Magic.)")
                .talk("brimstail").say("Oaknock the engineer's old machine is in my cave. It reveals illusions! But it needs repairing: bring maple and oak planks... 4 planks.")
                .stage("I should repair Oaknock's machine with 4 planks.")
                .talk("brimstail").take("plank:4").give("oaknocks_disc").say("*The machine whirs. Through its lens you see the gnome guards... are creatures in disguise!*")
                .stage("I should destroy the Evil Creatures Glouphrie has disguised as gnomes.")
                .kill("evil_creature").take("oaknocks_disc").say("* The creature dissolves. King Narnode must hear of this.")
                .stage("I should tell King Narnode about the illusions.")
                .talk("king_narnode").say("Glouphrie, the evil gnome mage, is behind this? We'll double the guard. Thank you.");
    }

    private static void rovingElves(QuestBook b) {
        b.npc("islwyn", "Islwyn", "elf_camp", 0, 3, 0xF4E0D4, 0x3A6A3B, 0x2A4A2C, 0xE0D091);
        b.npc("eluned", "Eluned", "elf_camp", 3, -2, 0xF4E0D5, 0x6A3A8C, 0x4A2A5C, 0xE0C092);
        b.item("consecration_seed", "Consecration seed", "seed", 0xB8F0FE, "A crystal seed for consecrating Glarial's new tomb.");
        b.items(s -> s.add("crystal_bow_quest", "New crystal bow", Kind.BOW, "longbow", 0xB8F0FF).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 70, 0, 0),
                        Map.of(Skill.RANGED, 70, Skill.AGILITY, 50), 5, AttackType.RANGED, EquipmentDef.WeaponClass.BOW))
                .examine("A nice sturdy bow made out of crystal. Needs no arrows."));
        b.boss(BossDef.of("moss_guardian", "Moss guardian").level(84).stats(60, 70, 70, 70, 1, 1).speed(6)
                .attack(AttackDef.of("club", AttackType.CRUSH, 13, 2.5, 10))
                .drops(Drop.always("big_bones")).skin(0x5A7A3B, 0x4A6A2A, 0x3A5A1A, 0x6A8A4A).size(1.0f, 2.6f, 1.5f)
                .at("baxtorian_falls").arena(9, "jungle"));
        b.quest("roving_elves", "Roving Elves", Difficulty.EXPERIENCED, 1)
                .reqQuest("regicide", "waterfall_quest").req(Skill.AGILITY, 56)
                .xp(Skill.STRENGTH, 10000).reward("crystal_bow_quest")
                .stage("I can start this quest by speaking to Islwyn in the elf camp in Tirannwn. (Requires Regicide, Waterfall Quest, 56 Agility.)")
                .talk("islwyn").say("Glarial's tomb was disturbed and her remains moved... by you. Eluned can perform a rite to consecrate a new resting place.")
                .stage("I should speak to Eluned about the consecration rite.")
                .talk("eluned").give("consecration_seed").say("Take this crystal seed. It must be charged with Glarial's spirit at the Baxtorian Falls, but a moss guardian protects the tree there.")
                .stage("I must defeat the moss guardian at the Baxtorian Falls and charge the seed.")
                .kill("moss_guardian").say("* The moss guardian falls. You charge the consecration seed in the falls.")
                .stage("I should return the seed to Islwyn.")
                .talk("islwyn").take("consecration_seed").say("Glarial's spirit rests again. You have earned the elves' trust. Take this crystal bow.");
    }

    private static void mourningsEnd(QuestBook b) {
        b.npc("mourner_spy", "Mourner", "west_ardougne_mourners", 0, 0, 0xE0B8A5, 0x2A2A2B, 0x2A2A2C, 0x1A1A1D);
        b.item("mourner_gear", "Mourner gear", "cloth", 0x2A2A2A, "A full set of mourner gear, as a disguise.");
        b.item("coloured_toads", "Dyed toad", "creature", 0x8A3AA0, "A toad dyed in sheep dye... it's a long story.");
        b.item("light_crystal", "Death talisman", "talisman", 0xE0E0FF, "The key to the Temple of Light.");
        b.boss(BossDef.of("shadow_of_light", "Shadow").level(73).stats(80, 70, 70, 70, 1, 1).speed(4)
                .attack(AttackDef.of("shadow_touch", AttackType.SLASH, 11, 2.5, 10).special(Special.DRAIN_PRAYER, 2))
                .drops(Drop.always("bones")).skin(0x1A1A2A, 0x0A0A1A, 0x0A0A0A, 0x3A3A6A).at("temple_of_light").arena(9, "void"));
        b.quest("mournings_end_part_i", "Mourning's End Part I", Difficulty.MASTER, 2)
                .xp(Skill.THIEVING, 40000).xp(Skill.HITPOINTS, 25000) // OSRS (current quest XP rewards)
                .reqQuest("roving_elves", "big_chompy_bird_hunting", "sheep_herder").req(Skill.RANGED, 60).req(Skill.THIEVING, 50)
                .stage("I can start this quest by speaking to Islwyn in Tirannwn. (Requires Roving Elves, Big Chompy Bird Hunting, Sheep Herder, 60 Ranged, 50 Thieving.)")
                .talk("islwyn").say("The mourners of West Ardougne are not what they seem. Arianwyn needs someone to infiltrate them.")
                .stage("I should get a mourner disguise. The mourners' camp is near the elf camp; one may be careless.")
                .talk("mourner_spy").take("coins:500").give("mourner_gear").say("*The mourner looks around, then hands over a spare set of gear for a bribe.* You didn't get this from me.")
                .missing("Move along. Unless you've got some coin to make it worth my while... (500 coins)")
                .stage("Disguised, I should help the mourners' 'research': they want dyed toads, for some reason.")
                .talk("mourner_spy").need("mourner_gear").take("bloated_toad").give("coloured_toads").say("Ah, a toad. We'll dye it. Don't ask. *You overhear: the mourners serve Lord Iorwerth!*")
                .missing("We need a bloated toad for the 'experiment'.")
                .stage("I should tell Arianwyn what I learned.")
                .talk("arianwyn").take("coloured_toads").say("The mourners serve Iorwerth! Thank you. We must find out what they are hiding beneath West Ardougne.");
        b.quest("mournings_end_part_ii", "Mourning's End Part II", Difficulty.MASTER, 2)
                .xp(Skill.AGILITY, 60000) // OSRS (current quest XP rewards)
                .reqQuest("mournings_end_part_i")
                .stage("I can start this quest by speaking to Arianwyn in Lletya. (Requires Mourning's End Part I.)")
                .talk("arianwyn").give("light_crystal").say("The mourners dig for the Temple of Light, the resting place of the Dark Lord's death altar. Take this talisman and go there first.")
                .stage("I should enter the Temple of Light beneath the mourner headquarters and solve its light puzzle.")
                .reach("temple_of_light", 8).say("* You realign the temple's crystals. Light pours into the chamber... and a shadow rises from the darkness!")
                .stage("I must defeat the shadow guarding the temple.")
                .kill("shadow_of_light").take("light_crystal").say("* The shadow is banished. The mourners will never reach the Death Altar now.")
                .stage("I should report to Arianwyn.")
                .talk("arianwyn").say("The temple is sealed. Iorwerth's mourners are finished. The path to Prifddinas begins with this victory.");
    }

    private static void scorpionCatcher(QuestBook b) {
        b.npc("thormac", "Thormac", "sorcerers_tower", 0, 0, 0xF0D8CC, 0x3A6A9B, 0x2A4A6B, 0xE0E0E8);
        b.item("scorpion_cage", "Scorpion cage", "box_trap", 0x8A6A3A, "A cage for kharid scorpions.");
        b.item("kharid_scorpions", "Scorpion cage (full)", "box_trap", 0x8A6A3B, "A cage holding Thormac's three kharid scorpions.");
        b.quest("scorpion_catcher", "Scorpion Catcher", Difficulty.INTERMEDIATE, 1)
                .reqQuest("alfred_grimhands_barcrawl")
                .req(Skill.PRAYER, 31).xp(Skill.STRENGTH, 6625)
                .unlock("Thormac enchants battlestaves into mystic staves for 40,000 coins")
                .stage("I can start this quest by speaking to Thormac in the Sorcerer's Tower south of Seers' Village. (Requires Alfred Grimhand's Barcrawl, 31 Prayer.)")
                .talk("thormac").give("scorpion_cage").say("My pet kharid scorpions escaped! There are three of them. The Seers can find them for you.")
                .stage("I should catch Thormac's three kharid scorpions (Taverley dungeon, the Barbarian Outpost and Monastery).")
                .kill("minecraft:spider").need("scorpion_cage").take("scorpion_cage").give("kharid_scorpions")
                .say("* Among the spiders you find and cage all three kharid scorpions.")
                .stage("I should return the scorpions to Thormac.")
                .talk("thormac").take("kharid_scorpions").say("All three of my precious scorpions! Thank you! I can enchant battlestaves for you now, for a fee.");
    }

    private static void fishingContest(QuestBook b) {
        b.npc("bonzo", "Bonzo", "hemenster", 0, 2, 0xE8C0B6, 0x3A6A8B, 0x2A3A5B, 0x8A5A2B);
        b.npc("vestri", "Vestri", "BURTHORPE", 6, 5, 0xD8A887, 0x6A3A2B, 0x3A2A1B, 0xC07A2C);
        b.item("fishing_pass", "Fishing pass", "note", 0xE0D0A9, "A pass to the Hemenster fishing contest.");
        b.item("fishing_trophy", "Fishing trophy", "statue", 0xE0C043, "First prize in the Hemenster fishing contest.");
        b.items(s -> s.add("giant_carp", "Giant carp", Kind.SIMPLE, "fish_raw", 0xC0A060).value(1).examine("Wow, what a fish!"));
        b.quest("fishing_contest", "Fishing Contest", Difficulty.NOVICE, 1)
                .req(Skill.FISHING, 10).xp(Skill.FISHING, 2437)
                .stage("I can start this quest by speaking to Vestri, a dwarf in Burthorpe, who wants to use the White Wolf Mountain tunnel. (Requires 10 Fishing.)")
                .talk("vestri").give("fishing_pass").say("The dwarves of the tunnel want the fishing trophy from Hemenster. Win it and we'll let you through!")
                .stage("I should enter the Hemenster fishing contest and catch the biggest fish. Garlic in the pipes keeps the rival anglers' fish away...")
                .talk("bonzo").take("fishing_pass", "garlic").give("giant_carp", "fishing_trophy")
                .say("Welcome to the contest! *You stuff garlic in the pipe by your rival's spot and catch a giant carp!*", "That's the winning fish! Here's the trophy.")
                .missing("You'll need your pass to enter. (Tip: rivals' spots near the pipes... garlic would ruin them.)")
                .stage("I should take the trophy to Vestri.")
                .talk("vestri").take("fishing_trophy").say("The trophy! You may use our tunnel under White Wolf Mountain whenever you like.");
    }

    private static void dwarfCannon(QuestBook b) {
        b.npc("captain_lawgof", "Captain Lawgof", "coal_trucks", 0, 3, 0xD8A888, 0x6A6A2B, 0x4A4A1B, 0xC07A2D);
        b.item("dwarf_remains", "Dwarf remains", "bones", 0xA09080, "The remains of a dwarf guard.");
        // Old quest reward kept for saved inventories: Nulodion swaps it for the real ammo mould (CommonItems), which the
        // quest now gives.
        b.item("cannon_ammo_mould", "Ammo mould (old)", "mould", 0x6A6A6A, "Nulodion will swap this for a working ammo mould.");
        b.item("nulodions_notes", "Nulodion's notes", "note", 0xE0D8C1, "Instructions for making cannonballs.");
        b.quest("dwarf_cannon", "Dwarf Cannon", Difficulty.NOVICE, 1)
                .xp(Skill.CRAFTING, 750).reward("ammo_mould", "nulodions_notes")
                .unlock("Ability to buy and use the dwarf multicannon")
                .stage("I can start this quest by speaking to Captain Lawgof at the dwarf camp north of the Coal Trucks.")
                .talk("captain_lawgof").say("Goblins are attacking my outpost! Repair the railings with these... I mean, bring me 6 planks for the railings.")
                .stage("I should repair the railings with 6 planks.")
                .talk("captain_lawgof").take("plank:6").say("Railings fixed! But one of my guards, Gilob, is missing. Check the watchtower.")
                .missing("I need 6 planks to repair the railings.")
                .stage("I should find the missing guard; the goblins may have taken him.")
                .drop("goblin", "dwarf_remains", 3)
                .talk("captain_lawgof").take("dwarf_remains").say("Poor Gilob... The goblins took his son Lollk too. And my cannon is broken!")
                .missing("Find Gilob! The goblins must have him.")
                .stage("I should fix the dwarf cannon with a hammer and steel bars.")
                .talk("captain_lawgof").need("hammer").take("steel_bar:2").say("*You fix the cannon's spring and barrel.* It fires! Nulodion at the Black Guard base will sell you one. Here's the ammo mould.")
                .missing("You'll need a hammer and 2 steel bars to fix the cannon.");
    }
}
