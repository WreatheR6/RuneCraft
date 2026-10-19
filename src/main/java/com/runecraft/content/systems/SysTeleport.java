package com.runecraft.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/** Teleporting to OSRS coordinates with the Wilderness level limits of spells (20) and jewellery (30), and Tele Block. */
public final class SysTeleport {
    public static final int SPELL_WILDERNESS = 20;
    public static final int JEWELLERY_WILDERNESS = 30;

    /** Teleport to House (the extras module's player-owned house registers itself here). */
    public static java.util.function.Consumer<ServerPlayerEntity> houseTeleport = p -> Msg.game(p, "You don't have a house to teleport to.");

    private SysTeleport() {
    }

    private static final java.util.List<java.util.function.BiFunction<ServerPlayerEntity, Teleports.Destination, String>> GATES =
            new java.util.ArrayList<>();

    /** Adds a check that can refuse a destination (returns the refusal message, or null), e.g. a minigame unlock. */
    public static void addGate(java.util.function.BiFunction<ServerPlayerEntity, Teleports.Destination, String> gate) {
        GATES.add(gate);
    }

    public static boolean canTeleport(ServerPlayerEntity p, int maxWilderness) {
        if (com.runecraft.combat.Teleblock.blocked(p) || com.runecraft.content.minigames.MinigameManager.teleportBlocked(p)) {
            return false;
        }
        int wild = Gielinor.wildernessLevel(p);
        if (wild > maxWilderness) {
            Msg.game(p, "A mysterious force blocks your teleport!");
            Msg.game(p, "You can't use this teleport after level " + maxWilderness + " Wilderness.");
            return false;
        }
        return true;
    }

    /** Teleports a player to OSRS world coordinates (from any dimension). */
    public static boolean teleport(ServerPlayerEntity p, Teleports.Destination dest, int maxWilderness) {
        for (java.util.function.BiFunction<ServerPlayerEntity, Teleports.Destination, String> gate : GATES) {
            String refusal = gate.apply(p, dest);
            if (refusal != null) {
                Msg.game(p, refusal);
                return false;
            }
        }
        if (dest.quest() != null && com.runecraft.osrs.content.ContentRegistry.quest(dest.quest()) != null
                && !com.runecraft.quest.QuestApi.isComplete(p, dest.quest())) {
            Msg.game(p, "You need to complete " + com.runecraft.osrs.content.ContentRegistry.quest(dest.quest()).displayName()
                    + " to teleport to " + dest.name() + ".");
            return false;
        }
        if (!canTeleport(p, maxWilderness)) {
            return false;
        }
        ServerWorld world = Gielinor.world(p.getServer());
        if (world == null) {
            Msg.game(p, "Gielinor is not available.");
            return false;
        }
        if (!Gielinor.is(p)) {
            Players.data(p).setReturnPos(p.getBlockPos());
        }
        if (p.getWorld() instanceof ServerWorld from) {
            from.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
        }
        BlockPos pos = arrival(world, dest);
        p.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYaw(), p.getPitch());
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        world.spawnParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.5, 1, 0.5, 0.5);
        Msg.action(p, "You teleport to " + dest.name() + ".");
        return true;
    }

    /**
     * Where a destination lands: the highest safe block of its column, or for an OSRS dungeon tile
     * ({@link Teleports#underground}) the floor of the dungeon carved under the surface tile above it (the surface if no
     * dungeon is there).
     */
    public static BlockPos arrival(ServerWorld world, Teleports.Destination dest) {
        int x = GielinorMap.toBlockX(dest.x());
        int z = GielinorMap.toBlockZ(Teleports.surfaceY(dest));
        if (Teleports.underground(dest)) {
            com.runecraft.world.StructurePlacer.applyAround(world, x, z, 16);
            BlockPos cave = caveFloor(world, x, z);
            if (cave != null) {
                return cave;
            }
        }
        return Gielinor.safeSpot(world, x, z);
    }

    /** The highest spot below the dungeon ceiling near column (x, z) with solid footing and two blocks of air, or null. */
    static BlockPos caveFloor(ServerWorld world, int x, int z) {
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int y = com.runecraft.osrs.content.dungeons.Dungeons.DEEP_TOP; y > world.getBottomY() + 1; y--) {
                        BlockPos feet = new BlockPos(x + dx, y, z + dz);
                        if (standable(world, feet)) {
                            return feet;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean standable(ServerWorld world, BlockPos feet) {
        net.minecraft.block.BlockState below = world.getBlockState(feet.down());
        return world.getBlockState(feet).isAir() && world.getBlockState(feet.up()).isAir()
                && !below.getCollisionShape(world, feet.down()).isEmpty() && below.getFluidState().isEmpty();
    }

    /**
     * The player's own respawn point in Gielinor (a bed they last slept in), or null when they would respawn in
     * Lumbridge (ring of returning).
     */
    public static BlockPos respawnPoint(ServerPlayerEntity p) {
        ServerWorld world = Gielinor.world(p.getServer());
        BlockPos spawn = p.getSpawnPointPosition();
        if (world == null || spawn == null || p.getSpawnPointDimension() != Gielinor.WORLD) {
            return null;
        }
        return net.minecraft.entity.player.PlayerEntity.findRespawnPosition(world, spawn, p.getSpawnAngle(), p.isSpawnForced(), true)
                .map(BlockPos::ofFloored).orElse(null);
    }

    /** Teleports a player to a block position in Gielinor (Tele Block and the Wilderness limit apply). */
    public static boolean teleportTo(ServerPlayerEntity p, BlockPos pos, String name, int maxWilderness) {
        ServerWorld world = Gielinor.world(p.getServer());
        if (world == null || !canTeleport(p, maxWilderness)) {
            return false;
        }
        if (p.getWorld() instanceof ServerWorld from) {
            from.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
        }
        p.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYaw(), p.getPitch());
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        Msg.action(p, "You teleport to " + name + ".");
        return true;
    }

    /** OSRS coordinates of the player. */
    public static int[] osrs(ServerPlayerEntity p) {
        return new int[]{GielinorMap.toOsrsX(p.getBlockPos().getX()), GielinorMap.toOsrsY(p.getBlockPos().getZ())};
    }
}
