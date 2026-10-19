package com.runecraft.block;

import com.runecraft.boss.DataBossService;
import com.runecraft.osrs.content.BossDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Summons a data-driven boss. */
public class DataBossSummonBlock extends Block {
    private final BossDef boss;

    public DataBossSummonBlock(BossDef boss, Properties settings) {
        super(settings);
        this.boss = boss;
    }

    public BossDef boss() {
        return boss;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            DataBossService.summon(sp, (ServerLevel) world, pos, boss);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
