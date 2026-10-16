package com.runecraft.osrs.content.systems;

/**
 * The looting bag's Wilderness drop (OSRS Wiki, "Looting bag"): any monster in the Wilderness may drop it, except
 * aviansies, abyssal demons and black chinchompas, and only while the player owns none. Level 10 or lower: 1/15; level
 * 50 or higher: 1/3; in between 1/(150/level).
 */
public final class LootingBag {
    public static final String ID = "looting_bag";
    /** Stacks the bag holds. */
    public static final int CAPACITY = 28;

    private LootingBag() {
    }

    /** Chance (0..1) that a Wilderness monster of this combat level drops the bag. */
    public static double chance(int combatLevel) {
        if (combatLevel <= 10) {
            return 1.0 / 15;
        }
        if (combatLevel >= 50) {
            return 1.0 / 3;
        }
        return combatLevel / 150.0;
    }

    /** Monsters that never drop it. */
    public static boolean excluded(String creatureId) {
        return creatureId.contains("aviansie") || creatureId.contains("abyssal_demon") || creatureId.contains("black_chinchompa");
    }
}
