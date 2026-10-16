package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;
import java.util.Map;

/**
 * Agility shortcuts. {@link #SHORTCUTS} are on the surface at their OSRS spots: a shortcut block
 * stands at each end (placed on the surface when the chunk first loads, never over a structure
 * block); using it with the level requirement carries the player to the other end. Requirements are
 * the wiki's; most shortcuts give no experience (XP given only where the wiki lists some). End tiles
 * are approx. to a tile or two.
 *
 * <p>Grapple shortcuts ({@link #GRAPPLE}) also need a mith grapple carried and a crossbow wielded, as
 * well as their Ranged and Strength levels.
 *
 * <p>{@link #INDOOR} shortcuts are inside dungeons and buildings (Taverley pipe, Edgeville monkeybars,
 * Brimhaven Dungeon, Kalphite Lair rope, Slayer Tower chains, Fremennik and Stronghold Slayer
 * dungeons, Asgarnian Ice Dungeon): their two ends are exact block positions, carved into the
 * dungeons by the dungeons module ({@code Dungeons.shortcutEnds()}) or, for the Slayer Tower, placed
 * into the tower's storeys ({@link #TOWER_ENDS}).
 */
public final class AgilityShortcuts {
    public static final String BLOCK = "agility_shortcut";
    /** Requirement tag: a mith grapple carried and a crossbow wielded. */
    public static final String GRAPPLE = "grapple";
    /** Requirement tag: a rope carried (tied to the rocks to climb down). */
    public static final String ROPE = "rope";
    public static final String MITH_GRAPPLE = "mith_grapple";

    /**
     * One shortcut between end {@code a} and end {@code b} (OSRS tiles; unused for indoor shortcuts);
     * {@code other} = extra skill levels, {@code needs} = {@link #GRAPPLE}, {@link #ROPE} or null.
     */
    public record Shortcut(String id, String name, String verb, int level, double xp, int ax, int ay, int bx, int by,
                           Map<Skill, Integer> other, String needs) {
        /** True if (x, y) is within a tile of end a (0), end b (1); else -1. */
        public int endAt(int x, int y) {
            if (Math.abs(x - ax) <= 1 && Math.abs(y - ay) <= 1) {
                return 0;
            }
            if (Math.abs(x - bx) <= 1 && Math.abs(y - by) <= 1) {
                return 1;
            }
            return -1;
        }
    }

    private static Shortcut s(String id, String name, String verb, int level, double xp, int ax, int ay, int bx, int by) {
        return new Shortcut(id, name, verb, level, xp, ax, ay, bx, by, Map.of(), null);
    }

    private static Shortcut s(String id, String name, String verb, int level, double xp, int ax, int ay, int bx, int by,
                              Map<Skill, Integer> other) {
        return new Shortcut(id, name, verb, level, xp, ax, ay, bx, by, other, null);
    }

    /** A grapple shortcut: needs a mith grapple and a wielded crossbow as well as the levels. */
    private static Shortcut g(String id, String name, int level, int ax, int ay, int bx, int by, int strength, int ranged) {
        return new Shortcut(id, name, "grapple across", level, 0, ax, ay, bx, by,
                Map.of(Skill.STRENGTH, strength, Skill.RANGED, ranged), GRAPPLE);
    }

    /** An indoor shortcut (its ends are block positions given elsewhere). */
    private static Shortcut in(String id, String name, String verb, int level) {
        return in(id, name, verb, level, 0);
    }

    private static Shortcut in(String id, String name, String verb, int level, double xp) {
        return new Shortcut(id, name, verb, level, xp, 0, 0, 0, 0, Map.of(), null);
    }

    /**
     * Surface shortcuts. Ends, levels and XP follow the wiki's "List of shortcuts" ({@code agility_shortcuts.tsv}, diffed in
     * {@code AgilityShortcutSnapshotTest}) and the shortest-path transport tiles; where the two OSRS objects stand a tile
     * apart an end is moved a tile or two out (approx) so the two shortcut blocks don't touch. Every shortcut works both
     * ways (a few are one-way in OSRS).
     */
    public static final List<Shortcut> SHORTCUTS = List.of(
            s("falador_crumbling_wall", "Crumbling wall (Falador)", "climb over the crumbling wall", 5, 0.5, 2938, 3358, 2933, 3355),
            s("varrock_south_fence", "Fence (south of Varrock)", "jump over the fence", 13, 0, 3240, 3336, 3240, 3333),
            s("yanille_underwall_tunnel", "Underwall tunnel (Yanille)", "squeeze through the tunnel", 16, 0, 2575, 3112, 2575, 3107),
            s("coal_truck_log_balance", "Log balance (Coal Trucks)", "walk across the log", 20, 8.5, 2603, 3477, 2597, 3477),
            s("grand_exchange_tunnel", "Underwall tunnel (Grand Exchange)", "climb into the tunnel", 21, 0, 3142, 3513, 3137, 3516),
            // Not on the July 2026 wiki list; the level is shortest-path's Broken fence (object 62422). // approx
            s("draynor_manor_railing", "Broken fence (Draynor Manor)", "squeeze through the fence", 49, 0, 3083, 3353, 3086, 3353),
            s("ardougne_log_balance", "Log balance (East Ardougne river)", "walk across the log", 33, 4, 2598, 3332, 2598, 3337),
            g("catherby_obelisk_grapple", "Grapple (Water obelisk island to Catherby)", 36, 2841, 3427, 2841, 3433, 22, 39),
            s("grand_tree_ottos_grotto", "Rocks (Grand Tree to Otto's Grotto)", "climb the rocks", 37, 0, 2486, 3514, 2489, 3521),
            s("al_kharid_mining_pit", "Cliffside (Al Kharid mine)", "scramble up the cliffside", 38, 0, 3306, 3315, 3302, 3315),
            s("sinclair_log_balance", "Log balance (Sinclair Mansion)", "walk across the log", 48, 4, 2722, 3592, 2722, 3596),
            g("karamja_volcano_grapple", "Grapple (south of the Karamja volcano)", 53, 2874, 3133, 2874, 3127, 21, 42),
            // The Trollheim Wilderness route: a chain of rocks from the Trollheim side (2915, 3672) up to 2949, 3681.
            s("trollheim_wilderness", "Rocks (Trollheim to the Wilderness)", "climb the rocks", 64, 0, 2915, 3672, 2949, 3681),
            // The wiki's Fence (Burgh de Rott), between the town and the Abandoned Mine (end tiles approx).
            s("burgh_de_rott_wall", "Fence (Burgh de Rott)", "jump over the fence", 25, 0, 3482, 3231, 3482, 3236),
            s("lumbridge_swamp_stepping_stone", "Stepping stone (Lumbridge Swamp)", "jump across the stepping stone", 66, 0,
                    3212, 3138, 3214, 3130),
            // The stepping stones near the waterfall east of Shilo Village (the wiki's "Stepping stones (Karamja)", level 30).
            s("shilo_stepping_stones", "Stepping stones (east of Shilo Village)", "hop across the stepping stones", 30, 3, 2925, 2952,
                    2925, 2947),
            s("lava_dragon_isle_stepping_stones", "Stepping stone (Lava Dragon Isle)", "jump across the stepping stone", 74, 0,
                    3201, 3807, 3201, 3810),
            s("elven_overpass_advanced", "Cliffside (Elven overpass, advanced)", "climb the cliffside", 85, 0, 2338, 3253, 2332, 3252),
            // God Wars Dungeon entrance: Strength 60 moves the boulder (or 60 Agility squeezes through the crack beside it).
            s("gwd_boulder", "Boulder (God Wars Dungeon)", "move the boulder", 0, 0, 2898, 3715, 2898, 3719, Map.of(Skill.STRENGTH, 60)),
            s("gwd_little_crack", "Little crack (God Wars Dungeon)", "squeeze through the crack", 60, 0, 2900, 3713, 2904, 3720),
            g("lumbridge_river_grapple", "Grapple (River Lum to Al Kharid)", 8, 3246, 3179, 3259, 3179, 19, 37),
            g("falador_wall_grapple", "Grapple (Falador north wall)", 11, 3037, 3398, 3037, 3391, 37, 19),
            g("yanille_wall_grapple", "Grapple (Yanille south wall)", 39, 2556, 3068, 2556, 3073, 38, 21),
            // ---- Round 30: the rest of the wiki's surface shortcuts in built areas (shortest-path tiles). Shilo Village's stepping
            // stone and rocks, the Varrock Palace trellis, the Troll Stronghold wall-climb and Trollheim's hard scramble are left
            // out: their tiles are inside those structures. ----
            s("yanille_climbing_rocks", "Climbing rocks (Yanille)", "climb the rocks", 5, 25, 2561, 3106, 2561, 3111),
            s("goblin_village_tight_gap", "Tight-gap (Goblin Village)", "manoeuvre past the gap", 14, 0, 2926, 3522, 2929, 3519),
            s("cairn_isle_rock_slide", "Rock slide (Cairn Isle)", "climb the rock slide", 15, 0, 2795, 2978, 2791, 2978),
            g("observatory_grapple", "Grapple (Observatory cliff)", 23, 2449, 3151, 2441, 3165, 28, 24),
            s("eagles_peak_rocks", "Rocks (Eagles' Peak)", "climb the rocks", 25, 1, 2322, 3502, 2324, 3497),
            s("falador_underwall_tunnel", "Underwall tunnel (Falador)", "climb into the tunnel", 26, 0, 2948, 3309, 2948, 3313),
            s("mount_karuulm_rocks_lower", "Rocks (Mount Karuulm, lower)", "climb the rocks", 29, 0, 1324, 3777, 1324, 3785),
            s("champions_guild_stepping_stones", "Stepping stones (Champions' Guild)", "hop across the stepping stones", 31, 3,
                    3149, 3363, 3154, 3363),
            g("catherby_cliff_grapple", "Grapple (Catherby cliffside)", 32, 2866, 3428, 2869, 3428, 35, 35),
            s("river_hos_stepping_stones", "Stepping stones (River Hos)", "jump across the stepping stones", 40, 0, 1614, 3570, 1610, 3570),
            s("trollheim_rocks_easy", "Rocks (Trollheim, easy)", "scramble up the rocks", 41, 0, 2872, 3671, 2869, 3671),
            s("draynor_underwall_tunnel", "Underwall tunnel (Draynor Village)", "climb into the tunnel", 42, 0, 3070, 3257, 3066, 3257),
            s("trollheim_rocks_medium", "Rocks (Trollheim, medium)", "scramble up the rocks", 43, 0, 2878, 3665, 2878, 3668),
            s("trollheim_rocks_advanced", "Rocks (Trollheim, advanced)", "scramble up the rocks", 44, 0, 2907, 3682, 2909, 3685),
            s("hosidius_stepping_stones", "Stepping stones (Hosidius)", "jump across the stepping stones", 45, 0, 1720, 3551, 1724, 3551),
            s("dense_essence_boulder", "Boulder (dense essence mine)", "leap the boulder", 49, 0, 1776, 3884, 1776, 3880),
            s("mort_myre_stepping_stone", "Stepping stone (Mort Myre Swamp)", "jump across the stepping stone", 50, 0, 3422, 3325,
                    3417, 3325),
            s("dense_essence_rocks_east", "Rocks (dense essence mine, east)", "scramble over the rocks", 52, 0, 1769, 3849, 1774, 3849),
            s("miscellania_stepping_stone", "Stepping stone (Miscellania docks)", "jump across the stepping stone", 55, 0, 2573, 3859,
                    2576, 3862),
            s("rellekka_broken_fence", "Broken fence (Rellekka)", "jump over the fence", 57, 0, 2688, 3697, 2691, 3697),
            s("tirannwn_rocks_easy", "Rocks (Elven overpass, easy)", "climb the cliffside", 59, 0, 2346, 3300, 2344, 3294),
            s("gwd_rocky_handholds", "Rocky handholds (God Wars Dungeon, Wilderness)", "climb the rocky handholds", 60, 0, 2927, 3757,
                    2927, 3761),
            s("mos_leharmless_stepping_stone", "Stepping stone (Mos Le'Harmless)", "jump across the stepping stone", 60, 0, 3708, 2969,
                    3714, 2969),
            s("mount_karuulm_rocks_upper", "Rocks (Mount Karuulm, upper)", "climb the rocks", 62, 0, 1324, 3788, 1324, 3795),
            s("salve_ornate_railing", "Ornate railing (Temple on the Salve)", "squeeze through the railing", 65, 0, 3423, 3476, 3427, 3477),
            s("taverley_climbing_rocks", "Climbing rocks (Taverley)", "climb the rocks", 66, 25, 2946, 3439, 2943, 3439),
            s("tirannwn_rocks_medium", "Rocks (Elven overpass, medium)", "climb the cliffside", 68, 0, 2338, 3286, 2338, 3281),
            s("ice_mountain_rocks", "Rocks (Ice Mountain)", "scramble up the rocks", 68, 0, 2997, 3483, 3002, 3483),
            s("dense_essence_rocks_north", "Rocks (dense essence mine, north)", "scramble over the rocks", 69, 0, 1761, 3875, 1761, 3871),
            s("gutanoth_crumbling_wall", "Crumbling wall (Gu'Tanoth)", "climb the crumbling wall", 71, 0, 2545, 3031, 2549, 3031),
            s("pollnivneach_stepping_stone", "Stepping stone (Pollnivneach)", "jump across the stepping stone", 71, 25, 3372, 2959,
                    3370, 2955),
            s("chaos_temple_stepping_stone", "Stepping stone (Wilderness Chaos Temple)", "jump across the stepping stone", 72, 0,
                    3268, 3629, 3268, 3625),
            s("dense_essence_rocks_west", "Rocks (dense essence mine, west)", "climb down the rocks", 73, 0, 1742, 3854, 1752, 3854),
            s("zul_andra_stepping_stone", "Stepping stone (Zul-Andra)", "jump across the stepping stone", 76, 0, 2154, 3072, 2160, 3072),
            s("kharazi_jungle_vine", "Vine (Kharazi Jungle)", "climb the vine", 79, 0, 2899, 2937, 2899, 2942),
            s("lava_maze_stepping_stone", "Stepping stone (Lava Maze)", "jump across the stepping stone", 82, 0, 3092, 3878, 3092, 3883),
            s("crandor_rocks", "Rocks (Crandor)", "climb the rocks", 84, 0, 2832, 3247, 2832, 3252),
            s("waterbirth_island_rocks", "Rocks (Waterbirth Island)", "climb the rocks", 85, 0, 2547, 3749, 2547, 3744));

    /**
     * The wiki shortcut (page name in {@code agility_shortcuts.tsv}) each shortcut is; the shortcuts in {@link #UNLISTED} are
     * not on the wiki's list.
     */
    public static final Map<String, String> WIKI = Map.ofEntries(
            Map.entry("falador_crumbling_wall", "Crumbling wall (Falador)"), Map.entry("varrock_south_fence", "Fence (Varrock)"),
            Map.entry("yanille_underwall_tunnel", "Underwall tunnel (Yanille)"), Map.entry("coal_truck_log_balance", "Log balance (Coal Trucks)"),
            Map.entry("grand_exchange_tunnel", "Underwall tunnel (Grand Exchange)"), Map.entry("ardougne_log_balance", "Log balance (East Ardougne)"),
            Map.entry("catherby_obelisk_grapple", "Crossbow Tree"), Map.entry("grand_tree_ottos_grotto", "Rocks (Tree Gnome Stronghold)"),
            Map.entry("al_kharid_mining_pit", "Rocks (Al Kharid)"), Map.entry("sinclair_log_balance", "Log balance (Fremennik Province)"),
            Map.entry("karamja_volcano_grapple", "Strong Tree (Karamja volcano)"), Map.entry("trollheim_wilderness", "Rocks (Wilderness, Trollheim)"),
            Map.entry("burgh_de_rott_wall", "Fence (Burgh de Rott)"), Map.entry("lumbridge_swamp_stepping_stone", "Stepping stone (Lumbridge Swamp)"),
            Map.entry("shilo_stepping_stones", "Stepping stones (Karamja)"), Map.entry("lava_dragon_isle_stepping_stones", "Stepping stone (Lava Dragon Isle)"),
            Map.entry("elven_overpass_advanced", "Rocks (Tirannwn)#Advanced"), Map.entry("gwd_little_crack", "Little crack"),
            Map.entry("lumbridge_river_grapple", "Broken Raft"), Map.entry("falador_wall_grapple", "Wall (Falador)"),
            Map.entry("yanille_wall_grapple", "Wall (Yanille)"), Map.entry("yanille_climbing_rocks", "Climbing rocks (Yanille)"),
            Map.entry("goblin_village_tight_gap", "Tight-gap (Goblin Village)"), Map.entry("cairn_isle_rock_slide", "Rock slide (Cairn Isle)"),
            Map.entry("observatory_grapple", "Rocks (Observatory)"), Map.entry("eagles_peak_rocks", "Rocks (Eagles' Peak)"),
            Map.entry("falador_underwall_tunnel", "Underwall tunnel (Falador)"), Map.entry("mount_karuulm_rocks_lower", "Rocks (Mount Karuulm)#Lower"),
            Map.entry("champions_guild_stepping_stones", "Stepping stone (Champions' Guild)"),
            Map.entry("catherby_cliff_grapple", "Rocks (Catherby)"), Map.entry("river_hos_stepping_stones", "Stepping stones (River Hos)"),
            Map.entry("trollheim_rocks_easy", "Rocks (Trollheim easy)"), Map.entry("draynor_underwall_tunnel", "Underwall tunnel (Draynor Village)"),
            Map.entry("trollheim_rocks_medium", "Rocks (Trollheim medium)"), Map.entry("trollheim_rocks_advanced", "Rocks (Trollheim advanced)"),
            Map.entry("hosidius_stepping_stones", "Stepping stones (Hosidius)"),
            Map.entry("dense_essence_boulder", "Boulder (dense essence mine)"), Map.entry("mort_myre_stepping_stone", "Stepping stone (Mort Myre Swamp)"),
            Map.entry("dense_essence_rocks_east", "Rocks (dense essence mine, east)"), Map.entry("miscellania_stepping_stone", "Stepping stone (Miscellania)"),
            Map.entry("rellekka_broken_fence", "Broken fence (Rellekka)"), Map.entry("tirannwn_rocks_easy", "Rocks (Tirannwn)#Easy"),
            Map.entry("gwd_rocky_handholds", "Rocky handholds (God Wars Dungeon, Wilderness)"),
            Map.entry("mos_leharmless_stepping_stone", "Stepping stone (Mos Le'Harmless)"), Map.entry("mount_karuulm_rocks_upper", "Rocks (Mount Karuulm)#Upper"),
            Map.entry("salve_ornate_railing", "Ornate railing"), Map.entry("taverley_climbing_rocks", "Climbing rocks (Taverley)"),
            Map.entry("tirannwn_rocks_medium", "Rocks (Tirannwn)#Medium"), Map.entry("ice_mountain_rocks", "Rocks (Ice Mountain)"),
            Map.entry("dense_essence_rocks_north", "Rocks (dense essence mine, north)"), Map.entry("gutanoth_crumbling_wall", "Crumbling wall (Gu'Tanoth)"),
            Map.entry("pollnivneach_stepping_stone", "Stepping stone (Pollnivneach)"),
            Map.entry("chaos_temple_stepping_stone", "Stepping stone (Wilderness Chaos Temple)"), 
            Map.entry("dense_essence_rocks_west", "Rocks (dense essence mine, west)"), Map.entry("zul_andra_stepping_stone", "Stepping stone (Zul-Andra)"),
            Map.entry("kharazi_jungle_vine", "Vine (Kharazi Jungle)"),
            Map.entry("lava_maze_stepping_stone", "Stepping stone (Lava Maze)"), Map.entry("crandor_rocks", "Rocks (Crandor)"),
            Map.entry("waterbirth_island_rocks", "Rocks (Waterbirth Island)"),
            Map.entry("taverley_dungeon_pipe", "Obstacle pipe (Taverley Dungeon)"), Map.entry("edgeville_dungeon_monkeybars", "Monkey bars (Edgeville Dungeon)"),
            Map.entry("brimhaven_stepping_stones", "Stepping stones (Western Brimhaven Dungeon)"),
            Map.entry("brimhaven_pipe_moss_giants", "Pipe (Brimhaven Dungeon Entrance)"), Map.entry("brimhaven_log_balance", "Log balance (Brimhaven Dungeon)"),
            Map.entry("brimhaven_pipe_dragons", "Pipe (Brimhaven Dungeon)"), Map.entry("slayer_tower_chain_low", "Spikey chain (Slayer Tower medium)"),
            Map.entry("slayer_tower_chain_high", "Spikey chain (Slayer Tower advanced)"), Map.entry("fremennik_slayer_crevice", "Crevice (Fremennik Slayer Dungeon)"),
            Map.entry("fremennik_slayer_strange_floor", "Strange floor (Fremennik Slayer Dungeon)"),
            Map.entry("stronghold_slayer_tunnel", "Tunnel (Stronghold Slayer Cave)"), Map.entry("asgarnian_ice_tunnel", "Tunnel (Asgarnian Ice Dungeon, level 72)"),
            Map.entry("tonali_log_balance", "Log balance (Tonali Cavern)"), Map.entry("tonali_stepping_stone", "Log balance (Tonali Cavern)"),
            Map.entry("stalker_den_stepping_stone", "Stepping stone (Stalker Den)"));

    /** Shortcuts that are not on the wiki's list: the Kalphite Lair rope, the God Wars boulder (Strength) and Draynor Manor's fence. */
    public static final java.util.Set<String> UNLISTED = java.util.Set.of("kalphite_lair_rope", "gwd_boulder", "draynor_manor_railing");

    /**
     * Shortcuts inside dungeons and buildings (wiki levels and XP; the Fremennik Slayer Dungeon's crevice is 43 and its
     * strange floor 61 on the wiki's July 2026 list). The Kalphite Lair's entrance is climbed down on a rope tied to the
     * rocks (no level needed). Which way the Slayer Tower chains run is approx. (61: ground to first floor, 71: first to
     * second floor).
     */
    public static final List<Shortcut> INDOOR = List.of(
            in("taverley_dungeon_pipe", "Obstacle pipe (Taverley Dungeon blue dragons)", "squeeze through the pipe", 70, 10),
            in("edgeville_dungeon_monkeybars", "Monkeybars (Edgeville Dungeon)", "swing across the monkeybars", 15, 20),
            in("brimhaven_stepping_stones", "Stepping stones (Brimhaven Dungeon)", "hop across the stepping stones", 12, 7.5),
            in("brimhaven_pipe_moss_giants", "Pipe (Brimhaven Dungeon moss giants)", "squeeze through the pipe", 22, 8.5),
            in("brimhaven_log_balance", "Log balance (Brimhaven Dungeon)", "walk across the log", 30, 10),
            in("brimhaven_pipe_dragons", "Pipe (Brimhaven Dungeon dragons)", "squeeze through the pipe", 34, 10),
            new Shortcut("kalphite_lair_rope", "Rope (Kalphite Lair entrance)", "climb down the rope", 0, 0, 0, 0, 0, 0, Map.of(), ROPE),
            in("slayer_tower_chain_low", "Spiked chain (Slayer Tower, first floor)", "climb the spiked chain", 61),
            in("slayer_tower_chain_high", "Spiked chain (Slayer Tower, second floor)", "climb the spiked chain", 71),
            in("fremennik_slayer_crevice", "Narrow crevice (Fremennik Slayer Dungeon)", "squeeze through the crevice", 43),
            in("fremennik_slayer_strange_floor", "Strange floor (Fremennik Slayer Dungeon)", "jump over the strange floor", 61, 10),
            in("stronghold_slayer_tunnel", "Tunnel (Stronghold Slayer Cave)", "crawl through the tunnel", 72),
            in("asgarnian_ice_tunnel", "Tunnel (Asgarnian Ice Dungeon)", "crawl through the tunnel", 72),
            // Varlamore (round 28): Tonali Cavern's log balance and stepping stone (46), the Stalker Den's stepping stone (64).
            in("tonali_log_balance", "Log balance (Tonali Cavern)", "walk across the log", 46),
            in("tonali_stepping_stone", "Stepping stone (Tonali Cavern)", "cross the stepping stone", 46),
            in("stalker_den_stepping_stone", "Stepping stone (Stalker Den)", "cross the stepping stone", 64));

    /** Two block positions joined by an indoor shortcut: end a and end b, each the block the shortcut stands in. */
    public record BlockEnds(String id, int x1, int y1, int z1, int x2, int y2, int z2) {
        /** 0 if (x, y, z) is end a, 1 if end b (within a block sideways, same height), else -1. */
        public int endAt(int x, int y, int z) {
            if (y == y1 && Math.abs(x - x1) <= 1 && Math.abs(z - z1) <= 1) {
                return 0;
            }
            if (y == y2 && Math.abs(x - x2) <= 1 && Math.abs(z - z2) <= 1) {
                return 1;
            }
            return -1;
        }
    }

    /**
     * The Slayer Tower chains, placed into the tower's storeys (feet levels {@code GROUND}, +7 and
     * +14) by the west and east walls of its inner chamber (approx. spots).
     */
    public static final List<BlockEnds> TOWER_ENDS = List.of(
            towerEnds("slayer_tower_chain_low", 3418, 3546, 0, 7),
            towerEnds("slayer_tower_chain_high", 3438, 3546, 7, 14));

    private static BlockEnds towerEnds(String id, int x, int y, int floorA, int floorB) {
        int bx = StructureDef.bx(x);
        int bz = StructureDef.bz(y);
        int g = StructureDef.GROUND;
        return new BlockEnds(id, bx, g + floorA, bz, bx + 1, g + floorB, bz);
    }

    private AgilityShortcuts() {
    }

    /** The indoor shortcut with this id, or null. */
    public static Shortcut indoor(String id) {
        for (Shortcut s : INDOOR) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** True if the item is a crossbow that can fire a grapple (any crossbow but Karil's). */
    public static boolean isCrossbow(String itemId) {
        return itemId != null && (itemId.endsWith("_crossbow") || itemId.equals("crossbow")) && !itemId.equals("karils_crossbow");
    }

    /** The surface shortcut with an end within a tile of (x, y), or null. */
    public static Shortcut at(int x, int y) {
        for (Shortcut s : SHORTCUTS) {
            if (s.endAt(x, y) >= 0) {
                return s;
            }
        }
        return null;
    }
}
