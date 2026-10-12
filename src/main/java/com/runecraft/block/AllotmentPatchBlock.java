package com.runecraft.block;

import com.runecraft.osrs.data.Allotment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

public class AllotmentPatchBlock extends FarmingPatchBlock {
    public static final IntProperty CROP = IntProperty.of("crop", 0, Allotment.values().length);

    public AllotmentPatchBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(CROP, 0).with(STAGE, 0).with(DISEASE, 0).with(COMPOST, 0)
                .with(PROTECTED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CROP, STAGE, DISEASE, COMPOST, PROTECTED);
    }

    @Override
    public IntProperty cropProperty() {
        return CROP;
    }

    @Override
    public boolean isHerbPatch() {
        return false;
    }

    @Override
    protected int growthOdds(BlockState state) {
        int crop = state.get(CROP);
        int minutes = crop <= 0 ? 40 : Allotment.values()[crop - 1].growthMinutes();
        return com.runecraft.osrs.FarmingYield.randomTickOdds(minutes, 3);
    }
}
