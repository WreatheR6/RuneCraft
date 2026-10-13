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
        return new Shortcut(id, name, verb, level, 0, 0, 0, 0, 0, Map.of(), null);
    }

    public static final List<Shortcut> SHORTCUTS = List.of(
            s("falador_crumbling_wall", "Crumbling wall (Falador)", "climb over the crumbling wall", 5, 0, 2938, 3358, 2933, 3355),
            s("varrock_south_fence", "Fence (south of Varrock)", "jump over the fence", 13, 0, 3240, 3336, 3240, 3333),
            s("yanille_underwall_tunnel", "Underwall tunnel (Yanille)", "squeeze through the tunnel", 16, 0, 2575, 3112, 2571, 3103),
            s("coal_truck_log_balance", "Log balance (Coal Trucks)", "walk across the log", 20, 0, 2603, 3477, 2597, 3477),
            s("grand_exchange_tunnel", "Underwall tunnel (Grand Exchange)", "climb into the tunnel", 21, 0, 3144, 3514, 3137, 3517),
            s("draynor_manor_railing", "Broken railing (Draynor Manor)", "squeeze through the railing", 28, 0, 3083, 3353, 3086, 3353),
            s("ardougne_log_balance", "Log balance (East Ardougne river)", "walk across the log", 33, 0, 2598, 3332, 2598, 3337),
            g("catherby_obelisk_grapple", "Grapple (Water obelisk island to Catherby)", 36, 2841, 3426, 2841, 3433, 22, 39),
            s("grand_tree_ottos_grotto", "Rocks (Grand Tree to Otto's Grotto)", "climb the rocks", 37, 0, 2486, 3514, 2489, 3521),
            s("al_kharid_mining_pit", "Cliffside (Al Kharid mine)", "scramble up the cliffside", 38, 0, 3305, 3315, 3302, 3315),
            s("sinclair_log_balance", "Log balance (Sinclair Mansion)", "walk across the log", 48, 0, 2722, 3592, 2722, 3597),
            g("karamja_volcano_grapple", "Grapple (south of the Karamja volcano)", 53, 2869, 3125, 2874, 3128, 21, 42),
            s("trollheim_wilderness", "Rocks (Trollheim to the Wilderness)", "climb the rocks", 64, 0, 2949, 3768, 2953, 3772),
            s("burgh_de_rott_wall", "Low wall (Burgh de Rott)", "squeeze under the wall", 65, 0, 3482, 3231, 3482, 3236),
            s("lumbridge_swamp_stepping_stone", "Stepping stone (Lumbridge Swamp)", "jump across the stepping stone", 66, 0,
                    3208, 3135, 3214, 3135),
            s("shilo_stepping_stones", "Stepping stones (Shilo Village)", "hop across the stepping stones", 74, 0, 2925, 2947,
                    2925, 2952),
            s("lava_dragon_isle_stepping_stones", "Stepping stones (Lava Dragon Isle)", "hop across the stepping stones", 74, 0,
                    3205, 3808, 3211, 3806),
            s("elven_overpass_advanced", "Cliffside (Elven overpass, advanced)", "climb the cliffside", 85, 0, 2340, 3260, 2336, 3254),
            // God Wars Dungeon entrance: Strength 60 moves the boulder (or 60 Agility squeezes through the crack beside it).
            s("gwd_boulder", "Boulder (God Wars Dungeon)", "move the boulder", 0, 0, 2898, 3715, 2898, 3719, Map.of(Skill.STRENGTH, 60)),
            s("gwd_little_crack", "Little crack (God Wars Dungeon)", "squeeze through the crack", 60, 0, 2900, 3713, 2904, 3720),
            // Grapples across the River Lum (Lumbridge to Al Kharid: 8 Agility, 37 Ranged, 19 Strength), over Falador's
            // north wall (11/19/37) and Yanille's south wall (39/21/38). End tiles approx.
            g("lumbridge_river_grapple", "Grapple (River Lum to Al Kharid)", 8, 3246, 3179, 3259, 3179, 19, 37),
            g("falador_wall_grapple", "Grapple (Falador north wall)", 11, 3037, 3398, 3037, 3391, 37, 19),
            g("yanille_wall_grapple", "Grapple (Yanille south wall)", 39, 2556, 3068, 2556, 3073, 38, 21));

    /**
     * Shortcuts inside dungeons and buildings (wiki levels; none gives XP). The Kalphite Lair's
     * entrance is climbed down on a rope tied to the rocks (no level needed). Which way the Slayer
     * Tower chains run is approx. (61: ground to first floor, 71: first to second floor), as is the
     * Fremennik Slayer Dungeon's strange floor level (81).
     */
    public static final List<Shortcut> INDOOR = List.of(
            in("taverley_dungeon_pipe", "Obstacle pipe (Taverley Dungeon blue dragons)", "squeeze through the pipe", 70),
            in("edgeville_dungeon_monkeybars", "Monkeybars (Edgeville Dungeon)", "swing across the monkeybars", 15),
            in("brimhaven_stepping_stones", "Stepping stones (Brimhaven Dungeon)", "hop across the stepping stones", 12),
            in("brimhaven_pipe_moss_giants", "Pipe (Brimhaven Dungeon moss giants)", "squeeze through the pipe", 22),
            in("brimhaven_log_balance", "Log balance (Brimhaven Dungeon)", "walk across the log", 30),
            in("brimhaven_pipe_dragons", "Pipe (Brimhaven Dungeon dragons)", "squeeze through the pipe", 34),
            new Shortcut("kalphite_lair_rope", "Rope (Kalphite Lair entrance)", "climb down the rope", 0, 0, 0, 0, 0, 0, Map.of(), ROPE),
            in("slayer_tower_chain_low", "Spiked chain (Slayer Tower, first floor)", "climb the spiked chain", 61),
            in("slayer_tower_chain_high", "Spiked chain (Slayer Tower, second floor)", "climb the spiked chain", 71),
            in("fremennik_slayer_crevice", "Narrow crevice (Fremennik Slayer Dungeon)", "squeeze through the crevice", 62),
            in("fremennik_slayer_strange_floor", "Strange floor (Fremennik Slayer Dungeon)", "jump over the strange floor", 81),
            in("stronghold_slayer_tunnel", "Tunnel (Stronghold Slayer Cave)", "crawl through the tunnel", 72),
            in("asgarnian_ice_tunnel", "Tunnel (Asgarnian Ice Dungeon)", "crawl through the tunnel", 72));

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
