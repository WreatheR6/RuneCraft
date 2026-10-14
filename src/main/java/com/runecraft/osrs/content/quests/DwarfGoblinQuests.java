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
 * The Dorgeshuun and dwarven quest lines: Death to the Dorgeshuun, Another Slice of H.A.M., Land of
 * the Goblins, Between a Rock... and Forgettable Tale... Dorgesh-Kaan and Yu'biusk are underground /
 * on another plane, so they are represented by their surface entrances.
 */
final class DwarfGoblinQuests {
    private DwarfGoblinQuests() {
    }

    static void register(QuestBook b) {
        b.place("quest_ham_hideout", "H.A.M. Hideout", 3166, 3251, PlaceDef.Style.CAVE);
        b.place("quest_dorgesh_kaan", "Dorgesh-Kaan", 3245, 3205, PlaceDef.Style.CAVE);
        b.place("quest_keldagrim_train_tunnel", "Dorgesh-Kaan train tunnel", 2745, 3705, PlaceDef.Style.CAVE);
        b.place("quest_ancient_goblin_temple", "Goblin Temple", 2960, 3495, PlaceDef.Style.RUINS);
        b.spot("quest_yubiusk", "Yu'biusk", 2970, 3520);
        b.place("quest_arzinian_mine", "Arzinian Mine", 2772, 3720, PlaceDef.Style.CAVE);
        b.place("quest_keldagrim_brewery", "Keldagrim brewery", 2715, 3700, PlaceDef.Style.TOWN);
        deathToTheDorgeshuun(b);
        anotherSliceOfHam(b);
        landOfTheGoblins(b);
        betweenARock(b);
        forgettableTale(b);
    }

    private static void deathToTheDorgeshuun(QuestBook b) {
        Cast.npc(b, "ur_tag", "Ur-tag", "quest_dorgesh_kaan", 0, 3, Cast.GOBLIN, 0x6A5A8A);
        Cast.npc(b, "zanik", "Zanik", "quest_dorgesh_kaan", 3, -2, Cast.GOBLIN, 0x8A6A3A);
        Cast.npc(b, "ham_deacon", "H.A.M. Deacon", "quest_ham_hideout", 0, 3, Cast.FAIR, 0xD8D0C0);
        b.items(s -> s.add("ham_hood", "H.A.M. hood", Kind.ARMOUR, "cowl", 0xD8D0C0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1), 1))
                .examine("The hood of a member of the Humans Against Monsters group."));
        b.items(s -> s.add("ham_shirt", "H.A.M. shirt", Kind.ARMOUR, "leather_body", 0xD8D0C0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 1), 1))
                .examine("The shirt of a member of the Humans Against Monsters group."));
        b.items(s -> s.add("ham_robe", "H.A.M. robe", Kind.ARMOUR, "chaps", 0xD8D0C0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1), 1))
                .examine("The robe of a member of the Humans Against Monsters group."));
        b.quest("death_to_the_dorgeshuun", "Death to the Dorgeshuun", Difficulty.INTERMEDIATE, 1)
                .reqQuest("the_lost_tribe").req(Skill.AGILITY, 23).req(Skill.THIEVING, 23)
                .xp(Skill.THIEVING, 2000).xp(Skill.RANGED, 2000).reward("ham_hood", "ham_shirt", "ham_robe")
                .unlock("Zanik can be found in Dorgesh-Kaan; the H.A.M. disguise")
                .stage("I can start this quest by speaking to Mistag in the Lumbridge cellar caves. (Requires The Lost Tribe, 23 Agility, 23 Thieving.)")
                .talk("mistag").say("The council of Dorgesh-Kaan wishes to speak to you, surface-dweller. Go and see Ur-tag.")
                .stage("I should speak to Ur-tag in Dorgesh-Kaan.")
                .talk("ur_tag").say("The humans of H.A.M. are a threat. Zanik, a young goblin, insists on going with you to find out what they plan.",
                        "@Zanik: I'm coming with you! I want to see the surface.")
                .stage("I need a H.A.M. disguise (a hood, shirt and robe from H.A.M. members) to sneak into their hideout west of Lumbridge with Zanik.")
                .drop("man", "ham_hood", 3).drop("man", "ham_shirt", 3).drop("man", "ham_robe", 3)
                .reach("quest_ham_hideout", 6).need("ham_hood", "ham_shirt", "ham_robe")
                .say("* Disguised as a H.A.M. member, you slip into the hideout. Zanik hides in your shadow.")
                .missing("* The H.A.M. guards won't let anyone in without the hood, shirt and robe of the order.")
                .stage("I should listen to the H.A.M. Deacon's meeting.")
                .talk("ham_deacon").say("Brothers and sisters! Sigmund has a plan to deal with the cave goblins once and for all. Meet him in the tunnels under the Dwarven Mine!",
                        "* Zanik tugs your sleeve. We must follow Sigmund!")
                .stage("I must follow Sigmund into the tunnels, where H.A.M. guards are digging towards Dorgesh-Kaan, and fight them off.")
                .kill("guard").say("* The H.A.M. guards are beaten. Sigmund escapes through a crack in the rock, swearing revenge.")
                .stage("I should report to Ur-tag.")
                .talk("ur_tag").say("You saved the city, and Zanik besides. The Dorgeshuun are in your debt. Keep the H.A.M. clothes; they may yet be useful.");
    }

    private static void anotherSliceOfHam(QuestBook b) {
        Cast.npc(b, "grubfoot", "Grubfoot", "quest_ancient_goblin_temple", 3, 3, Cast.GREEN_GOBLIN, 0x7A5A3A);
        b.boss(BossDef.of("quest_sigmund", "Sigmund").level(68).stats(80, 60, 60, 55, 40, 40).speed(4)
                .attack(AttackDef.of("sword", AttackType.SLASH, 11, 2.5, 6))
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 10, 8, 3))
                .attack(AttackDef.of("rune_spell", AttackType.MAGIC, 10, 8, 2).particle("minecraft:enchant"))
                .drops(Drop.always("bones")).skin(0xE8C0BB, 0xD8D0C0, 0x8A8A80, 0xA0A0A2)
                .at("quest_keldagrim_train_tunnel").arena(9, "stone"));
        b.items(s -> s.add("ancient_mace", "Ancient mace", Kind.MELEE_WEAPON, "club", 0x9A8A5A).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(-2, 0, 16, 0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 3),
                        Map.of(Skill.ATTACK, 15, Skill.PRAYER, 15), 4, AttackType.CRUSH, EquipmentDef.WeaponClass.MELEE))
                .examine("An ancient goblin mace. Its special attack drains an opponent's prayer to restore your own."));
        b.item("goblin_symbol_book", "Goblin symbol book", "book", 0x6A8A5A, "A book of the symbols of the goblin gods.");
        b.quest("another_slice_of_ham", "Another Slice of H.A.M.", Difficulty.INTERMEDIATE, 1)
                .reqQuest("death_to_the_dorgeshuun", "the_giant_dwarf", "the_dig_site").req(Skill.ATTACK, 15).req(Skill.PRAYER, 25)
                .xp(Skill.MINING, 3000).xp(Skill.PRAYER, 3000).reward("ancient_mace")
                .unlock("The Dorgesh-Kaan to Keldagrim train; goblin village teleport spheres")
                .stage("I can start this quest by speaking to Ur-tag in Dorgesh-Kaan. (Requires Death to the Dorgeshuun, The Giant Dwarf, The Dig Site, 15 Attack, 25 Prayer.)")
                .talk("ur_tag").say("A new train tunnel to Keldagrim has uncovered an ancient goblin temple! Zanik wants to learn about her heritage. Will you go with her?")
                .stage("I should help Zanik study the ancient goblin temple near Goblin Village, and ask Grubfoot about the symbols.")
                .talk("grubfoot").give("goblin_symbol_book").say("Dem symbols? Dey is de old gods' marks. Big High War God! Take dis book, it got all de pictures.")
                .stage("With the symbol book, Zanik can read the shrine. Something is wrong in the train tunnel: H.A.M. is there!")
                .reach("quest_keldagrim_train_tunnel", 8).need("goblin_symbol_book")
                .say("* Sigmund and his H.A.M. agents have planted explosives to collapse the tunnel... and Sigmund turns to face you!")
                .stage("I must defeat Sigmund before the tunnel is destroyed.")
                .kill("quest_sigmund").say("* Sigmund is defeated and taken prisoner by the dwarven Black Guard.")
                .stage("I should report to Ur-tag.")
                .talk("ur_tag").take("goblin_symbol_book").say("The tunnel is safe and H.A.M. is broken. Zanik found an ancient mace in the temple; she wants you to have it.");
    }

    private static void landOfTheGoblins(QuestBook b) {
        Cast.npc(b, "high_priest_bighead", "High Priest Bighead", "quest_yubiusk", 0, 3, Cast.GREEN_GOBLIN, 0x8A2A2A);
        Cast.npc(b, "snothead", "Snothead", "goblin_village", 3, 3, Cast.GREEN_GOBLIN, 0x5A6A2A);
        b.item("goblin_potion", "Goblin potion", "potion", 0x7A9A3A, "A potion that lets a non-goblin pass as one... for a while.");
        b.item("huzamogaarb_whistle", "Huzamogaarb's whistle", "secondary", 0xA0A0A0, "An old goblin priest's whistle.");
        b.quest("land_of_the_goblins", "Land of the Goblins", Difficulty.EXPERIENCED, 2)
                .xp(Skill.AGILITY, 8000).xp(Skill.FISHING, 8000).xp(Skill.THIEVING, 8000).xp(Skill.HERBLORE, 8000) // OSRS (current quest XP rewards)
                .reqQuest("another_slice_of_ham", "fishing_contest")
                .req(Skill.AGILITY, 38).req(Skill.FISHING, 40).req(Skill.THIEVING, 45).req(Skill.HERBLORE, 48)
                .unlock("Access to Yu'biusk; goblin transmogrification potions")
                .stage("I can start this quest by speaking to Zanik in Dorgesh-Kaan. (Requires Another Slice of H.A.M., Fishing Contest, 38 Agility, 40 Fishing, 45 Thieving, 48 Herblore.)")
                .talk("zanik").say("The goblins of Goblin Village speak of Yu'biusk, the goblins' homeland. I must see it! Will you help me?")
                .stage("I should find a way to pass as a goblin. Snothead in Goblin Village may know of the old priests' potion.")
                .talk("snothead").take("guam_leaf", "vial_of_water", "goblin_mail").give("goblin_potion")
                .say("Old priests drink green stuff, become goblin! *Snothead mixes the potion.* Now you smell like goblin, nearly.")
                .missing("Need guam leaf, water in a vial, and goblin mail for de smell.")
                .stage("Disguised as a goblin, I should search the ancient goblin temple for the way to Yu'biusk.")
                .reach("quest_ancient_goblin_temple", 8).take("goblin_potion").give("huzamogaarb_whistle")
                .say("* Deep in the temple you find the remains of the priest Huzamogaarb and his whistle. A portal hums open.")
                .stage("I should take the whistle through the portal to Yu'biusk and speak to High Priest Bighead.")
                .talk("high_priest_bighead").take("huzamogaarb_whistle").say("A goblin with the priest's whistle! The Big High War God left us long ago... Yu'biusk is dead, but you have brought hope.")
                .stage("I should tell Zanik what I found.")
                .talk("zanik").say("Yu'biusk... the home of all goblins. Thank you for showing me. Now I know where we came from.");
    }

    private static void betweenARock(QuestBook b) {
        Cast.npc(b, "dondakan", "Dondakan the Dwarf", "quest_arzinian_mine", 0, 3, 0xD8A888, 0x8A6A2A);
        Cast.npc(b, "engineer_rolad", "Rolad", "dwarven_mine_north", 3, 3, 0xD8A889, 0x5A5A6A);
        Cast.npc(b, "dwarven_engineer", "Dwarven Engineer", "keldagrim", -3, -3, 0xD8A88A, 0x6A4A2A);
        // The gold helmet itself is in CommonItems (the systems module's anvil recipe smiths more after the quest).
        b.item("arzinian_schematic", "Schematic", "note", 0xD8C8A0, "A dwarven cannon schematic for firing a dwarf into a rock.");
        b.boss(BossDef.of("quest_arzinian_avatar", "Arzinian Avatar").level(150).stats(130, 100, 100, 100, 100, 100).speed(4)
                .attack(AttackDef.of("strength", AttackType.CRUSH, 18, 3, 4))
                .attack(AttackDef.of("ranging", AttackType.RANGED, 16, 9, 3))
                .attack(AttackDef.of("magic", AttackType.MAGIC, 16, 9, 3).particle("minecraft:enchant"))
                .drops(Drop.always("bones")).skin(0xE0C040, 0xC0A030, 0xA08020, 0xF0E080).size(1.0f, 2.4f, 1.0f)
                .at("quest_arzinian_mine").arena(10, "stone"));
        b.quest("between_a_rock", "Between a Rock...", Difficulty.EXPERIENCED, 2)
                .reqQuest("dwarf_cannon", "fishing_contest").req(Skill.DEFENCE, 30).req(Skill.MINING, 40).req(Skill.SMITHING, 50)
                .xp(Skill.DEFENCE, 5000).xp(Skill.MINING, 5000).xp(Skill.SMITHING, 5000).reward("gold_helmet")
                .unlock("Access to the Arzinian Mine")
                .stage("I can start this quest by speaking to Dondakan the Dwarf by the rock blocking the Arzinian Mine. (Requires Dwarf Cannon, Fishing Contest, 30 Defence, 40 Mining, 50 Smithing.)")
                .talk("dondakan").say("A huge rock blocks my mine! I have a plan: we build a cannon and fire a dwarf at it. Ask Rolad about the old schematics.")
                .stage("I should ask Rolad in the Dwarven Mine about the cannon schematics.")
                .talk("engineer_rolad").give("arzinian_schematic").say("My father's schematic! Fire a dwarf into a rock... he always was a dreamer. Take it, and good luck.")
                .stage("I should have the Dwarven Engineer in Keldagrim check the schematic, and make a gold helmet (3 gold bars).")
                .talk("dwarven_engineer").take("gold_bar:3").need("arzinian_schematic", "hammer").give("gold_helmet")
                .say("*You smith a gold helmet under his direction.* The schematic is sound, if insane. Wear the helmet; the mine's spirits dislike gold-less heads.")
                .missing("I'll need 3 gold bars and a hammer to make your helmet, and the schematic to check.")
                .stage("I should return to Dondakan with the schematic and the gold helmet.")
                .talk("dondakan").take("arzinian_schematic").need("gold_helmet").say("*BOOM!* You fly through the rock! Inside, the gold glows... and the Arzinian Avatar rises to guard it!")
                .stage("I must defeat the Arzinian Avatar, a being of living gold.")
                .kill("quest_arzinian_avatar").say("* The avatar crumbles into golden dust. The mine is open.")
                .stage("I should tell Dondakan.")
                .talk("dondakan").say("You did it! The Arzinian Mine is ours. Come back with your gold helmet and mine as you please.");
    }

    private static void forgettableTale(QuestBook b) {
        Cast.npc(b, "rowdy_dwarf", "Rowdy dwarf", "quest_keldagrim_brewery", 3, 3, 0xD8A88B, 0x8A3A2A);
        Cast.npc(b, "gauss", "Gauss", "quest_keldagrim_brewery", -3, 3, 0xD8A88C, 0x3A5A3A);
        Cast.npc(b, "kjut", "Kjut", "keldagrim", 4, 4, 0xD8A88D, 0x6A6A2A);
        b.items(s -> s.add("kelda_seed", "Kelda seed", Kind.SEED, "seed", 0x9A7A3A).value(1).stack(1000)
                .examine("A rare dwarven hop seed. Can only be grown underground."));
        b.item("kelda_hops", "Kelda hops", "produce", 0xA08A3A, "Hops for Kelda stout.");
        b.item("kelda_stout", "Kelda stout", "beer", 0x4A2A1A, "A pint of the rarest dwarven stout.");
        b.quest("forgettable_tale", "Forgettable Tale...", Difficulty.INTERMEDIATE, 2)
                .reqQuest("the_giant_dwarf", "fishing_contest").req(Skill.COOKING, 22).req(Skill.FARMING, 17)
                .xp(Skill.COOKING, 5000).xp(Skill.FARMING, 5000).reward("beer:2")
                .unlock("Kelda hops patch; access to the Keldagrim brewery puzzle")
                .stage("I can start this quest by speaking to Commander Veldaban in Keldagrim. (Requires The Giant Dwarf, Fishing Contest, 22 Cooking, 17 Farming.)")
                .talk("commander_veldaban").say("The Rowdy dwarf in the brewery claims to know something about the Red Axe. But he's too drunk to remember. Sober him up... or drink him under.")
                .stage("I should talk to the Rowdy dwarf at the Keldagrim brewery.")
                .talk("rowdy_dwarf").say("Hic! I'll tell you everything... for a pint of Kelda stout! Nobody's brewed it in years. Ask Gauss.")
                .stage("I should ask Gauss how Kelda stout is brewed, and get Kelda seeds from Kjut.")
                .talk("kjut").stay().unless("kelda_seed", "kelda_hops", "kelda_stout").take("coins:50").give("kelda_seed")
                .say("Kelda seeds? Rare as an honest Red Axe. 50 coins.")
                .missing("50 coins, friend.")
                .talk("gauss").take("kelda_seed").need("spade").give("kelda_hops")
                .say("Plant 'em in the cave patch by the brewery. *You plant the Kelda seed and tend it until the hops grow.*")
                .missing("Get Kelda seeds from Kjut in the market, and bring a spade.")
                .stage("I should brew the Kelda hops into stout with Gauss.")
                .talk("gauss").take("kelda_hops", "bucket_of_sand").give("kelda_stout")
                .say("*You load the vat. Days later...* A perfect pint of Kelda stout! Well, I used your bucket to clean the vat.")
                .missing("Bring the Kelda hops, and a bucket of sand to scour the vat.")
                .stage("I should give the Kelda stout to the Rowdy dwarf.")
                .talk("rowdy_dwarf").take("kelda_stout").say("Ahhh... the Red Axe! I remember now: they're meeting in the old Keldagrim mines... *He passes out.*",
                        "* You find your way through the brewery's barrel maze and overhear the Red Axe's plans.")
                .stage("I should tell Commander Veldaban what I heard.")
                .talk("commander_veldaban").say("The Red Axe again! Thanks to you we know their plans. Have a couple of beers on the Black Guard.");
    }
}
