package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.crush;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Slayer task monsters still missing after round 16 (round 17 monster completeness, part 2): creatures of the wiki's
 * "Slayer task/..." tables whose homes are dungeons or post-snapshot Varlamore content. Stats from the wiki's DPS
 * calculator data (weirdgloop/osrs-dps-calc {@code monsters.json}; Rock from his infobox), drops, aggression, venom and
 * Slayer levels from the wiki pages (Kiyogitpy/osrs-wiki-data, July 2026). Drop items RuneCraft doesn't have (giant bat
 * wings, rat bones, wyrmling bones, araxyte venom sacs, troll bones, broken antlers, cannonballs...) are left out; the
 * {@code DropTable} templates become {@link Drop#herbs}/{@link Drop#gems}/... at the wiki's rates. Attributes, size,
 * poison/venom immunity and Slayer XP come from {@code MonsterTraits}.
 *
 * <p>Where they spawn: the Barrows tunnels (crypt rats and spiders; the bloodworms beside them are the dungeons module's), the Mos Le'Harmless caves (albino bats),
 * the Karuulm Slayer Dungeon (sulphur lizards) and the Morytania Spider Cave (araxytes) are dungeon camps (dungeons
 * module); feral vampyres, ice wolves, the Death Plateau trolls and Fossil Island's ancient zygomites are cache/wiki-tile
 * spawn points in {@code SpawnTable}; the Varlamore creatures wait at the entrances of their unbuilt dungeons (Neypotzli,
 * the Ruins of Tapoyauik, the Tonali Cavern, the Stalker Den) as zeah-module camps, like the frost nagua.
 */
final class SlayerTaskMonsters2 {
    private SlayerTaskMonsters2() {
    }

    private static final Drop[] TROLL = {Drop.always("big_bones"), Drop.herbs(9), Drop.gems(26),
            Drop.of("steel_platebody", 1, 1, 32), Drop.of("black_warhammer", 1, 1, 43), Drop.of("steel_warhammer", 1, 1, 43),
            Drop.of("adamant_axe", 1, 1, 64), Drop.of("adamant_sq_shield", 1, 1, 128), Drop.of("mithril_platebody", 1, 1, 128),
            Drop.of("rune_warhammer", 1, 1, 128), Drop.of("earth_rune", 80, 80, 16), Drop.of("nature_rune", 16, 16, 26),
            Drop.of("law_rune", 4, 4, 43), Drop.of("earth_rune", 65, 65, 128), Drop.of("earth_rune", 25, 25, 128),
            Drop.of("coins", 40, 40, 4), Drop.of("coins", 135, 135, 5), Drop.of("coins", 190, 190, 13), Drop.of("coins", 20, 20, 32),
            Drop.of("coins", 420, 420, 128), Drop.of("coal", 6, 6, 43), Drop.of("raw_tuna", 4, 4, 64),
            Drop.of("ensouled_troll_head", 1, 1, 28), Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012)};

    /** Araxyte drops; the level 146 araxytes roll the same table with doubled quantities. */
    private static Drop[] araxyte(int k) {
        return new Drop[]{Drop.herbs(13), Drop.uncommonSeeds(13),
                Drop.of("adamant_longsword", k, k, 25), Drop.of("adamant_battleaxe", k, k, 25), Drop.of("rune_dagger", k, k, 42),
                Drop.of("rune_med_helm", k, k, 64), Drop.of("rune_platelegs", k, k, 64), Drop.of("air_rune", 120 * k, 140 * k, 13),
                Drop.of("water_rune", 120 * k, 140 * k, 13), Drop.of("earth_rune", 120 * k, 140 * k, 13),
                Drop.of("fire_rune", 120 * k, 140 * k, 13), Drop.of("cosmic_rune", 7 * k, 12 * k, 25),
                Drop.of("chaos_rune", 10 * k, 15 * k, 25), Drop.of("nature_rune", 15 * k, 20 * k, 25),
                Drop.of("death_rune", 20 * k, 25 * k, 25), Drop.of("law_rune", 12 * k, 15 * k, 25),
                Drop.of("blood_rune", 15 * k, 18 * k, 25), Drop.of("soul_rune", 9 * k, 12 * k, 25),
                Drop.of("coins", 800 * k, 1200 * k, 13), Drop.of("araxyte_head", 1, 1, 2000)};
    }

    /** Custodian stalker drops (the three tiers share the table's shape, with larger stacks higher up). */
    private static Drop[] stalker(int rare, int gems, Drop... table) {
        List<Drop> out = new ArrayList<>(List.of(Drop.always("big_bones"), Drop.rare(rare), Drop.gems(gems),
                Drop.of("long_bone", 1, 1, 400), Drop.of("curved_bone", 1, 1, 5012)));
        out.addAll(List.of(table));
        return out.toArray(new Drop[0]);
    }

    static List<MonsterDef> all() {
        return List.of(
                // --- Bats: the Mos Le'Harmless caves
                m("albino_bat", "Albino bat", 52, 33, 57, 57, 30, 1, 1).speed(4).attack(stab(7))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0).weakness("air", 40)
                        .drops(Drop.always("bat_bones"))
                        .slayer(1, "bats").model("albino_bat").size(1.0f, 0.8f, 1.2f).spawn(Region.MORYTANIA, 3, 1, 3).flying().underground(),
                // --- The Barrows tunnels (aggressive to everyone; bones or nothing)
                m("crypt_rat", "Crypt rat", 43, 35, 70, 20, 20, 1, 1).speed(4).attack(stab(3))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "rats").model("crypt_rat").size(0.9f, 0.7f, 0.85f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                m("giant_crypt_rat", "Giant crypt rat", 76, 70, 80, 50, 65, 1, 1).speed(4).attack(stab(6))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "rats").model("giant_crypt_rat").size(1.2f, 0.9f, 1.2f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                m("crypt_spider", "Crypt spider", 56, 60, 45, 47, 45, 1, 1).speed(4).attack(stab(6))
                        .defenceBonus(20, 20, 10, 17, 20).attackBonus(0, 0, 0).weakness("fire", 50)
                        .slayer(1, "spiders").model("crypt_spider").size(1.0f, 0.7f, 0.9f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                m("giant_crypt_spider", "Giant crypt spider", 79, 80, 65, 67, 65, 1, 1).speed(4).attack(stab(8))
                        .defenceBonus(20, 20, 10, 17, 20).attackBonus(0, 0, 0).weakness("fire", 40)
                        .slayer(1, "spiders").model("giant_crypt_spider").size(1.2f, 0.8f, 1.1f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                // --- Lizards: the Karuulm Slayer Dungeon (44 Slayer; no ice cooler needed)
                m("sulphur_lizard", "Sulphur Lizard", 50, 50, 50, 45, 30, 1, 1).speed(4).attack(crush(5))
                        .defenceBonus(15, 25, 25, 0, 15).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(13), Drop.seeds(14), Drop.gems(21), Drop.of("mystic_gloves_light", 1, 1, 512),
                                Drop.of("fire_rune", 10, 25, 5), Drop.of("fire_rune", 40, 60, 32), Drop.of("nature_rune", 5, 10, 32),
                                Drop.of("iron_ore", 5, 10, 6), Drop.of("coal", 5, 10, 10), Drop.of("iron_bar", 6, 10, 32),
                                Drop.of("steel_bar", 3, 5, 32), Drop.of("tin_ore", 10, 15, 32), Drop.of("copper_ore", 10, 15, 43),
                                Drop.of("silver_ore", 5, 10, 43), Drop.of("silver_bar", 3, 5, 64), Drop.of("mithril_ore", 3, 5, 128))
                        .slayer(44, "lizards").model("sulphur_lizard").size(1.1f, 0.8f, 0.9f).spawn(Region.KOUREND, 3, 1, 3).underground(),
                // --- Araxytes: the Morytania Spider Cave (92 Slayer, venomous; also count as spiders)
                m("araxyte", "Araxyte", 96, 60, 80, 125, 60, 60, 1).speed(4)
                        .attack(crush(13).special(AttackDef.Special.VENOM, 0))
                        .defenceBonus(60, 30, 20, 20, 20).attackBonus(0, 0, 0).weakness("fire", 50)
                        .drops(araxyte(1))
                        .slayer(92, "araxytes").model("araxyte").size(0.9f, 0.8f, 1.0f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                m("araxyte_lvl146", "Araxyte", 146, 100, 160, 160, 70, 80, 1).speed(4)
                        .attack(crush(17).special(AttackDef.Special.VENOM, 0))
                        .defenceBonus(60, 30, 20, 20, 20).attackBonus(0, 0, 0).weakness("fire", 50)
                        .drops(araxyte(2))
                        .slayer(92, "araxytes").model("araxyte_lvl146").size(1.4f, 1.1f, 1.3f).spawn(Region.MORYTANIA, 3, 1, 3).underground(),
                // --- Vampyres: west of Burgh de Rott and the Haunted Woods
                m("feral_vampyre", "Feral Vampyre", 61, 40, 55, 60, 55, 40, 1).speed(4).attack(stab(7))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .drops(Drop.herbs(13), Drop.uncommonSeeds(7), Drop.gems(32), Drop.of("earth_rune", 4, 4, 13),
                                Drop.of("death_rune", 2, 2, 13), Drop.of("chaos_rune", 3, 3, 16), Drop.of("blood_rune", 1, 1, 26),
                                Drop.of("blood_rune", 2, 2, 128), Drop.of("coins", 15, 15, 3), Drop.of("black_axe", 1, 1, 43),
                                Drop.of("earth_talisman", 1, 1, 64))
                        .slayer(1, "vampyres").model("feral_vampyre").size(0.6f, 1.95f, 1.0f).spawn(Region.MORYTANIA, 3, 1, 3),
                // --- Wolves: Trollweiss Mountain (level 96), the Ice Path, Weiss and the Settlement Ruins (level 132)
                m("ice_wolf", "Ice wolf", 96, 70, 100, 90, 70, 1, 1).speed(4).attack(stab(10))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0).weakness("fire", 20)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("ice_wolf").size(0.8f, 0.9f, 1.3f).spawn(Region.FREMENNIK_PROVINCE, 3, 1, 3).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                m("ice_wolf_lvl132", "Ice wolf", 132, 70, 150, 150, 70, 1, 1).speed(4).attack(stab(16))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0).weakness("fire", 20)
                        .drops(Drop.always("wolf_bones"))
                        .slayer(1, "wolves").model("ice_wolf_lvl132").size(0.8f, 0.9f, 1.4f).spawn(Region.TROLL_COUNTRY, 3, 1, 3).spawn(Region.FREMENNIK_PROVINCE, 3, 1, 3).spawn(Region.KOUREND, 3, 1, 3),
                // --- Trolls: the named trolls of Death Plateau
                m("pee_hat", "Pee Hat", 91, 120, 50, 100, 50, 0, 0).speed(6).attack(crush(23))
                        .defenceBonus(25, 25, 40, 200, 200).attackBonus(40, 0, 0).weakness("earth", 20)
                        .drops(TROLL)
                        .slayer(1, "trolls").model("pee_hat").size(1.0f, 2.6f, 1.05f).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                m("kraka", "Kraka", 91, 120, 50, 100, 50, 0, 0).speed(6).attack(crush(23))
                        .defenceBonus(25, 25, 40, 200, 200).attackBonus(40, 0, 0).weakness("earth", 20)
                        .drops(TROLL)
                        .slayer(1, "trolls").model("kraka").size(1.0f, 2.6f, 1.05f).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                m("troll_stick", "Stick", 104, 135, 60, 110, 60, 0, 0).speed(6).attack(crush(27))
                        .defenceBonus(30, 30, 50, 200, 200).attackBonus(50, 0, 0).weakness("earth", 20)
                        .drops(TROLL)
                        .slayer(1, "trolls").model("troll_stick").size(1.0f, 2.6f, 1.1f).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                // Rock: not in the DPS calculator data; infobox stats (speed 4, slash, max hit 30).
                m("troll_rock", "Rock", 111, 140, 70, 110, 70, 0, 0).speed(4).attack(slash(30))
                        .defenceBonus(35, 60, 35, 200, 200).attackBonus(60, 0, 0) // approx: ranged defence from the other trolls
                        .drops(TROLL)
                        .slayer(1, "trolls").model("troll_rock").size(1.0f, 2.6f, 1.15f).spawn(Region.TROLL_COUNTRY, 3, 1, 3),
                // --- Skogres at Jiggig (skeletons and ogres) and the members' roosters (birds)
                m("skogre", "Skogre", 44, 71, 20, 36, 35, 1, 1).speed(6).attack(crush(5))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0)
                        .slayer(1, "skeletons").model("skogre").size(1.0f, 2.6f, 1.0f).spawn(Region.FELDIP_HILLS, 3, 1, 3),
                m("rooster", "Rooster", 3, 7, 2, 2, 1, 1, 1).speed(4).attack(stab(1))
                        .defenceBonus(-42, -42, -42, -42, -42).attackBonus(-20, 0, 0)
                        .drops(Drop.always("bones"))
                        .slayer(1, "birds").model("rooster").size(0.5f, 0.7f, 1.1f).spawn(Region.FREMENNIK_PROVINCE, 3, 1, 3).spawn(Region.TROLL_COUNTRY, 3, 1, 3).passive(),
                // --- Mutated zygomites: Fossil Island's ancient zygomites (57 Slayer; magical melee and ranged)
                m("ancient_zygomite", "Ancient Zygomite", 109, 150, 80, 80, 80, 80, 80).speed(4)
                        .attack(AttackDef.of("melee", AttackType.MAGIC, 9, 2.5, 10))
                        .attack(AttackDef.of("spores", AttackType.MAGIC, 10, 7, 5).particle("minecraft:spore_blossom_air"))
                        .defenceBonus(20, 20, 20, 30, 30).attackBonus(50, 0, 0).weakness("fire", 40)
                        .drops(Drop.always("mort_myre_fungus"), Drop.treeHerbSeeds(118), Drop.gems(20), Drop.of("rune_axe", 1, 1, 39),
                                Drop.of("earth_rune", 100, 100, 7), Drop.of("law_rune", 15, 15, 20), Drop.of("cosmic_rune", 15, 15, 20),
                                Drop.of("nature_rune", 15, 15, 20), Drop.of("grimy_torstol", 1, 1, 20), Drop.of("mushroom_spore", 1, 1, 12),
                                Drop.of("calcite", 1, 1, 7), Drop.of("supercompost", 2, 2, 9), Drop.of("mort_myre_fungus", 5, 5, 12),
                                Drop.of("volcanic_ash", 2, 2, 37))
                        .slayer(57, "mutated_zygomites").model("ancient_zygomite").size(1.1f, 1.8f, 1.2f).spawn(Region.FOSSIL_ISLAND, 3, 1, 3).passive(),
                // --- Varlamore (stand-ins at their dungeons' entrances, zeah module camps)
                m("wyrmling", "Wyrmling", 55, 65, 20, 30, 40, 60, 15).speed(4).attack(slash(5))
                        .defenceBonus(20, 50, 50, 50, 20).attackBonus(0, 10, 0).weakness("earth", 50)
                        .slayer(62, "wyrms").model("wyrmling").size(1.0f, 1.0f, 0.7f).spawn(Region.VARLAMORE, 3, 1, 3).passive(),
                m("grimy_lizard", "Grimy Lizard", 50, 75, 65, 25, 10, 25, 1).speed(4)
                        .attack(crush(3).special(AttackDef.Special.POISON, 3))
                        .defenceBonus(35, 15, 50, 50, 35).attackBonus(0, 0, 0)
                        .drops(Drop.always("bones"), Drop.herbs(13), Drop.rareSeeds(26), Drop.seeds(4), Drop.gems(32),
                                Drop.of("mystic_gloves_light", 1, 1, 512), Drop.of("earth_rune", 10, 25, 5), Drop.of("nature_rune", 5, 10, 32),
                                Drop.of("water_rune", 40, 60, 32), Drop.of("mahogany_seed", 1, 3, 128), Drop.of("teak_seed", 1, 3, 43),
                                Drop.of("earth_talisman", 1, 1, 128), Drop.of("water_talisman", 1, 1, 128), Drop.of("oak_roots", 1, 3, 10))
                        .slayer(44, "lizards").model("grimy_lizard").size(1.1f, 0.8f, 0.9f).spawn(Region.VARLAMORE, 3, 1, 3).poisonous(),
                m("frost_crab", "Frost Crab", 15, 60, 1, 1, 1, 1, 1).speed(4).attack(crush(1))
                        .defenceBonus(0, 0, 0, -50, 0).attackBonus(0, 0, 0).weakness("fire", 100)
                        .drops(Drop.gems(90), Drop.of("bronze_pickaxe", 1, 1, 15), Drop.of("iron_pickaxe", 1, 1, 18),
                                Drop.of("coins", 2, 2, 11), Drop.of("coins", 4, 4, 5), Drop.of("coins", 8, 8, 22), Drop.of("coins", 24, 24, 15),
                                Drop.of("coins", 36, 36, 16), Drop.of("water_rune", 1, 1, 26), Drop.of("water_rune", 2, 2, 26),
                                Drop.of("water_rune", 5, 5, 45), Drop.of("water_rune", 10, 10, 45), Drop.of("water_rune", 15, 15, 45),
                                Drop.of("tin_ore", 3, 3, 22), Drop.of("copper_ore", 3, 3, 45), Drop.of("iron_ore", 1, 1, 45),
                                Drop.of("iron_ore", 2, 2, 90), Drop.of("coal", 1, 1, 90), Drop.of("coal", 2, 2, 45), Drop.of("coal", 3, 3, 90),
                                Drop.of("chisel", 1, 1, 30), Drop.of("casket", 1, 1, 90), Drop.of("soft_clay", 1, 1, 45),
                                Drop.of("frozen_tear", 5, 5, 90))
                        .slayer(1, "crabs").model("frost_crab").size(0.9f, 0.6f, 0.9f).spawn(Region.VARLAMORE, 3, 1, 3),
                m("chilled_jelly", "Chilled jelly", 112, 140, 95, 90, 70, 95, 1).speed(4)
                        .attack(AttackDef.of("melee", AttackType.MAGIC, 10, 2.5, 10))
                        .defenceBonus(0, 0, 0, 0, 0).attackBonus(0, 0, 0).weakness("fire", 50)
                        .drops(Drop.gems(32), Drop.of("adamant_battleaxe", 1, 1, 12), Drop.of("black_2h_sword", 1, 1, 26),
                                Drop.of("adamant_axe", 1, 1, 43), Drop.of("adamant_2h_sword", 1, 1, 64), Drop.of("mithril_boots", 1, 1, 64),
                                Drop.of("rune_kiteshield", 1, 1, 64), Drop.of("rune_full_helm", 1, 1, 128), Drop.of("chaos_rune", 45, 45, 26),
                                Drop.of("death_rune", 15, 15, 43), Drop.of("coins", 44, 44, 5), Drop.of("coins", 102, 102, 5),
                                Drop.of("coins", 220, 220, 14), Drop.of("coins", 11, 11, 21), Drop.of("coins", 460, 460, 64),
                                Drop.of("lobster", 2, 2, 8), Drop.of("gold_bar", 1, 1, 64), Drop.of("thread", 10, 10, 128))
                        .slayer(52, "jellies").model("chilled_jelly").size(1.0f, 1.2f, 1.05f).spawn(Region.VARLAMORE, 3, 1, 3).passive(),
                m("earthen_nagua", "Earthen Nagua", 128, 160, 120, 120, 40, 40, 1).speed(4)
                        .attack(AttackDef.of("melee", AttackType.STAB, 13, 2.5, 10)) // two hits of 7 + 6 in OSRS
                        .defenceBonus(30, 50, 50, 60, 150).attackBonus(100, 0, 0).weakness("air", 15)
                        .drops(Drop.gems(32), Drop.of("chaos_rune", 8, 20, 11), Drop.of("nature_rune", 8, 15, 16),
                                Drop.of("death_rune", 8, 13, 16), Drop.of("earth_rune", 10, 19, 17), Drop.of("earth_rune", 20, 30, 11),
                                Drop.of("earth_rune", 31, 50, 25), Drop.of("iron_ore", 8, 15, 8), Drop.of("coal", 8, 15, 11),
                                Drop.of("gold_ore", 4, 8, 16), Drop.of("adamantite_ore", 1, 2, 16), Drop.of("soft_clay", 5, 10, 11),
                                Drop.of("earth_orb", 1, 2, 32), Drop.of("earth_talisman", 1, 1, 32))
                        .slayer(48, "lesser_nagua").model("earthen_nagua").size(1.0f, 2.2f, 1.1f).spawn(Region.VARLAMORE, 3, 1, 3),
                m("juvenile_custodian_stalker", "Juvenile custodian stalker", 93, 135, 95, 55, 45, 25, 1).speed(4).attack(crush(6))
                        .defenceBonus(30, -10, 50, 35, 5).attackBonus(0, 5, 0).rangedDefence(-10, 30).weakness("fire", 30)
                        .drops(stalker(160, 27, Drop.of("air_rune", 100, 100, 8), Drop.of("fire_rune", 50, 50, 8),
                                Drop.of("rune_arrow", 5, 20, 13), Drop.of("death_rune", 10, 26, 16), Drop.of("pure_essence", 15, 15, 13),
                                Drop.of("swordfish", 2, 3, 20), Drop.of("coins", 400, 750, 8), Drop.of("adamant_scimitar", 1, 1, 10)))
                        .slayer(54, "custodian_stalkers").model("juvenile_custodian_stalker").size(1.0f, 1.3f, 0.9f).spawn(Region.VARLAMORE, 3, 1, 3),
                m("mature_custodian_stalker", "Mature custodian stalker", 117, 190, 105, 75, 45, 40, 1).speed(4).attack(crush(8))
                        .defenceBonus(30, -10, 50, 35, 5).attackBonus(0, 15, 0).rangedDefence(-10, 30).weakness("fire", 30)
                        .drops(stalker(107, 18, Drop.of("air_rune", 150, 150, 9), Drop.of("fire_rune", 100, 100, 9),
                                Drop.of("rune_arrow", 10, 25, 11), Drop.of("death_rune", 25, 40, 18), Drop.of("pure_essence", 20, 20, 11),
                                Drop.of("monkfish", 2, 3, 13), Drop.of("mithril_bar", 5, 10, 21), Drop.of("huasca_seed", 1, 1, 107),
                                Drop.of("coins", 800, 1050, 11)))
                        .slayer(67, "custodian_stalkers").model("mature_custodian_stalker").size(1.0f, 1.3f, 1.05f).spawn(Region.VARLAMORE, 3, 1, 3),
                m("elder_custodian_stalker", "Elder custodian stalker", 142, 250, 120, 90, 45, 75, 1).speed(4).attack(crush(10))
                        .defenceBonus(30, -10, 50, 35, 5).attackBonus(0, 25, 0).rangedDefence(30, -10).weakness("fire", 30)
                        .drops(stalker(124, 21, Drop.of("air_rune", 200, 200, 10), Drop.of("fire_rune", 150, 150, 10),
                                Drop.of("rune_arrow", 20, 35, 10), Drop.of("death_rune", 40, 50, 21), Drop.of("shark", 2, 3, 10),
                                Drop.of("pure_essence", 30, 30, 10), Drop.of("mithril_bar", 10, 15, 21), Drop.of("huasca_seed", 2, 3, 124),
                                Drop.of("coins", 1180, 1300, 7)))
                        .slayer(76, "custodian_stalkers").model("elder_custodian_stalker").size(1.0f, 1.3f, 1.2f).spawn(Region.VARLAMORE, 3, 1, 3));
    }

    /** Recoloured look-alikes of existing models. */
    static List<ModelSpec.Variant> variants() {
        return List.of(
                new ModelSpec.Variant("albino_bat", "giant_bat", 1.1f, c -> mix(c, 0xE8E0D8, 0.7f)),
                new ModelSpec.Variant("crypt_rat", "giant_rat", 1.0f, c -> mix(c, 0x7A7468, 0.45f)),
                new ModelSpec.Variant("giant_crypt_rat", "giant_rat", 1.3f, c -> mix(c, 0x5A544A, 0.45f)),
                new ModelSpec.Variant("crypt_spider", "giant_spider", 0.9f, c -> mix(c, 0x3A3A3A, 0.5f)),
                new ModelSpec.Variant("giant_crypt_spider", "giant_spider", 1.1f, c -> mix(c, 0x2A2626, 0.5f)),
                new ModelSpec.Variant("sulphur_lizard", "lizard", 1.0f, c -> mix(c, 0xD8B040, 0.5f)),
                new ModelSpec.Variant("grimy_lizard", "lizard", 1.0f, c -> mix(c, 0x4A5A2A, 0.5f)),
                ModelSpec.Variant.of("araxyte", "acidic_araxyte"),
                new ModelSpec.Variant("araxyte_lvl146", "acidic_araxyte", 1.4f, c -> mix(c, 0x5A2A6A, 0.3f)),
                new ModelSpec.Variant("feral_vampyre", "vampyre_juvenile", 1.0f, c -> mix(c, 0x5A4A5A, 0.35f)),
                new ModelSpec.Variant("ice_wolf", "wolf", 1.0f, c -> mix(c, 0xD8E8F0, 0.6f)),
                new ModelSpec.Variant("ice_wolf_lvl132", "wolf", 1.1f, c -> mix(c, 0xB8D0E8, 0.6f)),
                new ModelSpec.Variant("pee_hat", "mountain_troll", 1.0f, c -> mix(c, 0x6A5A4A, 0.25f)),
                new ModelSpec.Variant("kraka", "mountain_troll", 1.0f, c -> mix(c, 0x5A6A5A, 0.25f)),
                new ModelSpec.Variant("troll_stick", "mountain_troll", 1.05f, c -> mix(c, 0x7A6A50, 0.25f)),
                new ModelSpec.Variant("troll_rock", "mountain_troll", 1.1f, c -> mix(c, 0x6A6A6A, 0.35f)),
                new ModelSpec.Variant("skogre", "ogre", 1.0f, c -> mix(c, 0xD8D0B8, 0.6f)),
                new ModelSpec.Variant("rooster", "chicken", 1.1f, c -> mix(c, 0x8A3A1A, 0.35f)),
                new ModelSpec.Variant("ancient_zygomite", "zygomite", 1.2f, c -> mix(c, 0x8A5A9A, 0.35f)),
                new ModelSpec.Variant("wyrmling", "wyrm", 0.6f, c -> mix(c, 0x6A5A4A, 0.2f)),
                new ModelSpec.Variant("frost_crab", "rock_crab", 0.9f, c -> mix(c, 0xC8E0F0, 0.6f)),
                new ModelSpec.Variant("chilled_jelly", "jelly", 1.0f, c -> mix(c, 0xB0D8F0, 0.55f)),
                new ModelSpec.Variant("earthen_nagua", "sulphur_nagua", 1.0f, c -> mix(c, 0x7A5A3A, 0.45f)),
                new ModelSpec.Variant("juvenile_custodian_stalker", "terror_dog", 0.9f, c -> mix(c, 0x5A6A3A, 0.45f)),
                new ModelSpec.Variant("mature_custodian_stalker", "terror_dog", 1.05f, c -> mix(c, 0x4A5A30, 0.45f)),
                new ModelSpec.Variant("elder_custodian_stalker", "terror_dog", 1.2f, c -> mix(c, 0x3A4A28, 0.5f)));
    }

    private static int mix(int rgb, int target, float amount) {
        int r = Math.round(((rgb >> 16) & 0xFF) * (1 - amount) + ((target >> 16) & 0xFF) * amount);
        int g = Math.round(((rgb >> 8) & 0xFF) * (1 - amount) + ((target >> 8) & 0xFF) * amount);
        int b = Math.round((rgb & 0xFF) * (1 - amount) + (target & 0xFF) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
