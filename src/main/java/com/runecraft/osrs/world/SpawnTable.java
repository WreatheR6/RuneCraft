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
    private static final int TAVERLEY_DUNGEON = 21;
    private static final int BRIMHAVEN_DUNGEON = 25;
    private static final int LUMBRIDGE_SWAMP_CAVES = 23;
    private static final int KOUREND_CATACOMBS = 39;

    private static void build() {
        // ============================================================ Misthalin
        // Lumbridge
        s("goblin", 3250, 3235, 6, 10);                // east of the River Lum
        s("goblin", 3145, 3262, 3, 6);                 // north-west of Lumbridge, towards Draynor
        s("minecraft:chicken", 3233, 3297, 5, 5);      // chicken farm east of the river
        s("minecraft:chicken", 3185, 3277, 4, 4);      // Fred's farm
        s("minecraft:cow", 3256, 3274, 6, 10);         // cow field east of the river
        s("minecraft:cow", 3176, 3318, 4, 6);          // cow field north of Fred's farm
        s("minecraft:sheep", 3200, 3266, 5, 6);        // Fred's sheep field
        // Lumbridge Swamp
        s("giant_rat", 3215, 3172, 3, 8);
        s("minecraft:silverfish", 3165, 3190, 3, 8);   // the smaller giant rats (level 3)
        s("minecraft:cave_spider", 3178, 3165, 3, 8);  // spiders (level 1)
        // Draynor
        s("dark_wizard", 3083, 3228, 3, 5);            // south of Draynor Village
        // Imps blink about the countryside of Misthalin (and Asgarnia, below)
        s("imp", 3180, 3245, 2, 14);                   // fields west of Lumbridge
        s("imp", 3115, 3290, 2, 14);                   // north of Draynor Village, towards the manor
        s("imp", 3255, 3365, 2, 14);                   // south-east of Varrock, by the Lumber Yard road
        s("imp", 3160, 3395, 2, 12);                   // west of Varrock
        // Varrock
        s("minecraft:witch", 3227, 3369, 2, 6);        // the level 20 dark wizards at the stone circle
        s("scorpion", 3300, 3300, 4, 8);               // Al Kharid mine
        // Edgeville / Barbarian Village: town residents and the dungeon are camps.

        // ============================================================ Asgarnia
        s("goblin", 2956, 3505, 6, 10);                // Goblin Village
        s("minecraft:chicken", 3014, 3290, 4, 5);      // farm south of Falador
        s("minecraft:cow", 3029, 3308, 4, 6);          // cow field south of Falador
        s("minecraft:cow", 2926, 3280, 3, 5);          // pen by the Crafting Guild
        s("hobgoblin", 2910, 3285, 4, 6);              // hobgoblin peninsula west of the Crafting Guild
        s("imp", 2995, 3325, 2, 14);                   // farmland south of Falador
        s("imp", 2935, 3420, 2, 12);                   // between Falador and Taverley
        s("dark_wizard", 2908, 3335, 3, 5);            // Dark Wizards' Tower
        s("minecraft:witch", 2910, 3338, 2, 5);
        s("icefiend", 3007, 3477, 4, 8);               // Ice Mountain summit
        s("minecraft:wolf", 2847, 3488, 4, 10);        // White Wolf Mountain
        f("lesser_demon", 3113, 3162, WIZARDS_TOWER_TOP, 1, 2); // the caged demon atop the Wizards' Tower
        f("black_dragon", 2860, 3375, TAVERLEY_DUNGEON, 2, 4);  // deep Taverley Dungeon
        f("baby_black_dragon", 2862, 3378, TAVERLEY_DUNGEON, 3, 4);

        // ============================================================ Wilderness
        s("minecraft:zombie", 3165, 3675, 5, 10);      // Graveyard of Shadows
        s("minecraft:skeleton", 3236, 3745, 5, 10);    // Bone Yard
        s("green_dragon", 2980, 3615, 4, 8);           // west of the Graveyard of Shadows
        s("baby_green_dragon", 2985, 3625, 2, 6);
        s("green_dragon", 3340, 3690, 4, 8);           // east of the Bone Yard
        s("minecraft:spider", 3168, 3887, 4, 8);       // Spider Hill
        s("greater_demon", 3288, 3886, 3, 8);          // Demonic Ruins
        s("lava_dragon", 3200, 3830, 4, 10);           // Lava Dragon Isle
        s("king_scorpion", 3040, 3925, 3, 6);          // by the Deep Wilderness Dungeon
        s("grizzly_bear", 3115, 3720, 2, 6);           // Wilderness bear spawn

        // ============================================================ Kharidian Desert
        s("desert_lizard", 3388, 3050, 4, 8);          // east of the Kalphite Lair
        s("lizard", 3440, 3030, 4, 8);                 // south-east of the Shantay Pass, towards Uzer

        // ============================================================ Morytania
        s("ghoul", 3433, 3463, 4, 8);                  // past the gate north of Paterdomus
        s("giant_bat", 3460, 3380, 3, 8);              // Mort Myre / Haunted Woods
        s("terror_dog", 3424, 3240, 4, 6);             // Tarn's Lair, beneath the Haunted Mine
        s("mutated_bloodveld", 3626, 3250, 4, 8);      // Meiyerditch laboratories
        // Shades of Mort'ton: loar shades roam the temple ruins, the stronger ones its catacombs
        // (the catacombs aren't built, so they gather around the ruined town).
        s("loar_shade", 3490, 3300, 4, 8);
        s("phrin_shade", 3478, 3292, 3, 6);
        s("riyl_shade", 3502, 3288, 3, 6);
        s("asyn_shade", 3484, 3280, 2, 5);
        s("fiyr_shade", 3500, 3276, 2, 5);
        // Slayer Tower: ground floor, first floor, second floor (its basement is a dungeon camp).
        f("crawling_hand", 3416, 3550, SLAYER_TOWER_0, 4, 3);
        f("banshee", 3440, 3550, SLAYER_TOWER_0, 4, 3);
        f("infernal_mage", 3416, 3550, SLAYER_TOWER_1, 3, 3);
        f("bloodveld", 3440, 3550, SLAYER_TOWER_1, 4, 3);
        f("aberrant_spectre", 3428, 3538, SLAYER_TOWER_1, 3, 3);
        f("gargoyle", 3416, 3550, SLAYER_TOWER_2, 3, 3);
        f("nechryael", 3440, 3550, SLAYER_TOWER_2, 3, 3);
        f("abyssal_demon", 3428, 3538, SLAYER_TOWER_2, 3, 3);

        // ============================================================ Kandarin
        s("minecraft:sheep", 2615, 3363, 4, 6);        // sheep north of East Ardougne
        s("chaos_druid", 2562, 3356, 3, 5);            // Chaos Druid Tower
        s("grizzly_bear", 2698, 3335, 2, 6);           // north of the Legends' Guild
        s("ogre", 2520, 3375, 3, 8);                   // Combat Training Camp

        // ============================================================ Fremennik Province, Troll Country
        s("brine_rat", 2752, 3735, 3, 6);              // Brine Rat Cavern, east of Rellekka
        s("rock_crab", 2706, 3718, 6, 8);              // rock crabs on the shore north-east of Rellekka
        s("rock_crab", 2675, 3714, 5, 8);              // and on the north-western beach
        si("rock_crab", 2525, 3755, 5, 8);             // Waterbirth Island's beaches
        s("mountain_troll", 2870, 3600, 4, 10);        // Death Plateau
        s("mountain_troll", 2825, 3680, 3, 8);         // outside the Troll Stronghold

        // ============================================================ Karamja
        s("giant_bat", 2875, 3160, 3, 8);              // north of the volcano
        s("harpie_bug_swarm", 2866, 3110, 4, 6);       // swamp south-west of Musa Point
        si("mogre", 2992, 3120, 2, 4);                 // Mudskipper Point
        s("monkey", 2795, 3070, 3, 10);                // jungle around Tai Bwo Wannai
        f("baby_red_dragon", 2743, 3100, BRIMHAVEN_DUNGEON, 3, 4); // Brimhaven Dungeon

        // ============================================================ Tirannwn
        s("elf_warrior", 2195, 3255, 4, 8);            // near the elf camp
        s("elf_warrior", 2240, 3176, 3, 8);            // Isafdar forest

        // ============================================================ islands, Zeah
        si("basilisk_knight", 2470, 3990, 3, 6);       // Jormungand's Prison, Island of Stone
        // Catacombs of Kourend (the zeah module's crypt beneath the castle)
        fi("greater_nechryael", 1639, 3693, KOUREND_CATACOMBS, 2, 10);
        fi("brutal_black_dragon", 1648, 3686, KOUREND_CATACOMBS, 2, 8);
        fi("brutal_blue_dragon", 1630, 3700, KOUREND_CATACOMBS, 2, 8);  // approx. (no Myths' Guild dungeon)
        fi("brutal_red_dragon", 1630, 3686, KOUREND_CATACOMBS, 2, 8);   // approx.
        // Dorgesh-Kaan's southern caves join the Lumbridge Swamp Caves.
        f("molanisk", 3141, 3181, LUMBRIDGE_SWAMP_CAVES, 2, 3);

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
        s("magpie_impling", 2420, 3200, 1, 24, IMPLING);
        s("ninja_impling", 3420, 3380, 1, 24, IMPLING);
        s("ninja_impling", 3380, 3000, 1, 24, IMPLING);
        s("dragon_impling", 3140, 3790, 1, 24, IMPLING * 6);
        s("lucky_impling", 2320, 3250, 1, 24, IMPLING * 6);

        // ============================================================ Slayer task creatures (slayer module extras)
        s("sourhog", 3150, 3345, 4, 5);                // Sourhog Cave entrance north of Draynor (cave not built; approx)
        s("crocodile", 3385, 2960, 4, 6);              // River Elid bank, east of Pollnivneach
        s("crocodile", 3385, 2885, 3, 6);              // River Elid bank, south towards Nardah
        s("shadow_warrior", 2732, 3338, 4, 4);         // by the Legends' Guild (its dungeon isn't built)
        s("otherworldly_being", 3198, 3164, 4, 5);     // Zanaris (this world's Zanaris sits in Lumbridge Swamp)
        s("ent", 3305, 3612, 3, 6);                    // Wilderness ent grove (approx)
        s("mammoth", 3170, 3598, 4, 7);                // south-east of the Ferox Enclave
        s("ice_spider", 2955, 3912, 4, 7);             // Frozen Waste Plateau
        s("bandit", 3040, 3701, 4, 7, 100);            // Wilderness Bandit Camp (60 s respawn)
        s("wilderness_bandit", 3036, 3696, 5, 8, 100); // Wilderness Bandit Camp, level 130s
        // Wilderness God Wars Dungeon (entrance cave; the dungeon itself isn't built)
        s("spiritual_warrior", 3017, 3745, 2, 5);
        s("spiritual_ranger", 3021, 3741, 2, 5);
        s("spiritual_mage", 3013, 3741, 2, 5);
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
            encounterOnly = Collections.unmodifiableMap(m);
        }
        return encounterOnly;
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
