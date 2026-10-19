package com.runecraft.world;

import com.runecraft.osrs.Station;
import com.runecraft.registry.ModBlocks;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Picks the floor an NPC stands on in its OSRS column. The highest block of a column is the roof
 * when a building stands there, so NPCs are put on the lowest walkable floor of the column instead
 * (solid below, two free blocks above, scanning down from the top until the ground mass), and
 * bankers stand beside the nearest bank booth on that booth's floor (e.g. Lumbridge's top-floor
 * bank). All scans are bounded (one column, or a small box around the spot for the booth search).
 */
public final class NpcFloor {
    /** How far below the column top floors are looked for. */
    private static final int SCAN_DEPTH = 48;
    /** Horizontal search radius for a banker's booth. */
    private static final int BOOTH_RADIUS = 4;
    /** Collision shapes at most this high (carpets, low snow) count as floor covering, not obstacles. */
    private static final double THIN = 0.25;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private NpcFloor() {
    }

    /** NPCs that work at a bank booth. */
    public static boolean isBanker(String id, String name) {
        return (id != null && id.toLowerCase(Locale.ROOT).contains("banker")) || "Banker".equalsIgnoreCase(name);
    }

    /** Where an NPC whose spot is column (x, z) should stand (block position of its feet). */
    public static BlockPos spot(ServerLevel world, int x, int z, boolean banker) {
        if (banker) {
            BlockPos booth = besideBooth(world, x, z);
            if (booth != null) {
                return booth;
            }
        }
        return new BlockPos(x, lowestY(world, x, z), z);
    }

    /**
     * Like {@link #spot} but on the given storey of the building at this column: the floor-th standable
     * spot above the ground-floor one, each under a ceiling of its own (falls back to the highest found).
     */
    public static BlockPos spot(ServerLevel world, int x, int z, boolean banker, int floor) {
        BlockPos ground = spot(world, x, z, banker);
        if (floor < 0) {
            return basement(world, ground, -floor);
        }
        if (floor == 0) {
            return ground;
        }
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, ground.getY(), z);
        int best = ground.getY();
        int found = 0;
        boolean ceiling = false;
        for (int y = ground.getY() + 1; y <= top && found < floor; y++) { // <= top: an open roof platform counts
            pos.setY(y);
            if (solid(world, pos)) {
                ceiling = true;
            } else if (ceiling && standable(world, pos)) {
                best = y;
                found++;
                ceiling = false;
            }
        }
        return new BlockPos(x, best, z);
    }

    /**
     * The depth-th standable spot below the ground one, each under rock of its own (a basement or cellar under the
     * building); the ground spot if there is none within the scan depth.
     */
    private static BlockPos basement(ServerLevel world, BlockPos ground, int depth) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(ground.getX(), ground.getY(), ground.getZ());
        int found = 0;
        boolean rock = false;
        for (int y = ground.getY() - 1; y > ground.getY() - SCAN_DEPTH && y > world.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (solid(world, pos)) {
                rock = true;
            } else if (rock && standable(world, pos)) {
                found++;
                rock = false;
                if (found == depth) {
                    return pos.immutable();
                }
            }
        }
        return ground;
    }

    /** Y to put an entity's feet at when standing in this block (on top of a carpet, if any). */
    public static double feetY(ServerLevel world, BlockPos pos) {
        VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
        return pos.getY() + (shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y));
    }

    private static boolean thin(VoxelShape shape) {
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) <= THIN;
    }

    private static boolean solid(ServerLevel world, BlockPos pos) {
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    /** Solid (or carpeted) floor under the feet, and room for a 2-block-tall body. */
    public static boolean standable(ServerLevel world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        VoxelShape feetShape = feet.getCollisionShape(world, pos);
        boolean carpet = thin(feetShape);
        if (!(feetShape.isEmpty() || carpet) || !feet.getFluidState().isEmpty()) {
            return false;
        }
        if (!carpet && !solid(world, pos.below())) {
            return false;
        }
        return world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty();
    }

    /** Natural ground (the column's terrain surface rather than a built floor). */
    public static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || !s.getFluidState().isEmpty();
    }

    /**
     * The lowest walkable spot of column (x, z) above the ground: scans down from the top, remembering
     * each standable spot, and stops on natural ground or at the solid mass under the ground floor (3+
     * solid blocks in a row), so cellars and caves underneath are not chosen. Falls back to the top.
     */
    public static int lowestY(ServerLevel world, int x, int z) {
        int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int limit = Math.max(world.getMinBuildHeight() + 1, top - SCAN_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        int best = -1;
        int solidRun = 0;
        for (int y = top; y > limit; y--) {
            pos.setY(y);
            if (standable(world, pos)) {
                best = y;
                if (natural(world.getBlockState(pos.below()))) {
                    break;
                }
            }
            if (solid(world, pos)) {
                if (++solidRun >= 3 && best >= 0) {
                    break;
                }
            } else {
                solidRun = 0;
            }
        }
        return best >= 0 ? best : top;
    }

    /** True if a bank booth is right next to (or diagonally beside) this spot, at feet level. */
    public static boolean nextToBooth(ServerLevel world, BlockPos feet) {
        Block booth = ModBlocks.STATIONS.get(Station.BANK_BOOTH);
        if (booth == null) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && world.getBlockState(feet.offset(dx, 0, dz)).is(booth)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A walkable spot beside the bank booth nearest to (x, z) (within a few blocks, any floor of the
     * column), preferring the side facing the NPC's own spot (behind the counter), or null.
     */
    public static BlockPos besideBooth(ServerLevel world, int x, int z) {
        Block booth = ModBlocks.STATIONS.get(Station.BANK_BOOTH);
        if (booth == null) {
            return null;
        }
        int low = lowestY(world, x, z) - 1;
        int high = Math.max(low + 2, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int bx = x - BOOTH_RADIUS; bx <= x + BOOTH_RADIUS; bx++) {
            for (int bz = z - BOOTH_RADIUS; bz <= z + BOOTH_RADIUS; bz++) {
                if (!world.hasChunk(bx >> 4, bz >> 4)) {
                    continue;
                }
                for (int by = low; by <= high; by++) {
                    if (!world.getBlockState(pos.set(bx, by, bz)).is(booth)) {
                        continue;
                    }
                    for (Direction d : SIDES) {
                        BlockPos stand = new BlockPos(bx + d.getStepX(), by, bz + d.getStepZ());
                        if (!world.hasChunk(stand.getX() >> 4, stand.getZ() >> 4) || !standable(world, stand)) {
                            continue;
                        }
                        int ddx = stand.getX() - x;
                        int ddz = stand.getZ() - z;
                        // nearest booth first, then the side nearest the NPC's spot
                        int score = ((bx - x) * (bx - x) + (bz - z) * (bz - z)) * 4 + ddx * ddx + ddz * ddz;
                        if (score < bestScore) {
                            bestScore = score;
                            best = stand;
                        }
                    }
                }
            }
        }
        return best;
    }
}
