package com.runecraft.osrs.data;

import com.runecraft.osrs.content.CombatProfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * OSRS monster traits that affect gameplay, for every creature with a wiki counterpart: size in tiles, attributes
 * (undead, demon, dragon, kalphite, leafy, vampyre, xerician, shade, spectral, fiery, golem, rat, penance, flying),
 * poison and venom immunity and the Slayer XP a kill gives when it differs from the hitpoints (bosses, dragons,
 * superiors...). Keys are creature ids, {@code Monster.<NAME>} for the built-in {@link Monster} constants and
 * {@link Boss#entityId()} for the built-in bosses.
 *
 * <p>Generated from the wiki's DPS calculator data (weirdgloop/osrs-dps-calc {@code monsters.json}, matched by name
 * and combat level; the {@code vampyre1..3} tiers count as {@code vampyre}) and the monster infoboxes
 * (Kiyogitpy/osrs-wiki-data, July 2026, with osrsreboxed-db where a page lacks the field), and locked by
 * {@code MonsterTraitsTest} against {@code src/test/resources/osrs/monster_traits.tsv}. Applied by
 * {@link com.runecraft.osrs.content.ContentRegistry} after {@link MonsterStatsTable}: a creature's attributes are
 * replaced by its row's.
 */
public final class MonsterTraits {
    /** One creature's traits; {@code slayerXp} 0 means "the hitpoints". */
    public record Traits(String key, int size, boolean poisonImmune, boolean venomImmune, double slayerXp,
                         Set<String> attributes) {
    }

    private static final Map<String, Traits> ROWS = new LinkedHashMap<>();

    static {
        rows0();
        rows1();
        rows2();
    }

    private MonsterTraits() {
    }

    /** The traits of a creature key, or null when it has no wiki counterpart. */
    public static Traits get(String key) {
        return key == null ? null : ROWS.get(key);
    }

    /** Every row, keyed by creature key. */
    public static Map<String, Traits> rows() {
        return Collections.unmodifiableMap(ROWS);
    }

    /** Key of a built-in monster constant. */
    public static String key(Monster m) {
        return "Monster." + m.name();
    }

    /** Slayer XP for a kill: the wiki's value where it differs from the hitpoints, else {@code hitpoints}. */
    public static double slayerXp(String key, int hitpoints) {
        Traits t = get(key);
        return t != null && t.slayerXp() > 0 ? t.slayerXp() : hitpoints;
    }

    public static boolean poisonImmune(String key) {
        Traits t = get(key);
        return t != null && t.poisonImmune();
    }

    /** Venom immunity (a poison-immune monster with no venom data is venom-immune too). */
    public static boolean venomImmune(String key) {
        Traits t = get(key);
        return t != null && t.venomImmune();
    }

    /** Size in tiles, or 0 when unknown. */
    public static int size(String key) {
        Traits t = get(key);
        return t == null ? 0 : t.size();
    }

    /** Copies a creature's traits onto it: attributes, size, immunities and Slayer XP. False when it has no row. */
    public static boolean apply(CombatProfile<?> p) {
        Traits t = ROWS.get(p.id);
        if (t == null) {
            return false;
        }
        p.attributes.clear();
        p.attributes.addAll(t.attributes());
        p.tiles = t.size();
        p.poisonImmune = t.poisonImmune();
        p.venomImmune = t.venomImmune();
        p.slayerXp = t.slayerXp();
        return true;
    }

    private static void t(String key, int size, boolean poison, boolean venom, double slayerXp, String... attributes) {
        if (ROWS.put(key, new Traits(key, size, poison, venom, slayerXp, Set.of(attributes))) != null) {
            throw new IllegalStateException("Duplicate monster traits row " + key);
        }
    }

    private static void rows0() {
        t("Monster.AL_KHARID_WARRIOR", 1, false, false, 0); // Al Kharid warrior
        t("Monster.BARBARIAN", 1, false, false, 0); // Barbarian (Level 10 (Acelin))
        t("Monster.BLACK_DEMON", 3, false, false, 157, "demon"); // Black demon (Level 172)
        t("Monster.CHICKEN", 1, true, true, 3); // Chicken (Falador Farm)
        t("Monster.COW", 2, true, true, 8); // Cow (1)
        t("Monster.DARK_WIZARD", 1, false, false, 0); // Dark wizard (Level 7)
        t("Monster.GHOUL", 1, false, false, 50); // Ghoul
        t("Monster.GIANT_RAT", 2, false, false, 5, "rat"); // Giant rat (Level 3)
        t("Monster.GIANT_SPIDER", 1, false, false, 33); // Giant spider (Level 27)
        t("Monster.GOBLIN", 1, false, false, 5); // Goblin (Level 2)
        t("Monster.GREATER_DEMON", 3, false, false, 87, "demon"); // Greater demon (Level 92)
        t("Monster.GUARD", 1, false, false, 0); // Guard (Edgeville 1)
        t("Monster.HILL_GIANT", 2, false, false, 35); // Hill Giant
        t("Monster.ICE_GIANT", 2, false, false, 70); // Ice giant (1)
        t("Monster.LESSER_DEMON", 2, false, false, 79, "demon"); // Lesser demon (Level 82)
        t("Monster.MAN", 1, false, false, 0); // Man (1)
        t("Monster.MOSS_GIANT", 2, false, false, 60); // Moss giant (Level 42)
        t("Monster.SKELETON", 1, false, false, 0, "undead"); // Skeleton (Level 22, 1)
        t("Monster.SPIDER", 1, false, false, 2); // Spider (Common)
        t("Monster.WOLF", 2, false, false, 34); // Wolf (Level 25)
        t("Monster.ZOMBIE", 1, false, false, 22, "undead"); // Zombie (Level 13, 1)
        t("aberrant_spectre", 2, false, false, 90, "spectral", "undead"); // Aberrant spectre
        t("abhorrent_spectre", 3, true, true, 2500, "spectral", "undead"); // Abhorrent spectre
        t("abyssal_demon", 1, false, false, 150, "demon"); // Abyssal demon (Catacombs of Kourend)
        t("abyssal_guardian", 1, false, false, 0); // Abyssal guardian
        t("abyssal_leech", 1, false, false, 0); // Abyssal leech
        t("abyssal_scion", 2, true, true, 0); // Scion
        t("abyssal_sire", 6, true, true, 478, "demon"); // Abyssal Sire (Phase 1)
        t("abyssal_spawn", 1, true, true, 0); // Spawn
        t("abyssal_walker", 1, false, false, 0); // Abyssal walker
        t("acidic_araxyte", 2, true, true, 58); // Acidic Araxyte
        t("adamant_dragon", 5, true, true, 324.5, "dragon", "fiery"); // Adamant dragon
        t("agrith_na_na", 3, false, false, 0); // Agrith-Na-Na
        t("agrith_naar", 3, false, false, 0, "demon"); // Agrith Naar
        t("ahrim", 1, false, false, 102.5, "spectral"); // Ahrim the Blighted
        t("akkhas_shadow", 3, true, true, 0); // Akkha's Shadow
        t("alchemical_hydra", 6, true, true, 1320, "dragon"); // Alchemical Hydra (Electric)
        t("alchemical_hydra_blue", 6, true, true, 1320, "dragon"); // Alchemical Hydra (Electric)
        t("alchemical_hydra_enraged", 6, true, true, 1320, "dragon"); // Alchemical Hydra (Electric)
        t("alchemical_hydra_red", 6, true, true, 1320, "dragon"); // Alchemical Hydra (Electric)
        t("ammonite_crab", 2, false, false, 100); // Ammonite Crab
        t("amoxliatl", 3, true, true, 546, "spectral"); // Amoxliatl (Post-quest)
        t("ancient_wyvern", 5, false, false, 315, "dragon"); // Ancient Wyvern
        t("animated_adamant_armour", 1, true, true, 0); // Animated Adamant Armour
        t("animated_black_armour", 1, true, true, 0); // Animated Black Armour
        t("animated_bronze_armour", 1, true, true, 0); // Animated Bronze Armour
        t("animated_iron_armour", 1, true, true, 0); // Animated Iron Armour
        t("animated_mithril_armour", 1, true, true, 0); // Animated Mithril Armour
        t("animated_rune_armour", 1, true, true, 0); // Animated Rune Armour
        t("animated_steel_armour", 1, true, true, 0); // Animated Steel Armour
        t("ankou", 1, false, false, 60, "spectral", "undead"); // Ankou (Level 75)
        t("araxxor", 7, true, true, 1708); // Araxxor
        t("ardougne_knight", 1, false, false, 0); // Knight of Ardougne (Female)
        t("arrav", 1, true, true, 0, "undead"); // Arrav
        t("artio", 3, true, true, 518); // Artio
        t("asyn_shade", 1, true, true, 90, "shade", "spectral", "undead"); // Asyn Shade (Shade)
        t("avatar_of_creation", 5, true, true, 0); // Avatar of Creation
        t("avatar_of_destruction", 5, true, true, 0); // Avatar of Destruction
        t("baby_black_dragon", 2, false, false, 80, "dragon"); // Baby black dragon (Myths' Guild)
        t("baby_blue_dragon", 2, false, false, 50, "dragon"); // Baby blue dragon (1)
        t("baby_green_dragon", 2, false, false, 0, "dragon"); // Baby green dragon (1)
        t("baby_impling", 0, false, false, 0); // Baby impling (Overworld)
        t("baby_red_dragon", 2, false, false, 50, "dragon"); // Baby red dragon (1)
        t("balance_elemental", 3, true, true, 0); // Balance Elemental (Magic)
        t("balfrug_kreeyath", 3, false, false, 161, "demon"); // Balfrug Kreeyath
        t("bandit", 1, false, false, 27); // Bandit (Level 22)
        t("banshee", 2, false, false, 22, "spectral", "undead"); // Banshee
        t("bark_blamish_snail", 1, false, false, 0); // Bark Blamish Snail
        t("basilisk", 2, false, false, 75); // Basilisk
        t("basilisk_knight", 3, true, true, 300); // Basilisk Knight
        t("basilisk_sentinel", 3, true, true, 5590); // Basilisk Sentinel
        t("bat", 1, false, false, 8); // Bat
        t("bear_cub", 1, false, false, 20); // Bear Cub
        t("big_wolf", 2, false, false, 74); // Big Wolf (Feldip Hills)
        t("black_bear", 2, false, false, 25); // Black bear
        t("black_dragon", 4, false, false, 194.7, "dragon", "fiery"); // Black dragon (Level 227)
        t("black_guard", 1, false, false, 30); // Black Guard (Level 25, 1)
        t("black_knight_titan", 1, false, false, 0); // Black Knight Titan
        t("blood_blamish_snail", 1, false, false, 0); // Blood Blamish Snail (Pointed)
        t("blood_blamish_snail_pointed", 1, false, false, 0); // Blood Blamish Snail (Pointed)
        t("blood_jaguar", 2, true, true, 0); // Blood jaguar
        t("blood_moon", 5, true, true, 0); // Blood Moon
        t("blood_reaver", 2, false, false, 0, "demon"); // Blood Reaver (Nex's chamber)
        t("blood_spawn", 1, true, true, 0); // Blood spawn (Entry)
        t("bloodveld", 2, false, false, 120, "demon"); // Bloodveld (Normal)
        t("blue_dragon", 4, false, false, 105, "dragon", "fiery"); // Blue dragon (1)
        t("blue_moon", 5, true, true, 0); // Blue Moon
        t("blue_portal", 3, true, true, 0); // Portal (Pest Control) (Blue, Intermediate/veteran)
        t("bouncer", 2, false, false, 116, "demon"); // Bouncer
        t("branda_the_fire_queen", 3, true, true, 735, "fiery"); // Branda the Fire Queen
        t("brawler", 2, false, false, 0); // Brawler (Level 129)
        t("bree", 2, false, false, 0); // Bree
        t("brine_rat", 1, false, false, 50, "rat"); // Brine rat
        t("bronze_dragon", 4, false, false, 125, "dragon", "fiery"); // Bronze dragon (Standard)
        t("bruise_blamish_snail", 1, false, false, 0); // Bruise Blamish Snail (Pointed)
        t("bruise_blamish_snail_pointed", 1, false, false, 0); // Bruise Blamish Snail (Pointed)
        t("brutal_black_dragon", 4, true, true, 346.4, "dragon", "fiery"); // Brutal black dragon
        t("brutal_blue_dragon", 4, true, true, 257, "dragon", "fiery"); // Brutal blue dragon (Catacombs of Kourend)
        t("brutal_green_dragon", 4, true, true, 0, "dragon", "fiery"); // Brutal green dragon
        t("brutal_red_dragon", 4, true, true, 306.2, "dragon", "fiery"); // Brutal red dragon
        t("brutus", 3, false, false, 116); // Brutus
        t("bryophyta", 3, false, false, 115); // Bryophyta
        t("callisto", 5, true, true, 1300); // Callisto
        t("calvarion", 3, true, true, 322.5, "undead"); // Calvar'ion (Enraged)
        t("catablepon", 2, false, false, 70); // Catablepon (Level 64)
        t("catacomb_abyssal_demon", 1, false, false, 150, "demon"); // Abyssal demon (Catacombs of Kourend)
        t("catacomb_ankou", 1, false, false, 60, "spectral", "undead"); // Ankou (Level 75)
        t("catacomb_black_demon", 3, false, false, 157, "demon"); // Black demon (Level 172)
        t("catacomb_dagannoth", 1, false, false, 70); // Dagannoth (Level 74 (1))
        t("catacomb_dust_devil", 1, false, false, 105); // Dust devil (Smoke Dungeon)
        t("catacomb_fire_giant", 2, false, false, 111, "fiery"); // Fire giant (Level 86)
        t("catacomb_greater_demon", 3, false, false, 87, "demon"); // Greater demon (Level 92)
        t("catacomb_hellhound", 2, false, false, 116, "demon"); // Hellhound (Level 122)
        t("catacomb_hill_giant", 2, false, false, 35); // Hill Giant
        t("catacomb_moss_giant", 2, false, false, 60); // Moss giant (Level 42)
        t("cave_abomination", 2, true, true, 1300); // Cave abomination
        t("cave_bug", 1, false, false, 5); // Cave bug (Level 6)
        t("cave_crawler", 2, false, false, 22); // Cave crawler (Icy (1))
        t("cave_goblin", 1, false, false, 10); // Cave goblin (monster) (Backpack)
        t("cave_goblin_guard", 1, false, false, 26); // Cave goblin guard (Level 24)
        t("cave_horror", 2, false, false, 55); // Cave horror (Alpha)
        t("cave_kraken", 2, false, false, 125); // Cave kraken (Cave kraken)
        t("cave_lizard", 1, false, false, 0); // Cave lizard
        t("cave_slime", 1, false, false, 25); // Cave slime
        t("cerberus", 5, true, true, 690, "demon"); // Cerberus
        t("champion_of_scabaras", 1, true, true, 0, "kalphite"); // Champion of Scabaras
        t("chaos_druid", 1, false, false, 20); // Chaos druid
        t("chaos_dwarf", 1, false, false, 61); // Chaos dwarf
        t("chaos_elemental", 3, false, false, 268.75); // Chaos Elemental
        t("chaos_fanatic", 1, false, false, 253); // Chaos Fanatic
        t("chasm_crawler", 3, true, true, 600); // Chasm Crawler (Icy)
        t("choke_devil", 2, true, true, 3000); // Choke devil
        t("chompy_bird", 1, true, true, 10); // Chompy bird
        t("chronozon", 3, false, false, 0, "demon"); // Chronozon
        t("cockathrice", 3, true, true, 950); // Cockathrice
        t("cockatrice", 2, false, false, 37); // Cockatrice
        t("colossal_hydra", 5, true, true, 8625, "dragon"); // Colossal Hydra
        t("commander_zilyana", 2, true, true, 350); // Commander Zilyana
        t("corporeal_beast", 5, true, true, 0); // Corporeal Beast
        t("corrupt_lizardman", 1, true, true, 50); // Corrupt Lizardman
        t("corrupted_hunllef", 5, true, true, 0); // Corrupted Hunllef
        t("count_draynor", 1, false, false, 0.8, "vampyre"); // Count Draynor
        t("cox_lizardman_shaman", 2, true, true, 157.5); // Lizardman shaman (Lizardman Temple)
        t("cox_lizardman_spawn", 1, true, true, 0); // Spawn
        t("crawling_hand", 1, false, false, 16, "undead"); // Crawling Hand (Level 8 (1))
        t("crazy_archaeologist", 1, false, false, 275); // Crazy archaeologist
        t("crocodile", 2, false, false, 62); // Crocodile (Land)
        t("cruor", 1, true, true, 0); // Cruor
        t("crushing_hand", 3, true, true, 550, "undead"); // Crushing hand
        t("crystalline_hunllef", 5, true, true, 0); // Crystalline Hunllef
        t("culinaromancer", 1, false, false, 0); // Culinaromancer
        t("cyclops", 2, false, false, 100); // Cyclops (Level 76)
        t("cyclops_elite", 2, false, false, 150); // Cyclops (Level 106)
        t("dad", 3, false, false, 126); // Dad
        t("dagannoth", 1, false, false, 70); // Dagannoth (Level 74 (1))
        t("dagannoth_fledgeling", 2, false, false, 0); // Dagannoth fledgeling
        t("dagannoth_lvl88", 1, false, false, 85); // Dagannoth (Waterbirth Island) (Level 88)
        t("dagannoth_lvl90", 1, false, false, 99.6); // Dagannoth (Waterbirth Island) (Level 90)
        t("dagannoth_lvl92", 2, false, false, 120); // Dagannoth (Level 92 (1))
        t("dagannoth_mother", 3, false, false, 126); // Dagannoth mother
        t("dagannoth_prime", 3, false, false, 331.4); // Dagannoth Prime
        t("dagannoth_rex", 3, false, false, 331.4); // Dagannoth Rex
        t("dagannoth_spawn", 1, false, false, 35); // Dagannoth spawn
        t("dagannoth_supreme", 3, false, false, 255); // Dagannoth Supreme
        t("damis", 1, false, false, 0); // Damis (First form)
        t("dark_beast", 3, false, false, 225.4); // Dark beast
        t("dark_energy_core", 1, false, false, 0); // Dark energy core
        t("dark_wizard_lvl11", 1, false, false, 0); // Dark wizard (Level 11)
        t("dark_wizard_lvl23", 1, false, false, 0); // Dark wizard (Level 23)
        t("dawn", 4, true, true, 0, "flying", "golem"); // Dawn
        t("deadly_red_spider", 1, false, false, 35); // Deadly red spider
        t("defiler", 1, false, false, 0); // Defiler (Level 97)
        t("delrith", 2, false, false, 0); // Delrith
        t("demonic_brutus", 3, true, true, 0); // Demonic Brutus (Brutus)
        t("demonic_gorilla", 2, false, false, 408.5, "demon"); // Demonic gorilla (1)
        t("deranged_archaeologist", 1, false, false, 275); // Deranged archaeologist
        t("desert_bandit", 1, true, true, 0); // Bandit (Bandit Camp) (Level 57)
        t("desert_lizard", 2, false, false, 25); // Desert Lizard (Green)
        t("desert_wolf", 2, false, false, 34); // Desert Wolf (1)
        t("dessourt", 1, false, false, 0); // Dessourt
        t("dessous", 1, false, true, 220, "vampyre"); // Dessous
        t("deviant_spectre", 2, false, false, 194.5, "spectral", "undead"); // Deviant spectre
        t("dharok", 1, false, false, 115, "spectral"); // Dharok the Wretched
        t("dire_wolf", 2, false, false, 85); // Dire Wolf (Level 88)
        t("doom_of_mokhaiotl", 5, false, false, 0, "demon"); // Doom of Mokhaiotl (Deep Delve)
        t("dragon_impling", 0, false, false, 0); // Dragon impling
        t("drake", 5, true, true, 230.6, "dragon"); // Drake
        t("draugen", 1, false, false, 0, "undead"); // The Draugen
        t("ds2_vorkath", 7, true, true, 460, "dragon", "fiery", "undead"); // Vorkath (Dragon Slayer II)
        t("duck", 1, true, true, 3); // Duck (Female)
        t("duke_sucellus", 7, false, false, 305, "demon"); // Duke Sucellus (Post-quest, Awake)
        t("dusk", 6, true, true, 1350, "golem"); // Dusk (Second form)
        t("dust_devil", 1, false, false, 105); // Dust devil (Smoke Dungeon)
        t("dwarf", 1, false, false, 10); // Dwarf (Worker (8))
        t("earth_impling", 0, false, false, 0); // Earth impling
        t("earth_warrior", 1, false, false, 54); // Earth warrior
        t("earth_warrior_champion", 1, false, false, 0); // Earth Warrior Champion
        t("eclectic_impling", 0, false, false, 0); // Eclectic impling
        t("eclipse_moon", 5, true, true, 0); // Eclipse Moon (Clone)
        t("eldric_the_ice_king", 3, true, true, 735); // Eldric the Ice King
        t("elf_archer", 1, false, false, 105); // Elf Archer (1)
        t("elf_warrior", 1, false, false, 107.5); // Elf Warrior (1)
        t("elidinis_warden", 5, false, false, 0); // Elidinis' Warden (Damaged)
        t("elvarg", 4, false, false, 80); // Elvarg
        t("enormous_tentacle", 2, false, false, 0); // Enormous Tentacle (Enormous Tentacle)
        t("ent", 2, false, false, 0); // Ent (Wilderness)
        t("essence_impling", 0, false, false, 0); // Essence impling
        t("evil_creature", 1, false, false, 0); // Evil Creature (Izzie)
        t("fareed", 1, false, false, 0); // Fareed
        t("farmer", 1, false, false, 0); // Farmer (Green shirt, long sleeves)
        t("fever_spider", 2, false, false, 40); // Fever spider
        t("fire_giant", 2, false, false, 111, "fiery"); // Fire giant (Level 86)
        t("fire_giant_lvl104", 2, false, false, 133.5, "fiery"); // Fire giant (Level 104)
        t("fire_giant_lvl109", 2, false, false, 153.5, "fiery"); // Fire giant (Level 109)
        t("fire_warrior_of_lesarkus", 1, false, false, 0); // Fire Warrior of Lesarkus
        t("fiyr_shade", 1, true, true, 110, "shade", "spectral", "undead"); // Fiyr Shade (Shade)
        t("flambeed", 5, false, false, 0); // Flambeed
        t("flaming_pyrelord", 2, true, true, 1250, "demon"); // Flaming pyrelord
        t("flesh_crawler", 2, false, false, 25); // Flesh Crawler (Level 41)
        t("flight_kilisa", 2, false, false, 132.5, "flying"); // Flight Kilisa
        t("flockleader_geerin", 2, false, false, 132.5, "flying"); // Flockleader Geerin
        t("fragment_of_seren", 3, true, true, 0); // Fragment of Seren
        t("fumus", 1, true, true, 0); // Fumus
        t("gadderanks", 1, true, true, 0); // Gadderanks
        t("galvek", 7, true, true, 0, "dragon", "fiery"); // Galvek (Air)
        t("gargoyle", 3, true, true, 105, "golem"); // Gargoyle (Basement)
        t("gelatinnoth_mother", 3, false, true, 0); // Gelatinnoth Mother
        t("gemstone_crab", 5, true, true, 0); // Gemstone Crab
        t("general_graardor", 4, true, true, 338); // General Graardor
        t("general_khazard", 2, false, false, 0); // General Khazard
        t("ghost", 1, false, false, 25, "spectral", "undead"); // Ghost (Level 19)
        t("ghoul_champion", 1, false, false, 0, "undead"); // undead per the wiki's undead list (DPS data has no attributes) // Ghoul Champion
        t("giant_bat", 2, false, false, 32); // Giant bat (Arceuus)
        t("giant_champion", 2, false, false, 0); // Giant Champion
        t("giant_mole", 3, false, false, 215); // Giant Mole
        t("giant_rat", 2, false, false, 10, "rat"); // Giant rat (Level 6)
        t("giant_rock_crab", 2, false, false, 198); // Giant Rock Crab (1)
        t("giant_rockslug", 3, true, true, 770); // Giant rockslug
        t("giant_scarab", 3, false, false, 137, "kalphite"); // Giant Scarab
        t("giant_sea_snake", 5, true, true, 0); // Giant Sea Snake
        t("giant_skeleton", 2, false, false, 70, "undead"); // Giant skeleton (Shadow Dungeon) (1)
        t("giant_spider", 1, false, false, 50); // Giant spider (Level 50)
        t("glacies", 1, true, true, 0); // Glacies
        t("glod", 4, true, true, 0); // Glod
        t("glough_black_demon", 3, false, false, 157, "demon"); // Black demon (Level 172)
        t("gnome", 1, false, false, 0); // Gnome (Level 1, 1)
        t("gnome_guard", 1, false, false, 0); // Gnome guard (Halberd)
        t("goblin_champion", 1, false, false, 0); // Goblin Champion
        t("goblin_lvl5", 1, false, false, 12); // Goblin (Level 5)
        t("gorak", 2, false, false, 0); // Gorak (Level 145)
    }

    private static void rows1() {
        t("gourmet_impling", 0, false, false, 0); // Gourmet impling
        t("grave_scorpion", 1, false, false, 7); // Grave scorpion
        t("great_olm", 5, false, false, 0, "dragon", "xerician"); // Great Olm (Head (Normal))
        t("greater_abyssal_demon", 1, true, true, 4200, "demon"); // Greater abyssal demon
        t("greater_nechryael", 1, false, false, 210, "demon"); // Greater Nechryael (Regular)
        t("greater_skeleton_hellhound", 2, false, false, 33, "undead"); // Greater Skeleton Hellhound (Vet'ion)
        t("green_dragon", 4, false, false, 75, "dragon", "fiery"); // Green dragon (Level 79)
        t("grizzly_bear", 2, false, false, 27); // Grizzly bear (Level 21)
        t("grizzly_bear_cub", 1, false, false, 35); // Grizzly bear cub (Level 36)
        t("growler", 3, false, false, 0); // Growler
        t("guard_bandit", 1, false, false, 29); // Guard Bandit
        t("guard_dog", 1, false, false, 49); // Guard dog (Hosidius)
        t("guardian_drake", 5, true, true, 7087, "dragon"); // Guardian Drake
        t("guthan", 1, false, false, 115, "spectral"); // Guthan the Infested
        t("gwd_aviansie", 2, false, false, 83, "flying"); // Aviansie (Level 79 (1))
        t("gwd_aviansie_elite", 2, false, false, 115, "flying"); // Aviansie (Level 131)
        t("gwd_bloodveld", 2, false, false, 134, "demon"); // Bloodveld (GWD)
        t("gwd_cyclops", 2, false, false, 100); // Cyclops (Level 76)
        t("gwd_goblin", 1, false, false, 18); // Goblin (God Wars Dungeon) (Level 17)
        t("gwd_gorak", 2, false, false, 0); // Gorak (GWD)
        t("gwd_hellhound", 2, false, false, 116, "demon"); // Hellhound (Level 127)
        t("gwd_hobgoblin", 1, false, false, 52); // Hobgoblin (Hobgoblin (GWD))
        t("gwd_imp", 1, false, false, 0, "demon"); // Imp (GWD)
        t("gwd_jogre", 2, false, false, 0); // Jogre (GWD)
        t("gwd_knight_of_saradomin", 1, false, false, 0); // Knight of Saradomin (Level 101)
        t("gwd_ogre", 2, false, false, 60); // Ogre (GWD)
        t("gwd_ork", 1, false, false, 0); // Ork
        t("gwd_pyrefiend", 1, false, false, 45, "demon", "fiery"); // Pyrefiend (Level 48)
        t("gwd_saradomin_priest", 1, false, false, 0); // Saradomin priest
        t("gwd_spiritual_mage_armadyl", 2, false, false, 75, "spectral"); // Spiritual mage (Armadyl)
        t("gwd_spiritual_mage_bandos", 1, false, false, 106, "spectral"); // Spiritual mage (Bandos)
        t("gwd_spiritual_mage_saradomin", 1, false, false, 85, "spectral"); // Spiritual mage (Saradomin)
        t("gwd_spiritual_mage_zamorak", 1, false, false, 106, "spectral"); // Spiritual mage (Bandos)
        t("gwd_spiritual_ranger_armadyl", 2, false, false, 120, "spectral"); // Spiritual ranger (Armadyl)
        t("gwd_spiritual_ranger_bandos", 1, false, false, 131, "spectral"); // Spiritual ranger (Bandos)
        t("gwd_spiritual_ranger_saradomin", 1, false, false, 106, "spectral"); // Spiritual ranger (Saradomin)
        t("gwd_spiritual_ranger_zamorak", 1, false, false, 106, "spectral"); // Spiritual ranger (Zamorak)
        t("gwd_spiritual_warrior_armadyl", 2, false, false, 98, "spectral"); // Spiritual warrior (Armadyl)
        t("gwd_spiritual_warrior_bandos", 1, false, false, 135, "spectral"); // Spiritual warrior (Bandos)
        t("gwd_spiritual_warrior_saradomin", 1, false, false, 110, "spectral"); // Spiritual warrior (Saradomin)
        t("gwd_spiritual_warrior_zamorak", 1, false, false, 110, "spectral"); // Spiritual warrior (Zamorak)
        t("gwd_werewolf", 1, false, false, 100); // Werewolf (God Wars Dungeon (1))
        t("harpie_bug_swarm", 1, false, false, 25); // Harpie Bug Swarm
        t("hellhound", 2, false, false, 116, "demon"); // Hellhound (Level 122)
        t("hero", 1, false, false, 0); // Hero (1)
        t("hespori", 3, true, true, 0); // Hespori
        t("hesporis_flower", 1, false, false, 0); // Flower
        t("highwayman", 1, false, false, 0); // Highwayman (Hood)
        t("hobgoblin", 1, false, false, 29); // Hobgoblin (Hobgoblin)
        t("hobgoblin_champion", 1, false, false, 0); // Hobgoblin Champion
        t("hydra", 3, false, false, 322.5, "dragon"); // Hydra
        t("ice_queen", 1, false, false, 0); // Ice Queen
        t("ice_spider", 1, false, false, 65); // Ice spider (Normal)
        t("ice_troll_female", 1, false, false, 84); // Ice troll female (Variant 1)
        t("ice_troll_grunt", 1, false, false, 84); // Ice troll grunt
        t("ice_troll_king", 2, false, false, 161); // Ice Troll King
        t("ice_troll_runt", 1, false, false, 63); // Ice troll runt (Variant 1)
        t("ice_warrior", 1, false, false, 59); // Ice warrior (Asgarnia Ice Dungeon)
        t("icefiend", 1, false, false, 15, "demon"); // Icefiend (Level 13)
        t("imp", 1, false, false, 0, "demon"); // Imp (Normal)
        t("imp_champion", 1, false, false, 0); // Imp Champion
        t("infernal_mage", 1, false, false, 60); // Infernal Mage
        t("insatiable_bloodveld", 3, true, true, 2900, "demon"); // Insatiable Bloodveld
        t("insatiable_mutated_bloodveld", 3, true, true, 4100, "demon"); // Insatiable mutated Bloodveld
        t("iorwerth_archer", 1, false, false, 105); // Iorwerth Archer (Female)
        t("iron_dragon", 4, false, false, 169.1, "dragon", "fiery"); // Iron dragon (Standard)
        t("ithoi_the_navigator", 1, false, false, 0); // Ithoi the Navigator (Attackable)
        t("jackal", 1, false, false, 27); // Jackal
        t("jal_ak", 3, true, true, 40); // Jal-Ak
        t("jal_akrek_ket", 1, true, true, 15); // Jal-AkRek-Ket
        t("jal_akrek_mej", 1, true, true, 15); // Jal-AkRek-Mej
        t("jal_akrek_xil", 1, false, true, 15); // Jal-AkRek-Xil
        t("jal_imkot", 4, true, true, 81); // Jal-ImKot
        t("jal_mejjak", 1, true, true, 75); // Jal-MejJak
        t("jal_mejrah", 2, true, true, 25); // Jal-MejRah
        t("jal_nib", 1, true, true, 10); // Jal-Nib
        t("jal_xil", 3, false, true, 125); // Jal-Xil
        t("jal_zek", 4, false, true, 220); // Jal-Zek
        t("jaltok_jad", 5, true, true, 350); // JalTok-Jad
        t("jelly", 1, false, false, 75); // Jelly (Dark)
        t("jogre", 2, false, false, 0); // Jogre (Normal)
        t("jogre_champion", 2, false, false, 0); // Jogre Champion
        t("jonny_the_beard", 1, false, false, 0); // Jonny the Beard
        t("judge_of_yama", 3, true, true, 0, "demon"); // Judge of Yama
        t("jungle_demon", 3, false, false, 0, "demon"); // Jungle Demon
        t("jungle_horror", 2, false, false, 45); // Jungle horror (Guard)
        t("jungle_spider", 1, false, false, 50); // Jungle spider
        t("jungle_wolf", 2, false, false, 70); // Jungle Wolf
        t("kalphite_guardian", 4, false, false, 170, "kalphite"); // Kalphite Guardian
        t("kalphite_queen", 5, false, false, 535.4, "kalphite"); // Kalphite Queen (Airborne)
        t("kalphite_queen_airborne", 5, false, false, 535.4, "kalphite"); // Kalphite Queen (Airborne)
        t("kalphite_soldier", 3, false, false, 90, "kalphite"); // Kalphite Soldier (Kalphite Cave (task only))
        t("kalphite_worker", 2, false, false, 40, "kalphite"); // Kalphite Worker
        t("kalrag", 3, false, false, 0); // Kalrag
        t("kamil", 1, false, false, 146); // Kamil
        t("karamel", 1, true, true, 0); // Karamel
        t("karil", 1, false, false, 100, "spectral"); // Karil the Tainted
        t("ket_zek", 5, false, false, 160); // Ket-Zek (Standard)
        t("khazard_warlord", 1, false, false, 0); // Khazard warlord
        t("killerwatt", 1, false, false, 51); // Killerwatt (Attacking)
        t("king_black_dragon", 5, false, false, 258, "dragon", "fiery"); // King Black Dragon
        t("king_kurask", 5, true, true, 2767.4, "leafy"); // King kurask
        t("king_sand_crab", 2, false, false, 200); // King Sand Crab (Active)
        t("king_scorpion", 2, false, false, 30); // King Scorpion
        t("kraken", 4, false, false, 255); // Kraken (Kraken)
        t("kreearra", 5, true, true, 357, "flying"); // Kree'arra
        t("kril_tsutsaroth", 5, true, true, 350.5, "demon"); // K'ril Tsutsaroth
        t("kruk", 1, true, true, 0); // Kruk
        t("kurask", 3, true, true, 97, "leafy"); // Kurask (Big)
        t("lava_dragon", 4, false, false, 248, "dragon", "fiery"); // Lava dragon
        t("leon_dcour", 1, false, false, 0); // Leon d'Cour
        t("lesser_demon_champion", 2, false, false, 0, "demon"); // Lesser Demon Champion
        t("lizard", 3, false, false, 40); // Lizard
        t("lizardman", 1, true, true, 60); // Lizardman (Level 62)
        t("lizardman_brute", 1, true, true, 60); // Lizardman brute (Battlefront)
        t("lizardman_shaman", 2, true, true, 157.5); // Lizardman shaman (Lizardman Temple)
        t("loar_shade", 1, true, true, 38, "shade", "spectral", "undead"); // Loar Shade (Shade)
        t("locust_rider", 2, false, false, 92.5, "kalphite"); // Locust rider (Lancer)
        t("locust_rider_ranger", 2, false, false, 92.5, "kalphite"); // Locust rider (Ranger)
        t("long_tailed_wyvern", 3, false, false, 205, "dragon"); // Long-tailed Wyvern
        t("lucky_impling", 0, false, false, 0); // Lucky impling (Overworld)
        t("maggot_king", 5, true, true, 3675); // Maggot King (Far)
        t("magpie_impling", 0, false, false, 0); // Magpie impling
        t("malevolent_mage", 2, true, true, 1750); // Malevolent Mage
        t("mammoth", 3, false, false, 130); // Mammoth (Normal)
        t("maniacal_monkey", 1, false, false, 65); // Maniacal monkey
        t("maniacal_monkey_archer", 1, false, false, 0); // Maniacal Monkey Archer
        t("marble_gargoyle", 3, true, true, 2768, "golem"); // Marble gargoyle
        t("me", 1, false, false, 0); // Me (Type A)
        t("melzar_the_mad", 1, true, true, 0); // Melzar the Mad
        t("mercenary_captain", 1, false, false, 0); // Mercenary Captain
        t("minotaur", 1, false, false, 22); // Minotaur (Level 27)
        t("mirrorback_araxyte", 2, true, true, 58); // Mirrorback Araxyte
        t("mithril_dragon", 4, true, true, 266.5, "dragon", "fiery"); // Mithril dragon
        t("mogre", 2, true, true, 48); // Mogre
        t("molanisk", 1, false, false, 52); // Molanisk
        t("monk", 1, false, false, 0); // Monk (Level 5)
        t("monk_of_zamorak", 1, false, false, 0); // Monk of Zamorak (Level 23)
        t("monkey", 1, false, false, 6); // Monkey (monster) (Common)
        t("monkey_archer", 1, false, false, 50); // Monkey Archer (Ape Atoll gate)
        t("monkey_guard", 2, false, false, 133); // Monkey Guard (Bearded)
        t("monkey_zombie", 1, false, false, 60, "undead"); // Monkey Zombie (Level 98)
        t("monkey_zombie_lvl129", 1, false, false, 90, "undead"); // Monkey Zombie (Level 129)
        t("monkey_zombie_lvl82", 1, false, false, 60, "undead"); // Monkey Zombie (Level 82)
        t("monstrous_basilisk", 3, true, true, 1700); // Monstrous basilisk
        t("moss_guardian", 2, false, false, 120); // Moss Guardian
        t("mountain_troll", 1, false, false, 90); // Mountain troll (Level 69)
        t("mudskipper", 2, false, false, 0); // Mudskipper (Level 31)
        t("mugger", 1, false, false, 0); // Mugger (Regular)
        t("mummy", 1, false, false, 0, "undead"); // undead per the wiki's undead list (DPS data has no attributes) // Mummy (Ancient Pyramid) (1)
        t("mutated_bloodveld", 2, false, false, 170, "demon"); // Mutated Bloodveld
        t("myre_blamish_snail", 1, false, false, 0); // Myre Blamish Snail (Round)
        t("myre_blamish_snail_pointed", 1, false, false, 0); // Myre Blamish Snail (Pointed)
        t("nature_impling", 0, false, false, 0); // Nature impling
        t("nazastarool", 2, false, false, 70, "undead"); // Nazastarool (Zombie)
        t("nechryael", 1, false, false, 105, "demon"); // Nechryael (Normal)
        t("nechryarch", 2, true, true, 3280, "demon"); // Nechryarch
        t("nex", 3, true, true, 0); // Nex
        t("nezikchened", 3, false, false, 0, "demon"); // Nezikchened (Normal)
        t("night_beast", 4, true, true, 6462); // Night beast
        t("nightmare_husk", 1, true, true, 0); // Husk (Magic)
        t("nightmare_parasite", 1, true, true, 0); // Parasite (Weakened)
        t("ninja_impling", 0, false, false, 0); // Ninja impling
        t("nmz_arrg", 2, false, false, 150.5); // Arrg
        t("nmz_black_knight_titan", 1, false, false, 0); // Black Knight Titan
        t("nmz_bouncer", 2, false, false, 116, "demon"); // Bouncer
        t("nmz_count_draynor", 1, false, false, 0.8, "vampyre"); // Count Draynor
        t("nmz_dad", 3, false, false, 126); // Dad
        t("nmz_elvarg", 4, false, false, 80); // Elvarg
        t("nmz_glod", 4, true, true, 0); // Glod
        t("nmz_khazard_warlord", 1, false, false, 0); // Khazard warlord
        t("nmz_king_roald", 1, true, true, 0); // King Roald (In combat)
        t("nmz_me", 1, false, false, 0); // Me (Type A)
        t("nmz_the_kendal", 2, false, false, 0); // The Kendal
        t("nmz_tree_spirit", 1, false, false, 85, "spectral", "undead"); // Tree spirit (Lost City)
        t("nuclear_smoke_devil", 2, true, true, 2400); // Nuclear smoke devil
        t("nylocas_athanatos", 3, true, true, 0); // Nylocas Athanatos (Entry)
        t("nylocas_hagios", 1, true, true, 0); // Nylocas Hagios (Small)
        t("nylocas_ischyros", 1, true, true, 0); // Nylocas Ischyros (Small)
        t("nylocas_matomenos", 2, false, false, 0); // Nylocas Matomenos (Normal Mode Maiden)
        t("nylocas_toxobolos", 1, true, true, 0); // Nylocas Toxobolos (Small)
        t("nylocas_vasilias", 4, true, true, 0); // Nylocas Vasilias (Hard)
        t("obor", 2, true, true, 129); // Obor
        t("ochre_blamish_snail", 1, false, false, 0); // Ochre Blamish Snail (Round)
        t("ochre_blamish_snail_pointed", 1, false, false, 0); // Ochre Blamish Snail (Pointed)
        t("ogre", 2, false, false, 60); // Ogre (Level 53)
        t("ogre_lvl63", 2, false, false, 60); // Ogre (Level 63)
        t("ogre_shaman", 2, true, true, 0); // Ogre shaman (1)
        t("ogress_shaman", 2, false, false, 82); // Ogress Shaman
        t("ogress_warrior", 2, false, false, 82); // Ogress Warrior (1)
        t("olm_left_claw", 5, false, false, 0, "dragon", "xerician"); // Great Olm (Left claw (Normal))
        t("olm_right_claw", 5, false, false, 0, "dragon", "xerician"); // Great Olm (Right claw (Normal))
        t("oomlie_bird", 1, false, false, 40); // Oomlie bird
        t("otherworldly_being", 1, false, false, 66, "spectral"); // Otherworldly being
        t("paladin", 1, false, false, 0); // Paladin
        t("penance_fighter", 1, false, true, 0, "penance"); // Penance Fighter (Wave 6)
        t("penance_queen", 5, true, true, 0); // Penance Queen
        t("penance_ranger", 1, false, true, 0, "penance"); // Penance Ranger (Wave 7/10)
        t("pestilent_bloat", 5, true, true, 0, "undead"); // Pestilent Bloat (Normal)
        t("phantom_muspah", 5, true, true, 1763.7, "spectral"); // Phantom Muspah (Melee)
        t("phosanis_nightmare", 5, true, true, 0); // Phosani's Nightmare
        t("phrin_shade", 1, true, true, 56, "shade", "spectral", "undead"); // Phrin Shade (Shade)
        t("pirate", 1, false, false, 20); // Pirate (Brimhaven)
        t("pit_scorpion", 1, false, false, 32); // Pit Scorpion
        t("poison_scorpion", 2, false, false, 23); // Poison Scorpion
        t("poison_spider", 1, false, false, 64); // Poison spider (Level 64)
        t("possessed_king_roald", 1, true, true, 0); // King Roald (In combat)
        t("prifddinas_guard", 1, false, false, 107.5); // Guard (Prifddinas) (1)
        t("purple_portal", 3, true, true, 0); // Portal (Pest Control) (Blue, Intermediate/veteran)
        t("pyrefiend", 1, false, false, 45, "demon", "fiery"); // Pyrefiend (Level 43)
        t("pyrelord", 2, false, false, 80, "demon"); // Pyrelord (1)
        t("quest_arrg", 2, false, false, 150.5); // Arrg
        t("quest_barrelchest", 3, false, false, 0); // Barrelchest
        t("quest_basilisk_boss", 1, false, false, 60); // Basilisk Youngling
        t("quest_black_eye_bethel", 1, false, false, 0); // Black Eye Bethel (Dragon scimitar)
        t("quest_blood_moon", 5, true, true, 0); // Blood Moon
        t("quest_blue_moon", 5, true, true, 0); // Blue Moon
        t("quest_derwen", 3, true, true, 0); // Derwen
        t("quest_eclipse_moon", 5, true, true, 0); // Eclipse Moon (Clone)
        t("quest_fear_reaper", 2, false, false, 0); // Fear reaper (Level 42)
        t("quest_giant_lobster", 3, false, false, 0); // Giant lobster (The Red Reef)
        t("quest_giant_roc", 5, false, false, 250); // Giant Roc
        t("quest_gryphon", 2, false, false, 110); // Gryphon
        t("quest_justiciar_zachariah", 3, true, true, 0); // Justiciar Zachariah
        t("quest_kolodion", 3, false, false, 107); // Kolodion (Demon)
        t("quest_mad_angel", 3, false, false, 0, "golem"); // Mad Angel (Quest)
        t("quest_mother", 3, false, false, 0); // Mother
        t("quest_porazdir", 3, true, true, 376, "demon"); // Porazdir
        t("quest_shellbane_gryphon", 4, true, true, 400); // Shellbane gryphon
        t("quest_sigmund", 1, false, false, 0); // Sigmund (Another Slice of H.A.M.)
        t("quest_skeleton_hellhound", 2, false, false, 55, "demon"); // Skeleton Hellhound
        t("quest_slagilith", 2, false, false, 0); // Slagilith
        t("quest_sourhog", 3, false, false, 40); // Sourhog
        t("ranis_drakan", 1, true, true, 400, "vampyre"); // Ranis Drakan (A Taste of Hope)
        t("rat", 1, false, false, 2, "rat"); // Rat (Regular)
        t("ravager", 1, false, false, 0); // Ravager (Level 106)
        t("reanimated_demon_spawn", 1, true, true, 0, "demon"); // Reanimated demon spawn
        t("red_dragon", 4, false, false, 143.4, "dragon", "fiery"); // Red dragon (1)
        t("red_portal", 3, true, true, 0); // Portal (Pest Control) (Blue, Intermediate/veteran)
        t("repugnant_spectre", 3, true, true, 4085, "spectral", "undead"); // Repugnant spectre
        t("respiratory_system", 2, false, false, 0, "demon"); // Respiratory system
        t("revenant_cyclops", 3, false, false, 119, "spectral", "undead"); // Revenant cyclops
        t("revenant_dark_beast", 3, false, false, 157, "spectral", "undead"); // Revenant dark beast
        t("revenant_demon", 3, false, false, 86, "spectral", "undead"); // Revenant demon
        t("revenant_dragon", 5, false, false, 186, "spectral", "undead"); // Revenant dragon
        t("revenant_goblin", 1, false, false, 14, "spectral", "undead"); // Revenant goblin
        t("revenant_hellhound", 3, false, false, 86, "spectral", "undead"); // Revenant hellhound
        t("revenant_hobgoblin", 1, false, false, 73, "spectral", "undead"); // Revenant hobgoblin
        t("revenant_imp", 1, false, false, 10, "spectral", "undead"); // Revenant imp
        t("revenant_knight", 1, false, false, 168, "spectral", "undead"); // Revenant knight
    }

    private static void rows2() {
        t("revenant_ork", 3, false, false, 115, "spectral", "undead"); // Revenant ork
        t("revenant_pyrefiend", 1, false, false, 48, "spectral", "undead"); // Revenant pyrefiend
        t("riyl_shade", 1, true, true, 76, "shade", "spectral", "undead"); // Riyl Shade (Shade)
        t("rock_crab", 1, false, false, 50); // Rock Crab (Active)
        t("rock_lobster", 2, false, false, 0); // Rock lobster (Active)
        t("rockslug", 1, false, false, 27); // Rockslug (Adult)
        t("rune_dragon", 5, true, true, 363, "dragon", "fiery"); // Rune dragon
        t("ruptura_araxyte", 2, true, true, 65); // Ruptura Araxyte
        t("sand_crab", 1, false, false, 60); // Sand Crab (Active)
        t("sarachnis", 5, true, true, 430); // Sarachnis
        t("scarab_mage", 1, false, false, 51.2, "kalphite"); // Scarab Mage (Level 93)
        t("scorpia", 5, false, false, 260); // Scorpia
        t("scorpias_guardian", 1, false, false, 70); // Scorpia's guardian
        t("scorpias_offspring", 1, false, false, 2); // Scorpia's offspring (monster)
        t("scorpion", 2, false, false, 17); // Scorpion (Level 14)
        t("screaming_banshee", 3, true, true, 610, "spectral", "undead"); // Screaming banshee
        t("screaming_twisted_banshee", 3, true, true, 2200, "spectral", "undead"); // Screaming twisted banshee
        t("scurrius", 3, false, false, 2137.5, "rat"); // Scurrius (Group)
        t("scurrius_giant_rat", 2, false, false, 15, "rat"); // Giant rat (Scurrius)
        t("sea_snake_young", 2, false, false, 85); // Sea Snake Young
        t("sea_troll_queen", 5, false, false, 0); // Sea Troll Queen
        t("seagull", 1, false, false, 5); // Seagull (Level 2)
        t("sergeant_grimspike", 1, false, false, 146); // Sergeant Grimspike
        t("sergeant_steelwill", 1, false, false, 127); // Sergeant Steelwill
        t("sergeant_strongstack", 1, false, false, 128); // Sergeant Strongstack
        t("shadow_hound", 1, false, false, 62); // Shadow Hound
        t("shadow_of_light", 1, false, false, 0); // Shadow (Temple of Light)
        t("shadow_spider", 1, false, false, 55); // Shadow spider
        t("shadow_warrior", 1, false, false, 67); // Shadow warrior
        t("shadow_wyrm", 5, true, true, 3520, "dragon"); // Shadow Wyrm (Attacking)
        t("shayzien_crypt_skeleton_magic", 1, false, false, 54, "undead"); // Skeleton (Shayzien Crypts) (Magic)
        t("shayzien_crypt_skeleton_melee", 1, false, false, 54, "undead"); // Skeleton (Shayzien Crypts) (Magic)
        t("shayzien_crypt_skeleton_ranged", 1, false, false, 54, "undead"); // Skeleton (Shayzien Crypts) (Magic)
        t("shayzien_crypt_zombie_magic", 1, false, false, 54.1, "undead"); // Zombie (Shayzien Crypts) (Magic)
        t("shayzien_crypt_zombie_melee", 1, false, false, 54.1, "undead"); // Zombie (Shayzien Crypts) (Magic)
        t("shayzien_crypt_zombie_ranged", 1, false, false, 54.1, "undead"); // Zombie (Shayzien Crypts) (Magic)
        t("shellbane_gryphon", 4, true, true, 400); // Shellbane gryphon
        t("shifter", 1, true, true, 0); // Shifter (Level 104)
        t("sir_mordred", 1, false, false, 0); // Sir Mordred
        t("skeletal_wyvern", 3, false, false, 210, "dragon"); // Skeletal Wyvern (1)
        t("skeleton_champion", 1, false, false, 0, "undead"); // Skeleton Champion
        t("skeleton_hellhound", 2, false, false, 33, "undead"); // Skeleton Hellhound (Vet'ion)
        t("skeleton_lvl142", 1, false, false, 110, "undead"); // Skeleton (Ape Atoll)
        t("skeleton_lvl25", 1, false, false, 0, "undead"); // Skeleton (Level 25, 1)
        t("skeleton_lvl45", 1, false, false, 0, "undead"); // Skeleton (Level 45, 1)
        t("skotizo", 5, true, true, 618.5, "demon"); // Skotizo
        t("slash_bash", 3, false, false, 100, "undead"); // Slash Bash
        t("slug_prince", 2, true, true, 0); // Slug Prince
        t("small_lizard", 1, false, false, 15); // Small Lizard (Green)
        t("smoke_devil", 1, true, true, 185); // Smoke devil
        t("snakeling", 1, true, true, 0); // Snakeling (Magic)
        t("sol_heredit", 5, true, true, 0); // Sol Heredit
        t("sorebones", 1, false, false, 22, "undead"); // Sorebones (Apron)
        t("sotetseg", 5, true, true, 0); // Sotetseg (Normal)
        t("sourhog", 3, false, false, 40); // Sourhog
        t("spawn_of_sarachnis", 2, false, false, 30.7); // Spawn of Sarachnis (Level 107)
        t("spiked_turoth", 3, true, true, 1998, "leafy"); // Spiked Turoth
        t("spindel", 4, true, true, 541); // Spindel
        t("spinner", 1, false, false, 0); // Spinner (Level 74)
        t("spinolyp", 1, false, false, 0); // Spinolyp (Spinolyp)
        t("spiritual_mage", 1, false, false, 85, "spectral"); // Spiritual mage (Saradomin)
        t("spiritual_ranger", 1, false, false, 131, "spectral"); // Spiritual ranger (Bandos)
        t("spiritual_warrior", 2, false, false, 98, "spectral"); // Spiritual warrior (Armadyl)
        t("spitting_wyvern", 3, false, false, 205, "dragon"); // Spitting Wyvern
        t("splatter", 1, false, false, 0); // Splatter (Level 54)
        t("starlight", 2, false, false, 0); // Starlight
        t("steel_dragon", 4, false, false, 215.2, "dragon", "fiery"); // Steel dragon (Level 246)
        t("sulphur_nagua", 2, false, false, 105, "spectral"); // Sulphur Nagua
        t("suqah", 2, false, false, 107.6); // Suqah (1)
        t("taloned_wyvern", 3, false, false, 205, "dragon"); // Taloned Wyvern
        t("tanglefoot", 1, true, true, 0); // Tanglefoot
        t("temple_guardian", 2, false, false, 45); // Temple Guardian
        t("temple_spider", 1, false, false, 70); // Temple Spider
        t("terror_dog", 1, false, false, 82); // Terror dog (Level 100)
        t("terrorbird", 2, false, false, 34); // Terrorbird (1)
        t("the_hueycoatl", 1, true, true, 0, "dragon"); // The Hueycoatl (Body)
        t("the_inadequacy", 5, true, true, 0); // The Inadequacy
        t("the_kendal", 2, false, false, 0); // The Kendal
        t("the_leviathan", 7, false, false, 1777.4); // The Leviathan (Post-quest)
        t("the_maiden_of_sugadinti", 6, true, true, 0); // The Maiden of Sugadinti (30% Health)
        t("the_mimic", 5, true, true, 0); // The Mimic
        t("the_nightmare", 5, true, true, 0); // The Nightmare
        t("the_whisperer", 3, true, true, 900); // The Whisperer (Post-quest)
        t("thermonuclear_smoke_devil", 4, true, true, 240); // Thermonuclear smoke devil
        t("thrower_troll", 1, false, false, 95); // Thrower Troll
        t("titan_fire_elemental", 1, false, false, 0); // Fire elemental (Royal Titans)
        t("titan_ice_elemental", 1, false, false, 0); // Ice elemental (Royal Titans)
        t("toa_arcane_scarab", 3, true, true, 0, "kalphite"); // Arcane Scarab
        t("toa_baboon_brawler", 1, true, true, 0); // Baboon Brawler (Level 56)
        t("toa_baboon_mage", 1, true, true, 0); // Baboon Mage (Level 56)
        t("toa_baboon_thrower", 1, true, true, 0); // Baboon Thrower (Level 56)
        t("toa_scarab_swarm", 1, true, true, 0); // Scarab Swarm (Tombs of Amascut)
        t("toa_soldier_scarab", 3, true, true, 0, "kalphite"); // Soldier Scarab
        t("toa_spitting_scarab", 3, true, true, 0, "kalphite"); // Spitting Scarab
        t("tok_xil", 3, false, false, 40); // Tok-Xil (Standard)
        t("torag", 1, false, false, 112.5, "spectral"); // Torag the Corrupted
        t("torcher", 1, false, false, 0); // Torcher (Level 79)
        t("tormented_demon", 3, false, false, 1065, "demon"); // Tormented Demon (1)
        t("tortured_gorilla", 2, false, false, 210); // Tortured gorilla (Level 141)
        t("tortured_soul", 1, false, false, 51, "spectral", "undead"); // Tortured soul
        t("tree_spirit", 1, false, false, 0); // Tree spirit (Level 79)
        t("trek_ghast_1", 2, false, false, 0, "spectral"); // Ghast (Level 79)
        t("trek_ghast_2", 2, false, false, 0, "spectral"); // Ghast (Level 109)
        t("trek_ghast_3", 2, false, false, 0, "spectral"); // Ghast (Level 139)
        t("trek_giant_snail_1", 3, false, false, 0); // Giant snail (Level 80)
        t("trek_giant_snail_2", 3, false, false, 0); // Giant snail (Level 109)
        t("trek_giant_snail_3", 3, false, false, 0); // Giant snail (Level 139)
        t("trek_nail_beast_1", 2, false, false, 0); // Nail beast (Level 69)
        t("trek_nail_beast_2", 2, false, false, 0); // Nail beast (Level 98)
        t("trek_nail_beast_3", 2, false, false, 0); // Nail beast (Level 141)
        t("trek_shade_1", 1, false, false, 76, "spectral", "undead"); // Riyl shadow (Temple Trekking)
        t("trek_shade_2", 1, false, false, 94, "spectral", "undead"); // Asyn shadow (Temple Trekking)
        t("trek_shade_3", 1, false, false, 115, "spectral", "undead"); // Shade (Catacombs of Kourend)
        t("trek_swamp_snake_1", 3, false, false, 0); // Swamp snake (Level 80)
        t("trek_swamp_snake_2", 4, false, false, 0); // Swamp snake (Level 109)
        t("trek_swamp_snake_3", 5, false, false, 0); // Swamp snake (Level 139)
        t("trek_undead_lumberjack_1", 1, false, false, 0, "undead"); // Undead Lumberjack (Level 30)
        t("trek_undead_lumberjack_2", 1, false, false, 0, "undead"); // Undead Lumberjack (Level 50)
        t("trek_undead_lumberjack_3", 1, false, false, 0, "undead"); // Undead Lumberjack (Level 70)
        t("trek_vampyre_juvinate_1", 1, true, true, 50, "vampyre"); // Vampyre Juvinate (Level 59 (Temple Trekking))
        t("trek_vampyre_juvinate_2", 1, true, true, 100, "vampyre"); // Vampyre Juvinate (Level 90 (Temple Trekking))
        t("trek_vampyre_juvinate_3", 1, true, true, 150, "vampyre"); // Vampyre Juvinate (Level 119 (Sins of the Father))
        t("trek_zombie", 1, false, false, 22, "undead"); // Zombie (Level 13, 1)
        t("treus_dayth", 1, true, false, 100, "spectral", "undead"); // Treus Dayth (In combat)
        t("troll_general", 2, false, false, 150.5); // Troll general (Hammer (brown))
        t("troll_spectator", 1, false, false, 90); // Troll spectator
        t("tstanon_karlak", 3, false, false, 142, "demon"); // Tstanon Karlak
        t("tumekens_warden", 5, false, false, 0); // Tumeken's Warden (Damaged)
        t("turoth", 2, false, false, 79, "leafy"); // Turoth (Mum)
        t("twisted_banshee", 2, false, false, 100, "spectral", "undead"); // Twisted Banshee
        t("tz_kek", 2, false, false, 20); // Tz-Kek (Level 45)
        t("tz_kek_small", 1, false, false, 10); // Tz-Kek (Level 22)
        t("tz_kih", 1, false, false, 10); // Tz-Kih
        t("tzhaar_hur_citizen", 1, false, false, 80); // TzHaar-Hur
        t("tzhaar_ket", 1, false, false, 140); // TzHaar-Ket (Level 149)
        t("tzhaar_mej", 1, false, false, 100); // TzHaar-Mej
        t("tzhaar_xil", 1, false, false, 120); // TzHaar-Xil (Knife)
        t("tzkal_zuk", 7, true, true, 101890); // TzKal-Zuk (Enraged)
        t("tztok_jad", 5, false, false, 25250); // TzTok-Jad
        t("ulfric", 1, false, true, 60, "undead"); // Ulfric
        t("umbra", 1, true, true, 0); // Umbra
        t("undead_chicken", 1, false, false, 3, "undead"); // Undead chicken
        t("undead_cow", 2, false, false, 8, "undead"); // Undead cow
        t("undead_druid", 1, false, false, 147, "undead"); // Undead Druid
        t("undead_one", 1, false, false, 59, "undead"); // Undead one (Level 73)
        t("urium_shade", 1, true, true, 130, "shade", "spectral", "undead"); // Urium Shade (Shade)
        t("vampyre_juvenile", 1, true, true, 60, "vampyre"); // Vampyre Juvenile (Burgh de Rott and Crombwick Manor)
        t("vanstrom_klause", 1, true, true, 0, "vampyre"); // Vanstrom Klause (Vampyre)
        t("vardorvis", 2, true, true, 1085); // Vardorvis (Post-quest)
        t("venenatis", 4, true, true, 956.2); // Venenatis
        t("verac", 1, false, false, 112.5, "spectral"); // Verac the Defiled
        t("verzik_vitur", 5, false, false, 0); // Verzik Vitur (Hard mode, Phase 1)
        t("verzik_vitur_p2", 3, false, false, 0); // Verzik Vitur (Hard mode, Phase 2)
        t("verzik_vitur_p3", 7, false, false, 0); // Verzik Vitur (Hard mode, Phase 3)
        t("vetion", 3, true, true, 322, "undead"); // Vet'ion (Enraged)
        t("vitreous_jelly", 2, true, true, 1900); // Vitreous Jelly
        t("vitreous_warped_jelly", 3, true, true, 2200); // Vitreous Warped Jelly
        t("void_knight", 0, false, false, 0); // Void Knight (Pest Control) (1)
        t("vorkath", 7, true, true, 750, "dragon", "fiery", "undead"); // Vorkath (Post-quest)
        t("vulture", 2, true, false, 10); // Vulture (Flying)
        t("vyrewatch_sentinel", 1, true, true, 150, "vampyre"); // Vyrewatch Sentinel (1)
        t("wall_beast", 1, true, true, 105); // Wall beast (Beast)
        t("wallasalki", 1, false, false, 0); // Wallasalki (1)
        t("warped_jelly", 1, false, false, 140); // Warped Jelly
        t("warped_terrorbird", 2, false, false, 200); // Warped Terrorbird (Level 138)
        t("warriors_guild_cyclops", 2, false, false, 75); // Cyclops (Level 56)
        t("warriors_guild_cyclops_76", 2, false, false, 100); // Cyclops (Level 76)
        t("waterfiend", 1, true, true, 128, "demon"); // Waterfiend (Normal)
        t("white_knight", 1, false, false, 0); // White Knight (Initiate (f))
        t("wild_dog", 1, false, false, 62); // Wild dog (Normal)
        t("wilderness_bandit", 1, false, false, 158.8); // Bandit (Level 130)
        t("wingman_skree", 2, false, false, 124, "flying"); // Wingman Skree
        t("witchs_experiment", 2, false, false, 0); // Witch's experiment (Wolf)
        t("woman", 1, false, false, 0); // Woman (1)
        t("wyrm", 3, true, true, 123, "dragon"); // Wyrm (Attacking)
        t("xarpus", 5, false, false, 0); // Xarpus (Normal mode)
        t("yak", 2, true, true, 0); // Yak
        t("yama", 5, true, true, 0, "demon"); // Yama (Normal)
        t("yama_void_flare", 2, false, false, 0, "demon"); // Void Flare (Fire)
        t("yellow_portal", 3, true, true, 0); // Portal (Pest Control) (Blue, Intermediate/veteran)
        t("young_impling", 0, false, false, 0); // Young impling
        t("yt_hurkot", 1, false, false, 60); // Yt-HurKot (Level 108)
        t("yt_hurkot_inferno", 1, false, false, 90); // Yt-HurKot (Level 141)
        t("yt_mejkot", 4, false, false, 80); // Yt-MejKot (Standard)
        t("zakln_gritch", 2, false, false, 150, "demon"); // Zakl'n Gritch
        t("zamorak_crafter", 1, false, false, 0); // Zamorak crafter
        t("zamorak_mage", 1, false, false, 0); // Zamorak mage (Level 84)
        t("zamorak_ranger", 1, false, false, 0); // Zamorak ranger (Level 81)
        t("zamorak_warrior", 1, false, false, 0); // Zamorak warrior (Level 84)
        t("zogre", 2, false, false, 71, "undead"); // Zogre
        t("zombie_lvl18", 1, false, false, 0, "undead"); // Zombie (Level 18, 1)
        t("zombie_lvl24", 1, false, false, 0, "undead"); // Zombie (Level 24, 1)
        t("zombie_pirate", 1, false, false, 52, "undead"); // Zombie pirate (Braindeath Island) (1)
        t("zombie_rat", 2, false, false, 2, "rat", "undead"); // Zombie rat (1)
        t("zombies_champion", 2, false, false, 0, "undead"); // undead per the wiki's undead list (DPS data has no attributes) // Zombies Champion
        t("zombified_spawn", 1, true, true, 0, "undead"); // Zombified Spawn (Post-quest)
        t("zulrah", 5, true, true, 500); // Zulrah (Magma)
        t("zulrah_magma", 5, true, true, 500); // Zulrah (Magma)
        t("zulrah_tanzanite", 5, true, true, 500); // Zulrah (Magma)
        // Round 17 slayer task monsters (SlayerTaskMonsters2)
        t("albino_bat", 2, false, false, 33); // Albino bat
        t("crypt_rat", 1, false, false, 35, "rat"); // Crypt rat
        t("giant_crypt_rat", 2, false, false, 70, "rat"); // Giant crypt rat
        t("crypt_spider", 1, false, false, 45); // Crypt spider
        t("giant_crypt_spider", 1, false, false, 80); // Giant crypt spider
        t("bloodworm", 1, false, false, 0); // Bloodworm
        t("sulphur_lizard", 2, false, false, 50); // Sulphur Lizard
        t("araxyte", 1, false, false, 60); // Araxyte
        t("araxyte_lvl146", 2, false, false, 100); // Araxyte
        t("feral_vampyre", 1, false, false, 40, "vampyre"); // Feral Vampyre
        t("ice_wolf", 2, false, false, 70); // Ice wolf
        t("ice_wolf_lvl132", 2, false, false, 70); // Ice wolf
        t("pee_hat", 2, false, false, 126); // Pee Hat
        t("kraka", 2, false, false, 126); // Kraka
        t("troll_stick", 2, false, false, 141.5); // Stick
        t("troll_rock", 2, false, false, 150.5); // Rock (Troll)
        t("ancient_zygomite", 2, false, false, 154); // Ancient Zygomite
        t("wyrmling", 2, false, false, 65, "dragon"); // Wyrmling
        t("grimy_lizard", 2, true, true, 75); // Grimy Lizard
        t("frost_crab", 1, false, false, 60); // Frost Crab
        t("chilled_jelly", 1, false, false, 140); // Chilled jelly
        t("earthen_nagua", 2, false, false, 165, "spectral"); // Earthen Nagua
        t("juvenile_custodian_stalker", 1, true, true, 135); // Juvenile custodian stalker
        t("mature_custodian_stalker", 1, true, true, 190); // Mature custodian stalker
        t("elder_custodian_stalker", 1, true, true, 250); // Elder custodian stalker
        t("skogre", 2, false, false, 71, "undead"); // Skogre
        t("rooster", 1, true, true, 7); // Rooster (2)
    }
}
