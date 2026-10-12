package com.runecraft.osrs.data;

/**
 * Woodcutting trees. Level, XP, firemaking data and depletion are from OSRS; low/high are the
 * bronze-axe success values out of 256 (better axes multiply them, see {@link ToolTier}).
 * Respawn is in game ticks (0.6s).
 *
 * <p>Depletion follows the Forestry (2023) rules: a normal tree falls after one log
 * ({@code despawnTicks == 0}); every other tree has a timer that counts down one per game tick while
 * anyone is chopping it and regenerates while nobody is. Once it reaches zero the tree falls on the
 * next log (oak 27s, willow/teak 30s, maple/mahogany 60s, yew 114s, magic 234s, redwood 264s).
 * See {@link com.runecraft.osrs.TreeTimers}.
 */
public enum Tree {
    TREE("tree", "Tree", "logs", "Logs", 1, 25.0, 64, 200, 0, 60, 1, 40.0, 0x6B4A2A, 4),
    OAK("oak", "Oak tree", "oak_logs", "Oak logs", 15, 37.5, 32, 100, 45, 14, 15, 60.0, 0x8A6A3A, 20),
    WILLOW("willow", "Willow tree", "willow_logs", "Willow logs", 30, 67.5, 16, 50, 50, 14, 30, 90.0, 0x6E7A3A, 16),
    TEAK("teak", "Teak tree", "teak_logs", "Teak logs", 35, 85.0, 15, 46, 50, 15, 35, 105.0, 0x9C7A4A, 30),
    MAPLE("maple", "Maple tree", "maple_logs", "Maple logs", 45, 100.0, 8, 25, 100, 59, 45, 135.0, 0xA0502A, 40),
    MAHOGANY("mahogany", "Mahogany tree", "mahogany_logs", "Mahogany logs", 50, 125.0, 8, 25, 100, 14, 50, 157.5, 0x6A2A1A, 90),
    YEW("yew", "Yew tree", "yew_logs", "Yew logs", 60, 175.0, 4, 12, 190, 99, 60, 202.5, 0x5A4A2A, 160),
    MAGIC("magic", "Magic tree", "magic_logs", "Magic logs", 75, 250.0, 2, 6, 390, 199, 75, 303.8, 0x3A8AA8, 1000),
    REDWOOD("redwood", "Redwood tree", "redwood_logs", "Redwood logs", 90, 380.0, 2, 6, 440, 199, 90, 350.0, 0xA03A2A, 600);

    private final String id;
    private final String name;
    private final String log;
    private final String logName;
    private final int level;
    private final double xp;
    private final int low;
    private final int high;
    private final int despawnTicks;
    private final int respawnTicks;
    private final int firemakingLevel;
    private final double firemakingXp;
    private final int color;
    private final int logValue;

    Tree(String id, String name, String log, String logName, int level, double xp, int low, int high,
         int despawnTicks, int respawnTicks, int firemakingLevel, double firemakingXp, int color, int logValue) {
        this.id = id;
        this.name = name;
        this.log = log;
        this.logName = logName;
        this.level = level;
        this.xp = xp;
        this.low = low;
        this.high = high;
        this.despawnTicks = despawnTicks;
        this.respawnTicks = respawnTicks;
        this.firemakingLevel = firemakingLevel;
        this.firemakingXp = firemakingXp;
        this.color = color;
        this.logValue = logValue;
    }

    /** Block id for this tree's trunk. */
    public String blockId() {
        return id + "_tree_log";
    }

    public String leavesId() {
        return id + "_tree_leaves";
    }

    public String displayName() {
        return name;
    }

    public String logItem() {
        return log;
    }

    public String logName() {
        return logName;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    /** Multipliers of the bronze-to-rune axes, matching {@link #EXACT}'s columns. */
    private static final double[] AXE_MULTIPLIERS = {1.0, 1.5, 2.0, 2.25, 2.5, 3.0, 3.5};

    /**
     * Exact per-axe low/high values (bronze, iron, steel, black, mithril, adamant, rune) of the trees
     * that have them in the game's woodcutting data; they are not always a clean multiple of the
     * bronze values (e.g. a yew with an iron axe is 6/19, not 6/18).
     */
    private static final int[][][] EXACT = {
            {{64, 200}, {96, 300}, {128, 400}, {144, 450}, {160, 500}, {192, 600}, {224, 700}},
            {{32, 100}, {48, 150}, {64, 200}, {72, 225}, {80, 250}, {96, 300}, {112, 350}},
            {{16, 50}, {24, 75}, {32, 100}, {36, 112}, {40, 125}, {48, 150}, {56, 175}},
            {{8, 25}, {12, 37}, {16, 50}, {18, 56}, {20, 62}, {24, 75}, {28, 87}},
            {{4, 12}, {6, 19}, {8, 25}, {9, 28}, {10, 31}, {12, 37}, {14, 44}},
            {{2, 6}, {3, 9}, {4, 12}, {5, 13}, {5, 15}, {6, 18}, {7, 21}}};

    private int[] exact(double axeMultiplier) {
        int row = switch (this) {
            case TREE -> 0;
            case OAK -> 1;
            case WILLOW -> 2;
            case MAPLE -> 3;
            case YEW -> 4;
            case MAGIC -> 5;
            default -> -1;
        };
        if (row < 0) {
            return null;
        }
        for (int i = 0; i < AXE_MULTIPLIERS.length; i++) {
            if (Math.abs(AXE_MULTIPLIERS[i] - axeMultiplier) < 1e-9) {
                return EXACT[row][i];
            }
        }
        return null;
    }

    /** Success low value for an axe with this woodcutting multiplier (exact where the game data has one). */
    public int low(double axeMultiplier) {
        int[] e = exact(axeMultiplier);
        return e != null ? e[0] : (int) Math.floor(low * axeMultiplier);
    }

    /** Success high value for an axe with this woodcutting multiplier (exact where the game data has one). */
    public int high(double axeMultiplier) {
        int[] e = exact(axeMultiplier);
        return e != null ? e[1] : (int) Math.floor(high * axeMultiplier);
    }

    /** Forestry tree timer in game ticks; 0 means the tree falls after a single log. */
    public int despawnTicks() {
        return despawnTicks;
    }

    public int respawnTicks() {
        return respawnTicks;
    }

    public int firemakingLevel() {
        return firemakingLevel;
    }

    public double firemakingXp() {
        return firemakingXp;
    }

    public int color() {
        return color;
    }

    public int logValue() {
        return logValue;
    }

    public String id() {
        return id;
    }

    public static Tree byLog(String itemId) {
        for (Tree t : values()) {
            if (t.log.equals(itemId)) {
                return t;
            }
        }
        return null;
    }
}
