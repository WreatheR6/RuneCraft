package com.runecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** A gatherable node (tree, rock) that depletes and respawns after a number of game ticks. */
public abstract class ResourceBlock extends Block {
    public static final BooleanProperty DEPLETED = BooleanProperty.of("depleted");
    /** One OSRS game tick is 0.6 seconds = 12 Minecraft ticks. */
    public static final int MC_TICKS_PER_GAME_TICK = 12;

    protected ResourceBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(DEPLETED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DEPLETED);
    }

    public boolean isDepleted(BlockState state) {
        return state.get(DEPLETED);
    }

    /** Replaces the node with its depleted form and schedules the respawn. */
    public void deplete(ServerWorld world, BlockPos pos, BlockState state, int respawnGameTicks) {
        world.setBlockState(pos, state.with(DEPLETED, true), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, Math.max(1, respawnGameTicks) * MC_TICKS_PER_GAME_TICK);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(DEPLETED)) {
            world.setBlockState(pos, state.with(DEPLETED, false), Block.NOTIFY_ALL);
        }
    }
}
