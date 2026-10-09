package com.runecraft.osrs.data;

/**
 * Woodcutting trees. Level, XP, firemaking data and depletion are from OSRS; low/high are the
 * bronze-axe success values out of 256 (better axes multiply them, see {@link ToolTier}).
 * Respawn is in game ticks (0.6s).
 */
public enum Tree {
    TREE("tree", "Tree", "logs", "Logs", 1, 25.0, 64, 200, 1.0, 60, 1, 40.0, 0x6B4A2A, 4),
    OAK("oak", "Oak tree", "oak_logs", "Oak logs", 15, 37.5, 32, 100, 0.125, 14, 15, 60.0, 0x8A6A3A, 20),
    WILLOW("willow", "Willow tree", "willow_logs", "Willow logs", 30, 67.5, 16, 50, 0.125, 14, 30, 90.0, 0x6E7A3A, 16),
    TEAK("teak", "Teak tree", "teak_logs", "Teak logs", 35, 85.0, 15, 46, 0.125, 15, 35, 105.0, 0x9C7A4A, 30),
    MAPLE("maple", "Maple tree", "maple_logs", "Maple logs", 45, 100.0, 8, 25, 0.125, 59, 45, 135.0, 0xA0502A, 40),
    MAHOGANY("mahogany", "Mahogany tree", "mahogany_logs", "Mahogany logs", 50, 125.0, 8, 25, 0.125, 14, 50, 157.5, 0x6A2A1A, 90),
    YEW("yew", "Yew tree", "yew_logs", "Yew logs", 60, 175.0, 4, 12, 0.125, 99, 60, 202.5, 0x5A4A2A, 160),
    MAGIC("magic", "Magic tree", "magic_logs", "Magic logs", 75, 250.0, 2, 6, 0.125, 199, 75, 303.8, 0x3A8AA8, 1000),
    REDWOOD("redwood", "Redwood tree", "redwood_logs", "Redwood logs", 90, 380.0, 2, 6, 0.125, 199, 90, 350.0, 0xA03A2A, 600);

    private final String id;
    private final String name;
    private final String log;
    private final String logName;
    private final int level;
    private final double xp;
    private final int low;
    private final int high;
    private final double depleteChance;
    private final int respawnTicks;
    private final int firemakingLevel;
    private final double firemakingXp;
    private final int color;
    private final int logValue;

    Tree(String id, String name, String log, String logName, int level, double xp, int low, int high,
         double depleteChance, int respawnTicks, int firemakingLevel, double firemakingXp, int color, int logValue) {
        this.id = id;
        this.name = name;
        this.log = log;
        this.logName = logName;
        this.level = level;
        this.xp = xp;
        this.low = low;
        this.high = high;
        this.depleteChance = depleteChance;
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

    public double depleteChance() {
        return depleteChance;
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
