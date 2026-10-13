package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Island monsters that no other module adds. They don't spawn naturally across a whole kingdom;
 * {@link IslandsPlaces#camps()} keeps them stocked at their real haunts (ammonite crabs on Fossil
 * Island's coast, wyverns in the Wyvern Cave, monkey guards in Marim, suqahs on Lunar Isle, yaks on
 * Neitiznot, wallasalki and rock lobsters on Waterbirth Island, jungle horrors on Mos Le'Harmless,
 * jogres in Kharazi Jungle and Feldip Hills, ogresses under Corsair Cove, mudskippers at Mudskipper
 * Point). Spitting and taloned wyverns, dagannoth, cave horrors, ice trolls, zygomites, dragons,
 * demons and giants come from the slayer module and the core game and are reused.
 *
 * <p>Stats are OSRS values where known; values marked "approx." are estimates.
 */
public final class IslandsMonsters {
    private IslandsMonsters() {
    }

    static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng);
    }

    static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    static AttackDef magic(int max, String particle) {
        return AttackDef.of("magic", AttackType.MAGIC, max, 10, 10).particle(particle);
    }

    static AttackDef ranged(int max) {
        return AttackDef.of("ranged", AttackType.RANGED, max, 10, 10).particle("minecraft:crit");
    }

    static AttackDef icyBreath() {
        return AttackDef.of("icy breath", AttackType.MAGIC, 50, 4, 2).special(Special.DRAGONFIRE, 0)
                .particle("minecraft:snowflake").message("The wyvern breathes icy breath!");
    }

    static Drop clue(String tier, int rarity) {
        return Drop.of("clue_scroll_" + tier, 1, 1, rarity);
    }

    public static List<MonsterDef> all() {
        return List.of(
                // Fossil Island.
                m("ammonite_crab", "Ammonite crab", 25, 100, 1, 1, 1, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 1))
                        .drops(Drop.of("numulite", 3, 12, 8), Drop.of("unidentified_small_fossil", 1, 1, 40),
                                Drop.of("unidentified_medium_fossil", 1, 1, 60), Drop.of("unidentified_large_fossil", 1, 1, 90),
                                Drop.of("unidentified_rare_fossil", 1, 1, 400), clue("easy", 128))
                        .skin(0x8A7A5A, 0xC0A070, 0xC0A070, 0xE0D0A0).size(1.0f, 0.8f, 1.0f),
                m("long_tailed_wyvern", "Long-tailed wyvern", 152, 200, 140, 155, 120, 80, 1).speed(4).attack(melee(AttackType.CRUSH, 18))
                        .attack(AttackDef.of("tail whip", AttackType.CRUSH, 22, 4, 4).special(Special.KNOCKBACK, 1)
                                .message("The wyvern lashes you with its tail!"))
                        .attack(icyBreath()).defenceBonus(90, 90, 90, 60, 90)
                        .drops(Drop.always("wyvern_bones"), Drop.of("wyvern_visage", 1, 1, 10000), Drop.of("granite_legs", 1, 1, 512),
                                Drop.of("granite_boots", 1, 1, 1024), Drop.of("granite_longsword", 1, 1, 1024), // approx. rates
                                Drop.of("rune_battleaxe", 1, 1, 128), Drop.herbs(12), Drop.gems(32),
                                clue("hard", 256),
                                clue("elite", 1200))
                        .slayer(66, "fossil_island_wyverns").model("taloned_wyvern").size(1.4f, 2.4f, 1.05f).underground(),
                // Ancient wyverns need level 82 Slayer (approx. stats).
                m("ancient_wyvern", "Ancient wyvern", 210, 300, 160, 175, 160, 140, 1).speed(4).attack(melee(AttackType.SLASH, 24))
                        .attack(magic(26, "minecraft:snowflake").message("The ancient wyvern blasts you with frost magic."))
                        .attack(icyBreath()).defenceBonus(120, 120, 120, 80, 100)
                        .drops(Drop.always("wyvern_bones"), Drop.of("wyvern_visage", 1, 1, 10000), Drop.of("granite_legs", 1, 1, 512),
                                Drop.of("granite_boots", 1, 1, 1024), Drop.of("granite_longsword", 1, 1, 1024), // approx. rates
                                Drop.of("rune_2h_sword", 1, 1, 128), Drop.of("dragon_platelegs", 1, 1, 1024),
                                Drop.herbs(10),
                                Drop.gems(24), clue("elite", 350))
                        .slayer(82, "fossil_island_wyverns").model("spitting_wyvern").size(1.4f, 2.4f, 1.2f).underground(),
                // Ape Atoll.
                m("monkey_guard", "Monkey Guard", 167, 130, 130, 130, 130, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 19))
                        .defenceBonus(60, 60, 60, 20, 60)
                        .drops(Drop.always("big_bones"), Drop.of("coins", 20, 300, 4), Drop.of("rune_dagger", 1, 1, 128),
                                Drop.herbs(16), Drop.gems(64), clue("hard", 256))
                        .slayer(1, "monkeys").model("monkey_guard").size(0.8f, 1.0f, 2.2f),
                m("monkey_archer", "Monkey Archer", 86, 50, 1, 1, 70, 1, 100).speed(5).attack(ranged(8))
                        .drops(Drop.always("bones"), Drop.of("mithril_arrow", 5, 15, 8), Drop.of("adamant_arrow", 3, 8, 32),
                                Drop.of("coins", 10, 100, 4))
                        .slayer(1, "monkeys").model("monkey").size(0.6f, 0.8f, 1.5f),
                m("monkey_zombie", "Monkey Zombie", 98, 60, 80, 80, 80, 1, 1).speed(4).attack(melee(AttackType.SLASH, 11))
                        .drops(Drop.always("bones"), Drop.of("coins", 10, 120, 4), Drop.herbs(32), clue("medium", 128))
                        .slayer(1, "monkeys").model("monkey_zombie").size(0.6f, 0.8f, 1.6f).skin(0x6A7A5A, 0x4A5A3A, 0x4A5A3A, 0x9AA080),
                // Lunar Isle.
                m("suqah", "Suqah", 111, 105, 107, 107, 107, 115, 1).speed(4).attack(melee(AttackType.SLASH, 13))
                        .attack(magic(14, "minecraft:enchant").message("The suqah casts a spell at you."))
                        .drops(Drop.always("big_bones"), Drop.always("suqah_hide"), Drop.always("suqah_tooth"),
                                Drop.of("astral_rune", 5, 15, 16), Drop.herbs(16), clue("hard", 128))
                        .skin(0x8A7AA0, 0x6A5A8A, 0x6A5A8A, 0xE0D0F0).size(0.9f, 2.4f, 1.0f),
                // Neitiznot.
                m("yak", "Yak", 22, 50, 20, 22, 18, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 3)).passive()
                        .drops(Drop.always("bones"), Drop.always("yak_hide"), Drop.always("raw_yak_meat"), Drop.of("yak_hair", 1, 1, 2))
                        .skin(0x5A4A3A, 0x4A3A2A, 0x4A3A2A, 0xC0B090).size(1.2f, 1.4f, 1.0f),
                // Waterbirth Island.
                m("wallasalki", "Wallasalki", 98, 120, 1, 1, 120, 120, 1).speed(4)
                        .attack(magic(12, "minecraft:splash").message("The wallasalki sprays water at you."))
                        .defenceBonus(20, 20, 20, 80, 20)
                        .drops(Drop.always("bones"), Drop.of("water_rune", 15, 45, 8), Drop.of("raw_shark", 1, 1, 64),
                                Drop.herbs(16), clue("hard", 256))
                        .slayer(1, "dagannoth").skin(0x3A6A8A, 0x2A4A6A, 0x2A4A6A, 0x8AB0C0).size(0.8f, 2.0f, 1.0f),
                m("rock_lobster", "Rock lobster", 127, 150, 100, 100, 120, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 12))
                        .defenceBonus(100, 100, 50, 10, 100)
                        .drops(Drop.always("bones"), Drop.of("raw_lobster", 1, 3, 4), Drop.of("coins", 30, 300, 4), Drop.herbs(24),
                                clue("hard", 256))
                        .slayer(1, "dagannoth").skin(0x6A2A2A, 0x8A3A2A, 0x8A3A2A, 0xC06A3A).size(1.0f, 1.0f, 1.0f),
                // Mos Le'Harmless.
                m("jungle_horror", "Jungle horror", 70, 45, 70, 70, 70, 1, 1).speed(4).attack(melee(AttackType.SLASH, 8))
                        .drops(Drop.always("bones"), Drop.of("coins", 10, 150, 4), Drop.herbs(16), Drop.seeds(24), clue("medium", 128))
                        .skin(0x4A6A3A, 0x3A4A2A, 0x3A4A2A, 0xA0A040).size(0.8f, 2.0f, 1.0f),
                // Kharazi Jungle and Feldip Hills.
                m("jogre", "Jogre", 53, 60, 43, 43, 43, 1, 1).speed(5).attack(melee(AttackType.CRUSH, 8))
                        .drops(Drop.of("jogre_champion_scroll", 1, 1, 5000), Drop.always("big_bones"), Drop.of("coins", 5, 100, 4), Drop.of("iron_ore", 1, 1, 16), Drop.herbs(32),
                                clue("medium", 128))
                        .slayer(1, "ogres").skin(0x7A6A3A, 0x5A4A2A, 0x5A4A2A, 0x6A5A3A).size(0.9f, 2.4f, 1.0f),
                // Corsair Cove Dungeon.
                m("ogress_warrior", "Ogress Warrior", 82, 82, 82, 82, 82, 1, 1).speed(5).attack(melee(AttackType.CRUSH, 12))
                        .drops(Drop.always("big_bones"), Drop.of("coins", 20, 400, 4), Drop.of("rune_full_helm", 1, 1, 256),
                                Drop.herbs(24), Drop.gems(64), clue("medium", 128))
                        .slayer(1, "ogres").skin(0x8A9A5A, 0x5A3A2A, 0x5A3A2A, 0x6A4A2A).size(0.9f, 2.4f, 1.05f).underground(),
                m("ogress_shaman", "Ogress Shaman", 82, 82, 1, 1, 82, 82, 1).speed(5)
                        .attack(magic(12, "minecraft:witch").message("The ogress shaman casts a spell at you."))
                        .drops(Drop.always("big_bones"), Drop.of("coins", 20, 400, 4), Drop.of("nature_rune", 5, 15, 16),
                                Drop.of("law_rune", 2, 6, 32), Drop.herbs(24), Drop.gems(64), clue("medium", 128))
                        .slayer(1, "ogres").skin(0x8A9A5A, 0x3A5A3A, 0x3A5A3A, 0xA0A060).size(0.9f, 2.4f, 1.05f).underground(),
                // Mudskipper Point (approx. stats).
                m("mudskipper", "Mudskipper", 31, 28, 25, 28, 25, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 4))
                        .drops(Drop.always("bones"), Drop.of("raw_shrimps", 1, 1, 8), Drop.of("coins", 3, 30, 8))
                        .skin(0x6A6A4A, 0x4A4A3A, 0x4A4A3A, 0x8A8A5A).size(0.9f, 0.7f, 1.0f));
    }
}
