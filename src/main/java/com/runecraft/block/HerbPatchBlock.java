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
        setDefaultState(this.stateManager.getDefaultState().with(CROP, 0).with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CROP, STAGE);
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
    protected int growthOdds() {
        return 18;
    }
}
