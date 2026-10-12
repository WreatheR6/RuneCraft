package com.runecraft.block;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.skill.FarmingService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
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
 *
 * <p>Disease (see {@link FarmingDisease}): every growth step may make the crop diseased
 * ({@code disease} 1) instead of growing, with the herb chance of 27/128 cut by the patch's
 * {@code compost} (0-3). A diseased crop dies ({@code disease} 2) at its next growth step unless it
 * is treated with plant cure. A crop the gardener was paid to look after ({@code protected}) never
 * gets diseased.
 */
public abstract class FarmingPatchBlock extends Block {
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);
    /** 0 healthy, 1 diseased, 2 dead. */
    public static final IntProperty DISEASE = IntProperty.of("disease", 0, 2);
    /** 0 none, 1 compost, 2 supercompost, 3 ultracompost. */
    public static final IntProperty COMPOST = IntProperty.of("compost", 0, 3);
    public static final BooleanProperty PROTECTED = BooleanProperty.of("protected");

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
        return state.get(cropProperty()) > 0 && stage >= 1 && stage < 4 && state.get(DISEASE) < 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!hasRandomTicks(state) || random.nextInt(growthOdds()) != 0) {
            return;
        }
        if (state.get(DISEASE) == 1) {
            // Left untreated for a whole growth cycle: the crop dies.
            world.setBlockState(pos, state.with(DISEASE, 2), Block.NOTIFY_ALL);
            return;
        }
        int chance = FarmingDisease.chance(FarmingDisease.BASE_CHANCE, state.get(COMPOST));
        if (FarmingDisease.rollDisease(chance, state.get(PROTECTED), random::nextInt)) {
            world.setBlockState(pos, state.with(DISEASE, 1), Block.NOTIFY_ALL);
            return;
        }
        world.setBlockState(pos, state.with(STAGE, state.get(STAGE) + 1), Block.NOTIFY_ALL);
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
