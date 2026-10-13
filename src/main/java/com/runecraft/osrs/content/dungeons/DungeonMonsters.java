package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Dungeon creatures that no other module defines. Everything else in the dungeons (dragons, demons,
 * giants, kalphites, slayer monsters, TzHaar, dwarves...) reuses existing monsters. These only appear
 * through the dungeon camps (no natural spawns). Stats follow the OSRS wiki where known; values
 * marked approx. are estimates.
 */
public final class DungeonMonsters {
    private DungeonMonsters() {
    }

    static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng).underground();
    }

    static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    public static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        // Taverley Dungeon / Edgeville Dungeon
        list.add(m("poison_spider", "Poison spider", 64, 64, 50, 50, 64, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 6).special(Special.POISON, 6)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.of("red_spiders_eggs", 1, 1, 8)) // approx.
                .slayer(1, "spiders").skin(0x3A6A2A, 0x2A4A1A, 0x1A2A10, 0xA0D040).size(1.4f, 0.8f, 1.0f));
        list.add(m("chaos_druid", "Chaos druid", 13, 20, 8, 7, 9, 12, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 2)).attack(AttackDef.of("bind", AttackType.MAGIC, 2, 8, 4).special(Special.FREEZE, 5))
                .defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.herbs(2), Drop.of("law_rune", 2, 2, 25), Drop.of("mind_rune", 3, 3, 25),
                        Drop.of("air_rune", 9, 9, 30), Drop.of("body_rune", 2, 2, 30), Drop.of("nature_rune", 3, 3, 40),
                        Drop.of("limpwurt_root", 1, 1, 50))
                .skin(0xC8A080, 0x3A2A4A, 0x2A1A3A, 0x6A4A8A));
        // Varrock Sewers / Stronghold of Security
        list.add(m("giant_rat", "Giant rat", 6, 10, 4, 4, 4, 1, 1).speed(4).attack(melee(AttackType.STAB, 2))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones")) // OSRS: also raw rat meat
                .skin(0x6A5A4A, 0x5A4A3A, 0x4A3A2A, 0xD08080).size(0.9f, 0.7f, 1.0f));
        list.add(m("flesh_crawler", "Flesh Crawler", 41, 25, 30, 25, 25, 1, 1).speed(4).attack(melee(AttackType.STAB, 4))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(4), Drop.of("coins", 10, 60, 3), Drop.of("iron_ore", 1, 1, 30)) // approx.
                .skin(0xA06A5A, 0x804A3A, 0x603A2A, 0xD0A090).size(1.0f, 0.8f, 1.0f));
        list.add(m("catablepon", "Catablepon", 64, 70, 50, 50, 50, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 7))
                .attack(AttackDef.of("weaken", AttackType.MAGIC, 7, 6, 3).special(Special.DRAIN_STAT, 0))
                .defenceBonus(10, 10, 10, 0, 10).drops(Drop.always("bones"), Drop.herbs(6), Drop.of("coins", 10, 120, 3)) // approx.
                .skin(0x5A4A3A, 0x3A2A1A, 0x2A1A10, 0x8A6A3A).size(1.3f, 1.4f, 1.0f));
        list.add(m("giant_spider", "Giant spider", 50, 50, 41, 43, 40, 1, 1).speed(4).attack(melee(AttackType.STAB, 5))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.of("red_spiders_eggs", 1, 1, 16)) // approx.
                .slayer(1, "spiders").skin(0x3A2A1A, 0x2A1A10, 0x1A1008, 0xA02020).size(1.5f, 0.9f, 1.0f));
        // Brimhaven Dungeon
        list.add(m("wild_dog", "Wild dog", 63, 62, 52, 52, 48, 1, 1).speed(4).attack(melee(AttackType.STAB, 8))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"))
                .skin(0x8A6A4A, 0x6A4A2A, 0x4A3A2A, 0x2A2A2A).size(0.8f, 0.9f, 1.0f));
        // Lumbridge cellar caves / Dorgesh-Kaan
        list.add(m("cave_goblin", "Cave goblin", 3, 10, 2, 2, 2, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 1)).passive()
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.of("coins", 1, 10, 3))
                .skin(0xC8D8C0, 0x6A5A8A, 0x4A3A5A, 0xF0F0E0).size(0.6f, 1.3f, 1.0f));
        list.add(m("cave_goblin_guard", "Cave goblin guard", 24, 26, 22, 22, 24, 1, 1).speed(4).attack(melee(AttackType.STAB, 3))
                .passive().defenceBonus(12, 12, 12, 0, 12).drops(Drop.always("bones"), Drop.of("coins", 5, 30, 3),
                        Drop.of("iron_dagger", 1, 1, 30))
                .skin(0xC8D8C0, 0x5A5A6A, 0x3A3A4A, 0xF0F0E0).size(0.6f, 1.4f, 1.0f));
        // The Abyss
        list.add(m("abyssal_leech", "Abyssal leech", 41, 35, 32, 50, 18, 1, 1).speed(3).attack(melee(AttackType.STAB, 6))
                .defenceBonus(5, 5, 5, 5, 5).drops(Drop.of("pure_essence", 1, 15, 3), Drop.of("cosmic_rune", 2, 10, 10),
                        Drop.of("blood_rune", 1, 5, 25), Drop.of("death_rune", 1, 5, 20))
                .skin(0x3A1A3A, 0x2A102A, 0x2A102A, 0xC03030).size(0.9f, 0.8f, 1.0f));
        list.add(m("abyssal_guardian", "Abyssal guardian", 59, 55, 30, 85, 40, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 9))
                .defenceBonus(20, 20, 20, 20, 20).drops(Drop.of("pure_essence", 1, 25, 3), Drop.of("nature_rune", 2, 12, 10),
                        Drop.of("law_rune", 2, 10, 15), Drop.of("blood_rune", 1, 7, 25))
                .skin(0x4A1A2A, 0x3A101A, 0x2A0A10, 0xF0A020).size(1.0f, 1.8f, 1.1f));
        list.add(m("abyssal_walker", "Abyssal walker", 81, 95, 110, 54, 70, 1, 1).speed(5).attack(melee(AttackType.CRUSH, 11))
                .defenceBonus(20, 20, 20, 20, 20).drops(Drop.of("pure_essence", 5, 40, 3), Drop.of("death_rune", 2, 12, 10),
                        Drop.of("soul_rune", 1, 6, 25), Drop.of("blood_rune", 2, 10, 20))
                .skin(0x2A1A3A, 0x1A102A, 0x1A0A20, 0xA020C0).size(1.2f, 2.2f, 1.1f));
        // Kruk's Dungeon (Monkey Madness II): stats from the wiki DPS calculator (cache ids 7118 and 7119). Both share one
        // 20-slot drop table, rolled here as independent 1/x chances. Small ninja monkey bones are plain bones, and the
        // 1-dose prayer potion and 2-dose antipoison are full potions at a quarter/half the rate (same doses per kill).
        list.add(m("maniacal_monkey", "Maniacal monkey", 140, 65, 200, 175, 10, 1, 1).speed(4)
                .attack(melee(AttackType.SLASH, 18)).defenceBonus(0, 0, 0, 0, 0).drops(maniacalDrops())
                .slayer(1, "monkeys").model("maniacal_monkey").skin(0x5A3A2A, 0x3A2418, 0x3A2418, 0xC0A060).size(0.6f, 0.9f, 1.4f));
        list.add(m("maniacal_monkey_archer", "Maniacal Monkey Archer", 132, 60, 120, 80, 80, 1, 200).speed(4)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 24, 8, 10)).defenceBonus(0, 0, 0, 0, 0).drops(maniacalDrops())
                .model("maniacal_monkey").skin(0x4A3020, 0x2A1A10, 0x2A1A10, 0x8AA040).size(0.6f, 0.9f, 1.4f));
        waterbirth(list);
        apeAtoll(list);
        return list;
    }

    /**
     * Waterbirth Island Dungeon (and Jormungand's Prison): the dagannoth of the wiki's "Dagannoth (Waterbirth Island)"
     * page - level 88 (ranged, 3-tick) and level 90 (stab) - their spawn and fledgelings, giant rock crabs, and the
     * prison's level 92 dagannoth. Stats from the wiki infoboxes and the DPS calculator data; drops are the wiki
     * tables' core items (Fremennik armour pieces, hides and crafting materials of other modules are left out, approx).
     */
    private static void waterbirth(List<MonsterDef> list) {
        list.add(m("dagannoth_lvl88", "Dagannoth", 88, 85, 10, 10, 75, 1, 100).speed(3)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 20, 4, 10)).defenceBonus(10, 10, 90, 200, 150)
                .drops(Drop.always("bones"), Drop.of("fremennik_shield", 1, 1, 43), Drop.of("fremennik_helm", 1, 1, 43),
                        Drop.of("fremennik_blade", 1, 1, 43), Drop.of("steel_arrow", 1, 30, 13), Drop.of("coins", 1, 140, 13),
                        Drop.herbs(16), Drop.rareSeeds(128), Drop.gems(128))
                .slayer(1, "dagannoth").model("dagannoth").skin(0x4A6A50, 0x2E4A36, 0x2E4A36, 0x9AA878)
                .size(1.0f, 2.2f, 1.0f));
        list.add(m("dagannoth_lvl90", "Dagannoth", 90, 95, 76, 78, 65, 1, 1).speed(4).attackBonus(50, 0, 0)
                .attack(melee(AttackType.STAB, 15)).defenceBonus(50, 50, 50, 50, 50)
                .drops(Drop.always("bones"), Drop.of("water_rune", 10, 10, 13), Drop.of("nature_rune", 1, 10, 13),
                        Drop.of("law_rune", 1, 10, 13), Drop.of("blood_rune", 1, 10, 64), Drop.of("steel_axe", 1, 1, 13),
                        Drop.of("raw_lobster", 1, 1, 26), Drop.of("fremennik_shield", 1, 1, 43), Drop.of("fremennik_helm", 1, 1, 43),
                        Drop.of("fremennik_blade", 1, 1, 64), Drop.herbs(32), Drop.rareSeeds(128), Drop.gems(128))
                .slayer(1, "dagannoth").model("dagannoth").skin(0x3A4A3A, 0x22322A, 0x22322A, 0x7A8A60)
                .size(1.0f, 2.2f, 1.0f));
        // Hatched from the eggs in the entrance hall (approx: they wait beside the eggs, which respawn after 5 minutes).
        list.add(m("dagannoth_spawn", "Dagannoth spawn", 42, 35, 50, 35, 25, 1, 1).speed(4).attack(melee(AttackType.STAB, 4))
                .defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.of("water_rune", 3, 3, 13), Drop.of("raw_tuna", 1, 1, 13),
                        Drop.of("raw_herring", 1, 1, 26), Drop.of("raw_sardine", 1, 1, 13), Drop.of("coins", 16, 16, 13),
                        Drop.of("coins", 25, 25, 13), Drop.of("fishing_bait", 3, 3, 13), Drop.gems(128))
                .slayer(1, "dagannoth").model("dagannoth").skin(0x6A8A60, 0x4A6A44, 0x4A6A44, 0xB0C090)
                .size(0.6f, 1.2f, 0.6f));
        list.add(m("dagannoth_fledgeling", "Dagannoth fledgeling", 70, 100, 50, 50, 50, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 6)).defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"))
                .slayer(1, "dagannoth").model("dagannoth").skin(0x5A7A56, 0x3A5A3E, 0x3A5A3E, 0xA0B484)
                .size(0.8f, 1.6f, 0.8f));
        list.add(m("giant_rock_crab", "Giant Rock Crab", 137, 180, 50, 80, 200, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 9))
                .defenceBonus(225, 200, 175, -10, 250)
                .drops(Drop.always("bones"), Drop.of("steel_platebody", 1, 1, 26), Drop.of("black_platebody", 1, 1, 26),
                        Drop.of("black_warhammer", 1, 1, 26), Drop.of("adamant_longsword", 1, 1, 26), Drop.of("mithril_axe", 1, 1, 13),
                        Drop.of("water_rune", 1, 40, 13), Drop.of("earth_rune", 1, 40, 13), Drop.of("coins", 1, 140, 13),
                        Drop.of("raw_lobster", 1, 1, 13), Drop.of("tinderbox", 1, 1, 13), Drop.herbs(16), Drop.rareSeeds(128),
                        Drop.gems(128))
                .model("rock_crab").skin(0x6A6A6A, 0x4A4A4A, 0x4A4A4A, 0xA04030).size(1.6f, 1.2f, 1.8f));
        // Jormungand's Prison's larger dagannoth (DPS calculator, cache id 973).
        list.add(m("dagannoth_lvl92", "Dagannoth", 92, 120, 68, 70, 71, 1, 1).speed(4).attack(melee(AttackType.STAB, 8))
                .defenceBonus(0, 0, 0, 50, 50)
                .drops(Drop.always("bones"), Drop.of("raw_shark", 1, 1, 32), Drop.of("water_rune", 10, 30, 6), Drop.herbs(16),
                        Drop.gems(128))
                .slayer(1, "dagannoth").model("dagannoth").skin(0x4A6A50, 0x2E4A36, 0x2E4A36, 0x9AA878)
                .size(1.4f, 2.6f, 1.2f));
    }

    /**
     * The Ape Atoll Dungeon and the Temple of Marimbo Dungeon: the level 82 and 129 monkey zombies (the level 98 one is
     * the islands module's) and the level 142 skeletons, from the DPS calculator data. Their bones (zombie monkey bones,
     * Ape Atoll bones) are plain bones here (approx).
     */
    private static void apeAtoll(List<MonsterDef> list) {
        list.add(m("monkey_zombie_lvl82", "Monkey Zombie", 82, 60, 100, 60, 60, 60, 1).speed(4).attackBonus(15, 0, 0)
                .attack(melee(AttackType.CRUSH, 7)).defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"))
                .slayer(1, "monkeys").attributes("undead").model("monkey_zombie").skin(0x6A7A5A, 0x4A5A3A, 0x4A5A3A, 0x9AA080)
                .size(0.6f, 0.8f, 1.4f));
        list.add(m("monkey_zombie_lvl129", "Monkey Zombie", 129, 90, 150, 110, 90, 90, 1).speed(4).attackBonus(35, 0, 0)
                .attack(melee(AttackType.CRUSH, 12)).defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("big_bones"))
                .slayer(1, "monkeys").attributes("undead").model("monkey_zombie").skin(0x5A6A4A, 0x3A4A2A, 0x3A4A2A, 0x8A9070)
                .size(0.7f, 1.0f, 1.8f));
        list.add(m("skeleton_lvl142", "Skeleton", 142, 77, 180, 90, 110, 110, 1).speed(4).attackBonus(50, 0, 0)
                .attack(melee(AttackType.SLASH, 10)).defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"))
                .slayer(1, "skeletons").attributes("undead").model("skeleton").skin(0xD8D0B0, 0xB8B090, 0xB8B090, 0x302820));
    }

    private static Drop[] maniacalDrops() {
        return new Drop[]{Drop.always("bones"), // approx: small ninja monkey bones
                Drop.of("steel_scimitar", 1, 1, 10), Drop.of("maple_shortbow", 1, 1, 20), Drop.of("oak_plank", 1, 1, 7),
                Drop.of("grimy_guam_leaf", 1, 1, 10), Drop.of("banana", 1, 1, 10), Drop.of("adamant_arrow", 5, 5, 20),
                Drop.of("antipoison", 1, 1, 20), Drop.of("bass", 2, 2, 5), Drop.of("prayer_potion", 1, 1, 40),
                Drop.of("rope", 1, 1, 20)};
    }
}
