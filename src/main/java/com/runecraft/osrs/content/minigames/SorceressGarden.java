package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.ItemDef.Kind;

import java.util.List;

/**
 * Sorceress's Garden (Al Kharid, after Prince Ali Rescue): sneak past the seasonal elementals to
 * pick a sq'irk or two herbs, crush sq'irks into sq'irkjuice and trade the juice to Osman for
 * Thieving XP. All numbers are from the OSRS Wiki ("Sorceress's Garden", the four season pages,
 * "Sq'irkjuice" and Mod Ash's herb chances).
 *
 * <ul>
 *     <li>Picking a sq'irk gives Farming XP only (30/40/50/60); Thieving XP comes from Osman.</li>
 *     <li>Crushing sq'irks into a beer glass gives 5 Cooking XP whatever the season.</li>
 *     <li>Herbs: always two clean herbs and 50 Farming XP (25 per herb) per trip.</li>
 *     <li>Elementals only see a player two or fewer squares away.</li>
 * </ul>
 */
public final class SorceressGarden {
    private SorceressGarden() {
    }

    /** OSRS coordinates of the garden itself (an instance) and of the sorceress's house north-east of the Shantay Pass. */
    public static final int GARDEN_X = 2912;
    public static final int GARDEN_Y = 5472;
    public static final int HOUSE_X = 3320;
    public static final int HOUSE_Y = 3139;

    /** Prince Ali Rescue unlocks the minigame (Osman tells you about the sq'irks). */
    public static final String QUEST = "prince_ali_rescue";
    /** Cooking XP for crushing one glass of sq'irkjuice. */
    public static final double JUICE_COOKING_XP = 5.0;
    /** Farming XP for one trip of herbs (two herbs). */
    public static final double HERB_FARMING_XP = 50.0;
    public static final int HERBS_PER_PICK = 2;
    /** Elementals see (and teleport away) a player this many squares away or closer. */
    public static final int SIGHT = 2;
    /** Rocky's base chance per sq'irk picked: 1/(10,339 - 25 * Thieving level) (pet roll, Mod Kieren). */
    public static final int ROCKY_BASE = 10_339;

    /**
     * The four gardens: Thieving level to enter, Farming XP per sq'irk, sq'irks per glass, Thieving
     * boost and run energy (%) from drinking the juice, and Thieving XP from Osman per glass.
     * Ordered by level (winter west, spring east, autumn south, summer north of the fountain).
     */
    public enum Season {
        WINTER("winter", "Winter", 1, 30.0, 5, 0, 5, 350, 0x9AC8E8, 0xC8E0F0),
        SPRING("spring", "Spring", 25, 40.0, 4, 1, 10, 1350, 0x6AC050, 0x9AE070),
        AUTUMN("autumn", "Autumn", 45, 50.0, 3, 2, 15, 2350, 0xC07020, 0xE0A040),
        SUMMER("summer", "Summer", 65, 60.0, 2, 3, 20, 3000, 0xE04040, 0xF0A030);

        private final String id;
        private final String displayName;
        private final int level;
        private final double farmingXp;
        private final int perGlass;
        private final int boost;
        private final int energy;
        private final int osmanXp;
        private final int fruitColor;
        private final int juiceColor;

        Season(String id, String displayName, int level, double farmingXp, int perGlass, int boost, int energy, int osmanXp,
               int fruitColor, int juiceColor) {
            this.id = id;
            this.displayName = displayName;
            this.level = level;
            this.farmingXp = farmingXp;
            this.perGlass = perGlass;
            this.boost = boost;
            this.energy = energy;
            this.osmanXp = osmanXp;
            this.fruitColor = fruitColor;
            this.juiceColor = juiceColor;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        /** Thieving level needed to open the garden's gate (boosts count). */
        public int level() {
            return level;
        }

        /** Farming XP for picking one sq'irk. */
        public double farmingXp() {
            return farmingXp;
        }

        /** Sq'irks crushed into one glass of juice. */
        public int perGlass() {
            return perGlass;
        }

        /** Temporary Thieving boost when the juice is drunk. */
        public int boost() {
            return boost;
        }

        /** Run energy restored (percent) when the juice is drunk. */
        public int energy() {
            return energy;
        }

        /** Thieving XP Osman gives for one glass. */
        public int osmanXp() {
            return osmanXp;
        }

        public String fruit() {
            return id + "_sqirk";
        }

        public String juice() {
            return id + "_sqirkjuice";
        }

        public String gate() {
            return "sg_gate_" + id;
        }

        public String tree() {
            return "sg_sqirk_tree_" + id;
        }

        public String herbs() {
            return "sg_herbs_" + id;
        }

        public String elemental() {
            return id + "_elemental";
        }

        /** Herbs picked in this garden (Mod Ash's chances, out of the table's total weight). */
        public List<MinigameRules.Loot> herbTable() {
            return HERBS.get(ordinal());
        }

        public int fruitColor() {
            return fruitColor;
        }

        public int juiceColor() {
            return juiceColor;
        }

        public static Season byFruit(String item) {
            for (Season s : values()) {
                if (s.fruit().equals(item)) {
                    return s;
                }
            }
            return null;
        }

        public static Season byJuice(String item) {
            for (Season s : values()) {
                if (s.juice().equals(item)) {
                    return s;
                }
            }
            return null;
        }
    }

    private static MinigameRules.Loot h(String herb, int weight) {
        return new MinigameRules.Loot(herb, 1, 1, weight);
    }

    /** Herb tables per season (Winter /54, Spring /75, Autumn /99, Summer /111). */
    private static final List<List<MinigameRules.Loot>> HERBS = List.of(
            List.of(h("guam_leaf", 16), h("marrentill", 14), h("tarromin", 13), h("harralander", 11)),
            List.of(h("guam_leaf", 16), h("marrentill", 14), h("tarromin", 13), h("harralander", 12), h("irit_leaf", 11),
                    h("avantoe", 9)),
            List.of(h("guam_leaf", 16), h("marrentill", 14), h("tarromin", 13), h("harralander", 12), h("irit_leaf", 11),
                    h("avantoe", 10), h("ranarr_weed", 9), h("kwuarm", 8), h("cadantine", 6)),
            List.of(h("guam_leaf", 16), h("marrentill", 14), h("tarromin", 13), h("harralander", 12), h("irit_leaf", 11),
                    h("avantoe", 10), h("ranarr_weed", 9), h("kwuarm", 8), h("cadantine", 7), h("lantadyme", 6),
                    h("dwarf_weed", 5)));

    /** Chance of Rocky per sq'irk picked at a Thieving level. */
    public static double rockyChance(int thievingLevel) {
        return 1.0 / (ROCKY_BASE - 25 * Math.max(1, Math.min(99, thievingLevel)));
    }

    /** Whether an elemental at (ex, ez) sees a player at (px, pz) (squares; Chebyshev distance). */
    public static boolean sees(int ex, int ez, int px, int pz) {
        return Math.max(Math.abs(ex - px), Math.abs(ez - pz)) <= SIGHT;
    }

    /**
     * A patrol: the elemental walks back and forth in a straight line between garden squares
     * (u1, v1) and (u2, v2) - u counts squares from the gate, v squares across the corridor -
     * {@code speed} squares per game tick, starting {@code phase} ticks into its loop.
     */
    public record Patrol(int u1, int v1, int u2, int v2, int speed, int phase) {
        private int offset(long ticks) {
            int len = Math.max(Math.abs(u2 - u1), Math.abs(v2 - v1));
            if (len == 0) {
                return 0;
            }
            long loop = 2L * len;
            long step = Math.floorMod((ticks + phase) * speed, loop);
            return (int) (step <= len ? step : loop - step);
        }

        /** Square along the garden after {@code ticks} game ticks (a ping-pong walk). */
        public int u(long ticks) {
            return u1 + Integer.signum(u2 - u1) * offset(ticks);
        }

        /** Square across the garden after {@code ticks} game ticks. */
        public int v(long ticks) {
            return v1 + Integer.signum(v2 - v1) * offset(ticks);
        }

        /** Whether this elemental sees square (u, v) at a game tick. */
        public boolean sees(long ticks, int u, int v) {
            return SorceressGarden.sees(u(ticks), v(ticks), u, v);
        }
    }

    /** A crossing: an elemental walking across the corridor at square {@code u}, out to {@code reach} on either side. */
    private static Patrol across(int u, int reach, int speed, int phase) {
        return new Patrol(u, -reach, u, reach, speed, phase);
    }

    /** The same crossing walked the other way (two elementals meeting in the corridor). */
    private static Patrol back(int u, int reach, int speed, int phase) {
        return new Patrol(u, reach, u, -reach, speed, phase);
    }

    /** An elemental walking up and down the corridor between squares {@code from} and {@code to}. */
    private static Patrol along(int from, int to, int speed, int phase) {
        return new Patrol(from, 0, to, 0, speed, phase);
    }

    /** Squares from the gate to the end of each garden's corridor; the clearing with the tree and herbs follows. */
    public static final int CORRIDOR = 34;
    /** Half the corridor's width (it is 3 squares wide) and the clearing's half width. */
    public static final int CORRIDOR_HALF_WIDTH = 1;
    public static final int CLEARING_HALF_WIDTH = 4;
    /** The sq'irk tree at the far end of the clearing, and the herb patch beside it. */
    public static final int TREE = CORRIDOR + 3;

    /**
     * Alcoves (safe spots two and three squares off the corridor) per season: {square, side}. The real
     * gardens are hedge mazes; RuneCraft lays each out as one corridor that elementals cross or walk
     * along, with alcoves to hide in (approx: the number of elementals follows the wiki's maps, the
     * layout does not reproduce the tiles). SorceressGardenTest checks every garden can be crossed.
     */
    public static int[][] alcoves(Season s) {
        return switch (s) {
            case WINTER -> new int[][]{{7, 1}, {14, -1}, {21, 1}, {27, -1}, {33, 1}};
            case SPRING -> new int[][]{{7, 1}, {13, -1}, {20, 1}, {27, -1}, {33, 1}};
            case AUTUMN -> new int[][]{{6, -1}, {12, 1}, {19, -1}, {26, 1}, {32, -1}};
            case SUMMER -> new int[][]{{6, 1}, {12, -1}, {19, 1}, {26, -1}, {32, 1}};
        };
    }

    /** The elementals' patrols per season: 4 in winter, 5 in spring, 6 in autumn and summer (wiki maps). */
    public static List<Patrol> patrols(Season s) {
        return switch (s) {
            case WINTER -> List.of(across(4, 5, 1, 0), along(10, 18, 1, 3), across(24, 5, 1, 4), across(30, 5, 1, 1));
            case SPRING -> List.of(across(4, 5, 1, 0), across(10, 5, 1, 3), along(16, 24, 1, 2), across(30, 5, 1, 5),
                    back(30, 5, 1, 2));
            case AUTUMN -> List.of(across(3, 6, 1, 0), across(9, 6, 1, 4), back(9, 6, 1, 1), along(15, 23, 1, 6),
                    across(29, 6, 1, 2), back(29, 6, 1, 7));
            case SUMMER -> List.of(across(3, 6, 2, 0), across(9, 6, 1, 2), back(9, 6, 1, 6), along(15, 23, 1, 1),
                    across(29, 6, 2, 5), back(29, 6, 2, 3));
        };
    }

    /**
     * Whether any elemental of a garden sees square (u, v) at a game tick: within its sight and with no hedge on the straight
     * line between them (OSRS: elementals can't see through the hedges).
     */
    public static boolean seen(Season s, long ticks, int u, int v) {
        for (Patrol p : patrols(s)) {
            if (p.sees(ticks, u, v) && clearLine(s, p.u(ticks), p.v(ticks), u, v)) {
                return true;
            }
        }
        return false;
    }

    /** Whether no hedge square lies on the straight line between two squares (Bresenham; the end squares don't block). */
    public static boolean clearLine(Season s, int u1, int v1, int u2, int v2) {
        int du = Math.abs(u2 - u1);
        int dv = Math.abs(v2 - v1);
        int su = Integer.signum(u2 - u1);
        int sv = Integer.signum(v2 - v1);
        int err = du - dv;
        int u = u1;
        int v = v1;
        while (u != u2 || v != v2) {
            int e2 = 2 * err;
            if (e2 > -dv) {
                err -= dv;
                u += su;
            }
            if (e2 < du) {
                err += du;
                v += sv;
            }
            if ((u != u2 || v != v2) && !walkable(s, u, v)) {
                return false;
            }
        }
        return true;
    }

    /** Whether garden square (u, v) is open (corridor, alcove, an elemental's path or the clearing); the rest is hedge. */
    public static boolean walkable(Season s, int u, int v) {
        if (u >= 1 && u <= CORRIDOR && Math.abs(v) <= CORRIDOR_HALF_WIDTH) {
            return true;
        }
        if (u > CORRIDOR && u <= TREE && Math.abs(v) <= CLEARING_HALF_WIDTH) {
            return true;
        }
        for (int[] a : alcoves(s)) {
            if (a[0] == u && (v == 2 * a[1] || v == 3 * a[1])) {
                return true;
            }
        }
        for (Patrol p : patrols(s)) {
            boolean onU = Math.min(p.u1(), p.u2()) <= u && u <= Math.max(p.u1(), p.u2());
            boolean onV = Math.min(p.v1(), p.v2()) <= v && v <= Math.max(p.v1(), p.v2());
            if (onU && onV) {
                return true;
            }
        }
        return false;
    }

    static void items(ItemSink items) {
        for (Season s : Season.values()) {
            items.add(s.fruit(), s.displayName() + " sq'irk", Kind.SIMPLE, "produce", s.fruitColor()).value(1).stack(1)
                    .examine("A slightly sq'irky, I mean quirky, fruit.");
            items.add(s.juice(), s.displayName() + " sq'irkjuice", Kind.SIMPLE, "beer", s.juiceColor()).value(1).stack(1)
                    .examine("Made from " + s.perGlass() + " " + s.id + " sq'irks.");
        }
    }
}
