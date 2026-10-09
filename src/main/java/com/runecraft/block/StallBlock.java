package com.runecraft.block;

import com.runecraft.osrs.data.Thieving;
import com.runecraft.skill.ThievingService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** A market stall that can be stolen from, then restocks after a delay. */
public class StallBlock extends Block {
    public static final BooleanProperty EMPTY = BooleanProperty.of("empty");

    private final Thieving.Stall stall;

    public StallBlock(Thieving.Stall stall, Settings settings) {
        super(settings);
        this.stall = stall;
        setDefaultState(this.stateManager.getDefaultState().with(EMPTY, false));
    }

    public Thieving.Stall stall() {
        return stall;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(EMPTY);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
            ThievingService.stealFromStall(sp, sw, pos, state, this);
        }
        return ActionResult.success(world.isClient());
    }

    public void empty(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(EMPTY, true), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, stall.respawnTicks() * ResourceBlock.MC_TICKS_PER_GAME_TICK);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(EMPTY)) {
            world.setBlockState(pos, state.with(EMPTY, false), Block.NOTIFY_ALL);
        }
    }
}
