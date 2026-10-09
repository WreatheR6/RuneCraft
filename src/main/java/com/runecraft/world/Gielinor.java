package com.runecraft.world;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Region;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/** The Gielinor dimension and travel helpers. */
public final class Gielinor {
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, RuneCraft.id("gielinor"));

    private Gielinor() {
    }

    public static boolean is(World world) {
        return world != null && world.getRegistryKey().equals(WORLD);
    }

    public static boolean is(Entity entity) {
        return entity != null && is(entity.getWorld());
    }

    public static ServerWorld world(MinecraftServer server) {
        return server.getWorld(WORLD);
    }

    public static Region region(Entity entity) {
        return GielinorMap.regionAtBlock(entity.getBlockX(), entity.getBlockZ());
    }

    public static int wildernessLevel(Entity entity) {
        return is(entity) ? GielinorMap.wildernessLevel(entity.getBlockX(), entity.getBlockZ()) : 0;
    }

    /** Finds a safe standing position at the given column (loads/generates the chunk). */
    public static BlockPos safeSpot(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, Math.max(y, world.getSeaLevel() + 1), z);
        BlockState below = world.getBlockState(pos.down());
        if (!below.isSolidBlock(world, pos.down()) && below.getFluidState().isEmpty() && y <= world.getBottomY() + 1) {
            pos = new BlockPos(x, world.getSeaLevel() + 1, z);
        }
        return pos;
    }

    /** Teleports a player to a landmark in Gielinor (from any dimension). */
    public static void teleport(ServerPlayerEntity player, Landmark landmark) {
        ServerWorld world = world(player.getServer());
        if (world == null) {
            RuneCraft.LOGGER.error("Gielinor dimension is missing - is the data pack loaded?");
            return;
        }
        if (!is(player)) {
            PlayerData data = Players.data(player);
            data.setReturnPos(player.getBlockPos());
        }
        LandmarkBuilder.buildNow(world, landmark);
        int x = landmark.blockX() + (landmark.isAltar() ? 3 : 0);
        int z = landmark.blockZ() + 3;
        BlockPos pos = safeSpot(world, x, z);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
        PlayerData data = Players.data(player);
        if (!data.visitedGielinor()) {
            data.setVisitedGielinor(true);
            data.setPrayerPoints(data.skills().level(com.runecraft.osrs.Skill.PRAYER));
            com.runecraft.util.Msg.game(player, "Welcome to Gielinor.");
            com.runecraft.util.Msg.game(player, "Press K to open your skills, prayers, spellbook and combat options.");
        }
    }

    /** Sends a player from Gielinor back to where they entered (or the overworld spawn). */
    public static void leave(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        PlayerData data = Players.data(player);
        BlockPos target = data.returnPos();
        if (target == null) {
            target = overworld.getSpawnPos();
        }
        overworld.getChunk(target.getX() >> 4, target.getZ() >> 4);
        if (!overworld.getBlockState(target).isAir() || !overworld.getBlockState(target.up()).isAir()) {
            target = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, target);
        }
        player.teleport(overworld, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}
