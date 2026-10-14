package com.runecraft.content.skilling;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.skilling.ResourceSites;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places {@link ResourceSites} (fishing spots, trees, rocks at their OSRS positions) on the real surface
 * the first time their chunk loads in Gielinor, after the chunk's structures have been built. Each tile
 * is built once (remembered in the world's saved state).
 */
final class ResourceSitePlacer {
    /** One resource tile waiting to be placed. */
    private record Tile(ResourceSites.Site site, int index, int x, int z) {
        String key() {
            return "res:" + site.id() + ":" + index;
        }
    }

    private static final Map<Long, List<Tile>> BY_CHUNK = new HashMap<>();
    private static final Deque<Tile> QUEUE = new ArrayDeque<>();
    private static final int PER_TICK = 4;
    /** How far (blocks) a tile may move to find open ground or shoreline water. */
    private static final int SEARCH = 6;

    static {
        for (ResourceSites.Site site : ResourceSites.SITES) {
            for (int i = 0; i < site.tiles().size(); i++) {
                int[] t = site.tiles().get(i);
                Tile tile = new Tile(site, i, StructureDef.bx(t[0]), StructureDef.bz(t[1]));
                BY_CHUNK.computeIfAbsent(ChunkPos.toLong(tile.x() >> 4, tile.z() >> 4), k -> new ArrayList<>()).add(tile);
            }
        }
    }

    private ResourceSitePlacer() {
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
                    RuneCraft.LOGGER.error("Failed to place resource {}", t.key(), e);
                }
            }
        });
    }

    private static void place(ServerWorld world, Tile t) {
        Block block = Registries.BLOCK.get(RuneCraft.resolve(t.site().block()));
        if (block == Blocks.AIR || alreadyNear(world, t, block)) {
            return;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                int x = t.x() + dx;
                int z = t.z() + dz;
                double d = dx * dx + dz * dz;
                if (d >= bestDist || !world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos p = t.site().kind() == ResourceSites.Kind.FISHING ? shoreWater(world, x, z, t.site().block().startsWith("lava"))
                        : openGround(world, x, z, t.site().kind());
                if (p != null) {
                    best = p;
                    bestDist = d;
                }
            }
        }
        if (best == null) {
            return;
        }
        switch (t.site().kind()) {
            case FISHING -> world.setBlockState(best, block.getDefaultState(), Block.NOTIFY_ALL);
            case ROCK -> world.setBlockState(best, block.getDefaultState(), Block.NOTIFY_ALL);
            case TREE -> tree(world, best, block, t.site().block());
        }
    }

    /** True if a structure already put this resource within a few blocks of the tile. */
    private static boolean alreadyNear(ServerWorld world, Tile t, Block block) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, t.x(), t.z());
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (!world.isChunkLoaded((t.x() + dx) >> 4, (t.z() + dz) >> 4)) {
                    continue;
                }
                for (int dy = -8; dy <= 4; dy++) {
                    if (world.getBlockState(p.set(t.x() + dx, top + dy, t.z() + dz)).isOf(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Still shoreline water with air above (the surface of the sea, a river or a lake), or null. Lava eel spots
     * ({@code lava}) take the shore of a still lava pool instead.
     */
    private static BlockPos shoreWater(ServerWorld world, int x, int z, boolean lava) {
        BlockPos p = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z);
        BlockState s = world.getBlockState(p);
        if (!s.isOf(lava ? Blocks.LAVA : Blocks.WATER) || !s.getFluidState().isStill() || !world.getBlockState(p.up()).isAir()) {
            return null;
        }
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos n = p.offset(d);
            if (!world.isChunkLoaded(n.getX() >> 4, n.getZ() >> 4)) {
                continue;
            }
            BlockState ns = world.getBlockState(n);
            if (ns.getFluidState().isEmpty() && !ns.isAir()) {
                return p;
            }
        }
        return null;
    }

    /** The first air block above natural, dry ground with room for a rock or tree, or null. */
    private static BlockPos openGround(ServerWorld world, int x, int z, ResourceSites.Kind kind) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);
        BlockState below = world.getBlockState(feet.down());
        if (!below.getFluidState().isEmpty() || !natural(below)) {
            return null;
        }
        int room = kind == ResourceSites.Kind.TREE ? 6 : 2;
        for (int up = 0; up < room; up++) {
            BlockState s = world.getBlockState(feet.up(up));
            if (!s.isAir() && !s.isReplaceable() && !s.isIn(BlockTags.LEAVES)) {
                return null;
            }
        }
        return feet;
    }

    private static boolean natural(BlockState s) {
        return s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.SAND) || s.isOf(Blocks.STONE) || s.isOf(Blocks.GRAVEL)
                || s.isOf(Blocks.ANDESITE) || s.isOf(Blocks.COBBLESTONE) || s.isOf(Blocks.SANDSTONE) || s.isOf(Blocks.SNOW_BLOCK)
                || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.TERRACOTTA) || s.isOf(Blocks.DIRT_PATH);
    }

    /** A choppable trunk (two resource log blocks) with a vanilla crown above. */
    private static void tree(ServerWorld world, BlockPos base, Block log, String id) {
        BlockState trunk;
        BlockState leaves;
        int height;
        if (id.startsWith("yew")) {
            trunk = Blocks.DARK_OAK_LOG.getDefaultState();
            leaves = Blocks.DARK_OAK_LEAVES.getDefaultState();
            height = 6;
        } else if (id.startsWith("magic")) {
            trunk = Blocks.BIRCH_LOG.getDefaultState();
            leaves = Blocks.AZALEA_LEAVES.getDefaultState();
            height = 6;
        } else if (id.startsWith("maple")) {
            trunk = Blocks.SPRUCE_LOG.getDefaultState();
            leaves = Blocks.ACACIA_LEAVES.getDefaultState();
            height = 6;
        } else if (id.startsWith("willow")) {
            trunk = Blocks.OAK_LOG.getDefaultState();
            leaves = Blocks.JUNGLE_LEAVES.getDefaultState();
            height = 5;
        } else {
            trunk = Blocks.JUNGLE_LOG.getDefaultState();
            leaves = Blocks.JUNGLE_LEAVES.getDefaultState();
            height = 6;
        }
        leaves = leaves.with(net.minecraft.block.LeavesBlock.PERSISTENT, true);
        world.setBlockState(base.down(), Blocks.DIRT.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(base, log.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(base.up(), log.getDefaultState(), Block.NOTIFY_ALL);
        for (int y = 2; y < height; y++) {
            world.setBlockState(base.up(y), trunk, Block.NOTIFY_ALL);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = height - 2; dy <= height + 1; dy++) {
                    int r = dy > height ? 1 : 2;
                    if (Math.abs(dx) > r || Math.abs(dz) > r || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
                        continue;
                    }
                    BlockPos p = base.add(dx, dy, dz);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlockState(p, leaves, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }
}
