package com.runecraft.block;

import com.runecraft.osrs.data.Rock;
import com.runecraft.skill.Gathering;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RockBlock extends ResourceBlock {
    private final Rock rock;

    public RockBlock(Rock rock, Properties settings) {
        super(settings);
        this.rock = rock;
    }

    public Rock rock() {
        return rock;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            Gathering.mine(sp, pos, rock);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
