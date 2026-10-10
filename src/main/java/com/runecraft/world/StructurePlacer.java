package com.runecraft.world;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.world.StructureDef;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@link StructureDef}s in Gielinor. Each structure is split per chunk: when a chunk loads
 * its share of every intersecting structure is queued and built a few chunks per tick, clipped to
 * that chunk, so a whole city appears without forcing neighbouring chunks to generate.
 */
public final class StructurePlacer {
    private static final int CHUNKS_PER_TICK = 2;
    private static final Map<Long, List<StructureDef>> BY_CHUNK = new HashMap<>();
    private static final Map<String, StructureDef> REPLACING = new HashMap<>();
    private static final Map<String, BlockState> STATE_CACHE = new HashMap<>();
    private static final Deque<Long> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    static {
        for (StructureDef def : ContentRegistry.structures()) {
            for (int cx = def.minX() >> 4; cx <= def.maxX() >> 4; cx++) {
                for (int cz = def.minZ() >> 4; cz <= def.maxZ() >> 4; cz++) {
                    final int fx = cx;
                    final int fz = cz;
                    if (def.ops().stream().anyMatch(op -> op.intersectsChunk(fx, fz))) {
                        BY_CHUNK.computeIfAbsent(ChunkPos.toLong(cx, cz), k -> new ArrayList<>()).add(def);
                    }
                }
            }
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
            if (BY_CHUNK.containsKey(key) && QUEUED.add(key)) {
                QUEUE.add(key);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty()) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
                long key = QUEUE.poll();
                QUEUED.remove(key);
                int cx = ChunkPos.getPackedX(key);
                int cz = ChunkPos.getPackedZ(key);
                if (world.isChunkLoaded(cx, cz)) {
                    applyChunk(world, cx, cz);
                }
            }
        });
    }

    /** Builds every structure share in the chunks within {@code radius} blocks of a point (loading them). */
    public static void applyAround(ServerWorld world, int x, int z, int radius) {
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
                if (BY_CHUNK.containsKey(ChunkPos.toLong(cx, cz))) {
                    world.getChunk(cx, cz);
                    applyChunk(world, cx, cz);
                }
            }
        }
    }

    /** Applies each not-yet-built structure share in one chunk. */
    public static void applyChunk(ServerWorld world, int cx, int cz) {
        List<StructureDef> defs = BY_CHUNK.get(ChunkPos.toLong(cx, cz));
        if (defs == null) {
            return;
        }
        PlayerDataState state = Players.state(world.getServer());
        for (StructureDef def : defs) {
            String key = "s:" + def.id() + ":" + cx + "," + cz;
            if (state.isBuilt(key)) {
                continue;
            }
            state.markBuilt(key);
            try {
                for (StructureDef.Op op : def.ops()) {
                    if (op.intersectsChunk(cx, cz)) {
                        apply(world, op, cx, cz);
                    }
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
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    pos.set(x, y, z);
                    switch (op.type()) {
                        case FILL -> set(world, pos, block);
                        case CLEAR -> set(world, pos, Blocks.AIR.getDefaultState());
                        case WALLS -> {
                            if (x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2()) {
                                set(world, pos, block);
                            }
                        }
                        case FILL_AIR -> {
                            BlockState here = world.getBlockState(pos);
                            if (here.isAir() || !here.getFluidState().isEmpty() || here.isReplaceable()) {
                                set(world, pos, block);
                            }
                        }
                        case REPLACE -> {
                            if (match != null && world.getBlockState(pos).isOf(match.getBlock())) {
                                set(world, pos, block);
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

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.contains(Properties.DOUBLE_BLOCK_HALF) || state.contains(Properties.BED_PART)) {
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
