package com.runecraft.osrs.data;

import com.runecraft.osrs.Landmark;

/**
 * Agility courses. Each obstacle gives an equal share of 80% of the lap XP and finishing the lap
 * gives the remaining 20%, approximating the per-obstacle split of the real courses. Lap XP is
 * from OSRS. Rooftop courses have a chance to drop a Mark of grace on lap completion.
 */
public enum AgilityCourse {
    GNOME_STRONGHOLD("Gnome Stronghold Agility Course", 1, 86.5, 7, Landmark.GNOME_STRONGHOLD, false),
    DRAYNOR("Draynor Village Rooftop Course", 10, 120.0, 7, Landmark.DRAYNOR_VILLAGE, true),
    AL_KHARID("Al Kharid Rooftop Course", 20, 216.0, 8, Landmark.AL_KHARID, true),
    VARROCK("Varrock Rooftop Course", 30, 270.0, 8, Landmark.VARROCK, true),
    CANIFIS("Canifis Rooftop Course", 40, 240.0, 8, Landmark.CANIFIS, true),
    FALADOR("Falador Rooftop Course", 50, 440.0, 8, Landmark.FALADOR, true),
    SEERS("Seers' Village Rooftop Course", 60, 570.0, 6, Landmark.SEERS_VILLAGE, true),
    POLLNIVNEACH("Pollnivneach Rooftop Course", 70, 890.0, 8, Landmark.POLLNIVNEACH, true),
    RELLEKKA("Rellekka Rooftop Course", 80, 780.0, 7, Landmark.RELLEKKA, true),
    ARDOUGNE("Ardougne Rooftop Course", 90, 793.0, 7, Landmark.ARDOUGNE, true);

    private final String name;
    private final int level;
    private final double lapXp;
    private final int obstacles;
    private final Landmark landmark;
    private final boolean rooftop;

    AgilityCourse(String name, int level, double lapXp, int obstacles, Landmark landmark, boolean rooftop) {
        this.name = name;
        this.level = level;
        this.lapXp = lapXp;
        this.obstacles = obstacles;
        this.landmark = landmark;
        this.rooftop = rooftop;
    }

    public String displayName() {
        return name;
    }

    public int level() {
        return level;
    }

    public double lapXp() {
        return lapXp;
    }

    public int obstacles() {
        return obstacles;
    }

    public Landmark landmark() {
        return landmark;
    }

    public boolean rooftop() {
        return rooftop;
    }

    public double obstacleXp() {
        return lapXp * 0.8 / obstacles;
    }

    public double lapBonusXp() {
        return lapXp * 0.2;
    }

    public static AgilityCourse at(Landmark landmark) {
        for (AgilityCourse c : values()) {
            if (c.landmark == landmark) {
                return c;
            }
        }
        return null;
    }
}
