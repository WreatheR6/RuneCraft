package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.clue;
import static com.runecraft.osrs.content.slayer.Kit.coins;
import static com.runecraft.osrs.content.slayer.Kit.crush;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.magic;
import static com.runecraft.osrs.content.slayer.Kit.ranged;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Mid-level slayer creatures (Slayer 40-69) and the unrestricted creatures masters assign
 * (hellhounds, fire giants, kalphite, trolls...). Stats from the OSRS wiki (approximate where noted).
 */
final class MidSlayerMonsters {
    private MidSlayerMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                m("basilisk", "Basilisk", 61, 75, 30, 30, 75, 1, 1).speed(4).attack(crush(8))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.herbs(5), Drop.of("iron_ore", 1, 1, 20),
                                Drop.of("mithril_ore", 1, 1, 64), Drop.of("rune_full_helm", 1, 1, 512), Drop.gems(64), clue("hard", 128))
                        .slayer(40, "basilisks").model("basilisk").size(1.2f, 1.0f, 1.0f).spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4)
                        .underground(),
                m("sea_snake_young", "Sea Snake Young", 90, 85, 75, 85, 50, 1, 1).speed(4)
                        .attack(stab(7).special(AttackDef.Special.POISON, 6)).poisonous()
                        .drops(Drop.always("bones"), Drop.herbs(10), clue("hard", 256)).slayer(40, "sea_snakes")
                        .model("sea_snake").size(0.9f, 0.6f, 1.2f).spawn(Region.FREMENNIK_PROVINCE, 3, 2, 3).underground(),
                m("terror_dog", "Terror dog", 100, 62, 77, 77, 62, 1, 1).speed(4).attack(stab(10))
                        .drops(Drop.always("bones"), Drop.herbs(12), coins(20, 300, 5), clue("hard", 128))
                        .slayer(40, "terror_dogs").model("terror_dog").size(0.9f, 1.1f, 1.0f).spawn(Region.MORYTANIA, 3, 2, 4)
                        .underground(),
                m("fever_spider", "Fever spider", 49, 40, 42, 42, 42, 1, 1).speed(4).attack(stab(7))
                        .drops(Drop.herbs(4), Drop.seeds(18), clue("medium", 128)).slayer(42, "fever_spiders")
                        .model("fever_spider").size(1.2f, 0.7f, 1.0f).spawn(Region.UNCHARTED_ISLES, 4, 2, 4).underground(),
                m("infernal_mage", "Infernal Mage", 66, 60, 1, 1, 66, 60, 1).speed(5)
                        .attack(magic(8, "minecraft:flame").message("The infernal mage casts Fire Bolt."))
                        .defenceBonus(0, 0, 0, 20, 0).drops(Drop.always("bones"), Drop.of("fire_rune", 20, 40, 4),
                                Drop.of("chaos_rune", 5, 10, 8), Drop.of("death_rune", 2, 5, 16), Drop.of("staff_of_fire", 1, 1, 128),
                                Drop.herbs(20), clue("medium", 128)).slayer(45, "infernal_mages")
                        .skin(0x8A6A5A, 0x6A1A1A, 0x3A0A0A, 0x2A0A0A).size(0.7f, 2.0f, 1.0f).spawn(Region.MORYTANIA, 4, 1, 3),
                m("brine_rat", "Brine rat", 70, 50, 70, 70, 50, 1, 1).speed(4).attack(crush(6))
                        .drops(Drop.always("bones"), Drop.of("brine_sabre", 1, 1, 512), Drop.of("raw_lobster", 1, 1, 12),
                                Drop.of("water_rune", 10, 30, 6), clue("hard", 128)).slayer(47, "brine_rats").model("brine_rat")
                        .size(1.0f, 0.7f, 1.0f).spawn(Region.FREMENNIK_PROVINCE, 4, 2, 4).underground(),
                m("bloodveld", "Bloodveld", 76, 120, 75, 45, 30, 1, 1).speed(4).attack(crush(6))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("vile_ashes"), coins(10, 450, 3), Drop.of("fire_rune", 30, 60, 8),
                                Drop.of("blood_rune", 3, 10, 32), Drop.of("steel_full_helm", 1, 1, 64), Drop.of("rune_full_helm", 1, 1, 1024),
                                Drop.gems(64), clue("hard", 256)).slayer(50, "bloodveld").model("bloodveld").size(0.9f, 1.8f, 1.0f)
                        .spawn(Region.MORYTANIA, 5, 1, 3).spawn(Region.TROLL_COUNTRY, 2, 1, 2).spawn(Region.KANDARIN, 2, 1, 2),
                m("mutated_bloodveld", "Mutated Bloodveld", 123, 170, 110, 110, 70, 1, 1).speed(4).attack(crush(11))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("vile_ashes"), coins(50, 800, 3), Drop.of("blood_rune", 5, 15, 16),
                                Drop.of("rune_full_helm", 1, 1, 256), Drop.herbs(12), Drop.gems(32), clue("hard", 256))
                        .slayer(50, "bloodveld").model("mutated_bloodveld").size(1.0f, 2.0f, 1.0f)
                        .spawn(Region.KOUREND, 3, 1, 2).underground(),
                m("jelly", "Jelly", 78, 75, 45, 45, 120, 1, 1).speed(4).attack(crush(9))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(8), coins(20, 300, 4), Drop.of("chaos_rune", 5, 15, 12),
                                Drop.gems(32), clue("hard", 128)).slayer(52, "jellies").model("jelly").size(1.0f, 1.6f, 1.0f)
                        .moveSpeed(0.18).spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4).spawn(Region.WILDERNESS, 2, 1, 3).underground(),
                m("turoth", "Turoth", 87, 79, 67, 69, 72, 1, 1).speed(6).attack(crush(10))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(4), Drop.of("leaf_bladed_spear", 1, 1, 1250),
                                Drop.of("leaf_bladed_sword", 1, 1, 1250), Drop.of("nature_rune", 4, 12, 16), Drop.seeds(20),
                                clue("hard", 128)).slayer(55, "turoth").model("turoth").size(1.0f, 2.2f, 1.0f)
                        .spawn(Region.FREMENNIK_PROVINCE, 4, 2, 4).underground(),
                m("mutated_zygomite", "Mutated Zygomite", 74, 65, 60, 60, 60, 1, 1).speed(4).attack(crush(7))
                        .drops(Drop.herbs(5), Drop.seeds(8), coins(10, 200, 5), clue("hard", 128)).slayer(57, "mutated_zygomites")
                        .model("zygomite").size(0.8f, 1.6f, 1.0f).spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 3).underground(),
                m("cave_horror", "Cave horror", 80, 55, 80, 80, 62, 1, 1).speed(4).attack(crush(10))
                        .drops(Drop.always("big_bones"), Drop.of("black_mask", 1, 1, 512), coins(30, 300, 4), Drop.herbs(12),
                                Drop.seeds(25), clue("hard", 128)).slayer(58, "cave_horrors")
                        .skin(0x3A5A3A, 0x2A3A2A, 0x2A3A2A, 0xA0A060).size(0.8f, 2.2f, 1.0f).spawn(Region.MORYTANIA, 4, 2, 4)
                        .underground(),
                m("aberrant_spectre", "Aberrant spectre", 96, 90, 1, 1, 90, 105, 1).speed(4)
                        .attack(magic(8, "minecraft:sneeze").message("The aberrant spectre releases a foul stench."))
                        .defenceBonus(20, 20, 20, 0, 15).drops(Drop.herbs(2), Drop.seeds(15), Drop.of("steel_longsword", 1, 1, 64),
                                Drop.of("lantadyme", 1, 1, 64), Drop.gems(64), clue("hard", 128)).slayer(60, "aberrant_spectres")
                        .model("aberrant_spectre").size(0.9f, 2.0f, 1.0f).flying().spawn(Region.MORYTANIA, 4, 1, 3)
                        .spawn(Region.KANDARIN, 3, 1, 3).underground(),
                m("spiritual_ranger", "Spiritual ranger", 115, 120, 100, 100, 100, 1, 115).speed(4)
                        .attack(ranged(14, "minecraft:crit")).defenceBonus(60, 60, 60, 40, 80)
                        .drops(Drop.always("bones"), Drop.of("rune_arrow", 5, 20, 12), Drop.of("adamant_arrow", 10, 30, 8),
                                Drop.herbs(16), clue("hard", 128)).slayer(63, "spiritual_creatures")
                        .skin(0x5A6A8A, 0x2A4A2A, 0x3A3A2A, 0x6A8A6A).size(0.7f, 2.0f, 1.0f)
                        .spawn(Region.TROLL_COUNTRY, 3, 1, 3).spawn(Region.WILDERNESS, 2, 1, 2).underground(),
                m("spiritual_warrior", "Spiritual warrior", 123, 100, 110, 115, 110, 1, 1).speed(5).attack(slash(16))
                        .defenceBonus(80, 80, 80, 0, 60).drops(Drop.always("bones"), Drop.of("rune_full_helm", 1, 1, 128),
                                Drop.of("adamant_platebody", 1, 1, 64), Drop.herbs(16), clue("hard", 128))
                        .slayer(68, "spiritual_creatures").skin(0x5A6A8A, 0x8A2A2A, 0x5A2A2A, 0xB0B0B0).size(0.7f, 2.0f, 1.0f)
                        .spawn(Region.TROLL_COUNTRY, 3, 1, 3).spawn(Region.WILDERNESS, 2, 1, 2).underground(),
                m("spiritual_mage", "Spiritual mage", 120, 75, 1, 1, 86, 140, 1).speed(5)
                        .attack(magic(18, "minecraft:witch")).defenceBonus(20, 20, 20, 80, 20)
                        .drops(Drop.always("bones"), Drop.of("dragon_boots", 1, 1, 128), Drop.of("death_rune", 5, 15, 8),
                                Drop.of("blood_rune", 2, 8, 16), Drop.herbs(16), clue("hard", 128))
                        .slayer(83, "spiritual_creatures").skin(0x5A6A8A, 0x2A2A6A, 0x2A2A4A, 0x8080FF).size(0.7f, 2.0f, 1.0f)
                        .spawn(Region.TROLL_COUNTRY, 2, 1, 2).spawn(Region.WILDERNESS, 1, 1, 2).underground(),
                m("dust_devil", "Dust devil", 93, 105, 105, 70, 40, 1, 1).speed(4).attack(crush(8))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.of("dragon_chainbody", 1, 1, 32768),
                                Drop.of("dust_battlestaff", 1, 1, 4000), Drop.of("chaos_rune", 10, 40, 6), Drop.of("earth_rune", 30, 80, 6),
                                Drop.of("adamant_dagger", 1, 1, 64), Drop.herbs(16), Drop.gems(32), clue("hard", 128))
                        .slayer(65, "dust_devils").model("dust_devil").size(0.9f, 1.8f, 1.0f).flying()
                        .spawn(Region.KHARIDIAN_DESERT, 5, 2, 4).underground(),
                m("spitting_wyvern", "Spitting wyvern", 139, 200, 100, 100, 120, 1, 105).speed(4).attack(crush(13))
                        .attack(ranged(14, "minecraft:snowflake").message("The wyvern spits icy venom."))
                        .attack(AttackDef.of("icy breath", AttackType.MAGIC, 40, 4, 2).special(AttackDef.Special.DRAGONFIRE, 0)
                                .particle("minecraft:snowflake").message("The wyvern breathes icy breath!"))
                        .defenceBonus(80, 80, 80, 60, 90).drops(Drop.always("wyvern_bones"), Drop.of("wyvern_visage", 1, 1, 10000),
                                Drop.of("granite_legs", 1, 1, 512), Drop.of("granite_boots", 1, 1, 1024), Drop.of("granite_longsword", 1, 1, 1024), Drop.of("rune_battleaxe", 1, 1, 128), Drop.herbs(12), Drop.gems(32),
                                clue("hard", 256), clue("elite", 1200)).slayer(66, "fossil_island_wyverns").model("spitting_wyvern")
                        .size(1.4f, 2.4f, 1.0f).spawn(Region.FOSSIL_ISLAND, 2, 1, 2).underground(),
                m("taloned_wyvern", "Taloned wyvern", 147, 200, 110, 110, 120, 1, 1).speed(4).attack(slash(16))
                        .attack(AttackDef.of("icy breath", AttackType.MAGIC, 40, 4, 2).special(AttackDef.Special.DRAGONFIRE, 0)
                                .particle("minecraft:snowflake").message("The wyvern breathes icy breath!"))
                        .defenceBonus(90, 90, 90, 60, 80).drops(Drop.always("wyvern_bones"), Drop.of("wyvern_visage", 1, 1, 10000),
                                Drop.of("granite_legs", 1, 1, 512), Drop.of("granite_boots", 1, 1, 1024), Drop.of("granite_longsword", 1, 1, 1024), Drop.of("rune_battleaxe", 1, 1, 128), Drop.herbs(12), Drop.gems(32),
                                clue("hard", 256), clue("elite", 1200)).slayer(66, "fossil_island_wyverns").model("taloned_wyvern")
                        .size(1.4f, 2.4f, 1.0f).spawn(Region.FOSSIL_ISLAND, 2, 1, 2).underground(),
                m("ankou", "Ankou", 75, 60, 60, 60, 60, 1, 1).speed(4).attack(crush(8))
                        .drops(Drop.always("bones"), Drop.of("death_rune", 2, 10, 6), Drop.of("blood_rune", 2, 8, 10),
                                Drop.of("law_rune", 2, 5, 10), Drop.of("adamant_arrow", 5, 15, 12), Drop.herbs(14), clue("hard", 256))
                        .slayer(1, "ankou").skin(0xE0E0D0, 0x2A2A3A, 0x2A2A3A, 0x101010).size(0.7f, 2.1f, 1.0f)
                        .spawn(Region.WILDERNESS, 3, 1, 3).spawn(Region.MISTHALIN, 2, 1, 3).spawn(Region.KANDARIN, 2, 1, 2).underground(),
                m("hellhound", "Hellhound", 122, 116, 105, 104, 102, 1, 1).speed(4).attack(stab(11))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("vile_ashes"), Drop.of("smouldering_stone", 1, 1, 32768),
                                clue("hard", 64)).slayer(1, "hellhounds").model("hellhound").size(1.0f, 1.3f, 1.0f)
                        .spawn(Region.ASGARNIA, 4, 1, 3).spawn(Region.WILDERNESS, 3, 1, 3).spawn(Region.KANDARIN, 2, 1, 2).underground(),
                m("fire_giant", "Fire giant", 86, 111, 65, 65, 65, 1, 1).speed(5).attack(crush(11))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.of("giant_champion_scroll", 1, 1, 5000), Drop.always("big_bones"), coins(10, 600, 3), Drop.of("fire_rune", 50, 150, 6),
                                Drop.of("rune_scimitar", 1, 1, 128), Drop.of("steel_axe", 1, 1, 32), Drop.of("law_rune", 2, 2, 20),
                                Drop.of("blood_rune", 5, 5, 32), Drop.herbs(14), Drop.gems(20), clue("hard", 128))
                        .slayer(1, "fire_giants").skin(0xD07040, 0x8A3A1A, 0x5A2A1A, 0xF0A020).size(1.0f, 3.0f, 1.0f)
                        .spawn(Region.KANDARIN, 4, 1, 3).spawn(Region.KHARIDIAN_DESERT, 3, 1, 3).underground(),
                m("kalphite_worker", "Kalphite Worker", 28, 40, 20, 20, 20, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(10, 10, 10, 0, 10).drops(Drop.herbs(30)).slayer(1, "kalphite").model("kalphite")
                        .size(1.0f, 0.9f, 0.8f).spawn(Region.KHARIDIAN_DESERT, 6, 2, 5).underground(),
                m("kalphite_soldier", "Kalphite Soldier", 85, 90, 70, 70, 70, 1, 1).speed(4).attack(stab(10))
                        .defenceBonus(30, 30, 30, 0, 30).drops(coins(20, 300, 4), Drop.herbs(16), Drop.gems(64), clue("hard", 256))
                        .slayer(1, "kalphite").model("kalphite").size(1.0f, 0.9f, 1.2f).spawn(Region.KHARIDIAN_DESERT, 4, 1, 3)
                        .underground(),
                m("kalphite_guardian", "Kalphite Guardian", 141, 170, 120, 130, 140, 1, 1).speed(4).attack(crush(15))
                        .defenceBonus(80, 80, 60, 0, 80).drops(coins(100, 800, 3), Drop.herbs(10), Drop.gems(32), clue("hard", 128))
                        .slayer(1, "kalphite").model("kalphite").size(1.0f, 0.9f, 1.6f).spawn(Region.KHARIDIAN_DESERT, 2, 1, 2)
                        .underground(),
                m("mountain_troll", "Mountain troll", 69, 90, 60, 60, 60, 1, 1).speed(4).attack(crush(8))
                        .drops(Drop.always("big_bones"), coins(10, 200, 4), Drop.herbs(14), Drop.gems(64), clue("medium", 128))
                        .slayer(1, "trolls").skin(0x8A8A6A, 0x6A6A4A, 0x4A4A3A, 0x2A2A1A).size(1.0f, 2.6f, 1.0f)
                        .spawn(Region.TROLL_COUNTRY, 6, 1, 3).spawn(Region.KOUREND, 2, 1, 3),
                m("ice_troll", "Ice troll", 82, 80, 70, 70, 70, 1, 1).speed(4).attack(crush(9))
                        .drops(Drop.always("big_bones"), Drop.of("raw_tuna", 1, 1, 10), Drop.herbs(14), Drop.gems(64), clue("hard", 256))
                        .slayer(1, "trolls").skin(0xA0C0D8, 0x7A9AB8, 0x5A7A98, 0xE0F0FF).size(1.0f, 2.6f, 1.0f)
                        .spawn(Region.FREMENNIK_PROVINCE, 4, 1, 3),
                m("ogre", "Ogre", 53, 60, 43, 43, 43, 1, 1).speed(6).attack(crush(7))
                        .drops(Drop.always("big_bones"), coins(5, 100, 4), Drop.herbs(18), clue("medium", 128))
                        .slayer(1, "ogres").skin(0x8AA060, 0x6A5A3A, 0x4A3A2A, 0x3A3A2A).size(1.0f, 2.6f, 1.0f)
                        .spawn(Region.FELDIP_HILLS, 6, 1, 3).spawn(Region.KANDARIN, 2, 1, 2),
                m("cyclops", "Cyclops", 76, 75, 55, 60, 55, 1, 1).speed(5).attack(crush(9))
                        .drops(Drop.always("big_bones"), coins(10, 300, 4), Drop.of("rune_full_helm", 1, 1, 256), Drop.herbs(16),
                                clue("hard", 256)).slayer(1, "cyclopes").skin(0xC8A080, 0x6A4A2A, 0x4A3A2A, 0xFFFFFF)
                        .size(1.0f, 2.8f, 1.0f).spawn(Region.ASGARNIA, 3, 1, 3).underground(),
                m("dagannoth", "Dagannoth", 74, 70, 68, 70, 71, 1, 1).speed(4).attack(crush(7))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.of("raw_shark", 1, 1, 32),
                                Drop.of("water_rune", 10, 30, 6), Drop.herbs(16), clue("hard", 256)).slayer(1, "dagannoth")
                        .model("dagannoth").size(1.0f, 2.2f, 1.0f).spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4).underground(),
                m("elf_warrior", "Elf warrior", 90, 105, 80, 80, 80, 1, 80).speed(5).attack(stab(11))
                        .defenceBonus(40, 40, 40, 20, 40).drops(Drop.always("bones"), coins(30, 400, 3), Drop.of("rune_dagger", 1, 1, 128),
                                Drop.herbs(14), Drop.gems(64), clue("hard", 128)).slayer(1, "elves")
                        .skin(0xE0C8A8, 0x3A6A3A, 0x2A4A2A, 0xE8E0B0).size(0.7f, 2.0f, 1.0f).spawn(Region.TIRANNWN, 5, 1, 3),
                m("lizardman", "Lizardman", 62, 60, 55, 55, 30, 1, 1).speed(4)
                        .attack(stab(7)).attack(AttackDef.of("acid", AttackType.RANGED, 6, 8, 5)
                                .special(AttackDef.Special.POISON, 4).particle("minecraft:item_slime"))
                        .drops(Drop.always("bones"), Drop.herbs(10), Drop.seeds(18), clue("medium", 128)).slayer(1, "lizardmen")
                        .model("lizardman").size(0.8f, 2.0f, 1.0f).spawn(Region.KOUREND, 4, 2, 4),
                m("lizardman_shaman", "Lizardman shaman", 150, 150, 120, 120, 140, 50, 120).speed(4)
                        .attack(stab(18)).attack(AttackDef.of("acid spit", AttackType.RANGED, 21, 8, 6)
                                .special(AttackDef.Special.POISON, 10).particle("minecraft:item_slime"))
                        .attack(AttackDef.of("jump", AttackType.CRUSH, 25, 8, 2).special(AttackDef.Special.SKY_STRIKE, 0)
                                .telegraph(3, "The shaman leaps into the air!"))
                        .defenceBonus(100, 100, 100, 50, 100).drops(Drop.always("bones"), Drop.of("dragon_warhammer", 1, 1, 5000),
                                Drop.herbs(8), Drop.seeds(12), Drop.gems(32), clue("hard", 256)).slayer(1, "lizardmen")
                        .model("lizardman").size(0.9f, 2.2f, 1.2f).spawn(Region.KOUREND, 2, 1, 2),
                m("waterfiend", "Waterfiend", 115, 128, 105, 105, 70, 105, 105).speed(4)
                        .attack(magic(14, "minecraft:splash")).attack(ranged(14, "minecraft:splash"))
                        .defenceBonus(80, 80, 80, 0, 80).drops(Drop.of("water_rune", 50, 150, 5), Drop.of("blood_rune", 5, 15, 16),
                                Drop.of("mist_battlestaff", 1, 1, 3000), Drop.herbs(12), Drop.gems(32), clue("hard", 256))
                        .slayer(1, "waterfiends").model("waterfiend").size(0.9f, 2.0f, 1.0f).flying()
                        .spawn(Region.KANDARIN, 2, 1, 3).underground());
    }
}
