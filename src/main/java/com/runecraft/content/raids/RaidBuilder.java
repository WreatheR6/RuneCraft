package com.runecraft.content.raids;

import com.runecraft.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

/**
 * Builds raid instances out of blocks. Each instance occupies its own 1024-block strip of the
 * Gielinor dimension far from the map ({@link #BASE_X}); rooms are enclosed boxes floating at
 * {@link #FLOOR_Y}, laid out west to east and joined by short corridors. Chunks are generated
 * synchronously on the server thread and force-loaded while the raid runs.
 */
public final class RaidBuilder {
    /** Instances start here; instance {@code slot} occupies x in [BASE_X + 1024*slot, +1024). */
    public static final int BASE_X = 2_000_000;
    public static final int BASE_Z = 2_000_000;
    public static final int SLOT_WIDTH = 1024;
    public static final int FLOOR_Y = 100;
    /** Distance between room centres. */
    public static final int SPACING = 56;
    /** Interior half-size: rooms span -HALF..HALF around their centre; walls sit at +-(HALF+1). */
    public static final int HALF = 20;
    public static final int HEIGHT = 10;
    /** Sotetseg's shadow-realm maze is built this far south of the room centre. */
    public static final int MAZE_OFFSET_Z = 64;

    private RaidBuilder() {
    }

    public static int originX(int slot) {
        return BASE_X + SLOT_WIDTH * slot;
    }

    public static BlockPos center(int slot, int room) {
        return new BlockPos(originX(slot) + 32 + room * SPACING, FLOOR_Y, BASE_Z);
    }

    /** Slot index for a block x coordinate, or -1 outside the instance area. */
    public static int slotAt(double x, double z) {
        if (x < BASE_X || Math.abs(z - BASE_Z) > 200) {
            return -1;
        }
        return (int) ((x - BASE_X) / SLOT_WIDTH);
    }

    public static boolean inInstanceArea(double x, double z) {
        return x >= BASE_X - 64 && Math.abs(z - BASE_Z) <= 256;
    }

    public record Palette(BlockState floor, BlockState wall, BlockState accent, BlockState light, BlockState pillar) {
    }

    public static Palette palette(String style) {
        return switch (style == null ? "stone" : style) {
            case "lava" -> new Palette(Blocks.BLACKSTONE.getDefaultState(), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(),
                    Blocks.MAGMA_BLOCK.getDefaultState(), Blocks.SHROOMLIGHT.getDefaultState(), Blocks.BASALT.getDefaultState());
            case "ice" -> new Palette(Blocks.PACKED_ICE.getDefaultState(), Blocks.BLUE_ICE.getDefaultState(),
                    Blocks.SNOW_BLOCK.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState(), Blocks.PACKED_ICE.getDefaultState());
            case "crypt" -> new Palette(Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState(),
                    Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState(), Blocks.SHROOMLIGHT.getDefaultState(),
                    Blocks.CHISELED_DEEPSLATE.getDefaultState());
            case "jungle" -> new Palette(Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
                    Blocks.MOSS_BLOCK.getDefaultState(), Blocks.OCHRE_FROGLIGHT.getDefaultState(), Blocks.JUNGLE_LOG.getDefaultState());
            case "void" -> new Palette(Blocks.OBSIDIAN.getDefaultState(), Blocks.CRYING_OBSIDIAN.getDefaultState(),
                    Blocks.PURPUR_BLOCK.getDefaultState(), Blocks.PEARLESCENT_FROGLIGHT.getDefaultState(),
                    Blocks.PURPUR_PILLAR.getDefaultState());
            case "water" -> new Palette(Blocks.PRISMARINE_BRICKS.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(),
                    Blocks.PRISMARINE.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState());
            case "sand" -> new Palette(Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.CUT_SANDSTONE.getDefaultState(),
                    Blocks.CHISELED_SANDSTONE.getDefaultState(), Blocks.VERDANT_FROGLIGHT.getDefaultState(),
                    Blocks.SANDSTONE.getDefaultState());
            default -> new Palette(Blocks.STONE_BRICKS.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
                    Blocks.CHISELED_STONE_BRICKS.getDefaultState(), Blocks.GLOWSTONE.getDefaultState(),
                    Blocks.STONE_BRICKS.getDefaultState());
        };
    }

    public static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    public static void fill(ServerWorld world, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    world.setBlockState(m.set(x, y, z), state, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    public static BlockState module(String blockId) {
        Block b = ModBlocks.MODULE_BLOCKS.get(blockId);
        return b == null ? Blocks.BEDROCK.getDefaultState() : b.getDefaultState();
    }

    /** Generates and force-loads every chunk in the box (server thread). */
    public static void loadChunks(ServerWorld world, int minX, int minZ, int maxX, int maxZ) {
        RaidsState state = RaidsState.get(world.getServer());
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                world.getChunk(cx, cz);
                world.setChunkForced(cx, cz, true);
                state.addForced(ChunkPos.toLong(cx, cz));
            }
        }
    }

    /**
     * Force-loads a room's chunks without waiting: the forced ticket makes the server generate them
     * in the background, so building the room later does not stall the tick.
     */
    public static void prefetch(ServerWorld world, int slot, int room) {
        BlockPos c = center(slot, room);
        int w = HALF + 1;
        RaidsState state = RaidsState.get(world.getServer());
        for (int cx = (c.getX() - w - 1) >> 4; cx <= (c.getX() + SPACING - w + 1) >> 4; cx++) {
            for (int cz = (c.getZ() - w - 1) >> 4; cz <= (c.getZ() + w + 1) >> 4; cz++) {
                world.setChunkForced(cx, cz, true);
                state.addForced(ChunkPos.toLong(cx, cz));
            }
        }
    }

    /** Releases every chunk of an instance slot. */
    public static void unloadSlot(ServerWorld world, int slot) {
        RaidsState state = RaidsState.get(world.getServer());
        int min = originX(slot) >> 4;
        int max = (originX(slot) + SLOT_WIDTH) >> 4;
        for (long c : new java.util.ArrayList<>(state.forcedChunks())) {
            int cx = ChunkPos.getPackedX(c);
            if (cx >= min && cx <= max) {
                world.setChunkForced(cx, ChunkPos.getPackedZ(c), false);
                state.removeForced(c);
            }
        }
    }

    /** Removes every non-player entity in a box (left-over monsters, dropped items). */
    public static void clearEntities(ServerWorld world, Box box) {
        for (Entity e : world.getOtherEntities(null, box, e -> !(e instanceof PlayerEntity))) {
            e.discard();
        }
    }

    public static Box roomBox(int slot, int room) {
        BlockPos c = center(slot, room);
        return new Box(c.getX() - HALF - 2, FLOOR_Y - 2, c.getZ() - HALF - 2, c.getX() + HALF + 2, FLOOR_Y + HEIGHT + 8,
                c.getZ() + HALF + 2);
    }

    public static Box slotBox(int slot) {
        return new Box(originX(slot), FLOOR_Y - 20, BASE_Z - 200, originX(slot) + SLOT_WIDTH, FLOOR_Y + 40, BASE_Z + 200);
    }

    /**
     * Builds room {@code i}: floor, walls, lit ceiling, doorways, the spectator gallery above it and
     * the corridor towards the next room (closed by a barrier until the room is cleared).
     */
    public static void buildRoom(ServerWorld world, int slot, int i, String style, boolean first, boolean last) {
        BlockPos c = center(slot, i);
        int cx = c.getX();
        int cz = c.getZ();
        int y = FLOOR_Y;
        int w = HALF + 1;
        loadChunks(world, cx - w - 1, cz - w - 1, cx + SPACING - w + 1, cz + w + 1);
        clearEntities(world, roomBox(slot, i));
        Palette pal = palette(style);
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -w; dx <= w; dx++) {
            for (int dz = -w; dz <= w; dz++) {
                boolean wall = Math.abs(dx) == w || Math.abs(dz) == w;
                int x = cx + dx;
                int z = cz + dz;
                world.setBlockState(m.set(x, y - 1, z), pal.wall(), Block.NOTIFY_LISTENERS);
                BlockState floor = wall ? pal.wall() : ((dx * 3 + dz * 5) % 11 == 0 ? pal.accent() : pal.floor());
                world.setBlockState(m.set(x, y, z), floor, Block.NOTIFY_LISTENERS);
                for (int up = 1; up <= HEIGHT; up++) {
                    world.setBlockState(m.set(x, y + up, z), wall ? pal.wall() : Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
                boolean lamp = !wall && Math.floorMod(dx, 6) == 3 && Math.floorMod(dz, 6) == 3;
                world.setBlockState(m.set(x, y + HEIGHT + 1, z), lamp ? pal.light() : pal.wall(), Block.NOTIFY_LISTENERS);
                for (int up = HEIGHT + 2; up <= HEIGHT + 6; up++) {
                    world.setBlockState(m.set(x, y + up, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
        // Corner columns for depth.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                fill(world, cx + sx * (w - 1), y + 1, cz + sz * (w - 1), cx + sx * (w - 2), y + HEIGHT, cz + sz * (w - 2), pal.pillar());
            }
        }
        // Spectator gallery: a glass box above the ceiling with a window looking down into the room.
        fill(world, cx - 3, y + HEIGHT + 1, cz - 3, cx + 3, y + HEIGHT + 1, cz + 3, Blocks.GLASS.getDefaultState());
        fill(world, cx - 4, y + HEIGHT + 1, cz - 4, cx + 4, y + HEIGHT + 5, cz + 4, Blocks.TINTED_GLASS.getDefaultState());
        fill(world, cx - 3, y + HEIGHT + 1, cz - 3, cx + 3, y + HEIGHT + 1, cz + 3, Blocks.GLASS.getDefaultState());
        fill(world, cx - 3, y + HEIGHT + 2, cz - 3, cx + 3, y + HEIGHT + 4, cz + 3, Blocks.AIR.getDefaultState());
        set(world, new BlockPos(cx, y + HEIGHT + 5, cz), Blocks.SEA_LANTERN.getDefaultState());
        // Doorways.
        if (!first) {
            fill(world, cx - w, y + 1, cz - 2, cx - w, y + 4, cz + 2, Blocks.AIR.getDefaultState());
        }
        if (!last) {
            fill(world, cx + w, y + 1, cz - 2, cx + w, y + 4, cz + 2, Blocks.RED_STAINED_GLASS.getDefaultState());
            // Corridor to the next room.
            int x1 = cx + w + 1;
            int x2 = cx + SPACING - w - 1;
            fill(world, x1, y - 1, cz - 3, x2, y + 6, cz + 3, pal.wall());
            fill(world, x1, y, cz - 2, x2, y, cz + 2, pal.floor());
            fill(world, x1, y + 1, cz - 2, x2, y + 4, cz + 2, Blocks.AIR.getDefaultState());
            for (int x = x1 + 1; x < x2; x += 4) {
                set(world, new BlockPos(x, y + 5, cz), pal.light());
            }
        }
    }

    /** Opens the barrier on a room's exit doorway. */
    public static void openExit(ServerWorld world, int slot, int i) {
        BlockPos c = center(slot, i);
        fill(world, c.getX() + HALF + 1, FLOOR_Y + 1, c.getZ() - 2, c.getX() + HALF + 1, FLOOR_Y + 4, c.getZ() + 2,
                Blocks.AIR.getDefaultState());
    }

    /** Fills a room's exit doorway with the given block (e.g. the lobby's start barrier). */
    public static void closeExit(ServerWorld world, int slot, int i, BlockState state) {
        BlockPos c = center(slot, i);
        fill(world, c.getX() + HALF + 1, FLOOR_Y + 1, c.getZ() - 2, c.getX() + HALF + 1, FLOOR_Y + 4, c.getZ() + 2, state);
    }

    /** Builds a free-standing pillar (used for line-of-sight cover). */
    public static void pillar(ServerWorld world, BlockPos base, int radius, int height, BlockState state) {
        fill(world, base.getX() - radius, base.getY() + 1, base.getZ() - radius, base.getX() + radius, base.getY() + height,
                base.getZ() + radius, state);
    }
}
