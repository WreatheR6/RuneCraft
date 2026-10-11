package com.runecraft.osrs.world;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Which (structure, chunk) shares have been placed, stored compactly as one primitive long set of
 * chunk keys ({@link StructureIndex#key}) per structure id, instead of one
 * {@code "s:<structure>:<cx>,<cz>"} string per share (there are ~12k shares in Gielinor).
 * {@link #parseLegacyKey} reads the old string form so existing saves keep their built state.
 */
public final class BuiltChunks {
    /** Prefix of the legacy per-share string keys. */
    public static final String LEGACY_PREFIX = "s:";

    private final Map<String, LongSet> byStructure = new LinkedHashMap<>();

    public boolean isBuilt(String structureId, int chunkX, int chunkZ) {
        LongSet set = byStructure.get(structureId);
        return set != null && set.contains(StructureIndex.key(chunkX, chunkZ));
    }

    /** Marks a share built; returns false if it already was. */
    public boolean markBuilt(String structureId, int chunkX, int chunkZ) {
        return byStructure.computeIfAbsent(structureId, k -> new LongSet()).add(StructureIndex.key(chunkX, chunkZ));
    }

    /** Adds every chunk key of a structure (loading a save). */
    public void addAll(String structureId, long[] chunkKeys) {
        LongSet set = byStructure.computeIfAbsent(structureId, k -> new LongSet());
        for (long k : chunkKeys) {
            set.add(k);
        }
    }

    public Set<String> structures() {
        return Collections.unmodifiableSet(byStructure.keySet());
    }

    /** The built chunk keys of a structure, sorted (stable save output). */
    public long[] chunkKeys(String structureId) {
        LongSet set = byStructure.get(structureId);
        if (set == null) {
            return new long[0];
        }
        long[] out = set.toArray();
        Arrays.sort(out);
        return out;
    }

    public int size() {
        int n = 0;
        for (LongSet s : byStructure.values()) {
            n += s.size;
        }
        return n;
    }

    /** The legacy string key of a share. */
    public static String legacyKey(String structureId, int chunkX, int chunkZ) {
        return LEGACY_PREFIX + structureId + ":" + chunkX + "," + chunkZ;
    }

    /**
     * Marks the share named by a legacy {@code "s:<structure>:<cx>,<cz>"} key; returns false (and
     * changes nothing) if the string is not such a key.
     */
    public boolean parseLegacyKey(String key) {
        if (key == null || !key.startsWith(LEGACY_PREFIX)) {
            return false;
        }
        int colon = key.lastIndexOf(':');
        int comma = key.lastIndexOf(',');
        if (colon <= LEGACY_PREFIX.length() || comma < colon) {
            return false;
        }
        try {
            int cx = Integer.parseInt(key.substring(colon + 1, comma));
            int cz = Integer.parseInt(key.substring(comma + 1));
            markBuilt(key.substring(LEGACY_PREFIX.length(), colon), cx, cz);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Minimal open-addressing set of longs (no boxing). */
    static final class LongSet {
        private static final long EMPTY = Long.MIN_VALUE;
        private long[] table = newTable(16);
        private boolean hasEmptyKey;
        private int size;

        private static long[] newTable(int n) {
            long[] t = new long[n];
            Arrays.fill(t, EMPTY);
            return t;
        }

        private static int slot(long k, int mask) {
            long h = k * 0x9E3779B97F4A7C15L;
            return (int) (h ^ (h >>> 32)) & mask;
        }

        boolean contains(long k) {
            if (k == EMPTY) {
                return hasEmptyKey;
            }
            int mask = table.length - 1;
            for (int i = slot(k, mask); ; i = (i + 1) & mask) {
                long v = table[i];
                if (v == k) {
                    return true;
                }
                if (v == EMPTY) {
                    return false;
                }
            }
        }

        boolean add(long k) {
            if (k == EMPTY) {
                if (hasEmptyKey) {
                    return false;
                }
                hasEmptyKey = true;
                size++;
                return true;
            }
            if ((size + 1) * 2 > table.length) {
                rehash(table.length * 2);
            }
            int mask = table.length - 1;
            for (int i = slot(k, mask); ; i = (i + 1) & mask) {
                long v = table[i];
                if (v == k) {
                    return false;
                }
                if (v == EMPTY) {
                    table[i] = k;
                    size++;
                    return true;
                }
            }
        }

        private void rehash(int n) {
            long[] old = table;
            table = newTable(n);
            int mask = n - 1;
            for (long v : old) {
                if (v != EMPTY) {
                    int i = slot(v, mask);
                    while (table[i] != EMPTY) {
                        i = (i + 1) & mask;
                    }
                    table[i] = v;
                }
            }
        }

        long[] toArray() {
            long[] out = new long[size];
            int n = 0;
            if (hasEmptyKey) {
                out[n++] = EMPTY;
            }
            for (long v : table) {
                if (v != EMPTY) {
                    out[n++] = v;
                }
            }
            return out;
        }
    }
}
