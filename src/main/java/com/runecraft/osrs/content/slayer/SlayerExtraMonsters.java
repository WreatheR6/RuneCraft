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
import static com.runecraft.osrs.content.slayer.Kit.ranged;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Slayer task creatures that had no counterpart yet: sourhogs (Spria), crocodiles, shadow warriors and
 * otherworldly beings (Vannaka), and Krystilia's Wilderness creatures (bandits, ents, mammoths, ice
 * spiders). Combat levels from the OSRS wiki; skill levels and some drop rates are approx where noted.
 * Spawn points live in {@code osrs/world/SpawnTable}; models in the bestiary module ({@code BestiaryMcd}).
 */
final class SlayerExtraMonsters {
    private SlayerExtraMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                // Sourhog Cave (A Porcine of Interest). Its spit drains Attack and Defence without reinforced goggles.
                m("sourhog", "Sourhog", 37, 45, 30, 33, 30, 1, 35).speed(5).attack(crush(6)) // approx. skills
                        .attack(ranged(6, "minecraft:item_slime").special(AttackDef.Special.DRAIN_STAT, 0) // 0 = Attack
                                .message("The sourhog spits at you!"))
                        .drops(Drop.always("bones"), Drop.of("nature_rune", 1, 5, 27), coins(5, 80, 4), Drop.herbs(20),
                                clue("easy", 128))
                        .slayer(1, "sourhogs").size(1.0f, 1.1f, 0.9f).spawn(Region.MISTHALIN, 3, 1, 3).underground(),
                // River Elid banks, Kharidian Desert.
                m("crocodile", "Crocodile", 63, 62, 55, 62, 56, 1, 1).speed(4).attack(stab(7).message("The crocodile bites you."))
                        .defenceBonus(10, 10, 10, 0, 10) // approx
                        .drops(Drop.always("bones"), coins(10, 120, 5), Drop.herbs(24), Drop.gems(64), clue("medium", 128))
                        .slayer(1, "crocodiles").size(1.2f, 0.7f, 1.0f).spawn(Region.KHARIDIAN_DESERT, 4, 2, 4),
                // Legends' Guild dungeon (Legends' Quest); the dungeon isn't built, so they lurk by the guild.
                m("shadow_warrior", "Shadow warrior", 48, 67, 40, 40, 38, 1, 1).speed(4).attack(slash(6)) // approx. skills
                        .drops(Drop.always("bones"), Drop.of("black_longsword", 1, 1, 32), Drop.of("black_full_helm", 1, 1, 64),
                                Drop.of("black_kiteshield", 1, 1, 128), coins(10, 200, 4), Drop.herbs(16), clue("hard", 128))
                        .slayer(1, "shadow_warriors").skin(0x2A2A30, 0x141418, 0x101014, 0x6A2AFF).size(0.6f, 1.95f, 1.0f)
                        .spawn(Region.KANDARIN, 3, 1, 3).underground(),
                // Zanaris (Lost City): invisible to the eye, only their outline shimmers.
                m("otherworldly_being", "Otherworldly being", 64, 66, 52, 56, 54, 1, 1).speed(4).attack(crush(6))
                        .drops(Drop.of("cosmic_rune", 2, 8, 8), Drop.of("law_rune", 1, 3, 16), coins(10, 300, 4), Drop.herbs(16),
                                Drop.seeds(40), clue("hard", 128)) // approx
                        .slayer(1, "otherworldly_beings").size(0.6f, 1.95f, 1.0f).spawn(Region.LUMBRIDGE_SWAMP, 3, 1, 3),
                // The Wilderness ents (level 101; stats from MonsterStatsTable). The level 86 Woodcutting Guild ents
                // aren't placed (the guild's dungeon isn't built). In OSRS the ent leaves a trunk that is chopped for
                // noted logs; here the logs drop directly (rates approx).
                m("ent", "Ent", 86, 105, 70, 75, 65, 1, 1).speed(5).attack(crush(10).message("The ent lashes you with a branch."))
                        .defenceBonus(20, 0, 30, 0, 50) // approx
                        .drops(Drop.of("logs", 10, 20, 4), Drop.of("oak_logs", 10, 20, 5), Drop.of("willow_logs", 10, 20, 6),
                                Drop.of("maple_logs", 8, 16, 8), Drop.of("yew_logs", 5, 10, 16), Drop.of("magic_logs", 3, 6, 64),
                                Drop.seeds(16), clue("hard", 128))
                        .slayer(1, "ents").size(1.0f, 3.0f, 1.0f).moveSpeed(0.18).spawn(Region.WILDERNESS, 3, 1, 3),
                // South-east of the Ferox Enclave.
                m("mammoth", "Mammoth", 80, 130, 65, 70, 50, 1, 1).speed(4).attack(crush(9).message("The mammoth gores you."))
                        .drops(Drop.always("big_bones"), Drop.seeds(10), coins(20, 300, 6), clue("medium", 128)) // approx
                        .slayer(1, "mammoths").size(2.0f, 2.6f, 1.0f).moveSpeed(0.2).spawn(Region.WILDERNESS, 3, 2, 4),
                // Frozen Waste Plateau, deep Wilderness. They count towards spider tasks.
                m("ice_spider", "Ice spider", 61, 65, 50, 55, 45, 1, 1).speed(4) // approx. skills
                        .attack(stab(7).particle("minecraft:snowflake"))
                        .drops(coins(10, 120, 4), Drop.herbs(32), clue("medium", 256)) // approx
                        .slayer(1, "spiders").size(1.2f, 0.7f, 1.0f).spawn(Region.WILDERNESS, 3, 2, 4),
                // Karamja jungle spiders (osrs-dps-calc stats; not poisonous, aggressive): the spider carcass source.
                m("jungle_spider", "Jungle spider", 44, 50, 35, 37, 35, 1, 1).speed(4).defenceBonus(20, 20, 10, 17, 20)
                        .attack(stab(6)).drops(Drop.always("spider_carcass"))
                        .slayer(1, "spiders").skin(0x4A5A2A, 0x3A4A1A, 0x2A3A12, 0xC03020).size(1.1f, 0.7f, 1.0f)
                        .spawn(Region.KARAMJA, 3, 2, 4).spawn(Region.KANDARIN, 1, 2, 4),
                // Kharazi Jungle oomlie birds (osrs-dps-calc stats; not aggressive): bones and raw oomlie.
                m("oomlie_bird", "Oomlie bird", 46, 40, 40, 40, 40, 1, 1).speed(4).attack(stab(5)).passive()
                        .drops(Drop.always("bones"), Drop.always("raw_oomlie"))
                        .slayer(1, "birds").skin(0xC0703A, 0xA05A2A, 0x8A4A1A, 0xE0C040).size(0.9f, 1.4f, 1.0f)
                        .spawn(Region.KARAMJA, 3, 1, 3),
                // Wilderness Bandit Camp: level 22 and level 130 bandits (aggressive; fast respawns).
                m("bandit", "Bandit", 22, 27, 18, 20, 16, 1, 1).speed(4).attack(slash(3)) // approx. skills
                        .drops(Drop.always("bones"), coins(5, 80, 3), Drop.of("iron_scimitar", 1, 1, 32), Drop.herbs(40))
                        .slayer(1, "bandits").skin(0xC89A70, 0x5A4A2A, 0x3A2E1E, 0x8A2A1A).size(0.6f, 1.95f, 1.0f)
                        .spawn(Region.WILDERNESS, 3, 2, 4),
                m("wilderness_bandit", "Bandit", 130, 120, 110, 115, 100, 1, 1).speed(4) // approx. skills
                        .attack(AttackDef.of("melee", AttackType.SLASH, 13, 2.5, 10))
                        .drops(Drop.always("bones"), coins(50, 600, 3), Drop.of("rune_scimitar", 1, 1, 128),
                                Drop.of("adamant_platebody", 1, 1, 128), Drop.herbs(16), Drop.gems(32), clue("hard", 128))
                        .slayer(1, "bandits").skin(0xB88A60, 0x3A2E22, 0x2A2218, 0xC0A040).size(0.6f, 1.95f, 1.0f)
                        .spawn(Region.WILDERNESS, 3, 2, 4));
    }
}
