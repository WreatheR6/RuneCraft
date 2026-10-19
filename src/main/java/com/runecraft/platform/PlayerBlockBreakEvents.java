package com.runecraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric's block break event: false cancels the break. Fired from NeoForge's {@code BlockEvent.BreakEvent}. */
public final class PlayerBlockBreakEvents {
    private PlayerBlockBreakEvents() {
    }

    @FunctionalInterface
    public interface Before {
        boolean beforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity);
    }

    public static final Event<Before> BEFORE = new Event<>(listeners -> (world, player, pos, state, blockEntity) -> {
        for (Before l : listeners) {
            if (!l.beforeBlockBreak(world, player, pos, state, blockEntity)) {
                return false;
            }
        }
        return true;
    });
}
