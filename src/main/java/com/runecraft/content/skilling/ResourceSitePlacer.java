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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
                BY_CHUNK.computeIfAbsent(ChunkPos.asLong(tile.x() >> 4, tile.z() >> 4), k -> new ArrayList<>()).add(tile);
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
                    place(world, t);
                } catch (RuntimeException e) {
                    RuneCraft.LOGGER.error("Failed to place resource {}", t.key(), e);
                }
            }
        });
    }

    private static void place(ServerLevel world, Tile t) {
        Block block = BuiltInRegistries.BLOCK.get(RuneCraft.resolve(t.site().block()));
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
                if (d >= bestDist || !world.hasChunk(x >> 4, z >> 4)) {
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
            case FISHING -> world.setBlock(best, block.defaultBlockState(), Block.UPDATE_ALL);
            case ROCK -> world.setBlock(best, block.defaultBlockState(), Block.UPDATE_ALL);
            case TREE -> tree(world, best, block, t.site().block());
        }
    }

    /** True if a structure already put this resource within a few blocks of the tile. */
    private static boolean alreadyNear(ServerLevel world, Tile t, Block block) {
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING, t.x(), t.z());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (!world.hasChunk((t.x() + dx) >> 4, (t.z() + dz) >> 4)) {
                    continue;
                }
                for (int dy = -8; dy <= 4; dy++) {
                    if (world.getBlockState(p.set(t.x() + dx, top + dy, t.z() + dz)).is(block)) {
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
    private static BlockPos shoreWater(ServerLevel world, int x, int z, boolean lava) {
        BlockPos p = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        BlockState s = world.getBlockState(p);
        if (!s.is(lava ? Blocks.LAVA : Blocks.WATER) || !s.getFluidState().isSource() || !world.getBlockState(p.above()).isAir()) {
            return null;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = p.relative(d);
            if (!world.hasChunk(n.getX() >> 4, n.getZ() >> 4)) {
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
    private static BlockPos openGround(ServerLevel world, int x, int z, ResourceSites.Kind kind) {
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);
        BlockState below = world.getBlockState(feet.below());
        if (!below.getFluidState().isEmpty() || !natural(below)) {
            return null;
        }
        int room = kind == ResourceSites.Kind.TREE ? 6 : 2;
        for (int up = 0; up < room; up++) {
            BlockState s = world.getBlockState(feet.above(up));
            if (!s.isAir() && !s.canBeReplaced() && !s.is(BlockTags.LEAVES)) {
                return null;
            }
        }
        return feet;
    }

    private static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.STONE) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.ANDESITE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.SANDSTONE) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.TERRACOTTA) || s.is(Blocks.DIRT_PATH);
    }

    /** A choppable trunk (two resource log blocks) with a vanilla crown above. */
    private static void tree(ServerLevel world, BlockPos base, Block log, String id) {
        BlockState trunk;
        BlockState leaves;
        int height;
        if (id.startsWith("yew")) {
            trunk = Blocks.DARK_OAK_LOG.defaultBlockState();
            leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState();
            height = 6;
        } else if (id.startsWith("magic")) {
            trunk = Blocks.BIRCH_LOG.defaultBlockState();
            leaves = Blocks.AZALEA_LEAVES.defaultBlockState();
            height = 6;
        } else if (id.startsWith("maple")) {
            trunk = Blocks.SPRUCE_LOG.defaultBlockState();
            leaves = Blocks.ACACIA_LEAVES.defaultBlockState();
            height = 6;
        } else if (id.startsWith("willow")) {
            trunk = Blocks.OAK_LOG.defaultBlockState();
            leaves = Blocks.JUNGLE_LEAVES.defaultBlockState();
            height = 5;
        } else {
            trunk = Blocks.JUNGLE_LOG.defaultBlockState();
            leaves = Blocks.JUNGLE_LEAVES.defaultBlockState();
            height = 6;
        }
        leaves = leaves.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        world.setBlock(base.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(base, log.defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(base.above(), log.defaultBlockState(), Block.UPDATE_ALL);
        for (int y = 2; y < height; y++) {
            world.setBlock(base.above(y), trunk, Block.UPDATE_ALL);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = height - 2; dy <= height + 1; dy++) {
                    int r = dy > height ? 1 : 2;
                    if (Math.abs(dx) > r || Math.abs(dz) > r || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
                        continue;
                    }
                    BlockPos p = base.offset(dx, dy, dz);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlock(p, leaves, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
