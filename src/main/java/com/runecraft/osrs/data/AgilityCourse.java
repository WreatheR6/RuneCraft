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
 * <p>Lap XP matches the wiki/RuneLite calculator values (Gnome 110.5, Barbarian 152.5, Shayzien
 * 153.5/508, Wilderness 571.4, Colossal Wyrm 504.1/749.6, Prifddinas 1,337, Werewolf 350 plus 380 for
 * the stick = 730, Penguin 540). The Werewolf course (below Canifis: 60 Agility, Creature of
 * Fenkenstrain and a ring of charos) and the Penguin course (below the Iceberg: 30 Agility, Cold War
 * started) are carved into the dungeons module's caves. Not modelled: Dorgesh-Kaan, and the
 * Hallowed Sepulchre (a minigame).
 *
 * <p>Rooftop courses spawn Marks of grace: a 1/3 chance per lap (Canifis 2/3), cut to 20% once the
 * player is 20+ levels above the course (not at Canifis), and at most one every 3 minutes.
 */
public enum AgilityCourse {
    // Log balance, obstacle net, tree branch, balancing rope, tree branch, obstacle net, obstacle pipe (lap: 110.5 since
    // the May 2024 Agility rework; the difference lands on the final pipe/lap bonus). No failures.
    GNOME_STRONGHOLD("Gnome Stronghold Agility Course", 1, 110.5, Landmark.GNOME_STRONGHOLD, true, false,
            new int[0], 0, 7.5, 7.5, 5, 7.5, 5, 7.5, 70.5),
    // Rough wall, tightrope, tightrope, narrow wall, wall, gap, crate. Level 1 and no failures since 8 May 2024.
    DRAYNOR("Draynor Village Rooftop Course", 1, 120.0, Landmark.DRAYNOR_VILLAGE, false, true,
            new int[0], 0, 5, 8, 7, 7, 10, 4, 79),
    // Rough wall, tightrope, cable, zip line, tropical tree, roof top beams, tightrope, gap (last: lap remainder).
    AL_KHARID("Al Kharid Rooftop Course", 20, 216.0, Landmark.AL_KHARID, false, true,
            new int[]{1, 6}, 3, 10, 30, 40, 40, 10, 5, 15, 30),
    // Rough wall, clothes line, gap, wall, gap, gap, gap, ledge, edge (last: lap remainder).
    VARROCK("Varrock Rooftop Course", 30, 269.7, Landmark.VARROCK, false, true,
            new int[]{1, 4}, 4, 12, 21, 17, 25, 9, 22, 4, 3, 125),
    // Tall tree, gap x4, pole-vault, gap, gap (last: lap remainder).
    CANIFIS("Canifis Rooftop Course", 40, 240.0, Landmark.CANIFIS, false, true,
            new int[]{1, 2, 3, 4, 6}, 2, 10, 8, 10, 11, 9, 22, 15, 175),
    // Rough wall, tightrope, hand holds, gap, gap, tightrope, tightrope, gap, ledge x4, edge (last: lap remainder).
    FALADOR("Falador Rooftop Course", 50, 586.0, Landmark.FALADOR, false, true,
            new int[]{1, 2, 5, 6}, 5, 8, 17, 27, 10, 10, 10, 10, 25, 10, 10, 10, 10, 180),
    // Wall, gap, tightrope, gap, gap, edge.
    SEERS("Seers' Village Rooftop Course", 60, 570.0, Landmark.SEERS_VILLAGE, true, true,
            new int[]{2}, 5, 45, 20, 20, 35, 15, 435),
    // Basket, market stall, banner, gap, tree, rough wall, monkeybars, tree, drying line.
    POLLNIVNEACH("Pollnivneach Rooftop Course", 70, 890.0, Landmark.POLLNIVNEACH, false, true,
            new int[]{2, 6, 8}, 6, 10, 40, 50, 50, 50, 50, 50, 50, 540),
    // Rough wall, gap, tightrope, gap, gap, tightrope, pile of fish.
    RELLEKKA("Rellekka Rooftop Course", 80, 780.0, Landmark.RELLEKKA, true, true,
            new int[]{2, 5}, 6, 20, 30, 40, 85, 25, 105, 475),
    // Wooden beams, gap, plank, gap, gap, steep roof, gap (last: lap remainder).
    ARDOUGNE("Ardougne Rooftop Course", 90, 889.0, Landmark.ARDOUGNE, true, true,
            new int[]{2, 3, 4, 5}, 6, 43, 65, 50, 21, 28, 57, 529),
    // ---- courses added after the rooftops (append only: block states and saves use the ordinal) ----
    // Rope swing 22, log balance 13.7, obstacle net 8.2, balancing ledge 22, crumbling wall x3 (lap 152.5).
    BARBARIAN_OUTPOST("Barbarian Outpost Agility Course", 35, 152.5, null, false, false,
            new int[]{0, 1, 3}, 5, 22, 13.7, 8.2, 22, 13.7, 13.7, 59.2),
    // Obstacle pipe 12.5, rope swing 20, stepping stones 20, log balance 20, rocks (+498.9 lap) = 571.4.
    WILDERNESS("Wilderness Agility Course", 52, 571.4, null, false, false,
            new int[]{1, 2, 3}, 7, 12.5, 20, 20, 20, 498.9),
    // Shayzien basic course (lap 153.5; per-obstacle split approx).
    SHAYZIEN_BASIC("Shayzien Basic Agility Course", 1, 153.5, null, false, false,
            new int[0], 0, 15, 20, 20, 20, 20, 58.5),
    // Shayzien advanced course (lap 508; per-obstacle split approx).
    SHAYZIEN_ADVANCED("Shayzien Advanced Agility Course", 45, 508.0, null, false, false,
            new int[]{2, 4}, 4, 20, 40, 40, 40, 40, 40, 288),
    // Colossal Wyrm Remains basic course, Varlamore (lap 504.1; split approx).
    COLOSSAL_WYRM_BASIC("Colossal Wyrm Basic Agility Course", 50, 504.1, null, false, false,
            new int[]{1, 3}, 4, 30, 40, 40, 40, 40, 314.1),
    // Colossal Wyrm Remains advanced course (lap 749.6; split approx).
    COLOSSAL_WYRM_ADVANCED("Colossal Wyrm Advanced Agility Course", 62, 749.6, null, false, false,
            new int[]{1, 3, 4}, 5, 30, 50, 50, 50, 50, 50, 469.6),
    // Prifddinas: ladder, tightrope, chimney, roof edge, dark hole, ladder, rope bridge, tightrope, rope bridge,
    // tightrope, dark hole (lap 1,337; split approx).
    PRIFDDINAS("Prifddinas Agility Course", 75, 1337.0, null, false, false,
            new int[]{1, 7, 9}, 6, 10, 40, 40, 40, 40, 10, 50, 50, 50, 50, 957),
    // Werewolf course below Canifis: stepping stones 50, hurdles 3x20, pipe 15, skull slope 25, deathslide 200 (lap 350;
    // handing the Agility Trainer the stick at the end adds STICK_XP = 380, 730 a lap). Only the deathslide can be failed.
    WEREWOLF("Werewolf Agility Course", 60, 350.0, null, true, false,
            new int[]{6}, 6, 50, 20, 20, 20, 15, 25, 200),
    // Penguin course below the Iceberg: stepping stones 80, four icicles 40 each, ice slope 180 + lap bonus (lap 540;
    // the split of the bonus approx). Its obstacles can be failed at any level (approx.: here up to level 65).
    PENGUIN("Penguin Agility Course", 30, 540.0, null, true, false,
            new int[]{0, 1, 2, 3, 4}, 3, 80, 40, 40, 40, 40, 300);

    /** Obstacle steps the obstacle block can represent (its {@code step} property is 0..MAX_OBSTACLES-1). */
    public static final int MAX_OBSTACLES = 16;
    /** Bonus XP for handing the stick to the Werewolf course's Agility Trainer at the end of a lap. */
    public static final double STICK_XP = 380.0;
    /** Item the Werewolf course's Agility Trainer throws at the start of a lap and takes back at the end. */
    public static final String WEREWOLF_STICK = "werewolf_stick";

    /** Shortest time between two Marks of grace, in game ticks (3 minutes). */
    public static final int MARK_COOLDOWN_TICKS = 300;

    private final String name;
    private final int level;
    private final double lapXp;
    private final Landmark landmark;
    private final boolean rooftop;
    private final boolean handBuilt;
    private final int[] failSteps;
    private final int failDamage;
    private final double[] obstacleXp;

    AgilityCourse(String name, int level, double lapXp, Landmark landmark, boolean handBuilt, boolean rooftop, int[] failSteps,
                  int failDamage, double... weights) {
        this.name = name;
        this.level = level;
        this.lapXp = lapXp;
        this.landmark = landmark;
        this.handBuilt = handBuilt;
        this.rooftop = rooftop;
        this.failSteps = failSteps;
        this.failDamage = failDamage;
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

    /**
     * True for courses whose obstacles are laid out by hand somewhere other than {@link AgilityLayouts}
     * (the Gnome Stronghold, Seers', Rellekka and Ardougne structures); the others get their obstacle
     * blocks placed at the OSRS tiles listed in {@link AgilityLayouts}.
     */
    public boolean handBuilt() {
        return handBuilt;
    }

    /** Whether obstacle {@code step} can be failed (falling off: damage and the lap restarts). */
    public boolean canFail(int step) {
        for (int s : failSteps) {
            if (s == step) {
                return true;
            }
        }
        return false;
    }

    /** Most damage a failed obstacle deals (approx.; a hit of 1..this). */
    public int failDamage() {
        return failDamage;
    }

    /**
     * Chance of failing a failable obstacle: 15% at the course's level, falling linearly to nothing
     * 35 levels above it (capped at 99). Approx.: the wiki gives no exact per-obstacle formula.
     */
    public double failChance(int step, int agilityLevel) {
        if (!canFail(step)) {
            return 0;
        }
        int safe = Math.min(99, level + 35);
        if (agilityLevel >= safe) {
            return 0;
        }
        double t = (double) (agilityLevel - level) / Math.max(1, safe - level);
        return Math.max(0, 0.15 * (1 - t));
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
     * Chance that finishing a lap spawns a Mark of grace (0 on non-rooftop courses except Prifddinas):
     * 1/3, or 2/3 at Canifis; a fifth of that once the player is 20 or more levels above the course
     * (not Canifis). Prifddinas' rate is approx. the rooftops'.
     */
    public double markChance(int agilityLevel) {
        if (!rooftop && this != PRIFDDINAS) {
            return 0;
        }
        if (this == CANIFIS) {
            return 2.0 / 3.0;
        }
        double chance = 1.0 / 3.0;
        // Draynor kept its old level-10 threshold when the requirement was removed (approx.).
        int penaltyFrom = (this == DRAYNOR ? 10 : level) + 20;
        return agilityLevel >= penaltyFrom ? chance * 0.2 : chance;
    }

    /** Quest this course needs (complete, or only started - see {@link #questStartedEnough()}), or null. */
    public String requiredQuest() {
        return switch (this) {
            case WEREWOLF -> "creature_of_fenkenstrain";
            case PENGUIN -> "cold_war";
            default -> null;
        };
    }

    /** True if having started the required quest is enough (the Penguin course opens partway through Cold War). */
    public boolean questStartedEnough() {
        return this == PENGUIN;
    }

    /** Item the player must carry or wear to use the course (the Werewolf course's ring of charos), or null. */
    public String requiredItem() {
        return this == WEREWOLF ? "ring_of_charos" : null;
    }

    public static AgilityCourse at(Landmark landmark) {
        for (AgilityCourse c : values()) {
            if (landmark != null && c.landmark == landmark) {
                return c;
            }
        }
        return null;
    }
}
