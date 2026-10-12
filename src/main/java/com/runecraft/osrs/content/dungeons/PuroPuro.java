package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.content.skilling.HunterData;
import com.runecraft.osrs.world.StructureDef;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * Puro-Puro, the implings' home plane: a square maze of wheat walls, reached through the crop circle in
 * the wheat field east of the Zanaris market (OSRS: the crop circle teleports you to the maze centre;
 * here a ladder in the circle's middle goes down to it). Elnock Inquisitor's exchange counter stands in
 * the centre. Implings drift through the maze from its invisible spawns: low-tier spawns everywhere and
 * high-tier spawns in the inner rings, with the wiki's spawn weights ({@link HunterData#PURO_LOW_TIER},
 * {@link HunterData#PURO_HIGH_TIER}). Catches here give the Puro-Puro XP ({@link HunterData.Impling#puroXp()}).
 *
 * <p>Approximations: the maze is carved beneath Zanaris rather than being its own plane, the walls are
 * concentric square rings with gaps (OSRS's maze is irregular) and implings don't flee through the walls.
 */
public final class PuroPuro {
    /** The crop circle tile in the Zanaris wheat field. */
    public static final int CROP_CIRCLE_X = 2452;
    public static final int CROP_CIRCLE_Y = 4442;
    /** Maze floor (the first air block is one above). */
    public static final int FLOOR = 20;
    public static final int HEIGHT = 5;
    /** Half size of the maze in blocks, and of each wheat-wall ring (outermost first). */
    public static final int HALF = 34;
    static final int[] RINGS = {26, 18, 10};

    private PuroPuro() {
    }

    public static int centreX() {
        return StructureDef.bx(CROP_CIRCLE_X);
    }

    public static int centreZ() {
        return StructureDef.bz(CROP_CIRCLE_Y);
    }

    /** True if the block lies inside the maze. */
    public static boolean contains(int blockX, int blockY, int blockZ) {
        return Math.abs(blockX - centreX()) <= HALF && Math.abs(blockZ - centreZ()) <= HALF
                && blockY > FLOOR - 2 && blockY <= FLOOR + HEIGHT + 2;
    }

    static Dungeons.Dungeon dungeon() {
        Cave c = new Cave("dng_puro_puro", "Puro-Puro", mc("moss_block"), mc("grass_block"), mc("glowstone"));
        int f = FLOOR;
        int cx = centreX();
        int cz = centreZ();
        c.room(cx - HALF, cz - HALF, cx + HALF, cz + HALF, f, HEIGHT);
        c.carve();
        // Wheat walls: concentric square rings, each with two 3-wide gaps on opposite sides (rotated ring by ring).
        String wheat = mc("hay_block");
        for (int i = 0; i < RINGS.length; i++) {
            int r = RINGS[i];
            boolean eastWest = i % 2 == 0;
            // North and south walls.
            wallX(c, cx - r, cx + r, cz - r, f, wheat, eastWest ? Integer.MIN_VALUE : cx);
            wallX(c, cx - r, cx + r, cz + r, f, wheat, eastWest ? Integer.MIN_VALUE : cx);
            // West and east walls.
            wallZ(c, cx - r, cz - r, cz + r, f, wheat, eastWest ? cz : Integer.MIN_VALUE);
            wallZ(c, cx + r, cz - r, cz + r, f, wheat, eastWest ? cz : Integer.MIN_VALUE);
        }
        // The centre: Elnock's exchange and a few flowers.
        c.set(cx + 3, f + 1, cz, rc(HunterData.ELNOCKS_EXCHANGE));
        for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            c.set(cx + p[0], f + 1, cz + p[1], mc("cornflower"));
        }
        c.cropCircle(cx, cz, f);
        // Low-tier spawns roam the whole maze; high-tier spawns sit inside the second ring.
        c.camp(cx - HALF + 1, cz - HALF + 1, cx + HALF - 1, cz + HALF - 1, f, 10, 30,
                HunterData.expand(HunterData.PURO_LOW_TIER).toArray(new String[0]));
        c.camp(cx - RINGS[1] + 1, cz - RINGS[1] + 1, cx + RINGS[1] - 1, cz + RINGS[1] - 1, f, 2, 100,
                HunterData.expand(HunterData.PURO_HIGH_TIER).toArray(new String[0])); // counts and respawns approx
        return c.build();
    }

    /** A wall along x at row z from x1 to x2, with a 3-wide gap centred on {@code gap} (MIN_VALUE = none). */
    private static void wallX(Cave c, int x1, int x2, int z, int f, String block, int gap) {
        if (gap == Integer.MIN_VALUE) {
            c.fill(x1, f + 1, z, x2, f + HEIGHT, z, block);
            return;
        }
        c.fill(x1, f + 1, z, gap - 2, f + HEIGHT, z, block);
        c.fill(gap + 2, f + 1, z, x2, f + HEIGHT, z, block);
    }

    private static void wallZ(Cave c, int x, int z1, int z2, int f, String block, int gap) {
        if (gap == Integer.MIN_VALUE) {
            c.fill(x, f + 1, z1, x, f + HEIGHT, z2, block);
            return;
        }
        c.fill(x, f + 1, z1, x, f + HEIGHT, gap - 2, block);
        c.fill(x, f + 1, gap + 2, x, f + HEIGHT, z2, block);
    }
}
