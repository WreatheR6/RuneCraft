package com.runecraft.block;

import com.runecraft.osrs.Station;
import com.runecraft.skill.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Furnaces, anvils, ranges, banks, shops, altars... anything you click to open a menu. */
public class StationBlock extends Block {
    private final Station station;

    public StationBlock(Station station, Properties settings) {
        super(settings);
        this.station = station;
    }

    public Station station() {
        return station;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (station == Station.FURNACE || station == Station.RANGE) {
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.03, 0);
        } else if (station == Station.PORTAL) {
            for (int i = 0; i < 3; i++) {
                world.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), random.nextGaussian() * 0.3, random.nextDouble() * 0.3, random.nextGaussian() * 0.3);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            Stations.use(sp, station, pos);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
