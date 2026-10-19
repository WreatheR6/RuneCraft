package com.runecraft.content.skilling;

import com.runecraft.RuneCraft;
import com.runecraft.block.AgilityObstacleBlock;
import com.runecraft.osrs.GielinorMap;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * each tile is placed once (remembered in the world's saved state). A surface shortcut end's key holds
 * its OSRS tile, so an end that moves is placed again at its new tile.
 */
final class AgilityPlacer {
    /**
     * A block to place at (x, z): on the surface, or at height {@code y} if it is not {@link #SURFACE}; for a surface
     * shortcut end, {@code shortcut} and {@code end} (0 = a, 1 = b), else null and -1.
     */
    private record Tile(String key, int x, int z, Supplier<BlockState> state, int y, AgilityShortcuts.Shortcut shortcut, int end) {
        Tile(String key, int x, int z, Supplier<BlockState> state) {
            this(key, x, z, state, SURFACE);
        }

        Tile(String key, int x, int z, Supplier<BlockState> state, int y) {
            this(key, x, z, state, y, null, -1);
        }
    }

    private static final int SURFACE = Integer.MIN_VALUE;

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
                        () -> ModBlocks.AGILITY_OBSTACLE.defaultBlockState().setValue(AgilityObstacleBlock.COURSE, course.ordinal())
                                .setValue(AgilityObstacleBlock.STEP, step)));
            }
        }
        for (AgilityShortcuts.Shortcut s : AgilityShortcuts.SHORTCUTS) {
            Supplier<BlockState> block = () -> BuiltInRegistries.BLOCK.get(RuneCraft.resolve(AgilityShortcuts.BLOCK)).defaultBlockState();
            add(new Tile("shortcut:" + s.id() + ":a:" + s.ax() + "," + s.ay(), StructureDef.bx(s.ax()), StructureDef.bz(s.ay()), block,
                    SURFACE, s, 0));
            add(new Tile("shortcut:" + s.id() + ":b:" + s.bx() + "," + s.by(), StructureDef.bx(s.bx()), StructureDef.bz(s.by()), block,
                    SURFACE, s, 1));
        }
        // Indoor shortcut ends inside surface buildings (the Slayer Tower's chains), at their storey's height.
        for (AgilityShortcuts.BlockEnds e : AgilityShortcuts.TOWER_ENDS) {
            Supplier<BlockState> block = () -> BuiltInRegistries.BLOCK.get(RuneCraft.resolve(AgilityShortcuts.BLOCK)).defaultBlockState();
            add(new Tile("shortcut:" + e.id() + ":a", e.x1(), e.z1(), block, e.y1()));
            add(new Tile("shortcut:" + e.id() + ":b", e.x2(), e.z2(), block, e.y2()));
        }
    }

    private static void add(Tile t) {
        BY_CHUNK.computeIfAbsent(ChunkPos.asLong(t.x() >> 4, t.z() >> 4), k -> new ArrayList<>()).add(t);
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
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            PlayerDataState state = Players.state(server);
            for (int n = 0; n < PER_TICK && !QUEUE.isEmpty(); n++) {
                Tile t = QUEUE.poll();
                if (state.isBuilt(t.key()) || !world.hasChunk(t.x() >> 4, t.z() >> 4)) {
                    continue;
                }
                state.markBuilt(t.key());
                try {
                    StructurePlacer.applyIfLoaded(world, t.x() >> 4, t.z() >> 4);
                    // Worlds from before round 30 keyed a shortcut end by id alone ("shortcut:<id>:a"): keep the block
                    // placed then if it still works as this end (the end did not move, or moved under a tile).
                    if (t.shortcut() != null && state.isBuilt("shortcut:" + t.shortcut().id() + (t.end() == 0 ? ":a" : ":b"))
                            && servesEnd(world, t)) {
                        continue;
                    }
                    place(world, t);
                } catch (RuntimeException e) {
                    RuneCraft.LOGGER.error("Failed to place agility tile {}", t.key(), e);
                }
            }
        });
    }

    /** True if a shortcut block near the tile's spot already counts as its shortcut's end (within a tile). */
    private static boolean servesEnd(ServerLevel world, Tile t) {
        Block shortcut = BuiltInRegistries.BLOCK.get(RuneCraft.resolve(AgilityShortcuts.BLOCK));
        int r = SEARCH + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = t.x() + dx;
                int z = t.z() + dz;
                int ox = GielinorMap.toOsrsX(x);
                int oy = GielinorMap.toOsrsY(z);
                if (!world.hasChunk(x >> 4, z >> 4) || AgilityShortcuts.at(ox, oy) != t.shortcut()
                        || t.shortcut().endAt(ox, oy) != t.end()) {
                    continue;
                }
                int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = top; y >= top - 3; y--) {
                    if (world.getBlockState(new BlockPos(x, y, z)).is(shortcut)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void place(ServerLevel world, Tile t) {
        BlockState block = t.state().get();
        if (block.is(Blocks.AIR)) {
            return;
        }
        if (t.y() != SURFACE) {
            // Exactly at its height (the shortcut's ends are matched by block position): only into air or
            // replaceable blocks (cobwebs...) with a floor below and head room above.
            BlockPos feet = new BlockPos(t.x(), t.y(), t.z());
            BlockState at = world.getBlockState(feet);
            if ((at.isAir() || at.canBeReplaced() || at.is(Blocks.COBWEB)) && at.getFluidState().isEmpty()) {
                world.setBlock(feet, block, Block.UPDATE_ALL);
            }
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
                    if (!world.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }
                    BlockPos feet = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    BlockState below = world.getBlockState(feet.below());
                    BlockState at = world.getBlockState(feet);
                    if (below.getFluidState().isEmpty() && !below.isAir() && (at.isAir() || at.canBeReplaced())
                            && at.getFluidState().isEmpty() && world.getBlockState(feet.above()).isAir()) {
                        world.setBlock(feet, block, Block.UPDATE_ALL);
                        return;
                    }
                }
            }
        }
    }
}
