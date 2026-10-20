package com.runecraft.osrs.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The parts of a monster's defence that {@link MonsterStatsTable} has no column for, from the OSRS Wiki DPS calculator's
 * {@code monsters.json} (weirdgloop/osrs-dps-calc): the light and heavy ranged defence (the table's ranged defence is the
 * "standard" one, used by bows; thrown weapons roll against light, crossbows, ballistae and chinchompas against heavy) and
 * the elemental weakness of the 2024 rework (element and severity in percent: a matching elemental spell gets that much
 * extra accuracy and damage). Rows were matched to the table's rows by name, version and stats (round 15, generator
 * {@code src/test/resources/osrs/combat/generator/monster_defence.py}); a creature that is not listed has light = heavy =
 * standard and no weakness. Applied together with the table's row by {@link MonsterStatsTable#apply}.
 */
public final class MonsterDefenceTable {
    /** Light / heavy ranged defence and elemental weakness (null element: none). */
    public record Row(String id, int lightRanged, int heavyRanged, String weakness, int weaknessPercent) {
    }

    private static final Map<String, Row> ROWS = new HashMap<>();

    static {
        load();
    }

    private MonsterDefenceTable() {
    }

    /** The row for a creature id, or null. */
    public static Row row(String id) {
        return ROWS.get(id);
    }

    public static Map<String, Row> rows() {
        return Collections.unmodifiableMap(ROWS);
    }

    private static void x(String id, int light, int heavy, String weakness, int percent) {
        if (ROWS.put(id, new Row(id, light, heavy, weakness, percent)) != null) {
            throw new IllegalStateException("Duplicate monster defence row " + id);
        }
    }

    private static void load() {
        x("giant_bat", 8, 8, "air", 10); // Giant bat (Normal)
        x("grizzly_bear", 0, 0, "fire", 20); // Grizzly bear (Level 21)
        x("scorpion", 5, 5, "fire", 25); // Scorpion (Level 14)
        x("king_scorpion", 5, 5, "fire", 20); // King Scorpion
        x("ghost", 5, 5, "air", 50); // Ghost (Level 19)
        x("icefiend", 0, 0, "fire", 100); // Icefiend (Level 13)
        x("ice_warrior", 30, 30, "fire", 100); // Ice warrior (Normal)
        x("cave_bug", 10, 10, "fire", 50); // Cave bug (Level 6)
        x("cave_crawler", 10, 10, "fire", 50); // Cave crawler (Icy (1))
        x("banshee", 5, 5, "air", 30); // Banshee
        x("cave_slime", 0, 0, "earth", 50); // Cave slime
        x("rockslug", 0, 0, "earth", 25); // Rockslug (Adult)
        x("pyrefiend", 10, 10, "water", 100); // Pyrefiend (Level 43)
        x("mogre", 0, 0, "earth", 20); // Mogre
        x("harpie_bug_swarm", 10, 10, "fire", 50); // Harpie Bug Swarm
        x("killerwatt", -10, -10, "air", 60); // Killerwatt (Attacking)
        x("molanisk", 55, 55, "earth", 60); // Molanisk
        x("basilisk", 0, 0, "earth", 40); // Basilisk
        x("sea_snake_young", 0, 0, "earth", 20); // Sea Snake Young
        x("fever_spider", 15, 15, "fire", 25); // Fever spider
        x("jelly", 0, 0, "earth", 35); // Jelly (Regular)
        x("mutated_zygomite", 20, 20, "fire", 40); // Zygomite (Level 74)
        x("cave_horror", 0, 0, "fire", 30); // Cave horror (Alpha)
        x("aberrant_spectre", 15, 15, "air", 50); // Aberrant spectre
        x("spiritual_ranger", 13, 13, "air", 30); // Spiritual ranger (Bandos)
        x("spiritual_warrior", 35, 35, "air", 30); // Spiritual warrior (Armadyl)
        x("spiritual_mage", 2, 2, "air", 30); // Spiritual mage (Saradomin)
        x("dust_devil", 0, 0, "air", 35); // Dust devil (Smoke Dungeon)
        x("spitting_wyvern", 120, 120, "air", 25); // Spitting Wyvern
        x("taloned_wyvern", 120, 120, "air", 35); // Taloned Wyvern
        x("ankou", 0, 0, "air", 40); // Ankou (Level 75)
        x("hellhound", 0, 0, "water", 50); // Hellhound (Level 122)
        x("fire_giant", 0, 0, "water", 100); // Fire giant (Level 86)
        x("kalphite_worker", 10, 10, "fire", 40); // Kalphite Worker
        x("kalphite_soldier", 50, 50, "fire", 40); // Kalphite Soldier (Kalphite Cave (task only))
        x("kalphite_guardian", 50, 50, "fire", 40); // Kalphite Guardian
        x("ice_troll", 0, 0, "fire", 100); // Ice troll male (Variant 1)
        x("ogre", 0, 0, "earth", 20); // Ogre (Level 53)
        x("ogre_lvl63", 0, 0, "earth", 20); // Ogre (Level 63)
        x("zombie_lvl18", 0, 0, "fire", 50); // Soulless
        x("zombie_lvl24", 11, 11, "fire", 50); // Zombie (Melzar's Maze)
        x("skeleton_lvl25", 4, 4, "earth", 35); // Skeleton (Level 25, 1)
        x("ent", 30, 30, "fire", 40); // Ent (Wilderness)
        x("dagannoth", 0, 0, "earth", 35); // Dagannoth (Level 74 (1))
        x("lizardman", -10, 0, null, 0); // Lizardman (Level 62)
        x("lizardman_shaman", -10, 0, null, 0); // Lizardman shaman (Standard)
        x("waterfiend", 100, 20, "earth", 100); // Waterfiend (Normal)
        x("wyrm", 20, 10, "earth", 50); // Wyrm (Attacking)
        x("skeletal_wyvern", 140, 140, "fire", 25); // Skeletal Wyvern (1)
        x("gargoyle", 20, -20, "earth", 40); // Gargoyle (Basement)
        x("cave_kraken", 100, 100, "earth", 50); // Cave kraken (Cave kraken)
        x("dark_beast", 100, 100, "earth", 60); // Dark beast
        x("drake", 100, 0, "water", 50); // Drake
        x("smoke_devil", 44, 44, "air", 30); // Smoke devil
        x("hydra", 0, 0, "earth", 40); // Hydra
        x("basilisk_knight", 20, -15, "earth", 40); // Basilisk Knight
        x("baby_green_dragon", 30, 30, "water", 50); // Baby green dragon (1)
        x("baby_blue_dragon", 30, 5, "water", 50); // Baby blue dragon (1)
        x("baby_red_dragon", 30, 10, "water", 50); // Baby red dragon (1)
        x("baby_black_dragon", 30, 5, "water", 50); // Baby black dragon (Normal)
        x("green_dragon", 50, 10, "water", 50); // Green dragon (Level 79)
        x("blue_dragon", 50, 10, "water", 50); // Blue dragon (1)
        x("red_dragon", 50, 10, "water", 50); // Red dragon (1)
        x("black_dragon", 50, 10, "water", 50); // Black dragon (Level 227)
        x("brutal_green_dragon", 50, 10, "water", 50); // Brutal green dragon
        x("brutal_red_dragon", 50, 10, "water", 50); // Brutal red dragon
        x("brutal_black_dragon", 50, 10, "water", 50); // Brutal black dragon
        x("lava_dragon", 70, 20, "water", 50); // Lava dragon
        x("bronze_dragon", 90, 10, "earth", 50); // Bronze dragon (Standard)
        x("iron_dragon", 90, 10, "earth", 50); // Iron dragon (Standard)
        x("steel_dragon", 90, 10, "earth", 50); // Steel dragon (Level 246)
        x("mithril_dragon", 90, 20, "earth", 50); // Mithril dragon
        x("adamant_dragon", 95, 65, "earth", 50); // Adamant dragon
        x("rune_dragon", 95, 50, "earth", 50); // Rune dragon
        x("chasm_crawler", 40, 40, null, 0); // Chasm Crawler (Standard)
        x("screaming_banshee", 13, 13, "air", 40); // Screaming banshee
        x("giant_rockslug", 0, 0, "earth", 20); // Giant rockslug
        x("flaming_pyrelord", 18, 18, "water", 100); // Flaming pyrelord
        x("vitreous_jelly", 0, 0, "earth", 30); // Vitreous Jelly
        x("abhorrent_spectre", 30, 30, "air", 50); // Abhorrent spectre
        x("choke_devil", 0, 0, "air", 25); // Choke devil
        x("shadow_wyrm", 20, 20, "earth", 50); // Shadow Wyrm (Attacking)
        x("marble_gargoyle", 50, 50, "earth", 40); // Marble gargoyle
        x("guardian_drake", 150, 0, "water", 50); // Guardian Drake
        x("nuclear_smoke_devil", 80, 80, "air", 20); // Nuclear smoke devil
        x("basilisk_sentinel", 10, 10, "earth", 40); // Basilisk Sentinel
        x("screaming_twisted_banshee", 25, 25, "air", 40); // Screaming twisted banshee
        x("vitreous_warped_jelly", 0, 0, "earth", 30); // Vitreous Warped Jelly
        x("repugnant_spectre", 115, 115, "air", 40); // Repugnant spectre
        x("gwd_aviansie", -20, 0, "air", 45); // Aviansie (Level 79 (1))
        x("gwd_aviansie_elite", -20, 0, "air", 45); // Aviansie (Level 131)
        x("gwd_spiritual_warrior_armadyl", 35, 35, "air", 30); // Spiritual warrior (Armadyl)
        x("gwd_spiritual_ranger_armadyl", 41, 41, "air", 30); // Spiritual ranger (Armadyl)
        x("gwd_spiritual_mage_armadyl", 28, 28, "air", 30); // Spiritual mage (Armadyl)
        x("gwd_ork", 13, 13, "earth", 15); // Ork
        x("gwd_ogre", 0, 0, "earth", 20); // Ogre (GWD)
        x("gwd_jogre", 0, 0, "earth", 25); // Jogre (GWD)
        x("gwd_spiritual_warrior_bandos", 13, 13, "air", 30); // Spiritual warrior (Bandos)
        x("gwd_spiritual_ranger_bandos", 13, 13, "air", 30); // Spiritual ranger (Bandos)
        x("gwd_spiritual_mage_bandos", 13, 13, "air", 30); // Spiritual mage (Bandos)
        x("gwd_spiritual_warrior_saradomin", 13, 13, "air", 30); // Spiritual warrior (Saradomin)
        x("gwd_spiritual_ranger_saradomin", 23, 23, "air", 30); // Spiritual ranger (Saradomin)
        x("gwd_spiritual_mage_saradomin", 2, 2, "air", 30); // Spiritual mage (Saradomin)
        x("gwd_imp", -42, -42, "water", 10); // Imp (GWD)
        x("gwd_pyrefiend", 10, 10, "water", 100); // Pyrefiend (Level 48)
        x("gwd_spiritual_warrior_zamorak", 0, 0, "air", 30); // Spiritual warrior (Zamorak)
        x("gwd_spiritual_ranger_zamorak", 0, 0, "air", 30); // Spiritual ranger (Zamorak)
        x("gwd_spiritual_mage_zamorak", 0, 0, "air", 30); // Spiritual mage (Zamorak)
        x("flight_kilisa", 0, 0, "air", 30); // Flight Kilisa
        x("wingman_skree", 0, 0, "air", 30); // Wingman Skree
        x("flockleader_geerin", 0, 0, "air", 30); // Flockleader Geerin
        x("tstanon_karlak", 0, 0, "water", 40); // Tstanon Karlak
        x("zakln_gritch", 0, 0, "water", 40); // Zakl'n Gritch
        x("balfrug_kreeyath", 0, 0, "water", 25); // Balfrug Kreeyath
        x("skeleton_hellhound", 266, 266, "earth", 30); // Skeleton Hellhound (Vet'ion)
        x("greater_skeleton_hellhound", 275, 275, "earth", 40); // Greater Skeleton Hellhound (Vet'ion)
        x("scorpias_offspring", 0, 0, "fire", 35); // Scorpia's offspring (monster)
        x("scorpias_guardian", 0, 0, "fire", 35); // Scorpia's guardian
        x("revenant_imp", 0, 0, "air", 30); // Revenant imp
        x("revenant_goblin", 31, 31, "air", 30); // Revenant goblin
        x("revenant_pyrefiend", 10, 10, "air", 30); // Revenant pyrefiend
        x("revenant_hobgoblin", 50, 20, "air", 30); // Revenant hobgoblin
        x("revenant_cyclops", 135, 95, "air", 30); // Revenant cyclops
        x("revenant_hellhound", 110, 140, "air", 30); // Revenant hellhound
        x("revenant_demon", 90, 90, "air", 30); // Revenant demon
        x("revenant_ork", 148, 118, "air", 30); // Revenant ork
        x("revenant_dark_beast", 158, 118, "air", 30); // Revenant dark beast
        x("revenant_knight", 190, 150, "air", 30); // Revenant knight
        x("revenant_dragon", 197, 157, "air", 30); // Revenant dragon
        x("zombified_spawn", 3, 3, "fire", 50); // Zombified Spawn (Post-quest)
        x("enormous_tentacle", 270, 270, "earth", 50); // Enormous Tentacle (Enormous Tentacle)
        x("hesporis_flower", 0, 0, "fire", 40); // Cave goblin (monster) (Backpack)
        x("spawn_of_sarachnis", 150, 20, null, 0); // Spawn of Sarachnis (Level 107)
        x("nightmare_parasite", 100, 100, "fire", 50); // Parasite (Weakened)
        x("acidic_araxyte", 25, 25, "fire", 50); // Acidic Araxyte
        x("mirrorback_araxyte", 25, 25, "fire", 50); // Mirrorback Araxyte
        x("ruptura_araxyte", 25, 25, "fire", 50); // Ruptura Araxyte
        x("titan_ice_elemental", 0, 0, "fire", 100); // Ice elemental (Royal Titans)
        x("titan_fire_elemental", 0, 0, "water", 100); // Fire elemental (Royal Titans)
        x("tz_kih", 0, 0, "water", 40); // Tz-Kih
        x("tz_kek", 0, 0, "water", 40); // Tz-Kek (Level 45)
        x("tz_kek_small", 0, 0, "water", 40); // Tz-Kek (Level 22)
        x("tok_xil", 0, 0, "water", 40); // Tok-Xil (Standard)
        x("yt_mejkot", 0, 0, "water", 40); // Yt-MejKot (Standard)
        x("ket_zek", 0, 0, "water", 40); // Ket-Zek (Standard)
        x("yt_hurkot", 100, 100, "water", 40); // Yt-HurKot (Level 108)
        x("yt_hurkot_inferno", 130, 130, "water", 40); // Yt-HurKot (Level 141)
        x("jal_nib", -20, -20, "water", 40); // Jal-Nib
        x("jal_mejrah", 45, 45, "water", 40); // Jal-MejRah
        x("jal_ak", 25, 25, "water", 40); // Jal-Ak
        x("jal_akrek_ket", 0, 0, "water", 40); // Jal-AkRek-Ket
        x("jal_akrek_xil", 25, 25, "water", 40); // Jal-AkRek-Xil
        x("jal_akrek_mej", 0, 0, "water", 40); // Jal-AkRek-Mej
        x("jal_imkot", 50, 50, "water", 40); // Jal-ImKot
        x("jal_xil", 0, 0, "water", 40); // Jal-Xil
        x("jal_zek", 0, 0, "water", 40); // Jal-Zek
        x("jaltok_jad", 0, 0, "water", 40); // JalTok-Jad
        x("jal_mejjak", 0, 0, "water", 40); // Jal-MejJak
        x("animated_bronze_armour", 400, 400, "earth", 30); // Animated Bronze Armour
        x("animated_iron_armour", 400, 400, "earth", 30); // Animated Iron Armour
        x("animated_steel_armour", 400, 400, "earth", 30); // Animated Steel Armour
        x("animated_black_armour", 400, 400, "earth", 30); // Animated Black Armour
        x("animated_mithril_armour", 400, 400, "earth", 30); // Animated Mithril Armour
        x("animated_adamant_armour", 400, 400, "earth", 30); // Animated Adamant Armour
        x("animated_rune_armour", 400, 400, "earth", 30); // Animated Rune Armour
        x("loar_shade", 0, 0, "air", 40); // Loar Shade (Shade)
        x("phrin_shade", 0, 0, "air", 40); // Phrin Shade (Shade)
        x("riyl_shade", 0, 0, "air", 40); // Riyl Shade (Shade)
        x("asyn_shade", 0, 0, "air", 40); // Asyn Shade (Shade)
        x("fiyr_shade", 0, 0, "air", 40); // Fiyr Shade (Shade)
        x("white_knight", 74, 54, null, 0); // White Knight (Initiate (f))
        x("tzhaar_hur_citizen", 0, 0, "water", 40); // TzHaar-Hur
        x("tzhaar_ket", 0, 0, "water", 40); // TzHaar-Ket (Level 149)
        x("tzhaar_xil", 0, 0, "water", 40); // TzHaar-Xil (Knife)
        x("tzhaar_mej", 0, 0, "water", 40); // TzHaar-Mej
        x("rock_crab", 0, 0, "earth", 20); // Rock Crab (Active)
        x("imp", -42, -42, "water", 10); // Imp (Normal)
        x("sand_crab", 0, 0, "earth", 20); // Sand Crab (Active)
        x("king_sand_crab", 0, 0, "earth", 15); // King Sand Crab (Active)
        x("lizardman_brute", -10, 0, null, 0); // Lizardman brute (Standard)
        x("deviant_spectre", 85, 20, "air", 30); // Deviant spectre
        x("twisted_banshee", 15, 15, "air", 25); // Twisted Banshee
        x("warped_jelly", 0, 0, "earth", 30); // Warped Jelly
        x("catacomb_greater_demon", 0, 0, "water", 40); // Greater demon (Level 92)
        x("catacomb_black_demon", 0, 0, "water", 40); // Black demon (Level 172)
        x("catacomb_hellhound", 0, 0, "water", 50); // Hellhound (Level 122)
        x("catacomb_fire_giant", 0, 0, "water", 100); // Fire giant (Level 86)
        x("catacomb_moss_giant", 0, 0, "fire", 50); // Moss giant (Level 42)
        x("catacomb_hill_giant", 0, 0, "earth", 25); // Hill Giant
        x("catacomb_dagannoth", 0, 0, "earth", 35); // Dagannoth (Level 74 (1))
        x("catacomb_ankou", 0, 0, "air", 40); // Ankou (Level 75)
        x("catacomb_dust_devil", 0, 0, "air", 35); // Dust devil (Smoke Dungeon)
        x("sulphur_nagua", 200, 200, "air", 15); // Sulphur Nagua
        x("blood_jaguar", 0, 0, "earth", 15); // Blood jaguar
        x("poison_spider", 14, 14, "fire", 40); // Poison spider (Level 64)
        x("flesh_crawler", 15, 15, "fire", 20); // Flesh Crawler (Level 41)
        x("giant_spider", 10, 10, "fire", 50); // Giant spider (Level 50)
        x("cave_goblin", 0, 0, "fire", 40); // Cave goblin (monster) (Backpack)
        x("cave_goblin_guard", 12, 12, "fire", 40); // Cave goblin guard (Level 24)
        x("abyssal_leech", 0, 0, null, 0); // Abyssal leech
        x("abyssal_guardian", 0, 0, null, 0); // Abyssal guardian
        x("abyssal_walker", 0, 0, null, 0); // Abyssal walker
        x("ammonite_crab", 0, 0, "earth", 25); // Ammonite Crab
        x("long_tailed_wyvern", 120, 70, "air", 25); // Long-tailed Wyvern
        x("ancient_wyvern", 120, 90, "air", 35); // Ancient Wyvern
        x("monkey_zombie", 0, 0, "fire", 50); // Monkey Zombie (Level 98)
        x("suqah", 50, 50, "earth", 20); // Suqah (1)
        x("wallasalki", -10, -10, "earth", 30); // Wallasalki (1)
        x("rock_lobster", 150, 90, "earth", 25); // Rock lobster (Active)
        x("jungle_horror", 0, 0, "fire", 25); // Jungle horror (Guard)
        x("jogre", 0, 0, "earth", 25); // Jogre (Normal)
        x("ogress_warrior", 16, 16, "earth", 20); // Ogress Warrior (1)
        x("ogress_shaman", 8, 8, "earth", 20); // Ogress Shaman
        x("mudskipper", 0, 0, "earth", 20); // Mudskipper (Level 31)
        x("kreearra", 200, 200, "air", 30); // Kree'arra
        x("commander_zilyana", 100, 75, null, 0); // Commander Zilyana
        x("nex", 190, 150, null, 0); // Nex
        x("callisto", 50, 50, "fire", 30); // Callisto
        x("venenatis", 150, 150, "fire", 40); // Venenatis
        x("spindel", 103, 103, "fire", 25); // Spindel
        x("scorpia", 284, 284, "fire", 35); // Scorpia
        x("chaos_elemental", 70, 70, "air", 50); // Chaos Elemental
        x("chaos_fanatic", 80, 80, null, 0); // Chaos Fanatic
        x("king_black_dragon", 70, 40, "water", 50); // King Black Dragon
        x("giant_mole", 60, 60, "earth", 50); // Giant Mole
        x("kalphite_queen", 100, 100, "fire", 40); // Kalphite Queen (Crawling)
        x("kalphite_queen_airborne", 10, 10, "fire", 40); // Kalphite Queen (Airborne)
        x("dagannoth_rex", 255, 255, "earth", 35); // Dagannoth Rex
        x("dagannoth_prime", 10, 10, "earth", 35); // Dagannoth Prime
        x("dagannoth_supreme", 550, 550, "earth", 35); // Dagannoth Supreme
        x("sarachnis", 300, 300, "fire", 40); // Sarachnis
        x("corporeal_beast", 230, 100, "earth", 10); // Corporeal Beast
        x("skotizo", 130, 130, "water", 40); // Skotizo
        x("hespori", 80, 80, "fire", 100); // Hespori
        x("zulrah", 50, 50, "fire", 50); // Zulrah (Serpentine)
        x("zulrah_magma", 300, 300, "fire", 50); // Zulrah (Magma)
        x("zulrah_tanzanite", 0, 0, "fire", 50); // Zulrah (Tanzanite)
        x("vorkath", 26, 26, "fire", 40); // Vorkath (Post-quest)
        x("kraken", 300, 300, "earth", 50); // Kraken (Kraken)
        x("thermonuclear_smoke_devil", 900, 900, "air", 20); // Thermonuclear smoke devil
        x("cerberus", 100, 100, "water", 40); // Cerberus
        x("alchemical_hydra", 45, 45, "earth", 50); // Alchemical Hydra (Serpentine)
        x("alchemical_hydra_blue", 45, 45, "earth", 50); // Alchemical Hydra (Electric)
        x("alchemical_hydra_red", 45, 45, "earth", 50); // Alchemical Hydra (Fire)
        x("alchemical_hydra_enraged", 45, 45, "earth", 50); // Alchemical Hydra (Extinguished)
        x("dusk", 0, 0, "earth", 40); // Dusk (Second form)
        x("dawn", 0, 0, "earth", 70); // Dawn
        x("araxxor", 218, 218, "fire", 50); // Araxxor
        x("phantom_muspah", 56, 56, "air", 65); // Phantom Muspah (Ranged)
        x("the_leviathan", 50, 25, null, 0); // The Leviathan (Post-quest)
        x("the_whisperer", 300, 300, "earth", 60); // The Whisperer (Post-quest)
        x("the_hueycoatl", 350, 350, "earth", 60); // The Hueycoatl (Normal)
        x("amoxliatl", 200, 200, "fire", 30); // Amoxliatl (Post-quest)
        x("branda_the_fire_queen", 700, 700, "water", 50); // Branda the Fire Queen
        x("eldric_the_ice_king", 700, 700, "fire", 50); // Eldric the Ice King
        x("elvarg", 20, 20, "water", 30); // Elvarg
        x("tree_spirit", 0, 0, "fire", 35); // Tree spirit (Level 79)
        x("chronozon", 0, 0, "water", 25); // Chronozon
        x("ice_queen", 30, 30, "fire", 100); // Ice Queen
        x("entranan_firebird", -26, -26, "water", 100); // Entrana firebird
        x("glough_black_demon", 0, 0, "water", 40); // Black demon (Level 172)
        x("jungle_demon", 0, 0, "fire", 25); // Jungle Demon
        x("nezikchened", 0, 0, "water", 10); // Nezikchened (Normal)
        x("draugen", 500, 500, "air", 50); // The Draugen
        x("dagannoth_mother", 50, 50, "earth", 35); // Dagannoth mother
        x("kamil", 0, 0, "fire", 10); // Kamil
        x("treus_dayth", 5, 5, "air", 25); // Treus Dayth (In combat)
        x("quest_skeleton_hellhound", 0, 0, "earth", 35); // Skeleton Hellhound
        x("dad", 200, 200, "earth", 40); // Dad
        x("troll_general", 200, 200, "earth", 20); // Troll general (Hammer (brown))
        x("ds2_vorkath", 80, 80, "fire", 40); // Vorkath (Dragon Slayer II)
        x("galvek", 86, 86, "water", 40); // Galvek (Air)
        x("balance_elemental", 5, -40, null, 0); // Balance Elemental (Magic)
        x("shadow_of_light", 30, 30, "air", 40); // Shadow (Temple of Light)
        x("agrith_naar", 0, 0, "water", 25); // Agrith Naar
        x("warped_terrorbird", 20, 62, null, 0); // Warped Terrorbird (Level 138)
        x("ice_troll_king", 2000, 2000, "fire", 35); // Ice Troll King
        x("giant_sea_snake", 0, 0, "earth", 20); // Giant Sea Snake
        x("glod", 100, 100, "earth", 50); // Glod
        x("giant_scarab", 149, 149, "fire", 50); // Giant Scarab
        x("champion_of_scabaras", 5, 5, "fire", 35); // Champion of Scabaras
        x("sea_troll_queen", 0, 0, "earth", 15); // Sea Troll Queen
        x("judge_of_yama", 0, 0, "water", 15); // Judge of Yama
        x("agrith_na_na", 100, 60, null, 0); // Agrith-Na-Na
        x("flambeed", 50, 50, "water", 50); // Flambeed
        x("karamel", 150, 150, "fire", 40); // Karamel
        x("quest_giant_roc", 0, 0, "air", 35); // Giant Roc
        x("quest_arrg", 200, 200, "earth", 50); // Arrg
        x("quest_slagilith", 50, 50, "earth", 40); // Slagilith
        x("quest_basilisk_boss", 0, 0, "earth", 40); // Basilisk Youngling
        x("quest_blood_moon", 500, 500, "air", 15); // Blood Moon
        x("quest_blue_moon", 500, 500, "air", 15); // Blue Moon
        x("quest_eclipse_moon", 500, 500, "air", 15); // Eclipse Moon (Regular)
        x("tzkal_zuk", 100, 100, "water", 40); // TzKal-Zuk (Normal)
        x("blue_moon", 500, 500, "air", 15); // Blue Moon
        x("eclipse_moon", 500, 500, "air", 15); // Eclipse Moon (Regular)
        x("blood_moon", 500, 500, "air", 15); // Blood Moon
        x("dagannoth_lvl88", 150, 150, "earth", 35); // Dagannoth (Waterbirth Island) (Level 88)
        x("dagannoth_lvl90", 50, 50, "earth", 35); // Dagannoth (Waterbirth Island) (Level 90)
        x("dagannoth_lvl92", 50, 50, "earth", 35); // Dagannoth (Level 92 (1))
        x("dagannoth_spawn", 0, 0, "earth", 35); // Dagannoth spawn
        x("dagannoth_fledgeling", 0, 0, "earth", 35); // Dagannoth fledgeling
        x("giant_rock_crab", 250, 80, "earth", 20); // Giant Rock Crab (2)
        x("monkey_zombie_lvl82", 0, 0, "fire", 50); // Monkey Zombie (Level 82)
        x("monkey_zombie_lvl129", 0, 0, "fire", 50); // Monkey Zombie (Level 129)
        x("skeleton_lvl142", 0, 0, "earth", 35); // Skeleton (Ape Atoll)
        x("urium_shade", 0, 0, "air", 40); // Urium Shade (Shade)
        x("demonic_gorilla", 0, 0, "water", 35); // Demonic gorilla (1)
        x("trek_ghast_1", 0, 0, "air", 25); // Ghast (Level 79)
        x("trek_ghast_2", 0, 0, "air", 25); // Ghast (Level 109)
        x("trek_ghast_3", 0, 0, "air", 25); // Ghast (Level 139)
        x("trek_shade_1", 0, 0, "air", 40); // Riyl shadow (Temple Trekking)
        x("trek_shade_2", 0, 0, "air", 40); // Asyn shadow (Temple Trekking)
        x("trek_shade_3", 0, 0, "air", 40); // Shade (Temple Trekking)
        x("trek_giant_snail_1", 0, 0, "earth", 45); // Giant snail (Level 80)
        x("trek_giant_snail_2", 0, 0, "earth", 45); // Giant snail (Level 109)
        x("trek_giant_snail_3", 0, 0, "earth", 45); // Giant snail (Level 139)
        x("trek_undead_lumberjack_1", 0, 0, "fire", 50); // Undead Lumberjack (Level 30)
        x("trek_undead_lumberjack_2", 0, 0, "fire", 50); // Undead Lumberjack (Level 50)
        x("trek_undead_lumberjack_3", 0, 0, "fire", 50); // Undead Lumberjack (Level 70)
        x("trek_zombie", 0, 0, "fire", 50); // Zombie (Level 13, 1)
        // Rows whose comment or stats in MonsterStatsTable match no monsters.json entry (matched by hand).
        x("mountain_troll", 200, 200, "fire", 50); // Mountain troll (Level 69)
        x("brutal_blue_dragon", 50, 10, "water", 50); // Brutal blue dragon (Catacombs of Kourend)
        x("nazastarool", 0, 0, "fire", 40); // Nazastarool (Zombie)
        x("vardorvis", 580, 580, "fire", 35); // Vardorvis (Post-quest)
    }
}
