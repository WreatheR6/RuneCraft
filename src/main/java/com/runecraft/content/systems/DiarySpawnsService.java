package com.runecraft.content.systems;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.systems.DiaryExtras;
import com.runecraft.platform.ServerChunkEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Item spawns picked up by hand ({@link DiaryExtras#GROUND_SPAWNS}: Karamja's seaweed, the Kalphite Lair's potato cacti). Each
 * spot gets its block when its chunk loads; taking it gives the item (an obtain event, which the diary tasks count) and
 * the block comes back after the spawn's respawn time. A spot in the sea, or with no free ground near it, stays empty.
 */
final class DiarySpawnsService {
    private DiarySpawnsService() {
    }

    /** One spot: its spawn, block X/Z, and an exact feet Y for a dungeon spot (or null on the surface). */
    private record Spot(DiaryExtras.GroundSpawn spawn, int x, int z, Integer y) {
    }

    private static final Map<Long, List<Spot>> BY_CHUNK = new HashMap<>();
    private static final Deque<Spot> QUEUE = new ArrayDeque<>();
    /** Spots taken and waiting to respawn: spot -> game tick it comes back. */
    private static final Map<Spot, Long> RESPAWN = new HashMap<>();
    private static final int PER_TICK = 4;

    static {
        for (DiaryExtras.GroundSpawn g : DiaryExtras.GROUND_SPAWNS) {
            for (int[] t : g.tiles()) {
                add(new Spot(g, GielinorMap.toBlockX(t[0]), GielinorMap.toBlockZ(t[1]), null));
            }
            for (int[] b : g.blocks()) {
                add(new Spot(g, b[0], b[2], b[1]));
            }
        }
    }

    private static void add(Spot s) {
        BY_CHUNK.computeIfAbsent(ChunkPos.asLong(s.x() >> 4, s.z() >> 4), k -> new ArrayList<>()).add(s);
    }

    static void register() {
        for (DiaryExtras.GroundSpawn g : DiaryExtras.GROUND_SPAWNS) {
            BlockHooks.register(g.block(), (p, pos, state) -> take(p, pos, g));
        }
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!Gielinor.is(world)) {
                return;
            }
            List<Spot> here = BY_CHUNK.get(chunk.getPos().toLong());
            if (here != null) {
                for (Spot s : here) {
                    if (!QUEUE.contains(s) && !RESPAWN.containsKey(s)) {
                        QUEUE.add(s);
                    }
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            if (!RESPAWN.isEmpty() && server.getTickCount() % 20 == 0) {
                long now = ActionManager.gameTick();
                Iterator<Map.Entry<Spot, Long>> it = RESPAWN.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Spot, Long> e = it.next();
                    if (e.getValue() <= now) {
                        it.remove();
                        QUEUE.add(e.getKey());
                    }
                }
            }
            for (int n = 0; n < PER_TICK && !QUEUE.isEmpty(); n++) {
                Spot s = QUEUE.poll();
                if (world.hasChunk(s.x() >> 4, s.z() >> 4)) {
                    try {
                        place(world, s);
                    } catch (RuntimeException ex) {
                        RuneCraft.LOGGER.error("Failed to place item spawn {}", s, ex);
                    }
                }
            }
        });
    }

    private static Block block(DiaryExtras.GroundSpawn g) {
        return BuiltInRegistries.BLOCK.get(RuneCraft.id(g.block()));
    }

    /** Puts a spot's block down unless it already stands there (or within two blocks of a surface spot). */
    private static void place(ServerLevel world, Spot s) {
        Block b = block(s.spawn());
        if (b == Blocks.AIR) {
            return;
        }
        StructurePlacer.applyIfLoaded(world, s.x() >> 4, s.z() >> 4);
        if (s.y() != null) {
            BlockPos at = new BlockPos(s.x(), s.y(), s.z());
            BlockState state = world.getBlockState(at);
            if (state.is(b)) {
                return;
            }
            if (state.isAir() && state.getFluidState().isEmpty() && !world.getBlockState(at.below()).isAir()) {
                world.setBlock(at, b.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }
        BlockPos free = null;
        for (int r = 0; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || !world.hasChunk((s.x() + dx) >> 4, (s.z() + dz) >> 4)) {
                        continue;
                    }
                    BlockPos feet = new BlockPos(s.x() + dx, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.x() + dx,
                            s.z() + dz), s.z() + dz);
                    if (world.getBlockState(feet.below()).is(b) || world.getBlockState(feet).is(b)) {
                        return; // already there
                    }
                    BlockState below = world.getBlockState(feet.below());
                    BlockState at = world.getBlockState(feet);
                    if (free == null && below.getFluidState().isEmpty() && !below.isAir() && (at.isAir() || at.canBeReplaced())
                            && at.getFluidState().isEmpty()) {
                        free = feet;
                    }
                }
            }
        }
        if (free != null) {
            world.setBlock(free, b.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The spot a block at this position belongs to (within two blocks of a surface spot, exactly for a dungeon spot). */
    private static Spot spotAt(DiaryExtras.GroundSpawn g, BlockPos pos) {
        List<Spot> near = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                near.addAll(BY_CHUNK.getOrDefault(ChunkPos.asLong((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz), List.of()));
            }
        }
        for (Spot s : near) {
            if (s.spawn() == g && Math.abs(s.x() - pos.getX()) <= 2 && Math.abs(s.z() - pos.getZ()) <= 2
                    && (s.y() == null || s.y() == pos.getY())) {
                return s;
            }
        }
        return null;
    }

    /** Takes the spawn: one item, the block gone until it respawns. */
    static void take(ServerPlayer p, BlockPos pos, DiaryExtras.GroundSpawn g) {
        if (!Inv.hasRoomFor(p, g.item())) {
            Msg.game(p, "You don't have enough inventory space to take that.");
            return;
        }
        p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        Spot s = spotAt(g, pos);
        if (s != null) {
            RESPAWN.put(s, ActionManager.gameTick() + g.respawnTicks());
        }
        Inv.give(p, g.item(), 1);
        Msg.action(p, "You take the " + g.name().toLowerCase(java.util.Locale.ROOT) + ".");
        GameEvents.fireObtain(p, g.item(), 1);
    }
}
