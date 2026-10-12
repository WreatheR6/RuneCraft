package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Skill;

import java.util.List;
import java.util.Map;

/**
 * Surface Agility shortcuts at their OSRS spots. A shortcut block stands at each end (placed on the
 * surface when the chunk first loads, never over a structure block); using it with the level
 * requirement carries the player to the other end. Requirements are the wiki's; most shortcuts give
 * no experience (XP given only where the wiki lists some). End tiles are approx. to a tile or two.
 *
 * <p>Not modelled: shortcuts inside dungeons (Taverley pipe, Edgeville monkeybars, Brimhaven Dungeon,
 * Kalphite Lair), whose caves here are not laid out tile-for-tile, and grapple shortcuts' mithril
 * grapple and crossbow (their Ranged and Strength levels are required instead).
 */
public final class AgilityShortcuts {
    public static final String BLOCK = "agility_shortcut";

    /** One shortcut between end {@code a} and end {@code b} (OSRS tiles); {@code other} = extra skill levels. */
    public record Shortcut(String id, String name, String verb, int level, double xp, int ax, int ay, int bx, int by,
                           Map<Skill, Integer> other) {
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
        return new Shortcut(id, name, verb, level, xp, ax, ay, bx, by, Map.of());
    }

    private static Shortcut s(String id, String name, String verb, int level, double xp, int ax, int ay, int bx, int by,
                              Map<Skill, Integer> other) {
        return new Shortcut(id, name, verb, level, xp, ax, ay, bx, by, other);
    }

    public static final List<Shortcut> SHORTCUTS = List.of(
            s("falador_crumbling_wall", "Crumbling wall (Falador)", "climb over the crumbling wall", 5, 0, 2938, 3358, 2933, 3355),
            s("varrock_south_fence", "Fence (south of Varrock)", "jump over the fence", 13, 0, 3240, 3336, 3240, 3333),
            s("yanille_underwall_tunnel", "Underwall tunnel (Yanille)", "squeeze through the tunnel", 16, 0, 2575, 3112, 2571, 3103),
            s("coal_truck_log_balance", "Log balance (Coal Trucks)", "walk across the log", 20, 0, 2603, 3477, 2597, 3477),
            s("grand_exchange_tunnel", "Underwall tunnel (Grand Exchange)", "climb into the tunnel", 21, 0, 3144, 3514, 3137, 3517),
            s("draynor_manor_railing", "Broken railing (Draynor Manor)", "squeeze through the railing", 28, 0, 3083, 3353, 3086, 3353),
            s("ardougne_log_balance", "Log balance (East Ardougne river)", "walk across the log", 33, 0, 2598, 3332, 2598, 3337),
            s("catherby_obelisk_grapple", "Grapple (Water obelisk island to Catherby)", "grapple across", 36, 0, 2841, 3426, 2841, 3433,
                    Map.of(Skill.STRENGTH, 22, Skill.RANGED, 39)),
            s("grand_tree_ottos_grotto", "Rocks (Grand Tree to Otto's Grotto)", "climb the rocks", 37, 0, 2486, 3514, 2489, 3521),
            s("al_kharid_mining_pit", "Cliffside (Al Kharid mine)", "scramble up the cliffside", 38, 0, 3305, 3315, 3302, 3315),
            s("sinclair_log_balance", "Log balance (Sinclair Mansion)", "walk across the log", 48, 0, 2722, 3592, 2722, 3597),
            s("karamja_volcano_grapple", "Grapple (south of the Karamja volcano)", "grapple across", 53, 0, 2869, 3125, 2874, 3128,
                    Map.of(Skill.STRENGTH, 21, Skill.RANGED, 42)),
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
            s("gwd_little_crack", "Little crack (God Wars Dungeon)", "squeeze through the crack", 60, 0, 2900, 3713, 2904, 3720));

    private AgilityShortcuts() {
    }

    /** The shortcut with an end within a tile of (x, y), or null. */
    public static Shortcut at(int x, int y) {
        for (Shortcut s : SHORTCUTS) {
            if (s.endAt(x, y) >= 0) {
                return s;
            }
        }
        return null;
    }
}
