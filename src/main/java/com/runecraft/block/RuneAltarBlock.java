package com.runecraft.block;

import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.skill.Runecrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RuneAltarBlock extends Block {
    private final RuneAltar altar;

    public RuneAltarBlock(RuneAltar altar, Properties settings) {
        super(settings);
        this.altar = altar;
    }

    public RuneAltar altar() {
        return altar;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        world.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                random.nextGaussian() * 0.5, -0.5, random.nextGaussian() * 0.5);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            Runecrafting.craft(sp, altar, pos);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
