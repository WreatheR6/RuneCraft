package com.runecraft.block;

import com.runecraft.boss.DataBossService;
import com.runecraft.osrs.content.BossDef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Summons a data-driven boss. */
public class DataBossSummonBlock extends Block {
    private final BossDef boss;

    public DataBossSummonBlock(BossDef boss, Settings settings) {
        super(settings);
        this.boss = boss;
    }

    public BossDef boss() {
        return boss;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            DataBossService.summon(sp, (ServerWorld) world, pos, boss);
        }
        return ActionResult.success(world.isClient());
    }
}
