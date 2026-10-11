package com.runecraft.osrs.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spatial index of {@link StructureDef}s by chunk: for every chunk, the structures with at least one
 * operation touching it, in registry order, each with the indices of exactly those operations (in
 * their original order). Built in one pass over the operations, so it costs time proportional to the
 * operations' chunk coverage instead of (chunks in each bounding box) x (operations), and placing a
 * chunk never scans a big structure's thousands of unrelated operations.
 */
public final class StructureIndex {
    /** One structure's share of a chunk: the indices into {@code def.ops()} that intersect it. */
    public record Share(StructureDef def, int[] opIndices) {
        public int size() {
            return opIndices.length;
        }

        public StructureDef.Op op(int i) {
            return def.ops().get(opIndices[i]);
        }

        /** The intersecting operations, in their original order. */
        public List<StructureDef.Op> ops() {
            List<StructureDef.Op> out = new ArrayList<>(opIndices.length);
            for (int i : opIndices) {
                out.add(def.ops().get(i));
            }
            return out;
        }
    }

    private final Map<Long, List<Share>> byChunk;

    private StructureIndex(Map<Long, List<Share>> byChunk) {
        this.byChunk = byChunk;
    }

    /** Same packing as Minecraft's {@code ChunkPos.toLong(x, z)}. */
    public static long key(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int keyX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    public static int keyZ(long key) {
        return (int) (key >>> 32 & 0xFFFFFFFFL);
    }

    public static StructureIndex build(List<StructureDef> defs) {
        Map<Long, List<Share>> byChunk = new HashMap<>();
        for (StructureDef def : defs) {
            // chunk -> growing list of op indices, in first-touch order (ops are visited in order)
            Map<Long, int[]> local = new LinkedHashMap<>();
            List<StructureDef.Op> ops = def.ops();
            for (int i = 0; i < ops.size(); i++) {
                StructureDef.Op op = ops.get(i);
                int m = op.margin();
                for (int cx = (op.x1() - m) >> 4; cx <= (op.x2() + m) >> 4; cx++) {
                    for (int cz = (op.z1() - m) >> 4; cz <= (op.z2() + m) >> 4; cz++) {
                        int[] list = local.computeIfAbsent(key(cx, cz), k -> new int[]{0, 0, 0, 0, 0});
                        int n = list[0];
                        if (n + 1 >= list.length) {
                            list = Arrays.copyOf(list, list.length * 2);
                            local.put(key(cx, cz), list);
                        }
                        list[n + 1] = i;
                        list[0] = n + 1;
                    }
                }
            }
            for (Map.Entry<Long, int[]> e : local.entrySet()) {
                int[] list = e.getValue();
                byChunk.computeIfAbsent(e.getKey(), k -> new ArrayList<>(2))
                        .add(new Share(def, Arrays.copyOfRange(list, 1, list[0] + 1)));
            }
        }
        for (Map.Entry<Long, List<Share>> e : byChunk.entrySet()) {
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }
        return new StructureIndex(byChunk);
    }

    /** The structure shares in a chunk (registry order), or null if none. */
    public List<Share> at(int chunkX, int chunkZ) {
        return byChunk.get(key(chunkX, chunkZ));
    }

    public boolean contains(long key) {
        return byChunk.containsKey(key);
    }

    public boolean contains(int chunkX, int chunkZ) {
        return byChunk.containsKey(key(chunkX, chunkZ));
    }

    /** Number of chunks touched by any structure. */
    public int chunkCount() {
        return byChunk.size();
    }

    /** Every indexed chunk key (for tests and diagnostics). */
    public Iterable<Long> chunkKeys() {
        return Collections.unmodifiableSet(byChunk.keySet());
    }
}
