package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Slayer task categories ("kill 120 abyssal demons"). A kill counts towards a category when the
 * creature's {@code slayerCategory} equals the category id (any module's monster or boss) or when
 * it is one of the listed built-in {@link com.runecraft.osrs.data.Monster} enum constants.
 */
public final class SlayerCategories {
    /**
     * @param id         category id, used as {@code MonsterDef.slayer(level, id)}
     * @param name       plural display name ("abyssal demons")
     * @param level      Slayer level needed to be assigned the task
     * @param builtIns   names of built-in Monster enum constants that count
     * @param regions    fallback regions for Konar's location restriction (module monsters add their own spawn regions)
     */
    public record Category(String id, String name, int level, List<String> builtIns, List<Region> regions) {
    }

    private static final Map<String, Category> ALL = new LinkedHashMap<>();
    private static final Map<String, String> MEMBERS = new LinkedHashMap<>();

    private SlayerCategories() {
    }

    private static void add(String id, String name, int level, List<String> builtIns, Region... regions) {
        if (ALL.put(id, new Category(id, name, level, builtIns, List.of(regions))) != null) {
            throw new IllegalStateException("Duplicate slayer category " + id);
        }
    }

    private static void add(String id, String name, int level, Region... regions) {
        add(id, name, level, List.of(), regions);
    }

    static {
        // Built-in (core) monsters
        add("birds", "birds", 1, List.of("CHICKEN"), Region.MISTHALIN);
        add("cows", "cows", 1, List.of("COW"), Region.MISTHALIN);
        add("goblins", "goblins", 1, List.of("GOBLIN"), Region.MISTHALIN, Region.ASGARNIA);
        add("rats", "rats", 1, List.of("GIANT_RAT"), Region.MISTHALIN);
        add("spiders", "spiders", 1, List.of("SPIDER", "GIANT_SPIDER"), Region.MISTHALIN, Region.WILDERNESS);
        add("wolves", "wolves", 1, List.of("WOLF"), Region.ASGARNIA, Region.WILDERNESS);
        add("zombies", "zombies", 1, List.of("ZOMBIE"), Region.MISTHALIN, Region.MORYTANIA);
        add("skeletons", "skeletons", 1, List.of("SKELETON"), Region.MISTHALIN, Region.WILDERNESS);
        add("ghouls", "ghouls", 1, List.of("GHOUL"), Region.MORYTANIA);
        add("hill_giants", "hill giants", 1, List.of("HILL_GIANT"), Region.MISTHALIN, Region.ASGARNIA);
        add("moss_giants", "moss giants", 1, List.of("MOSS_GIANT"), Region.MISTHALIN, Region.KARAMJA);
        add("ice_giants", "ice giants", 1, List.of("ICE_GIANT"), Region.ASGARNIA, Region.WILDERNESS);
        add("lesser_demons", "lesser demons", 1, List.of("LESSER_DEMON"), Region.KARAMJA, Region.WILDERNESS);
        add("greater_demons", "greater demons", 1, List.of("GREATER_DEMON"), Region.KARAMJA, Region.KANDARIN, Region.WILDERNESS);
        add("black_demons", "black demons", 1, List.of("BLACK_DEMON"), Region.ASGARNIA, Region.KARAMJA, Region.KANDARIN, Region.WILDERNESS);
        // Slayer module creatures (low level)
        add("bats", "bats", 1, Region.WILDERNESS, Region.MORYTANIA);
        add("bears", "bears", 1, Region.ASGARNIA, Region.WILDERNESS);
        add("scorpions", "scorpions", 1, Region.KHARIDIAN_DESERT, Region.WILDERNESS);
        add("minotaurs", "minotaurs", 1, Region.MISTHALIN);
        add("ghosts", "ghosts", 1, Region.MISTHALIN, Region.MORYTANIA);
        add("icefiends", "icefiends", 1, Region.ASGARNIA);
        add("hobgoblins", "hobgoblins", 1, Region.ASGARNIA, Region.WILDERNESS);
        add("monkeys", "monkeys", 1, Region.KARAMJA);
        add("crawling_hands", "crawling hands", 5, Region.MORYTANIA);
        add("cave_bugs", "cave bugs", 7, Region.LUMBRIDGE_SWAMP);
        add("cave_crawlers", "cave crawlers", 10, Region.FREMENNIK_PROVINCE, Region.LUMBRIDGE_SWAMP);
        add("banshees", "banshees", 15, Region.MORYTANIA);
        add("cave_slime", "cave slimes", 17, Region.LUMBRIDGE_SWAMP);
        add("rockslugs", "rockslugs", 20, Region.FREMENNIK_PROVINCE, Region.LUMBRIDGE_SWAMP);
        add("lizards", "lizards", 22, Region.KHARIDIAN_DESERT);
        add("cockatrice", "cockatrice", 25, Region.FREMENNIK_PROVINCE);
        add("pyrefiends", "pyrefiends", 30, Region.FREMENNIK_PROVINCE, Region.KHARIDIAN_DESERT);
        add("mogres", "mogres", 32, Region.ASGARNIA);
        add("harpie_bug_swarms", "harpie bug swarms", 33, Region.KARAMJA);
        add("wall_beasts", "wall beasts", 35, Region.LUMBRIDGE_SWAMP);
        add("killerwatts", "killerwatts", 37, Region.MISTHALIN);
        add("molanisks", "molanisks", 39, Region.MISTHALIN);
        add("basilisks", "basilisks", 40, Region.FREMENNIK_PROVINCE);
        add("sea_snakes", "sea snakes", 40, Region.FREMENNIK_PROVINCE);
        add("terror_dogs", "terror dogs", 40, Region.MORYTANIA);
        add("fever_spiders", "fever spiders", 42, Region.UNCHARTED_ISLES);
        add("infernal_mages", "infernal mages", 45, Region.MORYTANIA);
        add("brine_rats", "brine rats", 47, Region.FREMENNIK_PROVINCE);
        add("bloodveld", "bloodveld", 50, Region.MORYTANIA);
        add("jellies", "jellies", 52, Region.FREMENNIK_PROVINCE);
        add("turoth", "turoth", 55, Region.FREMENNIK_PROVINCE);
        add("mutated_zygomites", "mutated zygomites", 57, Region.LUMBRIDGE_SWAMP);
        add("cave_horrors", "cave horrors", 58, Region.MORYTANIA);
        add("aberrant_spectres", "aberrant spectres", 60, Region.MORYTANIA, Region.KANDARIN);
        add("wyrms", "wyrms", 62, Region.UNCHARTED_ISLES);
        add("spiritual_creatures", "spiritual creatures", 63, Region.TROLL_COUNTRY);
        add("dust_devils", "dust devils", 65, Region.KHARIDIAN_DESERT);
        add("fossil_island_wyverns", "fossil island wyverns", 66, Region.UNCHARTED_ISLES);
        add("kurask", "kurask", 70, Region.FREMENNIK_PROVINCE, Region.TIRANNWN);
        add("skeletal_wyverns", "skeletal wyverns", 72, Region.ASGARNIA);
        add("gargoyles", "gargoyles", 75, Region.MORYTANIA);
        add("nechryael", "nechryael", 80, Region.MORYTANIA);
        add("drakes", "drakes", 84, Region.UNCHARTED_ISLES);
        add("abyssal_demons", "abyssal demons", 85, Region.MORYTANIA, Region.WILDERNESS);
        add("cave_kraken", "cave kraken", 87, Region.KANDARIN);
        add("dark_beasts", "dark beasts", 90, Region.KANDARIN);
        add("smoke_devils", "smoke devils", 93, Region.KANDARIN);
        add("hydras", "hydras", 95, Region.UNCHARTED_ISLES);
        // Unrestricted creatures (Slayer level 1) from this module
        add("ankou", "ankou", 1, Region.WILDERNESS, Region.MISTHALIN);
        add("hellhounds", "hellhounds", 1, Region.ASGARNIA, Region.WILDERNESS);
        add("fire_giants", "fire giants", 1, Region.KARAMJA, Region.KANDARIN);
        add("kalphite", "kalphite", 1, Region.KHARIDIAN_DESERT);
        add("trolls", "trolls", 1, Region.TROLL_COUNTRY, Region.FREMENNIK_PROVINCE);
        add("ogres", "ogres", 1, Region.FELDIP_HILLS);
        add("cyclopes", "cyclopes", 1, Region.ASGARNIA);
        add("dagannoth", "dagannoth", 1, Region.FREMENNIK_PROVINCE);
        add("elves", "elves", 1, Region.TIRANNWN);
        add("lizardmen", "lizardmen", 1, Region.UNCHARTED_ISLES);
        add("waterfiends", "waterfiends", 1, Region.KANDARIN);
        add("earth_warriors", "earth warriors", 1, Region.WILDERNESS);
        add("ice_warriors", "ice warriors", 1, Region.ASGARNIA, Region.WILDERNESS);
        // Dragons
        add("green_dragons", "green dragons", 1, Region.WILDERNESS);
        add("blue_dragons", "blue dragons", 1, Region.ASGARNIA, Region.FELDIP_HILLS);
        add("red_dragons", "red dragons", 1, Region.KARAMJA);
        add("black_dragons", "black dragons", 1, Region.ASGARNIA, Region.WILDERNESS);
        add("lava_dragons", "lava dragons", 1, Region.WILDERNESS);
        // Bronze, iron, steel, mithril, adamant and rune dragons are one task since August 2025.
        add("metal_dragons", "metal dragons", 1, Region.KARAMJA, Region.KANDARIN, Region.UNCHARTED_ISLES);
        add("frost_dragons", "frost dragons", 1, Region.UNCHARTED_ISLES);
        // Provided by other modules (God Wars, TzHaar, Revenants...). Only assigned once such a creature exists.
        add("aviansies", "aviansies", 1, Region.TROLL_COUNTRY);
        add("tzhaar", "TzHaar", 1, Region.KARAMJA);
        add("revenants", "revenants", 1, Region.WILDERNESS);
        add("vampyres", "vampyres", 1, Region.MORYTANIA);
        add("dogs", "dogs", 1, Region.KARAMJA);
        add("dwarves", "dwarves", 1, Region.ASGARNIA);
        add("catablepon", "catablepon", 1, Region.MISTHALIN);
        add("flesh_crawlers", "flesh crawlers", 1, Region.MISTHALIN);
        add("shades", "shades", 1, Region.MORYTANIA);
        add("jungle_horrors", "jungle horrors", 1, Region.MORYTANIA);
        // Creatures added by this module's extra set (SlayerExtraMonsters).
        add("sourhogs", "sourhogs", 1, Region.MISTHALIN);
        add("crocodiles", "crocodiles", 1, Region.KHARIDIAN_DESERT);
        add("shadow_warriors", "shadow warriors", 1, Region.KANDARIN);
        add("otherworldly_beings", "otherworldly beings", 1, Region.LUMBRIDGE_SWAMP);
        add("ents", "ents", 1, Region.WILDERNESS);
        add("mammoths", "mammoths", 1, Region.WILDERNESS);
        add("bandits", "bandits", 1, Region.WILDERNESS, Region.KHARIDIAN_DESERT);
        add("chaos_druids", "chaos druids", 1, Region.WILDERNESS, Region.KANDARIN);
        // Newer OSRS tasks (wiki master tables). Assigned once one of their creatures exists in RuneCraft.
        add("crabs", "crabs", 1, Region.FREMENNIK_PROVINCE, Region.UNCHARTED_ISLES);
        add("werewolves", "werewolves", 1, Region.MORYTANIA);
        add("gryphons", "gryphons", 51, Region.UNCHARTED_ISLES);
        add("custodian_stalkers", "custodian stalkers", 54, Region.UNCHARTED_ISLES);
        add("lesser_nagua", "lesser nagua", 48, Region.UNCHARTED_ISLES);
        add("warped_creatures", "warped creatures", 56, Region.KANDARIN);
        add("araxytes", "araxytes", 92, Region.MORYTANIA);
        add("aquanites", "aquanites", 78, Region.UNCHARTED_ISLES);
        add("scabarites", "scabarites", 1, Region.KHARIDIAN_DESERT);
        add("suqahs", "suqahs", 1, Region.UNCHARTED_ISLES);
        // Krystilia's Wilderness-only tasks.
        add("black_knights", "black knights", 1, Region.WILDERNESS);
        add("dark_warriors", "dark warriors", 1, Region.WILDERNESS);
        add("magic_axes", "magic axes", 1, Region.WILDERNESS);
        add("pirates", "pirates", 1, Region.WILDERNESS);
        add("rogues", "rogues", 1, Region.WILDERNESS);
        // Boss tasks ("Like a Boss"): the masters' "bosses" line turns into one boss_<id> task.
        add(SlayerBossTasks.CATEGORY, "bosses", 1, Region.WILDERNESS, Region.TROLL_COUNTRY);
        for (SlayerBossTasks.BossTask t : SlayerBossTasks.all()) {
            add(t.id(), t.name(), t.slayerLevel(), t.region());
        }

        // Other modules' creatures that count towards a task without declaring a slayer category themselves.
        member("dogs", "wild_dog");
        member("dwarves", "dwarf");
        member("catablepon", "catablepon");
        member("flesh_crawlers", "flesh_crawler");
        member("shades", "loar_shade", "phrin_shade", "riyl_shade", "asyn_shade", "fiyr_shade", "urium_shade");
        member("jungle_horrors", "jungle_horror");
        member("bandits", "desert_bandit");
        member("chaos_druids", "chaos_druid");
        member("goblins", "cave_goblin", "cave_goblin_guard", "gwd_goblin");
        member("hobgoblins", "gwd_hobgoblin");
        member("rats", "giant_rat");
        member("zombies", "monkey_zombie");
        member("aviansies", "gwd_aviansie", "gwd_aviansie_elite");
        member("tzhaar", "tzhaar_ket", "tzhaar_xil", "tzhaar_mej", "tzhaar_hur_citizen");
        member("crabs", "rock_crab", "sand_crab", "king_sand_crab", "ammonite_crab");
        member("lesser_nagua", "sulphur_nagua", "frost_nagua");
        member("suqahs", "suqah");
        member("werewolves", "gwd_werewolf");
        member("revenants", "revenant_imp", "revenant_goblin", "revenant_pyrefiend", "revenant_hobgoblin",
                "revenant_cyclops", "revenant_hellhound", "revenant_demon", "revenant_ork", "revenant_dark_beast",
                "revenant_knight", "revenant_dragon");
    }

    private static void member(String category, String... monsterIds) {
        if (!ALL.containsKey(category)) {
            throw new IllegalStateException("Unknown slayer category " + category);
        }
        for (String id : monsterIds) {
            MEMBERS.put(id, category);
        }
    }

    /**
     * The category a module creature counts towards: its own {@code slayerCategory}, else the
     * category it is listed under here (e.g. the bestiary's wild dogs count as "dogs"), else null.
     */
    public static String categoryOf(String monsterId, String declared) {
        return declared != null ? declared : MEMBERS.get(monsterId);
    }

    /** Monster id to category for creatures listed by {@link #member}. */
    public static Map<String, String> members() {
        return Collections.unmodifiableMap(MEMBERS);
    }

    public static Category get(String id) {
        return ALL.get(id);
    }

    public static Map<String, Category> all() {
        return Collections.unmodifiableMap(ALL);
    }

    /** The category a built-in Monster enum constant counts towards, or null. */
    public static Category ofBuiltIn(String monsterEnumName) {
        for (Category c : ALL.values()) {
            if (c.builtIns().contains(monsterEnumName)) {
                return c;
            }
        }
        return null;
    }

    /** All category ids. */
    public static List<String> ids() {
        return new ArrayList<>(ALL.keySet());
    }
}
