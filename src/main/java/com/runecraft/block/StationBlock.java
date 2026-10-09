package com.runecraft.block;

import com.runecraft.osrs.Station;
import com.runecraft.skill.Stations;
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

/** Furnaces, anvils, ranges, banks, shops, altars... anything you click to open a menu. */
public class StationBlock extends Block {
    private final Station station;

    public StationBlock(Station station, Settings settings) {
        super(settings);
        this.station = station;
    }

    public Station station() {
        return station;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            Stations.use(sp, station, pos);
        }
        return ActionResult.success(world.isClient());
    }
}
