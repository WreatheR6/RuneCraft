package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.FishingSpotTimers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.function.IntUnaryOperator;

/**
 * Moves fishing spots around their fishing area on OSRS-like random timers ({@link FishingSpotTimers}).
 * A spot only moves to open still water next to the shore, within a few blocks of where it started.
 * Used by both the built-in fishing spots and the skilling module's spots.
 */
public final class FishingSpots {
    public static final FishingSpotTimers TIMERS = new FishingSpotTimers();

    private FishingSpots() {
    }

    /**
     * Checks the spot at {@code pos}; if its timer has run out it moves. Returns the new position, or
     * null if it stayed where it is.
     */
    public static BlockPos tick(ServerWorld world, BlockPos pos, BlockState state) {
        Random r = world.getRandom();
        IntUnaryOperator random = n -> r.nextInt(n);
        long key = pos.asLong();
        long now = ActionManager.gameTick();
        if (!TIMERS.due(key, pos.getX(), pos.getY(), pos.getZ(), now, random)) {
            return null;
        }
        int[] dest = TIMERS.destination(key, pos.getX(), pos.getY(), pos.getZ(), (x, y, z) -> validTile(world, new BlockPos(x, y, z)),
                random);
        if (dest == null || !nearWater(world, pos)) {
            TIMERS.moved(key, key, pos.getX(), pos.getY(), pos.getZ(), now, random);
            return null;
        }
        BlockPos to = new BlockPos(dest[0], dest[1], dest[2]);
        BlockState moved = state.contains(Properties.WATERLOGGED) ? state.with(Properties.WATERLOGGED, true) : state;
        boolean waterlogged = state.contains(Properties.WATERLOGGED);
        world.setBlockState(to, moved, Block.NOTIFY_ALL);
        world.setBlockState(pos, waterlogged || nearWater(world, pos) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(),
                Block.NOTIFY_ALL);
        TIMERS.moved(key, to.asLong(), to.getX(), to.getY(), to.getZ(), now, random);
        world.spawnParticles(ParticleTypes.SPLASH, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 12, 0.4, 0.1, 0.4, 0.05);
        world.playSound(null, to, SoundEvents.ENTITY_FISH_SWIM, SoundCategory.BLOCKS, 0.6f, 1.0f);
        return to;
    }

    /** Still water with air above and land (a non-water block) on at least one side. */
    static boolean validTile(ServerWorld world, BlockPos p) {
        if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
            return false;
        }
        BlockState s = world.getBlockState(p);
        if (!s.isOf(Blocks.WATER) || !s.getFluidState().isStill() || !world.getBlockState(p.up()).isAir()) {
            return false;
        }
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockState n = world.getBlockState(p.offset(d));
            if (n.getFluidState().isEmpty() && !n.isAir()) {
                return true;
            }
        }
        return false;
    }

    /** True if the spot sits in water (so it leaves water behind when it moves). */
    private static boolean nearWater(ServerWorld world, BlockPos p) {
        if (!world.getBlockState(p.down()).getFluidState().isEmpty()) {
            return true;
        }
        for (Direction d : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(p.offset(d)).isOf(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }
}
