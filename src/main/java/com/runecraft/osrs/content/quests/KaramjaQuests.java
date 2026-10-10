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
        b.npc("mosol_rei", "Mosol Rei", "SHILO_VILLAGE", 0, 9, 0x7A4A2A, 0xC0A040, 0x6A4A1A, 0x3A2A1A);
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
        b.quest("shilo_village", "Shilo Village", Difficulty.EXPERIENCED, 2)
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

    private static void legendsQuest(QuestBook b) {
        b.npc("radimus_erkle", "Radimus Erkle", "legends_guild", 0, 0, 0xE8C8A8, 0xE8E8E8, 0x3A3A6A, 0xB0B0B0);
        b.npc("gujuo", "Gujuo", "kharazi_jungle", 2, 2, 0x6A4A2A, 0xC0A040, 0x5A3A1A, 0x2A1A0A);
        b.npc("ungadulu", "Ungadulu", "kharazi_cave", 2, 2, 0x7A4A2A, 0x8A2A2A, 0x5A1A1A, 0xE0D0A0);
        b.item("radimus_notes", "Radimus notes", "book", 0x6A5A3A, "Notes on the Kharazi Jungle, for mapping.");
        b.item("kharazi_map", "Kharazi jungle map", "note", 0x8AA05A, "A completed map of the Kharazi Jungle.");
        b.item("yommi_tree_seeds", "Yommi tree seeds", "seed", 0x8A6A2A, "Seeds of the sacred Yommi tree.");
        b.item("totem_pole", "Totem pole", "log", 0xA0602A, "A carved, blessed totem pole.");
        b.boss(BossDef.of("nezikchened", "Nezikchened").level(187).stats(150, 155, 155, 140, 160, 1).speed(4)
                .defenceBonus(20, 20, 20, 30, 20)
                .attack(AttackDef.of("claws", AttackType.SLASH, 20, 3, 6))
                .attack(AttackDef.of("fire_blast", AttackType.MAGIC, 18, 9, 4).particle("minecraft:flame"))
                .attack(AttackDef.of("prayer_drain", AttackType.MAGIC, 10, 9, 2).special(Special.DRAIN_PRAYER, 10)
                        .message("Nezikchened drains your prayer!"))
                .drops(Drop.always("bones")).size(1.0f, 2.4f, 1.0f).at("kharazi_cave").arena(11, "lava"));
        b.quest("legends_quest", "Legends' Quest", Difficulty.MASTER, 4)
                .qp(107).req(Skill.AGILITY, 50).req(Skill.CRAFTING, 50).req(Skill.HERBLORE, 45).req(Skill.MAGIC, 56)
                .req(Skill.MINING, 52).req(Skill.PRAYER, 42).req(Skill.SMITHING, 50).req(Skill.STRENGTH, 50)
                .req(Skill.THIEVING, 50).req(Skill.WOODCUTTING, 50)
                .reqQuest("family_crest", "heroes_quest", "shilo_village", "underground_pass", "waterfall_quest")
                .xp(Skill.ATTACK, 7650).xp(Skill.DEFENCE, 7650).xp(Skill.STRENGTH, 7650).xp(Skill.HITPOINTS, 7650)
                .reward("cape_of_legends")
                .unlock("Access to the Legends' Guild")
                .stage("I can start this quest by speaking to Radimus Erkle in the Legends' Guild. (Requires 107 quest points and many skills.)")
                .talk("radimus_erkle").give("radimus_notes").say("To join the Legends you must map the Kharazi Jungle and bring us a gift from its people.")
                .stage("I should explore the Kharazi Jungle in southern Karamja and complete Radimus's map.")
                .reach("kharazi_jungle", 12).take("radimus_notes").give("kharazi_map")
                .say("* You chart the jungle. A young jungle native watches you from the trees.")
                .stage("I should speak to Gujuo, the jungle native.")
                .talk("gujuo").say("My people's shaman, Ungadulu, is possessed by a demon in the caves below. Free him and we will trust you.")
                .stage("I should enter the shaman's cave and defeat the demon Nezikchened.")
                .kill("nezikchened").say("* The demon Nezikchened is banished! Ungadulu staggers free of its control.")
                .stage("I should speak to the freed shaman Ungadulu.")
                .talk("ungadulu").give("yommi_tree_seeds").say("You saved me! My people need a sacred totem pole. Plant these Yommi seeds with pure water, then carve the tree.")
                .stage("I need to grow the Yommi tree (seeds + vial of water) and carve a totem pole with an axe.")
                .talk("gujuo").take("yommi_tree_seeds", "vial_of_water").need("rune_axe").give("totem_pole")
                .say("* The Yommi tree grows in an instant. You fell it with your axe and carve a totem pole; Gujuo blesses it.")
                .missing("You'll need the Yommi seeds, a vial of pure water and a strong axe (rune).")
                .stage("I should take the map and the totem pole to Radimus Erkle.")
                .talk("radimus_erkle").take("kharazi_map", "totem_pole").say("A complete map and a gift from the Kharazi people! Welcome to the Legends' Guild. Wear this cape with pride.")
                .done("radimus_erkle", "Welcome, legend.");
    }
}
