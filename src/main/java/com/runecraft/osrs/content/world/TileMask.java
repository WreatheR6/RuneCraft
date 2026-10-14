package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * A building footprint traced tile by tile from the OSRS map (mejrs layers_osrs). Row 0 is the
 * northmost tile row {@code tyTop}; column 0 is tile {@code tx0}; a tile is inside when its character
 * is one of {@code chars}. The mask works at block resolution (2 x 2 blocks per tile), so
 * {@link #outline} gives one-block walls that follow chamfers and diagonal wings.
 */
final class TileMask {
    private final boolean[][] in;
    private final int x0;
    private final int z0;

    private TileMask(boolean[][] in, int x0, int z0) {
        this.in = in;
        this.x0 = x0;
        this.z0 = z0;
    }

    /** Every non-space, non-'.' character is inside. */
    static TileMask of(int tx0, int tyTop, String... rows) {
        return of(tx0, tyTop, null, rows);
    }

    /** Only tiles whose character is in {@code chars} are inside ({@code null}: any but ' ' and '.'). */
    static TileMask of(int tx0, int tyTop, String chars, String... rows) {
        int w = 0;
        for (String r : rows) {
            w = Math.max(w, r.length());
        }
        boolean[][] in = new boolean[w * 2][rows.length * 2];
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                char c = rows[j].charAt(i);
                boolean on = chars == null ? (c != ' ' && c != '.') : chars.indexOf(c) >= 0;
                if (on) {
                    in[i * 2][j * 2] = true;
                    in[i * 2 + 1][j * 2] = true;
                    in[i * 2][j * 2 + 1] = true;
                    in[i * 2 + 1][j * 2 + 1] = true;
                }
            }
        }
        return new TileMask(in, bx(tx0), bz(tyTop));
    }

    private boolean at(int i, int j) {
        return i >= 0 && j >= 0 && i < in.length && j < in[0].length && in[i][j];
    }

    private boolean edge(int i, int j) {
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (!at(i + di, j + dj)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 0 = every inside block, 1 = edge blocks only, 2 = non-edge inside blocks only. */
    private void emit(StructureDef.Builder b, int y1, int y2, String block, int mode) {
        for (int j = 0; j < in[0].length; j++) {
            int run = -1;
            for (int i = 0; i <= in.length; i++) {
                boolean on = i < in.length && in[i][j] && (mode == 0 || (mode == 1) == edge(i, j));
                if (on && run < 0) {
                    run = i;
                } else if (!on && run >= 0) {
                    if (block == null) {
                        b.clear(x0 + run, y1, z0 + j, x0 + i - 1, y2, z0 + j);
                    } else {
                        b.fill(x0 + run, y1, z0 + j, x0 + i - 1, y2, z0 + j, block);
                    }
                    run = -1;
                }
            }
        }
    }

    /** Fills the whole footprint from y1 to y2 (absolute); a null block clears it. */
    TileMask fill(StructureDef.Builder b, int y1, int y2, String block) {
        emit(b, y1, y2, block, 0);
        return this;
    }

    /** The footprint's one-block perimeter walls. */
    TileMask outline(StructureDef.Builder b, int y1, int y2, String block) {
        emit(b, y1, y2, block, 1);
        return this;
    }

    /** The footprint less its perimeter (room interiors); a null block clears it. */
    TileMask inside(StructureDef.Builder b, int y1, int y2, String block) {
        emit(b, y1, y2, block, 2);
        return this;
    }

    /** Crenels: every other perimeter block one block above y. */
    TileMask crenels(StructureDef.Builder b, int y, String block) {
        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in[0].length; j++) {
                if (in[i][j] && edge(i, j) && ((i + j) & 1) == 0) {
                    b.set(x0 + i, y, z0 + j, block);
                }
            }
        }
        return this;
    }
}
