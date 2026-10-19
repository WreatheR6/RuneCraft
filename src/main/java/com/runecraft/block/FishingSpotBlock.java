package com.runecraft.block;

import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.skill.FishingSpots;
import com.runecraft.skill.Gathering;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A rippling fishing spot sitting on the water surface. It moves now and then, like OSRS spots. */
public class FishingSpotBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape OUTLINE = Block.box(0, 12, 0, 16, 15, 16);

    private final FishingSpot spot;

    public FishingSpotBlock(FishingSpot spot, Properties settings) {
        super(settings);
        this.spot = spot;
        registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true));
    }

    public FishingSpot spot() {
        return spot;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Spots wander around their fishing area on OSRS-like timers, even while nobody fishes them. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        FishingSpots.tick(world, pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            Gathering.fish(sp, pos, spot);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
