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
        // 20-slot drop table, rolled here as independent 1/x chances. MonsterDropsTable swaps the bones for small ninja
        // monkey bones; the prayer potion drops with 1 dose and the antipoison with 2 (see Drop#doses).
        list.add(m("maniacal_monkey", "Maniacal monkey", 140, 65, 200, 175, 10, 1, 1).speed(4)
                .attack(melee(AttackType.SLASH, 18)).defenceBonus(0, 0, 0, 0, 0).drops(maniacalDrops())
                .slayer(1, "monkeys").model("maniacal_monkey").skin(0x5A3A2A, 0x3A2418, 0x3A2418, 0xC0A060).size(0.6f, 0.9f, 1.4f));
        list.add(m("maniacal_monkey_archer", "Maniacal Monkey Archer", 132, 60, 120, 80, 80, 1, 200).speed(4)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 24, 8, 10)).defenceBonus(0, 0, 0, 0, 0).drops(maniacalDrops())
                .model("maniacal_monkey").skin(0x4A3020, 0x2A1A10, 0x2A1A10, 0x8AA040).size(0.6f, 0.9f, 1.4f));
        waterbirth(list);
        apeAtoll(list);
        cacheDungeons(list);
        remainingDungeons(list);
        // Barrows tunnels (not on any Slayer task): wiki DPS-calculator stats, no drops.
        list.add(m("bloodworm", "Bloodworm", 52, 45, 80, 20, 35, 1, 1).speed(4).attack(melee(AttackType.STAB, 3))
                .defenceBonus(0, 0, 0, 0, 0).model("abyssal_leech").skin(0x8A1A1A, 0x6A1010, 0x6A1010, 0xC03030)
                .size(0.8f, 0.6f, 0.8f));
        warpedCreatures(list);
        lavaStrykewyrm(list);
        return list;
    }

    /**
     * The Charred Dungeon's lava strykewyrm (Slayer 62, wyrm tasks): stats from the wiki's DPS calculator data, drops
     * from its wiki page (x/128). Strykewyrm bones, cannonballs, chilli potatoes, super attack/defence potions and the
     * dragon metal sheet are not in the game and are left out; the gem-tipped bolt tips are folded into diamond ones.
     */
    static void lavaStrykewyrm(List<MonsterDef> list) {
        list.add(m("lava_strykewyrm", "Lava Strykewyrm", 116, 150, 90, 90, 50, 50, 110).speed(5)
                .attack(melee(AttackType.CRUSH, 10)).attack(AttackDef.of("ranged", AttackType.RANGED, 12, 8, 10))
                .defenceBonus(30, 60, 70, 40, 120).rangedDefence(120, 120).attackBonus(30, 0, 40).weakness("water", 50)
                .drops(Drop.always("ashes"), Drop.herbs(64), Drop.of("lava_battlestaff", 1, 1, 26), Drop.of("adamant_platebody", 1, 1, 32),
                        Drop.of("rune_longsword", 1, 1, 64), Drop.of("rune_mace", 1, 1, 64), Drop.of("rune_med_helm", 1, 1, 64),
                        Drop.of("rune_pickaxe", 1, 1, 64), Drop.of("lava_rune", 50, 100, 21), Drop.of("death_rune", 5, 10, 21),
                        Drop.of("chaos_rune", 10, 20, 21), Drop.of("fire_rune", 50, 100, 21), Drop.of("lava_rune", 200, 200, 26),
                        Drop.of("rune_dart", 20, 30, 26), Drop.of("rune_knife", 20, 30, 26), Drop.of("rune_arrow", 20, 30, 26),
                        Drop.of("diamond_bolt_tips", 5, 10, 26), Drop.of("coins", 100, 200, 21), Drop.of("fire_orb", 1, 1, 26),
                        Drop.of("uncut_diamond", 1, 1, 26), Drop.of("dragon_arrowtips", 2, 6, 32), Drop.of("coal", 4, 6, 32),
                        Drop.of("adamantite_ore", 2, 4, 32), Drop.of("chaos_talisman", 1, 1, 32), Drop.of("fire_talisman", 1, 1, 32),
                        Drop.of("runite_ore", 1, 2, 43))
                .slayer(62, "wyrms").model("wyrm").size(1.4f, 1.6f, 1.2f));
    }

    /**
     * The Poison Waste Dungeon's warped creatures (Slayer 56, warped creatures tasks): the warped tortoise and the level
     * 96 warped terrorbird (the level 138 one is The Path of Glouphrie's boss, {@code warped_terrorbird}). Stats from
     * the wiki's DPS calculator data (weirdgloop/osrs-dps-calc), drops from the wiki pages (2/64 = 1 in 32); the warped
     * sceptre, tortoise shells, feathers and terrorbird wings are not in the game yet and are left out.
     */
    static void warpedCreatures(List<MonsterDef> list) {
        list.add(m("warped_tortoise", "Warped Tortoise", 121, 200, 78, 106, 78, 50, 1).speed(4).attack(melee(AttackType.CRUSH, 12))
                .defenceBonus(50, 50, 0, 0, 60).rangedDefence(60, 20).attackBonus(0, 0, 0).weakness("earth", 20)
                .drops(Drop.always("big_bones"), Drop.gems(21), Drop.of("adamant_axe", 1, 1, 32), Drop.of("adamant_platebody", 1, 1, 32),
                        Drop.of("rune_pickaxe", 1, 1, 64), Drop.of("rune_kiteshield", 1, 1, 64), Drop.of("rune_warhammer", 1, 1, 64),
                        Drop.of("earth_rune", 80, 100, 11), Drop.of("mud_rune", 30, 50, 13), Drop.of("death_rune", 15, 20, 21),
                        Drop.of("coins", 600, 800, 6), Drop.of("adamantite_ore", 3, 5, 32), Drop.of("swamp_tar", 40, 60, 11),
                        Drop.of("coal", 6, 12, 21), Drop.of("weapon_poison", 1, 1, 21), Drop.of("cabbage", 20, 40, 11),
                        Drop.of("pineapple", 1, 1, 21), Drop.of("tangled_toads_legs", 2, 3, 21), Drop.of("long_bone", 1, 1, 400),
                        Drop.of("curved_bone", 1, 1, 5012))
                .slayer(56, "warped_creatures").passive().model("rock_crab").skin(0x5A6A3A, 0x3A4A2A, 0x3A4A2A, 0x9AC040)
                .size(2.4f, 1.6f, 1.8f));
        list.add(m("warped_terrorbird_lvl96", "Warped Terrorbird", 96, 150, 60, 90, 20, 60, 40).speed(4)
                .attack(melee(AttackType.CRUSH, 10)).attack(AttackDef.of("ranged", AttackType.RANGED, 10, 8, 10))
                .defenceBonus(50, 50, 0, 0, 62).rangedDefence(20, 62).attackBonus(0, 0, 240)
                .drops(Drop.always("bones"), Drop.gems(21), Drop.of("adamant_warhammer", 1, 1, 32), Drop.of("adamant_platebody", 1, 1, 32),
                        Drop.of("rune_battleaxe", 1, 1, 64), Drop.of("rune_kiteshield", 1, 1, 64), Drop.of("rune_warhammer", 1, 1, 64),
                        Drop.of("air_rune", 80, 120, 11), Drop.of("earth_rune", 80, 100, 11), Drop.of("death_rune", 15, 20, 21),
                        Drop.of("law_rune", 15, 20, 21), Drop.of("soul_rune", 10, 15, 21), Drop.of("coins", 600, 800, 8),
                        Drop.of("adamantite_ore", 3, 5, 32), Drop.of("weapon_poison", 1, 1, 21), Drop.of("diamond_bolt_tips", 24, 32, 21),
                        Drop.of("raw_shark", 3, 7, 13), Drop.of("swamp_tar", 40, 60, 13), Drop.of("chocolate_bomb", 2, 3, 21))
                .slayer(56, "warped_creatures").model("warped_terrorbird").size(1.4f, 2.4f, 1.0f));
    }

    /**
     * Waterbirth Island Dungeon (and Jormungand's Prison): the dagannoth of the wiki's "Dagannoth (Waterbirth Island)"
     * page - level 88 (ranged, 3-tick) and level 90 (stab) - their spawn and fledgelings, giant rock crabs, and the
     * prison's level 92 dagannoth. Stats from the wiki infoboxes and the DPS calculator data; drops are the wiki
     * tables' core items; MonsterDropsTable adds the rest (Fremennik clothing, spined gloves/boots, Neitiznot helms,
     * oyster pearls; the hides and crafting materials are still left out, approx).
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
        // The Kings' lair's spinolyps (DPS calculator: level 76, 100 hitpoints, 100 Ranged); their spines drain prayer.
        // OSRS: they stay submerged in their pools; drops approx (wiki: fish, runes and seaweed).
        list.add(m("spinolyp", "Spinolyp", 76, 100, 10, 10, 10, 1, 100).speed(4)
                .attack(AttackDef.of("spines", AttackType.RANGED, 10, 8, 10).special(Special.DRAIN_PRAYER, 1))
                .attack(AttackDef.of("water", AttackType.MAGIC, 10, 8, 5))
                .defenceBonus(100, 100, 100, 50, 50)
                .drops(Drop.of("water_rune", 5, 15, 8), Drop.of("raw_shark", 1, 1, 32), Drop.of("seaweed", 1, 1, 8), // approx
                        Drop.of("coins", 10, 60, 8))
                .skin(0x3A5A6A, 0x2A4A5A, 0x2A4A5A, 0x9AC0C0).size(0.9f, 0.9f, 1.0f));
        list.add(m("dagannoth_fledgeling","Dagannoth fledgeling", 70, 100, 50, 50, 50, 1, 1).speed(4)
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
     * the islands module's) and the level 142 skeletons, from the DPS calculator data. MonsterDropsTable swaps the
     * zombies' bones for small/large zombie monkey bones; the skeletons' Ape Atoll "Bones" are plain bones (same name and
     * Prayer XP in OSRS).
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

    /**
     * The monsters of the Shadow Dungeon, the Sophanem Dungeon and the Forthos Dungeon ({@link CacheDungeons}). Stats are
     * the DPS calculator's (weirdgloop/osrs-dps-calc monsters.json) and the wiki's infoboxes; drops follow the wiki's
     * drop tables (the scarab mage shares the locust riders' table here, approx).
     */
    private static void cacheDungeons(List<MonsterDef> list) {
        // Shadow Dungeon (Desert Treasure I).
        list.add(m("shadow_hound", "Shadow Hound", 63, 62, 53, 54, 54, 1, 1).speed(4).attack(melee(AttackType.STAB, 6))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"))
                .slayer(1, "dogs").model("hellhound").skin(0x1A1A22, 0x101016, 0x101016, 0x6A2A8A).size(0.9f, 1.0f, 1.3f));
        list.add(m("giant_skeleton", "Giant skeleton", 80, 70, 70, 70, 70, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 8))
                .defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("big_bones"), Drop.of("steel_med_helm", 1, 1, 21), Drop.of("steel_sword", 1, 1, 32),
                        Drop.of("steel_axe", 1, 1, 64), Drop.of("iron_arrow", 24, 24, 64), Drop.of("air_rune", 30, 30, 43),
                        Drop.of("water_rune", 18, 18, 43), Drop.of("chaos_rune", 8, 8, 43), Drop.of("law_rune", 4, 4, 64),
                        Drop.of("coins", 10, 20, 3), Drop.of("iron_bar", 1, 1, 26), Drop.herbs(32))
                .slayer(1, "skeletons").attributes("undead").model("skeleton").skin(0xD8D0B0, 0xB8B090, 0xB8B090, 0x302820)
                .size(0.9f, 2.4f, 1.0f));
        // Sophanem Dungeon (Contact!): the locust lancer (106) and ranger (98), and the scarab mage (93).
        list.add(m("locust_rider", "Locust rider", 106, 90, 100, 90, 90, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 15))
                .defenceBonus(90, 90, 40, 0, 0).drops(scabariteDrops())
                .slayer(1, "scabarites").attributes("kalphite").model("kalphite")
                .skin(0x8A6A3A, 0x5A4A2A, 0x3A2A1A, 0xD0B060).size(1.6f, 2.0f, 1.8f));
        list.add(m("locust_rider_ranger", "Locust rider", 98, 90, 105, 90, 50, 1, 90).speed(6).attackBonus(0, 0, 80)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 22, 8, 10)).defenceBonus(50, 90, 50, 34, 66)
                .drops(scabariteDrops()).slayer(1, "scabarites").attributes("kalphite").model("kalphite")
                .skin(0x7A5A3A, 0x4A3A2A, 0x3A2A1A, 0xC0A050).size(1.6f, 2.0f, 1.8f));
        list.add(m("scarab_mage", "Scarab mage", 93, 50, 90, 90, 90, 70, 1).speed(5).attackBonus(0, 70, 0) // speed approx.
                .attack(AttackDef.of("magic", AttackType.MAGIC, 17, 8, 10)).defenceBonus(40, 90, 90, 34, 0)
                .drops(scabariteDrops()).slayer(1, "scabarites").attributes("kalphite").model("toa_scarab")
                .skin(0x3A5A6A, 0x2A3A4A, 0x1A2A3A, 0x60C0D0).size(0.9f, 1.4f, 1.0f));
        // Forthos Dungeon.
        list.add(m("temple_spider", "Temple Spider", 75, 70, 120, 50, 10, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 6))
                .defenceBonus(20, 30, 10, 25, 20).drops(Drop.of("red_spiders_eggs", 1, 1, 8), Drop.herbs(16)) // drops approx.
                .slayer(1, "spiders").skin(0x6A3A2A, 0x4A2A1A, 0x2A1A10, 0xE0A040).size(1.4f, 0.8f, 1.0f));
        list.add(m("undead_druid", "Undead Druid", 105, 140, 60, 50, 60, 115, 1).speed(4).attackBonus(50, 100, 0)
                .attack(melee(AttackType.CRUSH, 6)).attack(AttackDef.of("magic", AttackType.MAGIC, 22, 8, 10))
                .defenceBonus(40, 30, 80, 140, 40)
                .drops(Drop.always("bones"), Drop.herbs(4), Drop.of("nature_rune", 3, 9, 16), Drop.of("law_rune", 2, 6, 25),
                        Drop.of("air_rune", 20, 40, 16)) // drops approx.
                .attributes("undead").model("skeleton").skin(0x8A9A7A, 0x3A5A2A, 0x2A3A1A, 0x80F060));
    }

    /**
     * The monsters of the later cache dungeons ({@link CacheDungeons}): the Giants' Den, the Karamja Volcano dungeon,
     * the Deep Wilderness Dungeon, the Ice Troll Caves, the Ourania Cave and the small creatures of the Shadow Dungeon,
     * the Sophanem Dungeon and the Wilderness Slayer Cave. Stats are the DPS calculator's (weirdgloop/osrs-dps-calc
     * monsters.json); drops are the wiki's main lines only (approx).
     */
    private static void remainingDungeons(List<MonsterDef> list) {
        list.add(m("rat", "Rat", 1, 2, 1, 1, 1, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 0)).attackBonus(-47, 0, 0)
                .defenceBonus(-42, -42, -42, -42, -42).drops(Drop.always("bones")).slayer(1, "rats").attributes("rat")
                .model("giant_rat").skin(0x6A5A4A, 0x5A4A3A, 0x4A3A2A, 0xD08080).size(0.6f, 0.4f, 0.6f));
        list.add(m("bat", "Bat", 6, 8, 5, 5, 5, 1, 1).speed(4).attack(melee(AttackType.STAB, 1))
                .defenceBonus(2, 2, 5, 2, 0).drops(Drop.always("bat_bones")).slayer(1, "bats")
                .model("giant_bat").skin(0x3A3028, 0x2A2018, 0x2A2018, 0xA02020).size(0.6f, 0.5f, 0.6f));
        // Sophanem Dungeon (approx stats: the wiki's mummy is not in the DPS calculator data).
        list.add(m("mummy", "Mummy", 103, 80, 85, 85, 85, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 11)) // approx
                .defenceBonus(20, 20, 20, 0, 20).drops(Drop.always("bones")).attributes("undead") // approx
                .skin(0xC8B890, 0xB0A078, 0xB0A078, 0x2A2A1A));
        // Giants' Den (Zeah): its own level 104 and 109 fire giants.
        list.add(m("fire_giant_lvl104", "Fire giant", 104, 130, 65, 65, 120, 1, 1).speed(5).attack(melee(AttackType.SLASH, 10))
                .attackBonus(50, 0, 0).defenceBonus(20, 10, 10, 50, -10).drops(Drop.always("big_bones"), Drop.of("fire_rune", 15, 15, 16))
                .slayer(1, "fire_giants").attributes("fiery").model("fire_giant").skin(0xC86A3A, 0xA04A2A, 0x7A3A1A, 0xF0A030));
        list.add(m("fire_giant_lvl109", "Fire giant", 109, 150, 90, 80, 65, 1, 1).speed(5).attack(melee(AttackType.SLASH, 13))
                .attackBonus(29, 0, 0).defenceBonus(0, 3, 2, 0, 0).drops(Drop.always("big_bones"), Drop.of("fire_rune", 15, 15, 16))
                .slayer(1, "fire_giants").attributes("fiery").model("fire_giant").skin(0xC86A3A, 0xA04A2A, 0x7A3A1A, 0xF0A030));
        // Karamja Volcano dungeon.
        list.add(m("deadly_red_spider", "Deadly red spider", 34, 35, 30, 25, 30, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 3).special(Special.POISON, 4)).defenceBonus(15, 16, 7, 12, 16)
                .slayer(1, "spiders").model("spider").skin(0x8A2A1A, 0x6A1A10, 0x4A1008, 0xE04020).size(1.2f, 0.7f, 1.0f));
        list.add(m("skeleton_lvl45", "Skeleton", 45, 59, 32, 35, 36, 1, 1).speed(4).attack(melee(AttackType.SLASH, 5))
                .attackBonus(15, 0, 0).defenceBonus(9, 11, -2, 1, 4).drops(Drop.always("bones"))
                .slayer(1, "skeletons").attributes("undead").model("skeleton").skin(0xD8D0B0, 0xB8B090, 0xB8B090, 0x302820));
        // Deep Wilderness Dungeon.
        list.add(m("shadow_spider", "Shadow spider", 52, 55, 44, 42, 44, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 5).special(Special.DRAIN_PRAYER, 0)) // OSRS: drains Prayer by a quarter (approx)
                .defenceBonus(20, 15, 10, 15, 15).slayer(1, "spiders").model("giant_spider")
                .skin(0x2A2A3A, 0x1A1A2A, 0x10101A, 0x6A4A8A).size(1.3f, 0.8f, 1.0f));
        // The Shayzien Crypts' zombies and skeletons (osrs-dps-calc): one melee, ranged and magic version of each, level 132,
        // 54 Hitpoints, 160 Attack, 62 Defence, 180 in their style, max hit 19, attack speed 5. Drops: bones (approx).
        for (String kind : new String[]{"zombie", "skeleton"}) {
            String name = (kind.equals("zombie") ? "Zombie" : "Skeleton") + " (Shayzien Crypts)";
            int skin = kind.equals("zombie") ? 0x6A8A5A : 0xD8D0B8;
            list.add(m("shayzien_crypt_" + kind + "_melee", name, 132, 54, 160, 180, 62, 1, 1).speed(5)
                    .attack(melee(AttackType.SLASH, 19)).defenceBonus(25, 20, 25, 10, 30).drops(Drop.always("bones"))
                    .model(kind).skin(skin, 0x4A4A3A, 0x3A3A2A, skin));
            list.add(m("shayzien_crypt_" + kind + "_ranged", name, 132, 54, 160, 1, 62, 1, 180).speed(5)
                    .attack(AttackDef.of("ranged", AttackType.RANGED, 19, 8, 10)).defenceBonus(15, 10, 15, 30, 20)
                    .drops(Drop.always("bones")).model(kind).skin(skin, 0x3A4A2A, 0x3A3A2A, skin));
            list.add(m("shayzien_crypt_" + kind + "_magic", name, 132, 54, 160, 1, 62, 180, 1).speed(5)
                    .attack(AttackDef.of("magic", AttackType.MAGIC, 19, 8, 10)).defenceBonus(35, 30, 35, 20, 10)
                    .drops(Drop.always("bones")).model(kind).skin(skin, 0x3A2A4A, 0x3A3A2A, skin));
        }
        list.add(m("chaos_dwarf", "Chaos dwarf", 48, 61, 38, 42, 28, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 6))
                .attackBonus(13, 0, 0).defenceBonus(40, 34, 25, 10, 35).drops(Drop.always("bones"), Drop.of("coal", 1, 1, 8))
                .slayer(1, "dwarves").model("dwarf").skin(0xC89070, 0x6A1A1A, 0x3A1A1A, 0xE04020));
        list.add(m("monk_of_zamorak", "Monk of Zamorak", 23, 16, 15, 13, 18, 30, 1).speed(4).attack(melee(AttackType.CRUSH, 2))
                .attack(AttackDef.of("magic", AttackType.MAGIC, 3, 8, 4)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones")).model("monk").skin(0xC8A080, 0x8A1A1A, 0x6A1010, 0x2A2A2A));
        // Ice Troll Caves and the Jatizso mine (the level 82 males are the slayer module's ice troll).
        list.add(m("ice_troll_runt", "Ice troll runt", 74, 60, 60, 70, 70, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 15))
                .attackBonus(60, 0, 0).defenceBonus(30, 60, 30, 0, 0).drops(Drop.always("big_bones"), Drop.herbs(14))
                .slayer(1, "trolls").model("ice_troll").skin(0xA0C0D8, 0x7A9AB8, 0x5A7A98, 0xE0F0FF).size(0.9f, 2.0f, 0.8f));
        list.add(m("ice_troll_female", "Ice troll female", 82, 80, 80, 80, 40, 1, 1).speed(4)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 6, 8, 10)).attackBonus(60, 0, 0).defenceBonus(30, 60, 30, 0, 0)
                .drops(Drop.always("big_bones"), Drop.herbs(14)).slayer(1, "trolls").model("ice_troll")
                .skin(0xA8C8E0, 0x8AAAC8, 0x6A8AA8, 0xE0F0FF).size(1.0f, 2.4f, 0.95f));
        list.add(m("ice_troll_grunt", "Ice troll grunt", 100, 80, 100, 100, 60, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 21))
                .attackBonus(60, 0, 0).defenceBonus(30, 60, 30, 0, 0).drops(Drop.always("big_bones"), Drop.herbs(14))
                .slayer(1, "trolls").model("ice_troll").skin(0x90B0C8, 0x6A8AA8, 0x4A6A88, 0xE0F0FF).size(1.1f, 2.8f, 1.1f));
        // Ourania Cave (Zamorak's followers and the cave lizards on the way to the altar).
        // Shaman Caves (Legends' Quest): death wings (wiki: level 83, 80 hitpoints, 70 attack/strength/defence, stab, max
        // hit 8 approx, aggressive, spectral; a bat and a ghost for Slayer; only a rare beginner clue drop).
        list.add(m("death_wing", "Death wing", 83, 80, 70, 70, 70, 1, 1).speed(4).attack(melee(AttackType.STAB, 8))
                .defenceBonus(0, 0, 0, 0, 0).attributes("spectral").slayer(1, "bats").model("giant_bat")
                .skin(0x1A1420, 0x2A2030, 0x100C14, 0x6A3A8A).size(1.2f, 1.0f, 1.3f).flying().moveSpeed(0.3));
        // Clock Tower dungeon: dungeon rats (wiki: level 12, 12 hitpoints, 10 attack/strength/defence, stab max hit 2,
        // not aggressive; always bones).
        list.add(m("dungeon_rat", "Dungeon rat", 12, 12, 10, 10, 10, 1, 1).speed(4).attack(melee(AttackType.STAB, 2))
                .defenceBonus(0, 0, 0, 0, 0).passive().attributes("rat").drops(Drop.always("bones")).slayer(1, "rats")
                .model("giant_rat").skin(0x5A4A3A, 0x4A3A2A, 0x3A2A1A, 0xD09090).size(0.9f, 0.7f, 1.0f));
        list.add(m("cave_lizard", "Cave lizard", 37, 20, 38, 36, 35, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 5))
                .defenceBonus(5, 15, 15, 0, 5).model("lizard").skin(0x5A6A3A, 0x3A4A2A, 0x2A3A1A, 0xC0A040));
        list.add(m("zamorak_warrior", "Zamorak warrior", 84, 45, 85, 70, 90, 1, 1).speed(4).attack(melee(AttackType.SLASH, 8))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.of("law_rune", 2, 2, 16), Drop.of("death_rune", 2, 2, 16))
                .skin(0xC8A080, 0x6A1A1A, 0x3A3A3A, 0xA02020));
        list.add(m("zamorak_ranger", "Zamorak ranger", 81, 50, 75, 75, 80, 1, 80).speed(4)
                .attack(AttackDef.of("ranged", AttackType.RANGED, 9, 8, 10)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.of("law_rune", 2, 2, 16), Drop.of("death_rune", 2, 2, 16))
                .skin(0xC8A080, 0x5A2A1A, 0x3A2A1A, 0xA02020));
        list.add(m("zamorak_mage", "Zamorak mage", 84, 50, 78, 81, 82, 85, 1).speed(4)
                .attack(AttackDef.of("magic", AttackType.MAGIC, 9, 8, 10)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.of("law_rune", 2, 2, 16), Drop.of("death_rune", 2, 2, 16))
                .skin(0xC8A080, 0x8A1A1A, 0x6A1010, 0x2A2A2A));
        list.add(m("zamorak_crafter", "Zamorak crafter", 19, 25, 15, 15, 12, 20, 1).speed(4).attack(melee(AttackType.CRUSH, 2))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.of("pure_essence", 1, 1, 4))
                .skin(0xC8A080, 0x8A2A2A, 0x5A1A1A, 0x6A4A8A));
        list.addAll(DungeonMonstersExtra.all());
    }

    /** The wiki's locust rider drop table (rarities x/128, noted stacks as plain stacks). */
    private static Drop[] scabariteDrops() {
        return new Drop[]{Drop.always("bones"),
                Drop.of("adamant_kiteshield", 1, 1, 43), Drop.of("battlestaff", 3, 3, 64), Drop.of("rune_dagger", 1, 1, 64),
                Drop.of("rune_mace", 1, 1, 128), Drop.of("rune_sq_shield", 1, 1, 128), Drop.of("adamant_arrow", 36, 36, 32),
                Drop.of("blood_rune", 18, 18, 32), Drop.of("fire_rune", 250, 250, 64), Drop.of("cosmic_rune", 35, 35, 64),
                Drop.of("lava_rune", 150, 150, 64), Drop.of("rune_arrow", 18, 18, 64), Drop.of("raw_lobster", 15, 15, 18),
                Drop.of("adamantite_ore", 14, 14, 64), Drop.of("coal", 32, 32, 21), Drop.of("mithril_bar", 22, 22, 64),
                Drop.of("raw_bass", 24, 24, 32), Drop.of("uncut_sapphire", 4, 4, 43), Drop.of("coins", 2000, 3000, 4),
                Drop.of("lobster", 5, 5, 128)};
    }

    private static Drop[] maniacalDrops() {
        return new Drop[]{Drop.always("bones"), // MonsterDropsTable: small ninja monkey bones
                Drop.of("steel_scimitar", 1, 1, 10), Drop.of("maple_shortbow", 1, 1, 20), Drop.of("oak_plank", 1, 1, 7),
                Drop.of("grimy_guam_leaf", 1, 1, 10), Drop.of("banana", 1, 1, 10), Drop.of("adamant_arrow", 5, 5, 20),
                Drop.of("antipoison(2)", 1, 1, 10), Drop.of("bass", 2, 2, 5), Drop.of("prayer_potion(1)", 1, 1, 10),
                Drop.of("rope", 1, 1, 20)};
    }
}
