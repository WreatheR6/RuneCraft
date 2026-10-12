package com.runecraft.content.skilling;

import com.runecraft.RuneCraft;
import com.runecraft.block.AgilityObstacleBlock;
import com.runecraft.osrs.content.skilling.AgilityShortcuts;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.AgilityLayouts;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Places agility course obstacles ({@link AgilityLayouts}) and shortcut ends ({@link AgilityShortcuts})
 * on the surface at their OSRS tiles the first time the chunk loads, after the chunk's structures are
 * built. A block only goes into open air on top of the ground (it never replaces a structure block);
 * each tile is placed once (remembered in the world's saved state).
 */
final class AgilityPlacer {
    private record Tile(String key, int x, int z, Supplier<BlockState> state) {
    }

    private static final Map<Long, List<Tile>> BY_CHUNK = new HashMap<>();
    private static final Deque<Tile> QUEUE = new ArrayDeque<>();
    private static final int PER_TICK = 4;
    /** Blocks a tile may move to find a free spot on the ground. */
    private static final int SEARCH = 2;

    static {
        for (AgilityCourse course : AgilityCourse.values()) {
            List<int[]> tiles = AgilityLayouts.tiles(course);
            for (int i = 0; i < tiles.size(); i++) {
                int step = i;
                add(new Tile("agility:" + course.name() + ":" + i, StructureDef.bx(tiles.get(i)[0]), StructureDef.bz(tiles.get(i)[1]),
                        () -> ModBlocks.AGILITY_OBSTACLE.getDefaultState().with(AgilityObstacleBlock.COURSE, course.ordinal())
                                .with(AgilityObstacleBlock.STEP, step)));
            }
        }
        for (AgilityShortcuts.Shortcut s : AgilityShortcuts.SHORTCUTS) {
            Supplier<BlockState> block = () -> Registries.BLOCK.get(RuneCraft.resolve(AgilityShortcuts.BLOCK)).getDefaultState();
            add(new Tile("shortcut:" + s.id() + ":a", StructureDef.bx(s.ax()), StructureDef.bz(s.ay()), block));
            add(new Tile("shortcut:" + s.id() + ":b", StructureDef.bx(s.bx()), StructureDef.bz(s.by()), block));
        }
    }

    private static void add(Tile t) {
        BY_CHUNK.computeIfAbsent(ChunkPos.toLong(t.x() >> 4, t.z() >> 4), k -> new ArrayList<>()).add(t);
    }

    private AgilityPlacer() {
    }

    static void init() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!Gielinor.is(world)) {
                return;
            }
            List<Tile> here = BY_CHUNK.get(chunk.getPos().toLong());
            if (here != null) {
                for (Tile t : here) {
                    if (!QUEUE.contains(t)) {
                        QUEUE.add(t);
                    }
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty()) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            PlayerDataState state = Players.state(server);
            for (int n = 0; n < PER_TICK && !QUEUE.isEmpty(); n++) {
                Tile t = QUEUE.poll();
                if (state.isBuilt(t.key()) || !world.isChunkLoaded(t.x() >> 4, t.z() >> 4)) {
                    continue;
                }
                state.markBuilt(t.key());
                try {
                    StructurePlacer.applyIfLoaded(world, t.x() >> 4, t.z() >> 4);
                    place(world, t);
                } catch (RuntimeException e) {
                    RuneCraft.LOGGER.error("Failed to place agility tile {}", t.key(), e);
                }
            }
        });
    }

    private static void place(ServerWorld world, Tile t) {
        BlockState block = t.state().get();
        if (block.isOf(Blocks.AIR)) {
            return;
        }
        for (int r = 0; r <= SEARCH; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = t.x() + dx;
                    int z = t.z() + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    BlockPos feet = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    BlockState below = world.getBlockState(feet.down());
                    BlockState at = world.getBlockState(feet);
                    if (below.getFluidState().isEmpty() && !below.isAir() && (at.isAir() || at.isReplaceable())
                            && at.getFluidState().isEmpty() && world.getBlockState(feet.up()).isAir()) {
                        world.setBlockState(feet, block, Block.NOTIFY_ALL);
                        return;
                    }
                }
            }
        }
    }
}
