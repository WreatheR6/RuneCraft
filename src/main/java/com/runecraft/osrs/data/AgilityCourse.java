package com.runecraft.osrs.data;

import com.runecraft.osrs.Landmark;

/**
 * Agility courses with their real obstacle lists. Each obstacle gives its own OSRS XP and the last
 * one includes the lap-completion XP, so a lap gives the course's current OSRS lap XP (after the
 * May 2024 rooftop rework: Falador 586, Ardougne 889). The listed value of the last obstacle is only
 * informative: it is recomputed as lap XP minus the other obstacles, so where the rework raised a
 * course's lap XP the difference lands on the final obstacle/lap bonus (Al Kharid, Varrock, Falador,
 * Ardougne; approx. where the per-obstacle split after the rework isn't known). Obstacle counts
 * match the real courses (Varrock 9, Falador 13, Pollnivneach 9...).
 *
 * <p>Rooftop courses spawn Marks of grace: a 1/3 chance per lap (Canifis 2/3), cut to 20% once the
 * player is 20+ levels above the course (not at Canifis), and at most one every 3 minutes.
 */
public enum AgilityCourse {
    // Log balance, obstacle net, tree branch, balancing rope, tree branch, obstacle net, obstacle pipe (+39 lap).
    GNOME_STRONGHOLD("Gnome Stronghold Agility Course", 1, 86.5, Landmark.GNOME_STRONGHOLD, false,
            7.5, 7.5, 5, 7.5, 5, 7.5, 46.5),
    // Rough wall (approx.), tightrope, tightrope, narrow wall, wall, gap, crate.
    DRAYNOR("Draynor Village Rooftop Course", 10, 120.0, Landmark.DRAYNOR_VILLAGE, true,
            5, 8, 7, 7, 10, 4, 79),
    // Rough wall, tightrope, cable, zip line, tropical tree, roof top beams, tightrope, gap (last: lap remainder).
    AL_KHARID("Al Kharid Rooftop Course", 20, 216.0, Landmark.AL_KHARID, true,
            10, 30, 40, 40, 10, 5, 15, 30),
    // Rough wall, clothes line, gap, wall, gap, gap, gap, ledge, edge (last: lap remainder).
    VARROCK("Varrock Rooftop Course", 30, 270.0, Landmark.VARROCK, true,
            12, 21, 17, 25, 9, 22, 4, 3, 125),
    // Tall tree, gap x4, pole-vault, gap, gap (last: lap remainder).
    CANIFIS("Canifis Rooftop Course", 40, 240.0, Landmark.CANIFIS, true,
            10, 8, 10, 11, 9, 22, 15, 175),
    // Rough wall, tightrope, hand holds, gap, gap, tightrope, tightrope, gap, ledge x4, edge (last: lap remainder).
    FALADOR("Falador Rooftop Course", 50, 586.0, Landmark.FALADOR, true,
            8, 17, 27, 10, 10, 10, 10, 25, 10, 10, 10, 10, 180),
    // Wall, gap, tightrope, gap, gap, edge.
    SEERS("Seers' Village Rooftop Course", 60, 570.0, Landmark.SEERS_VILLAGE, true,
            45, 20, 20, 35, 15, 435),
    // Basket, market stall, banner, gap, tree, rough wall, monkeybars, tree, drying line.
    POLLNIVNEACH("Pollnivneach Rooftop Course", 70, 890.0, Landmark.POLLNIVNEACH, true,
            10, 40, 50, 50, 50, 50, 50, 50, 540),
    // Rough wall, gap, tightrope, gap, gap, tightrope, pile of fish.
    RELLEKKA("Rellekka Rooftop Course", 80, 780.0, Landmark.RELLEKKA, true,
            20, 30, 40, 85, 25, 105, 475),
    // Wooden beams, gap, plank, gap, gap, steep roof, gap (last: lap remainder).
    ARDOUGNE("Ardougne Rooftop Course", 90, 889.0, Landmark.ARDOUGNE, true,
            43, 65, 50, 21, 28, 57, 529);

    /** Obstacle steps the obstacle block can represent (its {@code step} property is 0..MAX_OBSTACLES-1). */
    public static final int MAX_OBSTACLES = 16;
    /** Shortest time between two Marks of grace, in game ticks (3 minutes). */
    public static final int MARK_COOLDOWN_TICKS = 300;

    private final String name;
    private final int level;
    private final double lapXp;
    private final Landmark landmark;
    private final boolean rooftop;
    private final double[] obstacleXp;

    AgilityCourse(String name, int level, double lapXp, Landmark landmark, boolean rooftop, double... weights) {
        this.name = name;
        this.level = level;
        this.lapXp = lapXp;
        this.landmark = landmark;
        this.rooftop = rooftop;
        this.obstacleXp = new double[weights.length];
        double given = 0;
        for (int i = 0; i < weights.length - 1; i++) {
            obstacleXp[i] = weights[i];
            given += weights[i];
        }
        obstacleXp[weights.length - 1] = Math.round((lapXp - given) * 10) / 10.0;
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
        return obstacleXp.length;
    }

    public Landmark landmark() {
        return landmark;
    }

    public boolean rooftop() {
        return rooftop;
    }

    /** XP for crossing obstacle {@code step} (0-based); the last one includes the lap bonus. */
    public double obstacleXp(int step) {
        return obstacleXp[Math.max(0, Math.min(obstacleXp.length - 1, step))];
    }

    /** XP awarded in one go on finishing a lap (the last obstacle's XP, lap bonus included). */
    public double lapBonusXp() {
        return obstacleXp[obstacleXp.length - 1];
    }

    /**
     * Chance that finishing a lap spawns a Mark of grace (0 on non-rooftop courses): 1/3, or 2/3 at
     * Canifis; a fifth of that once the player is 20 or more levels above the course (not Canifis).
     */
    public double markChance(int agilityLevel) {
        if (!rooftop) {
            return 0;
        }
        if (this == CANIFIS) {
            return 2.0 / 3.0;
        }
        double chance = 1.0 / 3.0;
        return agilityLevel >= level + 20 ? chance * 0.2 : chance;
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
