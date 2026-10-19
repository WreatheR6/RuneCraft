package com.runecraft.block;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.skill.FarmingService;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A farming patch. {@code crop} 0 is empty, otherwise the 1-based index of the crop; {@code stage}
 * goes 1 (planted) to 4 (fully grown). Herbs take about 80 minutes and allotments 40 to 80 minutes
 * depending on the crop, matching OSRS growth times ({@link com.runecraft.osrs.FarmingYield#randomTickOdds}).
 *
 * <p>Disease (see {@link FarmingDisease}): every growth step may make the crop diseased
 * ({@code disease} 1) instead of growing, with the herb chance of 27/128 (allotments approx. the
 * same) cut by the patch's {@code compost} (0-3) and a living iasor plant. A diseased crop dies
 * ({@code disease} 2) at its next growth step unless it is treated with plant cure. A crop the
 * gardener was paid to look after ({@code protected}, also set for Hosidius crops of players with the
 * easy Kourend &amp; Kebos diary) never gets diseased, nor does an allotment next to a fully grown
 * flower that protects it. A living kronos plant sometimes grows a crop two steps at once.
 */
public abstract class FarmingPatchBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    /** 0 healthy, 1 diseased, 2 dead. */
    public static final IntegerProperty DISEASE = IntegerProperty.create("disease", 0, 2);
    /** 0 none, 1 compost, 2 supercompost, 3 ultracompost. */
    public static final IntegerProperty COMPOST = IntegerProperty.create("compost", 0, 3);
    public static final BooleanProperty PROTECTED = BooleanProperty.create("protected");

    /** Whether a fully grown flower (or scarecrow) next to an allotment protects its crop (produce id). */
    public interface FlowerCheck {
        boolean protects(ServerLevel world, BlockPos pos, String crop);
    }

    /** Set by the skilling module (flower patches keep their state there). */
    public static FlowerCheck flowers = (world, pos, crop) -> false;
    /** Set by the skilling module: the living anima plant ("kronos", "iasor", "attas") or null. */
    public static java.util.function.Function<ServerLevel, String> anima = world -> null;

    protected FarmingPatchBlock(Properties settings) {
        super(settings);
    }

    public abstract IntegerProperty cropProperty();

    public abstract boolean isHerbPatch();

    /** Produce id of the crop in this state (allotments, for flower protection), or null. */
    protected String cropId(BlockState state) {
        return null;
    }

    /** Chance (1 in N) per random tick (~68s on average) to advance one stage, for the crop in this state. */
    protected abstract int growthOdds(BlockState state);

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        int stage = state.getValue(STAGE);
        return state.getValue(cropProperty()) > 0 && stage >= 1 && stage < 4 && state.getValue(DISEASE) < 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!isRandomlyTicking(state) || random.nextInt(growthOdds(state)) != 0) {
            return;
        }
        if (state.getValue(DISEASE) == 1) {
            // Left untreated for a whole growth cycle: the crop dies.
            world.setBlock(pos, state.setValue(DISEASE, 2), Block.UPDATE_ALL);
            return;
        }
        boolean free = com.runecraft.world.Gielinor.is(world) && FarmingDisease.diseaseFreeArea(
                com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX()), com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ()));
        String plant = anima.apply(world);
        int base = isHerbPatch() ? FarmingDisease.BASE_CHANCE : FarmingDisease.ALLOTMENT_CHANCE;
        int chance = free ? 0 : FarmingDisease.chance(base, state.getValue(COMPOST), "iasor".equals(plant));
        String crop = cropId(state);
        boolean safe = state.getValue(PROTECTED) || (chance > 0 && crop != null && flowers.protects(world, pos, crop));
        if (FarmingDisease.rollDisease(chance, safe, random::nextInt)) {
            world.setBlock(pos, state.setValue(DISEASE, 1), Block.UPDATE_ALL);
            return;
        }
        int grow = "kronos".equals(plant) && random.nextInt(100) < FarmingDisease.KRONOS_SKIP_PERCENT ? 2 : 1;
        world.setBlock(pos, state.setValue(STAGE, Math.min(4, state.getValue(STAGE) + grow)), Block.UPDATE_ALL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            FarmingService.use(sp, (ServerLevel) world, pos, state, this);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
