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
                .slayer(1, "monkeys").model("monkey").skin(0x5A3A2A, 0x3A2418, 0x3A2418, 0xC0A060).size(0.6f, 0.9f, 1.4f));
        list.add(m("maniacal_monkey_archer", "Maniacal Monkey Archer", 132, 60, 120, 80, 80, 1, 200).speed(4)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 24, 8, 10)).defenceBonus(0, 0, 0, 0, 0).drops(maniacalDrops())
                .model("monkey").skin(0x4A3020, 0x2A1A10, 0x2A1A10, 0x8AA040).size(0.6f, 0.9f, 1.4f));
        return list;
    }

    private static Drop[] maniacalDrops() {
        return new Drop[]{Drop.always("bones"), // approx: small ninja monkey bones
                Drop.of("steel_scimitar", 1, 1, 10), Drop.of("maple_shortbow", 1, 1, 20), Drop.of("oak_plank", 1, 1, 7),
                Drop.of("grimy_guam_leaf", 1, 1, 10), Drop.of("banana", 1, 1, 10), Drop.of("adamant_arrow", 5, 5, 20),
                Drop.of("antipoison", 1, 1, 20), Drop.of("bass", 2, 2, 5), Drop.of("prayer_potion", 1, 1, 40),
                Drop.of("rope", 1, 1, 20)};
    }
}
