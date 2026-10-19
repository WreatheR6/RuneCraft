package com.runecraft.block;

import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.skill.AgilityService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A numbered obstacle of an agility course; step on them in order to complete a lap. */
public class AgilityObstacleBlock extends Block {
    public static final IntegerProperty COURSE = IntegerProperty.create("course", 0, AgilityCourse.values().length - 1);
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, AgilityCourse.MAX_OBSTACLES - 1);

    public AgilityObstacleBlock(Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(COURSE, 0).setValue(STEP, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COURSE, STEP);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player) {
            AgilityService.step(player, AgilityCourse.values()[state.getValue(COURSE)], state.getValue(STEP));
        }
        super.stepOn(world, pos, state, entity);
    }
}
