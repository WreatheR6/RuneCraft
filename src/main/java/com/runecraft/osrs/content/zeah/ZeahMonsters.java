package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Kourend and Varlamore monsters. Lizardmen, lizardman shamans, wyrms, drakes, hydras and fire
 * giants already exist in the slayer module and are reused (their camps are kept here); this module
 * adds lizardman brutes, sand crabs and the King sand crab, the Catacombs of Kourend monsters (with
 * dark totem pieces and ancient shards on their drop tables), the naguas, Varlamore's guards and the
 * Blood Moon's jaguars.
 *
 * <p>Stats are OSRS values where known; Varlamore monsters (released 2024) and the catacomb drop
 * tables are approximations of the wiki.
 */
public final class ZeahMonsters {
    private ZeahMonsters() {
    }

    /** Every catacomb creature (spawned in the Catacombs carved under Kourend Castle). */
    public static final List<String> CATACOMB_CREATURES = List.of("deviant_spectre", "twisted_banshee", "warped_jelly",
            "catacomb_abyssal_demon", "catacomb_greater_demon", "catacomb_black_demon", "catacomb_hellhound",
            "catacomb_fire_giant", "catacomb_moss_giant", "catacomb_hill_giant", "catacomb_dagannoth", "catacomb_ankou",
            "catacomb_dust_devil");

    static MonsterDef m(String id, String name, int level, int hp, int att, int str, int def, int mag, int rng) {
        return MonsterDef.of(id, name).level(level).stats(hp, att, str, def, mag, rng);
    }

    static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    static Drop clue(String tier, int rarity) {
        return Drop.of("clue_scroll_" + tier, 1, 1, rarity);
    }

    /**
     * Ancient shard rarity in the Catacombs of Kourend: 1 / (2/3 x (500 - combat level)), at most 1/50.
     */
    public static int shardRarity(int combatLevel) {
        return Math.max(50, (int) Math.round(2.0 / 3.0 * (500 - Math.min(combatLevel, 425))));
    }

    /**
     * Rarity of each of the three dark totem pieces: one piece drops at 1 / (500 - combat level),
     * split evenly between base, middle and top.
     */
    public static int totemPieceRarity(int combatLevel) {
        return 3 * Math.max(75, 500 - combatLevel);
    }

    /** Adds the Catacombs of Kourend shard and totem rolls to a monster's drop table. */
    static MonsterDef catacomb(MonsterDef def) {
        int t = totemPieceRarity(def.combatLevel);
        return def.drops(Drop.always("malicious_ashes"), Drop.always("vile_ashes"), Drop.of("ancient_shard", 1, 1, shardRarity(def.combatLevel)), Drop.of("dark_totem_base", 1, 1, t),
                Drop.of("dark_totem_middle", 1, 1, t), Drop.of("dark_totem_top", 1, 1, t)).underground();
    }

    public static List<MonsterDef> all() {
        List<MonsterDef> list = new ArrayList<>();
        // ---- Kourend coast ----
        list.add(m("sand_crab", "Sand crab", 15, 60, 1, 1, 1, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 1))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.of("coins", 1, 20, 4), clue("easy", 500)) // approx.
                .model("sand_crab").size(1.0f, 0.7f, 1.0f).spawn(Region.KOUREND, 2, 1, 3));
        list.add(m("king_sand_crab", "King sand crab", 107, 150, 90, 90, 100, 1, 1).speed(4).attack(melee(AttackType.CRUSH, 9))
                .defenceBonus(30, 30, 30, 0, 30).drops(Drop.of("coins", 20, 200, 3), Drop.gems(32), clue("medium", 128)) // approx.
                .model("sand_crab").size(1.4f, 1.0f, 1.8f));
        // ---- Lizardmen ----
        list.add(m("lizardman_brute", "Lizardman brute", 73, 60, 70, 70, 64, 1, 50).speed(4).attack(melee(AttackType.CRUSH, 9))
                .attack(AttackDef.of("acid", AttackType.RANGED, 7, 8, 4).special(Special.POISON, 4).particle("minecraft:item_slime"))
                .defenceBonus(30, 30, 30, 20, 30)
                .drops(Drop.always("bones"), Drop.of("lizardman_fang", 1, 3, 1), Drop.of("xerics_talisman_inert", 1, 1, 250),
                        Drop.herbs(10), Drop.seeds(18), clue("medium", 128))
                .slayer(1, "lizardmen").model("lizardman").size(0.9f, 2.1f, 1.1f));
        // ---- Catacombs of Kourend ----
        list.add(catacomb(m("deviant_spectre", "Deviant spectre", 169, 190, 1, 1, 1, 1, 1).speed(4)
                .attack(AttackDef.of("spectral touch", AttackType.MAGIC, 21, 2.5, 10).special(Special.DRAIN_STAT, 0))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(4), Drop.of("coins", 20, 400, 4), clue("hard", 350)).slayer(60, "aberrant_spectres")
                .skin(0x6A7A5A, 0x4A5A3A, 0x3A4A2A, 0x9AB070).size(0.9f, 2.4f, 1.1f).flying()));
        list.add(catacomb(m("twisted_banshee", "Twisted banshee", 89, 100, 1, 1, 1, 1, 1).speed(4)
                .attack(AttackDef.of("scream", AttackType.MAGIC, 7, 2.5, 10).special(Special.DRAIN_STAT, 0))
                .defenceBonus(0, 0, 0, 0, 0).drops(Drop.herbs(6), Drop.of("cosmic_rune", 5, 15, 12), clue("hard", 350))
                .slayer(15, "banshees").skin(0xD8D0E0, 0xB0A8C0, 0x8A80A0, 0x5A3A6A).size(0.7f, 2.0f, 1.0f).flying()));
        list.add(catacomb(m("warped_jelly", "Warped jelly", 112, 140, 1, 1, 1, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 19)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.of("coins", 10, 500, 4), Drop.gems(32), clue("hard", 256)).slayer(52, "jellies")
                .skin(0x6A9A4A, 0x4A7A3A, 0x3A5A2A, 0xA0D060).size(1.0f, 1.4f, 1.1f)));
        list.add(catacomb(m("catacomb_abyssal_demon", "Abyssal demon", 124, 150, 97, 67, 135, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 8)).defenceBonus(20, 20, 20, 0, 20)
                .drops(Drop.always("abyssal_ashes"), Drop.of("abyssal_whip", 1, 1, 512), Drop.herbs(16), clue("hard", 256))
                .slayer(85, "abyssal_demons").skin(0x3A1A3A, 0x2A102A, 0x2A102A, 0xA02020).size(0.8f, 2.2f, 1.2f)));
        list.add(catacomb(m("catacomb_greater_demon", "Greater demon", 92, 87, 76, 78, 81, 1, 1).speed(4)
                .attack(melee(AttackType.SLASH, 9)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.of("coins", 30, 1000, 3), Drop.of("rune_full_helm", 1, 1, 128), Drop.of("fire_rune", 15, 75, 8), clue("hard", 128))
                .slayer(1, "greater_demons").skin(0xA02020, 0x7A1A1A, 0x5A1010, 0x2A2A2A).size(1.0f, 2.8f, 1.2f)));
        list.add(catacomb(m("catacomb_black_demon", "Black demon", 172, 157, 145, 148, 152, 1, 1).speed(4)
                .attack(melee(AttackType.SLASH, 16)).defenceBonus(0, 0, 0, -10, 0)
                .drops(Drop.of("coins", 50, 1500, 3), Drop.of("rune_med_helm", 1, 1, 128), Drop.of("blood_rune", 7, 7, 32), Drop.herbs(10),
                        clue("hard", 128))
                .slayer(1, "black_demons").skin(0x2A2A2A, 0x1A1A1A, 0x101010, 0xA02020).size(1.1f, 3.0f, 1.3f)));
        list.add(catacomb(m("catacomb_hellhound", "Hellhound", 122, 116, 105, 104, 102, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 11)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("vile_ashes"), clue("hard", 64)).slayer(1, "hellhounds")
                .skin(0x8A1A1A, 0x5A1010, 0x3A0A0A, 0xF0A020).size(1.0f, 1.4f, 1.2f)));
        list.add(catacomb(m("catacomb_fire_giant", "Fire giant", 86, 111, 65, 65, 65, 1, 1).speed(5)
                .attack(melee(AttackType.CRUSH, 11)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("big_bones"), Drop.of("fire_rune", 50, 150, 6), Drop.of("rune_scimitar", 1, 1, 128), Drop.herbs(14),
                        Drop.gems(20), clue("hard", 128))
                .slayer(1, "fire_giants").skin(0xD07040, 0x8A3A1A, 0x5A2A1A, 0xF0A020).size(1.0f, 3.0f, 1.0f)));
        list.add(catacomb(m("catacomb_moss_giant", "Moss giant", 42, 60, 30, 30, 30, 1, 1).speed(5)
                .attack(melee(AttackType.CRUSH, 6)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("big_bones"), Drop.of("nature_rune", 4, 4, 18), Drop.of("law_rune", 3, 3, 32), Drop.herbs(14),
                        Drop.seeds(10), clue("beginner", 40))
                .slayer(1, "moss_giants").skin(0x5A8A3A, 0x3A6A2A, 0x2A4A1A, 0x6A9A4A).size(1.0f, 3.0f, 1.0f)));
        list.add(catacomb(m("catacomb_hill_giant", "Hill giant", 28, 35, 18, 22, 26, 1, 1).speed(6)
                .attack(melee(AttackType.CRUSH, 4)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("big_bones"), Drop.of("giant_key", 1, 1, 128), Drop.herbs(14), clue("beginner", 50))
                .slayer(1, "hill_giants").skin(0xC8A080, 0x6A5A3A, 0x4A3A2A, 0x3A2A1A).size(1.0f, 3.0f, 1.0f)));
        list.add(catacomb(m("catacomb_dagannoth", "Dagannoth", 74, 70, 68, 70, 70, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 7)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.of("water_rune", 10, 30, 6), Drop.herbs(16), clue("hard", 256))
                .slayer(1, "dagannoth").skin(0x3A5A3A, 0x2A4A2A, 0x1A3A1A, 0x8A9A6A).size(1.0f, 2.2f, 1.0f)));
        list.add(catacomb(m("catacomb_ankou", "Ankou", 75, 60, 60, 60, 70, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 8)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.always("bones"), Drop.of("death_rune", 10, 10, 10), Drop.of("blood_rune", 5, 5, 25), clue("hard", 512))
                .slayer(1, "ankou").skin(0xE0E0E0, 0x2A2A3A, 0x1A1A2A, 0x6A6A8A).size(0.7f, 2.0f, 1.0f)));
        list.add(catacomb(m("catacomb_dust_devil", "Dust devil", 93, 105, 105, 70, 40, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 8)).defenceBonus(0, 0, 0, 0, 0)
                .drops(Drop.of("dust_battlestaff", 1, 1, 4000), Drop.of("dragon_chainbody", 1, 1, 32768), Drop.herbs(14),
                        Drop.gems(64), clue("hard", 128))
                .slayer(65, "dust_devils").skin(0xC0A070, 0xA08050, 0x806030, 0xE0D0A0).size(0.9f, 1.8f, 1.0f).flying()));
        // ---- Varlamore ----
        list.add(m("sulphur_nagua", "Sulphur nagua", 98, 100, 85, 85, 70, 70, 1).speed(4)
                .attack(melee(AttackType.SLASH, 14))
                .attack(AttackDef.of("sulphur breath", AttackType.MAGIC, 12, 6, 4).special(Special.POISON, 6)
                        .particle("minecraft:smoke"))
                .defenceBonus(40, 40, 40, 20, 40)
                .drops(Drop.always("bones"), Drop.of("fire_rune", 30, 90, 6), Drop.of("coins", 50, 600, 4), Drop.herbs(16),
                        clue("hard", 256)) // approx.
                .skin(0xC8A040, 0xA08020, 0x6A5010, 0xE0E060).size(0.9f, 2.4f, 1.1f));
        list.add(m("frost_nagua", "Frost nagua", 81, 85, 70, 70, 60, 60, 1).speed(4)
                .attack(melee(AttackType.SLASH, 12))
                .attack(AttackDef.of("frost breath", AttackType.MAGIC, 10, 6, 4).special(Special.FREEZE, 3)
                        .particle("minecraft:snowflake"))
                .defenceBonus(35, 35, 35, 40, 35)
                .drops(Drop.always("bones"), Drop.of("water_rune", 30, 90, 6), Drop.of("coins", 40, 500, 4), Drop.herbs(16),
                        clue("hard", 256)) // approx.
                .skin(0x8AB0D0, 0x5A80B0, 0x3A5A8A, 0xE0F0FF).size(0.9f, 2.4f, 1.1f));
        list.add(m("fortis_guard", "Guard", 46, 50, 40, 40, 40, 1, 1).speed(4).attack(melee(AttackType.STAB, 5))
                .defenceBonus(20, 20, 20, 0, 20).drops(Drop.always("bones"), Drop.of("coins", 5, 60, 2), clue("medium", 128))
                .passive().skin(0xA87850, 0xC0A060, 0x6A4A2A, 0xD0B040).size(0.6f, 1.95f, 1.0f)); // approx.
        list.add(m("blood_jaguar", "Blood jaguar", 102, 80, 90, 90, 60, 1, 1).speed(3).attack(melee(AttackType.SLASH, 10))
                .defenceBonus(20, 20, 20, 10, 20).skin(0x8A1A1A, 0x5A1010, 0x3A0A0A, 0xE0C040).size(1.0f, 1.2f, 1.2f)); // approx.
        return list;
    }
}
