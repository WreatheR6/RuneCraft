package com.runecraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A gatherable node (tree, rock) that depletes and respawns after a number of game ticks. */
public abstract class ResourceBlock extends Block {
    public static final BooleanProperty DEPLETED = BooleanProperty.create("depleted");
    /** One OSRS game tick is 0.6 seconds = 12 Minecraft ticks. */
    public static final int MC_TICKS_PER_GAME_TICK = 12;

    protected ResourceBlock(Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(DEPLETED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPLETED);
    }

    public boolean isDepleted(BlockState state) {
        return state.getValue(DEPLETED);
    }

    /** Replaces the node with its depleted form and schedules the respawn. */
    public void deplete(ServerLevel world, BlockPos pos, BlockState state, int respawnGameTicks) {
        world.setBlock(pos, state.setValue(DEPLETED, true), Block.UPDATE_ALL);
        world.scheduleTick(pos, this, Math.max(1, respawnGameTicks) * MC_TICKS_PER_GAME_TICK);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(DEPLETED)) {
            world.setBlock(pos, state.setValue(DEPLETED, false), Block.UPDATE_ALL);
        }
    }
}
