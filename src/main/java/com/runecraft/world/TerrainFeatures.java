package com.runecraft.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * Small decorations that belong on the natural terrain rather than on levelled structure ground,
 * built by the landmark builder on the real surface: e.g. the Oracle's standing stones on the
 * summit of Ice Mountain (the mountain itself is terrain from {@link com.runecraft.osrs.world.Elevation}).
 */
public final class TerrainFeatures {
    private TerrainFeatures() {
    }

    public static void register() {
        LandmarkBuilder.registerCustom("ice_mountain", TerrainFeatures::oracleStones);
    }

    /** A ring of six snow-capped standing stones around the Oracle on Ice Mountain's summit. */
    private static void oracleStones(ServerWorld world, BlockPos centre) {
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            int x = centre.getX() + (int) Math.round(4 * Math.cos(a));
            int z = centre.getZ() + (int) Math.round(4 * Math.sin(a));
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos.Mutable pos = new BlockPos.Mutable(x, y - 1, z);
            for (int dy = 0; dy < 3; dy++) {
                set(world, pos.setY(y - 1 + dy), Blocks.STONE_BRICKS.getDefaultState());
            }
            set(world, pos.setY(y + 2), Blocks.SNOW.getDefaultState());
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }
}
