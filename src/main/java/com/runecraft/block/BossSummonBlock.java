package com.runecraft.block;

import com.runecraft.entity.BossService;
import com.runecraft.osrs.data.Boss;
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

/** A lair entrance / sarcophagus / altar that summons its boss. */
public class BossSummonBlock extends Block {
    private final Boss boss;

    public BossSummonBlock(Boss boss, Settings settings) {
        super(settings);
        this.boss = boss;
    }

    public Boss boss() {
        return boss;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        world.addParticle(boss == Boss.TZTOK_JAD ? ParticleTypes.LAVA : ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            BossService.summon(sp, (ServerWorld) world, pos, boss);
        }
        return ActionResult.success(world.isClient());
    }
}
