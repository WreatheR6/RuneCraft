package com.runecraft.osrs.data;

import java.util.List;

/** Slayer masters, the combat level they require and the monsters they assign. */
public enum SlayerMaster {
    TURAEL("Turael", 3, 15, 50, List.of(Monster.GOBLIN, Monster.COW, Monster.CHICKEN, Monster.GIANT_RAT,
            Monster.ZOMBIE, Monster.SKELETON, Monster.GIANT_SPIDER, Monster.BARBARIAN, Monster.WOLF)),
    MAZCHNA("Mazchna", 20, 40, 70, List.of(Monster.HILL_GIANT, Monster.ZOMBIE, Monster.SKELETON, Monster.GHOUL,
            Monster.GIANT_SPIDER, Monster.WOLF, Monster.GUARD, Monster.DARK_WIZARD)),
    VANNAKA("Vannaka", 40, 40, 100, List.of(Monster.HILL_GIANT, Monster.MOSS_GIANT, Monster.ICE_GIANT, Monster.GHOUL,
            Monster.LESSER_DEMON)),
    CHAELDAR("Chaeldar", 70, 70, 130, List.of(Monster.LESSER_DEMON, Monster.GREATER_DEMON, Monster.ICE_GIANT,
            Monster.MOSS_GIANT)),
    DURADEL("Duradel", 100, 100, 180, List.of(Monster.GREATER_DEMON, Monster.BLACK_DEMON, Monster.LESSER_DEMON));

    private final String name;
    private final int combatRequirement;
    private final int minAmount;
    private final int maxAmount;
    private final List<Monster> assignments;

    SlayerMaster(String name, int combatRequirement, int minAmount, int maxAmount, List<Monster> assignments) {
        this.name = name;
        this.combatRequirement = combatRequirement;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.assignments = assignments;
    }

    public String displayName() {
        return name;
    }

    public int combatRequirement() {
        return combatRequirement;
    }

    public int minAmount() {
        return minAmount;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public List<Monster> assignments() {
        return assignments;
    }

    /** Highest master the player qualifies for. */
    public static SlayerMaster bestFor(int combatLevel) {
        SlayerMaster best = TURAEL;
        for (SlayerMaster m : values()) {
            if (combatLevel >= m.combatRequirement) {
                best = m;
            }
        }
        return best;
    }
}
