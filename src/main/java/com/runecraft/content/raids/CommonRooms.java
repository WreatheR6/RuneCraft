package com.runecraft.content.raids;

import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.osrs.content.raids.RoomDef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

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
                RaidBuilder.set(world(), at(6, 0).above(), Blocks.SEA_LANTERN.defaultBlockState());
            }
            BlockPos c = center();
            switch (raid.type) {
                case COX -> {
                    RaidBuilder.pillar(world(), c.offset(0, 0, -12), 1, 6, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                    RaidBuilder.pillar(world(), c.offset(0, 0, 12), 1, 6, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                }
                case TOB -> {
                    RaidBuilder.fill(world(), c.getX() - 6, c.getY() + 1, c.getZ() - 1, c.getX() + 18, c.getY() + 1, c.getZ() + 1,
                            Blocks.RED_CARPET.defaultBlockState());
                }
                case TOA -> {
                    RaidBuilder.pillar(world(), c.offset(-8, 0, -10), 1, 9, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    RaidBuilder.pillar(world(), c.offset(-8, 0, 10), 1, 9, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    RaidBuilder.pillar(world(), c.offset(8, 0, -10), 1, 9, Blocks.CHISELED_SANDSTONE.defaultBlockState());
                    RaidBuilder.pillar(world(), c.offset(8, 0, 10), 1, 9, Blocks.CHISELED_SANDSTONE.defaultBlockState());
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
                    raid.type == RaidType.TOA ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            RaidBuilder.set(world(), at(6, 0), RaidBuilder.module(RaidsContent.REWARD_CHEST));
            RaidBuilder.set(world(), at(6, -2), Blocks.LANTERN.defaultBlockState());
            RaidBuilder.set(world(), at(6, 2), Blocks.LANTERN.defaultBlockState());
            RaidBuilder.set(world(), at(RaidBuilder.HALF - 1, 0), RaidBuilder.module(RaidsContent.EXIT));
        }

        @Override
        public void start() {
        }

        @Override
        public void gameTick(long tick) {
            if (raid.finished() && tick % 4 == 0) {
                BlockPos chest = at(6, 0);
                world().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, chest.getX() + 0.5, chest.getY() + 1.2,
                        chest.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        public boolean useBlock(ServerPlayer player, String blockId, BlockPos pos) {
            return false;
        }
    }
}
