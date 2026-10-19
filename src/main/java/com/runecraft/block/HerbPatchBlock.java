package com.runecraft.block;

import com.runecraft.osrs.data.Herb;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class HerbPatchBlock extends FarmingPatchBlock {
    public static final IntegerProperty CROP = IntegerProperty.create("crop", 0, Herb.values().length);

    public HerbPatchBlock(Properties settings) {
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
        return true;
    }

    @Override
    protected int growthOdds(BlockState state) {
        // 4 x 20-minute cycles, shown as 3 visible growth steps.
        return com.runecraft.osrs.FarmingYield.randomTickOdds(80, 3);
    }
}
