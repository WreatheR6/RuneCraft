package com.runecraft.world;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.ChunkBuildQueue;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureIndex;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

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
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            LongPredicate loaded = key -> world.isChunkLoaded(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
            // Chunks that were announced but demoted before their turn wait until they are loaded again
            // (their load event will not fire a second time); see ChunkBuildQueue.
            if (QUEUE.waitingCount() > 0 && ++recheckTicks >= RECHECK_TICKS) {
                recheckTicks = 0;
                QUEUE.recheck(loaded);
            }
            for (long key : QUEUE.next(CHUNKS_PER_TICK, loaded)) {
                applyChunk(world, ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
            }
        });
    }

    /** Builds this chunk's structure shares now if the chunk is loaded (cheap no-op once built). */
    public static void applyIfLoaded(ServerWorld world, int cx, int cz) {
        if (INDEX.contains(cx, cz) && world.isChunkLoaded(cx, cz)) {
            applyChunk(world, cx, cz);
        }
    }

    /** Builds every structure share in the chunks within {@code radius} blocks of a point (loading them). */
    public static void applyAround(ServerWorld world, int x, int z, int radius) {
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
    public static void applyChunk(ServerWorld world, int cx, int cz) {
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

    private static void apply(ServerWorld world, StructureDef.Op op, int cx, int cz) {
        int m = op.margin();
        int x1 = Math.max(op.x1() - m, cx << 4);
        int x2 = Math.min(op.x2() + m, (cx << 4) + 15);
        int z1 = Math.max(op.z1() - m, cz << 4);
        int z2 = Math.min(op.z2() + m, (cz << 4) + 15);
        int y1 = Math.max(op.y1(), world.getBottomY());
        int y2 = Math.min(op.y2(), world.getTopY() - 1);
        if (op.type() == StructureDef.Op.Type.FLATTEN) {
            flatten(world, op, x1, z1, x2, z2);
            return;
        }
        BlockState block = parse(op.block());
        BlockState match = op.type() == StructureDef.Op.Type.REPLACE ? parse(op.extra()) : null;
        BlockState air = Blocks.AIR.getDefaultState();
        // property lookups hoisted out of the per-block loop (same result for every block of the op)
        boolean multipart = isMultipart(block);
        BlockPos.Mutable pos = new BlockPos.Mutable();
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
                            if (here.isAir() || !here.getFluidState().isEmpty() || here.isReplaceable()) {
                                set(world, pos, block, multipart);
                            }
                        }
                        case REPLACE -> {
                            if (match != null && world.getBlockState(pos).isOf(match.getBlock())) {
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
    private static void flatten(ServerWorld world, StructureDef.Op op, int x1, int z1, int x2, int z2) {
        BlockState top = parse(op.block());
        BlockState fill = op.extra() == null ? Blocks.DIRT.getDefaultState() : parse(op.extra());
        int target = op.y1() - 1;
        BlockPos.Mutable pos = new BlockPos.Mutable();
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
                            set(world, pos, Blocks.AIR.getDefaultState());
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
                    surface = Blocks.GRASS_BLOCK.getDefaultState();
                }
                if (h > natural) {
                    for (int y = natural; y < h; y++) {
                        set(world, pos.set(x, y, z), fill);
                    }
                    set(world, pos.set(x, h, z), surface);
                } else {
                    set(world, pos.set(x, h, z), surface);
                    for (int y = h + 1; y <= natural + 1; y++) {
                        set(world, pos.set(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    /** Highest solid, non-tree block in a column (water surfaces count as their floor). */
    private static int naturalGround(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR, x, z) - 1;
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);
        while (y > world.getBottomY()) {
            BlockState s = world.getBlockState(pos.setY(y));
            if (!(s.isAir() || s.getBlock() instanceof LeavesBlock || s.isIn(BlockTags.LOGS) || s.isReplaceable())) {
                break;
            }
            y--;
        }
        return y;
    }

    private static boolean isMultipart(BlockState state) {
        return state.contains(Properties.DOUBLE_BLOCK_HALF) || state.contains(Properties.BED_PART);
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        set(world, pos, state, isMultipart(state));
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state, boolean multipart) {
        if (multipart) {
            setMultipart(world, pos, state);
            return;
        }
        BlockState placed = state.isAir() ? state : Block.postProcessState(state, world, pos);
        world.setBlockState(pos, placed, Block.NOTIFY_LISTENERS | Block.SKIP_DROPS);
    }

    /**
     * Doors, tall plants and beds: neighbour post-processing would break a half placed alone, so
     * place the given half as-is together with its partner (FORCE_STATE stops either half from
     * popping before the other exists).
     */
    private static void setMultipart(ServerWorld world, BlockPos pos, BlockState state) {
        int flags = Block.NOTIFY_LISTENERS | Block.SKIP_DROPS | Block.FORCE_STATE;
        world.setBlockState(pos, state, flags);
        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            boolean lower = state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            BlockPos other = lower ? pos.up() : pos.down();
            world.setBlockState(other, state.with(Properties.DOUBLE_BLOCK_HALF,
                    lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER), flags);
        } else if (state.contains(Properties.HORIZONTAL_FACING)) {
            boolean foot = state.get(Properties.BED_PART) == BedPart.FOOT;
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            BlockPos other = foot ? pos.offset(facing) : pos.offset(facing.getOpposite());
            if (!world.isChunkLoaded(other.getX() >> 4, other.getZ() >> 4)) {
                return;
            }
            world.setBlockState(other, state.with(Properties.BED_PART, foot ? BedPart.HEAD : BedPart.FOOT), flags);
        }
    }

    /** Parses a block state in command syntax ("minecraft:oak_stairs[facing=north]"); unknown blocks become air. */
    public static BlockState parse(String text) {
        if (text == null) {
            return Blocks.AIR.getDefaultState();
        }
        return STATE_CACHE.computeIfAbsent(text, s -> {
            try {
                return BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), s, false).blockState();
            } catch (CommandSyntaxException e) {
                RuneCraft.LOGGER.warn("Structure block '{}' could not be parsed: {}", s, e.getMessage());
                return Blocks.AIR.getDefaultState();
            }
        });
    }
}
