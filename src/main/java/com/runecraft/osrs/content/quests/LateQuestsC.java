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

import java.util.List;
import java.util.Map;

/**
 * Remaining members' quests across the world: Tai Bwo Wannai Trio, Troll Romance, One Small Favour,
 * Rag and Bone Man II, A Soul's Bane, The Great Brain Robbery, The Fremennik Exiles, A Night at the
 * Theatre, A Porcine of Interest, Sleeping Giants and Pandemonium. XP-lamp rewards are real lamps the
 * player rubs ({@link QuestLamps}).
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
                // OSRS Wiki: 30 Firemaking is only recommended (karambwan can be cooked other ways).
                .reqQuest("jungle_potion").req(Skill.AGILITY, 15).req(Skill.COOKING, 30).req(Skill.FISHING, 5)
                .xp(Skill.ATTACK, 2500).xp(Skill.STRENGTH, 2500).xp(Skill.COOKING, 5000).xp(Skill.FISHING, 5000)
                .reward("coins:2000") // wiki: 2,000 coins from Timfraku
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
        b.quest("troll_romance", "Troll Romance", Difficulty.INTERMEDIATE, 2)
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
        // OSRS: Petra Fiyed is trapped in a rock sculpture in the Goblin Cave south-east of the Fishing Guild.
        b.spot("osf_goblin_cave", "Goblin Cave, north-east room", 2623, 3393); // approx: the cave entrance
        b.spot("osf_ham_hideout", "H.A.M. Hideout", 3166, 3251); // approx: the trapdoor
        b.spot("osf_fred", "Fred the Farmer's farmhouse", 3189, 3273);
        b.spot("osf_apothecary", "Varrock Apothecary", 3195, 3404);
        b.spot("osf_sanfew", "Sanfew's room, Taverley", 2897, 3426);
        b.spot("osf_dwarven_mine", "Dwarven Mine, west", 3016, 3450); // approx: the Ice Mountain entrance
        b.spot("osf_white_wolf_glider", "White Wolf Mountain gnome glider", 2847, 3498);
        b.spot("osf_seers_house", "House north of the Seers' chapel", 2738, 3507); // approx
        b.spot("osf_tindel", "Port Khazard south dock", 2673, 3150);
        b.spot("osf_feldip_glider", "Feldip Hills gnome glider", 2544, 2972);
        Cast.npc(b, "petra_fiyed", "Petra Fiyed", "osf_goblin_cave", 0, 3, Cast.PALE, 0x6A6A8A);
        Cast.npc(b, "osf_johanhus", "Johanhus Ulsbrecht", "osf_ham_hideout", 2, 2, Cast.FAIR, 0x6A5A3A);
        Cast.npc(b, "osf_tassie", "Tassie Slipcast", "BARBARIAN_VILLAGE", 4, -6, Cast.FAIR, 0x8A5A3A);
        Cast.npc(b, "osf_hammerspike", "Hammerspike Stoutbeard", "osf_dwarven_mine", 2, 2, Cast.TAN, 0x3A3A3A);
        Cast.npc(b, "osf_bleemadge", "Captain Bleemadge", "osf_white_wolf_glider", 2, 2, 0xC8946A, 0x6A4A2A);
        Cast.npc(b, "osf_phantuwti", "Phantuwti Fanstuwi Farsight", "osf_seers_house", 0, 2, Cast.FAIR, 0x3A3A8A);
        Cast.npc(b, "osf_gnormadium", "Gnormadium Avlafrim", "osf_feldip_glider", 2, 2, 0xC8946A, 0x4A6A2A);
        b.item("quest_animate_rock_scroll", "Animate rock scroll", "note", 0xC0B090, "A scroll that animates a rock golem.");
        b.item("quest_broken_cauldron", "Broken cauldron", "pot", 0x3A3A3A, "Aggie's cauldron. It has a hole in it."); // unused since round 25
        // OSRS Wiki: Slagilith level 92, 60 hitpoints, Attack 60, Strength 120, Defence 75, max hit 13, crush.
        b.boss(BossDef.of("quest_slagilith", "Slagilith").level(92).stats(60, 60, 120, 75, 1, 1).speed(4)
                .defenceBonus(50, 50, 5, 5, 50)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 13, 3, 6))
                .drops(Drop.always("iron_ore")).skin(0x6A6A6A, 0x5A5A5A, 0x4A4A4A, 0x8A8A8A).size(1.2f, 2.2f, 1.0f)
                .at("osf_goblin_cave").arena(8, "stone"));
        // Hammerspike's gang (levels 44, 48 and 49 in OSRS; the level-48 one stands in for all three).
        b.boss(BossDef.of("osf_dwarf_gang_member", "Dwarf gang member").level(48).stats(25, 80, 30, 25, 1, 1).speed(5)
                .defenceBonus(7, 7, 0, 0, 7)
                .attack(AttackDef.of("punch", AttackType.CRUSH, 4, 2.5, 6))
                .drops(Drop.always("bones")).skin(Cast.TAN, 0x3A3A3A, 0x2A2A2A, 0x5A3A2A).size(0.7f, 1.3f, 0.8f)
                .at("osf_dwarven_mine").arena(7, "stone"));
        b.item("osf_blunt_axe", "Blunt axe", "axe", 0x6A6A6A, "The jungle forester's axe. It's very blunt.");
        b.item("osf_sharp_axe", "Sharpened axe", "axe", 0x9A9AA0, "The jungle forester's axe, sharpened by Brian.");
        b.item("osf_herb_tea", "Herb tea", "vial", 0x8A6A3A, "Sanfew's special tea for the gnome pilot.");
        b.item("osf_red_mahogany", "Red mahogany log", "log", 0x8A2A1A, "A log of red mahogany for Yanni Salika.");
        b.quest("one_small_favour", "One Small Favour", Difficulty.EXPERIENCED, 2)
                .reqQuest("RUNE_MYSTERIES", "shilo_village", "DRUIDIC_RITUAL")
                .req(Skill.AGILITY, 36).req(Skill.CRAFTING, 25).req(Skill.HERBLORE, 18).req(Skill.SMITHING, 30)
                // two antique lamps of 10,000 XP in any skill of level 30 or above (QuestLamps)
                .stage("I can start this quest by speaking to Yanni Salika in Shilo Village. (Requires Rune Mysteries, Shilo Village, Druidic Ritual, 36 Agility, 25 Crafting, 18 Herblore, 30 Smithing.)")
                .talk("yanni_salika").say("Could you do me one small favour? The jungle foresters south of here owe me some red mahogany.")
                .stage("I should ask a Jungle forester south of Shilo Village about the red mahogany.")
                .talk("jungle_forester").give("osf_blunt_axe").say("Red mahogany? Not with this axe. Brian in Port Sarim could sharpen it... small favour?")
                // OSRS chain of favours: Brian -> Aggie -> Johanhus -> Fred -> Seth -> Horvik -> Apothecary -> Tassie -> Hammerspike
                // -> Sanfew -> Captain Bleemadge -> Arhein -> Phantuwti -> Petra's sculpture -> Cromperty -> Tindel -> Rantz -> Gnormadium.
                .stage("I should ask Brian in Port Sarim to sharpen the forester's axe... and see where that leads.")
                .talk("quest_brian_archery").stay().need("osf_blunt_axe").unlessFlag("osf_brian").flag("osf_brian")
                .say("A cursed axe? I'll sharpen it, but I need a character witness first. Ask Aggie in Draynor.")
                .missing("Sharpen what? You'll need to bring me the axe.")
                .talk("aggie").stay().needFlag("osf_brian").unlessFlag("osf_aggie").flag("osf_aggie")
                .say("A character witness? Only if you find my friend Jimmy the Chisel. The H.A.M. have him locked up.")
                .talk("osf_johanhus").stay().needFlag("osf_aggie").unlessFlag("osf_johanhus").flag("osf_johanhus")
                .say("Jimmy's worth a few chickens to me. Get Fred the Farmer to sell me some.")
                .reach("osf_fred", 5).stay().needFlag("osf_johanhus").unlessFlag("osf_fred").flag("osf_fred")
                .say("* Fred will part with chickens if Seth Groats lends him some cages.")
                .talk("quest_seth_groats").stay().needFlag("osf_fred").unlessFlag("osf_seth").flag("osf_seth")
                .say("Cages? Horvik in Varrock makes 'em. He owes me. Well, I owe him. One of us owes someone.")
                .talk("quest_horvik").stay().needFlag("osf_seth").unlessFlag("osf_horvik").take("steel_bar:3").flag("osf_horvik")
                .say("Chicken cages, fine - three steel bars. But my lungs are bad from the forge. The Apothecary owes me a tincture.")
                .missing("I'll need three steel bars before I make any chicken cages.")
                .reach("osf_apothecary", 4).stay().needFlag("osf_horvik").unlessFlag("osf_apothecary").flag("osf_apothecary")
                .say("* The Apothecary needs a pot lid from Tassie Slipcast in Barbarian Village to brew Horvik's tincture.")
                .talk("osf_tassie").stay().needFlag("osf_apothecary").unlessFlag("osf_tassie").flag("osf_tassie")
                .say("A lid? Gladly - once Hammerspike's gang stops shaking down my pottery. Talk to him in the Dwarven Mine.")
                .talk("osf_hammerspike").stay().needFlag("osf_tassie").unlessFlag("osf_hammerspike").flag("osf_hammerspike")
                .say("Leave the potter alone? Get me into the druids' circle and maybe. Ask Sanfew about initiates.")
                .reach("osf_sanfew", 4).stay().needFlag("osf_hammerspike").unlessFlag("osf_sanfew")
                .take("cup_of_hot_water", "harralander", "guam_leaf:2", "marrentill").give("osf_herb_tea").flag("osf_sanfew")
                .say("* Sanfew will take a dwarf initiate if the gnome pilot flies him to the ogres. He shows you how to brew a special tea:",
                        "* hot water in a teacup, a harralander, two guam leaves and a marrentill.")
                .missing("* Sanfew's tea needs a cup of hot water, a harralander, two guam leaves and a marrentill.")
                .talk("osf_bleemadge").needFlag("osf_sanfew").take("osf_herb_tea")
                .say("Lovely tea. Fly a druid to the ogres? Only if Arhein in Catherby sends my T.R.A.S.H. - Transport, Repair And Spare Hardware.")
                .missing("Got something for me? Sanfew usually sends tea.")
                .stage("I should arrange Captain Bleemadge's T.R.A.S.H. with Arhein in Catherby... and whatever that takes.")
                .talk("quest_ca_arhein").stay().unlessFlag("osf_arhein").flag("osf_arhein")
                .say("I'll ship it when I know the weather. Phantuwti in Seers' Village does forecasts.")
                .talk("osf_phantuwti").stay().needFlag("osf_arhein").unlessFlag("osf_phantuwti").flag("osf_phantuwti")
                .say("My seeing-stones are clouded! Petra, my seer friend, went to the Goblin Cave and never came back. Find her.")
                .reach("osf_goblin_cave", 6).stay().needFlag("osf_phantuwti").unlessFlag("osf_sculpture").flag("osf_sculpture")
                .say("* In the north-east room of the Goblin Cave stands a sculpture of a woman. It looks exactly like Petra. It IS Petra.")
                .talk("wizard_cromperty").stay().needFlag("osf_sculpture").unlessFlag("osf_cromperty").flag("osf_cromperty")
                .say("Turned to stone? I can write a scroll to undo it... if you bring me iron oxide. Tindel Marchant in Port Khazard has some.")
                .reach("osf_tindel", 5).stay().needFlag("osf_cromperty").unlessFlag("osf_tindel").flag("osf_tindel")
                .say("* Tindel will trade his iron oxide for a feather mattress. Rantz the ogre in the Feldip Hills makes them.")
                .talk("rantz").stay().needFlag("osf_tindel").unlessFlag("osf_rantz").flag("osf_rantz")
                .say("Rantz make mattress if gnome stop flying noisy glider over Rantz cave. Gnome lights broken. Fix lights.")
                .talk("osf_gnormadium").needFlag("osf_rantz").need("chisel")
                .say("* You search each landing light, cut its gem with your chisel and put it back. The glider lights shine true.",
                        "Splendid! Tell the ogre I'll use the new approach.")
                .missing("The landing lights' gems are all rough. You'll need a chisel to cut them.")
                .stage("I should report back up the chain: Rantz, Tindel and Wizard Cromperty.")
                .talk("rantz").stay().unlessFlag("osf_mattress").flag("osf_mattress").say("Gnome quiet now. Here mattress.")
                .reach("osf_tindel", 5).stay().needFlag("osf_mattress").unlessFlag("osf_oxide").flag("osf_oxide")
                .say("* Tindel takes the mattress and hands over the iron oxide.")
                .talk("wizard_cromperty").needFlag("osf_oxide").give("quest_animate_rock_scroll", "pigeon_cage:5")
                .say("Iron oxide! Here's your scroll. Read it by the sculpture... though it may animate the wrong rock first.",
                        "* On your way out you take five pigeon cages from south of the north Ardougne bank; Horvik will want them.")
                .missing("No iron oxide, no scroll.")
                .stage("I should read the animate rock scroll by Petra's sculpture in the Goblin Cave.")
                .reach("osf_goblin_cave", 6).stay().need("quest_animate_rock_scroll").unlessFlag("osf_slagilith").spawn("quest_slagilith")
                .say("* You read the scroll. The wrong rock animates: a Slagilith lurches out of the wall!")
                .kill("quest_slagilith").stay().unlessFlag("osf_slagilith").flag("osf_slagilith")
                .say("* The Slagilith crumbles into slag.")
                .talk("petra_fiyed").needFlag("osf_slagilith").take("quest_animate_rock_scroll")
                .say("* You read the scroll again and Petra steps out of the stone.", "Free! Tell Phantuwti I'm on my way home.")
                .missing("* The sculpture doesn't answer. Perhaps reading the animate rock scroll here would help.")
                .stage("Now every favour has to be repaid, all the way back to Yanni Salika.")
                .talk("osf_phantuwti").stay().unlessFlag("osf_vane").need("hammer", "bronze_bar", "iron_bar", "steel_bar").flag("osf_vane")
                .say("Petra's back, but my weather vane is broken! *You hammer off its three parts, fix each at the anvil with bronze, iron and steel bars, and refit them.*",
                        "A clear forecast! Take it to Arhein.")
                .missing("The weather vane on my roof needs fixing: bring a hammer and a bronze, an iron and a steel bar.")
                .talk("quest_ca_arhein").stay().needFlag("osf_vane").unlessFlag("osf_trash").flag("osf_trash").say("Fair winds. The T.R.A.S.H. is on its way.")
                .talk("osf_bleemadge").stay().needFlag("osf_trash").unlessFlag("osf_flight").flag("osf_flight").say("Got my T.R.A.S.H. Tell the druid I'll fly the dwarf.")
                .reach("osf_sanfew", 4).stay().needFlag("osf_flight").unlessFlag("osf_initiate").flag("osf_initiate")
                .say("* Sanfew agrees to take Hammerspike's man as an initiate.")
                .talk("osf_hammerspike").stay().needFlag("osf_initiate").unlessFlag("osf_gang").flag("osf_gang").spawn("osf_dwarf_gang_member")
                .say("An initiate! Tassie can... hey, the lads don't like this. Lads?")
                .kill("osf_dwarf_gang_member", 3).stay().needFlag("osf_gang").unlessFlag("osf_gang_done").flag("osf_gang_done")
                .say("* Hammerspike's gang lies groaning. 'Fine! The potter's safe!'")
                .talk("osf_tassie").stay().needFlag("osf_gang_done").unlessFlag("osf_lid").take("empty_pot").flag("osf_lid")
                .say("Thank you! Here's some clay - spin it into a lid and fire it. *You fit the pot lid to your pot.*")
                .missing("Bring an empty pot and I'll help you make a lid for it.")
                .reach("osf_apothecary", 4).stay().needFlag("osf_lid").unlessFlag("osf_tincture").flag("osf_tincture")
                .say("* The Apothecary seals Horvik's tincture and breathing salts in your lidded pot.")
                .talk("quest_horvik").stay().needFlag("osf_tincture").unlessFlag("osf_cages").take("pigeon_cage:5").flag("osf_cages")
                .say("Ahh, I can breathe! Five pigeon cages and I'll have your chicken cages ready.")
                .missing("Bring me five pigeon cages and I'll turn them into chicken cages.")
                .talk("quest_seth_groats").stay().needFlag("osf_cages").unlessFlag("osf_chickens").flag("osf_chickens").say("Cages! Fred can have his chickens.")
                .talk("osf_johanhus").stay().needFlag("osf_chickens").unlessFlag("osf_jimmy").flag("osf_jimmy").say("A chicken deal? Jimmy's free.")
                .talk("aggie").stay().needFlag("osf_jimmy").unlessFlag("osf_witness").flag("osf_witness").say("Jimmy's out? Then I'll vouch for you to Brian.")
                .talk("quest_brian_archery").stay().needFlag("osf_witness").unless("osf_sharp_axe").unlessFlag("osf_mahogany").take("osf_blunt_axe")
                .give("osf_sharp_axe").say("Aggie vouches for you? Then here's your axe, sharp as anything.")
                .talk("jungle_forester").stay().needFlag("osf_witness").unlessFlag("osf_mahogany").take("osf_sharp_axe").give("osf_red_mahogany").flag("osf_mahogany")
                .say("My axe! Sharp as the day I bought it. Here's Yanni's red mahogany.")
                .talk("yanni_salika").needFlag("osf_mahogany").take("osf_red_mahogany")
                .say("Red mahogany! Was it any trouble? No? Splendid. Here's a little something for your one small favour.");
    }

    private static void ragAndBoneManII(QuestBook b) {
        b.item("quest_bone_crate", "Crate of bones", "package", 0xC0B090, "A crate full of rare bones for the Odd Old Man.");
        b.item("bonesack", "Bonesack", "cape", 0xD0C0A0, "A sack of bones, worn on the back.");
        b.items(s -> s.add("ram_skull_helm", "Ram skull helm", Kind.ARMOUR, "full_helm", 0xE0D8C0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, -6, -2, 10, 12, 8, -1, 10, 0, 0, 0, 2), 1))
                .examine("A helmet made from a ram's skull."));
        b.gear("bonesack", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 2), "rag_and_bone_man_ii");
        b.quest("rag_and_bone_man_ii", "Rag and Bone Man II", Difficulty.EXPERIENCED, 1)
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
        b.quest("a_souls_bane", "A Soul's Bane", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.DEFENCE, 500).xp(Skill.HITPOINTS, 500).reward("coins:500") // wiki: 500 coins
                .unlock("Tolna's Rift, home of angry, fearful and confused souls")
                .stage("I can start this quest by speaking to Launa by the rift north-east of Varrock.")
                .talk("launa").say("My son Tolna climbed into this rift years ago and never came back. Will you go down and find him? Take a rope.")
                .stage("I should climb down into Tolna's Rift with a rope and face the manifestations of anger, fear, confusion and hopelessness.")
                .reach("quest_tolnas_rift", 6).need("rope").say("* Angry bears and unicorns, hopeless creatures and confused shapes swarm around you. You push through until a reaper blocks the way.")
                .missing("* The rift is too deep to climb without a rope.")
                // OSRS: fear reapers hide in the dark holes of the fear cave; killing 5 or 6 of them ends the room.
                .stage("I must defeat the Fear reapers hiding in the dark holes of the fear cave.")
                .kill("quest_fear_reaper", 5).flag("asb_reaper")
                .say("* The last reaper dissolves. Past the confusion and hopelessness caves, at the bottom of the rift, sits a young man, alone with his memories.")
                .stage("I should talk to Tolna.")
                .talk("tolna").needFlag("asb_reaper").flag("asb_tolna").say("Mother? She's still waiting for me? I let my anger and fear trap me here... I'll go up.")
                .stage("I should tell Launa that Tolna is safe.")
                .talk("launa").needFlag("asb_tolna").say("My boy! You brought him back to me. Thank you, adventurer, thank you.");
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
                .xp(Skill.PRAYER, 6000).xp(Skill.CRAFTING, 3000).xp(Skill.CONSTRUCTION, 2000).reward("barrelchest_anchor")
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

    /**
     * The Fremennik Exiles after the wiki quick guide and quest-helper (TheFremennikExiles): the basilisk youngling in the sandpit
     * and Freygerd's investigation, V's shield remade (a Fremennik shield, lunar glass from Baba Yaga and the Astral Altar, V's
     * sigil smithed from lunar bars and enchanted by the Fossegrimen, the polishing rock from the geyser), the basilisks in the
     * market, and the Island of Stone: the cave door's rune code, Typhor and the Jormungand. OSRS has no exile called Lady V:
     * the exiles are the basilisks' masters, and V's shield is restored by the player.
     */
    private static void fremennikExiles(QuestBook b) {
        b.items(s -> s.add("vs_shield", "V's shield", Kind.SHIELD, "kiteshield", 0x5A4A3A).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, -8, -2, 50, 52, 48, 0, 50, 0, 0, 0, 0), 60))
                .examine("The shield of V. Its special attack damages nearby enemies."));
        // quest-helper tiles: Freygerd's house south of the sandpit (2668, 3703; the sandpit 2668, 3708), Brundt south-east of
        // Rellekka (2705, 3634), the large geyser north of the Mountain Camp (2766, 3675), Rellekka's market (2644, 3677), the cave
        // on the Island of Stone (2465, 4012), the Astral Altar (2158, 3864) and the Lunar Isle mine's ladder (2142, 3944)
        b.npcAt("freygerd", "Freygerd", 2668, 3703, 0xF0C8A8, 0x8A6A4A, 0x5A4A3A, 0xE0B060);
        b.npcAt("brundt_exiles", "Brundt the Chieftain", 2705, 3634, 0xF0C8A9, 0x8A5A2B, 0x5A3A1B, 0xE0A041);
        b.spot("fe_sandpit", "Rellekka sandpit", 2668, 3708);
        b.spot("fe_geyser", "Large geyser", 2766, 3675);
        b.spot("fe_rellekka_market", "Rellekka market", 2644, 3677);
        b.spot("fe_island_cave", "Cave, Island of Stone", 2465, 4012);
        b.spot("fe_astral_altar", "Astral Altar", 2158, 3864);
        b.spot("fe_lunar_mine", "Lunar Isle mine", 2142, 3944);
        b.npcAt("baba_yaga", "Baba Yaga", 2085, 3931, 0xD8C0B0, 0x6A4A6A, 0x4A2A4A, 0x2A1A2A); // her house walks on chicken legs
        b.item("fe_unsealed_letter", "Unsealed letter", "note", 0xE0D8B0, "A letter the basilisk youngling was guarding.");
        b.item("fe_fang", "Fang", "bones", 0xE0E0D0, "A basilisk's fang, from the rockslide.");
        b.item("fe_venom_gland", "Venom gland", "secondary", 0x6A8A3A, "A basilisk's venom gland.");
        b.item("molten_glass_i", "Molten glass (i)", "glass", 0xC0A0E0, "Molten glass, infused by Baba Yaga.");
        b.item("lunar_glass", "Lunar glass", "glass", 0xA0C0F0, "Glass infused with astral energy.");
        b.item("lunar_ore", "Lunar ore", "ore", 0x8A9AC0, "Ore from the Lunar Isle mine.");
        b.item("lunar_bar", "Lunar bar", "bar", 0xA0B0D0, "A bar of lunar metal.");
        b.item("v_sigil", "V sigil", "amulet", 0x8A8AA0, "The sigil of V, smithed from lunar bars.");
        b.item("v_sigil_e", "V sigil (e)", "amulet", 0x6AA0C0, "The sigil of V, enchanted by the Fossegrimen.");
        b.item("polishing_rock", "Polishing rock", "ore", 0xC0C0C8, "A rock polished smooth by the geyser.");
        b.item("keg_of_beer", "Keg of beer", "pot_item", 0x8A6A3A, "A keg of Fremennik beer from the longhall.");
        // Thora the Barkeep in the longhall (quest-helper 2662, 3673) sells the kegs (wiki quick guide: 650 coins for two)
        b.npcAt("thora_the_barkeep", "Thora the Barkeep", 2662, 3673, 0xF0C8A8, 0x8A4A3A, 0x5A3A2A, 0xE0B050);
        // The quest's foes (osrs-dps-calc): the basilisk youngling (level 57, 60 HP, slash up to 5), the monstrous basilisks of the
        // market (level 135), Typhor (218, 360 HP, up to 20, magic and melee) and the Jormungand (363, 600 HP, up to 22, magic and
        // melee). All of them petrify without a mirror shield or V's shield.
        b.boss(BossDef.of("quest_basilisk_boss", "Basilisk Youngling").level(57).stats(60, 25, 40, 70, 1, 1).speed(4)
                .defenceBonus(20, 20, 0, 20, 0)
                .attack(AttackDef.of("bite", AttackType.SLASH, 5, 2.5, 3))
                .attack(AttackDef.of("gaze", AttackType.MAGIC, 5, 6, 1).special(Special.DRAIN_STAT, 1)) // drains stats without a mirror shield
                .drops(Drop.always("bones")).skin(0x5A7A4A, 0x3A5A2A, 0x2A3A1A, 0xE0C040).size(1.0f, 0.9f, 0.9f));
        b.boss(BossDef.of("fe_monstrous_basilisk", "Monstrous Basilisk").level(135).stats(170, 88, 98, 130, 1, 1).speed(4)
                .defenceBonus(35, 35, 0, 35, 0)
                .attack(AttackDef.of("bite", AttackType.SLASH, 11, 2.5, 4))
                // its stare drains stats without the shield // approx: a telegraphed stare of up to 11
                .attack(AttackDef.of("stare", AttackType.MAGIC, 11, 8, 1).special(Special.DRAIN_STAT, 1)
                        .telegraph(2, "The basilisk's eyes glow: keep V's shield raised!"))
                .drops(Drop.always("big_bones")).model("basilisk").skin(0x4A6A3A, 0x2A4A1A, 0x1A2A0A, 0xE0C040).size(1.4f, 1.3f, 1.4f));
        b.boss(BossDef.of("typhor", "Typhor").level(218).stats(360, 186, 186, 186, 186, 1).speed(4)
                .defenceBonus(30, 30, 0, 30, 0)
                .attack(AttackDef.of("claws", AttackType.SLASH, 20, 2.5, 3))
                .attack(AttackDef.of("stone_gaze", AttackType.MAGIC, 20, 9, 3).particle("minecraft:ash"))
                .drops(Drop.always("big_bones")).model("basilisk_knight").skin(0x5A6A4A, 0x3A4A2A, 0x2A2A1A, 0xC0A040).size(1.0f, 2.6f, 1.2f)
                .at("quest_island_of_stone").arena(12, "stone"));
        b.boss(BossDef.of("the_jormungand", "The Jormungand").level(363).stats(600, 214, 214, 214, 214, 214).speed(5)
                .defenceBonus(50, 50, 10, 50, 50)
                .attack(AttackDef.of("bite", AttackType.STAB, 22, 3, 3))
                .attack(AttackDef.of("venom_spit", AttackType.MAGIC, 22, 10, 3).particle("minecraft:item_slime"))
                // wiki: when the screen turns red, face away (50+ and a stat drain); frozen in rock, attack the snake to break free
                // (30+ when the rock explodes) // playability: a marked strike to step out of, and a freeze with a later blast
                .attack(AttackDef.of("petrifying_stare", AttackType.MAGIC, 50, 10, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Jormungand's stare turns the cave red: turn away!").particle("minecraft:ash"))
                .attack(AttackDef.of("rock_prison", AttackType.CRUSH, 30, 10, 1).special(Special.FREEZE, 4)
                        .telegraph(2, "Rock creeps over your legs: hit the snake to break free!"))
                .drops(Drop.always("big_bones")).skin(0x5A6A4A, 0x3A4A3A, 0x2A3A2A, 0xC0E040).size(2.0f, 2.4f, 2.0f));
        b.quest("the_fremennik_exiles", "The Fremennik Exiles", Difficulty.MASTER, 2)
                .req(Skill.MINING, 60)
                .xp(Skill.SLAYER, 50000).xp(Skill.CRAFTING, 50000).xp(Skill.RUNECRAFT, 30000) // OSRS (current quest XP rewards)
                .reqQuest("the_fremennik_isles", "lunar_diplomacy", "mountain_daughter", "heroes_quest")
                .req(Skill.CRAFTING, 65).req(Skill.SLAYER, 60).req(Skill.SMITHING, 60).req(Skill.FISHING, 60).req(Skill.RUNECRAFT, 55)
                .reward("vs_shield")
                .unlock("Basilisk Knights in Jormungand's Prison; the Neitiznot faceguard")
                .stage("I can start this quest by speaking to Brundt the Chieftain in Rellekka. (Requires The Fremennik Isles, Lunar Diplomacy, Mountain Daughter, Heroes' Quest, 65 Crafting, 60 Slayer, 60 Smithing, 60 Fishing, 60 Mining, 55 Runecraft.)")
                .talk("brundt").say("Something's been taking our people's pets and livestock. Freygerd, north of the market, saw something by her house.")
                // ---- A disturbance in Rellekka ----
                .stage("I should talk to Freygerd in the house south of Rellekka's sandpit and find out what is going on.")
                .talk("freygerd").stay().unlessFlag("fe_freygerd").flag("fe_freygerd")
                .say("Something lives in the sandpit! I hear it at night. Be careful: they say looking at those things turns you to stone.")
                .reach("fe_sandpit", 4).stay().needFlag("fe_freygerd").unlessFlag("fe_youngling").need("mirror_shield").spawn("quest_basilisk_boss")
                .say("* You search the sandpit. A basilisk youngling bursts out of the sand!")
                .missing("* Something stirs in the sandpit. Without a mirror shield its gaze could turn you to stone.")
                .kill("quest_basilisk_boss").stay().needFlag("fe_freygerd").unlessFlag("fe_youngling").need("mirror_shield")
                .flag("fe_youngling").give("fe_unsealed_letter")
                .say("* The youngling dies. In the sand lies an unsealed letter about basilisks being smuggled into Rellekka.")
                .reach("fe_sandpit", 8).stay().needFlag("fe_youngling").unless("fe_fang").unlessFlag("fe_clues").give("fe_fang", "fe_venom_gland")
                .say("* In the rockslide west of the house you find a fang, and in the boxes to the south a venom gland.")
                .talk("freygerd").stay().needFlag("fe_youngling").unlessFlag("fe_clues").take("fe_fang", "fe_venom_gland").flag("fe_clues")
                .say("A basilisk's fang and venom... someone's been raising them here. Tell Brundt.")
                .talk("brundt").needFlag("fe_clues").take("fe_unsealed_letter")
                .say("Basilisks, in Rellekka! This letter names the exiles who serve them. Meet me south-east of the town.")
                .missing("?!fe_youngling|Have you spoken to Freygerd? She lives south of the sandpit.")
                // ---- Redeeming the exile: V's shield ----
                .stage("I should restore V's shield: a Fremennik shield, lunar glass, V's sigil and a polishing rock, put together with a "
                        + "glassblowing pipe.")
                .talk("brundt_exiles").stay().unlessFlag("fe_plan").flag("fe_plan")
                .say("> Where can I find a Fremennik shield?", "From the dagannoth of Waterbirth, Rellekka's guards, or Bardur, deep in "
                                + "Waterbirth's dungeon.",
                        "> Where can I get lunar glass?", "Baba Yaga on Lunar Isle infuses molten glass; the Astral Altar does the rest.",
                        "> How do I make the polishing rock?", "The large geyser past the Mountain Camp polishes stone, if you can bear "
                                + "the cold.",
                        "> How do I make V's sigil?", "Smith it from lunar bars, and have the Fossegrimen enchant it.")
                // the Fremennik shield (wiki: an uncommon drop from the dagannoth of Waterbirth Island; Bardur sells it, the ring of
                // charos (a) for free) // approx: the drop rate
                .drop("dagannoth", "fremennik_shield", 32).needFlag("fe_plan").unless("vs_shield")
                .talk("baba_yaga").stay().needFlag("fe_plan").unless("molten_glass_i", "lunar_glass").take("molten_glass").give("molten_glass_i")
                .say("* Baba Yaga infuses your molten glass with the Moon Clan's magic.")
                .reach("fe_lunar_mine", 6).stay().needFlag("fe_plan").unless("lunar_ore:3", "lunar_bar", "v_sigil", "v_sigil_e")
                .needTool("pickaxe").give("lunar_ore:3")
                .say("* Down the Lunar Isle mine you mine the stalagmites for three lunar ore.")
                .reach("fe_astral_altar", 4).stay().needFlag("fe_plan").take("molten_glass_i", "astral_rune:100").give("lunar_glass")
                .say("* At the Astral Altar, 100 astral runes pour into the glass: lunar glass.")
                .missing("* Lunar glass takes molten glass (i) and 100 astral runes.")
                .reach("RELLEKKA", 12).stay().needFlag("fe_plan").take("lunar_ore:3").need("hammer").give("v_sigil").reqSkill(Skill.SMITHING, 60)
                .say("* You smelt three lunar bars at Rellekka's furnace and smith V's sigil on the anvil.")
                .talk("fossegrimen").stay().needFlag("fe_plan").take("v_sigil").give("v_sigil_e")
                .say("> Ask about V's Sigil.", "V's sigil... I remember her. There: it is enchanted.")
                .reach("fe_geyser", 4).stay().needFlag("fe_plan").unlessFlag("fe_geyser").take("pet_rock").flag("fe_geyser")
                .say("* You drop your pet rock into the large geyser. It's for the good of the Province!")
                .missing("* The large geyser could polish a stone. A pet rock, say.")
                .talk("peer").stay().needFlag("fe_geyser").unlessFlag("fe_peer").flag("fe_peer")
                .say("The geyser is bitterly cold below. Ice gloves will let you reach in.")
                .reach("fe_geyser", 4).stay().needFlag("fe_peer").unless("polishing_rock", "vs_shield").need("ice_gloves").give("polishing_rock")
                .say("* Wearing ice gloves you reach into the geyser and pull out your rock, polished smooth.")
                .talk("brundt_exiles").need("glassblowing_pipe").take("fremennik_shield", "lunar_glass", "v_sigil_e", "polishing_rock")
                .give("vs_shield").reqSkill(Skill.CRAFTING, 65)
                .say("* With the glassblowing pipe you set the lunar glass and V's sigil into the Fremennik shield and polish it: V's shield.",
                        "V's shield, whole again! Now we can face what's coming.")
                .missing("Bring the Fremennik shield, lunar glass, V's sigil (e) and the polishing rock, and a glassblowing pipe.")
                // ---- Basilisks in the Market ----
                .stage("I should defend Rellekka's market from the basilisks with V's shield.")
                .talk("thora_the_barkeep").stay().unless("keg_of_beer:2").unlessFlag("fe_market").take("coins:650").give("keg_of_beer:2")
                .say("Two kegs of beer for the chieftain? That'll be 650 coins.")
                .missing("Two kegs are 650 coins, friend.")
                .talk("brundt").stay().unlessFlag("fe_market").take("keg_of_beer:2").flag("fe_market")
                .say("Two kegs of beer for the longhall, and we're ready. They're coming through the market!")
                .missing("Bring two kegs of beer: the men need courage.")
                .reach("fe_rellekka_market", 8).stay().needFlag("fe_market").need("vs_shield").spawn("fe_monstrous_basilisk")
                .say("* A monstrous basilisk crashes through the market stalls!")
                // wiki: a monstrous basilisk counts for 10% of the defence, a basilisk for 3.33% // approx: the market's basilisks are
                // all monstrous here
                .kill("fe_monstrous_basilisk", 10).needFlag("fe_market").need("vs_shield")
                .say("* The last basilisk falls. Brundt readies the boat to the Island of Stone.")
                // ---- Island of Stone ----
                .stage("I should sail to the Island of Stone with Brundt, open the cave's door and end the Jormungand's threat.")
                .puzzle("fe_island_cave", FE_CAVE_DOOR).stay().unlessFlag("fe_door").flag("fe_door")
                .say("* The fourth rune glows green. The door grinds open on a cave full of stone.")
                .reach("fe_island_cave", 6).stay().needFlag("fe_door").unlessFlag("fe_typhor").need("vs_shield").spawn("typhor")
                .say("* In the cave, Typhor, the basilisks' king, turns his gaze on you!")
                .kill("typhor").stay().needFlag("fe_door").unlessFlag("fe_typhor").need("vs_shield").flag("fe_typhor")
                .say("* Typhor falls. The cave shakes: something vast is waking.")
                .reach("fe_island_cave", 6).stay().needFlag("fe_typhor").need("vs_shield").spawn("the_jormungand")
                .say("* The Jormungand breaks free of its prison!")
                .kill("the_jormungand").needFlag("fe_typhor").need("vs_shield")
                .say("* The Jormungand crumbles back into stone. Brundt: The Fremennik will sing of this. Keep V's shield: it was always yours "
                        + "to bear.");
    }

    /** The cave door's runes (wiki: air, water, earth, fire, astral and cosmic, a rune may repeat). */
    static final String[] FE_RUNES = {"Air", "Water", "Earth", "Fire", "Astral", "Cosmic"};

    /** One board of the cave door: the code and the guesses already on it, whose checks leave only that code. */
    record FeBoard(int[] code, List<int[]> guesses) {
    }

    /** A guess's green checks (right rune, right place) and red checks (right rune, wrong place), as in Mastermind. */
    static int[] feChecks(int[] code, int[] guess) {
        int green = 0;
        int[] codeLeft = new int[FE_RUNES.length];
        int[] guessLeft = new int[FE_RUNES.length];
        for (int i = 0; i < 4; i++) {
            if (code[i] == guess[i]) {
                green++;
            } else {
                codeLeft[code[i]]++;
                guessLeft[guess[i]]++;
            }
        }
        int red = 0;
        for (int r = 0; r < FE_RUNES.length; r++) {
            red += Math.min(codeLeft[r], guessLeft[r]);
        }
        return new int[]{green, red};
    }

    /** Every four-rune code the guesses' checks allow on a board for {@code code}. */
    static List<int[]> feConsistent(int[] code, List<int[]> guesses) {
        List<int[]> out = new java.util.ArrayList<>();
        for (int n = 0; n < 1296; n++) {
            int[] c = {n / 216, n / 36 % 6, n / 6 % 6, n % 6};
            boolean ok = true;
            for (int[] g : guesses) {
                ok &= java.util.Arrays.equals(feChecks(code, g), feChecks(c, g));
            }
            if (ok) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Eight boards (wiki: in OSRS the code is random and the board fills as the player guesses): each shows guesses drawn from the
     * codes still possible until their checks leave one code, at most eight guesses.
     */
    static final List<FeBoard> FE_BOARDS = feBoards();

    private static List<FeBoard> feBoards() {
        java.util.Random rng = new java.util.Random(0x5EEDF0EL);
        List<FeBoard> boards = new java.util.ArrayList<>();
        while (boards.size() < 8) {
            int[] code = {rng.nextInt(6), rng.nextInt(6), rng.nextInt(6), rng.nextInt(6)};
            List<int[]> guesses = new java.util.ArrayList<>();
            List<int[]> left = feConsistent(code, guesses);
            while (left.size() > 1 && guesses.size() < 8) {
                int[] g = left.get(rng.nextInt(left.size()));
                if (java.util.Arrays.equals(g, code)) {
                    continue;
                }
                guesses.add(g);
                left = feConsistent(code, guesses);
            }
            if (left.size() == 1) {
                boards.add(new FeBoard(code, List.copyOf(guesses)));
            }
        }
        return List.copyOf(boards);
    }

    /** The Island of Stone's cave door (wiki: Mastermind with six runes): deduce the four runes from the guesses on the board. */
    static final QuestPuzzle FE_CAVE_DOOR = feCaveDoor();

    private static QuestPuzzle feCaveDoor() {
        QuestPuzzle.Builder b = QuestPuzzle.builder("fe_cave_door").room("door");
        for (FeBoard board : FE_BOARDS) {
            List<String> prompt = new java.util.ArrayList<>();
            prompt.add("* The basilisks resealed the cave with a rune lock. Each green check is a right rune in the right place, each red "
                    + "check a right rune in the wrong place; a rune may appear more than once. Earlier tries on the board:");
            for (int[] g : board.guesses()) {
                int[] checks = feChecks(board.code(), g);
                prompt.add("* " + FE_RUNES[g[0]] + ", " + FE_RUNES[g[1]] + ", " + FE_RUNES[g[2]] + ", " + FE_RUNES[g[3]] + ": " + checks[0]
                        + " green, " + checks[1] + " red");
            }
            prompt.add("* Set the four runes, left to right.");
            b.variant(null).prompt(prompt.toArray(String[]::new)).options(FE_RUNES).solution(board.code());
        }
        return b.failLines("* The runes flash red and the board clears.").build();
    }

    private static void nightAtTheTheatre(QuestBook b) {
        Cast.npc(b, "quest_mysterious_stranger_tob", "Mysterious Stranger", "quest_ver_sinhaza", 3, -3, Cast.VAMPYRE, 0x2A2A2A);
        b.npcAt("daer_krand", "Daer Krand", 3728, 3302, 0xE0C8B0, 0x5A3A6A, 0x3A2A4A, 0xC0C0C0); // approx: the Sisterhood Sanctuary, Slepe
        b.spot("ant_ranis_crypt", "Ranis Drakan's crypt", 3673, 3232); // approx: just north of the Stranger
        b.spot("ant_araxyte_nest", "Araxyte nest", 3658, 3405); // approx: north-west of Slepe
        // wiki (Flower (A Night at the Theatre)): its four flowers stand at 3504-3512, 3353-3361 round it, on the island west of
        // Castle Drakan
        b.spot("ant_hespori_island", "Hespori island, Mort Myre", 3508, 3357);
        b.item("quest_spider_venom_sample", "Spider venom sample", "vial", 0x5A8A3A, "A sample of venom from the spiders beneath Ver Sinhaza.");
        b.item("quest_ranis_head", "Ranis' head", "creature", 0x8A7A8A, "The severed head of Ranis Drakan. Grim.");
        b.item("ant_sulphuric_acid", "Sulphuric acid", "vial", 0xD0E040, "Daer Krand's acid. It would burn through an egg sac.");
        b.item("ant_spider_eggs", "Strange spider eggs", "orb", 0x6A3A8A, "Araxyte eggs from the nest north of Darkmeyer.");
        b.item("ant_hespori_bark", "Hespori bark", "log", 0x5A6A2A, "Bark from a wild hespori in Mort Myre.");
        // Hespori (A Night at the Theatre), wiki infobox (MonsterStatsTable): a harder Hespori than the Farming Guild's (level
        // 302, 360 hitpoints), with its spores (8 x2), green wave (14) and entangle (40+), the flowers that shield it at the
        // start and at 66% and 33% (the Farming Guild Hespori's script, BossesHooks), and no drop table. Its flowers have 10
        // hitpoints and, unlike the Farming Guild's, don't always die in one hit (wiki): the script lets them take the hit rolled.
        b.boss(BossDef.of("ant_hespori", "Hespori").level(302).stats(360, 1, 1, 120, 126, 150).speed(6)
                .defenceBonus(60, 20, 60, 80, 80).attackBonus(0, 150, 150)
                .attack(com.runecraft.osrs.content.bosses.ClassicBosses.HESPORI_SPORES)
                .attack(com.runecraft.osrs.content.bosses.ClassicBosses.HESPORI_WAVE)
                .attack(com.runecraft.osrs.content.bosses.ClassicBosses.HESPORI_ENTANGLE)
                .phase(66, null, 1.0, false).phase(33, null, 1.0, false)
                .model("hespori").size(1.6f, 1.8f, 1.8f).moveSpeed(0.0));
        b.quest("a_night_at_the_theatre", "A Night at the Theatre", Difficulty.MASTER, 2)
                .reqQuest("a_taste_of_hope")
                // Wiki: four antique lamps, each 20,000 XP in Attack, Strength, Defence, Ranged, Magic or Hitpoints (50+) (QuestLamps)
                .unlock("The Theatre of Blood entry mode; Ver Sinhaza's spider caves")
                .stage("I can start this quest by speaking to the Mysterious Stranger in Ver Sinhaza. (Requires A Taste of Hope.)")
                .talk("quest_mysterious_stranger_tob").say("Verzik Vitur has powers no vampyre should have. A mercenary saw purple spider eggs fall "
                        + "from her: a nylocas no one has recorded.", "Ranis Drakan knew her well. Bring me his corpse, and I will relive his memories.")
                // Wiki (Behind The Scenes): a vyrewatch's crypt key, Ranis's head cut off with a saw.
                .stage("I should bring the Mysterious Stranger Ranis Drakan's remains from the crypt beneath Ver Sinhaza (a vyrewatch "
                        + "carries the key: bring an Ivandis or blisterwood flail, and a knife).")
                .kill("vyrewatch_sentinel").stay().unless("quest_ranis_head").unlessFlag("ant_crypt_key").flag("ant_crypt_key")
                .say("* The vyrewatch drops a crypt key.")
                // OSRS cuts the head off with a saw; the mod has no saw yet, so a knife does it (approx)
                .reach("ant_ranis_crypt", 6).stay().needFlag("ant_crypt_key").unless("quest_ranis_head").need("knife").give("quest_ranis_head")
                .say("* The crypt key opens the gate to Ranis Drakan's coffin. You cut off his head. Grim work.")
                .missing("* The coffin is behind a locked gate, and you'd need a blade for what comes next.")
                .talk("quest_mysterious_stranger_tob").take("quest_ranis_head")
                .say("* You share Ranis's memory: he gives Verzik spider eggs from an araxyte nest north of Darkmeyer, for her nylocas breeding.",
                        "Araxyte eggs... Bring me some from that nest.")
                .missing("Ranis lies in the crypt just north of here. A vyrewatch carries the key.")
                // Wiki: the nest's sticky note, Daer Krand's acid for the egg sac (the araxytes need not be fought), then the Get
                // Into The Grove part: the Nature Spirit, the wild hespori and its bark.
                .stage("I should bring the Stranger araxyte eggs from the nest north-west of Slepe, then bark from a wild hespori.")
                .reach("ant_araxyte_nest", 8).stay().unlessFlag("ant_note").flag("ant_note")
                .say("* Behind a great egg sac lies a corpse with a sticky note: Daer Krand will have sulphuric acid to burn through the sac.")
                .talk("daer_krand").stay().needFlag("ant_note").unless("ant_sulphuric_acid", "ant_spider_eggs").unlessFlag("ant_eggs")
                .give("ant_sulphuric_acid")
                .say("My assistant... dead in that nest? Poor fool. Here, the acid he wanted. Use it well.")
                .reach("ant_araxyte_nest", 8).stay().needFlag("ant_note").unless("ant_spider_eggs").unlessFlag("ant_eggs")
                .take("ant_sulphuric_acid").give("ant_spider_eggs")
                .say("* The acid eats through the egg sac. You grab some strange spider eggs as araxytes pour out, and flee.")
                .missing("* A great egg sac. Something strong would burn through it: the note mentioned Daer Krand.")
                .talk("quest_mysterious_stranger_tob").stay().unlessFlag("ant_eggs").take("ant_spider_eggs").flag("ant_eggs")
                .say("* Ranis's memories: Verzik breeds new nylocas from the eggs, and Ranis suggests hespori bark to make them regenerate.",
                        "Hespori: Queen Efaritay's living weapons. Filliman Tarlock, the Nature Spirit, may know where one grows.")
                .missing("The araxyte nest is north-west of Slepe. Bring me eggs.")
                .talk("nature_spirit").stay().needFlag("ant_eggs").unlessFlag("ant_grove").need("ghostspeak_amulet").flag("ant_grove")
                .say("A hespori? They are rare now. Look to the north-east, on a small island in the swamp.")
                .missing("* You'd need a ghostspeak amulet to understand the Nature Spirit.")
                .reach("ant_hespori_island", 8).stay().needFlag("ant_grove").unlessFlag("ant_hespori").spawn("ant_hespori")
                .kill("ant_hespori").stay().needFlag("ant_grove").unlessFlag("ant_hespori").flag("ant_hespori")
                .say("* The wild hespori wilts.")
                .reach("ant_hespori_island", 8).stay().needFlag("ant_hespori").unless("ant_hespori_bark").unlessFlag("ant_bark").needTool("axe")
                .give("ant_hespori_bark").say("* You chop some bark from the dead hespori.")
                .missing("* You'd need an axe to take its bark.")
                .talk("quest_mysterious_stranger_tob").needFlag("ant_eggs").take("ant_hespori_bark").flag("ant_bark")
                .say("* Ranis's last memory: Verzik's 'nylocas to end all nylocas', the tiny Nylocas Queen. It bites her and dies.",
                        "Its bite gave her these powers, but something is missing. I must touch Verzik herself. Get me through her Theatre.")
                .missing("A wild hespori grows somewhere in Mort Myre. The Nature Spirit would know.")
                // saves from before carry the old venom sample instead of the eggs and bark
                .talk("quest_mysterious_stranger_tob").take("quest_spider_venom_sample")
                .say("This venom will do. Now get me through Verzik's Theatre.")
                // Wiki (The Curtain Opens): a whole Theatre of Blood run (Entry mode will do; any earlier completion counts);
                // the raid sets the flag (RaidInstance).
                .stage("I must get the Mysterious Stranger to Verzik: complete the Theatre of Blood (Entry mode will do) in Ver Sinhaza.")
                .talk("quest_mysterious_stranger_tob").needFlag(QuestBossRules.TOB_COMPLETED_FLAG)
                .say("* As Verzik turned into a bat and fled, the Stranger touched her. Now he wants to share what he saw.")
                .missing("The Theatre waits. Entry mode is kinder, and you may retry its bosses as often as you need. Clear all six rooms.")
                .stage("I should talk to the Mysterious Stranger about what he saw.")
                .talk("quest_mysterious_stranger_tob")
                .say("* A memory, not Verzik's but her doctor Vulcan Orvoros's: a Nylocas Queen grew inside her and burst out, killing her.",
                        "The Verzik you fought is the Nylocas Queen. My associates were never real: I serve a master. He would speak with you.",
                        "@Sugadinti: You have given us what we need against the nylocas. Reward my servant's friend.");
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
                .xp(Skill.SLAYER, 1000).reward("coins:5000") // wiki: 5,000 coins
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
