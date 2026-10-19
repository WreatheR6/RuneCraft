package com.runecraft.block;

import com.runecraft.osrs.data.Thieving;
import com.runecraft.skill.ThievingService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A market stall that can be stolen from, then restocks after a delay. */
public class StallBlock extends Block {
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");

    private final Thieving.Stall stall;

    public StallBlock(Thieving.Stall stall, Properties settings) {
        super(settings);
        this.stall = stall;
        registerDefaultState(this.stateDefinition.any().setValue(EMPTY, false));
    }

    public Thieving.Stall stall() {
        return stall;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EMPTY);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp && world instanceof ServerLevel sw) {
            ThievingService.stealFromStall(sp, sw, pos, state, this);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    public void empty(ServerLevel world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(EMPTY, true), Block.UPDATE_ALL);
        world.scheduleTick(pos, this, stall.respawnTicks() * ResourceBlock.MC_TICKS_PER_GAME_TICK);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(EMPTY)) {
            world.setBlock(pos, state.setValue(EMPTY, false), Block.UPDATE_ALL);
        }
    }
}
