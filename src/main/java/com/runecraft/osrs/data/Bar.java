package com.runecraft.osrs.data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Smelting: ores to bars. Iron has a 50% success rate without a ring of forging. */
public enum Bar {
    BRONZE("bronze_bar", "Bronze bar", 1, 6.2, 1.0, 0xB08D57, 8, "copper_ore", 1, "tin_ore", 1),
    IRON("iron_bar", "Iron bar", 15, 12.5, 0.5, 0x8A8A8E, 28, "iron_ore", 1),
    SILVER("silver_bar", "Silver bar", 20, 13.7, 1.0, 0xD8D8E8, 150, "silver_ore", 1),
    STEEL("steel_bar", "Steel bar", 30, 17.5, 1.0, 0xB8B8C0, 100, "iron_ore", 1, "coal", 2),
    GOLD("gold_bar", "Gold bar", 40, 22.5, 1.0, 0xE8C030, 300, "gold_ore", 1),
    MITHRIL("mithril_bar", "Mithril bar", 50, 30.0, 1.0, 0x4A5A9C, 300, "mithril_ore", 1, "coal", 4),
    ADAMANTITE("adamantite_bar", "Adamantite bar", 70, 37.5, 1.0, 0x4C7A4C, 640, "adamantite_ore", 1, "coal", 6),
    RUNITE("runite_bar", "Runite bar", 85, 50.0, 1.0, 0x4FB8C8, 5000, "runite_ore", 1, "coal", 8);

    private final String id;
    private final String name;
    private final int level;
    private final double xp;
    private final double successChance;
    private final int color;
    private final int value;
    private final Map<String, Integer> ores = new LinkedHashMap<>();

    Bar(String id, String name, int level, double xp, double successChance, int color, int value, Object... ores) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.successChance = successChance;
        this.color = color;
        this.value = value;
        for (int i = 0; i < ores.length; i += 2) {
            this.ores.put((String) ores[i], (Integer) ores[i + 1]);
        }
    }

    public String itemId() {
        return id;
    }

    public String displayName() {
        return name;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public double successChance() {
        return successChance;
    }

    public int color() {
        return color;
    }

    public int value() {
        return value;
    }

    public Map<String, Integer> ores() {
        return ores;
    }
}
