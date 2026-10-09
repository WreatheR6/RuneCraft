package com.runecraft.block;

import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.skill.Gathering;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** A rippling fishing spot sitting on the water surface. */
public class FishingSpotBlock extends Block implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape OUTLINE = Block.createCuboidShape(0, 12, 0, 16, 15, 16);

    private final FishingSpot spot;

    public FishingSpotBlock(FishingSpot spot, Settings settings) {
        super(settings);
        this.spot = spot;
        setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, true));
    }

    public FishingSpot spot() {
        return spot;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
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
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY() + 0.95,
                    pos.getZ() + random.nextDouble(), 0, 0.05, 0);
        }
        if (random.nextInt(5) == 0) {
            world.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + random.nextDouble(), pos.getY() + 0.9,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            Gathering.fish(sp, pos, spot);
        }
        return ActionResult.success(world.isClient());
    }
}
