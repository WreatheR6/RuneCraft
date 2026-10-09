package com.runecraft.block;

import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.skill.HunterService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** A laid bird snare or box trap. Periodically rolls to catch a creature for the nearest hunter. */
public class TrapBlock extends Block {
    public static final BooleanProperty CAUGHT = BooleanProperty.of("caught");
    private static final VoxelShape OUTLINE = Block.createCuboidShape(3, 0, 3, 13, 7, 13);

    private final HunterCreature.Trap trap;

    public TrapBlock(HunterCreature.Trap trap, Settings settings) {
        super(settings);
        this.trap = trap;
        setDefaultState(this.stateManager.getDefaultState().with(CAUGHT, false));
    }

    public HunterCreature.Trap trap() {
        return trap;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CAUGHT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient() && !state.get(CAUGHT)) {
            world.scheduleBlockTick(pos, this, HunterService.CHECK_INTERVAL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        HunterService.checkTrap(world, pos, state, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            HunterService.collect(sp, (ServerWorld) world, pos, state, this);
        }
        return ActionResult.success(world.isClient());
    }
}
