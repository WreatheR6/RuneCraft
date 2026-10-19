package com.runecraft.block;

import com.runecraft.osrs.data.Allotment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class AllotmentPatchBlock extends FarmingPatchBlock {
    public static final IntegerProperty CROP = IntegerProperty.create("crop", 0, Allotment.values().length);

    public AllotmentPatchBlock(Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(CROP, 0).setValue(STAGE, 0).setValue(DISEASE, 0).setValue(COMPOST, 0)
                .setValue(PROTECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CROP, STAGE, DISEASE, COMPOST, PROTECTED);
    }

    @Override
    public IntegerProperty cropProperty() {
        return CROP;
    }

    @Override
    public boolean isHerbPatch() {
        return false;
    }

    @Override
    protected String cropId(BlockState state) {
        int crop = state.getValue(CROP);
        return crop <= 0 ? null : Allotment.values()[crop - 1].produceItem();
    }

    @Override
    protected int growthOdds(BlockState state) {
        int crop = state.getValue(CROP);
        int minutes = crop <= 0 ? 40 : Allotment.values()[crop - 1].growthMinutes();
        return com.runecraft.osrs.FarmingYield.randomTickOdds(minutes, 3);
    }
}
