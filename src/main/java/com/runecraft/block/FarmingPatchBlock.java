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
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);
    /** 0 healthy, 1 diseased, 2 dead. */
    public static final IntProperty DISEASE = IntProperty.of("disease", 0, 2);
    /** 0 none, 1 compost, 2 supercompost, 3 ultracompost. */
    public static final IntProperty COMPOST = IntProperty.of("compost", 0, 3);
    public static final BooleanProperty PROTECTED = BooleanProperty.of("protected");

    /** Whether a fully grown flower (or scarecrow) next to an allotment protects its crop (produce id). */
    public interface FlowerCheck {
        boolean protects(ServerWorld world, BlockPos pos, String crop);
    }

    /** Set by the skilling module (flower patches keep their state there). */
    public static FlowerCheck flowers = (world, pos, crop) -> false;
    /** Set by the skilling module: the living anima plant ("kronos", "iasor", "attas") or null. */
    public static java.util.function.Function<ServerWorld, String> anima = world -> null;

    protected FarmingPatchBlock(Settings settings) {
        super(settings);
    }

    public abstract IntProperty cropProperty();

    public abstract boolean isHerbPatch();

    /** Produce id of the crop in this state (allotments, for flower protection), or null. */
    protected String cropId(BlockState state) {
        return null;
    }

    /** Chance (1 in N) per random tick (~68s on average) to advance one stage, for the crop in this state. */
    protected abstract int growthOdds(BlockState state);

    @Override
    public boolean hasRandomTicks(BlockState state) {
        int stage = state.get(STAGE);
        return state.get(cropProperty()) > 0 && stage >= 1 && stage < 4 && state.get(DISEASE) < 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!hasRandomTicks(state) || random.nextInt(growthOdds(state)) != 0) {
            return;
        }
        if (state.get(DISEASE) == 1) {
            // Left untreated for a whole growth cycle: the crop dies.
            world.setBlockState(pos, state.with(DISEASE, 2), Block.NOTIFY_ALL);
            return;
        }
        boolean free = com.runecraft.world.Gielinor.is(world) && FarmingDisease.diseaseFreeArea(
                com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX()), com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ()));
        String plant = anima.apply(world);
        int base = isHerbPatch() ? FarmingDisease.BASE_CHANCE : FarmingDisease.ALLOTMENT_CHANCE;
        int chance = free ? 0 : FarmingDisease.chance(base, state.get(COMPOST), "iasor".equals(plant));
        String crop = cropId(state);
        boolean safe = state.get(PROTECTED) || (chance > 0 && crop != null && flowers.protects(world, pos, crop));
        if (FarmingDisease.rollDisease(chance, safe, random::nextInt)) {
            world.setBlockState(pos, state.with(DISEASE, 1), Block.NOTIFY_ALL);
            return;
        }
        int grow = "kronos".equals(plant) && random.nextInt(100) < FarmingDisease.KRONOS_SKIP_PERCENT ? 2 : 1;
        world.setBlockState(pos, state.with(STAGE, Math.min(4, state.get(STAGE) + grow)), Block.NOTIFY_ALL);
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
