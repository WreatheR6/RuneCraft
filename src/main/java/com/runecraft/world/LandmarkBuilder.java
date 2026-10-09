package com.runecraft.world;

import com.runecraft.RuneCraft;
import com.runecraft.block.AgilityObstacleBlock;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.player.PlayerDataState;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds settlements, rune altars, boss lairs, agility courses and quest characters at their
 * OSRS map positions the first time the containing chunk loads in Gielinor.
 */
public final class LandmarkBuilder {
    private static final Map<Long, List<Landmark>> BY_CHUNK = new HashMap<>();
    private static final Deque<Landmark> QUEUE = new ArrayDeque<>();

    static {
        for (Landmark l : Landmark.values()) {
            BY_CHUNK.computeIfAbsent(ChunkPos.toLong(l.blockX() >> 4, l.blockZ() >> 4), k -> new ArrayList<>()).add(l);
        }
    }

    private LandmarkBuilder() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!Gielinor.is(world)) {
                return;
            }
            List<Landmark> here = BY_CHUNK.get(chunk.getPos().toLong());
            if (here != null) {
                for (Landmark l : here) {
                    if (!QUEUE.contains(l)) {
                        QUEUE.add(l);
                    }
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty()) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world != null) {
                buildNow(world, QUEUE.poll());
            }
        });
    }

    /** Builds a landmark if it hasn't been built yet. */
    public static void buildNow(ServerWorld world, Landmark landmark) {
        PlayerDataState state = Players.state(world.getServer());
        if (state.isBuilt(landmark.name())) {
            return;
        }
        state.markBuilt(landmark.name());
        try {
            build(world, landmark);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("Failed to build landmark {}", landmark, e);
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    private static void build(ServerWorld world, Landmark landmark) {
        int cx = landmark.blockX();
        int cz = landmark.blockZ();
        world.getChunk(cx >> 4, cz >> 4);
        int ground = Math.max(world.getTopY(Heightmap.Type.OCEAN_FLOOR, cx, cz), world.getSeaLevel()) ;
        int y = Math.max(ground, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1);
        if (landmark.isAltar()) {
            buildAltar(world, landmark, cx, y, cz);
            return;
        }
        int radius = landmark.stations().size() > 6 ? 8 : 6;
        // Plaza with paving, cleared air above and a low wall with lanterns.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                boolean edge = Math.abs(dx) == radius || Math.abs(dz) == radius;
                for (int fill = 1; fill <= 4; fill++) {
                    BlockState below = world.getBlockState(p.down(fill));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        set(world, p.down(fill), Blocks.STONE_BRICKS.getDefaultState());
                    }
                }
                set(world, p, ((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.POLISHED_ANDESITE.getDefaultState());
                for (int up = 1; up <= 6; up++) {
                    set(world, p.up(up), Blocks.AIR.getDefaultState());
                }
                if (edge) {
                    boolean gate = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                    if (!gate) {
                        set(world, p.up(), Blocks.STONE_BRICK_WALL.getDefaultState());
                        if ((Math.abs(dx) == radius && Math.abs(dz) == radius)) {
                            set(world, p.up(2), Blocks.LANTERN.getDefaultState());
                        }
                    }
                }
            }
        }
        // Stations around the inside of the plaza.
        List<Station> stations = new ArrayList<>(landmark.stations());
        int slot = 0;
        int inner = radius - 2;
        for (Station s : stations) {
            BlockPos p = stationSlot(cx, y + 1, cz, inner, slot++);
            Block block = ModBlocks.STATIONS.get(s);
            if (block != null) {
                set(world, p, block.getDefaultState());
            }
        }
        // Lair for bosses based here.
        int lair = 0;
        for (Boss b : Boss.values()) {
            if (b.lair() == landmark) {
                BlockPos p = new BlockPos(cx - inner + lair * 2, y + 1, cz + radius + 4);
                buildLair(world, p, b);
                lair++;
            }
        }
        if (landmark == Landmark.LUMBRIDGE) {
            // Lumbridge castle courtyard trees and a fountain.
            set(world, new BlockPos(cx, y, cz), Blocks.WATER.getDefaultState());
        }
        AgilityCourse course = AgilityCourse.at(landmark);
        if (course != null) {
            buildCourse(world, course, cx + radius + 4, y, cz - radius);
        }
        spawnNpcs(world, landmark, cx, y + 1, cz);
    }

    private static BlockPos stationSlot(int cx, int y, int cz, int inner, int slot) {
        // Walk clockwise around a square of radius "inner", 3 blocks apart.
        int side = slot % 4;
        int along = -inner + 2 + (slot / 4) * 3;
        along = Math.max(-inner, Math.min(inner, along));
        return switch (side) {
            case 0 -> new BlockPos(cx + along, y, cz - inner);
            case 1 -> new BlockPos(cx + inner, y, cz + along);
            case 2 -> new BlockPos(cx - along, y, cz + inner);
            default -> new BlockPos(cx - inner, y, cz - along);
        };
    }

    private static void buildAltar(ServerWorld world, Landmark landmark, int cx, int y, int cz) {
        RuneAltar altar = RuneAltar.of(landmark.altar());
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) {
                    continue;
                }
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                set(world, p, (dx + dz) % 3 == 0 ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : Blocks.CRACKED_STONE_BRICKS.getDefaultState());
                for (int up = 1; up <= 4; up++) {
                    set(world, p.up(up), Blocks.AIR.getDefaultState());
                }
                if (dx * dx + dz * dz >= 8 && (dx + dz) % 2 == 0) {
                    set(world, p.up(), Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState());
                }
            }
        }
        if (altar != null) {
            set(world, new BlockPos(cx, y + 1, cz), ModBlocks.ALTARS.get(altar).getDefaultState());
        }
    }

    private static void buildLair(ServerWorld world, BlockPos pos, Boss boss) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(world, pos.add(dx, -1, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                for (int up = 0; up <= 3; up++) {
                    set(world, pos.add(dx, up, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
        set(world, pos, ModBlocks.BOSS_STONES.get(boss).getDefaultState());
    }

    /** A rooftop-style parkour course: numbered pillars, each a short jump from the previous one. */
    private static void buildCourse(ServerWorld world, AgilityCourse course, int x, int y, int z) {
        int height = y + 1;
        for (int step = 0; step < course.obstacles(); step++) {
            int px = x + (step % 4) * 3;
            int pz = z + (step / 4) * 4 + ((step / 4) % 2 == 0 ? 0 : 1);
            if ((step / 4) % 2 == 1) {
                px = x + (3 - step % 4) * 3;
            }
            height += step < course.obstacles() / 2 ? 1 : (step == course.obstacles() / 2 ? 0 : -1);
            for (int h = y; h < height; h++) {
                set(world, new BlockPos(px, h, pz), Blocks.STONE_BRICKS.getDefaultState());
            }
            for (int up = 0; up <= 3; up++) {
                set(world, new BlockPos(px, height + 1 + up, pz), Blocks.AIR.getDefaultState());
            }
            set(world, new BlockPos(px, height, pz), ModBlocks.AGILITY_OBSTACLE.getDefaultState()
                    .with(AgilityObstacleBlock.COURSE, course.ordinal()).with(AgilityObstacleBlock.STEP, step));
        }
    }

    private static void spawnNpcs(ServerWorld world, Landmark landmark, int cx, int y, int cz) {
        for (QuestNpc npc : QuestNpc.values()) {
            if (npc.landmark() != landmark) {
                continue;
            }
            QuestNpcEntity entity = ModEntities.QUEST_NPC.create(world);
            if (entity == null) {
                continue;
            }
            int x = cx + npc.dx();
            int z = cz + npc.dz();
            int top = Math.max(y, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z));
            entity.refreshPositionAndAngles(x + 0.5, top, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
            entity.setNpc(npc);
            entity.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.STRUCTURE, null, null);
            world.spawnEntity(entity);
        }
    }

}
