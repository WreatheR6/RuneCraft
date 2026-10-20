package com.runecraft.selftest;

import java.util.Comparator;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Gives the server the moment a player walking in real time always gives it before stepping somewhere, for the real-player
 * walks (raids, the Fortis Colosseum) that play a whole run inside one server tick.
 *
 * <p>A chunk a walk loads or force-loads (a raid room, a minigame arena slot, the spot it teleports to) only becomes visible
 * to entity searches, and its creatures and items only tick, once the server confirms it on its main thread, normally
 * between ticks. Until then an entity added there is left out of every area search ({@code getEntitiesOfClass}), of
 * {@code ServerLevel.getEntity(id)} and of the floor sweeps that use them: an item dropped there, a creature spawned there
 * or one that should be cleared away is simply not found. {@link #settle} runs the server's queued chunk work until every
 * chunk of an area is entity-ticking, at most {@link #MAX_NANOS}; {@link #holdAndSettle} first holds the chunks with a
 * short-lived ticket, as a player's own view does, for an area nothing else keeps loaded.
 */
public final class ChunkSettle {
    /** How long a settle waits for the chunks at most. */
    public static final long MAX_NANOS = 5_000_000_000L;
    /** Holds the chunks a walk settles around; the ticket lapses by itself a few seconds (100 server ticks) after the walk. */
    private static final TicketType<ChunkPos> HOLD = TicketType.create("runecraft_selftest_settle",
            Comparator.comparingLong(ChunkPos::toLong), 100);

    private ChunkSettle() {
    }

    /**
     * Runs the server's queued chunk work until every chunk over the block columns {@code minX..maxX, minZ..maxZ} is loaded,
     * visible and ticking for entities; false when they were still loading after {@link #MAX_NANOS}.
     */
    public static boolean settle(ServerLevel world, int minX, int minZ, int maxX, int maxZ) {
        ServerChunkCache chunks = world.getChunkSource();
        long deadline = System.nanoTime() + MAX_NANOS;
        while (!entityTicking(world, minX, minZ, maxX, maxZ)) {
            if (System.nanoTime() > deadline) {
                return false;
            }
            if (!chunks.pollTask()) {
                LockSupport.parkNanos(100_000L); // wait for the chunk workers
            }
        }
        return true;
    }

    /** {@link #settle(ServerLevel, int, int, int, int)} over every chunk within {@code r} blocks of {@code c}. */
    public static boolean settle(ServerLevel world, BlockPos c, int r) {
        return settle(world, c.getX() - r, c.getZ() - r, c.getX() + r, c.getZ() + r);
    }

    /** {@link #settle(ServerLevel, int, int, int, int)} over the columns of a box (an arena's bounds). */
    public static boolean settle(ServerLevel world, AABB box) {
        return settle(world, (int) Math.floor(box.minX), (int) Math.floor(box.minZ), (int) Math.floor(box.maxX),
                (int) Math.floor(box.maxZ));
    }

    /**
     * Holds the chunks within {@code r} blocks of {@code c} entity-ticking for a few seconds (a region ticket, as the chunks
     * around a player are held), then {@link #settle settles} them: for a spot nothing else force-loads, such as where a walk
     * just teleported the player.
     */
    public static boolean holdAndSettle(ServerLevel world, BlockPos c, int r) {
        ChunkPos at = new ChunkPos(c);
        int ring = (Math.max(0, r) + 15) >> 4;
        // Ticket level 33 - distance: 31 - ring at the centre, so every chunk out to the ring is at 31 (entity-ticking).
        world.getChunkSource().addRegionTicket(HOLD, at, 2 + ring, at);
        return settle(world, c, r);
    }

    /** Whether every chunk over the block columns {@code minX..maxX, minZ..maxZ} is loaded, visible and ticking for entities. */
    public static boolean entityTicking(ServerLevel world, int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                if (!world.isPositionEntityTicking(new BlockPos(x << 4, 0, z << 4))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** {@link #entityTicking(ServerLevel, int, int, int, int)} for every chunk within {@code r} blocks of {@code c}. */
    public static boolean entityTicking(ServerLevel world, BlockPos c, int r) {
        return entityTicking(world, c.getX() - r, c.getZ() - r, c.getX() + r, c.getZ() + r);
    }
}
