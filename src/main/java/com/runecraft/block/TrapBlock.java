package com.runecraft.block;

import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.skill.HunterService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A laid bird snare or box trap. Periodically rolls to catch a creature for the nearest hunter. */
public class TrapBlock extends Block {
    public static final BooleanProperty CAUGHT = BooleanProperty.create("caught");
    private static final VoxelShape OUTLINE = Block.box(3, 0, 3, 13, 7, 13);

    private final HunterCreature.Trap trap;

    public TrapBlock(HunterCreature.Trap trap, Properties settings) {
        super(settings);
        this.trap = trap;
        registerDefaultState(this.stateDefinition.any().setValue(CAUGHT, false));
    }

    public HunterCreature.Trap trap() {
        return trap;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAUGHT);
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

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClientSide() && !state.getValue(CAUGHT)) {
            world.scheduleTick(pos, this, HunterService.CHECK_INTERVAL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        HunterService.checkTrap(world, pos, state, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            HunterService.collect(sp, (ServerLevel) world, pos, state, this);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
