package com.runecraft.block;

import com.runecraft.entity.BossService;
import com.runecraft.osrs.data.Boss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A lair entrance / sarcophagus / altar that summons its boss. */
public class BossSummonBlock extends Block {
    private final Boss boss;

    public BossSummonBlock(Boss boss, Properties settings) {
        super(settings);
        this.boss = boss;
    }

    public Boss boss() {
        return boss;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        world.addParticle(boss == Boss.TZTOK_JAD ? ParticleTypes.LAVA : ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            if (boss == Boss.TZTOK_JAD) {
                // TzTok-Jad is only fought at the end of the Fight Cave (OSRS): his stone leads into the cave.
                com.runecraft.util.Msg.game(sp, "TzTok-Jad waits at the end of the Fight Cave. You step into the cave.");
                com.runecraft.content.minigames.MinigameManager.enter(sp, com.runecraft.osrs.content.minigames.Minigame.FIGHT_CAVES,
                        com.runecraft.osrs.content.minigames.Minigame.FIGHT_CAVES.entranceBlock());
                return InteractionResult.SUCCESS;
            }
            BossService.summon(sp, (ServerLevel) world, pos, boss);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
