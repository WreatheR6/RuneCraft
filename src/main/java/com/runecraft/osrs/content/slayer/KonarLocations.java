package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.world.SpawnPoint;
import com.runecraft.osrs.world.SpawnTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Konar quo Maten's task locations (OSRS Wiki "Konar quo Maten", July 2026): after rolling a task she rolls, with equal
 * chance, one of the locations the wiki lists for it that the player may enter (a location behind a quest is never named
 * before the quest is done), and only kills there count. Here a location is a set of carved dungeons and areas of map
 * tiles; she names only locations that are built and hold some of the task's monsters, and a task none of whose locations
 * is built falls back to the kingdom rule ({@link SlayerTasks#regions}).
 */
public final class KonarLocations {
    /** Any feet height. */
    private static final int ANY = Integer.MIN_VALUE;

    /**
     * Part of a location: a carved dungeon (Dungeons id), or a box of map tiles {@code x1..x2, y1..y2} in which, when
     * {@code yLo != ANY}, the feet must be at block height {@code yLo..yHi}.
     */
    public record Area(String dungeon, int x1, int y1, int x2, int y2, int yLo, int yHi) {
        static Area dungeon(String id) {
            return new Area(id, 0, 0, 0, 0, ANY, ANY);
        }

        static Area box(int x1, int y1, int x2, int y2) {
            return new Area(null, x1, y1, x2, y2, ANY, ANY);
        }

        static Area box(int x1, int y1, int x2, int y2, int yLo, int yHi) {
            return new Area(null, x1, y1, x2, y2, yLo, yHi);
        }

        boolean tile(int x, int y, int feetY) {
            return dungeon == null && x >= x1 && x <= x2 && y >= y1 && y <= y2
                    && (yLo == ANY || feetY != SpawnPoint.SURFACE && feetY >= yLo && feetY <= yHi);
        }

        boolean contains(int blockX, int blockY, int blockZ) {
            if (dungeon != null) {
                return Dungeons.inside(dungeon, blockX, blockY, blockZ);
            }
            return tile(GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ), blockY);
        }
    }

    /**
     * A location Konar can name: its wiki name, its areas, the quest that must be complete before she names it (null for
     * none) and the bosses that count there as the task's monsters (OSRS: Cerberus for hellhounds in Taverley Dungeon...).
     */
    public record Location(String id, String name, List<Area> areas, String quest, List<String> bosses) {
        public boolean contains(int blockX, int blockY, int blockZ) {
            for (Area a : areas) {
                if (a.contains(blockX, blockY, blockZ)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The wiki's location list per task category (snapshot test: konar_locations.txt). */
    public static final Map<String, List<String>> WIKI;
    private static final Map<String, Location> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, Location> BY_ID = new HashMap<>();
    private static final Map<String, List<Location>> CANDIDATES = new HashMap<>();

    static {
        Map<String, List<String>> w = new LinkedHashMap<>();
        w.put("aberrant_spectres", List.of("Catacombs of Kourend", "Slayer Tower", "Stronghold Slayer Cave"));
        w.put("abyssal_demons", List.of("Catacombs of Kourend", "Abyss", "Slayer Tower"));
        w.put("ankou", List.of("Stronghold of Security", "Stronghold Slayer Dungeon", "Catacombs of Kourend"));
        w.put("aviansies", List.of("God Wars Dungeon"));
        w.put("basilisks", List.of("Fremennik Slayer Dungeon", "Jormungand's Prison"));
        w.put("black_demons", List.of("Catacombs of Kourend", "Chasm of Fire", "Taverley Dungeon", "Brimhaven Dungeon"));
        w.put("black_dragons", List.of("Catacombs of Kourend", "Myths' Guild Dungeon", "Evil Chicken's Lair", "Taverley Dungeon"));
        w.put("bloodveld", List.of("Catacombs of Kourend", "God Wars Dungeon", "Iorwerth Dungeon", "Meiyerditch Laboratories",
                "Slayer Tower", "Stronghold Slayer Dungeon"));
        w.put("blue_dragons", List.of("Catacombs of Kourend", "Isle of Souls Dungeon", "Myths' Guild Dungeon", "Ogre Enclave",
                "Ruins of Tapoyauik", "Taverley Dungeon"));
        w.put("brine_rats", List.of("Brine Rat Cavern"));
        w.put("cave_kraken", List.of("Kraken Cove"));
        w.put("dagannoth", List.of("Catacombs of Kourend", "Lighthouse", "Waterbirth Island", "Jormungand's Prison"));
        w.put("dark_beasts", List.of("Mourner Tunnels", "Iorwerth Dungeon"));
        w.put("drakes", List.of("Karuulm Slayer Dungeon"));
        w.put("dust_devils", List.of("Catacombs of Kourend", "Smoke Dungeon"));
        w.put("fire_giants", List.of("Brimhaven Dungeon", "Catacombs of Kourend", "Isle of Souls Dungeon", "Giants' Den",
                "Karuulm Slayer Dungeon", "Stronghold Slayer Dungeon", "Waterfall Dungeon"));
        w.put("fossil_island_wyverns", List.of("Wyvern Cave"));
        w.put("gargoyles", List.of("Slayer Tower"));
        w.put("greater_demons", List.of("Catacombs of Kourend", "Chasm of Fire", "Isle of Souls Dungeon", "Karuulm Slayer Dungeon",
                "Brimhaven Dungeon"));
        w.put("hellhounds", List.of("Karuulm Slayer Dungeon", "Catacombs of Kourend", "Stronghold Slayer Dungeon", "Taverley Dungeon",
                "Witchaven Dungeon"));
        w.put("hydras", List.of("Karuulm Slayer Dungeon"));
        w.put("jellies", List.of("Fremennik Slayer Dungeon", "Catacombs of Kourend", "Ruins of Tapoyauik"));
        w.put("kalphite", List.of("Kalphite Lair", "Kalphite Cave"));
        w.put("kurask", List.of("Fremennik Slayer Dungeon", "Iorwerth Dungeon"));
        w.put("lesser_nagua", List.of("Neypotzli", "Ruins of Tapoyauik", "Crypt of Tonali"));
        w.put("lizardmen", List.of("Battlefront", "Lizardman Canyon", "Lizardman Settlement", "Kebos Swamp", "Molch"));
        w.put("metal_dragons", List.of("Brimhaven Dungeon", "Catacombs of Kourend", "Isle of Souls Dungeon", "Lithkren Vault"));
        w.put("mutated_zygomites", List.of("Fossil Island", "Zanaris"));
        w.put("nechryael", List.of("Catacombs of Kourend", "Iorwerth Dungeon", "Slayer Tower"));
        w.put("red_dragons", List.of("Brimhaven Dungeon", "Catacombs of Kourend", "Forthos Dungeon", "Myths' Guild Dungeon"));
        w.put("skeletal_wyverns", List.of("Asgarnian Ice Dungeon"));
        w.put("smoke_devils", List.of("Smoke Devil Dungeon"));
        w.put("trolls", List.of("Troll Stronghold", "Keldagrim", "Death Plateau", "Mount Quidamortem", "Fremennik Isles"));
        w.put("turoth", List.of("Fremennik Slayer Dungeon"));
        w.put("vampyres", List.of("Darkmeyer", "Meiyerditch", "Slepe", "Vampyrium"));
        w.put("warped_creatures", List.of("Poison Waste Dungeon"));
        w.put("waterfiends", List.of("Ancient Cavern", "Iorwerth Dungeon", "Kraken Cove"));
        w.put("wyrms", List.of("Karuulm Slayer Dungeon", "Neypotzli", "Charred Dungeon"));
        WIKI = Collections.unmodifiableMap(w);

        // Built locations. Dungeons by id; the rest by their map tiles (approx boxes around the wiki maps' areas).
        dungeon("Abyss", null, "dng_the_abyss");
        dungeon("Ancient Cavern", null, "dng_ancient_cavern");
        dungeon("Asgarnian Ice Dungeon", null, "dng_asgarnian_ice_dungeon");
        dungeon("Brimhaven Dungeon", null, "dng_brimhaven_dungeon");
        dungeon("Brine Rat Cavern", "olafs_quest", "dng_brine_rat_cavern");
        dungeon("Chasm of Fire", null, "dng_chasm_of_fire");
        dungeon("Charred Dungeon", null, "dng_charred_dungeon");
        // approx: the crypt's nagua live in the cavern carved below it.
        dungeon("Crypt of Tonali", null, "dng_tonali_cavern");
        dungeon("Forthos Dungeon", null, "dng_forthos_dungeon");
        dungeon("Fremennik Slayer Dungeon", null, "dng_fremennik_slayer_dungeon");
        dungeon("Giants' Den", null, "dng_giants_den");
        dungeon("Iorwerth Dungeon", "song_of_the_elves", "dng_iorwerth_dungeon");
        dungeon("Isle of Souls Dungeon", null, "dng_isle_of_souls_dungeon");
        dungeon("Jormungand's Prison", "the_fremennik_exiles", "dng_jormungands_prison");
        dungeon("Kalphite Cave", null, "dng_kalphite_cave");
        dungeon("Kalphite Lair", null, "dng_kalphite_lair").bosses.add("kalphite_queen");
        dungeon("Karuulm Slayer Dungeon", null, "dng_karuulm_slayer_dungeon").bosses.add("alchemical_hydra");
        dungeon("Kraken Cove", null, "dng_kraken_cove").bosses.add("kraken");
        dungeon("Lighthouse", "horror_from_the_deep", "dng_lighthouse_dungeon");
        dungeon("Lithkren Vault", "dragon_slayer_ii", "dng_lithkren_vault");
        dungeon("Myths' Guild Dungeon", "dragon_slayer_ii", "dng_myths_guild_dungeon");
        dungeon("Neypotzli", null, "dng_neypotzli");
        dungeon("Poison Waste Dungeon", null, "dng_poison_waste_dungeon");
        dungeon("Ruins of Tapoyauik", null, "dng_ruins_of_tapoyauik");
        dungeon("Smoke Devil Dungeon", null, "dng_smoke_devil_dungeon").bosses.add("thermonuclear_smoke_devil");
        dungeon("Smoke Dungeon", null, "dng_smoke_dungeon");
        dungeon("Stronghold of Security", null, "dng_stronghold_of_security");
        dungeon("Stronghold Slayer Cave", null, "dng_stronghold_slayer_cave");
        alias("Stronghold Slayer Dungeon", "Stronghold Slayer Cave");
        dungeon("Taverley Dungeon", null, "dng_taverley_dungeon").bosses.add("cerberus");
        dungeon("Waterbirth Island", null, "dng_waterbirth_dungeon").bosses.addAll(List.of("dagannoth_rex",
                "dagannoth_prime", "dagannoth_supreme"));
        dungeon("Waterfall Dungeon", null, "dng_waterfall_dungeon");
        dungeon("Wyvern Cave", "bone_voyage", "dng_wyvern_cave", "dng_wyvern_cave_south");
        // The Catacombs of Kourend are carved beneath Kourend Castle (zeah module, floor 38).
        place("Catacombs of Kourend", null, Area.box(1604, 3646, 1674, 3716, 30, 50));
        // The Slayer Tower's three floors and its basement.
        place("Slayer Tower", "priest_in_peril", Area.box(3400, 3528, 3455, 3580)).bosses.addAll(List.of("dusk", "dawn"));
        // The God Wars Dungeon at its cache tiles (godwars module).
        place("God Wars Dungeon", null, Area.box(2816, 5248, 2944, 5376)).bosses.add("kreearra");
        place("Fossil Island", "bone_voyage", Area.box(3640, 3690, 3860, 3905));
        place("Zanaris", "lost_city", Area.box(2352, 4376, 2472, 4488));
        place("Troll Stronghold", null, Area.dungeon("dng_troll_stronghold"), Area.box(2820, 3660, 2865, 3700));
        place("Death Plateau", null, Area.box(2835, 3580, 2925, 3625));
        place("Mount Quidamortem", null, Area.box(1190, 3470, 1275, 3580));
        place("Fremennik Isles", null, Area.dungeon("dng_ice_troll_caves"), Area.box(2300, 3780, 2420, 3910));
        place("Lizardman Canyon", null, Area.box(1410, 3675, 1535, 3725));
        place("Lizardman Settlement", null, Area.dungeon("dng_lizardman_caves"), Area.box(1280, 3575, 1340, 3625));
        place("Darkmeyer", "sins_of_the_father", Area.box(3590, 3325, 3670, 3400));
        place("Meiyerditch", null, Area.box(3580, 3165, 3660, 3324));
    }

    private KonarLocations() {
    }

    private static final class Builder {
        final List<String> bosses = new ArrayList<>();
    }

    private static Builder dungeon(String name, String quest, String... ids) {
        List<Area> areas = new ArrayList<>();
        for (String id : ids) {
            areas.add(Area.dungeon(id));
        }
        return add(name, quest, areas);
    }

    private static Builder place(String name, String quest, Area... areas) {
        return add(name, quest, List.of(areas));
    }

    private static Builder add(String name, String quest, List<Area> areas) {
        Builder b = new Builder();
        Location l = new Location(id(name), name, List.copyOf(areas), quest, Collections.unmodifiableList(b.bosses));
        BY_NAME.put(name, l);
        BY_ID.put(l.id(), l);
        return b;
    }

    private static void alias(String name, String of) {
        BY_NAME.put(name, BY_NAME.get(of));
    }

    /** A location's id: its name in lower case, words joined by '_'. */
    public static String id(String name) {
        return name.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    public static Location get(String id) {
        return BY_ID.get(id);
    }

    /** The built location the wiki name stands for, or null when it isn't built. */
    public static Location byName(String name) {
        return BY_NAME.get(name);
    }

    /** Every built location. */
    public static List<Location> all() {
        return List.copyOf(new HashSet<>(BY_NAME.values()));
    }

    /**
     * The locations Konar may name for a task: the wiki's, built and holding some of the task's monsters, whose quest the
     * player has done ({@code questDone} gets the quest id). Empty when none is (the kingdom rule applies then).
     */
    public static List<Location> candidates(String category, Predicate<String> questDone) {
        List<Location> out = new ArrayList<>();
        for (Location l : candidates(category)) {
            if (l.quest() == null || questDone.test(l.quest())) {
                out.add(l);
            }
        }
        return out;
    }

    /** The built locations of a task that hold its monsters, whatever the player's quests. */
    public static synchronized List<Location> candidates(String category) {
        return CANDIDATES.computeIfAbsent(category, KonarLocations::compute);
    }

    private static List<Location> compute(String category) {
        List<String> names = WIKI.get(category);
        if (names == null) {
            return List.of();
        }
        Set<String> ids = creatures(category);
        List<Location> out = new ArrayList<>();
        for (String name : names) {
            Location l = BY_NAME.get(name);
            if (l != null && !out.contains(l) && holds(l, ids)) {
                out.add(l);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Monster ids that count for a task category (its creatures and built-in monsters). */
    static Set<String> creatures(String category) {
        Set<String> ids = new HashSet<>();
        for (MonsterDef m : SlayerTasks.creatures(category)) {
            ids.add(m.id);
        }
        SlayerCategories.Category c = SlayerCategories.get(category);
        if (c != null) {
            for (String b : c.builtIns()) {
                ids.add(Monster.valueOf(b).entity());
            }
        }
        return ids;
    }

    /** Whether some of the monsters spawn in the location (a camp of one of its dungeons, or a spawn in one of its boxes). */
    static boolean holds(Location l, Set<String> ids) {
        for (Area a : l.areas()) {
            for (SpawnTable.CampRef c : SpawnTable.camps()) {
                if (ids.contains(c.creature()) && (a.dungeon() != null ? a.dungeon().equals(c.source()) : a.tile(c.x(), c.y(), c.floorY()))) {
                    return true;
                }
            }
            if (a.dungeon() == null) {
                for (SpawnPoint p : SpawnTable.all()) {
                    if (ids.contains(p.monster()) && a.tile(p.x(), p.y(), p.floorY())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether a kill at a block position (or of {@code bossId}, null for none) counts for the location {@code id}: inside
     * it, or one of the bosses that count there.
     */
    public static boolean counts(String id, int blockX, int blockY, int blockZ, String bossId) {
        Location l = BY_ID.get(id);
        if (l == null) {
            return true; // a location no longer built: don't strand the task
        }
        return l.contains(blockX, blockY, blockZ) || bossId != null && l.bosses().contains(bossId);
    }
}
