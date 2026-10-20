package com.runecraft.world;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Region;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** The Gielinor dimension and travel helpers. */
public final class Gielinor {
    public static final ResourceKey<Level> WORLD = ResourceKey.create(Registries.DIMENSION, RuneCraft.id("gielinor"));

    private Gielinor() {
    }

    public static boolean is(Level world) {
        return world != null && world.dimension().equals(WORLD);
    }

    public static boolean is(Entity entity) {
        return entity != null && is(entity.level());
    }

    public static ServerLevel world(MinecraftServer server) {
        return server.getLevel(WORLD);
    }

    public static Region region(Entity entity) {
        return GielinorMap.regionAtBlock(entity.blockPosition().getX(), entity.blockPosition().getZ());
    }

    public static int wildernessLevel(Entity entity) {
        return is(entity) ? GielinorMap.wildernessLevel(entity.blockPosition().getX(), entity.blockPosition().getZ()) : 0;
    }

    /**
     * In a safe zone inside the Wilderness: the Ferox Enclave on the surface (no player fights, no looting bag; the
     * Wilderness level still shows for teleports and the like).
     */
    public static boolean inSafeZone(Entity entity) {
        return is(entity) && entity.getY() >= 45
                && GielinorMap.inFeroxEnclave(entity.blockPosition().getX(), entity.blockPosition().getZ());
    }

    /** Finds a safe standing position at the given column (loads/generates the chunk). */
    public static BlockPos safeSpot(ServerLevel world, int x, int z) {
        BlockPos first = column(world, x, z);
        if (standable(world, first)) {
            return first;
        }
        // Under a tree canopy, over a ravine or on a roof edge: take the nearest clear column within 8 blocks.
        for (int r = 1; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos pos = column(world, x + dx, z + dz);
                    if (standable(world, pos)) {
                        return pos;
                    }
                }
            }
        }
        return first;
    }

    private static BlockPos column(ServerLevel world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, Math.max(y, world.getSeaLevel() + 1), z);
        BlockState below = world.getBlockState(pos.below());
        if (!below.isRedstoneConductor(world, pos.below()) && below.getFluidState().isEmpty() && y <= world.getMinBuildHeight() + 1) {
            pos = new BlockPos(x, world.getSeaLevel() + 1, z);
        }
        return pos;
    }

    /** Feet and head free of collision and fluid, and something solid (not fluid) underfoot. */
    private static boolean standable(ServerLevel world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState below = world.getBlockState(pos.below());
        return feet.getCollisionShape(world, pos).isEmpty() && head.getCollisionShape(world, pos.above()).isEmpty()
                && feet.getFluidState().isEmpty() && below.getFluidState().isEmpty()
                && !below.getCollisionShape(world, pos.below()).isEmpty();
    }

    /** Teleports a player to a landmark in Gielinor (from any dimension). */
    public static void teleport(ServerPlayer player, Landmark landmark) {
        ServerLevel world = world(player.getServer());
        if (world == null) {
            RuneCraft.LOGGER.error("Gielinor dimension is missing - is the data pack loaded?");
            return;
        }
        if (!is(player)) {
            PlayerData data = Players.data(player);
            data.setReturnPos(player.blockPosition());
        }
        LandmarkBuilder.buildNow(world, landmark);
        int x = landmark.blockX() + (landmark.isAltar() ? 3 : 0);
        int z = landmark.blockZ() + 3;
        BlockPos pos = safeSpot(world, x, z);
        player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        PlayerData data = Players.data(player);
        if (!data.visitedGielinor()) {
            data.setVisitedGielinor(true);
            data.setPrayerPoints(data.skills().level(com.runecraft.osrs.Skill.PRAYER));
            com.runecraft.util.Msg.game(player, "Welcome to Gielinor.");
            com.runecraft.util.Msg.game(player, "Press K to open your skills, prayers, spellbook and combat options.");
        }
    }

    /** Sends a player from Gielinor back to where they entered (or the overworld spawn). */
    public static void leave(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        PlayerData data = Players.data(player);
        BlockPos target = data.returnPos();
        if (target == null) {
            target = overworld.getSharedSpawnPos();
        }
        overworld.getChunk(target.getX() >> 4, target.getZ() >> 4);
        if (!overworld.getBlockState(target).isAir() || !overworld.getBlockState(target.above()).isAir()) {
            target = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        }
        player.teleportTo(overworld, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
}
