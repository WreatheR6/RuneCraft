package com.runecraft.block;

import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.skill.AgilityService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A numbered obstacle of an agility course; step on them in order to complete a lap. */
public class AgilityObstacleBlock extends Block {
    public static final IntProperty COURSE = IntProperty.of("course", 0, AgilityCourse.values().length - 1);
    public static final IntProperty STEP = IntProperty.of("step", 0, AgilityCourse.MAX_OBSTACLES - 1);

    public AgilityObstacleBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(COURSE, 0).with(STEP, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COURSE, STEP);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient() && entity instanceof ServerPlayerEntity player) {
            AgilityService.step(player, AgilityCourse.values()[state.get(COURSE)], state.get(STEP));
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
