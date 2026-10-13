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
 * High-level slayer creatures (Slayer 62-95). Karuulm Slayer Dungeon creatures (wyrms, drakes,
 * hydras) live in the Uncharted Isles because Zeah isn't on the map yet.
 */
final class HighSlayerMonsters {
    private HighSlayerMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                m("wyrm", "Wyrm", 99, 130, 85, 125, 160, 85, 1).speed(5).attack(slash(13))
                        .attack(magic(13, "minecraft:dragon_breath")).defenceBonus(50, 50, 50, 20, 60)
                        .drops(Drop.always("bones"), Drop.of("dragon_sword", 1, 1, 10000), Drop.of("dragon_harpoon", 1, 1, 10000), Drop.of("dragon_knife", 75, 150, 2000),
                                Drop.of("death_rune", 5, 15, 8), Drop.of("chaos_rune", 10, 30, 8), Drop.herbs(12), Drop.gems(32),
                                clue("hard", 128), clue("elite", 1200)).slayer(62, "wyrms").model("wyrm").size(1.0f, 1.4f, 1.3f)
                        .spawn(Region.KOUREND, 4, 1, 3).underground(),
                m("kurask", "Kurask", 106, 97, 67, 105, 105, 1, 1).speed(4).attack(crush(16))
                        .defenceBonus(20, 20, 10, 0, 20).drops(Drop.always("big_bones"), Drop.of("leaf_bladed_sword", 1, 1, 384),
                                Drop.of("leaf_bladed_battleaxe", 1, 1, 1026), Drop.of("nature_rune", 10, 30, 12),
                                Drop.of("rune_longsword", 1, 1, 384), Drop.herbs(4), Drop.seeds(18), Drop.rare(64), clue("hard", 128))
                        .slayer(70, "kurask").model("kurask").size(1.0f, 2.6f, 1.0f).spawn(Region.FREMENNIK_PROVINCE, 4, 2, 3)
                        .spawn(Region.TIRANNWN, 2, 1, 2).underground(),
                m("skeletal_wyvern", "Skeletal Wyvern", 140, 210, 125, 120, 120, 125, 125).speed(4).attack(slash(13))
                        .attack(ranged(16, "minecraft:snowflake").message("The wyvern whips its tail."))
                        .attack(AttackDef.of("icy breath", AttackType.MAGIC, 50, 4, 3).special(AttackDef.Special.DRAGONFIRE, 0)
                                .particle("minecraft:snowflake").message("The skeletal wyvern breathes icy breath!"))
                        .defenceBonus(140, 90, 90, 80, 140).drops(Drop.always("wyvern_bones"), Drop.of("granite_legs", 1, 1, 512),
                                Drop.of("draconic_visage", 1, 1, 10000), Drop.of("dragon_platelegs", 1, 1, 512),
                                Drop.of("rune_battleaxe", 1, 1, 128), Drop.of("adamantite_bar", 2, 2, 12), Drop.of("runite_ore", 1, 1, 64),
                                Drop.herbs(14), Drop.gems(32), clue("elite", 350)).slayer(72, "skeletal_wyverns")
                        .model("skeletal_wyvern").size(1.4f, 2.4f, 1.0f).spawn(Region.ASGARNIA, 2, 1, 3).underground(),
                m("gargoyle", "Gargoyle", 111, 105, 75, 105, 107, 1, 1).speed(4).attack(crush(11))
                        .defenceBonus(20, 20, 0, 20, 20).drops(Drop.of("granite_maul", 1, 1, 512),
                                Drop.of("mystic_robe_top_dark", 1, 1, 512), Drop.of("rune_full_helm", 1, 1, 128),
                                Drop.of("adamant_platelegs", 1, 1, 64), Drop.of("gold_bar", 2, 4, 12), Drop.of("steel_bar", 2, 4, 12),
                                Drop.of("mithril_bar", 1, 3, 16), coins(200, 800, 3), Drop.rare(32), clue("hard", 128))
                        .slayer(75, "gargoyles").model("gargoyle").size(1.0f, 2.2f, 1.0f).spawn(Region.MORYTANIA, 4, 1, 3),
                m("nechryael", "Nechryael", 115, 105, 97, 97, 105, 1, 1).speed(4).attack(crush(9))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("malicious_ashes"), Drop.of("rune_boots", 1, 1, 116),
                                Drop.of("rune_full_helm", 1, 1, 39), Drop.of("adamant_kiteshield", 1, 1, 64),
                                Drop.of("death_rune", 5, 15, 8), Drop.of("chaos_rune", 20, 40, 8),
                                coins(100, 1000, 3), Drop.herbs(12), Drop.rare(20), clue("hard", 128))
                        .slayer(80, "nechryael").model("nechryael").size(1.0f, 2.4f, 1.0f).spawn(Region.MORYTANIA, 4, 1, 3)
                        .spawn(Region.KANDARIN, 2, 1, 2),
                m("greater_nechryael", "Greater Nechryael", 200, 205, 210, 210, 120, 1, 1).speed(4).attack(crush(18))
                        .defenceBonus(20, 20, 20, 0, 20).drops(Drop.always("malicious_ashes"), Drop.of("rune_boots", 1, 1, 128),
                                Drop.of("rune_full_helm", 1, 1, 43),
                                Drop.of("rune_kiteshield", 1, 1, 128), Drop.of("death_rune", 10, 30, 8), Drop.of("blood_rune", 10, 20, 12),
                                coins(500, 3000, 3), Drop.herbs(10), Drop.gems(16), clue("hard", 128))
                        .slayer(80, "nechryael").model("nechryael").size(1.0f, 2.4f, 1.3f).spawn(Region.KOUREND, 2, 1, 2)
                        .underground(),
                m("abyssal_demon", "Abyssal demon", 124, 150, 97, 67, 135, 1, 1).speed(4).attack(stab(8))
                        .attack(AttackDef.of("teleport", AttackType.STAB, 8, 12, 1).special(AttackDef.Special.BLINK, 0)
                                .message("The abyssal demon teleports beside you."))
                        .defenceBonus(20, 20, 20, 0, 20).drops(Drop.always("abyssal_ashes"), Drop.of("abyssal_whip", 1, 1, 512),
                                Drop.of("abyssal_dagger", 1, 1, 32000), Drop.of("abyssal_head", 1, 1, 6000),
                                Drop.of("rune_full_helm", 1, 1, 128), Drop.of("blood_rune", 7, 7, 12), Drop.of("law_rune", 3, 3, 16),
                                Drop.of("pure_essence", 60, 60, 16), coins(100, 4000, 3), Drop.herbs(10), Drop.rare(32), clue("hard", 128),
                                clue("elite", 1200)).slayer(85, "abyssal_demons").model("abyssal_demon").size(0.8f, 2.2f, 1.2f)
                        .spawn(Region.MORYTANIA, 4, 1, 2).spawn(Region.KANDARIN, 3, 1, 2).underground(), // Slayer Tower, Stronghold Slayer Cave
                m("cave_kraken", "Cave kraken", 127, 125, 1, 1, 150, 120, 1).speed(4)
                        .attack(magic(13, "minecraft:splash").message("The cave kraken casts Water Strike."))
                        .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH).defenceBonus(0, 0, 0, 0, 0)
                        .drops(Drop.of("trident_of_the_seas", 1, 1, 200), Drop.of("raw_shark", 2, 2, 12),
                                Drop.of("water_rune", 30, 80, 5), Drop.of("rune_kiteshield", 1, 1, 256), coins(200, 1000, 3),
                                Drop.herbs(12), Drop.gems(32), clue("hard", 128)).slayer(87, "cave_kraken").model("cave_kraken")
                        .size(1.4f, 1.4f, 1.0f).moveSpeed(0.08).spawn(Region.KANDARIN, 3, 1, 3).underground(),
                m("dark_beast", "Dark beast", 182, 220, 170, 160, 170, 80, 1).speed(4).attack(crush(17))
                        .attack(magic(8, "minecraft:squid_ink").message("The dark beast fires a bolt of darkness."))
                        .defenceBonus(0, 0, 0, 0, 0).drops(Drop.always("big_bones"), Drop.of("dark_bow", 1, 1, 512),
                                Drop.of("death_rune", 10, 30, 8), Drop.of("blood_rune", 10, 30, 8), Drop.of("rune_full_helm", 1, 1, 64),
                                Drop.of("adamantite_bar", 3, 3, 12), Drop.of("runite_bar", 1, 1, 64), coins(150, 2000, 3),
                                Drop.herbs(10), Drop.rare(16), clue("hard", 128), clue("elite", 1200))
                        .slayer(90, "dark_beasts").model("dark_beast").size(1.2f, 1.8f, 1.0f).spawn(Region.TIRANNWN, 3, 1, 3)
                        .underground(), // Iorwerth Dungeon (the Mourner Tunnels aren't built)
                m("drake", "Drake", 192, 250, 140, 140, 70, 108, 140).speed(4).attack(crush(16).message("The drake bites you."))
                        .attack(ranged(14, "minecraft:flame"))
                        .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 30, 6, 2).special(AttackDef.Special.HAZARD, 6)
                                .telegraph(2, "The drake spews volcanic dragonfire!").particle("minecraft:lava"))
                        .defenceBonus(100, 100, 100, 60, 100).drops(Drop.always("dragon_bones"), Drop.of("drakes_tooth", 1, 1, 512),
                                Drop.of("drakes_claw", 1, 1, 512), Drop.of("dragon_thrownaxe", 100, 200, 2000), Drop.of("runite_ore", 1, 2, 32), Drop.of("rune_full_helm", 1, 1, 128),
                                Drop.herbs(10), Drop.gems(16), clue("hard", 256)).slayer(84, "drakes").model("drake")
                        .size(1.2f, 2.2f, 1.0f).spawn(Region.KOUREND, 3, 1, 2).underground(),
                m("smoke_devil", "Smoke devil", 160, 185, 140, 140, 255, 140, 140).speed(4)
                        .attack(ranged(8, "minecraft:large_smoke").message("The smoke devil lashes out with smoke."))
                        .defenceBonus(0, 0, 0, 600, 44).drops(Drop.of("occult_necklace", 1, 1, 512),
                                Drop.of("dragon_chainbody", 1, 1, 32768), Drop.of("rune_chainbody", 1, 1, 64),
                                Drop.of("fire_rune", 50, 150, 6), Drop.of("air_rune", 50, 150, 6), Drop.of("rune_full_helm", 1, 1, 128),
                                coins(200, 1500, 3), Drop.herbs(10), Drop.gems(16), clue("hard", 128), clue("elite", 1200))
                        .slayer(93, "smoke_devils").model("smoke_devil").size(1.0f, 1.8f, 1.0f).flying()
                        .spawn(Region.KANDARIN, 3, 1, 3).underground(),
                m("hydra", "Hydra", 194, 300, 100, 100, 100, 150, 150).speed(6)
                        .attack(ranged(22, "minecraft:item_slime").message("The hydra spits a venomous bolt."))
                        .attack(magic(22, "minecraft:witch")).defenceBonus(100, 100, 100, 50, 100)
                        .drops(Drop.always("hydra_bones"), Drop.of("hydras_eye", 1, 1, 512), Drop.of("hydras_fang", 1, 1, 512),
                                Drop.of("hydras_heart", 1, 1, 512), Drop.of("hydra_tail", 1, 1, 512), Drop.of("dragon_knife", 200, 400, 2000),
                                Drop.of("dragon_thrownaxe", 200, 400, 2000),
                                Drop.of("runite_bar", 1, 2, 32), Drop.of("rune_kiteshield", 1, 1, 128), Drop.herbs(8), Drop.gems(16),
                                clue("hard", 128), clue("elite", 512)).slayer(95, "hydras").model("hydra").size(1.4f, 2.2f, 1.0f)
                        .spawn(Region.KOUREND, 2, 1, 2).underground(),
                m("basilisk_knight", "Basilisk Knight", 204, 300, 200, 200, 200, 180, 1).speed(4).attack(slash(26))
                        .attack(magic(22, "minecraft:enchant")).defenceBonus(200, 200, 200, 100, 200)
                        .drops(Drop.always("big_bones"), Drop.of("basilisk_jaw", 1, 1, 1000), Drop.of("rune_full_helm", 1, 1, 64),
                                Drop.of("runite_bar", 1, 2, 32), Drop.herbs(8), Drop.gems(16), clue("hard", 128), clue("elite", 1200))
                        .slayer(60, "basilisks").model("basilisk_knight").size(1.0f, 2.4f, 1.0f)
                        .spawn(Region.FREMENNIK_PROVINCE, 2, 1, 2).underground());
    }
}
