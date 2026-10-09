package com.runecraft.block;

import com.runecraft.skill.FarmingService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A farming patch. {@code crop} 0 is empty, otherwise the 1-based index of the crop; {@code stage}
 * goes 1 (planted) to 4 (fully grown). Herbs take roughly 80 minutes, allotments 40 minutes,
 * matching OSRS growth times.
 */
public abstract class FarmingPatchBlock extends Block {
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);

    protected FarmingPatchBlock(Settings settings) {
        super(settings);
    }

    public abstract IntProperty cropProperty();

    public abstract boolean isHerbPatch();

    /** Chance per random tick (~68s on average) to advance one stage. */
    protected abstract int growthOdds();

    @Override
    public boolean hasRandomTicks(BlockState state) {
        int stage = state.get(STAGE);
        return state.get(cropProperty()) > 0 && stage >= 1 && stage < 4;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (hasRandomTicks(state) && random.nextInt(growthOdds()) == 0) {
            world.setBlockState(pos, state.with(STAGE, state.get(STAGE) + 1), Block.NOTIFY_ALL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            FarmingService.use(sp, (ServerWorld) world, pos, state, this);
        }
        return ActionResult.success(world.isClient());
    }
}
