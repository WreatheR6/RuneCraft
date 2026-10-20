package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** Karamja quests: Jungle Potion, Shilo Village and Legends' Quest. */
final class KaramjaQuests {
    private KaramjaQuests() {
    }

    static void register(QuestBook b) {
        b.place("tai_bwo_wannai", "Tai Bwo Wannai", 2790, 3055, PlaceDef.Style.CAMP);
        b.spot("karamja_vines", "Karamja jungle vines", 2825, 3085);
        b.spot("karamja_rocks", "Karamja rocks", 2860, 3030);
        b.spot("karamja_marsh", "Karamja marsh", 2760, 3040);
        b.spot("karamja_cave", "Jungle cave", 2805, 3010);
        b.place("ah_za_rhoon", "Ah Za Rhoon", 2930, 2950, PlaceDef.Style.RUINS);
        b.place("rashiliyias_tomb", "Rashiliyia's tomb", 2905, 2965, PlaceDef.Style.RUINS);
        b.spot("legends_guild", "Legends' Guild", 2729, 3348);
        b.place("kharazi_jungle", "Kharazi Jungle", 2830, 2920, PlaceDef.Style.CAMP);
        b.place("kharazi_cave", "Shaman's cave", 2800, 2905, PlaceDef.Style.CAVE);
        junglePotion(b);
        shiloVillage(b);
        legendsQuest(b);
    }

    private static void junglePotion(QuestBook b) {
        b.npc("trufitus", "Trufitus", "tai_bwo_wannai", 0, 2, 0x8A5A3A, 0xE0D0A0, 0x8A6A3A, 0xE0C060);
        b.item("snake_weed", "Snake weed", "herb", 0x6A9A3A, "A jungle herb.");
        b.item("ardrigal", "Ardrigal", "herb", 0x8AA03A, "A jungle herb found in the marsh.");
        b.item("sito_foil", "Sito foil", "herb", 0xA09A3A, "A herb that grows in scorched earth.");
        b.item("volencia_moss", "Volencia moss", "herb", 0x5A8A5A, "A moss that grows on rocks.");
        b.item("rogues_purse", "Rogue's purse", "herb", 0x9A6A4A, "A fungus that grows in caves.");
        b.quest("jungle_potion", "Jungle Potion", Difficulty.NOVICE, 1)
                .reqQuest("DRUIDIC_RITUAL").req(Skill.HERBLORE, 3).xp(Skill.HERBLORE, 775)
                .stage("I can start this quest by speaking to Trufitus in Tai Bwo Wannai. (Requires Druidic Ritual, 3 Herblore.)")
                .talk("trufitus").say("I must commune with the gods to protect my village from the evil of Rashiliyia. I need five jungle herbs:",
                        "snake weed from the vines, ardrigal from the marsh, sito foil from scorched earth, volencia moss from the rocks and rogue's purse from a cave.")
                .stage("I need snake weed, ardrigal, sito foil, volencia moss and rogue's purse from around Karamja.")
                .reach("karamja_vines", 6).stay().unless("snake_weed").give("snake_weed").say("* You find some snake weed among the vines.")
                .reach("karamja_marsh", 6).stay().unless("ardrigal").give("ardrigal").say("* You find some ardrigal in the marsh.")
                .reach("tai_bwo_wannai", 12).stay().unless("sito_foil").give("sito_foil").say("* You find sito foil growing in the scorched earth.")
                .reach("karamja_rocks", 6).stay().unless("volencia_moss").give("volencia_moss").say("* You scrape volencia moss from the rocks.")
                .reach("karamja_cave", 6).stay().unless("rogues_purse").give("rogues_purse").say("* You find rogue's purse on the cave wall.")
                .talk("trufitus").take("snake_weed", "ardrigal", "sito_foil", "volencia_moss", "rogues_purse")
                .say("All five herbs! *Trufitus brews a potion and drinks it* ...The gods have spoken to me. Thank you, Bwana.")
                .missing("I still need all five herbs.");
    }

    private static void shiloVillage(QuestBook b) {
        b.npc("mosol_rei", "Mosol Rei", "SHILO_VILLAGE", 62, 8, // outside the east gate (OSRS 2883, 2951)
                0x7A4A2A, 0xC0A040, 0x6A4A1A, 0x3A2A1A);
        b.item("zadimus_corpse", "Zadimus corpse", "bones", 0xA09080, "The remains of the priest Zadimus.");
        b.item("sword_pommel", "Sword pommel", "gem", 0xC0A060, "An ivory sword pommel.");
        b.item("bone_key", "Bone key", "key", 0xE8E0C8, "A key carved from bone.");
        b.item("rashiliyia_remains", "Rashiliyia's remains", "skull", 0xD8D0C0, "The remains of the Undead Queen Rashiliyia.");
        b.boss(BossDef.of("nazastarool", "Nazastarool").level(91).stats(70, 85, 85, 70, 1, 1).speed(4)
                .defenceBonus(20, 20, 0, 20, 30)
                .attack(AttackDef.of("claw", AttackType.SLASH, 11, 2.5, 10))
                .phase(66, "Nazastarool rises again as a skeleton!", 1.1, false, AttackDef.of("bone_strike", AttackType.CRUSH, 12, 2.5, 10))
                .phase(33, "Nazastarool rises a final time as a ghost!", 1.2, false,
                        AttackDef.of("ghost_touch", AttackType.MAGIC, 14, 6, 10).special(Special.DRAIN_PRAYER, 3))
                .drops(Drop.always("bones")).skin(0x8AA07A, 0x4A5A3A, 0x3A4A2A, 0x2A3A1A).at("rashiliyias_tomb").arena(9, "crypt"));
        b.quest("shilo_village", "Shilo Village", Difficulty.INTERMEDIATE, 2)
                .reqQuest("jungle_potion").req(Skill.CRAFTING, 20).req(Skill.AGILITY, 32).xp(Skill.CRAFTING, 3875)
                .unlock("Access to Shilo Village")
                .stage("I can start this quest by speaking to Mosol Rei outside Shilo Village. (Requires Jungle Potion, 20 Crafting, 32 Agility.)")
                .talk("mosol_rei").say("Run! The undead of Rashiliyia are attacking my village! Trufitus in Tai Bwo Wannai may know how to stop her.")
                .stage("I should ask Trufitus about Rashiliyia.")
                .talk("trufitus").say("Rashiliyia, the Undead Queen... Her old enemy Zadimus is buried at Ah Za Rhoon. His remains may help us.")
                .stage("I should search the ruins of Ah Za Rhoon.")
                .reach("ah_za_rhoon", 8).give("zadimus_corpse", "sword_pommel").say("* Deep in the ruins you find the corpse of Zadimus and an ivory sword pommel.")
                .stage("I should take what I found to Trufitus.")
                .talk("trufitus").take("zadimus_corpse", "sword_pommel").give("bone_key")
                .say("Bury Zadimus in sacred ground... *His spirit appears and gives you a bone key.* 'The key opens Rashiliyia's tomb.'")
                .stage("I should enter Rashiliyia's tomb with the bone key and destroy her guardian, Nazastarool.")
                .kill("nazastarool").take("bone_key").give("rashiliyia_remains").say("* Nazastarool collapses for good. You take Rashiliyia's remains from her coffin.")
                .stage("I should lay Rashiliyia's remains to rest and tell Mosol Rei the village is safe.")
                .talk("mosol_rei").take("rashiliyia_remains").say("* You bury Rashiliyia's remains with the corpse of her son. The undead fall still.",
                        "The undead are gone! Shilo Village is safe. You are welcome here, Bwana.")
                .done("mosol_rei", "Welcome to Shilo Village.");
    }

    /**
     * The marked wall in the Viyeldi caves (wiki): soul, mind, earth and two law runes, in that order ("SMELL"), open it; the
     * cut gems then go on the carved rocks and the binding book lies behind.
     */
    static final QuestPuzzle LQ_MARKED_WALL = QuestPuzzle.builder("lq_marked_wall").shuffle().room("wall").variant(null)
            .prompt("* A marked wall bars the way. Ancient letters spell S, M, E, L, L. Runes might fit the marks, one after another.")
            .options("Soul rune", "Mind rune", "Earth rune", "Law rune", "Air rune", "Fire rune", "Water rune", "Body rune", "Chaos rune", "Nature rune")
            .solution(0, 1, 2, 3, 3)
            .failLines("* The rune crumbles to dust in the mark. The wall is unmoved; you'll have to start the word again.").build();

    private static void legendsQuest(QuestBook b) {
        b.npc("radimus_erkle", "Radimus Erkle", "legends_guild", 0, 0, 0xE8C8A8, 0xE8E8E8, 0x3A3A6A, 0xB0B0B0);
        b.npc("gujuo", "Gujuo", "kharazi_jungle", 2, 2, 0x6A4A2A, 0xC0A040, 0x5A3A1A, 0x2A1A0A);
        b.npc("ungadulu", "Ungadulu", "kharazi_cave", 2, 2, 0x7A4A2A, 0x8A2A2A, 0x5A1A1A, 0xE0D0A0);
        // quest-helper tiles: the sacred water pool and the fertile soil by it (the jungle forester is One Small Favour's; the mossy
        // rock is the dungeons module's Shaman Caves entrance, by the kharazi_cave place)
        b.npc("echned_zekin", "Echned Zekin", "kharazi_cave", -3, -3, 0xC8D8E0, 0x6A6A8A, 0x4A4A6A, 0xA0B0C0);
        b.spot("lq_water_pool", "Kharazi water pool", 2838, 2916);
        b.spot("lq_fertile_soil", "Fertile soil (Kharazi Jungle)", 2833, 2924);
        b.item("radimus_notes", "Radimus notes", "book", 0x6A5A3A, "Notes on the Kharazi Jungle, for mapping.");
        b.item("kharazi_map", "Kharazi jungle map", "note", 0x8AA05A, "A completed map of the Kharazi Jungle.");
        b.item("yommi_tree_seeds", "Yommi tree seeds", "seed", 0x8A6A2A, "Seeds of the sacred Yommi tree.");
        b.item("totem_pole", "Gilded totem", "log", 0xC0902A, "A carved Yommi totem, gilded by Gujuo's people.");
        b.item("papyrus", "Papyrus", "note", 0xE8E0B8, "Used for making notes.");
        b.item("bullroarer", "Bullroarer", "tool", 0x8A6A3A, "Swing it near a totem pole to call the Kharazi natives.");
        b.item("lq_golden_bowl", "Golden bowl", "pot_item", 0xE0C040, "A golden bowl, blessed by Gujuo.");
        b.item("lq_bowl_of_water", "Golden bowl (sacred water)", "pot_item", 0x80C0F0, "A blessed golden bowl full of sacred water.");
        b.item("binding_book", "Binding book", "book", 0x5A2A6A, "An ancient book of binding spells.");
        b.item("lq_crystal_piece", "Crystal piece", "gem", 0xC0E0F0, "A piece of a heart-shaped crystal.");
        b.item("heart_crystal", "Heart crystal", "gem", 0xE0A0C0, "Three crystal pieces fused into a heart.");
        b.item("dark_dagger", "Dark dagger", "dagger", 0x2A2A3A, "A dagger of dark metal. Echned Zekin wants Viyeldi dead with it.");
        b.item("lq_yommi_totem", "Yommi totem", "log", 0xA0602A, "A totem carved from the sacred Yommi tree.");
        b.boss(BossDef.of("nezikchened", "Nezikchened").level(187).stats(150, 155, 155, 140, 160, 1).speed(4)
                .defenceBonus(20, 20, 20, 30, 20)
                .attack(AttackDef.of("claws", AttackType.SLASH, 20, 3, 6))
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 18, 9, 4).particle("minecraft:flame"))
                .attack(AttackDef.of("prayer_drain", AttackType.MAGIC, 10, 9, 2).special(Special.DRAIN_PRAYER, 10)
                        .message("Nezikchened drains your prayer!"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("kharazi_cave").arena(11, "lava"));
        // The three undead guardians of the crystal pieces (DPS calculator).
        b.boss(BossDef.of("san_tojalon", "San Tojalon").level(106).stats(120, 86, 84, 86, 0, 0).speed(4).defenceBonus(18, 22, 20, -1, 20)
                .attack(AttackDef.of("stab", AttackType.STAB, 12, 2, 1)).drops(Drop.always("bones")).model("skeleton").size(0.7f, 2.0f, 1.0f));
        b.boss(BossDef.of("irvig_senay", "Irvig Senay").level(100).stats(125, 76, 74, 81, 0, 0).speed(4).defenceBonus(27, 31, 29, -1, 29)
                .attack(AttackDef.of("slash", AttackType.SLASH, 12, 2, 1)).drops(Drop.always("bones")).model("skeleton").size(0.7f, 2.0f, 1.0f));
        b.boss(BossDef.of("ranalph_devere", "Ranalph Devere").level(92).stats(130, 66, 67, 66, 0, 0).speed(4).defenceBonus(38, 40, 36, 0, 38)
                .attack(AttackDef.of("slash", AttackType.SLASH, 13, 2, 1)).drops(Drop.always("bones")).model("skeleton").size(0.7f, 2.0f, 1.0f));
        b.quest("legends_quest", "Legends' Quest", Difficulty.MASTER, 4)
                .qp(107).req(Skill.AGILITY, 50).req(Skill.CRAFTING, 50).req(Skill.HERBLORE, 45).req(Skill.MAGIC, 56)
                .req(Skill.MINING, 52).req(Skill.PRAYER, 42).req(Skill.SMITHING, 50).req(Skill.STRENGTH, 50)
                .req(Skill.THIEVING, 50).req(Skill.WOODCUTTING, 50)
                .reqQuest("family_crest", "heroes_quest", "shilo_village", "underground_pass", "waterfall_quest")
                .xp(Skill.ATTACK, 7650).xp(Skill.DEFENCE, 7650).xp(Skill.STRENGTH, 7650).xp(Skill.HITPOINTS, 7650)
                .reward("cape_of_legends")
                .unlock("Access to the Legends' Guild")
                .stage("I can start this quest by speaking to Radimus Erkle in the Legends' Guild. (Requires 107 quest points and many skills.)")
                .talk("radimus_erkle").give("radimus_notes", "papyrus:3", "charcoal:3") // approx: charcoal is bought in Shilo in OSRS
                .say("To join the Legends you must map the Kharazi Jungle and bring us a gift from its people.",
                        "Take my notes and some papyrus from the table. You'll find machetes in the cupboards.")
                // Wiki: each of the three sections (west, middle, east) is mapped on a papyrus with charcoal, cutting in with an
                // axe and a machete.
                .stage("I should explore the Kharazi Jungle in southern Karamja and complete Radimus's map.")
                // more papyrus from Radimus's table (and for saves from before the papyrus)
                .talk("radimus_erkle").stay().unless("papyrus", "kharazi_map").give("papyrus:3", "charcoal:3")
                .say("Out of papyrus? Take some more from the table.")
                .reach("kharazi_jungle", 12).stay().unlessFlag("lq_map_west").need("radimus_notes", "machete").needTool("axe")
                .take("papyrus", "charcoal").flag("lq_map_west").say("* You hack your way into the western jungle and map it on a sheet of papyrus.")
                .missing("* You'll need Radimus's notes, papyrus and charcoal, an axe and a machete to map the jungle.")
                .reach("kharazi_jungle", 12).stay().needFlag("lq_map_west").unlessFlag("lq_map_middle").need("radimus_notes", "machete").needTool("axe")
                .take("papyrus", "charcoal").flag("lq_map_middle").say("* You map the middle of the jungle.")
                .reach("kharazi_jungle", 12).needFlag("lq_map_middle").need("machete").needTool("axe").take("radimus_notes", "papyrus", "charcoal")
                .give("kharazi_map").say("* You map the eastern jungle and complete Radimus's map. A young jungle native watches you from the trees.")
                .stage("I should speak to Gujuo, the jungle native.")
                // Wiki: a jungle forester south of Shilo copies the map for a bullroarer, which calls Gujuo at a totem pole.
                .talk("jungle_forester").stay().need("kharazi_map").unless("bullroarer").give("bullroarer")
                .say("A map of the Kharazi? Let me make a copy... Here, take this bullroarer. Swing it near a totem pole and the natives will come.")
                .talk("gujuo").need("bullroarer").say("My people's shaman, Ungadulu, is possessed by a demon in the caves below. Free him and we will trust you.",
                        "The cave is behind the mossy rock to the north-west.")
                .missing("* The jungle is silent. Swinging a bullroarer by a totem pole might call someone.")
                .stage("I should enter the shaman's cave and defeat the demon Nezikchened.")
                // Wiki: a wall of magic fire surrounds Ungadulu; sacred water in a blessed golden bowl puts it out, and the binding
                // book from the Viyeldi caves (lockpicked gate, three boulders, a 50 Strength gate, the SMELL wall) frees him.
                .reach("kharazi_cave", 10).stay().unlessFlag("lq_fire_wall").reqSkill(Skill.AGILITY, 50).flag("lq_fire_wall")
                .say("* You crawl in behind the mossy rock. Ungadulu stands within a wall of magic fire. Only pure water could douse it.")
                .talk("gujuo").stay().needFlag("lq_fire_wall").unless("lq_golden_bowl", "lq_bowl_of_water").unlessFlag("lq_bound")
                .reqSkill(Skill.PRAYER, 42).take("gold_bar:2").need("hammer").give("lq_golden_bowl")
                .say("Pure water? The sacred pool, west of the centre of the jungle. Only a blessed vessel can hold it. "
                        + "*You smith a golden bowl from the gold bars and Gujuo blesses it.*")
                .missing("Bring two gold bars and a hammer for a golden bowl, and I will bless it. (Requires 42 Prayer.)")
                .reach("lq_water_pool", 5).stay().need("machete").unlessFlag("lq_bound").take("lq_golden_bowl").give("lq_bowl_of_water")
                .say("* You cut a reed with your machete and fill the blessed bowl with the pool's sacred water.")
                .reach("kharazi_cave", 10).stay().needFlag("lq_fire_wall").unlessFlag("lq_viyeldi").need("lockpick").needTool("pickaxe")
                .reqSkill(Skill.STRENGTH, 50).flag("lq_viyeldi")
                .say("* Behind a bookcase a crevice leads into the Viyeldi caves. You pick the gate's lock, smash three boulders and force the next gate.")
                .missing("* A locked gate and boulders: you'll need a lockpick, a pickaxe and 50 Strength.")
                .puzzle("kharazi_cave", LQ_MARKED_WALL).stay().needFlag("lq_viyeldi").unless("binding_book").unlessFlag("lq_bound")
                .take("soul_rune", "mind_rune", "earth_rune", "law_rune:2", "opal", "jade", "red_topaz", "sapphire", "emerald", "ruby", "diamond")
                .give("binding_book")
                .say("* The wall slides open. You set the seven cut gems into the carved rocks and the last door opens: a binding book lies inside.")
                .talk("ungadulu").stay().need("binding_book").unlessFlag("lq_bound").take("lq_bowl_of_water").give("lq_golden_bowl").flag("lq_bound")
                .spawn("nezikchened")
                .say("* You pour the sacred water on the flames and read the binding book at Ungadulu. The demon is torn from him!",
                        "@Nezikchened: Fool! Your god cannot save you here. *Your prayer drains away.*")
                .missing("* The flames around Ungadulu roar. Sacred water and a binding book could free him.")
                .reach("kharazi_cave", 10).stay().needFlag("lq_bound").spawn("nezikchened")
                .kill("nezikchened").needFlag("lq_bound").say("* The demon Nezikchened is banished! Ungadulu staggers free of its control.")
                .stage("I should speak to the freed shaman Ungadulu.")
                .talk("ungadulu").give("yommi_tree_seeds").say("You saved me! My people need a sacred totem pole. Plant these Yommi seeds with pure water, then carve the tree.")
                // Wiki: the pool has dried up. The spring lies deep in the Viyeldi caves: a bravery potion, a charged orb for the
                // gate, a rope down the winch, three undead guardians' crystal pieces fused into a heart, and Echned Zekin, who is
                // Nezikchened in disguise (the good path: Ungadulu's Holy Force reveals him).
                .stage("I need to grow the Yommi tree (seeds + vial of water) and carve a totem pole with an axe.")
                .talk("gujuo").stay().unlessFlag("lq_spring").flag("lq_spring")
                .say("The water pool has dried up? Its spring lies deep in the Viyeldi caves. You'll need courage to go down: a bravery potion.")
                .reach("kharazi_cave", 10).stay().needFlag("lq_spring").unlessFlag("lq_winch").take("ardrigal", "snake_weed", "vial_of_water", "unpowered_orb")
                .need("rope").flag("lq_winch").spawn("san_tojalon")
                .say("* You drink a bravery potion of ardrigal and snake weed, charge an orb to open the north gate and climb down the winch.",
                        "* At the bottom, three undead guardians rise one after another: San Tojalon first!")
                .missing("* A deep shaft. You need a bravery potion (ardrigal, snake weed, a vial of water), an unpowered orb to charge and a rope.")
                .kill("san_tojalon").stay().needFlag("lq_winch").unless("heart_crystal").unlessFlag("lq_heart", "lq_irvig").give("lq_crystal_piece").flag("lq_irvig")
                .spawn("irvig_senay").say("* San Tojalon falls and drops a crystal piece. Irvig Senay attacks!")
                .kill("irvig_senay").stay().needFlag("lq_irvig").unless("heart_crystal").unlessFlag("lq_heart", "lq_ranalph").give("lq_crystal_piece").flag("lq_ranalph")
                .spawn("ranalph_devere").say("* Irvig Senay falls and drops a crystal piece. Ranalph Devere attacks!")
                .kill("ranalph_devere").stay().needFlag("lq_ranalph").unless("heart_crystal").unlessFlag("lq_heart", "lq_crystals").give("lq_crystal_piece").flag("lq_crystals")
                .say("* Ranalph Devere falls and drops the last crystal piece.")
                .reach("kharazi_cave", 10).stay().needFlag("lq_crystals").unlessFlag("lq_heart").take("lq_crystal_piece:3").give("heart_crystal").flag("lq_heart")
                .say("* You melt the three crystal pieces together in the furnace: a heart crystal. It opens the recess by the barrier.")
                .talk("echned_zekin").stay().needFlag("lq_heart").unless("dark_dagger").unlessFlag("lq_holy_force", "lq_zekin").give("dark_dagger").flag("lq_zekin")
                .say("I am Echned Zekin. The water is mine to give, if you kill Viyeldi for me with this dark dagger.")
                .talk("ungadulu").stay().needFlag("lq_zekin").unlessFlag("lq_holy_force").take("dark_dagger").flag("lq_holy_force")
                .say("This dagger reeks of Nezikchened! Echned Zekin is no spirit... Cast the Holy Force upon him and his true form will show.")
                .talk("echned_zekin").stay().needFlag("lq_holy_force").unlessFlag("lq_revealed").flag("lq_revealed").spawn("nezikchened")
                .say("* You cast Holy Force on Echned Zekin. The disguise burns away: Nezikchened!")
                .reach("kharazi_cave", 10).stay().needFlag("lq_revealed").unlessFlag("lq_spring_freed").spawn("nezikchened")
                .kill("nezikchened").stay().needFlag("lq_revealed").unlessFlag("lq_spring_freed").flag("lq_spring_freed")
                .say("* Nezikchened is banished again. You push the boulder aside and the sacred spring flows into the pool once more.")
                .reach("lq_water_pool", 5).stay().needFlag("lq_spring_freed").unless("lq_bowl_of_water", "lq_yommi_totem").unlessFlag("lq_totem_placed")
                .take("lq_golden_bowl").give("lq_bowl_of_water").say("* You refill the blessed bowl with sacred water.")
                .reach("lq_fertile_soil", 5).stay().needFlag("lq_spring_freed").unless("lq_yommi_totem", "totem_pole").unlessFlag("lq_totem_placed")
                .take("yommi_tree_seeds", "lq_bowl_of_water").needTool("axe:rune").give("lq_yommi_totem", "lq_golden_bowl")
                .say("* You germinate the seeds in sacred water and plant them in the fertile soil. Watered, the Yommi grows into a great tree.",
                        "* You fell it with your rune axe and carve it into a totem before it can rot.")
                .missing("* The seeds need sacred water, and only a rune or dragon axe can fell a Yommi tree.")
                .reach("kharazi_jungle", 12).stay().needFlag("lq_spring_freed").unlessFlag("lq_totem_placed").take("lq_yommi_totem").flag("lq_totem_placed")
                .spawn("nezikchened")
                .say("* You set the Yommi totem in place of an old totem pole. Nezikchened appears for the last time!")
                .reach("kharazi_jungle", 12).stay().needFlag("lq_totem_placed").unlessFlag("lq_nez_final").spawn("nezikchened")
                .kill("nezikchened").stay().needFlag("lq_totem_placed").unlessFlag("lq_nez_final").flag("lq_nez_final")
                .say("* Nezikchened is destroyed for good.")
                .talk("gujuo").needFlag("lq_nez_final").give("totem_pole")
                .say("The Yommi totem stands! My people have gilded it for you to take to your guild, with our thanks.")
                .missing("Plant the Yommi seeds with sacred water and carve the tree into a totem. Only a rune axe will do.")
                .stage("I should take the map and the totem pole to Radimus Erkle.")
                .talk("radimus_erkle").take("kharazi_map", "totem_pole").say("A complete map and a gift from the Kharazi people! Welcome to the Legends' Guild. Wear this cape with pride.")
                .done("radimus_erkle", "Welcome, legend.");
    }
}
