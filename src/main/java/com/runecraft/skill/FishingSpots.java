package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.FishingSpotTimers;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
    public static BlockPos tick(ServerLevel world, BlockPos pos, BlockState state) {
        RandomSource r = world.getRandom();
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
        BlockState moved = state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, true) : state;
        boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED);
        world.setBlock(to, moved, Block.UPDATE_ALL);
        world.setBlock(pos, waterlogged || nearWater(world, pos) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL);
        TIMERS.moved(key, to.asLong(), to.getX(), to.getY(), to.getZ(), now, random);
        world.sendParticles(ParticleTypes.SPLASH, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 12, 0.4, 0.1, 0.4, 0.05);
        world.playSound(null, to, SoundEvents.FISH_SWIM, SoundSource.BLOCKS, 0.6f, 1.0f);
        return to;
    }

    /** Still water with air above and land (a non-water block) on at least one side. */
    static boolean validTile(ServerLevel world, BlockPos p) {
        if (!world.hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
            return false;
        }
        BlockState s = world.getBlockState(p);
        if (!s.is(Blocks.WATER) || !s.getFluidState().isSource() || !world.getBlockState(p.above()).isAir()) {
            return false;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState n = world.getBlockState(p.relative(d));
            if (n.getFluidState().isEmpty() && !n.isAir()) {
                return true;
            }
        }
        return false;
    }

    /** True if the spot sits in water (so it leaves water behind when it moves). */
    private static boolean nearWater(ServerLevel world, BlockPos p) {
        if (!world.getBlockState(p.below()).getFluidState().isEmpty()) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(p.relative(d)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }
}
