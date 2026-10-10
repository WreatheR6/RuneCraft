package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;

/**
 * One OSRS monster spawn location: up to {@code count} of {@code monster} are kept alive within
 * {@code radius} OSRS tiles of tile ({@code x}, {@code y}); a killed one comes back after
 * {@code respawnTicks} game ticks (0.6 s each), as in OSRS.
 *
 * <p>{@code monster} is a RuneCraft creature id (built-in {@code Monster} or module {@code MonsterDef})
 * or a vanilla {@code minecraft:} mob that has OSRS stats in Gielinor. {@code floorY} is
 * {@link #SURFACE} for points on the open ground, otherwise the absolute block y the monsters stand
 * at (a dungeon floor, or an upper storey of a building such as the Slayer Tower).
 * {@code island} marks points outside the mapped kingdoms (islands, Zeah).
 */
public record SpawnPoint(String monster, int x, int y, int floorY, int count, int radius, int respawnTicks,
                         boolean island) {
    public static final int SURFACE = Integer.MIN_VALUE;

    public boolean surface() {
        return floorY == SURFACE;
    }

    public int blockX() {
        return GielinorMap.toBlockX(x);
    }

    public int blockZ() {
        return GielinorMap.toBlockZ(y);
    }

    public int radiusBlocks() {
        return radius * GielinorMap.SCALE;
    }

    /** Stable identity, stored on every mob this point spawns (survives reloads and table reordering). */
    public String key() {
        return monster + "@" + x + "," + y + (surface() ? "" : "," + floorY);
    }
}
