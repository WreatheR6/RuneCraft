package com.runecraft.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Teleporting to OSRS coordinates with the Wilderness level limits of spells (20) and jewellery (30), and Tele Block. */
public final class SysTeleport {
    public static final int SPELL_WILDERNESS = 20;
    public static final int JEWELLERY_WILDERNESS = 30;

    /** Teleport to House (the extras module's player-owned house registers itself here). */
    public static java.util.function.Consumer<ServerPlayer> houseTeleport = p -> Msg.game(p, "You don't have a house to teleport to.");

    private SysTeleport() {
    }

    private static final java.util.List<java.util.function.BiFunction<ServerPlayer, Teleports.Destination, String>> GATES =
            new java.util.ArrayList<>();

    /** Adds a check that can refuse a destination (returns the refusal message, or null), e.g. a minigame unlock. */
    public static void addGate(java.util.function.BiFunction<ServerPlayer, Teleports.Destination, String> gate) {
        GATES.add(gate);
    }

    public static boolean canTeleport(ServerPlayer p, int maxWilderness) {
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
    public static boolean teleport(ServerPlayer p, Teleports.Destination dest, int maxWilderness) {
        for (java.util.function.BiFunction<ServerPlayer, Teleports.Destination, String> gate : GATES) {
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
        ServerLevel world = Gielinor.world(p.getServer());
        if (world == null) {
            Msg.game(p, "Gielinor is not available.");
            return false;
        }
        if (!Gielinor.is(p)) {
            Players.data(p).setReturnPos(p.blockPosition());
        }
        if (p.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
        }
        BlockPos pos = arrival(world, dest);
        p.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        world.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        world.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.5, 1, 0.5, 0.5);
        Msg.action(p, "You teleport to " + dest.name() + ".");
        return true;
    }

    /**
     * Where a destination lands: the highest safe block of its column, or for an OSRS dungeon tile
     * ({@link Teleports#underground}) the floor of the dungeon carved under the surface tile above it (the surface if no
     * dungeon is there).
     */
    public static BlockPos arrival(ServerLevel world, Teleports.Destination dest) {
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
    static BlockPos caveFloor(ServerLevel world, int x, int z) {
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int y = com.runecraft.osrs.content.dungeons.Dungeons.DEEP_TOP; y > world.getMinBuildHeight() + 1; y--) {
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

    private static boolean standable(ServerLevel world, BlockPos feet) {
        net.minecraft.world.level.block.state.BlockState below = world.getBlockState(feet.below());
        return world.getBlockState(feet).isAir() && world.getBlockState(feet.above()).isAir()
                && !below.getCollisionShape(world, feet.below()).isEmpty() && below.getFluidState().isEmpty();
    }

    /**
     * The player's own respawn point in Gielinor (a bed they last slept in), or null when they would respawn in
     * Lumbridge (ring of returning).
     */
    public static BlockPos respawnPoint(ServerPlayer p) {
        ServerLevel world = Gielinor.world(p.getServer());
        BlockPos spawn = p.getRespawnPosition();
        if (world == null || spawn == null || p.getRespawnDimension() != Gielinor.WORLD) {
            return null;
        }
        return net.minecraft.world.entity.player.Player.findRespawnPositionAndUseSpawnBlock(world, spawn, p.getRespawnAngle(), p.isRespawnForced(), true)
                .map(BlockPos::containing).orElse(null);
    }

    /** Teleports a player to a block position in Gielinor (Tele Block and the Wilderness limit apply). */
    public static boolean teleportTo(ServerPlayer p, BlockPos pos, String name, int maxWilderness) {
        ServerLevel world = Gielinor.world(p.getServer());
        if (world == null || !canTeleport(p, maxWilderness)) {
            return false;
        }
        if (p.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
        }
        p.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        world.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        Msg.action(p, "You teleport to " + name + ".");
        return true;
    }

    /** OSRS coordinates of the player. */
    public static int[] osrs(ServerPlayer p) {
        return new int[]{GielinorMap.toOsrsX(p.blockPosition().getX()), GielinorMap.toOsrsY(p.blockPosition().getZ())};
    }
}
