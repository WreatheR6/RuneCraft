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
    private static final Map<Long, List<com.runecraft.osrs.content.PlaceDef>> PLACES_BY_CHUNK = new HashMap<>();
    private static final Deque<com.runecraft.osrs.content.PlaceDef> PLACE_QUEUE = new ArrayDeque<>();
    /** Quest characters standing far from their landmark, by the chunk of their OSRS tile (spawned when it loads). */
    private static final Map<Long, List<QuestNpc>> REMOTE_NPCS = new HashMap<>();
    private static final Deque<QuestNpc> NPC_QUEUE = new ArrayDeque<>();
    /** Custom structure builders keyed by Landmark enum name or PlaceDef id: (world, plaza centre at floor level). */
    private static final Map<String, List<java.util.function.BiConsumer<ServerWorld, BlockPos>>> CUSTOM = new HashMap<>();

    static {
        for (Landmark l : Landmark.values()) {
            BY_CHUNK.computeIfAbsent(ChunkPos.toLong(l.blockX() >> 4, l.blockZ() >> 4), k -> new ArrayList<>()).add(l);
        }
        for (com.runecraft.osrs.content.PlaceDef p : com.runecraft.osrs.content.ContentRegistry.places()) {
            PLACES_BY_CHUNK.computeIfAbsent(ChunkPos.toLong(p.blockX() >> 4, p.blockZ() >> 4), k -> new ArrayList<>()).add(p);
        }
        for (QuestNpc n : QuestNpc.values()) {
            if (n.remote()) {
                REMOTE_NPCS.computeIfAbsent(ChunkPos.toLong(n.blockX() >> 4, n.blockZ() >> 4), k -> new ArrayList<>()).add(n);
            }
        }
    }

    /**
     * Registers extra structure-building code for a landmark (enum name, e.g. "LUMBRIDGE") or module
     * place id. Runs once, right after the place's plaza is generated, with the plaza centre.
     */
    public static void registerCustom(String placeId, java.util.function.BiConsumer<ServerWorld, BlockPos> builder) {
        CUSTOM.computeIfAbsent(placeId, k -> new ArrayList<>()).add(builder);
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
            List<QuestNpc> npcs = REMOTE_NPCS.get(chunk.getPos().toLong());
            if (npcs != null) {
                for (QuestNpc n : npcs) {
                    if (!NPC_QUEUE.contains(n)) {
                        NPC_QUEUE.add(n);
                    }
                }
            }
            List<com.runecraft.osrs.content.PlaceDef> places = PLACES_BY_CHUNK.get(chunk.getPos().toLong());
            if (places != null) {
                for (com.runecraft.osrs.content.PlaceDef p : places) {
                    if (!PLACE_QUEUE.contains(p)) {
                        PLACE_QUEUE.add(p);
                    }
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty() && PLACE_QUEUE.isEmpty() && NPC_QUEUE.isEmpty()) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world != null) {
                if (!QUEUE.isEmpty()) {
                    buildNow(world, QUEUE.poll());
                } else if (!PLACE_QUEUE.isEmpty()) {
                    buildNow(world, PLACE_QUEUE.poll());
                } else {
                    spawnRemote(world, NPC_QUEUE.poll());
                }
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

    /** Builds a module place if it hasn't been built yet. */
    public static void buildNow(ServerWorld world, com.runecraft.osrs.content.PlaceDef place) {
        PlayerDataState state = Players.state(world.getServer());
        String key = "place:" + place.id();
        if (state.isBuilt(key)) {
            return;
        }
        state.markBuilt(key);
        try {
            buildPlace(world, place);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("Failed to build place {}", place.id(), e);
        }
    }

    private static int groundY(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int ground = Math.max(world.getTopY(Heightmap.Type.OCEAN_FLOOR, x, z), world.getSeaLevel());
        return Math.max(ground, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
    }

    private static void buildPlace(ServerWorld world, com.runecraft.osrs.content.PlaceDef place) {
        int cx = place.blockX();
        int cz = place.blockZ();
        if (replacedBuild(world, place.id(), cx, cz, place.stations(), place.blocks())) {
            return;
        }
        int y = groundY(world, cx, cz);
        if (place.style() != com.runecraft.osrs.content.PlaceDef.Style.NONE) {
            int radius = Math.max(5, 3 + (place.stations().size() + place.blocks().size()) / 2 + 2);
            plaza(world, cx, y, cz, radius, place.style());
            int slot = 0;
            for (Station s : place.stations()) {
                Block block = ModBlocks.STATIONS.get(s);
                if (block != null) {
                    set(world, stationSlot(cx, y + 1, cz, radius - 2, slot++), block.getDefaultState());
                }
            }
            for (String id : place.blocks()) {
                Block block = net.minecraft.registry.Registries.BLOCK.get(RuneCraft.resolve(id));
                if (block != Blocks.AIR) {
                    set(world, stationSlot(cx, y + 1, cz, radius - 2, slot++), block.getDefaultState());
                }
            }
        }
        afterBuild(world, place.id(), cx, y, cz);
    }

    /** Boss arenas, NPCs and custom builders shared by landmarks and module places. */
    private static void afterBuild(ServerWorld world, String placeId, int cx, int y, int cz) {
        int arena = 0;
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            if (b.raidOnly || !placeId.equals(b.place)) {
                continue;
            }
            int r = b.arenaRadius;
            com.runecraft.osrs.world.ArenaLayout.Arena spot = com.runecraft.osrs.world.ArenaLayout.of(b.id);
            int[] fallback = com.runecraft.osrs.world.ArenaLayout.defaultPosition(cx, cz, arena, r);
            int ax = spot != null ? spot.x() : fallback[0];
            int az = spot != null ? spot.z() : fallback[1];
            int ay = groundY(world, ax, az);
            com.runecraft.boss.DataBossService.buildArena(world, new BlockPos(ax, ay, az), r, b.arenaStyle);
            Block stone = ModBlocks.DATA_BOSS_STONES.get(b.id);
            if (stone != null) {
                set(world, new BlockPos(ax, ay + 1, az - r + 2), stone.getDefaultState());
            }
            arena++;
        }
        for (com.runecraft.osrs.content.NpcDef npc : com.runecraft.osrs.content.ContentRegistry.npcs()) {
            if (!placeId.equals(npc.place())) {
                continue;
            }
            QuestNpcEntity entity = ModEntities.QUEST_NPC.create(world);
            if (entity == null) {
                continue;
            }
            int x = cx + npc.dx();
            int z = cz + npc.dz();
            // the building around the spot first, so the NPC is placed on its floor rather than in the open
            StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
            Integer below = com.runecraft.osrs.world.NpcStoreys.undergroundFeetY(npc.id());
            BlockPos feet = below != null ? new BlockPos(x, below, z) : NpcFloor.spot(world, x, z, NpcFloor.isBanker(npc.id(), npc.name()),
                    com.runecraft.osrs.world.NpcStoreys.of(npc.id()));
            entity.refreshPositionAndAngles(feet.getX() + 0.5, below != null ? below : NpcFloor.feetY(world, feet), feet.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360f, 0);
            entity.setNpc(npc);
            entity.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.STRUCTURE, null, null);
            world.spawnEntity(entity);
        }
        for (java.util.function.BiConsumer<ServerWorld, BlockPos> custom : CUSTOM.getOrDefault(placeId, List.of())) {
            try {
                custom.accept(world, new BlockPos(cx, y, cz));
            } catch (RuntimeException e) {
                RuneCraft.LOGGER.error("Custom builder for {} failed", placeId, e);
            }
        }
    }

    private static void plaza(ServerWorld world, int cx, int y, int cz, int radius, com.runecraft.osrs.content.PlaceDef.Style style) {
        BlockState floorA;
        BlockState floorB;
        BlockState wallState;
        switch (style) {
            case RUINS -> {
                floorA = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
                floorB = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
                wallState = Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState();
            }
            case CAVE -> {
                floorA = Blocks.DEEPSLATE_TILES.getDefaultState();
                floorB = Blocks.COBBLED_DEEPSLATE.getDefaultState();
                wallState = Blocks.DEEPSLATE_BRICK_WALL.getDefaultState();
            }
            case CAMP -> {
                floorA = Blocks.COARSE_DIRT.getDefaultState();
                floorB = Blocks.PACKED_MUD.getDefaultState();
                wallState = Blocks.OAK_FENCE.getDefaultState();
            }
            case ARENA -> {
                floorA = Blocks.SMOOTH_SANDSTONE.getDefaultState();
                floorB = Blocks.CUT_SANDSTONE.getDefaultState();
                wallState = Blocks.SANDSTONE_WALL.getDefaultState();
            }
            default -> {
                floorA = Blocks.STONE_BRICKS.getDefaultState();
                floorB = Blocks.POLISHED_ANDESITE.getDefaultState();
                wallState = Blocks.STONE_BRICK_WALL.getDefaultState();
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                for (int fill = 1; fill <= 4; fill++) {
                    BlockState below = world.getBlockState(p.down(fill));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        set(world, p.down(fill), Blocks.STONE_BRICKS.getDefaultState());
                    }
                }
                set(world, p, ((dx + dz) & 1) == 0 ? floorA : floorB);
                for (int up = 1; up <= 6; up++) {
                    set(world, p.up(up), Blocks.AIR.getDefaultState());
                }
                boolean edge = Math.abs(dx) == radius || Math.abs(dz) == radius;
                if (edge && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
                    set(world, p.up(), wallState);
                }
            }
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    /**
     * If a hand-built {@link com.runecraft.osrs.world.StructureDef} replaces this place, builds the
     * structure around it first and then adds only what it lacks: any station block the structure
     * doesn't contain (placed on the ground near the centre), NPCs, arenas and custom builders.
     */
    private static boolean replacedBuild(ServerWorld world, String placeId, int cx, int cz,
                                         java.util.Collection<Station> stations, java.util.Collection<String> blocks) {
        com.runecraft.osrs.world.StructureDef def = StructurePlacer.replacing(placeId);
        if (def == null) {
            return false;
        }
        StructurePlacer.applyAround(world, cx, cz, 32);
        java.util.Set<String> used = new java.util.HashSet<>();
        for (com.runecraft.osrs.world.StructureDef.Op op : def.ops()) {
            if (op.block() != null) {
                String b = op.block();
                int bracket = b.indexOf('[');
                used.add(RuneCraft.resolve(bracket < 0 ? b : b.substring(0, bracket)).toString());
            }
        }
        List<Block> missing = new ArrayList<>();
        for (Station s : stations) {
            Block block = ModBlocks.STATIONS.get(s);
            if (block != null && !used.contains(net.minecraft.registry.Registries.BLOCK.getId(block).toString())) {
                missing.add(block);
            }
        }
        for (String id : blocks) {
            Block block = net.minecraft.registry.Registries.BLOCK.get(RuneCraft.resolve(id));
            if (block != Blocks.AIR && !used.contains(net.minecraft.registry.Registries.BLOCK.getId(block).toString())) {
                missing.add(block);
            }
        }
        int slot = 0;
        for (Block block : missing) {
            BlockPos p = stationSlot(cx, 0, cz, 4, slot++);
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
            set(world, new BlockPos(p.getX(), top, p.getZ()), block.getDefaultState());
        }
        afterBuild(world, placeId, cx, groundY(world, cx, cz), cz);
        return true;
    }

    private static void build(ServerWorld world, Landmark landmark) {
        int cx = landmark.blockX();
        int cz = landmark.blockZ();
        if (!landmark.isAltar() && replacedBuild(world, landmark.name(), cx, cz, landmark.stations(), List.of())) {
            for (Boss b : Boss.values()) {
                // The Barrows brothers are summoned from their sarcophagi in the crypts (dungeons module).
                if (b.lair() == landmark && landmark != Landmark.BARROWS) {
                    int lx = cx - 6 + b.ordinal() % 4 * 2;
                    int lz = cz + 12;
                    buildLair(world, new BlockPos(lx, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, lx, lz), lz), b);
                }
            }
            spawnNpcs(world, landmark, cx, groundY(world, cx, cz) + 1, cz);
            return;
        }
        world.getChunk(cx >> 4, cz >> 4);
        int ground = Math.max(world.getTopY(Heightmap.Type.OCEAN_FLOOR, cx, cz), world.getSeaLevel()) ;
        int y = Math.max(ground, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1);
        if (landmark.isAltar()) {
            buildAltar(world, landmark, cx, y, cz);
            afterBuild(world, landmark.name(), cx, y, cz);
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
            if (b.lair() == landmark && landmark != Landmark.BARROWS) {
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
        // Courses with OSRS obstacle tiles are placed by the skilling module's agility placer instead.
        if (course != null && com.runecraft.osrs.data.AgilityLayouts.tiles(course).isEmpty()) {
            buildCourse(world, course, cx + radius + 4, y, cz - radius);
        }
        spawnNpcs(world, landmark, cx, y + 1, cz);
        afterBuild(world, landmark.name(), cx, y, cz);
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

    /**
     * Spawns a quest character that stands far from its landmark, once, while the chunk of its OSRS tile is loaded
     * (the landmark may be built while that chunk is not, so it isn't spawned with the landmark).
     */
    private static void spawnRemote(ServerWorld world, QuestNpc npc) {
        if (npc == null) {
            return;
        }
        int x = npc.blockX();
        int z = npc.blockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        PlayerDataState state = Players.state(world.getServer());
        String key = "questnpc:" + npc.name();
        if (state.isBuilt(key)) {
            return;
        }
        state.markBuilt(key);
        try {
            QuestNpcEntity entity = ModEntities.QUEST_NPC.create(world);
            if (entity == null) {
                return;
            }
            StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
            BlockPos feet = NpcFloor.spot(world, x, z, NpcFloor.isBanker(npc.name(), npc.displayName()), npc.floor());
            entity.refreshPositionAndAngles(feet.getX() + 0.5, NpcFloor.feetY(world, feet), feet.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360f, 0);
            entity.setNpc(npc);
            entity.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.STRUCTURE, null, null);
            world.spawnEntity(entity);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("Failed to spawn quest NPC {}", npc, e);
        }
    }

    private static void spawnNpcs(ServerWorld world, Landmark landmark, int cx, int y, int cz) {
        for (QuestNpc npc : QuestNpc.values()) {
            if (npc.landmark() != landmark || npc.remote()) {
                continue;
            }
            QuestNpcEntity entity = ModEntities.QUEST_NPC.create(world);
            if (entity == null) {
                continue;
            }
            int x = cx + npc.dx();
            int z = cz + npc.dz();
            StructurePlacer.applyIfLoaded(world, x >> 4, z >> 4);
            BlockPos feet = NpcFloor.spot(world, x, z, NpcFloor.isBanker(npc.name(), npc.displayName()), npc.floor());
            entity.refreshPositionAndAngles(feet.getX() + 0.5, NpcFloor.feetY(world, feet), feet.getZ() + 0.5,
                    world.getRandom().nextFloat() * 360f, 0);
            entity.setNpc(npc);
            entity.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.STRUCTURE, null, null);
            world.spawnEntity(entity);
        }
    }

}
