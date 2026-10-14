package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.crush;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.ranged;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Slayer task monsters that were missing (round 16 monster completeness audit): every monster in a "Slayer task/..."
 * wiki table that has surface spawns in the cache was diffed against RuneCraft's creatures, and these were added.
 * Stats from the wiki's DPS calculator data (weirdgloop/osrs-dps-calc {@code monsters.json}); drops, aggression and
 * poison from the wiki monster pages (Kiyogitpy/osrs-wiki-data, July 2026). Drop items RuneCraft doesn't have (raw bear
 * meat, wolf bones, undead meat, Braindeath items...) are left out. Attributes, size, poison/venom immunity and Slayer
 * XP come from {@code MonsterTraits}. Spawn points are in {@code SpawnTable} at the cache clusters (mejrs/data_osrs);
 * every creature reuses the model of its OSRS look-alike, recoloured.
 */
final class SlayerTaskMonsters {
    private SlayerTaskMonsters() {
    }

    static List<MonsterDef> all() {
        return List.of(
                m("black_bear", "Black bear", 19, 25, 15, 16, 13, 1, 1).speed(4).attack(slash(3))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bear_fur"), Drop.always("bones"))
                        .slayer(1, "bears").model("black_bear").size(1.2f, 1.2f, 1.0f).spawn(Region.ASGARNIA, 3, 1, 3).spawn(Region.KANDARIN, 3, 1, 3).spawn(Region.KOUREND, 3, 1, 3).passive(),
                m("bear_cub", "Bear Cub", 15, 20, 10, 15, 10, 1, 1).speed(4).attack(slash(2))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bear_fur"), Drop.always("bones"))
                        .slayer(1, "bears").model("bear_cub").size(1.2f, 1.2f, 0.6f).spawn(Region.FREMENNIK_PROVINCE, 3, 1, 3).spawn(Region.KOUREND, 3, 1, 3).passive(),
                m("grizzly_bear_cub", "Grizzly bear cub", 36, 35, 32, 32, 28, 1, 1).speed(4).attack(slash(4))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bear_fur"), Drop.always("bones"))
                        .slayer(1, "bears").model("grizzly_bear_cub").size(1.2f, 1.2f, 0.7f).spawn(Region.TIRANNWN, 3, 1, 3).passive(),
                m("desert_wolf", "Desert Wolf", 27, 34, 20, 21, 22, 1, 1).speed(4).attack(slash(3))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("desert_wolf").size(0.8f, 0.9f, 1.0f).spawn(Region.KHARIDIAN_DESERT, 3, 1, 3),
                m("dire_wolf", "Dire Wolf", 88, 85, 75, 75, 75, 1, 1).speed(4).attack(stab(8))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("dire_wolf").size(0.8f, 0.9f, 1.35f).spawn(Region.TIRANNWN, 3, 1, 3),
                m("jungle_wolf", "Jungle wolf", 64, 69, 50, 55, 52, 1, 1).speed(4).attack(stab(6))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("jungle_wolf").size(0.8f, 0.9f, 1.15f).spawn(Region.KARAMJA, 3, 1, 3),
                m("big_wolf", "Big Wolf", 73, 74, 60, 61, 62, 1, 1).speed(4).attack(stab(7))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("big_wolf").size(0.8f, 0.9f, 1.35f).spawn(Region.KANDARIN, 3, 1, 3),
                m("jackal", "Jackal", 21, 27, 17, 18, 15, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "dogs").model("jackal").size(0.8f, 0.9f, 0.85f).spawn(Region.KHARIDIAN_DESERT, 3, 1, 3),
                m("guard_dog", "Guard dog", 44, 49, 35, 36, 37, 1, 1).speed(4).attack(crush(5))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "dogs").model("guard_dog").size(0.8f, 0.9f, 1.0f).spawn(Region.KANDARIN, 3, 1, 3).spawn(Region.KARAMJA, 3, 1, 3).spawn(Region.KOUREND, 3, 1, 3).passive(),
                m("small_lizard", "Small Lizard", 12, 15, 10, 10, 10, 1, 1).speed(4).attack(crush(2))
                        .defenceBonus(0, 5, 5, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(13), Drop.gems(21), Drop.seeds(14), Drop.of("mystic_gloves_light", 1, 1, 512), Drop.of("fire_rune", 5, 5, 4), Drop.of("fire_rune", 42, 42, 9), Drop.of("nature_rune", 5, 5, 32), Drop.of("coal", 1, 1, 10), Drop.of("copper_ore", 1, 1, 43), Drop.of("iron_ore", 1, 1, 6), Drop.of("silver_bar", 1, 1, 64), Drop.of("silver_ore", 1, 1, 43), Drop.of("tin_ore", 1, 1, 32), Drop.of("mithril_ore", 1, 1, 128), Drop.of("kebab", 1, 1, 10))
                        .slayer(22, "lizards").model("small_lizard").size(1.1f, 0.8f, 0.6f).spawn(Region.KHARIDIAN_DESERT, 3, 1, 3).passive(),
                m("grave_scorpion", "Grave scorpion", 12, 7, 11, 12, 14, 1, 1).speed(4).attack(crush(2))
                        .defenceBonus(0, 3, 3, 0, 0).attackBonus(0, 0, 0)
                        .slayer(1, "scorpions").model("grave_scorpion").size(0.9f, 0.6f, 0.75f).spawn(Region.KANDARIN, 3, 1, 3).passive(),
                m("poison_scorpion", "Poison scorpion", 20, 23, 16, 17, 15, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(5, 15, 15, 0, 5).attackBonus(0, 0, 0)
                        .slayer(1, "scorpions").model("poison_scorpion").size(0.9f, 0.6f, 0.9f).spawn(Region.KARAMJA, 3, 1, 3).poisonous(),
                m("pit_scorpion", "Pit Scorpion", 28, 32, 23, 23, 23, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(0, 5, 5, 0, 0).attackBonus(0, 0, 0)
                        .slayer(1, "scorpions").model("pit_scorpion").size(0.9f, 0.6f, 0.9f).spawn(Region.KANDARIN, 3, 1, 3),
                m("tortured_soul", "Tortured soul", 59, 51, 52, 62, 38, 1, 1).speed(4).attack(crush(7))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.seeds(6), Drop.of("body_rune", 7, 7, 18), Drop.of("water_rune", 6, 6, 21), Drop.of("law_rune", 2, 2, 43), Drop.of("grimy_harralander", 1, 1, 26), Drop.of("grimy_tarromin", 1, 1, 43), Drop.of("grimy_marrentill", 1, 1, 64), Drop.of("grimy_guam_leaf", 1, 1, 128), Drop.of("grimy_ranarr_weed", 1, 1, 128), Drop.of("coins", 12, 12, 128), Drop.of("coins", 2, 2, 6), Drop.of("coins", 6, 6, 43), Drop.of("coins", 5, 5, 43), Drop.of("coins", 3, 3, 4), Drop.of("limpwurt_root", 1, 1, 43), Drop.of("vial_of_water", 1, 1, 128))
                        .slayer(1, "ghosts").model("tortured_soul").size(0.7f, 1.9f, 1.0f).spawn(Region.MORYTANIA, 3, 1, 3).flying().passive(),
                m("undead_chicken", "Undead chicken", 1, 3, 1, 1, 1, 1, 1).speed(4).attack(stab(0))
                        .defenceBonus(-42, -42, -42, -42, -42).attackBonus(-47, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "birds").model("undead_chicken").size(0.5f, 0.7f, 1.0f).spawn(Region.MORYTANIA, 3, 1, 3).passive(),
                m("undead_cow", "Undead cow", 2, 8, 1, 1, 1, 1, 1).speed(4).attack(crush(1))
                        .defenceBonus(-21, -21, -21, -21, -21).attackBonus(-15, 0, 0)
                        .drops(Drop.always("bones"), Drop.always("cowhide"))
                        .slayer(1, "zombies").model("undead_cow").size(1.0f, 1.4f, 1.0f).spawn(Region.MORYTANIA, 3, 1, 3).passive(),
                m("zombie_rat", "Zombie rat", 3, 5, 2, 3, 2, 1, 1).speed(4).attack(stab(1))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "rats").model("zombie_rat").size(0.9f, 0.7f, 0.75f).spawn(Region.ASGARNIA, 3, 1, 3).passive(),
                m("vampyre_juvenile", "Vampyre Juvenile", 45, 60, 40, 30, 30, 1, 1).speed(4).attack(crush(4))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .slayer(1, "vampyres").model("vampyre_juvenile").size(0.6f, 1.95f, 0.85f).spawn(Region.MORYTANIA, 3, 1, 3).passive(),
                m("vyrewatch_sentinel", "Vyrewatch Sentinel", 151, 150, 120, 160, 180, 120, 120).speed(4).attack(slash(17))
                        .defenceBonus(0, 0, 0, 0, 100).attackBonus(0, 0, 0)
                        .drops(Drop.herbs(93), Drop.rare(93), Drop.seeds(93), Drop.of("rune_dagger", 1, 1, 16), Drop.of("adamant_platelegs", 1, 1, 16), Drop.of("adamant_platebody", 1, 1, 23), Drop.of("rune_full_helm", 1, 1, 93), Drop.of("rune_kiteshield", 1, 1, 93), Drop.of("death_rune", 6, 10, 9), Drop.of("blood_rune", 8, 16, 9), Drop.of("nature_rune", 6, 11, 9), Drop.of("rune_arrow", 4, 10, 23), Drop.of("rune_javelin", 5, 15, 46), Drop.of("opal_bolt_tips", 6, 14, 579), Drop.of("jade_bolt_tips", 6, 14, 1736), Drop.of("topaz_bolt_tips", 6, 14, 1736), Drop.of("pearl_bolt_tips", 6, 14, 579), Drop.of("sapphire_bolt_tips", 6, 14, 1736), Drop.of("emerald_bolt_tips", 6, 14, 1042), Drop.of("ruby_bolt_tips", 6, 14, 868), Drop.of("diamond_bolt_tips", 6, 14, 579), Drop.of("dragonstone_bolt_tips", 6, 14, 868), Drop.of("onyx_bolt_tips", 6, 14, 1736), Drop.of("bark", 4, 8, 23), Drop.of("coal", 8, 8, 23), Drop.of("runite_bar", 1, 1, 46), Drop.of("yew_logs", 6, 6, 46), Drop.of("runite_ore", 1, 1, 46), Drop.of("coins", 100, 1000, 4))
                        .slayer(1, "vampyres").model("vyrewatch_sentinel").size(0.6f, 1.95f, 1.1f).spawn(Region.MORYTANIA, 3, 1, 3),
                m("elf_archer", "Elf Archer", 90, 105, 10, 10, 80, 1, 90).speed(4).attack(ranged(11, "minecraft:crit"))
                        .defenceBonus(50, 50, 50, 60, 70).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(9), Drop.gems(26), Drop.of("green_dhide_chaps", 1, 1, 43), Drop.of("green_dhide_body", 1, 1, 32), Drop.of("mithril_spear", 1, 1, 64), Drop.of("mithril_kiteshield", 1, 1, 128), Drop.of("adamant_full_helm", 1, 1, 128), Drop.of("rune_dagger", 1, 1, 128), Drop.of("water_rune", 70, 70, 16), Drop.of("nature_rune", 12, 12, 26), Drop.of("law_rune", 2, 2, 43), Drop.of("fire_rune", 37, 37, 43), Drop.of("coins", 20, 20, 26), Drop.of("coins", 44, 44, 4), Drop.of("coins", 180, 180, 13), Drop.of("bass", 1, 1, 43), Drop.of("shark", 1, 1, 43), Drop.of("adamantite_ore", 1, 1, 128))
                        .slayer(1, "elves").model("elf_archer").size(0.7f, 2.0f, 1.0f).spawn(Region.TIRANNWN, 3, 1, 3).passive(),
                m("iorwerth_archer", "Iorwerth Archer", 90, 105, 10, 10, 80, 1, 90).speed(4).attack(ranged(11, "minecraft:crit"))
                        .defenceBonus(50, 50, 50, 60, 70).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(9), Drop.gems(26), Drop.seeds(8), Drop.of("green_dhide_chaps", 1, 1, 43), Drop.of("green_dhide_body", 1, 1, 32), Drop.of("mithril_spear", 1, 1, 64), Drop.of("mithril_kiteshield", 1, 1, 128), Drop.of("adamant_full_helm", 1, 1, 128), Drop.of("rune_dagger", 1, 1, 128), Drop.of("water_rune", 70, 70, 16), Drop.of("nature_rune", 12, 12, 26), Drop.of("law_rune", 2, 2, 43), Drop.of("fire_rune", 37, 37, 64), Drop.of("coins", 20, 20, 26), Drop.of("coins", 44, 44, 4), Drop.of("coins", 132, 132, 16), Drop.of("coins", 180, 180, 13), Drop.of("coins", 440, 440, 128), Drop.of("bass", 1, 1, 43), Drop.of("shark", 1, 1, 43), Drop.of("adamantite_ore", 1, 1, 64))
                        .slayer(1, "elves").model("iorwerth_archer").size(0.7f, 2.0f, 1.0f).spawn(Region.TIRANNWN, 3, 1, 3).passive(),
                m("guard_bandit", "Guard Bandit", 22, 27, 17, 17, 17, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(4, 8, 5, 1, 4).attackBonus(11, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "bandits").model("guard_bandit").size(0.6f, 1.95f, 1.0f).spawn(Region.WILDERNESS, 3, 1, 3),
                m("thrower_troll", "Thrower Troll", 67, 95, 30, 80, 30, 1, 60).speed(7).attack(ranged(6, "minecraft:crit"))
                        .defenceBonus(0, 0, 0, 200, 120).attackBonus(0, 0, 0)
                        .drops(Drop.always("big_bones"), Drop.herbs(9), Drop.gems(26), Drop.seeds(7), Drop.of("steel_med_helm", 1, 1, 32), Drop.of("black_warhammer", 1, 1, 43), Drop.of("steel_warhammer", 1, 1, 43), Drop.of("adamant_med_helm", 1, 1, 128), Drop.of("adamant_warhammer", 1, 1, 128), Drop.of("mithril_sq_shield", 1, 1, 128), Drop.of("earth_rune", 60, 60, 16), Drop.of("nature_rune", 7, 7, 26), Drop.of("law_rune", 2, 2, 43), Drop.of("earth_rune", 45, 45, 128), Drop.of("earth_rune", 25, 25, 128), Drop.of("coins", 35, 35, 4), Drop.of("coins", 100, 100, 13), Drop.of("coins", 8, 8, 18), Drop.of("coins", 50, 50, 21), Drop.of("coins", 250, 250, 128), Drop.of("coal", 3, 3, 43), Drop.of("raw_mackerel", 3, 3, 64), Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012))
                        .slayer(1, "trolls").model("thrower_troll").size(1.0f, 2.6f, 0.95f).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                m("troll_spectator", "Troll spectator", 71, 90, 40, 90, 25, 1, 1).speed(6).attack(crush(13))
                        .defenceBonus(0, 0, 10, 200, 200).attackBonus(20, 0, 0)
                        .drops(Drop.always("big_bones"), Drop.herbs(9), Drop.gems(26), Drop.seeds(7), Drop.of("steel_med_helm", 1, 1, 32), Drop.of("black_warhammer", 1, 1, 43), Drop.of("steel_warhammer", 1, 1, 43), Drop.of("adamant_med_helm", 1, 1, 128), Drop.of("adamant_warhammer", 1, 1, 128), Drop.of("mithril_sq_shield", 1, 1, 128), Drop.of("earth_rune", 60, 60, 16), Drop.of("nature_rune", 7, 7, 26), Drop.of("law_rune", 2, 2, 43), Drop.of("earth_rune", 45, 45, 128), Drop.of("earth_rune", 25, 25, 128), Drop.of("coins", 35, 35, 4), Drop.of("coins", 100, 100, 13), Drop.of("coins", 8, 8, 18), Drop.of("coins", 50, 50, 21), Drop.of("coins", 250, 250, 128), Drop.of("coal", 3, 3, 43), Drop.of("raw_mackerel", 3, 3, 64), Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012))
                        .slayer(1, "trolls").model("troll_spectator").size(1.0f, 2.6f, 1.0f).spawn(Region.TROLL_COUNTRY, 3, 1, 3).passive(),
                m("sorebones", "Sorebones", 57, 52, 49, 50, 50, 1, 1).speed(4).attack(crush(6))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(4), Drop.gems(128), Drop.of("steel_bolts", 2, 12, 43), Drop.of("mithril_dagger", 1, 1, 64), Drop.of("mithril_scimitar", 1, 1, 43), Drop.of("water_rune", 23, 23, 128), Drop.of("body_rune", 6, 6, 64), Drop.of("chaos_rune", 7, 7, 128), Drop.of("cosmic_rune", 5, 5, 43), Drop.of("law_rune", 2, 2, 43), Drop.of("coins", 30, 30, 13), Drop.of("coins", 38, 38, 6), Drop.of("coins", 46, 46, 16), Drop.of("coins", 55, 55, 21), Drop.of("coins", 60, 60, 64), Drop.of("fishing_bait", 10, 10, 6))
                        .slayer(1, "zombies").model("sorebones").size(0.6f, 1.95f, 1.0f).spawn(Region.UNCHARTED_ISLES, 3, 1, 3).passive(),
                m("zombie_pirate", "Zombie pirate", 57, 52, 49, 50, 50, 1, 1).speed(4).attack(crush(6))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(4), Drop.gems(128), Drop.of("steel_bolts", 2, 12, 43), Drop.of("mithril_dagger", 1, 1, 64), Drop.of("mithril_scimitar", 1, 1, 43), Drop.of("water_rune", 23, 23, 128), Drop.of("body_rune", 6, 6, 64), Drop.of("chaos_rune", 7, 7, 128), Drop.of("cosmic_rune", 5, 5, 43), Drop.of("law_rune", 2, 2, 43), Drop.of("coins", 30, 30, 13), Drop.of("coins", 38, 38, 6), Drop.of("coins", 46, 46, 16), Drop.of("coins", 55, 55, 21), Drop.of("coins", 60, 60, 64), Drop.of("fishing_bait", 10, 10, 6))
                        .slayer(1, "zombies").model("zombie_pirate").size(0.6f, 1.95f, 1.0f).spawn(Region.UNCHARTED_ISLES, 3, 1, 3),
                m("undead_one", "Undead one", 73, 59, 65, 65, 65, 1, 1).speed(4).attack(slash(8))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(4, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "zombies").model("undead_one").size(0.6f, 1.95f, 1.05f).spawn(Region.KARAMJA, 3, 1, 3),
                m("zogre", "Zogre", 44, 71, 20, 36, 35, 1, 1).speed(6).attack(crush(5))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .slayer(1, "zombies").model("zogre").size(1.0f, 2.6f, 1.0f).spawn(Region.FELDIP_HILLS, 3, 1, 3),
                m("seagull", "Seagull", 2, 6, 1, 1, 1, 1, 1).speed(4).attack(crush(0))
                        .defenceBonus(-42, -42, -42, -42, -42).attackBonus(-47, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "birds").model("seagull").size(0.6f, 0.9f, 0.8f).spawn(Region.ASGARNIA, 3, 1, 3).flying().passive(),
                m("black_guard", "Black Guard", 25, 30, 20, 20, 20, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(3, 4, 4, 2, 3).attackBonus(5, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(64), Drop.gems(128), Drop.of("bronze_pickaxe", 1, 1, 10), Drop.of("bronze_med_helm", 1, 1, 32), Drop.of("bronze_battleaxe", 1, 1, 64), Drop.of("iron_battleaxe", 1, 1, 128), Drop.of("chaos_rune", 2, 2, 32), Drop.of("nature_rune", 2, 2, 32), Drop.of("coins", 4, 4, 6), Drop.of("coins", 10, 10, 9), Drop.of("coins", 30, 30, 64), Drop.of("hammer", 1, 1, 13), Drop.of("bronze_bar", 1, 1, 18), Drop.of("iron_ore", 1, 1, 32), Drop.of("tin_ore", 1, 1, 32), Drop.of("copper_ore", 1, 1, 43), Drop.of("iron_bar", 1, 1, 43), Drop.of("coal", 1, 1, 64), Drop.of("bronze_warhammer", 1, 1, 43), Drop.of("iron_warhammer", 1, 1, 21), Drop.of("steel_warhammer", 1, 1, 128), Drop.of("black_warhammer", 1, 1, 64), Drop.of("copper_ore", 5, 5, 64), Drop.of("iron_ore", 2, 2, 43), Drop.of("coins", 15, 15, 3), Drop.of("coins", 17, 17, 26), Drop.of("coins", 22, 22, 14), Drop.of("coins", 27, 27, 43), Drop.of("beer", 1, 1, 128), Drop.of("cake", 1, 1, 32), Drop.of("ring_mould", 1, 1, 128))
                        .slayer(1, "dwarves").model("black_guard").size(0.6f, 1.4f, 1.0f).spawn(Region.ASGARNIA, 3, 1, 3).passive(),
                m("pyrelord", "Pyrelord", 60, 80, 60, 40, 30, 1, 1).speed(4).attack(crush(5))
                        .defenceBonus(10, 10, 10, 0, 10).attackBonus(0, 0, 0)
                        .drops(Drop.always("fiendish_ashes"), Drop.gems(43), Drop.of("mithril_axe", 1, 1, 32), Drop.of("mithril_full_helm", 1, 1, 32), Drop.of("staff_of_fire", 1, 1, 43), Drop.of("mithril_chainbody", 1, 1, 64), Drop.of("adamant_med_helm", 1, 1, 128), Drop.of("steel_boots", 1, 1, 128), Drop.of("fire_rune", 50, 50, 6), Drop.of("fire_rune", 100, 100, 16), Drop.of("chaos_rune", 20, 20, 26), Drop.of("death_rune", 5, 5, 43), Drop.of("coins", 10, 10, 18), Drop.of("coins", 60, 60, 5), Drop.of("coins", 200, 200, 6), Drop.of("coins", 400, 400, 13), Drop.of("coins", 600, 600, 64), Drop.of("gold_ore", 2, 2, 16), Drop.of("jug_of_wine", 1, 1, 64))
                        .slayer(30, "pyrefiends").model("pyrelord").size(0.7f, 1.4f, 1.15f).spawn(Region.UNCHARTED_ISLES, 3, 1, 3).flying().passive());
    }

    /** Recoloured look-alikes of existing models. */
    static List<ModelSpec.Variant> variants() {
        return List.of(
                new ModelSpec.Variant("black_bear", "grizzly_bear", 1.0f, c -> mix(c, 0x1E1A18, 0.7f)),
                new ModelSpec.Variant("bear_cub", "grizzly_bear", 1.0f, c -> mix(c, 0x7A5434, 0.3f)),
                ModelSpec.Variant.of("grizzly_bear_cub", "grizzly_bear"),
                new ModelSpec.Variant("desert_wolf", "wolf", 1.0f, c -> mix(c, 0xC8A870, 0.55f)),
                new ModelSpec.Variant("dire_wolf", "wolf", 1.0f, c -> mix(c, 0x3A3A3E, 0.55f)),
                new ModelSpec.Variant("jungle_wolf", "wolf", 1.0f, c -> mix(c, 0x5A4A2A, 0.5f)),
                new ModelSpec.Variant("big_wolf", "wolf", 1.0f, c -> mix(c, 0xE8E8E8, 0.5f)),
                new ModelSpec.Variant("jackal", "wild_dog", 1.0f, c -> mix(c, 0xC09A5A, 0.55f)),
                new ModelSpec.Variant("guard_dog", "wild_dog", 1.0f, c -> mix(c, 0x2A2422, 0.55f)),
                ModelSpec.Variant.of("small_lizard", "lizard"),
                new ModelSpec.Variant("grave_scorpion", "scorpion", 1.0f, c -> mix(c, 0x8A8A80, 0.5f)),
                new ModelSpec.Variant("poison_scorpion", "scorpion", 1.0f, c -> mix(c, 0x4A7A2A, 0.5f)),
                new ModelSpec.Variant("pit_scorpion", "scorpion", 1.0f, c -> mix(c, 0x6A2A1A, 0.45f)),
                new ModelSpec.Variant("tortured_soul", "ghost", 1.0f, c -> mix(c, 0x7AA088, 0.4f)),
                new ModelSpec.Variant("undead_chicken", "chicken", 1.0f, c -> mix(c, 0x8A9A7A, 0.5f)),
                new ModelSpec.Variant("undead_cow", "cow", 1.0f, c -> mix(c, 0x7A8A6A, 0.5f)),
                new ModelSpec.Variant("zombie_rat", "giant_rat", 1.0f, c -> mix(c, 0x6A8A5A, 0.45f)),
                new ModelSpec.Variant("vampyre_juvenile", "gwd_vampire", 1.0f, c -> mix(c, 0x6A6A7A, 0.3f)),
                new ModelSpec.Variant("vyrewatch_sentinel", "gwd_vampire", 1.0f, c -> mix(c, 0x3A1018, 0.4f)),
                new ModelSpec.Variant("elf_archer", "elf_warrior", 1.0f, c -> mix(c, 0x3A6A3A, 0.25f)),
                new ModelSpec.Variant("iorwerth_archer", "elf_warrior", 1.0f, c -> mix(c, 0x2A4A6A, 0.25f)),
                new ModelSpec.Variant("guard_bandit", "bandit", 1.0f, c -> mix(c, 0x5A4A3A, 0.25f)),
                new ModelSpec.Variant("thrower_troll", "mountain_troll", 1.0f, c -> mix(c, 0x7A6A5A, 0.3f)),
                new ModelSpec.Variant("troll_spectator", "mountain_troll", 1.0f, c -> mix(c, 0x5A6A4A, 0.3f)),
                new ModelSpec.Variant("sorebones", "zombie", 1.0f, c -> mix(c, 0xB8B0A0, 0.4f)),
                new ModelSpec.Variant("zombie_pirate", "pirate", 1.0f, c -> mix(c, 0x6A7A5A, 0.45f)),
                new ModelSpec.Variant("undead_one", "zombie", 1.0f, c -> mix(c, 0xA8A890, 0.35f)),
                new ModelSpec.Variant("zogre", "ogre", 1.0f, c -> mix(c, 0x6A7A4A, 0.5f)),
                new ModelSpec.Variant("seagull", "entranan_firebird", 1.0f, c -> mix(c, 0xE8E8E8, 0.7f)),
                new ModelSpec.Variant("black_guard", "dwarf", 1.0f, c -> mix(c, 0x2A2A2A, 0.5f)),
                new ModelSpec.Variant("pyrelord", "pyrefiend", 1.0f, c -> mix(c, 0xF0A030, 0.25f)));
    }

    private static int mix(int rgb, int target, float amount) {
        int r = Math.round(((rgb >> 16) & 0xFF) * (1 - amount) + ((target >> 16) & 0xFF) * amount);
        int g = Math.round(((rgb >> 8) & 0xFF) * (1 - amount) + ((target >> 8) & 0xFF) * amount);
        int b = Math.round((rgb & 0xFF) * (1 - amount) + (target & 0xFF) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
