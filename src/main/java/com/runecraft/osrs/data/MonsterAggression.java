package com.runecraft.osrs.data;

import java.util.HashMap;
import java.util.Map;

/**
 * OSRS aggressiveness (wiki monster infobox "aggressive") of every creature with a wiki counterpart, keyed like
 * {@link MonsterTraits} (creature id, {@code Monster.<NAME>} for the built-in constants, the boss's entity id). Only a plain
 * "Yes" or "No" is listed; conditional entries ("Yes, unless wearing a Bandos-affiliated item", "Varies", "N/A") are left out,
 * so the creature keeps its own flag and the module rules (God Wars, revenants, Mage Arena) decide.
 *
 * <p>Generated from Kiyogitpy/osrs-wiki-data (July 2026) by {@code src/test/resources/osrs/combat/generator/monster_aggression.py}
 * and locked by {@code MonsterAggressionTest} against {@code src/test/resources/osrs/monster_aggression.tsv}. Read by the open-world
 * monster AI ({@code OsrsMonsterEntity}, round 42): many module creatures were built aggressive by default while the wiki has most
 * Slayer creatures (abyssal demons, nechryael, dust devils, smoke devils...) passive.
 */
public final class MonsterAggression {
    private static final Map<String, Boolean> ROWS = new HashMap<>();

    private MonsterAggression() {
    }

    /** The wiki's plain Yes/No for a creature key, or null when the wiki gives a condition or the creature has no counterpart. */
    public static Boolean wiki(String key) {
        return key == null ? null : ROWS.get(key);
    }

    /** {@link #wiki} where the wiki is definite, else the creature's own flag. */
    public static boolean aggressive(String key, boolean own) {
        Boolean w = wiki(key);
        return w != null ? w : own;
    }

    /** Every definite row (for the snapshot test). */
    public static Map<String, Boolean> rows() {
        return java.util.Collections.unmodifiableMap(ROWS);
    }

    /**
     * Vanilla stand-ins the generator gives no definite row (no traits row of their own, or a wiki version it can't match), read
     * off the same wiki dump by hand (round 43): the villager stands in for the Man ("aggressive = No"), the witch for the level 20
     * Dark wizard ("Yes"), the skeleton for the level 22 Skeleton ("Yes"); sheep and pigs are Infobox NPCs in OSRS (they can't be
     * attacked), so they never start a fight.
     */
    private static final Map<String, Boolean> STAND_INS = Map.of(
            "Monster.VILLAGER", Boolean.FALSE,
            "Monster.WITCH", Boolean.TRUE,
            "Monster.SKELETON", Boolean.TRUE,
            "Monster.SHEEP", Boolean.FALSE,
            "Monster.PIG", Boolean.FALSE);

    /**
     * The aggressiveness of a built-in {@link Monster} (the vanilla stand-ins in Gielinor): its wiki row, else its hand-read
     * stand-in row ({@link #STAND_INS}), else its own flag.
     */
    public static boolean aggressive(Monster m) {
        String key = MonsterTraits.key(m);
        Boolean w = wiki(key);
        if (w == null) {
            w = STAND_INS.get(key);
        }
        return w != null ? w : m.aggressive();
    }

    /** The hand-read stand-in rows (for the tests). */
    public static Map<String, Boolean> standIns() {
        return STAND_INS;
    }

    // BEGIN GENERATED (combat/generator/monster_aggression.py)
    private static final String[] AGGRESSIVE = {
            "Monster.BLACK_DEMON", "Monster.DARK_WIZARD", "Monster.GHOUL", "Monster.GIANT_RAT", "Monster.GIANT_SPIDER",
            "Monster.GREATER_DEMON", "Monster.HILL_GIANT", "Monster.ICE_GIANT", "Monster.LESSER_DEMON",
            "Monster.MOSS_GIANT", "Monster.WOLF", "Monster.ZOMBIE", "aberrant_spectre", "abyssal_guardian",
            "abyssal_leech", "abyssal_scion", "abyssal_spawn", "abyssal_walker", "acidic_araxyte", "adamant_dragon",
            "agrith_na_na", "agrith_naar", "ahrim", "alchemical_hydra", "alchemical_hydra_blue",
            "alchemical_hydra_enraged", "alchemical_hydra_red", "ammonite_crab", "amoxliatl", "ancient_wyvern",
            "animated_adamant_armour", "animated_black_armour", "animated_bronze_armour", "animated_iron_armour",
            "animated_mithril_armour", "animated_rune_armour", "animated_steel_armour", "ankou", "araxxor", "arrav",
            "artio", "asyn_shade", "baby_blue_dragon", "baby_green_dragon", "baby_red_dragon", "balance_elemental",
            "balfrug_kreeyath", "bandit", "banshee", "bark_blamish_snail", "basilisk_knight", "basilisk_sentinel",
            "big_wolf", "black_dragon", "blood_blamish_snail", "blood_blamish_snail_pointed", "blood_jaguar", "blood_moon",
            "blood_reaver", "blue_dragon", "blue_moon", "bouncer", "branda_the_fire_queen", "brawler", "bree",
            "bronze_dragon", "bruise_blamish_snail", "bruise_blamish_snail_pointed", "brutal_black_dragon",
            "brutal_blue_dragon", "brutal_green_dragon", "brutal_red_dragon", "brutus", "bryophyta", "callisto",
            "calvarion", "catablepon", "catacomb_ankou", "catacomb_black_demon", "catacomb_dagannoth",
            "catacomb_fire_giant", "catacomb_greater_demon", "catacomb_hellhound", "catacomb_hill_giant",
            "catacomb_moss_giant", "cave_bug", "cave_crawler", "cave_horror", "cave_lizard", "cerberus",
            "champion_of_scabaras", "chaos_druid", "chaos_dwarf", "chaos_elemental", "chaos_fanatic", "chronozon",
            "commander_zilyana", "corporeal_beast", "corrupt_lizardman", "corrupted_hunllef", "count_draynor",
            "cox_lizardman_shaman", "cox_lizardman_spawn", "crazy_archaeologist", "crocodile", "crystalline_hunllef",
            "culinaromancer", "cyclops", "cyclops_elite", "dagannoth", "dagannoth_lvl88", "dagannoth_lvl90",
            "dagannoth_lvl92", "dagannoth_mother", "dagannoth_prime", "dagannoth_rex", "dagannoth_spawn",
            "dagannoth_supreme", "damis", "dark_beast", "dark_energy_core", "dark_wizard_lvl11", "dark_wizard_lvl23",
            "deadly_red_spider", "defiler", "demonic_brutus", "deranged_archaeologist", "desert_lizard", "desert_wolf",
            "dessourt", "dessous", "deviant_spectre", "dharok", "dire_wolf", "doom_of_mokhaiotl", "drake", "draugen",
            "earth_warrior", "earth_warrior_champion", "eclipse_moon", "eldric_the_ice_king", "elidinis_warden", "elvarg",
            "ent", "fareed", "fire_giant", "fire_giant_lvl104", "fire_giant_lvl109", "fire_warrior_of_lesarkus",
            "fiyr_shade", "flambeed", "flesh_crawler", "flight_kilisa", "flockleader_geerin", "fragment_of_seren",
            "galvek", "gargoyle", "gelatinnoth_mother", "gemstone_crab", "general_graardor", "ghost", "ghoul_champion",
            "giant_champion", "giant_rat", "giant_rock_crab", "giant_scarab", "giant_sea_snake", "giant_skeleton",
            "giant_spider", "glod", "glough_black_demon", "gorak", "great_olm", "greater_skeleton_hellhound",
            "green_dragon", "grizzly_bear", "growler", "guard_bandit", "guthan", "gwd_cyclops", "harpie_bug_swarm",
            "hellhound", "hespori", "highwayman", "hobgoblin", "hobgoblin_champion", "hydra", "ice_spider",
            "ice_troll_grunt", "ice_troll_king", "ice_troll_runt", "ice_warrior", "imp_champion", "iron_dragon",
            "ithoi_the_navigator", "jackal", "jal_ak", "jal_akrek_ket", "jal_akrek_mej", "jal_akrek_xil", "jal_imkot",
            "jal_mejrah", "jal_xil", "jal_zek", "jaltok_jad", "jogre", "jogre_champion", "judge_of_yama", "jungle_demon",
            "jungle_horror", "jungle_spider", "jungle_wolf", "kalphite_guardian", "kalphite_queen",
            "kalphite_queen_airborne", "kalphite_soldier", "kalrag", "kamil", "karamel", "karil", "ket_zek",
            "king_black_dragon", "king_kurask", "king_sand_crab", "king_scorpion", "kreearra", "kril_tsutsaroth", "kruk",
            "kurask", "lava_dragon", "leon_dcour", "lesser_demon_champion", "lizard", "lizardman", "lizardman_brute",
            "lizardman_shaman", "loar_shade", "locust_rider", "locust_rider_ranger", "long_tailed_wyvern", "maggot_king",
            "mammoth", "maniacal_monkey", "maniacal_monkey_archer", "me", "melzar_the_mad", "mirrorback_araxyte",
            "mithril_dragon", "mogre", "molanisk", "monkey_zombie", "monkey_zombie_lvl129", "monkey_zombie_lvl82",
            "moss_guardian", "mountain_troll", "mugger", "mummy", "myre_blamish_snail", "myre_blamish_snail_pointed",
            "nazastarool", "nex", "nezikchened", "nightmare_husk", "nightmare_parasite", "nmz_arrg", "nmz_bouncer",
            "nmz_count_draynor", "nmz_elvarg", "nmz_glod", "nmz_king_roald", "nmz_me", "nmz_the_kendal", "nmz_tree_spirit",
            "nylocas_vasilias", "obor", "ochre_blamish_snail", "ochre_blamish_snail_pointed", "ogre_lvl63",
            "ogress_shaman", "ogress_warrior", "penance_fighter", "penance_queen", "penance_ranger", "pestilent_bloat",
            "phantom_muspah", "phosanis_nightmare", "phrin_shade", "pit_scorpion", "poison_scorpion", "poison_spider",
            "possessed_king_roald", "quest_arrg", "quest_barrelchest", "quest_basilisk_boss", "quest_black_eye_bethel",
            "quest_blood_moon", "quest_blue_moon", "quest_derwen", "quest_eclipse_moon", "quest_fear_reaper",
            "quest_giant_lobster", "quest_giant_roc", "quest_kolodion", "quest_mother", "quest_shellbane_gryphon",
            "quest_sigmund", "quest_skeleton_hellhound", "quest_slagilith", "quest_sourhog", "ranis_drakan", "ravager",
            "reanimated_demon_spawn", "red_dragon", "revenant_cyclops", "revenant_dark_beast", "revenant_demon",
            "revenant_dragon", "revenant_goblin", "revenant_hellhound", "revenant_hobgoblin", "revenant_imp",
            "revenant_knight", "revenant_ork", "revenant_pyrefiend", "riyl_shade", "rock_crab", "rock_lobster",
            "rune_dragon", "ruptura_araxyte", "sarachnis", "scarab_mage", "scorpia", "scorpias_offspring", "scorpion",
            "scurrius", "scurrius_giant_rat", "sea_troll_queen", "sergeant_grimspike", "sergeant_steelwill",
            "sergeant_strongstack", "shadow_hound", "shadow_of_light", "shadow_spider", "shadow_warrior",
            "shayzien_crypt_skeleton_magic", "shayzien_crypt_skeleton_melee", "shayzien_crypt_skeleton_ranged",
            "shayzien_crypt_zombie_magic", "shayzien_crypt_zombie_melee", "shayzien_crypt_zombie_ranged",
            "shellbane_gryphon", "shifter", "skeletal_wyvern", "skeleton_champion", "skeleton_hellhound",
            "skeleton_lvl142", "skotizo", "slash_bash", "slug_prince", "snakeling", "sol_heredit", "sourhog",
            "spawn_of_sarachnis", "spindel", "spinolyp", "spitting_wyvern", "splatter", "starlight", "steel_dragon",
            "sulphur_nagua", "suqah", "taloned_wyvern", "temple_spider", "terror_dog", "the_hueycoatl", "the_inadequacy",
            "the_kendal", "the_leviathan", "the_maiden_of_sugadinti", "the_whisperer", "thrower_troll",
            "titan_fire_elemental", "titan_ice_elemental", "toa_arcane_scarab", "toa_baboon_brawler", "toa_baboon_mage",
            "toa_baboon_thrower", "toa_soldier_scarab", "toa_spitting_scarab", "tok_xil", "torag", "torcher",
            "tree_spirit", "trek_nail_beast_1", "trek_nail_beast_2", "trek_nail_beast_3", "trek_shade_1", "trek_shade_2",
            "trek_undead_lumberjack_1", "trek_undead_lumberjack_2", "trek_undead_lumberjack_3", "trek_vampyre_juvinate_1",
            "trek_vampyre_juvinate_2", "trek_vampyre_juvinate_3", "trek_zombie", "treus_dayth", "troll_general",
            "tstanon_karlak", "tumekens_warden", "twisted_banshee", "tz_kek", "tz_kek_small", "tz_kih", "tzkal_zuk",
            "tztok_jad", "ulfric", "undead_druid", "undead_one", "urium_shade", "vanstrom_klause", "venenatis", "verac",
            "vetion", "wall_beast", "wallasalki", "warped_terrorbird", "warriors_guild_cyclops", "waterfiend",
            "wilderness_bandit", "wingman_skree", "witchs_experiment", "xarpus", "yama", "yt_mejkot", "zakln_gritch",
            "zamorak_crafter", "zamorak_mage", "zamorak_ranger", "zamorak_warrior", "zogre", "zombie_lvl18",
            "zombie_lvl24", "zombie_pirate", "zombies_champion", "zombified_spawn", "zulrah", "zulrah_magma",
            "zulrah_tanzanite", "albino_bat", "crypt_rat", "giant_crypt_rat", "crypt_spider", "giant_crypt_spider",
            "bloodworm", "sulphur_lizard", "araxyte", "araxyte_lvl146", "feral_vampyre", "ice_wolf", "ice_wolf_lvl132",
            "pee_hat", "kraka", "troll_stick", "grimy_lizard", "frost_crab", "earthen_nagua", "juvenile_custodian_stalker",
            "mature_custodian_stalker", "elder_custodian_stalker", "skogre",
    };
    private static final String[] PASSIVE = {
            "Monster.BARBARIAN", "Monster.CHICKEN", "Monster.COW", "Monster.GOBLIN", "Monster.GUARD", "Monster.MAN",
            "Monster.SPIDER", "abhorrent_spectre", "abyssal_demon", "abyssal_sire", "akkhas_shadow", "ardougne_knight",
            "basilisk", "bat", "bear_cub", "black_bear", "black_guard", "blood_spawn", "bloodveld", "blue_portal",
            "brine_rat", "catacomb_abyssal_demon", "catacomb_dust_devil", "cave_abomination", "cave_goblin",
            "cave_goblin_guard", "cave_kraken", "cave_slime", "chasm_crawler", "choke_devil", "chompy_bird", "cockathrice",
            "cockatrice", "colossal_hydra", "crawling_hand", "crushing_hand", "dad", "dagannoth_fledgeling", "dawn",
            "delrith", "ds2_vorkath", "duck", "dusk", "dust_devil", "dwarf", "elf_archer", "elf_warrior",
            "enormous_tentacle", "evil_creature", "farmer", "fever_spider", "flaming_pyrelord", "gadderanks", "giant_bat",
            "giant_mole", "giant_rockslug", "gnome", "gnome_guard", "goblin_champion", "goblin_lvl5",
            "greater_abyssal_demon", "greater_nechryael", "grizzly_bear_cub", "guardian_drake", "gwd_bloodveld", "gwd_imp",
            "hesporis_flower", "ice_queen", "icefiend", "imp", "infernal_mage", "insatiable_bloodveld",
            "insatiable_mutated_bloodveld", "jal_mejjak", "jal_nib", "jelly", "jonny_the_beard", "killerwatt", "kraken",
            "malevolent_mage", "marble_gargoyle", "mercenary_captain", "minotaur", "monk", "monk_of_zamorak", "monkey",
            "monstrous_basilisk", "mudskipper", "mutated_bloodveld", "nechryael", "nechryarch", "night_beast", "nmz_dad",
            "nuclear_smoke_devil", "nylocas_athanatos", "nylocas_hagios", "nylocas_ischyros", "nylocas_matomenos",
            "nylocas_toxobolos", "olm_left_claw", "olm_right_claw", "oomlie_bird", "otherworldly_being", "pirate",
            "prifddinas_guard", "purple_portal", "pyrefiend", "pyrelord", "quest_gryphon", "quest_justiciar_zachariah",
            "quest_porazdir", "rat", "red_portal", "repugnant_spectre", "respiratory_system", "rockslug", "sand_crab",
            "scorpias_guardian", "screaming_banshee", "screaming_twisted_banshee", "sea_snake_young", "seagull",
            "shadow_wyrm", "sir_mordred", "small_lizard", "smoke_devil", "sorebones", "sotetseg", "spiked_turoth",
            "spinner", "tanglefoot", "temple_guardian", "terrorbird", "the_mimic", "the_nightmare",
            "thermonuclear_smoke_devil", "toa_scarab_swarm", "tormented_demon", "tortured_soul", "trek_ghast_1",
            "trek_ghast_2", "trek_ghast_3", "trek_shade_3", "troll_spectator", "turoth", "tzhaar_hur_citizen",
            "tzhaar_ket", "tzhaar_mej", "tzhaar_xil", "undead_chicken", "undead_cow", "vampyre_juvenile", "vardorvis",
            "vitreous_jelly", "vitreous_warped_jelly", "void_knight", "vorkath", "vulture", "warped_jelly", "white_knight",
            "wild_dog", "woman", "wyrm", "yak", "yama_void_flare", "yellow_portal", "yt_hurkot", "yt_hurkot_inferno",
            "zombie_rat", "ancient_zygomite", "chilled_jelly", "rooster",
    };
    // END GENERATED

    static {
        for (String k : AGGRESSIVE) {
            ROWS.put(k, Boolean.TRUE);
        }
        for (String k : PASSIVE) {
            ROWS.put(k, Boolean.FALSE);
        }
    }
}
