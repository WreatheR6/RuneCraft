package com.runecraft.block;

import com.runecraft.osrs.data.Rock;
import com.runecraft.skill.Gathering;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RockBlock extends ResourceBlock {
    private final Rock rock;

    public RockBlock(Rock rock, Settings settings) {
        super(settings);
        this.rock = rock;
    }

    public Rock rock() {
        return rock;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            Gathering.mine(sp, pos, rock);
        }
        return ActionResult.success(world.isClient());
    }
}
