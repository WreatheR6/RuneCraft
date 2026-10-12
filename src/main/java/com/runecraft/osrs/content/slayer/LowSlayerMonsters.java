package com.runecraft.osrs.content.slayer;

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
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Low-level slayer creatures (Slayer 1-39) plus the common creatures Turael and Mazchna assign.
 * Stats from the OSRS wiki; where a creature has several variants the most common one is used.
 */
final class LowSlayerMonsters {
    private LowSlayerMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                // --- Turael / Mazchna fodder ---
                m("giant_bat", "Giant bat", 27, 32, 22, 22, 22, 1, 1).speed(4).attack(stab(4))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), clue("easy", 128))
                        .slayer(1, "bats").model("giant_bat").size(0.9f, 0.9f, 1.0f).flying().moveSpeed(0.3)
                        .spawn(Region.WILDERNESS, 4, 1, 3).spawn(Region.MORYTANIA, 3, 1, 2).spawn(Region.KANDARIN, 2, 1, 2),
                m("grizzly_bear", "Grizzly bear", 21, 27, 17, 18, 15, 1, 1).speed(4).attack(slash(3))
                        .drops(Drop.always("bones"), clue("beginner", 70)).slayer(1, "bears").model("grizzly_bear")
                        .size(1.2f, 1.2f, 1.0f).spawn(Region.ASGARNIA, 3, 1, 2).spawn(Region.WILDERNESS, 3, 1, 2)
                        .spawn(Region.KANDARIN, 2, 1, 1),
                m("scorpion", "Scorpion", 14, 17, 11, 11, 11, 1, 1).speed(4).attack(stab(3))
                        .drops(clue("beginner", 70)).slayer(1, "scorpions").model("scorpion").size(0.9f, 0.6f, 1.0f)
                        .spawn(Region.KHARIDIAN_DESERT, 6, 1, 3).spawn(Region.MISTHALIN, 2, 1, 2),
                m("king_scorpion", "King scorpion", 32, 30, 28, 28, 28, 1, 1).speed(4).attack(stab(4))
                        .drops(clue("easy", 128)).slayer(1, "scorpions").model("scorpion").size(0.9f, 0.6f, 1.4f)
                        .spawn(Region.WILDERNESS, 3, 1, 3).spawn(Region.ASGARNIA, 2, 1, 2).underground(),
                m("minotaur", "Minotaur", 27, 22, 20, 21, 19, 1, 1).speed(4).attack(crush(4))
                        .drops(Drop.always("bones"), Drop.of("iron_arrow", 5, 10, 8), Drop.of("bronze_arrow", 5, 15, 8),
                                Drop.herbs(32), clue("beginner", 70)).slayer(1, "minotaurs")
                        .skin(0x7A5A3A, 0x6A4A2A, 0x4A3A2A, 0xD8D0B0).size(0.7f, 2.1f, 1.0f).spawn(Region.MISTHALIN, 3, 1, 3).underground(),
                m("ghost", "Ghost", 19, 25, 13, 13, 18, 1, 1).speed(4).attack(crush(2))
                        .drops(Drop.herbs(16), Drop.of("law_rune", 1, 2, 30), clue("beginner", 70)).slayer(1, "ghosts")
                        .model("ghost").size(0.7f, 1.9f, 1.0f).flying().spawn(Region.MISTHALIN, 3, 1, 2)
                        .spawn(Region.MORYTANIA, 4, 1, 3),
                m("icefiend", "Icefiend", 18, 15, 1, 18, 18, 1, 1).speed(4).attack(crush(3))
                        .drops(Drop.of("water_rune", 5, 15, 6), Drop.herbs(30), clue("beginner", 70)).slayer(1, "icefiends")
                        .model("icefiend").size(0.7f, 1.3f, 1.0f).flying().spawn(Region.ASGARNIA, 3, 2, 4),
                m("hobgoblin", "Hobgoblin", 28, 29, 22, 24, 24, 1, 1).speed(6).attack(stab(5))
                        .drops(Drop.of("hobgoblin_champion_scroll", 1, 1, 5000), Drop.always("bones"), Drop.of("limpwurt_root", 1, 1, 18), coins(5, 60, 4), Drop.herbs(18),
                                Drop.of("iron_dagger", 1, 1, 64), clue("easy", 128)).slayer(1, "hobgoblins")
                        .skin(0x6A8A3A, 0x6A4A2A, 0x4A3A2A, 0x202020).size(0.7f, 2.0f, 1.0f)
                        .spawn(Region.ASGARNIA, 4, 1, 3).spawn(Region.WILDERNESS, 3, 1, 3),
                m("monkey", "Monkey", 3, 6, 2, 2, 2, 1, 1).speed(4).attack(crush(1)).passive()
                        .drops(Drop.always("bones")).slayer(1, "monkeys").model("monkey").size(0.6f, 0.8f, 1.0f)
                        .spawn(Region.KARAMJA, 6, 1, 3),
                m("earth_warrior", "Earth warrior", 51, 54, 42, 42, 42, 1, 1).speed(4).attack(crush(6))
                        .defenceBonus(20, 20, 20, 0, 20).drops(Drop.of("earth_warrior_champion_scroll", 1, 1, 5000), Drop.always("bones"), Drop.of("earth_rune", 6, 30, 5),
                                Drop.of("law_rune", 2, 2, 20), Drop.herbs(14), clue("medium", 128)).slayer(1, "earth_warriors")
                        .skin(0x6A5A3A, 0x5A4A2A, 0x4A3A2A, 0x3A2A1A).size(0.7f, 2.0f, 1.0f).spawn(Region.WILDERNESS, 4, 2, 4)
                        .underground(),
                m("ice_warrior", "Ice warrior", 57, 59, 47, 47, 47, 1, 1).speed(4).attack(crush(7))
                        .defenceBonus(18, 18, 18, 0, 18).drops(Drop.of("water_rune", 10, 20, 5), Drop.of("nature_rune", 2, 4, 20),
                                Drop.of("mithril_ore", 1, 1, 64), Drop.herbs(16), clue("medium", 128)).slayer(1, "ice_warriors")
                        .skin(0x9AC8E8, 0x7AA8D8, 0x5A88B8, 0xD0F0FF).size(0.7f, 2.0f, 1.0f).spawn(Region.ASGARNIA, 3, 1, 3)
                        .spawn(Region.WILDERNESS, 3, 1, 3),

                // --- Slayer creatures ---
                m("crawling_hand", "Crawling Hand", 8, 16, 4, 4, 4, 1, 1).speed(4).attack(crush(2))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), coins(5, 40, 5), Drop.of("gold_ring", 1, 1, 30),
                                Drop.of("sapphire_ring", 1, 1, 50), Drop.of("emerald_ring", 1, 1, 100), Drop.of("leather", 1, 1, 16),
                                clue("easy", 128)).slayer(5, "crawling_hands").model("crawling_hand").size(0.7f, 0.5f, 1.0f)
                        .spawn(Region.MORYTANIA, 5, 2, 4),
                m("cave_bug", "Cave bug", 6, 5, 1, 1, 1, 1, 1).speed(4).attack(crush(1))
                        .drops(Drop.herbs(5), Drop.of("water_rune", 2, 5, 8), Drop.of("earth_rune", 3, 6, 10), Drop.seeds(20))
                        .slayer(7, "cave_bugs").model("cave_bug").size(0.6f, 0.4f, 1.0f).spawn(Region.LUMBRIDGE_SWAMP, 6, 2, 5)
                        .underground(),
                m("cave_crawler", "Cave crawler", 23, 22, 15, 18, 18, 1, 1).speed(4)
                        .attack(stab(3).special(AttackDef.Special.POISON, 3)).defenceBonus(0, 0, 0, 0, 0).poisonous()
                        .drops(Drop.herbs(4), Drop.of("unicorn_horn_dust", 1, 1, 30), Drop.of("red_spiders_eggs", 1, 1, 30),
                                Drop.of("limpwurt_root", 1, 1, 30), Drop.seeds(30), clue("easy", 128))
                        .slayer(10, "cave_crawlers").model("cave_crawler").size(0.9f, 0.6f, 1.0f)
                        .spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4).spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 3).underground(),
                m("banshee", "Banshee", 23, 22, 22, 22, 22, 1, 1).speed(4).attack(crush(1))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(4), Drop.of("pure_essence", 13, 13, 12),
                                Drop.of("fire_rune", 7, 7, 15), Drop.of("cosmic_rune", 2, 2, 20), Drop.seeds(20), clue("easy", 128))
                        .slayer(15, "banshees").model("banshee").size(0.7f, 1.9f, 1.0f).flying()
                        .spawn(Region.MORYTANIA, 5, 2, 4),
                m("cave_slime", "Cave slime", 23, 25, 1, 18, 18, 1, 1).speed(4)
                        .attack(crush(4).special(AttackDef.Special.POISON, 3)).poisonous()
                        .drops(Drop.of("water_rune", 5, 15, 6), Drop.of("iron_ore", 1, 1, 30), Drop.gems(64), clue("easy", 128))
                        .slayer(17, "cave_slime").model("cave_slime").size(0.9f, 0.7f, 1.0f).moveSpeed(0.18)
                        .spawn(Region.LUMBRIDGE_SWAMP, 5, 2, 4).underground(),
                m("rockslug", "Rockslug", 29, 27, 21, 21, 23, 1, 1).speed(4).attack(crush(4))
                        .defenceBonus(20, 20, 20, 10, 20).drops(Drop.of("iron_ore", 1, 1, 15), Drop.of("coal", 1, 1, 15),
                                Drop.herbs(10), Drop.gems(64), clue("easy", 128)).slayer(20, "rockslugs").model("rockslug")
                        .size(0.9f, 0.6f, 1.0f).moveSpeed(0.15).spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4)
                        .spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 3).underground(),
                m("lizard", "Lizard", 42, 40, 30, 31, 32, 1, 1).speed(4).attack(stab(5))
                        .drops(Drop.herbs(16), clue("easy", 128)).slayer(22, "lizards").model("lizard").size(1.1f, 0.8f, 1.2f)
                        .spawn(Region.KHARIDIAN_DESERT, 4, 1, 3),
                m("desert_lizard", "Desert Lizard", 24, 25, 15, 18, 14, 1, 1).speed(4).attack(stab(4))
                        .drops(Drop.herbs(16), clue("easy", 128)).slayer(22, "lizards").model("lizard").size(1.1f, 0.8f, 1.0f)
                        .spawn(Region.KHARIDIAN_DESERT, 6, 2, 4),
                m("cockatrice", "Cockatrice", 37, 37, 37, 37, 37, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("bones"), Drop.herbs(4), Drop.of("law_rune", 2, 2, 25),
                                Drop.of("nature_rune", 3, 3, 25), Drop.seeds(25), clue("medium", 128))
                        .slayer(25, "cockatrice").model("cockatrice").size(0.8f, 1.4f, 1.0f)
                        .spawn(Region.FREMENNIK_PROVINCE, 5, 2, 4).underground(),
                m("pyrefiend", "Pyrefiend", 43, 45, 52, 52, 33, 1, 1).speed(4)
                        .attack(crush(8).particle("minecraft:flame")).defenceBonus(15, 15, 15, 15, 15)
                        .drops(Drop.of("fire_rune", 10, 30, 4), Drop.of("steel_full_helm", 1, 1, 64), Drop.herbs(12),
                                Drop.gems(64), clue("medium", 128)).slayer(30, "pyrefiends").model("pyrefiend")
                        .size(0.7f, 1.4f, 1.0f).flying().spawn(Region.FREMENNIK_PROVINCE, 4, 2, 3)
                        .spawn(Region.KHARIDIAN_DESERT, 3, 1, 3).underground(),
                m("mogre", "Mogre", 60, 48, 30, 41, 43, 1, 1).speed(5).attack(crush(8))
                        .drops(Drop.always("big_bones"), Drop.of("raw_shark", 1, 1, 64), Drop.of("raw_lobster", 1, 1, 16),
                                Drop.herbs(16), clue("medium", 128)).slayer(32, "mogres")
                        .skin(0x5A8A7A, 0x3A6A5A, 0x3A4A3A, 0xE0E0D0).size(1.0f, 2.4f, 1.0f).spawn(Region.ASGARNIA, 2, 1, 3),
                m("harpie_bug_swarm", "Harpie Bug Swarm", 46, 25, 30, 30, 23, 1, 1).speed(4).attack(stab(6))
                        .drops(Drop.herbs(4), Drop.seeds(10), Drop.of("fire_rune", 5, 15, 12), clue("medium", 128))
                        .slayer(33, "harpie_bug_swarms").model("harpie_bug_swarm").size(1.0f, 1.0f, 1.0f).flying()
                        .spawn(Region.KARAMJA, 5, 2, 4),
                m("wall_beast", "Wall beast", 49, 105, 38, 38, 38, 1, 1).speed(5).attack(crush(6))
                        .defenceBonus(10, 10, 10, 0, 10).drops(Drop.always("big_bones"), Drop.of("steel_longsword", 1, 1, 64),
                                Drop.gems(32), clue("medium", 128)).slayer(35, "wall_beasts").model("wall_beast")
                        .size(1.0f, 2.0f, 1.0f).moveSpeed(0.0).spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 2).underground(),
                m("killerwatt", "Killerwatt", 55, 51, 50, 50, 50, 50, 1).speed(4)
                        .attack(magic(8, "minecraft:electric_spark")).attack(crush(8)).defenceBonus(0, 0, 0, 20, 0)
                        .drops(Drop.of("cosmic_rune", 5, 15, 8), Drop.of("air_rune", 15, 30, 6), Drop.herbs(16),
                                clue("medium", 128)).slayer(37, "killerwatts").model("killerwatt").size(0.8f, 1.0f, 1.0f)
                        .flying().spawn(Region.MISTHALIN, 3, 2, 4).underground(),
                m("molanisk", "Molanisk", 51, 52, 50, 40, 40, 1, 1).speed(4).attack(crush(5))
                        .drops(Drop.herbs(10), Drop.of("coal", 1, 2, 12), Drop.gems(64), clue("medium", 128))
                        .slayer(39, "molanisks").model("molanisk").size(1.0f, 0.9f, 1.0f).spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 3)
                        .underground());
    }
}
