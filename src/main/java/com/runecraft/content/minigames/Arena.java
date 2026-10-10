package com.runecraft.content.minigames;

import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Instanced arenas. Each minigame owns a row of instance slots far from the main map
 * (x = -2,000,000 + 320 * slot, z = -2,000,000 + 320 * minigame) at y = 180, sealed in a box so
 * the terrain below never matters. Arenas are rebuilt from scratch every time a session starts.
 */
public final class Arena {
    public static final int BASE = -2_000_000;
    public static final int SPACING = 320;
    public static final int FLOOR_Y = 180;

    private Arena() {
    }

    public static BlockPos origin(Minigame game, int slot) {
        return new BlockPos(BASE + SPACING * slot, FLOOR_Y, BASE + SPACING * game.ordinal());
    }

    /** True if a block position lies in the instance area. */
    public static boolean inInstanceArea(BlockPos pos) {
        return pos.getX() < BASE + 100_000 && pos.getZ() < BASE + 100_000;
    }

    public static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    /** A module block's default state (falls back to a lodestone if the id is unknown). */
    public static BlockState module(String id) {
        Block block = ModBlocks.MODULE_BLOCKS.get(id);
        return block == null ? Blocks.LODESTONE.getDefaultState() : block.getDefaultState();
    }

    /**
     * Builds a sealed rectangular room centred on {@code c}: floor at c.y, walls and ceiling of
     * {@code wall}, a lit ceiling grid and empty interior {@code height} blocks tall.
     */
    public static void room(ServerWorld world, BlockPos c, int halfX, int halfZ, int height, BlockState floor, BlockState wall,
                            BlockState accent) {
        for (int dx = -halfX - 1; dx <= halfX + 1; dx++) {
            for (int dz = -halfZ - 1; dz <= halfZ + 1; dz++) {
                boolean edge = Math.abs(dx) > halfX || Math.abs(dz) > halfZ;
                BlockPos p = c.add(dx, 0, dz);
                set(world, p.down(), wall);
                set(world, p, edge ? wall : ((dx * 7 + dz * 3) % 11 == 0 ? accent : floor));
                for (int y = 1; y <= height; y++) {
                    set(world, p.up(y), edge ? wall : Blocks.AIR.getDefaultState());
                }
                boolean light = !edge && Math.floorMod(dx, 6) == 0 && Math.floorMod(dz, 6) == 0;
                set(world, p.up(height + 1), light ? Blocks.SHROOMLIGHT.getDefaultState() : wall);
            }
        }
    }

    /** Clears a box of the given half sizes (used before rebuilding an instance). */
    public static void clear(ServerWorld world, BlockPos c, int halfX, int halfZ, int height) {
        for (int dx = -halfX - 1; dx <= halfX + 1; dx++) {
            for (int dz = -halfZ - 1; dz <= halfZ + 1; dz++) {
                for (int y = -1; y <= height + 1; y++) {
                    BlockPos p = c.add(dx, y, dz);
                    if (!world.getBlockState(p).isAir()) {
                        set(world, p, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    /** A solid pillar/wall segment from c.y+1 up to c.y+h. */
    public static void column(ServerWorld world, BlockPos c, int h, BlockState state) {
        for (int y = 1; y <= h; y++) {
            set(world, c.up(y), state);
        }
    }

    /** Fills a horizontal rectangle at a given y offset. */
    public static void fill(ServerWorld world, BlockPos c, int x1, int z1, int x2, int z2, int dy, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(world, c.add(x, dy, z), state);
            }
        }
    }
}
