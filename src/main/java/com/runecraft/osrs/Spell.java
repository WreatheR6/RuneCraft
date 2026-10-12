package com.runecraft.osrs;

import java.util.EnumMap;
import java.util.Map;

/** The standard spellbook spells that are implemented. Levels, XP, runes and max hits are from OSRS. */
public enum Spell {
    WIND_STRIKE("Wind Strike", 1, 5.5, Kind.COMBAT, 2, cost(Rune.AIR, 1, Rune.MIND, 1)),
    WATER_STRIKE("Water Strike", 5, 7.5, Kind.COMBAT, 4, cost(Rune.WATER, 1, Rune.AIR, 1, Rune.MIND, 1)),
    EARTH_STRIKE("Earth Strike", 9, 9.5, Kind.COMBAT, 6, cost(Rune.EARTH, 2, Rune.AIR, 1, Rune.MIND, 1)),
    FIRE_STRIKE("Fire Strike", 13, 11.5, Kind.COMBAT, 8, cost(Rune.FIRE, 3, Rune.AIR, 2, Rune.MIND, 1)),
    WIND_BOLT("Wind Bolt", 17, 13.5, Kind.COMBAT, 9, cost(Rune.AIR, 2, Rune.CHAOS, 1)),
    WATER_BOLT("Water Bolt", 23, 16.5, Kind.COMBAT, 10, cost(Rune.WATER, 2, Rune.AIR, 2, Rune.CHAOS, 1)),
    EARTH_BOLT("Earth Bolt", 29, 19.5, Kind.COMBAT, 11, cost(Rune.EARTH, 3, Rune.AIR, 2, Rune.CHAOS, 1)),
    FIRE_BOLT("Fire Bolt", 35, 22.5, Kind.COMBAT, 12, cost(Rune.FIRE, 4, Rune.AIR, 3, Rune.CHAOS, 1)),
    WIND_BLAST("Wind Blast", 41, 25.5, Kind.COMBAT, 13, cost(Rune.AIR, 3, Rune.DEATH, 1)),
    WATER_BLAST("Water Blast", 47, 28.5, Kind.COMBAT, 14, cost(Rune.WATER, 3, Rune.AIR, 3, Rune.DEATH, 1)),
    EARTH_BLAST("Earth Blast", 53, 31.5, Kind.COMBAT, 15, cost(Rune.EARTH, 4, Rune.AIR, 3, Rune.DEATH, 1)),
    FIRE_BLAST("Fire Blast", 59, 34.5, Kind.COMBAT, 16, cost(Rune.FIRE, 5, Rune.AIR, 4, Rune.DEATH, 1)),
    WIND_WAVE("Wind Wave", 62, 36.0, Kind.COMBAT, 17, cost(Rune.AIR, 5, Rune.BLOOD, 1)),
    WATER_WAVE("Water Wave", 65, 37.5, Kind.COMBAT, 18, cost(Rune.WATER, 7, Rune.AIR, 5, Rune.BLOOD, 1)),
    EARTH_WAVE("Earth Wave", 70, 40.0, Kind.COMBAT, 19, cost(Rune.EARTH, 7, Rune.AIR, 5, Rune.BLOOD, 1)),
    FIRE_WAVE("Fire Wave", 75, 42.5, Kind.COMBAT, 20, cost(Rune.FIRE, 7, Rune.AIR, 5, Rune.BLOOD, 1)),
    WIND_SURGE("Wind Surge", 81, 44.5, Kind.COMBAT, 21, cost(Rune.AIR, 7, Rune.WRATH, 1)),
    WATER_SURGE("Water Surge", 85, 46.5, Kind.COMBAT, 22, cost(Rune.WATER, 10, Rune.AIR, 7, Rune.WRATH, 1)),
    EARTH_SURGE("Earth Surge", 90, 48.5, Kind.COMBAT, 23, cost(Rune.EARTH, 10, Rune.AIR, 7, Rune.WRATH, 1)),
    FIRE_SURGE("Fire Surge", 95, 50.5, Kind.COMBAT, 24, cost(Rune.FIRE, 10, Rune.AIR, 7, Rune.WRATH, 1)),

    HOME_TELEPORT("Lumbridge Home Teleport", 1, 0, Kind.TELEPORT, 0, cost()),
    VARROCK_TELEPORT("Varrock Teleport", 25, 35.0, Kind.TELEPORT, 0, cost(Rune.FIRE, 1, Rune.AIR, 3, Rune.LAW, 1)),
    LUMBRIDGE_TELEPORT("Lumbridge Teleport", 31, 41.0, Kind.TELEPORT, 0, cost(Rune.EARTH, 1, Rune.AIR, 3, Rune.LAW, 1)),
    FALADOR_TELEPORT("Falador Teleport", 37, 48.0, Kind.TELEPORT, 0, cost(Rune.WATER, 1, Rune.AIR, 3, Rune.LAW, 1)),
    CAMELOT_TELEPORT("Camelot Teleport", 45, 55.5, Kind.TELEPORT, 0, cost(Rune.AIR, 5, Rune.LAW, 1)),
    ARDOUGNE_TELEPORT("Ardougne Teleport", 51, 61.0, Kind.TELEPORT, 0, cost(Rune.WATER, 2, Rune.LAW, 2)),
    WATCHTOWER_TELEPORT("Watchtower Teleport", 58, 68.0, Kind.TELEPORT, 0, cost(Rune.EARTH, 2, Rune.LAW, 2)),
    TROLLHEIM_TELEPORT("Trollheim Teleport", 61, 68.0, Kind.TELEPORT, 0, cost(Rune.FIRE, 2, Rune.LAW, 2)),

    LOW_LEVEL_ALCHEMY("Low Level Alchemy", 21, 31.0, Kind.ALCHEMY, 0, cost(Rune.FIRE, 3, Rune.NATURE, 1)),
    HIGH_LEVEL_ALCHEMY("High Level Alchemy", 55, 65.0, Kind.ALCHEMY, 0, cost(Rune.FIRE, 5, Rune.NATURE, 1)),
    SUPERHEAT_ITEM("Superheat Item", 43, 53.0, Kind.UTILITY, 0, cost(Rune.FIRE, 4, Rune.NATURE, 1));

    public enum Kind { COMBAT, TELEPORT, ALCHEMY, UTILITY }

    private final String displayName;
    private final int level;
    private final int xpTenths;
    private final Kind kind;
    private final int maxHit;
    private final Map<Rune, Integer> runes;

    Spell(String displayName, int level, double xp, Kind kind, int maxHit, Map<Rune, Integer> runes) {
        this.displayName = displayName;
        this.level = level;
        this.xpTenths = Experience.tenths(xp);
        this.kind = kind;
        this.maxHit = maxHit;
        this.runes = runes;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }

    /** Base cast experience in tenths (combat spells also give 2 XP per damage dealt). */
    public int xpTenths() {
        return xpTenths;
    }

    public Kind kind() {
        return kind;
    }

    public int maxHit() {
        return maxHit;
    }

    public Map<Rune, Integer> runes() {
        return runes;
    }

    /** Destination landmark for teleport spells. */
    public Landmark destination() {
        return switch (this) {
            case HOME_TELEPORT, LUMBRIDGE_TELEPORT -> Landmark.LUMBRIDGE;
            case VARROCK_TELEPORT -> Landmark.VARROCK;
            case FALADOR_TELEPORT -> Landmark.FALADOR;
            case CAMELOT_TELEPORT -> Landmark.CAMELOT;
            case ARDOUGNE_TELEPORT -> Landmark.ARDOUGNE;
            case WATCHTOWER_TELEPORT -> Landmark.YANILLE; // only with the hard Kandarin diary; see tile()
            case TROLLHEIM_TELEPORT -> Landmark.TROLLHEIM;
            default -> null;
        };
    }

    /**
     * An exact OSRS arrival tile {x, y} for teleports that don't land at a town landmark, or null to use
     * {@link #destination()}. Watchtower Teleport lands on top of the Watchtower north of Yanille.
     */
    public int[] tile() {
        return this == WATCHTOWER_TELEPORT ? new int[]{2549, 3112} : null;
    }

    private static Map<Rune, Integer> cost(Object... pairs) {
        Map<Rune, Integer> map = new EnumMap<>(Rune.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Rune) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
