package com.runecraft.world;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.ChunkBuildQueue;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureIndex;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import com.runecraft.platform.ServerChunkEvents;
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * Applies {@link StructureDef}s in Gielinor. Each structure is split per chunk: when a chunk loads
 * its share of every intersecting structure is queued and built a few chunks per tick, clipped to
 * that chunk, so a whole city appears without forcing neighbouring chunks to generate. A queued
 * chunk that is no longer loaded when its turn comes waits in {@link ChunkBuildQueue} until it is
 * loaded again (it used to be dropped, and since Fabric does not announce an in-memory chunk twice,
 * chunks just outside loaded areas were then never built).
 */
public final class StructurePlacer {
    private static final int CHUNKS_PER_TICK = 2;
    /** Per chunk: the structures touching it and exactly which of their operations do. */
    private static final StructureIndex INDEX = StructureIndex.build(ContentRegistry.structures());
    private static final Map<String, StructureDef> REPLACING = new HashMap<>();
    private static final Map<String, BlockState> STATE_CACHE = new HashMap<>();
    /** How often chunks waiting to be loaded again are rechecked. */
    private static final int RECHECK_TICKS = 20;
    private static final ChunkBuildQueue QUEUE = new ChunkBuildQueue();
    private static int recheckTicks;

    static {
        for (StructureDef def : ContentRegistry.structures()) {
            if (def.replacesLandmark() != null) {
                REPLACING.put(def.replacesLandmark(), def);
            }
        }
    }

    private StructurePlacer() {
    }

    /** The structure that replaces this landmark/place's default plaza, or null. */
    public static StructureDef replacing(String placeId) {
        return REPLACING.get(placeId);
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!Gielinor.is(world)) {
                return;
            }
            long key = chunk.getPos().toLong();
            if (INDEX.contains(key)) {
                QUEUE.offer(key);
            }
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (Gielinor.is(world)) {
                QUEUE.unloaded(chunk.getPos().toLong());
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty() && QUEUE.waitingCount() == 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            LongPredicate loaded = key -> world.hasChunk(ChunkPos.getX(key), ChunkPos.getZ(key));
            // Chunks that were announced but demoted before their turn wait until they are loaded again
            // (their load event will not fire a second time); see ChunkBuildQueue.
            if (QUEUE.waitingCount() > 0 && ++recheckTicks >= RECHECK_TICKS) {
                recheckTicks = 0;
                QUEUE.recheck(loaded);
            }
            for (long key : QUEUE.next(CHUNKS_PER_TICK, loaded)) {
                applyChunk(world, ChunkPos.getX(key), ChunkPos.getZ(key));
            }
        });
    }

    /** Builds this chunk's structure shares now if the chunk is loaded (cheap no-op once built). */
    public static void applyIfLoaded(ServerLevel world, int cx, int cz) {
        if (INDEX.contains(cx, cz) && world.hasChunk(cx, cz)) {
            applyChunk(world, cx, cz);
        }
    }

    /** Builds every structure share in the chunks within {@code radius} blocks of a point (loading them). */
    public static void applyAround(ServerLevel world, int x, int z, int radius) {
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
                if (INDEX.contains(cx, cz)) {
                    world.getChunk(cx, cz);
                    applyChunk(world, cx, cz);
                }
            }
        }
    }

    /** Applies each not-yet-built structure share in one chunk. */
    public static void applyChunk(ServerLevel world, int cx, int cz) {
        List<StructureIndex.Share> shares = INDEX.at(cx, cz);
        if (shares == null) {
            return;
        }
        PlayerDataState state = Players.state(world.getServer());
        for (StructureIndex.Share share : shares) {
            StructureDef def = share.def();
            if (state.isStructureBuilt(def.id(), cx, cz)) {
                continue;
            }
            state.markStructureBuilt(def.id(), cx, cz);
            try {
                for (int i = 0; i < share.size(); i++) {
                    apply(world, share.op(i), cx, cz);
                }
            } catch (RuntimeException e) {
                RuneCraft.LOGGER.error("Failed to place structure {} in chunk {},{}", def.id(), cx, cz, e);
            }
        }
    }

    private static void apply(ServerLevel world, StructureDef.Op op, int cx, int cz) {
        int m = op.margin();
        int x1 = Math.max(op.x1() - m, cx << 4);
        int x2 = Math.min(op.x2() + m, (cx << 4) + 15);
        int z1 = Math.max(op.z1() - m, cz << 4);
        int z2 = Math.min(op.z2() + m, (cz << 4) + 15);
        int y1 = Math.max(op.y1(), world.getMinBuildHeight());
        int y2 = Math.min(op.y2(), world.getMaxBuildHeight() - 1);
        if (op.type() == StructureDef.Op.Type.FLATTEN) {
            flatten(world, op, x1, z1, x2, z2);
            return;
        }
        BlockState block = parse(op.block());
        BlockState match = op.type() == StructureDef.Op.Type.REPLACE ? parse(op.extra()) : null;
        BlockState air = Blocks.AIR.defaultBlockState();
        // property lookups hoisted out of the per-block loop (same result for every block of the op)
        boolean multipart = isMultipart(block);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    pos.set(x, y, z);
                    switch (op.type()) {
                        case FILL -> set(world, pos, block, multipart);
                        case CLEAR -> set(world, pos, air, false);
                        case WALLS -> {
                            if (x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2()) {
                                set(world, pos, block, multipart);
                            }
                        }
                        case FILL_AIR -> {
                            BlockState here = world.getBlockState(pos);
                            if (here.isAir() || !here.getFluidState().isEmpty() || here.canBeReplaced()) {
                                set(world, pos, block, multipart);
                            }
                        }
                        case REPLACE -> {
                            if (match != null && world.getBlockState(pos).is(match.getBlock())) {
                                set(world, pos, block, multipart);
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    /**
     * Levels the box so its surface block sits at y1-1, then blends the ring of {@link
     * StructureDef.Op#BLEND} blocks around it linearly back to the natural ground height.
     */
    private static void flatten(ServerLevel world, StructureDef.Op op, int x1, int z1, int x2, int z2) {
        BlockState top = parse(op.block());
        BlockState fill = op.extra() == null ? Blocks.DIRT.defaultBlockState() : parse(op.extra());
        int target = op.y1() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int dx = Math.max(0, Math.max(op.x1() - x, x - op.x2()));
                int dz = Math.max(0, Math.max(op.z1() - z, z - op.z2()));
                int dist = Math.max(dx, dz);
                int natural = naturalGround(world, x, z);
                if (dist == 0) {
                    for (int y = Math.min(natural, target) - 3; y < target; y++) {
                        BlockState here = world.getBlockState(pos.set(x, y, z));
                        if (y >= natural - 1 || here.isAir() || !here.getFluidState().isEmpty()) {
                            set(world, pos, fill);
                        }
                    }
                    set(world, pos.set(x, target, z), top);
                    for (int y = target + 1; y <= op.y2(); y++) {
                        if (!world.getBlockState(pos.set(x, y, z)).isAir()) {
                            set(world, pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                    continue;
                }
                // Rivers, lakes and the sea next to a levelled area are left open (a bank instead of a dam).
                if (!world.getBlockState(pos.set(x, natural + 1, z)).getFluidState().isEmpty()) {
                    continue;
                }
                double t = dist / (double) (StructureDef.Op.BLEND + 1);
                int h = (int) Math.round(target + (natural - target) * t);
                if (h == natural) {
                    continue;
                }
                BlockState surface = world.getBlockState(pos.set(x, natural, z));
                if (!surface.getFluidState().isEmpty() || surface.isAir()) {
                    surface = Blocks.GRASS_BLOCK.defaultBlockState();
                }
                if (h > natural) {
                    for (int y = natural; y < h; y++) {
                        set(world, pos.set(x, y, z), fill);
                    }
                    set(world, pos.set(x, h, z), surface);
                } else {
                    set(world, pos.set(x, h, z), surface);
                    for (int y = h + 1; y <= natural + 1; y++) {
                        set(world, pos.set(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    /** Highest solid, non-tree block in a column (water surfaces count as their floor). */
    private static int naturalGround(ServerLevel world, int x, int z) {
        int y = world.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (y > world.getMinBuildHeight()) {
            BlockState s = world.getBlockState(pos.setY(y));
            if (!(s.isAir() || s.getBlock() instanceof LeavesBlock || s.is(BlockTags.LOGS) || s.canBeReplaced())) {
                break;
            }
            y--;
        }
        return y;
    }

    private static boolean isMultipart(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART);
    }

    private static void set(ServerLevel world, BlockPos pos, BlockState state) {
        set(world, pos, state, isMultipart(state));
    }

    private static void set(ServerLevel world, BlockPos pos, BlockState state, boolean multipart) {
        if (multipart) {
            setMultipart(world, pos, state);
            return;
        }
        BlockState placed = state.isAir() ? state : Block.updateFromNeighbourShapes(state, world, pos);
        world.setBlock(pos, placed, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
    }

    /**
     * Doors, tall plants and beds: neighbour post-processing would break a half placed alone, so
     * place the given half as-is together with its partner (FORCE_STATE stops either half from
     * popping before the other exists).
     */
    private static void setMultipart(ServerLevel world, BlockPos pos, BlockState state) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;
        world.setBlock(pos, state, flags);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            BlockPos other = lower ? pos.above() : pos.below();
            world.setBlock(other, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER), flags);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            boolean foot = state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT;
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos other = foot ? pos.relative(facing) : pos.relative(facing.getOpposite());
            if (!world.hasChunk(other.getX() >> 4, other.getZ() >> 4)) {
                return;
            }
            world.setBlock(other, state.setValue(BlockStateProperties.BED_PART, foot ? BedPart.HEAD : BedPart.FOOT), flags);
        }
    }

    /** Parses a block state in command syntax ("minecraft:oak_stairs[facing=north]"); unknown blocks become air. */
    public static BlockState parse(String text) {
        if (text == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return STATE_CACHE.computeIfAbsent(text, s -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, false).blockState();
            } catch (CommandSyntaxException e) {
                RuneCraft.LOGGER.warn("Structure block '{}' could not be parsed: {}", s, e.getMessage());
                return Blocks.AIR.defaultBlockState();
            }
        });
    }
}
