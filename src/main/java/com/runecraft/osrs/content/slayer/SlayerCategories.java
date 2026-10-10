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
        add("greater_demons", "greater demons", 1, List.of("GREATER_DEMON"), Region.KARAMJA, Region.WILDERNESS);
        add("black_demons", "black demons", 1, List.of("BLACK_DEMON"), Region.ASGARNIA, Region.WILDERNESS);
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
        add("basilisk_knights", "basilisk knights", 60, Region.FREMENNIK_PROVINCE);
        // Dragons
        add("green_dragons", "green dragons", 1, Region.WILDERNESS);
        add("blue_dragons", "blue dragons", 1, Region.ASGARNIA, Region.FELDIP_HILLS);
        add("red_dragons", "red dragons", 1, Region.KARAMJA);
        add("black_dragons", "black dragons", 1, Region.ASGARNIA, Region.WILDERNESS);
        add("lava_dragons", "lava dragons", 1, Region.WILDERNESS);
        add("bronze_dragons", "bronze dragons", 1, Region.KARAMJA);
        add("iron_dragons", "iron dragons", 1, Region.KARAMJA);
        add("steel_dragons", "steel dragons", 1, Region.KARAMJA);
        add("mithril_dragons", "mithril dragons", 1, Region.KANDARIN);
        add("adamant_dragons", "adamant dragons", 1, Region.UNCHARTED_ISLES);
        add("rune_dragons", "rune dragons", 1, Region.UNCHARTED_ISLES);
        // Provided by other modules (God Wars, TzHaar, Revenants...). Only assigned once such a creature exists.
        add("aviansies", "aviansies", 1, Region.TROLL_COUNTRY);
        add("tzhaar", "TzHaar", 1, Region.KARAMJA);
        add("revenants", "revenants", 1, Region.WILDERNESS);
        add("vampyres", "vampyres", 1, Region.MORYTANIA);
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
