package com.runecraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

/**
 * The static spawn point of an open-world monster (round 42), kept in the entity's persistent data so it survives reloads: set
 * when a world spawner places the monster ({@code finalizeSpawn} with SPAWNER or STRUCTURE, or {@code LocationSpawner} for vanilla
 * mobs). Monsters without one are encounter monsters (bosses, raids, minigames, quest and script spawns) and keep their own rules:
 * no leash, the old hunt range.
 */
public final class MonsterHome {
    private static final String KEY = "RcHome";

    private MonsterHome() {
    }

    public static BlockPos get(Mob mob) {
        return mob.getPersistentData().contains(KEY) ? BlockPos.of(mob.getPersistentData().getLong(KEY)) : null;
    }

    public static void set(Mob mob, BlockPos home) {
        if (home == null) {
            mob.getPersistentData().remove(KEY);
        } else {
            mob.getPersistentData().putLong(KEY, home.asLong());
        }
    }

    /** Records {@code home} unless the monster already has one. */
    public static void setIfAbsent(Mob mob, BlockPos home) {
        if (!mob.getPersistentData().contains(KEY)) {
            set(mob, home);
        }
    }

    /** Horizontal distance (blocks) from the monster's home to a point, or 0 without a home. */
    public static double fromHome(BlockPos home, double x, double z) {
        if (home == null) {
            return 0;
        }
        double dx = x - (home.getX() + 0.5);
        double dz = z - (home.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }
}
