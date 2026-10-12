package com.runecraft.block;

import com.runecraft.osrs.data.Herb;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

public class HerbPatchBlock extends FarmingPatchBlock {
    public static final IntProperty CROP = IntProperty.of("crop", 0, Herb.values().length);

    public HerbPatchBlock(Settings settings) {
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
        return true;
    }

    @Override
    protected int growthOdds(BlockState state) {
        // 4 x 20-minute cycles, shown as 3 visible growth steps.
        return com.runecraft.osrs.FarmingYield.randomTickOdds(80, 3);
    }
}
