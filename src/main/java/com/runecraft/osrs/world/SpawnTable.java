package com.runecraft.osrs.world;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.bosses.BossesContent;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.godwars.GwdPlaces;
import com.runecraft.osrs.content.islands.IslandsPlaces;
import com.runecraft.osrs.content.minigames.MinigamesContent;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.slayer.Superiors;
import com.runecraft.osrs.content.zeah.ZeahPlaces;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.GielinorMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where every monster lives. Gielinor has no natural (biome) mob spawning at all: monsters only
 * appear at their OSRS spawn locations, either from a module's camp (dungeon camps, God Wars camps,
 * Zeah and island camps, the npcs module's town residents) or from a point in this table, which
 * covers the surface spawns (and a few dungeon and building floors) that no camp handles.
 *
 * <p>Coordinates are OSRS tiles (Lumbridge = 3222, 3218); see {@link GielinorMap}. Respawn times are
 * in game ticks; when not given they follow the usual OSRS times (25 ticks for weak monsters, 35 for
 * mid levels and 50 for high levels). Locations follow the OSRS world map; where a monster's real
 * home is a dungeon or realm that isn't built here, the nearest built equivalent is used and noted.
 * Positions and counts were checked against the cache-derived OSRS NPC spawn list (mejrs/data_osrs
 * NPCList_OSRS.json): counts follow the OSRS spawn count, capped at 12 per point. Underground
 * monsters only spawn in carved dungeons unless listed in {@link #surfaceStandIns()}.
 */
public final class SpawnTable {
    /** Combat level and aggression of a creature id. */
    public record Info(String id, String name, int level, boolean aggressive) {
    }

    /**
     * A spawn handled by an existing camp: {@code floorY} is {@link SpawnPoint#SURFACE} for surface
     * camps; {@code radius} is in tiles.
     */
    public record CampRef(String source, String creature, int x, int y, int floorY, int radius) {
    }

    private static List<SpawnPoint> points;
    private static List<SpawnPoint> building;
    private static Map<String, String> encounterOnly;

    private SpawnTable() {
    }

    /** Every spawn point. */
    public static synchronized List<SpawnPoint> all() {
        if (points == null) {
            building = new ArrayList<>();
            build();
            points = Collections.unmodifiableList(building);
            building = null;
        }
        return points;
    }

    // ------------------------------------------------------------------ builder helpers

    private static void s(String id, int x, int y, int count, int radius) {
        add(id, x, y, SpawnPoint.SURFACE, count, radius, 0, false);
    }

    private static void s(String id, int x, int y, int count, int radius, int respawn) {
        add(id, x, y, SpawnPoint.SURFACE, count, radius, respawn, false);
    }

    private static void si(String id, int x, int y, int count, int radius) {
        add(id, x, y, SpawnPoint.SURFACE, count, radius, 0, true);
    }

    /** On a fixed floor: a dungeon floor or a storey of a building. */
    private static void f(String id, int x, int y, int floorY, int count, int radius) {
        add(id, x, y, floorY, count, radius, 0, false);
    }

    private static void fi(String id, int x, int y, int floorY, int count, int radius) {
        add(id, x, y, floorY, count, radius, 0, true);
    }

    private static void add(String id, int x, int y, int floorY, int count, int radius, int respawn, boolean island) {
        building.add(new SpawnPoint(id, x, y, floorY, count, radius, respawn > 0 ? respawn : defaultRespawn(id), island));
    }

    /** Usual OSRS respawn delay for a monster of this level (game ticks). */
    static int defaultRespawn(String id) {
        Info info = info(id);
        int level = info == null ? 1 : info.level();
        if (level <= 30) {
            return 25;
        }
        return level <= 100 ? 35 : 50;
    }

    /** Implings wander in from Puro-Puro rarely: about every 5 minutes (rarer ones much less often). */
    private static final int IMPLING = 500;
    /** Floors of the Slayer Tower (EasternMorytania): ground, first and second storey. */
    private static final int SLAYER_TOWER_0 = StructureDef.GROUND;
    private static final int SLAYER_TOWER_1 = StructureDef.GROUND + 7;
    private static final int SLAYER_TOWER_2 = StructureDef.GROUND + 14;
    /** Top floor of the Wizards' Tower (MisthalinDraynor): four storeys of 6 blocks. */
    private static final int WIZARDS_TOWER_TOP = StructureDef.GROUND + 18;
    /** Floor heights of built dungeons (see the dungeons and zeah modules' camps). */
    private static final int BRIMHAVEN_DUNGEON = 25;
    private static final int LUMBRIDGE_SWAMP_CAVES = 23;
    private static final int KOUREND_CATACOMBS = 39;
    private static final int DWARVEN_MINE = 31;

    private static void build() {
        // ============================================================ Misthalin
        // Lumbridge (counts follow the OSRS cache spawns, capped at 12 per point)
        s("goblin", 3252, 3239, 12, 12);               // east of the River Lum (about 50 in OSRS)
        s("goblin", 3165, 3290, 10, 16);               // the fields north-west of Lumbridge (35 in OSRS)
        s("goblin", 3192, 3248, 6, 8);                 // west of Lumbridge Castle
        s("goblin", 3145, 3262, 4, 6);                 // north-west of Lumbridge, towards Draynor
        s("goblin", 3144, 3231, 4, 4);                 // by the Lumbridge Swamp road west of the castle
        s("minecraft:chicken", 3233, 3297, 10, 6);     // chicken farm east of the river (12 in OSRS)
        s("minecraft:chicken", 3185, 3277, 5, 4);      // Fred's farm
        s("minecraft:cow", 3256, 3274, 12, 12);        // cow field east of the river (13 + calves in OSRS)
        s("minecraft:cow", 3176, 3318, 5, 6);          // cow field north of Fred's farm
        s("minecraft:sheep", 3200, 3266, 10, 7);       // Fred's sheep field (16 in OSRS)
        s("minecraft:cave_spider", 3167, 3246, 4, 3);  // spiders (level 1) west of Lumbridge
        // Lumbridge Swamp
        s("giant_rat", 3215, 3172, 3, 8);
        s("minecraft:silverfish", 3165, 3190, 3, 8);   // the smaller giant rats (level 3)
        // Draynor
        s("dark_wizard", 3084, 3237, 2, 4);            // south of Draynor Village (2 in OSRS)
        s("minecraft:pig", 3076, 3260, 4, 3);          // the pig pen in Draynor Village
        // Imps (fixed OSRS spawns are few and scattered)
        s("imp", 3214, 3281, 1, 6);                    // north of Lumbridge
        s("imp", 3240, 3307, 1, 6);                    // by the Lumbridge-Varrock road
        s("imp", 3205, 3355, 1, 6);                    // by the Champions' Guild
        s("imp", 3238, 3390, 1, 6);                    // south-east Varrock
        s("imp", 3076, 3498, 2, 4);                    // Edgeville
        // Varrock
        s("minecraft:witch", 3227, 3369, 4, 6);        // the level 20 dark wizards at the stone circle (11 wizards in OSRS)
        s("scorpion", 3299, 3299, 6, 10);              // Al Kharid mine (9 in OSRS)
        // Edgeville / Barbarian Village: town residents and the dungeon are camps.

        // ============================================================ Asgarnia
        s("goblin_lvl5", 2957, 3498, 10, 12);          // Goblin Village (level 5 goblins in OSRS; 23 spawns)
        s("minecraft:chicken", 3014, 3290, 5, 5);      // farm south of Falador
        s("minecraft:pig", 3016, 3309, 3, 3);          // pig pen on the same farm
        s("minecraft:cow", 3029, 3308, 6, 6);          // cow field south of Falador
        s("minecraft:cow", 2926, 3280, 3, 5);          // pen by the Crafting Guild
        s("hobgoblin", 2911, 3282, 8, 10);             // hobgoblin peninsula west of the Crafting Guild (10 in OSRS)
        s("imp", 3012, 3312, 3, 5);                    // farmland south of Falador
        s("imp", 2947, 3329, 1, 6);                    // west of Falador
        s("imp", 2951, 3360, 1, 6);                    // by Falador's west wall
        s("dark_wizard_lvl11", 2908, 3335, 2, 5);      // Dark Wizards' Tower: two level 11 wizards
        s("dark_wizard_lvl23", 2910, 3338, 5, 5);      // and five level 22-23 ones (approx: all level 23)
        s("icefiend", 3003, 3479, 6, 10);              // Ice Mountain summit: level 13 icefiends (10 in OSRS)
        s("minecraft:wolf", 2847, 3488, 4, 10);        // White Wolf Mountain
        f("lesser_demon", 3113, 3162, WIZARDS_TOWER_TOP, 1, 2); // the caged demon atop the Wizards' Tower
        // Taverley Dungeon's black dragons and baby black dragons are camps in its far west.
        f("king_scorpion", 3044, 3380, DWARVEN_MINE, 2, 4);     // the scorpion pit in the Dwarven Mine

        // ============================================================ Wilderness
        s("zombie_lvl18", 3169, 3676, 8, 14);          // Graveyard of Shadows: level 18 zombies (15 in OSRS)
        s("zombie_lvl24", 3175, 3668, 3, 8);           // and level 24 ones (6 in OSRS)
        s("zombie_lvl18", 3162, 3734, 8, 12);          // north of the graveyard (15 level 18s in OSRS)
        s("skeleton_lvl25", 3247, 3736, 9, 16);        // Bone Yard: level 25 skeletons (9 in OSRS)
        s("dark_wizard", 2982, 3568, 4, 6);            // dark wizards' camp north of the Edgeville ditch
        s("minecraft:witch", 2984, 3570, 3, 6);        // and the level 20 ones with them
        s("green_dragon", 2977, 3616, 3, 6);           // west of the Graveyard of Shadows
        s("green_dragon", 3142, 3702, 4, 6);           // the Graveyard of Shadows' dragons
        s("green_dragon", 3338, 3690, 8, 14);          // east of the Bone Yard (11 in OSRS)
        s("green_dragon", 3104, 3816, 4, 10);          // west of the Lava Maze
        s("minecraft:spider", 3168, 3887, 8, 8);       // Spider Hill (10 in OSRS)
        s("minecraft:cave_spider", 3170, 3885, 6, 8);  // and its small spiders (17 in OSRS)
        s("greater_demon", 3292, 3883, 4, 10);         // Demonic Ruins
        s("lava_dragon", 3205, 3826, 6, 14);           // Lava Dragon Isle (8 in OSRS)
        s("king_scorpion", 3072, 3838, 3, 8);          // the Wilderness scorpion pit, level 40
        s("grizzly_bear", 3112, 3660, 4, 10);          // the Wilderness bears west of the graveyard
        s("grizzly_bear", 3079, 3602, 3, 6);
        s("giant_bat", 3076, 3957, 6, 6);              // the deep Wilderness bat spawn

        // ============================================================ Kharidian Desert
        s("desert_lizard", 3388, 3050, 4, 8);          // east of the Kalphite Lair
        s("lizard", 3440, 3030, 4, 8);                 // south-east of the Shantay Pass, towards Uzer

        // ============================================================ Morytania
        s("ghoul", 3433, 3463, 8, 10);                 // past the gate north of Paterdomus (10 in OSRS)
        s("giant_bat", 3747, 3397, 6, 12);             // eastern Morytania (6 in OSRS)
        // Swamp crabs, disguised as swampy logs (cache): south of Port Phasmatys, in the Haunted Woods and near Slepe.
        s("swamp_crab", 3691, 3441, 5, 5);
        s("swamp_crab", 3637, 3424, 4, 3);
        s("swamp_crab", 3596, 3422, 7, 5);
        s("swamp_crab", 3597, 3449, 8, 7);
        s("swamp_crab", 3755, 3360, 6, 7);
        // Terror dogs: Tarn's Lair below the Haunted Mine (a dungeon camp).
        // Shades of Mort'ton: loar shades roam the temple ruins (25 in OSRS); the stronger ones are
        // camps in the catacombs below the town.
        s("loar_shade", 3493, 3293, 10, 16);
        // Slayer Tower: ground floor, first floor, second floor (its basement is a dungeon camp).
        f("crawling_hand", 3416, 3550, SLAYER_TOWER_0, 4, 3);
        f("banshee", 3440, 3550, SLAYER_TOWER_0, 4, 3);
        // Upper floors as in the cache: bloodveld and abyssal demons in the north-west, infernal mages and nechryael in
        // the north-east, aberrant spectres in the south and gargoyles in the south-east.
        f("infernal_mage", 3440, 3562, SLAYER_TOWER_1, 3, 3);
        f("bloodveld", 3416, 3562, SLAYER_TOWER_1, 4, 3);
        f("aberrant_spectre", 3428, 3538, SLAYER_TOWER_1, 3, 3);
        f("gargoyle", 3440, 3540, SLAYER_TOWER_2, 3, 3);
        f("nechryael", 3440, 3562, SLAYER_TOWER_2, 3, 3);
        f("abyssal_demon", 3416, 3562, SLAYER_TOWER_2, 3, 3);

        // ============================================================ Kandarin
        s("minecraft:sheep", 2653, 3367, 8, 10);       // sheep north of East Ardougne (10 in OSRS)
        s("chaos_druid", 2562, 3356, 4, 4);            // Chaos Druid Tower
        s("grizzly_bear", 2703, 3336, 6, 12);          // north of the Legends' Guild (9 in OSRS)
        s("ogre_lvl63", 2527, 3374, 6, 6);             // Combat Training Camp: level 63 ogres (8 in OSRS)
        s("giant_bat", 2583, 3477, 6, 12);             // by Baxtorian Falls
        s("minecraft:cave_spider", 2540, 3185, 6, 8);  // spiders in the Tree Gnome Village maze

        // ============================================================ Fremennik Province, Troll Country
        s("rock_crab", 2706, 3718, 10, 8);             // rock crabs on the shore north-east of Rellekka (10 in OSRS)
        s("rock_crab", 2675, 3714, 9, 8);              // and on the north-western beach (9 in OSRS)
        s("rock_crab", 2525, 3755, 3, 8);              // Waterbirth Island's beaches (3 in OSRS)
        s("mountain_troll", 2870, 3600, 8, 12);        // Death Plateau (9 in OSRS)
        s("mountain_troll", 2848, 3679, 6, 8);         // outside the Troll Stronghold (8 in OSRS)

        // ============================================================ Karamja
        s("harpie_bug_swarm", 2866, 3110, 6, 6);       // swamp south-west of Musa Point
        s("mogre", 2992, 3120, 2, 4);                  // Mudskipper Point
        s("monkey", 2921, 3022, 6, 10);                // the jungle south of Musa Point (10 in OSRS)
        s("monkey", 2902, 3079, 4, 8);
        s("monkey", 2834, 3030, 4, 6);                 // north of Tai Bwo Wannai
        f("baby_red_dragon", 2743, 3100, BRIMHAVEN_DUNGEON, 3, 4); // Brimhaven Dungeon

        // ============================================================ Tirannwn
        // The Iorwerth camp north of the Arandar pass (cache: 7 Iorwerth warriors, level 108 like the elf warrior, and 6
        // archers at 2188-2210, 3239-3261). Isafdar's forest has no elf spawns in OSRS.
        s("elf_warrior", 2199, 3251, 7, 12);
        // Prifddinas (its OSRS coordinates, which count as Tirannwn): the farm animals of the cache.
        s("minecraft:chicken", 3304, 6106, 4, 4);
        s("minecraft:cow", 3216, 6166, 6, 4);

        // ============================================================ islands, Zeah
        // Catacombs of Kourend (the zeah module's crypt beneath the castle)
        fi("greater_nechryael", 1639, 3693, KOUREND_CATACOMBS, 2, 10);
        fi("brutal_black_dragon", 1648, 3686, KOUREND_CATACOMBS, 2, 8);
        fi("brutal_blue_dragon", 1630, 3700, KOUREND_CATACOMBS, 2, 8);  // approx. (no Myths' Guild dungeon)
        fi("brutal_red_dragon", 1630, 3686, KOUREND_CATACOMBS, 2, 8);   // approx.
        // Dorgesh-Kaan's southern caves join the Lumbridge Swamp Caves.
        f("molanisk", 3141, 3181, LUMBRIDGE_SWAMP_CAVES, 2, 3);
        // Mutated bloodveld: the Catacombs of Kourend (their Meiyerditch laboratory isn't built).
        fi("mutated_bloodveld", 1645, 3680, KOUREND_CATACOMBS, 4, 6);
        // Otherworldly beings haunt the west of Zanaris.
        si("otherworldly_being", 2392, 4416, 4, 4);

        // ============================================================ implings (rare wanderers)
        s("baby_impling", 3170, 3300, 1, 24, IMPLING);
        s("baby_impling", 3010, 3330, 1, 24, IMPLING);
        s("young_impling", 3130, 3330, 1, 24, IMPLING);
        s("young_impling", 2770, 3380, 1, 24, IMPLING);
        s("gourmet_impling", 2740, 3445, 1, 24, IMPLING);
        s("gourmet_impling", 2850, 3050, 1, 24, IMPLING);
        s("earth_impling", 2990, 3440, 1, 24, IMPLING);
        s("earth_impling", 2690, 3720, 1, 24, IMPLING);
        s("essence_impling", 3120, 3200, 1, 24, IMPLING);
        s("essence_impling", 3270, 3380, 1, 24, IMPLING);
        s("eclectic_impling", 2620, 3250, 1, 24, IMPLING);
        s("eclectic_impling", 2230, 3220, 1, 24, IMPLING);
        s("nature_impling", 2560, 3150, 1, 24, IMPLING);
        s("nature_impling", 2610, 2970, 1, 24, IMPLING);
        s("magpie_impling", 2840, 2985, 1, 24, IMPLING);
        s("magpie_impling", 2560, 3250, 1, 24, IMPLING);
        s("ninja_impling", 3420, 3380, 1, 24, IMPLING);
        s("ninja_impling", 3380, 3000, 1, 24, IMPLING);
        s("dragon_impling", 3140, 3790, 1, 24, IMPLING * 6);
        s("lucky_impling", 2320, 3250, 1, 24, IMPLING * 6);
        // Crystal implings only appear in Prifddinas, at 12 fixed spots (wiki map: west 3210-3240, 6046-6129 and east
        // 3290-3308, 6041-6117), one per world every 30 minutes.
        s("crystal_impling", 3226, 6088, 1, 32, IMPLING * 3);
        s("crystal_impling", 3299, 6080, 1, 32, IMPLING * 3);

        // ============================================================ Slayer task creatures (slayer module extras)
        s("crocodile", 3351, 2927, 6, 10);             // the oasis south of Pollnivneach (7 in OSRS)
        s("crocodile", 3268, 2852, 10, 16);            // the river shallows west of Nardah (20 in OSRS)
        s("crocodile", 3300, 2917, 4, 6);
        s("ent", 3305, 3612, 3, 12);                   // Wilderness ents (level 101 there)
        s("ent", 3210, 3690, 3, 10);
        s("mammoth", 3167, 3593, 6, 7);                // south-east of the Ferox Enclave (8 in OSRS)
        s("ice_spider", 2962, 3924, 6, 9);             // Frozen Waste Plateau (9 in OSRS)
        s("jungle_spider", 2675, 3090, 6, 9);          // Yanille peninsula, east of the town (17 in OSRS)
        s("jungle_spider", 2861, 3085, 5, 7);          // north Karamja jungle (13 in OSRS)
        s("jungle_spider", 2921, 3046, 5, 10);         // south of the Karamja volcano (14 in OSRS)
        s("jungle_spider", 2890, 3001, 4, 7);          // north of Tai Bwo Wannai (10 in OSRS)
        s("oomlie_bird", 2905, 2910, 5, 18);           // eastern Kharazi Jungle (10 in OSRS)
        s("oomlie_bird", 2800, 2918, 5, 20);           // western Kharazi Jungle (22 in OSRS)
        s("bandit", 3040, 3701, 4, 7, 100);            // Wilderness Bandit Camp (60 s respawn)
        s("wilderness_bandit", 3036, 3696, 5, 8, 100); // Wilderness Bandit Camp, level 130s
        // Slayer task variants added in the monster completeness audit (SlayerTaskMonsters), at their cache clusters.
        s("black_bear", 2970, 3481, 3, 6);             // north of the Mind Altar / Goblin Village road
        s("black_bear", 2478, 3200, 2, 10);            // north of Gu'Tanoth / the Gnome Maze
        s("bear_cub", 2691, 3612, 4, 4);               // the woods south of Rellekka
        s("bear_cub", 2721, 3643, 4, 4);
        si("bear_cub", 1250, 3475, 4, 10);              // north of Land's End
        s("grizzly_bear_cub", 2199, 3219, 3, 3);       // Isafdar
        s("grizzly_bear_cub", 2291, 3154, 2, 3);
        s("desert_wolf", 3263, 3071, 5, 7);            // between the Shantay Pass and the Bedabin Camp
        s("desert_wolf", 3310, 3070, 4, 5);
        s("desert_wolf", 3323, 3031, 3, 3);
        s("dire_wolf", 2316, 3269, 3, 5);              // Isafdar, south of Prifddinas
        s("dire_wolf", 2337, 3275, 3, 4);
        s("jungle_wolf", 2900, 2910, 4, 14);           // Kharazi Jungle (16 in OSRS)
        s("big_wolf", 2840, 3497, 1, 2);               // White Wolf Mountain
        s("jackal", 3214, 2865, 5, 3);                 // the desert south of the Shantay Pass (55 in OSRS)
        s("jackal", 3266, 2933, 4, 3);
        s("jackal", 3319, 2899, 4, 3);
        s("jackal", 3240, 2845, 4, 3);
        s("guard_dog", 2664, 3479, 3, 9);              // McGrubor's Wood / Seers' Village farms
        s("guard_dog", 2785, 3194, 3, 4);              // Brimhaven
        si("guard_dog", 1740, 3597, 2, 3);              // Hosidius
        s("small_lizard", 3407, 3060, 6, 10);          // the desert east of the Kalphite Lair (31 in OSRS)
        s("small_lizard", 3439, 3058, 6, 10);
        s("small_lizard", 3442, 3014, 5, 4);
        s("grave_scorpion", 2469, 3226, 5, 5);
        s("poison_scorpion", 2720, 3217, 5, 6);        // north of Brimhaven
        s("pit_scorpion", 2604, 3270, 4, 3);           // Ardougne Zoo
        s("tortured_soul", 3659, 3533, 3, 7);          // the Port Phasmatys farmland
        s("tortured_soul", 3670, 3532, 2, 5);
        s("undead_chicken", 3630, 3528, 6, 4);         // Alice's farm, west of Port Phasmatys
        s("undead_cow", 3617, 3525, 4, 6);
        s("zombie_rat", 2930, 3249, 7, 7);             // Rimmington
        s("vampyre_juvenile", 3564, 3244, 4, 9);       // Meiyerditch (35 in OSRS)
        s("vampyre_juvenile", 3561, 3213, 3, 7);
        s("vampyre_juvenile", 3631, 3221, 3, 6);
        s("vampyre_juvenile", 3649, 3352, 4, 10);      // Darkmeyer
        s("vyrewatch_sentinel", 3598, 3353, 5, 11);    // Darkmeyer (19 in OSRS)
        s("vyrewatch_sentinel", 3601, 3383, 3, 6);
        s("vyrewatch_sentinel", 3627, 3381, 3, 7);
        s("elf_archer", 2329, 3170, 1, 3);             // Lletya
        s("iorwerth_archer", 2206, 3256, 3, 5);        // the Iorwerth Camp
        s("iorwerth_archer", 2191, 3246, 2, 5);
        s("guard_bandit", 3037, 3665, 4, 7, 100);      // Wilderness Bandit Camp
        s("thrower_troll", 2856, 3599, 4, 7);          // Death Plateau
        s("thrower_troll", 2891, 3698, 4, 7);          // Trollheim
        s("troll_spectator", 2920, 3606, 3, 6);        // the Troll Stronghold arena
        s("troll_spectator", 2921, 3619, 2, 4);
        // The Fremennik Isles' ice trolls on their cache clusters ("ice_troll" is the ice troll male): south of the
        // Neitiznot bridge, on the snowfields north of Neitiznot and north of Jatizso.
        s("ice_troll_runt", 2330, 3833, 4, 5);
        s("ice_troll_runt", 2363, 3832, 3, 6);
        s("ice_troll_female", 2368, 3831, 2, 5);
        s("ice_troll", 2362, 3834, 1, 2);
        si("ice_troll_runt", 2340, 3862, 3, 13);
        si("ice_troll_female", 2342, 3862, 3, 13);
        si("ice_troll", 2339, 3862, 3, 13);
        si("ice_troll_runt", 2331, 3892, 2, 12);
        si("ice_troll_female", 2331, 3892, 4, 14);
        si("ice_troll", 2332, 3891, 2, 13);
        si("ice_troll_grunt", 2332, 3891, 2, 10);
        si("ice_troll_runt", 2369, 3892, 3, 9);
        si("ice_troll_female", 2369, 3892, 4, 10);
        si("ice_troll", 2368, 3892, 2, 9);
        si("ice_troll_grunt", 2371, 3892, 2, 9);
        si("ice_troll_runt", 2396, 3863, 4, 10);
        s("ice_troll_female", 2405, 3858, 2, 4);
        s("ice_troll", 2404, 3861, 5, 13);
        si("sorebones", 3813, 2844, 7, 7);              // Harmony Island
        si("zombie_pirate", 3790, 2846, 3, 6);
        si("zombie_pirate", 3799, 2853, 3, 6);
        si("zombie_pirate", 3796, 2832, 2, 5);
        s("undead_one", 2874, 2953, 9, 7);             // the Karamja burial grounds, north of Shilo Village
        s("zogre", 2466, 3048, 5, 6);                  // Jiggig
        s("seagull", 3028, 3203, 4, 3);                // Port Sarim docks
        s("seagull", 3028, 3234, 4, 3);
        s("black_guard", 3015, 3445, 4, 4);            // the Dwarven Mine entrance north of Falador
        si("pyrelord", 2255, 2960, 3, 5);               // Isle of Souls
        // Round 17 slayer task monsters (SlayerTaskMonsters2) at their cache clusters (wiki tiles for the zygomites).
        s("feral_vampyre", 3460, 3233, 6, 6);          // west of Burgh de Rott
        s("feral_vampyre", 3592, 3484, 7, 14);         // the Haunted Woods (19 in OSRS)
        s("feral_vampyre", 3628, 3466, 10, 18);
        s("ice_wolf", 2783, 3859, 7, 12);              // Trollweiss Mountain
        s("ice_wolf_lvl132", 2892, 3728, 10, 8);       // the Ice Path (25 in OSRS)
        s("ice_wolf_lvl132", 2893, 3753, 11, 9);
        s("ice_wolf_lvl132", 2925, 3758, 4, 5);        // outside the God Wars Dungeon
        s("ice_wolf_lvl132", 2850, 3923, 5, 3);        // Weiss, behind its fence
        si("ice_wolf_lvl132", 1582, 3896, 4, 5);        // the Settlement Ruins
        s("pee_hat", 2872, 3598, 1, 1);                // Death Plateau's named trolls
        s("kraka", 2875, 3598, 1, 1);
        s("troll_stick", 2857, 3591, 1, 1);
        s("troll_rock", 2855, 3591, 1, 1);
        s("skogre", 2463, 3046, 4, 8);                 // Jiggig
        s("rooster", 2680, 3662, 1, 2);                // Rellekka chicken pen
        s("rooster", 2816, 3560, 1, 2);                // Tenzing's yard
        si("ancient_zygomite", 3686, 3828, 7, 10);      // Fossil Island's mushroom forest (27 in OSRS)
        si("ancient_zygomite", 3684, 3852, 10, 12);
        si("ancient_zygomite", 3690, 3875, 10, 14);
        // Brine rats, Jormungand's basilisk knights, baby green dragons, sourhogs, shadow warriors and the
        // Wilderness God Wars Dungeon's spiritual creatures are dungeon camps (DungeonsLairs).
    }

    // ------------------------------------------------------------------ monsters that never roam

    /**
     * Monsters that only appear in an encounter, never at a fixed spot: boss minions and bodyguards,
     * raid and minigame monsters, slayer superiors, and monsters whose OSRS home is a realm or quest
     * area that isn't part of this world. Maps id to the reason.
     */
    public static synchronized Map<String, String> encounterOnly() {
        if (encounterOnly == null) {
            Map<String, String> m = new LinkedHashMap<>();
            for (ContentModule module : Modules.ALL) {
                String why = module instanceof BossesContent ? "boss minion"
                        : module instanceof RaidsContent ? "raid"
                        : module instanceof MinigamesContent ? "minigame" : null;
                if (why != null) {
                    for (MonsterDef d : module.monsters()) {
                        m.put(d.id, why);
                    }
                }
            }
            for (BossDef b : ContentRegistry.bosses()) {
                if (b.minion != null && !b.minion.contains(":")) {
                    m.putIfAbsent(b.minion, "minion of " + b.id);
                }
            }
            for (String id : Superiors.all().keySet()) {
                m.put(id, "slayer superior");
            }
            for (String id : List.of("flight_kilisa", "wingman_skree", "flockleader_geerin", "starlight", "growler", "bree")) {
                m.put(id, "God Wars general's bodyguard");
            }
            m.put("blood_reaver", "minion of nex");
            m.put("skeleton_hellhound", "minion of vetion");
            m.put("greater_skeleton_hellhound", "minion of vetion");
            m.put("venenatis_spiderling", "minion of venenatis");
            m.put("scorpias_offspring", "minion of scorpia");
            m.put("scorpias_guardian", "minion of scorpia");
            m.put("blood_jaguar", "minion of the blood moon");
            m.put("cyclops", "God Wars Dungeon: gwd_cyclops spawns there (Warriors' Guild cyclopes are a minigame)");
            m.put("killerwatt", "lives in the Killerwatt plane, which isn't part of this world");
            m.put("sea_snake_young", "lives in the Royal Trouble quest's sea snake cave, which isn't part of this world");
            m.put("minecraft:villager", "stats for stray vanilla villagers; townsfolk spawn as \"man\" and \"woman\"");
            encounterOnly = Collections.unmodifiableMap(m);
        }
        return encounterOnly;
    }

    /**
     * Underground monsters that this table nevertheless puts on the open ground, mapped to why: OSRS
     * has surface spawns of them too, or their OSRS dungeon isn't built here and they wait at its
     * entrance instead. Every other underground monster only spawns in a carved dungeon.
     */
    public static Map<String, String> surfaceStandIns() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("king_scorpion", "OSRS also has a surface scorpion pit in the Wilderness");
        m.put("giant_rat", "OSRS giant rats also roam Lumbridge Swamp above ground");
        m.put("chaos_druid", "OSRS chaos druids also live in the Chaos Druid Tower north of Ardougne");
        m.put("loar_shade", "loar shades roam the ruins of Mort'ton above ground in OSRS");
        m.put("ice_troll_runt", "OSRS ice trolls also live on the snowfields of the Fremennik Isles");
        m.put("ice_troll_female", "OSRS ice trolls also live on the snowfields of the Fremennik Isles");
        m.put("ice_troll_grunt", "OSRS ice trolls also live on the snowfields of the Fremennik Isles");
        return Collections.unmodifiableMap(m);
    }

    // ------------------------------------------------------------------ camps of other modules

    /** Every monster spawn already handled by a module's camp. */
    public static List<CampRef> camps() {
        List<CampRef> out = new ArrayList<>();
        for (Dungeons.Camp c : Dungeons.camps()) {
            int r = Math.max(1, Math.max(c.x2() - c.x1(), c.z2() - c.z1()) / 2 / GielinorMap.SCALE);
            for (String id : c.creatures()) {
                out.add(new CampRef(c.dungeon(), id, GielinorMap.toOsrsX(c.centreX()), GielinorMap.toOsrsY(c.centreZ()),
                        c.feetY(), r));
            }
        }
        for (GwdPlaces.Camp c : GwdPlaces.camps()) {
            addPlaceCamp(out, c.place(), c.dx(), c.dz(), c.radius(), SpawnPoint.SURFACE, c.creatures());
        }
        for (IslandsPlaces.Camp c : IslandsPlaces.camps()) {
            addPlaceCamp(out, c.place(), c.dx(), c.dz(), c.radius(), c.surface() ? SpawnPoint.SURFACE : c.floorY(), c.creatures());
        }
        for (ZeahPlaces.Camp c : ZeahPlaces.camps()) {
            addPlaceCamp(out, c.place(), c.dx(), c.dz(), c.radius(), c.surface() ? SpawnPoint.SURFACE : c.floorY(), c.creatures());
        }
        for (TownMonsters.TownSpawn t : TownMonsters.spawns()) {
            out.add(new CampRef("town", t.creature(), t.x(), t.y(), SpawnPoint.SURFACE, t.radius()));
        }
        return out;
    }

    private static void addPlaceCamp(List<CampRef> out, String place, int dx, int dz, int radiusBlocks, int floorY,
                                     List<String> creatures) {
        int[] tile = placeTile(place);
        if (tile == null) {
            return;
        }
        int x = tile[0] + Math.floorDiv(dx, GielinorMap.SCALE);
        int y = tile[1] - Math.floorDiv(dz, GielinorMap.SCALE);
        for (String id : creatures) {
            out.add(new CampRef(place, id, x, y, floorY, Math.max(1, radiusBlocks / GielinorMap.SCALE)));
        }
    }

    /** OSRS tile of a module place id or a {@link Landmark} name, or null. */
    public static int[] placeTile(String place) {
        PlaceDef p = ContentRegistry.place(place);
        if (p != null) {
            return new int[]{p.osrsX(), p.osrsY()};
        }
        Landmark l = Landmark.byName(place);
        return l == null ? null : new int[]{l.osrsX(), l.osrsY()};
    }

    // ------------------------------------------------------------------ creature info

    /** Level and aggression of a built-in, vanilla-mapped or module creature, or null if unknown. */
    public static Info info(String id) {
        Monster m = Monster.byEntity(id);
        if (m != null) {
            return new Info(id, m.displayName(), m.combatLevel(), m.aggressive());
        }
        MonsterDef d = ContentRegistry.monster(id);
        if (d != null) {
            return new Info(id, d.name, d.combatLevel, d.aggressive);
        }
        return null;
    }
}
