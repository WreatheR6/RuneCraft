package com.runecraft.content.raids;

import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Lobby and reward rooms shared by all three raids. */
final class CommonRooms {
    private CommonRooms() {
    }

    /** Supplies, the ToA invocation obelisk and the start barrier (in the exit doorway). */
    static final class Lobby extends RoomLogic {
        Lobby(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            RaidBuilder.set(world(), at(-4, -8), RaidBuilder.module(RaidsContent.SUPPLIES));
            RaidBuilder.set(world(), at(-4, 8), RaidBuilder.module(RaidsContent.SUPPLIES));
            if (raid.type == RaidType.TOA) {
                RaidBuilder.set(world(), at(6, 0), RaidBuilder.module(RaidsContent.INVOCATION_OBELISK));
                RaidBuilder.set(world(), at(6, 0).up(), Blocks.SEA_LANTERN.getDefaultState());
            }
            BlockPos c = center();
            switch (raid.type) {
                case COX -> {
                    RaidBuilder.pillar(world(), c.add(0, 0, -12), 1, 6, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
                    RaidBuilder.pillar(world(), c.add(0, 0, 12), 1, 6, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
                }
                case TOB -> {
                    RaidBuilder.fill(world(), c.getX() - 6, c.getY() + 1, c.getZ() - 1, c.getX() + 18, c.getY() + 1, c.getZ() + 1,
                            Blocks.RED_CARPET.getDefaultState());
                }
                case TOA -> {
                    RaidBuilder.pillar(world(), c.add(-8, 0, -10), 1, 9, Blocks.CHISELED_SANDSTONE.getDefaultState());
                    RaidBuilder.pillar(world(), c.add(-8, 0, 10), 1, 9, Blocks.CHISELED_SANDSTONE.getDefaultState());
                    RaidBuilder.pillar(world(), c.add(8, 0, -10), 1, 9, Blocks.CHISELED_SANDSTONE.getDefaultState());
                    RaidBuilder.pillar(world(), c.add(8, 0, 10), 1, 9, Blocks.CHISELED_SANDSTONE.getDefaultState());
                }
            }
            RaidBuilder.set(world(), at(-RaidBuilder.HALF + 1, 3), RaidBuilder.module(RaidsContent.EXIT));
        }

        @Override
        public void start() {
        }

        @Override
        public void gameTick(long tick) {
        }
    }

    /** The reward chest and the exit. */
    static final class Reward extends RoomLogic {
        Reward(RaidInstance raid, RoomDef def, int index) {
            super(raid, def, index);
        }

        @Override
        public void decorate() {
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + 3, c.getY(), c.getZ() - 3, c.getX() + 9, c.getY(), c.getZ() + 3,
                    raid.type == RaidType.TOA ? Blocks.GOLD_BLOCK.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState());
            RaidBuilder.set(world(), at(6, 0), RaidBuilder.module(RaidsContent.REWARD_CHEST));
            RaidBuilder.set(world(), at(6, -2), Blocks.LANTERN.getDefaultState());
            RaidBuilder.set(world(), at(6, 2), Blocks.LANTERN.getDefaultState());
            RaidBuilder.set(world(), at(RaidBuilder.HALF - 1, 0), RaidBuilder.module(RaidsContent.EXIT));
        }

        @Override
        public void start() {
        }

        @Override
        public void gameTick(long tick) {
            if (raid.finished() && tick % 4 == 0) {
                BlockPos chest = at(6, 0);
                world().spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, chest.getX() + 0.5, chest.getY() + 1.2,
                        chest.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        public boolean useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
            return false;
        }
    }
}
