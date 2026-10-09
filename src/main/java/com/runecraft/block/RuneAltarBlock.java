package com.runecraft.block;

import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.skill.Runecrafting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class RuneAltarBlock extends Block {
    private final RuneAltar altar;

    public RuneAltarBlock(RuneAltar altar, Settings settings) {
        super(settings);
        this.altar = altar;
    }

    public RuneAltar altar() {
        return altar;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        world.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                random.nextGaussian() * 0.5, -0.5, random.nextGaussian() * 0.5);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            Runecrafting.craft(sp, altar, pos);
        }
        return ActionResult.success(world.isClient());
    }
}
