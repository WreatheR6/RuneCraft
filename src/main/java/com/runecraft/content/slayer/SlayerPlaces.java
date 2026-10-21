package com.runecraft.content.slayer;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.slayer.SlayerContent;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Slayer places built in the world: the ship west of the Pirates' Hideout carrying Larran's big chest. */
public final class SlayerPlaces {
    private static final int HALF_LENGTH = 9;
    private static final int HALF_WIDTH = 3;

    private SlayerPlaces() {
    }

    public static void register() {
        LandmarkBuilder.registerCustom(SlayerContent.LARRAN_SHIP, (world, centre) -> larransShip(world));
    }

    /** Half-width of the hull at {@code dz} blocks from midships: the bow and stern taper. */
    private static int halfWidth(int dz) {
        int a = Math.abs(dz);
        return a >= HALF_LENGTH ? 1 : a >= HALF_LENGTH - 2 ? 2 : HALF_WIDTH;
    }

    /**
     * A small pirate ship lying north-south with Larran's big chest on its deck (OSRS Wiki: the chest is on the ship's
     * deck, plane 1, at 3018, 3955), a mast and a sail, and a gangplank on the side facing the hideout. approx: the
     * terrain has no water there, so the ship lies beached.
     */
    private static void larransShip(ServerLevel world) {
        int cx = GielinorMap.toBlockX(SlayerContent.LARRAN_CHEST_TILE[0]);
        int cz = GielinorMap.toBlockZ(SlayerContent.LARRAN_CHEST_TILE[1]);
        int y0 = Integer.MIN_VALUE;
        for (int dz = -HALF_LENGTH; dz <= HALF_LENGTH; dz++) {
            for (int dx = -halfWidth(dz); dx <= halfWidth(dz); dx++) {
                world.getChunk((cx + dx) >> 4, (cz + dz) >> 4);
                y0 = Math.max(y0, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz) - 1);
            }
        }
        BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState deck = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState rail = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int dz = -HALF_LENGTH; dz <= HALF_LENGTH; dz++) {
            int w = halfWidth(dz);
            for (int dx = -w; dx <= w; dx++) {
                int x = cx + dx;
                int z = cz + dz;
                int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int y = Math.min(ground, y0) - 1; y <= y0; y++) {
                    world.setBlock(new BlockPos(x, y, z), hull, 2);
                }
                boolean edge = Math.abs(dx) == w || Math.abs(dz) == HALF_LENGTH;
                world.setBlock(new BlockPos(x, y0 + 1, z), edge ? hull : deck, 2);
                for (int y = y0 + 2; y <= y0 + 10; y++) {
                    world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                if (edge) {
                    world.setBlock(new BlockPos(x, y0 + 2, z), rail, 3);
                }
            }
        }
        // The mast and its sail fore of the chest.
        for (int y = y0 + 2; y <= y0 + 10; y++) {
            world.setBlock(new BlockPos(cx, y, cz - 4), Blocks.SPRUCE_LOG.defaultBlockState(), 2);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int y = y0 + 5; y <= y0 + 9; y++) {
                if (dx != 0) {
                    world.setBlock(new BlockPos(cx + dx, y, cz - 4), Blocks.WHITE_WOOL.defaultBlockState(), 2);
                }
            }
        }
        world.setBlock(new BlockPos(cx, y0 + 11, cz - 4), Blocks.BLACK_WOOL.defaultBlockState(), 2);
        // The gangplank on the east side, towards the hideout.
        world.setBlock(new BlockPos(cx + HALF_WIDTH, y0 + 2, cz), Blocks.AIR.defaultBlockState(), 3);
        int outside = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + HALF_WIDTH + 1, cz);
        for (int y = outside; y <= y0; y++) {
            world.setBlock(new BlockPos(cx + HALF_WIDTH + 1, y, cz), hull, 2);
        }
        world.setBlock(new BlockPos(cx + HALF_WIDTH + 1, y0 + 1, cz),
                Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,
                        net.minecraft.core.Direction.WEST), 3);
        // Larran's big chest amidships, barrels of loot beside it.
        Block chest = BuiltInRegistries.BLOCK.get(RuneCraft.resolve("larrans_big_chest"));
        if (chest != Blocks.AIR) {
            world.setBlock(new BlockPos(cx, y0 + 2, cz), chest.defaultBlockState(), 3);
        }
        world.setBlock(new BlockPos(cx - 2, y0 + 2, cz + 2), Blocks.BARREL.defaultBlockState(), 2);
        world.setBlock(new BlockPos(cx - 2, y0 + 2, cz + 3), Blocks.BARREL.defaultBlockState(), 2);
    }
}
